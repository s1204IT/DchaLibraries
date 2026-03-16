package java.nio;

import android.system.ErrnoException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PlainDatagramSocketImpl;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import libcore.io.IoBridge;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import libcore.util.EmptyArray;

class DatagramChannelImpl extends DatagramChannel implements FileDescriptorChannel {
    InetSocketAddress connectAddress;
    boolean connected;
    private final FileDescriptor fd;
    boolean isBound;
    InetAddress localAddress;
    private int localPort;
    private final Object readLock;
    private DatagramSocket socket;
    private final Object writeLock;

    protected DatagramChannelImpl(SelectorProvider selectorProvider) throws IOException {
        super(selectorProvider);
        this.connected = false;
        this.isBound = false;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.fd = IoBridge.socket(false);
    }

    private DatagramChannelImpl() {
        super(SelectorProvider.provider());
        this.connected = false;
        this.isBound = false;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.fd = new FileDescriptor();
        this.connectAddress = new InetSocketAddress(0);
    }

    @Override
    public synchronized DatagramSocket socket() {
        if (this.socket == null) {
            this.socket = new DatagramSocketAdapter(new PlainDatagramSocketImpl(this.fd, this.localPort), this);
        }
        return this.socket;
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
        return this.connected;
    }

    @Override
    public synchronized DatagramChannel connect(SocketAddress address) throws IOException {
        checkOpen();
        if (this.connected) {
            throw new IllegalStateException();
        }
        InetSocketAddress inetSocketAddress = SocketChannelImpl.validateAddress(address);
        InetAddress remoteAddress = inetSocketAddress.getAddress();
        int remotePort = inetSocketAddress.getPort();
        try {
            begin();
            IoBridge.connect(this.fd, remoteAddress, remotePort);
            end(true);
        } catch (ConnectException e) {
            end(true);
        } catch (Throwable th) {
            end(true);
            throw th;
        }
        if (!this.isBound) {
            onBind(true);
        }
        onConnect(remoteAddress, remotePort, true);
        return this;
    }

    void onConnect(InetAddress remoteAddress, int remotePort, boolean updateSocketState) {
        this.connected = true;
        this.connectAddress = new InetSocketAddress(remoteAddress, remotePort);
        if (updateSocketState && this.socket != null) {
            this.socket.onConnect(remoteAddress, remotePort);
        }
    }

    @Override
    public synchronized DatagramChannel disconnect() throws IOException {
        DatagramChannelImpl datagramChannelImpl;
        if (isConnected() && isOpen()) {
            onDisconnect(true);
            try {
                Libcore.os.connect(this.fd, InetAddress.UNSPECIFIED, 0);
                datagramChannelImpl = this;
            } catch (ErrnoException errnoException) {
                throw errnoException.rethrowAsIOException();
            }
        } else {
            datagramChannelImpl = this;
        }
        return datagramChannelImpl;
    }

    void onDisconnect(boolean updateSocketState) {
        this.connected = false;
        this.connectAddress = null;
        if (updateSocketState && this.socket != null && this.socket.isConnected()) {
            this.socket.onDisconnect();
        }
    }

    @Override
    public SocketAddress receive(ByteBuffer target) throws IOException {
        target.checkWritable();
        checkOpen();
        if (!this.isBound) {
            return null;
        }
        SocketAddress retAddr = null;
        try {
            begin();
            synchronized (this.readLock) {
                boolean loop = isBlocking();
                if (!target.isDirect()) {
                    retAddr = receiveImpl(target, loop);
                } else {
                    retAddr = receiveDirectImpl(target, loop);
                }
            }
            end(retAddr != null);
            return retAddr;
        } catch (InterruptedIOException e) {
            end(retAddr != null);
            return null;
        } catch (Throwable th) {
            end(retAddr != null);
            throw th;
        }
    }

    private SocketAddress receiveImpl(ByteBuffer target, boolean loop) throws IOException {
        DatagramPacket receivePacket;
        int oldposition = target.position();
        if (target.hasArray()) {
            receivePacket = new DatagramPacket(target.array(), target.position() + target.arrayOffset(), target.remaining());
        } else {
            receivePacket = new DatagramPacket(new byte[target.remaining()], target.remaining());
        }
        do {
            int received = IoBridge.recvfrom(false, this.fd, receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength(), 0, receivePacket, isConnected());
            if (receivePacket.getAddress() != null) {
                if (received > 0) {
                    if (target.hasArray()) {
                        target.position(oldposition + received);
                    } else {
                        target.put(receivePacket.getData(), 0, received);
                    }
                }
                SocketAddress retAddr = receivePacket.getSocketAddress();
                return retAddr;
            }
        } while (loop);
        return null;
    }

    private SocketAddress receiveDirectImpl(ByteBuffer target, boolean loop) throws IOException {
        DatagramPacket receivePacket = new DatagramPacket(EmptyArray.BYTE, 0);
        do {
            IoBridge.recvfrom(false, this.fd, target, 0, receivePacket, isConnected());
            if (receivePacket.getAddress() != null) {
                SocketAddress retAddr = receivePacket.getSocketAddress();
                return retAddr;
            }
        } while (loop);
        return null;
    }

    @Override
    public int send(ByteBuffer source, SocketAddress socketAddress) throws IOException {
        int sendCount;
        checkNotNull(source);
        checkOpen();
        InetSocketAddress isa = (InetSocketAddress) socketAddress;
        if (isa.getAddress() == null) {
            throw new IOException();
        }
        if (isConnected() && !this.connectAddress.equals(isa)) {
            throw new IllegalArgumentException("Connected to " + this.connectAddress + ", not " + socketAddress);
        }
        synchronized (this.writeLock) {
            sendCount = 0;
            try {
                begin();
                sendCount = IoBridge.sendto(this.fd, source, 0, isa.getAddress(), isa.getPort());
                if (!this.isBound) {
                    onBind(true);
                }
                end(sendCount >= 0);
            } catch (Throwable th) {
                end(sendCount >= 0);
                throw th;
            }
        }
        return sendCount;
    }

