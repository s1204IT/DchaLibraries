package sun.nio.ch;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.NetworkChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jdk.net.ExtendedSocketOptions;
import sun.net.ExtendedOptionsImpl;
import sun.net.ResourceManager;
import sun.nio.ch.MembershipKeyImpl;

class DatagramChannelImpl extends DatagramChannel implements SelChImpl {
    static final boolean $assertionsDisabled = false;
    private static final int ST_CONNECTED = 1;
    private static final int ST_KILLED = 2;
    private static final int ST_UNCONNECTED = 0;
    private static final int ST_UNINITIALIZED = -1;
    private static NativeDispatcher nd = new DatagramDispatcher();
    private InetAddress cachedSenderInetAddress;
    private int cachedSenderPort;
    private final ProtocolFamily family;

    @ReachabilitySensitive
    final FileDescriptor fd;
    private final int fdVal;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private boolean isReuseAddress;
    private InetSocketAddress localAddress;
    private final Object readLock;
    private volatile long readerThread;
    private MembershipRegistry registry;
    private InetSocketAddress remoteAddress;
    private boolean reuseAddressEmulated;
    private SocketAddress sender;
    private DatagramSocket socket;
    private int state;
    private final Object stateLock;
    private final Object writeLock;
    private volatile long writerThread;

    private static native void disconnect0(FileDescriptor fileDescriptor, boolean z) throws IOException;

    private static native void initIDs();

    private native int receive0(FileDescriptor fileDescriptor, long j, int i, boolean z) throws IOException;

    private native int send0(boolean z, FileDescriptor fileDescriptor, long j, int i, InetAddress inetAddress, int i2) throws IOException;

    static {
        initIDs();
    }

    @Override
    public NetworkChannel setOption(SocketOption socketOption, Object obj) throws IOException {
        return setOption((SocketOption<Object>) socketOption, obj);
    }

    public DatagramChannelImpl(SelectorProvider selectorProvider) throws IOException {
        super(selectorProvider);
        this.readerThread = 0L;
        this.writerThread = 0L;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.guard = CloseGuard.get();
        ResourceManager.beforeUdpCreate();
        try {
            this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
            this.fd = Net.socket(this.family, $assertionsDisabled);
            this.fdVal = IOUtil.fdVal(this.fd);
            this.state = 0;
            if (this.fd != null && this.fd.valid()) {
                this.guard.open("close");
            }
        } catch (IOException e) {
            ResourceManager.afterUdpClose();
            throw e;
        }
    }

    public DatagramChannelImpl(SelectorProvider selectorProvider, ProtocolFamily protocolFamily) throws IOException {
        super(selectorProvider);
        this.readerThread = 0L;
        this.writerThread = 0L;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.guard = CloseGuard.get();
        if (protocolFamily != StandardProtocolFamily.INET && protocolFamily != StandardProtocolFamily.INET6) {
            if (protocolFamily == null) {
                throw new NullPointerException("'family' is null");
            }
            throw new UnsupportedOperationException("Protocol family not supported");
        }
        if (protocolFamily == StandardProtocolFamily.INET6 && !Net.isIPv6Available()) {
            throw new UnsupportedOperationException("IPv6 not available");
        }
        this.family = protocolFamily;
        this.fd = Net.socket(protocolFamily, $assertionsDisabled);
        this.fdVal = IOUtil.fdVal(this.fd);
        this.state = 0;
        if (this.fd != null && this.fd.valid()) {
            this.guard.open("close");
        }
    }

    public DatagramChannelImpl(SelectorProvider selectorProvider, FileDescriptor fileDescriptor) throws IOException {
        super(selectorProvider);
        this.readerThread = 0L;
        this.writerThread = 0L;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.guard = CloseGuard.get();
        this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
        this.fd = fileDescriptor;
        this.fdVal = IOUtil.fdVal(fileDescriptor);
        this.state = 0;
        this.localAddress = Net.localAddress(fileDescriptor);
        if (fileDescriptor != null && fileDescriptor.valid()) {
            this.guard.open("close");
        }
    }

