package com.cyanflxy.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cyanflxy.widget.ListViewCompat;

public class ListViewCompatActivity extends Activity implements View.OnClickListener {

    private ListViewCompat listViewCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_list_view_compat);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText("ListViewCompat");
        findViewById(R.id.back).setOnClickListener(this);

        listViewCompat = (ListViewCompat) findViewById(R.id.list_view);

        findViewById(R.id.add_header).setOnClickListener(this);
        findViewById(R.id.remove_header).setOnClickListener(this);
        findViewById(R.id.long_clickable).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.add_header:
                addHeader();
                break;
            case R.id.remove_header:
                removeHeader();
                break;
            case R.id.long_clickable:
                toggleLongClickable();
                break;
        }
    }

    private void addHeader() {

    }

    private void removeHeader() {

    }

    public void toggleLongClickable() {

    }
}
