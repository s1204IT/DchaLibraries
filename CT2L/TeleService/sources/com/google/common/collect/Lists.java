package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.util.ArrayList;

public final class Lists {
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    static int computeArrayListCapacity(int arraySize) {
        Preconditions.checkArgument(arraySize >= 0);
        return Ints.saturatedCast(5 + ((long) arraySize) + ((long) (arraySize / 10)));
    }
}
