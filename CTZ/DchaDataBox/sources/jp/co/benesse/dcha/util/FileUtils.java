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
        } catch (IOException unused) {
        }
    }

    public static boolean fileDelete(File file) {
        File[] fileArrListFiles;
        if (!file.exists()) {
            return false;
        }
        if (file.isFile()) {
            return file.delete();
        }
        if (!file.isDirectory() || (fileArrListFiles = file.listFiles()) == null) {
            return false;
        }
        for (File file2 : fileArrListFiles) {
            if (!fileDelete(file2)) {
                return false;
            }
        }
        return file.delete();
    }
}
