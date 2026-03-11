package com.android.settings.display;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.settings.R;
import com.android.settingslib.display.DisplayDensityUtils;

public class ScreenZoomPreference extends PreferenceGroup {
    public ScreenZoomPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceScreenStyle, android.R.attr.preferenceScreenStyle));
        if (TextUtils.isEmpty(getFragment())) {
            setFragment("com.android.settings.display.ScreenZoomSettings");
        }
        DisplayDensityUtils density = new DisplayDensityUtils(context);
        int defaultIndex = density.getCurrentIndex();
        if (defaultIndex < 0) {
            setVisible(false);
            setEnabled(false);
        } else {
            if (!TextUtils.isEmpty(getSummary())) {
                return;
            }
            String[] entries = density.getEntries();
            int currentIndex = density.getCurrentIndex();
            setSummary(entries[currentIndex]);
        }
    }

    @Override
    protected boolean isOnSameScreenAsChildren() {
        return false;
    }
}
