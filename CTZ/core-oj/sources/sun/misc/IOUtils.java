package sun.misc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class IOUtils {
    public static byte[] readFully(InputStream inputStream, int i, boolean z) throws IOException {
        int length;
        int i2 = 0;
        byte[] bArrCopyOf = new byte[0];
        if (i == -1) {
            i = Integer.MAX_VALUE;
        }
        while (i2 < i) {
            if (i2 >= bArrCopyOf.length) {
                length = Math.min(i - i2, bArrCopyOf.length + 1024);
                int i3 = i2 + length;
                if (bArrCopyOf.length < i3) {
                    bArrCopyOf = Arrays.copyOf(bArrCopyOf, i3);
                }
            } else {
                length = bArrCopyOf.length - i2;
            }
            int i4 = inputStream.read(bArrCopyOf, i2, length);
            if (i4 < 0) {
                if (z && i != Integer.MAX_VALUE) {
                    throw new EOFException("Detect premature EOF");
                }
                if (bArrCopyOf.length != i2) {
                    return Arrays.copyOf(bArrCopyOf, i2);
                }
                return bArrCopyOf;
            }
            i2 += i4;
        }
        return bArrCopyOf;
    }
}
