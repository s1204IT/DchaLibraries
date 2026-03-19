package java.util.concurrent;

import java.util.Collection;
import java.util.Queue;

public interface BlockingQueue<E> extends Queue<E> {
    @Override
    boolean add(E e);

    @Override
    boolean contains(Object obj);

    int drainTo(Collection<? super E> collection);

    int drainTo(Collection<? super E> collection, int i);

    @Override
    boolean offer(E e);

    boolean offer(E e, long j, TimeUnit timeUnit) throws InterruptedException;

    E poll(long j, TimeUnit timeUnit) throws InterruptedException;

    void put(E e) throws InterruptedException;

    int remainingCapacity();

    @Override
    boolean remove(Object obj);

    E take() throws InterruptedException;
}
