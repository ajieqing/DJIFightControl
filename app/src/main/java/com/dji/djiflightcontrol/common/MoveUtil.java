package com.dji.djiflightcontrol.common;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.camera.DJICamera;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.products.DJIAircraft;

import static com.dji.djiflightcontrol.common.DJISampleApplication.getCameraInstance;

/**
 * 飞行器定距定速飞行控制工具类
 */

public class MoveUtil implements Serializable {
    private double vz, vz_old = 0;
    private double vx, vx_old = 0;
    private double vy, vy_old = 0;
    private double x = 0, y = 0, z = 0;

    private double correct = 0;
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

    private boolean initFlightControllered = false;
    private boolean done = false;
    private boolean hovering = false;
    private boolean takeoffing = false;
    private boolean landing = false;

    private BeforeStartAction bsa;

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 0) {
                if (initFlightControllered) {
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
    private com.dji.djiflightcontrol.common.Timer time;
    private Action action;

    protected MoveUtil() {
        time = new com.dji.djiflightcontrol.common.Timer();
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
        if (!initFlightControllered) {
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
                    if (done) {
                        if (bsa != null) bsa.actionStart();
                        y += (vy + vy_old) * 0.005;
                        x += (vx + vx_old) * 0.005;
                        z += (vz + vz_old) * 0.005;
                    }
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
        return initFlightControllered;
    }

    protected void setInitFlightControllered(boolean initFlightControllered) {
        this.initFlightControllered = initFlightControllered;
    }

    public void takeoff() {
        if (!takeoffing) {
            takeoffing = true;
            if (initFlightControllered)
                mFlightController.takeOff(new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        takeoffing = false;
                        Toast.makeText(context, djiError == null ? "起飞成功" : djiError.getDescription(), Toast.LENGTH_SHORT).show();
                        oldh = mFlightController.getCurrentState().getAircraftLocation().getAltitude();
                    }
                });
            else {
                takeoffing = false;
                showToast("设备未连接");
            }
        } else
            showToast("正在起飞");
    }

    public void showToast(String description) {
        Toast.makeText(context, description, Toast.LENGTH_SHORT).show();
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

    //初始化控制器
    public void initFlightController() {
        DJIAircraft aircraft = DJISampleApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            mFlightController = null;
            showToast("设备链接失败");
        } else {
            mFlightController = aircraft.getFlightController();
            if (mFlightController == null) {
                showToast("设备链接失败");
            } else {
                showToast("设备链接成功");
                setInitFlightControllered(true);
            }
            if (initFlightControllered) {
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
                    }
                });
            }
        }
    }

    public double getD() {
        return Math.sqrt(x * x + y * y);
    }

    public float getmYaw() {
        return mYaw;
    }

    public void setmYaw(float mYaw) {
        this.mYaw = mYaw;
    }

    public void startAction(final Action action) {
        this.action = action;
        this.distance = action.getDistance();
        ActionType type = action.getType();
        if (!prepaerd())
            return;
        prepare();
        switch (type) {
            case UP:
                time.setTimerFinishListener(new TimerFinishListener() {
                    @Override
                    public void onTimeFinish() {
                        z = 0;
                        mThrottle = -0.2f;
                        time.setTimerFinishListener(new TimerFinishListener() {
                            @Override
                            public void onTimeFinish() {
                                done = false;
                                oldh = mFlightController.getCurrentState().getAircraftLocation().getAltitude();
                                hover();
                                action.finish();
                            }
                        });
                        bsa = new BeforeStartAction() {
                            @Override
                            void actionStart() {
                                if (Math.abs(z) >= correct) {
                                    mThrottle = 0;
                                    if (vz == 0 && vz_old == 0) {
                                        time.start(10);
                                    }
                                }
                            }
                        };
                    }
                });
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (Math.abs(z) >= distance + correct) {
                            mThrottle = 0;
                            if (vz == 0 && vz_old == 0) {
                                time.start(10);
                            }
                        } else if (Math.abs(z) >= distance / 3) {
                            if (mThrottle > 0.2f)
                                mThrottle -= 0.1f;
                        } else if (Math.abs(z) >= correct) {
                            if (mThrottle < 3 && mThrottle < distance / 3)
                                mThrottle += 0.1f;
                        }
                    }
                };
                mThrottle = 0.2f;
                oldh = mFlightController.getCurrentState().getAircraftLocation().getAltitude();
                break;
            case Down:
                time.setTimerFinishListener(new TimerFinishListener() {
                    @Override
                    public void onTimeFinish() {
                        z = 0;
                        mThrottle = 0.2f;
                        time.setTimerFinishListener(new TimerFinishListener() {
                            @Override
                            public void onTimeFinish() {
                                done = false;
                                oldh = mFlightController.getCurrentState().getAircraftLocation().getAltitude();
                                hover();
                                action.finish();
                            }
                        });
                        bsa = new BeforeStartAction() {
                            @Override
                            void actionStart() {
                                if (Math.abs(z) >= correct) {
                                    mThrottle = 0;
                                    if (vz == 0 && vz_old == 0) {
                                        time.start(10);
                                    }
                                }
                            }
                        };
                    }
                });
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (Math.abs(z) >= distance + correct) {
                            mThrottle = 0;
                            if (vz == 0 && vz_old == 0) {
                                time.start(10);
                            }
                        } else if (Math.abs(z) >= distance / 3) {
                            if (mThrottle < -0.2f)
                                mThrottle += 0.1f;
                        } else if (Math.abs(z) >= correct) {
                            if (mThrottle > -2.5 && mThrottle > -distance / 3)
                                mThrottle -= 0.1f;
                        }
                    }
                };
                oldh = mFlightController.getCurrentState().getAircraftLocation().getAltitude();
                mThrottle = -0.2f;
                break;
            case Left:
                time.setTimerFinishListener(new TimerFinishListener() {
                    @Override
                    public void onTimeFinish() {
                        x = y = 0;
                        mPitch = 0.2f;
                        time.setTimerFinishListener(new TimerFinishListener() {
                            @Override
                            public void onTimeFinish() {
                                done = false;
                                hover();
                                action.finish();
                            }
                        });
                        bsa = new BeforeStartAction() {
                            @Override
                            void actionStart() {
                                if (x * x + y * y >= correct * correct) {
                                    mPitch = 0;
                                    if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                        time.start(10);
                                    }
                                }
                            }
                        };
                    }
                });
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance + correct) * (distance + correct)) {
                            mPitch = 0;
                            if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                time.start(10);
                            }
                        } else if (x * x + y * y >= (distance / 3) * (distance / 3)) {
                            if (mPitch < -0.2f)
                                mPitch += 0.1f;
                        } else if (x * x + y * y >= correct * correct) {
                            if (mPitch > -5 && mPitch > -distance / 3)
                                mPitch -= 0.1f;
                        }
                    }
                };
                mPitch = -0.2f;
                break;
            case Right:
                time.setTimerFinishListener(new TimerFinishListener() {
                    @Override
                    public void onTimeFinish() {
                        x = y = 0;
                        mPitch = -0.2f;
                        time.setTimerFinishListener(new TimerFinishListener() {
                            @Override
                            public void onTimeFinish() {
                                done = false;
                                hover();
                                action.finish();
                            }
                        });
                        bsa = new BeforeStartAction() {
                            @Override
                            void actionStart() {
                                if (x * x + y * y >= correct * correct) {
                                    mPitch = 0;
                                    if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                        time.start(10);
                                    }
                                }
                            }
                        };
                    }
                });
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance + correct) * (distance + correct)) {
                            mPitch = 0;
                            if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                time.start(10);
                            }
                        } else if (x * x + y * y >= (distance / 3) * (distance / 3)) {
                            if (mPitch > 0.2f)
                                mPitch -= 0.1f;
                        } else if (x * x + y * y >= correct * correct) {
                            if (mPitch < 5 && mPitch < distance / 3)
                                mPitch += 0.1f;
                        }
                    }
                };
                mPitch = 0.2f;
                break;
            case Fount:
                time.setTimerFinishListener(new TimerFinishListener() {
                    @Override
                    public void onTimeFinish() {
                        x = y = 0;
                        mRoll = -0.2f;
                        time.setTimerFinishListener(new TimerFinishListener() {
                            @Override
                            public void onTimeFinish() {
                                done = false;
                                hover();
                                action.finish();
                            }
                        });
                        bsa = new BeforeStartAction() {
                            @Override
                            void actionStart() {
                                if (x * x + y * y >= correct * correct) {
                                    mRoll = 0;
                                    if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                        time.start(10);
                                    }
                                }
                            }
                        };
                    }
                });
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance + correct) * (distance + correct)) {
                            mRoll = 0;
                            if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                time.start(10);
                            }
                        } else if (x * x + y * y >= (distance / 3) * (distance / 3)) {
                            if (mRoll > 0.2f)
                                mRoll -= 0.1f;
                        } else if (x * x + y * y >= correct * correct) {
                            if (mRoll < 5 && mRoll < distance / 3)
                                mRoll += 0.1f;
                        }
                    }
                };
                mRoll = 0.2f;
                break;
            case Back:
                time.setTimerFinishListener(new TimerFinishListener() {
                    @Override
                    public void onTimeFinish() {
                        x = y = 0;
                        mRoll = 0.2f;
                        time.setTimerFinishListener(new TimerFinishListener() {
                            @Override
                            public void onTimeFinish() {
                                done = false;
                                hover();
                                action.finish();
                            }
                        });
                        bsa = new BeforeStartAction() {
                            @Override
                            void actionStart() {
                                if (x * x + y * y >= correct * correct) {
                                    mRoll = 0;
                                    if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                        time.start(10);
                                    }
                                }
                            }
                        };
                    }
                });
                bsa = new BeforeStartAction() {
                    @Override
                    void actionStart() {
                        if (x * x + y * y >= (distance + correct) * (distance + correct)) {
                            mRoll = 0;
                            if (vx == 0 && vx_old == 0 && vy == 0 && vy_old == 0) {
                                time.start(10);
                            }
                        } else if (x * x + y * y >= (distance / 3) * (distance / 3)) {
                            if (mRoll < -0.2f)
                                mRoll += 0.1f;
                        } else if (x * x + y * y >= correct * correct) {
                            if (mRoll > -5 && mRoll > -distance / 3)
                                mRoll -= 0.1f;
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
        done = true;
        stopHover();
    }

    public void hover() {
        lo_old = lo;
        la_old = la;
        hovering = true;
        if (hoverTimer == null) {
            hoverTimer = new Timer();
            hoverTask = new TimerTask() {
                @Override
                public void run() {
                    if (hovering) {
                        double d = h - oldh;
                        if (d < 0) {
                            mThrottle = 0.2f;
                        } else if (d > 0) {
                            mThrottle = -0.2f;
                        } else {
                            mThrottle = 0;
                        }
                        double yaw = getYaw(), dy = getAAndBDsitanceForLongitudeAndLatitude(lo, 0, lo_old, 0), dx = getAAndBDsitanceForLongitudeAndLatitude(0, la, 0, la_old);
                        if (Math.abs(dy) > Math.abs(dx)) {
                            dx = (float) (0.2 * dx / dy);
                            dy = 0.2f;
                        } else {
                            if (dx != 0) {
                                dy = (float) (0.2 * dy / dx);
                                dx = 0.2f;
                            }
                        }
                        mPitch = -(float) (dy * Math.cos(yaw) - dx * Math.sin(yaw));
                        mRoll = -(float) (dy * Math.sin(yaw) + dx * Math.cos(yaw));
                        mRoll = matchValue(mRoll);
                        mPitch = matchValue(mPitch);
                    }
                }

                private float matchValue(float value) {
                    if (Math.abs(value) - 0.5 < 0)
                        value = 0;
                    else if (value < 0.1f && value > 0.5)
                        value = 0.1f;
                    else if (value > -0.1f && value < -0.5)
                        value = -0.1f;
                    return value;
                }
            };
            hoverTimer.schedule(hoverTask, 0, 20);
        }
    }

    public void takePhoto() {
        DJICamera camera = getCameraInstance();
        if (camera != null) {
            setCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto);
            DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode
            camera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                    if (error == null) {
                        showToast("拍照成功");
                    } else {
                        showToast(error.getDescription());
                    }
                    action.finish();
                }
            });
        }
    }

    private void stopHover() {
        hovering = false;
        correct = 0.4f;
    }

    public double getYaw() {
        return initFlightControllered ? mFlightController.getCurrentState().getAttitude().yaw : 361;
    }

    public double getAAndBDsitanceForLongitudeAndLatitude(double A_Longitude, double A_Latitude, double B_Longitude, double B_Latitude) {

        return Math.abs(6371004 * Math.acos(Math.sin(A_Latitude) * Math.sin(B_Latitude) * Math.cos(A_Longitude - B_Longitude) + Math.cos(A_Latitude) * Math.cos(B_Latitude)) * Math.PI / 180);
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

    public String getPower() {
        DJIAircraft aircraft = DJISampleApplication.getAircraftInstance();
        String power;
        if (aircraft != null)
            power = String.valueOf(aircraft.getBattery().getNumberOfCells() + "%");
        else
            power = "未知";
        return power;
    }

    public String getPosition() {

        double yaw = getYaw();
        String position;
        if (yaw > -22.5 && yaw < 22.5)
            position = "北";
        else if (yaw >= 22.5 && yaw <= 67.5)
            position = "东北";
        else if (yaw >= 67.5 && yaw <= 112.5)
            position = "东";
        else if (yaw >= 112.5 && yaw <= 157.5)
            position = "东南";
        else if (yaw >= 157.5 && yaw <= 180 || yaw >= -180 && yaw <= -157.5)
            position = "东";
        else if (yaw >= -157.5 && yaw <= -112.5)
            position = "西南";
        else if (yaw >= -112.5 && yaw <= -67.5)
            position = "西";
        else if (yaw >= -67.5 && yaw <= -22.5)
            position = "东北";
        else position = "未知";
        return position;
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

    //飞行控制任务TimerTask
    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (initFlightControllered) {
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
