package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {
    private Thread leader;
    private final transient ReentrantLock lock = new ReentrantLock();
    private final PriorityQueue<E> q = new PriorityQueue<>();
    private final Condition available = this.lock.newCondition();

    public DelayQueue() {
    }

    public DelayQueue(Collection<? extends E> collection) {
        addAll(collection);
    }

    @Override
    public boolean add(E e) {
        return offer((Delayed) e);
    }

    @Override
    public boolean offer(E e) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            this.q.offer(e);
            if (this.q.peek() == e) {
                this.leader = null;
                this.available.signal();
            }
            return true;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public void put(E e) {
        offer((Delayed) e);
    }

    @Override
    public boolean offer(E e, long j, TimeUnit timeUnit) {
        return offer((Delayed) e);
    }

    @Override
    public E poll() {
        E ePoll;
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            E ePeek = this.q.peek();
            if (ePeek == null || ePeek.getDelay(TimeUnit.NANOSECONDS) > 0) {
                ePoll = null;
            } else {
                ePoll = this.q.poll();
            }
            return ePoll;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (true) {
            try {
                E ePeek = this.q.peek();
                if (ePeek == null) {
                    this.available.await();
                } else {
                    long delay = ePeek.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0) {
                        break;
                    }
                    if (this.leader != null) {
                        this.available.await();
                    } else {
                        Thread threadCurrentThread = Thread.currentThread();
                        this.leader = threadCurrentThread;
                        try {
                            this.available.awaitNanos(delay);
                        } finally {
                            if (this.leader == threadCurrentThread) {
                                this.leader = null;
                            }
                        }
                    }
                }
            } finally {
                if (this.leader == null && this.q.peek() != null) {
                    this.available.signal();
                }
                reentrantLock.unlock();
            }
        }
        return this.q.poll();
    }

    @Override
    public E poll(long j, TimeUnit timeUnit) throws InterruptedException {
        long nanos = timeUnit.toNanos(j);
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (true) {
            try {
                E ePeek = this.q.peek();
                if (ePeek != null) {
                    long delay = ePeek.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0) {
                        E ePoll = this.q.poll();
                        if (this.leader == null && this.q.peek() != null) {
                            this.available.signal();
                        }
                        reentrantLock.unlock();
                        return ePoll;
                    }
                    if (nanos <= 0) {
                        if (this.leader == null && this.q.peek() != null) {
                            this.available.signal();
                        }
                        reentrantLock.unlock();
                        return null;
                    }
                    if (nanos < delay || this.leader != null) {
                        nanos = this.available.awaitNanos(nanos);
                    } else {
                        Thread threadCurrentThread = Thread.currentThread();
                        this.leader = threadCurrentThread;
                        try {
                            nanos -= delay - this.available.awaitNanos(delay);
                            if (this.leader == threadCurrentThread) {
                                this.leader = null;
                            }
                        } catch (Throwable th) {
                            if (this.leader == threadCurrentThread) {
                                this.leader = null;
                            }
                            throw th;
                        }
                    }
                } else {
                    if (nanos <= 0) {
                        return null;
                    }
                    nanos = this.available.awaitNanos(nanos);
                }
            } finally {
                if (this.leader == null && this.q.peek() != null) {
                    this.available.signal();
                }
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public E peek() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.q.peek();
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public int size() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.q.size();
        } finally {
            reentrantLock.unlock();
        }
    }

    private E peekExpired() {
        E ePeek = this.q.peek();
        if (ePeek != null && ePeek.getDelay(TimeUnit.NANOSECONDS) <= 0) {
            return ePeek;
        }
        return null;
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        int i = 0;
        while (true) {
            try {
                Delayed delayedPeekExpired = peekExpired();
                if (delayedPeekExpired != null) {
                    collection.add(delayedPeekExpired);
                    this.q.poll();
                    i++;
                } else {
                    return i;
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public int drainTo(Collection<? super E> collection, int i) {
        if (collection == null) {
            throw new NullPointerException();
        }
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        int i2 = 0;
        if (i <= 0) {
            return 0;
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        while (i2 < i) {
            try {
                Delayed delayedPeekExpired = peekExpired();
                if (delayedPeekExpired == null) {
                    break;
                }
                collection.add(delayedPeekExpired);
                this.q.poll();
                i2++;
            } finally {
                reentrantLock.unlock();
            }
        }
        return i2;
    }

    @Override
    public void clear() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            this.q.clear();
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Object[] toArray() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.q.toArray();
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return (T[]) this.q.toArray(tArr);
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public boolean remove(Object obj) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.q.remove(obj);
        } finally {
            reentrantLock.unlock();
        }
    }

    void removeEQ(Object obj) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            Iterator<E> it = this.q.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                } else if (obj == it.next()) {
                    break;
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    private class Itr implements Iterator<E> {
        final Object[] array;
        int cursor;
        int lastRet = -1;

        Itr(Object[] objArr) {
            this.array = objArr;
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.array.length;
        }

        @Override
        public E next() {
            if (this.cursor >= this.array.length) {
                throw new NoSuchElementException();
            }
            this.lastRet = this.cursor;
            Object[] objArr = this.array;
            int i = this.cursor;
            this.cursor = i + 1;
            return (E) objArr[i];
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            DelayQueue.this.removeEQ(this.array[this.lastRet]);
            this.lastRet = -1;
        }
    }
}
