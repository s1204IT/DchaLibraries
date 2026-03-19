package com.android.server.wifi.util;

import android.net.wifi.ScanResult;
import android.util.Log;
import com.android.server.wifi.anqp.CivicLocationElement;
import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.anqp.VenueNameElement;
import com.android.server.wifi.anqp.eap.EAP;
import com.android.server.wifi.hotspot2.NetworkDetail;
import java.net.ProtocolException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;

public class InformationElementUtil {
    public static ScanResult.InformationElement[] parseInformationElements(byte[] bytes) {
        if (bytes == null) {
            return new ScanResult.InformationElement[0];
        }
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        ArrayList<ScanResult.InformationElement> infoElements = new ArrayList<>();
        boolean found_ssid = false;
        while (data.remaining() > 1) {
            int eid = data.get() & 255;
            int elementLength = data.get() & 255;
            if (elementLength > data.remaining() || (eid == 0 && found_ssid)) {
                break;
            }
            if (eid == 0) {
                found_ssid = true;
            }
            ScanResult.InformationElement ie = new ScanResult.InformationElement();
            ie.id = eid;
            ie.bytes = new byte[elementLength];
            data.get(ie.bytes);
            infoElements.add(ie);
        }
        return (ScanResult.InformationElement[]) infoElements.toArray(new ScanResult.InformationElement[infoElements.size()]);
    }

    public static class BssLoad {
        public int stationCount = 0;
        public int channelUtilization = 0;
        public int capacity = 0;

        public void from(ScanResult.InformationElement ie) {
            if (ie.id != 11) {
                throw new IllegalArgumentException("Element id is not BSS_LOAD, : " + ie.id);
            }
            if (ie.bytes.length != 5) {
                throw new IllegalArgumentException("BSS Load element length is not 5: " + ie.bytes.length);
            }
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            this.stationCount = data.getShort() & 65535;
            this.channelUtilization = data.get() & 255;
            this.capacity = data.getShort() & 65535;
        }
    }

    public static class HtOperation {
        public int secondChannelOffset = 0;

        public int getChannelWidth() {
            return this.secondChannelOffset != 0 ? 1 : 0;
        }

        public int getCenterFreq0(int primaryFrequency) {
            if (this.secondChannelOffset == 0) {
                return 0;
            }
            if (this.secondChannelOffset == 1) {
                return primaryFrequency + 10;
            }
            if (this.secondChannelOffset == 3) {
                return primaryFrequency - 10;
            }
            Log.e("HtOperation", "Error on secondChannelOffset: " + this.secondChannelOffset);
            return 0;
        }

        public void from(ScanResult.InformationElement ie) {
            if (ie.id != 61) {
                throw new IllegalArgumentException("Element id is not HT_OPERATION, : " + ie.id);
            }
            this.secondChannelOffset = ie.bytes[1] & 3;
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

        public void from(ScanResult.InformationElement ie) {
            if (ie.id != 192) {
                throw new IllegalArgumentException("Element id is not VHT_OPERATION, : " + ie.id);
            }
            this.channelMode = ie.bytes[0] & 255;
            this.centerFreqIndex1 = ie.bytes[1] & 255;
            this.centerFreqIndex2 = ie.bytes[2] & 255;
        }
    }

    public static class Interworking {
        public NetworkDetail.Ant ant = null;
        public boolean internet = false;
        public VenueNameElement.VenueGroup venueGroup = null;
        public VenueNameElement.VenueType venueType = null;
        public long hessid = 0;

        public void from(ScanResult.InformationElement ie) {
            if (ie.id != 107) {
                throw new IllegalArgumentException("Element id is not INTERWORKING, : " + ie.id);
            }
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            int anOptions = data.get() & 255;
            this.ant = NetworkDetail.Ant.valuesCustom()[anOptions & 15];
            this.internet = (anOptions & 16) != 0;
            if (ie.bytes.length == 3 || ie.bytes.length == 9) {
                try {
                    ByteBuffer vinfo = data.duplicate();
                    vinfo.limit(vinfo.position() + 2);
                    VenueNameElement vne = new VenueNameElement(Constants.ANQPElementType.ANQPVenueName, vinfo);
                    this.venueGroup = vne.getGroup();
                    this.venueType = vne.getType();
                } catch (ProtocolException e) {
                }
            } else if (ie.bytes.length != 1 && ie.bytes.length != 7) {
                throw new IllegalArgumentException("Bad Interworking element length: " + ie.bytes.length);
            }
            if (ie.bytes.length != 7 && ie.bytes.length != 9) {
                return;
            }
            this.hessid = Constants.getInteger(data, ByteOrder.BIG_ENDIAN, 6);
        }
    }

