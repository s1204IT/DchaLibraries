package java.nio.channels;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ExecutionException;
import sun.nio.ch.ChannelInputStream;
import sun.nio.cs.StreamDecoder;
import sun.nio.cs.StreamEncoder;

public final class Channels {
    private Channels() {
    }

    private static void checkNotNull(Object obj, String str) {
        if (obj == null) {
            throw new NullPointerException("\"" + str + "\" is null!");
        }
    }

    private static void writeFullyImpl(WritableByteChannel writableByteChannel, ByteBuffer byteBuffer) throws IOException {
        while (byteBuffer.remaining() > 0) {
            if (writableByteChannel.write(byteBuffer) <= 0) {
                throw new RuntimeException("no bytes written");
            }
        }
    }

    private static void writeFully(WritableByteChannel writableByteChannel, ByteBuffer byteBuffer) throws IOException {
        if (writableByteChannel instanceof SelectableChannel) {
            SelectableChannel selectableChannel = (SelectableChannel) writableByteChannel;
            synchronized (selectableChannel.blockingLock()) {
                if (!selectableChannel.isBlocking()) {
                    throw new IllegalBlockingModeException();
                }
                writeFullyImpl(writableByteChannel, byteBuffer);
            }
            return;
        }
        writeFullyImpl(writableByteChannel, byteBuffer);
    }

    public static InputStream newInputStream(ReadableByteChannel readableByteChannel) {
        checkNotNull(readableByteChannel, "ch");
        return new ChannelInputStream(readableByteChannel);
    }

