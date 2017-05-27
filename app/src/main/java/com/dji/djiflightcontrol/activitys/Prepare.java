package com.dji.djiflightcontrol.activitys;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;

import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.views.BottomScrollview;

public class Prepare extends Activity {

    private Button btn;
    private ImageView[] imageViews = new ImageView[4];
    private int[] iv_id = new int[]{R.id.p_iv1, R.id.p_iv2, R.id.p_iv3, R.id.p_iv4};

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        setContentView(R.layout.prepare);
        btn = (Button) findViewById(R.id.btn_start_flightcontrol);
        btn.setClickable(false);
        BottomScrollview scrollView = (BottomScrollview) findViewById(R.id.p_sv);
        scrollView.setOnScrollToBottomLintener(new BottomScrollview.OnScrollToBottomListener() {
            @Override
            public void onScrollBottomListener(boolean isBottom) {
                if (isBottom) {
                    show();
                }else {
                    btn.setVisibility(View.INVISIBLE);
                    btn.setClickable(false);
                }
            }
        });
        int width = (int) ((double) metrics.heightPixels /851 * 709);
        for (int i = 0; i < 4; i++) {
            imageViews[i] = (ImageView) findViewById(iv_id[i]);
            ViewGroup.LayoutParams params = imageViews[i].getLayoutParams();
            params.width = width;
            params.height = metrics.heightPixels;
            imageViews[i].setLayoutParams(params);
        }
    }

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

    public void onClick(View view) {
        Intent intent = new Intent(this, Video.class);
        startActivity(intent);
        finish();
    }
}