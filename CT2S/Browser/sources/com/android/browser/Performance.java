package com.android.browser;

import android.net.ParseException;
import android.net.WebAddress;
import android.os.Debug;

public class Performance {
    private static final int[] SYSTEM_CPU_FORMAT = {288, 8224, 8224, 8224, 8224, 8224, 8224, 8224};
    private static boolean mInTrace;

    static void tracePageStart(String url) {
        String host;
        if (BrowserSettings.getInstance().isTracing()) {
            try {
                WebAddress uri = new WebAddress(url);
                host = uri.getHost();
            } catch (ParseException e) {
                host = "browser";
            }
            String host2 = host.replace('.', '_') + ".trace";
            mInTrace = true;
            Debug.startMethodTracing(host2, 20971520);
        }
    }

    static void tracePageFinished() {
        if (mInTrace) {
            mInTrace = false;
            Debug.stopMethodTracing();
        }
    }
}
