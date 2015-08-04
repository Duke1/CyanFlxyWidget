package com.cyanflxy.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanflxy.widget.ListViewCompat;

public class ListViewCompatActivity extends Activity implements View.OnClickListener {

    private ListViewCompat listViewCompat;
    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_list_view_compat);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText("ListViewCompat");
        findViewById(R.id.back).setOnClickListener(this);

        listViewCompat = (ListViewCompat) findViewById(R.id.list_view);
        myAdapter = new MyAdapter();
        listViewCompat.setAdapter(myAdapter);

        listViewCompat.setEmptyView(new TextView(this));

        listViewCompat.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ListViewCompatActivity.this, "onClick:" + position, Toast.LENGTH_SHORT).show();
            }
        });

        listViewCompat.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ListViewCompatActivity.this, "onLongClick:" + position, Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        findViewById(R.id.add_header).setOnClickListener(this);
        findViewById(R.id.remove_header).setOnClickListener(this);
        findViewById(R.id.add_content).setOnClickListener(this);
        findViewById(R.id.remove_content).setOnClickListener(this);
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
            case R.id.add_content:
                myAdapter.mCount++;
                break;
            case R.id.remove_content:
                if (myAdapter.mCount > 0) {
                    myAdapter.mCount--;
                }
                break;
        }
        myAdapter.notifyDataSetChanged();
    }

    private void addHeader() {
        View header = getView("header" + (listViewCompat.getHeaderViewsCount() + 1));
        listViewCompat.addHeaderView(header);
    }

    private void removeHeader() {
        listViewCompat.removeHeaderView(listViewCompat.getHeaderViewsCount() - 1);
    }

    private View getView(String content) {
        TextView view = (TextView) LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
        view.setText(content);
        return view;
    }

    private class MyAdapter extends BaseAdapter {

        public int mCount = 0;

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return ListViewCompatActivity.this.getView("Content" + position);
        }
    }
}
