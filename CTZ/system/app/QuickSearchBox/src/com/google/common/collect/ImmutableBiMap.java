package com.google.common.collect;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/* loaded from: classes.dex */
public abstract class ImmutableBiMap<K, V> extends ImmutableMap<K, V> implements BiMap<K, V> {
    private static final Map.Entry<?, ?>[] EMPTY_ENTRY_ARRAY = new Map.Entry[0];

    public abstract ImmutableBiMap<V, K> inverse();

    public static <K, V> ImmutableBiMap<K, V> of() {
        return EmptyImmutableBiMap.INSTANCE;
    }

    public static <K, V> ImmutableBiMap<K, V> of(K k, V v) {
        return new SingletonImmutableBiMap(k, v);
    }

    public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {
        /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.lang.Object */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.ImmutableMap.Builder
        public /* bridge */ /* synthetic */ ImmutableMap.Builder put(Object obj, Object obj2) {
            return put((Builder<K, V>) obj, obj2);
        }

        @Override // com.google.common.collect.ImmutableMap.Builder
        public Builder<K, V> put(K k, V v) {
            super.put((Builder<K, V>) k, (K) v);
            return this;
        }

        /* JADX DEBUG: Method merged with bridge method: build()Lcom/google/common/collect/ImmutableMap; */
        @Override // com.google.common.collect.ImmutableMap.Builder
        public ImmutableBiMap<K, V> build() {
            switch (this.size) {
                case 0:
                    return ImmutableBiMap.of();
                case 1:
                    return ImmutableBiMap.of((Object) this.entries[0].getKey(), (Object) this.entries[0].getValue());
                default:
                    return new RegularImmutableBiMap(this.size, this.entries);
            }
        }
    }

    ImmutableBiMap() {
    }

    /* JADX DEBUG: Method merged with bridge method: values()Lcom/google/common/collect/ImmutableCollection; */
    /* JADX DEBUG: Method merged with bridge method: values()Ljava/util/Collection; */
    /* JADX DEBUG: Method merged with bridge method: values()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableMap, java.util.Map, java.util.SortedMap
    public ImmutableSet<V> values() {
        return inverse().keySet();
    }

    private static class SerializedForm extends ImmutableMap.SerializedForm {
        private static final long serialVersionUID = 0;

        SerializedForm(ImmutableBiMap<?, ?> immutableBiMap) {
            super(immutableBiMap);
        }

        @Override // com.google.common.collect.ImmutableMap.SerializedForm
        Object readResolve() {
            return createMap(new Builder());
        }
    }

    @Override // com.google.common.collect.ImmutableMap
    Object writeReplace() {
        return new SerializedForm(this);
    }
}
