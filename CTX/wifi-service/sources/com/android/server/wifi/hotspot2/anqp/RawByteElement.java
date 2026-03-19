package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RawByteElement extends ANQPElement {
    private final byte[] mPayload;

    @VisibleForTesting
    public RawByteElement(Constants.ANQPElementType aNQPElementType, byte[] bArr) {
        super(aNQPElementType);
        this.mPayload = bArr;
    }

    public static RawByteElement parse(Constants.ANQPElementType aNQPElementType, ByteBuffer byteBuffer) {
        byte[] bArr = new byte[byteBuffer.remaining()];
        if (byteBuffer.hasRemaining()) {
            byteBuffer.get(bArr);
        }
        return new RawByteElement(aNQPElementType, bArr);
    }

    public byte[] getPayload() {
        return this.mPayload;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RawByteElement)) {
            return false;
        }
        RawByteElement rawByteElement = (RawByteElement) obj;
        return getID() == rawByteElement.getID() && Arrays.equals(this.mPayload, rawByteElement.mPayload);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mPayload);
    }
}