    public static class RoamingConsortium {
        public int anqpOICount = 0;
        public long[] roamingConsortiums = null;

        public void from(ScanResult.InformationElement ie) {
            if (ie.id != 111) {
                throw new IllegalArgumentException("Element id is not ROAMING_CONSORTIUM, : " + ie.id);
            }
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            this.anqpOICount = data.get() & 255;
            int oi12Length = data.get() & 255;
            int oi1Length = oi12Length & 15;
            int oi2Length = (oi12Length >>> 4) & 15;
            int oi3Length = ((ie.bytes.length - 2) - oi1Length) - oi2Length;
            int oiCount = 0;
            if (oi1Length > 0) {
                oiCount = 1;
                if (oi2Length > 0) {
                    oiCount = 1 + 1;
                    if (oi3Length > 0) {
                        oiCount++;
                    }
                }
            }
            this.roamingConsortiums = new long[oiCount];
            if (oi1Length > 0 && this.roamingConsortiums.length > 0) {
                this.roamingConsortiums[0] = Constants.getInteger(data, ByteOrder.BIG_ENDIAN, oi1Length);
            }
            if (oi2Length > 0 && this.roamingConsortiums.length > 1) {
                this.roamingConsortiums[1] = Constants.getInteger(data, ByteOrder.BIG_ENDIAN, oi2Length);
            }
            if (oi3Length <= 0 || this.roamingConsortiums.length <= 2) {
                return;
            }
            this.roamingConsortiums[2] = Constants.getInteger(data, ByteOrder.BIG_ENDIAN, oi3Length);
        }
    }

    public static class Vsa {
        private static final int ANQP_DOMID_BIT = 4;
        public NetworkDetail.HSRelease hsRelease = null;
        public int anqpDomainID = 0;

        public void from(ScanResult.InformationElement ie) {
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            if (ie.bytes.length < 5 || data.getInt() != 278556496) {
                return;
            }
            int hsConf = data.get() & 255;
            switch ((hsConf >> 4) & 15) {
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
            if ((hsConf & 4) == 0) {
                return;
            }
            if (ie.bytes.length < 7) {
                throw new IllegalArgumentException("HS20 indication element too short: " + ie.bytes.length);
            }
            this.anqpDomainID = data.getShort() & 65535;
        }
    }

    public static class ExtendedCapabilities {
        private static final int RTT_RESP_ENABLE_BIT = 70;
        private static final long SSID_UTF8_BIT = 281474976710656L;
        public Long extendedCapabilities;
        public boolean is80211McRTTResponder;

        public ExtendedCapabilities() {
            this.extendedCapabilities = null;
            this.is80211McRTTResponder = false;
        }

        public ExtendedCapabilities(ExtendedCapabilities other) {
            this.extendedCapabilities = null;
            this.is80211McRTTResponder = false;
            this.extendedCapabilities = other.extendedCapabilities;
            this.is80211McRTTResponder = other.is80211McRTTResponder;
        }

        public boolean isStrictUtf8() {
            return (this.extendedCapabilities == null || (this.extendedCapabilities.longValue() & SSID_UTF8_BIT) == 0) ? false : true;
        }

        public void from(ScanResult.InformationElement ie) {
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            this.extendedCapabilities = Long.valueOf(Constants.getInteger(data, ByteOrder.LITTLE_ENDIAN, ie.bytes.length));
            if (ie.bytes.length >= 9) {
                this.is80211McRTTResponder = (ie.bytes[8] & 64) != 0;
            } else {
                this.is80211McRTTResponder = false;
            }
        }
    }

