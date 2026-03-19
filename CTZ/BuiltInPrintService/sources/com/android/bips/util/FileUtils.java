package com.android.bips.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    public static void deleteAll(File file) {
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                deleteAll(file2);
            }
        }
        file.delete();
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws Exception {
        Throwable th;
        try {
            try {
                try {
                    byte[] bArr = new byte[8092];
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i <= 0) {
                            break;
                        } else if (i > 0) {
                            outputStream.write(bArr, 0, i);
                        }
                    }
                    if (outputStream != null) {
                        $closeResource(null, outputStream);
                    }
                    if (inputStream != null) {
                        $closeResource(null, inputStream);
                    }
                } finally {
                }
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (outputStream != null) {
                }
            }
        } catch (Throwable th3) {
            if (inputStream != null) {
                $closeResource(null, inputStream);
            }
            throw th3;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static boolean makeDirectory(File file) {
        if (file.exists()) {
            return file.isDirectory();
        }
        return file.mkdir();
    }
}
