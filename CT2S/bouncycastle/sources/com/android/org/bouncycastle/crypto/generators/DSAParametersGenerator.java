package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import com.android.org.bouncycastle.crypto.params.DSAParameterGenerationParameters;
import com.android.org.bouncycastle.crypto.params.DSAParameters;
import com.android.org.bouncycastle.crypto.params.DSAValidationParameters;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.BigIntegers;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.SecureRandom;

public class DSAParametersGenerator {
    private int L;
    private int N;
    private int certainty;
    private Digest digest;
    private SecureRandom random;
    private int usageIndex;
    private boolean use186_3;
    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    public DSAParametersGenerator() {
        this(AndroidDigestFactory.getSHA1());
    }

    public DSAParametersGenerator(Digest digest) {
        this.digest = digest;
    }

    public void init(int size, int certainty, SecureRandom random) {
        this.use186_3 = false;
        this.L = size;
        this.N = getDefaultN(size);
        this.certainty = certainty;
        this.random = random;
    }

    public void init(DSAParameterGenerationParameters params) {
        this.use186_3 = true;
        this.L = params.getL();
        this.N = params.getN();
        this.certainty = params.getCertainty();
        this.random = params.getRandom();
        this.usageIndex = params.getUsageIndex();
        if (this.L < 1024 || this.L > 3072 || this.L % 1024 != 0) {
            throw new IllegalArgumentException("L values must be between 1024 and 3072 and a multiple of 1024");
        }
        if (this.L == 1024 && this.N != 160) {
            throw new IllegalArgumentException("N must be 160 for L = 1024");
        }
        if (this.L == 2048 && this.N != 224 && this.N != 256) {
            throw new IllegalArgumentException("N must be 224 or 256 for L = 2048");
        }
        if (this.L == 3072 && this.N != 256) {
            throw new IllegalArgumentException("N must be 256 for L = 3072");
        }
        if (this.digest.getDigestSize() * 8 < this.N) {
            throw new IllegalStateException("Digest output size too small for value of N");
        }
    }

    public DSAParameters generateParameters() {
        return this.use186_3 ? generateParameters_FIPS186_3() : generateParameters_FIPS186_2();
    }

    private DSAParameters generateParameters_FIPS186_2() {
        byte[] seed = new byte[20];
        byte[] part1 = new byte[20];
        byte[] part2 = new byte[20];
        byte[] u = new byte[20];
        int n = (this.L - 1) / 160;
        byte[] w = new byte[this.L / 8];
        if (!this.digest.getAlgorithmName().equals("SHA-1")) {
            throw new IllegalStateException("can only use SHA-1 for generating FIPS 186-2 parameters");
        }
        while (true) {
            this.random.nextBytes(seed);
            hash(this.digest, seed, part1);
            System.arraycopy(seed, 0, part2, 0, seed.length);
            inc(part2);
            hash(this.digest, part2, part2);
            for (int i = 0; i != u.length; i++) {
                u[i] = (byte) (part1[i] ^ part2[i]);
            }
            u[0] = (byte) (u[0] | (-128));
            u[19] = (byte) (u[19] | 1);
            BigInteger q = new BigInteger(1, u);
            if (q.isProbablePrime(this.certainty)) {
                byte[] offset = Arrays.clone(seed);
                inc(offset);
                for (int counter = 0; counter < 4096; counter++) {
                    for (int k = 0; k < n; k++) {
                        inc(offset);
                        hash(this.digest, offset, part1);
                        System.arraycopy(part1, 0, w, w.length - ((k + 1) * part1.length), part1.length);
                    }
                    inc(offset);
                    hash(this.digest, offset, part1);
                    System.arraycopy(part1, part1.length - (w.length - (part1.length * n)), w, 0, w.length - (part1.length * n));
                    w[0] = (byte) (w[0] | (-128));
                    BigInteger x = new BigInteger(1, w);
                    BigInteger c = x.mod(q.shiftLeft(1));
                    BigInteger p = x.subtract(c.subtract(ONE));
                    if (p.bitLength() == this.L && p.isProbablePrime(this.certainty)) {
                        BigInteger g = calculateGenerator_FIPS186_2(p, q, this.random);
                        return new DSAParameters(p, q, g, new DSAValidationParameters(seed, counter));
                    }
                }
            }
        }
    }

