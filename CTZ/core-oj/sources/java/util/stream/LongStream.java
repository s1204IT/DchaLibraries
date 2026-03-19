package java.util.stream;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.StreamSpliterators;
import java.util.stream.Streams;

public interface LongStream extends BaseStream<Long, LongStream> {
    boolean allMatch(LongPredicate longPredicate);

    boolean anyMatch(LongPredicate longPredicate);

    DoubleStream asDoubleStream();

    OptionalDouble average();

    Stream<Long> boxed();

    <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> objLongConsumer, BiConsumer<R, R> biConsumer);

    long count();

    LongStream distinct();

    LongStream filter(LongPredicate longPredicate);

    OptionalLong findAny();

    OptionalLong findFirst();

    LongStream flatMap(LongFunction<? extends LongStream> longFunction);

    void forEach(LongConsumer longConsumer);

    void forEachOrdered(LongConsumer longConsumer);

    @Override
    Iterator<Long> iterator();

    LongStream limit(long j);

    LongStream map(LongUnaryOperator longUnaryOperator);

    DoubleStream mapToDouble(LongToDoubleFunction longToDoubleFunction);

    IntStream mapToInt(LongToIntFunction longToIntFunction);

    <U> Stream<U> mapToObj(LongFunction<? extends U> longFunction);

    OptionalLong max();

    OptionalLong min();

    boolean noneMatch(LongPredicate longPredicate);

    @Override
    LongStream parallel();

    LongStream peek(LongConsumer longConsumer);

    long reduce(long j, LongBinaryOperator longBinaryOperator);

    OptionalLong reduce(LongBinaryOperator longBinaryOperator);

    @Override
    LongStream sequential();

    LongStream skip(long j);

    LongStream sorted();

    @Override
    Spliterator<Long> spliterator2();

    long sum();

    LongSummaryStatistics summaryStatistics();

    long[] toArray();

    static Builder builder() {
        return new Streams.LongStreamBuilderImpl();
    }

    static LongStream empty() {
        return StreamSupport.longStream(Spliterators.emptyLongSpliterator(), false);
    }

    static LongStream of(long j) {
        return StreamSupport.longStream(new Streams.LongStreamBuilderImpl(j), false);
    }

    static LongStream of(long... jArr) {
        return Arrays.stream(jArr);
    }

    static LongStream iterate(final long j, final LongUnaryOperator longUnaryOperator) {
        Objects.requireNonNull(longUnaryOperator);
        return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(new PrimitiveIterator.OfLong() {
            long t;

            {
                this.t = j;
            }

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                long j2 = this.t;
                this.t = longUnaryOperator.applyAsLong(this.t);
                return j2;
            }
        }, 1296), false);
    }

    static LongStream generate(LongSupplier longSupplier) {
        Objects.requireNonNull(longSupplier);
        return StreamSupport.longStream(new StreamSpliterators.InfiniteSupplyingSpliterator.OfLong(Long.MAX_VALUE, longSupplier), false);
    }

    static LongStream range(long j, long j2) {
        if (j >= j2) {
            return empty();
        }
        if (j2 - j < 0) {
            long jLongValue = BigInteger.valueOf(j2).subtract(BigInteger.valueOf(j)).divide(BigInteger.valueOf(2L)).longValue() + j + 1;
            return concat(range(j, jLongValue), range(jLongValue, j2));
        }
        return StreamSupport.longStream(new Streams.RangeLongSpliterator(j, j2, false), false);
    }

    static LongStream rangeClosed(long j, long j2) {
        if (j > j2) {
            return empty();
        }
        if ((j2 - j) + 1 <= 0) {
            long jLongValue = BigInteger.valueOf(j2).subtract(BigInteger.valueOf(j)).divide(BigInteger.valueOf(2L)).longValue() + j + 1;
            return concat(range(j, jLongValue), rangeClosed(jLongValue, j2));
        }
        return StreamSupport.longStream(new Streams.RangeLongSpliterator(j, j2, true), false);
    }

    static LongStream concat(LongStream longStream, LongStream longStream2) {
        Objects.requireNonNull(longStream);
        Objects.requireNonNull(longStream2);
        return StreamSupport.longStream(new Streams.ConcatSpliterator.OfLong(longStream.spliterator2(), longStream2.spliterator2()), longStream.isParallel() || longStream2.isParallel()).onClose(Streams.composedClose(longStream, longStream2));
    }

    public interface Builder extends LongConsumer {
        @Override
        void accept(long j);

        LongStream build();

        default Builder add(long j) {
            accept(j);
            return this;
        }
    }
}
