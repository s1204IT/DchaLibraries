package android.webkit;

import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ProxyInfo;
import android.os.Build;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.Trace;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.webkit.IWebViewUpdateService;
import com.android.internal.R;
import com.android.server.LocalServices;
import dalvik.system.VMRuntime;
import java.io.File;
import java.util.Arrays;

public final class WebViewFactory {
    private static final long CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES = 104857600;
    private static final String CHROMIUM_WEBVIEW_FACTORY = "com.android.webview.chromium.WebViewChromiumFactoryProvider";
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_32 = "/data/misc/shared_relro/libwebviewchromium32.relro";
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_64 = "/data/misc/shared_relro/libwebviewchromium64.relro";
    public static final String CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY = "persist.sys.webview.vmsize";
    private static final boolean DEBUG = false;
    private static final String LOGTAG = "WebViewFactory";
    private static final String NULL_WEBVIEW_FACTORY = "com.android.webview.nullwebview.NullWebViewFactoryProvider";
    private static PackageInfo sPackageInfo;
    private static WebViewFactoryProvider sProviderInstance;
    private static final Object sProviderLock = new Object();
    private static boolean sAddressSpaceReserved = false;

    private static native boolean nativeCreateRelroFile(String str, String str2, String str3, String str4);

    private static native boolean nativeLoadWithRelroFile(String str, String str2, String str3, String str4);

    private static native boolean nativeReserveAddressSpace(long j);

    public static String getWebViewPackageName() {
        Application initialApp = AppGlobals.getInitialApplication();
        String pkg = initialApp.getString(R.string.config_alternateWebViewPackageName);
        return isPackageInstalled(initialApp, pkg) ? pkg : initialApp.getString(R.string.config_webViewPackageName);
    }

    public static PackageInfo getLoadedPackageInfo() {
        return sPackageInfo;
    }

    static WebViewFactoryProvider getProvider() {
        WebViewFactoryProvider webViewFactoryProvider;
        synchronized (sProviderLock) {
            if (sProviderInstance != null) {
                webViewFactoryProvider = sProviderInstance;
            } else {
                int uid = Process.myUid();
                if (uid == 0 || uid == 1000) {
                    throw new UnsupportedOperationException("For security reasons, WebView is not allowed in privileged processes");
                }
                Trace.traceBegin(16L, "WebViewFactory.getProvider()");
                try {
                    Trace.traceBegin(16L, "WebViewFactory.loadNativeLibrary()");
                    loadNativeLibrary();
                    Trace.traceEnd(16L);
                    Trace.traceBegin(16L, "WebViewFactory.getFactoryClass()");
                    try {
                        try {
                            Class<WebViewFactoryProvider> providerClass = getFactoryClass();
                            Trace.traceEnd(16L);
                            StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
                            Trace.traceBegin(16L, "providerClass.newInstance()");
                            try {
                                try {
                                    sProviderInstance = providerClass.getConstructor(WebViewDelegate.class).newInstance(new WebViewDelegate());
                                } catch (Exception e) {
                                    try {
                                        sProviderInstance = providerClass.newInstance();
                                    } catch (Exception e2) {
                                        Log.e(LOGTAG, "error instantiating provider", e2);
                                        throw new AndroidRuntimeException(e2);
                                    }
                                }
                                webViewFactoryProvider = sProviderInstance;
                                Trace.traceEnd(16L);
                            } finally {
                                Trace.traceEnd(16L);
                                StrictMode.setThreadPolicy(oldPolicy);
                            }
                        } catch (ClassNotFoundException e3) {
                            Log.e(LOGTAG, "error loading provider", e3);
                            throw new AndroidRuntimeException(e3);
                        }
                    } catch (Throwable th) {
                        Trace.traceEnd(16L);
                        throw th;
                    }
                } catch (Throwable th2) {
                    Trace.traceEnd(16L);
                    throw th2;
                }
            }
        }
        return webViewFactoryProvider;
    }

