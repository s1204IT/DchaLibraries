package jp.co.benesse.dcha.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class FileUtils {
    private FileUtils() {
    }

    public static final void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
        }
    }

    public static final boolean canReadFile(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }
}
