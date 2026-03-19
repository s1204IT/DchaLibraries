package com.android.contacts.compat;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;
import com.mediatek.contacts.util.Log;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mediatek.telephony.MtkTelephony;

public class TelephonyThreadsCompat {
    private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");
    private static final String[] ID_PROJECTION = {"_id"};
    private static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    public static long getOrCreateThreadId(Context context, String str) {
        if (SdkVersionOverride.getSdkVersion(23) >= 23) {
            return MtkTelephony.MtkThreads.getThreadId(context, str);
        }
        return getOrCreateThreadIdInternal(context, str);
    }

    private static long getOrCreateThreadIdInternal(Context context, String str) {
        HashSet hashSet = new HashSet();
        hashSet.add(str);
        return getOrCreateThreadIdInternal(context, hashSet);
    }

    private static long getOrCreateThreadIdInternal(Context context, Set<String> set) {
        Uri.Builder builderBuildUpon = THREAD_ID_CONTENT_URI.buildUpon();
        for (String strExtractAddrSpec : set) {
            if (isEmailAddress(strExtractAddrSpec)) {
                strExtractAddrSpec = extractAddrSpec(strExtractAddrSpec);
            }
            builderBuildUpon.appendQueryParameter("recipient", strExtractAddrSpec);
        }
        Uri uriBuild = builderBuildUpon.build();
        Cursor cursorQuery = query(context.getContentResolver(), uriBuild, ID_PROJECTION, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getLong(0);
                }
                Log.e("TelephonyThreadsCompat", "getOrCreateThreadId returned no rows!");
            } finally {
                cursorQuery.close();
            }
        }
        Log.e("TelephonyThreadsCompat", "getOrCreateThreadId failed with uri " + uriBuild.toString());
        throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
    }

    private static Cursor query(ContentResolver contentResolver, Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        try {
            return contentResolver.query(uri, strArr, str, strArr2, str2);
        } catch (Exception e) {
            Log.e("TelephonyThreadsCompat", "Catch an exception when query: ", e);
            return null;
        }
    }

    private static boolean isEmailAddress(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(extractAddrSpec(str)).matches();
    }

    private static String extractAddrSpec(String str) {
        Matcher matcher = NAME_ADDR_EMAIL_PATTERN.matcher(str);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return str;
    }
}
