package com.dji.djiflightcontrol.welocme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;

import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.work.Video;

import java.io.IOException;
import java.io.InputStream;

public class Prepare extends Activity {

    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);
        setContentView(R.layout.prepare);
        LargeImageView mLargeImageView = (LargeImageView) findViewById(R.id.largeimageview);
        btn = (Button) findViewById(R.id.btn_start_flightcontrol);
        btn.setClickable(false);
        try {
            InputStream inputStream = getAssets().open("m100.png");
            mLargeImageView.setInputStream(inputStream, display);
            mLargeImageView.setShowBtn(new LargeImageView.ShowBtn() {
                @Override
                public void show() {
                    if (btn.getVisibility() == View.INVISIBLE) {
                        AlphaAnimation alphaAnimation = new AlphaAnimation(0.1f, 1.0f);
                        alphaAnimation.setDuration(1000);
                        btn.setVisibility(View.VISIBLE);
                        btn.startAnimation(alphaAnimation);
                        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                btn.setClickable(true);
                            }
                        });
                    }
                }

                @Override
                public void hide() {
                    if (btn.getVisibility() == View.VISIBLE) {
                        btn.setVisibility(View.INVISIBLE);
                        btn.setClickable(false);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        Intent intent = new Intent(this, Video.class);
        startActivity(intent);
        finish();
    }
}
