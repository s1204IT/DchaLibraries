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
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallery3d.layout.Layout;
import com.mediatek.galleryportable.TraceHelper;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class AlbumSetDataLoader {
    private final MediaItem[] mCoverItem;
    private final MediaSet[] mData;
    private DataListener mDataListener;
    private Layout.DataChangeListener mFancyDataChangeListener;
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
    private volatile boolean mIsSourceSensive = true;

    public interface DataListener {
        void onContentChanged(int i);

        void onSizeChanged(int i);
    }

    public AlbumSetDataLoader(AbstractGalleryActivity abstractGalleryActivity, MediaSet mediaSet, int i) {
        this.mSource = (MediaSet) Utils.checkNotNull(mediaSet);
        this.mCoverItem = new MediaItem[i];
        this.mData = new MediaSet[i];
        this.mTotalCount = new int[i];
        this.mItemVersion = new long[i];
        this.mSetVersion = new long[i];
        Arrays.fill(this.mItemVersion, -1L);
        Arrays.fill(this.mSetVersion, -1L);
        this.mMainHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot()) {
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

    private void assertIsActive(int i) {
        if (i < this.mActiveStart || i >= this.mActiveEnd) {
            throw new IllegalArgumentException(String.format("%s not in (%s, %s)", Integer.valueOf(i), Integer.valueOf(this.mActiveStart), Integer.valueOf(this.mActiveEnd)));
        }
    }

    public MediaSet getMediaSet(int i) {
        assertIsActive(i);
        return this.mData[i % this.mData.length];
    }

    public MediaItem getCoverItem(int i) {
        assertIsActive(i);
        return this.mCoverItem[i % this.mCoverItem.length];
    }

    public int getTotalCount(int i) {
        assertIsActive(i);
        return this.mTotalCount[i % this.mTotalCount.length];
    }

    public int getActiveStart() {
        return this.mActiveStart;
    }

    public int getActiveEnd() {
        return this.mActiveEnd;
    }

    public boolean isActive(int i) {
        return i >= this.mActiveStart && i < this.mActiveEnd;
    }

    public int size() {
        return this.mSize;
    }

    public int findSet(Path path) {
        int length = this.mData.length;
        for (int i = this.mContentStart; i < this.mContentEnd; i++) {
            MediaSet mediaSet = this.mData[i % length];
            if (mediaSet != null && path == mediaSet.getPath()) {
                return i;
            }
        }
        return -1;
    }

    private void clearSlot(int i) {
        this.mData[i] = null;
        this.mCoverItem[i] = null;
        this.mTotalCount[i] = 0;
        this.mItemVersion[i] = -1;
        this.mSetVersion[i] = -1;
    }

    private void setContentWindow(int i, int i2) {
        if (i == this.mContentStart && i2 == this.mContentEnd) {
            return;
        }
        int length = this.mCoverItem.length;
        int i3 = this.mContentStart;
        int i4 = this.mContentEnd;
        this.mContentStart = i;
        this.mContentEnd = i2;
        if (i >= i4 || i3 >= i2) {
            while (i3 < i4) {
                clearSlot(i3 % length);
                i3++;
            }
        } else {
            while (i3 < i) {
                clearSlot(i3 % length);
                i3++;
            }
            while (i2 < i4) {
                clearSlot(i2 % length);
                i2++;
            }
        }
        this.mReloadTask.notifyDirty();
    }

    public void setActiveWindow(int i, int i2) {
        if (i == this.mActiveStart && i2 == this.mActiveEnd) {
            return;
        }
        if (i > i2 || i2 - i > this.mCoverItem.length || i2 > this.mSize) {
            Utils.fail("start = %s, end = %s, mCoverItem.length = %s, mSize = %s", Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(this.mCoverItem.length), Integer.valueOf(this.mSize));
        }
        this.mActiveStart = i;
        this.mActiveEnd = i2;
        int length = this.mCoverItem.length;
        if (i == i2) {
            return;
        }
        int iClamp = Utils.clamp(((i + i2) / 2) - (length / 2), 0, Math.max(0, this.mSize - length));
        int iMin = Math.min(length + iClamp, this.mSize);
        if (this.mContentStart > i || this.mContentEnd < i2 || Math.abs(iClamp - this.mContentStart) > 4) {
            setContentWindow(iClamp, iMin);
        }
    }

    private class MySourceListener implements ContentListener {
        private MySourceListener() {
        }

        @Override
        public void onContentDirty() {
            if (AlbumSetDataLoader.this.mIsSourceSensive && AlbumSetDataLoader.this.mReloadTask != null) {
                AlbumSetDataLoader.this.mReloadTask.notifyDirty();
            }
        }
    }

    public void setModelListener(DataListener dataListener) {
        this.mDataListener = dataListener;
    }

    public void setLoadingListener(LoadingListener loadingListener) {
        this.mLoadingListener = loadingListener;
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

        public GetUpdateInfo(long j) {
            this.mVersion = j;
        }

        private int getInvalidIndex(long j) {
            long[] jArr = AlbumSetDataLoader.this.mSetVersion;
            int length = jArr.length;
            int i = AlbumSetDataLoader.this.mContentEnd;
            for (int i2 = AlbumSetDataLoader.this.mContentStart; i2 < i; i2++) {
                if (jArr[i2 % length] != j) {
                    return i2;
                }
            }
            return -1;
        }

        @Override
        public UpdateInfo call() throws Exception {
            TraceHelper.beginSection(">>>>AlbumSetDataLoader-GetUpdateInfo.run");
            int invalidIndex = getInvalidIndex(this.mVersion);
            if (invalidIndex == -1 && AlbumSetDataLoader.this.mSourceVersion == this.mVersion) {
                TraceHelper.endSection();
                return null;
            }
            UpdateInfo updateInfo = new UpdateInfo();
            updateInfo.version = AlbumSetDataLoader.this.mSourceVersion;
            updateInfo.index = invalidIndex;
            updateInfo.size = AlbumSetDataLoader.this.mSize;
            TraceHelper.endSection();
            return updateInfo;
        }
    }

    private class UpdateContent implements Callable<Void> {
        private final UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo updateInfo) {
            this.mUpdateInfo = updateInfo;
        }

        @Override
        public Void call() {
            if (AlbumSetDataLoader.this.mReloadTask == null) {
                return null;
            }
            TraceHelper.beginSection(">>>>AlbumSetDataLoader-UpdateContent.run");
            UpdateInfo updateInfo = this.mUpdateInfo;
            AlbumSetDataLoader.this.mSourceVersion = updateInfo.version;
            if (AlbumSetDataLoader.this.mSize != updateInfo.size) {
                if (FancyHelper.isFancyLayoutSupported() && AlbumSetDataLoader.this.mFancyDataChangeListener != null) {
                    AlbumSetDataLoader.this.mFancyDataChangeListener.onDataChange(-1, null, updateInfo.size, false, "");
                    Log.d("Gallery2/AlbumSetDataAdapter", "<UpdateContent.call> <Fancy> onSizeChanged(" + updateInfo.size + ")");
                }
                AlbumSetDataLoader.this.mSize = updateInfo.size;
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
            if (updateInfo.index >= AlbumSetDataLoader.this.mContentStart && updateInfo.index < AlbumSetDataLoader.this.mContentEnd) {
                int length = updateInfo.index % AlbumSetDataLoader.this.mCoverItem.length;
                AlbumSetDataLoader.this.mSetVersion[length] = updateInfo.version;
                long dataVersion = updateInfo.item.getDataVersion();
                if (AlbumSetDataLoader.this.mItemVersion[length] != dataVersion) {
                    AlbumSetDataLoader.this.mItemVersion[length] = dataVersion;
                    AlbumSetDataLoader.this.mData[length] = updateInfo.item;
                    AlbumSetDataLoader.this.mCoverItem[length] = updateInfo.cover;
                    AlbumSetDataLoader.this.mTotalCount[length] = updateInfo.totalCount;
                    if (AlbumSetDataLoader.this.mDataListener != null && updateInfo.index >= AlbumSetDataLoader.this.mActiveStart && updateInfo.index < AlbumSetDataLoader.this.mActiveEnd) {
                        AlbumSetDataLoader.this.mDataListener.onContentChanged(updateInfo.index);
                    }
                } else {
                    TraceHelper.endSection();
                    return null;
                }
            }
            if (FancyHelper.isFancyLayoutSupported() && AlbumSetDataLoader.this.mFancyDataChangeListener != null && updateInfo.item != null && updateInfo.cover != null) {
                AlbumSetDataLoader.this.mFancyDataChangeListener.onDataChange(updateInfo.index, updateInfo.cover, updateInfo.size, updateInfo.item.isCameraRoll(), updateInfo.item.getName());
            } else if (updateInfo.cover == null) {
                Log.d("Gallery2/AlbumSetDataAdapter", "<UpdateContent.call> <Fancy> info.cover is null when info.index = " + updateInfo.index + ", not call onDataChange");
            }
            TraceHelper.endSection();
            return null;
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

    private class ReloadTask extends Thread {
        private volatile boolean mActive;
        private volatile boolean mDirty;
        private volatile boolean mIsLoading;

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
            AlbumSetDataLoader.this.mMainHandler.sendEmptyMessage(z ? 1 : 2);
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            boolean z = false;
            while (this.mActive) {
                synchronized (this) {
                    if (this.mActive && !this.mDirty && z) {
                        if (!AlbumSetDataLoader.this.mSource.isLoading()) {
                            updateLoading(false);
                        }
                        Utils.waitWithoutInterrupt(this);
                    } else {
                        this.mDirty = false;
                        z = true;
                        updateLoading(true);
                        TraceHelper.beginSection(">>>>AlbumSetDataLoader-reload");
                        long jReload = AlbumSetDataLoader.this.mSource.reload();
                        TraceHelper.endSection();
                        UpdateInfo updateInfo = (UpdateInfo) AlbumSetDataLoader.this.executeAndWait(AlbumSetDataLoader.this.new GetUpdateInfo(jReload));
                        if (updateInfo != null) {
                            z = false;
                        }
                        if (!z) {
                            if (updateInfo.version != jReload) {
                                updateInfo.version = jReload;
                                updateInfo.size = AlbumSetDataLoader.this.mSource.getSubMediaSetCount();
                                if (updateInfo.index >= updateInfo.size) {
                                    updateInfo.index = -1;
                                }
                            }
                            if (updateInfo.index != -1) {
                                updateInfo.item = AlbumSetDataLoader.this.mSource.getSubMediaSet(updateInfo.index);
                                if (updateInfo.item != null) {
                                    TraceHelper.beginSection(">>>>AlbumSetDataLoader-getCoverMediaItem");
                                    updateInfo.cover = updateInfo.item.getCoverMediaItem();
                                    TraceHelper.endSection();
                                    TraceHelper.beginSection(">>>>AlbumSetDataLoader-getTotalMediaItemCount");
                                    updateInfo.totalCount = updateInfo.item.getTotalMediaItemCount();
                                    TraceHelper.endSection();
                                }
                            }
                            AlbumSetDataLoader.this.executeAndWait(AlbumSetDataLoader.this.new UpdateContent(updateInfo));
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
            if (AlbumSetDataLoader.this.mSource != null) {
                AlbumSetDataLoader.this.mSource.stopReload();
            }
        }
    }

    public void setFancyDataChangeListener(Layout.DataChangeListener dataChangeListener) {
        this.mFancyDataChangeListener = dataChangeListener;
    }

    public void setSourceSensive(boolean z) {
        this.mIsSourceSensive = z;
    }

    public void fakeSourceChange() {
        this.mSourceListener.onContentDirty();
    }
}
