package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import sun.misc.Unsafe;

public class Random implements Serializable {
    static final String BadBound = "bound must be positive";
    static final String BadRange = "bound must be greater than origin";
    static final String BadSize = "size must be non-negative";
    private static final double DOUBLE_UNIT = 1.1102230246251565E-16d;
    private static final long addend = 11;
    private static final long mask = 281474976710655L;
    private static final long multiplier = 25214903917L;
    private static final long seedOffset;
    static final long serialVersionUID = 3905348978240129619L;
    private boolean haveNextNextGaussian;
    private double nextNextGaussian;
    private final AtomicLong seed;
    private static final AtomicLong seedUniquifier = new AtomicLong(8682522807148012L);
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("seed", Long.TYPE), new ObjectStreamField("nextNextGaussian", Double.TYPE), new ObjectStreamField("haveNextNextGaussian", Boolean.TYPE)};
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    public Random() {
        this(seedUniquifier() ^ System.nanoTime());
    }

    private static long seedUniquifier() {
        long j;
        long j2;
        do {
            j = seedUniquifier.get();
            j2 = 181783497276652981L * j;
        } while (!seedUniquifier.compareAndSet(j, j2));
        return j2;
    }

    static {
        try {
            seedOffset = unsafe.objectFieldOffset(Random.class.getDeclaredField("seed"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public Random(long j) {
        this.haveNextNextGaussian = false;
        if (getClass() == Random.class) {
            this.seed = new AtomicLong(initialScramble(j));
        } else {
            this.seed = new AtomicLong();
            setSeed(j);
        }
    }

    private static long initialScramble(long j) {
        return (j ^ multiplier) & mask;
    }

    public synchronized void setSeed(long j) {
        this.seed.set(initialScramble(j));
        this.haveNextNextGaussian = false;
    }

    protected int next(int i) {
        long j;
        long j2;
        AtomicLong atomicLong = this.seed;
        do {
            j = atomicLong.get();
            j2 = ((multiplier * j) + addend) & mask;
        } while (!atomicLong.compareAndSet(j, j2));
        return (int) (j2 >>> (48 - i));
    }

    public void nextBytes(byte[] bArr) {
        int length = bArr.length;
        int i = 0;
        while (i < length) {
            int iNextInt = nextInt();
            int iMin = Math.min(length - i, 4);
            while (true) {
                int i2 = iMin - 1;
                if (iMin > 0) {
                    bArr[i] = (byte) iNextInt;
                    iNextInt >>= 8;
                    i++;
                    iMin = i2;
                }
            }
        }
    }

    final long internalNextLong(long j, long j2) {
        long jNextLong = nextLong();
        if (j < j2) {
            long j3 = j2 - j;
            long j4 = j3 - 1;
            if ((j3 & j4) == 0) {
                return (jNextLong & j4) + j;
            }
            if (j3 > 0) {
                while (true) {
                    long j5 = jNextLong >>> 1;
                    long j6 = j5 + j4;
                    long j7 = j5 % j3;
                    if (j6 - j7 < 0) {
                        jNextLong = nextLong();
                    } else {
                        return j7 + j;
                    }
                }
            } else {
                while (true) {
                    if (jNextLong < j || jNextLong >= j2) {
                        jNextLong = nextLong();
                    } else {
                        return jNextLong;
                    }
                }
            }
        } else {
            return jNextLong;
        }
    }

    final int internalNextInt(int i, int i2) {
        if (i < i2) {
            int i3 = i2 - i;
            if (i3 > 0) {
                return nextInt(i3) + i;
            }
            while (true) {
                int iNextInt = nextInt();
                if (iNextInt >= i && iNextInt < i2) {
                    return iNextInt;
                }
            }
        } else {
            return nextInt();
        }
    }

    final double internalNextDouble(double d, double d2) {
        double dNextDouble = nextDouble();
        if (d < d2) {
            double d3 = (dNextDouble * (d2 - d)) + d;
            if (d3 >= d2) {
                return Double.longBitsToDouble(Double.doubleToLongBits(d2) - 1);
            }
            return d3;
        }
        return dNextDouble;
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException(BadBound);
        }
        int next = next(31);
        int i2 = i - 1;
        if ((i & i2) == 0) {
            return (int) ((((long) i) * ((long) next)) >> 31);
        }
        while (true) {
            int i3 = next % i;
            if ((next - i3) + i2 < 0) {
                next = next(31);
            } else {
                return i3;
            }
        }
    }

    public long nextLong() {
        return (((long) next(32)) << 32) + ((long) next(32));
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public float nextFloat() {
        return next(24) / 1.6777216E7f;
    }

    public double nextDouble() {
        return ((((long) next(26)) << 27) + ((long) next(27))) * DOUBLE_UNIT;
    }

    public synchronized double nextGaussian() {
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            return this.nextNextGaussian;
        }
        while (true) {
            double dNextDouble = (nextDouble() * 2.0d) - 1.0d;
            double dNextDouble2 = (2.0d * nextDouble()) - 1.0d;
            double d = (dNextDouble * dNextDouble) + (dNextDouble2 * dNextDouble2);
            if (d < 1.0d && d != 0.0d) {
                double dSqrt = StrictMath.sqrt(((-2.0d) * StrictMath.log(d)) / d);
                this.nextNextGaussian = dNextDouble2 * dSqrt;
                this.haveNextNextGaussian = true;
                return dNextDouble * dSqrt;
            }
        }
    }

    public IntStream ints(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(BadSize);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, j, Integer.MAX_VALUE, 0), false);
    }

    public IntStream ints() {
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0), false);
    }

    public IntStream ints(long j, int i, int i2) {
        if (j < 0) {
            throw new IllegalArgumentException(BadSize);
        }
        if (i >= i2) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, j, i, i2), false);
    }

    public IntStream ints(int i, int i2) {
        if (i >= i2) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, Long.MAX_VALUE, i, i2), false);
    }

    public LongStream longs(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(BadSize);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, j, Long.MAX_VALUE, 0L), false);
    }

    public LongStream longs() {
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L), false);
    }

    public LongStream longs(long j, long j2, long j3) {
        if (j < 0) {
            throw new IllegalArgumentException(BadSize);
        }
        if (j2 >= j3) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, j, j2, j3), false);
    }

    public LongStream longs(long j, long j2) {
        if (j >= j2) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, Long.MAX_VALUE, j, j2), false);
    }

    public DoubleStream doubles(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(BadSize);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, j, Double.MAX_VALUE, 0.0d), false);
    }

    public DoubleStream doubles() {
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0d), false);
    }

    public DoubleStream doubles(long j, double d, double d2) {
        if (j < 0) {
            throw new IllegalArgumentException(BadSize);
        }
        if (d >= d2) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, j, d, d2), false);
    }

    public DoubleStream doubles(double d, double d2) {
        if (d >= d2) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, Long.MAX_VALUE, d, d2), false);
    }

    static final class RandomIntsSpliterator implements Spliterator.OfInt {
        final int bound;
        final long fence;
        long index;
        final int origin;
        final Random rng;

        RandomIntsSpliterator(Random random, long j, long j2, int i, int i2) {
            this.rng = random;
            this.index = j;
            this.fence = j2;
            this.origin = i;
            this.bound = i2;
        }

        @Override
        public RandomIntsSpliterator trySplit() {
            long j = this.index;
            long j2 = (this.fence + j) >>> 1;
            if (j2 <= j) {
                return null;
            }
            Random random = this.rng;
            this.index = j2;
            return new RandomIntsSpliterator(random, j, j2, this.origin, this.bound);
        }

        @Override
        public long estimateSize() {
            return this.fence - this.index;
        }

        @Override
        public int characteristics() {
            return 17728;
        }

        @Override
        public boolean tryAdvance(IntConsumer intConsumer) {
            if (intConsumer == null) {
                throw new NullPointerException();
            }
            long j = this.index;
            if (j < this.fence) {
                intConsumer.accept(this.rng.internalNextInt(this.origin, this.bound));
                this.index = j + 1;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer intConsumer) {
            if (intConsumer == null) {
                throw new NullPointerException();
            }
            long j = this.index;
            long j2 = this.fence;
            if (j < j2) {
                this.index = j2;
                Random random = this.rng;
                int i = this.origin;
                int i2 = this.bound;
                do {
                    intConsumer.accept(random.internalNextInt(i, i2));
                    j++;
                } while (j < j2);
            }
        }
    }

    static final class RandomLongsSpliterator implements Spliterator.OfLong {
        final long bound;
        final long fence;
        long index;
        final long origin;
        final Random rng;

        RandomLongsSpliterator(Random random, long j, long j2, long j3, long j4) {
            this.rng = random;
            this.index = j;
            this.fence = j2;
            this.origin = j3;
            this.bound = j4;
        }

        @Override
        public RandomLongsSpliterator trySplit() {
            long j = this.index;
            long j2 = (this.fence + j) >>> 1;
            if (j2 <= j) {
                return null;
            }
            Random random = this.rng;
            this.index = j2;
            return new RandomLongsSpliterator(random, j, j2, this.origin, this.bound);
        }

        @Override
        public long estimateSize() {
            return this.fence - this.index;
        }

        @Override
        public int characteristics() {
            return 17728;
        }

        @Override
        public boolean tryAdvance(LongConsumer longConsumer) {
            if (longConsumer == null) {
                throw new NullPointerException();
            }
            long j = this.index;
            if (j < this.fence) {
                longConsumer.accept(this.rng.internalNextLong(this.origin, this.bound));
                this.index = j + 1;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(LongConsumer longConsumer) {
            if (longConsumer == null) {
                throw new NullPointerException();
            }
            long j = this.index;
            long j2 = this.fence;
            if (j < j2) {
                this.index = j2;
                Random random = this.rng;
                long j3 = this.origin;
                long j4 = this.bound;
                do {
                    longConsumer.accept(random.internalNextLong(j3, j4));
                    j++;
                } while (j < j2);
            }
        }
    }

    static final class RandomDoublesSpliterator implements Spliterator.OfDouble {
        final double bound;
        final long fence;
        long index;
        final double origin;
        final Random rng;

        RandomDoublesSpliterator(Random random, long j, long j2, double d, double d2) {
            this.rng = random;
            this.index = j;
            this.fence = j2;
            this.origin = d;
            this.bound = d2;
        }

        @Override
        public RandomDoublesSpliterator trySplit() {
            long j = this.index;
            long j2 = (this.fence + j) >>> 1;
            if (j2 <= j) {
                return null;
            }
            Random random = this.rng;
            this.index = j2;
            return new RandomDoublesSpliterator(random, j, j2, this.origin, this.bound);
        }

        @Override
        public long estimateSize() {
            return this.fence - this.index;
        }

        @Override
        public int characteristics() {
            return 17728;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer doubleConsumer) {
            if (doubleConsumer == null) {
                throw new NullPointerException();
            }
            long j = this.index;
            if (j < this.fence) {
                doubleConsumer.accept(this.rng.internalNextDouble(this.origin, this.bound));
                this.index = j + 1;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(DoubleConsumer doubleConsumer) {
            if (doubleConsumer == null) {
                throw new NullPointerException();
            }
            long j = this.index;
            long j2 = this.fence;
            if (j < j2) {
                this.index = j2;
                Random random = this.rng;
                double d = this.origin;
                double d2 = this.bound;
                do {
                    doubleConsumer.accept(random.internalNextDouble(d, d2));
                    j++;
                } while (j < j2);
            }
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        long j = fields.get("seed", -1L);
        if (j < 0) {
            throw new StreamCorruptedException("Random: invalid seed");
        }
        resetSeed(j);
        this.nextNextGaussian = fields.get("nextNextGaussian", 0.0d);
        this.haveNextNextGaussian = fields.get("haveNextNextGaussian", false);
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("seed", this.seed.get());
        putFieldPutFields.put("nextNextGaussian", this.nextNextGaussian);
        putFieldPutFields.put("haveNextNextGaussian", this.haveNextNextGaussian);
        objectOutputStream.writeFields();
    }

    private void resetSeed(long j) {
        unsafe.putObjectVolatile(this, seedOffset, new AtomicLong(j));
    }
}
