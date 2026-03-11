package com.android.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settingslib.RestrictedPreference;

public class DimmableIconPreference extends RestrictedPreference {
    private final CharSequence mContentDescription;

    public DimmableIconPreference(Context context) {
        this(context, (AttributeSet) null);
    }

    public DimmableIconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContentDescription = null;
        useAdminDisabledSummary(true);
    }

    public DimmableIconPreference(Context context, CharSequence contentDescription) {
        super(context);
        this.mContentDescription = contentDescription;
        useAdminDisabledSummary(true);
    }

    private void dimIcon(boolean dimmed) {
        Drawable icon = getIcon();
        if (icon == null) {
            return;
        }
        icon.mutate().setAlpha(dimmed ? 102 : 255);
        setIcon(icon);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (!TextUtils.isEmpty(this.mContentDescription)) {
            TextView titleView = (TextView) view.findViewById(android.R.id.title);
            titleView.setContentDescription(this.mContentDescription);
        }
        dimIcon(!isEnabled());
    }
}
