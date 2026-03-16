package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;

public class TreeMap<K, V> extends AbstractMap<K, V> implements SortedMap<K, V>, NavigableMap<K, V>, Cloneable, Serializable {
    private static final Comparator<Comparable> NATURAL_ORDER = new Comparator<Comparable>() {
        @Override
        public int compare(Comparable a, Comparable b) {
            return a.compareTo(b);
        }
    };
    private static final long serialVersionUID = 919286545866124006L;
    Comparator<? super K> comparator;
    private TreeMap<K, V>.EntrySet entrySet;
    private TreeMap<K, V>.KeySet keySet;
    int modCount;
    Node<K, V> root;
    int size;

    enum Bound {
        INCLUSIVE {
            @Override
            public String leftCap(Object from) {
                return "[" + from;
            }

            @Override
            public String rightCap(Object to) {
                return to + "]";
            }
        },
        EXCLUSIVE {
            @Override
            public String leftCap(Object from) {
                return "(" + from;
            }

            @Override
            public String rightCap(Object to) {
                return to + ")";
            }
        },
        NO_BOUND {
            @Override
            public String leftCap(Object from) {
                return ".";
            }

            @Override
            public String rightCap(Object to) {
                return ".";
            }
        };

        public abstract String leftCap(Object obj);

        public abstract String rightCap(Object obj);
    }

    public TreeMap() {
        this.size = 0;
        this.modCount = 0;
        this.comparator = NATURAL_ORDER;
    }

    public TreeMap(Map<? extends K, ? extends V> copyFrom) {
        this();
        for (Map.Entry<? extends K, ? extends V> entry : copyFrom.entrySet()) {
            putInternal(entry.getKey(), entry.getValue());
        }
    }

    public TreeMap(Comparator<? super K> comparator) {
        this.size = 0;
        this.modCount = 0;
        if (comparator != null) {
            this.comparator = comparator;
        } else {
            this.comparator = NATURAL_ORDER;
        }
    }

