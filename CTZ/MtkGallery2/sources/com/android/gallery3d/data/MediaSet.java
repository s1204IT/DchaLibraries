package com.android.gallery3d.data;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.Future;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.WeakHashMap;

public abstract class MediaSet extends MediaObject {
    private static final Future<Integer> FUTURE_STUB = new Future<Integer>() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Integer get() {
            return 0;
        }

        @Override
        public void waitDone() {
        }
    };
    public MediaSet mCurrentClusterAlbum;
    private WeakHashMap<ContentListener, Object> mListeners;
    private Object mWeakHashMapLock;
    public int offsetInStack;

    public interface ItemConsumer {
        void consume(int i, MediaItem mediaItem);

        boolean stopConsume();
    }

    public interface SyncListener {
        void onSyncDone(MediaSet mediaSet, int i);
    }

    public abstract String getName();

    public abstract long reload();

    public MediaSet(Path path, long j) {
        super(path, j);
        this.mListeners = new WeakHashMap<>();
        this.mWeakHashMapLock = new Object();
        this.offsetInStack = 0;
    }

    public int getMediaItemCount() {
        return 0;
    }

    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        return new ArrayList<>();
    }

    public MediaItem getCoverMediaItem() {
        ArrayList<MediaItem> mediaItem = getMediaItem(0, 1);
        if (mediaItem.size() > 0) {
            return mediaItem.get(0);
        }
        int subMediaSetCount = getSubMediaSetCount();
        for (int i = 0; i < subMediaSetCount; i++) {
            MediaItem coverMediaItem = getSubMediaSet(i).getCoverMediaItem();
            if (coverMediaItem != null) {
                return coverMediaItem;
            }
        }
        return null;
    }

    public int getSubMediaSetCount() {
        return 0;
    }

    public MediaSet getSubMediaSet(int i) {
        throw new IndexOutOfBoundsException();
    }

    public boolean isLeafAlbum() {
        return false;
    }

    public boolean isCameraRoll() {
        return false;
    }

    public boolean isLoading() {
        return false;
    }

    public int getTotalMediaItemCount() {
        int mediaItemCount = getMediaItemCount();
        int subMediaSetCount = getSubMediaSetCount();
        for (int i = 0; i < subMediaSetCount; i++) {
            mediaItemCount += getSubMediaSet(i).getTotalMediaItemCount();
        }
        return mediaItemCount;
    }

    public int getIndexOfItem(Path path, int i) {
        int iMax = Math.max(0, i - 64);
        int indexOf = getIndexOf(path, getMediaItem(iMax, 128));
        if (indexOf != -1) {
            return iMax + indexOf;
        }
        int i2 = iMax == 0 ? 128 : 0;
        ArrayList<MediaItem> mediaItem = getMediaItem(i2, 500);
        while (true) {
            int indexOf2 = getIndexOf(path, mediaItem);
            if (indexOf2 != -1) {
                return i2 + indexOf2;
            }
            if (mediaItem.size() < 500) {
                return -1;
            }
            i2 += 500;
            mediaItem = getMediaItem(i2, 500);
        }
    }

    public int getIndexOf(Path path, ArrayList<MediaItem> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            MediaItem mediaItem = arrayList.get(i);
            if (mediaItem != null && mediaItem.mPath.equalsIgnoreCase(path.toString())) {
                return i;
            }
        }
        return -1;
    }

    public void addContentListener(ContentListener contentListener) {
        synchronized (this.mWeakHashMapLock) {
            this.mListeners.put(contentListener, null);
        }
    }

    public void removeContentListener(ContentListener contentListener) {
        synchronized (this.mWeakHashMapLock) {
            this.mListeners.remove(contentListener);
        }
    }

    public void notifyContentChanged() {
        synchronized (this.mWeakHashMapLock) {
            Iterator<ContentListener> it = this.mListeners.keySet().iterator();
            while (it.hasNext()) {
                it.next().onContentDirty();
            }
        }
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        details.addDetail(1, getName());
        return details;
    }

    public void enumerateMediaItems(ItemConsumer itemConsumer) {
        enumerateMediaItems(itemConsumer, 0);
    }

    public void enumerateTotalMediaItems(ItemConsumer itemConsumer) {
        enumerateTotalMediaItems(itemConsumer, 0);
    }

    protected int enumerateMediaItems(ItemConsumer itemConsumer, int i) {
        int mediaItemCount = getMediaItemCount();
        int i2 = 0;
        boolean z = false;
        while (true) {
            if (i2 >= mediaItemCount) {
                break;
            }
            int iMin = Math.min(500, mediaItemCount - i2);
            ArrayList<MediaItem> mediaItem = getMediaItem(i2, iMin);
            int size = mediaItem.size();
            int i3 = 0;
            while (true) {
                if (i3 >= size) {
                    break;
                }
                itemConsumer.consume(i + i2 + i3, mediaItem.get(i3));
                if (!itemConsumer.stopConsume()) {
                    i3++;
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                Log.d("Gallery2/MediaSet", "<enumerateMediaItems> stopEnumerateItem = " + z);
                break;
            }
            i2 += iMin;
        }
        return mediaItemCount;
    }

    protected int enumerateTotalMediaItems(ItemConsumer itemConsumer, int i) {
        int iEnumerateMediaItems = enumerateMediaItems(itemConsumer, i) + 0;
        int subMediaSetCount = getSubMediaSetCount();
        for (int i2 = 0; i2 < subMediaSetCount; i2++) {
            MediaSet subMediaSet = getSubMediaSet(i2);
            if (subMediaSet == null) {
                Log.d("Gallery2/MediaSet", "<enumerateTotalMediaItems> SubMediaSet " + i2 + " is null");
                return iEnumerateMediaItems;
            }
            iEnumerateMediaItems += subMediaSet.enumerateTotalMediaItems(itemConsumer, i + iEnumerateMediaItems);
        }
        return iEnumerateMediaItems;
    }

    public Future<Integer> requestSync(SyncListener syncListener) {
        syncListener.onSyncDone(this, 0);
        return FUTURE_STUB;
    }

    protected Future<Integer> requestSyncOnMultipleSets(MediaSet[] mediaSetArr, SyncListener syncListener) {
        return new MultiSetSyncFuture(mediaSetArr, syncListener);
    }

    private class MultiSetSyncFuture implements SyncListener, Future<Integer> {
        private final Future<Integer>[] mFutures;
        private final SyncListener mListener;
        private int mPendingCount;
        private boolean mIsCancelled = false;
        private int mResult = -1;

        MultiSetSyncFuture(MediaSet[] mediaSetArr, SyncListener syncListener) {
            this.mListener = syncListener;
            this.mPendingCount = mediaSetArr.length;
            this.mFutures = new Future[mediaSetArr.length];
            synchronized (this) {
                int length = mediaSetArr.length;
                for (int i = 0; i < length; i++) {
                    this.mFutures[i] = mediaSetArr[i].requestSync(this);
                    Log.d("Gallery2/Gallery.MultiSetSync", "  request sync: " + Utils.maskDebugInfo(mediaSetArr[i].getName()));
                }
            }
        }

        @Override
        public synchronized void cancel() {
            if (this.mIsCancelled) {
                return;
            }
            this.mIsCancelled = true;
            for (Future<Integer> future : this.mFutures) {
                future.cancel();
            }
            if (this.mResult < 0) {
                this.mResult = 1;
            }
        }

        @Override
        public synchronized boolean isCancelled() {
            return this.mIsCancelled;
        }

        @Override
        public synchronized boolean isDone() {
            return this.mPendingCount == 0;
        }

        @Override
        public synchronized Integer get() {
            waitDone();
            return Integer.valueOf(this.mResult);
        }

        @Override
        public synchronized void waitDone() {
            while (!isDone()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.d("Gallery2/Gallery.MultiSetSync", "waitDone() interrupted");
                }
            }
        }

        @Override
        public void onSyncDone(MediaSet mediaSet, int i) {
            SyncListener syncListener;
            synchronized (this) {
                if (i == 2) {
                    try {
                        this.mResult = 2;
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                this.mPendingCount--;
                if (this.mPendingCount == 0) {
                    syncListener = this.mListener;
                    notifyAll();
                } else {
                    syncListener = null;
                }
                Log.d("Gallery2/Gallery.MultiSetSync", "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " #pending=" + this.mPendingCount);
            }
            if (syncListener != null) {
                syncListener.onSyncDone(MediaSet.this, this.mResult);
            }
        }
    }

    public long reloadForSlideShow() {
        return reload();
    }

    public void stopReload() {
    }
}
