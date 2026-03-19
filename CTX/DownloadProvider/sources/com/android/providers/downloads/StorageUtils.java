package com.android.providers.downloads;

import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import com.google.android.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class StorageUtils {
    static List<ConcreteFile> listFilesRecursive(File file, String str, int i) {
        File[] fileArrListFiles;
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        LinkedList linkedList = new LinkedList();
        linkedList.add(file);
        while (!linkedList.isEmpty()) {
            File file2 = (File) linkedList.removeFirst();
            if (!Objects.equals(file2.getName(), str) && (fileArrListFiles = file2.listFiles()) != null) {
                for (File file3 : fileArrListFiles) {
                    if (file3.isDirectory()) {
                        linkedList.add(file3);
                    } else if (file3.isFile()) {
                        try {
                            ConcreteFile concreteFile = new ConcreteFile(file3);
                            if (i == -1 || concreteFile.stat.st_uid == i) {
                                arrayListNewArrayList.add(concreteFile);
                            }
                        } catch (ErrnoException e) {
                        }
                    }
                }
            }
        }
        return arrayListNewArrayList;
    }

    static class ConcreteFile {
        public final File file;
        public final StructStat stat;

        public ConcreteFile(File file) throws ErrnoException {
            this.file = file;
            this.stat = Os.lstat(file.getAbsolutePath());
        }

        public int hashCode() {
            return (31 * (((int) (this.stat.st_dev ^ (this.stat.st_dev >>> 32))) + 31)) + ((int) (this.stat.st_ino ^ (this.stat.st_ino >>> 32)));
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ConcreteFile)) {
                return false;
            }
            ConcreteFile concreteFile = (ConcreteFile) obj;
            return concreteFile.stat.st_dev == this.stat.st_dev && concreteFile.stat.st_ino == this.stat.st_ino;
        }
    }
}
