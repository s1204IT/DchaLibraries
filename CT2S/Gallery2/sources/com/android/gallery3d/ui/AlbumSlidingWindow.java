package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.os.Message;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.JobLimiter;

public class AlbumSlidingWindow implements AlbumDataLoader.DataListener {
    private final AlbumEntry[] mData;
    private final SynchronizedHandler mHandler;
    private Listener mListener;
    private int mSize;
    private final AlbumDataLoader mSource;
    private final JobLimiter mThreadPool;
    private final TiledTexture.Uploader mTileUploader;
    private int mContentStart = 0;
    private int mContentEnd = 0;
    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;

    public interface Listener {
        void onContentChanged();

        void onSizeChanged(int i);
    }

    static int access$606(AlbumSlidingWindow x0) {
        int i = x0.mActiveRequestCount - 1;
        x0.mActiveRequestCount = i;
        return i;
    }

    public static class AlbumEntry {
        public TiledTexture bitmapTexture;
        public Texture content;
        private BitmapLoader contentLoader;
        public boolean isPanorama;
        public boolean isWaitDisplayed;
        public MediaItem item;
        private PanoSupportListener mPanoSupportListener;
        public int mediaType;
        public Path path;
        public int rotation;
    }

    private class PanoSupportListener implements MediaObject.PanoramaSupportCallback {
        public final AlbumEntry mEntry;

