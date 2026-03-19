package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.storage.MountServiceInternal;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.mediatek.appworkingset.AWSDBHelper;
import com.mediatek.cta.CtaUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AppOpsService extends IAppOpsService.Stub {
    static final boolean DEBUG = true;
    static final boolean DEBUG_WRITEFILE = false;
    static final boolean ENG_LOAD = "eng".equals(Build.TYPE);
    static final String TAG = "AppOps";
    static final long WRITE_DELAY = 1800000;
    Context mContext;
    boolean mFastWriteScheduled;
    final AtomicFile mFile;
    final Handler mHandler;
    boolean mWriteScheduled;
    final Runnable mWriteRunner = new Runnable() {
        @Override
        public void run() {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mWriteScheduled = false;
                AppOpsService.this.mFastWriteScheduled = false;
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        AppOpsService.this.writeState();
                        return null;
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            }
        }
    };
    private final SparseArray<UidState> mUidStates = new SparseArray<>();
    private final ArrayMap<IBinder, ClientRestrictionState> mOpUserRestrictions = new ArrayMap<>();
    final SparseArray<ArrayList<Callback>> mOpModeWatchers = new SparseArray<>();
    final ArrayMap<String, ArrayList<Callback>> mPackageModeWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, Callback> mModeWatchers = new ArrayMap<>();
    final SparseArray<SparseArray<Restriction>> mAudioRestrictions = new SparseArray<>();
    final ArrayMap<IBinder, ClientState> mClients = new ArrayMap<>();

    private static final class UidState {
        public SparseIntArray opModes;
        public ArrayMap<String, Ops> pkgOps;
        public final int uid;

        public UidState(int uid) {
            this.uid = uid;
        }

        public void clear() {
            this.pkgOps = null;
            this.opModes = null;
        }

        public boolean isDefault() {
            if (this.pkgOps == null || this.pkgOps.isEmpty()) {
                return this.opModes == null || this.opModes.size() <= 0;
            }
            return false;
        }
    }

    public static final class Ops extends SparseArray<Op> {
        public final boolean isPrivileged;
        public final String packageName;
        public final UidState uidState;

        public Ops(String _packageName, UidState _uidState, boolean _isPrivileged) {
            this.packageName = _packageName;
            this.uidState = _uidState;
            this.isPrivileged = _isPrivileged;
        }
    }

    public static final class Op {
        public int duration;
        public int mode;
        public int nesting;
        public final int op;
        public final String packageName;
        public String proxyPackageName;
        public int proxyUid = -1;
        public long rejectTime;
        public long time;
        public final int uid;

        public Op(int _uid, String _packageName, int _op) {
            this.uid = _uid;
            this.packageName = _packageName;
            this.op = _op;
            this.mode = AppOpsManager.opToDefaultMode(this.op);
        }
    }

    public final class Callback implements IBinder.DeathRecipient {
        final IAppOpsCallback mCallback;

        public Callback(IAppOpsCallback callback) {
            this.mCallback = callback;
            try {
                this.mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public void unlinkToDeath() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            AppOpsService.this.stopWatchingMode(this.mCallback);
        }
    }

    public final class ClientState extends Binder implements IBinder.DeathRecipient {
        final IBinder mAppToken;
        final int mPid = Binder.getCallingPid();
        final ArrayList<Op> mStartedOps;

        public ClientState(IBinder appToken) {
            this.mAppToken = appToken;
            if (appToken instanceof Binder) {
                this.mStartedOps = null;
                return;
            }
            this.mStartedOps = new ArrayList<>();
            try {
                this.mAppToken.linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            return "ClientState{mAppToken=" + this.mAppToken + ", " + (this.mStartedOps != null ? "pid=" + this.mPid : "local") + '}';
        }

        @Override
        public void binderDied() {
            synchronized (AppOpsService.this) {
                for (int i = this.mStartedOps.size() - 1; i >= 0; i--) {
                    AppOpsService.this.finishOperationLocked(this.mStartedOps.get(i));
                }
                AppOpsService.this.mClients.remove(this.mAppToken);
            }
        }
    }

    public AppOpsService(File storagePath, Handler handler) {
        this.mFile = new AtomicFile(storagePath);
        this.mHandler = handler;
        readState();
    }

    public void publish(Context context) {
        this.mContext = context;
        ServiceManager.addService("appops", asBinder());
    }

    public void systemReady() {
        synchronized (this) {
            boolean changed = false;
            for (int i = this.mUidStates.size() - 1; i >= 0; i--) {
                UidState uidState = this.mUidStates.valueAt(i);
                String[] packageNames = getPackagesForUid(uidState.uid);
                if (ArrayUtils.isEmpty(packageNames)) {
                    uidState.clear();
                    this.mUidStates.removeAt(i);
                    changed = true;
                } else {
                    ArrayMap<String, Ops> pkgs = uidState.pkgOps;
                    if (pkgs != null) {
                        Iterator<Ops> it = pkgs.values().iterator();
                        while (it.hasNext()) {
                            Ops ops = it.next();
                            int curUid = -1;
                            try {
                                curUid = AppGlobals.getPackageManager().getPackageUid(ops.packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, UserHandle.getUserId(ops.uidState.uid));
                            } catch (RemoteException e) {
                            }
                            if (curUid != ops.uidState.uid) {
                                Slog.i(TAG, "Pruning old package " + ops.packageName + "/" + ops.uidState + ": new uid=" + curUid);
                                it.remove();
                                changed = true;
                            }
                        }
                        if (uidState.isDefault()) {
                            this.mUidStates.removeAt(i);
                        }
                    }
                }
            }
            if (changed) {
                scheduleFastWriteLocked();
            }
        }
        MountServiceInternal mountServiceInternal = (MountServiceInternal) LocalServices.getService(MountServiceInternal.class);
        mountServiceInternal.addExternalStoragePolicy(new MountServiceInternal.ExternalStorageMountPolicy() {
            public int getMountMode(int uid, String packageName) {
                if (Process.isIsolated(uid) || AppOpsService.this.noteOperation(59, uid, packageName) != 0) {
                    return 0;
                }
                if (AppOpsService.this.noteOperation(60, uid, packageName) != 0) {
                    return 2;
                }
                return 3;
            }

            public boolean hasExternalStorage(int uid, String packageName) {
                int mountMode = getMountMode(uid, packageName);
                return mountMode == 2 || mountMode == 3;
            }
        });
    }

    public void packageRemoved(int uid, String packageName) {
        synchronized (this) {
            UidState uidState = this.mUidStates.get(uid);
            if (uidState == null) {
                return;
            }
            boolean changed = false;
            if (uidState.pkgOps != null && uidState.pkgOps.remove(packageName) != null) {
                changed = true;
            }
            if (changed && uidState.pkgOps.isEmpty() && getPackagesForUid(uid).length <= 0) {
                this.mUidStates.remove(uid);
            }
            if (changed) {
                scheduleFastWriteLocked();
            }
        }
    }

    public void uidRemoved(int uid) {
        synchronized (this) {
            if (this.mUidStates.indexOfKey(uid) >= 0) {
                this.mUidStates.remove(uid);
                scheduleFastWriteLocked();
            }
        }
    }

    public void shutdown() {
        Slog.w(TAG, "Writing app ops before shutdown...");
        boolean doWrite = false;
        synchronized (this) {
            if (this.mWriteScheduled) {
                this.mWriteScheduled = false;
                doWrite = true;
            }
        }
        if (!doWrite) {
            return;
        }
        writeState();
    }

    private ArrayList<AppOpsManager.OpEntry> collectOps(Ops pkgOps, int[] ops) {
        ArrayList<AppOpsManager.OpEntry> resOps = null;
        if (ops == null) {
            resOps = new ArrayList<>();
            for (int j = 0; j < pkgOps.size(); j++) {
                Op curOp = pkgOps.valueAt(j);
                resOps.add(new AppOpsManager.OpEntry(curOp.op, curOp.mode, curOp.time, curOp.rejectTime, curOp.duration, curOp.proxyUid, curOp.proxyPackageName));
            }
        } else {
            for (int i : ops) {
                Op curOp2 = pkgOps.get(i);
                if (curOp2 != null) {
                    if (resOps == null) {
                        resOps = new ArrayList<>();
                    }
                    resOps.add(new AppOpsManager.OpEntry(curOp2.op, curOp2.mode, curOp2.time, curOp2.rejectTime, curOp2.duration, curOp2.proxyUid, curOp2.proxyPackageName));
                }
            }
        }
        return resOps;
    }

    public List<AppOpsManager.PackageOps> getPackagesForOps(int[] ops) throws Throwable {
        ArrayList<AppOpsManager.PackageOps> res;
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        ArrayList<AppOpsManager.PackageOps> res2 = null;
        synchronized (this) {
            try {
                int uidStateCount = this.mUidStates.size();
                for (int i = 0; i < uidStateCount; i++) {
                    UidState uidState = this.mUidStates.valueAt(i);
                    if (uidState.pkgOps != null && !uidState.pkgOps.isEmpty()) {
                        ArrayMap<String, Ops> packages = uidState.pkgOps;
                        int packageCount = packages.size();
                        int j = 0;
                        ArrayList<AppOpsManager.PackageOps> res3 = res2;
                        while (j < packageCount) {
                            try {
                                Ops pkgOps = packages.valueAt(j);
                                ArrayList<AppOpsManager.OpEntry> resOps = collectOps(pkgOps, ops);
                                if (resOps != null) {
                                    res = res3 == null ? new ArrayList<>() : res3;
                                    AppOpsManager.PackageOps resPackage = new AppOpsManager.PackageOps(pkgOps.packageName, pkgOps.uidState.uid, resOps);
                                    res.add(resPackage);
                                } else {
                                    res = res3;
                                }
                                j++;
                                res3 = res;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        res2 = res3;
                    }
                }
                return res2;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public List<AppOpsManager.PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) {
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return Collections.emptyList();
        }
        synchronized (this) {
            Ops pkgOps = getOpsRawLocked(uid, resolvedPackageName, false);
            if (pkgOps == null) {
                return null;
            }
            ArrayList<AppOpsManager.OpEntry> resOps = collectOps(pkgOps, ops);
            if (resOps == null) {
                return null;
            }
            ArrayList<AppOpsManager.PackageOps> res = new ArrayList<>();
            AppOpsManager.PackageOps resPackage = new AppOpsManager.PackageOps(pkgOps.packageName, pkgOps.uidState.uid, resOps);
            res.add(resPackage);
            return res;
        }
    }

    private void pruneOp(Op op, int uid, String packageName) {
        Ops ops;
        UidState uidState;
        ArrayMap<String, Ops> pkgOps;
        if (op.time != 0 || op.rejectTime != 0 || (ops = getOpsRawLocked(uid, packageName, false)) == null) {
            return;
        }
        ops.remove(op.op);
        if (ops.size() > 0 || (pkgOps = (uidState = ops.uidState).pkgOps) == null) {
            return;
        }
        pkgOps.remove(ops.packageName);
        if (pkgOps.isEmpty()) {
            uidState.pkgOps = null;
        }
        if (!uidState.isDefault()) {
            return;
        }
        this.mUidStates.remove(uid);
    }

    public void setUidMode(int code, int uid, int mode) throws Throwable {
        int i;
        int length;
        ArrayMap<Callback, ArraySet<String>> callbackSpecs;
        String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        Log.d(TAG, "setUidMode() - code = " + code + " target uid = " + uid + " mode = " + mode + " requested from pid = " + Binder.getCallingPid() + " (pkg = " + callingApp);
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
        verifyIncomingOp(code);
        int code2 = AppOpsManager.opToSwitch(code);
        synchronized (this) {
            int defaultMode = AppOpsManager.opToDefaultMode(code2);
            UidState uidState = getUidStateLocked(uid, false);
            if (uidState == null) {
                if (mode == defaultMode) {
                    return;
                }
                UidState uidState2 = new UidState(uid);
                uidState2.opModes = new SparseIntArray();
                uidState2.opModes.put(code2, mode);
                this.mUidStates.put(uid, uidState2);
                scheduleWriteLocked();
            } else if (uidState.opModes != null) {
                if (uidState.opModes.get(code2) == mode) {
                    return;
                }
                if (mode == defaultMode) {
                    uidState.opModes.delete(code2);
                    if (uidState.opModes.size() <= 0) {
                        uidState.opModes = null;
                    }
                } else {
                    uidState.opModes.put(code2, mode);
                }
                scheduleWriteLocked();
            } else if (mode != defaultMode) {
                uidState.opModes = new SparseIntArray();
                uidState.opModes.put(code2, mode);
                scheduleWriteLocked();
            }
            String[] uidPackageNames = getPackagesForUid(uid);
            ArrayMap<Callback, ArraySet<String>> callbackSpecs2 = null;
            synchronized (this) {
                try {
                    ArrayList<Callback> callbacks = this.mOpModeWatchers.get(code2);
                    if (callbacks != null) {
                        int callbackCount = callbacks.size();
                        int i2 = 0;
                        callbackSpecs = null;
                        while (i2 < callbackCount) {
                            try {
                                Callback callback = callbacks.get(i2);
                                ArraySet<String> changedPackages = new ArraySet<>();
                                Collections.addAll(changedPackages, uidPackageNames);
                                ArrayMap<Callback, ArraySet<String>> callbackSpecs3 = new ArrayMap<>();
                                callbackSpecs3.put(callback, changedPackages);
                                i2++;
                                callbackSpecs = callbackSpecs3;
                            } catch (Throwable th) {
                                th = th;
                            }
                        }
                        callbackSpecs2 = callbackSpecs;
                    }
                    i = 0;
                    length = uidPackageNames.length;
                } catch (Throwable th2) {
                    th = th2;
                }
                while (true) {
                    callbackSpecs = callbackSpecs2;
                    if (i >= length) {
                        break;
                    }
                    String uidPackageName = uidPackageNames[i];
                    ArrayList<Callback> callbacks2 = this.mPackageModeWatchers.get(uidPackageName);
                    if (callbacks2 != null) {
                        callbackSpecs2 = callbackSpecs == null ? new ArrayMap<>() : callbackSpecs;
                        int callbackCount2 = callbacks2.size();
                        for (int i3 = 0; i3 < callbackCount2; i3++) {
                            Callback callback2 = callbacks2.get(i3);
                            ArraySet<String> changedPackages2 = callbackSpecs2.get(callback2);
                            if (changedPackages2 == null) {
                                changedPackages2 = new ArraySet<>();
                                callbackSpecs2.put(callback2, changedPackages2);
                            }
                            changedPackages2.add(uidPackageName);
                        }
                    } else {
                        callbackSpecs2 = callbackSpecs;
                    }
                    i++;
                    throw th;
                }
                if (callbackSpecs == null) {
                    return;
                }
                long identity = Binder.clearCallingIdentity();
                int i4 = 0;
                while (i4 < callbackSpecs.size()) {
                    try {
                        Callback callback3 = callbackSpecs.keyAt(i4);
                        ArraySet<String> reportedPackageNames = callbackSpecs.valueAt(i4);
                        if (reportedPackageNames == null) {
                            try {
                                callback3.mCallback.opChanged(code2, uid, (String) null);
                            } catch (RemoteException e) {
                                Log.w(TAG, "Error dispatching op op change", e);
                            }
                        } else {
                            int reportedPackageCount = reportedPackageNames.size();
                            for (int j = 0; j < reportedPackageCount; j++) {
                                String reportedPackageName = reportedPackageNames.valueAt(j);
                                callback3.mCallback.opChanged(code2, uid, reportedPackageName);
                            }
                        }
                        i4++;
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }
    }

    public void setMode(int code, int uid, String packageName, int mode) throws Throwable {
        ArrayList<Callback> repCbs;
        String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        Log.d(TAG, "setMode() - code = " + code + " targetPkgName = " + packageName + " mode = " + mode + " requested from pid = " + Binder.getCallingPid() + " (pkg = " + callingApp);
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
        verifyIncomingOp(code);
        ArrayList<Callback> repCbs2 = null;
        int code2 = AppOpsManager.opToSwitch(code);
        synchronized (this) {
            try {
                getUidStateLocked(uid, false);
                Op op = getOpLocked(code2, uid, packageName, true);
                if (op != null && op.mode != mode) {
                    op.mode = mode;
                    ArrayList<Callback> cbs = this.mOpModeWatchers.get(code2);
                    if (cbs != null) {
                        repCbs = new ArrayList<>();
                        try {
                            repCbs.addAll(cbs);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } else {
                        repCbs = null;
                    }
                    ArrayList<Callback> cbs2 = this.mPackageModeWatchers.get(packageName);
                    if (cbs2 != null) {
                        repCbs2 = repCbs == null ? new ArrayList<>() : repCbs;
                        repCbs2.addAll(cbs2);
                    } else {
                        repCbs2 = repCbs;
                    }
                    if (mode == AppOpsManager.opToDefaultMode(op.op)) {
                        pruneOp(op, uid, packageName);
                    }
                    scheduleFastWriteLocked();
                }
                if (repCbs2 != null) {
                    long identity = Binder.clearCallingIdentity();
                    for (int i = 0; i < repCbs2.size(); i++) {
                        try {
                            try {
                                repCbs2.get(i).mCallback.opChanged(code2, uid, packageName);
                            } catch (RemoteException e) {
                            }
                        } finally {
                            Binder.restoreCallingIdentity(identity);
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private static HashMap<Callback, ArrayList<ChangeRec>> addCallbacks(HashMap<Callback, ArrayList<ChangeRec>> callbacks, int op, int uid, String packageName, ArrayList<Callback> cbs) {
        if (cbs == null) {
            return callbacks;
        }
        if (callbacks == null) {
            callbacks = new HashMap<>();
        }
        boolean duplicate = false;
        for (int i = 0; i < cbs.size(); i++) {
            Callback cb = cbs.get(i);
            ArrayList<ChangeRec> reports = callbacks.get(cb);
            if (reports == null) {
                reports = new ArrayList<>();
                callbacks.put(cb, reports);
            } else {
                int reportCount = reports.size();
                int j = 0;
                while (true) {
                    if (j >= reportCount) {
                        break;
                    }
                    ChangeRec report = reports.get(j);
                    if (report.op != op || !report.pkg.equals(packageName)) {
                        j++;
                    } else {
                        duplicate = true;
                        break;
                    }
                }
            }
            if (!duplicate) {
                reports.add(new ChangeRec(op, uid, packageName));
            }
        }
        return callbacks;
    }

    static final class ChangeRec {
        final int op;
        final String pkg;
        final int uid;

        ChangeRec(int _op, int _uid, String _pkg) {
            this.op = _op;
            this.uid = _uid;
            this.pkg = _pkg;
        }
    }

    public void resetAllModes(int reqUserId, String reqPackageName) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", callingPid, callingUid, null);
        int reqUserId2 = ActivityManager.handleIncomingUser(callingPid, callingUid, reqUserId, true, true, "resetAllModes", null);
        int reqUid = -1;
        if (reqPackageName != null) {
            try {
                reqUid = AppGlobals.getPackageManager().getPackageUid(reqPackageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, reqUserId2);
            } catch (RemoteException e) {
            }
        }
        HashMap<Callback, ArrayList<ChangeRec>> callbacks = null;
        synchronized (this) {
            boolean changed = false;
            for (int i = this.mUidStates.size() - 1; i >= 0; i--) {
                UidState uidState = this.mUidStates.valueAt(i);
                SparseIntArray opModes = uidState.opModes;
                if (opModes != null && (uidState.uid == reqUid || reqUid == -1)) {
                    int uidOpCount = opModes.size();
                    for (int j = uidOpCount - 1; j >= 0; j--) {
                        int code = opModes.keyAt(j);
                        if (AppOpsManager.opAllowsReset(code)) {
                            opModes.removeAt(j);
                            if (opModes.size() <= 0) {
                                uidState.opModes = null;
                            }
                            for (String packageName : getPackagesForUid(uidState.uid)) {
                                callbacks = addCallbacks(addCallbacks(callbacks, code, uidState.uid, packageName, this.mOpModeWatchers.get(code)), code, uidState.uid, packageName, this.mPackageModeWatchers.get(packageName));
                            }
                        }
                    }
                }
                if (uidState.pkgOps != null && (reqUserId2 == -1 || reqUserId2 == UserHandle.getUserId(uidState.uid))) {
                    Map<String, Ops> packages = uidState.pkgOps;
                    Iterator<Map.Entry<String, Ops>> it = packages.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Ops> ent = it.next();
                        String packageName2 = ent.getKey();
                        if (reqPackageName == null || reqPackageName.equals(packageName2)) {
                            Ops pkgOps = ent.getValue();
                            for (int j2 = pkgOps.size() - 1; j2 >= 0; j2--) {
                                Op curOp = pkgOps.valueAt(j2);
                                if (AppOpsManager.opAllowsReset(curOp.op) && curOp.mode != AppOpsManager.opToDefaultMode(curOp.op)) {
                                    curOp.mode = AppOpsManager.opToDefaultMode(curOp.op);
                                    changed = true;
                                    callbacks = addCallbacks(addCallbacks(callbacks, curOp.op, curOp.uid, packageName2, this.mOpModeWatchers.get(curOp.op)), curOp.op, curOp.uid, packageName2, this.mPackageModeWatchers.get(packageName2));
                                    if (curOp.time == 0 && curOp.rejectTime == 0) {
                                        pkgOps.removeAt(j2);
                                    }
                                }
                            }
                            if (pkgOps.size() == 0) {
                                it.remove();
                            }
                        }
                    }
                    if (uidState.isDefault()) {
                        this.mUidStates.remove(uidState.uid);
                    }
                }
            }
            if (changed) {
                scheduleFastWriteLocked();
            }
        }
        if (callbacks == null) {
            return;
        }
        for (Map.Entry<Callback, ArrayList<ChangeRec>> ent2 : callbacks.entrySet()) {
            Callback cb = ent2.getKey();
            ArrayList<ChangeRec> reports = ent2.getValue();
            for (int i2 = 0; i2 < reports.size(); i2++) {
                ChangeRec rep = reports.get(i2);
                try {
                    cb.mCallback.opChanged(rep.op, rep.uid, rep.pkg);
                } catch (RemoteException e2) {
                }
            }
        }
    }

    public void startWatchingMode(int op, String packageName, IAppOpsCallback callback) {
        Callback cb;
        if (callback == null) {
            return;
        }
        synchronized (this) {
            if (op != -1) {
                op = AppOpsManager.opToSwitch(op);
                cb = this.mModeWatchers.get(callback.asBinder());
                if (cb == null) {
                    cb = new Callback(callback);
                    this.mModeWatchers.put(callback.asBinder(), cb);
                }
                if (op != -1) {
                    ArrayList<Callback> cbs = this.mOpModeWatchers.get(op);
                    if (cbs == null) {
                        cbs = new ArrayList<>();
                        this.mOpModeWatchers.put(op, cbs);
                    }
                    cbs.add(cb);
                }
                if (packageName != null) {
                    ArrayList<Callback> cbs2 = this.mPackageModeWatchers.get(packageName);
                    if (cbs2 == null) {
                        cbs2 = new ArrayList<>();
                        this.mPackageModeWatchers.put(packageName, cbs2);
                    }
                    cbs2.add(cb);
                }
            } else {
                cb = this.mModeWatchers.get(callback.asBinder());
                if (cb == null) {
                }
                if (op != -1) {
                }
                if (packageName != null) {
                }
            }
        }
    }

    public void stopWatchingMode(IAppOpsCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (this) {
            Callback cb = this.mModeWatchers.remove(callback.asBinder());
            if (cb != null) {
                cb.unlinkToDeath();
                for (int i = this.mOpModeWatchers.size() - 1; i >= 0; i--) {
                    ArrayList<Callback> cbs = this.mOpModeWatchers.valueAt(i);
                    cbs.remove(cb);
                    if (cbs.size() <= 0) {
                        this.mOpModeWatchers.removeAt(i);
                    }
                }
                for (int i2 = this.mPackageModeWatchers.size() - 1; i2 >= 0; i2--) {
                    ArrayList<Callback> cbs2 = this.mPackageModeWatchers.valueAt(i2);
                    cbs2.remove(cb);
                    if (cbs2.size() <= 0) {
                        this.mPackageModeWatchers.removeAt(i2);
                    }
                }
            }
        }
    }

    public IBinder getToken(IBinder clientToken) {
        ClientState cs;
        synchronized (this) {
            cs = this.mClients.get(clientToken);
            if (cs == null) {
                cs = new ClientState(clientToken);
                this.mClients.put(clientToken, cs);
            }
        }
        return cs;
    }

    public int checkOperation(int code, int uid, String packageName) {
        int uidMode;
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return 1;
        }
        synchronized (this) {
            if (isOpRestricted(uid, code, resolvedPackageName)) {
                return 1;
            }
            int code2 = AppOpsManager.opToSwitch(code);
            UidState uidState = getUidStateLocked(uid, false);
            if (uidState != null && uidState.opModes != null && (uidMode = uidState.opModes.get(code2)) != 0 && !operationFallBackCheck(uidState, code2, uid, packageName)) {
                if (ENG_LOAD) {
                    Log.d(TAG, "checkOperation(code = " + code2 + " uid = " + uid + " pkgName = " + packageName + ") - return uidMode = " + uidMode);
                }
                return uidMode;
            }
            Op op = getOpLocked(code2, uid, resolvedPackageName, false);
            if (op == null) {
                if (ENG_LOAD) {
                    Log.d(TAG, "checkOperation(code = " + code2 + " uid = " + uid + " pkgName = " + packageName + ") - op == null, return default mode = " + AppOpsManager.opToDefaultMode(code2));
                }
                return AppOpsManager.opToDefaultMode(code2);
            }
            if (op.mode != 0) {
                Ops ops = getOpsRawLocked(uid, packageName, false);
                if (!operationFallBackCheck(ops, code2, uid, packageName)) {
                    if (ENG_LOAD) {
                        Log.d(TAG, "checkOperation(code = " + code2 + " uid = " + uid + " pkgName = " + packageName + ") - return op.mode = " + op.mode);
                    }
                    return op.mode;
                }
            }
            return 0;
        }
    }

    public int checkAudioOperation(int code, int usage, int uid, String packageName) {
        boolean zIsPackageSuspendedForUser;
        try {
            zIsPackageSuspendedForUser = isPackageSuspendedForUser(packageName, uid);
        } catch (IllegalArgumentException e) {
            zIsPackageSuspendedForUser = false;
        }
        if (zIsPackageSuspendedForUser) {
            Log.i(TAG, "Audio disabled for suspended package=" + packageName + " for uid=" + uid);
            return 1;
        }
        synchronized (this) {
            int mode = checkRestrictionLocked(code, usage, uid, packageName);
            if (mode != 0) {
                return mode;
            }
            return checkOperation(code, uid, packageName);
        }
    }

    private boolean isPackageSuspendedForUser(String pkg, int uid) {
        try {
            return AppGlobals.getPackageManager().isPackageSuspendedForUser(pkg, UserHandle.getUserId(uid));
        } catch (RemoteException e) {
            throw new SecurityException("Could not talk to package manager service");
        }
    }

    private int checkRestrictionLocked(int code, int usage, int uid, String packageName) {
        Restriction r;
        SparseArray<Restriction> usageRestrictions = this.mAudioRestrictions.get(code);
        if (usageRestrictions != null && (r = usageRestrictions.get(usage)) != null && !r.exceptionPackages.contains(packageName)) {
            return r.mode;
        }
        return 0;
    }

    public void setAudioRestriction(int code, int usage, int uid, int mode, String[] exceptionPackages) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        synchronized (this) {
            SparseArray<Restriction> usageRestrictions = this.mAudioRestrictions.get(code);
            if (usageRestrictions == null) {
                usageRestrictions = new SparseArray<>();
                this.mAudioRestrictions.put(code, usageRestrictions);
            }
            usageRestrictions.remove(usage);
            if (mode != 0) {
                Restriction r = new Restriction(null);
                r.mode = mode;
                if (exceptionPackages != null) {
                    int N = exceptionPackages.length;
                    r.exceptionPackages = new ArraySet<>(N);
                    for (String pkg : exceptionPackages) {
                        if (pkg != null) {
                            r.exceptionPackages.add(pkg.trim());
                        }
                    }
                }
                usageRestrictions.put(usage, r);
            }
        }
        notifyWatchersOfChange(code);
    }

    public int checkPackage(int uid, String packageName) {
        Preconditions.checkNotNull(packageName);
        synchronized (this) {
            if (getOpsRawLocked(uid, packageName, true) != null) {
                return 0;
            }
            return 2;
        }
    }

    public int noteProxyOperation(int code, String proxyPackageName, int proxiedUid, String proxiedPackageName) {
        verifyIncomingOp(code);
        int proxyUid = Binder.getCallingUid();
        String resolveProxyPackageName = resolvePackageName(proxyUid, proxyPackageName);
        if (resolveProxyPackageName == null) {
            return 1;
        }
        int proxyMode = noteOperationUnchecked(code, proxyUid, resolveProxyPackageName, -1, null);
        if (proxyMode != 0 || Binder.getCallingUid() == proxiedUid) {
            return proxyMode;
        }
        String resolveProxiedPackageName = resolvePackageName(proxiedUid, proxiedPackageName);
        if (resolveProxiedPackageName == null) {
            return 1;
        }
        return noteOperationUnchecked(code, proxiedUid, resolveProxiedPackageName, proxyMode, resolveProxyPackageName);
    }

    public int noteOperation(int code, int uid, String packageName) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return 1;
        }
        return noteOperationUnchecked(code, uid, resolvedPackageName, 0, null);
    }

    private int noteOperationUnchecked(int code, int uid, String packageName, int proxyUid, String proxyPackageName) {
        synchronized (this) {
            Ops ops = getOpsRawLocked(uid, packageName, true);
            if (ops == null) {
                Log.d(TAG, "noteOperation: no op for code " + code + " uid " + uid + " package " + packageName);
                return 2;
            }
            Op op = getOpLocked(ops, code, true);
            if (isOpRestricted(uid, code, packageName)) {
                return 1;
            }
            if (op.duration == -1) {
                Slog.w(TAG, "Noting op not finished: uid " + uid + " pkg " + packageName + " code " + code + " time=" + op.time + " duration=" + op.duration);
            }
            op.duration = 0;
            int switchCode = AppOpsManager.opToSwitch(code);
            UidState uidState = ops.uidState;
            if (uidState.opModes == null || uidState.opModes.indexOfKey(switchCode) < 0) {
                Op switchOp = switchCode != code ? getOpLocked(ops, switchCode, true) : op;
                if (switchOp.mode != 0 && !operationFallBackCheck(ops, switchOp.op, uid, packageName)) {
                    if (ENG_LOAD) {
                        Log.d(TAG, "noteOperation: reject #" + op.mode + " for code " + switchCode + " (" + code + ") uid " + uid + " package " + packageName);
                    }
                    op.rejectTime = System.currentTimeMillis();
                    return switchOp.mode;
                }
            } else {
                int uidMode = uidState.opModes.get(switchCode);
                if (uidMode != 0 && !operationFallBackCheck(uidState, switchCode, uid, packageName)) {
                    if (ENG_LOAD) {
                        Log.d(TAG, "noteOperation: reject #" + op.mode + " for code " + switchCode + " (" + code + ") uid " + uid + " package " + packageName);
                    }
                    op.rejectTime = System.currentTimeMillis();
                    return uidMode;
                }
            }
            if (ENG_LOAD) {
                Log.d(TAG, "noteOperation: allowing code " + code + " uid " + uid + " package " + packageName);
            }
            op.time = System.currentTimeMillis();
            op.rejectTime = 0L;
            op.proxyUid = proxyUid;
            op.proxyPackageName = proxyPackageName;
            return 0;
        }
    }

    public int startOperation(IBinder token, int code, int uid, String packageName) {
        int uidMode;
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return 1;
        }
        ClientState client = (ClientState) token;
        synchronized (this) {
            Ops ops = getOpsRawLocked(uid, resolvedPackageName, true);
            if (ops == null) {
                Log.d(TAG, "startOperation: no op for code " + code + " uid " + uid + " package " + resolvedPackageName);
                return 2;
            }
            Op op = getOpLocked(ops, code, true);
            if (isOpRestricted(uid, code, resolvedPackageName)) {
                return 1;
            }
            int switchCode = AppOpsManager.opToSwitch(code);
            UidState uidState = ops.uidState;
            if (uidState.opModes != null && (uidMode = uidState.opModes.get(switchCode)) != 0 && !operationFallBackCheck(uidState, switchCode, uid, packageName)) {
                if (ENG_LOAD) {
                    Log.d(TAG, "noteOperation: reject #" + op.mode + " for code " + switchCode + " (" + code + ") uid " + uid + " package " + resolvedPackageName);
                }
                op.rejectTime = System.currentTimeMillis();
                return uidMode;
            }
            Op switchOp = switchCode != code ? getOpLocked(ops, switchCode, true) : op;
            if (switchOp.mode != 0 && !operationFallBackCheck(ops, switchOp.op, uid, packageName)) {
                if (ENG_LOAD) {
                    Log.d(TAG, "startOperation: reject #" + op.mode + " for code " + switchCode + " (" + code + ") uid " + uid + " package " + resolvedPackageName);
                }
                op.rejectTime = System.currentTimeMillis();
                return switchOp.mode;
            }
            Log.d(TAG, "startOperation: allowing code " + code + " uid " + uid + " package " + resolvedPackageName);
            if (op.nesting == 0) {
                op.time = System.currentTimeMillis();
                op.rejectTime = 0L;
                op.duration = -1;
            }
            op.nesting++;
            if (client.mStartedOps != null) {
                client.mStartedOps.add(op);
            }
            return 0;
        }
    }

    public void finishOperation(IBinder token, int code, int uid, String packageName) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName != null && (token instanceof ClientState)) {
            ClientState client = (ClientState) token;
            synchronized (this) {
                Op op = getOpLocked(code, uid, resolvedPackageName, true);
                if (op == null) {
                    return;
                }
                if (client.mStartedOps != null && !client.mStartedOps.remove(op)) {
                    throw new IllegalStateException("Operation not started: uid" + op.uid + " pkg=" + op.packageName + " op=" + op.op);
                }
                finishOperationLocked(op);
            }
        }
    }

    public int permissionToOpCode(String permission) {
        if (permission == null) {
            return -1;
        }
        return AppOpsManager.permissionToOpCode(permission);
    }

    void finishOperationLocked(Op op) {
        if (op.nesting > 1) {
            op.nesting--;
            return;
        }
        if (op.nesting == 1) {
            op.duration = (int) (System.currentTimeMillis() - op.time);
            op.time += (long) op.duration;
        } else {
            Slog.w(TAG, "Finishing op nesting under-run: uid " + op.uid + " pkg " + op.packageName + " code " + op.op + " time=" + op.time + " duration=" + op.duration + " nesting=" + op.nesting);
        }
        op.nesting = 0;
    }

    private void verifyIncomingUid(int uid) {
        if (uid == Binder.getCallingUid() || Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    private void verifyIncomingOp(int op) {
        if (op >= 0 && op < 69) {
        } else {
            throw new IllegalArgumentException("Bad operation #" + op);
        }
    }

    private UidState getUidStateLocked(int uid, boolean edit) {
        UidState uidState = this.mUidStates.get(uid);
        if (uidState == null) {
            if (!edit) {
                return null;
            }
            UidState uidState2 = new UidState(uid);
            this.mUidStates.put(uid, uidState2);
            return uidState2;
        }
        return uidState;
    }

    private Ops getOpsRawLocked(int uid, String packageName, boolean edit) {
        UidState uidState = getUidStateLocked(uid, edit);
        if (uidState == null) {
            return null;
        }
        if (uidState.pkgOps == null) {
            if (!edit) {
                return null;
            }
            uidState.pkgOps = new ArrayMap<>();
        }
        Ops ops = uidState.pkgOps.get(packageName);
        if (ops == null) {
            if (!edit) {
                return null;
            }
            boolean isPrivileged = false;
            if (uid != 0) {
                long ident = Binder.clearCallingIdentity();
                int pkgUid = -1;
                try {
                    try {
                        ApplicationInfo appInfo = ActivityThread.getPackageManager().getApplicationInfo(packageName, 268435456, UserHandle.getUserId(uid));
                        if (appInfo != null) {
                            pkgUid = appInfo.uid;
                            isPrivileged = (appInfo.privateFlags & 8) != 0;
                        } else if ("media".equals(packageName)) {
                            pkgUid = 1013;
                            isPrivileged = false;
                        } else if ("audioserver".equals(packageName)) {
                            pkgUid = 1041;
                            isPrivileged = false;
                        } else if ("cameraserver".equals(packageName)) {
                            pkgUid = 1047;
                            isPrivileged = false;
                        }
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Could not contact PackageManager", e);
                    }
                    if (pkgUid != uid) {
                        RuntimeException ex = new RuntimeException("here");
                        ex.fillInStackTrace();
                        Slog.w(TAG, "Bad call: specified package " + packageName + " under uid " + uid + " but it is really " + pkgUid, ex);
                        return null;
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            Ops ops2 = new Ops(packageName, uidState, isPrivileged);
            uidState.pkgOps.put(packageName, ops2);
            return ops2;
        }
        return ops;
    }

    private void scheduleWriteLocked() {
        if (this.mWriteScheduled) {
            return;
        }
        this.mWriteScheduled = true;
        this.mHandler.postDelayed(this.mWriteRunner, WRITE_DELAY);
    }

    private void scheduleFastWriteLocked() {
        if (this.mFastWriteScheduled) {
            return;
        }
        this.mWriteScheduled = true;
        this.mFastWriteScheduled = true;
        this.mHandler.removeCallbacks(this.mWriteRunner);
        this.mHandler.postDelayed(this.mWriteRunner, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private Op getOpLocked(int code, int uid, String packageName, boolean edit) {
        Ops ops = getOpsRawLocked(uid, packageName, edit);
        if (ops == null) {
            return null;
        }
        return getOpLocked(ops, code, edit);
    }

    private Op getOpLocked(Ops ops, int code, boolean edit) {
        Op op = ops.get(code);
        if (op == null) {
            if (!edit) {
                return null;
            }
            op = new Op(ops.uidState.uid, ops.packageName, code);
            ops.put(code, op);
        }
        if (edit) {
            scheduleWriteLocked();
        }
        return op;
    }

    private boolean isOpRestricted(int uid, int code, String packageName) {
        int userHandle = UserHandle.getUserId(uid);
        int restrictionSetCount = this.mOpUserRestrictions.size();
        for (int i = 0; i < restrictionSetCount; i++) {
            try {
                ClientRestrictionState restrictionState = this.mOpUserRestrictions.valueAt(i);
                if (restrictionState.hasRestriction(code, packageName, userHandle)) {
                    if (AppOpsManager.opAllowSystemBypassRestriction(code)) {
                        synchronized (this) {
                            Ops ops = getOpsRawLocked(uid, packageName, true);
                            if (ops != null) {
                                if (ops.isPrivileged) {
                                    return false;
                                }
                            }
                        }
                    }
                    Log.d(TAG, "Op is restricted(code = " + code + " uid = " + uid + " pkgName = " + packageName + "), return MODE_IGNORED");
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "isOpRestricted() - handle cast & outofbonds here.");
            }
        }
        return false;
    }

    void readState() {
        XmlPullParser parser;
        int type;
        synchronized (this.mFile) {
            synchronized (this) {
                try {
                    FileInputStream stream = this.mFile.openRead();
                    this.mUidStates.clear();
                    try {
                        try {
                            try {
                                try {
                                    try {
                                        try {
                                            try {
                                                parser = Xml.newPullParser();
                                                parser.setInput(stream, StandardCharsets.UTF_8.name());
                                                do {
                                                    type = parser.next();
                                                    if (type == 2) {
                                                        break;
                                                    }
                                                } while (type != 1);
                                            } catch (Throwable th) {
                                                if (0 == 0) {
                                                    this.mUidStates.clear();
                                                }
                                                try {
                                                    stream.close();
                                                } catch (IOException e) {
                                                }
                                                throw th;
                                            }
                                        } catch (NumberFormatException e2) {
                                            Slog.w(TAG, "Failed parsing " + e2);
                                            if (0 == 0) {
                                                this.mUidStates.clear();
                                            }
                                            try {
                                                stream.close();
                                            } catch (IOException e3) {
                                            }
                                        }
                                    } catch (IndexOutOfBoundsException e4) {
                                        Slog.w(TAG, "Failed parsing " + e4);
                                        if (0 == 0) {
                                            this.mUidStates.clear();
                                        }
                                        try {
                                            stream.close();
                                        } catch (IOException e5) {
                                        }
                                    }
                                } catch (IOException e6) {
                                    Slog.w(TAG, "Failed parsing " + e6);
                                    if (0 == 0) {
                                        this.mUidStates.clear();
                                    }
                                    try {
                                        stream.close();
                                    } catch (IOException e7) {
                                    }
                                }
                            } catch (IllegalStateException e8) {
                                Slog.w(TAG, "Failed parsing " + e8);
                                if (0 == 0) {
                                    this.mUidStates.clear();
                                }
                                try {
                                    stream.close();
                                } catch (IOException e9) {
                                }
                            }
                        } catch (NullPointerException e10) {
                            Slog.w(TAG, "Failed parsing " + e10);
                            if (0 == 0) {
                                this.mUidStates.clear();
                            }
                            try {
                                stream.close();
                            } catch (IOException e11) {
                            }
                        }
                    } catch (XmlPullParserException e12) {
                        Slog.w(TAG, "Failed parsing " + e12);
                        if (0 == 0) {
                            this.mUidStates.clear();
                        }
                        try {
                            stream.close();
                        } catch (IOException e13) {
                        }
                    }
                    if (type != 2) {
                        throw new IllegalStateException("no start tag found");
                    }
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                            break;
                        }
                        if (type2 != 3 && type2 != 4) {
                            String tagName = parser.getName();
                            if (tagName.equals("pkg")) {
                                readPackage(parser);
                            } else if (tagName.equals(AWSDBHelper.PackageProcessList.KEY_UID)) {
                                readUidOps(parser);
                            } else {
                                Slog.w(TAG, "Unknown element under <app-ops>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                    if (1 == 0) {
                        this.mUidStates.clear();
                    }
                    try {
                        stream.close();
                    } catch (IOException e14) {
                    }
                } catch (FileNotFoundException e15) {
                    Slog.i(TAG, "No existing app ops " + this.mFile.getBaseFile() + "; starting empty");
                }
            }
        }
    }

    void readUidOps(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
        int uid = Integer.parseInt(parser.getAttributeValue(null, "n"));
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals("op")) {
                    int code = Integer.parseInt(parser.getAttributeValue(null, "n"));
                    int mode = Integer.parseInt(parser.getAttributeValue(null, "m"));
                    UidState uidState = getUidStateLocked(uid, true);
                    if (uidState.opModes == null) {
                        uidState.opModes = new SparseIntArray();
                    }
                    uidState.opModes.put(code, mode);
                } else {
                    Slog.w(TAG, "Unknown element under <uid-ops>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    void readPackage(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
        String pkgName = parser.getAttributeValue(null, "n");
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(AWSDBHelper.PackageProcessList.KEY_UID)) {
                    readUid(parser, pkgName);
                } else {
                    Slog.w(TAG, "Unknown element under <pkg>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    void readUid(XmlPullParser parser, String pkgName) throws XmlPullParserException, IOException, NumberFormatException {
        int uid = Integer.parseInt(parser.getAttributeValue(null, "n"));
        String isPrivilegedString = parser.getAttributeValue(null, "p");
        boolean isPrivileged = false;
        if (isPrivilegedString == null) {
            try {
                IPackageManager packageManager = ActivityThread.getPackageManager();
                if (packageManager != null) {
                    ApplicationInfo appInfo = ActivityThread.getPackageManager().getApplicationInfo(pkgName, 0, UserHandle.getUserId(uid));
                    if (appInfo != null) {
                        isPrivileged = (appInfo.privateFlags & 8) != 0;
                    }
                } else {
                    return;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not contact PackageManager", e);
            }
        } else {
            isPrivileged = Boolean.parseBoolean(isPrivilegedString);
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals("op")) {
                    Op op = new Op(uid, pkgName, Integer.parseInt(parser.getAttributeValue(null, "n")));
                    String mode = parser.getAttributeValue(null, "m");
                    if (mode != null) {
                        op.mode = Integer.parseInt(mode);
                    }
                    String time = parser.getAttributeValue(null, "t");
                    if (time != null) {
                        op.time = Long.parseLong(time);
                    }
                    String time2 = parser.getAttributeValue(null, "r");
                    if (time2 != null) {
                        op.rejectTime = Long.parseLong(time2);
                    }
                    String dur = parser.getAttributeValue(null, "d");
                    if (dur != null) {
                        op.duration = Integer.parseInt(dur);
                    }
                    String proxyUid = parser.getAttributeValue(null, "pu");
                    if (proxyUid != null) {
                        op.proxyUid = Integer.parseInt(proxyUid);
                    }
                    String proxyPackageName = parser.getAttributeValue(null, "pp");
                    if (proxyPackageName != null) {
                        op.proxyPackageName = proxyPackageName;
                    }
                    UidState uidState = getUidStateLocked(uid, true);
                    if (uidState.pkgOps == null) {
                        uidState.pkgOps = new ArrayMap<>();
                    }
                    Ops ops = uidState.pkgOps.get(pkgName);
                    if (ops == null) {
                        ops = new Ops(pkgName, uidState, isPrivileged);
                        uidState.pkgOps.put(pkgName, ops);
                    }
                    ops.put(op.op, op);
                } else {
                    Slog.w(TAG, "Unknown element under <pkg>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    void writeState() {
        synchronized (this.mFile) {
            List<AppOpsManager.PackageOps> allOps = getPackagesForOps(null);
            try {
                FileOutputStream stream = this.mFile.startWrite();
                try {
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(stream, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, "app-ops");
                    int uidStateCount = this.mUidStates.size();
                    for (int i = 0; i < uidStateCount; i++) {
                        UidState uidState = this.mUidStates.valueAt(i);
                        if (uidState.opModes != null && uidState.opModes.size() > 0) {
                            fastXmlSerializer.startTag(null, AWSDBHelper.PackageProcessList.KEY_UID);
                            fastXmlSerializer.attribute(null, "n", Integer.toString(uidState.uid));
                            SparseIntArray uidOpModes = uidState.opModes;
                            int opCount = uidOpModes.size();
                            for (int j = 0; j < opCount; j++) {
                                int op = uidOpModes.keyAt(j);
                                int mode = uidOpModes.valueAt(j);
                                fastXmlSerializer.startTag(null, "op");
                                fastXmlSerializer.attribute(null, "n", Integer.toString(op));
                                fastXmlSerializer.attribute(null, "m", Integer.toString(mode));
                                fastXmlSerializer.endTag(null, "op");
                            }
                            fastXmlSerializer.endTag(null, AWSDBHelper.PackageProcessList.KEY_UID);
                        }
                    }
                    if (allOps != null) {
                        String lastPkg = null;
                        for (int i2 = 0; i2 < allOps.size(); i2++) {
                            AppOpsManager.PackageOps pkg = allOps.get(i2);
                            if (!pkg.getPackageName().equals(lastPkg)) {
                                if (lastPkg != null) {
                                    fastXmlSerializer.endTag(null, "pkg");
                                }
                                lastPkg = pkg.getPackageName();
                                fastXmlSerializer.startTag(null, "pkg");
                                fastXmlSerializer.attribute(null, "n", lastPkg);
                            }
                            fastXmlSerializer.startTag(null, AWSDBHelper.PackageProcessList.KEY_UID);
                            fastXmlSerializer.attribute(null, "n", Integer.toString(pkg.getUid()));
                            synchronized (this) {
                                Ops ops = getOpsRawLocked(pkg.getUid(), pkg.getPackageName(), false);
                                if (ops != null) {
                                    fastXmlSerializer.attribute(null, "p", Boolean.toString(ops.isPrivileged));
                                } else {
                                    fastXmlSerializer.attribute(null, "p", Boolean.toString(false));
                                }
                            }
                            List<AppOpsManager.OpEntry> ops2 = pkg.getOps();
                            for (int j2 = 0; j2 < ops2.size(); j2++) {
                                AppOpsManager.OpEntry op2 = ops2.get(j2);
                                fastXmlSerializer.startTag(null, "op");
                                fastXmlSerializer.attribute(null, "n", Integer.toString(op2.getOp()));
                                if (op2.getMode() != AppOpsManager.opToDefaultMode(op2.getOp())) {
                                    fastXmlSerializer.attribute(null, "m", Integer.toString(op2.getMode()));
                                }
                                long time = op2.getTime();
                                if (time != 0) {
                                    fastXmlSerializer.attribute(null, "t", Long.toString(time));
                                }
                                long time2 = op2.getRejectTime();
                                if (time2 != 0) {
                                    fastXmlSerializer.attribute(null, "r", Long.toString(time2));
                                }
                                int dur = op2.getDuration();
                                if (dur != 0) {
                                    fastXmlSerializer.attribute(null, "d", Integer.toString(dur));
                                }
                                int proxyUid = op2.getProxyUid();
                                if (proxyUid != -1) {
                                    fastXmlSerializer.attribute(null, "pu", Integer.toString(proxyUid));
                                }
                                String proxyPackageName = op2.getProxyPackageName();
                                if (proxyPackageName != null) {
                                    fastXmlSerializer.attribute(null, "pp", proxyPackageName);
                                }
                                fastXmlSerializer.endTag(null, "op");
                            }
                            fastXmlSerializer.endTag(null, AWSDBHelper.PackageProcessList.KEY_UID);
                        }
                        if (lastPkg != null) {
                            fastXmlSerializer.endTag(null, "pkg");
                        }
                    }
                    fastXmlSerializer.endTag(null, "app-ops");
                    fastXmlSerializer.endDocument();
                    this.mFile.finishWrite(stream);
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to write state, restoring backup.", e);
                    this.mFile.failWrite(stream);
                }
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to write state: " + e2);
            }
        }
    }

    static class Shell extends ShellCommand {
        final IAppOpsService mInterface;
        final AppOpsService mInternal;
        int mode;
        String modeStr;
        int op;
        String opStr;
        String packageName;
        int packageUid;
        int userId = 0;

        Shell(IAppOpsService iface, AppOpsService internal) {
            this.mInterface = iface;
            this.mInternal = internal;
        }

        public int onCommand(String cmd) {
            return AppOpsService.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            AppOpsService.dumpCommandHelp(pw);
        }

        private int strOpToOp(String op, PrintWriter err) {
            try {
                return AppOpsManager.strOpToOp(op);
            } catch (IllegalArgumentException e) {
                try {
                    return Integer.parseInt(op);
                } catch (NumberFormatException e2) {
                    try {
                        return AppOpsManager.strDebugOpToOp(op);
                    } catch (IllegalArgumentException e3) {
                        err.println("Error: " + e3.getMessage());
                        return -1;
                    }
                }
            }
        }

        int strModeToMode(String modeStr, PrintWriter err) {
            if (modeStr.equals("allow")) {
                return 0;
            }
            if (modeStr.equals("deny")) {
                return 2;
            }
            if (modeStr.equals("ignore")) {
                return 1;
            }
            if (modeStr.equals("default")) {
                return 3;
            }
            try {
                return Integer.parseInt(modeStr);
            } catch (NumberFormatException e) {
                err.println("Error: Mode " + modeStr + " is not valid");
                return -1;
            }
        }

        int parseUserOpMode(int defMode, PrintWriter err) throws RemoteException {
            this.userId = -2;
            this.opStr = null;
            this.modeStr = null;
            while (true) {
                String argument = getNextArg();
                if (argument == null) {
                    break;
                }
                if ("--user".equals(argument)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (this.opStr == null) {
                    this.opStr = argument;
                } else if (this.modeStr == null) {
                    this.modeStr = argument;
                    break;
                }
            }
            if (this.opStr == null) {
                err.println("Error: Operation not specified.");
                return -1;
            }
            this.op = strOpToOp(this.opStr, err);
            if (this.op < 0) {
                return -1;
            }
            if (this.modeStr != null) {
                int iStrModeToMode = strModeToMode(this.modeStr, err);
                this.mode = iStrModeToMode;
                if (iStrModeToMode < 0) {
                    return -1;
                }
            } else {
                this.mode = defMode;
            }
            return 0;
        }

        int parseUserPackageOp(boolean reqOp, PrintWriter err) throws RemoteException {
            this.userId = -2;
            this.packageName = null;
            this.opStr = null;
            while (true) {
                String argument = getNextArg();
                if (argument == null) {
                    break;
                }
                if ("--user".equals(argument)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (this.packageName == null) {
                    this.packageName = argument;
                } else if (this.opStr == null) {
                    this.opStr = argument;
                    break;
                }
            }
            if (this.packageName == null) {
                err.println("Error: Package name not specified.");
                return -1;
            }
            if (this.opStr == null && reqOp) {
                err.println("Error: Operation not specified.");
                return -1;
            }
            if (this.opStr != null) {
                this.op = strOpToOp(this.opStr, err);
                if (this.op < 0) {
                    return -1;
                }
            } else {
                this.op = -1;
            }
            if (this.userId == -2) {
                this.userId = ActivityManager.getCurrentUser();
            }
            if ("root".equals(this.packageName)) {
                this.packageUid = 0;
            } else {
                this.packageUid = AppGlobals.getPackageManager().getPackageUid(this.packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, this.userId);
            }
            if (this.packageUid >= 0) {
                return 0;
            }
            err.println("Error: No UID for " + this.packageName + " in user " + this.userId);
            return -1;
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) {
        new Shell(this, this).exec(this, in, out, err, args, resultReceiver);
    }

    static void dumpCommandHelp(PrintWriter pw) {
        pw.println("AppOps service (appops) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set [--user <USER_ID>] <PACKAGE> <OP> <MODE>");
        pw.println("    Set the mode for a particular application and operation.");
        pw.println("  get [--user <USER_ID>] <PACKAGE> [<OP>]");
        pw.println("    Return the mode for a particular application and optional operation.");
        pw.println("  query-op [--user <USER_ID>] <OP> [<MODE>]");
        pw.println("    Print all packages that currently have the given op in the given mode.");
        pw.println("  reset [--user <USER_ID>] [<PACKAGE>]");
        pw.println("    Reset the given application or all applications to default modes.");
        pw.println("  write-settings");
        pw.println("    Immediately write pending changes to storage.");
        pw.println("  read-settings");
        pw.println("    Read the last written settings, replacing current state in RAM.");
        pw.println("  options:");
        pw.println("    <PACKAGE> an Android package name.");
        pw.println("    <OP>      an AppOps operation.");
        pw.println("    <MODE>    one of allow, ignore, deny, or default");
        pw.println("    <USER_ID> the user id under which the package is installed. If --user is not");
        pw.println("              specified, the current user is assumed.");
    }

    static int onShellCommand(Shell shell, String cmd) {
        long token;
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        PrintWriter err = shell.getErrPrintWriter();
        try {
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
        if (cmd.equals("set")) {
            int res = shell.parseUserPackageOp(true, err);
            if (res < 0) {
                return res;
            }
            String modeStr = shell.getNextArg();
            if (modeStr == null) {
                err.println("Error: Mode not specified.");
                return -1;
            }
            int mode = shell.strModeToMode(modeStr, err);
            if (mode < 0) {
                return -1;
            }
            shell.mInterface.setMode(shell.op, shell.packageUid, shell.packageName, mode);
            return 0;
        }
        if (cmd.equals("get")) {
            int res2 = shell.parseUserPackageOp(false, err);
            if (res2 < 0) {
                return res2;
            }
            List<AppOpsManager.PackageOps> ops = shell.mInterface.getOpsForPackage(shell.packageUid, shell.packageName, shell.op != -1 ? new int[]{shell.op} : null);
            if (ops == null || ops.size() <= 0) {
                pw.println("No operations.");
                return 0;
            }
            long now = System.currentTimeMillis();
            for (int i = 0; i < ops.size(); i++) {
                List<AppOpsManager.OpEntry> entries = ops.get(i).getOps();
                for (int j = 0; j < entries.size(); j++) {
                    AppOpsManager.OpEntry ent = entries.get(j);
                    pw.print(AppOpsManager.opToName(ent.getOp()));
                    pw.print(": ");
                    switch (ent.getMode()) {
                        case 0:
                            pw.print("allow");
                            break;
                        case 1:
                            pw.print("ignore");
                            break;
                        case 2:
                            pw.print("deny");
                            break;
                        case 3:
                            pw.print("default");
                            break;
                        default:
                            pw.print("mode=");
                            pw.print(ent.getMode());
                            break;
                    }
                    if (ent.getTime() != 0) {
                        pw.print("; time=");
                        TimeUtils.formatDuration(now - ent.getTime(), pw);
                        pw.print(" ago");
                    }
                    if (ent.getRejectTime() != 0) {
                        pw.print("; rejectTime=");
                        TimeUtils.formatDuration(now - ent.getRejectTime(), pw);
                        pw.print(" ago");
                    }
                    if (ent.getDuration() == -1) {
                        pw.print(" (running)");
                    } else if (ent.getDuration() != 0) {
                        pw.print("; duration=");
                        TimeUtils.formatDuration(ent.getDuration(), pw);
                    }
                    pw.println();
                }
            }
            return 0;
        }
        if (cmd.equals("query-op")) {
            int res3 = shell.parseUserOpMode(1, err);
            if (res3 < 0) {
                return res3;
            }
            List<AppOpsManager.PackageOps> ops2 = shell.mInterface.getPackagesForOps(new int[]{shell.op});
            if (ops2 == null || ops2.size() <= 0) {
                pw.println("No operations.");
                return 0;
            }
            for (int i2 = 0; i2 < ops2.size(); i2++) {
                AppOpsManager.PackageOps pkg = ops2.get(i2);
                boolean hasMatch = false;
                List<AppOpsManager.OpEntry> entries2 = ops2.get(i2).getOps();
                int j2 = 0;
                while (true) {
                    if (j2 >= entries2.size()) {
                        break;
                    }
                    AppOpsManager.OpEntry ent2 = entries2.get(j2);
                    if (ent2.getOp() == shell.op && ent2.getMode() == shell.mode) {
                        hasMatch = true;
                        break;
                    }
                    j2++;
                }
                if (hasMatch) {
                    pw.println(pkg.getPackageName());
                }
            }
            return 0;
        }
        if (!cmd.equals("reset")) {
            if (cmd.equals("write-settings")) {
                shell.mInternal.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
                token = Binder.clearCallingIdentity();
                try {
                    synchronized (shell.mInternal) {
                        shell.mInternal.mHandler.removeCallbacks(shell.mInternal.mWriteRunner);
                    }
                    shell.mInternal.writeState();
                    pw.println("Current settings written.");
                    Binder.restoreCallingIdentity(token);
                    return 0;
                } finally {
                }
            }
            if (!cmd.equals("read-settings")) {
                return shell.handleDefaultCommands(cmd);
            }
            shell.mInternal.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
            token = Binder.clearCallingIdentity();
            try {
                shell.mInternal.readState();
                pw.println("Last settings read.");
                Binder.restoreCallingIdentity(token);
                return 0;
            } finally {
            }
            pw.println("Remote exception: " + e);
            return -1;
        }
        String packageName = null;
        int userId = -2;
        while (true) {
            String argument = shell.getNextArg();
            if (argument == null) {
                if (userId == -2) {
                    userId = ActivityManager.getCurrentUser();
                }
                shell.mInterface.resetAllModes(userId, packageName);
                pw.print("Reset all modes for: ");
                if (userId == -1) {
                    pw.print("all users");
                } else {
                    pw.print("user ");
                    pw.print(userId);
                }
                pw.print(", ");
                if (packageName == null) {
                    pw.println("all packages");
                    return 0;
                }
                pw.print("package ");
                pw.println(packageName);
                return 0;
            }
            if ("--user".equals(argument)) {
                String userStr = shell.getNextArgRequired();
                userId = UserHandle.parseUserArg(userStr);
            } else {
                if (packageName != null) {
                    err.println("Error: Unsupported argument: " + argument);
                    return -1;
                }
                packageName = argument;
            }
        }
    }

    private void dumpHelp(PrintWriter pw) {
        pw.println("AppOps service (appops) dump options:");
        pw.println("  none");
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ApOps service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        if (args != null) {
            for (String arg : args) {
                if ("-h".equals(arg)) {
                    dumpHelp(pw);
                    return;
                }
                if (!"-a".equals(arg)) {
                    if (arg.length() <= 0 || arg.charAt(0) != '-') {
                        pw.println("Unknown command: " + arg);
                        return;
                    } else {
                        pw.println("Unknown option: " + arg);
                        return;
                    }
                }
            }
        }
        synchronized (this) {
            pw.println("Current AppOps Service state:");
            long now = System.currentTimeMillis();
            boolean needSep = false;
            if (this.mOpModeWatchers.size() > 0) {
                needSep = true;
                pw.println("  Op mode watchers:");
                for (int i = 0; i < this.mOpModeWatchers.size(); i++) {
                    pw.print("    Op ");
                    pw.print(AppOpsManager.opToName(this.mOpModeWatchers.keyAt(i)));
                    pw.println(":");
                    ArrayList<Callback> callbacks = this.mOpModeWatchers.valueAt(i);
                    for (int j = 0; j < callbacks.size(); j++) {
                        pw.print("      #");
                        pw.print(j);
                        pw.print(": ");
                        pw.println(callbacks.get(j));
                    }
                }
            }
            if (this.mPackageModeWatchers.size() > 0) {
                needSep = true;
                pw.println("  Package mode watchers:");
                for (int i2 = 0; i2 < this.mPackageModeWatchers.size(); i2++) {
                    pw.print("    Pkg ");
                    pw.print(this.mPackageModeWatchers.keyAt(i2));
                    pw.println(":");
                    ArrayList<Callback> callbacks2 = this.mPackageModeWatchers.valueAt(i2);
                    for (int j2 = 0; j2 < callbacks2.size(); j2++) {
                        pw.print("      #");
                        pw.print(j2);
                        pw.print(": ");
                        pw.println(callbacks2.get(j2));
                    }
                }
            }
            if (this.mModeWatchers.size() > 0) {
                needSep = true;
                pw.println("  All mode watchers:");
                for (int i3 = 0; i3 < this.mModeWatchers.size(); i3++) {
                    pw.print("    ");
                    pw.print(this.mModeWatchers.keyAt(i3));
                    pw.print(" -> ");
                    pw.println(this.mModeWatchers.valueAt(i3));
                }
            }
            if (this.mClients.size() > 0) {
                needSep = true;
                pw.println("  Clients:");
                for (int i4 = 0; i4 < this.mClients.size(); i4++) {
                    pw.print("    ");
                    pw.print(this.mClients.keyAt(i4));
                    pw.println(":");
                    ClientState cs = this.mClients.valueAt(i4);
                    pw.print("      ");
                    pw.println(cs);
                    if (cs.mStartedOps != null && cs.mStartedOps.size() > 0) {
                        pw.println("      Started ops:");
                        for (int j3 = 0; j3 < cs.mStartedOps.size(); j3++) {
                            Op op = cs.mStartedOps.get(j3);
                            pw.print("        ");
                            pw.print("uid=");
                            pw.print(op.uid);
                            pw.print(" pkg=");
                            pw.print(op.packageName);
                            pw.print(" op=");
                            pw.println(AppOpsManager.opToName(op.op));
                        }
                    }
                }
            }
            if (this.mAudioRestrictions.size() > 0) {
                boolean printedHeader = false;
                for (int o = 0; o < this.mAudioRestrictions.size(); o++) {
                    String op2 = AppOpsManager.opToName(this.mAudioRestrictions.keyAt(o));
                    SparseArray<Restriction> restrictions = this.mAudioRestrictions.valueAt(o);
                    for (int i5 = 0; i5 < restrictions.size(); i5++) {
                        if (!printedHeader) {
                            pw.println("  Audio Restrictions:");
                            printedHeader = true;
                            needSep = true;
                        }
                        int usage = restrictions.keyAt(i5);
                        pw.print("    ");
                        pw.print(op2);
                        pw.print(" usage=");
                        pw.print(AudioAttributes.usageToString(usage));
                        Restriction r = restrictions.valueAt(i5);
                        pw.print(": mode=");
                        pw.println(r.mode);
                        if (!r.exceptionPackages.isEmpty()) {
                            pw.println("      Exceptions:");
                            for (int j4 = 0; j4 < r.exceptionPackages.size(); j4++) {
                                pw.print("        ");
                                pw.println(r.exceptionPackages.valueAt(j4));
                            }
                        }
                    }
                }
            }
            if (needSep) {
                pw.println();
            }
            for (int i6 = 0; i6 < this.mUidStates.size(); i6++) {
                UidState uidState = this.mUidStates.valueAt(i6);
                pw.print("  Uid ");
                UserHandle.formatUid(pw, uidState.uid);
                pw.println(":");
                SparseIntArray opModes = uidState.opModes;
                if (opModes != null) {
                    int opModeCount = opModes.size();
                    for (int j5 = 0; j5 < opModeCount; j5++) {
                        int code = opModes.keyAt(j5);
                        int mode = opModes.valueAt(j5);
                        pw.print("      ");
                        pw.print(AppOpsManager.opToName(code));
                        pw.print(": mode=");
                        pw.println(mode);
                    }
                }
                ArrayMap<String, Ops> pkgOps = uidState.pkgOps;
                if (pkgOps != null) {
                    for (Ops ops : pkgOps.values()) {
                        pw.print("    Package ");
                        pw.print(ops.packageName);
                        pw.println(":");
                        for (int j6 = 0; j6 < ops.size(); j6++) {
                            Op op3 = ops.valueAt(j6);
                            pw.print("      ");
                            pw.print(AppOpsManager.opToName(op3.op));
                            pw.print(": mode=");
                            pw.print(op3.mode);
                            if (op3.time != 0) {
                                pw.print("; time=");
                                TimeUtils.formatDuration(now - op3.time, pw);
                                pw.print(" ago");
                            }
                            if (op3.rejectTime != 0) {
                                pw.print("; rejectTime=");
                                TimeUtils.formatDuration(now - op3.rejectTime, pw);
                                pw.print(" ago");
                            }
                            if (op3.duration == -1) {
                                pw.print(" (running)");
                            } else if (op3.duration != 0) {
                                pw.print("; duration=");
                                TimeUtils.formatDuration(op3.duration, pw);
                            }
                            pw.println();
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

        Restriction(Restriction restriction) {
            this();
        }

        private Restriction() {
            this.exceptionPackages = NO_EXCEPTIONS;
        }
    }

    public void setUserRestrictions(Bundle restrictions, IBinder token, int userHandle) {
        checkSystemUid("setUserRestrictions");
        Preconditions.checkNotNull(restrictions);
        Preconditions.checkNotNull(token);
        for (int i = 0; i < 69; i++) {
            String restriction = AppOpsManager.opToRestriction(i);
            if (restriction != null) {
                setUserRestrictionNoCheck(i, restrictions.getBoolean(restriction, false), token, userHandle, null);
            }
        }
    }

    public void setUserRestriction(int code, boolean restricted, IBinder token, int userHandle, String[] exceptionPackages) {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.MANAGE_APP_OPS_RESTRICTIONS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
        if (userHandle != UserHandle.getCallingUserId() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") != 0) {
            throw new SecurityException("Need INTERACT_ACROSS_USERS_FULL or INTERACT_ACROSS_USERS to interact cross user ");
        }
        verifyIncomingOp(code);
        Preconditions.checkNotNull(token);
        setUserRestrictionNoCheck(code, restricted, token, userHandle, exceptionPackages);
    }

    private void setUserRestrictionNoCheck(int code, boolean restricted, IBinder token, int userHandle, String[] exceptionPackages) {
        ClientRestrictionState restrictionState = this.mOpUserRestrictions.get(token);
        if (restrictionState == null) {
            try {
                restrictionState = new ClientRestrictionState(token);
                this.mOpUserRestrictions.put(token, restrictionState);
            } catch (RemoteException e) {
                return;
            }
        }
        if (restrictionState.setRestriction(code, restricted, exceptionPackages, userHandle)) {
            notifyWatchersOfChange(code);
        }
        if (!restrictionState.isDefault()) {
            return;
        }
        this.mOpUserRestrictions.remove(token);
        restrictionState.destroy();
    }

    private void notifyWatchersOfChange(int code) {
        synchronized (this) {
            ArrayList<Callback> callbacks = this.mOpModeWatchers.get(code);
            if (callbacks == null) {
                return;
            }
            ArrayList<Callback> clonedCallbacks = new ArrayList<>(callbacks);
            long identity = Binder.clearCallingIdentity();
            try {
                int callbackCount = clonedCallbacks.size();
                for (int i = 0; i < callbackCount; i++) {
                    Callback callback = clonedCallbacks.get(i);
                    try {
                        callback.mCallback.opChanged(code, -1, (String) null);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Error dispatching op op change", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void removeUser(int userHandle) throws RemoteException {
        checkSystemUid("removeUser");
        int tokenCount = this.mOpUserRestrictions.size();
        for (int i = tokenCount - 1; i >= 0; i--) {
            ClientRestrictionState opRestrictions = this.mOpUserRestrictions.valueAt(i);
            opRestrictions.removeUser(userHandle);
        }
    }

    private void checkSystemUid(String function) {
        int uid = Binder.getCallingUid();
        if (uid == 1000) {
        } else {
            throw new SecurityException(function + " must by called by the system");
        }
    }

    private static String resolvePackageName(int uid, String packageName) {
        if (uid == 0) {
            return "root";
        }
        if (uid == 2000) {
            return "com.android.shell";
        }
        if (uid == 1000 && packageName == null) {
            return "android";
        }
        return packageName;
    }

    private static String[] getPackagesForUid(int uid) {
        String[] packageNames = null;
        try {
            packageNames = AppGlobals.getPackageManager().getPackagesForUid(uid);
        } catch (RemoteException e) {
        }
        if (packageNames == null) {
            return EmptyArray.STRING;
        }
        return packageNames;
    }

    private final class ClientRestrictionState implements IBinder.DeathRecipient {
        SparseArray<String[]> perUserExcludedPackages;
        SparseArray<boolean[]> perUserRestrictions;
        private final IBinder token;

        public ClientRestrictionState(IBinder token) throws RemoteException {
            token.linkToDeath(this, 0);
            this.token = token;
        }

        public boolean setRestriction(int code, boolean restricted, String[] excludedPackages, int userId) {
            boolean changed = false;
            if (this.perUserRestrictions == null && restricted) {
                this.perUserRestrictions = new SparseArray<>();
            }
            if (this.perUserRestrictions == null) {
                return false;
            }
            boolean[] userRestrictions = this.perUserRestrictions.get(userId);
            if (userRestrictions == null && restricted) {
                userRestrictions = new boolean[69];
                this.perUserRestrictions.put(userId, userRestrictions);
            }
            if (userRestrictions != null && userRestrictions[code] != restricted) {
                userRestrictions[code] = restricted;
                if (!restricted && isDefault(userRestrictions)) {
                    this.perUserRestrictions.remove(userId);
                    userRestrictions = null;
                }
                changed = true;
            }
            if (userRestrictions != null) {
                boolean noExcludedPackages = ArrayUtils.isEmpty(excludedPackages);
                if (this.perUserExcludedPackages == null && !noExcludedPackages) {
                    this.perUserExcludedPackages = new SparseArray<>();
                }
                if (this.perUserExcludedPackages != null && !Arrays.equals(excludedPackages, this.perUserExcludedPackages.get(userId))) {
                    if (noExcludedPackages) {
                        this.perUserExcludedPackages.remove(userId);
                        if (this.perUserExcludedPackages.size() <= 0) {
                            this.perUserExcludedPackages = null;
                        }
                    } else {
                        this.perUserExcludedPackages.put(userId, excludedPackages);
                    }
                    return true;
                }
                return changed;
            }
            return changed;
        }

        public boolean hasRestriction(int restriction, String packageName, int userId) {
            boolean[] restrictions;
            String[] perUserExclusions;
            if (this.perUserRestrictions == null || (restrictions = this.perUserRestrictions.get(userId)) == null || !restrictions[restriction]) {
                return false;
            }
            return this.perUserExcludedPackages == null || (perUserExclusions = this.perUserExcludedPackages.get(userId)) == null || !ArrayUtils.contains(perUserExclusions, packageName);
        }

        public void removeUser(int userId) {
            if (this.perUserExcludedPackages == null) {
                return;
            }
            this.perUserExcludedPackages.remove(userId);
            if (this.perUserExcludedPackages.size() > 0) {
                return;
            }
            this.perUserExcludedPackages = null;
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
                int userCount = this.perUserRestrictions.size();
                for (int i = 0; i < userCount; i++) {
                    boolean[] restrictions = this.perUserRestrictions.valueAt(i);
                    int restrictionCount = restrictions.length;
                    for (int j = 0; j < restrictionCount; j++) {
                        if (restrictions[j]) {
                            final int changedCode = j;
                            AppOpsService.this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ClientRestrictionState.this.m181x541d71a2(changedCode);
                                }
                            });
                        }
                    }
                }
                destroy();
            }
        }

        void m181x541d71a2(int changedCode) {
            AppOpsService.this.notifyWatchersOfChange(changedCode);
        }

        public void destroy() {
            this.token.unlinkToDeath(this, 0);
        }

        private boolean isDefault(boolean[] array) {
            if (ArrayUtils.isEmpty(array)) {
                return true;
            }
            for (boolean value : array) {
                if (value) {
                    return false;
                }
            }
            return true;
        }
    }

    boolean operationFallBackCheck(Ops ops, int oriCode, int uid, String pkg) {
        return isOpAllowed(null, ops, oriCode, uid, pkg);
    }

    boolean operationFallBackCheck(UidState uidState, int oriCode, int uid, String pkg) {
        return isOpAllowed(uidState, null, oriCode, uid, pkg);
    }

    boolean isOpAllowed(UidState uidState, Ops ops, int oriCode, int uid, String pkg) {
        boolean result = false;
        int mode = 1;
        if (CtaUtils.isCtaSupported() && !callerIsPkgInstaller(Binder.getCallingUid()) && oriCode == 0) {
            if (uidState != null) {
                mode = uidState.opModes.get(1);
            } else if (ops != null) {
                Op op = getOpLocked(ops, 1, false);
                mode = op.mode;
            }
            result = mode == 0;
            if (ENG_LOAD) {
                Log.d(TAG, "operationFallBackCheck() - FINE_LOC of  pkg = " + pkg + " uid = " + uid + "is allowed? result = " + result);
            }
        }
        return result;
    }

    boolean callerIsPkgInstaller(int callingUid) {
        String callingPkg;
        if (!CtaUtils.isCtaSupported() || (callingPkg = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid())) == null) {
            return false;
        }
        String permCtrlPkgName = this.mContext.getPackageManager().getPermissionControllerPackageName();
        if (!callingPkg.equals(permCtrlPkgName)) {
            return false;
        }
        return true;
    }
}
