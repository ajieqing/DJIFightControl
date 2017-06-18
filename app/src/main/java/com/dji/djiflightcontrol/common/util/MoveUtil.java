package com.dji.djiflightcontrol.common.util;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.dji.djiflightcontrol.common.action.Action;
import com.dji.djiflightcontrol.common.action.ActionType;
import com.dji.djiflightcontrol.common.media.MyMedia;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.battery.DJIBatteryState;
import dji.common.camera.CameraSDCardState;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.products.DJIAircraft;

import static com.dji.djiflightcontrol.common.DJISampleApplication.NAME;
import static com.dji.djiflightcontrol.common.DJISampleApplication.getAircraftInstance;
import static com.dji.djiflightcontrol.common.DJISampleApplication.getCameraInstance;

/**
 * 飞行器定距定速飞行控制工具类
 */

public class MoveUtil {
    private static MoveUtil util = new MoveUtil();
    private final HashMap<String, MyMedia> medias = new HashMap<>();
    private ArrayList<MyMedia> failureMedias = new ArrayList<>();
    private double vz, vz_old = 0;
    private double vx, vx_old = 0;
    private double vy, vy_old = 0;
    private double x = 0, y = 0, z = 0;
    private float mPitch = 0;
    private float mRoll = 0;
    private float mYaw = 0;
    private float mThrottle = 0;
    private DJIFlightController mFlightController;
    private Context context;
    private double la, lo, la_old, lo_old, h;
    private Timer mSendVirtualStickDataTimer, hoverTimer, runTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private TimerTask hoverTask, runTask;
    private DJIAircraft aircraft = getAircraftInstance();
    private boolean done = false;
    private boolean takeoffing = false;
    private boolean landing = false;
    private BeforeStartAction bsa;

    private Action action;
    private float carrect = 0, carrect_d;
    private int power;
    private int order = 0;
    private double d;

    private MoveUtil() {
    }

    public static MoveUtil getUtil() {
        return util;
    }

    public void update() {
        initFlightController();
    }

