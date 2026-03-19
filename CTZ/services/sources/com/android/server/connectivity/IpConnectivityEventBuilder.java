package com.android.server.connectivity;

import android.net.ConnectivityMetricsEvent;
import android.net.metrics.ApfProgramEvent;
import android.net.metrics.ApfStats;
import android.net.metrics.ConnectStats;
import android.net.metrics.DefaultNetworkEvent;
import android.net.metrics.DhcpClientEvent;
import android.net.metrics.DhcpErrorEvent;
import android.net.metrics.DnsEvent;
import android.net.metrics.IpManagerEvent;
import android.net.metrics.IpReachabilityEvent;
import android.net.metrics.NetworkEvent;
import android.net.metrics.RaEvent;
import android.net.metrics.ValidationProbeEvent;
import android.net.metrics.WakeupStats;
import android.os.Parcelable;
import android.util.SparseIntArray;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class IpConnectivityEventBuilder {
    private static final int[] IFNAME_LINKLAYERS;
    private static final String[] IFNAME_PREFIXES;
    private static final int KNOWN_PREFIX = 7;
    private static final int[] TRANSPORT_LINKLAYER_MAP = new int[7];

    private IpConnectivityEventBuilder() {
    }

    public static byte[] serialize(int i, List<IpConnectivityLogClass.IpConnectivityEvent> list) throws IOException {
        IpConnectivityLogClass.IpConnectivityLog ipConnectivityLog = new IpConnectivityLogClass.IpConnectivityLog();
        ipConnectivityLog.events = (IpConnectivityLogClass.IpConnectivityEvent[]) list.toArray(new IpConnectivityLogClass.IpConnectivityEvent[list.size()]);
        ipConnectivityLog.droppedEvents = i;
        if (ipConnectivityLog.events.length > 0 || i > 0) {
            ipConnectivityLog.version = 2;
        }
        return IpConnectivityLogClass.IpConnectivityLog.toByteArray(ipConnectivityLog);
    }

    public static List<IpConnectivityLogClass.IpConnectivityEvent> toProto(List<ConnectivityMetricsEvent> list) {
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<ConnectivityMetricsEvent> it = list.iterator();
        while (it.hasNext()) {
            IpConnectivityLogClass.IpConnectivityEvent proto = toProto(it.next());
            if (proto != null) {
                arrayList.add(proto);
            }
        }
        return arrayList;
    }

    public static IpConnectivityLogClass.IpConnectivityEvent toProto(ConnectivityMetricsEvent connectivityMetricsEvent) {
        IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEventBuildEvent = buildEvent(connectivityMetricsEvent.netId, connectivityMetricsEvent.transports, connectivityMetricsEvent.ifname);
        ipConnectivityEventBuildEvent.timeMs = connectivityMetricsEvent.timestamp;
        if (!setEvent(ipConnectivityEventBuildEvent, connectivityMetricsEvent.data)) {
            return null;
        }
        return ipConnectivityEventBuildEvent;
    }

    public static IpConnectivityLogClass.IpConnectivityEvent toProto(ConnectStats connectStats) {
        IpConnectivityLogClass.ConnectStatistics connectStatistics = new IpConnectivityLogClass.ConnectStatistics();
        connectStatistics.connectCount = connectStats.connectCount;
        connectStatistics.connectBlockingCount = connectStats.connectBlockingCount;
        connectStatistics.ipv6AddrCount = connectStats.ipv6ConnectCount;
        connectStatistics.latenciesMs = connectStats.latencies.toArray();
        connectStatistics.errnosCounters = toPairArray(connectStats.errnos);
        IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEventBuildEvent = buildEvent(connectStats.netId, connectStats.transports, null);
        ipConnectivityEventBuildEvent.setConnectStatistics(connectStatistics);
        return ipConnectivityEventBuildEvent;
    }

    public static IpConnectivityLogClass.IpConnectivityEvent toProto(DnsEvent dnsEvent) {
        IpConnectivityLogClass.DNSLookupBatch dNSLookupBatch = new IpConnectivityLogClass.DNSLookupBatch();
        dnsEvent.resize(dnsEvent.eventCount);
        dNSLookupBatch.eventTypes = bytesToInts(dnsEvent.eventTypes);
        dNSLookupBatch.returnCodes = bytesToInts(dnsEvent.returnCodes);
        dNSLookupBatch.latenciesMs = dnsEvent.latenciesMs;
        IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEventBuildEvent = buildEvent(dnsEvent.netId, dnsEvent.transports, null);
        ipConnectivityEventBuildEvent.setDnsLookupBatch(dNSLookupBatch);
        return ipConnectivityEventBuildEvent;
    }

    public static IpConnectivityLogClass.IpConnectivityEvent toProto(WakeupStats wakeupStats) {
        IpConnectivityLogClass.WakeupStats wakeupStats2 = new IpConnectivityLogClass.WakeupStats();
        wakeupStats.updateDuration();
        wakeupStats2.durationSec = wakeupStats.durationSec;
        wakeupStats2.totalWakeups = wakeupStats.totalWakeups;
        wakeupStats2.rootWakeups = wakeupStats.rootWakeups;
        wakeupStats2.systemWakeups = wakeupStats.systemWakeups;
        wakeupStats2.nonApplicationWakeups = wakeupStats.nonApplicationWakeups;
        wakeupStats2.applicationWakeups = wakeupStats.applicationWakeups;
        wakeupStats2.noUidWakeups = wakeupStats.noUidWakeups;
        wakeupStats2.l2UnicastCount = wakeupStats.l2UnicastCount;
        wakeupStats2.l2MulticastCount = wakeupStats.l2MulticastCount;
        wakeupStats2.l2BroadcastCount = wakeupStats.l2BroadcastCount;
        wakeupStats2.ethertypeCounts = toPairArray(wakeupStats.ethertypes);
        wakeupStats2.ipNextHeaderCounts = toPairArray(wakeupStats.ipNextHeaders);
        IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEventBuildEvent = buildEvent(0, 0L, wakeupStats.iface);
        ipConnectivityEventBuildEvent.setWakeupStats(wakeupStats2);
        return ipConnectivityEventBuildEvent;
    }

    public static IpConnectivityLogClass.IpConnectivityEvent toProto(DefaultNetworkEvent defaultNetworkEvent) {
        IpConnectivityLogClass.DefaultNetworkEvent defaultNetworkEvent2 = new IpConnectivityLogClass.DefaultNetworkEvent();
        defaultNetworkEvent2.finalScore = defaultNetworkEvent.finalScore;
        defaultNetworkEvent2.initialScore = defaultNetworkEvent.initialScore;
        defaultNetworkEvent2.ipSupport = ipSupportOf(defaultNetworkEvent);
        defaultNetworkEvent2.defaultNetworkDurationMs = defaultNetworkEvent.durationMs;
        defaultNetworkEvent2.validationDurationMs = defaultNetworkEvent.validatedMs;
        defaultNetworkEvent2.previousDefaultNetworkLinkLayer = transportsToLinkLayer(defaultNetworkEvent.previousTransports);
        IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEventBuildEvent = buildEvent(defaultNetworkEvent.netId, defaultNetworkEvent.transports, null);
        if (defaultNetworkEvent.transports == 0) {
            ipConnectivityEventBuildEvent.linkLayer = 5;
        }
        ipConnectivityEventBuildEvent.setDefaultNetworkEvent(defaultNetworkEvent2);
        return ipConnectivityEventBuildEvent;
    }

    private static IpConnectivityLogClass.IpConnectivityEvent buildEvent(int i, long j, String str) {
        IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent = new IpConnectivityLogClass.IpConnectivityEvent();
        ipConnectivityEvent.networkId = i;
        ipConnectivityEvent.transports = j;
        if (str != null) {
            ipConnectivityEvent.ifName = str;
        }
        inferLinkLayer(ipConnectivityEvent);
        return ipConnectivityEvent;
    }

    private static boolean setEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, Parcelable parcelable) {
        if (parcelable instanceof DhcpErrorEvent) {
            setDhcpErrorEvent(ipConnectivityEvent, (DhcpErrorEvent) parcelable);
            return true;
        }
        if (parcelable instanceof DhcpClientEvent) {
            setDhcpClientEvent(ipConnectivityEvent, (DhcpClientEvent) parcelable);
            return true;
        }
        if (parcelable instanceof IpManagerEvent) {
            setIpManagerEvent(ipConnectivityEvent, (IpManagerEvent) parcelable);
            return true;
        }
        if (parcelable instanceof IpReachabilityEvent) {
            setIpReachabilityEvent(ipConnectivityEvent, (IpReachabilityEvent) parcelable);
            return true;
        }
        if (parcelable instanceof NetworkEvent) {
            setNetworkEvent(ipConnectivityEvent, (NetworkEvent) parcelable);
            return true;
        }
        if (parcelable instanceof ValidationProbeEvent) {
            setValidationProbeEvent(ipConnectivityEvent, (ValidationProbeEvent) parcelable);
            return true;
        }
        if (parcelable instanceof ApfProgramEvent) {
            setApfProgramEvent(ipConnectivityEvent, (ApfProgramEvent) parcelable);
            return true;
        }
        if (parcelable instanceof ApfStats) {
            setApfStats(ipConnectivityEvent, (ApfStats) parcelable);
            return true;
        }
        if (parcelable instanceof RaEvent) {
            setRaEvent(ipConnectivityEvent, (RaEvent) parcelable);
            return true;
        }
        return false;
    }

    private static void setDhcpErrorEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, DhcpErrorEvent dhcpErrorEvent) {
        IpConnectivityLogClass.DHCPEvent dHCPEvent = new IpConnectivityLogClass.DHCPEvent();
        dHCPEvent.setErrorCode(dhcpErrorEvent.errorCode);
        ipConnectivityEvent.setDhcpEvent(dHCPEvent);
    }

    private static void setDhcpClientEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, DhcpClientEvent dhcpClientEvent) {
        IpConnectivityLogClass.DHCPEvent dHCPEvent = new IpConnectivityLogClass.DHCPEvent();
        dHCPEvent.setStateTransition(dhcpClientEvent.msg);
        dHCPEvent.durationMs = dhcpClientEvent.durationMs;
        ipConnectivityEvent.setDhcpEvent(dHCPEvent);
    }

    private static void setIpManagerEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, IpManagerEvent ipManagerEvent) {
        IpConnectivityLogClass.IpProvisioningEvent ipProvisioningEvent = new IpConnectivityLogClass.IpProvisioningEvent();
        ipProvisioningEvent.eventType = ipManagerEvent.eventType;
        ipProvisioningEvent.latencyMs = (int) ipManagerEvent.durationMs;
        ipConnectivityEvent.setIpProvisioningEvent(ipProvisioningEvent);
    }

    private static void setIpReachabilityEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, IpReachabilityEvent ipReachabilityEvent) {
        IpConnectivityLogClass.IpReachabilityEvent ipReachabilityEvent2 = new IpConnectivityLogClass.IpReachabilityEvent();
        ipReachabilityEvent2.eventType = ipReachabilityEvent.eventType;
        ipConnectivityEvent.setIpReachabilityEvent(ipReachabilityEvent2);
    }

    private static void setNetworkEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, NetworkEvent networkEvent) {
        IpConnectivityLogClass.NetworkEvent networkEvent2 = new IpConnectivityLogClass.NetworkEvent();
        networkEvent2.eventType = networkEvent.eventType;
        networkEvent2.latencyMs = (int) networkEvent.durationMs;
        ipConnectivityEvent.setNetworkEvent(networkEvent2);
    }

    private static void setValidationProbeEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, ValidationProbeEvent validationProbeEvent) {
        IpConnectivityLogClass.ValidationProbeEvent validationProbeEvent2 = new IpConnectivityLogClass.ValidationProbeEvent();
        validationProbeEvent2.latencyMs = (int) validationProbeEvent.durationMs;
        validationProbeEvent2.probeType = validationProbeEvent.probeType;
        validationProbeEvent2.probeResult = validationProbeEvent.returnCode;
        ipConnectivityEvent.setValidationProbeEvent(validationProbeEvent2);
    }

    private static void setApfProgramEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, ApfProgramEvent apfProgramEvent) {
        IpConnectivityLogClass.ApfProgramEvent apfProgramEvent2 = new IpConnectivityLogClass.ApfProgramEvent();
        apfProgramEvent2.lifetime = apfProgramEvent.lifetime;
        apfProgramEvent2.effectiveLifetime = apfProgramEvent.actualLifetime;
        apfProgramEvent2.filteredRas = apfProgramEvent.filteredRas;
        apfProgramEvent2.currentRas = apfProgramEvent.currentRas;
        apfProgramEvent2.programLength = apfProgramEvent.programLength;
        if (isBitSet(apfProgramEvent.flags, 0)) {
            apfProgramEvent2.dropMulticast = true;
        }
        if (isBitSet(apfProgramEvent.flags, 1)) {
            apfProgramEvent2.hasIpv4Addr = true;
        }
        ipConnectivityEvent.setApfProgramEvent(apfProgramEvent2);
    }

    private static void setApfStats(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, ApfStats apfStats) {
        IpConnectivityLogClass.ApfStatistics apfStatistics = new IpConnectivityLogClass.ApfStatistics();
        apfStatistics.durationMs = apfStats.durationMs;
        apfStatistics.receivedRas = apfStats.receivedRas;
        apfStatistics.matchingRas = apfStats.matchingRas;
        apfStatistics.droppedRas = apfStats.droppedRas;
        apfStatistics.zeroLifetimeRas = apfStats.zeroLifetimeRas;
        apfStatistics.parseErrors = apfStats.parseErrors;
        apfStatistics.programUpdates = apfStats.programUpdates;
        apfStatistics.programUpdatesAll = apfStats.programUpdatesAll;
        apfStatistics.programUpdatesAllowingMulticast = apfStats.programUpdatesAllowingMulticast;
        apfStatistics.maxProgramSize = apfStats.maxProgramSize;
        ipConnectivityEvent.setApfStatistics(apfStatistics);
    }

    private static void setRaEvent(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent, RaEvent raEvent) {
        IpConnectivityLogClass.RaEvent raEvent2 = new IpConnectivityLogClass.RaEvent();
        raEvent2.routerLifetime = raEvent.routerLifetime;
        raEvent2.prefixValidLifetime = raEvent.prefixValidLifetime;
        raEvent2.prefixPreferredLifetime = raEvent.prefixPreferredLifetime;
        raEvent2.routeInfoLifetime = raEvent.routeInfoLifetime;
        raEvent2.rdnssLifetime = raEvent.rdnssLifetime;
        raEvent2.dnsslLifetime = raEvent.dnsslLifetime;
        ipConnectivityEvent.setRaEvent(raEvent2);
    }

    private static int[] bytesToInts(byte[] bArr) {
        int[] iArr = new int[bArr.length];
        for (int i = 0; i < bArr.length; i++) {
            iArr[i] = bArr[i] & 255;
        }
        return iArr;
    }

    private static IpConnectivityLogClass.Pair[] toPairArray(SparseIntArray sparseIntArray) {
        int size = sparseIntArray.size();
        IpConnectivityLogClass.Pair[] pairArr = new IpConnectivityLogClass.Pair[size];
        for (int i = 0; i < size; i++) {
            IpConnectivityLogClass.Pair pair = new IpConnectivityLogClass.Pair();
            pair.key = sparseIntArray.keyAt(i);
            pair.value = sparseIntArray.valueAt(i);
            pairArr[i] = pair;
        }
        return pairArr;
    }

    private static int ipSupportOf(DefaultNetworkEvent defaultNetworkEvent) {
        if (defaultNetworkEvent.ipv4 && defaultNetworkEvent.ipv6) {
            return 3;
        }
        if (defaultNetworkEvent.ipv6) {
            return 2;
        }
        if (defaultNetworkEvent.ipv4) {
            return 1;
        }
        return 0;
    }

    private static boolean isBitSet(int i, int i2) {
        return (i & (1 << i2)) != 0;
    }

    private static void inferLinkLayer(IpConnectivityLogClass.IpConnectivityEvent ipConnectivityEvent) {
        int iIfnameToLinkLayer;
        if (ipConnectivityEvent.transports != 0) {
            iIfnameToLinkLayer = transportsToLinkLayer(ipConnectivityEvent.transports);
        } else if (ipConnectivityEvent.ifName != null) {
            iIfnameToLinkLayer = ifnameToLinkLayer(ipConnectivityEvent.ifName);
        } else {
            iIfnameToLinkLayer = 0;
        }
        if (iIfnameToLinkLayer == 0) {
            return;
        }
        ipConnectivityEvent.linkLayer = iIfnameToLinkLayer;
        ipConnectivityEvent.ifName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    private static int transportsToLinkLayer(long j) {
        switch (Long.bitCount(j)) {
            case 0:
                return 0;
            case 1:
                return transportToLinkLayer(Long.numberOfTrailingZeros(j));
            default:
                return 6;
        }
    }

    private static int transportToLinkLayer(int i) {
        if (i >= 0 && i < TRANSPORT_LINKLAYER_MAP.length) {
            return TRANSPORT_LINKLAYER_MAP[i];
        }
        return 0;
    }

    static {
        TRANSPORT_LINKLAYER_MAP[0] = 2;
        TRANSPORT_LINKLAYER_MAP[1] = 4;
        TRANSPORT_LINKLAYER_MAP[2] = 1;
        TRANSPORT_LINKLAYER_MAP[3] = 3;
        TRANSPORT_LINKLAYER_MAP[4] = 0;
        TRANSPORT_LINKLAYER_MAP[5] = 8;
        TRANSPORT_LINKLAYER_MAP[6] = 9;
        IFNAME_PREFIXES = new String[7];
        IFNAME_LINKLAYERS = new int[7];
        IFNAME_PREFIXES[0] = "rmnet";
        IFNAME_LINKLAYERS[0] = 2;
        IFNAME_PREFIXES[1] = "wlan";
        IFNAME_LINKLAYERS[1] = 4;
        IFNAME_PREFIXES[2] = "bt-pan";
        IFNAME_LINKLAYERS[2] = 1;
        IFNAME_PREFIXES[3] = "p2p";
        IFNAME_LINKLAYERS[3] = 7;
        IFNAME_PREFIXES[4] = "aware";
        IFNAME_LINKLAYERS[4] = 8;
        IFNAME_PREFIXES[5] = "eth";
        IFNAME_LINKLAYERS[5] = 3;
        IFNAME_PREFIXES[6] = "wpan";
        IFNAME_LINKLAYERS[6] = 9;
    }

    private static int ifnameToLinkLayer(String str) {
        for (int i = 0; i < 7; i++) {
            if (str.startsWith(IFNAME_PREFIXES[i])) {
                return IFNAME_LINKLAYERS[i];
            }
        }
        return 0;
    }
}
