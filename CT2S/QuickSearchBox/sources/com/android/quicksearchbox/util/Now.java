package com.android.quicksearchbox.util;

public class Now<C> implements NowOrLater<C> {
    private final C mValue;

    public Now(C value) {
        this.mValue = value;
    }

    @Override
    public void getLater(Consumer<? super C> consumer) {
        consumer.consume(getNow());
    }

    @Override
    public C getNow() {
        return this.mValue;
    }

    @Override
    public boolean haveNow() {
        return true;
    }
}
