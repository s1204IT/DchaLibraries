package com.android.settings.datausage;

import android.net.NetworkTemplate;
import android.support.v7.preference.Preference;
import com.android.settings.datausage.TemplatePreference;

public class NetworkRestrictionsPreference extends Preference implements TemplatePreference {
    @Override
    public void setTemplate(NetworkTemplate template, int subId, TemplatePreference.NetworkServices services) {
    }
}
