package android.net.wifi;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ScanResult implements Parcelable {
    public static final int CHANNEL_WIDTH_160MHZ = 3;
    public static final int CHANNEL_WIDTH_20MHZ = 0;
    public static final int CHANNEL_WIDTH_40MHZ = 1;
    public static final int CHANNEL_WIDTH_80MHZ = 2;
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;
    public static final int CIPHER_CCMP = 3;
    public static final int CIPHER_NONE = 0;
    public static final int CIPHER_NO_GROUP_ADDRESSED = 1;
    public static final int CIPHER_TKIP = 2;
    public static final Parcelable.Creator<ScanResult> CREATOR = new Parcelable.Creator<ScanResult>() {
        @Override
        public ScanResult createFromParcel(Parcel parcel) {
            WifiSsid wifiSsidCreateFromParcel;
            if (parcel.readInt() == 1) {
                wifiSsidCreateFromParcel = WifiSsid.CREATOR.createFromParcel(parcel);
            } else {
                wifiSsidCreateFromParcel = null;
            }
            ScanResult scanResult = new ScanResult(wifiSsidCreateFromParcel, parcel.readString(), parcel.readString(), parcel.readLong(), parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readLong(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), false);
            scanResult.seen = parcel.readLong();
            scanResult.untrusted = parcel.readInt() != 0;
            scanResult.numUsage = parcel.readInt();
            scanResult.venueName = parcel.readString();
            scanResult.operatorFriendlyName = parcel.readString();
            scanResult.flags = parcel.readLong();
            int i = parcel.readInt();
            if (i != 0) {
                scanResult.informationElements = new InformationElement[i];
                for (int i2 = 0; i2 < i; i2++) {
                    scanResult.informationElements[i2] = new InformationElement();
                    scanResult.informationElements[i2].id = parcel.readInt();
                    scanResult.informationElements[i2].bytes = new byte[parcel.readInt()];
                    parcel.readByteArray(scanResult.informationElements[i2].bytes);
                }
            }
            int i3 = parcel.readInt();
            if (i3 != 0) {
                scanResult.anqpLines = new ArrayList();
                for (int i4 = 0; i4 < i3; i4++) {
                    scanResult.anqpLines.add(parcel.readString());
                }
            }
            int i5 = parcel.readInt();
            if (i5 != 0) {
                scanResult.anqpElements = new AnqpInformationElement[i5];
                for (int i6 = 0; i6 < i5; i6++) {
                    int i7 = parcel.readInt();
                    int i8 = parcel.readInt();
                    byte[] bArr = new byte[parcel.readInt()];
                    parcel.readByteArray(bArr);
                    scanResult.anqpElements[i6] = new AnqpInformationElement(i7, i8, bArr);
                }
            }
            scanResult.isCarrierAp = parcel.readInt() != 0;
            scanResult.carrierApEapType = parcel.readInt();
            scanResult.carrierName = parcel.readString();
            int i9 = parcel.readInt();
            if (i9 != 0) {
                scanResult.radioChainInfos = new RadioChainInfo[i9];
                for (int i10 = 0; i10 < i9; i10++) {
                    scanResult.radioChainInfos[i10] = new RadioChainInfo();
                    scanResult.radioChainInfos[i10].id = parcel.readInt();
                    scanResult.radioChainInfos[i10].level = parcel.readInt();
                }
            }
            return scanResult;
        }

        @Override
        public ScanResult[] newArray(int i) {
            return new ScanResult[i];
        }
    };
    public static final long FLAG_80211mc_RESPONDER = 2;
    public static final long FLAG_PASSPOINT_NETWORK = 1;
    public static final int KEY_MGMT_EAP = 2;
    public static final int KEY_MGMT_EAP_SHA256 = 6;
    public static final int KEY_MGMT_FT_EAP = 4;
    public static final int KEY_MGMT_FT_PSK = 3;
    public static final int KEY_MGMT_NONE = 0;
    public static final int KEY_MGMT_OSEN = 7;
    public static final int KEY_MGMT_PSK = 1;
    public static final int KEY_MGMT_PSK_SHA256 = 5;
    public static final int PROTOCOL_NONE = 0;
    public static final int PROTOCOL_OSEN = 3;
    public static final int PROTOCOL_WPA = 1;
    public static final int PROTOCOL_WPA2 = 2;
    public static final int UNSPECIFIED = -1;
    public String BSSID;
    public String SSID;
    public int anqpDomainId;
    public AnqpInformationElement[] anqpElements;
    public List<String> anqpLines;
    public String capabilities;
    public int carrierApEapType;
    public String carrierName;
    public int centerFreq0;
    public int centerFreq1;
    public int channelWidth;
    public int distanceCm;
    public int distanceSdCm;
    public long flags;
    public int frequency;
    public long hessid;
    public InformationElement[] informationElements;
    public boolean is80211McRTTResponder;
    public boolean isCarrierAp;
    public int level;
    public int numUsage;
    public CharSequence operatorFriendlyName;
    public RadioChainInfo[] radioChainInfos;
    public long seen;
    public long timestamp;

    @SystemApi
    public boolean untrusted;
    public CharSequence venueName;
    public WifiSsid wifiSsid;

    public static class RadioChainInfo {
        public int id;
        public int level;

        public String toString() {
            return "RadioChainInfo: id=" + this.id + ", level=" + this.level;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RadioChainInfo)) {
                return false;
            }
            RadioChainInfo radioChainInfo = (RadioChainInfo) obj;
            return this.id == radioChainInfo.id && this.level == radioChainInfo.level;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.id), Integer.valueOf(this.level));
        }
    }

    public void setFlag(long j) {
        this.flags = j | this.flags;
    }

    public void clearFlag(long j) {
        this.flags = (~j) & this.flags;
    }

    public boolean is80211mcResponder() {
        return (this.flags & 2) != 0;
    }

    public boolean isPasspointNetwork() {
        return (this.flags & 1) != 0;
    }

    public boolean is24GHz() {
        return is24GHz(this.frequency);
    }

    public static boolean is24GHz(int i) {
        return i > 2400 && i < 2500;
    }

    public boolean is5GHz() {
        return is5GHz(this.frequency);
    }

    public static boolean is5GHz(int i) {
        return i > 4900 && i < 5900;
    }

    public static class InformationElement {
        public static final int EID_BSS_LOAD = 11;
        public static final int EID_ERP = 42;
        public static final int EID_EXTENDED_CAPS = 127;
        public static final int EID_EXTENDED_SUPPORTED_RATES = 50;
        public static final int EID_HT_CAPABILITIES = 45;
        public static final int EID_HT_OPERATION = 61;
        public static final int EID_INTERWORKING = 107;
        public static final int EID_ROAMING_CONSORTIUM = 111;
        public static final int EID_RSN = 48;
        public static final int EID_SSID = 0;
        public static final int EID_SUPPORTED_RATES = 1;
        public static final int EID_TIM = 5;
        public static final int EID_VHT_CAPABILITIES = 191;
        public static final int EID_VHT_OPERATION = 192;
        public static final int EID_VSA = 221;
        public byte[] bytes;
        public int id;

        public InformationElement() {
        }

        public InformationElement(InformationElement informationElement) {
            this.id = informationElement.id;
            this.bytes = (byte[]) informationElement.bytes.clone();
        }
    }

    public ScanResult(WifiSsid wifiSsid, String str, long j, int i, byte[] bArr, String str2, int i2, int i3, long j2) {
        this.wifiSsid = wifiSsid;
        this.SSID = wifiSsid != null ? wifiSsid.toString() : WifiSsid.NONE;
        this.BSSID = str;
        this.hessid = j;
        this.anqpDomainId = i;
        if (bArr != null) {
            this.anqpElements = new AnqpInformationElement[1];
            this.anqpElements[0] = new AnqpInformationElement(AnqpInformationElement.HOTSPOT20_VENDOR_ID, 8, bArr);
        }
        this.capabilities = str2;
        this.level = i2;
        this.frequency = i3;
        this.timestamp = j2;
        this.distanceCm = -1;
        this.distanceSdCm = -1;
        this.channelWidth = -1;
        this.centerFreq0 = -1;
        this.centerFreq1 = -1;
        this.flags = 0L;
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public ScanResult(WifiSsid wifiSsid, String str, String str2, int i, int i2, long j, int i3, int i4) {
        this.wifiSsid = wifiSsid;
        this.SSID = wifiSsid != null ? wifiSsid.toString() : WifiSsid.NONE;
        this.BSSID = str;
        this.capabilities = str2;
        this.level = i;
        this.frequency = i2;
        this.timestamp = j;
        this.distanceCm = i3;
        this.distanceSdCm = i4;
        this.channelWidth = -1;
        this.centerFreq0 = -1;
        this.centerFreq1 = -1;
        this.flags = 0L;
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public ScanResult(String str, String str2, long j, int i, String str3, int i2, int i3, long j2, int i4, int i5, int i6, int i7, int i8, boolean z) {
        this.SSID = str;
        this.BSSID = str2;
        this.hessid = j;
        this.anqpDomainId = i;
        this.capabilities = str3;
        this.level = i2;
        this.frequency = i3;
        this.timestamp = j2;
        this.distanceCm = i4;
        this.distanceSdCm = i5;
        this.channelWidth = i6;
        this.centerFreq0 = i7;
        this.centerFreq1 = i8;
        if (z) {
            this.flags = 2L;
        } else {
            this.flags = 0L;
        }
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public ScanResult(WifiSsid wifiSsid, String str, String str2, long j, int i, String str3, int i2, int i3, long j2, int i4, int i5, int i6, int i7, int i8, boolean z) {
        this(str, str2, j, i, str3, i2, i3, j2, i4, i5, i6, i7, i8, z);
        this.wifiSsid = wifiSsid;
    }

    public ScanResult(ScanResult scanResult) {
        if (scanResult != null) {
            this.wifiSsid = scanResult.wifiSsid;
            this.SSID = scanResult.SSID;
            this.BSSID = scanResult.BSSID;
            this.hessid = scanResult.hessid;
            this.anqpDomainId = scanResult.anqpDomainId;
            this.informationElements = scanResult.informationElements;
            this.anqpElements = scanResult.anqpElements;
            this.capabilities = scanResult.capabilities;
            this.level = scanResult.level;
            this.frequency = scanResult.frequency;
            this.channelWidth = scanResult.channelWidth;
            this.centerFreq0 = scanResult.centerFreq0;
            this.centerFreq1 = scanResult.centerFreq1;
            this.timestamp = scanResult.timestamp;
            this.distanceCm = scanResult.distanceCm;
            this.distanceSdCm = scanResult.distanceSdCm;
            this.seen = scanResult.seen;
            this.untrusted = scanResult.untrusted;
            this.numUsage = scanResult.numUsage;
            this.venueName = scanResult.venueName;
            this.operatorFriendlyName = scanResult.operatorFriendlyName;
            this.flags = scanResult.flags;
            this.isCarrierAp = scanResult.isCarrierAp;
            this.carrierApEapType = scanResult.carrierApEapType;
            this.carrierName = scanResult.carrierName;
            this.radioChainInfos = scanResult.radioChainInfos;
        }
    }

    public ScanResult() {
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("SSID: ");
        stringBuffer.append(this.wifiSsid == null ? WifiSsid.NONE : this.wifiSsid);
        stringBuffer.append(", BSSID: ");
        stringBuffer.append(this.BSSID == null ? "<none>" : this.BSSID);
        stringBuffer.append(", capabilities: ");
        stringBuffer.append(this.capabilities != null ? this.capabilities : "<none>");
        stringBuffer.append(", level: ");
        stringBuffer.append(this.level);
        stringBuffer.append(", frequency: ");
        stringBuffer.append(this.frequency);
        stringBuffer.append(", timestamp: ");
        stringBuffer.append(this.timestamp);
        stringBuffer.append(", distance: ");
        stringBuffer.append(this.distanceCm != -1 ? Integer.valueOf(this.distanceCm) : "?");
        stringBuffer.append("(cm)");
        stringBuffer.append(", distanceSd: ");
        stringBuffer.append(this.distanceSdCm != -1 ? Integer.valueOf(this.distanceSdCm) : "?");
        stringBuffer.append("(cm)");
        stringBuffer.append(", passpoint: ");
        stringBuffer.append((this.flags & 1) != 0 ? "yes" : "no");
        stringBuffer.append(", ChannelBandwidth: ");
        stringBuffer.append(this.channelWidth);
        stringBuffer.append(", centerFreq0: ");
        stringBuffer.append(this.centerFreq0);
        stringBuffer.append(", centerFreq1: ");
        stringBuffer.append(this.centerFreq1);
        stringBuffer.append(", 80211mcResponder: ");
        stringBuffer.append((this.flags & 2) != 0 ? "is supported" : "is not supported");
        stringBuffer.append(", Carrier AP: ");
        stringBuffer.append(this.isCarrierAp ? "yes" : "no");
        stringBuffer.append(", Carrier AP EAP Type: ");
        stringBuffer.append(this.carrierApEapType);
        stringBuffer.append(", Carrier name: ");
        stringBuffer.append(this.carrierName);
        stringBuffer.append(", Radio Chain Infos: ");
        stringBuffer.append(Arrays.toString(this.radioChainInfos));
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.wifiSsid != null) {
            parcel.writeInt(1);
            this.wifiSsid.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeString(this.SSID);
        parcel.writeString(this.BSSID);
        parcel.writeLong(this.hessid);
        parcel.writeInt(this.anqpDomainId);
        parcel.writeString(this.capabilities);
        parcel.writeInt(this.level);
        parcel.writeInt(this.frequency);
        parcel.writeLong(this.timestamp);
        parcel.writeInt(this.distanceCm);
        parcel.writeInt(this.distanceSdCm);
        parcel.writeInt(this.channelWidth);
        parcel.writeInt(this.centerFreq0);
        parcel.writeInt(this.centerFreq1);
        parcel.writeLong(this.seen);
        parcel.writeInt(this.untrusted ? 1 : 0);
        parcel.writeInt(this.numUsage);
        parcel.writeString(this.venueName != null ? this.venueName.toString() : "");
        parcel.writeString(this.operatorFriendlyName != null ? this.operatorFriendlyName.toString() : "");
        parcel.writeLong(this.flags);
        if (this.informationElements != null) {
            parcel.writeInt(this.informationElements.length);
            for (int i2 = 0; i2 < this.informationElements.length; i2++) {
                parcel.writeInt(this.informationElements[i2].id);
                parcel.writeInt(this.informationElements[i2].bytes.length);
                parcel.writeByteArray(this.informationElements[i2].bytes);
            }
        } else {
            parcel.writeInt(0);
        }
        if (this.anqpLines != null) {
            parcel.writeInt(this.anqpLines.size());
            for (int i3 = 0; i3 < this.anqpLines.size(); i3++) {
                parcel.writeString(this.anqpLines.get(i3));
            }
        } else {
            parcel.writeInt(0);
        }
        if (this.anqpElements != null) {
            parcel.writeInt(this.anqpElements.length);
            for (AnqpInformationElement anqpInformationElement : this.anqpElements) {
                parcel.writeInt(anqpInformationElement.getVendorId());
                parcel.writeInt(anqpInformationElement.getElementId());
                parcel.writeInt(anqpInformationElement.getPayload().length);
                parcel.writeByteArray(anqpInformationElement.getPayload());
            }
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.isCarrierAp ? 1 : 0);
        parcel.writeInt(this.carrierApEapType);
        parcel.writeString(this.carrierName);
        if (this.radioChainInfos != null) {
            parcel.writeInt(this.radioChainInfos.length);
            for (int i4 = 0; i4 < this.radioChainInfos.length; i4++) {
                parcel.writeInt(this.radioChainInfos[i4].id);
                parcel.writeInt(this.radioChainInfos[i4].level);
            }
            return;
        }
        parcel.writeInt(0);
    }
}
