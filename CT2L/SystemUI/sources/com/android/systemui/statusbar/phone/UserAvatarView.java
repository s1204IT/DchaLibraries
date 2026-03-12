package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

public class UserAvatarView extends View {
    private int mActiveFrameColor;
    private Bitmap mBitmap;
    private final Paint mBitmapPaint;
    private final Matrix mDrawMatrix;
    private Drawable mDrawable;
    private int mFrameColor;
    private float mFramePadding;
    private final Paint mFramePaint;
    private float mFrameWidth;
    private float mScale;

    public UserAvatarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mFramePaint = new Paint();
        this.mBitmapPaint = new Paint();
        this.mDrawMatrix = new Matrix();
        this.mScale = 1.0f;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UserAvatarView, defStyleAttr, defStyleRes);
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 0:
                    setFrameColor(a.getColor(attr, 0));
                    break;
                case 1:
                    setFrameWidth(a.getDimension(attr, 0.0f));
                    break;
                case 2:
                    setFramePadding(a.getDimension(attr, 0.0f));
                    break;
                case 3:
                    setActiveFrameColor(a.getColor(attr, 0));
                    break;
            }
        }
        a.recycle();
        this.mFramePaint.setAntiAlias(true);
        this.mFramePaint.setStyle(Paint.Style.STROKE);
        this.mBitmapPaint.setAntiAlias(true);
    }

    public UserAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public UserAvatarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UserAvatarView(Context context) {
        this(context, null);
    }

    public void setBitmap(Bitmap bitmap) {
        setDrawable(null);
        this.mBitmap = bitmap;
        if (this.mBitmap != null) {
            this.mBitmapPaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        } else {
            this.mBitmapPaint.setShader(null);
        }
        configureBounds();
        invalidate();
    }

    public void setFrameColor(int frameColor) {
        this.mFrameColor = frameColor;
        invalidate();
    }

    public void setActiveFrameColor(int activeFrameColor) {
        this.mActiveFrameColor = activeFrameColor;
        invalidate();
    }

    public void setFrameWidth(float frameWidth) {
        this.mFrameWidth = frameWidth;
        invalidate();
    }

    public void setFramePadding(float framePadding) {
        this.mFramePadding = framePadding;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        configureBounds();
    }

    public void configureBounds() {
        int dwidth;
        int dheight;
        int vwidth = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
        int vheight = (getHeight() - this.mPaddingTop) - this.mPaddingBottom;
        if (this.mBitmap != null) {
            dwidth = this.mBitmap.getWidth();
            dheight = this.mBitmap.getHeight();
        } else if (this.mDrawable != null) {
            vwidth = (int) (vwidth - ((this.mFrameWidth - 1.0f) * 2.0f));
            vheight = (int) (vheight - ((this.mFrameWidth - 1.0f) * 2.0f));
            dwidth = vwidth;
            dheight = vheight;
            this.mDrawable.setBounds(0, 0, dwidth, dheight);
        } else {
            return;
        }
        float scale = Math.min(vwidth / dwidth, vheight / dheight);
        float dx = (int) (((vwidth - (dwidth * scale)) * 0.5f) + 0.5f);
        float dy = (int) (((vheight - (dheight * scale)) * 0.5f) + 0.5f);
        this.mDrawMatrix.setScale(scale, scale);
        this.mDrawMatrix.postTranslate(dx, dy);
        this.mScale = scale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int frameColor = isActivated() ? this.mActiveFrameColor : this.mFrameColor;
        float halfW = getWidth() / 2.0f;
        float halfH = getHeight() / 2.0f;
        float halfSW = Math.min(halfH, halfW);
        if (this.mBitmap != null && this.mScale > 0.0f) {
            int saveCount = canvas.getSaveCount();
            canvas.save();
            canvas.translate(this.mPaddingLeft, this.mPaddingTop);
            canvas.concat(this.mDrawMatrix);
            float halfBW = this.mBitmap.getWidth() / 2.0f;
            float halfBH = this.mBitmap.getHeight() / 2.0f;
            float halfBSW = Math.min(halfBH, halfBW);
            canvas.drawCircle(halfBW, halfBH, (halfBSW - (this.mFrameWidth / this.mScale)) + 1.0f, this.mBitmapPaint);
            canvas.restoreToCount(saveCount);
        } else if (this.mDrawable != null && this.mScale > 0.0f) {
            int saveCount2 = canvas.getSaveCount();
            canvas.save();
            canvas.translate(this.mPaddingLeft, this.mPaddingTop);
            canvas.translate(this.mFrameWidth - 1.0f, this.mFrameWidth - 1.0f);
            canvas.concat(this.mDrawMatrix);
            this.mDrawable.draw(canvas);
            canvas.restoreToCount(saveCount2);
        }
        if (frameColor != 0) {
            this.mFramePaint.setColor(frameColor);
            this.mFramePaint.setStrokeWidth(this.mFrameWidth);
            canvas.drawCircle(halfW, halfH, ((this.mFramePadding - this.mFrameWidth) / 2.0f) + halfSW, this.mFramePaint);
        }
    }

    public void setDrawable(Drawable d) {
        if (this.mDrawable != null) {
            this.mDrawable.setCallback(null);
            unscheduleDrawable(this.mDrawable);
        }
        this.mDrawable = d;
        if (d != null) {
            d.setCallback(this);
            if (d.isStateful()) {
                d.setState(getDrawableState());
            }
            d.setLayoutDirection(getLayoutDirection());
            configureBounds();
        }
        if (d != null) {
            this.mBitmap = null;
        }
        configureBounds();
        invalidate();
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (dr == this.mDrawable) {
            invalidate();
        } else {
            super.invalidateDrawable(dr);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == this.mDrawable || super.verifyDrawable(who);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mDrawable != null && this.mDrawable.isStateful()) {
            this.mDrawable.setState(getDrawableState());
        }
    }
}
