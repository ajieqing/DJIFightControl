package com.dji.djiflightcontrol.welocme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

public class LargeImageView extends View {
    private static final BitmapFactory.Options options = new BitmapFactory.Options();

    static {
        options.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    private BitmapRegionDecoder mDecoder;
    /**
     * 图片的宽度和高度
     */
    private int mImageWidth, mImageHeight;
    /**
     * 绘制的区域
     */
    private volatile Rect mRect = new Rect();
    private MoveGestureDetector mDetector;
    private DisplayMetrics display;
    private ShowBtn showBtn;

    public LargeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        mDetector = new MoveGestureDetector(getContext(), new MoveGestureDetector.SimpleMoveGestureDetector() {
            @Override
            public boolean onMove(MoveGestureDetector detector) {
                int moveX = (int) detector.getMoveX();
                int moveY = (int) detector.getMoveY();
                if (mImageWidth > getWidth()) {
                    mRect.offset(-moveX, 0);
                    checkWidth();
                    invalidate();
                }
                if (mImageHeight > getHeight()) {
                    mRect.offset(0, -moveY);
                    checkHeight();
                    invalidate();
                }
                return true;
            }
        });
    }

    private void checkWidth() {


        Rect rect = mRect;
        int imageWidth = mImageWidth;

        if (rect.right > imageWidth) {
            rect.right = imageWidth;
            rect.left = imageWidth - getWidth();
        }

        if (rect.left < 0) {
            rect.left = 0;
            rect.right = getWidth();
        }
    }

    private void checkHeight() {

        Rect rect = mRect;
        int imageHeight = mImageHeight;

        if (rect.bottom >= imageHeight) {
            rect.bottom = imageHeight;
            rect.top = imageHeight - getHeight();
            if (showBtn != null)
                showBtn.show();
        } else {
            if (showBtn != null)
                showBtn.hide();
        }

        if (rect.top < 0) {
            rect.top = 0;
            rect.bottom = getHeight();
        }
    }

    public void setInputStream(InputStream is, DisplayMetrics display) {
        try {
            this.display = display;
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
            mImageWidth = mDecoder.getWidth();
            mImageHeight = mDecoder.getHeight();
            requestLayout();
            invalidate();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                if (is != null) is.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onToucEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bm = mDecoder.decodeRegion(mRect, options);
        bm = ThumbnailUtils.extractThumbnail(bm, display.widthPixels, display.heightPixels);
        canvas.drawBitmap(bm, 0, 0, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        Log.d(TAG, "onMeasure: w=" + width + "h=" + height);

        mRect.left = 0;
        mRect.top = 0;
        mRect.right = mRect.left + width;
        mRect.bottom = mRect.top + height;

    }

    public void setShowBtn(ShowBtn showBtn) {
        this.showBtn = showBtn;
    }

    interface ShowBtn {
        void show();

        void hide();
    }
}
