package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.util.Collection;

public final class Collections2 {
    static final Joiner STANDARD_JOINER = Joiner.on(", ");

    static boolean containsAllImpl(Collection<?> self, Collection<?> c) {
        Preconditions.checkNotNull(self);
        for (Object o : c) {
            if (!self.contains(o)) {
                return false;
            }
        }
        return true;
    }

    static String toStringImpl(final Collection<?> collection) {
        StringBuilder sb = newStringBuilderForCollection(collection.size()).append('[');
        STANDARD_JOINER.appendTo(sb, Iterables.transform(collection, new Function<Object, Object>() {
            @Override
            public Object apply(Object input) {
                return input == collection ? "(this Collection)" : input;
            }
        }));
        return sb.append(']').toString();
    }

    static StringBuilder newStringBuilderForCollection(int size) {
        Preconditions.checkArgument(size >= 0, "size must be non-negative");
        return new StringBuilder((int) Math.min(((long) size) * 8, 1073741824L));
    }

    static <T> Collection<T> cast(Iterable<T> iterable) {
        return (Collection) iterable;
    }
}
