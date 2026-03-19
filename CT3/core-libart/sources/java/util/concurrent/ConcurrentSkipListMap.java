package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import sun.misc.Unsafe;

public class ConcurrentSkipListMap<K, V> extends AbstractMap<K, V> implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable {
    private static final int EQ = 1;
    private static final int GT = 0;
    private static final long HEAD;
    private static final int LT = 2;
    private static final long serialVersionUID = -8627078645895051609L;
    final Comparator<? super K> comparator;
    private transient ConcurrentNavigableMap<K, V> descendingMap;
    private transient EntrySet<K, V> entrySet;
    private volatile transient HeadIndex<K, V> head;
    private transient KeySet<K, V> keySet;
    private transient Values<K, V> values;
    static final Object BASE_HEADER = new Object();
    private static final Unsafe U = Unsafe.getUnsafe();

    static {
        try {
            HEAD = U.objectFieldOffset(ConcurrentSkipListMap.class.getDeclaredField("head"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private void initialize() {
        this.keySet = null;
        this.entrySet = null;
        this.values = null;
        this.descendingMap = null;
        this.head = new HeadIndex<>(new Node(null, BASE_HEADER, null), null, null, 1);
    }

    private boolean casHead(HeadIndex<K, V> cmp, HeadIndex<K, V> val) {
        return U.compareAndSwapObject(this, HEAD, cmp, val);
    }

    static final class Node<K, V> {
        private static final long NEXT;
        private static final Unsafe U = Unsafe.getUnsafe();
        private static final long VALUE;
        final K key;
        volatile Node<K, V> next;
        volatile Object value;

        Node(K key, Object value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        Node(Node<K, V> next) {
            this.key = null;
            this.value = this;
            this.next = next;
        }

        boolean casValue(Object cmp, Object val) {
            return U.compareAndSwapObject(this, VALUE, cmp, val);
        }

        boolean casNext(Node<K, V> cmp, Node<K, V> val) {
            return U.compareAndSwapObject(this, NEXT, cmp, val);
        }

        boolean isMarker() {
            return this.value == this;
        }

        boolean isBaseHeader() {
            return this.value == ConcurrentSkipListMap.BASE_HEADER;
        }

        boolean appendMarker(Node<K, V> f) {
            return casNext(f, new Node<>(f));
        }

        void helpDelete(Node<K, V> b, Node<K, V> f) {
            if (f != this.next || this != b.next) {
                return;
            }
            if (f == null || f.value != f) {
                casNext(f, new Node<>(f));
            } else {
                b.casNext(this, f.next);
            }
        }

        V getValidValue() {
            V v = (V) this.value;
            if (v == this || v == ConcurrentSkipListMap.BASE_HEADER) {
                return null;
            }
            return v;
        }

        AbstractMap.SimpleImmutableEntry<K, V> createSnapshot() {
            Object v = this.value;
            if (v == null || v == this || v == ConcurrentSkipListMap.BASE_HEADER) {
                return null;
            }
            return new AbstractMap.SimpleImmutableEntry<>(this.key, v);
        }

        static {
            try {
                VALUE = U.objectFieldOffset(Node.class.getDeclaredField("value"));
                NEXT = U.objectFieldOffset(Node.class.getDeclaredField("next"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static class Index<K, V> {
        private static final long RIGHT;
        private static final Unsafe U = Unsafe.getUnsafe();
        final Index<K, V> down;
        final Node<K, V> node;
        volatile Index<K, V> right;

        Index(Node<K, V> node, Index<K, V> down, Index<K, V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }

        final boolean casRight(Index<K, V> cmp, Index<K, V> val) {
            return U.compareAndSwapObject(this, RIGHT, cmp, val);
        }

        final boolean indexesDeletedNode() {
            return this.node.value == null;
        }

        final boolean link(Index<K, V> succ, Index<K, V> newSucc) {
            Node<K, V> n = this.node;
            newSucc.right = succ;
            if (n.value != null) {
                return casRight(succ, newSucc);
            }
            return false;
        }

        final boolean unlink(Index<K, V> succ) {
            if (this.node.value != null) {
                return casRight(succ, succ.right);
            }
            return false;
        }

        static {
            try {
                RIGHT = U.objectFieldOffset(Index.class.getDeclaredField("right"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static final class HeadIndex<K, V> extends Index<K, V> {
        final int level;

        HeadIndex(Node<K, V> node, Index<K, V> down, Index<K, V> right, int level) {
            super(node, down, right);
            this.level = level;
        }
    }

    static final int cpr(Comparator c, Object x, Object y) {
        return c != null ? c.compare(x, y) : ((Comparable) x).compareTo(y);
    }

    private Node<K, V> findPredecessor(Object key, Comparator<? super K> cmp) {
        if (key == null) {
            throw new NullPointerException();
        }
        while (true) {
            Index<K, V> q = this.head;
            Index<K, V> r = q.right;
            while (true) {
                if (r != null) {
                    Node<K, V> n = r.node;
                    K k = n.key;
                    if (n.value == null) {
                        if (q.unlink(r)) {
                            r = q.right;
                        }
                    } else if (cpr(cmp, key, k) > 0) {
                        q = r;
                        r = r.right;
                    }
                }
                Index<K, V> d = q.down;
                if (d == null) {
                    return q.node;
                }
                q = d;
                r = d.right;
            }
        }
    }

    private Node<K, V> findNode(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> cmp = this.comparator;
        loop0: while (true) {
            Node<K, V> b = findPredecessor(key, cmp);
            Node<K, V> n = b.next;
            while (n != null) {
                Node<K, V> f = n.next;
                if (n == b.next) {
                    Object v = n.value;
                    if (v == null) {
                        n.helpDelete(b, f);
                    } else if (b.value != null && v != n) {
                        int c = cpr(cmp, key, n.key);
                        if (c == 0) {
                            return n;
                        }
                        if (c < 0) {
                            break loop0;
                        }
                        b = n;
                        n = f;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            break loop0;
        }
        return null;
    }

    private V doGet(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> comparator = this.comparator;
        loop0: while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(obj, comparator);
            Node<K, V> node = nodeFindPredecessor.next;
            while (node != null) {
                Node<K, V> node2 = node.next;
                if (node == nodeFindPredecessor.next) {
                    V v = (V) node.value;
                    if (v == null) {
                        node.helpDelete(nodeFindPredecessor, node2);
                    } else if (nodeFindPredecessor.value != null && v != node) {
                        int iCpr = cpr(comparator, obj, node.key);
                        if (iCpr == 0) {
                            return v;
                        }
                        if (iCpr < 0) {
                            break loop0;
                        }
                        nodeFindPredecessor = node;
                        node = node2;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            break loop0;
        }
        return null;
    }

    private V doPut(K k, V v, boolean z) {
        if (k == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> comparator = this.comparator;
        while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(k, comparator);
            Node<K, V> node = nodeFindPredecessor.next;
            while (true) {
                if (node == null) {
                    break;
                }
                Node<K, V> node2 = node.next;
                if (node != nodeFindPredecessor.next) {
                    break;
                }
                V v2 = (V) node.value;
                if (v2 == null) {
                    node.helpDelete(nodeFindPredecessor, node2);
                    break;
                }
                if (nodeFindPredecessor.value == null || v2 == node) {
                    break;
                }
                int iCpr = cpr(comparator, k, node.key);
                if (iCpr > 0) {
                    nodeFindPredecessor = node;
                    node = node2;
                } else if (iCpr == 0) {
                    if (z || node.casValue(v2, v)) {
                        break;
                    }
                }
            }
        }
    }

    final V doRemove(Object obj, Object obj2) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> comparator = this.comparator;
        loop0: while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(obj, comparator);
            Node<K, V> node = nodeFindPredecessor.next;
            while (true) {
                if (node != null) {
                    Node<K, V> node2 = node.next;
                    if (node == nodeFindPredecessor.next) {
                        V v = (V) node.value;
                        if (v == null) {
                            node.helpDelete(nodeFindPredecessor, node2);
                            break;
                        }
                        if (nodeFindPredecessor.value != null && v != node) {
                            int iCpr = cpr(comparator, obj, node.key);
                            if (iCpr >= 0) {
                                if (iCpr > 0) {
                                    nodeFindPredecessor = node;
                                    node = node2;
                                } else {
                                    if (obj2 != null && !obj2.equals(v)) {
                                        break;
                                    }
                                    if (node.casValue(v, null)) {
                                        if (!node.appendMarker(node2) || !nodeFindPredecessor.casNext(node, node2)) {
                                            findNode(obj);
                                        } else {
                                            findPredecessor(obj, comparator);
                                            if (this.head.right == null) {
                                                tryReduceLevel();
                                            }
                                        }
                                        return v;
                                    }
                                }
                            } else {
                                break loop0;
                            }
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                } else {
                    break loop0;
                }
            }
        }
    }

    private void tryReduceLevel() {
        HeadIndex<K, V> d;
        HeadIndex<K, V> e;
        HeadIndex<K, V> h = this.head;
        if (h.level <= 3 || (d = (HeadIndex) h.down) == null || (e = (HeadIndex) d.down) == null || e.right != null || d.right != null || h.right != null || !casHead(h, d) || h.right == null) {
            return;
        }
        casHead(d, h);
    }

    final Node<K, V> findFirst() {
        while (true) {
            Node<K, V> b = this.head.node;
            Node<K, V> n = b.next;
            if (n == null) {
                return null;
            }
            if (n.value != null) {
                return n;
            }
            n.helpDelete(b, n.next);
        }
    }

    private Map.Entry<K, V> doRemoveFirstEntry() {
        while (true) {
            Node<K, V> b = this.head.node;
            Node<K, V> n = b.next;
            if (n == null) {
                return null;
            }
            Node<K, V> f = n.next;
            if (n == b.next) {
                Object v = n.value;
                if (v == null) {
                    n.helpDelete(b, f);
                } else if (n.casValue(v, null)) {
                    if (!n.appendMarker(f) || !b.casNext(n, f)) {
                        findFirst();
                    }
                    clearIndexToFirst();
                    return new AbstractMap.SimpleImmutableEntry(n.key, v);
                }
            }
        }
    }

    private void clearIndexToFirst() {
        loop0: while (true) {
            Index<K, V> q = this.head;
            do {
                Index<K, V> r = q.right;
                if (r == null || !r.indexesDeletedNode() || q.unlink(r)) {
                    q = q.down;
                }
            } while (q != null);
            if (this.head.right != null) {
                tryReduceLevel();
                return;
            }
            return;
        }
        if (this.head.right != null) {
        }
    }

    private Map.Entry<K, V> doRemoveLastEntry() {
        while (true) {
            Node<K, V> b = findPredecessorOfLast();
            Node<K, V> n = b.next;
            if (n != null) {
                while (true) {
                    Node<K, V> f = n.next;
                    if (n != b.next) {
                        break;
                    }
                    Object v = n.value;
                    if (v == null) {
                        n.helpDelete(b, f);
                        break;
                    }
                    if (b.value == null || v == n) {
                        break;
                    }
                    if (f != null) {
                        b = n;
                        n = f;
                    } else if (n.casValue(v, null)) {
                        K key = n.key;
                        if (!n.appendMarker(f) || !b.casNext(n, f)) {
                            findNode(key);
                        } else {
                            findPredecessor(key, this.comparator);
                            if (this.head.right == null) {
                                tryReduceLevel();
                            }
                        }
                        return new AbstractMap.SimpleImmutableEntry(key, v);
                    }
                }
            } else if (b.isBaseHeader()) {
                return null;
            }
        }
    }

    final Node<K, V> findLast() {
        Node<K, V> b;
        Index<K, V> q = this.head;
        loop0: while (true) {
            Index<K, V> r = q.right;
            if (r != null) {
                if (r.indexesDeletedNode()) {
                    q.unlink(r);
                    q = this.head;
                } else {
                    q = r;
                }
            } else {
                Index<K, V> d = q.down;
                if (d != null) {
                    q = d;
                } else {
                    b = q.node;
                    Node<K, V> n = b.next;
                    while (n != null) {
                        Node<K, V> f = n.next;
                        if (n == b.next) {
                            Object v = n.value;
                            if (v == null) {
                                n.helpDelete(b, f);
                            } else if (b.value != null && v != n) {
                                b = n;
                                n = f;
                            }
                        }
                        q = this.head;
                    }
                    break loop0;
                }
            }
        }
        if (b.isBaseHeader()) {
            return null;
        }
        return b;
    }

    private Node<K, V> findPredecessorOfLast() {
        Index<K, V> r;
        while (true) {
            Index<K, V> q = this.head;
            while (true) {
                r = q.right;
                if (r != null) {
                    if (r.indexesDeletedNode()) {
                        break;
                    }
                    if (r.node.next != null) {
                        q = r;
                    }
                }
                Index<K, V> d = q.down;
                if (d != null) {
                    q = d;
                } else {
                    return q.node;
                }
            }
            q.unlink(r);
        }
    }

    final Node<K, V> findNear(K key, int rel, Comparator<? super K> cmp) {
        if (key == null) {
            throw new NullPointerException();
        }
        loop0: while (true) {
            Node<K, V> b = findPredecessor(key, cmp);
            Node<K, V> n = b.next;
            while (n != null) {
                Node<K, V> f = n.next;
                if (n == b.next) {
                    Object v = n.value;
                    if (v == null) {
                        n.helpDelete(b, f);
                    } else if (b.value != null && v != n) {
                        int c = cpr(cmp, key, n.key);
                        if ((c == 0 && (rel & 1) != 0) || (c < 0 && (rel & 2) == 0)) {
                            break loop0;
                        }
                        if (c <= 0 && (rel & 2) != 0) {
                            if (b.isBaseHeader()) {
                                return null;
                            }
                            return b;
                        }
                        b = n;
                        n = f;
                    } else {
                        break;
                    }
                }
            }
            if ((rel & 2) == 0 || b.isBaseHeader()) {
                return null;
            }
            return b;
        }
    }

    final AbstractMap.SimpleImmutableEntry<K, V> getNear(K key, int rel) {
        AbstractMap.SimpleImmutableEntry<K, V> e;
        Comparator<? super K> cmp = this.comparator;
        do {
            Node<K, V> n = findNear(key, rel, cmp);
            if (n == null) {
                return null;
            }
            e = n.createSnapshot();
        } while (e == null);
        return e;
    }

    public ConcurrentSkipListMap() {
        this.comparator = null;
        initialize();
    }

    public ConcurrentSkipListMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
        initialize();
    }

    public ConcurrentSkipListMap(Map<? extends K, ? extends V> m) {
        this.comparator = null;
        initialize();
        putAll(m);
    }

    public ConcurrentSkipListMap(SortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        initialize();
        buildFromSorted(m);
    }

    @Override
    public ConcurrentSkipListMap<K, V> clone() {
        try {
            ConcurrentSkipListMap<K, V> clone = (ConcurrentSkipListMap) super.clone();
            clone.initialize();
            clone.buildFromSorted(this);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    private void buildFromSorted(SortedMap<K, ? extends V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        HeadIndex<K, V> h = this.head;
        Node<K, V> basepred = h.node;
        ArrayList<Index<K, V>> preds = new ArrayList<>();
        for (int i = 0; i <= h.level; i++) {
            preds.add(null);
        }
        Index<K, V> q = h;
        for (int i2 = h.level; i2 > 0; i2--) {
            preds.set(i2, q);
            q = q.down;
        }
        for (Map.Entry<K, ? extends V> entry : map.entrySet()) {
            int rnd = ThreadLocalRandom.current().nextInt();
            int j = 0;
            if (((-2147483647) & rnd) == 0) {
                do {
                    j++;
                    rnd >>>= 1;
                } while ((rnd & 1) != 0);
                if (j > h.level) {
                    j = h.level + 1;
                }
            }
            K k = entry.getKey();
            V v = entry.getValue();
            if (k == null || v == null) {
                throw new NullPointerException();
            }
            Node<K, V> z = new Node<>(k, v, null);
            basepred.next = z;
            basepred = z;
            if (j > 0) {
                int i3 = 1;
                Index<K, V> idx = null;
                HeadIndex<K, V> h2 = h;
                while (i3 <= j) {
                    Index<K, V> idx2 = new Index<>(z, idx, null);
                    HeadIndex<K, V> h3 = i3 > h2.level ? new HeadIndex<>(h2.node, h2, idx2, i3) : h2;
                    if (i3 < preds.size()) {
                        preds.get(i3).right = idx2;
                        preds.set(i3, idx2);
                    } else {
                        preds.add(idx2);
                    }
                    i3++;
                    idx = idx2;
                    h2 = h3;
                }
                h = h2;
            }
        }
        this.head = h;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null) {
                s.writeObject(n.key);
                s.writeObject(v);
            }
        }
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        initialize();
        HeadIndex<K, V> h = this.head;
        Node<K, V> basepred = h.node;
        ArrayList<Index<K, V>> preds = new ArrayList<>();
        for (int i = 0; i <= h.level; i++) {
            preds.add(null);
        }
        Index<K, V> q = h;
        for (int i2 = h.level; i2 > 0; i2--) {
            preds.set(i2, q);
            q = q.down;
        }
        while (true) {
            Object k = s.readObject();
            if (k != null) {
                Object v = s.readObject();
                if (v == null) {
                    throw new NullPointerException();
                }
                int rnd = ThreadLocalRandom.current().nextInt();
                int j = 0;
                if (((-2147483647) & rnd) == 0) {
                    do {
                        j++;
                        rnd >>>= 1;
                    } while ((rnd & 1) != 0);
                    if (j > h.level) {
                        j = h.level + 1;
                    }
                }
                Node<K, V> z = new Node<>(k, v, null);
                basepred.next = z;
                basepred = z;
                if (j > 0) {
                    int i3 = 1;
                    Index<K, V> idx = null;
                    HeadIndex<K, V> h2 = h;
                    while (i3 <= j) {
                        Index<K, V> idx2 = new Index<>(z, idx, null);
                        HeadIndex<K, V> h3 = i3 > h2.level ? new HeadIndex<>(h2.node, h2, idx2, i3) : h2;
                        if (i3 < preds.size()) {
                            preds.get(i3).right = idx2;
                            preds.set(i3, idx2);
                        } else {
                            preds.add(idx2);
                        }
                        i3++;
                        idx = idx2;
                        h2 = h3;
                    }
                    h = h2;
                }
            } else {
                this.head = h;
                return;
            }
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return doGet(key) != null;
    }

    @Override
    public V get(Object key) {
        return doGet(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v = doGet(key);
        return v == null ? defaultValue : v;
    }

    @Override
    public V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return doPut(key, value, false);
    }

    @Override
    public V remove(Object key) {
        return doRemove(key, null);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null && value.equals(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        long count = 0;
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            if (n.getValidValue() != null) {
                count++;
            }
        }
        if (count >= 2147483647L) {
            return Integer.MAX_VALUE;
        }
        return (int) count;
    }

    @Override
    public boolean isEmpty() {
        return findFirst() == null;
    }

    @Override
    public void clear() {
        initialize();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V r;
        if (key == null || mappingFunction == null) {
            throw new NullPointerException();
        }
        V v = doGet(key);
        if (v == null && (r = mappingFunction.apply(key)) != null) {
            V p = doPut(key, r, true);
            return p == null ? r : p;
        }
        return v;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> n = findNode(key);
            if (n == null) {
                break;
            }
            Object v = n.value;
            if (v != null) {
                V r = remappingFunction.apply(key, v);
                if (r != null) {
                    if (n.casValue(v, r)) {
                        return r;
                    }
                } else if (doRemove(key, v) != null) {
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> n = findNode(key);
            if (n == null) {
                V r = remappingFunction.apply(key, null);
                if (r == null) {
                    break;
                }
                if (doPut(key, r, true) == null) {
                    return r;
                }
            } else {
                Object v = n.value;
                if (v == null) {
                    continue;
                } else {
                    V r2 = remappingFunction.apply(key, v);
                    if (r2 != null) {
                        if (n.casValue(v, r2)) {
                            return r2;
                        }
                    } else if (doRemove(key, v) != null) {
                        break;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> n = findNode(key);
            if (n == null) {
                if (doPut(key, value, true) == null) {
                    return value;
                }
            } else {
                Object v = n.value;
                if (v == null) {
                    continue;
                } else {
                    V r = remappingFunction.apply(v, value);
                    if (r != null) {
                        if (n.casValue(v, r)) {
                            return r;
                        }
                    } else if (doRemove(key, v) != null) {
                        return null;
                    }
                }
            }
        }
    }

    @Override
    public NavigableSet<K> keySet() {
        KeySet<K, V> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySet<K, V> ks2 = new KeySet<>(this);
        this.keySet = ks2;
        return ks2;
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        KeySet<K, V> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySet<K, V> ks2 = new KeySet<>(this);
        this.keySet = ks2;
        return ks2;
    }

    @Override
    public Collection<V> values() {
        Values<K, V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        Values<K, V> vs2 = new Values<>(this);
        this.values = vs2;
        return vs2;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        EntrySet<K, V> es = this.entrySet;
        if (es != null) {
            return es;
        }
        EntrySet<K, V> es2 = new EntrySet<>(this);
        this.entrySet = es2;
        return es2;
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        ConcurrentNavigableMap<K, V> dm = this.descendingMap;
        if (dm != null) {
            return dm;
        }
        ConcurrentNavigableMap<K, V> dm2 = new SubMap<>(this, null, false, null, false, true);
        this.descendingMap = dm2;
        return dm2;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map<?, ?> m = (Map) o;
        try {
            for (Map.Entry<K, V> e : entrySet()) {
                if (!e.getValue().equals(m.get(e.getKey()))) {
                    return false;
                }
            }
            Iterator e$iterator = m.entrySet().iterator();
            while (e$iterator.hasNext()) {
                Map.Entry<?, ?> e2 = (Map.Entry) e$iterator.next();
                Object k = e2.getKey();
                Object v = e2.getValue();
                if (k == null || v == null || !v.equals(get(k))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e3) {
            return false;
        } catch (NullPointerException e4) {
            return false;
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return doPut(key, value, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        return (value == null || doRemove(key, value) == null) ? false : true;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> n = findNode(key);
            if (n == null) {
                return false;
            }
            Object v = n.value;
            if (v != null) {
                if (!oldValue.equals(v)) {
                    return false;
                }
                if (n.casValue(v, newValue)) {
                    return true;
                }
            }
        }
    }

    @Override
    public V replace(K k, V v) {
        if (k == null || v == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> nodeFindNode = findNode(k);
            if (nodeFindNode == null) {
                return null;
            }
            V v2 = (V) nodeFindNode.value;
            if (v2 != null && nodeFindNode.casValue(v2, v)) {
                return v2;
            }
        }
    }

    @Override
    public Comparator<? super K> comparator() {
        return this.comparator;
    }

    @Override
    public K firstKey() {
        Node<K, V> n = findFirst();
        if (n == null) {
            throw new NoSuchElementException();
        }
        return n.key;
    }

    @Override
    public K lastKey() {
        Node<K, V> n = findLast();
        if (n == null) {
            throw new NoSuchElementException();
        }
        return n.key;
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        if (fromKey == null || toKey == null) {
            throw new NullPointerException();
        }
        return new SubMap(this, fromKey, fromInclusive, toKey, toInclusive, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        if (toKey == null) {
            throw new NullPointerException();
        }
        return new SubMap(this, null, false, toKey, inclusive, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        if (fromKey == null) {
            throw new NullPointerException();
        }
        return new SubMap(this, fromKey, inclusive, null, false, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
        return subMap((Object) fromKey, true, (Object) toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey) {
        return headMap((Object) toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey) {
        return tailMap((Object) fromKey, true);
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return getNear(key, 2);
    }

    @Override
    public K lowerKey(K key) {
        Node<K, V> n = findNear(key, 2, this.comparator);
        if (n == null) {
            return null;
        }
        return n.key;
    }

    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return getNear(key, 3);
    }

    @Override
    public K floorKey(K key) {
        Node<K, V> n = findNear(key, 3, this.comparator);
        if (n == null) {
            return null;
        }
        return n.key;
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return getNear(key, 1);
    }

    @Override
    public K ceilingKey(K key) {
        Node<K, V> n = findNear(key, 1, this.comparator);
        if (n == null) {
            return null;
        }
        return n.key;
    }

    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return getNear(key, 0);
    }

    @Override
    public K higherKey(K key) {
        Node<K, V> n = findNear(key, 0, this.comparator);
        if (n == null) {
            return null;
        }
        return n.key;
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        AbstractMap.SimpleImmutableEntry<K, V> e;
        do {
            Node<K, V> n = findFirst();
            if (n == null) {
                return null;
            }
            e = n.createSnapshot();
        } while (e == null);
        return e;
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        AbstractMap.SimpleImmutableEntry<K, V> e;
        do {
            Node<K, V> n = findLast();
            if (n == null) {
                return null;
            }
            e = n.createSnapshot();
        } while (e == null);
        return e;
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return doRemoveFirstEntry();
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return doRemoveLastEntry();
    }

    abstract class Iter<T> implements Iterator<T> {
        Node<K, V> lastReturned;
        Node<K, V> next;
        V nextValue;

        Iter() {
            while (true) {
                Node<K, V> nodeFindFirst = ConcurrentSkipListMap.this.findFirst();
                this.next = nodeFindFirst;
                if (nodeFindFirst == null) {
                    return;
                }
                V v = (V) this.next.value;
                if (v != null && v != this.next) {
                    this.nextValue = v;
                    return;
                }
            }
        }

        @Override
        public final boolean hasNext() {
            return this.next != null;
        }

        final void advance() {
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            this.lastReturned = this.next;
            while (true) {
                Node<K, V> node = this.next.next;
                this.next = node;
                if (node == null) {
                    return;
                }
                V v = (V) this.next.value;
                if (v != null && v != this.next) {
                    this.nextValue = v;
                    return;
                }
            }
        }

        @Override
        public void remove() {
            Node<K, V> l = this.lastReturned;
            if (l == null) {
                throw new IllegalStateException();
            }
            ConcurrentSkipListMap.this.remove(l.key);
            this.lastReturned = null;
        }
    }

    final class ValueIterator extends ConcurrentSkipListMap<K, V>.Iter<V> {
        ValueIterator() {
            super();
        }

        @Override
        public V next() {
            V v = this.nextValue;
            advance();
            return v;
        }
    }

    final class KeyIterator extends ConcurrentSkipListMap<K, V>.Iter<K> {
        KeyIterator() {
            super();
        }

        @Override
        public K next() {
            Node<K, V> n = this.next;
            advance();
            return n.key;
        }
    }

    final class EntryIterator extends ConcurrentSkipListMap<K, V>.Iter<Map.Entry<K, V>> {
        EntryIterator() {
            super();
        }

        @Override
        public Map.Entry<K, V> next() {
            Node<K, V> n = this.next;
            V v = this.nextValue;
            advance();
            return new AbstractMap.SimpleImmutableEntry(n.key, v);
        }
    }

    static final <E> List<E> toList(Collection<E> collection) {
        ArrayList arrayList = new ArrayList();
        Iterator<T> it = collection.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next());
        }
        return arrayList;
    }

    static final class KeySet<K, V> extends AbstractSet<K> implements NavigableSet<K> {
        final ConcurrentNavigableMap<K, V> m;

        KeySet(ConcurrentNavigableMap<K, V> map) {
            this.m = map;
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return this.m.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return this.m.remove(o) != null;
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public K lower(K e) {
            return this.m.lowerKey(e);
        }

        @Override
        public K floor(K e) {
            return this.m.floorKey(e);
        }

        @Override
        public K ceiling(K e) {
            return this.m.ceilingKey(e);
        }

        @Override
        public K higher(K e) {
            return this.m.higherKey(e);
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.m.comparator();
        }

        @Override
        public K first() {
            return (K) this.m.firstKey();
        }

        @Override
        public K last() {
            return (K) this.m.lastKey();
        }

        @Override
        public K pollFirst() {
            Map.Entry<K, V> e = this.m.pollFirstEntry();
            if (e == null) {
                return null;
            }
            return e.getKey();
        }

        @Override
        public K pollLast() {
            Map.Entry<K, V> e = this.m.pollLastEntry();
            if (e == null) {
                return null;
            }
            return e.getKey();
        }

        @Override
        public Iterator<K> iterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                ConcurrentSkipListMap concurrentSkipListMap = (ConcurrentSkipListMap) this.m;
                concurrentSkipListMap.getClass();
                return new KeyIterator();
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            return new SubMap.SubMapKeyIterator();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Set)) {
                return false;
            }
            Collection<?> c = (Collection) o;
            try {
                if (containsAll(c)) {
                    return c.containsAll(this);
                }
                return false;
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e2) {
                return false;
            }
        }

        @Override
        public Object[] toArray() {
            return ConcurrentSkipListMap.toList(this).toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) ConcurrentSkipListMap.toList(this).toArray(tArr);
        }

        @Override
        public Iterator<K> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
            return new KeySet(this.m.subMap((Object) fromElement, fromInclusive, (Object) toElement, toInclusive));
        }

        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            return new KeySet(this.m.headMap((Object) toElement, inclusive));
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            return new KeySet(this.m.tailMap((Object) fromElement, inclusive));
        }

        @Override
        public NavigableSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public NavigableSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return new KeySet(this.m.descendingMap());
        }

        @Override
        public Spliterator<K> spliterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                return ((ConcurrentSkipListMap) this.m).keySpliterator();
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            return new SubMap.SubMapKeyIterator();
        }
    }

    static final class Values<K, V> extends AbstractCollection<V> {
        final ConcurrentNavigableMap<K, V> m;

        Values(ConcurrentNavigableMap<K, V> map) {
            this.m = map;
        }

        @Override
        public Iterator<V> iterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                ConcurrentSkipListMap concurrentSkipListMap = (ConcurrentSkipListMap) this.m;
                concurrentSkipListMap.getClass();
                return new ValueIterator();
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            return new SubMap.SubMapValueIterator();
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return this.m.containsValue(o);
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public Object[] toArray() {
            return ConcurrentSkipListMap.toList(this).toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) ConcurrentSkipListMap.toList(this).toArray(tArr);
        }

        @Override
        public Spliterator<V> spliterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                return ((ConcurrentSkipListMap) this.m).valueSpliterator();
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            return new SubMap.SubMapValueIterator();
        }

        @Override
        public boolean removeIf(Predicate<? super V> predicate) {
            if (predicate == null) {
                throw new NullPointerException();
            }
            if (this.m instanceof ConcurrentSkipListMap) {
                return ((ConcurrentSkipListMap) this.m).removeValueIf(predicate);
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            SubMap.SubMapEntryIterator subMapEntryIterator = new SubMap.SubMapEntryIterator();
            boolean z = false;
            while (subMapEntryIterator.hasNext()) {
                Map.Entry<K, V> next = subMapEntryIterator.next();
                V value = next.getValue();
                if (predicate.test(value) && this.m.remove(next.getKey(), value)) {
                    z = true;
                }
            }
            return z;
        }
    }

    static final class EntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {
        final ConcurrentNavigableMap<K, V> m;

        EntrySet(ConcurrentNavigableMap<K, V> map) {
            this.m = map;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                ConcurrentSkipListMap concurrentSkipListMap = (ConcurrentSkipListMap) this.m;
                concurrentSkipListMap.getClass();
                return new EntryIterator();
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            return new SubMap.SubMapEntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            Object obj = this.m.get(e.getKey());
            if (obj != null) {
                return obj.equals(e.getValue());
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            return this.m.remove(e.getKey(), e.getValue());
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Set)) {
                return false;
            }
            Collection<?> c = (Collection) o;
            try {
                if (containsAll(c)) {
                    return c.containsAll(this);
                }
                return false;
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e2) {
                return false;
            }
        }

        @Override
        public Object[] toArray() {
            return ConcurrentSkipListMap.toList(this).toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) ConcurrentSkipListMap.toList(this).toArray(tArr);
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                return ((ConcurrentSkipListMap) this.m).entrySpliterator();
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            return new SubMap.SubMapEntryIterator();
        }

        @Override
        public boolean removeIf(Predicate<? super Map.Entry<K, V>> filter) {
            if (filter == null) {
                throw new NullPointerException();
            }
            if (this.m instanceof ConcurrentSkipListMap) {
                return ((ConcurrentSkipListMap) this.m).removeEntryIf(filter);
            }
            SubMap subMap = (SubMap) this.m;
            subMap.getClass();
            Iterator<Map.Entry<K, V>> it = new SubMap.SubMapEntryIterator();
            boolean removed = false;
            while (it.hasNext()) {
                Map.Entry<K, V> e = it.next();
                if (filter.test(e) && this.m.remove(e.getKey(), e.getValue())) {
                    removed = true;
                }
            }
            return removed;
        }
    }

    static final class SubMap<K, V> extends AbstractMap<K, V> implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable {
        private static final long serialVersionUID = -7647078645895051609L;
        private transient Set<Map.Entry<K, V>> entrySetView;
        private final K hi;
        private final boolean hiInclusive;
        final boolean isDescending;
        private transient KeySet<K, V> keySetView;
        private final K lo;
        private final boolean loInclusive;
        final ConcurrentSkipListMap<K, V> m;
        private transient Collection<V> valuesView;

        SubMap(ConcurrentSkipListMap<K, V> map, K fromKey, boolean fromInclusive, K toKey, boolean toInclusive, boolean isDescending) {
            Comparator<? super K> cmp = map.comparator;
            if (fromKey != null && toKey != null && ConcurrentSkipListMap.cpr(cmp, fromKey, toKey) > 0) {
                throw new IllegalArgumentException("inconsistent range");
            }
            this.m = map;
            this.lo = fromKey;
            this.hi = toKey;
            this.loInclusive = fromInclusive;
            this.hiInclusive = toInclusive;
            this.isDescending = isDescending;
        }

        boolean tooLow(Object key, Comparator<? super K> cmp) {
            if (this.lo == null) {
                return false;
            }
            int c = ConcurrentSkipListMap.cpr(cmp, key, this.lo);
            if (c >= 0) {
                return c == 0 && !this.loInclusive;
            }
            return true;
        }

        boolean tooHigh(Object key, Comparator<? super K> cmp) {
            if (this.hi == null) {
                return false;
            }
            int c = ConcurrentSkipListMap.cpr(cmp, key, this.hi);
            if (c <= 0) {
                return c == 0 && !this.hiInclusive;
            }
            return true;
        }

        boolean inBounds(Object key, Comparator<? super K> cmp) {
            return (tooLow(key, cmp) || tooHigh(key, cmp)) ? false : true;
        }

        void checkKeyBounds(K key, Comparator<? super K> cmp) {
            if (key == null) {
                throw new NullPointerException();
            }
            if (inBounds(key, cmp)) {
            } else {
                throw new IllegalArgumentException("key out of range");
            }
        }

        boolean isBeforeEnd(Node<K, V> n, Comparator<? super K> cmp) {
            K k;
            if (n == null) {
                return false;
            }
            if (this.hi == null || (k = n.key) == null) {
                return true;
            }
            int c = ConcurrentSkipListMap.cpr(cmp, k, this.hi);
            return c <= 0 && (c != 0 || this.hiInclusive);
        }

        Node<K, V> loNode(Comparator<? super K> cmp) {
            if (this.lo == null) {
                return this.m.findFirst();
            }
            if (this.loInclusive) {
                return this.m.findNear(this.lo, 1, cmp);
            }
            return this.m.findNear(this.lo, 0, cmp);
        }

        Node<K, V> hiNode(Comparator<? super K> cmp) {
            if (this.hi == null) {
                return this.m.findLast();
            }
            if (this.hiInclusive) {
                return this.m.findNear(this.hi, 3, cmp);
            }
            return this.m.findNear(this.hi, 2, cmp);
        }

        K lowestKey() {
            Comparator<? super K> cmp = this.m.comparator;
            Node<K, V> n = loNode(cmp);
            if (isBeforeEnd(n, cmp)) {
                return n.key;
            }
            throw new NoSuchElementException();
        }

        K highestKey() {
            Comparator<? super K> cmp = this.m.comparator;
            Node<K, V> n = hiNode(cmp);
            if (n != null) {
                K last = n.key;
                if (inBounds(last, cmp)) {
                    return last;
                }
            }
            throw new NoSuchElementException();
        }

        Map.Entry<K, V> lowestEntry() {
            Map.Entry<K, V> e;
            Comparator<? super K> cmp = this.m.comparator;
            do {
                Node<K, V> n = loNode(cmp);
                if (!isBeforeEnd(n, cmp)) {
                    return null;
                }
                e = n.createSnapshot();
            } while (e == null);
            return e;
        }

        Map.Entry<K, V> highestEntry() {
            Map.Entry<K, V> e;
            Comparator<? super K> cmp = this.m.comparator;
            do {
                Node<K, V> n = hiNode(cmp);
                if (n == null || !inBounds(n.key, cmp)) {
                    return null;
                }
                e = n.createSnapshot();
            } while (e == null);
            return e;
        }

        Map.Entry<K, V> removeLowest() {
            K k;
            V v;
            Comparator<? super K> cmp = this.m.comparator;
            do {
                Node<K, V> n = loNode(cmp);
                if (n == null) {
                    return null;
                }
                k = n.key;
                if (!inBounds(k, cmp)) {
                    return null;
                }
                v = this.m.doRemove(k, null);
            } while (v == null);
            return new AbstractMap.SimpleImmutableEntry(k, v);
        }

        Map.Entry<K, V> removeHighest() {
            K k;
            V v;
            Comparator<? super K> cmp = this.m.comparator;
            do {
                Node<K, V> n = hiNode(cmp);
                if (n == null) {
                    return null;
                }
                k = n.key;
                if (!inBounds(k, cmp)) {
                    return null;
                }
                v = this.m.doRemove(k, null);
            } while (v == null);
            return new AbstractMap.SimpleImmutableEntry(k, v);
        }

        Map.Entry<K, V> getNearEntry(K key, int rel) {
            K k;
            V v;
            Comparator<? super K> cmp = this.m.comparator;
            if (this.isDescending) {
                if ((rel & 2) == 0) {
                    rel |= 2;
                } else {
                    rel &= -3;
                }
            }
            if (tooLow(key, cmp)) {
                if ((rel & 2) != 0) {
                    return null;
                }
                return lowestEntry();
            }
            if (tooHigh(key, cmp)) {
                if ((rel & 2) != 0) {
                    return highestEntry();
                }
                return null;
            }
            do {
                Node<K, V> n = this.m.findNear(key, rel, cmp);
                if (n == null || !inBounds(n.key, cmp)) {
                    return null;
                }
                k = n.key;
                v = n.getValidValue();
            } while (v == null);
            return new AbstractMap.SimpleImmutableEntry(k, v);
        }

        K getNearKey(K key, int rel) {
            K k;
            V v;
            Node<K, V> n;
            Comparator<? super K> cmp = this.m.comparator;
            if (this.isDescending) {
                if ((rel & 2) == 0) {
                    rel |= 2;
                } else {
                    rel &= -3;
                }
            }
            if (tooLow(key, cmp)) {
                if ((rel & 2) == 0) {
                    Node<K, V> n2 = loNode(cmp);
                    if (isBeforeEnd(n2, cmp)) {
                        return n2.key;
                    }
                }
                return null;
            }
            if (tooHigh(key, cmp)) {
                if ((rel & 2) != 0 && (n = hiNode(cmp)) != null) {
                    K last = n.key;
                    if (inBounds(last, cmp)) {
                        return last;
                    }
                }
                return null;
            }
            do {
                Node<K, V> n3 = this.m.findNear(key, rel, cmp);
                if (n3 == null || !inBounds(n3.key, cmp)) {
                    return null;
                }
                k = n3.key;
                v = n3.getValidValue();
            } while (v == null);
            return k;
        }

        @Override
        public boolean containsKey(Object key) {
            if (key == null) {
                throw new NullPointerException();
            }
            if (inBounds(key, this.m.comparator)) {
                return this.m.containsKey(key);
            }
            return false;
        }

        @Override
        public V get(Object key) {
            if (key == null) {
                throw new NullPointerException();
            }
            if (inBounds(key, this.m.comparator)) {
                return this.m.get(key);
            }
            return null;
        }

        @Override
        public V put(K key, V value) {
            checkKeyBounds(key, this.m.comparator);
            return this.m.put(key, value);
        }

        @Override
        public V remove(Object key) {
            if (inBounds(key, this.m.comparator)) {
                return this.m.remove(key);
            }
            return null;
        }

        @Override
        public int size() {
            Comparator<? super K> cmp = this.m.comparator;
            long count = 0;
            for (Node<K, V> n = loNode(cmp); isBeforeEnd(n, cmp); n = n.next) {
                if (n.getValidValue() != null) {
                    count++;
                }
            }
            if (count >= 2147483647L) {
                return Integer.MAX_VALUE;
            }
            return (int) count;
        }

        @Override
        public boolean isEmpty() {
            Comparator<? super K> cmp = this.m.comparator;
            return !isBeforeEnd(loNode(cmp), cmp);
        }

        @Override
        public boolean containsValue(Object value) {
            if (value == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> cmp = this.m.comparator;
            for (Node<K, V> n = loNode(cmp); isBeforeEnd(n, cmp); n = n.next) {
                V v = n.getValidValue();
                if (v != null && value.equals(v)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            Comparator<? super K> cmp = this.m.comparator;
            for (Node<K, V> n = loNode(cmp); isBeforeEnd(n, cmp); n = n.next) {
                if (n.getValidValue() != null) {
                    this.m.remove(n.key);
                }
            }
        }

        @Override
        public V putIfAbsent(K key, V value) {
            checkKeyBounds(key, this.m.comparator);
            return this.m.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object key, Object value) {
            if (inBounds(key, this.m.comparator)) {
                return this.m.remove(key, value);
            }
            return false;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            checkKeyBounds(key, this.m.comparator);
            return this.m.replace(key, oldValue, newValue);
        }

        @Override
        public V replace(K key, V value) {
            checkKeyBounds(key, this.m.comparator);
            return this.m.replace(key, value);
        }

        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> cmp = this.m.comparator();
            if (this.isDescending) {
                return Collections.reverseOrder(cmp);
            }
            return cmp;
        }

        SubMap<K, V> newSubMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            Comparator<? super K> cmp = this.m.comparator;
            if (this.isDescending) {
                fromKey = toKey;
                toKey = fromKey;
                fromInclusive = toInclusive;
                toInclusive = fromInclusive;
            }
            if (this.lo != null) {
                if (fromKey == null) {
                    fromKey = this.lo;
                    fromInclusive = this.loInclusive;
                } else {
                    int c = ConcurrentSkipListMap.cpr(cmp, fromKey, this.lo);
                    if (c < 0 || (c == 0 && !this.loInclusive && fromInclusive)) {
                        throw new IllegalArgumentException("key out of range");
                    }
                }
            }
            if (this.hi != null) {
                if (toKey == null) {
                    toKey = this.hi;
                    toInclusive = this.hiInclusive;
                } else {
                    int c2 = ConcurrentSkipListMap.cpr(cmp, toKey, this.hi);
                    if (c2 > 0 || (c2 == 0 && !this.hiInclusive && toInclusive)) {
                        throw new IllegalArgumentException("key out of range");
                    }
                }
            }
            return new SubMap<>(this.m, fromKey, fromInclusive, toKey, toInclusive, this.isDescending);
        }

        @Override
        public SubMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            if (fromKey == null || toKey == null) {
                throw new NullPointerException();
            }
            return newSubMap(fromKey, fromInclusive, toKey, toInclusive);
        }

        @Override
        public SubMap<K, V> headMap(K toKey, boolean inclusive) {
            if (toKey == null) {
                throw new NullPointerException();
            }
            return newSubMap(null, false, toKey, inclusive);
        }

        @Override
        public SubMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if (fromKey == null) {
                throw new NullPointerException();
            }
            return newSubMap(fromKey, inclusive, null, false);
        }

        @Override
        public SubMap<K, V> subMap(K fromKey, K toKey) {
            return subMap((Object) fromKey, true, (Object) toKey, false);
        }

        @Override
        public SubMap<K, V> headMap(K toKey) {
            return headMap((Object) toKey, false);
        }

        @Override
        public SubMap<K, V> tailMap(K fromKey) {
            return tailMap((Object) fromKey, true);
        }

        @Override
        public SubMap<K, V> descendingMap() {
            return new SubMap<>(this.m, this.lo, this.loInclusive, this.hi, this.hiInclusive, !this.isDescending);
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K key) {
            return getNearEntry(key, 1);
        }

        @Override
        public K ceilingKey(K key) {
            return getNearKey(key, 1);
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K key) {
            return getNearEntry(key, 2);
        }

        @Override
        public K lowerKey(K key) {
            return getNearKey(key, 2);
        }

        @Override
        public Map.Entry<K, V> floorEntry(K key) {
            return getNearEntry(key, 3);
        }

        @Override
        public K floorKey(K key) {
            return getNearKey(key, 3);
        }

        @Override
        public Map.Entry<K, V> higherEntry(K key) {
            return getNearEntry(key, 0);
        }

        @Override
        public K higherKey(K key) {
            return getNearKey(key, 0);
        }

        @Override
        public K firstKey() {
            return this.isDescending ? highestKey() : lowestKey();
        }

        @Override
        public K lastKey() {
            return this.isDescending ? lowestKey() : highestKey();
        }

        @Override
        public Map.Entry<K, V> firstEntry() {
            return this.isDescending ? highestEntry() : lowestEntry();
        }

        @Override
        public Map.Entry<K, V> lastEntry() {
            return this.isDescending ? lowestEntry() : highestEntry();
        }

        @Override
        public Map.Entry<K, V> pollFirstEntry() {
            return this.isDescending ? removeHighest() : removeLowest();
        }

        @Override
        public Map.Entry<K, V> pollLastEntry() {
            return this.isDescending ? removeLowest() : removeHighest();
        }

        @Override
        public NavigableSet<K> keySet() {
            KeySet<K, V> ks = this.keySetView;
            if (ks != null) {
                return ks;
            }
            KeySet<K, V> ks2 = new KeySet<>(this);
            this.keySetView = ks2;
            return ks2;
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            KeySet<K, V> ks = this.keySetView;
            if (ks != null) {
                return ks;
            }
            KeySet<K, V> ks2 = new KeySet<>(this);
            this.keySetView = ks2;
            return ks2;
        }

        @Override
        public Collection<V> values() {
            Collection<V> vs = this.valuesView;
            if (vs != null) {
                return vs;
            }
            Collection<V> vs2 = new Values<>(this);
            this.valuesView = vs2;
            return vs2;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K, V>> es = this.entrySetView;
            if (es != null) {
                return es;
            }
            Set<Map.Entry<K, V>> es2 = new EntrySet<>(this);
            this.entrySetView = es2;
            return es2;
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        abstract class SubMapIter<T> implements Iterator<T>, Spliterator<T> {
            Node<K, V> lastReturned;
            Node<K, V> next;
            V nextValue;

            SubMapIter() {
                Comparator<? super K> comparator = SubMap.this.m.comparator;
                while (true) {
                    this.next = SubMap.this.isDescending ? SubMap.this.hiNode(comparator) : SubMap.this.loNode(comparator);
                    if (this.next == null) {
                        return;
                    }
                    V v = (V) this.next.value;
                    if (v != null && v != this.next) {
                        if (!SubMap.this.inBounds(this.next.key, comparator)) {
                            this.next = null;
                            return;
                        } else {
                            this.nextValue = v;
                            return;
                        }
                    }
                }
            }

            @Override
            public final boolean hasNext() {
                return this.next != null;
            }

            final void advance() {
                if (this.next == null) {
                    throw new NoSuchElementException();
                }
                this.lastReturned = this.next;
                if (SubMap.this.isDescending) {
                    descend();
                } else {
                    ascend();
                }
            }

            private void ascend() {
                Comparator<? super K> comparator = SubMap.this.m.comparator;
                while (true) {
                    this.next = this.next.next;
                    if (this.next == null) {
                        return;
                    }
                    V v = (V) this.next.value;
                    if (v != null && v != this.next) {
                        if (SubMap.this.tooHigh(this.next.key, comparator)) {
                            this.next = null;
                            return;
                        } else {
                            this.nextValue = v;
                            return;
                        }
                    }
                }
            }

            private void descend() {
                Comparator<? super K> comparator = SubMap.this.m.comparator;
                while (true) {
                    this.next = SubMap.this.m.findNear(this.lastReturned.key, 2, comparator);
                    if (this.next == null) {
                        return;
                    }
                    V v = (V) this.next.value;
                    if (v != null && v != this.next) {
                        if (SubMap.this.tooLow(this.next.key, comparator)) {
                            this.next = null;
                            return;
                        } else {
                            this.nextValue = v;
                            return;
                        }
                    }
                }
            }

            @Override
            public void remove() {
                Node<K, V> l = this.lastReturned;
                if (l == null) {
                    throw new IllegalStateException();
                }
                SubMap.this.m.remove(l.key);
                this.lastReturned = null;
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                if (hasNext()) {
                    consumer.accept(next());
                    return true;
                }
                return false;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                while (hasNext()) {
                    consumer.accept(next());
                }
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
        }

        final class SubMapValueIterator extends SubMap<K, V>.SubMapIter<V> {
            SubMapValueIterator() {
                super();
            }

            @Override
            public V next() {
                V v = this.nextValue;
                advance();
                return v;
            }

            @Override
            public int characteristics() {
                return 0;
            }
        }

        final class SubMapKeyIterator extends SubMap<K, V>.SubMapIter<K> {
            SubMapKeyIterator() {
                super();
            }

            @Override
            public K next() {
                Node<K, V> n = this.next;
                advance();
                return n.key;
            }

            @Override
            public int characteristics() {
                return 21;
            }

            @Override
            public final Comparator<? super K> getComparator() {
                return SubMap.this.comparator();
            }
        }

        final class SubMapEntryIterator extends SubMap<K, V>.SubMapIter<Map.Entry<K, V>> {
            SubMapEntryIterator() {
                super();
            }

            @Override
            public Map.Entry<K, V> next() {
                Node<K, V> n = this.next;
                V v = this.nextValue;
                advance();
                return new AbstractMap.SimpleImmutableEntry(n.key, v);
            }

            @Override
            public int characteristics() {
                return 1;
            }
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        if (biConsumer == null) {
            throw new NullPointerException();
        }
        for (Node<K, V> nodeFindFirst = findFirst(); nodeFindFirst != null; nodeFindFirst = nodeFindFirst.next) {
            Object validValue = nodeFindFirst.getValidValue();
            if (validValue != null) {
                biConsumer.accept(nodeFindFirst.key, validValue);
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Object validValue;
        V vApply;
        if (biFunction == null) {
            throw new NullPointerException();
        }
        for (Node<K, V> nodeFindFirst = findFirst(); nodeFindFirst != null; nodeFindFirst = nodeFindFirst.next) {
            do {
                validValue = nodeFindFirst.getValidValue();
                if (validValue == null) {
                    break;
                }
                vApply = biFunction.apply(nodeFindFirst.key, validValue);
                if (vApply == null) {
                    throw new NullPointerException();
                }
            } while (!nodeFindFirst.casValue(validValue, vApply));
        }
    }

    boolean removeEntryIf(Predicate<? super Map.Entry<K, V>> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        boolean removed = false;
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null) {
                K k = n.key;
                Map.Entry<K, V> e = new AbstractMap.SimpleImmutableEntry<>(k, v);
                if (function.test(e) && remove(k, v)) {
                    removed = true;
                }
            }
        }
        return removed;
    }

    boolean removeValueIf(Predicate<? super V> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        boolean removed = false;
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null) {
                K k = n.key;
                if (function.test(v) && remove(k, v)) {
                    removed = true;
                }
            }
        }
        return removed;
    }

    static abstract class CSLMSpliterator<K, V> {
        final Comparator<? super K> comparator;
        Node<K, V> current;
        int est;
        final K fence;
        Index<K, V> row;

        CSLMSpliterator(Comparator<? super K> comparator, Index<K, V> row, Node<K, V> origin, K fence, int est) {
            this.comparator = comparator;
            this.row = row;
            this.current = origin;
            this.fence = fence;
            this.est = est;
        }

        public final long estimateSize() {
            return this.est;
        }
    }

    static final class KeySpliterator<K, V> extends CSLMSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(Comparator<? super K> comparator, Index<K, V> row, Node<K, V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            K ek;
            Node<K, V> b;
            Node<K, V> n;
            K sk;
            Comparator<? super K> cmp = this.comparator;
            K f = this.fence;
            Node<K, V> e = this.current;
            if (e != null && (ek = e.key) != null) {
                Index<K, V> q = this.row;
                while (q != null) {
                    Index<K, V> s = q.right;
                    if (s == null || (b = s.node) == null || (n = b.next) == null || n.value == null || (sk = n.key) == null || ConcurrentSkipListMap.cpr(cmp, sk, ek) <= 0 || (f != null && ConcurrentSkipListMap.cpr(cmp, sk, f) >= 0)) {
                        q = q.down;
                        this.row = q;
                    } else {
                        this.current = n;
                        Index<K, V> r = q.down;
                        if (s.right == null) {
                            s = s.down;
                        }
                        this.row = s;
                        this.est -= this.est >>> 2;
                        return new KeySpliterator<>(cmp, r, e, sk, this.est);
                    }
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> comparator = this.comparator;
            K k = this.fence;
            this.current = null;
            for (Node<K, V> node = this.current; node != null; node = node.next) {
                K k2 = node.key;
                if (k2 != null && k != null && ConcurrentSkipListMap.cpr(comparator, k, k2) <= 0) {
                    return;
                }
                Object obj = node.value;
                if (obj != null && obj != node) {
                    consumer.accept(k2);
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> comparator = this.comparator;
            K k = this.fence;
            Node<K, V> node = this.current;
            while (true) {
                if (node != null) {
                    K k2 = node.key;
                    if (k2 != null && k != null && ConcurrentSkipListMap.cpr(comparator, k, k2) <= 0) {
                        node = null;
                        break;
                    }
                    Object obj = node.value;
                    if (obj == null || obj == node) {
                        node = node.next;
                    } else {
                        this.current = node.next;
                        consumer.accept(k2);
                        return true;
                    }
                } else {
                    break;
                }
            }
            this.current = node;
            return false;
        }

        @Override
        public int characteristics() {
            return 4373;
        }

        @Override
        public final Comparator<? super K> getComparator() {
            return this.comparator;
        }
    }

    final KeySpliterator<K, V> keySpliterator() {
        HeadIndex<K, V> h;
        Node<K, V> p;
        Comparator<? super K> cmp = this.comparator;
        while (true) {
            h = this.head;
            Node<K, V> b = h.node;
            p = b.next;
            if (p == null || p.value != null) {
                break;
            }
            p.helpDelete(b, p.next);
        }
        return new KeySpliterator<>(cmp, h, p, null, p == null ? 0 : Integer.MAX_VALUE);
    }

    static final class ValueSpliterator<K, V> extends CSLMSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(Comparator<? super K> comparator, Index<K, V> row, Node<K, V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            K ek;
            Node<K, V> b;
            Node<K, V> n;
            K sk;
            Comparator<? super K> cmp = this.comparator;
            K f = this.fence;
            Node<K, V> e = this.current;
            if (e != null && (ek = e.key) != null) {
                Index<K, V> q = this.row;
                while (q != null) {
                    Index<K, V> s = q.right;
                    if (s == null || (b = s.node) == null || (n = b.next) == null || n.value == null || (sk = n.key) == null || ConcurrentSkipListMap.cpr(cmp, sk, ek) <= 0 || (f != null && ConcurrentSkipListMap.cpr(cmp, sk, f) >= 0)) {
                        q = q.down;
                        this.row = q;
                    } else {
                        this.current = n;
                        Index<K, V> r = q.down;
                        if (s.right == null) {
                            s = s.down;
                        }
                        this.row = s;
                        this.est -= this.est >>> 2;
                        return new ValueSpliterator<>(cmp, r, e, sk, this.est);
                    }
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> cmp = this.comparator;
            K f = this.fence;
            this.current = null;
            for (Node<K, V> e = this.current; e != null; e = e.next) {
                K k = e.key;
                if (k != null && f != null && ConcurrentSkipListMap.cpr(cmp, f, k) <= 0) {
                    return;
                }
                Object v = e.value;
                if (v != null && v != e) {
                    action.accept(v);
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> cmp = this.comparator;
            K f = this.fence;
            Node<K, V> e = this.current;
            while (true) {
                if (e != null) {
                    K k = e.key;
                    if (k != null && f != null && ConcurrentSkipListMap.cpr(cmp, f, k) <= 0) {
                        e = null;
                        break;
                    }
                    Object v = e.value;
                    if (v == null || v == e) {
                        e = e.next;
                    } else {
                        this.current = e.next;
                        action.accept(v);
                        return true;
                    }
                } else {
                    break;
                }
            }
            this.current = e;
            return false;
        }

        @Override
        public int characteristics() {
            return 4368;
        }
    }

    final ValueSpliterator<K, V> valueSpliterator() {
        HeadIndex<K, V> h;
        Node<K, V> p;
        Comparator<? super K> cmp = this.comparator;
        while (true) {
            h = this.head;
            Node<K, V> b = h.node;
            p = b.next;
            if (p == null || p.value != null) {
                break;
            }
            p.helpDelete(b, p.next);
        }
        return new ValueSpliterator<>(cmp, h, p, null, p == null ? 0 : Integer.MAX_VALUE);
    }

    static final class EntrySpliterator<K, V> extends CSLMSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {

        final class java_util_Comparator_getComparator__LambdaImpl0 implements Comparator, Serializable {
            @Override
            public int compare(Object arg0, Object arg1) {
                return EntrySpliterator.m338x9dd9e5b1((Map.Entry) arg0, (Map.Entry) arg1);
            }
        }

        EntrySpliterator(Comparator<? super K> comparator, Index<K, V> row, Node<K, V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            K ek;
            Node<K, V> b;
            Node<K, V> n;
            K sk;
            Comparator<? super K> cmp = this.comparator;
            K f = this.fence;
            Node<K, V> e = this.current;
            if (e != null && (ek = e.key) != null) {
                Index<K, V> q = this.row;
                while (q != null) {
                    Index<K, V> s = q.right;
                    if (s == null || (b = s.node) == null || (n = b.next) == null || n.value == null || (sk = n.key) == null || ConcurrentSkipListMap.cpr(cmp, sk, ek) <= 0 || (f != null && ConcurrentSkipListMap.cpr(cmp, sk, f) >= 0)) {
                        q = q.down;
                        this.row = q;
                    } else {
                        this.current = n;
                        Index<K, V> r = q.down;
                        if (s.right == null) {
                            s = s.down;
                        }
                        this.row = s;
                        this.est -= this.est >>> 2;
                        return new EntrySpliterator<>(cmp, r, e, sk, this.est);
                    }
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> cmp = this.comparator;
            K f = this.fence;
            this.current = null;
            for (Node<K, V> e = this.current; e != null; e = e.next) {
                K k = e.key;
                if (k != null && f != null && ConcurrentSkipListMap.cpr(cmp, f, k) <= 0) {
                    return;
                }
                Object v = e.value;
                if (v != null && v != e) {
                    action.accept(new AbstractMap.SimpleImmutableEntry(k, v));
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> cmp = this.comparator;
            K f = this.fence;
            Node<K, V> e = this.current;
            while (true) {
                if (e != null) {
                    K k = e.key;
                    if (k != null && f != null && ConcurrentSkipListMap.cpr(cmp, f, k) <= 0) {
                        e = null;
                        break;
                    }
                    Object v = e.value;
                    if (v == null || v == e) {
                        e = e.next;
                    } else {
                        this.current = e.next;
                        action.accept(new AbstractMap.SimpleImmutableEntry(k, v));
                        return true;
                    }
                } else {
                    break;
                }
            }
            this.current = e;
            return false;
        }

        @Override
        public int characteristics() {
            return 4373;
        }

        @Override
        public final Comparator<Map.Entry<K, V>> getComparator() {
            if (this.comparator != null) {
                return Map.Entry.comparingByKey(this.comparator);
            }
            return new java_util_Comparator_getComparator__LambdaImpl0();
        }

        static int m338x9dd9e5b1(Map.Entry e1, Map.Entry e2) {
            Comparable<? super K> k1 = (Comparable) e1.getKey();
            return k1.compareTo(e2.getKey());
        }
    }

    final EntrySpliterator<K, V> entrySpliterator() {
        HeadIndex<K, V> h;
        Node<K, V> p;
        Comparator<? super K> cmp = this.comparator;
        while (true) {
            h = this.head;
            Node<K, V> b = h.node;
            p = b.next;
            if (p == null || p.value != null) {
                break;
            }
            p.helpDelete(b, p.next);
        }
        return new EntrySpliterator<>(cmp, h, p, null, p == null ? 0 : Integer.MAX_VALUE);
    }
}
