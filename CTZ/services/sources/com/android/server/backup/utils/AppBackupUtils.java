package com.android.server.backup.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.transport.TransportClient;
import com.android.server.pm.DumpState;

public class AppBackupUtils {
    private static final boolean DEBUG = false;

    public static boolean appIsEligibleForBackup(ApplicationInfo applicationInfo, PackageManager packageManager) {
        if ((applicationInfo.flags & 32768) == 0) {
            return false;
        }
        if ((applicationInfo.uid < 10000 && applicationInfo.backupAgentName == null) || applicationInfo.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE) || applicationInfo.isInstantApp()) {
            return false;
        }
        return !appIsDisabled(applicationInfo, packageManager);
    }

    public static boolean appIsRunningAndEligibleForBackupWithTransport(TransportClient transportClient, String str, PackageManager packageManager) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(str, 134217728);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (!appIsEligibleForBackup(applicationInfo, packageManager) || appIsStopped(applicationInfo) || appIsDisabled(applicationInfo, packageManager)) {
                return false;
            }
            if (transportClient != null) {
                try {
                    return transportClient.connectOrThrow("AppBackupUtils.appIsEligibleForBackupAtRuntime").isAppEligibleForBackup(packageInfo, appGetsFullBackup(packageInfo));
                } catch (Exception e) {
                    Slog.e(BackupManagerService.TAG, "Unable to ask about eligibility: " + e.getMessage());
                    return true;
                }
            }
            return true;
        } catch (PackageManager.NameNotFoundException e2) {
            return false;
        }
    }

    public static boolean appIsDisabled(ApplicationInfo applicationInfo, PackageManager packageManager) {
        switch (packageManager.getApplicationEnabledSetting(applicationInfo.packageName)) {
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    public static boolean appIsStopped(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & DumpState.DUMP_COMPILER_STATS) != 0;
    }

    public static boolean appGetsFullBackup(PackageInfo packageInfo) {
        return packageInfo.applicationInfo.backupAgentName == null || (packageInfo.applicationInfo.flags & 67108864) != 0;
    }

    public static boolean appIsKeyValueOnly(PackageInfo packageInfo) {
        return !appGetsFullBackup(packageInfo);
    }

    public static boolean signaturesMatch(Signature[] signatureArr, PackageInfo packageInfo, PackageManagerInternal packageManagerInternal) {
        boolean z;
        if (packageInfo == null || packageInfo.packageName == null) {
            return false;
        }
        if ((packageInfo.applicationInfo.flags & 1) != 0) {
            return true;
        }
        if (ArrayUtils.isEmpty(signatureArr)) {
            return false;
        }
        SigningInfo signingInfo = packageInfo.signingInfo;
        if (signingInfo == null) {
            Slog.w(BackupManagerService.TAG, "signingInfo is empty, app was either unsigned or the flag PackageManager#GET_SIGNING_CERTIFICATES was not specified");
            return false;
        }
        if (signatureArr.length == 1) {
            return packageManagerInternal.isDataRestoreSafe(signatureArr[0], packageInfo.packageName);
        }
        Signature[] apkContentsSigners = signingInfo.getApkContentsSigners();
        int length = apkContentsSigners.length;
        for (Signature signature : signatureArr) {
            int i = 0;
            while (true) {
                if (i < length) {
                    if (!signature.equals(apkContentsSigners[i])) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                return false;
            }
        }
        return true;
    }
}
