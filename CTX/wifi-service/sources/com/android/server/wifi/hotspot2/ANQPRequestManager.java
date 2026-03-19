package com.android.server.wifi.hotspot2;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ANQPRequestManager {

    @VisibleForTesting
    public static final int BASE_HOLDOFF_TIME_MILLISECONDS = 10000;

    @VisibleForTesting
    public static final int MAX_HOLDOFF_COUNT = 6;
    private static final List<Constants.ANQPElementType> R1_ANQP_BASE_SET = Arrays.asList(Constants.ANQPElementType.ANQPVenueName, Constants.ANQPElementType.ANQPIPAddrAvailability, Constants.ANQPElementType.ANQPNAIRealm, Constants.ANQPElementType.ANQP3GPPNetwork, Constants.ANQPElementType.ANQPDomName);
    private static final List<Constants.ANQPElementType> R2_ANQP_BASE_SET = Arrays.asList(Constants.ANQPElementType.HSFriendlyName, Constants.ANQPElementType.HSWANMetrics, Constants.ANQPElementType.HSConnCapability, Constants.ANQPElementType.HSOSUProviders);
    private static final String TAG = "ANQPRequestManager";
    private final Clock mClock;
    private final PasspointEventHandler mPasspointHandler;
    private final Map<Long, ANQPNetworkKey> mPendingQueries = new HashMap();
    private final Map<Long, HoldOffInfo> mHoldOffInfo = new HashMap();

    private class HoldOffInfo {
        public int holdOffCount;
        public long holdOffExpirationTime;

        private HoldOffInfo() {
        }
    }

    public ANQPRequestManager(PasspointEventHandler passpointEventHandler, Clock clock) {
        this.mPasspointHandler = passpointEventHandler;
        this.mClock = clock;
    }

    public boolean requestANQPElements(long j, ANQPNetworkKey aNQPNetworkKey, boolean z, boolean z2) {
        if (!canSendRequestNow(j) || !this.mPasspointHandler.requestANQP(j, getRequestElementIDs(z, z2))) {
            return false;
        }
        updateHoldOffInfo(j);
        this.mPendingQueries.put(Long.valueOf(j), aNQPNetworkKey);
        return true;
    }

    public ANQPNetworkKey onRequestCompleted(long j, boolean z) {
        if (z) {
            this.mHoldOffInfo.remove(Long.valueOf(j));
        }
        return this.mPendingQueries.remove(Long.valueOf(j));
    }

    private boolean canSendRequestNow(long j) {
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        HoldOffInfo holdOffInfo = this.mHoldOffInfo.get(Long.valueOf(j));
        if (holdOffInfo != null && holdOffInfo.holdOffExpirationTime > elapsedSinceBootMillis) {
            Log.d(TAG, "Not allowed to send ANQP request to " + j + " for another " + ((holdOffInfo.holdOffExpirationTime - elapsedSinceBootMillis) / 1000) + " seconds");
            return false;
        }
        return true;
    }

    private void updateHoldOffInfo(long j) {
        HoldOffInfo holdOffInfo = this.mHoldOffInfo.get(Long.valueOf(j));
        if (holdOffInfo == null) {
            holdOffInfo = new HoldOffInfo();
            this.mHoldOffInfo.put(Long.valueOf(j), holdOffInfo);
        }
        holdOffInfo.holdOffExpirationTime = this.mClock.getElapsedSinceBootMillis() + ((long) (10000 * (1 << holdOffInfo.holdOffCount)));
        if (holdOffInfo.holdOffCount < 6) {
            holdOffInfo.holdOffCount++;
        }
    }

    private static List<Constants.ANQPElementType> getRequestElementIDs(boolean z, boolean z2) {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(R1_ANQP_BASE_SET);
        if (z) {
            arrayList.add(Constants.ANQPElementType.ANQPRoamingConsortium);
        }
        if (z2) {
            arrayList.addAll(R2_ANQP_BASE_SET);
        }
        return arrayList;
    }
}
