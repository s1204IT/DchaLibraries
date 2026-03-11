package com.android.launcher3.anim;

import android.graphics.Path;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

public class Interpolators {
    public static final Interpolator EXAGGERATED_EASE;
    private static final float FAST_FLING_PX_MS = 10.0f;
    public static final Interpolator OVERSHOOT_1_2;
    public static final Interpolator SCROLL;
    public static final Interpolator SCROLL_CUBIC;
    public static final Interpolator TOUCH_RESPONSE_INTERPOLATOR;
    public static final Interpolator ZOOM_IN;
    public static final Interpolator ZOOM_OUT;
    public static final Interpolator LINEAR = new LinearInterpolator();
    public static final Interpolator ACCEL = new AccelerateInterpolator();
    public static final Interpolator ACCEL_1_5 = new AccelerateInterpolator(1.5f);
    public static final Interpolator ACCEL_2 = new AccelerateInterpolator(2.0f);
    public static final Interpolator DEACCEL = new DecelerateInterpolator();
    public static final Interpolator DEACCEL_1_5 = new DecelerateInterpolator(1.5f);
    public static final Interpolator DEACCEL_1_7 = new DecelerateInterpolator(1.7f);
    public static final Interpolator DEACCEL_2 = new DecelerateInterpolator(2.0f);
    public static final Interpolator DEACCEL_2_5 = new DecelerateInterpolator(2.5f);
    public static final Interpolator DEACCEL_3 = new DecelerateInterpolator(3.0f);
    public static final Interpolator FAST_OUT_SLOW_IN = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    public static final Interpolator AGGRESSIVE_EASE = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
    public static final Interpolator AGGRESSIVE_EASE_IN_OUT = new PathInterpolator(0.6f, 0.0f, 0.4f, 1.0f);

    static {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(0.05f, 0.0f, 0.133333f, 0.08f, 0.166666f, 0.4f);
        path.cubicTo(0.225f, 0.94f, 0.5f, 1.0f, 1.0f, 1.0f);
        EXAGGERATED_EASE = new PathInterpolator(path);
        OVERSHOOT_1_2 = new OvershootInterpolator(1.2f);
        TOUCH_RESPONSE_INTERPOLATOR = new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);
        ZOOM_IN = new Interpolator() {
            @Override
            public float getInterpolation(float f) {
                return Interpolators.DEACCEL_3.getInterpolation(1.0f - Interpolators.ZOOM_OUT.getInterpolation(1.0f - f));
            }
        };
        ZOOM_OUT = new Interpolator() {
            private static final float FOCAL_LENGTH = 0.35f;

            @Override
            public float getInterpolation(float f) {
                return zInterpolate(f);
            }

            private float zInterpolate(float f) {
                return (1.0f - (FOCAL_LENGTH / (f + FOCAL_LENGTH))) / 0.7407408f;
            }
        };
        SCROLL = new Interpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f - 1.0f;
                return (f2 * f2 * f2 * f2 * f2) + 1.0f;
            }
        };
        SCROLL_CUBIC = new Interpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f - 1.0f;
                return (f2 * f2 * f2) + 1.0f;
            }
        };
    }

    public static Interpolator scrollInterpolatorForVelocity(float f) {
        return Math.abs(f) > FAST_FLING_PX_MS ? SCROLL : SCROLL_CUBIC;
    }

    public static Interpolator clampToProgress(final Interpolator interpolator, final float f, final float f2) {
        if (f2 <= f) {
            throw new IllegalArgumentException("lowerBound must be less than upperBound");
        }
        return new Interpolator() {
            @Override
            public final float getInterpolation(float f3) {
                return Interpolators.lambda$clampToProgress$0(f, f2, interpolator, f3);
            }
        };
    }

    static float lambda$clampToProgress$0(float f, float f2, Interpolator interpolator, float f3) {
        if (f3 < f) {
            return 0.0f;
        }
        if (f3 > f2) {
            return 1.0f;
        }
        return interpolator.getInterpolation((f3 - f) / (f2 - f));
    }
}
