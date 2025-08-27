package com.android.settings.applications;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.ViewGroup;
import com.android.settings.R;

/* loaded from: classes.dex */
public class SpacePreference extends Preference {
    private int mHeight;

    public SpacePreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle, android.R.attr.preferenceStyle));
    }

    public SpacePreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SpacePreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setLayoutResource(R.layout.space_preference);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, new int[]{android.R.attr.layout_height}, i, i2);
        this.mHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.itemView.setLayoutParams(new ViewGroup.LayoutParams(-1, this.mHeight));
    }
}
