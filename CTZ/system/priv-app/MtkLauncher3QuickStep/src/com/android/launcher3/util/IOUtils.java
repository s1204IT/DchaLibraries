package com.android.launcher3.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class IOUtils {
    private static final int BUF_SIZE = 4096;

    /* JADX WARN: Removed duplicated region for block: B:16:0x001e  */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0015 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static byte[] toByteArray(File file) throws Throwable {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] byteArray = toByteArray(fileInputStream);
            fileInputStream.close();
            return byteArray;
        } catch (Throwable th) {
            th = th;
            try {
                throw th;
            } catch (Throwable th2) {
                th = th2;
                if (th == null) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    fileInputStream.close();
                }
                throw th;
            }
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
                j += i;
            } else {
                return j;
            }
        }
    }
}
