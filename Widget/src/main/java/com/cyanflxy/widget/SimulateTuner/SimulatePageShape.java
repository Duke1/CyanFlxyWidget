package com.cyanflxy.widget.SimulateTuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;

import com.cyanflxy.widget.SimulateTuner.GeometryTool.Bezier;
import com.cyanflxy.widget.SimulateTuner.GeometryTool.Line;


/**
 * 仿真页面上的图形.
 */
public class SimulatePageShape {
    private static final int TOUCH_LEFT = 1;
    private static final int TOUCH_RIGHT = 2;

    private static final int START_TOP = 1;//顶部
    private static final int START_MIDDLE = 2;//中部
    private static final int START_BOTTOM = 3;//底部

    public static final int DAY_MODE = 0;
    public static final int NIGHT_MODE = 1;

    private final float SHADOW_MAX_WIDTH;
    private final Path mShadowXPath = new Path();
    private final Path mShadowYPath = new Path();

    // 用于让翻页效果更流畅和正常
    private static final float MIN_CORNER_LEFT = 0.25f;
    private boolean isTurnPrevStart;

    private float mWidth;
    private float mHeight;

    private int mTouchType;
    private int mStartType;

    private final PointF mStartPoint = new PointF();
    private final PointF mCurrentPoint = new PointF();
    // 绘图时的起始点和终止点
    private final PointF mCalStartPoint = new PointF();
    private final PointF mCalDestPoint = new PointF();

    private Bezier bezier1 = new Bezier();
    private Bezier bezier2 = new Bezier();
    private final Path mBezierPath = new Path();//翻开部分区域
    private Path mCornerPath = new Path();// //正在翻开的折角的绘图区
    private Line mMiddleLine;// 翻起的角与原位置之间的中线

    public SimulatePageShape(Context c) {
        SHADOW_MAX_WIDTH = dip2px(c, 10);
    }

    public void setSize(float w, float h) {
        mWidth = w;
        mHeight = h;

        mShadowXPath.lineTo(w + h, 0);
        mShadowXPath.lineTo(w + h, -SHADOW_MAX_WIDTH);
        mShadowXPath.lineTo(-SHADOW_MAX_WIDTH, -SHADOW_MAX_WIDTH);
        mShadowXPath.close();

        mShadowYPath.lineTo(0, w + h);
        mShadowYPath.lineTo(-SHADOW_MAX_WIDTH, w + h);
        mShadowYPath.lineTo(-SHADOW_MAX_WIDTH, -SHADOW_MAX_WIDTH);
        mShadowYPath.close();

    }

    public void setOnTouchDownPoint(float x, float y) {
        float startX;
        float startY;

        if (y < mHeight * 0.25f) {
            startY = 0;
            mStartType = START_TOP;
        } else if (y > mHeight * 0.75f) {
            startY = mHeight;
            mStartType = START_BOTTOM;
        } else {
            startY = y;
            mStartType = START_MIDDLE;
        }

        if (x > mWidth / 2) {
            // 下一页
            mTouchType = TOUCH_RIGHT;
            startX = mWidth;
        } else {
            //上一页
            isTurnPrevStart = true;
            mTouchType = TOUCH_LEFT;
            startX = 0;
            startY = y;
        }

        mStartPoint.set(startX, startY);

        setOnTouch(x, y);

    }

    public void setOnTouch(float x, float y) {
        mCurrentPoint.set(x, y);
        if (mTouchType == TOUCH_LEFT) {
            // 翻上一页时，左侧小部分使用水平直翻
            if (x < mWidth * MIN_CORNER_LEFT && isTurnPrevStart) {
                mStartPoint.y = y;
            } else {
                isTurnPrevStart = false;
            }
            // 横向修正手指在最左侧的时候，上一页尚在y轴左侧0.25个宽度处
            mCurrentPoint.x += mWidth * 0.25f * (1 - x / mWidth);
        }

    }

    private void clearPath() {
        mBezierPath.reset();
        mCornerPath.reset();
    }

    public void calculateShape() {
        clearPath();

        if (mTouchType == TOUCH_RIGHT) {
            if (mStartType == START_MIDDLE) {
                calculateMiddle();
            } else {
                mCalStartPoint.set(mStartPoint);
                mCalDestPoint.set(mCurrentPoint);
                calculate();
            }
        } else {
            calculateTurnPrev();
        }

    }

