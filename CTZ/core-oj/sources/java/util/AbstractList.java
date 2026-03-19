package java.util;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected transient int modCount = 0;

    @Override
    public abstract E get(int i);

    protected AbstractList() {
    }

    @Override
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    @Override
    public E set(int i, E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int i, E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object obj) {
        ListIterator<E> listIterator = listIterator();
        if (obj == null) {
            while (listIterator.hasNext()) {
                if (listIterator.next() == null) {
                    return listIterator.previousIndex();
                }
            }
            return -1;
        }
        while (listIterator.hasNext()) {
            if (obj.equals(listIterator.next())) {
                return listIterator.previousIndex();
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object obj) {
        ListIterator<E> listIterator = listIterator(size());
        if (obj == null) {
            while (listIterator.hasPrevious()) {
                if (listIterator.previous() == null) {
                    return listIterator.nextIndex();
                }
            }
            return -1;
        }
        while (listIterator.hasPrevious()) {
            if (obj.equals(listIterator.previous())) {
                return listIterator.nextIndex();
            }
        }
        return -1;
    }

    @Override
    public void clear() {
        removeRange(0, size());
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        rangeCheckForAdd(i);
        Iterator<? extends E> it = collection.iterator();
        boolean z = false;
        while (it.hasNext()) {
            add(i, it.next());
            z = true;
            i++;
        }
        return z;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        rangeCheckForAdd(i);
        return new ListItr(i);
    }

    private class Itr implements Iterator<E> {
        int cursor;
        int expectedModCount;
        int lastRet;

        private Itr() {
            this.cursor = 0;
            this.lastRet = -1;
            this.expectedModCount = AbstractList.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return this.cursor != AbstractList.this.size();
        }

        @Override
        public E next() {
            checkForComodification();
            try {
                int i = this.cursor;
                E e = (E) AbstractList.this.get(i);
                this.lastRet = i;
                this.cursor = i + 1;
                return e;
            } catch (IndexOutOfBoundsException e2) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            checkForComodification();
            try {
                AbstractList.this.remove(this.lastRet);
                if (this.lastRet < this.cursor) {
                    this.cursor--;
                }
                this.lastRet = -1;
                this.expectedModCount = AbstractList.this.modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (AbstractList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private class ListItr extends AbstractList<E>.Itr implements ListIterator<E> {
        ListItr(int i) {
            super();
            this.cursor = i;
        }

        @Override
        public boolean hasPrevious() {
            return this.cursor != 0;
        }

        @Override
        public E previous() {
            checkForComodification();
            try {
                int i = this.cursor - 1;
                E e = (E) AbstractList.this.get(i);
                this.cursor = i;
                this.lastRet = i;
                return e;
            } catch (IndexOutOfBoundsException e2) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        @Override
        public int nextIndex() {
            return this.cursor;
        }

        @Override
        public int previousIndex() {
            return this.cursor - 1;
        }

        @Override
        public void set(E e) {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            checkForComodification();
            try {
                AbstractList.this.set(this.lastRet, e);
                this.expectedModCount = AbstractList.this.modCount;
            } catch (IndexOutOfBoundsException e2) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void add(E e) {
            checkForComodification();
            try {
                int i = this.cursor;
                AbstractList.this.add(i, e);
                this.lastRet = -1;
                this.cursor = i + 1;
                this.expectedModCount = AbstractList.this.modCount;
            } catch (IndexOutOfBoundsException e2) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public List<E> subList(int i, int i2) {
        if (this instanceof RandomAccess) {
            return new RandomAccessSubList(this, i, i2);
        }
        return new SubList(this, i, i2);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        ListIterator<E> listIterator = listIterator();
        ListIterator<E> listIterator2 = ((List) obj).listIterator();
        while (listIterator.hasNext() && listIterator2.hasNext()) {
            E next = listIterator.next();
            E next2 = listIterator2.next();
            if (next == null) {
                if (next2 != null) {
                    return false;
                }
            } else if (!next.equals(next2)) {
                return false;
            }
        }
        return (listIterator.hasNext() || listIterator2.hasNext()) ? false : true;
    }

    @Override
    public int hashCode() {
        Iterator<E> it = iterator();
        int iHashCode = 1;
        while (it.hasNext()) {
            E next = it.next();
            iHashCode = (next == null ? 0 : next.hashCode()) + (31 * iHashCode);
        }
        return iHashCode;
    }

    protected void removeRange(int i, int i2) {
        ListIterator<E> listIterator = listIterator(i);
        int i3 = i2 - i;
        for (int i4 = 0; i4 < i3; i4++) {
            listIterator.next();
            listIterator.remove();
        }
    }

    private void rangeCheckForAdd(int i) {
        if (i < 0 || i > size()) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
    }

    private String outOfBoundsMsg(int i) {
        return "Index: " + i + ", Size: " + size();
    }
}
