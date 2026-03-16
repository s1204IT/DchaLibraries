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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class AlbumDataLoader {
    private DataListener mDataListener;
    private LoadingListener mLoadingListener;
    private final Handler mMainHandler;
    private ReloadTask mReloadTask;
    private final MediaSet mSource;
    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private int mContentStart = 0;
    private int mContentEnd = 0;
    private long mSourceVersion = -1;
    private int mSize = 0;
    private MySourceListener mSourceListener = new MySourceListener();
    private long mFailedVersion = -1;
    private final MediaItem[] mData = new MediaItem[1000];
    private final long[] mItemVersion = new long[1000];
    private final long[] mSetVersion = new long[1000];

    public interface DataListener {
        void onContentChanged(int i);

        void onSizeChanged(int i);
    }

    public AlbumDataLoader(AbstractGalleryActivity context, MediaSet mediaSet) {
        this.mSource = mediaSet;
        Arrays.fill(this.mItemVersion, -1L);
        Arrays.fill(this.mSetVersion, -1L);
        this.mMainHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        if (AlbumDataLoader.this.mLoadingListener != null) {
                            AlbumDataLoader.this.mLoadingListener.onLoadingStarted();
                        }
                        break;
                    case 2:
                        if (AlbumDataLoader.this.mLoadingListener != null) {
                            boolean loadingFailed = AlbumDataLoader.this.mFailedVersion != -1;
                            AlbumDataLoader.this.mLoadingListener.onLoadingFinished(loadingFailed);
                        }
                        break;
                    case 3:
                        ((Runnable) message.obj).run();
                        break;
                }
            }
        };
    }

    public void resume() {
        this.mSource.addContentListener(this.mSourceListener);
        this.mReloadTask = new ReloadTask();
        this.mReloadTask.start();
    }

    public void pause() {
        this.mReloadTask.terminate();
        this.mReloadTask = null;
        this.mSource.removeContentListener(this.mSourceListener);
    }

    public MediaItem get(int index) {
        return !isActive(index) ? this.mSource.getMediaItem(index, 1).get(0) : this.mData[index % this.mData.length];
    }

    public boolean isActive(int index) {
        return index >= this.mActiveStart && index < this.mActiveEnd;
    }

    public int size() {
        return this.mSize;
    }

    public int findItem(Path id) {
        for (int i = this.mContentStart; i < this.mContentEnd; i++) {
            MediaItem item = this.mData[i % 1000];
            if (item != null && id == item.getPath()) {
                return i;
            }
        }
        return -1;
    }

    private void clearSlot(int slotIndex) {
        this.mData[slotIndex] = null;
        this.mItemVersion[slotIndex] = -1;
        this.mSetVersion[slotIndex] = -1;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart != this.mContentStart || contentEnd != this.mContentEnd) {
            int end = this.mContentEnd;
            int start = this.mContentStart;
            synchronized (this) {
                this.mContentStart = contentStart;
                this.mContentEnd = contentEnd;
            }
            long[] jArr = this.mItemVersion;
            long[] jArr2 = this.mSetVersion;
            if (contentStart >= end || start >= contentEnd) {
                for (int i = start; i < end; i++) {
                    clearSlot(i % 1000);
                }
            } else {
                for (int i2 = start; i2 < contentStart; i2++) {
                    clearSlot(i2 % 1000);
                }
                for (int i3 = contentEnd; i3 < end; i3++) {
                    clearSlot(i3 % 1000);
                }
            }
            if (this.mReloadTask != null) {
                this.mReloadTask.notifyDirty();
            }
        }
    }

    public void setActiveWindow(int start, int end) {
        if (start != this.mActiveStart || end != this.mActiveEnd) {
            Utils.assertTrue(start <= end && end - start <= this.mData.length && end <= this.mSize);
            int length = this.mData.length;
            this.mActiveStart = start;
            this.mActiveEnd = end;
            if (start != end) {
                int contentStart = Utils.clamp(((start + end) / 2) - (length / 2), 0, Math.max(0, this.mSize - length));
                int contentEnd = Math.min(contentStart + length, this.mSize);
                if (this.mContentStart > start || this.mContentEnd < end || Math.abs(contentStart - this.mContentStart) > 32) {
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
            if (AlbumDataLoader.this.mReloadTask != null) {
                AlbumDataLoader.this.mReloadTask.notifyDirty();
            }
        }
    }

    public void setDataListener(DataListener listener) {
        this.mDataListener = listener;
    }

    public void setLoadingListener(LoadingListener listener) {
        this.mLoadingListener = listener;
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
        public ArrayList<MediaItem> items;
        public int reloadCount;
        public int reloadStart;
        public int size;
        public long version;

        private UpdateInfo() {
        }
    }

    private class GetUpdateInfo implements Callable<UpdateInfo> {
        private final long mVersion;

        public GetUpdateInfo(long version) {
            this.mVersion = version;
        }

        @Override
        public UpdateInfo call() throws Exception {
            if (AlbumDataLoader.this.mFailedVersion == this.mVersion) {
                return null;
            }
            UpdateInfo info = new UpdateInfo();
            long version = this.mVersion;
            info.version = AlbumDataLoader.this.mSourceVersion;
            info.size = AlbumDataLoader.this.mSize;
            long[] setVersion = AlbumDataLoader.this.mSetVersion;
            int n = AlbumDataLoader.this.mContentEnd;
            for (int i = AlbumDataLoader.this.mContentStart; i < n; i++) {
                int index = i % 1000;
                if (setVersion[index] != version) {
                    info.reloadStart = i;
                    info.reloadCount = Math.min(64, n - i);
                    return info;
                }
            }
            if (AlbumDataLoader.this.mSourceVersion == this.mVersion) {
                return null;
            }
            return info;
        }
    }

    private class UpdateContent implements Callable<Void> {
        private UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo info) {
            this.mUpdateInfo = info;
        }

        @Override
        public Void call() throws Exception {
            UpdateInfo info = this.mUpdateInfo;
            AlbumDataLoader.this.mSourceVersion = info.version;
            if (AlbumDataLoader.this.mSize != info.size) {
                AlbumDataLoader.this.mSize = info.size;
                if (AlbumDataLoader.this.mDataListener != null) {
                    AlbumDataLoader.this.mDataListener.onSizeChanged(AlbumDataLoader.this.mSize);
                }
                if (AlbumDataLoader.this.mContentEnd > AlbumDataLoader.this.mSize) {
                    AlbumDataLoader.this.mContentEnd = AlbumDataLoader.this.mSize;
                }
                if (AlbumDataLoader.this.mActiveEnd > AlbumDataLoader.this.mSize) {
                    AlbumDataLoader.this.mActiveEnd = AlbumDataLoader.this.mSize;
                }
            }
            ArrayList<MediaItem> items = info.items;
            AlbumDataLoader.this.mFailedVersion = -1L;
            if (items != null && !items.isEmpty()) {
                int start = Math.max(info.reloadStart, AlbumDataLoader.this.mContentStart);
                int end = Math.min(info.reloadStart + items.size(), AlbumDataLoader.this.mContentEnd);
                for (int i = start; i < end; i++) {
                    int index = i % 1000;
                    AlbumDataLoader.this.mSetVersion[index] = info.version;
                    MediaItem updateItem = items.get(i - info.reloadStart);
                    long itemVersion = updateItem.getDataVersion();
                    if (AlbumDataLoader.this.mItemVersion[index] != itemVersion) {
                        AlbumDataLoader.this.mItemVersion[index] = itemVersion;
                        AlbumDataLoader.this.mData[index] = updateItem;
                        if (AlbumDataLoader.this.mDataListener != null && i >= AlbumDataLoader.this.mActiveStart && i < AlbumDataLoader.this.mActiveEnd) {
                            AlbumDataLoader.this.mDataListener.onContentChanged(i);
                        }
                    }
                }
            } else if (info.reloadCount > 0) {
                AlbumDataLoader.this.mFailedVersion = info.version;
                Log.d("AlbumDataAdapter", "loading failed: " + AlbumDataLoader.this.mFailedVersion);
            }
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
                AlbumDataLoader.this.mMainHandler.sendEmptyMessage(loading ? 1 : 2);
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            boolean updateComplete = false;
            while (this.mActive) {
                synchronized (this) {
                    if (this.mActive && !this.mDirty && updateComplete) {
                        updateLoading(false);
                        if (AlbumDataLoader.this.mFailedVersion != -1) {
                            Log.d("AlbumDataAdapter", "reload pause");
                        }
                        Utils.waitWithoutInterrupt(this);
                        if (this.mActive && AlbumDataLoader.this.mFailedVersion != -1) {
                            Log.d("AlbumDataAdapter", "reload resume");
                        }
                    } else {
                        this.mDirty = false;
                        updateLoading(true);
                        long version = AlbumDataLoader.this.mSource.reload();
                        UpdateInfo info = (UpdateInfo) AlbumDataLoader.this.executeAndWait(AlbumDataLoader.this.new GetUpdateInfo(version));
                        updateComplete = info == null;
                        if (!updateComplete) {
                            if (info.version != version) {
                                info.size = AlbumDataLoader.this.mSource.getMediaItemCount();
                                info.version = version;
                            }
                            if (info.reloadCount > 0) {
                                info.items = AlbumDataLoader.this.mSource.getMediaItem(info.reloadStart, info.reloadCount);
                            }
                            AlbumDataLoader.this.executeAndWait(AlbumDataLoader.this.new UpdateContent(info));
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
