package com.android.settings.vpn2;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;

public class LegacyVpnPreference extends ManageablePreference {
    private VpnProfile mProfile;

    LegacyVpnPreference(Context context) {
        super(context, null);
        setIcon(R.mipmap.ic_launcher_settings);
    }

    public VpnProfile getProfile() {
        return this.mProfile;
    }

    public void setProfile(VpnProfile profile) {
        String str = this.mProfile != null ? this.mProfile.name : null;
        String newLabel = profile != null ? profile.name : null;
        if (!TextUtils.equals(str, newLabel)) {
            setTitle(newLabel);
            notifyHierarchyChanged();
        }
        this.mProfile = profile;
    }

    @Override
    public int compareTo(Preference preference) {
        if (preference instanceof LegacyVpnPreference) {
            LegacyVpnPreference another = (LegacyVpnPreference) preference;
            int result = another.mState - this.mState;
            if (result == 0) {
                int result2 = this.mProfile.name.compareToIgnoreCase(another.mProfile.name);
                if (result2 == 0) {
                    int result3 = this.mProfile.type - another.mProfile.type;
                    if (result3 == 0) {
                        return this.mProfile.key.compareTo(another.mProfile.key);
                    }
                    return result3;
                }
                return result2;
            }
            return result;
        }
        if (preference instanceof AppPreference) {
            AppPreference another2 = (AppPreference) preference;
            if (this.mState != 3 && another2.getState() == 3) {
                return 1;
            }
            return -1;
        }
        return super.compareTo(preference);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_button && isDisabledByAdmin()) {
            performClick();
        } else {
            super.onClick(v);
        }
    }
}
