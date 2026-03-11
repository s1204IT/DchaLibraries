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
        setIcon(R.drawable.ic_vpn_key);
        setIconSize(2);
    }

    public VpnProfile getProfile() {
        return this.mProfile;
    }

    public void setProfile(VpnProfile vpnProfile) {
        String str = this.mProfile != null ? this.mProfile.name : null;
        String str2 = vpnProfile != null ? vpnProfile.name : null;
        if (!TextUtils.equals(str, str2)) {
            setTitle(str2);
            notifyHierarchyChanged();
        }
        this.mProfile = vpnProfile;
    }

    @Override
    public int compareTo(Preference preference) {
        if (preference instanceof LegacyVpnPreference) {
            LegacyVpnPreference legacyVpnPreference = (LegacyVpnPreference) preference;
            int i = legacyVpnPreference.mState - this.mState;
            if (i == 0) {
                int iCompareToIgnoreCase = this.mProfile.name.compareToIgnoreCase(legacyVpnPreference.mProfile.name);
                if (iCompareToIgnoreCase != 0) {
                    return iCompareToIgnoreCase;
                }
                int i2 = this.mProfile.type - legacyVpnPreference.mProfile.type;
                if (i2 == 0) {
                    return this.mProfile.key.compareTo(legacyVpnPreference.mProfile.key);
                }
                return i2;
            }
            return i;
        }
        if (preference instanceof AppPreference) {
            AppPreference appPreference = (AppPreference) preference;
            if (this.mState != 3 && appPreference.getState() == 3) {
                return 1;
            }
            return -1;
        }
        return super.compareTo(preference);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.settings_button && isDisabledByAdmin()) {
            performClick();
        } else {
            super.onClick(view);
        }
    }
}
