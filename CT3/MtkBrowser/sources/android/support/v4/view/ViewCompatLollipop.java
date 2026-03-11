package android.support.v4.view;

import android.view.View;
import android.view.WindowInsets;

class ViewCompatLollipop {
    ViewCompatLollipop() {
    }

    public static void setOnApplyWindowInsetsListener(View view, final OnApplyWindowInsetsListener listener) {
        if (listener == null) {
            view.setOnApplyWindowInsetsListener(null);
        } else {
            view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view2, WindowInsets windowInsets) {
                    WindowInsetsCompatApi21 insets = new WindowInsetsCompatApi21(windowInsets);
                    return ((WindowInsetsCompatApi21) listener.onApplyWindowInsets(view2, insets)).unwrap();
                }
            });
        }
    }

    public static WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
        WindowInsets unwrapped;
        WindowInsets result;
        if ((insets instanceof WindowInsetsCompatApi21) && (result = v.onApplyWindowInsets((unwrapped = ((WindowInsetsCompatApi21) insets).unwrap()))) != unwrapped) {
            return new WindowInsetsCompatApi21(result);
        }
        return insets;
    }

    public static WindowInsetsCompat dispatchApplyWindowInsets(View v, WindowInsetsCompat insets) {
        WindowInsets unwrapped;
        WindowInsets result;
        if ((insets instanceof WindowInsetsCompatApi21) && (result = v.dispatchApplyWindowInsets((unwrapped = ((WindowInsetsCompatApi21) insets).unwrap()))) != unwrapped) {
            return new WindowInsetsCompatApi21(result);
        }
        return insets;
    }
}
