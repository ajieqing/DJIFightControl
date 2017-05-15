package com.dji.djiflightcontrol.common;

/**
 * Created by �? on 2016/11/3.
 */

public class Timer {
    private boolean started = false;
    private TimerFinishListener mTimerFinishListener = null;

    public Timer() {
    }

    public void setTimerFinishListener(TimerFinishListener mTimerFinishListener) {
        this.mTimerFinishListener = mTimerFinishListener;
    }

    public void start(final long milliontime) {
        if (!started) {
            started = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(milliontime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (this) {
                        if (mTimerFinishListener != null)
                            mTimerFinishListener.onTimeFinish();
                        started = false;
                    }
                }
            }).start();

        }
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }
}
