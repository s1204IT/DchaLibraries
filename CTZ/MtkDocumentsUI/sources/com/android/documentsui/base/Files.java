package com.android.documentsui.base;

import java.io.File;

public final class Files {
    public static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                deleteRecursively(file2);
            }
        }
        file.delete();
    }
}
