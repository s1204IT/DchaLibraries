package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import sun.misc.Unsafe;

public class ThreadLocalRandom extends Random {
    static final String BAD_BOUND = "bound must be positive";
    static final String BAD_RANGE = "bound must be greater than origin";
    static final String BAD_SIZE = "size must be non-negative";
    private static final double DOUBLE_UNIT = 1.1102230246251565E-16d;
    private static final float FLOAT_UNIT = 5.9604645E-8f;
    private static final long GAMMA = -7046029254386353131L;
    private static final long PROBE;
    private static final int PROBE_INCREMENT = -1640531527;
    private static final long SECONDARY;
    private static final long SEED;
    private static final long SEEDER_INCREMENT = -4942790177534073029L;
    static final ThreadLocalRandom instance;
    private static final ThreadLocal<Double> nextLocalGaussian;
    private static final AtomicInteger probeGenerator;
    private static final AtomicLong seeder;
    private static final long serialVersionUID = -5851777807851030925L;
    boolean initialized = true;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("rnd", Long.TYPE), new ObjectStreamField("initialized", Boolean.TYPE)};
    private static final Unsafe U = Unsafe.getUnsafe();

    private static long mix64(long j) {
        long j2 = (j ^ (j >>> 33)) * (-49064778989728563L);
        long j3 = (j2 ^ (j2 >>> 33)) * (-4265267296055464877L);
        return j3 ^ (j3 >>> 33);
    }

    private static int mix32(long j) {
        long j2 = (j ^ (j >>> 33)) * (-49064778989728563L);
        return (int) (((j2 ^ (j2 >>> 33)) * (-4265267296055464877L)) >>> 32);
    }

    private ThreadLocalRandom() {
    }

    static final void localInit() {
        int iAddAndGet = probeGenerator.addAndGet(PROBE_INCREMENT);
        if (iAddAndGet == 0) {
            iAddAndGet = 1;
        }
        long jMix64 = mix64(seeder.getAndAdd(SEEDER_INCREMENT));
        Thread threadCurrentThread = Thread.currentThread();
        U.putLong(threadCurrentThread, SEED, jMix64);
        U.putInt(threadCurrentThread, PROBE, iAddAndGet);
    }

    public static ThreadLocalRandom current() {
        if (U.getInt(Thread.currentThread(), PROBE) == 0) {
            localInit();
        }
        return instance;
    }

    @Override
    public void setSeed(long j) {
        if (this.initialized) {
            throw new UnsupportedOperationException();
        }
    }

    final long nextSeed() {
        Unsafe unsafe = U;
        Thread threadCurrentThread = Thread.currentThread();
        long j = SEED;
        long j2 = GAMMA + U.getLong(threadCurrentThread, SEED);
        unsafe.putLong(threadCurrentThread, j, j2);
        return j2;
    }

    @Override
    protected int next(int i) {
        return (int) (mix64(nextSeed()) >>> (64 - i));
    }

    final long internalNextLong(long j, long j2) {
        long jMix64 = mix64(nextSeed());
        if (j < j2) {
            long j3 = j2 - j;
            long j4 = j3 - 1;
            if ((j3 & j4) == 0) {
                return (jMix64 & j4) + j;
            }
            if (j3 > 0) {
                while (true) {
                    long j5 = jMix64 >>> 1;
                    long j6 = j5 + j4;
                    long j7 = j5 % j3;
                    if (j6 - j7 < 0) {
                        jMix64 = mix64(nextSeed());
                    } else {
                        return j7 + j;
                    }
                }
            } else {
                while (true) {
                    if (jMix64 < j || jMix64 >= j2) {
                        jMix64 = mix64(nextSeed());
                    } else {
                        return jMix64;
                    }
                }
            }
        } else {
            return jMix64;
        }
    }

    final int internalNextInt(int i, int i2) {
        int iMix32 = mix32(nextSeed());
        if (i < i2) {
            int i3 = i2 - i;
            int i4 = i3 - 1;
            if ((i3 & i4) == 0) {
                return (iMix32 & i4) + i;
            }
            if (i3 > 0) {
                int iMix322 = iMix32 >>> 1;
                while (true) {
                    int i5 = iMix322 + i4;
                    int i6 = iMix322 % i3;
                    if (i5 - i6 < 0) {
                        iMix322 = mix32(nextSeed()) >>> 1;
                    } else {
                        return i6 + i;
                    }
                }
            } else {
                while (true) {
                    if (iMix32 < i || iMix32 >= i2) {
                        iMix32 = mix32(nextSeed());
                    } else {
                        return iMix32;
                    }
                }
            }
        } else {
            return iMix32;
        }
    }

    final double internalNextDouble(double d, double d2) {
        double dNextLong = (nextLong() >>> 11) * DOUBLE_UNIT;
        if (d < d2) {
            double d3 = (dNextLong * (d2 - d)) + d;
            if (d3 >= d2) {
                return Double.longBitsToDouble(Double.doubleToLongBits(d2) - 1);
            }
            return d3;
        }
        return dNextLong;
    }

    @Override
    public int nextInt() {
        return mix32(nextSeed());
    }

    @Override
    public int nextInt(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        int iMix32 = mix32(nextSeed());
        int i2 = i - 1;
        if ((i & i2) == 0) {
            return iMix32 & i2;
        }
        while (true) {
            int i3 = iMix32 >>> 1;
            int i4 = i3 + i2;
            int i5 = i3 % i;
            if (i4 - i5 < 0) {
                iMix32 = mix32(nextSeed());
            } else {
                return i5;
            }
        }
    }

    public int nextInt(int i, int i2) {
        if (i >= i2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextInt(i, i2);
    }

    @Override
    public long nextLong() {
        return mix64(nextSeed());
    }

    public long nextLong(long j) {
        if (j <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        long jMix64 = mix64(nextSeed());
        long j2 = j - 1;
        if ((j & j2) == 0) {
            return jMix64 & j2;
        }
        while (true) {
            long j3 = jMix64 >>> 1;
            long j4 = j3 + j2;
            long j5 = j3 % j;
            if (j4 - j5 < 0) {
                jMix64 = mix64(nextSeed());
            } else {
                return j5;
            }
        }
    }

    public long nextLong(long j, long j2) {
        if (j >= j2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextLong(j, j2);
    }

    @Override
    public double nextDouble() {
        return (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT;
    }

    public double nextDouble(double d) {
        if (d <= 0.0d) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        double dMix64 = (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT * d;
        if (dMix64 < d) {
            return dMix64;
        }
        return Double.longBitsToDouble(Double.doubleToLongBits(d) - 1);
    }

    public double nextDouble(double d, double d2) {
        if (d >= d2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextDouble(d, d2);
    }

    @Override
    public boolean nextBoolean() {
        return mix32(nextSeed()) < 0;
    }

    @Override
    public float nextFloat() {
        return (mix32(nextSeed()) >>> 8) * FLOAT_UNIT;
    }

    @Override
    public double nextGaussian() {
        Double d = nextLocalGaussian.get();
        if (d != null) {
            nextLocalGaussian.set(null);
            return d.doubleValue();
        }
        while (true) {
            double dNextDouble = (nextDouble() * 2.0d) - 1.0d;
            double dNextDouble2 = (2.0d * nextDouble()) - 1.0d;
            double d2 = (dNextDouble * dNextDouble) + (dNextDouble2 * dNextDouble2);
            if (d2 < 1.0d && d2 != 0.0d) {
                double dSqrt = StrictMath.sqrt(((-2.0d) * StrictMath.log(d2)) / d2);
                nextLocalGaussian.set(new Double(dNextDouble2 * dSqrt));
                return dNextDouble * dSqrt;
            }
        }
    }

    @Override
    public IntStream ints(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, j, Integer.MAX_VALUE, 0), false);
    }

    @Override
    public IntStream ints() {
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0), false);
    }

    @Override
    public IntStream ints(long j, int i, int i2) {
        if (j < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if (i >= i2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, j, i, i2), false);
    }

    @Override
    public IntStream ints(int i, int i2) {
        if (i >= i2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, Long.MAX_VALUE, i, i2), false);
    }

    @Override
    public LongStream longs(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, j, Long.MAX_VALUE, 0L), false);
    }

    @Override
    public LongStream longs() {
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L), false);
    }

    @Override
    public LongStream longs(long j, long j2, long j3) {
        if (j < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if (j2 >= j3) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, j, j2, j3), false);
    }

    @Override
    public LongStream longs(long j, long j2) {
        if (j >= j2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, Long.MAX_VALUE, j, j2), false);
    }

    @Override
    public DoubleStream doubles(long j) {
        if (j < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, j, Double.MAX_VALUE, 0.0d), false);
    }

    @Override
    public DoubleStream doubles() {
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0d), false);
    }

    @Override
    public DoubleStream doubles(long j, double d, double d2) {
        if (j < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if (d >= d2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, j, d, d2), false);
    }

    @Override
    public DoubleStream doubles(double d, double d2) {
        if (d >= d2) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, Long.MAX_VALUE, d, d2), false);
    }

    private static final class RandomIntsSpliterator implements Spliterator.OfInt {
        final int bound;
        final long fence;
        long index;
        final int origin;

        RandomIntsSpliterator(long j, long j2, int i, int i2) {
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
            this.index = j2;
            return new RandomIntsSpliterator(j, j2, this.origin, this.bound);
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
                intConsumer.accept(ThreadLocalRandom.current().internalNextInt(this.origin, this.bound));
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
                int i = this.origin;
                int i2 = this.bound;
                ThreadLocalRandom threadLocalRandomCurrent = ThreadLocalRandom.current();
                do {
                    intConsumer.accept(threadLocalRandomCurrent.internalNextInt(i, i2));
                    j++;
                } while (j < j2);
            }
        }
    }

    private static final class RandomLongsSpliterator implements Spliterator.OfLong {
        final long bound;
        final long fence;
        long index;
        final long origin;

        RandomLongsSpliterator(long j, long j2, long j3, long j4) {
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
            this.index = j2;
            return new RandomLongsSpliterator(j, j2, this.origin, this.bound);
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
                longConsumer.accept(ThreadLocalRandom.current().internalNextLong(this.origin, this.bound));
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
                long j3 = this.origin;
                long j4 = this.bound;
                ThreadLocalRandom threadLocalRandomCurrent = ThreadLocalRandom.current();
                do {
                    longConsumer.accept(threadLocalRandomCurrent.internalNextLong(j3, j4));
                    j++;
                } while (j < j2);
            }
        }
    }

    private static final class RandomDoublesSpliterator implements Spliterator.OfDouble {
        final double bound;
        final long fence;
        long index;
        final double origin;

        RandomDoublesSpliterator(long j, long j2, double d, double d2) {
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
            this.index = j2;
            return new RandomDoublesSpliterator(j, j2, this.origin, this.bound);
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
                doubleConsumer.accept(ThreadLocalRandom.current().internalNextDouble(this.origin, this.bound));
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
                double d = this.origin;
                double d2 = this.bound;
                ThreadLocalRandom threadLocalRandomCurrent = ThreadLocalRandom.current();
                do {
                    doubleConsumer.accept(threadLocalRandomCurrent.internalNextDouble(d, d2));
                    j++;
                } while (j < j2);
            }
        }
    }

    static final int getProbe() {
        return U.getInt(Thread.currentThread(), PROBE);
    }

    static final int advanceProbe(int i) {
        int i2 = i ^ (i << 13);
        int i3 = i2 ^ (i2 >>> 17);
        int i4 = i3 ^ (i3 << 5);
        U.putInt(Thread.currentThread(), PROBE, i4);
        return i4;
    }

    static final int nextSecondarySeed() {
        int iMix32;
        Thread threadCurrentThread = Thread.currentThread();
        int i = U.getInt(threadCurrentThread, SECONDARY);
        if (i != 0) {
            int i2 = i ^ (i << 13);
            int i3 = i2 ^ (i2 >>> 17);
            iMix32 = i3 ^ (i3 << 5);
        } else {
            iMix32 = mix32(seeder.getAndAdd(SEEDER_INCREMENT));
            if (iMix32 == 0) {
                iMix32 = 1;
            }
        }
        U.putInt(threadCurrentThread, SECONDARY, iMix32);
        return iMix32;
    }

    static {
        try {
            SEED = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomSeed"));
            PROBE = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomSecondarySeed"));
            nextLocalGaussian = new ThreadLocal<>();
            probeGenerator = new AtomicInteger();
            instance = new ThreadLocalRandom();
            seeder = new AtomicLong(mix64(System.currentTimeMillis()) ^ mix64(System.nanoTime()));
            if (((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return Boolean.valueOf(Boolean.getBoolean("java.util.secureRandomSeed"));
                }
            })).booleanValue()) {
                byte[] seed = SecureRandom.getSeed(8);
                long j = ((long) seed[0]) & 255;
                for (int i = 1; i < 8; i++) {
                    j = (j << 8) | (((long) seed[i]) & 255);
                }
                seeder.set(j);
            }
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("rnd", U.getLong(Thread.currentThread(), SEED));
        putFieldPutFields.put("initialized", true);
        objectOutputStream.writeFields();
    }

    private Object readResolve() {
        return current();
    }
}
