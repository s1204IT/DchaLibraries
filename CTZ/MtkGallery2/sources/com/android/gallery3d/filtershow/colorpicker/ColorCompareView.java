package com.android.gallery3d.filtershow.colorpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import java.util.ArrayList;
import java.util.Iterator;

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

    public ColorCompareView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBgcolor = 0;
        this.mHSVO = new float[4];
        this.mOrigHSVO = new float[4];
        this.mCheckDim = 8;
        this.mColorListeners = new ArrayList<>();
        this.mBorder = 0.0f * context.getResources().getDisplayMetrics().density;
        this.mBarPaint1 = new Paint();
        this.mOrigBarPaint1 = new Paint();
        this.mCheckDim = context.getResources().getDimensionPixelSize(R.dimen.draw_color_check_dim);
        this.mBarPaint1.setStyle(Paint.Style.FILL);
        this.mOrigBarPaint1.setStyle(Paint.Style.FILL);
        makeCheckPaint();
    }

    private void makeCheckPaint() {
        int i = this.mCheckDim * 2;
        int[] iArr = new int[i * i];
        for (int i2 = 0; i2 < iArr.length; i2++) {
            iArr[i2] = (i2 / this.mCheckDim) % 2 == i2 / (this.mCheckDim * i) ? -5592406 : -12303292;
        }
        BitmapShader bitmapShader = new BitmapShader(Bitmap.createBitmap(iArr, i, i, Bitmap.Config.ARGB_8888), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        this.mCheckPaint = new Paint();
        this.mCheckPaint.setShader(bitmapShader);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() != 1) {
            return true;
        }
        float x = motionEvent.getX();
        motionEvent.getY();
        if (x > this.mWidth - (2.0f * this.mHeight)) {
            resetToOriginal();
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
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mWidth = i;
        this.mHeight = i2;
        updatePaint();
    }

    private void updatePaint() {
        this.mBarPaint1.setColor(Color.HSVToColor((int) (this.mHSVO[3] * 255.0f), this.mHSVO));
        this.mOrigBarPaint1.setColor(Color.HSVToColor((int) (this.mOrigHSVO[3] * 255.0f), this.mOrigHSVO));
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

    public void setOrigColor(float[] fArr) {
        System.arraycopy(fArr, 0, this.mOrigHSVO, 0, this.mOrigHSVO.length);
        this.mOrigBarPaint1.setColor(Color.HSVToColor((int) (this.mOrigHSVO[3] * 255.0f), this.mOrigHSVO));
        updatePaint();
    }

    @Override
    public void setColor(float[] fArr) {
        System.arraycopy(fArr, 0, this.mHSVO, 0, this.mHSVO.length);
        updatePaint();
        invalidate();
    }

    public void notifyColorListeners(float[] fArr) {
        Iterator<ColorListener> it = this.mColorListeners.iterator();
        while (it.hasNext()) {
            it.next().setColor(fArr);
        }
    }

    @Override
    public void addColorListener(ColorListener colorListener) {
        this.mColorListeners.add(colorListener);
    }
}
