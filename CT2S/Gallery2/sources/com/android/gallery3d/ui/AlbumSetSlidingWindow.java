package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetDataLoader;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataSourceType;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TextureUploader;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;

public class AlbumSetSlidingWindow implements AlbumSetDataLoader.DataListener {
    private final TiledTexture.Uploader mContentUploader;
    private final AlbumSetEntry[] mData;
    private final SynchronizedHandler mHandler;
    private final AlbumLabelMaker mLabelMaker;
    private final TextureUploader mLabelUploader;
    private Listener mListener;
    private BitmapTexture mLoadingLabel;
    private final String mLoadingText;
    private int mSize;
    private int mSlotWidth;
    private final AlbumSetDataLoader mSource;
    private final ThreadPool mThreadPool;
    private int mContentStart = 0;
    private int mContentEnd = 0;
    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;

    private interface EntryUpdater {
        void updateEntry();
    }

    public interface Listener {
        void onContentChanged();

        void onSizeChanged(int i);
    }

    static int access$606(AlbumSetSlidingWindow x0) {
        int i = x0.mActiveRequestCount - 1;
        x0.mActiveRequestCount = i;
        return i;
    }

    public static class AlbumSetEntry {
        public MediaSet album;
        public TiledTexture bitmapTexture;
        public int cacheFlag;
        public int cacheStatus;
        public Texture content;
        public long coverDataVersion;
        public MediaItem coverItem;
        private BitmapLoader coverLoader;
        public boolean isWaitLoadingDisplayed;
        private BitmapLoader labelLoader;
        public BitmapTexture labelTexture;
        public int rotation;
        public long setDataVersion;
        public Path setPath;
        public int sourceType;
        public String title;
        public int totalCount;
    }

