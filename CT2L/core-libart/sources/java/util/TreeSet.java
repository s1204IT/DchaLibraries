package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, Serializable {
    private static final long serialVersionUID = -2479143000061671589L;
    private transient NavigableMap<E, Object> backingMap;
    private transient NavigableSet<E> descendingSet;

    TreeSet(NavigableMap<E, Object> map) {
        this.backingMap = map;
    }

    public TreeSet() {
        this.backingMap = new TreeMap();
    }

    public TreeSet(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    public TreeSet(Comparator<? super E> comparator) {
        this.backingMap = new TreeMap(comparator);
    }

    public TreeSet(SortedSet<E> set) {
        this(set.comparator());
        Iterator<E> it = set.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
    }

    @Override
    public boolean add(E object) {
        return this.backingMap.put(object, Boolean.TRUE) == null;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return super.addAll(collection);
    }

    @Override
    public void clear() {
        this.backingMap.clear();
    }

    public Object clone() {
        try {
            TreeSet<E> clone = (TreeSet) super.clone();
            if (this.backingMap instanceof TreeMap) {
                clone.backingMap = (NavigableMap) ((TreeMap) this.backingMap).clone();
            } else {
                clone.backingMap = new TreeMap((SortedMap) this.backingMap);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.backingMap.comparator();
    }

    @Override
    public boolean contains(Object object) {
        return this.backingMap.containsKey(object);
    }

    @Override
    public boolean isEmpty() {
        return this.backingMap.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return this.backingMap.keySet().iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public boolean remove(Object object) {
        return this.backingMap.remove(object) != null;
    }

    @Override
    public int size() {
        return this.backingMap.size();
    }

    @Override
    public E first() {
        return this.backingMap.firstKey();
    }

    @Override
    public E last() {
        return this.backingMap.lastKey();
    }

    @Override
    public E pollFirst() {
        Map.Entry<E, Object> entry = this.backingMap.pollFirstEntry();
        if (entry == null) {
            return null;
        }
        return entry.getKey();
    }

    @Override
    public E pollLast() {
        Map.Entry<E, Object> entry = this.backingMap.pollLastEntry();
        if (entry == null) {
            return null;
        }
        return entry.getKey();
    }

    @Override
    public E higher(E e) {
        return this.backingMap.higherKey(e);
    }

    @Override
    public E lower(E e) {
        return this.backingMap.lowerKey(e);
    }

    @Override
    public E ceiling(E e) {
        return this.backingMap.ceilingKey(e);
    }

    @Override
    public E floor(E e) {
        return this.backingMap.floorKey(e);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        if (this.descendingSet != null) {
            return this.descendingSet;
        }
        TreeSet treeSet = new TreeSet(this.backingMap.descendingMap());
        this.descendingSet = treeSet;
        return treeSet;
    }

    @Override
    public NavigableSet<E> subSet(E start, boolean startInclusive, E end, boolean endInclusive) {
        Comparator<? super E> c = this.backingMap.comparator();
        int compare = c == null ? ((Comparable) start).compareTo(end) : c.compare(start, end);
        if (compare <= 0) {
            return new TreeSet(this.backingMap.subMap(start, startInclusive, end, endInclusive));
        }
        throw new IllegalArgumentException();
    }

    @Override
    public NavigableSet<E> headSet(E end, boolean endInclusive) {
        Comparator<? super E> c = this.backingMap.comparator();
        if (c == null) {
            ((Comparable) end).compareTo(end);
        } else {
            c.compare(end, end);
        }
        return new TreeSet(this.backingMap.headMap(end, endInclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E start, boolean startInclusive) {
        Comparator<? super E> c = this.backingMap.comparator();
        if (c == null) {
            ((Comparable) start).compareTo(start);
        } else {
            c.compare(start, start);
        }
        return new TreeSet(this.backingMap.tailMap(start, startInclusive));
    }

    @Override
    public SortedSet<E> subSet(E start, E end) {
        return subSet(start, true, end, false);
    }

    @Override
    public SortedSet<E> headSet(E end) {
        return headSet(end, false);
    }

    @Override
    public SortedSet<E> tailSet(E start) {
        return tailSet(start, true);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(this.backingMap.comparator());
        int size = this.backingMap.size();
        stream.writeInt(size);
        if (size > 0) {
            Iterator<E> it = this.backingMap.keySet().iterator();
            while (it.hasNext()) {
                stream.writeObject(it.next());
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        TreeMap treeMap = new TreeMap((Comparator) stream.readObject());
        int size = stream.readInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                treeMap.put(stream.readObject(), Boolean.TRUE);
            }
        }
        this.backingMap = treeMap;
    }
}
