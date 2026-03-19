package com.android.server.wifi.anqp.eap;

import com.android.server.wifi.anqp.eap.EAP;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class InnerAuthEAP implements AuthParam {
    private final EAP.EAPMethodID mEapMethodID;

    public InnerAuthEAP(int length, ByteBuffer payload) throws ProtocolException {
        if (length != 1) {
            throw new ProtocolException("Bad length: " + length);
        }
        int typeID = payload.get() & 255;
        this.mEapMethodID = EAP.mapEAPMethod(typeID);
    }

    public InnerAuthEAP(EAP.EAPMethodID eapMethodID) {
        this.mEapMethodID = eapMethodID;
    }

    @Override
    public EAP.AuthInfoID getAuthInfoID() {
        return EAP.AuthInfoID.InnerAuthEAPMethodType;
    }

    public EAP.EAPMethodID getEAPMethodID() {
        return this.mEapMethodID;
    }

    public int hashCode() {
        if (this.mEapMethodID != null) {
            return this.mEapMethodID.hashCode();
        }
        return 0;
    }

    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }
        return thatObject != null && thatObject.getClass() == InnerAuthEAP.class && ((InnerAuthEAP) thatObject).getEAPMethodID() == getEAPMethodID();
    }

    public String toString() {
        return "Auth method InnerAuthEAP, inner = " + this.mEapMethodID + '\n';
    }
}
