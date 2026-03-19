package com.android.internal.telephony;

import android.os.SystemClock;
import android.telephony.ClientRequestStats;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ClientWakelockTracker {
    public static final String LOG_TAG = "ClientWakelockTracker";

    @VisibleForTesting
    public HashMap<String, ClientWakelockAccountant> mClients = new HashMap<>();

    @VisibleForTesting
    public ArrayList<ClientWakelockAccountant> mActiveClients = new ArrayList<>();

    @VisibleForTesting
    public void startTracking(String str, int i, int i2, int i3) {
        ClientWakelockAccountant clientWakelockAccountant = getClientWakelockAccountant(str);
        long jUptimeMillis = SystemClock.uptimeMillis();
        clientWakelockAccountant.startAttributingWakelock(i, i2, i3, jUptimeMillis);
        updateConcurrentRequests(i3, jUptimeMillis);
        synchronized (this.mActiveClients) {
            if (!this.mActiveClients.contains(clientWakelockAccountant)) {
                this.mActiveClients.add(clientWakelockAccountant);
            }
        }
    }

    @VisibleForTesting
    public void stopTracking(String str, int i, int i2, int i3) {
        ClientWakelockAccountant clientWakelockAccountant = getClientWakelockAccountant(str);
        long jUptimeMillis = SystemClock.uptimeMillis();
        clientWakelockAccountant.stopAttributingWakelock(i, i2, jUptimeMillis);
        if (clientWakelockAccountant.getPendingRequestCount() == 0) {
            synchronized (this.mActiveClients) {
                this.mActiveClients.remove(clientWakelockAccountant);
            }
        }
        updateConcurrentRequests(i3, jUptimeMillis);
    }

    @VisibleForTesting
    public void stopTrackingAll() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        synchronized (this.mActiveClients) {
            Iterator<ClientWakelockAccountant> it = this.mActiveClients.iterator();
            while (it.hasNext()) {
                it.next().stopAllPendingRequests(jUptimeMillis);
            }
            this.mActiveClients.clear();
        }
    }

    List<ClientRequestStats> getClientRequestStats() {
        ArrayList arrayList;
        long jUptimeMillis = SystemClock.uptimeMillis();
        synchronized (this.mClients) {
            arrayList = new ArrayList(this.mClients.size());
            Iterator<String> it = this.mClients.keySet().iterator();
            while (it.hasNext()) {
                ClientWakelockAccountant clientWakelockAccountant = this.mClients.get(it.next());
                clientWakelockAccountant.updatePendingRequestWakelockTime(jUptimeMillis);
                arrayList.add(new ClientRequestStats(clientWakelockAccountant.mRequestStats));
            }
        }
        return arrayList;
    }

    private ClientWakelockAccountant getClientWakelockAccountant(String str) {
        ClientWakelockAccountant clientWakelockAccountant;
        synchronized (this.mClients) {
            if (this.mClients.containsKey(str)) {
                clientWakelockAccountant = this.mClients.get(str);
            } else {
                ClientWakelockAccountant clientWakelockAccountant2 = new ClientWakelockAccountant(str);
                this.mClients.put(str, clientWakelockAccountant2);
                clientWakelockAccountant = clientWakelockAccountant2;
            }
        }
        return clientWakelockAccountant;
    }

    private void updateConcurrentRequests(int i, long j) {
        if (i != 0) {
            synchronized (this.mActiveClients) {
                Iterator<ClientWakelockAccountant> it = this.mActiveClients.iterator();
                while (it.hasNext()) {
                    it.next().changeConcurrentRequests(i, j);
                }
            }
        }
    }

    public boolean isClientActive(String str) {
        ClientWakelockAccountant clientWakelockAccountant = getClientWakelockAccountant(str);
        synchronized (this.mActiveClients) {
            if (this.mActiveClients.contains(clientWakelockAccountant)) {
                return true;
            }
            return false;
        }
    }

    void dumpClientRequestTracker(PrintWriter printWriter) {
        printWriter.println("-------mClients---------------");
        synchronized (this.mClients) {
            for (String str : this.mClients.keySet()) {
                printWriter.println("Client : " + str);
                printWriter.println(this.mClients.get(str).toString());
            }
        }
    }
}
