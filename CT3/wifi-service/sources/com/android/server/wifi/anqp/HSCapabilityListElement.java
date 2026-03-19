package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HSCapabilityListElement extends ANQPElement {
    private final Constants.ANQPElementType[] mCapabilities;

    public HSCapabilityListElement(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        super(infoID);
        this.mCapabilities = new Constants.ANQPElementType[payload.remaining()];
        int index = 0;
        while (payload.hasRemaining()) {
            int capID = payload.get() & 255;
            Constants.ANQPElementType capability = Constants.mapHS20Element(capID);
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
        return "HSCapabilityList{mCapabilities=" + Arrays.toString(this.mCapabilities) + '}';
    }
}
