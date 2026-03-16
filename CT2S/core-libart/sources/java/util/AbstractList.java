package java.util;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected transient int modCount;

    public abstract E get(int i);

    private class SimpleListIterator implements Iterator<E> {
        int expectedModCount;
        int pos = -1;
        int lastPosition = -1;

        SimpleListIterator() {
            this.expectedModCount = AbstractList.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return this.pos + 1 < AbstractList.this.size();
        }

        @Override
        public E next() {
            if (this.expectedModCount == AbstractList.this.modCount) {
                try {
                    E e = (E) AbstractList.this.get(this.pos + 1);
                    int i = this.pos + 1;
                    this.pos = i;
                    this.lastPosition = i;
                    return e;
                } catch (IndexOutOfBoundsException e2) {
                    throw new NoSuchElementException();
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public void remove() {
            if (this.lastPosition == -1) {
                throw new IllegalStateException();
            }
            if (this.expectedModCount != AbstractList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            try {
                AbstractList.this.remove(this.lastPosition);
                this.expectedModCount = AbstractList.this.modCount;
                if (this.pos == this.lastPosition) {
                    this.pos--;
                }
                this.lastPosition = -1;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private final class FullListIterator extends AbstractList<E>.SimpleListIterator implements ListIterator<E> {
        FullListIterator(int start) {
            super();
            if (start >= 0 && start <= AbstractList.this.size()) {
                this.pos = start - 1;
                return;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public void add(E object) {
            if (this.expectedModCount == AbstractList.this.modCount) {
                try {
                    AbstractList.this.add(this.pos + 1, object);
                    this.pos++;
                    this.lastPosition = -1;
                    if (AbstractList.this.modCount != this.expectedModCount) {
                        this.expectedModCount = AbstractList.this.modCount;
                        return;
                    }
                    return;
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean hasPrevious() {
            return this.pos >= 0;
        }

        @Override
        public int nextIndex() {
            return this.pos + 1;
        }

        @Override
        public E previous() {
            if (this.expectedModCount == AbstractList.this.modCount) {
                try {
                    E e = (E) AbstractList.this.get(this.pos);
                    this.lastPosition = this.pos;
                    this.pos--;
                    return e;
                } catch (IndexOutOfBoundsException e2) {
                    throw new NoSuchElementException();
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public int previousIndex() {
            return this.pos;
        }

        @Override
        public void set(E object) {
            if (this.expectedModCount == AbstractList.this.modCount) {
                try {
                    AbstractList.this.set(this.lastPosition, object);
                    return;
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalStateException();
                }
            }
            throw new ConcurrentModificationException();
        }
    }

    private static final class SubAbstractListRandomAccess<E> extends SubAbstractList<E> implements RandomAccess {
        SubAbstractListRandomAccess(AbstractList<E> list, int start, int end) {
            super(list, start, end);
        }
    }

    private static class SubAbstractList<E> extends AbstractList<E> {
        private final AbstractList<E> fullList;
        private int offset;
        private int size;

        private static final class SubAbstractListIterator<E> implements ListIterator<E> {
            private int end;
            private final ListIterator<E> iterator;
            private int start;
            private final SubAbstractList<E> subList;

            SubAbstractListIterator(ListIterator<E> it, SubAbstractList<E> list, int offset, int length) {
                this.iterator = it;
                this.subList = list;
                this.start = offset;
                this.end = this.start + length;
            }

            @Override
            public void add(E object) {
                this.iterator.add(object);
                this.subList.sizeChanged(true);
                this.end++;
            }

            @Override
            public boolean hasNext() {
                return this.iterator.nextIndex() < this.end;
            }

            @Override
            public boolean hasPrevious() {
                return this.iterator.previousIndex() >= this.start;
            }

            @Override
            public E next() {
                if (this.iterator.nextIndex() < this.end) {
                    return this.iterator.next();
                }
                throw new NoSuchElementException();
            }

            @Override
            public int nextIndex() {
                return this.iterator.nextIndex() - this.start;
            }

            @Override
            public E previous() {
                if (this.iterator.previousIndex() >= this.start) {
                    return this.iterator.previous();
                }
                throw new NoSuchElementException();
            }

            @Override
            public int previousIndex() {
                int previous = this.iterator.previousIndex();
                if (previous >= this.start) {
                    return previous - this.start;
                }
                return -1;
            }

            @Override
            public void remove() {
                this.iterator.remove();
                this.subList.sizeChanged(false);
                this.end--;
            }

            @Override
            public void set(E object) {
                this.iterator.set(object);
            }
        }

        SubAbstractList(AbstractList<E> list, int start, int end) {
            this.fullList = list;
            this.modCount = this.fullList.modCount;
            this.offset = start;
            this.size = end - start;
        }

        @Override
        public void add(int location, E object) {
            if (this.modCount == this.fullList.modCount) {
                if (location >= 0 && location <= this.size) {
                    this.fullList.add(this.offset + location, object);
                    this.size++;
                    this.modCount = this.fullList.modCount;
                    return;
                }
                throw new IndexOutOfBoundsException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean addAll(int location, Collection<? extends E> collection) {
            if (this.modCount == this.fullList.modCount) {
                if (location >= 0 && location <= this.size) {
                    boolean result = this.fullList.addAll(this.offset + location, collection);
                    if (result) {
                        this.size += collection.size();
                        this.modCount = this.fullList.modCount;
                    }
                    return result;
                }
                throw new IndexOutOfBoundsException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            if (this.modCount == this.fullList.modCount) {
                boolean result = this.fullList.addAll(this.offset + this.size, collection);
                if (result) {
                    this.size += collection.size();
                    this.modCount = this.fullList.modCount;
                }
                return result;
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public E get(int location) {
            if (this.modCount == this.fullList.modCount) {
                if (location >= 0 && location < this.size) {
                    return this.fullList.get(this.offset + location);
                }
                throw new IndexOutOfBoundsException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public Iterator<E> iterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(int location) {
            if (this.modCount == this.fullList.modCount) {
                if (location >= 0 && location <= this.size) {
                    return new SubAbstractListIterator(this.fullList.listIterator(this.offset + location), this, this.offset, this.size);
                }
                throw new IndexOutOfBoundsException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public E remove(int location) {
            if (this.modCount == this.fullList.modCount) {
                if (location >= 0 && location < this.size) {
                    E result = this.fullList.remove(this.offset + location);
                    this.size--;
                    this.modCount = this.fullList.modCount;
                    return result;
                }
                throw new IndexOutOfBoundsException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        protected void removeRange(int start, int end) {
            if (start != end) {
                if (this.modCount == this.fullList.modCount) {
                    this.fullList.removeRange(this.offset + start, this.offset + end);
                    this.size -= end - start;
                    this.modCount = this.fullList.modCount;
                    return;
                }
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public E set(int location, E object) {
            if (this.modCount == this.fullList.modCount) {
                if (location >= 0 && location < this.size) {
                    return this.fullList.set(this.offset + location, object);
                }
                throw new IndexOutOfBoundsException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public int size() {
            if (this.modCount == this.fullList.modCount) {
                return this.size;
            }
            throw new ConcurrentModificationException();
        }

        void sizeChanged(boolean increment) {
            if (increment) {
                this.size++;
            } else {
                this.size--;
            }
            this.modCount = this.fullList.modCount;
        }
    }

    protected AbstractList() {
    }

    @Override
    public void add(int location, E object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E object) {
        add(size(), object);
        return true;
    }

    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        Iterator<? extends E> it = collection.iterator();
        while (it.hasNext()) {
            add(location, it.next());
            location++;
        }
        return !collection.isEmpty();
    }

    @Override
    public void clear() {
        removeRange(0, size());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof List)) {
            return false;
        }
        List<?> list = (List) object;
        if (list.size() != size()) {
            return false;
        }
        Iterator<E> it = list.iterator();
        for (Object e1 : this) {
            Object e2 = it.next();
            if (e1 == null) {
                if (e2 != null) {
                    return false;
                }
            } else if (!e1.equals(e2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object object = it.next();
            result = (result * 31) + (object == null ? 0 : object.hashCode());
        }
        return result;
    }

    @Override
    public int indexOf(Object object) {
        ListIterator<E> listIterator = listIterator();
        if (object != null) {
            while (listIterator.hasNext()) {
                if (object.equals(listIterator.next())) {
                    return listIterator.previousIndex();
                }
            }
        } else {
            while (listIterator.hasNext()) {
                if (listIterator.next() == null) {
                    return listIterator.previousIndex();
                }
            }
        }
        return -1;
    }

    @Override
    public Iterator<E> iterator() {
        return new SimpleListIterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        ListIterator<E> listIterator = listIterator(size());
        if (object != null) {
            while (listIterator.hasPrevious()) {
                if (object.equals(listIterator.previous())) {
                    return listIterator.nextIndex();
                }
            }
        } else {
            while (listIterator.hasPrevious()) {
                if (listIterator.previous() == null) {
                    return listIterator.nextIndex();
                }
            }
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int location) {
        return new FullListIterator(location);
    }

    @Override
    public E remove(int location) {
        throw new UnsupportedOperationException();
    }

    protected void removeRange(int start, int end) {
        Iterator<?> it = listIterator(start);
        for (int i = start; i < end; i++) {
            it.next();
            it.remove();
        }
    }

    @Override
    public E set(int location, E object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<E> subList(int start, int end) {
        if (start >= 0 && end <= size()) {
            if (start <= end) {
                return this instanceof RandomAccess ? new SubAbstractListRandomAccess(this, start, end) : new SubAbstractList(this, start, end);
            }
            throw new IllegalArgumentException();
        }
        throw new IndexOutOfBoundsException();
    }
}
