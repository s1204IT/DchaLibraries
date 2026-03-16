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
import java.util.ArrayList;

public class ColorSVRectView extends View implements ColorListener {
    Bitmap mBitmap;
    private float mBorder;
    ArrayList<ColorListener> mColorListeners;
    private float mCtrX;
    private float mCtrY;
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
    private int mWidth;

    public ColorSVRectView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.mCtrY = 100.0f;
        this.mCtrX = 100.0f;
        this.mDotPaint = new Paint();
        this.mDotX = Float.NaN;
        this.mSliderColor = -13388315;
        this.mHSVO = new float[]{0.0f, 1.0f, 1.0f, 1.0f};
        this.mRect = new RectF();
        this.mColorListeners = new ArrayList<>();
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        this.mDpToPix = metrics.density;
        this.mDotRadus = this.mDpToPix * 20.0f;
        this.mBorder = this.mDpToPix * 20.0f;
        this.mPaint1 = new Paint();
        this.mDotPaint.setStyle(Paint.Style.FILL);
        if (isInEditMode()) {
            this.mDotPaint.setColor(6579300);
            this.mSliderColor = 8947848;
        } else {
            this.mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));
            this.mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);
        }
        this.mPaint1.setStyle(Paint.Style.FILL);
        this.mPaint1.setAntiAlias(true);
        this.mPaint1.setFilterBitmap(true);
        this.mBitmap = Bitmap.createBitmap(64, 46, Bitmap.Config.ARGB_8888);
        fillBitmap();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    void fillBitmap() {
        int w = this.mBitmap.getWidth();
        int h = this.mBitmap.getHeight();
        int[] buff = new int[w * h];
        float[] hsv = new float[3];
        hsv[0] = this.mHSVO[0];
        for (int i = 0; i < w * h; i++) {
            float sat = (i % w) / w;
            float val = (w - (i / w)) / w;
            hsv[1] = sat;
            hsv[2] = val;
            buff[i] = Color.HSVToColor(hsv);
        }
        this.mBitmap.setPixels(buff, 0, w, 0, 0, w, h);
    }

    private void setUpColorPanel() {
        updateDot();
        updateDotPaint();
        fillBitmap();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect r = canvas.getClipBounds();
        this.mRect.set(r);
        this.mRect.top += this.mBorder;
        this.mRect.bottom -= this.mBorder;
        this.mRect.left += this.mBorder;
        this.mRect.right -= this.mBorder;
        canvas.drawBitmap(this.mBitmap, (Rect) null, this.mRect, this.mPaint1);
        if (this.mDotX != Float.NaN) {
            canvas.drawCircle(this.mDotX, this.mDotY, this.mDotRadus, this.mDotPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        invalidate((int) (this.mDotX - this.mDotRadus), (int) (this.mDotY - this.mDotRadus), (int) (this.mDotX + this.mDotRadus), (int) (this.mDotY + this.mDotRadus));
        float x = event.getX();
        float y = event.getY();
        float x2 = Math.max(Math.min(x, this.mWidth - this.mBorder), this.mBorder);
        float y2 = Math.max(Math.min(y, this.mHeight - this.mBorder), this.mBorder);
        this.mDotX = x2;
        this.mDotY = y2;
        float sat = 1.0f - ((this.mDotY - this.mBorder) / (this.mHeight - (this.mBorder * 2.0f)));
        if (sat > 1.0f) {
            sat = 1.0f;
        }
        float value = (this.mDotX - this.mBorder) / (this.mHeight - (this.mBorder * 2.0f));
        this.mHSVO[2] = sat;
        this.mHSVO[1] = value;
        notifyColorListeners(this.mHSVO);
        updateDotPaint();
        invalidate((int) (this.mDotX - this.mDotRadus), (int) (this.mDotY - this.mDotRadus), (int) (this.mDotX + this.mDotRadus), (int) (this.mDotY + this.mDotRadus));
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mHeight = h;
        this.mCtrY = h / 2.0f;
        this.mCtrX = w / 2.0f;
        setUpColorPanel();
    }

    private void updateDot() {
        double d = this.mHSVO[0];
        double sat = this.mHSVO[1];
        double val = this.mHSVO[2];
        double d2 = this.mHSVO[3];
        this.mDotX = (float) (((double) this.mBorder) + (((double) (this.mHeight - (this.mBorder * 2.0f))) * sat));
        this.mDotY = (float) (((1.0d - val) * ((double) (this.mHeight - (this.mBorder * 2.0f)))) + ((double) this.mBorder));
    }

    private void updateDotPaint() {
        int[] colors3 = {this.mSliderColor, this.mSliderColor, 1711276032, 0};
        RadialGradient g = new RadialGradient(this.mDotX, this.mDotY, this.mDotRadus, colors3, new float[]{0.0f, 0.3f, 0.31f, 1.0f}, Shader.TileMode.CLAMP);
        this.mDotPaint.setShader(g);
    }

    @Override
    public void setColor(float[] hsvo) {
        if (hsvo[0] == this.mHSVO[0] && hsvo[1] == this.mHSVO[1] && hsvo[2] == this.mHSVO[2]) {
            this.mHSVO[3] = hsvo[3];
            return;
        }
        System.arraycopy(hsvo, 0, this.mHSVO, 0, this.mHSVO.length);
        setUpColorPanel();
        invalidate();
        updateDot();
        updateDotPaint();
    }

    public void notifyColorListeners(float[] hsv) {
        for (ColorListener l : this.mColorListeners) {
            l.setColor(hsv);
        }
    }

    @Override
    public void addColorListener(ColorListener l) {
        this.mColorListeners.add(l);
    }
}
