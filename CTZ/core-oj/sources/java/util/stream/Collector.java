package java.util.stream;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface Collector<T, A, R> {

    public enum Characteristics {
        CONCURRENT,
        UNORDERED,
        IDENTITY_FINISH
    }

    BiConsumer<A, T> accumulator();

    Set<Characteristics> characteristics();

    BinaryOperator<A> combiner();

    Function<A, R> finisher();

    Supplier<A> supplier();

    static <T, R> Collector<T, R, R> of(Supplier<R> supplier, BiConsumer<R, T> biConsumer, BinaryOperator<R> binaryOperator, Characteristics... characteristicsArr) {
        Set<Characteristics> setUnmodifiableSet;
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(biConsumer);
        Objects.requireNonNull(binaryOperator);
        Objects.requireNonNull(characteristicsArr);
        if (characteristicsArr.length == 0) {
            setUnmodifiableSet = Collectors.CH_ID;
        } else {
            setUnmodifiableSet = Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH, characteristicsArr));
        }
        return new Collectors.CollectorImpl(supplier, biConsumer, binaryOperator, setUnmodifiableSet);
    }

    static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier, BiConsumer<A, T> biConsumer, BinaryOperator<A> binaryOperator, Function<A, R> function, Characteristics... characteristicsArr) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(biConsumer);
        Objects.requireNonNull(binaryOperator);
        Objects.requireNonNull(function);
        Objects.requireNonNull(characteristicsArr);
        Set<Characteristics> setUnmodifiableSet = Collectors.CH_NOID;
        if (characteristicsArr.length > 0) {
            EnumSet enumSetNoneOf = EnumSet.noneOf(Characteristics.class);
            Collections.addAll(enumSetNoneOf, characteristicsArr);
            setUnmodifiableSet = Collections.unmodifiableSet(enumSetNoneOf);
        }
        return new Collectors.CollectorImpl(supplier, biConsumer, binaryOperator, function, setUnmodifiableSet);
    }
}
