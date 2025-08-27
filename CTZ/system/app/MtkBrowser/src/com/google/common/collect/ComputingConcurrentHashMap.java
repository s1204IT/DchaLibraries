package com.google.common.collect;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.common.collect.MapMakerInternalMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReferenceArray;

/* loaded from: classes.dex */
class ComputingConcurrentHashMap<K, V> extends MapMakerInternalMap<K, V> {
    private static final long serialVersionUID = 4;
    final Function<? super K, ? extends V> computingFunction;

    ComputingConcurrentHashMap(MapMaker mapMaker, Function<? super K, ? extends V> function) {
        super(mapMaker);
        this.computingFunction = (Function) Preconditions.checkNotNull(function);
    }

    @Override // com.google.common.collect.MapMakerInternalMap
    MapMakerInternalMap.Segment<K, V> createSegment(int i, int i2) {
        return new ComputingSegment(this, i, i2);
    }

    /* JADX DEBUG: Method merged with bridge method: segmentFor(I)Lcom/google/common/collect/MapMakerInternalMap$Segment; */
    @Override // com.google.common.collect.MapMakerInternalMap
    ComputingSegment<K, V> segmentFor(int i) {
        return (ComputingSegment) super.segmentFor(i);
    }

    V getOrCompute(K k) throws ExecutionException {
        int iHash = hash(Preconditions.checkNotNull(k));
        return segmentFor(iHash).getOrCompute(k, iHash, this.computingFunction);
    }

