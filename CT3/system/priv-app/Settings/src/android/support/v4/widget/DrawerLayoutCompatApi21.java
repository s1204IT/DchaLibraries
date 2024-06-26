package android.support.v4.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
/* loaded from: classes.dex */
class DrawerLayoutCompatApi21 {
    private static final int[] THEME_ATTRS = {16843828};

    DrawerLayoutCompatApi21() {
    }

    public static void configureApplyInsets(View drawerLayout) {
        if (!(drawerLayout instanceof DrawerLayoutImpl)) {
            return;
        }
        drawerLayout.setOnApplyWindowInsetsListener(new InsetsListener());
        drawerLayout.setSystemUiVisibility(1280);
    }

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

    public static Drawable getDefaultStatusBarBackground(Context context) {
        TypedArray a = context.obtainStyledAttributes(THEME_ATTRS);
        try {
            return a.getDrawable(0);
        } finally {
            a.recycle();
        }
    }

    /* loaded from: classes.dex */
    static class InsetsListener implements View.OnApplyWindowInsetsListener {
        InsetsListener() {
        }

        @Override // android.view.View.OnApplyWindowInsetsListener
        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
            DrawerLayoutImpl drawerLayout = (DrawerLayoutImpl) v;
            drawerLayout.setChildInsets(insets, insets.getSystemWindowInsetTop() > 0);
            return insets.consumeSystemWindowInsets();
        }
    }
}
