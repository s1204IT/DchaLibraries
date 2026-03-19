package android.support.v4.graphics;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Log;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TypefaceCompatUtil {
    public static File getTempFile(Context context) {
        String prefix = ".font" + Process.myPid() + "-" + Process.myTid() + "-";
        for (int i = 0; i < 100; i++) {
            File file = new File(context.getCacheDir(), prefix + i);
            if (file.createNewFile()) {
                return file;
            }
        }
        return null;
    }

    private static ByteBuffer mmap(File file) throws Throwable {
        Throwable th;
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                FileChannel channel = fis.getChannel();
                long size = channel.size();
                MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0L, size);
                fis.close();
                return map;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (th != null) {
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static ByteBuffer mmap(Context context, CancellationSignal cancellationSignal, Uri uri) throws Throwable {
        Throwable th;
        Throwable th2;
        ContentResolver resolver = context.getContentResolver();
        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r", cancellationSignal);
            if (pfd == null) {
                if (pfd != null) {
                    pfd.close();
                }
                return null;
            }
            try {
                try {
                    FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                    try {
                        FileChannel channel = fis.getChannel();
                        long size = channel.size();
                        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0L, size);
                        fis.close();
                        if (pfd != null) {
                            pfd.close();
                        }
                        return map;
                    } catch (Throwable th3) {
                        th = th3;
                        th2 = null;
                        if (th2 != null) {
                        }
                    }
                } catch (Throwable th4) {
                    try {
                        throw th4;
                    } catch (Throwable th5) {
                        th = th4;
                        th = th5;
                        if (pfd != null) {
                            throw th;
                        }
                        if (th == null) {
                            pfd.close();
                            throw th;
                        }
                        try {
                            pfd.close();
                            throw th;
                        } catch (Throwable th6) {
                            th.addSuppressed(th6);
                            throw th;
                        }
                    }
                }
            } catch (Throwable th7) {
                th = th7;
                th = null;
                if (pfd != null) {
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static ByteBuffer copyToDirectBuffer(Context context, Resources res, int id) {
        File tmpFile = getTempFile(context);
        ByteBuffer byteBufferMmap = null;
        if (tmpFile == null) {
            return null;
        }
        try {
            if (copyToFile(tmpFile, res, id)) {
                byteBufferMmap = mmap(tmpFile);
            }
            return byteBufferMmap;
        } finally {
            tmpFile.delete();
        }
    }

    public static boolean copyToFile(File file, InputStream is) {
        FileOutputStream os = null;
        boolean z = false;
        try {
            try {
                os = new FileOutputStream(file, false);
                byte[] buffer = new byte[1024];
                while (true) {
                    int readLen = is.read(buffer);
                    if (readLen == -1) {
                        break;
                    }
                    os.write(buffer, 0, readLen);
                }
                z = true;
            } catch (IOException e) {
                Log.e("TypefaceCompatUtil", "Error copying resource contents to temp file: " + e.getMessage());
            }
            return z;
        } finally {
            closeQuietly(os);
        }
    }

    public static boolean copyToFile(File file, Resources res, int id) {
        InputStream is = null;
        try {
            is = res.openRawResource(id);
            return copyToFile(file, is);
        } finally {
            closeQuietly(is);
        }
    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}
