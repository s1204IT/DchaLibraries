package com.android.settings.dashboard;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

class DashboardTilePlaceholderPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private int mOrder;

    public DashboardTilePlaceholderPreferenceController(Context context) {
        super(context);
        this.mOrder = Preference.DEFAULT_ORDER;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        Preference preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey());
        if (preferenceFindPreference != null) {
            this.mOrder = preferenceFindPreference.getOrder();
            preferenceScreen.removePreference(preferenceFindPreference);
        }
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return "dashboard_tile_placeholder";
    }

    public int getOrder() {
        return this.mOrder;
    }
}
