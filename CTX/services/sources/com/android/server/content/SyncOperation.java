package com.android.server.content;

import android.accounts.Account;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.content.SyncStorageEngine;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.slice.SliceClientPermissions;
import java.util.Iterator;

public class SyncOperation {
    public static final int NO_JOB_ID = -1;
    public static final int REASON_ACCOUNTS_UPDATED = -2;
    public static final int REASON_BACKGROUND_DATA_SETTINGS_CHANGED = -1;
    public static final int REASON_IS_SYNCABLE = -5;
    public static final int REASON_MASTER_SYNC_AUTO = -7;
    private static String[] REASON_NAMES = {"DataSettingsChanged", "AccountsUpdated", "ServiceChanged", "Periodic", "IsSyncable", "AutoSync", "MasterSyncAuto", "UserStart"};
    public static final int REASON_PERIODIC = -4;
    public static final int REASON_SERVICE_CHANGED = -3;
    public static final int REASON_SYNC_AUTO = -6;
    public static final int REASON_USER_START = -8;
    public static final String TAG = "SyncManager";
    public final boolean allowParallelSyncs;
    public long expectedRuntime;
    public final Bundle extras;
    public final long flexMillis;
    public final boolean isPeriodic;
    public int jobId;
    public final String key;
    public final String owningPackage;
    public final int owningUid;
    public final long periodMillis;
    public final int reason;
    int retries;
    public final int sourcePeriodicId;
    public int syncExemptionFlag;
    public final int syncSource;
    public final SyncStorageEngine.EndPoint target;
    public String wakeLockName;

    public SyncOperation(Account account, int i, int i2, String str, int i3, int i4, String str2, Bundle bundle, boolean z, int i5) {
        this(new SyncStorageEngine.EndPoint(account, str2, i), i2, str, i3, i4, bundle, z, i5);
    }

    private SyncOperation(SyncStorageEngine.EndPoint endPoint, int i, String str, int i2, int i3, Bundle bundle, boolean z, int i4) {
        this(endPoint, i, str, i2, i3, bundle, z, false, -1, 0L, 0L, i4);
    }

    public SyncOperation(SyncOperation syncOperation, long j, long j2) {
        this(syncOperation.target, syncOperation.owningUid, syncOperation.owningPackage, syncOperation.reason, syncOperation.syncSource, new Bundle(syncOperation.extras), syncOperation.allowParallelSyncs, syncOperation.isPeriodic, syncOperation.sourcePeriodicId, j, j2, 0);
    }

    public SyncOperation(SyncStorageEngine.EndPoint endPoint, int i, String str, int i2, int i3, Bundle bundle, boolean z, boolean z2, int i4, long j, long j2, int i5) {
        this.target = endPoint;
        this.owningUid = i;
        this.owningPackage = str;
        this.reason = i2;
        this.syncSource = i3;
        this.extras = new Bundle(bundle);
        this.allowParallelSyncs = z;
        this.isPeriodic = z2;
        this.sourcePeriodicId = i4;
        this.periodMillis = j;
        this.flexMillis = j2;
        this.jobId = -1;
        this.key = toKey();
        this.syncExemptionFlag = i5;
    }

    public SyncOperation createOneTimeSyncOperation() {
        if (!this.isPeriodic) {
            return null;
        }
        return new SyncOperation(this.target, this.owningUid, this.owningPackage, this.reason, this.syncSource, new Bundle(this.extras), this.allowParallelSyncs, false, this.jobId, this.periodMillis, this.flexMillis, 0);
    }

    public SyncOperation(SyncOperation syncOperation) {
        this.target = syncOperation.target;
        this.owningUid = syncOperation.owningUid;
        this.owningPackage = syncOperation.owningPackage;
        this.reason = syncOperation.reason;
        this.syncSource = syncOperation.syncSource;
        this.allowParallelSyncs = syncOperation.allowParallelSyncs;
        this.extras = new Bundle(syncOperation.extras);
        this.wakeLockName = syncOperation.wakeLockName();
        this.isPeriodic = syncOperation.isPeriodic;
        this.sourcePeriodicId = syncOperation.sourcePeriodicId;
        this.periodMillis = syncOperation.periodMillis;
        this.flexMillis = syncOperation.flexMillis;
        this.key = syncOperation.key;
        this.syncExemptionFlag = syncOperation.syncExemptionFlag;
    }

