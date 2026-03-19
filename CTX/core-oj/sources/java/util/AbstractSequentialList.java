package java.util;

public abstract class AbstractSequentialList<E> extends AbstractList<E> {
    @Override
    public abstract ListIterator<E> listIterator(int i);

    protected AbstractSequentialList() {
    }

    @Override
    public E get(int i) {
        try {
            return listIterator(i).next();
        } catch (NoSuchElementException e) {
            throw new IndexOutOfBoundsException("Index: " + i);
        }
    }

    @Override
    public E set(int i, E e) {
        try {
            ListIterator<E> listIterator = listIterator(i);
            E next = listIterator.next();
            listIterator.set(e);
            return next;
        } catch (NoSuchElementException e2) {
            throw new IndexOutOfBoundsException("Index: " + i);
        }
    }

    @Override
    public void add(int i, E e) {
        try {
            listIterator(i).add(e);
        } catch (NoSuchElementException e2) {
            throw new IndexOutOfBoundsException("Index: " + i);
        }
    }

    @Override
    public E remove(int i) {
        try {
            ListIterator<E> listIterator = listIterator(i);
            E next = listIterator.next();
            listIterator.remove();
            return next;
        } catch (NoSuchElementException e) {
            throw new IndexOutOfBoundsException("Index: " + i);
        }
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        boolean z = false;
        try {
            ListIterator<E> listIterator = listIterator(i);
            Iterator<? extends E> it = collection.iterator();
            while (it.hasNext()) {
                listIterator.add(it.next());
                z = true;
            }
            return z;
        } catch (NoSuchElementException e) {
            throw new IndexOutOfBoundsException("Index: " + i);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }
}
