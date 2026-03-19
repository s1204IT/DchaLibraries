package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.android.internal.R;

public class AlphaAnimation extends Animation {
    private float mFromAlpha;
    private float mToAlpha;

    public AlphaAnimation(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AlphaAnimation);
        this.mFromAlpha = typedArrayObtainStyledAttributes.getFloat(0, 1.0f);
        this.mToAlpha = typedArrayObtainStyledAttributes.getFloat(1, 1.0f);
        typedArrayObtainStyledAttributes.recycle();
    }

    public AlphaAnimation(float f, float f2) {
        this.mFromAlpha = f;
        this.mToAlpha = f2;
    }

    @Override
    protected void applyTransformation(float f, Transformation transformation) {
        float f2 = this.mFromAlpha;
        transformation.setAlpha(f2 + ((this.mToAlpha - f2) * f));
    }

    @Override
    public boolean willChangeTransformationMatrix() {
        return false;
    }

    @Override
    public boolean willChangeBounds() {
        return false;
    }

    @Override
    public boolean hasAlpha() {
        return true;
    }
}