    PersistableBundle toJobInfoExtras() {
        PersistableBundle persistableBundle = new PersistableBundle();
        PersistableBundle persistableBundle2 = new PersistableBundle();
        for (String str : this.extras.keySet()) {
            Object obj = this.extras.get(str);
            if (obj instanceof Account) {
                Account account = (Account) obj;
                PersistableBundle persistableBundle3 = new PersistableBundle();
                persistableBundle3.putString("accountName", account.name);
                persistableBundle3.putString("accountType", account.type);
                persistableBundle.putPersistableBundle("ACCOUNT:" + str, persistableBundle3);
            } else if (obj instanceof Long) {
                persistableBundle2.putLong(str, ((Long) obj).longValue());
            } else if (obj instanceof Integer) {
                persistableBundle2.putInt(str, ((Integer) obj).intValue());
            } else if (obj instanceof Boolean) {
                persistableBundle2.putBoolean(str, ((Boolean) obj).booleanValue());
            } else if (obj instanceof Float) {
                persistableBundle2.putDouble(str, ((Float) obj).floatValue());
            } else if (obj instanceof Double) {
                persistableBundle2.putDouble(str, ((Double) obj).doubleValue());
            } else if (obj instanceof String) {
                persistableBundle2.putString(str, (String) obj);
            } else if (obj == null) {
                persistableBundle2.putString(str, null);
            } else {
                Slog.e(TAG, "Unknown extra type.");
            }
        }
        persistableBundle.putPersistableBundle("syncExtras", persistableBundle2);
        persistableBundle.putBoolean("SyncManagerJob", true);
        persistableBundle.putString("provider", this.target.provider);
        persistableBundle.putString("accountName", this.target.account.name);
        persistableBundle.putString("accountType", this.target.account.type);
        persistableBundle.putInt("userId", this.target.userId);
        persistableBundle.putInt("owningUid", this.owningUid);
        persistableBundle.putString("owningPackage", this.owningPackage);
        persistableBundle.putInt(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, this.reason);
        persistableBundle.putInt("source", this.syncSource);
        persistableBundle.putBoolean("allowParallelSyncs", this.allowParallelSyncs);
        persistableBundle.putInt("jobId", this.jobId);
        persistableBundle.putBoolean("isPeriodic", this.isPeriodic);
        persistableBundle.putInt("sourcePeriodicId", this.sourcePeriodicId);
        persistableBundle.putLong("periodMillis", this.periodMillis);
        persistableBundle.putLong("flexMillis", this.flexMillis);
        persistableBundle.putLong("expectedRuntime", this.expectedRuntime);
        persistableBundle.putInt("retries", this.retries);
        persistableBundle.putInt("syncExemptionFlag", this.syncExemptionFlag);
        return persistableBundle;
    }

