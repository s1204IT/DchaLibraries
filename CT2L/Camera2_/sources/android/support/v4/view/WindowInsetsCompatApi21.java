package android.support.v4.view;

import android.graphics.Rect;
import android.view.WindowInsets;

class WindowInsetsCompatApi21 extends WindowInsetsCompat {
    private final WindowInsets mSource;

    WindowInsetsCompatApi21(WindowInsets source) {
        this.mSource = source;
    }

    @Override
    public int getSystemWindowInsetLeft() {
        return this.mSource.getSystemWindowInsetLeft();
    }

    @Override
    public int getSystemWindowInsetTop() {
        return this.mSource.getSystemWindowInsetTop();
    }

    @Override
    public int getSystemWindowInsetRight() {
        return this.mSource.getSystemWindowInsetRight();
    }

    @Override
    public int getSystemWindowInsetBottom() {
        return this.mSource.getSystemWindowInsetBottom();
    }

    @Override
    public boolean hasSystemWindowInsets() {
        return this.mSource.hasSystemWindowInsets();
    }

    @Override
    public boolean hasInsets() {
        return this.mSource.hasInsets();
    }

    @Override
    public boolean isConsumed() {
        return this.mSource.isConsumed();
    }

    @Override
    public boolean isRound() {
        return this.mSource.isRound();
    }

    @Override
    public WindowInsetsCompat consumeSystemWindowInsets() {
        return new WindowInsetsCompatApi21(this.mSource.consumeSystemWindowInsets());
    }

    @Override
    public WindowInsetsCompat replaceSystemWindowInsets(int left, int top, int right, int bottom) {
        return new WindowInsetsCompatApi21(this.mSource.replaceSystemWindowInsets(left, top, right, bottom));
    }

    @Override
    public WindowInsetsCompat replaceSystemWindowInsets(Rect systemWindowInsets) {
        return new WindowInsetsCompatApi21(this.mSource.replaceSystemWindowInsets(systemWindowInsets));
    }

    @Override
    public int getStableInsetTop() {
        return this.mSource.getStableInsetTop();
    }

    @Override
    public int getStableInsetLeft() {
        return this.mSource.getStableInsetLeft();
    }

    @Override
    public int getStableInsetRight() {
        return this.mSource.getStableInsetRight();
    }

    @Override
    public int getStableInsetBottom() {
        return this.mSource.getStableInsetBottom();
    }

    @Override
    public boolean hasStableInsets() {
        return this.mSource.hasStableInsets();
    }

    @Override
    public WindowInsetsCompat consumeStableInsets() {
        return new WindowInsetsCompatApi21(this.mSource.consumeStableInsets());
    }

    WindowInsets unwrap() {
        return this.mSource;
    }
}
