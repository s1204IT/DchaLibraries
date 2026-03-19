package java.util.concurrent;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentSkipListMap;
import sun.misc.Unsafe;

public class ConcurrentSkipListSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, Serializable {
    private static final long MAP;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = -2479143111061671589L;
    private final ConcurrentNavigableMap<E, Object> m;

    public ConcurrentSkipListSet() {
        this.m = new ConcurrentSkipListMap();
    }

    public ConcurrentSkipListSet(Comparator<? super E> comparator) {
        this.m = new ConcurrentSkipListMap(comparator);
    }

    public ConcurrentSkipListSet(Collection<? extends E> collection) {
        this.m = new ConcurrentSkipListMap();
        addAll(collection);
    }

    public ConcurrentSkipListSet(SortedSet<E> sortedSet) {
        this.m = new ConcurrentSkipListMap(sortedSet.comparator());
        addAll(sortedSet);
    }

    ConcurrentSkipListSet(ConcurrentNavigableMap<E, Object> concurrentNavigableMap) {
        this.m = concurrentNavigableMap;
    }

    public ConcurrentSkipListSet<E> clone() {
        try {
            ConcurrentSkipListSet<E> concurrentSkipListSet = (ConcurrentSkipListSet) super.clone();
            concurrentSkipListSet.setMap(new ConcurrentSkipListMap((SortedMap) this.m));
            return concurrentSkipListSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
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
        return this.m.putIfAbsent(e, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object obj) {
        return this.m.remove(obj, Boolean.TRUE);
    }

    @Override
    public void clear() {
        this.m.clear();
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
    public boolean removeAll(Collection<?> collection) {
        Iterator<?> it = collection.iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (remove(it.next())) {
                z = true;
            }
        }
        return z;
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
    public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
        return new ConcurrentSkipListSet(this.m.subMap(e, z, e2, z2));
    }

    @Override
    public NavigableSet<E> headSet(E e, boolean z) {
        return new ConcurrentSkipListSet(this.m.headMap(e, z));
    }

    @Override
    public NavigableSet<E> tailSet(E e, boolean z) {
        return new ConcurrentSkipListSet(this.m.tailMap(e, z));
    }

    @Override
    public NavigableSet<E> subSet(E e, E e2) {
        return subSet(e, true, e2, false);
    }

    @Override
    public NavigableSet<E> headSet(E e) {
        return headSet(e, false);
    }

    @Override
    public NavigableSet<E> tailSet(E e) {
        return tailSet(e, true);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ConcurrentSkipListSet(this.m.descendingMap());
    }

    @Override
    public Spliterator<E> spliterator() {
        if (this.m instanceof ConcurrentSkipListMap) {
            return ((ConcurrentSkipListMap) this.m).keySpliterator();
        }
        ConcurrentSkipListMap.SubMap subMap = (ConcurrentSkipListMap.SubMap) this.m;
        Objects.requireNonNull(subMap);
        return new ConcurrentSkipListMap.SubMap.SubMapKeyIterator();
    }

    private void setMap(ConcurrentNavigableMap<E, Object> concurrentNavigableMap) {
        U.putObjectVolatile(this, MAP, concurrentNavigableMap);
    }

    static {
        try {
            MAP = U.objectFieldOffset(ConcurrentSkipListSet.class.getDeclaredField("m"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
