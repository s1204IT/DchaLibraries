package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;

public class DatagramSocketAdaptor extends DatagramSocket {
    private static final DatagramSocketImpl dummyDatagramSocket = new DatagramSocketImpl() {
        @Override
        protected void create() throws SocketException {
        }

        @Override
        protected void bind(int i, InetAddress inetAddress) throws SocketException {
        }

        @Override
        protected void send(DatagramPacket datagramPacket) throws IOException {
        }

        @Override
        protected int peek(InetAddress inetAddress) throws IOException {
            return 0;
        }

        @Override
        protected int peekData(DatagramPacket datagramPacket) throws IOException {
            return 0;
        }

        @Override
        protected void receive(DatagramPacket datagramPacket) throws IOException {
        }

        @Override
        @Deprecated
        protected void setTTL(byte b) throws IOException {
        }

        @Override
        @Deprecated
        protected byte getTTL() throws IOException {
            return (byte) 0;
        }

        @Override
        protected void setTimeToLive(int i) throws IOException {
        }

        @Override
        protected int getTimeToLive() throws IOException {
            return 0;
        }

        @Override
        protected void join(InetAddress inetAddress) throws IOException {
        }

        @Override
        protected void leave(InetAddress inetAddress) throws IOException {
        }

        @Override
        protected void joinGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException {
        }

        @Override
        protected void leaveGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException {
        }

        @Override
        protected void close() {
        }

        @Override
        public Object getOption(int i) throws SocketException {
            return null;
        }

        @Override
        public void setOption(int i, Object obj) throws SocketException {
        }
    };
    private final DatagramChannelImpl dc;
    private volatile int timeout;

    private DatagramSocketAdaptor(DatagramChannelImpl datagramChannelImpl) throws IOException {
        super(dummyDatagramSocket);
        this.timeout = 0;
        this.dc = datagramChannelImpl;
    }

