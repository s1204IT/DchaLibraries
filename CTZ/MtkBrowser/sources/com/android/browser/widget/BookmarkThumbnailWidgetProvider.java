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
        if ("com.android.browser.BOOKMARK_APPWIDGET_UPDATE".equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            performUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(getComponentName(context)));
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        performUpdate(context, appWidgetManager, iArr);
    }

    @Override
    public void onDeleted(Context context, int[] iArr) {
        super.onDeleted(context, iArr);
        for (int i : iArr) {
            BookmarkThumbnailWidgetService.deleteWidgetState(context, i);
        }
        removeOrphanedFiles(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        removeOrphanedFiles(context);
    }

    void removeOrphanedFiles(Context context) {
        BookmarkThumbnailWidgetService.removeOrphanedStates(context, AppWidgetManager.getInstance(context).getAppWidgetIds(getComponentName(context)));
    }

    private void performUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        PendingIntent activity = PendingIntent.getActivity(context, 0, new Intent("show_browser", null, context, BrowserActivity.class), 134217728);
        for (int i : iArr) {
            Intent intent = new Intent(context, (Class<?>) BookmarkThumbnailWidgetService.class);
            intent.putExtra("appWidgetId", i);
            intent.setData(Uri.parse(intent.toUri(1)));
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bookmarkthumbnailwidget);
            remoteViews.setOnClickPendingIntent(R.id.app_shortcut, activity);
            remoteViews.setRemoteAdapter(R.id.bookmarks_list, intent);
            appWidgetManager.notifyAppWidgetViewDataChanged(i, R.id.bookmarks_list);
            remoteViews.setPendingIntentTemplate(R.id.bookmarks_list, PendingIntent.getBroadcast(context, 0, new Intent(context, (Class<?>) BookmarkWidgetProxy.class), 134217728));
            appWidgetManager.updateAppWidget(i, remoteViews);
        }
    }

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, (Class<?>) BookmarkThumbnailWidgetProvider.class);
    }

    public static void refreshWidgets(Context context) {
        context.sendBroadcast(new Intent("com.android.browser.BOOKMARK_APPWIDGET_UPDATE", null, context, BookmarkThumbnailWidgetProvider.class));
    }
}
