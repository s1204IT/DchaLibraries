package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;
import java.util.UUID;

/* loaded from: classes.dex */
public class WifiTetherPasswordPreferenceController extends WifiTetherBasePreferenceController implements ValidatedEditTextPreference.Validator {
    private String mPassword;

    public WifiTetherPasswordPreferenceController(Context context, WifiTetherBasePreferenceController.OnTetherConfigUpdateListener onTetherConfigUpdateListener) {
        super(context, onTetherConfigUpdateListener);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "wifi_tether_network_password";
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController
    public void updateDisplay() {
        WifiConfiguration wifiApConfiguration = this.mWifiManager.getWifiApConfiguration();
        if (wifiApConfiguration == null || (wifiApConfiguration.getAuthType() == 4 && TextUtils.isEmpty(wifiApConfiguration.preSharedKey))) {
            this.mPassword = generateRandomPassword();
        } else {
            this.mPassword = wifiApConfiguration.preSharedKey;
            Log.d("PrefControllerMixin", "Updating password in Preference, " + this.mPassword);
        }
        ((ValidatedEditTextPreference) this.mPreference).setValidator(this);
        ((ValidatedEditTextPreference) this.mPreference).setIsPassword(true);
        ((ValidatedEditTextPreference) this.mPreference).setIsSummaryPassword(true);
        updatePasswordDisplay((EditTextPreference) this.mPreference);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mPassword = (String) obj;
        updatePasswordDisplay((EditTextPreference) this.mPreference);
        this.mListener.onTetherConfigUpdated();
        return true;
    }

    public String getPasswordValidated(int i) {
        if (i == 0) {
            return "";
        }
        if (!isTextValid(this.mPassword)) {
            this.mPassword = generateRandomPassword();
            updatePasswordDisplay((EditTextPreference) this.mPreference);
        }
        return this.mPassword;
    }

    public void updateVisibility(int i) {
        this.mPreference.setVisible(i != 0);
    }

    @Override // com.android.settings.widget.ValidatedEditTextPreference.Validator
    public boolean isTextValid(String str) {
        return WifiUtils.isHotspotPasswordValid(str);
    }

    private static String generateRandomPassword() {
        String string = UUID.randomUUID().toString();
        return string.substring(0, 8) + string.substring(9, 13);
    }

    private void updatePasswordDisplay(EditTextPreference editTextPreference) {
        ValidatedEditTextPreference validatedEditTextPreference = (ValidatedEditTextPreference) editTextPreference;
        validatedEditTextPreference.setText(this.mPassword);
        if (!TextUtils.isEmpty(this.mPassword)) {
            validatedEditTextPreference.setIsSummaryPassword(true);
            validatedEditTextPreference.setSummary(this.mPassword);
            validatedEditTextPreference.setVisible(true);
        } else {
            validatedEditTextPreference.setIsSummaryPassword(false);
            validatedEditTextPreference.setSummary(R.string.wifi_hotspot_no_password_subtext);
            validatedEditTextPreference.setVisible(false);
        }
    }

    public void setPassword(String str) {
        this.mPassword = str;
        updatePasswordDisplay((EditTextPreference) this.mPreference);
    }

    public void setEnabled(boolean z) {
        this.mPreference.setEnabled(z);
        if (z && TextUtils.isEmpty(this.mPassword)) {
            String string = UUID.randomUUID().toString();
            this.mPassword = string.substring(0, 8) + string.substring(9, 13);
            updatePasswordDisplay((EditTextPreference) this.mPreference);
        }
    }
}
