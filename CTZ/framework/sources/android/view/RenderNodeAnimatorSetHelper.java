package android.view;

import android.animation.TimeInterpolator;
import com.android.internal.view.animation.FallbackLUTInterpolator;
import com.android.internal.view.animation.NativeInterpolatorFactory;
import com.android.internal.view.animation.NativeInterpolatorFactoryHelper;

public class RenderNodeAnimatorSetHelper {
    public static RenderNode getTarget(DisplayListCanvas displayListCanvas) {
        return displayListCanvas.mNode;
    }

    public static long createNativeInterpolator(TimeInterpolator timeInterpolator, long j) {
        if (timeInterpolator == null) {
            return NativeInterpolatorFactoryHelper.createLinearInterpolator();
        }
        if (RenderNodeAnimator.isNativeInterpolator(timeInterpolator)) {
            return ((NativeInterpolatorFactory) timeInterpolator).createNativeInterpolator();
        }
        return FallbackLUTInterpolator.createNativeInterpolator(timeInterpolator, j);
    }
}
