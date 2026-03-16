package com.android.okhttp.internal;

import com.android.okhttp.internal.spdy.Header;
import com.android.okio.ByteString;
import com.android.okio.OkBuffer;
import com.android.okio.Source;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class Util {
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final InputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(EMPTY_BYTE_ARRAY);
    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private Util() {
    }

    public static int getEffectivePort(URI uri) {
        return getEffectivePort(uri.getScheme(), uri.getPort());
    }

    public static int getEffectivePort(URL url) {
        return getEffectivePort(url.getProtocol(), url.getPort());
    }

    private static int getEffectivePort(String scheme, int specifiedPort) {
        return specifiedPort != -1 ? specifiedPort : getDefaultPort(scheme);
    }

    public static int getDefaultPort(String protocol) {
        if ("http".equals(protocol)) {
            return 80;
        }
        return "https".equals(protocol) ? 443 : -1;
    }

    public static void checkOffsetAndCount(long arrayLength, long offset, long count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public static void closeQuietly(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public static void closeAll(Closeable a, Closeable b) throws Throwable {
        Throwable thrown = null;
        try {
            a.close();
        } catch (Throwable e) {
            thrown = e;
        }
        try {
            b.close();
        } catch (Throwable e2) {
            if (thrown == null) {
                thrown = e2;
            }
        }
        if (thrown == null) {
            return;
        }
        if (thrown instanceof IOException) {
            throw ((IOException) thrown);
        }
        if (thrown instanceof RuntimeException) {
            throw ((RuntimeException) thrown);
        }
        if (!(thrown instanceof Error)) {
            throw new AssertionError(thrown);
        }
        throw ((Error) thrown);
    }

    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("not a readable directory: " + dir);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    public static void readFully(InputStream in, byte[] dst) throws IOException {
        readFully(in, dst, 0, dst.length);
    }

    public static void readFully(InputStream in, byte[] dst, int offset, int byteCount) throws IOException {
        if (byteCount != 0) {
            if (in == null) {
                throw new NullPointerException("in == null");
            }
            if (dst == null) {
                throw new NullPointerException("dst == null");
            }
            checkOffsetAndCount(dst.length, offset, byteCount);
            while (byteCount > 0) {
                int bytesRead = in.read(dst, offset, byteCount);
                if (bytesRead < 0) {
                    throw new EOFException();
                }
                offset += bytesRead;
                byteCount -= bytesRead;
            }
        }
    }

    public static OkBuffer readFully(Source source) throws IOException {
        OkBuffer result = new OkBuffer();
        while (source.read(result, 2048L) != -1) {
        }
        source.close();
        return result;
    }

    public static boolean skipAll(Source in, int timeoutMillis) throws IOException {
        long startNanos = System.nanoTime();
        OkBuffer skipBuffer = new OkBuffer();
        while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos) < timeoutMillis) {
            long read = in.read(skipBuffer, 2048L);
            if (read == -1) {
                return true;
            }
            skipBuffer.clear();
        }
        return false;
    }

    public static int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        while (true) {
            int c = in.read(buffer);
            if (c != -1) {
                total += c;
                out.write(buffer, 0, c);
            } else {
                return total;
            }
        }
    }

    public static String hash(String s) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5bytes = messageDigest.digest(s.getBytes("UTF-8"));
            return ByteString.of(md5bytes).hex();
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        } catch (NoSuchAlgorithmException e2) {
            throw new AssertionError(e2);
        }
    }

    public static <T> List<T> immutableList(List<T> list) {
        return Collections.unmodifiableList(new ArrayList(list));
    }

    public static <T> List<T> immutableList(T... elements) {
        return Collections.unmodifiableList(Arrays.asList((Object[]) elements.clone()));
    }

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }

    public static List<Header> headerEntries(String... elements) {
        List<Header> result = new ArrayList<>(elements.length / 2);
        for (int i = 0; i < elements.length; i += 2) {
            result.add(new Header(elements[i], elements[i + 1]));
        }
        return result;
    }
}
