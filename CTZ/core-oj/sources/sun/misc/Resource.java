package sun.misc;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.jar.Manifest;
import sun.nio.ByteBuffered;

public abstract class Resource {
    private InputStream cis;

    public abstract URL getCodeSourceURL();

    public abstract int getContentLength() throws IOException;

    public abstract InputStream getInputStream() throws IOException;

    public abstract String getName();

    public abstract URL getURL();

    private synchronized InputStream cachedInputStream() throws IOException {
        if (this.cis == null) {
            this.cis = getInputStream();
        }
        return this.cis;
    }

    public byte[] getBytes() throws Throwable {
        int contentLength;
        boolean z;
        Throwable th;
        int length;
        int i;
        InputStream inputStreamCachedInputStream = cachedInputStream();
        boolean zInterrupted = Thread.interrupted();
        boolean z2 = true;
        while (true) {
            try {
                contentLength = getContentLength();
                break;
            } catch (InterruptedIOException e) {
                Thread.interrupted();
                zInterrupted = true;
            }
        }
        try {
            byte[] bArrCopyOf = new byte[0];
            if (contentLength == -1) {
                contentLength = Integer.MAX_VALUE;
            }
            z = zInterrupted;
            int i2 = 0;
            while (true) {
                if (i2 < contentLength) {
                    try {
                        if (i2 >= bArrCopyOf.length) {
                            length = Math.min(contentLength - i2, bArrCopyOf.length + 1024);
                            int i3 = i2 + length;
                            if (bArrCopyOf.length < i3) {
                                bArrCopyOf = Arrays.copyOf(bArrCopyOf, i3);
                            }
                        } else {
                            length = bArrCopyOf.length - i2;
                        }
                        try {
                            i = inputStreamCachedInputStream.read(bArrCopyOf, i2, length);
                        } catch (InterruptedIOException e2) {
                            Thread.interrupted();
                            z = true;
                            i = 0;
                        }
                        if (i >= 0) {
                            i2 += i;
                        } else {
                            if (contentLength != Integer.MAX_VALUE) {
                                throw new EOFException("Detect premature EOF");
                            }
                            if (bArrCopyOf.length != i2) {
                                bArrCopyOf = Arrays.copyOf(bArrCopyOf, i2);
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        try {
                            inputStreamCachedInputStream.close();
                        } catch (InterruptedIOException e3) {
                            if (z2) {
                            }
                        } catch (IOException e4) {
                        }
                        z2 = z;
                        if (z2) {
                            throw th;
                        }
                        Thread.currentThread().interrupt();
                        throw th;
                    }
                }
            }
            try {
                inputStreamCachedInputStream.close();
            } catch (InterruptedIOException e5) {
            } catch (IOException e6) {
            }
            z2 = z;
            if (z2) {
                Thread.currentThread().interrupt();
            }
            return bArrCopyOf;
        } catch (Throwable th3) {
            z = zInterrupted;
            th = th3;
        }
    }

    public ByteBuffer getByteBuffer() throws IOException {
        Closeable closeableCachedInputStream = cachedInputStream();
        if (closeableCachedInputStream instanceof ByteBuffered) {
            return ((ByteBuffered) closeableCachedInputStream).getByteBuffer();
        }
        return null;
    }

    public Manifest getManifest() throws IOException {
        return null;
    }

    public Certificate[] getCertificates() {
        return null;
    }

    public CodeSigner[] getCodeSigners() {
        return null;
    }
}
