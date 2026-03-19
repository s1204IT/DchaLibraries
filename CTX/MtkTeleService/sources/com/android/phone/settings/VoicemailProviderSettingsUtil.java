package com.android.phone.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CallForwardInfo;

public class VoicemailProviderSettingsUtil {
    private static final String LOG_TAG = VoicemailProviderSettingsUtil.class.getSimpleName();

    public static VoicemailProviderSettings load(Context context, String str) {
        SharedPreferences prefs = getPrefs(context);
        String string = prefs.getString(str + "#VMNumber", null);
        if (string == null) {
            Log.w(LOG_TAG, "VoiceMailProvider settings for the key \"" + str + "\" were not found. Returning null.");
            return null;
        }
        CallForwardInfo[] callForwardInfoArr = VoicemailProviderSettings.NO_FORWARDING;
        String str2 = str + "#FWDSettings";
        int i = prefs.getInt(str2 + "#Length", 0);
        if (i > 0) {
            callForwardInfoArr = new CallForwardInfo[i];
            for (int i2 = 0; i2 < callForwardInfoArr.length; i2++) {
                String str3 = str2 + "#Setting" + String.valueOf(i2);
                callForwardInfoArr[i2] = new CallForwardInfo();
                callForwardInfoArr[i2].status = prefs.getInt(str3 + "#Status", 0);
                callForwardInfoArr[i2].reason = prefs.getInt(str3 + "#Reason", 5);
                callForwardInfoArr[i2].serviceClass = 1;
                callForwardInfoArr[i2].toa = 145;
                callForwardInfoArr[i2].number = prefs.getString(str3 + "#Number", "");
                callForwardInfoArr[i2].timeSeconds = prefs.getInt(str3 + "#Time", 20);
            }
        }
        VoicemailProviderSettings voicemailProviderSettings = new VoicemailProviderSettings(string, callForwardInfoArr);
        log("Loaded settings for " + str + ": " + voicemailProviderSettings.toString());
        return voicemailProviderSettings;
    }

    public static void save(Context context, String str, VoicemailProviderSettings voicemailProviderSettings) {
        if (voicemailProviderSettings.equals(load(context, str))) {
            log("save: Not saving setting for " + str + " since they have not changed");
            return;
        }
        log("Saving settings for " + str + ": " + voicemailProviderSettings.toString());
        SharedPreferences.Editor editorEdit = getPrefs(context).edit();
        editorEdit.putString(str + "#VMNumber", voicemailProviderSettings.getVoicemailNumber());
        String str2 = str + "#FWDSettings";
        CallForwardInfo[] forwardingSettings = voicemailProviderSettings.getForwardingSettings();
        if (forwardingSettings != VoicemailProviderSettings.NO_FORWARDING) {
            editorEdit.putInt(str2 + "#Length", forwardingSettings.length);
            for (int i = 0; i < forwardingSettings.length; i++) {
                String str3 = str2 + "#Setting" + String.valueOf(i);
                CallForwardInfo callForwardInfo = forwardingSettings[i];
                editorEdit.putInt(str3 + "#Status", callForwardInfo.status);
                editorEdit.putInt(str3 + "#Reason", callForwardInfo.reason);
                editorEdit.putString(str3 + "#Number", callForwardInfo.number);
                editorEdit.putInt(str3 + "#Time", callForwardInfo.timeSeconds);
            }
        } else {
            editorEdit.putInt(str2 + "#Length", 0);
        }
        editorEdit.apply();
    }

    public static void delete(Context context, String str) {
        log("Deleting settings for" + str);
        if (TextUtils.isEmpty(str)) {
            return;
        }
        getPrefs(context).edit().putString(str + "#VMNumber", null).putInt(str + "#FWDSettings#Length", 0).commit();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("vm_numbers", 0);
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
