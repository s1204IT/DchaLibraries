package com.mediatek.settings.display;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.hdmi.HdimReflectionHelper;

public class HdmiPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private Object mHdmiManager;

    public HdmiPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        this.mHdmiManager = HdimReflectionHelper.getHdmiService();
        return this.mHdmiManager != null;
    }

    @Override
    public String getPreferenceKey() {
        return "hdmi_settings";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        if (!isAvailable()) {
            setVisible(preferenceScreen, getPreferenceKey(), false);
            return;
        }
        Preference preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey());
        if (this.mHdmiManager != null) {
            String string = this.mContext.getString(R.string.hdmi_replace_hdmi);
            int hdmiDisplayTypeConstant = HdimReflectionHelper.getHdmiDisplayTypeConstant("DISPLAY_TYPE_MHL");
            int hdmiDisplayTypeConstant2 = HdimReflectionHelper.getHdmiDisplayTypeConstant("DISPLAY_TYPE_SLIMPORT");
            int hdmiDisplayType = HdimReflectionHelper.getHdmiDisplayType(this.mHdmiManager);
            if (hdmiDisplayType == hdmiDisplayTypeConstant) {
                String string2 = this.mContext.getString(R.string.hdmi_replace_mhl);
                preferenceFindPreference.setTitle(preferenceFindPreference.getTitle().toString().replaceAll(string, string2));
                preferenceFindPreference.setSummary(preferenceFindPreference.getSummary().toString().replaceAll(string, string2));
            } else if (hdmiDisplayType == hdmiDisplayTypeConstant2) {
                String string3 = this.mContext.getString(R.string.slimport_replace_hdmi);
                preferenceFindPreference.setTitle(preferenceFindPreference.getTitle().toString().replaceAll(string, string3));
                preferenceFindPreference.setSummary(preferenceFindPreference.getSummary().toString().replaceAll(string, string3));
            }
        }
    }
}
