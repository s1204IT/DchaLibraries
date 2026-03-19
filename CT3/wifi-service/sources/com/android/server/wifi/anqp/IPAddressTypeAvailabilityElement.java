package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class IPAddressTypeAvailabilityElement extends ANQPElement {
    private final IPv4Availability mV4Availability;
    private final IPv6Availability mV6Availability;

    public enum IPv4Availability {
        NotAvailable,
        Public,
        PortRestricted,
        SingleNAT,
        DoubleNAT,
        PortRestrictedAndSingleNAT,
        PortRestrictedAndDoubleNAT,
        Unknown;

        public static IPv4Availability[] valuesCustom() {
            return values();
        }
    }

    public enum IPv6Availability {
        NotAvailable,
        Available,
        Unknown,
        Reserved;

        public static IPv6Availability[] valuesCustom() {
            return values();
        }
    }

    public IPAddressTypeAvailabilityElement(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        IPv4Availability iPv4Availability;
        super(infoID);
        if (payload.remaining() != 1) {
            throw new ProtocolException("Bad IP Address Type Availability length: " + payload.remaining());
        }
        int ipField = payload.get();
        this.mV6Availability = IPv6Availability.valuesCustom()[ipField & 3];
        int ipField2 = (ipField >> 2) & 63;
        if (ipField2 < IPv4Availability.valuesCustom().length) {
            iPv4Availability = IPv4Availability.valuesCustom()[ipField2];
        } else {
            iPv4Availability = IPv4Availability.Unknown;
        }
        this.mV4Availability = iPv4Availability;
    }

    public IPv4Availability getV4Availability() {
        return this.mV4Availability;
    }

    public IPv6Availability getV6Availability() {
        return this.mV6Availability;
    }

    public String toString() {
        return "IPAddressTypeAvailability{mV4Availability=" + this.mV4Availability + ", mV6Availability=" + this.mV6Availability + '}';
    }
}
