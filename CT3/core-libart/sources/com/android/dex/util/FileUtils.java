package com.android.dex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class FileUtils {
    private FileUtils() {
    }

    public static byte[] readFile(String fileName) {
        File file = new File(fileName);
        return readFile(file);
    }

    public static byte[] readFile(File file) {
        if (!file.exists()) {
            throw new RuntimeException(file + ": file not found");
        }
        if (!file.isFile()) {
            throw new RuntimeException(file + ": not a file");
        }
        if (!file.canRead()) {
            throw new RuntimeException(file + ": file not readable");
        }
        long longLength = file.length();
        int length = (int) longLength;
        if (length != longLength) {
            throw new RuntimeException(file + ": file too long");
        }
        byte[] result = new byte[length];
        try {
            FileInputStream in = new FileInputStream(file);
            int at = 0;
            while (length > 0) {
                int amt = in.read(result, at, length);
                if (amt == -1) {
                    throw new RuntimeException(file + ": unexpected EOF");
                }
                at += amt;
                length -= amt;
            }
            in.close();
            return result;
        } catch (IOException ex) {
            throw new RuntimeException(file + ": trouble reading", ex);
        }
    }

    public static boolean hasArchiveSuffix(String fileName) {
        if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
            return true;
        }
        return fileName.endsWith(".apk");
    }
}
