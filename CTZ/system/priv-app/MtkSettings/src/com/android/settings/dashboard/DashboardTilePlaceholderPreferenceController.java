package com.android.settings.dashboard;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
class DashboardTilePlaceholderPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private int mOrder;

    public DashboardTilePlaceholderPreferenceController(Context context) {
        super(context);
        this.mOrder = Preference.DEFAULT_ORDER;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        Preference preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey());
        if (preferenceFindPreference != null) {
            this.mOrder = preferenceFindPreference.getOrder();
            preferenceScreen.removePreference(preferenceFindPreference);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "dashboard_tile_placeholder";
    }

    public int getOrder() {
        return this.mOrder;
    }
}
