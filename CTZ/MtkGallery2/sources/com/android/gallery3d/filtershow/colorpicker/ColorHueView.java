package com.android.gallery3d.filtershow.colorpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import com.mediatek.plugin.preload.SoOperater;
import java.util.ArrayList;
import java.util.Iterator;

public class ColorHueView extends View implements ColorListener {
    private int mBgcolor;
    Bitmap mBitmap;
    private float mBorder;
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
    private Paint mPaint;
    RectF mRect;
    private int mSliderColor;
    int[] mTmpBuff;
    float[] mTmpHSV;
    private float mWidth;

    public ColorHueView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.mBgcolor = 0;
        this.mHSVO = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        this.mDotX = this.mBorder;
        this.mDotY = this.mBorder;
        this.mRect = new RectF();
        this.mTmpHSV = new float[3];
        this.mColorListeners = new ArrayList<>();
        float f = 20.0f * context.getResources().getDisplayMetrics().density;
        this.mDotRadius = f;
        this.mBorder = f;
        this.mDotPaint = new Paint();
        this.mDotPaint.setStyle(Paint.Style.FILL);
        this.mDotPaint.setColor(context.getResources().getColor(R.color.slider_dot_color));
        this.mSliderColor = context.getResources().getColor(R.color.slider_line_color);
        this.mLinePaint1 = new Paint();
        this.mLinePaint1.setColor(-7829368);
        this.mLinePaint2 = new Paint();
        this.mLinePaint2.setColor(this.mSliderColor);
        this.mLinePaint2.setStrokeWidth(4.0f);
        this.mBitmap = Bitmap.createBitmap(256, 2, Bitmap.Config.ARGB_8888);
        this.mTmpBuff = new int[this.mBitmap.getWidth() * this.mBitmap.getHeight()];
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        fillBitmap();
        makeCheckPaint();
    }

    void fillBitmap() {
        int width = this.mBitmap.getWidth();
        int height = this.mBitmap.getHeight();
        for (int i = 0; i < width; i++) {
            this.mTmpHSV[0] = (360 * i) / width;
            this.mTmpHSV[1] = 1.0f;
            this.mTmpHSV[2] = 1.0f;
            int iHSVToColor = Color.HSVToColor(this.mTmpHSV);
            this.mTmpBuff[i] = iHSVToColor;
            this.mTmpBuff[i + width] = iHSVToColor;
        }
        this.mBitmap.setPixels(this.mTmpBuff, 0, width, 0, 0, width, height);
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
        this.mHSVO[0] = (360.0f * (this.mDotX - this.mBorder)) / (this.mWidth - (this.mBorder * 2.0f));
        notifyColorListeners(this.mHSVO);
        setupButton();
        fillBitmap();
        invalidate();
        return true;
    }

    private void setupButton() {
        this.mDotX = ((this.mHSVO[0] / 360.0f) * (this.mWidth - (this.mBorder * 2.0f))) + this.mBorder;
        this.mDotPaint.setShader(new RadialGradient(this.mDotX, this.mDotY, this.mDotRadius, new int[]{this.mSliderColor, this.mSliderColor, 1711276032, 0}, new float[]{0.0f, 0.3f, 0.31f, 1.0f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mDotY = this.mHeight / 2.0f;
        setupButton();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(this.mBgcolor);
        this.mRect.left = this.mBorder;
        this.mRect.right = this.mWidth - this.mBorder;
        this.mRect.top = 0.0f;
        this.mRect.bottom = this.mHeight;
        canvas.drawRect(this.mRect, this.mCheckPaint);
        canvas.drawBitmap(this.mBitmap, (Rect) null, this.mRect, this.mPaint);
        canvas.drawLine(this.mDotX, this.mDotY, this.mWidth - this.mBorder, this.mDotY, this.mLinePaint1);
        canvas.drawLine(this.mBorder, this.mDotY, this.mDotX, this.mDotY, this.mLinePaint2);
        if (!Float.isNaN(this.mDotX)) {
            canvas.drawCircle(this.mDotX, this.mDotY, this.mDotRadius, this.mDotPaint);
        }
    }

    private void makeCheckPaint() {
        int[] iArr = new int[SoOperater.STEP];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = (i / 16) % 2 == i / 512 ? -5592406 : -12303292;
        }
        BitmapShader bitmapShader = new BitmapShader(Bitmap.createBitmap(iArr, 16, 16, Bitmap.Config.ARGB_8888), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        this.mCheckPaint = new Paint();
        this.mCheckPaint.setShader(bitmapShader);
    }

    @Override
    public void setColor(float[] fArr) {
        System.arraycopy(fArr, 0, this.mHSVO, 0, this.mHSVO.length);
        fillBitmap();
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
