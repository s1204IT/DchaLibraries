package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.collect.MapMaker;

public abstract class GenericMapMaker<K0, V0> {
    MapMaker.RemovalListener<K0, V0> removalListener;

    enum NullListener implements MapMaker.RemovalListener<Object, Object> {
        INSTANCE;

        @Override
        public void onRemoval(MapMaker.RemovalNotification<Object, Object> notification) {
        }
    }

    GenericMapMaker() {
    }

    <K extends K0, V extends V0> MapMaker.RemovalListener<K, V> getRemovalListener() {
        return (MapMaker.RemovalListener) Objects.firstNonNull(this.removalListener, NullListener.INSTANCE);
    }
}
