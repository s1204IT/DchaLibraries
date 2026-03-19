package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final int MAXIMUM_CAPACITY = 1073741824;
    static final int MIN_TREEIFY_CAPACITY = 64;
    static final int TREEIFY_THRESHOLD = 8;
    static final int UNTREEIFY_THRESHOLD = 6;
    private static final long serialVersionUID = 362498820763181265L;
    transient Set<Map.Entry<K, V>> entrySet;
    final float loadFactor;
    transient int modCount;
    transient int size;
    transient Node<K, V>[] table;
    int threshold;

    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        Node<K, V> next;
        V value;

        Node(int i, K k, V v, Node<K, V> node) {
            this.hash = i;
            this.key = k;
            this.value = v;
            this.next = node;
        }

        @Override
        public final K getKey() {
            return this.key;
        }

        @Override
        public final V getValue() {
            return this.value;
        }

        public final String toString() {
            return ((Object) this.key) + "=" + ((Object) this.value);
        }

        @Override
        public final int hashCode() {
            return Objects.hashCode(this.key) ^ Objects.hashCode(this.value);
        }

        @Override
        public final V setValue(V v) {
            V v2 = this.value;
            this.value = v;
            return v2;
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) obj;
                if (Objects.equals(this.key, entry.getKey()) && Objects.equals(this.value, entry.getValue())) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    static final int hash(Object obj) {
        if (obj == null) {
            return 0;
        }
        int iHashCode = obj.hashCode();
        return iHashCode ^ (iHashCode >>> 16);
    }

    static Class<?> comparableClassFor(Object obj) {
        Type[] actualTypeArguments;
        if (obj instanceof Comparable) {
            Class<?> cls = obj.getClass();
            if (cls == String.class) {
                return cls;
            }
            Type[] genericInterfaces = cls.getGenericInterfaces();
            if (genericInterfaces != null) {
                for (Type type : genericInterfaces) {
                    if (type instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        if (parameterizedType.getRawType() == Comparable.class && (actualTypeArguments = parameterizedType.getActualTypeArguments()) != null && actualTypeArguments.length == 1 && actualTypeArguments[0] == cls) {
                            return cls;
                        }
                    }
                }
                return null;
            }
            return null;
        }
        return null;
    }

    static int compareComparables(Class<?> cls, Object obj, Object obj2) {
        if (obj2 == null || obj2.getClass() != cls) {
            return 0;
        }
        return ((Comparable) obj).compareTo(obj2);
    }

    static final int tableSizeFor(int i) {
        int i2 = i - 1;
        int i3 = i2 | (i2 >>> 1);
        int i4 = i3 | (i3 >>> 2);
        int i5 = i4 | (i4 >>> 4);
        int i6 = i5 | (i5 >>> 8);
        int i7 = i6 | (i6 >>> 16);
        if (i7 < 0) {
            return 1;
        }
        if (i7 < MAXIMUM_CAPACITY) {
            return 1 + i7;
        }
        return MAXIMUM_CAPACITY;
    }

    public HashMap(int i, float f) {
        if (i < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + i);
        }
        i = i > MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : i;
        if (f <= 0.0f || Float.isNaN(f)) {
            throw new IllegalArgumentException("Illegal load factor: " + f);
        }
        this.loadFactor = f;
        this.threshold = tableSizeFor(i);
    }

    public HashMap(int i) {
        this(i, DEFAULT_LOAD_FACTOR);
    }

    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    public HashMap(Map<? extends K, ? extends V> map) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(map, false);
    }

    final void putMapEntries(Map<? extends K, ? extends V> map, boolean z) {
        int size = map.size();
        if (size > 0) {
            if (this.table == null) {
                float f = (size / this.loadFactor) + 1.0f;
                int i = f < 1.0737418E9f ? (int) f : MAXIMUM_CAPACITY;
                if (i > this.threshold) {
                    this.threshold = tableSizeFor(i);
                }
            } else if (size > this.threshold) {
                resize();
            }
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                K key = entry.getKey();
                putVal(hash(key), key, entry.getValue(), false, z);
            }
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
    public V get(Object obj) {
        Node<K, V> node = getNode(hash(obj), obj);
        if (node == null) {
            return null;
        }
        return node.value;
    }

    final Node<K, V> getNode(int i, Object obj) {
        int length;
        Node<K, V> node;
        K k;
        K k2;
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null && (length = nodeArr.length) > 0 && (node = nodeArr[(length - 1) & i]) != null) {
            if (node.hash == i && ((k2 = node.key) == obj || (obj != null && obj.equals(k2)))) {
                return node;
            }
            Node<K, V> node2 = node.next;
            if (node2 != null) {
                if (node instanceof TreeNode) {
                    return ((TreeNode) node).getTreeNode(i, obj);
                }
                do {
                    if (node2.hash == i && ((k = node2.key) == obj || (obj != null && obj.equals(k)))) {
                        return node2;
                    }
                    node2 = node2.next;
                } while (node2 != null);
                return null;
            }
            return null;
        }
        return null;
    }

    @Override
    public boolean containsKey(Object obj) {
        return getNode(hash(obj), obj) != null;
    }

    @Override
    public V put(K k, V v) {
        return putVal(hash(k), k, v, false, true);
    }

    final V putVal(int i, K k, V v, boolean z, boolean z2) {
        int length;
        Node<K, V> node;
        K k2;
        K k3;
        Node<K, V>[] nodeArrResize = this.table;
        if (nodeArrResize == null || (length = nodeArrResize.length) == 0) {
            nodeArrResize = resize();
            length = nodeArrResize.length;
        }
        Node<K, V>[] nodeArr = nodeArrResize;
        int i2 = (length - 1) & i;
        Node<K, V> nodePutTreeVal = nodeArr[i2];
        if (nodePutTreeVal == null) {
            nodeArr[i2] = newNode(i, k, v, null);
        } else {
            if (nodePutTreeVal.hash != i || ((k3 = nodePutTreeVal.key) != k && (k == null || !k.equals(k3)))) {
                if (nodePutTreeVal instanceof TreeNode) {
                    nodePutTreeVal = ((TreeNode) nodePutTreeVal).putTreeVal(this, nodeArr, i, k, v);
                } else {
                    int i3 = 0;
                    while (true) {
                        node = nodePutTreeVal.next;
                        if (node == null) {
                            nodePutTreeVal.next = newNode(i, k, v, null);
                            if (i3 >= 7) {
                                treeifyBin(nodeArr, i);
                            }
                        } else {
                            if (node.hash == i && ((k2 = node.key) == k || (k != null && k.equals(k2)))) {
                                break;
                            }
                            i3++;
                            nodePutTreeVal = node;
                        }
                    }
                    nodePutTreeVal = node;
                }
            }
            if (nodePutTreeVal != null) {
                V v2 = nodePutTreeVal.value;
                if (!z || v2 == null) {
                    nodePutTreeVal.value = v;
                }
                afterNodeAccess(nodePutTreeVal);
                return v2;
            }
        }
        this.modCount++;
        int i4 = this.size + 1;
        this.size = i4;
        if (i4 > this.threshold) {
            resize();
        }
        afterNodeInsertion(z2);
        return null;
    }

    final Node<K, V>[] resize() {
        int length;
        int i;
        int i2;
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null) {
            length = nodeArr.length;
        } else {
            length = 0;
        }
        int i3 = this.threshold;
        int i4 = Integer.MAX_VALUE;
        if (length > 0) {
            if (length >= MAXIMUM_CAPACITY) {
                this.threshold = Integer.MAX_VALUE;
                return nodeArr;
            }
            i2 = length << 1;
            if (i2 < MAXIMUM_CAPACITY && length >= 16) {
                i = i3 << 1;
            } else {
                i = 0;
            }
        } else if (i3 <= 0) {
            i = 12;
            i2 = 16;
        } else {
            i2 = i3;
            i = 0;
        }
        if (i == 0) {
            float f = i2 * this.loadFactor;
            if (i2 < MAXIMUM_CAPACITY && f < 1.0737418E9f) {
                i4 = (int) f;
            }
            i = i4;
        }
        this.threshold = i;
        Node<K, V>[] nodeArr2 = new Node[i2];
        this.table = nodeArr2;
        if (nodeArr != null) {
            for (int i5 = 0; i5 < length; i5++) {
                Node<K, V> node = nodeArr[i5];
                if (node != null) {
                    nodeArr[i5] = null;
                    if (node.next == null) {
                        nodeArr2[node.hash & (i2 - 1)] = node;
                    } else if (node instanceof TreeNode) {
                        ((TreeNode) node).split(this, nodeArr2, i5, length);
                    } else {
                        Node<K, V> node2 = null;
                        Node<K, V> node3 = null;
                        Node<K, V> node4 = null;
                        Node<K, V> node5 = null;
                        while (true) {
                            Node<K, V> node6 = node.next;
                            if ((node.hash & length) == 0) {
                                if (node2 != null) {
                                    node2.next = node;
                                } else {
                                    node4 = node;
                                }
                                node2 = node;
                            } else {
                                if (node3 != null) {
                                    node3.next = node;
                                } else {
                                    node5 = node;
                                }
                                node3 = node;
                            }
                            if (node6 == null) {
                                break;
                            }
                            node = node6;
                        }
                        if (node2 != null) {
                            node2.next = null;
                            nodeArr2[i5] = node4;
                        }
                        if (node3 != null) {
                            node3.next = null;
                            nodeArr2[i5 + length] = node5;
                        }
                    }
                }
            }
        }
        return nodeArr2;
    }

    final void treeifyBin(Node<K, V>[] nodeArr, int i) {
        int length;
        if (nodeArr == null || (length = nodeArr.length) < 64) {
            resize();
            return;
        }
        int i2 = i & (length - 1);
        Node<K, V> node = nodeArr[i2];
        if (node != null) {
            TreeNode<K, V> treeNode = null;
            TreeNode<K, V> treeNode2 = null;
            while (true) {
                TreeNode<K, V> treeNodeReplacementTreeNode = replacementTreeNode(node, null);
                if (treeNode != null) {
                    treeNodeReplacementTreeNode.prev = treeNode;
                    treeNode.next = treeNodeReplacementTreeNode;
                } else {
                    treeNode2 = treeNodeReplacementTreeNode;
                }
                node = node.next;
                if (node == null) {
                    break;
                } else {
                    treeNode = treeNodeReplacementTreeNode;
                }
            }
            nodeArr[i2] = treeNode2;
            if (treeNode2 != null) {
                treeNode2.treeify(nodeArr);
            }
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        putMapEntries(map, true);
    }

    @Override
    public V remove(Object obj) {
        Node<K, V> nodeRemoveNode = removeNode(hash(obj), obj, null, false, true);
        if (nodeRemoveNode == null) {
            return null;
        }
        return nodeRemoveNode.value;
    }

    final Node<K, V> removeNode(int i, Object obj, Object obj2, boolean z, boolean z2) {
        int length;
        int i2;
        Node<K, V> node;
        Node<K, V> treeNode;
        K k;
        V v;
        K k2;
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null && (length = nodeArr.length) > 0 && (node = nodeArr[(i2 = (length - 1) & i)]) != null) {
            if (node.hash != i || ((k2 = node.key) != obj && (obj == null || !obj.equals(k2)))) {
                Node<K, V> node2 = node.next;
                if (node2 == null) {
                    treeNode = null;
                } else if (node instanceof TreeNode) {
                    treeNode = ((TreeNode) node).getTreeNode(i, obj);
                } else {
                    while (true) {
                        if (node2.hash == i && ((k = node2.key) == obj || (obj != null && obj.equals(k)))) {
                            break;
                        }
                        Node<K, V> node3 = node2.next;
                        if (node3 == null) {
                            treeNode = null;
                            node = node2;
                            break;
                        }
                        Node<K, V> node4 = node2;
                        node2 = node3;
                        node = node4;
                    }
                    treeNode = node2;
                }
            } else {
                treeNode = node;
            }
            if (treeNode != null && (!z || (v = treeNode.value) == obj2 || (obj2 != null && obj2.equals(v)))) {
                if (treeNode instanceof TreeNode) {
                    ((TreeNode) treeNode).removeTreeNode(this, nodeArr, z2);
                } else if (treeNode == node) {
                    nodeArr[i2] = treeNode.next;
                } else {
                    node.next = treeNode.next;
                }
                this.modCount++;
                this.size--;
                afterNodeRemoval(treeNode);
                return treeNode;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        this.modCount++;
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null && this.size > 0) {
            this.size = 0;
            for (int i = 0; i < nodeArr.length; i++) {
                nodeArr[i] = null;
            }
        }
    }

    @Override
    public boolean containsValue(Object obj) {
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null && this.size > 0) {
            for (Node<K, V> node : nodeArr) {
                for (; node != null; node = node.next) {
                    V v = node.value;
                    if (v != obj) {
                        if (obj != null && obj.equals(v)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = this.keySet;
        if (set == null) {
            KeySet keySet = new KeySet();
            this.keySet = keySet;
            return keySet;
        }
        return set;
    }

    final class KeySet extends AbstractSet<K> {
        KeySet() {
        }

        @Override
        public final int size() {
            return HashMap.this.size;
        }

        @Override
        public final void clear() {
            HashMap.this.clear();
        }

        @Override
        public final Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public final boolean contains(Object obj) {
            return HashMap.this.containsKey(obj);
        }

        @Override
        public final boolean remove(Object obj) {
            return HashMap.this.removeNode(HashMap.hash(obj), obj, null, false, true) != null;
        }

        @Override
        public final Spliterator<K> spliterator() {
            return new KeySpliterator(HashMap.this, 0, -1, 0, 0);
        }

        @Override
        public final void forEach(Consumer<? super K> consumer) {
            Node<K, V>[] nodeArr;
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (HashMap.this.size > 0 && (nodeArr = HashMap.this.table) != null) {
                int i = HashMap.this.modCount;
                for (int i2 = 0; i2 < nodeArr.length && HashMap.this.modCount == i; i2++) {
                    for (Node<K, V> node = nodeArr[i2]; node != null; node = node.next) {
                        consumer.accept(node.key);
                    }
                }
                if (HashMap.this.modCount != i) {
                    throw new ConcurrentModificationException();
                }
            }
        }
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

    final class Values extends AbstractCollection<V> {
        Values() {
        }

        @Override
        public final int size() {
            return HashMap.this.size;
        }

        @Override
        public final void clear() {
            HashMap.this.clear();
        }

        @Override
        public final Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public final boolean contains(Object obj) {
            return HashMap.this.containsValue(obj);
        }

        @Override
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator(HashMap.this, 0, -1, 0, 0);
        }

        @Override
        public final void forEach(Consumer<? super V> consumer) {
            Node<K, V>[] nodeArr;
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (HashMap.this.size > 0 && (nodeArr = HashMap.this.table) != null) {
                int i = HashMap.this.modCount;
                for (int i2 = 0; i2 < nodeArr.length && HashMap.this.modCount == i; i2++) {
                    for (Node<K, V> node = nodeArr[i2]; node != null; node = node.next) {
                        consumer.accept(node.value);
                    }
                }
                if (HashMap.this.modCount != i) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = this.entrySet;
        if (set != null) {
            return set;
        }
        EntrySet entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        EntrySet() {
        }

        @Override
        public final int size() {
            return HashMap.this.size;
        }

        @Override
        public final void clear() {
            HashMap.this.clear();
        }

        @Override
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public final boolean contains(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            Node<K, V> node = HashMap.this.getNode(HashMap.hash(key), key);
            return node != null && node.equals(entry);
        }

        @Override
        public final boolean remove(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object key = entry.getKey();
            return HashMap.this.removeNode(HashMap.hash(key), key, entry.getValue(), true, true) != null;
        }

        @Override
        public final Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator(HashMap.this, 0, -1, 0, 0);
        }

        @Override
        public final void forEach(Consumer<? super Map.Entry<K, V>> consumer) {
            Node<K, V>[] nodeArr;
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (HashMap.this.size > 0 && (nodeArr = HashMap.this.table) != null) {
                int i = HashMap.this.modCount;
                for (int i2 = 0; i2 < nodeArr.length && HashMap.this.modCount == i; i2++) {
                    for (Node<K, V> node = nodeArr[i2]; node != null; node = node.next) {
                        consumer.accept(node);
                    }
                }
                if (HashMap.this.modCount != i) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @Override
    public V getOrDefault(Object obj, V v) {
        Node<K, V> node = getNode(hash(obj), obj);
        return node == null ? v : node.value;
    }

    @Override
    public V putIfAbsent(K k, V v) {
        return putVal(hash(k), k, v, true, true);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        return removeNode(hash(obj), obj, obj2, true, true) != null;
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        Node<K, V> node = getNode(hash(k), k);
        if (node != null) {
            V v3 = node.value;
            if (v3 == v || (v3 != null && v3.equals(v))) {
                node.value = v2;
                afterNodeAccess(node);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public V replace(K k, V v) {
        Node<K, V> node = getNode(hash(k), k);
        if (node != null) {
            V v2 = node.value;
            node.value = v;
            afterNodeAccess(node);
            return v2;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
        Node<K, V>[] nodeArrResize;
        int length;
        int i;
        Node<K, V> treeNode;
        TreeNode treeNode2;
        K k2;
        V v;
        if (function == null) {
            throw new NullPointerException();
        }
        int iHash = hash(k);
        if (this.size > this.threshold || (nodeArrResize = this.table) == null || (length = nodeArrResize.length) == 0) {
            nodeArrResize = resize();
            length = nodeArrResize.length;
        }
        int i2 = length;
        Node<K, V>[] nodeArr = nodeArrResize;
        int i3 = (i2 - 1) & iHash;
        Node<K, V> node = nodeArr[i3];
        if (node != null) {
            if (node instanceof TreeNode) {
                treeNode2 = (TreeNode) node;
                i = 0;
                treeNode = treeNode2.getTreeNode(iHash, k);
            } else {
                int i4 = 0;
                treeNode = node;
                do {
                    if (treeNode.hash != iHash || ((k2 = treeNode.key) != k && (k == null || !k.equals(k2)))) {
                        i4++;
                        treeNode = treeNode.next;
                    } else {
                        i = i4;
                        treeNode2 = null;
                        break;
                    }
                } while (treeNode != null);
                treeNode = null;
                i = i4;
                treeNode2 = null;
            }
            if (treeNode != null && (v = treeNode.value) != null) {
                afterNodeAccess(treeNode);
                return v;
            }
        } else {
            i = 0;
            treeNode = null;
            treeNode2 = null;
        }
        V vApply = function.apply(k);
        if (vApply == null) {
            return null;
        }
        if (treeNode != null) {
            treeNode.value = vApply;
            afterNodeAccess(treeNode);
            return vApply;
        }
        if (treeNode2 != null) {
            treeNode2.putTreeVal(this, nodeArr, iHash, k, vApply);
        } else {
            nodeArr[i3] = newNode(iHash, k, vApply, node);
            if (i >= 7) {
                treeifyBin(nodeArr, iHash);
            }
        }
        this.modCount++;
        this.size++;
        afterNodeInsertion(true);
        return vApply;
    }

    @Override
    public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        V v;
        if (biFunction == null) {
            throw new NullPointerException();
        }
        int iHash = hash(k);
        Node<K, V> node = getNode(iHash, k);
        if (node != null && (v = node.value) != null) {
            V vApply = biFunction.apply(k, v);
            if (vApply != null) {
                node.value = vApply;
                afterNodeAccess(node);
                return vApply;
            }
            removeNode(iHash, k, null, false, true);
            return null;
        }
        return null;
    }

    @Override
    public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Node<K, V>[] nodeArrResize;
        int length;
        int i;
        Node<K, V> treeNode;
        Object obj;
        K k2;
        if (biFunction == null) {
            throw new NullPointerException();
        }
        int iHash = hash(k);
        if (this.size > this.threshold || (nodeArrResize = this.table) == null || (length = nodeArrResize.length) == 0) {
            nodeArrResize = resize();
            length = nodeArrResize.length;
        }
        int i2 = length;
        Node<K, V>[] nodeArr = nodeArrResize;
        int i3 = (i2 - 1) & iHash;
        Node<K, V> node = nodeArr[i3];
        V v = (Object) null;
        if (node == null) {
            i = 0;
            treeNode = null;
        } else {
            if (node instanceof TreeNode) {
                TreeNode treeNode2 = (TreeNode) node;
                i = 0;
                treeNode = treeNode2.getTreeNode(iHash, k);
                obj = treeNode2;
            } else {
                int i4 = 0;
                treeNode = node;
                do {
                    if (treeNode.hash != iHash || ((k2 = treeNode.key) != k && (k == null || !k.equals(k2)))) {
                        i4++;
                        treeNode = treeNode.next;
                    } else {
                        i = i4;
                        obj = null;
                        break;
                    }
                } while (treeNode != null);
                treeNode = null;
                i = i4;
            }
            if (treeNode != null) {
                v = treeNode.value;
            }
            V vApply = biFunction.apply(k, v);
            if (treeNode == null) {
                if (vApply != null) {
                    treeNode.value = vApply;
                    afterNodeAccess(treeNode);
                } else {
                    removeNode(iHash, k, null, false, true);
                }
            } else if (vApply != null) {
                if (obj != null) {
                    obj.putTreeVal(this, nodeArr, iHash, k, vApply);
                } else {
                    nodeArr[i3] = newNode(iHash, k, vApply, node);
                    if (i >= 7) {
                        treeifyBin(nodeArr, iHash);
                    }
                }
                this.modCount++;
                this.size++;
                afterNodeInsertion(true);
            }
            return vApply;
        }
        obj = treeNode;
        if (treeNode != null) {
        }
        V vApply2 = biFunction.apply(k, v);
        if (treeNode == null) {
        }
        return vApply2;
    }

    @Override
    public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        Node<K, V>[] nodeArrResize;
        int length;
        int i;
        TreeNode treeNode;
        K k2;
        if (v == 0) {
            throw new NullPointerException();
        }
        if (biFunction == null) {
            throw new NullPointerException();
        }
        int iHash = hash(k);
        if (this.size > this.threshold || (nodeArrResize = this.table) == null || (length = nodeArrResize.length) == 0) {
            nodeArrResize = resize();
            length = nodeArrResize.length;
        }
        int i2 = length;
        Node<K, V>[] nodeArr = nodeArrResize;
        int i3 = (i2 - 1) & iHash;
        Node<K, V> node = nodeArr[i3];
        Node<K, V> treeNode2 = null;
        if (node != null) {
            if (node instanceof TreeNode) {
                TreeNode treeNode3 = (TreeNode) node;
                i = 0;
                treeNode = treeNode3;
                treeNode2 = treeNode3.getTreeNode(iHash, k);
            } else {
                i = 0;
                Node<K, V> node2 = node;
                do {
                    if (node2.hash != iHash || ((k2 = node2.key) != k && (k == null || !k.equals(k2)))) {
                        i++;
                        node2 = node2.next;
                    } else {
                        treeNode2 = node2;
                        treeNode = null;
                        break;
                    }
                } while (node2 != null);
            }
            if (treeNode2 == null) {
                ?? Apply = v;
                if (treeNode2.value != null) {
                    Apply = biFunction.apply((V) treeNode2.value, v);
                }
                if (Apply != 0) {
                    treeNode2.value = (V) Apply;
                    afterNodeAccess(treeNode2);
                } else {
                    removeNode(iHash, k, null, false, true);
                }
                return (V) Apply;
            }
            if (v != 0) {
                if (treeNode != null) {
                    treeNode.putTreeVal(this, nodeArr, iHash, k, v);
                } else {
                    nodeArr[i3] = newNode(iHash, k, v, node);
                    if (i >= 7) {
                        treeifyBin(nodeArr, iHash);
                    }
                }
                this.modCount++;
                this.size++;
                afterNodeInsertion(true);
            }
            return v;
        }
        i = 0;
        treeNode = null;
        if (treeNode2 == null) {
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        Node<K, V>[] nodeArr;
        if (biConsumer == null) {
            throw new NullPointerException();
        }
        if (this.size > 0 && (nodeArr = this.table) != null) {
            int i = this.modCount;
            for (int i2 = 0; i2 < nodeArr.length && i == this.modCount; i2++) {
                for (Node<K, V> node = nodeArr[i2]; node != null; node = node.next) {
                    biConsumer.accept(node.key, node.value);
                }
            }
            if (this.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Node<K, V>[] nodeArr;
        if (biFunction == null) {
            throw new NullPointerException();
        }
        if (this.size > 0 && (nodeArr = this.table) != null) {
            int i = this.modCount;
            for (Node<K, V> node : nodeArr) {
                for (; node != null; node = node.next) {
                    node.value = biFunction.apply(node.key, node.value);
                }
            }
            if (this.modCount != i) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public Object clone() {
        try {
            HashMap map = (HashMap) super.clone();
            map.reinitialize();
            map.putMapEntries(this, false);
            return map;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    final float loadFactor() {
        return this.loadFactor;
    }

    final int capacity() {
        if (this.table != null) {
            return this.table.length;
        }
        if (this.threshold > 0) {
            return this.threshold;
        }
        return 16;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        int iCapacity = capacity();
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(iCapacity);
        objectOutputStream.writeInt(this.size);
        internalWriteEntries(objectOutputStream);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        int iTableSizeFor;
        objectInputStream.defaultReadObject();
        reinitialize();
        if (this.loadFactor <= 0.0f || Float.isNaN(this.loadFactor)) {
            throw new InvalidObjectException("Illegal load factor: " + this.loadFactor);
        }
        objectInputStream.readInt();
        int i = objectInputStream.readInt();
        if (i < 0) {
            throw new InvalidObjectException("Illegal mappings count: " + i);
        }
        if (i > 0) {
            float fMin = Math.min(Math.max(0.25f, this.loadFactor), 4.0f);
            float f = (i / fMin) + 1.0f;
            if (f < 16.0f) {
                iTableSizeFor = 16;
            } else if (f < 1.0737418E9f) {
                iTableSizeFor = tableSizeFor((int) f);
            } else {
                iTableSizeFor = MAXIMUM_CAPACITY;
            }
            float f2 = iTableSizeFor * fMin;
            this.threshold = (iTableSizeFor >= MAXIMUM_CAPACITY || f2 >= 1.0737418E9f) ? Integer.MAX_VALUE : (int) f2;
            this.table = new Node[iTableSizeFor];
            for (int i2 = 0; i2 < i; i2++) {
                Object object = objectInputStream.readObject();
                putVal(hash(object), object, objectInputStream.readObject(), false, false);
            }
        }
    }

    abstract class HashIterator {
        Node<K, V> current;
        int expectedModCount;
        int index;
        Node<K, V> next;

        HashIterator() {
            this.expectedModCount = HashMap.this.modCount;
            Node<K, V>[] nodeArr = HashMap.this.table;
            this.next = null;
            this.current = null;
            this.index = 0;
            if (nodeArr != null && HashMap.this.size > 0) {
                while (this.index < nodeArr.length) {
                    int i = this.index;
                    this.index = i + 1;
                    Node<K, V> node = nodeArr[i];
                    this.next = node;
                    if (node != null) {
                        return;
                    }
                }
            }
        }

        public final boolean hasNext() {
            return this.next != null;
        }

        final Node<K, V> nextNode() {
            Node<K, V>[] nodeArr;
            Node<K, V> node = this.next;
            if (HashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (node == null) {
                throw new NoSuchElementException();
            }
            this.current = node;
            Node<K, V> node2 = node.next;
            this.next = node2;
            if (node2 == null && (nodeArr = HashMap.this.table) != null) {
                while (this.index < nodeArr.length) {
                    int i = this.index;
                    this.index = i + 1;
                    Node<K, V> node3 = nodeArr[i];
                    this.next = node3;
                    if (node3 != null) {
                        break;
                    }
                }
            }
            return node;
        }

        public final void remove() {
            Node<K, V> node = this.current;
            if (node == null) {
                throw new IllegalStateException();
            }
            if (HashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            this.current = null;
            K k = node.key;
            HashMap.this.removeNode(HashMap.hash(k), k, null, false, false);
            this.expectedModCount = HashMap.this.modCount;
        }
    }

    final class KeyIterator extends HashMap<K, V>.HashIterator implements Iterator<K> {
        KeyIterator() {
            super();
        }

        @Override
        public final K next() {
            return nextNode().key;
        }
    }

    final class ValueIterator extends HashMap<K, V>.HashIterator implements Iterator<V> {
        ValueIterator() {
            super();
        }

        @Override
        public final V next() {
            return nextNode().value;
        }
    }

    final class EntryIterator extends HashMap<K, V>.HashIterator implements Iterator<Map.Entry<K, V>> {
        EntryIterator() {
            super();
        }

        @Override
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }

    static class HashMapSpliterator<K, V> {
        Node<K, V> current;
        int est;
        int expectedModCount;
        int fence;
        int index;
        final HashMap<K, V> map;

        HashMapSpliterator(HashMap<K, V> map, int i, int i2, int i3, int i4) {
            this.map = map;
            this.index = i;
            this.fence = i2;
            this.est = i3;
            this.expectedModCount = i4;
        }

        final int getFence() {
            int length = this.fence;
            if (length < 0) {
                HashMap<K, V> map = this.map;
                this.est = map.size;
                this.expectedModCount = map.modCount;
                Node<K, V>[] nodeArr = map.table;
                length = nodeArr == null ? 0 : nodeArr.length;
                this.fence = length;
            }
            return length;
        }

        public final long estimateSize() {
            getFence();
            return this.est;
        }
    }

    static final class KeySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(HashMap<K, V> map, int i, int i2, int i3, int i4) {
            super(map, i, i2, i3, i4);
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2 || this.current != null) {
                return null;
            }
            HashMap<K, V> map = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new KeySpliterator<>(map, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            int i;
            int i2;
            if (consumer == null) {
                throw new NullPointerException();
            }
            HashMap<K, V> map = this.map;
            Node<K, V>[] nodeArr = map.table;
            int i3 = this.fence;
            if (i3 < 0) {
                int i4 = map.modCount;
                this.expectedModCount = i4;
                int length = nodeArr == null ? 0 : nodeArr.length;
                this.fence = length;
                int i5 = length;
                i = i4;
                i3 = i5;
            } else {
                i = this.expectedModCount;
            }
            if (nodeArr == null || nodeArr.length < i3 || (i2 = this.index) < 0) {
                return;
            }
            this.index = i3;
            if (i2 < i3 || this.current != null) {
                Node<K, V> node = this.current;
                this.current = null;
                while (true) {
                    if (node == null) {
                        node = nodeArr[i2];
                        i2++;
                    } else {
                        consumer.accept(node.key);
                        node = node.next;
                    }
                    if (node == null && i2 >= i3) {
                        break;
                    }
                }
                if (map.modCount != i) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr == null) {
                return false;
            }
            int length = nodeArr.length;
            int fence = getFence();
            if (length < fence || this.index < 0) {
                return false;
            }
            while (true) {
                if (this.current != null || this.index < fence) {
                    if (this.current == null) {
                        int i = this.index;
                        this.index = i + 1;
                        this.current = nodeArr[i];
                    } else {
                        K k = this.current.key;
                        this.current = this.current.next;
                        consumer.accept(k);
                        if (this.map.modCount != this.expectedModCount) {
                            throw new ConcurrentModificationException();
                        }
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public int characteristics() {
            return ((this.fence < 0 || this.est == this.map.size) ? 64 : 0) | 1;
        }
    }

    static final class ValueSpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(HashMap<K, V> map, int i, int i2, int i3, int i4) {
            super(map, i, i2, i3, i4);
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2 || this.current != null) {
                return null;
            }
            HashMap<K, V> map = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new ValueSpliterator<>(map, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> consumer) {
            int i;
            int i2;
            if (consumer == null) {
                throw new NullPointerException();
            }
            HashMap<K, V> map = this.map;
            Node<K, V>[] nodeArr = map.table;
            int i3 = this.fence;
            if (i3 < 0) {
                int i4 = map.modCount;
                this.expectedModCount = i4;
                int length = nodeArr == null ? 0 : nodeArr.length;
                this.fence = length;
                int i5 = length;
                i = i4;
                i3 = i5;
            } else {
                i = this.expectedModCount;
            }
            if (nodeArr == null || nodeArr.length < i3 || (i2 = this.index) < 0) {
                return;
            }
            this.index = i3;
            if (i2 < i3 || this.current != null) {
                Node<K, V> node = this.current;
                this.current = null;
                while (true) {
                    if (node == null) {
                        node = nodeArr[i2];
                        i2++;
                    } else {
                        consumer.accept(node.value);
                        node = node.next;
                    }
                    if (node == null && i2 >= i3) {
                        break;
                    }
                }
                if (map.modCount != i) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr == null) {
                return false;
            }
            int length = nodeArr.length;
            int fence = getFence();
            if (length < fence || this.index < 0) {
                return false;
            }
            while (true) {
                if (this.current != null || this.index < fence) {
                    if (this.current == null) {
                        int i = this.index;
                        this.index = i + 1;
                        this.current = nodeArr[i];
                    } else {
                        V v = this.current.value;
                        this.current = this.current.next;
                        consumer.accept(v);
                        if (this.map.modCount != this.expectedModCount) {
                            throw new ConcurrentModificationException();
                        }
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public int characteristics() {
            return (this.fence < 0 || this.est == this.map.size) ? 64 : 0;
        }
    }

    static final class EntrySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(HashMap<K, V> map, int i, int i2, int i3, int i4) {
            super(map, i, i2, i3, i4);
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2 || this.current != null) {
                return null;
            }
            HashMap<K, V> map = this.map;
            this.index = i2;
            int i3 = this.est >>> 1;
            this.est = i3;
            return new EntrySpliterator<>(map, i, i2, i3, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> consumer) {
            int i;
            int i2;
            if (consumer == null) {
                throw new NullPointerException();
            }
            HashMap<K, V> map = this.map;
            Node<K, V>[] nodeArr = map.table;
            int i3 = this.fence;
            if (i3 < 0) {
                int i4 = map.modCount;
                this.expectedModCount = i4;
                int length = nodeArr == null ? 0 : nodeArr.length;
                this.fence = length;
                int i5 = length;
                i = i4;
                i3 = i5;
            } else {
                i = this.expectedModCount;
            }
            if (nodeArr == null || nodeArr.length < i3 || (i2 = this.index) < 0) {
                return;
            }
            this.index = i3;
            if (i2 < i3 || this.current != null) {
                Node<K, V> node = this.current;
                this.current = null;
                while (true) {
                    if (node == null) {
                        node = nodeArr[i2];
                        i2++;
                    } else {
                        consumer.accept(node);
                        node = node.next;
                    }
                    if (node == null && i2 >= i3) {
                        break;
                    }
                }
                if (map.modCount != i) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr == null) {
                return false;
            }
            int length = nodeArr.length;
            int fence = getFence();
            if (length < fence || this.index < 0) {
                return false;
            }
            while (true) {
                if (this.current != null || this.index < fence) {
                    if (this.current == null) {
                        int i = this.index;
                        this.index = i + 1;
                        this.current = nodeArr[i];
                    } else {
                        Node<K, V> node = this.current;
                        this.current = this.current.next;
                        consumer.accept(node);
                        if (this.map.modCount != this.expectedModCount) {
                            throw new ConcurrentModificationException();
                        }
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public int characteristics() {
            return ((this.fence < 0 || this.est == this.map.size) ? 64 : 0) | 1;
        }
    }

    Node<K, V> newNode(int i, K k, V v, Node<K, V> node) {
        return new Node<>(i, k, v, node);
    }

    Node<K, V> replacementNode(Node<K, V> node, Node<K, V> node2) {
        return new Node<>(node.hash, node.key, node.value, node2);
    }

    TreeNode<K, V> newTreeNode(int i, K k, V v, Node<K, V> node) {
        return new TreeNode<>(i, k, v, node);
    }

    TreeNode<K, V> replacementTreeNode(Node<K, V> node, Node<K, V> node2) {
        return new TreeNode<>(node.hash, node.key, node.value, node2);
    }

    void reinitialize() {
        this.table = null;
        this.entrySet = null;
        this.keySet = null;
        this.values = null;
        this.modCount = 0;
        this.threshold = 0;
        this.size = 0;
    }

    void afterNodeAccess(Node<K, V> node) {
    }

    void afterNodeInsertion(boolean z) {
    }

    void afterNodeRemoval(Node<K, V> node) {
    }

    void internalWriteEntries(ObjectOutputStream objectOutputStream) throws IOException {
        Node<K, V>[] nodeArr;
        if (this.size > 0 && (nodeArr = this.table) != null) {
            for (Node<K, V> node : nodeArr) {
                for (; node != null; node = node.next) {
                    objectOutputStream.writeObject(node.key);
                    objectOutputStream.writeObject(node.value);
                }
            }
        }
    }

    static final class TreeNode<K, V> extends LinkedHashMap.LinkedHashMapEntry<K, V> {
        static final boolean $assertionsDisabled = false;
        TreeNode<K, V> left;
        TreeNode<K, V> parent;
        TreeNode<K, V> prev;
        boolean red;
        TreeNode<K, V> right;

        TreeNode(int i, K k, V v, Node<K, V> node) {
            super(i, k, v, node);
        }

        final TreeNode<K, V> root() {
            TreeNode<K, V> treeNode = this;
            while (true) {
                TreeNode<K, V> treeNode2 = treeNode.parent;
                if (treeNode2 != null) {
                    treeNode = treeNode2;
                } else {
                    return treeNode;
                }
            }
        }

        static <K, V> void moveRootToFront(Node<K, V>[] nodeArr, TreeNode<K, V> treeNode) {
            int length;
            if (treeNode != null && nodeArr != null && (length = nodeArr.length) > 0) {
                int i = (length - 1) & treeNode.hash;
                TreeNode<K, V> treeNode2 = (TreeNode) nodeArr[i];
                if (treeNode != treeNode2) {
                    nodeArr[i] = treeNode;
                    TreeNode<K, V> treeNode3 = treeNode.prev;
                    Node<K, V> node = treeNode.next;
                    if (node != null) {
                        ((TreeNode) node).prev = treeNode3;
                    }
                    if (treeNode3 != null) {
                        treeNode3.next = node;
                    }
                    if (treeNode2 != null) {
                        treeNode2.prev = treeNode;
                    }
                    treeNode.next = treeNode2;
                    treeNode.prev = null;
                }
            }
        }

        final TreeNode<K, V> find(int i, Object obj, Class<?> cls) {
            int iCompareComparables;
            Class<?> clsComparableClassFor = cls;
            TreeNode<K, V> treeNode = this;
            do {
                TreeNode<K, V> treeNode2 = treeNode.left;
                TreeNode<K, V> treeNode3 = treeNode.right;
                int i2 = treeNode.hash;
                if (i2 <= i) {
                    if (i2 >= i) {
                        K k = treeNode.key;
                        if (k == obj || (obj != null && obj.equals(k))) {
                            return treeNode;
                        }
                        if (treeNode2 != null) {
                            if (treeNode3 != null) {
                                if ((clsComparableClassFor == null && (clsComparableClassFor = HashMap.comparableClassFor(obj)) == null) || (iCompareComparables = HashMap.compareComparables(clsComparableClassFor, obj, k)) == 0) {
                                    TreeNode<K, V> treeNodeFind = treeNode3.find(i, obj, clsComparableClassFor);
                                    if (treeNodeFind != null) {
                                        return treeNodeFind;
                                    }
                                } else if (iCompareComparables >= 0) {
                                    treeNode2 = treeNode3;
                                }
                            }
                            treeNode = treeNode2;
                        }
                    }
                    treeNode = treeNode3;
                } else {
                    treeNode = treeNode2;
                }
            } while (treeNode != null);
            return null;
        }

        final TreeNode<K, V> getTreeNode(int i, Object obj) {
            return (this.parent != null ? root() : this).find(i, obj, null);
        }

        static int tieBreakOrder(Object obj, Object obj2) {
            int iCompareTo;
            if (obj == null || obj2 == null || (iCompareTo = obj.getClass().getName().compareTo(obj2.getClass().getName())) == 0) {
                return System.identityHashCode(obj) <= System.identityHashCode(obj2) ? -1 : 1;
            }
            return iCompareTo;
        }

        final void treeify(Node<K, V>[] nodeArr) {
            int iCompareComparables;
            int iTieBreakOrder;
            TreeNode<K, V> treeNodeBalanceInsertion = this;
            TreeNode<K, V> treeNode = null;
            while (treeNodeBalanceInsertion != null) {
                TreeNode<K, V> treeNode2 = (TreeNode) treeNodeBalanceInsertion.next;
                treeNodeBalanceInsertion.right = null;
                treeNodeBalanceInsertion.left = null;
                if (treeNode == null) {
                    treeNodeBalanceInsertion.parent = null;
                    treeNodeBalanceInsertion.red = false;
                } else {
                    K k = treeNodeBalanceInsertion.key;
                    int i = treeNodeBalanceInsertion.hash;
                    Class<?> clsComparableClassFor = null;
                    TreeNode<K, V> treeNode3 = treeNode;
                    while (true) {
                        K k2 = treeNode3.key;
                        int i2 = treeNode3.hash;
                        if (i2 > i) {
                            iTieBreakOrder = -1;
                        } else if (i2 < i) {
                            iTieBreakOrder = 1;
                        } else if ((clsComparableClassFor == null && (clsComparableClassFor = HashMap.comparableClassFor(k)) == null) || (iCompareComparables = HashMap.compareComparables(clsComparableClassFor, k, k2)) == 0) {
                            iTieBreakOrder = tieBreakOrder(k, k2);
                        } else {
                            iTieBreakOrder = iCompareComparables;
                        }
                        TreeNode<K, V> treeNode4 = iTieBreakOrder <= 0 ? treeNode3.left : treeNode3.right;
                        if (treeNode4 == null) {
                            break;
                        } else {
                            treeNode3 = treeNode4;
                        }
                    }
                    treeNodeBalanceInsertion.parent = treeNode3;
                    if (iTieBreakOrder <= 0) {
                        treeNode3.left = treeNodeBalanceInsertion;
                    } else {
                        treeNode3.right = treeNodeBalanceInsertion;
                    }
                    treeNodeBalanceInsertion = balanceInsertion(treeNode, treeNodeBalanceInsertion);
                }
                treeNode = treeNodeBalanceInsertion;
                treeNodeBalanceInsertion = treeNode2;
            }
            moveRootToFront(nodeArr, treeNode);
        }

        final Node<K, V> untreeify(HashMap<K, V> map) {
            Node<K, V> node = this;
            Node<K, V> node2 = null;
            Node<K, V> node3 = null;
            while (node != null) {
                Node<K, V> nodeReplacementNode = map.replacementNode(node, null);
                if (node2 != null) {
                    node2.next = nodeReplacementNode;
                } else {
                    node3 = nodeReplacementNode;
                }
                node = node.next;
                node2 = nodeReplacementNode;
            }
            return node3;
        }

        final TreeNode<K, V> putTreeVal(HashMap<K, V> map, Node<K, V>[] nodeArr, int i, K k, V v) {
            int iCompareComparables;
            TreeNode<K, V> treeNode;
            TreeNode<K, V> treeNodeRoot = this.parent != null ? root() : this;
            Class<?> clsComparableClassFor = null;
            boolean z = false;
            TreeNode<K, V> treeNode2 = treeNodeRoot;
            while (true) {
                int i2 = treeNode2.hash;
                int iTieBreakOrder = 1;
                if (i2 > i) {
                    iTieBreakOrder = -1;
                } else if (i2 >= i) {
                    K k2 = treeNode2.key;
                    if (k2 == k || (k != null && k.equals(k2))) {
                        break;
                    }
                    if ((clsComparableClassFor == null && (clsComparableClassFor = HashMap.comparableClassFor(k)) == null) || (iCompareComparables = HashMap.compareComparables(clsComparableClassFor, k, k2)) == 0) {
                        if (!z) {
                            TreeNode<K, V> treeNode3 = treeNode2.left;
                            if ((treeNode3 != null && (r4 = treeNode3.find(i, k, clsComparableClassFor)) != null) || ((treeNode = treeNode2.right) != null && (r4 = treeNode.find(i, k, clsComparableClassFor)) != null)) {
                                break;
                            }
                            z = true;
                        }
                        iTieBreakOrder = tieBreakOrder(k, k2);
                    } else {
                        iTieBreakOrder = iCompareComparables;
                    }
                }
                TreeNode<K, V> treeNode4 = iTieBreakOrder <= 0 ? treeNode2.left : treeNode2.right;
                if (treeNode4 != null) {
                    treeNode2 = treeNode4;
                } else {
                    Node<K, V> node = treeNode2.next;
                    TreeNode<K, V> treeNodeNewTreeNode = map.newTreeNode(i, k, v, node);
                    if (iTieBreakOrder <= 0) {
                        treeNode2.left = treeNodeNewTreeNode;
                    } else {
                        treeNode2.right = treeNodeNewTreeNode;
                    }
                    treeNode2.next = treeNodeNewTreeNode;
                    treeNodeNewTreeNode.prev = treeNode2;
                    treeNodeNewTreeNode.parent = treeNode2;
                    if (node != null) {
                        ((TreeNode) node).prev = treeNodeNewTreeNode;
                    }
                    moveRootToFront(nodeArr, balanceInsertion(treeNodeRoot, treeNodeNewTreeNode));
                    return null;
                }
            }
        }

        final void removeTreeNode(HashMap<K, V> map, Node<K, V>[] nodeArr, boolean z) {
            int length;
            TreeNode<K, V> treeNode;
            TreeNode<K, V> treeNode2;
            if (nodeArr == null || (length = nodeArr.length) == 0) {
                return;
            }
            int i = (length - 1) & this.hash;
            TreeNode<K, V> treeNodeBalanceDeletion = (TreeNode) nodeArr[i];
            TreeNode<K, V> treeNode3 = (TreeNode) this.next;
            TreeNode<K, V> treeNode4 = this.prev;
            if (treeNode4 == null) {
                nodeArr[i] = treeNode3;
                treeNode = treeNode3;
            } else {
                treeNode4.next = treeNode3;
                treeNode = treeNodeBalanceDeletion;
            }
            if (treeNode3 != null) {
                treeNode3.prev = treeNode4;
            }
            if (treeNode == null) {
                return;
            }
            if (treeNodeBalanceDeletion.parent != null) {
                treeNodeBalanceDeletion = treeNodeBalanceDeletion.root();
            }
            if (treeNodeBalanceDeletion == null || treeNodeBalanceDeletion.right == null || (treeNode2 = treeNodeBalanceDeletion.left) == null || treeNode2.left == null) {
                nodeArr[i] = treeNode.untreeify(map);
                return;
            }
            TreeNode<K, V> treeNode5 = this.left;
            TreeNode<K, V> treeNode6 = this.right;
            if (treeNode5 != null && treeNode6 != null) {
                TreeNode<K, V> treeNode7 = treeNode6;
                while (true) {
                    TreeNode<K, V> treeNode8 = treeNode7.left;
                    if (treeNode8 == null) {
                        break;
                    } else {
                        treeNode7 = treeNode8;
                    }
                }
                boolean z2 = treeNode7.red;
                treeNode7.red = this.red;
                this.red = z2;
                TreeNode<K, V> treeNode9 = treeNode7.right;
                TreeNode<K, V> treeNode10 = this.parent;
                if (treeNode7 == treeNode6) {
                    this.parent = treeNode7;
                    treeNode7.right = this;
                } else {
                    TreeNode<K, V> treeNode11 = treeNode7.parent;
                    this.parent = treeNode11;
                    if (treeNode11 != null) {
                        if (treeNode7 == treeNode11.left) {
                            treeNode11.left = this;
                        } else {
                            treeNode11.right = this;
                        }
                    }
                    treeNode7.right = treeNode6;
                    if (treeNode6 != null) {
                        treeNode6.parent = treeNode7;
                    }
                }
                this.left = null;
                this.right = treeNode9;
                if (treeNode9 != null) {
                    treeNode9.parent = this;
                }
                treeNode7.left = treeNode5;
                if (treeNode5 != null) {
                    treeNode5.parent = treeNode7;
                }
                treeNode7.parent = treeNode10;
                if (treeNode10 != null) {
                    if (this == treeNode10.left) {
                        treeNode10.left = treeNode7;
                    } else {
                        treeNode10.right = treeNode7;
                    }
                } else {
                    treeNodeBalanceDeletion = treeNode7;
                }
                if (treeNode9 == null) {
                    treeNode9 = this;
                }
                treeNode5 = treeNode9;
            } else if (treeNode5 == null) {
                treeNode5 = treeNode6 != null ? treeNode6 : this;
            }
            if (treeNode5 != this) {
                TreeNode<K, V> treeNode12 = this.parent;
                treeNode5.parent = treeNode12;
                if (treeNode12 != null) {
                    if (this == treeNode12.left) {
                        treeNode12.left = treeNode5;
                    } else {
                        treeNode12.right = treeNode5;
                    }
                } else {
                    treeNodeBalanceDeletion = treeNode5;
                }
                this.parent = null;
                this.right = null;
                this.left = null;
            }
            if (!this.red) {
                treeNodeBalanceDeletion = balanceDeletion(treeNodeBalanceDeletion, treeNode5);
            }
            if (treeNode5 == this) {
                TreeNode<K, V> treeNode13 = this.parent;
                this.parent = null;
                if (treeNode13 != null) {
                    if (this == treeNode13.left) {
                        treeNode13.left = null;
                    } else if (this == treeNode13.right) {
                        treeNode13.right = null;
                    }
                }
            }
            if (z) {
                moveRootToFront(nodeArr, treeNodeBalanceDeletion);
            }
        }

        final void split(HashMap<K, V> map, Node<K, V>[] nodeArr, int i, int i2) {
            int i3 = 0;
            int i4 = 0;
            TreeNode<K, V> treeNode = null;
            TreeNode<K, V> treeNode2 = null;
            TreeNode<K, V> treeNode3 = null;
            TreeNode<K, V> treeNode4 = null;
            TreeNode<K, V> treeNode5 = this;
            while (treeNode5 != null) {
                TreeNode<K, V> treeNode6 = (TreeNode) treeNode5.next;
                treeNode5.next = null;
                if ((treeNode5.hash & i2) == 0) {
                    treeNode5.prev = treeNode2;
                    if (treeNode2 != null) {
                        treeNode2.next = treeNode5;
                    } else {
                        treeNode = treeNode5;
                    }
                    i3++;
                    treeNode2 = treeNode5;
                } else {
                    treeNode5.prev = treeNode3;
                    if (treeNode3 != null) {
                        treeNode3.next = treeNode5;
                    } else {
                        treeNode4 = treeNode5;
                    }
                    i4++;
                    treeNode3 = treeNode5;
                }
                treeNode5 = treeNode6;
            }
            if (treeNode != null) {
                if (i3 <= 6) {
                    nodeArr[i] = treeNode.untreeify(map);
                } else {
                    nodeArr[i] = treeNode;
                    if (treeNode4 != null) {
                        treeNode.treeify(nodeArr);
                    }
                }
            }
            if (treeNode4 != null) {
                if (i4 <= 6) {
                    nodeArr[i + i2] = treeNode4.untreeify(map);
                    return;
                }
                nodeArr[i + i2] = treeNode4;
                if (treeNode != null) {
                    treeNode4.treeify(nodeArr);
                }
            }
        }

        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> treeNode, TreeNode<K, V> treeNode2) {
            TreeNode<K, V> treeNode3;
            if (treeNode2 != null && (treeNode3 = treeNode2.right) != null) {
                TreeNode<K, V> treeNode4 = treeNode3.left;
                treeNode2.right = treeNode4;
                if (treeNode4 != null) {
                    treeNode4.parent = treeNode2;
                }
                TreeNode<K, V> treeNode5 = treeNode2.parent;
                treeNode3.parent = treeNode5;
                if (treeNode5 == null) {
                    treeNode3.red = false;
                    treeNode = treeNode3;
                } else if (treeNode5.left == treeNode2) {
                    treeNode5.left = treeNode3;
                } else {
                    treeNode5.right = treeNode3;
                }
                treeNode3.left = treeNode2;
                treeNode2.parent = treeNode3;
            }
            return treeNode;
        }

        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> treeNode, TreeNode<K, V> treeNode2) {
            TreeNode<K, V> treeNode3;
            if (treeNode2 != null && (treeNode3 = treeNode2.left) != null) {
                TreeNode<K, V> treeNode4 = treeNode3.right;
                treeNode2.left = treeNode4;
                if (treeNode4 != null) {
                    treeNode4.parent = treeNode2;
                }
                TreeNode<K, V> treeNode5 = treeNode2.parent;
                treeNode3.parent = treeNode5;
                if (treeNode5 == null) {
                    treeNode3.red = false;
                    treeNode = treeNode3;
                } else if (treeNode5.right == treeNode2) {
                    treeNode5.right = treeNode3;
                } else {
                    treeNode5.left = treeNode3;
                }
                treeNode3.right = treeNode2;
                treeNode2.parent = treeNode3;
            }
            return treeNode;
        }

        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> treeNode, TreeNode<K, V> treeNode2) {
            TreeNode<K, V> treeNode3;
            TreeNode<K, V> treeNode4;
            TreeNode<K, V> treeNode5;
            treeNode2.red = true;
            while (true) {
                TreeNode<K, V> treeNode6 = treeNode2.parent;
                if (treeNode6 == null) {
                    treeNode2.red = false;
                    return treeNode2;
                }
                if (!treeNode6.red || (treeNode3 = treeNode6.parent) == null) {
                    break;
                }
                TreeNode<K, V> treeNode7 = treeNode3.left;
                if (treeNode6 == treeNode7) {
                    TreeNode<K, V> treeNode8 = treeNode3.right;
                    if (treeNode8 != null && treeNode8.red) {
                        treeNode8.red = false;
                        treeNode6.red = false;
                        treeNode3.red = true;
                        treeNode2 = treeNode3;
                    } else {
                        if (treeNode2 == treeNode6.right) {
                            treeNode = rotateLeft(treeNode, treeNode6);
                            treeNode5 = treeNode6.parent;
                            if (treeNode5 != null) {
                                treeNode3 = treeNode5.parent;
                            } else {
                                treeNode3 = null;
                            }
                        } else {
                            treeNode6 = treeNode2;
                            treeNode5 = treeNode6;
                        }
                        if (treeNode5 != null) {
                            treeNode5.red = false;
                            if (treeNode3 != null) {
                                treeNode3.red = true;
                                treeNode = rotateRight(treeNode, treeNode3);
                            }
                        }
                        treeNode2 = treeNode6;
                    }
                } else if (treeNode7 != null && treeNode7.red) {
                    treeNode7.red = false;
                    treeNode6.red = false;
                    treeNode3.red = true;
                    treeNode2 = treeNode3;
                } else {
                    if (treeNode2 == treeNode6.left) {
                        treeNode = rotateRight(treeNode, treeNode6);
                        treeNode4 = treeNode6.parent;
                        if (treeNode4 != null) {
                            treeNode3 = treeNode4.parent;
                        } else {
                            treeNode3 = null;
                        }
                    } else {
                        treeNode6 = treeNode2;
                        treeNode4 = treeNode6;
                    }
                    if (treeNode4 != null) {
                        treeNode4.red = false;
                        if (treeNode3 != null) {
                            treeNode3.red = true;
                            treeNode = rotateLeft(treeNode, treeNode3);
                        }
                    }
                    treeNode2 = treeNode6;
                }
            }
            return treeNode;
        }

        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> treeNode, TreeNode<K, V> treeNode2) {
            while (treeNode2 != null && treeNode2 != treeNode) {
                TreeNode<K, V> treeNode3 = treeNode2.parent;
                if (treeNode3 == null) {
                    treeNode2.red = false;
                    return treeNode2;
                }
                if (treeNode2.red) {
                    treeNode2.red = false;
                    return treeNode;
                }
                TreeNode<K, V> treeNode4 = treeNode3.left;
                if (treeNode4 == treeNode2) {
                    TreeNode<K, V> treeNode5 = treeNode3.right;
                    if (treeNode5 != null && treeNode5.red) {
                        treeNode5.red = false;
                        treeNode3.red = true;
                        treeNode = rotateLeft(treeNode, treeNode3);
                        treeNode3 = treeNode2.parent;
                        treeNode5 = treeNode3 == null ? null : treeNode3.right;
                    }
                    if (treeNode5 == null) {
                        treeNode2 = treeNode3;
                    } else {
                        TreeNode<K, V> treeNode6 = treeNode5.left;
                        TreeNode<K, V> treeNode7 = treeNode5.right;
                        if ((treeNode7 == null || !treeNode7.red) && (treeNode6 == null || !treeNode6.red)) {
                            treeNode5.red = true;
                            treeNode2 = treeNode3;
                        } else {
                            if (treeNode7 == null || !treeNode7.red) {
                                if (treeNode6 != null) {
                                    treeNode6.red = false;
                                }
                                treeNode5.red = true;
                                treeNode = rotateRight(treeNode, treeNode5);
                                treeNode3 = treeNode2.parent;
                                treeNode5 = treeNode3 != null ? treeNode3.right : null;
                            }
                            if (treeNode5 != null) {
                                treeNode5.red = treeNode3 == null ? false : treeNode3.red;
                                TreeNode<K, V> treeNode8 = treeNode5.right;
                                if (treeNode8 != null) {
                                    treeNode8.red = false;
                                }
                            }
                            if (treeNode3 != null) {
                                treeNode3.red = false;
                                treeNode = rotateLeft(treeNode, treeNode3);
                            }
                            treeNode2 = treeNode;
                        }
                    }
                } else {
                    if (treeNode4 != null && treeNode4.red) {
                        treeNode4.red = false;
                        treeNode3.red = true;
                        treeNode = rotateRight(treeNode, treeNode3);
                        treeNode3 = treeNode2.parent;
                        treeNode4 = treeNode3 == null ? null : treeNode3.left;
                    }
                    if (treeNode4 == null) {
                        treeNode2 = treeNode3;
                    } else {
                        TreeNode<K, V> treeNode9 = treeNode4.left;
                        TreeNode<K, V> treeNode10 = treeNode4.right;
                        if ((treeNode9 == null || !treeNode9.red) && (treeNode10 == null || !treeNode10.red)) {
                            treeNode4.red = true;
                            treeNode2 = treeNode3;
                        } else {
                            if (treeNode9 == null || !treeNode9.red) {
                                if (treeNode10 != null) {
                                    treeNode10.red = false;
                                }
                                treeNode4.red = true;
                                treeNode = rotateLeft(treeNode, treeNode4);
                                treeNode3 = treeNode2.parent;
                                treeNode4 = treeNode3 != null ? treeNode3.left : null;
                            }
                            if (treeNode4 != null) {
                                treeNode4.red = treeNode3 == null ? false : treeNode3.red;
                                TreeNode<K, V> treeNode11 = treeNode4.left;
                                if (treeNode11 != null) {
                                    treeNode11.red = false;
                                }
                            }
                            if (treeNode3 != null) {
                                treeNode3.red = false;
                                treeNode = rotateRight(treeNode, treeNode3);
                            }
                            treeNode2 = treeNode;
                        }
                    }
                }
            }
            return treeNode;
        }

        static <K, V> boolean checkInvariants(TreeNode<K, V> treeNode) {
            TreeNode<K, V> treeNode2 = treeNode.parent;
            TreeNode<K, V> treeNode3 = treeNode.left;
            TreeNode<K, V> treeNode4 = treeNode.right;
            TreeNode<K, V> treeNode5 = treeNode.prev;
            TreeNode treeNode6 = (TreeNode) treeNode.next;
            if (treeNode5 != null && treeNode5.next != treeNode) {
                return false;
            }
            if (treeNode6 != null && treeNode6.prev != treeNode) {
                return false;
            }
            if (treeNode2 != null && treeNode != treeNode2.left && treeNode != treeNode2.right) {
                return false;
            }
            if (treeNode3 != null && (treeNode3.parent != treeNode || treeNode3.hash > treeNode.hash)) {
                return false;
            }
            if (treeNode4 != null && (treeNode4.parent != treeNode || treeNode4.hash < treeNode.hash)) {
                return false;
            }
            if (treeNode.red && treeNode3 != null && treeNode3.red && treeNode4 != null && treeNode4.red) {
                return false;
            }
            if (treeNode3 != null && !checkInvariants(treeNode3)) {
                return false;
            }
            if (treeNode4 != null && !checkInvariants(treeNode4)) {
                return false;
            }
            return true;
        }
    }
}
