package com.mediatek.gallerybasic.base;

import android.content.Intent;
import com.mediatek.gallerybasic.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public class MediaFilter {
    public static final int INVALID_BUCKET_ID = -1;
    private static final String TAG = "MtkGallery2/MediaFilter";
    private static int sCurrentFlag = 1;
    private static ArrayList<IFilter> sFilterArray = new ArrayList<>();
    private int mFlag;

    public static void registerFilter(IFilter iFilter) {
        sFilterArray.add(iFilter);
        Log.d(TAG, "<registerFilter> filter = " + iFilter);
    }

    public static synchronized int requestFlagBit() {
        int i;
        i = sCurrentFlag;
        sCurrentFlag <<= 1;
        return i;
    }

    public MediaFilter() {
        Iterator<IFilter> it = sFilterArray.iterator();
        while (it.hasNext()) {
            it.next().setDefaultFlag(this);
        }
    }

    public int getFlag() {
        return this.mFlag;
    }

    public int hashCode() {
        return this.mFlag;
    }

    public boolean equals(MediaFilter mediaFilter) {
        return mediaFilter != null && this.mFlag == mediaFilter.getFlag();
    }

    public void setFlagEnable(int i) {
        this.mFlag = i | this.mFlag;
    }

    public void setFlagDisable(int i) {
        this.mFlag = (~i) & this.mFlag;
    }

    public void setFlagFromIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Iterator<IFilter> it = sFilterArray.iterator();
        while (it.hasNext()) {
            it.next().setFlagFromIntent(intent, this);
        }
    }

    public String getExtWhereClauseForImage(String str, int i) {
        Iterator<IFilter> it = sFilterArray.iterator();
        while (it.hasNext()) {
            str = AND(str, it.next().getWhereClauseForImage(this.mFlag, i));
        }
        return str;
    }

    public String getExtWhereClauseForVideo(String str, int i) {
        Iterator<IFilter> it = sFilterArray.iterator();
        while (it.hasNext()) {
            str = AND(str, it.next().getWhereClauseForVideo(this.mFlag, i));
        }
        return str;
    }

    public String getExtWhereClause(String str, int i) {
        Iterator<IFilter> it = sFilterArray.iterator();
        while (it.hasNext()) {
            str = AND(str, it.next().getWhereClause(this.mFlag, i));
        }
        return str;
    }

    public String getExtDeleteWhereClauseForImage(String str, int i) {
        Iterator<IFilter> it = sFilterArray.iterator();
        while (it.hasNext()) {
            str = AND(str, it.next().getDeleteWhereClauseForImage(this.mFlag, i));
        }
        return str;
    }

    public String getExtDeleteWhereClauseForVideo(String str, int i) {
        Iterator<IFilter> it = sFilterArray.iterator();
        while (it.hasNext()) {
            str = AND(str, it.next().getDeleteWhereClauseForVideo(this.mFlag, i));
        }
        return str;
    }

    public static String AND(String str, String str2) {
        if ((str == null || str.equals("")) && (str2 == null || str2.equals(""))) {
            return "";
        }
        if (str == null || str.equals("")) {
            return str2;
        }
        if (str2 == null || str2.equals("")) {
            return str;
        }
        return "(" + str + ") AND (" + str2 + ")";
    }

    public static String OR(String str, String str2) {
        if ((str == null || str.equals("")) && (str2 == null || str2.equals(""))) {
            return "";
        }
        if (str == null || str.equals("")) {
            return str2;
        }
        if (str2 == null || str2.equals("")) {
            return str;
        }
        return "(" + str + ") OR (" + str2 + ")";
    }
}
