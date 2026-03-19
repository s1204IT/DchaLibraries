package com.android.server.wifi.anqp.eap;

import com.android.server.wifi.anqp.eap.EAP;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VendorSpecificAuth implements AuthParam {
    private final byte[] mData;

    public VendorSpecificAuth(int length, ByteBuffer payload) throws ProtocolException {
        this.mData = new byte[length];
        payload.get(this.mData);
    }

    @Override
    public EAP.AuthInfoID getAuthInfoID() {
        return EAP.AuthInfoID.VendorSpecific;
    }

    public int hashCode() {
        return Arrays.hashCode(this.mData);
    }

    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }
        if (thatObject == null || thatObject.getClass() != VendorSpecificAuth.class) {
            return false;
        }
        return Arrays.equals(((VendorSpecificAuth) thatObject).getData(), getData());
    }

    public byte[] getData() {
        return this.mData;
    }

    public String toString() {
        return "Auth method VendorSpecificAuth, data = " + Arrays.toString(this.mData) + '\n';
    }
}
