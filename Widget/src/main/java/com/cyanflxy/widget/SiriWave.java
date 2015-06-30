/*
 * Copyright (C) 2015 CyanFlxy <cyanflxy@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanflxy.widget;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.lang.ref.SoftReference;

/**
 * Siri那样的随声音变化的波动
 * TODO 支持自定义背景颜色，支持自定义前景色，支持固定高度
 * TODO 录屏怎么样？
 * <p/>
 * Created by CyanFlxy on 2014/6/15.
 */
public class SiriWave extends View {

    private final Paint mPaint;
    private final Handler mHandler;

    private MoveThread mMoveThread;// 右移线程
    private final Path mSinPath;

    // 音量振幅恢复时随时间变化的系数
    private final float AMPLITUDE_RESTORE_COEFFICIENT = -0.001f;
    // 音量振幅设置时随时间变化的系数
    private final float AMPLITUDE_SET_COEFFICIENT = -AMPLITUDE_RESTORE_COEFFICIENT * 2;
    private final float MIN_VOLUME_AMPLITUDE = 0.23f;

    private float volumeAmplitude = MIN_VOLUME_AMPLITUDE;// 音量振幅修正
    private final float frequency = 1.5f;// 做点修改，范围内有做少个周期
    private float phase = 0;// 周期起点 - 波形右移变量
    private float phaseProportion = 0.4f;// 右移随时间移动比例，跟手机像素密度有关.像素密度越大，该值越大

    // 波线宽度
    private final float[] WAVE_WIDTH = new float[]{3, 2, 2, 1, 1};
    // 透明度
    private final float[] WAVE_ALPHA = new float[]{1.0f, 0.4f, 0.4f, 0.4f,
            0.4f};
    // 周期起点修正
    private final int[] NORMED_PHASE = new int[]{0, 6, -9, 15, -21};

    public SiriWave(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(0xFFFFFFFF);

        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setColor(0xFF489AF7);

        mSinPath = new Path();

        mHandler = new MyHandler(this);

        init();
    }

    private void init() {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);

        float density = metric.density; // 屏幕密度（0.75 / 1.0 / 1.5）

        phaseProportion = density * 0.3f;

        for (int i = 0; i < WAVE_WIDTH.length; i++) {
            WAVE_WIDTH[i] = WAVE_WIDTH[i] * density / 2;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAni();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float halfHeight = getHeight() / 2.0f;
        float mid = width / 2.0f;
        // 最高振幅
        float maxAmplitude = halfHeight - 4.0f;

        for (int i = 0; i < WAVE_WIDTH.length; i++) {
            float progress = 1.0f - (float) i / WAVE_WIDTH.length;
            // 修正振幅
            float normedAmplitude = (1.5f * progress - 0.5f);

            mPaint.setStrokeWidth(WAVE_WIDTH[i]);
            // mPaint.setAlpha((int) (((progress / 3.0f * 2.0f) + (1.0f / 3.0f))
            // * 255));
            mPaint.setAlpha((int) (WAVE_ALPHA[i] * 255));

            mSinPath.reset();
            mSinPath.moveTo(0, halfHeight);

            for (int x = 1; x < width; x++) {
                float scaling = (float) (1 - Math.pow(1 / mid * (x - mid), 2));
                float y = (float) (scaling // 振幅与和水平中心的距离成反比，这样才有左右两端的直线效果
                        * maxAmplitude // 最高振幅
                        * normedAmplitude// 按第几条线修正振幅
                        * volumeAmplitude // 按音量修正振幅
                        * Math.sin(2 * Math.PI
                        * ((x + phase + NORMED_PHASE[i]) / width)
                        * frequency) + halfHeight);
                mSinPath.lineTo(x, y);
            }
            canvas.drawPath(mSinPath, mPaint);

        }
    }

