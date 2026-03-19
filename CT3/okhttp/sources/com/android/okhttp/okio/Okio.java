package com.android.okhttp.okio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Okio {
    private static final Logger logger = Logger.getLogger(Okio.class.getName());

    private Okio() {
    }

    public static BufferedSource buffer(Source source) {
        if (source == null) {
            throw new IllegalArgumentException("source == null");
        }
        return new RealBufferedSource(source);
    }

    public static BufferedSink buffer(Sink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("sink == null");
        }
        return new RealBufferedSink(sink);
    }

    public static Sink sink(OutputStream out) {
        return sink(out, new Timeout());
    }

    private static Sink sink(final OutputStream out, final Timeout timeout) {
        if (out == null) {
            throw new IllegalArgumentException("out == null");
        }
        if (timeout == null) {
            throw new IllegalArgumentException("timeout == null");
        }
        return new Sink() {
            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                Util.checkOffsetAndCount(source.size, 0L, byteCount);
                while (byteCount > 0) {
                    timeout.throwIfReached();
                    Segment head = source.head;
                    int toCopy = (int) Math.min(byteCount, head.limit - head.pos);
                    out.write(head.data, head.pos, toCopy);
                    head.pos += toCopy;
                    byteCount -= (long) toCopy;
                    source.size -= (long) toCopy;
                    if (head.pos == head.limit) {
                        source.head = head.pop();
                        SegmentPool.recycle(head);
                    }
                }
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                out.close();
            }

            @Override
            public Timeout timeout() {
                return timeout;
            }

            public String toString() {
                return "sink(" + out + ")";
            }
        };
    }

    public static Sink sink(Socket socket) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("socket == null");
        }
        AsyncTimeout timeout = timeout(socket);
        Sink sink = sink(socket.getOutputStream(), timeout);
        return timeout.sink(sink);
    }

    public static Source source(InputStream in) {
        return source(in, new Timeout());
    }

    private static Source source(final InputStream in, final Timeout timeout) {
        if (in == null) {
            throw new IllegalArgumentException("in == null");
        }
        if (timeout == null) {
            throw new IllegalArgumentException("timeout == null");
        }
        return new Source() {
            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                if (byteCount < 0) {
                    throw new IllegalArgumentException("byteCount < 0: " + byteCount);
                }
                if (byteCount == 0) {
                    return 0L;
                }
                try {
                    timeout.throwIfReached();
                    Segment tail = sink.writableSegment(1);
                    int maxToCopy = (int) Math.min(byteCount, 8192 - tail.limit);
                    int bytesRead = in.read(tail.data, tail.limit, maxToCopy);
                    if (bytesRead == -1) {
                        return -1L;
                    }
                    tail.limit += bytesRead;
                    sink.size += (long) bytesRead;
                    return bytesRead;
                } catch (AssertionError e) {
                    if (Okio.isAndroidGetsocknameError(e)) {
                        throw new IOException(e);
                    }
                    throw e;
                }
            }

            @Override
            public void close() throws IOException {
                in.close();
            }

            @Override
            public Timeout timeout() {
                return timeout;
            }

            public String toString() {
                return "source(" + in + ")";
            }
        };
    }

    public static Source source(File file) throws FileNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        return source(new FileInputStream(file));
    }

    public static Sink sink(File file) throws FileNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        return sink(new FileOutputStream(file));
    }

    public static Sink appendingSink(File file) throws FileNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        return sink(new FileOutputStream(file, true));
    }

    public static Source source(Socket socket) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("socket == null");
        }
        AsyncTimeout timeout = timeout(socket);
        Source source = source(socket.getInputStream(), timeout);
        return timeout.source(source);
    }

    private static AsyncTimeout timeout(final Socket socket) {
        return new AsyncTimeout() {
            @Override
            protected IOException newTimeoutException(IOException cause) {
                InterruptedIOException ioe = new SocketTimeoutException("timeout");
                if (cause != null) {
                    ioe.initCause(cause);
                }
                return ioe;
            }

            @Override
            protected void timedOut() {
                try {
                    try {
                        if (socket != null) {
                            try {
                                socket.shutdownInput();
                            } catch (Exception e) {
                            }
                            try {
                                socket.shutdownOutput();
                            } catch (Exception e2) {
                            }
                        }
                        socket.close();
                    } catch (Exception e3) {
                        Okio.logger.log(Level.WARNING, "Failed to close timed out socket " + socket, (Throwable) e3);
                    }
                } catch (AssertionError e4) {
                    if (Okio.isAndroidGetsocknameError(e4)) {
                        Okio.logger.log(Level.WARNING, "Failed to close timed out socket " + socket, (Throwable) e4);
                        return;
                    }
                    throw e4;
                }
            }
        };
    }

    private static boolean isAndroidGetsocknameError(AssertionError e) {
        if (e.getCause() == null || e.getMessage() == null) {
            return false;
        }
        return e.getMessage().contains("getsockname failed");
    }
}
