package sun.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaIndex {
    static final boolean $assertionsDisabled = false;
    private static volatile Map<File, MetaIndex> jarMap;
    private String[] contents;
    private boolean isClassOnlyJar;

    public static MetaIndex forJar(File file) {
        return getJarMap().get(file);
    }

    public static synchronized void registerDirectory(File file) {
        boolean z;
        File file2 = new File(file, "meta-index");
        if (file2.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file2));
                String strSubstring = null;
                ArrayList arrayList = new ArrayList();
                Map<File, MetaIndex> jarMap2 = getJarMap();
                File canonicalFile = file.getCanonicalFile();
                String line = bufferedReader.readLine();
                if (line != null && line.equals("% VERSION 2")) {
                    loop0: while (true) {
                        z = false;
                        while (true) {
                            String line2 = bufferedReader.readLine();
                            if (line2 == null) {
                                break loop0;
                            }
                            char cCharAt = line2.charAt(0);
                            if (cCharAt != '!' && cCharAt != '#') {
                                if (cCharAt != '%') {
                                    if (cCharAt != '@') {
                                        arrayList.add(line2);
                                    }
                                }
                            }
                            if (strSubstring != null && arrayList.size() > 0) {
                                jarMap2.put(new File(canonicalFile, strSubstring), new MetaIndex(arrayList, z));
                                arrayList.clear();
                            }
                            strSubstring = line2.substring(2);
                            if (line2.charAt(0) == '!') {
                                z = true;
                            } else if (z) {
                                break;
                            }
                        }
                    }
                    if (strSubstring != null && arrayList.size() > 0) {
                        jarMap2.put(new File(canonicalFile, strSubstring), new MetaIndex(arrayList, z));
                    }
                    bufferedReader.close();
                }
                bufferedReader.close();
            } catch (IOException e) {
            }
        }
    }

    public boolean mayContain(String str) {
        if (this.isClassOnlyJar && !str.endsWith(".class")) {
            return false;
        }
        for (String str2 : this.contents) {
            if (str.startsWith(str2)) {
                return true;
            }
        }
        return false;
    }

    private MetaIndex(List<String> list, boolean z) throws IllegalArgumentException {
        if (list == null) {
            throw new IllegalArgumentException();
        }
        this.contents = (String[]) list.toArray(new String[0]);
        this.isClassOnlyJar = z;
    }

    private static Map<File, MetaIndex> getJarMap() {
        if (jarMap == null) {
            synchronized (MetaIndex.class) {
                if (jarMap == null) {
                    jarMap = new HashMap();
                }
            }
        }
        return jarMap;
    }
}
