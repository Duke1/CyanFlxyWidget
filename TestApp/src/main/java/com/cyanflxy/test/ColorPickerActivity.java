package com.cyanflxy.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cyanflxy.widget.CircleColorPicker;

public class ColorPickerActivity extends Activity
        implements CircleColorPicker.OnColorSelectedListener, View.OnClickListener {

    private TextView colorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏

        setContentView(R.layout.activity_color_picker);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText("ColorPicker");
        findViewById(R.id.back).setOnClickListener(this);

        CircleColorPicker colorPicker = (CircleColorPicker) findViewById(R.id.color_picker);
        colorPicker.setOnColorSelectedListener(this);

        colorView = (TextView) findViewById(R.id.color);

        onColorSelected(colorPicker.getColor());
    }

    @Override
    public void onColorSelected(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        String str = String.format("0x%02X%02X%02X", red, green, blue);
        colorView.setText(str);
        colorView.setBackgroundColor(color);

        int max = Math.max(red, Math.max(green, blue));
        if (max == red) {
            colorView.setTextColor(Color.rgb(0, 255 - green, 255 - blue));
        } else if (max == green) {
            colorView.setTextColor(Color.rgb(255 - red, 0, 255 - blue));
        } else {
            colorView.setTextColor(Color.rgb(255 - red, 255 - green, 0));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }
}

