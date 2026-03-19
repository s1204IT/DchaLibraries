package com.android.server.wifi.hotspot2.anqp.eap;

import com.android.internal.annotations.VisibleForTesting;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class InnerAuthEAP extends AuthParam {

    @VisibleForTesting
    public static final int EXPECTED_LENGTH_VALUE = 1;
    private final int mEAPMethodID;

    @VisibleForTesting
    public InnerAuthEAP(int i) {
        super(3);
        this.mEAPMethodID = i;
    }

    public static InnerAuthEAP parse(ByteBuffer byteBuffer, int i) throws ProtocolException {
        if (i != 1) {
            throw new ProtocolException("Invalid length: " + i);
        }
        return new InnerAuthEAP(byteBuffer.get() & 255);
    }

    public int getEAPMethodID() {
        return this.mEAPMethodID;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj instanceof InnerAuthEAP) && this.mEAPMethodID == ((InnerAuthEAP) obj).mEAPMethodID;
    }

    public int hashCode() {
        return this.mEAPMethodID;
    }

    public String toString() {
        return "InnerAuthEAP{mEAPMethodID=" + this.mEAPMethodID + "}";
    }
}
