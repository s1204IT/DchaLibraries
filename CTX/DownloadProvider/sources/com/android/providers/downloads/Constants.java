package com.android.providers.downloads;

import android.os.Build;
import android.text.TextUtils;

public class Constants {
    public static final String DEFAULT_USER_AGENT;
    public static final boolean LOGV;
    public static final boolean LOGVV;

    static {
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        boolean z2 = !TextUtils.isEmpty(Build.VERSION.RELEASE);
        boolean z3 = !TextUtils.isEmpty(Build.ID);
        if (!"REL".equals(Build.VERSION.CODENAME) || TextUtils.isEmpty(Build.MODEL)) {
            z = false;
        }
        sb.append("AndroidDownloadManager");
        if (z2) {
            sb.append("/");
            sb.append(Build.VERSION.RELEASE);
        }
        sb.append(" (Linux; U; Android");
        if (z2) {
            sb.append(" ");
            sb.append(Build.VERSION.RELEASE);
        }
        if (z || z3) {
            sb.append(";");
            if (z) {
                sb.append(" ");
                sb.append(Build.MODEL);
            }
            if (z3) {
                sb.append(" Build/");
                sb.append(Build.ID);
            }
        }
        sb.append(")");
        DEFAULT_USER_AGENT = sb.toString();
        LOGV = false;
        LOGVV = false;
    }
}
