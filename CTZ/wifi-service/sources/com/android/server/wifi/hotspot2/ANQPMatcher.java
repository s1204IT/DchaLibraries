package com.android.server.wifi.hotspot2;

import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.hotspot2.anqp.CellularNetwork;
import com.android.server.wifi.hotspot2.anqp.DomainNameElement;
import com.android.server.wifi.hotspot2.anqp.NAIRealmData;
import com.android.server.wifi.hotspot2.anqp.NAIRealmElement;
import com.android.server.wifi.hotspot2.anqp.RoamingConsortiumElement;
import com.android.server.wifi.hotspot2.anqp.ThreeGPPNetworkElement;
import com.android.server.wifi.hotspot2.anqp.eap.AuthParam;
import com.android.server.wifi.hotspot2.anqp.eap.EAPMethod;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ANQPMatcher {
    public static boolean matchDomainName(DomainNameElement domainNameElement, String str, IMSIParameter iMSIParameter, List<String> list) {
        if (domainNameElement == null) {
            return false;
        }
        for (String str2 : domainNameElement.getDomains()) {
            if (DomainMatcher.arg2SubdomainOfArg1(str, str2) || matchMccMnc(Utils.getMccMnc(Utils.splitDomain(str2)), iMSIParameter, list)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchRoamingConsortium(RoamingConsortiumElement roamingConsortiumElement, long[] jArr) {
        if (roamingConsortiumElement == null || jArr == null) {
            return false;
        }
        List<Long> oIs = roamingConsortiumElement.getOIs();
        for (long j : jArr) {
            if (oIs.contains(Long.valueOf(j))) {
                return true;
            }
        }
        return false;
    }

    public static int matchNAIRealm(NAIRealmElement nAIRealmElement, String str, int i, AuthParam authParam) {
        if (nAIRealmElement == null || nAIRealmElement.getRealmDataList().isEmpty()) {
            return 0;
        }
        int i2 = -1;
        Iterator<NAIRealmData> it = nAIRealmElement.getRealmDataList().iterator();
        while (it.hasNext()) {
            int iMatchNAIRealmData = matchNAIRealmData(it.next(), str, i, authParam);
            if (iMatchNAIRealmData > i2) {
                if (iMatchNAIRealmData != 7) {
                    i2 = iMatchNAIRealmData;
                } else {
                    return iMatchNAIRealmData;
                }
            }
        }
        return i2;
    }

    public static boolean matchThreeGPPNetwork(ThreeGPPNetworkElement threeGPPNetworkElement, IMSIParameter iMSIParameter, List<String> list) {
        if (threeGPPNetworkElement == null) {
            return false;
        }
        Iterator<CellularNetwork> it = threeGPPNetworkElement.getNetworks().iterator();
        while (it.hasNext()) {
            if (matchCellularNetwork(it.next(), iMSIParameter, list)) {
                return true;
            }
        }
        return false;
    }

    private static int matchNAIRealmData(NAIRealmData nAIRealmData, String str, int i, AuthParam authParam) {
        int i2;
        Iterator<String> it = nAIRealmData.getRealms().iterator();
        while (true) {
            if (it.hasNext()) {
                if (DomainMatcher.arg2SubdomainOfArg1(str, it.next())) {
                    i2 = 4;
                    break;
                }
            } else {
                i2 = -1;
                break;
            }
        }
        if (nAIRealmData.getEAPMethods().isEmpty()) {
            return i2;
        }
        Iterator<EAPMethod> it2 = nAIRealmData.getEAPMethods().iterator();
        int iMatchEAPMethod = -1;
        while (it2.hasNext() && (iMatchEAPMethod = matchEAPMethod(it2.next(), i, authParam)) == -1) {
        }
        if (iMatchEAPMethod == -1) {
            return -1;
        }
        if (i2 == -1) {
            return iMatchEAPMethod;
        }
        return i2 | iMatchEAPMethod;
    }

    private static int matchEAPMethod(EAPMethod eAPMethod, int i, AuthParam authParam) {
        if (eAPMethod.getEAPMethodID() != i) {
            return -1;
        }
        if (authParam != null) {
            Set<AuthParam> set = eAPMethod.getAuthParams().get(Integer.valueOf(authParam.getAuthTypeID()));
            return (set == null || !set.contains(authParam)) ? -1 : 3;
        }
        return 2;
    }

    private static boolean matchCellularNetwork(CellularNetwork cellularNetwork, IMSIParameter iMSIParameter, List<String> list) {
        Iterator<String> it = cellularNetwork.getPlmns().iterator();
        while (it.hasNext()) {
            if (matchMccMnc(it.next(), iMSIParameter, list)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchMccMnc(String str, IMSIParameter iMSIParameter, List<String> list) {
        if (iMSIParameter == null || list == null || !iMSIParameter.matchesMccMnc(str)) {
            return false;
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(str)) {
                return true;
            }
        }
        return false;
    }
}
