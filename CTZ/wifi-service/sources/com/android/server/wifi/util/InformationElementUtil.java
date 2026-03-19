package com.android.server.wifi.util;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.net.wifi.ScanResult;
import android.util.Log;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.NetworkDetail;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

public class InformationElementUtil {
    private static final String TAG = "InformationElementUtil";

    public static ScanResult.InformationElement[] parseInformationElements(byte[] bArr) {
        boolean z = false;
        if (bArr == null) {
            return new ScanResult.InformationElement[0];
        }
        ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN);
        ArrayList arrayList = new ArrayList();
        while (byteBufferOrder.remaining() > 1) {
            int i = byteBufferOrder.get() & 255;
            int i2 = byteBufferOrder.get() & 255;
            if (i2 > byteBufferOrder.remaining() || (i == 0 && z)) {
                break;
            }
            if (i == 0) {
                z = true;
            }
            ScanResult.InformationElement informationElement = new ScanResult.InformationElement();
            informationElement.id = i;
            informationElement.bytes = new byte[i2];
            byteBufferOrder.get(informationElement.bytes);
            arrayList.add(informationElement);
        }
        return (ScanResult.InformationElement[]) arrayList.toArray(new ScanResult.InformationElement[arrayList.size()]);
    }

    public static RoamingConsortium getRoamingConsortiumIE(ScanResult.InformationElement[] informationElementArr) {
        RoamingConsortium roamingConsortium = new RoamingConsortium();
        if (informationElementArr != null) {
            for (ScanResult.InformationElement informationElement : informationElementArr) {
                if (informationElement.id == 111) {
                    try {
                        roamingConsortium.from(informationElement);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to parse Roaming Consortium IE: " + e.getMessage());
                    }
                }
            }
        }
        return roamingConsortium;
    }

    public static Vsa getHS2VendorSpecificIE(ScanResult.InformationElement[] informationElementArr) {
        Vsa vsa = new Vsa();
        if (informationElementArr != null) {
            for (ScanResult.InformationElement informationElement : informationElementArr) {
                if (informationElement.id == 221) {
                    try {
                        vsa.from(informationElement);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to parse Vendor Specific IE: " + e.getMessage());
                    }
                }
            }
        }
        return vsa;
    }

    public static Interworking getInterworkingIE(ScanResult.InformationElement[] informationElementArr) {
        Interworking interworking = new Interworking();
        if (informationElementArr != null) {
            for (ScanResult.InformationElement informationElement : informationElementArr) {
                if (informationElement.id == 107) {
                    try {
                        interworking.from(informationElement);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to parse Interworking IE: " + e.getMessage());
                    }
                }
            }
        }
        return interworking;
    }

    public static class BssLoad {
        public int stationCount = 0;
        public int channelUtilization = 0;
        public int capacity = 0;

        public void from(ScanResult.InformationElement informationElement) {
            if (informationElement.id != 11) {
                throw new IllegalArgumentException("Element id is not BSS_LOAD, : " + informationElement.id);
            }
            if (informationElement.bytes.length != 5) {
                throw new IllegalArgumentException("BSS Load element length is not 5: " + informationElement.bytes.length);
            }
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            this.stationCount = byteBufferOrder.getShort() & 65535;
            this.channelUtilization = byteBufferOrder.get() & 255;
            this.capacity = byteBufferOrder.getShort() & 65535;
        }
    }

    public static class HtOperation {
        public int secondChannelOffset = 0;

        public int getChannelWidth() {
            if (this.secondChannelOffset != 0) {
                return 1;
            }
            return 0;
        }

        public int getCenterFreq0(int i) {
            if (this.secondChannelOffset == 0) {
                return 0;
            }
            if (this.secondChannelOffset == 1) {
                return i + 10;
            }
            if (this.secondChannelOffset == 3) {
                return i - 10;
            }
            Log.e("HtOperation", "Error on secondChannelOffset: " + this.secondChannelOffset);
            return 0;
        }

        public void from(ScanResult.InformationElement informationElement) {
            if (informationElement.id != 61) {
                throw new IllegalArgumentException("Element id is not HT_OPERATION, : " + informationElement.id);
            }
            this.secondChannelOffset = informationElement.bytes[1] & 3;
        }
    }

    public static class VhtOperation {
        public int channelMode = 0;
        public int centerFreqIndex1 = 0;
        public int centerFreqIndex2 = 0;

        public boolean isValid() {
            return this.channelMode != 0;
        }

        public int getChannelWidth() {
            return this.channelMode + 1;
        }

        public int getCenterFreq0() {
            return ((this.centerFreqIndex1 - 36) * 5) + 5180;
        }

        public int getCenterFreq1() {
            if (this.channelMode > 1) {
                return ((this.centerFreqIndex2 - 36) * 5) + 5180;
            }
            return 0;
        }

        public void from(ScanResult.InformationElement informationElement) {
            if (informationElement.id != 192) {
                throw new IllegalArgumentException("Element id is not VHT_OPERATION, : " + informationElement.id);
            }
            this.channelMode = informationElement.bytes[0] & 255;
            this.centerFreqIndex1 = informationElement.bytes[1] & 255;
            this.centerFreqIndex2 = informationElement.bytes[2] & 255;
        }
    }

    public static class Interworking {
        public NetworkDetail.Ant ant = null;
        public boolean internet = false;
        public long hessid = 0;

        public void from(ScanResult.InformationElement informationElement) {
            if (informationElement.id != 107) {
                throw new IllegalArgumentException("Element id is not INTERWORKING, : " + informationElement.id);
            }
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            int i = byteBufferOrder.get() & 255;
            this.ant = NetworkDetail.Ant.values()[i & 15];
            this.internet = (i & 16) != 0;
            if (informationElement.bytes.length != 1 && informationElement.bytes.length != 3 && informationElement.bytes.length != 7 && informationElement.bytes.length != 9) {
                throw new IllegalArgumentException("Bad Interworking element length: " + informationElement.bytes.length);
            }
            if (informationElement.bytes.length == 3 || informationElement.bytes.length == 9) {
                ByteBufferReader.readInteger(byteBufferOrder, ByteOrder.BIG_ENDIAN, 2);
            }
            if (informationElement.bytes.length == 7 || informationElement.bytes.length == 9) {
                this.hessid = ByteBufferReader.readInteger(byteBufferOrder, ByteOrder.BIG_ENDIAN, 6);
            }
        }
    }

    public static class RoamingConsortium {
        public int anqpOICount = 0;
        private long[] roamingConsortiums = null;

        public long[] getRoamingConsortiums() {
            return this.roamingConsortiums;
        }

        public void from(ScanResult.InformationElement informationElement) {
            int i;
            if (informationElement.id != 111) {
                throw new IllegalArgumentException("Element id is not ROAMING_CONSORTIUM, : " + informationElement.id);
            }
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            this.anqpOICount = byteBufferOrder.get() & 255;
            int i2 = byteBufferOrder.get() & 255;
            int i3 = i2 & 15;
            int i4 = (i2 >>> 4) & 15;
            int length = ((informationElement.bytes.length - 2) - i3) - i4;
            if (i3 <= 0) {
                i = 0;
            } else if (i4 <= 0) {
                i = 1;
            } else if (length > 0) {
                i = 3;
            } else {
                i = 2;
            }
            this.roamingConsortiums = new long[i];
            if (i3 > 0 && this.roamingConsortiums.length > 0) {
                this.roamingConsortiums[0] = ByteBufferReader.readInteger(byteBufferOrder, ByteOrder.BIG_ENDIAN, i3);
            }
            if (i4 > 0 && this.roamingConsortiums.length > 1) {
                this.roamingConsortiums[1] = ByteBufferReader.readInteger(byteBufferOrder, ByteOrder.BIG_ENDIAN, i4);
            }
            if (length > 0 && this.roamingConsortiums.length > 2) {
                this.roamingConsortiums[2] = ByteBufferReader.readInteger(byteBufferOrder, ByteOrder.BIG_ENDIAN, length);
            }
        }
    }

    public static class Vsa {
        private static final int ANQP_DOMID_BIT = 4;
        public NetworkDetail.HSRelease hsRelease = null;
        public int anqpDomainID = 0;

        public void from(ScanResult.InformationElement informationElement) {
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            if (informationElement.bytes.length >= 5 && byteBufferOrder.getInt() == 278556496) {
                int i = byteBufferOrder.get() & 255;
                switch ((i >> 4) & 15) {
                    case 0:
                        this.hsRelease = NetworkDetail.HSRelease.R1;
                        break;
                    case 1:
                        this.hsRelease = NetworkDetail.HSRelease.R2;
                        break;
                    default:
                        this.hsRelease = NetworkDetail.HSRelease.Unknown;
                        break;
                }
                if ((i & 4) != 0) {
                    if (informationElement.bytes.length < 7) {
                        throw new IllegalArgumentException("HS20 indication element too short: " + informationElement.bytes.length);
                    }
                    this.anqpDomainID = byteBufferOrder.getShort() & 65535;
                }
            }
        }
    }

    public static class ExtendedCapabilities {
        private static final int RTT_RESP_ENABLE_BIT = 70;
        private static final int SSID_UTF8_BIT = 48;
        public BitSet capabilitiesBitSet;

        public boolean isStrictUtf8() {
            return this.capabilitiesBitSet.get(48);
        }

        public boolean is80211McRTTResponder() {
            return this.capabilitiesBitSet.get(RTT_RESP_ENABLE_BIT);
        }

        public ExtendedCapabilities() {
            this.capabilitiesBitSet = new BitSet();
        }

        public ExtendedCapabilities(ExtendedCapabilities extendedCapabilities) {
            this.capabilitiesBitSet = extendedCapabilities.capabilitiesBitSet;
        }

        public void from(ScanResult.InformationElement informationElement) {
            this.capabilitiesBitSet = BitSet.valueOf(informationElement.bytes);
        }
    }

    public static class Capabilities {
        private static final int CAP_ESS_BIT_OFFSET = 0;
        private static final int CAP_PRIVACY_BIT_OFFSET = 4;
        private static final short RSNE_VERSION = 1;
        private static final int RSN_CIPHER_CCMP = 78384896;
        private static final int RSN_CIPHER_NONE = 11276032;
        private static final int RSN_CIPHER_NO_GROUP_ADDRESSED = 128716544;
        private static final int RSN_CIPHER_TKIP = 44830464;
        private static final int WPA2_AKM_EAP = 28053248;
        private static final int WPA2_AKM_EAP_SHA256 = 95162112;
        private static final int WPA2_AKM_FT_EAP = 61607680;
        private static final int WPA2_AKM_FT_PSK = 78384896;
        private static final int WPA2_AKM_PSK = 44830464;
        private static final int WPA2_AKM_PSK_SHA256 = 111939328;
        private static final int WPA_AKM_EAP = 32657408;
        private static final int WPA_AKM_PSK = 49434624;
        private static final int WPA_CIPHER_CCMP = 82989056;
        private static final int WPA_CIPHER_NONE = 15880192;
        private static final int WPA_CIPHER_TKIP = 49434624;
        private static final int WPA_VENDOR_OUI_TYPE_ONE = 32657408;
        private static final short WPA_VENDOR_OUI_VERSION = 1;
        private static final int WPS_VENDOR_OUI_TYPE = 82989056;
        public ArrayList<Integer> groupCipher;
        public boolean isESS;
        public boolean isPrivacy;
        public boolean isWPS;
        public ArrayList<ArrayList<Integer>> keyManagement;
        public ArrayList<ArrayList<Integer>> pairwiseCipher;
        public ArrayList<Integer> protocol;

        private void parseRsnElement(ScanResult.InformationElement informationElement) {
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                if (byteBufferOrder.getShort() != 1) {
                    return;
                }
                this.protocol.add(2);
                this.groupCipher.add(Integer.valueOf(parseRsnCipher(byteBufferOrder.getInt())));
                short s = byteBufferOrder.getShort();
                ArrayList<Integer> arrayList = new ArrayList<>();
                for (int i = 0; i < s; i++) {
                    arrayList.add(Integer.valueOf(parseRsnCipher(byteBufferOrder.getInt())));
                }
                this.pairwiseCipher.add(arrayList);
                short s2 = byteBufferOrder.getShort();
                ArrayList<Integer> arrayList2 = new ArrayList<>();
                for (int i2 = 0; i2 < s2; i2++) {
                    switch (byteBufferOrder.getInt()) {
                        case WPA2_AKM_EAP:
                            arrayList2.add(2);
                            break;
                        case 44830464:
                            arrayList2.add(1);
                            break;
                        case WPA2_AKM_FT_EAP:
                            arrayList2.add(4);
                            break;
                        case 78384896:
                            arrayList2.add(3);
                            break;
                        case WPA2_AKM_EAP_SHA256:
                            arrayList2.add(6);
                            break;
                        case WPA2_AKM_PSK_SHA256:
                            arrayList2.add(5);
                            break;
                    }
                }
                if (arrayList2.isEmpty()) {
                    arrayList2.add(2);
                }
                this.keyManagement.add(arrayList2);
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse RSNE, buffer underflow");
            }
        }

        private static int parseWpaCipher(int i) {
            if (i == WPA_CIPHER_NONE) {
                return 0;
            }
            if (i == 49434624) {
                return 2;
            }
            if (i == 82989056) {
                return 3;
            }
            Log.w("IE_Capabilities", "Unknown WPA cipher suite: " + Integer.toHexString(i));
            return 0;
        }

        private static int parseRsnCipher(int i) {
            if (i == RSN_CIPHER_NONE) {
                return 0;
            }
            if (i == 44830464) {
                return 2;
            }
            if (i == 78384896) {
                return 3;
            }
            if (i == RSN_CIPHER_NO_GROUP_ADDRESSED) {
                return 1;
            }
            Log.w("IE_Capabilities", "Unknown RSN cipher suite: " + Integer.toHexString(i));
            return 0;
        }

        private static boolean isWpsElement(ScanResult.InformationElement informationElement) {
            try {
                return ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() == 82989056;
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse VSA IE, buffer underflow");
                return false;
            }
        }

        private static boolean isWpaOneElement(ScanResult.InformationElement informationElement) {
            try {
                return ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() == 32657408;
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse VSA IE, buffer underflow");
                return false;
            }
        }

        private void parseWpaOneElement(ScanResult.InformationElement informationElement) {
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                byteBufferOrder.getInt();
                if (byteBufferOrder.getShort() != 1) {
                    return;
                }
                this.protocol.add(1);
                this.groupCipher.add(Integer.valueOf(parseWpaCipher(byteBufferOrder.getInt())));
                short s = byteBufferOrder.getShort();
                ArrayList<Integer> arrayList = new ArrayList<>();
                for (int i = 0; i < s; i++) {
                    arrayList.add(Integer.valueOf(parseWpaCipher(byteBufferOrder.getInt())));
                }
                this.pairwiseCipher.add(arrayList);
                short s2 = byteBufferOrder.getShort();
                ArrayList<Integer> arrayList2 = new ArrayList<>();
                for (int i2 = 0; i2 < s2; i2++) {
                    int i3 = byteBufferOrder.getInt();
                    if (i3 == 32657408) {
                        arrayList2.add(2);
                    } else if (i3 == 49434624) {
                        arrayList2.add(1);
                    }
                }
                if (arrayList2.isEmpty()) {
                    arrayList2.add(2);
                }
                this.keyManagement.add(arrayList2);
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse type 1 WPA, buffer underflow");
            }
        }

        public void from(ScanResult.InformationElement[] informationElementArr, BitSet bitSet) {
            this.protocol = new ArrayList<>();
            this.keyManagement = new ArrayList<>();
            this.groupCipher = new ArrayList<>();
            this.pairwiseCipher = new ArrayList<>();
            if (informationElementArr == null || bitSet == null) {
                return;
            }
            this.isESS = bitSet.get(0);
            this.isPrivacy = bitSet.get(4);
            for (ScanResult.InformationElement informationElement : informationElementArr) {
                if (informationElement.id == 48) {
                    parseRsnElement(informationElement);
                }
                if (informationElement.id == 221) {
                    if (isWpaOneElement(informationElement)) {
                        parseWpaOneElement(informationElement);
                    }
                    if (isWpsElement(informationElement)) {
                        this.isWPS = true;
                    }
                }
            }
        }

        private String protocolToString(int i) {
            switch (i) {
                case 0:
                    return "None";
                case 1:
                    return "WPA";
                case 2:
                    return "WPA2";
                default:
                    return "?";
            }
        }

        private String keyManagementToString(int i) {
            switch (i) {
                case 0:
                    return "None";
                case 1:
                    return "PSK";
                case 2:
                    return "EAP";
                case 3:
                    return "FT/PSK";
                case 4:
                    return "FT/EAP";
                case 5:
                    return "PSK-SHA256";
                case 6:
                    return "EAP-SHA256";
                default:
                    return "?";
            }
        }

        private String cipherToString(int i) {
            if (i == 0) {
                return "None";
            }
            switch (i) {
                case 2:
                    return "TKIP";
                case 3:
                    return "CCMP";
                default:
                    return "?";
            }
        }

        public String generateCapabilitiesString() {
            String str = this.protocol.isEmpty() && this.isPrivacy ? "[WEP]" : "";
            for (int i = 0; i < this.protocol.size(); i++) {
                String str2 = str + "[" + protocolToString(this.protocol.get(i).intValue());
                if (i < this.keyManagement.size()) {
                    String string = str2;
                    int i2 = 0;
                    while (i2 < this.keyManagement.get(i).size()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(string);
                        sb.append(i2 == 0 ? "-" : "+");
                        sb.append(keyManagementToString(this.keyManagement.get(i).get(i2).intValue()));
                        string = sb.toString();
                        i2++;
                    }
                    str2 = string;
                }
                if (i < this.pairwiseCipher.size()) {
                    String string2 = str2;
                    int i3 = 0;
                    while (i3 < this.pairwiseCipher.get(i).size()) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(string2);
                        sb2.append(i3 == 0 ? "-" : "+");
                        sb2.append(cipherToString(this.pairwiseCipher.get(i).get(i3).intValue()));
                        string2 = sb2.toString();
                        i3++;
                    }
                    str2 = string2;
                }
                str = str2 + "]";
            }
            if (this.isESS) {
                str = str + "[ESS]";
            }
            if (this.isWPS) {
                return str + "[WPS]";
            }
            return str;
        }
    }

    public static class TrafficIndicationMap {
        private static final int MAX_TIM_LENGTH = 254;
        private boolean mValid = false;
        public int mLength = 0;
        public int mDtimCount = -1;
        public int mDtimPeriod = -1;
        public int mBitmapControl = 0;

        public boolean isValid() {
            return this.mValid;
        }

        public void from(ScanResult.InformationElement informationElement) {
            this.mValid = false;
            if (informationElement == null || informationElement.bytes == null) {
                return;
            }
            this.mLength = informationElement.bytes.length;
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                this.mDtimCount = byteBufferOrder.get() & 255;
                this.mDtimPeriod = byteBufferOrder.get() & 255;
                this.mBitmapControl = byteBufferOrder.get() & 255;
                byteBufferOrder.get();
                if (this.mLength <= MAX_TIM_LENGTH && this.mDtimPeriod > 0) {
                    this.mValid = true;
                }
            } catch (BufferUnderflowException e) {
            }
        }
    }

    public static class WifiMode {
        public static final int MODE_11A = 1;
        public static final int MODE_11AC = 5;
        public static final int MODE_11B = 2;
        public static final int MODE_11G = 3;
        public static final int MODE_11N = 4;
        public static final int MODE_UNDEFINED = 0;

        public static int determineMode(int i, int i2, boolean z, boolean z2, boolean z3) {
            if (z) {
                return 5;
            }
            if (z2) {
                return 4;
            }
            if (z3) {
                return 3;
            }
            if (i < 3000) {
                if (i2 >= 24000000) {
                    return 3;
                }
                return 2;
            }
            return 1;
        }

        public static String toString(int i) {
            switch (i) {
                case 1:
                    return "MODE_11A";
                case 2:
                    return "MODE_11B";
                case 3:
                    return "MODE_11G";
                case 4:
                    return "MODE_11N";
                case 5:
                    return "MODE_11AC";
                default:
                    return "MODE_UNDEFINED";
            }
        }
    }

    public static class SupportedRates {
        public static final int MASK = 127;
        public boolean mValid = false;
        public ArrayList<Integer> mRates = new ArrayList<>();

        public boolean isValid() {
            return this.mValid;
        }

        public static int getRateFromByte(int i) {
            switch (i & 127) {
                case 2:
                    return 1000000;
                case 4:
                    return 2000000;
                case 11:
                    return 5500000;
                case 12:
                    return 6000000;
                case 18:
                    return 9000000;
                case 22:
                    return 11000000;
                case 24:
                    return 12000000;
                case ISupplicantStaIfaceCallback.ReasonCode.STA_LEAVING:
                    return 18000000;
                case ISupplicantStaIfaceCallback.StatusCode.UNSUPPORTED_RSN_IE_VERSION:
                    return 22000000;
                case 48:
                    return 24000000;
                case ISupplicantStaIfaceCallback.ReasonCode.MESH_CHANNEL_SWITCH_UNSPECIFIED:
                    return 33000000;
                case ISupplicantStaIfaceCallback.StatusCode.INVALID_RSNIE:
                    return 36000000;
                case ISupplicantStaIfaceCallback.StatusCode.REJECT_DSE_BAND:
                    return 48000000;
                case 108:
                    return 54000000;
                default:
                    return -1;
            }
        }

        public void from(ScanResult.InformationElement informationElement) {
            this.mValid = false;
            if (informationElement == null || informationElement.bytes == null || informationElement.bytes.length > 8 || informationElement.bytes.length < 1) {
                return;
            }
            ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < informationElement.bytes.length; i++) {
                try {
                    int rateFromByte = getRateFromByte(byteBufferOrder.get());
                    if (rateFromByte > 0) {
                        this.mRates.add(Integer.valueOf(rateFromByte));
                    } else {
                        return;
                    }
                } catch (BufferUnderflowException e) {
                    return;
                }
            }
            this.mValid = true;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Iterator<Integer> it = this.mRates.iterator();
            while (it.hasNext()) {
                sb.append(String.format("%.1f", Double.valueOf(((double) it.next().intValue()) / 1000000.0d)) + ", ");
            }
            return sb.toString();
        }
    }
}
