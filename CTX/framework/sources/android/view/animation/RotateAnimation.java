package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.animation.Animation;
import com.android.internal.R;

public class RotateAnimation extends Animation {
    private float mFromDegrees;
    private float mPivotX;
    private int mPivotXType;
    private float mPivotXValue;
    private float mPivotY;
    private int mPivotYType;
    private float mPivotYValue;
    private float mToDegrees;

    public RotateAnimation(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPivotXType = 0;
        this.mPivotYType = 0;
        this.mPivotXValue = 0.0f;
        this.mPivotYValue = 0.0f;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RotateAnimation);
        this.mFromDegrees = typedArrayObtainStyledAttributes.getFloat(0, 0.0f);
        this.mToDegrees = typedArrayObtainStyledAttributes.getFloat(1, 0.0f);
        Animation.Description value = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(2));
        this.mPivotXType = value.type;
        this.mPivotXValue = value.value;
        Animation.Description value2 = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(3));
        this.mPivotYType = value2.type;
        this.mPivotYValue = value2.value;
        typedArrayObtainStyledAttributes.recycle();
        initializePivotPoint();
    }

    public RotateAnimation(float f, float f2) {
        this.mPivotXType = 0;
        this.mPivotYType = 0;
        this.mPivotXValue = 0.0f;
        this.mPivotYValue = 0.0f;
        this.mFromDegrees = f;
        this.mToDegrees = f2;
        this.mPivotX = 0.0f;
        this.mPivotY = 0.0f;
    }

    public RotateAnimation(float f, float f2, float f3, float f4) {
        this.mPivotXType = 0;
        this.mPivotYType = 0;
        this.mPivotXValue = 0.0f;
        this.mPivotYValue = 0.0f;
        this.mFromDegrees = f;
        this.mToDegrees = f2;
        this.mPivotXType = 0;
        this.mPivotYType = 0;
        this.mPivotXValue = f3;
        this.mPivotYValue = f4;
        initializePivotPoint();
    }

    public RotateAnimation(float f, float f2, int i, float f3, int i2, float f4) {
        this.mPivotXType = 0;
        this.mPivotYType = 0;
        this.mPivotXValue = 0.0f;
        this.mPivotYValue = 0.0f;
        this.mFromDegrees = f;
        this.mToDegrees = f2;
        this.mPivotXValue = f3;
        this.mPivotXType = i;
        this.mPivotYValue = f4;
        this.mPivotYType = i2;
        initializePivotPoint();
    }

    private void initializePivotPoint() {
        if (this.mPivotXType == 0) {
            this.mPivotX = this.mPivotXValue;
        }
        if (this.mPivotYType == 0) {
            this.mPivotY = this.mPivotYValue;
        }
    }

    @Override
    protected void applyTransformation(float f, Transformation transformation) {
        float f2 = this.mFromDegrees + ((this.mToDegrees - this.mFromDegrees) * f);
        float scaleFactor = getScaleFactor();
        if (this.mPivotX == 0.0f && this.mPivotY == 0.0f) {
            transformation.getMatrix().setRotate(f2);
        } else {
            transformation.getMatrix().setRotate(f2, this.mPivotX * scaleFactor, this.mPivotY * scaleFactor);
        }
    }

    @Override
    public void initialize(int i, int i2, int i3, int i4) {
        super.initialize(i, i2, i3, i4);
        this.mPivotX = resolveSize(this.mPivotXType, this.mPivotXValue, i, i3);
        this.mPivotY = resolveSize(this.mPivotYType, this.mPivotYValue, i2, i4);
    }
}
