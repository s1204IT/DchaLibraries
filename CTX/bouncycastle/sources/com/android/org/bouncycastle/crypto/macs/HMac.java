package com.android.org.bouncycastle.crypto.macs;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.ExtendedDigest;
import com.android.org.bouncycastle.crypto.Mac;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.util.Integers;
import com.android.org.bouncycastle.util.Memoable;
import java.util.Hashtable;

public class HMac implements Mac {
    private static final byte IPAD = 54;
    private static final byte OPAD = 92;
    private static Hashtable blockLengths = new Hashtable();
    private int blockLength;
    private Digest digest;
    private int digestSize;
    private byte[] inputPad;
    private Memoable ipadState;
    private Memoable opadState;
    private byte[] outputBuf;

    static {
        blockLengths.put("MD5", Integers.valueOf(64));
        blockLengths.put("SHA-1", Integers.valueOf(64));
        blockLengths.put("SHA-224", Integers.valueOf(64));
        blockLengths.put("SHA-256", Integers.valueOf(64));
        blockLengths.put("SHA-384", Integers.valueOf(128));
        blockLengths.put("SHA-512", Integers.valueOf(128));
    }

    private static int getByteLength(Digest digest) {
        if (digest instanceof ExtendedDigest) {
            return ((ExtendedDigest) digest).getByteLength();
        }
        Integer num = (Integer) blockLengths.get(digest.getAlgorithmName());
        if (num == null) {
            throw new IllegalArgumentException("unknown digest passed: " + digest.getAlgorithmName());
        }
        return num.intValue();
    }

    public HMac(Digest digest) {
        this(digest, getByteLength(digest));
    }

    private HMac(Digest digest, int i) {
        this.digest = digest;
        this.digestSize = digest.getDigestSize();
        this.blockLength = i;
        this.inputPad = new byte[this.blockLength];
        this.outputBuf = new byte[this.blockLength + this.digestSize];
    }

    @Override
    public String getAlgorithmName() {
        return this.digest.getAlgorithmName() + "/HMAC";
    }

    public Digest getUnderlyingDigest() {
        return this.digest;
    }

    @Override
    public void init(CipherParameters cipherParameters) {
        this.digest.reset();
        byte[] key = ((KeyParameter) cipherParameters).getKey();
        int length = key.length;
        if (length > this.blockLength) {
            this.digest.update(key, 0, length);
            this.digest.doFinal(this.inputPad, 0);
            length = this.digestSize;
        } else {
            System.arraycopy(key, 0, this.inputPad, 0, length);
        }
        while (length < this.inputPad.length) {
            this.inputPad[length] = 0;
            length++;
        }
        System.arraycopy(this.inputPad, 0, this.outputBuf, 0, this.blockLength);
        xorPad(this.inputPad, this.blockLength, IPAD);
        xorPad(this.outputBuf, this.blockLength, OPAD);
        if (this.digest instanceof Memoable) {
            this.opadState = ((Memoable) this.digest).copy();
            ((Digest) this.opadState).update(this.outputBuf, 0, this.blockLength);
        }
        this.digest.update(this.inputPad, 0, this.inputPad.length);
        if (this.digest instanceof Memoable) {
            this.ipadState = ((Memoable) this.digest).copy();
        }
    }

    @Override
    public int getMacSize() {
        return this.digestSize;
    }

    @Override
    public void update(byte b) {
        this.digest.update(b);
    }

    @Override
    public void update(byte[] bArr, int i, int i2) {
        this.digest.update(bArr, i, i2);
    }

    @Override
    public int doFinal(byte[] bArr, int i) {
        this.digest.doFinal(this.outputBuf, this.blockLength);
        if (this.opadState != null) {
            ((Memoable) this.digest).reset(this.opadState);
            this.digest.update(this.outputBuf, this.blockLength, this.digest.getDigestSize());
        } else {
            this.digest.update(this.outputBuf, 0, this.outputBuf.length);
        }
        int iDoFinal = this.digest.doFinal(bArr, i);
        for (int i2 = this.blockLength; i2 < this.outputBuf.length; i2++) {
            this.outputBuf[i2] = 0;
        }
        if (this.ipadState != null) {
            ((Memoable) this.digest).reset(this.ipadState);
        } else {
            this.digest.update(this.inputPad, 0, this.inputPad.length);
        }
        return iDoFinal;
    }

    @Override
    public void reset() {
        this.digest.reset();
        this.digest.update(this.inputPad, 0, this.inputPad.length);
    }

    private static void xorPad(byte[] bArr, int i, byte b) {
        for (int i2 = 0; i2 < i; i2++) {
            bArr[i2] = (byte) (bArr[i2] ^ b);
        }
    }
}
