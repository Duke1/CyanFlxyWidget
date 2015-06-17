/*
 * Copyright (C) 2015 CyanFlxy <xyufeico@gmail.com>
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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;

/**
 * 取色器
 * <p/>
 * Created by CyanFlxy on 2015/6/17.
 */
public class CircleColorPicker extends View {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private static final int MIN_SIZE = 400;

    private static final int[] PICKER_COLORS = {0xFF88FF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000, 0xFFFFFF00, 0xFF88FF00,};
    private static final float[] PICKER_SHADER_POSITIONS = {0, 1 / 12f, 3 / 12f, 5 / 12f, 7 / 12f, 9 / 12f, 11 / 12f, 1.0f};
    private int[] mBrightShaderColors = {0, Color.BLACK, Color.WHITE, 0};// 随着变化的数据
    private static final float[] BRIGHT_SHADER_POSITIONS = {0f, 0.25f, 0.75f, 1};

    // 取色器基本颜色渲染
    private final Shader mPickerColorShader = new SweepGradient(0, 0, PICKER_COLORS, PICKER_SHADER_POSITIONS);

    // 取色器区域比率
    private static final float PICKER_PERCENT = 0.9f;

    // 立体化边界
    private final float BORDER_WEIGHT;
    // 选择器的半径
    private final float CURSOR_RADIUS;


    private int mWidth;
    private int mHeight;
    private float mDRadius;//一个半径差

    private final PointF mCenter = new PointF();//绘图中心坐标
    private float mPickerRadius;//取色器半径
    private float mPickerBorderRadius;//取色器边框半径
    private final Path mBrightPath = new Path();// 亮度区域
    private float mBrightInnerRadius;// 亮度内径
    private float mBrightOuterXRadius;//亮度区X外径
    private float mBrightOuterYRadius;//亮度区Y外径

    private Shader mPickerGrayShader;// 取色器灰度渲染
    private Shader mBrightShader;   //亮度渲染

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnColorSelectedListener mListener;

    public CircleColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        // setLayerType(LAYER_TYPE_SOFTWARE, mPaint);// 软件渲染

