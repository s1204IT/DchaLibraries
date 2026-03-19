package com.android.gallery3d.app;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.os.Handler;
import android.os.Message;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.ui.BitmapScreenNail;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.TileImageViewAdapter;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import mf.org.apache.xerces.dom3.as.ASContentModel;

public class PhotoDataAdapter implements PhotoPage.Model {
    private AbstractGalleryActivity mActivity;
    private int mCameraIndex;
    private int mCurrentIndex;
    private DataListener mDataListener;
    private boolean mIsActive;
    private boolean mIsPanorama;
    private boolean mIsStaticCamera;
    private Path mItemPath;
    private final Handler mMainHandler;
    private final PhotoView mPhotoView;
    private ReloadTask mReloadTask;
    private final MediaSet mSource;
    private final ThreadPool mThreadPool;
    private final TiledTexture.Uploader mUploader;
    public static boolean sCurrentScreenNailDone = false;
    public static boolean sPerformanceCaseRunning = false;
    private static ImageFetch[] sImageFetchSeq = new ImageFetch[16];
    private final TileImageViewAdapter mTileProvider = new TileImageViewAdapter();
    private final MediaItem[] mData = new MediaItem[128];
    private int mContentStart = 0;
    private int mContentEnd = 0;
    private HashMap<Path, ImageEntry> mImageCache = new HashMap<>();
    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private final long[] mChanges = new long[7];
    private final Path[] mPaths = new Path[7];
    private long mSourceVersion = -1;
    private int mSize = 0;
    private int mFocusHintDirection = 0;
    private Path mFocusHintPath = null;
    private final SourceListener mSourceListener = new SourceListener();
    private final boolean mReDecodeToImproveImageQuality = true;
    private boolean mNeedFullImage = true;

    public interface DataListener extends LoadingListener {
        void onPhotoChanged(int i, Path path);
    }

    static {
        sImageFetchSeq[0] = new ImageFetch(0, 1);
        int i = 1;
        int i2 = 1;
        while (i < 7) {
            int i3 = i2 + 1;
            sImageFetchSeq[i2] = new ImageFetch(i, 1);
            sImageFetchSeq[i3] = new ImageFetch(-i, 1);
            i++;
            i2 = i3 + 1;
        }
        int i4 = i2 + 1;
        sImageFetchSeq[i2] = new ImageFetch(0, 2);
        sImageFetchSeq[i4] = new ImageFetch(1, 2);
        sImageFetchSeq[i4 + 1] = new ImageFetch(-1, 2);
    }

    private static class ImageFetch {
        int imageBit;
        int indexOffset;

        public ImageFetch(int i, int i2) {
            this.indexOffset = i;
            this.imageBit = i2;
        }
    }

