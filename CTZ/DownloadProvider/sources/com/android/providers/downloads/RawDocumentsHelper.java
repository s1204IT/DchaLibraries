package com.android.providers.downloads;

import java.io.File;

public class RawDocumentsHelper {
    public static boolean isRawDocId(String str) {
        return str != null && str.startsWith("raw:");
    }

    public static String getDocIdForFile(File file) {
        return "raw:" + file.getAbsolutePath();
    }

    public static String getAbsoluteFilePath(String str) {
        return str.substring("raw:".length());
    }
}