        // 初始化一些长度
        DisplayMetrics dm = getResources().getDisplayMetrics();
        BORDER_WEIGHT = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
        CURSOR_RADIUS = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, dm);

        mPickerColor = Color.GRAY;
        mBrightColor = Color.GRAY;
        initBrightLayer();
    }

    public void setOnColorSelectedListener(OnColorSelectedListener l) {
        mListener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measure(widthMeasureSpec);
        int height = measure(heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (width == 0 || height == 0) {
            return;
        }

        if (mWidth == width && mHeight == height) {
            return;
        }

        mWidth = width;
        mHeight = height;

        initPickerDimens();
        initBrightDimens();

        initPickerLayer();
        invalidate();
    }

    private int measure(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int result;
        if (specMode == MeasureSpec.AT_MOST) {
            result = specSize;
        } else if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = MIN_SIZE;
        }

        return result;
    }

    // 初始化取色器大小参数
    private void initPickerDimens() {
        int left = (int) (getPaddingLeft() + CURSOR_RADIUS * 1.5);
        int right = (int) (getPaddingRight() + CURSOR_RADIUS / 2);
        int top = getPaddingTop();
        int bottom = getPaddingBottom();

        int size = Math.min(mWidth - left - right, mHeight - top - bottom);
        mDRadius = size * (1 - PICKER_PERCENT) / 2f;

        // 取色器范围
        float tempR = size * PICKER_PERCENT / 2f;
        mCenter.x = (mWidth - left - right - size) / 2f + left + tempR;
        mCenter.y = (mHeight - top - bottom - size) / 2f + top + tempR + mDRadius;
        mPickerRadius = tempR - BORDER_WEIGHT;
        mPickerBorderRadius = mPickerRadius + BORDER_WEIGHT / 2;
    }

    // 亮度选择器范围参数
    private void initBrightDimens() {
        mBrightInnerRadius = mPickerRadius + mDRadius;
        mBrightOuterXRadius = mPickerRadius + mDRadius * 2;
        mBrightOuterYRadius = mBrightInnerRadius + CURSOR_RADIUS / 3;

        mBrightPath.reset();
        mBrightPath.moveTo(0, -mBrightInnerRadius);

        RectF tempRectF = new RectF();
        tempRectF.left = -mBrightInnerRadius;
        tempRectF.right = mBrightInnerRadius;
        tempRectF.top = -mBrightInnerRadius;
        tempRectF.bottom = mBrightInnerRadius;
        mBrightPath.addArc(tempRectF, 90, -180);

        tempRectF.left = -mBrightOuterXRadius;
        tempRectF.right = mBrightOuterXRadius;
        tempRectF.top = -mBrightOuterYRadius;
        tempRectF.bottom = mBrightOuterYRadius;
        mBrightPath.addArc(tempRectF, -90, 180);

        mBrightPath.close();

        updateBrightCursorPosition(mBrightCursor.x, mBrightCursor.y);
    }

    private void initPickerLayer() {
        mPickerGrayShader = new RadialGradient(0, 0, mPickerRadius,
                Color.GRAY, Color.TRANSPARENT, Shader.TileMode.CLAMP);

        // 软件重叠模式
        // mPickerColorShader = new ComposeShader(colorShader, grayShader, PorterDuff.Mode.MULTIPLY);
    }

    private void initBrightLayer() {
        mBrightShaderColors[0] = mBrightShaderColors[3] = mPickerColor;
        mBrightShader = new SweepGradient(0, 0, mBrightShaderColors, BRIGHT_SHADER_POSITIONS);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mWidth == 0 || mHeight == 0) {
            return;
        }

        canvas.translate(mCenter.x, mCenter.y);

        mPaint.setStyle(Paint.Style.FILL);

        //取色器圆盘
        mPaint.setShader(mPickerColorShader);
        canvas.drawCircle(0, 0, mPickerRadius, mPaint);
        mPaint.setShader(mPickerGrayShader);
        canvas.drawCircle(0, 0, mPickerRadius, mPaint);

        // 亮度月牙
        mPaint.setShader(mBrightShader);
        canvas.drawPath(mBrightPath, mPaint);

        // 边框立体效果
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setShader(null);

        //边框外环外环
        mPaint.setStrokeWidth(BORDER_WEIGHT);
        mPaint.setColor(0xff8b8f92);
        canvas.drawCircle(0, 0, mPickerBorderRadius, mPaint);//取色器
        canvas.drawCircle(mPickerCursor.x, mPickerCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, mPaint);//取色器游标
        canvas.drawCircle(mBrightCursor.x, mBrightCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, mPaint);//亮度游标

        //边框内环
        mPaint.setStrokeWidth(BORDER_WEIGHT / 2);
        mPaint.setColor(0xffccd0d3);
        canvas.drawCircle(0, 0, mPickerBorderRadius, mPaint);// 取色器
        canvas.drawCircle(mPickerCursor.x, mPickerCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, mPaint);//取色器游标
        canvas.drawCircle(mBrightCursor.x, mBrightCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, mPaint);//亮度游标

        // 游标内色
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mPickerColor);
        canvas.drawCircle(mPickerCursor.x, mPickerCursor.y, CURSOR_RADIUS, mPaint);//取色器游标
        mPaint.setColor(mBrightColor);
        canvas.drawCircle(mBrightCursor.x, mBrightCursor.y, CURSOR_RADIUS, mPaint);//亮度游标

        canvas.restore();
    }

    // 颜色选择部分

    private final PointF mPickerCursor = new PointF(0, 0);
    private final PointF mBrightCursor = new PointF(1.0f, 0);
    private int mPickerColor;//取色器游标中的颜色
    private int mBrightColor;//亮度游标颜色（当前取色器的真实颜色）

    private boolean mIsDownInPicker;
    private boolean mIsDownInBright;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - mCenter.x;
        float y = event.getY() - mCenter.y;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mIsDownInPicker = isInPicker(x, y);
                mIsDownInBright = isInBright(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (mIsDownInPicker) {
                    updatePickerCursorPosition(x, y);
                    pickupPickerColor();
                } else if (mIsDownInBright) {
                    updateBrightCursorPosition(x, y);
                    pickupBrightColor();
                }

                invalidate();

                if (mListener != null) {
                    mListener.onColorSelected(mBrightColor);
                }

                break;
        }

        return mIsDownInPicker || mIsDownInBright || super.onTouchEvent(event);
    }

    private boolean isInPicker(float x, float y) {
        return (x * x + y * y) < (mPickerRadius * mPickerRadius);
    }

    private boolean isInBright(float x, float y) {
        if (x < 0) {
            return false;
        }

        float range = CURSOR_RADIUS;
        float rx = (mBrightInnerRadius + mBrightOuterXRadius) / 2;
        float ry = mBrightInnerRadius;

        float innerRx2 = (rx - range) * (rx - range);
        float innerRy2 = (ry - range) * (ry - range);
        float outerRx2 = (rx + range) * (rx + range);
        float outerRy2 = (ry + range) * (ry + range);

        float xSquare = x * x;
        float ySquare = y * y;

        if (xSquare / innerRx2 + ySquare / innerRy2 < 1) {
            return false;
        }

        if (xSquare / outerRx2 + ySquare / outerRy2 > 1) {
            return false;
        }

        return true;
    }

    private void updatePickerCursorPosition(float x, float y) {
        if (isInPicker(x, y)) {
            mPickerCursor.x = x;
            mPickerCursor.y = y;
        } else {
            // 修正游标在边界的坐标
            float r = (float) Math.sqrt(x * x + y * y);
            float rate = mPickerRadius / r;
            mPickerCursor.x = x * rate;
            mPickerCursor.y = y * rate;

        }
    }

    private void updateBrightCursorPosition(float x, float y) {
        float rx = (mBrightInnerRadius + mBrightOuterXRadius) / 2;
        float ry = (mBrightInnerRadius + mBrightOuterYRadius) / 2;

        if (x <= 0) {
            if (y > mPickerRadius - CURSOR_RADIUS * 3) {
                mIsDownInBright = false;
                mBrightCursor.x = 0;
                mBrightCursor.y = ry;
                return;
            } else if (y < -mPickerRadius + CURSOR_RADIUS * 3) {
                mIsDownInBright = false;
                mBrightCursor.x = 0;
                mBrightCursor.y = -ry;
                return;
            }
            x = -x;
        }

        float rate = x / y;
        float rx2 = rx * rx;
        float ry2 = ry * ry;
        float x0 = (float) Math.sqrt(1 / (1 / rx2 + 1 / (ry2 * rate * rate)));
        float y0 = x0 / rate;

        mBrightCursor.x = x0;
        mBrightCursor.y = y0;

        if (x0 == 0) {
            mIsDownInBright = false;
        }
    }

    // 当前的颜色值
    private void pickupPickerColor() {
        // 计算色相
        float x = mPickerCursor.x;
        float y = mPickerCursor.y;

        float r = (float) Math.sqrt(x * x + y * y);
        double d = Math.asin(y / r);

        if (x <= 0) {
            d = -d + Math.PI;
        } else if (y < 0) {
            d = d + 2 * Math.PI;
        }

        if (d < Math.PI / 6) {
            d += Math.PI * 2 + Math.PI / 6;
        } else {
            d += Math.PI / 6;
        }

        double quotient = d / (Math.PI / 3);
        int index = (int) quotient;
        float percent = (float) (quotient - index);

        int startColor = PICKER_COLORS[index];
        int stopColor;
        if (index == 6) {
            stopColor = PICKER_COLORS[1];
        } else {
            stopColor = PICKER_COLORS[index + 1];
        }

        //色相颜色值
        int hueColor = aveColor(startColor, stopColor, percent);
        // 计算饱和度
        mPickerColor = aveColor(Color.GRAY, hueColor, r / mPickerRadius);

        pickupBrightColor();
        initBrightLayer();
    }

    private void pickupBrightColor() {
        float x = mBrightCursor.x;
        float y = mBrightCursor.y;

        double r = Math.sqrt(x * x + y * y);
        double d = Math.asin(y / r);
        float percent = (float) (d / (Math.PI / 2));

        if (y > 0) {
            mBrightColor = aveColor(mPickerColor, Color.BLACK, percent);
        } else {
            mBrightColor = aveColor(mPickerColor, Color.WHITE, -percent);
        }
    }

    private int aveColor(int c0, int c1, float p) {
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }

    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }

    // 状态存取

    public String getState() {
        State state = new State();

        float x = mPickerCursor.x;
        float y = mPickerCursor.y;

        double r = Math.sqrt(x * x + y * y);
        if (r != 0) {
            double hue = Math.asin(y / r);
            if (x < 0) {
                state.hue = -hue + Math.PI;
            } else {
                state.hue = hue;
            }
        } else {
            state.hue = 0d;
        }

        state.saturation = r / mPickerRadius;

        x = mBrightCursor.x;
        y = mBrightCursor.y;
        state.bright = Math.asin(y / Math.sqrt(x * x + y * y));

        Gson json = new Gson();

        return json.toJson(state);
    }

    public void setState(String stateStr) {
        Gson json = new Gson();
        State state = json.fromJson(stateStr, State.class);

        setPickerCursorPosition(state.hue, state.saturation);
        setBrightCursorPosition(state.bright);

        pickupPickerColor();
        invalidate();
    }

    private void setPickerCursorPosition(double hue, double saturation) {
        double r = saturation * mPickerRadius;
        double x = r * Math.cos(hue);
        double y = r * Math.sin(hue);
        mPickerCursor.x = (float) x;
        mPickerCursor.y = (float) y;
    }

    private void setBrightCursorPosition(double angle) {
        float rx = (mBrightInnerRadius + mBrightOuterXRadius) / 2;
        float ry = (mBrightInnerRadius + mBrightOuterYRadius) / 2;

        if (Math.abs(angle - Math.PI / 2) < 0.001) {
            mBrightCursor.x = 0;
            mBrightCursor.y = ry;
            return;
        } else if (Math.abs(angle + Math.PI / 2) < 0.001) {
            mBrightCursor.x = 0;
            mBrightCursor.y = -ry;
            return;
        }

        float rate = (float) (1 / Math.tan(angle));
        float rx2 = rx * rx;
        float ry2 = ry * ry;
        float x0 = (float) Math.sqrt(1 / (1 / rx2 + 1 / (ry2 * rate * rate)));
        float y0 = x0 / rate;

        mBrightCursor.x = x0;
        mBrightCursor.y = y0;

    }

    private static class State {
        double hue;
        double saturation;
        double bright;
    }

}
