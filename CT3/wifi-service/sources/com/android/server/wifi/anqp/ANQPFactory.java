package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.google.protobuf.nano.Extension;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class ANQPFactory {

    private static final int[] f5xb54e5a3d = null;
    private static final List<Constants.ANQPElementType> BaseANQPSet1 = Arrays.asList(Constants.ANQPElementType.ANQPVenueName, Constants.ANQPElementType.ANQPNwkAuthType, Constants.ANQPElementType.ANQPRoamingConsortium, Constants.ANQPElementType.ANQPIPAddrAvailability, Constants.ANQPElementType.ANQPNAIRealm, Constants.ANQPElementType.ANQP3GPPNetwork, Constants.ANQPElementType.ANQPDomName);
    private static final List<Constants.ANQPElementType> BaseANQPSet2 = Arrays.asList(Constants.ANQPElementType.ANQPVenueName, Constants.ANQPElementType.ANQPNwkAuthType, Constants.ANQPElementType.ANQPIPAddrAvailability, Constants.ANQPElementType.ANQPNAIRealm, Constants.ANQPElementType.ANQP3GPPNetwork, Constants.ANQPElementType.ANQPDomName);
    private static final List<Constants.ANQPElementType> HS20ANQPSet = Arrays.asList(Constants.ANQPElementType.HSFriendlyName, Constants.ANQPElementType.HSWANMetrics, Constants.ANQPElementType.HSConnCapability);
    private static final List<Constants.ANQPElementType> HS20ANQPSetwOSU = Arrays.asList(Constants.ANQPElementType.HSFriendlyName, Constants.ANQPElementType.HSWANMetrics, Constants.ANQPElementType.HSConnCapability, Constants.ANQPElementType.HSOSUProviders);

    private static int[] m406x612a3019() {
        if (f5xb54e5a3d != null) {
            return f5xb54e5a3d;
        }
        int[] iArr = new int[Constants.ANQPElementType.valuesCustom().length];
        try {
            iArr[Constants.ANQPElementType.ANQP3GPPNetwork.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPCapabilityList.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPCivicLoc.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPDomName.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPEmergencyAlert.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPEmergencyNAI.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPEmergencyNumber.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPGeoLoc.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPIPAddrAvailability.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPLocURI.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPNAIRealm.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPNeighborReport.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPNwkAuthType.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPQueryList.ordinal()] = 25;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPRoamingConsortium.ordinal()] = 14;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPTDLSCap.ordinal()] = 15;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPVendorSpec.ordinal()] = 16;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[Constants.ANQPElementType.ANQPVenueName.ordinal()] = 17;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[Constants.ANQPElementType.HSCapabilityList.ordinal()] = 18;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[Constants.ANQPElementType.HSConnCapability.ordinal()] = 19;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[Constants.ANQPElementType.HSFriendlyName.ordinal()] = 20;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[Constants.ANQPElementType.HSIconFile.ordinal()] = 21;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[Constants.ANQPElementType.HSIconRequest.ordinal()] = 26;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[Constants.ANQPElementType.HSNAIHomeRealmQuery.ordinal()] = 27;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[Constants.ANQPElementType.HSOSUProviders.ordinal()] = 22;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[Constants.ANQPElementType.HSOperatingclass.ordinal()] = 23;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[Constants.ANQPElementType.HSQueryList.ordinal()] = 28;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[Constants.ANQPElementType.HSWANMetrics.ordinal()] = 24;
        } catch (NoSuchFieldError e28) {
        }
        f5xb54e5a3d = iArr;
        return iArr;
    }

    public static List<Constants.ANQPElementType> getBaseANQPSet(boolean includeRC) {
        return includeRC ? BaseANQPSet1 : BaseANQPSet2;
    }

    public static List<Constants.ANQPElementType> getHS20ANQPSet(boolean includeOSU) {
        return includeOSU ? HS20ANQPSetwOSU : HS20ANQPSet;
    }

    public static List<Constants.ANQPElementType> buildQueryList(NetworkDetail networkDetail, boolean matchSet, boolean osu) {
        List<Constants.ANQPElementType> querySet = new ArrayList<>();
        if (matchSet) {
            querySet.addAll(getBaseANQPSet(networkDetail.getAnqpOICount() > 0));
        }
        if (networkDetail.getHSRelease() != null) {
            boolean includeOSU = osu && networkDetail.getHSRelease() == NetworkDetail.HSRelease.R2;
            if (matchSet) {
                querySet.addAll(getHS20ANQPSet(includeOSU));
            } else if (includeOSU) {
                querySet.add(Constants.ANQPElementType.HSOSUProviders);
            }
        }
        return querySet;
    }

    public static ByteBuffer buildQueryRequest(Set<Constants.ANQPElementType> elements, ByteBuffer target) {
        List<Constants.ANQPElementType> list = new ArrayList<>(elements);
        Collections.sort(list);
        ListIterator<Constants.ANQPElementType> elementIterator = list.listIterator();
        target.order(ByteOrder.LITTLE_ENDIAN);
        target.putShort((short) 256);
        int lenPos = target.position();
        target.putShort((short) 0);
        while (true) {
            if (!elementIterator.hasNext()) {
                break;
            }
            Integer id = Constants.getANQPElementID(elementIterator.next());
            if (id != null) {
                target.putShort(id.shortValue());
            } else {
                elementIterator.previous();
                break;
            }
        }
        target.putShort(lenPos, (short) ((target.position() - lenPos) - 2));
        if (elementIterator.hasNext()) {
            target.putShort((short) -8739);
            int vsLenPos = target.position();
            target.putShort((short) 0);
            target.putInt(Constants.HS20_PREFIX);
            target.put((byte) 1);
            target.put((byte) 0);
            while (elementIterator.hasNext()) {
                Constants.ANQPElementType elementType = elementIterator.next();
                Integer id2 = Constants.getHS20ElementID(elementType);
                if (id2 == null) {
                    throw new RuntimeException("Unmapped ANQPElementType: " + elementType);
                }
                target.put(id2.byteValue());
            }
            target.putShort(vsLenPos, (short) ((target.position() - vsLenPos) - 2));
        }
        target.flip();
        return target;
    }

    public static ByteBuffer buildHomeRealmRequest(List<String> realmNames, ByteBuffer target) {
        target.order(ByteOrder.LITTLE_ENDIAN);
        target.putShort((short) -8739);
        int lenPos = target.position();
        target.putShort((short) 0);
        target.putInt(Constants.HS20_PREFIX);
        target.put((byte) 6);
        target.put((byte) 0);
        target.put((byte) realmNames.size());
        for (String realmName : realmNames) {
            target.put((byte) 1);
            byte[] octets = realmName.getBytes(StandardCharsets.UTF_8);
            target.put((byte) octets.length);
            target.put(octets);
        }
        target.putShort(lenPos, (short) ((target.position() - lenPos) - 2));
        target.flip();
        return target;
    }

    public static ByteBuffer buildIconRequest(String fileName, ByteBuffer target) {
        target.order(ByteOrder.LITTLE_ENDIAN);
        target.putShort((short) -8739);
        int lenPos = target.position();
        target.putShort((short) 0);
        target.putInt(Constants.HS20_PREFIX);
        target.put((byte) 10);
        target.put((byte) 0);
        target.put(fileName.getBytes(StandardCharsets.UTF_8));
        target.putShort(lenPos, (short) ((target.position() - lenPos) - 2));
        target.flip();
        return target;
    }

    public static List<ANQPElement> parsePayload(ByteBuffer payload) throws ProtocolException {
        payload.order(ByteOrder.LITTLE_ENDIAN);
        List<ANQPElement> elements = new ArrayList<>();
        while (payload.hasRemaining()) {
            elements.add(buildElement(payload));
        }
        return elements;
    }

    private static ANQPElement buildElement(ByteBuffer payload) throws ProtocolException {
        if (payload.remaining() < 4) {
            throw new ProtocolException("Runt payload: " + payload.remaining());
        }
        int infoIDNumber = payload.getShort() & 65535;
        Constants.ANQPElementType infoID = Constants.mapANQPElement(infoIDNumber);
        if (infoID == null) {
            throw new ProtocolException("Bad info ID: " + infoIDNumber);
        }
        int length = payload.getShort() & 65535;
        if (payload.remaining() < length) {
            throw new ProtocolException("Truncated payload: " + payload.remaining() + " vs " + length);
        }
        return buildElement(payload, infoID, length);
    }

    public static ANQPElement buildElement(ByteBuffer payload, Constants.ANQPElementType infoID, int length) throws ProtocolException {
        try {
            ByteBuffer elementPayload = payload.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            payload.position(payload.position() + length);
            elementPayload.limit(elementPayload.position() + length);
            switch (m406x612a3019()[infoID.ordinal()]) {
                case 1:
                    return new ThreeGPPNetworkElement(infoID, elementPayload);
                case 2:
                    return new CapabilityListElement(infoID, elementPayload);
                case 3:
                    return new CivicLocationElement(infoID, elementPayload);
                case 4:
                    return new DomainNameElement(infoID, elementPayload);
                case 5:
                    return new GenericStringElement(infoID, elementPayload);
                case 6:
                    return new GenericStringElement(infoID, elementPayload);
                case 7:
                    return new EmergencyNumberElement(infoID, elementPayload);
                case 8:
                    return new GEOLocationElement(infoID, elementPayload);
                case 9:
                    return new IPAddressTypeAvailabilityElement(infoID, elementPayload);
                case 10:
                    return new GenericStringElement(infoID, elementPayload);
                case 11:
                    return new NAIRealmElement(infoID, elementPayload);
                case 12:
                    return new GenericBlobElement(infoID, elementPayload);
                case 13:
                    return new NetworkAuthenticationTypeElement(infoID, elementPayload);
                case Extension.TYPE_ENUM:
                    return new RoamingConsortiumElement(infoID, elementPayload);
                case 15:
                    return new GenericBlobElement(infoID, elementPayload);
                case 16:
                    if (elementPayload.remaining() > 5) {
                        int oi = elementPayload.getInt();
                        if (oi != 295333712) {
                            return null;
                        }
                        int subType = elementPayload.get() & 255;
                        Constants.ANQPElementType hs20ID = Constants.mapHS20Element(subType);
                        if (hs20ID == null) {
                            throw new ProtocolException("Bad HS20 info ID: " + subType);
                        }
                        elementPayload.get();
                        return buildHS20Element(hs20ID, elementPayload);
                    }
                    return new GenericBlobElement(infoID, elementPayload);
                case 17:
                    return new VenueNameElement(infoID, elementPayload);
                default:
                    throw new ProtocolException("Unknown element ID: " + infoID);
            }
        } catch (ProtocolException e) {
            throw e;
        } catch (Exception e2) {
            throw new ProtocolException("Unknown parsing error", e2);
        }
    }

    public static ANQPElement buildHS20Element(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        try {
            switch (m406x612a3019()[infoID.ordinal()]) {
                case 18:
                    return new HSCapabilityListElement(infoID, payload);
                case CivicLocationElement.HOUSE_NUMBER:
                    return new HSConnectionCapabilityElement(infoID, payload);
                case CivicLocationElement.HOUSE_NUMBER_SUFFIX:
                    return new HSFriendlyNameElement(infoID, payload);
                case 21:
                    return new HSIconFileElement(infoID, payload);
                case CivicLocationElement.ADDITIONAL_LOCATION:
                    return new RawByteElement(infoID, payload);
                case 23:
                    return new GenericBlobElement(infoID, payload);
                case 24:
                    return new HSWanMetricsElement(infoID, payload);
                default:
                    return null;
            }
        } catch (ProtocolException e) {
            throw e;
        } catch (Exception e2) {
            throw new ProtocolException("Unknown parsing error", e2);
        }
    }
}
