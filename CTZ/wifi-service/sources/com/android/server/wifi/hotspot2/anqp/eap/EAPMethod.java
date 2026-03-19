package com.android.server.wifi.hotspot2.anqp.eap;

import com.android.internal.annotations.VisibleForTesting;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EAPMethod {
    private final Map<Integer, Set<AuthParam>> mAuthParams;
    private final int mEAPMethodID;

    @VisibleForTesting
    public EAPMethod(int i, Map<Integer, Set<AuthParam>> map) {
        this.mEAPMethodID = i;
        this.mAuthParams = map;
    }

    public static EAPMethod parse(ByteBuffer byteBuffer) throws ProtocolException {
        int i = byteBuffer.get() & 255;
        if (i > byteBuffer.remaining()) {
            throw new ProtocolException("Invalid data length: " + i);
        }
        int i2 = byteBuffer.get() & 255;
        HashMap map = new HashMap();
        for (int i3 = byteBuffer.get() & 255; i3 > 0; i3--) {
            addAuthParam(map, parseAuthParam(byteBuffer));
        }
        return new EAPMethod(i2, map);
    }

    private static AuthParam parseAuthParam(ByteBuffer byteBuffer) throws ProtocolException {
        int i = byteBuffer.get() & 255;
        int i2 = byteBuffer.get() & 255;
        if (i != 221) {
            switch (i) {
                case 1:
                    return ExpandedEAPMethod.parse(byteBuffer, i2, false);
                case 2:
                    return NonEAPInnerAuth.parse(byteBuffer, i2);
                case 3:
                    return InnerAuthEAP.parse(byteBuffer, i2);
                case 4:
                    return ExpandedEAPMethod.parse(byteBuffer, i2, true);
                case 5:
                    return CredentialType.parse(byteBuffer, i2, false);
                case 6:
                    return CredentialType.parse(byteBuffer, i2, true);
                default:
                    throw new ProtocolException("Unknow Auth Type ID: " + i);
            }
        }
        return VendorSpecificAuth.parse(byteBuffer, i2);
    }

    private static void addAuthParam(Map<Integer, Set<AuthParam>> map, AuthParam authParam) {
        Set<AuthParam> hashSet = map.get(Integer.valueOf(authParam.getAuthTypeID()));
        if (hashSet == null) {
            hashSet = new HashSet<>();
            map.put(Integer.valueOf(authParam.getAuthTypeID()), hashSet);
        }
        hashSet.add(authParam);
    }

    public Map<Integer, Set<AuthParam>> getAuthParams() {
        return Collections.unmodifiableMap(this.mAuthParams);
    }

    public int getEAPMethodID() {
        return this.mEAPMethodID;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EAPMethod)) {
            return false;
        }
        EAPMethod eAPMethod = (EAPMethod) obj;
        return this.mEAPMethodID == eAPMethod.mEAPMethodID && this.mAuthParams.equals(eAPMethod.mAuthParams);
    }

    public int hashCode() {
        return (this.mEAPMethodID * 31) + this.mAuthParams.hashCode();
    }

    public String toString() {
        return "EAPMethod{mEAPMethodID=" + this.mEAPMethodID + " mAuthParams=" + this.mAuthParams + "}";
    }
}
