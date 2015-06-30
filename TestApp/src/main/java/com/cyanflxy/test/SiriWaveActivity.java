package com.cyanflxy.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cyanflxy.widget.SiriWave;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Random;

public class SiriWaveActivity extends Activity implements View.OnClickListener {

    private VolumeMaker mVolumeMakerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏

        setContentView(R.layout.activity_siri_wave);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText("SiriWave");
        findViewById(R.id.back).setOnClickListener(this);

        SiriWave siriWave = (SiriWave) findViewById(R.id.siri_wave);
        siriWave.startAni();

        mVolumeMakerThread = new VolumeMaker(siriWave);
        mVolumeMakerThread.start();
    }

    @Override
    protected void onDestroy() {
        mVolumeMakerThread.setStop();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }

    private static class VolumeMaker extends Thread {

        private Reference<SiriWave> siriWaveRef;
        private volatile boolean stop;

        public VolumeMaker(SiriWave siriWave) {
            siriWaveRef = new WeakReference<SiriWave>(siriWave);
            stop = false;
        }

        public void setStop() {
            stop = true;
        }

        @Override
        public void run() {
            Random random = new Random(System.currentTimeMillis());
            while (true) {
                if (stop) {
                    break;
                }

                int volume = random.nextInt(30);

                SiriWave siriWave = siriWaveRef.get();
                if (siriWave == null) {
                    break;
                }

                Log.i("xyq", "setVolume " + volume);
                siriWave.setVolume(volume);

                int time = random.nextInt(10);

                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.i("xyq", "VolumeMaker Thread exit!");
        }
    }
}
