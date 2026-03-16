package com.android.music;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.widget.RemoteViews;

public class MediaAppWidgetProvider extends AppWidgetProvider {
    private static MediaAppWidgetProvider sInstance;

    static synchronized MediaAppWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        Intent updateIntent = new Intent("com.android.music.musicservicecommand");
        updateIntent.putExtra("command", "appwidgetupdate");
        updateIntent.putExtra("appWidgetIds", appWidgetIds);
        updateIntent.addFlags(1073741824);
        context.sendBroadcast(updateIntent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        Resources res = context.getResources();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.album_appwidget);
        views.setViewVisibility(R.id.title, 8);
        views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));
        linkButtons(context, views, false);
        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, getClass()), views);
        }
    }

    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
        return appWidgetIds.length > 0;
    }

    void notifyChange(MediaPlaybackService service, String what) {
        if (hasInstances(service)) {
            if ("com.android.music.metachanged".equals(what) || "com.android.music.playstatechanged".equals(what)) {
                performUpdate(service, null);
            }
        }
    }

    void performUpdate(MediaPlaybackService service, int[] appWidgetIds) {
        Resources res = service.getResources();
        RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget);
        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
        CharSequence errorState = null;
        String status = Environment.getExternalStorageState();
        if (status.equals("shared") || status.equals("unmounted")) {
            if (Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_busy_title);
            } else {
                errorState = res.getText(R.string.sdcard_busy_title_nosdcard);
            }
        } else if (status.equals("removed")) {
            if (Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_missing_title);
            } else {
                errorState = res.getText(R.string.sdcard_missing_title_nosdcard);
            }
        } else if (titleName == null) {
            errorState = res.getText(R.string.emptyplaylist);
        }
        if (errorState != null) {
            views.setViewVisibility(R.id.title, 8);
            views.setTextViewText(R.id.artist, errorState);
        } else {
            views.setViewVisibility(R.id.title, 0);
            views.setTextViewText(R.id.title, titleName);
            views.setTextViewText(R.id.artist, artistName);
        }
        boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }
        linkButtons(service, views, playing);
        pushUpdate(service, appWidgetIds, views);
    }

    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        ComponentName serviceName = new ComponentName(context, (Class<?>) MediaPlaybackService.class);
        if (playerActive) {
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) MediaPlaybackActivity.class), 0);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        } else {
            PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) MusicBrowserActivity.class), 0);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent2);
        }
        Intent intent = new Intent("com.android.music.musicservicecommand.togglepause");
        intent.setComponent(serviceName);
        PendingIntent pendingIntent3 = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent3);
        Intent intent2 = new Intent("com.android.music.musicservicecommand.next");
        intent2.setComponent(serviceName);
        PendingIntent pendingIntent4 = PendingIntent.getService(context, 0, intent2, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent4);
    }
}
