package com.cyanflxy.widget.SimulateTuner;

import android.graphics.PointF;

/**
 * 几何工具
 */
public class GeometryTool {

    public static class Line {

        public static Line createMiddleLine(PointF a0, PointF b0) {
            float a = 2 * (b0.x - a0.x);
            float b = 2 * (b0.y - a0.y);
            float c = b0.y * b0.y + b0.x * b0.x - a0.x * a0.x - a0.y * a0.y;
            return new Line(a, b, c);
        }

        /**
         * 创建过两点p1,p2的直线
         */
        public static Line createLine(PointF p1, PointF p2) {
            float a = p1.y - p2.y;
            float b = p2.x - p1.x;
            float c = p1.x * (p1.y - p2.y) - p1.y * (p1.x - p2.x);

            return new Line(a, b, c);
        }

        /**
         * 创建经过o点垂直于oe的直线
         */
        public static Line createVerticalLine(PointF o, PointF e) {
            float a = 2 * (o.x - e.x);
            float b = 2 * (o.y - e.y);
            float c = 2 * (o.x * o.x + o.y * o.y - o.x * e.x - o.y * e.y);
            return new Line(a, b, c);
        }

        /**
         * 创建垂直于X轴的直线
         *
         * @param x 垂线x坐标
         */
        public static Line createVerticalLine(float x) {
            return new Line(1, 0, x);
        }

        // ax+by=c; 使用这种形式可以很好的处理垂直或水平的情况（只需要a或者b是0）
        private float a;
        private float b;
        private float c;

        private Line(float a, float b, float c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public float getY(float x) {
            if (isVertical()) {
                return Float.NaN;
            }
            return (c - a * x) / b;
        }

        public float getX(float y) {
            if (isHorizontal()) {
                return Float.NaN;
            }
            return (c - b * y) / a;
        }

        public boolean isHorizontal() {
            return Float.compare(a, 0) == 0;
        }

        public boolean isVertical() {
            return Float.compare(b, 0) == 0;
        }

        // 斜率
        public float getSlopeDegree() {
            if (isVertical()) {
                return 90;
            } else {
                return (float) (Math.atan(-a / b) / Math.PI * 180);
            }
        }
    }


    /**
     * 贝塞尔曲线
     */
    public static class Bezier {
        /**
         * 起始点
         */
        public PointF start = new PointF();

        /**
         * 控制点
         */
        public PointF control = new PointF();

        /**
         * 顶点
         */
        public PointF vertex = new PointF();

        /**
         * 结束点
         */
        public PointF end = new PointF();

        public void calculateVertex() {
            float x = start.x * 0.25f + end.x * 0.25f + control.x * 0.5f;
            float y = start.y * 0.25f + end.y * 0.25f + control.y * 0.5f;
            vertex.set(x, y);
        }

    }
}
