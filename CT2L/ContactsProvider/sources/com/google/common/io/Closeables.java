package com.google.common.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Closeables {
    static final Logger logger = Logger.getLogger(Closeables.class.getName());

    private Closeables() {
    }

    public static void close(Closeable closeable, boolean swallowIOException) throws IOException {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (swallowIOException) {
                    logger.log(Level.WARNING, "IOException thrown while closing Closeable.", (Throwable) e);
                    return;
                }
                throw e;
            }
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            close(closeable, true);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException should not have been thrown.", (Throwable) e);
        }
    }
}
