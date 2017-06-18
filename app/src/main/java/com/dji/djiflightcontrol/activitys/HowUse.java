package com.dji.djiflightcontrol.activitys;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dji.djiflightcontrol.R;

/**
 * Created by 杰 on 2017/5/16.
 */

public class HowUse extends Activity{
    private ImageView[] imageViews = new ImageView[4];
    private int[] iv_id = new int[]{R.id.p_iv1, R.id.p_iv2, R.id.p_iv3, R.id.p_iv4};
    private int[] iv_mm = new int[]{R.mipmap.h1, R.mipmap.h2, R.mipmap.h3, R.mipmap.h4};
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        setContentView(R.layout.prepare);
        int width = (int) ((double) metrics.heightPixels /414 * 730);
        for (int i = 0; i < 4; i++) {
            imageViews[i] = (ImageView) findViewById(iv_id[i]);
            imageViews[i].setImageDrawable(getDrawable(iv_mm[i]));
            ViewGroup.LayoutParams params = imageViews[i].getLayoutParams();
            params.width = width;
            params.height = metrics.heightPixels;
            imageViews[i].setLayoutParams(params);
        }
    }
}