    public static DatagramSocket create(DatagramChannelImpl datagramChannelImpl) {
        try {
            return new DatagramSocketAdaptor(datagramChannelImpl);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void connectInternal(SocketAddress socketAddress) throws SocketException {
        int port = Net.asInetSocketAddress(socketAddress).getPort();
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("connect: " + port);
        }
        if (socketAddress == null) {
            throw new IllegalArgumentException("connect: null address");
        }
        if (isClosed()) {
            return;
        }
        try {
            this.dc.connect(socketAddress);
        } catch (Exception e) {
            Net.translateToSocketException(e);
        }
    }

    @Override
    public void bind(SocketAddress socketAddress) throws SocketException {
        if (socketAddress == null) {
            try {
                socketAddress = new InetSocketAddress(0);
            } catch (Exception e) {
                Net.translateToSocketException(e);
                return;
            }
        }
        this.dc.bind(socketAddress);
    }

    @Override
    public void connect(InetAddress inetAddress, int i) {
        try {
            connectInternal(new InetSocketAddress(inetAddress, i));
        } catch (SocketException e) {
        }
    }

    @Override
    public void connect(SocketAddress socketAddress) throws SocketException {
        if (socketAddress == null) {
            throw new IllegalArgumentException("Address can't be null");
        }
        connectInternal(socketAddress);
    }

    @Override
    public void disconnect() {
        try {
            this.dc.disconnect();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public boolean isBound() {
        return this.dc.localAddress() != null;
    }

    @Override
    public boolean isConnected() {
        return this.dc.remoteAddress() != null;
    }

    @Override
    public InetAddress getInetAddress() {
        if (isConnected()) {
            return Net.asInetSocketAddress(this.dc.remoteAddress()).getAddress();
        }
        return null;
    }

    @Override
    public int getPort() {
        if (isConnected()) {
            return Net.asInetSocketAddress(this.dc.remoteAddress()).getPort();
        }
        return -1;
    }

    @Override
    public void send(DatagramPacket datagramPacket) throws IOException {
        synchronized (this.dc.blockingLock()) {
            if (!this.dc.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            try {
                synchronized (datagramPacket) {
                    ByteBuffer byteBufferWrap = ByteBuffer.wrap(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());
                    if (this.dc.isConnected() && datagramPacket.getAddress() == null) {
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) this.dc.remoteAddress();
                        datagramPacket.setPort(inetSocketAddress.getPort());
                        datagramPacket.setAddress(inetSocketAddress.getAddress());
                        this.dc.write(byteBufferWrap);
                    } else {
                        this.dc.send(byteBufferWrap, datagramPacket.getSocketAddress());
                    }
                }
            } catch (IOException e) {
                Net.translateException(e);
            }
        }
    }

    private SocketAddress receive(ByteBuffer byteBuffer) throws IOException {
        SocketAddress socketAddressReceive;
        if (this.timeout == 0) {
            return this.dc.receive(byteBuffer);
        }
        this.dc.configureBlocking(false);
        try {
            SocketAddress socketAddressReceive2 = this.dc.receive(byteBuffer);
            if (socketAddressReceive2 != null) {
                return socketAddressReceive2;
            }
            long jCurrentTimeMillis = this.timeout;
            while (this.dc.isOpen()) {
                long jCurrentTimeMillis2 = System.currentTimeMillis();
                int iPoll = this.dc.poll(Net.POLLIN, jCurrentTimeMillis);
                if (iPoll <= 0 || (iPoll & Net.POLLIN) == 0 || (socketAddressReceive = this.dc.receive(byteBuffer)) == null) {
                    jCurrentTimeMillis -= System.currentTimeMillis() - jCurrentTimeMillis2;
                    if (jCurrentTimeMillis <= 0) {
                        throw new SocketTimeoutException();
                    }
                } else {
                    if (this.dc.isOpen()) {
                        this.dc.configureBlocking(true);
                    }
                    return socketAddressReceive;
                }
            }
            throw new ClosedChannelException();
        } finally {
            if (this.dc.isOpen()) {
                this.dc.configureBlocking(true);
            }
        }
    }

    @Override
    public void receive(DatagramPacket datagramPacket) throws IOException {
        synchronized (this.dc.blockingLock()) {
            if (!this.dc.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            try {
                synchronized (datagramPacket) {
                    ByteBuffer byteBufferWrap = ByteBuffer.wrap(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());
                    datagramPacket.setSocketAddress(receive(byteBufferWrap));
                    datagramPacket.setLength(byteBufferWrap.position() - datagramPacket.getOffset());
                }
            } catch (IOException e) {
                Net.translateException(e);
            }
        }
    }

    @Override
    public InetAddress getLocalAddress() {
        if (isClosed()) {
            return null;
        }
        SocketAddress socketAddressLocalAddress = this.dc.localAddress();
        if (socketAddressLocalAddress == null) {
            socketAddressLocalAddress = new InetSocketAddress(0);
        }
        InetAddress address = ((InetSocketAddress) socketAddressLocalAddress).getAddress();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            try {
                securityManager.checkConnect(address.getHostAddress(), -1);
            } catch (SecurityException e) {
                return new InetSocketAddress(0).getAddress();
            }
        }
        return address;
    }

    @Override
    public int getLocalPort() {
        if (isClosed()) {
            return -1;
        }
        try {
            SocketAddress localAddress = this.dc.getLocalAddress();
            if (localAddress != null) {
                return ((InetSocketAddress) localAddress).getPort();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setSoTimeout(int i) throws SocketException {
        this.timeout = i;
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return this.timeout;
    }

    private void setBooleanOption(SocketOption<Boolean> socketOption, boolean z) throws SocketException {
        try {
            this.dc.setOption(socketOption, Boolean.valueOf(z));
        } catch (IOException e) {
            Net.translateToSocketException(e);
        }
    }

    private void setIntOption(SocketOption<Integer> socketOption, int i) throws SocketException {
        try {
            this.dc.setOption(socketOption, Integer.valueOf(i));
        } catch (IOException e) {
            Net.translateToSocketException(e);
        }
    }

    private boolean getBooleanOption(SocketOption<Boolean> socketOption) throws SocketException {
        try {
            return ((Boolean) this.dc.getOption(socketOption)).booleanValue();
        } catch (IOException e) {
            Net.translateToSocketException(e);
            return false;
        }
    }

    private int getIntOption(SocketOption<Integer> socketOption) throws SocketException {
        try {
            return ((Integer) this.dc.getOption(socketOption)).intValue();
        } catch (IOException e) {
            Net.translateToSocketException(e);
            return -1;
        }
    }

    @Override
    public void setSendBufferSize(int i) throws SocketException {
        if (i <= 0) {
            throw new IllegalArgumentException("Invalid send size");
        }
        setIntOption(StandardSocketOptions.SO_SNDBUF, i);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_SNDBUF);
    }

    @Override
    public void setReceiveBufferSize(int i) throws SocketException {
        if (i <= 0) {
            throw new IllegalArgumentException("Invalid receive size");
        }
        setIntOption(StandardSocketOptions.SO_RCVBUF, i);
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_RCVBUF);
    }

    @Override
    public void setReuseAddress(boolean z) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_REUSEADDR, z);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
    }

    @Override
    public void setBroadcast(boolean z) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_BROADCAST, z);
    }

    @Override
    public boolean getBroadcast() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_BROADCAST);
    }

    @Override
    public void setTrafficClass(int i) throws SocketException {
        setIntOption(StandardSocketOptions.IP_TOS, i);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return getIntOption(StandardSocketOptions.IP_TOS);
    }

    @Override
    public void close() {
        try {
            this.dc.close();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public boolean isClosed() {
        return !this.dc.isOpen();
    }

    @Override
    public DatagramChannel getChannel() {
        return this.dc;
    }

    @Override
    public final FileDescriptor getFileDescriptor$() {
        return this.dc.fd;
    }
}
