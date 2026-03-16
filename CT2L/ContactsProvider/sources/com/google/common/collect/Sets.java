package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public final class Sets {
    public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
        return new HashSet<>(Maps.capacity(expectedSize));
    }

    public static <E> TreeSet<E> newTreeSet(Comparator<? super E> comparator) {
        return new TreeSet<>((Comparator) Preconditions.checkNotNull(comparator));
    }

    static int hashCodeImpl(Set<?> s) {
        int hashCode = 0;
        Iterator<?> it = s.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            hashCode += o != null ? o.hashCode() : 0;
        }
        return hashCode;
    }

    static boolean equalsImpl(Set<?> s, Object object) {
        boolean z;
        if (s == object) {
            return true;
        }
        if (!(object instanceof Set)) {
            return false;
        }
        Set<?> o = (Set) object;
        try {
            if (s.size() == o.size()) {
                z = s.containsAll(o);
            }
            return z;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }
}
