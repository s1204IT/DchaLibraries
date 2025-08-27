package com.mediatek.apn;

/* loaded from: classes.dex */
public class ApnUtils {
    public static final String[] PROJECTION = {"_id", "name", "apn", "proxy", "port", "user", "server", "password", "mmsc", "mcc", "mnc", "numeric", "mmsproxy", "mmsport", "authtype", "type", "sourcetype", "omacpid", "napid", "proxyid"};

    public static String checkNotSet(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        return str;
    }
}
