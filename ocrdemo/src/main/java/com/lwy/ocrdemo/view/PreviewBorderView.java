package com.lwy.ocrdemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by lwy on 2018/7/4.
 */

public class PreviewBorderView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String DEFAULT_TIPS_TEXT = "请将身份证照片置于框内,并尽量对齐边框";
    private static final int DEFAULT_TIPS_TEXT_SIZE = 30;
    private static final int DEFAULT_TIPS_TEXT_COLOR = Color.GREEN;
    private int mScreenH;
    private int mScreenW;
    private Canvas mCanvas;
    private Paint mPaint;
    private Paint mPaintLine;
    private SurfaceHolder mHolder;
    private Thread mThread;
    /**
     * 自定义属性
     */
    private float tipTextSize;
    private int tipTextColor;
    private String tipText;
    private Paint mTextPaint;

    public PreviewBorderView(Context context) {
        this(context, null);
    }

    public PreviewBorderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewBorderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        init();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        tipTextSize = DEFAULT_TIPS_TEXT_SIZE;
        tipTextColor = DEFAULT_TIPS_TEXT_COLOR;
        if (tipText == null) {
            tipText = DEFAULT_TIPS_TEXT;
        }
    }

    /**
     * 初始化绘图变量
     */
    private void init() {
        this.mHolder = getHolder();
        this.mHolder.addCallback(this);
        this.mHolder.setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
        setZOrderMediaOverlay(true);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(Color.WHITE);
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mPaintLine = new Paint();
        this.mPaintLine.setColor(Color.BLUE);
        this.mPaintLine.setStrokeWidth(5.0F);

        this.mTextPaint = new Paint();
        this.mTextPaint.setColor(Color.WHITE);
        this.mTextPaint.setStrokeWidth(3.0F);

        setKeepScreenOn(true);
    }

    /**
     * 绘制取景框
     */
    private void draw() {
        try {
            this.mCanvas = this.mHolder.lockCanvas();
            this.mCanvas.drawARGB(100, 0, 0, 0);
            // 1.6:1 -> width:height
            //            this.mScreenW = (this.mScreenH * 4 / 3);
            float left;
            float top;
            float right;
            float bottom;
            float reactHeight;
            float reactWidth;
            float rate = 2 / 3f;
            if (this.mScreenW > this.mScreenH) {
                // 横屏
                reactHeight = this.mScreenH * rate;
                reactWidth = (float) (reactHeight * 1.6);
                left = this.mScreenW / 2 - reactWidth / 2;
                top = this.mScreenH / 5;
                right = left + reactWidth;
                bottom = top + reactHeight;
            } else {
                reactWidth = this.mScreenW * rate;
                reactHeight = (float) (reactWidth / 1.6);
                left = this.mScreenW / 2 - reactWidth / 2;
                top = this.mScreenH / 2 - reactHeight / 2;
                right = left + reactWidth;
                bottom = top + reactHeight;
            }
            this.mCanvas.drawRect(new RectF(left, top, right, bottom), this
                    .mPaint);

            // 画边框
            float lineLength = reactHeight / 15;
            this.mCanvas.drawLine(left, top, left + lineLength, top, mPaintLine);
            this.mCanvas.drawLine(left, top, left, top + lineLength, mPaintLine);
            this.mCanvas.drawLine(left, bottom, left + lineLength, bottom, mPaintLine);
            this.mCanvas.drawLine(left, bottom, left, bottom - lineLength, mPaintLine);
            this.mCanvas.drawLine(right, top, right, top + lineLength, mPaintLine);
            this.mCanvas.drawLine(right, top, right - lineLength, top, mPaintLine);
            this.mCanvas.drawLine(right, bottom, right - lineLength, bottom, mPaintLine);
            this.mCanvas.drawLine(right, bottom, right, bottom - lineLength, mPaintLine);

            mTextPaint.setTextSize(tipTextSize);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setDither(true);
            float length = mTextPaint.measureText(tipText);
            this.mCanvas.drawText(tipText, (left + reactWidth / 2) - length / 2, top + reactHeight / 2, mTextPaint);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (this.mCanvas != null) {
                this.mHolder.unlockCanvasAndPost(this.mCanvas);
            }
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //获得宽高，开启子线程绘图
        this.mScreenW = getWidth();
        this.mScreenH = getHeight();
        this.mThread = new Thread(this);
        this.mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.mScreenW = width;
        this.mScreenH = height;
        this.mThread = new Thread(this);
        this.mThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //停止线程
        try {
            mThread.interrupt();
            mThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //子线程绘图
        draw();
    }
}