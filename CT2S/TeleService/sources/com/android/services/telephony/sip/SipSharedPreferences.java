package com.android.services.telephony.sip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import com.android.phone.R;

public class SipSharedPreferences {
    private Context mContext;
    private SharedPreferences mPreferences;

    public SipSharedPreferences(Context context) {
        this.mPreferences = context.getSharedPreferences("SIP_PREFERENCES", 1);
        this.mContext = context;
    }

    @Deprecated
    public String getPrimaryAccount() {
        return this.mPreferences.getString("primary", null);
    }

    public void setProfilesCount(int number) {
        SharedPreferences.Editor editor = this.mPreferences.edit();
        editor.putInt("profiles", number);
        editor.apply();
    }

    public void setSipCallOption(String option) {
        Settings.System.putString(this.mContext.getContentResolver(), "sip_call_options", option);
        Intent intent = new Intent("com.android.phone.SIP_CALL_OPTION_CHANGED");
        this.mContext.sendBroadcast(intent);
    }

    public String getSipCallOption() {
        String option = Settings.System.getString(this.mContext.getContentResolver(), "sip_call_options");
        return option != null ? option : this.mContext.getString(R.string.sip_address_only);
    }

    public void setReceivingCallsEnabled(boolean enabled) {
        Settings.System.putInt(this.mContext.getContentResolver(), "sip_receive_calls", enabled ? 1 : 0);
    }

    public boolean isReceivingCallsEnabled() {
        try {
            return Settings.System.getInt(this.mContext.getContentResolver(), "sip_receive_calls") != 0;
        } catch (Settings.SettingNotFoundException e) {
            log("isReceivingCallsEnabled, option not set; use default value, exception: " + e);
            return false;
        }
    }

    public void cleanupPrimaryAccountSetting() {
        if (this.mPreferences.contains("primary")) {
            SharedPreferences.Editor editor = this.mPreferences.edit();
            editor.remove("primary");
            editor.apply();
        }
    }

    private static void log(String msg) {
        Log.d("SIP", "[SipSharedPreferences] " + msg);
    }
}
