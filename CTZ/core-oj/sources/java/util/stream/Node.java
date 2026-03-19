package java.util.stream;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.stream.Node;
import java.util.stream.Sink;

public interface Node<T> {

    public interface Builder<T> extends Sink<T> {

        public interface OfDouble extends Builder<Double>, Sink.OfDouble {
            @Override
            Node<Double> build2();
        }

        public interface OfInt extends Builder<Integer>, Sink.OfInt {
            @Override
            Node<Integer> build2();
        }

        public interface OfLong extends Builder<Long>, Sink.OfLong {
            @Override
            Node<Long> build2();
        }

        Node<T> build2();
    }

    T[] asArray(IntFunction<T[]> intFunction);

    void copyInto(T[] tArr, int i);

    long count();

    void forEach(Consumer<? super T> consumer);

    Spliterator<T> spliterator();

    default int getChildCount() {
        return 0;
    }

    default Node<T> getChild(int i) {
        throw new IndexOutOfBoundsException();
    }

    default Node<T> truncate(long j, long j2, IntFunction<T[]> intFunction) {
        if (j == 0 && j2 == count()) {
            return this;
        }
        Spliterator<T> spliterator = spliterator();
        long j3 = j2 - j;
        Builder builder = Nodes.builder(j3, intFunction);
        builder.begin(j3);
        for (int i = 0; i < j && spliterator.tryAdvance(new Consumer() {
            @Override
            public final void accept(Object obj) {
                Node.lambda$truncate$0(obj);
            }
        }); i++) {
        }
        for (int i2 = 0; i2 < j3 && spliterator.tryAdvance(builder); i2++) {
        }
        builder.end();
        return builder.build2();
    }

    static void lambda$truncate$0(Object obj) {
    }

    default StreamShape getShape() {
        return StreamShape.REFERENCE;
    }

    public interface OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_NODE extends OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> extends Node<T> {
        T_ARR asPrimitiveArray();

        void copyInto(T_ARR t_arr, int i);

        void forEach(T_CONS t_cons);

        T_ARR newArray(int i);

        @Override
        T_SPLITR spliterator();

        @Override
        T_NODE truncate(long j, long j2, IntFunction<T[]> intFunction);

