package java.util.concurrent;

import android.R;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ConcurrentMap<K, V> extends Map<K, V> {
    @Override
    V putIfAbsent(K k, V v);

    @Override
    boolean remove(Object obj, Object obj2);

    @Override
    V replace(K k, V v);

    @Override
    boolean replace(K k, V v, V v2);

    @Override
    default V getOrDefault(Object obj, V v) {
        V v2 = get(obj);
        return v2 != null ? v2 : v;
    }

    @Override
    default void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Objects.requireNonNull(biConsumer);
        for (Map.Entry<K, V> entry : entrySet()) {
            try {
                biConsumer.accept(entry.getKey(), entry.getValue());
            } catch (IllegalStateException e) {
            }
        }
    }

    @Override
    default void replaceAll(final BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        forEach(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ConcurrentMap.lambda$replaceAll$0(this.f$0, biFunction, obj, obj2);
            }
        });
    }

    static void lambda$replaceAll$0(ConcurrentMap concurrentMap, BiFunction biFunction, Object obj, Object obj2) {
        while (!concurrentMap.replace(obj, obj2, biFunction.apply(obj, obj2)) && (obj2 = concurrentMap.get(obj)) != null) {
        }
    }

    @Override
    default V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
        V vApply;
        Objects.requireNonNull(function);
        V vPutIfAbsent = get(k);
        if (vPutIfAbsent == null && (vApply = function.apply(k)) != null && (vPutIfAbsent = putIfAbsent(k, vApply)) == null) {
            return vApply;
        }
        return vPutIfAbsent;
    }

    @Override
    default V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        V vApply;
        Objects.requireNonNull(biFunction);
        while (true) {
            R.bool boolVar = (Object) get(k);
            if (boolVar != 0) {
                vApply = biFunction.apply(k, boolVar);
                if (vApply == null) {
                    if (remove(k, boolVar)) {
                        break;
                    }
                } else if (replace(k, boolVar, vApply)) {
                    break;
                }
            } else {
                return null;
            }
        }
        return vApply;
    }

    @Override
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
                    return null;
                }
            } while (v != null);
            return vApply;
        }
    }

    @Override
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
