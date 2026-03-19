package com.android.internal.telephony;

import android.telephony.ClientRequestStats;
import android.telephony.Rlog;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Iterator;

public class ClientWakelockAccountant {
    public static final String LOG_TAG = "ClientWakelockAccountant: ";

    @VisibleForTesting
    public ClientRequestStats mRequestStats = new ClientRequestStats();

    @VisibleForTesting
    public ArrayList<RilWakelockInfo> mPendingRilWakelocks = new ArrayList<>();

    @VisibleForTesting
    public ClientWakelockAccountant(String str) {
        this.mRequestStats.setCallingPackage(str);
    }

    @VisibleForTesting
    public void startAttributingWakelock(int i, int i2, int i3, long j) {
        RilWakelockInfo rilWakelockInfo = new RilWakelockInfo(i, i2, i3, j);
        synchronized (this.mPendingRilWakelocks) {
            this.mPendingRilWakelocks.add(rilWakelockInfo);
        }
    }

    @VisibleForTesting
    public void stopAttributingWakelock(int i, int i2, long j) {
        RilWakelockInfo rilWakelockInfoRemovePendingWakelock = removePendingWakelock(i, i2);
        if (rilWakelockInfoRemovePendingWakelock != null) {
            completeRequest(rilWakelockInfoRemovePendingWakelock, j);
        }
    }

    @VisibleForTesting
    public void stopAllPendingRequests(long j) {
        synchronized (this.mPendingRilWakelocks) {
            Iterator<RilWakelockInfo> it = this.mPendingRilWakelocks.iterator();
            while (it.hasNext()) {
                completeRequest(it.next(), j);
            }
            this.mPendingRilWakelocks.clear();
        }
    }

    @VisibleForTesting
    public void changeConcurrentRequests(int i, long j) {
        synchronized (this.mPendingRilWakelocks) {
            Iterator<RilWakelockInfo> it = this.mPendingRilWakelocks.iterator();
            while (it.hasNext()) {
                it.next().updateConcurrentRequests(i, j);
            }
        }
    }

    private void completeRequest(RilWakelockInfo rilWakelockInfo, long j) {
        rilWakelockInfo.setResponseTime(j);
        synchronized (this.mRequestStats) {
            this.mRequestStats.addCompletedWakelockTime(rilWakelockInfo.getWakelockTimeAttributedToClient());
            this.mRequestStats.incrementCompletedRequestsCount();
            this.mRequestStats.updateRequestHistograms(rilWakelockInfo.getRilRequestSent(), (int) rilWakelockInfo.getWakelockTimeAttributedToClient());
        }
    }

    @VisibleForTesting
    public int getPendingRequestCount() {
        return this.mPendingRilWakelocks.size();
    }

    @VisibleForTesting
    public synchronized long updatePendingRequestWakelockTime(long j) {
        long wakelockTimeAttributedToClient;
        wakelockTimeAttributedToClient = 0;
        synchronized (this.mPendingRilWakelocks) {
            for (RilWakelockInfo rilWakelockInfo : this.mPendingRilWakelocks) {
                rilWakelockInfo.updateTime(j);
                wakelockTimeAttributedToClient += rilWakelockInfo.getWakelockTimeAttributedToClient();
            }
        }
        synchronized (this.mRequestStats) {
            this.mRequestStats.setPendingRequestsCount(getPendingRequestCount());
            this.mRequestStats.setPendingRequestsWakelockTime(wakelockTimeAttributedToClient);
        }
        return wakelockTimeAttributedToClient;
    }

    private RilWakelockInfo removePendingWakelock(int i, int i2) {
        RilWakelockInfo rilWakelockInfo;
        synchronized (this.mPendingRilWakelocks) {
            rilWakelockInfo = null;
            for (RilWakelockInfo rilWakelockInfo2 : this.mPendingRilWakelocks) {
                if (rilWakelockInfo2.getTokenNumber() == i2 && rilWakelockInfo2.getRilRequestSent() == i) {
                    rilWakelockInfo = rilWakelockInfo2;
                }
            }
            if (rilWakelockInfo != null) {
                this.mPendingRilWakelocks.remove(rilWakelockInfo);
            }
        }
        if (rilWakelockInfo == null) {
            Rlog.w(LOG_TAG, "Looking for Request<" + i + "," + i2 + "> in " + this.mPendingRilWakelocks);
        }
        return rilWakelockInfo;
    }

    public String toString() {
        return "ClientWakelockAccountant{mRequestStats=" + this.mRequestStats + ", mPendingRilWakelocks=" + this.mPendingRilWakelocks + '}';
    }
}