        public PanoSupportListener(AlbumEntry entry) {
            this.mEntry = entry;
        }

        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama, boolean isPanorama360) {
            if (this.mEntry != null) {
                this.mEntry.isPanorama = isPanorama;
            }
        }
    }

    public AlbumSlidingWindow(AbstractGalleryActivity activity, AlbumDataLoader source, int cacheSize) {
        source.setDataListener(this);
        this.mSource = source;
        this.mData = new AlbumEntry[cacheSize];
        this.mSize = source.size();
        this.mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == 0);
                ((ThumbnailLoader) message.obj).updateEntry();
            }
        };
        this.mThreadPool = new JobLimiter(activity.getThreadPool(), 2);
        this.mTileUploader = new TiledTexture.Uploader(activity.getGLRoot());
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public AlbumEntry get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
            Utils.fail("invalid slot: %s outsides (%s, %s)", Integer.valueOf(slotIndex), Integer.valueOf(this.mActiveStart), Integer.valueOf(this.mActiveEnd));
        }
        return this.mData[slotIndex % this.mData.length];
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= this.mActiveStart && slotIndex < this.mActiveEnd;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart != this.mContentStart || contentEnd != this.mContentEnd) {
            if (!this.mIsActive) {
                this.mContentStart = contentStart;
                this.mContentEnd = contentEnd;
                this.mSource.setActiveWindow(contentStart, contentEnd);
                return;
            }
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
            Utils.fail("%s, %s, %s, %s", Integer.valueOf(start), Integer.valueOf(end), Integer.valueOf(this.mData.length), Integer.valueOf(this.mSize));
        }
        AlbumEntry[] data = this.mData;
        this.mActiveStart = start;
        this.mActiveEnd = end;
        int contentStart = Utils.clamp(((start + end) / 2) - (data.length / 2), 0, Math.max(0, this.mSize - data.length));
        int contentEnd = Math.min(data.length + contentStart, this.mSize);
        setContentWindow(contentStart, contentEnd);
        updateTextureUploadQueue();
        if (this.mIsActive) {
            updateAllImageRequests();
        }
    }

    private void uploadBgTextureInSlot(int index) {
        if (index < this.mContentEnd && index >= this.mContentStart) {
            AlbumEntry entry = this.mData[index % this.mData.length];
            if (entry.bitmapTexture != null) {
                this.mTileUploader.addTexture(entry.bitmapTexture);
            }
        }
    }

    private void updateTextureUploadQueue() {
        if (this.mIsActive) {
            this.mTileUploader.clear();
            int n = this.mActiveEnd;
            for (int i = this.mActiveStart; i < n; i++) {
                AlbumEntry entry = this.mData[i % this.mData.length];
                if (entry.bitmapTexture != null) {
                    this.mTileUploader.addTexture(entry.bitmapTexture);
                }
            }
            int range = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
            for (int i2 = 0; i2 < range; i2++) {
                uploadBgTextureInSlot(this.mActiveEnd + i2);
                uploadBgTextureInSlot((this.mActiveStart - i2) - 1);
            }
        }
    }

    private void requestNonactiveImages() {
        int range = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < range; i++) {
            requestSlotImage(this.mActiveEnd + i);
            requestSlotImage((this.mActiveStart - 1) - i);
        }
    }

    private boolean requestSlotImage(int slotIndex) {
        if (slotIndex < this.mContentStart || slotIndex >= this.mContentEnd) {
            return false;
        }
        AlbumEntry entry = this.mData[slotIndex % this.mData.length];
        if (entry.content != null || entry.item == null) {
            return false;
        }
        entry.mPanoSupportListener = new PanoSupportListener(entry);
        entry.item.getPanoramaSupport(entry.mPanoSupportListener);
        entry.contentLoader.startLoad();
        return entry.contentLoader.isRequestInProgress();
    }

    private void cancelNonactiveImages() {
        int range = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < range; i++) {
            cancelSlotImage(this.mActiveEnd + i);
            cancelSlotImage((this.mActiveStart - 1) - i);
        }
    }

    private void cancelSlotImage(int slotIndex) {
        if (slotIndex >= this.mContentStart && slotIndex < this.mContentEnd) {
            AlbumEntry item = this.mData[slotIndex % this.mData.length];
            if (item.contentLoader != null) {
                item.contentLoader.cancelLoad();
            }
        }
    }

    private void freeSlotContent(int slotIndex) {
        AlbumEntry[] data = this.mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        if (entry.contentLoader != null) {
            entry.contentLoader.recycle();
        }
        if (entry.bitmapTexture != null) {
            entry.bitmapTexture.recycle();
        }
        data[index] = null;
    }

    private void prepareSlotContent(int slotIndex) {
        AlbumEntry entry = new AlbumEntry();
        MediaItem item = this.mSource.get(slotIndex);
        entry.item = item;
        entry.mediaType = item == null ? 1 : entry.item.getMediaType();
        entry.path = item == null ? null : item.getPath();
        entry.rotation = item == null ? 0 : item.getRotation();
        entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);
        this.mData[slotIndex % this.mData.length] = entry;
    }

    private void updateAllImageRequests() {
        this.mActiveRequestCount = 0;
        int n = this.mActiveEnd;
        for (int i = this.mActiveStart; i < n; i++) {
            if (requestSlotImage(i)) {
                this.mActiveRequestCount++;
            }
        }
        if (this.mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }

    private class ThumbnailLoader extends BitmapLoader {
        private final MediaItem mItem;
        private final int mSlotIndex;

        public ThumbnailLoader(int slotIndex, MediaItem item) {
            this.mSlotIndex = slotIndex;
            this.mItem = item;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return AlbumSlidingWindow.this.mThreadPool.submit(this.mItem.requestImage(2), this);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            AlbumSlidingWindow.this.mHandler.obtainMessage(0, this).sendToTarget();
        }

        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap != null) {
                AlbumEntry entry = AlbumSlidingWindow.this.mData[this.mSlotIndex % AlbumSlidingWindow.this.mData.length];
                entry.bitmapTexture = new TiledTexture(bitmap);
                entry.content = entry.bitmapTexture;
                if (AlbumSlidingWindow.this.isActiveSlot(this.mSlotIndex)) {
                    AlbumSlidingWindow.this.mTileUploader.addTexture(entry.bitmapTexture);
                    AlbumSlidingWindow.access$606(AlbumSlidingWindow.this);
                    if (AlbumSlidingWindow.this.mActiveRequestCount == 0) {
                        AlbumSlidingWindow.this.requestNonactiveImages();
                    }
                    if (AlbumSlidingWindow.this.mListener != null) {
                        AlbumSlidingWindow.this.mListener.onContentChanged();
                        return;
                    }
                    return;
                }
                AlbumSlidingWindow.this.mTileUploader.addTexture(entry.bitmapTexture);
            }
        }
    }

    @Override
    public void onSizeChanged(int size) {
        if (this.mSize != size) {
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
        if (index >= this.mContentStart && index < this.mContentEnd && this.mIsActive) {
            freeSlotContent(index);
            prepareSlotContent(index);
            updateAllImageRequests();
            if (this.mListener != null && isActiveSlot(index)) {
                this.mListener.onContentChanged();
            }
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

    public void pause() {
        this.mIsActive = false;
        this.mTileUploader.clear();
        TiledTexture.freeResources();
        int n = this.mContentEnd;
        for (int i = this.mContentStart; i < n; i++) {
            freeSlotContent(i);
        }
    }
}
