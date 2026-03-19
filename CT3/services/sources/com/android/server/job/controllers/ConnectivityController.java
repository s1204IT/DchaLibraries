package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyListener;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ConnectivityController extends StateController implements ConnectivityManager.OnNetworkActiveListener {
    private static final String TAG = "JobScheduler.Conn";
    private static ConnectivityController mSingleton;
    private static Object sCreationLock = new Object();
    private final ConnectivityManager mConnManager;
    private BroadcastReceiver mConnectivityReceiver;
    private INetworkPolicyListener mNetPolicyListener;
    private final NetworkPolicyManager mNetPolicyManager;

    @GuardedBy("mLock")
    private final ArrayList<JobStatus> mTrackedJobs;

    public static ConnectivityController get(JobSchedulerService jms) {
        ConnectivityController connectivityController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new ConnectivityController(jms, jms.getContext(), jms.getLock());
            }
            connectivityController = mSingleton;
        }
        return connectivityController;
    }

    private ConnectivityController(StateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        this.mTrackedJobs = new ArrayList<>();
        this.mConnectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                ConnectivityController.this.updateTrackedJobs(-1);
            }
        };
        this.mNetPolicyListener = new INetworkPolicyListener.Stub() {
            public void onUidRulesChanged(int uid, int uidRules) {
                ConnectivityController.this.updateTrackedJobs(uid);
            }

            public void onMeteredIfacesChanged(String[] meteredIfaces) {
                ConnectivityController.this.updateTrackedJobs(-1);
            }

            public void onRestrictBackgroundChanged(boolean restrictBackground) {
                ConnectivityController.this.updateTrackedJobs(-1);
            }

            public void onRestrictBackgroundWhitelistChanged(int uid, boolean whitelisted) {
                ConnectivityController.this.updateTrackedJobs(uid);
            }

            public void onRestrictBackgroundBlacklistChanged(int uid, boolean blacklisted) {
                ConnectivityController.this.updateTrackedJobs(uid);
            }
        };
        this.mConnManager = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mNetPolicyManager = (NetworkPolicyManager) this.mContext.getSystemService(NetworkPolicyManager.class);
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiverAsUser(this.mConnectivityReceiver, UserHandle.SYSTEM, intentFilter, null, null);
        this.mNetPolicyManager.registerListener(this.mNetPolicyListener);
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        if (!jobStatus.hasConnectivityConstraint() && !jobStatus.hasUnmeteredConstraint() && !jobStatus.hasNotRoamingConstraint()) {
            return;
        }
        updateConstraintsSatisfied(jobStatus);
        this.mTrackedJobs.add(jobStatus);
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
        if (!jobStatus.hasConnectivityConstraint() && !jobStatus.hasUnmeteredConstraint() && !jobStatus.hasNotRoamingConstraint()) {
            return;
        }
        this.mTrackedJobs.remove(jobStatus);
    }

    private boolean updateConstraintsSatisfied(JobStatus jobStatus) {
        boolean ignoreBlocked = (jobStatus.getFlags() & 1) != 0;
        NetworkInfo info = this.mConnManager.getActiveNetworkInfoForUid(jobStatus.getSourceUid(), ignoreBlocked);
        boolean connected = info != null ? info.isConnected() : false;
        boolean unmetered = connected && !info.isMetered();
        boolean notRoaming = connected && !info.isRoaming();
        boolean changed = jobStatus.setConnectivityConstraintSatisfied(connected);
        return changed | jobStatus.setUnmeteredConstraintSatisfied(unmetered) | jobStatus.setNotRoamingConstraintSatisfied(notRoaming);
    }

    private void updateTrackedJobs(int uid) {
        synchronized (this.mLock) {
            boolean changed = false;
            for (int i = 0; i < this.mTrackedJobs.size(); i++) {
                JobStatus js = this.mTrackedJobs.get(i);
                if (uid == -1 || uid == js.getSourceUid()) {
                    changed |= updateConstraintsSatisfied(js);
                }
            }
            if (changed) {
                this.mStateChangedListener.onControllerStateChanged();
            }
        }
    }

    @Override
    public synchronized void onNetworkActive() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mTrackedJobs.size(); i++) {
                JobStatus js = this.mTrackedJobs.get(i);
                if (js.isReady()) {
                    if (DEBUG) {
                        Slog.d(TAG, "Running " + js + " due to network activity.");
                    }
                    this.mStateChangedListener.onRunJobNow(js);
                }
            }
        }
    }

    @Override
    public void dumpControllerStateLocked(PrintWriter pw, int filterUid) {
        pw.println("Connectivity.");
        pw.print("Tracking ");
        pw.print(this.mTrackedJobs.size());
        pw.println(":");
        for (int i = 0; i < this.mTrackedJobs.size(); i++) {
            JobStatus js = this.mTrackedJobs.get(i);
            if (js.shouldDump(filterUid)) {
                pw.print("  #");
                js.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, js.getSourceUid());
                pw.print(": C=");
                pw.print(js.hasConnectivityConstraint());
                pw.print(": UM=");
                pw.print(js.hasUnmeteredConstraint());
                pw.print(": NR=");
                pw.println(js.hasNotRoamingConstraint());
            }
        }
    }
}
