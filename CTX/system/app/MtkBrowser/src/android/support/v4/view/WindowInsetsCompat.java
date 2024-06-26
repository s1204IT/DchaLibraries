package android.support.v4.view;

import android.os.Build;
import android.view.WindowInsets;
/* loaded from: classes.dex */
public class WindowInsetsCompat {
    private final Object mInsets;

    private WindowInsetsCompat(Object insets) {
        this.mInsets = insets;
    }

    public int getSystemWindowInsetLeft() {
        if (Build.VERSION.SDK_INT >= 20) {
            return ((WindowInsets) this.mInsets).getSystemWindowInsetLeft();
        }
        return 0;
    }

    public int getSystemWindowInsetTop() {
        if (Build.VERSION.SDK_INT >= 20) {
            return ((WindowInsets) this.mInsets).getSystemWindowInsetTop();
        }
        return 0;
    }

    public int getSystemWindowInsetRight() {
        if (Build.VERSION.SDK_INT >= 20) {
            return ((WindowInsets) this.mInsets).getSystemWindowInsetRight();
        }
        return 0;
    }

    public int getSystemWindowInsetBottom() {
        if (Build.VERSION.SDK_INT >= 20) {
            return ((WindowInsets) this.mInsets).getSystemWindowInsetBottom();
        }
        return 0;
    }

    public boolean isConsumed() {
        if (Build.VERSION.SDK_INT >= 21) {
            return ((WindowInsets) this.mInsets).isConsumed();
        }
        return false;
    }

    public WindowInsetsCompat replaceSystemWindowInsets(int left, int top, int right, int bottom) {
        if (Build.VERSION.SDK_INT >= 20) {
            return new WindowInsetsCompat(((WindowInsets) this.mInsets).replaceSystemWindowInsets(left, top, right, bottom));
        }
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WindowInsetsCompat other = (WindowInsetsCompat) o;
        if (this.mInsets == null) {
            if (other.mInsets == null) {
                return true;
            }
            return false;
        }
        return this.mInsets.equals(other.mInsets);
    }

    public int hashCode() {
        if (this.mInsets == null) {
            return 0;
        }
        return this.mInsets.hashCode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static WindowInsetsCompat wrap(Object insets) {
        if (insets == null) {
            return null;
        }
        return new WindowInsetsCompat(insets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Object unwrap(WindowInsetsCompat insets) {
        if (insets == null) {
            return null;
        }
        return insets.mInsets;
    }
}
