package java.util.concurrent;

import java.util.Deque;
import java.util.Iterator;

public interface BlockingDeque<E> extends BlockingQueue<E>, Deque<E> {
    @Override
    boolean add(E e);

    @Override
    void addFirst(E e);

    @Override
    void addLast(E e);

    @Override
    boolean contains(Object obj);

    @Override
    E element();

    @Override
    Iterator<E> iterator();

    @Override
    boolean offer(E e);

    @Override
    boolean offer(E e, long j, TimeUnit timeUnit) throws InterruptedException;

    @Override
    boolean offerFirst(E e);

    boolean offerFirst(E e, long j, TimeUnit timeUnit) throws InterruptedException;

    @Override
    boolean offerLast(E e);

    boolean offerLast(E e, long j, TimeUnit timeUnit) throws InterruptedException;

    @Override
    E peek();

    @Override
    E poll();

    @Override
    E poll(long j, TimeUnit timeUnit) throws InterruptedException;

    E pollFirst(long j, TimeUnit timeUnit) throws InterruptedException;

    E pollLast(long j, TimeUnit timeUnit) throws InterruptedException;

    @Override
    void push(E e);

    @Override
    void put(E e) throws InterruptedException;

    void putFirst(E e) throws InterruptedException;

    void putLast(E e) throws InterruptedException;

    @Override
    E remove();

    @Override
    boolean remove(Object obj);

    @Override
    boolean removeFirstOccurrence(Object obj);

    @Override
    boolean removeLastOccurrence(Object obj);

    @Override
    int size();

    @Override
    E take() throws InterruptedException;

    E takeFirst() throws InterruptedException;

    E takeLast() throws InterruptedException;
}
