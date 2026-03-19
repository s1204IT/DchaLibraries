package com.android.server.wm;

import java.util.ArrayList;

class WindowList<E> extends ArrayList<E> {
    WindowList() {
    }

    void addFirst(E e) {
        add(0, e);
    }

    E peekLast() {
        if (size() > 0) {
            return get(size() - 1);
        }
        return null;
    }

    E peekFirst() {
        if (size() > 0) {
            return get(0);
        }
        return null;
    }
}
