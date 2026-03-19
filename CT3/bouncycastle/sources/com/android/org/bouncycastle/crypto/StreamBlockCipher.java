package com.android.org.bouncycastle.crypto;

public abstract class StreamBlockCipher implements BlockCipher, StreamCipher {
    private final BlockCipher cipher;

    protected abstract byte calculateByte(byte b);

    protected StreamBlockCipher(BlockCipher cipher) {
        this.cipher = cipher;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    @Override
    public final byte returnByte(byte in) {
        return calculateByte(in);
    }

    @Override
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
        if (outOff + len > out.length) {
            throw new DataLengthException("output buffer too short");
        }
        if (inOff + len > in.length) {
            throw new DataLengthException("input buffer too small");
        }
        int inEnd = inOff + len;
        int outStart = outOff;
        for (int inStart = inOff; inStart < inEnd; inStart++) {
            out[outStart] = calculateByte(in[inStart]);
            outStart++;
        }
        return len;
    }
}
