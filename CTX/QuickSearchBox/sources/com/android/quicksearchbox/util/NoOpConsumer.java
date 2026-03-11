package com.android.quicksearchbox.util;

public class NoOpConsumer<A> implements Consumer<A> {
    @Override
    public boolean consume(A a) {
        return false;
    }
}
