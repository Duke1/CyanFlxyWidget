package com.cyanflxy.test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Window;

import com.cyanflxy.widget.SimulateTurnView;

public class SimulateTurnActivity extends Activity {

    private SimulateTurnView simulateTurnView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_simulate_turn);

        simulateTurnView = (SimulateTurnView) findViewById(R.id.simulate_turn_view);
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
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

            int w = simulateTurnView.getWidth();
            int h = simulateTurnView.getHeight();
            Bitmap sizedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(sizedBitmap);

            RectF drawRect = new RectF(0, 0, w, h);
            canvas.drawBitmap(bitmap, null, drawRect, null);
            bitmap.recycle();

            return sizedBitmap;
        }
    }
}
