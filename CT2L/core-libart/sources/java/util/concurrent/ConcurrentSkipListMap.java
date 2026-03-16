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
import sun.misc.Unsafe;

public class ConcurrentSkipListMap<K, V> extends AbstractMap<K, V> implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable {
    private static final Object BASE_HEADER = new Object();
    private static final int EQ = 1;
    private static final int GT = 0;
    private static final int LT = 2;
    private static final Unsafe UNSAFE;
    private static final long headOffset;
    private static final long serialVersionUID = -8627078645895051609L;
    private final Comparator<? super K> comparator;
    private transient ConcurrentNavigableMap<K, V> descendingMap;
    private transient EntrySet<K, V> entrySet;
    private volatile transient HeadIndex<K, V> head;
    private transient KeySet<K> keySet;
    private transient int randomSeed;
    private transient Values<V> values;

    static {
        try {
            UNSAFE = Unsafe.getUnsafe();
            headOffset = UNSAFE.objectFieldOffset(ConcurrentSkipListMap.class.getDeclaredField("head"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    final void initialize() {
        this.keySet = null;
        this.entrySet = null;
        this.values = null;
        this.descendingMap = null;
        this.randomSeed = Math.randomIntInternal() | 256;
        this.head = new HeadIndex<>(new Node(null, BASE_HEADER, null), null, null, 1);
    }

    private boolean casHead(HeadIndex<K, V> cmp, HeadIndex<K, V> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    static final class Node<K, V> {
        private static final Unsafe UNSAFE;
        private static final long nextOffset;
        private static final long valueOffset;
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
            return UNSAFE.compareAndSwapObject(this, valueOffset, cmp, val);
        }

        boolean casNext(Node<K, V> cmp, Node<K, V> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
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
            if (f == this.next && this == b.next) {
                if (f == null || f.value != f) {
                    appendMarker(f);
                } else {
                    b.casNext(this, f.next);
                }
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
            V v = getValidValue();
            if (v == null) {
                return null;
            }
            return new AbstractMap.SimpleImmutableEntry<>(this.key, v);
        }

        static {
            try {
                UNSAFE = Unsafe.getUnsafe();
                valueOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("value"));
                nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    static class Index<K, V> {
        private static final Unsafe UNSAFE;
        private static final long rightOffset;
        final Index<K, V> down;
        final Node<K, V> node;
        volatile Index<K, V> right;

        Index(Node<K, V> node, Index<K, V> down, Index<K, V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }

        final boolean casRight(Index<K, V> cmp, Index<K, V> val) {
            return UNSAFE.compareAndSwapObject(this, rightOffset, cmp, val);
        }

        final boolean indexesDeletedNode() {
            return this.node.value == null;
        }

        final boolean link(Index<K, V> succ, Index<K, V> newSucc) {
            Node<K, V> n = this.node;
            newSucc.right = succ;
            return n.value != null && casRight(succ, newSucc);
        }

        final boolean unlink(Index<K, V> succ) {
            return !indexesDeletedNode() && casRight(succ, succ.right);
        }

        static {
            try {
                UNSAFE = Unsafe.getUnsafe();
                rightOffset = UNSAFE.objectFieldOffset(Index.class.getDeclaredField("right"));
            } catch (Exception e) {
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

    static final class ComparableUsingComparator<K> implements Comparable<K> {
        final K actualKey;
        final Comparator<? super K> cmp;

        ComparableUsingComparator(K key, Comparator<? super K> cmp) {
            this.actualKey = key;
            this.cmp = cmp;
        }

        @Override
        public int compareTo(K k) {
            return this.cmp.compare(this.actualKey, k);
        }
    }

    private Comparable<? super K> comparable(Object key) throws ClassCastException {
        if (key == null) {
            throw new NullPointerException();
        }
        return this.comparator != null ? new ComparableUsingComparator(key, this.comparator) : (Comparable) key;
    }

    int compare(K k1, K k2) throws ClassCastException {
        Comparator<? super K> cmp = this.comparator;
        return cmp != null ? cmp.compare(k1, k2) : ((Comparable) k1).compareTo(k2);
    }

    boolean inHalfOpenRange(K key, K least, K fence) {
        if (key == null) {
            throw new NullPointerException();
        }
        return (least == null || compare(key, least) >= 0) && (fence == null || compare(key, fence) < 0);
    }

    boolean inOpenRange(K key, K least, K fence) {
        if (key == null) {
            throw new NullPointerException();
        }
        return (least == null || compare(key, least) >= 0) && (fence == null || compare(key, fence) <= 0);
    }

    private Node<K, V> findPredecessor(Comparable<? super K> comparable) {
        if (comparable == null) {
            throw new NullPointerException();
        }
        while (true) {
            Index<K, V> index = this.head;
            Index<K, V> index2 = index.right;
            while (true) {
                if (index2 != null) {
                    Node<K, V> node = index2.node;
                    K k = node.key;
                    if (node.value == null) {
                        if (index.unlink(index2)) {
                            index2 = index.right;
                        }
                    } else if (comparable.compareTo(k) > 0) {
                        index = index2;
                        index2 = index2.right;
                    }
                }
                Index<K, V> index3 = index.down;
                if (index3 != null) {
                    index = index3;
                    index2 = index3.right;
                } else {
                    return index.node;
                }
            }
        }
    }

    private Node<K, V> findNode(Comparable<? super K> comparable) {
        while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(comparable);
            Node<K, V> node = nodeFindPredecessor.next;
            while (node != null) {
                Node<K, V> node2 = node.next;
                if (node != nodeFindPredecessor.next) {
                    break;
                }
                Object obj = node.value;
                if (obj == null) {
                    node.helpDelete(nodeFindPredecessor, node2);
                } else {
                    if (obj == node || nodeFindPredecessor.value == null) {
                        break;
                    }
                    int iCompareTo = comparable.compareTo(node.key);
                    if (iCompareTo != 0) {
                        if (iCompareTo < 0) {
                            return null;
                        }
                        nodeFindPredecessor = node;
                        node = node2;
                    } else {
                        return node;
                    }
                }
            }
            return null;
        }
    }

    private V doGet(Object obj) {
        V v;
        Comparable<? super K> comparable = comparable(obj);
        do {
            Node<K, V> nodeFindNode = findNode(comparable);
            if (nodeFindNode == null) {
                return null;
            }
            v = (V) nodeFindNode.value;
        } while (v == null);
        return v;
    }

    private V doPut(K k, V v, boolean z) {
        Comparable<? super K> comparable = comparable(k);
        while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(comparable);
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
                if (v2 == node || nodeFindPredecessor.value == null) {
                    break;
                }
                int iCompareTo = comparable.compareTo(node.key);
                if (iCompareTo > 0) {
                    nodeFindPredecessor = node;
                    node = node2;
                } else if (iCompareTo == 0) {
                    if (z || node.casValue(v2, v)) {
                        return v2;
                    }
                }
            }
        }
    }

    private int randomLevel() {
        int x = this.randomSeed;
        int x2 = x ^ (x << 13);
        int x3 = x2 ^ (x2 >>> 17);
        int x4 = x3 ^ (x3 << 5);
        this.randomSeed = x4;
        if (((-2147483647) & x4) != 0) {
            return 0;
        }
        int level = 1;
        while (true) {
            x4 >>>= 1;
            if ((x4 & 1) == 0) {
                return level;
            }
            level++;
        }
    }

    private void insertIndex(Node<K, V> z, int level) {
        HeadIndex<K, V> oldh;
        int k;
        HeadIndex<K, V> newh;
        HeadIndex<K, V> h = this.head;
        int max = h.level;
        if (level <= max) {
            Index<K, V> idx = null;
            int i = 1;
            while (true) {
                Index<K, V> idx2 = idx;
                if (i <= level) {
                    idx = new Index<>(z, idx2, null);
                    i++;
                } else {
                    addIndex(idx2, h, level);
                    return;
                }
            }
        } else {
            int level2 = max + 1;
            Index<K, V>[] idxs = new Index[level2 + 1];
            Index<K, V> idx3 = null;
            int i2 = 1;
            while (true) {
                Index<K, V> idx4 = idx3;
                if (i2 > level2) {
                    break;
                }
                idx3 = new Index<>(z, idx4, null);
                idxs[i2] = idx3;
                i2++;
            }
            while (true) {
                oldh = this.head;
                int oldLevel = oldh.level;
                if (level2 <= oldLevel) {
                    k = level2;
                    break;
                }
                HeadIndex<K, V> newh2 = oldh;
                Node<K, V> oldbase = oldh.node;
                int j = oldLevel + 1;
                while (true) {
                    newh = newh2;
                    if (j > level2) {
                        break;
                    }
                    newh2 = new HeadIndex<>(oldbase, newh, idxs[j], j);
                    j++;
                }
                if (casHead(oldh, newh)) {
                    k = oldLevel;
                    break;
                }
            }
            addIndex(idxs[k], oldh, k);
        }
    }

    private void addIndex(Index<K, V> index, HeadIndex<K, V> headIndex, int i) {
        int i2 = i;
        Comparable<? super K> comparable = comparable(index.node.key);
        if (comparable == null) {
            throw new NullPointerException();
        }
        while (true) {
            int i3 = headIndex.level;
            Index<K, V> index2 = headIndex;
            Index<K, V> index3 = index2.right;
            Index<K, V> index4 = index;
            while (true) {
                if (index3 != null) {
                    Node<K, V> node = index3.node;
                    int iCompareTo = comparable.compareTo(node.key);
                    if (node.value == null) {
                        if (index2.unlink(index3)) {
                            index3 = index2.right;
                        }
                    } else if (iCompareTo > 0) {
                        index2 = index3;
                        index3 = index3.right;
                    }
                }
                if (i3 == i2) {
                    if (index4.indexesDeletedNode()) {
                        findNode(comparable);
                        return;
                    } else if (index2.link(index3, index4)) {
                        i2--;
                        if (i2 == 0) {
                            if (index4.indexesDeletedNode()) {
                                findNode(comparable);
                                return;
                            }
                            return;
                        }
                    }
                }
                i3--;
                if (i3 >= i2 && i3 < i) {
                    index4 = index4.down;
                }
                index2 = index2.down;
                index3 = index2.right;
            }
        }
    }

    final V doRemove(Object obj, Object obj2) {
        Comparable<? super K> comparable = comparable(obj);
        while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(comparable);
            Node<K, V> node = nodeFindPredecessor.next;
            while (node != null) {
                Node<K, V> node2 = node.next;
                if (node != nodeFindPredecessor.next) {
                    break;
                }
                V v = (V) node.value;
                if (v == null) {
                    node.helpDelete(nodeFindPredecessor, node2);
                } else {
                    if (v == node || nodeFindPredecessor.value == null) {
                        break;
                    }
                    int iCompareTo = comparable.compareTo(node.key);
                    if (iCompareTo < 0) {
                        return null;
                    }
                    if (iCompareTo > 0) {
                        nodeFindPredecessor = node;
                        node = node2;
                    } else {
                        if (obj2 != null && !obj2.equals(v)) {
                            return null;
                        }
                        if (node.casValue(v, null)) {
                            if (!node.appendMarker(node2) || !nodeFindPredecessor.casNext(node, node2)) {
                                findNode(comparable);
                                return v;
                            }
                            findPredecessor(comparable);
                            if (this.head.right == null) {
                                tryReduceLevel();
                                return v;
                            }
                            return v;
                        }
                    }
                }
            }
            return null;
        }
    }

    private void tryReduceLevel() {
        HeadIndex<K, V> d;
        HeadIndex<K, V> e;
        HeadIndex<K, V> h = this.head;
        if (h.level > 3 && (d = (HeadIndex) h.down) != null && (e = (HeadIndex) d.down) != null && e.right == null && d.right == null && h.right == null && casHead(h, d) && h.right != null) {
            casHead(d, h);
        }
    }

    Node<K, V> findFirst() {
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

    Map.Entry<K, V> doRemoveFirstEntry() {
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

    Node<K, V> findLast() {
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
                            } else if (v != n && b.value != null) {
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

    Map.Entry<K, V> doRemoveLastEntry() {
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
                    if (v == n || b.value == null) {
                        break;
                    }
                    if (f != null) {
                        b = n;
                        n = f;
                    } else if (n.casValue(v, null)) {
                        K key = n.key;
                        Comparable<? super K> ck = comparable(key);
                        if (!n.appendMarker(f) || !b.casNext(n, f)) {
                            findNode(ck);
                        } else {
                            findPredecessor(ck);
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

    Node<K, V> findNear(K k, int i) {
        Comparable<? super K> comparable = comparable(k);
        loop0: while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(comparable);
            Node<K, V> node = nodeFindPredecessor.next;
            while (node != null) {
                Node<K, V> node2 = node.next;
                if (node == nodeFindPredecessor.next) {
                    Object obj = node.value;
                    if (obj == null) {
                        node.helpDelete(nodeFindPredecessor, node2);
                    } else if (obj != node && nodeFindPredecessor.value != null) {
                        int iCompareTo = comparable.compareTo(node.key);
                        if ((iCompareTo == 0 && (i & 1) != 0) || (iCompareTo < 0 && (i & 2) == 0)) {
                            break loop0;
                        }
                        if (iCompareTo <= 0 && (i & 2) != 0) {
                            return nodeFindPredecessor.isBaseHeader() ? null : nodeFindPredecessor;
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
            if ((i & 2) == 0 || nodeFindPredecessor.isBaseHeader()) {
                return null;
            }
            return nodeFindPredecessor;
        }
    }

    AbstractMap.SimpleImmutableEntry<K, V> getNear(K key, int rel) {
        AbstractMap.SimpleImmutableEntry<K, V> e;
        do {
            Node<K, V> n = findNear(key, rel);
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
            int j = randomLevel();
            if (j > h.level) {
                j = h.level + 1;
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

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
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
                int j = randomLevel();
                if (j > h.level) {
                    j = h.level + 1;
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
        return count >= 2147483647L ? Integer.MAX_VALUE : (int) count;
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
    public NavigableSet<K> keySet() {
        KeySet<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySet<K> ks2 = new KeySet<>(this);
        this.keySet = ks2;
        return ks2;
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        KeySet<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySet<K> ks2 = new KeySet<>(this);
        this.keySet = ks2;
        return ks2;
    }

    @Override
    public Collection<V> values() {
        Values<V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        Values<V> vs2 = new Values<>(this);
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
            for (Map.Entry<K, V> entry : m.entrySet()) {
                Object k = entry.getKey();
                Object v = entry.getValue();
                if (k == null || v == null || !v.equals(get(k))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e2) {
            return false;
        } catch (NullPointerException e3) {
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
        if (oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        Comparable<? super K> k = comparable(key);
        while (true) {
            Node<K, V> n = findNode(k);
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
        if (v == null) {
            throw new NullPointerException();
        }
        Comparable<? super K> comparable = comparable(k);
        while (true) {
            Node<K, V> nodeFindNode = findNode(comparable);
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
        Node<K, V> n = findNear(key, 2);
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
        Node<K, V> n = findNear(key, 3);
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
        Node<K, V> n = findNear(key, 1);
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
        Node<K, V> n = findNear(key, 0);
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
                this.next = ConcurrentSkipListMap.this.findFirst();
                if (this.next != null) {
                    V v = (V) this.next.value;
                    if (v != null && v != this.next) {
                        this.nextValue = v;
                        return;
                    }
                } else {
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
                this.next = this.next.next;
                if (this.next != null) {
                    V v = (V) this.next.value;
                    if (v != null && v != this.next) {
                        this.nextValue = v;
                        return;
                    }
                } else {
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

    Iterator<K> keyIterator() {
        return new KeyIterator();
    }

    Iterator<V> valueIterator() {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K, V>> entryIterator() {
        return new EntryIterator();
    }

    static final <E> List<E> toList(Collection<E> c) {
        ArrayList<E> list = new ArrayList<>();
        for (E e : c) {
            list.add(e);
        }
        return list;
    }

    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final ConcurrentNavigableMap<E, ?> m;

        KeySet(ConcurrentNavigableMap<E, ?> map) {
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
        public E lower(E e) {
            return this.m.lowerKey(e);
        }

        @Override
        public E floor(E e) {
            return this.m.floorKey(e);
        }

        @Override
        public E ceiling(E e) {
            return this.m.ceilingKey(e);
        }

        @Override
        public E higher(E e) {
            return this.m.higherKey(e);
        }

        @Override
        public Comparator<? super E> comparator() {
            return this.m.comparator();
        }

        @Override
        public E first() {
            return this.m.firstKey();
        }

        @Override
        public E last() {
            return this.m.lastKey();
        }

        @Override
        public E pollFirst() {
            Map.Entry<E, ?> e = this.m.pollFirstEntry();
            if (e == null) {
                return null;
            }
            return e.getKey();
        }

        @Override
        public E pollLast() {
            Map.Entry<E, ?> e = this.m.pollLastEntry();
            if (e == null) {
                return null;
            }
            return e.getKey();
        }

        @Override
        public Iterator<E> iterator() {
            return this.m instanceof ConcurrentSkipListMap ? ((ConcurrentSkipListMap) this.m).keyIterator() : ((SubMap) this.m).keyIterator();
        }

        @Override
        public boolean equals(Object o) {
            boolean z;
            if (o == this) {
                return true;
            }
            if (!(o instanceof Set)) {
                return false;
            }
            Collection<?> c = (Collection) o;
            try {
                if (containsAll(c)) {
                    z = c.containsAll(this);
                }
                return z;
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
        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return new KeySet(this.m.subMap(fromElement, fromInclusive, toElement, toInclusive));
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new KeySet(this.m.headMap(toElement, inclusive));
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new KeySet(this.m.tailMap(fromElement, inclusive));
        }

        @Override
        public NavigableSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public NavigableSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return new KeySet(this.m.descendingMap());
        }
    }

    static final class Values<E> extends AbstractCollection<E> {
        private final ConcurrentNavigableMap<?, E> m;

        Values(ConcurrentNavigableMap<?, E> map) {
            this.m = map;
        }

        @Override
        public Iterator<E> iterator() {
            return this.m instanceof ConcurrentSkipListMap ? ((ConcurrentSkipListMap) this.m).valueIterator() : ((SubMap) this.m).valueIterator();
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
    }

    static final class EntrySet<K1, V1> extends AbstractSet<Map.Entry<K1, V1>> {
        private final ConcurrentNavigableMap<K1, V1> m;

        EntrySet(ConcurrentNavigableMap<K1, V1> map) {
            this.m = map;
        }

        @Override
        public Iterator<Map.Entry<K1, V1>> iterator() {
            return this.m instanceof ConcurrentSkipListMap ? ((ConcurrentSkipListMap) this.m).entryIterator() : ((SubMap) this.m).entryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry) o;
            V1 v = this.m.get(e.getKey());
            return v != null && v.equals(e.getValue());
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
            boolean z;
            if (o == this) {
                return true;
            }
            if (!(o instanceof Set)) {
                return false;
            }
            Collection<?> c = (Collection) o;
            try {
                if (containsAll(c)) {
                    z = c.containsAll(this);
                }
                return z;
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
    }

    static final class SubMap<K, V> extends AbstractMap<K, V> implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable {
        private static final long serialVersionUID = -7647078645895051609L;
        private transient Set<Map.Entry<K, V>> entrySetView;
        private final K hi;
        private final boolean hiInclusive;
        private final boolean isDescending;
        private transient KeySet<K> keySetView;
        private final K lo;
        private final boolean loInclusive;
        private final ConcurrentSkipListMap<K, V> m;
        private transient Collection<V> valuesView;

        SubMap(ConcurrentSkipListMap<K, V> map, K fromKey, boolean fromInclusive, K toKey, boolean toInclusive, boolean isDescending) {
            if (fromKey != null && toKey != null && map.compare(fromKey, toKey) > 0) {
                throw new IllegalArgumentException("inconsistent range");
            }
            this.m = map;
            this.lo = fromKey;
            this.hi = toKey;
            this.loInclusive = fromInclusive;
            this.hiInclusive = toInclusive;
            this.isDescending = isDescending;
        }

        private boolean tooLow(K key) {
            int c;
            return this.lo != null && ((c = this.m.compare(key, this.lo)) < 0 || (c == 0 && !this.loInclusive));
        }

        private boolean tooHigh(K key) {
            int c;
            return this.hi != null && ((c = this.m.compare(key, this.hi)) > 0 || (c == 0 && !this.hiInclusive));
        }

        private boolean inBounds(K key) {
            return (tooLow(key) || tooHigh(key)) ? false : true;
        }

        private void checkKeyBounds(K key) throws IllegalArgumentException {
            if (key == null) {
                throw new NullPointerException();
            }
            if (!inBounds(key)) {
                throw new IllegalArgumentException("key out of range");
            }
        }

        private boolean isBeforeEnd(Node<K, V> n) {
            K k;
            if (n == null) {
                return false;
            }
            if (this.hi != null && (k = n.key) != null) {
                int c = this.m.compare(k, this.hi);
                if (c <= 0) {
                    return c != 0 || this.hiInclusive;
                }
                return false;
            }
            return true;
        }

        private Node<K, V> loNode() {
            if (this.lo == null) {
                return this.m.findFirst();
            }
            if (this.loInclusive) {
                return this.m.findNear(this.lo, 1);
            }
            return this.m.findNear(this.lo, 0);
        }

        private Node<K, V> hiNode() {
            if (this.hi == null) {
                return this.m.findLast();
            }
            if (this.hiInclusive) {
                return this.m.findNear(this.hi, 3);
            }
            return this.m.findNear(this.hi, 2);
        }

        private K lowestKey() {
            Node<K, V> n = loNode();
            if (isBeforeEnd(n)) {
                return n.key;
            }
            throw new NoSuchElementException();
        }

        private K highestKey() {
            Node<K, V> n = hiNode();
            if (n != null) {
                K last = n.key;
                if (inBounds(last)) {
                    return last;
                }
            }
            throw new NoSuchElementException();
        }

        private Map.Entry<K, V> lowestEntry() {
            Map.Entry<K, V> e;
            do {
                Node<K, V> n = loNode();
                if (!isBeforeEnd(n)) {
                    return null;
                }
                e = n.createSnapshot();
            } while (e == null);
            return e;
        }

        private Map.Entry<K, V> highestEntry() {
            Map.Entry<K, V> e;
            do {
                Node<K, V> n = hiNode();
                if (n == null || !inBounds(n.key)) {
                    return null;
                }
                e = n.createSnapshot();
            } while (e == null);
            return e;
        }

        private Map.Entry<K, V> removeLowest() {
            K k;
            V v;
            do {
                Node<K, V> n = loNode();
                if (n == null) {
                    return null;
                }
                k = n.key;
                if (!inBounds(k)) {
                    return null;
                }
                v = this.m.doRemove(k, null);
            } while (v == null);
            return new AbstractMap.SimpleImmutableEntry(k, v);
        }

        private Map.Entry<K, V> removeHighest() {
            K k;
            V v;
            do {
                Node<K, V> n = hiNode();
                if (n == null) {
                    return null;
                }
                k = n.key;
                if (!inBounds(k)) {
                    return null;
                }
                v = this.m.doRemove(k, null);
            } while (v == null);
            return new AbstractMap.SimpleImmutableEntry(k, v);
        }

        private Map.Entry<K, V> getNearEntry(K key, int rel) {
            K k;
            V v;
            if (this.isDescending) {
                if ((rel & 2) == 0) {
                    rel |= 2;
                } else {
                    rel &= -3;
                }
            }
            if (tooLow(key)) {
                if ((rel & 2) != 0) {
                    return null;
                }
                return lowestEntry();
            }
            if (tooHigh(key)) {
                if ((rel & 2) != 0) {
                    return highestEntry();
                }
                return null;
            }
            do {
                Node<K, V> n = this.m.findNear(key, rel);
                if (n == null || !inBounds(n.key)) {
                    return null;
                }
                k = n.key;
                v = n.getValidValue();
            } while (v == null);
            return new AbstractMap.SimpleImmutableEntry(k, v);
        }

        private K getNearKey(K key, int rel) {
            K k;
            V v;
            Node<K, V> n;
            if (this.isDescending) {
                if ((rel & 2) == 0) {
                    rel |= 2;
                } else {
                    rel &= -3;
                }
            }
            if (tooLow(key)) {
                if ((rel & 2) == 0) {
                    Node<K, V> n2 = loNode();
                    if (isBeforeEnd(n2)) {
                        return n2.key;
                    }
                }
                return null;
            }
            if (tooHigh(key)) {
                if ((rel & 2) != 0 && (n = hiNode()) != null) {
                    K last = n.key;
                    if (inBounds(last)) {
                        return last;
                    }
                }
                return null;
            }
            do {
                Node<K, V> n3 = this.m.findNear(key, rel);
                if (n3 == null || !inBounds(n3.key)) {
                    return null;
                }
                k = n3.key;
                v = n3.getValidValue();
            } while (v == null);
            return k;
        }

        @Override
        public boolean containsKey(Object obj) {
            if (obj == 0) {
                throw new NullPointerException();
            }
            return inBounds(obj) && this.m.containsKey(obj);
        }

        @Override
        public V get(Object obj) {
            if (obj == 0) {
                throw new NullPointerException();
            }
            if (inBounds(obj)) {
                return this.m.get(obj);
            }
            return null;
        }

        @Override
        public V put(K key, V value) {
            checkKeyBounds(key);
            return this.m.put(key, value);
        }

        @Override
        public V remove(Object obj) {
            if (inBounds(obj)) {
                return this.m.remove(obj);
            }
            return null;
        }

        @Override
        public int size() {
            long count = 0;
            for (Node<K, V> n = loNode(); isBeforeEnd(n); n = n.next) {
                if (n.getValidValue() != null) {
                    count++;
                }
            }
            return count >= 2147483647L ? Integer.MAX_VALUE : (int) count;
        }

        @Override
        public boolean isEmpty() {
            return !isBeforeEnd(loNode());
        }

        @Override
        public boolean containsValue(Object value) {
            if (value == null) {
                throw new NullPointerException();
            }
            for (Node<K, V> n = loNode(); isBeforeEnd(n); n = n.next) {
                V v = n.getValidValue();
                if (v != null && value.equals(v)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            for (Node<K, V> n = loNode(); isBeforeEnd(n); n = n.next) {
                if (n.getValidValue() != null) {
                    this.m.remove(n.key);
                }
            }
        }

        @Override
        public V putIfAbsent(K key, V value) {
            checkKeyBounds(key);
            return this.m.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object obj, Object value) {
            return inBounds(obj) && this.m.remove(obj, value);
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            checkKeyBounds(key);
            return this.m.replace(key, oldValue, newValue);
        }

        @Override
        public V replace(K key, V value) {
            checkKeyBounds(key);
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

        private SubMap<K, V> newSubMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
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
                    int c = this.m.compare(fromKey, this.lo);
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
                    int c2 = this.m.compare(toKey, this.hi);
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
            KeySet<K> ks = this.keySetView;
            if (ks != null) {
                return ks;
            }
            KeySet<K> ks2 = new KeySet<>(this);
            this.keySetView = ks2;
            return ks2;
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            KeySet<K> ks = this.keySetView;
            if (ks != null) {
                return ks;
            }
            KeySet<K> ks2 = new KeySet<>(this);
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

        Iterator<K> keyIterator() {
            return new SubMapKeyIterator();
        }

        Iterator<V> valueIterator() {
            return new SubMapValueIterator();
        }

        Iterator<Map.Entry<K, V>> entryIterator() {
            return new SubMapEntryIterator();
        }

        abstract class SubMapIter<T> implements Iterator<T> {
            Node<K, V> lastReturned;
            Node<K, V> next;
            V nextValue;

            SubMapIter() {
                while (true) {
                    this.next = SubMap.this.isDescending ? SubMap.this.hiNode() : SubMap.this.loNode();
                    if (this.next != null) {
                        V v = (V) this.next.value;
                        if (v != null && v != this.next) {
                            if (!SubMap.this.inBounds(this.next.key)) {
                                this.next = null;
                                return;
                            } else {
                                this.nextValue = v;
                                return;
                            }
                        }
                    } else {
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
                if (SubMap.this.isDescending) {
                    descend();
                } else {
                    ascend();
                }
            }

            private void ascend() {
                while (true) {
                    this.next = this.next.next;
                    if (this.next != null) {
                        V v = (V) this.next.value;
                        if (v != null && v != this.next) {
                            if (SubMap.this.tooHigh(this.next.key)) {
                                this.next = null;
                                return;
                            } else {
                                this.nextValue = v;
                                return;
                            }
                        }
                    } else {
                        return;
                    }
                }
            }

            private void descend() {
                while (true) {
                    this.next = SubMap.this.m.findNear(this.lastReturned.key, 2);
                    if (this.next != null) {
                        V v = (V) this.next.value;
                        if (v != null && v != this.next) {
                            if (SubMap.this.tooLow(this.next.key)) {
                                this.next = null;
                                return;
                            } else {
                                this.nextValue = v;
                                return;
                            }
                        }
                    } else {
                        return;
                    }
                }
            }

            @Override
            public void remove() {
                Node<K, V> l = this.lastReturned;
                if (l != null) {
                    SubMap.this.m.remove(l.key);
                    this.lastReturned = null;
                    return;
                }
                throw new IllegalStateException();
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
        }
    }
}
