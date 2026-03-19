package android.view;

import android.graphics.Rect;
import com.android.internal.util.Preconditions;
import java.util.Objects;

public final class WindowInsets {
    private boolean mAlwaysConsumeNavBar;
    private DisplayCutout mDisplayCutout;
    private boolean mDisplayCutoutConsumed;
    private boolean mIsRound;
    private Rect mStableInsets;
    private boolean mStableInsetsConsumed;
    private Rect mSystemWindowInsets;
    private boolean mSystemWindowInsetsConsumed;
    private Rect mTempRect;
    private Rect mWindowDecorInsets;
    private boolean mWindowDecorInsetsConsumed;
    private static final Rect EMPTY_RECT = new Rect(0, 0, 0, 0);
    public static final WindowInsets CONSUMED = new WindowInsets(null, null, null, false, false, null);

    public WindowInsets(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, DisplayCutout displayCutout) {
        boolean z3;
        boolean z4;
        boolean z5;
        this.mSystemWindowInsetsConsumed = false;
        this.mWindowDecorInsetsConsumed = false;
        this.mStableInsetsConsumed = false;
        this.mDisplayCutoutConsumed = false;
        if (rect != null) {
            z3 = false;
        } else {
            z3 = true;
        }
        this.mSystemWindowInsetsConsumed = z3;
        this.mSystemWindowInsets = this.mSystemWindowInsetsConsumed ? EMPTY_RECT : new Rect(rect);
        if (rect2 != null) {
            z4 = false;
        } else {
            z4 = true;
        }
        this.mWindowDecorInsetsConsumed = z4;
        this.mWindowDecorInsets = this.mWindowDecorInsetsConsumed ? EMPTY_RECT : new Rect(rect2);
        if (rect3 != null) {
            z5 = false;
        } else {
            z5 = true;
        }
        this.mStableInsetsConsumed = z5;
        this.mStableInsets = this.mStableInsetsConsumed ? EMPTY_RECT : new Rect(rect3);
        this.mIsRound = z;
        this.mAlwaysConsumeNavBar = z2;
        this.mDisplayCutoutConsumed = displayCutout == null;
        this.mDisplayCutout = (this.mDisplayCutoutConsumed || displayCutout.isEmpty()) ? null : displayCutout;
    }

    public WindowInsets(WindowInsets windowInsets) {
        this.mSystemWindowInsetsConsumed = false;
        this.mWindowDecorInsetsConsumed = false;
        this.mStableInsetsConsumed = false;
        this.mDisplayCutoutConsumed = false;
        this.mSystemWindowInsets = windowInsets.mSystemWindowInsets;
        this.mWindowDecorInsets = windowInsets.mWindowDecorInsets;
        this.mStableInsets = windowInsets.mStableInsets;
        this.mSystemWindowInsetsConsumed = windowInsets.mSystemWindowInsetsConsumed;
        this.mWindowDecorInsetsConsumed = windowInsets.mWindowDecorInsetsConsumed;
        this.mStableInsetsConsumed = windowInsets.mStableInsetsConsumed;
        this.mIsRound = windowInsets.mIsRound;
        this.mAlwaysConsumeNavBar = windowInsets.mAlwaysConsumeNavBar;
        this.mDisplayCutout = windowInsets.mDisplayCutout;
        this.mDisplayCutoutConsumed = windowInsets.mDisplayCutoutConsumed;
    }

    public WindowInsets(Rect rect) {
        this(rect, null, null, false, false, null);
    }

    public Rect getSystemWindowInsets() {
        if (this.mTempRect == null) {
            this.mTempRect = new Rect();
        }
        if (this.mSystemWindowInsets != null) {
            this.mTempRect.set(this.mSystemWindowInsets);
        } else {
            this.mTempRect.setEmpty();
        }
        return this.mTempRect;
    }

    public int getSystemWindowInsetLeft() {
        return this.mSystemWindowInsets.left;
    }

    public int getSystemWindowInsetTop() {
        return this.mSystemWindowInsets.top;
    }

    public int getSystemWindowInsetRight() {
        return this.mSystemWindowInsets.right;
    }

    public int getSystemWindowInsetBottom() {
        return this.mSystemWindowInsets.bottom;
    }

    public int getWindowDecorInsetLeft() {
        return this.mWindowDecorInsets.left;
    }

    public int getWindowDecorInsetTop() {
        return this.mWindowDecorInsets.top;
    }

    public int getWindowDecorInsetRight() {
        return this.mWindowDecorInsets.right;
    }

    public int getWindowDecorInsetBottom() {
        return this.mWindowDecorInsets.bottom;
    }