    @Override
    public DatagramSocket socket() {
        DatagramSocket datagramSocket;
        synchronized (this.stateLock) {
            if (this.socket == null) {
                this.socket = DatagramSocketAdaptor.create(this);
            }
            datagramSocket = this.socket;
        }
        return datagramSocket;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        InetSocketAddress revealedLocalAddress;
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            revealedLocalAddress = Net.getRevealedLocalAddress(this.localAddress);
        }
        return revealedLocalAddress;
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            inetSocketAddress = this.remoteAddress;
        }
        return inetSocketAddress;
    }

    @Override
    public <T> DatagramChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
        if (socketOption == null) {
            throw new NullPointerException();
        }
        if (!supportedOptions().contains(socketOption)) {
            throw new UnsupportedOperationException("'" + ((Object) socketOption) + "' not supported");
        }
        synchronized (this.stateLock) {
            ensureOpen();
            if (socketOption != StandardSocketOptions.IP_TOS && socketOption != StandardSocketOptions.IP_MULTICAST_TTL && socketOption != StandardSocketOptions.IP_MULTICAST_LOOP) {
                if (socketOption == StandardSocketOptions.IP_MULTICAST_IF) {
                    if (t == 0) {
                        throw new IllegalArgumentException("Cannot set IP_MULTICAST_IF to 'null'");
                    }
                    NetworkInterface networkInterface = (NetworkInterface) t;
                    if (this.family == StandardProtocolFamily.INET6) {
                        int index = networkInterface.getIndex();
                        if (index == -1) {
                            throw new IOException("Network interface cannot be identified");
                        }
                        Net.setInterface6(this.fd, index);
                    } else {
                        Inet4Address inet4AddressAnyInet4Address = Net.anyInet4Address(networkInterface);
                        if (inet4AddressAnyInet4Address == null) {
                            throw new IOException("Network interface not configured for IPv4");
                        }
                        Net.setInterface4(this.fd, Net.inet4AsInt(inet4AddressAnyInet4Address));
                    }
                    return this;
                }
                if (socketOption == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind() && this.localAddress != null) {
                    this.reuseAddressEmulated = true;
                    this.isReuseAddress = ((Boolean) t).booleanValue();
                }
                Net.setSocketOption(this.fd, Net.UNSPEC, socketOption, t);
                return this;
            }
            Net.setSocketOption(this.fd, this.family, socketOption, t);
            return this;
        }
    }

    @Override
    public <T> T getOption(SocketOption<T> socketOption) throws IOException {
        if (socketOption == null) {
            throw new NullPointerException();
        }
        if (!supportedOptions().contains(socketOption)) {
            throw new UnsupportedOperationException("'" + ((Object) socketOption) + "' not supported");
        }
        synchronized (this.stateLock) {
            ensureOpen();
            if (socketOption != StandardSocketOptions.IP_TOS && socketOption != StandardSocketOptions.IP_MULTICAST_TTL && socketOption != StandardSocketOptions.IP_MULTICAST_LOOP) {
                if (socketOption == StandardSocketOptions.IP_MULTICAST_IF) {
                    if (this.family == StandardProtocolFamily.INET) {
                        int interface4 = Net.getInterface4(this.fd);
                        if (interface4 == 0) {
                            return null;
                        }
                        T t = (T) NetworkInterface.getByInetAddress(Net.inet4FromInt(interface4));
                        if (t != null) {
                            return t;
                        }
                        throw new IOException("Unable to map address to interface");
                    }
                    int interface6 = Net.getInterface6(this.fd);
                    if (interface6 == 0) {
                        return null;
                    }
                    T t2 = (T) NetworkInterface.getByIndex(interface6);
                    if (t2 != null) {
                        return t2;
                    }
                    throw new IOException("Unable to map index to interface");
                }
                if (socketOption == StandardSocketOptions.SO_REUSEADDR && this.reuseAddressEmulated) {
                    return (T) Boolean.valueOf(this.isReuseAddress);
                }
                return (T) Net.getSocketOption(this.fd, Net.UNSPEC, socketOption);
            }
            return (T) Net.getSocketOption(this.fd, this.family, socketOption);
        }
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private DefaultOptionsHolder() {
        }

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet hashSet = new HashSet(8);
            hashSet.add(StandardSocketOptions.SO_SNDBUF);
            hashSet.add(StandardSocketOptions.SO_RCVBUF);
            hashSet.add(StandardSocketOptions.SO_REUSEADDR);
            hashSet.add(StandardSocketOptions.SO_BROADCAST);
            hashSet.add(StandardSocketOptions.IP_TOS);
            hashSet.add(StandardSocketOptions.IP_MULTICAST_IF);
            hashSet.add(StandardSocketOptions.IP_MULTICAST_TTL);
            hashSet.add(StandardSocketOptions.IP_MULTICAST_LOOP);
            if (ExtendedOptionsImpl.flowSupported()) {
                hashSet.add(ExtendedSocketOptions.SO_FLOW_SLA);
            }
            return Collections.unmodifiableSet(hashSet);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    @Override
    public SocketAddress receive(ByteBuffer byteBuffer) throws IOException {
        ?? r11;
        ?? Receive;
        int iReceive;
        if (byteBuffer.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        if (byteBuffer == 0) {
            throw new NullPointerException();
        }
        ?? r1 = 0;
         = 0;
        ?? r12 = 0;
        r1 = 0;
        if (this.localAddress == null) {
            return null;
        }
        synchronized (this.readLock) {
            ensureOpen();
            boolean z = true;
            try {
                begin();
                if (!isOpen()) {
                    this.readerThread = 0L;
                    end($assertionsDisabled);
                    return null;
                }
                SecurityManager securityManager = System.getSecurityManager();
                this.readerThread = NativeThread.current();
                if (!isConnected() && securityManager != null) {
                    Receive = Util.getTemporaryDirectBuffer(byteBuffer.remaining());
                    while (true) {
                        int i = 0;
                        while (true) {
                            try {
                                iReceive = receive(this.fd, Receive);
                                if (iReceive != -3) {
                                    break;
                                }
                                try {
                                    if (!isOpen()) {
                                        break;
                                    }
                                    i = iReceive;
                                } catch (Throwable th) {
                                    th = th;
                                    r1 = Receive;
                                    r11 = iReceive;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                r1 = Receive;
                                r11 = i;
                            }
                        }
                        if (iReceive == -2) {
                            if (Receive != 0) {
                                Util.releaseTemporaryDirectBuffer(Receive);
                            }
                            this.readerThread = 0L;
                            if (iReceive <= 0 && iReceive != -2) {
                                z = false;
                            }
                            end(z);
                            return null;
                        }
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) this.sender;
                        try {
                            securityManager.checkAccept(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
                            break;
                        } catch (SecurityException e) {
                            Receive.clear();
                        }
                    }
                }
                ?? r7 = 0;
                while (true) {
                    try {
                        Receive = receive(this.fd, byteBuffer);
                        if (Receive != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            r7 = Receive;
                        } catch (Throwable th3) {
                            th = th3;
                            r11 = Receive;
                            r1 = r12;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        r11 = r7;
                    }
                }
                if (Receive == -2) {
                    this.readerThread = 0L;
                    if (Receive <= 0 && Receive != -2) {
                        z = false;
                    }
                    end(z);
                    return null;
                }
                SocketAddress socketAddress = this.sender;
                if (r12 != 0) {
                    Util.releaseTemporaryDirectBuffer(r12);
                }
                this.readerThread = 0L;
                if (Receive <= 0 && Receive != -2) {
                    z = false;
                }
                end(z);
                return socketAddress;
            } catch (Throwable th5) {
                th = th5;
                r11 = 0;
            }
            if (r1 != 0) {
                Util.releaseTemporaryDirectBuffer(r1);
            }
            this.readerThread = 0L;
            if (r11 <= 0 && r11 != -2) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    private int receive(FileDescriptor fileDescriptor, ByteBuffer byteBuffer) throws IOException {
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if ((byteBuffer instanceof DirectBuffer) && i > 0) {
            return receiveIntoNativeBuffer(fileDescriptor, byteBuffer, i, iPosition);
        }
        int iMax = Math.max(i, 1);
        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer(iMax);
        try {
            BlockGuard.getThreadPolicy().onNetwork();
            int iReceiveIntoNativeBuffer = receiveIntoNativeBuffer(fileDescriptor, temporaryDirectBuffer, iMax, 0);
            temporaryDirectBuffer.flip();
            if (iReceiveIntoNativeBuffer > 0 && i > 0) {
                byteBuffer.put(temporaryDirectBuffer);
            }
            return iReceiveIntoNativeBuffer;
        } finally {
            Util.releaseTemporaryDirectBuffer(temporaryDirectBuffer);
        }
    }

    private int receiveIntoNativeBuffer(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, int i2) throws IOException {
        int iReceive0 = receive0(fileDescriptor, ((DirectBuffer) byteBuffer).address() + ((long) i2), i, isConnected());
        if (iReceive0 > 0) {
            byteBuffer.position(i2 + iReceive0);
        }
        return iReceive0;
    }

    @Override
    public int send(ByteBuffer byteBuffer, SocketAddress socketAddress) throws IOException {
        int iSend;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        synchronized (this.writeLock) {
            ensureOpen();
            InetSocketAddress inetSocketAddressCheckAddress = Net.checkAddress(socketAddress);
            InetAddress address = inetSocketAddressCheckAddress.getAddress();
            if (address == null) {
                throw new IOException("Target address not resolved");
            }
            synchronized (this.stateLock) {
                if (isConnected()) {
                    if (!socketAddress.equals(this.remoteAddress)) {
                        throw new IllegalArgumentException("Connected address not equal to target address");
                    }
                    return write(byteBuffer);
                }
                if (socketAddress == null) {
                    throw new NullPointerException();
                }
                SecurityManager securityManager = System.getSecurityManager();
                if (securityManager != null) {
                    if (address.isMulticastAddress()) {
                        securityManager.checkMulticast(address);
                    } else {
                        securityManager.checkConnect(address.getHostAddress(), inetSocketAddressCheckAddress.getPort());
                    }
                }
                boolean z = true;
                try {
                    begin();
                    if (!isOpen()) {
                        this.writerThread = 0L;
                        end($assertionsDisabled);
                        return 0;
                    }
                    this.writerThread = NativeThread.current();
                    BlockGuard.getThreadPolicy().onNetwork();
                    int i = 0;
                    while (true) {
                        try {
                            iSend = send(this.fd, byteBuffer, inetSocketAddressCheckAddress);
                            if (iSend != -3) {
                                break;
                            }
                            try {
                                if (!isOpen()) {
                                    break;
                                }
                                i = iSend;
                            } catch (Throwable th) {
                                th = th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            iSend = i;
                        }
                    }
                    synchronized (this.stateLock) {
                        if (isOpen() && this.localAddress == null) {
                            this.localAddress = Net.localAddress(this.fd);
                        }
                    }
                    int iNormalize = IOStatus.normalize(iSend);
                    this.writerThread = 0L;
                    if (iSend <= 0 && iSend != -2) {
                        z = false;
                    }
                    end(z);
                    return iNormalize;
                } catch (Throwable th3) {
                    th = th3;
                    iSend = 0;
                }
                this.writerThread = 0L;
                if (iSend <= 0 && iSend != -2) {
                    z = false;
                }
                end(z);
                throw th;
            }
        }
    }

    private int send(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, InetSocketAddress inetSocketAddress) throws IOException {
        if (byteBuffer instanceof DirectBuffer) {
            return sendFromNativeBuffer(fileDescriptor, byteBuffer, inetSocketAddress);
        }
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer(iPosition <= iLimit ? iLimit - iPosition : 0);
        try {
            temporaryDirectBuffer.put(byteBuffer);
            temporaryDirectBuffer.flip();
            byteBuffer.position(iPosition);
            int iSendFromNativeBuffer = sendFromNativeBuffer(fileDescriptor, temporaryDirectBuffer, inetSocketAddress);
            if (iSendFromNativeBuffer > 0) {
                byteBuffer.position(iPosition + iSendFromNativeBuffer);
            }
            return iSendFromNativeBuffer;
        } finally {
            Util.releaseTemporaryDirectBuffer(temporaryDirectBuffer);
        }
    }

    private int sendFromNativeBuffer(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, InetSocketAddress inetSocketAddress) throws IOException {
        int iSend0;
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        boolean z = $assertionsDisabled;
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (this.family != StandardProtocolFamily.INET) {
            z = true;
        }
        try {
            iSend0 = send0(z, fileDescriptor, ((DirectBuffer) byteBuffer).address() + ((long) iPosition), i, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
        } catch (PortUnreachableException e) {
            if (isConnected()) {
                throw e;
            }
            iSend0 = i;
        }
        if (iSend0 > 0) {
            byteBuffer.position(iPosition + iSend0);
        }
        return iSend0;
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        int i;
        int i2;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        synchronized (this.readLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (!isConnected()) {
                    throw new NotYetConnectedException();
                }
            }
            boolean z = true;
            try {
                begin();
                if (!isOpen()) {
                    this.readerThread = 0L;
                    end($assertionsDisabled);
                    return 0;
                }
                this.readerThread = NativeThread.current();
                i = 0;
                while (true) {
                    try {
                        i2 = IOUtil.read(this.fd, byteBuffer, -1L, nd);
                        if (i2 != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            i = i2;
                        } catch (Throwable th) {
                            th = th;
                            i = i2;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                int iNormalize = IOStatus.normalize(i2);
                this.readerThread = 0L;
                if (i2 <= 0 && i2 != -2) {
                    z = false;
                }
                end(z);
                return iNormalize;
            } catch (Throwable th3) {
                th = th3;
                i = 0;
            }
            this.readerThread = 0L;
            if (i <= 0 && i != -2) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    public long read(ByteBuffer[] byteBufferArr, int i, int i2) throws IOException {
        long j;
        long j2;
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.readLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (!isConnected()) {
                    throw new NotYetConnectedException();
                }
            }
            boolean z = true;
            try {
                begin();
                if (!isOpen()) {
                    this.readerThread = 0L;
                    end($assertionsDisabled);
                    return 0L;
                }
                this.readerThread = NativeThread.current();
                j = 0;
                while (true) {
                    try {
                        j2 = IOUtil.read(this.fd, byteBufferArr, i, i2, nd);
                        if (j2 != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            j = j2;
                        } catch (Throwable th) {
                            th = th;
                            j = j2;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                long jNormalize = IOStatus.normalize(j2);
                this.readerThread = 0L;
                if (j2 <= 0 && j2 != -2) {
                    z = false;
                }
                end(z);
                return jNormalize;
            } catch (Throwable th3) {
                th = th3;
                j = 0;
            }
            this.readerThread = 0L;
            if (j <= 0 && j != -2) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        int i;
        int iWrite;
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        synchronized (this.writeLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (!isConnected()) {
                    throw new NotYetConnectedException();
                }
            }
            boolean z = true;
            try {
                begin();
                if (!isOpen()) {
                    this.writerThread = 0L;
                    end($assertionsDisabled);
                    return 0;
                }
                this.writerThread = NativeThread.current();
                i = 0;
                while (true) {
                    try {
                        iWrite = IOUtil.write(this.fd, byteBuffer, -1L, nd);
                        if (iWrite != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            i = iWrite;
                        } catch (Throwable th) {
                            th = th;
                            i = iWrite;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                int iNormalize = IOStatus.normalize(iWrite);
                this.writerThread = 0L;
                if (iWrite <= 0 && iWrite != -2) {
                    z = false;
                }
                end(z);
                return iNormalize;
            } catch (Throwable th3) {
                th = th3;
                i = 0;
            }
            this.writerThread = 0L;
            if (i <= 0 && i != -2) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    public long write(ByteBuffer[] byteBufferArr, int i, int i2) throws IOException {
        long j;
        long jWrite;
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.writeLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (!isConnected()) {
                    throw new NotYetConnectedException();
                }
            }
            boolean z = true;
            try {
                begin();
                if (!isOpen()) {
                    this.writerThread = 0L;
                    end($assertionsDisabled);
                    return 0L;
                }
                this.writerThread = NativeThread.current();
                j = 0;
                while (true) {
                    try {
                        jWrite = IOUtil.write(this.fd, byteBufferArr, i, i2, nd);
                        if (jWrite != -3) {
                            break;
                        }
                        try {
                            if (!isOpen()) {
                                break;
                            }
                            j = jWrite;
                        } catch (Throwable th) {
                            th = th;
                            j = jWrite;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                long jNormalize = IOStatus.normalize(jWrite);
                this.writerThread = 0L;
                if (jWrite <= 0 && jWrite != -2) {
                    z = false;
                }
                end(z);
                return jNormalize;
            } catch (Throwable th3) {
                th = th3;
                j = 0;
            }
            this.writerThread = 0L;
            if (j <= 0 && j != -2) {
                z = false;
            }
            end(z);
            throw th;
        }
    }

    @Override
    protected void implConfigureBlocking(boolean z) throws IOException {
        IOUtil.configureBlocking(this.fd, z);
    }

    public SocketAddress localAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.localAddress;
        }
        return inetSocketAddress;
    }

    public SocketAddress remoteAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.remoteAddress;
        }
        return inetSocketAddress;
    }

    @Override
    public DatagramChannel bind(SocketAddress socketAddress) throws IOException {
        InetSocketAddress inetSocketAddressCheckAddress;
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpen();
                    if (this.localAddress != null) {
                        throw new AlreadyBoundException();
                    }
                    if (socketAddress == null) {
                        inetSocketAddressCheckAddress = this.family == StandardProtocolFamily.INET ? new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0) : new InetSocketAddress(0);
                    } else {
                        inetSocketAddressCheckAddress = Net.checkAddress(socketAddress);
                        if (this.family == StandardProtocolFamily.INET && !(inetSocketAddressCheckAddress.getAddress() instanceof Inet4Address)) {
                            throw new UnsupportedAddressTypeException();
                        }
                    }
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        securityManager.checkListen(inetSocketAddressCheckAddress.getPort());
                    }
                    Net.bind(this.family, this.fd, inetSocketAddressCheckAddress.getAddress(), inetSocketAddressCheckAddress.getPort());
                    this.localAddress = Net.localAddress(this.fd);
                }
            }
        }
        return this;
    }

    @Override
    public boolean isConnected() {
        boolean z;
        synchronized (this.stateLock) {
            z = true;
            if (this.state != 1) {
                z = $assertionsDisabled;
            }
        }
        return z;
    }

    void ensureOpenAndUnconnected() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.state != 0) {
                throw new IllegalStateException("Connect already invoked");
            }
        }
    }

    @Override
    public DatagramChannel connect(SocketAddress socketAddress) throws IOException {
        boolean zIsBlocking;
        Throwable th;
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpenAndUnconnected();
                    InetSocketAddress inetSocketAddressCheckAddress = Net.checkAddress(socketAddress);
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        securityManager.checkConnect(inetSocketAddressCheckAddress.getAddress().getHostAddress(), inetSocketAddressCheckAddress.getPort());
                    }
                    if (Net.connect(this.family, this.fd, inetSocketAddressCheckAddress.getAddress(), inetSocketAddressCheckAddress.getPort()) <= 0) {
                        throw new Error();
                    }
                    this.state = 1;
                    this.remoteAddress = inetSocketAddressCheckAddress;
                    this.sender = inetSocketAddressCheckAddress;
                    this.cachedSenderInetAddress = inetSocketAddressCheckAddress.getAddress();
                    this.cachedSenderPort = inetSocketAddressCheckAddress.getPort();
                    this.localAddress = Net.localAddress(this.fd);
                    synchronized (blockingLock()) {
                        try {
                            try {
                                zIsBlocking = isBlocking();
                                try {
                                    ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1);
                                    if (zIsBlocking) {
                                        configureBlocking($assertionsDisabled);
                                    }
                                    do {
                                        byteBufferAllocate.clear();
                                    } while (receive(byteBufferAllocate) != null);
                                    if (zIsBlocking) {
                                        configureBlocking(true);
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    if (zIsBlocking) {
                                        configureBlocking(true);
                                    }
                                    throw th;
                                }
                            } finally {
                            }
                        } catch (Throwable th3) {
                            zIsBlocking = false;
                            th = th3;
                        }
                    }
                }
            }
        }
        return this;
    }

    @Override
    public DatagramChannel disconnect() throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    if (isConnected() && isOpen()) {
                        InetSocketAddress inetSocketAddress = this.remoteAddress;
                        SecurityManager securityManager = System.getSecurityManager();
                        if (securityManager != null) {
                            securityManager.checkConnect(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
                        }
                        disconnect0(this.fd, this.family == StandardProtocolFamily.INET6);
                        this.remoteAddress = null;
                        this.state = 0;
                        this.localAddress = Net.localAddress(this.fd);
                        return this;
                    }
                    return this;
                }
            }
        }
    }

    private MembershipKey innerJoin(InetAddress inetAddress, NetworkInterface networkInterface, InetAddress inetAddress2) throws IOException {
        MembershipKeyImpl type4;
        if (!inetAddress.isMulticastAddress()) {
            throw new IllegalArgumentException("Group not a multicast address");
        }
        if (inetAddress instanceof Inet4Address) {
            if (this.family == StandardProtocolFamily.INET6 && !Net.canIPv6SocketJoinIPv4Group()) {
                throw new IllegalArgumentException("IPv6 socket cannot join IPv4 multicast group");
            }
        } else if (inetAddress instanceof Inet6Address) {
            if (this.family != StandardProtocolFamily.INET6) {
                throw new IllegalArgumentException("Only IPv6 sockets can join IPv6 multicast group");
            }
        } else {
            throw new IllegalArgumentException("Address type not supported");
        }
        if (inetAddress2 != null) {
            if (inetAddress2.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Source address is a wildcard address");
            }
            if (inetAddress2.isMulticastAddress()) {
                throw new IllegalArgumentException("Source address is multicast address");
            }
            if (inetAddress2.getClass() != inetAddress.getClass()) {
                throw new IllegalArgumentException("Source address is different type to group");
            }
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkMulticast(inetAddress);
        }
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.registry == null) {
                this.registry = new MembershipRegistry();
            } else {
                MembershipKey membershipKeyCheckMembership = this.registry.checkMembership(inetAddress, networkInterface, inetAddress2);
                if (membershipKeyCheckMembership != null) {
                    return membershipKeyCheckMembership;
                }
            }
            if (this.family == StandardProtocolFamily.INET6 && ((inetAddress instanceof Inet6Address) || Net.canJoin6WithIPv4Group())) {
                int index = networkInterface.getIndex();
                if (index == -1) {
                    throw new IOException("Network interface cannot be identified");
                }
                byte[] bArrInet6AsByteArray = Net.inet6AsByteArray(inetAddress);
                byte[] bArrInet6AsByteArray2 = inetAddress2 == null ? null : Net.inet6AsByteArray(inetAddress2);
                if (Net.join6(this.fd, bArrInet6AsByteArray, index, bArrInet6AsByteArray2) == -2) {
                    throw new UnsupportedOperationException();
                }
                type4 = new MembershipKeyImpl.Type6(this, inetAddress, networkInterface, inetAddress2, bArrInet6AsByteArray, index, bArrInet6AsByteArray2);
            } else {
                Inet4Address inet4AddressAnyInet4Address = Net.anyInet4Address(networkInterface);
                if (inet4AddressAnyInet4Address == null) {
                    throw new IOException("Network interface not configured for IPv4");
                }
                int iInet4AsInt = Net.inet4AsInt(inetAddress);
                int iInet4AsInt2 = Net.inet4AsInt(inet4AddressAnyInet4Address);
                int iInet4AsInt3 = inetAddress2 == null ? 0 : Net.inet4AsInt(inetAddress2);
                if (Net.join4(this.fd, iInet4AsInt, iInet4AsInt2, iInet4AsInt3) == -2) {
                    throw new UnsupportedOperationException();
                }
                type4 = new MembershipKeyImpl.Type4(this, inetAddress, networkInterface, inetAddress2, iInet4AsInt, iInet4AsInt2, iInet4AsInt3);
            }
            this.registry.add(type4);
            return type4;
        }
    }

    @Override
    public MembershipKey join(InetAddress inetAddress, NetworkInterface networkInterface) throws IOException {
        return innerJoin(inetAddress, networkInterface, null);
    }

    @Override
    public MembershipKey join(InetAddress inetAddress, NetworkInterface networkInterface, InetAddress inetAddress2) throws IOException {
        if (inetAddress2 == null) {
            throw new NullPointerException("source address is null");
        }
        return innerJoin(inetAddress, networkInterface, inetAddress2);
    }

    void drop(MembershipKeyImpl membershipKeyImpl) {
        synchronized (this.stateLock) {
            if (membershipKeyImpl.isValid()) {
                try {
                    if (membershipKeyImpl instanceof MembershipKeyImpl.Type6) {
                        MembershipKeyImpl.Type6 type6 = (MembershipKeyImpl.Type6) membershipKeyImpl;
                        Net.drop6(this.fd, type6.groupAddress(), type6.index(), type6.source());
                    } else {
                        MembershipKeyImpl.Type4 type4 = (MembershipKeyImpl.Type4) membershipKeyImpl;
                        Net.drop4(this.fd, type4.groupAddress(), type4.interfaceAddress(), type4.source());
                    }
                    membershipKeyImpl.invalidate();
                    this.registry.remove(membershipKeyImpl);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        }
    }

    void block(MembershipKeyImpl membershipKeyImpl, InetAddress inetAddress) throws IOException {
        int iBlock4;
        synchronized (this.stateLock) {
            if (!membershipKeyImpl.isValid()) {
                throw new IllegalStateException("key is no longer valid");
            }
            if (inetAddress.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Source address is a wildcard address");
            }
            if (inetAddress.isMulticastAddress()) {
                throw new IllegalArgumentException("Source address is multicast address");
            }
            if (inetAddress.getClass() != membershipKeyImpl.group().getClass()) {
                throw new IllegalArgumentException("Source address is different type to group");
            }
            if (membershipKeyImpl instanceof MembershipKeyImpl.Type6) {
                MembershipKeyImpl.Type6 type6 = (MembershipKeyImpl.Type6) membershipKeyImpl;
                iBlock4 = Net.block6(this.fd, type6.groupAddress(), type6.index(), Net.inet6AsByteArray(inetAddress));
            } else {
                MembershipKeyImpl.Type4 type4 = (MembershipKeyImpl.Type4) membershipKeyImpl;
                iBlock4 = Net.block4(this.fd, type4.groupAddress(), type4.interfaceAddress(), Net.inet4AsInt(inetAddress));
            }
            if (iBlock4 == -2) {
                throw new UnsupportedOperationException();
            }
        }
    }

    void unblock(MembershipKeyImpl membershipKeyImpl, InetAddress inetAddress) {
        synchronized (this.stateLock) {
            if (!membershipKeyImpl.isValid()) {
                throw new IllegalStateException("key is no longer valid");
            }
            try {
                if (membershipKeyImpl instanceof MembershipKeyImpl.Type6) {
                    MembershipKeyImpl.Type6 type6 = (MembershipKeyImpl.Type6) membershipKeyImpl;
                    Net.unblock6(this.fd, type6.groupAddress(), type6.index(), Net.inet6AsByteArray(inetAddress));
                } else {
                    MembershipKeyImpl.Type4 type4 = (MembershipKeyImpl.Type4) membershipKeyImpl;
                    Net.unblock4(this.fd, type4.groupAddress(), type4.interfaceAddress(), Net.inet4AsInt(inetAddress));
                }
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            this.guard.close();
            if (this.state != 2) {
                nd.preClose(this.fd);
            }
            ResourceManager.afterUdpClose();
            if (this.registry != null) {
                this.registry.invalidateAll();
            }
            long j = this.readerThread;
            if (j != 0) {
                NativeThread.signal(j);
            }
            long j2 = this.writerThread;
            if (j2 != 0) {
                NativeThread.signal(j2);
            }
            if (!isRegistered()) {
                kill();
            }
        }
    }

    @Override
    public void kill() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 2) {
                return;
            }
            if (this.state == -1) {
                this.state = 2;
            } else {
                nd.close(this.fd);
                this.state = 2;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            if (this.fd != null) {
                close();
            }
        } finally {
            super.finalize();
        }
    }

    public boolean translateReadyOps(int i, int i2, SelectionKeyImpl selectionKeyImpl) {
        int iNioInterestOps = selectionKeyImpl.nioInterestOps();
        int iNioReadyOps = selectionKeyImpl.nioReadyOps();
        if ((Net.POLLNVAL & i) != 0) {
            return $assertionsDisabled;
        }
        if (((Net.POLLERR | Net.POLLHUP) & i) != 0) {
            selectionKeyImpl.nioReadyOps(iNioInterestOps);
            if (((~iNioReadyOps) & iNioInterestOps) != 0) {
                return true;
            }
            return $assertionsDisabled;
        }
        if ((Net.POLLIN & i) != 0 && (iNioInterestOps & 1) != 0) {
            i2 |= 1;
        }
        if ((i & Net.POLLOUT) != 0 && (iNioInterestOps & 4) != 0) {
            i2 |= 4;
        }
        selectionKeyImpl.nioReadyOps(i2);
        if (((~iNioReadyOps) & i2) != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    @Override
    public boolean translateAndUpdateReadyOps(int i, SelectionKeyImpl selectionKeyImpl) {
        return translateReadyOps(i, selectionKeyImpl.nioReadyOps(), selectionKeyImpl);
    }

    @Override
    public boolean translateAndSetReadyOps(int i, SelectionKeyImpl selectionKeyImpl) {
        return translateReadyOps(i, 0, selectionKeyImpl);
    }

    int poll(int i, long j) throws IOException {
        synchronized (this.readLock) {
            boolean z = $assertionsDisabled;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        return 0;
                    }
                    this.readerThread = NativeThread.current();
                    int iPoll = Net.poll(this.fd, i, j);
                    this.readerThread = 0L;
                    if (iPoll > 0) {
                        z = true;
                    }
                    end(z);
                    return iPoll;
                }
            } finally {
                this.readerThread = 0L;
                end($assertionsDisabled);
            }
        }
    }

    @Override
    public void translateAndSetInterestOps(int i, SelectionKeyImpl selectionKeyImpl) {
        int i2 = (i & 1) != 0 ? 0 | Net.POLLIN : 0;
        if ((i & 4) != 0) {
            i2 |= Net.POLLOUT;
        }
        if ((i & 8) != 0) {
            i2 |= Net.POLLIN;
        }
        selectionKeyImpl.selector.putEventOps(selectionKeyImpl, i2);
    }

    @Override
    public FileDescriptor getFD() {
        return this.fd;
    }

    @Override
    public int getFDVal() {
        return this.fdVal;
    }
}
