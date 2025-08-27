package com.android.systemui.util.leak;

import android.os.SystemClock;
import android.util.ArrayMap;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
public class TrackedGarbage {
    private final HashSet<LeakReference> mGarbage = new HashSet<>();
    private final ReferenceQueue<Object> mRefQueue = new ReferenceQueue<>();
    private final TrackedCollections mTrackedCollections;

    public TrackedGarbage(TrackedCollections trackedCollections) {
        this.mTrackedCollections = trackedCollections;
    }

    public synchronized void track(Object obj) {
        cleanUp();
        this.mGarbage.add(new LeakReference(obj, this.mRefQueue));
        this.mTrackedCollections.track(this.mGarbage, "Garbage");
    }

    private void cleanUp() {
        while (true) {
            Reference<? extends Object> referencePoll = this.mRefQueue.poll();
            if (referencePoll != null) {
                this.mGarbage.remove(referencePoll);
            } else {
                return;
            }
        }
    }

    private static class LeakReference extends WeakReference<Object> {
        private final Class<?> clazz;
        private final long createdUptimeMillis;

        LeakReference(Object obj, ReferenceQueue<Object> referenceQueue) {
            super(obj, referenceQueue);
            this.clazz = obj.getClass();
            this.createdUptimeMillis = SystemClock.uptimeMillis();
        }
    }

    public synchronized void dump(PrintWriter printWriter) {
        cleanUp();
        long jUptimeMillis = SystemClock.uptimeMillis();
        ArrayMap arrayMap = new ArrayMap();
        ArrayMap arrayMap2 = new ArrayMap();
        Iterator<LeakReference> it = this.mGarbage.iterator();
        while (it.hasNext()) {
            LeakReference next = it.next();
            arrayMap.put(next.clazz, Integer.valueOf(((Integer) arrayMap.getOrDefault(next.clazz, 0)).intValue() + 1));
            if (isOld(next.createdUptimeMillis, jUptimeMillis)) {
                arrayMap2.put(next.clazz, Integer.valueOf(((Integer) arrayMap2.getOrDefault(next.clazz, 0)).intValue() + 1));
            }
        }
        for (Map.Entry entry : arrayMap.entrySet()) {
            printWriter.print(((Class) entry.getKey()).getName());
            printWriter.print(": ");
            printWriter.print(entry.getValue());
            printWriter.print(" total, ");
            printWriter.print(arrayMap2.getOrDefault(entry.getKey(), 0));
            printWriter.print(" old");
            printWriter.println();
        }
    }

    public synchronized int countOldGarbage() {
        int i;
        cleanUp();
        long jUptimeMillis = SystemClock.uptimeMillis();
        i = 0;
        Iterator<LeakReference> it = this.mGarbage.iterator();
        while (it.hasNext()) {
            if (isOld(it.next().createdUptimeMillis, jUptimeMillis)) {
                i++;
            }
        }
        return i;
    }

    private boolean isOld(long j, long j2) {
        return j + 60000 < j2;
    }
}