    public boolean hasSystemWindowInsets() {
        return (this.mSystemWindowInsets.left == 0 && this.mSystemWindowInsets.top == 0 && this.mSystemWindowInsets.right == 0 && this.mSystemWindowInsets.bottom == 0) ? false : true;
    }

    public boolean hasWindowDecorInsets() {
        return (this.mWindowDecorInsets.left == 0 && this.mWindowDecorInsets.top == 0 && this.mWindowDecorInsets.right == 0 && this.mWindowDecorInsets.bottom == 0) ? false : true;
    }

    public boolean hasInsets() {
        return hasSystemWindowInsets() || hasWindowDecorInsets() || hasStableInsets() || this.mDisplayCutout != null;
    }

    public DisplayCutout getDisplayCutout() {
        return this.mDisplayCutout;
    }

    public WindowInsets consumeDisplayCutout() {
        WindowInsets windowInsets = new WindowInsets(this);
        windowInsets.mDisplayCutout = null;
        windowInsets.mDisplayCutoutConsumed = true;
        return windowInsets;
    }

    public boolean isConsumed() {
        return this.mSystemWindowInsetsConsumed && this.mWindowDecorInsetsConsumed && this.mStableInsetsConsumed && this.mDisplayCutoutConsumed;
    }

    public boolean isRound() {
        return this.mIsRound;
    }

    public WindowInsets consumeSystemWindowInsets() {
        WindowInsets windowInsets = new WindowInsets(this);
        windowInsets.mSystemWindowInsets = EMPTY_RECT;
        windowInsets.mSystemWindowInsetsConsumed = true;
        return windowInsets;
    }

    public WindowInsets consumeSystemWindowInsets(boolean z, boolean z2, boolean z3, boolean z4) {
        int i;
        int i2;
        int i3;
        if (z || z2 || z3 || z4) {
            WindowInsets windowInsets = new WindowInsets(this);
            if (!z) {
                i = this.mSystemWindowInsets.left;
            } else {
                i = 0;
            }
            if (!z2) {
                i2 = this.mSystemWindowInsets.top;
            } else {
                i2 = 0;
            }
            if (!z3) {
                i3 = this.mSystemWindowInsets.right;
            } else {
                i3 = 0;
            }
            windowInsets.mSystemWindowInsets = new Rect(i, i2, i3, z4 ? 0 : this.mSystemWindowInsets.bottom);
            return windowInsets;
        }
        return this;
    }

    public WindowInsets replaceSystemWindowInsets(int i, int i2, int i3, int i4) {
        WindowInsets windowInsets = new WindowInsets(this);
        windowInsets.mSystemWindowInsets = new Rect(i, i2, i3, i4);
        return windowInsets;
    }

    public WindowInsets replaceSystemWindowInsets(Rect rect) {
        WindowInsets windowInsets = new WindowInsets(this);
        windowInsets.mSystemWindowInsets = new Rect(rect);
        return windowInsets;
    }

    public WindowInsets consumeWindowDecorInsets() {
        WindowInsets windowInsets = new WindowInsets(this);
        windowInsets.mWindowDecorInsets.set(0, 0, 0, 0);
        windowInsets.mWindowDecorInsetsConsumed = true;
        return windowInsets;
    }

    public WindowInsets consumeWindowDecorInsets(boolean z, boolean z2, boolean z3, boolean z4) {
        int i;
        int i2;
        int i3;
        if (z || z2 || z3 || z4) {
            WindowInsets windowInsets = new WindowInsets(this);
            if (!z) {
                i = this.mWindowDecorInsets.left;
            } else {
                i = 0;
            }
            if (!z2) {
                i2 = this.mWindowDecorInsets.top;
            } else {
                i2 = 0;
            }
            if (!z3) {
                i3 = this.mWindowDecorInsets.right;
            } else {
                i3 = 0;
            }
            windowInsets.mWindowDecorInsets = new Rect(i, i2, i3, z4 ? 0 : this.mWindowDecorInsets.bottom);
            return windowInsets;
        }
        return this;
    }

    public WindowInsets replaceWindowDecorInsets(int i, int i2, int i3, int i4) {
        WindowInsets windowInsets = new WindowInsets(this);
        windowInsets.mWindowDecorInsets = new Rect(i, i2, i3, i4);
        return windowInsets;
    }

    public int getStableInsetTop() {
        return this.mStableInsets.top;
    }

    public int getStableInsetLeft() {
        return this.mStableInsets.left;
    }

    public int getStableInsetRight() {
        return this.mStableInsets.right;
    }

    public int getStableInsetBottom() {
        return this.mStableInsets.bottom;
    }

