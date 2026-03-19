package java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Map<K, V> {
    void clear();

    boolean containsKey(Object obj);

    boolean containsValue(Object obj);

    Set<Entry<K, V>> entrySet();

    boolean equals(Object obj);

    V get(Object obj);

    int hashCode();

    boolean isEmpty();

    Set<K> keySet();

    V put(K k, V v);

    void putAll(Map<? extends K, ? extends V> map);

    V remove(Object obj);

    int size();

    Collection<V> values();

    public interface Entry<K, V> {
        boolean equals(Object obj);

        K getKey();

        V getValue();

        int hashCode();

        V setValue(V v);

        static <K extends Comparable<? super K>, V> Comparator<Entry<K, V>> comparingByKey() {
            return $$Lambda$Map$Entry$zJtjVuaqJl6rzQLvCcTd4dnXnnw.INSTANCE;
        }

        static <K, V extends Comparable<? super V>> Comparator<Entry<K, V>> comparingByValue() {
            return $$Lambda$Map$Entry$acJOHw6hO1wh4v9r2vtUuCFe5vI.INSTANCE;
        }

        static <K, V> Comparator<Entry<K, V>> comparingByKey(Comparator<? super K> comparator) {
            Objects.requireNonNull(comparator);
            return new $$Lambda$Map$Entry$g8sc1MgjjhwTaK8zHulzMasixMw(comparator);
        }

        static <K, V> Comparator<Entry<K, V>> comparingByValue(Comparator<? super V> comparator) {
            Objects.requireNonNull(comparator);
            return new $$Lambda$Map$Entry$Y3nKRmSXx8yzU_ApvOwqAqvBas(comparator);
        }
    }

    default V getOrDefault(Object obj, V v) {
        V v2 = get(obj);
        return (v2 != null || containsKey(obj)) ? v2 : v;
    }

    default void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Objects.requireNonNull(biConsumer);
        for (Entry<K, V> entry : entrySet()) {
            try {
                biConsumer.accept(entry.getKey(), entry.getValue());
            } catch (IllegalStateException e) {
                throw new ConcurrentModificationException(e);
            }
        }
    }

    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        for (Entry<K, V> entry : entrySet()) {
            try {
                try {
                    entry.setValue(biFunction.apply(entry.getKey(), entry.getValue()));
                } catch (IllegalStateException e) {
                    throw new ConcurrentModificationException(e);
                }
            } catch (IllegalStateException e2) {
                throw new ConcurrentModificationException(e2);
            }
        }
    }

    default V putIfAbsent(K k, V v) {
        V v2 = get(k);
        if (v2 == null) {
            return put(k, v);
        }
        return v2;
    }

    default boolean remove(Object obj, Object obj2) {
        V v = get(obj);
        if (Objects.equals(v, obj2)) {
            if (v == null && !containsKey(obj)) {
                return false;
            }
            remove(obj);
            return true;
        }
        return false;
    }

    default boolean replace(K k, V v, V v2) {
        V v3 = get(k);
        if (Objects.equals(v3, v)) {
            if (v3 == null && !containsKey(k)) {
                return false;
            }
            put(k, v2);
            return true;
        }
        return false;
    }

    default V replace(K k, V v) {
        V v2 = get(k);
        if (v2 != null || containsKey(k)) {
            return put(k, v);
        }
        return v2;
    }

    default V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
        V vApply;
        Objects.requireNonNull(function);
        V v = get(k);
        if (v == null && (vApply = function.apply(k)) != null) {
            put(k, vApply);
            return vApply;
        }
        return v;
    }

    default V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        V v = get(k);
        if (v == null) {
            return null;
        }
        V vApply = biFunction.apply(k, v);
        if (vApply != null) {
            put(k, vApply);
            return vApply;
        }
        remove(k);
        return null;
    }

    default V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        V v = get(k);
        V vApply = biFunction.apply(k, v);
        if (vApply != null) {
            put(k, vApply);
            return vApply;
        }
        if (v == null && !containsKey(k)) {
            return null;
        }
        remove(k);
        return null;
    }

    default V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        Objects.requireNonNull(v);
        Object obj = (Object) get(k);
        ?? Apply = v;
        if (obj != null) {
            Apply = biFunction.apply(obj, v);
        }
        if (Apply == 0) {
            remove(k);
        } else {
            put(k, (Object) Apply);
        }
        return (V) Apply;
    }
}
