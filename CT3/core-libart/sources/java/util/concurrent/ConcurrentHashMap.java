package java.util.concurrent;

import android.icu.impl.Normalizer2Impl;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import sun.misc.Unsafe;

public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
    private static final int ABASE;
    private static final int ASHIFT;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final int DEFAULT_CAPACITY = 16;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    static final int HASH_BITS = Integer.MAX_VALUE;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1073741824;
    static final int MAX_ARRAY_SIZE = 2147483639;
    private static final int MAX_RESIZERS = 65535;
    private static final int MIN_TRANSFER_STRIDE = 16;
    static final int MIN_TREEIFY_CAPACITY = 64;
    static final int MOVED = -1;
    static final int RESERVED = -3;
    private static final int RESIZE_STAMP_BITS = 16;
    private static final int RESIZE_STAMP_SHIFT = 16;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    static final int TREEBIN = -2;
    static final int TREEIFY_THRESHOLD = 8;
    static final int UNTREEIFY_THRESHOLD = 6;
    private static final long serialVersionUID = 7249069246763182397L;
    private volatile transient long baseCount;
    private volatile transient int cellsBusy;
    private volatile transient CounterCell[] counterCells;
    private transient EntrySetView<K, V> entrySet;
    private transient KeySetView<K, V> keySet;
    private volatile transient Node<K, V>[] nextTable;
    private volatile transient int sizeCtl;
    volatile transient Node<K, V>[] table;
    private volatile transient int transferIndex;
    private transient ValuesView<K, V> values;
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("segments", Segment[].class), new ObjectStreamField("segmentMask", Integer.TYPE), new ObjectStreamField("segmentShift", Integer.TYPE)};
    private static final Unsafe U = Unsafe.getUnsafe();

    static {
        try {
            SIZECTL = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("cellsBusy"));
            CELLVALUE = U.objectFieldOffset(CounterCell.class.getDeclaredField("value"));
            ABASE = U.arrayBaseOffset(Node[].class);
            int scale = U.arrayIndexScale(Node[].class);
            if (((scale - 1) & scale) != 0) {
                throw new Error("array index scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        volatile Node<K, V> next;
        volatile V val;

        Node(int hash, K key, V val, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        @Override
        public final K getKey() {
            return this.key;
        }

        @Override
        public final V getValue() {
            return this.val;
        }

        @Override
        public final int hashCode() {
            return this.key.hashCode() ^ this.val.hashCode();
        }

        public final String toString() {
            return Helpers.mapEntryToString(this.key, this.val);
        }

        @Override
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean equals(Object o) {
            Map.Entry<?, ?> e;
            Object k;
            Object v;
            if (!(o instanceof Map.Entry) || (k = (e = (Map.Entry) o).getKey()) == null || (v = e.getValue()) == null || !(k == this.key || k.equals(this.key))) {
                return false;
            }
            Object u = this.val;
            if (v != u) {
                return v.equals(u);
            }
            return true;
        }

        Node<K, V> find(int h, Object k) {
            K ek;
            Node<K, V> e = this;
            if (k != null) {
                do {
                    if (e.hash == h && ((ek = e.key) == k || (ek != null && k.equals(ek)))) {
                        return e;
                    }
                    e = e.next;
                } while (e != null);
            }
            return null;
        }
    }

    static final int spread(int h) {
        return ((h >>> 16) ^ h) & Integer.MAX_VALUE;
    }

    private static final int tableSizeFor(int c) {
        int n = c - 1;
        int n2 = n | (n >>> 1);
        int n3 = n2 | (n2 >>> 2);
        int n4 = n3 | (n3 >>> 4);
        int n5 = n4 | (n4 >>> 8);
        int n6 = n5 | (n5 >>> 16);
        if (n6 < 0) {
            return 1;
        }
        if (n6 < 1073741824) {
            return n6 + 1;
        }
        return 1073741824;
    }

    static Class<?> comparableClassFor(Object x) {
        Type[] as;
        if (x instanceof Comparable) {
            Class<?> c = x.getClass();
            if (c == String.class) {
                return c;
            }
            Type[] ts = c.getGenericInterfaces();
            if (ts != null) {
                for (Type t : ts) {
                    if (t instanceof ParameterizedType) {
                        ParameterizedType p = (ParameterizedType) t;
                        if (p.getRawType() == Comparable.class && (as = p.getActualTypeArguments()) != null && as.length == 1 && as[0] == c) {
                            return c;
                        }
                    }
                }
            }
        }
        return null;
    }

    static int compareComparables(Class<?> kc, Object k, Object x) {
        if (x == null || x.getClass() != kc) {
            return 0;
        }
        return ((Comparable) k).compareTo(x);
    }

    static final <K, V> Node<K, V> tabAt(Node<K, V>[] tab, int i) {
        return (Node) U.getObjectVolatile(tab, (((long) i) << ASHIFT) + ((long) ABASE));
    }

    static final <K, V> boolean casTabAt(Node<K, V>[] tab, int i, Node<K, V> c, Node<K, V> v) {
        return U.compareAndSwapObject(tab, (((long) i) << ASHIFT) + ((long) ABASE), c, v);
    }

    static final <K, V> void setTabAt(Node<K, V>[] tab, int i, Node<K, V> v) {
        U.putObjectVolatile(tab, (((long) i) << ASHIFT) + ((long) ABASE), v);
    }

    public ConcurrentHashMap() {
    }

    public ConcurrentHashMap(int initialCapacity) {
        int cap;
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        if (initialCapacity >= 536870912) {
            cap = 1073741824;
        } else {
            cap = tableSizeFor((initialCapacity >>> 1) + initialCapacity + 1);
        }
        this.sizeCtl = cap;
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = 16;
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }
        long size = (long) (((double) ((initialCapacity < concurrencyLevel ? concurrencyLevel : initialCapacity) / loadFactor)) + 1.0d);
        int cap = size >= 1073741824 ? 1073741824 : tableSizeFor((int) size);
        this.sizeCtl = cap;
    }

    @Override
    public int size() {
        long n = sumCount();
        if (n < 0) {
            return 0;
        }
        if (n > 2147483647L) {
            return Integer.MAX_VALUE;
        }
        return (int) n;
    }

    @Override
    public boolean isEmpty() {
        return sumCount() <= 0;
    }

    @Override
    public V get(Object key) {
        int n;
        Node<K, V> e;
        K ek;
        int h = spread(key.hashCode());
        Node<K, V>[] tab = this.table;
        if (tab != null && (n = tab.length) > 0 && (e = tabAt(tab, (n - 1) & h)) != null) {
            int eh = e.hash;
            if (eh == h) {
                K ek2 = e.key;
                if (ek2 == key || (ek2 != null && key.equals(ek2))) {
                    return e.val;
                }
            } else if (eh < 0) {
                Node<K, V> p = e.find(h, key);
                if (p != null) {
                    return p.val;
                }
                return null;
            }
            while (true) {
                e = e.next;
                if (e == null) {
                    break;
                }
                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                    break;
                }
            }
            return e.val;
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] t = this.table;
        if (t != null) {
            Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
            while (true) {
                Node<K, V> p = it.advance();
                if (p == null) {
                    break;
                }
                V v = p.val;
                if (v == value) {
                    return true;
                }
                if (v != null && value.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    final V putVal(K key, V value, boolean onlyIfAbsent) {
        K ek;
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        int hash = spread(key.hashCode());
        int binCount = 0;
        Node<K, V>[] tab = this.table;
        while (true) {
            if (tab != null) {
                int n = tab.length;
                if (n != 0) {
                    int i = (n - 1) & hash;
                    Node<K, V> f = tabAt(tab, i);
                    if (f == null) {
                        if (casTabAt(tab, i, null, new Node(hash, key, value, null))) {
                            break;
                        }
                    } else {
                        int fh = f.hash;
                        if (fh == -1) {
                            tab = helpTransfer(tab, f);
                        } else {
                            V oldVal = null;
                            synchronized (f) {
                                if (tabAt(tab, i) == f) {
                                    if (fh >= 0) {
                                        binCount = 1;
                                        Node<K, V> e = f;
                                        while (true) {
                                            if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                                break;
                                            }
                                            Node<K, V> pred = e;
                                            e = e.next;
                                            if (e != null) {
                                                binCount++;
                                            } else {
                                                pred.next = new Node<>(hash, key, value, null);
                                                break;
                                            }
                                        }
                                        oldVal = e.val;
                                        if (!onlyIfAbsent) {
                                            e.val = value;
                                        }
                                    } else if (f instanceof TreeBin) {
                                        binCount = 2;
                                        Node<K, V> p = ((TreeBin) f).putTreeVal(hash, key, value);
                                        if (p != null) {
                                            oldVal = p.val;
                                            if (!onlyIfAbsent) {
                                                p.val = value;
                                            }
                                        }
                                    } else if (f instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                            }
                            if (binCount != 0) {
                                if (binCount >= 8) {
                                    treeifyBin(tab, i);
                                }
                                if (oldVal != null) {
                                    return oldVal;
                                }
                            }
                        }
                    }
                }
            }
            tab = initTable();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            putVal(e.getKey(), e.getValue(), false);
        }
    }

    @Override
    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    final V replaceNode(Object key, V value, Object cv) {
        int i;
        Node<K, V> f;
        TreeNode<K, V> p;
        K ek;
        int hash = spread(key.hashCode());
        Node<K, V>[] tab = this.table;
        while (tab != null) {
            int n = tab.length;
            if (n != 0 && (f = tabAt(tab, (i = (n - 1) & hash))) != null) {
                int fh = f.hash;
                if (fh == -1) {
                    tab = helpTransfer(tab, f);
                } else {
                    V oldVal = null;
                    boolean validated = false;
                    synchronized (f) {
                        if (tabAt(tab, i) == f) {
                            if (fh >= 0) {
                                validated = true;
                                Node<K, V> e = f;
                                Node<K, V> node = null;
                                do {
                                    if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                        V ev = e.val;
                                        if (cv == null || cv == ev || (ev != null && cv.equals(ev))) {
                                            oldVal = ev;
                                            if (value != null) {
                                                e.val = value;
                                            } else if (node != null) {
                                                node.next = e.next;
                                            } else {
                                                setTabAt(tab, i, e.next);
                                            }
                                        }
                                    } else {
                                        node = e;
                                        e = e.next;
                                    }
                                } while (e != null);
                            } else if (f instanceof TreeBin) {
                                validated = true;
                                TreeBin<K, V> t = (TreeBin) f;
                                TreeNode<K, V> r = t.root;
                                if (r != null && (p = r.findTreeNode(hash, key, null)) != null) {
                                    V pv = p.val;
                                    if (cv == null || cv == pv || (pv != null && cv.equals(pv))) {
                                        oldVal = pv;
                                        if (value != null) {
                                            p.val = value;
                                        } else if (t.removeTreeNode(p)) {
                                            setTabAt(tab, i, untreeify(t.first));
                                        }
                                    }
                                }
                            } else if (f instanceof ReservationNode) {
                                throw new IllegalStateException("Recursive update");
                            }
                        }
                    }
                    if (validated) {
                        if (oldVal != null) {
                            if (value == null) {
                                addCount(-1L, -1);
                            }
                            return oldVal;
                        }
                        return null;
                    }
                }
            } else {
                return null;
            }
        }
        return null;
    }

    @Override
    public void clear() throws Throwable {
        int i;
        Node<K, V> p;
        long delta = 0;
        Node<K, V>[] tab = this.table;
        for (int i2 = 0; tab != null && i2 < tab.length; i2 = i) {
            Node<K, V> f = tabAt(tab, i2);
            if (f == null) {
                i = i2 + 1;
            } else {
                int fh = f.hash;
                if (fh == -1) {
                    tab = helpTransfer(tab, f);
                    i = 0;
                } else {
                    synchronized (f) {
                        try {
                            if (tabAt(tab, i2) == f) {
                                if (fh >= 0) {
                                    p = f;
                                } else {
                                    p = f instanceof TreeBin ? ((TreeBin) f).first : null;
                                }
                                while (p != null) {
                                    delta--;
                                    p = p.next;
                                }
                                i = i2 + 1;
                                try {
                                    setTabAt(tab, i2, null);
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } else {
                                i = i2;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                }
            }
        }
        if (delta == 0) {
            return;
        }
        addCount(delta, -1);
    }

    @Override
    public Set<K> keySet() {
        KeySetView<K, V> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySetView<K, V> ks2 = new KeySetView<>(this, null);
        this.keySet = ks2;
        return ks2;
    }

    @Override
    public Collection<V> values() {
        ValuesView<K, V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        ValuesView<K, V> vs2 = new ValuesView<>(this);
        this.values = vs2;
        return vs2;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        EntrySetView<K, V> es = this.entrySet;
        if (es != null) {
            return es;
        }
        EntrySetView<K, V> es2 = new EntrySetView<>(this);
        this.entrySet = es2;
        return es2;
    }

    @Override
    public int hashCode() {
        int h = 0;
        Node<K, V>[] t = this.table;
        if (t != null) {
            Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
            while (true) {
                Node<K, V> p = it.advance();
                if (p == null) {
                    break;
                }
                h += p.key.hashCode() ^ p.val.hashCode();
            }
        }
        return h;
    }

    @Override
    public String toString() {
        Node<K, V>[] t = this.table;
        int f = t == null ? 0 : t.length;
        Traverser<K, V> it = new Traverser<>(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K, V> p = it.advance();
        if (p != null) {
            while (true) {
                Object obj = p.key;
                Object obj2 = p.val;
                if (obj == this) {
                    obj = "(this Map)";
                }
                sb.append(obj);
                sb.append('=');
                if (obj2 == this) {
                    obj2 = "(this Map)";
                }
                sb.append(obj2);
                p = it.advance();
                if (p == null) {
                    break;
                }
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    @Override
    public boolean equals(Object o) {
        Object mv;
        Object v;
        if (o != this) {
            if (!(o instanceof Map)) {
                return false;
            }
            Map<?, ?> m = (Map) o;
            Node<K, V>[] t = this.table;
            int f = t == null ? 0 : t.length;
            Traverser<K, V> it = new Traverser<>(t, f, 0, f);
            while (true) {
                Node<K, V> p = it.advance();
                if (p != null) {
                    V val = p.val;
                    Object v2 = m.get(p.key);
                    if (v2 == null || (v2 != val && !v2.equals(val))) {
                        break;
                    }
                } else {
                    Iterator e$iterator = m.entrySet().iterator();
                    while (e$iterator.hasNext()) {
                        Map.Entry<?, ?> e = (Map.Entry) e$iterator.next();
                        Object mk = e.getKey();
                        if (mk == null || (mv = e.getValue()) == null || (v = get(mk)) == null || (mv != v && !mv.equals(v))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    static class Segment<K, V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;

        Segment(float lf) {
            this.loadFactor = lf;
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        int sshift = 0;
        int ssize = 1;
        while (ssize < 16) {
            sshift++;
            ssize <<= 1;
        }
        int segmentShift = 32 - sshift;
        int segmentMask = ssize - 1;
        Segment<K, V>[] segments = new Segment[16];
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment<>(LOAD_FACTOR);
        }
        ObjectOutputStream.PutField streamFields = s.putFields();
        streamFields.put("segments", segments);
        streamFields.put("segmentShift", segmentShift);
        streamFields.put("segmentMask", segmentMask);
        s.writeFields();
        Node<K, V>[] t = this.table;
        if (t != null) {
            Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
            while (true) {
                Node<K, V> p = it.advance();
                if (p == null) {
                    break;
                }
                s.writeObject(p.key);
                s.writeObject(p.val);
            }
        }
        s.writeObject(null);
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        int n;
        boolean insertAtFront;
        this.sizeCtl = -1;
        s.defaultReadObject();
        long size = 0;
        Node<K, V> node = null;
        while (true) {
            Object object = s.readObject();
            Object object2 = s.readObject();
            if (object == null || object2 == null) {
                break;
            }
            Node<K, V> p = new Node<>(spread(object.hashCode()), object, object2, node);
            size++;
            node = p;
        }
        if (size == 0) {
            this.sizeCtl = 0;
            return;
        }
        if (size >= 536870912) {
            n = 1073741824;
        } else {
            int sz = (int) size;
            n = tableSizeFor((sz >>> 1) + sz + 1);
        }
        Node<K, V>[] tab = new Node[n];
        int mask = n - 1;
        long added = 0;
        while (node != null) {
            Node<K, V> next = node.next;
            int h = node.hash;
            int j = h & mask;
            Node<K, V> first = tabAt(tab, j);
            if (first == null) {
                insertAtFront = true;
            } else {
                K k = node.key;
                if (first.hash < 0) {
                    if (((TreeBin) first).putTreeVal(h, k, node.val) == null) {
                        added++;
                    }
                    insertAtFront = false;
                } else {
                    int binCount = 0;
                    insertAtFront = true;
                    for (Node<K, V> q = first; q != null; q = q.next) {
                        if (q.hash == h) {
                            K qk = q.key;
                            if (qk == k || (qk != null && k.equals(qk))) {
                                insertAtFront = false;
                                break;
                            }
                        }
                        binCount++;
                    }
                    if (insertAtFront && binCount >= 8) {
                        insertAtFront = false;
                        added++;
                        node.next = first;
                        TreeNode<K, V> hd = null;
                        TreeNode<K, V> tl = null;
                        for (Node<K, V> q2 = node; q2 != null; q2 = q2.next) {
                            TreeNode<K, V> t = new TreeNode<>(q2.hash, q2.key, q2.val, null, null);
                            t.prev = tl;
                            if (tl == null) {
                                hd = t;
                            } else {
                                tl.next = t;
                            }
                            tl = t;
                        }
                        setTabAt(tab, j, new TreeBin(hd));
                    }
                }
            }
            if (insertAtFront) {
                added++;
                node.next = first;
                setTabAt(tab, j, node);
            }
            node = next;
        }
        this.table = tab;
        this.sizeCtl = n - (n >>> 2);
        this.baseCount = added;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        return (value == null || replaceNode(key, null, value) == null) ? false : true;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        return replaceNode(key, newValue, oldValue) != null;
    }

    @Override
    public V replace(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        return replaceNode(key, value, null);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v == null ? defaultValue : v;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        if (biConsumer == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr == null) {
            return;
        }
        Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
        while (true) {
            Node<K, V> nodeAdvance = traverser.advance();
            if (nodeAdvance == null) {
                return;
            } else {
                biConsumer.accept(nodeAdvance.key, nodeAdvance.val);
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr == null) {
            return;
        }
        Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
        while (true) {
            Node<K, V> nodeAdvance = traverser.advance();
            if (nodeAdvance == null) {
                return;
            }
            V v = nodeAdvance.val;
            K k = nodeAdvance.key;
            do {
                V vApply = biFunction.apply(k, v);
                if (vApply == null) {
                    throw new NullPointerException();
                }
                if (replaceNode(k, vApply, v) == null) {
                    v = get(k);
                }
            } while (v != null);
        }
    }

    boolean removeEntryIf(Predicate<? super Map.Entry<K, V>> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        boolean removed = false;
        Node<K, V>[] t = this.table;
        if (t != null) {
            Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
            while (true) {
                Node<K, V> p = it.advance();
                if (p == null) {
                    break;
                }
                K k = p.key;
                V v = p.val;
                Map.Entry<K, V> e = new AbstractMap.SimpleImmutableEntry<>(k, v);
                if (function.test(e) && replaceNode(k, null, v) != null) {
                    removed = true;
                }
            }
        }
        return removed;
    }

    boolean removeValueIf(Predicate<? super V> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        boolean z = false;
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null) {
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    break;
                }
                K k = nodeAdvance.key;
                V v = nodeAdvance.val;
                if (predicate.test(v) && replaceNode(k, null, v) != null) {
                    z = true;
                }
            }
        }
        return z;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        TreeNode<K, V> p;
        K ek;
        if (key == null || mappingFunction == null) {
            throw new NullPointerException();
        }
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        Node<K, V>[] tab = this.table;
        while (true) {
            if (tab != null) {
                int n = tab.length;
                if (n != 0) {
                    int i = (n - 1) & h;
                    Node<K, V> f = tabAt(tab, i);
                    if (f == null) {
                        Node<K, V> r = new ReservationNode<>();
                        synchronized (r) {
                            if (casTabAt(tab, i, null, r)) {
                                binCount = 1;
                                Node<K, V> node = null;
                                try {
                                    val = mappingFunction.apply(key);
                                    if (val != null) {
                                        node = new Node<>(h, key, val, null);
                                    }
                                } finally {
                                }
                            }
                        }
                        if (binCount != 0) {
                            break;
                        }
                    } else {
                        int fh = f.hash;
                        if (fh == -1) {
                            tab = helpTransfer(tab, f);
                        } else {
                            boolean added = false;
                            synchronized (f) {
                                if (tabAt(tab, i) == f) {
                                    if (fh >= 0) {
                                        binCount = 1;
                                        Node<K, V> e = f;
                                        while (true) {
                                            if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                                break;
                                            }
                                            Node<K, V> pred = e;
                                            e = e.next;
                                            if (e != null) {
                                                binCount++;
                                            } else {
                                                val = mappingFunction.apply(key);
                                                if (val != null) {
                                                    if (pred.next != null) {
                                                        throw new IllegalStateException("Recursive update");
                                                    }
                                                    added = true;
                                                    pred.next = new Node<>(h, key, val, null);
                                                }
                                            }
                                        }
                                        val = e.val;
                                    } else if (f instanceof TreeBin) {
                                        binCount = 2;
                                        TreeBin<K, V> t = (TreeBin) f;
                                        TreeNode<K, V> r2 = t.root;
                                        if (r2 != null && (p = r2.findTreeNode(h, key, null)) != null) {
                                            val = p.val;
                                        } else {
                                            val = mappingFunction.apply(key);
                                            if (val != null) {
                                                added = true;
                                                t.putTreeVal(h, key, val);
                                            }
                                        }
                                    } else if (f instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                            }
                            if (binCount != 0) {
                                if (binCount >= 8) {
                                    treeifyBin(tab, i);
                                }
                                if (!added) {
                                    return val;
                                }
                            }
                        }
                    }
                }
            }
            tab = initTable();
        }
    }

    @Override
    public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        TreeNode<K, V> treeNodeFindTreeNode;
        K k2;
        if (k == null || biFunction == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        V vApply = null;
        int i = 0;
        int i2 = 0;
        Node<K, V>[] nodeArrInitTable = this.table;
        while (true) {
            if (nodeArrInitTable != null) {
                int length = nodeArrInitTable.length;
                if (length != 0) {
                    int i3 = (length - 1) & iSpread;
                    Node<K, V> nodeTabAt = tabAt(nodeArrInitTable, i3);
                    if (nodeTabAt == null) {
                        break;
                    }
                    int i4 = nodeTabAt.hash;
                    if (i4 == -1) {
                        nodeArrInitTable = helpTransfer(nodeArrInitTable, nodeTabAt);
                    } else {
                        synchronized (nodeTabAt) {
                            if (tabAt(nodeArrInitTable, i3) == nodeTabAt) {
                                if (i4 >= 0) {
                                    i2 = 1;
                                    Node<K, V> node = nodeTabAt;
                                    Node<K, V> node2 = null;
                                    while (true) {
                                        if (node.hash == iSpread && ((k2 = node.key) == k || (k2 != null && k.equals(k2)))) {
                                            break;
                                        }
                                        node2 = node;
                                        node = node.next;
                                        if (node == null) {
                                            break;
                                        }
                                        i2++;
                                    }
                                } else if (nodeTabAt instanceof TreeBin) {
                                    i2 = 2;
                                    TreeBin treeBin = (TreeBin) nodeTabAt;
                                    TreeNode<K, V> treeNode = treeBin.root;
                                    if (treeNode != null && (treeNodeFindTreeNode = treeNode.findTreeNode(iSpread, k, null)) != null) {
                                        vApply = biFunction.apply(k, treeNodeFindTreeNode.val);
                                        if (vApply != null) {
                                            treeNodeFindTreeNode.val = vApply;
                                        } else {
                                            i = -1;
                                            if (treeBin.removeTreeNode(treeNodeFindTreeNode)) {
                                                setTabAt(nodeArrInitTable, i3, untreeify(treeBin.first));
                                            }
                                        }
                                    }
                                } else if (nodeTabAt instanceof ReservationNode) {
                                    throw new IllegalStateException("Recursive update");
                                }
                            }
                        }
                        if (i2 != 0) {
                            break;
                        }
                    }
                }
            }
            nodeArrInitTable = initTable();
        }
    }

    @Override
    public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        TreeNode<K, V> treeNodeFindTreeNode;
        K k2;
        if (k == null || biFunction == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        V vApply = null;
        int i = 0;
        int i2 = 0;
        Node<K, V>[] nodeArrInitTable = this.table;
        while (true) {
            if (nodeArrInitTable != null) {
                int length = nodeArrInitTable.length;
                if (length != 0) {
                    int i3 = (length - 1) & iSpread;
                    Node<K, V> nodeTabAt = tabAt(nodeArrInitTable, i3);
                    if (nodeTabAt == null) {
                        ReservationNode reservationNode = new ReservationNode();
                        synchronized (reservationNode) {
                            if (casTabAt(nodeArrInitTable, i3, null, reservationNode)) {
                                i2 = 1;
                                Node<K, V> node = null;
                                try {
                                    vApply = biFunction.apply(k, null);
                                    if (vApply != null) {
                                        i = 1;
                                        node = new Node<>(iSpread, k, vApply, null);
                                    }
                                } finally {
                                }
                            }
                        }
                        if (i2 != 0) {
                            break;
                        }
                    } else {
                        int i4 = nodeTabAt.hash;
                        if (i4 == -1) {
                            nodeArrInitTable = helpTransfer(nodeArrInitTable, nodeTabAt);
                        } else {
                            synchronized (nodeTabAt) {
                                if (tabAt(nodeArrInitTable, i3) == nodeTabAt) {
                                    if (i4 >= 0) {
                                        i2 = 1;
                                        Node<K, V> node2 = nodeTabAt;
                                        Node<K, V> node3 = null;
                                        while (true) {
                                            if (node2.hash == iSpread && ((k2 = node2.key) == k || (k2 != null && k.equals(k2)))) {
                                                break;
                                            }
                                            node3 = node2;
                                            node2 = node2.next;
                                            if (node2 != null) {
                                                i2++;
                                            } else {
                                                vApply = biFunction.apply(k, null);
                                                if (vApply != null) {
                                                    if (node3.next != null) {
                                                        throw new IllegalStateException("Recursive update");
                                                    }
                                                    i = 1;
                                                    node3.next = new Node<>(iSpread, k, vApply, null);
                                                }
                                            }
                                        }
                                        vApply = biFunction.apply(k, node2.val);
                                        if (vApply != null) {
                                            node2.val = vApply;
                                        } else {
                                            i = -1;
                                            Node<K, V> node4 = node2.next;
                                            if (node3 != null) {
                                                node3.next = node4;
                                            } else {
                                                setTabAt(nodeArrInitTable, i3, node4);
                                            }
                                        }
                                    } else if (nodeTabAt instanceof TreeBin) {
                                        i2 = 1;
                                        TreeBin treeBin = (TreeBin) nodeTabAt;
                                        TreeNode<K, V> treeNode = treeBin.root;
                                        if (treeNode != null) {
                                            treeNodeFindTreeNode = treeNode.findTreeNode(iSpread, k, null);
                                        } else {
                                            treeNodeFindTreeNode = null;
                                        }
                                        vApply = biFunction.apply(k, (Object) (treeNodeFindTreeNode == null ? null : treeNodeFindTreeNode.val));
                                        if (vApply != null) {
                                            if (treeNodeFindTreeNode != null) {
                                                treeNodeFindTreeNode.val = vApply;
                                            } else {
                                                i = 1;
                                                treeBin.putTreeVal(iSpread, k, vApply);
                                            }
                                        } else if (treeNodeFindTreeNode != null) {
                                            i = -1;
                                            if (treeBin.removeTreeNode(treeNodeFindTreeNode)) {
                                                setTabAt(nodeArrInitTable, i3, untreeify(treeBin.first));
                                            }
                                        }
                                    } else if (nodeTabAt instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                            }
                            if (i2 != 0) {
                                if (i2 >= 8) {
                                    treeifyBin(nodeArrInitTable, i3);
                                }
                            }
                        }
                    }
                }
            }
            nodeArrInitTable = initTable();
        }
    }

    @Override
    public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        K k2;
        if (k == null || v == null || biFunction == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        V vApply = null;
        int i = 0;
        int i2 = 0;
        Node<K, V>[] nodeArrInitTable = this.table;
        while (true) {
            if (nodeArrInitTable != null) {
                int length = nodeArrInitTable.length;
                if (length != 0) {
                    int i3 = (length - 1) & iSpread;
                    Node<K, V> nodeTabAt = tabAt(nodeArrInitTable, i3);
                    if (nodeTabAt == null) {
                        if (casTabAt(nodeArrInitTable, i3, null, new Node(iSpread, k, v, null))) {
                            i = 1;
                            vApply = v;
                            break;
                        }
                    } else {
                        int i4 = nodeTabAt.hash;
                        if (i4 == -1) {
                            nodeArrInitTable = helpTransfer(nodeArrInitTable, nodeTabAt);
                        } else {
                            synchronized (nodeTabAt) {
                                if (tabAt(nodeArrInitTable, i3) == nodeTabAt) {
                                    if (i4 >= 0) {
                                        i2 = 1;
                                        Node<K, V> node = nodeTabAt;
                                        Node<K, V> node2 = null;
                                        while (true) {
                                            if (node.hash == iSpread && ((k2 = node.key) == k || (k2 != null && k.equals(k2)))) {
                                                break;
                                            }
                                            node2 = node;
                                            node = node.next;
                                            if (node != null) {
                                                i2++;
                                            } else {
                                                i = 1;
                                                vApply = v;
                                                node2.next = new Node<>(iSpread, k, v, null);
                                                break;
                                            }
                                        }
                                        vApply = biFunction.apply(node.val, v);
                                        if (vApply != null) {
                                            node.val = vApply;
                                        } else {
                                            i = -1;
                                            Node<K, V> node3 = node.next;
                                            if (node2 != null) {
                                                node2.next = node3;
                                            } else {
                                                setTabAt(nodeArrInitTable, i3, node3);
                                            }
                                        }
                                    } else if (nodeTabAt instanceof TreeBin) {
                                        i2 = 2;
                                        TreeBin treeBin = (TreeBin) nodeTabAt;
                                        TreeNode<K, V> treeNode = treeBin.root;
                                        TreeNode<K, V> treeNodeFindTreeNode = treeNode == null ? null : treeNode.findTreeNode(iSpread, k, null);
                                        vApply = treeNodeFindTreeNode == null ? v : biFunction.apply(treeNodeFindTreeNode.val, v);
                                        if (vApply != null) {
                                            if (treeNodeFindTreeNode != null) {
                                                treeNodeFindTreeNode.val = vApply;
                                            } else {
                                                i = 1;
                                                treeBin.putTreeVal(iSpread, k, vApply);
                                            }
                                        } else if (treeNodeFindTreeNode != null) {
                                            i = -1;
                                            if (treeBin.removeTreeNode(treeNodeFindTreeNode)) {
                                                setTabAt(nodeArrInitTable, i3, untreeify(treeBin.first));
                                            }
                                        }
                                    } else if (nodeTabAt instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                            }
                            if (i2 != 0) {
                                if (i2 >= 8) {
                                    treeifyBin(nodeArrInitTable, i3);
                                }
                            }
                        }
                    }
                }
            }
            nodeArrInitTable = initTable();
        }
    }

    public boolean contains(Object value) {
        return containsValue(value);
    }

    public Enumeration<K> keys() {
        Node<K, V>[] t = this.table;
        int f = t == null ? 0 : t.length;
        return new KeyIterator(t, f, 0, f, this);
    }

    public Enumeration<V> elements() {
        Node<K, V>[] t = this.table;
        int f = t == null ? 0 : t.length;
        return new ValueIterator(t, f, 0, f, this);
    }

    public long mappingCount() {
        long n = sumCount();
        if (n < 0) {
            return 0L;
        }
        return n;
    }

    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView<>(new ConcurrentHashMap(), Boolean.TRUE);
    }

    public static <K> KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<>(new ConcurrentHashMap(initialCapacity), Boolean.TRUE);
    }

    public KeySetView<K, V> keySet(V mappedValue) {
        if (mappedValue == null) {
            throw new NullPointerException();
        }
        return new KeySetView<>(this, mappedValue);
    }

    static final class ForwardingNode<K, V> extends Node<K, V> {
        final Node<K, V>[] nextTable;

        ForwardingNode(Node<K, V>[] tab) {
            super(-1, null, null, null);
            this.nextTable = tab;
        }

        @Override
        Node<K, V> find(int h, Object k) {
            Node<K, V> e;
            K ek;
            Node<K, V>[] tab = this.nextTable;
            while (k != null && tab != null) {
                int n = tab.length;
                if (n == 0 || (e = ConcurrentHashMap.tabAt(tab, (n - 1) & h)) == null) {
                    break;
                }
                do {
                    int eh = e.hash;
                    if (eh == h && ((ek = e.key) == k || (ek != null && k.equals(ek)))) {
                        return e;
                    }
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode) e).nextTable;
                        } else {
                            return e.find(h, k);
                        }
                    } else {
                        e = e.next;
                    }
                } while (e != null);
                return null;
            }
            return null;
        }
    }

    static final class ReservationNode<K, V> extends Node<K, V> {
        ReservationNode() {
            super(ConcurrentHashMap.RESERVED, null, null, null);
        }

        @Override
        Node<K, V> find(int h, Object k) {
            return null;
        }
    }

    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | 32768;
    }

    private final Node<K, V>[] initTable() {
        while (true) {
            Node<K, V>[] tab = this.table;
            if (tab != null && tab.length != 0) {
                break;
            }
            int sc = this.sizeCtl;
            if (sc < 0) {
                Thread.yield();
            } else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    tab = this.table;
                    if (tab == null || tab.length == 0) {
                        int n = sc > 0 ? sc : 16;
                        Node<K, V>[] nt = new Node[n];
                        tab = nt;
                        this.table = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    this.sizeCtl = sc;
                }
            }
        }
    }

    private final void addCount(long x, int check) {
        int m;
        CounterCell a;
        long s;
        Node<K, V>[] tab;
        Node<K, V>[] nt;
        CounterCell[] as = this.counterCells;
        if (as == null) {
            Unsafe unsafe = U;
            long j = BASECOUNT;
            long b = this.baseCount;
            s = b + x;
            if (!unsafe.compareAndSwapLong(this, j, b, s)) {
                boolean uncontended = true;
                if (as != null && as.length - 1 >= 0 && (a = as[ThreadLocalRandom.getProbe() & m]) != null) {
                    Unsafe unsafe2 = U;
                    long j2 = CELLVALUE;
                    long v = a.value;
                    uncontended = unsafe2.compareAndSwapLong(a, j2, v, v + x);
                    if (uncontended) {
                        if (check <= 1) {
                            return;
                        } else {
                            s = sumCount();
                        }
                    }
                }
                fullAddCount(x, uncontended);
                return;
            }
        }
        if (check < 0) {
            return;
        }
        while (true) {
            int sc = this.sizeCtl;
            if (s < sc || (tab = this.table) == null) {
                return;
            }
            int n = tab.length;
            if (n >= 1073741824) {
                return;
            }
            int rs = resizeStamp(n);
            if (sc < 0) {
                if ((sc >>> 16) != rs || sc == rs + 1 || sc == 65535 + rs || (nt = this.nextTable) == null || this.transferIndex <= 0) {
                    return;
                }
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nt);
                }
            } else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << 16) + 2)) {
                transfer(tab, null);
            }
            s = sumCount();
        }
    }

    final Node<K, V>[] helpTransfer(Node<K, V>[] tab, Node<K, V> f) {
        Node<K, V>[] nextTab;
        int sc;
        if (tab != null && (f instanceof ForwardingNode) && (nextTab = ((ForwardingNode) f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            while (true) {
                if (nextTab != this.nextTable || this.table != tab || (sc = this.sizeCtl) >= 0 || (sc >>> 16) != rs || sc == rs + 1 || sc == 65535 + rs || this.transferIndex <= 0) {
                    break;
                }
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return this.table;
    }

    private final void tryPresize(int size) {
        int n;
        int c = size >= 536870912 ? 1073741824 : tableSizeFor((size >>> 1) + size + 1);
        while (true) {
            int sc = this.sizeCtl;
            if (sc < 0) {
                return;
            }
            Node<K, V>[] tab = this.table;
            if (tab == null || (n = tab.length) == 0) {
                int n2 = sc > c ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (this.table == tab) {
                            Node<K, V>[] nt = new Node[n2];
                            this.table = nt;
                            sc = n2 - (n2 >>> 2);
                        }
                    } finally {
                        this.sizeCtl = sc;
                    }
                } else {
                    continue;
                }
            } else {
                if (c <= sc || n >= 1073741824) {
                    return;
                }
                if (tab == this.table) {
                    int rs = resizeStamp(n);
                    if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << 16) + 2)) {
                        transfer(tab, null);
                    }
                }
            }
        }
    }

    private final void transfer(Node<K, V>[] tab, Node<K, V>[] nextTab) {
        Node<K, V> ln;
        Node<K, V> hn;
        Node<K, V> node;
        Node<K, V> ln2;
        Node<K, V> hn2;
        Node<K, V> ln3;
        int n = tab.length;
        int stride = NCPU > 1 ? (n >>> 3) / NCPU : n;
        if (stride < 16) {
            stride = 16;
        }
        if (nextTab == null) {
            try {
                Node<K, V>[] nt = new Node[n << 1];
                nextTab = nt;
                this.nextTable = nt;
                this.transferIndex = n;
            } catch (Throwable th) {
                this.sizeCtl = Integer.MAX_VALUE;
                return;
            }
        }
        int nextn = nextTab.length;
        ForwardingNode<K, V> fwd = new ForwardingNode<>(nextTab);
        boolean advance = true;
        boolean finishing = false;
        int i = 0;
        int bound = 0;
        while (true) {
            if (advance) {
                i--;
                if (i >= bound || finishing) {
                    advance = false;
                } else {
                    int nextIndex = this.transferIndex;
                    if (nextIndex <= 0) {
                        i = -1;
                        advance = false;
                    } else {
                        Unsafe unsafe = U;
                        long j = TRANSFERINDEX;
                        int nextBound = nextIndex > stride ? nextIndex - stride : 0;
                        if (unsafe.compareAndSwapInt(this, j, nextIndex, nextBound)) {
                            bound = nextBound;
                            i = nextIndex - 1;
                            advance = false;
                        }
                    }
                }
            } else if (i < 0 || i >= n || i + n >= nextn) {
                if (finishing) {
                    this.nextTable = null;
                    this.table = nextTab;
                    this.sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                Unsafe unsafe2 = U;
                long j2 = SIZECTL;
                int sc = this.sizeCtl;
                if (!unsafe2.compareAndSwapInt(this, j2, sc, sc - 1)) {
                    continue;
                } else {
                    if (sc - 2 != (resizeStamp(n) << 16)) {
                        return;
                    }
                    advance = true;
                    finishing = true;
                    i = n;
                }
            } else {
                Node<K, V> f = tabAt(tab, i);
                if (f == null) {
                    advance = casTabAt(tab, i, null, fwd);
                } else {
                    int fh = f.hash;
                    if (fh == -1) {
                        advance = true;
                    } else {
                        synchronized (f) {
                            if (tabAt(tab, i) == f) {
                                if (fh >= 0) {
                                    int runBit = fh & n;
                                    Node<K, V> lastRun = f;
                                    for (Node<K, V> p = f.next; p != null; p = p.next) {
                                        int b = p.hash & n;
                                        if (b != runBit) {
                                            runBit = b;
                                            lastRun = p;
                                        }
                                    }
                                    if (runBit == 0) {
                                        ln2 = lastRun;
                                        node = null;
                                    } else {
                                        node = lastRun;
                                        ln2 = null;
                                    }
                                    Node<K, V> p2 = f;
                                    Node<K, V> hn3 = node;
                                    Node<K, V> ln4 = ln2;
                                    while (p2 != lastRun) {
                                        int ph = p2.hash;
                                        K pk = p2.key;
                                        V pv = p2.val;
                                        if ((ph & n) == 0) {
                                            ln3 = new Node<>(ph, pk, pv, ln4);
                                            hn2 = hn3;
                                        } else {
                                            hn2 = new Node<>(ph, pk, pv, hn3);
                                            ln3 = ln4;
                                        }
                                        p2 = p2.next;
                                        hn3 = hn2;
                                        ln4 = ln3;
                                    }
                                    setTabAt(nextTab, i, ln4);
                                    setTabAt(nextTab, i + n, hn3);
                                    setTabAt(tab, i, fwd);
                                    advance = true;
                                } else if (f instanceof TreeBin) {
                                    TreeBin<K, V> t = (TreeBin) f;
                                    TreeNode<K, V> lo = null;
                                    TreeNode<K, V> loTail = null;
                                    TreeNode<K, V> hi = null;
                                    TreeNode<K, V> hiTail = null;
                                    int lc = 0;
                                    int hc = 0;
                                    for (Node<K, V> e = t.first; e != null; e = e.next) {
                                        int h = e.hash;
                                        TreeNode<K, V> p3 = new TreeNode<>(h, e.key, e.val, null, null);
                                        if ((h & n) == 0) {
                                            p3.prev = loTail;
                                            if (loTail == null) {
                                                lo = p3;
                                            } else {
                                                loTail.next = p3;
                                            }
                                            loTail = p3;
                                            lc++;
                                        } else {
                                            p3.prev = hiTail;
                                            if (hiTail == null) {
                                                hi = p3;
                                            } else {
                                                hiTail.next = p3;
                                            }
                                            hiTail = p3;
                                            hc++;
                                        }
                                    }
                                    if (lc <= 6) {
                                        ln = untreeify(lo);
                                    } else {
                                        ln = hc != 0 ? new TreeBin<>(lo) : t;
                                    }
                                    if (hc <= 6) {
                                        hn = untreeify(hi);
                                    } else {
                                        hn = lc != 0 ? new TreeBin<>(hi) : t;
                                    }
                                    setTabAt(nextTab, i, ln);
                                    setTabAt(nextTab, i + n, hn);
                                    setTabAt(tab, i, fwd);
                                    advance = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static final class CounterCell {
        volatile long value;

        CounterCell(long x) {
            this.value = x;
        }
    }

    final long sumCount() {
        CounterCell[] as = this.counterCells;
        long sum = this.baseCount;
        if (as != null) {
            for (CounterCell a : as) {
                if (a != null) {
                    sum += a.value;
                }
            }
        }
        return sum;
    }

    private final void fullAddCount(long x, boolean wasUncontended) {
        int n;
        int m;
        int h = ThreadLocalRandom.getProbe();
        if (h == 0) {
            ThreadLocalRandom.localInit();
            h = ThreadLocalRandom.getProbe();
            wasUncontended = true;
        }
        boolean collide = false;
        while (true) {
            CounterCell[] as = this.counterCells;
            if (as != null && (n = as.length) > 0) {
                CounterCell a = as[(n - 1) & h];
                if (a == null) {
                    if (this.cellsBusy == 0) {
                        CounterCell r = new CounterCell(x);
                        if (this.cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {
                                CounterCell[] rs = this.counterCells;
                                if (rs != null && (m = rs.length) > 0) {
                                    int j = (m - 1) & h;
                                    if (rs[j] == null) {
                                        rs[j] = r;
                                        created = true;
                                    }
                                }
                                if (created) {
                                    return;
                                }
                            } finally {
                            }
                        }
                    }
                    collide = false;
                    h = ThreadLocalRandom.advanceProbe(h);
                } else {
                    if (!wasUncontended) {
                        wasUncontended = true;
                    } else {
                        Unsafe unsafe = U;
                        long j2 = CELLVALUE;
                        long v = a.value;
                        if (unsafe.compareAndSwapLong(a, j2, v, v + x)) {
                            return;
                        }
                        if (this.counterCells != as || n >= NCPU) {
                            collide = false;
                        } else if (!collide) {
                            collide = true;
                        } else if (this.cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            try {
                                if (this.counterCells == as) {
                                    CounterCell[] rs2 = new CounterCell[n << 1];
                                    for (int i = 0; i < n; i++) {
                                        rs2[i] = as[i];
                                    }
                                    this.counterCells = rs2;
                                }
                                this.cellsBusy = 0;
                                collide = false;
                            } finally {
                            }
                        }
                    }
                    h = ThreadLocalRandom.advanceProbe(h);
                }
            } else if (this.cellsBusy == 0 && this.counterCells == as && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                boolean init = false;
                try {
                    if (this.counterCells == as) {
                        CounterCell[] rs3 = new CounterCell[2];
                        rs3[h & 1] = new CounterCell(x);
                        this.counterCells = rs3;
                        init = true;
                    }
                    if (init) {
                        return;
                    }
                } finally {
                }
            } else {
                Unsafe unsafe2 = U;
                long j3 = BASECOUNT;
                long v2 = this.baseCount;
                if (unsafe2.compareAndSwapLong(this, j3, v2, v2 + x)) {
                    return;
                }
            }
        }
    }

    private final void treeifyBin(Node<K, V>[] tab, int index) {
        if (tab == null) {
            return;
        }
        int n = tab.length;
        if (n < 64) {
            tryPresize(n << 1);
            return;
        }
        Node<K, V> b = tabAt(tab, index);
        if (b == null || b.hash < 0) {
            return;
        }
        synchronized (b) {
            if (tabAt(tab, index) == b) {
                TreeNode<K, V> hd = null;
                TreeNode<K, V> tl = null;
                for (Node<K, V> e = b; e != null; e = e.next) {
                    TreeNode<K, V> p = new TreeNode<>(e.hash, e.key, e.val, null, null);
                    p.prev = tl;
                    if (tl == null) {
                        hd = p;
                    } else {
                        tl.next = p;
                    }
                    tl = p;
                }
                setTabAt(tab, index, new TreeBin(hd));
            }
        }
    }

    static <K, V> Node<K, V> untreeify(Node<K, V> b) {
        Node<K, V> hd = null;
        Node<K, V> tl = null;
        for (Node<K, V> q = b; q != null; q = q.next) {
            Node<K, V> p = new Node<>(q.hash, q.key, q.val, null);
            if (tl == null) {
                hd = p;
            } else {
                tl.next = p;
            }
            tl = p;
        }
        return hd;
    }

    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> left;
        TreeNode<K, V> parent;
        TreeNode<K, V> prev;
        boolean red;
        TreeNode<K, V> right;

        TreeNode(int hash, K key, V val, Node<K, V> next, TreeNode<K, V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        @Override
        Node<K, V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        final TreeNode<K, V> findTreeNode(int h, Object k, Class<?> kc) {
            int dir;
            if (k != null) {
                TreeNode<K, V> p = this;
                do {
                    TreeNode<K, V> pl = p.left;
                    TreeNode<K, V> pr = p.right;
                    int ph = p.hash;
                    if (ph > h) {
                        p = pl;
                    } else if (ph < h) {
                        p = pr;
                    } else {
                        K pk = p.key;
                        if (pk == k || (pk != null && k.equals(pk))) {
                            return p;
                        }
                        if (pl == null) {
                            p = pr;
                        } else if (pr == null) {
                            p = pl;
                        } else if ((kc != null || (kc = ConcurrentHashMap.comparableClassFor(k)) != null) && (dir = ConcurrentHashMap.compareComparables(kc, k, pk)) != 0) {
                            p = dir < 0 ? pl : pr;
                        } else {
                            TreeNode<K, V> q = pr.findTreeNode(h, k, kc);
                            if (q != null) {
                                return q;
                            }
                            p = pl;
                        }
                    }
                } while (p != null);
            }
            return null;
        }
    }

    static final class TreeBin<K, V> extends Node<K, V> {

        static final boolean f126assertionsDisabled;
        private static final long LOCKSTATE;
        static final int READER = 4;
        private static final Unsafe U;
        static final int WAITER = 2;
        static final int WRITER = 1;
        volatile TreeNode<K, V> first;
        volatile int lockState;
        TreeNode<K, V> root;
        volatile Thread waiter;

        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null || (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0) {
                return System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1;
            }
            return d;
        }

        TreeBin(TreeNode<K, V> b) {
            int dir;
            TreeNode<K, V> xp;
            super(-2, null, null, null);
            this.first = b;
            TreeNode<K, V> r = null;
            TreeNode<K, V> x = b;
            while (x != null) {
                TreeNode<K, V> next = (TreeNode) x.next;
                x.right = null;
                x.left = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    TreeNode<K, V> p = r;
                    do {
                        K pk = p.key;
                        int ph = p.hash;
                        if (ph > h) {
                            dir = -1;
                        } else if (ph < h) {
                            dir = 1;
                        } else if ((kc == null && (kc = ConcurrentHashMap.comparableClassFor(k)) == null) || (dir = ConcurrentHashMap.compareComparables(kc, k, pk)) == 0) {
                            dir = tieBreakOrder(k, pk);
                        }
                        xp = p;
                        p = dir <= 0 ? p.left : p.right;
                    } while (p != null);
                    x.parent = xp;
                    if (dir <= 0) {
                        xp.left = x;
                    } else {
                        xp.right = x;
                    }
                    r = balanceInsertion(r, x);
                }
                x = next;
            }
            this.root = r;
            if (!f126assertionsDisabled && !checkInvariants(this.root)) {
                throw new AssertionError();
            }
        }

        private final void lockRoot() {
            if (U.compareAndSwapInt(this, LOCKSTATE, 0, 1)) {
                return;
            }
            contendedLock();
        }

        private final void unlockRoot() {
            this.lockState = 0;
        }

        private final void contendedLock() {
            boolean waiting = false;
            while (true) {
                int s = this.lockState;
                if ((s & ConcurrentHashMap.RESERVED) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, 1)) {
                        break;
                    }
                } else if ((s & 2) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | 2)) {
                        waiting = true;
                        this.waiter = Thread.currentThread();
                    }
                } else if (waiting) {
                    LockSupport.park(this);
                }
            }
            if (waiting) {
                this.waiter = null;
            }
        }

        @Override
        final Node<K, V> find(int h, Object k) {
            K ek;
            Thread w;
            if (k == null) {
                return null;
            }
            Node<K, V> e = this.first;
            while (e != null) {
                int s = this.lockState;
                if ((s & 3) != 0) {
                    if (e.hash == h && ((ek = e.key) == k || (ek != null && k.equals(ek)))) {
                        return e;
                    }
                    e = e.next;
                } else if (U.compareAndSwapInt(this, LOCKSTATE, s, s + 4)) {
                    try {
                        TreeNode<K, V> r = this.root;
                        return r == null ? null : r.findTreeNode(h, k, null);
                    } finally {
                        if (U.getAndAddInt(this, LOCKSTATE, -4) == 6 && (w = this.waiter) != null) {
                            LockSupport.unpark(w);
                        }
                    }
                }
            }
            return null;
        }

        final TreeNode<K, V> putTreeVal(int h, K k, V v) {
            int dir;
            TreeNode<K, V> ch;
            TreeNode<K, V> q;
            Class<?> kc = null;
            boolean searched = false;
            TreeNode<K, V> p = this.root;
            while (true) {
                if (p == null) {
                    TreeNode<K, V> treeNode = new TreeNode<>(h, k, v, null, null);
                    this.root = treeNode;
                    this.first = treeNode;
                    break;
                }
                int ph = p.hash;
                if (ph > h) {
                    dir = -1;
                } else if (ph < h) {
                    dir = 1;
                } else {
                    K pk = p.key;
                    if (pk == k || (pk != null && k.equals(pk))) {
                        break;
                    }
                    if ((kc == null && (kc = ConcurrentHashMap.comparableClassFor(k)) == null) || (dir = ConcurrentHashMap.compareComparables(kc, k, pk)) == 0) {
                        if (!searched) {
                            searched = true;
                            TreeNode<K, V> ch2 = p.left;
                            if ((ch2 != null && (q = ch2.findTreeNode(h, k, kc)) != null) || ((ch = p.right) != null && (q = ch.findTreeNode(h, k, kc)) != null)) {
                                break;
                            }
                        }
                        dir = tieBreakOrder(k, pk);
                    }
                }
                TreeNode<K, V> xp = p;
                p = dir <= 0 ? p.left : p.right;
                if (p == null) {
                    TreeNode<K, V> f = this.first;
                    TreeNode<K, V> x = new TreeNode<>(h, k, v, f, xp);
                    this.first = x;
                    if (f != null) {
                        f.prev = x;
                    }
                    if (dir <= 0) {
                        xp.left = x;
                    } else {
                        xp.right = x;
                    }
                    if (!xp.red) {
                        x.red = true;
                    } else {
                        lockRoot();
                        try {
                            this.root = balanceInsertion(this.root, x);
                        } finally {
                            unlockRoot();
                        }
                    }
                }
            }
            return q;
        }

        final boolean removeTreeNode(TreeNode<K, V> p) {
            TreeNode<K, V> rl;
            TreeNode<K, V> replacement;
            TreeNode<K, V> pp;
            TreeNode<K, V> next = (TreeNode) p.next;
            TreeNode<K, V> pred = p.prev;
            if (pred == null) {
                this.first = next;
            } else {
                pred.next = next;
            }
            if (next != null) {
                next.prev = pred;
            }
            if (this.first == null) {
                this.root = null;
                return true;
            }
            TreeNode<K, V> r = this.root;
            if (r == null || r.right == null || (rl = r.left) == null || rl.left == null) {
                return true;
            }
            lockRoot();
            try {
                TreeNode<K, V> pl = p.left;
                TreeNode<K, V> pr = p.right;
                if (pl != null && pr != null) {
                    TreeNode<K, V> s = pr;
                    while (true) {
                        TreeNode<K, V> sl = s.left;
                        if (sl == null) {
                            break;
                        }
                        s = sl;
                    }
                    boolean c = s.red;
                    s.red = p.red;
                    p.red = c;
                    TreeNode<K, V> sr = s.right;
                    TreeNode<K, V> pp2 = p.parent;
                    if (s == pr) {
                        p.parent = s;
                        s.right = p;
                    } else {
                        TreeNode<K, V> sp = s.parent;
                        p.parent = sp;
                        if (sp != null) {
                            if (s == sp.left) {
                                sp.left = p;
                            } else {
                                sp.right = p;
                            }
                        }
                        s.right = pr;
                        if (pr != null) {
                            pr.parent = s;
                        }
                    }
                    p.left = null;
                    p.right = sr;
                    if (sr != null) {
                        sr.parent = p;
                    }
                    s.left = pl;
                    if (pl != null) {
                        pl.parent = s;
                    }
                    s.parent = pp2;
                    if (pp2 == null) {
                        r = s;
                    } else if (p == pp2.left) {
                        pp2.left = s;
                    } else {
                        pp2.right = s;
                    }
                    if (sr != null) {
                        replacement = sr;
                    } else {
                        replacement = p;
                    }
                } else if (pl != null) {
                    replacement = pl;
                } else if (pr != null) {
                    replacement = pr;
                } else {
                    replacement = p;
                }
                if (replacement != p) {
                    TreeNode<K, V> pp3 = p.parent;
                    replacement.parent = pp3;
                    if (pp3 == null) {
                        r = replacement;
                    } else if (p == pp3.left) {
                        pp3.left = replacement;
                    } else {
                        pp3.right = replacement;
                    }
                    p.parent = null;
                    p.right = null;
                    p.left = null;
                }
                this.root = p.red ? r : balanceDeletion(r, replacement);
                if (p == replacement && (pp = p.parent) != null) {
                    if (p == pp.left) {
                        pp.left = null;
                    } else if (p == pp.right) {
                        pp.right = null;
                    }
                    p.parent = null;
                }
                unlockRoot();
                if (f126assertionsDisabled || checkInvariants(this.root)) {
                    return false;
                }
                throw new AssertionError();
            } catch (Throwable th) {
                unlockRoot();
                throw th;
            }
        }

        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> r;
            if (p != null && (r = p.right) != null) {
                TreeNode<K, V> rl = r.left;
                p.right = rl;
                if (rl != null) {
                    rl.parent = p;
                }
                TreeNode<K, V> pp = p.parent;
                r.parent = pp;
                if (pp == null) {
                    root = r;
                    r.red = false;
                } else if (pp.left == p) {
                    pp.left = r;
                } else {
                    pp.right = r;
                }
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> l;
            if (p != null && (l = p.left) != null) {
                TreeNode<K, V> lr = l.right;
                p.left = lr;
                if (lr != null) {
                    lr.parent = p;
                }
                TreeNode<K, V> pp = p.parent;
                l.parent = pp;
                if (pp == null) {
                    root = l;
                    l.red = false;
                } else if (pp.right == p) {
                    pp.right = l;
                } else {
                    pp.left = l;
                }
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> x) {
            TreeNode<K, V> xpp;
            x.red = true;
            while (true) {
                TreeNode<K, V> xp = x.parent;
                if (xp == null) {
                    x.red = false;
                    return x;
                }
                if (!xp.red || (xpp = xp.parent) == null) {
                    break;
                }
                TreeNode<K, V> xppl = xpp.left;
                if (xp == xppl) {
                    TreeNode<K, V> xppr = xpp.right;
                    if (xppr != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            x = xp;
                            root = rotateLeft(root, xp);
                            xp = xp.parent;
                            xpp = xp == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else if (xppl != null && xppl.red) {
                    xppl.red = false;
                    xp.red = false;
                    xpp.red = true;
                    x = xpp;
                } else {
                    if (x == xp.left) {
                        x = xp;
                        root = rotateRight(root, xp);
                        xp = xp.parent;
                        xpp = xp == null ? null : xp.parent;
                    }
                    if (xp != null) {
                        xp.red = false;
                        if (xpp != null) {
                            xpp.red = true;
                            root = rotateLeft(root, xpp);
                        }
                    }
                }
            }
            return root;
        }

        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> x) {
            while (x != null && x != root) {
                TreeNode<K, V> xp = x.parent;
                if (xp == null) {
                    x.red = false;
                    return x;
                }
                if (x.red) {
                    x.red = false;
                    return root;
                }
                TreeNode<K, V> xpl = xp.left;
                if (xpl == x) {
                    TreeNode<K, V> xpr = xp.right;
                    if (xpr != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xp = x.parent;
                        xpr = xp == null ? null : xp.right;
                    }
                    if (xpr == null) {
                        x = xp;
                    } else {
                        TreeNode<K, V> sl = xpr.left;
                        TreeNode<K, V> sr = xpr.right;
                        if ((sr == null || !sr.red) && (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null) {
                                    sl.red = false;
                                }
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xp = x.parent;
                                xpr = xp == null ? null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = xp == null ? false : xp.red;
                                TreeNode<K, V> sr2 = xpr.right;
                                if (sr2 != null) {
                                    sr2.red = false;
                                }
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else {
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xp = x.parent;
                        xpl = xp == null ? null : xp.left;
                    }
                    if (xpl == null) {
                        x = xp;
                    } else {
                        TreeNode<K, V> sl2 = xpl.left;
                        TreeNode<K, V> sr3 = xpl.right;
                        if ((sl2 == null || !sl2.red) && (sr3 == null || !sr3.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl2 == null || !sl2.red) {
                                if (sr3 != null) {
                                    sr3.red = false;
                                }
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xp = x.parent;
                                xpl = xp == null ? null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = xp == null ? false : xp.red;
                                TreeNode<K, V> sl3 = xpl.left;
                                if (sl3 != null) {
                                    sl3.red = false;
                                }
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
            return root;
        }

        static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
            TreeNode<K, V> tp = t.parent;
            TreeNode<K, V> tl = t.left;
            TreeNode<K, V> tr = t.right;
            TreeNode<K, V> tb = t.prev;
            TreeNode<K, V> tn = (TreeNode) t.next;
            if (tb != null && tb.next != t) {
                return false;
            }
            if (tn != null && tn.prev != t) {
                return false;
            }
            if (tp != null && t != tp.left && t != tp.right) {
                return false;
            }
            if (tl != null && (tl.parent != t || tl.hash > t.hash)) {
                return false;
            }
            if (tr != null && (tr.parent != t || tr.hash < t.hash)) {
                return false;
            }
            if (t.red && tl != null && tl.red && tr != null && tr.red) {
                return false;
            }
            if (tl == null || checkInvariants(tl)) {
                return tr == null || checkInvariants(tr);
            }
            return false;
        }

        static {
            f126assertionsDisabled = !TreeBin.class.desiredAssertionStatus();
            U = Unsafe.getUnsafe();
            try {
                LOCKSTATE = U.objectFieldOffset(TreeBin.class.getDeclaredField("lockState"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static final class TableStack<K, V> {
        int index;
        int length;
        TableStack<K, V> next;
        Node<K, V>[] tab;

        TableStack() {
        }
    }

    static class Traverser<K, V> {
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int index;
        Node<K, V> next = null;
        TableStack<K, V> spare;
        TableStack<K, V> stack;
        Node<K, V>[] tab;

        Traverser(Node<K, V>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.index = index;
            this.baseIndex = index;
            this.baseLimit = limit;
        }

        final Node<K, V> advance() {
            Node<K, V>[] t;
            int n;
            int i;
            Node<K, V> e = this.next;
            if (e != null) {
                e = e.next;
            }
            while (e == null) {
                if (this.baseIndex >= this.baseLimit || (t = this.tab) == null || (n = t.length) <= (i = this.index) || i < 0) {
                    this.next = null;
                    return null;
                }
                e = ConcurrentHashMap.tabAt(t, i);
                if (e != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        this.tab = ((ForwardingNode) e).nextTable;
                        e = null;
                        pushState(t, i, n);
                    } else {
                        e = e instanceof TreeBin ? ((TreeBin) e).first : null;
                    }
                }
                if (this.stack != null) {
                    recoverState(n);
                } else {
                    int i2 = this.baseSize + i;
                    this.index = i2;
                    if (i2 >= n) {
                        int i3 = this.baseIndex + 1;
                        this.baseIndex = i3;
                        this.index = i3;
                    }
                }
            }
            this.next = e;
            return e;
        }

        private void pushState(Node<K, V>[] t, int i, int n) {
            TableStack<K, V> s = this.spare;
            if (s != null) {
                this.spare = s.next;
            } else {
                s = new TableStack<>();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = this.stack;
            this.stack = s;
        }

        private void recoverState(int n) {
            TableStack<K, V> s;
            while (true) {
                s = this.stack;
                if (s == null) {
                    break;
                }
                int i = this.index;
                int len = s.length;
                int i2 = i + len;
                this.index = i2;
                if (i2 < n) {
                    break;
                }
                n = len;
                this.index = s.index;
                this.tab = s.tab;
                s.tab = null;
                TableStack<K, V> next = s.next;
                s.next = this.spare;
                this.stack = next;
                this.spare = s;
            }
            if (s == null) {
                int i3 = this.index + this.baseSize;
                this.index = i3;
                if (i3 < n) {
                    return;
                }
                int i4 = this.baseIndex + 1;
                this.baseIndex = i4;
                this.index = i4;
            }
        }
    }

    static class BaseIterator<K, V> extends Traverser<K, V> {
        Node<K, V> lastReturned;
        final ConcurrentHashMap<K, V> map;

        BaseIterator(Node<K, V>[] tab, int size, int index, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, size, index, limit);
            this.map = map;
            advance();
        }

        public final boolean hasNext() {
            return this.next != null;
        }

        public final boolean hasMoreElements() {
            return this.next != null;
        }

        public final void remove() {
            Node<K, V> p = this.lastReturned;
            if (p == null) {
                throw new IllegalStateException();
            }
            this.lastReturned = null;
            this.map.replaceNode(p.key, null, null);
        }
    }

    static final class KeyIterator<K, V> extends BaseIterator<K, V> implements Iterator<K>, Enumeration<K> {
        KeyIterator(Node<K, V>[] tab, int index, int size, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        @Override
        public final K next() {
            Node<K, V> p = this.next;
            if (p == null) {
                throw new NoSuchElementException();
            }
            K k = p.key;
            this.lastReturned = p;
            advance();
            return k;
        }

        @Override
        public final K nextElement() {
            return next();
        }
    }

    static final class ValueIterator<K, V> extends BaseIterator<K, V> implements Iterator<V>, Enumeration<V> {
        ValueIterator(Node<K, V>[] tab, int index, int size, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        @Override
        public final V next() {
            Node<K, V> p = this.next;
            if (p == null) {
                throw new NoSuchElementException();
            }
            V v = p.val;
            this.lastReturned = p;
            advance();
            return v;
        }

        @Override
        public final V nextElement() {
            return next();
        }
    }

    static final class EntryIterator<K, V> extends BaseIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        EntryIterator(Node<K, V>[] tab, int index, int size, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        @Override
        public final Map.Entry<K, V> next() {
            Node<K, V> p = this.next;
            if (p == null) {
                throw new NoSuchElementException();
            }
            K k = p.key;
            V v = p.val;
            this.lastReturned = p;
            advance();
            return new MapEntry(k, v, this.map);
        }
    }

    static final class MapEntry<K, V> implements Map.Entry<K, V> {
        final K key;
        final ConcurrentHashMap<K, V> map;
        V val;

        MapEntry(K key, V val, ConcurrentHashMap<K, V> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.val;
        }

        @Override
        public int hashCode() {
            return this.key.hashCode() ^ this.val.hashCode();
        }

        public String toString() {
            return Helpers.mapEntryToString(this.key, this.val);
        }

        @Override
        public boolean equals(Object o) {
            Map.Entry<?, ?> e;
            Object k;
            Object v;
            if (!(o instanceof Map.Entry) || (k = (e = (Map.Entry) o).getKey()) == null || (v = e.getValue()) == null || !(k == this.key || k.equals(this.key))) {
                return false;
            }
            if (v != this.val) {
                return v.equals(this.val);
            }
            return true;
        }

        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new NullPointerException();
            }
            V v = this.val;
            this.val = value;
            this.map.put(this.key, value);
            return v;
        }
    }

    static final class KeySpliterator<K, V> extends Traverser<K, V> implements Spliterator<K> {
        long est;

        KeySpliterator(Node<K, V>[] tab, int size, int index, int limit, long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int f = this.baseLimit;
            int h = (i + f) >>> 1;
            if (h <= i) {
                return null;
            }
            Node<K, V>[] nodeArr = this.tab;
            int i2 = this.baseSize;
            this.baseLimit = h;
            long j = this.est >>> 1;
            this.est = j;
            return new KeySpliterator<>(nodeArr, i2, h, f, j);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    return;
                } else {
                    consumer.accept(nodeAdvance.key);
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V> nodeAdvance = advance();
            if (nodeAdvance == null) {
                return false;
            }
            consumer.accept(nodeAdvance.key);
            return true;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return 4353;
        }
    }

    static final class ValueSpliterator<K, V> extends Traverser<K, V> implements Spliterator<V> {
        long est;

        ValueSpliterator(Node<K, V>[] tab, int size, int index, int limit, long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int f = this.baseLimit;
            int h = (i + f) >>> 1;
            if (h <= i) {
                return null;
            }
            Node<K, V>[] nodeArr = this.tab;
            int i2 = this.baseSize;
            this.baseLimit = h;
            long j = this.est >>> 1;
            this.est = j;
            return new ValueSpliterator<>(nodeArr, i2, h, f, j);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    return;
                } else {
                    consumer.accept(nodeAdvance.val);
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V> nodeAdvance = advance();
            if (nodeAdvance == null) {
                return false;
            }
            consumer.accept(nodeAdvance.val);
            return true;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return Normalizer2Impl.Hangul.JAMO_L_BASE;
        }
    }

    static final class EntrySpliterator<K, V> extends Traverser<K, V> implements Spliterator<Map.Entry<K, V>> {
        long est;
        final ConcurrentHashMap<K, V> map;

        EntrySpliterator(Node<K, V>[] tab, int size, int index, int limit, long est, ConcurrentHashMap<K, V> map) {
            super(tab, size, index, limit);
            this.map = map;
            this.est = est;
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int f = this.baseLimit;
            int h = (i + f) >>> 1;
            if (h <= i) {
                return null;
            }
            Node<K, V>[] nodeArr = this.tab;
            int i2 = this.baseSize;
            this.baseLimit = h;
            long j = this.est >>> 1;
            this.est = j;
            return new EntrySpliterator<>(nodeArr, i2, h, f, j, this.map);
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            while (true) {
                Node<K, V> p = advance();
                if (p == null) {
                    return;
                } else {
                    action.accept(new MapEntry(p.key, p.val, this.map));
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Node<K, V> p = advance();
            if (p == null) {
                return false;
            }
            action.accept(new MapEntry(p.key, p.val, this.map));
            return true;
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public int characteristics() {
            return 4353;
        }
    }

    final int batchFor(long b) {
        if (b == Long.MAX_VALUE) {
            return 0;
        }
        long n = sumCount();
        if (n <= 1 || n < b) {
            return 0;
        }
        int sp = ForkJoinPool.getCommonPoolParallelism() << 2;
        if (b <= 0) {
            return sp;
        }
        long n2 = n / b;
        return n2 >= ((long) sp) ? sp : (int) n2;
    }

    public void forEach(long parallelismThreshold, BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        new ForEachMappingTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
    }

    public <U> void forEach(long parallelismThreshold, BiFunction<? super K, ? super V, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedMappingTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U search(long parallelismThreshold, BiFunction<? super K, ? super V, ? extends U> searchFunction) {
        if (searchFunction == null) {
            throw new NullPointerException();
        }
        return new SearchMappingsTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
    }

    public <U> U reduce(long parallelismThreshold, BiFunction<? super K, ? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
    }

    public double reduceToDouble(long parallelismThreshold, ToDoubleBiFunction<? super K, ? super V> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().doubleValue();
    }

    public long reduceToLong(long parallelismThreshold, ToLongBiFunction<? super K, ? super V> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().longValue();
    }

    public int reduceToInt(long parallelismThreshold, ToIntBiFunction<? super K, ? super V> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().intValue();
    }

    public void forEachKey(long parallelismThreshold, Consumer<? super K> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        new ForEachKeyTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
    }

    public <U> void forEachKey(long parallelismThreshold, Function<? super K, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedKeyTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U searchKeys(long parallelismThreshold, Function<? super K, ? extends U> searchFunction) {
        if (searchFunction == null) {
            throw new NullPointerException();
        }
        return new SearchKeysTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
    }

    public K reduceKeys(long parallelismThreshold, BiFunction<? super K, ? super K, ? extends K> reducer) {
        if (reducer == null) {
            throw new NullPointerException();
        }
        return new ReduceKeysTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, reducer).invoke();
    }

    public <U> U reduceKeys(long parallelismThreshold, Function<? super K, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
    }

    public double reduceKeysToDouble(long parallelismThreshold, ToDoubleFunction<? super K> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().doubleValue();
    }

    public long reduceKeysToLong(long parallelismThreshold, ToLongFunction<? super K> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().longValue();
    }

    public int reduceKeysToInt(long parallelismThreshold, ToIntFunction<? super K> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().intValue();
    }

    public void forEachValue(long parallelismThreshold, Consumer<? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        new ForEachValueTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
    }

    public <U> void forEachValue(long parallelismThreshold, Function<? super V, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedValueTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U searchValues(long parallelismThreshold, Function<? super V, ? extends U> searchFunction) {
        if (searchFunction == null) {
            throw new NullPointerException();
        }
        return new SearchValuesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
    }

    public V reduceValues(long parallelismThreshold, BiFunction<? super V, ? super V, ? extends V> reducer) {
        if (reducer == null) {
            throw new NullPointerException();
        }
        return new ReduceValuesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, reducer).invoke();
    }

    public <U> U reduceValues(long parallelismThreshold, Function<? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
    }

    public double reduceValuesToDouble(long parallelismThreshold, ToDoubleFunction<? super V> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().doubleValue();
    }

    public long reduceValuesToLong(long parallelismThreshold, ToLongFunction<? super V> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().longValue();
    }

    public int reduceValuesToInt(long parallelismThreshold, ToIntFunction<? super V> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().intValue();
    }

    public void forEachEntry(long parallelismThreshold, Consumer<? super Map.Entry<K, V>> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        new ForEachEntryTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
    }

    public <U> void forEachEntry(long parallelismThreshold, Function<Map.Entry<K, V>, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedEntryTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U searchEntries(long parallelismThreshold, Function<Map.Entry<K, V>, ? extends U> searchFunction) {
        if (searchFunction == null) {
            throw new NullPointerException();
        }
        return new SearchEntriesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
    }

    public Map.Entry<K, V> reduceEntries(long parallelismThreshold, BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> reducer) {
        if (reducer == null) {
            throw new NullPointerException();
        }
        return new ReduceEntriesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, reducer).invoke();
    }

    public <U> U reduceEntries(long parallelismThreshold, Function<Map.Entry<K, V>, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
    }

    public double reduceEntriesToDouble(long parallelismThreshold, ToDoubleFunction<Map.Entry<K, V>> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().doubleValue();
    }

    public long reduceEntriesToLong(long parallelismThreshold, ToLongFunction<Map.Entry<K, V>> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().longValue();
    }

    public int reduceEntriesToInt(long parallelismThreshold, ToIntFunction<Map.Entry<K, V>> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer == null || reducer == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke().intValue();
    }

    static abstract class CollectionView<K, V, E> implements Collection<E>, Serializable {
        private static final String OOME_MSG = "Required array size too large";
        private static final long serialVersionUID = 7249069246763182397L;
        final ConcurrentHashMap<K, V> map;

        @Override
        public abstract boolean contains(Object obj);

        @Override
        public abstract Iterator<E> iterator();

        @Override
        public abstract boolean remove(Object obj);

        CollectionView(ConcurrentHashMap<K, V> map) {
            this.map = map;
        }

        public ConcurrentHashMap<K, V> getMap() {
            return this.map;
        }

        @Override
        public final void clear() throws Throwable {
            this.map.clear();
        }

        @Override
        public final int size() {
            return this.map.size();
        }

        @Override
        public final boolean isEmpty() {
            return this.map.isEmpty();
        }

        @Override
        public final Object[] toArray() {
            long sz = this.map.mappingCount();
            if (sz > 2147483639) {
                throw new OutOfMemoryError(OOME_MSG);
            }
            int n = (int) sz;
            Object[] r = new Object[n];
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= ConcurrentHashMap.MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(OOME_MSG);
                    }
                    if (n >= 1073741819) {
                        n = ConcurrentHashMap.MAX_ARRAY_SIZE;
                    } else {
                        n += (n >>> 1) + 1;
                    }
                    r = Arrays.copyOf(r, n);
                }
                r[i] = e;
                i++;
            }
            return i == n ? r : Arrays.copyOf(r, i);
        }

        @Override
        public final <T> T[] toArray(T[] tArr) {
            long jMappingCount = this.map.mappingCount();
            if (jMappingCount > 2147483639) {
                throw new OutOfMemoryError(OOME_MSG);
            }
            int i = (int) jMappingCount;
            Object[] objArr = tArr.length >= i ? tArr : (Object[]) Array.newInstance(tArr.getClass().getComponentType(), i);
            int length = objArr.length;
            int i2 = 0;
            for (E e : this) {
                if (i2 == length) {
                    if (length >= ConcurrentHashMap.MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(OOME_MSG);
                    }
                    if (length >= 1073741819) {
                        length = ConcurrentHashMap.MAX_ARRAY_SIZE;
                    } else {
                        length += (length >>> 1) + 1;
                    }
                    objArr = (T[]) Arrays.copyOf(objArr, length);
                }
                objArr[i2] = e;
                i2++;
            }
            if (tArr != objArr || i2 >= length) {
                return i2 == length ? (T[]) objArr : (T[]) Arrays.copyOf(objArr, i2);
            }
            objArr[i2] = null;
            return (T[]) objArr;
        }

        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            Iterator<E> it = iterator();
            if (it.hasNext()) {
                while (true) {
                    Object e = it.next();
                    if (e == this) {
                        e = "(this Collection)";
                    }
                    sb.append(e);
                    if (!it.hasNext()) {
                        break;
                    }
                    sb.append(',').append(' ');
                }
            }
            return sb.append(']').toString();
        }

        @Override
        public final boolean containsAll(Collection<?> c) {
            if (c != this) {
                for (Object e : c) {
                    if (e == null || !contains(e)) {
                        return false;
                    }
                }
                return true;
            }
            return true;
        }

        @Override
        public final boolean removeAll(Collection<?> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            boolean modified = false;
            Iterator<E> it = iterator();
            while (it.hasNext()) {
                if (c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public final boolean retainAll(Collection<?> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            boolean modified = false;
            Iterator<E> it = iterator();
            while (it.hasNext()) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }
    }

    public static class KeySetView<K, V> extends CollectionView<K, V, K> implements Set<K>, Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;

        @Override
        public ConcurrentHashMap getMap() {
            return super.getMap();
        }

        KeySetView(ConcurrentHashMap<K, V> map, V value) {
            super(map);
            this.value = value;
        }

        public V getMappedValue() {
            return this.value;
        }

        @Override
        public boolean contains(Object o) {
            return this.map.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return this.map.remove(o) != null;
        }

        @Override
        public Iterator<K> iterator() {
            ConcurrentHashMap<K, V> m = this.map;
            Node<K, V>[] t = m.table;
            int f = t == null ? 0 : t.length;
            return new KeyIterator(t, f, 0, f, m);
        }

        @Override
        public boolean add(K e) {
            V v = this.value;
            if (v == null) {
                throw new UnsupportedOperationException();
            }
            return this.map.putVal(e, v, true) == null;
        }

        @Override
        public boolean addAll(Collection<? extends K> collection) {
            boolean z = false;
            V v = this.value;
            if (v == null) {
                throw new UnsupportedOperationException();
            }
            Iterator<T> it = collection.iterator();
            while (it.hasNext()) {
                if (this.map.putVal((K) it.next(), v, true) == null) {
                    z = true;
                }
            }
            return z;
        }

        @Override
        public int hashCode() {
            int h = 0;
            for (K e : this) {
                h += e.hashCode();
            }
            return h;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Set)) {
                return false;
            }
            Set<?> c = (Set) o;
            if (c == this) {
                return true;
            }
            if (containsAll(c)) {
                return c.containsAll(this);
            }
            return false;
        }

        @Override
        public Spliterator<K> spliterator() {
            ConcurrentHashMap<K, V> m = this.map;
            long n = m.sumCount();
            Node<K, V>[] t = m.table;
            int f = t == null ? 0 : t.length;
            return new KeySpliterator(t, f, 0, f, n >= 0 ? n : 0L);
        }

        @Override
        public void forEach(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr == null) {
                return;
            }
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    return;
                } else {
                    consumer.accept(nodeAdvance.key);
                }
            }
        }
    }

    static final class ValuesView<K, V> extends CollectionView<K, V, V> implements Collection<V>, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        ValuesView(ConcurrentHashMap<K, V> map) {
            super(map);
        }

        @Override
        public final boolean contains(Object o) {
            return this.map.containsValue(o);
        }

        @Override
        public final boolean remove(Object o) {
            if (o != null) {
                Iterator<V> it = iterator();
                while (it.hasNext()) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        @Override
        public final Iterator<V> iterator() {
            ConcurrentHashMap<K, V> m = this.map;
            Node<K, V>[] t = m.table;
            int f = t == null ? 0 : t.length;
            return new ValueIterator(t, f, 0, f, m);
        }

        @Override
        public final boolean add(V e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean addAll(Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super V> filter) {
            return this.map.removeValueIf(filter);
        }

        @Override
        public Spliterator<V> spliterator() {
            ConcurrentHashMap<K, V> m = this.map;
            long n = m.sumCount();
            Node<K, V>[] t = m.table;
            int f = t == null ? 0 : t.length;
            return new ValueSpliterator(t, f, 0, f, n >= 0 ? n : 0L);
        }

        @Override
        public void forEach(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr == null) {
                return;
            }
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    return;
                } else {
                    consumer.accept(nodeAdvance.val);
                }
            }
        }
    }

    static final class EntrySetView<K, V> extends CollectionView<K, V, Map.Entry<K, V>> implements Set<Map.Entry<K, V>>, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        EntrySetView(ConcurrentHashMap<K, V> map) {
            super(map);
        }

        @Override
        public boolean contains(Object o) {
            Map.Entry<?, ?> e;
            Object k;
            Object r;
            Object v;
            if (!(o instanceof Map.Entry) || (k = (e = (Map.Entry) o).getKey()) == null || (r = this.map.get(k)) == null || (v = e.getValue()) == null) {
                return false;
            }
            if (v != r) {
                return v.equals(r);
            }
            return true;
        }

        @Override
        public boolean remove(Object o) {
            Map.Entry<?, ?> e;
            Object k;
            Object v;
            if (!(o instanceof Map.Entry) || (k = (e = (Map.Entry) o).getKey()) == null || (v = e.getValue()) == null) {
                return false;
            }
            return this.map.remove(k, v);
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            ConcurrentHashMap<K, V> m = this.map;
            Node<K, V>[] t = m.table;
            int f = t == null ? 0 : t.length;
            return new EntryIterator(t, f, 0, f, m);
        }

        @Override
        public boolean add(Map.Entry<K, V> e) {
            return this.map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        @Override
        public boolean addAll(Collection<? extends Map.Entry<K, V>> c) {
            boolean added = false;
            for (Map.Entry<K, V> e : c) {
                if (add((Map.Entry) e)) {
                    added = true;
                }
            }
            return added;
        }

        @Override
        public boolean removeIf(Predicate<? super Map.Entry<K, V>> filter) {
            return this.map.removeEntryIf(filter);
        }

        @Override
        public final int hashCode() {
            int h = 0;
            Node<K, V>[] t = this.map.table;
            if (t != null) {
                Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
                while (true) {
                    Node<K, V> p = it.advance();
                    if (p == null) {
                        break;
                    }
                    h += p.hashCode();
                }
            }
            return h;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Set)) {
                return false;
            }
            Set<?> c = (Set) o;
            if (c == this) {
                return true;
            }
            if (containsAll(c)) {
                return c.containsAll(this);
            }
            return false;
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            ConcurrentHashMap<K, V> m = this.map;
            long n = m.sumCount();
            Node<K, V>[] t = m.table;
            int f = t == null ? 0 : t.length;
            return new EntrySpliterator(t, f, 0, f, n >= 0 ? n : 0L, m);
        }

        @Override
        public void forEach(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] t = this.map.table;
            if (t == null) {
                return;
            }
            Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
            while (true) {
                Node<K, V> p = it.advance();
                if (p == null) {
                    return;
                } else {
                    action.accept(new MapEntry(p.key, p.val, this.map));
                }
            }
        }
    }

    static abstract class BulkTask<K, V, R> extends CountedCompleter<R> {
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int batch;
        int index;
        Node<K, V> next;
        TableStack<K, V> spare;
        TableStack<K, V> stack;
        Node<K, V>[] tab;

        BulkTask(BulkTask<K, V, ?> par, int b, int i, int f, Node<K, V>[] t) {
            super(par);
            this.batch = b;
            this.baseIndex = i;
            this.index = i;
            this.tab = t;
            if (t == null) {
                this.baseLimit = 0;
                this.baseSize = 0;
            } else if (par == null) {
                int length = t.length;
                this.baseLimit = length;
                this.baseSize = length;
            } else {
                this.baseLimit = f;
                this.baseSize = par.baseSize;
            }
        }

        final Node<K, V> advance() {
            Node<K, V>[] nodeArr;
            int length;
            int i;
            Node<K, V> node = this.next;
            TreeNode<K, V> treeNode = node;
            if (node != null) {
                treeNode = node.next;
            }
            while (treeNode == 0) {
                if (this.baseIndex >= this.baseLimit || (nodeArr = this.tab) == null || (length = nodeArr.length) <= (i = this.index) || i < 0) {
                    this.next = null;
                    return null;
                }
                treeNode = (TreeNode<K, V>) ConcurrentHashMap.tabAt(nodeArr, i);
                if (treeNode != 0 && treeNode.hash < 0) {
                    if (treeNode instanceof ForwardingNode) {
                        this.tab = treeNode.nextTable;
                        treeNode = (Node<K, V>) null;
                        pushState(nodeArr, i, length);
                    } else {
                        treeNode = treeNode instanceof TreeBin ? treeNode.first : (Node<K, V>) null;
                    }
                }
                if (this.stack != null) {
                    recoverState(length);
                } else {
                    int i2 = this.baseSize + i;
                    this.index = i2;
                    if (i2 >= length) {
                        int i3 = this.baseIndex + 1;
                        this.baseIndex = i3;
                        this.index = i3;
                    }
                }
            }
            this.next = (Node<K, V>) treeNode;
            return (Node<K, V>) treeNode;
        }

        private void pushState(Node<K, V>[] t, int i, int n) {
            TableStack<K, V> s = this.spare;
            if (s != null) {
                this.spare = s.next;
            } else {
                s = new TableStack<>();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = this.stack;
            this.stack = s;
        }

        private void recoverState(int n) {
            TableStack<K, V> s;
            while (true) {
                s = this.stack;
                if (s == null) {
                    break;
                }
                int i = this.index;
                int len = s.length;
                int i2 = i + len;
                this.index = i2;
                if (i2 < n) {
                    break;
                }
                n = len;
                this.index = s.index;
                this.tab = s.tab;
                s.tab = null;
                TableStack<K, V> next = s.next;
                s.next = this.spare;
                this.stack = next;
                this.spare = s;
            }
            if (s == null) {
                int i3 = this.index + this.baseSize;
                this.index = i3;
                if (i3 < n) {
                    return;
                }
                int i4 = this.baseIndex + 1;
                this.baseIndex = i4;
                this.index = i4;
            }
        }
    }

    static final class ForEachKeyTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super K> action;

        ForEachKeyTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Consumer<? super K> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        @Override
        public final void compute() {
            Consumer<? super K> consumer = this.action;
            if (consumer == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new ForEachKeyTask(this, i4, i3, i2, this.tab, consumer).fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    consumer.accept(nodeAdvance.key);
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class ForEachValueTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super V> action;

        ForEachValueTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Consumer<? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        @Override
        public final void compute() {
            Consumer<? super V> consumer = this.action;
            if (consumer == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new ForEachValueTask(this, i4, i3, i2, this.tab, consumer).fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    consumer.accept(nodeAdvance.val);
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class ForEachEntryTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super Map.Entry<K, V>> action;

        ForEachEntryTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Consumer<? super Map.Entry<K, V>> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        @Override
        public final void compute() {
            Consumer<? super Map.Entry<K, V>> action = this.action;
            if (action == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int f = this.baseLimit;
                int h = (f + i) >>> 1;
                if (h <= i) {
                    break;
                }
                addToPendingCount(1);
                int i2 = this.batch >>> 1;
                this.batch = i2;
                this.baseLimit = h;
                new ForEachEntryTask(this, i2, h, f, this.tab, action).fork();
            }
            while (true) {
                Node<K, V> p = advance();
                if (p != null) {
                    action.accept(p);
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class ForEachMappingTask<K, V> extends BulkTask<K, V, Void> {
        final BiConsumer<? super K, ? super V> action;

        ForEachMappingTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, BiConsumer<? super K, ? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        @Override
        public final void compute() {
            BiConsumer<? super K, ? super V> biConsumer = this.action;
            if (biConsumer == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new ForEachMappingTask(this, i4, i3, i2, this.tab, biConsumer).fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    biConsumer.accept(nodeAdvance.key, nodeAdvance.val);
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class ForEachTransformedKeyTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<? super K, ? extends U> transformer;

        ForEachTransformedKeyTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super K, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            Function<? super K, ? extends U> function = this.transformer;
            if (function == null || (consumer = this.action) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new ForEachTransformedKeyTask(this, i4, i3, i2, this.tab, function, consumer).fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    U uApply = function.apply(nodeAdvance.key);
                    if (uApply != null) {
                        consumer.accept(uApply);
                    }
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class ForEachTransformedValueTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<? super V, ? extends U> transformer;

        ForEachTransformedValueTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super V, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            Function<? super V, ? extends U> function = this.transformer;
            if (function == null || (consumer = this.action) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new ForEachTransformedValueTask(this, i4, i3, i2, this.tab, function, consumer).fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    U uApply = function.apply(nodeAdvance.val);
                    if (uApply != null) {
                        consumer.accept(uApply);
                    }
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class ForEachTransformedEntryTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<Map.Entry<K, V>, ? extends U> transformer;

        ForEachTransformedEntryTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<Map.Entry<K, V>, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            Function<Map.Entry<K, V>, ? extends U> function = this.transformer;
            if (function == null || (consumer = this.action) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new ForEachTransformedEntryTask(this, i4, i3, i2, this.tab, function, consumer).fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    U uApply = function.apply(nodeAdvance);
                    if (uApply != null) {
                        consumer.accept(uApply);
                    }
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class ForEachTransformedMappingTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final BiFunction<? super K, ? super V, ? extends U> transformer;

        ForEachTransformedMappingTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, BiFunction<? super K, ? super V, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            BiFunction<? super K, ? super V, ? extends U> biFunction = this.transformer;
            if (biFunction == null || (consumer = this.action) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new ForEachTransformedMappingTask(this, i4, i3, i2, this.tab, biFunction, consumer).fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    U uApply = biFunction.apply(nodeAdvance.key, nodeAdvance.val);
                    if (uApply != null) {
                        consumer.accept(uApply);
                    }
                } else {
                    propagateCompletion();
                    return;
                }
            }
        }
    }

    static final class SearchKeysTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<? super K, ? extends U> searchFunction;

        SearchKeysTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super K, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> atomicReference;
            Function<? super K, ? extends U> function = this.searchFunction;
            if (function == null || (atomicReference = this.result) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                if (atomicReference.get() != null) {
                    return;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new SearchKeysTask(this, i4, i3, i2, this.tab, function, atomicReference).fork();
            }
            while (atomicReference.get() == null) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    propagateCompletion();
                    return;
                }
                U uApply = function.apply(nodeAdvance.key);
                if (uApply != null) {
                    if (!atomicReference.compareAndSet(null, uApply)) {
                        return;
                    }
                    quietlyCompleteRoot();
                    return;
                }
            }
        }
    }

    static final class SearchValuesTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<? super V, ? extends U> searchFunction;

        SearchValuesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super V, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> atomicReference;
            Function<? super V, ? extends U> function = this.searchFunction;
            if (function == null || (atomicReference = this.result) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                if (atomicReference.get() != null) {
                    return;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new SearchValuesTask(this, i4, i3, i2, this.tab, function, atomicReference).fork();
            }
            while (atomicReference.get() == null) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    propagateCompletion();
                    return;
                }
                U uApply = function.apply(nodeAdvance.val);
                if (uApply != null) {
                    if (!atomicReference.compareAndSet(null, uApply)) {
                        return;
                    }
                    quietlyCompleteRoot();
                    return;
                }
            }
        }
    }

    static final class SearchEntriesTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<Map.Entry<K, V>, ? extends U> searchFunction;

        SearchEntriesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<Map.Entry<K, V>, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> result;
            Function<Map.Entry<K, V>, ? extends U> searchFunction = this.searchFunction;
            if (searchFunction == null || (result = this.result) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int f = this.baseLimit;
                int h = (f + i) >>> 1;
                if (h <= i) {
                    break;
                }
                if (result.get() != null) {
                    return;
                }
                addToPendingCount(1);
                int i2 = this.batch >>> 1;
                this.batch = i2;
                this.baseLimit = h;
                new SearchEntriesTask(this, i2, h, f, this.tab, searchFunction, result).fork();
            }
            while (result.get() == null) {
                Node<K, V> p = advance();
                if (p == null) {
                    propagateCompletion();
                    return;
                }
                U u = searchFunction.apply(p);
                if (u != null) {
                    if (result.compareAndSet(null, u)) {
                        quietlyCompleteRoot();
                        return;
                    }
                    return;
                }
            }
        }
    }

    static final class SearchMappingsTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final BiFunction<? super K, ? super V, ? extends U> searchFunction;

        SearchMappingsTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, BiFunction<? super K, ? super V, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> atomicReference;
            BiFunction<? super K, ? super V, ? extends U> biFunction = this.searchFunction;
            if (biFunction == null || (atomicReference = this.result) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                if (atomicReference.get() != null) {
                    return;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                new SearchMappingsTask(this, i4, i3, i2, this.tab, biFunction, atomicReference).fork();
            }
            while (atomicReference.get() == null) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    propagateCompletion();
                    return;
                }
                U uApply = biFunction.apply(nodeAdvance.key, nodeAdvance.val);
                if (uApply != null) {
                    if (!atomicReference.compareAndSet(null, uApply)) {
                        return;
                    }
                    quietlyCompleteRoot();
                    return;
                }
            }
        }
    }

    static final class ReduceKeysTask<K, V> extends BulkTask<K, V, K> {
        ReduceKeysTask<K, V> nextRight;
        final BiFunction<? super K, ? super K, ? extends K> reducer;
        K result;
        ReduceKeysTask<K, V> rights;

        ReduceKeysTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, ReduceKeysTask<K, V> nextRight, BiFunction<? super K, ? super K, ? extends K> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        @Override
        public final K getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super K, ? super K, ? extends K> biFunction = this.reducer;
            if (biFunction == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                ReduceKeysTask<K, V> reduceKeysTask = new ReduceKeysTask<>(this, i4, i3, i2, this.tab, this.rights, biFunction);
                this.rights = reduceKeysTask;
                reduceKeysTask.fork();
            }
            K kApply = null;
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                }
                K k = nodeAdvance.key;
                if (kApply == null) {
                    kApply = k;
                } else if (k != null) {
                    kApply = biFunction.apply(kApply, k);
                }
            }
            this.result = kApply;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                ReduceKeysTask reduceKeysTask2 = (ReduceKeysTask) countedCompleterFirstComplete;
                ReduceKeysTask<K, V> reduceKeysTask3 = reduceKeysTask2.rights;
                while (reduceKeysTask3 != null) {
                    K kApply2 = reduceKeysTask3.result;
                    if (kApply2 != null) {
                        K k2 = reduceKeysTask2.result;
                        if (k2 != null) {
                            kApply2 = biFunction.apply(k2, kApply2);
                        }
                        reduceKeysTask2.result = (K) kApply2;
                    }
                    reduceKeysTask3 = reduceKeysTask3.nextRight;
                    reduceKeysTask2.rights = reduceKeysTask3;
                }
            }
        }
    }

    static final class ReduceValuesTask<K, V> extends BulkTask<K, V, V> {
        ReduceValuesTask<K, V> nextRight;
        final BiFunction<? super V, ? super V, ? extends V> reducer;
        V result;
        ReduceValuesTask<K, V> rights;

        ReduceValuesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, ReduceValuesTask<K, V> nextRight, BiFunction<? super V, ? super V, ? extends V> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        @Override
        public final V getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super V, ? super V, ? extends V> biFunction = this.reducer;
            if (biFunction == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                ReduceValuesTask<K, V> reduceValuesTask = new ReduceValuesTask<>(this, i4, i3, i2, this.tab, this.rights, biFunction);
                this.rights = reduceValuesTask;
                reduceValuesTask.fork();
            }
            V vApply = null;
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                }
                V v = nodeAdvance.val;
                vApply = vApply == null ? v : biFunction.apply(vApply, v);
            }
            this.result = vApply;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                ReduceValuesTask reduceValuesTask2 = (ReduceValuesTask) countedCompleterFirstComplete;
                ReduceValuesTask<K, V> reduceValuesTask3 = reduceValuesTask2.rights;
                while (reduceValuesTask3 != null) {
                    V vApply2 = reduceValuesTask3.result;
                    if (vApply2 != null) {
                        V v2 = reduceValuesTask2.result;
                        if (v2 != null) {
                            vApply2 = biFunction.apply(v2, vApply2);
                        }
                        reduceValuesTask2.result = (V) vApply2;
                    }
                    reduceValuesTask3 = reduceValuesTask3.nextRight;
                    reduceValuesTask2.rights = reduceValuesTask3;
                }
            }
        }
    }

    static final class ReduceEntriesTask<K, V> extends BulkTask<K, V, Map.Entry<K, V>> {
        ReduceEntriesTask<K, V> nextRight;
        final BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> reducer;
        Map.Entry<K, V> result;
        ReduceEntriesTask<K, V> rights;

        ReduceEntriesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, ReduceEntriesTask<K, V> nextRight, BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        @Override
        public final Map.Entry<K, V> getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> reducer = this.reducer;
            if (reducer == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int f = this.baseLimit;
                int h = (f + i) >>> 1;
                if (h <= i) {
                    break;
                }
                addToPendingCount(1);
                int i2 = this.batch >>> 1;
                this.batch = i2;
                this.baseLimit = h;
                ReduceEntriesTask<K, V> reduceEntriesTask = new ReduceEntriesTask<>(this, i2, h, f, this.tab, this.rights, reducer);
                this.rights = reduceEntriesTask;
                reduceEntriesTask.fork();
            }
            Map.Entry<K, V> r = null;
            while (true) {
                Node<K, V> p = advance();
                if (p == null) {
                    break;
                } else {
                    r = r == null ? p : reducer.apply(r, p);
                }
            }
            this.result = r;
            for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                ReduceEntriesTask<K, V> t = (ReduceEntriesTask) c;
                ReduceEntriesTask<K, V> s = t.rights;
                while (s != null) {
                    Map.Entry<K, V> sr = s.result;
                    if (sr != null) {
                        Map.Entry<K, V> tr = t.result;
                        if (tr != null) {
                            sr = reducer.apply(tr, sr);
                        }
                        t.result = sr;
                    }
                    s = s.nextRight;
                    t.rights = s;
                }
            }
        }
    }

    static final class MapReduceKeysTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceKeysTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceKeysTask<K, V, U> rights;
        final Function<? super K, ? extends U> transformer;

        MapReduceKeysTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysTask<K, V, U> nextRight, Function<? super K, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            Function<? super K, ? extends U> function = this.transformer;
            if (function == null || (biFunction = this.reducer) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceKeysTask<K, V, U> mapReduceKeysTask = new MapReduceKeysTask<>(this, i4, i3, i2, this.tab, this.rights, function, biFunction);
                this.rights = mapReduceKeysTask;
                mapReduceKeysTask.fork();
            }
            U uApply = null;
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                }
                U uApply2 = function.apply(nodeAdvance.key);
                if (uApply2 != null) {
                    uApply = uApply == null ? uApply2 : biFunction.apply(uApply, uApply2);
                }
            }
            this.result = uApply;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceKeysTask mapReduceKeysTask2 = (MapReduceKeysTask) countedCompleterFirstComplete;
                MapReduceKeysTask<K, V, U> mapReduceKeysTask3 = mapReduceKeysTask2.rights;
                while (mapReduceKeysTask3 != null) {
                    U uApply3 = mapReduceKeysTask3.result;
                    if (uApply3 != null) {
                        U u = mapReduceKeysTask2.result;
                        if (u != null) {
                            uApply3 = biFunction.apply(u, uApply3);
                        }
                        mapReduceKeysTask2.result = (U) uApply3;
                    }
                    mapReduceKeysTask3 = mapReduceKeysTask3.nextRight;
                    mapReduceKeysTask2.rights = mapReduceKeysTask3;
                }
            }
        }
    }

    static final class MapReduceValuesTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceValuesTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceValuesTask<K, V, U> rights;
        final Function<? super V, ? extends U> transformer;

        MapReduceValuesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesTask<K, V, U> nextRight, Function<? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            Function<? super V, ? extends U> function = this.transformer;
            if (function == null || (biFunction = this.reducer) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceValuesTask<K, V, U> mapReduceValuesTask = new MapReduceValuesTask<>(this, i4, i3, i2, this.tab, this.rights, function, biFunction);
                this.rights = mapReduceValuesTask;
                mapReduceValuesTask.fork();
            }
            U uApply = null;
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                }
                U uApply2 = function.apply(nodeAdvance.val);
                if (uApply2 != null) {
                    uApply = uApply == null ? uApply2 : biFunction.apply(uApply, uApply2);
                }
            }
            this.result = uApply;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceValuesTask mapReduceValuesTask2 = (MapReduceValuesTask) countedCompleterFirstComplete;
                MapReduceValuesTask<K, V, U> mapReduceValuesTask3 = mapReduceValuesTask2.rights;
                while (mapReduceValuesTask3 != null) {
                    U uApply3 = mapReduceValuesTask3.result;
                    if (uApply3 != null) {
                        U u = mapReduceValuesTask2.result;
                        if (u != null) {
                            uApply3 = biFunction.apply(u, uApply3);
                        }
                        mapReduceValuesTask2.result = (U) uApply3;
                    }
                    mapReduceValuesTask3 = mapReduceValuesTask3.nextRight;
                    mapReduceValuesTask2.rights = mapReduceValuesTask3;
                }
            }
        }
    }

    static final class MapReduceEntriesTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceEntriesTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceEntriesTask<K, V, U> rights;
        final Function<Map.Entry<K, V>, ? extends U> transformer;

        MapReduceEntriesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesTask<K, V, U> nextRight, Function<Map.Entry<K, V>, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            Function<Map.Entry<K, V>, ? extends U> function = this.transformer;
            if (function == null || (biFunction = this.reducer) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceEntriesTask<K, V, U> mapReduceEntriesTask = new MapReduceEntriesTask<>(this, i4, i3, i2, this.tab, this.rights, function, biFunction);
                this.rights = mapReduceEntriesTask;
                mapReduceEntriesTask.fork();
            }
            U uApply = null;
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                }
                U uApply2 = function.apply(nodeAdvance);
                if (uApply2 != null) {
                    uApply = uApply == null ? uApply2 : biFunction.apply(uApply, uApply2);
                }
            }
            this.result = uApply;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceEntriesTask mapReduceEntriesTask2 = (MapReduceEntriesTask) countedCompleterFirstComplete;
                MapReduceEntriesTask<K, V, U> mapReduceEntriesTask3 = mapReduceEntriesTask2.rights;
                while (mapReduceEntriesTask3 != null) {
                    U uApply3 = mapReduceEntriesTask3.result;
                    if (uApply3 != null) {
                        U u = mapReduceEntriesTask2.result;
                        if (u != null) {
                            uApply3 = biFunction.apply(u, uApply3);
                        }
                        mapReduceEntriesTask2.result = (U) uApply3;
                    }
                    mapReduceEntriesTask3 = mapReduceEntriesTask3.nextRight;
                    mapReduceEntriesTask2.rights = mapReduceEntriesTask3;
                }
            }
        }
    }

    static final class MapReduceMappingsTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceMappingsTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceMappingsTask<K, V, U> rights;
        final BiFunction<? super K, ? super V, ? extends U> transformer;

        MapReduceMappingsTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsTask<K, V, U> nextRight, BiFunction<? super K, ? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            BiFunction<? super K, ? super V, ? extends U> biFunction2 = this.transformer;
            if (biFunction2 == null || (biFunction = this.reducer) == null) {
                return;
            }
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceMappingsTask<K, V, U> mapReduceMappingsTask = new MapReduceMappingsTask<>(this, i4, i3, i2, this.tab, this.rights, biFunction2, biFunction);
                this.rights = mapReduceMappingsTask;
                mapReduceMappingsTask.fork();
            }
            U uApply = null;
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                }
                U uApply2 = biFunction2.apply(nodeAdvance.key, nodeAdvance.val);
                if (uApply2 != null) {
                    uApply = uApply == null ? uApply2 : biFunction.apply(uApply, uApply2);
                }
            }
            this.result = uApply;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceMappingsTask mapReduceMappingsTask2 = (MapReduceMappingsTask) countedCompleterFirstComplete;
                MapReduceMappingsTask<K, V, U> mapReduceMappingsTask3 = mapReduceMappingsTask2.rights;
                while (mapReduceMappingsTask3 != null) {
                    U uApply3 = mapReduceMappingsTask3.result;
                    if (uApply3 != null) {
                        U u = mapReduceMappingsTask2.result;
                        if (u != null) {
                            uApply3 = biFunction.apply(u, uApply3);
                        }
                        mapReduceMappingsTask2.result = (U) uApply3;
                    }
                    mapReduceMappingsTask3 = mapReduceMappingsTask3.nextRight;
                    mapReduceMappingsTask2.rights = mapReduceMappingsTask3;
                }
            }
        }
    }

    static final class MapReduceKeysToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceKeysToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceKeysToDoubleTask<K, V> rights;
        final ToDoubleFunction<? super K> transformer;

        MapReduceKeysToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysToDoubleTask<K, V> nextRight, ToDoubleFunction<? super K> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator doubleBinaryOperator;
            ToDoubleFunction<? super K> toDoubleFunction = this.transformer;
            if (toDoubleFunction == null || (doubleBinaryOperator = this.reducer) == null) {
                return;
            }
            double dApplyAsDouble = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceKeysToDoubleTask<K, V> mapReduceKeysToDoubleTask = new MapReduceKeysToDoubleTask<>(this, i4, i3, i2, this.tab, this.rights, toDoubleFunction, dApplyAsDouble, doubleBinaryOperator);
                this.rights = mapReduceKeysToDoubleTask;
                mapReduceKeysToDoubleTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, toDoubleFunction.applyAsDouble(nodeAdvance.key));
                }
            }
            this.result = dApplyAsDouble;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceKeysToDoubleTask mapReduceKeysToDoubleTask2 = (MapReduceKeysToDoubleTask) countedCompleterFirstComplete;
                MapReduceKeysToDoubleTask<K, V> mapReduceKeysToDoubleTask3 = mapReduceKeysToDoubleTask2.rights;
                while (mapReduceKeysToDoubleTask3 != null) {
                    mapReduceKeysToDoubleTask2.result = doubleBinaryOperator.applyAsDouble(mapReduceKeysToDoubleTask2.result, mapReduceKeysToDoubleTask3.result);
                    mapReduceKeysToDoubleTask3 = mapReduceKeysToDoubleTask3.nextRight;
                    mapReduceKeysToDoubleTask2.rights = mapReduceKeysToDoubleTask3;
                }
            }
        }
    }

    static final class MapReduceValuesToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceValuesToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceValuesToDoubleTask<K, V> rights;
        final ToDoubleFunction<? super V> transformer;

        MapReduceValuesToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesToDoubleTask<K, V> nextRight, ToDoubleFunction<? super V> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator doubleBinaryOperator;
            ToDoubleFunction<? super V> toDoubleFunction = this.transformer;
            if (toDoubleFunction == null || (doubleBinaryOperator = this.reducer) == null) {
                return;
            }
            double dApplyAsDouble = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceValuesToDoubleTask<K, V> mapReduceValuesToDoubleTask = new MapReduceValuesToDoubleTask<>(this, i4, i3, i2, this.tab, this.rights, toDoubleFunction, dApplyAsDouble, doubleBinaryOperator);
                this.rights = mapReduceValuesToDoubleTask;
                mapReduceValuesToDoubleTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, toDoubleFunction.applyAsDouble(nodeAdvance.val));
                }
            }
            this.result = dApplyAsDouble;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceValuesToDoubleTask mapReduceValuesToDoubleTask2 = (MapReduceValuesToDoubleTask) countedCompleterFirstComplete;
                MapReduceValuesToDoubleTask<K, V> mapReduceValuesToDoubleTask3 = mapReduceValuesToDoubleTask2.rights;
                while (mapReduceValuesToDoubleTask3 != null) {
                    mapReduceValuesToDoubleTask2.result = doubleBinaryOperator.applyAsDouble(mapReduceValuesToDoubleTask2.result, mapReduceValuesToDoubleTask3.result);
                    mapReduceValuesToDoubleTask3 = mapReduceValuesToDoubleTask3.nextRight;
                    mapReduceValuesToDoubleTask2.rights = mapReduceValuesToDoubleTask3;
                }
            }
        }
    }

    static final class MapReduceEntriesToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceEntriesToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceEntriesToDoubleTask<K, V> rights;
        final ToDoubleFunction<Map.Entry<K, V>> transformer;

        MapReduceEntriesToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesToDoubleTask<K, V> nextRight, ToDoubleFunction<Map.Entry<K, V>> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator reducer;
            ToDoubleFunction<Map.Entry<K, V>> transformer = this.transformer;
            if (transformer == null || (reducer = this.reducer) == null) {
                return;
            }
            double r = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int f = this.baseLimit;
                int h = (f + i) >>> 1;
                if (h <= i) {
                    break;
                }
                addToPendingCount(1);
                int i2 = this.batch >>> 1;
                this.batch = i2;
                this.baseLimit = h;
                MapReduceEntriesToDoubleTask<K, V> mapReduceEntriesToDoubleTask = new MapReduceEntriesToDoubleTask<>(this, i2, h, f, this.tab, this.rights, transformer, r, reducer);
                this.rights = mapReduceEntriesToDoubleTask;
                mapReduceEntriesToDoubleTask.fork();
            }
            while (true) {
                Node<K, V> p = advance();
                if (p == null) {
                    break;
                } else {
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p));
                }
            }
            this.result = r;
            for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                MapReduceEntriesToDoubleTask<K, V> t = (MapReduceEntriesToDoubleTask) c;
                MapReduceEntriesToDoubleTask<K, V> s = t.rights;
                while (s != null) {
                    t.result = reducer.applyAsDouble(t.result, s.result);
                    s = s.nextRight;
                    t.rights = s;
                }
            }
        }
    }

    static final class MapReduceMappingsToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceMappingsToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceMappingsToDoubleTask<K, V> rights;
        final ToDoubleBiFunction<? super K, ? super V> transformer;

        MapReduceMappingsToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsToDoubleTask<K, V> nextRight, ToDoubleBiFunction<? super K, ? super V> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator doubleBinaryOperator;
            ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction = this.transformer;
            if (toDoubleBiFunction == null || (doubleBinaryOperator = this.reducer) == null) {
                return;
            }
            double dApplyAsDouble = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceMappingsToDoubleTask<K, V> mapReduceMappingsToDoubleTask = new MapReduceMappingsToDoubleTask<>(this, i4, i3, i2, this.tab, this.rights, toDoubleBiFunction, dApplyAsDouble, doubleBinaryOperator);
                this.rights = mapReduceMappingsToDoubleTask;
                mapReduceMappingsToDoubleTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, toDoubleBiFunction.applyAsDouble(nodeAdvance.key, nodeAdvance.val));
                }
            }
            this.result = dApplyAsDouble;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceMappingsToDoubleTask mapReduceMappingsToDoubleTask2 = (MapReduceMappingsToDoubleTask) countedCompleterFirstComplete;
                MapReduceMappingsToDoubleTask<K, V> mapReduceMappingsToDoubleTask3 = mapReduceMappingsToDoubleTask2.rights;
                while (mapReduceMappingsToDoubleTask3 != null) {
                    mapReduceMappingsToDoubleTask2.result = doubleBinaryOperator.applyAsDouble(mapReduceMappingsToDoubleTask2.result, mapReduceMappingsToDoubleTask3.result);
                    mapReduceMappingsToDoubleTask3 = mapReduceMappingsToDoubleTask3.nextRight;
                    mapReduceMappingsToDoubleTask2.rights = mapReduceMappingsToDoubleTask3;
                }
            }
        }
    }

    static final class MapReduceKeysToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceKeysToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceKeysToLongTask<K, V> rights;
        final ToLongFunction<? super K> transformer;

        MapReduceKeysToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysToLongTask<K, V> nextRight, ToLongFunction<? super K> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator longBinaryOperator;
            ToLongFunction<? super K> toLongFunction = this.transformer;
            if (toLongFunction == null || (longBinaryOperator = this.reducer) == null) {
                return;
            }
            long jApplyAsLong = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceKeysToLongTask<K, V> mapReduceKeysToLongTask = new MapReduceKeysToLongTask<>(this, i4, i3, i2, this.tab, this.rights, toLongFunction, jApplyAsLong, longBinaryOperator);
                this.rights = mapReduceKeysToLongTask;
                mapReduceKeysToLongTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, toLongFunction.applyAsLong(nodeAdvance.key));
                }
            }
            this.result = jApplyAsLong;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceKeysToLongTask mapReduceKeysToLongTask2 = (MapReduceKeysToLongTask) countedCompleterFirstComplete;
                MapReduceKeysToLongTask<K, V> mapReduceKeysToLongTask3 = mapReduceKeysToLongTask2.rights;
                while (mapReduceKeysToLongTask3 != null) {
                    mapReduceKeysToLongTask2.result = longBinaryOperator.applyAsLong(mapReduceKeysToLongTask2.result, mapReduceKeysToLongTask3.result);
                    mapReduceKeysToLongTask3 = mapReduceKeysToLongTask3.nextRight;
                    mapReduceKeysToLongTask2.rights = mapReduceKeysToLongTask3;
                }
            }
        }
    }

    static final class MapReduceValuesToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceValuesToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceValuesToLongTask<K, V> rights;
        final ToLongFunction<? super V> transformer;

        MapReduceValuesToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesToLongTask<K, V> nextRight, ToLongFunction<? super V> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator longBinaryOperator;
            ToLongFunction<? super V> toLongFunction = this.transformer;
            if (toLongFunction == null || (longBinaryOperator = this.reducer) == null) {
                return;
            }
            long jApplyAsLong = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceValuesToLongTask<K, V> mapReduceValuesToLongTask = new MapReduceValuesToLongTask<>(this, i4, i3, i2, this.tab, this.rights, toLongFunction, jApplyAsLong, longBinaryOperator);
                this.rights = mapReduceValuesToLongTask;
                mapReduceValuesToLongTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, toLongFunction.applyAsLong(nodeAdvance.val));
                }
            }
            this.result = jApplyAsLong;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceValuesToLongTask mapReduceValuesToLongTask2 = (MapReduceValuesToLongTask) countedCompleterFirstComplete;
                MapReduceValuesToLongTask<K, V> mapReduceValuesToLongTask3 = mapReduceValuesToLongTask2.rights;
                while (mapReduceValuesToLongTask3 != null) {
                    mapReduceValuesToLongTask2.result = longBinaryOperator.applyAsLong(mapReduceValuesToLongTask2.result, mapReduceValuesToLongTask3.result);
                    mapReduceValuesToLongTask3 = mapReduceValuesToLongTask3.nextRight;
                    mapReduceValuesToLongTask2.rights = mapReduceValuesToLongTask3;
                }
            }
        }
    }

    static final class MapReduceEntriesToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceEntriesToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceEntriesToLongTask<K, V> rights;
        final ToLongFunction<Map.Entry<K, V>> transformer;

        MapReduceEntriesToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesToLongTask<K, V> nextRight, ToLongFunction<Map.Entry<K, V>> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator reducer;
            ToLongFunction<Map.Entry<K, V>> transformer = this.transformer;
            if (transformer == null || (reducer = this.reducer) == null) {
                return;
            }
            long r = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int f = this.baseLimit;
                int h = (f + i) >>> 1;
                if (h <= i) {
                    break;
                }
                addToPendingCount(1);
                int i2 = this.batch >>> 1;
                this.batch = i2;
                this.baseLimit = h;
                MapReduceEntriesToLongTask<K, V> mapReduceEntriesToLongTask = new MapReduceEntriesToLongTask<>(this, i2, h, f, this.tab, this.rights, transformer, r, reducer);
                this.rights = mapReduceEntriesToLongTask;
                mapReduceEntriesToLongTask.fork();
            }
            while (true) {
                Node<K, V> p = advance();
                if (p == null) {
                    break;
                } else {
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p));
                }
            }
            this.result = r;
            for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                MapReduceEntriesToLongTask<K, V> t = (MapReduceEntriesToLongTask) c;
                MapReduceEntriesToLongTask<K, V> s = t.rights;
                while (s != null) {
                    t.result = reducer.applyAsLong(t.result, s.result);
                    s = s.nextRight;
                    t.rights = s;
                }
            }
        }
    }

    static final class MapReduceMappingsToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceMappingsToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceMappingsToLongTask<K, V> rights;
        final ToLongBiFunction<? super K, ? super V> transformer;

        MapReduceMappingsToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsToLongTask<K, V> nextRight, ToLongBiFunction<? super K, ? super V> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator longBinaryOperator;
            ToLongBiFunction<? super K, ? super V> toLongBiFunction = this.transformer;
            if (toLongBiFunction == null || (longBinaryOperator = this.reducer) == null) {
                return;
            }
            long jApplyAsLong = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceMappingsToLongTask<K, V> mapReduceMappingsToLongTask = new MapReduceMappingsToLongTask<>(this, i4, i3, i2, this.tab, this.rights, toLongBiFunction, jApplyAsLong, longBinaryOperator);
                this.rights = mapReduceMappingsToLongTask;
                mapReduceMappingsToLongTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, toLongBiFunction.applyAsLong(nodeAdvance.key, nodeAdvance.val));
                }
            }
            this.result = jApplyAsLong;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceMappingsToLongTask mapReduceMappingsToLongTask2 = (MapReduceMappingsToLongTask) countedCompleterFirstComplete;
                MapReduceMappingsToLongTask<K, V> mapReduceMappingsToLongTask3 = mapReduceMappingsToLongTask2.rights;
                while (mapReduceMappingsToLongTask3 != null) {
                    mapReduceMappingsToLongTask2.result = longBinaryOperator.applyAsLong(mapReduceMappingsToLongTask2.result, mapReduceMappingsToLongTask3.result);
                    mapReduceMappingsToLongTask3 = mapReduceMappingsToLongTask3.nextRight;
                    mapReduceMappingsToLongTask2.rights = mapReduceMappingsToLongTask3;
                }
            }
        }
    }

    static final class MapReduceKeysToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceKeysToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceKeysToIntTask<K, V> rights;
        final ToIntFunction<? super K> transformer;

        MapReduceKeysToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysToIntTask<K, V> nextRight, ToIntFunction<? super K> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator intBinaryOperator;
            ToIntFunction<? super K> toIntFunction = this.transformer;
            if (toIntFunction == null || (intBinaryOperator = this.reducer) == null) {
                return;
            }
            int iApplyAsInt = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceKeysToIntTask<K, V> mapReduceKeysToIntTask = new MapReduceKeysToIntTask<>(this, i4, i3, i2, this.tab, this.rights, toIntFunction, iApplyAsInt, intBinaryOperator);
                this.rights = mapReduceKeysToIntTask;
                mapReduceKeysToIntTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    iApplyAsInt = intBinaryOperator.applyAsInt(iApplyAsInt, toIntFunction.applyAsInt(nodeAdvance.key));
                }
            }
            this.result = iApplyAsInt;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceKeysToIntTask mapReduceKeysToIntTask2 = (MapReduceKeysToIntTask) countedCompleterFirstComplete;
                MapReduceKeysToIntTask<K, V> mapReduceKeysToIntTask3 = mapReduceKeysToIntTask2.rights;
                while (mapReduceKeysToIntTask3 != null) {
                    mapReduceKeysToIntTask2.result = intBinaryOperator.applyAsInt(mapReduceKeysToIntTask2.result, mapReduceKeysToIntTask3.result);
                    mapReduceKeysToIntTask3 = mapReduceKeysToIntTask3.nextRight;
                    mapReduceKeysToIntTask2.rights = mapReduceKeysToIntTask3;
                }
            }
        }
    }

    static final class MapReduceValuesToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceValuesToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceValuesToIntTask<K, V> rights;
        final ToIntFunction<? super V> transformer;

        MapReduceValuesToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesToIntTask<K, V> nextRight, ToIntFunction<? super V> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator intBinaryOperator;
            ToIntFunction<? super V> toIntFunction = this.transformer;
            if (toIntFunction == null || (intBinaryOperator = this.reducer) == null) {
                return;
            }
            int iApplyAsInt = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceValuesToIntTask<K, V> mapReduceValuesToIntTask = new MapReduceValuesToIntTask<>(this, i4, i3, i2, this.tab, this.rights, toIntFunction, iApplyAsInt, intBinaryOperator);
                this.rights = mapReduceValuesToIntTask;
                mapReduceValuesToIntTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    iApplyAsInt = intBinaryOperator.applyAsInt(iApplyAsInt, toIntFunction.applyAsInt(nodeAdvance.val));
                }
            }
            this.result = iApplyAsInt;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceValuesToIntTask mapReduceValuesToIntTask2 = (MapReduceValuesToIntTask) countedCompleterFirstComplete;
                MapReduceValuesToIntTask<K, V> mapReduceValuesToIntTask3 = mapReduceValuesToIntTask2.rights;
                while (mapReduceValuesToIntTask3 != null) {
                    mapReduceValuesToIntTask2.result = intBinaryOperator.applyAsInt(mapReduceValuesToIntTask2.result, mapReduceValuesToIntTask3.result);
                    mapReduceValuesToIntTask3 = mapReduceValuesToIntTask3.nextRight;
                    mapReduceValuesToIntTask2.rights = mapReduceValuesToIntTask3;
                }
            }
        }
    }

    static final class MapReduceEntriesToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceEntriesToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceEntriesToIntTask<K, V> rights;
        final ToIntFunction<Map.Entry<K, V>> transformer;

        MapReduceEntriesToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesToIntTask<K, V> nextRight, ToIntFunction<Map.Entry<K, V>> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator reducer;
            ToIntFunction<Map.Entry<K, V>> transformer = this.transformer;
            if (transformer == null || (reducer = this.reducer) == null) {
                return;
            }
            int r = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int f = this.baseLimit;
                int h = (f + i) >>> 1;
                if (h <= i) {
                    break;
                }
                addToPendingCount(1);
                int i2 = this.batch >>> 1;
                this.batch = i2;
                this.baseLimit = h;
                MapReduceEntriesToIntTask<K, V> mapReduceEntriesToIntTask = new MapReduceEntriesToIntTask<>(this, i2, h, f, this.tab, this.rights, transformer, r, reducer);
                this.rights = mapReduceEntriesToIntTask;
                mapReduceEntriesToIntTask.fork();
            }
            while (true) {
                Node<K, V> p = advance();
                if (p == null) {
                    break;
                } else {
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p));
                }
            }
            this.result = r;
            for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                MapReduceEntriesToIntTask<K, V> t = (MapReduceEntriesToIntTask) c;
                MapReduceEntriesToIntTask<K, V> s = t.rights;
                while (s != null) {
                    t.result = reducer.applyAsInt(t.result, s.result);
                    s = s.nextRight;
                    t.rights = s;
                }
            }
        }
    }

    static final class MapReduceMappingsToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceMappingsToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceMappingsToIntTask<K, V> rights;
        final ToIntBiFunction<? super K, ? super V> transformer;

        MapReduceMappingsToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsToIntTask<K, V> nextRight, ToIntBiFunction<? super K, ? super V> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator intBinaryOperator;
            ToIntBiFunction<? super K, ? super V> toIntBiFunction = this.transformer;
            if (toIntBiFunction == null || (intBinaryOperator = this.reducer) == null) {
                return;
            }
            int iApplyAsInt = this.basis;
            int i = this.baseIndex;
            while (this.batch > 0) {
                int i2 = this.baseLimit;
                int i3 = (i2 + i) >>> 1;
                if (i3 <= i) {
                    break;
                }
                addToPendingCount(1);
                int i4 = this.batch >>> 1;
                this.batch = i4;
                this.baseLimit = i3;
                MapReduceMappingsToIntTask<K, V> mapReduceMappingsToIntTask = new MapReduceMappingsToIntTask<>(this, i4, i3, i2, this.tab, this.rights, toIntBiFunction, iApplyAsInt, intBinaryOperator);
                this.rights = mapReduceMappingsToIntTask;
                mapReduceMappingsToIntTask.fork();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance == null) {
                    break;
                } else {
                    iApplyAsInt = intBinaryOperator.applyAsInt(iApplyAsInt, toIntBiFunction.applyAsInt(nodeAdvance.key, nodeAdvance.val));
                }
            }
            this.result = iApplyAsInt;
            for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                MapReduceMappingsToIntTask mapReduceMappingsToIntTask2 = (MapReduceMappingsToIntTask) countedCompleterFirstComplete;
                MapReduceMappingsToIntTask<K, V> mapReduceMappingsToIntTask3 = mapReduceMappingsToIntTask2.rights;
                while (mapReduceMappingsToIntTask3 != null) {
                    mapReduceMappingsToIntTask2.result = intBinaryOperator.applyAsInt(mapReduceMappingsToIntTask2.result, mapReduceMappingsToIntTask3.result);
                    mapReduceMappingsToIntTask3 = mapReduceMappingsToIntTask3.nextRight;
                    mapReduceMappingsToIntTask2.rights = mapReduceMappingsToIntTask3;
                }
            }
        }
    }
}
