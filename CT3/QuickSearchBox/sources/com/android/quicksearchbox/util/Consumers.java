package com.android.quicksearchbox.util;

import android.os.Handler;

public class Consumers {
    private Consumers() {
    }

    public static <A extends QuietlyCloseable> void consumeCloseable(Consumer<A> consumer, A value) {
        boolean accepted = false;
        try {
            accepted = consumer.consume(value);
        } finally {
            if (!accepted && value != null) {
                value.close();
            }
        }
    }

    public static <A extends QuietlyCloseable> void consumeCloseableAsync(Handler handler, final Consumer<A> consumer, final A value) {
        if (handler == null) {
            consumeCloseable(consumer, value);
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Consumers.consumeCloseable(consumer, value);
                }
            });
        }
    }
}
