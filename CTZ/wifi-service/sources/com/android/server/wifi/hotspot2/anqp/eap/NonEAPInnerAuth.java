package com.android.server.wifi.hotspot2.anqp.eap;

import com.android.internal.annotations.VisibleForTesting;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class NonEAPInnerAuth extends AuthParam {
    public static final int AUTH_TYPE_CHAP = 2;
    private static final Map<String, Integer> AUTH_TYPE_MAP = new HashMap();
    public static final int AUTH_TYPE_MSCHAP = 3;
    public static final int AUTH_TYPE_MSCHAPV2 = 4;
    public static final int AUTH_TYPE_PAP = 1;
    public static final int AUTH_TYPE_UNKNOWN = 0;

    @VisibleForTesting
    public static final int EXPECTED_LENGTH_VALUE = 1;
    private final int mAuthType;

    static {
        AUTH_TYPE_MAP.put("PAP", 1);
        AUTH_TYPE_MAP.put("CHAP", 2);
        AUTH_TYPE_MAP.put("MS-CHAP", 3);
        AUTH_TYPE_MAP.put("MS-CHAP-V2", 4);
    }

    public NonEAPInnerAuth(int i) {
        super(2);
        this.mAuthType = i;
    }

    public static NonEAPInnerAuth parse(ByteBuffer byteBuffer, int i) throws ProtocolException {
        if (i != 1) {
            throw new ProtocolException("Invalid length: " + i);
        }
        return new NonEAPInnerAuth(byteBuffer.get() & 255);
    }

    public static int getAuthTypeID(String str) {
        if (AUTH_TYPE_MAP.containsKey(str)) {
            return AUTH_TYPE_MAP.get(str).intValue();
        }
        return 0;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj instanceof NonEAPInnerAuth) && this.mAuthType == ((NonEAPInnerAuth) obj).mAuthType;
    }

    public int hashCode() {
        return this.mAuthType;
    }

    public String toString() {
        return "NonEAPInnerAuth{mAuthType=" + this.mAuthType + "}";
    }
}
