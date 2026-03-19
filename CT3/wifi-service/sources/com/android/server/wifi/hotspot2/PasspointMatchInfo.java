package com.android.server.wifi.hotspot2;

import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.anqp.ANQPElement;
import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.anqp.HSConnectionCapabilityElement;
import com.android.server.wifi.anqp.HSWanMetricsElement;
import com.android.server.wifi.anqp.IPAddressTypeAvailabilityElement;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.pps.HomeSP;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class PasspointMatchInfo implements Comparable<PasspointMatchInfo> {
    private static final int IPPROTO_ESP = 50;
    private static final int IPPROTO_ICMP = 1;
    private static final int IPPROTO_TCP = 6;
    private static final int IPPROTO_UDP = 17;
    private final HomeSP mHomeSP;
    private final PasspointMatch mPasspointMatch;
    private final ScanDetail mScanDetail;
    private final int mScore;
    private static final Map<IPAddressTypeAvailabilityElement.IPv4Availability, Integer> sIP4Scores = new EnumMap(IPAddressTypeAvailabilityElement.IPv4Availability.class);
    private static final Map<IPAddressTypeAvailabilityElement.IPv6Availability, Integer> sIP6Scores = new EnumMap(IPAddressTypeAvailabilityElement.IPv6Availability.class);
    private static final Map<Integer, Map<Integer, Integer>> sPortScores = new HashMap();
    private static final Map<NetworkDetail.Ant, Integer> sAntScores = new HashMap();

    static {
        sAntScores.put(NetworkDetail.Ant.FreePublic, 4);
        sAntScores.put(NetworkDetail.Ant.ChargeablePublic, 4);
        sAntScores.put(NetworkDetail.Ant.PrivateWithGuest, 4);
        sAntScores.put(NetworkDetail.Ant.Private, 4);
        sAntScores.put(NetworkDetail.Ant.Personal, 2);
        sAntScores.put(NetworkDetail.Ant.EmergencyOnly, 2);
        sAntScores.put(NetworkDetail.Ant.Wildcard, 1);
        sAntScores.put(NetworkDetail.Ant.TestOrExperimental, 0);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.NotAvailable, 0);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.PortRestricted, 1);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.PortRestrictedAndSingleNAT, 1);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.PortRestrictedAndDoubleNAT, 1);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.Unknown, 1);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.Public, 2);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.SingleNAT, 2);
        sIP4Scores.put(IPAddressTypeAvailabilityElement.IPv4Availability.DoubleNAT, 2);
        sIP6Scores.put(IPAddressTypeAvailabilityElement.IPv6Availability.NotAvailable, 0);
        sIP6Scores.put(IPAddressTypeAvailabilityElement.IPv6Availability.Reserved, 1);
        sIP6Scores.put(IPAddressTypeAvailabilityElement.IPv6Availability.Unknown, 1);
        sIP6Scores.put(IPAddressTypeAvailabilityElement.IPv6Availability.Available, 2);
        Map<Integer, Integer> tcpMap = new HashMap<>();
        tcpMap.put(20, 1);
        tcpMap.put(21, 1);
        tcpMap.put(22, 3);
        tcpMap.put(23, 2);
        tcpMap.put(25, 8);
        tcpMap.put(26, 8);
        tcpMap.put(53, 3);
        tcpMap.put(80, 10);
        tcpMap.put(110, 6);
        tcpMap.put(143, 6);
        tcpMap.put(443, 10);
        tcpMap.put(993, 6);
        tcpMap.put(1723, 7);
        Map<Integer, Integer> udpMap = new HashMap<>();
        udpMap.put(53, 10);
        udpMap.put(500, 7);
        udpMap.put(5060, 10);
        udpMap.put(4500, 4);
        sPortScores.put(6, tcpMap);
        sPortScores.put(17, udpMap);
    }

    public PasspointMatchInfo(PasspointMatch passpointMatch, ScanDetail scanDetail, HomeSP homeSP) {
        int score;
        this.mPasspointMatch = passpointMatch;
        this.mScanDetail = scanDetail;
        this.mHomeSP = homeSP;
        if (passpointMatch == PasspointMatch.HomeProvider) {
            score = 100;
        } else if (passpointMatch == PasspointMatch.RoamingProvider) {
            score = 0;
        } else {
            score = -1000;
        }
        if (getNetworkDetail().getHSRelease() != null) {
            score += getNetworkDetail().getHSRelease() != NetworkDetail.HSRelease.Unknown ? 50 : 0;
        }
        if (getNetworkDetail().hasInterworking()) {
            score += getNetworkDetail().isInternet() ? 20 : -20;
        }
        int score2 = score + (((Math.max(200 - getNetworkDetail().getStationCount(), 0) * (255 - getNetworkDetail().getChannelUtilization())) * getNetworkDetail().getCapacity()) >>> 26);
        score2 = getNetworkDetail().hasInterworking() ? score2 + sAntScores.get(getNetworkDetail().getAnt()).intValue() : score2;
        Map<Constants.ANQPElementType, ANQPElement> anqp = getNetworkDetail().getANQPElements();
        if (anqp != null) {
            HSWanMetricsElement wm = (HSWanMetricsElement) anqp.get(Constants.ANQPElementType.HSWANMetrics);
            if (wm != null) {
                if (wm.getStatus() != HSWanMetricsElement.LinkStatus.Up || wm.isCapped()) {
                    score2 -= 1000;
                } else {
                    long scaledSpeed = (wm.getDlSpeed() * ((long) (255 - wm.getDlLoad())) * 8) + (wm.getUlSpeed() * ((long) (255 - wm.getUlLoad())) * 2);
                    score2 = (int) (((long) score2) + (Math.min(scaledSpeed, 255000000L) >>> 23));
                }
            }
            IPAddressTypeAvailabilityElement ipa = (IPAddressTypeAvailabilityElement) anqp.get(Constants.ANQPElementType.ANQPIPAddrAvailability);
            if (ipa != null) {
                Integer as14 = sIP4Scores.get(ipa.getV4Availability());
                Integer as16 = sIP6Scores.get(ipa.getV6Availability());
                score2 += (Integer.valueOf(as14 != null ? as14.intValue() : 1).intValue() * 2) + Integer.valueOf(as16 != null ? as16.intValue() : 1).intValue();
            }
            HSConnectionCapabilityElement cce = (HSConnectionCapabilityElement) anqp.get(Constants.ANQPElementType.HSConnCapability);
            if (cce != null) {
                score2 = Math.min(Math.max(protoScore(cce) >> 3, -10), 10);
            }
        }
        this.mScore = score2;
    }

    public PasspointMatch getPasspointMatch() {
        return this.mPasspointMatch;
    }

    public ScanDetail getScanDetail() {
        return this.mScanDetail;
    }

    public NetworkDetail getNetworkDetail() {
        return this.mScanDetail.getNetworkDetail();
    }

    public HomeSP getHomeSP() {
        return this.mHomeSP;
    }

    public int getScore() {
        return this.mScore;
    }

    @Override
    public int compareTo(PasspointMatchInfo that) {
        return getScore() - that.getScore();
    }

    private static int protoScore(HSConnectionCapabilityElement cce) {
        int score = 0;
        for (HSConnectionCapabilityElement.ProtocolTuple tuple : cce.getStatusList()) {
            int sign = tuple.getStatus() == HSConnectionCapabilityElement.ProtoStatus.Open ? 1 : -1;
            int elementScore = 1;
            if (tuple.getProtocol() == 1) {
                elementScore = 1;
            } else if (tuple.getProtocol() == 50) {
                elementScore = 5;
            } else {
                Map<Integer, Integer> protoMap = sPortScores.get(Integer.valueOf(tuple.getProtocol()));
                if (protoMap != null) {
                    Integer portScore = protoMap.get(Integer.valueOf(tuple.getPort()));
                    elementScore = portScore != null ? portScore.intValue() : 0;
                }
            }
            score += elementScore * sign;
        }
        return score;
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        PasspointMatchInfo that = (PasspointMatchInfo) thatObject;
        if (getNetworkDetail().equals(that.getNetworkDetail()) && getHomeSP().equals(that.getHomeSP())) {
            return getPasspointMatch().equals(that.getPasspointMatch());
        }
        return false;
    }

    public int hashCode() {
        int result = this.mPasspointMatch != null ? this.mPasspointMatch.hashCode() : 0;
        return (((result * 31) + getNetworkDetail().hashCode()) * 31) + (this.mHomeSP != null ? this.mHomeSP.hashCode() : 0);
    }

    public String toString() {
        return "PasspointMatchInfo{, mPasspointMatch=" + this.mPasspointMatch + ", mNetworkInfo=" + getNetworkDetail().getSSID() + ", mHomeSP=" + this.mHomeSP.getFQDN() + '}';
    }
}
