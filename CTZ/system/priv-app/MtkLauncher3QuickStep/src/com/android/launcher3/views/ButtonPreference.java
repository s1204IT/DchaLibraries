package com.android.launcher3.views;

import android.R;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/* loaded from: classes.dex */
public class ButtonPreference extends Preference {
    private boolean mWidgetFrameVisible;

    public ButtonPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mWidgetFrameVisible = false;
    }

    public ButtonPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mWidgetFrameVisible = false;
    }

    public ButtonPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mWidgetFrameVisible = false;
    }

    public ButtonPreference(Context context) {
        super(context);
        this.mWidgetFrameVisible = false;
    }

    public void setWidgetFrameVisible(boolean z) {
        if (this.mWidgetFrameVisible != z) {
            this.mWidgetFrameVisible = z;
            notifyChanged();
        }
    }

    @Override // android.preference.Preference
    protected void onBindView(View view) {
        super.onBindView(view);
        ViewGroup viewGroup = (ViewGroup) view.findViewById(R.id.widget_frame);
        if (viewGroup != null) {
            viewGroup.setVisibility(this.mWidgetFrameVisible ? 0 : 8);
        }
    }
}
