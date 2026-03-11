package com.android.quicksearchbox.util;

public abstract class NowOrLaterWrapper<A, B> implements NowOrLater<B> {
    private final NowOrLater<A> mWrapped;

    public abstract B get(A a);

    public NowOrLaterWrapper(NowOrLater<A> wrapped) {
        this.mWrapped = wrapped;
    }

    @Override
    public void getLater(final Consumer<? super B> consumer) {
        this.mWrapped.getLater((Consumer<? super A>) new Consumer<A>() {
            @Override
            public boolean consume(A value) {
                return consumer.consume(NowOrLaterWrapper.this.get(value));
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
