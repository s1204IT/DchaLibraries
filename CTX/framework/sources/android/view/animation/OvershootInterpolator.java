package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.android.internal.R;
import com.android.internal.view.animation.HasNativeInterpolator;
import com.android.internal.view.animation.NativeInterpolatorFactory;
import com.android.internal.view.animation.NativeInterpolatorFactoryHelper;

@HasNativeInterpolator
public class OvershootInterpolator extends BaseInterpolator implements NativeInterpolatorFactory {
    private final float mTension;

    public OvershootInterpolator() {
        this.mTension = 2.0f;
    }

    public OvershootInterpolator(float f) {
        this.mTension = f;
    }

    public OvershootInterpolator(Context context, AttributeSet attributeSet) {
        this(context.getResources(), context.getTheme(), attributeSet);
    }

    public OvershootInterpolator(Resources resources, Resources.Theme theme, AttributeSet attributeSet) {
        TypedArray typedArrayObtainAttributes;
        if (theme != null) {
            typedArrayObtainAttributes = theme.obtainStyledAttributes(attributeSet, R.styleable.OvershootInterpolator, 0, 0);
        } else {
            typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.OvershootInterpolator);
        }
        this.mTension = typedArrayObtainAttributes.getFloat(0, 2.0f);
        setChangingConfiguration(typedArrayObtainAttributes.getChangingConfigurations());
        typedArrayObtainAttributes.recycle();
    }

    @Override
    public float getInterpolation(float f) {
        float f2 = f - 1.0f;
        return (f2 * f2 * (((this.mTension + 1.0f) * f2) + this.mTension)) + 1.0f;
    }

    @Override
    public long createNativeInterpolator() {
        return NativeInterpolatorFactoryHelper.createOvershootInterpolator(this.mTension);
    }
}
