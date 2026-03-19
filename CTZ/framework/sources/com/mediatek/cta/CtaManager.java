package com.mediatek.cta;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.service.notification.ZenModeConfig;
import java.util.List;

public class CtaManager {
    private static String PLATFORM_PACKAGE_NAME = ZenModeConfig.SYSTEM_AUTHORITY;

    public void createCtaPermsController(Context context) {
    }

    public void linkCtaPermissions(PackageParser.Package r1) {
    }

    public void reportPermRequestUsage(String str, int i) {
    }

    public void shutdown() {
    }

    public void systemReady() {
    }

    public boolean isPermissionReviewRequired(PackageParser.Package r1, int i, boolean z) {
        return false;
    }

    public List<String> getPermRecordPkgs() {
        return null;
    }

    public List<String> getPermRecordPerms(String str) {
        return null;
    }

    public List<Long> getRequestTimes(String str, String str2) {
        return null;
    }

    public boolean showPermErrorDialog(Context context, int i, String str, String str2, String str3) {
        return false;
    }

    public void filterReceiver(Context context, Intent intent, List<ResolveInfo> list, int i) {
    }

    public boolean isCtaSupported() {
        return false;
    }

    public void setCtaSupported(boolean z) {
    }

    public boolean isCtaOnlyPermission(String str) {
        return false;
    }

    public boolean isCtaMonitoredPerms(String str) {
        return false;
    }

    public boolean isPlatformPermissionGroup(String str, String str2) {
        if (str != null && PLATFORM_PACKAGE_NAME.equals(str)) {
            return true;
        }
        return false;
    }

    public String[] getCtaAddedPermissionGroups() {
        return null;
    }

    public boolean enforceCheckPermission(String str, String str2) {
        return false;
    }

    public boolean enforceCheckPermission(String str, String str2, String str3) {
        return false;
    }

    public boolean isPlatformPermission(String str, String str2) {
        if (str != null && PLATFORM_PACKAGE_NAME.equals(str)) {
            return true;
        }
        return false;
    }

    public boolean isSystemApp(Context context, String str) {
        return false;
    }

    public boolean needGrantCtaRuntimePerm(boolean z, int i) {
        return false;
    }

    public String[] getCtaOnlyPermissions() {
        return null;
    }

    public int opToSwitch(int i) {
        return -1;
    }

    public String opToName(int i) {
        return null;
    }

    public int strDebugOpToOp(String str) {
        return -1;
    }

    public String opToPermission(int i) {
        return null;
    }

    public String opToRestriction(int i) {
        return null;
    }

    public int permissionToOpCode(String str) {
        return -1;
    }

    public boolean opAllowSystemBypassRestriction(int i) {
        return false;
    }

    public int opToDefaultMode(int i) {
        return -1;
    }

    public boolean opAllowsReset(int i) {
        return false;
    }

    public String permissionToOp(String str) {
        return null;
    }

    public int strOpToOp(String str) {
        return -1;
    }

    public String getsOpToString(int i) {
        return null;
    }

    public boolean needClearReviewFlagAfterUpgrade(boolean z, String str, String str2) {
        return false;
    }

    public int getOpNum() {
        return 0;
    }
}
