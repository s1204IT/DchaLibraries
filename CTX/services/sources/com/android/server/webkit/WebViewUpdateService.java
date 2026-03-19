package com.android.server.webkit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.Slog;
import android.webkit.IWebViewUpdateService;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewProviderResponse;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.pm.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class WebViewUpdateService extends SystemService {
    static final int PACKAGE_ADDED = 1;
    static final int PACKAGE_ADDED_REPLACED = 2;
    static final int PACKAGE_CHANGED = 0;
    static final int PACKAGE_REMOVED = 3;
    private static final String TAG = "WebViewUpdateService";
    private WebViewUpdateServiceImpl mImpl;
    private BroadcastReceiver mWebViewUpdatedReceiver;

    public WebViewUpdateService(Context context) {
        super(context);
        this.mImpl = new WebViewUpdateServiceImpl(context, SystemImpl.getInstance());
    }

    @Override
    public void onStart() {
        this.mWebViewUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int intExtra;
                intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                switch (intent.getAction()) {
                    case "android.intent.action.PACKAGE_REMOVED":
                        if (!intent.getExtras().getBoolean("android.intent.extra.REPLACING")) {
                            WebViewUpdateService.this.mImpl.packageStateChanged(WebViewUpdateService.packageNameFromIntent(intent), 3, intExtra);
                            break;
                        }
                        break;
                    case "android.intent.action.PACKAGE_CHANGED":
                        if (WebViewUpdateService.entirePackageChanged(intent)) {
                            WebViewUpdateService.this.mImpl.packageStateChanged(WebViewUpdateService.packageNameFromIntent(intent), 0, intExtra);
                            break;
                        }
                        break;
                    case "android.intent.action.PACKAGE_ADDED":
                        WebViewUpdateService.this.mImpl.packageStateChanged(WebViewUpdateService.packageNameFromIntent(intent), intent.getExtras().getBoolean("android.intent.extra.REPLACING") ? 2 : 1, intExtra);
                        break;
                    case "android.intent.action.USER_STARTED":
                        WebViewUpdateService.this.mImpl.handleNewUser(intExtra);
                        break;
                    case "android.intent.action.USER_REMOVED":
                        WebViewUpdateService.this.mImpl.handleUserRemoved(intExtra);
                        break;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        for (WebViewProviderInfo webViewProviderInfo : this.mImpl.getWebViewPackages()) {
            intentFilter.addDataSchemeSpecificPart(webViewProviderInfo.packageName, 0);
        }
        getContext().registerReceiverAsUser(this.mWebViewUpdatedReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.USER_STARTED");
        intentFilter2.addAction("android.intent.action.USER_REMOVED");
        getContext().registerReceiverAsUser(this.mWebViewUpdatedReceiver, UserHandle.ALL, intentFilter2, null, null);
        publishBinderService("webviewupdate", new BinderService(), true);
    }

    public void prepareWebViewInSystemServer() {
        this.mImpl.prepareWebViewInSystemServer();
    }

    private static String packageNameFromIntent(Intent intent) {
        return intent.getDataString().substring("package:".length());
    }

    public static boolean entirePackageChanged(Intent intent) {
        return Arrays.asList(intent.getStringArrayExtra("android.intent.extra.changed_component_name_list")).contains(intent.getDataString().substring("package:".length()));
    }

    private class BinderService extends IWebViewUpdateService.Stub {
        private BinderService() {
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            new WebViewUpdateServiceShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }

        public void notifyRelroCreationCompleted() {
            if (Binder.getCallingUid() != 1037 && Binder.getCallingUid() != 1000) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                WebViewUpdateService.this.mImpl.notifyRelroCreationCompleted();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public WebViewProviderResponse waitForAndGetProvider() {
            if (Binder.getCallingPid() != Process.myPid()) {
                return WebViewUpdateService.this.mImpl.waitForAndGetProvider();
            }
            throw new IllegalStateException("Cannot create a WebView from the SystemServer");
        }

        public String changeProviderAndSetting(String str) {
            if (WebViewUpdateService.this.getContext().checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                String str2 = "Permission Denial: changeProviderAndSetting() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.WRITE_SECURE_SETTINGS";
                Slog.w(WebViewUpdateService.TAG, str2);
                throw new SecurityException(str2);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return WebViewUpdateService.this.mImpl.changeProviderAndSetting(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public WebViewProviderInfo[] getValidWebViewPackages() {
            return WebViewUpdateService.this.mImpl.getValidWebViewPackages();
        }

        public WebViewProviderInfo[] getAllWebViewPackages() {
            return WebViewUpdateService.this.mImpl.getWebViewPackages();
        }

        public String getCurrentWebViewPackageName() {
            PackageInfo currentWebViewPackage = WebViewUpdateService.this.mImpl.getCurrentWebViewPackage();
            if (currentWebViewPackage == null) {
                return null;
            }
            return currentWebViewPackage.packageName;
        }

        public PackageInfo getCurrentWebViewPackage() {
            return WebViewUpdateService.this.mImpl.getCurrentWebViewPackage();
        }

        public boolean isFallbackPackage(String str) {
            return WebViewUpdateService.this.mImpl.isFallbackPackage(str);
        }

        public void enableFallbackLogic(boolean z) {
            if (WebViewUpdateService.this.getContext().checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                String str = "Permission Denial: enableFallbackLogic() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.WRITE_SECURE_SETTINGS";
                Slog.w(WebViewUpdateService.TAG, str);
                throw new SecurityException(str);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                WebViewUpdateService.this.mImpl.enableFallbackLogic(z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isMultiProcessEnabled() {
            return WebViewUpdateService.this.mImpl.isMultiProcessEnabled();
        }

        public void enableMultiProcess(boolean z) {
            if (WebViewUpdateService.this.getContext().checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                String str = "Permission Denial: enableMultiProcess() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.WRITE_SECURE_SETTINGS";
                Slog.w(WebViewUpdateService.TAG, str);
                throw new SecurityException(str);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                WebViewUpdateService.this.mImpl.enableMultiProcess(z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(WebViewUpdateService.this.getContext(), WebViewUpdateService.TAG, printWriter)) {
                WebViewUpdateService.this.mImpl.dumpState(printWriter);
            }
        }
    }
}
