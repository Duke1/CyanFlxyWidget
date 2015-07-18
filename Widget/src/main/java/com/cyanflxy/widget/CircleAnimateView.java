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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.cyanflxy.annotation.API;
import com.cyanflxy.annotation.ReflectInvoke;

/**
 * Google 语音那样的圆圈动画
 * <p/>
 * Created by CyanFlxy on 2014/9/15.
 */
public class CircleAnimateView extends View implements ValueAnimator.AnimatorUpdateListener {

    public static final int RELATIVE_START = 0;
    public static final int RELATIVE_CENTER = 1;
    public static final int RELATIVE_END = 2;

    private float maxValue;// 动画最大幅度值

    private int width;// 绘图区宽度
    private int height;//绘图区高度

    private int relativeX;// 绘图中心相对X位置
    private int relativeY;// 绘图中心相对Y位置

    private float shiftX;// 绘图中心偏移X距离
    private float shiftY;// 绘图中心偏移X距离

    private float centerX;// 绘图中心X
    private float centerY;// 绘图中心Y

    private float centerSize;// 绘图中心区域大小

    private int duration;// 动画频率

    private Drawable centerDrawable;// 中心区域图片

    private AnimatorHolder innerDrawableHolder;// 内圈图片
    private Animator innerCircleAnimator;//内圈动画
    private InnerAniInterpolator innerAniInterpolator;//内圈插值器

    private AnimatorHolder outerDrawableHolder;// 外圈图片
    private Animator outerCircleAnimator;//外圈动画

    public CircleAnimateView(Context c, AttributeSet attrs) {
        super(c, attrs);

        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CircleAnimateView);
        maxValue = a.getFloat(R.styleable.CircleAnimateView_CircleAnimateView_max_value, 100);
        centerSize = a.getDimension(R.styleable.CircleAnimateView_CircleAnimateView_center_size, 0);

        centerDrawable = a.getDrawable(R.styleable.CircleAnimateView_CircleAnimateView_center_image);
        Drawable innerDrawable = a.getDrawable(R.styleable.CircleAnimateView_CircleAnimateView_inner_circle_image);
        Drawable outerDrawable = a.getDrawable(R.styleable.CircleAnimateView_CircleAnimateView_outer_circle_image);

        relativeX = a.getInt(R.styleable.CircleAnimateView_CircleAnimateView_center_x_relative, RELATIVE_CENTER);
        relativeY = a.getInt(R.styleable.CircleAnimateView_CircleAnimateView_center_x_relative, RELATIVE_CENTER);
        shiftX = a.getDimension(R.styleable.CircleAnimateView_CircleAnimateView_center_x_shift, 0);
        shiftY = a.getDimension(R.styleable.CircleAnimateView_CircleAnimateView_center_y_shift, 0);

        duration = a.getInteger(R.styleable.CircleAnimateView_CircleAnimateView_animate_duration, 700);

        a.recycle();

        calculateCenter();

        if (innerDrawable != null) {
            innerDrawableHolder = new AnimatorHolder(innerDrawable);
            innerCircleAnimator = createInnerAnimator(innerDrawableHolder);
        }

