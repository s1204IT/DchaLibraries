package com.android.server.wifi.hotspot2.pps;

import android.util.Log;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.anqp.ANQPElement;
import com.android.server.wifi.anqp.CellularNetwork;
import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.anqp.DomainNameElement;
import com.android.server.wifi.anqp.NAIRealmElement;
import com.android.server.wifi.anqp.RoamingConsortiumElement;
import com.android.server.wifi.anqp.ThreeGPPNetworkElement;
import com.android.server.wifi.hotspot2.AuthMatch;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointMatch;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.pps.DomainMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeSP {
    private final Map<String, String> mAAATrustRoots;
    private final Credential mCredential;
    private final int mCredentialPriority;
    private final DomainMatcher mDomainMatcher;
    private final String mFQDN;
    private final String mFriendlyName;
    private final String mIconURL;
    private final List<Long> mMatchAllOIs;
    private final Set<Long> mMatchAnyOIs;
    private final Set<String> mOtherHomePartners;
    private final Policy mPolicy;
    private final HashSet<Long> mRoamingConsortiums;
    private final Map<String, Long> mSSIDs;
    private final SubscriptionParameters mSubscriptionParameters;
    private final UpdateInfo mSubscriptionUpdate;
    private final int mUpdateIdentifier;

    @Deprecated
    public HomeSP(Map<String, Long> ssidMap, String fqdn, HashSet<Long> roamingConsortiums, Set<String> otherHomePartners, Set<Long> matchAnyOIs, List<Long> matchAllOIs, String friendlyName, String iconURL, Credential credential) {
        this.mSSIDs = ssidMap;
        List<List<String>> otherPartners = new ArrayList<>(otherHomePartners.size());
        for (String otherPartner : otherHomePartners) {
            otherPartners.add(Utils.splitDomain(otherPartner));
        }
        this.mOtherHomePartners = otherHomePartners;
        this.mFQDN = fqdn;
        this.mDomainMatcher = new DomainMatcher(Utils.splitDomain(fqdn), otherPartners);
        this.mRoamingConsortiums = roamingConsortiums;
        this.mMatchAnyOIs = matchAnyOIs;
        this.mMatchAllOIs = matchAllOIs;
        this.mFriendlyName = friendlyName;
        this.mIconURL = iconURL;
        this.mCredential = credential;
        this.mPolicy = null;
        this.mCredentialPriority = -1;
        this.mAAATrustRoots = null;
        this.mSubscriptionUpdate = null;
        this.mSubscriptionParameters = null;
        this.mUpdateIdentifier = -1;
    }

    public HomeSP(Map<String, Long> ssidMap, String fqdn, HashSet<Long> roamingConsortiums, Set<String> otherHomePartners, Set<Long> matchAnyOIs, List<Long> matchAllOIs, String friendlyName, String iconURL, Credential credential, Policy policy, int credentialPriority, Map<String, String> AAATrustRoots, UpdateInfo subscriptionUpdate, SubscriptionParameters subscriptionParameters, int updateIdentifier) {
        this.mSSIDs = ssidMap;
        List<List<String>> otherPartners = new ArrayList<>(otherHomePartners.size());
        for (String otherPartner : otherHomePartners) {
            otherPartners.add(Utils.splitDomain(otherPartner));
        }
        this.mOtherHomePartners = otherHomePartners;
        this.mFQDN = fqdn;
        this.mDomainMatcher = new DomainMatcher(Utils.splitDomain(fqdn), otherPartners);
        this.mRoamingConsortiums = roamingConsortiums;
        this.mMatchAnyOIs = matchAnyOIs;
        this.mMatchAllOIs = matchAllOIs;
        this.mFriendlyName = friendlyName;
        this.mIconURL = iconURL;
        this.mCredential = credential;
        this.mPolicy = policy;
        this.mCredentialPriority = credentialPriority;
        this.mAAATrustRoots = AAATrustRoots;
        this.mSubscriptionUpdate = subscriptionUpdate;
        this.mSubscriptionParameters = subscriptionParameters;
        this.mUpdateIdentifier = updateIdentifier;
    }

    public int getUpdateIdentifier() {
        return this.mUpdateIdentifier;
    }

    public UpdateInfo getSubscriptionUpdate() {
        return this.mSubscriptionUpdate;
    }

    public Policy getPolicy() {
        return this.mPolicy;
    }

    public PasspointMatch match(NetworkDetail networkDetail, Map<Constants.ANQPElementType, ANQPElement> anqpElementMap, SIMAccessor simAccessor) {
        List<String> imsis = simAccessor.getMatchingImsis(this.mCredential.getImsi());
        PasspointMatch spMatch = matchSP(networkDetail, anqpElementMap, imsis);
        if (spMatch == PasspointMatch.Incomplete || spMatch == PasspointMatch.Declined) {
            return spMatch;
        }
        if (imsiMatch(imsis, (ThreeGPPNetworkElement) anqpElementMap.get(Constants.ANQPElementType.ANQP3GPPNetwork)) != null) {
            return spMatch == PasspointMatch.None ? PasspointMatch.RoamingProvider : spMatch;
        }
        NAIRealmElement naiRealmElement = (NAIRealmElement) anqpElementMap.get(Constants.ANQPElementType.ANQPNAIRealm);
        int authMatch = naiRealmElement != null ? naiRealmElement.match(this.mCredential) : 0;
        Log.d(Utils.hs2LogTag(getClass()), networkDetail.toKeyString() + " match on " + this.mFQDN + ": " + spMatch + ", auth " + AuthMatch.toString(authMatch));
        return authMatch == -1 ? PasspointMatch.None : ((authMatch & 4) != 0 && spMatch == PasspointMatch.None) ? PasspointMatch.RoamingProvider : spMatch;
    }

    public PasspointMatch matchSP(NetworkDetail networkDetail, Map<Constants.ANQPElementType, ANQPElement> anqpElementMap, List<String> imsis) {
        boolean validANQP;
        RoamingConsortiumElement rcElement;
        Long hessid;
        if (this.mSSIDs.containsKey(networkDetail.getSSID()) && ((hessid = this.mSSIDs.get(networkDetail.getSSID())) == null || networkDetail.getHESSID() == hessid.longValue())) {
            Log.d(Utils.hs2LogTag(getClass()), "match SSID");
            return PasspointMatch.HomeProvider;
        }
        Set<Long> anOIs = new HashSet<>();
        if (networkDetail.getRoamingConsortiums() != null) {
            for (long oi : networkDetail.getRoamingConsortiums()) {
                anOIs.add(Long.valueOf(oi));
            }
        }
        if (anqpElementMap == null) {
            validANQP = false;
        } else {
            validANQP = Constants.hasBaseANQPElements(anqpElementMap.keySet());
        }
        if (validANQP) {
            rcElement = (RoamingConsortiumElement) anqpElementMap.get(Constants.ANQPElementType.ANQPRoamingConsortium);
        } else {
            rcElement = null;
        }
        if (rcElement != null) {
            anOIs.addAll(rcElement.getOIs());
        }
        boolean roamingMatch = false;
        if (!this.mMatchAllOIs.isEmpty()) {
            boolean matchesAll = true;
            Iterator spOI$iterator = this.mMatchAllOIs.iterator();
            while (true) {
                if (!spOI$iterator.hasNext()) {
                    break;
                }
                long spOI = ((Long) spOI$iterator.next()).longValue();
                if (!anOIs.contains(Long.valueOf(spOI))) {
                    matchesAll = false;
                    break;
                }
            }
            if (matchesAll) {
                roamingMatch = true;
            } else {
                if (validANQP || networkDetail.getAnqpOICount() == 0) {
                    return PasspointMatch.Declined;
                }
                return PasspointMatch.Incomplete;
            }
        }
        if (!roamingMatch && (!Collections.disjoint(this.mMatchAnyOIs, anOIs) || !Collections.disjoint(this.mRoamingConsortiums, anOIs))) {
            roamingMatch = true;
        }
        if (!validANQP) {
            return PasspointMatch.Incomplete;
        }
        DomainNameElement domainNameElement = (DomainNameElement) anqpElementMap.get(Constants.ANQPElementType.ANQPDomName);
        if (domainNameElement != null) {
            for (String domain : domainNameElement.getDomains()) {
                List<String> anLabels = Utils.splitDomain(domain);
                DomainMatcher.Match match = this.mDomainMatcher.isSubDomain(anLabels);
                if (match != DomainMatcher.Match.None) {
                    return PasspointMatch.HomeProvider;
                }
                if (imsiMatch(imsis, anLabels) != null) {
                    return PasspointMatch.HomeProvider;
                }
            }
        }
        return roamingMatch ? PasspointMatch.RoamingProvider : PasspointMatch.None;
    }

    private String imsiMatch(List<String> imsis, ThreeGPPNetworkElement plmnElement) {
        if (imsis == null || plmnElement == null || plmnElement.getPlmns().isEmpty()) {
            return null;
        }
        for (CellularNetwork network : plmnElement.getPlmns()) {
            for (String mccMnc : network) {
                String imsi = imsiMatch(imsis, mccMnc);
                if (imsi != null) {
                    return imsi;
                }
            }
        }
        return null;
    }

    private String imsiMatch(List<String> imsis, List<String> fqdn) {
        String mccMnc;
        if (imsis == null || (mccMnc = Utils.getMccMnc(fqdn)) == null) {
            return null;
        }
        return imsiMatch(imsis, mccMnc);
    }

    private String imsiMatch(List<String> imsis, String mccMnc) {
        if (this.mCredential.getImsi().matchesMccMnc(mccMnc)) {
            for (String imsi : imsis) {
                if (imsi.startsWith(mccMnc)) {
                    return imsi;
                }
            }
            return null;
        }
        return null;
    }

    public String getFQDN() {
        return this.mFQDN;
    }

    public String getFriendlyName() {
        return this.mFriendlyName;
    }

    public HashSet<Long> getRoamingConsortiums() {
        return this.mRoamingConsortiums;
    }

    public Credential getCredential() {
        return this.mCredential;
    }

    public Map<String, Long> getSSIDs() {
        return this.mSSIDs;
    }

    public Collection<String> getOtherHomePartners() {
        return this.mOtherHomePartners;
    }

    public Set<Long> getMatchAnyOIs() {
        return this.mMatchAnyOIs;
    }

    public List<Long> getMatchAllOIs() {
        return this.mMatchAllOIs;
    }

    public String getIconURL() {
        return this.mIconURL;
    }

    public boolean deepEquals(HomeSP other) {
        if (this.mFQDN.equals(other.mFQDN) && this.mSSIDs.equals(other.mSSIDs) && this.mOtherHomePartners.equals(other.mOtherHomePartners) && this.mRoamingConsortiums.equals(other.mRoamingConsortiums) && this.mMatchAnyOIs.equals(other.mMatchAnyOIs) && this.mMatchAllOIs.equals(other.mMatchAllOIs) && this.mFriendlyName.equals(other.mFriendlyName) && Utils.compare(this.mIconURL, other.mIconURL) == 0) {
            return this.mCredential.equals(other.mCredential);
        }
        return false;
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        HomeSP that = (HomeSP) thatObject;
        return this.mFQDN.equals(that.mFQDN);
    }

    public int hashCode() {
        return this.mFQDN.hashCode();
    }

    public String toString() {
        return "HomeSP{SSIDs=" + this.mSSIDs + ", FQDN='" + this.mFQDN + "', DomainMatcher=" + this.mDomainMatcher + ", RoamingConsortiums={" + Utils.roamingConsortiumsToString(this.mRoamingConsortiums) + "}, MatchAnyOIs={" + Utils.roamingConsortiumsToString(this.mMatchAnyOIs) + "}, MatchAllOIs={" + Utils.roamingConsortiumsToString(this.mMatchAllOIs) + "}, Credential=" + this.mCredential + ", FriendlyName='" + this.mFriendlyName + "', IconURL='" + this.mIconURL + "'}";
    }
}
