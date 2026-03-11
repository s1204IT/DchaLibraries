package com.android.settings.notification;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class EmergencyTonePreferenceController extends SettingPrefController {
    public EmergencyTonePreferenceController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, Lifecycle lifecycle) {
        super(context, settingsPreferenceFragment, lifecycle);
        this.mPreference = new SettingPref(1, "emergency_tone", "emergency_tone", 0, 1, 2, 0) {
            @Override
            public boolean isApplicable(Context context2) {
                TelephonyManager telephonyManager = (TelephonyManager) context2.getSystemService("phone");
                return telephonyManager != null && telephonyManager.getCurrentPhoneType() == 2;
            }

            @Override
            protected String getCaption(Resources resources, int i) {
                switch (i) {
                    case 0:
                        return resources.getString(R.string.emergency_tone_silent);
                    case 1:
                        return resources.getString(R.string.emergency_tone_alert);
                    case 2:
                        return resources.getString(R.string.emergency_tone_vibrate);
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };
    }
}
