package com.android.quicksearchbox.util;

public abstract class NowOrLaterWrapper<A, B> implements NowOrLater<B> {
    private final NowOrLater<A> mWrapped;

    public abstract B get(A a);

    public NowOrLaterWrapper(NowOrLater<A> nowOrLater) {
        this.mWrapped = nowOrLater;
    }

    @Override
    public void getLater(final Consumer<? super B> consumer) {
        this.mWrapped.getLater((Consumer<? super A>) new Consumer<A>() {
            @Override
            public boolean consume(A a) {
                return consumer.consume(NowOrLaterWrapper.this.get(a));
            }
        });
    }

    @Override
    public B getNow() {
        return get(this.mWrapped.getNow());
    }

    @Override
    public boolean haveNow() {
        return this.mWrapped.haveNow();
    }
}
