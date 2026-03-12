package com.android.settings.intelligence.suggestions.ranking;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SuggestionEventStore {
    private static final Set<String> EVENTS = new HashSet(Arrays.asList("shown", "dismissed", "clicked"));
    private static final Set<String> METRICS = new HashSet(Arrays.asList("last_event_time", "count"));
    private static SuggestionEventStore sEventStore;
    private final SharedPreferences mSharedPrefs;

    public static SuggestionEventStore get(Context context) {
        if (sEventStore == null) {
            sEventStore = new SuggestionEventStore(context);
        }
        return sEventStore;
    }

    private SuggestionEventStore(Context context) {
        this.mSharedPrefs = context.getSharedPreferences("SuggestionEventStore", 0);
    }

    public void writeEvent(String str, String str2) {
        if (!EVENTS.contains(str2)) {
            Log.w("SuggestionEventStore", "Reported event type " + str2 + " is not a valid type!");
            return;
        }
        String prefKey = getPrefKey(str, str2, "last_event_time");
        String prefKey2 = getPrefKey(str, str2, "count");
        writePref(prefKey, System.currentTimeMillis());
        writePref(prefKey2, readPref(prefKey2, 0L) + 1);
    }

    public long readMetric(String str, String str2, String str3) {
        if (!EVENTS.contains(str2)) {
            Log.w("SuggestionEventStore", "Reported event type " + str2 + " is not a valid event!");
            return 0L;
        }
        if (!METRICS.contains(str3)) {
            Log.w("SuggestionEventStore", "Required stat type + " + str3 + " is not a valid stat!");
            return 0L;
        }
        return readPref(getPrefKey(str, str2, str3), 0L);
    }

    public void clear() {
        this.mSharedPrefs.edit().clear().apply();
    }

    private void writePref(String str, long j) {
        this.mSharedPrefs.edit().putLong(str, j).apply();
    }

    private long readPref(String str, Long l) {
        return this.mSharedPrefs.getLong(str, l.longValue());
    }

    private String getPrefKey(String str, String str2, String str3) {
        return "setting_suggestion_" + str + "_" + str2 + "_" + str3;
    }
}
