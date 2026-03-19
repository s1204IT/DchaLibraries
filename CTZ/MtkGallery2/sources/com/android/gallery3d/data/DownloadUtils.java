package com.android.gallery3d.data;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URL;

public class DownloadUtils {
    public static boolean requestDownload(ThreadPool.JobContext jobContext, URL url, File file) throws Throwable {
        FileOutputStream fileOutputStream;
        FileOutputStream fileOutputStream2 = null;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (Throwable th) {
            th = th;
        }
        try {
            boolean zDownload = download(jobContext, url, fileOutputStream);
            Utils.closeSilently(fileOutputStream);
            return zDownload;
        } catch (Throwable th2) {
            th = th2;
            fileOutputStream2 = fileOutputStream;
            Utils.closeSilently(fileOutputStream2);
            throw th;
        }
    }

    public static void dump(ThreadPool.JobContext jobContext, InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[4096];
        int i = inputStream.read(bArr, 0, bArr.length);
        final Thread threadCurrentThread = Thread.currentThread();
        jobContext.setCancelListener(new ThreadPool.CancelListener() {
            @Override
            public void onCancel() {
                threadCurrentThread.interrupt();
            }
        });
        while (i > 0) {
            if (jobContext.isCancelled()) {
                throw new InterruptedIOException();
            }
            outputStream.write(bArr, 0, i);
            i = inputStream.read(bArr, 0, bArr.length);
        }
        jobContext.setCancelListener(null);
        Thread.interrupted();
    }

    public static boolean download(ThreadPool.JobContext jobContext, URL url, OutputStream outputStream) throws Throwable {
        InputStream inputStreamOpenStream;
        InputStream inputStream = null;
        try {
            try {
                inputStreamOpenStream = url.openStream();
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            dump(jobContext, inputStreamOpenStream, outputStream);
            Utils.closeSilently(inputStreamOpenStream);
            return true;
        } catch (Throwable th3) {
            th = th3;
            inputStream = inputStreamOpenStream;
            Utils.closeSilently(inputStream);
            throw th;
        }
    }
}
