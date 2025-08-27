package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class FccEquipmentIdPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public FccEquipmentIdPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !TextUtils.isEmpty(SystemProperties.get("ro.ril.fccid"));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference("fcc_equipment_id");
        if (preferenceFindPreference != null) {
            preferenceFindPreference.setSummary(SystemProperties.get("ro.ril.fccid", this.mContext.getResources().getString(R.string.device_info_default)));
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "fcc_equipment_id";
    }
}
