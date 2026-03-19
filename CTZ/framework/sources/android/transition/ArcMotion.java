package android.transition;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.util.AttributeSet;
import com.android.internal.R;

public class ArcMotion extends PathMotion {
    private static final float DEFAULT_MAX_ANGLE_DEGREES = 70.0f;
    private static final float DEFAULT_MAX_TANGENT = (float) Math.tan(Math.toRadians(35.0d));
    private static final float DEFAULT_MIN_ANGLE_DEGREES = 0.0f;
    private float mMaximumAngle;
    private float mMaximumTangent;
    private float mMinimumHorizontalAngle;
    private float mMinimumHorizontalTangent;
    private float mMinimumVerticalAngle;
    private float mMinimumVerticalTangent;

    public ArcMotion() {
        this.mMinimumHorizontalAngle = 0.0f;
        this.mMinimumVerticalAngle = 0.0f;
        this.mMaximumAngle = DEFAULT_MAX_ANGLE_DEGREES;
        this.mMinimumHorizontalTangent = 0.0f;
        this.mMinimumVerticalTangent = 0.0f;
        this.mMaximumTangent = DEFAULT_MAX_TANGENT;
    }

    public ArcMotion(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMinimumHorizontalAngle = 0.0f;
        this.mMinimumVerticalAngle = 0.0f;
        this.mMaximumAngle = DEFAULT_MAX_ANGLE_DEGREES;
        this.mMinimumHorizontalTangent = 0.0f;
        this.mMinimumVerticalTangent = 0.0f;
        this.mMaximumTangent = DEFAULT_MAX_TANGENT;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ArcMotion);
        setMinimumVerticalAngle(typedArrayObtainStyledAttributes.getFloat(1, 0.0f));
        setMinimumHorizontalAngle(typedArrayObtainStyledAttributes.getFloat(0, 0.0f));
        setMaximumAngle(typedArrayObtainStyledAttributes.getFloat(2, DEFAULT_MAX_ANGLE_DEGREES));
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setMinimumHorizontalAngle(float f) {
        this.mMinimumHorizontalAngle = f;
        this.mMinimumHorizontalTangent = toTangent(f);
    }

    public float getMinimumHorizontalAngle() {
        return this.mMinimumHorizontalAngle;
    }

    public void setMinimumVerticalAngle(float f) {
        this.mMinimumVerticalAngle = f;
        this.mMinimumVerticalTangent = toTangent(f);
    }

    public float getMinimumVerticalAngle() {
        return this.mMinimumVerticalAngle;
    }

    public void setMaximumAngle(float f) {
        this.mMaximumAngle = f;
        this.mMaximumTangent = toTangent(f);
    }

    public float getMaximumAngle() {
        return this.mMaximumAngle;
    }

    private static float toTangent(float f) {
        if (f < 0.0f || f > 90.0f) {
            throw new IllegalArgumentException("Arc must be between 0 and 90 degrees");
        }
        return (float) Math.tan(Math.toRadians(f / 2.0f));
    }

    @Override
    public Path getPath(float f, float f2, float f3, float f4) {
        float fAbs;
        float fAbs2;
        float f5;
        float f6;
        float f7;
        Path path = new Path();
        path.moveTo(f, f2);
        float f8 = f3 - f;
        float f9 = f4 - f2;
        float f10 = (f8 * f8) + (f9 * f9);
        float f11 = (f + f3) / 2.0f;
        float f12 = (f2 + f4) / 2.0f;
        float f13 = 0.25f * f10;
        boolean z = f2 > f4;
        if (f9 == 0.0f) {
            fAbs2 = (Math.abs(f8) * 0.5f * this.mMinimumHorizontalTangent) + f12;
            fAbs = f11;
        } else if (f8 == 0.0f) {
            fAbs = (Math.abs(f9) * 0.5f * this.mMinimumVerticalTangent) + f11;
            fAbs2 = f12;
        } else {
            if (Math.abs(f8) < Math.abs(f9)) {
                float fAbs3 = Math.abs(f10 / (f9 * 2.0f));
                if (z) {
                    fAbs2 = f4 + fAbs3;
                    fAbs = f3;
                } else {
                    fAbs2 = fAbs3 + f2;
                    fAbs = f;
                }
                f5 = this.mMinimumVerticalTangent * f13 * this.mMinimumVerticalTangent;
            } else {
                float f14 = f10 / (f8 * 2.0f);
                if (z) {
                    fAbs = f + f14;
                    fAbs2 = f2;
                } else {
                    fAbs = f3 - f14;
                    fAbs2 = f4;
                }
                f5 = this.mMinimumHorizontalTangent * f13 * this.mMinimumHorizontalTangent;
            }
            float f15 = f11 - fAbs;
            float f16 = f12 - fAbs2;
            f6 = (f15 * f15) + (f16 * f16);
            f7 = this.mMaximumTangent * f13 * this.mMaximumTangent;
            if (f6 == 0.0f && f6 < f5) {
                f7 = f5;
            } else if (f6 <= f7) {
                f7 = 0.0f;
            }
            if (f7 != 0.0f) {
                float fSqrt = (float) Math.sqrt(f7 / f6);
                fAbs = ((fAbs - f11) * fSqrt) + f11;
                fAbs2 = f12 + (fSqrt * (fAbs2 - f12));
            }
            path.cubicTo((f + fAbs) / 2.0f, (f2 + fAbs2) / 2.0f, (fAbs + f3) / 2.0f, (fAbs2 + f4) / 2.0f, f3, f4);
            return path;
        }
        f5 = 0.0f;
        float f152 = f11 - fAbs;
        float f162 = f12 - fAbs2;
        f6 = (f152 * f152) + (f162 * f162);
        f7 = this.mMaximumTangent * f13 * this.mMaximumTangent;
        if (f6 == 0.0f) {
            if (f6 <= f7) {
            }
        }
        if (f7 != 0.0f) {
        }
        path.cubicTo((f + fAbs) / 2.0f, (f2 + fAbs2) / 2.0f, (fAbs + f3) / 2.0f, (fAbs2 + f4) / 2.0f, f3, f4);
        return path;
    }
}
