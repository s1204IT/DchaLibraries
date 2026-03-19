package com.android.server.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Slog;
import android.webkit.UserPackage;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewProviderResponse;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class WebViewUpdater {
    private static final String TAG = WebViewUpdater.class.getSimpleName();
    private static final int VALIDITY_INCORRECT_SDK_VERSION = 1;
    private static final int VALIDITY_INCORRECT_SIGNATURE = 3;
    private static final int VALIDITY_INCORRECT_VERSION_CODE = 2;
    private static final int VALIDITY_NO_LIBRARY_FLAG = 4;
    private static final int VALIDITY_OK = 0;
    private static final int WAIT_TIMEOUT_MS = 1000;
    private Context mContext;
    private SystemInterface mSystemInterface;
    private long mMinimumVersionCode = -1;
    private int mNumRelroCreationsStarted = 0;
    private int mNumRelroCreationsFinished = 0;
    private boolean mWebViewPackageDirty = false;
    private boolean mAnyWebViewInstalled = false;
    private int NUMBER_OF_RELROS_UNKNOWN = Integer.MAX_VALUE;
    private PackageInfo mCurrentWebViewPackage = null;
    private final Object mLock = new Object();

    private static class WebViewPackageMissingException extends Exception {
        public WebViewPackageMissingException(String str) {
            super(str);
        }

        public WebViewPackageMissingException(Exception exc) {
            super(exc);
        }
    }

    WebViewUpdater(Context context, SystemInterface systemInterface) {
        this.mContext = context;
        this.mSystemInterface = systemInterface;
    }

    void packageStateChanged(String str, int i) {
        String str2;
        boolean zEquals;
        boolean z;
        boolean z2 = false;
        for (WebViewProviderInfo webViewProviderInfo : this.mSystemInterface.getWebViewPackages()) {
            if (webViewProviderInfo.packageName.equals(str)) {
                synchronized (this.mLock) {
                    try {
                        PackageInfo packageInfoFindPreferredWebViewPackage = findPreferredWebViewPackage();
                        if (this.mCurrentWebViewPackage != null) {
                            str2 = this.mCurrentWebViewPackage.packageName;
                            if (i == 0) {
                                try {
                                    if (packageInfoFindPreferredWebViewPackage.packageName.equals(str2)) {
                                        return;
                                    }
                                } catch (WebViewPackageMissingException e) {
                                    e = e;
                                    zEquals = false;
                                    this.mCurrentWebViewPackage = null;
                                    Slog.e(TAG, "Could not find valid WebView package to create relro with " + e);
                                    z = z2;
                                    if (!z) {
                                        return;
                                    } else {
                                        return;
                                    }
                                }
                            }
                            if (packageInfoFindPreferredWebViewPackage.packageName.equals(str2) && packageInfoFindPreferredWebViewPackage.lastUpdateTime == this.mCurrentWebViewPackage.lastUpdateTime) {
                                return;
                            }
                        } else {
                            str2 = null;
                        }
                        if (webViewProviderInfo.packageName.equals(packageInfoFindPreferredWebViewPackage.packageName) || webViewProviderInfo.packageName.equals(str2)) {
                            z = true;
                            try {
                                zEquals = webViewProviderInfo.packageName.equals(str2);
                                if (z) {
                                    try {
                                        onWebViewProviderChanged(packageInfoFindPreferredWebViewPackage);
                                    } catch (WebViewPackageMissingException e2) {
                                        e = e2;
                                        z2 = z;
                                        e = e;
                                        this.mCurrentWebViewPackage = null;
                                        Slog.e(TAG, "Could not find valid WebView package to create relro with " + e);
                                        z = z2;
                                    }
                                }
                            } catch (WebViewPackageMissingException e3) {
                                e = e3;
                                zEquals = false;
                            }
                        } else {
                            if (this.mCurrentWebViewPackage != null) {
                                z = false;
                            }
                            zEquals = webViewProviderInfo.packageName.equals(str2);
                            if (z) {
                            }
                        }
                    } catch (WebViewPackageMissingException e4) {
                        e = e4;
                        str2 = null;
                    }
                    if (!z && !zEquals && str2 != null) {
                        this.mSystemInterface.killPackageDependents(str2);
                        return;
                    }
                    return;
                }
            }
        }
    }

    void prepareWebViewInSystemServer() {
        try {
            synchronized (this.mLock) {
                this.mCurrentWebViewPackage = findPreferredWebViewPackage();
                this.mSystemInterface.updateUserSetting(this.mContext, this.mCurrentWebViewPackage.packageName);
                onWebViewProviderChanged(this.mCurrentWebViewPackage);
            }
        } catch (Throwable th) {
            Slog.e(TAG, "error preparing webview provider from system server", th);
        }
    }

    String changeProviderAndSetting(String str) {
        PackageInfo packageInfoUpdateCurrentWebViewPackage = updateCurrentWebViewPackage(str);
        return packageInfoUpdateCurrentWebViewPackage == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : packageInfoUpdateCurrentWebViewPackage.packageName;
    }

    PackageInfo updateCurrentWebViewPackage(String str) {
        PackageInfo packageInfo;
        PackageInfo packageInfoFindPreferredWebViewPackage;
        boolean z;
        synchronized (this.mLock) {
            packageInfo = this.mCurrentWebViewPackage;
            if (str != null) {
                this.mSystemInterface.updateUserSetting(this.mContext, str);
            }
            try {
                packageInfoFindPreferredWebViewPackage = findPreferredWebViewPackage();
                if (packageInfo != null) {
                    z = !packageInfoFindPreferredWebViewPackage.packageName.equals(packageInfo.packageName);
                }
                if (z) {
                    onWebViewProviderChanged(packageInfoFindPreferredWebViewPackage);
                }
            } catch (WebViewPackageMissingException e) {
                this.mCurrentWebViewPackage = null;
                Slog.e(TAG, "Couldn't find WebView package to use " + e);
                return null;
            }
        }
        if (z && packageInfo != null) {
            this.mSystemInterface.killPackageDependents(packageInfo.packageName);
        }
        return packageInfoFindPreferredWebViewPackage;
    }

    private void onWebViewProviderChanged(PackageInfo packageInfo) {
        synchronized (this.mLock) {
            this.mAnyWebViewInstalled = true;
            if (this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished) {
                this.mCurrentWebViewPackage = packageInfo;
                this.mNumRelroCreationsStarted = this.NUMBER_OF_RELROS_UNKNOWN;
                this.mNumRelroCreationsFinished = 0;
                this.mNumRelroCreationsStarted = this.mSystemInterface.onWebViewProviderChanged(packageInfo);
                checkIfRelrosDoneLocked();
            } else {
                this.mWebViewPackageDirty = true;
            }
        }
    }

    WebViewProviderInfo[] getValidWebViewPackages() {
        ProviderAndPackageInfo[] validWebViewPackagesAndInfos = getValidWebViewPackagesAndInfos();
        WebViewProviderInfo[] webViewProviderInfoArr = new WebViewProviderInfo[validWebViewPackagesAndInfos.length];
        for (int i = 0; i < validWebViewPackagesAndInfos.length; i++) {
            webViewProviderInfoArr[i] = validWebViewPackagesAndInfos[i].provider;
        }
        return webViewProviderInfoArr;
    }

    private static class ProviderAndPackageInfo {
        public final PackageInfo packageInfo;
        public final WebViewProviderInfo provider;

        public ProviderAndPackageInfo(WebViewProviderInfo webViewProviderInfo, PackageInfo packageInfo) {
            this.provider = webViewProviderInfo;
            this.packageInfo = packageInfo;
        }
    }

    private ProviderAndPackageInfo[] getValidWebViewPackagesAndInfos() {
        WebViewProviderInfo[] webViewPackages = this.mSystemInterface.getWebViewPackages();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < webViewPackages.length; i++) {
            try {
                PackageInfo packageInfoForProvider = this.mSystemInterface.getPackageInfoForProvider(webViewPackages[i]);
                if (isValidProvider(webViewPackages[i], packageInfoForProvider)) {
                    arrayList.add(new ProviderAndPackageInfo(webViewPackages[i], packageInfoForProvider));
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return (ProviderAndPackageInfo[]) arrayList.toArray(new ProviderAndPackageInfo[arrayList.size()]);
    }

    private PackageInfo findPreferredWebViewPackage() throws WebViewPackageMissingException {
        ProviderAndPackageInfo[] validWebViewPackagesAndInfos = getValidWebViewPackagesAndInfos();
        String userChosenWebViewProvider = this.mSystemInterface.getUserChosenWebViewProvider(this.mContext);
        for (ProviderAndPackageInfo providerAndPackageInfo : validWebViewPackagesAndInfos) {
            if (providerAndPackageInfo.provider.packageName.equals(userChosenWebViewProvider) && isInstalledAndEnabledForAllUsers(this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, providerAndPackageInfo.provider))) {
                return providerAndPackageInfo.packageInfo;
            }
        }
        for (ProviderAndPackageInfo providerAndPackageInfo2 : validWebViewPackagesAndInfos) {
            if (providerAndPackageInfo2.provider.availableByDefault && isInstalledAndEnabledForAllUsers(this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, providerAndPackageInfo2.provider))) {
                return providerAndPackageInfo2.packageInfo;
            }
        }
        this.mAnyWebViewInstalled = false;
        throw new WebViewPackageMissingException("Could not find a loadable WebView package");
    }

    static boolean isInstalledAndEnabledForAllUsers(List<UserPackage> list) {
        for (UserPackage userPackage : list) {
            if (!userPackage.isInstalledPackage() || !userPackage.isEnabledPackage()) {
                return false;
            }
        }
        return true;
    }

    void notifyRelroCreationCompleted() {
        synchronized (this.mLock) {
            this.mNumRelroCreationsFinished++;
            checkIfRelrosDoneLocked();
        }
    }

    WebViewProviderResponse waitForAndGetProvider() {
        boolean zWebViewIsReadyLocked;
        PackageInfo packageInfo;
        int i;
        long jNanoTime = (System.nanoTime() / 1000000) + 1000;
        synchronized (this.mLock) {
            zWebViewIsReadyLocked = webViewIsReadyLocked();
            while (!zWebViewIsReadyLocked) {
                long jNanoTime2 = System.nanoTime() / 1000000;
                if (jNanoTime2 >= jNanoTime) {
                    break;
                }
                try {
                    this.mLock.wait(jNanoTime - jNanoTime2);
                } catch (InterruptedException e) {
                }
                zWebViewIsReadyLocked = webViewIsReadyLocked();
            }
            packageInfo = this.mCurrentWebViewPackage;
            if (!zWebViewIsReadyLocked) {
                if (!this.mAnyWebViewInstalled) {
                    i = 4;
                } else {
                    i = 3;
                    Slog.e(TAG, "Timed out waiting for relro creation, relros started " + this.mNumRelroCreationsStarted + " relros finished " + this.mNumRelroCreationsFinished + " package dirty? " + this.mWebViewPackageDirty);
                }
            } else {
                i = 0;
            }
        }
        if (!zWebViewIsReadyLocked) {
            Slog.w(TAG, "creating relro file timed out");
        }
        return new WebViewProviderResponse(packageInfo, i);
    }

    PackageInfo getCurrentWebViewPackage() {
        PackageInfo packageInfo;
        synchronized (this.mLock) {
            packageInfo = this.mCurrentWebViewPackage;
        }
        return packageInfo;
    }

    private boolean webViewIsReadyLocked() {
        return !this.mWebViewPackageDirty && this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished && this.mAnyWebViewInstalled;
    }

    private void checkIfRelrosDoneLocked() {
        if (this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished) {
            if (this.mWebViewPackageDirty) {
                this.mWebViewPackageDirty = false;
                try {
                    onWebViewProviderChanged(findPreferredWebViewPackage());
                    return;
                } catch (WebViewPackageMissingException e) {
                    this.mCurrentWebViewPackage = null;
                    return;
                }
            }
            this.mLock.notifyAll();
        }
    }

    boolean isValidProvider(WebViewProviderInfo webViewProviderInfo, PackageInfo packageInfo) {
        return validityResult(webViewProviderInfo, packageInfo) == 0;
    }

    private int validityResult(WebViewProviderInfo webViewProviderInfo, PackageInfo packageInfo) {
        if (!UserPackage.hasCorrectTargetSdkVersion(packageInfo)) {
            return 1;
        }
        if (!versionCodeGE(packageInfo.getLongVersionCode(), getMinimumVersionCode()) && !this.mSystemInterface.systemIsDebuggable()) {
            return 2;
        }
        if (!providerHasValidSignature(webViewProviderInfo, packageInfo, this.mSystemInterface)) {
            return 3;
        }
        if (WebViewFactory.getWebViewLibrary(packageInfo.applicationInfo) == null) {
            return 4;
        }
        return 0;
    }

    private static boolean versionCodeGE(long j, long j2) {
        return j / 100000 >= j2 / 100000;
    }

    private long getMinimumVersionCode() {
        if (this.mMinimumVersionCode > 0) {
            return this.mMinimumVersionCode;
        }
        long j = -1;
        for (WebViewProviderInfo webViewProviderInfo : this.mSystemInterface.getWebViewPackages()) {
            if (webViewProviderInfo.availableByDefault && !webViewProviderInfo.isFallback) {
                try {
                    long factoryPackageVersion = this.mSystemInterface.getFactoryPackageVersion(webViewProviderInfo.packageName);
                    if (j < 0 || factoryPackageVersion < j) {
                        j = factoryPackageVersion;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        this.mMinimumVersionCode = j;
        return this.mMinimumVersionCode;
    }

    private static boolean providerHasValidSignature(WebViewProviderInfo webViewProviderInfo, PackageInfo packageInfo, SystemInterface systemInterface) {
        if (systemInterface.systemIsDebuggable()) {
            return true;
        }
        if (webViewProviderInfo.signatures == null || webViewProviderInfo.signatures.length == 0) {
            return packageInfo.applicationInfo.isSystemApp();
        }
        if (packageInfo.signatures.length != 1) {
            return false;
        }
        for (Signature signature : webViewProviderInfo.signatures) {
            if (signature.equals(packageInfo.signatures[0])) {
                return true;
            }
        }
        return false;
    }

    void dumpState(PrintWriter printWriter) {
        synchronized (this.mLock) {
            if (this.mCurrentWebViewPackage == null) {
                printWriter.println("  Current WebView package is null");
            } else {
                printWriter.println(String.format("  Current WebView package (name, version): (%s, %s)", this.mCurrentWebViewPackage.packageName, this.mCurrentWebViewPackage.versionName));
            }
            printWriter.println(String.format("  Minimum WebView version code: %d", Long.valueOf(this.mMinimumVersionCode)));
            printWriter.println(String.format("  Number of relros started: %d", Integer.valueOf(this.mNumRelroCreationsStarted)));
            printWriter.println(String.format("  Number of relros finished: %d", Integer.valueOf(this.mNumRelroCreationsFinished)));
            printWriter.println(String.format("  WebView package dirty: %b", Boolean.valueOf(this.mWebViewPackageDirty)));
            printWriter.println(String.format("  Any WebView package installed: %b", Boolean.valueOf(this.mAnyWebViewInstalled)));
            try {
                PackageInfo packageInfoFindPreferredWebViewPackage = findPreferredWebViewPackage();
                printWriter.println(String.format("  Preferred WebView package (name, version): (%s, %s)", packageInfoFindPreferredWebViewPackage.packageName, packageInfoFindPreferredWebViewPackage.versionName));
            } catch (WebViewPackageMissingException e) {
                printWriter.println(String.format("  Preferred WebView package: none", new Object[0]));
            }
            dumpAllPackageInformationLocked(printWriter);
        }
    }

    private void dumpAllPackageInformationLocked(PrintWriter printWriter) {
        WebViewProviderInfo[] webViewPackages = this.mSystemInterface.getWebViewPackages();
        printWriter.println("  WebView packages:");
        for (WebViewProviderInfo webViewProviderInfo : webViewPackages) {
            PackageInfo packageInfo = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, webViewProviderInfo).get(0).getPackageInfo();
            if (packageInfo == null) {
                printWriter.println(String.format("    %s is NOT installed.", webViewProviderInfo.packageName));
            } else {
                int iValidityResult = validityResult(webViewProviderInfo, packageInfo);
                String str = String.format("versionName: %s, versionCode: %d, targetSdkVersion: %d", packageInfo.versionName, Long.valueOf(packageInfo.getLongVersionCode()), Integer.valueOf(packageInfo.applicationInfo.targetSdkVersion));
                if (iValidityResult == 0) {
                    boolean zIsInstalledAndEnabledForAllUsers = isInstalledAndEnabledForAllUsers(this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, webViewProviderInfo));
                    Object[] objArr = new Object[3];
                    objArr[0] = packageInfo.packageName;
                    objArr[1] = str;
                    objArr[2] = zIsInstalledAndEnabledForAllUsers ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "NOT";
                    printWriter.println(String.format("    Valid package %s (%s) is %s installed/enabled for all users", objArr));
                } else {
                    printWriter.println(String.format("    Invalid package %s (%s), reason: %s", packageInfo.packageName, str, getInvalidityReason(iValidityResult)));
                }
            }
        }
    }

    private static String getInvalidityReason(int i) {
        switch (i) {
            case 1:
                return "SDK version too low";
            case 2:
                return "Version code too low";
            case 3:
                return "Incorrect signature";
            case 4:
                return "No WebView-library manifest flag";
            default:
                return "Unexcepted validity-reason";
        }
    }
}
