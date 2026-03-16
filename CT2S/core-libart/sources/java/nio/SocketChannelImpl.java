package java.nio;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PlainSocketImpl;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketUtils;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import libcore.io.IoBridge;
import libcore.io.IoUtils;
import libcore.io.Libcore;

class SocketChannelImpl extends SocketChannel implements FileDescriptorChannel {
    private static final int SOCKET_STATUS_CLOSED = 3;
    private static final int SOCKET_STATUS_CONNECTED = 2;
    private static final int SOCKET_STATUS_PENDING = 1;
    private static final int SOCKET_STATUS_UNCONNECTED = 0;
    private static final int SOCKET_STATUS_UNINITIALIZED = -1;
    private InetSocketAddress connectAddress;
    private final FileDescriptor fd;
    private volatile boolean isBound;
    private InetAddress localAddress;
    private int localPort;
    private final Object readLock;
    private SocketAdapter socket;
    private int status;
    private final Object writeLock;

    public SocketChannelImpl(SelectorProvider selectorProvider) throws IOException {
        this(selectorProvider, true);
    }

    public SocketChannelImpl(SelectorProvider selectorProvider, boolean connect) throws IOException {
        super(selectorProvider);
        this.socket = null;
        this.connectAddress = null;
        this.localAddress = null;
        this.status = -1;
        this.isBound = false;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.status = 0;
        this.fd = connect ? IoBridge.socket(true) : new FileDescriptor();
    }

    public SocketChannelImpl(SelectorProvider selectorProvider, FileDescriptor existingFd) throws IOException {
        super(selectorProvider);
        this.socket = null;
        this.connectAddress = null;
        this.localAddress = null;
        this.status = -1;
        this.isBound = false;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.status = 2;
        this.fd = existingFd;
    }

    @Override
    public synchronized Socket socket() {
        SocketAdapter socketAdapter;
        if (this.socket == null) {
            InetAddress addr = null;
            int port = 0;
            try {
                if (this.connectAddress != null) {
                    addr = this.connectAddress.getAddress();
                    port = this.connectAddress.getPort();
                }
                this.socket = new SocketAdapter(new PlainSocketImpl(this.fd, this.localPort, addr, port), this);
            } catch (SocketException e) {
                socketAdapter = null;
            }
        }
        socketAdapter = this.socket;
        return socketAdapter;
    }

    void onBind(boolean updateSocketState) {
        try {
            SocketAddress sa = Libcore.os.getsockname(this.fd);
            this.isBound = true;
            InetSocketAddress localSocketAddress = (InetSocketAddress) sa;
            this.localAddress = localSocketAddress.getAddress();
            this.localPort = localSocketAddress.getPort();
            if (updateSocketState && this.socket != null) {
                this.socket.onBind(this.localAddress, this.localPort);
            }
        } catch (ErrnoException errnoException) {
            throw new AssertionError(errnoException);
        }
    }

    @Override
    public synchronized boolean isConnected() {
        return this.status == 2;
    }

    @Override
    public synchronized boolean isConnectionPending() {
        boolean z;
        synchronized (this) {
            z = this.status == 1;
        }
        return z;
    }

    @Override
    public boolean connect(SocketAddress socketAddress) throws IOException {
        int newStatus;
        checkUnconnected();
        InetSocketAddress inetSocketAddress = validateAddress(socketAddress);
        InetAddress normalAddr = inetSocketAddress.getAddress();
        int port = inetSocketAddress.getPort();
        if (normalAddr.isAnyLocalAddress()) {
            normalAddr = InetAddress.getLocalHost();
        }
        boolean isBlocking = isBlocking();
        boolean finished = false;
        if (isBlocking) {
            try {
                try {
                    begin();
                } catch (IOException e) {
                    if (isEINPROGRESS(e)) {
                        newStatus = 1;
                        if (isBlocking) {
                            end(false);
                        }
                    } else {
                        if (isOpen()) {
                            close();
                            finished = true;
                        }
                        throw e;
                    }
                }
            } finally {
                if (isBlocking) {
                    end(finished);
                }
            }
        }
        IoBridge.connect(this.fd, normalAddr, port);
        newStatus = isBlocking ? 2 : 1;
        finished = true;
        if (!this.isBound) {
            onBind(true);
        }
        onConnectStatusChanged(inetSocketAddress, newStatus, true);
        return this.status == 2;
    }

