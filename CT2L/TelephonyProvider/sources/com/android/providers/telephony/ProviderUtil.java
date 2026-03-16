package com.android.providers.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

public class ProviderUtil {
    public static String getPackageNamesByUid(Context context, int uid) {
        PackageManager pm = context.getPackageManager();
        String[] packageNames = pm.getPackagesForUid(uid);
        if (packageNames == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String name : packageNames) {
            if (!TextUtils.isEmpty(name)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(name);
            }
        }
        return sb.toString();
    }

    public static boolean shouldSetCreator(ContentValues values, int uid) {
        return ((uid == 1000 || uid == 1001) && (values.containsKey("creator") || values.containsKey("creator"))) ? false : true;
    }

    public static boolean shouldRemoveCreator(ContentValues values, int uid) {
        return (uid == 1000 || uid == 1001 || (!values.containsKey("creator") && !values.containsKey("creator"))) ? false : true;
    }
}
