package java.net;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import sun.security.util.SecurityConstants;

public class DatagramSocket implements Closeable {
    static final int ST_CONNECTED = 1;
    static final int ST_CONNECTED_NO_IMPL = 2;
    static final int ST_NOT_CONNECTED = 0;
    static DatagramSocketImplFactory factory;
    static Class<?> implClass = null;
    private boolean bound;
    private int bytesLeftToFilter;
    private Object closeLock;
    private boolean closed;
    int connectState;
    InetAddress connectedAddress;
    int connectedPort;
    private boolean created;
    private boolean explicitFilter;
    DatagramSocketImpl impl;
    boolean oldImpl;
    private SocketException pendingConnectException;

    private synchronized void connectInternal(InetAddress inetAddress, int i) throws SocketException {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("connect: " + i);
        }
        if (inetAddress == null) {
            throw new IllegalArgumentException("connect: null address");
        }
        checkAddress(inetAddress, SecurityConstants.SOCKET_CONNECT_ACTION);
        if (isClosed()) {
            return;
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            if (inetAddress.isMulticastAddress()) {
                securityManager.checkMulticast(inetAddress);
            } else {
                securityManager.checkConnect(inetAddress.getHostAddress(), i);
                securityManager.checkAccept(inetAddress.getHostAddress(), i);
            }
        }
        if (!isBound()) {
            bind(new InetSocketAddress(0));
        }
        try {
            if (this.oldImpl || ((this.impl instanceof AbstractPlainDatagramSocketImpl) && ((AbstractPlainDatagramSocketImpl) this.impl).nativeConnectDisabled())) {
                this.connectState = 2;
            } else {
                try {
                    getImpl().connect(inetAddress, i);
                    boolean z = true;
                    this.connectState = 1;
                    int iDataAvailable = getImpl().dataAvailable();
                    if (iDataAvailable == -1) {
                        throw new SocketException();
                    }
                    if (iDataAvailable <= 0) {
                        z = false;
                    }
                    this.explicitFilter = z;
                    if (this.explicitFilter) {
                        this.bytesLeftToFilter = getReceiveBufferSize();
                    }
                } catch (SocketException e) {
                    this.connectState = 2;
                    throw e;
                }
            }
        } finally {
            this.connectedAddress = inetAddress;
            this.connectedPort = i;
        }
    }

    public DatagramSocket() throws SocketException {
        this(new InetSocketAddress(0));
    }

    protected DatagramSocket(DatagramSocketImpl datagramSocketImpl) {
        this.created = false;
        this.bound = false;
        this.closed = false;
        this.closeLock = new Object();
        this.oldImpl = false;
        this.explicitFilter = false;
        this.connectState = 0;
        this.connectedAddress = null;
        this.connectedPort = -1;
        if (datagramSocketImpl == null) {
            throw new NullPointerException();
        }
        this.impl = datagramSocketImpl;
        checkOldImpl();
    }

    public DatagramSocket(SocketAddress socketAddress) throws SocketException {
        this.created = false;
        this.bound = false;
        this.closed = false;
        this.closeLock = new Object();
        this.oldImpl = false;
        this.explicitFilter = false;
        this.connectState = 0;
        this.connectedAddress = null;
        this.connectedPort = -1;
        createImpl();
        if (socketAddress != null) {
            try {
                bind(socketAddress);
            } finally {
                if (!isBound()) {
                    close();
                }
            }
        }
    }

    public DatagramSocket(int i) throws SocketException {
        this(i, null);
    }

    public DatagramSocket(int i, InetAddress inetAddress) throws SocketException {
        this(new InetSocketAddress(inetAddress, i));
    }

    private void checkOldImpl() {
        if (this.impl == null) {
            return;
        }
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws NoSuchMethodException {
                    DatagramSocket.this.impl.getClass().getDeclaredMethod("peekData", DatagramPacket.class);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            this.oldImpl = true;
        }
    }

    void createImpl() throws SocketException {
        boolean z;
        if (this.impl == null) {
            if (factory != null) {
                this.impl = factory.createDatagramSocketImpl();
                checkOldImpl();
            } else {
                if (!(this instanceof MulticastSocket)) {
                    z = false;
                } else {
                    z = true;
                }
                this.impl = DefaultDatagramSocketImplFactory.createDatagramSocketImpl(z);
                checkOldImpl();
            }
        }
        this.impl.create();
        this.impl.setDatagramSocket(this);
        this.created = true;
    }

    DatagramSocketImpl getImpl() throws SocketException {
        if (!this.created) {
            createImpl();
        }
        return this.impl;
    }

    public synchronized void bind(SocketAddress socketAddress) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (isBound()) {
            throw new SocketException("already bound");
        }
        if (socketAddress == null) {
            socketAddress = new InetSocketAddress(0);
        }
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type!");
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        if (inetSocketAddress.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        InetAddress address = inetSocketAddress.getAddress();
        int port = inetSocketAddress.getPort();
        checkAddress(address, "bind");
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkListen(port);
        }
        try {
            getImpl().bind(port, address);
            this.bound = true;
        } catch (SocketException e) {
            getImpl().close();
            throw e;
        }
    }

    void checkAddress(InetAddress inetAddress, String str) {
        if (inetAddress != null && !(inetAddress instanceof Inet4Address) && !(inetAddress instanceof Inet6Address)) {
            throw new IllegalArgumentException(str + ": invalid address type");
        }
    }

    public void connect(InetAddress inetAddress, int i) {
        try {
            connectInternal(inetAddress, i);
        } catch (SocketException e) {
            this.pendingConnectException = e;
        }
    }

    public void connect(SocketAddress socketAddress) throws SocketException {
        if (socketAddress == null) {
            throw new IllegalArgumentException("Address can't be null");
        }
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        if (inetSocketAddress.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        connectInternal(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
    }

    public void disconnect() {
        synchronized (this) {
            if (isClosed()) {
                return;
            }
            if (this.connectState == 1) {
                this.impl.disconnect();
            }
            this.connectedAddress = null;
            this.connectedPort = -1;
            this.connectState = 0;
            this.explicitFilter = false;
        }
    }

    public boolean isBound() {
        return this.bound;
    }

    public boolean isConnected() {
        return this.connectState != 0;
    }

    public InetAddress getInetAddress() {
        return this.connectedAddress;
    }

    public int getPort() {
        return this.connectedPort;
    }

    public SocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }
        return new InetSocketAddress(getInetAddress(), getPort());
    }

    public SocketAddress getLocalSocketAddress() {
        if (!isClosed() && isBound()) {
            return new InetSocketAddress(getLocalAddress(), getLocalPort());
        }
        return null;
    }

    public void send(DatagramPacket datagramPacket) throws IOException {
        synchronized (datagramPacket) {
            if (this.pendingConnectException != null) {
                throw new SocketException("Pending connect failure", this.pendingConnectException);
            }
            if (isClosed()) {
                throw new SocketException("Socket is closed");
            }
            checkAddress(datagramPacket.getAddress(), "send");
            if (this.connectState == 0) {
                SecurityManager securityManager = System.getSecurityManager();
                if (securityManager != null) {
                    if (datagramPacket.getAddress().isMulticastAddress()) {
                        securityManager.checkMulticast(datagramPacket.getAddress());
                    } else {
                        securityManager.checkConnect(datagramPacket.getAddress().getHostAddress(), datagramPacket.getPort());
                    }
                }
            } else {
                InetAddress address = datagramPacket.getAddress();
                if (address == null) {
                    datagramPacket.setAddress(this.connectedAddress);
                    datagramPacket.setPort(this.connectedPort);
                } else if (!address.equals(this.connectedAddress) || datagramPacket.getPort() != this.connectedPort) {
                    throw new IllegalArgumentException("connected address and packet address differ");
                }
            }
            if (!isBound()) {
                bind(new InetSocketAddress(0));
            }
            getImpl().send(datagramPacket);
        }
    }

    public synchronized void receive(DatagramPacket datagramPacket) throws IOException {
        InetAddress inetAddress;
        int iPeek;
        SecurityManager securityManager;
        int iPeek2;
        String hostAddress;
        synchronized (datagramPacket) {
            boolean z = false;
            if (!isBound()) {
                bind(new InetSocketAddress(0));
            }
            if (this.pendingConnectException != null) {
                throw new SocketException("Pending connect failure", this.pendingConnectException);
            }
            if (this.connectState == 0 && (securityManager = System.getSecurityManager()) != null) {
                while (true) {
                    if (!this.oldImpl) {
                        DatagramPacket datagramPacket2 = new DatagramPacket(new byte[1], 1);
                        iPeek2 = getImpl().peekData(datagramPacket2);
                        hostAddress = datagramPacket2.getAddress().getHostAddress();
                    } else {
                        InetAddress inetAddress2 = new InetAddress();
                        iPeek2 = getImpl().peek(inetAddress2);
                        hostAddress = inetAddress2.getHostAddress();
                    }
                    try {
                        securityManager.checkAccept(hostAddress, iPeek2);
                        break;
                    } catch (SecurityException e) {
                        getImpl().receive(new DatagramPacket(new byte[1], 1));
                    }
                }
            }
            DatagramPacket datagramPacket3 = null;
            if (this.connectState == 2 || this.explicitFilter) {
                while (!z) {
                    if (!this.oldImpl) {
                        DatagramPacket datagramPacket4 = new DatagramPacket(new byte[1], 1);
                        iPeek = getImpl().peekData(datagramPacket4);
                        inetAddress = datagramPacket4.getAddress();
                    } else {
                        inetAddress = new InetAddress();
                        iPeek = getImpl().peek(inetAddress);
                    }
                    if (!this.connectedAddress.equals(inetAddress) || this.connectedPort != iPeek) {
                        datagramPacket3 = new DatagramPacket(new byte[1024], 1024);
                        getImpl().receive(datagramPacket3);
                        if (this.explicitFilter && checkFiltering(datagramPacket3)) {
                            z = true;
                        }
                    } else {
                        z = true;
                    }
                }
            }
            getImpl().receive(datagramPacket);
            if (this.explicitFilter && datagramPacket3 == null) {
                checkFiltering(datagramPacket);
            }
        }
    }

    private boolean checkFiltering(DatagramPacket datagramPacket) throws SocketException {
        this.bytesLeftToFilter -= datagramPacket.getLength();
        if (this.bytesLeftToFilter > 0 && getImpl().dataAvailable() > 0) {
            return false;
        }
        this.explicitFilter = false;
        return true;
    }

    public InetAddress getLocalAddress() {
        if (isClosed()) {
            return null;
        }
        try {
            InetAddress inetAddressAnyLocalAddress = (InetAddress) getImpl().getOption(15);
            if (inetAddressAnyLocalAddress.isAnyLocalAddress()) {
                inetAddressAnyLocalAddress = InetAddress.anyLocalAddress();
            }
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkConnect(inetAddressAnyLocalAddress.getHostAddress(), -1);
                return inetAddressAnyLocalAddress;
            }
            return inetAddressAnyLocalAddress;
        } catch (Exception e) {
            return InetAddress.anyLocalAddress();
        }
    }

    public int getLocalPort() {
        if (isClosed()) {
            return -1;
        }
        try {
            return getImpl().getLocalPort();
        } catch (Exception e) {
            return 0;
        }
    }

    public synchronized void setSoTimeout(int i) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_TIMEOUT, new Integer(i));
    }

    public synchronized int getSoTimeout() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (getImpl() == null) {
            return 0;
        }
        Object option = getImpl().getOption(SocketOptions.SO_TIMEOUT);
        if (!(option instanceof Integer)) {
            return 0;
        }
        return ((Integer) option).intValue();
    }

    public synchronized void setSendBufferSize(int i) throws SocketException {
        if (i <= 0) {
            throw new IllegalArgumentException("negative send size");
        }
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_SNDBUF, new Integer(i));
    }

    public synchronized int getSendBufferSize() throws SocketException {
        int iIntValue;
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        iIntValue = 0;
        Object option = getImpl().getOption(SocketOptions.SO_SNDBUF);
        if (option instanceof Integer) {
            iIntValue = ((Integer) option).intValue();
        }
        return iIntValue;
    }

    public synchronized void setReceiveBufferSize(int i) throws SocketException {
        if (i <= 0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_RCVBUF, new Integer(i));
    }

    public synchronized int getReceiveBufferSize() throws SocketException {
        int iIntValue;
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        iIntValue = 0;
        Object option = getImpl().getOption(SocketOptions.SO_RCVBUF);
        if (option instanceof Integer) {
            iIntValue = ((Integer) option).intValue();
        }
        return iIntValue;
    }

    public synchronized void setReuseAddress(boolean z) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (this.oldImpl) {
            getImpl().setOption(4, new Integer(z ? -1 : 0));
        } else {
            getImpl().setOption(4, Boolean.valueOf(z));
        }
    }

    public synchronized boolean getReuseAddress() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(4)).booleanValue();
    }

    public synchronized void setBroadcast(boolean z) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(32, Boolean.valueOf(z));
    }

    public synchronized boolean getBroadcast() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(32)).booleanValue();
    }

    public synchronized void setTrafficClass(int i) throws SocketException {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("tc is not in range 0 -- 255");
        }
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        try {
            getImpl().setOption(3, Integer.valueOf(i));
        } catch (SocketException e) {
            if (!isConnected()) {
                throw e;
            }
        }
    }

    public synchronized int getTrafficClass() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Integer) getImpl().getOption(3)).intValue();
    }

    @Override
    public void close() {
        synchronized (this.closeLock) {
            if (isClosed()) {
                return;
            }
            this.impl.close();
            this.closed = true;
        }
    }

    public boolean isClosed() {
        boolean z;
        synchronized (this.closeLock) {
            z = this.closed;
        }
        return z;
    }

    public DatagramChannel getChannel() {
        return null;
    }

    public static synchronized void setDatagramSocketImplFactory(DatagramSocketImplFactory datagramSocketImplFactory) throws IOException {
        if (factory != null) {
            throw new SocketException("factory already defined");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSetFactory();
        }
        factory = datagramSocketImplFactory;
    }

    public FileDescriptor getFileDescriptor$() {
        return this.impl.fd;
    }
}
