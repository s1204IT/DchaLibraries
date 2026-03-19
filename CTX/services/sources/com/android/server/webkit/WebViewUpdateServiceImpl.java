package com.android.server.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.webkit.UserPackage;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewProviderResponse;
import java.io.PrintWriter;
import java.util.List;

public class WebViewUpdateServiceImpl {
    private static final int MULTIPROCESS_SETTING_OFF_VALUE = Integer.MIN_VALUE;
    private static final int MULTIPROCESS_SETTING_ON_VALUE = Integer.MAX_VALUE;
    private static final String TAG = WebViewUpdateServiceImpl.class.getSimpleName();
    private final Context mContext;
    private SystemInterface mSystemInterface;
    private WebViewUpdater mWebViewUpdater;

    public WebViewUpdateServiceImpl(Context context, SystemInterface systemInterface) {
        this.mContext = context;
        this.mSystemInterface = systemInterface;
        this.mWebViewUpdater = new WebViewUpdater(this.mContext, this.mSystemInterface);
    }

    void packageStateChanged(String str, int i, int i2) {
        updateFallbackStateOnPackageChange(str, i);
        this.mWebViewUpdater.packageStateChanged(str, i);
    }

    void prepareWebViewInSystemServer() {
        updateFallbackStateOnBoot();
        this.mWebViewUpdater.prepareWebViewInSystemServer();
        this.mSystemInterface.notifyZygote(isMultiProcessEnabled());
    }

    private boolean existsValidNonFallbackProvider(WebViewProviderInfo[] webViewProviderInfoArr) {
        for (WebViewProviderInfo webViewProviderInfo : webViewProviderInfoArr) {
            if (webViewProviderInfo.availableByDefault && !webViewProviderInfo.isFallback) {
                List<UserPackage> packageInfoForProviderAllUsers = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, webViewProviderInfo);
                if (WebViewUpdater.isInstalledAndEnabledForAllUsers(packageInfoForProviderAllUsers) && this.mWebViewUpdater.isValidProvider(webViewProviderInfo, packageInfoForProviderAllUsers.get(0).getPackageInfo())) {
                    return true;
                }
            }
        }
        return false;
    }

    void handleNewUser(int i) {
        if (i == 0) {
            return;
        }
        handleUserChange();
    }

    void handleUserRemoved(int i) {
        handleUserChange();
    }

    private void handleUserChange() {
        if (this.mSystemInterface.isFallbackLogicEnabled()) {
            updateFallbackState(this.mSystemInterface.getWebViewPackages());
        }
        this.mWebViewUpdater.updateCurrentWebViewPackage(null);
    }

    void notifyRelroCreationCompleted() {
        this.mWebViewUpdater.notifyRelroCreationCompleted();
    }

    WebViewProviderResponse waitForAndGetProvider() {
        return this.mWebViewUpdater.waitForAndGetProvider();
    }

    String changeProviderAndSetting(String str) {
        return this.mWebViewUpdater.changeProviderAndSetting(str);
    }

    WebViewProviderInfo[] getValidWebViewPackages() {
        return this.mWebViewUpdater.getValidWebViewPackages();
    }

    WebViewProviderInfo[] getWebViewPackages() {
        return this.mSystemInterface.getWebViewPackages();
    }

    PackageInfo getCurrentWebViewPackage() {
        return this.mWebViewUpdater.getCurrentWebViewPackage();
    }

    void enableFallbackLogic(boolean z) {
        this.mSystemInterface.enableFallbackLogic(z);
    }

    private void updateFallbackStateOnBoot() {
        if (this.mSystemInterface.isFallbackLogicEnabled()) {
            updateFallbackState(this.mSystemInterface.getWebViewPackages());
        }
    }

    private void updateFallbackStateOnPackageChange(String str, int i) {
        if (this.mSystemInterface.isFallbackLogicEnabled()) {
            WebViewProviderInfo[] webViewPackages = this.mSystemInterface.getWebViewPackages();
            int length = webViewPackages.length;
            boolean z = false;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                WebViewProviderInfo webViewProviderInfo = webViewPackages[i2];
                if (!webViewProviderInfo.packageName.equals(str)) {
                    i2++;
                } else if (webViewProviderInfo.availableByDefault) {
                    z = true;
                }
            }
            if (z) {
                updateFallbackState(webViewPackages);
            }
        }
    }

    private void updateFallbackState(WebViewProviderInfo[] webViewProviderInfoArr) {
        WebViewProviderInfo fallbackProvider = getFallbackProvider(webViewProviderInfoArr);
        if (fallbackProvider == null) {
            return;
        }
        boolean zExistsValidNonFallbackProvider = existsValidNonFallbackProvider(webViewProviderInfoArr);
        List<UserPackage> packageInfoForProviderAllUsers = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, fallbackProvider);
        if (zExistsValidNonFallbackProvider && !isDisabledForAllUsers(packageInfoForProviderAllUsers)) {
            this.mSystemInterface.uninstallAndDisablePackageForAllUsers(this.mContext, fallbackProvider.packageName);
        } else if (!zExistsValidNonFallbackProvider && !WebViewUpdater.isInstalledAndEnabledForAllUsers(packageInfoForProviderAllUsers)) {
            this.mSystemInterface.enablePackageForAllUsers(this.mContext, fallbackProvider.packageName, true);
        }
    }

    private static WebViewProviderInfo getFallbackProvider(WebViewProviderInfo[] webViewProviderInfoArr) {
        for (WebViewProviderInfo webViewProviderInfo : webViewProviderInfoArr) {
            if (webViewProviderInfo.isFallback) {
                return webViewProviderInfo;
            }
        }
        return null;
    }

    boolean isFallbackPackage(String str) {
        WebViewProviderInfo fallbackProvider;
        return str != null && this.mSystemInterface.isFallbackLogicEnabled() && (fallbackProvider = getFallbackProvider(this.mSystemInterface.getWebViewPackages())) != null && str.equals(fallbackProvider.packageName);
    }

    boolean isMultiProcessEnabled() {
        int multiProcessSetting = this.mSystemInterface.getMultiProcessSetting(this.mContext);
        return this.mSystemInterface.isMultiProcessDefaultEnabled() ? multiProcessSetting > Integer.MIN_VALUE : multiProcessSetting >= MULTIPROCESS_SETTING_ON_VALUE;
    }

    void enableMultiProcess(boolean z) {
        PackageInfo currentWebViewPackage = getCurrentWebViewPackage();
        this.mSystemInterface.setMultiProcessSetting(this.mContext, z ? MULTIPROCESS_SETTING_ON_VALUE : Integer.MIN_VALUE);
        this.mSystemInterface.notifyZygote(z);
        if (currentWebViewPackage != null) {
            this.mSystemInterface.killPackageDependents(currentWebViewPackage.packageName);
        }
    }

    private static boolean isDisabledForAllUsers(List<UserPackage> list) {
        for (UserPackage userPackage : list) {
            if (userPackage.getPackageInfo() != null && userPackage.isEnabledPackage()) {
                return false;
            }
        }
        return true;
    }

    void dumpState(PrintWriter printWriter) {
        printWriter.println("Current WebView Update Service state");
        printWriter.println(String.format("  Fallback logic enabled: %b", Boolean.valueOf(this.mSystemInterface.isFallbackLogicEnabled())));
        printWriter.println(String.format("  Multiprocess enabled: %b", Boolean.valueOf(isMultiProcessEnabled())));
        this.mWebViewUpdater.dumpState(printWriter);
    }
}
