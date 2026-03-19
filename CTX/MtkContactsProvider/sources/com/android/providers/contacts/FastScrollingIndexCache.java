package com.android.providers.contacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
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
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
            try {
                sSingleton = new FastScrollingIndexCache(PreferenceManager.getDefaultSharedPreferences(context));
            } finally {
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            }
        }
        return sSingleton;
    }

    static synchronized FastScrollingIndexCache getInstanceForTest(SharedPreferences sharedPreferences) {
        sSingleton = new FastScrollingIndexCache(sharedPreferences);
        return sSingleton;
    }

    private FastScrollingIndexCache(SharedPreferences sharedPreferences) {
        this.mPrefs = sharedPreferences;
    }

    private static void appendIfNotNull(StringBuilder sb, Object obj) {
        if (obj != null) {
            sb.append(obj.toString());
        }
    }

    private static String buildCacheKey(Uri uri, String str, String[] strArr, String str2, String str3) {
        StringBuilder sb = new StringBuilder();
        appendIfNotNull(sb, uri);
        appendIfNotNull(sb, "\u0001");
        appendIfNotNull(sb, str);
        appendIfNotNull(sb, "\u0001");
        appendIfNotNull(sb, str2);
        appendIfNotNull(sb, "\u0001");
        appendIfNotNull(sb, str3);
        if (strArr != null) {
            for (String str4 : strArr) {
                appendIfNotNull(sb, "\u0001");
                appendIfNotNull(sb, str4);
            }
        }
        return sb.toString();
    }

    static String buildCacheValue(String[] strArr, int[] iArr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) {
                appendIfNotNull(sb, "\u0001");
            }
            appendIfNotNull(sb, strArr[i]);
            appendIfNotNull(sb, "\u0001");
            appendIfNotNull(sb, Integer.toString(iArr[i]));
        }
        return sb.toString();
    }

    public static final Bundle buildExtraBundle(String[] strArr, int[] iArr) {
        Bundle bundle = new Bundle();
        bundle.putStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES", strArr);
        bundle.putIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS", iArr);
        return bundle;
    }

    static Bundle buildExtraBundleFromValue(String str) {
        String[] strArrSplit;
        if (TextUtils.isEmpty(str)) {
            strArrSplit = new String[0];
        } else {
            strArrSplit = SEPARATOR_PATTERN.split(str);
        }
        if (strArrSplit.length % 2 != 0) {
            return null;
        }
        try {
            int length = strArrSplit.length / 2;
            String[] strArr = new String[length];
            int[] iArr = new int[length];
            for (int i = 0; i < length; i++) {
                int i2 = i * 2;
                strArr[i] = strArrSplit[i2];
                iArr[i] = Integer.parseInt(strArrSplit[i2 + 1]);
            }
            return buildExtraBundle(strArr, iArr);
        } catch (RuntimeException e) {
            Log.w(PREFERENCE_KEY, "Failed to parse cached value", e);
            return null;
        }
    }

    public Bundle get(Uri uri, String str, String[] strArr, String str2, String str3) {
        synchronized (this.mCache) {
            ensureLoaded();
            String strBuildCacheKey = buildCacheKey(uri, str, strArr, str2, str3);
            String str4 = this.mCache.get(strBuildCacheKey);
            if (str4 == null) {
                if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                    Log.v(PREFERENCE_KEY, "Miss: " + strBuildCacheKey);
                }
                return null;
            }
            Bundle bundleBuildExtraBundleFromValue = buildExtraBundleFromValue(str4);
            if (bundleBuildExtraBundleFromValue != null) {
                if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                    Log.v(PREFERENCE_KEY, "Hit:  " + strBuildCacheKey);
                }
            } else {
                this.mCache.remove(strBuildCacheKey);
                save();
            }
            return bundleBuildExtraBundleFromValue;
        }
    }

    public void put(Uri uri, String str, String[] strArr, String str2, String str3, Bundle bundle) {
        synchronized (this.mCache) {
            ensureLoaded();
            String strBuildCacheKey = buildCacheKey(uri, str, strArr, str2, str3);
            this.mCache.put(strBuildCacheKey, buildCacheValue(bundle.getStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES"), bundle.getIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS")));
            save();
            if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                Log.v(PREFERENCE_KEY, "Put: " + strBuildCacheKey);
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
        for (String str : this.mCache.keySet()) {
            if (sb.length() > 0) {
                appendIfNotNull(sb, "\u0002");
            }
            appendIfNotNull(sb, str);
            appendIfNotNull(sb, "\u0002");
            appendIfNotNull(sb, this.mCache.get(str));
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
            String string = this.mPrefs.getString(PREFERENCE_KEY, null);
            if (TextUtils.isEmpty(string)) {
                return;
            }
            String[] strArrSplit = SAVE_SEPARATOR_PATTERN.split(string);
            if (strArrSplit.length % 2 != 0) {
                return;
            }
            for (int i = 1; i < strArrSplit.length; i += 2) {
                String str = strArrSplit[i - 1];
                String str2 = strArrSplit[i];
                if (Log.isLoggable(PREFERENCE_KEY, 2)) {
                    Log.v(PREFERENCE_KEY, "Loaded: " + str);
                }
                this.mCache.put(str, str2);
            }
        } catch (RuntimeException e) {
            Log.w(PREFERENCE_KEY, "Failed to load from preferences", e);
        } finally {
            invalidate();
        }
    }
}
