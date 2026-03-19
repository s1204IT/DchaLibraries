package java.util.concurrent;

import android.icu.text.DateFormat;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
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

    public ConcurrentSkipListSet(Collection<? extends E> c) {
        this.m = new ConcurrentSkipListMap();
        addAll(c);
    }

    public ConcurrentSkipListSet(SortedSet<E> s) {
        this.m = new ConcurrentSkipListMap(s.comparator());
        addAll(s);
    }

    ConcurrentSkipListSet(ConcurrentNavigableMap<E, Object> m) {
        this.m = m;
    }

    public ConcurrentSkipListSet<E> clone() {
        try {
            ConcurrentSkipListSet<E> clone = (ConcurrentSkipListSet) super.clone();
            clone.setMap(new ConcurrentSkipListMap((SortedMap) this.m));
            return clone;
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
    public boolean contains(Object o) {
        return this.m.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return this.m.putIfAbsent(e, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return this.m.remove(o, Boolean.TRUE);
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        Collection<?> c = (Collection) o;
        try {
            if (containsAll(c)) {
                return c.containsAll(this);
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object e : c) {
            if (remove(e)) {
                modified = true;
            }
        }
        return modified;
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
        Map.Entry<E, Object> e = this.m.pollFirstEntry();
        if (e == null) {
            return null;
        }
        return e.getKey();
    }

    @Override
    public E pollLast() {
        Map.Entry<E, Object> e = this.m.pollLastEntry();
        if (e == null) {
            return null;
        }
        return e.getKey();
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.m.comparator();
    }

    @Override
    public E first() {
        return (E) this.m.firstKey();
    }

    @Override
    public E last() {
        return (E) this.m.lastKey();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new ConcurrentSkipListSet(this.m.subMap(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new ConcurrentSkipListSet(this.m.headMap(toElement, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new ConcurrentSkipListSet(this.m.tailMap(fromElement, inclusive));
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
        return new ConcurrentSkipListSet(this.m.descendingMap());
    }

    @Override
    public Spliterator<E> spliterator() {
        if (this.m instanceof ConcurrentSkipListMap) {
            return ((ConcurrentSkipListMap) this.m).keySpliterator();
        }
        ConcurrentSkipListMap.SubMap subMap = (ConcurrentSkipListMap.SubMap) this.m;
        subMap.getClass();
        return new ConcurrentSkipListMap.SubMap.SubMapKeyIterator();
    }

    private void setMap(ConcurrentNavigableMap<E, Object> map) {
        U.putObjectVolatile(this, MAP, map);
    }

    static {
        try {
            MAP = U.objectFieldOffset(ConcurrentSkipListSet.class.getDeclaredField(DateFormat.MINUTE));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
