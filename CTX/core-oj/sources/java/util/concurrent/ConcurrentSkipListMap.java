package java.util.concurrent;

import android.R;
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
import java.util.Objects;
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

    private boolean casHead(HeadIndex<K, V> headIndex, HeadIndex<K, V> headIndex2) {
        return U.compareAndSwapObject(this, HEAD, headIndex, headIndex2);
    }

    static final class Node<K, V> {
        private static final long NEXT;
        private static final Unsafe U = Unsafe.getUnsafe();
        private static final long VALUE;
        final K key;
        volatile Node<K, V> next;
        volatile Object value;

        Node(K k, Object obj, Node<K, V> node) {
            this.key = k;
            this.value = obj;
            this.next = node;
        }

        Node(Node<K, V> node) {
            this.key = null;
            this.value = this;
            this.next = node;
        }

        boolean casValue(Object obj, Object obj2) {
            return U.compareAndSwapObject(this, VALUE, obj, obj2);
        }

        boolean casNext(Node<K, V> node, Node<K, V> node2) {
            return U.compareAndSwapObject(this, NEXT, node, node2);
        }

        boolean isMarker() {
            return this.value == this;
        }

        boolean isBaseHeader() {
            return this.value == ConcurrentSkipListMap.BASE_HEADER;
        }

        boolean appendMarker(Node<K, V> node) {
            return casNext(node, new Node<>(node));
        }

        void helpDelete(Node<K, V> node, Node<K, V> node2) {
            if (node2 == this.next && this == node.next) {
                if (node2 == null || node2.value != node2) {
                    casNext(node2, new Node<>(node2));
                } else {
                    node.casNext(this, node2.next);
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
            Object obj = this.value;
            if (obj == null || obj == this || obj == ConcurrentSkipListMap.BASE_HEADER) {
                return null;
            }
            return new AbstractMap.SimpleImmutableEntry<>(this.key, obj);
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

        Index(Node<K, V> node, Index<K, V> index, Index<K, V> index2) {
            this.node = node;
            this.down = index;
            this.right = index2;
        }

        final boolean casRight(Index<K, V> index, Index<K, V> index2) {
            return U.compareAndSwapObject(this, RIGHT, index, index2);
        }

        final boolean indexesDeletedNode() {
            return this.node.value == null;
        }

        final boolean link(Index<K, V> index, Index<K, V> index2) {
            Node<K, V> node = this.node;
            index2.right = index;
            return node.value != null && casRight(index, index2);
        }

        final boolean unlink(Index<K, V> index) {
            return this.node.value != null && casRight(index, index.right);
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

        HeadIndex(Node<K, V> node, Index<K, V> index, Index<K, V> index2, int i) {
            super(node, index, index2);
            this.level = i;
        }
    }

    static final int cpr(Comparator comparator, Object obj, Object obj2) {
        return comparator != null ? comparator.compare(obj, obj2) : ((Comparable) obj).compareTo(obj2);
    }

    private Node<K, V> findPredecessor(Object obj, Comparator<? super K> comparator) {
        if (obj == null) {
            throw new NullPointerException();
        }
        while (true) {
            Index<K, V> index = this.head;
            Index<K, V> index2 = index.right;
            while (true) {
                Index<K, V> index3 = index2;
                Index<K, V> index4 = index;
                index = index3;
                while (true) {
                    if (index == null) {
                        break;
                    }
                    Node<K, V> node = index.node;
                    K k = node.key;
                    if (node.value == null) {
                        if (!index4.unlink(index)) {
                            break;
                        }
                        index = index4.right;
                    } else if (cpr(comparator, obj, k) > 0) {
                        index2 = index.right;
                    }
                }
            }
        }
    }

    private Node<K, V> findNode(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> comparator = this.comparator;
        while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(obj, comparator);
            Node<K, V> node = nodeFindPredecessor;
            Node<K, V> node2 = nodeFindPredecessor.next;
            while (node2 != null) {
                Node<K, V> node3 = node2.next;
                if (node2 == node.next) {
                    Object obj2 = node2.value;
                    if (obj2 == null) {
                        node2.helpDelete(node, node3);
                    } else {
                        if (node.value == null || obj2 == node2) {
                            break;
                        }
                        int iCpr = cpr(comparator, obj, node2.key);
                        if (iCpr == 0) {
                            return node2;
                        }
                        if (iCpr >= 0) {
                            node = node2;
                            node2 = node3;
                        } else {
                            return null;
                        }
                    }
                }
            }
            return null;
        }
    }

    private V doGet(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> comparator = this.comparator;
        while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(obj, comparator);
            Node<K, V> node = nodeFindPredecessor;
            Node<K, V> node2 = nodeFindPredecessor.next;
            while (node2 != null) {
                Node<K, V> node3 = node2.next;
                if (node2 == node.next) {
                    V v = (V) node2.value;
                    if (v == null) {
                        node2.helpDelete(node, node3);
                    } else {
                        if (node.value == null || v == node2) {
                            break;
                        }
                        int iCpr = cpr(comparator, obj, node2.key);
                        if (iCpr == 0) {
                            return v;
                        }
                        if (iCpr >= 0) {
                            node = node2;
                            node2 = node3;
                        } else {
                            return null;
                        }
                    }
                }
            }
            return null;
        }
    }

    private V doPut(K k, V v, boolean z) {
        if (k == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> comparator = this.comparator;
        while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(k, comparator);
            Node<K, V> node = nodeFindPredecessor;
            Node<K, V> node2 = nodeFindPredecessor.next;
            while (true) {
                if (node2 == null) {
                    break;
                }
                Node<K, V> node3 = node2.next;
                if (node2 == node.next) {
                    V v2 = (V) node2.value;
                    if (v2 == null) {
                        node2.helpDelete(node, node3);
                        break;
                    }
                    if (node.value == null || v2 == node2) {
                        break;
                    }
                    int iCpr = cpr(comparator, k, node2.key);
                    if (iCpr > 0) {
                        node = node2;
                        node2 = node3;
                    } else if (iCpr == 0) {
                        if (z || node2.casValue(v2, v)) {
                            break;
                        }
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
            Node<K, V> node = nodeFindPredecessor;
            Node<K, V> node2 = nodeFindPredecessor.next;
            while (true) {
                if (node2 == null) {
                    break loop0;
                }
                Node<K, V> node3 = node2.next;
                if (node2 == node.next) {
                    V v = (V) node2.value;
                    if (v == null) {
                        node2.helpDelete(node, node3);
                        break;
                    }
                    if (node.value != null && v != node2) {
                        int iCpr = cpr(comparator, obj, node2.key);
                        if (iCpr < 0) {
                            break loop0;
                        }
                        if (iCpr > 0) {
                            node = node2;
                            node2 = node3;
                        } else {
                            if (obj2 != null && !obj2.equals(v)) {
                                break;
                            }
                            if (node2.casValue(v, null)) {
                                if (!node2.appendMarker(node3) || !node.casNext(node2, node3)) {
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
                        break;
                    }
                }
            }
        }
    }

    private void tryReduceLevel() {
        HeadIndex<K, V> headIndex;
        HeadIndex headIndex2;
        HeadIndex<K, V> headIndex3 = this.head;
        if (headIndex3.level > 3 && (headIndex = (HeadIndex) headIndex3.down) != null && (headIndex2 = (HeadIndex) headIndex.down) != null && headIndex2.right == null && headIndex.right == null && headIndex3.right == null && casHead(headIndex3, headIndex) && headIndex3.right != null) {
            casHead(headIndex, headIndex3);
        }
    }

    final Node<K, V> findFirst() {
        while (true) {
            Node<K, V> node = this.head.node;
            Node<K, V> node2 = node.next;
            if (node2 == null) {
                return null;
            }
            if (node2.value != null) {
                return node2;
            }
            node2.helpDelete(node, node2.next);
        }
    }

    private Map.Entry<K, V> doRemoveFirstEntry() {
        while (true) {
            Node<K, V> node = this.head.node;
            Node<K, V> node2 = node.next;
            if (node2 == null) {
                return null;
            }
            Node<K, V> node3 = node2.next;
            if (node2 == node.next) {
                Object obj = node2.value;
                if (obj == null) {
                    node2.helpDelete(node, node3);
                } else if (node2.casValue(obj, null)) {
                    if (!node2.appendMarker(node3) || !node.casNext(node2, node3)) {
                        findFirst();
                    }
                    clearIndexToFirst();
                    return new AbstractMap.SimpleImmutableEntry(node2.key, obj);
                }
            }
        }
    }

    private void clearIndexToFirst() {
        loop0: while (true) {
            Index index = this.head;
            do {
                Index<K, V> index2 = index.right;
                if (index2 == null || !index2.indexesDeletedNode() || index.unlink(index2)) {
                    index = index.down;
                }
            } while (index != null);
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
            Node<K, V> nodeFindPredecessorOfLast = findPredecessorOfLast();
            Node<K, V> node = nodeFindPredecessorOfLast.next;
            if (node == null) {
                if (nodeFindPredecessorOfLast.isBaseHeader()) {
                    return null;
                }
            } else {
                Node<K, V> node2 = nodeFindPredecessorOfLast;
                Node<K, V> node3 = node;
                while (true) {
                    Node<K, V> node4 = node3.next;
                    if (node3 == node2.next) {
                        Object obj = node3.value;
                        if (obj == null) {
                            node3.helpDelete(node2, node4);
                            break;
                        }
                        if (node2.value == null || obj == node3) {
                            break;
                        }
                        if (node4 != null) {
                            node2 = node3;
                            node3 = node4;
                        } else if (node3.casValue(obj, null)) {
                            K k = node3.key;
                            if (!node3.appendMarker(node4) || !node2.casNext(node3, node4)) {
                                findNode(k);
                            } else {
                                findPredecessor(k, this.comparator);
                                if (this.head.right == null) {
                                    tryReduceLevel();
                                }
                            }
                            return new AbstractMap.SimpleImmutableEntry(k, obj);
                        }
                    }
                }
            }
        }
    }

    final Node<K, V> findLast() {
        Node<K, V> node;
        Index<K, V> index = this.head;
        loop0: while (true) {
            Index<K, V> index2 = index.right;
            if (index2 != null) {
                if (index2.indexesDeletedNode()) {
                    index.unlink(index2);
                    index = this.head;
                } else {
                    index = index2;
                }
            } else {
                index2 = index.down;
                if (index2 == null) {
                    Node<K, V> node2 = index.node;
                    node = node2;
                    Node<K, V> node3 = node2.next;
                    while (node3 != null) {
                        Node<K, V> node4 = node3.next;
                        if (node3 == node.next) {
                            Object obj = node3.value;
                            if (obj == null) {
                                node3.helpDelete(node, node4);
                            } else if (node.value != null && obj != node3) {
                                node = node3;
                                node3 = node4;
                            }
                        }
                        index = this.head;
                    }
                    break loop0;
                }
                index = index2;
            }
        }
        if (node.isBaseHeader()) {
            return null;
        }
        return node;
    }

    private Node<K, V> findPredecessorOfLast() {
        Index<K, V> index;
        while (true) {
            Index<K, V> index2 = this.head;
            while (true) {
                index = index2.right;
                if (index != null) {
                    if (index.indexesDeletedNode()) {
                        break;
                    }
                    if (index.node.next != null) {
                        continue;
                    }
                } else {
                    index = index2.down;
                    if (index == null) {
                        return index2.node;
                    }
                }
                index2 = index;
            }
            index2.unlink(index);
        }
    }

    final Node<K, V> findNear(K k, int i, Comparator<? super K> comparator) {
        if (k == null) {
            throw new NullPointerException();
        }
        loop0: while (true) {
            Node<K, V> nodeFindPredecessor = findPredecessor(k, comparator);
            Node<K, V> node = nodeFindPredecessor;
            Node<K, V> node2 = nodeFindPredecessor.next;
            while (node2 != null) {
                Node<K, V> node3 = node2.next;
                if (node2 == node.next) {
                    Object obj = node2.value;
                    if (obj == null) {
                        node2.helpDelete(node, node3);
                    } else if (node.value != null && obj != node2) {
                        int iCpr = cpr(comparator, k, node2.key);
                        if ((iCpr == 0 && (i & 1) != 0) || (iCpr < 0 && (i & 2) == 0)) {
                            break loop0;
                        }
                        if (iCpr > 0 || (i & 2) == 0) {
                            node = node2;
                            node2 = node3;
                        } else {
                            if (node.isBaseHeader()) {
                                return null;
                            }
                            return node;
                        }
                    } else {
                        break;
                    }
                }
            }
            if ((i & 2) == 0 || node.isBaseHeader()) {
                return null;
            }
            return node;
        }
    }

    final AbstractMap.SimpleImmutableEntry<K, V> getNear(K k, int i) {
        AbstractMap.SimpleImmutableEntry<K, V> simpleImmutableEntryCreateSnapshot;
        Comparator<? super K> comparator = this.comparator;
        do {
            Node<K, V> nodeFindNear = findNear(k, i, comparator);
            if (nodeFindNear == null) {
                return null;
            }
            simpleImmutableEntryCreateSnapshot = nodeFindNear.createSnapshot();
        } while (simpleImmutableEntryCreateSnapshot == null);
        return simpleImmutableEntryCreateSnapshot;
    }

    public ConcurrentSkipListMap() {
        this.comparator = null;
        initialize();
    }

    public ConcurrentSkipListMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
        initialize();
    }

    public ConcurrentSkipListMap(Map<? extends K, ? extends V> map) {
        this.comparator = null;
        initialize();
        putAll(map);
    }

    public ConcurrentSkipListMap(SortedMap<K, ? extends V> sortedMap) {
        this.comparator = sortedMap.comparator();
        initialize();
        buildFromSorted(sortedMap);
    }

    @Override
    public ConcurrentSkipListMap<K, V> clone() {
        try {
            ConcurrentSkipListMap<K, V> concurrentSkipListMap = (ConcurrentSkipListMap) super.clone();
            concurrentSkipListMap.initialize();
            concurrentSkipListMap.buildFromSorted(this);
            return concurrentSkipListMap;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    private void buildFromSorted(SortedMap<K, ? extends V> sortedMap) {
        int i;
        if (sortedMap == null) {
            throw new NullPointerException();
        }
        HeadIndex<K, V> headIndex = this.head;
        Node<K, V> node = headIndex.node;
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 <= headIndex.level; i2++) {
            arrayList.add(null);
        }
        Index<K, V> index = headIndex;
        for (int i3 = headIndex.level; i3 > 0; i3--) {
            arrayList.set(i3, index);
            index = index.down;
        }
        for (Map.Entry<K, ? extends V> entry : sortedMap.entrySet()) {
            int iNextInt = ThreadLocalRandom.current().nextInt();
            int i4 = 1;
            if (((-2147483647) & iNextInt) == 0) {
                int i5 = iNextInt;
                i = 0;
                do {
                    i++;
                    i5 >>>= 1;
                } while ((i5 & 1) != 0);
                if (i > headIndex.level) {
                    i = headIndex.level + 1;
                }
            } else {
                i = 0;
            }
            K key = entry.getKey();
            V value = entry.getValue();
            if (key == null || value == null) {
                throw new NullPointerException();
            }
            Node<K, V> node2 = new Node<>(key, value, null);
            node.next = node2;
            if (i > 0) {
                Index<K, V> index2 = null;
                while (i4 <= i) {
                    Index<K, V> index3 = new Index<>(node2, index2, null);
                    if (i4 > headIndex.level) {
                        headIndex = new HeadIndex<>(headIndex.node, headIndex, index3, i4);
                    }
                    if (i4 < arrayList.size()) {
                        ((Index) arrayList.get(i4)).right = index3;
                        arrayList.set(i4, index3);
                    } else {
                        arrayList.add(index3);
                    }
                    i4++;
                    index2 = index3;
                }
            }
            node = node2;
        }
        this.head = headIndex;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        for (Node<K, V> nodeFindFirst = findFirst(); nodeFindFirst != null; nodeFindFirst = nodeFindFirst.next) {
            V validValue = nodeFindFirst.getValidValue();
            if (validValue != null) {
                objectOutputStream.writeObject(nodeFindFirst.key);
                objectOutputStream.writeObject(validValue);
            }
        }
        objectOutputStream.writeObject(null);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        int i;
        objectInputStream.defaultReadObject();
        initialize();
        HeadIndex<K, V> headIndex = this.head;
        Node<K, V> node = headIndex.node;
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 <= headIndex.level; i2++) {
            arrayList.add(null);
        }
        Index<K, V> index = headIndex;
        for (int i3 = headIndex.level; i3 > 0; i3--) {
            arrayList.set(i3, index);
            index = index.down;
        }
        while (true) {
            Object object = objectInputStream.readObject();
            if (object != null) {
                Object object2 = objectInputStream.readObject();
                if (object2 == null) {
                    throw new NullPointerException();
                }
                int iNextInt = ThreadLocalRandom.current().nextInt();
                int i4 = 1;
                if (((-2147483647) & iNextInt) == 0) {
                    int i5 = iNextInt;
                    i = 0;
                    do {
                        i++;
                        i5 >>>= 1;
                    } while ((i5 & 1) != 0);
                    if (i > headIndex.level) {
                        i = headIndex.level + 1;
                    }
                } else {
                    i = 0;
                }
                Node<K, V> node2 = new Node<>(object, object2, null);
                node.next = node2;
                if (i > 0) {
                    Index<K, V> index2 = null;
                    while (i4 <= i) {
                        Index<K, V> index3 = new Index<>(node2, index2, null);
                        if (i4 > headIndex.level) {
                            headIndex = new HeadIndex<>(headIndex.node, headIndex, index3, i4);
                        }
                        if (i4 < arrayList.size()) {
                            ((Index) arrayList.get(i4)).right = index3;
                            arrayList.set(i4, index3);
                        } else {
                            arrayList.add(index3);
                        }
                        i4++;
                        index2 = index3;
                    }
                }
                node = node2;
            } else {
                this.head = headIndex;
                return;
            }
        }
    }

    @Override
    public boolean containsKey(Object obj) {
        return doGet(obj) != null;
    }

    @Override
    public V get(Object obj) {
        return doGet(obj);
    }

    @Override
    public V getOrDefault(Object obj, V v) {
        V vDoGet = doGet(obj);
        return vDoGet == null ? v : vDoGet;
    }

    @Override
    public V put(K k, V v) {
        if (v == null) {
            throw new NullPointerException();
        }
        return doPut(k, v, false);
    }

    @Override
    public V remove(Object obj) {
        return doRemove(obj, null);
    }

    @Override
    public boolean containsValue(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        for (Node<K, V> nodeFindFirst = findFirst(); nodeFindFirst != null; nodeFindFirst = nodeFindFirst.next) {
            V validValue = nodeFindFirst.getValidValue();
            if (validValue != null && obj.equals(validValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        long j = 0;
        for (Node<K, V> nodeFindFirst = findFirst(); nodeFindFirst != null; nodeFindFirst = nodeFindFirst.next) {
            if (nodeFindFirst.getValidValue() != null) {
                j++;
            }
        }
        return j >= 2147483647L ? Integer.MAX_VALUE : (int) j;
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
    public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
        V vApply;
        if (k == null || function == null) {
            throw new NullPointerException();
        }
        V vDoGet = doGet(k);
        if (vDoGet != null || (vApply = function.apply(k)) == null) {
            return vDoGet;
        }
        V vDoPut = doPut(k, vApply, true);
        return vDoPut == null ? vApply : vDoPut;
    }

    @Override
    public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        if (k == null || biFunction == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> nodeFindNode = findNode(k);
            if (nodeFindNode != null) {
                R.bool boolVar = (Object) nodeFindNode.value;
                if (boolVar != null) {
                    V vApply = biFunction.apply(k, boolVar);
                    if (vApply != null) {
                        if (nodeFindNode.casValue(boolVar, vApply)) {
                            return vApply;
                        }
                    } else if (doRemove(k, boolVar) != null) {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        if (k == null || biFunction == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> nodeFindNode = findNode(k);
            if (nodeFindNode == null) {
                V vApply = biFunction.apply(k, null);
                if (vApply == null) {
                    break;
                }
                if (doPut(k, vApply, true) == null) {
                    return vApply;
                }
            } else {
                R.bool boolVar = (Object) nodeFindNode.value;
                if (boolVar == null) {
                    continue;
                } else {
                    V vApply2 = biFunction.apply(k, boolVar);
                    if (vApply2 != null) {
                        if (nodeFindNode.casValue(boolVar, vApply2)) {
                            return vApply2;
                        }
                    } else if (doRemove(k, boolVar) != null) {
                        break;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        if (k == null || v == null || biFunction == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> nodeFindNode = findNode(k);
            if (nodeFindNode == null) {
                if (doPut(k, v, true) == null) {
                    return v;
                }
            } else {
                R.bool boolVar = (Object) nodeFindNode.value;
                if (boolVar == null) {
                    continue;
                } else {
                    V vApply = biFunction.apply(boolVar, v);
                    if (vApply != null) {
                        if (nodeFindNode.casValue(boolVar, vApply)) {
                            return vApply;
                        }
                    } else if (doRemove(k, boolVar) != null) {
                        return null;
                    }
                }
            }
        }
    }

    @Override
    public NavigableSet<K> keySet() {
        KeySet<K, V> keySet = this.keySet;
        if (keySet != null) {
            return keySet;
        }
        KeySet<K, V> keySet2 = new KeySet<>(this);
        this.keySet = keySet2;
        return keySet2;
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        KeySet<K, V> keySet = this.keySet;
        if (keySet != null) {
            return keySet;
        }
        KeySet<K, V> keySet2 = new KeySet<>(this);
        this.keySet = keySet2;
        return keySet2;
    }

    @Override
    public Collection<V> values() {
        Values<K, V> values = this.values;
        if (values != null) {
            return values;
        }
        Values<K, V> values2 = new Values<>(this);
        this.values = values2;
        return values2;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        EntrySet<K, V> entrySet = this.entrySet;
        if (entrySet != null) {
            return entrySet;
        }
        EntrySet<K, V> entrySet2 = new EntrySet<>(this);
        this.entrySet = entrySet2;
        return entrySet2;
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        ConcurrentNavigableMap<K, V> concurrentNavigableMap = this.descendingMap;
        if (concurrentNavigableMap != null) {
            return concurrentNavigableMap;
        }
        SubMap subMap = new SubMap(this, null, false, null, false, true);
        this.descendingMap = subMap;
        return subMap;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        Map map = (Map) obj;
        try {
            for (Map.Entry<K, V> entry : entrySet()) {
                if (!entry.getValue().equals(map.get(entry.getKey()))) {
                    return false;
                }
            }
            for (Map.Entry<K, V> entry2 : map.entrySet()) {
                K key = entry2.getKey();
                V value = entry2.getValue();
                if (key == null || value == null || !value.equals(get(key))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @Override
    public V putIfAbsent(K k, V v) {
        if (v == null) {
            throw new NullPointerException();
        }
        return doPut(k, v, true);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        if (obj != null) {
            return (obj2 == null || doRemove(obj, obj2) == null) ? false : true;
        }
        throw new NullPointerException();
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        if (k == null || v == null || v2 == null) {
            throw new NullPointerException();
        }
        while (true) {
            Node<K, V> nodeFindNode = findNode(k);
            if (nodeFindNode == null) {
                return false;
            }
            Object obj = nodeFindNode.value;
            if (obj != null) {
                if (!v.equals(obj)) {
                    return false;
                }
                if (nodeFindNode.casValue(obj, v2)) {
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
        Node<K, V> nodeFindFirst = findFirst();
        if (nodeFindFirst == null) {
            throw new NoSuchElementException();
        }
        return nodeFindFirst.key;
    }

    @Override
    public K lastKey() {
        Node<K, V> nodeFindLast = findLast();
        if (nodeFindLast == null) {
            throw new NoSuchElementException();
        }
        return nodeFindLast.key;
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
        if (k == null || k2 == null) {
            throw new NullPointerException();
        }
        return new SubMap(this, k, z, k2, z2, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K k, boolean z) {
        if (k == null) {
            throw new NullPointerException();
        }
        return new SubMap(this, null, false, k, z, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K k, boolean z) {
        if (k == null) {
            throw new NullPointerException();
        }
        return new SubMap(this, k, z, null, false, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K k, K k2) {
        return subMap((Object) k, true, (Object) k2, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K k) {
        return headMap((Object) k, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K k) {
        return tailMap((Object) k, true);
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K k) {
        return getNear(k, 2);
    }

    @Override
    public K lowerKey(K k) {
        Node<K, V> nodeFindNear = findNear(k, 2, this.comparator);
        if (nodeFindNear == null) {
            return null;
        }
        return nodeFindNear.key;
    }

    @Override
    public Map.Entry<K, V> floorEntry(K k) {
        return getNear(k, 3);
    }

    @Override
    public K floorKey(K k) {
        Node<K, V> nodeFindNear = findNear(k, 3, this.comparator);
        if (nodeFindNear == null) {
            return null;
        }
        return nodeFindNear.key;
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K k) {
        return getNear(k, 1);
    }

    @Override
    public K ceilingKey(K k) {
        Node<K, V> nodeFindNear = findNear(k, 1, this.comparator);
        if (nodeFindNear == null) {
            return null;
        }
        return nodeFindNear.key;
    }

    @Override
    public Map.Entry<K, V> higherEntry(K k) {
        return getNear(k, 0);
    }

    @Override
    public K higherKey(K k) {
        Node<K, V> nodeFindNear = findNear(k, 0, this.comparator);
        if (nodeFindNear == null) {
            return null;
        }
        return nodeFindNear.key;
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        AbstractMap.SimpleImmutableEntry<K, V> simpleImmutableEntryCreateSnapshot;
        do {
            Node<K, V> nodeFindFirst = findFirst();
            if (nodeFindFirst == null) {
                return null;
            }
            simpleImmutableEntryCreateSnapshot = nodeFindFirst.createSnapshot();
        } while (simpleImmutableEntryCreateSnapshot == null);
        return simpleImmutableEntryCreateSnapshot;
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        AbstractMap.SimpleImmutableEntry<K, V> simpleImmutableEntryCreateSnapshot;
        do {
            Node<K, V> nodeFindLast = findLast();
            if (nodeFindLast == null) {
                return null;
            }
            simpleImmutableEntryCreateSnapshot = nodeFindLast.createSnapshot();
        } while (simpleImmutableEntryCreateSnapshot == null);
        return simpleImmutableEntryCreateSnapshot;
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
                if (nodeFindFirst != null) {
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
                Node<K, V> node = this.next.next;
                this.next = node;
                if (node != null) {
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
            Node<K, V> node = this.lastReturned;
            if (node == null) {
                throw new IllegalStateException();
            }
            ConcurrentSkipListMap.this.remove(node.key);
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
            Node<K, V> node = this.next;
            advance();
            return node.key;
        }
    }

    final class EntryIterator extends ConcurrentSkipListMap<K, V>.Iter<Map.Entry<K, V>> {
        EntryIterator() {
            super();
        }

        @Override
        public Map.Entry<K, V> next() {
            Node<K, V> node = this.next;
            V v = this.nextValue;
            advance();
            return new AbstractMap.SimpleImmutableEntry(node.key, v);
        }
    }

    static final <E> List<E> toList(Collection<E> collection) {
        ArrayList arrayList = new ArrayList();
        Iterator<E> it = collection.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next());
        }
        return arrayList;
    }

    static final class KeySet<K, V> extends AbstractSet<K> implements NavigableSet<K> {
        final ConcurrentNavigableMap<K, V> m;

        KeySet(ConcurrentNavigableMap<K, V> concurrentNavigableMap) {
            this.m = concurrentNavigableMap;
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
        public boolean contains(Object obj) {
            return this.m.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            return this.m.remove(obj) != null;
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public K lower(K k) {
            return this.m.lowerKey(k);
        }

        @Override
        public K floor(K k) {
            return this.m.floorKey(k);
        }

        @Override
        public K ceiling(K k) {
            return this.m.ceilingKey(k);
        }

        @Override
        public K higher(K k) {
            return this.m.higherKey(k);
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.m.comparator();
        }

        @Override
        public K first() {
            return this.m.firstKey();
        }

        @Override
        public K last() {
            return this.m.lastKey();
        }

        @Override
        public K pollFirst() {
            Map.Entry<K, V> entryPollFirstEntry = this.m.pollFirstEntry();
            if (entryPollFirstEntry == null) {
                return null;
            }
            return entryPollFirstEntry.getKey();
        }

        @Override
        public K pollLast() {
            Map.Entry<K, V> entryPollLastEntry = this.m.pollLastEntry();
            if (entryPollLastEntry == null) {
                return null;
            }
            return entryPollLastEntry.getKey();
        }

        @Override
        public Iterator<K> iterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                ConcurrentSkipListMap concurrentSkipListMap = (ConcurrentSkipListMap) this.m;
                Objects.requireNonNull(concurrentSkipListMap);
                return new KeyIterator();
            }
            SubMap subMap = (SubMap) this.m;
            Objects.requireNonNull(subMap);
            return new SubMap.SubMapKeyIterator();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Set)) {
                return false;
            }
            Collection<?> collection = (Collection) obj;
            try {
                if (containsAll(collection)) {
                    if (collection.containsAll(this)) {
                        return true;
                    }
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
        public NavigableSet<K> subSet(K k, boolean z, K k2, boolean z2) {
            return new KeySet(this.m.subMap((Object) k, z, (Object) k2, z2));
        }

        @Override
        public NavigableSet<K> headSet(K k, boolean z) {
            return new KeySet(this.m.headMap((Object) k, z));
        }

        @Override
        public NavigableSet<K> tailSet(K k, boolean z) {
            return new KeySet(this.m.tailMap((Object) k, z));
        }

        @Override
        public NavigableSet<K> subSet(K k, K k2) {
            return subSet(k, true, k2, false);
        }

        @Override
        public NavigableSet<K> headSet(K k) {
            return headSet(k, false);
        }

        @Override
        public NavigableSet<K> tailSet(K k) {
            return tailSet(k, true);
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
            Objects.requireNonNull(subMap);
            return new SubMap.SubMapKeyIterator();
        }
    }

    static final class Values<K, V> extends AbstractCollection<V> {
        final ConcurrentNavigableMap<K, V> m;

        Values(ConcurrentNavigableMap<K, V> concurrentNavigableMap) {
            this.m = concurrentNavigableMap;
        }

        @Override
        public Iterator<V> iterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                ConcurrentSkipListMap concurrentSkipListMap = (ConcurrentSkipListMap) this.m;
                Objects.requireNonNull(concurrentSkipListMap);
                return new ValueIterator();
            }
            SubMap subMap = (SubMap) this.m;
            Objects.requireNonNull(subMap);
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
        public boolean contains(Object obj) {
            return this.m.containsValue(obj);
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
            Objects.requireNonNull(subMap);
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
            Objects.requireNonNull(subMap);
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

        EntrySet(ConcurrentNavigableMap<K, V> concurrentNavigableMap) {
            this.m = concurrentNavigableMap;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            if (this.m instanceof ConcurrentSkipListMap) {
                ConcurrentSkipListMap concurrentSkipListMap = (ConcurrentSkipListMap) this.m;
                Objects.requireNonNull(concurrentSkipListMap);
                return new EntryIterator();
            }
            SubMap subMap = (SubMap) this.m;
            Objects.requireNonNull(subMap);
            return new SubMap.SubMapEntryIterator();
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            V v = this.m.get(entry.getKey());
            return v != null && v.equals(entry.getValue());
        }

        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            return this.m.remove(entry.getKey(), entry.getValue());
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
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Set)) {
                return false;
            }
            Collection<?> collection = (Collection) obj;
            try {
                if (containsAll(collection)) {
                    if (collection.containsAll(this)) {
                        return true;
                    }
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
            Objects.requireNonNull(subMap);
            return new SubMap.SubMapEntryIterator();
        }

        @Override
        public boolean removeIf(Predicate<? super Map.Entry<K, V>> predicate) {
            if (predicate == null) {
                throw new NullPointerException();
            }
            if (this.m instanceof ConcurrentSkipListMap) {
                return ((ConcurrentSkipListMap) this.m).removeEntryIf(predicate);
            }
            SubMap subMap = (SubMap) this.m;
            Objects.requireNonNull(subMap);
            SubMap.SubMapEntryIterator subMapEntryIterator = new SubMap.SubMapEntryIterator();
            boolean z = false;
            while (subMapEntryIterator.hasNext()) {
                Map.Entry<K, V> next = subMapEntryIterator.next();
                if (predicate.test(next) && this.m.remove(next.getKey(), next.getValue())) {
                    z = true;
                }
            }
            return z;
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

        SubMap(ConcurrentSkipListMap<K, V> concurrentSkipListMap, K k, boolean z, K k2, boolean z2, boolean z3) {
            Comparator<? super K> comparator = concurrentSkipListMap.comparator;
            if (k != null && k2 != null && ConcurrentSkipListMap.cpr(comparator, k, k2) > 0) {
                throw new IllegalArgumentException("inconsistent range");
            }
            this.m = concurrentSkipListMap;
            this.lo = k;
            this.hi = k2;
            this.loInclusive = z;
            this.hiInclusive = z2;
            this.isDescending = z3;
        }

        boolean tooLow(Object obj, Comparator<? super K> comparator) {
            int iCpr;
            return this.lo != null && ((iCpr = ConcurrentSkipListMap.cpr(comparator, obj, this.lo)) < 0 || (iCpr == 0 && !this.loInclusive));
        }

        boolean tooHigh(Object obj, Comparator<? super K> comparator) {
            int iCpr;
            return this.hi != null && ((iCpr = ConcurrentSkipListMap.cpr(comparator, obj, this.hi)) > 0 || (iCpr == 0 && !this.hiInclusive));
        }

        boolean inBounds(Object obj, Comparator<? super K> comparator) {
            return (tooLow(obj, comparator) || tooHigh(obj, comparator)) ? false : true;
        }

        void checkKeyBounds(K k, Comparator<? super K> comparator) {
            if (k == null) {
                throw new NullPointerException();
            }
            if (!inBounds(k, comparator)) {
                throw new IllegalArgumentException("key out of range");
            }
        }

        boolean isBeforeEnd(Node<K, V> node, Comparator<? super K> comparator) {
            K k;
            if (node == null) {
                return false;
            }
            if (this.hi == null || (k = node.key) == null) {
                return true;
            }
            int iCpr = ConcurrentSkipListMap.cpr(comparator, k, this.hi);
            return iCpr <= 0 && (iCpr != 0 || this.hiInclusive);
        }

        Node<K, V> loNode(Comparator<? super K> comparator) {
            if (this.lo == null) {
                return this.m.findFirst();
            }
            if (this.loInclusive) {
                return this.m.findNear(this.lo, 1, comparator);
            }
            return this.m.findNear(this.lo, 0, comparator);
        }

        Node<K, V> hiNode(Comparator<? super K> comparator) {
            if (this.hi == null) {
                return this.m.findLast();
            }
            if (this.hiInclusive) {
                return this.m.findNear(this.hi, 3, comparator);
            }
            return this.m.findNear(this.hi, 2, comparator);
        }

        K lowestKey() {
            Comparator<? super K> comparator = this.m.comparator;
            Node<K, V> nodeLoNode = loNode(comparator);
            if (isBeforeEnd(nodeLoNode, comparator)) {
                return nodeLoNode.key;
            }
            throw new NoSuchElementException();
        }

        K highestKey() {
            Comparator<? super K> comparator = this.m.comparator;
            Node<K, V> nodeHiNode = hiNode(comparator);
            if (nodeHiNode != null) {
                K k = nodeHiNode.key;
                if (inBounds(k, comparator)) {
                    return k;
                }
            }
            throw new NoSuchElementException();
        }

        Map.Entry<K, V> lowestEntry() {
            AbstractMap.SimpleImmutableEntry<K, V> simpleImmutableEntryCreateSnapshot;
            Comparator<? super K> comparator = this.m.comparator;
            do {
                Node<K, V> nodeLoNode = loNode(comparator);
                if (!isBeforeEnd(nodeLoNode, comparator)) {
                    return null;
                }
                simpleImmutableEntryCreateSnapshot = nodeLoNode.createSnapshot();
            } while (simpleImmutableEntryCreateSnapshot == null);
            return simpleImmutableEntryCreateSnapshot;
        }

        Map.Entry<K, V> highestEntry() {
            AbstractMap.SimpleImmutableEntry<K, V> simpleImmutableEntryCreateSnapshot;
            Comparator<? super K> comparator = this.m.comparator;
            do {
                Node<K, V> nodeHiNode = hiNode(comparator);
                if (nodeHiNode == null || !inBounds(nodeHiNode.key, comparator)) {
                    return null;
                }
                simpleImmutableEntryCreateSnapshot = nodeHiNode.createSnapshot();
            } while (simpleImmutableEntryCreateSnapshot == null);
            return simpleImmutableEntryCreateSnapshot;
        }

        Map.Entry<K, V> removeLowest() {
            K k;
            V vDoRemove;
            Comparator<? super K> comparator = this.m.comparator;
            do {
                Node<K, V> nodeLoNode = loNode(comparator);
                if (nodeLoNode == null) {
                    return null;
                }
                k = nodeLoNode.key;
                if (!inBounds(k, comparator)) {
                    return null;
                }
                vDoRemove = this.m.doRemove(k, null);
            } while (vDoRemove == null);
            return new AbstractMap.SimpleImmutableEntry(k, vDoRemove);
        }

        Map.Entry<K, V> removeHighest() {
            K k;
            V vDoRemove;
            Comparator<? super K> comparator = this.m.comparator;
            do {
                Node<K, V> nodeHiNode = hiNode(comparator);
                if (nodeHiNode == null) {
                    return null;
                }
                k = nodeHiNode.key;
                if (!inBounds(k, comparator)) {
                    return null;
                }
                vDoRemove = this.m.doRemove(k, null);
            } while (vDoRemove == null);
            return new AbstractMap.SimpleImmutableEntry(k, vDoRemove);
        }

        Map.Entry<K, V> getNearEntry(K k, int i) {
            K k2;
            V validValue;
            Comparator<? super K> comparator = this.m.comparator;
            if (this.isDescending) {
                if ((i & 2) == 0) {
                    i |= 2;
                } else {
                    i &= -3;
                }
            }
            if (tooLow(k, comparator)) {
                if ((i & 2) != 0) {
                    return null;
                }
                return lowestEntry();
            }
            if (tooHigh(k, comparator)) {
                if ((i & 2) != 0) {
                    return highestEntry();
                }
                return null;
            }
            do {
                Node<K, V> nodeFindNear = this.m.findNear(k, i, comparator);
                if (nodeFindNear == null || !inBounds(nodeFindNear.key, comparator)) {
                    return null;
                }
                k2 = nodeFindNear.key;
                validValue = nodeFindNear.getValidValue();
            } while (validValue == null);
            return new AbstractMap.SimpleImmutableEntry(k2, validValue);
        }

        K getNearKey(K k, int i) {
            Node<K, V> nodeFindNear;
            K k2;
            Node<K, V> nodeHiNode;
            Comparator<? super K> comparator = this.m.comparator;
            if (this.isDescending) {
                if ((i & 2) == 0) {
                    i |= 2;
                } else {
                    i &= -3;
                }
            }
            if (tooLow(k, comparator)) {
                if ((i & 2) == 0) {
                    Node<K, V> nodeLoNode = loNode(comparator);
                    if (isBeforeEnd(nodeLoNode, comparator)) {
                        return nodeLoNode.key;
                    }
                }
                return null;
            }
            if (tooHigh(k, comparator)) {
                if ((i & 2) != 0 && (nodeHiNode = hiNode(comparator)) != null) {
                    K k3 = nodeHiNode.key;
                    if (inBounds(k3, comparator)) {
                        return k3;
                    }
                }
                return null;
            }
            do {
                nodeFindNear = this.m.findNear(k, i, comparator);
                if (nodeFindNear == null || !inBounds(nodeFindNear.key, comparator)) {
                    return null;
                }
                k2 = nodeFindNear.key;
            } while (nodeFindNear.getValidValue() == null);
            return k2;
        }

        @Override
        public boolean containsKey(Object obj) {
            if (obj != null) {
                return inBounds(obj, this.m.comparator) && this.m.containsKey(obj);
            }
            throw new NullPointerException();
        }

        @Override
        public V get(Object obj) {
            if (obj == null) {
                throw new NullPointerException();
            }
            if (inBounds(obj, this.m.comparator)) {
                return this.m.get(obj);
            }
            return null;
        }

        @Override
        public V put(K k, V v) {
            checkKeyBounds(k, this.m.comparator);
            return this.m.put(k, v);
        }

        @Override
        public V remove(Object obj) {
            if (inBounds(obj, this.m.comparator)) {
                return this.m.remove(obj);
            }
            return null;
        }

        @Override
        public int size() {
            Comparator<? super K> comparator = this.m.comparator;
            long j = 0;
            for (Node<K, V> nodeLoNode = loNode(comparator); isBeforeEnd(nodeLoNode, comparator); nodeLoNode = nodeLoNode.next) {
                if (nodeLoNode.getValidValue() != null) {
                    j++;
                }
            }
            return j >= 2147483647L ? Integer.MAX_VALUE : (int) j;
        }

        @Override
        public boolean isEmpty() {
            Comparator<? super K> comparator = this.m.comparator;
            return !isBeforeEnd(loNode(comparator), comparator);
        }

        @Override
        public boolean containsValue(Object obj) {
            if (obj == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> comparator = this.m.comparator;
            for (Node<K, V> nodeLoNode = loNode(comparator); isBeforeEnd(nodeLoNode, comparator); nodeLoNode = nodeLoNode.next) {
                V validValue = nodeLoNode.getValidValue();
                if (validValue != null && obj.equals(validValue)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            Comparator<? super K> comparator = this.m.comparator;
            for (Node<K, V> nodeLoNode = loNode(comparator); isBeforeEnd(nodeLoNode, comparator); nodeLoNode = nodeLoNode.next) {
                if (nodeLoNode.getValidValue() != null) {
                    this.m.remove(nodeLoNode.key);
                }
            }
        }

        @Override
        public V putIfAbsent(K k, V v) {
            checkKeyBounds(k, this.m.comparator);
            return this.m.putIfAbsent(k, v);
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            return inBounds(obj, this.m.comparator) && this.m.remove(obj, obj2);
        }

        @Override
        public boolean replace(K k, V v, V v2) {
            checkKeyBounds(k, this.m.comparator);
            return this.m.replace(k, v, v2);
        }

        @Override
        public V replace(K k, V v) {
            checkKeyBounds(k, this.m.comparator);
            return this.m.replace(k, v);
        }

        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> comparator = this.m.comparator();
            if (this.isDescending) {
                return Collections.reverseOrder(comparator);
            }
            return comparator;
        }

        SubMap<K, V> newSubMap(K k, boolean z, K k2, boolean z2) {
            Comparator<? super K> comparator = this.m.comparator;
            if (this.isDescending) {
                k2 = k;
                k = k2;
                z2 = z;
                z = z2;
            }
            if (this.lo != null) {
                if (k == null) {
                    k = this.lo;
                    z = this.loInclusive;
                } else {
                    int iCpr = ConcurrentSkipListMap.cpr(comparator, k, this.lo);
                    if (iCpr < 0 || (iCpr == 0 && !this.loInclusive && z)) {
                        throw new IllegalArgumentException("key out of range");
                    }
                }
            }
            K k3 = k;
            boolean z3 = z;
            if (this.hi != null) {
                if (k2 == null) {
                    k2 = this.hi;
                    z2 = this.hiInclusive;
                } else {
                    int iCpr2 = ConcurrentSkipListMap.cpr(comparator, k2, this.hi);
                    if (iCpr2 > 0 || (iCpr2 == 0 && !this.hiInclusive && z2)) {
                        throw new IllegalArgumentException("key out of range");
                    }
                }
            }
            return new SubMap<>(this.m, k3, z3, k2, z2, this.isDescending);
        }

        @Override
        public SubMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
            if (k == null || k2 == null) {
                throw new NullPointerException();
            }
            return newSubMap(k, z, k2, z2);
        }

        @Override
        public SubMap<K, V> headMap(K k, boolean z) {
            if (k == null) {
                throw new NullPointerException();
            }
            return newSubMap(null, false, k, z);
        }

        @Override
        public SubMap<K, V> tailMap(K k, boolean z) {
            if (k == null) {
                throw new NullPointerException();
            }
            return newSubMap(k, z, null, false);
        }

        @Override
        public SubMap<K, V> subMap(K k, K k2) {
            return subMap((Object) k, true, (Object) k2, false);
        }

        @Override
        public SubMap<K, V> headMap(K k) {
            return headMap((Object) k, false);
        }

        @Override
        public SubMap<K, V> tailMap(K k) {
            return tailMap((Object) k, true);
        }

        @Override
        public SubMap<K, V> descendingMap() {
            return new SubMap<>(this.m, this.lo, this.loInclusive, this.hi, this.hiInclusive, !this.isDescending);
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K k) {
            return getNearEntry(k, 1);
        }

        @Override
        public K ceilingKey(K k) {
            return getNearKey(k, 1);
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K k) {
            return getNearEntry(k, 2);
        }

        @Override
        public K lowerKey(K k) {
            return getNearKey(k, 2);
        }

        @Override
        public Map.Entry<K, V> floorEntry(K k) {
            return getNearEntry(k, 3);
        }

        @Override
        public K floorKey(K k) {
            return getNearKey(k, 3);
        }

        @Override
        public Map.Entry<K, V> higherEntry(K k) {
            return getNearEntry(k, 0);
        }

        @Override
        public K higherKey(K k) {
            return getNearKey(k, 0);
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
            KeySet<K, V> keySet = this.keySetView;
            if (keySet != null) {
                return keySet;
            }
            KeySet<K, V> keySet2 = new KeySet<>(this);
            this.keySetView = keySet2;
            return keySet2;
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            KeySet<K, V> keySet = this.keySetView;
            if (keySet != null) {
                return keySet;
            }
            KeySet<K, V> keySet2 = new KeySet<>(this);
            this.keySetView = keySet2;
            return keySet2;
        }

        @Override
        public Collection<V> values() {
            Collection<V> collection = this.valuesView;
            if (collection != null) {
                return collection;
            }
            Values values = new Values(this);
            this.valuesView = values;
            return values;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K, V>> set = this.entrySetView;
            if (set != null) {
                return set;
            }
            EntrySet entrySet = new EntrySet(this);
            this.entrySetView = entrySet;
            return entrySet;
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
                    if (this.next != null) {
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
                Comparator<? super K> comparator = SubMap.this.m.comparator;
                while (true) {
                    this.next = this.next.next;
                    if (this.next != null) {
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
                    } else {
                        return;
                    }
                }
            }

            private void descend() {
                Comparator<? super K> comparator = SubMap.this.m.comparator;
                while (true) {
                    this.next = SubMap.this.m.findNear(this.lastReturned.key, 2, comparator);
                    if (this.next != null) {
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
                    } else {
                        return;
                    }
                }
            }

            @Override
            public void remove() {
                Node<K, V> node = this.lastReturned;
                if (node == null) {
                    throw new IllegalStateException();
                }
                SubMap.this.m.remove(node.key);
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
                Node<K, V> node = this.next;
                advance();
                return node.key;
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
                Node<K, V> node = this.next;
                V v = this.nextValue;
                advance();
                return new AbstractMap.SimpleImmutableEntry(node.key, v);
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

    boolean removeEntryIf(Predicate<? super Map.Entry<K, V>> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        boolean z = false;
        for (Node<K, V> nodeFindFirst = findFirst(); nodeFindFirst != null; nodeFindFirst = nodeFindFirst.next) {
            V validValue = nodeFindFirst.getValidValue();
            if (validValue != null) {
                K k = nodeFindFirst.key;
                if (predicate.test(new AbstractMap.SimpleImmutableEntry(k, validValue)) && remove(k, validValue)) {
                    z = true;
                }
            }
        }
        return z;
    }

    boolean removeValueIf(Predicate<? super V> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        boolean z = false;
        for (Node<K, V> nodeFindFirst = findFirst(); nodeFindFirst != null; nodeFindFirst = nodeFindFirst.next) {
            V validValue = nodeFindFirst.getValidValue();
            if (validValue != null) {
                K k = nodeFindFirst.key;
                if (predicate.test(validValue) && remove(k, validValue)) {
                    z = true;
                }
            }
        }
        return z;
    }

    static abstract class CSLMSpliterator<K, V> {
        final Comparator<? super K> comparator;
        Node<K, V> current;
        int est;
        final K fence;
        Index<K, V> row;

        CSLMSpliterator(Comparator<? super K> comparator, Index<K, V> index, Node<K, V> node, K k, int i) {
            this.comparator = comparator;
            this.row = index;
            this.current = node;
            this.fence = k;
            this.est = i;
        }

        public final long estimateSize() {
            return this.est;
        }
    }

    static final class KeySpliterator<K, V> extends CSLMSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(Comparator<? super K> comparator, Index<K, V> index, Node<K, V> node, K k, int i) {
            super(comparator, index, node, k, i);
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            K k;
            Node<K, V> node;
            Node<K, V> node2;
            K k2;
            Comparator<? super K> comparator = this.comparator;
            K k3 = this.fence;
            Node<K, V> node3 = this.current;
            if (node3 != null && (k = node3.key) != null) {
                Index<K, V> index = this.row;
                while (index != null) {
                    Index<K, V> index2 = index.right;
                    if (index2 == null || (node = index2.node) == null || (node2 = node.next) == null || node2.value == null || (k2 = node2.key) == null || ConcurrentSkipListMap.cpr(comparator, k2, k) <= 0 || (k3 != null && ConcurrentSkipListMap.cpr(comparator, k2, k3) >= 0)) {
                        index = index.down;
                        this.row = index;
                    } else {
                        this.current = node2;
                        Index<K, V> index3 = index.down;
                        if (index2.right == null) {
                            index2 = index2.down;
                        }
                        this.row = index2;
                        this.est -= this.est >>> 2;
                        return new KeySpliterator<>(comparator, index3, node3, k2, this.est);
                    }
                }
                return null;
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
                if (k2 == null || k == null || ConcurrentSkipListMap.cpr(comparator, k, k2) > 0) {
                    Object obj = node.value;
                    if (obj != null && obj != node) {
                        consumer.accept(k2);
                    }
                } else {
                    return;
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
        HeadIndex<K, V> headIndex;
        Node<K, V> node;
        Comparator<? super K> comparator = this.comparator;
        while (true) {
            headIndex = this.head;
            Node<K, V> node2 = headIndex.node;
            node = node2.next;
            if (node == null || node.value != null) {
                break;
            }
            node.helpDelete(node2, node.next);
        }
        return new KeySpliterator<>(comparator, headIndex, node, null, node == null ? 0 : Integer.MAX_VALUE);
    }

    static final class ValueSpliterator<K, V> extends CSLMSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(Comparator<? super K> comparator, Index<K, V> index, Node<K, V> node, K k, int i) {
            super(comparator, index, node, k, i);
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            K k;
            Node<K, V> node;
            Node<K, V> node2;
            K k2;
            Comparator<? super K> comparator = this.comparator;
            K k3 = this.fence;
            Node<K, V> node3 = this.current;
            if (node3 != null && (k = node3.key) != null) {
                Index<K, V> index = this.row;
                while (index != null) {
                    Index<K, V> index2 = index.right;
                    if (index2 == null || (node = index2.node) == null || (node2 = node.next) == null || node2.value == null || (k2 = node2.key) == null || ConcurrentSkipListMap.cpr(comparator, k2, k) <= 0 || (k3 != null && ConcurrentSkipListMap.cpr(comparator, k2, k3) >= 0)) {
                        index = index.down;
                        this.row = index;
                    } else {
                        this.current = node2;
                        Index<K, V> index3 = index.down;
                        if (index2.right == null) {
                            index2 = index2.down;
                        }
                        this.row = index2;
                        this.est -= this.est >>> 2;
                        return new ValueSpliterator<>(comparator, index3, node3, k2, this.est);
                    }
                }
                return null;
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> comparator = this.comparator;
            K k = this.fence;
            this.current = null;
            for (Node<K, V> node = this.current; node != null; node = node.next) {
                K k2 = node.key;
                if (k2 == null || k == null || ConcurrentSkipListMap.cpr(comparator, k, k2) > 0) {
                    Node<K, V> node2 = (Object) node.value;
                    if (node2 != null && node2 != node) {
                        consumer.accept(node2);
                    }
                } else {
                    return;
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> consumer) {
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
                    Node<K, V> node2 = (Object) node.value;
                    if (node2 == null || node2 == node) {
                        node = node.next;
                    } else {
                        this.current = node.next;
                        consumer.accept(node2);
                        return true;
                    }
                } else {
                    break;
                }
            }
        }

        @Override
        public int characteristics() {
            return 4368;
        }
    }

    final ValueSpliterator<K, V> valueSpliterator() {
        HeadIndex<K, V> headIndex;
        Node<K, V> node;
        Comparator<? super K> comparator = this.comparator;
        while (true) {
            headIndex = this.head;
            Node<K, V> node2 = headIndex.node;
            node = node2.next;
            if (node == null || node.value != null) {
                break;
            }
            node.helpDelete(node2, node.next);
        }
        return new ValueSpliterator<>(comparator, headIndex, node, null, node == null ? 0 : Integer.MAX_VALUE);
    }

    static final class EntrySpliterator<K, V> extends CSLMSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(Comparator<? super K> comparator, Index<K, V> index, Node<K, V> node, K k, int i) {
            super(comparator, index, node, k, i);
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            K k;
            Node<K, V> node;
            Node<K, V> node2;
            K k2;
            Comparator<? super K> comparator = this.comparator;
            K k3 = this.fence;
            Node<K, V> node3 = this.current;
            if (node3 != null && (k = node3.key) != null) {
                Index<K, V> index = this.row;
                while (index != null) {
                    Index<K, V> index2 = index.right;
                    if (index2 == null || (node = index2.node) == null || (node2 = node.next) == null || node2.value == null || (k2 = node2.key) == null || ConcurrentSkipListMap.cpr(comparator, k2, k) <= 0 || (k3 != null && ConcurrentSkipListMap.cpr(comparator, k2, k3) >= 0)) {
                        index = index.down;
                        this.row = index;
                    } else {
                        this.current = node2;
                        Index<K, V> index3 = index.down;
                        if (index2.right == null) {
                            index2 = index2.down;
                        }
                        this.row = index2;
                        this.est -= this.est >>> 2;
                        return new EntrySpliterator<>(comparator, index3, node3, k2, this.est);
                    }
                }
                return null;
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> comparator = this.comparator;
            K k = this.fence;
            this.current = null;
            for (Node<K, V> node = this.current; node != null; node = node.next) {
                K k2 = node.key;
                if (k2 == null || k == null || ConcurrentSkipListMap.cpr(comparator, k, k2) > 0) {
                    Object obj = node.value;
                    if (obj != null && obj != node) {
                        consumer.accept(new AbstractMap.SimpleImmutableEntry(k2, obj));
                    }
                } else {
                    return;
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> consumer) {
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
                        consumer.accept(new AbstractMap.SimpleImmutableEntry(k2, obj));
                        return true;
                    }
                } else {
                    break;
                }
            }
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
            return $$Lambda$ConcurrentSkipListMap$EntrySpliterator$y0KdhWWpZC4eKUM6bCtPBgl2u2o.INSTANCE;
        }
    }

    final EntrySpliterator<K, V> entrySpliterator() {
        HeadIndex<K, V> headIndex;
        Node<K, V> node;
        Comparator<? super K> comparator = this.comparator;
        while (true) {
            headIndex = this.head;
            Node<K, V> node2 = headIndex.node;
            node = node2.next;
            if (node == null || node.value != null) {
                break;
            }
            node.helpDelete(node2, node.next);
        }
        return new EntrySpliterator<>(comparator, headIndex, node, null, node == null ? 0 : Integer.MAX_VALUE);
    }
}
