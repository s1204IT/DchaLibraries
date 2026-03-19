package com.android.gallery3d.filtershow.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import java.util.ArrayList;
import java.util.Iterator;

public class ColorOpacityView extends View implements ColorListener {
    private Paint mBarPaint1;
    private int mBgcolor;
    private float mBorder;
    private int mCheckDim;
    private Paint mCheckPaint;
    ArrayList<ColorListener> mColorListeners;
    private Paint mDotPaint;
    private float mDotRadius;
    private float mDotX;
    private float mDotY;
    private float[] mHSVO;
    private float mHeight;
    private Paint mLinePaint1;
    private Paint mLinePaint2;
    private int mSliderColor;
    private float mWidth;

    public ColorOpacityView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBgcolor = 0;
        this.mHSVO = new float[4];
        this.mDotX = this.mBorder;
        this.mDotY = this.mBorder;
        this.mCheckDim = 8;
        this.mColorListeners = new ArrayList<>();
        float f = 20.0f * context.getResources().getDisplayMetrics().density;
        this.mDotRadius = f;
        this.mBorder = f;
        this.mBarPaint1 = new Paint();
        this.mDotPaint = new Paint();
        this.mDotPaint.setStyle(Paint.Style.FILL);
        Resources resources = context.getResources();
        this.mCheckDim = resources.getDimensionPixelSize(R.dimen.draw_color_check_dim);
        this.mDotPaint.setColor(resources.getColor(R.color.slider_dot_color));
        this.mSliderColor = resources.getColor(R.color.slider_line_color);
        this.mBarPaint1.setStyle(Paint.Style.FILL);
        this.mLinePaint1 = new Paint();
        this.mLinePaint1.setColor(-7829368);
        this.mLinePaint2 = new Paint();
        this.mLinePaint2.setColor(this.mSliderColor);
        this.mLinePaint2.setStrokeWidth(4.0f);
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
        float f = this.mDotX;
        float f2 = this.mDotY;
        float x = motionEvent.getX();
        motionEvent.getY();
        this.mDotX = x;
        if (this.mDotX < this.mBorder) {
            this.mDotX = this.mBorder;
        }
        if (this.mDotX > this.mWidth - this.mBorder) {
            this.mDotX = this.mWidth - this.mBorder;
        }
        this.mHSVO[3] = (this.mDotX - this.mBorder) / (this.mWidth - (this.mBorder * 2.0f));
        notifyColorListeners(this.mHSVO);
        setupButton();
        invalidate((int) (f - this.mDotRadius), (int) (f2 - this.mDotRadius), (int) (f + this.mDotRadius), (int) (f2 + this.mDotRadius));
        invalidate((int) (this.mDotX - this.mDotRadius), (int) (this.mDotY - this.mDotRadius), (int) (this.mDotX + this.mDotRadius), (int) (this.mDotY + this.mDotRadius));
        return true;
    }

    private void setupButton() {
        this.mDotX = (this.mHSVO[3] * (this.mWidth - (this.mBorder * 2.0f))) + this.mBorder;
        this.mDotPaint.setShader(new RadialGradient(this.mDotX, this.mDotY, this.mDotRadius, new int[]{this.mSliderColor, this.mSliderColor, 1711276032, 0}, new float[]{0.0f, 0.3f, 0.31f, 1.0f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mDotY = this.mHeight / 2.0f;
        updatePaint();
        setupButton();
    }

    private void updatePaint() {
        int iHSVToColor = Color.HSVToColor(this.mHSVO);
        this.mBarPaint1.setShader(new LinearGradient(this.mBorder, this.mBorder, this.mWidth - this.mBorder, this.mBorder, iHSVToColor & 16777215, iHSVToColor, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(this.mBgcolor);
        canvas.drawRect(this.mBorder, 0.0f, this.mWidth - this.mBorder, this.mHeight, this.mCheckPaint);
        canvas.drawRect(this.mBorder, 0.0f, this.mWidth - this.mBorder, this.mHeight, this.mBarPaint1);
        canvas.drawLine(this.mDotX, this.mDotY, this.mWidth - this.mBorder, this.mDotY, this.mLinePaint1);
        canvas.drawLine(this.mBorder, this.mDotY, this.mDotX, this.mDotY, this.mLinePaint2);
        if (!Float.isNaN(this.mDotX)) {
            canvas.drawCircle(this.mDotX, this.mDotY, this.mDotRadius, this.mDotPaint);
        }
    }

    @Override
    public void setColor(float[] fArr) {
        System.arraycopy(fArr, 0, this.mHSVO, 0, this.mHSVO.length);
        updatePaint();
        setupButton();
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
