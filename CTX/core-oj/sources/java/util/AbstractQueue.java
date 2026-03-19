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
        E ePoll = poll();
        if (ePoll != null) {
            return ePoll;
        }
        throw new NoSuchElementException();
    }

    @Override
    public E element() {
        E ePeek = peek();
        if (ePeek != null) {
            return ePeek;
        }
        throw new NoSuchElementException();
    }

    @Override
    public void clear() {
        while (poll() != null) {
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        boolean z = false;
        Iterator<? extends E> it = collection.iterator();
        while (it.hasNext()) {
            if (add(it.next())) {
                z = true;
            }
        }
        return z;
    }
}
