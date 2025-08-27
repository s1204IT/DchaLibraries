package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/* loaded from: classes.dex */
public final class Iterators {
    static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR = new UnmodifiableListIterator<Object>() { // from class: com.google.common.collect.Iterators.1
        AnonymousClass1() {
        }

        @Override // java.util.Iterator, java.util.ListIterator
        public boolean hasNext() {
            return false;
        }

        @Override // java.util.Iterator, java.util.ListIterator
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override // java.util.ListIterator
        public boolean hasPrevious() {
            return false;
        }

        @Override // java.util.ListIterator
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override // java.util.ListIterator
        public int nextIndex() {
            return 0;
        }

        @Override // java.util.ListIterator
        public int previousIndex() {
            return -1;
        }
    };
    private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR = new Iterator<Object>() { // from class: com.google.common.collect.Iterators.2
        AnonymousClass2() {
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return false;
        }

        @Override // java.util.Iterator
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override // java.util.Iterator
        public void remove() {
            CollectPreconditions.checkRemove(false);
        }
    };

    /* renamed from: com.google.common.collect.Iterators$1 */
    class AnonymousClass1 extends UnmodifiableListIterator<Object> {
        AnonymousClass1() {
        }

        @Override // java.util.Iterator, java.util.ListIterator
        public boolean hasNext() {
            return false;
        }

        @Override // java.util.Iterator, java.util.ListIterator
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override // java.util.ListIterator
        public boolean hasPrevious() {
            return false;
        }

        @Override // java.util.ListIterator
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override // java.util.ListIterator
        public int nextIndex() {
            return 0;
        }

        @Override // java.util.ListIterator
        public int previousIndex() {
            return -1;
        }
    }

    @Deprecated
    public static <T> UnmodifiableIterator<T> emptyIterator() {
        return emptyListIterator();
    }

    static <T> UnmodifiableListIterator<T> emptyListIterator() {
        return (UnmodifiableListIterator<T>) EMPTY_LIST_ITERATOR;
    }

    /* renamed from: com.google.common.collect.Iterators$2 */
    class AnonymousClass2 implements Iterator<Object> {
        AnonymousClass2() {
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return false;
        }

        @Override // java.util.Iterator
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override // java.util.Iterator
        public void remove() {
            CollectPreconditions.checkRemove(false);
        }
    }

    static <T> Iterator<T> emptyModifiableIterator() {
        return (Iterator<T>) EMPTY_MODIFIABLE_ITERATOR;
    }

