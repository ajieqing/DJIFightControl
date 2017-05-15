package com.dji.djiflightcontrol.common;

import com.dji.djiflightcontrol.R;

/**
 * 飞行器执行的动作
 */
public class Action{
    private float distance;             //飞行器移动距离
    private ActionType type;            //动作类型
    private int id;                     //动作对应图片资源的id
    private OnActionFinishListener listener;//飞行器动作完成监听器
    private int n = 0;                      //拍照次数
    private MoveUtil1 util;
    private int step = 0;

    public Action(float distance, ActionType type) {
        this.distance = distance;
        this.type = type;
        this.util = DJISampleApplication.util;
        switch (type) {
            case UP:
                id = R.drawable.up;
                break;
            case Down:
                id = R.drawable.down;
                break;
            case Left:
                id = R.drawable.left;
                break;
            case Right:
                id = R.drawable.right;
                break;
            case Fount:
                id = R.drawable.fount;
                break;
            case Back:
                id = R.drawable.back;
                break;
            default:
                break;
        }
    }

    public Action(float distance, int n, ActionType type) {
        this.n = n;
        this.distance = distance;
        this.type = type;
        this.util = DJISampleApplication.util;
        switch (type) {
            case UP:
                id = R.drawable.up;
                break;
            case Down:
                id = R.drawable.down;
                break;
            case Left:
                id = R.drawable.left;
                break;
            case Right:
                id = R.drawable.right;
                break;
            case Fount:
                id = R.drawable.fount;
                break;
            case Back:
                id = R.drawable.back;
                break;
            default:
                break;
        }
    }

    public void setOnActionFinishListener(OnActionFinishListener listener) {
        this.listener = listener;
    }

    //动作完成时调用
    public void finish() {
        if (listener != null)
            listener.onActionFinish();
    }

    public float getDistance() {
        return distance;
    }

    public int getId() {
        return id;
    }

    public ActionType getType() {
        return type;
    }

    public void start() {
        setStep(0);
        util.startAction(this);
    }

    public void cancel() {
        util.cancelAction(this);
    }

    public void onPause() {
        util.onPause(this);
    }

    public void onResume() {
        util.onResume(this);
    }

    public int getN() {
        return n;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
