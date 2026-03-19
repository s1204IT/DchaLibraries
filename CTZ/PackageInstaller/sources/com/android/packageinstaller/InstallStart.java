package com.android.packageinstaller;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

public class InstallStart extends Activity {
    private static final String LOG_TAG = InstallStart.class.getSimpleName();
    private boolean mAbortInstall = false;
    private IActivityManager mIActivityManager;
    private IPackageManager mIPackageManager;

    @Override
    protected void onCreate(Bundle bundle) {
        int intExtra;
        boolean booleanExtra;
        super.onCreate(bundle);
        this.mIPackageManager = AppGlobals.getPackageManager();
        Intent intent = getIntent();
        String callingPackage = getCallingPackage();
        boolean zEquals = "android.content.pm.action.CONFIRM_PERMISSIONS".equals(intent.getAction());
        if (zEquals) {
            intExtra = intent.getIntExtra("android.content.pm.extra.SESSION_ID", -1);
        } else {
            intExtra = -1;
        }
        if (callingPackage == null && intExtra != -1) {
            PackageInstaller.SessionInfo sessionInfo = getPackageManager().getPackageInstaller().getSessionInfo(intExtra);
            callingPackage = sessionInfo != null ? sessionInfo.getInstallerPackageName() : null;
        }
        ApplicationInfo sourceInfo = getSourceInfo(callingPackage);
        int originatingUid = getOriginatingUid(sourceInfo);
        if (sourceInfo != null && (sourceInfo.privateFlags & 8) != 0) {
            booleanExtra = intent.getBooleanExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", false);
        } else {
            booleanExtra = false;
        }
        if (!booleanExtra && originatingUid != -1) {
            int maxTargetSdkVersionForUid = PackageUtil.getMaxTargetSdkVersionForUid(this, originatingUid);
            if (maxTargetSdkVersionForUid < 0) {
                Log.w(LOG_TAG, "Cannot get target sdk version for uid " + originatingUid);
                this.mAbortInstall = true;
            } else if (maxTargetSdkVersionForUid >= 26 && !declaresAppOpPermission(originatingUid, "android.permission.REQUEST_INSTALL_PACKAGES")) {
                Log.e(LOG_TAG, "Requesting uid " + originatingUid + " needs to declare permission android.permission.REQUEST_INSTALL_PACKAGES");
                this.mAbortInstall = true;
            }
        }
        if (this.mAbortInstall) {
            setResult(0);
            finish();
            return;
        }
        Intent intent2 = new Intent(intent);
        intent2.setFlags(33554432);
        intent2.putExtra("EXTRA_CALLING_PACKAGE", callingPackage);
        intent2.putExtra("EXTRA_ORIGINAL_SOURCE_INFO", sourceInfo);
        intent2.putExtra("android.intent.extra.ORIGINATING_UID", originatingUid);
        if (zEquals) {
            intent2.setClass(this, PackageInstallerActivity.class);
        } else {
            Uri data = intent.getData();
            if (data != null && (data.getScheme().equals("file") || data.getScheme().equals("content"))) {
                intent2.setClass(this, InstallStaging.class);
            } else if (data != null && data.getScheme().equals("package")) {
                intent2.setClass(this, PackageInstallerActivity.class);
            } else {
                Intent intent3 = new Intent();
                intent3.putExtra("android.intent.extra.INSTALL_RESULT", -3);
                setResult(1, intent3);
                intent2 = null;
            }
        }
        if (intent2 != null) {
            startActivity(intent2);
        }
        finish();
    }

    private boolean declaresAppOpPermission(int i, String str) {
        try {
            String[] appOpPermissionPackages = this.mIPackageManager.getAppOpPermissionPackages(str);
            if (appOpPermissionPackages == null) {
                return false;
            }
            for (String str2 : appOpPermissionPackages) {
                if (i == getPackageManager().getPackageUid(str2, 0)) {
                    return true;
                }
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    private ApplicationInfo getSourceInfo(String str) {
        if (str != null) {
            try {
                return getPackageManager().getApplicationInfo(str, 0);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private int getOriginatingUid(ApplicationInfo applicationInfo) {
        int launchedFromUid;
        int intExtra = getIntent().getIntExtra("android.intent.extra.ORIGINATING_UID", -1);
        if (applicationInfo != null) {
            launchedFromUid = applicationInfo.uid;
        } else {
            try {
                launchedFromUid = getIActivityManager().getLaunchedFromUid(getActivityToken());
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Could not determine the launching uid.");
                this.mAbortInstall = true;
                return -1;
            }
        }
        try {
            if (this.mIPackageManager.checkUidPermission("android.permission.MANAGE_DOCUMENTS", launchedFromUid) == 0) {
                return intExtra;
            }
        } catch (RemoteException e2) {
        }
        if (isSystemDownloadsProvider(launchedFromUid)) {
            return intExtra;
        }
        return launchedFromUid;
    }

    private boolean isSystemDownloadsProvider(int i) {
        ProviderInfo providerInfoResolveContentProvider = getPackageManager().resolveContentProvider("downloads", 0);
        if (providerInfoResolveContentProvider == null) {
            return false;
        }
        ApplicationInfo applicationInfo = providerInfoResolveContentProvider.applicationInfo;
        return applicationInfo.isSystemApp() && i == applicationInfo.uid;
    }

    private IActivityManager getIActivityManager() {
        if (this.mIActivityManager == null) {
            return ActivityManager.getService();
        }
        return this.mIActivityManager;
    }

    @VisibleForTesting
    void injectIActivityManager(IActivityManager iActivityManager) {
        this.mIActivityManager = iActivityManager;
    }
}
