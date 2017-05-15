package com.dji.djiflightcontrol.welocme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.common.Go;

/**
 * 欢迎界面
 */

public class Welcome extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
    }

    public void onClick(View view) {
        Intent intent;
        if (view.getId() == R.id.btn_go)
            intent = new Intent(this, Go.class);
        else
            intent = new Intent(this, Prepare.class);

        startActivity(intent);
        finish();
    }
}
