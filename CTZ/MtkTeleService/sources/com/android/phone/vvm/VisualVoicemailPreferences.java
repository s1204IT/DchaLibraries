package com.android.phone.vvm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telecom.PhoneAccountHandle;

public class VisualVoicemailPreferences {
    private final PhoneAccountHandle mPhoneAccountHandle;
    private final SharedPreferences mPreferences;

    public VisualVoicemailPreferences(Context context, PhoneAccountHandle phoneAccountHandle) {
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mPhoneAccountHandle = phoneAccountHandle;
    }

    public boolean getBoolean(String str, boolean z) {
        return ((Boolean) getValue(str, Boolean.valueOf(z))).booleanValue();
    }

    public String getString(String str) {
        return (String) getValue(str, null);
    }

    public boolean contains(String str) {
        return this.mPreferences.contains(getKey(str));
    }

    private <T> T getValue(String str, T t) {
        T t2;
        if (!contains(str) || (t2 = (T) this.mPreferences.getAll().get(getKey(str))) == null) {
            return t;
        }
        return t2;
    }

    private String getKey(String str) {
        return "visual_voicemail_" + str + "_" + this.mPhoneAccountHandle.getId();
    }
}
