package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TreeMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, Serializable {
    private static final boolean BLACK = true;
    private static final boolean RED = false;
    private static final Object UNBOUNDED = new Object();
    private static final long serialVersionUID = 919286545866124006L;
    private final Comparator<? super K> comparator;
    private transient NavigableMap<K, V> descendingMap;
    private transient TreeMap<K, V>.EntrySet entrySet;
    private transient int modCount;
    private transient KeySet<K> navigableKeySet;
    private transient TreeMapEntry<K, V> root;
    private transient int size;

    public TreeMap() {
        this.size = 0;
        this.modCount = 0;
        this.comparator = null;
    }

    public TreeMap(Comparator<? super K> comparator) {
        this.size = 0;
        this.modCount = 0;
        this.comparator = comparator;
    }

    public TreeMap(Map<? extends K, ? extends V> map) {
        this.size = 0;
        this.modCount = 0;
        this.comparator = null;
        putAll(map);
    }

    public TreeMap(SortedMap<K, ? extends V> sortedMap) {
        this.size = 0;
        this.modCount = 0;
        this.comparator = sortedMap.comparator();
        try {
            buildFromSorted(sortedMap.size(), sortedMap.entrySet().iterator(), null, null);
        } catch (IOException e) {
        } catch (ClassNotFoundException e2) {
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean containsKey(Object obj) {
        return getEntry(obj) != null ? BLACK : RED;
    }

    @Override
    public boolean containsValue(Object obj) {
        for (TreeMapEntry<K, V> firstEntry = getFirstEntry(); firstEntry != null; firstEntry = successor(firstEntry)) {
            if (valEquals(obj, firstEntry.value)) {
                return BLACK;
            }
        }
        return RED;
    }

    @Override
    public V get(Object obj) {
        TreeMapEntry<K, V> entry = getEntry(obj);
        if (entry == null) {
            return null;
        }
        return entry.value;
    }

    @Override
    public Comparator<? super K> comparator() {
        return this.comparator;
    }

    @Override
    public K firstKey() {
        return (K) key(getFirstEntry());
    }

    @Override
    public K lastKey() {
        return (K) key(getLastEntry());
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        Comparator<? super K> comparator;
        int size = map.size();
        if (this.size == 0 && size != 0 && (map instanceof SortedMap) && ((comparator = ((SortedMap) map).comparator()) == this.comparator || (comparator != null && comparator.equals(this.comparator)))) {
            this.modCount++;
            try {
                buildFromSorted(size, map.entrySet().iterator(), null, null);
                return;
            } catch (IOException e) {
                return;
            } catch (ClassNotFoundException e2) {
                return;
            }
        }
        super.putAll(map);
    }

    final TreeMapEntry<K, V> getEntry(Object obj) {
        if (this.comparator != null) {
            return getEntryUsingComparator(obj);
        }
        if (obj == null) {
            throw new NullPointerException();
        }
        Comparable comparable = (Comparable) obj;
        TreeMapEntry<K, V> treeMapEntry = this.root;
        while (treeMapEntry != null) {
            int iCompareTo = comparable.compareTo(treeMapEntry.key);
            if (iCompareTo < 0) {
                treeMapEntry = treeMapEntry.left;
            } else if (iCompareTo > 0) {
                treeMapEntry = treeMapEntry.right;
            } else {
                return treeMapEntry;
            }
        }
        return null;
    }

    final TreeMapEntry<K, V> getEntryUsingComparator(Object obj) {
        Comparator<? super K> comparator = this.comparator;
        if (comparator != null) {
            TreeMapEntry<K, V> treeMapEntry = this.root;
            while (treeMapEntry != null) {
                int iCompare = comparator.compare(obj, treeMapEntry.key);
                if (iCompare < 0) {
                    treeMapEntry = treeMapEntry.left;
                } else if (iCompare > 0) {
                    treeMapEntry = treeMapEntry.right;
                } else {
                    return treeMapEntry;
                }
            }
            return null;
        }
        return null;
    }

    final TreeMapEntry<K, V> getCeilingEntry(K k) {
        TreeMapEntry<K, V> treeMapEntry = this.root;
        while (treeMapEntry != null) {
            int iCompare = compare(k, treeMapEntry.key);
            if (iCompare < 0) {
                if (treeMapEntry.left != null) {
                    treeMapEntry = treeMapEntry.left;
                } else {
                    return treeMapEntry;
                }
            } else if (iCompare > 0) {
                if (treeMapEntry.right != null) {
                    treeMapEntry = treeMapEntry.right;
                } else {
                    TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.parent;
                    while (true) {
                        TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry;
                        treeMapEntry = treeMapEntry2;
                        if (treeMapEntry == null || treeMapEntry3 != treeMapEntry.right) {
                            break;
                        }
                        treeMapEntry2 = treeMapEntry.parent;
                    }
                    return treeMapEntry;
                }
            } else {
                return treeMapEntry;
            }
        }
        return null;
    }

    final TreeMapEntry<K, V> getFloorEntry(K k) {
        TreeMapEntry<K, V> treeMapEntry = this.root;
        while (treeMapEntry != null) {
            int iCompare = compare(k, treeMapEntry.key);
            if (iCompare > 0) {
                if (treeMapEntry.right != null) {
                    treeMapEntry = treeMapEntry.right;
                } else {
                    return treeMapEntry;
                }
            } else if (iCompare < 0) {
                if (treeMapEntry.left != null) {
                    treeMapEntry = treeMapEntry.left;
                } else {
                    TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.parent;
                    while (true) {
                        TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry;
                        treeMapEntry = treeMapEntry2;
                        if (treeMapEntry == null || treeMapEntry3 != treeMapEntry.left) {
                            break;
                        }
                        treeMapEntry2 = treeMapEntry.parent;
                    }
                    return treeMapEntry;
                }
            } else {
                return treeMapEntry;
            }
        }
        return null;
    }

    final TreeMapEntry<K, V> getHigherEntry(K k) {
        TreeMapEntry<K, V> treeMapEntry = this.root;
        while (treeMapEntry != null) {
            if (compare(k, treeMapEntry.key) < 0) {
                if (treeMapEntry.left != null) {
                    treeMapEntry = treeMapEntry.left;
                } else {
                    return treeMapEntry;
                }
            } else if (treeMapEntry.right != null) {
                treeMapEntry = treeMapEntry.right;
            } else {
                TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.parent;
                while (true) {
                    TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry;
                    treeMapEntry = treeMapEntry2;
                    if (treeMapEntry == null || treeMapEntry3 != treeMapEntry.right) {
                        break;
                    }
                    treeMapEntry2 = treeMapEntry.parent;
                }
                return treeMapEntry;
            }
        }
        return null;
    }

    final TreeMapEntry<K, V> getLowerEntry(K k) {
        TreeMapEntry<K, V> treeMapEntry = this.root;
        while (treeMapEntry != null) {
            if (compare(k, treeMapEntry.key) > 0) {
                if (treeMapEntry.right != null) {
                    treeMapEntry = treeMapEntry.right;
                } else {
                    return treeMapEntry;
                }
            } else if (treeMapEntry.left != null) {
                treeMapEntry = treeMapEntry.left;
            } else {
                TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.parent;
                while (true) {
                    TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry;
                    treeMapEntry = treeMapEntry2;
                    if (treeMapEntry == null || treeMapEntry3 != treeMapEntry.left) {
                        break;
                    }
                    treeMapEntry2 = treeMapEntry.parent;
                }
                return treeMapEntry;
            }
        }
        return null;
    }

    @Override
    public V put(K k, V v) {
        int iCompareTo;
        TreeMapEntry<K, V> treeMapEntry;
        TreeMapEntry<K, V> treeMapEntry2;
        TreeMapEntry<K, V> treeMapEntry3 = this.root;
        if (treeMapEntry3 == null) {
            compare(k, k);
            this.root = new TreeMapEntry<>(k, v, null);
            this.size = 1;
            this.modCount++;
            return null;
        }
        Comparator<? super K> comparator = this.comparator;
        if (comparator != null) {
            while (true) {
                iCompareTo = comparator.compare(k, treeMapEntry3.key);
                if (iCompareTo < 0) {
                    treeMapEntry2 = treeMapEntry3.left;
                } else if (iCompareTo > 0) {
                    treeMapEntry2 = treeMapEntry3.right;
                } else {
                    return treeMapEntry3.setValue(v);
                }
                if (treeMapEntry2 == null) {
                    break;
                }
                treeMapEntry3 = treeMapEntry2;
            }
        } else {
            if (k == null) {
                throw new NullPointerException();
            }
            Comparable comparable = (Comparable) k;
            while (true) {
                iCompareTo = comparable.compareTo(treeMapEntry3.key);
                if (iCompareTo < 0) {
                    treeMapEntry = treeMapEntry3.left;
                } else if (iCompareTo > 0) {
                    treeMapEntry = treeMapEntry3.right;
                } else {
                    return treeMapEntry3.setValue(v);
                }
                if (treeMapEntry == null) {
                    break;
                }
                treeMapEntry3 = treeMapEntry;
            }
        }
        TreeMapEntry<K, V> treeMapEntry4 = new TreeMapEntry<>(k, v, treeMapEntry3);
        if (iCompareTo < 0) {
            treeMapEntry3.left = treeMapEntry4;
        } else {
            treeMapEntry3.right = treeMapEntry4;
        }
        fixAfterInsertion(treeMapEntry4);
        this.size++;
        this.modCount++;
        return null;
    }

    @Override
    public V remove(Object obj) {
        TreeMapEntry<K, V> entry = getEntry(obj);
        if (entry == null) {
            return null;
        }
        V v = entry.value;
        deleteEntry(entry);
        return v;
    }

    @Override
    public void clear() {
        this.modCount++;
        this.size = 0;
        this.root = null;
    }

    @Override
    public Object clone() {
        try {
            TreeMap treeMap = (TreeMap) super.clone();
            treeMap.root = null;
            treeMap.size = 0;
            treeMap.modCount = 0;
            treeMap.entrySet = null;
            treeMap.navigableKeySet = null;
            treeMap.descendingMap = null;
            try {
                treeMap.buildFromSorted(this.size, entrySet().iterator(), null, null);
            } catch (IOException e) {
            } catch (ClassNotFoundException e2) {
            }
            return treeMap;
        } catch (CloneNotSupportedException e3) {
            throw new InternalError(e3);
        }
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        return exportEntry(getFirstEntry());
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return exportEntry(getLastEntry());
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        TreeMapEntry<K, V> firstEntry = getFirstEntry();
        Map.Entry<K, V> entryExportEntry = exportEntry(firstEntry);
        if (firstEntry != null) {
            deleteEntry(firstEntry);
        }
        return entryExportEntry;
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        TreeMapEntry<K, V> lastEntry = getLastEntry();
        Map.Entry<K, V> entryExportEntry = exportEntry(lastEntry);
        if (lastEntry != null) {
            deleteEntry(lastEntry);
        }
        return entryExportEntry;
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K k) {
        return exportEntry(getLowerEntry(k));
    }

    @Override
    public K lowerKey(K k) {
        return (K) keyOrNull(getLowerEntry(k));
    }

    @Override
    public Map.Entry<K, V> floorEntry(K k) {
        return exportEntry(getFloorEntry(k));
    }

    @Override
    public K floorKey(K k) {
        return (K) keyOrNull(getFloorEntry(k));
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K k) {
        return exportEntry(getCeilingEntry(k));
    }

    @Override
    public K ceilingKey(K k) {
        return (K) keyOrNull(getCeilingEntry(k));
    }

    @Override
    public Map.Entry<K, V> higherEntry(K k) {
        return exportEntry(getHigherEntry(k));
    }

    @Override
    public K higherKey(K k) {
        return (K) keyOrNull(getHigherEntry(k));
    }

    @Override
    public Set<K> keySet() {
        return navigableKeySet();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        KeySet<K> keySet = this.navigableKeySet;
        if (keySet != null) {
            return keySet;
        }
        KeySet<K> keySet2 = new KeySet<>(this);
        this.navigableKeySet = keySet2;
        return keySet2;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection == null) {
            Values values = new Values();
            this.values = values;
            return values;
        }
        return collection;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        TreeMap<K, V>.EntrySet entrySet = this.entrySet;
        if (entrySet != null) {
            return entrySet;
        }
        TreeMap<K, V>.EntrySet entrySet2 = new EntrySet();
        this.entrySet = entrySet2;
        return entrySet2;
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        NavigableMap<K, V> navigableMap = this.descendingMap;
        if (navigableMap != null) {
            return navigableMap;
        }
        DescendingSubMap descendingSubMap = new DescendingSubMap(this, BLACK, null, BLACK, BLACK, null, BLACK);
        this.descendingMap = descendingSubMap;
        return descendingSubMap;
    }

    @Override
    public NavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
        return new AscendingSubMap(this, RED, k, z, RED, k2, z2);
    }

    @Override
    public NavigableMap<K, V> headMap(K k, boolean z) {
        return new AscendingSubMap(this, BLACK, null, BLACK, RED, k, z);
    }

    @Override
    public NavigableMap<K, V> tailMap(K k, boolean z) {
        return new AscendingSubMap(this, RED, k, z, BLACK, null, BLACK);
    }

    @Override
    public SortedMap<K, V> subMap(K k, K k2) {
        return subMap(k, BLACK, k2, RED);
    }

    @Override
    public SortedMap<K, V> headMap(K k) {
        return headMap(k, RED);
    }

    @Override
    public SortedMap<K, V> tailMap(K k) {
        return tailMap(k, BLACK);
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        TreeMapEntry<K, V> entry = getEntry(k);
        if (entry != null && Objects.equals(v, entry.value)) {
            entry.value = v2;
            return BLACK;
        }
        return RED;
    }

    @Override
    public V replace(K k, V v) {
        TreeMapEntry<K, V> entry = getEntry(k);
        if (entry != null) {
            V v2 = entry.value;
            entry.value = v;
            return v2;
        }
        return null;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Objects.requireNonNull(biConsumer);
        int i = this.modCount;
        for (TreeMapEntry<K, V> firstEntry = getFirstEntry(); firstEntry != null; firstEntry = successor(firstEntry)) {
            biConsumer.accept(firstEntry.key, firstEntry.value);
            if (i != this.modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Objects.requireNonNull(biFunction);
        int i = this.modCount;
        for (TreeMapEntry<K, V> firstEntry = getFirstEntry(); firstEntry != null; firstEntry = successor(firstEntry)) {
            firstEntry.value = biFunction.apply(firstEntry.key, firstEntry.value);
            if (i != this.modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    class Values extends AbstractCollection<V> {
        Values() {
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator(TreeMap.this.getFirstEntry());
        }

        @Override
        public int size() {
            return TreeMap.this.size();
        }

        @Override
        public boolean contains(Object obj) {
            return TreeMap.this.containsValue(obj);
        }

        @Override
        public boolean remove(Object obj) {
            for (TreeMapEntry<K, V> firstEntry = TreeMap.this.getFirstEntry(); firstEntry != null; firstEntry = TreeMap.successor(firstEntry)) {
                if (TreeMap.valEquals(firstEntry.getValue(), obj)) {
                    TreeMap.this.deleteEntry(firstEntry);
                    return TreeMap.BLACK;
                }
            }
            return TreeMap.RED;
        }

        @Override
        public void clear() {
            TreeMap.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return new ValueSpliterator(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        EntrySet() {
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator(TreeMap.this.getFirstEntry());
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return TreeMap.RED;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object value = entry.getValue();
            TreeMapEntry<K, V> entry2 = TreeMap.this.getEntry(entry.getKey());
            return (entry2 == null || !TreeMap.valEquals(entry2.getValue(), value)) ? TreeMap.RED : TreeMap.BLACK;
        }

        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return TreeMap.RED;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object value = entry.getValue();
            TreeMapEntry<K, V> entry2 = TreeMap.this.getEntry(entry.getKey());
            if (entry2 == null || !TreeMap.valEquals(entry2.getValue(), value)) {
                return TreeMap.RED;
            }
            TreeMap.this.deleteEntry(entry2);
            return TreeMap.BLACK;
        }

        @Override
        public int size() {
            return TreeMap.this.size();
        }

        @Override
        public void clear() {
            TreeMap.this.clear();
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    Iterator<K> keyIterator() {
        return new KeyIterator(getFirstEntry());
    }

    Iterator<K> descendingKeyIterator() {
        return new DescendingKeyIterator(getLastEntry());
    }

    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableMap<E, ?> m;

        KeySet(NavigableMap<E, ?> navigableMap) {
            this.m = navigableMap;
        }

        @Override
        public Iterator<E> iterator() {
            if (this.m instanceof TreeMap) {
                return ((TreeMap) this.m).keyIterator();
            }
            return ((NavigableSubMap) this.m).keyIterator();
        }

        @Override
        public Iterator<E> descendingIterator() {
            if (this.m instanceof TreeMap) {
                return ((TreeMap) this.m).descendingKeyIterator();
            }
            return ((NavigableSubMap) this.m).descendingKeyIterator();
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
        public E first() {
            return this.m.firstKey();
        }

        @Override
        public E last() {
            return this.m.lastKey();
        }

        @Override
        public Comparator<? super E> comparator() {
            return this.m.comparator();
        }

        @Override
        public E pollFirst() {
            Map.Entry<E, ?> entryPollFirstEntry = this.m.pollFirstEntry();
            if (entryPollFirstEntry == null) {
                return null;
            }
            return entryPollFirstEntry.getKey();
        }

        @Override
        public E pollLast() {
            Map.Entry<E, ?> entryPollLastEntry = this.m.pollLastEntry();
            if (entryPollLastEntry == null) {
                return null;
            }
            return entryPollLastEntry.getKey();
        }

        @Override
        public boolean remove(Object obj) {
            int size = size();
            this.m.remove(obj);
            return size() != size ? TreeMap.BLACK : TreeMap.RED;
        }

        @Override
        public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
            return new KeySet(this.m.subMap(e, z, e2, z2));
        }

        @Override
        public NavigableSet<E> headSet(E e, boolean z) {
            return new KeySet(this.m.headMap(e, z));
        }

        @Override
        public NavigableSet<E> tailSet(E e, boolean z) {
            return new KeySet(this.m.tailMap(e, z));
        }

        @Override
        public SortedSet<E> subSet(E e, E e2) {
            return subSet(e, TreeMap.BLACK, e2, TreeMap.RED);
        }

        @Override
        public SortedSet<E> headSet(E e) {
            return headSet(e, TreeMap.RED);
        }

        @Override
        public SortedSet<E> tailSet(E e) {
            return tailSet(e, TreeMap.BLACK);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return new KeySet(this.m.descendingMap());
        }

        @Override
        public Spliterator<E> spliterator() {
            return TreeMap.keySpliteratorFor(this.m);
        }
    }

    abstract class PrivateEntryIterator<T> implements Iterator<T> {
        int expectedModCount;
        TreeMapEntry<K, V> lastReturned = null;
        TreeMapEntry<K, V> next;

        PrivateEntryIterator(TreeMapEntry<K, V> treeMapEntry) {
            this.expectedModCount = TreeMap.this.modCount;
            this.next = treeMapEntry;
        }

        @Override
        public final boolean hasNext() {
            return this.next != null ? TreeMap.BLACK : TreeMap.RED;
        }

        final TreeMapEntry<K, V> nextEntry() {
            TreeMapEntry<K, V> treeMapEntry = this.next;
            if (treeMapEntry != null) {
                if (TreeMap.this.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                this.next = TreeMap.successor(treeMapEntry);
                this.lastReturned = treeMapEntry;
                return treeMapEntry;
            }
            throw new NoSuchElementException();
        }

        final TreeMapEntry<K, V> prevEntry() {
            TreeMapEntry<K, V> treeMapEntry = this.next;
            if (treeMapEntry != null) {
                if (TreeMap.this.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                this.next = TreeMap.predecessor(treeMapEntry);
                this.lastReturned = treeMapEntry;
                return treeMapEntry;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            if (this.lastReturned != null) {
                if (TreeMap.this.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                if (this.lastReturned.left != null && this.lastReturned.right != null) {
                    this.next = this.lastReturned;
                }
                TreeMap.this.deleteEntry(this.lastReturned);
                this.expectedModCount = TreeMap.this.modCount;
                this.lastReturned = null;
                return;
            }
            throw new IllegalStateException();
        }
    }

    final class EntryIterator extends TreeMap<K, V>.PrivateEntryIterator<Map.Entry<K, V>> {
        EntryIterator(TreeMapEntry<K, V> treeMapEntry) {
            super(treeMapEntry);
        }

        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    final class ValueIterator extends TreeMap<K, V>.PrivateEntryIterator<V> {
        ValueIterator(TreeMapEntry<K, V> treeMapEntry) {
            super(treeMapEntry);
        }

        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    final class KeyIterator extends TreeMap<K, V>.PrivateEntryIterator<K> {
        KeyIterator(TreeMapEntry<K, V> treeMapEntry) {
            super(treeMapEntry);
        }

        @Override
        public K next() {
            return nextEntry().key;
        }
    }

    final class DescendingKeyIterator extends TreeMap<K, V>.PrivateEntryIterator<K> {
        DescendingKeyIterator(TreeMapEntry<K, V> treeMapEntry) {
            super(treeMapEntry);
        }

        @Override
        public K next() {
            return prevEntry().key;
        }

        @Override
        public void remove() {
            if (this.lastReturned != null) {
                if (TreeMap.this.modCount == this.expectedModCount) {
                    TreeMap.this.deleteEntry(this.lastReturned);
                    this.lastReturned = null;
                    this.expectedModCount = TreeMap.this.modCount;
                    return;
                }
                throw new ConcurrentModificationException();
            }
            throw new IllegalStateException();
        }
    }

    final int compare(Object obj, Object obj2) {
        return this.comparator == null ? ((Comparable) obj).compareTo(obj2) : this.comparator.compare(obj, obj2);
    }

    static final boolean valEquals(Object obj, Object obj2) {
        return obj == null ? obj2 == null ? BLACK : RED : obj.equals(obj2);
    }

    static <K, V> Map.Entry<K, V> exportEntry(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry == null) {
            return null;
        }
        return new AbstractMap.SimpleImmutableEntry(treeMapEntry);
    }

    static <K, V> K keyOrNull(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry == null) {
            return null;
        }
        return treeMapEntry.key;
    }

    static <K> K key(TreeMapEntry<K, ?> treeMapEntry) {
        if (treeMapEntry == null) {
            throw new NoSuchElementException();
        }
        return treeMapEntry.key;
    }

    static abstract class NavigableSubMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Serializable {
        private static final long serialVersionUID = 2765629423043303731L;
        transient NavigableMap<K, V> descendingMapView;
        transient NavigableSubMap<K, V>.EntrySetView entrySetView;
        final boolean fromStart;
        final K hi;
        final boolean hiInclusive;
        final K lo;
        final boolean loInclusive;
        final TreeMap<K, V> m;
        transient KeySet<K> navigableKeySetView;
        final boolean toEnd;

        abstract Iterator<K> descendingKeyIterator();

        abstract Iterator<K> keyIterator();

        abstract Spliterator<K> keySpliterator();

        abstract TreeMapEntry<K, V> subCeiling(K k);

        abstract TreeMapEntry<K, V> subFloor(K k);

        abstract TreeMapEntry<K, V> subHigher(K k);

        abstract TreeMapEntry<K, V> subHighest();

        abstract TreeMapEntry<K, V> subLower(K k);

        abstract TreeMapEntry<K, V> subLowest();

        NavigableSubMap(TreeMap<K, V> treeMap, boolean z, K k, boolean z2, boolean z3, K k2, boolean z4) {
            if (!z && !z3) {
                if (treeMap.compare(k, k2) > 0) {
                    throw new IllegalArgumentException("fromKey > toKey");
                }
            } else {
                if (!z) {
                    treeMap.compare(k, k);
                }
                if (!z3) {
                    treeMap.compare(k2, k2);
                }
            }
            this.m = treeMap;
            this.fromStart = z;
            this.lo = k;
            this.loInclusive = z2;
            this.toEnd = z3;
            this.hi = k2;
            this.hiInclusive = z4;
        }

        final boolean tooLow(Object obj) {
            if (!this.fromStart) {
                int iCompare = this.m.compare(obj, this.lo);
                if (iCompare < 0) {
                    return TreeMap.BLACK;
                }
                if (iCompare == 0 && !this.loInclusive) {
                    return TreeMap.BLACK;
                }
                return TreeMap.RED;
            }
            return TreeMap.RED;
        }

        final boolean tooHigh(Object obj) {
            if (!this.toEnd) {
                int iCompare = this.m.compare(obj, this.hi);
                if (iCompare > 0) {
                    return TreeMap.BLACK;
                }
                if (iCompare == 0 && !this.hiInclusive) {
                    return TreeMap.BLACK;
                }
                return TreeMap.RED;
            }
            return TreeMap.RED;
        }

        final boolean inRange(Object obj) {
            return (tooLow(obj) || tooHigh(obj)) ? TreeMap.RED : TreeMap.BLACK;
        }

        final boolean inClosedRange(Object obj) {
            return ((this.fromStart || this.m.compare(obj, this.lo) >= 0) && (this.toEnd || this.m.compare(this.hi, obj) >= 0)) ? TreeMap.BLACK : TreeMap.RED;
        }

        final boolean inRange(Object obj, boolean z) {
            return z ? inRange(obj) : inClosedRange(obj);
        }

        final TreeMapEntry<K, V> absLowest() {
            TreeMapEntry<K, V> ceilingEntry;
            if (this.fromStart) {
                ceilingEntry = this.m.getFirstEntry();
            } else {
                ceilingEntry = this.loInclusive ? this.m.getCeilingEntry(this.lo) : this.m.getHigherEntry(this.lo);
            }
            if (ceilingEntry == null || tooHigh(ceilingEntry.key)) {
                return null;
            }
            return ceilingEntry;
        }

        final TreeMapEntry<K, V> absHighest() {
            TreeMapEntry<K, V> floorEntry;
            if (this.toEnd) {
                floorEntry = this.m.getLastEntry();
            } else {
                floorEntry = this.hiInclusive ? this.m.getFloorEntry(this.hi) : this.m.getLowerEntry(this.hi);
            }
            if (floorEntry == null || tooLow(floorEntry.key)) {
                return null;
            }
            return floorEntry;
        }

        final TreeMapEntry<K, V> absCeiling(K k) {
            if (tooLow(k)) {
                return absLowest();
            }
            TreeMapEntry<K, V> ceilingEntry = this.m.getCeilingEntry(k);
            if (ceilingEntry == null || tooHigh(ceilingEntry.key)) {
                return null;
            }
            return ceilingEntry;
        }

        final TreeMapEntry<K, V> absHigher(K k) {
            if (tooLow(k)) {
                return absLowest();
            }
            TreeMapEntry<K, V> higherEntry = this.m.getHigherEntry(k);
            if (higherEntry == null || tooHigh(higherEntry.key)) {
                return null;
            }
            return higherEntry;
        }

        final TreeMapEntry<K, V> absFloor(K k) {
            if (tooHigh(k)) {
                return absHighest();
            }
            TreeMapEntry<K, V> floorEntry = this.m.getFloorEntry(k);
            if (floorEntry == null || tooLow(floorEntry.key)) {
                return null;
            }
            return floorEntry;
        }

        final TreeMapEntry<K, V> absLower(K k) {
            if (tooHigh(k)) {
                return absHighest();
            }
            TreeMapEntry<K, V> lowerEntry = this.m.getLowerEntry(k);
            if (lowerEntry == null || tooLow(lowerEntry.key)) {
                return null;
            }
            return lowerEntry;
        }

        final TreeMapEntry<K, V> absHighFence() {
            if (this.toEnd) {
                return null;
            }
            if (this.hiInclusive) {
                return this.m.getHigherEntry(this.hi);
            }
            return this.m.getCeilingEntry(this.hi);
        }

        final TreeMapEntry<K, V> absLowFence() {
            if (this.fromStart) {
                return null;
            }
            if (this.loInclusive) {
                return this.m.getLowerEntry(this.lo);
            }
            return this.m.getFloorEntry(this.lo);
        }

        @Override
        public boolean isEmpty() {
            return (this.fromStart && this.toEnd) ? this.m.isEmpty() : entrySet().isEmpty();
        }

        @Override
        public int size() {
            return (this.fromStart && this.toEnd) ? this.m.size() : entrySet().size();
        }

        @Override
        public final boolean containsKey(Object obj) {
            return (inRange(obj) && this.m.containsKey(obj)) ? TreeMap.BLACK : TreeMap.RED;
        }

        @Override
        public final V put(K k, V v) {
            if (!inRange(k)) {
                throw new IllegalArgumentException("key out of range");
            }
            return this.m.put(k, v);
        }

        @Override
        public final V get(Object obj) {
            if (inRange(obj)) {
                return this.m.get(obj);
            }
            return null;
        }

        @Override
        public final V remove(Object obj) {
            if (inRange(obj)) {
                return this.m.remove(obj);
            }
            return null;
        }

        @Override
        public final Map.Entry<K, V> ceilingEntry(K k) {
            return TreeMap.exportEntry(subCeiling(k));
        }

        @Override
        public final K ceilingKey(K k) {
            return (K) TreeMap.keyOrNull(subCeiling(k));
        }

        @Override
        public final Map.Entry<K, V> higherEntry(K k) {
            return TreeMap.exportEntry(subHigher(k));
        }

        @Override
        public final K higherKey(K k) {
            return (K) TreeMap.keyOrNull(subHigher(k));
        }

        @Override
        public final Map.Entry<K, V> floorEntry(K k) {
            return TreeMap.exportEntry(subFloor(k));
        }

        @Override
        public final K floorKey(K k) {
            return (K) TreeMap.keyOrNull(subFloor(k));
        }

        @Override
        public final Map.Entry<K, V> lowerEntry(K k) {
            return TreeMap.exportEntry(subLower(k));
        }

        @Override
        public final K lowerKey(K k) {
            return (K) TreeMap.keyOrNull(subLower(k));
        }

        @Override
        public final K firstKey() {
            return (K) TreeMap.key(subLowest());
        }

        @Override
        public final K lastKey() {
            return (K) TreeMap.key(subHighest());
        }

        @Override
        public final Map.Entry<K, V> firstEntry() {
            return TreeMap.exportEntry(subLowest());
        }

        @Override
        public final Map.Entry<K, V> lastEntry() {
            return TreeMap.exportEntry(subHighest());
        }

        @Override
        public final Map.Entry<K, V> pollFirstEntry() {
            TreeMapEntry<K, V> treeMapEntrySubLowest = subLowest();
            Map.Entry<K, V> entryExportEntry = TreeMap.exportEntry(treeMapEntrySubLowest);
            if (treeMapEntrySubLowest != null) {
                this.m.deleteEntry(treeMapEntrySubLowest);
            }
            return entryExportEntry;
        }

        @Override
        public final Map.Entry<K, V> pollLastEntry() {
            TreeMapEntry<K, V> treeMapEntrySubHighest = subHighest();
            Map.Entry<K, V> entryExportEntry = TreeMap.exportEntry(treeMapEntrySubHighest);
            if (treeMapEntrySubHighest != null) {
                this.m.deleteEntry(treeMapEntrySubHighest);
            }
            return entryExportEntry;
        }

        @Override
        public final NavigableSet<K> navigableKeySet() {
            KeySet<K> keySet = this.navigableKeySetView;
            if (keySet != null) {
                return keySet;
            }
            KeySet<K> keySet2 = new KeySet<>(this);
            this.navigableKeySetView = keySet2;
            return keySet2;
        }

        @Override
        public final Set<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        @Override
        public final SortedMap<K, V> subMap(K k, K k2) {
            return subMap(k, TreeMap.BLACK, k2, TreeMap.RED);
        }

        @Override
        public final SortedMap<K, V> headMap(K k) {
            return headMap(k, TreeMap.RED);
        }

        @Override
        public final SortedMap<K, V> tailMap(K k) {
            return tailMap(k, TreeMap.BLACK);
        }

        abstract class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
            private transient int size = -1;
            private transient int sizeModCount;

            EntrySetView() {
            }

            @Override
            public int size() {
                if (NavigableSubMap.this.fromStart && NavigableSubMap.this.toEnd) {
                    return NavigableSubMap.this.m.size();
                }
                if (this.size == -1 || this.sizeModCount != ((TreeMap) NavigableSubMap.this.m).modCount) {
                    this.sizeModCount = ((TreeMap) NavigableSubMap.this.m).modCount;
                    this.size = 0;
                    Iterator<Map.Entry<K, V>> it = iterator();
                    while (it.hasNext()) {
                        this.size++;
                        it.next();
                    }
                }
                return this.size;
            }

            @Override
            public boolean isEmpty() {
                TreeMapEntry<K, V> treeMapEntryAbsLowest = NavigableSubMap.this.absLowest();
                return (treeMapEntryAbsLowest == null || NavigableSubMap.this.tooHigh(treeMapEntryAbsLowest.key)) ? TreeMap.BLACK : TreeMap.RED;
            }

            @Override
            public boolean contains(Object obj) {
                TreeMapEntry<K, V> entry;
                if (!(obj instanceof Map.Entry)) {
                    return TreeMap.RED;
                }
                Map.Entry entry2 = (Map.Entry) obj;
                Object key = entry2.getKey();
                return (NavigableSubMap.this.inRange(key) && (entry = NavigableSubMap.this.m.getEntry(key)) != null && TreeMap.valEquals(entry.getValue(), entry2.getValue())) ? TreeMap.BLACK : TreeMap.RED;
            }

            @Override
            public boolean remove(Object obj) {
                TreeMapEntry<K, V> entry;
                if (!(obj instanceof Map.Entry)) {
                    return TreeMap.RED;
                }
                Map.Entry entry2 = (Map.Entry) obj;
                Object key = entry2.getKey();
                if (!NavigableSubMap.this.inRange(key) || (entry = NavigableSubMap.this.m.getEntry(key)) == null || !TreeMap.valEquals(entry.getValue(), entry2.getValue())) {
                    return TreeMap.RED;
                }
                NavigableSubMap.this.m.deleteEntry(entry);
                return TreeMap.BLACK;
            }
        }

        abstract class SubMapIterator<T> implements Iterator<T> {
            int expectedModCount;
            final Object fenceKey;
            TreeMapEntry<K, V> lastReturned = null;
            TreeMapEntry<K, V> next;

            SubMapIterator(TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2) {
                this.expectedModCount = ((TreeMap) NavigableSubMap.this.m).modCount;
                this.next = treeMapEntry;
                this.fenceKey = treeMapEntry2 == null ? TreeMap.UNBOUNDED : treeMapEntry2.key;
            }

            @Override
            public final boolean hasNext() {
                return (this.next == null || this.next.key == this.fenceKey) ? TreeMap.RED : TreeMap.BLACK;
            }

            final TreeMapEntry<K, V> nextEntry() {
                TreeMapEntry<K, V> treeMapEntry = this.next;
                if (treeMapEntry != null && treeMapEntry.key != this.fenceKey) {
                    if (((TreeMap) NavigableSubMap.this.m).modCount != this.expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                    this.next = TreeMap.successor(treeMapEntry);
                    this.lastReturned = treeMapEntry;
                    return treeMapEntry;
                }
                throw new NoSuchElementException();
            }

            final TreeMapEntry<K, V> prevEntry() {
                TreeMapEntry<K, V> treeMapEntry = this.next;
                if (treeMapEntry != null && treeMapEntry.key != this.fenceKey) {
                    if (((TreeMap) NavigableSubMap.this.m).modCount != this.expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                    this.next = TreeMap.predecessor(treeMapEntry);
                    this.lastReturned = treeMapEntry;
                    return treeMapEntry;
                }
                throw new NoSuchElementException();
            }

            final void removeAscending() {
                if (this.lastReturned != null) {
                    if (((TreeMap) NavigableSubMap.this.m).modCount != this.expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                    if (this.lastReturned.left != null && this.lastReturned.right != null) {
                        this.next = this.lastReturned;
                    }
                    NavigableSubMap.this.m.deleteEntry(this.lastReturned);
                    this.lastReturned = null;
                    this.expectedModCount = ((TreeMap) NavigableSubMap.this.m).modCount;
                    return;
                }
                throw new IllegalStateException();
            }

            final void removeDescending() {
                if (this.lastReturned != null) {
                    if (((TreeMap) NavigableSubMap.this.m).modCount == this.expectedModCount) {
                        NavigableSubMap.this.m.deleteEntry(this.lastReturned);
                        this.lastReturned = null;
                        this.expectedModCount = ((TreeMap) NavigableSubMap.this.m).modCount;
                        return;
                    }
                    throw new ConcurrentModificationException();
                }
                throw new IllegalStateException();
            }
        }

        final class SubMapEntryIterator extends NavigableSubMap<K, V>.SubMapIterator<Map.Entry<K, V>> {
            SubMapEntryIterator(TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2) {
                super(treeMapEntry, treeMapEntry2);
            }

            @Override
            public Map.Entry<K, V> next() {
                return nextEntry();
            }

            @Override
            public void remove() {
                removeAscending();
            }
        }

        final class DescendingSubMapEntryIterator extends NavigableSubMap<K, V>.SubMapIterator<Map.Entry<K, V>> {
            DescendingSubMapEntryIterator(TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2) {
                super(treeMapEntry, treeMapEntry2);
            }

            @Override
            public Map.Entry<K, V> next() {
                return prevEntry();
            }

            @Override
            public void remove() {
                removeDescending();
            }
        }

        final class SubMapKeyIterator extends NavigableSubMap<K, V>.SubMapIterator<K> implements Spliterator<K> {
            SubMapKeyIterator(TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2) {
                super(treeMapEntry, treeMapEntry2);
            }

            @Override
            public K next() {
                return nextEntry().key;
            }

            @Override
            public void remove() {
                removeAscending();
            }

            @Override
            public Spliterator<K> trySplit() {
                return null;
            }

            @Override
            public void forEachRemaining(Consumer<? super K> consumer) {
                while (hasNext()) {
                    consumer.accept((Object) next());
                }
            }

            @Override
            public boolean tryAdvance(Consumer<? super K> consumer) {
                if (hasNext()) {
                    consumer.accept((Object) next());
                    return TreeMap.BLACK;
                }
                return TreeMap.RED;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return 21;
            }

            @Override
            public final Comparator<? super K> getComparator() {
                return NavigableSubMap.this.comparator();
            }
        }

        final class DescendingSubMapKeyIterator extends NavigableSubMap<K, V>.SubMapIterator<K> implements Spliterator<K> {
            DescendingSubMapKeyIterator(TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2) {
                super(treeMapEntry, treeMapEntry2);
            }

            @Override
            public K next() {
                return prevEntry().key;
            }

            @Override
            public void remove() {
                removeDescending();
            }

            @Override
            public Spliterator<K> trySplit() {
                return null;
            }

            @Override
            public void forEachRemaining(Consumer<? super K> consumer) {
                while (hasNext()) {
                    consumer.accept((Object) next());
                }
            }

            @Override
            public boolean tryAdvance(Consumer<? super K> consumer) {
                if (hasNext()) {
                    consumer.accept((Object) next());
                    return TreeMap.BLACK;
                }
                return TreeMap.RED;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return 17;
            }
        }
    }

    static final class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {
        private static final long serialVersionUID = 912986545866124060L;

        AscendingSubMap(TreeMap<K, V> treeMap, boolean z, K k, boolean z2, boolean z3, K k2, boolean z4) {
            super(treeMap, z, k, z2, z3, k2, z4);
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.m.comparator();
        }

        @Override
        public NavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
            if (!inRange(k, z)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(k2, z2)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap(this.m, TreeMap.RED, k, z, TreeMap.RED, k2, z2);
        }

        @Override
        public NavigableMap<K, V> headMap(K k, boolean z) {
            if (!inRange(k) && (this.toEnd || this.m.compare(k, this.hi) != 0 || this.hiInclusive || z)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap(this.m, this.fromStart, this.lo, this.loInclusive, TreeMap.RED, k, z);
        }

        @Override
        public NavigableMap<K, V> tailMap(K k, boolean z) {
            if (!inRange(k) && (this.fromStart || this.m.compare(k, this.lo) != 0 || this.loInclusive || z)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new AscendingSubMap(this.m, TreeMap.RED, k, z, this.toEnd, this.hi, this.hiInclusive);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            NavigableMap<K, V> navigableMap = this.descendingMapView;
            if (navigableMap != null) {
                return navigableMap;
            }
            DescendingSubMap descendingSubMap = new DescendingSubMap(this.m, this.fromStart, this.lo, this.loInclusive, this.toEnd, this.hi, this.hiInclusive);
            this.descendingMapView = descendingSubMap;
            return descendingSubMap;
        }

        @Override
        Iterator<K> keyIterator() {
            return new NavigableSubMap.SubMapKeyIterator(absLowest(), absHighFence());
        }

        @Override
        Spliterator<K> keySpliterator() {
            return new NavigableSubMap.SubMapKeyIterator(absLowest(), absHighFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new NavigableSubMap.DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        final class AscendingEntrySetView extends NavigableSubMap<K, V>.EntrySetView {
            AscendingEntrySetView() {
                super();
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new NavigableSubMap.SubMapEntryIterator(AscendingSubMap.this.absLowest(), AscendingSubMap.this.absHighFence());
            }
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            NavigableSubMap<K, V>.EntrySetView entrySetView = this.entrySetView;
            if (entrySetView != null) {
                return entrySetView;
            }
            AscendingEntrySetView ascendingEntrySetView = new AscendingEntrySetView();
            this.entrySetView = ascendingEntrySetView;
            return ascendingEntrySetView;
        }

        @Override
        TreeMapEntry<K, V> subLowest() {
            return absLowest();
        }

        @Override
        TreeMapEntry<K, V> subHighest() {
            return absHighest();
        }

        @Override
        TreeMapEntry<K, V> subCeiling(K k) {
            return absCeiling(k);
        }

        @Override
        TreeMapEntry<K, V> subHigher(K k) {
            return absHigher(k);
        }

        @Override
        TreeMapEntry<K, V> subFloor(K k) {
            return absFloor(k);
        }

        @Override
        TreeMapEntry<K, V> subLower(K k) {
            return absLower(k);
        }
    }

    static final class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {
        private static final long serialVersionUID = 912986545866120460L;
        private final Comparator<? super K> reverseComparator;

        DescendingSubMap(TreeMap<K, V> treeMap, boolean z, K k, boolean z2, boolean z3, K k2, boolean z4) {
            super(treeMap, z, k, z2, z3, k2, z4);
            this.reverseComparator = Collections.reverseOrder(((TreeMap) this.m).comparator);
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.reverseComparator;
        }

        @Override
        public NavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
            if (!inRange(k, z)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(k2, z2)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap(this.m, TreeMap.RED, k2, z2, TreeMap.RED, k, z);
        }

        @Override
        public NavigableMap<K, V> headMap(K k, boolean z) {
            if (!inRange(k) && (this.fromStart || this.m.compare(k, this.lo) != 0 || this.loInclusive || z)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap(this.m, TreeMap.RED, k, z, this.toEnd, this.hi, this.hiInclusive);
        }

        @Override
        public NavigableMap<K, V> tailMap(K k, boolean z) {
            if (!inRange(k) && (this.toEnd || this.m.compare(k, this.hi) != 0 || this.hiInclusive || z)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new DescendingSubMap(this.m, this.fromStart, this.lo, this.loInclusive, TreeMap.RED, k, z);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            NavigableMap<K, V> navigableMap = this.descendingMapView;
            if (navigableMap != null) {
                return navigableMap;
            }
            AscendingSubMap ascendingSubMap = new AscendingSubMap(this.m, this.fromStart, this.lo, this.loInclusive, this.toEnd, this.hi, this.hiInclusive);
            this.descendingMapView = ascendingSubMap;
            return ascendingSubMap;
        }

        @Override
        Iterator<K> keyIterator() {
            return new NavigableSubMap.DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        @Override
        Spliterator<K> keySpliterator() {
            return new NavigableSubMap.DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new NavigableSubMap.SubMapKeyIterator(absLowest(), absHighFence());
        }

        final class DescendingEntrySetView extends NavigableSubMap<K, V>.EntrySetView {
            DescendingEntrySetView() {
                super();
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new NavigableSubMap.DescendingSubMapEntryIterator(DescendingSubMap.this.absHighest(), DescendingSubMap.this.absLowFence());
            }
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            NavigableSubMap<K, V>.EntrySetView entrySetView = this.entrySetView;
            if (entrySetView != null) {
                return entrySetView;
            }
            DescendingEntrySetView descendingEntrySetView = new DescendingEntrySetView();
            this.entrySetView = descendingEntrySetView;
            return descendingEntrySetView;
        }

        @Override
        TreeMapEntry<K, V> subLowest() {
            return absHighest();
        }

        @Override
        TreeMapEntry<K, V> subHighest() {
            return absLowest();
        }

        @Override
        TreeMapEntry<K, V> subCeiling(K k) {
            return absFloor(k);
        }

        @Override
        TreeMapEntry<K, V> subHigher(K k) {
            return absLower(k);
        }

        @Override
        TreeMapEntry<K, V> subFloor(K k) {
            return absCeiling(k);
        }

        @Override
        TreeMapEntry<K, V> subLower(K k) {
            return absHigher(k);
        }
    }

    private class SubMap extends AbstractMap<K, V> implements SortedMap<K, V>, Serializable {
        private static final long serialVersionUID = -6520786458950516097L;
        private K fromKey;
        private K toKey;
        private boolean fromStart = TreeMap.RED;
        private boolean toEnd = TreeMap.RED;

        private SubMap() {
        }

        private Object readResolve() {
            return new AscendingSubMap(TreeMap.this, this.fromStart, this.fromKey, TreeMap.BLACK, this.toEnd, this.toKey, TreeMap.RED);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            throw new InternalError();
        }

        @Override
        public K lastKey() {
            throw new InternalError();
        }

        @Override
        public K firstKey() {
            throw new InternalError();
        }

        @Override
        public SortedMap<K, V> subMap(K k, K k2) {
            throw new InternalError();
        }

        @Override
        public SortedMap<K, V> headMap(K k) {
            throw new InternalError();
        }

        @Override
        public SortedMap<K, V> tailMap(K k) {
            throw new InternalError();
        }

        @Override
        public Comparator<? super K> comparator() {
            throw new InternalError();
        }
    }

    static final class TreeMapEntry<K, V> implements Map.Entry<K, V> {
        boolean color = TreeMap.BLACK;
        K key;
        TreeMapEntry<K, V> left;
        TreeMapEntry<K, V> parent;
        TreeMapEntry<K, V> right;
        V value;

        TreeMapEntry(K k, V v, TreeMapEntry<K, V> treeMapEntry) {
            this.key = k;
            this.value = v;
            this.parent = treeMapEntry;
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
        public V setValue(V v) {
            V v2 = this.value;
            this.value = v;
            return v2;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return TreeMap.RED;
            }
            Map.Entry entry = (Map.Entry) obj;
            return (TreeMap.valEquals(this.key, entry.getKey()) && TreeMap.valEquals(this.value, entry.getValue())) ? TreeMap.BLACK : TreeMap.RED;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            if (this.key != null) {
                iHashCode = this.key.hashCode();
            } else {
                iHashCode = 0;
            }
            return iHashCode ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public String toString() {
            return ((Object) this.key) + "=" + ((Object) this.value);
        }
    }

    final TreeMapEntry<K, V> getFirstEntry() {
        TreeMapEntry<K, V> treeMapEntry = this.root;
        if (treeMapEntry != null) {
            while (treeMapEntry.left != null) {
                treeMapEntry = treeMapEntry.left;
            }
        }
        return treeMapEntry;
    }

    final TreeMapEntry<K, V> getLastEntry() {
        TreeMapEntry<K, V> treeMapEntry = this.root;
        if (treeMapEntry != null) {
            while (treeMapEntry.right != null) {
                treeMapEntry = treeMapEntry.right;
            }
        }
        return treeMapEntry;
    }

    static <K, V> TreeMapEntry<K, V> successor(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry == null) {
            return null;
        }
        if (treeMapEntry.right != null) {
            TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.right;
            while (treeMapEntry2.left != null) {
                treeMapEntry2 = treeMapEntry2.left;
            }
            return treeMapEntry2;
        }
        TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry.parent;
        while (true) {
            TreeMapEntry<K, V> treeMapEntry4 = treeMapEntry3;
            TreeMapEntry<K, V> treeMapEntry5 = treeMapEntry;
            treeMapEntry = treeMapEntry4;
            if (treeMapEntry == null || treeMapEntry5 != treeMapEntry.right) {
                break;
            }
            treeMapEntry3 = treeMapEntry.parent;
        }
        return treeMapEntry;
    }

    static <K, V> TreeMapEntry<K, V> predecessor(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry == null) {
            return null;
        }
        if (treeMapEntry.left != null) {
            TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.left;
            while (treeMapEntry2.right != null) {
                treeMapEntry2 = treeMapEntry2.right;
            }
            return treeMapEntry2;
        }
        TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry.parent;
        while (true) {
            TreeMapEntry<K, V> treeMapEntry4 = treeMapEntry3;
            TreeMapEntry<K, V> treeMapEntry5 = treeMapEntry;
            treeMapEntry = treeMapEntry4;
            if (treeMapEntry == null || treeMapEntry5 != treeMapEntry.left) {
                break;
            }
            treeMapEntry3 = treeMapEntry.parent;
        }
        return treeMapEntry;
    }

    private static <K, V> boolean colorOf(TreeMapEntry<K, V> treeMapEntry) {
        return treeMapEntry == null ? BLACK : treeMapEntry.color;
    }

    private static <K, V> TreeMapEntry<K, V> parentOf(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry == null) {
            return null;
        }
        return treeMapEntry.parent;
    }

    private static <K, V> void setColor(TreeMapEntry<K, V> treeMapEntry, boolean z) {
        if (treeMapEntry != null) {
            treeMapEntry.color = z;
        }
    }

    private static <K, V> TreeMapEntry<K, V> leftOf(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry == null) {
            return null;
        }
        return treeMapEntry.left;
    }

    private static <K, V> TreeMapEntry<K, V> rightOf(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry == null) {
            return null;
        }
        return treeMapEntry.right;
    }

    private void rotateLeft(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry != null) {
            TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.right;
            treeMapEntry.right = treeMapEntry2.left;
            if (treeMapEntry2.left != null) {
                treeMapEntry2.left.parent = treeMapEntry;
            }
            treeMapEntry2.parent = treeMapEntry.parent;
            if (treeMapEntry.parent == null) {
                this.root = treeMapEntry2;
            } else if (treeMapEntry.parent.left == treeMapEntry) {
                treeMapEntry.parent.left = treeMapEntry2;
            } else {
                treeMapEntry.parent.right = treeMapEntry2;
            }
            treeMapEntry2.left = treeMapEntry;
            treeMapEntry.parent = treeMapEntry2;
        }
    }

    private void rotateRight(TreeMapEntry<K, V> treeMapEntry) {
        if (treeMapEntry != null) {
            TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.left;
            treeMapEntry.left = treeMapEntry2.right;
            if (treeMapEntry2.right != null) {
                treeMapEntry2.right.parent = treeMapEntry;
            }
            treeMapEntry2.parent = treeMapEntry.parent;
            if (treeMapEntry.parent == null) {
                this.root = treeMapEntry2;
            } else if (treeMapEntry.parent.right == treeMapEntry) {
                treeMapEntry.parent.right = treeMapEntry2;
            } else {
                treeMapEntry.parent.left = treeMapEntry2;
            }
            treeMapEntry2.right = treeMapEntry;
            treeMapEntry.parent = treeMapEntry2;
        }
    }

    private void fixAfterInsertion(TreeMapEntry<K, V> treeMapEntry) {
        treeMapEntry.color = RED;
        while (treeMapEntry != null && treeMapEntry != this.root && !treeMapEntry.parent.color) {
            if (parentOf(treeMapEntry) == leftOf(parentOf(parentOf(treeMapEntry)))) {
                TreeMapEntry treeMapEntryRightOf = rightOf(parentOf(parentOf(treeMapEntry)));
                if (!colorOf(treeMapEntryRightOf)) {
                    setColor(parentOf(treeMapEntry), BLACK);
                    setColor(treeMapEntryRightOf, BLACK);
                    setColor(parentOf(parentOf(treeMapEntry)), RED);
                    treeMapEntry = parentOf(parentOf(treeMapEntry));
                } else {
                    if (treeMapEntry == rightOf(parentOf(treeMapEntry))) {
                        treeMapEntry = parentOf(treeMapEntry);
                        rotateLeft(treeMapEntry);
                    }
                    setColor(parentOf(treeMapEntry), BLACK);
                    setColor(parentOf(parentOf(treeMapEntry)), RED);
                    rotateRight(parentOf(parentOf(treeMapEntry)));
                }
            } else {
                TreeMapEntry treeMapEntryLeftOf = leftOf(parentOf(parentOf(treeMapEntry)));
                if (!colorOf(treeMapEntryLeftOf)) {
                    setColor(parentOf(treeMapEntry), BLACK);
                    setColor(treeMapEntryLeftOf, BLACK);
                    setColor(parentOf(parentOf(treeMapEntry)), RED);
                    treeMapEntry = parentOf(parentOf(treeMapEntry));
                } else {
                    if (treeMapEntry == leftOf(parentOf(treeMapEntry))) {
                        treeMapEntry = parentOf(treeMapEntry);
                        rotateRight(treeMapEntry);
                    }
                    setColor(parentOf(treeMapEntry), BLACK);
                    setColor(parentOf(parentOf(treeMapEntry)), RED);
                    rotateLeft(parentOf(parentOf(treeMapEntry)));
                }
            }
        }
        this.root.color = BLACK;
    }

    private void deleteEntry(TreeMapEntry<K, V> treeMapEntry) {
        this.modCount++;
        this.size--;
        if (treeMapEntry.left != null && treeMapEntry.right != null) {
            TreeMapEntry<K, V> treeMapEntrySuccessor = successor(treeMapEntry);
            treeMapEntry.key = treeMapEntrySuccessor.key;
            treeMapEntry.value = treeMapEntrySuccessor.value;
            treeMapEntry = treeMapEntrySuccessor;
        }
        TreeMapEntry<K, V> treeMapEntry2 = treeMapEntry.left != null ? treeMapEntry.left : treeMapEntry.right;
        if (treeMapEntry2 != null) {
            treeMapEntry2.parent = treeMapEntry.parent;
            if (treeMapEntry.parent == null) {
                this.root = treeMapEntry2;
            } else if (treeMapEntry == treeMapEntry.parent.left) {
                treeMapEntry.parent.left = treeMapEntry2;
            } else {
                treeMapEntry.parent.right = treeMapEntry2;
            }
            treeMapEntry.parent = null;
            treeMapEntry.right = null;
            treeMapEntry.left = null;
            if (treeMapEntry.color) {
                fixAfterDeletion(treeMapEntry2);
                return;
            }
            return;
        }
        if (treeMapEntry.parent == null) {
            this.root = null;
            return;
        }
        if (treeMapEntry.color) {
            fixAfterDeletion(treeMapEntry);
        }
        if (treeMapEntry.parent != null) {
            if (treeMapEntry == treeMapEntry.parent.left) {
                treeMapEntry.parent.left = null;
            } else if (treeMapEntry == treeMapEntry.parent.right) {
                treeMapEntry.parent.right = null;
            }
            treeMapEntry.parent = null;
        }
    }

    private void fixAfterDeletion(TreeMapEntry<K, V> treeMapEntry) {
        while (treeMapEntry != this.root && colorOf(treeMapEntry)) {
            if (treeMapEntry == leftOf(parentOf(treeMapEntry))) {
                TreeMapEntry<K, V> treeMapEntryRightOf = rightOf(parentOf(treeMapEntry));
                if (!colorOf(treeMapEntryRightOf)) {
                    setColor(treeMapEntryRightOf, BLACK);
                    setColor(parentOf(treeMapEntry), RED);
                    rotateLeft(parentOf(treeMapEntry));
                    treeMapEntryRightOf = rightOf(parentOf(treeMapEntry));
                }
                if (colorOf(leftOf(treeMapEntryRightOf)) && colorOf(rightOf(treeMapEntryRightOf))) {
                    setColor(treeMapEntryRightOf, RED);
                    treeMapEntry = parentOf(treeMapEntry);
                } else {
                    if (colorOf(rightOf(treeMapEntryRightOf))) {
                        setColor(leftOf(treeMapEntryRightOf), BLACK);
                        setColor(treeMapEntryRightOf, RED);
                        rotateRight(treeMapEntryRightOf);
                        treeMapEntryRightOf = rightOf(parentOf(treeMapEntry));
                    }
                    setColor(treeMapEntryRightOf, colorOf(parentOf(treeMapEntry)));
                    setColor(parentOf(treeMapEntry), BLACK);
                    setColor(rightOf(treeMapEntryRightOf), BLACK);
                    rotateLeft(parentOf(treeMapEntry));
                    treeMapEntry = this.root;
                }
            } else {
                TreeMapEntry<K, V> treeMapEntryLeftOf = leftOf(parentOf(treeMapEntry));
                if (!colorOf(treeMapEntryLeftOf)) {
                    setColor(treeMapEntryLeftOf, BLACK);
                    setColor(parentOf(treeMapEntry), RED);
                    rotateRight(parentOf(treeMapEntry));
                    treeMapEntryLeftOf = leftOf(parentOf(treeMapEntry));
                }
                if (colorOf(rightOf(treeMapEntryLeftOf)) && colorOf(leftOf(treeMapEntryLeftOf))) {
                    setColor(treeMapEntryLeftOf, RED);
                    treeMapEntry = parentOf(treeMapEntry);
                } else {
                    if (colorOf(leftOf(treeMapEntryLeftOf))) {
                        setColor(rightOf(treeMapEntryLeftOf), BLACK);
                        setColor(treeMapEntryLeftOf, RED);
                        rotateLeft(treeMapEntryLeftOf);
                        treeMapEntryLeftOf = leftOf(parentOf(treeMapEntry));
                    }
                    setColor(treeMapEntryLeftOf, colorOf(parentOf(treeMapEntry)));
                    setColor(parentOf(treeMapEntry), BLACK);
                    setColor(leftOf(treeMapEntryLeftOf), BLACK);
                    rotateRight(parentOf(treeMapEntry));
                    treeMapEntry = this.root;
                }
            }
        }
        setColor(treeMapEntry, BLACK);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.size);
        for (Map.Entry<K, V> entry : entrySet()) {
            objectOutputStream.writeObject(entry.getKey());
            objectOutputStream.writeObject(entry.getValue());
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        buildFromSorted(objectInputStream.readInt(), null, objectInputStream, null);
    }

    void readTreeSet(int i, ObjectInputStream objectInputStream, V v) throws IOException, ClassNotFoundException {
        buildFromSorted(i, null, objectInputStream, v);
    }

    void addAllForTreeSet(SortedSet<? extends K> sortedSet, V v) {
        try {
            buildFromSorted(sortedSet.size(), sortedSet.iterator(), null, v);
        } catch (IOException e) {
        } catch (ClassNotFoundException e2) {
        }
    }

    private void buildFromSorted(int i, Iterator<?> it, ObjectInputStream objectInputStream, V v) throws IOException, ClassNotFoundException {
        this.size = i;
        this.root = buildFromSorted(0, 0, i - 1, computeRedLevel(i), it, objectInputStream, v);
    }

    private final TreeMapEntry<K, V> buildFromSorted(int i, int i2, int i3, int i4, Iterator<?> it, ObjectInputStream objectInputStream, V v) throws IOException, ClassNotFoundException {
        TreeMapEntry<K, V> treeMapEntryBuildFromSorted;
        Object object;
        Object object2;
        if (i3 < i2) {
            return null;
        }
        int i5 = (i2 + i3) >>> 1;
        if (i2 < i5) {
            treeMapEntryBuildFromSorted = buildFromSorted(i + 1, i2, i5 - 1, i4, it, objectInputStream, v);
        } else {
            treeMapEntryBuildFromSorted = null;
        }
        if (it != null) {
            if (v == null) {
                Map.Entry entry = (Map.Entry) it.next();
                object = entry.getKey();
                object2 = entry.getValue();
            } else {
                object = it.next();
            }
        } else {
            object = objectInputStream.readObject();
            object2 = v != null ? v : objectInputStream.readObject();
        }
        TreeMapEntry<K, V> treeMapEntry = new TreeMapEntry<>(object, object2, null);
        if (i == i4) {
            treeMapEntry.color = RED;
        }
        if (treeMapEntryBuildFromSorted != null) {
            treeMapEntry.left = treeMapEntryBuildFromSorted;
            treeMapEntryBuildFromSorted.parent = treeMapEntry;
        }
        if (i5 < i3) {
            TreeMapEntry<K, V> treeMapEntryBuildFromSorted2 = buildFromSorted(i + 1, i5 + 1, i3, i4, it, objectInputStream, v);
            treeMapEntry.right = treeMapEntryBuildFromSorted2;
            treeMapEntryBuildFromSorted2.parent = treeMapEntry;
        }
        return treeMapEntry;
    }

    private static int computeRedLevel(int i) {
        int i2 = 0;
        for (int i3 = i - 1; i3 >= 0; i3 = (i3 / 2) - 1) {
            i2++;
        }
        return i2;
    }

    static <K> Spliterator<K> keySpliteratorFor(NavigableMap<K, ?> navigableMap) {
        if (navigableMap instanceof TreeMap) {
            return ((TreeMap) navigableMap).keySpliterator();
        }
        if (navigableMap instanceof DescendingSubMap) {
            DescendingSubMap descendingSubMap = (DescendingSubMap) navigableMap;
            TreeMap<K, V> treeMap = descendingSubMap.m;
            if (descendingSubMap == ((TreeMap) treeMap).descendingMap) {
                return treeMap.descendingKeySpliterator();
            }
        }
        return ((NavigableSubMap) navigableMap).keySpliterator();
    }

    final Spliterator<K> keySpliterator() {
        return new KeySpliterator(this, null, null, 0, -1, 0);
    }

    final Spliterator<K> descendingKeySpliterator() {
        return new DescendingKeySpliterator(this, null, null, 0, -2, 0);
    }

    static class TreeMapSpliterator<K, V> {
        TreeMapEntry<K, V> current;
        int est;
        int expectedModCount;
        TreeMapEntry<K, V> fence;
        int side;
        final TreeMap<K, V> tree;

        TreeMapSpliterator(TreeMap<K, V> treeMap, TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2, int i, int i2, int i3) {
            this.tree = treeMap;
            this.current = treeMapEntry;
            this.fence = treeMapEntry2;
            this.side = i;
            this.est = i2;
            this.expectedModCount = i3;
        }

        final int getEstimate() {
            int i = this.est;
            if (i < 0) {
                TreeMap<K, V> treeMap = this.tree;
                if (treeMap != null) {
                    this.current = i == -1 ? treeMap.getFirstEntry() : treeMap.getLastEntry();
                    int i2 = ((TreeMap) treeMap).size;
                    this.est = i2;
                    this.expectedModCount = ((TreeMap) treeMap).modCount;
                    return i2;
                }
                this.est = 0;
                return 0;
            }
            return i;
        }

        public final long estimateSize() {
            return getEstimate();
        }
    }

    static final class KeySpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(TreeMap<K, V> treeMap, TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2, int i, int i2, int i3) {
            super(treeMap, treeMapEntry, treeMapEntry2, i, i2, i3);
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            TreeMapEntry<K, V> treeMapEntry;
            TreeMapEntry<K, V> treeMapEntry2;
            if (this.est < 0) {
                getEstimate();
            }
            int i = this.side;
            TreeMapEntry<K, V> treeMapEntry3 = this.current;
            TreeMapEntry<K, V> treeMapEntry4 = this.fence;
            if (treeMapEntry3 != null && treeMapEntry3 != treeMapEntry4) {
                if (i == 0) {
                    treeMapEntry2 = ((TreeMap) this.tree).root;
                } else if (i > 0) {
                    treeMapEntry2 = treeMapEntry3.right;
                } else {
                    if (i < 0 && treeMapEntry4 != null) {
                        treeMapEntry2 = treeMapEntry4.left;
                    }
                    treeMapEntry = null;
                }
                treeMapEntry = treeMapEntry2;
            } else {
                treeMapEntry = null;
            }
            if (treeMapEntry == null || treeMapEntry == treeMapEntry3 || treeMapEntry == treeMapEntry4 || this.tree.compare(treeMapEntry3.key, treeMapEntry.key) >= 0) {
                return null;
            }
            this.side = 1;
            TreeMap<K, V> treeMap = this.tree;
            this.current = treeMapEntry;
            int i2 = this.est >>> 1;
            this.est = i2;
            return new KeySpliterator<>(treeMap, treeMapEntry3, treeMapEntry, -1, i2, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.fence;
            TreeMapEntry<K, V> treeMapEntry2 = this.current;
            if (treeMapEntry2 != null && treeMapEntry2 != treeMapEntry) {
                this.current = treeMapEntry;
                do {
                    consumer.accept(treeMapEntry2.key);
                    TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry2.right;
                    if (treeMapEntry3 != null) {
                        while (true) {
                            TreeMapEntry<K, V> treeMapEntry4 = treeMapEntry3.left;
                            if (treeMapEntry4 == null) {
                                break;
                            } else {
                                treeMapEntry3 = treeMapEntry4;
                            }
                        }
                    } else {
                        while (true) {
                            treeMapEntry3 = treeMapEntry2.parent;
                            if (treeMapEntry3 == null || treeMapEntry2 != treeMapEntry3.right) {
                                break;
                            } else {
                                treeMapEntry2 = treeMapEntry3;
                            }
                        }
                    }
                    treeMapEntry2 = treeMapEntry3;
                    if (treeMapEntry2 == null) {
                        break;
                    }
                } while (treeMapEntry2 != treeMapEntry);
                if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.current;
            if (treeMapEntry == null || treeMapEntry == this.fence) {
                return TreeMap.RED;
            }
            this.current = TreeMap.successor(treeMapEntry);
            consumer.accept(treeMapEntry.key);
            if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return TreeMap.BLACK;
        }

        @Override
        public int characteristics() {
            return (this.side == 0 ? 64 : 0) | 1 | 4 | 16;
        }

        @Override
        public final Comparator<? super K> getComparator() {
            return ((TreeMap) this.tree).comparator;
        }
    }

    static final class DescendingKeySpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<K> {
        DescendingKeySpliterator(TreeMap<K, V> treeMap, TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2, int i, int i2, int i3) {
            super(treeMap, treeMapEntry, treeMapEntry2, i, i2, i3);
        }

        @Override
        public DescendingKeySpliterator<K, V> trySplit() {
            TreeMapEntry<K, V> treeMapEntry;
            TreeMapEntry<K, V> treeMapEntry2;
            if (this.est < 0) {
                getEstimate();
            }
            int i = this.side;
            TreeMapEntry<K, V> treeMapEntry3 = this.current;
            TreeMapEntry<K, V> treeMapEntry4 = this.fence;
            if (treeMapEntry3 != null && treeMapEntry3 != treeMapEntry4) {
                if (i == 0) {
                    treeMapEntry2 = ((TreeMap) this.tree).root;
                } else if (i < 0) {
                    treeMapEntry2 = treeMapEntry3.left;
                } else {
                    if (i > 0 && treeMapEntry4 != null) {
                        treeMapEntry2 = treeMapEntry4.right;
                    }
                    treeMapEntry = null;
                }
                treeMapEntry = treeMapEntry2;
            } else {
                treeMapEntry = null;
            }
            if (treeMapEntry == null || treeMapEntry == treeMapEntry3 || treeMapEntry == treeMapEntry4 || this.tree.compare(treeMapEntry3.key, treeMapEntry.key) <= 0) {
                return null;
            }
            this.side = 1;
            TreeMap<K, V> treeMap = this.tree;
            this.current = treeMapEntry;
            int i2 = this.est >>> 1;
            this.est = i2;
            return new DescendingKeySpliterator<>(treeMap, treeMapEntry3, treeMapEntry, -1, i2, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.fence;
            TreeMapEntry<K, V> treeMapEntry2 = this.current;
            if (treeMapEntry2 != null && treeMapEntry2 != treeMapEntry) {
                this.current = treeMapEntry;
                do {
                    consumer.accept(treeMapEntry2.key);
                    TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry2.left;
                    if (treeMapEntry3 != null) {
                        while (true) {
                            TreeMapEntry<K, V> treeMapEntry4 = treeMapEntry3.right;
                            if (treeMapEntry4 == null) {
                                break;
                            } else {
                                treeMapEntry3 = treeMapEntry4;
                            }
                        }
                    } else {
                        while (true) {
                            treeMapEntry3 = treeMapEntry2.parent;
                            if (treeMapEntry3 == null || treeMapEntry2 != treeMapEntry3.left) {
                                break;
                            } else {
                                treeMapEntry2 = treeMapEntry3;
                            }
                        }
                    }
                    treeMapEntry2 = treeMapEntry3;
                    if (treeMapEntry2 == null) {
                        break;
                    }
                } while (treeMapEntry2 != treeMapEntry);
                if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.current;
            if (treeMapEntry == null || treeMapEntry == this.fence) {
                return TreeMap.RED;
            }
            this.current = TreeMap.predecessor(treeMapEntry);
            consumer.accept(treeMapEntry.key);
            if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return TreeMap.BLACK;
        }

        @Override
        public int characteristics() {
            return (this.side == 0 ? 64 : 0) | 1 | 16;
        }
    }

    static final class ValueSpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(TreeMap<K, V> treeMap, TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2, int i, int i2, int i3) {
            super(treeMap, treeMapEntry, treeMapEntry2, i, i2, i3);
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            TreeMapEntry<K, V> treeMapEntry;
            TreeMapEntry<K, V> treeMapEntry2;
            if (this.est < 0) {
                getEstimate();
            }
            int i = this.side;
            TreeMapEntry<K, V> treeMapEntry3 = this.current;
            TreeMapEntry<K, V> treeMapEntry4 = this.fence;
            if (treeMapEntry3 != null && treeMapEntry3 != treeMapEntry4) {
                if (i == 0) {
                    treeMapEntry2 = ((TreeMap) this.tree).root;
                } else if (i > 0) {
                    treeMapEntry2 = treeMapEntry3.right;
                } else {
                    if (i < 0 && treeMapEntry4 != null) {
                        treeMapEntry2 = treeMapEntry4.left;
                    }
                    treeMapEntry = null;
                }
                treeMapEntry = treeMapEntry2;
            } else {
                treeMapEntry = null;
            }
            if (treeMapEntry == null || treeMapEntry == treeMapEntry3 || treeMapEntry == treeMapEntry4 || this.tree.compare(treeMapEntry3.key, treeMapEntry.key) >= 0) {
                return null;
            }
            this.side = 1;
            TreeMap<K, V> treeMap = this.tree;
            this.current = treeMapEntry;
            int i2 = this.est >>> 1;
            this.est = i2;
            return new ValueSpliterator<>(treeMap, treeMapEntry3, treeMapEntry, -1, i2, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.fence;
            TreeMapEntry<K, V> treeMapEntry2 = this.current;
            if (treeMapEntry2 != null && treeMapEntry2 != treeMapEntry) {
                this.current = treeMapEntry;
                do {
                    consumer.accept(treeMapEntry2.value);
                    TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry2.right;
                    if (treeMapEntry3 != null) {
                        while (true) {
                            TreeMapEntry<K, V> treeMapEntry4 = treeMapEntry3.left;
                            if (treeMapEntry4 == null) {
                                break;
                            } else {
                                treeMapEntry3 = treeMapEntry4;
                            }
                        }
                    } else {
                        while (true) {
                            treeMapEntry3 = treeMapEntry2.parent;
                            if (treeMapEntry3 == null || treeMapEntry2 != treeMapEntry3.right) {
                                break;
                            } else {
                                treeMapEntry2 = treeMapEntry3;
                            }
                        }
                    }
                    treeMapEntry2 = treeMapEntry3;
                    if (treeMapEntry2 == null) {
                        break;
                    }
                } while (treeMapEntry2 != treeMapEntry);
                if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.current;
            if (treeMapEntry == null || treeMapEntry == this.fence) {
                return TreeMap.RED;
            }
            this.current = TreeMap.successor(treeMapEntry);
            consumer.accept(treeMapEntry.value);
            if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return TreeMap.BLACK;
        }

        @Override
        public int characteristics() {
            return (this.side == 0 ? 64 : 0) | 16;
        }
    }

    static final class EntrySpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(TreeMap<K, V> treeMap, TreeMapEntry<K, V> treeMapEntry, TreeMapEntry<K, V> treeMapEntry2, int i, int i2, int i3) {
            super(treeMap, treeMapEntry, treeMapEntry2, i, i2, i3);
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            TreeMapEntry<K, V> treeMapEntry;
            TreeMapEntry<K, V> treeMapEntry2;
            if (this.est < 0) {
                getEstimate();
            }
            int i = this.side;
            TreeMapEntry<K, V> treeMapEntry3 = this.current;
            TreeMapEntry<K, V> treeMapEntry4 = this.fence;
            if (treeMapEntry3 != null && treeMapEntry3 != treeMapEntry4) {
                if (i == 0) {
                    treeMapEntry2 = ((TreeMap) this.tree).root;
                } else if (i > 0) {
                    treeMapEntry2 = treeMapEntry3.right;
                } else {
                    if (i < 0 && treeMapEntry4 != null) {
                        treeMapEntry2 = treeMapEntry4.left;
                    }
                    treeMapEntry = null;
                }
                treeMapEntry = treeMapEntry2;
            } else {
                treeMapEntry = null;
            }
            if (treeMapEntry == null || treeMapEntry == treeMapEntry3 || treeMapEntry == treeMapEntry4 || this.tree.compare(treeMapEntry3.key, treeMapEntry.key) >= 0) {
                return null;
            }
            this.side = 1;
            TreeMap<K, V> treeMap = this.tree;
            this.current = treeMapEntry;
            int i2 = this.est >>> 1;
            this.est = i2;
            return new EntrySpliterator<>(treeMap, treeMapEntry3, treeMapEntry, -1, i2, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.fence;
            TreeMapEntry<K, V> treeMapEntry2 = this.current;
            if (treeMapEntry2 != null && treeMapEntry2 != treeMapEntry) {
                this.current = treeMapEntry;
                do {
                    consumer.accept(treeMapEntry2);
                    TreeMapEntry<K, V> treeMapEntry3 = treeMapEntry2.right;
                    if (treeMapEntry3 != null) {
                        while (true) {
                            TreeMapEntry<K, V> treeMapEntry4 = treeMapEntry3.left;
                            if (treeMapEntry4 == null) {
                                break;
                            } else {
                                treeMapEntry3 = treeMapEntry4;
                            }
                        }
                    } else {
                        while (true) {
                            treeMapEntry3 = treeMapEntry2.parent;
                            if (treeMapEntry3 == null || treeMapEntry2 != treeMapEntry3.right) {
                                break;
                            } else {
                                treeMapEntry2 = treeMapEntry3;
                            }
                        }
                    }
                    treeMapEntry2 = treeMapEntry3;
                    if (treeMapEntry2 == null) {
                        break;
                    }
                } while (treeMapEntry2 != treeMapEntry);
                if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.est < 0) {
                getEstimate();
            }
            TreeMapEntry<K, V> treeMapEntry = this.current;
            if (treeMapEntry == null || treeMapEntry == this.fence) {
                return TreeMap.RED;
            }
            this.current = TreeMap.successor(treeMapEntry);
            consumer.accept(treeMapEntry);
            if (((TreeMap) this.tree).modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return TreeMap.BLACK;
        }

        @Override
        public int characteristics() {
            return (this.side == 0 ? 64 : 0) | 1 | 4 | 16;
        }

        @Override
        public Comparator<Map.Entry<K, V>> getComparator() {
            if (((TreeMap) this.tree).comparator != null) {
                return Map.Entry.comparingByKey(((TreeMap) this.tree).comparator);
            }
            return $$Lambda$TreeMap$EntrySpliterator$YqCulUmBGNzQr1PJ_ERFnbxUmTQ.INSTANCE;
        }
    }
}
