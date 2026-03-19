package java.util.concurrent;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements Serializable {
    private static final long serialVersionUID = 5457747651344034263L;
    private final CopyOnWriteArrayList<E> al;

    public CopyOnWriteArraySet() {
        this.al = new CopyOnWriteArrayList<>();
    }

    public CopyOnWriteArraySet(Collection<? extends E> c) {
        if (c.getClass() == CopyOnWriteArraySet.class) {
            CopyOnWriteArraySet<E> cc = (CopyOnWriteArraySet) c;
            this.al = new CopyOnWriteArrayList<>(cc.al);
        } else {
            this.al = new CopyOnWriteArrayList<>();
            this.al.addAllAbsent(c);
        }
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
        if (c instanceof Set) {
            return compareSets(this.al.getArray(), (Set) c) >= 0;
        }
        return this.al.containsAll(c);
    }

    private static int compareSets(Object[] snapshot, Set<?> set) {
        int len = snapshot.length;
        boolean[] matched = new boolean[len];
        int j = 0;
        for (Object x : set) {
            for (int i = j; i < len; i++) {
                if (!matched[i] && Objects.equals(x, snapshot[i])) {
                    matched[i] = true;
                    if (i == j) {
                        do {
                            j++;
                            if (j < len) {
                            }
                        } while (matched[j]);
                    }
                }
            }
            return -1;
        }
        return j == len ? 0 : 1;
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
        if (o != this) {
            return (o instanceof Set) && compareSets(this.al.getArray(), (Set) o) == 0;
        }
        return true;
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return this.al.removeIf(filter);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        this.al.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this.al.getArray(), 1025);
    }
}
