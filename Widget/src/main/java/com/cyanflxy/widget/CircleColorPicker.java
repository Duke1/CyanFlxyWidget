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

import com.cyanflxy.annotation.API;
import com.google.gson.Gson;

/**
 * 取色器
 * <p/>
 * Created by CyanFlxy on 2015/6/17.
 */
public class CircleColorPicker extends View {

    @API
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private static final int MIN_SIZE = 400;

    private static final int[] PICKER_COLORS = {0xFF88FF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000, 0xFFFFFF00, 0xFF88FF00,};
    private static final float[] PICKER_SHADER_POSITIONS = {0, 1 / 12f, 3 / 12f, 5 / 12f, 7 / 12f, 9 / 12f, 11 / 12f, 1.0f};
    private static final float[] BRIGHT_SHADER_POSITIONS = {0f, 0.25f, 0.75f, 1};

    private int[] BrightShaderColors = {0, Color.BLACK, Color.WHITE, 0};// 随着变化的数据

    // 取色器基本颜色渲染
    private final Shader pickerColorShader = new SweepGradient(0, 0, PICKER_COLORS, PICKER_SHADER_POSITIONS);

    // 取色器区域比率
    private static final float PICKER_PERCENT = 0.9f;
    // 立体化边界
    private final float BORDER_WEIGHT;
    // 选择器的半径
    private final float CURSOR_RADIUS;


    private int width;
    private int height;
    private float dRadius;//一个半径差

    private final PointF center = new PointF();//绘图中心坐标
    private float pickerRadius;//取色器半径
    private float pickerBorderRadius;//取色器边框半径
    private final Path brightPath = new Path();// 亮度区域
    private float brightInnerRadius;// 亮度内径
    private float brightOuterXRadius;//亮度区X外径
    private float brightOuterYRadius;//亮度区Y外径

    private Shader pickerGrayShader;// 取色器灰度渲染
    private Shader brightShader;   //亮度渲染

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnColorSelectedListener listener;

    public CircleColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        // setLayerType(LAYER_TYPE_SOFTWARE, paint);// 软件渲染

