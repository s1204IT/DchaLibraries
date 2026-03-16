package java.nio;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import libcore.io.IoUtils;
import libcore.io.Libcore;

final class PipeImpl extends Pipe {
    private final PipeSinkChannel sink;
    private final PipeSourceChannel source;

    public PipeImpl(SelectorProvider selectorProvider) throws IOException {
        try {
            FileDescriptor fd1 = new FileDescriptor();
            FileDescriptor fd2 = new FileDescriptor();
            Libcore.os.socketpair(OsConstants.AF_UNIX, OsConstants.SOCK_STREAM, 0, fd1, fd2);
            this.sink = new PipeSinkChannel(selectorProvider, fd1);
            this.source = new PipeSourceChannel(selectorProvider, fd2);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    @Override
    public Pipe.SinkChannel sink() {
        return this.sink;
    }

    @Override
    public Pipe.SourceChannel source() {
        return this.source;
    }

    private class PipeSourceChannel extends Pipe.SourceChannel implements FileDescriptorChannel {
        private final SocketChannel channel;
        private final FileDescriptor fd;

        private PipeSourceChannel(SelectorProvider selectorProvider, FileDescriptor fd) throws IOException {
            super(selectorProvider);
            this.fd = fd;
            this.channel = new SocketChannelImpl(selectorProvider, fd);
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            this.channel.close();
        }

        @Override
        protected void implConfigureBlocking(boolean blocking) throws IOException {
            IoUtils.setBlocking(getFD(), blocking);
        }

        @Override
        public int read(ByteBuffer buffer) throws IOException {
            return this.channel.read(buffer);
        }

        @Override
        public long read(ByteBuffer[] buffers) throws IOException {
            return this.channel.read(buffers);
        }

        @Override
        public long read(ByteBuffer[] buffers, int offset, int length) throws IOException {
            return this.channel.read(buffers, offset, length);
        }

        @Override
        public FileDescriptor getFD() {
            return this.fd;
        }
    }

    private class PipeSinkChannel extends Pipe.SinkChannel implements FileDescriptorChannel {
        private final SocketChannel channel;
        private final FileDescriptor fd;

        private PipeSinkChannel(SelectorProvider selectorProvider, FileDescriptor fd) throws IOException {
            super(selectorProvider);
            this.fd = fd;
            this.channel = new SocketChannelImpl(selectorProvider, fd);
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            this.channel.close();
        }

        @Override
        protected void implConfigureBlocking(boolean blocking) throws IOException {
            IoUtils.setBlocking(getFD(), blocking);
        }

        @Override
        public int write(ByteBuffer buffer) throws IOException {
            return this.channel.write(buffer);
        }

        @Override
        public long write(ByteBuffer[] buffers) throws IOException {
            return this.channel.write(buffers);
        }

        @Override
        public long write(ByteBuffer[] buffers, int offset, int length) throws IOException {
            return this.channel.write(buffers, offset, length);
        }

        @Override
        public FileDescriptor getFD() {
            return this.fd;
        }
    }
}
