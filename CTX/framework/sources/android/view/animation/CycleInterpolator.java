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
public class CycleInterpolator extends BaseInterpolator implements NativeInterpolatorFactory {
    private float mCycles;

    public CycleInterpolator(float f) {
        this.mCycles = f;
    }

    public CycleInterpolator(Context context, AttributeSet attributeSet) {
        this(context.getResources(), context.getTheme(), attributeSet);
    }

    public CycleInterpolator(Resources resources, Resources.Theme theme, AttributeSet attributeSet) {
        TypedArray typedArrayObtainAttributes;
        if (theme != null) {
            typedArrayObtainAttributes = theme.obtainStyledAttributes(attributeSet, R.styleable.CycleInterpolator, 0, 0);
        } else {
            typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.CycleInterpolator);
        }
        this.mCycles = typedArrayObtainAttributes.getFloat(0, 1.0f);
        setChangingConfiguration(typedArrayObtainAttributes.getChangingConfigurations());
        typedArrayObtainAttributes.recycle();
    }

    @Override
    public float getInterpolation(float f) {
        return (float) Math.sin(((double) (2.0f * this.mCycles)) * 3.141592653589793d * ((double) f));
    }

    @Override
    public long createNativeInterpolator() {
        return NativeInterpolatorFactoryHelper.createCycleInterpolator(this.mCycles);
    }
}
