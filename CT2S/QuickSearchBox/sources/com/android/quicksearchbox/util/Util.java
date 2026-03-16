package com.android.quicksearchbox.util;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

public class Util {
    public static Uri getResourceUri(Context packageContext, int res) {
        try {
            Resources resources = packageContext.getResources();
            return getResourceUri(resources, packageContext.getPackageName(), res);
        } catch (Resources.NotFoundException e) {
            Log.e("QSB.Util", "Resource not found: " + res + " in " + packageContext.getPackageName());
            return null;
        }
    }

    private static Uri getResourceUri(Resources resources, String appPkg, int res) throws Resources.NotFoundException {
        String resPkg = resources.getResourcePackageName(res);
        String type = resources.getResourceTypeName(res);
        String name = resources.getResourceEntryName(res);
        return makeResourceUri(appPkg, resPkg, type, name);
    }

    private static Uri makeResourceUri(String appPkg, String resPkg, String type, String name) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("android.resource");
        uriBuilder.encodedAuthority(appPkg);
        uriBuilder.appendEncodedPath(type);
        if (!appPkg.equals(resPkg)) {
            uriBuilder.appendEncodedPath(resPkg + ":" + name);
        } else {
            uriBuilder.appendEncodedPath(name);
        }
        return uriBuilder.build();
    }
}
