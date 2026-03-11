package com.android.quicksearchbox.util;

import android.os.Handler;

public class Consumers {
    public static <A extends QuietlyCloseable> void consumeCloseable(Consumer<A> consumer, A a) {
        try {
            if (consumer.consume(a) || a == null) {
            }
        } finally {
            if (a != null) {
                a.close();
            }
        }
    }

    public static <A extends QuietlyCloseable> void consumeCloseableAsync(Handler handler, Consumer<A> consumer, A a) {
        if (handler == null) {
            consumeCloseable(consumer, a);
        } else {
            handler.post(new Runnable(consumer, a) {
                final Consumer val$consumer;
                final QuietlyCloseable val$value;

                {
                    this.val$consumer = consumer;
                    this.val$value = a;
                }

                @Override
                public void run() {
                    Consumers.consumeCloseable(this.val$consumer, this.val$value);
                }
            });
        }
    }
}