    private void calculateMiddle() {
        Line middleLine = Line.createMiddleLine(mStartPoint, mCurrentPoint);
        if (middleLine.isVertical()) {
            calculateVertical(middleLine.getX(0));
            return;
        }

        if (mCurrentPoint.y > mStartPoint.y) {
            // 向下方倾斜
            mCalStartPoint.set(mWidth, 0);
        } else {
            // 向上方倾斜
            mCalStartPoint.set(mWidth, mHeight);
        }

        float x0 = mCalStartPoint.x;
        float y0 = middleLine.getY(x0);

        float x = x0 - (mCalStartPoint.y - y0) / (mStartPoint.y - y0) * (x0 - mCurrentPoint.x);
        float y = y0 + (mCalStartPoint.y - y0) / (mStartPoint.y - y0) * (mCurrentPoint.y - y0);
        mCalDestPoint.set(x, y);

        calculate();
    }

    private void calculateTurnPrev() {
        if (mCurrentPoint.y == mStartPoint.y) {
            calculateVertical(mCurrentPoint.x);
            return;
        }
        if (mCurrentPoint.y < mStartPoint.y) {
            // 向下方倾斜
            mCalStartPoint.set(mWidth, 0);
        } else {
            // 向上方倾斜
            mCalStartPoint.set(mWidth, mHeight);
        }

        Line verticalLine = Line.createVerticalLine(mCurrentPoint, mStartPoint);
        float y1 = verticalLine.getY(0);
        float y2 = verticalLine.getY(mWidth);

        float x = mWidth - (mCurrentPoint.x / (mStartPoint.y - y1) * (y2 - mCalStartPoint.y)) * 2;
        float y = mCalStartPoint.y - (mCalStartPoint.y - y2 + (mCurrentPoint.y - y1) / (mStartPoint.y - y1) * (y2 - mCalStartPoint.y)) * 2;
        mCalDestPoint.set(x, y);

        calculate();
    }

    private void correctDestPoint(float cx, float cy, float r) {
        if (cx == mCalStartPoint.x) {
            if (cy == 0) {
                mCalDestPoint.set(cx, cy + r);
            } else {
                mCalDestPoint.set(cx, cy - r);
            }
        } else {
            float slope = (cy - mCalDestPoint.y) / (cx - mCalDestPoint.x);//斜率
            float slopeDegree = (float) Math.atan(slope);
            float sin = (float) Math.sin(slopeDegree);

            float y = sin * r * Math.signum(mCalDestPoint.x - cx);
            float x = Math.abs(y / slope) * Math.signum(mCalDestPoint.x - cx);
            mCalDestPoint.set(cx + x, cy + y);
        }
    }

    /**
     * 计算折角的几个区域
     */
    private void calculate() {
        // 轴距（目标点和左上或左下角的距离）不能超过页宽的90%
        float axisLen = (float) Math.hypot(mCalDestPoint.x - mWidth * MIN_CORNER_LEFT, mCalDestPoint.y - mCalStartPoint.y);
        if (axisLen > mWidth * (1 - MIN_CORNER_LEFT)) {
            correctDestPoint(mWidth * MIN_CORNER_LEFT, mCalStartPoint.y, mWidth * (1 - MIN_CORNER_LEFT));
        }

        // 两点间的中线
        mMiddleLine = Line.createMiddleLine(mCalStartPoint, mCalDestPoint);
        if (mMiddleLine.isVertical()) {
            calculateVertical(mMiddleLine.getX(0));
            return;
        }

        // 中线与边框的焦点
        float y1 = mCalStartPoint.y;
        float x1 = mMiddleLine.getX(y1);
        float x2 = mCalStartPoint.x;
        float y2 = mMiddleLine.getY(x2);

        float len1 = mCalStartPoint.x - x1;
        float len2 = mCalStartPoint.y - y2;

        // 和上下边框相交的贝塞尔曲线
        float bezier1Start = Math.max(x1 - len1 / 2, 0);
        bezier1.start.set(bezier1Start, y1);
        bezier1.control.set(x1, y1);
        bezier1.end.set((x1 + mCalDestPoint.x) / 2, (y1 + mCalDestPoint.y) / 2);
        bezier1.calculateVertex();

        // 和右边框相交的贝塞尔曲线
        bezier2.start.set((x2 + mCalDestPoint.x) / 2, (y2 + mCalDestPoint.y) / 2);
        bezier2.control.set(x2, y2);
        bezier2.end.set(x2, y2 - len2 / 2);
        bezier2.calculateVertex();

        //包含翻起部分和底页部分
        mBezierPath.moveTo(bezier1.start.x, bezier1.start.y);
        mBezierPath.quadTo(bezier1.control.x, bezier1.control.y, bezier1.end.x, bezier1.end.y);
        mBezierPath.lineTo(mCalDestPoint.x, mCalDestPoint.y);
        mBezierPath.lineTo(bezier2.start.x, bezier2.start.y);
        mBezierPath.quadTo(bezier2.control.x, bezier2.control.y, bezier2.end.x, bezier2.end.y);
        mBezierPath.lineTo(mCalStartPoint.x, mCalStartPoint.y);
        mBezierPath.close();

        // 包含翻起部分和多余部分
        mCornerPath.moveTo(bezier2.vertex.x, bezier2.vertex.y);
        mCornerPath.lineTo(bezier1.vertex.x, bezier1.vertex.y);
        mCornerPath.lineTo(mCalDestPoint.x, mCalDestPoint.y);
        mCornerPath.close();
    }

