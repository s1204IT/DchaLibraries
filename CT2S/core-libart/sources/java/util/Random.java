package java.util;

import java.io.Serializable;

public class Random implements Serializable {
    private static final long multiplier = 25214903917L;
    private static volatile long seedBase = 0;
    private static final long serialVersionUID = 3905348978240129619L;
    private boolean haveNextNextGaussian;
    private double nextNextGaussian;
    private long seed;

    public Random() {
        setSeed(System.nanoTime() + seedBase);
        seedBase++;
    }

    public Random(long seed) {
        setSeed(seed);
    }

    protected synchronized int next(int bits) {
        this.seed = ((this.seed * multiplier) + 11) & 281474976710655L;
        return (int) (this.seed >>> (48 - bits));
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public void nextBytes(byte[] buf) {
        int rand = 0;
        int loop = 0;
        for (int count = 0; count < buf.length; count++) {
            if (loop == 0) {
                rand = nextInt();
                loop = 3;
            } else {
                loop--;
            }
            buf[count] = (byte) rand;
            rand >>= 8;
        }
    }

    public double nextDouble() {
        return ((((long) next(26)) << 27) + ((long) next(27))) / 9.007199254740992E15d;
    }

    public float nextFloat() {
        return next(24) / 1.6777216E7f;
    }

    public synchronized double nextGaussian() {
        double v1;
        double v2;
        double s;
        double d;
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            d = this.nextNextGaussian;
        } else {
            while (true) {
                v1 = (2.0d * nextDouble()) - 1.0d;
                v2 = (2.0d * nextDouble()) - 1.0d;
                s = (v1 * v1) + (v2 * v2);
                if (s < 1.0d && s != 0.0d) {
                    break;
                }
            }
            double multiplier2 = StrictMath.sqrt(((-2.0d) * StrictMath.log(s)) / s);
            this.nextNextGaussian = v2 * multiplier2;
            this.haveNextNextGaussian = true;
            d = v1 * multiplier2;
        }
        return d;
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int n) {
        int bits;
        int val;
        if (n <= 0) {
            throw new IllegalArgumentException("n <= 0: " + n);
        }
        if (((-n) & n) == n) {
            return (int) ((((long) n) * ((long) next(31))) >> 31);
        }
        do {
            bits = next(31);
            val = bits % n;
        } while ((bits - val) + (n - 1) < 0);
        return val;
    }

    public long nextLong() {
        return (((long) next(32)) << 32) + ((long) next(32));
    }

    public synchronized void setSeed(long seed) {
        this.seed = (multiplier ^ seed) & 281474976710655L;
        this.haveNextNextGaussian = false;
    }
}
