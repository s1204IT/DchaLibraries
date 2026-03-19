package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import java.security.SecureRandom;

public class TBCPadding implements BlockCipherPadding {
    @Override
    public void init(SecureRandom secureRandom) throws IllegalArgumentException {
    }

    @Override
    public String getPaddingName() {
        return "TBC";
    }

    @Override
    public int addPadding(byte[] bArr, int i) {
        byte b;
        int length = bArr.length - i;
        if (i > 0) {
            b = (byte) ((bArr[i + (-1)] & 1) == 0 ? 255 : 0);
        } else {
            b = (byte) ((bArr[bArr.length + (-1)] & 1) == 0 ? 255 : 0);
        }
        while (i < bArr.length) {
            bArr[i] = b;
            i++;
        }
        return length;
    }

    @Override
    public int padCount(byte[] bArr) throws InvalidCipherTextException {
        byte b = bArr[bArr.length - 1];
        int length = bArr.length - 1;
        while (length > 0 && bArr[length - 1] == b) {
            length--;
        }
        return bArr.length - length;
    }
}
