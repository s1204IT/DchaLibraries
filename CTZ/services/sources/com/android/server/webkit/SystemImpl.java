package com.android.server.webkit;

import android.R;
import android.app.ActivityManager;
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
import android.webkit.UserPackage;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewZygote;
import com.android.internal.util.XmlUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class SystemImpl implements SystemInterface {
    private static final int PACKAGE_FLAGS = 272630976;
    private static final String TAG = SystemImpl.class.getSimpleName();
    private static final String TAG_AVAILABILITY = "availableByDefault";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_FALLBACK = "isFallback";
    private static final String TAG_PACKAGE_NAME = "packageName";
    private static final String TAG_SIGNATURE = "signature";
    private static final String TAG_START = "webviewproviders";
    private static final String TAG_WEBVIEW_PROVIDER = "webviewprovider";
    private final WebViewProviderInfo[] mWebViewProviderPackages;

    private static class LazyHolder {
        private static final SystemImpl INSTANCE = new SystemImpl();

        private LazyHolder() {
        }
    }

    public static SystemImpl getInstance() {
        return LazyHolder.INSTANCE;
    }

    private SystemImpl() throws Throwable {
        XmlResourceParser xml;
        ArrayList arrayList = new ArrayList();
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                xml = AppGlobals.getInitialApplication().getResources().getXml(R.xml.config_user_types);
            } catch (IOException | XmlPullParserException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
            xml = xmlResourceParser;
        }
        try {
            try {
                XmlUtils.beginDocument(xml, TAG_START);
                int i = 0;
                int i2 = 0;
                int i3 = 0;
                while (true) {
                    XmlUtils.nextElement(xml);
                    String name = xml.getName();
                    if (name == null) {
                        if (xml != null) {
                            xml.close();
                        }
                        if (i == 0) {
                            throw new AndroidRuntimeException("There must be at least one WebView package that is available by default");
                        }
                        if (i2 == 0) {
                            throw new AndroidRuntimeException("There must be at least one WebView package that is available by default and not a fallback");
                        }
                        this.mWebViewProviderPackages = (WebViewProviderInfo[]) arrayList.toArray(new WebViewProviderInfo[arrayList.size()]);
                        return;
                    }
                    if (name.equals(TAG_WEBVIEW_PROVIDER)) {
                        String attributeValue = xml.getAttributeValue(null, "packageName");
                        if (attributeValue == null) {
                            throw new AndroidRuntimeException("WebView provider in framework resources missing package name");
                        }
                        String attributeValue2 = xml.getAttributeValue(null, TAG_DESCRIPTION);
                        if (attributeValue2 == null) {
                            throw new AndroidRuntimeException("WebView provider in framework resources missing description");
                        }
                        WebViewProviderInfo webViewProviderInfo = new WebViewProviderInfo(attributeValue, attributeValue2, "true".equals(xml.getAttributeValue(null, TAG_AVAILABILITY)), "true".equals(xml.getAttributeValue(null, TAG_FALLBACK)), readSignatures(xml));
                        if (webViewProviderInfo.isFallback) {
                            i3++;
                            if (!webViewProviderInfo.availableByDefault) {
                                throw new AndroidRuntimeException("Each WebView fallback package must be available by default.");
                            }
                            if (i3 > 1) {
                                throw new AndroidRuntimeException("There can be at most one WebView fallback package.");
                            }
                        }
                        if (webViewProviderInfo.availableByDefault) {
                            i++;
                            if (!webViewProviderInfo.isFallback) {
                                i2++;
                            }
                        }
                        arrayList.add(webViewProviderInfo);
                    } else {
                        Log.e(TAG, "Found an element that is not a WebView provider");
                    }
                }
            } catch (IOException | XmlPullParserException e2) {
                e = e2;
                xmlResourceParser = xml;
                throw new AndroidRuntimeException("Error when parsing WebView config " + e);
            }
        } catch (Throwable th2) {
            th = th2;
            if (xml != null) {
                xml.close();
            }
            throw th;
        }
    }

    @Override
    public WebViewProviderInfo[] getWebViewPackages() {
        return this.mWebViewProviderPackages;
    }

    @Override
    public long getFactoryPackageVersion(String str) throws PackageManager.NameNotFoundException {
        return AppGlobals.getInitialApplication().getPackageManager().getPackageInfo(str, DumpState.DUMP_COMPILER_STATS).getLongVersionCode();
    }

    private static String[] readSignatures(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
        ArrayList arrayList = new ArrayList();
        int depth = xmlResourceParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlResourceParser, depth)) {
            if (xmlResourceParser.getName().equals(TAG_SIGNATURE)) {
                arrayList.add(xmlResourceParser.nextText());
            } else {
                Log.e(TAG, "Found an element in a webview provider that is not a signature");
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
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
    public void updateUserSetting(Context context, String str) {
        ContentResolver contentResolver = context.getContentResolver();
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        Settings.Global.putString(contentResolver, "webview_provider", str);
    }

    @Override
    public void killPackageDependents(String str) {
        try {
            ActivityManager.getService().killPackageDependents(str, -1);
        } catch (RemoteException e) {
        }
    }

    @Override
    public boolean isFallbackLogicEnabled() {
        return Settings.Global.getInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", 1) == 1;
    }

    @Override
    public void enableFallbackLogic(boolean z) {
        Settings.Global.putInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", z ? 1 : 0);
    }

    @Override
    public void uninstallAndDisablePackageForAllUsers(final Context context, String str) {
        enablePackageForAllUsers(context, str, false);
        try {
            PackageManager packageManager = AppGlobals.getInitialApplication().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 0);
            if (applicationInfo != null && applicationInfo.isUpdatedSystemApp()) {
                packageManager.deletePackage(str, new IPackageDeleteObserver.Stub() {
                    public void packageDeleted(String str2, int i) {
                        SystemImpl.this.enablePackageForAllUsers(context, str2, false);
                    }
                }, 6);
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    @Override
    public void enablePackageForAllUsers(Context context, String str, boolean z) {
        Iterator it = ((UserManager) context.getSystemService("user")).getUsers().iterator();
        while (it.hasNext()) {
            enablePackageForUser(str, z, ((UserInfo) it.next()).id);
        }
    }

    @Override
    public void enablePackageForUser(String str, boolean z, int i) {
        try {
            AppGlobals.getPackageManager().setApplicationEnabledSetting(str, z ? 0 : 3, 0, i, (String) null);
        } catch (RemoteException | IllegalArgumentException e) {
            String str2 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Tried to ");
            sb.append(z ? "enable " : "disable ");
            sb.append(str);
            sb.append(" for user ");
            sb.append(i);
            sb.append(": ");
            sb.append(e);
            Log.w(str2, sb.toString());
        }
    }

    @Override
    public boolean systemIsDebuggable() {
        return Build.IS_DEBUGGABLE;
    }

    @Override
    public PackageInfo getPackageInfoForProvider(WebViewProviderInfo webViewProviderInfo) throws PackageManager.NameNotFoundException {
        return AppGlobals.getInitialApplication().getPackageManager().getPackageInfo(webViewProviderInfo.packageName, PACKAGE_FLAGS);
    }

    @Override
    public List<UserPackage> getPackageInfoForProviderAllUsers(Context context, WebViewProviderInfo webViewProviderInfo) {
        return UserPackage.getPackageInfosAllUsers(context, webViewProviderInfo.packageName, PACKAGE_FLAGS);
    }

    @Override
    public int getMultiProcessSetting(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "webview_multiprocess", 0);
    }

    @Override
    public void setMultiProcessSetting(Context context, int i) {
        Settings.Global.putInt(context.getContentResolver(), "webview_multiprocess", i);
    }

    @Override
    public void notifyZygote(boolean z) {
        WebViewZygote.setMultiprocessEnabled(z);
    }

    @Override
    public boolean isMultiProcessDefaultEnabled() {
        return Build.SUPPORTED_64_BIT_ABIS.length > 0 || !ActivityManager.isLowRamDeviceStatic();
    }
}
