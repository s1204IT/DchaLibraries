package com.android.systemui.statusbar;

import android.util.Log;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.phone.StatusBar;

/* loaded from: classes.dex */
public class CrossFadeHelper {
    public static void fadeOut(View view, Runnable runnable) {
        fadeOut(view, 210L, 0, runnable);
    }

    public static void fadeOut(View view, long j, int i, Runnable runnable) {
        view.animate().cancel();
        if (StatusBar.DEBUG) {
            Log.d("CrossFadeHelper", "<fadeOut> view: " + view + ", duration: " + j + ", delay: " + i + ", endRunnable: " + runnable);
        }
        view.animate().alpha(0.0f).setDuration(j).setInterpolator(Interpolators.ALPHA_OUT).setStartDelay(i).withEndAction(new Runnable() { // from class: com.android.systemui.statusbar.CrossFadeHelper.1
            final /* synthetic */ Runnable val$endRunnable;
            final /* synthetic */ View val$view;

            AnonymousClass1(Runnable runnable2, View view2) {
                runnable = runnable2;
                view = view2;
            }

            @Override // java.lang.Runnable
            public void run() {
                if (runnable != null) {
                    runnable.run();
                }
                view.setVisibility(4);
                if (StatusBar.DEBUG) {
                    Log.d("CrossFadeHelper", "<fadeOut> execute endRunnable, view: " + view + ", " + runnable);
                }
            }
        });
        if (view2.hasOverlappingRendering()) {
            view2.animate().withLayer();
        }
    }

    /* renamed from: com.android.systemui.statusbar.CrossFadeHelper$1 */
    class AnonymousClass1 implements Runnable {
        final /* synthetic */ Runnable val$endRunnable;
        final /* synthetic */ View val$view;

        AnonymousClass1(Runnable runnable2, View view2) {
            runnable = runnable2;
            view = view2;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (runnable != null) {
                runnable.run();
            }
            view.setVisibility(4);
            if (StatusBar.DEBUG) {
                Log.d("CrossFadeHelper", "<fadeOut> execute endRunnable, view: " + view + ", " + runnable);
            }
        }
    }

    public static void fadeOut(View view, float f) {
        fadeOut(view, f, true);
    }

    public static void fadeOut(View view, float f, boolean z) {
        view.animate().cancel();
        if (f == 1.0f) {
            view.setVisibility(4);
        } else if (view.getVisibility() == 4) {
            view.setVisibility(0);
        }
        if (z) {
            f = mapToFadeDuration(f);
        }
        float interpolation = Interpolators.ALPHA_OUT.getInterpolation(1.0f - f);
        view.setAlpha(interpolation);
        updateLayerType(view, interpolation);
    }

    private static float mapToFadeDuration(float f) {
        return Math.min(f / 0.5833333f, 1.0f);
    }

    private static void updateLayerType(View view, float f) {
        if (view.hasOverlappingRendering() && f > 0.0f && f < 1.0f) {
            view.setLayerType(2, null);
        } else if (view.getLayerType() == 2) {
            view.setLayerType(0, null);
        }
    }

    public static void fadeIn(View view) {
        fadeIn(view, 210L, 0);
    }

    public static void fadeIn(View view, long j, int i) {
        view.animate().cancel();
        if (view.getVisibility() == 4) {
            view.setAlpha(0.0f);
            view.setVisibility(0);
        }
        view.animate().alpha(1.0f).setDuration(j).setStartDelay(i).setInterpolator(Interpolators.ALPHA_IN).withEndAction(null);
        if (view.hasOverlappingRendering()) {
            view.animate().withLayer();
        }
    }

    public static void fadeIn(View view, float f) {
        fadeIn(view, f, true);
    }

    public static void fadeIn(View view, float f, boolean z) {
        view.animate().cancel();
        if (view.getVisibility() == 4) {
            view.setVisibility(0);
        }
        if (z) {
            f = mapToFadeDuration(f);
        }
        float interpolation = Interpolators.ALPHA_IN.getInterpolation(f);
        view.setAlpha(interpolation);
        updateLayerType(view, interpolation);
    }
}
