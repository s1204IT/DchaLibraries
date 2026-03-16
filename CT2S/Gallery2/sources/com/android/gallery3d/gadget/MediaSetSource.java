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
import java.util.ArrayList;
import java.util.Arrays;

public class MediaSetSource implements ContentListener, WidgetSource {
    private Path mAlbumPath;
    private DataManager mDataManager;
    private ContentListener mListener;
    private MediaSet mRootSet;
    private WidgetSource mSource;

    public MediaSetSource(DataManager manager, String albumPath) {
        MediaSet mediaSet = (MediaSet) manager.getMediaObject(albumPath);
        if (mediaSet != null) {
            this.mSource = new CheckedMediaSetSource(mediaSet);
            return;
        }
        this.mDataManager = (DataManager) Utils.checkNotNull(manager);
        this.mAlbumPath = Path.fromString(albumPath);
        this.mSource = new EmptySource();
        monitorRootPath();
    }

    @Override
    public int size() {
        return this.mSource.size();
    }

    @Override
    public Bitmap getImage(int index) {
        return this.mSource.getImage(index);
    }

    @Override
    public Uri getContentUri(int index) {
        return this.mSource.getContentUri(index);
    }

    @Override
    public synchronized void setContentListener(ContentListener listener) {
        if (this.mRootSet != null) {
            this.mListener = listener;
        } else {
            this.mSource.setContentListener(listener);
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
        String rootPath = this.mDataManager.getTopSetPath(3);
        this.mRootSet = (MediaSet) this.mDataManager.getMediaObject(rootPath);
        this.mRootSet.addContentListener(this);
    }

    private synchronized void resolveAlbumPath() {
        MediaSet mediaSet;
        if (this.mDataManager != null && (mediaSet = (MediaSet) this.mDataManager.getMediaObject(this.mAlbumPath)) != null) {
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

        public CheckedMediaSetSource(MediaSet source) {
            this.mSource = (MediaSet) Utils.checkNotNull(source);
            this.mSource.addContentListener(this);
        }

        @Override
        public void close() {
            this.mSource.removeContentListener(this);
        }

        private void ensureCacheRange(int index) {
            if (index < this.mCacheStart || index >= this.mCacheEnd) {
                long token = Binder.clearCallingIdentity();
                try {
                    this.mCacheStart = index;
                    ArrayList<MediaItem> items = this.mSource.getMediaItem(this.mCacheStart, 32);
                    this.mCacheEnd = this.mCacheStart + items.size();
                    items.toArray(this.mCache);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        @Override
        public synchronized Uri getContentUri(int index) {
            ensureCacheRange(index);
            return (index < this.mCacheStart || index >= this.mCacheEnd) ? null : this.mCache[index - this.mCacheStart].getContentUri();
        }

        @Override
        public synchronized Bitmap getImage(int index) {
            ensureCacheRange(index);
            return (index < this.mCacheStart || index >= this.mCacheEnd) ? null : WidgetUtils.createWidgetBitmap(this.mCache[index - this.mCacheStart]);
        }

        @Override
        public void reload() {
            long version = this.mSource.reload();
            if (this.mSourceVersion != version) {
                this.mSourceVersion = version;
                this.mCacheStart = 0;
                this.mCacheEnd = 0;
                Arrays.fill(this.mCache, (Object) null);
            }
        }

        @Override
        public void setContentListener(ContentListener listener) {
            this.mContentListener = listener;
        }

        @Override
        public int size() {
            long token = Binder.clearCallingIdentity();
            try {
                return this.mSource.getMediaItemCount();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void onContentDirty() {
            if (this.mContentListener != null) {
                this.mContentListener.onContentDirty();
            }
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
        public Bitmap getImage(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uri getContentUri(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContentListener(ContentListener listener) {
        }

        @Override
        public void reload() {
        }

        @Override
        public void close() {
        }
    }
}
