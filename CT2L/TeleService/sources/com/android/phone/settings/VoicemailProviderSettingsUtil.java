package com.android.phone.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CallForwardInfo;

public class VoicemailProviderSettingsUtil {
    private static final String LOG_TAG = VoicemailProviderSettingsUtil.class.getSimpleName();

    public static VoicemailProviderSettings load(Context context, String key) {
        SharedPreferences prefs = getPrefs(context);
        String vmNumberSetting = prefs.getString(key + "#VMNumber", null);
        if (vmNumberSetting == null) {
            Log.w(LOG_TAG, "VoiceMailProvider settings for the key \"" + key + "\" were not found. Returning null.");
            return null;
        }
        CallForwardInfo[] cfi = VoicemailProviderSettings.NO_FORWARDING;
        String fwdKey = key + "#FWDSettings";
        int fwdLen = prefs.getInt(fwdKey + "#Length", 0);
        if (fwdLen > 0) {
            cfi = new CallForwardInfo[fwdLen];
            for (int i = 0; i < cfi.length; i++) {
                String settingKey = fwdKey + "#Setting" + String.valueOf(i);
                cfi[i] = new CallForwardInfo();
                cfi[i].status = prefs.getInt(settingKey + "#Status", 0);
                cfi[i].reason = prefs.getInt(settingKey + "#Reason", 5);
                cfi[i].serviceClass = 1;
                cfi[i].toa = 145;
                cfi[i].number = prefs.getString(settingKey + "#Number", "");
                cfi[i].timeSeconds = prefs.getInt(settingKey + "#Time", 20);
            }
        }
        VoicemailProviderSettings settings = new VoicemailProviderSettings(vmNumberSetting, cfi);
        log("Loaded settings for " + key + ": " + settings.toString());
        return settings;
    }

    public static void save(Context context, String key, VoicemailProviderSettings newSettings) {
        VoicemailProviderSettings curSettings = load(context, key);
        if (newSettings.equals(curSettings)) {
            log("save: Not saving setting for " + key + " since they have not changed");
            return;
        }
        log("Saving settings for " + key + ": " + newSettings.toString());
        SharedPreferences prefs = getPrefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key + "#VMNumber", newSettings.getVoicemailNumber());
        String fwdKey = key + "#FWDSettings";
        CallForwardInfo[] s = newSettings.getForwardingSettings();
        if (s != VoicemailProviderSettings.NO_FORWARDING) {
            editor.putInt(fwdKey + "#Length", s.length);
            for (int i = 0; i < s.length; i++) {
                String settingKey = fwdKey + "#Setting" + String.valueOf(i);
                CallForwardInfo fi = s[i];
                editor.putInt(settingKey + "#Status", fi.status);
                editor.putInt(settingKey + "#Reason", fi.reason);
                editor.putString(settingKey + "#Number", fi.number);
                editor.putInt(settingKey + "#Time", fi.timeSeconds);
            }
        } else {
            editor.putInt(fwdKey + "#Length", 0);
        }
        editor.apply();
    }

    public static void delete(Context context, String key) {
        log("Deleting settings for" + key);
        if (!TextUtils.isEmpty(key)) {
            SharedPreferences prefs = getPrefs(context);
            prefs.edit().putString(key + "#VMNumber", null).putInt(key + "#FWDSettings#Length", 0).commit();
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("vm_numbers", 0);
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
