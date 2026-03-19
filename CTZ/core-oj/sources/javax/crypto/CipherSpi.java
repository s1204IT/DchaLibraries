package javax.crypto;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

public abstract class CipherSpi {
    protected abstract int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException;

    protected abstract byte[] engineDoFinal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException;

    protected abstract int engineGetBlockSize();

    protected abstract byte[] engineGetIV();

    protected abstract int engineGetOutputSize(int i);

    protected abstract AlgorithmParameters engineGetParameters();

    protected abstract void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException;

    protected abstract void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineSetMode(String str) throws NoSuchAlgorithmException;

    protected abstract void engineSetPadding(String str) throws NoSuchPaddingException;

    protected abstract int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException;

    protected abstract byte[] engineUpdate(byte[] bArr, int i, int i2);

    protected int engineUpdate(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws ShortBufferException {
        try {
            return bufferCrypt(byteBuffer, byteBuffer2, true);
        } catch (BadPaddingException e) {
            throw new ProviderException("Internal error in update()");
        } catch (IllegalBlockSizeException e2) {
            throw new ProviderException("Internal error in update()");
        }
    }

    protected int engineDoFinal(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        return bufferCrypt(byteBuffer, byteBuffer2, false);
    }

    static int getTempArraySize(int i) {
        return Math.min(4096, i);
    }

    private int bufferCrypt(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, boolean z) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] bArr;
        int iArrayOffset;
        int i;
        int i2;
        byte[] bArr2;
        int iEngineUpdate;
        int i3;
        ShortBufferException e;
        byte[] bArr3;
        int i4;
        int iEngineUpdate2;
        int iEngineDoFinal;
        if (byteBuffer == null || byteBuffer2 == null) {
            throw new NullPointerException("Input and output buffers must not be null");
        }
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        int i5 = iLimit - iPosition;
        if (z && i5 == 0) {
            return 0;
        }
        int iEngineGetOutputSize = engineGetOutputSize(i5);
        if (byteBuffer2.remaining() < iEngineGetOutputSize) {
            throw new ShortBufferException("Need at least " + iEngineGetOutputSize + " bytes of space in output buffer");
        }
        boolean zHasArray = byteBuffer.hasArray();
        boolean zHasArray2 = byteBuffer2.hasArray();
        if (zHasArray && zHasArray2) {
            byte[] bArrArray = byteBuffer.array();
            int iArrayOffset2 = byteBuffer.arrayOffset() + iPosition;
            byte[] bArrArray2 = byteBuffer2.array();
            int iPosition2 = byteBuffer2.position();
            int iArrayOffset3 = byteBuffer2.arrayOffset() + iPosition2;
            if (z) {
                iEngineDoFinal = engineUpdate(bArrArray, iArrayOffset2, i5, bArrArray2, iArrayOffset3);
            } else {
                iEngineDoFinal = engineDoFinal(bArrArray, iArrayOffset2, i5, bArrArray2, iArrayOffset3);
            }
            byteBuffer.position(iLimit);
            byteBuffer2.position(iPosition2 + iEngineDoFinal);
            return iEngineDoFinal;
        }
        if (!zHasArray && zHasArray2) {
            int iPosition3 = byteBuffer2.position();
            byte[] bArrArray3 = byteBuffer2.array();
            int iArrayOffset4 = byteBuffer2.arrayOffset() + iPosition3;
            byte[] bArr4 = new byte[getTempArraySize(i5)];
            int i6 = iArrayOffset4;
            int i7 = i5;
            int i8 = 0;
            do {
                int iMin = Math.min(i7, bArr4.length);
                if (iMin > 0) {
                    byteBuffer.get(bArr4, 0, iMin);
                }
                if (z || i7 != iMin) {
                    i4 = iMin;
                    iEngineUpdate2 = engineUpdate(bArr4, 0, i4, bArrArray3, i6);
                } else {
                    i4 = iMin;
                    iEngineUpdate2 = engineDoFinal(bArr4, 0, iMin, bArrArray3, i6);
                }
                i8 += iEngineUpdate2;
                i6 += iEngineUpdate2;
                i7 -= i4;
            } while (i7 > 0);
            byteBuffer2.position(iPosition3 + i8);
            return i8;
        }
        if (zHasArray) {
            byte[] bArrArray4 = byteBuffer.array();
            iArrayOffset = iPosition + byteBuffer.arrayOffset();
            bArr = bArrArray4;
        } else {
            bArr = new byte[getTempArraySize(i5)];
            iArrayOffset = 0;
        }
        byte[] bArr5 = new byte[getTempArraySize(iEngineGetOutputSize)];
        int length = bArr5.length;
        byte[] bArr6 = bArr5;
        int i9 = i5;
        boolean z2 = false;
        int i10 = 0;
        int i11 = iArrayOffset;
        int i12 = length;
        do {
            int iMin2 = Math.min(i9, i12 == 0 ? bArr.length : i12);
            if (zHasArray || z2 || iMin2 <= 0) {
                i = i11;
            } else {
                byteBuffer.get(bArr, 0, iMin2);
                i = 0;
            }
            if (z || i9 != iMin2) {
                i2 = iMin2;
                bArr2 = bArr6;
                iEngineUpdate = engineUpdate(bArr, i, i2, bArr2, 0);
            } else {
                i2 = iMin2;
                bArr2 = bArr6;
                try {
                    iEngineUpdate = engineDoFinal(bArr, i, iMin2, bArr6, 0);
                } catch (ShortBufferException e2) {
                    e = e2;
                    i3 = i2;
                    if (!z2) {
                    }
                }
            }
            i3 = i2;
            i += i3;
            i9 -= i3;
            if (iEngineUpdate > 0) {
                bArr3 = bArr2;
                try {
                    byteBuffer2.put(bArr3, 0, iEngineUpdate);
                    i10 += iEngineUpdate;
                } catch (ShortBufferException e3) {
                    e = e3;
                    z2 = false;
                    if (!z2) {
                        throw ((ProviderException) new ProviderException("Could not determine buffer size").initCause(e));
                    }
                    int iEngineGetOutputSize2 = engineGetOutputSize(i3);
                    bArr6 = new byte[iEngineGetOutputSize2];
                    z2 = true;
                    i12 = iEngineGetOutputSize2;
                }
            } else {
                bArr3 = bArr2;
            }
            bArr6 = bArr3;
            z2 = false;
            i11 = i;
        } while (i9 > 0);
        if (zHasArray) {
            byteBuffer.position(iLimit);
        }
        return i10;
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected Key engineUnwrap(byte[] bArr, String str, int i) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected void engineUpdateAAD(byte[] bArr, int i, int i2) {
        throw new UnsupportedOperationException("The underlying Cipher implementation does not support this method");
    }

    protected void engineUpdateAAD(ByteBuffer byteBuffer) {
        throw new UnsupportedOperationException("The underlying Cipher implementation does not support this method");
    }
}
