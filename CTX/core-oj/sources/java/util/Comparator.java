package java.util;

import java.util.Comparators;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

@FunctionalInterface
public interface Comparator<T> {
    int compare(T t, T t2);

    boolean equals(Object obj);

    default Comparator<T> reversed() {
        return Collections.reverseOrder(this);
    }

    default Comparator<T> thenComparing(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new $$Lambda$Comparator$BZSVCoA8i87ehjxxZ1weEounfDQ(this, comparator);
    }

    static int lambda$thenComparing$36697e65$1(Comparator comparator, Comparator comparator2, Object obj, Object obj2) {
        int iCompare = comparator.compare(obj, obj2);
        return iCompare != 0 ? iCompare : comparator2.compare(obj, obj2);
    }

    default <U> Comparator<T> thenComparing(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return thenComparing(comparing(function, comparator));
    }

    default <U extends Comparable<? super U>> Comparator<T> thenComparing(Function<? super T, ? extends U> function) {
        return thenComparing(comparing(function));
    }

    default Comparator<T> thenComparingInt(ToIntFunction<? super T> toIntFunction) {
        return thenComparing(comparingInt(toIntFunction));
    }

    default Comparator<T> thenComparingLong(ToLongFunction<? super T> toLongFunction) {
        return thenComparing(comparingLong(toLongFunction));
    }

    default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> toDoubleFunction) {
        return thenComparing(comparingDouble(toDoubleFunction));
    }

    static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }

    static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return Comparators.NaturalOrderComparator.INSTANCE;
    }

    static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        return new Comparators.NullComparator(true, comparator);
    }

    static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        return new Comparators.NullComparator(false, comparator);
    }

    static <T, U> Comparator<T> comparing(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        Objects.requireNonNull(function);
        Objects.requireNonNull(comparator);
        return new $$Lambda$Comparator$KVN0LWz1D1wyrL2gs1CbubvLa9o(comparator, function);
    }

    static <T, U extends Comparable<? super U>> Comparator<T> comparing(Function<? super T, ? extends U> function) {
        Objects.requireNonNull(function);
        return new $$Lambda$Comparator$SPB8K9Yj7Pw1mljm7LpasV7zxWw(function);
    }

    static <T> Comparator<T> comparingInt(ToIntFunction<? super T> toIntFunction) {
        Objects.requireNonNull(toIntFunction);
        return new $$Lambda$Comparator$DNgpxUFZqmT4lOBzlVyPjWwvEvw(toIntFunction);
    }

    static <T> Comparator<T> comparingLong(ToLongFunction<? super T> toLongFunction) {
        Objects.requireNonNull(toLongFunction);
        return new $$Lambda$Comparator$4V5k8aLimtS0VsEILEAqQ9UGZYo(toLongFunction);
    }

    static <T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> toDoubleFunction) {
        Objects.requireNonNull(toDoubleFunction);
        return new $$Lambda$Comparator$edSxqANnwdmzeJ1aMMcwJWE2wII(toDoubleFunction);
    }
}
