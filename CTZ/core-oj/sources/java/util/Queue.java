package java.util;

public interface Queue<E> extends Collection<E> {
    @Override
    boolean add(E e);

    E element();

    boolean offer(E e);

    E peek();

    E poll();

    E remove();
}
