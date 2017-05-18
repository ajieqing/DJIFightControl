package com.dji.djiflightcontrol.welocme;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;

class MoveGestureDetector extends BaseGestureDetector {

    //用于记录最终结果，并返回
    private PointF mExtenalPointer = new PointF();

    private OnMoveGestureListener mListenter;


    MoveGestureDetector(Context context, OnMoveGestureListener listener) {
        super(context);
        mListenter = listener;
    }

    @Override
    protected void handleInProgressEvent(MotionEvent event) {
        int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
        switch (actionCode) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mListenter.onMoveEnd(this);
                resetState();
                break;
            case MotionEvent.ACTION_MOVE:
                updateStateByEvent(event);
                boolean update = mListenter.onMove(this);
                if (update) {
                    mPreMotionEvent.recycle();
                    mPreMotionEvent = MotionEvent.obtain(event);
                }
                break;

        }
    }

    protected void updateStateByEvent(MotionEvent event) {
        final MotionEvent prev = mPreMotionEvent;

        PointF mPrePointer = caculateFocalPointer(prev);
        PointF mCurrentPointer = caculateFocalPointer(event);

        //Log.e("TAG", mPrePointer.toString() + " ,  " + mCurrentPointer);

        boolean mSkipThisMoveEvent = prev.getPointerCount() != event.getPointerCount();

        //Log.e("TAG", "mSkipThisMoveEvent = " + mSkipThisMoveEvent);
        mExtenalPointer.x = mSkipThisMoveEvent ? 0 : mCurrentPointer.x - mPrePointer.x;
        mExtenalPointer.y = mSkipThisMoveEvent ? 0 : mCurrentPointer.y - mPrePointer.y;

    }

    /**
     * 根据event计算多指中心点
     */
    private PointF caculateFocalPointer(MotionEvent event) {
        final int count = event.getPointerCount();
        float x = 0, y = 0;
        for (int i = 0; i < count; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }

        x /= count;
        y /= count;

        return new PointF(x, y);
    }

    @Override
    protected void handleStartProgressEvent(MotionEvent event) {
        int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
        switch (actionCode) {
            case MotionEvent.ACTION_DOWN:
                resetState();//防止没有接收到CANCEL or UP ,保险起见
                mPreMotionEvent = MotionEvent.obtain(event);
                updateStateByEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                mGestureInProgress = mListenter.onMoveBegin(this);
                break;
        }

    }

    float getMoveX() {
        return mExtenalPointer.x;

    }

    float getMoveY() {
        return mExtenalPointer.y;
    }


    interface OnMoveGestureListener {
        boolean onMoveBegin(MoveGestureDetector detector);

        boolean onMove(MoveGestureDetector detector);

        void onMoveEnd(MoveGestureDetector detector);
    }

    static class SimpleMoveGestureDetector implements OnMoveGestureListener {

        @Override
        public boolean onMoveBegin(MoveGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onMove(MoveGestureDetector detector) {
            return false;
        }

        @Override
        public void onMoveEnd(MoveGestureDetector detector) {
        }
    }

}