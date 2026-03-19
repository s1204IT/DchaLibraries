package java.lang;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ThreadLocal<T> {
    private static final int HASH_INCREMENT = 1640531527;
    private static AtomicInteger nextHashCode = new AtomicInteger();
    private final int threadLocalHashCode = nextHashCode();

    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    protected T initialValue() {
        return null;
    }

    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal(supplier);
    }

    public T get() {
        ThreadLocalMap.Entry entry;
        ThreadLocalMap map = getMap(Thread.currentThread());
        if (map != null && (entry = map.getEntry(this)) != null) {
            return (T) entry.value;
        }
        return setInitialValue();
    }

    private T setInitialValue() {
        T tInitialValue = initialValue();
        Thread threadCurrentThread = Thread.currentThread();
        ThreadLocalMap map = getMap(threadCurrentThread);
        if (map == null) {
            createMap(threadCurrentThread, tInitialValue);
        } else {
            map.set(this, tInitialValue);
        }
        return tInitialValue;
    }

    public void set(T t) {
        Thread threadCurrentThread = Thread.currentThread();
        ThreadLocalMap map = getMap(threadCurrentThread);
        if (map == null) {
            createMap(threadCurrentThread, t);
        } else {
            map.set(this, t);
        }
    }

    public void remove() {
        ThreadLocalMap map = getMap(Thread.currentThread());
        if (map == null) {
            return;
        }
        map.remove(this);
    }

    ThreadLocalMap getMap(Thread thread) {
        return thread.threadLocals;
    }

    void createMap(Thread thread, T t) {
        thread.threadLocals = new ThreadLocalMap((ThreadLocal<?>) this, (Object) t);
    }

    static ThreadLocalMap createInheritedMap(ThreadLocalMap threadLocalMap) {
        return new ThreadLocalMap(threadLocalMap);
    }

    T childValue(T t) {
        throw new UnsupportedOperationException();
    }

    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {
        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = (Supplier) Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return this.supplier.get();
        }
    }

    static class ThreadLocalMap {
        private static final int INITIAL_CAPACITY = 16;
        private int size;
        private Entry[] table;
        private int threshold;

        static class Entry extends WeakReference<ThreadLocal<?>> {
            Object value;

            Entry(ThreadLocal<?> threadLocal, Object obj) {
                super(threadLocal);
                this.value = obj;
            }
        }

        private void setThreshold(int i) {
            this.threshold = (i * 2) / 3;
        }

        private static int nextIndex(int i, int i2) {
            int i3 = i + 1;
            if (i3 < i2) {
                return i3;
            }
            return 0;
        }

        private static int prevIndex(int i, int i2) {
            int i3 = i - 1;
            return i3 >= 0 ? i3 : i2 - 1;
        }

        ThreadLocalMap(ThreadLocal<?> threadLocal, Object obj) {
            this.size = 0;
            this.table = new Entry[16];
            this.table[((ThreadLocal) threadLocal).threadLocalHashCode & 15] = new Entry(threadLocal, obj);
            this.size = 1;
            setThreshold(16);
        }

        private ThreadLocalMap(ThreadLocalMap threadLocalMap) {
            ThreadLocal<?> threadLocal;
            this.size = 0;
            Entry[] entryArr = threadLocalMap.table;
            int length = entryArr.length;
            setThreshold(length);
            this.table = new Entry[length];
            for (Entry entry : entryArr) {
                if (entry != null && (threadLocal = entry.get()) != null) {
                    Entry entry2 = new Entry(threadLocal, threadLocal.childValue(entry.value));
                    int iNextIndex = ((ThreadLocal) threadLocal).threadLocalHashCode & (length - 1);
                    while (this.table[iNextIndex] != null) {
                        iNextIndex = nextIndex(iNextIndex, length);
                    }
                    this.table[iNextIndex] = entry2;
                    this.size++;
                }
            }
        }

        private Entry getEntry(ThreadLocal<?> threadLocal) {
            int length = ((ThreadLocal) threadLocal).threadLocalHashCode & (this.table.length - 1);
            Entry entry = this.table[length];
            if (entry != null && entry.get() == threadLocal) {
                return entry;
            }
            return getEntryAfterMiss(threadLocal, length, entry);
        }

        private Entry getEntryAfterMiss(ThreadLocal<?> threadLocal, int i, Entry entry) {
            Entry[] entryArr = this.table;
            int length = entryArr.length;
            while (entry != null) {
                ThreadLocal<?> threadLocal2 = entry.get();
                if (threadLocal2 == threadLocal) {
                    return entry;
                }
                if (threadLocal2 == null) {
                    expungeStaleEntry(i);
                } else {
                    i = nextIndex(i, length);
                }
                entry = entryArr[i];
            }
            return null;
        }

        private void set(ThreadLocal<?> threadLocal, Object obj) {
            Entry[] entryArr = this.table;
            int length = entryArr.length;
            int iNextIndex = ((ThreadLocal) threadLocal).threadLocalHashCode & (length - 1);
            Entry entry = entryArr[iNextIndex];
            while (entry != null) {
                ThreadLocal<?> threadLocal2 = entry.get();
                if (threadLocal2 == threadLocal) {
                    entry.value = obj;
                    return;
                } else if (threadLocal2 != null) {
                    iNextIndex = nextIndex(iNextIndex, length);
                    entry = entryArr[iNextIndex];
                } else {
                    replaceStaleEntry(threadLocal, obj, iNextIndex);
                    return;
                }
            }
            entryArr[iNextIndex] = new Entry(threadLocal, obj);
            int i = this.size + 1;
            this.size = i;
            if (!cleanSomeSlots(iNextIndex, i) && i >= this.threshold) {
                rehash();
            }
        }

        private void remove(ThreadLocal<?> threadLocal) {
            Entry[] entryArr = this.table;
            int length = entryArr.length;
            int iNextIndex = ((ThreadLocal) threadLocal).threadLocalHashCode & (length - 1);
            Entry entry = entryArr[iNextIndex];
            while (entry != null) {
                if (entry.get() != threadLocal) {
                    iNextIndex = nextIndex(iNextIndex, length);
                    entry = entryArr[iNextIndex];
                } else {
                    entry.clear();
                    expungeStaleEntry(iNextIndex);
                    return;
                }
            }
        }

        private void replaceStaleEntry(ThreadLocal<?> threadLocal, Object obj, int i) {
            Entry[] entryArr = this.table;
            int length = entryArr.length;
            int iPrevIndex = prevIndex(i, length);
            int i2 = i;
            while (true) {
                Entry entry = entryArr[iPrevIndex];
                if (entry == null) {
                    break;
                }
                if (entry.get() == null) {
                    i2 = iPrevIndex;
                }
                iPrevIndex = prevIndex(iPrevIndex, length);
            }
            int iNextIndex = nextIndex(i, length);
            while (true) {
                Entry entry2 = entryArr[iNextIndex];
                if (entry2 != null) {
                    ThreadLocal<?> threadLocal2 = entry2.get();
                    if (threadLocal2 == threadLocal) {
                        entry2.value = obj;
                        entryArr[iNextIndex] = entryArr[i];
                        entryArr[i] = entry2;
                        if (i2 != i) {
                            iNextIndex = i2;
                        }
                        cleanSomeSlots(expungeStaleEntry(iNextIndex), length);
                        return;
                    }
                    if (threadLocal2 == null && i2 == i) {
                        i2 = iNextIndex;
                    }
                    iNextIndex = nextIndex(iNextIndex, length);
                } else {
                    entryArr[i].value = null;
                    entryArr[i] = new Entry(threadLocal, obj);
                    if (i2 != i) {
                        cleanSomeSlots(expungeStaleEntry(i2), length);
                        return;
                    }
                    return;
                }
            }
        }

        private int expungeStaleEntry(int i) {
            Entry[] entryArr = this.table;
            int length = entryArr.length;
            entryArr[i].value = null;
            entryArr[i] = null;
            this.size--;
            int iNextIndex = nextIndex(i, length);
            while (true) {
                Entry entry = entryArr[iNextIndex];
                if (entry != null) {
                    ThreadLocal<?> threadLocal = entry.get();
                    if (threadLocal != null) {
                        int iNextIndex2 = ((ThreadLocal) threadLocal).threadLocalHashCode & (length - 1);
                        if (iNextIndex2 != iNextIndex) {
                            entryArr[iNextIndex] = null;
                            while (entryArr[iNextIndex2] != null) {
                                iNextIndex2 = nextIndex(iNextIndex2, length);
                            }
                            entryArr[iNextIndex2] = entry;
                        }
                    } else {
                        entry.value = null;
                        entryArr[iNextIndex] = null;
                        this.size--;
                    }
                    iNextIndex = nextIndex(iNextIndex, length);
                } else {
                    return iNextIndex;
                }
            }
        }

        private boolean cleanSomeSlots(int i, int i2) {
            Entry[] entryArr = this.table;
            int length = entryArr.length;
            boolean z = false;
            do {
                i = nextIndex(i, length);
                Entry entry = entryArr[i];
                if (entry != null && entry.get() == null) {
                    i = expungeStaleEntry(i);
                    i2 = length;
                    z = true;
                }
                i2 >>>= 1;
            } while (i2 != 0);
            return z;
        }

        private void rehash() {
            expungeStaleEntries();
            if (this.size >= this.threshold - (this.threshold / 4)) {
                resize();
            }
        }

        private void resize() {
            Entry[] entryArr = this.table;
            int length = entryArr.length * 2;
            Entry[] entryArr2 = new Entry[length];
            int i = 0;
            for (Entry entry : entryArr) {
                if (entry != null) {
                    ThreadLocal<?> threadLocal = entry.get();
                    if (threadLocal != null) {
                        int iNextIndex = ((ThreadLocal) threadLocal).threadLocalHashCode & (length - 1);
                        while (entryArr2[iNextIndex] != null) {
                            iNextIndex = nextIndex(iNextIndex, length);
                        }
                        entryArr2[iNextIndex] = entry;
                        i++;
                    } else {
                        entry.value = null;
                    }
                }
            }
            setThreshold(length);
            this.size = i;
            this.table = entryArr2;
        }

        private void expungeStaleEntries() {
            Entry[] entryArr = this.table;
            int length = entryArr.length;
            for (int i = 0; i < length; i++) {
                Entry entry = entryArr[i];
                if (entry != null && entry.get() == null) {
                    expungeStaleEntry(i);
                }
            }
        }
    }
}
