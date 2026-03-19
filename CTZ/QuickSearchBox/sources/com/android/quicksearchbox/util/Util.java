package com.android.quicksearchbox.util;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

public class Util {
    public static Uri getResourceUri(Context context, int i) {
        try {
            return getResourceUri(context.getResources(), context.getPackageName(), i);
        } catch (Resources.NotFoundException e) {
            Log.e("QSB.Util", "Resource not found: " + i + " in " + context.getPackageName());
            return null;
        }
    }

    private static Uri getResourceUri(Resources resources, String str, int i) throws Resources.NotFoundException {
        return makeResourceUri(str, resources.getResourcePackageName(i), resources.getResourceTypeName(i), resources.getResourceEntryName(i));
    }

    private static Uri makeResourceUri(String str, String str2, String str3, String str4) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("android.resource");
        builder.encodedAuthority(str);
        builder.appendEncodedPath(str3);
        if (!str.equals(str2)) {
            builder.appendEncodedPath(str2 + ":" + str4);
        } else {
            builder.appendEncodedPath(str4);
        }
        return builder.build();
    }
}
