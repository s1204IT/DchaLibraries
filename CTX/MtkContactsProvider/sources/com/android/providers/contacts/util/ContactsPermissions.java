package com.android.providers.contacts.util;

import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;

public class ContactsPermissions {
    public static boolean ALLOW_SELF_CALL = true;

    public static boolean hasCallerOrSelfPermission(Context context, String str) {
        return (ALLOW_SELF_CALL && Binder.getCallingPid() == Process.myPid()) || context.checkCallingOrSelfPermission(str) == 0;
    }

    public static void enforceCallingOrSelfPermission(Context context, String str) {
        if (!hasCallerOrSelfPermission(context, str)) {
            throw new SecurityException(String.format("The caller must have the %s permission.", str));
        }
    }

    public static boolean hasPackagePermission(Context context, String str, String str2) {
        return (ALLOW_SELF_CALL && context.getPackageName().equals(str2)) || context.getPackageManager().checkPermission(str, str2) == 0;
    }

    public static boolean hasCallerUriPermission(Context context, Uri uri, int i) {
        return (ALLOW_SELF_CALL && Binder.getCallingPid() == Process.myPid()) || context.checkCallingUriPermission(uri, i) == 0;
    }
}
