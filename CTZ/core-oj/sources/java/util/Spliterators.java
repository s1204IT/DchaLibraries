package java.util;

import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public final class Spliterators {
    private static final Spliterator<Object> EMPTY_SPLITERATOR = new EmptySpliterator.OfRef();
    private static final Spliterator.OfInt EMPTY_INT_SPLITERATOR = new EmptySpliterator.OfInt();
    private static final Spliterator.OfLong EMPTY_LONG_SPLITERATOR = new EmptySpliterator.OfLong();
    private static final Spliterator.OfDouble EMPTY_DOUBLE_SPLITERATOR = new EmptySpliterator.OfDouble();

    private Spliterators() {
    }

    public static <T> Spliterator<T> emptySpliterator() {
        return (Spliterator<T>) EMPTY_SPLITERATOR;
    }

    public static Spliterator.OfInt emptyIntSpliterator() {
        return EMPTY_INT_SPLITERATOR;
    }

    public static Spliterator.OfLong emptyLongSpliterator() {
        return EMPTY_LONG_SPLITERATOR;
    }

    public static Spliterator.OfDouble emptyDoubleSpliterator() {
        return EMPTY_DOUBLE_SPLITERATOR;
    }

    public static <T> Spliterator<T> spliterator(Object[] objArr, int i) {
        return new ArraySpliterator((Object[]) Objects.requireNonNull(objArr), i);
    }

    public static <T> Spliterator<T> spliterator(Object[] objArr, int i, int i2, int i3) {
        checkFromToBounds(((Object[]) Objects.requireNonNull(objArr)).length, i, i2);
        return new ArraySpliterator(objArr, i, i2, i3);
    }

    public static Spliterator.OfInt spliterator(int[] iArr, int i) {
        return new IntArraySpliterator((int[]) Objects.requireNonNull(iArr), i);
    }

    public static Spliterator.OfInt spliterator(int[] iArr, int i, int i2, int i3) {
        checkFromToBounds(((int[]) Objects.requireNonNull(iArr)).length, i, i2);
        return new IntArraySpliterator(iArr, i, i2, i3);
    }

    public static Spliterator.OfLong spliterator(long[] jArr, int i) {
        return new LongArraySpliterator((long[]) Objects.requireNonNull(jArr), i);
    }

    public static Spliterator.OfLong spliterator(long[] jArr, int i, int i2, int i3) {
        checkFromToBounds(((long[]) Objects.requireNonNull(jArr)).length, i, i2);
        return new LongArraySpliterator(jArr, i, i2, i3);
    }

    public static Spliterator.OfDouble spliterator(double[] dArr, int i) {
        return new DoubleArraySpliterator((double[]) Objects.requireNonNull(dArr), i);
    }

    public static Spliterator.OfDouble spliterator(double[] dArr, int i, int i2, int i3) {
        checkFromToBounds(((double[]) Objects.requireNonNull(dArr)).length, i, i2);
        return new DoubleArraySpliterator(dArr, i, i2, i3);
    }

    private static void checkFromToBounds(int i, int i2, int i3) {
        if (i2 > i3) {
            throw new ArrayIndexOutOfBoundsException("origin(" + i2 + ") > fence(" + i3 + ")");
        }
        if (i2 < 0) {
            throw new ArrayIndexOutOfBoundsException(i2);
        }
        if (i3 > i) {
            throw new ArrayIndexOutOfBoundsException(i3);
        }
    }

    public static <T> Spliterator<T> spliterator(Collection<? extends T> collection, int i) {
        return new IteratorSpliterator((Collection) Objects.requireNonNull(collection), i);
    }

    public static <T> Spliterator<T> spliterator(Iterator<? extends T> it, long j, int i) {
        return new IteratorSpliterator((Iterator) Objects.requireNonNull(it), j, i);
    }

    public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> it, int i) {
        return new IteratorSpliterator((Iterator) Objects.requireNonNull(it), i);
    }

    public static Spliterator.OfInt spliterator(PrimitiveIterator.OfInt ofInt, long j, int i) {
        return new IntIteratorSpliterator((PrimitiveIterator.OfInt) Objects.requireNonNull(ofInt), j, i);
    }

    public static Spliterator.OfInt spliteratorUnknownSize(PrimitiveIterator.OfInt ofInt, int i) {
        return new IntIteratorSpliterator((PrimitiveIterator.OfInt) Objects.requireNonNull(ofInt), i);
    }

    public static Spliterator.OfLong spliterator(PrimitiveIterator.OfLong ofLong, long j, int i) {
        return new LongIteratorSpliterator((PrimitiveIterator.OfLong) Objects.requireNonNull(ofLong), j, i);
    }

    public static Spliterator.OfLong spliteratorUnknownSize(PrimitiveIterator.OfLong ofLong, int i) {
        return new LongIteratorSpliterator((PrimitiveIterator.OfLong) Objects.requireNonNull(ofLong), i);
    }

    public static Spliterator.OfDouble spliterator(PrimitiveIterator.OfDouble ofDouble, long j, int i) {
        return new DoubleIteratorSpliterator((PrimitiveIterator.OfDouble) Objects.requireNonNull(ofDouble), j, i);
    }

    public static Spliterator.OfDouble spliteratorUnknownSize(PrimitiveIterator.OfDouble ofDouble, int i) {
        return new DoubleIteratorSpliterator((PrimitiveIterator.OfDouble) Objects.requireNonNull(ofDouble), i);
    }

    class C1Adapter<T> implements Iterator<T>, Consumer<T> {
        T nextElement;
        final Spliterator val$spliterator;
        boolean valueReady = false;

        C1Adapter(Spliterator spliterator) {
            this.val$spliterator = spliterator;
        }

        @Override
        public void accept(T t) {
            this.valueReady = true;
            this.nextElement = t;
        }

        @Override
        public boolean hasNext() {
            if (!this.valueReady) {
                this.val$spliterator.tryAdvance(this);
            }
            return this.valueReady;
        }

        @Override
        public T next() {
            if (!this.valueReady && !hasNext()) {
                throw new NoSuchElementException();
            }
            this.valueReady = false;
            return this.nextElement;
        }
    }

    public static <T> Iterator<T> iterator(Spliterator<? extends T> spliterator) {
        Objects.requireNonNull(spliterator);
        return new C1Adapter(spliterator);
    }

    class C2Adapter implements PrimitiveIterator.OfInt, IntConsumer {
        int nextElement;
        final Spliterator.OfInt val$spliterator;
        boolean valueReady = false;

        C2Adapter(Spliterator.OfInt ofInt) {
            this.val$spliterator = ofInt;
        }

        @Override
        public void accept(int i) {
            this.valueReady = true;
            this.nextElement = i;
        }

        @Override
        public boolean hasNext() {
            if (!this.valueReady) {
                this.val$spliterator.tryAdvance((IntConsumer) this);
            }
            return this.valueReady;
        }

        @Override
        public int nextInt() {
            if (!this.valueReady && !hasNext()) {
                throw new NoSuchElementException();
            }
            this.valueReady = false;
            return this.nextElement;
        }
    }

    public static PrimitiveIterator.OfInt iterator(Spliterator.OfInt ofInt) {
        Objects.requireNonNull(ofInt);
        return new C2Adapter(ofInt);
    }

    class C3Adapter implements PrimitiveIterator.OfLong, LongConsumer {
        long nextElement;
        final Spliterator.OfLong val$spliterator;
        boolean valueReady = false;

        C3Adapter(Spliterator.OfLong ofLong) {
            this.val$spliterator = ofLong;
        }

        @Override
        public void accept(long j) {
            this.valueReady = true;
            this.nextElement = j;
        }

        @Override
        public boolean hasNext() {
            if (!this.valueReady) {
                this.val$spliterator.tryAdvance((LongConsumer) this);
            }
            return this.valueReady;
        }

        @Override
        public long nextLong() {
            if (!this.valueReady && !hasNext()) {
                throw new NoSuchElementException();
            }
            this.valueReady = false;
            return this.nextElement;
        }
    }

    public static PrimitiveIterator.OfLong iterator(Spliterator.OfLong ofLong) {
        Objects.requireNonNull(ofLong);
        return new C3Adapter(ofLong);
    }

    class C4Adapter implements PrimitiveIterator.OfDouble, DoubleConsumer {
        double nextElement;
        final Spliterator.OfDouble val$spliterator;
        boolean valueReady = false;

        C4Adapter(Spliterator.OfDouble ofDouble) {
            this.val$spliterator = ofDouble;
        }

        @Override
        public void accept(double d) {
            this.valueReady = true;
            this.nextElement = d;
        }

        @Override
        public boolean hasNext() {
            if (!this.valueReady) {
                this.val$spliterator.tryAdvance((DoubleConsumer) this);
            }
            return this.valueReady;
        }

        @Override
        public double nextDouble() {
            if (!this.valueReady && !hasNext()) {
                throw new NoSuchElementException();
            }
            this.valueReady = false;
            return this.nextElement;
        }
    }

    public static PrimitiveIterator.OfDouble iterator(Spliterator.OfDouble ofDouble) {
        Objects.requireNonNull(ofDouble);
        return new C4Adapter(ofDouble);
    }

    private static abstract class EmptySpliterator<T, S extends Spliterator<T>, C> {
        EmptySpliterator() {
        }

        public S trySplit() {
            return null;
        }

        public boolean tryAdvance(C c) {
            Objects.requireNonNull(c);
            return false;
        }

        public void forEachRemaining(C c) {
            Objects.requireNonNull(c);
        }

        public long estimateSize() {
            return 0L;
        }

        public int characteristics() {
            return 16448;
        }

        private static final class OfRef<T> extends EmptySpliterator<T, Spliterator<T>, Consumer<? super T>> implements Spliterator<T> {
            @Override
            public void forEachRemaining(Consumer consumer) {
                super.forEachRemaining(consumer);
            }

            @Override
            public boolean tryAdvance(Consumer consumer) {
                return super.tryAdvance(consumer);
            }

            OfRef() {
            }
        }

        private static final class OfInt extends EmptySpliterator<Integer, Spliterator.OfInt, IntConsumer> implements Spliterator.OfInt {
            @Override
            public void forEachRemaining(IntConsumer intConsumer) {
                super.forEachRemaining(intConsumer);
            }

            @Override
            public boolean tryAdvance(IntConsumer intConsumer) {
                return super.tryAdvance(intConsumer);
            }

            @Override
            public Spliterator.OfInt trySplit() {
                return (Spliterator.OfInt) super.trySplit();
            }

            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            OfInt() {
            }
        }

        private static final class OfLong extends EmptySpliterator<Long, Spliterator.OfLong, LongConsumer> implements Spliterator.OfLong {
            @Override
            public void forEachRemaining(LongConsumer longConsumer) {
                super.forEachRemaining(longConsumer);
            }

            @Override
            public boolean tryAdvance(LongConsumer longConsumer) {
                return super.tryAdvance(longConsumer);
            }

            @Override
            public Spliterator.OfLong trySplit() {
                return (Spliterator.OfLong) super.trySplit();
            }

            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            OfLong() {
            }
        }

        private static final class OfDouble extends EmptySpliterator<Double, Spliterator.OfDouble, DoubleConsumer> implements Spliterator.OfDouble {
            @Override
            public void forEachRemaining(DoubleConsumer doubleConsumer) {
                super.forEachRemaining(doubleConsumer);
            }

            @Override
            public boolean tryAdvance(DoubleConsumer doubleConsumer) {
                return super.tryAdvance(doubleConsumer);
            }

            @Override
            public Spliterator.OfDouble trySplit() {
                return (Spliterator.OfDouble) super.trySplit();
            }

            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            OfDouble() {
            }
        }
    }

    static final class ArraySpliterator<T> implements Spliterator<T> {
        private final Object[] array;
        private final int characteristics;
        private final int fence;
        private int index;

        public ArraySpliterator(Object[] objArr, int i) {
            this(objArr, 0, objArr.length, i);
        }

        public ArraySpliterator(Object[] objArr, int i, int i2, int i3) {
            this.array = objArr;
            this.index = i;
            this.fence = i2;
            this.characteristics = i3 | 64 | 16384;
        }

        @Override
        public Spliterator<T> trySplit() {
            int i = this.index;
            int i2 = (this.fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            Object[] objArr = this.array;
            this.index = i2;
            return new ArraySpliterator(objArr, i, i2, this.characteristics);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            int i;
            if (consumer == null) {
                throw new NullPointerException();
            }
            Object[] objArr = this.array;
            int length = objArr.length;
            int i2 = this.fence;
            if (length < i2 || (i = this.index) < 0) {
                return;
            }
            this.index = i2;
            if (i < i2) {
                do {
                    consumer.accept(objArr[i]);
                    i++;
                } while (i < i2);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.index >= 0 && this.index < this.fence) {
                Object[] objArr = this.array;
                int i = this.index;
                this.index = i + 1;
                consumer.accept(objArr[i]);
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return this.fence - this.index;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super T> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }

    static final class IntArraySpliterator implements Spliterator.OfInt {
        private final int[] array;
        private final int characteristics;
        private final int fence;
        private int index;

        public IntArraySpliterator(int[] iArr, int i) {
            this(iArr, 0, iArr.length, i);
        }

        public IntArraySpliterator(int[] iArr, int i, int i2, int i3) {
            this.array = iArr;
            this.index = i;
            this.fence = i2;
            this.characteristics = i3 | 64 | 16384;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            int i = this.index;
            int i2 = (this.fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            int[] iArr = this.array;
            this.index = i2;
            return new IntArraySpliterator(iArr, i, i2, this.characteristics);
        }

        @Override
        public void forEachRemaining(IntConsumer intConsumer) {
            int i;
            if (intConsumer == null) {
                throw new NullPointerException();
            }
            int[] iArr = this.array;
            int length = iArr.length;
            int i2 = this.fence;
            if (length < i2 || (i = this.index) < 0) {
                return;
            }
            this.index = i2;
            if (i < i2) {
                do {
                    intConsumer.accept(iArr[i]);
                    i++;
                } while (i < i2);
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer intConsumer) {
            if (intConsumer == null) {
                throw new NullPointerException();
            }
            if (this.index >= 0 && this.index < this.fence) {
                int[] iArr = this.array;
                int i = this.index;
                this.index = i + 1;
                intConsumer.accept(iArr[i]);
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return this.fence - this.index;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }

    static final class LongArraySpliterator implements Spliterator.OfLong {
        private final long[] array;
        private final int characteristics;
        private final int fence;
        private int index;

        public LongArraySpliterator(long[] jArr, int i) {
            this(jArr, 0, jArr.length, i);
        }

        public LongArraySpliterator(long[] jArr, int i, int i2, int i3) {
            this.array = jArr;
            this.index = i;
            this.fence = i2;
            this.characteristics = i3 | 64 | 16384;
        }

        @Override
        public Spliterator.OfLong trySplit() {
            int i = this.index;
            int i2 = (this.fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            long[] jArr = this.array;
            this.index = i2;
            return new LongArraySpliterator(jArr, i, i2, this.characteristics);
        }

        @Override
        public void forEachRemaining(LongConsumer longConsumer) {
            int i;
            if (longConsumer == null) {
                throw new NullPointerException();
            }
            long[] jArr = this.array;
            int length = jArr.length;
            int i2 = this.fence;
            if (length < i2 || (i = this.index) < 0) {
                return;
            }
            this.index = i2;
            if (i < i2) {
                do {
                    longConsumer.accept(jArr[i]);
                    i++;
                } while (i < i2);
            }
        }

        @Override
        public boolean tryAdvance(LongConsumer longConsumer) {
            if (longConsumer == null) {
                throw new NullPointerException();
            }
            if (this.index >= 0 && this.index < this.fence) {
                long[] jArr = this.array;
                int i = this.index;
                this.index = i + 1;
                longConsumer.accept(jArr[i]);
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return this.fence - this.index;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }

    static final class DoubleArraySpliterator implements Spliterator.OfDouble {
        private final double[] array;
        private final int characteristics;
        private final int fence;
        private int index;

        public DoubleArraySpliterator(double[] dArr, int i) {
            this(dArr, 0, dArr.length, i);
        }

        public DoubleArraySpliterator(double[] dArr, int i, int i2, int i3) {
            this.array = dArr;
            this.index = i;
            this.fence = i2;
            this.characteristics = i3 | 64 | 16384;
        }

        @Override
        public Spliterator.OfDouble trySplit() {
            int i = this.index;
            int i2 = (this.fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            double[] dArr = this.array;
            this.index = i2;
            return new DoubleArraySpliterator(dArr, i, i2, this.characteristics);
        }

        @Override
        public void forEachRemaining(DoubleConsumer doubleConsumer) {
            int i;
            if (doubleConsumer == null) {
                throw new NullPointerException();
            }
            double[] dArr = this.array;
            int length = dArr.length;
            int i2 = this.fence;
            if (length < i2 || (i = this.index) < 0) {
                return;
            }
            this.index = i2;
            if (i < i2) {
                do {
                    doubleConsumer.accept(dArr[i]);
                    i++;
                } while (i < i2);
            }
        }

        @Override
        public boolean tryAdvance(DoubleConsumer doubleConsumer) {
            if (doubleConsumer == null) {
                throw new NullPointerException();
            }
            if (this.index >= 0 && this.index < this.fence) {
                double[] dArr = this.array;
                int i = this.index;
                this.index = i + 1;
                doubleConsumer.accept(dArr[i]);
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return this.fence - this.index;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super Double> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }

    public static abstract class AbstractSpliterator<T> implements Spliterator<T> {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private long est;

        protected AbstractSpliterator(long j, int i) {
            this.est = j;
            this.characteristics = (i & 64) != 0 ? i | 16384 : i;
        }

        static final class HoldingConsumer<T> implements Consumer<T> {
            Object value;

            HoldingConsumer() {
            }

            @Override
            public void accept(T t) {
                this.value = t;
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            HoldingConsumer holdingConsumer = new HoldingConsumer();
            long j = this.est;
            if (j > 1 && tryAdvance(holdingConsumer)) {
                int i = this.batch + 1024;
                if (i > j) {
                    i = (int) j;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                Object[] objArr = new Object[i2];
                int i3 = 0;
                do {
                    objArr[i3] = holdingConsumer.value;
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (tryAdvance(holdingConsumer));
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new ArraySpliterator(objArr, 0, i3, characteristics());
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }
    }

    public static abstract class AbstractIntSpliterator implements Spliterator.OfInt {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private long est;

        protected AbstractIntSpliterator(long j, int i) {
            this.est = j;
            this.characteristics = (i & 64) != 0 ? i | 16384 : i;
        }

        static final class HoldingIntConsumer implements IntConsumer {
            int value;

            HoldingIntConsumer() {
            }

            @Override
            public void accept(int i) {
                this.value = i;
            }
        }

        @Override
        public Spliterator.OfInt trySplit() {
            HoldingIntConsumer holdingIntConsumer = new HoldingIntConsumer();
            long j = this.est;
            if (j > 1 && tryAdvance((IntConsumer) holdingIntConsumer)) {
                int i = this.batch + 1024;
                if (i > j) {
                    i = (int) j;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                int[] iArr = new int[i2];
                int i3 = 0;
                do {
                    iArr[i3] = holdingIntConsumer.value;
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (tryAdvance((IntConsumer) holdingIntConsumer));
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new IntArraySpliterator(iArr, 0, i3, characteristics());
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }
    }

    public static abstract class AbstractLongSpliterator implements Spliterator.OfLong {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private long est;

        protected AbstractLongSpliterator(long j, int i) {
            this.est = j;
            this.characteristics = (i & 64) != 0 ? i | 16384 : i;
        }

        static final class HoldingLongConsumer implements LongConsumer {
            long value;

            HoldingLongConsumer() {
            }

            @Override
            public void accept(long j) {
                this.value = j;
            }
        }

        @Override
        public Spliterator.OfLong trySplit() {
            HoldingLongConsumer holdingLongConsumer = new HoldingLongConsumer();
            long j = this.est;
            if (j > 1 && tryAdvance((LongConsumer) holdingLongConsumer)) {
                int i = this.batch + 1024;
                if (i > j) {
                    i = (int) j;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                long[] jArr = new long[i2];
                int i3 = 0;
                do {
                    jArr[i3] = holdingLongConsumer.value;
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (tryAdvance((LongConsumer) holdingLongConsumer));
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new LongArraySpliterator(jArr, 0, i3, characteristics());
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }
    }

    public static abstract class AbstractDoubleSpliterator implements Spliterator.OfDouble {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private long est;

        protected AbstractDoubleSpliterator(long j, int i) {
            this.est = j;
            this.characteristics = (i & 64) != 0 ? i | 16384 : i;
        }

        static final class HoldingDoubleConsumer implements DoubleConsumer {
            double value;

            HoldingDoubleConsumer() {
            }

            @Override
            public void accept(double d) {
                this.value = d;
            }
        }

        @Override
        public Spliterator.OfDouble trySplit() {
            HoldingDoubleConsumer holdingDoubleConsumer = new HoldingDoubleConsumer();
            long j = this.est;
            if (j > 1 && tryAdvance((DoubleConsumer) holdingDoubleConsumer)) {
                int i = this.batch + 1024;
                if (i > j) {
                    i = (int) j;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                double[] dArr = new double[i2];
                int i3 = 0;
                do {
                    dArr[i3] = holdingDoubleConsumer.value;
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (tryAdvance((DoubleConsumer) holdingDoubleConsumer));
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new DoubleArraySpliterator(dArr, 0, i3, characteristics());
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }
    }

    static class IteratorSpliterator<T> implements Spliterator<T> {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private final Collection<? extends T> collection;
        private long est;
        private Iterator<? extends T> it;

        public IteratorSpliterator(Collection<? extends T> collection, int i) {
            this.collection = collection;
            this.it = null;
            this.characteristics = (i & 4096) == 0 ? i | 64 | 16384 : i;
        }

        public IteratorSpliterator(Iterator<? extends T> it, long j, int i) {
            this.collection = null;
            this.it = it;
            this.est = j;
            this.characteristics = (i & 4096) == 0 ? i | 64 | 16384 : i;
        }

        public IteratorSpliterator(Iterator<? extends T> it, int i) {
            this.collection = null;
            this.it = it;
            this.est = Long.MAX_VALUE;
            this.characteristics = i & (-16449);
        }

        @Override
        public Spliterator<T> trySplit() {
            long size;
            Iterator<? extends T> it = this.it;
            if (it == null) {
                it = this.collection.iterator();
                this.it = it;
                size = this.collection.size();
                this.est = size;
            } else {
                size = this.est;
            }
            if (size > 1 && it.hasNext()) {
                int i = this.batch + 1024;
                if (i > size) {
                    i = (int) size;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                Object[] objArr = new Object[i2];
                int i3 = 0;
                do {
                    objArr[i3] = it.next();
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (it.hasNext());
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new ArraySpliterator(objArr, 0, i3, this.characteristics);
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Iterator<? extends T> it = this.it;
            if (it == null) {
                it = this.collection.iterator();
                this.it = it;
                this.est = this.collection.size();
            }
            it.forEachRemaining(consumer);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.it == null) {
                this.it = this.collection.iterator();
                this.est = this.collection.size();
            }
            if (this.it.hasNext()) {
                consumer.accept(this.it.next());
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            if (this.it == null) {
                this.it = this.collection.iterator();
                long size = this.collection.size();
                this.est = size;
                return size;
            }
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super T> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }

    static final class IntIteratorSpliterator implements Spliterator.OfInt {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private long est;
        private PrimitiveIterator.OfInt it;

        public IntIteratorSpliterator(PrimitiveIterator.OfInt ofInt, long j, int i) {
            this.it = ofInt;
            this.est = j;
            this.characteristics = (i & 4096) == 0 ? i | 64 | 16384 : i;
        }

        public IntIteratorSpliterator(PrimitiveIterator.OfInt ofInt, int i) {
            this.it = ofInt;
            this.est = Long.MAX_VALUE;
            this.characteristics = i & (-16449);
        }

        @Override
        public Spliterator.OfInt trySplit() {
            PrimitiveIterator.OfInt ofInt = this.it;
            long j = this.est;
            if (j > 1 && ofInt.hasNext()) {
                int i = this.batch + 1024;
                if (i > j) {
                    i = (int) j;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                int[] iArr = new int[i2];
                int i3 = 0;
                do {
                    iArr[i3] = ofInt.nextInt();
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (ofInt.hasNext());
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new IntArraySpliterator(iArr, 0, i3, this.characteristics);
            }
            return null;
        }

        @Override
        public void forEachRemaining(IntConsumer intConsumer) {
            if (intConsumer == null) {
                throw new NullPointerException();
            }
            this.it.forEachRemaining(intConsumer);
        }

        @Override
        public boolean tryAdvance(IntConsumer intConsumer) {
            if (intConsumer == null) {
                throw new NullPointerException();
            }
            if (this.it.hasNext()) {
                intConsumer.accept(this.it.nextInt());
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }

    static final class LongIteratorSpliterator implements Spliterator.OfLong {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private long est;
        private PrimitiveIterator.OfLong it;

        public LongIteratorSpliterator(PrimitiveIterator.OfLong ofLong, long j, int i) {
            this.it = ofLong;
            this.est = j;
            this.characteristics = (i & 4096) == 0 ? i | 64 | 16384 : i;
        }

        public LongIteratorSpliterator(PrimitiveIterator.OfLong ofLong, int i) {
            this.it = ofLong;
            this.est = Long.MAX_VALUE;
            this.characteristics = i & (-16449);
        }

        @Override
        public Spliterator.OfLong trySplit() {
            PrimitiveIterator.OfLong ofLong = this.it;
            long j = this.est;
            if (j > 1 && ofLong.hasNext()) {
                int i = this.batch + 1024;
                if (i > j) {
                    i = (int) j;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                long[] jArr = new long[i2];
                int i3 = 0;
                do {
                    jArr[i3] = ofLong.nextLong();
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (ofLong.hasNext());
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new LongArraySpliterator(jArr, 0, i3, this.characteristics);
            }
            return null;
        }

        @Override
        public void forEachRemaining(LongConsumer longConsumer) {
            if (longConsumer == null) {
                throw new NullPointerException();
            }
            this.it.forEachRemaining(longConsumer);
        }

        @Override
        public boolean tryAdvance(LongConsumer longConsumer) {
            if (longConsumer == null) {
                throw new NullPointerException();
            }
            if (this.it.hasNext()) {
                longConsumer.accept(this.it.nextLong());
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }

    static final class DoubleIteratorSpliterator implements Spliterator.OfDouble {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        private int batch;
        private final int characteristics;
        private long est;
        private PrimitiveIterator.OfDouble it;

        public DoubleIteratorSpliterator(PrimitiveIterator.OfDouble ofDouble, long j, int i) {
            this.it = ofDouble;
            this.est = j;
            this.characteristics = (i & 4096) == 0 ? i | 64 | 16384 : i;
        }

        public DoubleIteratorSpliterator(PrimitiveIterator.OfDouble ofDouble, int i) {
            this.it = ofDouble;
            this.est = Long.MAX_VALUE;
            this.characteristics = i & (-16449);
        }

        @Override
        public Spliterator.OfDouble trySplit() {
            PrimitiveIterator.OfDouble ofDouble = this.it;
            long j = this.est;
            if (j > 1 && ofDouble.hasNext()) {
                int i = this.batch + 1024;
                if (i > j) {
                    i = (int) j;
                }
                int i2 = MAX_BATCH;
                if (i <= MAX_BATCH) {
                    i2 = i;
                }
                double[] dArr = new double[i2];
                int i3 = 0;
                do {
                    dArr[i3] = ofDouble.nextDouble();
                    i3++;
                    if (i3 >= i2) {
                        break;
                    }
                } while (ofDouble.hasNext());
                this.batch = i3;
                if (this.est != Long.MAX_VALUE) {
                    this.est -= (long) i3;
                }
                return new DoubleArraySpliterator(dArr, 0, i3, this.characteristics);
            }
            return null;
        }

        @Override
        public void forEachRemaining(DoubleConsumer doubleConsumer) {
            if (doubleConsumer == null) {
                throw new NullPointerException();
            }
            this.it.forEachRemaining(doubleConsumer);
        }

        @Override
        public boolean tryAdvance(DoubleConsumer doubleConsumer) {
            if (doubleConsumer == null) {
                throw new NullPointerException();
            }
            if (this.it.hasNext()) {
                doubleConsumer.accept(this.it.nextDouble());
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return this.characteristics;
        }

        @Override
        public Comparator<? super Double> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }
}
