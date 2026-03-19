package com.android.server.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.webkit.WebViewProviderInfo;

public interface SystemInterface {
    void enableFallbackLogic(boolean z);

    void enablePackageForAllUsers(Context context, String str, boolean z);

    void enablePackageForUser(String str, boolean z, int i);

    int getFactoryPackageVersion(String str) throws PackageManager.NameNotFoundException;

    PackageInfo getPackageInfoForProvider(WebViewProviderInfo webViewProviderInfo) throws PackageManager.NameNotFoundException;

    String getUserChosenWebViewProvider(Context context);

    WebViewProviderInfo[] getWebViewPackages();

    boolean isFallbackLogicEnabled();

    void killPackageDependents(String str);

    int onWebViewProviderChanged(PackageInfo packageInfo);

    boolean systemIsDebuggable();

    void uninstallAndDisablePackageForAllUsers(Context context, String str);

    void updateUserSetting(Context context, String str);
}
