package com.android.gallery3d.gadget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;
import com.android.gallery3d.onetimeinitializer.GalleryWidgetMigrator;

public class PhotoAppWidgetProvider extends AppWidgetProvider {
    static RemoteViews buildWidget(Context context, int id, WidgetDatabaseHelper.Entry entry) {
        switch (entry.type) {
            case 0:
                return buildFrameWidget(context, id, entry);
            case 1:
            case 2:
                return buildStackWidget(context, id, entry);
            default:
                throw new RuntimeException("invalid type - " + entry.type);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (ApiHelper.HAS_REMOTE_VIEWS_SERVICE) {
            GalleryWidgetMigrator.migrateGalleryWidgets(context);
        }
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(context);
        try {
            for (int id : appWidgetIds) {
                WidgetDatabaseHelper.Entry entry = helper.getEntry(id);
                if (entry != null) {
                    RemoteViews views = buildWidget(context, id, entry);
                    appWidgetManager.updateAppWidget(id, views);
                } else {
                    Log.e("WidgetProvider", "cannot load widget: " + id);
                }
            }
            helper.close();
            super.onUpdate(context, appWidgetManager, appWidgetIds);
        } catch (Throwable th) {
            helper.close();
            throw th;
        }
    }

    @TargetApi(11)
    private static RemoteViews buildStackWidget(Context context, int widgetId, WidgetDatabaseHelper.Entry entry) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_main);
        Intent intent = new Intent(context, (Class<?>) WidgetService.class);
        intent.putExtra("appWidgetId", widgetId);
        intent.putExtra("widget-type", entry.type);
        intent.putExtra("album-path", entry.albumPath);
        intent.setData(Uri.parse("widget://gallery/" + widgetId));
        views.setRemoteAdapter(widgetId, R.id.appwidget_stack_view, intent);
        views.setEmptyView(R.id.appwidget_stack_view, R.id.appwidget_empty_view);
        Intent clickIntent = new Intent(context, (Class<?>) WidgetClickHandler.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, clickIntent, 134217728);
        views.setPendingIntentTemplate(R.id.appwidget_stack_view, pendingIntent);
        return views;
    }

    static RemoteViews buildFrameWidget(Context context, int appWidgetId, WidgetDatabaseHelper.Entry entry) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.photo_frame);
        try {
            byte[] data = entry.imageData;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            views.setImageViewBitmap(R.id.photo, bitmap);
        } catch (Throwable t) {
            Log.w("WidgetProvider", "cannot load widget image: " + appWidgetId, t);
        }
        if (entry.imageUri != null) {
            try {
                Uri uri = Uri.parse(entry.imageUri);
                Intent clickIntent = new Intent(context, (Class<?>) WidgetClickHandler.class).setData(uri);
                PendingIntent pendingClickIntent = PendingIntent.getActivity(context, 0, clickIntent, 268435456);
                views.setOnClickPendingIntent(R.id.photo, pendingClickIntent);
            } catch (Throwable t2) {
                Log.w("WidgetProvider", "cannot load widget uri: " + appWidgetId, t2);
            }
        }
        return views;
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(context);
        for (int appWidgetId : appWidgetIds) {
            helper.deleteEntry(appWidgetId);
        }
        helper.close();
    }
}
