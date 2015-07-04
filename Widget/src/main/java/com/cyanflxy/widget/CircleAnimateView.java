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
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Google 语音那样的圆圈动画
 * <p/>
 * Created by CyanFlxy on 2014/9/15.
 */
public class CircleAnimateView extends RelativeLayout implements OnClickListener {

    private ImageView mMicImg;
    private ImageView mOuterCircle;
    private ImageView mInnerCircle;

    private Animation mListenCircling;
    private AnimationSet mOuterCircleAni;
    private Animation mInnerCircleAni;
    private InnerAniInterpolator mInnerAniInterpolator;

    private View mRootView;
    private OnClickListener mOnClickListener;

    public CircleAnimateView(Context c, AttributeSet attrs) {
        super(c, attrs);
        inflate(c, R.layout.voice_mic_animate_view, this);

        initView(this);
        initAnimate();

//        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.voice_style);
//        float dist = a.getDimension(R.styleable.voice_style_bottom_distance, 0);
//
//        a.recycle();
    }

    private void initView(View rootView) {
        mRootView = rootView;
        mMicImg = (ImageView) rootView.findViewById(R.id.mic_img);
        mMicImg.setOnClickListener(this);
        mOuterCircle = (ImageView) rootView.findViewById(R.id.outer_circle);
        mInnerCircle = (ImageView) rootView.findViewById(R.id.inner_circle);

    }

    private void initAnimate() {
        mListenCircling = new RotateAnimation(0f, -360f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mListenCircling.setDuration(2000);
        mListenCircling.setRepeatCount(Animation.INFINITE);
        mListenCircling.setInterpolator(new LinearInterpolator());

        // 外层大圈动画，扩大与透明化
        long duration = 700;
        Animation outer = new ScaleAnimation(1, 3, 1, 3,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        outer.setDuration(duration);
        outer.setRepeatCount(Animation.INFINITE);

        Animation alpha = new AlphaAnimation(1.0f, 0.0f);
        alpha.setDuration(duration);
        alpha.setRepeatCount(Animation.INFINITE);

        mOuterCircleAni = new AnimationSet(true);
        mOuterCircleAni.addAnimation(outer);
        mOuterCircleAni.addAnimation(alpha);
        mOuterCircleAni.setInterpolator(new DecelerateInterpolator());

        // 内圈动画，震动扩大
        mInnerCircleAni = new ScaleAnimation(0.95f, 2.0f, 0.95f, 2.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mInnerCircleAni.setDuration(500);
        mInnerCircleAni.setRepeatCount(Animation.INFINITE);
        mInnerAniInterpolator = new InnerAniInterpolator();
        mInnerCircleAni.setInterpolator(mInnerAniInterpolator);

    }

    @Override
    protected void onDetachedFromWindow() {
        stopListenAni();
        super.onDetachedFromWindow();
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        mMicImg.setClickable(clickable);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.mic_img) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(mRootView);
            }
        }
    }

    // 监听时动画
    public void startListenAni() {
        mOuterCircleAni.reset();
        mOuterCircle.setVisibility(View.VISIBLE);
        mOuterCircle.startAnimation(mOuterCircleAni);

        mInnerAniInterpolator.reset();
        mInnerCircleAni.reset();
        mInnerCircle.setVisibility(View.VISIBLE);
        mInnerCircle.startAnimation(mInnerCircleAni);

    }

    public void stopListenAni() {
        mOuterCircleAni.cancel();
        mOuterCircle.clearAnimation();
        mOuterCircle.setVisibility(View.GONE);

        mInnerCircleAni.cancel();
        mInnerCircle.clearAnimation();
        mInnerCircle.setVisibility(View.GONE);

        mMicImg.setImageResource(R.drawable.voice_mic);
    }

    public void setValue(float v) {
        mInnerAniInterpolator.setVolume(v);
    }

    /**
     * 内圈动画的插值器，该插值器根据音量产生变化
     *
     * @author yuqiang.xia
     * @since 2014-7-20
     */
    private class InnerAniInterpolator implements Interpolator {

        /**
         * 缩减周期比值
         */
        private static final float TIME_SHRINK = 0.8f;
        /**
         * 普通振动的波谷
         */
        private static final float Amplitude_LOW = -0.4f;
        /**
         * 动画结束时的波谷
         */
        private static final float Amplitude_END = -2.0f;

        /**
         * 上次输入
         */
        private float mLastInput;
        /**
         * 上次输出
         */
        private float mLastOutput;

        /**
         * 时间周期状态，一个周期是1.0+TIME_SHRINK，振幅从-0.1到高峰再回到-0.1
         */
        private float mTimeCicle;
        /**
         * 当前周期需要达到的波峰
         */
        private float mHighAmplitude;
        /**
         * 当前周期需要达到的波谷
         */
        private float mLowAmplitude;

        public InnerAniInterpolator() {
            reset();
        }

        public void reset() {
            mLastInput = 0.0f;
            mLastOutput = -1.0f;
            mTimeCicle = 0.0f;
            mHighAmplitude = Amplitude_LOW;
            mLowAmplitude = Amplitude_LOW;
        }

        public void setVolume(float volume) {
            if (volume == 0) {
                return;
            }

            float amplitude = (float) Math.min(volume / 30f, 0.7);
            if (amplitude < mLastOutput) {
                return;
            }

            // 改变过小、频率过快，会导致震动频率大，
            if (Math.abs(amplitude - mHighAmplitude) < 0.05) {
                return;
            }

            mHighAmplitude = amplitude;

            // 依据当前振幅位置，计算当前周期进行范围
            mTimeCicle = (mLastOutput - Amplitude_LOW)
                    / (mHighAmplitude - Amplitude_LOW);

        }

        @Override
        public float getInterpolation(float input) {
            // 计算我们自己的时序
            float delta;
            if (input >= mLastInput) {
                delta = input - mLastInput;
            } else {
                delta = input + 1.0f - mLastInput;
            }

            mTimeCicle += delta;
            if (mTimeCicle > (1.0f + TIME_SHRINK)) {
                mTimeCicle = mTimeCicle - 1.0f - TIME_SHRINK;
                mHighAmplitude = Amplitude_LOW;

                if (mLowAmplitude == Amplitude_END) {
                    return mLowAmplitude;
                }
            }

            if (mTimeCicle <= 1.0f) {
                mLastOutput = (mHighAmplitude - mLowAmplitude) * mTimeCicle
                        + mLowAmplitude;
            } else {
                mLastOutput = (mLowAmplitude - mHighAmplitude) / TIME_SHRINK
                        * (mTimeCicle - 1.0f) + mHighAmplitude;
            }

            mLastInput = input;
            return mLastOutput;
        }

    }
}