    public PhotoDataAdapter(AbstractGalleryActivity abstractGalleryActivity, PhotoView photoView, MediaSet mediaSet, Path path, int i, int i2, boolean z, boolean z2) {
        this.mSource = (MediaSet) Utils.checkNotNull(mediaSet);
        this.mPhotoView = (PhotoView) Utils.checkNotNull(photoView);
        this.mItemPath = (Path) Utils.checkNotNull(path);
        this.mCurrentIndex = i;
        this.mCameraIndex = i2;
        this.mIsPanorama = z;
        this.mIsStaticCamera = z2;
        this.mThreadPool = abstractGalleryActivity.getThreadPool();
        Arrays.fill(this.mChanges, -1L);
        this.mUploader = new TiledTexture.Uploader(abstractGalleryActivity.getGLRoot());
        this.mMainHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        if (PhotoDataAdapter.this.mDataListener != null) {
                            PhotoDataAdapter.this.mDataListener.onLoadingStarted();
                            return;
                        }
                        return;
                    case 2:
                        if (PhotoDataAdapter.this.mDataListener != null) {
                            PhotoDataAdapter.this.mDataListener.onLoadingFinished(false);
                            return;
                        }
                        return;
                    case 3:
                        ((Runnable) message.obj).run();
                        return;
                    case 4:
                        PhotoDataAdapter.this.updateImageRequests();
                        return;
                    default:
                        throw new AssertionError();
                }
            }
        };
        updateSlidingWindow();
        this.mActivity = abstractGalleryActivity;
    }

    private MediaItem getItemInternal(int i) {
        if (i < 0 || i >= this.mSize || i < this.mContentStart || i >= this.mContentEnd) {
            return null;
        }
        return this.mData[i % 128];
    }

    private long getVersion(int i) {
        MediaItem itemInternal = getItemInternal(i);
        if (itemInternal == null) {
            return -1L;
        }
        return itemInternal.getDataVersion();
    }

    private Path getPath(int i) {
        MediaItem itemInternal = getItemInternal(i);
        if (itemInternal == null) {
            return null;
        }
        return itemInternal.getPath();
    }

    private void fireDataChange() {
        boolean z = false;
        for (int i = -3; i <= 3; i++) {
            long version = getVersion(this.mCurrentIndex + i);
            int i2 = i + 3;
            if (this.mChanges[i2] != version) {
                this.mChanges[i2] = version;
                z = true;
            }
        }
        if (z) {
            int[] iArr = new int[7];
            Path[] pathArr = new Path[7];
            System.arraycopy(this.mPaths, 0, pathArr, 0, 7);
            for (int i3 = 0; i3 < 7; i3++) {
                this.mPaths[i3] = getPath((this.mCurrentIndex + i3) - 3);
            }
            for (int i4 = 0; i4 < 7; i4++) {
                Path path = this.mPaths[i4];
                int i5 = ASContentModel.AS_UNBOUNDED;
                if (path == null) {
                    iArr[i4] = Integer.MAX_VALUE;
                } else {
                    int i6 = 0;
                    while (i6 < 7 && pathArr[i6] != path) {
                        i6++;
                    }
                    if (i6 < 7) {
                        i5 = i6 - 3;
                    }
                    iArr[i4] = i5;
                }
            }
            this.mPhotoView.notifyDataChange(iArr, -this.mCurrentIndex, (this.mSize - 1) - this.mCurrentIndex);
        }
    }

    public void setDataListener(DataListener dataListener) {
        this.mDataListener = dataListener;
    }

    private void updateScreenNail(Path path, Future<ScreenNail> future) {
        boolean z;
        ImageEntry imageEntry = this.mImageCache.get(path);
        ScreenNail screenNailCombine = future.get();
        if (imageEntry == null || imageEntry.screenNailTask != future) {
            if (screenNailCombine != null) {
                screenNailCombine.recycle();
                return;
            }
            return;
        }
        if (path != null) {
            ?? mediaObject = this.mActivity.getDataManager().getMediaObject(path);
            if (mediaObject instanceof LocalMediaItem) {
                String str = mediaObject.filePath;
                if (!new File(str).exists()) {
                    Log.d("Gallery2/PhotoDataAdapter", "<updateScreenNail> filePath" + str + " not exists!!!");
                    return;
                }
            }
        }
        imageEntry.screenNailTask = null;
        if (screenNailCombine != null) {
            z = false;
        } else {
            z = true;
        }
        if (imageEntry.screenNail instanceof BitmapScreenNail) {
            screenNailCombine = ((BitmapScreenNail) imageEntry.screenNail).combine(screenNailCombine);
        }
        if (screenNailCombine == null) {
            imageEntry.failToLoad = z;
        } else {
            imageEntry.failToLoad = z;
            imageEntry.screenNail = screenNailCombine;
        }
        int i = -3;
        while (true) {
            if (i > 3) {
                break;
            }
            if (path != getPath(this.mCurrentIndex + i)) {
                i++;
            } else {
                if (i == 0) {
                    updateTileProvider(imageEntry);
                }
                this.mPhotoView.notifyImageChange(i);
            }
        }
        updateImageRequests();
        updateScreenNailUploadQueue();
    }

    private void updateFullImage(Path path, Future<BitmapRegionDecoder> future) {
        ImageEntry imageEntry = this.mImageCache.get(path);
        if (imageEntry == null || imageEntry.fullImageTask != future) {
            BitmapRegionDecoder bitmapRegionDecoder = future.get();
            if (bitmapRegionDecoder != null) {
                bitmapRegionDecoder.recycle();
                return;
            }
            return;
        }
        imageEntry.fullImageTask = null;
        imageEntry.fullImage = future.get();
        if (imageEntry.fullImage != null && path == getPath(this.mCurrentIndex)) {
            updateTileProvider(imageEntry);
            this.mPhotoView.notifyImageChange(0);
        }
        updateImageRequests();
    }

    @Override
    public void resume() {
        this.mIsActive = true;
        TiledTexture.prepareResources();
        this.mSource.addContentListener(this.mSourceListener);
        updateImageCache();
        updateImageRequests();
        this.mReloadTask = new ReloadTask();
        this.mReloadTask.start();
        fireDataChange();
    }

    @Override
    public void pause() {
        this.mIsActive = false;
        if (this.mReloadTask != null) {
            this.mReloadTask.terminate();
        }
        this.mReloadTask = null;
        this.mSource.removeContentListener(this.mSourceListener);
        for (ImageEntry imageEntry : this.mImageCache.values()) {
            if (imageEntry.fullImageTask != null) {
                imageEntry.fullImageTask.cancel();
            }
            if (imageEntry.screenNailTask != null) {
                imageEntry.screenNailTask.cancel();
            }
            if (imageEntry.screenNail != null) {
                imageEntry.screenNail.recycle();
            }
            if (imageEntry.highQualityImageTask != null) {
                imageEntry.highQualityImageTask.cancel();
            }
            if (imageEntry.highQualityScreenNail != null) {
                imageEntry.highQualityScreenNail.recycle();
            }
        }
        this.mImageCache.clear();
        this.mTileProvider.clear();
        this.mUploader.clear();
        TiledTexture.freeResources();
    }

    private MediaItem getItem(int i) {
        if (i < 0 || i >= this.mSize || !this.mIsActive) {
            return null;
        }
        Utils.assertTrue(i >= this.mActiveStart && i < this.mActiveEnd);
        if (i < this.mContentStart || i >= this.mContentEnd) {
            return null;
        }
        return this.mData[i % 128];
    }

    private void updateCurrentIndex(int i) {
        if (this.mCurrentIndex == i) {
            return;
        }
        int i2 = this.mCurrentIndex;
        this.mCurrentIndex = i;
        updateSlidingWindow();
        MediaItem mediaItem = this.mData[i % 128];
        this.mItemPath = mediaItem == null ? null : mediaItem.getPath();
        updateImageCache();
        updateImageRequests();
        updateTileProvider();
        if (this.mDataListener != null) {
            this.mDataListener.onPhotoChanged(i, this.mItemPath);
        }
        fireDataChange();
    }

    private void uploadScreenNail(int i) {
        MediaItem item;
        ImageEntry imageEntry;
        int i2 = this.mCurrentIndex + i;
        if (i2 < this.mActiveStart || i2 >= this.mActiveEnd || (item = getItem(i2)) == null || (imageEntry = this.mImageCache.get(item.getPath())) == null) {
            return;
        }
        if (imageEntry.highQualityScreenNail != null) {
            ScreenNail screenNail = imageEntry.highQualityScreenNail;
            Log.d("Gallery2/PhotoDataAdapter", "<uploadScreenNail> highQualityScreenNail " + screenNail.getWidth() + "  " + screenNail.getHeight());
            return;
        }
        ScreenNail screenNail2 = imageEntry.screenNail;
    }

    private void updateScreenNailUploadQueue() {
        this.mUploader.clear();
        uploadScreenNail(0);
        for (int i = 1; i < 7; i++) {
            uploadScreenNail(i);
            uploadScreenNail(-i);
        }
    }

    @Override
    public void moveTo(int i) {
        updateCurrentIndex(i);
    }

    @Override
    public ScreenNail getScreenNail(int i) {
        ImageEntry imageEntry;
        int i2 = this.mCurrentIndex + i;
        if (i2 < 0 || i2 >= this.mSize || !this.mIsActive) {
            return null;
        }
        Utils.assertTrue(i2 >= this.mActiveStart && i2 < this.mActiveEnd);
        MediaItem item = getItem(i2);
        if (item == null || (imageEntry = this.mImageCache.get(item.getPath())) == null) {
            return null;
        }
        if (imageEntry.screenNail == null && !isCamera(i)) {
            imageEntry.screenNail = newPlaceholderScreenNail(item);
            if (i == 0) {
                updateTileProvider(imageEntry);
            }
        }
        if (imageEntry.highQualityScreenNail != null) {
            return imageEntry.highQualityScreenNail;
        }
        return imageEntry.screenNail;
    }

    @Override
    public void getImageSize(int i, PhotoView.Size size) {
        MediaItem item = getItem(this.mCurrentIndex + i);
        if (item == null) {
            size.width = 0;
            size.height = 0;
        } else {
            size.width = item.getWidth();
            size.height = item.getHeight();
        }
    }

    @Override
    public int getImageRotation(int i) {
        MediaItem item = getItem(this.mCurrentIndex + i);
        if (item == null) {
            return 0;
        }
        return item.getFullImageRotation();
    }

    @Override
    public void setNeedFullImage(boolean z) {
        this.mNeedFullImage = z;
        this.mMainHandler.sendEmptyMessage(4);
    }

    @Override
    public boolean isCamera(int i) {
        return this.mCurrentIndex + i == this.mCameraIndex;
    }

    @Override
    public boolean isPanorama(int i) {
        return isCamera(i) && this.mIsPanorama;
    }

    @Override
    public boolean isStaticCamera(int i) {
        return isCamera(i) && this.mIsStaticCamera;
    }

    @Override
    public boolean isVideo(int i) {
        MediaItem item = getItem(this.mCurrentIndex + i);
        return item != null && item.getMediaType() == 4;
    }

    @Override
    public boolean isDeletable(int i) {
        MediaItem item = getItem(this.mCurrentIndex + i);
        return (item == null || (item.getSupportedOperations() & 1) == 0) ? false : true;
    }

    @Override
    public int getLoadingState(int i) {
        ImageEntry imageEntry = this.mImageCache.get(getPath(this.mCurrentIndex + i));
        if (imageEntry == null) {
            return 0;
        }
        if (imageEntry.failToLoad) {
            return 2;
        }
        return imageEntry.screenNail != null ? 1 : 0;
    }

    @Override
    public ScreenNail getScreenNail() {
        return getScreenNail(0);
    }

    @Override
    public int getImageHeight() {
        return this.mTileProvider.getImageHeight();
    }

    @Override
    public int getImageWidth() {
        return this.mTileProvider.getImageWidth();
    }

    @Override
    public int getLevelCount() {
        return this.mTileProvider.getLevelCount();
    }

    @Override
    public Bitmap getTile(int i, int i2, int i3, int i4) {
        return this.mTileProvider.getTile(i, i2, i3, i4);
    }

    @Override
    public boolean isEmpty() {
        return this.mSize == 0;
    }

    @Override
    public int getCurrentIndex() {
        return this.mCurrentIndex;
    }

    @Override
    public MediaItem getMediaItem(int i) {
        int i2 = this.mCurrentIndex + i;
        if (i2 >= this.mContentStart && i2 < this.mContentEnd) {
            return this.mData[i2 % 128];
        }
        return null;
    }

    @Override
    public void setCurrentPhoto(Path path, int i) {
        if (this.mItemPath == path) {
            return;
        }
        this.mItemPath = path;
        this.mCurrentIndex = i;
        updateSlidingWindow();
        updateImageCache();
        fireDataChange();
        MediaItem mediaItem = getMediaItem(0);
        if (mediaItem == null || mediaItem.getPath() == path || this.mReloadTask == null) {
            return;
        }
        this.mReloadTask.notifyDirty();
    }

    @Override
    public void setFocusHintDirection(int i) {
        this.mFocusHintDirection = i;
    }

    @Override
    public void setFocusHintPath(Path path) {
        this.mFocusHintPath = path;
    }

    private void updateTileProvider() {
        ImageEntry imageEntry = this.mImageCache.get(getPath(this.mCurrentIndex));
        if (imageEntry == null) {
            this.mTileProvider.clear();
        } else {
            updateTileProvider(imageEntry);
        }
    }

    private void updateTileProvider(ImageEntry imageEntry) {
        ScreenNail screenNail = imageEntry.screenNail;
        if (imageEntry.highQualityScreenNail != null) {
            screenNail = imageEntry.highQualityScreenNail;
            Log.d("Gallery2/PhotoDataAdapter", "<updateTileProvider> highQualityScreenNail  " + screenNail.getWidth() + "  " + screenNail.getHeight());
        }
        BitmapRegionDecoder bitmapRegionDecoder = imageEntry.fullImage;
        if (screenNail != null) {
            if (bitmapRegionDecoder != null) {
                if (imageEntry.width > 0 && imageEntry.height > 0) {
                    this.mTileProvider.setScreenNail(screenNail, imageEntry.width, imageEntry.height);
                    this.mTileProvider.setRegionDecoder(bitmapRegionDecoder, imageEntry.width, imageEntry.height);
                } else {
                    this.mTileProvider.setScreenNail(screenNail, bitmapRegionDecoder.getWidth(), bitmapRegionDecoder.getHeight());
                    this.mTileProvider.setRegionDecoder(bitmapRegionDecoder);
                }
                MediaItem item = getItem(this.mCurrentIndex);
                if (item != null) {
                    this.mTileProvider.mMimeType = item.getMimeType();
                    return;
                }
                return;
            }
            this.mTileProvider.setScreenNail(screenNail, screenNail.getWidth(), screenNail.getHeight());
            return;
        }
        this.mTileProvider.clear();
    }

    private void updateSlidingWindow() {
        int iClamp = Utils.clamp(this.mCurrentIndex - 3, 0, Math.max(0, this.mSize - 7));
        int iMin = Math.min(this.mSize, iClamp + 7);
        if (this.mActiveStart == iClamp && this.mActiveEnd == iMin) {
            return;
        }
        this.mActiveStart = iClamp;
        this.mActiveEnd = iMin;
        int iClamp2 = Utils.clamp(this.mCurrentIndex - 64, 0, Math.max(0, this.mSize - 128));
        int iMin2 = Math.min(this.mSize, iClamp2 + 128);
        if (this.mContentStart > this.mActiveStart || this.mContentEnd < this.mActiveEnd || Math.abs(iClamp2 - this.mContentStart) > 16) {
            for (int i = this.mContentStart; i < this.mContentEnd; i++) {
                if (i < iClamp2 || i >= iMin2) {
                    this.mData[i % 128] = null;
                }
            }
            this.mContentStart = iClamp2;
            this.mContentEnd = iMin2;
            if (this.mReloadTask != null) {
                this.mReloadTask.notifyDirty();
            }
        }
    }

    private void updateImageRequests() {
        if (this.mIsActive) {
            int i = this.mCurrentIndex;
            MediaItem mediaItem = this.mData[i % 128];
            if (mediaItem == null || mediaItem.getPath() != this.mItemPath) {
                return;
            }
            Future<?> futureStartTaskIfNeeded = null;
            for (int i2 = 0; i2 < sImageFetchSeq.length; i2++) {
                int i3 = sImageFetchSeq[i2].indexOffset;
                int i4 = sImageFetchSeq[i2].imageBit;
                if ((i4 != 2 || this.mNeedFullImage) && (futureStartTaskIfNeeded = startTaskIfNeeded(i3 + i, i4)) != null) {
                    break;
                }
            }
            for (ImageEntry imageEntry : this.mImageCache.values()) {
                if (imageEntry.screenNailTask != null && imageEntry.screenNailTask != futureStartTaskIfNeeded) {
                    imageEntry.screenNailTask.cancel();
                    imageEntry.screenNailTask = null;
                    imageEntry.requestedScreenNail = -1L;
                }
                if (imageEntry.fullImageTask != null && imageEntry.fullImageTask != futureStartTaskIfNeeded) {
                    imageEntry.fullImageTask.cancel();
                    imageEntry.fullImageTask = null;
                    imageEntry.requestedFullImage = -1L;
                }
                if (imageEntry.highQualityImageTask != null && imageEntry.highQualityImageTask != futureStartTaskIfNeeded) {
                    imageEntry.highQualityImageTask.cancel();
                    imageEntry.highQualityImageTask = null;
                    imageEntry.requestedhighQualityImage = -1L;
                }
            }
        }
    }

    private class ScreenNailJob implements ThreadPool.Job<ScreenNail> {
        private MediaItem mItem;

        public ScreenNailJob(MediaItem mediaItem) {
            this.mItem = mediaItem;
        }

        @Override
        public ScreenNail run(ThreadPool.JobContext jobContext) {
            ScreenNail screenNail = this.mItem.getScreenNail();
            if (screenNail != null) {
                return screenNail;
            }
            if (PhotoDataAdapter.this.isTemporaryItem(this.mItem)) {
                Log.d("Gallery2/PhotoDataAdapter", "<ScreenNailJob.run> this is temporary item");
                return PhotoDataAdapter.this.newPlaceholderScreenNail(this.mItem);
            }
            Log.d("Gallery2/PhotoDataAdapter", "<ScreenNailJob.run> ScreenNail requestImage");
            Bitmap bitmapRun = this.mItem.requestImage(1).run(jobContext);
            if (jobContext.isCancelled()) {
                return null;
            }
            if (bitmapRun != null) {
                bitmapRun = BitmapUtils.rotateBitmap(bitmapRun, this.mItem.getRotation() - this.mItem.getFullImageRotation(), true);
            }
            if (PhotoDataAdapter.sPerformanceCaseRunning && bitmapRun != null && PhotoDataAdapter.this.getMediaItem(0) == this.mItem) {
                PhotoDataAdapter.sCurrentScreenNailDone = true;
            }
            if (bitmapRun == null) {
                return null;
            }
            return new BitmapScreenNail(bitmapRun, this.mItem);
        }
    }

    private class FullImageJob implements ThreadPool.Job<BitmapRegionDecoder> {
        private MediaItem mItem;

        public FullImageJob(MediaItem mediaItem) {
            this.mItem = mediaItem;
        }

        @Override
        public BitmapRegionDecoder run(ThreadPool.JobContext jobContext) {
            if (PhotoDataAdapter.this.isTemporaryItem(this.mItem)) {
                return null;
            }
            ImageEntry imageEntry = (ImageEntry) PhotoDataAdapter.this.mImageCache.get(this.mItem.getPath());
            if (imageEntry != null && imageEntry.failToLoad) {
                Log.d("Gallery2/PhotoDataAdapter", "<FullImageJob.run> decode thumbnail fail,no need to decode full image, return null");
                return null;
            }
            BitmapRegionDecoder bitmapRegionDecoderRun = this.mItem.requestLargeImage().run(jobContext);
            PhotoDataAdapter.this.cacheFullImageSize(imageEntry, this.mItem, bitmapRegionDecoderRun);
            return bitmapRegionDecoderRun;
        }
    }

    private void cacheFullImageSize(ImageEntry imageEntry, MediaItem mediaItem, BitmapRegionDecoder bitmapRegionDecoder) {
        int height;
        if (imageEntry == null) {
            Log.w("Gallery2/PhotoDataAdapter", "can not cache full image size");
            return;
        }
        int width = 0;
        if (mediaItem != null && mediaItem.getWidth() > 0 && mediaItem.getHeight() > 0) {
            width = mediaItem.getWidth();
            height = mediaItem.getHeight();
            Log.d("Gallery2/PhotoDataAdapter", "cache image size from media provider: " + width + "x" + height);
        } else if (bitmapRegionDecoder != null) {
            width = bitmapRegionDecoder.getWidth();
            height = bitmapRegionDecoder.getHeight();
            Log.d("Gallery2/PhotoDataAdapter", "cache image size from region decoder: " + width + "x" + height);
        } else {
            height = 0;
        }
        imageEntry.width = width;
        imageEntry.height = height;
    }

    private boolean isTemporaryItem(MediaItem mediaItem) {
        return this.mCameraIndex >= 0 && (mediaItem instanceof LocalMediaItem) && mediaItem.getBucketId() == MediaSetUtils.CAMERA_BUCKET_ID && mediaItem.getSize() == 0 && mediaItem.getWidth() != 0 && mediaItem.getHeight() != 0 && mediaItem.getDateInMs() - System.currentTimeMillis() <= 10000;
    }

    private ScreenNail newPlaceholderScreenNail(MediaItem mediaItem) {
        int width = mediaItem.getWidth();
        int height = mediaItem.getHeight();
        MediaData mediaData = mediaItem.getMediaData();
        if (mediaData != null && mediaData.isVideo) {
            if (ExtFieldsUtils.getVideoRotation(mediaData) % 180 != 0) {
                Log.v("Gallery2/PhotoDataAdapter", "swap w & h");
                height = width;
                width = height;
            }
            Log.v("Gallery2/PhotoDataAdapter", "<newPlaceholderScreenNail> width=" + width + ", height=" + height);
        }
        return new BitmapScreenNail(width, height, mediaItem);
    }

    private Future<?> startTaskIfNeeded(int i, int i2) {
        ImageEntry imageEntry;
        if (i < this.mActiveStart || i >= this.mActiveEnd || (imageEntry = this.mImageCache.get(getPath(i))) == null) {
            return null;
        }
        MediaItem mediaItem = this.mData[i % 128];
        Utils.assertTrue(mediaItem != null);
        long dataVersion = mediaItem.getDataVersion();
        if (i2 == 1 && imageEntry.screenNailTask != null && imageEntry.requestedScreenNail == dataVersion) {
            return imageEntry.screenNailTask;
        }
        if (i2 == 2 && imageEntry.fullImageTask != null && imageEntry.requestedFullImage == dataVersion) {
            return imageEntry.fullImageTask;
        }
        if (i2 == 2 && imageEntry.highQualityImageTask != null && imageEntry.requestedhighQualityImage == dataVersion) {
            return imageEntry.highQualityImageTask;
        }
        if (i2 == 1 && imageEntry.requestedScreenNail != dataVersion) {
            imageEntry.requestedScreenNail = dataVersion;
            imageEntry.screenNailTask = this.mThreadPool.submit(new ScreenNailJob(mediaItem), new ScreenNailListener(mediaItem));
            return imageEntry.screenNailTask;
        }
        if (i2 == 2 && imageEntry.requestedFullImage != dataVersion && (mediaItem.getSupportedOperations() & 64) != 0) {
            Log.d("Gallery2/PhotoDataAdapter", "<startTaskIfNeeded> fullImageTask!");
            imageEntry.requestedFullImage = dataVersion;
            imageEntry.fullImageTask = this.mThreadPool.submit(new FullImageJob(mediaItem), new FullImageListener(mediaItem));
            return imageEntry.fullImageTask;
        }
        if (i2 != 2 || imageEntry.requestedhighQualityImage == dataVersion || imageEntry.failToLoad || mediaItem.getExtItem() == null || !mediaItem.getExtItem().supportHighQuality() || (mediaItem.getSupportedOperations() & 64) != 0) {
            return null;
        }
        Log.d("Gallery2/PhotoDataAdapter", "<startTaskIfNeeded> highQualityImageTask!");
        imageEntry.requestedhighQualityImage = dataVersion;
        imageEntry.highQualityImageTask = this.mThreadPool.submit(new HighQualityScreenNailJob(mediaItem), new HighQualityScreenNailListener(mediaItem));
        return imageEntry.highQualityImageTask;
    }

    private void updateImageCache() {
        HashSet hashSet = new HashSet(this.mImageCache.keySet());
        for (int i = this.mActiveStart; i < this.mActiveEnd; i++) {
            MediaItem mediaItem = this.mData[i % 128];
            if (mediaItem != null) {
                Path path = mediaItem.getPath();
                ImageEntry imageEntry = this.mImageCache.get(path);
                hashSet.remove(path);
                if (imageEntry != null) {
                    if (Math.abs(i - this.mCurrentIndex) > 1) {
                        if (imageEntry.fullImageTask != null) {
                            imageEntry.fullImageTask.cancel();
                            imageEntry.fullImageTask = null;
                        }
                        imageEntry.fullImage = null;
                        imageEntry.requestedFullImage = -1L;
                    }
                    if (imageEntry.requestedScreenNail != mediaItem.getDataVersion() && (imageEntry.screenNail instanceof BitmapScreenNail)) {
                        ((BitmapScreenNail) imageEntry.screenNail).updatePlaceholderSize(mediaItem.getWidth(), mediaItem.getHeight());
                    }
                } else {
                    this.mImageCache.put(path, new ImageEntry());
                }
            }
        }
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            ImageEntry imageEntryRemove = this.mImageCache.remove((Path) it.next());
            if (imageEntryRemove.fullImageTask != null) {
                imageEntryRemove.fullImageTask.cancel();
            }
            if (imageEntryRemove.screenNailTask != null) {
                imageEntryRemove.screenNailTask.cancel();
            }
            if (imageEntryRemove.screenNail != null) {
                imageEntryRemove.screenNail.recycle();
            }
            if (imageEntryRemove.highQualityImageTask != null) {
                imageEntryRemove.highQualityImageTask.cancel();
            }
            if (imageEntryRemove.highQualityScreenNail != null) {
                imageEntryRemove.highQualityScreenNail.recycle();
            }
        }
        updateScreenNailUploadQueue();
    }

    private class FullImageListener implements FutureListener<BitmapRegionDecoder>, Runnable {
        private Future<BitmapRegionDecoder> mFuture;
        private final Path mPath;

        public FullImageListener(MediaItem mediaItem) {
            this.mPath = mediaItem.getPath();
        }

        @Override
        public void onFutureDone(Future<BitmapRegionDecoder> future) {
            this.mFuture = future;
            PhotoDataAdapter.this.mMainHandler.sendMessage(PhotoDataAdapter.this.mMainHandler.obtainMessage(3, this));
        }

        @Override
        public void run() {
            PhotoDataAdapter.this.updateFullImage(this.mPath, this.mFuture);
        }
    }

    private class ScreenNailListener implements FutureListener<ScreenNail>, Runnable {
        private Future<ScreenNail> mFuture;
        private final Path mPath;

        public ScreenNailListener(MediaItem mediaItem) {
            this.mPath = mediaItem.getPath();
        }

        @Override
        public void onFutureDone(Future<ScreenNail> future) {
            this.mFuture = future;
            PhotoDataAdapter.this.mMainHandler.sendMessage(PhotoDataAdapter.this.mMainHandler.obtainMessage(3, this));
        }

        @Override
        public void run() {
            PhotoDataAdapter.this.updateScreenNail(this.mPath, this.mFuture);
        }
    }

    private static class ImageEntry {
        public boolean failToLoad;
        public BitmapRegionDecoder fullImage;
        public Future<BitmapRegionDecoder> fullImageTask;
        public int height;
        public Future<ScreenNail> highQualityImageTask;
        public ScreenNail highQualityScreenNail;
        public long requestedFullImage;
        public long requestedScreenNail;
        public long requestedhighQualityImage;
        public ScreenNail screenNail;
        public Future<ScreenNail> screenNailTask;
        public int width;

        private ImageEntry() {
            this.requestedhighQualityImage = -1L;
            this.requestedScreenNail = -1L;
            this.requestedFullImage = -1L;
            this.failToLoad = false;
        }
    }

    private class SourceListener implements ContentListener {
        private SourceListener() {
        }

        @Override
        public void onContentDirty() {
            if (PhotoDataAdapter.this.mReloadTask != null) {
                PhotoDataAdapter.this.mReloadTask.notifyDirty();
            }
        }
    }

    private <T> T executeAndWait(Callable<T> callable) {
        FutureTask futureTask = new FutureTask(callable);
        this.mMainHandler.sendMessage(this.mMainHandler.obtainMessage(3, futureTask));
        try {
            return (T) futureTask.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e2) {
            throw new RuntimeException(e2);
        }
    }

    private static class UpdateInfo {
        public int contentEnd;
        public int contentStart;
        public int indexHint;
        public ArrayList<MediaItem> items;
        public boolean reloadContent;
        public int size;
        public Path target;
        public long version;

        private UpdateInfo() {
        }
    }

    private class GetUpdateInfo implements Callable<UpdateInfo> {
        private GetUpdateInfo() {
        }

        private boolean needContentReload() {
            int i = PhotoDataAdapter.this.mContentEnd;
            for (int i2 = PhotoDataAdapter.this.mContentStart; i2 < i; i2++) {
                if (PhotoDataAdapter.this.mData[i2 % 128] == null) {
                    return true;
                }
            }
            MediaItem mediaItem = PhotoDataAdapter.this.mData[PhotoDataAdapter.this.mCurrentIndex % 128];
            return mediaItem == null || mediaItem.getPath() != PhotoDataAdapter.this.mItemPath;
        }

        @Override
        public UpdateInfo call() throws Exception {
            UpdateInfo updateInfo = new UpdateInfo();
            updateInfo.version = PhotoDataAdapter.this.mSourceVersion;
            updateInfo.reloadContent = needContentReload();
            updateInfo.target = PhotoDataAdapter.this.mItemPath;
            updateInfo.indexHint = PhotoDataAdapter.this.mCurrentIndex;
            updateInfo.contentStart = PhotoDataAdapter.this.mContentStart;
            updateInfo.contentEnd = PhotoDataAdapter.this.mContentEnd;
            updateInfo.size = PhotoDataAdapter.this.mSize;
            return updateInfo;
        }
    }

    private class UpdateContent implements Callable<Void> {
        UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo updateInfo) {
            this.mUpdateInfo = updateInfo;
        }

        @Override
        public Void call() throws Exception {
            UpdateInfo updateInfo = this.mUpdateInfo;
            PhotoDataAdapter.this.mSourceVersion = updateInfo.version;
            if (updateInfo.size != PhotoDataAdapter.this.mSize) {
                PhotoDataAdapter.this.mSize = updateInfo.size;
                if (PhotoDataAdapter.this.mContentEnd > PhotoDataAdapter.this.mSize) {
                    PhotoDataAdapter.this.mContentEnd = PhotoDataAdapter.this.mSize;
                }
                if (PhotoDataAdapter.this.mActiveEnd > PhotoDataAdapter.this.mSize) {
                    PhotoDataAdapter.this.mActiveEnd = PhotoDataAdapter.this.mSize;
                }
            }
            if (updateInfo.target == PhotoDataAdapter.this.mItemPath) {
                int unused = PhotoDataAdapter.this.mCurrentIndex;
                PhotoDataAdapter.this.mCurrentIndex = updateInfo.indexHint;
            }
            if (PhotoDataAdapter.this.mSize > 0 && PhotoDataAdapter.this.mCurrentIndex >= PhotoDataAdapter.this.mSize) {
                PhotoDataAdapter.this.mCurrentIndex = PhotoDataAdapter.this.mSize - 1;
            }
            PhotoDataAdapter.this.updateSlidingWindow();
            if (updateInfo.items != null) {
                int iMax = Math.max(updateInfo.contentStart, PhotoDataAdapter.this.mContentStart);
                int iMin = Math.min(updateInfo.contentStart + updateInfo.items.size(), PhotoDataAdapter.this.mContentEnd);
                int i = iMax % 128;
                while (iMax < iMin) {
                    PhotoDataAdapter.this.mData[i] = updateInfo.items.get(iMax - updateInfo.contentStart);
                    i++;
                    if (i == 128) {
                        i = 0;
                    }
                    iMax++;
                }
            }
            MediaItem mediaItem = PhotoDataAdapter.this.mData[PhotoDataAdapter.this.mCurrentIndex % 128];
            PhotoDataAdapter.this.mItemPath = mediaItem == null ? null : mediaItem.getPath();
            PhotoDataAdapter.this.updateImageCache();
            PhotoDataAdapter.this.updateTileProvider();
            PhotoDataAdapter.this.updateImageRequests();
            if (PhotoDataAdapter.this.mDataListener != null) {
                PhotoDataAdapter.this.mDataListener.onPhotoChanged(PhotoDataAdapter.this.mCurrentIndex, PhotoDataAdapter.this.mItemPath);
            }
            PhotoDataAdapter.this.fireDataChange();
            return null;
        }
    }

    private class ReloadTask extends Thread {
        private volatile boolean mActive;
        private volatile boolean mDirty;
        private boolean mIsLoading;

        private ReloadTask() {
            this.mActive = true;
            this.mDirty = true;
            this.mIsLoading = false;
        }

        private void updateLoading(boolean z) {
            if (this.mIsLoading == z) {
                return;
            }
            this.mIsLoading = z;
            PhotoDataAdapter.this.mMainHandler.sendEmptyMessage(z ? 1 : 2);
        }

        @Override
        public void run() {
            int iFindIndexOfTarget;
            Log.d("Gallery2/PhotoDataAdapter", "<ReloadTask.run> begin, tid = " + getId());
            while (this.mActive) {
                synchronized (this) {
                    if (!this.mDirty && this.mActive) {
                        updateLoading(false);
                        Utils.waitWithoutInterrupt(this);
                    } else {
                        this.mDirty = false;
                        UpdateInfo updateInfo = (UpdateInfo) PhotoDataAdapter.this.executeAndWait(new GetUpdateInfo());
                        updateLoading(true);
                        if (updateInfo.version != PhotoDataAdapter.this.mSource.reload()) {
                            updateInfo.reloadContent = true;
                            updateInfo.size = PhotoDataAdapter.this.mSource.getMediaItemCount();
                            Log.d("Gallery2/PhotoDataAdapter", "<ReloadTask.run> set=" + PhotoDataAdapter.this.mSource + ", name=" + PhotoDataAdapter.this.mSource.getName() + ", item count=" + updateInfo.size + ", mSize=" + PhotoDataAdapter.this.mSize);
                        }
                        if (updateInfo.reloadContent) {
                            if (updateInfo.contentEnd == 0) {
                                updateInfo.items = PhotoDataAdapter.this.mSource.getMediaItem(updateInfo.contentStart, Math.min(updateInfo.size, 128));
                            } else {
                                updateInfo.items = PhotoDataAdapter.this.mSource.getMediaItem(updateInfo.contentStart, Math.min(updateInfo.size, (updateInfo.contentEnd - updateInfo.contentStart) + 1));
                            }
                            if (PhotoDataAdapter.this.mFocusHintPath != null) {
                                iFindIndexOfTarget = findIndexOfPathInCache(updateInfo, PhotoDataAdapter.this.mFocusHintPath);
                                PhotoDataAdapter.this.mFocusHintPath = null;
                            } else {
                                iFindIndexOfTarget = -1;
                            }
                            if (iFindIndexOfTarget == -1 && updateInfo.size != PhotoDataAdapter.this.mSize - 1) {
                                MediaItem mediaItemFindCurrentMediaItem = findCurrentMediaItem(updateInfo);
                                if (mediaItemFindCurrentMediaItem != null && mediaItemFindCurrentMediaItem.getPath() == updateInfo.target) {
                                    iFindIndexOfTarget = updateInfo.indexHint;
                                } else {
                                    iFindIndexOfTarget = findIndexOfTarget(updateInfo);
                                }
                            }
                            if (iFindIndexOfTarget == -1) {
                                iFindIndexOfTarget = updateInfo.indexHint;
                                if ((iFindIndexOfTarget != PhotoDataAdapter.this.mCameraIndex + 1 ? PhotoDataAdapter.this.mFocusHintDirection : 0) == 1 && iFindIndexOfTarget > 0) {
                                    iFindIndexOfTarget--;
                                }
                            }
                            if (updateInfo.size > 0 && iFindIndexOfTarget >= updateInfo.size) {
                                iFindIndexOfTarget = updateInfo.size - 1;
                            }
                            updateInfo.indexHint = iFindIndexOfTarget;
                            PhotoDataAdapter.this.executeAndWait(PhotoDataAdapter.this.new UpdateContent(updateInfo));
                        }
                    }
                }
            }
            Log.d("Gallery2/PhotoDataAdapter", "<ReloadTask.run> exit, tid = " + getId());
        }

        public synchronized void notifyDirty() {
            this.mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            Log.d("Gallery2/PhotoDataAdapter", "<ReloadTask.terminate>");
            this.mActive = false;
            if (PhotoDataAdapter.this.mSource != null) {
                PhotoDataAdapter.this.mSource.stopReload();
            }
            notifyAll();
        }

        private MediaItem findCurrentMediaItem(UpdateInfo updateInfo) {
            ArrayList<MediaItem> arrayList = updateInfo.items;
            int i = updateInfo.indexHint - updateInfo.contentStart;
            if (i < 0 || i >= arrayList.size()) {
                return null;
            }
            return arrayList.get(i);
        }

        private int findIndexOfTarget(UpdateInfo updateInfo) {
            int iFindIndexOfPathInCache;
            if (updateInfo.target == null) {
                return updateInfo.indexHint;
            }
            if (updateInfo.items != null && (iFindIndexOfPathInCache = findIndexOfPathInCache(updateInfo, updateInfo.target)) != -1) {
                return iFindIndexOfPathInCache;
            }
            if (updateInfo.size <= 5000) {
                return PhotoDataAdapter.this.mSource.getIndexOfItem(updateInfo.target, updateInfo.indexHint);
            }
            return getIndexOfItemQuickStop(updateInfo.target, updateInfo.indexHint);
        }

        private int findIndexOfPathInCache(UpdateInfo updateInfo, Path path) {
            ArrayList<MediaItem> arrayList = updateInfo.items;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                MediaItem mediaItem = arrayList.get(i);
                if (mediaItem != null && mediaItem.getPath() == path) {
                    return i + updateInfo.contentStart;
                }
            }
            return -1;
        }

        private int getIndexOfItemQuickStop(Path path, int i) {
            int iMax = Math.max(0, i - 100);
            int indexOf = PhotoDataAdapter.this.mSource.getIndexOf(path, PhotoDataAdapter.this.mSource.getMediaItem(iMax, 200));
            if (indexOf != -1) {
                return iMax + indexOf;
            }
            int i2 = iMax == 0 ? 200 : 0;
            ArrayList<MediaItem> mediaItem = PhotoDataAdapter.this.mSource.getMediaItem(i2, 200);
            while (this.mActive) {
                int indexOf2 = PhotoDataAdapter.this.mSource.getIndexOf(path, mediaItem);
                if (indexOf2 != -1) {
                    return i2 + indexOf2;
                }
                if (mediaItem.size() < 200) {
                    return -1;
                }
                i2 += 200;
                mediaItem = PhotoDataAdapter.this.mSource.getMediaItem(i2, 200);
            }
            Log.d("Gallery2/PhotoDataAdapter", "<ReloadTask.getIndexOfItemQuickStop> mActive = false, return MediaSet.INDEX_NOT_FOUND");
            return -1;
        }
    }

    private static class HighQualityScreenNailJob implements ThreadPool.Job<ScreenNail> {
        private MediaItem mItem;

        public HighQualityScreenNailJob(MediaItem mediaItem) {
            this.mItem = mediaItem;
        }

        @Override
        public ScreenNail run(ThreadPool.JobContext jobContext) {
            Bitmap bitmapRun;
            ThreadPool.Job<Bitmap> jobRequestImage = this.mItem.requestImage(4);
            if (jobRequestImage == null || (bitmapRun = jobRequestImage.run(jobContext)) == null) {
                return null;
            }
            BitmapScreenNail bitmapScreenNail = new BitmapScreenNail(bitmapRun, this.mItem);
            bitmapScreenNail.setDebugEnable(DebugUtils.DEBUG_HIGH_QUALITY_SCREENAIL);
            return bitmapScreenNail;
        }
    }

    private class HighQualityScreenNailListener implements FutureListener<ScreenNail>, Runnable {
        private Future<ScreenNail> mFuture;
        private final Path mPath;

        public HighQualityScreenNailListener(MediaItem mediaItem) {
            this.mPath = mediaItem.getPath();
        }

        @Override
        public void onFutureDone(Future<ScreenNail> future) {
            this.mFuture = future;
            PhotoDataAdapter.this.mMainHandler.sendMessage(PhotoDataAdapter.this.mMainHandler.obtainMessage(3, this));
        }

        @Override
        public void run() {
            PhotoDataAdapter.this.updateHighQualityScreenNail(this.mPath, this.mFuture);
        }
    }

    private void updateHighQualityScreenNail(Path path, Future<ScreenNail> future) {
        ImageEntry imageEntry = this.mImageCache.get(path);
        ScreenNail screenNailCombine = future.get();
        if (imageEntry == null || imageEntry.highQualityImageTask != future) {
            if (screenNailCombine != null) {
                screenNailCombine.recycle();
                return;
            }
            return;
        }
        imageEntry.highQualityImageTask = null;
        if (screenNailCombine == null) {
            return;
        }
        if (imageEntry.highQualityScreenNail instanceof BitmapScreenNail) {
            screenNailCombine = ((BitmapScreenNail) imageEntry.highQualityScreenNail).combine(screenNailCombine);
        }
        if (screenNailCombine != null) {
            imageEntry.highQualityScreenNail = screenNailCombine;
        }
        uploadScreenNail(0);
        if (imageEntry.highQualityScreenNail != null && path == getPath(this.mCurrentIndex)) {
            updateTileProvider(imageEntry);
            this.mPhotoView.notifyImageChange(0);
        }
        updateImageRequests();
    }

    public int getTotalCount() {
        return this.mSize;
    }
}
