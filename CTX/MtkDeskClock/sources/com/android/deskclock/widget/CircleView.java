package com.android.deskclock.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import com.android.deskclock.R;

public class CircleView extends View {
    public static final Property<CircleView, Integer> FILL_COLOR = new Property<CircleView, Integer>(Integer.class, "fillColor") {
        @Override
        public Integer get(CircleView circleView) {
            return Integer.valueOf(circleView.getFillColor());
        }

        @Override
        public void set(CircleView circleView, Integer num) {
            circleView.setFillColor(num.intValue());
        }
    };
    public static final Property<CircleView, Float> RADIUS = new Property<CircleView, Float>(Float.class, "radius") {
        @Override
        public Float get(CircleView circleView) {
            return Float.valueOf(circleView.getRadius());
        }

        @Override
        public void set(CircleView circleView, Float f) {
            circleView.setRadius(f.floatValue());
        }
    };
    private float mCenterX;
    private float mCenterY;
    private final Paint mCirclePaint;
    private int mGravity;
    private float mRadius;

    public CircleView(Context context) {
        this(context, null);
    }

    public CircleView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CircleView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mCirclePaint = new Paint();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CircleView, i, 0);
        this.mGravity = typedArrayObtainStyledAttributes.getInt(0, 0);
        this.mCenterX = typedArrayObtainStyledAttributes.getDimension(1, 0.0f);
        this.mCenterY = typedArrayObtainStyledAttributes.getDimension(2, 0.0f);
        this.mRadius = typedArrayObtainStyledAttributes.getDimension(4, 0.0f);
        this.mCirclePaint.setColor(typedArrayObtainStyledAttributes.getColor(3, -1));
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (this.mGravity != 0) {
            applyGravity(this.mGravity, i);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mGravity != 0) {
            applyGravity(this.mGravity, getLayoutDirection());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(this.mCenterX, this.mCenterY, this.mRadius, this.mCirclePaint);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return getBackground() != null;
    }

    public final int getGravity() {
        return this.mGravity;
    }

    public CircleView setGravity(int i) {
        if (this.mGravity != i) {
            this.mGravity = i;
            if (i != 0 && isLayoutDirectionResolved()) {
                applyGravity(i, getLayoutDirection());
            }
        }
        return this;
    }

    public final int getFillColor() {
        return this.mCirclePaint.getColor();
    }

    public CircleView setFillColor(int i) {
        if (this.mCirclePaint.getColor() != i) {
            this.mCirclePaint.setColor(i);
            invalidate(this.mCenterX, this.mCenterY, this.mRadius);
        }
        return this;
    }

    public CircleView setCenterX(float f) {
        float f2 = this.mCenterX;
        if (f2 != f) {
            this.mCenterX = f;
            invalidate(f2, this.mCenterY, this.mRadius);
            invalidate(f, this.mCenterY, this.mRadius);
        }
        this.mGravity &= -8;
        return this;
    }

    public CircleView setCenterY(float f) {
        float f2 = this.mCenterY;
        if (f2 != f) {
            this.mCenterY = f;
            invalidate(this.mCenterX, f2, this.mRadius);
            invalidate(this.mCenterX, f, this.mRadius);
        }
        this.mGravity &= -113;
        return this;
    }

    public final float getRadius() {
        return this.mRadius;
    }

    public CircleView setRadius(float f) {
        float f2 = this.mRadius;
        if (f2 != f) {
            this.mRadius = f;
            invalidate(this.mCenterX, this.mCenterY, f2);
            if (f > f2) {
                invalidate(this.mCenterX, this.mCenterY, f);
            }
        }
        if ((this.mGravity & 7) == 7) {
            this.mGravity &= -8;
        }
        if ((this.mGravity & 112) == 112) {
            this.mGravity &= -113;
        }
        return this;
    }

    private void invalidate(float f, float f2, float f3) {
        invalidate((int) ((f - f3) - 0.5f), (int) ((f2 - f3) - 0.5f), (int) (f + f3 + 0.5f), (int) (f2 + f3 + 0.5f));
    }

    @SuppressLint({"RtlHardcoded"})
    private void applyGravity(int i, int i2) {
        int absoluteGravity = Gravity.getAbsoluteGravity(i, i2);
        float f = this.mRadius;
        float f2 = this.mCenterX;
        float f3 = this.mCenterY;
        int i3 = absoluteGravity & 7;
        if (i3 == 1) {
            this.mCenterX = getWidth() / 2.0f;
        } else if (i3 == 3) {
            this.mCenterX = 0.0f;
        } else if (i3 == 5) {
            this.mCenterX = getWidth();
        } else if (i3 == 7) {
        }
        int i4 = absoluteGravity & 112;
        if (i4 == 16) {
            this.mCenterY = getHeight() / 2.0f;
        } else if (i4 == 48) {
            this.mCenterY = 0.0f;
        } else if (i4 == 80) {
            this.mCenterY = getHeight();
        } else if (i4 == 112) {
        }
        int i5 = absoluteGravity & 119;
        if (i5 == 7) {
            this.mRadius = getWidth() / 2.0f;
        } else if (i5 != 112) {
            if (i5 == 119) {
                this.mRadius = Math.min(getWidth(), getHeight()) / 2.0f;
            }
        } else {
            this.mRadius = getHeight() / 2.0f;
        }
        if (f2 != this.mCenterX || f3 != this.mCenterY || f != this.mRadius) {
            invalidate(f2, f3, f);
            invalidate(this.mCenterX, this.mCenterY, this.mRadius);
        }
    }
}
