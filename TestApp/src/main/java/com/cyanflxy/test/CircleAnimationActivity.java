package com.cyanflxy.test;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cyanflxy.widget.CircleAnimateView;

public class CircleAnimationActivity extends Activity implements View.OnClickListener {

    private RecordThread recordThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏

        setContentView(R.layout.activity_circle_animation);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText("CircleAnimation");
        findViewById(R.id.back).setOnClickListener(this);

        CircleAnimateView circleAnimateView = (CircleAnimateView) findViewById(R.id.circle_animation);
        circleAnimateView.startListenAni();

        recordThread = new RecordThread(circleAnimateView);
        recordThread.start();

    }

    @Override
    protected void onDestroy() {
        recordThread.setStop();
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

    private class RecordThread extends Thread {
        private AudioRecord audioRecord;
        private int bufferSize;
        private final int SAMPLE_RATE_IN_HZ = 8000;
        private volatile boolean isRun = false;

        private CircleAnimateView circleAnimateView;

        @SuppressWarnings("deprecation")
        public RecordThread(CircleAnimateView circleAnimateView) {
            super();
            this.circleAnimateView = circleAnimateView;

            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        }

        public void run() {
            super.run();
            audioRecord.startRecording();

            short[] buffer = new short[bufferSize];
            isRun = true;

            while (isRun) {
                int len = audioRecord.read(buffer, 0, bufferSize);

                int v = 0;
                for (int i = 0; i < len; i++) {
                    v += buffer[i] * buffer[i];
                }

                float dB = (float) (10 * Math.log10((double) v / len));
                Log.i("SineWave", "db=" + dB);
                if (Float.compare(dB, 100) > 0) {
                    dB = 100;
                }
                circleAnimateView.setValue(dB);
            }
            audioRecord.stop();
            audioRecord.release();
        }

        public void setStop() {
            isRun = false;
        }

        public void start() {
            if (!isRun) {
                super.start();
            }
        }

    }
}
