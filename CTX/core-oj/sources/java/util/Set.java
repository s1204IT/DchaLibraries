package java.util;

public interface Set<E> extends Collection<E> {
    @Override
    boolean add(E e);

    @Override
    boolean addAll(Collection<? extends E> collection);

    void clear();

    boolean contains(Object obj);

    @Override
    boolean containsAll(Collection<?> collection);

    boolean equals(Object obj);

    int hashCode();

    boolean isEmpty();

    Iterator<E> iterator();

    boolean remove(Object obj);

    @Override
    boolean removeAll(Collection<?> collection);

    @Override
    boolean retainAll(Collection<?> collection);

    int size();

    @Override
    Object[] toArray();

    @Override
    <T> T[] toArray(T[] tArr);

    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 1);
    }
}
