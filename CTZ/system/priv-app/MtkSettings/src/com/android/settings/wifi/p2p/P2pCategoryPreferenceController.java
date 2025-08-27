package com.android.settings.wifi.p2p;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public abstract class P2pCategoryPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    protected PreferenceGroup mCategory;

    public P2pCategoryPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mCategory = (PreferenceGroup) preferenceScreen.findPreference(getPreferenceKey());
    }

    public void removeAllChildren() {
        if (this.mCategory != null) {
            this.mCategory.removeAll();
            this.mCategory.setVisible(false);
        }
    }

    public void addChild(Preference preference) {
        if (this.mCategory != null) {
            this.mCategory.addPreference(preference);
            this.mCategory.setVisible(true);
        }
    }

    public void setEnabled(boolean z) {
        if (this.mCategory != null) {
            this.mCategory.setEnabled(z);
        }
    }
}
