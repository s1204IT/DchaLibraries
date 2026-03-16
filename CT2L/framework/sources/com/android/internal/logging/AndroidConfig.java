package com.android.internal.logging;

import android.net.ProxyInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AndroidConfig {
    public AndroidConfig() {
        try {
            Logger rootLogger = Logger.getLogger(ProxyInfo.LOCAL_EXCL_LIST);
            rootLogger.addHandler(new AndroidHandler());
            rootLogger.setLevel(Level.INFO);
            Logger.getLogger("org.apache").setLevel(Level.WARNING);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
