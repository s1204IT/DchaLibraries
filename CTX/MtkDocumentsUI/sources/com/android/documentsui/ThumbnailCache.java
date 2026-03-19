package com.android.documentsui;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.util.LruCache;
import android.util.Pair;
import android.util.Pools;
import com.android.documentsui.base.Shared;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

public class ThumbnailCache {
    static final boolean $assertionsDisabled = false;
    private static final SizeComparator SIZE_COMPARATOR = new SizeComparator();
    private final Cache mCache;
    private final HashMap<Uri, TreeMap<Point, Pair<Uri, Point>>> mSizeIndex = new HashMap<>();

    public ThumbnailCache(int i) {
        this.mCache = new Cache(i);
    }

    public Result getThumbnail(Uri uri, Point point) {
        Pair<Uri, Point> pair;
        Entry entry;
        Pair<Uri, Point> pair2;
        Entry entry2;
        Entry entry3;
        TreeMap<Point, Pair<Uri, Point>> treeMap = this.mSizeIndex.get(uri);
        if (treeMap != null && !treeMap.isEmpty()) {
            Pair<Uri, Point> pair3 = treeMap.get(point);
            if (pair3 == null || (entry3 = this.mCache.get(pair3)) == null) {
                Point pointHigherKey = treeMap.higherKey(point);
                if (pointHigherKey == null || (pair2 = treeMap.get(pointHigherKey)) == null || (entry2 = this.mCache.get(pair2)) == null) {
                    Point pointLowerKey = treeMap.lowerKey(point);
                    return (pointLowerKey == null || (pair = treeMap.get(pointLowerKey)) == null || (entry = this.mCache.get(pair)) == null) ? Result.obtainMiss() : Result.obtain(2, pointLowerKey, entry);
                }
                return Result.obtain(3, pointHigherKey, entry2);
            }
            return Result.obtain(1, point, entry3);
        }
        return Result.obtainMiss();
    }

    public void putThumbnail(Uri uri, Point point, Bitmap bitmap, long j) {
        TreeMap<Point, Pair<Uri, Point>> treeMap;
        Pair<Uri, Point> pairCreate = Pair.create(uri, point);
        synchronized (this.mSizeIndex) {
            treeMap = this.mSizeIndex.get(uri);
            if (treeMap == null) {
                treeMap = new TreeMap<>(SIZE_COMPARATOR);
                this.mSizeIndex.put(uri, treeMap);
            }
        }
        this.mCache.put(pairCreate, new Entry(bitmap, j));
        synchronized (treeMap) {
            treeMap.put(point, pairCreate);
        }
    }

    public void removeUri(Uri uri) {
        TreeMap<Point, Pair<Uri, Point>> treeMap;
        synchronized (this.mSizeIndex) {
            treeMap = this.mSizeIndex.get(uri);
        }
        if (treeMap != null) {
            for (Pair pair : (Pair[]) treeMap.values().toArray(new Pair[0])) {
                this.mCache.remove(pair);
            }
        }
    }

    private void removeKey(Uri uri, Point point) {
        TreeMap<Point, Pair<Uri, Point>> treeMap;
        synchronized (this.mSizeIndex) {
            treeMap = this.mSizeIndex.get(uri);
        }
        synchronized (treeMap) {
            treeMap.remove(point);
        }
    }

    public void onTrimMemory(int i) {
        if (i >= 60) {
            this.mCache.evictAll();
        } else if (i >= 40) {
            this.mCache.trimToSize(this.mCache.size() / 2);
        }
    }

    public static final class Result {
        static final boolean $assertionsDisabled = false;
        private static final Pools.SimplePool<Result> sPool = new Pools.SimplePool<>(1);
        private long mLastModified;
        private Point mSize;
        private int mStatus;
        private Bitmap mThumbnail;

        private static Result obtainMiss() {
            return obtain(0, null, null, 0L);
        }

        private static Result obtain(int i, Point point, Entry entry) {
            return obtain(i, entry.mThumbnail, point, entry.mLastModified);
        }

        private static Result obtain(int i, Bitmap bitmap, Point point, long j) {
            Shared.checkMainLoop();
            Result result = (Result) sPool.acquire();
            if (result == null) {
                result = new Result();
            }
            result.mStatus = i;
            result.mThumbnail = bitmap;
            result.mSize = point;
            result.mLastModified = j;
            return result;
        }

        private Result() {
        }

        public void recycle() {
            Shared.checkMainLoop();
            this.mStatus = -1;
            this.mThumbnail = null;
            this.mSize = null;
            this.mLastModified = -1L;
            sPool.release(this);
        }

        public int getStatus() {
            return this.mStatus;
        }

        public Bitmap getThumbnail() {
            return this.mThumbnail;
        }

        public long getLastModified() {
            return this.mLastModified;
        }

        public boolean isHit() {
            return this.mStatus != 0;
        }

        public boolean isExactHit() {
            return this.mStatus == 1;
        }
    }

    private static final class Entry {
        private final long mLastModified;
        private final Bitmap mThumbnail;

        private Entry(Bitmap bitmap, long j) {
            this.mThumbnail = bitmap;
            this.mLastModified = j;
        }
    }

    private final class Cache extends LruCache<Pair<Uri, Point>, Entry> {
        private Cache(int i) {
            super(i);
        }

        @Override
        protected int sizeOf(Pair<Uri, Point> pair, Entry entry) {
            return entry.mThumbnail.getByteCount();
        }

        @Override
        protected void entryRemoved(boolean z, Pair<Uri, Point> pair, Entry entry, Entry entry2) {
            if (entry2 == null) {
                ThumbnailCache.this.removeKey((Uri) pair.first, (Point) pair.second);
            }
        }
    }

    private static final class SizeComparator implements Comparator<Point> {
        private SizeComparator() {
        }

        @Override
        public int compare(Point point, Point point2) {
            return point.x - point2.x;
        }
    }
}
