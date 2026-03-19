package com.google.common.collect;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Deprecated
abstract class GenericMapMaker<K0, V0> {
    MapMaker.RemovalListener<K0, V0> removalListener;

    public abstract GenericMapMaker<K0, V0> concurrencyLevel(int i);

    abstract GenericMapMaker<K0, V0> expireAfterAccess(long j, TimeUnit timeUnit);

    abstract GenericMapMaker<K0, V0> expireAfterWrite(long j, TimeUnit timeUnit);

    public abstract GenericMapMaker<K0, V0> initialCapacity(int i);

    abstract GenericMapMaker<K0, V0> keyEquivalence(Equivalence<Object> equivalence);

    @Deprecated
    abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeComputingMap(Function<? super K, ? extends V> function);

    abstract <K, V> MapMakerInternalMap<K, V> makeCustomMap();

    public abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeMap();

    abstract GenericMapMaker<K0, V0> maximumSize(int i);

    @Deprecated
    public abstract GenericMapMaker<K0, V0> softValues();

    public abstract GenericMapMaker<K0, V0> weakKeys();

    public abstract GenericMapMaker<K0, V0> weakValues();

    enum NullListener implements MapMaker.RemovalListener<Object, Object> {
        INSTANCE;

        @Override
        public void onRemoval(MapMaker.RemovalNotification<Object, Object> removalNotification) {
        }
    }

    GenericMapMaker() {
    }

    <K extends K0, V extends V0> MapMaker.RemovalListener<K, V> getRemovalListener() {
        return (MapMaker.RemovalListener) MoreObjects.firstNonNull(this.removalListener, NullListener.INSTANCE);
    }
}
