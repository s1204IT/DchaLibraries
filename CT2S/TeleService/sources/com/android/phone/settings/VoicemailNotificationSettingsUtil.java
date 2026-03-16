package com.android.phone.settings;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;

public class VoicemailNotificationSettingsUtil {
    public static void setVibrationEnabled(Phone phone, boolean isEnabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getVoicemailVibrationSharedPrefsKey(phone), isEnabled);
        editor.commit();
    }

    public static boolean isVibrationEnabled(Phone phone) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        migrateVoicemailVibrationSettingsIfNeeded(phone, prefs);
        return prefs.getBoolean(getVoicemailVibrationSharedPrefsKey(phone), false);
    }

    public static void setRingtoneUri(Phone phone, Uri ringtoneUri) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        String ringtoneUriStr = ringtoneUri != null ? ringtoneUri.toString() : "";
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getVoicemailRingtoneSharedPrefsKey(phone), ringtoneUriStr);
        editor.commit();
    }

    public static Uri getRingtoneUri(Phone phone) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        migrateVoicemailRingtoneSettingsIfNeeded(phone, prefs);
        String uriString = prefs.getString(getVoicemailRingtoneSharedPrefsKey(phone), Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        if (TextUtils.isEmpty(uriString)) {
            return null;
        }
        return Uri.parse(uriString);
    }

    private static void migrateVoicemailVibrationSettingsIfNeeded(Phone phone, SharedPreferences prefs) {
        String key = getVoicemailVibrationSharedPrefsKey(phone);
        TelephonyManager telephonyManager = TelephonyManager.from(phone.getContext());
        if (!prefs.contains(key) && telephonyManager.getPhoneCount() == 1) {
            if (prefs.contains("button_voicemail_notification_vibrate_key")) {
                boolean voicemailVibrate = prefs.getBoolean("button_voicemail_notification_vibrate_key", false);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(key, voicemailVibrate).remove("button_voicemail_notification_vibrate_when_key").commit();
            }
            if (prefs.contains("button_voicemail_notification_vibrate_when_key")) {
                String vibrateWhen = prefs.getString("button_voicemail_notification_vibrate_when_key", "never");
                boolean voicemailVibrate2 = vibrateWhen.equals("always");
                SharedPreferences.Editor editor2 = prefs.edit();
                editor2.putBoolean(key, voicemailVibrate2).remove("button_voicemail_notification_vibrate_key").commit();
            }
        }
    }

    private static void migrateVoicemailRingtoneSettingsIfNeeded(Phone phone, SharedPreferences prefs) {
        String key = getVoicemailRingtoneSharedPrefsKey(phone);
        TelephonyManager telephonyManager = TelephonyManager.from(phone.getContext());
        if (!prefs.contains(key) && telephonyManager.getPhoneCount() == 1 && prefs.contains("button_voicemail_notification_ringtone_key")) {
            String uriString = prefs.getString("button_voicemail_notification_ringtone_key", null);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key, uriString).remove("button_voicemail_notification_ringtone_key").commit();
        }
    }

    private static String getVoicemailVibrationSharedPrefsKey(Phone phone) {
        return "voicemail_notification_vibrate_" + phone.getSubId();
    }

    public static String getVoicemailRingtoneSharedPrefsKey(Phone phone) {
        return "voicemail_notification_ringtone_" + phone.getSubId();
    }
}
