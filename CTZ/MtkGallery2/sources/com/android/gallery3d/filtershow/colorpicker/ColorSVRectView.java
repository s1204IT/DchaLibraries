package com.android.gallery3d.filtershow.colorpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public class ColorSVRectView extends View implements ColorListener {
    Bitmap mBitmap;
    private float mBorder;
    ArrayList<ColorListener> mColorListeners;
    private float mCtrX;
    private float mCtrY;
    private int mDefaultViewHeight;
    private Paint mDotPaint;
    private float mDotRadus;
    private float mDotX;
    private float mDotY;
    private float mDpToPix;
    private float[] mHSVO;
    private int mHeight;
    private Paint mPaint1;
    RectF mRect;
    private int mSliderColor;
    private int mViewHeight;
    private int mWidth;

    public ColorSVRectView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCtrY = 100.0f;
        this.mCtrX = 100.0f;
        this.mDotPaint = new Paint();
        this.mDotX = Float.NaN;
        this.mSliderColor = -13388315;
        this.mHSVO = new float[]{0.0f, 1.0f, 1.0f, 1.0f};
        this.mRect = new RectF();
        this.mColorListeners = new ArrayList<>();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        this.mDpToPix = displayMetrics.density;
        this.mViewHeight = (int) (((double) displayMetrics.heightPixels) - (((double) (230.0f * displayMetrics.density)) + 0.5d));
        this.mDefaultViewHeight = (int) (((double) (256.0f * displayMetrics.density)) + 0.5d);
        Log.d("Gallery2/ColorSVRectView", "<ColorSVRectView> mViewHeight = " + this.mViewHeight + " mDefaultHeight = " + this.mDefaultViewHeight);
        this.mDotRadus = this.mDpToPix * 20.0f;
        this.mBorder = 20.0f * this.mDpToPix;
        this.mPaint1 = new Paint();
        this.mDotPaint.setStyle(Paint.Style.FILL);
        if (isInEditMode()) {
            this.mDotPaint.setColor(6579300);
            this.mSliderColor = 8947848;
        } else {
            this.mDotPaint.setColor(context.getResources().getColor(R.color.slider_dot_color));
            this.mSliderColor = context.getResources().getColor(R.color.slider_line_color);
        }
        this.mPaint1.setStyle(Paint.Style.FILL);
        this.mPaint1.setAntiAlias(true);
        this.mPaint1.setFilterBitmap(true);
        this.mBitmap = Bitmap.createBitmap(64, 46, Bitmap.Config.ARGB_8888);
        fillBitmap();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i);
        if (this.mViewHeight < this.mDefaultViewHeight) {
            setMeasuredDimension(View.MeasureSpec.getSize(i), this.mViewHeight);
        }
    }

    void fillBitmap() {
        int width = this.mBitmap.getWidth();
        int height = this.mBitmap.getHeight();
        int i = width * height;
        int[] iArr = new int[i];
        float[] fArr = new float[3];
        fArr[0] = this.mHSVO[0];
        for (int i2 = 0; i2 < i; i2++) {
            float f = width;
            fArr[1] = (i2 % width) / f;
            fArr[2] = (width - (i2 / width)) / f;
            iArr[i2] = Color.HSVToColor(fArr);
        }
        this.mBitmap.setPixels(iArr, 0, width, 0, 0, width, height);
    }

    private void setUpColorPanel() {
        updateDot();
        updateDotPaint();
        fillBitmap();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mRect.set(canvas.getClipBounds());
        this.mRect.top += this.mBorder;
        this.mRect.bottom -= this.mBorder;
        this.mRect.left += this.mBorder;
        this.mRect.right -= this.mBorder;
        canvas.drawBitmap(this.mBitmap, (Rect) null, this.mRect, this.mPaint1);
        if (!Float.isNaN(this.mDotX)) {
            canvas.drawCircle(this.mDotX, this.mDotY, this.mDotRadus, this.mDotPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        invalidate((int) (this.mDotX - this.mDotRadus), (int) (this.mDotY - this.mDotRadus), (int) (this.mDotX + this.mDotRadus), (int) (this.mDotY + this.mDotRadus));
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        float fMax = Math.max(Math.min(x, this.mWidth - this.mBorder), this.mBorder);
        float fMax2 = Math.max(Math.min(y, this.mHeight - this.mBorder), this.mBorder);
        this.mDotX = fMax;
        this.mDotY = fMax2;
        float f = 1.0f - ((this.mDotY - this.mBorder) / (this.mHeight - (this.mBorder * 2.0f)));
        if (f > 1.0f) {
            f = 1.0f;
        }
        float f2 = (this.mDotX - this.mBorder) / (this.mHeight - (2.0f * this.mBorder));
        this.mHSVO[2] = f;
        this.mHSVO[1] = f2;
        notifyColorListeners(this.mHSVO);
        updateDotPaint();
        invalidate((int) (this.mDotX - this.mDotRadus), (int) (this.mDotY - this.mDotRadus), (int) (this.mDotX + this.mDotRadus), (int) (this.mDotY + this.mDotRadus));
        return true;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mCtrY = i2 / 2.0f;
        this.mCtrX = i / 2.0f;
        setUpColorPanel();
    }

    private void updateDot() {
        float f = this.mHSVO[0];
        double d = this.mHSVO[1];
        double d2 = this.mHSVO[2];
        float f2 = this.mHSVO[3];
        this.mDotX = (float) (((double) this.mBorder) + (((double) (this.mHeight - (this.mBorder * 2.0f))) * d));
        this.mDotY = (float) (((1.0d - d2) * ((double) (this.mHeight - (2.0f * this.mBorder)))) + ((double) this.mBorder));
    }

    private void updateDotPaint() {
        this.mDotPaint.setShader(new RadialGradient(this.mDotX, this.mDotY, this.mDotRadus, new int[]{this.mSliderColor, this.mSliderColor, 1711276032, 0}, new float[]{0.0f, 0.3f, 0.31f, 1.0f}, Shader.TileMode.CLAMP));
    }

    @Override
    public void setColor(float[] fArr) {
        if (fArr[0] != this.mHSVO[0] || fArr[1] != this.mHSVO[1] || fArr[2] != this.mHSVO[2]) {
            System.arraycopy(fArr, 0, this.mHSVO, 0, this.mHSVO.length);
            setUpColorPanel();
            invalidate();
            updateDot();
            updateDotPaint();
            return;
        }
        this.mHSVO[3] = fArr[3];
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
