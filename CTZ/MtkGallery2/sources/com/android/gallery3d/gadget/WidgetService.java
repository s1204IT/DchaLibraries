package com.android.gallery3d.gadget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.data.MediaItem;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.util.PermissionHelper;
import com.mediatek.gallerybasic.base.MediaFilter;
import com.mediatek.gallerybasic.base.MediaFilterSetting;
import com.mediatek.omadrm.OmaDrmStore;

@TargetApi(11)
public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new PhotoRVFactory((GalleryApp) getApplicationContext(), intent.getIntExtra("appWidgetId", 0), intent.getIntExtra("widget-type", 0), intent.getStringExtra("album-path"));
    }

    private class PhotoRVFactory implements RemoteViewsService.RemoteViewsFactory, ContentListener {
        private final String mAlbumPath;
        private final GalleryApp mApp;
        private final int mAppWidgetId;
        private WidgetSource mSource;
        private final int mType;
        private boolean mLastPermissionStatus = true;
        private HandlerThread mHandlerThread = null;
        private Handler mHandler = null;
        private long mStartTime = -1;

        public PhotoRVFactory(GalleryApp galleryApp, int i, int i2, String str) {
            this.mApp = galleryApp;
            this.mAppWidgetId = i;
            this.mType = i2;
            this.mAlbumPath = str;
        }

        @Override
        public void onCreate() {
            Log.d("Gallery2/GalleryAppWidgetService", "<PhotoRVFactory.onCreate>");
            ImageCacheService.setCacheName("imagecacheforwidget");
            PhotoPlayFacade.initialize((GalleryAppImpl) WidgetService.this.getApplication(), MediaItem.getTargetSize(2), MediaItem.getTargetSize(1), MediaItem.getTargetSize(4));
            PhotoPlayFacade.registerWidgetMedias(this.mApp.getAndroidContext());
            MediaFilter mediaFilter = new MediaFilter();
            Intent intent = new Intent();
            intent.putExtra(OmaDrmStore.DrmIntentExtra.EXTRA_DRM_LEVEL, 1);
            mediaFilter.setFlagFromIntent(intent);
            MediaFilterSetting.setCurrentFilter(WidgetService.this, mediaFilter);
            initHandler();
            if (this.mType == 2) {
                this.mSource = new MediaSetSource(this.mApp.getDataManager(), this.mAlbumPath);
            } else {
                this.mSource = new LocalPhotoSource(this.mApp.getAndroidContext());
            }
            this.mSource.setContentListener(this);
            onContentDirty();
        }

        @Override
        public void onDestroy() {
            Log.d("Gallery2/GalleryAppWidgetService", "<PhotoRVFactory.onDestroy>");
            MediaFilterSetting.removeFilter(WidgetService.this);
            closeHandler();
            this.mSource.close();
            this.mSource = null;
        }

        @Override
        public int getCount() {
            if (this.mSource == null) {
                Log.d("Gallery2/GalleryAppWidgetService", "<PhotoRVFactory.getCount> mSource is null, return 0");
                return 0;
            }
            Log.d("Gallery2/GalleryAppWidgetService", "<PhotoRVFactory.getCount> count=" + this.mSource.size());
            return this.mSource.size();
        }

        @Override
        public long getItemId(int i) {
            return i;
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
            RemoteViews remoteViews = new RemoteViews(this.mApp.getAndroidContext().getPackageName(), R.layout.appwidget_loading_item);
            remoteViews.setProgressBar(R.id.appwidget_loading_item, 0, 0, true);
            return remoteViews;
        }

        @Override
        public RemoteViews getViewAt(int i) {
            Log.d("Gallery2/GalleryAppWidgetService", "<PhotoRVFactory.getViewAt> " + i);
            Uri contentUri = this.mSource.getContentUri(i);
            if (contentUri == null) {
                Log.d("Gallery2/GalleryAppWidgetService", "<PhotoRVFactory.getViewAt> uri is null, return loading view");
                return getLoadingView();
            }
            Bitmap image = this.mSource.getImage(i);
            if (image == null) {
                Log.d("Gallery2/GalleryAppWidgetService", "<PhotoRVFactory.getViewAt> bitmap is null, return loading view");
                return getLoadingView();
            }
            RemoteViews remoteViews = new RemoteViews(this.mApp.getAndroidContext().getPackageName(), R.layout.appwidget_photo_item);
            remoteViews.setImageViewBitmap(R.id.appwidget_photo_item, image);
            remoteViews.setOnClickFillInIntent(R.id.appwidget_photo_item, new Intent().setFlags(32768).setData(contentUri));
            return remoteViews;
        }

        @Override
        public void onDataSetChanged() {
            Log.d("Gallery2/GalleryAppWidgetService", "<onDataSetChanged>");
            boolean zCheckStoragePermission = PermissionHelper.checkStoragePermission(WidgetService.this);
            if (this.mLastPermissionStatus != zCheckStoragePermission && this.mSource != null) {
                Log.d("Gallery2/GalleryAppWidgetService", "<onDataSetChanged> forceNotifyDirty");
                this.mSource.forceNotifyDirty();
            }
            this.mLastPermissionStatus = zCheckStoragePermission;
        }

        @Override
        public void onContentDirty() {
            Log.d("Gallery2/GalleryAppWidgetService", "<onContentDirty> send MSG_CONTENT_DIRTY");
            this.mHandler.sendEmptyMessage(1);
        }

        private void initHandler() {
            this.mHandlerThread = new HandlerThread("WidgetService-HandlerThread", 10);
            this.mHandlerThread.start();
            this.mHandler = new ContentDirtyHandler(this.mHandlerThread.getLooper());
        }

        private void closeHandler() {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quit();
                this.mHandlerThread = null;
            }
            this.mHandler = null;
        }

        private class ContentDirtyHandler extends Handler {
            private Message mLastAbandonMsg;
            private long mLastAbandonMsgId;
            private long mLastReceiveMsgId;

            public ContentDirtyHandler(Looper looper) {
                super(looper);
                this.mLastAbandonMsgId = 0L;
                this.mLastReceiveMsgId = 0L;
            }

            @Override
            public void dispatchMessage(Message message) {
                if (message.what == -1) {
                    if (this.mLastAbandonMsgId == this.mLastReceiveMsgId && this.mLastAbandonMsg != null) {
                        sendMessage(this.mLastAbandonMsg);
                        this.mLastAbandonMsg = null;
                        return;
                    }
                    return;
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                long j = jCurrentTimeMillis - PhotoRVFactory.this.mStartTime;
                this.mLastReceiveMsgId++;
                if (j >= 1000) {
                    PhotoRVFactory.this.mStartTime = jCurrentTimeMillis;
                    super.dispatchMessage(message);
                } else {
                    this.mLastAbandonMsgId = this.mLastReceiveMsgId;
                    this.mLastAbandonMsg = Message.obtain(message);
                    sendMessageDelayed(Message.obtain(this, -1), 1000L);
                }
            }

            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    Log.d("Gallery2/GalleryAppWidgetService", "<handleMessage> mSource = " + PhotoRVFactory.this.mSource + ", reload");
                    if (PhotoRVFactory.this.mSource != null) {
                        PhotoRVFactory.this.mSource.reload();
                    }
                    Log.d("Gallery2/GalleryAppWidgetService", "<handleMessage> notifyAppWidgetViewDataChanged");
                    AppWidgetManager.getInstance(PhotoRVFactory.this.mApp.getAndroidContext()).notifyAppWidgetViewDataChanged(PhotoRVFactory.this.mAppWidgetId, R.id.appwidget_stack_view);
                }
            }
        }
    }
}
