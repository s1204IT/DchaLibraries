package com.android.settings.deviceinfo.firmwareversion;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class FirmwareVersionPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final Fragment mFragment;

    public FirmwareVersionPreferenceController(Context context, Fragment fragment) {
        super(context);
        this.mFragment = fragment;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey());
        if (preferenceFindPreference != null) {
            preferenceFindPreference.setSummary(Build.VERSION.RELEASE);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "firmware_version";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        FirmwareVersionDialogFragment.show(this.mFragment);
        return true;
    }
}