    public boolean hasStableInsets() {
        return (this.mStableInsets.top == 0 && this.mStableInsets.left == 0 && this.mStableInsets.right == 0 && this.mStableInsets.bottom == 0) ? false : true;
    }

    public WindowInsets consumeStableInsets() {
        WindowInsets windowInsets = new WindowInsets(this);
        windowInsets.mStableInsets = EMPTY_RECT;
        windowInsets.mStableInsetsConsumed = true;
        return windowInsets;
    }

    public boolean shouldAlwaysConsumeNavBar() {
        return this.mAlwaysConsumeNavBar;
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("WindowInsets{systemWindowInsets=");
        sb.append(this.mSystemWindowInsets);
        sb.append(" windowDecorInsets=");
        sb.append(this.mWindowDecorInsets);
        sb.append(" stableInsets=");
        sb.append(this.mStableInsets);
        if (this.mDisplayCutout != null) {
            str = " cutout=" + this.mDisplayCutout;
        } else {
            str = "";
        }
        sb.append(str);
        sb.append(isRound() ? " round" : "");
        sb.append("}");
        return sb.toString();
    }

    public WindowInsets inset(Rect rect) {
        return inset(rect.left, rect.top, rect.right, rect.bottom);
    }

    public WindowInsets inset(int i, int i2, int i3, int i4) {
        Preconditions.checkArgumentNonnegative(i);
        Preconditions.checkArgumentNonnegative(i2);
        Preconditions.checkArgumentNonnegative(i3);
        Preconditions.checkArgumentNonnegative(i4);
        WindowInsets windowInsets = new WindowInsets(this);
        if (!windowInsets.mSystemWindowInsetsConsumed) {
            windowInsets.mSystemWindowInsets = insetInsets(windowInsets.mSystemWindowInsets, i, i2, i3, i4);
        }
        if (!windowInsets.mWindowDecorInsetsConsumed) {
            windowInsets.mWindowDecorInsets = insetInsets(windowInsets.mWindowDecorInsets, i, i2, i3, i4);
        }
        if (!windowInsets.mStableInsetsConsumed) {
            windowInsets.mStableInsets = insetInsets(windowInsets.mStableInsets, i, i2, i3, i4);
        }
        if (this.mDisplayCutout != null) {
            windowInsets.mDisplayCutout = windowInsets.mDisplayCutout.inset(i, i2, i3, i4);
            if (windowInsets.mDisplayCutout.isEmpty()) {
                windowInsets.mDisplayCutout = null;
            }
        }
        return windowInsets;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof WindowInsets)) {
            return false;
        }
        WindowInsets windowInsets = (WindowInsets) obj;
        if (this.mIsRound == windowInsets.mIsRound && this.mAlwaysConsumeNavBar == windowInsets.mAlwaysConsumeNavBar && this.mSystemWindowInsetsConsumed == windowInsets.mSystemWindowInsetsConsumed && this.mWindowDecorInsetsConsumed == windowInsets.mWindowDecorInsetsConsumed && this.mStableInsetsConsumed == windowInsets.mStableInsetsConsumed && this.mDisplayCutoutConsumed == windowInsets.mDisplayCutoutConsumed && Objects.equals(this.mSystemWindowInsets, windowInsets.mSystemWindowInsets) && Objects.equals(this.mWindowDecorInsets, windowInsets.mWindowDecorInsets) && Objects.equals(this.mStableInsets, windowInsets.mStableInsets) && Objects.equals(this.mDisplayCutout, windowInsets.mDisplayCutout)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mSystemWindowInsets, this.mWindowDecorInsets, this.mStableInsets, Boolean.valueOf(this.mIsRound), this.mDisplayCutout, Boolean.valueOf(this.mAlwaysConsumeNavBar), Boolean.valueOf(this.mSystemWindowInsetsConsumed), Boolean.valueOf(this.mWindowDecorInsetsConsumed), Boolean.valueOf(this.mStableInsetsConsumed), Boolean.valueOf(this.mDisplayCutoutConsumed));
    }

    private static Rect insetInsets(Rect rect, int i, int i2, int i3, int i4) {
        int iMax = Math.max(0, rect.left - i);
        int iMax2 = Math.max(0, rect.top - i2);
        int iMax3 = Math.max(0, rect.right - i3);
        int iMax4 = Math.max(0, rect.bottom - i4);
        if (iMax == i && iMax2 == i2 && iMax3 == i3 && iMax4 == i4) {
            return rect;
        }
        return new Rect(iMax, iMax2, iMax3, iMax4);
    }

    boolean isSystemWindowInsetsConsumed() {
        return this.mSystemWindowInsetsConsumed;
    }
}
