package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ANQPParser {

    @VisibleForTesting
    public static final int VENDOR_SPECIFIC_HS20_OI = 5271450;

    @VisibleForTesting
    public static final int VENDOR_SPECIFIC_HS20_TYPE = 17;

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType = new int[Constants.ANQPElementType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.ANQPVenueName.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.ANQPRoamingConsortium.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.ANQPIPAddrAvailability.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.ANQPNAIRealm.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.ANQP3GPPNetwork.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.ANQPDomName.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.ANQPVendorSpec.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.HSFriendlyName.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.HSWANMetrics.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.HSConnCapability.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[Constants.ANQPElementType.HSOSUProviders.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
        }
    }

    public static ANQPElement parseElement(Constants.ANQPElementType aNQPElementType, ByteBuffer byteBuffer) throws ProtocolException {
        switch (AnonymousClass1.$SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[aNQPElementType.ordinal()]) {
            case 1:
                return VenueNameElement.parse(byteBuffer);
            case 2:
                return RoamingConsortiumElement.parse(byteBuffer);
            case 3:
                return IPAddressTypeAvailabilityElement.parse(byteBuffer);
            case 4:
                return NAIRealmElement.parse(byteBuffer);
            case 5:
                return ThreeGPPNetworkElement.parse(byteBuffer);
            case 6:
                return DomainNameElement.parse(byteBuffer);
            case 7:
                return parseVendorSpecificElement(byteBuffer);
            default:
                throw new ProtocolException("Unknown element ID: " + aNQPElementType);
        }
    }

    public static ANQPElement parseHS20Element(Constants.ANQPElementType aNQPElementType, ByteBuffer byteBuffer) throws ProtocolException {
        switch (AnonymousClass1.$SwitchMap$com$android$server$wifi$hotspot2$anqp$Constants$ANQPElementType[aNQPElementType.ordinal()]) {
            case 8:
                return HSFriendlyNameElement.parse(byteBuffer);
            case 9:
                return HSWanMetricsElement.parse(byteBuffer);
            case 10:
                return HSConnectionCapabilityElement.parse(byteBuffer);
            case 11:
                return HSOsuProvidersElement.parse(byteBuffer);
            default:
                throw new ProtocolException("Unknown element ID: " + aNQPElementType);
        }
    }

    private static ANQPElement parseVendorSpecificElement(ByteBuffer byteBuffer) throws ProtocolException {
        int integer = (int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.BIG_ENDIAN, 3);
        int i = byteBuffer.get() & 255;
        if (integer != 5271450 || i != 17) {
            throw new ProtocolException("Unsupported vendor specific OI=" + integer + " type=" + i);
        }
        int i2 = byteBuffer.get() & 255;
        Constants.ANQPElementType aNQPElementTypeMapHS20Element = Constants.mapHS20Element(i2);
        if (aNQPElementTypeMapHS20Element == null) {
            throw new ProtocolException("Unsupported subtype: " + i2);
        }
        byteBuffer.get();
        return parseHS20Element(aNQPElementTypeMapHS20Element, byteBuffer);
    }
}
