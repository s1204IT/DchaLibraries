package com.android.providers.contacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.collect.Maps;
import java.util.Map;
import java.util.regex.Pattern;

public class FastScrollingIndexCache {
    static final String PREFERENCE_KEY = "LetterCountCache";
    private static FastScrollingIndexCache sSingleton;
    private final Map<String, String> mCache = Maps.newHashMap();
    private boolean mPreferenceLoaded;
    private final SharedPreferences mPrefs;
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("\u0001");
    private static final Pattern SAVE_SEPARATOR_PATTERN = Pattern.compile("\u0002");

    public static FastScrollingIndexCache getInstance(Context context) {
        if (sSingleton == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            sSingleton = new FastScrollingIndexCache(prefs);
        }
        return sSingleton;
    }

    static synchronized FastScrollingIndexCache getInstanceForTest(SharedPreferences prefs) {
        sSingleton = new FastScrollingIndexCache(prefs);
        return sSingleton;
    }

    private FastScrollingIndexCache(SharedPreferences prefs) {
        this.mPrefs = prefs;
    }

    private static void appendIfNotNull(StringBuilder sb, Object value) {
        if (value != null) {
            sb.append(value.toString());
        }
    }

    private static String buildCacheKey(Uri queryUri, String selection, String[] selectionArgs, String sortOrder, String countExpression) {
        StringBuilder sb = new StringBuilder();
        appendIfNotNull(sb, queryUri);
        appendIfNotNull(sb, "\u0001");
        appendIfNotNull(sb, selection);
        appendIfNotNull(sb, "\u0001");
        appendIfNotNull(sb, sortOrder);
        appendIfNotNull(sb, "\u0001");
        appendIfNotNull(sb, countExpression);
        if (selectionArgs != null) {
            for (String str : selectionArgs) {
                appendIfNotNull(sb, "\u0001");
                appendIfNotNull(sb, str);
            }
        }
        return sb.toString();
    }

    static String buildCacheValue(String[] titles, int[] counts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < titles.length; i++) {
            if (i > 0) {
                appendIfNotNull(sb, "\u0001");
            }
            appendIfNotNull(sb, titles[i]);
            appendIfNotNull(sb, "\u0001");
            appendIfNotNull(sb, Integer.toString(counts[i]));
        }
        return sb.toString();
    }

    public static final Bundle buildExtraBundle(String[] titles, int[] counts) {
        Bundle bundle = new Bundle();
        bundle.putStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES", titles);
        bundle.putIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS", counts);
        return bundle;
    }

    static Bundle buildExtraBundleFromValue(String value) {
        String[] values;
        if (TextUtils.isEmpty(value)) {
            values = new String[0];
        } else {
            values = SEPARATOR_PATTERN.split(value);
        }
        if (values.length % 2 != 0) {
            return null;
        }
        try {
            int numTitles = values.length / 2;
            String[] titles = new String[numTitles];
            int[] counts = new int[numTitles];
            for (int i = 0; i < numTitles; i++) {
                titles[i] = values[i * 2];
                counts[i] = Integer.parseInt(values[(i * 2) + 1]);
            }
            return buildExtraBundle(titles, counts);
        } catch (RuntimeException e) {
            Log.w(PREFERENCE_KEY, "Failed to parse cached value", e);
            return null;
        }
    }

    public Bundle get(Uri queryUri, String selection, String[] selectionArgs, String sortOrder, String countExpression) {
        Bundle b;
        synchronized (this.mCache) {
            ensureLoaded();
            String key = buildCacheKey(queryUri, selection, selectionArgs, sortOrder, countExpression);
            String value = this.mCache.get(key);
            if (value == null) {
                if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                    Log.v(PREFERENCE_KEY, "Miss: " + key);
                }
                b = null;
            } else {
                b = buildExtraBundleFromValue(value);
                if (b == null) {
                    this.mCache.remove(key);
                    save();
                } else if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                    Log.v(PREFERENCE_KEY, "Hit:  " + key);
                }
            }
        }
        return b;
    }

    public void put(Uri queryUri, String selection, String[] selectionArgs, String sortOrder, String countExpression, Bundle bundle) {
        synchronized (this.mCache) {
            ensureLoaded();
            String key = buildCacheKey(queryUri, selection, selectionArgs, sortOrder, countExpression);
            this.mCache.put(key, buildCacheValue(bundle.getStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES"), bundle.getIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS")));
            save();
            if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                Log.v(PREFERENCE_KEY, "Put: " + key);
            }
        }
    }

    public void invalidate() {
        synchronized (this.mCache) {
            this.mPrefs.edit().remove(PREFERENCE_KEY).commit();
            this.mCache.clear();
            this.mPreferenceLoaded = true;
            if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                Log.v(PREFERENCE_KEY, "Invalidated");
            }
        }
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (String key : this.mCache.keySet()) {
            if (sb.length() > 0) {
                appendIfNotNull(sb, "\u0002");
            }
            appendIfNotNull(sb, key);
            appendIfNotNull(sb, "\u0002");
            appendIfNotNull(sb, this.mCache.get(key));
        }
        this.mPrefs.edit().putString(PREFERENCE_KEY, sb.toString()).apply();
    }

    private void ensureLoaded() {
        if (this.mPreferenceLoaded) {
            return;
        }
        if (Log.isLoggable(PREFERENCE_KEY, 2)) {
            Log.v(PREFERENCE_KEY, "Loading...");
        }
        this.mPreferenceLoaded = true;
        try {
            try {
                String savedValue = this.mPrefs.getString(PREFERENCE_KEY, null);
                if (!TextUtils.isEmpty(savedValue)) {
                    String[] keysAndValues = SAVE_SEPARATOR_PATTERN.split(savedValue);
                    if (keysAndValues.length % 2 != 0) {
                        if (0 == 0) {
                            invalidate();
                            return;
                        }
                        return;
                    }
                    for (int i = 1; i < keysAndValues.length; i += 2) {
                        String key = keysAndValues[i - 1];
                        String value = keysAndValues[i];
                        if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                            Log.v(PREFERENCE_KEY, "Loaded: " + key);
                        }
                        this.mCache.put(key, value);
                    }
                }
                if (1 == 0) {
                    invalidate();
                }
            } catch (RuntimeException e) {
                Log.w(PREFERENCE_KEY, "Failed to load from preferences", e);
                if (0 == 0) {
                    invalidate();
                }
            }
        } catch (Throwable th) {
            if (0 == 0) {
                invalidate();
            }
            throw th;
        }
    }
}
