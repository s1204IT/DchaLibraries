package com.android.launcher3;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.model.LoaderTask;
import com.android.launcher3.provider.RestoreDbTask;
import com.android.launcher3.util.ContentWriter;

public class AppWidgetsRestoredReceiver extends BroadcastReceiver {
    private static final String TAG = "AWRestoredReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if ("android.appwidget.action.APPWIDGET_HOST_RESTORED".equals(intent.getAction())) {
            int intExtra = intent.getIntExtra("hostId", 0);
            Log.d(TAG, "Widget ID map received for host:" + intExtra);
            if (intExtra != 1024) {
                return;
            }
            final int[] intArrayExtra = intent.getIntArrayExtra("appWidgetOldIds");
            final int[] intArrayExtra2 = intent.getIntArrayExtra("appWidgetIds");
            if (intArrayExtra.length == intArrayExtra2.length) {
                final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
                new Handler(LauncherModel.getWorkerLooper()).postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        AppWidgetsRestoredReceiver.restoreAppWidgetIds(context, intArrayExtra, intArrayExtra2);
                        pendingResultGoAsync.finish();
                    }
                });
            } else {
                Log.e(TAG, "Invalid host restored received");
            }
        }
    }

    @WorkerThread
    static void restoreAppWidgetIds(Context context, int[] iArr, int[] iArr2) {
        int i;
        LauncherAppWidgetHost launcherAppWidgetHost = new LauncherAppWidgetHost(context);
        if (!RestoreDbTask.isPending(context)) {
            Log.e(TAG, "Skipping widget ID remap as DB already in use");
            for (int i2 : iArr2) {
                Log.d(TAG, "Deleting widgetId: " + i2);
                launcherAppWidgetHost.deleteAppWidgetId(i2);
            }
            return;
        }
        ContentResolver contentResolver = context.getContentResolver();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (int i3 = 0; i3 < iArr.length; i3++) {
            Log.i(TAG, "Widget state restore id " + iArr[i3] + " => " + iArr2[i3]);
            if (LoaderTask.isValidProvider(appWidgetManager.getAppWidgetInfo(iArr2[i3]))) {
                i = 4;
            } else {
                i = 2;
            }
            String[] strArr = {Integer.toString(iArr[i3])};
            if (new ContentWriter(context, new ContentWriter.CommitParams("appWidgetId=? and (restored & 1) = 1", strArr)).put(LauncherSettings.Favorites.APPWIDGET_ID, Integer.valueOf(iArr2[i3])).put(LauncherSettings.Favorites.RESTORED, Integer.valueOf(i)).commit() == 0) {
                Cursor cursorQuery = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{LauncherSettings.Favorites.APPWIDGET_ID}, "appWidgetId=?", strArr, null);
                try {
                    if (!cursorQuery.moveToFirst()) {
                        launcherAppWidgetHost.deleteAppWidgetId(iArr2[i3]);
                    }
                } finally {
                    cursorQuery.close();
                }
            }
        }
        LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
        if (instanceNoCreate != null) {
            instanceNoCreate.getModel().forceReload();
        }
    }
}
