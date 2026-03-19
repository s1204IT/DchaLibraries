package com.mediatek.gallerybasic.base;

import android.app.Activity;
import android.app.Service;
import com.mediatek.gallerybasic.util.Log;
import java.util.HashMap;
import java.util.Map;

public class MediaFilterSetting {
    static final boolean $assertionsDisabled = false;
    private static final String TAG = "MtkGallery2/MediaFilterSetting";
    private static String sCurrentActivity;
    private static MediaFilter sCurrentFilter;
    private static HashMap<String, MediaFilter> sFilterMap = new HashMap<>();

    public static boolean setCurrentFilter(Activity activity, MediaFilter mediaFilter) {
        Log.d(TAG, "<setCurrentFilter> activity = " + activity);
        return setCurrentFilter(activity.toString(), mediaFilter);
    }

    public static void removeFilter(Activity activity) {
        Log.d(TAG, "<removeFilter> activity = " + activity);
        sFilterMap.remove(activity.toString());
        logAllFilter();
    }

    public static boolean restoreFilter(Activity activity) {
        Log.d(TAG, "<restoreFilter> activity = " + activity);
        return restoreFilter(activity.toString());
    }

    public static void setCurrentFilter(Service service, MediaFilter mediaFilter) {
        Log.d(TAG, "<setCurrentFilter> service = " + service);
        setCurrentFilter(service.toString(), mediaFilter);
    }

    public static void removeFilter(Service service) {
        Log.d(TAG, "<removeFilter> service = " + service);
        sFilterMap.remove(service.toString());
        logAllFilter();
    }

    public static boolean restoreFilter(Service service) {
        Log.d(TAG, "<restoreFilter> service = " + service);
        return restoreFilter(service.toString());
    }

    public static synchronized MediaFilter getCurrentFilter() {
        return sCurrentFilter;
    }

    private static synchronized boolean setCurrentFilter(String str, MediaFilter mediaFilter) {
        boolean z;
        z = sCurrentFilter == null || sCurrentFilter.equals(mediaFilter);
        sFilterMap.put(str, mediaFilter);
        sCurrentActivity = str;
        sCurrentFilter = mediaFilter;
        logAllFilter();
        return z;
    }

    private static synchronized boolean restoreFilter(String str) {
        MediaFilter mediaFilter = sCurrentFilter;
        boolean z = false;
        if (sFilterMap.containsKey(str)) {
            sCurrentActivity = str;
            sCurrentFilter = sFilterMap.get(str);
            logAllFilter();
            if (sCurrentFilter != null && sCurrentFilter.equals(mediaFilter)) {
                z = true;
            }
            return z;
        }
        Log.d(TAG, "<restoreFilter> Cannot find filter of this activity, return false");
        logAllFilter();
        return false;
    }

    public static synchronized String getExtWhereClauseForImage(String str) {
        return sCurrentFilter.getExtWhereClauseForImage(str, -1);
    }

    public static synchronized String getExtWhereClauseForVideo(String str) {
        return sCurrentFilter.getExtWhereClauseForVideo(str, -1);
    }

    public static synchronized String getExtWhereClauseForImage(String str, int i) {
        return sCurrentFilter.getExtWhereClauseForImage(str, i);
    }

    public static synchronized String getExtWhereClauseForVideo(String str, int i) {
        return sCurrentFilter.getExtWhereClauseForVideo(str, i);
    }

    public static synchronized String getExtWhereClause(String str) {
        return sCurrentFilter.getExtWhereClause(str, -1);
    }

    public static synchronized String getExtDeleteWhereClauseForImage(String str, int i) {
        return sCurrentFilter.getExtDeleteWhereClauseForImage(str, i);
    }

    public static synchronized String getExtDeleteWhereClauseForVideo(String str, int i) {
        return sCurrentFilter.getExtDeleteWhereClauseForVideo(str, i);
    }

    public static synchronized String getExtWhereClause(String str, int i) {
        return sCurrentFilter.getExtWhereClause(str, i);
    }

    private static void logAllFilter() {
        int i;
        Log.d(TAG, "<logAllFilter> begin ----------------------------");
        int i2 = 1;
        for (Map.Entry<String, MediaFilter> entry : sFilterMap.entrySet()) {
            if (entry.getValue() == sCurrentFilter) {
                StringBuilder sb = new StringBuilder();
                sb.append("<logAllFilter> ");
                i = i2 + 1;
                sb.append(i2);
                sb.append(". [");
                sb.append(entry.getKey());
                sb.append("], ");
                sb.append(entry.getValue());
                sb.append(", >>> This is current !!");
                Log.d(TAG, sb.toString());
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("<logAllFilter> ");
                i = i2 + 1;
                sb2.append(i2);
                sb2.append(". [");
                sb2.append(entry.getKey());
                sb2.append("], ");
                sb2.append(entry.getValue());
                Log.d(TAG, sb2.toString());
            }
            i2 = i;
        }
        Log.d(TAG, "<logAllFilter> end ------------------------------");
    }
}
