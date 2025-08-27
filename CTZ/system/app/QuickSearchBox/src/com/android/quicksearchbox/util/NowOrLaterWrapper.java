package com.android.quicksearchbox.util;

/* loaded from: classes.dex */
public abstract class NowOrLaterWrapper<A, B> implements NowOrLater<B> {
    private final NowOrLater<A> mWrapped;

    public abstract B get(A a);

    public NowOrLaterWrapper(NowOrLater<A> nowOrLater) {
        this.mWrapped = nowOrLater;
    }

    @Override // com.android.quicksearchbox.util.NowOrLater
    public void getLater(final Consumer<? super B> consumer) {
        this.mWrapped.getLater(new Consumer<A>() { // from class: com.android.quicksearchbox.util.NowOrLaterWrapper.1
            /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.quicksearchbox.util.Consumer */
            /* JADX WARN: Multi-variable type inference failed */
            @Override // com.android.quicksearchbox.util.Consumer
            public boolean consume(A a) {
                return consumer.consume(NowOrLaterWrapper.this.get(a));
            }
        });
    }

    @Override // com.android.quicksearchbox.util.NowOrLater
    public B getNow() {
        return get(this.mWrapped.getNow());
    }

    @Override // com.android.quicksearchbox.util.NowOrLater
    public boolean haveNow() {
        return this.mWrapped.haveNow();
    }
}
