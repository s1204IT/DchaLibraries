package android.support.v4.view;

import android.view.View;
import android.view.ViewParent;
/* loaded from: a.zip:android/support/v4/view/ViewCompatJB.class */
class ViewCompatJB {
    ViewCompatJB() {
    }

    public static boolean getFitsSystemWindows(View view) {
        return view.getFitsSystemWindows();
    }

    public static int getImportantForAccessibility(View view) {
        return view.getImportantForAccessibility();
    }

    public static int getMinimumHeight(View view) {
        return view.getMinimumHeight();
    }

    public static int getMinimumWidth(View view) {
        return view.getMinimumWidth();
    }

    public static ViewParent getParentForAccessibility(View view) {
        return view.getParentForAccessibility();
    }

    public static boolean hasOverlappingRendering(View view) {
        return view.hasOverlappingRendering();
    }

    public static boolean hasTransientState(View view) {
        return view.hasTransientState();
    }

    public static void postInvalidateOnAnimation(View view) {
        view.postInvalidateOnAnimation();
    }

    public static void postInvalidateOnAnimation(View view, int i, int i2, int i3, int i4) {
        view.postInvalidate(i, i2, i3, i4);
    }

    public static void postOnAnimation(View view, Runnable runnable) {
        view.postOnAnimation(runnable);
    }

    public static void postOnAnimationDelayed(View view, Runnable runnable, long j) {
        view.postOnAnimationDelayed(runnable, j);
    }

    public static void requestApplyInsets(View view) {
        view.requestFitSystemWindows();
    }

    public static void setImportantForAccessibility(View view, int i) {
        view.setImportantForAccessibility(i);
    }
}
