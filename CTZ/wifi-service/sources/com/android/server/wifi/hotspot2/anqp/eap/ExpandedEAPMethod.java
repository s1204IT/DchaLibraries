package com.android.server.wifi.hotspot2.anqp.eap;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ExpandedEAPMethod extends AuthParam {
    public static final int EXPECTED_LENGTH_VALUE = 7;
    private final int mVendorID;
    private final long mVendorType;

    @VisibleForTesting
    public ExpandedEAPMethod(int i, int i2, long j) {
        super(i);
        this.mVendorID = i2;
        this.mVendorType = j;
    }

    public static ExpandedEAPMethod parse(ByteBuffer byteBuffer, int i, boolean z) throws ProtocolException {
        if (i != 7) {
            throw new ProtocolException("Invalid length value: " + i);
        }
        return new ExpandedEAPMethod(z ? 4 : 1, ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.BIG_ENDIAN, 3)) & 16777215, ByteBufferReader.readInteger(byteBuffer, ByteOrder.BIG_ENDIAN, 4) & (-1));
    }

    public int getVendorID() {
        return this.mVendorID;
    }

    public long getVendorType() {
        return this.mVendorType;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ExpandedEAPMethod)) {
            return false;
        }
        ExpandedEAPMethod expandedEAPMethod = (ExpandedEAPMethod) obj;
        return this.mVendorID == expandedEAPMethod.mVendorID && this.mVendorType == expandedEAPMethod.mVendorType;
    }

    public int hashCode() {
        return (this.mVendorID * 31) + ((int) this.mVendorType);
    }

    public String toString() {
        return "ExpandedEAPMethod{mVendorID=" + this.mVendorID + " mVendorType=" + this.mVendorType + "}";
    }
}
