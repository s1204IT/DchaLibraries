package com.android.gallery3d.app;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.SynchronizedHandler;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class AlbumSetDataLoader {
    private final MediaItem[] mCoverItem;
    private final MediaSet[] mData;
    private DataListener mDataListener;
    private final long[] mItemVersion;
    private LoadingListener mLoadingListener;
    private final Handler mMainHandler;
    private ReloadTask mReloadTask;
    private final long[] mSetVersion;
    private int mSize;
    private final MediaSet mSource;
    private final int[] mTotalCount;
    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private int mContentStart = 0;
    private int mContentEnd = 0;
    private long mSourceVersion = -1;
    private final MySourceListener mSourceListener = new MySourceListener();

    public interface DataListener {
        void onContentChanged(int i);

        void onSizeChanged(int i);
    }

    public AlbumSetDataLoader(AbstractGalleryActivity activity, MediaSet albumSet, int cacheSize) {
        this.mSource = (MediaSet) Utils.checkNotNull(albumSet);
        this.mCoverItem = new MediaItem[cacheSize];
        this.mData = new MediaSet[cacheSize];
        this.mTotalCount = new int[cacheSize];
        this.mItemVersion = new long[cacheSize];
        this.mSetVersion = new long[cacheSize];
        Arrays.fill(this.mItemVersion, -1L);
        Arrays.fill(this.mSetVersion, -1L);
        this.mMainHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        if (AlbumSetDataLoader.this.mLoadingListener != null) {
                            AlbumSetDataLoader.this.mLoadingListener.onLoadingStarted();
                        }
                        break;
                    case 2:
                        if (AlbumSetDataLoader.this.mLoadingListener != null) {
                            AlbumSetDataLoader.this.mLoadingListener.onLoadingFinished(false);
                        }
                        break;
                    case 3:
                        ((Runnable) message.obj).run();
                        break;
                }
            }
        };
    }

    public void pause() {
        this.mReloadTask.terminate();
        this.mReloadTask = null;
        this.mSource.removeContentListener(this.mSourceListener);
    }

    public void resume() {
        this.mSource.addContentListener(this.mSourceListener);
        this.mReloadTask = new ReloadTask();
        this.mReloadTask.start();
    }

    private void assertIsActive(int index) {
        if (index < this.mActiveStart || index >= this.mActiveEnd) {
            throw new IllegalArgumentException(String.format("%s not in (%s, %s)", Integer.valueOf(index), Integer.valueOf(this.mActiveStart), Integer.valueOf(this.mActiveEnd)));
        }
    }

    public MediaSet getMediaSet(int index) {
        assertIsActive(index);
        return this.mData[index % this.mData.length];
    }

    public MediaItem getCoverItem(int index) {
        assertIsActive(index);
        return this.mCoverItem[index % this.mCoverItem.length];
    }

    public int getTotalCount(int index) {
        assertIsActive(index);
        return this.mTotalCount[index % this.mTotalCount.length];
    }

    public int size() {
        return this.mSize;
    }

    public int findSet(Path id) {
        int length = this.mData.length;
        for (int i = this.mContentStart; i < this.mContentEnd; i++) {
            MediaSet set = this.mData[i % length];
            if (set != null && id == set.getPath()) {
                return i;
            }
        }
        return -1;
    }

    private void clearSlot(int slotIndex) {
        this.mData[slotIndex] = null;
        this.mCoverItem[slotIndex] = null;
        this.mTotalCount[slotIndex] = 0;
        this.mItemVersion[slotIndex] = -1;
        this.mSetVersion[slotIndex] = -1;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart != this.mContentStart || contentEnd != this.mContentEnd) {
            int length = this.mCoverItem.length;
            int start = this.mContentStart;
            int end = this.mContentEnd;
            this.mContentStart = contentStart;
            this.mContentEnd = contentEnd;
            if (contentStart >= end || start >= contentEnd) {
                for (int i = start; i < end; i++) {
                    clearSlot(i % length);
                }
            } else {
                for (int i2 = start; i2 < contentStart; i2++) {
                    clearSlot(i2 % length);
                }
                for (int i3 = contentEnd; i3 < end; i3++) {
                    clearSlot(i3 % length);
                }
            }
            this.mReloadTask.notifyDirty();
        }
    }

    public void setActiveWindow(int start, int end) {
        if (start != this.mActiveStart || end != this.mActiveEnd) {
            Utils.assertTrue(start <= end && end - start <= this.mCoverItem.length && end <= this.mSize);
            this.mActiveStart = start;
            this.mActiveEnd = end;
            int length = this.mCoverItem.length;
            if (start != end) {
                int contentStart = Utils.clamp(((start + end) / 2) - (length / 2), 0, Math.max(0, this.mSize - length));
                int contentEnd = Math.min(contentStart + length, this.mSize);
                if (this.mContentStart > start || this.mContentEnd < end || Math.abs(contentStart - this.mContentStart) > 4) {
                    setContentWindow(contentStart, contentEnd);
                }
            }
        }
    }

    private class MySourceListener implements ContentListener {
        private MySourceListener() {
        }

        @Override
        public void onContentDirty() {
            AlbumSetDataLoader.this.mReloadTask.notifyDirty();
        }
    }

    public void setModelListener(DataListener listener) {
        this.mDataListener = listener;
    }

    public void setLoadingListener(LoadingListener listener) {
        this.mLoadingListener = listener;
    }

    private static class UpdateInfo {
        public MediaItem cover;
        public int index;
        public MediaSet item;
        public int size;
        public int totalCount;
        public long version;

        private UpdateInfo() {
        }
    }

    private class GetUpdateInfo implements Callable<UpdateInfo> {
        private final long mVersion;

        public GetUpdateInfo(long version) {
            this.mVersion = version;
        }

        private int getInvalidIndex(long version) {
            long[] setVersion = AlbumSetDataLoader.this.mSetVersion;
            int length = setVersion.length;
            int n = AlbumSetDataLoader.this.mContentEnd;
            for (int i = AlbumSetDataLoader.this.mContentStart; i < n; i++) {
                int i2 = i % length;
                if (setVersion[i % length] != version) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public UpdateInfo call() throws Exception {
            int index = getInvalidIndex(this.mVersion);
            if (index == -1 && AlbumSetDataLoader.this.mSourceVersion == this.mVersion) {
                return null;
            }
            UpdateInfo info = new UpdateInfo();
            info.version = AlbumSetDataLoader.this.mSourceVersion;
            info.index = index;
            info.size = AlbumSetDataLoader.this.mSize;
            return info;
        }
    }

    private class UpdateContent implements Callable<Void> {
        private final UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo info) {
            this.mUpdateInfo = info;
        }

        @Override
        public Void call() {
            if (AlbumSetDataLoader.this.mReloadTask != null) {
                UpdateInfo info = this.mUpdateInfo;
                AlbumSetDataLoader.this.mSourceVersion = info.version;
                if (AlbumSetDataLoader.this.mSize != info.size) {
                    AlbumSetDataLoader.this.mSize = info.size;
                    if (AlbumSetDataLoader.this.mDataListener != null) {
                        AlbumSetDataLoader.this.mDataListener.onSizeChanged(AlbumSetDataLoader.this.mSize);
                    }
                    if (AlbumSetDataLoader.this.mContentEnd > AlbumSetDataLoader.this.mSize) {
                        AlbumSetDataLoader.this.mContentEnd = AlbumSetDataLoader.this.mSize;
                    }
                    if (AlbumSetDataLoader.this.mActiveEnd > AlbumSetDataLoader.this.mSize) {
                        AlbumSetDataLoader.this.mActiveEnd = AlbumSetDataLoader.this.mSize;
                    }
                }
                if (info.index >= AlbumSetDataLoader.this.mContentStart && info.index < AlbumSetDataLoader.this.mContentEnd) {
                    int pos = info.index % AlbumSetDataLoader.this.mCoverItem.length;
                    AlbumSetDataLoader.this.mSetVersion[pos] = info.version;
                    long itemVersion = info.item.getDataVersion();
                    if (AlbumSetDataLoader.this.mItemVersion[pos] != itemVersion) {
                        AlbumSetDataLoader.this.mItemVersion[pos] = itemVersion;
                        AlbumSetDataLoader.this.mData[pos] = info.item;
                        AlbumSetDataLoader.this.mCoverItem[pos] = info.cover;
                        AlbumSetDataLoader.this.mTotalCount[pos] = info.totalCount;
                        if (AlbumSetDataLoader.this.mDataListener != null && info.index >= AlbumSetDataLoader.this.mActiveStart && info.index < AlbumSetDataLoader.this.mActiveEnd) {
                            AlbumSetDataLoader.this.mDataListener.onContentChanged(info.index);
                        }
                    }
                }
            }
            return null;
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

    private class ReloadTask extends Thread {
        private volatile boolean mActive;
        private volatile boolean mDirty;
        private volatile boolean mIsLoading;

        private ReloadTask() {
            this.mActive = true;
            this.mDirty = true;
            this.mIsLoading = false;
        }

        private void updateLoading(boolean loading) {
            if (this.mIsLoading != loading) {
                this.mIsLoading = loading;
                AlbumSetDataLoader.this.mMainHandler.sendEmptyMessage(loading ? 1 : 2);
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            boolean updateComplete = false;
            while (this.mActive) {
                synchronized (this) {
                    if (this.mActive && !this.mDirty && updateComplete) {
                        if (!AlbumSetDataLoader.this.mSource.isLoading()) {
                            updateLoading(false);
                        }
                        Utils.waitWithoutInterrupt(this);
                    } else {
                        this.mDirty = false;
                        updateLoading(true);
                        long version = AlbumSetDataLoader.this.mSource.reload();
                        UpdateInfo info = (UpdateInfo) AlbumSetDataLoader.this.executeAndWait(AlbumSetDataLoader.this.new GetUpdateInfo(version));
                        updateComplete = info == null;
                        if (!updateComplete) {
                            if (info.version != version) {
                                info.version = version;
                                info.size = AlbumSetDataLoader.this.mSource.getSubMediaSetCount();
                                if (info.index >= info.size) {
                                    info.index = -1;
                                }
                            }
                            if (info.index != -1) {
                                info.item = AlbumSetDataLoader.this.mSource.getSubMediaSet(info.index);
                                if (info.item != null) {
                                    info.cover = info.item.getCoverMediaItem();
                                    info.totalCount = info.item.getTotalMediaItemCount();
                                }
                            }
                            AlbumSetDataLoader.this.executeAndWait(AlbumSetDataLoader.this.new UpdateContent(info));
                        }
                    }
                }
            }
            updateLoading(false);
        }

        public synchronized void notifyDirty() {
            this.mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            this.mActive = false;
            notifyAll();
        }
    }
}