    public static class Capabilities {
        private static final int CAP_ESS_BIT_OFFSET = 0;
        private static final int CAP_PRIVACY_BIT_OFFSET = 4;
        private static final short RSNE_VERSION = 1;
        private static final int WAPI_AUTH_KEY_MGMT_PSK = 41030656;
        private static final int WAPI_AUTH_KEY_MGMT_WAI = 24253440;
        private static final int WAPI_VERSION = 1;
        private static final int WPA2_AKM_EAP = 28053248;
        private static final int WPA2_AKM_EAP_SHA256 = 95162112;
        private static final int WPA2_AKM_FT_EAP = 61607680;
        private static final int WPA2_AKM_FT_PSK = 78384896;
        private static final int WPA2_AKM_PSK = 44830464;
        private static final int WPA2_AKM_PSK_SHA256 = 111939328;
        private static final int WPA_AKM_EAP = 32657408;
        private static final int WPA_AKM_PSK = 49434624;
        private static final int WPA_VENDOR_OUI_TYPE_ONE = 32657408;
        private static final short WPA_VENDOR_OUI_VERSION = 1;

        private static String parseRsnElement(ScanResult.InformationElement ie) {
            ByteBuffer buf = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                if (buf.getShort() != 1) {
                    return null;
                }
                buf.getInt();
                short cipherCount = buf.getShort();
                for (int i = 0; i < cipherCount; i++) {
                    buf.getInt();
                }
                short akmCount = buf.getShort();
                String security = akmCount == 0 ? "[WPA2-EAP" : "[WPA2";
                boolean found = false;
                for (int i2 = 0; i2 < akmCount; i2++) {
                    int akm = buf.getInt();
                    switch (akm) {
                        case WPA2_AKM_EAP:
                            security = security + (found ? "+" : "-") + "EAP";
                            found = true;
                            break;
                        case WPA2_AKM_PSK:
                            security = security + (found ? "+" : "-") + "PSK";
                            found = true;
                            break;
                        case WPA2_AKM_FT_EAP:
                            security = security + (found ? "+" : "-") + "FT/EAP";
                            found = true;
                            break;
                        case WPA2_AKM_FT_PSK:
                            security = security + (found ? "+" : "-") + "FT/PSK";
                            found = true;
                            break;
                        case WPA2_AKM_EAP_SHA256:
                            security = security + (found ? "+" : "-") + "EAP-SHA256";
                            found = true;
                            break;
                        case WPA2_AKM_PSK_SHA256:
                            security = security + (found ? "+" : "-") + "PSK-SHA256";
                            found = true;
                            break;
                    }
                }
                return security + "]";
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse RSNE, buffer underflow");
                return null;
            }
        }

        private static boolean isWpaOneElement(ScanResult.InformationElement ie) {
            ByteBuffer buf = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                return buf.getInt() == 32657408;
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse VSA IE, buffer underflow");
                return false;
            }
        }

        private static String parseWpaOneElement(ScanResult.InformationElement ie) {
            ByteBuffer buf = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                buf.getInt();
                if (buf.getShort() != 1) {
                    return null;
                }
                buf.getInt();
                short cipherCount = buf.getShort();
                for (int i = 0; i < cipherCount; i++) {
                    buf.getInt();
                }
                short akmCount = buf.getShort();
                String security = akmCount == 0 ? "[WPA-EAP" : "[WPA";
                boolean found = false;
                for (int i2 = 0; i2 < akmCount; i2++) {
                    int akm = buf.getInt();
                    switch (akm) {
                        case 32657408:
                            security = security + (found ? "+" : "-") + "EAP";
                            found = true;
                            break;
                        case WPA_AKM_PSK:
                            security = security + (found ? "+" : "-") + "PSK";
                            found = true;
                            break;
                    }
                }
                return security + "]";
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse type 1 WPA, buffer underflow");
                return null;
            }
        }

