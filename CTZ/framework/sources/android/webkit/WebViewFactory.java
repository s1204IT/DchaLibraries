package android.webkit;

import android.annotation.SystemApi;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Trace;
import android.util.AndroidRuntimeException;
import android.util.ArraySet;
import android.util.Log;
import android.webkit.IWebViewUpdateService;
import java.io.File;
import java.lang.reflect.Method;

@SystemApi
public final class WebViewFactory {
    private static final String CHROMIUM_WEBVIEW_FACTORY = "com.android.webview.chromium.WebViewChromiumFactoryProviderForP";
    private static final String CHROMIUM_WEBVIEW_FACTORY_METHOD = "create";
    public static final String CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY = "persist.sys.webview.vmsize";
    private static final boolean DEBUG = false;
    public static final int LIBLOAD_ADDRESS_SPACE_NOT_RESERVED = 2;
    public static final int LIBLOAD_FAILED_JNI_CALL = 7;
    public static final int LIBLOAD_FAILED_LISTING_WEBVIEW_PACKAGES = 4;
    public static final int LIBLOAD_FAILED_TO_FIND_NAMESPACE = 10;
    public static final int LIBLOAD_FAILED_TO_LOAD_LIBRARY = 6;
    public static final int LIBLOAD_FAILED_TO_OPEN_RELRO_FILE = 5;
    public static final int LIBLOAD_FAILED_WAITING_FOR_RELRO = 3;
    public static final int LIBLOAD_FAILED_WAITING_FOR_WEBVIEW_REASON_UNKNOWN = 8;
    public static final int LIBLOAD_SUCCESS = 0;
    public static final int LIBLOAD_WRONG_PACKAGE_NAME = 1;
    private static final String LOGTAG = "WebViewFactory";
    private static String sDataDirectorySuffix;
    private static PackageInfo sPackageInfo;
    private static WebViewFactoryProvider sProviderInstance;
    private static boolean sWebViewDisabled;
    private static Boolean sWebViewSupported;
    private static final Object sProviderLock = new Object();
    private static String WEBVIEW_UPDATE_SERVICE_NAME = "webviewupdate";

    private static String getWebViewPreparationErrorReason(int i) {
        if (i != 8) {
            switch (i) {
                case 3:
                    return "Time out waiting for Relro files being created";
                case 4:
                    return "No WebView installed";
                default:
                    return "Unknown";
            }
        }
        return "Crashed for unknown reason";
    }

    static class MissingWebViewPackageException extends Exception {
        public MissingWebViewPackageException(String str) {
            super(str);
        }

        public MissingWebViewPackageException(Exception exc) {
            super(exc);
        }
    }

