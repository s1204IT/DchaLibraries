package com.android.providers.downloads;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Process;
import android.provider.DocumentsContract;
import android.provider.Downloads;
import android.util.Log;
import java.io.File;

public class OpenHelper {
    public static boolean startViewIntent(Context context, long j, int i) {
        Intent intentBuildViewIntent = buildViewIntent(context, j);
        if (intentBuildViewIntent == null) {
            Log.w("DownloadManager", "No intent built for " + j);
            return false;
        }
        intentBuildViewIntent.addFlags(i);
        try {
            context.startActivity(intentBuildViewIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w("DownloadManager", "Failed to start " + intentBuildViewIntent + ": " + e);
            return false;
        }
    }

    private static Intent buildViewIntent(Context context, long j) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService("download");
        downloadManager.setAccessAllDownloads(true);
        downloadManager.setAccessFilename(true);
        Cursor cursorQuery = downloadManager.query(new DownloadManager.Query().setFilterById(j));
        try {
            if (!cursorQuery.moveToFirst()) {
                return null;
            }
            String originalMimeType = DownloadDrmHelper.getOriginalMimeType(context, getCursorFile(cursorQuery, "local_filename"), getCursorString(cursorQuery, "media_type"));
            Uri uriBuildDocumentUri = DocumentsContract.buildDocumentUri("com.android.providers.downloads.documents", String.valueOf(j));
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType(uriBuildDocumentUri, originalMimeType);
            intent.setFlags(3);
            if ("application/vnd.android.package-archive".equals(originalMimeType)) {
                intent.putExtra("android.intent.extra.ORIGINATING_URI", getCursorUri(cursorQuery, "uri"));
                intent.putExtra("android.intent.extra.REFERRER", getRefererUri(context, j));
                intent.putExtra("android.intent.extra.ORIGINATING_UID", getOriginatingUid(context, j));
            }
            return intent;
        } finally {
            cursorQuery.close();
        }
    }

    private static Uri getRefererUri(Context context, long j) {
        Cursor cursorQuery = context.getContentResolver().query(Uri.withAppendedPath(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, j), "headers"), null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                if ("Referer".equalsIgnoreCase(getCursorString(cursorQuery, "header"))) {
                    return getCursorUri(cursorQuery, "value");
                }
            } finally {
                cursorQuery.close();
            }
        }
        cursorQuery.close();
        return null;
    }

    private static int getOriginatingUid(Context context, long j) {
        Cursor cursorQuery = context.getContentResolver().query(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, j), new String[]{"uid"}, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    int i = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("uid"));
                    if (i != Process.myUid()) {
                        return i;
                    }
                }
                return -1;
            } finally {
                cursorQuery.close();
            }
        }
        return -1;
    }

    private static String getCursorString(Cursor cursor, String str) {
        return cursor.getString(cursor.getColumnIndexOrThrow(str));
    }

    private static Uri getCursorUri(Cursor cursor, String str) {
        return Uri.parse(getCursorString(cursor, str));
    }

    private static File getCursorFile(Cursor cursor, String str) {
        return new File(cursor.getString(cursor.getColumnIndexOrThrow(str)));
    }
}
