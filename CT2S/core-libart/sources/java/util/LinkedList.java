package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;

public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Queue<E>, Cloneable, Serializable {
    private static final long serialVersionUID = 876323262645176354L;
    transient int size;
    transient Link<E> voidLink;

    private static final class Link<ET> {
        ET data;
        Link<ET> next;
        Link<ET> previous;

        Link(ET o, Link<ET> p, Link<ET> n) {
            this.data = o;
            this.previous = p;
            this.next = n;
        }
    }

    private static final class LinkIterator<ET> implements ListIterator<ET> {
        int expectedModCount;
        Link<ET> lastLink;
        Link<ET> link;
        final LinkedList<ET> list;
        int pos;

        LinkIterator(LinkedList<ET> object, int location) {
            this.list = object;
            this.expectedModCount = this.list.modCount;
            if (location >= 0 && location <= this.list.size) {
                this.link = this.list.voidLink;
                if (location < this.list.size / 2) {
                    this.pos = -1;
                    while (this.pos + 1 < location) {
                        this.link = this.link.next;
                        this.pos++;
                    }
                    return;
                }
                this.pos = this.list.size;
                while (this.pos >= location) {
                    this.link = this.link.previous;
                    this.pos--;
                }
                return;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public void add(ET object) {
            if (this.expectedModCount == this.list.modCount) {
                Link<ET> next = this.link.next;
                Link<ET> newLink = new Link<>(object, this.link, next);
                this.link.next = newLink;
                next.previous = newLink;
                this.link = newLink;
                this.lastLink = null;
                this.pos++;
                this.expectedModCount++;
                this.list.size++;
                this.list.modCount++;
                return;
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean hasNext() {
            return this.link.next != this.list.voidLink;
        }

        @Override
        public boolean hasPrevious() {
            return this.link != this.list.voidLink;
        }

        @Override
        public ET next() {
            if (this.expectedModCount == this.list.modCount) {
                Link<ET> next = this.link.next;
                if (next != this.list.voidLink) {
                    this.link = next;
                    this.lastLink = next;
                    this.pos++;
                    return this.link.data;
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public int nextIndex() {
            return this.pos + 1;
        }

        @Override
        public ET previous() {
            if (this.expectedModCount == this.list.modCount) {
                if (this.link != this.list.voidLink) {
                    this.lastLink = this.link;
                    this.link = this.link.previous;
                    this.pos--;
                    return this.lastLink.data;
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public int previousIndex() {
            return this.pos;
        }

        @Override
        public void remove() {
            if (this.expectedModCount == this.list.modCount) {
                if (this.lastLink != null) {
                    Link<ET> next = this.lastLink.next;
                    Link<ET> previous = this.lastLink.previous;
                    next.previous = previous;
                    previous.next = next;
                    if (this.lastLink == this.link) {
                        this.pos--;
                    }
                    this.link = previous;
                    this.lastLink = null;
                    this.expectedModCount++;
                    LinkedList<ET> linkedList = this.list;
                    linkedList.size--;
                    this.list.modCount++;
                    return;
                }
                throw new IllegalStateException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public void set(ET object) {
            if (this.expectedModCount == this.list.modCount) {
                if (this.lastLink != null) {
                    this.lastLink.data = object;
                    return;
                }
                throw new IllegalStateException();
            }
            throw new ConcurrentModificationException();
        }
    }

    private class ReverseLinkIterator<ET> implements Iterator<ET> {
        private boolean canRemove = false;
        private int expectedModCount;
        private Link<ET> link;
        private final LinkedList<ET> list;

        ReverseLinkIterator(LinkedList<ET> linkedList) {
            this.list = linkedList;
            this.expectedModCount = this.list.modCount;
            this.link = this.list.voidLink;
        }

        @Override
        public boolean hasNext() {
            return this.link.previous != this.list.voidLink;
        }

        @Override
        public ET next() {
            if (this.expectedModCount == this.list.modCount) {
                if (hasNext()) {
                    this.link = this.link.previous;
                    this.canRemove = true;
                    return this.link.data;
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public void remove() {
            if (this.expectedModCount == this.list.modCount) {
                if (this.canRemove) {
                    Link<ET> next = this.link.previous;
                    Link<ET> previous = this.link.next;
                    next.next = previous;
                    previous.previous = next;
                    this.link = previous;
                    LinkedList<ET> linkedList = this.list;
                    linkedList.size--;
                    this.list.modCount++;
                    this.expectedModCount++;
                    this.canRemove = false;
                    return;
                }
                throw new IllegalStateException();
            }
            throw new ConcurrentModificationException();
        }
    }

    public LinkedList() {
        this.size = 0;
        this.voidLink = new Link<>(null, null, null);
        this.voidLink.previous = this.voidLink;
        this.voidLink.next = this.voidLink;
    }

    public LinkedList(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    @Override
    public void add(int location, E object) {
        if (location >= 0 && location <= this.size) {
            Link link = this.voidLink;
            if (location < this.size / 2) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i2 = this.size; i2 > location; i2--) {
                    link = link.previous;
                }
            }
            Link<ET> link2 = link.previous;
            Link<ET> link3 = new Link<>(object, link2, link);
            link2.next = link3;
            link.previous = link3;
            this.size++;
            this.modCount++;
            return;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean add(E object) {
        return addLastImpl(object);
    }

    private boolean addLastImpl(E object) {
        Link<E> oldLast = this.voidLink.previous;
        Link<ET> link = new Link<>(object, oldLast, this.voidLink);
        this.voidLink.previous = link;
        oldLast.next = link;
        this.size++;
        this.modCount++;
        return true;
    }

    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        if (location < 0 || location > this.size) {
            throw new IndexOutOfBoundsException();
        }
        int adding = collection.size();
        if (adding == 0) {
            return false;
        }
        Collection<? extends E> elements = collection == this ? new ArrayList<>(collection) : collection;
        Link link = this.voidLink;
        if (location < this.size / 2) {
            for (int i = 0; i < location; i++) {
                link = link.next;
            }
        } else {
            for (int i2 = this.size; i2 >= location; i2--) {
                link = link.previous;
            }
        }
        Link<ET> link2 = link.next;
        for (E e : elements) {
            Link link3 = new Link(e, link, null);
            link.next = link3;
            link = link3;
        }
        link.next = link2;
        link2.previous = link;
        this.size += adding;
        this.modCount++;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        int size = collection.size();
        if (size == 0) {
            return false;
        }
        Collection<? extends E> arrayList = collection == this ? new ArrayList<>(collection) : collection;
        Link<E> link = this.voidLink.previous;
        Iterator<? extends E> it = arrayList.iterator();
        ?? r5 = link;
        while (it.hasNext()) {
            Link link2 = new Link(it.next(), r5, null);
            r5.next = link2;
            r5 = (Link<E>) link2;
        }
        r5.next = (Link<E>) this.voidLink;
        this.voidLink.previous = r5;
        this.size += size;
        this.modCount++;
        return true;
    }

    @Override
    public void addFirst(E object) {
        addFirstImpl(object);
    }

    private boolean addFirstImpl(E object) {
        Link<E> oldFirst = this.voidLink.next;
        Link<ET> link = new Link<>(object, this.voidLink, oldFirst);
        this.voidLink.next = link;
        oldFirst.previous = link;
        this.size++;
        this.modCount++;
        return true;
    }

    @Override
    public void addLast(E object) {
        addLastImpl(object);
    }

    @Override
    public void clear() {
        if (this.size > 0) {
            this.size = 0;
            this.voidLink.next = this.voidLink;
            this.voidLink.previous = this.voidLink;
            this.modCount++;
        }
    }

    public Object clone() {
        try {
            LinkedList linkedList = (LinkedList) super.clone();
            linkedList.size = 0;
            linkedList.voidLink = new Link<>(null, null, null);
            linkedList.voidLink.previous = linkedList.voidLink;
            linkedList.voidLink.next = linkedList.voidLink;
            linkedList.addAll(this);
            return linkedList;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean contains(Object object) {
        Link link = this.voidLink.next;
        if (object != null) {
            while (link != this.voidLink) {
                if (object.equals(link.data)) {
                    return true;
                }
                link = link.next;
            }
        } else {
            while (link != this.voidLink) {
                if (link.data == 0) {
                    return true;
                }
                link = link.next;
            }
        }
        return false;
    }

    @Override
    public E get(int i) {
        if (i >= 0 && i < this.size) {
            Link link = this.voidLink;
            if (i < this.size / 2) {
                for (int i2 = 0; i2 <= i; i2++) {
                    link = link.next;
                }
            } else {
                for (int i3 = this.size; i3 > i; i3--) {
                    link = link.previous;
                }
            }
            return (E) link.data;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public E getFirst() {
        return getFirstImpl();
    }

    private E getFirstImpl() {
        Link<E> first = this.voidLink.next;
        if (first != this.voidLink) {
            return first.data;
        }
        throw new NoSuchElementException();
    }

    @Override
    public E getLast() {
        Link<E> last = this.voidLink.previous;
        if (last != this.voidLink) {
            return last.data;
        }
        throw new NoSuchElementException();
    }

    @Override
    public int indexOf(Object object) {
        int pos = 0;
        Link link = this.voidLink.next;
        if (object != null) {
            while (link != this.voidLink) {
                if (object.equals(link.data)) {
                    return pos;
                }
                link = link.next;
                pos++;
            }
        } else {
            while (link != this.voidLink) {
                if (link.data == 0) {
                    return pos;
                }
                link = link.next;
                pos++;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object object) {
        int pos = this.size;
        Link link = this.voidLink.previous;
        if (object != null) {
            while (link != this.voidLink) {
                pos--;
                if (object.equals(link.data)) {
                    return pos;
                }
                link = link.previous;
            }
        } else {
            while (link != this.voidLink) {
                pos--;
                if (link.data == 0) {
                    return pos;
                }
                link = link.previous;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator(int location) {
        return new LinkIterator(this, location);
    }

    @Override
    public E remove(int i) {
        if (i >= 0 && i < this.size) {
            Link link = this.voidLink;
            if (i < this.size / 2) {
                for (int i2 = 0; i2 <= i; i2++) {
                    link = link.next;
                }
            } else {
                for (int i3 = this.size; i3 > i; i3--) {
                    link = link.previous;
                }
            }
            Link<ET> link2 = link.previous;
            Link<ET> link3 = link.next;
            link2.next = link3;
            link3.previous = link2;
            this.size--;
            this.modCount++;
            return (E) link.data;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean remove(Object object) {
        return removeFirstOccurrenceImpl(object);
    }

    @Override
    public E removeFirst() {
        return removeFirstImpl();
    }

    private E removeFirstImpl() {
        Link<E> link = this.voidLink.next;
        if (link != this.voidLink) {
            Link<E> link2 = link.next;
            this.voidLink.next = link2;
            link2.previous = this.voidLink;
            this.size--;
            this.modCount++;
            return link.data;
        }
        throw new NoSuchElementException();
    }

    @Override
    public E removeLast() {
        return removeLastImpl();
    }

    private E removeLastImpl() {
        Link<E> link = this.voidLink.previous;
        if (link != this.voidLink) {
            Link<E> link2 = link.previous;
            this.voidLink.previous = link2;
            link2.next = this.voidLink;
            this.size--;
            this.modCount++;
            return link.data;
        }
        throw new NoSuchElementException();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReverseLinkIterator(this);
    }

    @Override
    public boolean offerFirst(E e) {
        return addFirstImpl(e);
    }

    @Override
    public boolean offerLast(E e) {
        return addLastImpl(e);
    }

    @Override
    public E peekFirst() {
        return peekFirstImpl();
    }

    @Override
    public E peekLast() {
        Link<E> last = this.voidLink.previous;
        if (last == this.voidLink) {
            return null;
        }
        return last.data;
    }

    @Override
    public E pollFirst() {
        if (this.size == 0) {
            return null;
        }
        return removeFirstImpl();
    }

    @Override
    public E pollLast() {
        if (this.size == 0) {
            return null;
        }
        return removeLastImpl();
    }

    @Override
    public E pop() {
        return removeFirstImpl();
    }

    @Override
    public void push(E e) {
        addFirstImpl(e);
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return removeFirstOccurrenceImpl(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        Iterator<E> iter = new ReverseLinkIterator<>(this);
        return removeOneOccurrence(o, iter);
    }

    private boolean removeFirstOccurrenceImpl(Object o) {
        Iterator<E> iter = new LinkIterator<>(this, 0);
        return removeOneOccurrence(o, iter);
    }

    private boolean removeOneOccurrence(Object o, Iterator<E> iter) {
        while (iter.hasNext()) {
            E element = iter.next();
            if (o == null) {
                if (element == null) {
                    iter.remove();
                    return true;
                }
            } else if (o.equals(element)) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public E set(int i, E e) {
        if (i >= 0 && i < this.size) {
            Link link = this.voidLink;
            if (i < this.size / 2) {
                for (int i2 = 0; i2 <= i; i2++) {
                    link = link.next;
                }
            } else {
                for (int i3 = this.size; i3 > i; i3--) {
                    link = link.previous;
                }
            }
            E e2 = (E) link.data;
            link.data = e;
            return e2;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean offer(E o) {
        return addLastImpl(o);
    }

    @Override
    public E poll() {
        if (this.size == 0) {
            return null;
        }
        return removeFirst();
    }

    @Override
    public E remove() {
        return removeFirstImpl();
    }

    @Override
    public E peek() {
        return peekFirstImpl();
    }

    private E peekFirstImpl() {
        Link<E> first = this.voidLink.next;
        if (first == this.voidLink) {
            return null;
        }
        return first.data;
    }

    @Override
    public E element() {
        return getFirstImpl();
    }

    @Override
    public Object[] toArray() {
        int index = 0;
        Object[] contents = new Object[this.size];
        Link link = this.voidLink.next;
        while (link != this.voidLink) {
            contents[index] = link.data;
            link = link.next;
            index++;
        }
        return contents;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        int i = 0;
        if (this.size > tArr.length) {
            tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), this.size));
        }
        Link link = this.voidLink.next;
        while (link != this.voidLink) {
            tArr[i] = link.data;
            link = link.next;
            i++;
        }
        if (i < tArr.length) {
            tArr[i] = null;
        }
        return tArr;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.size);
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            stream.writeObject(it.next());
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.size = objectInputStream.readInt();
        this.voidLink = new Link<>(null, null, null);
        Link<E> link = this.voidLink;
        int i = this.size;
        ?? r1 = link;
        while (true) {
            i--;
            if (i >= 0) {
                Link link2 = new Link(objectInputStream.readObject(), r1, null);
                r1.next = link2;
                r1 = (Link<E>) link2;
            } else {
                r1.next = (Link<E>) this.voidLink;
                this.voidLink.previous = r1;
                return;
            }
        }
    }
}