        // 初始化一些长度
        DisplayMetrics dm = getResources().getDisplayMetrics();
        BORDER_WEIGHT = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
        CURSOR_RADIUS = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, dm);

        pickerColor = Color.GRAY;
        brightColor = Color.GRAY;
        initBrightLayer();
    }

    @API
    public void setOnColorSelectedListener(OnColorSelectedListener l) {
        listener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = measure(widthMeasureSpec);
        int h = measure(heightMeasureSpec);
        setMeasuredDimension(w, h);

        if (w == 0 || h == 0) {
            return;
        }

        if (width == w && height == h) {
            return;
        }

        width = w;
        height = h;

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

        int size = Math.min(width - left - right, height - top - bottom);
        dRadius = size * (1 - PICKER_PERCENT) / 2f;

        // 取色器范围
        float tempR = size * PICKER_PERCENT / 2f;
        center.x = (width - left - right - size) / 2f + left + tempR;
        center.y = (height - top - bottom - size) / 2f + top + tempR + dRadius;
        pickerRadius = tempR - BORDER_WEIGHT;
        pickerBorderRadius = pickerRadius + BORDER_WEIGHT / 2;
    }

    // 亮度选择器范围参数
    private void initBrightDimens() {
        brightInnerRadius = pickerRadius + dRadius;
        brightOuterXRadius = pickerRadius + dRadius * 2;
        brightOuterYRadius = brightInnerRadius + CURSOR_RADIUS / 3;

        brightPath.reset();
        brightPath.moveTo(0, -brightInnerRadius);

        RectF tempRectF = new RectF();
        tempRectF.left = -brightInnerRadius;
        tempRectF.right = brightInnerRadius;
        tempRectF.top = -brightInnerRadius;
        tempRectF.bottom = brightInnerRadius;
        brightPath.addArc(tempRectF, 90, -180);

        tempRectF.left = -brightOuterXRadius;
        tempRectF.right = brightOuterXRadius;
        tempRectF.top = -brightOuterYRadius;
        tempRectF.bottom = brightOuterYRadius;
        brightPath.addArc(tempRectF, -90, 180);

        brightPath.close();

        updateBrightCursorPosition(brightCursor.x, brightCursor.y);
    }

    private void initPickerLayer() {
        pickerGrayShader = new RadialGradient(0, 0, pickerRadius,
                Color.GRAY, Color.TRANSPARENT, Shader.TileMode.CLAMP);

        // 软件重叠模式
        // pickerColorShader = new ComposeShader(colorShader, grayShader, PorterDuff.Mode.MULTIPLY);
    }

    private void initBrightLayer() {
        BrightShaderColors[0] = BrightShaderColors[3] = pickerColor;
        brightShader = new SweepGradient(0, 0, BrightShaderColors, BRIGHT_SHADER_POSITIONS);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (width == 0 || height == 0) {
            return;
        }

        canvas.translate(center.x, center.y);

        paint.setStyle(Paint.Style.FILL);

        //取色器圆盘
        paint.setShader(pickerColorShader);
        canvas.drawCircle(0, 0, pickerRadius, paint);
        paint.setShader(pickerGrayShader);
        canvas.drawCircle(0, 0, pickerRadius, paint);

        // 亮度月牙
        paint.setShader(brightShader);
        canvas.drawPath(brightPath, paint);

        // 边框立体效果
        paint.setStyle(Paint.Style.STROKE);
        paint.setShader(null);

        //边框外环外环
        paint.setStrokeWidth(BORDER_WEIGHT);
        paint.setColor(0xff8b8f92);
        canvas.drawCircle(0, 0, pickerBorderRadius, paint);//取色器
        canvas.drawCircle(pickerCursor.x, pickerCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, paint);//取色器游标
        canvas.drawCircle(brightCursor.x, brightCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, paint);//亮度游标

        //边框内环
        paint.setStrokeWidth(BORDER_WEIGHT / 2);
        paint.setColor(0xffccd0d3);
        canvas.drawCircle(0, 0, pickerBorderRadius, paint);// 取色器
        canvas.drawCircle(pickerCursor.x, pickerCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, paint);//取色器游标
        canvas.drawCircle(brightCursor.x, brightCursor.y,
                CURSOR_RADIUS + BORDER_WEIGHT / 2, paint);//亮度游标

        // 游标内色
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pickerColor);
        canvas.drawCircle(pickerCursor.x, pickerCursor.y, CURSOR_RADIUS, paint);//取色器游标
        paint.setColor(brightColor);
        canvas.drawCircle(brightCursor.x, brightCursor.y, CURSOR_RADIUS, paint);//亮度游标

        canvas.restore();
    }

    // 颜色选择部分

    private final PointF pickerCursor = new PointF(0, 0);
    private final PointF brightCursor = new PointF(1.0f, 0);
    private int pickerColor;//取色器游标中的颜色
    private int brightColor;//亮度游标颜色（当前取色器的真实颜色）

    private boolean isDownInPicker;
    private boolean isDownInBright;

    @API
    public int getColor() {
        return brightColor;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - center.x;
        float y = event.getY() - center.y;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                isDownInPicker = isInPicker(x, y);
                isDownInBright = isInBright(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (isDownInPicker) {
                    updatePickerCursorPosition(x, y);
                    pickupPickerColor();
                } else if (isDownInBright) {
                    updateBrightCursorPosition(x, y);
                    pickupBrightColor();
                }

                invalidate();

                if (listener != null) {
                    listener.onColorSelected(brightColor);
                }

                break;
        }

        return isDownInPicker || isDownInBright || super.onTouchEvent(event);
    }

    private boolean isInPicker(float x, float y) {
        return (x * x + y * y) < (pickerRadius * pickerRadius);
    }

    private boolean isInBright(float x, float y) {
        if (x < 0) {
            return false;
        }

        float range = CURSOR_RADIUS;
        float rx = (brightInnerRadius + brightOuterXRadius) / 2;
        float ry = brightInnerRadius;

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
            pickerCursor.x = x;
            pickerCursor.y = y;
        } else {
            // 修正游标在边界的坐标
            float r = (float) Math.sqrt(x * x + y * y);
            float rate = pickerRadius / r;
            pickerCursor.x = x * rate;
            pickerCursor.y = y * rate;

        }
    }

    private void updateBrightCursorPosition(float x, float y) {
        float rx = (brightInnerRadius + brightOuterXRadius) / 2;
        float ry = (brightInnerRadius + brightOuterYRadius) / 2;

        if (x <= 0) {
            if (y > pickerRadius - CURSOR_RADIUS * 3) {
                isDownInBright = false;
                brightCursor.x = 0;
                brightCursor.y = ry;
                return;
            } else if (y < -pickerRadius + CURSOR_RADIUS * 3) {
                isDownInBright = false;
                brightCursor.x = 0;
                brightCursor.y = -ry;
                return;
            }
            x = -x;
        }

        float rate = x / y;
        float rx2 = rx * rx;
        float ry2 = ry * ry;
        float x0 = (float) Math.sqrt(1 / (1 / rx2 + 1 / (ry2 * rate * rate)));
        float y0 = x0 / rate;

        brightCursor.x = x0;
        brightCursor.y = y0;

        if (x0 == 0) {
            isDownInBright = false;
        }
    }

    // 当前的颜色值
    private void pickupPickerColor() {
        // 计算色相
        float x = pickerCursor.x;
        float y = pickerCursor.y;

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
        pickerColor = aveColor(Color.GRAY, hueColor, r / pickerRadius);

        pickupBrightColor();
        initBrightLayer();
    }

    private void pickupBrightColor() {
        float x = brightCursor.x;
        float y = brightCursor.y;

        double r = Math.sqrt(x * x + y * y);
        double d = Math.asin(y / r);
        float percent = (float) (d / (Math.PI / 2));

        if (y > 0) {
            brightColor = aveColor(pickerColor, Color.BLACK, percent);
        } else {
            brightColor = aveColor(pickerColor, Color.WHITE, -percent);
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

    @API
    public String getState() {
        State state = new State();

        float x = pickerCursor.x;
        float y = pickerCursor.y;

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

        state.saturation = r / pickerRadius;

        x = brightCursor.x;
        y = brightCursor.y;
        state.bright = Math.asin(y / Math.sqrt(x * x + y * y));

        Gson json = new Gson();

        return json.toJson(state);
    }

    @API
    public void setState(String stateStr) {
        Gson json = new Gson();
        State state = json.fromJson(stateStr, State.class);

        setPickerCursorPosition(state.hue, state.saturation);
        setBrightCursorPosition(state.bright);

        pickupPickerColor();
        invalidate();
    }

    private void setPickerCursorPosition(double hue, double saturation) {
        double r = saturation * pickerRadius;
        double x = r * Math.cos(hue);
        double y = r * Math.sin(hue);
        pickerCursor.x = (float) x;
        pickerCursor.y = (float) y;
    }

    private void setBrightCursorPosition(double angle) {
        float rx = (brightInnerRadius + brightOuterXRadius) / 2;
        float ry = (brightInnerRadius + brightOuterYRadius) / 2;

        if (Math.abs(angle - Math.PI / 2) < 0.001) {
            brightCursor.x = 0;
            brightCursor.y = ry;
            return;
        } else if (Math.abs(angle + Math.PI / 2) < 0.001) {
            brightCursor.x = 0;
            brightCursor.y = -ry;
            return;
        }

        float rate = (float) (1 / Math.tan(angle));
        float rx2 = rx * rx;
        float ry2 = ry * ry;
        float x0 = (float) Math.sqrt(1 / (1 / rx2 + 1 / (ry2 * rate * rate)));
        float y0 = x0 / rate;

        brightCursor.x = x0;
        brightCursor.y = y0;

    }

    private static class State {
        double hue;
        double saturation;
        double bright;
    }

}
