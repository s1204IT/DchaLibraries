package com.google.android.rappor;

import java.security.SecureRandom;
import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@NotThreadSafe
public class HmacDrbg {
    private static final byte[] BYTE_ARRAY_0 = {0};
    private static final byte[] BYTE_ARRAY_1 = {1};
    private static final int DIGEST_NUM_BYTES = 32;
    public static final int ENTROPY_INPUT_SIZE_BYTES = 48;
    private static final int MAX_BYTES_PER_REQUEST = 937;
    public static final int MAX_BYTES_TOTAL = 10000;
    public static final int MAX_PERSONALIZATION_STRING_LENGTH_BYTES = 20;
    public static final int SECURITY_STRENGTH = 256;
    private int bytesGenerated;
    private Mac hmac;
    private byte[] value;

    public HmacDrbg(byte[] bArr, byte[] bArr2) {
        byte[] bArrBytesConcat = bytesConcat(bArr, emptyIfNull(bArr2));
        setKey(new byte[32]);
        this.value = new byte[32];
        Arrays.fill(this.value, (byte) 1);
        hmacDrbgUpdate(bArrBytesConcat);
        this.bytesGenerated = 0;
    }

    private static byte[] emptyIfNull(byte[] bArr) {
        return bArr == null ? new byte[0] : bArr;
    }

    private void setKey(byte[] bArr) {
        try {
            this.hmac = Mac.getInstance("HmacSHA256");
            this.hmac.init(new SecretKeySpec(bArr, "HmacSHA256"));
        } catch (Exception e) {
        }
    }

    private byte[] hash(byte[] bArr) {
        try {
            return this.hmac.doFinal(bArr);
        } catch (Exception e) {
            return null;
        }
    }

    private void hmacDrbgUpdate(byte[] bArr) {
        setKey(hash(bytesConcat(this.value, BYTE_ARRAY_0, emptyIfNull(bArr))));
        this.value = hash(this.value);
        if (bArr == null) {
            return;
        }
        setKey(hash(bytesConcat(this.value, BYTE_ARRAY_1, bArr)));
        this.value = hash(this.value);
    }

    private void hmacDrbgGenerate(byte[] bArr, int i, int i2) {
        int i3 = 0;
        while (i3 < i2) {
            this.value = hash(this.value);
            int iMin = Math.min(i2 - i3, 32);
            System.arraycopy(this.value, 0, bArr, i + i3, iMin);
            i3 += iMin;
        }
        hmacDrbgUpdate(null);
    }

    public static byte[] generateEntropyInput() {
        byte[] bArr = new byte[48];
        new SecureRandom().nextBytes(bArr);
        return bArr;
    }

    public byte[] nextBytes(int i) {
        byte[] bArr = new byte[i];
        nextBytes(bArr);
        return bArr;
    }

    public void nextBytes(byte[] bArr) {
        nextBytes(bArr, 0, bArr.length);
    }

    public void nextBytes(byte[] bArr, int i, int i2) {
        if (i2 == 0) {
            return;
        }
        if (this.bytesGenerated + i2 > 10000) {
            throw new IllegalStateException("Cannot generate more than a total of " + i2 + " bytes.");
        }
        int i3 = 0;
        while (i3 < i2) {
            try {
                int iMin = Math.min(i2 - i3, MAX_BYTES_PER_REQUEST);
                hmacDrbgGenerate(bArr, i + i3, iMin);
                i3 += iMin;
            } finally {
                this.bytesGenerated += i2;
            }
        }
    }

    private static byte[] bytesConcat(byte[]... bArr) {
        int length = 0;
        for (byte[] bArr2 : bArr) {
            length += bArr2.length;
        }
        byte[] bArr3 = new byte[length];
        int length2 = 0;
        for (byte[] bArr4 : bArr) {
            System.arraycopy(bArr4, 0, bArr3, length2, bArr4.length);
            length2 += bArr4.length;
        }
        return bArr3;
    }
}
