package com.android.server.job.controllers;

import android.net.ConnectivityManager;
import android.net.INetworkPolicyListener;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.net.NetworkRequest;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import java.util.Objects;
import java.util.function.Predicate;

public final class ConnectivityController extends StateController implements ConnectivityManager.OnNetworkActiveListener {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Connectivity";
    private final ConnectivityManager mConnManager;
    private final INetworkPolicyListener mNetPolicyListener;
    private final NetworkPolicyManager mNetPolicyManager;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;

    @GuardedBy("mLock")
    private final ArraySet<JobStatus> mTrackedJobs;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public ConnectivityController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.mTrackedJobs = new ArraySet<>();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (ConnectivityController.DEBUG) {
                    Slog.v(ConnectivityController.TAG, "onCapabilitiesChanged: " + network);
                }
                ConnectivityController.this.updateTrackedJobs(-1, network);
            }

            @Override
            public void onLost(Network network) {
                if (ConnectivityController.DEBUG) {
                    Slog.v(ConnectivityController.TAG, "onLost: " + network);
                }
                ConnectivityController.this.updateTrackedJobs(-1, network);
            }
        };
        this.mNetPolicyListener = new NetworkPolicyManager.Listener() {
            public void onUidRulesChanged(int i, int i2) {
                if (ConnectivityController.DEBUG) {
                    Slog.v(ConnectivityController.TAG, "onUidRulesChanged: " + i);
                }
                ConnectivityController.this.updateTrackedJobs(i, null);
            }
        };
        this.mConnManager = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mNetPolicyManager = (NetworkPolicyManager) this.mContext.getSystemService(NetworkPolicyManager.class);
        this.mConnManager.registerNetworkCallback(new NetworkRequest.Builder().clearCapabilities().build(), this.mNetworkCallback);
        this.mNetPolicyManager.registerListener(this.mNetPolicyListener);
    }

    @Override
    @GuardedBy("mLock")
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus.hasConnectivityConstraint()) {
            updateConstraintsSatisfied(jobStatus);
            this.mTrackedJobs.add(jobStatus);
            jobStatus.setTrackingController(2);
        }
    }

    @Override
    @GuardedBy("mLock")
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        if (jobStatus.clearTrackingController(2)) {
            this.mTrackedJobs.remove(jobStatus);
        }
    }

    private static boolean isInsane(JobStatus jobStatus, Network network, NetworkCapabilities networkCapabilities, JobSchedulerService.Constants constants) {
        long estimatedNetworkBytes = jobStatus.getEstimatedNetworkBytes();
        if (estimatedNetworkBytes == -1) {
            return false;
        }
        long jMinBandwidth = NetworkCapabilities.minBandwidth(networkCapabilities.getLinkDownstreamBandwidthKbps(), networkCapabilities.getLinkUpstreamBandwidthKbps());
        if (jMinBandwidth == 0) {
            return false;
        }
        long j = (1000 * estimatedNetworkBytes) / ((1024 * jMinBandwidth) / 8);
        if (j <= 600000) {
            return false;
        }
        Slog.w(TAG, "Estimated " + estimatedNetworkBytes + " bytes over " + jMinBandwidth + " kbps network would take " + j + "ms; that's insane!");
        return true;
    }

    private static boolean isCongestionDelayed(JobStatus jobStatus, Network network, NetworkCapabilities networkCapabilities, JobSchedulerService.Constants constants) {
        return !networkCapabilities.hasCapability(20) && jobStatus.getFractionRunTime() < constants.CONN_CONGESTION_DELAY_FRAC;
    }

    private static boolean isStrictSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities networkCapabilities, JobSchedulerService.Constants constants) {
        return jobStatus.getJob().getRequiredNetwork().networkCapabilities.satisfiedByNetworkCapabilities(networkCapabilities);
    }

    private static boolean isRelaxedSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities networkCapabilities, JobSchedulerService.Constants constants) {
        return jobStatus.getJob().isPrefetch() && new NetworkCapabilities(jobStatus.getJob().getRequiredNetwork().networkCapabilities).removeCapability(11).satisfiedByNetworkCapabilities(networkCapabilities) && jobStatus.getFractionRunTime() > constants.CONN_PREFETCH_RELAX_FRAC;
    }

    @VisibleForTesting
    static boolean isSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities networkCapabilities, JobSchedulerService.Constants constants) {
        if (network == null || networkCapabilities == null || isInsane(jobStatus, network, networkCapabilities, constants) || isCongestionDelayed(jobStatus, network, networkCapabilities, constants)) {
            return false;
        }
        return isStrictSatisfied(jobStatus, network, networkCapabilities, constants) || isRelaxedSatisfied(jobStatus, network, networkCapabilities, constants);
    }

    private boolean updateConstraintsSatisfied(JobStatus jobStatus) {
        Network activeNetworkForUid = this.mConnManager.getActiveNetworkForUid(jobStatus.getSourceUid());
        return updateConstraintsSatisfied(jobStatus, activeNetworkForUid, this.mConnManager.getNetworkCapabilities(activeNetworkForUid));
    }

    private boolean updateConstraintsSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities networkCapabilities) {
        NetworkInfo networkInfoForUid = this.mConnManager.getNetworkInfoForUid(network, jobStatus.getSourceUid(), (jobStatus.getFlags() & 1) != 0);
        boolean z = networkInfoForUid != null && networkInfoForUid.isConnected();
        boolean zIsSatisfied = isSatisfied(jobStatus, network, networkCapabilities, this.mConstants);
        boolean connectivityConstraintSatisfied = jobStatus.setConnectivityConstraintSatisfied(z && zIsSatisfied);
        jobStatus.network = network;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("Connectivity ");
            sb.append(connectivityConstraintSatisfied ? "CHANGED" : "unchanged");
            sb.append(" for ");
            sb.append(jobStatus);
            sb.append(": connected=");
            sb.append(z);
            sb.append(" satisfied=");
            sb.append(zIsSatisfied);
            Slog.i(TAG, sb.toString());
        }
        return connectivityConstraintSatisfied;
    }

    private void updateTrackedJobs(int i, Network network) {
        synchronized (this.mLock) {
            SparseArray sparseArray = new SparseArray();
            SparseArray sparseArray2 = new SparseArray();
            boolean zUpdateConstraintsSatisfied = false;
            for (int size = this.mTrackedJobs.size() - 1; size >= 0; size--) {
                JobStatus jobStatusValueAt = this.mTrackedJobs.valueAt(size);
                int sourceUid = jobStatusValueAt.getSourceUid();
                if (i == -1 || i == sourceUid) {
                    Network activeNetworkForUid = (Network) sparseArray.get(sourceUid);
                    if (activeNetworkForUid == null) {
                        activeNetworkForUid = this.mConnManager.getActiveNetworkForUid(sourceUid);
                        sparseArray.put(sourceUid, activeNetworkForUid);
                    }
                    boolean z = network == null || Objects.equals(network, activeNetworkForUid);
                    boolean z2 = !Objects.equals(jobStatusValueAt.network, activeNetworkForUid);
                    if (z || z2) {
                        int i2 = activeNetworkForUid != null ? activeNetworkForUid.netId : -1;
                        NetworkCapabilities networkCapabilities = (NetworkCapabilities) sparseArray2.get(i2);
                        if (networkCapabilities == null) {
                            networkCapabilities = this.mConnManager.getNetworkCapabilities(activeNetworkForUid);
                            sparseArray2.put(i2, networkCapabilities);
                        }
                        zUpdateConstraintsSatisfied |= updateConstraintsSatisfied(jobStatusValueAt, activeNetworkForUid, networkCapabilities);
                    }
                }
            }
            if (zUpdateConstraintsSatisfied) {
                this.mStateChangedListener.onControllerStateChanged();
            }
        }
    }

    @Override
    public void onNetworkActive() {
        synchronized (this.mLock) {
            for (int size = this.mTrackedJobs.size() - 1; size >= 0; size--) {
                JobStatus jobStatusValueAt = this.mTrackedJobs.valueAt(size);
                if (jobStatusValueAt.isReady()) {
                    if (DEBUG) {
                        Slog.d(TAG, "Running " + jobStatusValueAt + " due to network activity.");
                    }
                    this.mStateChangedListener.onRunJobNow(jobStatusValueAt);
                }
            }
        }
    }

    @Override
    @GuardedBy("mLock")
    public void dumpControllerStateLocked(IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        for (int i = 0; i < this.mTrackedJobs.size(); i++) {
            JobStatus jobStatusValueAt = this.mTrackedJobs.valueAt(i);
            if (predicate.test(jobStatusValueAt)) {
                indentingPrintWriter.print("#");
                jobStatusValueAt.printUniqueId(indentingPrintWriter);
                indentingPrintWriter.print(" from ");
                UserHandle.formatUid(indentingPrintWriter, jobStatusValueAt.getSourceUid());
                indentingPrintWriter.print(": ");
                indentingPrintWriter.print(jobStatusValueAt.getJob().getRequiredNetwork());
                indentingPrintWriter.println();
            }
        }
    }

    @Override
    @GuardedBy("mLock")
    public void dumpControllerStateLocked(ProtoOutputStream protoOutputStream, long j, Predicate<JobStatus> predicate) {
        long jStart = protoOutputStream.start(j);
        long jStart2 = protoOutputStream.start(1146756268035L);
        for (int i = 0; i < this.mTrackedJobs.size(); i++) {
            JobStatus jobStatusValueAt = this.mTrackedJobs.valueAt(i);
            if (predicate.test(jobStatusValueAt)) {
                long jStart3 = protoOutputStream.start(2246267895810L);
                jobStatusValueAt.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1120986464258L, jobStatusValueAt.getSourceUid());
                NetworkRequest requiredNetwork = jobStatusValueAt.getJob().getRequiredNetwork();
                if (requiredNetwork != null) {
                    requiredNetwork.writeToProto(protoOutputStream, 1146756268035L);
                }
                protoOutputStream.end(jStart3);
            }
        }
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }
}
