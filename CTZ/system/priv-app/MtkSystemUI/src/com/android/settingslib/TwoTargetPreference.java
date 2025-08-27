package com.android.settingslib;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/* loaded from: classes.dex */
public class TwoTargetPreference extends Preference {
    private int mIconSize;
    private int mMediumIconSize;
    private int mSmallIconSize;

    public TwoTargetPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        init(context);
    }

    public TwoTargetPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    private void init(Context context) {
        setLayoutResource(R.layout.preference_two_target);
        this.mSmallIconSize = context.getResources().getDimensionPixelSize(R.dimen.two_target_pref_small_icon_size);
        this.mMediumIconSize = context.getResources().getDimensionPixelSize(R.dimen.two_target_pref_medium_icon_size);
        int secondTargetResId = getSecondTargetResId();
        if (secondTargetResId != 0) {
            setWidgetLayoutResource(secondTargetResId);
        }
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        ImageView imageView = (ImageView) preferenceViewHolder.itemView.findViewById(android.R.id.icon);
        switch (this.mIconSize) {
            case 1:
                imageView.setLayoutParams(new LinearLayout.LayoutParams(this.mMediumIconSize, this.mMediumIconSize));
                break;
            case 2:
                imageView.setLayoutParams(new LinearLayout.LayoutParams(this.mSmallIconSize, this.mSmallIconSize));
                break;
        }
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.two_target_divider);
        View viewFindViewById2 = preferenceViewHolder.findViewById(android.R.id.widget_frame);
        boolean zShouldHideSecondTarget = shouldHideSecondTarget();
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(zShouldHideSecondTarget ? 8 : 0);
        }
        if (viewFindViewById2 != null) {
            viewFindViewById2.setVisibility(zShouldHideSecondTarget ? 8 : 0);
        }
    }

    protected boolean shouldHideSecondTarget() {
        return getSecondTargetResId() == 0;
    }

    protected int getSecondTargetResId() {
        return 0;
    }
}
