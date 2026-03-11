package com.android.settings.deviceinfo;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;

public class ImeiInformation extends PreferenceActivity {
    private boolean isMultiSIM = false;
    private SubscriptionManager mSubscriptionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSubscriptionManager = SubscriptionManager.from(this);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        initPreferenceScreen(telephonyManager.getSimCount());
    }

    private void initPreferenceScreen(int slotCount) {
        this.isMultiSIM = slotCount > 1;
        for (int slotId = 0; slotId < slotCount; slotId++) {
            addPreferencesFromResource(R.xml.device_info_phone_status);
            setPreferenceValue(slotId);
            setNewKey(slotId);
        }
    }

    private void setPreferenceValue(int phoneId) {
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            if (phone.getPhoneType() == 2) {
                setSummaryText("meid_number", phone.getMeid());
                setSummaryText("min_number", phone.getCdmaMin());
                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference("min_number").setTitle(R.string.status_msid_number);
                }
                setSummaryText("prl_version", phone.getCdmaPrlVersion());
                removePreferenceFromScreen("imei_sv");
                if (phone.getLteOnCdmaMode() == 1) {
                    setSummaryText("icc_id", phone.getIccSerialNumber());
                    setSummaryText("imei", phone.getImei());
                    return;
                } else {
                    removePreferenceFromScreen("imei");
                    removePreferenceFromScreen("icc_id");
                    return;
                }
            }
            setSummaryText("imei", phone.getDeviceId());
            setSummaryText("imei_sv", ((TelephonyManager) getSystemService("phone")).getDeviceSoftwareVersion(phoneId));
            removePreferenceFromScreen("prl_version");
            removePreferenceFromScreen("meid_number");
            removePreferenceFromScreen("min_number");
            removePreferenceFromScreen("icc_id");
        }
    }

    private void setNewKey(int slotId) {
        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = prefScreen.getPreference(i);
            String key = pref.getKey();
            if (!key.startsWith("_")) {
                pref.setKey("_" + key + String.valueOf(slotId));
                updateTitle(pref, slotId);
            }
        }
    }

    private void updateTitle(Preference pref, int slotId) {
        if (pref != null) {
            String title = pref.getTitle().toString();
            if (this.isMultiSIM) {
                title = title + " " + getResources().getString(R.string.slot_number, Integer.valueOf(slotId + 1));
            }
            pref.setTitle(title);
        }
    }

    private void setSummaryText(String key, String text) {
        Preference preference = findPreference(key);
        if (TextUtils.isEmpty(text)) {
            text = getResources().getString(R.string.device_info_default);
        }
        if (preference != null) {
            preference.setSummary(text);
        }
    }

    private void removePreferenceFromScreen(String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }
}
