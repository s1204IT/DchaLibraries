package com.android.quicksearchbox.util;

public abstract class NowOrLaterWrapper<A, B> implements NowOrLater<B> {
    private final NowOrLater<A> mWrapped;

    public NowOrLaterWrapper(NowOrLater<A> nowOrLater) {
        this.mWrapped = nowOrLater;
    }

    public abstract B get(A a);

    @Override
    public void getLater(Consumer<? super B> consumer) {
        this.mWrapped.getLater((Consumer<? super A>) new Consumer<A>(this, consumer) {
            final NowOrLaterWrapper this$0;
            final Consumer val$consumer;

            {
                this.this$0 = this;
                this.val$consumer = consumer;
            }

            @Override
            public boolean consume(A a) {
                return this.val$consumer.consume(this.this$0.get(a));
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
