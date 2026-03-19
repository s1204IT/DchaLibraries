package com.android.music;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.widget.RemoteViews;

public class MediaAppWidgetProvider extends AppWidgetProvider {
    public static boolean isAppWidget = false;
    private static MediaAppWidgetProvider sInstance;

    static synchronized MediaAppWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        MusicLogUtils.v("MusicAppWidget", "onUpdate");
        isAppWidget = true;
        Intent intent = new Intent(context, (Class<?>) MediaPlaybackService.class);
        intent.setAction("com.android.music.musicservicecommand");
        intent.putExtra("command", "appwidgetupdate");
        intent.putExtra("appWidgetIds", iArr);
        intent.addFlags(1073741824);
        context.startService(intent);
    }

    private void defaultAppWidget(Context context, int[] iArr) {
        Resources resources = context.getResources();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.album_appwidget);
        remoteViews.setViewVisibility(R.id.title, 8);
        remoteViews.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        remoteViews.setTextViewText(R.id.artist, resources.getText(R.string.widget_initial_text));
        linkButtons(context, remoteViews, false);
        pushUpdate(context, iArr, remoteViews);
    }

    private void pushUpdate(Context context, int[] iArr, RemoteViews remoteViews) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        MusicLogUtils.v("MusicAppWidget", "pushUpdate");
        if (iArr != null) {
            appWidgetManager.updateAppWidget(iArr, remoteViews);
        } else {
            appWidgetManager.updateAppWidget(new ComponentName(context, getClass()), remoteViews);
        }
    }

    private boolean hasInstances(Context context) {
        int length;
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, getClass()));
        if (appWidgetIds != null) {
            length = appWidgetIds.length;
        } else {
            length = 0;
        }
        MusicLogUtils.v("MusicAppWidget", "hasInstances number is " + length);
        return length > 0;
    }

    void notifyChange(MediaPlaybackService mediaPlaybackService, String str) {
        MusicLogUtils.v("MusicAppWidget", "notifyChange");
        if (hasInstances(mediaPlaybackService)) {
            if ("com.android.music.metachanged".equals(str) || "com.android.music.playstatechanged".equals(str) || "com.android.music.quitplayback".equals(str)) {
                performUpdate(mediaPlaybackService, null);
                return;
            }
            MusicLogUtils.v("MusicAppWidget", "notifyChange(" + str + "):discard!");
            return;
        }
        MusicLogUtils.v("MusicAppWidget", "notifyChange: no Instance");
    }

    void performUpdate(MediaPlaybackService mediaPlaybackService, int[] iArr) {
        CharSequence text;
        MusicLogUtils.v("MusicAppWidget", "performUpdate");
        Resources resources = mediaPlaybackService.getResources();
        RemoteViews remoteViews = new RemoteViews(mediaPlaybackService.getPackageName(), R.layout.album_appwidget);
        String trackName = mediaPlaybackService.getTrackName();
        String artistName = mediaPlaybackService.getArtistName();
        if ("<unknown>".equals(artistName)) {
            artistName = resources.getString(R.string.unknown_artist_name);
        }
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals("shared") || externalStorageState.equals("unmounted")) {
            text = resources.getText(R.string.sdcard_busy_title);
        } else if (externalStorageState.equals("removed")) {
            text = resources.getText(R.string.sdcard_missing_title);
        } else if (trackName == null) {
            text = resources.getText(R.string.widget_initial_text);
        } else {
            text = null;
        }
        if (text != null) {
            remoteViews.setViewVisibility(R.id.artist, 0);
            remoteViews.setViewVisibility(R.id.title, 8);
            remoteViews.setTextViewText(R.id.artist, text);
        } else if (trackName != null && trackName.toString().startsWith("http://")) {
            remoteViews.setViewVisibility(R.id.title, 0);
            remoteViews.setViewVisibility(R.id.artist, 8);
            remoteViews.setTextViewText(R.id.title, trackName);
        } else {
            remoteViews.setViewVisibility(R.id.title, 0);
            remoteViews.setViewVisibility(R.id.artist, 0);
            remoteViews.setTextViewText(R.id.title, trackName);
            remoteViews.setTextViewText(R.id.artist, artistName);
        }
        boolean zIsPlaying = mediaPlaybackService.isPlaying();
        if (zIsPlaying) {
            remoteViews.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            remoteViews.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }
        MusicLogUtils.v("MusicAppWidget", "performUpdate,Track is " + ((Object) trackName) + " Artist is " + ((Object) artistName) + " Error is " + ((Object) text) + " Playing is " + zIsPlaying);
        linkButtons(mediaPlaybackService, remoteViews, zIsPlaying);
        pushUpdate(mediaPlaybackService, iArr, remoteViews);
    }

    private void linkButtons(Context context, RemoteViews remoteViews, boolean z) {
        ComponentName componentName = new ComponentName(context, (Class<?>) MediaPlaybackService.class);
        if (z) {
            remoteViews.setOnClickPendingIntent(R.id.album_appwidget, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) MediaPlaybackActivity.class), 0));
        } else {
            remoteViews.setOnClickPendingIntent(R.id.album_appwidget, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) MusicBrowserActivity.class), 0));
        }
        Intent intent = new Intent("com.android.music.musicservicecommand.togglepause");
        intent.setComponent(componentName);
        remoteViews.setOnClickPendingIntent(R.id.control_play, PendingIntent.getService(context, 0, intent, 0));
        Intent intent2 = new Intent("com.android.music.musicservicecommand.next");
        intent2.setComponent(componentName);
        remoteViews.setOnClickPendingIntent(R.id.control_next, PendingIntent.getService(context, 0, intent2, 1073741824));
    }

    public static class PackageDataClearedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED".equals(intent.getAction())) {
                return;
            }
            String stringExtra = intent.getStringExtra("packageName");
            MusicLogUtils.v("MusicAppWidget", "PackageDataClearedReceiver recevied pkgName = " + stringExtra);
            if (stringExtra != null && stringExtra.equals(context.getPackageName())) {
                MediaAppWidgetProvider mediaAppWidgetProvider = MediaAppWidgetProvider.getInstance();
                if (mediaAppWidgetProvider != null) {
                    mediaAppWidgetProvider.defaultAppWidget(context, null);
                } else {
                    MusicLogUtils.v("MusicAppWidget", "mediaAppWidgetProvider is null ");
                }
            }
        }
    }
}
