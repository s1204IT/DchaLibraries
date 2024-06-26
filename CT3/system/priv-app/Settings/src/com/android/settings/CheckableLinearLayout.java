package com.android.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
/* loaded from: classes.dex */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private boolean mChecked;
    private float mDisabledAlpha;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedValue alpha = new TypedValue();
        context.getTheme().resolveAttribute(16842803, alpha, true);
        this.mDisabledAlpha = alpha.getFloat();
    }

    @Override // android.view.View
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            getChildAt(i).setAlpha(enabled ? 1.0f : this.mDisabledAlpha);
        }
    }

    @Override // android.widget.Checkable
    public void setChecked(boolean checked) {
        this.mChecked = checked;
        updateChecked();
    }

    @Override // android.widget.Checkable
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override // android.widget.Checkable
    public void toggle() {
        setChecked(!this.mChecked);
    }

    private void updateChecked() {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View child = getChildAt(i);
            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(this.mChecked);
            }
        }
    }
}
