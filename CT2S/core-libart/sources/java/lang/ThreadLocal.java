package java.lang;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadLocal<T> {
    private static AtomicInteger hashCounter = new AtomicInteger(0);
    private final Reference<ThreadLocal<T>> reference = new WeakReference(this);
    private final int hash = hashCounter.getAndAdd(-1013904242);

    public T get() {
        Thread threadCurrentThread = Thread.currentThread();
        Values values = values(threadCurrentThread);
        if (values == null) {
            values = initializeValues(threadCurrentThread);
        } else {
            Object[] objArr = values.table;
            int i = this.hash & values.mask;
            if (this.reference == objArr[i]) {
                return (T) objArr[i + 1];
            }
        }
        return (T) values.getAfterMiss(this);
    }

    protected T initialValue() {
        return null;
    }

    public void set(T value) {
        Thread currentThread = Thread.currentThread();
        Values values = values(currentThread);
        if (values == null) {
            values = initializeValues(currentThread);
        }
        values.put(this, value);
    }

    public void remove() {
        Thread currentThread = Thread.currentThread();
        Values values = values(currentThread);
        if (values != null) {
            values.remove(this);
        }
    }

    Values initializeValues(Thread current) {
        Values values = new Values();
        current.localValues = values;
        return values;
    }

    Values values(Thread current) {
        return current.localValues;
    }

    static class Values {
        private static final int INITIAL_SIZE = 16;
        private static final Object TOMBSTONE = new Object();
        private int clean;
        private int mask;
        private int maximumLoad;
        private int size;
        private Object[] table;
        private int tombstones;

        Values() {
            initializeTable(16);
            this.size = 0;
            this.tombstones = 0;
        }

        Values(Values fromParent) {
            this.table = (Object[]) fromParent.table.clone();
            this.mask = fromParent.mask;
            this.size = fromParent.size;
            this.tombstones = fromParent.tombstones;
            this.maximumLoad = fromParent.maximumLoad;
            this.clean = fromParent.clean;
            inheritValues(fromParent);
        }

        private void inheritValues(Values fromParent) {
            Object[] table = this.table;
            for (int i = table.length - 2; i >= 0; i -= 2) {
                Object k = table[i];
                if (k != null && k != TOMBSTONE) {
                    Reference<InheritableThreadLocal<?>> reference = (Reference) k;
                    InheritableThreadLocal<?> inheritableThreadLocal = reference.get();
                    if (inheritableThreadLocal != null) {
                        table[i + 1] = inheritableThreadLocal.childValue(fromParent.table[i + 1]);
                    } else {
                        table[i] = TOMBSTONE;
                        table[i + 1] = null;
                        fromParent.table[i] = TOMBSTONE;
                        fromParent.table[i + 1] = null;
                        this.tombstones++;
                        fromParent.tombstones++;
                        this.size--;
                        fromParent.size--;
                    }
                }
            }
        }

        private void initializeTable(int capacity) {
            this.table = new Object[capacity * 2];
            this.mask = this.table.length - 1;
            this.clean = 0;
            this.maximumLoad = (capacity * 2) / 3;
        }

        private void cleanUp() {
            if (!rehash() && this.size != 0) {
                int index = this.clean;
                Object[] table = this.table;
                int counter = table.length;
                while (counter > 0) {
                    Object k = table[index];
                    if (k != TOMBSTONE && k != null) {
                        Reference<ThreadLocal<?>> reference = (Reference) k;
                        if (reference.get() == null) {
                            table[index] = TOMBSTONE;
                            table[index + 1] = null;
                            this.tombstones++;
                            this.size--;
                        }
                    }
                    counter >>= 1;
                    index = next(index);
                }
                this.clean = index;
            }
        }

        private boolean rehash() {
            if (this.tombstones + this.size < this.maximumLoad) {
                return false;
            }
            int capacity = this.table.length >> 1;
            int newCapacity = capacity;
            if (this.size > (capacity >> 1)) {
                newCapacity = capacity * 2;
            }
            Object[] oldTable = this.table;
            initializeTable(newCapacity);
            this.tombstones = 0;
            if (this.size == 0) {
                return true;
            }
            for (int i = oldTable.length - 2; i >= 0; i -= 2) {
                Object k = oldTable[i];
                if (k != null && k != TOMBSTONE) {
                    Reference<ThreadLocal<?>> reference = (Reference) k;
                    ThreadLocal<?> key = reference.get();
                    if (key != null) {
                        add(key, oldTable[i + 1]);
                    } else {
                        this.size--;
                    }
                }
            }
            return true;
        }

        void add(ThreadLocal<?> key, Object value) {
            int index = ((ThreadLocal) key).hash & this.mask;
            while (k != null) {
                index = next(index);
            }
            this.table[index] = ((ThreadLocal) key).reference;
            this.table[index + 1] = value;
        }

        void put(ThreadLocal<?> key, Object value) {
            cleanUp();
            int firstTombstone = -1;
            int index = ((ThreadLocal) key).hash & this.mask;
            while (true) {
                Object k = this.table[index];
                if (k == ((ThreadLocal) key).reference) {
                    this.table[index + 1] = value;
                    return;
                }
                if (k == null) {
                    if (firstTombstone == -1) {
                        this.table[index] = ((ThreadLocal) key).reference;
                        this.table[index + 1] = value;
                        this.size++;
                        return;
                    } else {
                        this.table[firstTombstone] = ((ThreadLocal) key).reference;
                        this.table[firstTombstone + 1] = value;
                        this.tombstones--;
                        this.size++;
                        return;
                    }
                }
                if (firstTombstone == -1 && k == TOMBSTONE) {
                    firstTombstone = index;
                }
                index = next(index);
            }
        }

        Object getAfterMiss(ThreadLocal<?> key) {
            Object[] table = this.table;
            int index = ((ThreadLocal) key).hash & this.mask;
            if (table[index] == null) {
                Object value = key.initialValue();
                if (this.table == table && table[index] == null) {
                    table[index] = ((ThreadLocal) key).reference;
                    table[index + 1] = value;
                    this.size++;
                    cleanUp();
                    return value;
                }
                put(key, value);
                return value;
            }
            int firstTombstone = -1;
            int index2 = next(index);
            while (true) {
                Object reference = table[index2];
                if (reference == ((ThreadLocal) key).reference) {
                    return table[index2 + 1];
                }
                if (reference == null) {
                    Object value2 = key.initialValue();
                    if (this.table == table) {
                        if (firstTombstone > -1 && table[firstTombstone] == TOMBSTONE) {
                            table[firstTombstone] = ((ThreadLocal) key).reference;
                            table[firstTombstone + 1] = value2;
                            this.tombstones--;
                            this.size++;
                            return value2;
                        }
                        if (table[index2] == null) {
                            table[index2] = ((ThreadLocal) key).reference;
                            table[index2 + 1] = value2;
                            this.size++;
                            cleanUp();
                            return value2;
                        }
                    }
                    put(key, value2);
                    return value2;
                }
                if (firstTombstone == -1 && reference == TOMBSTONE) {
                    firstTombstone = index2;
                }
                index2 = next(index2);
            }
        }

        void remove(ThreadLocal<?> key) {
            cleanUp();
            int index = ((ThreadLocal) key).hash & this.mask;
            while (true) {
                Object reference = this.table[index];
                if (reference == ((ThreadLocal) key).reference) {
                    this.table[index] = TOMBSTONE;
                    this.table[index + 1] = null;
                    this.tombstones++;
                    this.size--;
                    return;
                }
                if (reference != null) {
                    index = next(index);
                } else {
                    return;
                }
            }
        }

        private int next(int index) {
            return (index + 2) & this.mask;
        }
    }
}
