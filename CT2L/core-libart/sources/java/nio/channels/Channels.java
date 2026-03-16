package java.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import libcore.io.Streams;

public final class Channels {
    private Channels() {
    }

    public static InputStream newInputStream(ReadableByteChannel channel) {
        return new ChannelInputStream(channel);
    }

    public static OutputStream newOutputStream(WritableByteChannel channel) {
        return new ChannelOutputStream(channel);
    }

    public static ReadableByteChannel newChannel(InputStream inputStream) {
        return new InputStreamChannel(inputStream);
    }

    public static WritableByteChannel newChannel(OutputStream outputStream) {
        return new OutputStreamChannel(outputStream);
    }

    public static Reader newReader(ReadableByteChannel channel, CharsetDecoder decoder, int minBufferCapacity) {
        return new InputStreamReader(new ChannelInputStream(channel), decoder);
    }

    public static Reader newReader(ReadableByteChannel channel, String charsetName) {
        if (charsetName == null) {
            throw new NullPointerException("charsetName == null");
        }
        return newReader(channel, Charset.forName(charsetName).newDecoder(), -1);
    }

    public static Writer newWriter(WritableByteChannel channel, CharsetEncoder encoder, int minBufferCapacity) {
        return new OutputStreamWriter(new ChannelOutputStream(channel), encoder);
    }

    public static Writer newWriter(WritableByteChannel channel, String charsetName) {
        if (charsetName == null) {
            throw new NullPointerException("charsetName == null");
        }
        return newWriter(channel, Charset.forName(charsetName).newEncoder(), -1);
    }

    private static class ChannelInputStream extends InputStream {
        private final ReadableByteChannel channel;

        ChannelInputStream(ReadableByteChannel channel) {
            if (channel == null) {
                throw new NullPointerException("channel == null");
            }
            this.channel = channel;
        }

        @Override
        public synchronized int read() throws IOException {
            return Streams.readSingleByte(this);
        }

        @Override
        public synchronized int read(byte[] target, int byteOffset, int byteCount) throws IOException {
            ByteBuffer buffer;
            buffer = ByteBuffer.wrap(target, byteOffset, byteCount);
            Channels.checkBlocking(this.channel);
            return this.channel.read(buffer);
        }

        @Override
        public int available() throws IOException {
            if (this.channel instanceof FileChannel) {
                FileChannel fileChannel = (FileChannel) this.channel;
                long result = fileChannel.size() - fileChannel.position();
                return result > 2147483647L ? Integer.MAX_VALUE : (int) result;
            }
            return super.available();
        }

        @Override
        public synchronized void close() throws IOException {
            this.channel.close();
        }
    }

    private static class ChannelOutputStream extends OutputStream {
        private final WritableByteChannel channel;

        ChannelOutputStream(WritableByteChannel channel) {
            if (channel == null) {
                throw new NullPointerException("channel == null");
            }
            this.channel = channel;
        }

        @Override
        public synchronized void write(int oneByte) throws IOException {
            byte[] wrappedByte = {(byte) oneByte};
            write(wrappedByte);
        }

        @Override
        public synchronized void write(byte[] source, int offset, int length) throws IOException {
            ByteBuffer buffer = ByteBuffer.wrap(source, offset, length);
            Channels.checkBlocking(this.channel);
            int total = 0;
            while (total < length) {
                total += this.channel.write(buffer);
            }
        }

        @Override
        public synchronized void close() throws IOException {
            this.channel.close();
        }
    }

    static void checkBlocking(Channel channel) {
        if ((channel instanceof SelectableChannel) && !((SelectableChannel) channel).isBlocking()) {
            throw new IllegalBlockingModeException();
        }
    }

    private static class InputStreamChannel extends AbstractInterruptibleChannel implements ReadableByteChannel {
        private final InputStream inputStream;

        InputStreamChannel(InputStream inputStream) {
            if (inputStream == null) {
                throw new NullPointerException("inputStream == null");
            }
            this.inputStream = inputStream;
        }

        @Override
        public synchronized int read(ByteBuffer target) throws IOException {
            int readCount;
            synchronized (this) {
                if (!isOpen()) {
                    throw new ClosedChannelException();
                }
                int bytesRemain = target.remaining();
                byte[] bytes = new byte[bytesRemain];
                readCount = 0;
                try {
                    begin();
                    readCount = this.inputStream.read(bytes);
                    if (readCount > 0) {
                        target.put(bytes, 0, readCount);
                    }
                } finally {
                    end(readCount >= 0);
                }
            }
            return readCount;
        }

        @Override
        protected void implCloseChannel() throws IOException {
            this.inputStream.close();
        }
    }

    private static class OutputStreamChannel extends AbstractInterruptibleChannel implements WritableByteChannel {
        private final OutputStream outputStream;

        OutputStreamChannel(OutputStream outputStream) {
            if (outputStream == null) {
                throw new NullPointerException("outputStream == null");
            }
            this.outputStream = outputStream;
        }

        @Override
        public synchronized int write(ByteBuffer source) throws IOException {
            int bytesRemain;
            synchronized (this) {
                if (!isOpen()) {
                    throw new ClosedChannelException();
                }
                bytesRemain = source.remaining();
                if (bytesRemain == 0) {
                    bytesRemain = 0;
                } else {
                    byte[] buf = new byte[bytesRemain];
                    source.get(buf);
                    try {
                        begin();
                        this.outputStream.write(buf, 0, bytesRemain);
                        end(bytesRemain >= 0);
                    } catch (Throwable th) {
                        end(bytesRemain >= 0);
                        throw th;
                    }
                }
            }
            return bytesRemain;
        }

        @Override
        protected void implCloseChannel() throws IOException {
            this.outputStream.close();
        }
    }
}