    static SyncOperation maybeCreateFromJobExtras(PersistableBundle persistableBundle) {
        Iterator<String> it;
        if (persistableBundle == null || !persistableBundle.getBoolean("SyncManagerJob", false)) {
            return null;
        }
        String string = persistableBundle.getString("accountName");
        String string2 = persistableBundle.getString("accountType");
        String string3 = persistableBundle.getString("provider");
        int i = persistableBundle.getInt("userId", Integer.MAX_VALUE);
        int i2 = persistableBundle.getInt("owningUid");
        String string4 = persistableBundle.getString("owningPackage");
        int i3 = persistableBundle.getInt(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, Integer.MAX_VALUE);
        int i4 = persistableBundle.getInt("source", Integer.MAX_VALUE);
        boolean z = persistableBundle.getBoolean("allowParallelSyncs", false);
        boolean z2 = persistableBundle.getBoolean("isPeriodic", false);
        int i5 = persistableBundle.getInt("sourcePeriodicId", -1);
        long j = persistableBundle.getLong("periodMillis");
        long j2 = persistableBundle.getLong("flexMillis");
        int i6 = persistableBundle.getInt("syncExemptionFlag", 0);
        Bundle bundle = new Bundle();
        PersistableBundle persistableBundle2 = persistableBundle.getPersistableBundle("syncExtras");
        if (persistableBundle2 != null) {
            bundle.putAll(persistableBundle2);
        }
        Iterator<String> it2 = persistableBundle.keySet().iterator();
        while (it2.hasNext()) {
            String next = it2.next();
            if (next != null && next.startsWith("ACCOUNT:")) {
                String strSubstring = next.substring(8);
                PersistableBundle persistableBundle3 = persistableBundle.getPersistableBundle(next);
                it = it2;
                bundle.putParcelable(strSubstring, new Account(persistableBundle3.getString("accountName"), persistableBundle3.getString("accountType")));
            } else {
                it = it2;
            }
            it2 = it;
        }
        SyncOperation syncOperation = new SyncOperation(new SyncStorageEngine.EndPoint(new Account(string, string2), string3, i), i2, string4, i3, i4, bundle, z, z2, i5, j, j2, i6);
        syncOperation.jobId = persistableBundle.getInt("jobId");
        syncOperation.expectedRuntime = persistableBundle.getLong("expectedRuntime");
        syncOperation.retries = persistableBundle.getInt("retries");
        return syncOperation;
    }

    boolean isConflict(SyncOperation syncOperation) {
        SyncStorageEngine.EndPoint endPoint = syncOperation.target;
        return this.target.account.type.equals(endPoint.account.type) && this.target.provider.equals(endPoint.provider) && this.target.userId == endPoint.userId && (!this.allowParallelSyncs || this.target.account.name.equals(endPoint.account.name));
    }

    boolean isReasonPeriodic() {
        return this.reason == -4;
    }

    boolean matchesPeriodicOperation(SyncOperation syncOperation) {
        return this.target.matchesSpec(syncOperation.target) && SyncManager.syncExtrasEquals(this.extras, syncOperation.extras, true) && this.periodMillis == syncOperation.periodMillis && this.flexMillis == syncOperation.flexMillis;
    }

    boolean isDerivedFromFailedPeriodicSync() {
        return this.sourcePeriodicId != -1;
    }

    int findPriority() {
        if (isInitialization()) {
            return 20;
        }
        if (isExpedited()) {
            return 10;
        }
        return 0;
    }

    private String toKey() {
        StringBuilder sb = new StringBuilder();
        sb.append("provider: ");
        sb.append(this.target.provider);
        sb.append(" account {name=" + this.target.account.name + ", user=" + this.target.userId + ", type=" + this.target.account.type + "}");
        sb.append(" isPeriodic: ");
        sb.append(this.isPeriodic);
        sb.append(" period: ");
        sb.append(this.periodMillis);
        sb.append(" flex: ");
        sb.append(this.flexMillis);
        sb.append(" extras: ");
        extrasToStringBuilder(this.extras, sb);
        return sb.toString();
    }

    public String toString() {
        return dump(null, true, null);
    }

