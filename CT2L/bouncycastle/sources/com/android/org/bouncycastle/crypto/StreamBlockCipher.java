package com.android.org.bouncycastle.crypto;

public class StreamBlockCipher implements StreamCipher {
    private BlockCipher cipher;
    private byte[] oneByte = new byte[1];

    public StreamBlockCipher(BlockCipher cipher) {
        if (cipher.getBlockSize() != 1) {
            throw new IllegalArgumentException("block cipher block size != 1.");
        }
        this.cipher = cipher;
    }

    @Override
    public void init(boolean forEncryption, CipherParameters params) {
        this.cipher.init(forEncryption, params);
    }

    @Override
    public String getAlgorithmName() {
        return this.cipher.getAlgorithmName();
    }

    @Override
    public byte returnByte(byte in) {
        this.oneByte[0] = in;
        this.cipher.processBlock(this.oneByte, 0, this.oneByte, 0);
        return this.oneByte[0];
    }

    @Override
    public void processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
        if (outOff + len > out.length) {
            throw new DataLengthException("output buffer too small in processBytes()");
        }
        for (int i = 0; i != len; i++) {
            this.cipher.processBlock(in, inOff + i, out, outOff + i);
        }
    }

    @Override
    public void reset() {
        this.cipher.reset();
    }
}
