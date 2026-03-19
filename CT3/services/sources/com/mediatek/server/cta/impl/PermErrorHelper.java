package com.mediatek.server.cta.impl;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.text.TextUtils;
import android.util.Slog;
import com.mediatek.cta.CtaUtils;
import com.mediatek.server.cta.CtaPermsController;

public class PermErrorHelper {
    private static final String PATTERN_OR = " or ";
    private static final String PATTERN_PERMISSION_DENIAL = "Permission Denial";
    private static final String PATTERN_REQUIRES = " requires ";
    private static final String PATTERN_SECURITY_EXCEPTION = "SecurityException";
    private static final String TAG = "PermErrorHelper";
    private static PermErrorHelper sInstance;
    private Context mContext;

    public static PermErrorHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PermErrorHelper(context);
        }
        return sInstance;
    }

    private PermErrorHelper(Context context) {
        this.mContext = context;
    }

    public String parsePermName(int i, String str, String str2) {
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "parsePermName uid = " + i + ", packageName = " + str + ", exceptionMsg = " + str2);
        }
        if (!CtaUtils.isCtaSupported() || TextUtils.isEmpty(str2) || !str2.contains(PATTERN_SECURITY_EXCEPTION) || !str2.contains(PATTERN_PERMISSION_DENIAL)) {
            return null;
        }
        String strSubstring = str2.substring(str2.indexOf(PATTERN_REQUIRES) + PATTERN_REQUIRES.length(), str2.length());
        if (strSubstring.contains(PATTERN_OR)) {
            strSubstring = strSubstring.substring(strSubstring.indexOf(PATTERN_OR) + PATTERN_OR.length(), strSubstring.length());
        }
        String strTrim = strSubstring.trim();
        if (CtaPermsController.DEBUG) {
            Slog.d(TAG, "initMtkPermErrorDialog() parseResult = " + strTrim);
        }
        try {
            PermissionInfo permissionInfo = this.mContext.getPackageManager().getPermissionInfo(strTrim, 0);
            if (permissionInfo.protectionLevel == 1 && CtaUtils.isPlatformPermission(permissionInfo.packageName, permissionInfo.name)) {
                return permissionInfo.name;
            }
            if (permissionInfo.protectionLevel == 18 && CtaUtils.isCtaOnlyPermission(permissionInfo.name)) {
                return permissionInfo.name;
            }
            return "";
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
