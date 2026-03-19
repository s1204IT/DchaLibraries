package android.support.v4.view.animation;

import android.os.Build;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

public final class PathInterpolatorCompat {
    public static Interpolator create(float controlX1, float controlY1, float controlX2, float controlY2) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new PathInterpolator(controlX1, controlY1, controlX2, controlY2);
        }
        return new PathInterpolatorApi14(controlX1, controlY1, controlX2, controlY2);
    }
}
