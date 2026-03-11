package com.android.settings.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import com.android.settings.R;

public class CircleFramedDrawable extends Drawable {
    private final Bitmap mBitmap;
    private RectF mDstRect;
    private final int mFrameColor;
    private Path mFramePath;
    private RectF mFrameRect;
    private final int mFrameShadowColor;
    private final int mHighlightColor;
    private final Paint mPaint;
    private boolean mPressed;
    private float mScale;
    private final float mShadowRadius;
    private final int mSize;
    private Rect mSrcRect;
    private final float mStrokeWidth;

    public static CircleFramedDrawable getInstance(Context context, Bitmap icon) {
        Resources res = context.getResources();
        float iconSize = res.getDimension(R.dimen.circle_avatar_size);
        float strokeWidth = res.getDimension(R.dimen.circle_avatar_frame_stroke_width);
        float shadowRadius = res.getDimension(R.dimen.circle_avatar_frame_shadow_radius);
        int frameColor = res.getColor(R.color.circle_avatar_frame_color);
        int frameShadowColor = res.getColor(R.color.circle_avatar_frame_shadow_color);
        int highlightColor = res.getColor(R.color.circle_avatar_frame_pressed_color);
        CircleFramedDrawable instance = new CircleFramedDrawable(icon, (int) iconSize, frameColor, strokeWidth, frameShadowColor, shadowRadius, highlightColor);
        return instance;
    }

    public CircleFramedDrawable(Bitmap icon, int size, int frameColor, float strokeWidth, int frameShadowColor, float shadowRadius, int highlightColor) {
        this.mSize = size;
        this.mShadowRadius = shadowRadius;
        this.mFrameColor = frameColor;
        this.mFrameShadowColor = frameShadowColor;
        this.mStrokeWidth = strokeWidth;
        this.mHighlightColor = highlightColor;
        this.mBitmap = Bitmap.createBitmap(this.mSize, this.mSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.mBitmap);
        int width = icon.getWidth();
        int height = icon.getHeight();
        int square = Math.min(width, height);
        Rect cropRect = new Rect((width - square) / 2, (height - square) / 2, square, square);
        RectF circleRect = new RectF(0.0f, 0.0f, this.mSize, this.mSize);
        circleRect.inset(this.mStrokeWidth / 2.0f, this.mStrokeWidth / 2.0f);
        circleRect.inset(this.mShadowRadius, this.mShadowRadius);
        Path fillPath = new Path();
        fillPath.addArc(circleRect, 0.0f, 360.0f);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(-16777216);
        this.mPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(fillPath, this.mPaint);
        this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(icon, cropRect, circleRect, this.mPaint);
        this.mPaint.setXfermode(null);
        this.mScale = 1.0f;
        this.mSrcRect = new Rect(0, 0, this.mSize, this.mSize);
        this.mDstRect = new RectF(0.0f, 0.0f, this.mSize, this.mSize);
        this.mFrameRect = new RectF(this.mDstRect);
        this.mFramePath = new Path();
    }

    @Override
    public void draw(Canvas canvas) {
        float inside = this.mScale * this.mSize;
        float pad = (this.mSize - inside) / 2.0f;
        this.mDstRect.set(pad, pad, this.mSize - pad, this.mSize - pad);
        canvas.drawBitmap(this.mBitmap, this.mSrcRect, this.mDstRect, (Paint) null);
        this.mFrameRect.set(this.mDstRect);
        this.mFrameRect.inset(this.mStrokeWidth / 2.0f, this.mStrokeWidth / 2.0f);
        this.mFrameRect.inset(this.mShadowRadius, this.mShadowRadius);
        this.mFramePath.reset();
        this.mFramePath.addArc(this.mFrameRect, 0.0f, 360.0f);
        if (this.mPressed) {
            this.mPaint.setStyle(Paint.Style.FILL);
            this.mPaint.setColor(Color.argb(84, Color.red(this.mHighlightColor), Color.green(this.mHighlightColor), Color.blue(this.mHighlightColor)));
            canvas.drawPath(this.mFramePath, this.mPaint);
        }
        this.mPaint.setStrokeWidth(this.mStrokeWidth);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setColor(this.mPressed ? this.mHighlightColor : this.mFrameColor);
        this.mPaint.setShadowLayer(this.mShadowRadius, 0.0f, 0.0f, this.mFrameShadowColor);
        canvas.drawPath(this.mFramePath, this.mPaint);
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mSize;
    }
}
