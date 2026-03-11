package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.collect.MapMaker;

@Beta
@GwtCompatible(emulated = true)
@Deprecated
abstract class GenericMapMaker<K0, V0> {

    @GwtIncompatible("To be supported")
    MapMaker.RemovalListener<K0, V0> removalListener;

    @GwtIncompatible("To be supported")
    enum NullListener implements MapMaker.RemovalListener<Object, Object> {
        INSTANCE;

        public static NullListener[] valuesCustom() {
            return values();
        }

        @Override
        public void onRemoval(MapMaker.RemovalNotification<Object, Object> notification) {
        }
    }

    GenericMapMaker() {
    }

    @GwtIncompatible("To be supported")
    <K extends K0, V extends V0> MapMaker.RemovalListener<K, V> getRemovalListener() {
        return (MapMaker.RemovalListener) MoreObjects.firstNonNull(this.removalListener, NullListener.INSTANCE);
    }
}
