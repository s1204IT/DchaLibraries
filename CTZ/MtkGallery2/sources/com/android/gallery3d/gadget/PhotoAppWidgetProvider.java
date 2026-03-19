package com.android.gallery3d.gadget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.RemoteViews;
import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;
import com.android.gallery3d.onetimeinitializer.GalleryWidgetMigrator;
import com.mediatek.gallery3d.util.Log;

public class PhotoAppWidgetProvider extends AppWidgetProvider {
    static RemoteViews buildWidget(Context context, int i, WidgetDatabaseHelper.Entry entry) {
        switch (entry.type) {
            case 0:
                return buildFrameWidget(context, i, entry);
            case 1:
            case 2:
                return buildStackWidget(context, i, entry);
            default:
                throw new RuntimeException("invalid type - " + entry.type);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        if (ApiHelper.HAS_REMOTE_VIEWS_SERVICE) {
            GalleryWidgetMigrator.migrateGalleryWidgets(context);
        }
        WidgetDatabaseHelper widgetDatabaseHelper = new WidgetDatabaseHelper(context);
        try {
            for (int i : iArr) {
                WidgetDatabaseHelper.Entry entry = widgetDatabaseHelper.getEntry(i);
                StringBuilder sb = new StringBuilder();
                sb.append(" <onUpdate>: entry for id[");
                sb.append(i);
                sb.append("]=");
                sb.append(entry == null ? "null" : "(" + entry.type + ", " + entry.imageUri + ", " + entry.albumPath + ", " + entry.imageData + ")");
                Log.d("Gallery2/PhotoAppWidgetProvider", sb.toString());
                if (entry != null) {
                    appWidgetManager.updateAppWidget(i, buildWidget(context, i, entry));
                } else {
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget_main);
                    remoteViews.setEmptyView(R.id.appwidget_stack_view, R.id.appwidget_empty_view);
                    appWidgetManager.updateAppWidget(i, remoteViews);
                    Log.e("Gallery2/PhotoAppWidgetProvider", "<onUpdate>cannot load widget: " + i);
                }
            }
            widgetDatabaseHelper.close();
            super.onUpdate(context, appWidgetManager, iArr);
        } catch (Throwable th) {
            widgetDatabaseHelper.close();
            throw th;
        }
    }

    @TargetApi(11)
    private static RemoteViews buildStackWidget(Context context, int i, WidgetDatabaseHelper.Entry entry) {
        Log.d("Gallery2/PhotoAppWidgetProvider", "<buildStackWidget> for id=" + i + ", entry=(" + entry.type + ", " + entry.imageUri + ", " + entry.albumPath + ", " + entry.imageData + ")");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget_main);
        Intent intent = new Intent(context, (Class<?>) WidgetService.class);
        intent.putExtra("appWidgetId", i);
        intent.putExtra("widget-type", entry.type);
        intent.putExtra("album-path", entry.albumPath);
        StringBuilder sb = new StringBuilder();
        sb.append("widget://gallery/");
        sb.append(i);
        intent.setData(Uri.parse(sb.toString()));
        remoteViews.setRemoteAdapter(i, R.id.appwidget_stack_view, intent);
        remoteViews.setEmptyView(R.id.appwidget_stack_view, R.id.appwidget_empty_view);
        remoteViews.setPendingIntentTemplate(R.id.appwidget_stack_view, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) WidgetClickHandler.class), 134217728));
        Intent intent2 = new Intent(context, (Class<?>) WidgetClickHandler.class);
        intent2.putExtra("on_click_from_empty_view", true);
        intent2.putExtra("widget_id", i);
        remoteViews.setOnClickPendingIntent(R.id.appwidget_empty_view, PendingIntent.getActivity(context, 0, intent2, 134217728));
        return remoteViews;
    }

    static RemoteViews buildFrameWidget(Context context, int i, WidgetDatabaseHelper.Entry entry) {
        Log.d("Gallery2/PhotoAppWidgetProvider", "<buildFrameWidget> for id=" + i + ", entry=(" + entry.type + ", " + entry.imageUri + ", " + entry.albumPath + ", " + entry.imageData + ")");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.photo_frame);
        try {
            byte[] bArr = entry.imageData;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            remoteViews.setImageViewBitmap(R.id.photo, BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options));
        } catch (Throwable th) {
            Log.w("Gallery2/PhotoAppWidgetProvider", "<buildFrameWidget> cannot load widget image: " + i, th);
        }
        if (entry.imageUri != null) {
            try {
                Intent data = new Intent(context, (Class<?>) WidgetClickHandler.class).setData(Uri.parse(entry.imageUri));
                data.setFlags(32768);
                Log.d("Gallery2/PhotoAppWidgetProvider", "<buildFrameWidget> set FLAG_ACTIVITY_CLEAR_TASK..");
                remoteViews.setOnClickPendingIntent(R.id.photo, PendingIntent.getActivity(context, 0, data, 134217728));
            } catch (Throwable th2) {
                Log.w("Gallery2/PhotoAppWidgetProvider", "<buildFrameWidget>cannot load widget uri: " + i, th2);
            }
        }
        return remoteViews;
    }

    @Override
    public void onDeleted(Context context, int[] iArr) {
        WidgetDatabaseHelper widgetDatabaseHelper = new WidgetDatabaseHelper(context);
        for (int i : iArr) {
            Log.d("Gallery2/PhotoAppWidgetProvider", "<onDeleted>onDelete: id=" + i);
            widgetDatabaseHelper.deleteEntry(i);
        }
        widgetDatabaseHelper.close();
    }
}
