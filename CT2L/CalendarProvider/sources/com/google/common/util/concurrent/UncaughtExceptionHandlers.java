package com.google.common.util.concurrent;

import java.lang.Thread;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UncaughtExceptionHandlers {

    static final class Exiter implements Thread.UncaughtExceptionHandler {
        private static final Logger logger = Logger.getLogger(Exiter.class.getName());
        private final Runtime runtime;

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.log(Level.SEVERE, String.format("Caught an exception in %s.  Shutting down.", t), e);
            this.runtime.exit(1);
        }
    }
}
