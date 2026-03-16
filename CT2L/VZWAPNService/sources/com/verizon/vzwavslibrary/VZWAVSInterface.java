package com.verizon.vzwavslibrary;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class VZWAVSInterface {
    private static final Uri CONTENT_URI = Uri.parse("content://com.verizon.vzwavs.provider/apis");

    public static boolean isPackageAuthorized(Context context, String packageName, String api) {
        boolean result = false;
        if (api != null && context != null && packageName != null) {
            api.toUpperCase();
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(CONTENT_URI, null, packageName, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String apis = cursor.getString(0);
                result = apis.contains(api);
            }
            return result;
        }
        return false;
    }
}
