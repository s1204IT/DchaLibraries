package com.android.quicksearchbox.util;

/* loaded from: classes.dex */
public class Now<C> implements NowOrLater<C> {
    private final C mValue;

    public Now(C c) {
        this.mValue = c;
    }

    @Override // com.android.quicksearchbox.util.NowOrLater
    public void getLater(Consumer<? super C> consumer) {
        consumer.consume(getNow());
    }

    @Override // com.android.quicksearchbox.util.NowOrLater
    public C getNow() {
        return this.mValue;
    }

    @Override // com.android.quicksearchbox.util.NowOrLater
    public boolean haveNow() {
        return true;
    }
}
