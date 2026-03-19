package com.android.server.wifi.anqp.eap;

import com.android.server.wifi.anqp.eap.EAP;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class NonEAPInnerAuth implements AuthParam {
    private static final Map<NonEAPType, String> sOmaMap = new EnumMap(NonEAPType.class);
    private static final Map<String, NonEAPType> sRevOmaMap = new HashMap();
    private final NonEAPType mType;

    public enum NonEAPType {
        Reserved,
        PAP,
        CHAP,
        MSCHAP,
        MSCHAPv2;

        public static NonEAPType[] valuesCustom() {
            return values();
        }
    }

    static {
        sOmaMap.put(NonEAPType.PAP, "PAP");
        sOmaMap.put(NonEAPType.CHAP, "CHAP");
        sOmaMap.put(NonEAPType.MSCHAP, "MS-CHAP");
        sOmaMap.put(NonEAPType.MSCHAPv2, "MS-CHAP-V2");
        for (Map.Entry<NonEAPType, String> entry : sOmaMap.entrySet()) {
            sRevOmaMap.put(entry.getValue(), entry.getKey());
        }
    }

    public NonEAPInnerAuth(int length, ByteBuffer payload) throws ProtocolException {
        NonEAPType nonEAPType;
        if (length != 1) {
            throw new ProtocolException("Bad length: " + payload.remaining());
        }
        int typeID = payload.get() & 255;
        if (typeID < NonEAPType.valuesCustom().length) {
            nonEAPType = NonEAPType.valuesCustom()[typeID];
        } else {
            nonEAPType = NonEAPType.Reserved;
        }
        this.mType = nonEAPType;
    }

    public NonEAPInnerAuth(NonEAPType type) {
        this.mType = type;
    }

    public NonEAPInnerAuth(String eapType) {
        this.mType = sRevOmaMap.get(eapType);
    }

    @Override
    public EAP.AuthInfoID getAuthInfoID() {
        return EAP.AuthInfoID.NonEAPInnerAuthType;
    }

    public NonEAPType getType() {
        return this.mType;
    }

    public String getOMAtype() {
        return sOmaMap.get(this.mType);
    }

    public static String mapInnerType(NonEAPType type) {
        return sOmaMap.get(type);
    }

    public int hashCode() {
        return this.mType.hashCode();
    }

    public boolean equals(Object thatObject) {
        if (thatObject == this) {
            return true;
        }
        return thatObject != null && thatObject.getClass() == NonEAPInnerAuth.class && ((NonEAPInnerAuth) thatObject).getType() == getType();
    }

    public String toString() {
        return "Auth method NonEAPInnerAuthEAP, inner = " + this.mType + '\n';
    }
}
