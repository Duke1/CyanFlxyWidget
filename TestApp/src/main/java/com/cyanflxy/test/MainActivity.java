package com.cyanflxy.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.cyanflxy.widget.CircleColorPicker;


public class MainActivity extends Activity implements CircleColorPicker.OnColorSelectedListener, View.OnClickListener {

    private CircleColorPicker mColorPicker;
    private TextView mColorView;
    private View mDemoColor;

    private String stateString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏

        setContentView(R.layout.activity_main);

        mColorPicker = (CircleColorPicker) findViewById(R.id.color_picker);
        mColorPicker.setOnColorSelectedListener(this);

        mColorView = (TextView) findViewById(R.id.color);
        mDemoColor = findViewById(R.id.demo_color);

        findViewById(R.id.set_state).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mColorPicker.setState(stateString);
            }
        });

        findViewById(R.id.get_state).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stateString = mColorPicker.getState();
            }
        });
    }

    @Override
    public void onColorSelected(int color) {
        String str = String.format("Color:R=%x,G=%x,B=%x",
                Color.red(color), Color.green(color), Color.blue(color));
        mColorView.setText(str);
        mDemoColor.setBackgroundColor(color);
    }

    @Override
    public void onClick(View v) {
    }
}
