package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AppOpsService extends IAppOpsService.Stub {
    static final boolean DEBUG = false;
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
                AppOpsService.this.mWriteScheduled = AppOpsService.DEBUG;
                AppOpsService.this.mFastWriteScheduled = AppOpsService.DEBUG;
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
    final SparseArray<HashMap<String, Ops>> mUidOps = new SparseArray<>();
    private final SparseArray<boolean[]> mOpRestrictions = new SparseArray<>();
    final SparseArray<ArrayList<Callback>> mOpModeWatchers = new SparseArray<>();
    final ArrayMap<String, ArrayList<Callback>> mPackageModeWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, Callback> mModeWatchers = new ArrayMap<>();
    final SparseArray<SparseArray<Restriction>> mAudioRestrictions = new SparseArray<>();
    final ArrayMap<IBinder, ClientState> mClients = new ArrayMap<>();

    public static final class Ops extends SparseArray<Op> {
        public final boolean isPrivileged;
        public final String packageName;
        public final int uid;

        public Ops(String _packageName, int _uid, boolean _isPrivileged) {
            this.packageName = _packageName;
            this.uid = _uid;
            this.isPrivileged = _isPrivileged;
        }
    }

    public static final class Op {
        public int duration;
        public int mode;
        public int nesting;
        public final int op;
        public final String packageName;
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
        int curUid;
        synchronized (this) {
            boolean changed = DEBUG;
            for (int i = 0; i < this.mUidOps.size(); i++) {
                HashMap<String, Ops> pkgs = this.mUidOps.valueAt(i);
                Iterator<Ops> it = pkgs.values().iterator();
                while (it.hasNext()) {
                    Ops ops = it.next();
                    try {
                        curUid = this.mContext.getPackageManager().getPackageUid(ops.packageName, UserHandle.getUserId(ops.uid));
                    } catch (PackageManager.NameNotFoundException e) {
                        curUid = -1;
                    }
                    if (curUid != ops.uid) {
                        Slog.i(TAG, "Pruning old package " + ops.packageName + "/" + ops.uid + ": new uid=" + curUid);
                        it.remove();
                        changed = true;
                    }
                }
                if (pkgs.size() <= 0) {
                    this.mUidOps.removeAt(i);
                }
            }
            if (changed) {
                scheduleFastWriteLocked();
            }
        }
    }

    public void packageRemoved(int uid, String packageName) {
        synchronized (this) {
            HashMap<String, Ops> pkgs = this.mUidOps.get(uid);
            if (pkgs != null && pkgs.remove(packageName) != null) {
                if (pkgs.size() <= 0) {
                    this.mUidOps.remove(uid);
                }
                scheduleFastWriteLocked();
            }
        }
    }

    public void uidRemoved(int uid) {
        synchronized (this) {
            if (this.mUidOps.indexOfKey(uid) >= 0) {
                this.mUidOps.remove(uid);
                scheduleFastWriteLocked();
            }
        }
    }

    public void shutdown() {
        Slog.w(TAG, "Writing app ops before shutdown...");
        boolean doWrite = DEBUG;
        synchronized (this) {
            if (this.mWriteScheduled) {
                this.mWriteScheduled = DEBUG;
                doWrite = true;
            }
        }
        if (doWrite) {
            writeState();
        }
    }

    private ArrayList<AppOpsManager.OpEntry> collectOps(Ops pkgOps, int[] ops) {
        ArrayList<AppOpsManager.OpEntry> resOps = null;
        if (ops == null) {
            resOps = new ArrayList<>();
            for (int j = 0; j < pkgOps.size(); j++) {
                Op curOp = pkgOps.valueAt(j);
                resOps.add(new AppOpsManager.OpEntry(curOp.op, curOp.mode, curOp.time, curOp.rejectTime, curOp.duration));
            }
        } else {
            for (int i : ops) {
                Op curOp2 = pkgOps.get(i);
                if (curOp2 != null) {
                    if (resOps == null) {
                        resOps = new ArrayList<>();
                    }
                    resOps.add(new AppOpsManager.OpEntry(curOp2.op, curOp2.mode, curOp2.time, curOp2.rejectTime, curOp2.duration));
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
            int i = 0;
            while (i < this.mUidOps.size()) {
                try {
                    HashMap<String, Ops> packages = this.mUidOps.valueAt(i);
                    ArrayList<AppOpsManager.PackageOps> res3 = res2;
                    for (Ops pkgOps : packages.values()) {
                        try {
                            ArrayList<AppOpsManager.OpEntry> resOps = collectOps(pkgOps, ops);
                            if (resOps != null) {
                                res = res3 == null ? new ArrayList<>() : res3;
                                AppOpsManager.PackageOps resPackage = new AppOpsManager.PackageOps(pkgOps.packageName, pkgOps.uid, resOps);
                                res.add(resPackage);
                            } else {
                                res = res3;
                            }
                            res3 = res;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    i++;
                    res2 = res3;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return res2;
        }
    }

    public List<AppOpsManager.PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) {
        ArrayList<AppOpsManager.PackageOps> res = null;
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        synchronized (this) {
            Ops pkgOps = getOpsLocked(uid, packageName, DEBUG);
            if (pkgOps != null) {
                ArrayList<AppOpsManager.OpEntry> resOps = collectOps(pkgOps, ops);
                if (resOps != null) {
                    res = new ArrayList<>();
                    AppOpsManager.PackageOps resPackage = new AppOpsManager.PackageOps(pkgOps.packageName, pkgOps.uid, resOps);
                    res.add(resPackage);
                }
            }
        }
        return res;
    }

    private void pruneOp(Op op, int uid, String packageName) {
        Ops ops;
        HashMap<String, Ops> pkgOps;
        if (op.time == 0 && op.rejectTime == 0 && (ops = getOpsLocked(uid, packageName, DEBUG)) != null) {
            ops.remove(op.op);
            if (ops.size() <= 0 && (pkgOps = this.mUidOps.get(uid)) != null) {
                pkgOps.remove(ops.packageName);
                if (pkgOps.size() <= 0) {
                    this.mUidOps.remove(uid);
                }
            }
        }
    }

    public void setMode(int code, int uid, String packageName, int mode) throws Throwable {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
        verifyIncomingOp(code);
        ArrayList<Callback> repCbs = null;
        int code2 = AppOpsManager.opToSwitch(code);
        synchronized (this) {
            try {
                Op op = getOpLocked(code2, uid, packageName, true);
                if (op != null && op.mode != mode) {
                    op.mode = mode;
                    ArrayList<Callback> cbs = this.mOpModeWatchers.get(code2);
                    if (cbs != null) {
                        if (0 == 0) {
                            repCbs = new ArrayList<>();
                        }
                        repCbs.addAll(cbs);
                    }
                    ArrayList<Callback> repCbs2 = repCbs;
                    try {
                        ArrayList<Callback> cbs2 = this.mPackageModeWatchers.get(packageName);
                        if (cbs2 != null) {
                            repCbs = repCbs2 == null ? new ArrayList<>() : repCbs2;
                            repCbs.addAll(cbs2);
                        } else {
                            repCbs = repCbs2;
                        }
                        if (mode == AppOpsManager.opToDefaultMode(op.op)) {
                            pruneOp(op, uid, packageName);
                        }
                        scheduleFastWriteLocked();
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (repCbs != null) {
                    for (int i = 0; i < repCbs.size(); i++) {
                        try {
                            repCbs.get(i).mCallback.opChanged(code2, packageName);
                        } catch (RemoteException e) {
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private static HashMap<Callback, ArrayList<Pair<String, Integer>>> addCallbacks(HashMap<Callback, ArrayList<Pair<String, Integer>>> callbacks, String packageName, int op, ArrayList<Callback> cbs) {
        if (cbs != null) {
            if (callbacks == null) {
                callbacks = new HashMap<>();
            }
            for (int i = 0; i < cbs.size(); i++) {
                Callback cb = cbs.get(i);
                ArrayList<Pair<String, Integer>> reports = callbacks.get(cb);
                if (reports == null) {
                    reports = new ArrayList<>();
                    callbacks.put(cb, reports);
                }
                reports.add(new Pair<>(packageName, Integer.valueOf(op)));
            }
        }
        return callbacks;
    }

    public void resetAllModes(int reqUserId, String reqPackageName) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", callingPid, callingUid, null);
        int reqUserId2 = ActivityManager.handleIncomingUser(callingPid, callingUid, reqUserId, true, true, "resetAllModes", null);
        HashMap<Callback, ArrayList<Pair<String, Integer>>> callbacks = null;
        synchronized (this) {
            boolean changed = DEBUG;
            for (int i = this.mUidOps.size() - 1; i >= 0; i--) {
                HashMap<String, Ops> packages = this.mUidOps.valueAt(i);
                if (reqUserId2 == -1 || reqUserId2 == UserHandle.getUserId(this.mUidOps.keyAt(i))) {
                    Iterator<Map.Entry<String, Ops>> it = packages.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Ops> ent = it.next();
                        String packageName = ent.getKey();
                        if (reqPackageName == null || reqPackageName.equals(packageName)) {
                            Ops pkgOps = ent.getValue();
                            for (int j = pkgOps.size() - 1; j >= 0; j--) {
                                Op curOp = pkgOps.valueAt(j);
                                if (AppOpsManager.opAllowsReset(curOp.op) && curOp.mode != AppOpsManager.opToDefaultMode(curOp.op)) {
                                    curOp.mode = AppOpsManager.opToDefaultMode(curOp.op);
                                    changed = true;
                                    callbacks = addCallbacks(addCallbacks(callbacks, packageName, curOp.op, this.mOpModeWatchers.get(curOp.op)), packageName, curOp.op, this.mPackageModeWatchers.get(packageName));
                                    if (curOp.time == 0 && curOp.rejectTime == 0) {
                                        pkgOps.removeAt(j);
                                    }
                                }
                            }
                            if (pkgOps.size() == 0) {
                                it.remove();
                            }
                        }
                    }
                    if (packages.size() == 0) {
                        this.mUidOps.removeAt(i);
                    }
                }
            }
            if (changed) {
                scheduleFastWriteLocked();
            }
        }
        if (callbacks != null) {
            for (Map.Entry<Callback, ArrayList<Pair<String, Integer>>> ent2 : callbacks.entrySet()) {
                Callback cb = ent2.getKey();
                ArrayList<Pair<String, Integer>> reports = ent2.getValue();
                for (int i2 = 0; i2 < reports.size(); i2++) {
                    Pair<String, Integer> rep = reports.get(i2);
                    try {
                        cb.mCallback.opChanged(((Integer) rep.second).intValue(), (String) rep.first);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public void startWatchingMode(int op, String packageName, IAppOpsCallback callback) {
        synchronized (this) {
            int op2 = AppOpsManager.opToSwitch(op);
            Callback cb = this.mModeWatchers.get(callback.asBinder());
            if (cb == null) {
                cb = new Callback(callback);
                this.mModeWatchers.put(callback.asBinder(), cb);
            }
            if (op2 != -1) {
                ArrayList<Callback> cbs = this.mOpModeWatchers.get(op2);
                if (cbs == null) {
                    cbs = new ArrayList<>();
                    this.mOpModeWatchers.put(op2, cbs);
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
        }
    }

    public void stopWatchingMode(IAppOpsCallback callback) {
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
        int iOpToDefaultMode;
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        synchronized (this) {
            if (isOpRestricted(uid, code, packageName)) {
                iOpToDefaultMode = 1;
            } else {
                Op op = getOpLocked(AppOpsManager.opToSwitch(code), uid, packageName, DEBUG);
                if (op == null) {
                    iOpToDefaultMode = AppOpsManager.opToDefaultMode(code);
                } else {
                    iOpToDefaultMode = op.mode;
                }
            }
        }
        return iOpToDefaultMode;
    }

    public int checkAudioOperation(int code, int usage, int uid, String packageName) {
        synchronized (this) {
            int mode = checkRestrictionLocked(code, usage, uid, packageName);
            return mode != 0 ? mode : checkOperation(code, uid, packageName);
        }
    }

    private int checkRestrictionLocked(int code, int usage, int uid, String packageName) {
        Restriction r;
        SparseArray<Restriction> usageRestrictions = this.mAudioRestrictions.get(code);
        if (usageRestrictions == null || (r = usageRestrictions.get(usage)) == null || r.exceptionPackages.contains(packageName)) {
            return 0;
        }
        return r.mode;
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
                Restriction r = new Restriction();
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
    }

    public int checkPackage(int uid, String packageName) {
        int i;
        synchronized (this) {
            i = getOpsRawLocked(uid, packageName, true) != null ? 0 : 2;
        }
        return i;
    }

    public int noteOperation(int code, int uid, String packageName) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        synchronized (this) {
            Ops ops = getOpsLocked(uid, packageName, true);
            if (ops == null) {
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
            Op switchOp = switchCode != code ? getOpLocked(ops, switchCode, true) : op;
            if (switchOp.mode != 0) {
                op.rejectTime = System.currentTimeMillis();
                return switchOp.mode;
            }
            op.time = System.currentTimeMillis();
            op.rejectTime = 0L;
            return 0;
        }
    }

    public int startOperation(IBinder token, int code, int uid, String packageName) {
        int i = 1;
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        ClientState client = (ClientState) token;
        synchronized (this) {
            Ops ops = getOpsLocked(uid, packageName, true);
            if (ops == null) {
                i = 2;
            } else {
                Op op = getOpLocked(ops, code, true);
                if (!isOpRestricted(uid, code, packageName)) {
                    int switchCode = AppOpsManager.opToSwitch(code);
                    Op switchOp = switchCode != code ? getOpLocked(ops, switchCode, true) : op;
                    if (switchOp.mode != 0) {
                        op.rejectTime = System.currentTimeMillis();
                        i = switchOp.mode;
                    } else {
                        if (op.nesting == 0) {
                            op.time = System.currentTimeMillis();
                            op.rejectTime = 0L;
                            op.duration = -1;
                        }
                        op.nesting++;
                        if (client.mStartedOps != null) {
                            client.mStartedOps.add(op);
                        }
                        i = 0;
                    }
                }
            }
        }
        return i;
    }

    public void finishOperation(IBinder token, int code, int uid, String packageName) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        ClientState client = (ClientState) token;
        synchronized (this) {
            Op op = getOpLocked(code, uid, packageName, true);
            if (op != null) {
                if (client.mStartedOps != null && !client.mStartedOps.remove(op)) {
                    throw new IllegalStateException("Operation not started: uid" + op.uid + " pkg=" + op.packageName + " op=" + op.op);
                }
                finishOperationLocked(op);
            }
        }
    }

    void finishOperationLocked(Op op) {
        if (op.nesting <= 1) {
            if (op.nesting == 1) {
                op.duration = (int) (System.currentTimeMillis() - op.time);
                op.time += (long) op.duration;
            } else {
                Slog.w(TAG, "Finishing op nesting under-run: uid " + op.uid + " pkg " + op.packageName + " code " + op.op + " time=" + op.time + " duration=" + op.duration + " nesting=" + op.nesting);
            }
            op.nesting = 0;
            return;
        }
        op.nesting--;
    }

    private void verifyIncomingUid(int uid) {
        if (uid != Binder.getCallingUid() && Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
    }

    private void verifyIncomingOp(int op) {
        if (op >= 0 && op < 48) {
        } else {
            throw new IllegalArgumentException("Bad operation #" + op);
        }
    }

    private Ops getOpsLocked(int uid, String packageName, boolean edit) {
        if (uid == 0) {
            packageName = "root";
        } else if (uid == 2000) {
            packageName = "com.android.shell";
        }
        return getOpsRawLocked(uid, packageName, edit);
    }

    private Ops getOpsRawLocked(int uid, String packageName, boolean edit) {
        HashMap<String, Ops> pkgOps = this.mUidOps.get(uid);
        if (pkgOps == null) {
            if (!edit) {
                return null;
            }
            pkgOps = new HashMap<>();
            this.mUidOps.put(uid, pkgOps);
        }
        Ops ops = pkgOps.get(packageName);
        if (ops == null) {
            if (!edit) {
                return null;
            }
            boolean isPrivileged = DEBUG;
            if (uid != 0) {
                long ident = Binder.clearCallingIdentity();
                int pkgUid = -1;
                try {
                    try {
                        ApplicationInfo appInfo = ActivityThread.getPackageManager().getApplicationInfo(packageName, 0, UserHandle.getUserId(uid));
                        if (appInfo != null) {
                            pkgUid = appInfo.uid;
                            isPrivileged = (appInfo.flags & 1073741824) != 0 ? true : DEBUG;
                        } else if ("media".equals(packageName)) {
                            pkgUid = 1013;
                            isPrivileged = DEBUG;
                        }
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Could not contact PackageManager", e);
                    }
                    if (pkgUid != uid) {
                        Slog.w(TAG, "Bad call: specified package " + packageName + " under uid " + uid + " but it is really " + pkgUid);
                        return null;
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            Ops ops2 = new Ops(packageName, uid, isPrivileged);
            pkgOps.put(packageName, ops2);
            return ops2;
        }
        return ops;
    }

    private void scheduleWriteLocked() {
        if (!this.mWriteScheduled) {
            this.mWriteScheduled = true;
            this.mHandler.postDelayed(this.mWriteRunner, WRITE_DELAY);
        }
    }

    private void scheduleFastWriteLocked() {
        if (!this.mFastWriteScheduled) {
            this.mWriteScheduled = true;
            this.mFastWriteScheduled = true;
            this.mHandler.removeCallbacks(this.mWriteRunner);
            this.mHandler.postDelayed(this.mWriteRunner, 10000L);
        }
    }

    private Op getOpLocked(int code, int uid, String packageName, boolean edit) {
        Ops ops = getOpsLocked(uid, packageName, edit);
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
            op = new Op(ops.uid, ops.packageName, code);
            ops.put(code, op);
        }
        if (edit) {
            scheduleWriteLocked();
        }
        return op;
    }

    private boolean isOpRestricted(int uid, int code, String packageName) {
        int userHandle = UserHandle.getUserId(uid);
        boolean[] opRestrictions = this.mOpRestrictions.get(userHandle);
        if (opRestrictions == null || !opRestrictions[code]) {
            return DEBUG;
        }
        if (AppOpsManager.opAllowSystemBypassRestriction(code)) {
            synchronized (this) {
                Ops ops = getOpsLocked(uid, packageName, true);
                if (ops != null && ops.isPrivileged) {
                    return DEBUG;
                }
            }
        }
        return true;
    }

    void readState() {
        XmlPullParser parser;
        int type;
        synchronized (this.mFile) {
            synchronized (this) {
                try {
                    FileInputStream stream = this.mFile.openRead();
                    try {
                        try {
                            try {
                                try {
                                    try {
                                        try {
                                            parser = Xml.newPullParser();
                                            parser.setInput(stream, null);
                                            do {
                                                type = parser.next();
                                                if (type == 2) {
                                                    break;
                                                }
                                            } while (type != 1);
                                        } catch (XmlPullParserException e) {
                                            Slog.w(TAG, "Failed parsing " + e);
                                            if (0 == 0) {
                                                this.mUidOps.clear();
                                            }
                                            try {
                                                stream.close();
                                            } catch (IOException e2) {
                                            }
                                        }
                                    } catch (IOException e3) {
                                        Slog.w(TAG, "Failed parsing " + e3);
                                        if (0 == 0) {
                                            this.mUidOps.clear();
                                        }
                                        try {
                                            stream.close();
                                        } catch (IOException e4) {
                                        }
                                    }
                                } catch (IndexOutOfBoundsException e5) {
                                    Slog.w(TAG, "Failed parsing " + e5);
                                    if (0 == 0) {
                                        this.mUidOps.clear();
                                    }
                                    try {
                                        stream.close();
                                    } catch (IOException e6) {
                                    }
                                }
                            } catch (IllegalStateException e7) {
                                Slog.w(TAG, "Failed parsing " + e7);
                            }
                        } catch (NullPointerException e8) {
                            Slog.w(TAG, "Failed parsing " + e8);
                            if (0 == 0) {
                                this.mUidOps.clear();
                            }
                            try {
                                stream.close();
                            } catch (IOException e9) {
                            }
                        } catch (NumberFormatException e10) {
                            Slog.w(TAG, "Failed parsing " + e10);
                            if (0 == 0) {
                                this.mUidOps.clear();
                            }
                            try {
                                stream.close();
                            } catch (IOException e11) {
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
                                } else {
                                    Slog.w(TAG, "Unknown element under <app-ops>: " + parser.getName());
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            }
                        }
                        if (1 == 0) {
                            this.mUidOps.clear();
                        }
                        try {
                            stream.close();
                        } catch (IOException e12) {
                        }
                    } finally {
                        if (0 == 0) {
                            this.mUidOps.clear();
                        }
                        try {
                            stream.close();
                        } catch (IOException e13) {
                        }
                    }
                } catch (FileNotFoundException e14) {
                    Slog.i(TAG, "No existing app ops " + this.mFile.getBaseFile() + "; starting empty");
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
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals("uid")) {
                        readUid(parser, pkgName);
                    } else {
                        Slog.w(TAG, "Unknown element under <pkg>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            } else {
                return;
            }
        }
    }

    void readUid(XmlPullParser parser, String pkgName) throws XmlPullParserException, IOException, NumberFormatException {
        int uid = Integer.parseInt(parser.getAttributeValue(null, "n"));
        String isPrivilegedString = parser.getAttributeValue(null, "p");
        boolean isPrivileged = DEBUG;
        if (isPrivilegedString == null) {
            try {
                IPackageManager packageManager = ActivityThread.getPackageManager();
                if (packageManager != null) {
                    ApplicationInfo appInfo = ActivityThread.getPackageManager().getApplicationInfo(pkgName, 0, UserHandle.getUserId(uid));
                    if (appInfo != null) {
                        isPrivileged = (appInfo.flags & 1073741824) != 0 ? true : DEBUG;
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
            if (type != 3 || parser.getDepth() > outerDepth) {
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
                        HashMap<String, Ops> pkgOps = this.mUidOps.get(uid);
                        if (pkgOps == null) {
                            pkgOps = new HashMap<>();
                            this.mUidOps.put(uid, pkgOps);
                        }
                        Ops ops = pkgOps.get(pkgName);
                        if (ops == null) {
                            ops = new Ops(pkgName, uid, isPrivileged);
                            pkgOps.put(pkgName, ops);
                        }
                        ops.put(op.op, op);
                    } else {
                        Slog.w(TAG, "Unknown element under <pkg>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            } else {
                return;
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
                    fastXmlSerializer.setOutput(stream, "utf-8");
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, "app-ops");
                    if (allOps != null) {
                        String lastPkg = null;
                        for (int i = 0; i < allOps.size(); i++) {
                            AppOpsManager.PackageOps pkg = allOps.get(i);
                            if (!pkg.getPackageName().equals(lastPkg)) {
                                if (lastPkg != null) {
                                    fastXmlSerializer.endTag(null, "pkg");
                                }
                                lastPkg = pkg.getPackageName();
                                fastXmlSerializer.startTag(null, "pkg");
                                fastXmlSerializer.attribute(null, "n", lastPkg);
                            }
                            fastXmlSerializer.startTag(null, "uid");
                            fastXmlSerializer.attribute(null, "n", Integer.toString(pkg.getUid()));
                            synchronized (this) {
                                Ops ops = getOpsLocked(pkg.getUid(), pkg.getPackageName(), DEBUG);
                                if (ops != null) {
                                    fastXmlSerializer.attribute(null, "p", Boolean.toString(ops.isPrivileged));
                                } else {
                                    fastXmlSerializer.attribute(null, "p", Boolean.toString(DEBUG));
                                }
                            }
                            List<AppOpsManager.OpEntry> ops2 = pkg.getOps();
                            for (int j = 0; j < ops2.size(); j++) {
                                AppOpsManager.OpEntry op = ops2.get(j);
                                fastXmlSerializer.startTag(null, "op");
                                fastXmlSerializer.attribute(null, "n", Integer.toString(op.getOp()));
                                if (op.getMode() != AppOpsManager.opToDefaultMode(op.getOp())) {
                                    fastXmlSerializer.attribute(null, "m", Integer.toString(op.getMode()));
                                }
                                long time = op.getTime();
                                if (time != 0) {
                                    fastXmlSerializer.attribute(null, "t", Long.toString(time));
                                }
                                long time2 = op.getRejectTime();
                                if (time2 != 0) {
                                    fastXmlSerializer.attribute(null, "r", Long.toString(time2));
                                }
                                int dur = op.getDuration();
                                if (dur != 0) {
                                    fastXmlSerializer.attribute(null, "d", Integer.toString(dur));
                                }
                                fastXmlSerializer.endTag(null, "op");
                            }
                            fastXmlSerializer.endTag(null, "uid");
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

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ApOps service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this) {
            pw.println("Current AppOps Service state:");
            long now = System.currentTimeMillis();
            boolean needSep = DEBUG;
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
                boolean printedHeader = DEBUG;
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
            for (int i6 = 0; i6 < this.mUidOps.size(); i6++) {
                pw.print("  Uid ");
                UserHandle.formatUid(pw, this.mUidOps.keyAt(i6));
                pw.println(":");
                HashMap<String, Ops> pkgOps = this.mUidOps.valueAt(i6);
                for (Ops ops : pkgOps.values()) {
                    pw.print("    Package ");
                    pw.print(ops.packageName);
                    pw.println(":");
                    for (int j5 = 0; j5 < ops.size(); j5++) {
                        Op op3 = ops.valueAt(j5);
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

    private static final class Restriction {
        private static final ArraySet<String> NO_EXCEPTIONS = new ArraySet<>();
        ArraySet<String> exceptionPackages;
        int mode;

        private Restriction() {
            this.exceptionPackages = NO_EXCEPTIONS;
        }
    }

    public void setUserRestrictions(Bundle restrictions, int userHandle) throws RemoteException {
        checkSystemUid("setUserRestrictions");
        boolean[] opRestrictions = this.mOpRestrictions.get(userHandle);
        if (opRestrictions == null) {
            opRestrictions = new boolean[48];
            this.mOpRestrictions.put(userHandle, opRestrictions);
        }
        for (int i = 0; i < opRestrictions.length; i++) {
            String restriction = AppOpsManager.opToRestriction(i);
            if (restriction != null) {
                opRestrictions[i] = restrictions.getBoolean(restriction, DEBUG);
            } else {
                opRestrictions[i] = DEBUG;
            }
        }
    }

    public void removeUser(int userHandle) throws RemoteException {
        checkSystemUid("removeUser");
        this.mOpRestrictions.remove(userHandle);
    }

    private void checkSystemUid(String function) {
        int uid = Binder.getCallingUid();
        if (uid != 1000) {
            throw new SecurityException(function + " must by called by the system");
        }
    }
}
