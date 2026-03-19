package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import sun.misc.Unsafe;

public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private static final long LOCK;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 8673264195747942595L;
    private volatile transient Object[] elements;
    final transient Object lock = new Object();

    final Object[] getArray() {
        return this.elements;
    }

    final void setArray(Object[] objArr) {
        this.elements = objArr;
    }

    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    public CopyOnWriteArrayList(Collection<? extends E> collection) {
        Object[] array;
        if (collection.getClass() == CopyOnWriteArrayList.class) {
            array = ((CopyOnWriteArrayList) collection).getArray();
        } else {
            array = collection.toArray();
            if (array.getClass() != Object[].class) {
                array = Arrays.copyOf(array, array.length, Object[].class);
            }
        }
        setArray(array);
    }

    public CopyOnWriteArrayList(E[] eArr) {
        setArray(Arrays.copyOf(eArr, eArr.length, Object[].class));
    }

    @Override
    public int size() {
        return getArray().length;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    private static int indexOf(Object obj, Object[] objArr, int i, int i2) {
        if (obj == null) {
            while (i < i2) {
                if (objArr[i] != null) {
                    i++;
                } else {
                    return i;
                }
            }
            return -1;
        }
        while (i < i2) {
            if (!obj.equals(objArr[i])) {
                i++;
            } else {
                return i;
            }
        }
        return -1;
    }

    private static int lastIndexOf(Object obj, Object[] objArr, int i) {
        if (obj == null) {
            while (i >= 0) {
                if (objArr[i] != null) {
                    i--;
                } else {
                    return i;
                }
            }
            return -1;
        }
        while (i >= 0) {
            if (!obj.equals(objArr[i])) {
                i--;
            } else {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(Object obj) {
        Object[] array = getArray();
        return indexOf(obj, array, 0, array.length) >= 0;
    }

    @Override
    public int indexOf(Object obj) {
        Object[] array = getArray();
        return indexOf(obj, array, 0, array.length);
    }

    public int indexOf(E e, int i) {
        Object[] array = getArray();
        return indexOf(e, array, i, array.length);
    }

    @Override
    public int lastIndexOf(Object obj) {
        return lastIndexOf(obj, getArray(), r0.length - 1);
    }

    public int lastIndexOf(E e, int i) {
        return lastIndexOf(e, getArray(), i);
    }

    public Object clone() {
        try {
            CopyOnWriteArrayList copyOnWriteArrayList = (CopyOnWriteArrayList) super.clone();
            copyOnWriteArrayList.resetLock();
            return copyOnWriteArrayList;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    @Override
    public Object[] toArray() {
        Object[] array = getArray();
        return Arrays.copyOf(array, array.length);
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        Object[] array = getArray();
        int length = array.length;
        if (tArr.length < length) {
            return (T[]) Arrays.copyOf(array, length, tArr.getClass());
        }
        System.arraycopy(array, 0, tArr, 0, length);
        if (tArr.length > length) {
            tArr[length] = null;
        }
        return tArr;
    }

    private E get(Object[] objArr, int i) {
        return (E) objArr[i];
    }

    static String outOfBounds(int i, int i2) {
        return "Index: " + i + ", Size: " + i2;
    }

    @Override
    public E get(int i) {
        return get(getArray(), i);
    }

    @Override
    public E set(int i, E e) {
        E e2;
        synchronized (this.lock) {
            Object[] array = getArray();
            e2 = get(array, i);
            if (e2 != e) {
                Object[] objArrCopyOf = Arrays.copyOf(array, array.length);
                objArrCopyOf[i] = e;
                setArray(objArrCopyOf);
            } else {
                setArray(array);
            }
        }
        return e2;
    }

    @Override
    public boolean add(E e) {
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            Object[] objArrCopyOf = Arrays.copyOf(array, length + 1);
            objArrCopyOf[length] = e;
            setArray(objArrCopyOf);
        }
        return true;
    }

    @Override
    public void add(int i, E e) {
        Object[] objArrCopyOf;
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            if (i > length || i < 0) {
                throw new IndexOutOfBoundsException(outOfBounds(i, length));
            }
            int i2 = length - i;
            if (i2 == 0) {
                objArrCopyOf = Arrays.copyOf(array, length + 1);
            } else {
                Object[] objArr = new Object[length + 1];
                System.arraycopy(array, 0, objArr, 0, i);
                System.arraycopy(array, i, objArr, i + 1, i2);
                objArrCopyOf = objArr;
            }
            objArrCopyOf[i] = e;
            setArray(objArrCopyOf);
        }
    }

    @Override
    public E remove(int i) {
        E e;
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            e = get(array, i);
            int i2 = (length - i) - 1;
            if (i2 == 0) {
                setArray(Arrays.copyOf(array, length - 1));
            } else {
                Object[] objArr = new Object[length - 1];
                System.arraycopy(array, 0, objArr, 0, i);
                System.arraycopy(array, i + 1, objArr, i, i2);
                setArray(objArr);
            }
        }
        return e;
    }

    @Override
    public boolean remove(Object obj) {
        Object[] array = getArray();
        int iIndexOf = indexOf(obj, array, 0, array.length);
        if (iIndexOf < 0) {
            return false;
        }
        return remove(obj, array, iIndexOf);
    }

    private boolean remove(Object obj, Object[] objArr, int i) {
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            if (objArr != array) {
                int iMin = Math.min(i, length);
                int i2 = 0;
                while (true) {
                    if (i2 < iMin) {
                        if (array[i2] == objArr[i2] || !Objects.equals(obj, array[i2])) {
                            i2++;
                        } else {
                            i = i2;
                            break;
                        }
                    } else {
                        if (i >= length) {
                            return false;
                        }
                        if (array[i] != obj && (i = indexOf(obj, array, i, length)) < 0) {
                            return false;
                        }
                    }
                }
            }
            Object[] objArr2 = new Object[length - 1];
            System.arraycopy(array, 0, objArr2, 0, i);
            System.arraycopy(array, i + 1, objArr2, i, (length - i) - 1);
            setArray(objArr2);
            return true;
        }
    }

    void removeRange(int i, int i2) {
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            if (i < 0 || i2 > length || i2 < i) {
                throw new IndexOutOfBoundsException();
            }
            int i3 = length - (i2 - i);
            int i4 = length - i2;
            if (i4 == 0) {
                setArray(Arrays.copyOf(array, i3));
            } else {
                Object[] objArr = new Object[i3];
                System.arraycopy(array, 0, objArr, 0, i);
                System.arraycopy(array, i2, objArr, i, i4);
                setArray(objArr);
            }
        }
    }

    public boolean addIfAbsent(E e) {
        Object[] array = getArray();
        if (indexOf(e, array, 0, array.length) >= 0) {
            return false;
        }
        return addIfAbsent(e, array);
    }

    private boolean addIfAbsent(E e, Object[] objArr) {
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            if (objArr != array) {
                int iMin = Math.min(objArr.length, length);
                for (int i = 0; i < iMin; i++) {
                    if (array[i] != objArr[i] && Objects.equals(e, array[i])) {
                        return false;
                    }
                }
                if (indexOf(e, array, iMin, length) >= 0) {
                    return false;
                }
            }
            Object[] objArrCopyOf = Arrays.copyOf(array, length + 1);
            objArrCopyOf[length] = e;
            setArray(objArrCopyOf);
            return true;
        }
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        Object[] array = getArray();
        int length = array.length;
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            if (indexOf(it.next(), array, 0, length) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            if (length != 0) {
                Object[] objArr = new Object[length];
                int i = 0;
                for (Object obj : array) {
                    if (!collection.contains(obj)) {
                        objArr[i] = obj;
                        i++;
                    }
                }
                if (i != length) {
                    setArray(Arrays.copyOf(objArr, i));
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            if (length != 0) {
                Object[] objArr = new Object[length];
                int i = 0;
                for (Object obj : array) {
                    if (collection.contains(obj)) {
                        objArr[i] = obj;
                        i++;
                    }
                }
                if (i != length) {
                    setArray(Arrays.copyOf(objArr, i));
                    return true;
                }
            }
            return false;
        }
    }

    public int addAllAbsent(Collection<? extends E> collection) {
        int i;
        Object[] array = collection.toArray();
        if (array.length == 0) {
            return 0;
        }
        synchronized (this.lock) {
            Object[] array2 = getArray();
            int length = array2.length;
            i = 0;
            for (Object obj : array) {
                if (indexOf(obj, array2, 0, length) < 0 && indexOf(obj, array, 0, i) < 0) {
                    array[i] = obj;
                    i++;
                }
            }
            if (i > 0) {
                Object[] objArrCopyOf = Arrays.copyOf(array2, length + i);
                System.arraycopy(array, 0, objArrCopyOf, length, i);
                setArray(objArrCopyOf);
            }
        }
        return i;
    }

    @Override
    public void clear() {
        synchronized (this.lock) {
            setArray(new Object[0]);
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Object[] array = collection.getClass() == CopyOnWriteArrayList.class ? ((CopyOnWriteArrayList) collection).getArray() : collection.toArray();
        if (array.length == 0) {
            return false;
        }
        synchronized (this.lock) {
            Object[] array2 = getArray();
            int length = array2.length;
            if (length == 0 && array.getClass() == Object[].class) {
                setArray(array);
            } else {
                Object[] objArrCopyOf = Arrays.copyOf(array2, array.length + length);
                System.arraycopy(array, 0, objArrCopyOf, length, array.length);
                setArray(objArrCopyOf);
            }
        }
        return true;
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        Object[] objArrCopyOf;
        Object[] array = collection.toArray();
        synchronized (this.lock) {
            Object[] array2 = getArray();
            int length = array2.length;
            if (i > length || i < 0) {
                throw new IndexOutOfBoundsException(outOfBounds(i, length));
            }
            if (array.length == 0) {
                return false;
            }
            int i2 = length - i;
            if (i2 == 0) {
                objArrCopyOf = Arrays.copyOf(array2, length + array.length);
            } else {
                Object[] objArr = new Object[length + array.length];
                System.arraycopy(array2, 0, objArr, 0, i);
                System.arraycopy(array2, i, objArr, array.length + i, i2);
                objArrCopyOf = objArr;
            }
            System.arraycopy(array, 0, objArrCopyOf, i, array.length);
            setArray(objArrCopyOf);
            return true;
        }
    }

    @Override
    public void forEach(Consumer<? super E> consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }
        for (Object obj : getArray()) {
            consumer.accept(obj);
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            int i = 0;
            while (i < length) {
                if (!predicate.test(array[i])) {
                    i++;
                } else {
                    int i2 = length - 1;
                    Object[] objArrCopyOf = new Object[i2];
                    System.arraycopy(array, 0, objArrCopyOf, 0, i);
                    for (int i3 = i + 1; i3 < length; i3++) {
                        Object obj = array[i3];
                        if (!predicate.test(obj)) {
                            objArrCopyOf[i] = obj;
                            i++;
                        }
                    }
                    if (i != i2) {
                        objArrCopyOf = Arrays.copyOf(objArrCopyOf, i);
                    }
                    setArray(objArrCopyOf);
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void replaceAll(UnaryOperator<E> unaryOperator) {
        if (unaryOperator == 0) {
            throw new NullPointerException();
        }
        synchronized (this.lock) {
            Object[] array = getArray();
            int length = array.length;
            Object[] objArrCopyOf = Arrays.copyOf(array, length);
            for (int i = 0; i < length; i++) {
                objArrCopyOf[i] = unaryOperator.apply(array[i]);
            }
            setArray(objArrCopyOf);
        }
    }

    @Override
    public void sort(Comparator<? super E> comparator) {
        synchronized (this.lock) {
            Object[] array = getArray();
            Object[] objArrCopyOf = Arrays.copyOf(array, array.length);
            Arrays.sort(objArrCopyOf, comparator);
            setArray(objArrCopyOf);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        Object[] array = getArray();
        objectOutputStream.writeInt(array.length);
        for (Object obj : array) {
            objectOutputStream.writeObject(obj);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        resetLock();
        int i = objectInputStream.readInt();
        Object[] objArr = new Object[i];
        for (int i2 = 0; i2 < i; i2++) {
            objArr[i2] = objectInputStream.readObject();
        }
        setArray(objArr);
    }

    public String toString() {
        return Arrays.toString(getArray());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        Iterator<E> it = ((List) obj).iterator();
        for (Object obj2 : getArray()) {
            if (!it.hasNext() || !Objects.equals(obj2, it.next())) {
                return false;
            }
        }
        return !it.hasNext();
    }

    @Override
    public int hashCode() {
        Object[] array = getArray();
        int length = array.length;
        int iHashCode = 1;
        for (int i = 0; i < length; i++) {
            Object obj = array[i];
            iHashCode = (obj == null ? 0 : obj.hashCode()) + (31 * iHashCode);
        }
        return iHashCode;
    }

    @Override
    public Iterator<E> iterator() {
        return new COWIterator(getArray(), 0);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new COWIterator(getArray(), 0);
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        Object[] array = getArray();
        int length = array.length;
        if (i < 0 || i > length) {
            throw new IndexOutOfBoundsException(outOfBounds(i, length));
        }
        return new COWIterator(array, i);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(getArray(), 1040);
    }

    static final class COWIterator<E> implements ListIterator<E> {
        private int cursor;
        private final Object[] snapshot;

        COWIterator(Object[] objArr, int i) {
            this.cursor = i;
            this.snapshot = objArr;
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.snapshot.length;
        }

        @Override
        public boolean hasPrevious() {
            return this.cursor > 0;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Object[] objArr = this.snapshot;
            int i = this.cursor;
            this.cursor = i + 1;
            return (E) objArr[i];
        }

        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            Object[] objArr = this.snapshot;
            int i = this.cursor - 1;
            this.cursor = i;
            return (E) objArr[i];
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
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            int length = this.snapshot.length;
            for (int i = this.cursor; i < length; i++) {
                consumer.accept(this.snapshot[i]);
            }
            this.cursor = length;
        }
    }

    @Override
    public List<E> subList(int i, int i2) {
        COWSubList cOWSubList;
        synchronized (this.lock) {
            int length = getArray().length;
            if (i < 0 || i2 > length || i > i2) {
                throw new IndexOutOfBoundsException();
            }
            cOWSubList = new COWSubList(this, i, i2);
        }
        return cOWSubList;
    }

    private static class COWSubList<E> extends AbstractList<E> implements RandomAccess {
        private Object[] expectedArray;
        private final CopyOnWriteArrayList<E> l;
        private final int offset;
        private int size;

        COWSubList(CopyOnWriteArrayList<E> copyOnWriteArrayList, int i, int i2) {
            this.l = copyOnWriteArrayList;
            this.expectedArray = this.l.getArray();
            this.offset = i;
            this.size = i2 - i;
        }

        private void checkForComodification() {
            if (this.l.getArray() != this.expectedArray) {
                throw new ConcurrentModificationException();
            }
        }

        private void rangeCheck(int i) {
            if (i < 0 || i >= this.size) {
                throw new IndexOutOfBoundsException(CopyOnWriteArrayList.outOfBounds(i, this.size));
            }
        }

        @Override
        public E set(int i, E e) {
            E e2;
            synchronized (this.l.lock) {
                rangeCheck(i);
                checkForComodification();
                e2 = this.l.set(i + this.offset, e);
                this.expectedArray = this.l.getArray();
            }
            return e2;
        }

        @Override
        public E get(int i) {
            E e;
            synchronized (this.l.lock) {
                rangeCheck(i);
                checkForComodification();
                e = this.l.get(i + this.offset);
            }
            return e;
        }

        @Override
        public int size() {
            int i;
            synchronized (this.l.lock) {
                checkForComodification();
                i = this.size;
            }
            return i;
        }

        @Override
        public void add(int i, E e) {
            synchronized (this.l.lock) {
                checkForComodification();
                if (i < 0 || i > this.size) {
                    throw new IndexOutOfBoundsException(CopyOnWriteArrayList.outOfBounds(i, this.size));
                }
                this.l.add(i + this.offset, e);
                this.expectedArray = this.l.getArray();
                this.size++;
            }
        }

        @Override
        public void clear() {
            synchronized (this.l.lock) {
                checkForComodification();
                this.l.removeRange(this.offset, this.offset + this.size);
                this.expectedArray = this.l.getArray();
                this.size = 0;
            }
        }

        @Override
        public E remove(int i) {
            E eRemove;
            synchronized (this.l.lock) {
                rangeCheck(i);
                checkForComodification();
                eRemove = this.l.remove(i + this.offset);
                this.expectedArray = this.l.getArray();
                this.size--;
            }
            return eRemove;
        }

        @Override
        public boolean remove(Object obj) {
            int iIndexOf = indexOf(obj);
            if (iIndexOf == -1) {
                return false;
            }
            remove(iIndexOf);
            return true;
        }

        @Override
        public Iterator<E> iterator() {
            COWSubListIterator cOWSubListIterator;
            synchronized (this.l.lock) {
                checkForComodification();
                cOWSubListIterator = new COWSubListIterator(this.l, 0, this.offset, this.size);
            }
            return cOWSubListIterator;
        }

        @Override
        public ListIterator<E> listIterator(int i) {
            COWSubListIterator cOWSubListIterator;
            synchronized (this.l.lock) {
                checkForComodification();
                if (i < 0 || i > this.size) {
                    throw new IndexOutOfBoundsException(CopyOnWriteArrayList.outOfBounds(i, this.size));
                }
                cOWSubListIterator = new COWSubListIterator(this.l, i, this.offset, this.size);
            }
            return cOWSubListIterator;
        }

        @Override
        public List<E> subList(int i, int i2) {
            COWSubList cOWSubList;
            synchronized (this.l.lock) {
                checkForComodification();
                if (i < 0 || i2 > this.size || i > i2) {
                    throw new IndexOutOfBoundsException();
                }
                cOWSubList = new COWSubList(this.l, i + this.offset, i2 + this.offset);
            }
            return cOWSubList;
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int i = this.offset;
            int i2 = this.offset + this.size;
            Object[] objArr = this.expectedArray;
            if (this.l.getArray() != objArr) {
                throw new ConcurrentModificationException();
            }
            if (i < 0 || i2 > objArr.length) {
                throw new IndexOutOfBoundsException();
            }
            while (i < i2) {
                consumer.accept(objArr[i]);
                i++;
            }
        }

        @Override
        public void replaceAll(UnaryOperator<E> unaryOperator) {
            if (unaryOperator == 0) {
                throw new NullPointerException();
            }
            synchronized (this.l.lock) {
                int i = this.offset;
                int i2 = this.offset + this.size;
                Object[] objArr = this.expectedArray;
                if (this.l.getArray() != objArr) {
                    throw new ConcurrentModificationException();
                }
                int length = objArr.length;
                if (i < 0 || i2 > length) {
                    throw new IndexOutOfBoundsException();
                }
                Object[] objArrCopyOf = Arrays.copyOf(objArr, length);
                while (i < i2) {
                    objArrCopyOf[i] = unaryOperator.apply(objArr[i]);
                    i++;
                }
                CopyOnWriteArrayList<E> copyOnWriteArrayList = this.l;
                this.expectedArray = objArrCopyOf;
                copyOnWriteArrayList.setArray(objArrCopyOf);
            }
        }

        @Override
        public void sort(Comparator<? super E> comparator) {
            synchronized (this.l.lock) {
                int i = this.offset;
                int i2 = this.offset + this.size;
                Object[] objArr = this.expectedArray;
                if (this.l.getArray() != objArr) {
                    throw new ConcurrentModificationException();
                }
                int length = objArr.length;
                if (i < 0 || i2 > length) {
                    throw new IndexOutOfBoundsException();
                }
                Object[] objArrCopyOf = Arrays.copyOf(objArr, length);
                Arrays.sort(objArrCopyOf, i, i2, comparator);
                CopyOnWriteArrayList<E> copyOnWriteArrayList = this.l;
                this.expectedArray = objArrCopyOf;
                copyOnWriteArrayList.setArray(objArrCopyOf);
            }
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean z;
            if (collection == null) {
                throw new NullPointerException();
            }
            synchronized (this.l.lock) {
                int i = this.size;
                z = false;
                if (i > 0) {
                    int i2 = this.offset;
                    int i3 = this.offset + i;
                    Object[] objArr = this.expectedArray;
                    if (this.l.getArray() != objArr) {
                        throw new ConcurrentModificationException();
                    }
                    int length = objArr.length;
                    if (i2 < 0 || i3 > length) {
                        throw new IndexOutOfBoundsException();
                    }
                    Object[] objArr2 = new Object[i];
                    int i4 = 0;
                    for (int i5 = i2; i5 < i3; i5++) {
                        Object obj = objArr[i5];
                        if (!collection.contains(obj)) {
                            objArr2[i4] = obj;
                            i4++;
                        }
                    }
                    if (i4 != i) {
                        Object[] objArr3 = new Object[(length - i) + i4];
                        System.arraycopy(objArr, 0, objArr3, 0, i2);
                        System.arraycopy(objArr2, 0, objArr3, i2, i4);
                        System.arraycopy(objArr, i3, objArr3, i2 + i4, length - i3);
                        this.size = i4;
                        CopyOnWriteArrayList<E> copyOnWriteArrayList = this.l;
                        this.expectedArray = objArr3;
                        copyOnWriteArrayList.setArray(objArr3);
                        z = true;
                    }
                }
            }
            return z;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean z;
            if (collection == null) {
                throw new NullPointerException();
            }
            synchronized (this.l.lock) {
                int i = this.size;
                z = false;
                if (i > 0) {
                    int i2 = this.offset;
                    int i3 = this.offset + i;
                    Object[] objArr = this.expectedArray;
                    if (this.l.getArray() != objArr) {
                        throw new ConcurrentModificationException();
                    }
                    int length = objArr.length;
                    if (i2 < 0 || i3 > length) {
                        throw new IndexOutOfBoundsException();
                    }
                    Object[] objArr2 = new Object[i];
                    int i4 = 0;
                    for (int i5 = i2; i5 < i3; i5++) {
                        Object obj = objArr[i5];
                        if (collection.contains(obj)) {
                            objArr2[i4] = obj;
                            i4++;
                        }
                    }
                    if (i4 != i) {
                        Object[] objArr3 = new Object[(length - i) + i4];
                        System.arraycopy(objArr, 0, objArr3, 0, i2);
                        System.arraycopy(objArr2, 0, objArr3, i2, i4);
                        System.arraycopy(objArr, i3, objArr3, i2 + i4, length - i3);
                        this.size = i4;
                        CopyOnWriteArrayList<E> copyOnWriteArrayList = this.l;
                        this.expectedArray = objArr3;
                        copyOnWriteArrayList.setArray(objArr3);
                        z = true;
                    }
                }
            }
            return z;
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            boolean z;
            if (predicate == null) {
                throw new NullPointerException();
            }
            synchronized (this.l.lock) {
                int i = this.size;
                z = false;
                if (i > 0) {
                    int i2 = this.offset;
                    int i3 = this.offset + i;
                    Object[] objArr = this.expectedArray;
                    if (this.l.getArray() != objArr) {
                        throw new ConcurrentModificationException();
                    }
                    int length = objArr.length;
                    if (i2 < 0 || i3 > length) {
                        throw new IndexOutOfBoundsException();
                    }
                    Object[] objArr2 = new Object[i];
                    int i4 = 0;
                    for (int i5 = i2; i5 < i3; i5++) {
                        Object obj = objArr[i5];
                        if (!predicate.test(obj)) {
                            objArr2[i4] = obj;
                            i4++;
                        }
                    }
                    if (i4 != i) {
                        Object[] objArr3 = new Object[(length - i) + i4];
                        System.arraycopy(objArr, 0, objArr3, 0, i2);
                        System.arraycopy(objArr2, 0, objArr3, i2, i4);
                        System.arraycopy(objArr, i3, objArr3, i2 + i4, length - i3);
                        this.size = i4;
                        CopyOnWriteArrayList<E> copyOnWriteArrayList = this.l;
                        this.expectedArray = objArr3;
                        copyOnWriteArrayList.setArray(objArr3);
                        z = true;
                    }
                }
            }
            return z;
        }

        @Override
        public Spliterator<E> spliterator() {
            int i = this.offset;
            int i2 = this.offset + this.size;
            Object[] objArr = this.expectedArray;
            if (this.l.getArray() != objArr) {
                throw new ConcurrentModificationException();
            }
            if (i < 0 || i2 > objArr.length) {
                throw new IndexOutOfBoundsException();
            }
            return Spliterators.spliterator(objArr, i, i2, 1040);
        }
    }

    private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<E> list, int i, int i2, int i3) {
            this.offset = i2;
            this.size = i3;
            this.it = list.listIterator(i + i2);
        }

        @Override
        public boolean hasNext() {
            return nextIndex() < this.size;
        }

        @Override
        public E next() {
            if (hasNext()) {
                return this.it.next();
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
                return this.it.previous();
            }
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return this.it.nextIndex() - this.offset;
        }

        @Override
        public int previousIndex() {
            return this.it.previousIndex() - this.offset;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            while (nextIndex() < this.size) {
                consumer.accept(this.it.next());
            }
        }
    }

    private void resetLock() {
        U.putObjectVolatile(this, LOCK, new Object());
    }

    static {
        try {
            LOCK = U.objectFieldOffset(CopyOnWriteArrayList.class.getDeclaredField("lock"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