    private static Class<WebViewFactoryProvider> getFactoryClass() throws ClassNotFoundException {
        Application initialApplication = AppGlobals.getInitialApplication();
        try {
            String packageName = getWebViewPackageName();
            sPackageInfo = initialApplication.getPackageManager().getPackageInfo(packageName, 0);
            Log.i(LOGTAG, "Loading " + packageName + " version " + sPackageInfo.versionName + " (code " + sPackageInfo.versionCode + ")");
            Context webViewContext = initialApplication.createPackageContext(packageName, 3);
            initialApplication.getAssets().addAssetPath(webViewContext.getApplicationInfo().sourceDir);
            ClassLoader clazzLoader = webViewContext.getClassLoader();
            Trace.traceBegin(16L, "Class.forName()");
            try {
                Class cls = Class.forName(CHROMIUM_WEBVIEW_FACTORY, true, clazzLoader);
                Trace.traceEnd(16L);
                return cls;
            } catch (Throwable th) {
                Trace.traceEnd(16L);
                throw th;
            }
        } catch (PackageManager.NameNotFoundException e) {
            try {
                return Class.forName(NULL_WEBVIEW_FACTORY);
            } catch (ClassNotFoundException e2) {
                Log.e(LOGTAG, "Chromium WebView package does not exist", e);
                throw new AndroidRuntimeException(e);
            }
        }
    }