    void onConnectStatusChanged(InetSocketAddress address, int status, boolean updateSocketState) {
        this.status = status;
        this.connectAddress = address;
        if (status == 2 && updateSocketState && this.socket != null) {
            this.socket.onConnect(this.connectAddress.getAddress(), this.connectAddress.getPort());
        }
    }

    private boolean isEINPROGRESS(IOException e) {
        if (isBlocking() || !(e instanceof ConnectException)) {
            return false;
        }
        Throwable cause = e.getCause();
        return (cause instanceof ErrnoException) && ((ErrnoException) cause).errno == OsConstants.EINPROGRESS;
    }

    @Override
    public boolean finishConnect() throws IOException {
        boolean finished = true;
        synchronized (this) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.status != 2) {
                if (this.status != 1) {
                    throw new NoConnectionPendingException();
                }
                boolean finished2 = false;
                try {
                    try {
                        begin();
                        InetAddress inetAddress = this.connectAddress.getAddress();
                        int port = this.connectAddress.getPort();
                        finished2 = IoBridge.isConnected(this.fd, inetAddress, port, 0, 0);
                        synchronized (this) {
                            this.status = finished2 ? 2 : this.status;
                            if (finished2 && this.socket != null) {
                                this.socket.onConnect(this.connectAddress.getAddress(), this.connectAddress.getPort());
                            }
                        }
                    } catch (ConnectException e) {
                        if (isOpen()) {
                            close();
                            finished2 = true;
                        }
                        throw e;
                    }
                } finally {
                    end(finished2);
                }
            }
            return finished;
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        dst.checkWritable();
        checkOpenConnected();
        if (dst.hasRemaining()) {
            return readImpl(dst);
        }
        return 0;
    }

    @Override
    public long read(ByteBuffer[] targets, int offset, int length) throws IOException {
        Arrays.checkOffsetAndCount(targets.length, offset, length);
        checkOpenConnected();
        int totalCount = FileChannelImpl.calculateTotalRemaining(targets, offset, length, true);
        if (totalCount == 0) {
            return 0L;
        }
        byte[] readArray = new byte[totalCount];
        ByteBuffer readBuffer = ByteBuffer.wrap(readArray);
        int readCount = readImpl(readBuffer);
        readBuffer.flip();
        if (readCount > 0) {
            int left = readCount;
            int index = offset;
            while (left > 0) {
                int putLength = Math.min(targets[index].remaining(), left);
                targets[index].put(readArray, readCount - left, putLength);
                index++;
                left -= putLength;
            }
        }
        return readCount;
    }

    private int readImpl(ByteBuffer dst) throws IOException {
        int readCount;
        synchronized (this.readLock) {
            try {
                if (isBlocking()) {
                    begin();
                }
                readCount = IoBridge.recvfrom(true, this.fd, dst, 0, null, false);
                if (isBlocking()) {
                    end(readCount > 0);
                }
            } catch (Throwable th) {
                if (isBlocking()) {
                    end(0 > 0);
                }
                throw th;
            }
        }
        return readCount;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        checkOpenConnected();
        if (src.hasRemaining()) {
            return writeImpl(src);
        }
        return 0;
    }

    @Override
    public long write(ByteBuffer[] sources, int offset, int length) throws IOException {
        Arrays.checkOffsetAndCount(sources.length, offset, length);
        checkOpenConnected();
        int count = FileChannelImpl.calculateTotalRemaining(sources, offset, length, false);
        if (count == 0) {
            return 0L;
        }
        ByteBuffer writeBuf = ByteBuffer.allocate(count);
        for (int val = offset; val < length + offset; val++) {
            ByteBuffer source = sources[val];
            int oldPosition = source.position();
            writeBuf.put(source);
            source.position(oldPosition);
        }
        writeBuf.flip();
        int result = writeImpl(writeBuf);
        int val2 = offset;
        while (result > 0) {
            ByteBuffer source2 = sources[val2];
            int gap = Math.min(result, source2.remaining());
            source2.position(source2.position() + gap);
            val2++;
            result -= gap;
        }
        return result;
    }

    private int writeImpl(ByteBuffer src) throws IOException {
        synchronized (this.writeLock) {
            if (!src.hasRemaining()) {
                return 0;
            }
            try {
                if (isBlocking()) {
                    begin();
                }
                int writeCount = IoBridge.sendto(this.fd, src, 0, null, 0);
                if (isBlocking()) {
                    end(writeCount >= 0);
                }
                return writeCount;
            } catch (Throwable th) {
                if (isBlocking()) {
                    end(0 >= 0);
                }
                throw th;
            }
        }
    }

    private synchronized void checkOpenConnected() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (!isConnected()) {
            throw new NotYetConnectedException();
        }
    }

    private synchronized void checkUnconnected() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (this.status == 2) {
            throw new AlreadyConnectedException();
        }
        if (this.status == 1) {
            throw new ConnectionPendingException();
        }
    }

    static InetSocketAddress validateAddress(SocketAddress socketAddress) {
        if (socketAddress == null) {
            throw new IllegalArgumentException("socketAddress == null");
        }
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        if (inetSocketAddress.isUnresolved()) {
            throw new UnresolvedAddressException();
        }
        return inetSocketAddress;
    }

    @Override
    protected synchronized void implCloseSelectableChannel() throws IOException {
        if (this.status != 3) {
            this.status = 3;
            IoBridge.closeAndSignalBlockedThreads(this.fd);
            if (this.socket != null && !this.socket.isClosed()) {
                this.socket.onClose();
            }
        }
    }

    @Override
    protected void implConfigureBlocking(boolean blocking) throws IOException {
        IoUtils.setBlocking(this.fd, blocking);
    }

    @Override
    public FileDescriptor getFD() {
        return this.fd;
    }

    public void onAccept(InetSocketAddress remoteAddress, boolean updateSocketState) {
        onBind(updateSocketState);
        onConnectStatusChanged(remoteAddress, 2, updateSocketState);
    }

    private static class SocketAdapter extends Socket {
        private final SocketChannelImpl channel;
        private final PlainSocketImpl socketImpl;

        SocketAdapter(PlainSocketImpl socketImpl, SocketChannelImpl channel) throws SocketException {
            super(socketImpl);
            this.socketImpl = socketImpl;
            this.channel = channel;
            SocketUtils.setCreated(this);
            if (channel.isBound) {
                onBind(channel.localAddress, channel.localPort);
            }
            if (channel.isConnected()) {
                onConnect(channel.connectAddress.getAddress(), channel.connectAddress.getPort());
            }
            if (!channel.isOpen()) {
                onClose();
            }
        }

        @Override
        public SocketChannel getChannel() {
            return this.channel;
        }

        @Override
        public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
            if (!this.channel.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            if (isConnected()) {
                throw new AlreadyConnectedException();
            }
            super.connect(remoteAddr, timeout);
            this.channel.onBind(false);
            if (super.isConnected()) {
                InetSocketAddress remoteInetAddress = (InetSocketAddress) remoteAddr;
                this.channel.onConnectStatusChanged(remoteInetAddress, 2, false);
            }
        }

        @Override
        public void bind(SocketAddress localAddr) throws IOException {
            if (this.channel.isConnected()) {
                throw new AlreadyConnectedException();
            }
            if (1 == this.channel.status) {
                throw new ConnectionPendingException();
            }
            super.bind(localAddr);
            this.channel.onBind(false);
        }

        @Override
        public void close() throws IOException {
            synchronized (this.channel) {
                super.close();
                if (this.channel.isOpen()) {
                    this.channel.close();
                }
            }
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new BlockingCheckOutputStream(super.getOutputStream(), this.channel);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new BlockingCheckInputStream(super.getInputStream(), this.channel);
        }

        @Override
        public FileDescriptor getFileDescriptor$() {
            return this.socketImpl.getFD$();
        }
    }

    private static class BlockingCheckOutputStream extends FilterOutputStream {
        private final SocketChannel channel;

        public BlockingCheckOutputStream(OutputStream out, SocketChannel channel) {
            super(out);
            this.channel = channel;
        }

        @Override
        public void write(byte[] buffer, int offset, int byteCount) throws IOException {
            checkBlocking();
            this.out.write(buffer, offset, byteCount);
        }

        @Override
        public void write(int oneByte) throws IOException {
            checkBlocking();
            this.out.write(oneByte);
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            checkBlocking();
            this.out.write(buffer);
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.channel.close();
        }

        private void checkBlocking() {
            if (!this.channel.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
        }
    }

    private static class BlockingCheckInputStream extends FilterInputStream {
        private final SocketChannel channel;

        public BlockingCheckInputStream(InputStream in, SocketChannel channel) {
            super(in);
            this.channel = channel;
        }

        @Override
        public int read() throws IOException {
            checkBlocking();
            return this.in.read();
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            checkBlocking();
            return this.in.read(buffer, byteOffset, byteCount);
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            checkBlocking();
            return this.in.read(buffer);
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.channel.close();
        }

        private void checkBlocking() {
            if (!this.channel.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
        }
    }
}
