package com.android.providers.contacts.debug;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.android.providers.contacts.util.Hex;
import com.google.common.io.Closeables;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataExporter {
    private static String TAG = "DataExporter";

    public static Uri exportData(Context context) throws IOException {
        String fileName = generateRandomName() + "-contacts-db.zip";
        File outFile = getOutputFile(context, fileName);
        removeDumpFiles(context);
        Log.i(TAG, "Dump started...");
        ensureOutputDirectory(context);
        ZipOutputStream os = new ZipOutputStream(new FileOutputStream(outFile));
        os.setLevel(9);
        try {
            addDirectory(context, os, context.getFilesDir().getParentFile(), "contacts-files");
            Closeables.closeQuietly(os);
            Log.i(TAG, "Dump finished.");
            return DumpFileProvider.AUTHORITY_URI.buildUpon().appendPath(fileName).build();
        } catch (Throwable th) {
            Closeables.closeQuietly(os);
            throw th;
        }
    }

    private static String generateRandomName() {
        SecureRandom rng = new SecureRandom();
        byte[] random = new byte[32];
        rng.nextBytes(random);
        return Hex.encodeHex(random, true);
    }

    public static void ensureValidFileName(String fileName) {
        if (fileName.contains("..")) {
            throw new IllegalArgumentException(".. path specifier not allowed. Bad file name: " + fileName);
        }
        if (!fileName.matches("[0-9A-Fa-f]+-contacts-db\\.zip")) {
            throw new IllegalArgumentException("Only [0-9A-Fa-f]+-contacts-db\\.zip files are supported. Bad file name: " + fileName);
        }
    }

    private static File getOutputDirectory(Context context) {
        return new File(context.getCacheDir(), "dumpedfiles");
    }

    private static void ensureOutputDirectory(Context context) {
        File directory = getOutputDirectory(context);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static File getOutputFile(Context context, String fileName) {
        return new File(getOutputDirectory(context), fileName);
    }

    public static boolean dumpFileExists(Context context) {
        return getOutputDirectory(context).exists();
    }

    public static void removeDumpFiles(Context context) {
        removeFileOrDirectory(getOutputDirectory(context));
    }

    private static void removeFileOrDirectory(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                Log.i(TAG, "Removing " + file);
                file.delete();
                return;
            }
            if (file.isDirectory()) {
                File[] arr$ = file.listFiles();
                for (File child : arr$) {
                    removeFileOrDirectory(child);
                }
                Log.i(TAG, "Removing " + file);
                file.delete();
            }
        }
    }

    private static void addDirectory(Context context, ZipOutputStream os, File current, String storedPath) throws IOException {
        File[] arr$ = current.listFiles();
        for (File child : arr$) {
            String childStoredPath = storedPath + "/" + child.getName();
            if (child.isDirectory()) {
                if (!child.equals(context.getCacheDir()) && !child.getName().equals("dumpedfiles")) {
                    addDirectory(context, os, child, childStoredPath);
                }
            } else if (child.isFile()) {
                addFile(os, child, childStoredPath);
            }
        }
    }

    private static void addFile(ZipOutputStream os, File current, String storedPath) throws IOException {
        Log.i(TAG, "Adding " + current.getAbsolutePath() + " ...");
        InputStream is = new FileInputStream(current);
        os.putNextEntry(new ZipEntry(storedPath));
        byte[] buf = new byte[32768];
        int totalLen = 0;
        while (true) {
            int len = is.read(buf);
            if (len > 0) {
                os.write(buf, 0, len);
                totalLen += len;
            } else {
                os.closeEntry();
                Log.i(TAG, "Added " + current.getAbsolutePath() + " as " + storedPath + " (" + totalLen + " bytes)");
                return;
            }
        }
    }
}
