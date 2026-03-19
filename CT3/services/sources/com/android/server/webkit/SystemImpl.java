package com.android.server.webkit;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class SystemImpl implements SystemInterface {
    private static final int PACKAGE_FLAGS = 268443840;
    private static final String TAG = SystemImpl.class.getSimpleName();
    private static final String TAG_AVAILABILITY = "availableByDefault";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_FALLBACK = "isFallback";
    private static final String TAG_PACKAGE_NAME = "packageName";
    private static final String TAG_SIGNATURE = "signature";
    private static final String TAG_START = "webviewproviders";
    private static final String TAG_WEBVIEW_PROVIDER = "webviewprovider";
    private final WebViewProviderInfo[] mWebViewProviderPackages;

    SystemImpl(SystemImpl systemImpl) {
        this();
    }

    private static class LazyHolder {
        private static final SystemImpl INSTANCE = new SystemImpl(null);

        private LazyHolder() {
        }
    }

    public static SystemImpl getInstance() {
        return LazyHolder.INSTANCE;
    }

    private SystemImpl() {
        int numFallbackPackages = 0;
        int numAvailableByDefaultPackages = 0;
        int numAvByDefaultAndNotFallback = 0;
        XmlResourceParser xmlResourceParser = null;
        List<WebViewProviderInfo> webViewProviders = new ArrayList<>();
        try {
            try {
                XmlResourceParser parser = AppGlobals.getInitialApplication().getResources().getXml(R.bool.config_perDisplayFocusEnabled);
                XmlUtils.beginDocument(parser, TAG_START);
                while (true) {
                    XmlUtils.nextElement(parser);
                    String element = parser.getName();
                    if (element != null) {
                        if (element.equals(TAG_WEBVIEW_PROVIDER)) {
                            String packageName = parser.getAttributeValue(null, TAG_PACKAGE_NAME);
                            if (packageName == null) {
                                throw new AndroidRuntimeException("WebView provider in framework resources missing package name");
                            }
                            String description = parser.getAttributeValue(null, TAG_DESCRIPTION);
                            if (description == null) {
                                throw new AndroidRuntimeException("WebView provider in framework resources missing description");
                            }
                            boolean availableByDefault = "true".equals(parser.getAttributeValue(null, TAG_AVAILABILITY));
                            boolean isFallback = "true".equals(parser.getAttributeValue(null, TAG_FALLBACK));
                            WebViewProviderInfo currentProvider = new WebViewProviderInfo(packageName, description, availableByDefault, isFallback, readSignatures(parser));
                            if (currentProvider.isFallback) {
                                numFallbackPackages++;
                                if (!currentProvider.availableByDefault) {
                                    throw new AndroidRuntimeException("Each WebView fallback package must be available by default.");
                                }
                                if (numFallbackPackages > 1) {
                                    throw new AndroidRuntimeException("There can be at most one WebView fallback package.");
                                }
                            }
                            if (currentProvider.availableByDefault) {
                                numAvailableByDefaultPackages++;
                                if (!currentProvider.isFallback) {
                                    numAvByDefaultAndNotFallback++;
                                }
                            }
                            webViewProviders.add(currentProvider);
                        } else {
                            Log.e(TAG, "Found an element that is not a WebView provider");
                        }
                    } else {
                        if (parser != null) {
                            parser.close();
                        }
                        if (numAvailableByDefaultPackages == 0) {
                            throw new AndroidRuntimeException("There must be at least one WebView package that is available by default");
                        }
                        if (numAvByDefaultAndNotFallback == 0) {
                            throw new AndroidRuntimeException("There must be at least one WebView package that is available by default and not a fallback");
                        }
                        this.mWebViewProviderPackages = (WebViewProviderInfo[]) webViewProviders.toArray(new WebViewProviderInfo[webViewProviders.size()]);
                        return;
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                throw new AndroidRuntimeException("Error when parsing WebView config " + e);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                xmlResourceParser.close();
            }
            throw th;
        }
    }

    @Override
    public WebViewProviderInfo[] getWebViewPackages() {
        return this.mWebViewProviderPackages;
    }

    @Override
    public int getFactoryPackageVersion(String packageName) throws PackageManager.NameNotFoundException {
        PackageManager pm = AppGlobals.getInitialApplication().getPackageManager();
        return pm.getPackageInfo(packageName, 2097152).versionCode;
    }

    private static String[] readSignatures(XmlResourceParser parser) throws XmlPullParserException, IOException {
        List<String> signatures = new ArrayList<>();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_SIGNATURE)) {
                String signature = parser.nextText();
                signatures.add(signature);
            } else {
                Log.e(TAG, "Found an element in a webview provider that is not a signature");
            }
        }
        return (String[]) signatures.toArray(new String[signatures.size()]);
    }

    @Override
    public int onWebViewProviderChanged(PackageInfo packageInfo) {
        return WebViewFactory.onWebViewProviderChanged(packageInfo);
    }

    @Override
    public String getUserChosenWebViewProvider(Context context) {
        return Settings.Global.getString(context.getContentResolver(), "webview_provider");
    }

    @Override
    public void updateUserSetting(Context context, String newProviderName) {
        ContentResolver contentResolver = context.getContentResolver();
        if (newProviderName == null) {
            newProviderName = "";
        }
        Settings.Global.putString(contentResolver, "webview_provider", newProviderName);
    }

    @Override
    public void killPackageDependents(String packageName) {
        try {
            ActivityManagerNative.getDefault().killPackageDependents(packageName, -1);
        } catch (RemoteException e) {
        }
    }

    @Override
    public boolean isFallbackLogicEnabled() {
        return Settings.Global.getInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", 1) == 1;
    }

    @Override
    public void enableFallbackLogic(boolean enable) {
        Settings.Global.putInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", enable ? 1 : 0);
    }

    @Override
    public void uninstallAndDisablePackageForAllUsers(final Context context, String packageName) {
        enablePackageForAllUsers(context, packageName, false);
        try {
            PackageManager pm = AppGlobals.getInitialApplication().getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            if (applicationInfo == null || !applicationInfo.isUpdatedSystemApp()) {
                return;
            }
            pm.deletePackage(packageName, new IPackageDeleteObserver.Stub() {
                public void packageDeleted(String packageName2, int returnCode) {
                    SystemImpl.this.enablePackageForAllUsers(context, packageName2, false);
                }
            }, 6);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    @Override
    public void enablePackageForAllUsers(Context context, String packageName, boolean enable) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        for (UserInfo userInfo : userManager.getUsers()) {
            enablePackageForUser(packageName, enable, userInfo.id);
        }
    }

    @Override
    public void enablePackageForUser(String packageName, boolean enable, int userId) {
        try {
            AppGlobals.getPackageManager().setApplicationEnabledSetting(packageName, enable ? 0 : 3, 0, userId, (String) null);
        } catch (RemoteException | IllegalArgumentException e) {
            Log.w(TAG, "Tried to " + (enable ? "enable " : "disable ") + packageName + " for user " + userId + ": " + e);
        }
    }

    @Override
    public boolean systemIsDebuggable() {
        return Build.IS_DEBUGGABLE;
    }

    @Override
    public PackageInfo getPackageInfoForProvider(WebViewProviderInfo configInfo) throws PackageManager.NameNotFoundException {
        PackageManager pm = AppGlobals.getInitialApplication().getPackageManager();
        return pm.getPackageInfo(configInfo.packageName, PACKAGE_FLAGS);
    }
}
