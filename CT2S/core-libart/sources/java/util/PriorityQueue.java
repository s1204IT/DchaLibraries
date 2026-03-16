package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PriorityQueue<E> extends AbstractQueue<E> implements Serializable {
    private static final int DEFAULT_CAPACITY = 11;
    private static final int DEFAULT_CAPACITY_RATIO = 2;
    private static final double DEFAULT_INIT_CAPACITY_RATIO = 1.1d;
    private static final long serialVersionUID = -7720805057305804111L;
    private Comparator<? super E> comparator;
    private transient E[] elements;
    private int size;

    public PriorityQueue() {
        this(11);
    }

    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity < 1: " + initialCapacity);
        }
        this.elements = newElementArray(initialCapacity);
        this.comparator = comparator;
    }

    public PriorityQueue(Collection<? extends E> c) {
        if (c instanceof PriorityQueue) {
            getFromPriorityQueue((PriorityQueue) c);
        } else if (c instanceof SortedSet) {
            getFromSortedSet((SortedSet) c);
        } else {
            initSize(c);
            addAll(c);
        }
    }

    public PriorityQueue(PriorityQueue<? extends E> c) {
        getFromPriorityQueue(c);
    }

    public PriorityQueue(SortedSet<? extends E> c) {
        getFromSortedSet(c);
    }

    @Override
    public Iterator<E> iterator() {
        return new PriorityIterator();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        Arrays.fill(this.elements, (Object) null);
        this.size = 0;
    }

    @Override
    public boolean offer(E o) {
        if (o == null) {
            throw new NullPointerException("o == null");
        }
        growToSize(this.size + 1);
        this.elements[this.size] = o;
        int i = this.size;
        this.size = i + 1;
        siftUp(i);
        return true;
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        E e = this.elements[0];
        removeAt(0);
        return e;
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return this.elements[0];
    }

    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        for (int targetIndex = 0; targetIndex < this.size; targetIndex++) {
            if (o.equals(this.elements[targetIndex])) {
                removeAt(targetIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(E o) {
        return offer(o);
    }

    private class PriorityIterator implements Iterator<E> {
        private boolean allowRemove;
        private int currentIndex;

        private PriorityIterator() {
            this.currentIndex = -1;
            this.allowRemove = false;
        }

        @Override
        public boolean hasNext() {
            return this.currentIndex < PriorityQueue.this.size + (-1);
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.allowRemove = true;
            Object[] objArr = PriorityQueue.this.elements;
            int i = this.currentIndex + 1;
            this.currentIndex = i;
            return (E) objArr[i];
        }

        @Override
        public void remove() {
            if (!this.allowRemove) {
                throw new IllegalStateException();
            }
            this.allowRemove = false;
            PriorityQueue priorityQueue = PriorityQueue.this;
            int i = this.currentIndex;
            this.currentIndex = i - 1;
            priorityQueue.removeAt(i);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.elements = newElementArray(objectInputStream.readInt());
        for (int i = 0; i < this.size; i++) {
            ((E[]) this.elements)[i] = objectInputStream.readObject();
        }
    }

    private E[] newElementArray(int i) {
        return (E[]) new Object[i];
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(this.elements.length);
        for (int i = 0; i < this.size; i++) {
            out.writeObject(this.elements[i]);
        }
    }

    private void getFromPriorityQueue(PriorityQueue<? extends E> priorityQueue) {
        initSize(priorityQueue);
        this.comparator = priorityQueue.comparator();
        System.arraycopy(priorityQueue.elements, 0, this.elements, 0, priorityQueue.size());
        this.size = priorityQueue.size();
    }

    private void getFromSortedSet(SortedSet<? extends E> sortedSet) {
        initSize(sortedSet);
        this.comparator = sortedSet.comparator();
        Iterator<? extends E> it = sortedSet.iterator();
        while (it.hasNext()) {
            E[] eArr = this.elements;
            int i = this.size;
            this.size = i + 1;
            eArr[i] = it.next();
        }
    }

    private void removeAt(int index) {
        this.size--;
        E moved = this.elements[this.size];
        this.elements[index] = moved;
        siftDown(index);
        this.elements[this.size] = null;
        if (moved == this.elements[index]) {
            siftUp(index);
        }
    }

    private int compare(E o1, E o2) {
        return this.comparator != null ? this.comparator.compare(o1, o2) : ((Comparable) o1).compareTo(o2);
    }

    private void siftUp(int childIndex) {
        E target = this.elements[childIndex];
        while (childIndex > 0) {
            int parentIndex = (childIndex - 1) / 2;
            E parent = this.elements[parentIndex];
            if (compare(parent, target) <= 0) {
                break;
            }
            this.elements[childIndex] = parent;
            childIndex = parentIndex;
        }
        this.elements[childIndex] = target;
    }

    private void siftDown(int rootIndex) {
        E target = this.elements[rootIndex];
        while (true) {
            int childIndex = (rootIndex * 2) + 1;
            if (childIndex >= this.size) {
                break;
            }
            if (childIndex + 1 < this.size && compare(this.elements[childIndex + 1], this.elements[childIndex]) < 0) {
                childIndex++;
            }
            if (compare(target, this.elements[childIndex]) <= 0) {
                break;
            }
            this.elements[rootIndex] = this.elements[childIndex];
            rootIndex = childIndex;
        }
        this.elements[rootIndex] = target;
    }

    private void initSize(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException("c == null");
        }
        if (c.isEmpty()) {
            this.elements = newElementArray(1);
        } else {
            int capacity = (int) Math.ceil(((double) c.size()) * DEFAULT_INIT_CAPACITY_RATIO);
            this.elements = newElementArray(capacity);
        }
    }

    private void growToSize(int size) {
        if (size > this.elements.length) {
            E[] newElements = newElementArray(size * 2);
            System.arraycopy(this.elements, 0, newElements, 0, this.elements.length);
            this.elements = newElements;
        }
    }
}
