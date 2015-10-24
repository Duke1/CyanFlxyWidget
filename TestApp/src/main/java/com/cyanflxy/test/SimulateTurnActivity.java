package com.cyanflxy.test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Window;

public class SimulateTurnActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_simulate_turn);

        SimulateTurnView simulateTurnView = (SimulateTurnView) findViewById(R.id.simulate_turn_view);
        simulateTurnView.setBitmapAdapter(new MyBitmapAdapter());
    }

    private class MyBitmapAdapter implements SimulateTurnView.BitmapAdapter {

        private int[] ids = new int[]{R.drawable.pic1, R.drawable.pic2, R.drawable.pic3, R.drawable.pic4};

        @Override
        public int getCount() {
            return ids.length;
        }

        @Override
        public Bitmap getBitmap(int index) {
            return BitmapFactory.decodeResource(getResources(), ids[index]);
        }
    }
}
