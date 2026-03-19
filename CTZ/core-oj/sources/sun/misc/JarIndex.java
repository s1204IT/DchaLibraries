package sun.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.AccessController;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sun.security.action.GetPropertyAction;

public class JarIndex {
    public static final String INDEX_NAME = "META-INF/INDEX.LIST";
    private static final boolean metaInfFilenames = "true".equals(AccessController.doPrivileged(new GetPropertyAction("sun.misc.JarIndex.metaInfFilenames")));
    private HashMap<String, LinkedList<String>> indexMap;
    private String[] jarFiles;
    private HashMap<String, LinkedList<String>> jarMap;

    public JarIndex() {
        this.indexMap = new HashMap<>();
        this.jarMap = new HashMap<>();
    }

    public JarIndex(InputStream inputStream) throws IOException {
        this();
        read(inputStream);
    }

    public JarIndex(String[] strArr) throws IOException {
        this();
        this.jarFiles = strArr;
        parseJars(strArr);
    }

    public static JarIndex getJarIndex(JarFile jarFile) throws IOException {
        return getJarIndex(jarFile, null);
    }

    public static JarIndex getJarIndex(JarFile jarFile, MetaIndex metaIndex) throws IOException {
        JarEntry jarEntry;
        if ((metaIndex != null && !metaIndex.mayContain(INDEX_NAME)) || (jarEntry = jarFile.getJarEntry(INDEX_NAME)) == null) {
            return null;
        }
        return new JarIndex(jarFile.getInputStream(jarEntry));
    }

    public String[] getJarFiles() {
        return this.jarFiles;
    }

    private void addToList(String str, String str2, HashMap<String, LinkedList<String>> map) {
        LinkedList<String> linkedList = map.get(str);
        if (linkedList == null) {
            LinkedList<String> linkedList2 = new LinkedList<>();
            linkedList2.add(str2);
            map.put(str, linkedList2);
        } else if (!linkedList.contains(str2)) {
            linkedList.add(str2);
        }
    }

    public LinkedList<String> get(String str) {
        int iLastIndexOf;
        LinkedList<String> linkedList = this.indexMap.get(str);
        if (linkedList == null && (iLastIndexOf = str.lastIndexOf("/")) != -1) {
            return this.indexMap.get(str.substring(0, iLastIndexOf));
        }
        return linkedList;
    }

    public void add(String str, String str2) {
        int iLastIndexOf = str.lastIndexOf("/");
        if (iLastIndexOf != -1) {
            str = str.substring(0, iLastIndexOf);
        }
        addMapping(str, str2);
    }

    private void addMapping(String str, String str2) {
        addToList(str, str2, this.indexMap);
        addToList(str2, str, this.jarMap);
    }

    private void parseJars(String[] strArr) throws IOException {
        if (strArr == null) {
            return;
        }
        for (String str : strArr) {
            ZipFile zipFile = new ZipFile(str.replace('/', File.separatorChar));
            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
            while (enumerationEntries.hasMoreElements()) {
                ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                String name = zipEntryNextElement.getName();
                if (!name.equals("META-INF/") && !name.equals(INDEX_NAME) && !name.equals(JarFile.MANIFEST_NAME)) {
                    if (!metaInfFilenames || !name.startsWith("META-INF/")) {
                        add(name, str);
                    } else if (!zipEntryNextElement.isDirectory()) {
                        addMapping(name, str);
                    }
                }
            }
            zipFile.close();
        }
    }

    public void write(OutputStream outputStream) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF8"));
        bufferedWriter.write("JarIndex-Version: 1.0\n\n");
        if (this.jarFiles != null) {
            for (int i = 0; i < this.jarFiles.length; i++) {
                String str = this.jarFiles[i];
                bufferedWriter.write(str + "\n");
                LinkedList<String> linkedList = this.jarMap.get(str);
                if (linkedList != null) {
                    Iterator<String> it = linkedList.iterator();
                    while (it.hasNext()) {
                        bufferedWriter.write(it.next() + "\n");
                    }
                }
                bufferedWriter.write("\n");
            }
            bufferedWriter.flush();
        }
    }

    public void read(InputStream inputStream) throws IOException {
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
        Vector vector = new Vector();
        do {
            line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
        } while (!line.endsWith(".jar"));
        String str = null;
        while (line != null) {
            if (line.length() != 0) {
                if (line.endsWith(".jar")) {
                    vector.add(line);
                    str = line;
                } else {
                    addMapping(line, str);
                }
            }
            line = bufferedReader.readLine();
        }
        this.jarFiles = (String[]) vector.toArray(new String[vector.size()]);
    }

    public void merge(JarIndex jarIndex, String str) {
        for (Map.Entry<String, LinkedList<String>> entry : this.indexMap.entrySet()) {
            String key = entry.getKey();
            for (String strConcat : entry.getValue()) {
                if (str != null) {
                    strConcat = str.concat(strConcat);
                }
                jarIndex.addMapping(key, strConcat);
            }
        }
    }
}
