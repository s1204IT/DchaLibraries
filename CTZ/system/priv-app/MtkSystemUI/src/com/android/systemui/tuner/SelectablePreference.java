package com.android.systemui.tuner;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.CheckBoxPreference;
import android.util.TypedValue;
import com.android.systemui.R;
import com.android.systemui.statusbar.ScalingDrawableWrapper;

/* loaded from: classes.dex */
public class SelectablePreference extends CheckBoxPreference {
    private final int mSize;

    public SelectablePreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        setSelectable(true);
        this.mSize = (int) TypedValue.applyDimension(1, 32.0f, context.getResources().getDisplayMetrics());
    }

    @Override // android.support.v7.preference.Preference
    public void setIcon(Drawable drawable) {
        super.setIcon(new ScalingDrawableWrapper(drawable, this.mSize / drawable.getIntrinsicWidth()));
    }

    @Override // android.support.v7.preference.Preference
    public String toString() {
        return "";
    }
}
