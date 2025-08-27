package com.android.quicksearchbox.util;

/* loaded from: classes.dex */
public class NoOpConsumer<A> implements Consumer<A> {
    @Override // com.android.quicksearchbox.util.Consumer
    public boolean consume(A a) {
        return false;
    }
}
