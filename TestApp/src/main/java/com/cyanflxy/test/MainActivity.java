package com.cyanflxy.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    private List<ActivityInfo> activityList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏

        setContentView(R.layout.activity_main);

        initActivityList();
        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(new MyAdapter());
        list.setOnItemClickListener(this);
    }

    private void initActivityList() {
        activityList = new LinkedList<ActivityInfo>();
        activityList.add(new ActivityInfo(ColorPickerActivity.class));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ActivityInfo info = (ActivityInfo) view.getTag();

        Intent intent = new Intent(this, info.clazz);
        startActivity(intent);
    }

    private class ActivityInfo {
        public Class<? extends Activity> clazz;
        public String label;

        public ActivityInfo(Class<? extends Activity> activityClass) {
            clazz = activityClass;

            String name = clazz.getSimpleName();
            label = name.substring(0, name.length() - 8);
        }

    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return activityList.size();
        }

        @Override
        public Object getItem(int position) {
            return activityList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ActivityInfo info = activityList.get(position);

            if (convertView == null) {
                convertView = View.inflate(MainActivity.this, R.layout.list_item, null);
            }

            TextView view = (TextView) convertView;
            view.setText(info.label);

            view.setTag(info);
            return view;
        }
    }
}
