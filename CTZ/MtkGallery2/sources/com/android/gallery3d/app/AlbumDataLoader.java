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
import com.mediatek.gallerybasic.base.ExtItem;
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
    private volatile boolean mIsSourceSensive = true;
    private final MediaItem[] mData = new MediaItem[1000];
    private final long[] mItemVersion = new long[1000];
    private final long[] mSetVersion = new long[1000];

    public interface DataListener {
        void onContentChanged(int i);

        void onSizeChanged(int i);
    }

    public AlbumDataLoader(AbstractGalleryActivity abstractGalleryActivity, MediaSet mediaSet) {
        this.mSource = mediaSet;
        Arrays.fill(this.mItemVersion, -1L);
        Arrays.fill(this.mSetVersion, -1L);
        this.mMainHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot()) {
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
                            AlbumDataLoader.this.mLoadingListener.onLoadingFinished(AlbumDataLoader.this.mFailedVersion != -1);
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

    public MediaItem get(int i) {
        if (!isActive(i)) {
            ArrayList<MediaItem> mediaItem = this.mSource.getMediaItem(i, 1);
            if (mediaItem != null && mediaItem.size() != 0) {
                return mediaItem.get(0);
            }
            return null;
        }
        return this.mData[i % this.mData.length];
    }

    public int getActiveStart() {
        return this.mActiveStart;
    }

    public boolean isActive(int i) {
        return i >= this.mActiveStart && i < this.mActiveEnd;
    }

    public int size() {
        return this.mSize;
    }

    public int findItem(Path path) {
        for (int i = this.mContentStart; i < this.mContentEnd; i++) {
            MediaItem mediaItem = this.mData[i % 1000];
            if (mediaItem != null && path == mediaItem.getPath()) {
                return i;
            }
        }
        return -1;
    }

    private void clearSlot(int i) {
        this.mData[i] = null;
        this.mItemVersion[i] = -1;
        this.mSetVersion[i] = -1;
    }

    private void setContentWindow(int i, int i2) {
        if (i == this.mContentStart && i2 == this.mContentEnd) {
            return;
        }
        int i3 = this.mContentEnd;
        int i4 = this.mContentStart;
        synchronized (this) {
            this.mContentStart = i;
            this.mContentEnd = i2;
        }
        long[] jArr = this.mItemVersion;
        long[] jArr2 = this.mSetVersion;
        if (i >= i3 || i4 >= i2) {
            while (i4 < i3) {
                clearSlot(i4 % 1000);
                i4++;
            }
        } else {
            while (i4 < i) {
                clearSlot(i4 % 1000);
                i4++;
            }
            while (i2 < i3) {
                clearSlot(i2 % 1000);
                i2++;
            }
        }
        if (this.mReloadTask != null) {
            this.mReloadTask.notifyDirty();
        }
    }

    public void setActiveWindow(int i, int i2) {
        if (i == this.mActiveStart && i2 == this.mActiveEnd) {
            return;
        }
        Utils.assertTrue(i <= i2 && i2 - i <= this.mData.length && i2 <= this.mSize);
        int length = this.mData.length;
        this.mActiveStart = i;
        this.mActiveEnd = i2;
        if (i == i2) {
            return;
        }
        int iClamp = Utils.clamp(((i + i2) / 2) - (length / 2), 0, Math.max(0, this.mSize - length));
        int iMin = Math.min(length + iClamp, this.mSize);
        if (this.mContentStart > i || this.mContentEnd < i2 || Math.abs(iClamp - this.mContentStart) > 32) {
            setContentWindow(iClamp, iMin);
        }
    }

    private class MySourceListener implements ContentListener {
        private MySourceListener() {
        }

        @Override
        public void onContentDirty() {
            if (AlbumDataLoader.this.mIsSourceSensive && AlbumDataLoader.this.mReloadTask != null) {
                AlbumDataLoader.this.mReloadTask.notifyDirty();
            }
        }
    }

    public void setDataListener(DataListener dataListener) {
        this.mDataListener = dataListener;
    }

    public void setLoadingListener(LoadingListener loadingListener) {
        this.mLoadingListener = loadingListener;
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

        public GetUpdateInfo(long j) {
            this.mVersion = j;
        }

        @Override
        public UpdateInfo call() throws Exception {
            if (AlbumDataLoader.this.mFailedVersion == this.mVersion) {
                return null;
            }
            UpdateInfo updateInfo = new UpdateInfo();
            long j = this.mVersion;
            updateInfo.version = AlbumDataLoader.this.mSourceVersion;
            updateInfo.size = AlbumDataLoader.this.mSize;
            long[] jArr = AlbumDataLoader.this.mSetVersion;
            int i = AlbumDataLoader.this.mContentEnd;
            for (int i2 = AlbumDataLoader.this.mContentStart; i2 < i; i2++) {
                if (jArr[i2 % 1000] != j) {
                    updateInfo.reloadStart = i2;
                    updateInfo.reloadCount = Math.min(64, i - i2);
                    return updateInfo;
                }
            }
            if (AlbumDataLoader.this.mSourceVersion == this.mVersion) {
                return null;
            }
            return updateInfo;
        }
    }

    private class UpdateContent implements Callable<Void> {
        private UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo updateInfo) {
            this.mUpdateInfo = updateInfo;
        }

        @Override
        public Void call() throws Exception {
            UpdateInfo updateInfo = this.mUpdateInfo;
            AlbumDataLoader.this.mSourceVersion = updateInfo.version;
            if (AlbumDataLoader.this.mSize != updateInfo.size) {
                AlbumDataLoader.this.mSize = updateInfo.size;
                if (AlbumDataLoader.this.mDataListener != null && AlbumDataLoader.this.mSize >= 0) {
                    AlbumDataLoader.this.mDataListener.onSizeChanged(AlbumDataLoader.this.mSize);
                }
                if (AlbumDataLoader.this.mContentEnd > AlbumDataLoader.this.mSize) {
                    AlbumDataLoader.this.mContentEnd = AlbumDataLoader.this.mSize;
                }
                if (AlbumDataLoader.this.mActiveEnd > AlbumDataLoader.this.mSize) {
                    AlbumDataLoader.this.mActiveEnd = AlbumDataLoader.this.mSize;
                }
            }
            ArrayList<MediaItem> arrayList = updateInfo.items;
            AlbumDataLoader.this.mFailedVersion = -1L;
            if (arrayList != null && !arrayList.isEmpty()) {
                int iMin = Math.min(updateInfo.reloadStart + arrayList.size(), AlbumDataLoader.this.mContentEnd);
                for (int iMax = Math.max(updateInfo.reloadStart, AlbumDataLoader.this.mContentStart); iMax < iMin; iMax++) {
                    int i = iMax % 1000;
                    AlbumDataLoader.this.mSetVersion[i] = updateInfo.version;
                    MediaItem mediaItem = arrayList.get(iMax - updateInfo.reloadStart);
                    long dataVersion = mediaItem.getDataVersion();
                    if (AlbumDataLoader.this.mItemVersion[i] != dataVersion) {
                        AlbumDataLoader.this.mItemVersion[i] = dataVersion;
                        AlbumDataLoader.this.mData[i] = mediaItem;
                        if (mediaItem.getExtItem() != null) {
                            mediaItem.getExtItem().registerListener(new ExtItem.DataChangeListener() {
                                @Override
                                public void onExtItemDataChange(ArrayList<String> arrayList2) {
                                    Log.d("Gallery2/AlbumDataAdapter", "<onExtItemDataChange>,size:" + arrayList2.size() + ",mContentEnd:" + AlbumDataLoader.this.mContentEnd);
                                    for (int i2 = 0; i2 < AlbumDataLoader.this.mContentEnd; i2++) {
                                        MediaItem mediaItem2 = AlbumDataLoader.this.mData[i2];
                                        if (mediaItem2 != null && mediaItem2.getExtItem() != null && arrayList2.indexOf(mediaItem2.getMediaData().filePath) != -1) {
                                            AlbumDataLoader.this.mDataListener.onContentChanged(i2);
                                        }
                                    }
                                }
                            });
                        }
                        if (AlbumDataLoader.this.mReloadTask != null && AlbumDataLoader.this.mDataListener != null && iMax >= AlbumDataLoader.this.mActiveStart && iMax < AlbumDataLoader.this.mActiveEnd) {
                            AlbumDataLoader.this.mDataListener.onContentChanged(iMax);
                        }
                    }
                }
                return null;
            }
            if (updateInfo.reloadCount > 0) {
                AlbumDataLoader.this.mFailedVersion = updateInfo.version;
                Log.d("Gallery2/AlbumDataAdapter", "loading failed: " + AlbumDataLoader.this.mFailedVersion);
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

        private void updateLoading(boolean z) {
            if (this.mIsLoading == z) {
                return;
            }
            this.mIsLoading = z;
            AlbumDataLoader.this.mMainHandler.sendEmptyMessage(z ? 1 : 2);
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            boolean z = false;
            while (this.mActive) {
                synchronized (this) {
                    if (this.mActive && !this.mDirty && z) {
                        updateLoading(false);
                        if (AlbumDataLoader.this.mFailedVersion != -1) {
                            Log.d("Gallery2/AlbumDataAdapter", "reload pause");
                        }
                        Utils.waitWithoutInterrupt(this);
                        if (this.mActive && AlbumDataLoader.this.mFailedVersion != -1) {
                            Log.d("Gallery2/AlbumDataAdapter", "reload resume");
                        }
                    } else {
                        this.mDirty = false;
                        z = true;
                        updateLoading(true);
                        long jReload = AlbumDataLoader.this.mSource.reload();
                        UpdateInfo updateInfo = (UpdateInfo) AlbumDataLoader.this.executeAndWait(AlbumDataLoader.this.new GetUpdateInfo(jReload));
                        if (updateInfo != null) {
                            z = false;
                        }
                        if (!z) {
                            if (updateInfo.version != jReload) {
                                updateInfo.size = AlbumDataLoader.this.mSource.getMediaItemCount();
                                updateInfo.version = jReload;
                            }
                            if (updateInfo.reloadCount > 0) {
                                updateInfo.items = AlbumDataLoader.this.mSource.getMediaItem(updateInfo.reloadStart, updateInfo.reloadCount);
                            }
                            AlbumDataLoader.this.executeAndWait(AlbumDataLoader.this.new UpdateContent(updateInfo));
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
            if (AlbumDataLoader.this.mSource != null) {
                AlbumDataLoader.this.mSource.stopReload();
            }
        }
    }

    public int getActiveEnd() {
        return this.mActiveEnd;
    }

    public void setSourceSensive(boolean z) {
        this.mIsSourceSensive = z;
    }

    public void fakeSourceChange() {
        this.mSourceListener.onContentDirty();
    }
}
