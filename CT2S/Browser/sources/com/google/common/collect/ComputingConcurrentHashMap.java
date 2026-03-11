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

class ComputingConcurrentHashMap<K, V> extends MapMakerInternalMap<K, V> {
    private static final long serialVersionUID = 4;
    final Function<? super K, ? extends V> computingFunction;

    ComputingConcurrentHashMap(MapMaker builder, Function<? super K, ? extends V> computingFunction) {
        super(builder);
        this.computingFunction = (Function) Preconditions.checkNotNull(computingFunction);
    }

    @Override
    MapMakerInternalMap.Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
        return new ComputingSegment(this, initialCapacity, maxSegmentSize);
    }

    @Override
    public ComputingSegment<K, V> segmentFor(int hash) {
        return (ComputingSegment) super.segmentFor(hash);
    }

    V getOrCompute(K key) throws ExecutionException {
        int hash = hash(Preconditions.checkNotNull(key));
        return segmentFor(hash).getOrCompute(key, hash, this.computingFunction);
    }

    static final class ComputingSegment<K, V> extends MapMakerInternalMap.Segment<K, V> {
        ComputingSegment(MapMakerInternalMap<K, V> map, int initialCapacity, int maxSegmentSize) {
            super(map, initialCapacity, maxSegmentSize);
        }

        V getOrCompute(K k, int i, Function<? super K, ? extends V> function) throws ExecutionException {
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> atomicReferenceArray;
            int length;
            MapMakerInternalMap.ReferenceEntry<K, V> referenceEntry;
            V vCompute;
            while (true) {
                try {
                    MapMakerInternalMap.ReferenceEntry<K, V> entry = getEntry(k, i);
                    if (entry != null && (vCompute = getLiveValue(entry)) != null) {
                        recordRead(entry);
                        break;
                    }
                    if (entry == null || !entry.getValueReference().isComputingReference()) {
                        boolean z = true;
                        ComputingValueReference<K, V> computingValueReference = null;
                        lock();
                        try {
                            preWriteCleanup();
                            int i2 = this.count - 1;
                            atomicReferenceArray = this.table;
                            length = i & (atomicReferenceArray.length() - 1);
                            referenceEntry = atomicReferenceArray.get(length);
                            entry = referenceEntry;
                        } catch (Throwable th) {
                            th = th;
                        }
                        while (true) {
                            if (entry == null) {
                                break;
                            }
                            K key = entry.getKey();
                            if (entry.getHash() == i && key != null && this.map.keyEquivalence.equivalent(k, key)) {
                                break;
                            }
                            entry = entry.getNext();
                            unlock();
                            postWriteCleanup();
                            throw th;
                        }
                        if (z) {
                            ComputingValueReference<K, V> computingValueReference2 = new ComputingValueReference<>(function);
                            if (entry == null) {
                                try {
                                    entry = newEntry(k, i, referenceEntry);
                                    entry.setValueReference(computingValueReference2);
                                    atomicReferenceArray.set(length, entry);
                                    computingValueReference = computingValueReference2;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } else {
                                entry.setValueReference(computingValueReference2);
                                computingValueReference = computingValueReference2;
                            }
                        }
                        unlock();
                        postWriteCleanup();
                        if (z) {
                            vCompute = compute(k, i, entry, computingValueReference);
                            break;
                        }
                        Preconditions.checkState(!Thread.holdsLock(entry), "Recursive computation");
                        vCompute = entry.getValueReference().waitForValue();
                        if (vCompute != null) {
                            recordRead(entry);
                            break;
                        }
                    }
                } finally {
                    postReadCleanup();
                }
            }
            return vCompute;
        }

        V compute(K key, int hash, MapMakerInternalMap.ReferenceEntry<K, V> e, ComputingValueReference<K, V> computingValueReference) throws ExecutionException {
            V value = null;
            System.nanoTime();
            long end = 0;
            try {
                synchronized (e) {
                    value = computingValueReference.compute(key, hash);
                    end = System.nanoTime();
                }
                if (value != null) {
                    V oldValue = put(key, hash, value, true);
                    if (oldValue != null) {
                        enqueueNotification(key, hash, value, MapMaker.RemovalCause.REPLACED);
                    }
                }
                return value;
            } finally {
                if (end == 0) {
                    System.nanoTime();
                }
                if (value == null) {
                    clearValue(key, hash, computingValueReference);
                }
            }
        }
    }

    private static final class ComputationExceptionReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
        final Throwable t;

        ComputationExceptionReference(Throwable t) {
            this.t = t;
        }

        @Override
        public V get() {
            return null;
        }

        @Override
        public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override
        public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
            return this;
        }

        @Override
        public boolean isComputingReference() {
            return false;
        }

        @Override
        public V waitForValue() throws ExecutionException {
            throw new ExecutionException(this.t);
        }

        @Override
        public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
        }
    }

    private static final class ComputedReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
        final V value;

        ComputedReference(V value) {
            this.value = value;
        }

        @Override
        public V get() {
            return this.value;
        }

        @Override
        public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override
        public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
            return this;
        }

        @Override
        public boolean isComputingReference() {
            return false;
        }

        @Override
        public V waitForValue() {
            return get();
        }

        @Override
        public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
        }
    }

    private static final class ComputingValueReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
        volatile MapMakerInternalMap.ValueReference<K, V> computedReference = MapMakerInternalMap.unset();
        final Function<? super K, ? extends V> computingFunction;

        public ComputingValueReference(Function<? super K, ? extends V> computingFunction) {
            this.computingFunction = computingFunction;
        }

        @Override
        public V get() {
            return null;
        }

        @Override
        public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
            return null;
        }

        @Override
        public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
            return this;
        }

        @Override
        public boolean isComputingReference() {
            return true;
        }

        @Override
        public V waitForValue() throws ExecutionException {
            if (this.computedReference == MapMakerInternalMap.UNSET) {
                boolean interrupted = false;
                try {
                    synchronized (this) {
                        while (this.computedReference == MapMakerInternalMap.UNSET) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                interrupted = true;
                            }
                        }
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return this.computedReference.waitForValue();
        }

        @Override
        public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
            setValueReference(newValue);
        }

        V compute(K key, int hash) throws ExecutionException {
            try {
                V value = this.computingFunction.apply(key);
                setValueReference(new ComputedReference(value));
                return value;
            } catch (Throwable t) {
                setValueReference(new ComputationExceptionReference(t));
                throw new ExecutionException(t);
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

    @Override
    Object writeReplace() {
        return new ComputingSerializationProxy(this.keyStrength, this.valueStrength, this.keyEquivalence, this.valueEquivalence, this.expireAfterWriteNanos, this.expireAfterAccessNanos, this.maximumSize, this.concurrencyLevel, this.removalListener, this, this.computingFunction);
    }

    static final class ComputingSerializationProxy<K, V> extends MapMakerInternalMap.AbstractSerializationProxy<K, V> {
        private static final long serialVersionUID = 4;
        final Function<? super K, ? extends V> computingFunction;

        ComputingSerializationProxy(MapMakerInternalMap.Strength keyStrength, MapMakerInternalMap.Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize, int concurrencyLevel, MapMaker.RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> delegate, Function<? super K, ? extends V> computingFunction) {
            super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize, concurrencyLevel, removalListener, delegate);
            this.computingFunction = computingFunction;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            writeMapTo(out);
        }

        private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
            in.defaultReadObject();
            MapMaker mapMaker = readMapMaker(in);
            this.delegate = mapMaker.makeComputingMap(this.computingFunction);
            readEntries(in);
        }

        Object readResolve() {
            return this.delegate;
        }
    }
}
