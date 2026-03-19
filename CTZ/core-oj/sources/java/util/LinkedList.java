package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.function.Consumer;

public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, Serializable {
    private static final long serialVersionUID = 876323262645176354L;
    transient Node<E> first;
    transient Node<E> last;
    transient int size;

    public LinkedList() {
        this.size = 0;
    }

    public LinkedList(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    private void linkFirst(E e) {
        Node<E> node = this.first;
        Node<E> node2 = new Node<>(null, e, node);
        this.first = node2;
        if (node == null) {
            this.last = node2;
        } else {
            node.prev = node2;
        }
        this.size++;
        this.modCount++;
    }

    void linkLast(E e) {
        Node<E> node = this.last;
        Node<E> node2 = new Node<>(node, e, null);
        this.last = node2;
        if (node == null) {
            this.first = node2;
        } else {
            node.next = node2;
        }
        this.size++;
        this.modCount++;
    }

    void linkBefore(E e, Node<E> node) {
        Node<E> node2 = node.prev;
        Node<E> node3 = new Node<>(node2, e, node);
        node.prev = node3;
        if (node2 == null) {
            this.first = node3;
        } else {
            node2.next = node3;
        }
        this.size++;
        this.modCount++;
    }

    private E unlinkFirst(Node<E> node) {
        E e = node.item;
        Node<E> node2 = node.next;
        node.item = null;
        node.next = null;
        this.first = node2;
        if (node2 == null) {
            this.last = null;
        } else {
            node2.prev = null;
        }
        this.size--;
        this.modCount++;
        return e;
    }

    private E unlinkLast(Node<E> node) {
        E e = node.item;
        Node<E> node2 = node.prev;
        node.item = null;
        node.prev = null;
        this.last = node2;
        if (node2 == null) {
            this.first = null;
        } else {
            node2.next = null;
        }
        this.size--;
        this.modCount++;
        return e;
    }

    E unlink(Node<E> node) {
        E e = node.item;
        Node<E> node2 = node.next;
        Node<E> node3 = node.prev;
        if (node3 == null) {
            this.first = node2;
        } else {
            node3.next = node2;
            node.prev = null;
        }
        if (node2 == null) {
            this.last = node3;
        } else {
            node2.prev = node3;
            node.next = null;
        }
        node.item = null;
        this.size--;
        this.modCount++;
        return e;
    }

    @Override
    public E getFirst() {
        Node<E> node = this.first;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.item;
    }

    @Override
    public E getLast() {
        Node<E> node = this.last;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.item;
    }

    @Override
    public E removeFirst() {
        Node<E> node = this.first;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return unlinkFirst(node);
    }

    @Override
    public E removeLast() {
        Node<E> node = this.last;
        if (node == null) {
            throw new NoSuchElementException();
        }
        return unlinkLast(node);
    }

    @Override
    public void addFirst(E e) {
        linkFirst(e);
    }

    @Override
    public void addLast(E e) {
        linkLast(e);
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) != -1;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public boolean remove(Object obj) {
        if (obj == null) {
            for (Node<E> node = this.first; node != null; node = node.next) {
                if (node.item == null) {
                    unlink(node);
                    return true;
                }
            }
            return false;
        }
        for (Node<E> node2 = this.first; node2 != null; node2 = node2.next) {
            if (obj.equals(node2.item)) {
                unlink(node2);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return addAll(this.size, collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        Node<E> node;
        Node<E> node2;
        checkPositionIndex(i);
        Object[] array = collection.toArray();
        int length = array.length;
        int i2 = 0;
        if (length == 0) {
            return false;
        }
        if (i == this.size) {
            node2 = this.last;
            node = null;
        } else {
            Node<E> node3 = node(i);
            node = node3;
            node2 = node3.prev;
        }
        int length2 = array.length;
        while (i2 < length2) {
            Node<E> node4 = new Node<>(node2, array[i2], null);
            if (node2 == null) {
                this.first = node4;
            } else {
                node2.next = node4;
            }
            i2++;
            node2 = node4;
        }
        if (node == null) {
            this.last = node2;
        } else {
            node2.next = node;
            node.prev = node2;
        }
        this.size += length;
        this.modCount++;
        return true;
    }

    @Override
    public void clear() {
        Node<E> node = this.first;
        while (node != null) {
            Node<E> node2 = node.next;
            node.item = null;
            node.next = null;
            node.prev = null;
            node = node2;
        }
        this.last = null;
        this.first = null;
        this.size = 0;
        this.modCount++;
    }

    @Override
    public E get(int i) {
        checkElementIndex(i);
        return node(i).item;
    }

    @Override
    public E set(int i, E e) {
        checkElementIndex(i);
        Node<E> node = node(i);
        E e2 = node.item;
        node.item = e;
        return e2;
    }

    @Override
    public void add(int i, E e) {
        checkPositionIndex(i);
        if (i == this.size) {
            linkLast(e);
        } else {
            linkBefore(e, node(i));
        }
    }

    @Override
    public E remove(int i) {
        checkElementIndex(i);
        return unlink(node(i));
    }

    private boolean isElementIndex(int i) {
        return i >= 0 && i < this.size;
    }

    private boolean isPositionIndex(int i) {
        return i >= 0 && i <= this.size;
    }

    private String outOfBoundsMsg(int i) {
        return "Index: " + i + ", Size: " + this.size;
    }

    private void checkElementIndex(int i) {
        if (!isElementIndex(i)) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
    }

    private void checkPositionIndex(int i) {
        if (!isPositionIndex(i)) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
    }

    Node<E> node(int i) {
        if (i < (this.size >> 1)) {
            Node<E> node = this.first;
            for (int i2 = 0; i2 < i; i2++) {
                node = node.next;
            }
            return node;
        }
        Node<E> node2 = this.last;
        for (int i3 = this.size - 1; i3 > i; i3--) {
            node2 = node2.prev;
        }
        return node2;
    }

    @Override
    public int indexOf(Object obj) {
        int i = 0;
        if (obj == null) {
            for (Node<E> node = this.first; node != null; node = node.next) {
                if (node.item == null) {
                    return i;
                }
                i++;
            }
            return -1;
        }
        for (Node<E> node2 = this.first; node2 != null; node2 = node2.next) {
            if (obj.equals(node2.item)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object obj) {
        int i = this.size;
        if (obj == null) {
            for (Node<E> node = this.last; node != null; node = node.prev) {
                i--;
                if (node.item == null) {
                    return i;
                }
            }
        } else {
            for (Node<E> node2 = this.last; node2 != null; node2 = node2.prev) {
                i--;
                if (obj.equals(node2.item)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public E peek() {
        Node<E> node = this.first;
        if (node == null) {
            return null;
        }
        return node.item;
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E poll() {
        Node<E> node = this.first;
        if (node == null) {
            return null;
        }
        return unlinkFirst(node);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E peekFirst() {
        Node<E> node = this.first;
        if (node == null) {
            return null;
        }
        return node.item;
    }

    @Override
    public E peekLast() {
        Node<E> node = this.last;
        if (node == null) {
            return null;
        }
        return node.item;
    }

    @Override
    public E pollFirst() {
        Node<E> node = this.first;
        if (node == null) {
            return null;
        }
        return unlinkFirst(node);
    }

    @Override
    public E pollLast() {
        Node<E> node = this.last;
        if (node == null) {
            return null;
        }
        return unlinkLast(node);
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public boolean removeFirstOccurrence(Object obj) {
        return remove(obj);
    }

    @Override
    public boolean removeLastOccurrence(Object obj) {
        if (obj == null) {
            for (Node<E> node = this.last; node != null; node = node.prev) {
                if (node.item == null) {
                    unlink(node);
                    return true;
                }
            }
            return false;
        }
        for (Node<E> node2 = this.last; node2 != null; node2 = node2.prev) {
            if (obj.equals(node2.item)) {
                unlink(node2);
                return true;
            }
        }
        return false;
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        checkPositionIndex(i);
        return new ListItr(i);
    }

    private class ListItr implements ListIterator<E> {
        private int expectedModCount;
        private Node<E> lastReturned;
        private Node<E> next;
        private int nextIndex;

        ListItr(int i) {
            this.expectedModCount = LinkedList.this.modCount;
            this.next = i == LinkedList.this.size ? null : LinkedList.this.node(i);
            this.nextIndex = i;
        }

        @Override
        public boolean hasNext() {
            return this.nextIndex < LinkedList.this.size;
        }

        @Override
        public E next() {
            checkForComodification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.lastReturned = this.next;
            this.next = this.next.next;
            this.nextIndex++;
            return this.lastReturned.item;
        }

        @Override
        public boolean hasPrevious() {
            return this.nextIndex > 0;
        }

        @Override
        public E previous() {
            checkForComodification();
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            Node<E> node = this.next == null ? LinkedList.this.last : this.next.prev;
            this.next = node;
            this.lastReturned = node;
            this.nextIndex--;
            return this.lastReturned.item;
        }

        @Override
        public int nextIndex() {
            return this.nextIndex;
        }

        @Override
        public int previousIndex() {
            return this.nextIndex - 1;
        }

        @Override
        public void remove() {
            checkForComodification();
            if (this.lastReturned == null) {
                throw new IllegalStateException();
            }
            Node<E> node = this.lastReturned.next;
            LinkedList.this.unlink(this.lastReturned);
            if (this.next == this.lastReturned) {
                this.next = node;
            } else {
                this.nextIndex--;
            }
            this.lastReturned = null;
            this.expectedModCount++;
        }

        @Override
        public void set(E e) {
            if (this.lastReturned == null) {
                throw new IllegalStateException();
            }
            checkForComodification();
            this.lastReturned.item = e;
        }

        @Override
        public void add(E e) {
            checkForComodification();
            this.lastReturned = null;
            if (this.next == null) {
                LinkedList.this.linkLast(e);
            } else {
                LinkedList.this.linkBefore(e, this.next);
            }
            this.nextIndex++;
            this.expectedModCount++;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            while (LinkedList.this.modCount == this.expectedModCount && this.nextIndex < LinkedList.this.size) {
                consumer.accept(this.next.item);
                this.lastReturned = this.next;
                this.next = this.next.next;
                this.nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {
            if (LinkedList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> node, E e, Node<E> node2) {
            this.item = e;
            this.next = node2;
            this.prev = node;
        }
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    private class DescendingIterator implements Iterator<E> {
        private final LinkedList<E>.ListItr itr;

        private DescendingIterator() {
            this.itr = new ListItr(LinkedList.this.size());
        }

        @Override
        public boolean hasNext() {
            return this.itr.hasPrevious();
        }

        @Override
        public E next() {
            return this.itr.previous();
        }

        @Override
        public void remove() {
            this.itr.remove();
        }
    }

    private LinkedList<E> superClone() {
        try {
            return (LinkedList) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    public Object clone() {
        LinkedList<E> linkedListSuperClone = superClone();
        linkedListSuperClone.last = null;
        linkedListSuperClone.first = null;
        linkedListSuperClone.size = 0;
        linkedListSuperClone.modCount = 0;
        for (Node<E> node = this.first; node != null; node = node.next) {
            linkedListSuperClone.add(node.item);
        }
        return linkedListSuperClone;
    }

    @Override
    public Object[] toArray() {
        Object[] objArr = new Object[this.size];
        Node<E> node = this.first;
        int i = 0;
        while (node != null) {
            objArr[i] = node.item;
            node = node.next;
            i++;
        }
        return objArr;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        if (tArr.length < this.size) {
            tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), this.size));
        }
        int i = 0;
        Node<E> node = this.first;
        while (node != null) {
            tArr[i] = node.item;
            node = node.next;
            i++;
        }
        if (tArr.length > this.size) {
            tArr[this.size] = null;
        }
        return tArr;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.size);
        for (Node<E> node = this.first; node != null; node = node.next) {
            objectOutputStream.writeObject(node.item);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        int i = objectInputStream.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            linkLast(objectInputStream.readObject());
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator(this, -1, 0);
    }

    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1024;
        static final int MAX_BATCH = 33554432;
        int batch;
        Node<E> current;
        int est;
        int expectedModCount;
        final LinkedList<E> list;

        LLSpliterator(LinkedList<E> linkedList, int i, int i2) {
            this.list = linkedList;
            this.est = i;
            this.expectedModCount = i2;
        }

        final int getEst() {
            int i = this.est;
            if (i < 0) {
                LinkedList<E> linkedList = this.list;
                if (linkedList == null) {
                    this.est = 0;
                    return 0;
                }
                this.expectedModCount = linkedList.modCount;
                this.current = linkedList.first;
                int i2 = linkedList.size;
                this.est = i2;
                return i2;
            }
            return i;
        }

        @Override
        public long estimateSize() {
            return getEst();
        }

        @Override
        public Spliterator<E> trySplit() {
            Node<E> node;
            int i;
            int est = getEst();
            if (est > 1 && (node = this.current) != null) {
                int i2 = this.batch + 1024;
                if (i2 > est) {
                    i2 = est;
                }
                if (i2 > MAX_BATCH) {
                    i2 = MAX_BATCH;
                }
                Object[] objArr = new Object[i2];
                Node<E> node2 = node;
                int i3 = 0;
                while (true) {
                    i = i3 + 1;
                    objArr[i3] = node2.item;
                    node2 = node2.next;
                    if (node2 == null || i >= i2) {
                        break;
                    }
                    i3 = i;
                }
                this.current = node2;
                this.batch = i;
                this.est = est - i;
                return Spliterators.spliterator(objArr, 0, i, 16);
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int est = getEst();
            if (est > 0 && (node = this.current) != null) {
                this.current = null;
                this.est = 0;
                do {
                    E e = node.item;
                    Node<E> node = node.next;
                    consumer.accept(e);
                    if (node == null) {
                        break;
                    } else {
                        est--;
                    }
                } while (est > 0);
            }
            if (this.list.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            Node<E> node;
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (getEst() > 0 && (node = this.current) != null) {
                this.est--;
                E e = node.item;
                this.current = node.next;
                consumer.accept(e);
                if (this.list.modCount == this.expectedModCount) {
                    return true;
                }
                throw new ConcurrentModificationException();
            }
            return false;
        }

        @Override
        public int characteristics() {
            return 16464;
        }
    }
}
