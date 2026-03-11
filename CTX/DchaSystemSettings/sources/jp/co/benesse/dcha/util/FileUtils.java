package jp.co.benesse.dcha.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class FileUtils {
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
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            return false;
        }
        return true;
    }
}
