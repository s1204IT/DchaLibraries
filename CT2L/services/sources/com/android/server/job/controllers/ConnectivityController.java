package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.ServiceManager;
import android.os.UserHandle;
import com.android.server.ConnectivityService;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class ConnectivityController extends StateController implements ConnectivityManager.OnNetworkActiveListener {
    private static final String TAG = "JobScheduler.Conn";
    private static ConnectivityController mSingleton;
    private static Object sCreationLock = new Object();
    private final BroadcastReceiver mConnectivityChangedReceiver;
    private boolean mNetworkConnected;
    private boolean mNetworkUnmetered;
    private final List<JobStatus> mTrackedJobs;

    public static ConnectivityController get(JobSchedulerService jms) {
        ConnectivityController connectivityController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new ConnectivityController(jms, jms.getContext());
            }
            connectivityController = mSingleton;
        }
        return connectivityController;
    }

    private ConnectivityController(StateChangedListener stateChangedListener, Context context) {
        super(stateChangedListener, context);
        this.mTrackedJobs = new LinkedList();
        this.mConnectivityChangedReceiver = new ConnectivityChangedReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiverAsUser(this.mConnectivityChangedReceiver, UserHandle.ALL, intentFilter, null, null);
        ConnectivityService cs = (ConnectivityService) ServiceManager.getService("connectivity");
        if (cs != null) {
            if (cs.getActiveNetworkInfo() != null) {
                this.mNetworkConnected = cs.getActiveNetworkInfo().isConnected();
            }
            this.mNetworkUnmetered = this.mNetworkConnected && !cs.isActiveNetworkMetered();
        }
    }

    @Override
    public void maybeStartTrackingJob(JobStatus jobStatus) {
        if (jobStatus.hasConnectivityConstraint() || jobStatus.hasUnmeteredConstraint()) {
            synchronized (this.mTrackedJobs) {
                jobStatus.connectivityConstraintSatisfied.set(this.mNetworkConnected);
                jobStatus.unmeteredConstraintSatisfied.set(this.mNetworkUnmetered);
                this.mTrackedJobs.add(jobStatus);
            }
        }
    }

    @Override
    public void maybeStopTrackingJob(JobStatus jobStatus) {
        if (jobStatus.hasConnectivityConstraint() || jobStatus.hasUnmeteredConstraint()) {
            synchronized (this.mTrackedJobs) {
                this.mTrackedJobs.remove(jobStatus);
            }
        }
    }

    private void updateTrackedJobs(int userId) {
        synchronized (this.mTrackedJobs) {
            boolean changed = false;
            for (JobStatus js : this.mTrackedJobs) {
                if (js.getUserId() == userId) {
                    boolean prevIsConnected = js.connectivityConstraintSatisfied.getAndSet(this.mNetworkConnected);
                    boolean prevIsMetered = js.unmeteredConstraintSatisfied.getAndSet(this.mNetworkUnmetered);
                    if (prevIsConnected != this.mNetworkConnected || prevIsMetered != this.mNetworkUnmetered) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                this.mStateChangedListener.onControllerStateChanged();
            }
        }
    }

    @Override
    public synchronized void onNetworkActive() {
        synchronized (this.mTrackedJobs) {
            for (JobStatus js : this.mTrackedJobs) {
                if (js.isReady()) {
                    this.mStateChangedListener.onRunJobNow(js);
                }
            }
        }
    }

    class ConnectivityChangedReceiver extends BroadcastReceiver {
        ConnectivityChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                int networkType = intent.getIntExtra("networkType", -1);
                ConnectivityManager connManager = (ConnectivityManager) context.getSystemService("connectivity");
                NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                int userid = context.getUserId();
                if (activeNetwork == null) {
                    ConnectivityController.this.mNetworkUnmetered = false;
                    ConnectivityController.this.mNetworkConnected = false;
                    ConnectivityController.this.updateTrackedJobs(userid);
                } else if (activeNetwork.getType() == networkType) {
                    ConnectivityController.this.mNetworkUnmetered = false;
                    ConnectivityController.this.mNetworkConnected = !intent.getBooleanExtra("noConnectivity", false);
                    if (ConnectivityController.this.mNetworkConnected) {
                        ConnectivityController.this.mNetworkUnmetered = connManager.isActiveNetworkMetered() ? false : true;
                    }
                    ConnectivityController.this.updateTrackedJobs(userid);
                }
            }
        }
    }

    @Override
    public void dumpControllerState(PrintWriter pw) {
        pw.println("Conn.");
        pw.println("connected: " + this.mNetworkConnected + " unmetered: " + this.mNetworkUnmetered);
        for (JobStatus js : this.mTrackedJobs) {
            pw.println(String.valueOf(js.hashCode()).substring(0, 3) + "..: C=" + js.hasConnectivityConstraint() + ", UM=" + js.hasUnmeteredConstraint());
        }
    }
}
