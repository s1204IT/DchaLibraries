package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

public class SerialBitmapReferenceCounter implements BitmapReferenceCounter {
    private final Map<Bitmap, InnerTracker> counter = new WeakHashMap();
    private final InnerTrackerPool pool = new InnerTrackerPool();
    private final BitmapPool target;

    private static class InnerTrackerPool {
        private LinkedList<InnerTracker> pool;

        private InnerTrackerPool() {
            this.pool = new LinkedList<>();
        }

        public InnerTracker get() {
            InnerTracker result = this.pool.poll();
            if (result == null) {
                return new InnerTracker();
            }
            return result;
        }

        public void release(InnerTracker innerTracker) {
            this.pool.offer(innerTracker);
        }
    }

    private static class InnerTracker {
        private int refs;

        private InnerTracker() {
            this.refs = 0;
        }

        public void acquire() {
            this.refs++;
        }

        public boolean release() {
            this.refs--;
            return this.refs == 0;
        }
    }

    public SerialBitmapReferenceCounter(BitmapPool target) {
        this.target = target;
    }

    private void initBitmap(Bitmap toInit) {
        InnerTracker tracker = this.counter.get(toInit);
        if (tracker == null) {
            this.counter.put(toInit, this.pool.get());
        }
    }

    @Override
    public void acquireBitmap(Bitmap bitmap) {
        initBitmap(bitmap);
        this.counter.get(bitmap).acquire();
    }

    @Override
    public void releaseBitmap(Bitmap bitmap) {
        InnerTracker tracker = this.counter.get(bitmap);
        if (tracker.release()) {
            recycle(tracker, bitmap);
        }
    }

    private void recycle(InnerTracker tracker, Bitmap bitmap) {
        if (!this.target.put(bitmap)) {
            bitmap.recycle();
        }
        this.counter.remove(bitmap);
        this.pool.release(tracker);
    }
}
