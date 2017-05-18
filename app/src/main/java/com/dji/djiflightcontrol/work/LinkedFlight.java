package com.dji.djiflightcontrol.work;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.welocme.LargeImageView;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by 杰 on 2017/5/18.
 */

public class LinkedFlight extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);
        setContentView(R.layout.prepare);
        LargeImageView mLargeImageView = (LargeImageView) findViewById(R.id.largeimageview);
        try {
            InputStream inputStream = getAssets().open("m100.png");
            mLargeImageView.setInputStream(inputStream, display);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
