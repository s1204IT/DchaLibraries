package android.icu.impl;

import android.icu.impl.ICUBinary;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class StringPrepDataReader implements ICUBinary.Authenticate {
    private static final int DATA_FORMAT_ID = 1397772880;
    private ByteBuffer byteBuffer;
    private int unicodeVersion;
    private static final boolean debug = ICUDebug.enabled("NormalizerDataReader");
    private static final byte[] DATA_FORMAT_VERSION = {3, 2, 5, 2};

    public StringPrepDataReader(ByteBuffer byteBuffer) throws IOException {
        if (debug) {
            System.out.println("Bytes in buffer " + byteBuffer.remaining());
        }
        this.byteBuffer = byteBuffer;
        this.unicodeVersion = ICUBinary.readHeader(this.byteBuffer, DATA_FORMAT_ID, this);
        if (debug) {
            System.out.println("Bytes left in byteBuffer " + this.byteBuffer.remaining());
        }
    }

    public char[] read(int i) throws IOException {
        return ICUBinary.getChars(this.byteBuffer, i, 0);
    }

    @Override
    public boolean isDataVersionAcceptable(byte[] bArr) {
        return bArr[0] == DATA_FORMAT_VERSION[0] && bArr[2] == DATA_FORMAT_VERSION[2] && bArr[3] == DATA_FORMAT_VERSION[3];
    }

    public int[] readIndexes(int i) throws IOException {
        int[] iArr = new int[i];
        for (int i2 = 0; i2 < i; i2++) {
            iArr[i2] = this.byteBuffer.getInt();
        }
        return iArr;
    }

    public byte[] getUnicodeVersion() {
        return ICUBinary.getVersionByteArrayFromCompactInt(this.unicodeVersion);
    }
}
