package com.android.gallery3d.filtershow.colorpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import java.util.ArrayList;
import java.util.Arrays;

public class ColorSaturationView extends View implements ColorListener {
    private Paint mBarPaint1;
    private int mBgcolor;
    private float mBorder;
    private Paint mCheckPaint;
    private ArrayList<ColorListener> mColorListeners;
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

    public ColorSaturationView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.mBgcolor = 0;
        this.mHSVO = new float[4];
        this.mDotX = this.mBorder;
        this.mDotY = this.mBorder;
        this.mColorListeners = new ArrayList<>();
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float mDpToPix = metrics.density;
        this.mDotRadius = 20.0f * mDpToPix;
        this.mBorder = 20.0f * mDpToPix;
        this.mBarPaint1 = new Paint();
        this.mDotPaint = new Paint();
        this.mDotPaint.setStyle(Paint.Style.FILL);
        this.mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));
        this.mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);
        this.mBarPaint1.setStyle(Paint.Style.FILL);
        this.mLinePaint1 = new Paint();
        this.mLinePaint1.setColor(-7829368);
        this.mLinePaint2 = new Paint();
        this.mLinePaint2.setColor(this.mSliderColor);
        this.mLinePaint2.setStrokeWidth(4.0f);
        int[] colors = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        for (int i = 0; i < colors.length; i++) {
            int y = i / 128;
            int x = (i / 8) % 2;
            colors[i] = x == y ? -5592406 : -12303292;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, 16, 16, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        this.mCheckPaint = new Paint();
        this.mCheckPaint.setShader(bs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float ox = this.mDotX;
        float oy = this.mDotY;
        float x = event.getX();
        event.getY();
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
        invalidate((int) (ox - this.mDotRadius), (int) (oy - this.mDotRadius), (int) (this.mDotRadius + ox), (int) (this.mDotRadius + oy));
        invalidate((int) (this.mDotX - this.mDotRadius), (int) (this.mDotY - this.mDotRadius), (int) (this.mDotX + this.mDotRadius), (int) (this.mDotY + this.mDotRadius));
        return true;
    }

    private void setupButton() {
        float pos = this.mHSVO[3] * (this.mWidth - (this.mBorder * 2.0f));
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
        updatePaint();
        setupButton();
    }

    private void updatePaint() {
        float[] hsvo = Arrays.copyOf(this.mHSVO, 4);
        hsvo[3] = 1.0f;
        hsvo[2] = 1.0f;
        hsvo[1] = 1.0f;
        int color2 = Color.HSVToColor(hsvo);
        hsvo[1] = 0.0f;
        int color1 = Color.HSVToColor(hsvo);
        Shader sg = new LinearGradient(this.mBorder, this.mBorder, this.mWidth - this.mBorder, this.mBorder, color1, color2, Shader.TileMode.CLAMP);
        this.mBarPaint1.setShader(sg);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(this.mBgcolor);
        canvas.drawRect(this.mBorder, this.mBorder, this.mWidth - this.mBorder, this.mHeight - this.mBorder, this.mCheckPaint);
        canvas.drawRect(this.mBorder, this.mBorder, this.mWidth - this.mBorder, this.mHeight - this.mBorder, this.mBarPaint1);
        canvas.drawLine(this.mDotX, this.mDotY, this.mWidth - this.mBorder, this.mDotY, this.mLinePaint1);
        canvas.drawLine(this.mBorder, this.mDotY, this.mDotX, this.mDotY, this.mLinePaint2);
        if (this.mDotX != Float.NaN) {
            canvas.drawCircle(this.mDotX, this.mDotY, this.mDotRadius, this.mDotPaint);
        }
    }

    @Override
    public void setColor(float[] hsv) {
        System.arraycopy(hsv, 0, this.mHSVO, 0, this.mHSVO.length);
        float f = this.mDotY;
        updatePaint();
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
