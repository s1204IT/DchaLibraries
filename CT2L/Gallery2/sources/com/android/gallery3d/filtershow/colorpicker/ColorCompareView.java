package com.android.gallery3d.filtershow.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import java.util.ArrayList;

public class ColorCompareView extends View implements ColorListener {
    private Paint mBarPaint1;
    private int mBgcolor;
    private float mBorder;
    private int mCheckDim;
    private Paint mCheckPaint;
    ArrayList<ColorListener> mColorListeners;
    private float[] mHSVO;
    private float mHeight;
    private Paint mOrigBarPaint1;
    private float[] mOrigHSVO;
    private Path mOrigRegion;
    private Path mRegion;
    private float mWidth;

    public ColorCompareView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.mBgcolor = 0;
        this.mHSVO = new float[4];
        this.mOrigHSVO = new float[4];
        this.mCheckDim = 8;
        this.mColorListeners = new ArrayList<>();
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float mDpToPix = metrics.density;
        this.mBorder = 0.0f * mDpToPix;
        this.mBarPaint1 = new Paint();
        this.mOrigBarPaint1 = new Paint();
        Resources res = ctx.getResources();
        this.mCheckDim = res.getDimensionPixelSize(R.dimen.draw_color_check_dim);
        this.mBarPaint1.setStyle(Paint.Style.FILL);
        this.mOrigBarPaint1.setStyle(Paint.Style.FILL);
        makeCheckPaint();
    }

    private void makeCheckPaint() {
        int imgdim = this.mCheckDim * 2;
        int[] colors = new int[imgdim * imgdim];
        for (int i = 0; i < colors.length; i++) {
            int y = i / (this.mCheckDim * imgdim);
            int x = (i / this.mCheckDim) % 2;
            colors[i] = x == y ? -5592406 : -12303292;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, imgdim, imgdim, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        this.mCheckPaint = new Paint();
        this.mCheckPaint.setShader(bs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == 1) {
            float x = event.getX();
            event.getY();
            if (x > this.mWidth - (2.0f * this.mHeight)) {
                resetToOriginal();
            }
        }
        return true;
    }

    public void resetToOriginal() {
        System.arraycopy(this.mOrigHSVO, 0, this.mHSVO, 0, this.mOrigHSVO.length);
        updatePaint();
        notifyColorListeners(this.mHSVO);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mHeight = h;
        updatePaint();
    }

    private void updatePaint() {
        int color = Color.HSVToColor((int) (this.mHSVO[3] * 255.0f), this.mHSVO);
        this.mBarPaint1.setColor(color);
        int origColor = Color.HSVToColor((int) (this.mOrigHSVO[3] * 255.0f), this.mOrigHSVO);
        this.mOrigBarPaint1.setColor(origColor);
        this.mOrigRegion = new Path();
        this.mOrigRegion.moveTo(this.mWidth, 0.0f);
        this.mOrigRegion.lineTo(this.mWidth, this.mHeight);
        this.mOrigRegion.lineTo(this.mWidth - (this.mHeight * 2.0f), this.mHeight);
        this.mOrigRegion.lineTo(this.mWidth - this.mHeight, 0.0f);
        this.mRegion = new Path();
        this.mRegion.moveTo(0.0f, 0.0f);
        this.mRegion.lineTo(this.mWidth - this.mHeight, 0.0f);
        this.mRegion.lineTo(this.mWidth - (this.mHeight * 2.0f), this.mHeight);
        this.mRegion.lineTo(0.0f, this.mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(this.mBgcolor);
        canvas.drawRect(this.mBorder, 0.0f, this.mWidth, this.mHeight, this.mCheckPaint);
        canvas.drawPath(this.mRegion, this.mBarPaint1);
        canvas.drawPath(this.mOrigRegion, this.mOrigBarPaint1);
    }

    public void setOrigColor(float[] hsv) {
        System.arraycopy(hsv, 0, this.mOrigHSVO, 0, this.mOrigHSVO.length);
        int color2 = Color.HSVToColor((int) (this.mOrigHSVO[3] * 255.0f), this.mOrigHSVO);
        this.mOrigBarPaint1.setColor(color2);
        updatePaint();
    }

    @Override
    public void setColor(float[] hsv) {
        System.arraycopy(hsv, 0, this.mHSVO, 0, this.mHSVO.length);
        updatePaint();
        invalidate();
    }

    public void notifyColorListeners(float[] hsvo) {
        for (ColorListener l : this.mColorListeners) {
            l.setColor(hsvo);
        }
    }

    @Override
    public void addColorListener(ColorListener l) {
        this.mColorListeners.add(l);
    }
}
