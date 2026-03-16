package com.android.server.content;

import android.content.SyncAdapterType;
import android.content.SyncAdaptersCache;
import android.content.pm.PackageManager;
import android.content.pm.RegisteredServicesCache;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import com.android.server.content.SyncStorageEngine;
import com.google.android.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SyncQueue {
    private static final String TAG = "SyncManager";
    private final HashMap<String, SyncOperation> mOperationsMap = Maps.newHashMap();
    private final PackageManager mPackageManager;
    private final SyncAdaptersCache mSyncAdapters;
    private final SyncStorageEngine mSyncStorageEngine;

    public SyncQueue(PackageManager packageManager, SyncStorageEngine syncStorageEngine, SyncAdaptersCache syncAdapters) {
        this.mPackageManager = packageManager;
        this.mSyncStorageEngine = syncStorageEngine;
        this.mSyncAdapters = syncAdapters;
    }

    public void addPendingOperations(int userId) {
        for (SyncStorageEngine.PendingOperation op : this.mSyncStorageEngine.getPendingOperations()) {
            SyncStorageEngine.EndPoint info = op.target;
            if (info.userId == userId) {
                Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(info);
                if (info.target_provider) {
                    RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(info.provider, info.account.type), info.userId);
                    if (syncAdapterInfo == null) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.v("SyncManager", "Missing sync adapter info for authority " + op.target);
                        }
                    } else {
                        SyncOperation operationToAdd = new SyncOperation(info.account, info.userId, op.reason, op.syncSource, info.provider, op.extras, op.expedited ? -1L : 0L, 0L, backoff != null ? ((Long) backoff.first).longValue() : 0L, this.mSyncStorageEngine.getDelayUntilTime(info), ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs());
                        operationToAdd.pendingOperation = op;
                        add(operationToAdd, op);
                    }
                } else if (info.target_service) {
                    try {
                        this.mPackageManager.getServiceInfo(info.service, 0);
                        SyncOperation operationToAdd2 = new SyncOperation(info.service, info.userId, op.reason, op.syncSource, op.extras, op.expedited ? -1L : 0L, 0L, backoff != null ? ((Long) backoff.first).longValue() : 0L, this.mSyncStorageEngine.getDelayUntilTime(info));
                        operationToAdd2.pendingOperation = op;
                        add(operationToAdd2, op);
                    } catch (PackageManager.NameNotFoundException e) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.w("SyncManager", "Missing sync service for authority " + op.target);
                        }
                    }
                }
            }
        }
    }

    public boolean add(SyncOperation operation) {
        return add(operation, null);
    }

    private boolean add(SyncOperation operation, SyncStorageEngine.PendingOperation pop) {
        String operationKey = operation.key;
        SyncOperation existingOperation = this.mOperationsMap.get(operationKey);
        if (existingOperation != null) {
            if (operation.compareTo(existingOperation) > 0) {
                return false;
            }
            long newRunTime = Math.min(existingOperation.latestRunTime, operation.latestRunTime);
            existingOperation.latestRunTime = newRunTime;
            existingOperation.flexTime = operation.flexTime;
            return true;
        }
        operation.pendingOperation = pop;
        if (operation.pendingOperation == null) {
            SyncStorageEngine.PendingOperation pop2 = this.mSyncStorageEngine.insertIntoPending(operation);
            if (pop2 == null) {
                throw new IllegalStateException("error adding pending sync operation " + operation);
            }
            operation.pendingOperation = pop2;
        }
        this.mOperationsMap.put(operationKey, operation);
        return true;
    }

    public void removeUserLocked(int userId) {
        ArrayList<SyncOperation> opsToRemove = new ArrayList<>();
        for (SyncOperation op : this.mOperationsMap.values()) {
            if (op.target.userId == userId) {
                opsToRemove.add(op);
            }
        }
        Iterator<SyncOperation> it = opsToRemove.iterator();
        while (it.hasNext()) {
            remove(it.next());
        }
    }

    public void remove(SyncOperation operation) {
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        SyncOperation operationToRemove = this.mOperationsMap.remove(operation.key);
        if (isLoggable) {
            Log.v("SyncManager", "Attempting to remove: " + operation.key);
        }
        if (operationToRemove == null) {
            if (isLoggable) {
                Log.v("SyncManager", "Could not find: " + operation.key);
            }
        } else if (!this.mSyncStorageEngine.deleteFromPending(operationToRemove.pendingOperation)) {
            String errorMessage = "unable to find pending row for " + operationToRemove;
            Log.e("SyncManager", errorMessage, new IllegalStateException(errorMessage));
        }
    }

    public void clearBackoffs() {
        for (SyncOperation op : this.mOperationsMap.values()) {
            op.backoff = 0L;
            op.updateEffectiveRunTime();
        }
    }

    public void onBackoffChanged(SyncStorageEngine.EndPoint target, long backoff) {
        for (SyncOperation op : this.mOperationsMap.values()) {
            if (op.target.matchesSpec(target)) {
                op.backoff = backoff;
                op.updateEffectiveRunTime();
            }
        }
    }

    public void onDelayUntilTimeChanged(SyncStorageEngine.EndPoint target, long delayUntil) {
        for (SyncOperation op : this.mOperationsMap.values()) {
            if (op.target.matchesSpec(target)) {
                op.delayUntil = delayUntil;
                op.updateEffectiveRunTime();
            }
        }
    }

    public void remove(SyncStorageEngine.EndPoint info, Bundle extras) {
        Iterator<Map.Entry<String, SyncOperation>> entries = this.mOperationsMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, SyncOperation> entry = entries.next();
            SyncOperation syncOperation = entry.getValue();
            SyncStorageEngine.EndPoint opInfo = syncOperation.target;
            if (opInfo.matchesSpec(info) && (extras == null || SyncManager.syncExtrasEquals(syncOperation.extras, extras, false))) {
                entries.remove();
                if (!this.mSyncStorageEngine.deleteFromPending(syncOperation.pendingOperation)) {
                    String errorMessage = "unable to find pending row for " + syncOperation;
                    Log.e("SyncManager", errorMessage, new IllegalStateException(errorMessage));
                }
            }
        }
    }

    public Collection<SyncOperation> getOperations() {
        return this.mOperationsMap.values();
    }

    public void dump(StringBuilder sb) {
        long now = SystemClock.elapsedRealtime();
        sb.append("SyncQueue: ").append(this.mOperationsMap.size()).append(" operation(s)\n");
        for (SyncOperation operation : this.mOperationsMap.values()) {
            sb.append("  ");
            if (operation.effectiveRunTime <= now) {
                sb.append("READY");
            } else {
                sb.append(DateUtils.formatElapsedTime((operation.effectiveRunTime - now) / 1000));
            }
            sb.append(" - ");
            sb.append(operation.dump(this.mPackageManager, false)).append("\n");
        }
    }
}
