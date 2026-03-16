package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.LinkedList;

public final class Lists {
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    static int computeArrayListCapacity(int arraySize) {
        Preconditions.checkArgument(arraySize >= 0);
        return Ints.saturatedCast(5 + ((long) arraySize) + ((long) (arraySize / 10)));
    }

    public static <E> LinkedList<E> newLinkedList() {
        return new LinkedList<>();
    }
}
