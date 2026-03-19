package java.security;

import java.nio.ByteBuffer;
import sun.security.jca.JCAUtil;

public abstract class MessageDigestSpi {
    private byte[] tempArray;

    protected abstract byte[] engineDigest();

    protected abstract void engineReset();

    protected abstract void engineUpdate(byte b);

    protected abstract void engineUpdate(byte[] bArr, int i, int i2);

    protected int engineGetDigestLength() {
        return 0;
    }

    protected void engineUpdate(ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return;
        }
        if (byteBuffer.hasArray()) {
            byte[] bArrArray = byteBuffer.array();
            int iArrayOffset = byteBuffer.arrayOffset();
            int iPosition = byteBuffer.position();
            int iLimit = byteBuffer.limit();
            engineUpdate(bArrArray, iArrayOffset + iPosition, iLimit - iPosition);
            byteBuffer.position(iLimit);
            return;
        }
        int iRemaining = byteBuffer.remaining();
        int tempArraySize = JCAUtil.getTempArraySize(iRemaining);
        if (this.tempArray == null || tempArraySize > this.tempArray.length) {
            this.tempArray = new byte[tempArraySize];
        }
        while (iRemaining > 0) {
            int iMin = Math.min(iRemaining, this.tempArray.length);
            byteBuffer.get(this.tempArray, 0, iMin);
            engineUpdate(this.tempArray, 0, iMin);
            iRemaining -= iMin;
        }
    }

    protected int engineDigest(byte[] bArr, int i, int i2) throws DigestException {
        byte[] bArrEngineDigest = engineDigest();
        if (i2 < bArrEngineDigest.length) {
            throw new DigestException("partial digests not returned");
        }
        if (bArr.length - i < bArrEngineDigest.length) {
            throw new DigestException("insufficient space in the output buffer to store the digest");
        }
        System.arraycopy(bArrEngineDigest, 0, bArr, i, bArrEngineDigest.length);
        return bArrEngineDigest.length;
    }

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
