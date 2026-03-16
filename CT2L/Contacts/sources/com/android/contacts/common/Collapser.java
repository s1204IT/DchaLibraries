package com.android.contacts.common;

import android.content.Context;
import java.util.Iterator;
import java.util.List;

public final class Collapser {

    public interface Collapsible<T> {
        void collapseWith(T t);

        boolean shouldCollapseWith(T t, Context context);
    }

    public static <T extends Collapsible<T>> void collapseList(List<T> list, Context context) {
        int listSize = list.size();
        if (listSize <= 20) {
            for (int i = 0; i < listSize; i++) {
                T iItem = list.get(i);
                if (iItem != null) {
                    int j = i + 1;
                    while (true) {
                        if (j >= listSize) {
                            break;
                        }
                        T jItem = list.get(j);
                        if (jItem != null) {
                            if (iItem.shouldCollapseWith(jItem, context)) {
                                iItem.collapseWith(jItem);
                                list.set(j, null);
                            } else if (jItem.shouldCollapseWith(iItem, context)) {
                                jItem.collapseWith(iItem);
                                list.set(i, null);
                                break;
                            }
                        }
                        j++;
                    }
                }
            }
            Iterator<T> itr = list.iterator();
            while (itr.hasNext()) {
                if (itr.next() == null) {
                    itr.remove();
                }
            }
        }
    }
}
