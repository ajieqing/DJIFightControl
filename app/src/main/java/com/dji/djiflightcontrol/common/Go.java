package com.dji.djiflightcontrol.common;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.djiflightcontrol.R;

import java.util.Timer;
import java.util.TimerTask;

public class Go extends Activity {
    private Timer timer;
    private TimerTask timerTask;
    private com.dji.djiflightcontrol.common.Timer t;
    private MoveUtil1 util;
    private boolean canGo = false;
    private int step = 4;
    private float dis = 15f;
    private int state = 1;
    private TextView textView, textView1;
    private TextView textView2;
    private double oldLA, oldLo;
    private TextView textView3;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            double d = 0;
            if (util.isInitFlightControllered()) {
                double la = util.getmFlightController().getCurrentState().getAircraftLocation().getLatitude();
                double lo = util.getmFlightController().getCurrentState().getAircraftLocation().getLongitude();
                d = util.getAAndBDsitanceForLongitudeAndLatitude(lo, la, oldLo, oldLA);
            }
            if (msg.what == 1) {
                textView.setText("GPS=:" + new java.text.DecimalFormat("#.000").format(d));
                textView1.setText("积分=" + new java.text.DecimalFormat("#.000").format(util.getD()));
            } else if (msg.what == 0) {
                if (!canGo) {
                    Toast.makeText(Go.this, "完成", Toast.LENGTH_SHORT).show();
                }
            } else if (msg.what == 2) {
                if (util.isInitFlightControllered()) {
                    textView2.setText("误差=" + new java.text.DecimalFormat("#.000").format(Math.abs(util.getD() - d)));
                    textView3.setText("偏差=" + new java.text.DecimalFormat("#.000").format(Math.abs(util.getV())));
                }
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.go);
        textView = (TextView) findViewById(R.id.text);
        textView1 = (TextView) findViewById(R.id.text1);
        textView2 = (TextView) findViewById(R.id.text2);
        textView3 = (TextView) findViewById(R.id.text3);
        util = DJISampleApplication.util;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(2);
            }
        }, 0, 100);
        t = new com.dji.djiflightcontrol.common.Timer();
        t.setTimerFinishListener(new com.dji.djiflightcontrol.common.TimerFinishListener() {
            @Override
            public void onTimeFinish() {
                oldLA = util.getmFlightController().getCurrentState().getAircraftLocation().getLatitude();
                oldLo = util.getmFlightController().getCurrentState().getAircraftLocation().getLongitude();
                step = 1;
                state--;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        util.finish();
        if (timer != null) {
            timerTask.cancel();
            timerTask = null;
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public void reset(View v) {
        if (util.prepaerd()) {
            util.setmYaw(0);
            util.turn(0);
        }
    }

    public void turn(View v) {
        util.turn(5);
    }

    public void takeoff(View v) {
        util.takeoff();
    }

    public void land(View v) {
        canGo = false;
        if (null != timer) {
            timerTask.cancel();
            timerTask = null;
            timer.cancel();
            timer.purge();
            timer = null;
        }
        util.land();
    }

    public void connect(View v) {
        if (util.isInitFlightControllered())
            Toast.makeText(this, "设备已连接", Toast.LENGTH_SHORT).show();
        else
            util.initFlightController();
    }

    public void go(View view) {
        if (!util.prepaerd()) {
            return;
        }

        if (canGo) {
            Toast.makeText(this, "任务正在执行", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, "任务开始始执行", Toast.LENGTH_SHORT).show();
        }
        step = 0;
        state = 1;
        canGo = true;
        oldLA = util.getmFlightController().getCurrentState().getAircraftLocation().getLatitude();
        oldLo = util.getmFlightController().getCurrentState().getAircraftLocation().getLongitude();
        if (timer == null) {
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Action action;
                    handler.sendEmptyMessage(1);
                    if (canGo) {
                        switch (step) {
                            case 0:
                                action = new Action(dis, 10, ActionType.Fount);
                                util.startAction(action);
                                step = 6;
                                break;
                            case 1:
                                action = new Action(dis, 10, ActionType.Back);
                                util.startAction(action);
                                step = 6;
                                break;
                            case 2:
                                action = new Action(dis, ActionType.Right);
                                util.startAction(action);
                                step = 6;
                                break;
                            case 3:
                                action = new Action(dis, ActionType.Left);
                                util.startAction(action);
                                step = 6;
                                break;
                            case 4:
                                action = new Action(dis, ActionType.UP);
                                util.startAction(action);
                                step = 6;
                                break;
                            case 5:
                                action = new Action(dis, ActionType.Down);
                                util.startAction(action);
                                step = 6;
                                break;
                            case 6:
                                if (!util.prepaerd()) {
                                    break;
                                }
                                if (state == 0) {
                                    canGo = false;
                                    util.setmRoll(0);
                                    util.setmPitch(0);
                                    util.setmThrottle(0);
                                    handler.sendEmptyMessage(0);
                                    break;
                                }
                                t.start(0);
                                break;
                            default:
                                break;
                        }
                    }
                }
            };
            timer.schedule(timerTask, 0, 100);
        }
    }
}