    static final class ComputingSegment<K, V> extends MapMakerInternalMap.Segment<K, V> {
        ComputingSegment(MapMakerInternalMap<K, V> mapMakerInternalMap, int i, int i2) {
            super(mapMakerInternalMap, i, i2);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [145=4, 167=5] */
        /* JADX WARN: Code restructure failed: missing block: B:23:0x005b, code lost:
        
            if (r6.getValueReference().isComputingReference() == false) goto L25;
         */
        /* JADX WARN: Code restructure failed: missing block: B:24:0x005d, code lost:
        
            r2 = false;
         */
        /* JADX WARN: Code restructure failed: missing block: B:25:0x005f, code lost:
        
            r8 = r6.getValueReference().get();
         */
        /* JADX WARN: Code restructure failed: missing block: B:26:0x0067, code lost:
        
            if (r8 != null) goto L28;
         */
        /* JADX WARN: Code restructure failed: missing block: B:27:0x0069, code lost:
        
            enqueueNotification(r7, r12, r8, com.google.common.collect.MapMaker.RemovalCause.COLLECTED);
         */
        /* JADX WARN: Code restructure failed: missing block: B:29:0x0075, code lost:
        
            if (r10.map.expires() == false) goto L66;
         */
        /* JADX WARN: Code restructure failed: missing block: B:31:0x007d, code lost:
        
            if (r10.map.isExpired(r6) == false) goto L67;
         */
        /* JADX WARN: Code restructure failed: missing block: B:32:0x007f, code lost:
        
            enqueueNotification(r7, r12, r8, com.google.common.collect.MapMaker.RemovalCause.EXPIRED);
         */
        /* JADX WARN: Code restructure failed: missing block: B:33:0x0084, code lost:
        
            r10.evictionQueue.remove(r6);
            r10.expirationQueue.remove(r6);
            r10.count = r2;
         */
        /* JADX WARN: Code restructure failed: missing block: B:34:0x0091, code lost:
        
            recordLockedRead(r6);
         */
        /* JADX WARN: Code restructure failed: missing block: B:37:0x009e, code lost:
        
            return r8;
         */
        /* JADX WARN: Removed duplicated region for block: B:68:0x00c9 A[SYNTHETIC] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        V getOrCompute(K k, int i, Function<? super K, ? extends V> function) throws ExecutionException {
            MapMakerInternalMap.ReferenceEntry<K, V> entry;
            ComputingValueReference<K, V> computingValueReference;
            V vWaitForValue;
            V liveValue;
            do {
                try {
                    entry = getEntry(k, i);
                    if (entry != null && (liveValue = getLiveValue(entry)) != null) {
                        recordRead(entry);
                        return liveValue;
                    }
                    if (entry == null || !entry.getValueReference().isComputingReference()) {
                        ComputingValueReference<K, V> computingValueReference2 = null;
                        lock();
                        try {
                            preWriteCleanup();
                            int i2 = this.count - 1;
                            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> atomicReferenceArray = this.table;
                            int length = (atomicReferenceArray.length() - 1) & i;
                            MapMakerInternalMap.ReferenceEntry<K, V> referenceEntry = atomicReferenceArray.get(length);
                            MapMakerInternalMap.ReferenceEntry<K, V> next = referenceEntry;
                            while (true) {
                                if (next == null) {
                                    break;
                                }
                                K key = next.getKey();
                                if (next.getHash() == i && key != null && this.map.keyEquivalence.equivalent(k, key)) {
                                    break;
                                }
                                next = next.getNext();
                            }
                            boolean z = true;
                            if (z) {
                                computingValueReference2 = new ComputingValueReference<>(function);
                                if (next == null) {
                                    MapMakerInternalMap.ReferenceEntry<K, V> referenceEntryNewEntry = newEntry(k, i, referenceEntry);
                                    referenceEntryNewEntry.setValueReference(computingValueReference2);
                                    atomicReferenceArray.set(length, referenceEntryNewEntry);
                                    computingValueReference = computingValueReference2;
                                    entry = referenceEntryNewEntry;
                                    if (z) {
                                        return compute(k, i, entry, computingValueReference);
                                    }
                                } else {
                                    next.setValueReference(computingValueReference2);
                                    computingValueReference = computingValueReference2;
                                    entry = next;
                                    if (z) {
                                    }
                                }
                            } else {
                                computingValueReference = computingValueReference2;
                                entry = next;
                                if (z) {
                                }
                            }
                        } finally {
                            unlock();
                            postWriteCleanup();
                        }
                    }
                    Preconditions.checkState(true ^ Thread.holdsLock(entry), "Recursive computation");
                    vWaitForValue = entry.getValueReference().waitForValue();
                } finally {
                    postReadCleanup();
                }
            } while (vWaitForValue == null);
            recordRead(entry);
            return vWaitForValue;
        }

        /* JADX WARN: Removed duplicated region for block: B:32:0x0043  */
        /* JADX WARN: Removed duplicated region for block: B:34:0x0048  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        V compute(K k, int i, MapMakerInternalMap.ReferenceEntry<K, V> referenceEntry, ComputingValueReference<K, V> computingValueReference) throws Throwable {
            System.nanoTime();
            try {
                try {
                    try {
                        synchronized (referenceEntry) {
                            try {
                                V vCompute = computingValueReference.compute(k, i);
                                try {
                                    long jNanoTime = System.nanoTime();
                                    if (vCompute != null && put(k, i, vCompute, true) != null) {
                                        enqueueNotification(k, i, vCompute, MapMaker.RemovalCause.REPLACED);
                                    }
                                    if (jNanoTime == 0) {
                                        System.nanoTime();
                                    }
                                    if (vCompute == null) {
                                        clearValue(k, i, computingValueReference);
                                    }
                                    return vCompute;
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        if (0 == 0) {
                        }
                        if (0 == 0) {
                        }
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
                if (0 == 0) {
                    System.nanoTime();
                }
                if (0 == 0) {
                    clearValue(k, i, computingValueReference);
                }
                throw th;
            }
        }
    }

    private static final class ComputationExceptionReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
        final Throwable t;

        ComputationExceptionReference(Throwable th) {
            this.t = th;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V get() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, MapMakerInternalMap.ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public boolean isComputingReference() {
            return false;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V waitForValue() throws ExecutionException {
            throw new ExecutionException(this.t);
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public void clear(MapMakerInternalMap.ValueReference<K, V> valueReference) {
        }
    }

    private static final class ComputedReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
        final V value;

        ComputedReference(V v) {
            this.value = v;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V get() {
            return this.value;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, MapMakerInternalMap.ReferenceEntry<K, V> referenceEntry) {
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
        public void clear(MapMakerInternalMap.ValueReference<K, V> valueReference) {
        }
    }

    private static final class ComputingValueReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
        volatile MapMakerInternalMap.ValueReference<K, V> computedReference = MapMakerInternalMap.unset();
        final Function<? super K, ? extends V> computingFunction;

        public ComputingValueReference(Function<? super K, ? extends V> function) {
            this.computingFunction = function;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V get() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, MapMakerInternalMap.ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public boolean isComputingReference() {
            return true;
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public V waitForValue() throws Throwable {
            boolean z;
            Throwable th;
            if (this.computedReference == MapMakerInternalMap.UNSET) {
                try {
                    synchronized (this) {
                        z = false;
                        while (this.computedReference == MapMakerInternalMap.UNSET) {
                            try {
                                try {
                                    wait();
                                } catch (InterruptedException e) {
                                    z = true;
                                }
                            } catch (Throwable th2) {
                                try {
                                    throw th2;
                                } catch (Throwable th3) {
                                    th = th3;
                                    if (z) {
                                        Thread.currentThread().interrupt();
                                    }
                                    throw th;
                                }
                            }
                        }
                    }
                    if (z) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Throwable th4) {
                    z = false;
                    th = th4;
                }
            }
            return this.computedReference.waitForValue();
        }

        @Override // com.google.common.collect.MapMakerInternalMap.ValueReference
        public void clear(MapMakerInternalMap.ValueReference<K, V> valueReference) {
            setValueReference(valueReference);
        }

        V compute(K k, int i) throws ExecutionException {
            try {
                V vApply = this.computingFunction.apply(k);
                setValueReference(new ComputedReference(vApply));
                return vApply;
            } catch (Throwable th) {
                setValueReference(new ComputationExceptionReference(th));
                throw new ExecutionException(th);
            }
        }

        void setValueReference(MapMakerInternalMap.ValueReference<K, V> valueReference) {
            synchronized (this) {
                if (this.computedReference == MapMakerInternalMap.UNSET) {
                    this.computedReference = valueReference;
                    notifyAll();
                }
            }
        }
    }

    @Override // com.google.common.collect.MapMakerInternalMap
    Object writeReplace() {
        return new ComputingSerializationProxy(this.keyStrength, this.valueStrength, this.keyEquivalence, this.valueEquivalence, this.expireAfterWriteNanos, this.expireAfterAccessNanos, this.maximumSize, this.concurrencyLevel, this.removalListener, this, this.computingFunction);
    }

    static final class ComputingSerializationProxy<K, V> extends MapMakerInternalMap.AbstractSerializationProxy<K, V> {
        private static final long serialVersionUID = 4;
        final Function<? super K, ? extends V> computingFunction;

        ComputingSerializationProxy(MapMakerInternalMap.Strength strength, MapMakerInternalMap.Strength strength2, Equivalence<Object> equivalence, Equivalence<Object> equivalence2, long j, long j2, int i, int i2, MapMaker.RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> concurrentMap, Function<? super K, ? extends V> function) {
            super(strength, strength2, equivalence, equivalence2, j, j2, i, i2, removalListener, concurrentMap);
            this.computingFunction = function;
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            writeMapTo(objectOutputStream);
        }

        private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
            objectInputStream.defaultReadObject();
            this.delegate = readMapMaker(objectInputStream).makeComputingMap(this.computingFunction);
            readEntries(objectInputStream);
        }

        Object readResolve() {
            return this.delegate;
        }
    }
}