    public static OutputStream newOutputStream(final WritableByteChannel writableByteChannel) {
        checkNotNull(writableByteChannel, "ch");
        return new OutputStream() {
            private ByteBuffer bb = null;
            private byte[] bs = null;
            private byte[] b1 = null;

            @Override
            public synchronized void write(int i) throws IOException {
                if (this.b1 == null) {
                    this.b1 = new byte[1];
                }
                this.b1[0] = (byte) i;
                write(this.b1);
            }

            @Override
            public synchronized void write(byte[] bArr, int i, int i2) throws IOException {
                int i3;
                ByteBuffer byteBufferWrap;
                if (i >= 0) {
                    if (i <= bArr.length && i2 >= 0 && (i3 = i + i2) <= bArr.length && i3 >= 0) {
                        if (i2 == 0) {
                            return;
                        }
                        if (this.bs == bArr) {
                            byteBufferWrap = this.bb;
                        } else {
                            byteBufferWrap = ByteBuffer.wrap(bArr);
                        }
                        byteBufferWrap.limit(Math.min(i3, byteBufferWrap.capacity()));
                        byteBufferWrap.position(i);
                        this.bb = byteBufferWrap;
                        this.bs = bArr;
                        Channels.writeFully(writableByteChannel, byteBufferWrap);
                        return;
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            @Override
            public void close() throws IOException {
                writableByteChannel.close();
            }
        };
    }

    public static InputStream newInputStream(final AsynchronousByteChannel asynchronousByteChannel) {
        checkNotNull(asynchronousByteChannel, "ch");
        return new InputStream() {
            private ByteBuffer bb = null;
            private byte[] bs = null;
            private byte[] b1 = null;

            @Override
            public synchronized int read() throws IOException {
                if (this.b1 == null) {
                    this.b1 = new byte[1];
                }
                if (read(this.b1) == 1) {
                    return this.b1[0] & Character.DIRECTIONALITY_UNDEFINED;
                }
                return -1;
            }

            @Override
            public synchronized int read(byte[] bArr, int i, int i2) throws IOException {
                int i3;
                ByteBuffer byteBufferWrap;
                int iIntValue;
                if (i >= 0) {
                    try {
                        if (i <= bArr.length && i2 >= 0 && (i3 = i + i2) <= bArr.length && i3 >= 0) {
                            boolean z = false;
                            if (i2 == 0) {
                                return 0;
                            }
                            if (this.bs == bArr) {
                                byteBufferWrap = this.bb;
                            } else {
                                byteBufferWrap = ByteBuffer.wrap(bArr);
                            }
                            byteBufferWrap.position(i);
                            byteBufferWrap.limit(Math.min(i3, byteBufferWrap.capacity()));
                            this.bb = byteBufferWrap;
                            this.bs = bArr;
                            while (true) {
                                try {
                                    try {
                                        iIntValue = asynchronousByteChannel.read(byteBufferWrap).get().intValue();
                                        break;
                                    } catch (InterruptedException e) {
                                        z = true;
                                    } catch (ExecutionException e2) {
                                        throw new IOException(e2.getCause());
                                    }
                                } finally {
                                    if (z) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            }
                            return iIntValue;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            @Override
            public void close() throws IOException {
                asynchronousByteChannel.close();
            }
        };
    }

    public static OutputStream newOutputStream(final AsynchronousByteChannel asynchronousByteChannel) {
        checkNotNull(asynchronousByteChannel, "ch");
        return new OutputStream() {
            private ByteBuffer bb = null;
            private byte[] bs = null;
            private byte[] b1 = null;

            @Override
            public synchronized void write(int i) throws IOException {
                if (this.b1 == null) {
                    this.b1 = new byte[1];
                }
                this.b1[0] = (byte) i;
                write(this.b1);
            }

            @Override
            public synchronized void write(byte[] bArr, int i, int i2) throws IOException {
                int i3;
                ByteBuffer byteBufferWrap;
                if (i >= 0) {
                    try {
                        if (i <= bArr.length && i2 >= 0 && (i3 = i + i2) <= bArr.length && i3 >= 0) {
                            if (i2 == 0) {
                                return;
                            }
                            if (this.bs == bArr) {
                                byteBufferWrap = this.bb;
                            } else {
                                byteBufferWrap = ByteBuffer.wrap(bArr);
                            }
                            byteBufferWrap.limit(Math.min(i3, byteBufferWrap.capacity()));
                            byteBufferWrap.position(i);
                            this.bb = byteBufferWrap;
                            this.bs = bArr;
                            boolean z = false;
                            while (byteBufferWrap.remaining() > 0) {
                                try {
                                    try {
                                        asynchronousByteChannel.write(byteBufferWrap).get();
                                    } catch (InterruptedException e) {
                                        z = true;
                                    } catch (ExecutionException e2) {
                                        throw new IOException(e2.getCause());
                                    }
                                } finally {
                                    if (z) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            }
                            return;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            @Override
            public void close() throws IOException {
                asynchronousByteChannel.close();
            }
        };
    }

    public static ReadableByteChannel newChannel(InputStream inputStream) {
        checkNotNull(inputStream, "in");
        if ((inputStream instanceof FileInputStream) && FileInputStream.class.equals(inputStream.getClass())) {
            return ((FileInputStream) inputStream).getChannel();
        }
        return new ReadableByteChannelImpl(inputStream);
    }

    private static class ReadableByteChannelImpl extends AbstractInterruptibleChannel implements ReadableByteChannel {
        private static final int TRANSFER_SIZE = 8192;
        InputStream in;
        private byte[] buf = new byte[0];
        private boolean open = true;
        private Object readLock = new Object();

        ReadableByteChannelImpl(InputStream inputStream) {
            this.in = inputStream;
        }

        @Override
        public int read(ByteBuffer byteBuffer) throws IOException {
            int iRemaining = byteBuffer.remaining();
            synchronized (this.readLock) {
                boolean z = false;
                int i = 0;
                int i2 = 0;
                while (true) {
                    if (i >= iRemaining) {
                        break;
                    }
                    try {
                        int iMin = Math.min(iRemaining - i, 8192);
                        if (this.buf.length < iMin) {
                            this.buf = new byte[iMin];
                        }
                        if (i > 0 && this.in.available() <= 0) {
                            break;
                        }
                        boolean z2 = true;
                        try {
                            begin();
                            int i3 = this.in.read(this.buf, 0, iMin);
                            if (i3 <= 0) {
                                z2 = false;
                            }
                            end(z2);
                            if (i3 >= 0) {
                                i += i3;
                                byteBuffer.put(this.buf, 0, i3);
                                i2 = i3;
                            } else {
                                i2 = i3;
                                break;
                            }
                        } catch (Throwable th) {
                            if (i2 > 0) {
                                z = true;
                            }
                            end(z);
                            throw th;
                        }
                    } finally {
                    }
                }
                if (i2 >= 0 || i != 0) {
                    return i;
                }
                return -1;
            }
        }

        @Override
        protected void implCloseChannel() throws IOException {
            this.in.close();
            this.open = false;
        }
    }

    public static WritableByteChannel newChannel(OutputStream outputStream) {
        checkNotNull(outputStream, "out");
        return new WritableByteChannelImpl(outputStream);
    }

    private static class WritableByteChannelImpl extends AbstractInterruptibleChannel implements WritableByteChannel {
        private static final int TRANSFER_SIZE = 8192;
        OutputStream out;
        private byte[] buf = new byte[0];
        private boolean open = true;
        private Object writeLock = new Object();

        WritableByteChannelImpl(OutputStream outputStream) {
            this.out = outputStream;
        }

        @Override
        public int write(ByteBuffer byteBuffer) throws IOException {
            int i;
            int iRemaining = byteBuffer.remaining();
            synchronized (this.writeLock) {
                boolean z = false;
                i = 0;
                while (i < iRemaining) {
                    try {
                        int iMin = Math.min(iRemaining - i, 8192);
                        if (this.buf.length < iMin) {
                            this.buf = new byte[iMin];
                        }
                        byteBuffer.get(this.buf, 0, iMin);
                        boolean z2 = true;
                        try {
                            begin();
                            this.out.write(this.buf, 0, iMin);
                            if (iMin <= 0) {
                                z2 = false;
                            }
                            end(z2);
                            i += iMin;
                        } catch (Throwable th) {
                            if (iMin > 0) {
                                z = true;
                            }
                            end(z);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        throw th2;
                    }
                }
            }
            return i;
        }

        @Override
        protected void implCloseChannel() throws IOException {
            this.out.close();
            this.open = false;
        }
    }

    public static Reader newReader(ReadableByteChannel readableByteChannel, CharsetDecoder charsetDecoder, int i) {
        checkNotNull(readableByteChannel, "ch");
        return StreamDecoder.forDecoder(readableByteChannel, charsetDecoder.reset(), i);
    }

    public static Reader newReader(ReadableByteChannel readableByteChannel, String str) {
        checkNotNull(str, "csName");
        return newReader(readableByteChannel, Charset.forName(str).newDecoder(), -1);
    }

    public static Writer newWriter(WritableByteChannel writableByteChannel, CharsetEncoder charsetEncoder, int i) {
        checkNotNull(writableByteChannel, "ch");
        return StreamEncoder.forEncoder(writableByteChannel, charsetEncoder.reset(), i);
    }

    public static Writer newWriter(WritableByteChannel writableByteChannel, String str) {
        checkNotNull(str, "csName");
        return newWriter(writableByteChannel, Charset.forName(str).newEncoder(), -1);
    }
}
