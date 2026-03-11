package com.mediatek.settings.network;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWWOPJoynSettingsExt;
import java.util.List;

public class RcsePreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver {
    IWWOPJoynSettingsExt mJoynExt;

    public RcsePreferenceController(Context context) {
        super(context);
        this.mJoynExt = UtilsExt.getWWOPJoynSettingsExt(context);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!this.mJoynExt.isJoynSettingsEnabled() && "rcse_settings".equals(preference.getKey())) {
            try {
                this.mContext.startActivity(new Intent("com.mediatek.rcse.RCSE_SETTINGS"));
                return true;
            } catch (ActivityNotFoundException e) {
                Log.w("RcsePrefContr", "handlePreferenceTreeClick: startActivity failed" + e);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        if (this.mJoynExt.isJoynSettingsEnabled()) {
            Log.d("RcsePrefContr", "com.mediatek.rcse.RCSE_SETTINGS is enabled");
            return true;
        }
        Log.d("RcsePrefContr", "com.mediatek.rcse.RCSE_SETTINGS is not enabled");
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return "rcse_settings";
    }

    @Override
    public void updateNonIndexableKeys(List<String> list) {
        if (!isAvailable()) {
            list.add(getPreferenceKey());
        }
    }
}
