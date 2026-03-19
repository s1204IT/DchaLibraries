package java.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.DoublePipeline;
import java.util.stream.IntPipeline;
import java.util.stream.LongPipeline;
import java.util.stream.Node;
import java.util.stream.ReferencePipeline;
import java.util.stream.Sink;
import java.util.stream.SpinedBuffer;

final class SortedOps {
    private SortedOps() {
    }

    static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> abstractPipeline) {
        return new OfRef(abstractPipeline);
    }

    static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> abstractPipeline, Comparator<? super T> comparator) {
        return new OfRef(abstractPipeline, comparator);
    }

    static <T> IntStream makeInt(AbstractPipeline<?, Integer, ?> abstractPipeline) {
        return new OfInt(abstractPipeline);
    }

    static <T> LongStream makeLong(AbstractPipeline<?, Long, ?> abstractPipeline) {
        return new OfLong(abstractPipeline);
    }

    static <T> DoubleStream makeDouble(AbstractPipeline<?, Double, ?> abstractPipeline) {
        return new OfDouble(abstractPipeline);
    }

    private static final class OfRef<T> extends ReferencePipeline.StatefulOp<T, T> {
        private final Comparator<? super T> comparator;
        private final boolean isNaturalSort;

        OfRef(AbstractPipeline<?, T, ?> abstractPipeline) {
            super(abstractPipeline, StreamShape.REFERENCE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
            this.isNaturalSort = true;
            this.comparator = Comparator.naturalOrder();
        }

        OfRef(AbstractPipeline<?, T, ?> abstractPipeline, Comparator<? super T> comparator) {
            super(abstractPipeline, StreamShape.REFERENCE, StreamOpFlag.IS_ORDERED | StreamOpFlag.NOT_SORTED);
            this.isNaturalSort = false;
            this.comparator = (Comparator) Objects.requireNonNull(comparator);
        }

        @Override
        public Sink<T> opWrapSink(int i, Sink<T> sink) {
            Objects.requireNonNull(sink);
            if (StreamOpFlag.SORTED.isKnown(i) && this.isNaturalSort) {
                return sink;
            }
            if (StreamOpFlag.SIZED.isKnown(i)) {
                return new SizedRefSortingSink(sink, this.comparator);
            }
            return new RefSortingSink(sink, this.comparator);
        }

        @Override
        public <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<T[]> intFunction) {
            if (StreamOpFlag.SORTED.isKnown(pipelineHelper.getStreamAndOpFlags()) && this.isNaturalSort) {
                return pipelineHelper.evaluate(spliterator, false, intFunction);
            }
            T[] tArrAsArray = pipelineHelper.evaluate(spliterator, true, intFunction).asArray(intFunction);
            Arrays.parallelSort(tArrAsArray, this.comparator);
            return Nodes.node(tArrAsArray);
        }
    }

    private static final class OfInt extends IntPipeline.StatefulOp<Integer> {
        OfInt(AbstractPipeline<?, Integer, ?> abstractPipeline) {
            super(abstractPipeline, StreamShape.INT_VALUE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
        }

        @Override
        public Sink<Integer> opWrapSink(int i, Sink<Integer> sink) {
            Objects.requireNonNull(sink);
            if (StreamOpFlag.SORTED.isKnown(i)) {
                return sink;
            }
            if (StreamOpFlag.SIZED.isKnown(i)) {
                return new SizedIntSortingSink(sink);
            }
            return new IntSortingSink(sink);
        }

        @Override
        public <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> intFunction) {
            if (StreamOpFlag.SORTED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return pipelineHelper.evaluate(spliterator, false, intFunction);
            }
            int[] iArrAsPrimitiveArray = ((Node.OfInt) pipelineHelper.evaluate(spliterator, true, intFunction)).asPrimitiveArray();
            Arrays.parallelSort(iArrAsPrimitiveArray);
            return Nodes.node(iArrAsPrimitiveArray);
        }
    }

    private static final class OfLong extends LongPipeline.StatefulOp<Long> {
        OfLong(AbstractPipeline<?, Long, ?> abstractPipeline) {
            super(abstractPipeline, StreamShape.LONG_VALUE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
        }

        @Override
        public Sink<Long> opWrapSink(int i, Sink<Long> sink) {
            Objects.requireNonNull(sink);
            if (StreamOpFlag.SORTED.isKnown(i)) {
                return sink;
            }
            if (StreamOpFlag.SIZED.isKnown(i)) {
                return new SizedLongSortingSink(sink);
            }
            return new LongSortingSink(sink);
        }

        @Override
        public <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Long[]> intFunction) {
            if (StreamOpFlag.SORTED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return pipelineHelper.evaluate(spliterator, false, intFunction);
            }
            long[] jArrAsPrimitiveArray = ((Node.OfLong) pipelineHelper.evaluate(spliterator, true, intFunction)).asPrimitiveArray();
            Arrays.parallelSort(jArrAsPrimitiveArray);
            return Nodes.node(jArrAsPrimitiveArray);
        }
    }

    private static final class OfDouble extends DoublePipeline.StatefulOp<Double> {
        OfDouble(AbstractPipeline<?, Double, ?> abstractPipeline) {
            super(abstractPipeline, StreamShape.DOUBLE_VALUE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
        }

        @Override
        public Sink<Double> opWrapSink(int i, Sink<Double> sink) {
            Objects.requireNonNull(sink);
            if (StreamOpFlag.SORTED.isKnown(i)) {
                return sink;
            }
            if (StreamOpFlag.SIZED.isKnown(i)) {
                return new SizedDoubleSortingSink(sink);
            }
            return new DoubleSortingSink(sink);
        }

        @Override
        public <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Double[]> intFunction) {
            if (StreamOpFlag.SORTED.isKnown(pipelineHelper.getStreamAndOpFlags())) {
                return pipelineHelper.evaluate(spliterator, false, intFunction);
            }
            double[] dArrAsPrimitiveArray = ((Node.OfDouble) pipelineHelper.evaluate(spliterator, true, intFunction)).asPrimitiveArray();
            Arrays.parallelSort(dArrAsPrimitiveArray);
            return Nodes.node(dArrAsPrimitiveArray);
        }
    }

    private static abstract class AbstractRefSortingSink<T> extends Sink.ChainedReference<T, T> {
        protected boolean cancellationWasRequested;
        protected final Comparator<? super T> comparator;

        AbstractRefSortingSink(Sink<? super T> sink, Comparator<? super T> comparator) {
            super(sink);
            this.comparator = comparator;
        }

        @Override
        public final boolean cancellationRequested() {
            this.cancellationWasRequested = true;
            return false;
        }
    }

    private static final class SizedRefSortingSink<T> extends AbstractRefSortingSink<T> {
        private T[] array;
        private int offset;

        SizedRefSortingSink(Sink<? super T> sink, Comparator<? super T> comparator) {
            super(sink, comparator);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.array = (T[]) new Object[(int) j];
        }

        @Override
        public void end() {
            int i = 0;
            Arrays.sort(this.array, 0, this.offset, this.comparator);
            this.downstream.begin(this.offset);
            if (!this.cancellationWasRequested) {
                while (i < this.offset) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            } else {
                while (i < this.offset && !this.downstream.cancellationRequested()) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            }
            this.downstream.end();
            this.array = null;
        }

        @Override
        public void accept(T t) {
            T[] tArr = this.array;
            int i = this.offset;
            this.offset = i + 1;
            tArr[i] = t;
        }
    }

    private static final class RefSortingSink<T> extends AbstractRefSortingSink<T> {
        private ArrayList<T> list;

        RefSortingSink(Sink<? super T> sink, Comparator<? super T> comparator) {
            super(sink, comparator);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.list = j >= 0 ? new ArrayList<>((int) j) : new ArrayList<>();
        }

        @Override
        public void end() {
            this.list.sort(this.comparator);
            this.downstream.begin(this.list.size());
            if (!this.cancellationWasRequested) {
                ArrayList<T> arrayList = this.list;
                final Sink<? super E_OUT> sink = this.downstream;
                Objects.requireNonNull(sink);
                arrayList.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        sink.accept(obj);
                    }
                });
            } else {
                for (T t : this.list) {
                    if (this.downstream.cancellationRequested()) {
                        break;
                    } else {
                        this.downstream.accept(t);
                    }
                }
            }
            this.downstream.end();
            this.list = null;
        }

        @Override
        public void accept(T t) {
            this.list.add(t);
        }
    }

    private static abstract class AbstractIntSortingSink extends Sink.ChainedInt<Integer> {
        protected boolean cancellationWasRequested;

        AbstractIntSortingSink(Sink<? super Integer> sink) {
            super(sink);
        }

        @Override
        public final boolean cancellationRequested() {
            this.cancellationWasRequested = true;
            return false;
        }
    }

    private static final class SizedIntSortingSink extends AbstractIntSortingSink {
        private int[] array;
        private int offset;

        SizedIntSortingSink(Sink<? super Integer> sink) {
            super(sink);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.array = new int[(int) j];
        }

        @Override
        public void end() {
            int i = 0;
            Arrays.sort(this.array, 0, this.offset);
            this.downstream.begin(this.offset);
            if (!this.cancellationWasRequested) {
                while (i < this.offset) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            } else {
                while (i < this.offset && !this.downstream.cancellationRequested()) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            }
            this.downstream.end();
            this.array = null;
        }

        @Override
        public void accept(int i) {
            int[] iArr = this.array;
            int i2 = this.offset;
            this.offset = i2 + 1;
            iArr[i2] = i;
        }
    }

    private static final class IntSortingSink extends AbstractIntSortingSink {
        private SpinedBuffer.OfInt b;

        IntSortingSink(Sink<? super Integer> sink) {
            super(sink);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.b = j > 0 ? new SpinedBuffer.OfInt((int) j) : new SpinedBuffer.OfInt();
        }

        @Override
        public void end() {
            int[] iArrAsPrimitiveArray = this.b.asPrimitiveArray();
            Arrays.sort(iArrAsPrimitiveArray);
            this.downstream.begin(iArrAsPrimitiveArray.length);
            int i = 0;
            if (!this.cancellationWasRequested) {
                int length = iArrAsPrimitiveArray.length;
                while (i < length) {
                    this.downstream.accept(iArrAsPrimitiveArray[i]);
                    i++;
                }
            } else {
                int length2 = iArrAsPrimitiveArray.length;
                while (i < length2) {
                    int i2 = iArrAsPrimitiveArray[i];
                    if (this.downstream.cancellationRequested()) {
                        break;
                    }
                    this.downstream.accept(i2);
                    i++;
                }
            }
            this.downstream.end();
        }

        @Override
        public void accept(int i) {
            this.b.accept(i);
        }
    }

    private static abstract class AbstractLongSortingSink extends Sink.ChainedLong<Long> {
        protected boolean cancellationWasRequested;

        AbstractLongSortingSink(Sink<? super Long> sink) {
            super(sink);
        }

        @Override
        public final boolean cancellationRequested() {
            this.cancellationWasRequested = true;
            return false;
        }
    }

    private static final class SizedLongSortingSink extends AbstractLongSortingSink {
        private long[] array;
        private int offset;

        SizedLongSortingSink(Sink<? super Long> sink) {
            super(sink);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.array = new long[(int) j];
        }

        @Override
        public void end() {
            int i = 0;
            Arrays.sort(this.array, 0, this.offset);
            this.downstream.begin(this.offset);
            if (!this.cancellationWasRequested) {
                while (i < this.offset) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            } else {
                while (i < this.offset && !this.downstream.cancellationRequested()) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            }
            this.downstream.end();
            this.array = null;
        }

        @Override
        public void accept(long j) {
            long[] jArr = this.array;
            int i = this.offset;
            this.offset = i + 1;
            jArr[i] = j;
        }
    }

    private static final class LongSortingSink extends AbstractLongSortingSink {
        private SpinedBuffer.OfLong b;

        LongSortingSink(Sink<? super Long> sink) {
            super(sink);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.b = j > 0 ? new SpinedBuffer.OfLong((int) j) : new SpinedBuffer.OfLong();
        }

        @Override
        public void end() {
            long[] jArrAsPrimitiveArray = this.b.asPrimitiveArray();
            Arrays.sort(jArrAsPrimitiveArray);
            this.downstream.begin(jArrAsPrimitiveArray.length);
            int i = 0;
            if (!this.cancellationWasRequested) {
                int length = jArrAsPrimitiveArray.length;
                while (i < length) {
                    this.downstream.accept(jArrAsPrimitiveArray[i]);
                    i++;
                }
            } else {
                int length2 = jArrAsPrimitiveArray.length;
                while (i < length2) {
                    long j = jArrAsPrimitiveArray[i];
                    if (this.downstream.cancellationRequested()) {
                        break;
                    }
                    this.downstream.accept(j);
                    i++;
                }
            }
            this.downstream.end();
        }

        @Override
        public void accept(long j) {
            this.b.accept(j);
        }
    }

    private static abstract class AbstractDoubleSortingSink extends Sink.ChainedDouble<Double> {
        protected boolean cancellationWasRequested;

        AbstractDoubleSortingSink(Sink<? super Double> sink) {
            super(sink);
        }

        @Override
        public final boolean cancellationRequested() {
            this.cancellationWasRequested = true;
            return false;
        }
    }

    private static final class SizedDoubleSortingSink extends AbstractDoubleSortingSink {
        private double[] array;
        private int offset;

        SizedDoubleSortingSink(Sink<? super Double> sink) {
            super(sink);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.array = new double[(int) j];
        }

        @Override
        public void end() {
            int i = 0;
            Arrays.sort(this.array, 0, this.offset);
            this.downstream.begin(this.offset);
            if (!this.cancellationWasRequested) {
                while (i < this.offset) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            } else {
                while (i < this.offset && !this.downstream.cancellationRequested()) {
                    this.downstream.accept(this.array[i]);
                    i++;
                }
            }
            this.downstream.end();
            this.array = null;
        }

        @Override
        public void accept(double d) {
            double[] dArr = this.array;
            int i = this.offset;
            this.offset = i + 1;
            dArr[i] = d;
        }
    }

    private static final class DoubleSortingSink extends AbstractDoubleSortingSink {
        private SpinedBuffer.OfDouble b;

        DoubleSortingSink(Sink<? super Double> sink) {
            super(sink);
        }

        @Override
        public void begin(long j) {
            if (j >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            this.b = j > 0 ? new SpinedBuffer.OfDouble((int) j) : new SpinedBuffer.OfDouble();
        }

        @Override
        public void end() {
            double[] dArrAsPrimitiveArray = this.b.asPrimitiveArray();
            Arrays.sort(dArrAsPrimitiveArray);
            this.downstream.begin(dArrAsPrimitiveArray.length);
            int i = 0;
            if (!this.cancellationWasRequested) {
                int length = dArrAsPrimitiveArray.length;
                while (i < length) {
                    this.downstream.accept(dArrAsPrimitiveArray[i]);
                    i++;
                }
            } else {
                int length2 = dArrAsPrimitiveArray.length;
                while (i < length2) {
                    double d = dArrAsPrimitiveArray[i];
                    if (this.downstream.cancellationRequested()) {
                        break;
                    }
                    this.downstream.accept(d);
                    i++;
                }
            }
            this.downstream.end();
        }

        @Override
        public void accept(double d) {
            this.b.accept(d);
        }
    }
}