    public TreeMap(SortedMap<K, ? extends V> copyFrom) {
        this.size = 0;
        this.modCount = 0;
        Comparator<? super K> sourceComparator = copyFrom.comparator();
        if (sourceComparator != null) {
            this.comparator = sourceComparator;
        } else {
            this.comparator = NATURAL_ORDER;
        }
        for (Map.Entry<K, ? extends V> entry : copyFrom.entrySet()) {
            putInternal(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object clone() {
        try {
            TreeMap<K, V> map = (TreeMap) super.clone();
            map.root = this.root != null ? this.root.copy(null) : null;
            map.entrySet = null;
            map.keySet = null;
            return map;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public V get(Object key) {
        Map.Entry<K, V> entry = findByObject(key);
        if (entry != null) {
            return entry.getValue();
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return findByObject(key) != null;
    }

    @Override
    public V put(K key, V value) {
        return putInternal(key, value);
    }

    @Override
    public void clear() {
        this.root = null;
        this.size = 0;
        this.modCount++;
    }

    @Override
    public V remove(Object key) {
        Node<K, V> node = removeInternalByKey(key);
        if (node != null) {
            return node.value;
        }
        return null;
    }

    enum Relation {
        LOWER,
        FLOOR,
        EQUAL,
        CREATE,
        CEILING,
        HIGHER;

        Relation forOrder(boolean ascending) {
            if (!ascending) {
                switch (this) {
                    case LOWER:
                        return HIGHER;
                    case FLOOR:
                        return CEILING;
                    case EQUAL:
                        return EQUAL;
                    case CEILING:
                        return FLOOR;
                    case HIGHER:
                        return LOWER;
                    default:
                        throw new IllegalStateException();
                }
            }
            return this;
        }
    }

    V putInternal(K key, V value) {
        Node<K, V> created = find(key, Relation.CREATE);
        V result = created.value;
        created.value = value;
        return result;
    }

    java.util.TreeMap.Node<K, V> find(K r10, java.util.TreeMap.Relation r11) {
        throw new UnsupportedOperationException("Method not decompiled: java.util.TreeMap.find(java.lang.Object, java.util.TreeMap$Relation):java.util.TreeMap$Node");
    }

    Node<K, V> findByObject(Object obj) {
        return find(obj, Relation.EQUAL);
    }

    Node<K, V> findByEntry(Map.Entry<?, ?> entry) {
        Node<K, V> mine = findByObject(entry.getKey());
        boolean valuesEqual = mine != null && libcore.util.Objects.equal(mine.value, entry.getValue());
        if (valuesEqual) {
            return mine;
        }
        return null;
    }

    void removeInternal(Node<K, V> node) {
        Node<K, V> left = node.left;
        Node<K, V> right = node.right;
        Node<K, V> originalParent = node.parent;
        if (left != null && right != null) {
            Node<K, V> adjacent = left.height > right.height ? left.last() : right.first();
            removeInternal(adjacent);
            int leftHeight = 0;
            Node<K, V> left2 = node.left;
            if (left2 != null) {
                leftHeight = left2.height;
                adjacent.left = left2;
                left2.parent = adjacent;
                node.left = null;
            }
            int rightHeight = 0;
            Node<K, V> right2 = node.right;
            if (right2 != null) {
                rightHeight = right2.height;
                adjacent.right = right2;
                right2.parent = adjacent;
                node.right = null;
            }
            adjacent.height = Math.max(leftHeight, rightHeight) + 1;
            replaceInParent(node, adjacent);
            return;
        }
        if (left != null) {
            replaceInParent(node, left);
            node.left = null;
        } else if (right != null) {
            replaceInParent(node, right);
            node.right = null;
        } else {
            replaceInParent(node, null);
        }
        rebalance(originalParent, false);
        this.size--;
        this.modCount++;
    }

    Node<K, V> removeInternalByKey(Object key) {
        Node<K, V> node = findByObject(key);
        if (node != null) {
            removeInternal(node);
        }
        return node;
    }

    private void replaceInParent(Node<K, V> node, Node<K, V> replacement) {
        Node<K, V> parent = node.parent;
        node.parent = null;
        if (replacement != null) {
            replacement.parent = parent;
        }
        if (parent != null) {
            if (parent.left == node) {
                parent.left = replacement;
                return;
            } else {
                parent.right = replacement;
                return;
            }
        }
        this.root = replacement;
    }

    private void rebalance(Node<K, V> unbalanced, boolean insert) {
        for (Node<K, V> node = unbalanced; node != null; node = node.parent) {
            Node<K, V> left = node.left;
            Node<K, V> right = node.right;
            int leftHeight = left != null ? left.height : 0;
            int rightHeight = right != null ? right.height : 0;
            int delta = leftHeight - rightHeight;
            if (delta == -2) {
                Node<K, V> rightLeft = right.left;
                Node<K, V> rightRight = right.right;
                int rightRightHeight = rightRight != null ? rightRight.height : 0;
                int rightLeftHeight = rightLeft != null ? rightLeft.height : 0;
                int rightDelta = rightLeftHeight - rightRightHeight;
                if (rightDelta == -1 || (rightDelta == 0 && !insert)) {
                    rotateLeft(node);
                } else {
                    rotateRight(right);
                    rotateLeft(node);
                }
                if (insert) {
                    return;
                }
            } else if (delta == 2) {
                Node<K, V> leftLeft = left.left;
                Node<K, V> leftRight = left.right;
                int leftRightHeight = leftRight != null ? leftRight.height : 0;
                int leftLeftHeight = leftLeft != null ? leftLeft.height : 0;
                int leftDelta = leftLeftHeight - leftRightHeight;
                if (leftDelta == 1 || (leftDelta == 0 && !insert)) {
                    rotateRight(node);
                } else {
                    rotateLeft(left);
                    rotateRight(node);
                }
                if (insert) {
                    return;
                }
            } else if (delta == 0) {
                node.height = leftHeight + 1;
                if (insert) {
                    return;
                }
            } else {
                node.height = Math.max(leftHeight, rightHeight) + 1;
                if (!insert) {
                    return;
                }
            }
        }
    }

    private void rotateLeft(Node<K, V> root) {
        Node<K, V> left = root.left;
        Node<K, V> pivot = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;
        root.right = pivotLeft;
        if (pivotLeft != null) {
            pivotLeft.parent = root;
        }
        replaceInParent(root, pivot);
        pivot.left = root;
        root.parent = pivot;
        root.height = Math.max(left != null ? left.height : 0, pivotLeft != null ? pivotLeft.height : 0) + 1;
        pivot.height = Math.max(root.height, pivotRight != null ? pivotRight.height : 0) + 1;
    }

    private void rotateRight(Node<K, V> root) {
        Node<K, V> pivot = root.left;
        Node<K, V> right = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;
        root.left = pivotRight;
        if (pivotRight != null) {
            pivotRight.parent = root;
        }
        replaceInParent(root, pivot);
        pivot.right = root;
        root.parent = pivot;
        root.height = Math.max(right != null ? right.height : 0, pivotRight != null ? pivotRight.height : 0) + 1;
        pivot.height = Math.max(root.height, pivotLeft != null ? pivotLeft.height : 0) + 1;
    }

    private AbstractMap.SimpleImmutableEntry<K, V> immutableCopy(Map.Entry<K, V> entry) {
        if (entry == null) {
            return null;
        }
        return new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        return immutableCopy(this.root == null ? null : this.root.first());
    }

    private Map.Entry<K, V> internalPollFirstEntry() {
        if (this.root == null) {
            return null;
        }
        Node<K, V> result = this.root.first();
        removeInternal(result);
        return result;
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return immutableCopy(internalPollFirstEntry());
    }

    @Override
    public K firstKey() {
        if (this.root == null) {
            throw new NoSuchElementException();
        }
        return this.root.first().getKey();
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return immutableCopy(this.root == null ? null : this.root.last());
    }

    private Map.Entry<K, V> internalPollLastEntry() {
        if (this.root == null) {
            return null;
        }
        Node<K, V> result = this.root.last();
        removeInternal(result);
        return result;
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return immutableCopy(internalPollLastEntry());
    }

    @Override
    public K lastKey() {
        if (this.root == null) {
            throw new NoSuchElementException();
        }
        return this.root.last().getKey();
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return immutableCopy(find(key, Relation.LOWER));
    }

    @Override
    public K lowerKey(K key) {
        Map.Entry<K, V> entry = find(key, Relation.LOWER);
        if (entry != null) {
            return entry.getKey();
        }
        return null;
    }

    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return immutableCopy(find(key, Relation.FLOOR));
    }

    @Override
    public K floorKey(K key) {
        Map.Entry<K, V> entry = find(key, Relation.FLOOR);
        if (entry != null) {
            return entry.getKey();
        }
        return null;
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return immutableCopy(find(key, Relation.CEILING));
    }

    @Override
    public K ceilingKey(K key) {
        Map.Entry<K, V> entry = find(key, Relation.CEILING);
        if (entry != null) {
            return entry.getKey();
        }
        return null;
    }

    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return immutableCopy(find(key, Relation.HIGHER));
    }

    @Override
    public K higherKey(K key) {
        Map.Entry<K, V> entry = find(key, Relation.HIGHER);
        if (entry != null) {
            return entry.getKey();
        }
        return null;
    }

    @Override
    public Comparator<? super K> comparator() {
        if (this.comparator != NATURAL_ORDER) {
            return this.comparator;
        }
        return null;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        TreeMap<K, V>.EntrySet result = this.entrySet;
        if (result != null) {
            return result;
        }
        TreeMap<K, V>.EntrySet result2 = new EntrySet();
        this.entrySet = result2;
        return result2;
    }

    @Override
    public Set<K> keySet() {
        TreeMap<K, V>.KeySet result = this.keySet;
        if (result != null) {
            return result;
        }
        TreeMap<K, V>.KeySet result2 = new KeySet();
        this.keySet = result2;
        return result2;
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        TreeMap<K, V>.KeySet result = this.keySet;
        if (result != null) {
            return result;
        }
        TreeMap<K, V>.KeySet result2 = new KeySet();
        this.keySet = result2;
        return result2;
    }

    @Override
    public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
        Bound fromBound = fromInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
        Bound toBound = toInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
        return new BoundedMap(true, from, fromBound, to, toBound);
    }

    @Override
    public SortedMap<K, V> subMap(K fromInclusive, K toExclusive) {
        return new BoundedMap(true, fromInclusive, Bound.INCLUSIVE, toExclusive, Bound.EXCLUSIVE);
    }

    @Override
    public NavigableMap<K, V> headMap(K to, boolean inclusive) {
        Bound toBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
        return new BoundedMap(true, null, Bound.NO_BOUND, to, toBound);
    }

    @Override
    public SortedMap<K, V> headMap(K toExclusive) {
        return new BoundedMap(true, null, Bound.NO_BOUND, toExclusive, Bound.EXCLUSIVE);
    }

    @Override
    public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
        Bound fromBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
        return new BoundedMap(true, from, fromBound, null, Bound.NO_BOUND);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromInclusive) {
        return new BoundedMap(true, fromInclusive, Bound.INCLUSIVE, null, Bound.NO_BOUND);
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return new BoundedMap(false, null, Bound.NO_BOUND, null, Bound.NO_BOUND);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return new BoundedMap(false, null, Bound.NO_BOUND, null, Bound.NO_BOUND).navigableKeySet();
    }

    static class Node<K, V> implements Map.Entry<K, V> {
        int height = 1;
        final K key;
        Node<K, V> left;
        Node<K, V> parent;
        Node<K, V> right;
        V value;

        Node(Node<K, V> parent, K key) {
            this.parent = parent;
            this.key = key;
        }

        Node<K, V> copy(Node<K, V> parent) {
            Node<K, V> result = new Node<>(parent, this.key);
            if (this.left != null) {
                result.left = this.left.copy(result);
            }
            if (this.right != null) {
                result.right = this.right.copy(result);
            }
            result.value = this.value;
            result.height = this.height;
            return result;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry other = (Map.Entry) o;
            if (this.key == null) {
                if (other.getKey() != null) {
                    return false;
                }
            } else if (!this.key.equals(other.getKey())) {
                return false;
            }
            if (this.value == null) {
                if (other.getValue() != null) {
                    return false;
                }
            } else if (!this.value.equals(other.getValue())) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public String toString() {
            return this.key + "=" + this.value;
        }

        Node<K, V> next() {
            if (this.right != null) {
                return this.right.first();
            }
            Node<K, V> node = this;
            Node<K, V> parent = node.parent;
            while (parent != null) {
                if (parent.left != node) {
                    node = parent;
                    parent = node.parent;
                } else {
                    return parent;
                }
            }
            return null;
        }

        public Node<K, V> prev() {
            if (this.left != null) {
                return this.left.last();
            }
            Node<K, V> node = this;
            Node<K, V> parent = node.parent;
            while (parent != null) {
                if (parent.right != node) {
                    node = parent;
                    parent = node.parent;
                } else {
                    return parent;
                }
            }
            return null;
        }

        public Node<K, V> first() {
            Node<K, V> node = this;
            Node<K, V> child = node.left;
            while (child != null) {
                node = child;
                child = node.left;
            }
            return node;
        }

        public Node<K, V> last() {
            Node<K, V> node = this;
            Node<K, V> child = node.right;
            while (child != null) {
                node = child;
                child = node.right;
            }
            return node;
        }
    }

    abstract class MapIterator<T> implements Iterator<T> {
        protected int expectedModCount;
        protected Node<K, V> last;
        protected Node<K, V> next;

        MapIterator(Node<K, V> next) {
            this.expectedModCount = TreeMap.this.modCount;
            this.next = next;
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        protected Node<K, V> stepForward() {
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            if (TreeMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            this.last = this.next;
            this.next = this.next.next();
            return this.last;
        }

        protected Node<K, V> stepBackward() {
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            if (TreeMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            this.last = this.next;
            this.next = this.next.prev();
            return this.last;
        }

        @Override
        public void remove() {
            if (this.last == null) {
                throw new IllegalStateException();
            }
            TreeMap.this.removeInternal(this.last);
            this.expectedModCount = TreeMap.this.modCount;
            this.last = null;
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        EntrySet() {
        }

        @Override
        public int size() {
            return TreeMap.this.size;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new TreeMap<K, V>.MapIterator<Map.Entry<K, V>>(TreeMap.this.root == null ? null : TreeMap.this.root.first()) {
                {
                    TreeMap treeMap = TreeMap.this;
                }

                @Override
                public Map.Entry<K, V> next() {
                    return stepForward();
                }
            };
        }

        @Override
        public boolean contains(Object o) {
            return (o instanceof Map.Entry) && TreeMap.this.findByEntry((Map.Entry) o) != null;
        }

        @Override
        public boolean remove(Object o) {
            Node<K, V> node;
            if (!(o instanceof Map.Entry) || (node = TreeMap.this.findByEntry((Map.Entry) o)) == null) {
                return false;
            }
            TreeMap.this.removeInternal(node);
            return true;
        }

        @Override
        public void clear() {
            TreeMap.this.clear();
        }
    }

    class KeySet extends AbstractSet<K> implements NavigableSet<K> {
        KeySet() {
        }

        @Override
        public int size() {
            return TreeMap.this.size;
        }

        @Override
        public Iterator<K> iterator() {
            return new TreeMap<K, V>.MapIterator<K>(TreeMap.this.root == null ? null : TreeMap.this.root.first()) {
                {
                    TreeMap treeMap = TreeMap.this;
                }

                @Override
                public K next() {
                    return stepForward().key;
                }
            };
        }

        @Override
        public Iterator<K> descendingIterator() {
            return new TreeMap<K, V>.MapIterator<K>(TreeMap.this.root == null ? null : TreeMap.this.root.last()) {
                {
                    TreeMap treeMap = TreeMap.this;
                }

                @Override
                public K next() {
                    return stepBackward().key;
                }
            };
        }

        @Override
        public boolean contains(Object o) {
            return TreeMap.this.containsKey(o);
        }

        @Override
        public boolean remove(Object key) {
            return TreeMap.this.removeInternalByKey(key) != null;
        }

        @Override
        public void clear() {
            TreeMap.this.clear();
        }

        @Override
        public Comparator<? super K> comparator() {
            return TreeMap.this.comparator();
        }

        @Override
        public K first() {
            return (K) TreeMap.this.firstKey();
        }

        @Override
        public K last() {
            return (K) TreeMap.this.lastKey();
        }

        @Override
        public K lower(K k) {
            return (K) TreeMap.this.lowerKey(k);
        }

        @Override
        public K floor(K k) {
            return (K) TreeMap.this.floorKey(k);
        }

        @Override
        public K ceiling(K k) {
            return (K) TreeMap.this.ceilingKey(k);
        }

        @Override
        public K higher(K k) {
            return (K) TreeMap.this.higherKey(k);
        }

        @Override
        public K pollFirst() {
            Map.Entry<K, V> entry = TreeMap.this.internalPollFirstEntry();
            if (entry != null) {
                return entry.getKey();
            }
            return null;
        }

        @Override
        public K pollLast() {
            Map.Entry<K, V> entry = TreeMap.this.internalPollLastEntry();
            if (entry != null) {
                return entry.getKey();
            }
            return null;
        }

        @Override
        public NavigableSet<K> subSet(K from, boolean fromInclusive, K to, boolean toInclusive) {
            return TreeMap.this.subMap(from, fromInclusive, to, toInclusive).navigableKeySet();
        }

        @Override
        public SortedSet<K> subSet(K fromInclusive, K toExclusive) {
            return TreeMap.this.subMap(fromInclusive, true, toExclusive, false).navigableKeySet();
        }

        @Override
        public NavigableSet<K> headSet(K to, boolean inclusive) {
            return TreeMap.this.headMap(to, inclusive).navigableKeySet();
        }

        @Override
        public SortedSet<K> headSet(K toExclusive) {
            return TreeMap.this.headMap(toExclusive, false).navigableKeySet();
        }

        @Override
        public NavigableSet<K> tailSet(K from, boolean inclusive) {
            return TreeMap.this.tailMap(from, inclusive).navigableKeySet();
        }

        @Override
        public SortedSet<K> tailSet(K fromInclusive) {
            return TreeMap.this.tailMap(fromInclusive, true).navigableKeySet();
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return new BoundedMap(false, null, Bound.NO_BOUND, null, Bound.NO_BOUND).navigableKeySet();
        }
    }

    final class BoundedMap extends AbstractMap<K, V> implements NavigableMap<K, V>, Serializable {
        private final transient boolean ascending;
        private transient TreeMap<K, V>.BoundedMap.BoundedEntrySet entrySet;
        private final transient K from;
        private final transient Bound fromBound;
        private transient TreeMap<K, V>.BoundedMap.BoundedKeySet keySet;
        private final transient K to;
        private final transient Bound toBound;

        BoundedMap(boolean ascending, K from, Bound fromBound, K to, Bound toBound) {
            if (fromBound != Bound.NO_BOUND && toBound != Bound.NO_BOUND) {
                if (TreeMap.this.comparator.compare(from, to) > 0) {
                    throw new IllegalArgumentException(from + " > " + to);
                }
            } else if (fromBound != Bound.NO_BOUND) {
                TreeMap.this.comparator.compare(from, from);
            } else if (toBound != Bound.NO_BOUND) {
                TreeMap.this.comparator.compare(to, to);
            }
            this.ascending = ascending;
            this.from = from;
            this.fromBound = fromBound;
            this.to = to;
            this.toBound = toBound;
        }

        @Override
        public int size() {
            return TreeMap.count(entrySet().iterator());
        }

        @Override
        public boolean isEmpty() {
            return endpoint(true) == null;
        }

        @Override
        public V get(Object obj) {
            if (isInBounds(obj)) {
                return (V) TreeMap.this.get(obj);
            }
            return null;
        }

        @Override
        public boolean containsKey(Object key) {
            return isInBounds(key) && TreeMap.this.containsKey(key);
        }

        @Override
        public V put(K k, V v) {
            if (!isInBounds(k)) {
                throw outOfBounds(k, this.fromBound, this.toBound);
            }
            return (V) TreeMap.this.putInternal(k, v);
        }

        @Override
        public V remove(Object obj) {
            if (isInBounds(obj)) {
                return (V) TreeMap.this.remove(obj);
            }
            return null;
        }

        private boolean isInBounds(Object key) {
            return isInBounds(key, this.fromBound, this.toBound);
        }

        private boolean isInBounds(K k, Bound bound, Bound bound2) {
            if (bound == Bound.INCLUSIVE) {
                if (TreeMap.this.comparator.compare(k, this.from) < 0) {
                    return false;
                }
            } else if (bound == Bound.EXCLUSIVE && TreeMap.this.comparator.compare(k, this.from) <= 0) {
                return false;
            }
            if (bound2 == Bound.INCLUSIVE) {
                if (TreeMap.this.comparator.compare(k, this.to) > 0) {
                    return false;
                }
            } else if (bound2 == Bound.EXCLUSIVE && TreeMap.this.comparator.compare(k, this.to) >= 0) {
                return false;
            }
            return true;
        }

        private Node<K, V> bound(Node<K, V> node, Bound fromBound, Bound toBound) {
            if (node == null || !isInBounds(node.getKey(), fromBound, toBound)) {
                return null;
            }
            return node;
        }

        @Override
        public Map.Entry<K, V> firstEntry() {
            return TreeMap.this.immutableCopy(endpoint(true));
        }

        @Override
        public Map.Entry<K, V> pollFirstEntry() {
            Node<K, V> result = endpoint(true);
            if (result != null) {
                TreeMap.this.removeInternal(result);
            }
            return TreeMap.this.immutableCopy(result);
        }

        @Override
        public K firstKey() {
            Map.Entry<K, V> entry = endpoint(true);
            if (entry == null) {
                throw new NoSuchElementException();
            }
            return entry.getKey();
        }

        @Override
        public Map.Entry<K, V> lastEntry() {
            return TreeMap.this.immutableCopy(endpoint(false));
        }

        @Override
        public Map.Entry<K, V> pollLastEntry() {
            Node<K, V> result = endpoint(false);
            if (result != null) {
                TreeMap.this.removeInternal(result);
            }
            return TreeMap.this.immutableCopy(result);
        }

        @Override
        public K lastKey() {
            Map.Entry<K, V> entry = endpoint(false);
            if (entry == null) {
                throw new NoSuchElementException();
            }
            return entry.getKey();
        }

        private Node<K, V> endpoint(boolean first) {
            Node<K, V> node = null;
            if (this.ascending == first) {
                switch (this.fromBound) {
                    case NO_BOUND:
                        if (TreeMap.this.root != null) {
                            node = TreeMap.this.root.first();
                        }
                        break;
                    case INCLUSIVE:
                        node = TreeMap.this.find(this.from, Relation.CEILING);
                        break;
                    case EXCLUSIVE:
                        node = TreeMap.this.find(this.from, Relation.HIGHER);
                        break;
                    default:
                        throw new AssertionError();
                }
                return bound(node, Bound.NO_BOUND, this.toBound);
            }
            switch (this.toBound) {
                case NO_BOUND:
                    if (TreeMap.this.root != null) {
                        node = TreeMap.this.root.last();
                    }
                    break;
                case INCLUSIVE:
                    node = TreeMap.this.find(this.to, Relation.FLOOR);
                    break;
                case EXCLUSIVE:
                    node = TreeMap.this.find(this.to, Relation.LOWER);
                    break;
                default:
                    throw new AssertionError();
            }
            return bound(node, this.fromBound, Bound.NO_BOUND);
        }

        private Map.Entry<K, V> findBounded(K k, Relation relation) {
            Relation relationForOrder = relation.forOrder(this.ascending);
            Bound bound = this.fromBound;
            Bound bound2 = this.toBound;
            if (this.toBound != Bound.NO_BOUND && (relationForOrder == Relation.LOWER || relationForOrder == Relation.FLOOR)) {
                int iCompare = TreeMap.this.comparator.compare(this.to, k);
                if (iCompare <= 0) {
                    k = this.to;
                    if (this.toBound == Bound.EXCLUSIVE) {
                        relationForOrder = Relation.LOWER;
                    } else if (iCompare < 0) {
                        relationForOrder = Relation.FLOOR;
                    }
                }
                bound2 = Bound.NO_BOUND;
            }
            if (this.fromBound != Bound.NO_BOUND && (relationForOrder == Relation.CEILING || relationForOrder == Relation.HIGHER)) {
                int iCompare2 = TreeMap.this.comparator.compare(this.from, k);
                if (iCompare2 >= 0) {
                    k = this.from;
                    if (this.fromBound == Bound.EXCLUSIVE) {
                        relationForOrder = Relation.HIGHER;
                    } else if (iCompare2 > 0) {
                        relationForOrder = Relation.CEILING;
                    }
                }
                bound = Bound.NO_BOUND;
            }
            return bound(TreeMap.this.find(k, relationForOrder), bound, bound2);
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K key) {
            return TreeMap.this.immutableCopy(findBounded(key, Relation.LOWER));
        }

        @Override
        public K lowerKey(K key) {
            Map.Entry<K, V> entry = findBounded(key, Relation.LOWER);
            if (entry != null) {
                return entry.getKey();
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> floorEntry(K key) {
            return TreeMap.this.immutableCopy(findBounded(key, Relation.FLOOR));
        }

        @Override
        public K floorKey(K key) {
            Map.Entry<K, V> entry = findBounded(key, Relation.FLOOR);
            if (entry != null) {
                return entry.getKey();
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K key) {
            return TreeMap.this.immutableCopy(findBounded(key, Relation.CEILING));
        }

        @Override
        public K ceilingKey(K key) {
            Map.Entry<K, V> entry = findBounded(key, Relation.CEILING);
            if (entry != null) {
                return entry.getKey();
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> higherEntry(K key) {
            return TreeMap.this.immutableCopy(findBounded(key, Relation.HIGHER));
        }

        @Override
        public K higherKey(K key) {
            Map.Entry<K, V> entry = findBounded(key, Relation.HIGHER);
            if (entry != null) {
                return entry.getKey();
            }
            return null;
        }

        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> forward = TreeMap.this.comparator();
            return this.ascending ? forward : Collections.reverseOrder(forward);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            TreeMap<K, V>.BoundedMap.BoundedEntrySet result = this.entrySet;
            if (result != null) {
                return result;
            }
            TreeMap<K, V>.BoundedMap.BoundedEntrySet result2 = new BoundedEntrySet();
            this.entrySet = result2;
            return result2;
        }

        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            TreeMap<K, V>.BoundedMap.BoundedKeySet result = this.keySet;
            if (result != null) {
                return result;
            }
            TreeMap<K, V>.BoundedMap.BoundedKeySet result2 = new BoundedKeySet();
            this.keySet = result2;
            return result2;
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return new BoundedMap(!this.ascending, this.from, this.fromBound, this.to, this.toBound);
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return new BoundedMap(!this.ascending, this.from, this.fromBound, this.to, this.toBound).navigableKeySet();
        }

        @Override
        public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
            Bound fromBound = fromInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
            Bound toBound = toInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
            return subMap(from, fromBound, to, toBound);
        }

        @Override
        public NavigableMap<K, V> subMap(K fromInclusive, K toExclusive) {
            return subMap(fromInclusive, Bound.INCLUSIVE, toExclusive, Bound.EXCLUSIVE);
        }

        @Override
        public NavigableMap<K, V> headMap(K to, boolean inclusive) {
            Bound toBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
            return subMap((Object) null, Bound.NO_BOUND, to, toBound);
        }

        @Override
        public NavigableMap<K, V> headMap(K toExclusive) {
            return subMap((Object) null, Bound.NO_BOUND, toExclusive, Bound.EXCLUSIVE);
        }

        @Override
        public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
            Bound fromBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
            return subMap(from, fromBound, (Object) null, Bound.NO_BOUND);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromInclusive) {
            return subMap(fromInclusive, Bound.INCLUSIVE, (Object) null, Bound.NO_BOUND);
        }

        private NavigableMap<K, V> subMap(K from, Bound fromBound, K to, Bound toBound) {
            if (!this.ascending) {
                from = to;
                fromBound = toBound;
                to = from;
                toBound = fromBound;
            }
            if (fromBound == Bound.NO_BOUND) {
                from = this.from;
                fromBound = this.fromBound;
            } else {
                Bound fromBoundToCheck = fromBound == this.fromBound ? Bound.INCLUSIVE : this.fromBound;
                if (!isInBounds(from, fromBoundToCheck, this.toBound)) {
                    throw outOfBounds(to, fromBoundToCheck, this.toBound);
                }
            }
            if (toBound == Bound.NO_BOUND) {
                to = this.to;
                toBound = this.toBound;
            } else {
                Bound toBoundToCheck = toBound == this.toBound ? Bound.INCLUSIVE : this.toBound;
                if (!isInBounds(to, this.fromBound, toBoundToCheck)) {
                    throw outOfBounds(to, this.fromBound, toBoundToCheck);
                }
            }
            return new BoundedMap(this.ascending, from, fromBound, to, toBound);
        }

        private IllegalArgumentException outOfBounds(Object value, Bound fromBound, Bound toBound) {
            return new IllegalArgumentException(value + " not in range " + fromBound.leftCap(this.from) + ".." + toBound.rightCap(this.to));
        }

        abstract class BoundedIterator<T> extends TreeMap<K, V>.MapIterator<T> {
            protected BoundedIterator(Node<K, V> next) {
                super(next);
            }

            @Override
            protected Node<K, V> stepForward() {
                Node<K, V> result = super.stepForward();
                if (this.next != null && !BoundedMap.this.isInBounds(this.next.key, Bound.NO_BOUND, BoundedMap.this.toBound)) {
                    this.next = null;
                }
                return result;
            }

            @Override
            protected Node<K, V> stepBackward() {
                Node<K, V> result = super.stepBackward();
                if (this.next != null && !BoundedMap.this.isInBounds(this.next.key, BoundedMap.this.fromBound, Bound.NO_BOUND)) {
                    this.next = null;
                }
                return result;
            }
        }

        final class BoundedEntrySet extends AbstractSet<Map.Entry<K, V>> {
            BoundedEntrySet() {
            }

            @Override
            public int size() {
                return BoundedMap.this.size();
            }

            @Override
            public boolean isEmpty() {
                return BoundedMap.this.isEmpty();
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new TreeMap<K, V>.BoundedMap.BoundedIterator<Map.Entry<K, V>>(BoundedMap.this.endpoint(true)) {
                    {
                        BoundedMap boundedMap = BoundedMap.this;
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        return BoundedMap.this.ascending ? stepForward() : stepBackward();
                    }
                };
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry<?, ?> entry = (Map.Entry) o;
                return BoundedMap.this.isInBounds(entry.getKey()) && TreeMap.this.findByEntry(entry) != null;
            }

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry<?, ?> entry = (Map.Entry) o;
                return BoundedMap.this.isInBounds(entry.getKey()) && TreeMap.this.entrySet().remove(entry);
            }
        }

        final class BoundedKeySet extends AbstractSet<K> implements NavigableSet<K> {
            BoundedKeySet() {
            }

            @Override
            public int size() {
                return BoundedMap.this.size();
            }

            @Override
            public boolean isEmpty() {
                return BoundedMap.this.isEmpty();
            }

            @Override
            public Iterator<K> iterator() {
                return new TreeMap<K, V>.BoundedMap.BoundedIterator<K>(BoundedMap.this.endpoint(true)) {
                    {
                        BoundedMap boundedMap = BoundedMap.this;
                    }

                    @Override
                    public K next() {
                        return (BoundedMap.this.ascending ? stepForward() : stepBackward()).key;
                    }
                };
            }

            @Override
            public Iterator<K> descendingIterator() {
                return new TreeMap<K, V>.BoundedMap.BoundedIterator<K>(BoundedMap.this.endpoint(false)) {
                    {
                        BoundedMap boundedMap = BoundedMap.this;
                    }

                    @Override
                    public K next() {
                        return (BoundedMap.this.ascending ? stepBackward() : stepForward()).key;
                    }
                };
            }

            @Override
            public boolean contains(Object key) {
                return BoundedMap.this.isInBounds(key) && TreeMap.this.findByObject(key) != null;
            }

            @Override
            public boolean remove(Object key) {
                return BoundedMap.this.isInBounds(key) && TreeMap.this.removeInternalByKey(key) != null;
            }

            @Override
            public K first() {
                return (K) BoundedMap.this.firstKey();
            }

            @Override
            public K pollFirst() {
                Map.Entry<K, V> entryPollFirstEntry = BoundedMap.this.pollFirstEntry();
                if (entryPollFirstEntry != null) {
                    return entryPollFirstEntry.getKey();
                }
                return null;
            }

            @Override
            public K last() {
                return (K) BoundedMap.this.lastKey();
            }

            @Override
            public K pollLast() {
                Map.Entry<K, V> entryPollLastEntry = BoundedMap.this.pollLastEntry();
                if (entryPollLastEntry != null) {
                    return entryPollLastEntry.getKey();
                }
                return null;
            }

            @Override
            public K lower(K k) {
                return (K) BoundedMap.this.lowerKey(k);
            }

            @Override
            public K floor(K k) {
                return (K) BoundedMap.this.floorKey(k);
            }

            @Override
            public K ceiling(K k) {
                return (K) BoundedMap.this.ceilingKey(k);
            }

            @Override
            public K higher(K k) {
                return (K) BoundedMap.this.higherKey(k);
            }

            @Override
            public Comparator<? super K> comparator() {
                return BoundedMap.this.comparator();
            }

            @Override
            public NavigableSet<K> subSet(K from, boolean fromInclusive, K to, boolean toInclusive) {
                return BoundedMap.this.subMap(from, fromInclusive, to, toInclusive).navigableKeySet();
            }

            @Override
            public SortedSet<K> subSet(K fromInclusive, K toExclusive) {
                return BoundedMap.this.subMap((Object) fromInclusive, (Object) toExclusive).navigableKeySet();
            }

            @Override
            public NavigableSet<K> headSet(K to, boolean inclusive) {
                return BoundedMap.this.headMap(to, inclusive).navigableKeySet();
            }

            @Override
            public SortedSet<K> headSet(K toExclusive) {
                return BoundedMap.this.headMap((Object) toExclusive).navigableKeySet();
            }

            @Override
            public NavigableSet<K> tailSet(K from, boolean inclusive) {
                return BoundedMap.this.tailMap(from, inclusive).navigableKeySet();
            }

            @Override
            public SortedSet<K> tailSet(K fromInclusive) {
                return BoundedMap.this.tailMap((Object) fromInclusive).navigableKeySet();
            }

            @Override
            public NavigableSet<K> descendingSet() {
                return new BoundedMap(!BoundedMap.this.ascending, BoundedMap.this.from, BoundedMap.this.fromBound, BoundedMap.this.to, BoundedMap.this.toBound).navigableKeySet();
            }
        }

        Object writeReplace() throws ObjectStreamException {
            return this.ascending ? new AscendingSubMap(TreeMap.this, this.from, this.fromBound, this.to, this.toBound) : new DescendingSubMap(TreeMap.this, this.from, this.fromBound, this.to, this.toBound);
        }
    }

    static int count(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.putFields().put("comparator", comparator());
        stream.writeFields();
        stream.writeInt(this.size);
        for (Map.Entry<K, V> entry : entrySet()) {
            stream.writeObject(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        this.comparator = (Comparator) fields.get("comparator", (Object) null);
        if (this.comparator == null) {
            this.comparator = NATURAL_ORDER;
        }
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
            putInternal(stream.readObject(), stream.readObject());
        }
    }

    static abstract class NavigableSubMap<K, V> extends AbstractMap<K, V> implements Serializable {
        private static final long serialVersionUID = -2102997345730753016L;
        boolean fromStart;
        Object hi;
        boolean hiInclusive;
        Object lo;
        boolean loInclusive;
        TreeMap<K, V> m;
        boolean toEnd;

        NavigableSubMap(TreeMap<K, V> delegate, K from, Bound fromBound, K to, Bound toBound) {
            this.m = delegate;
            this.lo = from;
            this.hi = to;
            this.fromStart = fromBound == Bound.NO_BOUND;
            this.toEnd = toBound == Bound.NO_BOUND;
            this.loInclusive = fromBound == Bound.INCLUSIVE;
            this.hiInclusive = toBound == Bound.INCLUSIVE;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException();
        }

        protected Object readResolve() throws ObjectStreamException {
            Bound fromBound;
            Bound toBound;
            if (this.fromStart) {
                fromBound = Bound.NO_BOUND;
            } else {
                fromBound = this.loInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
            }
            if (this.toEnd) {
                toBound = Bound.NO_BOUND;
            } else {
                toBound = this.hiInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
            }
            boolean ascending = !(this instanceof DescendingSubMap);
            TreeMap<K, V> treeMap = this.m;
            treeMap.getClass();
            return treeMap.new BoundedMap(ascending, this.lo, fromBound, this.hi, toBound);
        }
    }

    static class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {
        private static final long serialVersionUID = 912986545866120460L;
        Comparator<K> reverseComparator;

        DescendingSubMap(TreeMap<K, V> delegate, K from, Bound fromBound, K to, Bound toBound) {
            super(delegate, from, fromBound, to, toBound);
        }
    }

    static class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {
        private static final long serialVersionUID = 912986545866124060L;

        AscendingSubMap(TreeMap<K, V> delegate, K from, Bound fromBound, K to, Bound toBound) {
            super(delegate, from, fromBound, to, toBound);
        }
    }

    class SubMap extends AbstractMap<K, V> implements Serializable {
        private static final long serialVersionUID = -6520786458950516097L;
        Object fromKey;
        boolean fromStart;
        boolean toEnd;
        Object toKey;

        SubMap() {
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException();
        }

        protected Object readResolve() throws ObjectStreamException {
            Bound fromBound = this.fromStart ? Bound.NO_BOUND : Bound.INCLUSIVE;
            Bound toBound = this.toEnd ? Bound.NO_BOUND : Bound.EXCLUSIVE;
            return new BoundedMap(true, this.fromKey, fromBound, this.toKey, toBound);
        }
    }
}
