package com.android.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private boolean mChecked;
    private float mDisabledAlpha;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedValue alpha = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, alpha, true);
        this.mDisabledAlpha = alpha.getFloat();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            getChildAt(i).setAlpha(enabled ? 1.0f : this.mDisabledAlpha);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        this.mChecked = checked;
        updateChecked();
    }

    @Override
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!this.mChecked);
    }

    private void updateChecked() {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            KeyEvent.Callback childAt = getChildAt(i);
            if (childAt instanceof Checkable) {
                ((Checkable) childAt).setChecked(this.mChecked);
            }
        }
    }
}
