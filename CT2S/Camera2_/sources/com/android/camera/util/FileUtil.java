package com.android.camera.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil {
    public static boolean deleteDirectoryRecursively(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }
        File[] arr$ = directory.listFiles();
        for (File entry : arr$) {
            if (entry.isDirectory()) {
                deleteDirectoryRecursively(entry);
            }
            if (!entry.delete()) {
                return false;
            }
        }
        return directory.delete();
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        int length = (int) file.length();
        byte[] data = new byte[length];
        FileInputStream stream = new FileInputStream(file);
        for (int offset = 0; offset < length; offset += stream.read(data, offset, length - offset)) {
            try {
                try {
                } catch (IOException e) {
                    throw e;
                }
            } finally {
                stream.close();
            }
        }
        return data;
    }
}
