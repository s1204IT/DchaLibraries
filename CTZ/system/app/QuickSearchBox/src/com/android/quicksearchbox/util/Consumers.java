package com.android.quicksearchbox.util;

import android.os.Handler;

/* loaded from: classes.dex */
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

    public static <A extends QuietlyCloseable> void consumeCloseableAsync(Handler handler, final Consumer<A> consumer, final A a) {
        if (handler == null) {
            consumeCloseable(consumer, a);
        } else {
            handler.post(new Runnable() { // from class: com.android.quicksearchbox.util.Consumers.2
                @Override // java.lang.Runnable
                public void run() {
                    Consumers.consumeCloseable(consumer, a);
                }
            });
        }
    }
}
