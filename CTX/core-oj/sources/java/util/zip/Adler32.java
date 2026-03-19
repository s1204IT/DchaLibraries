package java.util.zip;

import java.nio.ByteBuffer;
import sun.nio.ch.DirectBuffer;

public class Adler32 implements Checksum {
    static final boolean $assertionsDisabled = false;
    private int adler = 1;

    private static native int update(int i, int i2);

    private static native int updateByteBuffer(int i, long j, int i2, int i3);

    private static native int updateBytes(int i, byte[] bArr, int i2, int i3);

    @Override
    public void update(int i) {
        this.adler = update(this.adler, i);
    }

    @Override
    public void update(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i > bArr.length - i2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.adler = updateBytes(this.adler, bArr, i, i2);
    }

    public void update(byte[] bArr) {
        this.adler = updateBytes(this.adler, bArr, 0, bArr.length);
    }

    public void update(ByteBuffer byteBuffer) {
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        int i = iLimit - iPosition;
        if (i <= 0) {
            return;
        }
        if (byteBuffer instanceof DirectBuffer) {
            this.adler = updateByteBuffer(this.adler, ((DirectBuffer) byteBuffer).address(), iPosition, i);
        } else if (byteBuffer.hasArray()) {
            this.adler = updateBytes(this.adler, byteBuffer.array(), iPosition + byteBuffer.arrayOffset(), i);
        } else {
            byte[] bArr = new byte[i];
            byteBuffer.get(bArr);
            this.adler = updateBytes(this.adler, bArr, 0, bArr.length);
        }
        byteBuffer.position(iLimit);
    }

    @Override
    public void reset() {
        this.adler = 1;
    }

    @Override
    public long getValue() {
        return ((long) this.adler) & 4294967295L;
    }
}
