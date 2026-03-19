package java.util;

import dalvik.system.VMRuntime;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Collections {
    private static final int BINARYSEARCH_THRESHOLD = 5000;
    private static final int COPY_THRESHOLD = 10;
    public static final List EMPTY_LIST;
    public static final Map EMPTY_MAP;
    public static final Set EMPTY_SET;
    private static final int FILL_THRESHOLD = 25;
    private static final int INDEXOFSUBLIST_THRESHOLD = 35;
    private static final int REPLACEALL_THRESHOLD = 11;
    private static final int REVERSE_THRESHOLD = 18;
    private static final int ROTATE_THRESHOLD = 100;
    private static final int SHUFFLE_THRESHOLD = 5;
    private static Random r;

    private Collections() {
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        sort(list, null);
    }

    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        if (VMRuntime.getRuntime().getTargetSdkVersion() > FILL_THRESHOLD) {
            list.sort(comparator);
            return;
        }
        if (list.getClass() == ArrayList.class) {
            Arrays.sort(((ArrayList) list).elementData, 0, list.size(), comparator);
            return;
        }
        Object[] array = list.toArray();
        Arrays.sort(array, comparator);
        ListIterator<T> listIterator = list.listIterator();
        for (Object obj : array) {
            listIterator.next();
            listIterator.set(obj);
        }
    }

    public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T t) {
        if ((list instanceof RandomAccess) || list.size() < BINARYSEARCH_THRESHOLD) {
            return indexedBinarySearch(list, t);
        }
        return iteratorBinarySearch(list, t);
    }

    private static <T> int indexedBinarySearch(List<? extends Comparable<? super T>> list, T t) {
        int size = list.size() - 1;
        int i = 0;
        while (i <= size) {
            int i2 = (i + size) >>> 1;
            int iCompareTo = list.get(i2).compareTo(t);
            if (iCompareTo < 0) {
                i = i2 + 1;
            } else if (iCompareTo > 0) {
                size = i2 - 1;
            } else {
                return i2;
            }
        }
        return -(i + 1);
    }

    private static <T> int iteratorBinarySearch(List<? extends Comparable<? super T>> list, T t) {
        int size = list.size() - 1;
        ListIterator<? extends Comparable<? super T>> listIterator = list.listIterator();
        int i = 0;
        while (i <= size) {
            int i2 = (i + size) >>> 1;
            int iCompareTo = ((Comparable) get(listIterator, i2)).compareTo(t);
            if (iCompareTo < 0) {
                i = i2 + 1;
            } else if (iCompareTo > 0) {
                size = i2 - 1;
            } else {
                return i2;
            }
        }
        return -(i + 1);
    }

    private static <T> T get(ListIterator<? extends T> listIterator, int i) {
        T tPrevious;
        int iNextIndex = listIterator.nextIndex();
        if (iNextIndex <= i) {
            while (true) {
                tPrevious = listIterator.next();
                int i2 = iNextIndex + 1;
                if (iNextIndex >= i) {
                    break;
                }
                iNextIndex = i2;
            }
        } else {
            do {
                tPrevious = listIterator.previous();
                iNextIndex--;
            } while (iNextIndex > i);
        }
        return tPrevious;
    }

    public static <T> int binarySearch(List<? extends T> list, T t, Comparator<? super T> comparator) {
        if (comparator == null) {
            return binarySearch(list, t);
        }
        if ((list instanceof RandomAccess) || list.size() < BINARYSEARCH_THRESHOLD) {
            return indexedBinarySearch(list, t, comparator);
        }
        return iteratorBinarySearch(list, t, comparator);
    }

    private static <T> int indexedBinarySearch(List<? extends T> list, T t, Comparator<? super T> comparator) {
        int size = list.size() - 1;
        int i = 0;
        while (i <= size) {
            int i2 = (i + size) >>> 1;
            int iCompare = comparator.compare(list.get(i2), t);
            if (iCompare < 0) {
                i = i2 + 1;
            } else if (iCompare > 0) {
                size = i2 - 1;
            } else {
                return i2;
            }
        }
        return -(i + 1);
    }

    private static <T> int iteratorBinarySearch(List<? extends T> list, T t, Comparator<? super T> comparator) {
        int size = list.size() - 1;
        ListIterator<? extends T> listIterator = list.listIterator();
        int i = 0;
        while (i <= size) {
            int i2 = (i + size) >>> 1;
            int iCompare = comparator.compare((Object) get(listIterator, i2), t);
            if (iCompare < 0) {
                i = i2 + 1;
            } else if (iCompare > 0) {
                size = i2 - 1;
            } else {
                return i2;
            }
        }
        return -(i + 1);
    }

    public static void reverse(List<?> list) {
        int size = list.size();
        int i = 0;
        if (size < 18 || (list instanceof RandomAccess)) {
            int i2 = size >> 1;
            int i3 = size - 1;
            while (i < i2) {
                swap(list, i, i3);
                i++;
                i3--;
            }
            return;
        }
        ListIterator<?> listIterator = list.listIterator();
        ListIterator<?> listIterator2 = list.listIterator(size);
        int size2 = list.size() >> 1;
        while (i < size2) {
            Object next = listIterator.next();
            listIterator.set(listIterator2.previous());
            listIterator2.set(next);
            i++;
        }
    }

    public static void shuffle(List<?> list) {
        Random random = r;
        if (random == null) {
            random = new Random();
            r = random;
        }
        shuffle(list, random);
    }

    public static void shuffle(List<?> list, Random random) {
        int size = list.size();
        if (size < 5 || (list instanceof RandomAccess)) {
            while (size > 1) {
                swap(list, size - 1, random.nextInt(size));
                size--;
            }
            return;
        }
        Object[] array = list.toArray();
        while (size > 1) {
            swap(array, size - 1, random.nextInt(size));
            size--;
        }
        ListIterator<?> listIterator = list.listIterator();
        for (Object obj : array) {
            listIterator.next();
            listIterator.set(obj);
        }
    }

    public static void swap(List<?> list, int i, int i2) {
        list.set(i, list.set(i2, list.get(i)));
    }

    private static void swap(Object[] objArr, int i, int i2) {
        Object obj = objArr[i];
        objArr[i] = objArr[i2];
        objArr[i2] = obj;
    }

    public static <T> void fill(List<? super T> list, T t) {
        int size = list.size();
        int i = 0;
        if (size < FILL_THRESHOLD || (list instanceof RandomAccess)) {
            while (i < size) {
                list.set(i, t);
                i++;
            }
        } else {
            ListIterator<? super T> listIterator = list.listIterator();
            while (i < size) {
                listIterator.next();
                listIterator.set(t);
                i++;
            }
        }
    }

    public static <T> void copy(List<? super T> list, List<? extends T> list2) {
        int size = list2.size();
        if (size > list.size()) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }
        int i = 0;
        if (size < 10 || ((list2 instanceof RandomAccess) && (list instanceof RandomAccess))) {
            while (i < size) {
                list.set(i, list2.get(i));
                i++;
            }
        } else {
            ListIterator<? super T> listIterator = list.listIterator();
            ListIterator<? extends T> listIterator2 = list2.listIterator();
            while (i < size) {
                listIterator.next();
                listIterator.set(listIterator2.next());
                i++;
            }
        }
    }

    public static <T extends Comparable<? super T>> T min(Collection<? extends T> collection) {
        Iterator<? extends T> it = collection.iterator();
        T next = it.next();
        while (it.hasNext()) {
            T next2 = it.next();
            if (next2.compareTo(next) < 0) {
                next = next2;
            }
        }
        return next;
    }

    public static <T> T min(Collection<? extends T> collection, Comparator<? super T> comparator) {
        if (comparator == null) {
            return (T) min(collection);
        }
        Iterator<? extends T> it = collection.iterator();
        T next = it.next();
        while (it.hasNext()) {
            T next2 = it.next();
            if (comparator.compare(next2, next) < 0) {
                next = next2;
            }
        }
        return next;
    }

    public static <T extends Comparable<? super T>> T max(Collection<? extends T> collection) {
        Iterator<? extends T> it = collection.iterator();
        T next = it.next();
        while (it.hasNext()) {
            T next2 = it.next();
            if (next2.compareTo(next) > 0) {
                next = next2;
            }
        }
        return next;
    }

    public static <T> T max(Collection<? extends T> collection, Comparator<? super T> comparator) {
        if (comparator == null) {
            return (T) max(collection);
        }
        Iterator<? extends T> it = collection.iterator();
        T next = it.next();
        while (it.hasNext()) {
            T next2 = it.next();
            if (comparator.compare(next2, next) > 0) {
                next = next2;
            }
        }
        return next;
    }

    public static void rotate(List<?> list, int i) {
        if ((list instanceof RandomAccess) || list.size() < ROTATE_THRESHOLD) {
            rotate1(list, i);
        } else {
            rotate2(list, i);
        }
    }

    private static <T> void rotate1(List<T> list, int i) {
        int size = list.size();
        if (size == 0) {
            return;
        }
        int i2 = i % size;
        if (i2 < 0) {
            i2 += size;
        }
        if (i2 == 0) {
            return;
        }
        int i3 = 0;
        int i4 = 0;
        while (i3 != size) {
            T t = list.get(i4);
            int i5 = i3;
            int i6 = i4;
            do {
                i6 += i2;
                if (i6 >= size) {
                    i6 -= size;
                }
                t = list.set(i6, t);
                i5++;
            } while (i6 != i4);
            i4++;
            i3 = i5;
        }
    }

    private static void rotate2(List<?> list, int i) {
        int size = list.size();
        if (size == 0) {
            return;
        }
        int i2 = (-i) % size;
        if (i2 < 0) {
            i2 += size;
        }
        if (i2 == 0) {
            return;
        }
        reverse(list.subList(0, i2));
        reverse(list.subList(i2, size));
        reverse(list);
    }

    public static <T> boolean replaceAll(List<T> list, T t, T t2) {
        boolean z;
        int size = list.size();
        int i = 0;
        if (size < 11 || (list instanceof RandomAccess)) {
            if (t == null) {
                boolean z2 = false;
                while (i < size) {
                    if (list.get(i) == null) {
                        list.set(i, t2);
                        z2 = true;
                    }
                    i++;
                }
                return z2;
            }
            z = false;
            while (i < size) {
                if (t.equals(list.get(i))) {
                    list.set(i, t2);
                    z = true;
                }
                i++;
            }
        } else {
            ListIterator<T> listIterator = list.listIterator();
            if (t == null) {
                boolean z3 = false;
                while (i < size) {
                    if (listIterator.next() == null) {
                        listIterator.set(t2);
                        z3 = true;
                    }
                    i++;
                }
                return z3;
            }
            z = false;
            while (i < size) {
                if (t.equals(listIterator.next())) {
                    listIterator.set(t2);
                    z = true;
                }
                i++;
            }
        }
        return z;
    }

    public static int indexOfSubList(List<?> list, List<?> list2) {
        int size = list.size();
        int size2 = list2.size();
        int i = size - size2;
        if (size < INDEXOFSUBLIST_THRESHOLD || ((list instanceof RandomAccess) && (list2 instanceof RandomAccess))) {
            int i2 = 0;
            while (i2 <= i) {
                int i3 = i2;
                int i4 = 0;
                while (i4 < size2) {
                    if (!eq(list2.get(i4), list.get(i3))) {
                        break;
                    }
                    i4++;
                    i3++;
                }
                return i2;
            }
            return -1;
        }
        ListIterator<?> listIterator = list.listIterator();
        for (int i5 = 0; i5 <= i; i5++) {
            ListIterator<?> listIterator2 = list2.listIterator();
            for (int i6 = 0; i6 < size2; i6++) {
                if (!eq(listIterator2.next(), listIterator.next())) {
                    for (int i7 = 0; i7 < i6; i7++) {
                        listIterator.previous();
                    }
                }
            }
            return i5;
        }
        return -1;
    }

    public static int lastIndexOfSubList(List<?> list, List<?> list2) {
        int size = list.size();
        int size2 = list2.size();
        int i = size - size2;
        if (size < INDEXOFSUBLIST_THRESHOLD || (list instanceof RandomAccess)) {
            while (i >= 0) {
                int i2 = i;
                int i3 = 0;
                while (i3 < size2) {
                    if (!eq(list2.get(i3), list.get(i2))) {
                        break;
                    }
                    i3++;
                    i2++;
                }
                return i;
            }
        }
        if (i < 0) {
            return -1;
        }
        ListIterator<?> listIterator = list.listIterator(i);
        while (i >= 0) {
            ListIterator<?> listIterator2 = list2.listIterator();
            for (int i4 = 0; i4 < size2; i4++) {
                if (!eq(listIterator2.next(), listIterator.next())) {
                    if (i != 0) {
                        for (int i5 = 0; i5 <= i4 + 1; i5++) {
                            listIterator.previous();
                        }
                    }
                    i--;
                }
            }
            return i;
        }
        return -1;
    }

    public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> collection) {
        return new UnmodifiableCollection(collection);
    }

    static class UnmodifiableCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 1820017752578914078L;
        final Collection<? extends E> c;

        UnmodifiableCollection(Collection<? extends E> collection) {
            if (collection == null) {
                throw new NullPointerException();
            }
            this.c = collection;
        }

        @Override
        public int size() {
            return this.c.size();
        }

        @Override
        public boolean isEmpty() {
            return this.c.isEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            return this.c.contains(obj);
        }

        @Override
        public Object[] toArray() {
            return this.c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.c.toArray(tArr);
        }

        public String toString() {
            return this.c.toString();
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private final Iterator<? extends E> i;

                {
                    this.i = UnmodifiableCollection.this.c.iterator();
                }

                @Override
                public boolean hasNext() {
                    return this.i.hasNext();
                }

                @Override
                public E next() {
                    return this.i.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void forEachRemaining(Consumer<? super E> consumer) {
                    this.i.forEachRemaining(consumer);
                }
            };
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.c.containsAll(collection);
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            this.c.forEach(consumer);
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Spliterator<E> spliterator() {
            return this.c.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return this.c.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return this.c.parallelStream();
        }
    }

    public static <T> Set<T> unmodifiableSet(Set<? extends T> set) {
        return new UnmodifiableSet(set);
    }

    static class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E>, Serializable {
        private static final long serialVersionUID = -9215047833775013803L;

        UnmodifiableSet(Set<? extends E> set) {
            super(set);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.c.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.c.hashCode();
        }
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> sortedSet) {
        return new UnmodifiableSortedSet(sortedSet);
    }

    static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> implements SortedSet<E>, Serializable {
        private static final long serialVersionUID = -4929149591599911165L;
        private final SortedSet<E> ss;

        UnmodifiableSortedSet(SortedSet<E> sortedSet) {
            super(sortedSet);
            this.ss = sortedSet;
        }

        @Override
        public Comparator<? super E> comparator() {
            return this.ss.comparator();
        }

        @Override
        public SortedSet<E> subSet(E e, E e2) {
            return new UnmodifiableSortedSet(this.ss.subSet(e, e2));
        }

        @Override
        public SortedSet<E> headSet(E e) {
            return new UnmodifiableSortedSet(this.ss.headSet(e));
        }

        @Override
        public SortedSet<E> tailSet(E e) {
            return new UnmodifiableSortedSet(this.ss.tailSet(e));
        }

        @Override
        public E first() {
            return this.ss.first();
        }

        @Override
        public E last() {
            return this.ss.last();
        }
    }

    public static <T> NavigableSet<T> unmodifiableNavigableSet(NavigableSet<T> navigableSet) {
        return new UnmodifiableNavigableSet(navigableSet);
    }

    static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E> implements NavigableSet<E>, Serializable {
        private static final NavigableSet<?> EMPTY_NAVIGABLE_SET = new EmptyNavigableSet();
        private static final long serialVersionUID = -6027448201786391929L;
        private final NavigableSet<E> ns;

        private static class EmptyNavigableSet<E> extends UnmodifiableNavigableSet<E> implements Serializable {
            private static final long serialVersionUID = -6291252904449939134L;

            public EmptyNavigableSet() {
                super(new TreeSet());
            }

            private Object readResolve() {
                return UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
            }
        }

        UnmodifiableNavigableSet(NavigableSet<E> navigableSet) {
            super(navigableSet);
            this.ns = navigableSet;
        }

        @Override
        public E lower(E e) {
            return this.ns.lower(e);
        }

        @Override
        public E floor(E e) {
            return this.ns.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return this.ns.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return this.ns.higher(e);
        }

        @Override
        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return new UnmodifiableNavigableSet(this.ns.descendingSet());
        }

        @Override
        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
            return new UnmodifiableNavigableSet(this.ns.subSet(e, z, e2, z2));
        }

        @Override
        public NavigableSet<E> headSet(E e, boolean z) {
            return new UnmodifiableNavigableSet(this.ns.headSet(e, z));
        }

        @Override
        public NavigableSet<E> tailSet(E e, boolean z) {
            return new UnmodifiableNavigableSet(this.ns.tailSet(e, z));
        }
    }

    public static <T> List<T> unmodifiableList(List<? extends T> list) {
        if (list instanceof RandomAccess) {
            return new UnmodifiableRandomAccessList(list);
        }
        return new UnmodifiableList(list);
    }

    static class UnmodifiableList<E> extends UnmodifiableCollection<E> implements List<E> {
        private static final long serialVersionUID = -283967356065247728L;
        final List<? extends E> list;

        UnmodifiableList(List<? extends E> list) {
            super(list);
            this.list = list;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.list.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.list.hashCode();
        }

        @Override
        public E get(int i) {
            return this.list.get(i);
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
            return this.list.indexOf(obj);
        }

        @Override
        public int lastIndexOf(Object obj) {
            return this.list.lastIndexOf(obj);
        }

        @Override
        public boolean addAll(int i, Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(UnaryOperator<E> unaryOperator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super E> comparator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(final int i) {
            return new ListIterator<E>() {
                private final ListIterator<? extends E> i;

                {
                    this.i = UnmodifiableList.this.list.listIterator(i);
                }

                @Override
                public boolean hasNext() {
                    return this.i.hasNext();
                }

                @Override
                public E next() {
                    return this.i.next();
                }

                @Override
                public boolean hasPrevious() {
                    return this.i.hasPrevious();
                }

                @Override
                public E previous() {
                    return this.i.previous();
                }

                @Override
                public int nextIndex() {
                    return this.i.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return this.i.previousIndex();
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
                    this.i.forEachRemaining(consumer);
                }
            };
        }

        @Override
        public List<E> subList(int i, int i2) {
            return new UnmodifiableList(this.list.subList(i, i2));
        }

        private Object readResolve() {
            if (!(this.list instanceof RandomAccess)) {
                return this;
            }
            return new UnmodifiableRandomAccessList(this.list);
        }
    }

    static class UnmodifiableRandomAccessList<E> extends UnmodifiableList<E> implements RandomAccess {
        private static final long serialVersionUID = -2542308836966382001L;

        UnmodifiableRandomAccessList(List<? extends E> list) {
            super(list);
        }

        @Override
        public List<E> subList(int i, int i2) {
            return new UnmodifiableRandomAccessList(this.list.subList(i, i2));
        }

        private Object writeReplace() {
            return new UnmodifiableList(this.list);
        }
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> map) {
        return new UnmodifiableMap(map);
    }

    private static class UnmodifiableMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = -1034234728574286014L;
        private transient Set<Map.Entry<K, V>> entrySet;
        private transient Set<K> keySet;
        private final Map<? extends K, ? extends V> m;
        private transient Collection<V> values;

        UnmodifiableMap(Map<? extends K, ? extends V> map) {
            if (map == null) {
                throw new NullPointerException();
            }
            this.m = map;
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.m.containsKey(obj);
        }

        @Override
        public boolean containsValue(Object obj) {
            return this.m.containsValue(obj);
        }

        @Override
        public V get(Object obj) {
            return this.m.get(obj);
        }

        @Override
        public V put(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            if (this.keySet == null) {
                this.keySet = Collections.unmodifiableSet(this.m.keySet());
            }
            return this.keySet;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            if (this.entrySet == null) {
                this.entrySet = new UnmodifiableEntrySet(this.m.entrySet());
            }
            return this.entrySet;
        }

        @Override
        public Collection<V> values() {
            if (this.values == null) {
                this.values = Collections.unmodifiableCollection(this.m.values());
            }
            return this.values;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.m.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.m.hashCode();
        }

        public String toString() {
            return this.m.toString();
        }

        @Override
        public V getOrDefault(Object obj, V v) {
            return this.m.getOrDefault(obj, v);
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
            this.m.forEach(biConsumer);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V putIfAbsent(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K k, V v, V v2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        static class UnmodifiableEntrySet<K, V> extends UnmodifiableSet<Map.Entry<K, V>> {
            private static final long serialVersionUID = 7854390611657943733L;

            UnmodifiableEntrySet(Set<? extends Map.Entry<? extends K, ? extends V>> set) {
                super(set);
            }

            static <K, V> Consumer<Map.Entry<K, V>> entryConsumer(final Consumer<? super Map.Entry<K, V>> consumer) {
                return new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        consumer.accept(new Collections.UnmodifiableMap.UnmodifiableEntrySet.UnmodifiableEntry((Map.Entry) obj));
                    }
                };
            }

            @Override
            public void forEach(Consumer<? super Map.Entry<K, V>> consumer) {
                Objects.requireNonNull(consumer);
                this.c.forEach((Consumer<? super T>) entryConsumer(consumer));
            }

            static final class UnmodifiableEntrySetSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
                final Spliterator<Map.Entry<K, V>> s;

                UnmodifiableEntrySetSpliterator(Spliterator<Map.Entry<K, V>> spliterator) {
                    this.s = spliterator;
                }

                @Override
                public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> consumer) {
                    Objects.requireNonNull(consumer);
                    return this.s.tryAdvance(UnmodifiableEntrySet.entryConsumer(consumer));
                }

                @Override
                public void forEachRemaining(Consumer<? super Map.Entry<K, V>> consumer) {
                    Objects.requireNonNull(consumer);
                    this.s.forEachRemaining(UnmodifiableEntrySet.entryConsumer(consumer));
                }

                @Override
                public Spliterator<Map.Entry<K, V>> trySplit() {
                    Spliterator<Map.Entry<K, V>> spliteratorTrySplit = this.s.trySplit();
                    if (spliteratorTrySplit == null) {
                        return null;
                    }
                    return new UnmodifiableEntrySetSpliterator(spliteratorTrySplit);
                }

                @Override
                public long estimateSize() {
                    return this.s.estimateSize();
                }

                @Override
                public long getExactSizeIfKnown() {
                    return this.s.getExactSizeIfKnown();
                }

                @Override
                public int characteristics() {
                    return this.s.characteristics();
                }

                @Override
                public boolean hasCharacteristics(int i) {
                    return this.s.hasCharacteristics(i);
                }

                @Override
                public Comparator<? super Map.Entry<K, V>> getComparator() {
                    return this.s.getComparator();
                }
            }

            @Override
            public Spliterator<Map.Entry<K, V>> spliterator() {
                return new UnmodifiableEntrySetSpliterator(this.c.spliterator());
            }

            @Override
            public Stream<Map.Entry<K, V>> stream() {
                return StreamSupport.stream(spliterator(), false);
            }

            @Override
            public Stream<Map.Entry<K, V>> parallelStream() {
                return StreamSupport.stream(spliterator(), true);
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    private final Iterator<? extends Map.Entry<? extends K, ? extends V>> i;

                    {
                        this.i = UnmodifiableEntrySet.this.c.iterator();
                    }

                    @Override
                    public boolean hasNext() {
                        return this.i.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        return new UnmodifiableEntry(this.i.next());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public Object[] toArray() {
                Object[] array = this.c.toArray();
                for (int i = 0; i < array.length; i++) {
                    array[i] = new UnmodifiableEntry((Map.Entry) array[i]);
                }
                return array;
            }

            @Override
            public <T> T[] toArray(T[] tArr) {
                T[] tArr2 = (T[]) this.c.toArray(tArr.length == 0 ? tArr : Arrays.copyOf(tArr, 0));
                for (int i = 0; i < tArr2.length; i++) {
                    tArr2[i] = new UnmodifiableEntry((Map.Entry) tArr2[i]);
                }
                if (tArr2.length > tArr.length) {
                    return tArr2;
                }
                System.arraycopy(tArr2, 0, tArr, 0, tArr2.length);
                if (tArr.length > tArr2.length) {
                    tArr[tArr2.length] = null;
                }
                return tArr;
            }

            @Override
            public boolean contains(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                return this.c.contains(new UnmodifiableEntry((Map.Entry) obj));
            }

            @Override
            public boolean containsAll(Collection<?> collection) {
                Iterator<?> it = collection.iterator();
                while (it.hasNext()) {
                    if (!contains(it.next())) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Set)) {
                    return false;
                }
                Set set = (Set) obj;
                if (set.size() != this.c.size()) {
                    return false;
                }
                return containsAll(set);
            }

            private static class UnmodifiableEntry<K, V> implements Map.Entry<K, V> {
                private Map.Entry<? extends K, ? extends V> e;

                UnmodifiableEntry(Map.Entry<? extends K, ? extends V> entry) {
                    this.e = (Map.Entry) Objects.requireNonNull(entry);
                }

                @Override
                public K getKey() {
                    return this.e.getKey();
                }

                @Override
                public V getValue() {
                    return this.e.getValue();
                }

                @Override
                public V setValue(V v) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int hashCode() {
                    return this.e.hashCode();
                }

                @Override
                public boolean equals(Object obj) {
                    if (this == obj) {
                        return true;
                    }
                    if (!(obj instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry) obj;
                    return Collections.eq(this.e.getKey(), entry.getKey()) && Collections.eq(this.e.getValue(), entry.getValue());
                }

                public String toString() {
                    return this.e.toString();
                }
            }
        }
    }

    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> sortedMap) {
        return new UnmodifiableSortedMap(sortedMap);
    }

    static class UnmodifiableSortedMap<K, V> extends UnmodifiableMap<K, V> implements SortedMap<K, V>, Serializable {
        private static final long serialVersionUID = -8806743815996713206L;
        private final SortedMap<K, ? extends V> sm;

        UnmodifiableSortedMap(SortedMap<K, ? extends V> sortedMap) {
            super(sortedMap);
            this.sm = sortedMap;
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.sm.comparator();
        }

        @Override
        public SortedMap<K, V> subMap(K k, K k2) {
            return new UnmodifiableSortedMap(this.sm.subMap(k, k2));
        }

        @Override
        public SortedMap<K, V> headMap(K k) {
            return new UnmodifiableSortedMap(this.sm.headMap(k));
        }

        @Override
        public SortedMap<K, V> tailMap(K k) {
            return new UnmodifiableSortedMap(this.sm.tailMap(k));
        }

        @Override
        public K firstKey() {
            return this.sm.firstKey();
        }

        @Override
        public K lastKey() {
            return this.sm.lastKey();
        }
    }

    public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, ? extends V> navigableMap) {
        return new UnmodifiableNavigableMap(navigableMap);
    }

    static class UnmodifiableNavigableMap<K, V> extends UnmodifiableSortedMap<K, V> implements NavigableMap<K, V>, Serializable {
        private static final EmptyNavigableMap<?, ?> EMPTY_NAVIGABLE_MAP = new EmptyNavigableMap<>();
        private static final long serialVersionUID = -4858195264774772197L;
        private final NavigableMap<K, ? extends V> nm;

        private static class EmptyNavigableMap<K, V> extends UnmodifiableNavigableMap<K, V> implements Serializable {
            private static final long serialVersionUID = -2239321462712562324L;

            EmptyNavigableMap() {
                super(new TreeMap());
            }

            @Override
            public NavigableSet<K> navigableKeySet() {
                return Collections.emptyNavigableSet();
            }

            private Object readResolve() {
                return UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
            }
        }

        UnmodifiableNavigableMap(NavigableMap<K, ? extends V> navigableMap) {
            super(navigableMap);
            this.nm = navigableMap;
        }

        @Override
        public K lowerKey(K k) {
            return this.nm.lowerKey(k);
        }

        @Override
        public K floorKey(K k) {
            return this.nm.floorKey(k);
        }

        @Override
        public K ceilingKey(K k) {
            return this.nm.ceilingKey(k);
        }

        @Override
        public K higherKey(K k) {
            return this.nm.higherKey(k);
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K k) {
            Map.Entry<K, ? extends V> entryLowerEntry = this.nm.lowerEntry(k);
            if (entryLowerEntry != null) {
                return new UnmodifiableMap.UnmodifiableEntrySet.UnmodifiableEntry(entryLowerEntry);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> floorEntry(K k) {
            Map.Entry<K, ? extends V> entryFloorEntry = this.nm.floorEntry(k);
            if (entryFloorEntry != null) {
                return new UnmodifiableMap.UnmodifiableEntrySet.UnmodifiableEntry(entryFloorEntry);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K k) {
            Map.Entry<K, ? extends V> entryCeilingEntry = this.nm.ceilingEntry(k);
            if (entryCeilingEntry != null) {
                return new UnmodifiableMap.UnmodifiableEntrySet.UnmodifiableEntry(entryCeilingEntry);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> higherEntry(K k) {
            Map.Entry<K, ? extends V> entryHigherEntry = this.nm.higherEntry(k);
            if (entryHigherEntry != null) {
                return new UnmodifiableMap.UnmodifiableEntrySet.UnmodifiableEntry(entryHigherEntry);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> firstEntry() {
            Map.Entry<K, ? extends V> entryFirstEntry = this.nm.firstEntry();
            if (entryFirstEntry != null) {
                return new UnmodifiableMap.UnmodifiableEntrySet.UnmodifiableEntry(entryFirstEntry);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> lastEntry() {
            Map.Entry<K, ? extends V> entryLastEntry = this.nm.lastEntry();
            if (entryLastEntry != null) {
                return new UnmodifiableMap.UnmodifiableEntrySet.UnmodifiableEntry(entryLastEntry);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> pollFirstEntry() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map.Entry<K, V> pollLastEntry() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return Collections.unmodifiableNavigableMap(this.nm.descendingMap());
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return Collections.unmodifiableNavigableSet(this.nm.navigableKeySet());
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return Collections.unmodifiableNavigableSet(this.nm.descendingKeySet());
        }

        @Override
        public NavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
            return Collections.unmodifiableNavigableMap(this.nm.subMap(k, z, k2, z2));
        }

        @Override
        public NavigableMap<K, V> headMap(K k, boolean z) {
            return Collections.unmodifiableNavigableMap(this.nm.headMap(k, z));
        }

        @Override
        public NavigableMap<K, V> tailMap(K k, boolean z) {
            return Collections.unmodifiableNavigableMap(this.nm.tailMap(k, z));
        }
    }

    public static <T> Collection<T> synchronizedCollection(Collection<T> collection) {
        return new SynchronizedCollection(collection);
    }

    static <T> Collection<T> synchronizedCollection(Collection<T> collection, Object obj) {
        return new SynchronizedCollection(collection, obj);
    }

    static class SynchronizedCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 3053995032091335093L;
        final Collection<E> c;
        final Object mutex;

        SynchronizedCollection(Collection<E> collection) {
            this.c = (Collection) Objects.requireNonNull(collection);
            this.mutex = this;
        }

        SynchronizedCollection(Collection<E> collection, Object obj) {
            this.c = (Collection) Objects.requireNonNull(collection);
            this.mutex = Objects.requireNonNull(obj);
        }

        @Override
        public int size() {
            int size;
            synchronized (this.mutex) {
                size = this.c.size();
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            boolean zIsEmpty;
            synchronized (this.mutex) {
                zIsEmpty = this.c.isEmpty();
            }
            return zIsEmpty;
        }

        @Override
        public boolean contains(Object obj) {
            boolean zContains;
            synchronized (this.mutex) {
                zContains = this.c.contains(obj);
            }
            return zContains;
        }

        @Override
        public Object[] toArray() {
            Object[] array;
            synchronized (this.mutex) {
                array = this.c.toArray();
            }
            return array;
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            T[] tArr2;
            synchronized (this.mutex) {
                tArr2 = (T[]) this.c.toArray(tArr);
            }
            return tArr2;
        }

        @Override
        public Iterator<E> iterator() {
            return this.c.iterator();
        }

        @Override
        public boolean add(E e) {
            boolean zAdd;
            synchronized (this.mutex) {
                zAdd = this.c.add(e);
            }
            return zAdd;
        }

        @Override
        public boolean remove(Object obj) {
            boolean zRemove;
            synchronized (this.mutex) {
                zRemove = this.c.remove(obj);
            }
            return zRemove;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            boolean zContainsAll;
            synchronized (this.mutex) {
                zContainsAll = this.c.containsAll(collection);
            }
            return zContainsAll;
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            boolean zAddAll;
            synchronized (this.mutex) {
                zAddAll = this.c.addAll(collection);
            }
            return zAddAll;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean zRemoveAll;
            synchronized (this.mutex) {
                zRemoveAll = this.c.removeAll(collection);
            }
            return zRemoveAll;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean zRetainAll;
            synchronized (this.mutex) {
                zRetainAll = this.c.retainAll(collection);
            }
            return zRetainAll;
        }

        @Override
        public void clear() {
            synchronized (this.mutex) {
                this.c.clear();
            }
        }

        public String toString() {
            String string;
            synchronized (this.mutex) {
                string = this.c.toString();
            }
            return string;
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            synchronized (this.mutex) {
                this.c.forEach(consumer);
            }
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            boolean zRemoveIf;
            synchronized (this.mutex) {
                zRemoveIf = this.c.removeIf(predicate);
            }
            return zRemoveIf;
        }

        @Override
        public Spliterator<E> spliterator() {
            return this.c.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return this.c.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return this.c.parallelStream();
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            synchronized (this.mutex) {
                objectOutputStream.defaultWriteObject();
            }
        }
    }

    public static <T> Set<T> synchronizedSet(Set<T> set) {
        return new SynchronizedSet(set);
    }

    static <T> Set<T> synchronizedSet(Set<T> set, Object obj) {
        return new SynchronizedSet(set, obj);
    }

    static class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E> {
        private static final long serialVersionUID = 487447009682186044L;

        SynchronizedSet(Set<E> set) {
            super(set);
        }

        SynchronizedSet(Set<E> set, Object obj) {
            super(set, obj);
        }

        @Override
        public boolean equals(Object obj) {
            boolean zEquals;
            if (this == obj) {
                return true;
            }
            synchronized (this.mutex) {
                zEquals = this.c.equals(obj);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = this.c.hashCode();
            }
            return iHashCode;
        }
    }

    public static <T> SortedSet<T> synchronizedSortedSet(SortedSet<T> sortedSet) {
        return new SynchronizedSortedSet(sortedSet);
    }

    static class SynchronizedSortedSet<E> extends SynchronizedSet<E> implements SortedSet<E> {
        private static final long serialVersionUID = 8695801310862127406L;
        private final SortedSet<E> ss;

        SynchronizedSortedSet(SortedSet<E> sortedSet) {
            super(sortedSet);
            this.ss = sortedSet;
        }

        SynchronizedSortedSet(SortedSet<E> sortedSet, Object obj) {
            super(sortedSet, obj);
            this.ss = sortedSet;
        }

        @Override
        public Comparator<? super E> comparator() {
            Comparator<? super E> comparator;
            synchronized (this.mutex) {
                comparator = this.ss.comparator();
            }
            return comparator;
        }

        public SortedSet<E> subSet(E e, E e2) {
            SynchronizedSortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.subSet(e, e2), this.mutex);
            }
            return synchronizedSortedSet;
        }

        public SortedSet<E> headSet(E e) {
            SynchronizedSortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.headSet(e), this.mutex);
            }
            return synchronizedSortedSet;
        }

        public SortedSet<E> tailSet(E e) {
            SynchronizedSortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.tailSet(e), this.mutex);
            }
            return synchronizedSortedSet;
        }

        @Override
        public E first() {
            E eFirst;
            synchronized (this.mutex) {
                eFirst = this.ss.first();
            }
            return eFirst;
        }

        @Override
        public E last() {
            E eLast;
            synchronized (this.mutex) {
                eLast = this.ss.last();
            }
            return eLast;
        }
    }

    public static <T> NavigableSet<T> synchronizedNavigableSet(NavigableSet<T> navigableSet) {
        return new SynchronizedNavigableSet(navigableSet);
    }

    static class SynchronizedNavigableSet<E> extends SynchronizedSortedSet<E> implements NavigableSet<E> {
        private static final long serialVersionUID = -5505529816273629798L;
        private final NavigableSet<E> ns;

        SynchronizedNavigableSet(NavigableSet<E> navigableSet) {
            super(navigableSet);
            this.ns = navigableSet;
        }

        SynchronizedNavigableSet(NavigableSet<E> navigableSet, Object obj) {
            super(navigableSet, obj);
            this.ns = navigableSet;
        }

        @Override
        public E lower(E e) {
            E eLower;
            synchronized (this.mutex) {
                eLower = this.ns.lower(e);
            }
            return eLower;
        }

        @Override
        public E floor(E e) {
            E eFloor;
            synchronized (this.mutex) {
                eFloor = this.ns.floor(e);
            }
            return eFloor;
        }

        @Override
        public E ceiling(E e) {
            E eCeiling;
            synchronized (this.mutex) {
                eCeiling = this.ns.ceiling(e);
            }
            return eCeiling;
        }

        @Override
        public E higher(E e) {
            E eHigher;
            synchronized (this.mutex) {
                eHigher = this.ns.higher(e);
            }
            return eHigher;
        }

        @Override
        public E pollFirst() {
            E ePollFirst;
            synchronized (this.mutex) {
                ePollFirst = this.ns.pollFirst();
            }
            return ePollFirst;
        }

        @Override
        public E pollLast() {
            E ePollLast;
            synchronized (this.mutex) {
                ePollLast = this.ns.pollLast();
            }
            return ePollLast;
        }

        @Override
        public NavigableSet<E> descendingSet() {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.descendingSet(), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public Iterator<E> descendingIterator() {
            Iterator<E> it;
            synchronized (this.mutex) {
                it = descendingSet().iterator();
            }
            return it;
        }

        @Override
        public NavigableSet<E> subSet(E e, E e2) {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.subSet(e, true, e2, false), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public NavigableSet<E> headSet(E e) {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.headSet(e, false), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public NavigableSet<E> tailSet(E e) {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.tailSet(e, true), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.subSet(e, z, e2, z2), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public NavigableSet<E> headSet(E e, boolean z) {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.headSet(e, z), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public NavigableSet<E> tailSet(E e, boolean z) {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.tailSet(e, z), this.mutex);
            }
            return synchronizedNavigableSet;
        }
    }

    public static <T> List<T> synchronizedList(List<T> list) {
        if (list instanceof RandomAccess) {
            return new SynchronizedRandomAccessList(list);
        }
        return new SynchronizedList(list);
    }

    static <T> List<T> synchronizedList(List<T> list, Object obj) {
        if (list instanceof RandomAccess) {
            return new SynchronizedRandomAccessList(list, obj);
        }
        return new SynchronizedList(list, obj);
    }

    static class SynchronizedList<E> extends SynchronizedCollection<E> implements List<E> {
        private static final long serialVersionUID = -7754090372962971524L;
        final List<E> list;

        SynchronizedList(List<E> list) {
            super(list);
            this.list = list;
        }

        SynchronizedList(List<E> list, Object obj) {
            super(list, obj);
            this.list = list;
        }

        @Override
        public boolean equals(Object obj) {
            boolean zEquals;
            if (this == obj) {
                return true;
            }
            synchronized (this.mutex) {
                zEquals = this.list.equals(obj);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = this.list.hashCode();
            }
            return iHashCode;
        }

        @Override
        public E get(int i) {
            E e;
            synchronized (this.mutex) {
                e = this.list.get(i);
            }
            return e;
        }

        @Override
        public E set(int i, E e) {
            E e2;
            synchronized (this.mutex) {
                e2 = this.list.set(i, e);
            }
            return e2;
        }

        @Override
        public void add(int i, E e) {
            synchronized (this.mutex) {
                this.list.add(i, e);
            }
        }

        @Override
        public E remove(int i) {
            E eRemove;
            synchronized (this.mutex) {
                eRemove = this.list.remove(i);
            }
            return eRemove;
        }

        @Override
        public int indexOf(Object obj) {
            int iIndexOf;
            synchronized (this.mutex) {
                iIndexOf = this.list.indexOf(obj);
            }
            return iIndexOf;
        }

        @Override
        public int lastIndexOf(Object obj) {
            int iLastIndexOf;
            synchronized (this.mutex) {
                iLastIndexOf = this.list.lastIndexOf(obj);
            }
            return iLastIndexOf;
        }

        @Override
        public boolean addAll(int i, Collection<? extends E> collection) {
            boolean zAddAll;
            synchronized (this.mutex) {
                zAddAll = this.list.addAll(i, collection);
            }
            return zAddAll;
        }

        @Override
        public ListIterator<E> listIterator() {
            return this.list.listIterator();
        }

        @Override
        public ListIterator<E> listIterator(int i) {
            return this.list.listIterator(i);
        }

        @Override
        public List<E> subList(int i, int i2) {
            SynchronizedList synchronizedList;
            synchronized (this.mutex) {
                synchronizedList = new SynchronizedList(this.list.subList(i, i2), this.mutex);
            }
            return synchronizedList;
        }

        @Override
        public void replaceAll(UnaryOperator<E> unaryOperator) {
            synchronized (this.mutex) {
                this.list.replaceAll(unaryOperator);
            }
        }

        @Override
        public void sort(Comparator<? super E> comparator) {
            synchronized (this.mutex) {
                this.list.sort(comparator);
            }
        }

        private Object readResolve() {
            if (!(this.list instanceof RandomAccess)) {
                return this;
            }
            return new SynchronizedRandomAccessList(this.list);
        }
    }

    static class SynchronizedRandomAccessList<E> extends SynchronizedList<E> implements RandomAccess {
        private static final long serialVersionUID = 1530674583602358482L;

        SynchronizedRandomAccessList(List<E> list) {
            super(list);
        }

        SynchronizedRandomAccessList(List<E> list, Object obj) {
            super(list, obj);
        }

        @Override
        public List<E> subList(int i, int i2) {
            SynchronizedRandomAccessList synchronizedRandomAccessList;
            synchronized (this.mutex) {
                synchronizedRandomAccessList = new SynchronizedRandomAccessList(this.list.subList(i, i2), this.mutex);
            }
            return synchronizedRandomAccessList;
        }

        private Object writeReplace() {
            return new SynchronizedList(this.list);
        }
    }

    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> map) {
        return new SynchronizedMap(map);
    }

    private static class SynchronizedMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = 1978198479659022715L;
        private transient Set<Map.Entry<K, V>> entrySet;
        private transient Set<K> keySet;
        private final Map<K, V> m;
        final Object mutex;
        private transient Collection<V> values;

        SynchronizedMap(Map<K, V> map) {
            this.m = (Map) Objects.requireNonNull(map);
            this.mutex = this;
        }

        SynchronizedMap(Map<K, V> map, Object obj) {
            this.m = map;
            this.mutex = obj;
        }

        @Override
        public int size() {
            int size;
            synchronized (this.mutex) {
                size = this.m.size();
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            boolean zIsEmpty;
            synchronized (this.mutex) {
                zIsEmpty = this.m.isEmpty();
            }
            return zIsEmpty;
        }

        @Override
        public boolean containsKey(Object obj) {
            boolean zContainsKey;
            synchronized (this.mutex) {
                zContainsKey = this.m.containsKey(obj);
            }
            return zContainsKey;
        }

        @Override
        public boolean containsValue(Object obj) {
            boolean zContainsValue;
            synchronized (this.mutex) {
                zContainsValue = this.m.containsValue(obj);
            }
            return zContainsValue;
        }

        @Override
        public V get(Object obj) {
            V v;
            synchronized (this.mutex) {
                v = this.m.get(obj);
            }
            return v;
        }

        @Override
        public V put(K k, V v) {
            V vPut;
            synchronized (this.mutex) {
                vPut = this.m.put(k, v);
            }
            return vPut;
        }

        @Override
        public V remove(Object obj) {
            V vRemove;
            synchronized (this.mutex) {
                vRemove = this.m.remove(obj);
            }
            return vRemove;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            synchronized (this.mutex) {
                this.m.putAll(map);
            }
        }

        @Override
        public void clear() {
            synchronized (this.mutex) {
                this.m.clear();
            }
        }

        @Override
        public Set<K> keySet() {
            Set<K> set;
            synchronized (this.mutex) {
                if (this.keySet == null) {
                    this.keySet = new SynchronizedSet(this.m.keySet(), this.mutex);
                }
                set = this.keySet;
            }
            return set;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K, V>> set;
            synchronized (this.mutex) {
                if (this.entrySet == null) {
                    this.entrySet = new SynchronizedSet(this.m.entrySet(), this.mutex);
                }
                set = this.entrySet;
            }
            return set;
        }

        @Override
        public Collection<V> values() {
            Collection<V> collection;
            synchronized (this.mutex) {
                if (this.values == null) {
                    this.values = new SynchronizedCollection(this.m.values(), this.mutex);
                }
                collection = this.values;
            }
            return collection;
        }

        @Override
        public boolean equals(Object obj) {
            boolean zEquals;
            if (this == obj) {
                return true;
            }
            synchronized (this.mutex) {
                zEquals = this.m.equals(obj);
            }
            return zEquals;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            synchronized (this.mutex) {
                iHashCode = this.m.hashCode();
            }
            return iHashCode;
        }

        public String toString() {
            String string;
            synchronized (this.mutex) {
                string = this.m.toString();
            }
            return string;
        }

        @Override
        public V getOrDefault(Object obj, V v) {
            V orDefault;
            synchronized (this.mutex) {
                orDefault = this.m.getOrDefault(obj, v);
            }
            return orDefault;
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
            synchronized (this.mutex) {
                this.m.forEach(biConsumer);
            }
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
            synchronized (this.mutex) {
                this.m.replaceAll(biFunction);
            }
        }

        @Override
        public V putIfAbsent(K k, V v) {
            V vPutIfAbsent;
            synchronized (this.mutex) {
                vPutIfAbsent = this.m.putIfAbsent(k, v);
            }
            return vPutIfAbsent;
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            boolean zRemove;
            synchronized (this.mutex) {
                zRemove = this.m.remove(obj, obj2);
            }
            return zRemove;
        }

        @Override
        public boolean replace(K k, V v, V v2) {
            boolean zReplace;
            synchronized (this.mutex) {
                zReplace = this.m.replace(k, v, v2);
            }
            return zReplace;
        }

        @Override
        public V replace(K k, V v) {
            V vReplace;
            synchronized (this.mutex) {
                vReplace = this.m.replace(k, v);
            }
            return vReplace;
        }

        @Override
        public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
            V vComputeIfAbsent;
            synchronized (this.mutex) {
                vComputeIfAbsent = this.m.computeIfAbsent(k, function);
            }
            return vComputeIfAbsent;
        }

        @Override
        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            V vComputeIfPresent;
            synchronized (this.mutex) {
                vComputeIfPresent = this.m.computeIfPresent(k, biFunction);
            }
            return vComputeIfPresent;
        }

        @Override
        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            V vCompute;
            synchronized (this.mutex) {
                vCompute = this.m.compute(k, biFunction);
            }
            return vCompute;
        }

        @Override
        public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            V vMerge;
            synchronized (this.mutex) {
                vMerge = this.m.merge(k, v, biFunction);
            }
            return vMerge;
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            synchronized (this.mutex) {
                objectOutputStream.defaultWriteObject();
            }
        }
    }

    public static <K, V> SortedMap<K, V> synchronizedSortedMap(SortedMap<K, V> sortedMap) {
        return new SynchronizedSortedMap(sortedMap);
    }

    static class SynchronizedSortedMap<K, V> extends SynchronizedMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = -8798146769416483793L;
        private final SortedMap<K, V> sm;

        SynchronizedSortedMap(SortedMap<K, V> sortedMap) {
            super(sortedMap);
            this.sm = sortedMap;
        }

        SynchronizedSortedMap(SortedMap<K, V> sortedMap, Object obj) {
            super(sortedMap, obj);
            this.sm = sortedMap;
        }

        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> comparator;
            synchronized (this.mutex) {
                comparator = this.sm.comparator();
            }
            return comparator;
        }

        public SortedMap<K, V> subMap(K k, K k2) {
            SynchronizedSortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.subMap(k, k2), this.mutex);
            }
            return synchronizedSortedMap;
        }

        public SortedMap<K, V> headMap(K k) {
            SynchronizedSortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.headMap(k), this.mutex);
            }
            return synchronizedSortedMap;
        }

        public SortedMap<K, V> tailMap(K k) {
            SynchronizedSortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.tailMap(k), this.mutex);
            }
            return synchronizedSortedMap;
        }

        @Override
        public K firstKey() {
            K kFirstKey;
            synchronized (this.mutex) {
                kFirstKey = this.sm.firstKey();
            }
            return kFirstKey;
        }

        @Override
        public K lastKey() {
            K kLastKey;
            synchronized (this.mutex) {
                kLastKey = this.sm.lastKey();
            }
            return kLastKey;
        }
    }

    public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(NavigableMap<K, V> navigableMap) {
        return new SynchronizedNavigableMap(navigableMap);
    }

    static class SynchronizedNavigableMap<K, V> extends SynchronizedSortedMap<K, V> implements NavigableMap<K, V> {
        private static final long serialVersionUID = 699392247599746807L;
        private final NavigableMap<K, V> nm;

        SynchronizedNavigableMap(NavigableMap<K, V> navigableMap) {
            super(navigableMap);
            this.nm = navigableMap;
        }

        SynchronizedNavigableMap(NavigableMap<K, V> navigableMap, Object obj) {
            super(navigableMap, obj);
            this.nm = navigableMap;
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K k) {
            Map.Entry<K, V> entryLowerEntry;
            synchronized (this.mutex) {
                entryLowerEntry = this.nm.lowerEntry(k);
            }
            return entryLowerEntry;
        }

        @Override
        public K lowerKey(K k) {
            K kLowerKey;
            synchronized (this.mutex) {
                kLowerKey = this.nm.lowerKey(k);
            }
            return kLowerKey;
        }

        @Override
        public Map.Entry<K, V> floorEntry(K k) {
            Map.Entry<K, V> entryFloorEntry;
            synchronized (this.mutex) {
                entryFloorEntry = this.nm.floorEntry(k);
            }
            return entryFloorEntry;
        }

        @Override
        public K floorKey(K k) {
            K kFloorKey;
            synchronized (this.mutex) {
                kFloorKey = this.nm.floorKey(k);
            }
            return kFloorKey;
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K k) {
            Map.Entry<K, V> entryCeilingEntry;
            synchronized (this.mutex) {
                entryCeilingEntry = this.nm.ceilingEntry(k);
            }
            return entryCeilingEntry;
        }

        @Override
        public K ceilingKey(K k) {
            K kCeilingKey;
            synchronized (this.mutex) {
                kCeilingKey = this.nm.ceilingKey(k);
            }
            return kCeilingKey;
        }

        @Override
        public Map.Entry<K, V> higherEntry(K k) {
            Map.Entry<K, V> entryHigherEntry;
            synchronized (this.mutex) {
                entryHigherEntry = this.nm.higherEntry(k);
            }
            return entryHigherEntry;
        }

        @Override
        public K higherKey(K k) {
            K kHigherKey;
            synchronized (this.mutex) {
                kHigherKey = this.nm.higherKey(k);
            }
            return kHigherKey;
        }

        @Override
        public Map.Entry<K, V> firstEntry() {
            Map.Entry<K, V> entryFirstEntry;
            synchronized (this.mutex) {
                entryFirstEntry = this.nm.firstEntry();
            }
            return entryFirstEntry;
        }

        @Override
        public Map.Entry<K, V> lastEntry() {
            Map.Entry<K, V> entryLastEntry;
            synchronized (this.mutex) {
                entryLastEntry = this.nm.lastEntry();
            }
            return entryLastEntry;
        }

        @Override
        public Map.Entry<K, V> pollFirstEntry() {
            Map.Entry<K, V> entryPollFirstEntry;
            synchronized (this.mutex) {
                entryPollFirstEntry = this.nm.pollFirstEntry();
            }
            return entryPollFirstEntry;
        }

        @Override
        public Map.Entry<K, V> pollLastEntry() {
            Map.Entry<K, V> entryPollLastEntry;
            synchronized (this.mutex) {
                entryPollLastEntry = this.nm.pollLastEntry();
            }
            return entryPollLastEntry;
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            SynchronizedNavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.descendingMap(), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        @Override
        public NavigableSet<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.nm.navigableKeySet(), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            SynchronizedNavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.nm.descendingKeySet(), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        @Override
        public SortedMap<K, V> subMap(K k, K k2) {
            SynchronizedNavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.subMap(k, true, k2, false), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        @Override
        public SortedMap<K, V> headMap(K k) {
            SynchronizedNavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.headMap(k, false), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        @Override
        public SortedMap<K, V> tailMap(K k) {
            SynchronizedNavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.tailMap(k, true), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        @Override
        public NavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
            SynchronizedNavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.subMap(k, z, k2, z2), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        @Override
        public NavigableMap<K, V> headMap(K k, boolean z) {
            SynchronizedNavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.headMap(k, z), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        @Override
        public NavigableMap<K, V> tailMap(K k, boolean z) {
            SynchronizedNavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.tailMap(k, z), this.mutex);
            }
            return synchronizedNavigableMap;
        }
    }

    public static <E> Collection<E> checkedCollection(Collection<E> collection, Class<E> cls) {
        return new CheckedCollection(collection, cls);
    }

    static <T> T[] zeroLengthArray(Class<T> cls) {
        return (T[]) ((Object[]) Array.newInstance((Class<?>) cls, 0));
    }

    static class CheckedCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 1578914078182001775L;
        final Collection<E> c;
        final Class<E> type;
        private E[] zeroLengthElementArray;

        E typeCheck(Object obj) {
            if (obj != 0 && !this.type.isInstance(obj)) {
                throw new ClassCastException(badElementMsg(obj));
            }
            return obj;
        }

        private String badElementMsg(Object obj) {
            return "Attempt to insert " + ((Object) obj.getClass()) + " element into collection with element type " + ((Object) this.type);
        }

        CheckedCollection(Collection<E> collection, Class<E> cls) {
            this.c = (Collection) Objects.requireNonNull(collection, "c");
            this.type = (Class) Objects.requireNonNull(cls, "type");
        }

        @Override
        public int size() {
            return this.c.size();
        }

        @Override
        public boolean isEmpty() {
            return this.c.isEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            return this.c.contains(obj);
        }

        @Override
        public Object[] toArray() {
            return this.c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.c.toArray(tArr);
        }

        public String toString() {
            return this.c.toString();
        }

        @Override
        public boolean remove(Object obj) {
            return this.c.remove(obj);
        }

        @Override
        public void clear() {
            this.c.clear();
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.c.containsAll(collection);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return this.c.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return this.c.retainAll(collection);
        }

        @Override
        public Iterator<E> iterator() {
            final Iterator<E> it = this.c.iterator();
            return new Iterator<E>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    return (E) it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public boolean add(E e) {
            return this.c.add(typeCheck(e));
        }

        private E[] zeroLengthElementArray() {
            if (this.zeroLengthElementArray != null) {
                return this.zeroLengthElementArray;
            }
            E[] eArr = (E[]) Collections.zeroLengthArray(this.type);
            this.zeroLengthElementArray = eArr;
            return eArr;
        }

        Collection<E> checkedCopyOf(Collection<? extends E> collection) {
            Object[] array;
            try {
                E[] eArrZeroLengthElementArray = zeroLengthElementArray();
                array = collection.toArray(eArrZeroLengthElementArray);
                if (array.getClass() != eArrZeroLengthElementArray.getClass()) {
                    array = Arrays.copyOf(array, array.length, eArrZeroLengthElementArray.getClass());
                }
            } catch (ArrayStoreException e) {
                array = (Object[]) collection.toArray().clone();
                for (Object obj : array) {
                    typeCheck(obj);
                }
            }
            return Arrays.asList(array);
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            return this.c.addAll(checkedCopyOf(collection));
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            this.c.forEach(consumer);
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            return this.c.removeIf(predicate);
        }

        @Override
        public Spliterator<E> spliterator() {
            return this.c.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return this.c.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return this.c.parallelStream();
        }
    }

    public static <E> Queue<E> checkedQueue(Queue<E> queue, Class<E> cls) {
        return new CheckedQueue(queue, cls);
    }

    static class CheckedQueue<E> extends CheckedCollection<E> implements Queue<E>, Serializable {
        private static final long serialVersionUID = 1433151992604707767L;
        final Queue<E> queue;

        CheckedQueue(Queue<E> queue, Class<E> cls) {
            super(queue, cls);
            this.queue = queue;
        }

        @Override
        public E element() {
            return this.queue.element();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.c.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.c.hashCode();
        }

        @Override
        public E peek() {
            return this.queue.peek();
        }

        @Override
        public E poll() {
            return this.queue.poll();
        }

        @Override
        public E remove() {
            return this.queue.remove();
        }

        @Override
        public boolean offer(E e) {
            return this.queue.offer(typeCheck(e));
        }
    }

    public static <E> Set<E> checkedSet(Set<E> set, Class<E> cls) {
        return new CheckedSet(set, cls);
    }

    static class CheckedSet<E> extends CheckedCollection<E> implements Set<E>, Serializable {
        private static final long serialVersionUID = 4694047833775013803L;

        CheckedSet(Set<E> set, Class<E> cls) {
            super(set, cls);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.c.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.c.hashCode();
        }
    }

    public static <E> SortedSet<E> checkedSortedSet(SortedSet<E> sortedSet, Class<E> cls) {
        return new CheckedSortedSet(sortedSet, cls);
    }

    static class CheckedSortedSet<E> extends CheckedSet<E> implements SortedSet<E>, Serializable {
        private static final long serialVersionUID = 1599911165492914959L;
        private final SortedSet<E> ss;

        CheckedSortedSet(SortedSet<E> sortedSet, Class<E> cls) {
            super(sortedSet, cls);
            this.ss = sortedSet;
        }

        @Override
        public Comparator<? super E> comparator() {
            return this.ss.comparator();
        }

        @Override
        public E first() {
            return this.ss.first();
        }

        @Override
        public E last() {
            return this.ss.last();
        }

        public SortedSet<E> subSet(E e, E e2) {
            return Collections.checkedSortedSet(this.ss.subSet(e, e2), this.type);
        }

        public SortedSet<E> headSet(E e) {
            return Collections.checkedSortedSet(this.ss.headSet(e), this.type);
        }

        public SortedSet<E> tailSet(E e) {
            return Collections.checkedSortedSet(this.ss.tailSet(e), this.type);
        }
    }

    public static <E> NavigableSet<E> checkedNavigableSet(NavigableSet<E> navigableSet, Class<E> cls) {
        return new CheckedNavigableSet(navigableSet, cls);
    }

    static class CheckedNavigableSet<E> extends CheckedSortedSet<E> implements NavigableSet<E>, Serializable {
        private static final long serialVersionUID = -5429120189805438922L;
        private final NavigableSet<E> ns;

        CheckedNavigableSet(NavigableSet<E> navigableSet, Class<E> cls) {
            super(navigableSet, cls);
            this.ns = navigableSet;
        }

        @Override
        public E lower(E e) {
            return this.ns.lower(e);
        }

        @Override
        public E floor(E e) {
            return this.ns.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return this.ns.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return this.ns.higher(e);
        }

        @Override
        public E pollFirst() {
            return this.ns.pollFirst();
        }

        @Override
        public E pollLast() {
            return this.ns.pollLast();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return Collections.checkedNavigableSet(this.ns.descendingSet(), this.type);
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Collections.checkedNavigableSet(this.ns.descendingSet(), this.type).iterator();
        }

        @Override
        public NavigableSet<E> subSet(E e, E e2) {
            return Collections.checkedNavigableSet(this.ns.subSet(e, true, e2, false), this.type);
        }

        @Override
        public NavigableSet<E> headSet(E e) {
            return Collections.checkedNavigableSet(this.ns.headSet(e, false), this.type);
        }

        @Override
        public NavigableSet<E> tailSet(E e) {
            return Collections.checkedNavigableSet(this.ns.tailSet(e, true), this.type);
        }

        @Override
        public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
            return Collections.checkedNavigableSet(this.ns.subSet(e, z, e2, z2), this.type);
        }

        @Override
        public NavigableSet<E> headSet(E e, boolean z) {
            return Collections.checkedNavigableSet(this.ns.headSet(e, z), this.type);
        }

        @Override
        public NavigableSet<E> tailSet(E e, boolean z) {
            return Collections.checkedNavigableSet(this.ns.tailSet(e, z), this.type);
        }
    }

    public static <E> List<E> checkedList(List<E> list, Class<E> cls) {
        if (list instanceof RandomAccess) {
            return new CheckedRandomAccessList(list, cls);
        }
        return new CheckedList(list, cls);
    }

    static class CheckedList<E> extends CheckedCollection<E> implements List<E> {
        private static final long serialVersionUID = 65247728283967356L;
        final List<E> list;

        CheckedList(List<E> list, Class<E> cls) {
            super(list, cls);
            this.list = list;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.list.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.list.hashCode();
        }

        @Override
        public E get(int i) {
            return this.list.get(i);
        }

        @Override
        public E remove(int i) {
            return this.list.remove(i);
        }

        @Override
        public int indexOf(Object obj) {
            return this.list.indexOf(obj);
        }

        @Override
        public int lastIndexOf(Object obj) {
            return this.list.lastIndexOf(obj);
        }

        @Override
        public E set(int i, E e) {
            return this.list.set(i, typeCheck(e));
        }

        @Override
        public void add(int i, E e) {
            this.list.add(i, typeCheck(e));
        }

        @Override
        public boolean addAll(int i, Collection<? extends E> collection) {
            return this.list.addAll(i, checkedCopyOf(collection));
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(int i) {
            final ListIterator<E> listIterator = this.list.listIterator(i);
            return new ListIterator<E>() {
                @Override
                public boolean hasNext() {
                    return listIterator.hasNext();
                }

                @Override
                public E next() {
                    return (E) listIterator.next();
                }

                @Override
                public boolean hasPrevious() {
                    return listIterator.hasPrevious();
                }

                @Override
                public E previous() {
                    return (E) listIterator.previous();
                }

                @Override
                public int nextIndex() {
                    return listIterator.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return listIterator.previousIndex();
                }

                @Override
                public void remove() {
                    listIterator.remove();
                }

                @Override
                public void set(E e) {
                    listIterator.set(CheckedList.this.typeCheck(e));
                }

                @Override
                public void add(E e) {
                    listIterator.add(CheckedList.this.typeCheck(e));
                }

                @Override
                public void forEachRemaining(Consumer<? super E> consumer) {
                    listIterator.forEachRemaining(consumer);
                }
            };
        }

        @Override
        public List<E> subList(int i, int i2) {
            return new CheckedList(this.list.subList(i, i2), this.type);
        }

        @Override
        public void replaceAll(final UnaryOperator<E> unaryOperator) {
            Objects.requireNonNull(unaryOperator);
            this.list.replaceAll(new UnaryOperator() {
                @Override
                public final Object apply(Object obj) {
                    return this.f$0.typeCheck(unaryOperator.apply(obj));
                }
            });
        }

        @Override
        public void sort(Comparator<? super E> comparator) {
            this.list.sort(comparator);
        }
    }

    static class CheckedRandomAccessList<E> extends CheckedList<E> implements RandomAccess {
        private static final long serialVersionUID = 1638200125423088369L;

        CheckedRandomAccessList(List<E> list, Class<E> cls) {
            super(list, cls);
        }

        @Override
        public List<E> subList(int i, int i2) {
            return new CheckedRandomAccessList(this.list.subList(i, i2), this.type);
        }
    }

    public static <K, V> Map<K, V> checkedMap(Map<K, V> map, Class<K> cls, Class<V> cls2) {
        return new CheckedMap(map, cls, cls2);
    }

    private static class CheckedMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = 5742860141034234728L;
        private transient Set<Map.Entry<K, V>> entrySet;
        final Class<K> keyType;
        private final Map<K, V> m;
        final Class<V> valueType;

        private void typeCheck(Object obj, Object obj2) {
            if (obj != null && !this.keyType.isInstance(obj)) {
                throw new ClassCastException(badKeyMsg(obj));
            }
            if (obj2 != null && !this.valueType.isInstance(obj2)) {
                throw new ClassCastException(badValueMsg(obj2));
            }
        }

        private BiFunction<? super K, ? super V, ? extends V> typeCheck(final BiFunction<? super K, ? super V, ? extends V> biFunction) {
            Objects.requireNonNull(biFunction);
            return new BiFunction() {
                @Override
                public final Object apply(Object obj, Object obj2) {
                    return Collections.CheckedMap.lambda$typeCheck$0(this.f$0, biFunction, obj, obj2);
                }
            };
        }

        public static Object lambda$typeCheck$0(CheckedMap checkedMap, BiFunction biFunction, Object obj, Object obj2) {
            Object objApply = biFunction.apply(obj, obj2);
            checkedMap.typeCheck(obj, objApply);
            return objApply;
        }

        private String badKeyMsg(Object obj) {
            return "Attempt to insert " + ((Object) obj.getClass()) + " key into map with key type " + ((Object) this.keyType);
        }

        private String badValueMsg(Object obj) {
            return "Attempt to insert " + ((Object) obj.getClass()) + " value into map with value type " + ((Object) this.valueType);
        }

        CheckedMap(Map<K, V> map, Class<K> cls, Class<V> cls2) {
            this.m = (Map) Objects.requireNonNull(map);
            this.keyType = (Class) Objects.requireNonNull(cls);
            this.valueType = (Class) Objects.requireNonNull(cls2);
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.m.containsKey(obj);
        }

        @Override
        public boolean containsValue(Object obj) {
            return this.m.containsValue(obj);
        }

        @Override
        public V get(Object obj) {
            return this.m.get(obj);
        }

        @Override
        public V remove(Object obj) {
            return this.m.remove(obj);
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public Set<K> keySet() {
            return this.m.keySet();
        }

        @Override
        public Collection<V> values() {
            return this.m.values();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.m.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.m.hashCode();
        }

        public String toString() {
            return this.m.toString();
        }

        @Override
        public V put(K k, V v) {
            typeCheck(k, v);
            return this.m.put(k, v);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            Object[] array = map.entrySet().toArray();
            ArrayList<Map.Entry> arrayList = new ArrayList(array.length);
            for (Object obj : array) {
                Map.Entry entry = (Map.Entry) obj;
                Object key = entry.getKey();
                Object value = entry.getValue();
                typeCheck(key, value);
                arrayList.add(new AbstractMap.SimpleImmutableEntry(key, value));
            }
            for (Map.Entry entry2 : arrayList) {
                this.m.put((K) entry2.getKey(), (V) entry2.getValue());
            }
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            if (this.entrySet == null) {
                this.entrySet = new CheckedEntrySet(this.m.entrySet(), this.valueType);
            }
            return this.entrySet;
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
            this.m.forEach(biConsumer);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
            this.m.replaceAll(typeCheck(biFunction));
        }

        @Override
        public V putIfAbsent(K k, V v) {
            typeCheck(k, v);
            return this.m.putIfAbsent(k, v);
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            return this.m.remove(obj, obj2);
        }

        @Override
        public boolean replace(K k, V v, V v2) {
            typeCheck(k, v2);
            return this.m.replace(k, v, v2);
        }

        @Override
        public V replace(K k, V v) {
            typeCheck(k, v);
            return this.m.replace(k, v);
        }

        @Override
        public V computeIfAbsent(K k, final Function<? super K, ? extends V> function) {
            Objects.requireNonNull(function);
            return this.m.computeIfAbsent(k, new Function() {
                @Override
                public final Object apply(Object obj) {
                    return Collections.CheckedMap.lambda$computeIfAbsent$1(this.f$0, function, obj);
                }
            });
        }

        public static Object lambda$computeIfAbsent$1(CheckedMap checkedMap, Function function, Object obj) {
            Object objApply = function.apply(obj);
            checkedMap.typeCheck(obj, objApply);
            return objApply;
        }

        @Override
        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            return this.m.computeIfPresent(k, typeCheck(biFunction));
        }

        @Override
        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            return this.m.compute(k, typeCheck(biFunction));
        }

        @Override
        public V merge(K k, V v, final BiFunction<? super V, ? super V, ? extends V> biFunction) {
            Objects.requireNonNull(biFunction);
            return this.m.merge(k, v, new BiFunction() {
                @Override
                public final Object apply(Object obj, Object obj2) {
                    return Collections.CheckedMap.lambda$merge$2(this.f$0, biFunction, obj, obj2);
                }
            });
        }

        public static Object lambda$merge$2(CheckedMap checkedMap, BiFunction biFunction, Object obj, Object obj2) {
            Object objApply = biFunction.apply(obj, obj2);
            checkedMap.typeCheck(null, objApply);
            return objApply;
        }

        static class CheckedEntrySet<K, V> implements Set<Map.Entry<K, V>> {
            private final Set<Map.Entry<K, V>> s;
            private final Class<V> valueType;

            CheckedEntrySet(Set<Map.Entry<K, V>> set, Class<V> cls) {
                this.s = set;
                this.valueType = cls;
            }

            @Override
            public int size() {
                return this.s.size();
            }

            @Override
            public boolean isEmpty() {
                return this.s.isEmpty();
            }

            public String toString() {
                return this.s.toString();
            }

            @Override
            public int hashCode() {
                return this.s.hashCode();
            }

            @Override
            public void clear() {
                this.s.clear();
            }

            @Override
            public boolean add(Map.Entry<K, V> entry) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends Map.Entry<K, V>> collection) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                final Iterator<Map.Entry<K, V>> it = this.s.iterator();
                final Class<V> cls = this.valueType;
                return new Iterator<Map.Entry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        return CheckedEntrySet.checkedEntry((Map.Entry) it.next(), cls);
                    }
                };
            }

            @Override
            public Object[] toArray() {
                Object[] objArr;
                Object[] array = this.s.toArray();
                if (!CheckedEntry.class.isInstance(array.getClass().getComponentType())) {
                    objArr = new Object[array.length];
                } else {
                    objArr = array;
                }
                for (int i = 0; i < array.length; i++) {
                    objArr[i] = checkedEntry((Map.Entry) array[i], this.valueType);
                }
                return objArr;
            }

            @Override
            public <T> T[] toArray(T[] tArr) {
                T[] tArr2 = (T[]) this.s.toArray(tArr.length == 0 ? tArr : Arrays.copyOf(tArr, 0));
                for (int i = 0; i < tArr2.length; i++) {
                    tArr2[i] = checkedEntry((Map.Entry) tArr2[i], this.valueType);
                }
                if (tArr2.length > tArr.length) {
                    return tArr2;
                }
                System.arraycopy(tArr2, 0, tArr, 0, tArr2.length);
                if (tArr.length > tArr2.length) {
                    tArr[tArr2.length] = null;
                }
                return tArr;
            }

            @Override
            public boolean contains(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entryCheckedEntry = (Map.Entry) obj;
                Set<Map.Entry<K, V>> set = this.s;
                if (!(entryCheckedEntry instanceof CheckedEntry)) {
                    entryCheckedEntry = checkedEntry(entryCheckedEntry, this.valueType);
                }
                return set.contains(entryCheckedEntry);
            }

            @Override
            public boolean containsAll(Collection<?> collection) {
                Iterator<?> it = collection.iterator();
                while (it.hasNext()) {
                    if (!contains(it.next())) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean remove(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                return this.s.remove(new AbstractMap.SimpleImmutableEntry((Map.Entry) obj));
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                return batchRemove(collection, false);
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                return batchRemove(collection, true);
            }

            private boolean batchRemove(Collection<?> collection, boolean z) {
                Objects.requireNonNull(collection);
                Iterator<Map.Entry<K, V>> it = iterator();
                boolean z2 = false;
                while (it.hasNext()) {
                    if (collection.contains(it.next()) != z) {
                        it.remove();
                        z2 = true;
                    }
                }
                return z2;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Set)) {
                    return false;
                }
                Set set = (Set) obj;
                return set.size() == this.s.size() && containsAll(set);
            }

            static <K, V, T> CheckedEntry<K, V, T> checkedEntry(Map.Entry<K, V> entry, Class<T> cls) {
                return new CheckedEntry<>(entry, cls);
            }

            private static class CheckedEntry<K, V, T> implements Map.Entry<K, V> {
                private final Map.Entry<K, V> e;
                private final Class<T> valueType;

                CheckedEntry(Map.Entry<K, V> entry, Class<T> cls) {
                    this.e = (Map.Entry) Objects.requireNonNull(entry);
                    this.valueType = (Class) Objects.requireNonNull(cls);
                }

                @Override
                public K getKey() {
                    return this.e.getKey();
                }

                @Override
                public V getValue() {
                    return this.e.getValue();
                }

                @Override
                public int hashCode() {
                    return this.e.hashCode();
                }

                public String toString() {
                    return this.e.toString();
                }

                @Override
                public V setValue(V v) {
                    if (v != null && !this.valueType.isInstance(v)) {
                        throw new ClassCastException(badValueMsg(v));
                    }
                    return this.e.setValue(v);
                }

                private String badValueMsg(Object obj) {
                    return "Attempt to insert " + ((Object) obj.getClass()) + " value into map with value type " + ((Object) this.valueType);
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (!(obj instanceof Map.Entry)) {
                        return false;
                    }
                    return this.e.equals(new AbstractMap.SimpleImmutableEntry((Map.Entry) obj));
                }
            }
        }
    }

    public static <K, V> SortedMap<K, V> checkedSortedMap(SortedMap<K, V> sortedMap, Class<K> cls, Class<V> cls2) {
        return new CheckedSortedMap(sortedMap, cls, cls2);
    }

    static class CheckedSortedMap<K, V> extends CheckedMap<K, V> implements SortedMap<K, V>, Serializable {
        private static final long serialVersionUID = 1599671320688067438L;
        private final SortedMap<K, V> sm;

        CheckedSortedMap(SortedMap<K, V> sortedMap, Class<K> cls, Class<V> cls2) {
            super(sortedMap, cls, cls2);
            this.sm = sortedMap;
        }

        public Comparator<? super K> comparator() {
            return this.sm.comparator();
        }

        public K firstKey() {
            return this.sm.firstKey();
        }

        public K lastKey() {
            return this.sm.lastKey();
        }

        public SortedMap<K, V> subMap(K k, K k2) {
            return Collections.checkedSortedMap(this.sm.subMap(k, k2), this.keyType, this.valueType);
        }

        public SortedMap<K, V> headMap(K k) {
            return Collections.checkedSortedMap(this.sm.headMap(k), this.keyType, this.valueType);
        }

        public SortedMap<K, V> tailMap(K k) {
            return Collections.checkedSortedMap(this.sm.tailMap(k), this.keyType, this.valueType);
        }
    }

    public static <K, V> NavigableMap<K, V> checkedNavigableMap(NavigableMap<K, V> navigableMap, Class<K> cls, Class<V> cls2) {
        return new CheckedNavigableMap(navigableMap, cls, cls2);
    }

    static class CheckedNavigableMap<K, V> extends CheckedSortedMap<K, V> implements NavigableMap<K, V>, Serializable {
        private static final long serialVersionUID = -4852462692372534096L;
        private final NavigableMap<K, V> nm;

        CheckedNavigableMap(NavigableMap<K, V> navigableMap, Class<K> cls, Class<V> cls2) {
            super(navigableMap, cls, cls2);
            this.nm = navigableMap;
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.nm.comparator();
        }

        @Override
        public K firstKey() {
            return this.nm.firstKey();
        }

        @Override
        public K lastKey() {
            return this.nm.lastKey();
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K k) {
            Map.Entry<K, V> entryLowerEntry = this.nm.lowerEntry(k);
            if (entryLowerEntry != null) {
                return new CheckedMap.CheckedEntrySet.CheckedEntry(entryLowerEntry, this.valueType);
            }
            return null;
        }

        @Override
        public K lowerKey(K k) {
            return this.nm.lowerKey(k);
        }

        @Override
        public Map.Entry<K, V> floorEntry(K k) {
            Map.Entry<K, V> entryFloorEntry = this.nm.floorEntry(k);
            if (entryFloorEntry != null) {
                return new CheckedMap.CheckedEntrySet.CheckedEntry(entryFloorEntry, this.valueType);
            }
            return null;
        }

        @Override
        public K floorKey(K k) {
            return this.nm.floorKey(k);
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K k) {
            Map.Entry<K, V> entryCeilingEntry = this.nm.ceilingEntry(k);
            if (entryCeilingEntry != null) {
                return new CheckedMap.CheckedEntrySet.CheckedEntry(entryCeilingEntry, this.valueType);
            }
            return null;
        }

        @Override
        public K ceilingKey(K k) {
            return this.nm.ceilingKey(k);
        }

        @Override
        public Map.Entry<K, V> higherEntry(K k) {
            Map.Entry<K, V> entryHigherEntry = this.nm.higherEntry(k);
            if (entryHigherEntry != null) {
                return new CheckedMap.CheckedEntrySet.CheckedEntry(entryHigherEntry, this.valueType);
            }
            return null;
        }

        @Override
        public K higherKey(K k) {
            return this.nm.higherKey(k);
        }

        @Override
        public Map.Entry<K, V> firstEntry() {
            Map.Entry<K, V> entryFirstEntry = this.nm.firstEntry();
            if (entryFirstEntry != null) {
                return new CheckedMap.CheckedEntrySet.CheckedEntry(entryFirstEntry, this.valueType);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> lastEntry() {
            Map.Entry<K, V> entryLastEntry = this.nm.lastEntry();
            if (entryLastEntry != null) {
                return new CheckedMap.CheckedEntrySet.CheckedEntry(entryLastEntry, this.valueType);
            }
            return null;
        }

        @Override
        public Map.Entry<K, V> pollFirstEntry() {
            Map.Entry<K, V> entryPollFirstEntry = this.nm.pollFirstEntry();
            if (entryPollFirstEntry == null) {
                return null;
            }
            return new CheckedMap.CheckedEntrySet.CheckedEntry(entryPollFirstEntry, this.valueType);
        }

        @Override
        public Map.Entry<K, V> pollLastEntry() {
            Map.Entry<K, V> entryPollLastEntry = this.nm.pollLastEntry();
            if (entryPollLastEntry == null) {
                return null;
            }
            return new CheckedMap.CheckedEntrySet.CheckedEntry(entryPollLastEntry, this.valueType);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return Collections.checkedNavigableMap(this.nm.descendingMap(), this.keyType, this.valueType);
        }

        @Override
        public NavigableSet<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return Collections.checkedNavigableSet(this.nm.navigableKeySet(), this.keyType);
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return Collections.checkedNavigableSet(this.nm.descendingKeySet(), this.keyType);
        }

        @Override
        public NavigableMap<K, V> subMap(K k, K k2) {
            return Collections.checkedNavigableMap(this.nm.subMap(k, true, k2, false), this.keyType, this.valueType);
        }

        @Override
        public NavigableMap<K, V> headMap(K k) {
            return Collections.checkedNavigableMap(this.nm.headMap(k, false), this.keyType, this.valueType);
        }

        @Override
        public NavigableMap<K, V> tailMap(K k) {
            return Collections.checkedNavigableMap(this.nm.tailMap(k, true), this.keyType, this.valueType);
        }

        @Override
        public NavigableMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
            return Collections.checkedNavigableMap(this.nm.subMap(k, z, k2, z2), this.keyType, this.valueType);
        }

        @Override
        public NavigableMap<K, V> headMap(K k, boolean z) {
            return Collections.checkedNavigableMap(this.nm.headMap(k, z), this.keyType, this.valueType);
        }

        @Override
        public NavigableMap<K, V> tailMap(K k, boolean z) {
            return Collections.checkedNavigableMap(this.nm.tailMap(k, z), this.keyType, this.valueType);
        }
    }

    public static <T> Iterator<T> emptyIterator() {
        return EmptyIterator.EMPTY_ITERATOR;
    }

    private static class EmptyIterator<E> implements Iterator<E> {
        static final EmptyIterator<Object> EMPTY_ITERATOR = new EmptyIterator<>();

        private EmptyIterator() {
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new IllegalStateException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
        }
    }

    public static <T> ListIterator<T> emptyListIterator() {
        return EmptyListIterator.EMPTY_ITERATOR;
    }

    private static class EmptyListIterator<E> extends EmptyIterator<E> implements ListIterator<E> {
        static final EmptyListIterator<Object> EMPTY_ITERATOR = new EmptyListIterator<>();

        private EmptyListIterator() {
            super();
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public E previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }

        @Override
        public void set(E e) {
            throw new IllegalStateException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    public static <T> Enumeration<T> emptyEnumeration() {
        return EmptyEnumeration.EMPTY_ENUMERATION;
    }

    private static class EmptyEnumeration<E> implements Enumeration<E> {
        static final EmptyEnumeration<Object> EMPTY_ENUMERATION = new EmptyEnumeration<>();

        private EmptyEnumeration() {
        }

        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public E nextElement() {
            throw new NoSuchElementException();
        }
    }

    static {
        EMPTY_SET = new EmptySet();
        EMPTY_LIST = new EmptyList();
        EMPTY_MAP = new EmptyMap();
    }

    public static final <T> Set<T> emptySet() {
        return EMPTY_SET;
    }

    private static class EmptySet<E> extends AbstractSet<E> implements Serializable {
        private static final long serialVersionUID = 1582296315990362920L;

        private EmptySet() {
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return collection.isEmpty();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            if (tArr.length > 0) {
                tArr[0] = null;
            }
            return tArr;
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            Objects.requireNonNull(predicate);
            return false;
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.emptySpliterator();
        }

        private Object readResolve() {
            return Collections.EMPTY_SET;
        }
    }

    public static <E> SortedSet<E> emptySortedSet() {
        return UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
    }

    public static <E> NavigableSet<E> emptyNavigableSet() {
        return UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
    }

    public static final <T> List<T> emptyList() {
        return EMPTY_LIST;
    }

    private static class EmptyList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        private static final long serialVersionUID = 8842843931221139166L;

        private EmptyList() {
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public ListIterator<E> listIterator() {
            return Collections.emptyListIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return collection.isEmpty();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            if (tArr.length > 0) {
                tArr[0] = null;
            }
            return tArr;
        }

        @Override
        public E get(int i) {
            throw new IndexOutOfBoundsException("Index: " + i);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof List) && ((List) obj).isEmpty();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            Objects.requireNonNull(predicate);
            return false;
        }

        @Override
        public void replaceAll(UnaryOperator<E> unaryOperator) {
            Objects.requireNonNull(unaryOperator);
        }

        @Override
        public void sort(Comparator<? super E> comparator) {
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.emptySpliterator();
        }

        private Object readResolve() {
            return Collections.EMPTY_LIST;
        }
    }

    public static final <K, V> Map<K, V> emptyMap() {
        return EMPTY_MAP;
    }

    public static final <K, V> SortedMap<K, V> emptySortedMap() {
        return UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
    }

    public static final <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
    }

    private static class EmptyMap<K, V> extends AbstractMap<K, V> implements Serializable {
        private static final long serialVersionUID = 6428348081105594320L;

        private EmptyMap() {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object obj) {
            return false;
        }

        @Override
        public boolean containsValue(Object obj) {
            return false;
        }

        @Override
        public V get(Object obj) {
            return null;
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptySet();
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Map) && ((Map) obj).isEmpty();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public V getOrDefault(Object obj, V v) {
            return v;
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
            Objects.requireNonNull(biConsumer);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
            Objects.requireNonNull(biFunction);
        }

        @Override
        public V putIfAbsent(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K k, V v, V v2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        private Object readResolve() {
            return Collections.EMPTY_MAP;
        }
    }

    public static <T> Set<T> singleton(T t) {
        return new SingletonSet(t);
    }

    static <E> Iterator<E> singletonIterator(final E e) {
        return new Iterator<E>() {
            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return this.hasNext;
            }

            @Override
            public E next() {
                if (this.hasNext) {
                    this.hasNext = false;
                    return (E) e;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachRemaining(Consumer<? super E> consumer) {
                Objects.requireNonNull(consumer);
                if (this.hasNext) {
                    consumer.accept((Object) e);
                    this.hasNext = false;
                }
            }
        };
    }

    static <T> Spliterator<T> singletonSpliterator(final T t) {
        return new Spliterator<T>() {
            long est = 1;

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                if (this.est > 0) {
                    this.est--;
                    consumer.accept((Object) t);
                    return true;
                }
                return false;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                tryAdvance(consumer);
            }

            @Override
            public long estimateSize() {
                return this.est;
            }

            @Override
            public int characteristics() {
                return (t != null ? 256 : 0) | 64 | 16384 | 1024 | 1 | 16;
            }
        };
    }

    private static class SingletonSet<E> extends AbstractSet<E> implements Serializable {
        private static final long serialVersionUID = 3193687207550431679L;
        private final E element;

        SingletonSet(E e) {
            this.element = e;
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.singletonIterator(this.element);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean contains(Object obj) {
            return Collections.eq(obj, this.element);
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            consumer.accept(this.element);
        }

        @Override
        public Spliterator<E> spliterator() {
            return Collections.singletonSpliterator(this.element);
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            throw new UnsupportedOperationException();
        }
    }

    public static <T> List<T> singletonList(T t) {
        return new SingletonList(t);
    }

    private static class SingletonList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        private static final long serialVersionUID = 3093736618740652951L;
        private final E element;

        SingletonList(E e) {
            this.element = e;
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.singletonIterator(this.element);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean contains(Object obj) {
            return Collections.eq(obj, this.element);
        }

        @Override
        public E get(int i) {
            if (i != 0) {
                throw new IndexOutOfBoundsException("Index: " + i + ", Size: 1");
            }
            return this.element;
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            consumer.accept(this.element);
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(UnaryOperator<E> unaryOperator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super E> comparator) {
        }

        @Override
        public Spliterator<E> spliterator() {
            return Collections.singletonSpliterator(this.element);
        }
    }

    public static <K, V> Map<K, V> singletonMap(K k, V v) {
        return new SingletonMap(k, v);
    }

    private static class SingletonMap<K, V> extends AbstractMap<K, V> implements Serializable {
        private static final long serialVersionUID = -6979724477215052911L;
        private transient Set<Map.Entry<K, V>> entrySet;
        private final K k;
        private transient Set<K> keySet;
        private final V v;
        private transient Collection<V> values;

        SingletonMap(K k, V v) {
            this.k = k;
            this.v = v;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object obj) {
            return Collections.eq(obj, this.k);
        }

        @Override
        public boolean containsValue(Object obj) {
            return Collections.eq(obj, this.v);
        }

        @Override
        public V get(Object obj) {
            if (Collections.eq(obj, this.k)) {
                return this.v;
            }
            return null;
        }

        @Override
        public Set<K> keySet() {
            if (this.keySet == null) {
                this.keySet = Collections.singleton(this.k);
            }
            return this.keySet;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            if (this.entrySet == null) {
                this.entrySet = Collections.singleton(new AbstractMap.SimpleImmutableEntry(this.k, this.v));
            }
            return this.entrySet;
        }

        @Override
        public Collection<V> values() {
            if (this.values == null) {
                this.values = Collections.singleton(this.v);
            }
            return this.values;
        }

        @Override
        public V getOrDefault(Object obj, V v) {
            return Collections.eq(obj, this.k) ? this.v : v;
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
            biConsumer.accept(this.k, this.v);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V putIfAbsent(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object obj, Object obj2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K k, V v, V v2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K k, V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }
    }

    public static <T> List<T> nCopies(int i, T t) {
        if (i < 0) {
            throw new IllegalArgumentException("List length = " + i);
        }
        return new CopiesList(i, t);
    }

    private static class CopiesList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        static final boolean $assertionsDisabled = false;
        private static final long serialVersionUID = 2739099268398711800L;
        final E element;
        final int n;

        CopiesList(int i, E e) {
            this.n = i;
            this.element = e;
        }

        @Override
        public int size() {
            return this.n;
        }

        @Override
        public boolean contains(Object obj) {
            if (this.n == 0 || !Collections.eq(obj, this.element)) {
                return $assertionsDisabled;
            }
            return true;
        }

        @Override
        public int indexOf(Object obj) {
            return contains(obj) ? 0 : -1;
        }

        @Override
        public int lastIndexOf(Object obj) {
            if (contains(obj)) {
                return this.n - 1;
            }
            return -1;
        }

        @Override
        public E get(int i) {
            if (i < 0 || i >= this.n) {
                throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + this.n);
            }
            return this.element;
        }

        @Override
        public Object[] toArray() {
            Object[] objArr = new Object[this.n];
            if (this.element != null) {
                Arrays.fill(objArr, 0, this.n, this.element);
            }
            return objArr;
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            int i = this.n;
            if (tArr.length < i) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i));
                if (this.element != null) {
                    Arrays.fill(tArr, 0, i, this.element);
                }
            } else {
                Arrays.fill(tArr, 0, i, this.element);
                if (tArr.length > i) {
                    tArr[i] = null;
                }
            }
            return tArr;
        }

        @Override
        public List<E> subList(int i, int i2) {
            if (i < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + i);
            }
            if (i2 > this.n) {
                throw new IndexOutOfBoundsException("toIndex = " + i2);
            }
            if (i > i2) {
                throw new IllegalArgumentException("fromIndex(" + i + ") > toIndex(" + i2 + ")");
            }
            return new CopiesList(i2 - i, this.element);
        }

        @Override
        public Stream<E> stream() {
            return IntStream.range(0, this.n).mapToObj(new IntFunction() {
                @Override
                public final Object apply(int i) {
                    return this.f$0.element;
                }
            });
        }

        @Override
        public Stream<E> parallelStream() {
            return IntStream.range(0, this.n).parallel().mapToObj(new IntFunction() {
                @Override
                public final Object apply(int i) {
                    return this.f$0.element;
                }
            });
        }

        @Override
        public Spliterator<E> spliterator() {
            return stream().spliterator2();
        }
    }

    public static <T> Comparator<T> reverseOrder() {
        return ReverseComparator.REVERSE_ORDER;
    }

    private static class ReverseComparator implements Comparator<Comparable<Object>>, Serializable {
        static final ReverseComparator REVERSE_ORDER = new ReverseComparator();
        private static final long serialVersionUID = 7207038068494060240L;

        private ReverseComparator() {
        }

        @Override
        public int compare(Comparable<Object> comparable, Comparable<Object> comparable2) {
            return comparable2.compareTo(comparable);
        }

        private Object readResolve() {
            return Collections.reverseOrder();
        }

        @Override
        public Comparator<Comparable<Object>> reversed() {
            return Comparator.naturalOrder();
        }
    }

    public static <T> Comparator<T> reverseOrder(Comparator<T> comparator) {
        if (comparator == null) {
            return reverseOrder();
        }
        if (comparator instanceof ReverseComparator2) {
            return ((ReverseComparator2) comparator).cmp;
        }
        return new ReverseComparator2(comparator);
    }

    private static class ReverseComparator2<T> implements Comparator<T>, Serializable {
        static final boolean $assertionsDisabled = false;
        private static final long serialVersionUID = 4374092139857L;
        final Comparator<T> cmp;

        ReverseComparator2(Comparator<T> comparator) {
            this.cmp = comparator;
        }

        @Override
        public int compare(T t, T t2) {
            return this.cmp.compare(t2, t);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this || ((obj instanceof ReverseComparator2) && this.cmp.equals(((ReverseComparator2) obj).cmp))) {
                return true;
            }
            return $assertionsDisabled;
        }

        public int hashCode() {
            return this.cmp.hashCode() ^ Integer.MIN_VALUE;
        }

        @Override
        public Comparator<T> reversed() {
            return this.cmp;
        }
    }

    public static <T> Enumeration<T> enumeration(final Collection<T> collection) {
        return new Enumeration<T>() {
            private final Iterator<T> i;

            {
                this.i = collection.iterator();
            }

            @Override
            public boolean hasMoreElements() {
                return this.i.hasNext();
            }

            @Override
            public T nextElement() {
                return this.i.next();
            }
        };
    }

    public static <T> ArrayList<T> list(Enumeration<T> enumeration) {
        ArrayList<T> arrayList = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            arrayList.add(enumeration.nextElement());
        }
        return arrayList;
    }

    static boolean eq(Object obj, Object obj2) {
        return obj == null ? obj2 == null : obj.equals(obj2);
    }

    public static int frequency(Collection<?> collection, Object obj) {
        int i = 0;
        if (obj == null) {
            Iterator<?> it = collection.iterator();
            while (it.hasNext()) {
                if (it.next() == null) {
                    i++;
                }
            }
        } else {
            Iterator<?> it2 = collection.iterator();
            while (it2.hasNext()) {
                if (obj.equals(it2.next())) {
                    i++;
                }
            }
        }
        return i;
    }

    public static boolean disjoint(Collection<?> collection, Collection<?> collection2) {
        if (!(collection instanceof Set)) {
            if (!(collection2 instanceof Set)) {
                int size = collection.size();
                int size2 = collection2.size();
                if (size == 0 || size2 == 0) {
                    return true;
                }
                if (size > size2) {
                    collection2 = collection;
                    collection = collection2;
                }
            }
        }
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            if (collection2.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public static <T> boolean addAll(Collection<? super T> collection, T... tArr) {
        boolean zAdd = false;
        for (T t : tArr) {
            zAdd |= collection.add((Object) t);
        }
        return zAdd;
    }

    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        return new SetFromMap(map);
    }

    private static class SetFromMap<E> extends AbstractSet<E> implements Set<E>, Serializable {
        private static final long serialVersionUID = 2454657854757543876L;
        private final Map<E, Boolean> m;
        private transient Set<E> s;

        SetFromMap(Map<E, Boolean> map) {
            if (!map.isEmpty()) {
                throw new IllegalArgumentException("Map is non-empty");
            }
            this.m = map;
            this.s = map.keySet();
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            return this.m.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            return this.m.remove(obj) != null;
        }

        @Override
        public boolean add(E e) {
            return this.m.put(e, Boolean.TRUE) == null;
        }

        @Override
        public Iterator<E> iterator() {
            return this.s.iterator();
        }

        @Override
        public Object[] toArray() {
            return this.s.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.s.toArray(tArr);
        }

        @Override
        public String toString() {
            return this.s.toString();
        }

        @Override
        public int hashCode() {
            return this.s.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.s.equals(obj);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.s.containsAll(collection);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return this.s.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return this.s.retainAll(collection);
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            this.s.forEach(consumer);
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            return this.s.removeIf(predicate);
        }

        @Override
        public Spliterator<E> spliterator() {
            return this.s.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return this.s.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return this.s.parallelStream();
        }

        private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
            objectInputStream.defaultReadObject();
            this.s = this.m.keySet();
        }
    }

    public static <T> Queue<T> asLifoQueue(Deque<T> deque) {
        return new AsLIFOQueue(deque);
    }

    static class AsLIFOQueue<E> extends AbstractQueue<E> implements Queue<E>, Serializable {
        private static final long serialVersionUID = 1802017725587941708L;
        private final Deque<E> q;

        AsLIFOQueue(Deque<E> deque) {
            this.q = deque;
        }

        @Override
        public boolean add(E e) {
            this.q.addFirst(e);
            return true;
        }

        @Override
        public boolean offer(E e) {
            return this.q.offerFirst(e);
        }

        @Override
        public E poll() {
            return this.q.pollFirst();
        }

        @Override
        public E remove() {
            return this.q.removeFirst();
        }

        @Override
        public E peek() {
            return this.q.peekFirst();
        }

        @Override
        public E element() {
            return this.q.getFirst();
        }

        @Override
        public void clear() {
            this.q.clear();
        }

        @Override
        public int size() {
            return this.q.size();
        }

        @Override
        public boolean isEmpty() {
            return this.q.isEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            return this.q.contains(obj);
        }

        @Override
        public boolean remove(Object obj) {
            return this.q.remove(obj);
        }

        @Override
        public Iterator<E> iterator() {
            return this.q.iterator();
        }

        @Override
        public Object[] toArray() {
            return this.q.toArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) this.q.toArray(tArr);
        }

        @Override
        public String toString() {
            return this.q.toString();
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return this.q.containsAll(collection);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return this.q.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return this.q.retainAll(collection);
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            this.q.forEach(consumer);
        }

        @Override
        public boolean removeIf(Predicate<? super E> predicate) {
            return this.q.removeIf(predicate);
        }

        @Override
        public Spliterator<E> spliterator() {
            return this.q.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return this.q.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return this.q.parallelStream();
        }
    }
}