    //初始化控制器
    public void initFlightController() {
        aircraft = getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            mFlightController = null;
            showToast("设备链接失败");
        } else {
            mFlightController = aircraft.getFlightController();
            if (mFlightController == null) {
                showToast("设备链接失败");
            } else {
                showToast("设备链接成功");
            }
            if (mFlightController != null) {
                mFlightController.enableVirtualStickControlMode(new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }
                    }
                });
                mFlightController.setHorizontalCoordinateSystem(DJIVirtualStickFlightCoordinateSystem.Body);
                mFlightController.setRollPitchControlMode(DJIVirtualStickRollPitchControlMode.Velocity);
                mFlightController.setVerticalControlMode(DJIVirtualStickVerticalControlMode.Velocity);
                mFlightController.setYawControlMode(DJIVirtualStickYawControlMode.Angle);
                mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                    @Override
                    public void onResult(DJIFlightControllerCurrentState djiFlightControllerCurrentState) {
                        vx = djiFlightControllerCurrentState.getVelocityX();
                        vy = djiFlightControllerCurrentState.getVelocityY();
                        vz = djiFlightControllerCurrentState.getVelocityZ();
                        h = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                        la = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                        lo = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                        if (done && (Math.abs(vx) > 0 || Math.abs(vy) > 0) && carrect == 0) {
                            carrect = carrect_d + (float) getAAndBDsitanceForLongitudeAndLatitude(lo, la, lo_old, la_old);
                        } else if (!done && vx == 0 && vy == 0) {
                            carrect_d = (float) getAAndBDsitanceForLongitudeAndLatitude(lo, la, lo_old, la_old);
                        }
                    }
                });
                aircraft.getBattery().setBatteryStateUpdateCallback(new DJIBattery.DJIBatteryStateUpdateCallback() {
                    @Override
                    public void onResult(DJIBatteryState djiBatteryState) {
                        power = djiBatteryState.getLifetimeRemainingPercent();
                        power = djiBatteryState.getCurrentEnergy() * 100 / djiBatteryState.getFullChargeEnergy();

                    }
                });
            }
        }
    }

    public void showToast(String description) {
        Toast.makeText(context, description, Toast.LENGTH_SHORT).show();
    }

    public double getAAndBDsitanceForLongitudeAndLatitude(double A_Longitude, double A_Latitude, double B_Longitude, double B_Latitude) {

        return Math.abs(6371004 * Math.acos(Math.sin(A_Latitude) * Math.sin(B_Latitude) * Math.cos(A_Longitude - B_Longitude) + Math.cos(A_Latitude) * Math.cos(B_Latitude)) * Math.PI / 180);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setmPitch(float mPitch) {
        this.mPitch = mPitch;
    }

    public void setmRoll(float mRoll) {
        this.mRoll = mRoll;
    }

    public boolean isInitFlightControllered() {
        return aircraft != null && aircraft.isConnected() && mFlightController != null;
    }

    public void takeoff() {
        if (!takeoffing) {
            takeoffing = true;
            if (aircraft != null && aircraft.isConnected() && mFlightController != null)
                mFlightController.takeOff(new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        takeoffing = false;
                        Toast.makeText(context, djiError == null ? "起飞成功" : djiError.getDescription(), Toast.LENGTH_SHORT).show();
                        lo_old = mFlightController.getCurrentState().getAircraftLocation().getLongitude();
                        la_old = mFlightController.getCurrentState().getAircraftLocation().getLatitude();
                    }
                });
            else {
                takeoffing = false;
                showToast("设备未连接");
            }
        } else
            showToast("正在起飞");
    }

    public void land() {
        if (!landing) {
            landing = true;
            if (mFlightController != null) {
                finish();
                mFlightController.autoLanding(new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        landing = false;
                        Toast.makeText(context, djiError == null ? "开始着陆" : djiError.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                landing = false;
                showToast("设备未连接");
            }
        } else
            showToast("正在着陆");
    }

    //工具类使用完成后的善后工作，关闭Timer
    private void finish() {
        done = false;
        mRoll = mPitch = mThrottle = mYaw = 0;
        x = y = 0;
        vx = vy = vz = vz_old = vx_old = vy_old = 0;
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        if (null != hoverTimer) {
            hoverTask.cancel();
            hoverTask = null;
            hoverTimer.cancel();
            hoverTimer.purge();
            hoverTimer = null;
        }
        if (null != runTimer) {
            runTask.cancel();
            runTask = null;
            runTimer.cancel();
            runTimer.purge();
            runTimer = null;
        }
    }

    public void setmYaw(float mYaw) {
        this.mYaw = mYaw;
    }

    public void takePhoto() {
        final DJICamera camera = getCameraInstance();

        if (camera != null) {
            showToast("拍照");
            camera.setDJIUpdateCameraSDCardStateCallBack(new DJICamera.CameraUpdatedSDCardStateCallback() {
                @Override
                public void onResult(CameraSDCardState cameraSDCardState) {
                    long size = cameraSDCardState.getAvailableCaptureCount();
                    if (size < 50) {
                        showToast("无人机内存卡满，请及时清除无人机sd卡无用数据");
                    }
                    if (size < 10) {
                        camera.formatSDCard(new DJICommonCallbacks.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    showToast(djiError.getDescription());
                                } else {
                                    showToast("已成功格式化无人机sd卡");
                                }
                            }
                        });
                    }
                }
            });
            camera.setCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error != null) {
                        showToast(error.getDescription());
                        return;
                    }
                    DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode
                    camera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {
                            order++;
                            setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                            if (error == null) {
                                showToast("拍照成功");
                                Date date = new Date();
                                medias.put(DateFormat.format("yyyy-MM-dd kk:mm:ss", date.getTime()).toString(), new MyMedia(getH(), NAME + order));
                            } else {
                                showToast(error.getDescription());
                                failureMedias.add(new MyMedia(getH(), NAME + order));
                            }
                            action.finish();
                        }
                    });
                }
            });
        }
    }

    public void setCameraMode(final DJICameraSettingsDef.CameraMode cameraMode) {
        DJICamera camera = getCameraInstance();
        if (camera != null) {
            camera.setCameraMode(cameraMode, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                }
            });
        }
    }

    public double getH() {
        return h;
    }

    public ArrayList<MyMedia> getFailureMedias() {
        return failureMedias;
    }

    public HashMap<String, MyMedia> getMedias() {
        return medias;
    }

    public int getPower() {
        return power;
    }

    public double getYaw() {
        return aircraft != null && aircraft.isConnected() && mFlightController != null ? mFlightController.getCurrentState().getAttitude().yaw : 361;
    }

    public double getV() {
        return vz;
    }

    public void setmThrottle(float mThrottle) {
        this.mThrottle = mThrottle;
    }

    public void cancelAction(Action action) {
        if (action.equals(this.action)) {
            finish();
        }
    }

    public void onPause(Action action) {

    }

    public void onResume(Action action) {

    }

    public boolean isFlying() {
        return mFlightController != null && mFlightController.getCurrentState().isFlying();
    }

    public void retakephotos(Set<Double> retakephotos) {
        Iterator<Double> iterable = retakephotos.iterator();
        while (iterable.hasNext()) {
            Double re_h = iterable.next();
            startAction(new Action(re_h, 1, ActionType.UP));
        }
    }

    public void startAction(Action action) {
        if (!prepaerd()) {
            return;
        }
        this.action = action;
        double distance = action.getDistance();
        ActionType type = action.getType();
        this.d = distance / action.getN();
        prepare();
        switch (type) {
            case UP:
                bsa = new BaseStartAction();
                mThrottle = 0.2f;
                break;
            case Down:
                bsa = new BaseStartAction();
                mThrottle = -0.2f;
                break;
            case Left:
                bsa = new BaseStartAction();
                mPitch = -0.2f;
                break;
            case Right:
                bsa = new BaseStartAction();
                mPitch = 0.2f;
                break;
            case Fount:
                bsa = new BaseStartAction();
                mRoll = 0.2f;
                break;
            case Back:
                bsa = new BaseStartAction();
                mRoll = -0.2f;
                break;
        }
    }

    //飞行前的准备工作
    public boolean prepaerd() {
        if (aircraft == null || !aircraft.isConnected() || mFlightController == null) {
            showToast("设备未连接");
            return false;
        }
        if (!mFlightController.getCurrentState().isFlying()) {
            if (aircraft != null && aircraft.isConnected()) {
                showToast("飞行器未起飞");

            } else {
                showToast("设备未连接");
            }
            return false;
        }
        startTimer();
        return !done;
    }

    private void prepare() {
        mPitch = mRoll = mThrottle = 0;
        x = y = z = 0;
        carrect = 0;
        done = true;
    }

    private void startTimer() {
        if (mSendVirtualStickDataTask == null) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 10);
        }
        if (runTimer == null) {
            runTask = new TimerTask() {
                @Override
                public void run() {
                    if (bsa != null) {
                        bsa.actionStart();
                    }
                    y += (vy + vy_old) * 0.005;
                    x += (vx + vx_old) * 0.005;
                    z += (vz + vz_old) * 0.005;
                    vy_old = vy;
                    vx_old = vx;
                    vz_old = vz;
                }
            };
            runTimer = new Timer();
            runTimer.schedule(runTask, 0, 10);
        }
    }

    //飞行控制任务TimerTask
    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (aircraft != null && aircraft.isConnected() && mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new DJICommonCallbacks.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        }
                );
            }
        }
    }

    private abstract class BeforeStartAction {
        abstract void actionStart();
    }

    private class BaseStartAction extends BeforeStartAction {
        private double n;
        private double distance;
        private ActionType type;

        BaseStartAction() {
            type = action.getType();
        }

        @Override
        void actionStart() {
            if (action != null) {
                switch (type) {
                    case UP:
                        distance = z;
                        break;
                    case Down:
                        distance = z;
                        break;
                    default:
                        distance = Math.sqrt(x * x + y * y);
                        break;
                }
                n = Math.abs(distance) / d;
                if (Math.abs(n - action.getN()) < 0.01) {
                    takePhoto();
                    carrect = 0;
                    switch (type) {
                        case UP:
                            mThrottle = 0;
                            break;
                        case Down:
                            mThrottle = 0;
                            break;
                        case Fount:
                            mRoll = 0;
                            break;
                        case Back:
                            mRoll = 0;
                            break;
                        case Right:
                            mPitch = 0;
                            break;
                        case Left:
                            mPitch = 0;
                            break;
                        default:
                            break;
                    }
                    x = y = z = 0;
                    lo_old = mFlightController.getCurrentState().getAircraftLocation().getLongitude();
                    la_old = mFlightController.getCurrentState().getAircraftLocation().getLatitude();
                    done = false;
                    action.finish();
                    action = null;
                    bsa = null;
                } else if (Math.abs(n - action.getStep()) < 0.01) {
                    action.setStep(action.getStep() + 1);
                    takePhoto();
                }
            }
        }
    }
}
