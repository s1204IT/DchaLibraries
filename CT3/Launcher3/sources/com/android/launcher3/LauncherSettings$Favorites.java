package com.android.launcher3;

import android.net.Uri;
import android.provider.BaseColumns;
import com.android.launcher3.config.ProviderConfig;

public final class LauncherSettings$Favorites implements BaseColumns {
    public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/favorites");

    public static Uri getContentUri(long id) {
        return Uri.parse("content://" + ProviderConfig.AUTHORITY + "/favorites/" + id);
    }

    static final String containerToString(int container) {
        switch (container) {
            case -101:
                return "hotseat";
            case -100:
                return "desktop";
            default:
                return String.valueOf(container);
        }
    }
}