    private static BigInteger calculateGenerator_FIPS186_2(BigInteger p, BigInteger q, SecureRandom r) {
        BigInteger g;
        BigInteger e = p.subtract(ONE).divide(q);
        BigInteger pSub2 = p.subtract(TWO);
        do {
            BigInteger h = BigIntegers.createRandomInRange(TWO, pSub2, r);
            g = h.modPow(e, p);
        } while (g.bitLength() <= 1);
        return g;
    }

    private DSAParameters generateParameters_FIPS186_3() {
        BigInteger q;
        int counter;
        BigInteger p;
        BigInteger g;
        Digest d = this.digest;
        int outlen = d.getDigestSize() * 8;
        int seedlen = this.N;
        byte[] seed = new byte[seedlen / 8];
        int n = (this.L - 1) / outlen;
        int b = (this.L - 1) % outlen;
        byte[] output = new byte[d.getDigestSize()];
        loop0: while (true) {
            this.random.nextBytes(seed);
            hash(d, seed, output);
            BigInteger U = new BigInteger(1, output).mod(ONE.shiftLeft(this.N - 1));
            q = ONE.shiftLeft(this.N - 1).add(U).add(ONE).subtract(U.mod(TWO));
            if (q.isProbablePrime(this.certainty)) {
                byte[] offset = Arrays.clone(seed);
                int counterLimit = this.L * 4;
                counter = 0;
                while (counter < counterLimit) {
                    BigInteger W = ZERO;
                    int j = 0;
                    int exp = 0;
                    while (j <= n) {
                        inc(offset);
                        hash(d, offset, output);
                        BigInteger Vj = new BigInteger(1, output);
                        if (j == n) {
                            Vj = Vj.mod(ONE.shiftLeft(b));
                        }
                        W = W.add(Vj.shiftLeft(exp));
                        j++;
                        exp += outlen;
                    }
                    BigInteger X = W.add(ONE.shiftLeft(this.L - 1));
                    BigInteger c = X.mod(q.shiftLeft(1));
                    p = X.subtract(c.subtract(ONE));
                    if (p.bitLength() == this.L && p.isProbablePrime(this.certainty)) {
                        break loop0;
                    }
                    counter++;
                }
            }
        }
        if (this.usageIndex >= 0 && (g = calculateGenerator_FIPS186_3_Verifiable(d, p, q, seed, this.usageIndex)) != null) {
            return new DSAParameters(p, q, g, new DSAValidationParameters(seed, counter, this.usageIndex));
        }
        return new DSAParameters(p, q, calculateGenerator_FIPS186_3_Unverifiable(p, q, this.random), new DSAValidationParameters(seed, counter));
    }

    private static BigInteger calculateGenerator_FIPS186_3_Unverifiable(BigInteger p, BigInteger q, SecureRandom r) {
        return calculateGenerator_FIPS186_2(p, q, r);
    }

    private static BigInteger calculateGenerator_FIPS186_3_Verifiable(Digest d, BigInteger p, BigInteger q, byte[] seed, int index) {
        BigInteger e = p.subtract(ONE).divide(q);
        byte[] ggen = Hex.decode("6767656E");
        byte[] U = new byte[seed.length + ggen.length + 1 + 2];
        System.arraycopy(seed, 0, U, 0, seed.length);
        System.arraycopy(ggen, 0, U, seed.length, ggen.length);
        U[U.length - 3] = (byte) index;
        byte[] w = new byte[d.getDigestSize()];
        for (int count = 1; count < 65536; count++) {
            inc(U);
            hash(d, U, w);
            BigInteger W = new BigInteger(1, w);
            BigInteger g = W.modPow(e, p);
            if (g.compareTo(TWO) >= 0) {
                return g;
            }
        }
        return null;
    }

    private static void hash(Digest d, byte[] input, byte[] output) {
        d.update(input, 0, input.length);
        d.doFinal(output, 0);
    }

    private static int getDefaultN(int L) {
        return L > 1024 ? 256 : 160;
    }

    private static void inc(byte[] buf) {
        for (int i = buf.length - 1; i >= 0; i--) {
            byte b = (byte) ((buf[i] + 1) & 255);
            buf[i] = b;
            if (b != 0) {
                return;
            }
        }
    }
}
