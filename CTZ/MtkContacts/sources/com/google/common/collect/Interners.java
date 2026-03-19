package com.google.common.collect;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMakerInternalMap;
import java.util.concurrent.ConcurrentMap;

public final class Interners {
    private Interners() {
    }

    public static <E> Interner<E> newStrongInterner() {
        final ConcurrentMap concurrentMapMakeMap = new MapMaker().makeMap();
        return new Interner<E>() {
            @Override
            public E intern(E e) {
                E e2 = (E) concurrentMapMakeMap.putIfAbsent(Preconditions.checkNotNull(e), e);
                return e2 == null ? e : e2;
            }
        };
    }

    public static <E> Interner<E> newWeakInterner() {
        return new WeakInterner();
    }

    private static class WeakInterner<E> implements Interner<E> {
        private final MapMakerInternalMap<E, Dummy> map;

        private enum Dummy {
            VALUE
        }

        private WeakInterner() {
            this.map = (MapMakerInternalMap<E, Dummy>) new MapMaker().weakKeys().keyEquivalence2(Equivalence.equals()).makeCustomMap();
        }

        @Override
        public E intern(E e) {
            E key;
            do {
                MapMakerInternalMap.ReferenceEntry<E, Dummy> entry = this.map.getEntry(e);
                if (entry != null && (key = entry.getKey()) != null) {
                    return key;
                }
            } while (this.map.putIfAbsent(e, Dummy.VALUE) != null);
            return e;
        }
    }

    public static <E> Function<E, E> asFunction(Interner<E> interner) {
        return new InternerFunction((Interner) Preconditions.checkNotNull(interner));
    }

    private static class InternerFunction<E> implements Function<E, E> {
        private final Interner<E> interner;

        public InternerFunction(Interner<E> interner) {
            this.interner = interner;
        }

        @Override
        public E apply(E e) {
            return this.interner.intern(e);
        }

        public int hashCode() {
            return this.interner.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof InternerFunction) {
                return this.interner.equals(((InternerFunction) obj).interner);
            }
            return false;
        }
    }
}