    private static boolean isWebViewSupported() {
        if (sWebViewSupported == null) {
            sWebViewSupported = Boolean.valueOf(AppGlobals.getInitialApplication().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WEBVIEW));
        }
        return sWebViewSupported.booleanValue();
    }

    static void disableWebView() {
        synchronized (sProviderLock) {
            if (sProviderInstance != null) {
                throw new IllegalStateException("Can't disable WebView: WebView already initialized");
            }
            sWebViewDisabled = true;
        }
    }

    static void setDataDirectorySuffix(String str) {
        synchronized (sProviderLock) {
            if (sProviderInstance != null) {
                throw new IllegalStateException("Can't set data directory suffix: WebView already initialized");
            }
            if (str.indexOf(File.separatorChar) >= 0) {
                throw new IllegalArgumentException("Suffix " + str + " contains a path separator");
            }
            sDataDirectorySuffix = str;
        }
    }

    static String getDataDirectorySuffix() {
        String str;
        synchronized (sProviderLock) {
            str = sDataDirectorySuffix;
        }
        return str;
    }

    public static String getWebViewLibrary(ApplicationInfo applicationInfo) {
        if (applicationInfo.metaData != null) {
            return applicationInfo.metaData.getString("com.android.webview.WebViewLibrary");
        }
        return null;
    }

    public static PackageInfo getLoadedPackageInfo() {
        PackageInfo packageInfo;
        synchronized (sProviderLock) {
            packageInfo = sPackageInfo;
        }
        return packageInfo;
    }

    public static Class<WebViewFactoryProvider> getWebViewProviderClass(ClassLoader classLoader) throws ClassNotFoundException {
        return Class.forName(CHROMIUM_WEBVIEW_FACTORY, true, classLoader);
    }

    public static int loadWebViewNativeLibraryFromPackage(String str, ClassLoader classLoader) {
        if (!isWebViewSupported()) {
            return 1;
        }
        try {
            WebViewProviderResponse webViewProviderResponseWaitForAndGetProvider = getUpdateService().waitForAndGetProvider();
            if (webViewProviderResponseWaitForAndGetProvider.status != 0 && webViewProviderResponseWaitForAndGetProvider.status != 3) {
                return webViewProviderResponseWaitForAndGetProvider.status;
            }
            if (!webViewProviderResponseWaitForAndGetProvider.packageInfo.packageName.equals(str)) {
                return 1;
            }
            try {
                int iLoadNativeLibrary = WebViewLibraryLoader.loadNativeLibrary(classLoader, getWebViewLibrary(AppGlobals.getInitialApplication().getPackageManager().getPackageInfo(str, 268435584).applicationInfo));
                return iLoadNativeLibrary == 0 ? webViewProviderResponseWaitForAndGetProvider.status : iLoadNativeLibrary;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOGTAG, "Couldn't find package " + str);
                return 1;
            }
        } catch (RemoteException e2) {
            Log.e(LOGTAG, "error waiting for relro creation", e2);
            return 8;
        }
    }

    static WebViewFactoryProvider getProvider() {
        Method method;
        synchronized (sProviderLock) {
            if (sProviderInstance != null) {
                return sProviderInstance;
            }
            int iMyUid = Process.myUid();
            if (iMyUid == 0 || iMyUid == 1000 || iMyUid == 1001 || iMyUid == 1027 || iMyUid == 1002) {
                throw new UnsupportedOperationException("For security reasons, WebView is not allowed in privileged processes");
            }
            if (!isWebViewSupported()) {
                throw new UnsupportedOperationException();
            }
            if (sWebViewDisabled) {
                throw new IllegalStateException("WebView.disableWebView() was called: WebView is disabled");
            }
            Trace.traceBegin(16L, "WebViewFactory.getProvider()");
            try {
                try {
                    method = getProviderClass().getMethod(CHROMIUM_WEBVIEW_FACTORY_METHOD, WebViewDelegate.class);
                } catch (Exception e) {
                    method = null;
                }
                Trace.traceBegin(16L, "WebViewFactoryProvider invocation");
                try {
                    sProviderInstance = (WebViewFactoryProvider) method.invoke(null, new WebViewDelegate());
                    WebViewFactoryProvider webViewFactoryProvider = sProviderInstance;
                    Trace.traceEnd(16L);
                    return webViewFactoryProvider;
                } catch (Exception e2) {
                    Log.e(LOGTAG, "error instantiating provider", e2);
                    throw new AndroidRuntimeException(e2);
                }
            } finally {
            }
        }
    }

    private static boolean signaturesEquals(Signature[] signatureArr, Signature[] signatureArr2) {
        if (signatureArr == null) {
            return signatureArr2 == null;
        }
        if (signatureArr2 == null) {
            return false;
        }
        ArraySet arraySet = new ArraySet();
        for (Signature signature : signatureArr) {
            arraySet.add(signature);
        }
        ArraySet arraySet2 = new ArraySet();
        for (Signature signature2 : signatureArr2) {
            arraySet2.add(signature2);
        }
        return arraySet.equals(arraySet2);
    }

    private static void verifyPackageInfo(PackageInfo packageInfo, PackageInfo packageInfo2) throws MissingWebViewPackageException {
        if (!packageInfo.packageName.equals(packageInfo2.packageName)) {
            throw new MissingWebViewPackageException("Failed to verify WebView provider, packageName mismatch, expected: " + packageInfo.packageName + " actual: " + packageInfo2.packageName);
        }
        if (packageInfo.getLongVersionCode() > packageInfo2.getLongVersionCode()) {
            throw new MissingWebViewPackageException("Failed to verify WebView provider, version code is lower than expected: " + packageInfo.getLongVersionCode() + " actual: " + packageInfo2.getLongVersionCode());
        }
        if (getWebViewLibrary(packageInfo2.applicationInfo) == null) {
            throw new MissingWebViewPackageException("Tried to load an invalid WebView provider: " + packageInfo2.packageName);
        }
        if (!signaturesEquals(packageInfo.signatures, packageInfo2.signatures)) {
            throw new MissingWebViewPackageException("Failed to verify WebView provider, signature mismatch");
        }
    }

    private static void fixupStubApplicationInfo(ApplicationInfo applicationInfo, PackageManager packageManager) throws MissingWebViewPackageException {
        String string;
        if (applicationInfo.metaData != null) {
            string = applicationInfo.metaData.getString("com.android.webview.WebViewDonorPackage");
        } else {
            string = null;
        }
        if (string != null) {
            try {
                ApplicationInfo applicationInfo2 = packageManager.getPackageInfo(string, 270541824).applicationInfo;
                applicationInfo.sourceDir = applicationInfo2.sourceDir;
                applicationInfo.splitSourceDirs = applicationInfo2.splitSourceDirs;
                applicationInfo.nativeLibraryDir = applicationInfo2.nativeLibraryDir;
                applicationInfo.secondaryNativeLibraryDir = applicationInfo2.secondaryNativeLibraryDir;
                applicationInfo.primaryCpuAbi = applicationInfo2.primaryCpuAbi;
                applicationInfo.secondaryCpuAbi = applicationInfo2.secondaryCpuAbi;
            } catch (PackageManager.NameNotFoundException e) {
                throw new MissingWebViewPackageException("Failed to find donor package: " + string);
            }
        }
    }

    private static Context getWebViewContextAndSetProvider() throws MissingWebViewPackageException {
        Application initialApplication = AppGlobals.getInitialApplication();
        try {
            Trace.traceBegin(16L, "WebViewUpdateService.waitForAndGetProvider()");
            try {
                WebViewProviderResponse webViewProviderResponseWaitForAndGetProvider = getUpdateService().waitForAndGetProvider();
                Trace.traceEnd(16L);
                if (webViewProviderResponseWaitForAndGetProvider.status != 0 && webViewProviderResponseWaitForAndGetProvider.status != 3) {
                    throw new MissingWebViewPackageException("Failed to load WebView provider: " + getWebViewPreparationErrorReason(webViewProviderResponseWaitForAndGetProvider.status));
                }
                Trace.traceBegin(16L, "ActivityManager.addPackageDependency()");
                try {
                    ActivityManager.getService().addPackageDependency(webViewProviderResponseWaitForAndGetProvider.packageInfo.packageName);
                    Trace.traceEnd(16L);
                    PackageManager packageManager = initialApplication.getPackageManager();
                    Trace.traceBegin(16L, "PackageManager.getPackageInfo()");
                    try {
                        PackageInfo packageInfo = packageManager.getPackageInfo(webViewProviderResponseWaitForAndGetProvider.packageInfo.packageName, 268444864);
                        Trace.traceEnd(16L);
                        verifyPackageInfo(webViewProviderResponseWaitForAndGetProvider.packageInfo, packageInfo);
                        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                        fixupStubApplicationInfo(applicationInfo, packageManager);
                        Trace.traceBegin(16L, "initialApplication.createApplicationContext");
                        try {
                            Context contextCreateApplicationContext = initialApplication.createApplicationContext(applicationInfo, 3);
                            sPackageInfo = packageInfo;
                            return contextCreateApplicationContext;
                        } finally {
                        }
                    } finally {
                    }
                } finally {
                }
            } finally {
            }
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
            throw new MissingWebViewPackageException("Failed to load WebView provider: " + e);
        }
    }

    private static Class<WebViewFactoryProvider> getProviderClass() {
        Application initialApplication = AppGlobals.getInitialApplication();
        try {
            Trace.traceBegin(16L, "WebViewFactory.getWebViewContextAndSetProvider()");
            try {
                Context webViewContextAndSetProvider = getWebViewContextAndSetProvider();
                Trace.traceEnd(16L);
                Log.i(LOGTAG, "Loading " + sPackageInfo.packageName + " version " + sPackageInfo.versionName + " (code " + sPackageInfo.getLongVersionCode() + ")");
                Trace.traceBegin(16L, "WebViewFactory.getChromiumProviderClass()");
                try {
                    try {
                        initialApplication.getAssets().addAssetPathAsSharedLibrary(webViewContextAndSetProvider.getApplicationInfo().sourceDir);
                        ClassLoader classLoader = webViewContextAndSetProvider.getClassLoader();
                        Trace.traceBegin(16L, "WebViewFactory.loadNativeLibrary()");
                        WebViewLibraryLoader.loadNativeLibrary(classLoader, getWebViewLibrary(sPackageInfo.applicationInfo));
                        Trace.traceEnd(16L);
                        Trace.traceBegin(16L, "Class.forName()");
                        try {
                            Class<WebViewFactoryProvider> webViewProviderClass = getWebViewProviderClass(classLoader);
                            Trace.traceEnd(16L);
                            return webViewProviderClass;
                        } finally {
                        }
                    } catch (ClassNotFoundException e) {
                        Log.e(LOGTAG, "error loading provider", e);
                        throw new AndroidRuntimeException(e);
                    }
                } finally {
                }
            } finally {
            }
        } catch (MissingWebViewPackageException e2) {
            Log.e(LOGTAG, "Chromium WebView package does not exist", e2);
            throw new AndroidRuntimeException(e2);
        }
    }

    public static void prepareWebViewInZygote() {
        try {
            WebViewLibraryLoader.reserveAddressSpaceInZygote();
        } catch (Throwable th) {
            Log.e(LOGTAG, "error preparing native loader", th);
        }
    }

    public static int onWebViewProviderChanged(PackageInfo packageInfo) {
        int iPrepareNativeLibraries;
        ApplicationInfo applicationInfo = new ApplicationInfo(packageInfo.applicationInfo);
        try {
            fixupStubApplicationInfo(packageInfo.applicationInfo, AppGlobals.getInitialApplication().getPackageManager());
            iPrepareNativeLibraries = WebViewLibraryLoader.prepareNativeLibraries(packageInfo);
        } catch (Throwable th) {
            Log.e(LOGTAG, "error preparing webview native library", th);
            iPrepareNativeLibraries = 0;
        }
        WebViewZygote.onWebViewProviderChanged(packageInfo, applicationInfo);
        return iPrepareNativeLibraries;
    }

    public static IWebViewUpdateService getUpdateService() {
        if (isWebViewSupported()) {
            return getUpdateServiceUnchecked();
        }
        return null;
    }

    static IWebViewUpdateService getUpdateServiceUnchecked() {
        return IWebViewUpdateService.Stub.asInterface(ServiceManager.getService(WEBVIEW_UPDATE_SERVICE_NAME));
    }
}