    private void calculateVertical(float x) {
        float left = mWidth - (mWidth - x) * 2;

        float bezierLen = (mWidth - x) / 2;
        float bezierStart = x - bezierLen;
        float bezierVertex = (bezierStart + x) / 2;

        mCalStartPoint.set(mWidth, 0);
        mCalDestPoint.set(left, 0);
        mMiddleLine = Line.createVerticalLine(x);

        mBezierPath.addRect(left, 0, mWidth, mHeight, Path.Direction.CW);
        mCornerPath.addRect(left, 0, bezierVertex, mHeight, Path.Direction.CW);

    }

    public boolean prepareTop(Canvas canvas) {
        return canvas.clipPath(mBezierPath, Region.Op.XOR);
    }

    public Matrix prepareCorner(Canvas canvas) {
        boolean draw = canvas.clipPath(mBezierPath) &&
                canvas.clipPath(mCornerPath, Region.Op.INTERSECT);
        if (!draw) {
            return null;
        }

        canvas.translate(mCalDestPoint.x, mCalDestPoint.y);
        canvas.rotate(2 * mMiddleLine.getSlopeDegree() - 180);

        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        matrix.postTranslate(mWidth, -mCalStartPoint.y);

        return matrix;
    }

    public boolean prepareBottom(Canvas canvas) {
        return canvas.clipPath(mBezierPath) &&
                canvas.clipPath(mCornerPath, Region.Op.DIFFERENCE);
    }

    /**
     * 绘制阴影.
     * 阴影分为
     * <ul>
     * <li>边：正在翻页绘图区折起的两条边</li>
     * <li>角：折起的角部（绘制背面图片部分）</li>
     * <li>弦：过两条贝塞尔曲线顶点的直线两侧(折边)</li>
     * </ul>
     *
     * @param canvas 绘图区
     * @param mode   白天模式还是夜晚模式
     */
    public void drawShadow(Canvas canvas, int mode) {
        if (mMiddleLine.isVertical()) {
            drawVerticalShadow(canvas, mode);
        } else if (mCalStartPoint.y == 0) {
            drawShadow(canvas, mode, 1, -90);
        } else {
            drawShadow(canvas, mode, -1, 90);
        }
    }

    private void drawVerticalShadow(Canvas canvas, int mode) {
        Shadow shadow = Shadow.getShadow(mode);

        float cornerLen = (float) Math.hypot(mCalDestPoint.x - mCalStartPoint.x, mCalDestPoint.y - mCalStartPoint.y);
        int edgeShadowSize = (int) Math.min(cornerLen / 2, SHADOW_MAX_WIDTH);
        shadow.mEdgeShadowGradientY.setBounds((int) (mCalDestPoint.x - edgeShadowSize), 0, (int) mCalDestPoint.x, (int) mHeight);
        shadow.mEdgeShadowGradientY.draw(canvas);

        int edgefoldStart = (int) (mCalDestPoint.x + cornerLen * 0.375);
        int edgefoldShadowSize = (int) (cornerLen / 6);
        shadow.mEdgefoldShadowGradient.setBounds(edgefoldStart - edgefoldShadowSize, 0, edgefoldStart + edgefoldShadowSize, (int) mHeight);
        shadow.mEdgefoldShadowGradient.draw(canvas);

        int cornerShadowSize = (int) (cornerLen * 0.22);
        shadow.mCornerShadowGradient.setBounds((int) mCalDestPoint.x, 0, (int) mCalDestPoint.x + cornerShadowSize, (int) mHeight);
        shadow.mCornerShadowGradient.draw(canvas);
    }

