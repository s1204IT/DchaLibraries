package com.android.printspooler.model;

import android.graphics.drawable.Icon;
import android.print.PrinterId;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

public class CustomPrinterIconCache {
    private final File mCacheDirectory;

    public CustomPrinterIconCache(File file) {
        this.mCacheDirectory = new File(file, "icons");
        if (!this.mCacheDirectory.exists()) {
            this.mCacheDirectory.mkdir();
        }
    }

    private File getIconFileName(PrinterId printerId) {
        StringBuffer stringBuffer = new StringBuffer(printerId.getServiceName().getPackageName());
        stringBuffer.append("-");
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update((printerId.getServiceName().getClassName() + ":" + printerId.getLocalId()).getBytes("UTF-16"));
            stringBuffer.append(String.format("%#040x", new BigInteger(1, messageDigest.digest())));
            return new File(this.mCacheDirectory, stringBuffer.toString());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Log.e("CustomPrinterIconCache", "Could not compute custom printer icon file name", e);
            return null;
        }
    }

    public synchronized Icon getIcon(PrinterId printerId) {
        Icon icon;
        FileInputStream fileInputStream;
        Throwable th;
        File iconFileName = getIconFileName(printerId);
        icon = null;
        if (iconFileName != null && iconFileName.exists()) {
            try {
                fileInputStream = new FileInputStream(iconFileName);
            } catch (IOException e) {
                Log.e("CustomPrinterIconCache", "Could not read icon from " + iconFileName, e);
            }
            try {
                Icon iconCreateFromStream = Icon.createFromStream(fileInputStream);
                $closeResource(null, fileInputStream);
                icon = iconCreateFromStream;
                iconFileName.setLastModified(System.currentTimeMillis());
            } catch (Throwable th2) {
                th = th2;
                th = null;
                $closeResource(th, fileInputStream);
                throw th;
            }
        }
        return icon;
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public void removeOldFiles(int i) {
        File[] fileArrListFiles = this.mCacheDirectory.listFiles();
        if (fileArrListFiles.length > i * 2) {
            TreeMap treeMap = new TreeMap();
            for (File file : fileArrListFiles) {
                treeMap.put(Long.valueOf(file.lastModified()), file);
            }
            while (treeMap.size() > i) {
                treeMap.remove(treeMap.firstKey());
            }
        }
    }

    public synchronized void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) {
        File iconFileName = getIconFileName(printerId);
        if (iconFileName == null) {
            return;
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(iconFileName);
            try {
                icon.writeToStream(fileOutputStream);
            } finally {
                $closeResource(null, fileOutputStream);
            }
        } catch (IOException e) {
            Log.e("CustomPrinterIconCache", "Could not write icon for " + printerId + " to storage", e);
        }
        removeOldFiles(1024);
    }

    public synchronized void clear() {
        for (File file : this.mCacheDirectory.listFiles()) {
            file.delete();
        }
    }
}
