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

    public DelayQueue(Collection<? extends E> c) {
        addAll(c);
    }

    @Override
    public boolean add(E e) {
        return offer((Delayed) e);
    }

    @Override
    public boolean offer(E e) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            this.q.offer(e);
            if (this.q.peek() == e) {
                this.leader = null;
                this.available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) {
        offer((Delayed) e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer((Delayed) e);
    }

    @Override
    public E poll() {
        E ePoll = null;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E first = this.q.peek();
            if (first != null && first.getDelay(TimeUnit.NANOSECONDS) <= 0) {
                ePoll = this.q.poll();
            }
            return ePoll;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (true) {
            try {
                E first = this.q.peek();
                if (first == null) {
                    this.available.await();
                } else {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0) {
                        break;
                    }
                    if (this.leader != null) {
                        this.available.await();
                    } else {
                        Thread thisThread = Thread.currentThread();
                        this.leader = thisThread;
                        try {
                            this.available.awaitNanos(delay);
                            if (this.leader == thisThread) {
                                this.leader = null;
                            }
                        } catch (Throwable th) {
                            if (this.leader == thisThread) {
                                this.leader = null;
                            }
                            throw th;
                        }
                    }
                }
            } finally {
                if (this.leader == null && this.q.peek() != null) {
                    this.available.signal();
                }
                lock.unlock();
            }
        }
        return this.q.poll();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (true) {
            try {
                E first = this.q.peek();
                if (first != null) {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0) {
                        E ePoll = this.q.poll();
                        if (this.leader == null && this.q.peek() != null) {
                            this.available.signal();
                        }
                        lock.unlock();
                        return ePoll;
                    }
                    if (nanos <= 0) {
                        if (this.leader == null && this.q.peek() != null) {
                            this.available.signal();
                        }
                        lock.unlock();
                        return null;
                    }
                    if (nanos < delay || this.leader != null) {
                        nanos = this.available.awaitNanos(nanos);
                    } else {
                        Thread thisThread = Thread.currentThread();
                        this.leader = thisThread;
                        try {
                            long timeLeft = this.available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                            if (this.leader == thisThread) {
                                this.leader = null;
                            }
                        } catch (Throwable th) {
                            if (this.leader == thisThread) {
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
                lock.unlock();
            }
        }
    }

    @Override
    public E peek() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.q.peek();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.q.size();
        } finally {
            lock.unlock();
        }
    }

    private E peekExpired() {
        E first = this.q.peek();
        if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) {
            return null;
        }
        return first;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        int n = 0;
        while (true) {
            try {
                Delayed delayedPeekExpired = peekExpired();
                if (delayedPeekExpired != null) {
                    c.add(delayedPeekExpired);
                    this.q.poll();
                    n++;
                } else {
                    return n;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        int n = 0;
        while (n < maxElements) {
            try {
                Delayed delayedPeekExpired = peekExpired();
                if (delayedPeekExpired == null) {
                    break;
                }
                c.add(delayedPeekExpired);
                this.q.poll();
                n++;
            } finally {
                lock.unlock();
            }
        }
        return n;
    }

    @Override
    public void clear() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            this.q.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Object[] toArray() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.q.toArray();
        } finally {
            lock.unlock();
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
    public boolean remove(Object o) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.q.remove(o);
        } finally {
            lock.unlock();
        }
    }

    void removeEQ(Object o) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Iterator<E> it = this.q.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                } else if (o == it.next()) {
                    break;
                }
            }
        } finally {
            lock.unlock();
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

        Itr(Object[] array) {
            this.array = array;
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
