package java.util;

public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {
    protected AbstractQueue() {
    }

    @Override
    public boolean add(E e) {
        if (offer(e)) {
            return true;
        }
        throw new IllegalStateException("Queue full");
    }

    @Override
    public E remove() {
        E x = poll();
        if (x != null) {
            return x;
        }
        throw new NoSuchElementException();
    }

    @Override
    public E element() {
        E x = peek();
        if (x != null) {
            return x;
        }
        throw new NoSuchElementException();
    }

    @Override
    public void clear() {
        while (poll() != null) {
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException("c == null");
        }
        if (c == this) {
            throw new IllegalArgumentException("c == this");
        }
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }
}
