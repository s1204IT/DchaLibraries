package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import java.security.SecureRandom;

public class X923Padding implements BlockCipherPadding {
    SecureRandom random = null;

    @Override
    public void init(SecureRandom random) throws IllegalArgumentException {
        this.random = random;
    }

    @Override
    public String getPaddingName() {
        return "X9.23";
    }

    @Override
    public int addPadding(byte[] in, int inOff) {
        byte code = (byte) (in.length - inOff);
        while (inOff < in.length - 1) {
            if (this.random == null) {
                in[inOff] = 0;
            } else {
                in[inOff] = (byte) this.random.nextInt();
            }
            inOff++;
        }
        in[inOff] = code;
        return code;
    }

    @Override
    public int padCount(byte[] in) throws InvalidCipherTextException {
        int count = in[in.length - 1] & 255;
        if (count > in.length) {
            throw new InvalidCipherTextException("pad block corrupted");
        }
        return count;
    }
}
