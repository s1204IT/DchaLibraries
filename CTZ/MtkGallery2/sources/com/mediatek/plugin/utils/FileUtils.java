package com.mediatek.plugin.utils;

import java.io.File;
import java.util.ArrayList;

public class FileUtils {
    public static ArrayList<String> getAllFile(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (str == null || str.equals("")) {
            return arrayList;
        }
        File file = new File(str);
        if (!file.isDirectory()) {
            return arrayList;
        }
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(file);
        for (int i = 0; i < arrayList2.size(); i++) {
            File file2 = (File) arrayList2.get(i);
            if (file2.isFile()) {
                arrayList.add(file2.getAbsolutePath());
            } else {
                File[] fileArrListFiles = file2.listFiles();
                if (fileArrListFiles != null) {
                    for (File file3 : fileArrListFiles) {
                        arrayList2.add(file3);
                    }
                }
            }
        }
        return arrayList;
    }
}
