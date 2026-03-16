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
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import java.util.ArrayList;

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

    public ColorHueView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.mPaint = new Paint();
        this.mBgcolor = 0;
        this.mHSVO = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        this.mDotX = this.mBorder;
        this.mDotY = this.mBorder;
        this.mRect = new RectF();
        this.mTmpHSV = new float[3];
        this.mColorListeners = new ArrayList<>();
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float mDpToPix = metrics.density;
        this.mDotRadius = 20.0f * mDpToPix;
        this.mBorder = 20.0f * mDpToPix;
        this.mDotPaint = new Paint();
        this.mDotPaint.setStyle(Paint.Style.FILL);
        this.mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));
        this.mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);
        this.mLinePaint1 = new Paint();
        this.mLinePaint1.setColor(-7829368);
        this.mLinePaint2 = new Paint();
        this.mLinePaint2.setColor(this.mSliderColor);
        this.mLinePaint2.setStrokeWidth(4.0f);
        this.mBitmap = Bitmap.createBitmap(NotificationCompat.FLAG_LOCAL_ONLY, 2, Bitmap.Config.ARGB_8888);
        this.mTmpBuff = new int[this.mBitmap.getWidth() * this.mBitmap.getHeight()];
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        fillBitmap();
        makeCheckPaint();
    }

    void fillBitmap() {
        int w = this.mBitmap.getWidth();
        int h = this.mBitmap.getHeight();
        for (int x = 0; x < w; x++) {
            float hue = (x * 360) / w;
            this.mTmpHSV[0] = hue;
            this.mTmpHSV[1] = 1.0f;
            this.mTmpHSV[2] = 1.0f;
            int color = Color.HSVToColor(this.mTmpHSV);
            this.mTmpBuff[x] = color;
            this.mTmpBuff[x + w] = color;
        }
        this.mBitmap.setPixels(this.mTmpBuff, 0, w, 0, 0, w, h);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float f = this.mDotX;
        float f2 = this.mDotY;
        float x = event.getX();
        event.getY();
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
        float pos = (this.mHSVO[0] / 360.0f) * (this.mWidth - (this.mBorder * 2.0f));
        this.mDotX = this.mBorder + pos;
        int[] colors3 = {this.mSliderColor, this.mSliderColor, 1711276032, 0};
        RadialGradient g = new RadialGradient(this.mDotX, this.mDotY, this.mDotRadius, colors3, new float[]{0.0f, 0.3f, 0.31f, 1.0f}, Shader.TileMode.CLAMP);
        this.mDotPaint.setShader(g);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mHeight = h;
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
        if (this.mDotX != Float.NaN) {
            canvas.drawCircle(this.mDotX, this.mDotY, this.mDotRadius, this.mDotPaint);
        }
    }

    private void makeCheckPaint() {
        int i = 16 * 2;
        int[] colors = new int[1024];
        for (int i2 = 0; i2 < colors.length; i2++) {
            int y = i2 / NotificationCompat.FLAG_GROUP_SUMMARY;
            int x = (i2 / 16) % 2;
            colors[i2] = x == y ? -5592406 : -12303292;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, 16, 16, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        this.mCheckPaint = new Paint();
        this.mCheckPaint.setShader(bs);
    }

    @Override
    public void setColor(float[] hsv) {
        System.arraycopy(hsv, 0, this.mHSVO, 0, this.mHSVO.length);
        fillBitmap();
        setupButton();
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
