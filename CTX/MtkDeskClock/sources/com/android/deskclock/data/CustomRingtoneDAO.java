package com.android.deskclock.data;

import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CustomRingtoneDAO {
    private static final String NEXT_RINGTONE_ID = "next_ringtone_id";
    private static final String RINGTONE_IDS = "ringtone_ids";
    private static final String RINGTONE_TITLE = "ringtone_title_";
    private static final String RINGTONE_URI = "ringtone_uri_";

    private CustomRingtoneDAO() {
    }

    static CustomRingtone addCustomRingtone(SharedPreferences sharedPreferences, Uri uri, String str) {
        long j = sharedPreferences.getLong(NEXT_RINGTONE_ID, 0L);
        Set<String> ringtoneIds = getRingtoneIds(sharedPreferences);
        ringtoneIds.add(String.valueOf(j));
        sharedPreferences.edit().putString(RINGTONE_URI + j, uri.toString()).putString(RINGTONE_TITLE + j, str).putLong(NEXT_RINGTONE_ID, 1 + j).putStringSet(RINGTONE_IDS, ringtoneIds).apply();
        return new CustomRingtone(j, uri, str, true);
    }

    static void removeCustomRingtone(SharedPreferences sharedPreferences, long j) {
        Set<String> ringtoneIds = getRingtoneIds(sharedPreferences);
        ringtoneIds.remove(String.valueOf(j));
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.remove(RINGTONE_URI + j);
        editorEdit.remove(RINGTONE_TITLE + j);
        if (ringtoneIds.isEmpty()) {
            editorEdit.remove(RINGTONE_IDS);
            editorEdit.remove(NEXT_RINGTONE_ID);
        } else {
            editorEdit.putStringSet(RINGTONE_IDS, ringtoneIds);
        }
        editorEdit.apply();
    }

    static List<CustomRingtone> getCustomRingtones(SharedPreferences sharedPreferences) {
        Set<String> stringSet = sharedPreferences.getStringSet(RINGTONE_IDS, Collections.emptySet());
        ArrayList arrayList = new ArrayList(stringSet.size());
        for (String str : stringSet) {
            arrayList.add(new CustomRingtone(Long.parseLong(str), Uri.parse(sharedPreferences.getString(RINGTONE_URI + str, null)), sharedPreferences.getString(RINGTONE_TITLE + str, null), true));
        }
        return arrayList;
    }

    private static Set<String> getRingtoneIds(SharedPreferences sharedPreferences) {
        return new HashSet(sharedPreferences.getStringSet(RINGTONE_IDS, Collections.emptySet()));
    }
}
