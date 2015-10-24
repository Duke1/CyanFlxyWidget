package com.cyanflxy.widget.SimulateTuner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * 仿真翻页，翻页效果图片绘制<br>
 * 用法：
 * <ol>
 * <li>实现接口 {@link OnBitmapTurnListener}，并设置：{@link #setOnBitmapTurnListener(OnBitmapTurnListener)}</li>
 * <li>提供手势动作 {@link #onTouchEvent(MotionEvent event)}</li>
 * <li>在重绘回调{@link OnBitmapTurnListener#invalidatePage()}中调用{@link #draw(Canvas)}</li>
 * <li>提供页面大小{@link #setSize(int w, int h)}</li>
 * <li>提供翻页背景色 {@link #setBackgroundColor(int color)}</li>
 * <li>设置白天模式还是夜晚模式：{@link #setDayMode()} {@link #setNightMode()} 以便使用合适的阴影色值</li>
 * <li>结束后调用{@link #destroy()}回收图片缓存</li>
 */
public class SimulatePageTurner {

    /**
     * 翻到上一页
     */
    public static final int TURN_TYPE_PREV = 1;
    /**
     * 翻到下一页
     */
    public static final int TURN_TYPE_NEXT = -1;

    /**
     * 翻页成功
     */
    public static final int TURN_SUCCESS = 1;
    /**
     * 没有翻页
     */
    public static final int TURN_FAIL = 0;

    public interface OnBitmapTurnListener {
        Bitmap getTopBitmap();

        Bitmap getBottomBitmap();

        /**
         * 通知翻页状态
         * {@link #TURN_TYPE_PREV}, {@link #TURN_TYPE_NEXT}
         *
         * @param type 翻到上一页或者下一页
         */
        void setTurnType(int type);

        void invalidatePage();

        /**
         * 通知翻页结果:
         *
         * @param turnType   翻到上一页，下一页或者是没有翻页, {@link #TURN_TYPE_PREV}, {@link #TURN_TYPE_NEXT}
         * @param turnResult 翻页是否成功, {@link #TURN_SUCCESS}, {@link #TURN_FAIL}
         */
        void onTurningEnd(int turnType, int turnResult);
    }

    private static final int AUTO_FLIP_INTERVAL = 10;// 自动翻页效果动画毫秒
    private float mDefaultFlipSpeed = 1200;// px/s 依赖像素密度！！
    private float mAutoFlipSpeed;

    private Bitmap mTopBitmap;
    private Bitmap mBottomBitmap;
    private int mBackgroundColor;
    private int mDayMode = SimulatePageShape.DAY_MODE;

    private OnBitmapTurnListener mOnBitmapTurnListener;
    private SimulatePageShape mSimulatePageShape;
    private GestureDetector mGestureDetector;
    private AutoFlipHandler mHandler;

    private int mWidth;
    private int mTurnType;
    private int mTurnResult;
    private boolean isTurning;
    private boolean isSendTurnType;

    private float mLastTouchX;
    private float mLastDx;
    private boolean isFirstMove;

    public SimulatePageTurner(Context c) {
        mSimulatePageShape = new SimulatePageShape(c);
        mGestureDetector = new GestureDetector(c, onGestureListener);
        mHandler = new AutoFlipHandler(this);

        mDefaultFlipSpeed = SimulatePageShape.dip2px(c, mDefaultFlipSpeed);
        mAutoFlipSpeed = mDefaultFlipSpeed;
    }

    public void setOnBitmapTurnListener(OnBitmapTurnListener l) {
        mOnBitmapTurnListener = l;
    }

    public void setSize(int w, int h) {
        mWidth = w;
        mSimulatePageShape.setSize(w, h);
        mHandler.setWidth(w);
    }

    public void draw(Canvas canvas) {

        if (isTurning) {

            mSimulatePageShape.calculateShape();

            // 底部图片
            canvas.save();
            if (mSimulatePageShape.prepareBottom(canvas)) {
                canvas.drawBitmap(mBottomBitmap, 0, 0, null);
            }
            canvas.restore();

            //顶部图片
            canvas.save();
            if (mSimulatePageShape.prepareTop(canvas)) {
                canvas.drawBitmap(mTopBitmap, 0, 0, null);
            }
            canvas.restore();

            // 翻角部分
            canvas.save();
            Matrix matrix = mSimulatePageShape.prepareCorner(canvas);
            if (matrix != null) {
                canvas.drawColor(mBackgroundColor | 0xFF000000);
                canvas.drawBitmap(mTopBitmap, matrix, null);
                canvas.drawColor(mBackgroundColor);
            }
            canvas.restore();

            mSimulatePageShape.drawShadow(canvas, mDayMode);
        } else {
            if (mOnBitmapTurnListener != null) {
                mTopBitmap = mOnBitmapTurnListener.getTopBitmap();
                mBottomBitmap = mOnBitmapTurnListener.getBottomBitmap();

                if (mTopBitmap != null) {
                    canvas.drawBitmap(mTopBitmap, 0, 0, null);
                } else if (mBottomBitmap != null) {
                    canvas.drawBitmap(mBottomBitmap, 0, 0, null);
                }
            }
        }
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
    }

    public void setDayMode() {
        mDayMode = SimulatePageShape.DAY_MODE;
    }

    public void setNightMode() {
        mDayMode = SimulatePageShape.NIGHT_MODE;
    }

    public boolean onTouchEvent(MotionEvent event) {
        // 手势检测单击效果
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isSendTurnType = false;
                onTouchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                onTouchUp(x, y);
                break;
        }
        return true;
    }

    public void destroy() {
        mSimulatePageShape.destroy();
    }

    private void onTouchDown(float x, float y) {
        mSimulatePageShape.setOnTouchDownPoint(x, y);
        mLastTouchX = x;
        isFirstMove = true;
    }

    private void onTouchMove(float x, float y) {
        mSimulatePageShape.setOnTouch(x, y);
        float dx = x - mLastTouchX;

        if (isFirstMove) {
            int sig = (int) Math.signum(dx);
            if (sig != 0) {
                isFirstMove = false;
                onTurn(sig);
                isTurning = canTurn();
            }
        } else {
            if (dx != 0) {
                mLastDx = dx;
            }
        }

        mLastTouchX = x;
        if (isTurning) {
            mOnBitmapTurnListener.invalidatePage();
        }

    }

    private void onTouchUp(float x, float y) {
        mSimulatePageShape.setOnTouch(x, y);

        int dir = (int) Math.signum(mLastDx);
        if (dir == mTurnType) {
            mTurnResult = TURN_SUCCESS;
        } else {
            mTurnResult = TURN_FAIL;
        }
        mHandler.start(x, y, dir);
    }

    private void onEnd() {
        mAutoFlipSpeed = mDefaultFlipSpeed;
        isTurning = false;
        if (mOnBitmapTurnListener != null) {
            mOnBitmapTurnListener.invalidatePage();
        }
    }

    private void sendTurnResult() {
        if (mOnBitmapTurnListener != null) {
            mOnBitmapTurnListener.onTurningEnd(mTurnType, mTurnResult);
        }
    }

    private void onTurn(int turnType) {
        mTurnType = turnType;
        if (mOnBitmapTurnListener != null) {
            if (!isSendTurnType) {
                isSendTurnType = true;
                mOnBitmapTurnListener.setTurnType(turnType);
                mTopBitmap = mOnBitmapTurnListener.getTopBitmap();
                mBottomBitmap = mOnBitmapTurnListener.getBottomBitmap();
            }
        }
    }

    private boolean canTurn() {
        return mTopBitmap != null && mBottomBitmap != null;
    }

    //手势处理
    @SuppressWarnings("FieldCanBeLocal")
    private GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            mTurnResult = TURN_SUCCESS;
            if (x < mWidth / 2) {
                onTurn(TURN_TYPE_PREV);
                mHandler.start(x, y, 1);
            } else {
                onTurn(TURN_TYPE_NEXT);
                mHandler.start(x, y, -1);
            }

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float speed = Math.abs(velocityX);
            if (speed > mDefaultFlipSpeed * 0.9f) {
                mAutoFlipSpeed = speed;
            }
            return false;
        }
    };

    private static class AutoFlipHandler extends Handler {

        private int mWidth;
        private Reference<SimulatePageTurner> mTunerReference;

        private float mTouchX;
        private float mTouchY;
        private int mDirect;
        private long mLastTime;

        public AutoFlipHandler(SimulatePageTurner turner) {
            mTunerReference = new SoftReference<SimulatePageTurner>(turner);
        }

        public void setWidth(int w) {
            mWidth = w;
        }

        public void start(float x, float y, int direct) {
            SimulatePageTurner turner = mTunerReference.get();
            if (turner == null) {
                return;
            }

            turner.isTurning = turner.canTurn();
            if (!turner.isTurning) {
                return;
            }

            turner.sendTurnResult();
            turner.isTurning = true;

            mTouchX = x;
            mTouchY = y;
            mDirect = direct;
            mLastTime = System.currentTimeMillis();

            sendEmptyMessage(0);
        }

        @Override
        public void handleMessage(Message msg) {
            SimulatePageTurner turner = mTunerReference.get();
            if (turner == null) {
                return;
            }

            long current = System.currentTimeMillis();
            long interval = current - mLastTime;
            int dx = (int) (interval * turner.mAutoFlipSpeed / 1000f);
            mTouchX = mTouchX + dx * mDirect;

            int minLeft = -mWidth;
            if(mDirect < 0 && turner.mTurnType == TURN_TYPE_PREV){
                minLeft = 0;
            }

            if (mTouchX > minLeft && mTouchX < mWidth) {
                mLastTime = current;
                turner.onTouchMove(mTouchX, mTouchY);
                sendEmptyMessageDelayed(0, AUTO_FLIP_INTERVAL);
            } else {
                turner.onEnd();
            }

        }
    }

}
