package com.android.timezone.distro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public final class FileUtils {
    private FileUtils() {
    }

    public static File createSubFile(File file, String str) throws IOException {
        File canonicalFile = new File(file, str).getCanonicalFile();
        if (!canonicalFile.getPath().startsWith(file.getCanonicalPath())) {
            throw new IOException(str + " must exist beneath " + file + ". Canonicalized subpath: " + canonicalFile);
        }
        return canonicalFile;
    }

    public static void ensureDirectoriesExist(File file, boolean z) throws IOException {
        LinkedList<File> linkedList = new LinkedList();
        File parentFile = file;
        do {
            linkedList.addFirst(parentFile);
            parentFile = parentFile.getParentFile();
        } while (parentFile != null);
        for (File file2 : linkedList) {
            if (!file2.exists()) {
                if (!file2.mkdir()) {
                    throw new IOException("Unable to create directory: " + file);
                }
                if (z) {
                    makeDirectoryWorldAccessible(file2);
                }
            } else if (!file2.isDirectory()) {
                throw new IOException(file2 + " exists but is not a directory");
            }
        }
    }

    public static void makeDirectoryWorldAccessible(File file) throws IOException {
        if (!file.isDirectory()) {
            throw new IOException(file + " must be a directory");
        }
        makeWorldReadable(file);
        if (!file.setExecutable(true, false)) {
            throw new IOException("Unable to make " + file + " world-executable");
        }
    }

    public static void makeWorldReadable(File file) throws IOException {
        if (!file.setReadable(true, false)) {
            throw new IOException("Unable to make " + file + " world-readable");
        }
    }

    public static void rename(File file, File file2) throws IOException {
        ensureFileDoesNotExist(file2);
        if (!file.renameTo(file2)) {
            throw new IOException("Unable to rename " + file + " to " + file2);
        }
    }

    public static void ensureFileDoesNotExist(File file) throws IOException {
        if (file.exists()) {
            if (!file.isFile()) {
                throw new IOException(file + " is not a file");
            }
            doDelete(file);
        }
    }

    public static void doDelete(File file) throws IOException {
        if (!file.delete()) {
            throw new IOException("Unable to delete: " + file);
        }
    }

    public static boolean isSymlink(File file) throws IOException {
        return !file.getCanonicalPath().equals(new File(file.getParentFile().getCanonicalFile(), file.getName()).getPath());
    }

    public static void deleteRecursive(File file) throws IOException {
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                if (file2.isDirectory() && !isSymlink(file2)) {
                    deleteRecursive(file2);
                } else {
                    doDelete(file2);
                }
            }
            String[] list = file.list();
            if (list.length != 0) {
                throw new IOException("Unable to delete files: " + Arrays.toString(list));
            }
        }
        doDelete(file);
    }

    public static boolean filesExist(File file, String... strArr) {
        for (String str : strArr) {
            if (!new File(file, str).exists()) {
                return false;
            }
        }
        return true;
    }

    public static byte[] readBytes(File file, int i) throws IOException {
        if (i <= 0) {
            throw new IllegalArgumentException("maxBytes ==" + i);
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        Throwable th = null;
        try {
            byte[] bArr = new byte[i];
            int i2 = fileInputStream.read(bArr, 0, i);
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, 0, bArr2, 0, i2);
            fileInputStream.close();
            return bArr2;
        } catch (Throwable th2) {
            if (th != null) {
                try {
                    fileInputStream.close();
                } catch (Throwable th3) {
                    th.addSuppressed(th3);
                }
            } else {
                fileInputStream.close();
            }
            throw th2;
        }
    }

    public static void createEmptyFile(File file) throws IOException {
        new FileOutputStream(file, false).close();
    }
}
