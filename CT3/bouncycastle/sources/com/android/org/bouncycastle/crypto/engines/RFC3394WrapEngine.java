package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.Wrapper;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.util.Arrays;

public class RFC3394WrapEngine implements Wrapper {
    private BlockCipher engine;
    private boolean forWrapping;
    private byte[] iv;
    private KeyParameter param;
    private boolean wrapCipherMode;

    public RFC3394WrapEngine(BlockCipher engine) {
        this(engine, false);
    }

    public RFC3394WrapEngine(BlockCipher engine, boolean useReverseDirection) {
        this.iv = new byte[]{-90, -90, -90, -90, -90, -90, -90, -90};
        this.engine = engine;
        this.wrapCipherMode = !useReverseDirection;
    }

    @Override
    public void init(boolean z, CipherParameters cipherParameters) {
        this.forWrapping = z;
        boolean z2 = cipherParameters instanceof ParametersWithRandom;
        ParametersWithIV parameters = cipherParameters;
        if (z2) {
            parameters = cipherParameters.getParameters();
        }
        if (parameters instanceof KeyParameter) {
            this.param = parameters;
        } else {
            if (!(parameters instanceof ParametersWithIV)) {
                return;
            }
            this.iv = parameters.getIV();
            this.param = (KeyParameter) parameters.getParameters();
            if (this.iv.length == 8) {
            } else {
                throw new IllegalArgumentException("IV not equal to 8");
            }
        }
    }

    @Override
    public String getAlgorithmName() {
        return this.engine.getAlgorithmName();
    }

    @Override
    public byte[] wrap(byte[] in, int inOff, int inLen) {
        if (!this.forWrapping) {
            throw new IllegalStateException("not set for wrapping");
        }
        int n = inLen / 8;
        if (n * 8 != inLen) {
            throw new DataLengthException("wrap data must be a multiple of 8 bytes");
        }
        byte[] block = new byte[this.iv.length + inLen];
        byte[] buf = new byte[this.iv.length + 8];
        System.arraycopy(this.iv, 0, block, 0, this.iv.length);
        System.arraycopy(in, inOff, block, this.iv.length, inLen);
        this.engine.init(this.wrapCipherMode, this.param);
        for (int j = 0; j != 6; j++) {
            for (int i = 1; i <= n; i++) {
                System.arraycopy(block, 0, buf, 0, this.iv.length);
                System.arraycopy(block, i * 8, buf, this.iv.length, 8);
                this.engine.processBlock(buf, 0, buf, 0);
                int t = (n * j) + i;
                int k = 1;
                while (t != 0) {
                    byte v = (byte) t;
                    int length = this.iv.length - k;
                    buf[length] = (byte) (buf[length] ^ v);
                    t >>>= 8;
                    k++;
                }
                System.arraycopy(buf, 0, block, 0, 8);
                System.arraycopy(buf, 8, block, i * 8, 8);
            }
        }
        return block;
    }

    @Override
    public byte[] unwrap(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        if (this.forWrapping) {
            throw new IllegalStateException("not set for unwrapping");
        }
        int n = inLen / 8;
        if (n * 8 != inLen) {
            throw new InvalidCipherTextException("unwrap data must be a multiple of 8 bytes");
        }
        byte[] block = new byte[inLen - this.iv.length];
        byte[] a = new byte[this.iv.length];
        byte[] buf = new byte[this.iv.length + 8];
        System.arraycopy(in, inOff, a, 0, this.iv.length);
        System.arraycopy(in, this.iv.length + inOff, block, 0, inLen - this.iv.length);
        this.engine.init(!this.wrapCipherMode, this.param);
        int n2 = n - 1;
        for (int j = 5; j >= 0; j--) {
            for (int i = n2; i >= 1; i--) {
                System.arraycopy(a, 0, buf, 0, this.iv.length);
                System.arraycopy(block, (i - 1) * 8, buf, this.iv.length, 8);
                int t = (n2 * j) + i;
                int k = 1;
                while (t != 0) {
                    byte v = (byte) t;
                    int length = this.iv.length - k;
                    buf[length] = (byte) (buf[length] ^ v);
                    t >>>= 8;
                    k++;
                }
                this.engine.processBlock(buf, 0, buf, 0);
                System.arraycopy(buf, 0, a, 0, 8);
                System.arraycopy(buf, 8, block, (i - 1) * 8, 8);
            }
        }
        if (!Arrays.constantTimeAreEqual(a, this.iv)) {
            throw new InvalidCipherTextException("checksum failed");
        }
        return block;
    }
}
