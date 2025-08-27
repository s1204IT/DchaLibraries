package com.google.common.collect;

import com.google.common.base.Ascii;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.collect.MapMakerInternalMap;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public final class MapMaker extends GenericMapMaker<Object, Object> {
    Equivalence<Object> keyEquivalence;
    MapMakerInternalMap.Strength keyStrength;
    RemovalCause nullRemovalCause;
    Ticker ticker;
    boolean useCustomMap;
    MapMakerInternalMap.Strength valueStrength;
    int initialCapacity = -1;
    int concurrencyLevel = -1;
    int maximumSize = -1;
    long expireAfterWriteNanos = -1;
    long expireAfterAccessNanos = -1;

    enum RemovalCause {
        EXPLICIT { // from class: com.google.common.collect.MapMaker.RemovalCause.1
        },
        REPLACED { // from class: com.google.common.collect.MapMaker.RemovalCause.2
        },
        COLLECTED { // from class: com.google.common.collect.MapMaker.RemovalCause.3
        },
        EXPIRED { // from class: com.google.common.collect.MapMaker.RemovalCause.4
        },
        SIZE { // from class: com.google.common.collect.MapMaker.RemovalCause.5
        }
    }

    interface RemovalListener<K, V> {
        void onRemoval(RemovalNotification<K, V> removalNotification);
    }

    MapMaker keyEquivalence(Equivalence<Object> equivalence) {
        Preconditions.checkState(this.keyEquivalence == null, "key equivalence was already set to %s", this.keyEquivalence);
        this.keyEquivalence = (Equivalence) Preconditions.checkNotNull(equivalence);
        this.useCustomMap = true;
        return this;
    }

    Equivalence<Object> getKeyEquivalence() {
        return (Equivalence) MoreObjects.firstNonNull(this.keyEquivalence, getKeyStrength().defaultEquivalence());
    }

    public MapMaker initialCapacity(int i) {
        Preconditions.checkState(this.initialCapacity == -1, "initial capacity was already set to %s", Integer.valueOf(this.initialCapacity));
        Preconditions.checkArgument(i >= 0);
        this.initialCapacity = i;
        return this;
    }

    int getInitialCapacity() {
        if (this.initialCapacity == -1) {
            return 16;
        }
        return this.initialCapacity;
    }

    @Deprecated
    MapMaker maximumSize(int i) {
        Preconditions.checkState(this.maximumSize == -1, "maximum size was already set to %s", Integer.valueOf(this.maximumSize));
        Preconditions.checkArgument(i >= 0, "maximum size must not be negative");
        this.maximumSize = i;
        this.useCustomMap = true;
        if (this.maximumSize == 0) {
            this.nullRemovalCause = RemovalCause.SIZE;
        }
        return this;
    }

    public MapMaker concurrencyLevel(int i) {
        Preconditions.checkState(this.concurrencyLevel == -1, "concurrency level was already set to %s", Integer.valueOf(this.concurrencyLevel));
        Preconditions.checkArgument(i > 0);
        this.concurrencyLevel = i;
        return this;
    }

    int getConcurrencyLevel() {
        if (this.concurrencyLevel == -1) {
            return 4;
        }
        return this.concurrencyLevel;
    }

    public MapMaker weakKeys() {
        return setKeyStrength(MapMakerInternalMap.Strength.WEAK);
    }

    MapMaker setKeyStrength(MapMakerInternalMap.Strength strength) {
        Preconditions.checkState(this.keyStrength == null, "Key strength was already set to %s", this.keyStrength);
        this.keyStrength = (MapMakerInternalMap.Strength) Preconditions.checkNotNull(strength);
        Preconditions.checkArgument(this.keyStrength != MapMakerInternalMap.Strength.SOFT, "Soft keys are not supported");
        if (strength != MapMakerInternalMap.Strength.STRONG) {
            this.useCustomMap = true;
        }
        return this;
    }

    MapMakerInternalMap.Strength getKeyStrength() {
        return (MapMakerInternalMap.Strength) MoreObjects.firstNonNull(this.keyStrength, MapMakerInternalMap.Strength.STRONG);
    }

    MapMaker setValueStrength(MapMakerInternalMap.Strength strength) {
        Preconditions.checkState(this.valueStrength == null, "Value strength was already set to %s", this.valueStrength);
        this.valueStrength = (MapMakerInternalMap.Strength) Preconditions.checkNotNull(strength);
        if (strength != MapMakerInternalMap.Strength.STRONG) {
            this.useCustomMap = true;
        }
        return this;
    }

    MapMakerInternalMap.Strength getValueStrength() {
        return (MapMakerInternalMap.Strength) MoreObjects.firstNonNull(this.valueStrength, MapMakerInternalMap.Strength.STRONG);
    }

    @Deprecated
    MapMaker expireAfterWrite(long j, TimeUnit timeUnit) {
        checkExpiration(j, timeUnit);
        this.expireAfterWriteNanos = timeUnit.toNanos(j);
        if (j == 0 && this.nullRemovalCause == null) {
            this.nullRemovalCause = RemovalCause.EXPIRED;
        }
        this.useCustomMap = true;
        return this;
    }

    private void checkExpiration(long j, TimeUnit timeUnit) {
        Preconditions.checkState(this.expireAfterWriteNanos == -1, "expireAfterWrite was already set to %s ns", Long.valueOf(this.expireAfterWriteNanos));
        Preconditions.checkState(this.expireAfterAccessNanos == -1, "expireAfterAccess was already set to %s ns", Long.valueOf(this.expireAfterAccessNanos));
        Preconditions.checkArgument(j >= 0, "duration cannot be negative: %s %s", Long.valueOf(j), timeUnit);
    }

    long getExpireAfterWriteNanos() {
        if (this.expireAfterWriteNanos == -1) {
            return 0L;
        }
        return this.expireAfterWriteNanos;
    }

    @Deprecated
    MapMaker expireAfterAccess(long j, TimeUnit timeUnit) {
        checkExpiration(j, timeUnit);
        this.expireAfterAccessNanos = timeUnit.toNanos(j);
        if (j == 0 && this.nullRemovalCause == null) {
            this.nullRemovalCause = RemovalCause.EXPIRED;
        }
        this.useCustomMap = true;
        return this;
    }

    long getExpireAfterAccessNanos() {
        if (this.expireAfterAccessNanos == -1) {
            return 0L;
        }
        return this.expireAfterAccessNanos;
    }

    Ticker getTicker() {
        return (Ticker) MoreObjects.firstNonNull(this.ticker, Ticker.systemTicker());
    }

    @Deprecated
    <K, V> GenericMapMaker<K, V> removalListener(RemovalListener<K, V> removalListener) {
        Preconditions.checkState(this.removalListener == null);
        this.removalListener = (RemovalListener) Preconditions.checkNotNull(removalListener);
        this.useCustomMap = true;
        return this;
    }

    public <K, V> ConcurrentMap<K, V> makeMap() {
        ConcurrentMap<K, V> nullConcurrentMap;
        if (!this.useCustomMap) {
            return new ConcurrentHashMap(getInitialCapacity(), 0.75f, getConcurrencyLevel());
        }
        if (this.nullRemovalCause == null) {
            nullConcurrentMap = new MapMakerInternalMap<>(this);
        } else {
            nullConcurrentMap = new NullConcurrentMap<>(this);
        }
        return nullConcurrentMap;
    }

    @Override // com.google.common.collect.GenericMapMaker
    @Deprecated
    <K, V> ConcurrentMap<K, V> makeComputingMap(Function<? super K, ? extends V> function) {
        ConcurrentMap<K, V> nullComputingConcurrentMap;
        if (this.nullRemovalCause == null) {
            nullComputingConcurrentMap = new ComputingMapAdapter<>(this, function);
        } else {
            nullComputingConcurrentMap = new NullComputingConcurrentMap<>(this, function);
        }
        return nullComputingConcurrentMap;
    }

    public String toString() {
        MoreObjects.ToStringHelper stringHelper = MoreObjects.toStringHelper(this);
        if (this.initialCapacity != -1) {
            stringHelper.add("initialCapacity", this.initialCapacity);
        }
        if (this.concurrencyLevel != -1) {
            stringHelper.add("concurrencyLevel", this.concurrencyLevel);
        }
        if (this.maximumSize != -1) {
            stringHelper.add("maximumSize", this.maximumSize);
        }
        if (this.expireAfterWriteNanos != -1) {
            stringHelper.add("expireAfterWrite", this.expireAfterWriteNanos + "ns");
        }
        if (this.expireAfterAccessNanos != -1) {
            stringHelper.add("expireAfterAccess", this.expireAfterAccessNanos + "ns");
        }
        if (this.keyStrength != null) {
            stringHelper.add("keyStrength", Ascii.toLowerCase(this.keyStrength.toString()));
        }
        if (this.valueStrength != null) {
            stringHelper.add("valueStrength", Ascii.toLowerCase(this.valueStrength.toString()));
        }
        if (this.keyEquivalence != null) {
            stringHelper.addValue("keyEquivalence");
        }
        if (this.removalListener != null) {
            stringHelper.addValue("removalListener");
        }
        return stringHelper.toString();
    }

    static final class RemovalNotification<K, V> extends ImmutableEntry<K, V> {
        private static final long serialVersionUID = 0;
        private final RemovalCause cause;

        RemovalNotification(K k, V v, RemovalCause removalCause) {
            super(k, v);
            this.cause = removalCause;
        }
    }

    static class NullConcurrentMap<K, V> extends AbstractMap<K, V> implements Serializable, ConcurrentMap<K, V> {
        private static final long serialVersionUID = 0;
        private final RemovalCause removalCause;
        private final RemovalListener<K, V> removalListener;

        NullConcurrentMap(MapMaker mapMaker) {
            this.removalListener = mapMaker.getRemovalListener();
            this.removalCause = mapMaker.nullRemovalCause;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsKey(Object obj) {
            return false;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public boolean containsValue(Object obj) {
            return false;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public V get(Object obj) {
            return null;
        }

        void notifyRemoval(K k, V v) {
            this.removalListener.onRemoval(new RemovalNotification<>(k, v, this.removalCause));
        }

        @Override // java.util.AbstractMap, java.util.Map
        public V put(K k, V v) {
            Preconditions.checkNotNull(k);
            Preconditions.checkNotNull(v);
            notifyRemoval(k, v);
            return null;
        }

        @Override // java.util.Map, java.util.concurrent.ConcurrentMap
        public V putIfAbsent(K k, V v) {
            return put(k, v);
        }

        @Override // java.util.AbstractMap, java.util.Map
        public V remove(Object obj) {
            return null;
        }

        @Override // java.util.Map, java.util.concurrent.ConcurrentMap
        public boolean remove(Object obj, Object obj2) {
            return false;
        }

        @Override // java.util.Map, java.util.concurrent.ConcurrentMap
        public V replace(K k, V v) {
            Preconditions.checkNotNull(k);
            Preconditions.checkNotNull(v);
            return null;
        }

        @Override // java.util.Map, java.util.concurrent.ConcurrentMap
        public boolean replace(K k, V v, V v2) {
            Preconditions.checkNotNull(k);
            Preconditions.checkNotNull(v2);
            return false;
        }

        @Override // java.util.AbstractMap, java.util.Map
        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    static final class NullComputingConcurrentMap<K, V> extends NullConcurrentMap<K, V> {
        private static final long serialVersionUID = 0;
        final Function<? super K, ? extends V> computingFunction;

        NullComputingConcurrentMap(MapMaker mapMaker, Function<? super K, ? extends V> function) {
            super(mapMaker);
            this.computingFunction = (Function) Preconditions.checkNotNull(function);
        }

        @Override // com.google.common.collect.MapMaker.NullConcurrentMap, java.util.AbstractMap, java.util.Map
        public V get(Object obj) {
            V vCompute = compute(obj);
            Preconditions.checkNotNull(vCompute, "%s returned null for key %s.", this.computingFunction, obj);
            notifyRemoval(obj, vCompute);
            return vCompute;
        }

        private V compute(K k) {
            Preconditions.checkNotNull(k);
            try {
                return this.computingFunction.apply(k);
            } catch (ComputationException e) {
                throw e;
            } catch (Throwable th) {
                throw new ComputationException(th);
            }
        }
    }

    static final class ComputingMapAdapter<K, V> extends ComputingConcurrentHashMap<K, V> implements Serializable {
        private static final long serialVersionUID = 0;

        ComputingMapAdapter(MapMaker mapMaker, Function<? super K, ? extends V> function) {
            super(mapMaker, function);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: java.lang.Object */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.MapMakerInternalMap, java.util.AbstractMap, java.util.Map
        public V get(Object obj) throws Throwable {
            try {
                V orCompute = getOrCompute(obj);
                if (orCompute == null) {
                    throw new NullPointerException(this.computingFunction + " returned null for key " + obj + ".");
                }
                return orCompute;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                Throwables.propagateIfInstanceOf(cause, ComputationException.class);
                throw new ComputationException(cause);
            }
        }
    }
}
