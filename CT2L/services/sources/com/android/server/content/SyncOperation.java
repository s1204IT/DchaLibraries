package com.android.server.content;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.content.SyncStorageEngine;

public class SyncOperation implements Comparable {
    public static final int REASON_ACCOUNTS_UPDATED = -2;
    public static final int REASON_BACKGROUND_DATA_SETTINGS_CHANGED = -1;
    public static final int REASON_IS_SYNCABLE = -5;
    public static final int REASON_MASTER_SYNC_AUTO = -7;
    private static String[] REASON_NAMES = {"DataSettingsChanged", "AccountsUpdated", "ServiceChanged", "Periodic", "IsSyncable", "AutoSync", "MasterSyncAuto", "UserStart"};
    public static final int REASON_PERIODIC = -4;
    public static final int REASON_SERVICE_CHANGED = -3;
    public static final int REASON_SYNC_AUTO = -6;
    public static final int REASON_USER_START = -8;
    public static final int SYNC_TARGET_ADAPTER = 1;
    public static final int SYNC_TARGET_SERVICE = 2;
    public static final int SYNC_TARGET_UNKNOWN = 0;
    public static final String TAG = "SyncManager";
    public final boolean allowParallelSyncs;
    public long backoff;
    public long delayUntil;
    public long effectiveRunTime;
    private final boolean expedited;
    public Bundle extras;
    public long flexTime;
    public final String key;
    public long latestRunTime;
    public SyncStorageEngine.PendingOperation pendingOperation;
    public final int reason;
    public final int syncSource;
    public final SyncStorageEngine.EndPoint target;
    public String wakeLockName;

    public SyncOperation(Account account, int userId, int reason, int source, String provider, Bundle extras, long runTimeFromNow, long flexTime, long backoff, long delayUntil, boolean allowParallelSyncs) {
        this(new SyncStorageEngine.EndPoint(account, provider, userId), reason, source, extras, runTimeFromNow, flexTime, backoff, delayUntil, allowParallelSyncs);
    }

    public SyncOperation(ComponentName service, int userId, int reason, int source, Bundle extras, long runTimeFromNow, long flexTime, long backoff, long delayUntil) {
        this(new SyncStorageEngine.EndPoint(service, userId), reason, source, extras, runTimeFromNow, flexTime, backoff, delayUntil, true);
    }

    private SyncOperation(SyncStorageEngine.EndPoint info, int reason, int source, Bundle extras, long runTimeFromNow, long flexTime, long backoff, long delayUntil, boolean allowParallelSyncs) {
        this.target = info;
        this.reason = reason;
        this.syncSource = source;
        this.extras = new Bundle(extras);
        cleanBundle(this.extras);
        this.delayUntil = delayUntil;
        this.backoff = backoff;
        this.allowParallelSyncs = allowParallelSyncs;
        long now = SystemClock.elapsedRealtime();
        if (runTimeFromNow < 0) {
            this.expedited = true;
            if (!this.extras.getBoolean("expedited", false)) {
                this.extras.putBoolean("expedited", true);
            }
            this.latestRunTime = now;
            this.flexTime = 0L;
        } else {
            this.expedited = false;
            this.extras.remove("expedited");
            this.latestRunTime = now + runTimeFromNow;
            this.flexTime = flexTime;
        }
        updateEffectiveRunTime();
        this.key = toKey(info, this.extras);
    }

    public SyncOperation(SyncOperation other, long newRunTimeFromNow) {
        this(other.target, other.reason, other.syncSource, new Bundle(other.extras), newRunTimeFromNow, 0L, other.backoff, other.delayUntil, other.allowParallelSyncs);
    }

    public boolean matchesAuthority(SyncOperation other) {
        return this.target.matchesSpec(other.target);
    }

    private void cleanBundle(Bundle bundle) {
        removeFalseExtra(bundle, "upload");
        removeFalseExtra(bundle, "force");
        removeFalseExtra(bundle, "ignore_settings");
        removeFalseExtra(bundle, "ignore_backoff");
        removeFalseExtra(bundle, "do_not_retry");
        removeFalseExtra(bundle, "discard_deletions");
        removeFalseExtra(bundle, "expedited");
        removeFalseExtra(bundle, "deletions_override");
        removeFalseExtra(bundle, "allow_metered");
    }

    private void removeFalseExtra(Bundle bundle, String extraName) {
        if (!bundle.getBoolean(extraName, false)) {
            bundle.remove(extraName);
        }
    }

    public boolean isConflict(SyncOperation toRun) {
        SyncStorageEngine.EndPoint other = toRun.target;
        return this.target.target_provider ? this.target.account.type.equals(other.account.type) && this.target.provider.equals(other.provider) && this.target.userId == other.userId && (!this.allowParallelSyncs || this.target.account.name.equals(other.account.name)) : this.target.service.equals(other.service) && !this.allowParallelSyncs;
    }

    public String toString() {
        return dump(null, true);
    }

