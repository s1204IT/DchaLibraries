package com.android.server.connectivity;

import android.net.LinkProperties;
import android.net.metrics.DefaultNetworkEvent;
import android.os.SystemClock;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.BitUtils;
import com.android.internal.util.RingBuffer;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultNetworkMetrics {
    private static final int ROLLING_LOG_SIZE = 64;

    @GuardedBy("this")
    private DefaultNetworkEvent mCurrentDefaultNetwork;

    @GuardedBy("this")
    private boolean mIsCurrentlyValid;

    @GuardedBy("this")
    private int mLastTransports;

    @GuardedBy("this")
    private long mLastValidationTimeMs;
    public final long creationTimeMs = SystemClock.elapsedRealtime();

    @GuardedBy("this")
    private final List<DefaultNetworkEvent> mEvents = new ArrayList();

    @GuardedBy("this")
    private final RingBuffer<DefaultNetworkEvent> mEventsLog = new RingBuffer<>(DefaultNetworkEvent.class, 64);

    public DefaultNetworkMetrics() {
        newDefaultNetwork(this.creationTimeMs, null);
    }

    public synchronized void listEvents(PrintWriter printWriter) {
        printWriter.println("default network events:");
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        for (DefaultNetworkEvent defaultNetworkEvent : (DefaultNetworkEvent[]) this.mEventsLog.toArray()) {
            printEvent(jCurrentTimeMillis, printWriter, defaultNetworkEvent);
        }
        this.mCurrentDefaultNetwork.updateDuration(jElapsedRealtime);
        if (this.mIsCurrentlyValid) {
            updateValidationTime(jElapsedRealtime);
            this.mLastValidationTimeMs = jElapsedRealtime;
        }
        printEvent(jCurrentTimeMillis, printWriter, this.mCurrentDefaultNetwork);
    }

    public synchronized void listEventsAsProto(PrintWriter printWriter) {
        for (DefaultNetworkEvent defaultNetworkEvent : (DefaultNetworkEvent[]) this.mEventsLog.toArray()) {
            printWriter.print(IpConnectivityEventBuilder.toProto(defaultNetworkEvent));
        }
    }

    public synchronized void flushEvents(List<IpConnectivityLogClass.IpConnectivityEvent> list) {
        Iterator<DefaultNetworkEvent> it = this.mEvents.iterator();
        while (it.hasNext()) {
            list.add(IpConnectivityEventBuilder.toProto(it.next()));
        }
        this.mEvents.clear();
    }

    public synchronized void logDefaultNetworkValidity(long j, boolean z) {
        if (!z) {
            try {
                if (this.mIsCurrentlyValid) {
                    this.mIsCurrentlyValid = false;
                    updateValidationTime(j);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (z && !this.mIsCurrentlyValid) {
            this.mIsCurrentlyValid = true;
            this.mLastValidationTimeMs = j;
        }
    }

    private void updateValidationTime(long j) {
        this.mCurrentDefaultNetwork.validatedMs += j - this.mLastValidationTimeMs;
    }

    public synchronized void logDefaultNetworkEvent(long j, NetworkAgentInfo networkAgentInfo, NetworkAgentInfo networkAgentInfo2) {
        logCurrentDefaultNetwork(j, networkAgentInfo2);
        newDefaultNetwork(j, networkAgentInfo);
    }

    private void logCurrentDefaultNetwork(long j, NetworkAgentInfo networkAgentInfo) {
        if (this.mIsCurrentlyValid) {
            updateValidationTime(j);
        }
        DefaultNetworkEvent defaultNetworkEvent = this.mCurrentDefaultNetwork;
        defaultNetworkEvent.updateDuration(j);
        defaultNetworkEvent.previousTransports = this.mLastTransports;
        if (networkAgentInfo != null) {
            fillLinkInfo(defaultNetworkEvent, networkAgentInfo);
            defaultNetworkEvent.finalScore = networkAgentInfo.getCurrentScore();
        }
        if (defaultNetworkEvent.transports != 0) {
            this.mLastTransports = defaultNetworkEvent.transports;
        }
        this.mEvents.add(defaultNetworkEvent);
        this.mEventsLog.append(defaultNetworkEvent);
    }

    private void newDefaultNetwork(long j, NetworkAgentInfo networkAgentInfo) {
        DefaultNetworkEvent defaultNetworkEvent = new DefaultNetworkEvent(j);
        defaultNetworkEvent.durationMs = j;
        if (networkAgentInfo != null) {
            fillLinkInfo(defaultNetworkEvent, networkAgentInfo);
            defaultNetworkEvent.initialScore = networkAgentInfo.getCurrentScore();
            if (networkAgentInfo.lastValidated) {
                this.mIsCurrentlyValid = true;
                this.mLastValidationTimeMs = j;
            }
        } else {
            this.mIsCurrentlyValid = false;
        }
        this.mCurrentDefaultNetwork = defaultNetworkEvent;
    }

    private static void fillLinkInfo(DefaultNetworkEvent defaultNetworkEvent, NetworkAgentInfo networkAgentInfo) {
        LinkProperties linkProperties = networkAgentInfo.linkProperties;
        defaultNetworkEvent.netId = networkAgentInfo.network().netId;
        defaultNetworkEvent.transports = (int) (((long) defaultNetworkEvent.transports) | BitUtils.packBits(networkAgentInfo.networkCapabilities.getTransportTypes()));
        boolean z = false;
        defaultNetworkEvent.ipv4 |= linkProperties.hasIPv4Address() && linkProperties.hasIPv4DefaultRoute();
        boolean z2 = defaultNetworkEvent.ipv6;
        if (linkProperties.hasGlobalIPv6Address() && linkProperties.hasIPv6DefaultRoute()) {
            z = true;
        }
        defaultNetworkEvent.ipv6 = z2 | z;
    }

    private static void printEvent(long j, PrintWriter printWriter, DefaultNetworkEvent defaultNetworkEvent) {
        long j2 = j - defaultNetworkEvent.durationMs;
        printWriter.println(String.format("%tT.%tL: %s", Long.valueOf(j2), Long.valueOf(j2), defaultNetworkEvent));
    }
}
