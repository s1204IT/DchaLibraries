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

    public CopyOnWriteArraySet(Collection<? extends E> collection) {
        if (collection.getClass() == CopyOnWriteArraySet.class) {
            this.al = new CopyOnWriteArrayList<>(((CopyOnWriteArraySet) collection).al);
        } else {
            this.al = new CopyOnWriteArrayList<>();
            this.al.addAllAbsent(collection);
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
    public boolean contains(Object obj) {
        return this.al.contains(obj);
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
    public boolean remove(Object obj) {
        return this.al.remove(obj);
    }

    @Override
    public boolean add(E e) {
        return this.al.addIfAbsent(e);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (collection instanceof Set) {
            return compareSets(this.al.getArray(), (Set) collection) >= 0;
        }
        return this.al.containsAll(collection);
    }

    private static int compareSets(Object[] objArr, Set<?> set) {
        int length = objArr.length;
        boolean[] zArr = new boolean[length];
        int i = 0;
        for (Object obj : set) {
            for (int i2 = i; i2 < length; i2++) {
                if (!zArr[i2] && Objects.equals(obj, objArr[i2])) {
                    zArr[i2] = true;
                    if (i2 == i) {
                        do {
                            i++;
                            if (i < length) {
                            }
                        } while (zArr[i]);
                    }
                }
            }
            return -1;
        }
        return i == length ? 0 : 1;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return this.al.addAllAbsent(collection) > 0;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return this.al.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return this.al.retainAll(collection);
    }

    @Override
    public Iterator<E> iterator() {
        return this.al.iterator();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || ((obj instanceof Set) && compareSets(this.al.getArray(), (Set) obj) == 0);
    }

    @Override
    public boolean removeIf(Predicate<? super E> predicate) {
        return this.al.removeIf(predicate);
    }

    @Override
    public void forEach(Consumer<? super E> consumer) {
        this.al.forEach(consumer);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this.al.getArray(), 1025);
    }
}
