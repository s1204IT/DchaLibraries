package com.android.quicksearchbox.util;

public interface NowOrLater<C> {
    void getLater(Consumer<? super C> consumer);

    C getNow();

    boolean haveNow();
}
