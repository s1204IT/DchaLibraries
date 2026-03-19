package com.android.server.wifi.hotspot2;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ANQPData {

    @VisibleForTesting
    public static final long DATA_LIFETIME_MILLISECONDS = 3600000;
    private final Map<Constants.ANQPElementType, ANQPElement> mANQPElements = new HashMap();
    private final Clock mClock;
    private final long mExpiryTime;

    public ANQPData(Clock clock, Map<Constants.ANQPElementType, ANQPElement> map) {
        this.mClock = clock;
        if (map != null) {
            this.mANQPElements.putAll(map);
        }
        this.mExpiryTime = this.mClock.getElapsedSinceBootMillis() + 3600000;
    }

    public Map<Constants.ANQPElementType, ANQPElement> getElements() {
        return Collections.unmodifiableMap(this.mANQPElements);
    }

    public boolean expired(long j) {
        return this.mExpiryTime <= j;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.mANQPElements.size());
        sb.append(" elements, ");
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        sb.append(" expires in ");
        sb.append(Utils.toHMS(this.mExpiryTime - elapsedSinceBootMillis));
        sb.append(' ');
        sb.append(expired(elapsedSinceBootMillis) ? 'x' : '-');
        return sb.toString();
    }
}
