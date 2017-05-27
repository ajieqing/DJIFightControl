package com.dji.djiflightcontrol.activitys;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.dji.djiflightcontrol.R;

/**
 * Created by 杰 on 2016/11/15.
 */
public class Help extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        String[] helps = getResources().getStringArray(R.array.helps);
        final String[] urls = getResources().getStringArray(R.array.urls);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.text1, helps);
        ListView help_lv = (ListView) findViewById(R.id.help_lv);
        help_lv.setAdapter(arrayAdapter);
        help_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 4:
                        startActivity(new Intent(Help.this, Question.class));
                        break;
                    case 5:
                        startActivity(new Intent(Help.this, Prepare.class));
                        break;
                    case 6:
                        startActivity(new Intent(Help.this, HowUse.class));
                        break;
                    default:
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urls[position])));
                        break;

                }
            }
        });
    }

}