    public void setVolume(int volume) {
        if (volume == 0) {
            return;
        }

        float newAmplitude;
        float amplitude = (2 - 30f / (volume + 15))
                * ((1 - MIN_VOLUME_AMPLITUDE) / 2) + MIN_VOLUME_AMPLITUDE;

        if (amplitude < MIN_VOLUME_AMPLITUDE) {
            newAmplitude = MIN_VOLUME_AMPLITUDE;
        } else if (amplitude > 1.0f) {
            newAmplitude = 1.0f;
        } else {
            newAmplitude = amplitude;
        }

        if (mMoveThread != null) {
            mMoveThread.insertAmplitude(newAmplitude);
        }

    }

    public void startAni() {
        phase = 0;
        volumeAmplitude = MIN_VOLUME_AMPLITUDE;
        invalidate();

        if (mMoveThread != null) {
            mMoveThread.stopRunning();
        }
        mMoveThread = new MoveThread();
        mMoveThread.start();
    }

    public void stopAni() {
        if (mMoveThread != null) {
            mMoveThread.stopRunning();
            mMoveThread = null;
        }
    }

    private static final int MSG_MOVE = 1;

    private static class MyHandler extends Handler {
        private final SoftReference<View> host;

        public MyHandler(View view) {
            host = new SoftReference<View>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            View outer = host.get();
            if (outer == null) {
                return;
            }

            outer.invalidate();
        }
    }

    class MoveThread extends Thread {

        private boolean isStop;

        private long amplitudeSetTime;
        private float lastAmplitude;
        private float nextTargetAmplitude;

        @Override
        public void run() {
            synchronized (this) {
                isStop = false;
            }

            long lastTime = System.currentTimeMillis();
            amplitudeSetTime = lastTime;
            lastAmplitude = volumeAmplitude;
            nextTargetAmplitude = lastAmplitude;
            phase = 0;

            while (true) {

                try {
                    sleep(79);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (this) {
                    if (isStop) {
                        notify();
                        break;
                    }
                }

                long cur = System.currentTimeMillis();

                // 移动速度跟时间、像素密度、振幅加速正相关
                phase -= (cur - lastTime) * phaseProportion
                        * (1 + nextTargetAmplitude - MIN_VOLUME_AMPLITUDE);
                lastTime = cur;

                // 计算音量振幅
                volumeAmplitude = currentVolumeAmplitude(cur);

                if (!mHandler.hasMessages(MSG_MOVE)) {
                    mHandler.sendEmptyMessage(MSG_MOVE);
                }

            }

        }

        public void insertAmplitude(float amplitude) {
            long curTime = System.currentTimeMillis();
            lastAmplitude = currentVolumeAmplitude(curTime);
            amplitudeSetTime = curTime;
            nextTargetAmplitude = amplitude;

            interrupt();
        }

        // 计算当前时间下的振幅
        private float currentVolumeAmplitude(long curTime) {
            if (lastAmplitude == nextTargetAmplitude) {
                return nextTargetAmplitude;
            }

            if (curTime == amplitudeSetTime) {
                return lastAmplitude;
            }

            // 走设置流程，改变速度快
            if (nextTargetAmplitude > lastAmplitude) {
                float target = lastAmplitude + AMPLITUDE_SET_COEFFICIENT
                        * (curTime - amplitudeSetTime);
                if (target >= nextTargetAmplitude) {
                    target = nextTargetAmplitude;
                    lastAmplitude = nextTargetAmplitude;
                    amplitudeSetTime = curTime;
                    nextTargetAmplitude = MIN_VOLUME_AMPLITUDE;
                }
                return target;
            }

            // 走恢复流程，改变速度慢
            if (nextTargetAmplitude < lastAmplitude) {
                float target = lastAmplitude + AMPLITUDE_RESTORE_COEFFICIENT
                        * (curTime - amplitudeSetTime);
                if (target <= nextTargetAmplitude) {
                    target = nextTargetAmplitude;
                    lastAmplitude = nextTargetAmplitude;
                    amplitudeSetTime = curTime;
                    nextTargetAmplitude = MIN_VOLUME_AMPLITUDE;
                }
                return target;
            }

            return MIN_VOLUME_AMPLITUDE;
        }

        public synchronized void stopRunning() {
            isStop = true;
            interrupt();
            try {
                wait(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (mHandler.hasMessages(MSG_MOVE)) {
                mHandler.removeMessages(MSG_MOVE);
            }
        }
    }
}
