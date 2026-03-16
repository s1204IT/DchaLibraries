package com.android.providers.downloads;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.widget.Toast;

public class DownloadReceiver extends BroadcastReceiver {
    private static Handler sAsyncHandler;
    SystemFacade mSystemFacade = null;

    static {
        HandlerThread thread = new HandlerThread("DownloadReceiver");
        thread.start();
        sAsyncHandler = new Handler(thread.getLooper());
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (this.mSystemFacade == null) {
            this.mSystemFacade = new RealSystemFacade(context);
        }
        String action = intent.getAction();
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            startService(context);
            return;
        }
        if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
            startService(context);
            return;
        }
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService("connectivity");
            NetworkInfo info = connManager.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                startService(context);
                return;
            }
            return;
        }
        if ("android.intent.action.UID_REMOVED".equals(action)) {
            final BroadcastReceiver.PendingResult result = goAsync();
            sAsyncHandler.post(new Runnable() {
                @Override
                public void run() {
                    DownloadReceiver.this.handleUidRemoved(context, intent);
                    result.finish();
                }
            });
            return;
        }
        if ("android.intent.action.DOWNLOAD_WAKEUP".equals(action)) {
            startService(context);
            return;
        }
        if ("android.intent.action.DOWNLOAD_OPEN".equals(action) || "android.intent.action.DOWNLOAD_LIST".equals(action) || "android.intent.action.DOWNLOAD_HIDE".equals(action)) {
            final BroadcastReceiver.PendingResult result2 = goAsync();
            if (result2 == null) {
                handleNotificationBroadcast(context, intent);
            } else {
                sAsyncHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DownloadReceiver.this.handleNotificationBroadcast(context, intent);
                        result2.finish();
                    }
                });
            }
        }
    }

    private void handleUidRemoved(Context context, Intent intent) {
        ContentResolver resolver = context.getContentResolver();
        int uid = intent.getIntExtra("android.intent.extra.UID", -1);
        int count = resolver.delete(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, "uid=" + uid, null);
        if (count > 0) {
            Slog.d("DownloadManager", "Deleted " + count + " downloads owned by UID " + uid);
        }
    }

    private void handleNotificationBroadcast(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.DOWNLOAD_LIST".equals(action)) {
            long[] ids = intent.getLongArrayExtra("extra_click_download_ids");
            sendNotificationClickedIntent(context, ids);
        } else if ("android.intent.action.DOWNLOAD_OPEN".equals(action)) {
            long id = ContentUris.parseId(intent.getData());
            openDownload(context, id);
            hideNotification(context, id);
        } else if ("android.intent.action.DOWNLOAD_HIDE".equals(action)) {
            hideNotification(context, ContentUris.parseId(intent.getData()));
        }
    }

    private void hideNotification(Context context, long id) {
        Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                int status = getInt(cursor, "status");
                int visibility = getInt(cursor, "visibility");
                cursor.close();
                if (Downloads.Impl.isStatusCompleted(status)) {
                    if (visibility == 1 || visibility == 3) {
                        ContentValues values = new ContentValues();
                        values.put("visibility", (Integer) 0);
                        context.getContentResolver().update(uri, values, null, null);
                        return;
                    }
                    return;
                }
                return;
            }
            Log.w("DownloadManager", "Missing details for download " + id);
        } finally {
            cursor.close();
        }
    }

    private void openDownload(Context context, long id) {
        if (!OpenHelper.startViewIntent(context, id, 268435456)) {
            Toast.makeText(context, R.string.download_no_application_title, 0).show();
        }
    }

    private void sendNotificationClickedIntent(Context context, long[] ids) {
        Intent appIntent;
        Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, ids[0]);
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                String packageName = getString(cursor, "notificationpackage");
                String clazz = getString(cursor, "notificationclass");
                boolean isPublicApi = getInt(cursor, "is_public_api") != 0;
                cursor.close();
                if (TextUtils.isEmpty(packageName)) {
                    Log.w("DownloadManager", "Missing package; skipping broadcast");
                    return;
                }
                if (isPublicApi) {
                    appIntent = new Intent("android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED");
                    appIntent.setPackage(packageName);
                    appIntent.putExtra("extra_click_download_ids", ids);
                } else {
                    if (TextUtils.isEmpty(clazz)) {
                        Log.w("DownloadManager", "Missing class; skipping broadcast");
                        return;
                    }
                    appIntent = new Intent("android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED");
                    appIntent.setClassName(packageName, clazz);
                    appIntent.putExtra("extra_click_download_ids", ids);
                    if (ids.length == 1) {
                        appIntent.setData(uri);
                    } else {
                        appIntent.setData(Downloads.Impl.CONTENT_URI);
                    }
                }
                this.mSystemFacade.sendBroadcast(appIntent);
                return;
            }
            Log.w("DownloadManager", "Missing details for download " + ids[0]);
        } finally {
            cursor.close();
        }
    }

    private static String getString(Cursor cursor, String col) {
        return cursor.getString(cursor.getColumnIndexOrThrow(col));
    }

    private static int getInt(Cursor cursor, String col) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(col));
    }

    private void startService(Context context) {
        context.startService(new Intent(context, (Class<?>) DownloadService.class));
    }
}
