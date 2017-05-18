package com.dji.djiflightcontrol.common;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.battery.DJIBatteryState;
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

import static com.dji.djiflightcontrol.common.DJISampleApplication.getAircraftInstance;
import static com.dji.djiflightcontrol.common.DJISampleApplication.getCameraInstance;

/**
 * 飞行器定距定速飞行控制工具类
 */

public class MoveUtil1 {
    private static MoveUtil1 util = new MoveUtil1();
    private final HashMap<Integer, MyMedio> medios = new HashMap<>();
    private ArrayList<MyMedio> failureMedios = new ArrayList<>();
    private double vz, vz_old = 0;
    private double vx, vx_old = 0;
    private double vy, vy_old = 0;
    private double x = 0, y = 0, z = 0;
    private float distance;
    private float mPitch = 0;
    private float mRoll = 0;
    private float mYaw = 0;
    private float mThrottle = 0;
    private DJIFlightController mFlightController;
    private Context context;
    private double oldh = 0;
    private double la, lo, la_old, lo_old, h;
    private Timer mSendVirtualStickDataTimer, hoverTimer, runTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private TimerTask hoverTask, runTask;
    private DJIAircraft aircraft = getAircraftInstance();
    private boolean done = false;
    private boolean hovering = false;
    private boolean takeoffing = false;
    private boolean landing = false;
    private BeforeStartAction bsa;

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 0) {
                if (aircraft != null && aircraft.isConnected()) {
                    showToast("飞行器未起飞");
                } else {
                    showToast("设备未连接");
                }
            } else if (msg.what == 1) {
                showToast("设备未连接");
            }
            return false;
        }
    });
    private Action action;
    private float carrect = 0, carrect_d;
    private int power;
    private int order = 0;

    private MoveUtil1() {
    }

    public static MoveUtil1 getUtil() {
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

    //逆时针旋旋转
    public void turnToNegative(float angle) {
        if (!prepaerd()) return;
        mYaw -= angle;
        if (mYaw < -180) {
            mYaw = -180;
        }
    }

    //飞行前的准备工作
    public boolean prepaerd() {
        if (aircraft == null || !aircraft.isConnected() || mFlightController == null) {
            handler.sendEmptyMessage(1);
            return false;
        }
        if (!mFlightController.getCurrentState().isFlying()) {
            handler.sendEmptyMessage(0);
            return false;
        }
        startTimer();
        return !done;
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
                    if (bsa != null) bsa.actionStart();
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

    //顺时针旋旋转
    public void turn(float angle) {
        if (!prepaerd()) return;
        mYaw += angle;
        if (mYaw > 180) {
            mYaw = 180;
        }
    }

    protected DJIFlightController getmFlightController() {
        return mFlightController;
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
                        oldh = mFlightController.getCurrentState().getAircraftLocation().getAltitude();
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
                        Toast.makeText(context, djiError == null ? "着陆完成" : djiError.getDescription(), Toast.LENGTH_SHORT).show();
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
    void finish() {
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

    public double getD() {
        return Math.sqrt(x * x + y * y) + carrect;
    }

    public float getmYaw() {
        return mYaw;
    }

    public void setmYaw(float mYaw) {
        this.mYaw = mYaw;
    }

    public void startAction(final Action action) {
        if (!prepaerd()) {
            return;
        }
        this.action = action;
        this.distance = action.getDistance();
        ActionType type = action.getType();
        final float d = distance / action.getN();
        prepare();
        switch (type) {
            case UP:
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (Math.abs(z) >= distance) {
                            takePhoto();
                            done = false;
                        }
                    }
                };
                mThrottle = 0.2f;
                break;
            case Down:
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (Math.abs(z) >= distance) {
                            takePhoto();
                            done = false;
                        }
                    }
                };
                mThrottle = -0.2f;
                break;
            case Left:
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance) * (distance)) {
                            takePhoto();
                            done = false;
                        }
                    }
                };
                mPitch = -0.2f;
                break;
            case Right:
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance) * (distance)) {
                            takePhoto();
                            done = false;
                        }
                    }
                };
                mPitch = 0.2f;
                break;
            case Fount:
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance) * (distance)) {
                            takePhoto();
                            carrect = 0;
                            mRoll = 0;
                            x = y = z = 0;
                            lo_old = mFlightController.getCurrentState().getAircraftLocation().getLongitude();
                            la_old = mFlightController.getCurrentState().getAircraftLocation().getLatitude();
                            done = false;
                        } else {
                            if (action.getStep() == 0 && x * x + y * y >= (d - carrect) * (d - carrect)) {
                                takePhoto();
                                action.setStep(action.getStep() + 1);
                            } else if (action.getStep() < action.getN() - 1 && x * x + y * y >= (d * action.getStep() + d - carrect) * (d * action.getStep() + d - carrect)) {
                                takePhoto();
                                action.setStep(action.getStep() + 1);
                            }
                        }
                    }
                };
                mRoll = 0.2f;
                break;
            case Back:
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance) * (distance)) {
                            takePhoto();
                            carrect = 0;
                            x = y = z = 0;
                            lo_old = mFlightController.getCurrentState().getAircraftLocation().getLongitude();
                            la_old = mFlightController.getCurrentState().getAircraftLocation().getLatitude();
                            mRoll = 0;
                            done = false;
                        } else {
                            if (action.getStep() == 0 && x * x + y * y >= (d - carrect) * (d - carrect)) {
                                takePhoto();
                                action.setStep(action.getStep() + 1);
                            } else if (action.getStep() < action.getN() - 1 && x * x + y * y >= (d * action.getStep() + d - carrect) * (d * action.getStep() + d - carrect)) {
                                takePhoto();
                                action.setStep(action.getStep() + 1);
                            }
                        }
                    }
                };
                mRoll = -0.2f;
                break;
            default:
                takePhoto();
                break;
        }
    }

    private void prepare() {
        mPitch = mRoll = mThrottle = 0;
        x = y = z = 0;
        carrect = 0;
        done = true;
    }

    public void takePhoto() {
        final DJICamera camera = getCameraInstance();
        if (camera != null) {
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
                            setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                            if (error == null) {
                                showToast("拍照成功");
//                                medios.put(order++, new MyMedio(getH(), la, lo, Setting.NAME + order));
                            } else {
                                showToast(error.getDescription());
//                                failureMedios.add(new MyMedio(getH(), la, lo, Setting.NAME));
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

    public ArrayList<MyMedio> getFailureMedios() {
        return failureMedios;
    }

    public HashMap<Integer, MyMedio> getMedios() {
        return medios;
    }

    public int getPower() {
        return power;
    }

    public double getYaw() {
        return aircraft != null && aircraft.isConnected() && mFlightController != null ? mFlightController.getCurrentState().getAttitude().yaw : 361;
    }

    public double getV() {
        return Math.sqrt(vx * vx + vy * vy);
    }

    public double getmThrottle() {
        return mThrottle;
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

}
