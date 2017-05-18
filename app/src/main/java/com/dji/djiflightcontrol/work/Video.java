package com.dji.djiflightcontrol.work;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.common.Action;
import com.dji.djiflightcontrol.common.ActionType;
import com.dji.djiflightcontrol.common.DJISampleApplication;
import com.dji.djiflightcontrol.common.MoveUtil1;
import com.dji.djiflightcontrol.common.OnActionFinishListener;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.camera.DJICamera;
import dji.sdk.codec.DJICodecManager;

import static com.dji.djiflightcontrol.common.DJISampleApplication.getCameraInstance;


/**
 * 录像控制activity
 */

public class Video extends Activity implements View.OnClickListener, TextureView.SurfaceTextureListener {
    private MoveUtil1 util;
    private float mYaw, mThrottle, mPitch, mRoll;
    private DJICodecManager mCodecManager;
    private TextView tv_v, tv_h, tv_y, tv_battry, tv_flight_mode;
    private ImageButton ib_forward, ib_backward, ib_leftward, ib_rightward, ib_turnleft, ib_turnright, ib_rise, ib_fall, ib_takeoff, ib_switch, ib_menu, ib_visible;
    private ProgressBar pb_battry;
    private ImageView iv_compass_hand;
    private Timer timer;
    private TimerTask timerTask;
    private TextToSpeech speech;
    private Boolean isVisible = false;
    private boolean flightmode = true;
    private boolean isAutoDownload = false;
    private TextView flightstate;
    private DecimalFormat format = new DecimalFormat("0.0");
    private Timer speechtimer;
    private TimerTask speechtimertask;
    private DJICameraSettingsDef.CameraOpticalZoomSpec spec;
    private Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("NewApi")
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(Video.this, "任务完成", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(Video.this, "单步任务完成", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    tv_v.setText(String.valueOf(format.format(util.getV())));
                    tv_h.setText(String.valueOf(format.format(util.getH())));
                    tv_y.setText(String.valueOf(format.format(util.getYaw())));
                    iv_compass_hand.setRotation((float) util.getYaw());
                    int power = util.getPower();
                    pb_battry.setProgress(power);
                    if (power > 60) {
                        pb_battry.setProgressDrawable(getResources().getDrawable(R.drawable.power_full));
                    } else if (power > 30) {
                        pb_battry.setProgressDrawable(getResources().getDrawable(R.drawable.power_medium));
                        if (timer != null) {
                            timer.purge();
                            timer.cancel();
                            timer = null;
                            speechtimertask.cancel();
                            speechtimertask = null;
                        }
                    } else {
                        pb_battry.setProgressDrawable(getResources().getDrawable(R.drawable.power_low));
                        if (speech == null) {
                            speech = new TextToSpeech(Video.this, new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    if (status == TextToSpeech.SUCCESS) {
                                        speech.setLanguage(Locale.CHINESE);
                                    }
                                }
                            });
                        }
                        if (speechtimer == null) {
                            speechtimer = new Timer();
                            speechtimertask = new TimerTask() {
                                @Override
                                public void run() {
                                    if (util.isInitFlightControllered())
                                        speech.speak("电量过低", TextToSpeech.QUEUE_FLUSH, null, "power lower");
                                }
                            };
                            speechtimer.schedule(speechtimertask, 1000);
                        }
                    }
                    tv_battry.setText(power + "%");
                    if (flightstate != null) {
                        if (util.isInitFlightControllered()) {
                            flightstate.setText("飞行器-已连接");
                        } else {
                            flightstate.setText("飞行器-未连接");
                        }
                    }
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_visible:
                isVisible = !isVisible;
                showView();
                break;
            case R.id.ib_switch:
                flightmode = !flightmode;
                if (isVisible) {
                    giveFunction();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            start();
                        }
                    }).start();
                }
                break;
            case R.id.ib_takeoff:
                if (util.isInitFlightControllered()) {
                    if (util.isFlying()) {
                        util.land();
                    } else {
                        util.takeoff();
                    }
                } else {
                    util.showToast("飞行器未连接或者设备未初始化");
                }
                break;
            case R.id.ib_menu:
                ib_menu.setImageDrawable(getResources().getDrawable(R.drawable.menu_selected));
                showMenu();
                break;
        }
    }

    private void start() {
        Action action = new Action(Setting.HIGH, Setting.N, ActionType.UP);
        action.start();
        action.setOnActionFinishListener(new OnActionFinishListener() {
            @Override
            public void onActionFinish() {
                util.land();
            }
        });
    }

    private void showMenu() {
        Dialog dialog = new Dialog(this, R.style.dialog);
        LinearLayout ly = (LinearLayout) getLayoutInflater().inflate(R.layout.menu, null);
        dialog.setContentView(ly);
        WindowManager.LayoutParams wl = dialog.getWindow().getAttributes();
        int[] location = new int[2];
        ib_menu.getLocationInWindow(location);
        wl.x = location[0] - 300;
        wl.y = location[1];
        dialog.getWindow().setAttributes(wl);
        dialog.show();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ib_menu.setImageDrawable(getResources().getDrawable(R.drawable.menu_normal));
            }
        });
        flightstate = (TextView) ly.findViewById(R.id.tv_flight_state);
        if (util.isInitFlightControllered()) {
            flightstate.setText("飞行器-已连接");
        } else {
            flightstate.setText("飞行器-未连接");
        }
        flightstate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (util.isInitFlightControllered()) {
                    util.showToast("飞行器已连接");
                } else {
                    util.initFlightController();
                }
            }
        });
