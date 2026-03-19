package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, Serializable {
    private static final Object PRESENT = new Object();
    private static final long serialVersionUID = -2479143000061671589L;
    private transient NavigableMap<E, Object> m;

    TreeSet(NavigableMap<E, Object> navigableMap) {
        this.m = navigableMap;
    }

    public TreeSet() {
        this(new TreeMap());
    }

    public TreeSet(Comparator<? super E> comparator) {
        this(new TreeMap(comparator));
    }

    public TreeSet(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    public TreeSet(SortedSet<E> sortedSet) {
        this(sortedSet.comparator());
        addAll(sortedSet);
    }

    @Override
    public Iterator<E> iterator() {
        return this.m.navigableKeySet().iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return this.m.descendingKeySet().iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new TreeSet(this.m.descendingMap());
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
    public boolean add(E e) {
        return this.m.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object obj) {
        return this.m.remove(obj) == PRESENT;
    }

    @Override
    public void clear() {
        this.m.clear();
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        SortedSet sortedSet;
        TreeMap treeMap;
        Comparator<? super E> comparator;
        Comparator<? super E> comparator2;
        if (this.m.size() == 0 && collection.size() > 0 && (collection instanceof SortedSet) && (this.m instanceof TreeMap) && ((comparator = (sortedSet = (SortedSet) collection).comparator()) == (comparator2 = (treeMap = (TreeMap) this.m).comparator()) || (comparator != null && comparator.equals(comparator2)))) {
            treeMap.addAllForTreeSet(sortedSet, PRESENT);
            return true;
        }
        return super.addAll(collection);
    }

    @Override
    public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
        return new TreeSet(this.m.subMap(e, z, e2, z2));
    }

    @Override
    public NavigableSet<E> headSet(E e, boolean z) {
        return new TreeSet(this.m.headMap(e, z));
    }

    @Override
    public NavigableSet<E> tailSet(E e, boolean z) {
        return new TreeSet(this.m.tailMap(e, z));
    }

    @Override
    public SortedSet<E> subSet(E e, E e2) {
        return subSet(e, true, e2, false);
    }

    @Override
    public SortedSet<E> headSet(E e) {
        return headSet(e, false);
    }

    @Override
    public SortedSet<E> tailSet(E e) {
        return tailSet(e, true);
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
    public E pollFirst() {
        Map.Entry<E, Object> entryPollFirstEntry = this.m.pollFirstEntry();
        if (entryPollFirstEntry == null) {
            return null;
        }
        return entryPollFirstEntry.getKey();
    }

    @Override
    public E pollLast() {
        Map.Entry<E, Object> entryPollLastEntry = this.m.pollLastEntry();
        if (entryPollLastEntry == null) {
            return null;
        }
        return entryPollLastEntry.getKey();
    }

    public Object clone() {
        try {
            TreeSet treeSet = (TreeSet) super.clone();
            treeSet.m = new TreeMap((SortedMap) this.m);
            return treeSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(this.m.comparator());
        objectOutputStream.writeInt(this.m.size());
        Iterator<E> it = this.m.keySet().iterator();
        while (it.hasNext()) {
            objectOutputStream.writeObject(it.next());
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        TreeMap treeMap = new TreeMap((Comparator) objectInputStream.readObject());
        this.m = treeMap;
        treeMap.readTreeSet(objectInputStream.readInt(), objectInputStream, PRESENT);
    }

    @Override
    public Spliterator<E> spliterator() {
        return TreeMap.keySpliteratorFor(this.m);
    }
}