    public static <T> UnmodifiableIterator<T> unmodifiableIterator(Iterator<T> it) {
        Preconditions.checkNotNull(it);
        if (it instanceof UnmodifiableIterator) {
            return (UnmodifiableIterator) it;
        }
        return new UnmodifiableIterator<T>() { // from class: com.google.common.collect.Iterators.3
            final /* synthetic */ Iterator val$iterator;

            AnonymousClass3(Iterator it2) {
                it = it2;
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override // java.util.Iterator
            public T next() {
                return (T) it.next();
            }
        };
    }

    /* renamed from: com.google.common.collect.Iterators$3 */
    class AnonymousClass3<T> extends UnmodifiableIterator<T> {
        final /* synthetic */ Iterator val$iterator;

        AnonymousClass3(Iterator it2) {
            it = it2;
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override // java.util.Iterator
        public T next() {
            return (T) it.next();
        }
    }

    public static int size(Iterator<?> it) {
        int i = 0;
        while (it.hasNext()) {
            it.next();
            i++;
        }
        return i;
    }

    public static boolean contains(Iterator<?> it, Object obj) {
        return any(it, Predicates.equalTo(obj));
    }

    public static boolean removeAll(Iterator<?> it, Collection<?> collection) {
        return removeIf(it, Predicates.in(collection));
    }

    public static <T> boolean removeIf(Iterator<T> it, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate);
        boolean z = false;
        while (it.hasNext()) {
            if (predicate.apply(it.next())) {
                it.remove();
                z = true;
            }
        }
        return z;
    }

    public static boolean elementsEqual(Iterator<?> it, Iterator<?> it2) {
        while (it.hasNext()) {
            if (!it2.hasNext() || !Objects.equal(it.next(), it2.next())) {
                return false;
            }
        }
        return !it2.hasNext();
    }

    public static <T> T getOnlyElement(Iterator<T> it) {
        T next = it.next();
        if (!it.hasNext()) {
            return next;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("expected one element but was: <" + next);
        for (int i = 0; i < 4 && it.hasNext(); i++) {
            sb.append(", " + it.next());
        }
        if (it.hasNext()) {
            sb.append(", ...");
        }
        sb.append('>');
        throw new IllegalArgumentException(sb.toString());
    }

    public static <T> boolean addAll(Collection<T> collection, Iterator<? extends T> it) {
        Preconditions.checkNotNull(collection);
        Preconditions.checkNotNull(it);
        boolean zAdd = false;
        while (it.hasNext()) {
            zAdd |= collection.add(it.next());
        }
        return zAdd;
    }

    public static <T> boolean any(Iterator<T> it, Predicate<? super T> predicate) {
        return indexOf(it, predicate) != -1;
    }

    public static <T> int indexOf(Iterator<T> it, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate, "predicate");
        int i = 0;
        while (it.hasNext()) {
            if (!predicate.apply(it.next())) {
                i++;
            } else {
                return i;
            }
        }
        return -1;
    }

    /* renamed from: com.google.common.collect.Iterators$8 */
    class AnonymousClass8<F, T> extends TransformedIterator<F, T> {
        final /* synthetic */ Function val$function;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass8(Iterator it, Function function) {
            super(it);
            function = function;
        }

        @Override // com.google.common.collect.TransformedIterator
        T transform(F f) {
            return (T) function.apply(f);
        }
    }

    public static <F, T> Iterator<T> transform(Iterator<F> it, Function<? super F, ? extends T> function) {
        Preconditions.checkNotNull(function);
        return new TransformedIterator<F, T>(it) { // from class: com.google.common.collect.Iterators.8
            final /* synthetic */ Function val$function;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            AnonymousClass8(Iterator it2, Function function2) {
                super(it2);
                function = function2;
            }

            @Override // com.google.common.collect.TransformedIterator
            T transform(F f) {
                return (T) function.apply(f);
            }
        };
    }

    public static <T> T getNext(Iterator<? extends T> it, T t) {
        return it.hasNext() ? it.next() : t;
    }

    static <T> T pollNext(Iterator<T> it) {
        if (it.hasNext()) {
            T next = it.next();
            it.remove();
            return next;
        }
        return null;
    }

    static void clear(Iterator<?> it) {
        Preconditions.checkNotNull(it);
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    public static <T> UnmodifiableIterator<T> forArray(T... tArr) {
        return forArray(tArr, 0, tArr.length, 0);
    }

    static <T> UnmodifiableListIterator<T> forArray(T[] tArr, int i, int i2, int i3) {
        Preconditions.checkArgument(i2 >= 0);
        Preconditions.checkPositionIndexes(i, i + i2, tArr.length);
        Preconditions.checkPositionIndex(i3, i2);
        if (i2 == 0) {
            return emptyListIterator();
        }
        return new AbstractIndexedListIterator<T>(i2, i3) { // from class: com.google.common.collect.Iterators.11
            final /* synthetic */ Object[] val$array;
            final /* synthetic */ int val$offset;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            AnonymousClass11(int i22, int i32, Object[] tArr2, int i4) {
                super(i22, i32);
                objArr = tArr2;
                i = i4;
            }

            @Override // com.google.common.collect.AbstractIndexedListIterator
            protected T get(int i4) {
                return (T) objArr[i + i4];
            }
        };
    }

    /* renamed from: com.google.common.collect.Iterators$11 */
    class AnonymousClass11<T> extends AbstractIndexedListIterator<T> {
        final /* synthetic */ Object[] val$array;
        final /* synthetic */ int val$offset;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass11(int i22, int i32, Object[] tArr2, int i4) {
            super(i22, i32);
            objArr = tArr2;
            i = i4;
        }

        @Override // com.google.common.collect.AbstractIndexedListIterator
        protected T get(int i4) {
            return (T) objArr[i + i4];
        }
    }

    /* renamed from: com.google.common.collect.Iterators$12 */
    class AnonymousClass12<T> extends UnmodifiableIterator<T> {
        boolean done;
        final /* synthetic */ Object val$value;

        AnonymousClass12(Object obj) {
            obj = obj;
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return !this.done;
        }

        @Override // java.util.Iterator
        public T next() {
            if (this.done) {
                throw new NoSuchElementException();
            }
            this.done = true;
            return (T) obj;
        }
    }

    public static <T> UnmodifiableIterator<T> singletonIterator(T t) {
        return new UnmodifiableIterator<T>() { // from class: com.google.common.collect.Iterators.12
            boolean done;
            final /* synthetic */ Object val$value;

            AnonymousClass12(Object t2) {
                obj = t2;
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                return !this.done;
            }

            @Override // java.util.Iterator
            public T next() {
                if (this.done) {
                    throw new NoSuchElementException();
                }
                this.done = true;
                return (T) obj;
            }
        };
    }

    private static class PeekingImpl<E> implements PeekingIterator<E> {
        private boolean hasPeeked;
        private final Iterator<? extends E> iterator;
        private E peekedElement;

        public PeekingImpl(Iterator<? extends E> it) {
            this.iterator = (Iterator) Preconditions.checkNotNull(it);
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.hasPeeked || this.iterator.hasNext();
        }

        @Override // com.google.common.collect.PeekingIterator, java.util.Iterator
        public E next() {
            if (!this.hasPeeked) {
                return this.iterator.next();
            }
            E e = this.peekedElement;
            this.hasPeeked = false;
            this.peekedElement = null;
            return e;
        }

        @Override // java.util.Iterator
        public void remove() {
            Preconditions.checkState(!this.hasPeeked, "Can't remove after you've peeked at next");
            this.iterator.remove();
        }

        @Override // com.google.common.collect.PeekingIterator
        public E peek() {
            if (!this.hasPeeked) {
                this.peekedElement = this.iterator.next();
                this.hasPeeked = true;
            }
            return this.peekedElement;
        }
    }

    public static <T> PeekingIterator<T> peekingIterator(Iterator<? extends T> it) {
        if (it instanceof PeekingImpl) {
            return (PeekingImpl) it;
        }
        return new PeekingImpl(it);
    }
}
