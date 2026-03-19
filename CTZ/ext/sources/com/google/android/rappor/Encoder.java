package com.google.android.rappor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.BitSet;
import java.util.Random;

public class Encoder {
    private static final byte HMAC_DRBG_TYPE_COHORT = 0;
    private static final byte HMAC_DRBG_TYPE_PRR = 1;
    public static final int MAX_BITS = 4096;
    public static final int MAX_BLOOM_HASHES = 8;
    public static final int MAX_COHORTS = 128;
    public static final int MIN_USER_SECRET_BYTES = 48;
    public static final long VERSION = 3;
    private final int cohort;
    private final byte[] encoderIdBytes;
    private final BitSet inputMask;
    private final MessageDigest md5;
    private final int numBits;
    private final int numBloomHashes;
    private final int numCohorts;
    private final double probabilityF;
    private final double probabilityP;
    private final double probabilityQ;
    private final Random random;
    private final MessageDigest sha256;
    private final byte[] userSecret;

    public Encoder(byte[] bArr, String str, int i, double d, double d2, double d3, int i2, int i3) {
        this(null, null, null, bArr, str, i, d, d2, d3, i2, i3);
    }

    public Encoder(Random random, MessageDigest messageDigest, MessageDigest messageDigest2, byte[] bArr, String str, int i, double d, double d2, double d3, int i2, int i3) {
        if (messageDigest != null) {
            this.md5 = messageDigest;
        } else {
            try {
                this.md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e);
            }
        }
        this.md5.reset();
        if (messageDigest2 != null) {
            this.sha256 = messageDigest2;
        } else {
            try {
                this.sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e2) {
                throw new AssertionError(e2);
            }
        }
        this.sha256.reset();
        this.encoderIdBytes = str.getBytes(StandardCharsets.UTF_8);
        if (random != null) {
            this.random = random;
        } else {
            this.random = new SecureRandom();
        }
        checkArgument(bArr.length >= 48, "userSecret must be at least 48 bytes of high-quality entropy.");
        this.userSecret = bArr;
        checkArgument(d >= 0.0d && d <= 1.0d, "probabilityF must be on range [0.0, 1.0]");
        this.probabilityF = Math.round(d * 128.0d) / 128.0d;
        checkArgument(d2 >= 0.0d && d2 <= 1.0d, "probabilityP must be on range [0.0, 1.0]");
        this.probabilityP = d2;
        checkArgument(d3 >= 0.0d && d3 <= 1.0d, "probabilityQ must be on range [0.0, 1.0]");
        this.probabilityQ = d3;
        checkArgument(i >= 1 && i <= 4096, "numBits must be on range [1, 4096].");
        this.numBits = i;
        this.inputMask = new BitSet(i);
        this.inputMask.set(0, i, true);
        checkArgument(i3 >= 1 && i3 <= i, "numBloomHashes must be on range [1, numBits).");
        this.numBloomHashes = i3;
        checkArgument(i2 >= 1 && i2 <= 128, "numCohorts must be on range [1, 128].");
        int i4 = i2 - 1;
        checkArgument((i2 & i4) == 0, "numCohorts must be a power of 2.");
        this.numCohorts = i2;
        this.cohort = (Math.abs(ByteBuffer.wrap(new HmacDrbg(bArr, new byte[]{HMAC_DRBG_TYPE_COHORT}).nextBytes(4)).getInt()) % 128) & i4;
    }

    public double getProbabilityF() {
        return this.probabilityF;
    }

    public double getProbabilityP() {
        return this.probabilityP;
    }

    public double getProbabilityQ() {
        return this.probabilityQ;
    }

    public int getNumBits() {
        return this.numBits;
    }

    public int getNumBloomHashes() {
        return this.numBloomHashes;
    }

    public int getNumCohorts() {
        return this.numCohorts;
    }

    public int getCohort() {
        return this.cohort;
    }

    public String getEncoderId() {
        return new String(this.encoderIdBytes, StandardCharsets.UTF_8);
    }

    public byte[] encodeBoolean(boolean z) {
        BitSet bitSet = new BitSet(this.numBits);
        bitSet.set(0, z);
        return encodeBits(bitSet);
    }

    public byte[] encodeOrdinal(int i) {
        checkArgument(i >= 0 && i < this.numBits, "Ordinal value must be in range [0, numBits).");
        BitSet bitSet = new BitSet(this.numBits);
        bitSet.set(i, true);
        return encodeBits(bitSet);
    }

    public byte[] encodeString(String str) {
        byte[] bArrDigest;
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] bArrArray = ByteBuffer.allocate(4 + bytes.length).putInt(this.cohort).put(bytes).array();
        synchronized (this) {
            this.md5.reset();
            bArrDigest = this.md5.digest(bArrArray);
        }
        verify(bArrDigest.length == 16);
        verify(this.numBloomHashes <= bArrDigest.length / 2);
        BitSet bitSet = new BitSet(this.numBits);
        for (int i = 0; i < this.numBloomHashes; i++) {
            int i2 = i * 2;
            bitSet.set((((bArrDigest[i2] & 255) * 256) + (bArrDigest[i2 + 1] & 255)) % this.numBits, true);
        }
        return encodeBits(bitSet);
    }

    public byte[] encodeBits(byte[] bArr) {
        return encodeBits(BitSet.valueOf(bArr));
    }

    private byte[] encodeBits(BitSet bitSet) {
        byte[] byteArray = computeInstantaneousRandomizedResponse(computePermanentRandomizedResponse(bitSet)).toByteArray();
        byte[] bArr = new byte[(this.numBits + 7) / 8];
        verify(byteArray.length <= bArr.length);
        System.arraycopy(byteArray, 0, bArr, 0, byteArray.length);
        return bArr;
    }

    private BitSet computePermanentRandomizedResponse(BitSet bitSet) {
        byte[] bArr;
        BitSet bitSet2 = new BitSet();
        bitSet2.or(bitSet);
        bitSet2.andNot(this.inputMask);
        checkArgument(bitSet2.isEmpty(), "Input bits had bits set past Encoder's numBits limit.");
        if (this.probabilityF == 0.0d) {
            return bitSet;
        }
        synchronized (this) {
            bArr = new byte[Math.min(20, this.sha256.getDigestLength() + 1)];
            bArr[0] = HMAC_DRBG_TYPE_PRR;
            this.sha256.reset();
            this.sha256.update(this.encoderIdBytes);
            this.sha256.update(new byte[]{HMAC_DRBG_TYPE_COHORT});
            this.sha256.update(bitSet.toByteArray());
            System.arraycopy(this.sha256.digest(bArr), 0, bArr, 1, bArr.length - 1);
        }
        byte[] bArrNextBytes = new HmacDrbg(this.userSecret, bArr).nextBytes(this.numBits);
        verify(this.numBits <= bArrNextBytes.length);
        int iRound = (int) Math.round(this.probabilityF * 128.0d);
        BitSet bitSet3 = new BitSet(this.numBits);
        for (int i = 0; i < this.numBits; i++) {
            int i2 = bArrNextBytes[i] & 255;
            if ((i2 >> 1) < iRound) {
                bitSet3.set(i, (i2 & 1) > 0);
            } else {
                bitSet3.set(i, bitSet.get(i));
            }
        }
        return bitSet3;
    }

    private BitSet computeInstantaneousRandomizedResponse(BitSet bitSet) {
        BitSet bitSet2 = new BitSet();
        bitSet2.or(bitSet);
        bitSet2.andNot(this.inputMask);
        checkArgument(bitSet2.isEmpty(), "Input bits had bits set past Encoder's numBits limit.");
        if (this.probabilityP == 0.0d && this.probabilityQ == 1.0d) {
            return bitSet;
        }
        BitSet bitSet3 = new BitSet(this.numBits);
        for (int i = 0; i < this.numBits; i++) {
            bitSet3.set(i, ((double) this.random.nextFloat()) < (bitSet.get(i) ? this.probabilityQ : this.probabilityP));
        }
        return bitSet3;
    }

    private static void checkArgument(boolean z, Object obj) {
        if (!z) {
            throw new IllegalArgumentException(String.valueOf(obj));
        }
    }

    private static void verify(boolean z) {
        if (!z) {
            throw new IllegalStateException();
        }
    }
}
