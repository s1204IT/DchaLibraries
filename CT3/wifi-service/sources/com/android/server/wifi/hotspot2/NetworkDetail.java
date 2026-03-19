package com.android.server.wifi.hotspot2;

import android.net.wifi.ScanResult;
import android.util.Log;
import com.android.server.wifi.anqp.ANQPElement;
import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.anqp.RawByteElement;
import com.android.server.wifi.anqp.VenueNameElement;
import com.android.server.wifi.anqp.eap.EAP;
import com.android.server.wifi.util.InformationElementUtil;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkDetail {
    private static final boolean DBG = true;
    private static final String TAG = "NetworkDetail:";
    private static final boolean VDBG = false;
    private final Map<Constants.ANQPElementType, ANQPElement> mANQPElements;
    private final int mAnqpDomainID;
    private final int mAnqpOICount;
    private final Ant mAnt;
    private final long mBSSID;
    private final int mCapacity;
    private final int mCenterfreq0;
    private final int mCenterfreq1;
    private final int mChannelUtilization;
    private final int mChannelWidth;
    private int mDtimInterval;
    private final InformationElementUtil.ExtendedCapabilities mExtendedCapabilities;
    private final long mHESSID;
    private final HSRelease mHSRelease;
    private final boolean mInternet;
    private final int mMaxRate;
    private final int mPrimaryFreq;
    private final long[] mRoamingConsortiums;
    private final String mSSID;
    private final int mStationCount;
    private final VenueNameElement.VenueGroup mVenueGroup;
    private final VenueNameElement.VenueType mVenueType;
    private final int mWifiMode;

    public enum Ant {
        Private,
        PrivateWithGuest,
        ChargeablePublic,
        FreePublic,
        Personal,
        EmergencyOnly,
        Resvd6,
        Resvd7,
        Resvd8,
        Resvd9,
        Resvd10,
        Resvd11,
        Resvd12,
        Resvd13,
        TestOrExperimental,
        Wildcard;

        public static Ant[] valuesCustom() {
            return values();
        }
    }

    public enum HSRelease {
        R1,
        R2,
        Unknown;

        public static HSRelease[] valuesCustom() {
            return values();
        }
    }

    public NetworkDetail(String bssid, ScanResult.InformationElement[] infoElements, List<String> anqpLines, int freq) {
        this.mDtimInterval = -1;
        if (infoElements == null) {
            throw new IllegalArgumentException("Null information elements");
        }
        this.mBSSID = Utils.parseMac(bssid);
        String ssid = null;
        byte[] ssidOctets = null;
        InformationElementUtil.BssLoad bssLoad = new InformationElementUtil.BssLoad();
        InformationElementUtil.Interworking interworking = new InformationElementUtil.Interworking();
        InformationElementUtil.RoamingConsortium roamingConsortium = new InformationElementUtil.RoamingConsortium();
        InformationElementUtil.Vsa vsa = new InformationElementUtil.Vsa();
        InformationElementUtil.HtOperation htOperation = new InformationElementUtil.HtOperation();
        InformationElementUtil.VhtOperation vhtOperation = new InformationElementUtil.VhtOperation();
        InformationElementUtil.ExtendedCapabilities extendedCapabilities = new InformationElementUtil.ExtendedCapabilities();
        InformationElementUtil.TrafficIndicationMap trafficIndicationMap = new InformationElementUtil.TrafficIndicationMap();
        InformationElementUtil.SupportedRates supportedRates = new InformationElementUtil.SupportedRates();
        InformationElementUtil.SupportedRates extendedSupportedRates = new InformationElementUtil.SupportedRates();
        RuntimeException exception = null;
        ArrayList<Integer> iesFound = new ArrayList<>();
        try {
            for (ScanResult.InformationElement ie : infoElements) {
                iesFound.add(Integer.valueOf(ie.id));
                switch (ie.id) {
                    case 0:
                        ssidOctets = ie.bytes;
                        break;
                    case 1:
                        supportedRates.from(ie);
                        break;
                    case 5:
                        trafficIndicationMap.from(ie);
                        break;
                    case 11:
                        bssLoad.from(ie);
                        break;
                    case EAP.EAP_AKAPrim:
                        extendedSupportedRates.from(ie);
                        break;
                    case 61:
                        htOperation.from(ie);
                        break;
                    case 107:
                        interworking.from(ie);
                        break;
                    case 111:
                        roamingConsortium.from(ie);
                        break;
                    case InformationElementUtil.SupportedRates.MASK:
                        extendedCapabilities.from(ie);
                        break;
                    case 192:
                        vhtOperation.from(ie);
                        break;
                    case EAP.VendorSpecific:
                        vsa.from(ie);
                        break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e) {
            Log.d(Utils.hs2LogTag(getClass()), "Caught " + e);
            if (ssidOctets == null) {
                throw new IllegalArgumentException("Malformed IE string (no SSID)", e);
            }
            exception = e;
        }
        if (ssidOctets != null) {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            try {
                CharBuffer decoded = decoder.decode(ByteBuffer.wrap(ssidOctets));
                ssid = decoded.toString();
            } catch (CharacterCodingException e2) {
                ssid = null;
            }
            if (ssid == null) {
                if (extendedCapabilities.isStrictUtf8() && exception != null) {
                    throw new IllegalArgumentException("Failed to decode SSID in dubious IE string");
                }
                ssid = new String(ssidOctets, StandardCharsets.ISO_8859_1);
            }
        }
        this.mSSID = ssid;
        this.mHESSID = interworking.hessid;
        this.mStationCount = bssLoad.stationCount;
        this.mChannelUtilization = bssLoad.channelUtilization;
        this.mCapacity = bssLoad.capacity;
        this.mAnt = interworking.ant;
        this.mInternet = interworking.internet;
        this.mVenueGroup = interworking.venueGroup;
        this.mVenueType = interworking.venueType;
        this.mHSRelease = vsa.hsRelease;
        this.mAnqpDomainID = vsa.anqpDomainID;
        this.mAnqpOICount = roamingConsortium.anqpOICount;
        this.mRoamingConsortiums = roamingConsortium.roamingConsortiums;
        this.mExtendedCapabilities = extendedCapabilities;
        this.mANQPElements = SupplicantBridge.parseANQPLines(anqpLines);
        this.mPrimaryFreq = freq;
        if (vhtOperation.isValid()) {
            this.mChannelWidth = vhtOperation.getChannelWidth();
            this.mCenterfreq0 = vhtOperation.getCenterFreq0();
            this.mCenterfreq1 = vhtOperation.getCenterFreq1();
        } else {
            this.mChannelWidth = htOperation.getChannelWidth();
            this.mCenterfreq0 = htOperation.getCenterFreq0(this.mPrimaryFreq);
            this.mCenterfreq1 = 0;
        }
        this.mDtimInterval = trafficIndicationMap.mDtimPeriod;
        int maxRateB = extendedSupportedRates.isValid() ? extendedSupportedRates.mRates.get(extendedSupportedRates.mRates.size() - 1).intValue() : 0;
        if (supportedRates.isValid()) {
            int maxRateA = supportedRates.mRates.get(supportedRates.mRates.size() - 1).intValue();
            this.mMaxRate = maxRateA > maxRateB ? maxRateA : maxRateB;
            this.mWifiMode = InformationElementUtil.WifiMode.determineMode(this.mPrimaryFreq, this.mMaxRate, vhtOperation.isValid(), iesFound.contains(61), iesFound.contains(42));
        } else {
            this.mWifiMode = 0;
            this.mMaxRate = 0;
            Log.w("WifiMode", this.mSSID + ", Invalid SupportedRates!!!");
        }
    }

    private static ByteBuffer getAndAdvancePayload(ByteBuffer data, int plLength) {
        ByteBuffer payload = data.duplicate().order(data.order());
        payload.limit(payload.position() + plLength);
        data.position(data.position() + plLength);
        return payload;
    }

    private NetworkDetail(NetworkDetail base, Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        this.mDtimInterval = -1;
        this.mSSID = base.mSSID;
        this.mBSSID = base.mBSSID;
        this.mHESSID = base.mHESSID;
        this.mStationCount = base.mStationCount;
        this.mChannelUtilization = base.mChannelUtilization;
        this.mCapacity = base.mCapacity;
        this.mAnt = base.mAnt;
        this.mInternet = base.mInternet;
        this.mVenueGroup = base.mVenueGroup;
        this.mVenueType = base.mVenueType;
        this.mHSRelease = base.mHSRelease;
        this.mAnqpDomainID = base.mAnqpDomainID;
        this.mAnqpOICount = base.mAnqpOICount;
        this.mRoamingConsortiums = base.mRoamingConsortiums;
        this.mExtendedCapabilities = new InformationElementUtil.ExtendedCapabilities(base.mExtendedCapabilities);
        this.mANQPElements = anqpElements;
        this.mChannelWidth = base.mChannelWidth;
        this.mPrimaryFreq = base.mPrimaryFreq;
        this.mCenterfreq0 = base.mCenterfreq0;
        this.mCenterfreq1 = base.mCenterfreq1;
        this.mDtimInterval = base.mDtimInterval;
        this.mWifiMode = base.mWifiMode;
        this.mMaxRate = base.mMaxRate;
    }

    public NetworkDetail complete(Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        return new NetworkDetail(this, anqpElements);
    }

    public boolean queriable(List<Constants.ANQPElementType> queryElements) {
        if (this.mAnt == null) {
            return false;
        }
        if (Constants.hasBaseANQPElements(queryElements)) {
            return true;
        }
        return Constants.hasR2Elements(queryElements) && this.mHSRelease == HSRelease.R2;
    }

    public boolean has80211uInfo() {
        return (this.mAnt == null && this.mRoamingConsortiums == null && this.mHSRelease == null) ? false : true;
    }

    public boolean hasInterworking() {
        return this.mAnt != null;
    }

    public String getSSID() {
        return this.mSSID;
    }

    public String getTrimmedSSID() {
        for (int n = 0; n < this.mSSID.length(); n++) {
            if (this.mSSID.charAt(n) != 0) {
                return this.mSSID;
            }
        }
        return "";
    }

    public long getHESSID() {
        return this.mHESSID;
    }

    public long getBSSID() {
        return this.mBSSID;
    }

    public int getStationCount() {
        return this.mStationCount;
    }

    public int getChannelUtilization() {
        return this.mChannelUtilization;
    }

    public int getCapacity() {
        return this.mCapacity;
    }

    public boolean isInterworking() {
        return this.mAnt != null;
    }

    public Ant getAnt() {
        return this.mAnt;
    }

    public boolean isInternet() {
        return this.mInternet;
    }

    public VenueNameElement.VenueGroup getVenueGroup() {
        return this.mVenueGroup;
    }

    public VenueNameElement.VenueType getVenueType() {
        return this.mVenueType;
    }

    public HSRelease getHSRelease() {
        return this.mHSRelease;
    }

    public int getAnqpDomainID() {
        return this.mAnqpDomainID;
    }

    public byte[] getOsuProviders() {
        ANQPElement osuProviders;
        if (this.mANQPElements == null || (osuProviders = this.mANQPElements.get(Constants.ANQPElementType.HSOSUProviders)) == null) {
            return null;
        }
        return ((RawByteElement) osuProviders).getPayload();
    }

    public int getAnqpOICount() {
        return this.mAnqpOICount;
    }

    public long[] getRoamingConsortiums() {
        return this.mRoamingConsortiums;
    }

    public Long getExtendedCapabilities() {
        return this.mExtendedCapabilities.extendedCapabilities;
    }

    public Map<Constants.ANQPElementType, ANQPElement> getANQPElements() {
        return this.mANQPElements;
    }

    public int getChannelWidth() {
        return this.mChannelWidth;
    }

    public int getCenterfreq0() {
        return this.mCenterfreq0;
    }

    public int getCenterfreq1() {
        return this.mCenterfreq1;
    }

    public int getWifiMode() {
        return this.mWifiMode;
    }

    public int getDtimInterval() {
        return this.mDtimInterval;
    }

    public boolean is80211McResponderSupport() {
        return this.mExtendedCapabilities.is80211McRTTResponder;
    }

    public boolean isSSID_UTF8() {
        return this.mExtendedCapabilities.isStrictUtf8();
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        NetworkDetail that = (NetworkDetail) thatObject;
        return getSSID().equals(that.getSSID()) && getBSSID() == that.getBSSID();
    }

    public int hashCode() {
        return (((this.mSSID.hashCode() * 31) + ((int) (this.mBSSID >>> 32))) * 31) + ((int) this.mBSSID);
    }

    public String toString() {
        return String.format("NetworkInfo{SSID='%s', HESSID=%x, BSSID=%x, StationCount=%d, ChannelUtilization=%d, Capacity=%d, Ant=%s, Internet=%s, VenueGroup=%s, VenueType=%s, HSRelease=%s, AnqpDomainID=%d, AnqpOICount=%d, RoamingConsortiums=%s}", this.mSSID, Long.valueOf(this.mHESSID), Long.valueOf(this.mBSSID), Integer.valueOf(this.mStationCount), Integer.valueOf(this.mChannelUtilization), Integer.valueOf(this.mCapacity), this.mAnt, Boolean.valueOf(this.mInternet), this.mVenueGroup, this.mVenueType, this.mHSRelease, Integer.valueOf(this.mAnqpDomainID), Integer.valueOf(this.mAnqpOICount), Utils.roamingConsortiumsToString(this.mRoamingConsortiums));
    }

    public String toKeyString() {
        if (this.mHESSID != 0) {
            return String.format("'%s':%012x (%012x)", this.mSSID, Long.valueOf(this.mBSSID), Long.valueOf(this.mHESSID));
        }
        return String.format("'%s':%012x", this.mSSID, Long.valueOf(this.mBSSID));
    }

    public String getBSSIDString() {
        return toMACString(this.mBSSID);
    }

    public static String toMACString(long mac) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int n = 5; n >= 0; n--) {
            if (first) {
                first = false;
            } else {
                sb.append(':');
            }
            sb.append(String.format("%02x", Long.valueOf((mac >>> (n * 8)) & 255)));
        }
        return sb.toString();
    }
}
