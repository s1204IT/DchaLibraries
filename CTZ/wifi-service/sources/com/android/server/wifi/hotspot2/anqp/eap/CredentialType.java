package com.android.server.wifi.hotspot2.anqp.eap;

import com.android.internal.annotations.VisibleForTesting;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class CredentialType extends AuthParam {
    public static final int CREDENTIAL_TYPE_ANONYMOUS = 9;
    public static final int CREDENTIAL_TYPE_CERTIFICATE = 6;
    public static final int CREDENTIAL_TYPE_HARDWARE_TOKEN = 4;
    public static final int CREDENTIAL_TYPE_NFC = 3;
    public static final int CREDENTIAL_TYPE_NONE = 8;
    public static final int CREDENTIAL_TYPE_SIM = 1;
    public static final int CREDENTIAL_TYPE_SOFTWARE_TOKEN = 5;
    public static final int CREDENTIAL_TYPE_USERNAME_PASSWORD = 7;
    public static final int CREDENTIAL_TYPE_USIM = 2;
    public static final int CREDENTIAL_TYPE_VENDOR_SPECIFIC = 10;

    @VisibleForTesting
    public static final int EXPECTED_LENGTH_VALUE = 1;
    private final int mType;

    @VisibleForTesting
    public CredentialType(int i, int i2) {
        super(i);
        this.mType = i2;
    }

    public static CredentialType parse(ByteBuffer byteBuffer, int i, boolean z) throws ProtocolException {
        if (i != 1) {
            throw new ProtocolException("Invalid length: " + i);
        }
        return new CredentialType(z ? 6 : 5, byteBuffer.get() & 255);
    }

    public int getType() {
        return this.mType;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj instanceof CredentialType) && this.mType == ((CredentialType) obj).mType;
    }

    public int hashCode() {
        return this.mType;
    }

    public String toString() {
        return "CredentialType{mType=" + this.mType + "}";
    }
}
