package com.android.providers.downloads;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.BenesseExtension;
import android.provider.Downloads;
import android.util.Log;
import java.io.File;

public class OpenHelper {
    public static boolean startViewIntent(Context context, long id, int intentFlags) {
        Intent intent = buildViewIntent(context, id);
        if (intent == null) {
            Log.w("DownloadManager", "No intent built for " + id);
            return false;
        }
        intent.addFlags(intentFlags);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w("DownloadManager", "Failed to start " + intent + ": " + e);
            return false;
        }
    }

    private static Intent buildViewIntent(Context context, long id) {
        DownloadManager downManager = (DownloadManager) context.getSystemService("download");
        downManager.setAccessAllDownloads(true);
        Cursor cursor = downManager.query(new DownloadManager.Query().setFilterById(id));
        try {
            if (cursor.moveToFirst() && BenesseExtension.getDchaState() == 0) {
                Uri localUri = getCursorUri(cursor, "local_uri");
                File file = getCursorFile(cursor, "local_filename");
                String mimeType = DownloadDrmHelper.getOriginalMimeType(context, file, getCursorString(cursor, "media_type"));
                Intent intent = new Intent("android.intent.action.VIEW");
                if ("application/vnd.android.package-archive".equals(mimeType)) {
                    intent.setDataAndType(localUri, mimeType);
                    Uri remoteUri = getCursorUri(cursor, "uri");
                    intent.putExtra("android.intent.extra.ORIGINATING_URI", remoteUri);
                    intent.putExtra("android.intent.extra.REFERRER", getRefererUri(context, id));
                    intent.putExtra("android.intent.extra.ORIGINATING_UID", getOriginatingUid(context, id));
                } else if ("file".equals(localUri.getScheme())) {
                    intent.setFlags(3);
                    intent.setDataAndType(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id), mimeType);
                } else {
                    intent.setFlags(1);
                    intent.setDataAndType(localUri, mimeType);
                }
                return intent;
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    private static Uri getRefererUri(Context context, long id) {
        Uri cursorUri = null;
        Uri headersUri = Uri.withAppendedPath(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id), "headers");
        Cursor headers = context.getContentResolver().query(headersUri, null, null, null, null);
        while (true) {
            try {
                if (!headers.moveToNext()) {
                    break;
                }
                String header = getCursorString(headers, "header");
                if ("Referer".equalsIgnoreCase(header)) {
                    break;
                }
            } finally {
                headers.close();
            }
        }
        return cursorUri;
    }

    private static int getOriginatingUid(Context context, long id) {
        Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"uid"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(cursor.getColumnIndexOrThrow("uid"));
                }
            } finally {
                cursor.close();
            }
        }
        return -1;
    }

    private static String getCursorString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }

    private static Uri getCursorUri(Cursor cursor, String column) {
        return Uri.parse(getCursorString(cursor, column));
    }

    private static File getCursorFile(Cursor cursor, String column) {
        return new File(cursor.getString(cursor.getColumnIndexOrThrow(column)));
    }
}
