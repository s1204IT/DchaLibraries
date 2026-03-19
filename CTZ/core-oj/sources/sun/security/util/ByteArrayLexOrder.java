package sun.security.util;

import java.util.Comparator;

public class ByteArrayLexOrder implements Comparator<byte[]> {
    @Override
    public final int compare(byte[] bArr, byte[] bArr2) {
        for (int i = 0; i < bArr.length && i < bArr2.length; i++) {
            int i2 = (bArr[i] & Character.DIRECTIONALITY_UNDEFINED) - (bArr2[i] & Character.DIRECTIONALITY_UNDEFINED);
            if (i2 != 0) {
                return i2;
            }
        }
        return bArr.length - bArr2.length;
    }
}
