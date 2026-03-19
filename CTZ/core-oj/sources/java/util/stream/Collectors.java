package java.util.stream;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class Collectors {
    static final Set<Collector.Characteristics> CH_CONCURRENT_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_CONCURRENT_NOID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED));
    static final Set<Collector.Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_UNORDERED_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_NOID = Collections.emptySet();

    private Collectors() {
    }

    static Object lambda$throwingMerger$0(Object obj, Object obj2) {
        throw new IllegalStateException(String.format("Duplicate key %s", obj));
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$throwingMerger$0(obj, obj2);
            }
        };
    }

    private static <I, R> Function<I, R> castingIdentity() {
        return new Function() {
            @Override
            public final Object apply(Object obj) {
                return Collectors.lambda$castingIdentity$1(obj);
            }
        };
    }

    static Object lambda$castingIdentity$1(Object obj) {
        return obj;
    }

    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private final BiConsumer<A, T> accumulator;
        private final Set<Collector.Characteristics> characteristics;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Supplier<A> supplier;

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> biConsumer, BinaryOperator<A> binaryOperator, Function<A, R> function, Set<Collector.Characteristics> set) {
            this.supplier = supplier;
            this.accumulator = biConsumer;
            this.combiner = binaryOperator;
            this.finisher = function;
            this.characteristics = set;
        }

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> biConsumer, BinaryOperator<A> binaryOperator, Set<Collector.Characteristics> set) {
            this(supplier, biConsumer, binaryOperator, Collectors.castingIdentity(), set);
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return this.accumulator;
        }

        @Override
        public Supplier<A> supplier() {
            return this.supplier;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return this.combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return this.finisher;
        }

        @Override
        public Set<Collector.Characteristics> characteristics() {
            return this.characteristics;
        }
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> supplier) {
        return new CollectorImpl(supplier, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((Collection) obj).add(obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$toCollection$2((Collection) obj, (Collection) obj2);
            }
        }, CH_ID);
    }

    static Collection lambda$toCollection$2(Collection collection, Collection collection2) {
        collection.addAll(collection2);
        return collection;
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return new ArrayList();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((List) obj).add(obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$toList$3((List) obj, (List) obj2);
            }
        }, CH_ID);
    }

    static List lambda$toList$3(List list, List list2) {
        list.addAll(list2);
        return list;
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return new HashSet();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((Set) obj).add(obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$toSet$4((Set) obj, (Set) obj2);
            }
        }, CH_UNORDERED_ID);
    }

    static Set lambda$toSet$4(Set set, Set set2) {
        set.addAll(set2);
        return set;
    }

    public static Collector<CharSequence, ?, String> joining() {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return new StringBuilder();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((StringBuilder) obj).append((CharSequence) obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$joining$5((StringBuilder) obj, (StringBuilder) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((StringBuilder) obj).toString();
            }
        }, CH_NOID);
    }

    static StringBuilder lambda$joining$5(StringBuilder sb, StringBuilder sb2) {
        sb.append((CharSequence) sb2);
        return sb;
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence charSequence) {
        return joining(charSequence, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(final CharSequence charSequence, final CharSequence charSequence2, final CharSequence charSequence3) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$joining$6(charSequence, charSequence2, charSequence3);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((StringJoiner) obj).add((CharSequence) obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return ((StringJoiner) obj).merge((StringJoiner) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((StringJoiner) obj).toString();
            }
        }, CH_NOID);
    }

    static StringJoiner lambda$joining$6(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        return new StringJoiner(charSequence, charSequence2, charSequence3);
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(final BinaryOperator<V> binaryOperator) {
        return new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$mapMerger$7(binaryOperator, (Map) obj, (Map) obj2);
            }
        };
    }

    static Map lambda$mapMerger$7(BinaryOperator binaryOperator, Map map, Map map2) {
        for (Map.Entry entry : map2.entrySet()) {
            map.merge(entry.getKey(), entry.getValue(), binaryOperator);
        }
        return map;
    }

    public static <T, U, A, R> Collector<T, ?, R> mapping(final Function<? super T, ? extends U> function, Collector<? super U, A, R> collector) {
        final BiConsumer<A, ? super U> biConsumerAccumulator = collector.accumulator();
        return new CollectorImpl(collector.supplier(), new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                biConsumerAccumulator.accept(obj, function.apply(obj2));
            }
        }, collector.combiner(), collector.finisher(), collector.characteristics());
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> collector, Function<R, RR> function) {
        Set<Collector.Characteristics> setCharacteristics = collector.characteristics();
        if (setCharacteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
            if (setCharacteristics.size() == 1) {
                setCharacteristics = CH_NOID;
            } else {
                EnumSet enumSetCopyOf = EnumSet.copyOf(setCharacteristics);
                enumSetCopyOf.remove(Collector.Characteristics.IDENTITY_FINISH);
                setCharacteristics = Collections.unmodifiableSet(enumSetCopyOf);
            }
        }
        return new CollectorImpl(collector.supplier(), collector.accumulator(), collector.combiner(), collector.finisher().andThen(function), setCharacteristics);
    }

    public static <T> Collector<T, ?, Long> counting() {
        return reducing(0L, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Collectors.lambda$counting$9(obj);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Long.valueOf(Long.sum(((Long) obj).longValue(), ((Long) obj2).longValue()));
            }
        });
    }

    static Long lambda$counting$9(Object obj) {
        return 1L;
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.minBy(comparator));
    }

    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.maxBy(comparator));
    }

    public static <T> Collector<T, ?, Integer> summingInt(final ToIntFunction<? super T> toIntFunction) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$summingInt$10();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$summingInt$11(toIntFunction, (int[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$summingInt$12((int[]) obj, (int[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((int[]) obj)[0]);
            }
        }, CH_NOID);
    }

    static int[] lambda$summingInt$10() {
        return new int[1];
    }

    static void lambda$summingInt$11(ToIntFunction toIntFunction, int[] iArr, Object obj) {
        iArr[0] = iArr[0] + toIntFunction.applyAsInt(obj);
    }

    static int[] lambda$summingInt$12(int[] iArr, int[] iArr2) {
        iArr[0] = iArr[0] + iArr2[0];
        return iArr;
    }

    public static <T> Collector<T, ?, Long> summingLong(final ToLongFunction<? super T> toLongFunction) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$summingLong$14();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$summingLong$15(toLongFunction, (long[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$summingLong$16((long[]) obj, (long[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Long.valueOf(((long[]) obj)[0]);
            }
        }, CH_NOID);
    }

    static long[] lambda$summingLong$14() {
        return new long[1];
    }

    static void lambda$summingLong$15(ToLongFunction toLongFunction, long[] jArr, Object obj) {
        jArr[0] = jArr[0] + toLongFunction.applyAsLong(obj);
    }

    static long[] lambda$summingLong$16(long[] jArr, long[] jArr2) {
        jArr[0] = jArr[0] + jArr2[0];
        return jArr;
    }

    public static <T> Collector<T, ?, Double> summingDouble(final ToDoubleFunction<? super T> toDoubleFunction) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$summingDouble$18();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$summingDouble$19(toDoubleFunction, (double[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$summingDouble$20((double[]) obj, (double[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Double.valueOf(Collectors.computeFinalSum((double[]) obj));
            }
        }, CH_NOID);
    }

    static double[] lambda$summingDouble$18() {
        return new double[3];
    }

    static void lambda$summingDouble$19(ToDoubleFunction toDoubleFunction, double[] dArr, Object obj) {
        sumWithCompensation(dArr, toDoubleFunction.applyAsDouble(obj));
        dArr[2] = dArr[2] + toDoubleFunction.applyAsDouble(obj);
    }

    static double[] lambda$summingDouble$20(double[] dArr, double[] dArr2) {
        sumWithCompensation(dArr, dArr2[0]);
        dArr[2] = dArr[2] + dArr2[2];
        return sumWithCompensation(dArr, dArr2[1]);
    }

    static double[] sumWithCompensation(double[] dArr, double d) {
        double d2 = d - dArr[1];
        double d3 = dArr[0];
        double d4 = d3 + d2;
        dArr[1] = (d4 - d3) - d2;
        dArr[0] = d4;
        return dArr;
    }

    static double computeFinalSum(double[] dArr) {
        double d = dArr[0] + dArr[1];
        double d2 = dArr[dArr.length - 1];
        if (Double.isNaN(d) && Double.isInfinite(d2)) {
            return d2;
        }
        return d;
    }

    public static <T> Collector<T, ?, Double> averagingInt(final ToIntFunction<? super T> toIntFunction) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$averagingInt$22();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$averagingInt$23(toIntFunction, (long[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$averagingInt$24((long[]) obj, (long[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Double.valueOf(((long[]) obj)[1] == 0 ? 0.0d : r1[0] / r1[1]);
            }
        }, CH_NOID);
    }

    static long[] lambda$averagingInt$22() {
        return new long[2];
    }

    static void lambda$averagingInt$23(ToIntFunction toIntFunction, long[] jArr, Object obj) {
        jArr[0] = jArr[0] + ((long) toIntFunction.applyAsInt(obj));
        jArr[1] = jArr[1] + 1;
    }

    static long[] lambda$averagingInt$24(long[] jArr, long[] jArr2) {
        jArr[0] = jArr[0] + jArr2[0];
        jArr[1] = jArr[1] + jArr2[1];
        return jArr;
    }

    public static <T> Collector<T, ?, Double> averagingLong(final ToLongFunction<? super T> toLongFunction) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$averagingLong$26();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$averagingLong$27(toLongFunction, (long[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$averagingLong$28((long[]) obj, (long[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Double.valueOf(((long[]) obj)[1] == 0 ? 0.0d : r1[0] / r1[1]);
            }
        }, CH_NOID);
    }

    static long[] lambda$averagingLong$26() {
        return new long[2];
    }

    static void lambda$averagingLong$27(ToLongFunction toLongFunction, long[] jArr, Object obj) {
        jArr[0] = jArr[0] + toLongFunction.applyAsLong(obj);
        jArr[1] = jArr[1] + 1;
    }

    static long[] lambda$averagingLong$28(long[] jArr, long[] jArr2) {
        jArr[0] = jArr[0] + jArr2[0];
        jArr[1] = jArr[1] + jArr2[1];
        return jArr;
    }

    public static <T> Collector<T, ?, Double> averagingDouble(final ToDoubleFunction<? super T> toDoubleFunction) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$averagingDouble$30();
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$averagingDouble$31(toDoubleFunction, (double[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$averagingDouble$32((double[]) obj, (double[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                double[] dArr = (double[]) obj;
                return Double.valueOf(dArr[2] != 0.0d ? Collectors.computeFinalSum(dArr) / dArr[2] : 0.0d);
            }
        }, CH_NOID);
    }

    static double[] lambda$averagingDouble$30() {
        return new double[4];
    }

    static void lambda$averagingDouble$31(ToDoubleFunction toDoubleFunction, double[] dArr, Object obj) {
        sumWithCompensation(dArr, toDoubleFunction.applyAsDouble(obj));
        dArr[2] = dArr[2] + 1.0d;
        dArr[3] = dArr[3] + toDoubleFunction.applyAsDouble(obj);
    }

    static double[] lambda$averagingDouble$32(double[] dArr, double[] dArr2) {
        sumWithCompensation(dArr, dArr2[0]);
        sumWithCompensation(dArr, dArr2[1]);
        dArr[2] = dArr[2] + dArr2[2];
        dArr[3] = dArr[3] + dArr2[3];
        return dArr;
    }

    public static <T> Collector<T, ?, T> reducing(T t, final BinaryOperator<T> binaryOperator) {
        return new CollectorImpl(boxSupplier(t), new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$reducing$34(binaryOperator, (Object[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$reducing$35(binaryOperator, (Object[]) obj, (Object[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Collectors.lambda$reducing$36((Object[]) obj);
            }
        }, CH_NOID);
    }

    static void lambda$reducing$34(BinaryOperator binaryOperator, Object[] objArr, Object obj) {
        objArr[0] = binaryOperator.apply(objArr[0], obj);
    }

    static Object[] lambda$reducing$35(BinaryOperator binaryOperator, Object[] objArr, Object[] objArr2) {
        objArr[0] = binaryOperator.apply(objArr[0], objArr2[0]);
        return objArr;
    }

    static Object lambda$reducing$36(Object[] objArr) {
        return objArr[0];
    }

    private static <T> Supplier<T[]> boxSupplier(final T t) {
        return new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$boxSupplier$37(t);
            }
        };
    }

    static Object[] lambda$boxSupplier$37(Object obj) {
        return new Object[]{obj};
    }

    class C1OptionalBox<T> implements Consumer<T> {
        final BinaryOperator val$op;
        T value = null;
        boolean present = false;

        C1OptionalBox(BinaryOperator binaryOperator) {
            this.val$op = binaryOperator;
        }

        @Override
        public void accept(T t) {
            if (this.present) {
                this.value = this.val$op.apply(this.value, t);
            } else {
                this.value = t;
                this.present = true;
            }
        }
    }

    public static <T> Collector<T, ?, Optional<T>> reducing(final BinaryOperator<T> binaryOperator) {
        return new CollectorImpl(new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$reducing$38(binaryOperator);
            }
        }, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((Collectors.C1OptionalBox) obj).accept(obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$reducing$39((Collectors.C1OptionalBox) obj, (Collectors.C1OptionalBox) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Optional.ofNullable(((Collectors.C1OptionalBox) obj).value);
            }
        }, CH_NOID);
    }

    static C1OptionalBox lambda$reducing$38(BinaryOperator binaryOperator) {
        return new C1OptionalBox(binaryOperator);
    }

    static C1OptionalBox lambda$reducing$39(C1OptionalBox c1OptionalBox, C1OptionalBox c1OptionalBox2) {
        if (c1OptionalBox2.present) {
            c1OptionalBox.accept(c1OptionalBox2.value);
        }
        return c1OptionalBox;
    }

    public static <T, U> Collector<T, ?, U> reducing(U u, final Function<? super T, ? extends U> function, final BinaryOperator<U> binaryOperator) {
        return new CollectorImpl(boxSupplier(u), new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.lambda$reducing$41(binaryOperator, function, (Object[]) obj, obj2);
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$reducing$42(binaryOperator, (Object[]) obj, (Object[]) obj2);
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Collectors.lambda$reducing$43((Object[]) obj);
            }
        }, CH_NOID);
    }

    static void lambda$reducing$41(BinaryOperator binaryOperator, Function function, Object[] objArr, Object obj) {
        objArr[0] = binaryOperator.apply(objArr[0], function.apply(obj));
    }

    static Object[] lambda$reducing$42(BinaryOperator binaryOperator, Object[] objArr, Object[] objArr2) {
        objArr[0] = binaryOperator.apply(objArr[0], objArr2[0]);
        return objArr;
    }

    static Object lambda$reducing$43(Object[] objArr) {
        return objArr[0];
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> function) {
        return groupingBy(function, toList());
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> function, Collector<? super T, A, D> collector) {
        return groupingBy(function, $$Lambda$ry7iWszBr7beYy31SdRxibDyciQ.INSTANCE, collector);
    }

    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(final Function<? super T, ? extends K> function, Supplier<M> supplier, Collector<? super T, A, D> collector) {
        final Supplier<A> supplier2 = collector.supplier();
        final BiConsumer<A, ? super T> biConsumerAccumulator = collector.accumulator();
        BiConsumer biConsumer = new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                biConsumerAccumulator.accept(((Map) obj).computeIfAbsent(Objects.requireNonNull(function.apply(obj2), "element cannot be mapped to a null key"), new Function() {
                    @Override
                    public final Object apply(Object obj3) {
                        return supplier.get();
                    }
                }), obj2);
            }
        };
        BinaryOperator binaryOperatorMapMerger = mapMerger(collector.combiner());
        if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(supplier, biConsumer, binaryOperatorMapMerger, CH_ID);
        }
        final Function<A, D> functionFinisher = collector.finisher();
        return new CollectorImpl(supplier, biConsumer, binaryOperatorMapMerger, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Collectors.lambda$groupingBy$47(functionFinisher, (Map) obj);
            }
        }, CH_NOID);
    }

    static Map lambda$groupingBy$47(final Function function, Map map) {
        map.replaceAll(new BiFunction() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return function.apply(obj2);
            }
        });
        return map;
    }

    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> function) {
        return groupingByConcurrent(function, $$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE, toList());
    }

    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> function, Collector<? super T, A, D> collector) {
        return groupingByConcurrent(function, $$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE, collector);
    }

    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> function, Supplier<M> supplier, Collector<? super T, A, D> collector) {
        BiConsumer biConsumer;
        final Supplier<A> supplier2 = collector.supplier();
        final BiConsumer<A, ? super T> biConsumerAccumulator = collector.accumulator();
        BinaryOperator binaryOperatorMapMerger = mapMerger(collector.combiner());
        if (collector.characteristics().contains(Collector.Characteristics.CONCURRENT)) {
            biConsumer = new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    biConsumerAccumulator.accept(((ConcurrentMap) obj).computeIfAbsent(Objects.requireNonNull(function.apply(obj2), "element cannot be mapped to a null key"), new Function() {
                        @Override
                        public final Object apply(Object obj3) {
                            return supplier.get();
                        }
                    }), obj2);
                }
            };
        } else {
            biConsumer = new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    Collectors.lambda$groupingByConcurrent$51(function, supplier2, biConsumerAccumulator, (ConcurrentMap) obj, obj2);
                }
            };
        }
        BiConsumer biConsumer2 = biConsumer;
        if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(supplier, biConsumer2, binaryOperatorMapMerger, CH_CONCURRENT_ID);
        }
        final Function<A, D> functionFinisher = collector.finisher();
        return new CollectorImpl(supplier, biConsumer2, binaryOperatorMapMerger, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Collectors.lambda$groupingByConcurrent$53(functionFinisher, (ConcurrentMap) obj);
            }
        }, CH_CONCURRENT_NOID);
    }

    static void lambda$groupingByConcurrent$51(Function function, final Supplier supplier, BiConsumer biConsumer, ConcurrentMap concurrentMap, Object obj) {
        Object objComputeIfAbsent = concurrentMap.computeIfAbsent(Objects.requireNonNull(function.apply(obj), "element cannot be mapped to a null key"), new Function() {
            @Override
            public final Object apply(Object obj2) {
                return supplier.get();
            }
        });
        synchronized (objComputeIfAbsent) {
            biConsumer.accept(objComputeIfAbsent, obj);
        }
    }

    static ConcurrentMap lambda$groupingByConcurrent$53(final Function function, ConcurrentMap concurrentMap) {
        concurrentMap.replaceAll(new BiFunction() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return function.apply(obj2);
            }
        });
        return concurrentMap;
    }

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, toList());
    }

    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(final Predicate<? super T> predicate, final Collector<? super T, A, D> collector) {
        final BiConsumer<A, ? super T> biConsumerAccumulator = collector.accumulator();
        BiConsumer biConsumer = new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Collectors.Partition partition = (Collectors.Partition) obj;
                biConsumerAccumulator.accept(predicate.test(obj2) ? partition.forTrue : partition.forFalse, obj2);
            }
        };
        final BinaryOperator<A> binaryOperatorCombiner = collector.combiner();
        BinaryOperator binaryOperator = new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$partitioningBy$55(binaryOperatorCombiner, (Collectors.Partition) obj, (Collectors.Partition) obj2);
            }
        };
        Supplier supplier = new Supplier() {
            @Override
            public final Object get() {
                return Collectors.lambda$partitioningBy$56(collector);
            }
        };
        if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(supplier, biConsumer, binaryOperator, CH_ID);
        }
        return new CollectorImpl(supplier, biConsumer, binaryOperator, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Collectors.lambda$partitioningBy$57(collector, (Collectors.Partition) obj);
            }
        }, CH_NOID);
    }

    static Partition lambda$partitioningBy$55(BinaryOperator binaryOperator, Partition partition, Partition partition2) {
        return new Partition(binaryOperator.apply(partition.forTrue, partition2.forTrue), binaryOperator.apply(partition.forFalse, partition2.forFalse));
    }

    static Partition lambda$partitioningBy$56(Collector collector) {
        return new Partition(collector.supplier().get(), collector.supplier().get());
    }

    static Map lambda$partitioningBy$57(Collector collector, Partition partition) {
        return new Partition(collector.finisher().apply(partition.forTrue), collector.finisher().apply(partition.forFalse));
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> function, Function<? super T, ? extends U> function2) {
        return toMap(function, function2, throwingMerger(), $$Lambda$ry7iWszBr7beYy31SdRxibDyciQ.INSTANCE);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> function, Function<? super T, ? extends U> function2, BinaryOperator<U> binaryOperator) {
        return toMap(function, function2, binaryOperator, $$Lambda$ry7iWszBr7beYy31SdRxibDyciQ.INSTANCE);
    }

    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends U> function2, final BinaryOperator<U> binaryOperator, Supplier<M> supplier) {
        return new CollectorImpl(supplier, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                Map map = (Map) obj;
                map.merge(function.apply(obj2), function2.apply(obj2), binaryOperator);
            }
        }, mapMerger(binaryOperator), CH_ID);
    }

    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> function, Function<? super T, ? extends U> function2) {
        return toConcurrentMap(function, function2, throwingMerger(), $$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE);
    }

    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> function, Function<? super T, ? extends U> function2, BinaryOperator<U> binaryOperator) {
        return toConcurrentMap(function, function2, binaryOperator, $$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE);
    }

    public static <T, K, U, M extends ConcurrentMap<K, U>> Collector<T, ?, M> toConcurrentMap(final Function<? super T, ? extends K> function, final Function<? super T, ? extends U> function2, final BinaryOperator<U> binaryOperator, Supplier<M> supplier) {
        return new CollectorImpl(supplier, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ConcurrentMap concurrentMap = (ConcurrentMap) obj;
                concurrentMap.merge(function.apply(obj2), function2.apply(obj2), binaryOperator);
            }
        }, mapMerger(binaryOperator), CH_CONCURRENT_ID);
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(final ToIntFunction<? super T> toIntFunction) {
        return new CollectorImpl($$Lambda$_Ea_sNpqZAwihIOCRBaP7hHgWWI.INSTANCE, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((IntSummaryStatistics) obj).accept(toIntFunction.applyAsInt(obj2));
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$summarizingInt$61((IntSummaryStatistics) obj, (IntSummaryStatistics) obj2);
            }
        }, CH_ID);
    }

    static IntSummaryStatistics lambda$summarizingInt$61(IntSummaryStatistics intSummaryStatistics, IntSummaryStatistics intSummaryStatistics2) {
        intSummaryStatistics.combine(intSummaryStatistics2);
        return intSummaryStatistics;
    }

    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(final ToLongFunction<? super T> toLongFunction) {
        return new CollectorImpl($$Lambda$kZuTETptiPwvB1J27Na7j760aLU.INSTANCE, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((LongSummaryStatistics) obj).accept(toLongFunction.applyAsLong(obj2));
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$summarizingLong$63((LongSummaryStatistics) obj, (LongSummaryStatistics) obj2);
            }
        }, CH_ID);
    }

    static LongSummaryStatistics lambda$summarizingLong$63(LongSummaryStatistics longSummaryStatistics, LongSummaryStatistics longSummaryStatistics2) {
        longSummaryStatistics.combine(longSummaryStatistics2);
        return longSummaryStatistics;
    }

    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(final ToDoubleFunction<? super T> toDoubleFunction) {
        return new CollectorImpl($$Lambda$745FUy7cYwYu7KrMQTYh2DNqh1I.INSTANCE, new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((DoubleSummaryStatistics) obj).accept(toDoubleFunction.applyAsDouble(obj2));
            }
        }, new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return Collectors.lambda$summarizingDouble$65((DoubleSummaryStatistics) obj, (DoubleSummaryStatistics) obj2);
            }
        }, CH_ID);
    }

    static DoubleSummaryStatistics lambda$summarizingDouble$65(DoubleSummaryStatistics doubleSummaryStatistics, DoubleSummaryStatistics doubleSummaryStatistics2) {
        doubleSummaryStatistics.combine(doubleSummaryStatistics2);
        return doubleSummaryStatistics;
    }

    private static final class Partition<T> extends AbstractMap<Boolean, T> implements Map<Boolean, T> {
        final T forFalse;
        final T forTrue;

        Partition(T t, T t2) {
            this.forTrue = t;
            this.forFalse = t2;
        }

        @Override
        public Set<Map.Entry<Boolean, T>> entrySet() {
            return new AbstractSet<Map.Entry<Boolean, T>>() {
                @Override
                public Iterator<Map.Entry<Boolean, T>> iterator() {
                    return Arrays.asList(new AbstractMap.SimpleImmutableEntry(false, Partition.this.forFalse), new AbstractMap.SimpleImmutableEntry(true, Partition.this.forTrue)).iterator();
                }

                @Override
                public int size() {
                    return 2;
                }
            };
        }
    }
}
