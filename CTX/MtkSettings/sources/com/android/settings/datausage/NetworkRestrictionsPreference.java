package com.android.settings.datausage;

import android.content.Context;
import android.net.NetworkTemplate;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import com.android.settings.datausage.TemplatePreference;

public class NetworkRestrictionsPreference extends Preference implements TemplatePreference {
    public NetworkRestrictionsPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setTemplate(NetworkTemplate networkTemplate, int i, TemplatePreference.NetworkServices networkServices) {
    }
}
