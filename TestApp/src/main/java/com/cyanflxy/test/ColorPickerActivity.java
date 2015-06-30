package com.cyanflxy.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.cyanflxy.widget.CircleColorPicker;

public class ColorPickerActivity extends Activity
        implements CircleColorPicker.OnColorSelectedListener {

    private TextView mColorView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏

        setContentView(R.layout.activity_color_picker);

        CircleColorPicker mColorPicker = (CircleColorPicker) findViewById(R.id.color_picker);
        mColorPicker.setOnColorSelectedListener(this);

        mColorView = (TextView) findViewById(R.id.color);
    }

    @Override
    public void onColorSelected(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        String str = String.format("0x%02X%02X%02X", red, green, blue);
        mColorView.setText(str);
        mColorView.setBackgroundColor(color);

        int max = Math.max(red, Math.max(green, blue));
        if (max == red) {
            mColorView.setTextColor(Color.rgb(0, 255 - green, 255 - blue));
        } else if (max == green) {
            mColorView.setTextColor(Color.rgb(255 - red, 0, 255 - blue));
        } else {
            mColorView.setTextColor(Color.rgb(255 - red, 255 - green, 0));
        }
    }


}

