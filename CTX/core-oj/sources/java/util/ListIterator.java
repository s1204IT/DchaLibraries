package java.util;

public interface ListIterator<E> extends Iterator<E> {
    void add(E e);

    @Override
    boolean hasNext();

    boolean hasPrevious();

    @Override
    E next();

    int nextIndex();

    E previous();

    int previousIndex();

    @Override
    void remove();

    void set(E e);
}
