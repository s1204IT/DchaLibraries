package java.util.concurrent;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
    private static final long ABASE;
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
    private static final int MIN_TRANSFER_STRIDE = 16;
    static final int MIN_TREEIFY_CAPACITY = 64;
    static final int MOVED = -1879048193;
    static final int RESERVED = -2147483647;
    static final int SEED_INCREMENT = 1640531527;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long TRANSFERORIGIN;
    static final int TREEBIN = Integer.MIN_VALUE;
    static final int TREEIFY_THRESHOLD = 8;
    private static final Unsafe U;
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
    private volatile transient int transferOrigin;
    private transient ValuesView<K, V> values;
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("segments", (Class<?>) Segment[].class), new ObjectStreamField("segmentMask", Integer.TYPE), new ObjectStreamField("segmentShift", Integer.TYPE)};
    static final AtomicInteger counterHashCodeGenerator = new AtomicInteger();
    static final ThreadLocal<CounterHashCode> threadCounterHashCode = new ThreadLocal<>();

    static {
        try {
            U = Unsafe.getUnsafe();
            SIZECTL = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("transferIndex"));
            TRANSFERORIGIN = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("transferOrigin"));
            BASECOUNT = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("cellsBusy"));
            CELLVALUE = U.objectFieldOffset(CounterCell.class.getDeclaredField("value"));
            ABASE = U.arrayBaseOffset(Node[].class);
            int scale = U.arrayIndexScale(Node[].class);
            if (((scale - 1) & scale) != 0) {
                throw new Error("data type scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        Node<K, V> next;
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
            return this.key + "=" + this.val;
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
            Object u;
            return (o instanceof Map.Entry) && (k = (e = (Map.Entry) o).getKey()) != null && (v = e.getValue()) != null && (k == this.key || k.equals(this.key)) && (v == (u = this.val) || v.equals(u));
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
            if (c != String.class) {
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
            } else {
                return c;
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
        return (Node) U.getObjectVolatile(tab, (((long) i) << ASHIFT) + ABASE);
    }

    static final <K, V> boolean casTabAt(Node<K, V>[] tab, int i, Node<K, V> c, Node<K, V> v) {
        return U.compareAndSwapObject(tab, (((long) i) << ASHIFT) + ABASE, c, v);
    }

    static final <K, V> void setTabAt(Node<K, V>[] tab, int i, Node<K, V> v) {
        U.putOrderedObject(tab, (((long) i) << ASHIFT) + ABASE, v);
    }

    public ConcurrentHashMap() {
    }

    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        int cap = initialCapacity >= 536870912 ? 1073741824 : tableSizeFor((initialCapacity >>> 1) + initialCapacity + 1);
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
        if (loadFactor <= 0.0f || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }
        long size = (long) (1.0d + ((double) ((initialCapacity < concurrencyLevel ? concurrencyLevel : initialCapacity) / loadFactor)));
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
        if (tab == null || (n = tab.length) <= 0 || (e = tabAt(tab, (n - 1) & h)) == null) {
            return null;
        }
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
                return null;
            }
            if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                break;
            }
        }
        return e.val;
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
        if (t == null) {
            return false;
        }
        Traverser<K, V> it = new Traverser<>(t, t.length, 0, t.length);
        while (true) {
            Node<K, V> p = it.advance();
            if (p == null) {
                return false;
            }
            V v = p.val;
            if (v == value || (v != null && value.equals(v))) {
                break;
            }
        }
        return true;
    }

    @Override
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    final V putVal(K key, V value, boolean onlyIfAbsent) {
        V v;
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
                        if (fh == MOVED) {
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
                                        v = oldVal;
                                    } else if (f instanceof TreeBin) {
                                        binCount = 2;
                                        Node<K, V> p = ((TreeBin) f).putTreeVal(hash, key, value);
                                        if (p != null) {
                                            V oldVal2 = p.val;
                                            if (!onlyIfAbsent) {
                                                p.val = value;
                                            }
                                            v = oldVal2;
                                        }
                                    }
                                }
                                v = null;
                            }
                            if (binCount != 0) {
                                if (binCount >= 8) {
                                    treeifyBin(tab, i);
                                }
                                if (v != null) {
                                    return v;
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
        V v;
        TreeNode<K, V> p;
        K ek;
        int hash = spread(key.hashCode());
        Node<K, V>[] tab = this.table;
        while (true) {
            if (tab == null) {
                break;
            }
            int n = tab.length;
            if (n == 0 || (f = tabAt(tab, (i = (n - 1) & hash))) == null) {
                break;
            }
            int fh = f.hash;
            if (fh == MOVED) {
                tab = helpTransfer(tab, f);
            } else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            Node<K, V> e = f;
                            Node<K, V> pred = null;
                            do {
                                if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    if (cv == null || cv == ev || (ev != null && cv.equals(ev))) {
                                        oldVal = ev;
                                        if (value != null) {
                                            e.val = value;
                                        } else if (pred != null) {
                                            pred.next = e.next;
                                        } else {
                                            setTabAt(tab, i, e.next);
                                        }
                                    }
                                    v = oldVal;
                                } else {
                                    pred = e;
                                    e = e.next;
                                }
                            } while (e != null);
                            v = oldVal;
                        } else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K, V> t = (TreeBin) f;
                            TreeNode<K, V> r = t.root;
                            if (r != null && (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv || (pv != null && cv.equals(pv))) {
                                    if (value != null) {
                                        p.val = value;
                                        v = pv;
                                    } else {
                                        if (t.removeTreeNode(p)) {
                                            setTabAt(tab, i, untreeify(t.first));
                                        }
                                        v = pv;
                                    }
                                }
                            }
                        }
                    }
                    v = null;
                }
                if (validated) {
                    if (v != null) {
                        if (value != null) {
                            return v;
                        }
                        addCount(-1L, -1);
                        return v;
                    }
                }
            }
        }
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
                if (fh == MOVED) {
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
                            throw th;
                        }
                    }
                }
            }
        }
        if (delta != 0) {
            addCount(delta, -1);
        }
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
                    if (v2 == null) {
                        return false;
                    }
                    if (v2 != val && !v2.equals(val)) {
                        return false;
                    }
                } else {
                    for (Map.Entry<K, V> entry : m.entrySet()) {
                        Object mk = entry.getKey();
                        if (mk == null || (mv = entry.getValue()) == null || (v = get(mk)) == null) {
                            return false;
                        }
                        if (mv != v && !mv.equals(v)) {
                            return false;
                        }
                    }
                }
            }
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
        s.putFields().put("segments", segments);
        s.putFields().put("segmentShift", segmentShift);
        s.putFields().put("segmentMask", segmentMask);
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

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        int n;
        boolean insertAtFront;
        this.sizeCtl = -1;
        s.defaultReadObject();
        long size = 0;
        Node<K, V> p = null;
        while (true) {
            Object object = s.readObject();
            Object object2 = s.readObject();
            if (object == null || object2 == null) {
                break;
            }
            size++;
            p = new Node<>(spread(object.hashCode()), object, object2, p);
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
        while (p != null) {
            Node<K, V> next = p.next;
            int h = p.hash;
            int j = h & mask;
            Node<K, V> first = tabAt(tab, j);
            if (first == null) {
                insertAtFront = true;
            } else {
                K k = p.key;
                if (first.hash < 0) {
                    if (((TreeBin) first).putTreeVal(h, k, p.val) == null) {
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
                        p.next = first;
                        TreeNode<K, V> hd = null;
                        TreeNode<K, V> tl = null;
                        for (Node<K, V> q2 = p; q2 != null; q2 = q2.next) {
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
                p.next = first;
                setTabAt(tab, j, p);
            }
            p = next;
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

    public Set<K> keySet(V mappedValue) {
        if (mappedValue == null) {
            throw new NullPointerException();
        }
        return new KeySetView(this, mappedValue);
    }

    static final class ForwardingNode<K, V> extends Node<K, V> {
        final Node<K, V>[] nextTable;

        ForwardingNode(Node<K, V>[] tab) {
            super(ConcurrentHashMap.MOVED, null, null, null);
            this.nextTable = tab;
        }

        @Override
        Node<K, V> find(int h, Object k) {
            int n;
            Node<K, V> e;
            Node<K, V>[] tab = this.nextTable;
            if (k != null && tab != null && (n = tab.length) > 0 && (e = ConcurrentHashMap.tabAt(tab, (n - 1) & h)) != null) {
                do {
                    int eh = e.hash;
                    if (eh == h) {
                        K ek = e.key;
                        if (ek == k) {
                            return e;
                        }
                        if (ek != null && k.equals(ek)) {
                            return e;
                        }
                    }
                    if (eh < 0) {
                        return e.find(h, k);
                    }
                    e = e.next;
                } while (e != null);
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
                CounterHashCode hc = threadCounterHashCode.get();
                if (hc != null && as != null && as.length - 1 >= 0 && (a = as[hc.code & m]) != null) {
                    Unsafe unsafe2 = U;
                    long j2 = CELLVALUE;
                    long v = a.value;
                    uncontended = unsafe2.compareAndSwapLong(a, j2, v, v + x);
                    if (uncontended) {
                        if (check > 1) {
                            s = sumCount();
                        } else {
                            return;
                        }
                    }
                }
                fullAddCount(x, hc, uncontended);
                return;
            }
        }
        if (check < 0) {
            return;
        }
        while (true) {
            int sc = this.sizeCtl;
            if (s >= sc && (tab = this.table) != null && tab.length < 1073741824) {
                if (sc < 0) {
                    if (sc != -1 && this.transferIndex > this.transferOrigin && (nt = this.nextTable) != null) {
                        if (U.compareAndSwapInt(this, SIZECTL, sc, sc - 1)) {
                            transfer(tab, nt);
                        }
                    } else {
                        return;
                    }
                } else if (U.compareAndSwapInt(this, SIZECTL, sc, -2)) {
                    transfer(tab, null);
                }
                s = sumCount();
            } else {
                return;
            }
        }
    }

    final Node<K, V>[] helpTransfer(Node<K, V>[] tab, Node<K, V> f) {
        Node<K, V>[] nextTab;
        int sc;
        if (!(f instanceof ForwardingNode) || (nextTab = ((ForwardingNode) f).nextTable) == null) {
            return this.table;
        }
        if (nextTab == this.nextTable && tab == this.table && this.transferIndex > this.transferOrigin && (sc = this.sizeCtl) < -1 && U.compareAndSwapInt(this, SIZECTL, sc, sc - 1)) {
            transfer(tab, nextTab);
            return nextTab;
        }
        return nextTab;
    }

    private final void tryPresize(int size) {
        int n;
        int c = size >= 536870912 ? 1073741824 : tableSizeFor((size >>> 1) + size + 1);
        while (true) {
            int sc = this.sizeCtl;
            if (sc >= 0) {
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
                } else if (c > sc && n < 1073741824) {
                    if (tab == this.table && U.compareAndSwapInt(this, SIZECTL, sc, -2)) {
                        transfer(tab, null);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private final void transfer(Node<K, V>[] tab, Node<K, V>[] nextTab) {
        Unsafe unsafe;
        long j;
        int sc;
        int sc2;
        Node<K, V> hn;
        Node<K, V> ln;
        Node<K, V> hn2;
        Node<K, V> ln2;
        Node<K, V> hn3;
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
                this.nextTable = nextTab;
                this.transferOrigin = n;
                this.transferIndex = n;
                ForwardingNode<K, V> rev = new ForwardingNode<>(tab);
                int k = n;
                while (k > 0) {
                    int nextk = k > stride ? k - stride : 0;
                    for (int m = nextk; m < k; m++) {
                        nextTab[m] = rev;
                    }
                    for (int m2 = n + nextk; m2 < n + k; m2++) {
                        nextTab[m2] = rev;
                    }
                    k = nextk;
                    U.putOrderedInt(this, TRANSFERORIGIN, nextk);
                }
            } catch (Throwable th) {
                this.sizeCtl = Integer.MAX_VALUE;
                return;
            }
        }
        int nextn = nextTab.length;
        ForwardingNode<K, V> fwd = new ForwardingNode<>(nextTab);
        boolean advance = true;
        int i = 0;
        int bound = 0;
        while (true) {
            if (advance) {
                i--;
                if (i >= bound) {
                    advance = false;
                } else {
                    int nextIndex = this.transferIndex;
                    if (nextIndex <= this.transferOrigin) {
                        i = -1;
                        advance = false;
                    } else {
                        Unsafe unsafe2 = U;
                        long j2 = TRANSFERINDEX;
                        int nextBound = nextIndex > stride ? nextIndex - stride : 0;
                        if (unsafe2.compareAndSwapInt(this, j2, nextIndex, nextBound)) {
                            bound = nextBound;
                            i = nextIndex - 1;
                            advance = false;
                        }
                    }
                }
            } else {
                if (i < 0 || i >= n || i + n >= nextn) {
                    break;
                }
                Node<K, V> f = tabAt(tab, i);
                if (f == null) {
                    if (casTabAt(tab, i, null, fwd)) {
                        setTabAt(nextTab, i, null);
                        setTabAt(nextTab, i + n, null);
                        advance = true;
                    }
                } else {
                    int fh = f.hash;
                    if (fh == MOVED) {
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
                                        hn2 = null;
                                    } else {
                                        hn2 = lastRun;
                                        ln2 = null;
                                    }
                                    Node<K, V> p2 = f;
                                    Node<K, V> hn4 = hn2;
                                    Node<K, V> ln4 = ln2;
                                    while (p2 != lastRun) {
                                        int ph = p2.hash;
                                        K pk = p2.key;
                                        V pv = p2.val;
                                        if ((ph & n) == 0) {
                                            ln3 = new Node<>(ph, pk, pv, ln4);
                                            hn3 = hn4;
                                        } else {
                                            hn3 = new Node<>(ph, pk, pv, hn4);
                                            ln3 = ln4;
                                        }
                                        p2 = p2.next;
                                        hn4 = hn3;
                                        ln4 = ln3;
                                    }
                                    hn = hn4;
                                    ln = ln4;
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
                                } else {
                                    hn = null;
                                    ln = null;
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
        do {
            unsafe = U;
            j = SIZECTL;
            sc = this.sizeCtl;
            sc2 = sc + 1;
        } while (!unsafe.compareAndSwapInt(this, j, sc, sc2));
        if (sc2 == -1) {
            this.nextTable = null;
            this.table = nextTab;
            this.sizeCtl = (n << 1) - (n >>> 1);
        }
    }

    private final void treeifyBin(Node<K, V>[] tab, int index) {
        int sc;
        if (tab != null) {
            int n = tab.length;
            if (n < 64) {
                if (tab == this.table && (sc = this.sizeCtl) >= 0 && U.compareAndSwapInt(this, SIZECTL, sc, -2)) {
                    transfer(tab, null);
                    return;
                }
                return;
            }
            Node<K, V> b = tabAt(tab, index);
            if (b != null) {
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
            TreeNode<K, V> q;
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
                        if (pk != k && (pk == null || !k.equals(pk))) {
                            if (pl == null && pr == null) {
                                break;
                            }
                            if ((kc != null || (kc = ConcurrentHashMap.comparableClassFor(k)) != null) && (dir = ConcurrentHashMap.compareComparables(kc, k, pk)) != 0) {
                                p = dir < 0 ? pl : pr;
                            } else if (pl == null) {
                                p = pr;
                            } else {
                                if (pr != null && (q = pr.findTreeNode(h, k, kc)) != null) {
                                    return q;
                                }
                                p = pl;
                            }
                        } else {
                            return p;
                        }
                    }
                } while (p != null);
            }
            return null;
        }
    }

    static final class TreeBin<K, V> extends Node<K, V> {
        static final boolean $assertionsDisabled;
        private static final long LOCKSTATE;
        static final int READER = 4;
        private static final Unsafe U;
        static final int WAITER = 2;
        static final int WRITER = 1;
        volatile TreeNode<K, V> first;
        volatile int lockState;
        TreeNode<K, V> root;
        volatile Thread waiter;

        static {
            $assertionsDisabled = !ConcurrentHashMap.class.desiredAssertionStatus();
            try {
                U = Unsafe.getUnsafe();
                LOCKSTATE = U.objectFieldOffset(TreeBin.class.getDeclaredField("lockState"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        TreeBin(TreeNode<K, V> b) {
            int dir;
            TreeNode<K, V> xp;
            super(Integer.MIN_VALUE, null, null, null);
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
                    Object key = x.key;
                    int hash = x.hash;
                    Class<?> kc = null;
                    TreeNode<K, V> p = r;
                    do {
                        int ph = p.hash;
                        if (ph > hash) {
                            dir = -1;
                        } else if (ph < hash) {
                            dir = 1;
                        } else if (kc != null || (kc = ConcurrentHashMap.comparableClassFor(key)) != null) {
                            dir = ConcurrentHashMap.compareComparables(kc, key, p.key);
                        } else {
                            dir = 0;
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
        }

        private final void lockRoot() {
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, 1)) {
                contendedLock();
            }
        }

        private final void unlockRoot() {
            this.lockState = 0;
        }

        private final void contendedLock() {
            boolean waiting = false;
            while (true) {
                int s = this.lockState;
                if ((s & 1) == 0) {
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
            Unsafe unsafe;
            long j;
            int ls;
            Thread w;
            Unsafe unsafe2;
            long j2;
            int ls2;
            Thread w2;
            if (k != null) {
                for (Node<K, V> e = this.first; e != null; e = e.next) {
                    int s = this.lockState;
                    if ((s & 3) != 0) {
                        if (e.hash == h) {
                            K ek = e.key;
                            if (ek == k) {
                                return e;
                            }
                            if (ek != null && k.equals(ek)) {
                                return e;
                            }
                        } else {
                            continue;
                        }
                    } else if (U.compareAndSwapInt(this, LOCKSTATE, s, s + 4)) {
                        try {
                            TreeNode<K, V> r = this.root;
                            Node<K, V> p = r == null ? null : r.findTreeNode(h, k, null);
                            do {
                                unsafe2 = U;
                                j2 = LOCKSTATE;
                                ls2 = this.lockState;
                            } while (!unsafe2.compareAndSwapInt(this, j2, ls2, ls2 - 4));
                            if (ls2 == 6 && (w2 = this.waiter) != null) {
                                LockSupport.unpark(w2);
                            }
                            Node<K, V> e2 = p;
                            return e2;
                        } catch (Throwable th) {
                            do {
                                unsafe = U;
                                j = LOCKSTATE;
                                ls = this.lockState;
                            } while (!unsafe.compareAndSwapInt(this, j, ls, ls - 4));
                            if (ls == 6 && (w = this.waiter) != null) {
                                LockSupport.unpark(w);
                            }
                            throw th;
                        }
                    }
                }
            }
            return null;
        }

        final TreeNode<K, V> putTreeVal(int h, K k, V v) {
            int dir;
            TreeNode<K, V> q;
            Class<?> kc = null;
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
                        if (p.left == null) {
                            dir = 1;
                        } else {
                            TreeNode<K, V> pr = p.right;
                            if (pr == null || (q = pr.findTreeNode(h, k, kc)) == null) {
                                dir = -1;
                            } else {
                                return q;
                            }
                        }
                    }
                }
                TreeNode<K, V> xp = p;
                p = dir < 0 ? p.left : p.right;
                if (p == null) {
                    TreeNode<K, V> f = this.first;
                    TreeNode<K, V> x = new TreeNode<>(h, k, v, f, xp);
                    this.first = x;
                    if (f != null) {
                        f.prev = x;
                    }
                    if (dir < 0) {
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
                if ($assertionsDisabled || checkInvariants(this.root)) {
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
                            xp = x.parent;
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
                        xp = x.parent;
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
                    TreeNode<K, V> root2 = x;
                    return root2;
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
    }

    static class Traverser<K, V> {
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int index;
        Node<K, V> next = null;
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
                e = ConcurrentHashMap.tabAt(t, this.index);
                if (e != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        this.tab = ((ForwardingNode) e).nextTable;
                        e = null;
                    } else {
                        e = e instanceof TreeBin ? ((TreeBin) e).first : null;
                    }
                }
                int i2 = this.index + this.baseSize;
                this.index = i2;
                if (i2 >= n) {
                    int i3 = this.baseIndex + 1;
                    this.baseIndex = i3;
                    this.index = i3;
                }
            }
            this.next = e;
            return e;
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
            return this.key + "=" + this.val;
        }

        @Override
        public boolean equals(Object o) {
            Map.Entry<?, ?> e;
            Object k;
            Object v;
            return (o instanceof Map.Entry) && (k = (e = (Map.Entry) o).getKey()) != null && (v = e.getValue()) != null && (k == this.key || k.equals(this.key)) && (v == this.val || v.equals(this.val));
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

    static abstract class CollectionView<K, V, E> implements Collection<E>, Serializable {
        private static final String oomeMsg = "Required array size too large";
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
                throw new OutOfMemoryError(oomeMsg);
            }
            int n = (int) sz;
            Object[] r = new Object[n];
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= ConcurrentHashMap.MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(oomeMsg);
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
                throw new OutOfMemoryError(oomeMsg);
            }
            int i = (int) jMappingCount;
            Object[] objArr = tArr.length >= i ? tArr : (Object[]) Array.newInstance(tArr.getClass().getComponentType(), i);
            int length = objArr.length;
            int i2 = 0;
            for (E e : this) {
                if (i2 == length) {
                    if (length >= ConcurrentHashMap.MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(oomeMsg);
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
                return i2 != length ? (T[]) Arrays.copyOf(objArr, i2) : (T[]) objArr;
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
            }
            return true;
        }

        @Override
        public final boolean removeAll(Collection<?> c) {
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
        public boolean addAll(Collection<? extends K> c) {
            boolean added = false;
            V v = this.value;
            if (v == null) {
                throw new UnsupportedOperationException();
            }
            for (K e : c) {
                if (this.map.putVal(e, v, true) == null) {
                    added = true;
                }
            }
            return added;
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
            Set<?> c;
            return (o instanceof Set) && ((c = (Set) o) == this || (containsAll(c) && c.containsAll(this)));
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
            return (!(o instanceof Map.Entry) || (k = (e = (Map.Entry) o).getKey()) == null || (r = this.map.get(k)) == null || (v = e.getValue()) == null || (v != r && !v.equals(r))) ? false : true;
        }

        @Override
        public boolean remove(Object o) {
            Map.Entry<?, ?> e;
            Object k;
            Object v;
            return (o instanceof Map.Entry) && (k = (e = (Map.Entry) o).getKey()) != null && (v = e.getValue()) != null && this.map.remove(k, v);
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
            Set<?> c;
            return (o instanceof Set) && ((c = (Set) o) == this || (containsAll(c) && c.containsAll(this)));
        }
    }

    static final class CounterCell {
        volatile long p0;
        volatile long p1;
        volatile long p2;
        volatile long p3;
        volatile long p4;
        volatile long p5;
        volatile long p6;
        volatile long q0;
        volatile long q1;
        volatile long q2;
        volatile long q3;
        volatile long q4;
        volatile long q5;
        volatile long q6;
        volatile long value;

        CounterCell(long x) {
            this.value = x;
        }
    }

    static final class CounterHashCode {
        int code;

        CounterHashCode() {
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

    private final void fullAddCount(long x, CounterHashCode hc, boolean wasUncontended) {
        int h;
        int n;
        int m;
        if (hc == null) {
            hc = new CounterHashCode();
            int s = counterHashCodeGenerator.addAndGet(SEED_INCREMENT);
            h = s == 0 ? 1 : s;
            hc.code = h;
            threadCounterHashCode.set(hc);
        } else {
            h = hc.code;
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
                                    break;
                                }
                            } finally {
                            }
                        }
                    }
                    collide = false;
                    int h2 = h ^ (h << 13);
                    int h3 = h2 ^ (h2 >>> 17);
                    h = h3 ^ (h3 << 5);
                } else {
                    if (!wasUncontended) {
                        wasUncontended = true;
                    } else {
                        Unsafe unsafe = U;
                        long j2 = CELLVALUE;
                        long v = a.value;
                        if (unsafe.compareAndSwapLong(a, j2, v, v + x)) {
                            break;
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
                    int h22 = h ^ (h << 13);
                    int h32 = h22 ^ (h22 >>> 17);
                    h = h32 ^ (h32 << 5);
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
                    this.cellsBusy = 0;
                    if (init) {
                        break;
                    }
                } finally {
                }
            } else {
                Unsafe unsafe2 = U;
                long j3 = BASECOUNT;
                long v2 = this.baseCount;
                if (unsafe2.compareAndSwapLong(this, j3, v2, v2 + x)) {
                    break;
                }
            }
        }
        hc.code = h;
    }
}
