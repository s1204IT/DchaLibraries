package java.util;

public interface List<E> extends Collection<E> {
    void add(int i, E e);

    @Override
    boolean add(E e);

    boolean addAll(int i, Collection<? extends E> collection);

    @Override
    boolean addAll(Collection<? extends E> collection);

    @Override
    void clear();

    @Override
    boolean contains(Object obj);

    @Override
    boolean containsAll(Collection<?> collection);

    @Override
    boolean equals(Object obj);

    E get(int i);

    @Override
    int hashCode();

    int indexOf(Object obj);

    @Override
    boolean isEmpty();

    @Override
    Iterator<E> iterator();

    int lastIndexOf(Object obj);

    ListIterator<E> listIterator();

    ListIterator<E> listIterator(int i);

    E remove(int i);

    @Override
    boolean remove(Object obj);

    @Override
    boolean removeAll(Collection<?> collection);

    @Override
    boolean retainAll(Collection<?> collection);

    E set(int i, E e);

    int size();

    List<E> subList(int i, int i2);

    @Override
    Object[] toArray();

    @Override
    <T> T[] toArray(T[] tArr);
}
