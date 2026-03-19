package com.android.server.wifi.anqp.eap;

import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.anqp.eap.EAP;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ExpandedEAPMethod implements AuthParam {
    private final EAP.AuthInfoID mAuthInfoID;
    private final int mVendorID;
    private final long mVendorType;

    public ExpandedEAPMethod(EAP.AuthInfoID authInfoID, int length, ByteBuffer payload) throws ProtocolException {
        if (length != 7) {
            throw new ProtocolException("Bad length: " + payload.remaining());
        }
        this.mAuthInfoID = authInfoID;
        ByteBuffer vndBuffer = payload.duplicate().order(ByteOrder.BIG_ENDIAN);
        int id = vndBuffer.getShort() & 65535;
        this.mVendorID = (id << 8) | (vndBuffer.get() & 255);
        this.mVendorType = ((long) vndBuffer.getInt()) & Constants.INT_MASK;
        payload.position(payload.position() + 7);
    }

    public ExpandedEAPMethod(EAP.AuthInfoID authInfoID, int vendorID, long vendorType) {
        this.mAuthInfoID = authInfoID;
        this.mVendorID = vendorID;
        this.mVendorType = vendorType;
    }

    @Override
    public EAP.AuthInfoID getAuthInfoID() {
        return this.mAuthInfoID;
    }

    public int hashCode() {
        return (((this.mAuthInfoID.hashCode() * 31) + this.mVendorID) * 31) + ((int) this.mVendorType);
    }

    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }
        if (thatObject == null || thatObject.getClass() != ExpandedEAPMethod.class) {
            return false;
        }
        ExpandedEAPMethod that = (ExpandedEAPMethod) thatObject;
        return that.getVendorID() == getVendorID() && that.getVendorType() == getVendorType();
    }

    public int getVendorID() {
        return this.mVendorID;
    }

    public long getVendorType() {
        return this.mVendorType;
    }

    public String toString() {
        return "Auth method " + this.mAuthInfoID + ", id " + this.mVendorID + ", type " + this.mVendorType + "\n";
    }
}
