package com.android.server.webkit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Process;
import android.util.Slog;
import android.webkit.IWebViewUpdateService;
import android.webkit.WebViewFactory;
import com.android.server.SystemService;

public class WebViewUpdateService extends SystemService {
    private static final String TAG = "WebViewUpdateService";
    private static final int WAIT_TIMEOUT_MS = 5000;
    private boolean mRelroReady32Bit;
    private boolean mRelroReady64Bit;
    private BroadcastReceiver mWebViewUpdatedReceiver;

    public WebViewUpdateService(Context context) {
        super(context);
        this.mRelroReady32Bit = false;
        this.mRelroReady64Bit = false;
    }

    @Override
    public void onStart() {
        this.mWebViewUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String webviewPackage = "package:" + WebViewFactory.getWebViewPackageName();
                if (webviewPackage.equals(intent.getDataString())) {
                    WebViewUpdateService.this.onWebViewUpdateInstalled();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        getContext().registerReceiver(this.mWebViewUpdatedReceiver, filter);
        publishBinderService("webviewupdate", new BinderService());
    }

    private void onWebViewUpdateInstalled() {
        Slog.d(TAG, "WebView Package updated!");
        synchronized (this) {
            this.mRelroReady32Bit = false;
            this.mRelroReady64Bit = false;
        }
        WebViewFactory.onWebViewUpdateInstalled();
    }

    private class BinderService extends IWebViewUpdateService.Stub {
        private BinderService() {
        }

        public void notifyRelroCreationCompleted(boolean is64Bit, boolean success) {
            if (Binder.getCallingUid() == 1037 || Binder.getCallingUid() == 1000) {
                synchronized (WebViewUpdateService.this) {
                    if (is64Bit) {
                        WebViewUpdateService.this.mRelroReady64Bit = true;
                    } else {
                        WebViewUpdateService.this.mRelroReady32Bit = true;
                    }
                    WebViewUpdateService.this.notifyAll();
                }
            }
        }

        public void waitForRelroCreationCompleted(boolean is64Bit) {
            if (Binder.getCallingPid() == Process.myPid()) {
                throw new IllegalStateException("Cannot create a WebView from the SystemServer");
            }
            long timeoutTimeMs = (System.nanoTime() / 1000000) + 5000;
            boolean relroReady = is64Bit ? WebViewUpdateService.this.mRelroReady64Bit : WebViewUpdateService.this.mRelroReady32Bit;
            synchronized (WebViewUpdateService.this) {
                while (!relroReady) {
                    long timeNowMs = System.nanoTime() / 1000000;
                    if (timeNowMs >= timeoutTimeMs) {
                        break;
                    }
                    try {
                        WebViewUpdateService.this.wait(timeoutTimeMs - timeNowMs);
                    } catch (InterruptedException e) {
                    }
                    relroReady = is64Bit ? WebViewUpdateService.this.mRelroReady64Bit : WebViewUpdateService.this.mRelroReady32Bit;
                }
            }
            if (!relroReady) {
                Slog.w(WebViewUpdateService.TAG, "creating relro file timed out");
            }
        }
    }
}
