package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManagerInternal;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IAppOpsActiveCallback;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.AppOpsService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AppOpsService extends IAppOpsService.Stub {
    private static final int CURRENT_VERSION = 1;
    static final boolean DEBUG = false;
    private static final int NO_VERSION = -1;
    static final String TAG = "AppOps";
    private static final int UID_ANY = -2;
    static final long WRITE_DELAY = 1800000;
    private final Constants mConstants;
    Context mContext;
    boolean mFastWriteScheduled;
    final AtomicFile mFile;
    final Handler mHandler;
    long mLastUptime;
    SparseIntArray mProfileOwners;
    boolean mWriteScheduled;
    private static final CtaManager CTA_MANAGER = CtaManagerFactory.getInstance().makeCtaManager();
    private static final int[] PROCESS_STATE_TO_UID_STATE = {0, 0, 1, 2, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5};
    static final String[] UID_STATE_NAMES = {"pers ", "top  ", "fgsvc", "fg   ", "bg   ", "cch  "};
    static final String[] UID_STATE_TIME_ATTRS = {"tp", "tt", "tfs", "tf", "tb", "tc"};
    static final String[] UID_STATE_REJECT_ATTRS = {"rp", "rt", "rfs", "rf", "rb", "rc"};
    private static final int[] OPS_RESTRICTED_ON_SUSPEND = {28, 27, 26};
    private final AppOpsManagerInternalImpl mAppOpsManagerInternal = new AppOpsManagerInternalImpl();
    final Runnable mWriteRunner = new Runnable() {
        @Override
        public void run() {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mWriteScheduled = false;
                AppOpsService.this.mFastWriteScheduled = false;
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voidArr) {
                        AppOpsService.this.writeState();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            }
        }
    };

    @VisibleForTesting
    final SparseArray<UidState> mUidStates = new SparseArray<>();
    private final ArrayMap<IBinder, ClientRestrictionState> mOpUserRestrictions = new ArrayMap<>();
    final SparseArray<ArraySet<ModeCallback>> mOpModeWatchers = new SparseArray<>();
    final ArrayMap<String, ArraySet<ModeCallback>> mPackageModeWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, ModeCallback> mModeWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, SparseArray<ActiveCallback>> mActiveWatchers = new ArrayMap<>();
    final SparseArray<SparseArray<Restriction>> mAudioRestrictions = new SparseArray<>();
    final ArrayMap<IBinder, ClientState> mClients = new ArrayMap<>();

    private final class Constants extends ContentObserver {
        private static final String KEY_BG_STATE_SETTLE_TIME = "bg_state_settle_time";
        private static final String KEY_FG_SERVICE_STATE_SETTLE_TIME = "fg_service_state_settle_time";
        private static final String KEY_TOP_STATE_SETTLE_TIME = "top_state_settle_time";
        public long BG_STATE_SETTLE_TIME;
        public long FG_SERVICE_STATE_SETTLE_TIME;
        public long TOP_STATE_SETTLE_TIME;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            this.mParser = new KeyValueListParser(',');
            updateConstants();
        }

        public void startMonitoring(ContentResolver contentResolver) {
            this.mResolver = contentResolver;
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("app_ops_constants"), false, this);
            updateConstants();
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            String string = this.mResolver != null ? Settings.Global.getString(this.mResolver, "app_ops_constants") : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            synchronized (AppOpsService.this) {
                try {
                    this.mParser.setString(string);
                } catch (IllegalArgumentException e) {
                    Slog.e(AppOpsService.TAG, "Bad app ops settings", e);
                }
                this.TOP_STATE_SETTLE_TIME = this.mParser.getDurationMillis(KEY_TOP_STATE_SETTLE_TIME, 30000L);
                this.FG_SERVICE_STATE_SETTLE_TIME = this.mParser.getDurationMillis(KEY_FG_SERVICE_STATE_SETTLE_TIME, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                this.BG_STATE_SETTLE_TIME = this.mParser.getDurationMillis(KEY_BG_STATE_SETTLE_TIME, 1000L);
            }
        }

        void dump(PrintWriter printWriter) {
            printWriter.println("  Settings:");
            printWriter.print("    ");
            printWriter.print(KEY_TOP_STATE_SETTLE_TIME);
            printWriter.print("=");
            TimeUtils.formatDuration(this.TOP_STATE_SETTLE_TIME, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_FG_SERVICE_STATE_SETTLE_TIME);
            printWriter.print("=");
            TimeUtils.formatDuration(this.FG_SERVICE_STATE_SETTLE_TIME, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_BG_STATE_SETTLE_TIME);
            printWriter.print("=");
            TimeUtils.formatDuration(this.BG_STATE_SETTLE_TIME, printWriter);
            printWriter.println();
        }
    }

    @VisibleForTesting
    static final class UidState {
        public SparseBooleanArray foregroundOps;
        public boolean hasForegroundWatchers;
        public SparseIntArray opModes;
        public long pendingStateCommitTime;
        public ArrayMap<String, Ops> pkgOps;
        public int startNesting;
        public final int uid;
        public int state = 5;
        public int pendingState = 5;

        public UidState(int i) {
            this.uid = i;
        }

        public void clear() {
            this.pkgOps = null;
            this.opModes = null;
        }

        public boolean isDefault() {
            return (this.pkgOps == null || this.pkgOps.isEmpty()) && (this.opModes == null || this.opModes.size() <= 0);
        }

        int evalMode(int i) {
            if (i == 4) {
                return this.state <= 2 ? 0 : 1;
            }
            return i;
        }

        private void evalForegroundWatchers(int i, SparseArray<ArraySet<ModeCallback>> sparseArray, SparseBooleanArray sparseBooleanArray) {
            boolean z = sparseBooleanArray.get(i, false);
            ArraySet<ModeCallback> arraySet = sparseArray.get(i);
            if (arraySet != null) {
                for (int size = arraySet.size() - 1; !z && size >= 0; size--) {
                    if ((arraySet.valueAt(size).mFlags & 1) != 0) {
                        this.hasForegroundWatchers = true;
                        z = true;
                    }
                }
            }
            sparseBooleanArray.put(i, z);
        }

        public void evalForegroundOps(SparseArray<ArraySet<ModeCallback>> sparseArray) {
            this.hasForegroundWatchers = false;
            SparseBooleanArray sparseBooleanArray = null;
            if (this.opModes != null) {
                for (int size = this.opModes.size() - 1; size >= 0; size--) {
                    if (this.opModes.valueAt(size) == 4) {
                        if (sparseBooleanArray == null) {
                            sparseBooleanArray = new SparseBooleanArray();
                        }
                        evalForegroundWatchers(this.opModes.keyAt(size), sparseArray, sparseBooleanArray);
                    }
                }
            }
            if (this.pkgOps != null) {
                for (int size2 = this.pkgOps.size() - 1; size2 >= 0; size2--) {
                    Ops opsValueAt = this.pkgOps.valueAt(size2);
                    for (int size3 = opsValueAt.size() - 1; size3 >= 0; size3--) {
                        if (opsValueAt.valueAt(size3).mode == 4) {
                            if (sparseBooleanArray == null) {
                                sparseBooleanArray = new SparseBooleanArray();
                            }
                            evalForegroundWatchers(opsValueAt.keyAt(size3), sparseArray, sparseBooleanArray);
                        }
                    }
                }
            }
            this.foregroundOps = sparseBooleanArray;
        }
    }

    static final class Ops extends SparseArray<Op> {
        final boolean isPrivileged;
        final String packageName;
        final UidState uidState;

        Ops(String str, UidState uidState, boolean z) {
            this.packageName = str;
            this.uidState = uidState;
            this.isPrivileged = z;
        }
    }

    static final class Op {
        int duration;
        int mode;
        final int op;
        final String packageName;
        String proxyPackageName;
        int startNesting;
        long startRealtime;
        final int uid;
        final UidState uidState;
        int proxyUid = -1;
        long[] time = new long[6];
        long[] rejectTime = new long[6];

        Op(UidState uidState, String str, int i) {
            this.uidState = uidState;
            this.uid = uidState.uid;
            this.packageName = str;
            this.op = i;
            this.mode = AppOpsManager.opToDefaultMode(this.op);
        }

        boolean hasAnyTime() {
            for (int i = 0; i < 6; i++) {
                if (this.time[i] != 0 || this.rejectTime[i] != 0) {
                    return true;
                }
            }
            return false;
        }

        int getMode() {
            return this.uidState.evalMode(this.mode);
        }
    }

    final class ModeCallback implements IBinder.DeathRecipient {
        final IAppOpsCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mFlags;
        final int mWatchingUid;

        ModeCallback(IAppOpsCallback iAppOpsCallback, int i, int i2, int i3, int i4) {
            this.mCallback = iAppOpsCallback;
            this.mWatchingUid = i;
            this.mFlags = i2;
            this.mCallingUid = i3;
            this.mCallingPid = i4;
            try {
                this.mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public boolean isWatchingUid(int i) {
            return i == -2 || this.mWatchingUid < 0 || this.mWatchingUid == i;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ModeCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(this.mFlags));
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void unlinkToDeath() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            AppOpsService.this.stopWatchingMode(this.mCallback);
        }
    }

    final class ActiveCallback implements IBinder.DeathRecipient {
        final IAppOpsActiveCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mWatchingUid;

        ActiveCallback(IAppOpsActiveCallback iAppOpsActiveCallback, int i, int i2, int i3) {
            this.mCallback = iAppOpsActiveCallback;
            this.mWatchingUid = i;
            this.mCallingUid = i2;
            this.mCallingPid = i3;
            try {
                this.mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActiveCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void destroy() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            AppOpsService.this.stopWatchingActive(this.mCallback);
        }
    }

    final class ClientState extends Binder implements IBinder.DeathRecipient {
        final IBinder mAppToken;
        final ArrayList<Op> mStartedOps = new ArrayList<>();
        final int mPid = Binder.getCallingPid();

        ClientState(IBinder iBinder) {
            this.mAppToken = iBinder;
            if (!(iBinder instanceof Binder)) {
                try {
                    this.mAppToken.linkToDeath(this, 0);
                } catch (RemoteException e) {
                }
            }
        }

        public String toString() {
            return "ClientState{mAppToken=" + this.mAppToken + ", pid=" + this.mPid + '}';
        }

        @Override
        public void binderDied() {
            synchronized (AppOpsService.this) {
                for (int size = this.mStartedOps.size() - 1; size >= 0; size--) {
                    AppOpsService.this.finishOperationLocked(this.mStartedOps.get(size), true);
                }
                AppOpsService.this.mClients.remove(this.mAppToken);
            }
        }
    }

    public AppOpsService(File file, Handler handler) {
        LockGuard.installLock(this, 0);
        this.mFile = new AtomicFile(file, "appops");
        this.mHandler = handler;
        this.mConstants = new Constants(this.mHandler);
        readState();
    }

    public void publish(Context context) {
        this.mContext = context;
        ServiceManager.addService("appops", asBinder());
        LocalServices.addService(AppOpsManagerInternal.class, this.mAppOpsManagerInternal);
    }

    public void systemReady() {
        int packageUid;
        this.mConstants.startMonitoring(this.mContext.getContentResolver());
        synchronized (this) {
            boolean z = false;
            for (int size = this.mUidStates.size() - 1; size >= 0; size--) {
                UidState uidStateValueAt = this.mUidStates.valueAt(size);
                if (ArrayUtils.isEmpty(getPackagesForUid(uidStateValueAt.uid))) {
                    uidStateValueAt.clear();
                    this.mUidStates.removeAt(size);
                    z = true;
                } else {
                    ArrayMap<String, Ops> arrayMap = uidStateValueAt.pkgOps;
                    if (arrayMap != null) {
                        Iterator<Ops> it = arrayMap.values().iterator();
                        while (it.hasNext()) {
                            Ops next = it.next();
                            try {
                                packageUid = AppGlobals.getPackageManager().getPackageUid(next.packageName, 8192, UserHandle.getUserId(next.uidState.uid));
                            } catch (RemoteException e) {
                                packageUid = -1;
                            }
                            if (packageUid != next.uidState.uid) {
                                Slog.i(TAG, "Pruning old package " + next.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + next.uidState + ": new uid=" + packageUid);
                                it.remove();
                                z = true;
                            }
                        }
                        if (uidStateValueAt.isDefault()) {
                            this.mUidStates.removeAt(size);
                        }
                    }
                }
            }
            if (z) {
                scheduleFastWriteLocked();
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        intentFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                for (int i : AppOpsService.OPS_RESTRICTED_ON_SUSPEND) {
                    synchronized (AppOpsService.this) {
                        ArraySet<ModeCallback> arraySet = AppOpsService.this.mOpModeWatchers.get(i);
                        if (arraySet != null) {
                            ArraySet arraySet2 = new ArraySet((ArraySet) arraySet);
                            for (String str : stringArrayExtra) {
                                AppOpsService.this.notifyOpChanged((ArraySet<ModeCallback>) arraySet2, i, -1, str);
                            }
                        }
                    }
                }
            }
        }, intentFilter);
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setExternalSourcesPolicy(new PackageManagerInternal.ExternalSourcesPolicy() {
            public int getPackageTrustedToInstallApps(String str, int i) {
                int iCheckOperation = AppOpsService.this.checkOperation(66, i, str);
                if (iCheckOperation != 0) {
                    return iCheckOperation != 2 ? 2 : 1;
                }
                return 0;
            }
        });
        ((StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class)).addExternalStoragePolicy(new StorageManagerInternal.ExternalStorageMountPolicy() {
            public int getMountMode(int i, String str) {
                if (Process.isIsolated(i) || AppOpsService.this.noteOperation(59, i, str) != 0) {
                    return 0;
                }
                if (AppOpsService.this.noteOperation(60, i, str) != 0) {
                    return 2;
                }
                return 3;
            }

            public boolean hasExternalStorage(int i, String str) {
                int mountMode = getMountMode(i, str);
                return mountMode == 2 || mountMode == 3;
            }
        });
    }

    public void packageRemoved(int i, String str) {
        synchronized (this) {
            UidState uidState = this.mUidStates.get(i);
            if (uidState == null) {
                return;
            }
            Ops opsRemove = null;
            if (uidState.pkgOps != null) {
                opsRemove = uidState.pkgOps.remove(str);
            }
            if (opsRemove != null && uidState.pkgOps.isEmpty() && getPackagesForUid(i).length <= 0) {
                this.mUidStates.remove(i);
            }
            int size = this.mClients.size();
            for (int i2 = 0; i2 < size; i2++) {
                ClientState clientStateValueAt = this.mClients.valueAt(i2);
                if (clientStateValueAt.mStartedOps != null) {
                    for (int size2 = clientStateValueAt.mStartedOps.size() - 1; size2 >= 0; size2--) {
                        Op op = clientStateValueAt.mStartedOps.get(size2);
                        if (i == op.uid && str.equals(op.packageName)) {
                            finishOperationLocked(op, true);
                            clientStateValueAt.mStartedOps.remove(size2);
                            if (op.startNesting <= 0) {
                                scheduleOpActiveChangedIfNeededLocked(op.op, i, str, false);
                            }
                        }
                    }
                }
            }
            if (opsRemove != null) {
                scheduleFastWriteLocked();
                int size3 = opsRemove.size();
                for (int i3 = 0; i3 < size3; i3++) {
                    Op opValueAt = opsRemove.valueAt(i3);
                    if (opValueAt.duration == -1) {
                        scheduleOpActiveChangedIfNeededLocked(opValueAt.op, opValueAt.uid, opValueAt.packageName, false);
                    }
                }
            }
        }
    }

    public void uidRemoved(int i) {
        synchronized (this) {
            if (this.mUidStates.indexOfKey(i) >= 0) {
                this.mUidStates.remove(i);
                scheduleFastWriteLocked();
            }
        }
    }

    public void updateUidProcState(int i, int i2) {
        long j;
        synchronized (this) {
            UidState uidStateLocked = getUidStateLocked(i, true);
            int i3 = PROCESS_STATE_TO_UID_STATE[i2];
            if (uidStateLocked != null && uidStateLocked.pendingState != i3) {
                int i4 = uidStateLocked.pendingState;
                uidStateLocked.pendingState = i3;
                if (i3 < uidStateLocked.state || i3 <= 2) {
                    commitUidPendingStateLocked(uidStateLocked);
                } else if (uidStateLocked.pendingStateCommitTime == 0) {
                    if (uidStateLocked.state <= 1) {
                        j = this.mConstants.TOP_STATE_SETTLE_TIME;
                    } else if (uidStateLocked.state <= 2) {
                        j = this.mConstants.FG_SERVICE_STATE_SETTLE_TIME;
                    } else {
                        j = this.mConstants.BG_STATE_SETTLE_TIME;
                    }
                    uidStateLocked.pendingStateCommitTime = SystemClock.uptimeMillis() + j;
                }
                if (uidStateLocked.startNesting != 0) {
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    for (int size = uidStateLocked.pkgOps.size() - 1; size >= 0; size--) {
                        Ops opsValueAt = uidStateLocked.pkgOps.valueAt(size);
                        for (int size2 = opsValueAt.size() - 1; size2 >= 0; size2--) {
                            Op opValueAt = opsValueAt.valueAt(size2);
                            if (opValueAt.startNesting > 0) {
                                opValueAt.time[i4] = jCurrentTimeMillis;
                                opValueAt.time[i3] = jCurrentTimeMillis;
                            }
                        }
                    }
                }
            }
        }
    }

    public void shutdown() {
        boolean z;
        Slog.w(TAG, "Writing app ops before shutdown...");
        synchronized (this) {
            z = false;
            if (this.mWriteScheduled) {
                this.mWriteScheduled = false;
                z = true;
            }
        }
        if (z) {
            writeState();
        }
    }

    private ArrayList<AppOpsManager.OpEntry> collectOps(Ops ops, int[] iArr) {
        long j;
        long j2;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        int i = -1;
        if (iArr == null) {
            ArrayList<AppOpsManager.OpEntry> arrayList = new ArrayList<>();
            for (int i2 = 0; i2 < ops.size(); i2++) {
                Op opValueAt = ops.valueAt(i2);
                boolean z = opValueAt.duration == -1;
                if (z) {
                    j2 = jElapsedRealtime - opValueAt.startRealtime;
                } else {
                    j2 = opValueAt.duration;
                }
                arrayList.add(new AppOpsManager.OpEntry(opValueAt.op, opValueAt.mode, opValueAt.time, opValueAt.rejectTime, (int) j2, z, opValueAt.proxyUid, opValueAt.proxyPackageName));
            }
            return arrayList;
        }
        ArrayList<AppOpsManager.OpEntry> arrayList2 = null;
        int i3 = 0;
        while (i3 < iArr.length) {
            Op op = ops.get(iArr[i3]);
            if (op != null) {
                if (arrayList2 == null) {
                    arrayList2 = new ArrayList<>();
                }
                boolean z2 = op.duration == i;
                if (z2) {
                    j = jElapsedRealtime - op.startRealtime;
                } else {
                    j = op.duration;
                }
                arrayList2.add(new AppOpsManager.OpEntry(op.op, op.mode, op.time, op.rejectTime, (int) j, z2, op.proxyUid, op.proxyPackageName));
            }
            i3++;
            i = -1;
        }
        return arrayList2;
    }

    private ArrayList<AppOpsManager.OpEntry> collectOps(SparseIntArray sparseIntArray, int[] iArr) {
        int i = 0;
        if (iArr == null) {
            ArrayList<AppOpsManager.OpEntry> arrayList = new ArrayList<>();
            while (i < sparseIntArray.size()) {
                arrayList.add(new AppOpsManager.OpEntry(sparseIntArray.keyAt(i), sparseIntArray.valueAt(i), 0L, 0L, 0, -1, (String) null));
                i++;
            }
            return arrayList;
        }
        ArrayList<AppOpsManager.OpEntry> arrayList2 = null;
        while (i < iArr.length) {
            int iIndexOfKey = sparseIntArray.indexOfKey(iArr[i]);
            if (iIndexOfKey >= 0) {
                if (arrayList2 == null) {
                    arrayList2 = new ArrayList<>();
                }
                arrayList2.add(new AppOpsManager.OpEntry(sparseIntArray.keyAt(iIndexOfKey), sparseIntArray.valueAt(iIndexOfKey), 0L, 0L, 0, -1, (String) null));
            }
            i++;
        }
        return arrayList2;
    }

    public List<AppOpsManager.PackageOps> getPackagesForOps(int[] iArr) {
        ArrayList arrayList = null;
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        synchronized (this) {
            int size = this.mUidStates.size();
            for (int i = 0; i < size; i++) {
                UidState uidStateValueAt = this.mUidStates.valueAt(i);
                if (uidStateValueAt.pkgOps != null && !uidStateValueAt.pkgOps.isEmpty()) {
                    ArrayMap<String, Ops> arrayMap = uidStateValueAt.pkgOps;
                    int size2 = arrayMap.size();
                    ArrayList arrayList2 = arrayList;
                    for (int i2 = 0; i2 < size2; i2++) {
                        Ops opsValueAt = arrayMap.valueAt(i2);
                        ArrayList<AppOpsManager.OpEntry> arrayListCollectOps = collectOps(opsValueAt, iArr);
                        if (arrayListCollectOps != null) {
                            if (arrayList2 == null) {
                                arrayList2 = new ArrayList();
                            }
                            arrayList2.add(new AppOpsManager.PackageOps(opsValueAt.packageName, opsValueAt.uidState.uid, arrayListCollectOps));
                        }
                    }
                    arrayList = arrayList2;
                }
            }
        }
        return arrayList;
    }

    public List<AppOpsManager.PackageOps> getOpsForPackage(int i, String str, int[] iArr) {
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        String strResolvePackageName = resolvePackageName(i, str);
        if (strResolvePackageName == null) {
            return Collections.emptyList();
        }
        synchronized (this) {
            Ops opsRawLocked = getOpsRawLocked(i, strResolvePackageName, false, false);
            if (opsRawLocked == null) {
                return null;
            }
            ArrayList<AppOpsManager.OpEntry> arrayListCollectOps = collectOps(opsRawLocked, iArr);
            if (arrayListCollectOps == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            arrayList.add(new AppOpsManager.PackageOps(opsRawLocked.packageName, opsRawLocked.uidState.uid, arrayListCollectOps));
            return arrayList;
        }
    }

    public List<AppOpsManager.PackageOps> getUidOps(int i, int[] iArr) {
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        synchronized (this) {
            UidState uidStateLocked = getUidStateLocked(i, false);
            if (uidStateLocked == null) {
                return null;
            }
            ArrayList<AppOpsManager.OpEntry> arrayListCollectOps = collectOps(uidStateLocked.opModes, iArr);
            if (arrayListCollectOps == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            arrayList.add(new AppOpsManager.PackageOps((String) null, uidStateLocked.uid, arrayListCollectOps));
            return arrayList;
        }
    }

    private void pruneOp(Op op, int i, String str) {
        Ops opsRawLocked;
        UidState uidState;
        ArrayMap<String, Ops> arrayMap;
        if (!op.hasAnyTime() && (opsRawLocked = getOpsRawLocked(i, str, false, false)) != null) {
            opsRawLocked.remove(op.op);
            if (opsRawLocked.size() <= 0 && (arrayMap = (uidState = opsRawLocked.uidState).pkgOps) != null) {
                arrayMap.remove(opsRawLocked.packageName);
                if (arrayMap.isEmpty()) {
                    uidState.pkgOps = null;
                }
                if (uidState.isDefault()) {
                    this.mUidStates.remove(i);
                }
            }
        }
    }

    void enforceManageAppOpsModes(int i, int i2, int i3) {
        if (i == Process.myPid()) {
            return;
        }
        int userId = UserHandle.getUserId(i2);
        synchronized (this) {
            if (this.mProfileOwners == null || this.mProfileOwners.get(userId, -1) != i2 || i3 < 0 || userId != UserHandle.getUserId(i3)) {
                this.mContext.enforcePermission("android.permission.MANAGE_APP_OPS_MODES", Binder.getCallingPid(), Binder.getCallingUid(), null);
            }
        }
    }

    public void setUidMode(int i, int i2, int i3) {
        ArrayMap arrayMap;
        ArrayMap arrayMap2;
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), i2);
        verifyIncomingOp(i);
        int iOpToSwitch = AppOpsManager.opToSwitch(i);
        synchronized (this) {
            int iOpToDefaultMode = AppOpsManager.opToDefaultMode(iOpToSwitch);
            int i4 = 0;
            UidState uidStateLocked = getUidStateLocked(i2, false);
            if (uidStateLocked == null) {
                if (i3 == iOpToDefaultMode) {
                    return;
                }
                UidState uidState = new UidState(i2);
                uidState.opModes = new SparseIntArray();
                uidState.opModes.put(iOpToSwitch, i3);
                this.mUidStates.put(i2, uidState);
                scheduleWriteLocked();
            } else if (uidStateLocked.opModes != null) {
                if (uidStateLocked.opModes.get(iOpToSwitch) == i3) {
                    return;
                }
                if (i3 != iOpToDefaultMode) {
                    uidStateLocked.opModes.put(iOpToSwitch, i3);
                } else {
                    uidStateLocked.opModes.delete(iOpToSwitch);
                    if (uidStateLocked.opModes.size() <= 0) {
                        uidStateLocked.opModes = null;
                    }
                }
                scheduleWriteLocked();
            } else if (i3 != iOpToDefaultMode) {
                uidStateLocked.opModes = new SparseIntArray();
                uidStateLocked.opModes.put(iOpToSwitch, i3);
                scheduleWriteLocked();
            }
            String[] packagesForUid = getPackagesForUid(i2);
            synchronized (this) {
                ArraySet<ModeCallback> arraySet = this.mOpModeWatchers.get(iOpToSwitch);
                if (arraySet != null) {
                    int size = arraySet.size();
                    arrayMap = null;
                    for (int i5 = 0; i5 < size; i5++) {
                        ModeCallback modeCallbackValueAt = arraySet.valueAt(i5);
                        ArraySet arraySet2 = new ArraySet();
                        Collections.addAll(arraySet2, packagesForUid);
                        if (arrayMap == null) {
                            arrayMap = new ArrayMap();
                        }
                        arrayMap.put(modeCallbackValueAt, arraySet2);
                    }
                } else {
                    arrayMap = null;
                }
                arrayMap2 = arrayMap;
                for (String str : packagesForUid) {
                    ArraySet<ModeCallback> arraySet3 = this.mPackageModeWatchers.get(str);
                    if (arraySet3 != null) {
                        if (arrayMap2 == null) {
                            arrayMap2 = new ArrayMap();
                        }
                        int size2 = arraySet3.size();
                        for (int i6 = 0; i6 < size2; i6++) {
                            ModeCallback modeCallbackValueAt2 = arraySet3.valueAt(i6);
                            ArraySet arraySet4 = (ArraySet) arrayMap2.get(modeCallbackValueAt2);
                            if (arraySet4 == null) {
                                arraySet4 = new ArraySet();
                                arrayMap2.put(modeCallbackValueAt2, arraySet4);
                            }
                            arraySet4.add(str);
                        }
                    }
                }
            }
            if (arrayMap2 == null) {
                return;
            }
            int i7 = 0;
            while (i7 < arrayMap2.size()) {
                ModeCallback modeCallback = (ModeCallback) arrayMap2.keyAt(i7);
                ArraySet arraySet5 = (ArraySet) arrayMap2.valueAt(i7);
                if (arraySet5 == null) {
                    this.mHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE, this, modeCallback, Integer.valueOf(iOpToSwitch), Integer.valueOf(i2), (String) null));
                } else {
                    int size3 = arraySet5.size();
                    for (int i8 = i4; i8 < size3; i8++) {
                        this.mHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE, this, modeCallback, Integer.valueOf(iOpToSwitch), Integer.valueOf(i2), (String) arraySet5.valueAt(i8)));
                    }
                }
                i7++;
                i4 = 0;
            }
        }
    }

    public void setMode(int i, int i2, String str, int i3) {
        ArraySet arraySet;
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), i2);
        verifyIncomingOp(i);
        int iOpToSwitch = AppOpsManager.opToSwitch(i);
        synchronized (this) {
            UidState uidStateLocked = getUidStateLocked(i2, false);
            Op opLocked = getOpLocked(iOpToSwitch, i2, str, true);
            arraySet = null;
            if (opLocked != null && opLocked.mode != i3) {
                opLocked.mode = i3;
                if (uidStateLocked != null) {
                    uidStateLocked.evalForegroundOps(this.mOpModeWatchers);
                }
                ArraySet<ModeCallback> arraySet2 = this.mOpModeWatchers.get(iOpToSwitch);
                if (arraySet2 != null) {
                    arraySet = new ArraySet();
                    arraySet.addAll((ArraySet) arraySet2);
                }
                ArraySet<ModeCallback> arraySet3 = this.mPackageModeWatchers.get(str);
                if (arraySet3 != null) {
                    if (arraySet == null) {
                        arraySet = new ArraySet();
                    }
                    arraySet.addAll((ArraySet) arraySet3);
                }
                if (i3 == AppOpsManager.opToDefaultMode(opLocked.op)) {
                    pruneOp(opLocked, i2, str);
                }
                scheduleFastWriteLocked();
            }
        }
        if (arraySet != null) {
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() {
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((AppOpsService) obj).notifyOpChanged((ArraySet<AppOpsService.ModeCallback>) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (String) obj5);
                }
            }, this, arraySet, Integer.valueOf(iOpToSwitch), Integer.valueOf(i2), str));
        }
    }

    private void notifyOpChanged(ArraySet<ModeCallback> arraySet, int i, int i2, String str) {
        for (int i3 = 0; i3 < arraySet.size(); i3++) {
            notifyOpChanged(arraySet.valueAt(i3), i, i2, str);
        }
    }

    private void notifyOpChanged(ModeCallback modeCallback, int i, int i2, String str) {
        if (i2 != -2 && modeCallback.mWatchingUid >= 0 && modeCallback.mWatchingUid != i2) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            modeCallback.mCallback.opChanged(i, i2, str);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private static HashMap<ModeCallback, ArrayList<ChangeRec>> addCallbacks(HashMap<ModeCallback, ArrayList<ChangeRec>> map, int i, int i2, String str, ArraySet<ModeCallback> arraySet) {
        if (arraySet == null) {
            return map;
        }
        if (map == null) {
            map = new HashMap<>();
        }
        int size = arraySet.size();
        boolean z = false;
        for (int i3 = 0; i3 < size; i3++) {
            ModeCallback modeCallbackValueAt = arraySet.valueAt(i3);
            ArrayList<ChangeRec> arrayList = map.get(modeCallbackValueAt);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                map.put(modeCallbackValueAt, arrayList);
            } else {
                int size2 = arrayList.size();
                int i4 = 0;
                while (true) {
                    if (i4 >= size2) {
                        break;
                    }
                    ChangeRec changeRec = arrayList.get(i4);
                    if (changeRec.op != i || !changeRec.pkg.equals(str)) {
                        i4++;
                    } else {
                        z = true;
                        break;
                    }
                }
            }
            if (!z) {
                arrayList.add(new ChangeRec(i, i2, str));
            }
        }
        return map;
    }

    static final class ChangeRec {
        final int op;
        final String pkg;
        final int uid;

        ChangeRec(int i, int i2, String str) {
            this.op = i;
            this.uid = i2;
            this.pkg = str;
        }
    }

    public void resetAllModes(int i, String str) {
        int packageUid;
        HashMap<ModeCallback, ArrayList<ChangeRec>> mapAddCallbacks;
        int i2;
        int i3;
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(callingPid, callingUid, i, true, true, "resetAllModes", null);
        int i4 = -1;
        if (str != null) {
            try {
                packageUid = AppGlobals.getPackageManager().getPackageUid(str, 8192, iHandleIncomingUser);
            } catch (RemoteException e) {
                packageUid = -1;
            }
        } else {
            packageUid = -1;
        }
        enforceManageAppOpsModes(callingPid, callingUid, packageUid);
        synchronized (this) {
            int i5 = 1;
            int size = this.mUidStates.size() - 1;
            SparseIntArray sparseIntArray = null;
            mapAddCallbacks = null;
            boolean z = false;
            while (size >= 0) {
                UidState uidStateValueAt = this.mUidStates.valueAt(size);
                SparseIntArray sparseIntArray2 = uidStateValueAt.opModes;
                if (sparseIntArray2 != null && (uidStateValueAt.uid == packageUid || packageUid == i4)) {
                    int size2 = sparseIntArray2.size() - i5;
                    while (size2 >= 0) {
                        int iKeyAt = sparseIntArray2.keyAt(size2);
                        if (AppOpsManager.opAllowsReset(iKeyAt)) {
                            sparseIntArray2.removeAt(size2);
                            if (sparseIntArray2.size() <= 0) {
                                uidStateValueAt.opModes = sparseIntArray;
                            }
                            String[] packagesForUid = getPackagesForUid(uidStateValueAt.uid);
                            int length = packagesForUid.length;
                            HashMap<ModeCallback, ArrayList<ChangeRec>> mapAddCallbacks2 = mapAddCallbacks;
                            int i6 = 0;
                            while (i6 < length) {
                                String str2 = packagesForUid[i6];
                                mapAddCallbacks2 = addCallbacks(addCallbacks(mapAddCallbacks2, iKeyAt, uidStateValueAt.uid, str2, this.mOpModeWatchers.get(iKeyAt)), iKeyAt, uidStateValueAt.uid, str2, this.mPackageModeWatchers.get(str2));
                                i6++;
                                packageUid = packageUid;
                            }
                            i3 = packageUid;
                            mapAddCallbacks = mapAddCallbacks2;
                        } else {
                            i3 = packageUid;
                        }
                        size2--;
                        packageUid = i3;
                        sparseIntArray = null;
                    }
                }
                int i7 = packageUid;
                if (uidStateValueAt.pkgOps != null && (iHandleIncomingUser == -1 || iHandleIncomingUser == UserHandle.getUserId(uidStateValueAt.uid))) {
                    Iterator<Map.Entry<String, Ops>> it = uidStateValueAt.pkgOps.entrySet().iterator();
                    boolean z2 = false;
                    while (it.hasNext()) {
                        Map.Entry<String, Ops> next = it.next();
                        String key = next.getKey();
                        if (str == null || str.equals(key)) {
                            Ops value = next.getValue();
                            for (int size3 = value.size() - 1; size3 >= 0; size3--) {
                                Op opValueAt = value.valueAt(size3);
                                if (AppOpsManager.opAllowsReset(opValueAt.op) && opValueAt.mode != AppOpsManager.opToDefaultMode(opValueAt.op)) {
                                    opValueAt.mode = AppOpsManager.opToDefaultMode(opValueAt.op);
                                    mapAddCallbacks = addCallbacks(addCallbacks(mapAddCallbacks, opValueAt.op, opValueAt.uid, key, this.mOpModeWatchers.get(opValueAt.op)), opValueAt.op, opValueAt.uid, key, this.mPackageModeWatchers.get(key));
                                    if (!opValueAt.hasAnyTime()) {
                                        value.removeAt(size3);
                                    }
                                    z2 = true;
                                    z = true;
                                }
                            }
                            if (value.size() == 0) {
                                it.remove();
                            }
                        }
                    }
                    i2 = 1;
                    if (uidStateValueAt.isDefault()) {
                        this.mUidStates.remove(uidStateValueAt.uid);
                    }
                    if (z2) {
                        uidStateValueAt.evalForegroundOps(this.mOpModeWatchers);
                    }
                } else {
                    i2 = 1;
                }
                size--;
                i5 = i2;
                packageUid = i7;
                i4 = -1;
                sparseIntArray = null;
            }
            if (z) {
                scheduleFastWriteLocked();
            }
        }
        if (mapAddCallbacks != null) {
            for (Map.Entry<ModeCallback, ArrayList<ChangeRec>> entry : mapAddCallbacks.entrySet()) {
                ModeCallback key2 = entry.getKey();
                ArrayList<ChangeRec> value2 = entry.getValue();
                for (int i8 = 0; i8 < value2.size(); i8++) {
                    ChangeRec changeRec = value2.get(i8);
                    this.mHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE, this, key2, Integer.valueOf(changeRec.op), Integer.valueOf(changeRec.uid), changeRec.pkg));
                }
            }
        }
    }

    private void evalAllForegroundOpsLocked() {
        for (int size = this.mUidStates.size() - 1; size >= 0; size--) {
            UidState uidStateValueAt = this.mUidStates.valueAt(size);
            if (uidStateValueAt.foregroundOps != null) {
                uidStateValueAt.evalForegroundOps(this.mOpModeWatchers);
            }
        }
    }

    public void startWatchingMode(int i, String str, IAppOpsCallback iAppOpsCallback) {
        startWatchingModeWithFlags(i, str, 0, iAppOpsCallback);
    }

    public void startWatchingModeWithFlags(int i, String str, int i2, IAppOpsCallback iAppOpsCallback) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        Preconditions.checkArgumentInRange(i, -1, 77, "Invalid op code: " + i);
        if (iAppOpsCallback == null) {
            return;
        }
        synchronized (this) {
            if (i != -1) {
                try {
                    i = AppOpsManager.opToSwitch(i);
                } catch (Throwable th) {
                    throw th;
                }
            }
            ModeCallback modeCallback = this.mModeWatchers.get(iAppOpsCallback.asBinder());
            if (modeCallback == null) {
                ModeCallback modeCallback2 = new ModeCallback(iAppOpsCallback, -1, i2, callingUid, callingPid);
                this.mModeWatchers.put(iAppOpsCallback.asBinder(), modeCallback2);
                modeCallback = modeCallback2;
            }
            if (i != -1) {
                ArraySet<ModeCallback> arraySet = this.mOpModeWatchers.get(i);
                if (arraySet == null) {
                    arraySet = new ArraySet<>();
                    this.mOpModeWatchers.put(i, arraySet);
                }
                arraySet.add(modeCallback);
            }
            if (str != null) {
                ArraySet<ModeCallback> arraySet2 = this.mPackageModeWatchers.get(str);
                if (arraySet2 == null) {
                    arraySet2 = new ArraySet<>();
                    this.mPackageModeWatchers.put(str, arraySet2);
                }
                arraySet2.add(modeCallback);
            }
            evalAllForegroundOpsLocked();
        }
    }

    public void stopWatchingMode(IAppOpsCallback iAppOpsCallback) {
        if (iAppOpsCallback == null) {
            return;
        }
        synchronized (this) {
            ModeCallback modeCallbackRemove = this.mModeWatchers.remove(iAppOpsCallback.asBinder());
            if (modeCallbackRemove != null) {
                modeCallbackRemove.unlinkToDeath();
                for (int size = this.mOpModeWatchers.size() - 1; size >= 0; size--) {
                    ArraySet<ModeCallback> arraySetValueAt = this.mOpModeWatchers.valueAt(size);
                    arraySetValueAt.remove(modeCallbackRemove);
                    if (arraySetValueAt.size() <= 0) {
                        this.mOpModeWatchers.removeAt(size);
                    }
                }
                for (int size2 = this.mPackageModeWatchers.size() - 1; size2 >= 0; size2--) {
                    ArraySet<ModeCallback> arraySetValueAt2 = this.mPackageModeWatchers.valueAt(size2);
                    arraySetValueAt2.remove(modeCallbackRemove);
                    if (arraySetValueAt2.size() <= 0) {
                        this.mPackageModeWatchers.removeAt(size2);
                    }
                }
            }
            evalAllForegroundOpsLocked();
        }
    }

    public IBinder getToken(IBinder iBinder) {
        ClientState clientState;
        synchronized (this) {
            clientState = this.mClients.get(iBinder);
            if (clientState == null) {
                clientState = new ClientState(iBinder);
                this.mClients.put(iBinder, clientState);
            }
        }
        return clientState;
    }

    public int checkOperation(int i, int i2, String str) {
        verifyIncomingUid(i2);
        verifyIncomingOp(i);
        String strResolvePackageName = resolvePackageName(i2, str);
        if (strResolvePackageName == null || isOpRestrictedDueToSuspend(i, str, i2)) {
            return 1;
        }
        synchronized (this) {
            if (isOpRestrictedLocked(i2, i, strResolvePackageName)) {
                return 1;
            }
            int iOpToSwitch = AppOpsManager.opToSwitch(i);
            UidState uidStateLocked = getUidStateLocked(i2, false);
            if (uidStateLocked != null && uidStateLocked.opModes != null && uidStateLocked.opModes.indexOfKey(iOpToSwitch) >= 0) {
                return uidStateLocked.opModes.get(iOpToSwitch);
            }
            Op opLocked = getOpLocked(iOpToSwitch, i2, strResolvePackageName, false);
            if (opLocked == null) {
                return AppOpsManager.opToDefaultMode(iOpToSwitch);
            }
            return opLocked.mode;
        }
    }

    public int checkAudioOperation(int i, int i2, int i3, String str) {
        boolean zIsPackageSuspendedForUser;
        try {
            zIsPackageSuspendedForUser = isPackageSuspendedForUser(str, i3);
        } catch (IllegalArgumentException e) {
            zIsPackageSuspendedForUser = false;
        }
        if (zIsPackageSuspendedForUser) {
            Slog.i(TAG, "Audio disabled for suspended package=" + str + " for uid=" + i3);
            return 1;
        }
        synchronized (this) {
            int iCheckRestrictionLocked = checkRestrictionLocked(i, i2, i3, str);
            return iCheckRestrictionLocked != 0 ? iCheckRestrictionLocked : checkOperation(i, i3, str);
        }
    }

    private boolean isOpRestrictedDueToSuspend(int i, String str, int i2) {
        if (!ArrayUtils.contains(OPS_RESTRICTED_ON_SUSPEND, i)) {
            return false;
        }
        return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).isPackageSuspended(str, UserHandle.getUserId(i2));
    }

    private boolean isPackageSuspendedForUser(String str, int i) {
        try {
            return AppGlobals.getPackageManager().isPackageSuspendedForUser(str, UserHandle.getUserId(i));
        } catch (RemoteException e) {
            throw new SecurityException("Could not talk to package manager service");
        }
    }

    private int checkRestrictionLocked(int i, int i2, int i3, String str) {
        Restriction restriction;
        SparseArray<Restriction> sparseArray = this.mAudioRestrictions.get(i);
        if (sparseArray != null && (restriction = sparseArray.get(i2)) != null && !restriction.exceptionPackages.contains(str)) {
            return restriction.mode;
        }
        return 0;
    }

    public void setAudioRestriction(int i, int i2, int i3, int i4, String[] strArr) {
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), i3);
        verifyIncomingUid(i3);
        verifyIncomingOp(i);
        synchronized (this) {
            SparseArray<Restriction> sparseArray = this.mAudioRestrictions.get(i);
            if (sparseArray == null) {
                sparseArray = new SparseArray<>();
                this.mAudioRestrictions.put(i, sparseArray);
            }
            sparseArray.remove(i2);
            if (i4 != 0) {
                Restriction restriction = new Restriction();
                restriction.mode = i4;
                if (strArr != null) {
                    restriction.exceptionPackages = new ArraySet<>(strArr.length);
                    for (String str : strArr) {
                        if (str != null) {
                            restriction.exceptionPackages.add(str.trim());
                        }
                    }
                }
                sparseArray.put(i2, restriction);
            }
        }
        this.mHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo.INSTANCE, this, Integer.valueOf(i), -2));
    }

    public int checkPackage(int i, String str) {
        Preconditions.checkNotNull(str);
        synchronized (this) {
            if (getOpsRawLocked(i, str, true, true) != null) {
                return 0;
            }
            return 2;
        }
    }

    public int noteProxyOperation(int i, String str, int i2, String str2) {
        verifyIncomingOp(i);
        int callingUid = Binder.getCallingUid();
        String strResolvePackageName = resolvePackageName(callingUid, str);
        if (strResolvePackageName == null) {
            return 1;
        }
        int iNoteOperationUnchecked = noteOperationUnchecked(i, callingUid, strResolvePackageName, -1, null);
        if (iNoteOperationUnchecked != 0 || Binder.getCallingUid() == i2) {
            return iNoteOperationUnchecked;
        }
        String strResolvePackageName2 = resolvePackageName(i2, str2);
        if (strResolvePackageName2 == null) {
            return 1;
        }
        return noteOperationUnchecked(i, i2, strResolvePackageName2, iNoteOperationUnchecked, strResolvePackageName);
    }

    public int noteOperation(int i, int i2, String str) {
        verifyIncomingUid(i2);
        verifyIncomingOp(i);
        String strResolvePackageName = resolvePackageName(i2, str);
        if (strResolvePackageName == null) {
            return 1;
        }
        return noteOperationUnchecked(i, i2, strResolvePackageName, 0, null);
    }

    private int noteOperationUnchecked(int i, int i2, String str, int i3, String str2) {
        synchronized (this) {
            Ops opsRawLocked = getOpsRawLocked(i2, str, true, false);
            if (opsRawLocked == null) {
                return 2;
            }
            Op opLocked = getOpLocked(opsRawLocked, i, true);
            if (isOpRestrictedLocked(i2, i, str)) {
                return 1;
            }
            UidState uidState = opsRawLocked.uidState;
            if (opLocked.duration == -1) {
                Slog.w(TAG, "Noting op not finished: uid " + i2 + " pkg " + str + " code " + i + " time=" + opLocked.time[uidState.state] + " duration=" + opLocked.duration);
            }
            opLocked.duration = 0;
            int iOpToSwitch = AppOpsManager.opToSwitch(i);
            if (uidState.opModes != null && uidState.opModes.indexOfKey(iOpToSwitch) >= 0) {
                int iEvalMode = uidState.evalMode(uidState.opModes.get(iOpToSwitch));
                if (iEvalMode != 0) {
                    opLocked.rejectTime[uidState.state] = System.currentTimeMillis();
                    return iEvalMode;
                }
            } else {
                int mode = (iOpToSwitch != i ? getOpLocked(opsRawLocked, iOpToSwitch, true) : opLocked).getMode();
                if (mode != 0) {
                    opLocked.rejectTime[uidState.state] = System.currentTimeMillis();
                    return mode;
                }
            }
            opLocked.time[uidState.state] = System.currentTimeMillis();
            opLocked.rejectTime[uidState.state] = 0;
            opLocked.proxyUid = i3;
            opLocked.proxyPackageName = str2;
            return 0;
        }
    }

    public void startWatchingActive(int[] iArr, IAppOpsActiveCallback iAppOpsActiveCallback) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        int i = this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") != 0 ? callingUid : -1;
        if (iArr != null) {
            Preconditions.checkArrayElementsInRange(iArr, 0, 77, "Invalid op code in: " + Arrays.toString(iArr));
        }
        if (iAppOpsActiveCallback == null) {
            return;
        }
        synchronized (this) {
            SparseArray<ActiveCallback> sparseArray = this.mActiveWatchers.get(iAppOpsActiveCallback.asBinder());
            if (sparseArray == null) {
                sparseArray = new SparseArray<>();
                this.mActiveWatchers.put(iAppOpsActiveCallback.asBinder(), sparseArray);
            }
            SparseArray<ActiveCallback> sparseArray2 = sparseArray;
            ActiveCallback activeCallback = new ActiveCallback(iAppOpsActiveCallback, i, callingUid, callingPid);
            for (int i2 : iArr) {
                sparseArray2.put(i2, activeCallback);
            }
        }
    }

    public void stopWatchingActive(IAppOpsActiveCallback iAppOpsActiveCallback) {
        if (iAppOpsActiveCallback == null) {
            return;
        }
        synchronized (this) {
            SparseArray<ActiveCallback> sparseArrayRemove = this.mActiveWatchers.remove(iAppOpsActiveCallback.asBinder());
            if (sparseArrayRemove == null) {
                return;
            }
            int size = sparseArrayRemove.size();
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    sparseArrayRemove.valueAt(i).destroy();
                }
            }
        }
    }

    public int startOperation(IBinder iBinder, int i, int i2, String str, boolean z) {
        Op opLocked;
        verifyIncomingUid(i2);
        verifyIncomingOp(i);
        String strResolvePackageName = resolvePackageName(i2, str);
        if (strResolvePackageName == null) {
            return 1;
        }
        ClientState clientState = (ClientState) iBinder;
        synchronized (this) {
            Ops opsRawLocked = getOpsRawLocked(i2, strResolvePackageName, true, false);
            if (opsRawLocked == null) {
                return 2;
            }
            Op opLocked2 = getOpLocked(opsRawLocked, i, true);
            if (isOpRestrictedLocked(i2, i, strResolvePackageName)) {
                return 1;
            }
            int iOpToSwitch = AppOpsManager.opToSwitch(i);
            UidState uidState = opsRawLocked.uidState;
            if (uidState.opModes != null && uidState.opModes.indexOfKey(iOpToSwitch) >= 0) {
                int iEvalMode = uidState.evalMode(uidState.opModes.get(iOpToSwitch));
                if (iEvalMode != 0 && (!z || iEvalMode != 3)) {
                    opLocked2.rejectTime[uidState.state] = System.currentTimeMillis();
                    return iEvalMode;
                }
            } else {
                if (iOpToSwitch != i) {
                    opLocked = getOpLocked(opsRawLocked, iOpToSwitch, true);
                } else {
                    opLocked = opLocked2;
                }
                int mode = opLocked.getMode();
                if (mode != 0 && (!z || mode != 3)) {
                    opLocked2.rejectTime[uidState.state] = System.currentTimeMillis();
                    return mode;
                }
            }
            if (opLocked2.startNesting == 0) {
                opLocked2.startRealtime = SystemClock.elapsedRealtime();
                opLocked2.time[uidState.state] = System.currentTimeMillis();
                opLocked2.rejectTime[uidState.state] = 0;
                opLocked2.duration = -1;
                scheduleOpActiveChangedIfNeededLocked(i, i2, str, true);
            }
            opLocked2.startNesting++;
            uidState.startNesting++;
            if (clientState.mStartedOps != null) {
                clientState.mStartedOps.add(opLocked2);
            }
            return 0;
        }
    }

    public void finishOperation(IBinder iBinder, int i, int i2, String str) {
        verifyIncomingUid(i2);
        verifyIncomingOp(i);
        String strResolvePackageName = resolvePackageName(i2, str);
        if (strResolvePackageName == null || !(iBinder instanceof ClientState)) {
            return;
        }
        ClientState clientState = (ClientState) iBinder;
        synchronized (this) {
            Op opLocked = getOpLocked(i, i2, strResolvePackageName, true);
            if (opLocked == null) {
                return;
            }
            if (!clientState.mStartedOps.remove(opLocked)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageUid(strResolvePackageName, 0, UserHandle.getUserId(i2)) < 0) {
                        Slog.i(TAG, "Finishing op=" + AppOpsManager.opToName(i) + " for non-existing package=" + strResolvePackageName + " in uid=" + i2);
                        return;
                    }
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Slog.wtf(TAG, "Operation not started: uid=" + opLocked.uid + " pkg=" + opLocked.packageName + " op=" + AppOpsManager.opToName(opLocked.op));
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            finishOperationLocked(opLocked, false);
            if (opLocked.startNesting <= 0) {
                scheduleOpActiveChangedIfNeededLocked(i, i2, str, false);
            }
        }
    }

    private void scheduleOpActiveChangedIfNeededLocked(int i, int i2, String str, boolean z) {
        int size = this.mActiveWatchers.size();
        ArraySet arraySet = null;
        for (int i3 = 0; i3 < size; i3++) {
            ActiveCallback activeCallback = this.mActiveWatchers.valueAt(i3).get(i);
            if (activeCallback != null && (activeCallback.mWatchingUid < 0 || activeCallback.mWatchingUid == i2)) {
                if (arraySet == null) {
                    arraySet = new ArraySet();
                }
                arraySet.add(activeCallback);
            }
        }
        if (arraySet == null) {
            return;
        }
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new HexConsumer() {
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                ((AppOpsService) obj).notifyOpActiveChanged((ArraySet) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (String) obj5, ((Boolean) obj6).booleanValue());
            }
        }, this, arraySet, Integer.valueOf(i), Integer.valueOf(i2), str, Boolean.valueOf(z)));
    }

    private void notifyOpActiveChanged(ArraySet<ActiveCallback> arraySet, int i, int i2, String str, boolean z) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int size = arraySet.size();
            for (int i3 = 0; i3 < size; i3++) {
                try {
                    arraySet.valueAt(i3).mCallback.opActiveChanged(i, i2, str, z);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int permissionToOpCode(String str) {
        if (str == null) {
            return -1;
        }
        return AppOpsManager.permissionToOpCode(str);
    }

    void finishOperationLocked(Op op, boolean z) {
        if (op.startNesting <= 1 || z) {
            if (op.startNesting == 1 || z) {
                op.duration = (int) (SystemClock.elapsedRealtime() - op.startRealtime);
                op.time[op.uidState.state] = System.currentTimeMillis();
            } else {
                Slog.w(TAG, "Finishing op nesting under-run: uid " + op.uid + " pkg " + op.packageName + " code " + op.op + " time=" + op.time + " duration=" + op.duration + " nesting=" + op.startNesting);
            }
            if (op.startNesting >= 1) {
                op.uidState.startNesting -= op.startNesting;
            }
            op.startNesting = 0;
            return;
        }
        op.startNesting--;
        op.uidState.startNesting--;
    }

    private void verifyIncomingUid(int i) {
        if (i == Binder.getCallingUid() || Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    private void verifyIncomingOp(int i) {
        if (CTA_MANAGER.isCtaSupported()) {
            if (i >= 0 && i < CTA_MANAGER.getOpNum()) {
                return;
            }
        } else if (i >= 0 && i < 78) {
            return;
        }
        throw new IllegalArgumentException("Bad operation #" + i);
    }

    private UidState getUidStateLocked(int i, boolean z) {
        UidState uidState = this.mUidStates.get(i);
        if (uidState == null) {
            if (!z) {
                return null;
            }
            UidState uidState2 = new UidState(i);
            this.mUidStates.put(i, uidState2);
            return uidState2;
        }
        if (uidState.pendingStateCommitTime == 0) {
            return uidState;
        }
        if (uidState.pendingStateCommitTime < this.mLastUptime) {
            commitUidPendingStateLocked(uidState);
            return uidState;
        }
        this.mLastUptime = SystemClock.uptimeMillis();
        if (uidState.pendingStateCommitTime < this.mLastUptime) {
            commitUidPendingStateLocked(uidState);
            return uidState;
        }
        return uidState;
    }

    private void commitUidPendingStateLocked(UidState uidState) {
        int iKeyAt;
        ArraySet<ModeCallback> arraySet;
        int i;
        boolean z = uidState.state <= 2;
        boolean z2 = uidState.pendingState <= 2;
        uidState.state = uidState.pendingState;
        uidState.pendingStateCommitTime = 0L;
        if (uidState.hasForegroundWatchers && z != z2) {
            for (int size = uidState.foregroundOps.size() - 1; size >= 0; size--) {
                if (uidState.foregroundOps.valueAt(size) && (arraySet = this.mOpModeWatchers.get((iKeyAt = uidState.foregroundOps.keyAt(size)))) != null) {
                    for (int size2 = arraySet.size() - 1; size2 >= 0; size2--) {
                        ModeCallback modeCallbackValueAt = arraySet.valueAt(size2);
                        if ((modeCallbackValueAt.mFlags & 1) != 0 && modeCallbackValueAt.isWatchingUid(uidState.uid)) {
                            boolean z3 = uidState.opModes != null && uidState.opModes.get(iKeyAt) == 4;
                            if (uidState.pkgOps != null) {
                                int size3 = uidState.pkgOps.size() - 1;
                                while (size3 >= 0) {
                                    Op op = uidState.pkgOps.valueAt(size3).get(iKeyAt);
                                    if (!z3 && (op == null || op.mode != 4)) {
                                        i = size3;
                                    } else {
                                        i = size3;
                                        this.mHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE, this, modeCallbackValueAt, Integer.valueOf(iKeyAt), Integer.valueOf(uidState.uid), uidState.pkgOps.keyAt(size3)));
                                    }
                                    size3 = i - 1;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Ops getOpsRawLocked(int i, String str, boolean z, boolean z2) {
        int iResolveUid;
        UidState uidStateLocked = getUidStateLocked(i, z);
        if (uidStateLocked == null) {
            return null;
        }
        if (uidStateLocked.pkgOps == null) {
            if (!z) {
                return null;
            }
            uidStateLocked.pkgOps = new ArrayMap<>();
        }
        Ops ops = uidStateLocked.pkgOps.get(str);
        if (ops != null) {
            return ops;
        }
        if (!z) {
            return null;
        }
        boolean z3 = false;
        if (i != 0) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int i2 = -1;
            try {
                try {
                    ApplicationInfo applicationInfo = ActivityThread.getPackageManager().getApplicationInfo(str, 268435456, UserHandle.getUserId(i));
                    if (applicationInfo != null) {
                        iResolveUid = applicationInfo.uid;
                        try {
                            i2 = applicationInfo.privateFlags & 8;
                            if (i2 != 0) {
                                z3 = true;
                            }
                        } catch (RemoteException e) {
                            e = e;
                            Slog.w(TAG, "Could not contact PackageManager", e);
                        }
                    } else {
                        iResolveUid = resolveUid(str);
                        if (iResolveUid >= 0) {
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (RemoteException e2) {
                e = e2;
                iResolveUid = i2;
            }
            if (iResolveUid != i) {
                if (!z2) {
                    RuntimeException runtimeException = new RuntimeException("here");
                    runtimeException.fillInStackTrace();
                    Slog.w(TAG, "Bad call: specified package " + str + " under uid " + i + " but it is really " + iResolveUid, runtimeException);
                }
                return null;
            }
        }
        Ops ops2 = new Ops(str, uidStateLocked, z3);
        uidStateLocked.pkgOps.put(str, ops2);
        return ops2;
    }

    private void scheduleWriteLocked() {
        if (!this.mWriteScheduled) {
            this.mWriteScheduled = true;
            this.mHandler.postDelayed(this.mWriteRunner, 1800000L);
        }
    }

    private void scheduleFastWriteLocked() {
        if (!this.mFastWriteScheduled) {
            this.mWriteScheduled = true;
            this.mFastWriteScheduled = true;
            this.mHandler.removeCallbacks(this.mWriteRunner);
            this.mHandler.postDelayed(this.mWriteRunner, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    private Op getOpLocked(int i, int i2, String str, boolean z) {
        Ops opsRawLocked = getOpsRawLocked(i2, str, z, false);
        if (opsRawLocked == null) {
            return null;
        }
        return getOpLocked(opsRawLocked, i, z);
    }

    private Op getOpLocked(Ops ops, int i, boolean z) {
        Op op = ops.get(i);
        if (op == null) {
            if (!z) {
                return null;
            }
            op = new Op(ops.uidState, ops.packageName, i);
            ops.put(i, op);
        }
        if (z) {
            scheduleWriteLocked();
        }
        return op;
    }

    private boolean isOpRestrictedLocked(int i, int i2, String str) {
        int userId = UserHandle.getUserId(i);
        int size = this.mOpUserRestrictions.size();
        for (int i3 = 0; i3 < size; i3++) {
            if (this.mOpUserRestrictions.valueAt(i3).hasRestriction(i2, str, userId)) {
                if (AppOpsManager.opAllowSystemBypassRestriction(i2)) {
                    synchronized (this) {
                        Ops opsRawLocked = getOpsRawLocked(i, str, true, false);
                        if (opsRawLocked != null && opsRawLocked.isPrivileged) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    void readState() {
        r0 = r9.mFile;
        synchronized (r0) {
            ;
            synchronized (r9) {
                ;
                r1 = r9.mFile.openRead();
                r9.mUidStates.clear();
                r2 = -1;
                r3 = android.util.Xml.newPullParser();
                r3.setInput(r1, java.nio.charset.StandardCharsets.UTF_8.name());
                do {
                    r4 = r3.next();
                    if (r4 != 2) {
                    }
                } while (r4 != 1);
                if (r4 == 2) {
                    r4 = r3.getAttributeValue(null, "v");
                    if (r4 != null) {
                        r2 = java.lang.Integer.parseInt(r4);
                    }
                    r4 = r3.getDepth();
                    while (true) {
                        r6 = r3.next();
                        if (r6 == 1 || (r6 == 3 && r3.getDepth() <= r4)) {
                        } else {
                            if (r6 != 3 && r6 != 4) {
                                r6 = r3.getName();
                                if (r6.equals("pkg")) {
                                    readPackage(r3);
                                } else {
                                    if (r6.equals(com.android.server.net.watchlist.WatchlistLoggingHandler.WatchlistEventKeys.UID)) {
                                        readUidOps(r3);
                                    } else {
                                        r7 = new java.lang.StringBuilder();
                                        r7.append("Unknown element under <app-ops>: ");
                                        r7.append(r3.getName());
                                        android.util.Slog.w(com.android.server.AppOpsService.TAG, r7.toString());
                                        com.android.internal.util.XmlUtils.skipCurrentTag(r3);
                                    }
                                }
                            }
                        }
                    }
                    r1.close();
                    while (true) {
                    }
                } else {
                    throw new java.lang.IllegalStateException("no start tag found");
                }
            }
        }
        synchronized (r9) {
            ;
            upgradeLocked(r2);
        }
        return;
    }

    private void upgradeRunAnyInBackgroundLocked() {
        Op op;
        int iIndexOfKey;
        for (int i = 0; i < this.mUidStates.size(); i++) {
            UidState uidStateValueAt = this.mUidStates.valueAt(i);
            if (uidStateValueAt != null) {
                if (uidStateValueAt.opModes != null && (iIndexOfKey = uidStateValueAt.opModes.indexOfKey(63)) >= 0) {
                    uidStateValueAt.opModes.put(70, uidStateValueAt.opModes.valueAt(iIndexOfKey));
                }
                if (uidStateValueAt.pkgOps != null) {
                    boolean z = false;
                    for (int i2 = 0; i2 < uidStateValueAt.pkgOps.size(); i2++) {
                        Ops opsValueAt = uidStateValueAt.pkgOps.valueAt(i2);
                        if (opsValueAt != null && (op = opsValueAt.get(63)) != null && op.mode != AppOpsManager.opToDefaultMode(op.op)) {
                            Op op2 = new Op(op.uidState, op.packageName, 70);
                            op2.mode = op.mode;
                            opsValueAt.put(70, op2);
                            z = true;
                        }
                    }
                    if (z) {
                        uidStateValueAt.evalForegroundOps(this.mOpModeWatchers);
                    }
                }
            }
        }
    }

    private void upgradeLocked(int i) {
        if (i < 1) {
            Slog.d(TAG, "Upgrading app-ops xml from version " + i + " to 1");
            if (i == -1) {
                upgradeRunAnyInBackgroundLocked();
            }
            scheduleFastWriteLocked();
        }
    }

    void readUidOps(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException, NumberFormatException {
        int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "n"));
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals("op")) {
                            int i2 = Integer.parseInt(xmlPullParser.getAttributeValue(null, "n"));
                            int i3 = Integer.parseInt(xmlPullParser.getAttributeValue(null, "m"));
                            UidState uidStateLocked = getUidStateLocked(i, true);
                            if (uidStateLocked.opModes == null) {
                                uidStateLocked.opModes = new SparseIntArray();
                            }
                            uidStateLocked.opModes.put(i2, i3);
                        } else {
                            Slog.w(TAG, "Unknown element under <uid-ops>: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readPackage(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException, NumberFormatException {
        String attributeValue = xmlPullParser.getAttributeValue(null, "n");
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (xmlPullParser.getName().equals(WatchlistLoggingHandler.WatchlistEventKeys.UID)) {
                            readUid(xmlPullParser, attributeValue);
                        } else {
                            Slog.w(TAG, "Unknown element under <pkg>: " + xmlPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readUid(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException, NumberFormatException {
        boolean z;
        ?? r6;
        String str2 = null;
        int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "n"));
        String attributeValue = xmlPullParser.getAttributeValue(null, "p");
        boolean z2 = true;
        if (attributeValue == null) {
            try {
                if (ActivityThread.getPackageManager() != null) {
                    ApplicationInfo applicationInfo = ActivityThread.getPackageManager().getApplicationInfo(str, 0, UserHandle.getUserId(i));
                    if (applicationInfo != null) {
                        z = (applicationInfo.privateFlags & 8) != 0;
                    }
                } else {
                    return;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not contact PackageManager", e);
                z = false;
            }
        } else {
            z = Boolean.parseBoolean(attributeValue);
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != z2 && (next != 3 || xmlPullParser.getDepth() > depth)) {
                if (next != 3 && next != 4) {
                    if (xmlPullParser.getName().equals("op")) {
                        UidState uidStateLocked = getUidStateLocked(i, z2);
                        if (uidStateLocked.pkgOps == null) {
                            uidStateLocked.pkgOps = new ArrayMap<>();
                        }
                        Op op = new Op(uidStateLocked, str, Integer.parseInt(xmlPullParser.getAttributeValue(str2, "n")));
                        int attributeCount = xmlPullParser.getAttributeCount() - (z2 ? 1 : 0);
                        ?? r8 = z2;
                        while (attributeCount >= 0) {
                            String attributeName = xmlPullParser.getAttributeName(attributeCount);
                            String attributeValue2 = xmlPullParser.getAttributeValue(attributeCount);
                            switch (attributeName.hashCode()) {
                                case 100:
                                    r6 = !attributeName.equals("d") ? -1 : r8;
                                    break;
                                case HdmiCecKeycode.CEC_KEYCODE_POWER_ON_FUNCTION:
                                    if (attributeName.equals("m")) {
                                        r6 = 0;
                                        break;
                                    }
                                    break;
                                case 114:
                                    if (attributeName.equals("r")) {
                                        r6 = 17;
                                        break;
                                    }
                                    break;
                                case HdmiCecKeycode.CEC_KEYCODE_F4_YELLOW:
                                    if (attributeName.equals("t")) {
                                        r6 = 16;
                                        break;
                                    }
                                    break;
                                case 3584:
                                    if (attributeName.equals("pp")) {
                                        r6 = 3;
                                        break;
                                    }
                                    break;
                                case 3589:
                                    if (attributeName.equals("pu")) {
                                        r6 = 2;
                                        break;
                                    }
                                    break;
                                case 3632:
                                    if (attributeName.equals("rb")) {
                                        r6 = 14;
                                        break;
                                    }
                                    break;
                                case 3633:
                                    if (attributeName.equals("rc")) {
                                        r6 = 15;
                                        break;
                                    }
                                    break;
                                case 3636:
                                    if (attributeName.equals("rf")) {
                                        r6 = 13;
                                        break;
                                    }
                                    break;
                                case 3646:
                                    if (attributeName.equals("rp")) {
                                        r6 = 10;
                                        break;
                                    }
                                    break;
                                case 3650:
                                    if (attributeName.equals("rt")) {
                                        r6 = 11;
                                        break;
                                    }
                                    break;
                                case 3694:
                                    if (attributeName.equals("tb")) {
                                        r6 = 8;
                                        break;
                                    }
                                    break;
                                case 3695:
                                    if (attributeName.equals("tc")) {
                                        r6 = 9;
                                        break;
                                    }
                                    break;
                                case 3698:
                                    if (attributeName.equals("tf")) {
                                        r6 = 7;
                                        break;
                                    }
                                    break;
                                case 3708:
                                    if (attributeName.equals("tp")) {
                                        r6 = 4;
                                        break;
                                    }
                                    break;
                                case 3712:
                                    if (attributeName.equals("tt")) {
                                        r6 = 5;
                                        break;
                                    }
                                    break;
                                case 112831:
                                    if (attributeName.equals("rfs")) {
                                        r6 = 12;
                                        break;
                                    }
                                    break;
                                case 114753:
                                    if (attributeName.equals("tfs")) {
                                        r6 = 6;
                                        break;
                                    }
                                    break;
                            }
                            switch (r6) {
                                case 0:
                                    op.mode = Integer.parseInt(attributeValue2);
                                    break;
                                case 1:
                                    op.duration = Integer.parseInt(attributeValue2);
                                    break;
                                case 2:
                                    op.proxyUid = Integer.parseInt(attributeValue2);
                                    break;
                                case 3:
                                    op.proxyPackageName = attributeValue2;
                                    break;
                                case 4:
                                    op.time[0] = Long.parseLong(attributeValue2);
                                    break;
                                case 5:
                                    op.time[r8] = Long.parseLong(attributeValue2);
                                    break;
                                case 6:
                                    op.time[2] = Long.parseLong(attributeValue2);
                                    break;
                                case 7:
                                    op.time[3] = Long.parseLong(attributeValue2);
                                    break;
                                case 8:
                                    op.time[4] = Long.parseLong(attributeValue2);
                                    break;
                                case 9:
                                    op.time[5] = Long.parseLong(attributeValue2);
                                    break;
                                case 10:
                                    op.rejectTime[0] = Long.parseLong(attributeValue2);
                                    break;
                                case 11:
                                    op.rejectTime[r8] = Long.parseLong(attributeValue2);
                                    break;
                                case 12:
                                    op.rejectTime[2] = Long.parseLong(attributeValue2);
                                    break;
                                case 13:
                                    op.rejectTime[3] = Long.parseLong(attributeValue2);
                                    break;
                                case 14:
                                    op.rejectTime[4] = Long.parseLong(attributeValue2);
                                    break;
                                case 15:
                                    op.rejectTime[5] = Long.parseLong(attributeValue2);
                                    break;
                                case 16:
                                    op.time[r8] = Long.parseLong(attributeValue2);
                                    break;
                                case 17:
                                    op.rejectTime[r8] = Long.parseLong(attributeValue2);
                                    break;
                                default:
                                    Slog.w(TAG, "Unknown attribute in 'op' tag: " + attributeName);
                                    break;
                            }
                            attributeCount--;
                            r8 = 1;
                        }
                        Ops ops = uidStateLocked.pkgOps.get(str);
                        if (ops == null) {
                            ops = new Ops(str, uidStateLocked, z);
                            uidStateLocked.pkgOps.put(str, ops);
                        }
                        ops.put(op.op, op);
                    } else {
                        Slog.w(TAG, "Unknown element under <pkg>: " + xmlPullParser.getName());
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                    str2 = null;
                    z2 = true;
                }
            }
        }
        UidState uidStateLocked2 = getUidStateLocked(i, false);
        if (uidStateLocked2 != null) {
            uidStateLocked2.evalForegroundOps(this.mOpModeWatchers);
        }
    }

    void writeState() {
        synchronized (this.mFile) {
            try {
                try {
                    FileOutputStream fileOutputStreamStartWrite = this.mFile.startWrite();
                    List<AppOpsManager.PackageOps> packagesForOps = getPackagesForOps(null);
                    try {
                        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                        fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                        fastXmlSerializer.startDocument(null, true);
                        fastXmlSerializer.startTag(null, "app-ops");
                        fastXmlSerializer.attribute(null, "v", String.valueOf(1));
                        int size = this.mUidStates.size();
                        boolean z = false;
                        for (int i = 0; i < size; i++) {
                            UidState uidStateValueAt = this.mUidStates.valueAt(i);
                            if (uidStateValueAt.opModes != null && uidStateValueAt.opModes.size() > 0) {
                                fastXmlSerializer.startTag(null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                                fastXmlSerializer.attribute(null, "n", Integer.toString(uidStateValueAt.uid));
                                SparseIntArray sparseIntArray = uidStateValueAt.opModes;
                                int size2 = sparseIntArray.size();
                                for (int i2 = 0; i2 < size2; i2++) {
                                    int iKeyAt = sparseIntArray.keyAt(i2);
                                    int iValueAt = sparseIntArray.valueAt(i2);
                                    fastXmlSerializer.startTag(null, "op");
                                    fastXmlSerializer.attribute(null, "n", Integer.toString(iKeyAt));
                                    fastXmlSerializer.attribute(null, "m", Integer.toString(iValueAt));
                                    fastXmlSerializer.endTag(null, "op");
                                }
                                fastXmlSerializer.endTag(null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                            }
                        }
                        if (packagesForOps != null) {
                            String packageName = null;
                            int i3 = 0;
                            while (i3 < packagesForOps.size()) {
                                AppOpsManager.PackageOps packageOps = packagesForOps.get(i3);
                                if (!packageOps.getPackageName().equals(packageName)) {
                                    if (packageName != null) {
                                        fastXmlSerializer.endTag(null, "pkg");
                                    }
                                    packageName = packageOps.getPackageName();
                                    fastXmlSerializer.startTag(null, "pkg");
                                    fastXmlSerializer.attribute(null, "n", packageName);
                                }
                                fastXmlSerializer.startTag(null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                                fastXmlSerializer.attribute(null, "n", Integer.toString(packageOps.getUid()));
                                synchronized (this) {
                                    Ops opsRawLocked = getOpsRawLocked(packageOps.getUid(), packageOps.getPackageName(), z, z);
                                    if (opsRawLocked != null) {
                                        fastXmlSerializer.attribute(null, "p", Boolean.toString(opsRawLocked.isPrivileged));
                                    } else {
                                        fastXmlSerializer.attribute(null, "p", Boolean.toString(z));
                                    }
                                }
                                ?? ops = packageOps.getOps();
                                for (?? r10 = z; r10 < ops.size(); r10++) {
                                    AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) ops.get(r10);
                                    fastXmlSerializer.startTag(null, "op");
                                    fastXmlSerializer.attribute(null, "n", Integer.toString(opEntry.getOp()));
                                    if (opEntry.getMode() != AppOpsManager.opToDefaultMode(opEntry.getOp())) {
                                        fastXmlSerializer.attribute(null, "m", Integer.toString(opEntry.getMode()));
                                    }
                                    for (?? r12 = z; r12 < 6; r12++) {
                                        long lastTimeFor = opEntry.getLastTimeFor((int) r12);
                                        if (lastTimeFor != 0) {
                                            fastXmlSerializer.attribute(null, UID_STATE_TIME_ATTRS[r12], Long.toString(lastTimeFor));
                                        }
                                        long lastRejectTimeFor = opEntry.getLastRejectTimeFor((int) r12);
                                        if (lastRejectTimeFor != 0) {
                                            fastXmlSerializer.attribute(null, UID_STATE_REJECT_ATTRS[r12], Long.toString(lastRejectTimeFor));
                                        }
                                    }
                                    int duration = opEntry.getDuration();
                                    if (duration != 0) {
                                        fastXmlSerializer.attribute(null, "d", Integer.toString(duration));
                                    }
                                    int proxyUid = opEntry.getProxyUid();
                                    if (proxyUid != -1) {
                                        fastXmlSerializer.attribute(null, "pu", Integer.toString(proxyUid));
                                    }
                                    String proxyPackageName = opEntry.getProxyPackageName();
                                    if (proxyPackageName != null) {
                                        fastXmlSerializer.attribute(null, "pp", proxyPackageName);
                                    }
                                    fastXmlSerializer.endTag(null, "op");
                                    z = false;
                                }
                                fastXmlSerializer.endTag(null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                                i3++;
                                z = false;
                            }
                            if (packageName != null) {
                                fastXmlSerializer.endTag(null, "pkg");
                            }
                        }
                        fastXmlSerializer.endTag(null, "app-ops");
                        fastXmlSerializer.endDocument();
                        this.mFile.finishWrite(fileOutputStreamStartWrite);
                    } catch (IOException e) {
                        Slog.w(TAG, "Failed to write state, restoring backup.", e);
                        this.mFile.failWrite(fileOutputStreamStartWrite);
                    }
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to write state: " + e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    static class Shell extends ShellCommand {
        static final Binder sBinder = new Binder();
        final IAppOpsService mInterface;
        final AppOpsService mInternal;
        IBinder mToken;
        int mode;
        String modeStr;
        int nonpackageUid;
        int op;
        String opStr;
        String packageName;
        int packageUid;
        int userId = 0;

        Shell(IAppOpsService iAppOpsService, AppOpsService appOpsService) {
            this.mInterface = iAppOpsService;
            this.mInternal = appOpsService;
            try {
                this.mToken = this.mInterface.getToken(sBinder);
            } catch (RemoteException e) {
            }
        }

        public int onCommand(String str) {
            return AppOpsService.onShellCommand(this, str);
        }

        public void onHelp() {
            AppOpsService.dumpCommandHelp(getOutPrintWriter());
        }

        private static int strOpToOp(String str, PrintWriter printWriter) {
            try {
                return AppOpsManager.strOpToOp(str);
            } catch (IllegalArgumentException e) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e2) {
                    try {
                        return AppOpsManager.strDebugOpToOp(str);
                    } catch (IllegalArgumentException e3) {
                        printWriter.println("Error: " + e3.getMessage());
                        return -1;
                    }
                }
            }
        }

        static int strModeToMode(String str, PrintWriter printWriter) {
            for (int length = AppOpsManager.MODE_NAMES.length - 1; length >= 0; length--) {
                if (AppOpsManager.MODE_NAMES[length].equals(str)) {
                    return length;
                }
            }
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                printWriter.println("Error: Mode " + str + " is not valid");
                return -1;
            }
        }

        int parseUserOpMode(int i, PrintWriter printWriter) throws RemoteException {
            this.userId = -2;
            this.opStr = null;
            this.modeStr = null;
            while (true) {
                String nextArg = getNextArg();
                if (nextArg == null) {
                    break;
                }
                if ("--user".equals(nextArg)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (this.opStr == null) {
                    this.opStr = nextArg;
                } else if (this.modeStr == null) {
                    this.modeStr = nextArg;
                    break;
                }
            }
            if (this.opStr == null) {
                printWriter.println("Error: Operation not specified.");
                return -1;
            }
            this.op = strOpToOp(this.opStr, printWriter);
            if (this.op < 0) {
                return -1;
            }
            if (this.modeStr != null) {
                int iStrModeToMode = strModeToMode(this.modeStr, printWriter);
                this.mode = iStrModeToMode;
                return iStrModeToMode < 0 ? -1 : 0;
            }
            this.mode = i;
            return 0;
        }

        int parseUserPackageOp(boolean z, PrintWriter printWriter) throws RemoteException {
            this.userId = -2;
            this.packageName = null;
            this.opStr = null;
            while (true) {
                String nextArg = getNextArg();
                if (nextArg == null) {
                    break;
                }
                if ("--user".equals(nextArg)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (this.packageName == null) {
                    this.packageName = nextArg;
                } else if (this.opStr == null) {
                    this.opStr = nextArg;
                    break;
                }
            }
            if (this.packageName == null) {
                printWriter.println("Error: Package name not specified.");
                return -1;
            }
            if (this.opStr == null && z) {
                printWriter.println("Error: Operation not specified.");
                return -1;
            }
            if (this.opStr != null) {
                this.op = strOpToOp(this.opStr, printWriter);
                if (this.op < 0) {
                    return -1;
                }
            } else {
                this.op = -1;
            }
            if (this.userId == -2) {
                this.userId = ActivityManager.getCurrentUser();
            }
            this.nonpackageUid = -1;
            try {
                this.nonpackageUid = Integer.parseInt(this.packageName);
            } catch (NumberFormatException e) {
            }
            if (this.nonpackageUid == -1 && this.packageName.length() > 1 && this.packageName.charAt(0) == 'u' && this.packageName.indexOf(46) < 0) {
                int i = 1;
                while (i < this.packageName.length() && this.packageName.charAt(i) >= '0' && this.packageName.charAt(i) <= '9') {
                    i++;
                }
                if (i > 1 && i < this.packageName.length()) {
                    try {
                        int i2 = Integer.parseInt(this.packageName.substring(1, i));
                        char cCharAt = this.packageName.charAt(i);
                        int i3 = i + 1;
                        int i4 = i3;
                        while (i4 < this.packageName.length() && this.packageName.charAt(i4) >= '0' && this.packageName.charAt(i4) <= '9') {
                            i4++;
                        }
                        if (i4 > i3) {
                            try {
                                int i5 = Integer.parseInt(this.packageName.substring(i3, i4));
                                if (cCharAt == 'a') {
                                    this.nonpackageUid = UserHandle.getUid(i2, i5 + 10000);
                                } else if (cCharAt == 's') {
                                    this.nonpackageUid = UserHandle.getUid(i2, i5);
                                }
                            } catch (NumberFormatException e2) {
                            }
                        }
                    } catch (NumberFormatException e3) {
                    }
                }
            }
            if (this.nonpackageUid == -1) {
                this.packageUid = AppOpsService.resolveUid(this.packageName);
                if (this.packageUid < 0) {
                    this.packageUid = AppGlobals.getPackageManager().getPackageUid(this.packageName, 8192, this.userId);
                }
                if (this.packageUid < 0) {
                    printWriter.println("Error: No UID for " + this.packageName + " in user " + this.userId);
                    return -1;
                }
            } else {
                this.packageName = null;
            }
            return 0;
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new Shell(this, this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    static void dumpCommandHelp(PrintWriter printWriter) {
        printWriter.println("AppOps service (appops) commands:");
        printWriter.println("  help");
        printWriter.println("    Print this help text.");
        printWriter.println("  start [--user <USER_ID>] <PACKAGE | UID> <OP> ");
        printWriter.println("    Starts a given operation for a particular application.");
        printWriter.println("  stop [--user <USER_ID>] <PACKAGE | UID> <OP> ");
        printWriter.println("    Stops a given operation for a particular application.");
        printWriter.println("  set [--user <USER_ID>] <PACKAGE | UID> <OP> <MODE>");
        printWriter.println("    Set the mode for a particular application and operation.");
        printWriter.println("  get [--user <USER_ID>] <PACKAGE | UID> [<OP>]");
        printWriter.println("    Return the mode for a particular application and optional operation.");
        printWriter.println("  query-op [--user <USER_ID>] <OP> [<MODE>]");
        printWriter.println("    Print all packages that currently have the given op in the given mode.");
        printWriter.println("  reset [--user <USER_ID>] [<PACKAGE>]");
        printWriter.println("    Reset the given application or all applications to default modes.");
        printWriter.println("  write-settings");
        printWriter.println("    Immediately write pending changes to storage.");
        printWriter.println("  read-settings");
        printWriter.println("    Read the last written settings, replacing current state in RAM.");
        printWriter.println("  options:");
        printWriter.println("    <PACKAGE> an Android package name.");
        printWriter.println("    <OP>      an AppOps operation.");
        printWriter.println("    <MODE>    one of allow, ignore, deny, or default");
        printWriter.println("    <USER_ID> the user id under which the package is installed. If --user is not");
        printWriter.println("              specified, the current user is assumed.");
    }

    static int onShellCommand(Shell shell, String str) {
        byte b;
        List uidOps;
        Object[] objArr;
        if (str == null) {
            return shell.handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = shell.getOutPrintWriter();
        PrintWriter errPrintWriter = shell.getErrPrintWriter();
        try {
            switch (str.hashCode()) {
                case -1703718319:
                    b = str.equals("write-settings") ? (byte) 4 : (byte) -1;
                    break;
                case -1166702330:
                    if (str.equals("query-op")) {
                        b = 2;
                        break;
                    }
                    break;
                case 102230:
                    if (str.equals("get")) {
                        b = 1;
                        break;
                    }
                    break;
                case 113762:
                    if (str.equals("set")) {
                        b = 0;
                        break;
                    }
                    break;
                case 3540994:
                    if (str.equals("stop")) {
                        b = 7;
                        break;
                    }
                    break;
                case 108404047:
                    if (str.equals("reset")) {
                        b = 3;
                        break;
                    }
                    break;
                case 109757538:
                    if (str.equals("start")) {
                        b = 6;
                        break;
                    }
                    break;
                case 2085703290:
                    if (str.equals("read-settings")) {
                        b = 5;
                        break;
                    }
                    break;
                default:
                    break;
            }
            String str2 = null;
            switch (b) {
                case 0:
                    int userPackageOp = shell.parseUserPackageOp(true, errPrintWriter);
                    if (userPackageOp < 0) {
                        return userPackageOp;
                    }
                    String nextArg = shell.getNextArg();
                    if (nextArg == null) {
                        errPrintWriter.println("Error: Mode not specified.");
                        return -1;
                    }
                    int iStrModeToMode = Shell.strModeToMode(nextArg, errPrintWriter);
                    if (iStrModeToMode < 0) {
                        return -1;
                    }
                    if (shell.packageName != null) {
                        shell.mInterface.setMode(shell.op, shell.packageUid, shell.packageName, iStrModeToMode);
                    } else {
                        shell.mInterface.setUidMode(shell.op, shell.nonpackageUid, iStrModeToMode);
                    }
                    return 0;
                case 1:
                    int userPackageOp2 = shell.parseUserPackageOp(false, errPrintWriter);
                    if (userPackageOp2 < 0) {
                        return userPackageOp2;
                    }
                    if (shell.packageName != null) {
                        uidOps = shell.mInterface.getOpsForPackage(shell.packageUid, shell.packageName, shell.op != -1 ? new int[]{shell.op} : null);
                    } else {
                        uidOps = shell.mInterface.getUidOps(shell.nonpackageUid, shell.op != -1 ? new int[]{shell.op} : null);
                    }
                    if (uidOps != null && uidOps.size() > 0) {
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        for (int i = 0; i < uidOps.size(); i++) {
                            List ops = ((AppOpsManager.PackageOps) uidOps.get(i)).getOps();
                            for (int i2 = 0; i2 < ops.size(); i2++) {
                                AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) ops.get(i2);
                                outPrintWriter.print(AppOpsManager.opToName(opEntry.getOp()));
                                outPrintWriter.print(": ");
                                outPrintWriter.print(AppOpsManager.modeToName(opEntry.getMode()));
                                if (opEntry.getTime() != 0) {
                                    outPrintWriter.print("; time=");
                                    TimeUtils.formatDuration(jCurrentTimeMillis - opEntry.getTime(), outPrintWriter);
                                    outPrintWriter.print(" ago");
                                }
                                if (opEntry.getRejectTime() != 0) {
                                    outPrintWriter.print("; rejectTime=");
                                    TimeUtils.formatDuration(jCurrentTimeMillis - opEntry.getRejectTime(), outPrintWriter);
                                    outPrintWriter.print(" ago");
                                }
                                if (opEntry.getDuration() == -1) {
                                    outPrintWriter.print(" (running)");
                                } else if (opEntry.getDuration() != 0) {
                                    outPrintWriter.print("; duration=");
                                    TimeUtils.formatDuration(opEntry.getDuration(), outPrintWriter);
                                }
                                outPrintWriter.println();
                            }
                        }
                        return 0;
                    }
                    outPrintWriter.println("No operations.");
                    if (shell.op > -1 && shell.op < 78) {
                        outPrintWriter.println("Default mode: " + AppOpsManager.modeToName(AppOpsManager.opToDefaultMode(shell.op)));
                    }
                    return 0;
                case 2:
                    int userOpMode = shell.parseUserOpMode(1, errPrintWriter);
                    if (userOpMode < 0) {
                        return userOpMode;
                    }
                    List packagesForOps = shell.mInterface.getPackagesForOps(new int[]{shell.op});
                    if (packagesForOps != null && packagesForOps.size() > 0) {
                        for (int i3 = 0; i3 < packagesForOps.size(); i3++) {
                            AppOpsManager.PackageOps packageOps = (AppOpsManager.PackageOps) packagesForOps.get(i3);
                            List ops2 = ((AppOpsManager.PackageOps) packagesForOps.get(i3)).getOps();
                            int i4 = 0;
                            while (true) {
                                if (i4 < ops2.size()) {
                                    AppOpsManager.OpEntry opEntry2 = (AppOpsManager.OpEntry) ops2.get(i4);
                                    if (opEntry2.getOp() != shell.op || opEntry2.getMode() != shell.mode) {
                                        i4++;
                                    } else {
                                        objArr = true;
                                    }
                                } else {
                                    objArr = false;
                                }
                            }
                            if (objArr != false) {
                                outPrintWriter.println(packageOps.getPackageName());
                            }
                        }
                        return 0;
                    }
                    outPrintWriter.println("No operations.");
                    return 0;
                case 3:
                    int currentUser = -2;
                    while (true) {
                        String nextArg2 = shell.getNextArg();
                        if (nextArg2 != null) {
                            if ("--user".equals(nextArg2)) {
                                currentUser = UserHandle.parseUserArg(shell.getNextArgRequired());
                            } else {
                                if (str2 != null) {
                                    errPrintWriter.println("Error: Unsupported argument: " + nextArg2);
                                    return -1;
                                }
                                str2 = nextArg2;
                            }
                        } else {
                            if (currentUser == -2) {
                                currentUser = ActivityManager.getCurrentUser();
                            }
                            shell.mInterface.resetAllModes(currentUser, str2);
                            outPrintWriter.print("Reset all modes for: ");
                            if (currentUser == -1) {
                                outPrintWriter.print("all users");
                            } else {
                                outPrintWriter.print("user ");
                                outPrintWriter.print(currentUser);
                            }
                            outPrintWriter.print(", ");
                            if (str2 == null) {
                                outPrintWriter.println("all packages");
                            } else {
                                outPrintWriter.print("package ");
                                outPrintWriter.println(str2);
                            }
                            return 0;
                        }
                    }
                    break;
                case 4:
                    shell.mInternal.enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), -1);
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        synchronized (shell.mInternal) {
                            shell.mInternal.mHandler.removeCallbacks(shell.mInternal.mWriteRunner);
                            break;
                        }
                        shell.mInternal.writeState();
                        outPrintWriter.println("Current settings written.");
                        return 0;
                    } finally {
                    }
                case 5:
                    shell.mInternal.enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), -1);
                    long jClearCallingIdentity2 = Binder.clearCallingIdentity();
                    try {
                        shell.mInternal.readState();
                        outPrintWriter.println("Last settings read.");
                        return 0;
                    } finally {
                    }
                case 6:
                    int userPackageOp3 = shell.parseUserPackageOp(true, errPrintWriter);
                    if (userPackageOp3 < 0) {
                        return userPackageOp3;
                    }
                    if (shell.packageName == null) {
                        return -1;
                    }
                    shell.mInterface.startOperation(shell.mToken, shell.op, shell.packageUid, shell.packageName, true);
                    return 0;
                case 7:
                    int userPackageOp4 = shell.parseUserPackageOp(true, errPrintWriter);
                    if (userPackageOp4 < 0) {
                        return userPackageOp4;
                    }
                    if (shell.packageName == null) {
                        return -1;
                    }
                    shell.mInterface.finishOperation(shell.mToken, shell.op, shell.packageUid, shell.packageName);
                    return 0;
                default:
                    return shell.handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
        outPrintWriter.println("Remote exception: " + e);
        return -1;
    }

    private void dumpHelp(PrintWriter printWriter) {
        printWriter.println("AppOps service (appops) dump options:");
        printWriter.println("  -h");
        printWriter.println("    Print this help text.");
        printWriter.println("  --op [OP]");
        printWriter.println("    Limit output to data associated with the given app op code.");
        printWriter.println("  --mode [MODE]");
        printWriter.println("    Limit output to data associated with the given app op mode.");
        printWriter.println("  --package [PACKAGE]");
        printWriter.println("    Limit output to data associated with the given package name.");
    }

    private void dumpTimesLocked(PrintWriter printWriter, String str, String str2, long[] jArr, long j, SimpleDateFormat simpleDateFormat, Date date) {
        boolean z;
        boolean z2;
        int i = 0;
        while (true) {
            z = true;
            if (i < 6) {
                if (jArr[i] == 0) {
                    i++;
                } else {
                    z2 = true;
                    break;
                }
            } else {
                z2 = false;
                break;
            }
        }
        if (!z2) {
            return;
        }
        for (int i2 = 0; i2 < 6; i2++) {
            if (jArr[i2] != 0) {
                printWriter.print(z ? str : str2);
                printWriter.print(UID_STATE_NAMES[i2]);
                printWriter.print(" = ");
                date.setTime(jArr[i2]);
                printWriter.print(simpleDateFormat.format(date));
                printWriter.print(" (");
                TimeUtils.formatDuration(jArr[i2] - j, printWriter);
                printWriter.println(")");
                z = false;
            }
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        int i;
        int appId;
        int i2;
        boolean z;
        Date date;
        boolean z2;
        boolean z3;
        SimpleDateFormat simpleDateFormat;
        int i3;
        long j;
        Date date2;
        SimpleDateFormat simpleDateFormat2;
        String str2;
        SimpleDateFormat simpleDateFormat3;
        boolean z4;
        boolean z5;
        int i4;
        Date date3;
        int i5;
        long j2;
        Ops ops;
        SimpleDateFormat simpleDateFormat4;
        String str3;
        String str4;
        Date date4;
        Date date5;
        int i6;
        int packageUid;
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            int i7 = 0;
            if (strArr != null) {
                String str5 = null;
                int i8 = 0;
                appId = -1;
                int iStrOpToOp = -1;
                int iStrModeToMode = -1;
                while (i8 < strArr.length) {
                    String str6 = strArr[i8];
                    if ("-h".equals(str6)) {
                        dumpHelp(printWriter);
                        return;
                    }
                    if (!"-a".equals(str6)) {
                        if ("--op".equals(str6)) {
                            i8++;
                            if (i8 >= strArr.length) {
                                printWriter.println("No argument for --op option");
                                return;
                            } else {
                                iStrOpToOp = Shell.strOpToOp(strArr[i8], printWriter);
                                if (iStrOpToOp < 0) {
                                    return;
                                }
                            }
                        } else if ("--package".equals(str6)) {
                            int i9 = i8 + 1;
                            if (i9 >= strArr.length) {
                                printWriter.println("No argument for --package option");
                                return;
                            }
                            String str7 = strArr[i9];
                            try {
                                packageUid = AppGlobals.getPackageManager().getPackageUid(str7, 12591104, 0);
                            } catch (RemoteException e) {
                                packageUid = appId;
                            }
                            if (packageUid < 0) {
                                printWriter.println("Unknown package: " + str7);
                                return;
                            }
                            appId = UserHandle.getAppId(packageUid);
                            i8 = i9;
                            str5 = str7;
                        } else {
                            if (!"--mode".equals(str6)) {
                                if (str6.length() <= 0 || str6.charAt(0) != '-') {
                                    printWriter.println("Unknown command: " + str6);
                                    return;
                                }
                                printWriter.println("Unknown option: " + str6);
                                return;
                            }
                            i8++;
                            if (i8 >= strArr.length) {
                                printWriter.println("No argument for --mode option");
                                return;
                            } else {
                                iStrModeToMode = Shell.strModeToMode(strArr[i8], printWriter);
                                if (iStrModeToMode < 0) {
                                    return;
                                }
                            }
                        }
                    }
                    i8++;
                }
                i = iStrOpToOp;
                i2 = iStrModeToMode;
                str = str5;
            } else {
                str = null;
                i = -1;
                appId = -1;
                i2 = -1;
            }
            synchronized (this) {
                printWriter.println("Current AppOps Service state:");
                this.mConstants.dump(printWriter);
                printWriter.println();
                long jCurrentTimeMillis = System.currentTimeMillis();
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                long jUptimeMillis = SystemClock.uptimeMillis();
                SimpleDateFormat simpleDateFormat5 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date date6 = new Date();
                if (i < 0 && i2 < 0 && str == null && this.mProfileOwners != null) {
                    printWriter.println("  Profile owners:");
                    for (int i10 = 0; i10 < this.mProfileOwners.size(); i10++) {
                        printWriter.print("    User #");
                        printWriter.print(this.mProfileOwners.keyAt(i10));
                        printWriter.print(": ");
                        UserHandle.formatUid(printWriter, this.mProfileOwners.valueAt(i10));
                        printWriter.println();
                    }
                    printWriter.println();
                }
                if (this.mOpModeWatchers.size() > 0) {
                    int i11 = 0;
                    boolean z6 = false;
                    z = false;
                    while (i11 < this.mOpModeWatchers.size()) {
                        if (i < 0 || i == this.mOpModeWatchers.keyAt(i11)) {
                            ArraySet<ModeCallback> arraySetValueAt = this.mOpModeWatchers.valueAt(i11);
                            boolean z7 = z;
                            int i12 = i7;
                            boolean z8 = z6;
                            for (int i13 = i12; i13 < arraySetValueAt.size(); i13++) {
                                ModeCallback modeCallbackValueAt = arraySetValueAt.valueAt(i13);
                                if (str == null || modeCallbackValueAt.mWatchingUid < 0 || appId == UserHandle.getAppId(modeCallbackValueAt.mWatchingUid)) {
                                    if (!z8) {
                                        printWriter.println("  Op mode watchers:");
                                        z8 = true;
                                    }
                                    if (i12 == 0) {
                                        printWriter.print("    Op ");
                                        printWriter.print(AppOpsManager.opToName(this.mOpModeWatchers.keyAt(i11)));
                                        printWriter.println(":");
                                        i12 = 1;
                                    }
                                    printWriter.print("      #");
                                    printWriter.print(i13);
                                    printWriter.print(": ");
                                    printWriter.println(modeCallbackValueAt);
                                    z7 = true;
                                }
                            }
                            z6 = z8;
                            z = z7;
                        }
                        i11++;
                        i7 = 0;
                    }
                } else {
                    z = false;
                }
                if (this.mPackageModeWatchers.size() > 0 && i < 0) {
                    boolean z9 = false;
                    for (int i14 = 0; i14 < this.mPackageModeWatchers.size(); i14++) {
                        if (str == null || str.equals(this.mPackageModeWatchers.keyAt(i14))) {
                            if (!z9) {
                                printWriter.println("  Package mode watchers:");
                                z9 = true;
                            }
                            printWriter.print("    Pkg ");
                            printWriter.print(this.mPackageModeWatchers.keyAt(i14));
                            printWriter.println(":");
                            ArraySet<ModeCallback> arraySetValueAt2 = this.mPackageModeWatchers.valueAt(i14);
                            for (int i15 = 0; i15 < arraySetValueAt2.size(); i15++) {
                                printWriter.print("      #");
                                printWriter.print(i15);
                                printWriter.print(": ");
                                printWriter.println(arraySetValueAt2.valueAt(i15));
                            }
                            z = true;
                        }
                    }
                }
                if (this.mModeWatchers.size() > 0 && i < 0) {
                    boolean z10 = false;
                    for (int i16 = 0; i16 < this.mModeWatchers.size(); i16++) {
                        ModeCallback modeCallbackValueAt2 = this.mModeWatchers.valueAt(i16);
                        if (str == null || modeCallbackValueAt2.mWatchingUid < 0 || appId == UserHandle.getAppId(modeCallbackValueAt2.mWatchingUid)) {
                            if (!z10) {
                                printWriter.println("  All op mode watchers:");
                                z10 = true;
                            }
                            printWriter.print("    ");
                            printWriter.print(Integer.toHexString(System.identityHashCode(this.mModeWatchers.keyAt(i16))));
                            printWriter.print(": ");
                            printWriter.println(modeCallbackValueAt2);
                            z = true;
                        }
                    }
                }
                boolean z11 = z;
                if (this.mActiveWatchers.size() > 0 && i2 < 0) {
                    int i17 = 0;
                    boolean z12 = false;
                    while (i17 < this.mActiveWatchers.size()) {
                        SparseArray<ActiveCallback> sparseArrayValueAt = this.mActiveWatchers.valueAt(i17);
                        if (sparseArrayValueAt.size() > 0) {
                            ActiveCallback activeCallbackValueAt = sparseArrayValueAt.valueAt(0);
                            if ((i < 0 || sparseArrayValueAt.indexOfKey(i) >= 0) && (str == null || activeCallbackValueAt.mWatchingUid < 0 || appId == UserHandle.getAppId(activeCallbackValueAt.mWatchingUid))) {
                                if (!z12) {
                                    printWriter.println("  All op active watchers:");
                                    z12 = true;
                                }
                                printWriter.print("    ");
                                printWriter.print(Integer.toHexString(System.identityHashCode(this.mActiveWatchers.keyAt(i17))));
                                printWriter.println(" ->");
                                printWriter.print("        [");
                                int size = sparseArrayValueAt.size();
                                i6 = 0;
                                while (i6 < size) {
                                    if (i6 > 0) {
                                        printWriter.print(' ');
                                    }
                                    printWriter.print(AppOpsManager.opToName(sparseArrayValueAt.keyAt(i6)));
                                    if (i6 < size - 1) {
                                        printWriter.print(',');
                                    }
                                    i6++;
                                }
                                printWriter.println("]");
                                printWriter.print("        ");
                                printWriter.println(activeCallbackValueAt);
                            }
                            i17 = i6 + 1;
                        }
                        i6 = i17;
                        i17 = i6 + 1;
                    }
                    z11 = true;
                }
                if (this.mClients.size() <= 0 || i2 >= 0) {
                    date = date6;
                } else {
                    int i18 = 0;
                    boolean z13 = false;
                    while (i18 < this.mClients.size()) {
                        ClientState clientStateValueAt = this.mClients.valueAt(i18);
                        if (clientStateValueAt.mStartedOps.size() > 0) {
                            boolean z14 = z13;
                            int i19 = 0;
                            boolean z15 = false;
                            boolean z16 = false;
                            while (i19 < clientStateValueAt.mStartedOps.size()) {
                                Op op = clientStateValueAt.mStartedOps.get(i19);
                                if (i >= 0) {
                                    date5 = date6;
                                    if (op.op != i) {
                                    }
                                    i19++;
                                    date6 = date5;
                                } else {
                                    date5 = date6;
                                }
                                if (str == null || str.equals(op.packageName)) {
                                    if (!z14) {
                                        printWriter.println("  Clients:");
                                        z14 = true;
                                    }
                                    if (!z15) {
                                        printWriter.print("    ");
                                        printWriter.print(this.mClients.keyAt(i18));
                                        printWriter.println(":");
                                        printWriter.print("      ");
                                        printWriter.println(clientStateValueAt);
                                        z15 = true;
                                    }
                                    if (!z16) {
                                        printWriter.println("      Started ops:");
                                        z16 = true;
                                    }
                                    printWriter.print("        ");
                                    printWriter.print("uid=");
                                    printWriter.print(op.uid);
                                    printWriter.print(" pkg=");
                                    printWriter.print(op.packageName);
                                    printWriter.print(" op=");
                                    printWriter.println(AppOpsManager.opToName(op.op));
                                }
                                i19++;
                                date6 = date5;
                            }
                            date4 = date6;
                            z13 = z14;
                        } else {
                            date4 = date6;
                        }
                        i18++;
                        date6 = date4;
                    }
                    date = date6;
                    z11 = true;
                }
                if (this.mAudioRestrictions.size() > 0 && i < 0 && str != null && i2 < 0) {
                    int i20 = 0;
                    boolean z17 = false;
                    while (i20 < this.mAudioRestrictions.size()) {
                        String strOpToName = AppOpsManager.opToName(this.mAudioRestrictions.keyAt(i20));
                        SparseArray<Restriction> sparseArrayValueAt2 = this.mAudioRestrictions.valueAt(i20);
                        boolean z18 = z17;
                        int i21 = 0;
                        while (i21 < sparseArrayValueAt2.size()) {
                            if (!z18) {
                                printWriter.println("  Audio Restrictions:");
                                z18 = true;
                                z11 = true;
                            }
                            int iKeyAt = sparseArrayValueAt2.keyAt(i21);
                            printWriter.print("    ");
                            printWriter.print(strOpToName);
                            printWriter.print(" usage=");
                            printWriter.print(AudioAttributes.usageToString(iKeyAt));
                            Restriction restrictionValueAt = sparseArrayValueAt2.valueAt(i21);
                            printWriter.print(": mode=");
                            printWriter.println(AppOpsManager.modeToName(restrictionValueAt.mode));
                            if (restrictionValueAt.exceptionPackages.isEmpty()) {
                                str4 = strOpToName;
                            } else {
                                printWriter.println("      Exceptions:");
                                int i22 = 0;
                                while (true) {
                                    str4 = strOpToName;
                                    if (i22 < restrictionValueAt.exceptionPackages.size()) {
                                        printWriter.print("        ");
                                        printWriter.println(restrictionValueAt.exceptionPackages.valueAt(i22));
                                        i22++;
                                        strOpToName = str4;
                                    }
                                }
                            }
                            i21++;
                            strOpToName = str4;
                        }
                        i20++;
                        z17 = z18;
                    }
                }
                if (z11) {
                    printWriter.println();
                }
                int i23 = 0;
                while (i23 < this.mUidStates.size()) {
                    UidState uidStateValueAt = this.mUidStates.valueAt(i23);
                    SparseIntArray sparseIntArray = uidStateValueAt.opModes;
                    ArrayMap<String, Ops> arrayMap = uidStateValueAt.pkgOps;
                    if (i >= 0 || str != null || i2 >= 0) {
                        boolean z19 = i < 0 || (uidStateValueAt.opModes != null && uidStateValueAt.opModes.indexOfKey(i) >= 0);
                        boolean z20 = str == null;
                        boolean z21 = i2 < 0;
                        if (z21 || sparseIntArray == null) {
                            z2 = z19;
                        } else {
                            z2 = z19;
                            int i24 = 0;
                            while (!z21) {
                                z3 = z20;
                                if (i24 >= sparseIntArray.size()) {
                                    break;
                                }
                                if (sparseIntArray.valueAt(i24) == i2) {
                                    z21 = true;
                                }
                                i24++;
                                z20 = z3;
                            }
                        }
                        z3 = z20;
                        if (arrayMap != null) {
                            int i25 = 0;
                            while (true) {
                                if ((z2 && z3 && z21) || i25 >= arrayMap.size()) {
                                    break;
                                }
                                Ops opsValueAt = arrayMap.valueAt(i25);
                                if (!z2 && opsValueAt != null && opsValueAt.indexOfKey(i) >= 0) {
                                    z2 = true;
                                }
                                if (z21) {
                                    simpleDateFormat3 = simpleDateFormat5;
                                } else {
                                    simpleDateFormat3 = simpleDateFormat5;
                                    int i26 = 0;
                                    while (true) {
                                        if (z21) {
                                            z4 = z21;
                                            break;
                                        }
                                        z4 = z21;
                                        if (i26 >= opsValueAt.size()) {
                                            break;
                                        }
                                        z21 = opsValueAt.valueAt(i26).mode == i2 ? true : z4;
                                        i26++;
                                    }
                                    z21 = z4;
                                }
                                if (!z3 && str.equals(opsValueAt.packageName)) {
                                    z3 = true;
                                }
                                i25++;
                                simpleDateFormat5 = simpleDateFormat3;
                            }
                        }
                        simpleDateFormat = simpleDateFormat5;
                        if (uidStateValueAt.foregroundOps != null && !z2 && uidStateValueAt.foregroundOps.indexOfKey(i) > 0) {
                            z2 = true;
                        }
                        if (!z2 || !z3 || !z21) {
                            i3 = i;
                            j = jUptimeMillis;
                            date2 = date;
                            simpleDateFormat2 = simpleDateFormat;
                            str2 = str;
                        }
                        i23++;
                        date = date2;
                        simpleDateFormat5 = simpleDateFormat2;
                        jUptimeMillis = j;
                        str = str2;
                        i = i3;
                    } else {
                        simpleDateFormat = simpleDateFormat5;
                    }
                    printWriter.print("  Uid ");
                    UserHandle.formatUid(printWriter, uidStateValueAt.uid);
                    printWriter.println(":");
                    printWriter.print("    state=");
                    printWriter.println(UID_STATE_NAMES[uidStateValueAt.state]);
                    if (uidStateValueAt.state != uidStateValueAt.pendingState) {
                        printWriter.print("    pendingState=");
                        printWriter.println(UID_STATE_NAMES[uidStateValueAt.pendingState]);
                    }
                    if (uidStateValueAt.pendingStateCommitTime != 0) {
                        printWriter.print("    pendingStateCommitTime=");
                        TimeUtils.formatDuration(uidStateValueAt.pendingStateCommitTime, jUptimeMillis, printWriter);
                        printWriter.println();
                    }
                    if (uidStateValueAt.startNesting != 0) {
                        printWriter.print("    startNesting=");
                        printWriter.println(uidStateValueAt.startNesting);
                    }
                    if (uidStateValueAt.foregroundOps != null && (i2 < 0 || i2 == 4)) {
                        printWriter.println("    foregroundOps:");
                        for (int i27 = 0; i27 < uidStateValueAt.foregroundOps.size(); i27++) {
                            if (i < 0 || i == uidStateValueAt.foregroundOps.keyAt(i27)) {
                                printWriter.print("      ");
                                printWriter.print(AppOpsManager.opToName(uidStateValueAt.foregroundOps.keyAt(i27)));
                                printWriter.print(": ");
                                printWriter.println(uidStateValueAt.foregroundOps.valueAt(i27) ? "WATCHER" : "SILENT");
                            }
                        }
                        printWriter.print("    hasForegroundWatchers=");
                        printWriter.println(uidStateValueAt.hasForegroundWatchers);
                    }
                    if (sparseIntArray != null) {
                        int size2 = sparseIntArray.size();
                        for (int i28 = 0; i28 < size2; i28++) {
                            int iKeyAt2 = sparseIntArray.keyAt(i28);
                            int iValueAt = sparseIntArray.valueAt(i28);
                            if ((i < 0 || i == iKeyAt2) && (i2 < 0 || i2 == iValueAt)) {
                                printWriter.print("      ");
                                printWriter.print(AppOpsManager.opToName(iKeyAt2));
                                printWriter.print(": mode=");
                                printWriter.println(AppOpsManager.modeToName(iValueAt));
                            }
                        }
                    }
                    if (arrayMap != null) {
                        int i29 = 0;
                        while (i29 < arrayMap.size()) {
                            Ops opsValueAt2 = arrayMap.valueAt(i29);
                            if (str == null || str.equals(opsValueAt2.packageName)) {
                                boolean z22 = false;
                                int i30 = 0;
                                while (i30 < opsValueAt2.size()) {
                                    Op opValueAt = opsValueAt2.valueAt(i30);
                                    if ((i < 0 || i == opValueAt.op) && (i2 < 0 || i2 == opValueAt.mode)) {
                                        if (z22) {
                                            z5 = z22;
                                        } else {
                                            printWriter.print("    Package ");
                                            printWriter.print(opsValueAt2.packageName);
                                            printWriter.println(":");
                                            z5 = true;
                                        }
                                        printWriter.print("      ");
                                        printWriter.print(AppOpsManager.opToName(opValueAt.op));
                                        printWriter.print(" (");
                                        printWriter.print(AppOpsManager.modeToName(opValueAt.mode));
                                        int iOpToSwitch = AppOpsManager.opToSwitch(opValueAt.op);
                                        if (iOpToSwitch != opValueAt.op) {
                                            printWriter.print(" / switch ");
                                            printWriter.print(AppOpsManager.opToName(iOpToSwitch));
                                            Op op2 = opsValueAt2.get(iOpToSwitch);
                                            int iOpToDefaultMode = op2 != null ? op2.mode : AppOpsManager.opToDefaultMode(iOpToSwitch);
                                            printWriter.print("=");
                                            printWriter.print(AppOpsManager.modeToName(iOpToDefaultMode));
                                        }
                                        printWriter.println("): ");
                                        i4 = i;
                                        date3 = date;
                                        i5 = i30;
                                        j2 = jUptimeMillis;
                                        ops = opsValueAt2;
                                        simpleDateFormat4 = simpleDateFormat;
                                        str3 = str;
                                        dumpTimesLocked(printWriter, "          Access: ", "                  ", opValueAt.time, jCurrentTimeMillis, simpleDateFormat4, date3);
                                        dumpTimesLocked(printWriter, "          Reject: ", "                  ", opValueAt.rejectTime, jCurrentTimeMillis, simpleDateFormat4, date3);
                                        if (opValueAt.duration == -1) {
                                            printWriter.print("          Running start at: ");
                                            TimeUtils.formatDuration(jElapsedRealtime - opValueAt.startRealtime, printWriter);
                                            printWriter.println();
                                        } else if (opValueAt.duration != 0) {
                                            printWriter.print("          duration=");
                                            TimeUtils.formatDuration(opValueAt.duration, printWriter);
                                            printWriter.println();
                                        }
                                        if (opValueAt.startNesting != 0) {
                                            printWriter.print("          startNesting=");
                                            printWriter.println(opValueAt.startNesting);
                                        }
                                        z22 = z5;
                                    } else {
                                        i4 = i;
                                        j2 = jUptimeMillis;
                                        ops = opsValueAt2;
                                        date3 = date;
                                        simpleDateFormat4 = simpleDateFormat;
                                        i5 = i30;
                                        str3 = str;
                                    }
                                    i30 = i5 + 1;
                                    date = date3;
                                    jUptimeMillis = j2;
                                    opsValueAt2 = ops;
                                    str = str3;
                                    i = i4;
                                    simpleDateFormat = simpleDateFormat4;
                                }
                            }
                            i29++;
                            date = date;
                            jUptimeMillis = jUptimeMillis;
                            str = str;
                            i = i;
                            simpleDateFormat = simpleDateFormat;
                        }
                    }
                    i3 = i;
                    j = jUptimeMillis;
                    date2 = date;
                    simpleDateFormat2 = simpleDateFormat;
                    str2 = str;
                    z11 = true;
                    i23++;
                    date = date2;
                    simpleDateFormat5 = simpleDateFormat2;
                    jUptimeMillis = j;
                    str = str2;
                    i = i3;
                }
                if (z11) {
                    printWriter.println();
                }
                int size3 = this.mOpUserRestrictions.size();
                for (int i31 = 0; i31 < size3; i31++) {
                    IBinder iBinderKeyAt = this.mOpUserRestrictions.keyAt(i31);
                    ClientRestrictionState clientRestrictionStateValueAt = this.mOpUserRestrictions.valueAt(i31);
                    printWriter.println("  User restrictions for token " + iBinderKeyAt + ":");
                    int size4 = clientRestrictionStateValueAt.perUserRestrictions != null ? clientRestrictionStateValueAt.perUserRestrictions.size() : 0;
                    if (size4 > 0) {
                        printWriter.println("      Restricted ops:");
                        for (int i32 = 0; i32 < size4; i32++) {
                            int iKeyAt3 = clientRestrictionStateValueAt.perUserRestrictions.keyAt(i32);
                            boolean[] zArrValueAt = clientRestrictionStateValueAt.perUserRestrictions.valueAt(i32);
                            if (zArrValueAt != null) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("[");
                                int length = zArrValueAt.length;
                                for (int i33 = 0; i33 < length; i33++) {
                                    if (zArrValueAt[i33]) {
                                        if (sb.length() > 1) {
                                            sb.append(", ");
                                        }
                                        sb.append(AppOpsManager.opToName(i33));
                                    }
                                }
                                sb.append("]");
                                printWriter.print("        ");
                                printWriter.print("user: ");
                                printWriter.print(iKeyAt3);
                                printWriter.print(" restricted ops: ");
                                printWriter.println(sb);
                            }
                        }
                    }
                    int size5 = clientRestrictionStateValueAt.perUserExcludedPackages != null ? clientRestrictionStateValueAt.perUserExcludedPackages.size() : 0;
                    if (size5 > 0) {
                        printWriter.println("      Excluded packages:");
                        for (int i34 = 0; i34 < size5; i34++) {
                            int iKeyAt4 = clientRestrictionStateValueAt.perUserExcludedPackages.keyAt(i34);
                            String[] strArrValueAt = clientRestrictionStateValueAt.perUserExcludedPackages.valueAt(i34);
                            printWriter.print("        ");
                            printWriter.print("user: ");
                            printWriter.print(iKeyAt4);
                            printWriter.print(" packages: ");
                            printWriter.println(Arrays.toString(strArrValueAt));
                        }
                    }
                }
            }
        }
    }

    private static final class Restriction {
        private static final ArraySet<String> NO_EXCEPTIONS = new ArraySet<>();
        ArraySet<String> exceptionPackages;
        int mode;

        private Restriction() {
            this.exceptionPackages = NO_EXCEPTIONS;
        }
    }

    public void setUserRestrictions(Bundle bundle, IBinder iBinder, int i) {
        checkSystemUid("setUserRestrictions");
        Preconditions.checkNotNull(bundle);
        Preconditions.checkNotNull(iBinder);
        int opNum = CTA_MANAGER.isCtaSupported() ? CTA_MANAGER.getOpNum() : 78;
        for (int i2 = 0; i2 < opNum; i2++) {
            String strOpToRestriction = AppOpsManager.opToRestriction(i2);
            if (strOpToRestriction != null) {
                setUserRestrictionNoCheck(i2, bundle.getBoolean(strOpToRestriction, false), iBinder, i, null);
            }
        }
    }

    public void setUserRestriction(int i, boolean z, IBinder iBinder, int i2, String[] strArr) {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.MANAGE_APP_OPS_RESTRICTIONS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
        if (i2 != UserHandle.getCallingUserId() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") != 0) {
            throw new SecurityException("Need INTERACT_ACROSS_USERS_FULL or INTERACT_ACROSS_USERS to interact cross user ");
        }
        verifyIncomingOp(i);
        Preconditions.checkNotNull(iBinder);
        setUserRestrictionNoCheck(i, z, iBinder, i2, strArr);
    }

    private void setUserRestrictionNoCheck(int i, boolean z, IBinder iBinder, int i2, String[] strArr) {
        synchronized (this) {
            ClientRestrictionState clientRestrictionState = this.mOpUserRestrictions.get(iBinder);
            if (clientRestrictionState == null) {
                try {
                    clientRestrictionState = new ClientRestrictionState(iBinder);
                    this.mOpUserRestrictions.put(iBinder, clientRestrictionState);
                } catch (RemoteException e) {
                    return;
                }
            }
            if (clientRestrictionState.setRestriction(i, z, strArr, i2)) {
                this.mHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo.INSTANCE, this, Integer.valueOf(i), -2));
            }
            if (clientRestrictionState.isDefault()) {
                this.mOpUserRestrictions.remove(iBinder);
                clientRestrictionState.destroy();
            }
        }
    }

    private void notifyWatchersOfChange(int i, int i2) {
        synchronized (this) {
            ArraySet<ModeCallback> arraySet = this.mOpModeWatchers.get(i);
            if (arraySet == null) {
                return;
            }
            notifyOpChanged(new ArraySet<>((ArraySet) arraySet), i, i2, (String) null);
        }
    }

    public void removeUser(int i) throws RemoteException {
        checkSystemUid("removeUser");
        synchronized (this) {
            for (int size = this.mOpUserRestrictions.size() - 1; size >= 0; size--) {
                this.mOpUserRestrictions.valueAt(size).removeUser(i);
            }
            removeUidsForUserLocked(i);
        }
    }

    public boolean isOperationActive(int i, int i2, String str) {
        if (Binder.getCallingUid() != i2 && this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") != 0) {
            return false;
        }
        verifyIncomingOp(i);
        if (resolvePackageName(i2, str) == null) {
            return false;
        }
        synchronized (this) {
            for (int size = this.mClients.size() - 1; size >= 0; size--) {
                ClientState clientStateValueAt = this.mClients.valueAt(size);
                for (int size2 = clientStateValueAt.mStartedOps.size() - 1; size2 >= 0; size2--) {
                    Op op = clientStateValueAt.mStartedOps.get(size2);
                    if (op.op == i && op.uid == i2) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void removeUidsForUserLocked(int i) {
        for (int size = this.mUidStates.size() - 1; size >= 0; size--) {
            if (UserHandle.getUserId(this.mUidStates.keyAt(size)) == i) {
                this.mUidStates.removeAt(size);
            }
        }
    }

    private void checkSystemUid(String str) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException(str + " must by called by the system");
        }
    }

    private static String resolvePackageName(int i, String str) {
        if (i == 0) {
            return "root";
        }
        if (i == 2000) {
            return "com.android.shell";
        }
        if (i == 1013) {
            return "media";
        }
        if (i == 1041) {
            return "audioserver";
        }
        if (i == 1047) {
            return "cameraserver";
        }
        if (i == 1000 && str == null) {
            return PackageManagerService.PLATFORM_PACKAGE_NAME;
        }
        return str;
    }

    private static int resolveUid(String str) {
        if (str == null) {
            return -1;
        }
        switch (str) {
        }
        return -1;
    }

    private static String[] getPackagesForUid(int i) {
        String[] packagesForUid;
        try {
            packagesForUid = AppGlobals.getPackageManager().getPackagesForUid(i);
        } catch (RemoteException e) {
            packagesForUid = null;
        }
        if (packagesForUid == null) {
            return EmptyArray.STRING;
        }
        return packagesForUid;
    }

    private final class ClientRestrictionState implements IBinder.DeathRecipient {
        SparseArray<String[]> perUserExcludedPackages;
        SparseArray<boolean[]> perUserRestrictions;
        private final IBinder token;

        public ClientRestrictionState(IBinder iBinder) throws RemoteException {
            iBinder.linkToDeath(this, 0);
            this.token = iBinder;
        }

        public boolean setRestriction(int i, boolean z, String[] strArr, int i2) {
            int[] iArr;
            if (this.perUserRestrictions == null && z) {
                this.perUserRestrictions = new SparseArray<>();
            }
            if (i2 == -1) {
                List users = UserManager.get(AppOpsService.this.mContext).getUsers(false);
                iArr = new int[users.size()];
                for (int i3 = 0; i3 < users.size(); i3++) {
                    iArr[i3] = ((UserInfo) users.get(i3)).id;
                }
            } else {
                iArr = new int[]{i2};
            }
            if (this.perUserRestrictions == null) {
                return false;
            }
            boolean z2 = false;
            for (int i4 : iArr) {
                boolean[] zArr = this.perUserRestrictions.get(i4);
                if (zArr == null && z) {
                    if (AppOpsService.CTA_MANAGER.isCtaSupported()) {
                        zArr = new boolean[AppOpsService.CTA_MANAGER.getOpNum()];
                    } else {
                        zArr = new boolean[78];
                    }
                    this.perUserRestrictions.put(i4, zArr);
                }
                if (zArr != null && zArr[i] != z) {
                    zArr[i] = z;
                    if (!z && isDefault(zArr)) {
                        this.perUserRestrictions.remove(i4);
                        zArr = null;
                    }
                    z2 = true;
                }
                if (zArr != null) {
                    boolean zIsEmpty = ArrayUtils.isEmpty(strArr);
                    if (this.perUserExcludedPackages == null && !zIsEmpty) {
                        this.perUserExcludedPackages = new SparseArray<>();
                    }
                    if (this.perUserExcludedPackages != null && !Arrays.equals(strArr, this.perUserExcludedPackages.get(i4))) {
                        if (zIsEmpty) {
                            this.perUserExcludedPackages.remove(i4);
                            if (this.perUserExcludedPackages.size() <= 0) {
                                this.perUserExcludedPackages = null;
                            }
                        } else {
                            this.perUserExcludedPackages.put(i4, strArr);
                        }
                        z2 = true;
                    }
                }
            }
            return z2;
        }

        public boolean hasRestriction(int i, String str, int i2) {
            boolean[] zArr;
            String[] strArr;
            if (this.perUserRestrictions == null || (zArr = this.perUserRestrictions.get(i2)) == null || !zArr[i]) {
                return false;
            }
            if (this.perUserExcludedPackages == null || (strArr = this.perUserExcludedPackages.get(i2)) == null) {
                return true;
            }
            return !ArrayUtils.contains(strArr, str);
        }

        public void removeUser(int i) {
            if (this.perUserExcludedPackages != null) {
                this.perUserExcludedPackages.remove(i);
                if (this.perUserExcludedPackages.size() <= 0) {
                    this.perUserExcludedPackages = null;
                }
            }
            if (this.perUserRestrictions != null) {
                this.perUserRestrictions.remove(i);
                if (this.perUserRestrictions.size() <= 0) {
                    this.perUserRestrictions = null;
                }
            }
        }

        public boolean isDefault() {
            return this.perUserRestrictions == null || this.perUserRestrictions.size() <= 0;
        }

        @Override
        public void binderDied() {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mOpUserRestrictions.remove(this.token);
                if (this.perUserRestrictions == null) {
                    return;
                }
                int size = this.perUserRestrictions.size();
                for (int i = 0; i < size; i++) {
                    boolean[] zArrValueAt = this.perUserRestrictions.valueAt(i);
                    int length = zArrValueAt.length;
                    for (final int i2 = 0; i2 < length; i2++) {
                        if (zArrValueAt[i2]) {
                            AppOpsService.this.mHandler.post(new Runnable() {
                                @Override
                                public final void run() {
                                    AppOpsService.this.notifyWatchersOfChange(i2, -2);
                                }
                            });
                        }
                    }
                }
                destroy();
            }
        }

        public void destroy() {
            this.token.unlinkToDeath(this, 0);
        }

        private boolean isDefault(boolean[] zArr) {
            if (ArrayUtils.isEmpty(zArr)) {
                return true;
            }
            for (boolean z : zArr) {
                if (z) {
                    return false;
                }
            }
            return true;
        }
    }

    private final class AppOpsManagerInternalImpl extends AppOpsManagerInternal {
        private AppOpsManagerInternalImpl() {
        }

        public void setDeviceAndProfileOwners(SparseIntArray sparseIntArray) {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mProfileOwners = sparseIntArray;
            }
        }
    }
}
