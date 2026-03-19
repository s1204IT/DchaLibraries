package com.android.providers.contacts.debug;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.android.providers.contacts.util.Hex;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataExporter {
    private static String TAG = "DataExporter";

    public static Uri exportData(Context context) throws IOException {
        String str = generateRandomName() + "-contacts-db.zip";
        File outputFile = getOutputFile(context, str);
        removeDumpFiles(context);
        Log.i(TAG, "Dump started...");
        ensureOutputDirectory(context);
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
        Throwable th = null;
        try {
            try {
                zipOutputStream.setLevel(9);
                addDirectory(context, zipOutputStream, context.getFilesDir().getParentFile(), "contacts-files");
                zipOutputStream.close();
                Log.i(TAG, "Dump finished.");
                return DumpFileProvider.AUTHORITY_URI.buildUpon().appendPath(str).build();
            } finally {
            }
        } catch (Throwable th2) {
            if (th != null) {
                try {
                    zipOutputStream.close();
                } catch (Throwable th3) {
                    th.addSuppressed(th3);
                }
            } else {
                zipOutputStream.close();
            }
            throw th2;
        }
    }

    private static String generateRandomName() {
        byte[] bArr = new byte[32];
        new SecureRandom().nextBytes(bArr);
        return Hex.encodeHex(bArr, true);
    }

    public static void ensureValidFileName(String str) {
        if (str.contains("..")) {
            throw new IllegalArgumentException(".. path specifier not allowed. Bad file name: " + str);
        }
        if (!str.matches("[0-9A-Fa-f]+-contacts-db\\.zip")) {
            throw new IllegalArgumentException("Only [0-9A-Fa-f]+-contacts-db\\.zip files are supported. Bad file name: " + str);
        }
    }

    private static File getOutputDirectory(Context context) {
        return new File(context.getCacheDir(), "dumpedfiles");
    }

    private static void ensureOutputDirectory(Context context) {
        File outputDirectory = getOutputDirectory(context);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdir();
        }
    }

    public static File getOutputFile(Context context, String str) {
        return new File(getOutputDirectory(context), str);
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
                for (File file2 : file.listFiles()) {
                    removeFileOrDirectory(file2);
                }
                Log.i(TAG, "Removing " + file);
                file.delete();
            }
        }
    }

    private static void addDirectory(Context context, ZipOutputStream zipOutputStream, File file, String str) throws IOException {
        for (File file2 : file.listFiles()) {
            String str2 = str + "/" + file2.getName();
            if (file2.isDirectory()) {
                if (!file2.equals(context.getCacheDir()) && !file2.getName().equals("dumpedfiles")) {
                    addDirectory(context, zipOutputStream, file2, str2);
                }
            } else if (file2.isFile()) {
                addFile(zipOutputStream, file2, str2);
            }
        }
    }

    private static void addFile(ZipOutputStream zipOutputStream, File file, String str) throws IOException {
        Log.i(TAG, "Adding " + file.getAbsolutePath() + " ...");
        FileInputStream fileInputStream = new FileInputStream(file);
        zipOutputStream.putNextEntry(new ZipEntry(str));
        byte[] bArr = new byte[32768];
        int i = 0;
        while (true) {
            int i2 = fileInputStream.read(bArr);
            if (i2 > 0) {
                zipOutputStream.write(bArr, 0, i2);
                i += i2;
            } else {
                zipOutputStream.closeEntry();
                Log.i(TAG, "Added " + file.getAbsolutePath() + " as " + str + " (" + i + " bytes)");
                return;
            }
        }
    }
}
