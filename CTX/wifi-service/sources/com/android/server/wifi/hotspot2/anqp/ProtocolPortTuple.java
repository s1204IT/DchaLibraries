package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ProtocolPortTuple {
    public static final int PROTO_STATUS_CLOSED = 0;
    public static final int PROTO_STATUS_OPEN = 1;
    public static final int PROTO_STATUS_UNKNOWN = 2;

    @VisibleForTesting
    public static final int RAW_BYTE_SIZE = 4;
    private final int mPort;
    private final int mProtocol;
    private final int mStatus;

    @VisibleForTesting
    public ProtocolPortTuple(int i, int i2, int i3) {
        this.mProtocol = i;
        this.mPort = i2;
        this.mStatus = i3;
    }

    public static ProtocolPortTuple parse(ByteBuffer byteBuffer) {
        return new ProtocolPortTuple(byteBuffer.get(), ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK, byteBuffer.get() & 255);
    }

    public int getProtocol() {
        return this.mProtocol;
    }

    public int getPort() {
        return this.mPort;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProtocolPortTuple)) {
            return false;
        }
        ProtocolPortTuple protocolPortTuple = (ProtocolPortTuple) obj;
        return this.mProtocol == protocolPortTuple.mProtocol && this.mPort == protocolPortTuple.mPort && this.mStatus == protocolPortTuple.mStatus;
    }

    public int hashCode() {
        return (((this.mProtocol * 31) + this.mPort) * 31) + this.mStatus;
    }

    public String toString() {
        return "ProtocolTuple{mProtocol=" + this.mProtocol + ", mPort=" + this.mPort + ", mStatus=" + this.mStatus + '}';
    }
}
