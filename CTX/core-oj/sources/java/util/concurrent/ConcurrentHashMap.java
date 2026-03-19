package java.util.concurrent;

import java.awt.font.NumericShaper;
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
            int iArrayIndexScale = U.arrayIndexScale(Node[].class);
            if (((iArrayIndexScale - 1) & iArrayIndexScale) != 0) {
                throw new Error("array index scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(iArrayIndexScale);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        volatile Node<K, V> next;
        volatile V val;

        Node(int i, K k, V v, Node<K, V> node) {
            this.hash = i;
            this.key = k;
            this.val = v;
            this.next = node;
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
        public final V setValue(V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean equals(Object obj) {
            Map.Entry entry;
            Object key;
            Object value;
            V v;
            return (obj instanceof Map.Entry) && (key = (entry = (Map.Entry) obj).getKey()) != null && (value = entry.getValue()) != null && (key == this.key || key.equals(this.key)) && (value == (v = this.val) || value.equals(v));
        }

        Node<K, V> find(int i, Object obj) {
            K k;
            if (obj != null) {
                Node<K, V> node = this;
                do {
                    if (node.hash == i && ((k = node.key) == obj || (k != null && obj.equals(k)))) {
                        return node;
                    }
                    node = node.next;
                } while (node != null);
                return null;
            }
            return null;
        }
    }

    static final int spread(int i) {
        return (i ^ (i >>> 16)) & Integer.MAX_VALUE;
    }

    private static final int tableSizeFor(int i) {
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

    static final <K, V> Node<K, V> tabAt(Node<K, V>[] nodeArr, int i) {
        return (Node) U.getObjectVolatile(nodeArr, (((long) i) << ASHIFT) + ((long) ABASE));
    }

    static final <K, V> boolean casTabAt(Node<K, V>[] nodeArr, int i, Node<K, V> node, Node<K, V> node2) {
        return U.compareAndSwapObject(nodeArr, ((long) ABASE) + (((long) i) << ASHIFT), node, node2);
    }

    static final <K, V> void setTabAt(Node<K, V>[] nodeArr, int i, Node<K, V> node) {
        U.putObjectVolatile(nodeArr, (((long) i) << ASHIFT) + ((long) ABASE), node);
    }

    public ConcurrentHashMap() {
    }

    public ConcurrentHashMap(int i) {
        int iTableSizeFor;
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        if (i >= 536870912) {
            iTableSizeFor = MAXIMUM_CAPACITY;
        } else {
            iTableSizeFor = tableSizeFor(i + (i >>> 1) + 1);
        }
        this.sizeCtl = iTableSizeFor;
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> map) {
        this.sizeCtl = 16;
        putAll(map);
    }

    public ConcurrentHashMap(int i, float f) {
        this(i, f, 1);
    }

    public ConcurrentHashMap(int i, float f, int i2) {
        if (f <= 0.0f || i < 0 || i2 <= 0) {
            throw new IllegalArgumentException();
        }
        long j = (long) (1.0d + ((double) ((i < i2 ? i2 : i) / f)));
        this.sizeCtl = j >= 1073741824 ? MAXIMUM_CAPACITY : tableSizeFor((int) j);
    }

    @Override
    public int size() {
        long jSumCount = sumCount();
        if (jSumCount < 0) {
            return 0;
        }
        if (jSumCount > 2147483647L) {
            return Integer.MAX_VALUE;
        }
        return (int) jSumCount;
    }

    @Override
    public boolean isEmpty() {
        return sumCount() <= 0;
    }

    @Override
    public V get(Object obj) {
        int length;
        Node<K, V> nodeTabAt;
        K k;
        int iSpread = spread(obj.hashCode());
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null && (length = nodeArr.length) > 0 && (nodeTabAt = tabAt(nodeArr, (length - 1) & iSpread)) != null) {
            int i = nodeTabAt.hash;
            if (i == iSpread) {
                K k2 = nodeTabAt.key;
                if (k2 == obj || (k2 != null && obj.equals(k2))) {
                    return nodeTabAt.val;
                }
            } else if (i < 0) {
                Node<K, V> nodeFind = nodeTabAt.find(iSpread, obj);
                if (nodeFind != null) {
                    return nodeFind.val;
                }
                return null;
            }
            while (true) {
                nodeTabAt = nodeTabAt.next;
                if (nodeTabAt == null) {
                    break;
                }
                if (nodeTabAt.hash == iSpread && ((k = nodeTabAt.key) == obj || (k != null && obj.equals(k)))) {
                    break;
                }
            }
            return nodeTabAt.val;
        }
        return null;
    }

    @Override
    public boolean containsKey(Object obj) {
        return get(obj) != null;
    }

    @Override
    public boolean containsValue(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null) {
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    break;
                }
                V v = nodeAdvance.val;
                if (v == obj) {
                    return true;
                }
                if (v != null && obj.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V put(K k, V v) {
        return putVal(k, v, false);
    }

    final V putVal(K k, V v, boolean z) {
        V v2;
        K k2;
        if (k == null || v == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        int i = 0;
        Node<K, V>[] nodeArrInitTable = this.table;
        while (true) {
            if (nodeArrInitTable != null) {
                int length = nodeArrInitTable.length;
                if (length != 0) {
                    int i2 = (length - 1) & iSpread;
                    Node<K, V> nodeTabAt = tabAt(nodeArrInitTable, i2);
                    if (nodeTabAt == null) {
                        if (casTabAt(nodeArrInitTable, i2, null, new Node(iSpread, k, v, null))) {
                            break;
                        }
                    } else {
                        int i3 = nodeTabAt.hash;
                        if (i3 == -1) {
                            nodeArrInitTable = helpTransfer(nodeArrInitTable, nodeTabAt);
                        } else {
                            synchronized (nodeTabAt) {
                                if (tabAt(nodeArrInitTable, i2) == nodeTabAt) {
                                    if (i3 >= 0) {
                                        int i4 = 1;
                                        Node<K, V> node = nodeTabAt;
                                        while (true) {
                                            if (node.hash == iSpread && ((k2 = node.key) == k || (k2 != null && k.equals(k2)))) {
                                                break;
                                            }
                                            Node<K, V> node2 = node.next;
                                            if (node2 != null) {
                                                i4++;
                                                node = node2;
                                            } else {
                                                node.next = new Node<>(iSpread, k, v, null);
                                                v2 = null;
                                                break;
                                            }
                                        }
                                        v2 = node.val;
                                        if (!z) {
                                            node.val = v;
                                        }
                                        i = i4;
                                    } else if (nodeTabAt instanceof TreeBin) {
                                        i = 2;
                                        TreeNode<K, V> treeNodePutTreeVal = ((TreeBin) nodeTabAt).putTreeVal(iSpread, k, v);
                                        if (treeNodePutTreeVal != null) {
                                            v2 = treeNodePutTreeVal.val;
                                            if (!z) {
                                                treeNodePutTreeVal.val = v;
                                            }
                                        } else {
                                            v2 = null;
                                        }
                                    } else if (nodeTabAt instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                                v2 = null;
                            }
                            if (i != 0) {
                                if (i >= 8) {
                                    treeifyBin(nodeArrInitTable, i2);
                                }
                                if (v2 != null) {
                                    return v2;
                                }
                            }
                        }
                    }
                }
            }
            nodeArrInitTable = initTable();
        }
        addCount(1L, i);
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        tryPresize(map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            putVal(entry.getKey(), entry.getValue(), false);
        }
    }

    @Override
    public V remove(Object obj) {
        return replaceNode(obj, null, null);
    }

    final V replaceNode(Object obj, V v, Object obj2) {
        int length;
        int i;
        Node<K, V> nodeTabAt;
        boolean z;
        V v2;
        TreeNode<K, V> treeNodeFindTreeNode;
        K k;
        int iSpread = spread(obj.hashCode());
        Node<K, V>[] nodeArrHelpTransfer = this.table;
        while (true) {
            if (nodeArrHelpTransfer == null || (length = nodeArrHelpTransfer.length) == 0 || (nodeTabAt = tabAt(nodeArrHelpTransfer, (i = (length - 1) & iSpread))) == null) {
                break;
            }
            int i2 = nodeTabAt.hash;
            if (i2 == -1) {
                nodeArrHelpTransfer = helpTransfer(nodeArrHelpTransfer, nodeTabAt);
            } else {
                synchronized (nodeTabAt) {
                    z = true;
                    if (tabAt(nodeArrHelpTransfer, i) == nodeTabAt) {
                        if (i2 >= 0) {
                            Node<K, V> node = null;
                            Node<K, V> node2 = nodeTabAt;
                            while (true) {
                                if (node2.hash == iSpread && ((k = node2.key) == obj || (k != null && obj.equals(k)))) {
                                    break;
                                }
                                Node<K, V> node3 = node2.next;
                                if (node3 == null) {
                                    break;
                                }
                                node = node2;
                                node2 = node3;
                            }
                            v2 = node2.val;
                            if (obj2 == null || obj2 == v2 || (v2 != null && obj2.equals(v2))) {
                                if (v != null) {
                                    node2.val = v;
                                } else if (node != null) {
                                    node.next = node2.next;
                                } else {
                                    setTabAt(nodeArrHelpTransfer, i, node2.next);
                                }
                            } else {
                                v2 = null;
                            }
                        } else if (nodeTabAt instanceof TreeBin) {
                            TreeBin treeBin = (TreeBin) nodeTabAt;
                            TreeNode<K, V> treeNode = treeBin.root;
                            if (treeNode != null && (treeNodeFindTreeNode = treeNode.findTreeNode(iSpread, obj, null)) != null) {
                                v2 = treeNodeFindTreeNode.val;
                                if (obj2 == null || obj2 == v2 || (v2 != null && obj2.equals(v2))) {
                                    if (v != null) {
                                        treeNodeFindTreeNode.val = v;
                                    } else if (treeBin.removeTreeNode(treeNodeFindTreeNode)) {
                                        setTabAt(nodeArrHelpTransfer, i, untreeify(treeBin.first));
                                    }
                                }
                            }
                            v2 = null;
                        } else if (nodeTabAt instanceof ReservationNode) {
                            throw new IllegalStateException("Recursive update");
                        }
                    }
                    v2 = null;
                    z = false;
                }
                if (z) {
                    if (v2 != null) {
                        if (v == null) {
                            addCount(-1L, -1);
                        }
                        return v2;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void clear() {
        Node<K, V>[] nodeArrHelpTransfer = this.table;
        int i = 0;
        long j = 0;
        while (nodeArrHelpTransfer != null && i < nodeArrHelpTransfer.length) {
            Node<K, V> nodeTabAt = tabAt(nodeArrHelpTransfer, i);
            if (nodeTabAt == null) {
                i++;
            } else {
                int i2 = nodeTabAt.hash;
                if (i2 == -1) {
                    nodeArrHelpTransfer = helpTransfer(nodeArrHelpTransfer, nodeTabAt);
                    i = 0;
                } else {
                    synchronized (nodeTabAt) {
                        if (tabAt(nodeArrHelpTransfer, i) == nodeTabAt) {
                            for (Node<K, V> node = i2 >= 0 ? nodeTabAt : nodeTabAt instanceof TreeBin ? ((TreeBin) nodeTabAt).first : null; node != null; node = node.next) {
                                j--;
                            }
                            setTabAt(nodeArrHelpTransfer, i, null);
                            i++;
                        }
                    }
                }
            }
        }
        if (j != 0) {
            addCount(j, -1);
        }
    }

    @Override
    public Set<K> keySet() {
        KeySetView<K, V> keySetView = this.keySet;
        if (keySetView != null) {
            return keySetView;
        }
        KeySetView<K, V> keySetView2 = new KeySetView<>(this, null);
        this.keySet = keySetView2;
        return keySetView2;
    }

    @Override
    public Collection<V> values() {
        ValuesView<K, V> valuesView = this.values;
        if (valuesView != null) {
            return valuesView;
        }
        ValuesView<K, V> valuesView2 = new ValuesView<>(this);
        this.values = valuesView2;
        return valuesView2;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        EntrySetView<K, V> entrySetView = this.entrySet;
        if (entrySetView != null) {
            return entrySetView;
        }
        EntrySetView<K, V> entrySetView2 = new EntrySetView<>(this);
        this.entrySet = entrySetView2;
        return entrySetView2;
    }

    @Override
    public int hashCode() {
        Node<K, V>[] nodeArr = this.table;
        int iHashCode = 0;
        if (nodeArr != null) {
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    break;
                }
                iHashCode += nodeAdvance.val.hashCode() ^ nodeAdvance.key.hashCode();
            }
        }
        return iHashCode;
    }

    @Override
    public String toString() {
        int length;
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null) {
            length = nodeArr.length;
        } else {
            length = 0;
        }
        Traverser traverser = new Traverser(nodeArr, length, 0, length);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K, V> nodeAdvance = traverser.advance();
        if (nodeAdvance != null) {
            while (true) {
                Object obj = nodeAdvance.key;
                Object obj2 = nodeAdvance.val;
                if (obj == this) {
                    obj = "(this Map)";
                }
                sb.append(obj);
                sb.append('=');
                if (obj2 == this) {
                    obj2 = "(this Map)";
                }
                sb.append(obj2);
                nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    break;
                }
                sb.append(',');
                sb.append(' ');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        int length;
        V value;
        V v;
        if (obj != this) {
            if (!(obj instanceof Map)) {
                return false;
            }
            Map map = (Map) obj;
            Node<K, V>[] nodeArr = this.table;
            if (nodeArr != null) {
                length = nodeArr.length;
            } else {
                length = 0;
            }
            Traverser traverser = new Traverser(nodeArr, length, 0, length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance != null) {
                    V v2 = nodeAdvance.val;
                    Object obj2 = map.get(nodeAdvance.key);
                    if (obj2 == null || (obj2 != v2 && !obj2.equals(v2))) {
                        break;
                    }
                } else {
                    for (Map.Entry<K, V> entry : map.entrySet()) {
                        K key = entry.getKey();
                        if (key == null || (value = entry.getValue()) == null || (v = get(key)) == null || (value != v && !value.equals(v))) {
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

        Segment(float f) {
            this.loadFactor = f;
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        int i = 0;
        int i2 = 1;
        while (i2 < 16) {
            i++;
            i2 <<= 1;
        }
        int i3 = 32 - i;
        int i4 = i2 - 1;
        Segment[] segmentArr = new Segment[16];
        for (int i5 = 0; i5 < segmentArr.length; i5++) {
            segmentArr[i5] = new Segment(LOAD_FACTOR);
        }
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("segments", segmentArr);
        putFieldPutFields.put("segmentShift", i3);
        putFieldPutFields.put("segmentMask", i4);
        objectOutputStream.writeFields();
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null) {
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    break;
                }
                objectOutputStream.writeObject(nodeAdvance.key);
                objectOutputStream.writeObject(nodeAdvance.val);
            }
        }
        objectOutputStream.writeObject(null);
        objectOutputStream.writeObject(null);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        long j;
        int iTableSizeFor;
        boolean z;
        K k;
        long j2;
        this.sizeCtl = -1;
        objectInputStream.defaultReadObject();
        long j3 = 0;
        long j4 = 0;
        Node<K, V> node = null;
        while (true) {
            Object object = objectInputStream.readObject();
            Object object2 = objectInputStream.readObject();
            j = 1;
            if (object == null || object2 == null) {
                break;
            }
            j4++;
            node = new Node<>(spread(object.hashCode()), object, object2, node);
        }
        if (j4 == 0) {
            this.sizeCtl = 0;
            return;
        }
        boolean z2 = true;
        if (j4 >= 536870912) {
            iTableSizeFor = MAXIMUM_CAPACITY;
        } else {
            int i = (int) j4;
            iTableSizeFor = tableSizeFor(i + (i >>> 1) + 1);
        }
        Node<K, V>[] nodeArr = new Node[iTableSizeFor];
        int i2 = iTableSizeFor - 1;
        while (node != null) {
            Node<K, V> node2 = node.next;
            int i3 = node.hash;
            int i4 = i3 & i2;
            Node<K, V> nodeTabAt = tabAt(nodeArr, i4);
            if (nodeTabAt != null) {
                K k2 = node.key;
                if (nodeTabAt.hash < 0) {
                    if (((TreeBin) nodeTabAt).putTreeVal(i3, k2, node.val) == null) {
                        j3 += j;
                    }
                } else {
                    int i5 = 0;
                    for (Node<K, V> node3 = nodeTabAt; node3 != null; node3 = node3.next) {
                        if (node3.hash != i3 || ((k = node3.key) != k2 && (k == null || !k2.equals(k)))) {
                            i5++;
                        } else {
                            z = false;
                            break;
                        }
                    }
                    z = true;
                    if (z && i5 >= 8) {
                        j3++;
                        node.next = nodeTabAt;
                        Node<K, V> node4 = node;
                        TreeNode<K, V> treeNode = null;
                        TreeNode<K, V> treeNode2 = null;
                        while (node4 != null) {
                            long j5 = j3;
                            TreeNode<K, V> treeNode3 = new TreeNode<>(node4.hash, node4.key, node4.val, null, null);
                            treeNode3.prev = treeNode;
                            if (treeNode != null) {
                                treeNode.next = treeNode3;
                            } else {
                                treeNode2 = treeNode3;
                            }
                            node4 = node4.next;
                            treeNode = treeNode3;
                            j3 = j5;
                        }
                        setTabAt(nodeArr, i4, new TreeBin(treeNode2));
                    }
                }
                z = false;
            } else {
                z = z2;
            }
            if (z) {
                j2 = 1;
                j3++;
                node.next = nodeTabAt;
                setTabAt(nodeArr, i4, node);
            } else {
                j2 = 1;
            }
            j = j2;
            node = node2;
            z2 = true;
        }
        this.table = nodeArr;
        this.sizeCtl = iTableSizeFor - (iTableSizeFor >>> 2);
        this.baseCount = j3;
    }

    @Override
    public V putIfAbsent(K k, V v) {
        return putVal(k, v, true);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        if (obj != null) {
            return (obj2 == null || replaceNode(obj, null, obj2) == null) ? false : true;
        }
        throw new NullPointerException();
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        if (k == null || v == null || v2 == null) {
            throw new NullPointerException();
        }
        return replaceNode(k, v2, v) != null;
    }

    @Override
    public V replace(K k, V v) {
        if (k == null || v == null) {
            throw new NullPointerException();
        }
        return replaceNode(k, v, null);
    }

    @Override
    public V getOrDefault(Object obj, V v) {
        V v2 = get(obj);
        return v2 == null ? v : v2;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
        if (biConsumer == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null) {
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance != null) {
                    biConsumer.accept(nodeAdvance.key, nodeAdvance.val);
                } else {
                    return;
                }
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] nodeArr = this.table;
        if (nodeArr != null) {
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance != null) {
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
                } else {
                    return;
                }
            }
        }
    }

    boolean removeEntryIf(Predicate<? super Map.Entry<K, V>> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] nodeArr = this.table;
        boolean z = false;
        if (nodeArr != null) {
            Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
            while (true) {
                Node<K, V> nodeAdvance = traverser.advance();
                if (nodeAdvance == null) {
                    break;
                }
                K k = nodeAdvance.key;
                V v = nodeAdvance.val;
                if (predicate.test(new AbstractMap.SimpleImmutableEntry(k, v)) && replaceNode(k, null, v) != null) {
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
        Node<K, V>[] nodeArr = this.table;
        boolean z = false;
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
    public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
        TreeNode<K, V> treeNodeFindTreeNode;
        V vApply;
        K k2;
        if (k == null || function == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        Node<K, V>[] nodeArrInitTable = this.table;
        int i = 0;
        V vApply2 = null;
        while (true) {
            if (nodeArrInitTable != null) {
                int length = nodeArrInitTable.length;
                if (length != 0) {
                    int i2 = (length - 1) & iSpread;
                    Node<K, V> nodeTabAt = tabAt(nodeArrInitTable, i2);
                    boolean z = true;
                    if (nodeTabAt == null) {
                        ReservationNode reservationNode = new ReservationNode();
                        synchronized (reservationNode) {
                            if (casTabAt(nodeArrInitTable, i2, null, reservationNode)) {
                                try {
                                    vApply2 = function.apply(k);
                                    setTabAt(nodeArrInitTable, i2, vApply2 != null ? new Node(iSpread, k, vApply2, null) : null);
                                    i = 1;
                                } finally {
                                }
                            }
                        }
                        if (i != 0) {
                            break;
                        }
                    } else {
                        int i3 = nodeTabAt.hash;
                        if (i3 == -1) {
                            nodeArrInitTable = helpTransfer(nodeArrInitTable, nodeTabAt);
                        } else {
                            synchronized (nodeTabAt) {
                                if (tabAt(nodeArrInitTable, i2) == nodeTabAt) {
                                    if (i3 >= 0) {
                                        Node<K, V> node = nodeTabAt;
                                        int i4 = 1;
                                        while (true) {
                                            if (node.hash == iSpread && ((k2 = node.key) == k || (k2 != null && k.equals(k2)))) {
                                                break;
                                            }
                                            Node<K, V> node2 = node.next;
                                            if (node2 == null) {
                                                vApply = function.apply(k);
                                                if (vApply == null) {
                                                    z = false;
                                                } else {
                                                    if (node.next != null) {
                                                        throw new IllegalStateException("Recursive update");
                                                    }
                                                    node.next = new Node<>(iSpread, k, vApply, null);
                                                }
                                            } else {
                                                i4++;
                                                node = node2;
                                            }
                                        }
                                        z = false;
                                        vApply = node.val;
                                        i = i4;
                                        vApply2 = vApply;
                                    } else if (nodeTabAt instanceof TreeBin) {
                                        i = 2;
                                        TreeBin treeBin = (TreeBin) nodeTabAt;
                                        TreeNode<K, V> treeNode = treeBin.root;
                                        if (treeNode == null || (treeNodeFindTreeNode = treeNode.findTreeNode(iSpread, k, null)) == null) {
                                            V vApply3 = function.apply(k);
                                            if (vApply3 != null) {
                                                treeBin.putTreeVal(iSpread, k, vApply3);
                                            } else {
                                                z = false;
                                            }
                                            vApply2 = vApply3;
                                        } else {
                                            vApply2 = treeNodeFindTreeNode.val;
                                            z = false;
                                        }
                                    } else if (nodeTabAt instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                                z = false;
                            }
                            if (i != 0) {
                                if (i >= 8) {
                                    treeifyBin(nodeArrInitTable, i2);
                                }
                                if (!z) {
                                    return vApply2;
                                }
                            }
                        }
                    }
                }
            }
            nodeArrInitTable = initTable();
        }
        if (vApply2 != null) {
            addCount(1L, i);
        }
        return vApply2;
    }

    @Override
    public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        TreeNode<K, V> treeNodeFindTreeNode;
        K k2;
        if (k == null || biFunction == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        Node<K, V>[] nodeArrInitTable = this.table;
        int i = 0;
        int i2 = 0;
        V vApply = null;
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
                                    Node<K, V> node = null;
                                    int i5 = 1;
                                    Node<K, V> node2 = nodeTabAt;
                                    while (true) {
                                        if (node2.hash == iSpread && ((k2 = node2.key) == k || (k2 != null && k.equals(k2)))) {
                                            break;
                                        }
                                        Node<K, V> node3 = node2.next;
                                        if (node3 == null) {
                                            break;
                                        }
                                        i5++;
                                        node = node2;
                                        node2 = node3;
                                    }
                                    vApply = biFunction.apply(k, node2.val);
                                    if (vApply != null) {
                                        node2.val = vApply;
                                    } else {
                                        Node<K, V> node4 = node2.next;
                                        if (node != null) {
                                            node.next = node4;
                                        } else {
                                            setTabAt(nodeArrInitTable, i3, node4);
                                        }
                                        i = -1;
                                    }
                                    i2 = i5;
                                } else if (nodeTabAt instanceof TreeBin) {
                                    i2 = 2;
                                    TreeBin treeBin = (TreeBin) nodeTabAt;
                                    TreeNode<K, V> treeNode = treeBin.root;
                                    if (treeNode != null && (treeNodeFindTreeNode = treeNode.findTreeNode(iSpread, k, null)) != null) {
                                        vApply = biFunction.apply(k, treeNodeFindTreeNode.val);
                                        if (vApply != null) {
                                            treeNodeFindTreeNode.val = vApply;
                                        } else {
                                            if (treeBin.removeTreeNode(treeNodeFindTreeNode)) {
                                                setTabAt(nodeArrInitTable, i3, untreeify(treeBin.first));
                                            }
                                            i = -1;
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
        if (i != 0) {
            addCount(i, i2);
        }
        return vApply;
    }

    @Override
    public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
        Node node;
        V vApply;
        K k2;
        if (k == null || biFunction == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        Node<K, V>[] nodeArrInitTable = this.table;
        int i = 0;
        int i2 = 0;
        V vApply2 = null;
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
                                try {
                                    vApply2 = biFunction.apply(k, null);
                                    if (vApply2 != null) {
                                        node = new Node(iSpread, k, vApply2, null);
                                        i2 = 1;
                                    } else {
                                        node = null;
                                    }
                                    setTabAt(nodeArrInitTable, i3, node);
                                    i = 1;
                                } finally {
                                }
                            }
                        }
                        if (i != 0) {
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
                                        Node<K, V> node2 = null;
                                        Node<K, V> node3 = nodeTabAt;
                                        int i5 = 1;
                                        while (true) {
                                            if (node3.hash == iSpread && ((k2 = node3.key) == k || (k2 != null && k.equals(k2)))) {
                                                break;
                                            }
                                            Node<K, V> node4 = node3.next;
                                            if (node4 == null) {
                                                V vApply3 = biFunction.apply(k, null);
                                                if (vApply3 != null) {
                                                    if (node3.next != null) {
                                                        throw new IllegalStateException("Recursive update");
                                                    }
                                                    node3.next = new Node<>(iSpread, k, vApply3, null);
                                                    i2 = 1;
                                                }
                                                vApply = vApply3;
                                            } else {
                                                i5++;
                                                node2 = node3;
                                                node3 = node4;
                                            }
                                        }
                                        vApply = biFunction.apply(k, node3.val);
                                        if (vApply != null) {
                                            node3.val = vApply;
                                        } else {
                                            Node<K, V> node5 = node3.next;
                                            if (node2 != null) {
                                                node2.next = node5;
                                            } else {
                                                setTabAt(nodeArrInitTable, i3, node5);
                                            }
                                            i2 = -1;
                                        }
                                        i = i5;
                                        vApply2 = vApply;
                                    } else if (nodeTabAt instanceof TreeBin) {
                                        TreeBin treeBin = (TreeBin) nodeTabAt;
                                        TreeNode<K, V> treeNode = treeBin.root;
                                        TreeNode<K, V> treeNodeFindTreeNode = treeNode != null ? treeNode.findTreeNode(iSpread, k, null) : null;
                                        V vApply4 = biFunction.apply(k, treeNodeFindTreeNode == null ? null : treeNodeFindTreeNode.val);
                                        if (vApply4 != null) {
                                            if (treeNodeFindTreeNode != null) {
                                                treeNodeFindTreeNode.val = vApply4;
                                            } else {
                                                treeBin.putTreeVal(iSpread, k, vApply4);
                                                i2 = 1;
                                            }
                                        } else if (treeNodeFindTreeNode != null) {
                                            if (treeBin.removeTreeNode(treeNodeFindTreeNode)) {
                                                setTabAt(nodeArrInitTable, i3, untreeify(treeBin.first));
                                            }
                                            i2 = -1;
                                        }
                                        i = 1;
                                        vApply2 = vApply4;
                                    } else if (nodeTabAt instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                            }
                            if (i != 0) {
                                if (i >= 8) {
                                    treeifyBin(nodeArrInitTable, i3);
                                }
                            }
                        }
                    }
                }
            }
            nodeArrInitTable = initTable();
        }
        if (i2 != 0) {
            addCount(i2, i);
        }
        return vApply2;
    }

    @Override
    public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        int i;
        V vApply;
        K k2;
        V v2 = (Object) v;
        if (k == null || v2 == null || biFunction == null) {
            throw new NullPointerException();
        }
        int iSpread = spread(k.hashCode());
        Node<K, V>[] nodeArrInitTable = this.table;
        int i2 = 0;
        int i3 = 0;
        V v3 = null;
        while (true) {
            if (nodeArrInitTable != null) {
                int length = nodeArrInitTable.length;
                if (length != 0) {
                    int i4 = (length - 1) & iSpread;
                    Node<K, V> nodeTabAt = tabAt(nodeArrInitTable, i4);
                    i = 1;
                    if (nodeTabAt != null) {
                        int i5 = nodeTabAt.hash;
                        if (i5 == -1) {
                            nodeArrInitTable = helpTransfer(nodeArrInitTable, nodeTabAt);
                        } else {
                            synchronized (nodeTabAt) {
                                if (tabAt(nodeArrInitTable, i4) == nodeTabAt) {
                                    if (i5 >= 0) {
                                        Node<K, V> node = null;
                                        Node<K, V> node2 = nodeTabAt;
                                        int i6 = 1;
                                        while (true) {
                                            if (node2.hash == iSpread && ((k2 = node2.key) == k || (k2 != null && k.equals(k2)))) {
                                                break;
                                            }
                                            Node<K, V> node3 = node2.next;
                                            if (node3 == null) {
                                                node2.next = new Node<>(iSpread, k, v2, null);
                                                i3 = 1;
                                                vApply = (V) v2;
                                                break;
                                            }
                                            i6++;
                                            node = node2;
                                            node2 = node3;
                                        }
                                        vApply = biFunction.apply(node2.val, v2);
                                        if (vApply != null) {
                                            node2.val = vApply;
                                        } else {
                                            Node<K, V> node4 = node2.next;
                                            if (node != null) {
                                                node.next = node4;
                                            } else {
                                                setTabAt(nodeArrInitTable, i4, node4);
                                            }
                                            i3 = -1;
                                        }
                                        i2 = i6;
                                    } else if (nodeTabAt instanceof TreeBin) {
                                        i2 = 2;
                                        TreeBin treeBin = (TreeBin) nodeTabAt;
                                        TreeNode<K, V> treeNode = treeBin.root;
                                        TreeNode<K, V> treeNodeFindTreeNode = treeNode == null ? null : treeNode.findTreeNode(iSpread, k, null);
                                        V vApply2 = treeNodeFindTreeNode == null ? (V) v2 : biFunction.apply(treeNodeFindTreeNode.val, v2);
                                        if (vApply2 != null) {
                                            if (treeNodeFindTreeNode != null) {
                                                treeNodeFindTreeNode.val = vApply2;
                                            } else {
                                                treeBin.putTreeVal(iSpread, k, vApply2);
                                                i3 = 1;
                                            }
                                        } else if (treeNodeFindTreeNode != null) {
                                            if (treeBin.removeTreeNode(treeNodeFindTreeNode)) {
                                                setTabAt(nodeArrInitTable, i4, untreeify(treeBin.first));
                                            }
                                            i3 = -1;
                                        }
                                        vApply = vApply2;
                                    } else if (nodeTabAt instanceof ReservationNode) {
                                        throw new IllegalStateException("Recursive update");
                                    }
                                }
                                vApply = v3;
                            }
                            if (i2 != 0) {
                                if (i2 >= 8) {
                                    treeifyBin(nodeArrInitTable, i4);
                                }
                                v2 = vApply;
                                i = i3;
                            } else {
                                v3 = vApply;
                            }
                        }
                    } else if (casTabAt(nodeArrInitTable, i4, null, new Node(iSpread, k, v2, null))) {
                        break;
                    }
                }
            }
            nodeArrInitTable = initTable();
        }
        if (i != 0) {
            addCount(i, i2);
        }
        return (V) v2;
    }

    public boolean contains(Object obj) {
        return containsValue(obj);
    }

    public Enumeration<K> keys() {
        Node<K, V>[] nodeArr = this.table;
        int length = nodeArr == null ? 0 : nodeArr.length;
        return new KeyIterator(nodeArr, length, 0, length, this);
    }

    public Enumeration<V> elements() {
        Node<K, V>[] nodeArr = this.table;
        int length = nodeArr == null ? 0 : nodeArr.length;
        return new ValueIterator(nodeArr, length, 0, length, this);
    }

    public long mappingCount() {
        long jSumCount = sumCount();
        if (jSumCount < 0) {
            return 0L;
        }
        return jSumCount;
    }

    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView<>(new ConcurrentHashMap(), Boolean.TRUE);
    }

    public static <K> KeySetView<K, Boolean> newKeySet(int i) {
        return new KeySetView<>(new ConcurrentHashMap(i), Boolean.TRUE);
    }

    public KeySetView<K, V> keySet(V v) {
        if (v == null) {
            throw new NullPointerException();
        }
        return new KeySetView<>(this, v);
    }

    static final class ForwardingNode<K, V> extends Node<K, V> {
        final Node<K, V>[] nextTable;

        ForwardingNode(Node<K, V>[] nodeArr) {
            super(-1, null, null, null);
            this.nextTable = nodeArr;
        }

        @Override
        Node<K, V> find(int i, Object obj) {
            int length;
            Node<K, V> nodeTabAt;
            K k;
            Node<K, V>[] nodeArr = this.nextTable;
            while (obj != null && nodeArr != null && (length = nodeArr.length) != 0 && (nodeTabAt = ConcurrentHashMap.tabAt(nodeArr, (length - 1) & i)) != null) {
                do {
                    int i2 = nodeTabAt.hash;
                    if (i2 == i && ((k = nodeTabAt.key) == obj || (k != null && obj.equals(k)))) {
                        return nodeTabAt;
                    }
                    if (i2 < 0) {
                        if (nodeTabAt instanceof ForwardingNode) {
                            nodeArr = ((ForwardingNode) nodeTabAt).nextTable;
                        } else {
                            return nodeTabAt.find(i, obj);
                        }
                    } else {
                        nodeTabAt = nodeTabAt.next;
                    }
                } while (nodeTabAt != null);
                return null;
            }
            return null;
        }
    }

    static final class ReservationNode<K, V> extends Node<K, V> {
        ReservationNode() {
            super(-3, null, null, null);
        }

        @Override
        Node<K, V> find(int i, Object obj) {
            return null;
        }
    }

    static final int resizeStamp(int i) {
        return Integer.numberOfLeadingZeros(i) | NumericShaper.MYANMAR;
    }

    private final Node<K, V>[] initTable() {
        int i;
        while (true) {
            Node<K, V>[] nodeArr = this.table;
            if (nodeArr != null && nodeArr.length != 0) {
                return nodeArr;
            }
            int i2 = this.sizeCtl;
            if (i2 < 0) {
                Thread.yield();
            } else if (U.compareAndSwapInt(this, SIZECTL, i2, -1)) {
                try {
                    Node<K, V>[] nodeArr2 = this.table;
                    if (nodeArr2 == null || nodeArr2.length == 0) {
                        if (i2 <= 0) {
                            i = 16;
                        } else {
                            i = i2;
                        }
                        Node<K, V>[] nodeArr3 = new Node[i];
                        this.table = nodeArr3;
                        i2 = i - (i >>> 2);
                        nodeArr2 = nodeArr3;
                    }
                    this.sizeCtl = i2;
                    return nodeArr2;
                } catch (Throwable th) {
                    this.sizeCtl = i2;
                    throw th;
                }
            }
        }
    }

    private final void addCount(long j, int i) {
        boolean zCompareAndSwapLong;
        int length;
        CounterCell counterCell;
        long jSumCount;
        Node<K, V>[] nodeArr;
        int length2;
        Node<K, V>[] nodeArr2;
        CounterCell[] counterCellArr = this.counterCells;
        if (counterCellArr == null) {
            Unsafe unsafe = U;
            long j2 = BASECOUNT;
            long j3 = this.baseCount;
            jSumCount = j3 + j;
            if (!unsafe.compareAndSwapLong(this, j2, j3, jSumCount)) {
                if (counterCellArr != null && (length = counterCellArr.length - 1) >= 0 && (counterCell = counterCellArr[length & ThreadLocalRandom.getProbe()]) != null) {
                    Unsafe unsafe2 = U;
                    long j4 = CELLVALUE;
                    long j5 = counterCell.value;
                    zCompareAndSwapLong = unsafe2.compareAndSwapLong(counterCell, j4, j5, j5 + j);
                    if (zCompareAndSwapLong) {
                        if (i <= 1) {
                            return;
                        } else {
                            jSumCount = sumCount();
                        }
                    }
                } else {
                    zCompareAndSwapLong = true;
                }
                fullAddCount(j, zCompareAndSwapLong);
                return;
            }
        }
        if (i < 0) {
            return;
        }
        while (true) {
            int i2 = this.sizeCtl;
            if (jSumCount >= i2 && (nodeArr = this.table) != null && (length2 = nodeArr.length) < MAXIMUM_CAPACITY) {
                int iResizeStamp = resizeStamp(length2);
                if (i2 < 0) {
                    if ((i2 >>> 16) == iResizeStamp && i2 != iResizeStamp + 1 && i2 != iResizeStamp + MAX_RESIZERS && (nodeArr2 = this.nextTable) != null && this.transferIndex > 0) {
                        if (U.compareAndSwapInt(this, SIZECTL, i2, i2 + 1)) {
                            transfer(nodeArr, nodeArr2);
                        }
                    } else {
                        return;
                    }
                } else if (U.compareAndSwapInt(this, SIZECTL, i2, (iResizeStamp << 16) + 2)) {
                    transfer(nodeArr, null);
                }
                jSumCount = sumCount();
            } else {
                return;
            }
        }
    }

    final Node<K, V>[] helpTransfer(Node<K, V>[] nodeArr, Node<K, V> node) {
        Node<K, V>[] nodeArr2;
        int i;
        if (nodeArr != null && (node instanceof ForwardingNode) && (nodeArr2 = ((ForwardingNode) node).nextTable) != null) {
            int iResizeStamp = resizeStamp(nodeArr.length);
            while (true) {
                if (nodeArr2 != this.nextTable || this.table != nodeArr || (i = this.sizeCtl) >= 0 || (i >>> 16) != iResizeStamp || i == iResizeStamp + 1 || i == MAX_RESIZERS + iResizeStamp || this.transferIndex <= 0) {
                    break;
                }
                if (U.compareAndSwapInt(this, SIZECTL, i, i + 1)) {
                    transfer(nodeArr, nodeArr2);
                    break;
                }
            }
            return nodeArr2;
        }
        return this.table;
    }

    private final void tryPresize(int i) {
        int iTableSizeFor;
        int length;
        if (i < 536870912) {
            iTableSizeFor = tableSizeFor(i + (i >>> 1) + 1);
        } else {
            iTableSizeFor = MAXIMUM_CAPACITY;
        }
        while (true) {
            int i2 = this.sizeCtl;
            if (i2 >= 0) {
                Node<K, V>[] nodeArr = this.table;
                if (nodeArr == null || (length = nodeArr.length) == 0) {
                    int i3 = i2 > iTableSizeFor ? i2 : iTableSizeFor;
                    if (U.compareAndSwapInt(this, SIZECTL, i2, -1)) {
                        try {
                            if (this.table == nodeArr) {
                                this.table = new Node[i3];
                                i2 = i3 - (i3 >>> 2);
                            }
                        } finally {
                            this.sizeCtl = i2;
                        }
                    } else {
                        continue;
                    }
                } else if (iTableSizeFor > i2 && length < MAXIMUM_CAPACITY) {
                    if (nodeArr == this.table) {
                        if (U.compareAndSwapInt(this, SIZECTL, i2, (resizeStamp(length) << 16) + 2)) {
                            transfer(nodeArr, null);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private final void transfer(Node<K, V>[] nodeArr, Node<K, V>[] nodeArr2) {
        Node<K, V>[] nodeArr3;
        int i;
        int i2;
        ForwardingNode forwardingNode;
        ConcurrentHashMap<K, V> concurrentHashMap;
        boolean z;
        char c;
        int i3;
        Node<K, V> node;
        int i4;
        ConcurrentHashMap<K, V> concurrentHashMap2 = this;
        Node<K, V>[] nodeArr4 = nodeArr;
        int length = nodeArr4.length;
        boolean z2 = true;
        int i5 = NCPU > 1 ? (length >>> 3) / NCPU : length;
        char c2 = 16;
        int i6 = i5 < 16 ? 16 : i5;
        if (nodeArr2 == null) {
            try {
                Node<K, V>[] nodeArr5 = new Node[length << 1];
                concurrentHashMap2.nextTable = nodeArr5;
                concurrentHashMap2.transferIndex = length;
                nodeArr3 = nodeArr5;
            } catch (Throwable th) {
                concurrentHashMap2.sizeCtl = Integer.MAX_VALUE;
                return;
            }
        } else {
            nodeArr3 = nodeArr2;
        }
        int length2 = nodeArr3.length;
        ForwardingNode forwardingNode2 = new ForwardingNode(nodeArr3);
        boolean zCasTabAt = true;
        int i7 = 0;
        int i8 = 0;
        boolean z3 = false;
        while (true) {
            if (zCasTabAt) {
                int i9 = i8 - 1;
                if (i9 >= i7 || z3) {
                    i7 = i7;
                    i8 = i9;
                    zCasTabAt = false;
                } else {
                    int i10 = concurrentHashMap2.transferIndex;
                    if (i10 <= 0) {
                        i8 = -1;
                    } else {
                        Unsafe unsafe = U;
                        long j = TRANSFERINDEX;
                        int i11 = i10 > i6 ? i10 - i6 : 0;
                        int i12 = i7;
                        if (unsafe.compareAndSwapInt(concurrentHashMap2, j, i10, i11)) {
                            i8 = i10 - 1;
                            i7 = i11;
                        } else {
                            i7 = i12;
                            i8 = i9;
                        }
                    }
                    zCasTabAt = false;
                }
            } else {
                int i13 = i7;
                Node<K, V> node2 = null;
                if (i8 < 0 || i8 >= length || (i3 = i8 + length) >= length2) {
                    i = i6;
                    i2 = length2;
                    forwardingNode = forwardingNode2;
                    if (z3) {
                        this.nextTable = null;
                        this.table = nodeArr3;
                        this.sizeCtl = (length << 1) - (length >>> 1);
                        return;
                    }
                    concurrentHashMap = this;
                    z = true;
                    Unsafe unsafe2 = U;
                    long j2 = SIZECTL;
                    int i14 = concurrentHashMap.sizeCtl;
                    int i15 = i8;
                    if (unsafe2.compareAndSwapInt(concurrentHashMap, j2, i14, i14 - 1)) {
                        c = 16;
                        if (i14 - 2 != (resizeStamp(length) << 16)) {
                            return;
                        }
                        i8 = length;
                        zCasTabAt = true;
                        z3 = true;
                    } else {
                        c = 16;
                        i8 = i15;
                    }
                } else {
                    Node<K, V> nodeTabAt = tabAt(nodeArr4, i8);
                    if (nodeTabAt == null) {
                        zCasTabAt = casTabAt(nodeArr4, i8, null, forwardingNode2);
                        c = c2;
                        i = i6;
                        i2 = length2;
                        z = z2;
                        concurrentHashMap = concurrentHashMap2;
                    } else {
                        int i16 = nodeTabAt.hash;
                        if (i16 == -1) {
                            zCasTabAt = z2;
                            c = c2;
                            i = i6;
                            i2 = length2;
                            concurrentHashMap = concurrentHashMap2;
                            z = zCasTabAt;
                        } else {
                            synchronized (nodeTabAt) {
                                if (tabAt(nodeArr4, i8) == nodeTabAt) {
                                    if (i16 >= 0) {
                                        int i17 = i16 & length;
                                        Node<K, V> node3 = nodeTabAt;
                                        for (Node<K, V> node4 = nodeTabAt.next; node4 != null; node4 = node4.next) {
                                            int i18 = node4.hash & length;
                                            if (i18 != i17) {
                                                node3 = node4;
                                                i17 = i18;
                                            }
                                        }
                                        if (i17 == 0) {
                                            node = null;
                                            node2 = node3;
                                        } else {
                                            node = node3;
                                        }
                                        Node<K, V> node5 = node;
                                        Node<K, V> node6 = nodeTabAt;
                                        while (node6 != node3) {
                                            int i19 = node6.hash;
                                            K k = node6.key;
                                            int i20 = i6;
                                            V v = node6.val;
                                            if ((i19 & length) == 0) {
                                                i4 = length2;
                                                node2 = new Node<>(i19, k, v, node2);
                                            } else {
                                                i4 = length2;
                                                node5 = new Node<>(i19, k, v, node5);
                                            }
                                            node6 = node6.next;
                                            i6 = i20;
                                            length2 = i4;
                                        }
                                        i = i6;
                                        i2 = length2;
                                        setTabAt(nodeArr3, i8, node2);
                                        setTabAt(nodeArr3, i3, node5);
                                        setTabAt(nodeArr4, i8, forwardingNode2);
                                        forwardingNode = forwardingNode2;
                                    } else {
                                        i = i6;
                                        i2 = length2;
                                        if (nodeTabAt instanceof TreeBin) {
                                            TreeBin treeBin = (TreeBin) nodeTabAt;
                                            Node node7 = treeBin.first;
                                            TreeNode<K, V> treeNode = null;
                                            TreeNode<K, V> treeNode2 = null;
                                            TreeNode<K, V> treeNode3 = null;
                                            TreeNode<K, V> treeNode4 = null;
                                            int i21 = 0;
                                            int i22 = 0;
                                            while (node7 != null) {
                                                TreeBin treeBin2 = treeBin;
                                                int i23 = node7.hash;
                                                ForwardingNode forwardingNode3 = forwardingNode2;
                                                TreeNode<K, V> treeNode5 = new TreeNode<>(i23, node7.key, node7.val, null, null);
                                                if ((i23 & length) == 0) {
                                                    treeNode5.prev = treeNode3;
                                                    if (treeNode3 == null) {
                                                        treeNode = treeNode5;
                                                    } else {
                                                        treeNode3.next = treeNode5;
                                                    }
                                                    i21++;
                                                    treeNode3 = treeNode5;
                                                } else {
                                                    treeNode5.prev = treeNode4;
                                                    if (treeNode4 == null) {
                                                        treeNode2 = treeNode5;
                                                    } else {
                                                        treeNode4.next = treeNode5;
                                                    }
                                                    i22++;
                                                    treeNode4 = treeNode5;
                                                }
                                                node7 = node7.next;
                                                treeBin = treeBin2;
                                                forwardingNode2 = forwardingNode3;
                                            }
                                            TreeBin treeBin3 = treeBin;
                                            ForwardingNode forwardingNode4 = forwardingNode2;
                                            Node nodeUntreeify = i21 <= 6 ? untreeify(treeNode) : i22 != 0 ? new TreeBin(treeNode) : treeBin3;
                                            Node nodeUntreeify2 = i22 <= 6 ? untreeify(treeNode2) : i21 != 0 ? new TreeBin(treeNode2) : treeBin3;
                                            setTabAt(nodeArr3, i8, nodeUntreeify);
                                            setTabAt(nodeArr3, i3, nodeUntreeify2);
                                            forwardingNode = forwardingNode4;
                                            nodeArr4 = nodeArr;
                                            setTabAt(nodeArr4, i8, forwardingNode);
                                        }
                                    }
                                    zCasTabAt = true;
                                } else {
                                    i = i6;
                                    i2 = length2;
                                }
                                forwardingNode = forwardingNode2;
                            }
                            c = 16;
                            concurrentHashMap = this;
                            z = true;
                        }
                    }
                    forwardingNode = forwardingNode2;
                }
                forwardingNode2 = forwardingNode;
                concurrentHashMap2 = concurrentHashMap;
                z2 = z;
                i7 = i13;
                i6 = i;
                length2 = i2;
                c2 = c;
            }
        }
    }

    static final class CounterCell {
        volatile long value;

        CounterCell(long j) {
            this.value = j;
        }
    }

    final long sumCount() {
        CounterCell[] counterCellArr = this.counterCells;
        long j = this.baseCount;
        if (counterCellArr != null) {
            for (CounterCell counterCell : counterCellArr) {
                if (counterCell != null) {
                    j += counterCell.value;
                }
            }
        }
        return j;
    }

    private final void fullAddCount(long j, boolean z) {
        int probe;
        boolean z2;
        boolean z3;
        int length;
        boolean z4;
        int length2;
        int probe2 = ThreadLocalRandom.getProbe();
        if (probe2 == 0) {
            ThreadLocalRandom.localInit();
            probe = ThreadLocalRandom.getProbe();
            z2 = true;
        } else {
            probe = probe2;
            z2 = z;
        }
        boolean z5 = z2;
        int iAdvanceProbe = probe;
        while (true) {
            boolean z6 = false;
            while (true) {
                CounterCell[] counterCellArr = this.counterCells;
                if (counterCellArr != null && (length = counterCellArr.length) > 0) {
                    CounterCell counterCell = counterCellArr[(length - 1) & iAdvanceProbe];
                    if (counterCell != null) {
                        if (z5) {
                            Unsafe unsafe = U;
                            long j2 = CELLVALUE;
                            long j3 = counterCell.value;
                            if (unsafe.compareAndSwapLong(counterCell, j2, j3, j3 + j)) {
                                return;
                            }
                            if (this.counterCells == counterCellArr && length < NCPU) {
                                if (!z6) {
                                    z6 = true;
                                } else if (this.cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                                    try {
                                        break;
                                    } finally {
                                    }
                                }
                            }
                        } else {
                            z5 = true;
                        }
                        iAdvanceProbe = ThreadLocalRandom.advanceProbe(iAdvanceProbe);
                    } else if (this.cellsBusy == 0) {
                        CounterCell counterCell2 = new CounterCell(j);
                        if (this.cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            try {
                                CounterCell[] counterCellArr2 = this.counterCells;
                                if (counterCellArr2 == null || (length2 = counterCellArr2.length) <= 0) {
                                    z4 = false;
                                    if (!z4) {
                                        return;
                                    }
                                } else {
                                    int i = (length2 - 1) & iAdvanceProbe;
                                    if (counterCellArr2[i] == null) {
                                        counterCellArr2[i] = counterCell2;
                                        z4 = true;
                                    }
                                    if (!z4) {
                                    }
                                }
                            } finally {
                            }
                        }
                    }
                    z6 = false;
                    iAdvanceProbe = ThreadLocalRandom.advanceProbe(iAdvanceProbe);
                } else if (this.cellsBusy == 0 && this.counterCells == counterCellArr && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        if (this.counterCells == counterCellArr) {
                            CounterCell[] counterCellArr3 = new CounterCell[2];
                            counterCellArr3[iAdvanceProbe & 1] = new CounterCell(j);
                            this.counterCells = counterCellArr3;
                            z3 = true;
                        } else {
                            z3 = false;
                        }
                        if (z3) {
                            return;
                        }
                    } finally {
                    }
                } else {
                    Unsafe unsafe2 = U;
                    long j4 = BASECOUNT;
                    long j5 = this.baseCount;
                    if (unsafe2.compareAndSwapLong(this, j4, j5, j5 + j)) {
                        return;
                    }
                }
            }
        }
    }

    private final void treeifyBin(Node<K, V>[] nodeArr, int i) {
        if (nodeArr != null) {
            int length = nodeArr.length;
            if (length < 64) {
                tryPresize(length << 1);
                return;
            }
            Node<K, V> nodeTabAt = tabAt(nodeArr, i);
            if (nodeTabAt != null && nodeTabAt.hash >= 0) {
                synchronized (nodeTabAt) {
                    if (tabAt(nodeArr, i) == nodeTabAt) {
                        TreeNode<K, V> treeNode = null;
                        TreeNode<K, V> treeNode2 = null;
                        Node<K, V> node = nodeTabAt;
                        while (node != null) {
                            TreeNode<K, V> treeNode3 = new TreeNode<>(node.hash, node.key, node.val, null, null);
                            treeNode3.prev = treeNode2;
                            if (treeNode2 != null) {
                                treeNode2.next = treeNode3;
                            } else {
                                treeNode = treeNode3;
                            }
                            node = node.next;
                            treeNode2 = treeNode3;
                        }
                        setTabAt(nodeArr, i, new TreeBin(treeNode));
                    }
                }
            }
        }
    }

    static <K, V> Node<K, V> untreeify(Node<K, V> node) {
        Node<K, V> node2 = null;
        Node<K, V> node3 = null;
        while (node != null) {
            Node<K, V> node4 = new Node<>(node.hash, node.key, node.val, null);
            if (node2 != null) {
                node2.next = node4;
            } else {
                node3 = node4;
            }
            node = node.next;
            node2 = node4;
        }
        return node3;
    }

    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> left;
        TreeNode<K, V> parent;
        TreeNode<K, V> prev;
        boolean red;
        TreeNode<K, V> right;

        TreeNode(int i, K k, V v, Node<K, V> node, TreeNode<K, V> treeNode) {
            super(i, k, v, node);
            this.parent = treeNode;
        }

        @Override
        Node<K, V> find(int i, Object obj) {
            return findTreeNode(i, obj, null);
        }

        final TreeNode<K, V> findTreeNode(int i, Object obj, Class<?> cls) {
            int iCompareComparables;
            if (obj == null) {
                return null;
            }
            Class<?> clsComparableClassFor = cls;
            TreeNode<K, V> treeNode = this;
            do {
                TreeNode<K, V> treeNode2 = treeNode.left;
                TreeNode<K, V> treeNode3 = treeNode.right;
                int i2 = treeNode.hash;
                if (i2 <= i) {
                    if (i2 >= i) {
                        K k = treeNode.key;
                        if (k == obj || (k != null && obj.equals(k))) {
                            return treeNode;
                        }
                        if (treeNode2 != null) {
                            if (treeNode3 != null) {
                                if ((clsComparableClassFor == null && (clsComparableClassFor = ConcurrentHashMap.comparableClassFor(obj)) == null) || (iCompareComparables = ConcurrentHashMap.compareComparables(clsComparableClassFor, obj, k)) == 0) {
                                    TreeNode<K, V> treeNodeFindTreeNode = treeNode3.findTreeNode(i, obj, clsComparableClassFor);
                                    if (treeNodeFindTreeNode != null) {
                                        return treeNodeFindTreeNode;
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
    }

    static final class TreeBin<K, V> extends Node<K, V> {
        static final boolean $assertionsDisabled = false;
        private static final long LOCKSTATE;
        static final int READER = 4;
        private static final Unsafe U = Unsafe.getUnsafe();
        static final int WAITER = 2;
        static final int WRITER = 1;
        volatile TreeNode<K, V> first;
        volatile int lockState;
        TreeNode<K, V> root;
        volatile Thread waiter;

        static {
            try {
                LOCKSTATE = U.objectFieldOffset(TreeBin.class.getDeclaredField("lockState"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }

        static int tieBreakOrder(Object obj, Object obj2) {
            int iCompareTo;
            if (obj == null || obj2 == null || (iCompareTo = obj.getClass().getName().compareTo(obj2.getClass().getName())) == 0) {
                return System.identityHashCode(obj) <= System.identityHashCode(obj2) ? -1 : 1;
            }
            return iCompareTo;
        }

        TreeBin(TreeNode<K, V> treeNode) {
            int iCompareComparables;
            int iTieBreakOrder;
            super(-2, null, null, null);
            this.first = treeNode;
            TreeNode<K, V> treeNode2 = null;
            while (treeNode != null) {
                TreeNode<K, V> treeNode3 = (TreeNode) treeNode.next;
                treeNode.right = null;
                treeNode.left = null;
                if (treeNode2 == null) {
                    treeNode.parent = null;
                    treeNode.red = $assertionsDisabled;
                } else {
                    K k = treeNode.key;
                    int i = treeNode.hash;
                    Class<?> clsComparableClassFor = null;
                    TreeNode<K, V> treeNode4 = treeNode2;
                    while (true) {
                        K k2 = treeNode4.key;
                        int i2 = treeNode4.hash;
                        if (i2 > i) {
                            iTieBreakOrder = -1;
                        } else if (i2 < i) {
                            iTieBreakOrder = 1;
                        } else if ((clsComparableClassFor == null && (clsComparableClassFor = ConcurrentHashMap.comparableClassFor(k)) == null) || (iCompareComparables = ConcurrentHashMap.compareComparables(clsComparableClassFor, k, k2)) == 0) {
                            iTieBreakOrder = tieBreakOrder(k, k2);
                        } else {
                            iTieBreakOrder = iCompareComparables;
                        }
                        TreeNode<K, V> treeNode5 = iTieBreakOrder <= 0 ? treeNode4.left : treeNode4.right;
                        if (treeNode5 == null) {
                            break;
                        } else {
                            treeNode4 = treeNode5;
                        }
                    }
                    treeNode.parent = treeNode4;
                    if (iTieBreakOrder <= 0) {
                        treeNode4.left = treeNode;
                    } else {
                        treeNode4.right = treeNode;
                    }
                    treeNode = balanceInsertion(treeNode2, treeNode);
                }
                treeNode2 = treeNode;
                treeNode = treeNode3;
            }
            this.root = treeNode2;
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
            boolean z = $assertionsDisabled;
            while (true) {
                int i = this.lockState;
                if ((i & (-3)) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, i, 1)) {
                        break;
                    }
                } else if ((i & 2) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, i, i | 2)) {
                        z = true;
                        this.waiter = Thread.currentThread();
                    }
                } else if (z) {
                    LockSupport.park(this);
                }
            }
            if (z) {
                this.waiter = null;
            }
        }

        @Override
        final Node<K, V> find(int i, Object obj) {
            K k;
            Thread thread;
            TreeNode<K, V> treeNodeFindTreeNode = null;
            if (obj != null) {
                Node<K, V> node = this.first;
                while (node != null) {
                    int i2 = this.lockState;
                    if ((i2 & 3) != 0) {
                        if (node.hash == i && ((k = node.key) == obj || (k != null && obj.equals(k)))) {
                            return node;
                        }
                        node = node.next;
                    } else if (U.compareAndSwapInt(this, LOCKSTATE, i2, i2 + 4)) {
                        try {
                            TreeNode<K, V> treeNode = this.root;
                            if (treeNode != null) {
                                treeNodeFindTreeNode = treeNode.findTreeNode(i, obj, null);
                            }
                            return treeNodeFindTreeNode;
                        } finally {
                            if (U.getAndAddInt(this, LOCKSTATE, -4) == 6 && (thread = this.waiter) != null) {
                                LockSupport.unpark(thread);
                            }
                        }
                    }
                }
            }
            return null;
        }

        final TreeNode<K, V> putTreeVal(int i, K k, V v) {
            int iCompareComparables;
            int i2;
            int iTieBreakOrder;
            TreeNode<K, V> treeNode;
            TreeNode<K, V> treeNode2;
            TreeNode<K, V> treeNode3 = this.root;
            boolean z = $assertionsDisabled;
            TreeNode<K, V> treeNode4 = treeNode3;
            Class<?> clsComparableClassFor = null;
            while (true) {
                if (treeNode4 == null) {
                    TreeNode<K, V> treeNode5 = new TreeNode<>(i, k, v, null, null);
                    this.root = treeNode5;
                    this.first = treeNode5;
                    break;
                }
                int i3 = treeNode4.hash;
                if (i3 > i) {
                    iTieBreakOrder = -1;
                } else {
                    if (i3 >= i) {
                        K k2 = treeNode4.key;
                        if (k2 == k || (k2 != null && k.equals(k2))) {
                            break;
                        }
                        if ((clsComparableClassFor == null && (clsComparableClassFor = ConcurrentHashMap.comparableClassFor(k)) == null) || (iCompareComparables = ConcurrentHashMap.compareComparables(clsComparableClassFor, k, k2)) == 0) {
                            if (!z) {
                                TreeNode<K, V> treeNode6 = treeNode4.left;
                                if ((treeNode6 != null && (r3 = treeNode6.findTreeNode(i, k, clsComparableClassFor)) != null) || ((treeNode = treeNode4.right) != null && (r3 = treeNode.findTreeNode(i, k, clsComparableClassFor)) != null)) {
                                    break;
                                }
                                z = true;
                            }
                            iTieBreakOrder = tieBreakOrder(k, k2);
                        } else {
                            i2 = iCompareComparables;
                        }
                    } else {
                        i2 = 1;
                    }
                    treeNode2 = i2 > 0 ? treeNode4.left : treeNode4.right;
                    if (treeNode2 == null) {
                        treeNode4 = treeNode2;
                    } else {
                        TreeNode<K, V> treeNode7 = this.first;
                        TreeNode<K, V> treeNode8 = new TreeNode<>(i, k, v, treeNode7, treeNode4);
                        this.first = treeNode8;
                        if (treeNode7 != null) {
                            treeNode7.prev = treeNode8;
                        }
                        if (i2 <= 0) {
                            treeNode4.left = treeNode8;
                        } else {
                            treeNode4.right = treeNode8;
                        }
                        if (!treeNode4.red) {
                            treeNode8.red = true;
                        } else {
                            lockRoot();
                            try {
                                this.root = balanceInsertion(this.root, treeNode8);
                            } finally {
                                unlockRoot();
                            }
                        }
                    }
                }
                i2 = iTieBreakOrder;
                if (i2 > 0) {
                }
                if (treeNode2 == null) {
                }
            }
        }

        final boolean removeTreeNode(TreeNode<K, V> treeNode) {
            TreeNode<K, V> treeNode2;
            TreeNode<K, V> treeNode3;
            TreeNode<K, V> treeNode4 = (TreeNode) treeNode.next;
            TreeNode<K, V> treeNode5 = treeNode.prev;
            if (treeNode5 == null) {
                this.first = treeNode4;
            } else {
                treeNode5.next = treeNode4;
            }
            if (treeNode4 != null) {
                treeNode4.prev = treeNode5;
            }
            if (this.first == null) {
                this.root = null;
                return true;
            }
            TreeNode<K, V> treeNodeBalanceDeletion = this.root;
            if (treeNodeBalanceDeletion == null || treeNodeBalanceDeletion.right == null || (treeNode2 = treeNodeBalanceDeletion.left) == null || treeNode2.left == null) {
                return true;
            }
            lockRoot();
            try {
                TreeNode<K, V> treeNode6 = treeNode.left;
                TreeNode<K, V> treeNode7 = treeNode.right;
                if (treeNode6 != null && treeNode7 != null) {
                    TreeNode<K, V> treeNode8 = treeNode7;
                    while (true) {
                        TreeNode<K, V> treeNode9 = treeNode8.left;
                        if (treeNode9 == null) {
                            break;
                        }
                        treeNode8 = treeNode9;
                    }
                    boolean z = treeNode8.red;
                    treeNode8.red = treeNode.red;
                    treeNode.red = z;
                    TreeNode<K, V> treeNode10 = treeNode8.right;
                    TreeNode<K, V> treeNode11 = treeNode.parent;
                    if (treeNode8 == treeNode7) {
                        treeNode.parent = treeNode8;
                        treeNode8.right = treeNode;
                    } else {
                        TreeNode<K, V> treeNode12 = treeNode8.parent;
                        treeNode.parent = treeNode12;
                        if (treeNode12 != null) {
                            if (treeNode8 == treeNode12.left) {
                                treeNode12.left = treeNode;
                            } else {
                                treeNode12.right = treeNode;
                            }
                        }
                        treeNode8.right = treeNode7;
                        if (treeNode7 != null) {
                            treeNode7.parent = treeNode8;
                        }
                    }
                    treeNode.left = null;
                    treeNode.right = treeNode10;
                    if (treeNode10 != null) {
                        treeNode10.parent = treeNode;
                    }
                    treeNode8.left = treeNode6;
                    if (treeNode6 != null) {
                        treeNode6.parent = treeNode8;
                    }
                    treeNode8.parent = treeNode11;
                    if (treeNode11 != null) {
                        if (treeNode == treeNode11.left) {
                            treeNode11.left = treeNode8;
                        } else {
                            treeNode11.right = treeNode8;
                        }
                    } else {
                        treeNodeBalanceDeletion = treeNode8;
                    }
                    if (treeNode10 == null) {
                        treeNode10 = treeNode;
                    }
                    treeNode6 = treeNode10;
                } else if (treeNode6 == null) {
                    treeNode6 = treeNode7 != null ? treeNode7 : treeNode;
                }
                if (treeNode6 != treeNode) {
                    TreeNode<K, V> treeNode13 = treeNode.parent;
                    treeNode6.parent = treeNode13;
                    if (treeNode13 != null) {
                        if (treeNode == treeNode13.left) {
                            treeNode13.left = treeNode6;
                        } else {
                            treeNode13.right = treeNode6;
                        }
                    } else {
                        treeNodeBalanceDeletion = treeNode6;
                    }
                    treeNode.parent = null;
                    treeNode.right = null;
                    treeNode.left = null;
                }
                if (!treeNode.red) {
                    treeNodeBalanceDeletion = balanceDeletion(treeNodeBalanceDeletion, treeNode6);
                }
                this.root = treeNodeBalanceDeletion;
                if (treeNode == treeNode6 && (treeNode3 = treeNode.parent) != null) {
                    if (treeNode == treeNode3.left) {
                        treeNode3.left = null;
                    } else if (treeNode == treeNode3.right) {
                        treeNode3.right = null;
                    }
                    treeNode.parent = null;
                }
                unlockRoot();
                return $assertionsDisabled;
            } catch (Throwable th) {
                unlockRoot();
                throw th;
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
                    treeNode3.red = $assertionsDisabled;
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
                    treeNode3.red = $assertionsDisabled;
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
                    treeNode2.red = $assertionsDisabled;
                    return treeNode2;
                }
                if (!treeNode6.red || (treeNode3 = treeNode6.parent) == null) {
                    break;
                }
                TreeNode<K, V> treeNode7 = treeNode3.left;
                if (treeNode6 == treeNode7) {
                    TreeNode<K, V> treeNode8 = treeNode3.right;
                    if (treeNode8 != null && treeNode8.red) {
                        treeNode8.red = $assertionsDisabled;
                        treeNode6.red = $assertionsDisabled;
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
                            treeNode5.red = $assertionsDisabled;
                            if (treeNode3 != null) {
                                treeNode3.red = true;
                                treeNode = rotateRight(treeNode, treeNode3);
                            }
                        }
                        treeNode2 = treeNode6;
                    }
                } else if (treeNode7 != null && treeNode7.red) {
                    treeNode7.red = $assertionsDisabled;
                    treeNode6.red = $assertionsDisabled;
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
                        treeNode4.red = $assertionsDisabled;
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
                    treeNode2.red = $assertionsDisabled;
                    return treeNode2;
                }
                if (treeNode2.red) {
                    treeNode2.red = $assertionsDisabled;
                    return treeNode;
                }
                TreeNode<K, V> treeNode4 = treeNode3.left;
                if (treeNode4 == treeNode2) {
                    TreeNode<K, V> treeNode5 = treeNode3.right;
                    if (treeNode5 != null && treeNode5.red) {
                        treeNode5.red = $assertionsDisabled;
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
                                    treeNode6.red = $assertionsDisabled;
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
                                    treeNode8.red = $assertionsDisabled;
                                }
                            }
                            if (treeNode3 != null) {
                                treeNode3.red = $assertionsDisabled;
                                treeNode = rotateLeft(treeNode, treeNode3);
                            }
                            treeNode2 = treeNode;
                        }
                    }
                } else {
                    if (treeNode4 != null && treeNode4.red) {
                        treeNode4.red = $assertionsDisabled;
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
                                    treeNode10.red = $assertionsDisabled;
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
                                    treeNode11.red = $assertionsDisabled;
                                }
                            }
                            if (treeNode3 != null) {
                                treeNode3.red = $assertionsDisabled;
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
                return $assertionsDisabled;
            }
            if (treeNode6 != null && treeNode6.prev != treeNode) {
                return $assertionsDisabled;
            }
            if (treeNode2 != null && treeNode != treeNode2.left && treeNode != treeNode2.right) {
                return $assertionsDisabled;
            }
            if (treeNode3 != null && (treeNode3.parent != treeNode || treeNode3.hash > treeNode.hash)) {
                return $assertionsDisabled;
            }
            if (treeNode4 != null && (treeNode4.parent != treeNode || treeNode4.hash < treeNode.hash)) {
                return $assertionsDisabled;
            }
            if (treeNode.red && treeNode3 != null && treeNode3.red && treeNode4 != null && treeNode4.red) {
                return $assertionsDisabled;
            }
            if (treeNode3 != null && !checkInvariants(treeNode3)) {
                return $assertionsDisabled;
            }
            if (treeNode4 != null && !checkInvariants(treeNode4)) {
                return $assertionsDisabled;
            }
            return true;
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

        Traverser(Node<K, V>[] nodeArr, int i, int i2, int i3) {
            this.tab = nodeArr;
            this.baseSize = i;
            this.index = i2;
            this.baseIndex = i2;
            this.baseLimit = i3;
        }

        final Node<K, V> advance() {
            Node<K, V>[] nodeArr;
            int length;
            int i;
            Node<K, V> node = this.next;
            if (node != null) {
                node = node.next;
            }
            while (node == null) {
                if (this.baseIndex >= this.baseLimit || (nodeArr = this.tab) == null || (length = nodeArr.length) <= (i = this.index) || i < 0) {
                    this.next = null;
                    return null;
                }
                Node<K, V> nodeTabAt = ConcurrentHashMap.tabAt(nodeArr, i);
                if (nodeTabAt == null || nodeTabAt.hash >= 0) {
                    node = nodeTabAt;
                    if (this.stack == null) {
                        recoverState(length);
                    } else {
                        int i2 = i + this.baseSize;
                        this.index = i2;
                        if (i2 >= length) {
                            int i3 = this.baseIndex + 1;
                            this.baseIndex = i3;
                            this.index = i3;
                        }
                    }
                } else if (nodeTabAt instanceof ForwardingNode) {
                    this.tab = ((ForwardingNode) nodeTabAt).nextTable;
                    pushState(nodeArr, i, length);
                    node = null;
                } else {
                    if (nodeTabAt instanceof TreeBin) {
                        node = ((TreeBin) nodeTabAt).first;
                    } else {
                        node = null;
                    }
                    if (this.stack == null) {
                    }
                }
            }
            this.next = node;
            return node;
        }

        private void pushState(Node<K, V>[] nodeArr, int i, int i2) {
            TableStack<K, V> tableStack = this.spare;
            if (tableStack != null) {
                this.spare = tableStack.next;
            } else {
                tableStack = new TableStack<>();
            }
            tableStack.tab = nodeArr;
            tableStack.length = i2;
            tableStack.index = i;
            tableStack.next = this.stack;
            this.stack = tableStack;
        }

        private void recoverState(int i) {
            TableStack<K, V> tableStack;
            while (true) {
                tableStack = this.stack;
                if (tableStack == null) {
                    break;
                }
                int i2 = this.index;
                int i3 = tableStack.length;
                int i4 = i2 + i3;
                this.index = i4;
                if (i4 < i) {
                    break;
                }
                this.index = tableStack.index;
                this.tab = tableStack.tab;
                tableStack.tab = null;
                TableStack<K, V> tableStack2 = tableStack.next;
                tableStack.next = this.spare;
                this.stack = tableStack2;
                this.spare = tableStack;
                i = i3;
            }
            if (tableStack == null) {
                int i5 = this.index + this.baseSize;
                this.index = i5;
                if (i5 >= i) {
                    int i6 = this.baseIndex + 1;
                    this.baseIndex = i6;
                    this.index = i6;
                }
            }
        }
    }

    static class BaseIterator<K, V> extends Traverser<K, V> {
        Node<K, V> lastReturned;
        final ConcurrentHashMap<K, V> map;

        BaseIterator(Node<K, V>[] nodeArr, int i, int i2, int i3, ConcurrentHashMap<K, V> concurrentHashMap) {
            super(nodeArr, i, i2, i3);
            this.map = concurrentHashMap;
            advance();
        }

        public final boolean hasNext() {
            return this.next != null;
        }

        public final boolean hasMoreElements() {
            return this.next != null;
        }

        public final void remove() {
            Node<K, V> node = this.lastReturned;
            if (node == null) {
                throw new IllegalStateException();
            }
            this.lastReturned = null;
            this.map.replaceNode(node.key, null, null);
        }
    }

    static final class KeyIterator<K, V> extends BaseIterator<K, V> implements Iterator<K>, Enumeration<K> {
        KeyIterator(Node<K, V>[] nodeArr, int i, int i2, int i3, ConcurrentHashMap<K, V> concurrentHashMap) {
            super(nodeArr, i, i2, i3, concurrentHashMap);
        }

        @Override
        public final K next() {
            Node<K, V> node = this.next;
            if (node == null) {
                throw new NoSuchElementException();
            }
            K k = node.key;
            this.lastReturned = node;
            advance();
            return k;
        }

        @Override
        public final K nextElement() {
            return next();
        }
    }

    static final class ValueIterator<K, V> extends BaseIterator<K, V> implements Iterator<V>, Enumeration<V> {
        ValueIterator(Node<K, V>[] nodeArr, int i, int i2, int i3, ConcurrentHashMap<K, V> concurrentHashMap) {
            super(nodeArr, i, i2, i3, concurrentHashMap);
        }

        @Override
        public final V next() {
            Node<K, V> node = this.next;
            if (node == null) {
                throw new NoSuchElementException();
            }
            V v = node.val;
            this.lastReturned = node;
            advance();
            return v;
        }

        @Override
        public final V nextElement() {
            return next();
        }
    }

    static final class EntryIterator<K, V> extends BaseIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        EntryIterator(Node<K, V>[] nodeArr, int i, int i2, int i3, ConcurrentHashMap<K, V> concurrentHashMap) {
            super(nodeArr, i, i2, i3, concurrentHashMap);
        }

        @Override
        public final Map.Entry<K, V> next() {
            Node<K, V> node = this.next;
            if (node == null) {
                throw new NoSuchElementException();
            }
            K k = node.key;
            V v = node.val;
            this.lastReturned = node;
            advance();
            return new MapEntry(k, v, this.map);
        }
    }

    static final class MapEntry<K, V> implements Map.Entry<K, V> {
        final K key;
        final ConcurrentHashMap<K, V> map;
        V val;

        MapEntry(K k, V v, ConcurrentHashMap<K, V> concurrentHashMap) {
            this.key = k;
            this.val = v;
            this.map = concurrentHashMap;
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
        public boolean equals(Object obj) {
            Map.Entry entry;
            Object key;
            Object value;
            return (obj instanceof Map.Entry) && (key = (entry = (Map.Entry) obj).getKey()) != null && (value = entry.getValue()) != null && (key == this.key || key.equals(this.key)) && (value == this.val || value.equals(this.val));
        }

        @Override
        public V setValue(V v) {
            if (v == null) {
                throw new NullPointerException();
            }
            V v2 = this.val;
            this.val = v;
            this.map.put(this.key, v);
            return v2;
        }
    }

    static final class KeySpliterator<K, V> extends Traverser<K, V> implements Spliterator<K> {
        long est;

        KeySpliterator(Node<K, V>[] nodeArr, int i, int i2, int i3, long j) {
            super(nodeArr, i, i2, i3);
            this.est = j;
        }

        @Override
        public KeySpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int i2 = this.baseLimit;
            int i3 = (i + i2) >>> 1;
            if (i3 <= i) {
                return null;
            }
            Node<K, V>[] nodeArr = this.tab;
            int i4 = this.baseSize;
            this.baseLimit = i3;
            long j = this.est >>> 1;
            this.est = j;
            return new KeySpliterator<>(nodeArr, i4, i3, i2, j);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    consumer.accept(nodeAdvance.key);
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

        ValueSpliterator(Node<K, V>[] nodeArr, int i, int i2, int i3, long j) {
            super(nodeArr, i, i2, i3);
            this.est = j;
        }

        @Override
        public ValueSpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int i2 = this.baseLimit;
            int i3 = (i + i2) >>> 1;
            if (i3 <= i) {
                return null;
            }
            Node<K, V>[] nodeArr = this.tab;
            int i4 = this.baseSize;
            this.baseLimit = i3;
            long j = this.est >>> 1;
            this.est = j;
            return new ValueSpliterator<>(nodeArr, i4, i3, i2, j);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    consumer.accept(nodeAdvance.val);
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
            return 4352;
        }
    }

    static final class EntrySpliterator<K, V> extends Traverser<K, V> implements Spliterator<Map.Entry<K, V>> {
        long est;
        final ConcurrentHashMap<K, V> map;

        EntrySpliterator(Node<K, V>[] nodeArr, int i, int i2, int i3, long j, ConcurrentHashMap<K, V> concurrentHashMap) {
            super(nodeArr, i, i2, i3);
            this.map = concurrentHashMap;
            this.est = j;
        }

        @Override
        public EntrySpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int i2 = this.baseLimit;
            int i3 = (i + i2) >>> 1;
            if (i3 <= i) {
                return null;
            }
            Node<K, V>[] nodeArr = this.tab;
            int i4 = this.baseSize;
            this.baseLimit = i3;
            long j = this.est >>> 1;
            this.est = j;
            return new EntrySpliterator<>(nodeArr, i4, i3, i2, j, this.map);
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            while (true) {
                Node<K, V> nodeAdvance = advance();
                if (nodeAdvance != null) {
                    consumer.accept(new MapEntry(nodeAdvance.key, nodeAdvance.val, this.map));
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
            Node<K, V> nodeAdvance = advance();
            if (nodeAdvance == null) {
                return false;
            }
            consumer.accept(new MapEntry(nodeAdvance.key, nodeAdvance.val, this.map));
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

    final int batchFor(long j) {
        if (j == Long.MAX_VALUE) {
            return 0;
        }
        long jSumCount = sumCount();
        if (jSumCount <= 1 || jSumCount < j) {
            return 0;
        }
        int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism() << 2;
        if (j <= 0) {
            return commonPoolParallelism;
        }
        long j2 = jSumCount / j;
        return j2 >= ((long) commonPoolParallelism) ? commonPoolParallelism : (int) j2;
    }

    public void forEach(long j, BiConsumer<? super K, ? super V> biConsumer) {
        if (biConsumer == null) {
            throw new NullPointerException();
        }
        new ForEachMappingTask(null, batchFor(j), 0, 0, this.table, biConsumer).invoke();
    }

    public <U> void forEach(long j, BiFunction<? super K, ? super V, ? extends U> biFunction, Consumer<? super U> consumer) {
        if (biFunction == null || consumer == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedMappingTask(null, batchFor(j), 0, 0, this.table, biFunction, consumer).invoke();
    }

    public <U> U search(long j, BiFunction<? super K, ? super V, ? extends U> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        return new SearchMappingsTask(null, batchFor(j), 0, 0, this.table, biFunction, new AtomicReference()).invoke();
    }

    public <U> U reduce(long j, BiFunction<? super K, ? super V, ? extends U> biFunction, BiFunction<? super U, ? super U, ? extends U> biFunction2) {
        if (biFunction == null || biFunction2 == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsTask(null, batchFor(j), 0, 0, this.table, null, biFunction, biFunction2).invoke();
    }

    public double reduceToDouble(long j, ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
        if (toDoubleBiFunction == null || doubleBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsToDoubleTask(null, batchFor(j), 0, 0, this.table, null, toDoubleBiFunction, d, doubleBinaryOperator).invoke().doubleValue();
    }

    public long reduceToLong(long j, ToLongBiFunction<? super K, ? super V> toLongBiFunction, long j2, LongBinaryOperator longBinaryOperator) {
        if (toLongBiFunction == null || longBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsToLongTask(null, batchFor(j), 0, 0, this.table, null, toLongBiFunction, j2, longBinaryOperator).invoke().longValue();
    }

    public int reduceToInt(long j, ToIntBiFunction<? super K, ? super V> toIntBiFunction, int i, IntBinaryOperator intBinaryOperator) {
        if (toIntBiFunction == null || intBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceMappingsToIntTask(null, batchFor(j), 0, 0, this.table, null, toIntBiFunction, i, intBinaryOperator).invoke().intValue();
    }

    public void forEachKey(long j, Consumer<? super K> consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }
        new ForEachKeyTask(null, batchFor(j), 0, 0, this.table, consumer).invoke();
    }

    public <U> void forEachKey(long j, Function<? super K, ? extends U> function, Consumer<? super U> consumer) {
        if (function == null || consumer == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedKeyTask(null, batchFor(j), 0, 0, this.table, function, consumer).invoke();
    }

    public <U> U searchKeys(long j, Function<? super K, ? extends U> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        return new SearchKeysTask(null, batchFor(j), 0, 0, this.table, function, new AtomicReference()).invoke();
    }

    public K reduceKeys(long j, BiFunction<? super K, ? super K, ? extends K> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        return new ReduceKeysTask(null, batchFor(j), 0, 0, this.table, null, biFunction).invoke();
    }

    public <U> U reduceKeys(long j, Function<? super K, ? extends U> function, BiFunction<? super U, ? super U, ? extends U> biFunction) {
        if (function == null || biFunction == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysTask(null, batchFor(j), 0, 0, this.table, null, function, biFunction).invoke();
    }

    public double reduceKeysToDouble(long j, ToDoubleFunction<? super K> toDoubleFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
        if (toDoubleFunction == null || doubleBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysToDoubleTask(null, batchFor(j), 0, 0, this.table, null, toDoubleFunction, d, doubleBinaryOperator).invoke().doubleValue();
    }

    public long reduceKeysToLong(long j, ToLongFunction<? super K> toLongFunction, long j2, LongBinaryOperator longBinaryOperator) {
        if (toLongFunction == null || longBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysToLongTask(null, batchFor(j), 0, 0, this.table, null, toLongFunction, j2, longBinaryOperator).invoke().longValue();
    }

    public int reduceKeysToInt(long j, ToIntFunction<? super K> toIntFunction, int i, IntBinaryOperator intBinaryOperator) {
        if (toIntFunction == null || intBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceKeysToIntTask(null, batchFor(j), 0, 0, this.table, null, toIntFunction, i, intBinaryOperator).invoke().intValue();
    }

    public void forEachValue(long j, Consumer<? super V> consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }
        new ForEachValueTask(null, batchFor(j), 0, 0, this.table, consumer).invoke();
    }

    public <U> void forEachValue(long j, Function<? super V, ? extends U> function, Consumer<? super U> consumer) {
        if (function == null || consumer == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedValueTask(null, batchFor(j), 0, 0, this.table, function, consumer).invoke();
    }

    public <U> U searchValues(long j, Function<? super V, ? extends U> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        return new SearchValuesTask(null, batchFor(j), 0, 0, this.table, function, new AtomicReference()).invoke();
    }

    public V reduceValues(long j, BiFunction<? super V, ? super V, ? extends V> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        return new ReduceValuesTask(null, batchFor(j), 0, 0, this.table, null, biFunction).invoke();
    }

    public <U> U reduceValues(long j, Function<? super V, ? extends U> function, BiFunction<? super U, ? super U, ? extends U> biFunction) {
        if (function == null || biFunction == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesTask(null, batchFor(j), 0, 0, this.table, null, function, biFunction).invoke();
    }

    public double reduceValuesToDouble(long j, ToDoubleFunction<? super V> toDoubleFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
        if (toDoubleFunction == null || doubleBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesToDoubleTask(null, batchFor(j), 0, 0, this.table, null, toDoubleFunction, d, doubleBinaryOperator).invoke().doubleValue();
    }

    public long reduceValuesToLong(long j, ToLongFunction<? super V> toLongFunction, long j2, LongBinaryOperator longBinaryOperator) {
        if (toLongFunction == null || longBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesToLongTask(null, batchFor(j), 0, 0, this.table, null, toLongFunction, j2, longBinaryOperator).invoke().longValue();
    }

    public int reduceValuesToInt(long j, ToIntFunction<? super V> toIntFunction, int i, IntBinaryOperator intBinaryOperator) {
        if (toIntFunction == null || intBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceValuesToIntTask(null, batchFor(j), 0, 0, this.table, null, toIntFunction, i, intBinaryOperator).invoke().intValue();
    }

    public void forEachEntry(long j, Consumer<? super Map.Entry<K, V>> consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }
        new ForEachEntryTask(null, batchFor(j), 0, 0, this.table, consumer).invoke();
    }

    public <U> void forEachEntry(long j, Function<Map.Entry<K, V>, ? extends U> function, Consumer<? super U> consumer) {
        if (function == null || consumer == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedEntryTask(null, batchFor(j), 0, 0, this.table, function, consumer).invoke();
    }

    public <U> U searchEntries(long j, Function<Map.Entry<K, V>, ? extends U> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        return new SearchEntriesTask(null, batchFor(j), 0, 0, this.table, function, new AtomicReference()).invoke();
    }

    public Map.Entry<K, V> reduceEntries(long j, BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        return new ReduceEntriesTask(null, batchFor(j), 0, 0, this.table, null, biFunction).invoke();
    }

    public <U> U reduceEntries(long j, Function<Map.Entry<K, V>, ? extends U> function, BiFunction<? super U, ? super U, ? extends U> biFunction) {
        if (function == null || biFunction == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesTask(null, batchFor(j), 0, 0, this.table, null, function, biFunction).invoke();
    }

    public double reduceEntriesToDouble(long j, ToDoubleFunction<Map.Entry<K, V>> toDoubleFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
        if (toDoubleFunction == null || doubleBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesToDoubleTask(null, batchFor(j), 0, 0, this.table, null, toDoubleFunction, d, doubleBinaryOperator).invoke().doubleValue();
    }

    public long reduceEntriesToLong(long j, ToLongFunction<Map.Entry<K, V>> toLongFunction, long j2, LongBinaryOperator longBinaryOperator) {
        if (toLongFunction == null || longBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesToLongTask(null, batchFor(j), 0, 0, this.table, null, toLongFunction, j2, longBinaryOperator).invoke().longValue();
    }

    public int reduceEntriesToInt(long j, ToIntFunction<Map.Entry<K, V>> toIntFunction, int i, IntBinaryOperator intBinaryOperator) {
        if (toIntFunction == null || intBinaryOperator == null) {
            throw new NullPointerException();
        }
        return new MapReduceEntriesToIntTask(null, batchFor(j), 0, 0, this.table, null, toIntFunction, i, intBinaryOperator).invoke().intValue();
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

        CollectionView(ConcurrentHashMap<K, V> concurrentHashMap) {
            this.map = concurrentHashMap;
        }

        public ConcurrentHashMap<K, V> getMap() {
            return this.map;
        }

        @Override
        public final void clear() {
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
            long jMappingCount = this.map.mappingCount();
            if (jMappingCount > 2147483639) {
                throw new OutOfMemoryError(OOME_MSG);
            }
            int i = (int) jMappingCount;
            Object[] objArrCopyOf = new Object[i];
            int i2 = 0;
            for (E e : this) {
                if (i2 == i) {
                    int i3 = ConcurrentHashMap.MAX_ARRAY_SIZE;
                    if (i >= ConcurrentHashMap.MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(OOME_MSG);
                    }
                    if (i < 1073741819) {
                        i3 = (i >>> 1) + 1 + i;
                    }
                    objArrCopyOf = Arrays.copyOf(objArrCopyOf, i3);
                    i = i3;
                }
                objArrCopyOf[i2] = e;
                i2++;
            }
            return i2 == i ? objArrCopyOf : Arrays.copyOf(objArrCopyOf, i2);
        }

        @Override
        public final <T> T[] toArray(T[] tArr) {
            long jMappingCount = this.map.mappingCount();
            if (jMappingCount > 2147483639) {
                throw new OutOfMemoryError(OOME_MSG);
            }
            int i = (int) jMappingCount;
            Object[] objArr = tArr.length < i ? (Object[]) Array.newInstance(tArr.getClass().getComponentType(), i) : tArr;
            int length = objArr.length;
            int i2 = 0;
            for (E e : this) {
                if (i2 == length) {
                    int i3 = ConcurrentHashMap.MAX_ARRAY_SIZE;
                    if (length >= ConcurrentHashMap.MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(OOME_MSG);
                    }
                    if (length < 1073741819) {
                        i3 = (length >>> 1) + 1 + length;
                    }
                    objArr = (T[]) Arrays.copyOf(objArr, i3);
                    length = i3;
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
                    Object next = it.next();
                    if (next == this) {
                        next = "(this Collection)";
                    }
                    sb.append(next);
                    if (!it.hasNext()) {
                        break;
                    }
                    sb.append(',');
                    sb.append(' ');
                }
            }
            sb.append(']');
            return sb.toString();
        }

        @Override
        public final boolean containsAll(Collection<?> collection) {
            if (collection != this) {
                for (Object obj : collection) {
                    if (obj == null || !contains(obj)) {
                        return false;
                    }
                }
                return true;
            }
            return true;
        }

        @Override
        public final boolean removeAll(Collection<?> collection) {
            if (collection == null) {
                throw new NullPointerException();
            }
            boolean z = false;
            Iterator<E> it = iterator();
            while (it.hasNext()) {
                if (collection.contains(it.next())) {
                    it.remove();
                    z = true;
                }
            }
            return z;
        }

        @Override
        public final boolean retainAll(Collection<?> collection) {
            if (collection == null) {
                throw new NullPointerException();
            }
            boolean z = false;
            Iterator<E> it = iterator();
            while (it.hasNext()) {
                if (!collection.contains(it.next())) {
                    it.remove();
                    z = true;
                }
            }
            return z;
        }
    }

    public static class KeySetView<K, V> extends CollectionView<K, V, K> implements Set<K>, Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;

        @Override
        public ConcurrentHashMap getMap() {
            return super.getMap();
        }

        KeySetView(ConcurrentHashMap<K, V> concurrentHashMap, V v) {
            super(concurrentHashMap);
            this.value = v;
        }

        public V getMappedValue() {
            return this.value;
        }

        @Override
        public boolean contains(Object obj) {
            return this.map.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            return this.map.remove(obj) != null;
        }

        @Override
        public Iterator<K> iterator() {
            ConcurrentHashMap<K, V> concurrentHashMap = this.map;
            Node<K, V>[] nodeArr = concurrentHashMap.table;
            int length = nodeArr == null ? 0 : nodeArr.length;
            return new KeyIterator(nodeArr, length, 0, length, concurrentHashMap);
        }

        @Override
        public boolean add(K k) {
            V v = this.value;
            if (v != null) {
                return this.map.putVal(k, v, true) == null;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends K> collection) {
            V v = this.value;
            if (v == null) {
                throw new UnsupportedOperationException();
            }
            Iterator<? extends K> it = collection.iterator();
            boolean z = false;
            while (it.hasNext()) {
                if (this.map.putVal(it.next(), v, true) == null) {
                    z = true;
                }
            }
            return z;
        }

        @Override
        public int hashCode() {
            Iterator<K> it = iterator();
            int iHashCode = 0;
            while (it.hasNext()) {
                iHashCode += it.next().hashCode();
            }
            return iHashCode;
        }

        @Override
        public boolean equals(Object obj) {
            Set set;
            return (obj instanceof Set) && ((set = (Set) obj) == this || (containsAll(set) && set.containsAll(this)));
        }

        @Override
        public Spliterator<K> spliterator() {
            ConcurrentHashMap<K, V> concurrentHashMap = this.map;
            long jSumCount = concurrentHashMap.sumCount();
            Node<K, V>[] nodeArr = concurrentHashMap.table;
            int length = nodeArr == null ? 0 : nodeArr.length;
            return new KeySpliterator(nodeArr, length, 0, length, jSumCount >= 0 ? jSumCount : 0L);
        }

        @Override
        public void forEach(Consumer<? super K> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr != null) {
                Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
                while (true) {
                    Node<K, V> nodeAdvance = traverser.advance();
                    if (nodeAdvance != null) {
                        consumer.accept(nodeAdvance.key);
                    } else {
                        return;
                    }
                }
            }
        }
    }

    static final class ValuesView<K, V> extends CollectionView<K, V, V> implements Collection<V>, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        ValuesView(ConcurrentHashMap<K, V> concurrentHashMap) {
            super(concurrentHashMap);
        }

        @Override
        public final boolean contains(Object obj) {
            return this.map.containsValue(obj);
        }

        @Override
        public final boolean remove(Object obj) {
            if (obj != null) {
                Iterator<V> it = iterator();
                while (it.hasNext()) {
                    if (obj.equals(it.next())) {
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
            ConcurrentHashMap<K, V> concurrentHashMap = this.map;
            Node<K, V>[] nodeArr = concurrentHashMap.table;
            int length = nodeArr == null ? 0 : nodeArr.length;
            return new ValueIterator(nodeArr, length, 0, length, concurrentHashMap);
        }

        @Override
        public final boolean add(V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean addAll(Collection<? extends V> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super V> predicate) {
            return this.map.removeValueIf(predicate);
        }

        @Override
        public Spliterator<V> spliterator() {
            ConcurrentHashMap<K, V> concurrentHashMap = this.map;
            long jSumCount = concurrentHashMap.sumCount();
            Node<K, V>[] nodeArr = concurrentHashMap.table;
            int length = nodeArr == null ? 0 : nodeArr.length;
            return new ValueSpliterator(nodeArr, length, 0, length, jSumCount >= 0 ? jSumCount : 0L);
        }

        @Override
        public void forEach(Consumer<? super V> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr != null) {
                Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
                while (true) {
                    Node<K, V> nodeAdvance = traverser.advance();
                    if (nodeAdvance != null) {
                        consumer.accept(nodeAdvance.val);
                    } else {
                        return;
                    }
                }
            }
        }
    }

    static final class EntrySetView<K, V> extends CollectionView<K, V, Map.Entry<K, V>> implements Set<Map.Entry<K, V>>, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        EntrySetView(ConcurrentHashMap<K, V> concurrentHashMap) {
            super(concurrentHashMap);
        }

        @Override
        public boolean contains(Object obj) {
            Map.Entry entry;
            Object key;
            V v;
            Object value;
            return (!(obj instanceof Map.Entry) || (key = (entry = (Map.Entry) obj).getKey()) == null || (v = this.map.get(key)) == null || (value = entry.getValue()) == null || (value != v && !value.equals(v))) ? false : true;
        }

        @Override
        public boolean remove(Object obj) {
            Map.Entry entry;
            Object key;
            Object value;
            return (obj instanceof Map.Entry) && (key = (entry = (Map.Entry) obj).getKey()) != null && (value = entry.getValue()) != null && this.map.remove(key, value);
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            ConcurrentHashMap<K, V> concurrentHashMap = this.map;
            Node<K, V>[] nodeArr = concurrentHashMap.table;
            int length = nodeArr == null ? 0 : nodeArr.length;
            return new EntryIterator(nodeArr, length, 0, length, concurrentHashMap);
        }

        @Override
        public boolean add(Map.Entry<K, V> entry) {
            return this.map.putVal(entry.getKey(), entry.getValue(), false) == null;
        }

        @Override
        public boolean addAll(Collection<? extends Map.Entry<K, V>> collection) {
            Iterator<? extends Map.Entry<K, V>> it = collection.iterator();
            boolean z = false;
            while (it.hasNext()) {
                if (add((Map.Entry) it.next())) {
                    z = true;
                }
            }
            return z;
        }

        @Override
        public boolean removeIf(Predicate<? super Map.Entry<K, V>> predicate) {
            return this.map.removeEntryIf(predicate);
        }

        @Override
        public final int hashCode() {
            Node<K, V>[] nodeArr = this.map.table;
            int iHashCode = 0;
            if (nodeArr != null) {
                Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
                while (true) {
                    Node<K, V> nodeAdvance = traverser.advance();
                    if (nodeAdvance == null) {
                        break;
                    }
                    iHashCode += nodeAdvance.hashCode();
                }
            }
            return iHashCode;
        }

        @Override
        public final boolean equals(Object obj) {
            Set set;
            return (obj instanceof Set) && ((set = (Set) obj) == this || (containsAll(set) && set.containsAll(this)));
        }

        @Override
        public Spliterator<Map.Entry<K, V>> spliterator() {
            ConcurrentHashMap<K, V> concurrentHashMap = this.map;
            long jSumCount = concurrentHashMap.sumCount();
            Node<K, V>[] nodeArr = concurrentHashMap.table;
            int length = nodeArr == null ? 0 : nodeArr.length;
            return new EntrySpliterator(nodeArr, length, 0, length, jSumCount >= 0 ? jSumCount : 0L, concurrentHashMap);
        }

        @Override
        public void forEach(Consumer<? super Map.Entry<K, V>> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] nodeArr = this.map.table;
            if (nodeArr != null) {
                Traverser traverser = new Traverser(nodeArr, nodeArr.length, 0, nodeArr.length);
                while (true) {
                    Node<K, V> nodeAdvance = traverser.advance();
                    if (nodeAdvance != null) {
                        consumer.accept(new MapEntry(nodeAdvance.key, nodeAdvance.val, this.map));
                    } else {
                        return;
                    }
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

        BulkTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr) {
            super(bulkTask);
            this.batch = i;
            this.baseIndex = i2;
            this.index = i2;
            this.tab = nodeArr;
            if (nodeArr == null) {
                this.baseLimit = 0;
                this.baseSize = 0;
            } else if (bulkTask == null) {
                int length = nodeArr.length;
                this.baseLimit = length;
                this.baseSize = length;
            } else {
                this.baseLimit = i3;
                this.baseSize = bulkTask.baseSize;
            }
        }

        final Node<K, V> advance() {
            Node<K, V>[] nodeArr;
            int length;
            int i;
            Node<K, V> node = this.next;
            if (node != null) {
                node = node.next;
            }
            while (node == null) {
                if (this.baseIndex >= this.baseLimit || (nodeArr = this.tab) == null || (length = nodeArr.length) <= (i = this.index) || i < 0) {
                    this.next = null;
                    return null;
                }
                Node<K, V> nodeTabAt = ConcurrentHashMap.tabAt(nodeArr, i);
                if (nodeTabAt == null || nodeTabAt.hash >= 0) {
                    node = nodeTabAt;
                    if (this.stack == null) {
                        recoverState(length);
                    } else {
                        int i2 = i + this.baseSize;
                        this.index = i2;
                        if (i2 >= length) {
                            int i3 = this.baseIndex + 1;
                            this.baseIndex = i3;
                            this.index = i3;
                        }
                    }
                } else if (nodeTabAt instanceof ForwardingNode) {
                    this.tab = ((ForwardingNode) nodeTabAt).nextTable;
                    pushState(nodeArr, i, length);
                    node = null;
                } else {
                    if (nodeTabAt instanceof TreeBin) {
                        node = ((TreeBin) nodeTabAt).first;
                    } else {
                        node = null;
                    }
                    if (this.stack == null) {
                    }
                }
            }
            this.next = node;
            return node;
        }

        private void pushState(Node<K, V>[] nodeArr, int i, int i2) {
            TableStack<K, V> tableStack = this.spare;
            if (tableStack != null) {
                this.spare = tableStack.next;
            } else {
                tableStack = new TableStack<>();
            }
            tableStack.tab = nodeArr;
            tableStack.length = i2;
            tableStack.index = i;
            tableStack.next = this.stack;
            this.stack = tableStack;
        }

        private void recoverState(int i) {
            TableStack<K, V> tableStack;
            while (true) {
                tableStack = this.stack;
                if (tableStack == null) {
                    break;
                }
                int i2 = this.index;
                int i3 = tableStack.length;
                int i4 = i2 + i3;
                this.index = i4;
                if (i4 < i) {
                    break;
                }
                this.index = tableStack.index;
                this.tab = tableStack.tab;
                tableStack.tab = null;
                TableStack<K, V> tableStack2 = tableStack.next;
                tableStack.next = this.spare;
                this.stack = tableStack2;
                this.spare = tableStack;
                i = i3;
            }
            if (tableStack == null) {
                int i5 = this.index + this.baseSize;
                this.index = i5;
                if (i5 >= i) {
                    int i6 = this.baseIndex + 1;
                    this.baseIndex = i6;
                    this.index = i6;
                }
            }
        }
    }

    static final class ForEachKeyTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super K> action;

        ForEachKeyTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Consumer<? super K> consumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.action = consumer;
        }

        @Override
        public final void compute() {
            Consumer<? super K> consumer = this.action;
            if (consumer != null) {
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
    }

    static final class ForEachValueTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super V> action;

        ForEachValueTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Consumer<? super V> consumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.action = consumer;
        }

        @Override
        public final void compute() {
            Consumer<? super V> consumer = this.action;
            if (consumer != null) {
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
    }

    static final class ForEachEntryTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super Map.Entry<K, V>> action;

        ForEachEntryTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Consumer<? super Map.Entry<K, V>> consumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.action = consumer;
        }

        @Override
        public final void compute() {
            Consumer<? super Map.Entry<K, V>> consumer = this.action;
            if (consumer != null) {
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
                    new ForEachEntryTask(this, i4, i3, i2, this.tab, consumer).fork();
                }
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance != null) {
                        consumer.accept(nodeAdvance);
                    } else {
                        propagateCompletion();
                        return;
                    }
                }
            }
        }
    }

    static final class ForEachMappingTask<K, V> extends BulkTask<K, V, Void> {
        final BiConsumer<? super K, ? super V> action;

        ForEachMappingTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, BiConsumer<? super K, ? super V> biConsumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.action = biConsumer;
        }

        @Override
        public final void compute() {
            BiConsumer<? super K, ? super V> biConsumer = this.action;
            if (biConsumer != null) {
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
    }

    static final class ForEachTransformedKeyTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<? super K, ? extends U> transformer;

        ForEachTransformedKeyTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Function<? super K, ? extends U> function, Consumer<? super U> consumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.transformer = function;
            this.action = consumer;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            Function<? super K, ? extends U> function = this.transformer;
            if (function != null && (consumer = this.action) != null) {
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
    }

    static final class ForEachTransformedValueTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<? super V, ? extends U> transformer;

        ForEachTransformedValueTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Function<? super V, ? extends U> function, Consumer<? super U> consumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.transformer = function;
            this.action = consumer;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            Function<? super V, ? extends U> function = this.transformer;
            if (function != null && (consumer = this.action) != null) {
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
    }

    static final class ForEachTransformedEntryTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<Map.Entry<K, V>, ? extends U> transformer;

        ForEachTransformedEntryTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Function<Map.Entry<K, V>, ? extends U> function, Consumer<? super U> consumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.transformer = function;
            this.action = consumer;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            Function<Map.Entry<K, V>, ? extends U> function = this.transformer;
            if (function != null && (consumer = this.action) != null) {
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
    }

    static final class ForEachTransformedMappingTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final BiFunction<? super K, ? super V, ? extends U> transformer;

        ForEachTransformedMappingTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, BiFunction<? super K, ? super V, ? extends U> biFunction, Consumer<? super U> consumer) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.transformer = biFunction;
            this.action = consumer;
        }

        @Override
        public final void compute() {
            Consumer<? super U> consumer;
            BiFunction<? super K, ? super V, ? extends U> biFunction = this.transformer;
            if (biFunction != null && (consumer = this.action) != null) {
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
    }

    static final class SearchKeysTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<? super K, ? extends U> searchFunction;

        SearchKeysTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Function<? super K, ? extends U> function, AtomicReference<U> atomicReference) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.searchFunction = function;
            this.result = atomicReference;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> atomicReference;
            Function<? super K, ? extends U> function = this.searchFunction;
            if (function != null && (atomicReference = this.result) != null) {
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
                        if (atomicReference.compareAndSet(null, uApply)) {
                            quietlyCompleteRoot();
                            return;
                        }
                        return;
                    }
                }
            }
        }
    }

    static final class SearchValuesTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<? super V, ? extends U> searchFunction;

        SearchValuesTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Function<? super V, ? extends U> function, AtomicReference<U> atomicReference) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.searchFunction = function;
            this.result = atomicReference;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> atomicReference;
            Function<? super V, ? extends U> function = this.searchFunction;
            if (function != null && (atomicReference = this.result) != null) {
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
                        if (atomicReference.compareAndSet(null, uApply)) {
                            quietlyCompleteRoot();
                            return;
                        }
                        return;
                    }
                }
            }
        }
    }

    static final class SearchEntriesTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<Map.Entry<K, V>, ? extends U> searchFunction;

        SearchEntriesTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, Function<Map.Entry<K, V>, ? extends U> function, AtomicReference<U> atomicReference) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.searchFunction = function;
            this.result = atomicReference;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> atomicReference;
            Function<Map.Entry<K, V>, ? extends U> function = this.searchFunction;
            if (function != null && (atomicReference = this.result) != null) {
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
                    new SearchEntriesTask(this, i4, i3, i2, this.tab, function, atomicReference).fork();
                }
                while (atomicReference.get() == null) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        propagateCompletion();
                        return;
                    }
                    U uApply = function.apply(nodeAdvance);
                    if (uApply != null) {
                        if (atomicReference.compareAndSet(null, uApply)) {
                            quietlyCompleteRoot();
                            return;
                        }
                        return;
                    }
                }
            }
        }
    }

    static final class SearchMappingsTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final BiFunction<? super K, ? super V, ? extends U> searchFunction;

        SearchMappingsTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, BiFunction<? super K, ? super V, ? extends U> biFunction, AtomicReference<U> atomicReference) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.searchFunction = biFunction;
            this.result = atomicReference;
        }

        @Override
        public final U getRawResult() {
            return this.result.get();
        }

        @Override
        public final void compute() {
            AtomicReference<U> atomicReference;
            BiFunction<? super K, ? super V, ? extends U> biFunction = this.searchFunction;
            if (biFunction != null && (atomicReference = this.result) != null) {
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
                        if (atomicReference.compareAndSet(null, uApply)) {
                            quietlyCompleteRoot();
                            return;
                        }
                        return;
                    }
                }
            }
        }
    }

    static final class ReduceKeysTask<K, V> extends BulkTask<K, V, K> {
        ReduceKeysTask<K, V> nextRight;
        final BiFunction<? super K, ? super K, ? extends K> reducer;
        K result;
        ReduceKeysTask<K, V> rights;

        ReduceKeysTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, ReduceKeysTask<K, V> reduceKeysTask, BiFunction<? super K, ? super K, ? extends K> biFunction) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = reduceKeysTask;
            this.reducer = biFunction;
        }

        @Override
        public final K getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super K, ? super K, ? extends K> biFunction = this.reducer;
            if (biFunction != null) {
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
    }

    static final class ReduceValuesTask<K, V> extends BulkTask<K, V, V> {
        ReduceValuesTask<K, V> nextRight;
        final BiFunction<? super V, ? super V, ? extends V> reducer;
        V result;
        ReduceValuesTask<K, V> rights;

        ReduceValuesTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, ReduceValuesTask<K, V> reduceValuesTask, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = reduceValuesTask;
            this.reducer = biFunction;
        }

        @Override
        public final V getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super V, ? super V, ? extends V> biFunction = this.reducer;
            if (biFunction != null) {
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
                    vApply = vApply != null ? biFunction.apply(vApply, v) : v;
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
    }

    static final class ReduceEntriesTask<K, V> extends BulkTask<K, V, Map.Entry<K, V>> {
        ReduceEntriesTask<K, V> nextRight;
        final BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> reducer;
        Map.Entry<K, V> result;
        ReduceEntriesTask<K, V> rights;

        ReduceEntriesTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, ReduceEntriesTask<K, V> reduceEntriesTask, BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> biFunction) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = reduceEntriesTask;
            this.reducer = biFunction;
        }

        @Override
        public final Map.Entry<K, V> getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>> biFunction = this.reducer;
            if (biFunction != null) {
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
                    ReduceEntriesTask<K, V> reduceEntriesTask = new ReduceEntriesTask<>(this, i4, i3, i2, this.tab, this.rights, biFunction);
                    this.rights = reduceEntriesTask;
                    reduceEntriesTask.fork();
                }
                Map.Entry<K, V> entryApply = null;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else if (entryApply == null) {
                        entryApply = nodeAdvance;
                    } else {
                        entryApply = biFunction.apply(entryApply, nodeAdvance);
                    }
                }
                this.result = entryApply;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    ReduceEntriesTask reduceEntriesTask2 = (ReduceEntriesTask) countedCompleterFirstComplete;
                    ReduceEntriesTask<K, V> reduceEntriesTask3 = reduceEntriesTask2.rights;
                    while (reduceEntriesTask3 != null) {
                        Map.Entry<K, V> entryApply2 = reduceEntriesTask3.result;
                        if (entryApply2 != null) {
                            Map.Entry<K, V> entry = reduceEntriesTask2.result;
                            if (entry != null) {
                                entryApply2 = biFunction.apply(entry, entryApply2);
                            }
                            reduceEntriesTask2.result = entryApply2;
                        }
                        reduceEntriesTask3 = reduceEntriesTask3.nextRight;
                        reduceEntriesTask2.rights = reduceEntriesTask3;
                    }
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

        MapReduceKeysTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceKeysTask<K, V, U> mapReduceKeysTask, Function<? super K, ? extends U> function, BiFunction<? super U, ? super U, ? extends U> biFunction) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceKeysTask;
            this.transformer = function;
            this.reducer = biFunction;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            Function<? super K, ? extends U> function = this.transformer;
            if (function != null && (biFunction = this.reducer) != null) {
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
                U u = null;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    }
                    U uApply = function.apply(nodeAdvance.key);
                    if (uApply != null) {
                        if (u != null) {
                            uApply = biFunction.apply(u, uApply);
                        }
                        u = (U) uApply;
                    }
                }
                this.result = u;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    MapReduceKeysTask mapReduceKeysTask2 = (MapReduceKeysTask) countedCompleterFirstComplete;
                    MapReduceKeysTask<K, V, U> mapReduceKeysTask3 = mapReduceKeysTask2.rights;
                    while (mapReduceKeysTask3 != null) {
                        U uApply2 = mapReduceKeysTask3.result;
                        if (uApply2 != null) {
                            U u2 = mapReduceKeysTask2.result;
                            if (u2 != null) {
                                uApply2 = biFunction.apply(u2, uApply2);
                            }
                            mapReduceKeysTask2.result = (U) uApply2;
                        }
                        mapReduceKeysTask3 = mapReduceKeysTask3.nextRight;
                        mapReduceKeysTask2.rights = mapReduceKeysTask3;
                    }
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

        MapReduceValuesTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceValuesTask<K, V, U> mapReduceValuesTask, Function<? super V, ? extends U> function, BiFunction<? super U, ? super U, ? extends U> biFunction) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceValuesTask;
            this.transformer = function;
            this.reducer = biFunction;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            Function<? super V, ? extends U> function = this.transformer;
            if (function != null && (biFunction = this.reducer) != null) {
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
                U u = null;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    }
                    U uApply = function.apply(nodeAdvance.val);
                    if (uApply != null) {
                        if (u != null) {
                            uApply = biFunction.apply(u, uApply);
                        }
                        u = (U) uApply;
                    }
                }
                this.result = u;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    MapReduceValuesTask mapReduceValuesTask2 = (MapReduceValuesTask) countedCompleterFirstComplete;
                    MapReduceValuesTask<K, V, U> mapReduceValuesTask3 = mapReduceValuesTask2.rights;
                    while (mapReduceValuesTask3 != null) {
                        U uApply2 = mapReduceValuesTask3.result;
                        if (uApply2 != null) {
                            U u2 = mapReduceValuesTask2.result;
                            if (u2 != null) {
                                uApply2 = biFunction.apply(u2, uApply2);
                            }
                            mapReduceValuesTask2.result = (U) uApply2;
                        }
                        mapReduceValuesTask3 = mapReduceValuesTask3.nextRight;
                        mapReduceValuesTask2.rights = mapReduceValuesTask3;
                    }
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

        MapReduceEntriesTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceEntriesTask<K, V, U> mapReduceEntriesTask, Function<Map.Entry<K, V>, ? extends U> function, BiFunction<? super U, ? super U, ? extends U> biFunction) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceEntriesTask;
            this.transformer = function;
            this.reducer = biFunction;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            Function<Map.Entry<K, V>, ? extends U> function = this.transformer;
            if (function != null && (biFunction = this.reducer) != null) {
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
                U u = null;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    }
                    U uApply = function.apply(nodeAdvance);
                    if (uApply != null) {
                        if (u != null) {
                            uApply = biFunction.apply(u, uApply);
                        }
                        u = (U) uApply;
                    }
                }
                this.result = u;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    MapReduceEntriesTask mapReduceEntriesTask2 = (MapReduceEntriesTask) countedCompleterFirstComplete;
                    MapReduceEntriesTask<K, V, U> mapReduceEntriesTask3 = mapReduceEntriesTask2.rights;
                    while (mapReduceEntriesTask3 != null) {
                        U uApply2 = mapReduceEntriesTask3.result;
                        if (uApply2 != null) {
                            U u2 = mapReduceEntriesTask2.result;
                            if (u2 != null) {
                                uApply2 = biFunction.apply(u2, uApply2);
                            }
                            mapReduceEntriesTask2.result = (U) uApply2;
                        }
                        mapReduceEntriesTask3 = mapReduceEntriesTask3.nextRight;
                        mapReduceEntriesTask2.rights = mapReduceEntriesTask3;
                    }
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

        MapReduceMappingsTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceMappingsTask<K, V, U> mapReduceMappingsTask, BiFunction<? super K, ? super V, ? extends U> biFunction, BiFunction<? super U, ? super U, ? extends U> biFunction2) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceMappingsTask;
            this.transformer = biFunction;
            this.reducer = biFunction2;
        }

        @Override
        public final U getRawResult() {
            return this.result;
        }

        @Override
        public final void compute() {
            BiFunction<? super U, ? super U, ? extends U> biFunction;
            BiFunction<? super K, ? super V, ? extends U> biFunction2 = this.transformer;
            if (biFunction2 != null && (biFunction = this.reducer) != null) {
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
                U u = null;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    }
                    U uApply = biFunction2.apply(nodeAdvance.key, nodeAdvance.val);
                    if (uApply != null) {
                        if (u != null) {
                            uApply = biFunction.apply(u, uApply);
                        }
                        u = (U) uApply;
                    }
                }
                this.result = u;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    MapReduceMappingsTask mapReduceMappingsTask2 = (MapReduceMappingsTask) countedCompleterFirstComplete;
                    MapReduceMappingsTask<K, V, U> mapReduceMappingsTask3 = mapReduceMappingsTask2.rights;
                    while (mapReduceMappingsTask3 != null) {
                        U uApply2 = mapReduceMappingsTask3.result;
                        if (uApply2 != null) {
                            U u2 = mapReduceMappingsTask2.result;
                            if (u2 != null) {
                                uApply2 = biFunction.apply(u2, uApply2);
                            }
                            mapReduceMappingsTask2.result = (U) uApply2;
                        }
                        mapReduceMappingsTask3 = mapReduceMappingsTask3.nextRight;
                        mapReduceMappingsTask2.rights = mapReduceMappingsTask3;
                    }
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

        MapReduceKeysToDoubleTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceKeysToDoubleTask<K, V> mapReduceKeysToDoubleTask, ToDoubleFunction<? super K> toDoubleFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceKeysToDoubleTask;
            this.transformer = toDoubleFunction;
            this.basis = d;
            this.reducer = doubleBinaryOperator;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator doubleBinaryOperator;
            ToDoubleFunction<? super K> toDoubleFunction = this.transformer;
            if (toDoubleFunction != null && (doubleBinaryOperator = this.reducer) != null) {
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
                    toDoubleFunction = toDoubleFunction;
                    i = i;
                }
                ToDoubleFunction<? super K> toDoubleFunction2 = toDoubleFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, toDoubleFunction2.applyAsDouble(nodeAdvance.key));
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
    }

    static final class MapReduceValuesToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceValuesToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceValuesToDoubleTask<K, V> rights;
        final ToDoubleFunction<? super V> transformer;

        MapReduceValuesToDoubleTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceValuesToDoubleTask<K, V> mapReduceValuesToDoubleTask, ToDoubleFunction<? super V> toDoubleFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceValuesToDoubleTask;
            this.transformer = toDoubleFunction;
            this.basis = d;
            this.reducer = doubleBinaryOperator;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator doubleBinaryOperator;
            ToDoubleFunction<? super V> toDoubleFunction = this.transformer;
            if (toDoubleFunction != null && (doubleBinaryOperator = this.reducer) != null) {
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
                    toDoubleFunction = toDoubleFunction;
                    i = i;
                }
                ToDoubleFunction<? super V> toDoubleFunction2 = toDoubleFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, toDoubleFunction2.applyAsDouble(nodeAdvance.val));
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
    }

    static final class MapReduceEntriesToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceEntriesToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceEntriesToDoubleTask<K, V> rights;
        final ToDoubleFunction<Map.Entry<K, V>> transformer;

        MapReduceEntriesToDoubleTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceEntriesToDoubleTask<K, V> mapReduceEntriesToDoubleTask, ToDoubleFunction<Map.Entry<K, V>> toDoubleFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceEntriesToDoubleTask;
            this.transformer = toDoubleFunction;
            this.basis = d;
            this.reducer = doubleBinaryOperator;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator doubleBinaryOperator;
            ToDoubleFunction<Map.Entry<K, V>> toDoubleFunction = this.transformer;
            if (toDoubleFunction != null && (doubleBinaryOperator = this.reducer) != null) {
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
                    MapReduceEntriesToDoubleTask<K, V> mapReduceEntriesToDoubleTask = new MapReduceEntriesToDoubleTask<>(this, i4, i3, i2, this.tab, this.rights, toDoubleFunction, dApplyAsDouble, doubleBinaryOperator);
                    this.rights = mapReduceEntriesToDoubleTask;
                    mapReduceEntriesToDoubleTask.fork();
                    toDoubleFunction = toDoubleFunction;
                    i = i;
                }
                ToDoubleFunction<Map.Entry<K, V>> toDoubleFunction2 = toDoubleFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, toDoubleFunction2.applyAsDouble(nodeAdvance));
                    }
                }
                this.result = dApplyAsDouble;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    MapReduceEntriesToDoubleTask mapReduceEntriesToDoubleTask2 = (MapReduceEntriesToDoubleTask) countedCompleterFirstComplete;
                    MapReduceEntriesToDoubleTask<K, V> mapReduceEntriesToDoubleTask3 = mapReduceEntriesToDoubleTask2.rights;
                    while (mapReduceEntriesToDoubleTask3 != null) {
                        mapReduceEntriesToDoubleTask2.result = doubleBinaryOperator.applyAsDouble(mapReduceEntriesToDoubleTask2.result, mapReduceEntriesToDoubleTask3.result);
                        mapReduceEntriesToDoubleTask3 = mapReduceEntriesToDoubleTask3.nextRight;
                        mapReduceEntriesToDoubleTask2.rights = mapReduceEntriesToDoubleTask3;
                    }
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

        MapReduceMappingsToDoubleTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceMappingsToDoubleTask<K, V> mapReduceMappingsToDoubleTask, ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction, double d, DoubleBinaryOperator doubleBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceMappingsToDoubleTask;
            this.transformer = toDoubleBiFunction;
            this.basis = d;
            this.reducer = doubleBinaryOperator;
        }

        @Override
        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        @Override
        public final void compute() {
            DoubleBinaryOperator doubleBinaryOperator;
            ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction = this.transformer;
            if (toDoubleBiFunction != null && (doubleBinaryOperator = this.reducer) != null) {
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
                    toDoubleBiFunction = toDoubleBiFunction;
                    i = i;
                }
                ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction2 = toDoubleBiFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, toDoubleBiFunction2.applyAsDouble(nodeAdvance.key, nodeAdvance.val));
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
    }

    static final class MapReduceKeysToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceKeysToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceKeysToLongTask<K, V> rights;
        final ToLongFunction<? super K> transformer;

        MapReduceKeysToLongTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceKeysToLongTask<K, V> mapReduceKeysToLongTask, ToLongFunction<? super K> toLongFunction, long j, LongBinaryOperator longBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceKeysToLongTask;
            this.transformer = toLongFunction;
            this.basis = j;
            this.reducer = longBinaryOperator;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator longBinaryOperator;
            ToLongFunction<? super K> toLongFunction = this.transformer;
            if (toLongFunction != null && (longBinaryOperator = this.reducer) != null) {
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
                    toLongFunction = toLongFunction;
                    i = i;
                }
                ToLongFunction<? super K> toLongFunction2 = toLongFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, toLongFunction2.applyAsLong(nodeAdvance.key));
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
    }

    static final class MapReduceValuesToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceValuesToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceValuesToLongTask<K, V> rights;
        final ToLongFunction<? super V> transformer;

        MapReduceValuesToLongTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceValuesToLongTask<K, V> mapReduceValuesToLongTask, ToLongFunction<? super V> toLongFunction, long j, LongBinaryOperator longBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceValuesToLongTask;
            this.transformer = toLongFunction;
            this.basis = j;
            this.reducer = longBinaryOperator;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator longBinaryOperator;
            ToLongFunction<? super V> toLongFunction = this.transformer;
            if (toLongFunction != null && (longBinaryOperator = this.reducer) != null) {
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
                    toLongFunction = toLongFunction;
                    i = i;
                }
                ToLongFunction<? super V> toLongFunction2 = toLongFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, toLongFunction2.applyAsLong(nodeAdvance.val));
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
    }

    static final class MapReduceEntriesToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceEntriesToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceEntriesToLongTask<K, V> rights;
        final ToLongFunction<Map.Entry<K, V>> transformer;

        MapReduceEntriesToLongTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceEntriesToLongTask<K, V> mapReduceEntriesToLongTask, ToLongFunction<Map.Entry<K, V>> toLongFunction, long j, LongBinaryOperator longBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceEntriesToLongTask;
            this.transformer = toLongFunction;
            this.basis = j;
            this.reducer = longBinaryOperator;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator longBinaryOperator;
            ToLongFunction<Map.Entry<K, V>> toLongFunction = this.transformer;
            if (toLongFunction != null && (longBinaryOperator = this.reducer) != null) {
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
                    MapReduceEntriesToLongTask<K, V> mapReduceEntriesToLongTask = new MapReduceEntriesToLongTask<>(this, i4, i3, i2, this.tab, this.rights, toLongFunction, jApplyAsLong, longBinaryOperator);
                    this.rights = mapReduceEntriesToLongTask;
                    mapReduceEntriesToLongTask.fork();
                    toLongFunction = toLongFunction;
                    i = i;
                }
                ToLongFunction<Map.Entry<K, V>> toLongFunction2 = toLongFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, toLongFunction2.applyAsLong(nodeAdvance));
                    }
                }
                this.result = jApplyAsLong;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    MapReduceEntriesToLongTask mapReduceEntriesToLongTask2 = (MapReduceEntriesToLongTask) countedCompleterFirstComplete;
                    MapReduceEntriesToLongTask<K, V> mapReduceEntriesToLongTask3 = mapReduceEntriesToLongTask2.rights;
                    while (mapReduceEntriesToLongTask3 != null) {
                        mapReduceEntriesToLongTask2.result = longBinaryOperator.applyAsLong(mapReduceEntriesToLongTask2.result, mapReduceEntriesToLongTask3.result);
                        mapReduceEntriesToLongTask3 = mapReduceEntriesToLongTask3.nextRight;
                        mapReduceEntriesToLongTask2.rights = mapReduceEntriesToLongTask3;
                    }
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

        MapReduceMappingsToLongTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceMappingsToLongTask<K, V> mapReduceMappingsToLongTask, ToLongBiFunction<? super K, ? super V> toLongBiFunction, long j, LongBinaryOperator longBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceMappingsToLongTask;
            this.transformer = toLongBiFunction;
            this.basis = j;
            this.reducer = longBinaryOperator;
        }

        @Override
        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        @Override
        public final void compute() {
            LongBinaryOperator longBinaryOperator;
            ToLongBiFunction<? super K, ? super V> toLongBiFunction = this.transformer;
            if (toLongBiFunction != null && (longBinaryOperator = this.reducer) != null) {
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
                    toLongBiFunction = toLongBiFunction;
                    i = i;
                }
                ToLongBiFunction<? super K, ? super V> toLongBiFunction2 = toLongBiFunction;
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, toLongBiFunction2.applyAsLong(nodeAdvance.key, nodeAdvance.val));
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
    }

    static final class MapReduceKeysToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceKeysToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceKeysToIntTask<K, V> rights;
        final ToIntFunction<? super K> transformer;

        MapReduceKeysToIntTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceKeysToIntTask<K, V> mapReduceKeysToIntTask, ToIntFunction<? super K> toIntFunction, int i4, IntBinaryOperator intBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceKeysToIntTask;
            this.transformer = toIntFunction;
            this.basis = i4;
            this.reducer = intBinaryOperator;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator intBinaryOperator;
            ToIntFunction<? super K> toIntFunction = this.transformer;
            if (toIntFunction != null && (intBinaryOperator = this.reducer) != null) {
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
    }

    static final class MapReduceValuesToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceValuesToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceValuesToIntTask<K, V> rights;
        final ToIntFunction<? super V> transformer;

        MapReduceValuesToIntTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceValuesToIntTask<K, V> mapReduceValuesToIntTask, ToIntFunction<? super V> toIntFunction, int i4, IntBinaryOperator intBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceValuesToIntTask;
            this.transformer = toIntFunction;
            this.basis = i4;
            this.reducer = intBinaryOperator;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator intBinaryOperator;
            ToIntFunction<? super V> toIntFunction = this.transformer;
            if (toIntFunction != null && (intBinaryOperator = this.reducer) != null) {
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
    }

    static final class MapReduceEntriesToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceEntriesToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceEntriesToIntTask<K, V> rights;
        final ToIntFunction<Map.Entry<K, V>> transformer;

        MapReduceEntriesToIntTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceEntriesToIntTask<K, V> mapReduceEntriesToIntTask, ToIntFunction<Map.Entry<K, V>> toIntFunction, int i4, IntBinaryOperator intBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceEntriesToIntTask;
            this.transformer = toIntFunction;
            this.basis = i4;
            this.reducer = intBinaryOperator;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator intBinaryOperator;
            ToIntFunction<Map.Entry<K, V>> toIntFunction = this.transformer;
            if (toIntFunction != null && (intBinaryOperator = this.reducer) != null) {
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
                    MapReduceEntriesToIntTask<K, V> mapReduceEntriesToIntTask = new MapReduceEntriesToIntTask<>(this, i4, i3, i2, this.tab, this.rights, toIntFunction, iApplyAsInt, intBinaryOperator);
                    this.rights = mapReduceEntriesToIntTask;
                    mapReduceEntriesToIntTask.fork();
                }
                while (true) {
                    Node<K, V> nodeAdvance = advance();
                    if (nodeAdvance == null) {
                        break;
                    } else {
                        iApplyAsInt = intBinaryOperator.applyAsInt(iApplyAsInt, toIntFunction.applyAsInt(nodeAdvance));
                    }
                }
                this.result = iApplyAsInt;
                for (CountedCompleter<?> countedCompleterFirstComplete = firstComplete(); countedCompleterFirstComplete != null; countedCompleterFirstComplete = countedCompleterFirstComplete.nextComplete()) {
                    MapReduceEntriesToIntTask mapReduceEntriesToIntTask2 = (MapReduceEntriesToIntTask) countedCompleterFirstComplete;
                    MapReduceEntriesToIntTask<K, V> mapReduceEntriesToIntTask3 = mapReduceEntriesToIntTask2.rights;
                    while (mapReduceEntriesToIntTask3 != null) {
                        mapReduceEntriesToIntTask2.result = intBinaryOperator.applyAsInt(mapReduceEntriesToIntTask2.result, mapReduceEntriesToIntTask3.result);
                        mapReduceEntriesToIntTask3 = mapReduceEntriesToIntTask3.nextRight;
                        mapReduceEntriesToIntTask2.rights = mapReduceEntriesToIntTask3;
                    }
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

        MapReduceMappingsToIntTask(BulkTask<K, V, ?> bulkTask, int i, int i2, int i3, Node<K, V>[] nodeArr, MapReduceMappingsToIntTask<K, V> mapReduceMappingsToIntTask, ToIntBiFunction<? super K, ? super V> toIntBiFunction, int i4, IntBinaryOperator intBinaryOperator) {
            super(bulkTask, i, i2, i3, nodeArr);
            this.nextRight = mapReduceMappingsToIntTask;
            this.transformer = toIntBiFunction;
            this.basis = i4;
            this.reducer = intBinaryOperator;
        }

        @Override
        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        @Override
        public final void compute() {
            IntBinaryOperator intBinaryOperator;
            ToIntBiFunction<? super K, ? super V> toIntBiFunction = this.transformer;
            if (toIntBiFunction != null && (intBinaryOperator = this.reducer) != null) {
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
}
