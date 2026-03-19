package com.android.gallery3d.app;

import android.graphics.Bitmap;
import com.android.gallery3d.app.SlideshowPage;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SlideshowDataAdapter implements SlideshowPage.Model {
    private boolean mDataReady;
    private Path mInitialPath;
    private int mLoadIndex;
    private boolean mNeedReset;
    private int mNextOutput;
    private Future<Void> mReloadTask;
    private final SlideshowSource mSource;
    private final ThreadPool mThreadPool;
    private boolean mIsActive = false;
    private final LinkedList<SlideshowPage.Slide> mImageQueue = new LinkedList<>();
    private long mDataVersion = -1;
    private final AtomicBoolean mNeedReload = new AtomicBoolean(false);
    private final SourceListener mSourceListener = new SourceListener();
    private Set<String> mUriOfObtainBitmapItem = new HashSet();
    private Set<String> mUriNotGetBitmapItem = new HashSet();

    public interface SlideshowSource {
        void addContentListener(ContentListener contentListener);

        int findItemIndex(Path path, int i);

        MediaItem getMediaItem(int i);

        long reload();

        void removeContentListener(ContentListener contentListener);

        void stopReload();
    }

    static int access$604(SlideshowDataAdapter slideshowDataAdapter) {
        int i = slideshowDataAdapter.mLoadIndex + 1;
        slideshowDataAdapter.mLoadIndex = i;
        return i;
    }

    public SlideshowDataAdapter(GalleryContext galleryContext, SlideshowSource slideshowSource, int i, Path path) {
        this.mLoadIndex = 0;
        this.mNextOutput = 0;
        this.mSource = slideshowSource;
        this.mInitialPath = path;
        this.mLoadIndex = i;
        this.mNextOutput = i;
        this.mThreadPool = galleryContext.getThreadPool();
    }

    private MediaItem loadItem() {
        if (this.mNeedReload.compareAndSet(true, false)) {
            long jReload = this.mSource.reload();
            if (jReload != this.mDataVersion) {
                this.mDataVersion = jReload;
                this.mNeedReset = true;
                return null;
            }
        }
        int iFindItemIndex = this.mLoadIndex;
        if (this.mInitialPath != null) {
            iFindItemIndex = this.mSource.findItemIndex(this.mInitialPath, iFindItemIndex);
            this.mInitialPath = null;
        }
        return this.mSource.getMediaItem(iFindItemIndex);
    }

    private class ReloadTask implements ThreadPool.Job<Void> {
        private ReloadTask() {
        }

        @Override
        public Void run(ThreadPool.JobContext jobContext) {
            while (true) {
                synchronized (SlideshowDataAdapter.this) {
                    while (SlideshowDataAdapter.this.mIsActive && (!SlideshowDataAdapter.this.mDataReady || SlideshowDataAdapter.this.mImageQueue.size() >= 3)) {
                        try {
                            SlideshowDataAdapter.this.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                if (!SlideshowDataAdapter.this.mIsActive) {
                    return null;
                }
                SlideshowDataAdapter.this.mNeedReset = false;
                MediaItem mediaItemLoadItem = SlideshowDataAdapter.this.loadItem();
                if (SlideshowDataAdapter.this.mNeedReset) {
                    synchronized (SlideshowDataAdapter.this) {
                        SlideshowDataAdapter.this.mImageQueue.clear();
                        SlideshowDataAdapter.this.mLoadIndex = SlideshowDataAdapter.this.mNextOutput;
                        SlideshowDataAdapter.this.mUriNotGetBitmapItem.clear();
                        SlideshowDataAdapter.this.mUriOfObtainBitmapItem.clear();
                    }
                } else if (mediaItemLoadItem == null) {
                    synchronized (SlideshowDataAdapter.this) {
                        if (!SlideshowDataAdapter.this.mNeedReload.get()) {
                            SlideshowDataAdapter.this.mDataReady = false;
                        }
                        SlideshowDataAdapter.this.notifyAll();
                    }
                } else {
                    Bitmap bitmapRun = mediaItemLoadItem.requestImage(1).run(jobContext);
                    if (bitmapRun != null) {
                        synchronized (SlideshowDataAdapter.this) {
                            SlideshowDataAdapter.this.mImageQueue.addLast(new SlideshowPage.Slide(mediaItemLoadItem, SlideshowDataAdapter.this.mLoadIndex, bitmapRun));
                            if (SlideshowDataAdapter.this.mImageQueue.size() == 1) {
                                SlideshowDataAdapter.this.notifyAll();
                            }
                        }
                        SlideshowDataAdapter.this.mUriOfObtainBitmapItem.add(mediaItemLoadItem.getContentUri().toString());
                    } else if (SlideshowDataAdapter.this.mUriNotGetBitmapItem.contains(mediaItemLoadItem.getContentUri().toString())) {
                        if (SlideshowDataAdapter.this.mUriOfObtainBitmapItem.size() == 0) {
                            synchronized (SlideshowDataAdapter.this) {
                                SlideshowDataAdapter.this.mImageQueue.addLast(new SlideshowPage.Slide(mediaItemLoadItem, -1, null));
                                if (SlideshowDataAdapter.this.mImageQueue.size() == 1) {
                                    SlideshowDataAdapter.this.notifyAll();
                                }
                            }
                        }
                    } else {
                        SlideshowDataAdapter.this.mUriNotGetBitmapItem.add(mediaItemLoadItem.getContentUri().toString());
                    }
                    SlideshowDataAdapter.access$604(SlideshowDataAdapter.this);
                }
            }
        }
    }

    private class SourceListener implements ContentListener {
        private SourceListener() {
        }

        @Override
        public void onContentDirty() {
            synchronized (SlideshowDataAdapter.this) {
                SlideshowDataAdapter.this.mNeedReload.set(true);
                SlideshowDataAdapter.this.mDataReady = true;
                SlideshowDataAdapter.this.notifyAll();
            }
        }
    }

    private synchronized SlideshowPage.Slide innerNextBitmap() {
        while (this.mIsActive && this.mDataReady && this.mImageQueue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new AssertionError();
            }
        }
        if (this.mImageQueue.isEmpty()) {
            return null;
        }
        this.mNextOutput++;
        notifyAll();
        return this.mImageQueue.removeFirst();
    }

    @Override
    public Future<SlideshowPage.Slide> nextSlide(FutureListener<SlideshowPage.Slide> futureListener) {
        return this.mThreadPool.submit(new ThreadPool.Job<SlideshowPage.Slide>() {
            @Override
            public SlideshowPage.Slide run(ThreadPool.JobContext jobContext) {
                jobContext.setMode(0);
                return SlideshowDataAdapter.this.innerNextBitmap();
            }
        }, futureListener);
    }

    @Override
    public void pause() {
        synchronized (this) {
            this.mIsActive = false;
            notifyAll();
        }
        this.mSource.removeContentListener(this.mSourceListener);
        this.mSource.stopReload();
        this.mReloadTask.cancel();
        this.mReloadTask.waitDone();
        this.mReloadTask = null;
    }

    @Override
    public synchronized void resume() {
        this.mIsActive = true;
        this.mSource.addContentListener(this.mSourceListener);
        this.mNeedReload.set(true);
        this.mDataReady = true;
        this.mReloadTask = this.mThreadPool.submit(new ReloadTask());
    }
}
