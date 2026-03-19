package java.util;

class SubList<E> extends AbstractList<E> {
    private final AbstractList<E> l;
    private final int offset;
    private int size;

    static int access$208(SubList subList) {
        int i = subList.size;
        subList.size = i + 1;
        return i;
    }

    static int access$210(SubList subList) {
        int i = subList.size;
        subList.size = i - 1;
        return i;
    }

    SubList(AbstractList<E> abstractList, int i, int i2) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + i);
        }
        if (i2 > abstractList.size()) {
            throw new IndexOutOfBoundsException("toIndex = " + i2);
        }
        if (i > i2) {
            throw new IllegalArgumentException("fromIndex(" + i + ") > toIndex(" + i2 + ")");
        }
        this.l = abstractList;
        this.offset = i;
        this.size = i2 - i;
        this.modCount = this.l.modCount;
    }

    @Override
    public E set(int i, E e) {
        rangeCheck(i);
        checkForComodification();
        return this.l.set(i + this.offset, e);
    }

    @Override
    public E get(int i) {
        rangeCheck(i);
        checkForComodification();
        return this.l.get(i + this.offset);
    }

    @Override
    public int size() {
        checkForComodification();
        return this.size;
    }

    @Override
    public void add(int i, E e) {
        rangeCheckForAdd(i);
        checkForComodification();
        this.l.add(i + this.offset, e);
        this.modCount = this.l.modCount;
        this.size++;
    }

    @Override
    public E remove(int i) {
        rangeCheck(i);
        checkForComodification();
        E eRemove = this.l.remove(i + this.offset);
        this.modCount = this.l.modCount;
        this.size--;
        return eRemove;
    }

    @Override
    protected void removeRange(int i, int i2) {
        checkForComodification();
        this.l.removeRange(this.offset + i, this.offset + i2);
        this.modCount = this.l.modCount;
        this.size -= i2 - i;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return addAll(this.size, collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        rangeCheckForAdd(i);
        int size = collection.size();
        if (size == 0) {
            return false;
        }
        checkForComodification();
        this.l.addAll(this.offset + i, collection);
        this.modCount = this.l.modCount;
        this.size += size;
        return true;
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<E> listIterator(final int i) {
        checkForComodification();
        rangeCheckForAdd(i);
        return new ListIterator<E>() {
            private final ListIterator<E> i;

            {
                this.i = SubList.this.l.listIterator(i + SubList.this.offset);
            }

            @Override
            public boolean hasNext() {
                return nextIndex() < SubList.this.size;
            }

            @Override
            public E next() {
                if (hasNext()) {
                    return this.i.next();
                }
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasPrevious() {
                return previousIndex() >= 0;
            }

            @Override
            public E previous() {
                if (hasPrevious()) {
                    return this.i.previous();
                }
                throw new NoSuchElementException();
            }

            @Override
            public int nextIndex() {
                return this.i.nextIndex() - SubList.this.offset;
            }

            @Override
            public int previousIndex() {
                return this.i.previousIndex() - SubList.this.offset;
            }

            @Override
            public void remove() {
                this.i.remove();
                SubList.this.modCount = SubList.this.l.modCount;
                SubList.access$210(SubList.this);
            }

            @Override
            public void set(E e) {
                this.i.set(e);
            }

            @Override
            public void add(E e) {
                this.i.add(e);
                SubList.this.modCount = SubList.this.l.modCount;
                SubList.access$208(SubList.this);
            }
        };
    }

    @Override
    public List<E> subList(int i, int i2) {
        return new SubList(this, i, i2);
    }

    private void rangeCheck(int i) {
        if (i < 0 || i >= this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
    }

    private void rangeCheckForAdd(int i) {
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
    }

    private String outOfBoundsMsg(int i) {
        return "Index: " + i + ", Size: " + this.size;
    }

    private void checkForComodification() {
        if (this.modCount != this.l.modCount) {
            throw new ConcurrentModificationException();
        }
    }
}
