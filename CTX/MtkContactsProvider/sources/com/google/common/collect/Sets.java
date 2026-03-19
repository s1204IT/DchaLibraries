package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class Sets {

    static abstract class ImprovedAbstractSet<E> extends AbstractSet<E> {
        ImprovedAbstractSet() {
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return Sets.removeAllImpl(this, collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return super.retainAll((Collection) Preconditions.checkNotNull(collection));
        }
    }

    public static <E> HashSet<E> newHashSet() {
        return new HashSet<>();
    }

    public static <E> HashSet<E> newHashSetWithExpectedSize(int i) {
        return new HashSet<>(Maps.capacity(i));
    }

    public static abstract class SetView<E> extends AbstractSet<E> {
        SetView(AnonymousClass1 anonymousClass1) {
            this();
        }

        private SetView() {
        }
    }

    class AnonymousClass1<E> extends SetView<E> {
        final Set val$set1;
        final Set val$set2;
        final Set val$set2minus1;

        @Override
        public int size() {
            return this.val$set1.size() + this.val$set2minus1.size();
        }

        @Override
        public boolean isEmpty() {
            return this.val$set1.isEmpty() && this.val$set2.isEmpty();
        }

        @Override
        public Iterator<E> iterator() {
            return Iterators.unmodifiableIterator(Iterators.concat(this.val$set1.iterator(), this.val$set2minus1.iterator()));
        }

        @Override
        public boolean contains(Object obj) {
            return this.val$set1.contains(obj) || this.val$set2.contains(obj);
        }
    }

    public static <E> SetView<E> difference(final Set<E> set, final Set<?> set2) {
        Preconditions.checkNotNull(set, "set1");
        Preconditions.checkNotNull(set2, "set2");
        final Predicate predicateNot = Predicates.not(Predicates.in(set2));
        return new SetView<E>() {
            {
                super(null);
            }

            @Override
            public Iterator<E> iterator() {
                return Iterators.filter(set.iterator(), predicateNot);
            }

            @Override
            public int size() {
                return Iterators.size(iterator());
            }

            @Override
            public boolean isEmpty() {
                return set2.containsAll(set);
            }

            @Override
            public boolean contains(Object obj) {
                return set.contains(obj) && !set2.contains(obj);
            }
        };
    }

    static int hashCodeImpl(Set<?> set) {
        Iterator<?> it = set.iterator();
        int i = 0;
        while (it.hasNext()) {
            Object next = it.next();
            i = ~(~(i + (next != null ? next.hashCode() : 0)));
        }
        return i;
    }

    static boolean equalsImpl(Set<?> set, Object obj) {
        if (set == obj) {
            return true;
        }
        if (!(obj instanceof Set)) {
            return false;
        }
        Set set2 = (Set) obj;
        try {
            if (set.size() == set2.size()) {
                if (set.containsAll(set2)) {
                    return true;
                }
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    static boolean removeAllImpl(Set<?> set, Iterator<?> it) {
        boolean zRemove = false;
        while (it.hasNext()) {
            zRemove |= set.remove(it.next());
        }
        return zRemove;
    }

    static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
        Preconditions.checkNotNull(collection);
        if (collection instanceof Multiset) {
            collection = ((Multiset) collection).elementSet();
        }
        if ((collection instanceof Set) && collection.size() > set.size()) {
            return Iterators.removeAll(set.iterator(), collection);
        }
        return removeAllImpl(set, collection.iterator());
    }
}
