package com.android.settings.deviceinfo;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;

/* loaded from: classes.dex */
public class DeviceModelPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private static IDeviceInfoSettingsExt mExt;
    private final Fragment mHost;

    public DeviceModelPreferenceController(Context context, Fragment fragment) {
        super(context);
        this.mHost = fragment;
        mExt = UtilsExt.getDeviceInfoSettingsExt(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_device_model);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference("device_model");
        if (preferenceFindPreference != null) {
            preferenceFindPreference.setSummary(this.mContext.getResources().getString(R.string.model_summary, getDeviceModel()));
            mExt.updateSummary(preferenceFindPreference, Build.MODEL, this.mContext.getResources().getString(R.string.model_summary, getDeviceModel()));
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "device_model";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), "device_model")) {
            return false;
        }
        HardwareInfoDialogFragment.newInstance().show(this.mHost.getFragmentManager(), "HardwareInfo");
        return true;
    }

    public static String getDeviceModel() {
        String str = SystemProperties.get("ro.boot.rsc");
        if (TextUtils.isEmpty(str)) {
            str = Build.MODEL + DeviceInfoUtils.getMsvSuffix();
        }
        return UtilsExt.useDeviceInfoSettingsExt().customeModelInfo(str);
    }
}
