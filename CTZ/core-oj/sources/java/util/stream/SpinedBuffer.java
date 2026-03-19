package java.util.stream;

import android.R;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;

public class SpinedBuffer<E> extends AbstractSpinedBuffer implements Consumer<E>, Iterable<E> {
    private static final int SPLITERATOR_CHARACTERISTICS = 16464;
    protected E[] curChunk;
    protected E[][] spine;

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    public SpinedBuffer(int i) {
        super(i);
        this.curChunk = (E[]) new Object[1 << this.initialChunkPower];
    }

    public SpinedBuffer() {
        this.curChunk = (E[]) new Object[1 << this.initialChunkPower];
    }

    protected long capacity() {
        if (this.spineIndex == 0) {
            return this.curChunk.length;
        }
        return this.priorElementCount[this.spineIndex] + ((long) this.spine[this.spineIndex].length);
    }

    private void inflateSpine() {
        if (this.spine == null) {
            this.spine = (E[][]) new Object[8][];
            this.priorElementCount = new long[8];
            this.spine[0] = this.curChunk;
        }
    }

    protected final void ensureCapacity(long j) {
        long jCapacity = capacity();
        if (j > jCapacity) {
            inflateSpine();
            int i = this.spineIndex;
            while (true) {
                i++;
                if (j > jCapacity) {
                    if (i >= this.spine.length) {
                        int length = this.spine.length * 2;
                        this.spine = (E[][]) ((Object[][]) Arrays.copyOf(this.spine, length));
                        this.priorElementCount = Arrays.copyOf(this.priorElementCount, length);
                    }
                    int iChunkSize = chunkSize(i);
                    ((E[][]) this.spine)[i] = new Object[iChunkSize];
                    int i2 = i - 1;
                    this.priorElementCount[i] = this.priorElementCount[i2] + ((long) this.spine[i2].length);
                    jCapacity += (long) iChunkSize;
                } else {
                    return;
                }
            }
        }
    }

    protected void increaseCapacity() {
        ensureCapacity(capacity() + 1);
    }

    public E get(long j) {
        if (this.spineIndex == 0) {
            if (j < this.elementIndex) {
                return this.curChunk[(int) j];
            }
            throw new IndexOutOfBoundsException(Long.toString(j));
        }
        if (j >= count()) {
            throw new IndexOutOfBoundsException(Long.toString(j));
        }
        for (int i = 0; i <= this.spineIndex; i++) {
            if (j < this.priorElementCount[i] + ((long) this.spine[i].length)) {
                return this.spine[i][(int) (j - this.priorElementCount[i])];
            }
        }
        throw new IndexOutOfBoundsException(Long.toString(j));
    }

    public void copyInto(E[] eArr, int i) {
        long j = i;
        long jCount = count() + j;
        if (jCount > eArr.length || jCount < j) {
            throw new IndexOutOfBoundsException("does not fit");
        }
        if (this.spineIndex == 0) {
            System.arraycopy(this.curChunk, 0, eArr, i, this.elementIndex);
            return;
        }
        int length = i;
        for (int i2 = 0; i2 < this.spineIndex; i2++) {
            System.arraycopy(this.spine[i2], 0, eArr, length, this.spine[i2].length);
            length += this.spine[i2].length;
        }
        if (this.elementIndex > 0) {
            System.arraycopy(this.curChunk, 0, eArr, length, this.elementIndex);
        }
    }

    public E[] asArray(IntFunction<E[]> intFunction) {
        long jCount = count();
        if (jCount >= 2147483639) {
            throw new IllegalArgumentException("Stream size exceeds max array size");
        }
        E[] eArrApply = intFunction.apply((int) jCount);
        copyInto(eArrApply, 0);
        return eArrApply;
    }

    @Override
    public void clear() {
        if (this.spine != null) {
            this.curChunk = this.spine[0];
            for (int i = 0; i < this.curChunk.length; i++) {
                this.curChunk[i] = null;
            }
            this.spine = null;
            this.priorElementCount = null;
        } else {
            for (int i2 = 0; i2 < this.elementIndex; i2++) {
                this.curChunk[i2] = null;
            }
        }
        this.elementIndex = 0;
        this.spineIndex = 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Spliterators.iterator(spliterator());
    }

