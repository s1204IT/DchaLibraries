package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.io.Serializable;
import java.lang.Enum;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

final class ImmutableEnumMap<K extends Enum<K>, V> extends ImmutableMap<K, V> {
    private final transient EnumMap<K, V> delegate;

    static <K extends Enum<K>, V> ImmutableMap<K, V> asImmutable(EnumMap<K, V> enumMap) {
        switch (enumMap.size()) {
            case 0:
                return ImmutableMap.of();
            case Camera2Proxy.TEMPLATE_PREVIEW:
                Map.Entry entry = (Map.Entry) Iterables.getOnlyElement(enumMap.entrySet());
                return ImmutableMap.of(entry.getKey(), entry.getValue());
            default:
                return new ImmutableEnumMap(enumMap);
        }
    }

    private ImmutableEnumMap(EnumMap<K, V> enumMap) {
        this.delegate = enumMap;
        Preconditions.checkArgument(!enumMap.isEmpty());
    }

    @Override
    ImmutableSet<K> createKeySet() {
        return (ImmutableSet<K>) new ImmutableSet<K>() {
            @Override
            public boolean contains(Object obj) {
                return ImmutableEnumMap.this.delegate.containsKey(obj);
            }

            @Override
            public int size() {
                return ImmutableEnumMap.this.size();
            }

            @Override
            public UnmodifiableIterator<K> iterator() {
                return Iterators.unmodifiableIterator(ImmutableEnumMap.this.delegate.keySet().iterator());
            }

            @Override
            boolean isPartialView() {
                return true;
            }
        };
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean containsKey(Object obj) {
        return this.delegate.containsKey(obj);
    }

    @Override
    public V get(Object obj) {
        return this.delegate.get(obj);
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet<K, V>() {
            @Override
            ImmutableMap<K, V> map() {
                return ImmutableEnumMap.this;
            }

            @Override
            public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
                return (UnmodifiableIterator<Map.Entry<K, V>>) new UnmodifiableIterator<Map.Entry<K, V>>() {
                    private final Iterator<Map.Entry<K, V>> backingIterator;

                    {
                        this.backingIterator = ImmutableEnumMap.this.delegate.entrySet().iterator();
                    }

                    @Override
                    public boolean hasNext() {
                        return this.backingIterator.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        Map.Entry<K, V> next = this.backingIterator.next();
                        return Maps.immutableEntry(next.getKey(), next.getValue());
                    }
                };
            }
        };
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    Object writeReplace() {
        return new EnumSerializedForm(this.delegate);
    }

    private static class EnumSerializedForm<K extends Enum<K>, V> implements Serializable {
        private static final long serialVersionUID = 0;
        final EnumMap<K, V> delegate;

        EnumSerializedForm(EnumMap<K, V> enumMap) {
            this.delegate = enumMap;
        }

        Object readResolve() {
            return new ImmutableEnumMap(this.delegate);
        }
    }
}
