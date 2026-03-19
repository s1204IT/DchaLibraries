package com.android.providers.downloads;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.util.Log;

public class RawDocumentsHelper {
    public static boolean isRawDocId(String str) {
        return str != null && str.startsWith("raw:");
    }

    public static boolean startViewIntent(Context context, Uri uri) {
        if (BenesseExtension.getDchaState() != 0) {
            return true;
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(uri);
        intent.setFlags(3);
        intent.putExtra("android.intent.extra.ORIGINATING_UID", -1);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w("DownloadManager", "Failed to start " + intent + ": " + e);
            return false;
        }
    }
}
