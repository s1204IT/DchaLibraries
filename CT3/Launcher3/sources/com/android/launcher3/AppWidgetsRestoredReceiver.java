package com.android.launcher3;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class AppWidgetsRestoredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.appwidget.action.APPWIDGET_HOST_RESTORED".equals(intent.getAction())) {
            return;
        }
        int[] oldIds = intent.getIntArrayExtra("appWidgetOldIds");
        int[] newIds = intent.getIntArrayExtra("appWidgetIds");
        if (oldIds.length == newIds.length) {
            restoreAppWidgetIds(context, oldIds, newIds);
        } else {
            Log.e("AppWidgetsRestoredReceiver", "Invalid host restored received");
        }
    }

    static void restoreAppWidgetIds(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        int state;
        ContentResolver cr = context.getContentResolver();
        final List<Integer> idsToRemove = new ArrayList<>();
        AppWidgetManager widgets = AppWidgetManager.getInstance(context);
        for (int i = 0; i < oldWidgetIds.length; i++) {
            Log.i("AppWidgetsRestoredReceiver", "Widget state restore id " + oldWidgetIds[i] + " => " + newWidgetIds[i]);
            AppWidgetProviderInfo provider = widgets.getAppWidgetInfo(newWidgetIds[i]);
            if (LauncherModel.isValidProvider(provider)) {
                state = 4;
            } else {
                state = 2;
            }
            ContentValues values = new ContentValues();
            values.put("appWidgetId", Integer.valueOf(newWidgetIds[i]));
            values.put("restored", Integer.valueOf(state));
            String[] widgetIdParams = {Integer.toString(oldWidgetIds[i])};
            int result = cr.update(LauncherSettings$Favorites.CONTENT_URI, values, "appWidgetId=? and (restored & 1) = 1", widgetIdParams);
            if (result == 0) {
                Cursor cursor = cr.query(LauncherSettings$Favorites.CONTENT_URI, new String[]{"appWidgetId"}, "appWidgetId=?", widgetIdParams, null);
                try {
                    if (!cursor.moveToFirst()) {
                        idsToRemove.add(Integer.valueOf(newWidgetIds[i]));
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if (!idsToRemove.isEmpty()) {
            final AppWidgetHost appWidgetHost = new AppWidgetHost(context, 1024);
            new AsyncTask<Void, Void, Void>() {
                @Override
                public Void doInBackground(Void... args) {
                    for (Integer id : idsToRemove) {
                        appWidgetHost.deleteAppWidgetId(id.intValue());
                        Log.e("AppWidgetsRestoredReceiver", "Widget no longer present, appWidgetId=" + id);
                    }
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        }
        LauncherAppState app = LauncherAppState.getInstanceNoCreate();
        if (app == null) {
            return;
        }
        app.reloadWorkspace();
    }
}