        private static String parseWapiElement(ScanResult.InformationElement ie) {
            ByteBuffer buf = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            Log.d("InformationElementUtil.WAPI", "parseWapiElement start");
            try {
                if (buf.getShort() != 1) {
                    Log.e("InformationElementUtil.WAPI", "incorrect WAPI version");
                    return null;
                }
                int count = buf.getShort();
                if (count != 1) {
                    Log.e("InformationElementUtil.WAPI", "WAPI IE invalid AKM count: " + count);
                }
                String security = "[WAPI";
                int keyMgmt = buf.getInt();
                if (keyMgmt == WAPI_AUTH_KEY_MGMT_WAI) {
                    security = "[WAPI-CERT";
                } else if (keyMgmt == WAPI_AUTH_KEY_MGMT_PSK) {
                    security = "[WAPI-PSK";
                }
                return security + "]";
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse WAPI element, buffer underflow");
                return null;
            }
        }

        public static String buildCapabilities(ScanResult.InformationElement[] ies, BitSet beaconCap) {
            String capabilities = "";
            boolean rsneFound = false;
            boolean wpaFound = false;
            boolean wapiFound = false;
            if (ies == null || beaconCap == null) {
                return "";
            }
            boolean ess = beaconCap.get(0);
            boolean privacy = beaconCap.get(4);
            for (ScanResult.InformationElement ie : ies) {
                if (ie.id == 48) {
                    rsneFound = true;
                    capabilities = capabilities + parseRsnElement(ie);
                }
                if (ie.id == 221 && isWpaOneElement(ie)) {
                    wpaFound = true;
                    capabilities = capabilities + parseWpaOneElement(ie);
                }
                if (ie.id == 68) {
                    wapiFound = true;
                    capabilities = capabilities + parseWapiElement(ie);
                }
            }
            if (!rsneFound && !wpaFound && privacy && !wapiFound) {
                capabilities = capabilities + "[WEP]";
            }
            if (ess) {
                return capabilities + "[ESS]";
            }
            return capabilities;
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

        public void from(ScanResult.InformationElement ie) {
            this.mValid = false;
            if (ie == null || ie.bytes == null) {
                return;
            }
            this.mLength = ie.bytes.length;
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                this.mDtimCount = data.get() & 255;
                this.mDtimPeriod = data.get() & 255;
                this.mBitmapControl = data.get() & 255;
                data.get();
                if (this.mLength > MAX_TIM_LENGTH) {
                    return;
                }
                this.mValid = true;
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

        public static int determineMode(int frequency, int maxRate, boolean foundVht, boolean foundHt, boolean foundErp) {
            if (foundVht) {
                return 5;
            }
            if (foundHt) {
                return 4;
            }
            if (foundErp) {
                return 3;
            }
            if (frequency < 3000) {
                return maxRate < 24000000 ? 2 : 3;
            }
            return 1;
        }

        public static String toString(int mode) {
            switch (mode) {
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

        public static int getRateFromByte(int byteVal) {
            switch (byteVal & MASK) {
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
                case CivicLocationElement.ADDITIONAL_LOCATION:
                    return 11000000;
                case 24:
                    return 12000000;
                case CivicLocationElement.BRANCH_ROAD:
                    return 18000000;
                case EAP.EAP_ZLXEAP:
                    return 22000000;
                case EAP.EAP_SAKE:
                    return 24000000;
                case 66:
                    return 33000000;
                case 72:
                    return 36000000;
                case 96:
                    return 48000000;
                case 108:
                    return 54000000;
                default:
                    return -1;
            }
        }

        public void from(ScanResult.InformationElement ie) {
            this.mValid = false;
            if (ie == null || ie.bytes == null || ie.bytes.length > 8 || ie.bytes.length < 1) {
                return;
            }
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < ie.bytes.length; i++) {
                try {
                    int rate = getRateFromByte(data.get());
                    if (rate > 0) {
                        this.mRates.add(Integer.valueOf(rate));
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
            StringBuilder sbuf = new StringBuilder();
            for (Integer rate : this.mRates) {
                sbuf.append(String.format("%.1f", Double.valueOf(((double) rate.intValue()) / 1000000.0d))).append(", ");
            }
            return sbuf.toString();
        }
    }
}
