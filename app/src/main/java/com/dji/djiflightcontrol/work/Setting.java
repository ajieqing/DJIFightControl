package com.dji.djiflightcontrol.work;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.dji.djiflightcontrol.R;

import java.io.Serializable;


public class Setting extends Activity implements Serializable {
    public static String NAME = "";
    public static float HIGH = 0;
    public static int N = 0;
    private EditText name, high, n;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
    }

    public void save(View view) {
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

    @Override
    protected void onResume() {
        super.onResume();
        name = (EditText) findViewById(R.id.e_name);
        high = (EditText) findViewById(R.id.e_high);
        n = (EditText) findViewById(R.id.e_n);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("NAME", NAME);
        outState.putFloat("HIGH", HIGH);
        outState.putInt("N", N);
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        NAME = savedInstanceState.getString("NAME");
        HIGH = savedInstanceState.getFloat("HIGH");
        N = savedInstanceState.getInt("N");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void back(View view) {
        finish();
    }
}
