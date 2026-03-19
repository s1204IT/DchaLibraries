package com.google.android.mms.pdu;

import java.io.ByteArrayOutputStream;

public class QuotedPrintable {
    private static byte ESCAPE_CHAR = 61;

    public static final byte[] decodeQuotedPrintable(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i = 0;
        while (i < bArr.length) {
            byte b = bArr[i];
            if (b == ESCAPE_CHAR) {
                int i2 = i + 1;
                try {
                    if ('\r' == ((char) bArr[i2])) {
                        i += 2;
                        if ('\n' != ((char) bArr[i])) {
                            int iDigit = Character.digit((char) bArr[i2], 16);
                            int i3 = i2 + 1;
                            int iDigit2 = Character.digit((char) bArr[i3], 16);
                            if (iDigit != -1 && iDigit2 != -1) {
                                byteArrayOutputStream.write((char) ((iDigit << 4) + iDigit2));
                                i = i3;
                            }
                            return null;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                byteArrayOutputStream.write(b);
            }
            i++;
        }
        return byteArrayOutputStream.toByteArray();
    }
}
