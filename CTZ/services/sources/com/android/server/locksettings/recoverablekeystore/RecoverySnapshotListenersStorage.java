package com.android.server.locksettings.recoverablekeystore;

import android.app.PendingIntent;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;

public class RecoverySnapshotListenersStorage {
    private static final String TAG = "RecoverySnapshotLstnrs";

    @GuardedBy("this")
    private SparseArray<PendingIntent> mAgentIntents = new SparseArray<>();

    @GuardedBy("this")
    private ArraySet<Integer> mAgentsWithPendingSnapshots = new ArraySet<>();

    public synchronized void setSnapshotListener(int i, PendingIntent pendingIntent) {
        Log.i(TAG, "Registered listener for agent with uid " + i);
        this.mAgentIntents.put(i, pendingIntent);
        if (this.mAgentsWithPendingSnapshots.contains(Integer.valueOf(i))) {
            Log.i(TAG, "Snapshot already created for agent. Immediately triggering intent.");
            tryToSendIntent(i, pendingIntent);
        }
    }

    public synchronized boolean hasListener(int i) {
        return this.mAgentIntents.get(i) != null;
    }

    public synchronized void recoverySnapshotAvailable(int i) {
        PendingIntent pendingIntent = this.mAgentIntents.get(i);
        if (pendingIntent == null) {
            Log.i(TAG, "Snapshot available for agent " + i + " but agent has not yet initialized. Will notify agent when it does.");
            this.mAgentsWithPendingSnapshots.add(Integer.valueOf(i));
            return;
        }
        tryToSendIntent(i, pendingIntent);
    }

    private synchronized void tryToSendIntent(int i, PendingIntent pendingIntent) {
        try {
            pendingIntent.send();
            this.mAgentsWithPendingSnapshots.remove(Integer.valueOf(i));
            Log.d(TAG, "Successfully notified listener.");
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Failed to trigger PendingIntent for " + i, e);
            this.mAgentsWithPendingSnapshots.add(Integer.valueOf(i));
        }
    }
}
