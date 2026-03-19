package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.nio.ByteBuffer;

public class RawByteElement extends ANQPElement {
    private final byte[] mPayload;

    public RawByteElement(Constants.ANQPElementType infoID, ByteBuffer payload) {
        super(infoID);
        this.mPayload = new byte[payload.remaining()];
        payload.get(this.mPayload);
    }

    public byte[] getPayload() {
        return this.mPayload;
    }
}
