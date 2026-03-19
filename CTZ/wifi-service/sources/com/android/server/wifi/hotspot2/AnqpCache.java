package com.android.server.wifi.hotspot2;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AnqpCache {

    @VisibleForTesting
    public static final long CACHE_SWEEP_INTERVAL_MILLISECONDS = 60000;
    private final Map<ANQPNetworkKey, ANQPData> mANQPCache = new HashMap();
    private Clock mClock;
    private long mLastSweep;

    public AnqpCache(Clock clock) {
        this.mClock = clock;
        this.mLastSweep = this.mClock.getElapsedSinceBootMillis();
    }

    public void addEntry(ANQPNetworkKey aNQPNetworkKey, Map<Constants.ANQPElementType, ANQPElement> map) {
        this.mANQPCache.put(aNQPNetworkKey, new ANQPData(this.mClock, map));
    }

    public ANQPData getEntry(ANQPNetworkKey aNQPNetworkKey) {
        return this.mANQPCache.get(aNQPNetworkKey);
    }

    public void sweep() {
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        if (elapsedSinceBootMillis < this.mLastSweep + CACHE_SWEEP_INTERVAL_MILLISECONDS) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<ANQPNetworkKey, ANQPData> entry : this.mANQPCache.entrySet()) {
            if (entry.getValue().expired(elapsedSinceBootMillis)) {
                arrayList.add(entry.getKey());
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            this.mANQPCache.remove((ANQPNetworkKey) it.next());
        }
        this.mLastSweep = elapsedSinceBootMillis;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Last sweep " + Utils.toHMS(this.mClock.getElapsedSinceBootMillis() - this.mLastSweep) + " ago.");
        for (Map.Entry<ANQPNetworkKey, ANQPData> entry : this.mANQPCache.entrySet()) {
            printWriter.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
