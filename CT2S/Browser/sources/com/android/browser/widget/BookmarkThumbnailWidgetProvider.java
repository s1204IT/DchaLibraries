package com.android.browser.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import com.android.browser.BrowserActivity;
import com.android.browser.R;

public class BookmarkThumbnailWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.android.browser.BOOKMARK_APPWIDGET_UPDATE".equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            performUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(getComponentName(context)));
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mngr, int[] ids) {
        performUpdate(context, mngr, ids);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int widgetId : appWidgetIds) {
            BookmarkThumbnailWidgetService.deleteWidgetState(context, widgetId);
        }
        removeOrphanedFiles(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        removeOrphanedFiles(context);
    }

    void removeOrphanedFiles(Context context) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        int[] ids = wm.getAppWidgetIds(getComponentName(context));
        BookmarkThumbnailWidgetService.removeOrphanedStates(context, ids);
    }

    private void performUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        PendingIntent launchBrowser = PendingIntent.getActivity(context, 0, new Intent("show_browser", null, context, BrowserActivity.class), 134217728);
        for (int appWidgetId : appWidgetIds) {
            Intent updateIntent = new Intent(context, (Class<?>) BookmarkThumbnailWidgetService.class);
            updateIntent.putExtra("appWidgetId", appWidgetId);
            updateIntent.setData(Uri.parse(updateIntent.toUri(1)));
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bookmarkthumbnailwidget);
            views.setOnClickPendingIntent(R.id.app_shortcut, launchBrowser);
            views.setRemoteAdapter(R.id.bookmarks_list, updateIntent);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.bookmarks_list);
            Intent ic = new Intent(context, (Class<?>) BookmarkWidgetProxy.class);
            views.setPendingIntentTemplate(R.id.bookmarks_list, PendingIntent.getBroadcast(context, 0, ic, 134217728));
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, (Class<?>) BookmarkThumbnailWidgetProvider.class);
    }

    public static void refreshWidgets(Context context) {
        context.sendBroadcast(new Intent("com.android.browser.BOOKMARK_APPWIDGET_UPDATE", null, context, BookmarkThumbnailWidgetProvider.class));
    }
}
