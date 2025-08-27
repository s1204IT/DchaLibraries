package com.google.common.collect;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.util.Collection;

/* loaded from: classes.dex */
public final class Collections2 {
    static final Joiner STANDARD_JOINER = Joiner.on(", ").useForNull("null");

    static boolean safeContains(Collection<?> collection, Object obj) {
        Preconditions.checkNotNull(collection);
        try {
            return collection.contains(obj);
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    static StringBuilder newStringBuilderForCollection(int i) {
        CollectPreconditions.checkNonnegative(i, "size");
        return new StringBuilder((int) Math.min(i * 8, 1073741824L));
    }

    static <T> Collection<T> cast(Iterable<T> iterable) {
        return (Collection) iterable;
    }
}
