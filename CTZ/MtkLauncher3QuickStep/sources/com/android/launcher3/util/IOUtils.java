package com.android.launcher3.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
    private static final int BUF_SIZE = 4096;

    public static byte[] toByteArray(File file) throws Throwable {
        Throwable th;
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] byteArray = toByteArray(fileInputStream);
            fileInputStream.close();
            return byteArray;
        } catch (Throwable th2) {
            th = th2;
            th = null;
            if (th == null) {
            }
            throw th;
        }
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[4096];
        long j = 0;
        while (true) {
            int i = inputStream.read(bArr);
            if (i != -1) {
                outputStream.write(bArr, 0, i);
                j += (long) i;
            } else {
                return j;
            }
        }
    }
}
