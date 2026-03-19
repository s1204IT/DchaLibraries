package java.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class LinkedHashMap<K, V> extends HashMap<K, V> implements Map<K, V> {
    private static final long serialVersionUID = 3801124242820219131L;
    final boolean accessOrder;
    transient LinkedHashMapEntry<K, V> head;
    transient LinkedHashMapEntry<K, V> tail;

    static class LinkedHashMapEntry<K, V> extends HashMap.Node<K, V> {
        LinkedHashMapEntry<K, V> after;
        LinkedHashMapEntry<K, V> before;

        LinkedHashMapEntry(int i, K k, V v, HashMap.Node<K, V> node) {
            super(i, k, v, node);
        }
    }

    private void linkNodeLast(LinkedHashMapEntry<K, V> linkedHashMapEntry) {
        LinkedHashMapEntry<K, V> linkedHashMapEntry2 = this.tail;
        this.tail = linkedHashMapEntry;
        if (linkedHashMapEntry2 == null) {
            this.head = linkedHashMapEntry;
        } else {
            linkedHashMapEntry.before = linkedHashMapEntry2;
            linkedHashMapEntry2.after = linkedHashMapEntry;
        }
    }

    private void transferLinks(LinkedHashMapEntry<K, V> linkedHashMapEntry, LinkedHashMapEntry<K, V> linkedHashMapEntry2) {
        LinkedHashMapEntry<K, V> linkedHashMapEntry3 = linkedHashMapEntry.before;
        linkedHashMapEntry2.before = linkedHashMapEntry3;
        LinkedHashMapEntry<K, V> linkedHashMapEntry4 = linkedHashMapEntry.after;
        linkedHashMapEntry2.after = linkedHashMapEntry4;
        if (linkedHashMapEntry3 == null) {
            this.head = linkedHashMapEntry2;
        } else {
            linkedHashMapEntry3.after = linkedHashMapEntry2;
        }
        if (linkedHashMapEntry4 == null) {
            this.tail = linkedHashMapEntry2;
        } else {
            linkedHashMapEntry4.before = linkedHashMapEntry2;
        }
    }

    @Override
    void reinitialize() {
        super.reinitialize();
        this.tail = null;
        this.head = null;
    }

    @Override
    HashMap.Node<K, V> newNode(int i, K k, V v, HashMap.Node<K, V> node) {
        LinkedHashMapEntry<K, V> linkedHashMapEntry = new LinkedHashMapEntry<>(i, k, v, node);
        linkNodeLast(linkedHashMapEntry);
        return linkedHashMapEntry;
    }

    @Override
    HashMap.Node<K, V> replacementNode(HashMap.Node<K, V> node, HashMap.Node<K, V> node2) {
        LinkedHashMapEntry<K, V> linkedHashMapEntry = (LinkedHashMapEntry) node;
        LinkedHashMapEntry<K, V> linkedHashMapEntry2 = new LinkedHashMapEntry<>(linkedHashMapEntry.hash, linkedHashMapEntry.key, linkedHashMapEntry.value, node2);
        transferLinks(linkedHashMapEntry, linkedHashMapEntry2);
        return linkedHashMapEntry2;
    }

    @Override
    HashMap.TreeNode<K, V> newTreeNode(int i, K k, V v, HashMap.Node<K, V> node) {
        HashMap.TreeNode<K, V> treeNode = new HashMap.TreeNode<>(i, k, v, node);
        linkNodeLast(treeNode);
        return treeNode;
    }

    @Override
    HashMap.TreeNode<K, V> replacementTreeNode(HashMap.Node<K, V> node, HashMap.Node<K, V> node2) {
        LinkedHashMapEntry<K, V> linkedHashMapEntry = (LinkedHashMapEntry) node;
        HashMap.TreeNode<K, V> treeNode = new HashMap.TreeNode<>(linkedHashMapEntry.hash, linkedHashMapEntry.key, linkedHashMapEntry.value, node2);
        transferLinks(linkedHashMapEntry, treeNode);
        return treeNode;
    }

    @Override
    void afterNodeRemoval(HashMap.Node<K, V> node) {
        LinkedHashMapEntry linkedHashMapEntry = (LinkedHashMapEntry) node;
        LinkedHashMapEntry<K, V> linkedHashMapEntry2 = linkedHashMapEntry.before;
        LinkedHashMapEntry<K, V> linkedHashMapEntry3 = linkedHashMapEntry.after;
        linkedHashMapEntry.after = null;
        linkedHashMapEntry.before = null;
        if (linkedHashMapEntry2 == null) {
            this.head = linkedHashMapEntry3;
        } else {
            linkedHashMapEntry2.after = linkedHashMapEntry3;
        }
        if (linkedHashMapEntry3 == null) {
            this.tail = linkedHashMapEntry2;
        } else {
            linkedHashMapEntry3.before = linkedHashMapEntry2;
        }
    }

    @Override
    void afterNodeInsertion(boolean z) {
        LinkedHashMapEntry<K, V> linkedHashMapEntry;
        if (z && (linkedHashMapEntry = this.head) != null && removeEldestEntry(linkedHashMapEntry)) {
            K k = linkedHashMapEntry.key;
            removeNode(hash(k), k, null, false, true);
        }
    }

    @Override
    void afterNodeAccess(HashMap.Node<K, V> node) {
        LinkedHashMapEntry<K, V> linkedHashMapEntry;
        if (this.accessOrder && (linkedHashMapEntry = this.tail) != node) {
            LinkedHashMapEntry<K, V> linkedHashMapEntry2 = (LinkedHashMapEntry) node;
            LinkedHashMapEntry<K, V> linkedHashMapEntry3 = linkedHashMapEntry2.before;
            LinkedHashMapEntry<K, V> linkedHashMapEntry4 = linkedHashMapEntry2.after;
            linkedHashMapEntry2.after = null;
            if (linkedHashMapEntry3 == null) {
                this.head = linkedHashMapEntry4;
            } else {
                linkedHashMapEntry3.after = linkedHashMapEntry4;
            }
            if (linkedHashMapEntry4 != null) {
                linkedHashMapEntry4.before = linkedHashMapEntry3;
            } else {
                linkedHashMapEntry = linkedHashMapEntry3;
            }
            if (linkedHashMapEntry == null) {
                this.head = linkedHashMapEntry2;
            } else {
                linkedHashMapEntry2.before = linkedHashMapEntry;
                linkedHashMapEntry.after = linkedHashMapEntry2;
            }
            this.tail = linkedHashMapEntry2;
            this.modCount++;
        }
    }

    @Override
    void internalWriteEntries(ObjectOutputStream objectOutputStream) throws IOException {
        for (LinkedHashMapEntry<K, V> linkedHashMapEntry = this.head; linkedHashMapEntry != null; linkedHashMapEntry = linkedHashMapEntry.after) {
            objectOutputStream.writeObject(linkedHashMapEntry.key);
            objectOutputStream.writeObject(linkedHashMapEntry.value);
        }
    }

    public LinkedHashMap(int i, float f) {
        super(i, f);
        this.accessOrder = false;
    }

    public LinkedHashMap(int i) {
        super(i);
        this.accessOrder = false;
    }

    public LinkedHashMap() {
        this.accessOrder = false;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> map) {
        this.accessOrder = false;
        putMapEntries(map, false);
    }

    public LinkedHashMap(int i, float f, boolean z) {
        super(i, f);
        this.accessOrder = z;
    }

    @Override
    public boolean containsValue(Object obj) {
        for (LinkedHashMapEntry<K, V> linkedHashMapEntry = this.head; linkedHashMapEntry != null; linkedHashMapEntry = linkedHashMapEntry.after) {
            V v = linkedHashMapEntry.value;
            if (v == obj) {
                return true;
            }
            if (obj != null && obj.equals(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object obj) {
        HashMap.Node<K, V> node = getNode(hash(obj), obj);
        if (node == null) {
            return null;
        }
        if (this.accessOrder) {
            afterNodeAccess(node);
        }
        return node.value;
    }

    @Override
    public V getOrDefault(Object obj, V v) {
        HashMap.Node<K, V> node = getNode(hash(obj), obj);
        if (node == null) {
            return v;
        }
        if (this.accessOrder) {
            afterNodeAccess(node);
        }
        return node.value;
    }

    @Override
    public void clear() {
        super.clear();
        this.tail = null;
        this.head = null;
    }

    public Map.Entry<K, V> eldest() {
        return this.head;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return false;
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set == null) {
            LinkedKeySet linkedKeySet = new LinkedKeySet();
            this.keySet = linkedKeySet;
            return linkedKeySet;
        }
        return set;
    }

    final class LinkedKeySet extends AbstractSet<K> {
        LinkedKeySet() {
        }

        @Override
        public final int size() {
            return LinkedHashMap.this.size;
        }

        @Override
        public final void clear() {
            LinkedHashMap.this.clear();
        }

        @Override
        public final Iterator<K> iterator() {
            return new LinkedKeyIterator();
        }

        @Override
        public final boolean contains(Object obj) {
            return LinkedHashMap.this.containsKey(obj);
        }

        @Override
        public final boolean remove(Object obj) {
            return LinkedHashMap.this.removeNode(HashMap.hash(obj), obj, null, false, true) != null;
        }

        @Override
        public final Spliterator<K> spliterator() {
            return Spliterators.spliterator(this, 81);
        }

        @Override
        public final void forEach(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int i = LinkedHashMap.this.modCount;
            for (LinkedHashMapEntry<K, V> linkedHashMapEntry = LinkedHashMap.this.head; linkedHashMapEntry != null && LinkedHashMap.this.modCount == i; linkedHashMapEntry = linkedHashMapEntry.after) {
                consumer.accept(linkedHashMapEntry.key);
            }
            if (LinkedHashMap.this.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = this.values;
        if (collection == null) {
            LinkedValues linkedValues = new LinkedValues();
            this.values = linkedValues;
            return linkedValues;
        }
        return collection;
    }

    final class LinkedValues extends AbstractCollection<V> {
        LinkedValues() {
        }

        @Override
        public final int size() {
            return LinkedHashMap.this.size;
        }

        @Override
        public final void clear() {
            LinkedHashMap.this.clear();
        }

        @Override
        public final Iterator<V> iterator() {
            return new LinkedValueIterator();
        }

        @Override
        public final boolean contains(Object obj) {
            return LinkedHashMap.this.containsValue(obj);
        }

        @Override
        public final Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, 80);
        }

        @Override
        public final void forEach(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int i = LinkedHashMap.this.modCount;
            for (LinkedHashMapEntry<K, V> linkedHashMapEntry = LinkedHashMap.this.head; linkedHashMapEntry != null && LinkedHashMap.this.modCount == i; linkedHashMapEntry = linkedHashMapEntry.after) {
                consumer.accept(linkedHashMapEntry.value);
            }
            if (LinkedHashMap.this.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        LinkedEntrySet linkedEntrySet = new LinkedEntrySet();
        this.entrySet = linkedEntrySet;
        return linkedEntrySet;
    }

    final class LinkedEntrySet extends AbstractSet<Map.Entry<K, V>> {
        LinkedEntrySet() {
        }

        @Override
        public final int size() {
            return LinkedHashMap.this.size;
        }

        @Override
        public final void clear() {
            LinkedHashMap.this.clear();
        }

        @Override
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new LinkedEntryIterator();
        }

        @Override
        public final boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            HashMap.Node<K, V> node = LinkedHashMap.this.getNode(HashMap.hash(key), key);
            return node != null && node.equals(entry);
        }

        @Override
        public final boolean remove(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            return LinkedHashMap.this.removeNode(HashMap.hash(key), key, entry.getValue(), true, true) != null;
        }

        @Override
        public final Spliterator<Map.Entry<K, V>> spliterator() {
            return Spliterators.spliterator(this, 81);
        }

        @Override
        public final void forEach(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int i = LinkedHashMap.this.modCount;
            for (LinkedHashMapEntry<K, V> linkedHashMapEntry = LinkedHashMap.this.head; linkedHashMapEntry != null && i == LinkedHashMap.this.modCount; linkedHashMapEntry = linkedHashMapEntry.after) {
                consumer.accept(linkedHashMapEntry);
            }
            if (LinkedHashMap.this.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        if (biConsumer == null) {
            throw new NullPointerException();
        }
        int i = this.modCount;
        for (LinkedHashMapEntry<K, V> linkedHashMapEntry = this.head; this.modCount == i && linkedHashMapEntry != null; linkedHashMapEntry = linkedHashMapEntry.after) {
            biConsumer.accept(linkedHashMapEntry.key, linkedHashMapEntry.value);
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        int i = this.modCount;
        for (LinkedHashMapEntry<K, V> linkedHashMapEntry = this.head; this.modCount == i && linkedHashMapEntry != null; linkedHashMapEntry = linkedHashMapEntry.after) {
            linkedHashMapEntry.value = biFunction.apply(linkedHashMapEntry.key, linkedHashMapEntry.value);
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
    }

    abstract class LinkedHashIterator {
        LinkedHashMapEntry<K, V> current = null;
        int expectedModCount;
        LinkedHashMapEntry<K, V> next;

        LinkedHashIterator() {
            this.next = LinkedHashMap.this.head;
            this.expectedModCount = LinkedHashMap.this.modCount;
        }

        public final boolean hasNext() {
            return this.next != null;
        }

        final LinkedHashMapEntry<K, V> nextNode() {
            LinkedHashMapEntry<K, V> linkedHashMapEntry = this.next;
            if (LinkedHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (linkedHashMapEntry == null) {
                throw new NoSuchElementException();
            }
            this.current = linkedHashMapEntry;
            this.next = linkedHashMapEntry.after;
            return linkedHashMapEntry;
        }

        public final void remove() {
            LinkedHashMapEntry<K, V> linkedHashMapEntry = this.current;
            if (linkedHashMapEntry == null) {
                throw new IllegalStateException();
            }
            if (LinkedHashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            this.current = null;
            K k = linkedHashMapEntry.key;
            LinkedHashMap.this.removeNode(HashMap.hash(k), k, null, false, false);
            this.expectedModCount = LinkedHashMap.this.modCount;
        }
    }

    final class LinkedKeyIterator extends LinkedHashMap<K, V>.LinkedHashIterator implements Iterator<K> {
        LinkedKeyIterator() {
            super();
        }

        @Override
        public final K next() {
            return nextNode().getKey();
        }
    }

    final class LinkedValueIterator extends LinkedHashMap<K, V>.LinkedHashIterator implements Iterator<V> {
        LinkedValueIterator() {
            super();
        }

        @Override
        public final V next() {
            return nextNode().value;
        }
    }

    final class LinkedEntryIterator extends LinkedHashMap<K, V>.LinkedHashIterator implements Iterator<Map.Entry<K, V>> {
        LinkedEntryIterator() {
            super();
        }

        @Override
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }
}
