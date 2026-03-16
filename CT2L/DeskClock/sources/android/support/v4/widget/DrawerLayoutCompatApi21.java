package android.support.v4.widget;

import android.R;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

class DrawerLayoutCompatApi21 {
    private static final int[] THEME_ATTRS = {R.attr.colorPrimaryDark};

    public static void dispatchChildInsets(View child, Object insets, int gravity) {
        WindowInsets wi = (WindowInsets) insets;
        if (gravity == 3) {
            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(), wi.getSystemWindowInsetTop(), 0, wi.getSystemWindowInsetBottom());
        } else if (gravity == 5) {
            wi = wi.replaceSystemWindowInsets(0, wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(), wi.getSystemWindowInsetBottom());
        }
        child.dispatchApplyWindowInsets(wi);
    }

    public static void applyMarginInsets(ViewGroup.MarginLayoutParams lp, Object insets, int gravity) {
        WindowInsets wi = (WindowInsets) insets;
        if (gravity == 3) {
            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(), wi.getSystemWindowInsetTop(), 0, wi.getSystemWindowInsetBottom());
        } else if (gravity == 5) {
            wi = wi.replaceSystemWindowInsets(0, wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(), wi.getSystemWindowInsetBottom());
        }
        lp.leftMargin = wi.getSystemWindowInsetLeft();
        lp.topMargin = wi.getSystemWindowInsetTop();
        lp.rightMargin = wi.getSystemWindowInsetRight();
        lp.bottomMargin = wi.getSystemWindowInsetBottom();
    }

    public static int getTopInset(Object insets) {
        if (insets != null) {
            return ((WindowInsets) insets).getSystemWindowInsetTop();
        }
        return 0;
    }
}
