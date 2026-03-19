package com.android.server.wifi.hotspot2;

import android.net.RssiCurve;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSWanMetricsElement;
import com.android.server.wifi.hotspot2.anqp.IPAddressTypeAvailabilityElement;
import java.util.HashMap;
import java.util.Map;

public class PasspointNetworkScore {

    @VisibleForTesting
    public static final int HOME_PROVIDER_AWARD = 100;

    @VisibleForTesting
    public static final int INTERNET_ACCESS_AWARD = 50;

    @VisibleForTesting
    public static final int PERSONAL_OR_EMERGENCY_NETWORK_AWARDS = 2;

    @VisibleForTesting
    public static final int PUBLIC_OR_PRIVATE_NETWORK_AWARDS = 4;

    @VisibleForTesting
    public static final int RESTRICTED_OR_UNKNOWN_IP_AWARDS = 1;

    @VisibleForTesting
    public static final int UNRESTRICTED_IP_AWARDS = 2;

    @VisibleForTesting
    public static final int WAN_PORT_DOWN_OR_CAPPED_PENALTY = 50;
    private static final Map<Integer, Integer> IPV4_SCORES = new HashMap();
    private static final Map<Integer, Integer> IPV6_SCORES = new HashMap();
    private static final Map<NetworkDetail.Ant, Integer> NETWORK_TYPE_SCORES = new HashMap();

    @VisibleForTesting
    public static final RssiCurve RSSI_SCORE = new RssiCurve(-80, 20, new byte[]{-10, 0, 10, 20, 30, 40}, 20);

    static {
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.FreePublic, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.ChargeablePublic, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.PrivateWithGuest, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.Private, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.Personal, 2);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.EmergencyOnly, 2);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.Wildcard, 0);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.TestOrExperimental, 0);
        IPV4_SCORES.put(0, 0);
        IPV4_SCORES.put(2, 1);
        IPV4_SCORES.put(5, 1);
        IPV4_SCORES.put(6, 1);
        IPV4_SCORES.put(7, 1);
        IPV4_SCORES.put(1, 2);
        IPV4_SCORES.put(3, 2);
        IPV4_SCORES.put(4, 2);
        IPV6_SCORES.put(0, 0);
        IPV6_SCORES.put(2, 1);
        IPV6_SCORES.put(1, 2);
    }

    public static int calculateScore(boolean z, ScanDetail scanDetail, Map<Constants.ANQPElementType, ANQPElement> map, boolean z2) {
        int i;
        NetworkDetail networkDetail = scanDetail.getNetworkDetail();
        if (z) {
            i = 100;
        } else {
            i = 0;
        }
        int iIntValue = i + ((networkDetail.isInternet() ? 1 : -1) * 50) + NETWORK_TYPE_SCORES.get(networkDetail.getAnt()).intValue();
        if (map != null) {
            HSWanMetricsElement hSWanMetricsElement = (HSWanMetricsElement) map.get(Constants.ANQPElementType.HSWANMetrics);
            if (hSWanMetricsElement != null && (hSWanMetricsElement.getStatus() != 1 || hSWanMetricsElement.isCapped())) {
                iIntValue -= 50;
            }
            IPAddressTypeAvailabilityElement iPAddressTypeAvailabilityElement = (IPAddressTypeAvailabilityElement) map.get(Constants.ANQPElementType.ANQPIPAddrAvailability);
            if (iPAddressTypeAvailabilityElement != null) {
                Integer num = IPV4_SCORES.get(Integer.valueOf(iPAddressTypeAvailabilityElement.getV4Availability()));
                Integer num2 = IPV6_SCORES.get(Integer.valueOf(iPAddressTypeAvailabilityElement.getV6Availability()));
                iIntValue += Integer.valueOf(num != null ? num.intValue() : 0).intValue() + Integer.valueOf(num2 != null ? num2.intValue() : 0).intValue();
            }
        }
        return iIntValue + RSSI_SCORE.lookupScore(scanDetail.getScanResult().level, z2);
    }
}
