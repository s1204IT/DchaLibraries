package com.android.contacts;

import android.content.Context;
import java.util.Iterator;
import java.util.List;

public final class Collapser {
    private static final int MAX_LISTSIZE_TO_COLLAPSE = 20;

    public interface Collapsible<T> {
        void collapseWith(T t);

        boolean shouldCollapseWith(T t, Context context);
    }

    private Collapser() {
    }

    public static <T extends Collapsible<T>> void collapseList(List<T> list, Context context) {
        int size = list.size();
        if (size > MAX_LISTSIZE_TO_COLLAPSE) {
            return;
        }
        for (int i = 0; i < size; i++) {
            T t = list.get(i);
            if (t != null) {
                int i2 = i + 1;
                while (true) {
                    if (i2 >= size) {
                        break;
                    }
                    T t2 = list.get(i2);
                    if (t2 != null) {
                        if (t.shouldCollapseWith(t2, context)) {
                            t.collapseWith(t2);
                            list.set(i2, null);
                        } else if (t2.shouldCollapseWith(t, context)) {
                            t2.collapseWith(t);
                            list.set(i, null);
                            break;
                        }
                    }
                    i2++;
                }
            }
        }
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
    }
}
