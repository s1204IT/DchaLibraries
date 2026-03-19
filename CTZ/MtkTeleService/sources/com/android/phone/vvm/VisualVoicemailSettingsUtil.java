package com.android.phone.vvm;

import android.content.Context;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;

public class VisualVoicemailSettingsUtil {
    public static Bundle dump(Context context, PhoneAccountHandle phoneAccountHandle) {
        Bundle bundle = new Bundle();
        VisualVoicemailPreferences visualVoicemailPreferences = new VisualVoicemailPreferences(context, phoneAccountHandle);
        if (visualVoicemailPreferences.contains("is_enabled")) {
            bundle.putBoolean("android.telephony.extra.VISUAL_VOICEMAIL_ENABLED_BY_USER_BOOL", visualVoicemailPreferences.getBoolean("is_enabled", false));
        }
        bundle.putString("android.telephony.extra.VOICEMAIL_SCRAMBLED_PIN_STRING", visualVoicemailPreferences.getString("default_old_pin"));
        return bundle;
    }
}
