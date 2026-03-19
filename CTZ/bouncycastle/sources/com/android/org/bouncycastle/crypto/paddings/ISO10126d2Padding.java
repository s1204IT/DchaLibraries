package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import java.security.SecureRandom;

public class ISO10126d2Padding implements BlockCipherPadding {
    SecureRandom random;

    @Override
    public void init(SecureRandom secureRandom) throws IllegalArgumentException {
        if (secureRandom != null) {
            this.random = secureRandom;
        } else {
            this.random = new SecureRandom();
        }
    }

    @Override
    public String getPaddingName() {
        return "ISO10126-2";
    }

    @Override
    public int addPadding(byte[] bArr, int i) {
        byte length = (byte) (bArr.length - i);
        while (i < bArr.length - 1) {
            bArr[i] = (byte) this.random.nextInt();
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
