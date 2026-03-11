package com.android.launcher2;

import android.net.Uri;
import android.provider.BaseColumns;

class LauncherSettings {

    interface BaseLauncherColumns extends BaseColumns {
    }

    static final class Favorites implements BaseLauncherColumns {
        static final Uri CONTENT_URI = Uri.parse("content://com.android.launcher2.settings/favorites?notify=true");
        static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://com.android.launcher2.settings/favorites?notify=false");

        static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://com.android.launcher2.settings/favorites/" + id + "?notify=" + notify);
        }
    }
}
