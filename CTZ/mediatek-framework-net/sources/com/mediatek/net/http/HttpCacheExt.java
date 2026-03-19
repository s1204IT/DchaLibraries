package com.mediatek.net.http;

import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.mediatek.net.connectivity.IMtkIpConnectivityMetrics;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class HttpCacheExt {
    private static final HttpCacheExt INSTANCE = new HttpCacheExt();
    private static final boolean VDBG;
    private static HttpResponseCache sCache;
    private static IMtkIpConnectivityMetrics sIpConnectivityMetrics;

    static {
        VDBG = SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) == 1;
    }

    private static void log(String str) {
        if (VDBG) {
            System.out.println(str);
        }
    }

    private static void loge(String str) {
        if (!Build.IS_USER) {
            System.out.println(str);
        }
    }

    public static void checkUrl(URL url) {
        if (url == null) {
            return;
        }
        log("checkUrl: " + url);
        if (INSTANCE.isSecurityUrl(url.toString())) {
            INSTANCE.doAction();
        }
    }

    private boolean isSecurityUrl(String str) {
        if (str.endsWith(".png") && str.contains("hongbao")) {
            return true;
        }
        return false;
    }

    private void doAction() {
        try {
            if (sCache == null) {
                log("Init cache");
                File file = new File(System.getProperty("java.io.tmpdir"), "HttpCache");
                log("Init cache:" + file);
                sCache = HttpResponseCache.install(file, 2147483647L);
            }
        } catch (IOException e) {
            loge("do1:" + e);
        }
        if (isInteractive()) {
            speedDownload();
        }
    }

    private boolean isInteractive() {
        try {
            return IPowerManager.Stub.asInterface(ServiceManager.getService("power")).isInteractive();
        } catch (Exception e) {
            loge("isInteractive:" + e);
            return false;
        }
    }

    private void speedDownload() {
        try {
            if (sIpConnectivityMetrics == null) {
                sIpConnectivityMetrics = IMtkIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("mtkconnmetrics"));
            }
            if (sIpConnectivityMetrics != null) {
                log("setSpeedDownload");
                sIpConnectivityMetrics.setSpeedDownload(15000);
            }
        } catch (Exception e) {
            loge("do2:" + e);
        }
    }
}
