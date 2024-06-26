package com.android.systemui.util.leak;

import android.os.SystemClock;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class TrackedCollections {
    private final WeakIdentityHashMap<Collection<?>, CollectionState> mCollections = new WeakIdentityHashMap<>();

    public synchronized void track(Collection<?> collection, String str) {
        CollectionState collectionState = this.mCollections.get(collection);
        if (collectionState == null) {
            collectionState = new CollectionState();
            collectionState.tag = str;
            collectionState.startUptime = SystemClock.uptimeMillis();
            this.mCollections.put(collection, collectionState);
        }
        if (collectionState.halfwayCount == -1 && SystemClock.uptimeMillis() - collectionState.startUptime > 1800000) {
            collectionState.halfwayCount = collectionState.lastCount;
        }
        collectionState.lastCount = collection.size();
        collectionState.lastUptime = SystemClock.uptimeMillis();
    }

    /* loaded from: classes.dex */
    private static class CollectionState {
        int halfwayCount;
        int lastCount;
        long lastUptime;
        long startUptime;
        String tag;

        private CollectionState() {
            this.halfwayCount = -1;
            this.lastCount = -1;
        }

        void dump(PrintWriter printWriter) {
            long uptimeMillis = SystemClock.uptimeMillis();
            printWriter.format("%s: %.2f (start-30min) / %.2f (30min-now) / %.2f (start-now) (growth rate in #/hour); %d (current size)", this.tag, Float.valueOf(ratePerHour(this.startUptime, 0, this.startUptime + 1800000, this.halfwayCount)), Float.valueOf(ratePerHour(this.startUptime + 1800000, this.halfwayCount, uptimeMillis, this.lastCount)), Float.valueOf(ratePerHour(this.startUptime, 0, uptimeMillis, this.lastCount)), Integer.valueOf(this.lastCount));
        }

        private float ratePerHour(long j, int i, long j2, int i2) {
            if (j >= j2 || i < 0 || i2 < 0) {
                return Float.NaN;
            }
            return ((i2 - i) / ((float) (j2 - j))) * 60.0f * 60000.0f;
        }
    }

    public synchronized void dump(PrintWriter printWriter, Predicate<Collection<?>> predicate) {
        for (Map.Entry<WeakReference<Collection<?>>, CollectionState> entry : this.mCollections.entrySet()) {
            Collection<?> collection = entry.getKey().get();
            if (predicate == null || (collection != null && predicate.test(collection))) {
                entry.getValue().dump(printWriter);
                printWriter.println();
            }
        }
    }
}
