package com.android.server.wifi.hotspot2.anqp.eap;

import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VendorSpecificAuth extends AuthParam {
    private final byte[] mData;

    @VisibleForTesting
    public VendorSpecificAuth(byte[] bArr) {
        super(AuthParam.PARAM_TYPE_VENDOR_SPECIFIC);
        this.mData = bArr;
    }

    public static VendorSpecificAuth parse(ByteBuffer byteBuffer, int i) {
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr);
        return new VendorSpecificAuth(bArr);
    }

    public byte[] getData() {
        return this.mData;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VendorSpecificAuth)) {
            return false;
        }
        return Arrays.equals(this.mData, ((VendorSpecificAuth) obj).mData);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mData);
    }

    public String toString() {
        return "VendorSpecificAuth{mData=" + Arrays.toString(this.mData) + "}";
    }
}
