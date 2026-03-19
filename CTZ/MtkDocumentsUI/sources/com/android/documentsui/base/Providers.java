package com.android.documentsui.base;

import android.net.Uri;
import java.util.HashSet;
import java.util.Set;

public final class Providers {
    private static final Set<String> SYSTEM_AUTHORITIES = new HashSet<String>() {
        {
            add("com.android.externalstorage.documents");
            add("com.android.providers.downloads.documents");
            add("com.android.providers.media.documents");
            add("com.android.mtp.documents");
        }
    };

    public static boolean isArchiveUri(Uri uri) {
        return uri != null && "com.android.documentsui.archives".equals(uri.getAuthority());
    }
}
