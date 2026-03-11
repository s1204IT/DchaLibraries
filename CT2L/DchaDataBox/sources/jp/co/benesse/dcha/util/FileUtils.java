package jp.co.benesse.dcha.util;

import java.io.Closeable;
import java.io.IOException;

public class FileUtils {
    private FileUtils() {
    }

    public static final void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
