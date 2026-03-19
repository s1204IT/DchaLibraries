package com.mediatek.plugin.preload;

import android.content.Context;
import com.mediatek.plugin.utils.IoUtils;
import com.mediatek.plugin.utils.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class SoOperater {
    public static final String CPU_ARMEABI = "armeabi";
    public static final String CPU_MIPS = "mips";
    public static final String CPU_X86 = "x86";
    public static final String PREFERENCE_NAME = "dynamic_load_configs";
    public static final int STEP = 1024;
    private static final String TAG = "PluginManager/SoOperater";
    private JarFile mJarFile;
    private String mSoFileName;
    private String mSoPath;
    private ZipEntry mZipEntry;
    private String mCpuName = getCpuName();
    private String mCpuArchitect = getCpuArch(this.mCpuName);

    public boolean isNewSo(Context context, String str, long j) {
        return str.contains(this.mCpuArchitect) && j == getSoLastModifiedTime(context, str);
    }

    public void copy(Context context, JarFile jarFile, ZipEntry zipEntry, String str) {
        init(jarFile, zipEntry, str);
        try {
            writeSoFile2LibDir();
            Log.d(TAG, "<copy> copy so lib success: " + this.mZipEntry.getName());
        } catch (IOException e) {
            Log.e(TAG, "<copy> copy so lib failed: ", e);
        }
        setSoLastModifiedTime(context, zipEntry.getName(), zipEntry.getTime());
    }

    private void init(JarFile jarFile, ZipEntry zipEntry, String str) {
        this.mJarFile = jarFile;
        this.mZipEntry = zipEntry;
        this.mSoFileName = parseSoFileName(zipEntry.getName());
        Log.d(TAG, "<init> mSoFileName = " + this.mSoFileName);
        this.mSoPath = str;
    }

    private final String parseSoFileName(String str) {
        return str.substring(str.lastIndexOf("/") + 1);
    }

    private void writeSoFile2LibDir() throws IOException {
        InputStream inputStream = this.mJarFile.getInputStream(this.mZipEntry);
        Log.d(TAG, "<writeSoFile2LibDir> mSoPath = " + this.mSoPath);
        Log.d(TAG, "<writeSoFile2LibDir> mSoFileName = " + this.mSoFileName);
        File file = new File(this.mSoPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(new File(file, this.mSoFileName));
        copy(inputStream, fileOutputStream);
        IoUtils.closeQuietly(fileOutputStream);
    }

    private void copy(InputStream inputStream, OutputStream outputStream) {
        if (inputStream == null || outputStream == null) {
            return;
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        try {
            int availableSize = getAvailableSize(bufferedInputStream);
            byte[] bArr = new byte[availableSize];
            while (true) {
                int i = bufferedInputStream.read(bArr, 0, availableSize);
                if (i != -1) {
                    bufferedOutputStream.write(bArr, 0, i);
                } else {
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    bufferedInputStream.close();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getAvailableSize(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return 0;
        }
        int iAvailable = inputStream.available();
        return iAvailable <= 0 ? STEP : iAvailable;
    }

    private String getCpuName() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line = bufferedReader.readLine();
            bufferedReader.close();
            if (line == null) {
                return "";
            }
            String[] strArrSplit = line.split(":\\s+", 2);
            if (strArrSplit.length >= 2) {
                return strArrSplit[1];
            }
            return "";
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e2) {
            e2.printStackTrace();
            return "";
        }
    }

    private String getCpuArch(String str) {
        if (str.toLowerCase().contains("arm")) {
            return CPU_ARMEABI;
        }
        if (str.toLowerCase().contains(CPU_X86)) {
            return CPU_X86;
        }
        if (!str.toLowerCase().contains(CPU_MIPS)) {
            return CPU_ARMEABI;
        }
        return CPU_MIPS;
    }

    private void setSoLastModifiedTime(Context context, String str, long j) {
        context.getSharedPreferences(PREFERENCE_NAME, 4).edit().putLong(str, j).apply();
    }

    private long getSoLastModifiedTime(Context context, String str) {
        return context.getSharedPreferences(PREFERENCE_NAME, 4).getLong(str, 0L);
    }
}
