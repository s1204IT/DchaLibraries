package java.util.concurrent;

import android.R;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ConcurrentMap<K, V> extends Map<K, V> {
    V putIfAbsent(K k, V v);

    boolean remove(Object obj, Object obj2);

    V replace(K k, V v);

    boolean replace(K k, V v, V v2);

    default V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v != null ? v : defaultValue;
    }

    default void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Objects.requireNonNull(biConsumer);
        Iterator<T> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            try {
                biConsumer.accept((K) entry.getKey(), (V) entry.getValue());
            } catch (IllegalStateException e) {
            }
        }
    }

    default void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach(new BiConsumer() {
            @Override
            public void accept(Object arg0, Object arg1) {
                ConcurrentMap.this.m337java_util_concurrent_ConcurrentMap_lambda$1(function, arg0, arg1);
            }
        });
    }

    default void m337java_util_concurrent_ConcurrentMap_lambda$1(BiFunction function, Object k, Object v) {
        while (!replace(k, v, function.apply(k, v)) && (v = get(k)) != null) {
        }
    }

    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V newValue;
        Objects.requireNonNull(mappingFunction);
        V oldValue = get(key);
        return (oldValue == null && (newValue = mappingFunction.apply(key)) != null && (oldValue = putIfAbsent(key, newValue)) == null) ? newValue : oldValue;
    }

    default V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        V vApply;
        boolean zReplace;
        Objects.requireNonNull(biFunction);
        do {
            R.bool boolVar = (Object) get(k);
            if (boolVar == 0) {
                return null;
            }
            vApply = biFunction.apply(k, boolVar);
            if (vApply == null) {
                zReplace = remove(k, boolVar);
            } else {
                zReplace = replace(k, boolVar, vApply);
            }
        } while (!zReplace);
        return vApply;
    }

    default V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        V vApply;
        while (true) {
            V v = (Object) get(k);
            do {
                vApply = biFunction.apply(k, v);
                if (vApply != null) {
                    if (v != null) {
                        if (replace(k, v, vApply)) {
                            return vApply;
                        }
                    } else {
                        v = (Object) putIfAbsent(k, vApply);
                    }
                } else if (v == null || remove(k, v)) {
                    break;
                }
            } while (v != null);
            return vApply;
        }
        return null;
    }

    default V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        Objects.requireNonNull(v);
        while (true) {
            V v2 = (Object) get(k);
            while (v2 == null) {
                v2 = (Object) putIfAbsent(k, v);
                if (v2 == null) {
                    return v;
                }
            }
            V vApply = biFunction.apply(v2, v);
            if (vApply != null) {
                if (replace(k, v2, vApply)) {
                    return vApply;
                }
            } else if (remove(k, v2)) {
                return null;
            }
        }
    }
}
