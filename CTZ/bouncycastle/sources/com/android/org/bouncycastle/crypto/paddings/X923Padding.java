package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import java.security.SecureRandom;

public class X923Padding implements BlockCipherPadding {
    SecureRandom random = null;

    @Override
    public void init(SecureRandom secureRandom) throws IllegalArgumentException {
        this.random = secureRandom;
    }

    @Override
    public String getPaddingName() {
        return "X9.23";
    }

    @Override
    public int addPadding(byte[] bArr, int i) {
        byte length = (byte) (bArr.length - i);
        while (i < bArr.length - 1) {
            if (this.random == null) {
                bArr[i] = 0;
            } else {
                bArr[i] = (byte) this.random.nextInt();
            }
            i++;
        }
        bArr[i] = length;
        return length;
    }

    @Override
    public int padCount(byte[] bArr) throws InvalidCipherTextException {
        int i = bArr[bArr.length - 1] & 255;
        if (i > bArr.length) {
            throw new InvalidCipherTextException("pad block corrupted");
        }
        return i;
    }
}
