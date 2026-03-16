package com.android.gallery3d.app;

import android.graphics.Bitmap;
import com.android.gallery3d.app.SlideshowPage;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import java.util.LinkedList;
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

    public interface SlideshowSource {
        void addContentListener(ContentListener contentListener);

        int findItemIndex(Path path, int i);

        MediaItem getMediaItem(int i);

        long reload();

        void removeContentListener(ContentListener contentListener);
    }

    static int access$604(SlideshowDataAdapter x0) {
        int i = x0.mLoadIndex + 1;
        x0.mLoadIndex = i;
        return i;
    }

    public SlideshowDataAdapter(GalleryContext context, SlideshowSource source, int index, Path initialPath) {
        this.mLoadIndex = 0;
        this.mNextOutput = 0;
        this.mSource = source;
        this.mInitialPath = initialPath;
        this.mLoadIndex = index;
        this.mNextOutput = index;
        this.mThreadPool = context.getThreadPool();
    }

    private MediaItem loadItem() {
        if (this.mNeedReload.compareAndSet(true, false)) {
            long v = this.mSource.reload();
            if (v != this.mDataVersion) {
                this.mDataVersion = v;
                this.mNeedReset = true;
                return null;
            }
        }
        int index = this.mLoadIndex;
        if (this.mInitialPath != null) {
            index = this.mSource.findItemIndex(this.mInitialPath, index);
            this.mInitialPath = null;
        }
        return this.mSource.getMediaItem(index);
    }

    private class ReloadTask implements ThreadPool.Job<Void> {
        private ReloadTask() {
        }

        @Override
        public Void run(ThreadPool.JobContext jc) {
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
                MediaItem item = SlideshowDataAdapter.this.loadItem();
                if (SlideshowDataAdapter.this.mNeedReset) {
                    synchronized (SlideshowDataAdapter.this) {
                        SlideshowDataAdapter.this.mImageQueue.clear();
                        SlideshowDataAdapter.this.mLoadIndex = SlideshowDataAdapter.this.mNextOutput;
                    }
                } else if (item == null) {
                    synchronized (SlideshowDataAdapter.this) {
                        if (!SlideshowDataAdapter.this.mNeedReload.get()) {
                            SlideshowDataAdapter.this.mDataReady = false;
                        }
                        SlideshowDataAdapter.this.notifyAll();
                    }
                } else {
                    Bitmap bitmap = item.requestImage(1).run(jc);
                    if (bitmap != null) {
                        synchronized (SlideshowDataAdapter.this) {
                            SlideshowDataAdapter.this.mImageQueue.addLast(new SlideshowPage.Slide(item, SlideshowDataAdapter.this.mLoadIndex, bitmap));
                            if (SlideshowDataAdapter.this.mImageQueue.size() == 1) {
                                SlideshowDataAdapter.this.notifyAll();
                            }
                        }
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
        SlideshowPage.Slide slideRemoveFirst;
        while (this.mIsActive && this.mDataReady && this.mImageQueue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new AssertionError();
            }
        }
        if (this.mImageQueue.isEmpty()) {
            slideRemoveFirst = null;
        } else {
            this.mNextOutput++;
            notifyAll();
            slideRemoveFirst = this.mImageQueue.removeFirst();
        }
        return slideRemoveFirst;
    }

    @Override
    public Future<SlideshowPage.Slide> nextSlide(FutureListener<SlideshowPage.Slide> listener) {
        return this.mThreadPool.submit(new ThreadPool.Job<SlideshowPage.Slide>() {
            @Override
            public SlideshowPage.Slide run(ThreadPool.JobContext jc) {
                jc.setMode(0);
                return SlideshowDataAdapter.this.innerNextBitmap();
            }
        }, listener);
    }

    @Override
    public void pause() {
        synchronized (this) {
            this.mIsActive = false;
            notifyAll();
        }
        this.mSource.removeContentListener(this.mSourceListener);
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