    public static void prepareWebViewInZygote() {
        try {
            System.loadLibrary("webviewchromium_loader");
            long addressSpaceToReserve = SystemProperties.getLong(CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
            sAddressSpaceReserved = nativeReserveAddressSpace(addressSpaceToReserve);
            if (!sAddressSpaceReserved) {
                Log.e(LOGTAG, "reserving " + addressSpaceToReserve + " bytes of address space failed");
            }
        } catch (Throwable t) {
            Log.e(LOGTAG, "error preparing native loader", t);
        }
    }

    public static void prepareWebViewInSystemServer() {
        String[] nativePaths = null;
        try {
            nativePaths = getWebViewNativeLibraryPaths();
        } catch (Throwable t) {
            Log.e(LOGTAG, "error preparing webview native library", t);
        }
        prepareWebViewInSystemServer(nativePaths);
    }

    private static void prepareWebViewInSystemServer(String[] nativeLibraryPaths) {
        if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
            createRelroFile(false, nativeLibraryPaths);
        }
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
            createRelroFile(true, nativeLibraryPaths);
        }
    }

    public static void onWebViewUpdateInstalled() {
        String[] nativeLibs = null;
        try {
            nativeLibs = getWebViewNativeLibraryPaths();
            if (nativeLibs != null) {
                long newVmSize = 0;
                for (String path : nativeLibs) {
                    if (path != null) {
                        File f = new File(path);
                        if (f.exists()) {
                            long length = f.length();
                            if (length > newVmSize) {
                                newVmSize = length;
                            }
                        }
                    }
                }
                long newVmSize2 = Math.max(2 * newVmSize, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
                Log.d(LOGTAG, "Setting new address space to " + newVmSize2);
                SystemProperties.set(CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, Long.toString(newVmSize2));
            }
        } catch (Throwable t) {
            Log.e(LOGTAG, "error preparing webview native library", t);
        }
        prepareWebViewInSystemServer(nativeLibs);
    }

    private static String[] getWebViewNativeLibraryPaths() throws PackageManager.NameNotFoundException {
        String path32;
        String path64;
        PackageManager pm = AppGlobals.getInitialApplication().getPackageManager();
        ApplicationInfo ai = pm.getApplicationInfo(getWebViewPackageName(), 0);
        boolean primaryArchIs64bit = VMRuntime.is64BitAbi(ai.primaryCpuAbi);
        if (!TextUtils.isEmpty(ai.secondaryCpuAbi)) {
            if (primaryArchIs64bit) {
                path64 = ai.nativeLibraryDir;
                path32 = ai.secondaryNativeLibraryDir;
            } else {
                path64 = ai.secondaryNativeLibraryDir;
                path32 = ai.nativeLibraryDir;
            }
        } else if (primaryArchIs64bit) {
            path64 = ai.nativeLibraryDir;
            path32 = ProxyInfo.LOCAL_EXCL_LIST;
        } else {
            path32 = ai.nativeLibraryDir;
            path64 = ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (!TextUtils.isEmpty(path32)) {
            path32 = path32 + "/libwebviewchromium.so";
        }
        if (!TextUtils.isEmpty(path64)) {
            path64 = path64 + "/libwebviewchromium.so";
        }
        return new String[]{path32, path64};
    }

    private static void createRelroFile(final boolean is64Bit, String[] nativeLibraryPaths) {
        final String abi = is64Bit ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        Runnable crashHandler = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e(WebViewFactory.LOGTAG, "relro file creator for " + abi + " crashed. Proceeding without");
                    WebViewFactory.getUpdateService().notifyRelroCreationCompleted(is64Bit, false);
                } catch (RemoteException e) {
                    Log.e(WebViewFactory.LOGTAG, "Cannot reach WebViewUpdateService. " + e.getMessage());
                }
            }
        };
        if (nativeLibraryPaths != null) {
            try {
                if (nativeLibraryPaths[0] != null && nativeLibraryPaths[1] != null) {
                    int pid = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).startIsolatedProcess(RelroFileCreator.class.getName(), nativeLibraryPaths, "WebViewLoader-" + abi, abi, 1037, crashHandler);
                    if (pid <= 0) {
                        throw new Exception("Failed to start the relro file creator process");
                    }
                    return;
                }
            } catch (Throwable t) {
                Log.e(LOGTAG, "error starting relro file creator for abi " + abi, t);
                crashHandler.run();
                return;
            }
        }
        throw new IllegalArgumentException("Native library paths to the WebView RelRo process must not be null!");
    }

    private static class RelroFileCreator {
        private RelroFileCreator() {
        }

        public static void main(String[] args) {
            boolean is64Bit = VMRuntime.getRuntime().is64Bit();
            try {
                if (args.length != 2 || args[0] == null || args[1] == null) {
                    Log.e(WebViewFactory.LOGTAG, "Invalid RelroFileCreator args: " + Arrays.toString(args));
                    return;
                }
                Log.v(WebViewFactory.LOGTAG, "RelroFileCreator (64bit = " + is64Bit + "),  32-bit lib: " + args[0] + ", 64-bit lib: " + args[1]);
                if (!WebViewFactory.sAddressSpaceReserved) {
                    Log.e(WebViewFactory.LOGTAG, "can't create relro file; address space not reserved");
                    try {
                        WebViewFactory.getUpdateService().notifyRelroCreationCompleted(is64Bit, false);
                    } catch (RemoteException e) {
                        Log.e(WebViewFactory.LOGTAG, "error notifying update service", e);
                    }
                    if (0 == 0) {
                        Log.e(WebViewFactory.LOGTAG, "failed to create relro file");
                    }
                    System.exit(0);
                    return;
                }
                boolean result = WebViewFactory.nativeCreateRelroFile(args[0], args[1], WebViewFactory.CHROMIUM_WEBVIEW_NATIVE_RELRO_32, WebViewFactory.CHROMIUM_WEBVIEW_NATIVE_RELRO_64);
                if (result) {
                }
                try {
                    WebViewFactory.getUpdateService().notifyRelroCreationCompleted(is64Bit, result);
                } catch (RemoteException e2) {
                    Log.e(WebViewFactory.LOGTAG, "error notifying update service", e2);
                }
                if (!result) {
                    Log.e(WebViewFactory.LOGTAG, "failed to create relro file");
                }
                System.exit(0);
            } finally {
                try {
                    WebViewFactory.getUpdateService().notifyRelroCreationCompleted(is64Bit, false);
                } catch (RemoteException e3) {
                    Log.e(WebViewFactory.LOGTAG, "error notifying update service", e3);
                }
                if (0 == 0) {
                    Log.e(WebViewFactory.LOGTAG, "failed to create relro file");
                }
                System.exit(0);
            }
        }
    }

    private static void loadNativeLibrary() {
        if (!sAddressSpaceReserved) {
            Log.e(LOGTAG, "can't load with relro file; address space not reserved");
            return;
        }
        try {
            getUpdateService().waitForRelroCreationCompleted(VMRuntime.getRuntime().is64Bit());
            try {
                String[] args = getWebViewNativeLibraryPaths();
                boolean result = nativeLoadWithRelroFile(args[0], args[1], CHROMIUM_WEBVIEW_NATIVE_RELRO_32, CHROMIUM_WEBVIEW_NATIVE_RELRO_64);
                if (!result) {
                    Log.w(LOGTAG, "failed to load with relro file, proceeding without");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOGTAG, "Failed to list WebView package libraries for loadNativeLibrary", e);
            }
        } catch (RemoteException e2) {
            Log.e(LOGTAG, "error waiting for relro creation, proceeding without", e2);
        }
    }

    private static IWebViewUpdateService getUpdateService() {
        return IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
