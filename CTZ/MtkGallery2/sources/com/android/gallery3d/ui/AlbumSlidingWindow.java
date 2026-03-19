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
import com.android.gallery3d.util.ThreadPool;

public class AlbumSlidingWindow implements AlbumDataLoader.DataListener {
    private static final int JOB_LIMIT = ThreadPool.PARALLEL_THREAD_NUM;
    private static final int MSG_UPDATE_ENTRY = 0;
    private static final String TAG = "Gallery2/AlbumSlidingWindow";
    private final AlbumEntry[] mData;
    private final SynchronizedHandler mHandler;
    private Listener mListener;
    private int mSize;
    private final AlbumDataLoader mSource;
    private final JobLimiter mThreadPool;
    private final TiledTexture.Uploader mTileUploader;
    private final JobLimiter mVideoMicroThumbDecoder;
    private int mContentStart = 0;
    private int mContentEnd = 0;
    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;
    public boolean mDecodeFinished = false;
    public long mDecodeFinishTime = 0;

    public interface Listener {
        void onContentChanged();

        void onSizeChanged(int i);
    }

    static int access$806(AlbumSlidingWindow albumSlidingWindow) {
        int i = albumSlidingWindow.mActiveRequestCount - 1;
        albumSlidingWindow.mActiveRequestCount = i;
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

        public PanoSupportListener(AlbumEntry albumEntry) {
            this.mEntry = albumEntry;
        }

        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean z, boolean z2) {
            if (this.mEntry != null) {
                this.mEntry.isPanorama = z;
            }
        }
    }

    public AlbumSlidingWindow(AbstractGalleryActivity abstractGalleryActivity, AlbumDataLoader albumDataLoader, int i) {
        albumDataLoader.setDataListener(this);
        this.mSource = albumDataLoader;
        this.mData = new AlbumEntry[i];
        this.mSize = albumDataLoader.size();
        this.mHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == 0);
                ((ThumbnailLoader) message.obj).updateEntry();
            }
        };
        this.mThreadPool = new JobLimiter(abstractGalleryActivity.getThreadPool(), JOB_LIMIT);
        this.mVideoMicroThumbDecoder = new JobLimiter(abstractGalleryActivity.getThreadPool(), 2);
        this.mTileUploader = new TiledTexture.Uploader(abstractGalleryActivity.getGLRoot());
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public AlbumEntry get(int i) {
        if (!isActiveSlot(i)) {
            Utils.fail("invalid slot: %s outsides (%s, %s)", Integer.valueOf(i), Integer.valueOf(this.mActiveStart), Integer.valueOf(this.mActiveEnd));
        }
        return this.mData[i % this.mData.length];
    }

    public boolean isActiveSlot(int i) {
        return i >= this.mActiveStart && i < this.mActiveEnd;
    }

    private void setContentWindow(int i, int i2) {
        if (i == this.mContentStart && i2 == this.mContentEnd) {
            return;
        }
        if (!this.mIsActive) {
            this.mContentStart = i;
            this.mContentEnd = i2;
            this.mSource.setActiveWindow(i, i2);
            return;
        }
        if (i >= this.mContentEnd || this.mContentStart >= i2) {
            int i3 = this.mContentEnd;
            for (int i4 = this.mContentStart; i4 < i3; i4++) {
                freeSlotContent(i4);
            }
            this.mSource.setActiveWindow(i, i2);
            for (int i5 = i; i5 < i2; i5++) {
                prepareSlotContent(i5);
            }
        } else {
            for (int i6 = this.mContentStart; i6 < i; i6++) {
                freeSlotContent(i6);
            }
            int i7 = this.mContentEnd;
            for (int i8 = i2; i8 < i7; i8++) {
                freeSlotContent(i8);
            }
            this.mSource.setActiveWindow(i, i2);
            int i9 = this.mContentStart;
            for (int i10 = i; i10 < i9; i10++) {
                prepareSlotContent(i10);
            }
            for (int i11 = this.mContentEnd; i11 < i2; i11++) {
                prepareSlotContent(i11);
            }
        }
        this.mContentStart = i;
        this.mContentEnd = i2;
    }

    public void setActiveWindow(int i, int i2) {
        if (i > i2 || i2 - i > this.mData.length || i2 > this.mSize) {
            Utils.fail("%s, %s, %s, %s", Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(this.mData.length), Integer.valueOf(this.mSize));
        }
        AlbumEntry[] albumEntryArr = this.mData;
        this.mActiveStart = i;
        this.mActiveEnd = i2;
        int iClamp = Utils.clamp(((i + i2) / 2) - (albumEntryArr.length / 2), 0, Math.max(0, this.mSize - albumEntryArr.length));
        setContentWindow(iClamp, Math.min(albumEntryArr.length + iClamp, this.mSize));
        updateTextureUploadQueue();
        if (this.mIsActive) {
            updateAllImageRequests();
        }
    }

    private void uploadBgTextureInSlot(int i) {
        if (i < this.mContentEnd && i >= this.mContentStart) {
            AlbumEntry albumEntry = this.mData[i % this.mData.length];
            if (albumEntry.bitmapTexture != null) {
                this.mTileUploader.addTexture(albumEntry.bitmapTexture);
            }
        }
    }

    private void updateTextureUploadQueue() {
        if (this.mIsActive) {
            this.mTileUploader.clear();
            int i = this.mActiveEnd;
            for (int i2 = this.mActiveStart; i2 < i; i2++) {
                AlbumEntry albumEntry = this.mData[i2 % this.mData.length];
                if (albumEntry.bitmapTexture != null) {
                    this.mTileUploader.addTexture(albumEntry.bitmapTexture);
                }
            }
            int iMax = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
            for (int i3 = 0; i3 < iMax; i3++) {
                uploadBgTextureInSlot(this.mActiveEnd + i3);
                uploadBgTextureInSlot((this.mActiveStart - i3) - 1);
            }
        }
    }

    private void requestNonactiveImages() {
        int iMax = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < iMax; i++) {
            requestSlotImage(this.mActiveEnd + i);
            requestSlotImage((this.mActiveStart - 1) - i);
        }
    }

    private boolean requestSlotImage(int i) {
        if (i < this.mContentStart || i >= this.mContentEnd) {
            return false;
        }
        AlbumEntry albumEntry = this.mData[i % this.mData.length];
        if (albumEntry.content != null || albumEntry.item == null) {
            return false;
        }
        albumEntry.mPanoSupportListener = new PanoSupportListener(albumEntry);
        albumEntry.item.getPanoramaSupport(albumEntry.mPanoSupportListener);
        albumEntry.contentLoader.startLoad();
        return albumEntry.contentLoader.isRequestInProgress();
    }

    private void cancelNonactiveImages() {
        int iMax = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < iMax; i++) {
            cancelSlotImage(this.mActiveEnd + i);
            cancelSlotImage((this.mActiveStart - 1) - i);
        }
    }

    private void cancelSlotImage(int i) {
        if (i < this.mContentStart || i >= this.mContentEnd) {
            return;
        }
        AlbumEntry albumEntry = this.mData[i % this.mData.length];
        if (albumEntry.contentLoader != null) {
            albumEntry.contentLoader.cancelLoad();
        }
    }

    private void freeSlotContent(int i) {
        AlbumEntry[] albumEntryArr = this.mData;
        int length = i % albumEntryArr.length;
        AlbumEntry albumEntry = albumEntryArr[length];
        if (albumEntry.contentLoader != null) {
            albumEntry.contentLoader.recycle();
        }
        if (albumEntry.bitmapTexture != null) {
            albumEntry.bitmapTexture.recycle();
        }
        albumEntryArr[length] = null;
    }

    private void prepareSlotContent(int i) {
        int mediaType;
        AlbumEntry albumEntry = new AlbumEntry();
        MediaItem mediaItem = this.mSource.get(i);
        albumEntry.item = mediaItem;
        if (mediaItem == null) {
            mediaType = 1;
        } else {
            mediaType = albumEntry.item.getMediaType();
        }
        albumEntry.mediaType = mediaType;
        albumEntry.path = mediaItem == null ? null : mediaItem.getPath();
        albumEntry.rotation = mediaItem == null ? 0 : mediaItem.getRotation();
        albumEntry.contentLoader = new ThumbnailLoader(i, albumEntry.item);
        this.mData[i % this.mData.length] = albumEntry;
    }

    private void updateAllImageRequests() {
        this.mActiveRequestCount = 0;
        int i = this.mActiveEnd;
        for (int i2 = this.mActiveStart; i2 < i; i2++) {
            if (requestSlotImage(i2)) {
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

        public ThumbnailLoader(int i, MediaItem mediaItem) {
            this.mSlotIndex = i;
            this.mItem = mediaItem;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> futureListener) {
            if (4 == this.mItem.getMediaType()) {
                return AlbumSlidingWindow.this.mVideoMicroThumbDecoder.submit(this.mItem.requestImage(2), this);
            }
            return AlbumSlidingWindow.this.mThreadPool.submit(this.mItem.requestImage(2), this);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            AlbumSlidingWindow.this.mHandler.obtainMessage(0, this).sendToTarget();
        }

        public void updateEntry() {
            Bitmap bitmap;
            AlbumEntry albumEntry;
            if (!AlbumSlidingWindow.this.mIsActive || (bitmap = getBitmap()) == null || (albumEntry = AlbumSlidingWindow.this.mData[this.mSlotIndex % AlbumSlidingWindow.this.mData.length]) == null) {
                return;
            }
            albumEntry.bitmapTexture = new TiledTexture(bitmap);
            albumEntry.content = albumEntry.bitmapTexture;
            if (AlbumSlidingWindow.this.isActiveSlot(this.mSlotIndex)) {
                AlbumSlidingWindow.this.mTileUploader.addTexture(albumEntry.bitmapTexture);
                AlbumSlidingWindow.access$806(AlbumSlidingWindow.this);
                if (AlbumSlidingWindow.this.mActiveRequestCount == 0) {
                    AlbumSlidingWindow.this.requestNonactiveImages();
                }
                if (AlbumSlidingWindow.this.mListener != null) {
                    AlbumSlidingWindow.this.mListener.onContentChanged();
                }
            } else {
                AlbumSlidingWindow.this.mTileUploader.addTexture(albumEntry.bitmapTexture);
            }
            this.mBitmapLoaded = true;
            if (AlbumSlidingWindow.this.isActiveSlot(this.mSlotIndex) && AlbumSlidingWindow.this.mActiveRequestCount == 0) {
                AlbumSlidingWindow.this.mDecodeFinished = true;
                AlbumSlidingWindow.this.mDecodeFinishTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onSizeChanged(int i) {
        if (this.mSize != i) {
            this.mSize = i;
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
    public void onContentChanged(int i) {
        if (i >= this.mContentStart && i < this.mContentEnd && this.mIsActive) {
            freeSlotContent(i);
            prepareSlotContent(i);
            updateAllImageRequests();
            if (this.mListener != null && isActiveSlot(i)) {
                this.mListener.onContentChanged();
            }
        }
    }

    public void resume() {
        this.mIsActive = true;
        TiledTexture.prepareResources();
        int i = this.mContentEnd;
        for (int i2 = this.mContentStart; i2 < i; i2++) {
            prepareSlotContent(i2);
        }
        updateAllImageRequests();
    }

    public void pause() {
        this.mIsActive = false;
        this.mTileUploader.clear();
        TiledTexture.freeResources();
        int i = this.mContentEnd;
        for (int i2 = this.mContentStart; i2 < i; i2++) {
            freeSlotContent(i2);
        }
    }

    public void recycle(AlbumEntry albumEntry) {
        if (albumEntry.contentLoader != null) {
            albumEntry.contentLoader.recycle();
        }
    }

    public boolean isAllActiveSlotsFilled() {
        int i = this.mActiveStart;
        int i2 = this.mActiveEnd;
        if (i < 0 || i >= i2) {
            Log.w(TAG, "<isAllActiveSlotFilled> active range not ready yet");
            return false;
        }
        while (i < i2) {
            AlbumEntry albumEntry = this.mData[i % this.mData.length];
            if (albumEntry != null) {
                BitmapLoader bitmapLoader = albumEntry.contentLoader;
                if (bitmapLoader != null && bitmapLoader.isLoadingCompleted()) {
                    i++;
                } else {
                    Log.d(TAG, "<isAllActiveSlotsFilled> slot " + i + " is not loaded, return false");
                    return false;
                }
            } else {
                Log.d(TAG, "<isAllActiveSlotsFilled> slot " + i + " is not loaded, return false");
                return false;
            }
        }
        Log.d(TAG, "<isAllActiveSlotsFilled> return true");
        return true;
    }
}
