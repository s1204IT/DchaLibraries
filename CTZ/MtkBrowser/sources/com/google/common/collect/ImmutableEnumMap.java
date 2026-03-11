package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.lang.Enum;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

final class ImmutableEnumMap<K extends Enum<K>, V> extends ImmutableMap<K, V> {
    private final transient EnumMap<K, V> delegate;

    class AnonymousClass2 extends ImmutableMapEntrySet<K, V> {
        final ImmutableEnumMap this$0;

        AnonymousClass2(ImmutableEnumMap immutableEnumMap) {
            this.this$0 = immutableEnumMap;
        }

        @Override
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return (UnmodifiableIterator<Map.Entry<K, V>>) new UnmodifiableIterator<Map.Entry<K, V>>(this) {
                private final Iterator<Map.Entry<K, V>> backingIterator;
                final AnonymousClass2 this$1;

                {
                    this.this$1 = this;
                    this.backingIterator = this.this$1.this$0.delegate.entrySet().iterator();
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

        @Override
        ImmutableMap<K, V> map() {
            return this.this$0;
        }
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

    private ImmutableEnumMap(EnumMap<K, V> enumMap) {
        this.delegate = enumMap;
        Preconditions.checkArgument(!enumMap.isEmpty());
    }

    static <K extends Enum<K>, V> ImmutableMap<K, V> asImmutable(EnumMap<K, V> enumMap) {
        switch (enumMap.size()) {
            case 0:
                return ImmutableMap.of();
            case 1:
                Map.Entry entry = (Map.Entry) Iterables.getOnlyElement(enumMap.entrySet());
                return ImmutableMap.of(entry.getKey(), entry.getValue());
            default:
                return new ImmutableEnumMap(enumMap);
        }
    }

    @Override
    public boolean containsKey(Object obj) {
        return this.delegate.containsKey(obj);
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new AnonymousClass2(this);
    }

    @Override
    ImmutableSet<K> createKeySet() {
        return (ImmutableSet<K>) new ImmutableSet<K>(this) {
            final ImmutableEnumMap this$0;

            {
                this.this$0 = this;
            }

            @Override
            public boolean contains(Object obj) {
                return this.this$0.delegate.containsKey(obj);
            }

            @Override
            boolean isPartialView() {
                return true;
            }

            @Override
            public UnmodifiableIterator<K> iterator() {
                return Iterators.unmodifiableIterator(this.this$0.delegate.keySet().iterator());
            }

            @Override
            public int size() {
                return this.this$0.size();
            }
        };
    }

    @Override
    public V get(Object obj) {
        return this.delegate.get(obj);
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    Object writeReplace() {
        return new EnumSerializedForm(this.delegate);
    }
}
