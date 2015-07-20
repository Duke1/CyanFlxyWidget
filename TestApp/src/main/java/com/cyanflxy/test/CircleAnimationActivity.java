package com.cyanflxy.test;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.cyanflxy.widget.CircleAnimateView;

import java.io.IOException;

public class CircleAnimationActivity extends Activity implements View.OnClickListener {

    private RecordThread recordThread;
    private CircleAnimateView circleAnimateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏

        setContentView(R.layout.activity_circle_animation);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText("CircleAnimation");
        findViewById(R.id.back).setOnClickListener(this);

        circleAnimateView = (CircleAnimateView) findViewById(R.id.circle_animation);

        recordThread = new RecordThread(circleAnimateView);
        recordThread.start();

        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);

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
            case R.id.start:
                circleAnimateView.startAnimation();
                break;
            case R.id.stop:
                circleAnimateView.stopAnimation();
                break;
        }
    }

    private class RecordThread extends Thread {
        private volatile boolean isRun = false;

        private CircleAnimateView circleAnimateView;
        MediaRecorder mediaRecorder;


        public RecordThread(CircleAnimateView circleAnimateView) {
            super();
            this.circleAnimateView = circleAnimateView;

            mediaRecorder = new MediaRecorder();

            mediaRecorder.setMaxDuration(70000);
            mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {

                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    Log.i("AnimationActivity", " error=" + what + " ex=" + extra);
                }
            });

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //noinspection deprecation
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(getFilesDir().getPath() + "/hello_world.amr");
        }

        public void run() {
            super.run();
            isRun = true;

            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                Log.i("AnimationActivity", "prepare error:", e);
                return;
            }

            mediaRecorder.start();

            while (isRun) {
                int amplitude = mediaRecorder.getMaxAmplitude();
                Log.i("AnimationActivity", "Max Amplitude:" + amplitude);

                float value = amplitude / 200f;
                if (Float.compare(value, 100f) > 0) {
                    value = 100f;
                }
                circleAnimateView.setValue(value);

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            mediaRecorder.stop();
            mediaRecorder.release();
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
