package com.android.settingslib.widget;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import com.android.settingslib.R;

public class FooterPreference extends Preference {
    public FooterPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, TypedArrayUtils.getAttr(context, R.attr.footerPreferenceStyle, android.R.attr.preferenceStyle));
        init();
    }

    private void init() {
        setIcon(R.drawable.ic_info_outline_24dp);
        setKey("footer_preference");
        setOrder(2147483646);
        setSelectable(false);
    }
}
