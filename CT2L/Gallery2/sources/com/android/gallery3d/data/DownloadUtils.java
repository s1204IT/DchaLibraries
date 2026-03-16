package com.android.gallery3d.data;

import android.support.v4.app.FragmentTransaction;
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
    public static boolean requestDownload(ThreadPool.JobContext jc, URL url, File file) throws Throwable {
        FileOutputStream fos = null;
        try {
            FileOutputStream fos2 = new FileOutputStream(file);
            try {
                boolean zDownload = download(jc, url, fos2);
                Utils.closeSilently(fos2);
                return zDownload;
            } catch (Throwable th) {
                th = th;
                fos = fos2;
                Utils.closeSilently(fos);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static void dump(ThreadPool.JobContext jc, InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[FragmentTransaction.TRANSIT_ENTER_MASK];
        int rc = is.read(buffer, 0, buffer.length);
        final Thread thread = Thread.currentThread();
        jc.setCancelListener(new ThreadPool.CancelListener() {
            @Override
            public void onCancel() {
                thread.interrupt();
            }
        });
        while (rc > 0) {
            if (jc.isCancelled()) {
                throw new InterruptedIOException();
            }
            os.write(buffer, 0, rc);
            rc = is.read(buffer, 0, buffer.length);
        }
        jc.setCancelListener(null);
        Thread.interrupted();
    }

    public static boolean download(ThreadPool.JobContext jc, URL url, OutputStream output) {
        InputStream input = null;
        try {
            input = url.openStream();
            dump(jc, input, output);
            return true;
        } catch (Throwable t) {
            Log.w("DownloadService", "fail to download", t);
            return false;
        } finally {
            Utils.closeSilently(input);
        }
    }
}
