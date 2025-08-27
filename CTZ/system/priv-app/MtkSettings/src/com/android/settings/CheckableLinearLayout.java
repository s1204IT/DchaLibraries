package com.android.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Checkable;
import android.widget.LinearLayout;

/* loaded from: classes.dex */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private boolean mChecked;
    private float mDisabledAlpha;

    public CheckableLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, typedValue, true);
        this.mDisabledAlpha = typedValue.getFloat();
    }

    @Override // android.view.View
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setAlpha(z ? 1.0f : this.mDisabledAlpha);
        }
    }

    @Override // android.widget.Checkable
    public void setChecked(boolean z) {
        this.mChecked = z;
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
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            KeyEvent.Callback childAt = getChildAt(i);
            if (childAt instanceof Checkable) {
                ((Checkable) childAt).setChecked(this.mChecked);
            }
        }
    }
}
