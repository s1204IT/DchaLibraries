package com.android.server.wifi.hotspot2;

import android.net.wifi.ScanResult;
import android.util.Log;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.RawByteElement;
import com.android.server.wifi.util.InformationElementUtil;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkDetail {
    private static final boolean DBG = false;
    private static final String TAG = "NetworkDetail:";
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
    private final boolean mIsHiddenSsid;
    private final int mMaxRate;
    private final int mPrimaryFreq;
    private final long[] mRoamingConsortiums;
    private final String mSSID;
    private final int mStationCount;
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
        Wildcard
    }

    public enum HSRelease {
        R1,
        R2,
        Unknown
    }

    public NetworkDetail(String str, ScanResult.InformationElement[] informationElementArr, List<String> list, int i) {
        Throwable th;
        byte[] bArr;
        byte[] bArr2;
        boolean z;
        String str2;
        int iIntValue;
        String str3;
        ScanResult.InformationElement[] informationElementArr2 = informationElementArr;
        this.mDtimInterval = -1;
        if (informationElementArr2 == null) {
            throw new IllegalArgumentException("Null information elements");
        }
        this.mBSSID = Utils.parseMac(str);
        InformationElementUtil.BssLoad bssLoad = new InformationElementUtil.BssLoad();
        InformationElementUtil.Interworking interworking = new InformationElementUtil.Interworking();
        InformationElementUtil.RoamingConsortium roamingConsortium = new InformationElementUtil.RoamingConsortium();
        InformationElementUtil.Vsa vsa = new InformationElementUtil.Vsa();
        InformationElementUtil.HtOperation htOperation = new InformationElementUtil.HtOperation();
        InformationElementUtil.VhtOperation vhtOperation = new InformationElementUtil.VhtOperation();
        InformationElementUtil.ExtendedCapabilities extendedCapabilities = new InformationElementUtil.ExtendedCapabilities();
        InformationElementUtil.TrafficIndicationMap trafficIndicationMap = new InformationElementUtil.TrafficIndicationMap();
        InformationElementUtil.SupportedRates supportedRates = new InformationElementUtil.SupportedRates();
        InformationElementUtil.SupportedRates supportedRates2 = new InformationElementUtil.SupportedRates();
        ArrayList arrayList = new ArrayList();
        try {
            int length = informationElementArr2.length;
            int i2 = 0;
            bArr = null;
            while (i2 < length) {
                try {
                    ScanResult.InformationElement informationElement = informationElementArr2[i2];
                    arrayList.add(Integer.valueOf(informationElement.id));
                    int i3 = informationElement.id;
                    int i4 = length;
                    if (i3 == 5) {
                        trafficIndicationMap.from(informationElement);
                    } else if (i3 == 11) {
                        bssLoad.from(informationElement);
                    } else if (i3 == 50) {
                        supportedRates2.from(informationElement);
                    } else if (i3 == 61) {
                        htOperation.from(informationElement);
                    } else if (i3 == 107) {
                        interworking.from(informationElement);
                    } else if (i3 == 111) {
                        roamingConsortium.from(informationElement);
                    } else if (i3 == 127) {
                        extendedCapabilities.from(informationElement);
                    } else if (i3 == 192) {
                        vhtOperation.from(informationElement);
                    } else if (i3 != 221) {
                        switch (i3) {
                            case 0:
                                bArr = informationElement.bytes;
                                break;
                            case 1:
                                supportedRates.from(informationElement);
                                break;
                        }
                    } else {
                        vsa.from(informationElement);
                    }
                    i2++;
                    length = i4;
                    informationElementArr2 = informationElementArr;
                } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e) {
                    th = e;
                    Log.d(Utils.hs2LogTag(getClass()), "Caught " + th);
                    if (bArr == null) {
                        throw new IllegalArgumentException("Malformed IE string (no SSID)", th);
                    }
                    bArr2 = bArr;
                    if (bArr2 == null) {
                    }
                    this.mSSID = str2;
                    this.mHESSID = interworking.hessid;
                    this.mIsHiddenSsid = z;
                    this.mStationCount = bssLoad.stationCount;
                    this.mChannelUtilization = bssLoad.channelUtilization;
                    this.mCapacity = bssLoad.capacity;
                    this.mAnt = interworking.ant;
                    this.mInternet = interworking.internet;
                    this.mHSRelease = vsa.hsRelease;
                    this.mAnqpDomainID = vsa.anqpDomainID;
                    this.mAnqpOICount = roamingConsortium.anqpOICount;
                    this.mRoamingConsortiums = roamingConsortium.getRoamingConsortiums();
                    this.mExtendedCapabilities = extendedCapabilities;
                    this.mANQPElements = null;
                    this.mPrimaryFreq = i;
                    if (!vhtOperation.isValid()) {
                    }
                    if (trafficIndicationMap.isValid()) {
                    }
                    if (!supportedRates2.isValid()) {
                    }
                    if (!supportedRates.isValid()) {
                    }
                }
            }
            bArr2 = bArr;
            th = null;
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e2) {
            th = e2;
            bArr = null;
        }
        if (bArr2 == null) {
            try {
                str3 = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bArr2)).toString();
            } catch (CharacterCodingException e3) {
                str3 = null;
            }
            if (str3 == null) {
                if (extendedCapabilities.isStrictUtf8() && th != null) {
                    throw new IllegalArgumentException("Failed to decode SSID in dubious IE string");
                }
                str3 = new String(bArr2, StandardCharsets.ISO_8859_1);
            }
            str2 = str3;
            int length2 = bArr2.length;
            int i5 = 0;
            while (true) {
                if (i5 < length2) {
                    if (bArr2[i5] == 0) {
                        i5++;
                    } else {
                        z = DBG;
                    }
                } else {
                    z = true;
                }
            }
        } else {
            z = DBG;
            str2 = null;
        }
        this.mSSID = str2;
        this.mHESSID = interworking.hessid;
        this.mIsHiddenSsid = z;
        this.mStationCount = bssLoad.stationCount;
        this.mChannelUtilization = bssLoad.channelUtilization;
        this.mCapacity = bssLoad.capacity;
        this.mAnt = interworking.ant;
        this.mInternet = interworking.internet;
        this.mHSRelease = vsa.hsRelease;
        this.mAnqpDomainID = vsa.anqpDomainID;
        this.mAnqpOICount = roamingConsortium.anqpOICount;
        this.mRoamingConsortiums = roamingConsortium.getRoamingConsortiums();
        this.mExtendedCapabilities = extendedCapabilities;
        this.mANQPElements = null;
        this.mPrimaryFreq = i;
        if (!vhtOperation.isValid()) {
            this.mChannelWidth = vhtOperation.getChannelWidth();
            this.mCenterfreq0 = vhtOperation.getCenterFreq0();
            this.mCenterfreq1 = vhtOperation.getCenterFreq1();
        } else {
            this.mChannelWidth = htOperation.getChannelWidth();
            this.mCenterfreq0 = htOperation.getCenterFreq0(this.mPrimaryFreq);
            this.mCenterfreq1 = 0;
        }
        if (trafficIndicationMap.isValid()) {
            this.mDtimInterval = trafficIndicationMap.mDtimPeriod;
        }
        if (!supportedRates2.isValid()) {
            iIntValue = supportedRates2.mRates.get(supportedRates2.mRates.size() - 1).intValue();
        } else {
            iIntValue = 0;
        }
        if (!supportedRates.isValid()) {
            int iIntValue2 = supportedRates.mRates.get(supportedRates.mRates.size() - 1).intValue();
            this.mMaxRate = iIntValue2 > iIntValue ? iIntValue2 : iIntValue;
            this.mWifiMode = InformationElementUtil.WifiMode.determineMode(this.mPrimaryFreq, this.mMaxRate, vhtOperation.isValid(), arrayList.contains(61), arrayList.contains(42));
        } else {
            this.mWifiMode = 0;
            this.mMaxRate = 0;
        }
    }

    private static ByteBuffer getAndAdvancePayload(ByteBuffer byteBuffer, int i) {
        ByteBuffer byteBufferOrder = byteBuffer.duplicate().order(byteBuffer.order());
        byteBufferOrder.limit(byteBufferOrder.position() + i);
        byteBuffer.position(byteBuffer.position() + i);
        return byteBufferOrder;
    }

    private NetworkDetail(NetworkDetail networkDetail, Map<Constants.ANQPElementType, ANQPElement> map) {
        this.mDtimInterval = -1;
        this.mSSID = networkDetail.mSSID;
        this.mIsHiddenSsid = networkDetail.mIsHiddenSsid;
        this.mBSSID = networkDetail.mBSSID;
        this.mHESSID = networkDetail.mHESSID;
        this.mStationCount = networkDetail.mStationCount;
        this.mChannelUtilization = networkDetail.mChannelUtilization;
        this.mCapacity = networkDetail.mCapacity;
        this.mAnt = networkDetail.mAnt;
        this.mInternet = networkDetail.mInternet;
        this.mHSRelease = networkDetail.mHSRelease;
        this.mAnqpDomainID = networkDetail.mAnqpDomainID;
        this.mAnqpOICount = networkDetail.mAnqpOICount;
        this.mRoamingConsortiums = networkDetail.mRoamingConsortiums;
        this.mExtendedCapabilities = new InformationElementUtil.ExtendedCapabilities(networkDetail.mExtendedCapabilities);
        this.mANQPElements = map;
        this.mChannelWidth = networkDetail.mChannelWidth;
        this.mPrimaryFreq = networkDetail.mPrimaryFreq;
        this.mCenterfreq0 = networkDetail.mCenterfreq0;
        this.mCenterfreq1 = networkDetail.mCenterfreq1;
        this.mDtimInterval = networkDetail.mDtimInterval;
        this.mWifiMode = networkDetail.mWifiMode;
        this.mMaxRate = networkDetail.mMaxRate;
    }

    public NetworkDetail complete(Map<Constants.ANQPElementType, ANQPElement> map) {
        return new NetworkDetail(this, map);
    }

    public boolean queriable(List<Constants.ANQPElementType> list) {
        if (this.mAnt == null || !(Constants.hasBaseANQPElements(list) || (Constants.hasR2Elements(list) && this.mHSRelease == HSRelease.R2))) {
            return DBG;
        }
        return true;
    }

    public boolean has80211uInfo() {
        if (this.mAnt == null && this.mRoamingConsortiums == null && this.mHSRelease == null) {
            return DBG;
        }
        return true;
    }

    public boolean hasInterworking() {
        if (this.mAnt != null) {
            return true;
        }
        return DBG;
    }

    public String getSSID() {
        return this.mSSID;
    }

    public String getTrimmedSSID() {
        if (this.mSSID != null) {
            for (int i = 0; i < this.mSSID.length(); i++) {
                if (this.mSSID.charAt(i) != 0) {
                    return this.mSSID;
                }
            }
            return "";
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
        if (this.mAnt != null) {
            return true;
        }
        return DBG;
    }

    public Ant getAnt() {
        return this.mAnt;
    }

    public boolean isInternet() {
        return this.mInternet;
    }

    public HSRelease getHSRelease() {
        return this.mHSRelease;
    }

    public int getAnqpDomainID() {
        return this.mAnqpDomainID;
    }

    public byte[] getOsuProviders() {
        ANQPElement aNQPElement;
        if (this.mANQPElements == null || (aNQPElement = this.mANQPElements.get(Constants.ANQPElementType.HSOSUProviders)) == null) {
            return null;
        }
        return ((RawByteElement) aNQPElement).getPayload();
    }

    public int getAnqpOICount() {
        return this.mAnqpOICount;
    }

    public long[] getRoamingConsortiums() {
        return this.mRoamingConsortiums;
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
        return this.mExtendedCapabilities.is80211McRTTResponder();
    }

    public boolean isSSID_UTF8() {
        return this.mExtendedCapabilities.isStrictUtf8();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return DBG;
        }
        NetworkDetail networkDetail = (NetworkDetail) obj;
        if (getSSID().equals(networkDetail.getSSID()) && getBSSID() == networkDetail.getBSSID()) {
            return true;
        }
        return DBG;
    }

    public int hashCode() {
        return (((this.mSSID.hashCode() * 31) + ((int) (this.mBSSID >>> 32))) * 31) + ((int) this.mBSSID);
    }

    public String toString() {
        return String.format("NetworkInfo{SSID='%s', HESSID=%x, BSSID=%x, StationCount=%d, ChannelUtilization=%d, Capacity=%d, Ant=%s, Internet=%s, HSRelease=%s, AnqpDomainID=%d, AnqpOICount=%d, RoamingConsortiums=%s}", this.mSSID, Long.valueOf(this.mHESSID), Long.valueOf(this.mBSSID), Integer.valueOf(this.mStationCount), Integer.valueOf(this.mChannelUtilization), Integer.valueOf(this.mCapacity), this.mAnt, Boolean.valueOf(this.mInternet), this.mHSRelease, Integer.valueOf(this.mAnqpDomainID), Integer.valueOf(this.mAnqpOICount), Utils.roamingConsortiumsToString(this.mRoamingConsortiums));
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

    public boolean isBeaconFrame() {
        if (this.mDtimInterval > 0) {
            return true;
        }
        return DBG;
    }

    public boolean isHiddenBeaconFrame() {
        if (isBeaconFrame() && this.mIsHiddenSsid) {
            return true;
        }
        return DBG;
    }

    public static String toMACString(long j) {
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        for (int i = 5; i >= 0; i--) {
            if (!z) {
                sb.append(':');
            } else {
                z = false;
            }
            sb.append(String.format("%02x", Long.valueOf((j >>> (i * 8)) & 255)));
        }
        return sb.toString();
    }
}