        @Override
        default T_NODE getChild(int i) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        default T[] asArray(IntFunction<T[]> intFunction) {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Node.OfPrimitive.asArray");
            }
            if (count() >= 2147483639) {
                throw new IllegalArgumentException("Stream size exceeds max array size");
            }
            T[] tArrApply = intFunction.apply((int) count());
            copyInto((Object[]) tArrApply, 0);
            return tArrApply;
        }
    }

    public interface OfInt extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, OfInt> {
        @Override
        default OfPrimitive truncate(long j, long j2, IntFunction intFunction) {
            return truncate(j, j2, (IntFunction<Integer[]>) intFunction);
        }

        @Override
        default Node truncate(long j, long j2, IntFunction intFunction) {
            return truncate(j, j2, (IntFunction<Integer[]>) intFunction);
        }

        @Override
        default void forEach(Consumer<? super Integer> consumer) {
            if (consumer instanceof IntConsumer) {
                forEach((IntConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Node.OfInt.forEachRemaining(Consumer)");
            }
            spliterator().forEachRemaining(consumer);
        }

        @Override
        default void copyInto(Integer[] numArr, int i) {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Node.OfInt.copyInto(Integer[], int)");
            }
            int[] iArrAsPrimitiveArray = asPrimitiveArray();
            for (int i2 = 0; i2 < iArrAsPrimitiveArray.length; i2++) {
                numArr[i + i2] = Integer.valueOf(iArrAsPrimitiveArray[i2]);
            }
        }

        @Override
        default OfInt truncate(long j, long j2, IntFunction<Integer[]> intFunction) {
            if (j == 0 && j2 == count()) {
                return this;
            }
            long j3 = j2 - j;
            Spliterator.OfInt ofIntSpliterator = spliterator();
            Builder.OfInt ofIntIntBuilder = Nodes.intBuilder(j3);
            ofIntIntBuilder.begin(j3);
            for (int i = 0; i < j && ofIntSpliterator.tryAdvance((IntConsumer) new IntConsumer() {
                @Override
                public final void accept(int i2) {
                    Node.OfInt.lambda$truncate$0(i2);
                }
            }); i++) {
            }
            for (int i2 = 0; i2 < j3 && ofIntSpliterator.tryAdvance((IntConsumer) ofIntIntBuilder); i2++) {
            }
            ofIntIntBuilder.end();
            return ofIntIntBuilder.build2();
        }

        static void lambda$truncate$0(int i) {
        }

        @Override
        default int[] newArray(int i) {
            return new int[i];
        }

        @Override
        default StreamShape getShape() {
            return StreamShape.INT_VALUE;
        }
    }

    public interface OfLong extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, OfLong> {
        @Override
        default OfPrimitive truncate(long j, long j2, IntFunction intFunction) {
            return truncate(j, j2, (IntFunction<Long[]>) intFunction);
        }

        @Override
        default Node truncate(long j, long j2, IntFunction intFunction) {
            return truncate(j, j2, (IntFunction<Long[]>) intFunction);
        }

        @Override
        default void forEach(Consumer<? super Long> consumer) {
            if (consumer instanceof LongConsumer) {
                forEach((LongConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Node.OfLong.forEachRemaining(Consumer)");
            }
            spliterator().forEachRemaining(consumer);
        }

        @Override
        default void copyInto(Long[] lArr, int i) {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Node.OfInt.copyInto(Long[], int)");
            }
            long[] jArrAsPrimitiveArray = asPrimitiveArray();
            for (int i2 = 0; i2 < jArrAsPrimitiveArray.length; i2++) {
                lArr[i + i2] = Long.valueOf(jArrAsPrimitiveArray[i2]);
            }
        }

        @Override
        default OfLong truncate(long j, long j2, IntFunction<Long[]> intFunction) {
            if (j == 0 && j2 == count()) {
                return this;
            }
            long j3 = j2 - j;
            Spliterator.OfLong ofLongSpliterator = spliterator();
            Builder.OfLong ofLongLongBuilder = Nodes.longBuilder(j3);
            ofLongLongBuilder.begin(j3);
            for (int i = 0; i < j && ofLongSpliterator.tryAdvance((LongConsumer) new LongConsumer() {
                @Override
                public final void accept(long j4) {
                    Node.OfLong.lambda$truncate$0(j4);
                }
            }); i++) {
            }
            for (int i2 = 0; i2 < j3 && ofLongSpliterator.tryAdvance((LongConsumer) ofLongLongBuilder); i2++) {
            }
            ofLongLongBuilder.end();
            return ofLongLongBuilder.build2();
        }

        static void lambda$truncate$0(long j) {
        }

        @Override
        default long[] newArray(int i) {
            return new long[i];
        }

        @Override
        default StreamShape getShape() {
            return StreamShape.LONG_VALUE;
        }
    }

    public interface OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, OfDouble> {
        @Override
        default OfPrimitive truncate(long j, long j2, IntFunction intFunction) {
            return truncate(j, j2, (IntFunction<Double[]>) intFunction);
        }

        @Override
        default Node truncate(long j, long j2, IntFunction intFunction) {
            return truncate(j, j2, (IntFunction<Double[]>) intFunction);
        }

        @Override
        default void forEach(Consumer<? super Double> consumer) {
            if (consumer instanceof DoubleConsumer) {
                forEach((DoubleConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Node.OfLong.forEachRemaining(Consumer)");
            }
            spliterator().forEachRemaining(consumer);
        }

        @Override
        default void copyInto(Double[] dArr, int i) {
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Node.OfDouble.copyInto(Double[], int)");
            }
            double[] dArrAsPrimitiveArray = asPrimitiveArray();
            for (int i2 = 0; i2 < dArrAsPrimitiveArray.length; i2++) {
                dArr[i + i2] = Double.valueOf(dArrAsPrimitiveArray[i2]);
            }
        }

        @Override
        default OfDouble truncate(long j, long j2, IntFunction<Double[]> intFunction) {
            if (j == 0 && j2 == count()) {
                return this;
            }
            long j3 = j2 - j;
            Spliterator.OfDouble ofDoubleSpliterator = spliterator();
            Builder.OfDouble ofDoubleDoubleBuilder = Nodes.doubleBuilder(j3);
            ofDoubleDoubleBuilder.begin(j3);
            for (int i = 0; i < j && ofDoubleSpliterator.tryAdvance((DoubleConsumer) new DoubleConsumer() {
                @Override
                public final void accept(double d) {
                    Node.OfDouble.lambda$truncate$0(d);
                }
            }); i++) {
            }
            for (int i2 = 0; i2 < j3 && ofDoubleSpliterator.tryAdvance((DoubleConsumer) ofDoubleDoubleBuilder); i2++) {
            }
            ofDoubleDoubleBuilder.end();
            return ofDoubleDoubleBuilder.build2();
        }

        static void lambda$truncate$0(double d) {
        }

        @Override
        default double[] newArray(int i) {
            return new double[i];
        }

        @Override
        default StreamShape getShape() {
            return StreamShape.DOUBLE_VALUE;
        }
    }
}
