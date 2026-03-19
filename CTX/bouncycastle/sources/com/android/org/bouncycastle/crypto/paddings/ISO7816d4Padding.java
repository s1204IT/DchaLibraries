package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import java.security.SecureRandom;

public class ISO7816d4Padding implements BlockCipherPadding {
    @Override
    public void init(SecureRandom secureRandom) throws IllegalArgumentException {
    }

    @Override
    public String getPaddingName() {
        return "ISO7816-4";
    }

    @Override
    public int addPadding(byte[] bArr, int i) {
        int length = bArr.length - i;
        bArr[i] = -128;
        while (true) {
            i++;
            if (i < bArr.length) {
                bArr[i] = 0;
            } else {
                return length;
            }
        }
    }

    @Override
    public int padCount(byte[] bArr) throws InvalidCipherTextException {
        int length = bArr.length - 1;
        while (length > 0 && bArr[length] == 0) {
            length--;
        }
        if (bArr[length] != -128) {
            throw new InvalidCipherTextException("pad block corrupted");
        }
        return bArr.length - length;
    }
}
