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

    private static long mix64(long z) {
        long z2 = ((z >>> 33) ^ z) * (-49064778989728563L);
        long z3 = ((z2 >>> 33) ^ z2) * (-4265267296055464877L);
        return (z3 >>> 33) ^ z3;
    }

    private static int mix32(long z) {
        long z2 = ((z >>> 33) ^ z) * (-49064778989728563L);
        return (int) ((((z2 >>> 33) ^ z2) * (-4265267296055464877L)) >>> 32);
    }

    private ThreadLocalRandom() {
    }

    static final void localInit() {
        int p = probeGenerator.addAndGet(PROBE_INCREMENT);
        int probe = p == 0 ? 1 : p;
        long seed = mix64(seeder.getAndAdd(SEEDER_INCREMENT));
        Thread t = Thread.currentThread();
        U.putLong(t, SEED, seed);
        U.putInt(t, PROBE, probe);
    }

    public static ThreadLocalRandom current() {
        if (U.getInt(Thread.currentThread(), PROBE) == 0) {
            localInit();
        }
        return instance;
    }

    @Override
    public void setSeed(long seed) {
        if (!this.initialized) {
        } else {
            throw new UnsupportedOperationException();
        }
    }

    final long nextSeed() {
        Unsafe unsafe = U;
        Thread t = Thread.currentThread();
        long j = SEED;
        long r = U.getLong(t, SEED) + GAMMA;
        unsafe.putLong(t, j, r);
        return r;
    }

    @Override
    protected int next(int bits) {
        return (int) (mix64(nextSeed()) >>> (64 - bits));
    }

    final long internalNextLong(long origin, long bound) {
        long r = mix64(nextSeed());
        if (origin < bound) {
            long n = bound - origin;
            long m = n - 1;
            if ((n & m) == 0) {
                return (r & m) + origin;
            }
            if (n > 0) {
                long u = r >>> 1;
                while (true) {
                    long r2 = u % n;
                    if ((u + m) - r2 < 0) {
                        u = mix64(nextSeed()) >>> 1;
                    } else {
                        return r2 + origin;
                    }
                }
            } else {
                while (true) {
                    if (r < origin || r >= bound) {
                        r = mix64(nextSeed());
                    } else {
                        return r;
                    }
                }
            }
        } else {
            return r;
        }
    }

    final int internalNextInt(int origin, int bound) {
        int r = mix32(nextSeed());
        if (origin < bound) {
            int n = bound - origin;
            int m = n - 1;
            if ((n & m) == 0) {
                return (r & m) + origin;
            }
            if (n > 0) {
                int u = r >>> 1;
                while (true) {
                    int r2 = u % n;
                    if ((u + m) - r2 < 0) {
                        u = mix32(nextSeed()) >>> 1;
                    } else {
                        return r2 + origin;
                    }
                }
            } else {
                while (true) {
                    if (r < origin || r >= bound) {
                        r = mix32(nextSeed());
                    } else {
                        return r;
                    }
                }
            }
        } else {
            return r;
        }
    }

    final double internalNextDouble(double origin, double bound) {
        double r = (nextLong() >>> 11) * DOUBLE_UNIT;
        if (origin < bound) {
            double r2 = ((bound - origin) * r) + origin;
            if (r2 >= bound) {
                return Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
            }
            return r2;
        }
        return r;
    }

    @Override
    public int nextInt() {
        return mix32(nextSeed());
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        int r = mix32(nextSeed());
        int m = bound - 1;
        if ((bound & m) == 0) {
            return r & m;
        }
        int u = r >>> 1;
        while (true) {
            int r2 = u % bound;
            if ((u + m) - r2 < 0) {
                u = mix32(nextSeed()) >>> 1;
            } else {
                return r2;
            }
        }
    }

    public int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextInt(origin, bound);
    }

    @Override
    public long nextLong() {
        return mix64(nextSeed());
    }

    public long nextLong(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        long r = mix64(nextSeed());
        long m = bound - 1;
        if ((bound & m) == 0) {
            return r & m;
        }
        long u = r >>> 1;
        while (true) {
            long r2 = u % bound;
            if ((u + m) - r2 < 0) {
                u = mix64(nextSeed()) >>> 1;
            } else {
                return r2;
            }
        }
    }

    public long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextLong(origin, bound);
    }

    @Override
    public double nextDouble() {
        return (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT;
    }

    public double nextDouble(double bound) {
        if (!(bound > 0.0d)) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        double result = (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT * bound;
        return result < bound ? result : Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
    }

    public double nextDouble(double origin, double bound) {
        if (!(origin < bound)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextDouble(origin, bound);
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
            double v1 = (nextDouble() * 2.0d) - 1.0d;
            double v2 = (nextDouble() * 2.0d) - 1.0d;
            double s = (v1 * v1) + (v2 * v2);
            if (s < 1.0d && s != 0.0d) {
                double multiplier = StrictMath.sqrt((StrictMath.log(s) * (-2.0d)) / s);
                nextLocalGaussian.set(new Double(v2 * multiplier));
                return v1 * multiplier;
            }
        }
    }

    @Override
    public IntStream ints(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, streamSize, Integer.MAX_VALUE, 0), false);
    }

    @Override
    public IntStream ints() {
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0), false);
    }

    @Override
    public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if (randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }

    @Override
    public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        if (randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }

    @Override
    public LongStream longs(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, streamSize, Long.MAX_VALUE, 0L), false);
    }

    @Override
    public LongStream longs() {
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L), false);
    }

    @Override
    public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if (randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }

    @Override
    public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        if (randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }

    @Override
    public DoubleStream doubles(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, streamSize, Double.MAX_VALUE, 0.0d), false);
    }

    @Override
    public DoubleStream doubles() {
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0d), false);
    }

    @Override
    public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if (!(randomNumberOrigin < randomNumberBound)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }

    @Override
    public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        if (!(randomNumberOrigin < randomNumberBound)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }

    private static final class RandomIntsSpliterator implements Spliterator.OfInt {
        final int bound;
        final long fence;
        long index;
        final int origin;

        RandomIntsSpliterator(long index, long fence, int origin, int bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }

        @Override
        public RandomIntsSpliterator trySplit() {
            long i = this.index;
            long m = (this.fence + i) >>> 1;
            if (m <= i) {
                return null;
            }
            this.index = m;
            return new RandomIntsSpliterator(i, m, this.origin, this.bound);
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
        public boolean tryAdvance(IntConsumer consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            long i = this.index;
            long f = this.fence;
            if (i < f) {
                consumer.accept(ThreadLocalRandom.current().internalNextInt(this.origin, this.bound));
                this.index = 1 + i;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            long i = this.index;
            long f = this.fence;
            if (i >= f) {
                return;
            }
            this.index = f;
            int o = this.origin;
            int b = this.bound;
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            do {
                consumer.accept(rng.internalNextInt(o, b));
                i++;
            } while (i < f);
        }
    }

    private static final class RandomLongsSpliterator implements Spliterator.OfLong {
        final long bound;
        final long fence;
        long index;
        final long origin;

        RandomLongsSpliterator(long index, long fence, long origin, long bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }

        @Override
        public RandomLongsSpliterator trySplit() {
            long i = this.index;
            long m = (this.fence + i) >>> 1;
            if (m <= i) {
                return null;
            }
            this.index = m;
            return new RandomLongsSpliterator(i, m, this.origin, this.bound);
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
        public boolean tryAdvance(LongConsumer consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            long i = this.index;
            long f = this.fence;
            if (i < f) {
                consumer.accept(ThreadLocalRandom.current().internalNextLong(this.origin, this.bound));
                this.index = 1 + i;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(LongConsumer consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            long i = this.index;
            long f = this.fence;
            if (i >= f) {
                return;
            }
            this.index = f;
            long o = this.origin;
            long b = this.bound;
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            do {
                consumer.accept(rng.internalNextLong(o, b));
                i++;
            } while (i < f);
        }
    }

    private static final class RandomDoublesSpliterator implements Spliterator.OfDouble {
        final double bound;
        final long fence;
        long index;
        final double origin;

        RandomDoublesSpliterator(long index, long fence, double origin, double bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }

        @Override
        public RandomDoublesSpliterator trySplit() {
            long i = this.index;
            long m = (this.fence + i) >>> 1;
            if (m <= i) {
                return null;
            }
            this.index = m;
            return new RandomDoublesSpliterator(i, m, this.origin, this.bound);
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
        public boolean tryAdvance(DoubleConsumer consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            long i = this.index;
            long f = this.fence;
            if (i < f) {
                consumer.accept(ThreadLocalRandom.current().internalNextDouble(this.origin, this.bound));
                this.index = 1 + i;
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(DoubleConsumer consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            long i = this.index;
            long f = this.fence;
            if (i >= f) {
                return;
            }
            this.index = f;
            double o = this.origin;
            double b = this.bound;
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            do {
                consumer.accept(rng.internalNextDouble(o, b));
                i++;
            } while (i < f);
        }
    }

    static final int getProbe() {
        return U.getInt(Thread.currentThread(), PROBE);
    }

    static final int advanceProbe(int probe) {
        int probe2 = probe ^ (probe << 13);
        int probe3 = probe2 ^ (probe2 >>> 17);
        int probe4 = probe3 ^ (probe3 << 5);
        U.putInt(Thread.currentThread(), PROBE, probe4);
        return probe4;
    }

    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        int r2 = U.getInt(t, SECONDARY);
        if (r2 != 0) {
            int r3 = r2 ^ (r2 << 13);
            int r4 = r3 ^ (r3 >>> 17);
            r = r4 ^ (r4 << 5);
        } else {
            r = mix32(seeder.getAndAdd(SEEDER_INCREMENT));
            if (r == 0) {
                r = 1;
            }
        }
        U.putInt(t, SECONDARY, r);
        return r;
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
            if (!((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return Boolean.valueOf(Boolean.getBoolean("java.util.secureRandomSeed"));
                }
            })).booleanValue()) {
                return;
            }
            byte[] seedBytes = SecureRandom.getSeed(8);
            long s = ((long) seedBytes[0]) & 255;
            for (int i = 1; i < 8; i++) {
                s = (s << 8) | (((long) seedBytes[i]) & 255);
            }
            seeder.set(s);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("rnd", U.getLong(Thread.currentThread(), SEED));
        fields.put("initialized", true);
        s.writeFields();
    }

    private Object readResolve() {
        return current();
    }
}
