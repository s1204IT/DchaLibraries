package com.android.gallery3d.app;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.TileImageViewAdapter;
import com.android.gallery3d.ui.TiledScreenNail;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class PhotoDataAdapter implements PhotoPage.Model {
    private static ImageFetch[] sImageFetchSeq = new ImageFetch[16];
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
    private final TileImageViewAdapter mTileProvider = new TileImageViewAdapter();
    private final MediaItem[] mData = new MediaItem[NotificationCompat.FLAG_LOCAL_ONLY];
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
    private boolean mUpdateComplete = false;
    private final SourceListener mSourceListener = new SourceListener();
    private boolean mNeedFullImage = true;

    public interface DataListener extends LoadingListener {
        void onPhotoChanged(int i, Path path);
    }

    private static class ImageFetch {
        int imageBit;
        int indexOffset;

        public ImageFetch(int offset, int bit) {
            this.indexOffset = offset;
            this.imageBit = bit;
        }
    }

    static {
        int k = 0 + 1;
        sImageFetchSeq[0] = new ImageFetch(0, 1);
        for (int i = 1; i < 7; i++) {
            int k2 = k + 1;
            sImageFetchSeq[k] = new ImageFetch(i, 1);
            k = k2 + 1;
            sImageFetchSeq[k2] = new ImageFetch(-i, 1);
        }
        int k3 = k + 1;
        sImageFetchSeq[k] = new ImageFetch(0, 2);
        int k4 = k3 + 1;
        sImageFetchSeq[k3] = new ImageFetch(1, 2);
        int i2 = k4 + 1;
        sImageFetchSeq[k4] = new ImageFetch(-1, 2);
    }

    public PhotoDataAdapter(AbstractGalleryActivity activity, PhotoView view, MediaSet mediaSet, Path itemPath, int indexHint, int cameraIndex, boolean isPanorama, boolean isStaticCamera) {
        this.mSource = (MediaSet) Utils.checkNotNull(mediaSet);
        this.mPhotoView = (PhotoView) Utils.checkNotNull(view);
        this.mItemPath = (Path) Utils.checkNotNull(itemPath);
        this.mCurrentIndex = indexHint;
        this.mCameraIndex = cameraIndex;
        this.mIsPanorama = isPanorama;
        this.mIsStaticCamera = isStaticCamera;
        this.mThreadPool = activity.getThreadPool();
        Arrays.fill(this.mChanges, -1L);
        this.mUploader = new TiledTexture.Uploader(activity.getGLRoot());
        this.mMainHandler = new SynchronizedHandler(activity.getGLRoot()) {
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
    }

    private MediaItem getItemInternal(int index) {
        if (index < 0 || index >= this.mSize || index < this.mContentStart || index >= this.mContentEnd) {
            return null;
        }
        return this.mData[index % NotificationCompat.FLAG_LOCAL_ONLY];
    }

    private long getVersion(int index) {
        MediaItem item = getItemInternal(index);
        if (item == null) {
            return -1L;
        }
        return item.getDataVersion();
    }

    private Path getPath(int index) {
        MediaItem item = getItemInternal(index);
        if (item == null) {
            return null;
        }
        return item.getPath();
    }

    private void fireDataChange() {
        boolean changed = false;
        for (int i = -3; i <= 3; i++) {
            long newVersion = getVersion(this.mCurrentIndex + i);
            if (this.mChanges[i + 3] != newVersion) {
                this.mChanges[i + 3] = newVersion;
                changed = true;
            }
        }
        if (changed) {
            int[] fromIndex = new int[7];
            Path[] oldPaths = new Path[7];
            System.arraycopy(this.mPaths, 0, oldPaths, 0, 7);
            for (int i2 = 0; i2 < 7; i2++) {
                this.mPaths[i2] = getPath((this.mCurrentIndex + i2) - 3);
            }
            for (int i3 = 0; i3 < 7; i3++) {
                Path p = this.mPaths[i3];
                if (p == null) {
                    fromIndex[i3] = Integer.MAX_VALUE;
                } else {
                    int j = 0;
                    while (j < 7 && oldPaths[j] != p) {
                        j++;
                    }
                    fromIndex[i3] = j < 7 ? j - 3 : Integer.MAX_VALUE;
                }
            }
            this.mPhotoView.notifyDataChange(fromIndex, -this.mCurrentIndex, (this.mSize - 1) - this.mCurrentIndex);
        }
    }

    public void setDataListener(DataListener listener) {
        this.mDataListener = listener;
    }

    private void updateScreenNail(Path path, Future<ScreenNail> future) {
        ImageEntry entry = this.mImageCache.get(path);
        ScreenNail screenNail = future.get();
        if (entry == null || entry.screenNailTask != future) {
            if (screenNail != null) {
                screenNail.recycle();
                return;
            }
            return;
        }
        entry.screenNailTask = null;
        if (entry.screenNail instanceof TiledScreenNail) {
            TiledScreenNail original = (TiledScreenNail) entry.screenNail;
            screenNail = original.combine(screenNail);
        }
        if (screenNail == null) {
            entry.failToLoad = true;
        } else {
            entry.failToLoad = false;
            entry.screenNail = screenNail;
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
                    updateTileProvider(entry);
                }
                this.mPhotoView.notifyImageChange(i);
            }
        }
        updateImageRequests();
        updateScreenNailUploadQueue();
    }

    private void updateFullImage(Path path, Future<BitmapRegionDecoder> future) {
        ImageEntry entry = this.mImageCache.get(path);
        if (entry == null || entry.fullImageTask != future) {
            BitmapRegionDecoder fullImage = future.get();
            if (fullImage != null) {
                fullImage.recycle();
                return;
            }
            return;
        }
        entry.fullImageTask = null;
        entry.fullImage = future.get();
        if (entry.fullImage != null && path == getPath(this.mCurrentIndex)) {
            updateTileProvider(entry);
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
        this.mReloadTask.terminate();
        this.mReloadTask = null;
        this.mSource.removeContentListener(this.mSourceListener);
        for (ImageEntry entry : this.mImageCache.values()) {
            if (entry.fullImageTask != null) {
                entry.fullImageTask.cancel();
            }
            if (entry.screenNailTask != null) {
                entry.screenNailTask.cancel();
            }
            if (entry.screenNail != null) {
                entry.screenNail.recycle();
            }
        }
        this.mImageCache.clear();
        this.mTileProvider.clear();
        this.mUploader.clear();
        TiledTexture.freeResources();
    }

    private MediaItem getItem(int index) {
        if (index < 0 || index >= this.mSize || !this.mIsActive) {
            return null;
        }
        Utils.assertTrue(index >= this.mActiveStart && index < this.mActiveEnd);
        if (index < this.mContentStart || index >= this.mContentEnd) {
            return null;
        }
        return this.mData[index % NotificationCompat.FLAG_LOCAL_ONLY];
    }

    private void updateCurrentIndex(int index) {
        if (this.mCurrentIndex != index) {
            this.mCurrentIndex = index;
            updateSlidingWindow();
            MediaItem item = this.mData[index % NotificationCompat.FLAG_LOCAL_ONLY];
            this.mItemPath = item == null ? null : item.getPath();
            updateImageCache();
            updateImageRequests();
            updateTileProvider();
            if (this.mDataListener != null) {
                this.mDataListener.onPhotoChanged(index, this.mItemPath);
            }
            fireDataChange();
        }
    }

    private void uploadScreenNail(int offset) {
        MediaItem item;
        ImageEntry e;
        TiledTexture t;
        int index = this.mCurrentIndex + offset;
        if (index >= this.mActiveStart && index < this.mActiveEnd && (item = getItem(index)) != null && (e = this.mImageCache.get(item.getPath())) != null) {
            ScreenNail s = e.screenNail;
            if (!(s instanceof TiledScreenNail) || (t = ((TiledScreenNail) s).getTexture()) == null || t.isReady()) {
                return;
            }
            this.mUploader.addTexture(t);
        }
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
    public void moveTo(int index) {
        updateCurrentIndex(index);
    }

    @Override
    public ScreenNail getScreenNail(int offset) {
        ImageEntry entry;
        int index = this.mCurrentIndex + offset;
        if (index < 0 || index >= this.mSize || !this.mIsActive) {
            return null;
        }
        Utils.assertTrue(index >= this.mActiveStart && index < this.mActiveEnd);
        MediaItem item = getItem(index);
        if (item != null && (entry = this.mImageCache.get(item.getPath())) != null) {
            if (entry.screenNail == null && !isCamera(offset)) {
                entry.screenNail = newPlaceholderScreenNail(item);
                if (offset == 0) {
                    updateTileProvider(entry);
                }
            }
            return entry.screenNail;
        }
        return null;
    }

    @Override
    public void getImageSize(int offset, PhotoView.Size size) {
        MediaItem item = getItem(this.mCurrentIndex + offset);
        if (item == null) {
            size.width = 0;
            size.height = 0;
        } else {
            size.width = item.getWidth();
            size.height = item.getHeight();
        }
    }

    @Override
    public int getImageRotation(int offset) {
        MediaItem item = getItem(this.mCurrentIndex + offset);
        if (item == null) {
            return 0;
        }
        return item.getFullImageRotation();
    }

    @Override
    public void setNeedFullImage(boolean enabled) {
        this.mNeedFullImage = enabled;
        this.mMainHandler.sendEmptyMessage(4);
    }

    @Override
    public boolean isCamera(int offset) {
        return this.mCurrentIndex + offset == this.mCameraIndex;
    }

    @Override
    public boolean isPanorama(int offset) {
        return isCamera(offset) && this.mIsPanorama;
    }

    @Override
    public boolean isStaticCamera(int offset) {
        return isCamera(offset) && this.mIsStaticCamera;
    }

    @Override
    public boolean isVideo(int offset) {
        MediaItem item = getItem(this.mCurrentIndex + offset);
        if (item == null) {
            return false;
        }
        return item.getMediaType() == 4 || BitmapUtils.isGifPicture(item.getMimeType());
    }

    @Override
    public boolean isDeletable(int offset) {
        MediaItem item = getItem(this.mCurrentIndex + offset);
        return (item == null || (item.getSupportedOperations() & 1) == 0) ? false : true;
    }

    @Override
    public int getLoadingState(int offset) {
        ImageEntry entry = this.mImageCache.get(getPath(this.mCurrentIndex + offset));
        if (entry == null) {
            return 0;
        }
        if (entry.failToLoad) {
            return 2;
        }
        return entry.screenNail != null ? 1 : 0;
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
    public Bitmap getTile(int level, int x, int y, int tileSize) {
        return this.mTileProvider.getTile(level, x, y, tileSize);
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
    public MediaItem getMediaItem(int offset) {
        int index = this.mCurrentIndex + offset;
        if (index < this.mContentStart || index >= this.mContentEnd) {
            return null;
        }
        return this.mData[index % NotificationCompat.FLAG_LOCAL_ONLY];
    }

    @Override
    public void setCurrentPhoto(Path path, int indexHint) {
        if (this.mItemPath != path) {
            this.mItemPath = path;
            this.mCurrentIndex = indexHint;
            updateSlidingWindow();
            updateImageCache();
            fireDataChange();
            MediaItem item = getMediaItem(0);
            if (item == null || item.getPath() == path || this.mReloadTask == null) {
                return;
            }
            this.mReloadTask.notifyDirty();
        }
    }

    @Override
    public void setFocusHintDirection(int direction) {
        this.mFocusHintDirection = direction;
    }

    @Override
    public void setFocusHintPath(Path path) {
        this.mFocusHintPath = path;
    }

    private void updateTileProvider() {
        ImageEntry entry = this.mImageCache.get(getPath(this.mCurrentIndex));
        if (entry == null) {
            this.mTileProvider.clear();
        } else {
            updateTileProvider(entry);
        }
    }

    private void updateTileProvider(ImageEntry entry) {
        ScreenNail screenNail = entry.screenNail;
        BitmapRegionDecoder fullImage = entry.fullImage;
        if (screenNail != null) {
            if (fullImage != null) {
                this.mTileProvider.setScreenNail(screenNail, fullImage.getWidth(), fullImage.getHeight());
                this.mTileProvider.setRegionDecoder(fullImage);
                return;
            } else {
                int width = screenNail.getWidth();
                int height = screenNail.getHeight();
                this.mTileProvider.setScreenNail(screenNail, width, height);
                return;
            }
        }
        this.mTileProvider.clear();
    }

    private void updateSlidingWindow() {
        int start = Utils.clamp(this.mCurrentIndex - 3, 0, Math.max(0, this.mSize - 7));
        int end = Math.min(this.mSize, start + 7);
        if (this.mActiveStart != start || this.mActiveEnd != end) {
            this.mActiveStart = start;
            this.mActiveEnd = end;
            int start2 = Utils.clamp(this.mCurrentIndex - 128, 0, Math.max(0, this.mSize - 256));
            int end2 = Math.min(this.mSize, start2 + NotificationCompat.FLAG_LOCAL_ONLY);
            if (this.mContentStart > this.mActiveStart || this.mContentEnd < this.mActiveEnd || Math.abs(start2 - this.mContentStart) > 16) {
                for (int i = this.mContentStart; i < this.mContentEnd; i++) {
                    if (i < start2 || i >= end2) {
                        this.mData[i % NotificationCompat.FLAG_LOCAL_ONLY] = null;
                    }
                }
                this.mContentStart = start2;
                this.mContentEnd = end2;
                if (this.mReloadTask != null) {
                    this.mReloadTask.notifyDirty();
                }
            }
        }
    }

    private void updateImageRequests() {
        if (this.mIsActive) {
            int currentIndex = this.mCurrentIndex;
            MediaItem item = this.mData[currentIndex % NotificationCompat.FLAG_LOCAL_ONLY];
            if (item != null && item.getPath() == this.mItemPath) {
                Future<?> task = null;
                for (int i = 0; i < sImageFetchSeq.length; i++) {
                    int offset = sImageFetchSeq[i].indexOffset;
                    int bit = sImageFetchSeq[i].imageBit;
                    if ((bit != 2 || this.mNeedFullImage) && (task = startTaskIfNeeded(currentIndex + offset, bit)) != null) {
                        break;
                    }
                }
                for (ImageEntry entry : this.mImageCache.values()) {
                    if (entry.screenNailTask != null && entry.screenNailTask != task) {
                        entry.screenNailTask.cancel();
                        entry.screenNailTask = null;
                        entry.requestedScreenNail = -1L;
                    }
                    if (entry.fullImageTask != null && entry.fullImageTask != task) {
                        entry.fullImageTask.cancel();
                        entry.fullImageTask = null;
                        entry.requestedFullImage = -1L;
                    }
                }
            }
        }
    }

    private class ScreenNailJob implements ThreadPool.Job<ScreenNail> {
        private MediaItem mItem;

        public ScreenNailJob(MediaItem item) {
            this.mItem = item;
        }

        @Override
        public ScreenNail run(ThreadPool.JobContext jc) {
            ScreenNail s = this.mItem.getScreenNail();
            if (s != null) {
                return s;
            }
            if (PhotoDataAdapter.this.isTemporaryItem(this.mItem)) {
                return PhotoDataAdapter.this.newPlaceholderScreenNail(this.mItem);
            }
            Bitmap bitmap = this.mItem.requestImage(1).run(jc);
            if (jc.isCancelled()) {
                return null;
            }
            if (bitmap != null) {
                bitmap = BitmapUtils.rotateBitmap(bitmap, this.mItem.getRotation() - this.mItem.getFullImageRotation(), true);
            }
            if (bitmap != null) {
                return new TiledScreenNail(bitmap);
            }
            return null;
        }
    }

    private class FullImageJob implements ThreadPool.Job<BitmapRegionDecoder> {
        private MediaItem mItem;

        public FullImageJob(MediaItem item) {
            this.mItem = item;
        }

        @Override
        public BitmapRegionDecoder run(ThreadPool.JobContext jc) {
            if (PhotoDataAdapter.this.isTemporaryItem(this.mItem)) {
                return null;
            }
            return this.mItem.requestLargeImage().run(jc);
        }
    }

    private boolean isTemporaryItem(MediaItem mediaItem) {
        if (this.mCameraIndex < 0 || !(mediaItem instanceof LocalMediaItem)) {
            return false;
        }
        LocalMediaItem item = (LocalMediaItem) mediaItem;
        return item.getBucketId() == MediaSetUtils.CAMERA_BUCKET_ID && item.getSize() == 0 && item.getWidth() != 0 && item.getHeight() != 0 && item.getDateInMs() - System.currentTimeMillis() <= 10000;
    }

    private ScreenNail newPlaceholderScreenNail(MediaItem item) {
        int width = item.getWidth();
        int height = item.getHeight();
        return new TiledScreenNail(width, height);
    }

    private Future<?> startTaskIfNeeded(int index, int which) {
        if (index < this.mActiveStart || index >= this.mActiveEnd) {
            return null;
        }
        ImageEntry entry = this.mImageCache.get(getPath(index));
        if (entry == null) {
            return null;
        }
        MediaItem item = this.mData[index % NotificationCompat.FLAG_LOCAL_ONLY];
        Utils.assertTrue(item != null);
        long version = item.getDataVersion();
        if (which == 1 && entry.screenNailTask != null && entry.requestedScreenNail == version) {
            return entry.screenNailTask;
        }
        if (which == 2 && entry.fullImageTask != null && entry.requestedFullImage == version) {
            return entry.fullImageTask;
        }
        if (which == 1 && entry.requestedScreenNail != version) {
            entry.requestedScreenNail = version;
            entry.screenNailTask = this.mThreadPool.submit(new ScreenNailJob(item), new ScreenNailListener(item));
            return entry.screenNailTask;
        }
        if (which != 2 || entry.requestedFullImage == version || (item.getSupportedOperations() & 64) == 0) {
            return null;
        }
        entry.requestedFullImage = version;
        entry.fullImageTask = this.mThreadPool.submit(new FullImageJob(item), new FullImageListener(item));
        return entry.fullImageTask;
    }

    private void updateImageCache() {
        HashSet<Path> toBeRemoved = new HashSet<>(this.mImageCache.keySet());
        for (int i = this.mActiveStart; i < this.mActiveEnd; i++) {
            MediaItem item = this.mData[i % NotificationCompat.FLAG_LOCAL_ONLY];
            if (item != null) {
                Path path = item.getPath();
                ImageEntry entry = this.mImageCache.get(path);
                toBeRemoved.remove(path);
                if (entry != null) {
                    if (Math.abs(i - this.mCurrentIndex) > 1) {
                        if (entry.fullImageTask != null) {
                            entry.fullImageTask.cancel();
                            entry.fullImageTask = null;
                        }
                        entry.fullImage = null;
                        entry.requestedFullImage = -1L;
                    }
                    if (entry.requestedScreenNail != item.getDataVersion() && (entry.screenNail instanceof TiledScreenNail)) {
                        TiledScreenNail s = (TiledScreenNail) entry.screenNail;
                        s.updatePlaceholderSize(item.getWidth(), item.getHeight());
                    }
                } else {
                    this.mImageCache.put(path, new ImageEntry());
                }
            }
        }
        Iterator<Path> it = toBeRemoved.iterator();
        while (it.hasNext()) {
            ImageEntry entry2 = this.mImageCache.remove(it.next());
            if (entry2.fullImageTask != null) {
                entry2.fullImageTask.cancel();
            }
            if (entry2.screenNailTask != null) {
                entry2.screenNailTask.cancel();
            }
            if (entry2.screenNail != null) {
                entry2.screenNail.recycle();
            }
        }
        updateScreenNailUploadQueue();
    }

    private class FullImageListener implements FutureListener<BitmapRegionDecoder>, Runnable {
        private Future<BitmapRegionDecoder> mFuture;
        private final Path mPath;

        public FullImageListener(MediaItem item) {
            this.mPath = item.getPath();
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

        public ScreenNailListener(MediaItem item) {
            this.mPath = item.getPath();
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
        public long requestedFullImage;
        public long requestedScreenNail;
        public ScreenNail screenNail;
        public Future<ScreenNail> screenNailTask;

        private ImageEntry() {
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
        FutureTask<T> task = new FutureTask<>(callable);
        this.mMainHandler.sendMessage(this.mMainHandler.obtainMessage(3, task));
        try {
            return task.get();
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
            int n = PhotoDataAdapter.this.mContentEnd;
            for (int i = PhotoDataAdapter.this.mContentStart; i < n; i++) {
                if (PhotoDataAdapter.this.mData[i % NotificationCompat.FLAG_LOCAL_ONLY] == null) {
                    return true;
                }
            }
            MediaItem current = PhotoDataAdapter.this.mData[PhotoDataAdapter.this.mCurrentIndex % NotificationCompat.FLAG_LOCAL_ONLY];
            return current == null || current.getPath() != PhotoDataAdapter.this.mItemPath;
        }

        @Override
        public UpdateInfo call() throws Exception {
            UpdateInfo info = new UpdateInfo();
            info.version = PhotoDataAdapter.this.mSourceVersion;
            info.reloadContent = needContentReload();
            info.target = PhotoDataAdapter.this.mItemPath;
            info.indexHint = PhotoDataAdapter.this.mCurrentIndex;
            info.contentStart = PhotoDataAdapter.this.mContentStart;
            info.contentEnd = PhotoDataAdapter.this.mContentEnd;
            info.size = PhotoDataAdapter.this.mSize;
            return info;
        }
    }

    private class UpdateContent implements Callable<Void> {
        UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo updateInfo) {
            this.mUpdateInfo = updateInfo;
        }

        @Override
        public Void call() throws Exception {
            UpdateInfo info = this.mUpdateInfo;
            PhotoDataAdapter.this.mSourceVersion = info.version;
            if (info.size != PhotoDataAdapter.this.mSize) {
                PhotoDataAdapter.this.mUpdateComplete = false;
                PhotoDataAdapter.this.mSize = info.size;
                if (PhotoDataAdapter.this.mContentEnd > PhotoDataAdapter.this.mSize) {
                    PhotoDataAdapter.this.mContentEnd = PhotoDataAdapter.this.mSize;
                }
                if (PhotoDataAdapter.this.mActiveEnd > PhotoDataAdapter.this.mSize) {
                    PhotoDataAdapter.this.mActiveEnd = PhotoDataAdapter.this.mSize;
                }
            } else {
                PhotoDataAdapter.this.mUpdateComplete = true;
            }
            PhotoDataAdapter.this.mCurrentIndex = info.indexHint;
            PhotoDataAdapter.this.updateSlidingWindow();
            if (info.items != null) {
                int start = Math.max(info.contentStart, PhotoDataAdapter.this.mContentStart);
                int end = Math.min(info.contentStart + info.items.size(), PhotoDataAdapter.this.mContentEnd);
                int dataIndex = start % NotificationCompat.FLAG_LOCAL_ONLY;
                for (int i = start; i < end; i++) {
                    PhotoDataAdapter.this.mData[dataIndex] = info.items.get(i - info.contentStart);
                    dataIndex++;
                    if (dataIndex == 256) {
                        dataIndex = 0;
                    }
                }
            }
            MediaItem current = PhotoDataAdapter.this.mData[PhotoDataAdapter.this.mCurrentIndex % NotificationCompat.FLAG_LOCAL_ONLY];
            PhotoDataAdapter.this.mItemPath = current == null ? null : current.getPath();
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

        private void updateLoading(boolean loading) {
            if (this.mIsLoading != loading) {
                this.mIsLoading = loading;
                PhotoDataAdapter.this.mMainHandler.sendEmptyMessage(loading ? 1 : 2);
            }
        }

        @Override
        public void run() {
            PhotoDataAdapter.this.mUpdateComplete = false;
            while (this.mActive) {
                synchronized (this) {
                    if (!this.mDirty && this.mActive && PhotoDataAdapter.this.mUpdateComplete) {
                        updateLoading(false);
                        Utils.waitWithoutInterrupt(this);
                    } else {
                        this.mDirty = false;
                        UpdateInfo info = (UpdateInfo) PhotoDataAdapter.this.executeAndWait(new GetUpdateInfo());
                        updateLoading(true);
                        long version = PhotoDataAdapter.this.mSource.reload();
                        if (info.version != version) {
                            info.reloadContent = true;
                            info.size = PhotoDataAdapter.this.mSource.getMediaItemCount();
                        }
                        if (info.reloadContent) {
                            info.items = PhotoDataAdapter.this.mSource.getMediaItem(info.contentStart, info.contentEnd);
                            int index = -1;
                            if (PhotoDataAdapter.this.mFocusHintPath != null) {
                                index = findIndexOfPathInCache(info, PhotoDataAdapter.this.mFocusHintPath);
                                PhotoDataAdapter.this.mFocusHintPath = null;
                            }
                            if (index == -1) {
                                MediaItem item = findCurrentMediaItem(info);
                                if (item != null && item.getPath() == info.target) {
                                    index = info.indexHint;
                                } else {
                                    index = findIndexOfTarget(info);
                                }
                            }
                            if (index == -1) {
                                index = info.indexHint;
                                int focusHintDirection = PhotoDataAdapter.this.mFocusHintDirection;
                                if (index == PhotoDataAdapter.this.mCameraIndex + 1) {
                                    focusHintDirection = 0;
                                }
                                if (focusHintDirection == 1 && index > 0) {
                                    index--;
                                }
                            }
                            if (PhotoDataAdapter.this.mSize > 0 && index >= PhotoDataAdapter.this.mSize) {
                                index = PhotoDataAdapter.this.mSize - 1;
                            }
                            info.indexHint = index;
                            PhotoDataAdapter.this.executeAndWait(PhotoDataAdapter.this.new UpdateContent(info));
                        }
                    }
                }
            }
        }

        public synchronized void notifyDirty() {
            this.mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            this.mActive = false;
            notifyAll();
        }

        private MediaItem findCurrentMediaItem(UpdateInfo info) {
            ArrayList<MediaItem> items = info.items;
            int index = info.indexHint - info.contentStart;
            if (index < 0 || index >= items.size()) {
                return null;
            }
            return items.get(index);
        }

        private int findIndexOfTarget(UpdateInfo info) {
            int i;
            if (info.target == null) {
                return info.indexHint;
            }
            ArrayList<MediaItem> items = info.items;
            if (items == null || (i = findIndexOfPathInCache(info, info.target)) == -1) {
                return PhotoDataAdapter.this.mSource.getIndexOfItem(info.target, info.indexHint);
            }
            return i;
        }

        private int findIndexOfPathInCache(UpdateInfo info, Path path) {
            ArrayList<MediaItem> items = info.items;
            int n = items.size();
            for (int i = 0; i < n; i++) {
                MediaItem item = items.get(i);
                if (item != null && item.getPath() == path) {
                    return info.contentStart + i;
                }
            }
            return -1;
        }
    }
}
