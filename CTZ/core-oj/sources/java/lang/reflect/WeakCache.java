package java.lang.reflect;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class WeakCache<K, P, V> {
    private final BiFunction<K, P, ?> subKeyFactory;
    private final BiFunction<K, P, V> valueFactory;
    private final ReferenceQueue<K> refQueue = new ReferenceQueue<>();
    private final ConcurrentMap<Object, ConcurrentMap<Object, Supplier<V>>> map = new ConcurrentHashMap();
    private final ConcurrentMap<Supplier<V>, Boolean> reverseMap = new ConcurrentHashMap();

    private interface Value<V> extends Supplier<V> {
    }

    public WeakCache(BiFunction<K, P, ?> biFunction, BiFunction<K, P, V> biFunction2) {
        this.subKeyFactory = (BiFunction) Objects.requireNonNull(biFunction);
        this.valueFactory = (BiFunction) Objects.requireNonNull(biFunction2);
    }

    public V get(K k, P p) {
        Objects.requireNonNull(p);
        expungeStaleEntries();
        Object objValueOf = CacheKey.valueOf(k, this.refQueue);
        ConcurrentMap<Object, Supplier<V>> concurrentMapPutIfAbsent = this.map.get(objValueOf);
        if (concurrentMapPutIfAbsent == null) {
            ConcurrentMap<Object, ConcurrentMap<Object, Supplier<V>>> concurrentMap = this.map;
            ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
            concurrentMapPutIfAbsent = concurrentMap.putIfAbsent(objValueOf, concurrentHashMap);
            if (concurrentMapPutIfAbsent == null) {
                concurrentMapPutIfAbsent = concurrentHashMap;
            }
        }
        Object objRequireNonNull = Objects.requireNonNull(this.subKeyFactory.apply(k, p));
        Supplier<V> supplierPutIfAbsent = concurrentMapPutIfAbsent.get(objRequireNonNull);
        Factory factory = null;
        while (true) {
            if (supplierPutIfAbsent != null) {
                V v = supplierPutIfAbsent.get();
                if (v != null) {
                    return v;
                }
            }
            if (factory == null) {
                factory = new Factory(k, p, objRequireNonNull, concurrentMapPutIfAbsent);
            }
            if (supplierPutIfAbsent == null) {
                supplierPutIfAbsent = concurrentMapPutIfAbsent.putIfAbsent(objRequireNonNull, factory);
                if (supplierPutIfAbsent == null) {
                    supplierPutIfAbsent = factory;
                }
            } else if (concurrentMapPutIfAbsent.replace(objRequireNonNull, supplierPutIfAbsent, factory)) {
                supplierPutIfAbsent = factory;
            } else {
                supplierPutIfAbsent = concurrentMapPutIfAbsent.get(objRequireNonNull);
            }
        }
    }

    public boolean containsValue(V v) {
        Objects.requireNonNull(v);
        expungeStaleEntries();
        return this.reverseMap.containsKey(new LookupValue(v));
    }

    public int size() {
        expungeStaleEntries();
        return this.reverseMap.size();
    }

    private void expungeStaleEntries() {
        while (true) {
            CacheKey cacheKey = (CacheKey) this.refQueue.poll();
            if (cacheKey != null) {
                cacheKey.expungeFrom(this.map, this.reverseMap);
            } else {
                return;
            }
        }
    }

    private final class Factory implements Supplier<V> {
        static final boolean $assertionsDisabled = false;
        private final K key;
        private final P parameter;
        private final Object subKey;
        private final ConcurrentMap<Object, Supplier<V>> valuesMap;

        Factory(K k, P p, Object obj, ConcurrentMap<Object, Supplier<V>> concurrentMap) {
            this.key = k;
            this.parameter = p;
            this.subKey = obj;
            this.valuesMap = concurrentMap;
        }

        @Override
        public synchronized V get() {
            if (this.valuesMap.get(this.subKey) == this) {
                try {
                    V v = (V) Objects.requireNonNull(WeakCache.this.valueFactory.apply(this.key, this.parameter));
                    if (v == null) {
                    }
                    CacheValue cacheValue = new CacheValue(v);
                    if (this.valuesMap.replace(this.subKey, this, cacheValue)) {
                        WeakCache.this.reverseMap.put(cacheValue, Boolean.TRUE);
                        return v;
                    }
                    throw new AssertionError((Object) "Should not reach here");
                } finally {
                    this.valuesMap.remove(this.subKey, this);
                }
            }
            return null;
        }
    }

    private static final class LookupValue<V> implements Value<V> {
        private final V value;

        LookupValue(V v) {
            this.value = v;
        }

        @Override
        public V get() {
            return this.value;
        }

        public int hashCode() {
            return System.identityHashCode(this.value);
        }

        public boolean equals(Object obj) {
            return obj == this || ((obj instanceof Value) && this.value == ((Value) obj).get());
        }
    }

    private static final class CacheValue<V> extends WeakReference<V> implements Value<V> {
        private final int hash;

        CacheValue(V v) {
            super(v);
            this.hash = System.identityHashCode(v);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            V v;
            return obj == this || ((obj instanceof Value) && (v = get()) != null && v == ((Value) obj).get());
        }
    }

    private static final class CacheKey<K> extends WeakReference<K> {
        private static final Object NULL_KEY = new Object();
        private final int hash;

        static <K> Object valueOf(K k, ReferenceQueue<K> referenceQueue) {
            if (k == null) {
                return NULL_KEY;
            }
            return new CacheKey(k, referenceQueue);
        }

        private CacheKey(K k, ReferenceQueue<K> referenceQueue) {
            super(k, referenceQueue);
            this.hash = System.identityHashCode(k);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            K k;
            return obj == this || (obj != null && obj.getClass() == getClass() && (k = get()) != null && k == ((CacheKey) obj).get());
        }

        void expungeFrom(ConcurrentMap<?, ? extends ConcurrentMap<?, ?>> concurrentMap, ConcurrentMap<?, Boolean> concurrentMap2) {
            ConcurrentMap<?, ?> concurrentMapRemove = concurrentMap.remove(this);
            if (concurrentMapRemove != null) {
                Iterator<?> it = concurrentMapRemove.values().iterator();
                while (it.hasNext()) {
                    concurrentMap2.remove(it.next());
                }
            }
        }
    }
}
