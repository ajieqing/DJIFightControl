package com.dji.djiflightcontrol.welocme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.common.DJISampleApplication;
import com.dji.djiflightcontrol.common.MoveUtil1;
import com.dji.djiflightcontrol.work.Video;

/**
 * Created by 杰 on 2016/11/14.
 */
public class Prepare extends Activity {
    private MoveUtil1 util;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prepare);
    }

    @Override
    protected void onResume() {
        super.onResume();
        util = DJISampleApplication.util;
    }

    public void onClick(View view) {
        util.initFlightController();
        if (util.isInitFlightControllered()) {
//            Intent intent = new Intent(this, Setting.class);
//            startActivity(intent);
        }
        Intent intent = new Intent(this, Video.class);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