        if (outerDrawable != null) {
            outerDrawableHolder = new AnimatorHolder(outerDrawable);
            outerCircleAnimator = createOuterAnimator(this.outerDrawableHolder);
        }

    }

    private void calculateCenter() {
        if (centerSize == 0 && centerDrawable != null) {
            centerSize = Math.min(
                    centerDrawable.getIntrinsicHeight(),
                    centerDrawable.getIntrinsicWidth());
        }

        if (centerSize <= 0) {
            centerSize = 100;
        }

        resizeCenterDrawable();
    }

    private void resizeCenterDrawable() {
        if (centerDrawable != null) {
            Rect center = new Rect();
            int half = (int) (centerSize / 2);
            center.left = -half;
            center.top = -half;
            center.right = half;
            center.bottom = half;

            centerDrawable.setBounds(center);
        }
    }

    private Animator createInnerAnimator(AnimatorHolder holder) {
        PropertyValuesHolder x = PropertyValuesHolder.ofFloat("width", centerSize, centerSize * 3);
        PropertyValuesHolder y = PropertyValuesHolder.ofFloat("height", centerSize, centerSize * 3);

        ValueAnimator animator = ObjectAnimator.ofPropertyValuesHolder(holder, x, y);
        animator.setDuration(duration);
        animator.addUpdateListener(this);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        innerAniInterpolator = new InnerAniInterpolator();
        animator.setInterpolator(innerAniInterpolator);

        return animator;
    }

    private Animator createOuterAnimator(AnimatorHolder holder) {
        PropertyValuesHolder x = PropertyValuesHolder.ofFloat("width", centerSize, centerSize * 3);
        PropertyValuesHolder y = PropertyValuesHolder.ofFloat("height", centerSize, centerSize * 3);
        PropertyValuesHolder a = PropertyValuesHolder.ofInt("alpha", 255, 50);

        ValueAnimator animator = ObjectAnimator.ofPropertyValuesHolder(holder, x, y, a);
        animator.setDuration(duration);
        animator.addUpdateListener(this);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new DecelerateInterpolator());

        return animator;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) {
            return;
        }

        if (width == w && height == h) {
            return;
        }

        width = w;
        height = h;

        setCenterX(relativeX, shiftX);
        setCenterY(relativeY, shiftY);

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(centerX, centerY);

        if (outerDrawableHolder != null) {
            outerDrawableHolder.draw(canvas);
        }

        if (innerDrawableHolder != null) {
            innerDrawableHolder.draw(canvas);
        }

        if (centerDrawable != null) {
            centerDrawable.draw(canvas);
        }

        canvas.restore();

    }


    @Override
    protected void onDetachedFromWindow() {
        stopListenAni();
        super.onDetachedFromWindow();
    }

    @API
    public void setMaxValue(float max) {
        maxValue = max;
    }

    @API
    public void setCenterX(float centerX) {
        this.centerX = centerX;
        this.relativeX = RELATIVE_START;
        this.shiftX = centerX;
    }

    @API
    public void setCenterY(float centerY) {
        this.centerY = centerY;
        this.relativeY = RELATIVE_START;
        this.shiftY = centerY;
    }

    @API
    public void setCenterX(int relativeX, float shiftX) {
        this.relativeX = relativeX;
        this.shiftX = shiftX;

        switch (relativeX) {
            case RELATIVE_CENTER:
                centerX = width / 2f;
                break;
            case RELATIVE_START:
                centerX = 0;
                break;
            case RELATIVE_END:
                centerX = width;
                break;
        }

        centerX += shiftX;
    }

    @API
    public void setCenterY(int relativeY, float shiftY) {
        this.relativeY = relativeY;
        this.shiftY = shiftY;

        switch (relativeY) {
            case RELATIVE_CENTER:
                centerY = height / 2f;
                break;
            case RELATIVE_START:
                centerY = 0;
                break;
            case RELATIVE_END:
                centerY = height;
                break;
        }

        centerY += shiftY;
    }

    @API
    public void setCenterSize(float size) {
        centerSize = size;

        resizeCenterDrawable();

        if (innerDrawableHolder != null) {
            if (innerCircleAnimator.isRunning()) {
                innerCircleAnimator.cancel();
            }
            innerCircleAnimator = createInnerAnimator(innerDrawableHolder);
        }

        if (outerDrawableHolder != null) {
            if (outerCircleAnimator.isRunning()) {
                outerCircleAnimator.cancel();
            }

            outerCircleAnimator = createOuterAnimator(outerDrawableHolder);
        }
    }

    @API
    public void setDuration(int duration) {
        this.duration = duration;
        if (innerCircleAnimator != null) {
            innerCircleAnimator.setDuration(duration);
        }
        if (outerCircleAnimator != null) {
            outerCircleAnimator.setDuration(duration);
        }
    }

    @API
    public void setCenterDrawable(Drawable drawable) {
        centerDrawable = drawable;
        resizeCenterDrawable();
        invalidate();
    }

    @API
    public void setInnerDrawable(Drawable drawable) {
        if (innerDrawableHolder == null) {
            innerDrawableHolder = new AnimatorHolder(drawable);
            innerCircleAnimator = createInnerAnimator(innerDrawableHolder);
        } else {
            innerDrawableHolder.setDrawable(drawable);
        }
    }

    @API
    public void setOuterDrawable(Drawable drawable) {
        if (outerDrawableHolder == null) {
            outerDrawableHolder = new AnimatorHolder(drawable);
            outerCircleAnimator = createOuterAnimator(this.outerDrawableHolder);
        } else {
            outerDrawableHolder.setDrawable(drawable);
        }
    }


    @API
    public void startListenAni() {
        if (innerCircleAnimator != null) {
            innerAniInterpolator.reset();
            innerCircleAnimator.start();
        }

        if (outerCircleAnimator != null) {
            outerCircleAnimator.start();
        }
    }

    @API
    public void stopListenAni() {
        if (innerCircleAnimator != null) {
            innerCircleAnimator.cancel();
        }

        if (outerCircleAnimator != null) {
            outerCircleAnimator.cancel();
        }
    }

    @API
    public void setValue(float v) {
        if (Float.compare(v, 0) < 0 || Float.compare(v, maxValue) > 0) {
            throw new IllegalArgumentException("Value range [0," + maxValue + "],now is " + v);
        }
        innerAniInterpolator.setVolume(v / maxValue);
    }


    /**
     * 内圈动画的插值器，该插值器根据音量产生变化
     *
     * @author yuqiang.xia
     * @since 2014-7-20
     */
    private static class InnerAniInterpolator implements Interpolator {

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
        private float mTimeCircle;
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
            mTimeCircle = 0.0f;
            mHighAmplitude = Amplitude_LOW;
            mLowAmplitude = Amplitude_LOW;
        }

        public void setVolume(float volume) {
            if (Float.compare(volume, 0) == 0) {
                return;
            }

            float amplitude = (float) Math.min(volume, 0.7);
            if (amplitude < mLastOutput) {
                return;
            }

            // 改变过小、频率过快，会导致震动频率大，
            if (Math.abs(amplitude - mHighAmplitude) < 0.05) {
                return;
            }

            mHighAmplitude = amplitude;

            // 依据当前振幅位置，计算当前周期进行范围
            mTimeCircle = (mLastOutput - Amplitude_LOW)
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

            mTimeCircle += delta;
            if (mTimeCircle > (1.0f + TIME_SHRINK)) {
                mTimeCircle = mTimeCircle - 1.0f - TIME_SHRINK;
                mHighAmplitude = Amplitude_LOW;

                if (mLowAmplitude == Amplitude_END) {
                    return mLowAmplitude;
                }
            }

            if (mTimeCircle <= 1.0f) {
                mLastOutput = (mHighAmplitude - mLowAmplitude) * mTimeCircle
                        + mLowAmplitude;
            } else {
                mLastOutput = (mLowAmplitude - mHighAmplitude) / TIME_SHRINK
                        * (mTimeCircle - 1.0f) + mHighAmplitude;
            }

            mLastInput = input;
            return mLastOutput;
        }

    }

    class AnimatorHolder {
        private Drawable drawable;
        private int alpha = 255;
        private Rect dst;

        public AnimatorHolder(Drawable d) {
            drawable = d;
            dst = new Rect();
        }

        public void setDrawable(Drawable d) {
            drawable = d;
        }

        @ReflectInvoke(invokeBy = "API")
        public void setAlpha(int a) {
            alpha = a;
        }

        @ReflectInvoke(invokeBy = "API")
        public void setWidth(float width) {
            int half = (int) (width / 2);
            dst.left = -half;
            dst.right = half;
        }

        @ReflectInvoke(invokeBy = "API")
        public void setHeight(float height) {
            int half = (int) (height / 2);
            dst.top = -half;
            dst.bottom = half;
        }

        public void draw(Canvas canvas) {
            drawable.setBounds(dst);
            drawable.setAlpha(alpha);
            drawable.draw(canvas);
        }

    }
}