    public void forEach(Consumer<? super E> consumer) {
        for (int i = 0; i < this.spineIndex; i++) {
            for (R.color colorVar : this.spine[i]) {
                consumer.accept(colorVar);
            }
        }
        for (int i2 = 0; i2 < this.elementIndex; i2++) {
            consumer.accept(this.curChunk[i2]);
        }
    }

    public void accept(E e) {
        if (this.elementIndex == this.curChunk.length) {
            inflateSpine();
            if (this.spineIndex + 1 >= this.spine.length || this.spine[this.spineIndex + 1] == null) {
                increaseCapacity();
            }
            this.elementIndex = 0;
            this.spineIndex++;
            this.curChunk = this.spine[this.spineIndex];
        }
        E[] eArr = this.curChunk;
        int i = this.elementIndex;
        this.elementIndex = i + 1;
        eArr[i] = e;
    }

    public String toString() {
        final ArrayList arrayList = new ArrayList();
        Objects.requireNonNull(arrayList);
        forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                arrayList.add(obj);
            }
        });
        return "SpinedBuffer:" + arrayList.toString();
    }

    class C1Splitr implements Spliterator<E> {
        static final boolean $assertionsDisabled = false;
        final int lastSpineElementFence;
        final int lastSpineIndex;
        E[] splChunk;
        int splElementIndex;
        int splSpineIndex;

        C1Splitr(int i, int i2, int i3, int i4) {
            this.splSpineIndex = i;
            this.lastSpineIndex = i2;
            this.splElementIndex = i3;
            this.lastSpineElementFence = i4;
            this.splChunk = SpinedBuffer.this.spine == null ? SpinedBuffer.this.curChunk : SpinedBuffer.this.spine[i];
        }

        @Override
        public long estimateSize() {
            if (this.splSpineIndex == this.lastSpineIndex) {
                return ((long) this.lastSpineElementFence) - ((long) this.splElementIndex);
            }
            return ((SpinedBuffer.this.priorElementCount[this.lastSpineIndex] + ((long) this.lastSpineElementFence)) - SpinedBuffer.this.priorElementCount[this.splSpineIndex]) - ((long) this.splElementIndex);
        }

        @Override
        public int characteristics() {
            return SpinedBuffer.SPLITERATOR_CHARACTERISTICS;
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            if (this.splSpineIndex >= this.lastSpineIndex && (this.splSpineIndex != this.lastSpineIndex || this.splElementIndex >= this.lastSpineElementFence)) {
                return false;
            }
            E[] eArr = this.splChunk;
            int i = this.splElementIndex;
            this.splElementIndex = i + 1;
            consumer.accept(eArr[i]);
            if (this.splElementIndex == this.splChunk.length) {
                this.splElementIndex = 0;
                this.splSpineIndex++;
                if (SpinedBuffer.this.spine != null && this.splSpineIndex <= this.lastSpineIndex) {
                    this.splChunk = SpinedBuffer.this.spine[this.splSpineIndex];
                }
            }
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            if (this.splSpineIndex < this.lastSpineIndex || (this.splSpineIndex == this.lastSpineIndex && this.splElementIndex < this.lastSpineElementFence)) {
                int i = this.splElementIndex;
                for (int i2 = this.splSpineIndex; i2 < this.lastSpineIndex; i2++) {
                    R.color[] colorVarArr = SpinedBuffer.this.spine[i2];
                    while (i < colorVarArr.length) {
                        consumer.accept(colorVarArr[i]);
                        i++;
                    }
                    i = 0;
                }
                E[] eArr = this.splSpineIndex == this.lastSpineIndex ? this.splChunk : (E[]) SpinedBuffer.this.spine[this.lastSpineIndex];
                int i3 = this.lastSpineElementFence;
                while (i < i3) {
                    consumer.accept(eArr[i]);
                    i++;
                }
                this.splSpineIndex = this.lastSpineIndex;
                this.splElementIndex = this.lastSpineElementFence;
            }
        }

        @Override
        public Spliterator<E> trySplit() {
            int i;
            if (this.splSpineIndex < this.lastSpineIndex) {
                C1Splitr c1Splitr = new C1Splitr(this.splSpineIndex, this.lastSpineIndex - 1, this.splElementIndex, SpinedBuffer.this.spine[this.lastSpineIndex - 1].length);
                this.splSpineIndex = this.lastSpineIndex;
                this.splElementIndex = 0;
                this.splChunk = SpinedBuffer.this.spine[this.splSpineIndex];
                return c1Splitr;
            }
            if (this.splSpineIndex != this.lastSpineIndex || (i = (this.lastSpineElementFence - this.splElementIndex) / 2) == 0) {
                return null;
            }
            Spliterator<E> spliterator = Arrays.spliterator(this.splChunk, this.splElementIndex, this.splElementIndex + i);
            this.splElementIndex += i;
            return spliterator;
        }
    }

    public Spliterator<E> spliterator() {
        return new C1Splitr(0, this.spineIndex, 0, this.elementIndex);
    }

    public static abstract class OfPrimitive<E, T_ARR, T_CONS> extends AbstractSpinedBuffer implements Iterable<E> {
        T_ARR curChunk;
        T_ARR[] spine;

        protected abstract void arrayForEach(T_ARR t_arr, int i, int i2, T_CONS t_cons);

        protected abstract int arrayLength(T_ARR t_arr);

        public abstract void forEach(Consumer<? super E> consumer);

        public abstract Iterator<E> iterator();

        public abstract T_ARR newArray(int i);

        protected abstract T_ARR[] newArrayArray(int i);

        @Override
        public long count() {
            return super.count();
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty();
        }

        OfPrimitive(int i) {
            super(i);
            this.curChunk = newArray(1 << this.initialChunkPower);
        }

        OfPrimitive() {
            this.curChunk = newArray(1 << this.initialChunkPower);
        }

        protected long capacity() {
            if (this.spineIndex == 0) {
                return arrayLength(this.curChunk);
            }
            return this.priorElementCount[this.spineIndex] + ((long) arrayLength(this.spine[this.spineIndex]));
        }

        private void inflateSpine() {
            if (this.spine == null) {
                this.spine = newArrayArray(8);
                this.priorElementCount = new long[8];
                this.spine[0] = this.curChunk;
            }
        }

        protected final void ensureCapacity(long j) {
            long jCapacity = capacity();
            if (j > jCapacity) {
                inflateSpine();
                int i = this.spineIndex;
                while (true) {
                    i++;
                    if (j > jCapacity) {
                        if (i >= this.spine.length) {
                            int length = this.spine.length * 2;
                            this.spine = (T_ARR[]) Arrays.copyOf(this.spine, length);
                            this.priorElementCount = Arrays.copyOf(this.priorElementCount, length);
                        }
                        int iChunkSize = chunkSize(i);
                        this.spine[i] = newArray(iChunkSize);
                        int i2 = i - 1;
                        this.priorElementCount[i] = this.priorElementCount[i2] + ((long) arrayLength(this.spine[i2]));
                        jCapacity += (long) iChunkSize;
                    } else {
                        return;
                    }
                }
            }
        }

        protected void increaseCapacity() {
            ensureCapacity(capacity() + 1);
        }

        protected int chunkFor(long j) {
            if (this.spineIndex == 0) {
                if (j < this.elementIndex) {
                    return 0;
                }
                throw new IndexOutOfBoundsException(Long.toString(j));
            }
            if (j >= count()) {
                throw new IndexOutOfBoundsException(Long.toString(j));
            }
            for (int i = 0; i <= this.spineIndex; i++) {
                if (j < this.priorElementCount[i] + ((long) arrayLength(this.spine[i]))) {
                    return i;
                }
            }
            throw new IndexOutOfBoundsException(Long.toString(j));
        }

        public void copyInto(T_ARR t_arr, int i) {
            long j = i;
            long jCount = count() + j;
            if (jCount > arrayLength(t_arr) || jCount < j) {
                throw new IndexOutOfBoundsException("does not fit");
            }
            if (this.spineIndex == 0) {
                System.arraycopy(this.curChunk, 0, t_arr, i, this.elementIndex);
                return;
            }
            int iArrayLength = i;
            for (int i2 = 0; i2 < this.spineIndex; i2++) {
                System.arraycopy(this.spine[i2], 0, t_arr, iArrayLength, arrayLength(this.spine[i2]));
                iArrayLength += arrayLength(this.spine[i2]);
            }
            if (this.elementIndex > 0) {
                System.arraycopy(this.curChunk, 0, t_arr, iArrayLength, this.elementIndex);
            }
        }

        public T_ARR asPrimitiveArray() {
            long jCount = count();
            if (jCount >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            T_ARR t_arrNewArray = newArray((int) jCount);
            copyInto(t_arrNewArray, 0);
            return t_arrNewArray;
        }

        protected void preAccept() {
            if (this.elementIndex == arrayLength(this.curChunk)) {
                inflateSpine();
                if (this.spineIndex + 1 >= this.spine.length || this.spine[this.spineIndex + 1] == null) {
                    increaseCapacity();
                }
                this.elementIndex = 0;
                this.spineIndex++;
                this.curChunk = this.spine[this.spineIndex];
            }
        }

        @Override
        public void clear() {
            if (this.spine != null) {
                this.curChunk = this.spine[0];
                this.spine = null;
                this.priorElementCount = null;
            }
            this.elementIndex = 0;
            this.spineIndex = 0;
        }

        public void forEach(T_CONS t_cons) {
            for (int i = 0; i < this.spineIndex; i++) {
                arrayForEach(this.spine[i], 0, arrayLength(this.spine[i]), t_cons);
            }
            arrayForEach(this.curChunk, 0, this.elementIndex, t_cons);
        }

        abstract class BaseSpliterator<T_SPLITR extends Spliterator.OfPrimitive<E, T_CONS, T_SPLITR>> implements Spliterator.OfPrimitive<E, T_CONS, T_SPLITR> {
            static final boolean $assertionsDisabled = false;
            final int lastSpineElementFence;
            final int lastSpineIndex;
            T_ARR splChunk;
            int splElementIndex;
            int splSpineIndex;

            abstract void arrayForOne(T_ARR t_arr, int i, T_CONS t_cons);

            abstract T_SPLITR arraySpliterator(T_ARR t_arr, int i, int i2);

            abstract T_SPLITR newSpliterator(int i, int i2, int i3, int i4);

            BaseSpliterator(int i, int i2, int i3, int i4) {
                this.splSpineIndex = i;
                this.lastSpineIndex = i2;
                this.splElementIndex = i3;
                this.lastSpineElementFence = i4;
                this.splChunk = OfPrimitive.this.spine == null ? OfPrimitive.this.curChunk : OfPrimitive.this.spine[i];
            }

            @Override
            public long estimateSize() {
                if (this.splSpineIndex == this.lastSpineIndex) {
                    return ((long) this.lastSpineElementFence) - ((long) this.splElementIndex);
                }
                return ((OfPrimitive.this.priorElementCount[this.lastSpineIndex] + ((long) this.lastSpineElementFence)) - OfPrimitive.this.priorElementCount[this.splSpineIndex]) - ((long) this.splElementIndex);
            }

            @Override
            public int characteristics() {
                return SpinedBuffer.SPLITERATOR_CHARACTERISTICS;
            }

            @Override
            public boolean tryAdvance(T_CONS t_cons) {
                Objects.requireNonNull(t_cons);
                if (this.splSpineIndex >= this.lastSpineIndex && (this.splSpineIndex != this.lastSpineIndex || this.splElementIndex >= this.lastSpineElementFence)) {
                    return false;
                }
                T_ARR t_arr = this.splChunk;
                int i = this.splElementIndex;
                this.splElementIndex = i + 1;
                arrayForOne(t_arr, i, t_cons);
                if (this.splElementIndex == OfPrimitive.this.arrayLength(this.splChunk)) {
                    this.splElementIndex = 0;
                    this.splSpineIndex++;
                    if (OfPrimitive.this.spine != null && this.splSpineIndex <= this.lastSpineIndex) {
                        this.splChunk = OfPrimitive.this.spine[this.splSpineIndex];
                    }
                }
                return true;
            }

            @Override
            public void forEachRemaining(T_CONS t_cons) {
                Objects.requireNonNull(t_cons);
                if (this.splSpineIndex < this.lastSpineIndex || (this.splSpineIndex == this.lastSpineIndex && this.splElementIndex < this.lastSpineElementFence)) {
                    int i = this.splElementIndex;
                    for (int i2 = this.splSpineIndex; i2 < this.lastSpineIndex; i2++) {
                        T_ARR t_arr = OfPrimitive.this.spine[i2];
                        OfPrimitive.this.arrayForEach(t_arr, i, OfPrimitive.this.arrayLength(t_arr), t_cons);
                        i = 0;
                    }
                    OfPrimitive.this.arrayForEach(this.splSpineIndex == this.lastSpineIndex ? this.splChunk : OfPrimitive.this.spine[this.lastSpineIndex], i, this.lastSpineElementFence, t_cons);
                    this.splSpineIndex = this.lastSpineIndex;
                    this.splElementIndex = this.lastSpineElementFence;
                }
            }

            @Override
            public T_SPLITR trySplit() {
                int i;
                if (this.splSpineIndex < this.lastSpineIndex) {
                    T_SPLITR t_splitr = (T_SPLITR) newSpliterator(this.splSpineIndex, this.lastSpineIndex - 1, this.splElementIndex, OfPrimitive.this.arrayLength(OfPrimitive.this.spine[this.lastSpineIndex - 1]));
                    this.splSpineIndex = this.lastSpineIndex;
                    this.splElementIndex = 0;
                    this.splChunk = OfPrimitive.this.spine[this.splSpineIndex];
                    return t_splitr;
                }
                if (this.splSpineIndex != this.lastSpineIndex || (i = (this.lastSpineElementFence - this.splElementIndex) / 2) == 0) {
                    return null;
                }
                T_SPLITR t_splitr2 = (T_SPLITR) arraySpliterator(this.splChunk, this.splElementIndex, i);
                this.splElementIndex += i;
                return t_splitr2;
            }
        }
    }

    public static class OfInt extends OfPrimitive<Integer, int[], IntConsumer> implements IntConsumer {
        public OfInt() {
        }

        public OfInt(int i) {
            super(i);
        }

        @Override
        public void forEach(Consumer<? super Integer> consumer) {
            if (consumer instanceof IntConsumer) {
                forEach((IntConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfInt.forEach(Consumer)");
            }
            spliterator().forEachRemaining(consumer);
        }

        @Override
        protected int[][] newArrayArray(int i) {
            return new int[i][];
        }

        @Override
        public int[] newArray(int i) {
            return new int[i];
        }

        @Override
        protected int arrayLength(int[] iArr) {
            return iArr.length;
        }

        @Override
        protected void arrayForEach(int[] iArr, int i, int i2, IntConsumer intConsumer) {
            while (i < i2) {
                intConsumer.accept(iArr[i]);
                i++;
            }
        }

        public void accept(int i) {
            preAccept();
            int[] iArr = (int[]) this.curChunk;
            int i2 = this.elementIndex;
            this.elementIndex = i2 + 1;
            iArr[i2] = i;
        }

        public int get(long j) {
            int iChunkFor = chunkFor(j);
            if (this.spineIndex == 0 && iChunkFor == 0) {
                return ((int[]) this.curChunk)[(int) j];
            }
            return ((int[][]) this.spine)[iChunkFor][(int) (j - this.priorElementCount[iChunkFor])];
        }

        @Override
        public PrimitiveIterator.OfInt iterator() {
            return Spliterators.iterator(spliterator());
        }

        class C1Splitr extends OfPrimitive<Integer, int[], IntConsumer>.BaseSpliterator<Spliterator.OfInt> implements Spliterator.OfInt {
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

            C1Splitr(int i, int i2, int i3, int i4) {
                super(i, i2, i3, i4);
            }

            @Override
            C1Splitr newSpliterator(int i, int i2, int i3, int i4) {
                return OfInt.this.new C1Splitr(i, i2, i3, i4);
            }

            @Override
            void arrayForOne(int[] iArr, int i, IntConsumer intConsumer) {
                intConsumer.accept(iArr[i]);
            }

            @Override
            Spliterator.OfInt arraySpliterator(int[] iArr, int i, int i2) {
                return Arrays.spliterator(iArr, i, i2 + i);
            }
        }

        @Override
        public Spliterator.OfInt spliterator() {
            return new C1Splitr(0, this.spineIndex, 0, this.elementIndex);
        }

        public String toString() {
            int[] iArrAsPrimitiveArray = asPrimitiveArray();
            if (iArrAsPrimitiveArray.length < 200) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), Integer.valueOf(iArrAsPrimitiveArray.length), Integer.valueOf(this.spineIndex), Arrays.toString(iArrAsPrimitiveArray));
            }
            return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), Integer.valueOf(iArrAsPrimitiveArray.length), Integer.valueOf(this.spineIndex), Arrays.toString(Arrays.copyOf(iArrAsPrimitiveArray, HttpURLConnection.HTTP_OK)));
        }
    }

    public static class OfLong extends OfPrimitive<Long, long[], LongConsumer> implements LongConsumer {
        public OfLong() {
        }

        public OfLong(int i) {
            super(i);
        }

        @Override
        public void forEach(Consumer<? super Long> consumer) {
            if (consumer instanceof LongConsumer) {
                forEach((LongConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfLong.forEach(Consumer)");
            }
            spliterator().forEachRemaining(consumer);
        }

        @Override
        protected long[][] newArrayArray(int i) {
            return new long[i][];
        }

        @Override
        public long[] newArray(int i) {
            return new long[i];
        }

        @Override
        protected int arrayLength(long[] jArr) {
            return jArr.length;
        }

        @Override
        protected void arrayForEach(long[] jArr, int i, int i2, LongConsumer longConsumer) {
            while (i < i2) {
                longConsumer.accept(jArr[i]);
                i++;
            }
        }

        public void accept(long j) {
            preAccept();
            long[] jArr = (long[]) this.curChunk;
            int i = this.elementIndex;
            this.elementIndex = i + 1;
            jArr[i] = j;
        }

        public long get(long j) {
            int iChunkFor = chunkFor(j);
            if (this.spineIndex == 0 && iChunkFor == 0) {
                return ((long[]) this.curChunk)[(int) j];
            }
            return ((long[][]) this.spine)[iChunkFor][(int) (j - this.priorElementCount[iChunkFor])];
        }

        @Override
        public PrimitiveIterator.OfLong iterator() {
            return Spliterators.iterator(spliterator());
        }

        class C1Splitr extends OfPrimitive<Long, long[], LongConsumer>.BaseSpliterator<Spliterator.OfLong> implements Spliterator.OfLong {
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

            C1Splitr(int i, int i2, int i3, int i4) {
                super(i, i2, i3, i4);
            }

            @Override
            C1Splitr newSpliterator(int i, int i2, int i3, int i4) {
                return OfLong.this.new C1Splitr(i, i2, i3, i4);
            }

            @Override
            void arrayForOne(long[] jArr, int i, LongConsumer longConsumer) {
                longConsumer.accept(jArr[i]);
            }

            @Override
            Spliterator.OfLong arraySpliterator(long[] jArr, int i, int i2) {
                return Arrays.spliterator(jArr, i, i2 + i);
            }
        }

        @Override
        public Spliterator.OfLong spliterator() {
            return new C1Splitr(0, this.spineIndex, 0, this.elementIndex);
        }

        public String toString() {
            long[] jArrAsPrimitiveArray = asPrimitiveArray();
            if (jArrAsPrimitiveArray.length < 200) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), Integer.valueOf(jArrAsPrimitiveArray.length), Integer.valueOf(this.spineIndex), Arrays.toString(jArrAsPrimitiveArray));
            }
            return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), Integer.valueOf(jArrAsPrimitiveArray.length), Integer.valueOf(this.spineIndex), Arrays.toString(Arrays.copyOf(jArrAsPrimitiveArray, HttpURLConnection.HTTP_OK)));
        }
    }

    public static class OfDouble extends OfPrimitive<Double, double[], DoubleConsumer> implements DoubleConsumer {
        public OfDouble() {
        }

        public OfDouble(int i) {
            super(i);
        }

        @Override
        public void forEach(Consumer<? super Double> consumer) {
            if (consumer instanceof DoubleConsumer) {
                forEach((DoubleConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfDouble.forEach(Consumer)");
            }
            spliterator().forEachRemaining(consumer);
        }

        @Override
        protected double[][] newArrayArray(int i) {
            return new double[i][];
        }

        @Override
        public double[] newArray(int i) {
            return new double[i];
        }

        @Override
        protected int arrayLength(double[] dArr) {
            return dArr.length;
        }

        @Override
        protected void arrayForEach(double[] dArr, int i, int i2, DoubleConsumer doubleConsumer) {
            while (i < i2) {
                doubleConsumer.accept(dArr[i]);
                i++;
            }
        }

        public void accept(double d) {
            preAccept();
            double[] dArr = (double[]) this.curChunk;
            int i = this.elementIndex;
            this.elementIndex = i + 1;
            dArr[i] = d;
        }

        public double get(long j) {
            int iChunkFor = chunkFor(j);
            if (this.spineIndex == 0 && iChunkFor == 0) {
                return ((double[]) this.curChunk)[(int) j];
            }
            return ((double[][]) this.spine)[iChunkFor][(int) (j - this.priorElementCount[iChunkFor])];
        }

        @Override
        public PrimitiveIterator.OfDouble iterator() {
            return Spliterators.iterator(spliterator());
        }

        class C1Splitr extends OfPrimitive<Double, double[], DoubleConsumer>.BaseSpliterator<Spliterator.OfDouble> implements Spliterator.OfDouble {
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

            C1Splitr(int i, int i2, int i3, int i4) {
                super(i, i2, i3, i4);
            }

            @Override
            C1Splitr newSpliterator(int i, int i2, int i3, int i4) {
                return OfDouble.this.new C1Splitr(i, i2, i3, i4);
            }

            @Override
            void arrayForOne(double[] dArr, int i, DoubleConsumer doubleConsumer) {
                doubleConsumer.accept(dArr[i]);
            }

            @Override
            Spliterator.OfDouble arraySpliterator(double[] dArr, int i, int i2) {
                return Arrays.spliterator(dArr, i, i2 + i);
            }
        }

        @Override
        public Spliterator.OfDouble spliterator() {
            return new C1Splitr(0, this.spineIndex, 0, this.elementIndex);
        }

        public String toString() {
            double[] dArrAsPrimitiveArray = asPrimitiveArray();
            if (dArrAsPrimitiveArray.length < 200) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), Integer.valueOf(dArrAsPrimitiveArray.length), Integer.valueOf(this.spineIndex), Arrays.toString(dArrAsPrimitiveArray));
            }
            return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), Integer.valueOf(dArrAsPrimitiveArray.length), Integer.valueOf(this.spineIndex), Arrays.toString(Arrays.copyOf(dArrAsPrimitiveArray, HttpURLConnection.HTTP_OK)));
        }
    }
}