    private void drawShadow(Canvas canvas, int mode, int edgeScaleY, int rotateCorner) {
        Shadow shadow = Shadow.getShadow(mode);
        float cornerLen = (float) Math.hypot(mCalDestPoint.x - mCalStartPoint.x, mCalDestPoint.y - mCalStartPoint.y);
        Line edge = Line.createLine(bezier1.vertex, bezier2.vertex);

        canvas.save();
        if (canvas.clipPath(mBezierPath, Region.Op.XOR)) {
            canvas.translate(mCalDestPoint.x, mCalDestPoint.y);
            canvas.rotate(2 * mMiddleLine.getSlopeDegree() - 180);
            canvas.scale(1, edgeScaleY);

            int edgeShadowSize = (int) Math.min(cornerLen / 2, SHADOW_MAX_WIDTH);
            int edgeShadowLength = (int) Math.max(mWidth, mHeight);

            canvas.save();
            if (canvas.clipPath(mShadowXPath)) {
                shadow.mEdgeShadowGradientX.setBounds(-edgeShadowSize, -edgeShadowSize, edgeShadowLength, 0);
                shadow.mEdgeShadowGradientX.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            if (canvas.clipPath(mShadowYPath)) {
                shadow.mEdgeShadowGradientY.setBounds(-edgeShadowSize, -edgeShadowSize, 0, edgeShadowLength);
                shadow.mEdgeShadowGradientY.draw(canvas);
            }
            canvas.restore();
        }
        canvas.restore();


        canvas.save();
        if (canvas.clipPath(mBezierPath)) {
            canvas.translate(edge.getX(mCalStartPoint.y), mCalStartPoint.y);
            canvas.rotate(edge.getSlopeDegree() + rotateCorner);

            int edgefoldShadowSize = (int) (cornerLen / 6);
            int edgefoldShadowLength = (int) (mWidth + mHeight);
            shadow.mEdgefoldShadowGradient.setBounds(-edgefoldShadowSize, -edgefoldShadowLength, edgefoldShadowSize, edgefoldShadowLength);
            shadow.mEdgefoldShadowGradient.draw(canvas);
        }
        canvas.restore();


        canvas.save();
        if (canvas.clipPath(mBezierPath)) {
            canvas.translate(mCalDestPoint.x, mCalDestPoint.y);
            canvas.rotate(edge.getSlopeDegree() + rotateCorner);

            int cornerShadowSize = (int) (cornerLen * 0.22);
            int cornerShadowLen = (int) (mHeight);
            shadow.mCornerShadowGradient.setBounds(0, -cornerShadowLen, cornerShadowSize, cornerShadowLen);
            shadow.mCornerShadowGradient.draw(canvas);
        }

        canvas.restore();

    }

    public void destroy() {
        Shadow.destroy();
    }

    private static class Shadow {

        private static Shadow[] sShadows = new Shadow[2];

        public static Shadow getShadow(int mode) {
            if (sShadows[mode] == null) {
                sShadows[mode] = new Shadow(mode);
            }
            return sShadows[mode];
        }

        public static void destroy() {
            sShadows[0] = null;
            sShadows[1] = null;
        }

        //折起的两条边
        private static final int[][] EDGE_SHADOW_COLORS = {{0x80454545, 0x00454545}, {0x80151515, 0x00151515}};
        // 折边直线上的阴影
        private static final int[][] EDGEFOLD_SHADOW_COLORS = {{0x00454545, 0x80454545, 0x00454545}, {0x00151515, 0x80151515, 0x00151515}};
        //三角区
        private static final int[][] CORNER_SHADOW_COLORS = {{0x80454545, 0x00454545}, {0x80151515, 0x00151515}};

        private GradientDrawable mEdgeShadowGradientX; //X轴上的阴影
        private GradientDrawable mEdgeShadowGradientY; //Y轴上的阴影
        private GradientDrawable mEdgefoldShadowGradient;//折边阴影
        private GradientDrawable mCornerShadowGradient;//三角区阴影

        private Shadow(int mode) {
            mEdgeShadowGradientX = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, EDGE_SHADOW_COLORS[mode]);
            mEdgeShadowGradientX.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            mEdgeShadowGradientY = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, EDGE_SHADOW_COLORS[mode]);
            mEdgeShadowGradientY.setGradientType(GradientDrawable.LINEAR_GRADIENT);

            mEdgefoldShadowGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, EDGEFOLD_SHADOW_COLORS[mode]);
            mEdgefoldShadowGradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);

            mCornerShadowGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, CORNER_SHADOW_COLORS[mode]);
            mCornerShadowGradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        }

    }

    /*package*/
    static int dip2px(Context c, float dpValue) {
        float scale = c.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
