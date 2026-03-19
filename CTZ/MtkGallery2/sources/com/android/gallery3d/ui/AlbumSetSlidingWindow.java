package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.os.Message;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetDataLoader;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataSourceType;
import com.android.gallery3d.data.LocalVideo;
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
import com.android.gallery3d.util.JobLimiter;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.layout.FancyHelper;

public class AlbumSetSlidingWindow implements AlbumSetDataLoader.DataListener {
    private static final int MSG_UPDATE_ALBUM_ENTRY = 1;
    private static final String TAG = "Gallery2/AlbumSetSlidingWindow";
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
    private final JobLimiter mVideoMicroThumbDecoder;
    private int mContentStart = 0;
    private int mContentEnd = 0;
    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;
    public boolean mDecodeFinished = false;
    public long mDecodeFinishTime = 0;
    private volatile boolean mIsFancyLayout = true;
    private int mOrientation = -1;

    private interface EntryUpdater {
        void updateEntry();
    }

    public interface Listener {
        void onContentChanged();

        void onSizeChanged(int i);
    }

    static int access$906(AlbumSetSlidingWindow albumSetSlidingWindow) {
        int i = albumSetSlidingWindow.mActiveRequestCount - 1;
        albumSetSlidingWindow.mActiveRequestCount = i;
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

    public AlbumSetSlidingWindow(AbstractGalleryActivity abstractGalleryActivity, AlbumSetDataLoader albumSetDataLoader, AlbumSetSlotRenderer.LabelSpec labelSpec, int i) {
        albumSetDataLoader.setModelListener(this);
        this.mSource = albumSetDataLoader;
        this.mData = new AlbumSetEntry[i];
        this.mSize = albumSetDataLoader.size();
        this.mThreadPool = abstractGalleryActivity.getThreadPool();
        this.mVideoMicroThumbDecoder = new JobLimiter(abstractGalleryActivity.getThreadPool(), 2);
        this.mLabelMaker = new AlbumLabelMaker(abstractGalleryActivity.getAndroidContext(), labelSpec);
        this.mLoadingText = abstractGalleryActivity.getAndroidContext().getString(R.string.loading);
        this.mContentUploader = new TiledTexture.Uploader(abstractGalleryActivity.getGLRoot());
        this.mLabelUploader = new TextureUploader(abstractGalleryActivity.getGLRoot());
        this.mHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot()) {
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

    public AlbumSetEntry get(int i) {
        if (!isActiveSlot(i)) {
            Utils.fail("invalid slot: %s outsides (%s, %s)", Integer.valueOf(i), Integer.valueOf(this.mActiveStart), Integer.valueOf(this.mActiveEnd));
        }
        return this.mData[i % this.mData.length];
    }

    public int size() {
        return this.mSize;
    }

    public boolean isActiveSlot(int i) {
        return i >= this.mActiveStart && i < this.mActiveEnd;
    }

    private void setContentWindow(int i, int i2) {
        if (i == this.mContentStart && i2 == this.mContentEnd) {
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
            Utils.fail("start = %s, end = %s, length = %s, size = %s", Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(this.mData.length), Integer.valueOf(this.mSize));
        }
        AlbumSetEntry[] albumSetEntryArr = this.mData;
        this.mActiveStart = i;
        this.mActiveEnd = i2;
        int iClamp = Utils.clamp(((i + i2) / 2) - (albumSetEntryArr.length / 2), 0, Math.max(0, this.mSize - albumSetEntryArr.length));
        setContentWindow(iClamp, Math.min(albumSetEntryArr.length + iClamp, this.mSize));
        if (this.mIsActive) {
            updateTextureUploadQueue();
            updateAllImageRequests();
        }
    }

    private void requestNonactiveImages() {
        int iMax = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < iMax; i++) {
            requestImagesInSlot(this.mActiveEnd + i);
            requestImagesInSlot((this.mActiveStart - 1) - i);
        }
    }

    private void cancelNonactiveImages() {
        int iMax = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
        for (int i = 0; i < iMax; i++) {
            cancelImagesInSlot(this.mActiveEnd + i);
            cancelImagesInSlot((this.mActiveStart - 1) - i);
        }
    }

    private void requestImagesInSlot(int i) {
        if (i < this.mContentStart || i >= this.mContentEnd) {
            return;
        }
        AlbumSetEntry albumSetEntry = this.mData[i % this.mData.length];
        if (albumSetEntry.coverLoader != null) {
            albumSetEntry.coverLoader.startLoad();
        }
        if (albumSetEntry.labelLoader != null) {
            albumSetEntry.labelLoader.startLoad();
        }
    }

    private void cancelImagesInSlot(int i) {
        if (i < this.mContentStart || i >= this.mContentEnd) {
            return;
        }
        AlbumSetEntry albumSetEntry = this.mData[i % this.mData.length];
        if (albumSetEntry.coverLoader != null) {
            albumSetEntry.coverLoader.cancelLoad();
        }
        if (albumSetEntry.labelLoader != null) {
            albumSetEntry.labelLoader.cancelLoad();
        }
    }

    private static long getDataVersion(MediaObject mediaObject) {
        if (mediaObject == null) {
            return -1L;
        }
        return mediaObject.getDataVersion();
    }

    private void freeSlotContent(int i) {
        AlbumSetEntry albumSetEntry = this.mData[i % this.mData.length];
        if (albumSetEntry != null) {
            if (albumSetEntry.coverLoader != null) {
                albumSetEntry.coverLoader.recycle();
            }
            if (albumSetEntry.labelLoader != null) {
                albumSetEntry.labelLoader.recycle();
            }
            if (albumSetEntry.labelTexture != null) {
                albumSetEntry.labelTexture.recycle();
            }
            if (albumSetEntry.bitmapTexture != null) {
                albumSetEntry.bitmapTexture.recycle();
            }
            this.mData[i % this.mData.length] = null;
        }
    }

    private boolean isLabelChanged(AlbumSetEntry albumSetEntry, String str, int i, int i2) {
        return (Utils.equals(albumSetEntry.title, str) && albumSetEntry.totalCount == i && albumSetEntry.sourceType == i2) ? false : true;
    }

    private void updateAlbumSetEntry(AlbumSetEntry albumSetEntry, int i) {
        boolean zIsCameraFolderCoverChanged;
        MediaSet mediaSet = this.mSource.getMediaSet(i);
        MediaItem coverItem = this.mSource.getCoverItem(i);
        int totalCount = this.mSource.getTotalCount(i);
        albumSetEntry.album = mediaSet;
        albumSetEntry.setDataVersion = getDataVersion(mediaSet);
        albumSetEntry.cacheFlag = identifyCacheFlag(mediaSet);
        albumSetEntry.cacheStatus = identifyCacheStatus(mediaSet);
        albumSetEntry.setPath = mediaSet == null ? null : mediaSet.getPath();
        String strEnsureNotNull = mediaSet == null ? "" : Utils.ensureNotNull(mediaSet.getName());
        int iIdentifySourceType = DataSourceType.identifySourceType(mediaSet);
        if (FancyHelper.isFancyLayoutSupported()) {
            zIsCameraFolderCoverChanged = isCameraFolderCoverChanged(albumSetEntry, coverItem, i);
        } else {
            zIsCameraFolderCoverChanged = false;
        }
        if (zIsCameraFolderCoverChanged || isLabelChanged(albumSetEntry, strEnsureNotNull, totalCount, iIdentifySourceType)) {
            albumSetEntry.title = strEnsureNotNull;
            albumSetEntry.totalCount = totalCount;
            albumSetEntry.sourceType = iIdentifySourceType;
            if (albumSetEntry.labelLoader != null) {
                albumSetEntry.labelLoader.recycle();
                albumSetEntry.labelLoader = null;
                albumSetEntry.labelTexture = null;
            }
            if (mediaSet != null) {
                if (FancyHelper.isFancyLayoutSupported()) {
                    albumSetEntry.labelLoader = new AlbumLabelLoader(i, strEnsureNotNull, totalCount, iIdentifySourceType, isLandCameraFolder(i, mediaSet, coverItem), this.mIsFancyLayout);
                } else {
                    albumSetEntry.labelLoader = new AlbumLabelLoader(i, strEnsureNotNull, totalCount, iIdentifySourceType);
                }
            }
        }
        albumSetEntry.coverItem = coverItem;
        if (getDataVersion(coverItem) != albumSetEntry.coverDataVersion) {
            albumSetEntry.coverDataVersion = getDataVersion(coverItem);
            albumSetEntry.rotation = coverItem != null ? coverItem.getRotation() : 0;
            if (albumSetEntry.coverLoader != null) {
                albumSetEntry.coverLoader.recycle();
                albumSetEntry.coverLoader = null;
                albumSetEntry.bitmapTexture = null;
                albumSetEntry.content = null;
            }
            if (coverItem != null) {
                albumSetEntry.coverLoader = new AlbumCoverLoader(i, coverItem);
            }
        }
    }

    private void prepareSlotContent(int i) {
        AlbumSetEntry albumSetEntry = new AlbumSetEntry();
        updateAlbumSetEntry(albumSetEntry, i);
        this.mData[i % this.mData.length] = albumSetEntry;
    }

    private static boolean startLoadBitmap(BitmapLoader bitmapLoader) {
        if (bitmapLoader == null) {
            return false;
        }
        bitmapLoader.startLoad();
        return bitmapLoader.isRequestInProgress();
    }

    private void uploadBackgroundTextureInSlot(int i) {
        if (i < this.mContentStart || i >= this.mContentEnd) {
            return;
        }
        AlbumSetEntry albumSetEntry = this.mData[i % this.mData.length];
        if (albumSetEntry.bitmapTexture != null) {
            this.mContentUploader.addTexture(albumSetEntry.bitmapTexture);
        }
        if (albumSetEntry.labelTexture != null) {
            this.mLabelUploader.addBgTexture(albumSetEntry.labelTexture);
        }
    }

    private void updateTextureUploadQueue() {
        if (this.mIsActive) {
            this.mContentUploader.clear();
            this.mLabelUploader.clear();
            int i = this.mActiveEnd;
            for (int i2 = this.mActiveStart; i2 < i; i2++) {
                AlbumSetEntry albumSetEntry = this.mData[i2 % this.mData.length];
                if (albumSetEntry.bitmapTexture != null) {
                    this.mContentUploader.addTexture(albumSetEntry.bitmapTexture);
                }
                if (albumSetEntry.labelTexture != null) {
                    this.mLabelUploader.addFgTexture(albumSetEntry.labelTexture);
                }
            }
            int iMax = Math.max(this.mContentEnd - this.mActiveEnd, this.mActiveStart - this.mContentStart);
            for (int i3 = 0; i3 < iMax; i3++) {
                uploadBackgroundTextureInSlot(this.mActiveEnd + i3);
                uploadBackgroundTextureInSlot((this.mActiveStart - i3) - 1);
            }
        }
    }

    private void updateAllImageRequests() {
        this.mActiveRequestCount = 0;
        int i = this.mActiveEnd;
        for (int i2 = this.mActiveStart; i2 < i; i2++) {
            AlbumSetEntry albumSetEntry = this.mData[i2 % this.mData.length];
            if (startLoadBitmap(albumSetEntry.coverLoader)) {
                this.mActiveRequestCount++;
            }
            if (startLoadBitmap(albumSetEntry.labelLoader)) {
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
    public void onSizeChanged(int i) {
        if (this.mIsActive && this.mSize != i) {
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
        if (!this.mIsActive) {
            return;
        }
        if (i < this.mContentStart || i >= this.mContentEnd) {
            Log.w(TAG, String.format("invalid update: %s is outside (%s, %s)", Integer.valueOf(i), Integer.valueOf(this.mContentStart), Integer.valueOf(this.mContentEnd)));
            return;
        }
        updateAlbumSetEntry(this.mData[i % this.mData.length], i);
        updateAllImageRequests();
        updateTextureUploadQueue();
        if (this.mListener != null && isActiveSlot(i)) {
            this.mListener.onContentChanged();
        }
    }

    public BitmapTexture getLoadingTexture() {
        if (this.mLoadingLabel == null) {
            this.mLoadingLabel = new BitmapTexture(this.mLabelMaker.requestLabel(this.mLoadingText, "", 0).run(ThreadPool.JOB_CONTEXT_STUB));
            this.mLoadingLabel.setOpaque(false);
        }
        return this.mLoadingLabel;
    }

    public void pause() {
        if (this.mSource != null) {
            this.mSource.setModelListener(null);
            Log.d(TAG, "<pause> set loader ModelListener as null");
        }
        this.mIsActive = false;
        this.mLabelUploader.clear();
        this.mContentUploader.clear();
        TiledTexture.freeResources();
        int i = this.mContentEnd;
        for (int i2 = this.mContentStart; i2 < i; i2++) {
            freeSlotContent(i2);
        }
    }

    public void resume() {
        if (this.mSource != null) {
            this.mSource.setModelListener(this);
            Log.d(TAG, "<resume> reset loader ModelListener");
        }
        this.mIsActive = true;
        TiledTexture.prepareResources();
        int i = this.mContentEnd;
        for (int i2 = this.mContentStart; i2 < i; i2++) {
            prepareSlotContent(i2);
        }
        updateAllImageRequests();
    }

    private class AlbumCoverLoader extends BitmapLoader implements EntryUpdater {
        private MediaItem mMediaItem;
        private final int mSlotIndex;

        public AlbumCoverLoader(int i, MediaItem mediaItem) {
            this.mSlotIndex = i;
            this.mMediaItem = mediaItem;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> futureListener) {
            if (4 == this.mMediaItem.getMediaType()) {
                if (!FancyHelper.isFancyLayoutSupported() || !AlbumSetSlidingWindow.this.mIsFancyLayout) {
                    return AlbumSetSlidingWindow.this.mVideoMicroThumbDecoder.submit(this.mMediaItem.requestImage(2), futureListener);
                }
                return AlbumSetSlidingWindow.this.mVideoMicroThumbDecoder.submit(this.mMediaItem.requestImage(3), futureListener);
            }
            if (!FancyHelper.isFancyLayoutSupported() || !AlbumSetSlidingWindow.this.mIsFancyLayout) {
                return AlbumSetSlidingWindow.this.mThreadPool.submit(this.mMediaItem.requestImage(2), futureListener);
            }
            return AlbumSetSlidingWindow.this.mThreadPool.submit(this.mMediaItem.requestImage(3), futureListener);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            AlbumSetSlidingWindow.this.mHandler.obtainMessage(1, this).sendToTarget();
        }

        @Override
        public void updateEntry() {
            Bitmap bitmap;
            AlbumSetEntry albumSetEntry;
            if (!AlbumSetSlidingWindow.this.mIsActive || (bitmap = getBitmap()) == null || (albumSetEntry = AlbumSetSlidingWindow.this.mData[this.mSlotIndex % AlbumSetSlidingWindow.this.mData.length]) == null) {
                return;
            }
            TiledTexture tiledTexture = new TiledTexture(bitmap);
            albumSetEntry.bitmapTexture = tiledTexture;
            albumSetEntry.content = tiledTexture;
            if (AlbumSetSlidingWindow.this.isActiveSlot(this.mSlotIndex)) {
                AlbumSetSlidingWindow.this.mContentUploader.addTexture(tiledTexture);
                AlbumSetSlidingWindow.access$906(AlbumSetSlidingWindow.this);
                if (AlbumSetSlidingWindow.this.mActiveRequestCount == 0) {
                    AlbumSetSlidingWindow.this.requestNonactiveImages();
                }
                if (AlbumSetSlidingWindow.this.mListener != null) {
                    AlbumSetSlidingWindow.this.mListener.onContentChanged();
                }
            } else {
                AlbumSetSlidingWindow.this.mContentUploader.addTexture(tiledTexture);
            }
            this.mBitmapLoaded = true;
            if (AlbumSetSlidingWindow.this.isActiveSlot(this.mSlotIndex) && AlbumSetSlidingWindow.this.mActiveRequestCount == 0) {
                AlbumSetSlidingWindow.this.mDecodeFinished = true;
                AlbumSetSlidingWindow.this.mDecodeFinishTime = System.currentTimeMillis();
            }
        }
    }

    private static int identifyCacheFlag(MediaSet mediaSet) {
        if (mediaSet == null || (mediaSet.getSupportedOperations() & 256) == 0) {
            return 0;
        }
        return mediaSet.getCacheFlag();
    }

    private static int identifyCacheStatus(MediaSet mediaSet) {
        if (mediaSet == null || (mediaSet.getSupportedOperations() & 256) == 0) {
            return 0;
        }
        return mediaSet.getCacheStatus();
    }

    private class AlbumLabelLoader extends BitmapLoader implements EntryUpdater {
        private final boolean mIsFancy;
        private final boolean mLandCamera;
        private final int mSlotIndex;
        private final int mSourceType;
        private final String mTitle;
        private final int mTotalCount;

        public AlbumLabelLoader(int i, String str, int i2, int i3, boolean z, boolean z2) {
            this.mSlotIndex = i;
            this.mTitle = str;
            this.mTotalCount = i2;
            this.mSourceType = i3;
            this.mLandCamera = z;
            this.mIsFancy = z2;
        }

        public AlbumLabelLoader(int i, String str, int i2, int i3) {
            this.mSlotIndex = i;
            this.mTitle = str;
            this.mTotalCount = i2;
            this.mSourceType = i3;
            this.mLandCamera = false;
            this.mIsFancy = false;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> futureListener) {
            if (FancyHelper.isFancyLayoutSupported()) {
                return AlbumSetSlidingWindow.this.mThreadPool.submit(AlbumSetSlidingWindow.this.mLabelMaker.requestLabel(this.mTitle, String.valueOf(this.mTotalCount), this.mSourceType, this.mLandCamera, this.mIsFancy), futureListener);
            }
            return AlbumSetSlidingWindow.this.mThreadPool.submit(AlbumSetSlidingWindow.this.mLabelMaker.requestLabel(this.mTitle, String.valueOf(this.mTotalCount), this.mSourceType), futureListener);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            AlbumSetSlidingWindow.this.mHandler.obtainMessage(1, this).sendToTarget();
        }

        @Override
        public void updateEntry() {
            Bitmap bitmap;
            AlbumSetEntry albumSetEntry;
            if (!AlbumSetSlidingWindow.this.mIsActive || (bitmap = getBitmap()) == null || (albumSetEntry = AlbumSetSlidingWindow.this.mData[this.mSlotIndex % AlbumSetSlidingWindow.this.mData.length]) == null) {
                return;
            }
            BitmapTexture bitmapTexture = new BitmapTexture(bitmap);
            bitmapTexture.setOpaque(false);
            albumSetEntry.labelTexture = bitmapTexture;
            if (AlbumSetSlidingWindow.this.isActiveSlot(this.mSlotIndex)) {
                AlbumSetSlidingWindow.this.mLabelUploader.addFgTexture(bitmapTexture);
                AlbumSetSlidingWindow.access$906(AlbumSetSlidingWindow.this);
                if (AlbumSetSlidingWindow.this.mActiveRequestCount == 0) {
                    AlbumSetSlidingWindow.this.requestNonactiveImages();
                }
                if (AlbumSetSlidingWindow.this.mListener != null) {
                    AlbumSetSlidingWindow.this.mListener.onContentChanged();
                }
            } else {
                AlbumSetSlidingWindow.this.mLabelUploader.addBgTexture(bitmapTexture);
            }
            this.mBitmapLoaded = true;
            if (AlbumSetSlidingWindow.this.isActiveSlot(this.mSlotIndex) && AlbumSetSlidingWindow.this.mActiveRequestCount == 0) {
                AlbumSetSlidingWindow.this.mDecodeFinished = true;
                AlbumSetSlidingWindow.this.mDecodeFinishTime = System.currentTimeMillis();
            }
        }
    }

    public void onSlotSizeChanged(int i, int i2) {
        if (this.mSlotWidth == i) {
            return;
        }
        this.mSlotWidth = i;
        this.mLoadingLabel = null;
        this.mLabelMaker.setLabelWidth(this.mSlotWidth);
        if (this.mIsActive) {
            int i3 = this.mContentEnd;
            for (int i4 = this.mContentStart; i4 < i3; i4++) {
                AlbumSetEntry albumSetEntry = this.mData[i4 % this.mData.length];
                if (albumSetEntry.labelLoader != null) {
                    albumSetEntry.labelLoader.recycle();
                    albumSetEntry.labelLoader = null;
                    albumSetEntry.labelTexture = null;
                }
                if (albumSetEntry.album != null) {
                    if (!FancyHelper.isFancyLayoutSupported()) {
                        albumSetEntry.labelLoader = new AlbumLabelLoader(i4, albumSetEntry.title, albumSetEntry.totalCount, albumSetEntry.sourceType);
                    } else {
                        int i5 = i4;
                        albumSetEntry.labelLoader = new AlbumLabelLoader(i5, albumSetEntry.title, albumSetEntry.totalCount, albumSetEntry.sourceType, isLandCameraFolder(i4, albumSetEntry.album, albumSetEntry.coverItem), this.mIsFancyLayout);
                    }
                }
            }
            updateAllImageRequests();
            updateTextureUploadQueue();
        }
    }

    public boolean isAllActiveSlotsFilled() {
        int i = this.mActiveStart;
        int i2 = this.mActiveEnd;
        if (i < 0 || i >= i2) {
            Log.w(TAG, "<isAllActiveSlotsFilled> active range not ready yet");
            return false;
        }
        while (i < i2) {
            AlbumSetEntry albumSetEntry = this.mData[i % this.mData.length];
            if (albumSetEntry != null) {
                BitmapLoader bitmapLoader = albumSetEntry.coverLoader;
                if (bitmapLoader != null && bitmapLoader.isLoadingCompleted()) {
                    BitmapLoader bitmapLoader2 = albumSetEntry.labelLoader;
                    if (bitmapLoader2 != null && bitmapLoader2.isLoadingCompleted()) {
                        i++;
                    } else {
                        Log.d(TAG, "<isAllActiveSlotsFilled> slot " + i + " is not loaded, return false");
                        return false;
                    }
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

    public void onEyePositionChanged(int i) {
        if (this.mOrientation == i) {
            return;
        }
        this.mOrientation = i;
        this.mIsFancyLayout = this.mOrientation == 1;
        Log.d(TAG, "<onEyePositionChanged> <Fancy> mIsFancyLayout " + this.mIsFancyLayout);
        forceRefreshCurrentContentWindow();
    }

    private void forceRefreshCurrentContentWindow() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int i = AlbumSetSlidingWindow.this.mContentEnd;
                for (int i2 = AlbumSetSlidingWindow.this.mContentStart; i2 < i; i2++) {
                    AlbumSetSlidingWindow.this.freeSlotContent(i2);
                    AlbumSetSlidingWindow.this.prepareSlotContent(i2);
                }
                AlbumSetSlidingWindow.this.updateAllImageRequests();
            }
        });
    }

    private boolean isLandCameraFolder(int i, MediaSet mediaSet, MediaItem mediaItem) {
        if (i != 0 || mediaItem == null || mediaSet == null || !mediaSet.isCameraRoll() || mediaItem.getMediaData() == null) {
            return false;
        }
        if (mediaItem.getMediaType() == 4) {
            int orientation = ((LocalVideo) mediaItem).getOrientation();
            return ((orientation != 0 && orientation != 180) || mediaItem.getHeight() == 0 || mediaItem.getWidth() == 0) ? false : true;
        }
        int rotation = mediaItem.getRotation();
        return (rotation == 90 || rotation == 270) ? mediaItem.getHeight() > mediaItem.getWidth() : mediaItem.getWidth() > mediaItem.getHeight();
    }

    private boolean isCameraFolderCoverChanged(AlbumSetEntry albumSetEntry, MediaItem mediaItem, int i) {
        if (i != 0 || mediaItem == null || albumSetEntry == null || albumSetEntry.coverItem == null) {
            return false;
        }
        if (mediaItem.getRotation() == albumSetEntry.rotation && mediaItem.getWidth() == albumSetEntry.coverItem.getWidth() && mediaItem.getHeight() == albumSetEntry.coverItem.getHeight()) {
            return false;
        }
        return true;
    }

    public void recycle(AlbumSetEntry albumSetEntry) {
        if (albumSetEntry.coverLoader != null) {
            albumSetEntry.coverLoader.recycle();
        }
    }
}
