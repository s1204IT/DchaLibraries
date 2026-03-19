package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import java.security.SecureRandom;

public class ISO7816d4Padding implements BlockCipherPadding {
    @Override
    public void init(SecureRandom random) throws IllegalArgumentException {
    }

    @Override
    public String getPaddingName() {
        return "ISO7816-4";
    }

    @Override
    public int addPadding(byte[] in, int inOff) {
        int added = in.length - inOff;
        in[inOff] = -128;
        while (true) {
            inOff++;
            if (inOff < in.length) {
                in[inOff] = 0;
            } else {
                return added;
            }
        }
    }

    @Override
    public int padCount(byte[] in) throws InvalidCipherTextException {
        int count = in.length - 1;
        while (count > 0 && in[count] == 0) {
            count--;
        }
        if (in[count] != -128) {
            throw new InvalidCipherTextException("pad block corrupted");
        }
        return in.length - count;
    }
}
