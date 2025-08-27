package com.android.quicksearchbox.util;

/* loaded from: classes.dex */
public interface NowOrLater<C> {
    void getLater(Consumer<? super C> consumer);

    C getNow();

    boolean haveNow();
}
