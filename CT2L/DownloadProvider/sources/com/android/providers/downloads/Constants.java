package com.android.providers.downloads;

import android.os.Build;
import android.text.TextUtils;

public class Constants {
    public static final String DEFAULT_USER_AGENT;
    public static final boolean LOGV;
    public static final boolean LOGVV;

    static {
        StringBuilder builder = new StringBuilder();
        boolean validRelease = !TextUtils.isEmpty(Build.VERSION.RELEASE);
        boolean validId = !TextUtils.isEmpty(Build.ID);
        boolean includeModel = "REL".equals(Build.VERSION.CODENAME) && !TextUtils.isEmpty(Build.MODEL);
        builder.append("AndroidDownloadManager");
        if (validRelease) {
            builder.append("/").append(Build.VERSION.RELEASE);
        }
        builder.append(" (Linux; U; Android");
        if (validRelease) {
            builder.append(" ").append(Build.VERSION.RELEASE);
        }
        if (includeModel || validId) {
            builder.append(";");
            if (includeModel) {
                builder.append(" ").append(Build.MODEL);
            }
            if (validId) {
                builder.append(" Build/").append(Build.ID);
            }
        }
        builder.append(")");
        DEFAULT_USER_AGENT = builder.toString();
        LOGV = false;
        LOGVV = false;
    }
}