    @Override
    public int read(ByteBuffer target) throws IOException {
        target.checkWritable();
        checkOpenConnected();
        if (!target.hasRemaining()) {
            return 0;
        }
        if (target.isDirect() || target.hasArray()) {
            return readImpl(target);
        }
        byte[] readArray = new byte[target.remaining()];
        ByteBuffer readBuffer = ByteBuffer.wrap(readArray);
        int readCount = readImpl(readBuffer);
        if (readCount > 0) {
            target.put(readArray, 0, readCount);
            return readCount;
        }
        return readCount;
    }

    @Override
    public long read(ByteBuffer[] targets, int offset, int length) throws IOException {
        Arrays.checkOffsetAndCount(targets.length, offset, length);
        checkOpenConnected();
        int totalCount = FileChannelImpl.calculateTotalRemaining(targets, offset, length, true);
        if (totalCount == 0) {
            return 0L;
        }
        ByteBuffer readBuffer = ByteBuffer.allocate(totalCount);
        int readCount = readImpl(readBuffer);
        int left = readCount;
        int index = offset;
        byte[] readArray = readBuffer.array();
        while (left > 0) {
            int putLength = Math.min(targets[index].remaining(), left);
            targets[index].put(readArray, readCount - left, putLength);
            index++;
            left -= putLength;
        }
        return readCount;
    }

    private int readImpl(ByteBuffer dst) throws IOException {
        int readCount;
        synchronized (this.readLock) {
            try {
                try {
                    begin();
                    readCount = IoBridge.recvfrom(false, this.fd, dst, 0, null, isConnected());
                    end(readCount > 0);
                } catch (InterruptedIOException e) {
                    end(0 > 0);
                    return 0;
                }
            } catch (Throwable th) {
                end(0 > 0);
                throw th;
            }
        }
        return readCount;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkNotNull(src);
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

    private int writeImpl(ByteBuffer buf) throws IOException {
        int result;
        synchronized (this.writeLock) {
            try {
                begin();
                result = IoBridge.sendto(this.fd, buf, 0, null, 0);
                end(result > 0);
            } catch (Throwable th) {
                end(0 > 0);
                throw th;
            }
        }
        return result;
    }

    @Override
    protected synchronized void implCloseSelectableChannel() throws IOException {
        onDisconnect(true);
        IoBridge.closeAndSignalBlockedThreads(this.fd);
        if (this.socket != null && !this.socket.isClosed()) {
            this.socket.onClose();
        }
    }

    @Override
    protected void implConfigureBlocking(boolean blocking) throws IOException {
        IoUtils.setBlocking(this.fd, blocking);
    }

    private void checkOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    private void checkOpenConnected() throws IOException {
        checkOpen();
        if (!isConnected()) {
            throw new NotYetConnectedException();
        }
    }

    private void checkNotNull(ByteBuffer source) {
        if (source == null) {
            throw new NullPointerException("source == null");
        }
    }

    @Override
    public FileDescriptor getFD() {
        return this.fd;
    }

    private static class DatagramSocketAdapter extends DatagramSocket {
        private final DatagramChannelImpl channelImpl;

        DatagramSocketAdapter(DatagramSocketImpl socketimpl, DatagramChannelImpl channelImpl) {
            super(socketimpl);
            this.channelImpl = channelImpl;
            if (channelImpl.isBound) {
                onBind(channelImpl.localAddress, channelImpl.localPort);
            }
            if (channelImpl.connected) {
                onConnect(channelImpl.connectAddress.getAddress(), channelImpl.connectAddress.getPort());
            } else {
                onDisconnect();
            }
            if (!channelImpl.isOpen()) {
                onClose();
            }
        }

        @Override
        public DatagramChannel getChannel() {
            return this.channelImpl;
        }

        @Override
        public void bind(SocketAddress localAddr) throws SocketException {
            if (this.channelImpl.isConnected()) {
                throw new AlreadyConnectedException();
            }
            super.bind(localAddr);
            this.channelImpl.onBind(false);
        }

        @Override
        public void connect(SocketAddress peer) throws SocketException {
            if (isConnected()) {
                throw new IllegalStateException("Socket is already connected.");
            }
            super.connect(peer);
            this.channelImpl.onBind(false);
            InetSocketAddress inetSocketAddress = (InetSocketAddress) peer;
            this.channelImpl.onConnect(inetSocketAddress.getAddress(), inetSocketAddress.getPort(), false);
        }

        @Override
        public void connect(InetAddress address, int port) {
            try {
                connect(new InetSocketAddress(address, port));
            } catch (SocketException e) {
            }
        }

        @Override
        public void receive(DatagramPacket packet) throws IOException {
            if (!this.channelImpl.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            boolean wasBound = isBound();
            super.receive(packet);
            if (!wasBound) {
                this.channelImpl.onBind(false);
            }
        }

        @Override
        public void send(DatagramPacket packet) throws IOException {
            if (!this.channelImpl.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            boolean wasBound = isBound();
            super.send(packet);
            if (!wasBound) {
                this.channelImpl.onBind(false);
            }
        }

        @Override
        public void close() {
            synchronized (this.channelImpl) {
                super.close();
                if (this.channelImpl.isOpen()) {
                    try {
                        this.channelImpl.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        @Override
        public void disconnect() {
            super.disconnect();
            this.channelImpl.onDisconnect(false);
        }
    }
}
