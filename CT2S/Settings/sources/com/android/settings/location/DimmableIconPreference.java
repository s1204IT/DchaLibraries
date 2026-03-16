package com.android.settings.location;

import android.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

public class DimmableIconPreference extends Preference {
    private final CharSequence mContentDescription;

    public DimmableIconPreference(Context context, CharSequence contentDescription) {
        super(context);
        this.mContentDescription = contentDescription;
    }

    private void dimIcon(boolean dimmed) {
        Drawable icon = getIcon();
        if (icon != null) {
            icon.mutate().setAlpha(dimmed ? 102 : 255);
            setIcon(icon);
        }
    }

    @Override
    public void onParentChanged(Preference parent, boolean disableChild) {
        dimIcon(disableChild);
        super.onParentChanged(parent, disableChild);
    }

    @Override
    public void setEnabled(boolean enabled) {
        dimIcon(!enabled);
        super.setEnabled(enabled);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (!TextUtils.isEmpty(this.mContentDescription)) {
            TextView titleView = (TextView) view.findViewById(R.id.title);
            titleView.setContentDescription(this.mContentDescription);
        }
    }
}
