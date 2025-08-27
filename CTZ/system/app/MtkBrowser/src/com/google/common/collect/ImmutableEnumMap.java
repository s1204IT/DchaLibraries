package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.lang.Enum;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
final class ImmutableEnumMap<K extends Enum<K>, V> extends ImmutableMap<K, V> {
    private final transient EnumMap<K, V> delegate;

    /* synthetic */ ImmutableEnumMap(EnumMap enumMap, AnonymousClass1 anonymousClass1) {
        this(enumMap);
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

    private ImmutableEnumMap(EnumMap<K, V> enumMap) {
        this.delegate = enumMap;
        Preconditions.checkArgument(!enumMap.isEmpty());
    }

    /* renamed from: com.google.common.collect.ImmutableEnumMap$1 */
    class AnonymousClass1 extends ImmutableSet<K> {
        AnonymousClass1() {
        }

        @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(Object obj) {
            return ImmutableEnumMap.this.delegate.containsKey(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return ImmutableEnumMap.this.size();
        }

        /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
        public UnmodifiableIterator<K> iterator() {
            return Iterators.unmodifiableIterator(ImmutableEnumMap.this.delegate.keySet().iterator());
        }

        @Override // com.google.common.collect.ImmutableCollection
        boolean isPartialView() {
            return true;
        }
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<K> createKeySet() {
        return new ImmutableSet<K>() { // from class: com.google.common.collect.ImmutableEnumMap.1
            AnonymousClass1() {
            }

            @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
            public boolean contains(Object obj) {
                return ImmutableEnumMap.this.delegate.containsKey(obj);
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
            public int size() {
                return ImmutableEnumMap.this.size();
            }

            /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
            @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
            public UnmodifiableIterator<K> iterator() {
                return Iterators.unmodifiableIterator(ImmutableEnumMap.this.delegate.keySet().iterator());
            }

            @Override // com.google.common.collect.ImmutableCollection
            boolean isPartialView() {
                return true;
            }
        };
    }

    @Override // java.util.Map
    public int size() {
        return this.delegate.size();
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public boolean containsKey(Object obj) {
        return this.delegate.containsKey(obj);
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public V get(Object obj) {
        return this.delegate.get(obj);
    }

    /* renamed from: com.google.common.collect.ImmutableEnumMap$2 */
    class AnonymousClass2 extends ImmutableMapEntrySet<K, V> {
        AnonymousClass2() {
        }

        @Override // com.google.common.collect.ImmutableMapEntrySet
        ImmutableMap<K, V> map() {
            return ImmutableEnumMap.this;
        }

        /* renamed from: com.google.common.collect.ImmutableEnumMap$2$1 */
        class AnonymousClass1 extends UnmodifiableIterator<Map.Entry<K, V>> {
            private final Iterator<Map.Entry<K, V>> backingIterator;

            AnonymousClass1() {
                this.backingIterator = ImmutableEnumMap.this.delegate.entrySet().iterator();
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                return this.backingIterator.hasNext();
            }

            /* JADX DEBUG: Method merged with bridge method: next()Ljava/lang/Object; */
            @Override // java.util.Iterator
            public Map.Entry<K, V> next() {
                Map.Entry<K, V> next = this.backingIterator.next();
                return Maps.immutableEntry(next.getKey(), next.getValue());
            }
        }

        /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return new UnmodifiableIterator<Map.Entry<K, V>>() { // from class: com.google.common.collect.ImmutableEnumMap.2.1
                private final Iterator<Map.Entry<K, V>> backingIterator;

                AnonymousClass1() {
                    this.backingIterator = ImmutableEnumMap.this.delegate.entrySet().iterator();
                }

                @Override // java.util.Iterator
                public boolean hasNext() {
                    return this.backingIterator.hasNext();
                }

                /* JADX DEBUG: Method merged with bridge method: next()Ljava/lang/Object; */
                @Override // java.util.Iterator
                public Map.Entry<K, V> next() {
                    Map.Entry<K, V> next = this.backingIterator.next();
                    return Maps.immutableEntry(next.getKey(), next.getValue());
                }
            };
        }
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet<K, V>() { // from class: com.google.common.collect.ImmutableEnumMap.2
            AnonymousClass2() {
            }

            @Override // com.google.common.collect.ImmutableMapEntrySet
            ImmutableMap<K, V> map() {
                return ImmutableEnumMap.this;
            }

            /* renamed from: com.google.common.collect.ImmutableEnumMap$2$1 */
            class AnonymousClass1 extends UnmodifiableIterator<Map.Entry<K, V>> {
                private final Iterator<Map.Entry<K, V>> backingIterator;

                AnonymousClass1() {
                    this.backingIterator = ImmutableEnumMap.this.delegate.entrySet().iterator();
                }

                @Override // java.util.Iterator
                public boolean hasNext() {
                    return this.backingIterator.hasNext();
                }

                /* JADX DEBUG: Method merged with bridge method: next()Ljava/lang/Object; */
                @Override // java.util.Iterator
                public Map.Entry<K, V> next() {
                    Map.Entry<K, V> next = this.backingIterator.next();
                    return Maps.immutableEntry(next.getKey(), next.getValue());
                }
            }

            /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
            @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
            public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
                return new UnmodifiableIterator<Map.Entry<K, V>>() { // from class: com.google.common.collect.ImmutableEnumMap.2.1
                    private final Iterator<Map.Entry<K, V>> backingIterator;

                    AnonymousClass1() {
                        this.backingIterator = ImmutableEnumMap.this.delegate.entrySet().iterator();
                    }

                    @Override // java.util.Iterator
                    public boolean hasNext() {
                        return this.backingIterator.hasNext();
                    }

                    /* JADX DEBUG: Method merged with bridge method: next()Ljava/lang/Object; */
                    @Override // java.util.Iterator
                    public Map.Entry<K, V> next() {
                        Map.Entry<K, V> next = this.backingIterator.next();
                        return Maps.immutableEntry(next.getKey(), next.getValue());
                    }
                };
            }
        };
    }

    @Override // com.google.common.collect.ImmutableMap
    boolean isPartialView() {
        return false;
    }

    @Override // com.google.common.collect.ImmutableMap
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
