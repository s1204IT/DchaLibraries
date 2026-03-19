package libcore.tzdata.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

public final class FileUtils {
    private FileUtils() {
    }

    public static File createSubFile(File parentDir, String name) throws IOException {
        File subFile = new File(parentDir, name).getCanonicalFile();
        if (!subFile.getPath().startsWith(parentDir.getCanonicalPath())) {
            throw new IOException(name + " must exist beneath " + parentDir + ". Canonicalized subpath: " + subFile);
        }
        return subFile;
    }

    public static void ensureDirectoriesExist(File dir, boolean makeWorldReadable) throws IOException {
        LinkedList<File> dirs = new LinkedList<>();
        File currentDir = dir;
        do {
            dirs.addFirst(currentDir);
            currentDir = currentDir.getParentFile();
        } while (currentDir != null);
        for (File dirToCheck : dirs) {
            if (!dirToCheck.exists()) {
                if (!dirToCheck.mkdir()) {
                    throw new IOException("Unable to create directory: " + dir);
                }
                if (makeWorldReadable) {
                    makeDirectoryWorldAccessible(dirToCheck);
                }
            } else if (!dirToCheck.isDirectory()) {
                throw new IOException(dirToCheck + " exists but is not a directory");
            }
        }
    }

    public static void makeDirectoryWorldAccessible(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException(directory + " must be a directory");
        }
        makeWorldReadable(directory);
        if (directory.setExecutable(true, false)) {
        } else {
            throw new IOException("Unable to make " + directory + " world-executable");
        }
    }

    public static void makeWorldReadable(File file) throws IOException {
        if (file.setReadable(true, false)) {
        } else {
            throw new IOException("Unable to make " + file + " world-readable");
        }
    }

    public static long calculateChecksum(File file) throws Throwable {
        FileInputStream fis;
        Throwable th = null;
        CRC32 crc32 = new CRC32();
        FileInputStream fileInputStream = null;
        try {
            fis = new FileInputStream(file);
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            byte[] buffer = new byte[8196];
            while (true) {
                int count = fis.read(buffer);
                if (count == -1) {
                    break;
                }
                crc32.update(buffer, 0, count);
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (th != null) {
                throw th;
            }
            return crc32.getValue();
        } catch (Throwable th4) {
            th = th4;
            fileInputStream = fis;
            if (fileInputStream != null) {
            }
            if (th == null) {
            }
        }
    }

    public static void rename(File from, File to) throws IOException {
        ensureFileDoesNotExist(to);
        if (from.renameTo(to)) {
        } else {
            throw new IOException("Unable to rename " + from + " to " + to);
        }
    }

    public static void ensureFileDoesNotExist(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (!file.isFile()) {
            throw new IOException(file + " is not a file");
        }
        doDelete(file);
    }

    public static void doDelete(File file) throws IOException {
        if (file.delete()) {
        } else {
            throw new IOException("Unable to delete: " + file);
        }
    }

    public static boolean isSymlink(File file) throws IOException {
        String baseName = file.getName();
        String canonicalPathExceptBaseName = new File(file.getParentFile().getCanonicalFile(), baseName).getPath();
        return !file.getCanonicalPath().equals(canonicalPathExceptBaseName);
    }

    public static void deleteRecursive(File toDelete) throws IOException {
        if (toDelete.isDirectory()) {
            for (File file : toDelete.listFiles()) {
                if (file.isDirectory() && !isSymlink(file)) {
                    deleteRecursive(file);
                } else {
                    doDelete(file);
                }
            }
            String[] remainingFiles = toDelete.list();
            if (remainingFiles.length != 0) {
                throw new IOException("Unable to delete files: " + Arrays.toString(remainingFiles));
            }
        }
        doDelete(toDelete);
    }

    public static boolean filesExist(File rootDir, String... fileNames) throws IOException {
        for (String fileName : fileNames) {
            File file = new File(rootDir, fileName);
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }

    public static List<String> readLines(File file) throws Throwable {
        BufferedReader fileReader;
        Throwable th = null;
        FileInputStream in = new FileInputStream(file);
        BufferedReader bufferedReader = null;
        try {
            fileReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            List<String> lines = new ArrayList<>();
            while (true) {
                String line = fileReader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (th != null) {
                throw th;
            }
            return lines;
        } catch (Throwable th4) {
            th = th4;
            bufferedReader = fileReader;
            if (bufferedReader != null) {
            }
            if (th == null) {
            }
        }
    }
}