    public AlbumSetSlidingWindow(AbstractGalleryActivity activity, AlbumSetDataLoader source, AlbumSetSlotRenderer.LabelSpec labelSpec, int cacheSize) {
        source.setModelListener(this);
        this.mSource = source;
        this.mData = new AlbumSetEntry[cacheSize];
        this.mSize = source.size();
        this.mThreadPool = activity.getThreadPool();
        this.mLabelMaker = new AlbumLabelMaker(activity.getAndroidContext(), labelSpec);
        this.mLoadingText = activity.getAndroidContext().getString(R.string.loading);
        this.mContentUploader = new TiledTexture.Uploader(activity.getGLRoot());
        this.mLabelUploader = new TextureUploader(activity.getGLRoot());
        this.mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == 1);
                ((EntryUpdater) message.obj).updateEntry();
            }
        };
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public AlbumSetEntry get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
            Utils.fail("invalid slot: %s outsides (%s, %s)", Integer.valueOf(slotIndex), Integer.valueOf(this.mActiveStart), Integer.valueOf(this.mActiveEnd));
        }
        return this.mData[slotIndex % this.mData.length];
    }

    public int size() {
        return this.mSize;
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= this.mActiveStart && slotIndex < this.mActiveEnd;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart != this.mContentStart || contentEnd != this.mContentEnd) {
            if (contentStart >= this.mContentEnd || this.mContentStart >= contentEnd) {
                int n = this.mContentEnd;
                for (int i = this.mContentStart; i < n; i++) {
                    freeSlotContent(i);
                }
                this.mSource.setActiveWindow(contentStart, contentEnd);
                for (int i2 = contentStart; i2 < contentEnd; i2++) {
                    prepareSlotContent(i2);
                }
            } else {
                for (int i3 = this.mContentStart; i3 < contentStart; i3++) {
                    freeSlotContent(i3);
                }
                int n2 = this.mContentEnd;
                for (int i4 = contentEnd; i4 < n2; i4++) {
                    freeSlotContent(i4);
                }
                this.mSource.setActiveWindow(contentStart, contentEnd);
                int n3 = this.mContentStart;
                for (int i5 = contentStart; i5 < n3; i5++) {
                    prepareSlotContent(i5);
                }
                for (int i6 = this.mContentEnd; i6 < contentEnd; i6++) {
                    prepareSlotContent(i6);
                }
            }
            this.mContentStart = contentStart;
            this.mContentEnd = contentEnd;
        }
    }

    public void setActiveWindow(int start, int end) {
        if (start > end || end - start > this.mData.length || end > this.mSize) {
            Utils.fail("start = %s, end = %s, length = %s, size = %s", Integer.valueOf(start), Integer.valueOf(end), Integer.valueOf(this.mData.length), Integer.valueOf(this.mSize));
        }
        AlbumSetEntry[] data = this.mData;
        this.mActiveStart = start;
        this.mActiveEnd = end;
        int contentStart = Utils.clamp(((start + end) / 2) - (data.length / 2), 0, Math.max(0, this.mSize - data.length));
        int contentEnd = Math.min(data.length + contentStart, this.mSize);
        setContentWindow(contentStart, contentEnd);
        if (this.mIsActive) {
            updateTextureUploadQueue();
            updateAllImageRequests();
        }
    }

    private void requestNonactiveImages() {
        int range = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < range; i++) {
            requestImagesInSlot(this.mActiveEnd + i);
            requestImagesInSlot((this.mActiveStart - 1) - i);
        }
    }

    private void cancelNonactiveImages() {
        int range = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < range; i++) {
            cancelImagesInSlot(this.mActiveEnd + i);
            cancelImagesInSlot((this.mActiveStart - 1) - i);
        }
    }

    private void requestImagesInSlot(int slotIndex) {
        if (slotIndex >= this.mContentStart && slotIndex < this.mContentEnd) {
            AlbumSetEntry entry = this.mData[slotIndex % this.mData.length];
            if (entry.coverLoader != null) {
                entry.coverLoader.startLoad();
            }
            if (entry.labelLoader != null) {
                entry.labelLoader.startLoad();
            }
        }
    }

    private void cancelImagesInSlot(int slotIndex) {
        if (slotIndex >= this.mContentStart && slotIndex < this.mContentEnd) {
            AlbumSetEntry entry = this.mData[slotIndex % this.mData.length];
            if (entry.coverLoader != null) {
                entry.coverLoader.cancelLoad();
            }
            if (entry.labelLoader != null) {
                entry.labelLoader.cancelLoad();
            }
        }
    }

    private static long getDataVersion(MediaObject object) {
        if (object == null) {
            return -1L;
        }
        return object.getDataVersion();
    }

    private void freeSlotContent(int slotIndex) {
        AlbumSetEntry entry = this.mData[slotIndex % this.mData.length];
        if (entry.coverLoader != null) {
            entry.coverLoader.recycle();
        }
        if (entry.labelLoader != null) {
            entry.labelLoader.recycle();
        }
        if (entry.labelTexture != null) {
            entry.labelTexture.recycle();
        }
        if (entry.bitmapTexture != null) {
            entry.bitmapTexture.recycle();
        }
        this.mData[slotIndex % this.mData.length] = null;
    }

    private boolean isLabelChanged(AlbumSetEntry entry, String title, int totalCount, int sourceType) {
        return (Utils.equals(entry.title, title) && entry.totalCount == totalCount && entry.sourceType == sourceType) ? false : true;
    }

    private void updateAlbumSetEntry(AlbumSetEntry entry, int slotIndex) {
        MediaSet album = this.mSource.getMediaSet(slotIndex);
        MediaItem cover = this.mSource.getCoverItem(slotIndex);
        int totalCount = this.mSource.getTotalCount(slotIndex);
        entry.album = album;
        entry.setDataVersion = getDataVersion(album);
        entry.cacheFlag = identifyCacheFlag(album);
        entry.cacheStatus = identifyCacheStatus(album);
        entry.setPath = album == null ? null : album.getPath();
        String title = album == null ? "" : Utils.ensureNotNull(album.getName());
        int sourceType = DataSourceType.identifySourceType(album);
        if (isLabelChanged(entry, title, totalCount, sourceType)) {
            entry.title = title;
            entry.totalCount = totalCount;
            entry.sourceType = sourceType;
            if (entry.labelLoader != null) {
                entry.labelLoader.recycle();
                entry.labelLoader = null;
                entry.labelTexture = null;
            }
            if (album != null) {
                entry.labelLoader = new AlbumLabelLoader(slotIndex, title, totalCount, sourceType);
            }
        }
        entry.coverItem = cover;
        if (getDataVersion(cover) != entry.coverDataVersion) {
            entry.coverDataVersion = getDataVersion(cover);
            entry.rotation = cover == null ? 0 : cover.getRotation();
            if (entry.coverLoader != null) {
                entry.coverLoader.recycle();
                entry.coverLoader = null;
                entry.bitmapTexture = null;
                entry.content = null;
            }
            if (cover != null) {
                entry.coverLoader = new AlbumCoverLoader(slotIndex, cover);
            }
        }
    }

    private void prepareSlotContent(int slotIndex) {
        AlbumSetEntry entry = new AlbumSetEntry();
        updateAlbumSetEntry(entry, slotIndex);
        this.mData[slotIndex % this.mData.length] = entry;
    }

    private static boolean startLoadBitmap(BitmapLoader loader) {
        if (loader == null) {
            return false;
        }
        loader.startLoad();
        return loader.isRequestInProgress();
    }

    private void uploadBackgroundTextureInSlot(int index) {
        if (index >= this.mContentStart && index < this.mContentEnd) {
            AlbumSetEntry entry = this.mData[index % this.mData.length];
            if (entry.bitmapTexture != null) {
                this.mContentUploader.addTexture(entry.bitmapTexture);
            }
            if (entry.labelTexture != null) {
                this.mLabelUploader.addBgTexture(entry.labelTexture);
            }
        }
    }

    private void updateTextureUploadQueue() {
        if (this.mIsActive) {
            this.mContentUploader.clear();
            this.mLabelUploader.clear();
            int n = this.mActiveEnd;
            for (int i = this.mActiveStart; i < n; i++) {
                AlbumSetEntry entry = this.mData[i % this.mData.length];
                if (entry.bitmapTexture != null) {
                    this.mContentUploader.addTexture(entry.bitmapTexture);
                }
                if (entry.labelTexture != null) {
                    this.mLabelUploader.addFgTexture(entry.labelTexture);
                }
            }
            int range = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
            for (int i2 = 0; i2 < range; i2++) {
                uploadBackgroundTextureInSlot(this.mActiveEnd + i2);
                uploadBackgroundTextureInSlot((this.mActiveStart - i2) - 1);
            }
        }
    }

    private void updateAllImageRequests() {
        this.mActiveRequestCount = 0;
        int n = this.mActiveEnd;
        for (int i = this.mActiveStart; i < n; i++) {
            AlbumSetEntry entry = this.mData[i % this.mData.length];
            if (startLoadBitmap(entry.coverLoader)) {
                this.mActiveRequestCount++;
            }
            if (startLoadBitmap(entry.labelLoader)) {
                this.mActiveRequestCount++;
            }
        }
        if (this.mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }

    @Override
    public void onSizeChanged(int size) {
        if (this.mIsActive && this.mSize != size) {
            this.mSize = size;
            if (this.mListener != null) {
                this.mListener.onSizeChanged(this.mSize);
            }
            if (this.mContentEnd > this.mSize) {
                this.mContentEnd = this.mSize;
            }
            if (this.mActiveEnd > this.mSize) {
                this.mActiveEnd = this.mSize;
            }
        }
    }

    @Override
    public void onContentChanged(int index) {
        if (this.mIsActive) {
            if (index < this.mContentStart || index >= this.mContentEnd) {
                Log.w("AlbumSetSlidingWindow", String.format("invalid update: %s is outside (%s, %s)", Integer.valueOf(index), Integer.valueOf(this.mContentStart), Integer.valueOf(this.mContentEnd)));
                return;
            }
            AlbumSetEntry entry = this.mData[index % this.mData.length];
            updateAlbumSetEntry(entry, index);
            updateAllImageRequests();
            updateTextureUploadQueue();
            if (this.mListener != null && isActiveSlot(index)) {
                this.mListener.onContentChanged();
            }
        }
    }

    public void pause() {
        this.mIsActive = false;
        this.mLabelUploader.clear();
        this.mContentUploader.clear();
        TiledTexture.freeResources();
        int n = this.mContentEnd;
        for (int i = this.mContentStart; i < n; i++) {
            freeSlotContent(i);
        }
    }

    public void resume() {
        this.mIsActive = true;
        TiledTexture.prepareResources();
        int n = this.mContentEnd;
        for (int i = this.mContentStart; i < n; i++) {
            prepareSlotContent(i);
        }
        updateAllImageRequests();
    }

    private class AlbumCoverLoader extends BitmapLoader implements EntryUpdater {
        private MediaItem mMediaItem;
        private final int mSlotIndex;

        public AlbumCoverLoader(int slotIndex, MediaItem item) {
            this.mSlotIndex = slotIndex;
            this.mMediaItem = item;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return AlbumSetSlidingWindow.this.mThreadPool.submit(this.mMediaItem.requestImage(2), l);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            AlbumSetSlidingWindow.this.mHandler.obtainMessage(1, this).sendToTarget();
        }

        @Override
        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap != null) {
                AlbumSetEntry entry = AlbumSetSlidingWindow.this.mData[this.mSlotIndex % AlbumSetSlidingWindow.this.mData.length];
                TiledTexture texture = new TiledTexture(bitmap);
                entry.bitmapTexture = texture;
                entry.content = texture;
                if (AlbumSetSlidingWindow.this.isActiveSlot(this.mSlotIndex)) {
                    AlbumSetSlidingWindow.this.mContentUploader.addTexture(texture);
                    AlbumSetSlidingWindow.access$606(AlbumSetSlidingWindow.this);
                    if (AlbumSetSlidingWindow.this.mActiveRequestCount == 0) {
                        AlbumSetSlidingWindow.this.requestNonactiveImages();
                    }
                    if (AlbumSetSlidingWindow.this.mListener != null) {
                        AlbumSetSlidingWindow.this.mListener.onContentChanged();
                        return;
                    }
                    return;
                }
                AlbumSetSlidingWindow.this.mContentUploader.addTexture(texture);
            }
        }
    }

    private static int identifyCacheFlag(MediaSet set) {
        if (set == null || (set.getSupportedOperations() & NotificationCompat.FLAG_LOCAL_ONLY) == 0) {
            return 0;
        }
        return set.getCacheFlag();
    }

    private static int identifyCacheStatus(MediaSet set) {
        if (set == null || (set.getSupportedOperations() & NotificationCompat.FLAG_LOCAL_ONLY) == 0) {
            return 0;
        }
        return set.getCacheStatus();
    }

    private class AlbumLabelLoader extends BitmapLoader implements EntryUpdater {
        private final int mSlotIndex;
        private final int mSourceType;
        private final String mTitle;
        private final int mTotalCount;

        public AlbumLabelLoader(int slotIndex, String title, int totalCount, int sourceType) {
            this.mSlotIndex = slotIndex;
            this.mTitle = title;
            this.mTotalCount = totalCount;
            this.mSourceType = sourceType;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return AlbumSetSlidingWindow.this.mThreadPool.submit(AlbumSetSlidingWindow.this.mLabelMaker.requestLabel(this.mTitle, String.valueOf(this.mTotalCount), this.mSourceType), l);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            AlbumSetSlidingWindow.this.mHandler.obtainMessage(1, this).sendToTarget();
        }

        @Override
        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap != null) {
                AlbumSetEntry entry = AlbumSetSlidingWindow.this.mData[this.mSlotIndex % AlbumSetSlidingWindow.this.mData.length];
                BitmapTexture texture = new BitmapTexture(bitmap);
                texture.setOpaque(false);
                entry.labelTexture = texture;
                if (AlbumSetSlidingWindow.this.isActiveSlot(this.mSlotIndex)) {
                    AlbumSetSlidingWindow.this.mLabelUploader.addFgTexture(texture);
                    AlbumSetSlidingWindow.access$606(AlbumSetSlidingWindow.this);
                    if (AlbumSetSlidingWindow.this.mActiveRequestCount == 0) {
                        AlbumSetSlidingWindow.this.requestNonactiveImages();
                    }
                    if (AlbumSetSlidingWindow.this.mListener != null) {
                        AlbumSetSlidingWindow.this.mListener.onContentChanged();
                        return;
                    }
                    return;
                }
                AlbumSetSlidingWindow.this.mLabelUploader.addBgTexture(texture);
            }
        }
    }

    public void onSlotSizeChanged(int width, int height) {
        if (this.mSlotWidth != width) {
            this.mSlotWidth = width;
            this.mLoadingLabel = null;
            this.mLabelMaker.setLabelWidth(this.mSlotWidth);
            if (this.mIsActive) {
                int n = this.mContentEnd;
                for (int i = this.mContentStart; i < n; i++) {
                    AlbumSetEntry entry = this.mData[i % this.mData.length];
                    if (entry.labelLoader != null) {
                        entry.labelLoader.recycle();
                        entry.labelLoader = null;
                        entry.labelTexture = null;
                    }
                    if (entry.album != null) {
                        entry.labelLoader = new AlbumLabelLoader(i, entry.title, entry.totalCount, entry.sourceType);
                    }
                }
                updateAllImageRequests();
                updateTextureUploadQueue();
            }
        }
    }
}
