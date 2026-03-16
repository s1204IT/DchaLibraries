package java.util.concurrent;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements Serializable {
    private static final long serialVersionUID = 5457747651344034263L;
    private final CopyOnWriteArrayList<E> al = new CopyOnWriteArrayList<>();

    public CopyOnWriteArraySet() {
    }

    public CopyOnWriteArraySet(Collection<? extends E> c) {
        this.al.addAllAbsent(c);
    }

    @Override
    public int size() {
        return this.al.size();
    }

    @Override
    public boolean isEmpty() {
        return this.al.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.al.contains(o);
    }

    @Override
    public Object[] toArray() {
        return this.al.toArray();
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        return (T[]) this.al.toArray(tArr);
    }

    @Override
    public void clear() {
        this.al.clear();
    }

    @Override
    public boolean remove(Object o) {
        return this.al.remove(o);
    }

    @Override
    public boolean add(E e) {
        return this.al.addIfAbsent(e);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.al.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return this.al.addAllAbsent(c) > 0;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.al.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.al.retainAll(c);
    }

    @Override
    public Iterator<E> iterator() {
        return this.al.iterator();
    }

    @Override
    public boolean equals(Object o) {
        int i;
        if (o == this) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        Set<?> set = (Set) o;
        Object[] elements = this.al.getArray();
        int len = elements.length;
        boolean[] matched = new boolean[len];
        int k = 0;
        for (Object x : set) {
            k++;
            if (k > len) {
                return false;
            }
            while (i < len) {
                i = (matched[i] || !eq(x, elements[i])) ? i + 1 : 0;
            }
            return false;
        }
        return k == len;
    }

    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }
}
