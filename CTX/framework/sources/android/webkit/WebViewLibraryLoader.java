package android.webkit;

import android.app.ActivityManagerInternal;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebViewFactory;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@VisibleForTesting
public class WebViewLibraryLoader {
    private static final long CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES = 104857600;
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_32 = "/data/misc/shared_relro/libwebviewchromium32.relro";
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_64 = "/data/misc/shared_relro/libwebviewchromium64.relro";
    private static final boolean DEBUG = false;
    private static final String LOGTAG = WebViewLibraryLoader.class.getSimpleName();
    private static boolean sAddressSpaceReserved = false;

    static native boolean nativeCreateRelroFile(String str, String str2);

    static native int nativeLoadWithRelroFile(String str, String str2, ClassLoader classLoader);

    static native boolean nativeReserveAddressSpace(long j);

    private static class RelroFileCreator {
        private RelroFileCreator() {
        }

        public static void main(String[] strArr) {
            boolean zIs64Bit = VMRuntime.getRuntime().is64Bit();
            try {
                if (strArr.length == 1 && strArr[0] != null) {
                    Log.v(WebViewLibraryLoader.LOGTAG, "RelroFileCreator (64bit = " + zIs64Bit + "), lib: " + strArr[0]);
                    if (!WebViewLibraryLoader.sAddressSpaceReserved) {
                        Log.e(WebViewLibraryLoader.LOGTAG, "can't create relro file; address space not reserved");
                        return;
                    }
                    boolean zNativeCreateRelroFile = WebViewLibraryLoader.nativeCreateRelroFile(strArr[0], zIs64Bit ? WebViewLibraryLoader.CHROMIUM_WEBVIEW_NATIVE_RELRO_64 : WebViewLibraryLoader.CHROMIUM_WEBVIEW_NATIVE_RELRO_32);
                    try {
                        WebViewFactory.getUpdateServiceUnchecked().notifyRelroCreationCompleted();
                    } catch (RemoteException e) {
                        Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e);
                    }
                    if (!zNativeCreateRelroFile) {
                        Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                    }
                    System.exit(0);
                    return;
                }
                Log.e(WebViewLibraryLoader.LOGTAG, "Invalid RelroFileCreator args: " + Arrays.toString(strArr));
                try {
                    WebViewFactory.getUpdateServiceUnchecked().notifyRelroCreationCompleted();
                } catch (RemoteException e2) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e2);
                }
                Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                System.exit(0);
            } finally {
                try {
                    WebViewFactory.getUpdateServiceUnchecked().notifyRelroCreationCompleted();
                } catch (RemoteException e3) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e3);
                }
                Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                System.exit(0);
            }
        }
    }

    static void createRelroFile(boolean z, WebViewNativeLibrary webViewNativeLibrary) {
        final String str = z ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e(WebViewLibraryLoader.LOGTAG, "relro file creator for " + str + " crashed. Proceeding without");
                    WebViewFactory.getUpdateService().notifyRelroCreationCompleted();
                } catch (RemoteException e) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "Cannot reach WebViewUpdateService. " + e.getMessage());
                }
            }
        };
        if (webViewNativeLibrary != null) {
            try {
                if (webViewNativeLibrary.path != null) {
                    if (!((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).startIsolatedProcess(RelroFileCreator.class.getName(), new String[]{webViewNativeLibrary.path}, "WebViewLoader-" + str, str, 1037, runnable)) {
                        throw new Exception("Failed to start the relro file creator process");
                    }
                    return;
                }
            } catch (Throwable th) {
                Log.e(LOGTAG, "error starting relro file creator for abi " + str, th);
                runnable.run();
                return;
            }
        }
        throw new IllegalArgumentException("Native library paths to the WebView RelRo process must not be null!");
    }

    static int prepareNativeLibraries(PackageInfo packageInfo) throws WebViewFactory.MissingWebViewPackageException {
        WebViewNativeLibrary webViewNativeLibrary = getWebViewNativeLibrary(packageInfo, false);
        WebViewNativeLibrary webViewNativeLibrary2 = getWebViewNativeLibrary(packageInfo, true);
        updateWebViewZygoteVmSize(webViewNativeLibrary, webViewNativeLibrary2);
        return createRelros(webViewNativeLibrary, webViewNativeLibrary2);
    }

    private static int createRelros(WebViewNativeLibrary webViewNativeLibrary, WebViewNativeLibrary webViewNativeLibrary2) {
        int i = 0;
        if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
            if (webViewNativeLibrary == null) {
                Log.e(LOGTAG, "No 32-bit WebView library path, skipping relro creation.");
            } else {
                createRelroFile(false, webViewNativeLibrary);
                i = 1;
            }
        }
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
            if (webViewNativeLibrary2 == null) {
                Log.e(LOGTAG, "No 64-bit WebView library path, skipping relro creation.");
                return i;
            }
            createRelroFile(true, webViewNativeLibrary2);
            return i + 1;
        }
        return i;
    }

    private static void updateWebViewZygoteVmSize(WebViewNativeLibrary webViewNativeLibrary, WebViewNativeLibrary webViewNativeLibrary2) throws WebViewFactory.MissingWebViewPackageException {
        long jMax = webViewNativeLibrary != null ? Math.max(0L, webViewNativeLibrary.size) : 0L;
        if (webViewNativeLibrary2 != null) {
            jMax = Math.max(jMax, webViewNativeLibrary2.size);
        }
        long jMax2 = Math.max(2 * jMax, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
        Log.d(LOGTAG, "Setting new address space to " + jMax2);
        setWebViewZygoteVmSize(jMax2);
    }

    static void reserveAddressSpaceInZygote() {
        System.loadLibrary("webviewchromium_loader");
        long j = SystemProperties.getLong(WebViewFactory.CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
        sAddressSpaceReserved = nativeReserveAddressSpace(j);
        if (!sAddressSpaceReserved) {
            Log.e(LOGTAG, "reserving " + j + " bytes of address space failed");
        }
    }

    public static int loadNativeLibrary(ClassLoader classLoader, String str) {
        if (!sAddressSpaceReserved) {
            Log.e(LOGTAG, "can't load with relro file; address space not reserved");
            return 2;
        }
        int iNativeLoadWithRelroFile = nativeLoadWithRelroFile(str, VMRuntime.getRuntime().is64Bit() ? CHROMIUM_WEBVIEW_NATIVE_RELRO_64 : CHROMIUM_WEBVIEW_NATIVE_RELRO_32, classLoader);
        if (iNativeLoadWithRelroFile != 0) {
            Log.w(LOGTAG, "failed to load with relro file, proceeding without");
        }
        return iNativeLoadWithRelroFile;
    }

    @VisibleForTesting
    public static WebViewNativeLibrary getWebViewNativeLibrary(PackageInfo packageInfo, boolean z) throws WebViewFactory.MissingWebViewPackageException {
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        return findNativeLibrary(applicationInfo, WebViewFactory.getWebViewLibrary(applicationInfo), z ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS, getWebViewNativeLibraryDirectory(applicationInfo, z));
    }

    @VisibleForTesting
    public static String getWebViewNativeLibraryDirectory(ApplicationInfo applicationInfo, boolean z) {
        if (z == VMRuntime.is64BitAbi(applicationInfo.primaryCpuAbi)) {
            return applicationInfo.nativeLibraryDir;
        }
        if (!TextUtils.isEmpty(applicationInfo.secondaryCpuAbi)) {
            return applicationInfo.secondaryNativeLibraryDir;
        }
        return "";
    }

    private static WebViewNativeLibrary findNativeLibrary(ApplicationInfo applicationInfo, String str, String[] strArr, String str2) throws WebViewFactory.MissingWebViewPackageException {
        if (TextUtils.isEmpty(str2)) {
            return null;
        }
        String str3 = str2 + "/" + str;
        File file = new File(str3);
        if (file.exists()) {
            return new WebViewNativeLibrary(str3, file.length());
        }
        return getLoadFromApkPath(applicationInfo.sourceDir, strArr, str);
    }

    @VisibleForTesting
    public static class WebViewNativeLibrary {
        public final String path;
        public final long size;

        WebViewNativeLibrary(String str, long j) {
            this.path = str;
            this.size = j;
        }
    }

    private static WebViewNativeLibrary getLoadFromApkPath(String str, String[] strArr, String str2) throws WebViewFactory.MissingWebViewPackageException {
        int i;
        try {
            ZipFile zipFile = new ZipFile(str);
            Throwable th = null;
            try {
                try {
                } finally {
                }
            } finally {
            }
            for (String str3 : strArr) {
                String str4 = "lib/" + str3 + "/" + str2;
                ZipEntry entry = zipFile.getEntry(str4);
                if (entry != null && entry.getMethod() == 0) {
                    WebViewNativeLibrary webViewNativeLibrary = new WebViewNativeLibrary(str + "!/" + str4, entry.getSize());
                    zipFile.close();
                    return webViewNativeLibrary;
                }
            }
            zipFile.close();
            return null;
        } catch (IOException e) {
            throw new WebViewFactory.MissingWebViewPackageException(e);
        }
    }

    private static void setWebViewZygoteVmSize(long j) {
        SystemProperties.set(WebViewFactory.CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, Long.toString(j));
    }
}
