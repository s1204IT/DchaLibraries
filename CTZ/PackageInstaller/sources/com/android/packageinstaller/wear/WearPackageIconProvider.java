package com.android.packageinstaller.wear;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;

public class WearPackageIconProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        throw new UnsupportedOperationException("Query is not supported.");
    }

    @Override
    public String getType(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI passed in is null.");
        }
        if ("com.google.android.packageinstaller.wear.provider".equals(uri.getEncodedAuthority())) {
            return "vnd.android.cursor.item/cw_package_icon";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("Insert is not supported.");
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        File iconFile;
        if (uri == null) {
            throw new IllegalArgumentException("URI passed in is null.");
        }
        enforcePermissions(uri);
        if ("vnd.android.cursor.item/cw_package_icon".equals(getType(uri)) && (iconFile = WearPackageUtil.getIconFile(getContext().getApplicationContext(), getPackageNameFromUri(uri))) != null) {
            iconFile.delete();
            return 0;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException("Update is not supported.");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        File iconFile;
        if (uri == null) {
            throw new IllegalArgumentException("URI passed in is null.");
        }
        enforcePermissions(uri);
        if ("vnd.android.cursor.item/cw_package_icon".equals(getType(uri)) && (iconFile = WearPackageUtil.getIconFile(getContext().getApplicationContext(), getPackageNameFromUri(uri))) != null) {
            return ParcelFileDescriptor.open(iconFile, 268435456);
        }
        return null;
    }

    private String getPackageNameFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        String str = uri.getPathSegments().get(r3.size() - 1);
        if (str.endsWith(".icon")) {
            return str.substring(0, str.lastIndexOf("."));
        }
        return str;
    }

    @TargetApi(DialogFragment.STYLE_NO_FRAME)
    private void enforcePermissions(Uri uri) {
        Context context = getContext();
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid() || isSystemApp(context, callingPid) || context.checkPermission("com.google.android.permission.INSTALL_WEARABLE_PACKAGES", callingPid, callingUid) == 0 || context.checkUriPermission(uri, callingPid, callingUid, 1) == 0) {
            return;
        }
        throw new SecurityException("Permission Denial: reading " + getClass().getName() + " uri " + uri + " from pid=" + callingPid + ", uid=" + callingUid);
    }

    @TargetApi(DialogFragment.STYLE_NO_INPUT)
    private boolean isSystemApp(Context context, int i) {
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses()) {
            if (runningAppProcessInfo.pid == i) {
                try {
                    PackageInfo packageInfo = context.getPackageManager().getPackageInfo(runningAppProcessInfo.pkgList[0], 0);
                    if (packageInfo != null && packageInfo.applicationInfo != null && (packageInfo.applicationInfo.flags & 1) != 0) {
                        Log.d("WearPackageIconProvider", i + " is a system app.");
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("WearPackageIconProvider", "Could not find package information.", e);
                    return false;
                }
            }
        }
        return false;
    }
}
