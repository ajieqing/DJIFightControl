package com.dji.djiflightcontrol.activitys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.dji.djiflightcontrol.R;

import java.io.Serializable;

import static com.dji.djiflightcontrol.common.DJISampleApplication.HIGH;
import static com.dji.djiflightcontrol.common.DJISampleApplication.N;
import static com.dji.djiflightcontrol.common.DJISampleApplication.NAME;


public class Setting extends Activity implements Serializable {

    private EditText name, high, n;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        name = (EditText) findViewById(R.id.e_name);
        high = (EditText) findViewById(R.id.e_high);
        n = (EditText) findViewById(R.id.e_n);
        name.setText(NAME);
        high.setText(HIGH + "");
        n.setText(N + "");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        String s;
        s = name.getText().toString();
        if (s.equals("")) {
            Toast.makeText(this, "请输入桥墩编号", Toast.LENGTH_SHORT).show();
            return;
        }
        NAME = s;
        s = high.getText().toString();
        if (s.equals("")) {
            Toast.makeText(this, "请输入桥墩编号", Toast.LENGTH_SHORT).show();
            return;
        }
        HIGH = Float.parseFloat(s);
        s = n.getText().toString();
        if (s.equals("")) {
            Toast.makeText(this, "请输入拍照次数", Toast.LENGTH_SHORT).show();
            return;
        }
        N = Integer.parseInt(s);
        Intent intent = new Intent(this, Video.class);
        startActivity(intent);
        finish();
    }

}