//        final TextView textView1 = (TextView) ly.findViewById(R.id.tv_download);
//        if (isAutoDownload) {
//            textView1.setText("自动下载-开");
//        } else {
//            textView1.setText("自动下载-关");
//        }
        LinearLayout ly_task = (LinearLayout) ly.findViewById(R.id.ly_task);
        ly_task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        LinearLayout ly_album = (LinearLayout) ly.findViewById(R.id.ly_album);
        ly_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Video.this, Photos.class);
                startActivity(intent);
            }
        });
        LinearLayout ly_setting = (LinearLayout) ly.findViewById(R.id.ly_setting);
        ly_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Video.this, Setting.class);
                startActivity(intent);
            }
        });
        LinearLayout ly_help = (LinearLayout) ly.findViewById(R.id.ly_help);
        ly_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Video.this, Help.class);
                startActivity(intent);
            }
        });
        LinearLayout ly_developer = (LinearLayout) ly.findViewById(R.id.ly_developer);
        ly_developer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Video.this, AboutOurs.class);
                startActivity(intent);
            }
        });
//        LinearLayout ly_download = (LinearLayout) ly.findViewById(R.id.ly_download);
//        ly_download.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                isAutoDownload = !isAutoDownload;
//                if (isAutoDownload) {
//                    textView1.setText("自动下载-开");
//                } else {
//                    textView1.setText("自动下载-关");
//                }
//            }
//        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isVisible", isVisible);
        outState.putBoolean("flightmode", flightmode);
        outState.putBoolean("isAutoDownload", isAutoDownload);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        isVisible = savedInstanceState.getBoolean("isVisible");
        flightmode = savedInstanceState.getBoolean("flightmode");
        isAutoDownload = savedInstanceState.getBoolean("isAutoDownload");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        util = DJISampleApplication.util;
        initView();
        if (timer == null) {
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(2);
                }
            };
            timer.schedule(timerTask, 0, 10);
        }
    }

    private void initView() {
        setContentView(R.layout.video);
        /*
        if (getAircraftInstance() != null && getAircraftInstance().isConnected() && getAircraftInstance().getGimbal() != null) {
            DJIGimbal gimbal = getAircraftInstance().getGimbal();
            gimbal.setGimbalControllerMode(DJIGimbalControllerMode.Free, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
            gimbal.setGimbalWorkMode(DJIGimbalWorkMode.FreeMode, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
            gimbal.resetGimbal(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }*/
        getView();
        final DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = new DJICamera.CameraReceivedVideoDataCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        if (getCameraInstance() != null) {
            util.setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
            getCameraInstance().setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
            /*
            getCameraInstance().setLensFocusMode(DJICameraSettingsDef.CameraLensFocusMode.Auto, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
            */
        }
        TextureView videoPlayer = (TextureView) findViewById(R.id.videoplayer);
        videoPlayer.setSurfaceTextureListener(this);
        ib_switch.setOnClickListener(this);
        ib_takeoff.setOnClickListener(this);
        ib_menu.setOnClickListener(this);
        ib_visible.setOnClickListener(this);
        showView();
    }

    public void getView() {
        tv_battry = (TextView) findViewById(R.id.tv_battry_show);
        tv_h = (TextView) findViewById(R.id.tv_height_show);
        tv_v = (TextView) findViewById(R.id.tv_velocity_show);
        tv_y = (TextView) findViewById(R.id.tv_direction_show);
        tv_flight_mode = (TextView) findViewById(R.id.mode);
        iv_compass_hand = (ImageView) findViewById(R.id.iv_compass_hand);
        pb_battry = (ProgressBar) findViewById(R.id.pb_battry);
        ib_rise = (ImageButton) findViewById(R.id.ib_rise);
        ib_fall = (ImageButton) findViewById(R.id.ib_fall);
        ib_turnleft = (ImageButton) findViewById(R.id.ib_turnleft);
        ib_turnright = (ImageButton) findViewById(R.id.ib_turnright);
        ib_forward = (ImageButton) findViewById(R.id.ib_forward);
        ib_backward = (ImageButton) findViewById(R.id.ib_backward);
        ib_leftward = (ImageButton) findViewById(R.id.ib_leftward);
        ib_rightward = (ImageButton) findViewById(R.id.ib_rigthward);
        ib_menu = (ImageButton) findViewById(R.id.ib_menu);
        ib_switch = (ImageButton) findViewById(R.id.ib_switch);
        ib_takeoff = (ImageButton) findViewById(R.id.ib_takeoff);
        ib_visible = (ImageButton) findViewById(R.id.ib_visible);
    }

    private void showView() {
        if (isVisible) {
            ib_forward.setVisibility(View.VISIBLE);
            ib_backward.setVisibility(View.VISIBLE);
            ib_leftward.setVisibility(View.VISIBLE);
            ib_rightward.setVisibility(View.VISIBLE);
            ib_rise.setVisibility(View.VISIBLE);
            ib_fall.setVisibility(View.VISIBLE);
            ib_turnleft.setVisibility(View.VISIBLE);
            ib_turnright.setVisibility(View.VISIBLE);
            ib_switch.setImageDrawable(getResources().getDrawable(R.drawable.switch_button));
            ib_visible.setImageDrawable(getResources().getDrawable(R.drawable.visible_button));
            giveFunction();
        } else {
            ib_forward.setVisibility(View.INVISIBLE);
            ib_backward.setVisibility(View.INVISIBLE);
            ib_leftward.setVisibility(View.INVISIBLE);
            ib_rightward.setVisibility(View.INVISIBLE);
            ib_rise.setVisibility(View.INVISIBLE);
            ib_fall.setVisibility(View.INVISIBLE);
            ib_turnleft.setVisibility(View.INVISIBLE);
            ib_turnright.setVisibility(View.INVISIBLE);
            ib_switch.setImageDrawable(getResources().getDrawable(R.drawable.start));
            ib_visible.setImageDrawable(getResources().getDrawable(R.drawable.invisible_button));
            tv_flight_mode.setText("自动模式");
        }
    }

    private void giveFunction() {
        if (flightmode) {
            ib_forward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mRoll = mPitch = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mPitch = 0;
                            mRoll = 0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_backward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mRoll = mPitch = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mPitch = 0;
                            mRoll = -0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_leftward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mRoll = mPitch = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mRoll = 0;
                            mPitch = -0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_rightward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mRoll = mPitch = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mRoll = 0;
                            mPitch = 0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_rise.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mThrottle = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mThrottle = -0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_fall.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mThrottle = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mThrottle = 0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_turnleft.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mYaw -= 1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_turnright.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mYaw += 1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            tv_flight_mode.setText("飞行模式");
        } else {
            ib_forward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            ib_backward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            ib_leftward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            if (spec == null) if (getCameraInstance() != null) {
                                getCameraInstance().getOpticalZoomSpec(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraOpticalZoomSpec>() {
                                    @Override
                                    public void onSuccess(DJICameraSettingsDef.CameraOpticalZoomSpec cameraOpticalZoomSpec) {
                                        spec = cameraOpticalZoomSpec;
                                    }

                                    @Override
                                    public void onFailure(DJIError djiError) {

                                    }
                                });
                            }
                            if (getCameraInstance() != null && spec != null) {
                                getCameraInstance().getOpticalZoomFocalLength(new DJICommonCallbacks.DJICompletionCallbackWith<Integer>() {
                                    @Override
                                    public void onSuccess(Integer integer) {
                                        util.showToast(integer.toString());
                                        int length = integer + spec.focalLengthStep;
                                        if (length > spec.maxFocalLength) {
                                            length = spec.maxFocalLength;
                                        }
                                        util.showToast(length + "length");
                                        getCameraInstance().setOpticalZoomFocalLength(length, new DJICommonCallbacks.DJICompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (djiError != null)
                                                    util.showToast(djiError.getDescription());
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(DJIError djiError) {

                                    }
                                });
                            }
                            break;
                    }
                    return true;
                }
            });
            ib_rightward.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            if (spec == null)
                                if (getCameraInstance() != null) {
                                    getCameraInstance().getOpticalZoomSpec(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraOpticalZoomSpec>() {
                                        @Override
                                        public void onSuccess(DJICameraSettingsDef.CameraOpticalZoomSpec cameraOpticalZoomSpec) {
                                            spec = cameraOpticalZoomSpec;
                                        }

                                        @Override
                                        public void onFailure(DJIError djiError) {

                                        }
                                    });
                                }
                            if (getCameraInstance() != null && spec != null) {
                                getCameraInstance().getOpticalZoomFocalLength(new DJICommonCallbacks.DJICompletionCallbackWith<Integer>() {
                                    @Override
                                    public void onSuccess(Integer integer) {
                                        int length = integer - spec.focalLengthStep;
                                        if (length < spec.minFocalLength) {
                                            length = spec.minFocalLength;
                                        }
                                        getCameraInstance().setOpticalZoomFocalLength(length, new DJICommonCallbacks.DJICompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {

                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(DJIError djiError) {

                                    }
                                });
                            }
                            break;
                    }
                    return true;
                }
            });
            ib_rise.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mThrottle = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mThrottle = -0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_fall.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mThrottle = 0;
                            updateUtil();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            mThrottle = 0.1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_turnleft.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mYaw -= 1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            ib_turnright.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mYaw += 1f;
                            updateUtil();
                            break;
                    }
                    return true;
                }
            });
            tv_flight_mode.setText("相机模式");
        }

    }

    private void updateUtil() {
        if (!util.prepaerd()) return;
        util.setmRoll(mRoll);
        util.setmPitch(mPitch);
        util.setmThrottle(mThrottle);
        util.setmYaw(mYaw);
    }

    @Override
    protected void onPause() {
        if (timer != null) {
            timerTask.cancel();
            timerTask = null;
            timer.cancel();
            timer.purge();
            timer = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        DJICamera camera = getCameraInstance();
        if (camera != null) {
            camera.setDJICameraReceivedVideoDataCallback(null);
        }
        if (speech != null) {
            speech.stop();
            speech.shutdown();
        }
        super.onDestroy();

    }
}