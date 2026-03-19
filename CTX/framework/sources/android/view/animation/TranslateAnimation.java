package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.animation.Animation;
import com.android.internal.R;

public class TranslateAnimation extends Animation {
    protected float mFromXDelta;
    private int mFromXType;
    protected float mFromXValue;
    protected float mFromYDelta;
    private int mFromYType;
    protected float mFromYValue;
    protected float mToXDelta;
    private int mToXType;
    protected float mToXValue;
    protected float mToYDelta;
    private int mToYType;
    protected float mToYValue;

    public TranslateAnimation(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFromXType = 0;
        this.mToXType = 0;
        this.mFromYType = 0;
        this.mToYType = 0;
        this.mFromXValue = 0.0f;
        this.mToXValue = 0.0f;
        this.mFromYValue = 0.0f;
        this.mToYValue = 0.0f;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TranslateAnimation);
        Animation.Description value = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(0));
        this.mFromXType = value.type;
        this.mFromXValue = value.value;
        Animation.Description value2 = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(1));
        this.mToXType = value2.type;
        this.mToXValue = value2.value;
        Animation.Description value3 = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(2));
        this.mFromYType = value3.type;
        this.mFromYValue = value3.value;
        Animation.Description value4 = Animation.Description.parseValue(typedArrayObtainStyledAttributes.peekValue(3));
        this.mToYType = value4.type;
        this.mToYValue = value4.value;
        typedArrayObtainStyledAttributes.recycle();
    }

    public TranslateAnimation(float f, float f2, float f3, float f4) {
        this.mFromXType = 0;
        this.mToXType = 0;
        this.mFromYType = 0;
        this.mToYType = 0;
        this.mFromXValue = 0.0f;
        this.mToXValue = 0.0f;
        this.mFromYValue = 0.0f;
        this.mToYValue = 0.0f;
        this.mFromXValue = f;
        this.mToXValue = f2;
        this.mFromYValue = f3;
        this.mToYValue = f4;
        this.mFromXType = 0;
        this.mToXType = 0;
        this.mFromYType = 0;
        this.mToYType = 0;
    }

    public TranslateAnimation(int i, float f, int i2, float f2, int i3, float f3, int i4, float f4) {
        this.mFromXType = 0;
        this.mToXType = 0;
        this.mFromYType = 0;
        this.mToYType = 0;
        this.mFromXValue = 0.0f;
        this.mToXValue = 0.0f;
        this.mFromYValue = 0.0f;
        this.mToYValue = 0.0f;
        this.mFromXValue = f;
        this.mToXValue = f2;
        this.mFromYValue = f3;
        this.mToYValue = f4;
        this.mFromXType = i;
        this.mToXType = i2;
        this.mFromYType = i3;
        this.mToYType = i4;
    }

    @Override
    protected void applyTransformation(float f, Transformation transformation) {
        float f2 = this.mFromXDelta;
        float f3 = this.mFromYDelta;
        if (this.mFromXDelta != this.mToXDelta) {
            f2 = this.mFromXDelta + ((this.mToXDelta - this.mFromXDelta) * f);
        }
        if (this.mFromYDelta != this.mToYDelta) {
            f3 = this.mFromYDelta + ((this.mToYDelta - this.mFromYDelta) * f);
        }
        transformation.getMatrix().setTranslate(f2, f3);
    }

    @Override
    public void initialize(int i, int i2, int i3, int i4) {
        super.initialize(i, i2, i3, i4);
        this.mFromXDelta = resolveSize(this.mFromXType, this.mFromXValue, i, i3);
        this.mToXDelta = resolveSize(this.mToXType, this.mToXValue, i, i3);
        this.mFromYDelta = resolveSize(this.mFromYType, this.mFromYValue, i2, i4);
        this.mToYDelta = resolveSize(this.mToYType, this.mToYValue, i2, i4);
    }
}
