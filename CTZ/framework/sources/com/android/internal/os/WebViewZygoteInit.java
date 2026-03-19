package com.android.internal.os;

import android.app.ApplicationLoaders;
import android.net.LocalSocket;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebViewFactory;
import android.webkit.WebViewFactoryProvider;
import android.webkit.WebViewLibraryLoader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

class WebViewZygoteInit {
    public static final String TAG = "WebViewZygoteInit";
    private static ZygoteServer sServer;

    WebViewZygoteInit() {
    }

    private static class WebViewZygoteServer extends ZygoteServer {
        private WebViewZygoteServer() {
        }

        @Override
        protected ZygoteConnection createNewConnection(LocalSocket localSocket, String str) throws IOException {
            return new WebViewZygoteConnection(localSocket, str);
        }
    }

    private static class WebViewZygoteConnection extends ZygoteConnection {
        WebViewZygoteConnection(LocalSocket localSocket, String str) throws IOException {
            super(localSocket, str);
        }

        @Override
        protected void preload() {
        }

        @Override
        protected boolean isPreloadComplete() {
            return true;
        }

        @Override
        protected void handlePreloadPackage(String str, String str2, String str3, String str4) {
            boolean z;
            boolean zBooleanValue;
            Log.i(WebViewZygoteInit.TAG, "Beginning package preload");
            ClassLoader classLoaderCreateAndCacheWebViewClassLoader = ApplicationLoaders.getDefault().createAndCacheWebViewClassLoader(str, str2, str4);
            WebViewLibraryLoader.loadNativeLibrary(classLoaderCreateAndCacheWebViewClassLoader, str3);
            for (String str5 : TextUtils.split(str, File.pathSeparator)) {
                Zygote.nativeAllowFileAcrossFork(str5);
            }
            int i = 1;
            try {
                Class<WebViewFactoryProvider> webViewProviderClass = WebViewFactory.getWebViewProviderClass(classLoaderCreateAndCacheWebViewClassLoader);
                Method method = webViewProviderClass.getMethod("preloadInZygote", new Class[0]);
                method.setAccessible(true);
                if (method.getReturnType() != Boolean.TYPE) {
                    Log.e(WebViewZygoteInit.TAG, "Unexpected return type: preloadInZygote must return boolean");
                    zBooleanValue = false;
                } else {
                    zBooleanValue = ((Boolean) webViewProviderClass.getMethod("preloadInZygote", new Class[0]).invoke(null, new Object[0])).booleanValue();
                    if (!zBooleanValue) {
                        try {
                            Log.e(WebViewZygoteInit.TAG, "preloadInZygote returned false");
                        } catch (ReflectiveOperationException e) {
                            z = zBooleanValue;
                            e = e;
                            Log.e(WebViewZygoteInit.TAG, "Exception while preloading package", e);
                            zBooleanValue = z;
                        }
                    }
                }
            } catch (ReflectiveOperationException e2) {
                e = e2;
                z = false;
            }
            try {
                DataOutputStream socketOutputStream = getSocketOutputStream();
                if (!zBooleanValue) {
                    i = 0;
                }
                socketOutputStream.writeInt(i);
                Log.i(WebViewZygoteInit.TAG, "Package preload done");
            } catch (IOException e3) {
                throw new IllegalStateException("Error writing to command socket", e3);
            }
        }
    }

    public static void main(String[] strArr) {
        Log.i(TAG, "Starting WebViewZygoteInit");
        String strSubstring = null;
        for (String str : strArr) {
            Log.i(TAG, str);
            if (str.startsWith(Zygote.CHILD_ZYGOTE_SOCKET_NAME_ARG)) {
                strSubstring = str.substring(Zygote.CHILD_ZYGOTE_SOCKET_NAME_ARG.length());
            }
        }
        if (strSubstring == null) {
            throw new RuntimeException("No --zygote-socket= specified");
        }
        try {
            Os.prctl(OsConstants.PR_SET_NO_NEW_PRIVS, 1L, 0L, 0L, 0L);
            sServer = new WebViewZygoteServer();
            try {
                try {
                    sServer.registerServerSocketAtAbstractName(strSubstring);
                    Zygote.nativeAllowFileAcrossFork("ABSTRACT/" + strSubstring);
                    Runnable runnableRunSelectLoop = sServer.runSelectLoop(TextUtils.join(",", Build.SUPPORTED_ABIS));
                    if (runnableRunSelectLoop != null) {
                        runnableRunSelectLoop.run();
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal exception:", e);
                    throw e;
                }
            } finally {
                sServer.closeServerSocket();
            }
        } catch (ErrnoException e2) {
            throw new RuntimeException("Failed to set PR_SET_NO_NEW_PRIVS", e2);
        }
    }
}