    String dump(PackageManager packageManager, boolean z, SyncAdapterStateFetcher syncAdapterStateFetcher) {
        StringBuilder sb = new StringBuilder();
        sb.append("JobId=");
        sb.append(this.jobId);
        sb.append(" ");
        sb.append(this.target.account.name);
        sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
        sb.append(this.target.account.type);
        sb.append(" u");
        sb.append(this.target.userId);
        sb.append(" [");
        sb.append(this.target.provider);
        sb.append("] ");
        sb.append(SyncStorageEngine.SOURCES[this.syncSource]);
        if (this.expectedRuntime != 0) {
            sb.append(" ExpectedIn=");
            SyncManager.formatDurationHMS(sb, this.expectedRuntime - SystemClock.elapsedRealtime());
        }
        if (this.extras.getBoolean("expedited", false)) {
            sb.append(" EXPEDITED");
        }
        switch (this.syncExemptionFlag) {
            case 0:
                break;
            case 1:
                sb.append(" STANDBY-EXEMPTED");
                break;
            case 2:
                sb.append(" STANDBY-EXEMPTED(TOP)");
                break;
            default:
                sb.append(" ExemptionFlag=" + this.syncExemptionFlag);
                break;
        }
        sb.append(" Reason=");
        sb.append(reasonToString(packageManager, this.reason));
        if (this.isPeriodic) {
            sb.append(" (period=");
            SyncManager.formatDurationHMS(sb, this.periodMillis);
            sb.append(" flex=");
            SyncManager.formatDurationHMS(sb, this.flexMillis);
            sb.append(")");
        }
        if (this.retries > 0) {
            sb.append(" Retries=");
            sb.append(this.retries);
        }
        if (!z) {
            sb.append(" Owner={");
            UserHandle.formatUid(sb, this.owningUid);
            sb.append(" ");
            sb.append(this.owningPackage);
            if (syncAdapterStateFetcher != null) {
                sb.append(" [");
                sb.append(syncAdapterStateFetcher.getStandbyBucket(UserHandle.getUserId(this.owningUid), this.owningPackage));
                sb.append("]");
                if (syncAdapterStateFetcher.isAppActive(this.owningUid)) {
                    sb.append(" [ACTIVE]");
                }
            }
            sb.append("}");
            if (!this.extras.keySet().isEmpty()) {
                sb.append(" ");
                extrasToStringBuilder(this.extras, sb);
            }
        }
        return sb.toString();
    }

    static String reasonToString(PackageManager packageManager, int i) {
        if (i < 0) {
            int i2 = (-i) - 1;
            if (i2 >= REASON_NAMES.length) {
                return String.valueOf(i);
            }
            return REASON_NAMES[i2];
        }
        if (packageManager != null) {
            String[] packagesForUid = packageManager.getPackagesForUid(i);
            if (packagesForUid != null && packagesForUid.length == 1) {
                return packagesForUid[0];
            }
            String nameForUid = packageManager.getNameForUid(i);
            if (nameForUid != null) {
                return nameForUid;
            }
            return String.valueOf(i);
        }
        return String.valueOf(i);
    }

    boolean isInitialization() {
        return this.extras.getBoolean("initialize", false);
    }

    boolean isExpedited() {
        return this.extras.getBoolean("expedited", false);
    }

    boolean ignoreBackoff() {
        return this.extras.getBoolean("ignore_backoff", false);
    }

    boolean isNotAllowedOnMetered() {
        return this.extras.getBoolean("allow_metered", false);
    }

    boolean isManual() {
        return this.extras.getBoolean("force", false);
    }

    boolean isIgnoreSettings() {
        return this.extras.getBoolean("ignore_settings", false);
    }

    boolean isAppStandbyExempted() {
        return this.syncExemptionFlag != 0;
    }

    static void extrasToStringBuilder(Bundle bundle, StringBuilder sb) {
        if (bundle == null) {
            sb.append("null");
            return;
        }
        sb.append("[");
        for (String str : bundle.keySet()) {
            sb.append(str);
            sb.append("=");
            sb.append(bundle.get(str));
            sb.append(" ");
        }
        sb.append("]");
    }

    static String extrasToString(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        extrasToStringBuilder(bundle, sb);
        return sb.toString();
    }

    String wakeLockName() {
        if (this.wakeLockName != null) {
            return this.wakeLockName;
        }
        String str = this.target.provider + SliceClientPermissions.SliceAuthority.DELIMITER + this.target.account.type + SliceClientPermissions.SliceAuthority.DELIMITER + this.target.account.name;
        this.wakeLockName = str;
        return str;
    }

    public Object[] toEventLog(int i) {
        return new Object[]{this.target.provider, Integer.valueOf(i), Integer.valueOf(this.syncSource), Integer.valueOf(this.target.account.name.hashCode())};
    }
}
