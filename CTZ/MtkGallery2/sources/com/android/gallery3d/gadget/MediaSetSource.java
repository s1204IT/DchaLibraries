package com.android.gallery3d.gadget;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

public class MediaSetSource implements ContentListener, WidgetSource {
    private Path mAlbumPath;
    private DataManager mDataManager;
    private ContentListener mListener;
    private MediaSet mRootSet;
    private WidgetSource mSource;

    public MediaSetSource(DataManager dataManager, String str) {
        MediaSet mediaSet = (MediaSet) dataManager.getMediaObject(str);
        if (mediaSet != null) {
            this.mSource = new CheckedMediaSetSource(mediaSet);
            return;
        }
        this.mDataManager = (DataManager) Utils.checkNotNull(dataManager);
        this.mAlbumPath = Path.fromString(str);
        this.mSource = new EmptySource();
        monitorRootPath();
    }

    @Override
    public int size() {
        return this.mSource.size();
    }

    @Override
    public Bitmap getImage(int i) {
        return this.mSource.getImage(i);
    }

    @Override
    public Uri getContentUri(int i) {
        return this.mSource.getContentUri(i);
    }

    @Override
    public synchronized void setContentListener(ContentListener contentListener) {
        if (this.mRootSet != null) {
            this.mListener = contentListener;
        } else {
            this.mSource.setContentListener(contentListener);
        }
    }

    @Override
    public void reload() {
        this.mSource.reload();
    }

    @Override
    public void close() {
        this.mSource.close();
    }

    @Override
    public void onContentDirty() {
        resolveAlbumPath();
    }

    private void monitorRootPath() {
        this.mRootSet = (MediaSet) this.mDataManager.getMediaObject(this.mDataManager.getTopSetPath(3));
        this.mRootSet.addContentListener(this);
    }

    private synchronized void resolveAlbumPath() {
        if (this.mDataManager == null) {
            return;
        }
        MediaSet mediaSet = (MediaSet) this.mDataManager.getMediaObject(this.mAlbumPath);
        if (mediaSet != null) {
            this.mRootSet = null;
            this.mSource = new CheckedMediaSetSource(mediaSet);
            if (this.mListener != null) {
                this.mListener.onContentDirty();
                this.mSource.setContentListener(this.mListener);
                this.mListener = null;
            }
            this.mDataManager = null;
            this.mAlbumPath = null;
        }
    }

    private static class CheckedMediaSetSource implements ContentListener, WidgetSource {
        private int mCacheEnd;
        private int mCacheStart;
        private ContentListener mContentListener;
        private MediaSet mSource;
        private MediaItem[] mCache = new MediaItem[32];
        private long mSourceVersion = -1;

        public CheckedMediaSetSource(MediaSet mediaSet) {
            this.mSource = (MediaSet) Utils.checkNotNull(mediaSet);
            this.mSource.addContentListener(this);
        }

        @Override
        public void close() {
            this.mSource.removeContentListener(this);
        }

        private void ensureCacheRange(int i) {
            if (i < this.mCacheStart || i >= this.mCacheEnd) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mCacheStart = i;
                    ArrayList<MediaItem> mediaItem = this.mSource.getMediaItem(this.mCacheStart, 32);
                    this.mCacheEnd = this.mCacheStart + mediaItem.size();
                    mediaItem.toArray(this.mCache);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        @Override
        public synchronized Uri getContentUri(int i) {
            ensureCacheRange(i);
            if (i >= this.mCacheStart && i < this.mCacheEnd) {
                return this.mCache[i - this.mCacheStart].getContentUri();
            }
            Log.e("Gallery2/CheckedMediaSetSource", "getContentUri: index out of range: " + i + ", start=" + this.mCacheStart + ", end=" + this.mCacheEnd);
            return null;
        }

        @Override
        public synchronized Bitmap getImage(int i) {
            ensureCacheRange(i);
            if (i >= this.mCacheStart && i < this.mCacheEnd) {
                MediaItem mediaItem = this.mCache[i - this.mCacheStart];
                StringBuilder sb = new StringBuilder();
                sb.append("getImage: mediaitem=");
                sb.append(mediaItem == null ? "null" : mediaItem.getName());
                Log.d("Gallery2/CheckedMediaSetSource", sb.toString());
                return WidgetUtils.createWidgetBitmap(this.mCache[i - this.mCacheStart]);
            }
            Log.e("Gallery2/CheckedMediaSetSource", "getImage: index out of range: " + i + ", start=" + this.mCacheStart + ", end=" + this.mCacheEnd);
            return null;
        }

        @Override
        public void reload() {
            long jReload = this.mSource.reload();
            if (this.mSourceVersion != jReload) {
                Log.d("Gallery2/CheckedMediaSetSource", "reload: new data version!");
                this.mSourceVersion = jReload;
                this.mCacheStart = 0;
                this.mCacheEnd = 0;
                Arrays.fill(this.mCache, (Object) null);
            }
        }

        @Override
        public void setContentListener(ContentListener contentListener) {
            this.mContentListener = contentListener;
        }

        @Override
        public int size() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return this.mSource.getMediaItemCount();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onContentDirty() {
            if (this.mContentListener != null) {
                this.mContentListener.onContentDirty();
            }
        }

        @Override
        public void forceNotifyDirty() {
            onContentDirty();
        }
    }

    private static class EmptySource implements WidgetSource {
        private EmptySource() {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Bitmap getImage(int i) {
            return null;
        }

        @Override
        public Uri getContentUri(int i) {
            return null;
        }

        @Override
        public void setContentListener(ContentListener contentListener) {
        }

        @Override
        public void reload() {
        }

        @Override
        public void close() {
        }

        @Override
        public void forceNotifyDirty() {
        }
    }

    @Override
    public void forceNotifyDirty() {
        onContentDirty();
    }
}
