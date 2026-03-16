package com.android.gallery3d.gadget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.ContentListener;

@TargetApi(11)
public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        int id = intent.getIntExtra("appWidgetId", 0);
        int type = intent.getIntExtra("widget-type", 0);
        String albumPath = intent.getStringExtra("album-path");
        return new PhotoRVFactory((GalleryApp) getApplicationContext(), id, type, albumPath);
    }

    private static class PhotoRVFactory implements RemoteViewsService.RemoteViewsFactory, ContentListener {
        private final String mAlbumPath;
        private final GalleryApp mApp;
        private final int mAppWidgetId;
        private WidgetSource mSource;
        private final int mType;

        public PhotoRVFactory(GalleryApp app, int id, int type, String albumPath) {
            this.mApp = app;
            this.mAppWidgetId = id;
            this.mType = type;
            this.mAlbumPath = albumPath;
        }

        @Override
        public void onCreate() {
            if (this.mType == 2) {
                this.mSource = new MediaSetSource(this.mApp.getDataManager(), this.mAlbumPath);
            } else {
                this.mSource = new LocalPhotoSource(this.mApp.getAndroidContext());
            }
            this.mSource.setContentListener(this);
            AppWidgetManager.getInstance(this.mApp.getAndroidContext()).notifyAppWidgetViewDataChanged(this.mAppWidgetId, R.id.appwidget_stack_view);
        }

        @Override
        public void onDestroy() {
            this.mSource.close();
            this.mSource = null;
        }

        @Override
        public int getCount() {
            return this.mSource.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public RemoteViews getLoadingView() {
            RemoteViews rv = new RemoteViews(this.mApp.getAndroidContext().getPackageName(), R.layout.appwidget_loading_item);
            rv.setProgressBar(R.id.appwidget_loading_item, 0, 0, true);
            return rv;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Bitmap bitmap = this.mSource.getImage(position);
            if (bitmap == null) {
                return getLoadingView();
            }
            RemoteViews views = new RemoteViews(this.mApp.getAndroidContext().getPackageName(), R.layout.appwidget_photo_item);
            views.setImageViewBitmap(R.id.appwidget_photo_item, bitmap);
            views.setOnClickFillInIntent(R.id.appwidget_photo_item, new Intent().setFlags(67108864).setData(this.mSource.getContentUri(position)));
            return views;
        }

        @Override
        public void onDataSetChanged() {
            this.mSource.reload();
        }

        @Override
        public void onContentDirty() {
            AppWidgetManager.getInstance(this.mApp.getAndroidContext()).notifyAppWidgetViewDataChanged(this.mAppWidgetId, R.id.appwidget_stack_view);
        }
    }
}
