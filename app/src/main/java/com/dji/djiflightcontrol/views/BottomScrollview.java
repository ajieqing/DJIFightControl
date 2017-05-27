package com.dji.djiflightcontrol.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

/**
 * Created by 杰 on 2017/5/19.
 */

public class BottomScrollview extends HorizontalScrollView {
    private OnScrollToBottomListener onScrollToBottom;

    public BottomScrollview(Context context) {
        super(context);
    }

    public BottomScrollview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomScrollview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
                                  boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if (scrollX > 10 && null != onScrollToBottom) {
            onScrollToBottom.onScrollBottomListener(clampedX);
        }
    }

    public void setOnScrollToBottomLintener(OnScrollToBottomListener listener) {
        onScrollToBottom = listener;
    }

    public interface OnScrollToBottomListener {
        void onScrollBottomListener(boolean isBottom);
    }
}
