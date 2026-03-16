package com.android.phone.common.util;

import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;

public class SettingsUtil {
    private static final String DEFAULT_NOTIFICATION_URI_STRING = Settings.System.DEFAULT_NOTIFICATION_URI.toString();

    public static void updateRingtoneName(Context context, Handler handler, int type, String key, int msg) {
        Uri ringtoneUri;
        boolean defaultRingtone = false;
        if (type == 1) {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String uriString = prefs.getString(key, DEFAULT_NOTIFICATION_URI_STRING);
            if (TextUtils.isEmpty(uriString)) {
                ringtoneUri = null;
            } else if (uriString.equals(DEFAULT_NOTIFICATION_URI_STRING)) {
                defaultRingtone = true;
                ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
            } else {
                ringtoneUri = Uri.parse(uriString);
            }
        }
        CharSequence summary = context.getString(R.string.imProtocolGoogleTalk);
        if (ringtoneUri == null) {
            summary = context.getString(R.string.imProtocolAim);
        } else {
            try {
                Cursor cursor = context.getContentResolver().query(ringtoneUri, new String[]{"title"}, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        summary = cursor.getString(0);
                    }
                    cursor.close();
                }
            } catch (SQLiteException e) {
            }
        }
        if (defaultRingtone) {
            summary = context.getString(com.android.phone.R.string.default_notification_description, summary);
        }
        handler.sendMessage(handler.obtainMessage(msg, summary));
    }
}
