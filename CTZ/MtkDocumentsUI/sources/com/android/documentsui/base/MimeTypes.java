package com.android.documentsui.base;

import java.util.List;

public final class MimeTypes {
    public static final String[] VISUAL_MIMES = {"image/*", "video/*"};

    public static String[] splitMimeType(String str) {
        String[] strArrSplit = str.split("/");
        if (strArrSplit.length != 2 || strArrSplit[0].isEmpty() || strArrSplit[1].isEmpty()) {
            return null;
        }
        return strArrSplit;
    }

    public static String findCommonMimeType(List<String> list) {
        String[] strArrSplitMimeType = splitMimeType(list.get(0));
        if (strArrSplitMimeType == null) {
            return "*/*";
        }
        int i = 1;
        while (true) {
            if (i >= list.size()) {
                break;
            }
            String[] strArrSplit = list.get(i).split("/");
            if (strArrSplit.length == 2) {
                if (!strArrSplitMimeType[1].equals(strArrSplit[1])) {
                    strArrSplitMimeType[1] = "*";
                }
                if (!strArrSplitMimeType[0].equals(strArrSplit[0])) {
                    strArrSplitMimeType[0] = "*";
                    strArrSplitMimeType[1] = "*";
                    break;
                }
            }
            i++;
        }
        return strArrSplitMimeType[0] + "/" + strArrSplitMimeType[1];
    }

    public static boolean mimeMatches(String[] strArr, String[] strArr2) {
        if (strArr2 == null) {
            return false;
        }
        for (String str : strArr2) {
            if (mimeMatches(strArr, str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mimeMatches(String[] strArr, String str) {
        if (strArr == null) {
            return true;
        }
        for (String str2 : strArr) {
            if (mimeMatches(str2, str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mimeMatches(String str, String str2) {
        if (str2 == null) {
            return false;
        }
        if (str == null || "*/*".equals(str) || str.equals(str2)) {
            return true;
        }
        if (str.endsWith("/*")) {
            return str.regionMatches(0, str2, 0, str.indexOf(47));
        }
        return false;
    }

    public static boolean isApkType(String str) {
        return "application/vnd.android.package-archive".equals(str);
    }

    public static boolean isDirectoryType(String str) {
        return "vnd.android.document/directory".equals(str);
    }
}