    public String dump(PackageManager pm, boolean useOneLine) {
        StringBuilder sb = new StringBuilder();
        if (this.target.target_provider) {
            sb.append(this.target.account.name).append(" u").append(this.target.userId).append(" (").append(this.target.account.type).append(")").append(", ").append(this.target.provider).append(", ");
        } else if (this.target.target_service) {
            sb.append(this.target.service.getPackageName()).append(" u").append(this.target.userId).append(" (").append(this.target.service.getClassName()).append(")").append(", ");
        }
        sb.append(SyncStorageEngine.SOURCES[this.syncSource]).append(", currentRunTime ").append(this.effectiveRunTime);
        if (this.expedited) {
            sb.append(", EXPEDITED");
        }
        sb.append(", reason: ");
        sb.append(reasonToString(pm, this.reason));
        if (!useOneLine && !this.extras.keySet().isEmpty()) {
            sb.append("\n    ");
            extrasToStringBuilder(this.extras, sb);
        }
        return sb.toString();
    }

    public static String reasonToString(PackageManager pm, int reason) {
        if (reason >= 0) {
            if (pm != null) {
                String[] packages = pm.getPackagesForUid(reason);
                if (packages != null && packages.length == 1) {
                    return packages[0];
                }
                String name = pm.getNameForUid(reason);
                if (name == null) {
                    return String.valueOf(reason);
                }
                return name;
            }
            return String.valueOf(reason);
        }
        int index = (-reason) - 1;
        if (index >= REASON_NAMES.length) {
            return String.valueOf(reason);
        }
        return REASON_NAMES[index];
    }

    public boolean isInitialization() {
        return this.extras.getBoolean("initialize", false);
    }

    public boolean isExpedited() {
        return this.expedited;
    }

    public boolean ignoreBackoff() {
        return this.extras.getBoolean("ignore_backoff", false);
    }

    public boolean isNotAllowedOnMetered() {
        return this.extras.getBoolean("allow_metered", false);
    }

    public boolean isManual() {
        return this.extras.getBoolean("force", false);
    }

    public boolean isIgnoreSettings() {
        return this.extras.getBoolean("ignore_settings", false);
    }

    public static String toKey(SyncStorageEngine.EndPoint info, Bundle extras) {
        StringBuilder sb = new StringBuilder();
        if (info.target_provider) {
            sb.append("provider: ").append(info.provider);
            sb.append(" account {name=" + info.account.name + ", user=" + info.userId + ", type=" + info.account.type + "}");
        } else if (info.target_service) {
            sb.append("service {package=").append(info.service.getPackageName()).append(" user=").append(info.userId).append(", class=").append(info.service.getClassName()).append("}");
        } else {
            Log.v(TAG, "Converting SyncOperaton to key, invalid target: " + info.toString());
            return "";
        }
        sb.append(" extras: ");
        extrasToStringBuilder(extras, sb);
        return sb.toString();
    }

    private static void extrasToStringBuilder(Bundle bundle, StringBuilder sb) {
        sb.append("[");
        for (String key : bundle.keySet()) {
            sb.append(key).append("=").append(bundle.get(key)).append(" ");
        }
        sb.append("]");
    }

    public String wakeLockName() {
        if (this.wakeLockName != null) {
            return this.wakeLockName;
        }
        if (this.target.target_provider) {
            String str = this.target.provider + "/" + this.target.account.type + "/" + this.target.account.name;
            this.wakeLockName = str;
            return str;
        }
        if (this.target.target_service) {
            String str2 = this.target.service.getPackageName() + "/" + this.target.service.getClassName();
            this.wakeLockName = str2;
            return str2;
        }
        Log.wtf(TAG, "Invalid target getting wakelock name for operation - " + this.key);
        return null;
    }

    public void updateEffectiveRunTime() {
        this.effectiveRunTime = ignoreBackoff() ? this.latestRunTime : Math.max(Math.max(this.latestRunTime, this.delayUntil), this.backoff);
    }

    @Override
    public int compareTo(Object o) {
        SyncOperation other = (SyncOperation) o;
        if (this.expedited != other.expedited) {
            return this.expedited ? -1 : 1;
        }
        long thisIntervalStart = Math.max(this.effectiveRunTime - this.flexTime, 0L);
        long otherIntervalStart = Math.max(other.effectiveRunTime - other.flexTime, 0L);
        if (thisIntervalStart >= otherIntervalStart) {
            return otherIntervalStart < thisIntervalStart ? 1 : 0;
        }
        return -1;
    }

    public Object[] toEventLog(int event) {
        Object[] logArray = new Object[4];
        logArray[1] = Integer.valueOf(event);
        logArray[2] = Integer.valueOf(this.syncSource);
        if (this.target.target_provider) {
            logArray[0] = this.target.provider;
            logArray[3] = Integer.valueOf(this.target.account.name.hashCode());
        } else if (this.target.target_service) {
            logArray[0] = this.target.service.getPackageName();
            logArray[3] = Integer.valueOf(this.target.service.hashCode());
        } else {
            Log.wtf(TAG, "sync op with invalid target: " + this.key);
        }
        return logArray;
    }
}
