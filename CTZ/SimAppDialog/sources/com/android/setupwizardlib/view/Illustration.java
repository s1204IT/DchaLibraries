package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import com.android.setupwizardlib.R;

public class Illustration extends FrameLayout {
    private float mAspectRatio;
    private Drawable mBackground;
    private float mBaselineGridSize;
    private Drawable mIllustration;
    private final Rect mIllustrationBounds;
    private float mScale;
    private final Rect mViewBounds;

    public Illustration(Context context) {
        super(context);
        this.mViewBounds = new Rect();
        this.mIllustrationBounds = new Rect();
        this.mScale = 1.0f;
        this.mAspectRatio = 0.0f;
        init(null, 0);
    }

    public Illustration(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mViewBounds = new Rect();
        this.mIllustrationBounds = new Rect();
        this.mScale = 1.0f;
        this.mAspectRatio = 0.0f;
        init(attributeSet, 0);
    }

    @TargetApi(11)
    public Illustration(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mViewBounds = new Rect();
        this.mIllustrationBounds = new Rect();
        this.mScale = 1.0f;
        this.mAspectRatio = 0.0f;
        init(attributeSet, i);
    }

    private void init(AttributeSet attributeSet, int i) {
        if (attributeSet != null) {
            TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwIllustration, i, 0);
            this.mAspectRatio = typedArrayObtainStyledAttributes.getFloat(R.styleable.SuwIllustration_suwAspectRatio, 0.0f);
            typedArrayObtainStyledAttributes.recycle();
        }
        this.mBaselineGridSize = getResources().getDisplayMetrics().density * 8.0f;
        setWillNotDraw(false);
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        if (drawable == this.mBackground) {
            return;
        }
        this.mBackground = drawable;
        invalidate();
        requestLayout();
    }

    public void setIllustration(Drawable drawable) {
        if (drawable == this.mIllustration) {
            return;
        }
        this.mIllustration = drawable;
        invalidate();
        requestLayout();
    }

    @Override
    @Deprecated
    public void setForeground(Drawable drawable) {
        setIllustration(drawable);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (this.mAspectRatio != 0.0f) {
            float size = (int) (View.MeasureSpec.getSize(i) / this.mAspectRatio);
            setPadding(0, (int) (size - (size % this.mBaselineGridSize)), 0, 0);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            setOutlineProvider(ViewOutlineProvider.BOUNDS);
        }
        super.onMeasure(i, i2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        int i6 = i4 - i2;
        if (this.mIllustration != null) {
            int intrinsicWidth = this.mIllustration.getIntrinsicWidth();
            int intrinsicHeight = this.mIllustration.getIntrinsicHeight();
            this.mViewBounds.set(0, 0, i5, i6);
            if (this.mAspectRatio != 0.0f) {
                this.mScale = i5 / intrinsicWidth;
                intrinsicHeight = (int) (intrinsicHeight * this.mScale);
                intrinsicWidth = i5;
            }
            Gravity.apply(55, intrinsicWidth, intrinsicHeight, this.mViewBounds, this.mIllustrationBounds);
            this.mIllustration.setBounds(this.mIllustrationBounds);
        }
        if (this.mBackground != null) {
            this.mBackground.setBounds(0, 0, (int) Math.ceil(i5 / this.mScale), (int) Math.ceil((i6 - this.mIllustrationBounds.height()) / this.mScale));
        }
        super.onLayout(z, i, i2, i3, i4);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mBackground != null) {
            canvas.save();
            canvas.translate(0.0f, this.mIllustrationBounds.height());
            canvas.scale(this.mScale, this.mScale, 0.0f, 0.0f);
            if (Build.VERSION.SDK_INT > 17 && shouldMirrorDrawable(this.mBackground, getLayoutDirection())) {
                canvas.scale(-1.0f, 1.0f);
                canvas.translate(-this.mBackground.getBounds().width(), 0.0f);
            }
            this.mBackground.draw(canvas);
            canvas.restore();
        }
        if (this.mIllustration != null) {
            canvas.save();
            if (Build.VERSION.SDK_INT > 17 && shouldMirrorDrawable(this.mIllustration, getLayoutDirection())) {
                canvas.scale(-1.0f, 1.0f);
                canvas.translate(-this.mIllustrationBounds.width(), 0.0f);
            }
            this.mIllustration.draw(canvas);
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    private boolean shouldMirrorDrawable(Drawable drawable, int i) {
        if (i == 1) {
            if (Build.VERSION.SDK_INT >= 19) {
                return drawable.isAutoMirrored();
            }
            return Build.VERSION.SDK_INT >= 17 && (getContext().getApplicationInfo().flags & 4194304) != 0;
        }
        return false;
    }
}
