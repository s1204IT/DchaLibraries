package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CapabilityListElement extends ANQPElement {
    private final Constants.ANQPElementType[] mCapabilities;

    public CapabilityListElement(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        super(infoID);
        if ((payload.remaining() & 1) == 1) {
            throw new ProtocolException("Odd length");
        }
        this.mCapabilities = new Constants.ANQPElementType[payload.remaining() / 2];
        int index = 0;
        while (payload.hasRemaining()) {
            int capID = payload.getShort() & 65535;
            Constants.ANQPElementType capability = Constants.mapANQPElement(capID);
            if (capability == null) {
                throw new ProtocolException("Unknown capability: " + capID);
            }
            this.mCapabilities[index] = capability;
            index++;
        }
    }

    public Constants.ANQPElementType[] getCapabilities() {
        return this.mCapabilities;
    }

    public String toString() {
        return "CapabilityList{mCapabilities=" + Arrays.toString(this.mCapabilities) + '}';
    }
}
