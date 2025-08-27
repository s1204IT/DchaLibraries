package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;

@Deprecated
/* loaded from: classes.dex */
abstract class GenericMapMaker<K0, V0> {
    MapMaker.RemovalListener<K0, V0> removalListener;

    @Deprecated
    abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeComputingMap(Function<? super K, ? extends V> function);

    enum NullListener implements MapMaker.RemovalListener<Object, Object> {
        INSTANCE;

        @Override // com.google.common.collect.MapMaker.RemovalListener
        public void onRemoval(MapMaker.RemovalNotification<Object, Object> removalNotification) {
        }
    }

    GenericMapMaker() {
    }

    <K extends K0, V extends V0> MapMaker.RemovalListener<K, V> getRemovalListener() {
        return (MapMaker.RemovalListener) MoreObjects.firstNonNull(this.removalListener, NullListener.INSTANCE);
    }
}
