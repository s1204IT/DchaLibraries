package com.android.server.wifi.anqp.eap;

import com.android.server.wifi.anqp.eap.EAP;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class Credential implements AuthParam {
    private final EAP.AuthInfoID mAuthInfoID;
    private final CredType mCredType;

    public enum CredType {
        Reserved,
        SIM,
        USIM,
        NFC,
        HWToken,
        Softoken,
        Certificate,
        Username,
        None,
        Anonymous,
        VendorSpecific;

        public static CredType[] valuesCustom() {
            return values();
        }
    }

    public Credential(EAP.AuthInfoID infoID, int length, ByteBuffer payload) throws ProtocolException {
        CredType credType;
        if (length != 1) {
            throw new ProtocolException("Bad length: " + length);
        }
        this.mAuthInfoID = infoID;
        int typeID = payload.get() & 255;
        if (typeID < CredType.valuesCustom().length) {
            credType = CredType.valuesCustom()[typeID];
        } else {
            credType = CredType.Reserved;
        }
        this.mCredType = credType;
    }

    @Override
    public EAP.AuthInfoID getAuthInfoID() {
        return this.mAuthInfoID;
    }

    public int hashCode() {
        return (this.mAuthInfoID.hashCode() * 31) + this.mCredType.hashCode();
    }

    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }
        return thatObject != null && thatObject.getClass() == Credential.class && ((Credential) thatObject).getCredType() == getCredType();
    }

    public CredType getCredType() {
        return this.mCredType;
    }

    public String toString() {
        return "Auth method " + this.mAuthInfoID + " = " + this.mCredType + "\n";
    }
}
