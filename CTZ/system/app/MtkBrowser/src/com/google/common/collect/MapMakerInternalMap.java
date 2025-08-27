package com.google.common.collect;

import com.google.common.base.Equivalence;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.collect.GenericMapMaker;
import com.google.common.collect.MapMaker;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/* loaded from: classes.dex */
class MapMakerInternalMap<K, V> extends AbstractMap<K, V> implements Serializable, ConcurrentMap<K, V> {
    private static final long serialVersionUID = 5;
    final int concurrencyLevel;
    final transient EntryFactory entryFactory;
    transient Set<Map.Entry<K, V>> entrySet;
    final long expireAfterAccessNanos;
    final long expireAfterWriteNanos;
    final Equivalence<Object> keyEquivalence;
    transient Set<K> keySet;
    final Strength keyStrength;
    final int maximumSize;
    final MapMaker.RemovalListener<K, V> removalListener;
    final Queue<MapMaker.RemovalNotification<K, V>> removalNotificationQueue;
    final transient int segmentMask;
    final transient int segmentShift;
    final transient Segment<K, V>[] segments;
    final Ticker ticker;
    final Equivalence<Object> valueEquivalence;
    final Strength valueStrength;
    transient Collection<V> values;
    private static final Logger logger = Logger.getLogger(MapMakerInternalMap.class.getName());
    static final ValueReference<Object, Object> UNSET = new ValueReference<Object, Object>() { // from class: com.google.common.collect.MapMakerInternalMap.1
        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public Object get() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ReferenceEntry<Object, Object> getEntry() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ValueReference<Object, Object> copyFor(ReferenceQueue<Object> referenceQueue, Object obj, ReferenceEntry<Object, Object> referenceEntry) {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public boolean isComputingReference() {
            return false;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public Object waitForValue() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public void clear(ValueReference<Object, Object> valueReference) {
        }
    };
    static final Queue<? extends Object> DISCARDING_QUEUE = new AbstractQueue<Object>() { // from class: com.google.common.collect.MapMakerInternalMap.2
        @Override // java.util.Queue
        public boolean offer(Object obj) {
            return true;
        }

        @Override // java.util.Queue
        public Object peek() {
            return null;
        }

        @Override // java.util.Queue
        public Object poll() {
            return null;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public int size() {
            return 0;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
        public Iterator<Object> iterator() {
            return Iterators.emptyIterator();
        }
    };

    interface ReferenceEntry<K, V> {
        long getExpirationTime();

        int getHash();

        K getKey();

        ReferenceEntry<K, V> getNext();

        ReferenceEntry<K, V> getNextEvictable();

        ReferenceEntry<K, V> getNextExpirable();

        ReferenceEntry<K, V> getPreviousEvictable();

        ReferenceEntry<K, V> getPreviousExpirable();

        ValueReference<K, V> getValueReference();

        void setExpirationTime(long j);

        void setNextEvictable(ReferenceEntry<K, V> referenceEntry);

        void setNextExpirable(ReferenceEntry<K, V> referenceEntry);

        void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry);

        void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry);

        void setValueReference(ValueReference<K, V> valueReference);
    }

    enum Strength {
        STRONG { // from class: com.google.common.collect.MapMakerInternalMap.Strength.1
            @Override // com.google.common.collect.MapMakerInternalMap.Strength
            <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V v) {
                return new StrongValueReference(v);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.Strength
            Equivalence<Object> defaultEquivalence() {
                return Equivalence.equals();
            }
        },
        SOFT { // from class: com.google.common.collect.MapMakerInternalMap.Strength.2
            @Override // com.google.common.collect.MapMakerInternalMap.Strength
            <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V v) {
                return new SoftValueReference(segment.valueReferenceQueue, v, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.Strength
            Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        },
        WEAK { // from class: com.google.common.collect.MapMakerInternalMap.Strength.3
            @Override // com.google.common.collect.MapMakerInternalMap.Strength
            <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V v) {
                return new WeakValueReference(segment.valueReferenceQueue, v, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.Strength
            Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        };

        abstract Equivalence<Object> defaultEquivalence();

        abstract <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V v);
    }

    interface ValueReference<K, V> {
        void clear(ValueReference<K, V> valueReference);

        ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry);

        V get();

        ReferenceEntry<K, V> getEntry();

        boolean isComputingReference();

        V waitForValue() throws ExecutionException;
    }

    MapMakerInternalMap(MapMaker mapMaker) {
        Queue<MapMaker.RemovalNotification<K, V>> concurrentLinkedQueue;
        this.concurrencyLevel = Math.min(mapMaker.getConcurrencyLevel(), 65536);
        this.keyStrength = mapMaker.getKeyStrength();
        this.valueStrength = mapMaker.getValueStrength();
        this.keyEquivalence = mapMaker.getKeyEquivalence();
        this.valueEquivalence = this.valueStrength.defaultEquivalence();
        this.maximumSize = mapMaker.maximumSize;
        this.expireAfterAccessNanos = mapMaker.getExpireAfterAccessNanos();
        this.expireAfterWriteNanos = mapMaker.getExpireAfterWriteNanos();
        this.entryFactory = EntryFactory.getFactory(this.keyStrength, expires(), evictsBySize());
        this.ticker = mapMaker.getTicker();
        this.removalListener = mapMaker.getRemovalListener();
        if (this.removalListener == GenericMapMaker.NullListener.INSTANCE) {
            concurrentLinkedQueue = discardingQueue();
        } else {
            concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        }
        this.removalNotificationQueue = concurrentLinkedQueue;
        int iMin = Math.min(mapMaker.getInitialCapacity(), 1073741824);
        iMin = evictsBySize() ? Math.min(iMin, this.maximumSize) : iMin;
        int i = 0;
        int i2 = 0;
        int i3 = 1;
        while (i3 < this.concurrencyLevel && (!evictsBySize() || i3 * 2 <= this.maximumSize)) {
            i2++;
            i3 <<= 1;
        }
        this.segmentShift = 32 - i2;
        this.segmentMask = i3 - 1;
        this.segments = newSegmentArray(i3);
        int i4 = iMin / i3;
        i4 = i4 * i3 < iMin ? i4 + 1 : i4;
        int i5 = 1;
        while (i5 < i4) {
            i5 <<= 1;
        }
        if (evictsBySize()) {
            int i6 = (this.maximumSize / i3) + 1;
            int i7 = this.maximumSize % i3;
            while (i < this.segments.length) {
                if (i == i7) {
                    i6--;
                }
                this.segments[i] = createSegment(i5, i6);
                i++;
            }
            return;
        }
        while (i < this.segments.length) {
            this.segments[i] = createSegment(i5, -1);
            i++;
        }
    }

    boolean evictsBySize() {
        return this.maximumSize != -1;
    }

    boolean expires() {
        return expiresAfterWrite() || expiresAfterAccess();
    }

    boolean expiresAfterWrite() {
        return this.expireAfterWriteNanos > 0;
    }

    boolean expiresAfterAccess() {
        return this.expireAfterAccessNanos > 0;
    }

    boolean usesKeyReferences() {
        return this.keyStrength != Strength.STRONG;
    }

    boolean usesValueReferences() {
        return this.valueStrength != Strength.STRONG;
    }

    enum EntryFactory {
        STRONG { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.1
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new StrongEntry(k, i, referenceEntry);
            }
        },
        STRONG_EXPIRABLE { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.2
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new StrongExpirableEntry(k, i, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
                ReferenceEntry<K, V> referenceEntryCopyEntry = super.copyEntry(segment, referenceEntry, referenceEntry2);
                copyExpirableEntry(referenceEntry, referenceEntryCopyEntry);
                return referenceEntryCopyEntry;
            }
        },
        STRONG_EVICTABLE { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.3
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new StrongEvictableEntry(k, i, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
                ReferenceEntry<K, V> referenceEntryCopyEntry = super.copyEntry(segment, referenceEntry, referenceEntry2);
                copyEvictableEntry(referenceEntry, referenceEntryCopyEntry);
                return referenceEntryCopyEntry;
            }
        },
        STRONG_EXPIRABLE_EVICTABLE { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.4
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new StrongExpirableEvictableEntry(k, i, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
                ReferenceEntry<K, V> referenceEntryCopyEntry = super.copyEntry(segment, referenceEntry, referenceEntry2);
                copyExpirableEntry(referenceEntry, referenceEntryCopyEntry);
                copyEvictableEntry(referenceEntry, referenceEntryCopyEntry);
                return referenceEntryCopyEntry;
            }
        },
        WEAK { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.5
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new WeakEntry(segment.keyReferenceQueue, k, i, referenceEntry);
            }
        },
        WEAK_EXPIRABLE { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.6
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new WeakExpirableEntry(segment.keyReferenceQueue, k, i, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
                ReferenceEntry<K, V> referenceEntryCopyEntry = super.copyEntry(segment, referenceEntry, referenceEntry2);
                copyExpirableEntry(referenceEntry, referenceEntryCopyEntry);
                return referenceEntryCopyEntry;
            }
        },
        WEAK_EVICTABLE { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.7
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new WeakEvictableEntry(segment.keyReferenceQueue, k, i, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
                ReferenceEntry<K, V> referenceEntryCopyEntry = super.copyEntry(segment, referenceEntry, referenceEntry2);
                copyEvictableEntry(referenceEntry, referenceEntryCopyEntry);
                return referenceEntryCopyEntry;
            }
        },
        WEAK_EXPIRABLE_EVICTABLE { // from class: com.google.common.collect.MapMakerInternalMap.EntryFactory.8
            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry) {
                return new WeakExpirableEvictableEntry(segment.keyReferenceQueue, k, i, referenceEntry);
            }

            @Override // com.google.common.collect.MapMakerInternalMap.EntryFactory
            <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
                ReferenceEntry<K, V> referenceEntryCopyEntry = super.copyEntry(segment, referenceEntry, referenceEntry2);
                copyExpirableEntry(referenceEntry, referenceEntryCopyEntry);
                copyEvictableEntry(referenceEntry, referenceEntryCopyEntry);
                return referenceEntryCopyEntry;
            }
        };

        static final EntryFactory[][] factories = {new EntryFactory[]{STRONG, STRONG_EXPIRABLE, STRONG_EVICTABLE, STRONG_EXPIRABLE_EVICTABLE}, new EntryFactory[0], new EntryFactory[]{WEAK, WEAK_EXPIRABLE, WEAK_EVICTABLE, WEAK_EXPIRABLE_EVICTABLE}};

        abstract <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, ReferenceEntry<K, V> referenceEntry);

        static EntryFactory getFactory(Strength strength, boolean z, boolean z2) {
            return factories[strength.ordinal()][(z ? 1 : 0) | (z2 ? 2 : 0)];
        }

        <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
            return newEntry(segment, referenceEntry.getKey(), referenceEntry.getHash(), referenceEntry2);
        }

        <K, V> void copyExpirableEntry(ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
            referenceEntry2.setExpirationTime(referenceEntry.getExpirationTime());
            MapMakerInternalMap.connectExpirables(referenceEntry.getPreviousExpirable(), referenceEntry2);
            MapMakerInternalMap.connectExpirables(referenceEntry2, referenceEntry.getNextExpirable());
            MapMakerInternalMap.nullifyExpirable(referenceEntry);
        }

        <K, V> void copyEvictableEntry(ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
            MapMakerInternalMap.connectEvictables(referenceEntry.getPreviousEvictable(), referenceEntry2);
            MapMakerInternalMap.connectEvictables(referenceEntry2, referenceEntry.getNextEvictable());
            MapMakerInternalMap.nullifyEvictable(referenceEntry);
        }
    }

    static <K, V> ValueReference<K, V> unset() {
        return (ValueReference<K, V>) UNSET;
    }

    private enum NullEntry implements ReferenceEntry<Object, Object> {
        INSTANCE;

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ValueReference<Object, Object> getValueReference() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setValueReference(ValueReference<Object, Object> valueReference) {
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<Object, Object> getNext() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public int getHash() {
            return 0;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public Object getKey() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            return 0L;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<Object, Object> getNextExpirable() {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<Object, Object> referenceEntry) {
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<Object, Object> getPreviousExpirable() {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<Object, Object> referenceEntry) {
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<Object, Object> getNextEvictable() {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<Object, Object> referenceEntry) {
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<Object, Object> getPreviousEvictable() {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<Object, Object> referenceEntry) {
        }
    }

    static abstract class AbstractReferenceEntry<K, V> implements ReferenceEntry<K, V> {
        AbstractReferenceEntry() {
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ValueReference<K, V> getValueReference() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setValueReference(ValueReference<K, V> valueReference) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNext() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public int getHash() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public K getKey() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }
    }

    static <K, V> ReferenceEntry<K, V> nullEntry() {
        return NullEntry.INSTANCE;
    }

    static <E> Queue<E> discardingQueue() {
        return (Queue<E>) DISCARDING_QUEUE;
    }

    static class StrongEntry<K, V> implements ReferenceEntry<K, V> {
        final int hash;
        final K key;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = MapMakerInternalMap.unset();

        StrongEntry(K k, int i, ReferenceEntry<K, V> referenceEntry) {
            this.key = k;
            this.hash = i;
            this.next = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public K getKey() {
            return this.key;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ValueReference<K, V> getValueReference() {
            return this.valueReference;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> valueReference2 = this.valueReference;
            this.valueReference = valueReference;
            valueReference2.clear(valueReference);
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public int getHash() {
            return this.hash;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNext() {
            return this.next;
        }
    }

    static final class StrongExpirableEntry<K, V> extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextExpirable;
        ReferenceEntry<K, V> previousExpirable;
        volatile long time;

        StrongExpirableEntry(K k, int i, ReferenceEntry<K, V> referenceEntry) {
            super(k, i, referenceEntry);
            this.time = Long.MAX_VALUE;
            this.nextExpirable = MapMakerInternalMap.nullEntry();
            this.previousExpirable = MapMakerInternalMap.nullEntry();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            return this.time;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
            this.time = j;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.nextExpirable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.previousExpirable = referenceEntry;
        }
    }

    static final class StrongEvictableEntry<K, V> extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable;
        ReferenceEntry<K, V> previousEvictable;

        StrongEvictableEntry(K k, int i, ReferenceEntry<K, V> referenceEntry) {
            super(k, i, referenceEntry);
            this.nextEvictable = MapMakerInternalMap.nullEntry();
            this.previousEvictable = MapMakerInternalMap.nullEntry();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.nextEvictable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.previousEvictable = referenceEntry;
        }
    }

    static final class StrongExpirableEvictableEntry<K, V> extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable;
        ReferenceEntry<K, V> nextExpirable;
        ReferenceEntry<K, V> previousEvictable;
        ReferenceEntry<K, V> previousExpirable;
        volatile long time;

        StrongExpirableEvictableEntry(K k, int i, ReferenceEntry<K, V> referenceEntry) {
            super(k, i, referenceEntry);
            this.time = Long.MAX_VALUE;
            this.nextExpirable = MapMakerInternalMap.nullEntry();
            this.previousExpirable = MapMakerInternalMap.nullEntry();
            this.nextEvictable = MapMakerInternalMap.nullEntry();
            this.previousEvictable = MapMakerInternalMap.nullEntry();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            return this.time;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
            this.time = j;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.nextExpirable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.previousExpirable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.nextEvictable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.StrongEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.previousEvictable = referenceEntry;
        }
    }

    static class WeakEntry<K, V> extends WeakReference<K> implements ReferenceEntry<K, V> {
        final int hash;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference;

        WeakEntry(ReferenceQueue<K> referenceQueue, K k, int i, ReferenceEntry<K, V> referenceEntry) {
            super(k, referenceQueue);
            this.valueReference = MapMakerInternalMap.unset();
            this.hash = i;
            this.next = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public K getKey() {
            return (K) get();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ValueReference<K, V> getValueReference() {
            return this.valueReference;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> valueReference2 = this.valueReference;
            this.valueReference = valueReference;
            valueReference2.clear(valueReference);
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public int getHash() {
            return this.hash;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNext() {
            return this.next;
        }
    }

    static final class WeakExpirableEntry<K, V> extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextExpirable;
        ReferenceEntry<K, V> previousExpirable;
        volatile long time;

        WeakExpirableEntry(ReferenceQueue<K> referenceQueue, K k, int i, ReferenceEntry<K, V> referenceEntry) {
            super(referenceQueue, k, i, referenceEntry);
            this.time = Long.MAX_VALUE;
            this.nextExpirable = MapMakerInternalMap.nullEntry();
            this.previousExpirable = MapMakerInternalMap.nullEntry();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            return this.time;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
            this.time = j;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.nextExpirable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.previousExpirable = referenceEntry;
        }
    }

    static final class WeakEvictableEntry<K, V> extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable;
        ReferenceEntry<K, V> previousEvictable;

        WeakEvictableEntry(ReferenceQueue<K> referenceQueue, K k, int i, ReferenceEntry<K, V> referenceEntry) {
            super(referenceQueue, k, i, referenceEntry);
            this.nextEvictable = MapMakerInternalMap.nullEntry();
            this.previousEvictable = MapMakerInternalMap.nullEntry();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.nextEvictable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.previousEvictable = referenceEntry;
        }
    }

    static final class WeakExpirableEvictableEntry<K, V> extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable;
        ReferenceEntry<K, V> nextExpirable;
        ReferenceEntry<K, V> previousEvictable;
        ReferenceEntry<K, V> previousExpirable;
        volatile long time;

        WeakExpirableEvictableEntry(ReferenceQueue<K> referenceQueue, K k, int i, ReferenceEntry<K, V> referenceEntry) {
            super(referenceQueue, k, i, referenceEntry);
            this.time = Long.MAX_VALUE;
            this.nextExpirable = MapMakerInternalMap.nullEntry();
            this.previousExpirable = MapMakerInternalMap.nullEntry();
            this.nextEvictable = MapMakerInternalMap.nullEntry();
            this.previousEvictable = MapMakerInternalMap.nullEntry();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public long getExpirationTime() {
            return this.time;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setExpirationTime(long j) {
            this.time = j;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.nextExpirable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            this.previousExpirable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.nextEvictable = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.WeakEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            this.previousEvictable = referenceEntry;
        }
    }

    static final class WeakValueReference<K, V> extends WeakReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        WeakValueReference(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            super(v, referenceQueue);
            this.entry = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ReferenceEntry<K, V> getEntry() {
            return this.entry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public void clear(ValueReference<K, V> valueReference) {
            clear();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            return new WeakValueReference(referenceQueue, v, referenceEntry);
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public boolean isComputingReference() {
            return false;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V waitForValue() {
            return get();
        }
    }

    static final class SoftValueReference<K, V> extends SoftReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        SoftValueReference(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            super(v, referenceQueue);
            this.entry = referenceEntry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ReferenceEntry<K, V> getEntry() {
            return this.entry;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public void clear(ValueReference<K, V> valueReference) {
            clear();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            return new SoftValueReference(referenceQueue, v, referenceEntry);
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public boolean isComputingReference() {
            return false;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V waitForValue() {
            return get();
        }
    }

    static final class StrongValueReference<K, V> implements ValueReference<K, V> {
        final V referent;

        StrongValueReference(V v) {
            this.referent = v;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V get() {
            return this.referent;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public boolean isComputingReference() {
            return false;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V waitForValue() {
            return get();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public void clear(ValueReference<K, V> valueReference) {
        }
    }

    static int rehash(int i) {
        int i2 = i + ((i << 15) ^ (-12931));
        int i3 = i2 ^ (i2 >>> 10);
        int i4 = i3 + (i3 << 3);
        int i5 = i4 ^ (i4 >>> 6);
        int i6 = i5 + (i5 << 2) + (i5 << 14);
        return i6 ^ (i6 >>> 16);
    }

    ReferenceEntry<K, V> newEntry(K k, int i, ReferenceEntry<K, V> referenceEntry) {
        return segmentFor(i).newEntry(k, i, referenceEntry);
    }

    ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
        return segmentFor(referenceEntry.getHash()).copyEntry(referenceEntry, referenceEntry2);
    }

    ValueReference<K, V> newValueReference(ReferenceEntry<K, V> referenceEntry, V v) {
        return this.valueStrength.referenceValue(segmentFor(referenceEntry.getHash()), referenceEntry, v);
    }

    int hash(Object obj) {
        return rehash(this.keyEquivalence.hash(obj));
    }

    void reclaimValue(ValueReference<K, V> valueReference) {
        ReferenceEntry<K, V> entry = valueReference.getEntry();
        int hash = entry.getHash();
        segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
    }

    void reclaimKey(ReferenceEntry<K, V> referenceEntry) {
        int hash = referenceEntry.getHash();
        segmentFor(hash).reclaimKey(referenceEntry, hash);
    }

    boolean isLive(ReferenceEntry<K, V> referenceEntry) {
        return segmentFor(referenceEntry.getHash()).getLiveValue(referenceEntry) != null;
    }

    Segment<K, V> segmentFor(int i) {
        return this.segments[(i >>> this.segmentShift) & this.segmentMask];
    }

    Segment<K, V> createSegment(int i, int i2) {
        return new Segment<>(this, i, i2);
    }

    V getLiveValue(ReferenceEntry<K, V> referenceEntry) {
        V v;
        if (referenceEntry.getKey() == null || (v = referenceEntry.getValueReference().get()) == null) {
            return null;
        }
        if (expires() && isExpired(referenceEntry)) {
            return null;
        }
        return v;
    }

    boolean isExpired(ReferenceEntry<K, V> referenceEntry) {
        return isExpired(referenceEntry, this.ticker.read());
    }

    boolean isExpired(ReferenceEntry<K, V> referenceEntry, long j) {
        return j - referenceEntry.getExpirationTime() > 0;
    }

    static <K, V> void connectExpirables(ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
        referenceEntry.setNextExpirable(referenceEntry2);
        referenceEntry2.setPreviousExpirable(referenceEntry);
    }

    static <K, V> void nullifyExpirable(ReferenceEntry<K, V> referenceEntry) {
        ReferenceEntry<K, V> referenceEntryNullEntry = nullEntry();
        referenceEntry.setNextExpirable(referenceEntryNullEntry);
        referenceEntry.setPreviousExpirable(referenceEntryNullEntry);
    }

    void processPendingNotifications() {
        while (true) {
            MapMaker.RemovalNotification<K, V> removalNotificationPoll = this.removalNotificationQueue.poll();
            if (removalNotificationPoll != null) {
                try {
                    this.removalListener.onRemoval(removalNotificationPoll);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception thrown by removal listener", (Throwable) e);
                }
            } else {
                return;
            }
        }
    }

    static <K, V> void connectEvictables(ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
        referenceEntry.setNextEvictable(referenceEntry2);
        referenceEntry2.setPreviousEvictable(referenceEntry);
    }

    static <K, V> void nullifyEvictable(ReferenceEntry<K, V> referenceEntry) {
        ReferenceEntry<K, V> referenceEntryNullEntry = nullEntry();
        referenceEntry.setNextEvictable(referenceEntryNullEntry);
        referenceEntry.setPreviousEvictable(referenceEntryNullEntry);
    }

    final Segment<K, V>[] newSegmentArray(int i) {
        return new Segment[i];
    }

    static class Segment<K, V> extends ReentrantLock {
        volatile int count;
        final Queue<ReferenceEntry<K, V>> evictionQueue;
        final Queue<ReferenceEntry<K, V>> expirationQueue;
        final ReferenceQueue<K> keyReferenceQueue;
        final MapMakerInternalMap<K, V> map;
        final int maxSegmentSize;
        int modCount;
        final AtomicInteger readCount = new AtomicInteger();
        final Queue<ReferenceEntry<K, V>> recencyQueue;
        volatile AtomicReferenceArray<ReferenceEntry<K, V>> table;
        int threshold;
        final ReferenceQueue<V> valueReferenceQueue;

        Segment(MapMakerInternalMap<K, V> mapMakerInternalMap, int i, int i2) {
            Queue<ReferenceEntry<K, V>> concurrentLinkedQueue;
            Queue<ReferenceEntry<K, V>> queueDiscardingQueue;
            Queue<ReferenceEntry<K, V>> queueDiscardingQueue2;
            this.map = mapMakerInternalMap;
            this.maxSegmentSize = i2;
            initTable(newEntryArray(i));
            this.keyReferenceQueue = mapMakerInternalMap.usesKeyReferences() ? new ReferenceQueue<>() : null;
            this.valueReferenceQueue = mapMakerInternalMap.usesValueReferences() ? new ReferenceQueue<>() : null;
            if (mapMakerInternalMap.evictsBySize() || mapMakerInternalMap.expiresAfterAccess()) {
                concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
            } else {
                concurrentLinkedQueue = MapMakerInternalMap.discardingQueue();
            }
            this.recencyQueue = concurrentLinkedQueue;
            if (mapMakerInternalMap.evictsBySize()) {
                queueDiscardingQueue = new EvictionQueue<>();
            } else {
                queueDiscardingQueue = MapMakerInternalMap.discardingQueue();
            }
            this.evictionQueue = queueDiscardingQueue;
            if (mapMakerInternalMap.expires()) {
                queueDiscardingQueue2 = new ExpirationQueue<>();
            } else {
                queueDiscardingQueue2 = MapMakerInternalMap.discardingQueue();
            }
            this.expirationQueue = queueDiscardingQueue2;
        }

        AtomicReferenceArray<ReferenceEntry<K, V>> newEntryArray(int i) {
            return new AtomicReferenceArray<>(i);
        }

        void initTable(AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray) {
            this.threshold = (atomicReferenceArray.length() * 3) / 4;
            if (this.threshold == this.maxSegmentSize) {
                this.threshold++;
            }
            this.table = atomicReferenceArray;
        }

        ReferenceEntry<K, V> newEntry(K k, int i, ReferenceEntry<K, V> referenceEntry) {
            return this.map.entryFactory.newEntry(this, k, i, referenceEntry);
        }

        ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
            if (referenceEntry.getKey() == null) {
                return null;
            }
            ValueReference<K, V> valueReference = referenceEntry.getValueReference();
            V v = valueReference.get();
            if (v == null && !valueReference.isComputingReference()) {
                return null;
            }
            ReferenceEntry<K, V> referenceEntryCopyEntry = this.map.entryFactory.copyEntry(this, referenceEntry, referenceEntry2);
            referenceEntryCopyEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, v, referenceEntryCopyEntry));
            return referenceEntryCopyEntry;
        }

        void setValue(ReferenceEntry<K, V> referenceEntry, V v) {
            referenceEntry.setValueReference(this.map.valueStrength.referenceValue(this, referenceEntry, v));
            recordWrite(referenceEntry);
        }

        void tryDrainReferenceQueues() {
            if (tryLock()) {
                try {
                    drainReferenceQueues();
                } finally {
                    unlock();
                }
            }
        }

        void drainReferenceQueues() {
            if (this.map.usesKeyReferences()) {
                drainKeyReferenceQueue();
            }
            if (this.map.usesValueReferences()) {
                drainValueReferenceQueue();
            }
        }

        void drainKeyReferenceQueue() {
            int i = 0;
            do {
                Reference<? extends K> referencePoll = this.keyReferenceQueue.poll();
                if (referencePoll != null) {
                    this.map.reclaimKey((ReferenceEntry) referencePoll);
                    i++;
                } else {
                    return;
                }
            } while (i != 16);
        }

        void drainValueReferenceQueue() {
            int i = 0;
            do {
                Reference<? extends V> referencePoll = this.valueReferenceQueue.poll();
                if (referencePoll != null) {
                    this.map.reclaimValue((ValueReference) referencePoll);
                    i++;
                } else {
                    return;
                }
            } while (i != 16);
        }

        void clearReferenceQueues() {
            if (this.map.usesKeyReferences()) {
                clearKeyReferenceQueue();
            }
            if (this.map.usesValueReferences()) {
                clearValueReferenceQueue();
            }
        }

        void clearKeyReferenceQueue() {
            while (this.keyReferenceQueue.poll() != null) {
            }
        }

        void clearValueReferenceQueue() {
            while (this.valueReferenceQueue.poll() != null) {
            }
        }

        void recordRead(ReferenceEntry<K, V> referenceEntry) {
            if (this.map.expiresAfterAccess()) {
                recordExpirationTime(referenceEntry, this.map.expireAfterAccessNanos);
            }
            this.recencyQueue.add(referenceEntry);
        }

        void recordLockedRead(ReferenceEntry<K, V> referenceEntry) {
            this.evictionQueue.add(referenceEntry);
            if (this.map.expiresAfterAccess()) {
                recordExpirationTime(referenceEntry, this.map.expireAfterAccessNanos);
                this.expirationQueue.add(referenceEntry);
            }
        }

        void recordWrite(ReferenceEntry<K, V> referenceEntry) {
            long j;
            drainRecencyQueue();
            this.evictionQueue.add(referenceEntry);
            if (this.map.expires()) {
                if (this.map.expiresAfterAccess()) {
                    j = this.map.expireAfterAccessNanos;
                } else {
                    j = this.map.expireAfterWriteNanos;
                }
                recordExpirationTime(referenceEntry, j);
                this.expirationQueue.add(referenceEntry);
            }
        }

        void drainRecencyQueue() {
            while (true) {
                ReferenceEntry<K, V> referenceEntryPoll = this.recencyQueue.poll();
                if (referenceEntryPoll != null) {
                    if (this.evictionQueue.contains(referenceEntryPoll)) {
                        this.evictionQueue.add(referenceEntryPoll);
                    }
                    if (this.map.expiresAfterAccess() && this.expirationQueue.contains(referenceEntryPoll)) {
                        this.expirationQueue.add(referenceEntryPoll);
                    }
                } else {
                    return;
                }
            }
        }

        void recordExpirationTime(ReferenceEntry<K, V> referenceEntry, long j) {
            referenceEntry.setExpirationTime(this.map.ticker.read() + j);
        }

        void tryExpireEntries() {
            if (tryLock()) {
                try {
                    expireEntries();
                } finally {
                    unlock();
                }
            }
        }

        void expireEntries() {
            ReferenceEntry<K, V> referenceEntryPeek;
            drainRecencyQueue();
            if (this.expirationQueue.isEmpty()) {
                return;
            }
            long j = this.map.ticker.read();
            do {
                referenceEntryPeek = this.expirationQueue.peek();
                if (referenceEntryPeek == null || !this.map.isExpired(referenceEntryPeek, j)) {
                    return;
                }
            } while (removeEntry(referenceEntryPeek, referenceEntryPeek.getHash(), MapMaker.RemovalCause.EXPIRED));
            throw new AssertionError();
        }

        void enqueueNotification(ReferenceEntry<K, V> referenceEntry, MapMaker.RemovalCause removalCause) {
            enqueueNotification(referenceEntry.getKey(), referenceEntry.getHash(), referenceEntry.getValueReference().get(), removalCause);
        }

        void enqueueNotification(K k, int i, V v, MapMaker.RemovalCause removalCause) {
            if (this.map.removalNotificationQueue != MapMakerInternalMap.DISCARDING_QUEUE) {
                this.map.removalNotificationQueue.offer(new MapMaker.RemovalNotification<>(k, v, removalCause));
            }
        }

        boolean evictEntries() {
            if (this.map.evictsBySize() && this.count >= this.maxSegmentSize) {
                drainRecencyQueue();
                ReferenceEntry<K, V> referenceEntryRemove = this.evictionQueue.remove();
                if (!removeEntry(referenceEntryRemove, referenceEntryRemove.getHash(), MapMaker.RemovalCause.SIZE)) {
                    throw new AssertionError();
                }
                return true;
            }
            return false;
        }

        ReferenceEntry<K, V> getFirst(int i) {
            return this.table.get(i & (r0.length() - 1));
        }

        ReferenceEntry<K, V> getEntry(Object obj, int i) {
            if (this.count != 0) {
                for (ReferenceEntry<K, V> first = getFirst(i); first != null; first = first.getNext()) {
                    if (first.getHash() == i) {
                        K key = first.getKey();
                        if (key == null) {
                            tryDrainReferenceQueues();
                        } else if (this.map.keyEquivalence.equivalent(obj, key)) {
                            return first;
                        }
                    }
                }
                return null;
            }
            return null;
        }

        ReferenceEntry<K, V> getLiveEntry(Object obj, int i) {
            ReferenceEntry<K, V> entry = getEntry(obj, i);
            if (entry == null) {
                return null;
            }
            if (this.map.expires() && this.map.isExpired(entry)) {
                tryExpireEntries();
                return null;
            }
            return entry;
        }

        V get(Object obj, int i) {
            try {
                ReferenceEntry<K, V> liveEntry = getLiveEntry(obj, i);
                if (liveEntry != null) {
                    V v = liveEntry.getValueReference().get();
                    if (v != null) {
                        recordRead(liveEntry);
                    } else {
                        tryDrainReferenceQueues();
                    }
                    return v;
                }
                return null;
            } finally {
                postReadCleanup();
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2464=4] */
        boolean containsKey(Object obj, int i) {
            try {
                if (this.count == 0) {
                    return false;
                }
                ReferenceEntry<K, V> liveEntry = getLiveEntry(obj, i);
                if (liveEntry == null) {
                    return false;
                }
                return liveEntry.getValueReference().get() != null;
            } finally {
                postReadCleanup();
            }
        }

        boolean containsValue(Object obj) {
            try {
                if (this.count != 0) {
                    AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                    int length = atomicReferenceArray.length();
                    for (int i = 0; i < length; i++) {
                        for (ReferenceEntry<K, V> next = atomicReferenceArray.get(i); next != null; next = next.getNext()) {
                            V liveValue = getLiveValue(next);
                            if (liveValue != null && this.map.valueEquivalence.equivalent(obj, liveValue)) {
                                postReadCleanup();
                                return true;
                            }
                        }
                    }
                }
                return false;
            } finally {
                postReadCleanup();
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2560=5, 2561=5] */
        V put(K k, int i, V v, boolean z) {
            lock();
            try {
                preWriteCleanup();
                int i2 = this.count + 1;
                if (i2 > this.threshold) {
                    expand();
                    i2 = this.count + 1;
                }
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry; next != null; next = next.getNext()) {
                    K key = next.getKey();
                    if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(k, key)) {
                        ValueReference<K, V> valueReference = next.getValueReference();
                        V v2 = valueReference.get();
                        if (v2 != null) {
                            if (z) {
                                recordLockedRead(next);
                                return v2;
                            }
                            this.modCount++;
                            enqueueNotification(k, i, v2, MapMaker.RemovalCause.REPLACED);
                            setValue(next, v);
                            return v2;
                        }
                        this.modCount++;
                        setValue(next, v);
                        if (!valueReference.isComputingReference()) {
                            enqueueNotification(k, i, v2, MapMaker.RemovalCause.COLLECTED);
                            i2 = this.count;
                        } else if (evictEntries()) {
                            i2 = this.count + 1;
                        }
                        this.count = i2;
                        return null;
                    }
                }
                this.modCount++;
                ReferenceEntry<K, V> referenceEntryNewEntry = newEntry(k, i, referenceEntry);
                setValue(referenceEntryNewEntry, v);
                atomicReferenceArray.set(length, referenceEntryNewEntry);
                if (evictEntries()) {
                    i2 = this.count + 1;
                }
                this.count = i2;
                return null;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        void expand() {
            AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
            int length = atomicReferenceArray.length();
            if (length >= 1073741824) {
                return;
            }
            int i = this.count;
            AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArrayNewEntryArray = newEntryArray(length << 1);
            this.threshold = (atomicReferenceArrayNewEntryArray.length() * 3) / 4;
            int length2 = atomicReferenceArrayNewEntryArray.length() - 1;
            for (int i2 = 0; i2 < length; i2++) {
                ReferenceEntry<K, V> next = atomicReferenceArray.get(i2);
                if (next != null) {
                    ReferenceEntry<K, V> next2 = next.getNext();
                    int hash = next.getHash() & length2;
                    if (next2 == null) {
                        atomicReferenceArrayNewEntryArray.set(hash, next);
                    } else {
                        ReferenceEntry<K, V> referenceEntry = next;
                        while (next2 != null) {
                            int hash2 = next2.getHash() & length2;
                            if (hash2 != hash) {
                                referenceEntry = next2;
                                hash = hash2;
                            }
                            next2 = next2.getNext();
                        }
                        atomicReferenceArrayNewEntryArray.set(hash, referenceEntry);
                        while (next != referenceEntry) {
                            int hash3 = next.getHash() & length2;
                            ReferenceEntry<K, V> referenceEntryCopyEntry = copyEntry(next, atomicReferenceArrayNewEntryArray.get(hash3));
                            if (referenceEntryCopyEntry != null) {
                                atomicReferenceArrayNewEntryArray.set(hash3, referenceEntryCopyEntry);
                            } else {
                                removeCollectedEntry(next);
                                i--;
                            }
                            next = next.getNext();
                        }
                    }
                }
            }
            this.table = atomicReferenceArrayNewEntryArray;
            this.count = i;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2683=5, 2684=5] */
        boolean replace(K k, int i, V v, V v2) {
            lock();
            try {
                preWriteCleanup();
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry; next != null; next = next.getNext()) {
                    K key = next.getKey();
                    if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(k, key)) {
                        ValueReference<K, V> valueReference = next.getValueReference();
                        V v3 = valueReference.get();
                        if (v3 != null) {
                            if (!this.map.valueEquivalence.equivalent(v, v3)) {
                                recordLockedRead(next);
                                return false;
                            }
                            this.modCount++;
                            enqueueNotification(k, i, v3, MapMaker.RemovalCause.REPLACED);
                            setValue(next, v2);
                            return true;
                        }
                        if (isCollected(valueReference)) {
                            int i2 = this.count;
                            this.modCount++;
                            enqueueNotification(key, i, v3, MapMaker.RemovalCause.COLLECTED);
                            ReferenceEntry<K, V> referenceEntryRemoveFromChain = removeFromChain(referenceEntry, next);
                            int i3 = this.count - 1;
                            atomicReferenceArray.set(length, referenceEntryRemoveFromChain);
                            this.count = i3;
                        }
                        return false;
                    }
                }
                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2727=4, 2728=4] */
        V replace(K k, int i, V v) {
            lock();
            try {
                preWriteCleanup();
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry; next != null; next = next.getNext()) {
                    K key = next.getKey();
                    if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(k, key)) {
                        ValueReference<K, V> valueReference = next.getValueReference();
                        V v2 = valueReference.get();
                        if (v2 != null) {
                            this.modCount++;
                            enqueueNotification(k, i, v2, MapMaker.RemovalCause.REPLACED);
                            setValue(next, v);
                            return v2;
                        }
                        if (isCollected(valueReference)) {
                            int i2 = this.count;
                            this.modCount++;
                            enqueueNotification(key, i, v2, MapMaker.RemovalCause.COLLECTED);
                            ReferenceEntry<K, V> referenceEntryRemoveFromChain = removeFromChain(referenceEntry, next);
                            int i3 = this.count - 1;
                            atomicReferenceArray.set(length, referenceEntryRemoveFromChain);
                            this.count = i3;
                        }
                        return null;
                    }
                }
                return null;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2770=4, 2771=4] */
        V remove(Object obj, int i) {
            MapMaker.RemovalCause removalCause;
            lock();
            try {
                preWriteCleanup();
                int i2 = this.count;
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry; next != null; next = next.getNext()) {
                    K key = next.getKey();
                    if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(obj, key)) {
                        ValueReference<K, V> valueReference = next.getValueReference();
                        V v = valueReference.get();
                        if (v != null) {
                            removalCause = MapMaker.RemovalCause.EXPLICIT;
                        } else {
                            if (!isCollected(valueReference)) {
                                return null;
                            }
                            removalCause = MapMaker.RemovalCause.COLLECTED;
                        }
                        this.modCount++;
                        enqueueNotification(key, i, v, removalCause);
                        ReferenceEntry<K, V> referenceEntryRemoveFromChain = removeFromChain(referenceEntry, next);
                        int i3 = this.count - 1;
                        atomicReferenceArray.set(length, referenceEntryRemoveFromChain);
                        this.count = i3;
                        return v;
                    }
                }
                return null;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2813=4, 2814=4] */
        boolean remove(Object obj, int i, Object obj2) {
            MapMaker.RemovalCause removalCause;
            lock();
            try {
                preWriteCleanup();
                int i2 = this.count;
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry; next != null; next = next.getNext()) {
                    K key = next.getKey();
                    if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(obj, key)) {
                        ValueReference<K, V> valueReference = next.getValueReference();
                        V v = valueReference.get();
                        if (this.map.valueEquivalence.equivalent(obj2, v)) {
                            removalCause = MapMaker.RemovalCause.EXPLICIT;
                        } else {
                            if (!isCollected(valueReference)) {
                                return false;
                            }
                            removalCause = MapMaker.RemovalCause.COLLECTED;
                        }
                        this.modCount++;
                        enqueueNotification(key, i, v, removalCause);
                        ReferenceEntry<K, V> referenceEntryRemoveFromChain = removeFromChain(referenceEntry, next);
                        int i3 = this.count - 1;
                        atomicReferenceArray.set(length, referenceEntryRemoveFromChain);
                        this.count = i3;
                        return removalCause == MapMaker.RemovalCause.EXPLICIT;
                    }
                }
                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        void clear() {
            if (this.count != 0) {
                lock();
                try {
                    AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                    if (this.map.removalNotificationQueue != MapMakerInternalMap.DISCARDING_QUEUE) {
                        for (int i = 0; i < atomicReferenceArray.length(); i++) {
                            for (ReferenceEntry<K, V> next = atomicReferenceArray.get(i); next != null; next = next.getNext()) {
                                if (!next.getValueReference().isComputingReference()) {
                                    enqueueNotification(next, MapMaker.RemovalCause.EXPLICIT);
                                }
                            }
                        }
                    }
                    for (int i2 = 0; i2 < atomicReferenceArray.length(); i2++) {
                        atomicReferenceArray.set(i2, null);
                    }
                    clearReferenceQueues();
                    this.evictionQueue.clear();
                    this.expirationQueue.clear();
                    this.readCount.set(0);
                    this.modCount++;
                    this.count = 0;
                } finally {
                    unlock();
                    postWriteCleanup();
                }
            }
        }

        ReferenceEntry<K, V> removeFromChain(ReferenceEntry<K, V> referenceEntry, ReferenceEntry<K, V> referenceEntry2) {
            this.evictionQueue.remove(referenceEntry2);
            this.expirationQueue.remove(referenceEntry2);
            int i = this.count;
            ReferenceEntry<K, V> next = referenceEntry2.getNext();
            while (referenceEntry != referenceEntry2) {
                ReferenceEntry<K, V> referenceEntryCopyEntry = copyEntry(referenceEntry, next);
                if (referenceEntryCopyEntry != null) {
                    next = referenceEntryCopyEntry;
                } else {
                    removeCollectedEntry(referenceEntry);
                    i--;
                }
                referenceEntry = referenceEntry.getNext();
            }
            this.count = i;
            return next;
        }

        void removeCollectedEntry(ReferenceEntry<K, V> referenceEntry) {
            enqueueNotification(referenceEntry, MapMaker.RemovalCause.COLLECTED);
            this.evictionQueue.remove(referenceEntry);
            this.expirationQueue.remove(referenceEntry);
        }

        boolean reclaimKey(ReferenceEntry<K, V> referenceEntry, int i) {
            lock();
            try {
                int i2 = this.count;
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry2 = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry2; next != null; next = next.getNext()) {
                    if (next == referenceEntry) {
                        this.modCount++;
                        enqueueNotification(next.getKey(), i, next.getValueReference().get(), MapMaker.RemovalCause.COLLECTED);
                        ReferenceEntry<K, V> referenceEntryRemoveFromChain = removeFromChain(referenceEntry2, next);
                        int i3 = this.count - 1;
                        atomicReferenceArray.set(length, referenceEntryRemoveFromChain);
                        this.count = i3;
                        return true;
                    }
                }
                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[INVOKE, INVOKE]}, finally: {[INVOKE, INVOKE, INVOKE, IF] complete} */
        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2950=4, 2951=4, 2952=4] */
        boolean reclaimValue(K k, int i, ValueReference<K, V> valueReference) {
            lock();
            try {
                int i2 = this.count;
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry; next != null; next = next.getNext()) {
                    K key = next.getKey();
                    if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(k, key)) {
                        if (next.getValueReference() != valueReference) {
                            unlock();
                            if (!isHeldByCurrentThread()) {
                                postWriteCleanup();
                            }
                            return false;
                        }
                        this.modCount++;
                        enqueueNotification(k, i, valueReference.get(), MapMaker.RemovalCause.COLLECTED);
                        ReferenceEntry<K, V> referenceEntryRemoveFromChain = removeFromChain(referenceEntry, next);
                        int i3 = this.count - 1;
                        atomicReferenceArray.set(length, referenceEntryRemoveFromChain);
                        this.count = i3;
                        return true;
                    }
                }
                unlock();
                if (!isHeldByCurrentThread()) {
                    postWriteCleanup();
                }
                return false;
            } finally {
                unlock();
                if (!isHeldByCurrentThread()) {
                    postWriteCleanup();
                }
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2983=4, 2984=4] */
        boolean clearValue(K k, int i, ValueReference<K, V> valueReference) {
            lock();
            try {
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                int length = (atomicReferenceArray.length() - 1) & i;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                for (ReferenceEntry<K, V> next = referenceEntry; next != null; next = next.getNext()) {
                    K key = next.getKey();
                    if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(k, key)) {
                        if (next.getValueReference() != valueReference) {
                            return false;
                        }
                        atomicReferenceArray.set(length, removeFromChain(referenceEntry, next));
                        return true;
                    }
                }
                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        boolean removeEntry(ReferenceEntry<K, V> referenceEntry, int i, MapMaker.RemovalCause removalCause) {
            int i2 = this.count;
            AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.table;
            int length = (atomicReferenceArray.length() - 1) & i;
            ReferenceEntry<K, V> referenceEntry2 = atomicReferenceArray.get(length);
            for (ReferenceEntry<K, V> next = referenceEntry2; next != null; next = next.getNext()) {
                if (next == referenceEntry) {
                    this.modCount++;
                    enqueueNotification(next.getKey(), i, next.getValueReference().get(), removalCause);
                    ReferenceEntry<K, V> referenceEntryRemoveFromChain = removeFromChain(referenceEntry2, next);
                    int i3 = this.count - 1;
                    atomicReferenceArray.set(length, referenceEntryRemoveFromChain);
                    this.count = i3;
                    return true;
                }
            }
            return false;
        }

        boolean isCollected(ValueReference<K, V> valueReference) {
            return !valueReference.isComputingReference() && valueReference.get() == null;
        }

        V getLiveValue(ReferenceEntry<K, V> referenceEntry) {
            if (referenceEntry.getKey() == null) {
                tryDrainReferenceQueues();
                return null;
            }
            V v = referenceEntry.getValueReference().get();
            if (v == null) {
                tryDrainReferenceQueues();
                return null;
            }
            if (this.map.expires() && this.map.isExpired(referenceEntry)) {
                tryExpireEntries();
                return null;
            }
            return v;
        }

        void postReadCleanup() {
            if ((this.readCount.incrementAndGet() & 63) == 0) {
                runCleanup();
            }
        }

        void preWriteCleanup() {
            runLockedCleanup();
        }

        void postWriteCleanup() {
            runUnlockedCleanup();
        }

        void runCleanup() {
            runLockedCleanup();
            runUnlockedCleanup();
        }

        void runLockedCleanup() {
            if (tryLock()) {
                try {
                    drainReferenceQueues();
                    expireEntries();
                    this.readCount.set(0);
                } finally {
                    unlock();
                }
            }
        }

        void runUnlockedCleanup() {
            if (!isHeldByCurrentThread()) {
                this.map.processPendingNotifications();
            }
        }
    }

    static final class EvictionQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() { // from class: com.google.common.collect.MapMakerInternalMap.EvictionQueue.1
            ReferenceEntry<K, V> nextEvictable = this;
            ReferenceEntry<K, V> previousEvictable = this;

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public ReferenceEntry<K, V> getNextEvictable() {
                return this.nextEvictable;
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
                this.nextEvictable = referenceEntry;
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public ReferenceEntry<K, V> getPreviousEvictable() {
                return this.previousEvictable;
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
                this.previousEvictable = referenceEntry;
            }
        };

        EvictionQueue() {
        }

        /* JADX DEBUG: Method merged with bridge method: offer(Ljava/lang/Object;)Z */
        @Override // java.util.Queue
        public boolean offer(ReferenceEntry<K, V> referenceEntry) {
            MapMakerInternalMap.connectEvictables(referenceEntry.getPreviousEvictable(), referenceEntry.getNextEvictable());
            MapMakerInternalMap.connectEvictables(this.head.getPreviousEvictable(), referenceEntry);
            MapMakerInternalMap.connectEvictables(referenceEntry, this.head);
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method: peek()Ljava/lang/Object; */
        @Override // java.util.Queue
        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> nextEvictable = this.head.getNextEvictable();
            if (nextEvictable == this.head) {
                return null;
            }
            return nextEvictable;
        }

        /* JADX DEBUG: Method merged with bridge method: poll()Ljava/lang/Object; */
        @Override // java.util.Queue
        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> nextEvictable = this.head.getNextEvictable();
            if (nextEvictable == this.head) {
                return null;
            }
            remove(nextEvictable);
            return nextEvictable;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean remove(Object obj) {
            ReferenceEntry referenceEntry = (ReferenceEntry) obj;
            ReferenceEntry<K, V> previousEvictable = referenceEntry.getPreviousEvictable();
            ReferenceEntry<K, V> nextEvictable = referenceEntry.getNextEvictable();
            MapMakerInternalMap.connectEvictables(previousEvictable, nextEvictable);
            MapMakerInternalMap.nullifyEvictable(referenceEntry);
            return nextEvictable != NullEntry.INSTANCE;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean contains(Object obj) {
            return ((ReferenceEntry) obj).getNextEvictable() != NullEntry.INSTANCE;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean isEmpty() {
            return this.head.getNextEvictable() == this.head;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public int size() {
            int i = 0;
            for (ReferenceEntry<K, V> nextEvictable = this.head.getNextEvictable(); nextEvictable != this.head; nextEvictable = nextEvictable.getNextEvictable()) {
                i++;
            }
            return i;
        }

        @Override // java.util.AbstractQueue, java.util.AbstractCollection, java.util.Collection
        public void clear() {
            ReferenceEntry<K, V> nextEvictable = this.head.getNextEvictable();
            while (nextEvictable != this.head) {
                ReferenceEntry<K, V> nextEvictable2 = nextEvictable.getNextEvictable();
                MapMakerInternalMap.nullifyEvictable(nextEvictable);
                nextEvictable = nextEvictable2;
            }
            this.head.setNextEvictable(this.head);
            this.head.setPreviousEvictable(this.head);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) { // from class: com.google.common.collect.MapMakerInternalMap.EvictionQueue.2
                /* JADX DEBUG: Method merged with bridge method: computeNext(Ljava/lang/Object;)Ljava/lang/Object; */
                @Override // com.google.common.collect.AbstractSequentialIterator
                protected ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> referenceEntry) {
                    ReferenceEntry<K, V> nextEvictable = referenceEntry.getNextEvictable();
                    if (nextEvictable == EvictionQueue.this.head) {
                        return null;
                    }
                    return nextEvictable;
                }
            };
        }
    }

    static final class ExpirationQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() { // from class: com.google.common.collect.MapMakerInternalMap.ExpirationQueue.1
            ReferenceEntry<K, V> nextExpirable = this;
            ReferenceEntry<K, V> previousExpirable = this;

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public long getExpirationTime() {
                return Long.MAX_VALUE;
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public void setExpirationTime(long j) {
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public ReferenceEntry<K, V> getNextExpirable() {
                return this.nextExpirable;
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
                this.nextExpirable = referenceEntry;
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public ReferenceEntry<K, V> getPreviousExpirable() {
                return this.previousExpirable;
            }

            @Override // com.google.common.collect.MapMakerInternalMap.AbstractReferenceEntry, com.google.common.collect.MapMakerInternalMap.ReferenceEntry
            public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
                this.previousExpirable = referenceEntry;
            }
        };

        ExpirationQueue() {
        }

        /* JADX DEBUG: Method merged with bridge method: offer(Ljava/lang/Object;)Z */
        @Override // java.util.Queue
        public boolean offer(ReferenceEntry<K, V> referenceEntry) {
            MapMakerInternalMap.connectExpirables(referenceEntry.getPreviousExpirable(), referenceEntry.getNextExpirable());
            MapMakerInternalMap.connectExpirables(this.head.getPreviousExpirable(), referenceEntry);
            MapMakerInternalMap.connectExpirables(referenceEntry, this.head);
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method: peek()Ljava/lang/Object; */
        @Override // java.util.Queue
        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> nextExpirable = this.head.getNextExpirable();
            if (nextExpirable == this.head) {
                return null;
            }
            return nextExpirable;
        }

        /* JADX DEBUG: Method merged with bridge method: poll()Ljava/lang/Object; */
        @Override // java.util.Queue
        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> nextExpirable = this.head.getNextExpirable();
            if (nextExpirable == this.head) {
                return null;
            }
            remove(nextExpirable);
            return nextExpirable;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean remove(Object obj) {
            ReferenceEntry referenceEntry = (ReferenceEntry) obj;
            ReferenceEntry<K, V> previousExpirable = referenceEntry.getPreviousExpirable();
            ReferenceEntry<K, V> nextExpirable = referenceEntry.getNextExpirable();
            MapMakerInternalMap.connectExpirables(previousExpirable, nextExpirable);
            MapMakerInternalMap.nullifyExpirable(referenceEntry);
            return nextExpirable != NullEntry.INSTANCE;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean contains(Object obj) {
            return ((ReferenceEntry) obj).getNextExpirable() != NullEntry.INSTANCE;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean isEmpty() {
            return this.head.getNextExpirable() == this.head;
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public int size() {
            int i = 0;
            for (ReferenceEntry<K, V> nextExpirable = this.head.getNextExpirable(); nextExpirable != this.head; nextExpirable = nextExpirable.getNextExpirable()) {
                i++;
            }
            return i;
        }

        @Override // java.util.AbstractQueue, java.util.AbstractCollection, java.util.Collection
        public void clear() {
            ReferenceEntry<K, V> nextExpirable = this.head.getNextExpirable();
            while (nextExpirable != this.head) {
                ReferenceEntry<K, V> nextExpirable2 = nextExpirable.getNextExpirable();
                MapMakerInternalMap.nullifyExpirable(nextExpirable);
                nextExpirable = nextExpirable2;
            }
            this.head.setNextExpirable(this.head);
            this.head.setPreviousExpirable(this.head);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) { // from class: com.google.common.collect.MapMakerInternalMap.ExpirationQueue.2
                /* JADX DEBUG: Method merged with bridge method: computeNext(Ljava/lang/Object;)Ljava/lang/Object; */
                @Override // com.google.common.collect.AbstractSequentialIterator
                protected ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> referenceEntry) {
                    ReferenceEntry<K, V> nextExpirable = referenceEntry.getNextExpirable();
                    if (nextExpirable == ExpirationQueue.this.head) {
                        return null;
                    }
                    return nextExpirable;
                }
            };
        }
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean isEmpty() {
        Segment<K, V>[] segmentArr = this.segments;
        long j = 0;
        for (int i = 0; i < segmentArr.length; i++) {
            if (segmentArr[i].count != 0) {
                return false;
            }
            j += segmentArr[i].modCount;
        }
        if (j != 0) {
            for (int i2 = 0; i2 < segmentArr.length; i2++) {
                if (segmentArr[i2].count != 0) {
                    return false;
                }
                j -= segmentArr[i2].modCount;
            }
            return j == 0;
        }
        return true;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public int size() {
        long j = 0;
        for (int i = 0; i < this.segments.length; i++) {
            j += r0[i].count;
        }
        return Ints.saturatedCast(j);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public V get(Object obj) {
        if (obj == null) {
            return null;
        }
        int iHash = hash(obj);
        return segmentFor(iHash).get(obj, iHash);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean containsKey(Object obj) {
        if (obj == null) {
            return false;
        }
        int iHash = hash(obj);
        return segmentFor(iHash).containsKey(obj, iHash);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean containsValue(Object obj) {
        int i = 0;
        if (obj == null) {
            return false;
        }
        Segment<K, V>[] segmentArr = this.segments;
        long j = -1;
        int i2 = 0;
        while (i2 < 3) {
            int length = segmentArr.length;
            long j2 = 0;
            int i3 = i;
            while (i3 < length) {
                Segment<K, V> segment = segmentArr[i3];
                int i4 = segment.count;
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = segment.table;
                for (int i5 = i; i5 < atomicReferenceArray.length(); i5++) {
                    for (ReferenceEntry<K, V> next = atomicReferenceArray.get(i5); next != null; next = next.getNext()) {
                        V liveValue = segment.getLiveValue(next);
                        if (liveValue != null && this.valueEquivalence.equivalent(obj, liveValue)) {
                            return true;
                        }
                    }
                }
                j2 += segment.modCount;
                i3++;
                i = 0;
            }
            if (j2 != j) {
                i2++;
                j = j2;
                i = 0;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public V put(K k, V v) {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(v);
        int iHash = hash(k);
        return segmentFor(iHash).put(k, iHash, v, false);
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public V putIfAbsent(K k, V v) {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(v);
        int iHash = hash(k);
        return segmentFor(iHash).put(k, iHash, v, true);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override // java.util.AbstractMap, java.util.Map
    public V remove(Object obj) {
        if (obj == null) {
            return null;
        }
        int iHash = hash(obj);
        return segmentFor(iHash).remove(obj, iHash);
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public boolean remove(Object obj, Object obj2) {
        if (obj == null || obj2 == null) {
            return false;
        }
        int iHash = hash(obj);
        return segmentFor(iHash).remove(obj, iHash, obj2);
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public boolean replace(K k, V v, V v2) {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(v2);
        if (v == null) {
            return false;
        }
        int iHash = hash(k);
        return segmentFor(iHash).replace(k, iHash, v, v2);
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public V replace(K k, V v) {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(v);
        int iHash = hash(k);
        return segmentFor(iHash).replace(k, iHash, v);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public void clear() {
        for (Segment<K, V> segment : this.segments) {
            segment.clear();
        }
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set != null) {
            return set;
        }
        KeySet keySet = new KeySet();
        this.keySet = keySet;
        return keySet;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection != null) {
            return collection;
        }
        Values values = new Values();
        this.values = values;
        return values;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        EntrySet entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    abstract class HashIterator<E> implements Iterator<E> {
        Segment<K, V> currentSegment;
        AtomicReferenceArray<ReferenceEntry<K, V>> currentTable;

        /* JADX WARN: Incorrect inner types in field signature: Lcom/google/common/collect/MapMakerInternalMap<TK;TV;>.com/google/common/collect/MapMakerInternalMap$com/google/common/collect/MapMakerInternalMap$WriteThroughEntry; */
        WriteThroughEntry lastReturned;
        ReferenceEntry<K, V> nextEntry;

        /* JADX WARN: Incorrect inner types in field signature: Lcom/google/common/collect/MapMakerInternalMap<TK;TV;>.com/google/common/collect/MapMakerInternalMap$com/google/common/collect/MapMakerInternalMap$WriteThroughEntry; */
        WriteThroughEntry nextExternal;
        int nextSegmentIndex;
        int nextTableIndex = -1;

        HashIterator() {
            this.nextSegmentIndex = MapMakerInternalMap.this.segments.length - 1;
            advance();
        }

        final void advance() {
            this.nextExternal = null;
            if (nextInChain() || nextInTable()) {
                return;
            }
            while (this.nextSegmentIndex >= 0) {
                Segment<K, V>[] segmentArr = MapMakerInternalMap.this.segments;
                int i = this.nextSegmentIndex;
                this.nextSegmentIndex = i - 1;
                this.currentSegment = segmentArr[i];
                if (this.currentSegment.count != 0) {
                    this.currentTable = this.currentSegment.table;
                    this.nextTableIndex = this.currentTable.length() - 1;
                    if (nextInTable()) {
                        return;
                    }
                }
            }
        }

        boolean nextInChain() {
            if (this.nextEntry != null) {
                do {
                    this.nextEntry = this.nextEntry.getNext();
                    if (this.nextEntry == null) {
                        return false;
                    }
                } while (!advanceTo(this.nextEntry));
                return true;
            }
            return false;
        }

        boolean nextInTable() {
            while (this.nextTableIndex >= 0) {
                AtomicReferenceArray<ReferenceEntry<K, V>> atomicReferenceArray = this.currentTable;
                int i = this.nextTableIndex;
                this.nextTableIndex = i - 1;
                ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(i);
                this.nextEntry = referenceEntry;
                if (referenceEntry != null && (advanceTo(this.nextEntry) || nextInChain())) {
                    return true;
                }
            }
            return false;
        }

        boolean advanceTo(ReferenceEntry<K, V> referenceEntry) {
            Segment<K, V> segment;
            try {
                K key = referenceEntry.getKey();
                Object liveValue = MapMakerInternalMap.this.getLiveValue(referenceEntry);
                if (liveValue != null) {
                    this.nextExternal = new WriteThroughEntry(key, liveValue);
                    return true;
                }
                return false;
            } finally {
                this.currentSegment.postReadCleanup();
            }
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.nextExternal != null;
        }

        MapMakerInternalMap<K, V>.WriteThroughEntry nextEntry() {
            if (this.nextExternal == null) {
                throw new NoSuchElementException();
            }
            this.lastReturned = this.nextExternal;
            advance();
            return this.lastReturned;
        }

        @Override // java.util.Iterator
        public void remove() {
            CollectPreconditions.checkRemove(this.lastReturned != null);
            MapMakerInternalMap.this.remove(this.lastReturned.getKey());
            this.lastReturned = null;
        }
    }

    final class KeyIterator extends MapMakerInternalMap<K, V>.HashIterator<K> {
        KeyIterator() {
            super();
        }

        public K next() {
            return (K) nextEntry().getKey();
        }
    }

    final class ValueIterator extends MapMakerInternalMap<K, V>.HashIterator<V> {
        ValueIterator() {
            super();
        }

        public V next() {
            return (V) nextEntry().getValue();
        }
    }

    final class WriteThroughEntry extends AbstractMapEntry<K, V> {
        final K key;
        V value;

        WriteThroughEntry(K k, V v) {
            this.key = k;
            this.value = v;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        public K getKey() {
            return this.key;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        public V getValue() {
            return this.value;
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        public boolean equals(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return this.key.equals(entry.getKey()) && this.value.equals(entry.getValue());
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        public int hashCode() {
            return this.key.hashCode() ^ this.value.hashCode();
        }

        @Override // com.google.common.collect.AbstractMapEntry, java.util.Map.Entry
        public V setValue(V v) {
            V v2 = (V) MapMakerInternalMap.this.put(this.key, v);
            this.value = v;
            return v2;
        }
    }

    final class EntryIterator extends MapMakerInternalMap<K, V>.HashIterator<Map.Entry<K, V>> {
        EntryIterator() {
            super();
        }

        /* JADX DEBUG: Method merged with bridge method: next()Ljava/lang/Object; */
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    final class KeySet extends AbstractSet<K> {
        KeySet() {
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return MapMakerInternalMap.this.size();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(Object obj) {
            return MapMakerInternalMap.this.containsKey(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(Object obj) {
            return MapMakerInternalMap.this.remove(obj) != null;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    final class Values extends AbstractCollection<V> {
        Values() {
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public int size() {
            return MapMakerInternalMap.this.size();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public boolean contains(Object obj) {
            return MapMakerInternalMap.this.containsValue(obj);
        }

        @Override // java.util.AbstractCollection, java.util.Collection
        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        EntrySet() {
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean contains(Object obj) {
            Map.Entry entry;
            Object key;
            Object obj2;
            return (obj instanceof Map.Entry) && (key = (entry = (Map.Entry) obj).getKey()) != null && (obj2 = MapMakerInternalMap.this.get(key)) != null && MapMakerInternalMap.this.valueEquivalence.equivalent(entry.getValue(), obj2);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean remove(Object obj) {
            Map.Entry entry;
            Object key;
            return (obj instanceof Map.Entry) && (key = (entry = (Map.Entry) obj).getKey()) != null && MapMakerInternalMap.this.remove(key, entry.getValue());
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public int size() {
            return MapMakerInternalMap.this.size();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    Object writeReplace() {
        return new SerializationProxy(this.keyStrength, this.valueStrength, this.keyEquivalence, this.valueEquivalence, this.expireAfterWriteNanos, this.expireAfterAccessNanos, this.maximumSize, this.concurrencyLevel, this.removalListener, this);
    }

    static abstract class AbstractSerializationProxy<K, V> extends ForwardingConcurrentMap<K, V> implements Serializable {
        private static final long serialVersionUID = 3;
        final int concurrencyLevel;
        transient ConcurrentMap<K, V> delegate;
        final long expireAfterAccessNanos;
        final long expireAfterWriteNanos;
        final Equivalence<Object> keyEquivalence;
        final Strength keyStrength;
        final int maximumSize;
        final MapMaker.RemovalListener<? super K, ? super V> removalListener;
        final Equivalence<Object> valueEquivalence;
        final Strength valueStrength;

        AbstractSerializationProxy(Strength strength, Strength strength2, Equivalence<Object> equivalence, Equivalence<Object> equivalence2, long j, long j2, int i, int i2, MapMaker.RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> concurrentMap) {
            this.keyStrength = strength;
            this.valueStrength = strength2;
            this.keyEquivalence = equivalence;
            this.valueEquivalence = equivalence2;
            this.expireAfterWriteNanos = j;
            this.expireAfterAccessNanos = j2;
            this.maximumSize = i;
            this.concurrencyLevel = i2;
            this.removalListener = removalListener;
            this.delegate = concurrentMap;
        }

        /* JADX DEBUG: Method merged with bridge method: delegate()Ljava/lang/Object; */
        /* JADX DEBUG: Method merged with bridge method: delegate()Ljava/util/Map; */
        @Override // com.google.common.collect.ForwardingConcurrentMap, com.google.common.collect.ForwardingMap, com.google.common.collect.ForwardingObject
        protected ConcurrentMap<K, V> delegate() {
            return this.delegate;
        }

        void writeMapTo(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.writeInt(this.delegate.size());
            for (Map.Entry<K, V> entry : this.delegate.entrySet()) {
                objectOutputStream.writeObject(entry.getKey());
                objectOutputStream.writeObject(entry.getValue());
            }
            objectOutputStream.writeObject(null);
        }

        MapMaker readMapMaker(ObjectInputStream objectInputStream) throws IOException {
            MapMaker mapMakerConcurrencyLevel = new MapMaker().initialCapacity(objectInputStream.readInt()).setKeyStrength(this.keyStrength).setValueStrength(this.valueStrength).keyEquivalence(this.keyEquivalence).concurrencyLevel(this.concurrencyLevel);
            mapMakerConcurrencyLevel.removalListener(this.removalListener);
            if (this.expireAfterWriteNanos > 0) {
                mapMakerConcurrencyLevel.expireAfterWrite(this.expireAfterWriteNanos, TimeUnit.NANOSECONDS);
            }
            if (this.expireAfterAccessNanos > 0) {
                mapMakerConcurrencyLevel.expireAfterAccess(this.expireAfterAccessNanos, TimeUnit.NANOSECONDS);
            }
            if (this.maximumSize != -1) {
                mapMakerConcurrencyLevel.maximumSize(this.maximumSize);
            }
            return mapMakerConcurrencyLevel;
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.google.common.collect.ComputingConcurrentHashMap */
        /* JADX WARN: Multi-variable type inference failed */
        void readEntries(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            while (true) {
                Object object = objectInputStream.readObject();
                if (object != null) {
                    this.delegate.put(object, objectInputStream.readObject());
                } else {
                    return;
                }
            }
        }
    }

    private static final class SerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {
        private static final long serialVersionUID = 3;

        SerializationProxy(Strength strength, Strength strength2, Equivalence<Object> equivalence, Equivalence<Object> equivalence2, long j, long j2, int i, int i2, MapMaker.RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> concurrentMap) {
            super(strength, strength2, equivalence, equivalence2, j, j2, i, i2, removalListener, concurrentMap);
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            writeMapTo(objectOutputStream);
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.delegate = readMapMaker(objectInputStream).makeMap();
            readEntries(objectInputStream);
        }

        private Object readResolve() {
            return this.delegate;
        }
    }
}
