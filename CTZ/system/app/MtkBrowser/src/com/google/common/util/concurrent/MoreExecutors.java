package com.google.common.util.concurrent;

/* loaded from: classes.dex */
public final class MoreExecutors {

    static class Application {
        Application() {
        }

        void addShutdownHook(Thread thread) {
            Runtime.getRuntime().addShutdownHook(thread);
        }
    }
}
