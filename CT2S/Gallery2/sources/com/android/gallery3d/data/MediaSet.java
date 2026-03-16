package com.android.gallery3d.data;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.Future;
import java.util.ArrayList;
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
        public Integer get() {
            return 0;
        }

        @Override
        public void waitDone() {
        }
    };
    private WeakHashMap<ContentListener, Object> mListeners;

    public interface ItemConsumer {
        void consume(int i, MediaItem mediaItem);
    }

    public interface SyncListener {
        void onSyncDone(MediaSet mediaSet, int i);
    }

    public abstract String getName();

    public abstract long reload();

    public MediaSet(Path path, long version) {
        super(path, version);
        this.mListeners = new WeakHashMap<>();
    }

    public int getMediaItemCount() {
        return 0;
    }

    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        return new ArrayList<>();
    }

    public MediaItem getCoverMediaItem() {
        ArrayList<MediaItem> items = getMediaItem(0, 1);
        if (items.size() > 0) {
            return items.get(0);
        }
        int n = getSubMediaSetCount();
        for (int i = 0; i < n; i++) {
            MediaItem cover = getSubMediaSet(i).getCoverMediaItem();
            if (cover != null) {
                return cover;
            }
        }
        return null;
    }

    public int getSubMediaSetCount() {
        return 0;
    }

    public MediaSet getSubMediaSet(int index) {
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
        int total = getMediaItemCount();
        int n = getSubMediaSetCount();
        for (int i = 0; i < n; i++) {
            total += getSubMediaSet(i).getTotalMediaItemCount();
        }
        return total;
    }

    public int getIndexOfItem(Path path, int hint) {
        int start = Math.max(0, hint - 250);
        ArrayList<MediaItem> list = getMediaItem(start, 500);
        int index = getIndexOf(path, list);
        if (index != -1) {
            return start + index;
        }
        int start2 = start == 0 ? 500 : 0;
        ArrayList<MediaItem> list2 = getMediaItem(start2, 500);
        while (true) {
            int index2 = getIndexOf(path, list2);
            if (index2 != -1) {
                return start2 + index2;
            }
            if (list2.size() < 500) {
                return -1;
            }
            start2 += 500;
            list2 = getMediaItem(start2, 500);
        }
    }

    protected int getIndexOf(Path path, ArrayList<MediaItem> list) {
        int n = list.size();
        for (int i = 0; i < n; i++) {
            MediaObject item = list.get(i);
            if (item != null && item.mPath == path) {
                return i;
            }
        }
        return -1;
    }

    public void addContentListener(ContentListener listener) {
        this.mListeners.put(listener, null);
    }

    public void removeContentListener(ContentListener listener) {
        this.mListeners.remove(listener);
    }

    public void notifyContentChanged() {
        for (ContentListener listener : this.mListeners.keySet()) {
            listener.onContentDirty();
        }
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        details.addDetail(1, getName());
        return details;
    }

    public void enumerateMediaItems(ItemConsumer consumer) {
        enumerateMediaItems(consumer, 0);
    }

    public void enumerateTotalMediaItems(ItemConsumer consumer) {
        enumerateTotalMediaItems(consumer, 0);
    }

    protected int enumerateMediaItems(ItemConsumer consumer, int startIndex) {
        int total = getMediaItemCount();
        int start = 0;
        while (start < total) {
            int count = Math.min(500, total - start);
            ArrayList<MediaItem> items = getMediaItem(start, count);
            int n = items.size();
            for (int i = 0; i < n; i++) {
                MediaItem item = items.get(i);
                consumer.consume(startIndex + start + i, item);
            }
            start += count;
        }
        return total;
    }

    protected int enumerateTotalMediaItems(ItemConsumer consumer, int startIndex) {
        int start = 0 + enumerateMediaItems(consumer, startIndex);
        int m = getSubMediaSetCount();
        for (int i = 0; i < m; i++) {
            start += getSubMediaSet(i).enumerateTotalMediaItems(consumer, startIndex + start);
        }
        return start;
    }

    public Future<Integer> requestSync(SyncListener listener) {
        listener.onSyncDone(this, 0);
        return FUTURE_STUB;
    }

    protected Future<Integer> requestSyncOnMultipleSets(MediaSet[] sets, SyncListener listener) {
        return new MultiSetSyncFuture(sets, listener);
    }

    private class MultiSetSyncFuture implements SyncListener, Future<Integer> {
        private final Future<Integer>[] mFutures;
        private final SyncListener mListener;
        private int mPendingCount;
        private boolean mIsCancelled = false;
        private int mResult = -1;

        MultiSetSyncFuture(MediaSet[] sets, SyncListener listener) {
            this.mListener = listener;
            this.mPendingCount = sets.length;
            this.mFutures = new Future[sets.length];
            synchronized (this) {
                int n = sets.length;
                for (int i = 0; i < n; i++) {
                    this.mFutures[i] = sets[i].requestSync(this);
                    Log.d("Gallery.MultiSetSync", "  request sync: " + Utils.maskDebugInfo(sets[i].getName()));
                }
            }
        }

        @Override
        public synchronized void cancel() {
            if (!this.mIsCancelled) {
                this.mIsCancelled = true;
                for (Future<Integer> future : this.mFutures) {
                    future.cancel();
                }
                if (this.mResult < 0) {
                    this.mResult = 1;
                }
            }
        }

        @Override
        public synchronized boolean isCancelled() {
            return this.mIsCancelled;
        }

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
                    Log.d("Gallery.MultiSetSync", "waitDone() interrupted");
                }
            }
        }

        @Override
        public void onSyncDone(MediaSet mediaSet, int resultCode) {
            SyncListener listener = null;
            synchronized (this) {
                if (resultCode == 2) {
                    this.mResult = 2;
                    this.mPendingCount--;
                    if (this.mPendingCount == 0) {
                        listener = this.mListener;
                        notifyAll();
                    }
                    Log.d("Gallery.MultiSetSync", "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " #pending=" + this.mPendingCount);
                } else {
                    this.mPendingCount--;
                    if (this.mPendingCount == 0) {
                    }
                    Log.d("Gallery.MultiSetSync", "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " #pending=" + this.mPendingCount);
                }
            }
            if (listener != null) {
                listener.onSyncDone(MediaSet.this, this.mResult);
            }
        }
    }
}
