package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;
import com.android.internal.view.animation.HasNativeInterpolator;
import com.android.internal.view.animation.NativeInterpolatorFactory;
import com.android.internal.view.animation.NativeInterpolatorFactoryHelper;

@HasNativeInterpolator
public class LinearInterpolator extends BaseInterpolator implements NativeInterpolatorFactory {
    public LinearInterpolator() {
    }

    public LinearInterpolator(Context context, AttributeSet attributeSet) {
    }

    @Override
    public float getInterpolation(float f) {
        return f;
    }

    @Override
    public long createNativeInterpolator() {
        return NativeInterpolatorFactoryHelper.createLinearInterpolator();
    }
}
