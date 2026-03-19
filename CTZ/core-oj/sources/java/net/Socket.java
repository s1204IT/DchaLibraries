package java.net;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.cta.CtaAdapter;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import libcore.io.IoBridge;
import sun.net.ApplicationProxy;
import sun.security.util.SecurityConstants;

public class Socket implements Closeable {
    private static SocketImplFactory factory = null;
    private boolean bound;
    private Object closeLock;
    private boolean closed;
    private boolean connected;
    private boolean created;
    SocketImpl impl;
    private boolean oldImpl;
    private boolean shutIn;
    private boolean shutOut;

    public Socket() {
        this.created = false;
        this.bound = false;
        this.connected = false;
        this.closed = false;
        this.closeLock = new Object();
        this.shutIn = false;
        this.shutOut = false;
        this.oldImpl = false;
        setImpl();
    }

    public Socket(Proxy proxy) {
        this.created = false;
        this.bound = false;
        this.connected = false;
        this.closed = false;
        this.closeLock = new Object();
        this.shutIn = false;
        this.shutOut = false;
        this.oldImpl = false;
        if (proxy == null) {
            throw new IllegalArgumentException("Invalid Proxy");
        }
        Proxy proxyCreate = proxy == Proxy.NO_PROXY ? Proxy.NO_PROXY : ApplicationProxy.create(proxy);
        if (proxyCreate.type() == Proxy.Type.SOCKS) {
            SecurityManager securityManager = System.getSecurityManager();
            InetSocketAddress inetSocketAddress = (InetSocketAddress) proxyCreate.address();
            if (inetSocketAddress.getAddress() != null) {
                checkAddress(inetSocketAddress.getAddress(), "Socket");
            }
            if (securityManager != null) {
                inetSocketAddress = inetSocketAddress.isUnresolved() ? new InetSocketAddress(inetSocketAddress.getHostName(), inetSocketAddress.getPort()) : inetSocketAddress;
                if (inetSocketAddress.isUnresolved()) {
                    securityManager.checkConnect(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                } else {
                    securityManager.checkConnect(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
                }
            }
            this.impl = new SocksSocketImpl(proxyCreate);
            this.impl.setSocket(this);
            return;
        }
        if (proxyCreate == Proxy.NO_PROXY) {
            if (factory == null) {
                this.impl = new PlainSocketImpl();
                this.impl.setSocket(this);
                return;
            } else {
                setImpl();
                return;
            }
        }
        throw new IllegalArgumentException("Invalid Proxy");
    }

    protected Socket(SocketImpl socketImpl) throws SocketException {
        this.created = false;
        this.bound = false;
        this.connected = false;
        this.closed = false;
        this.closeLock = new Object();
        this.shutIn = false;
        this.shutOut = false;
        this.oldImpl = false;
        this.impl = socketImpl;
        if (socketImpl != null) {
            checkOldImpl();
            this.impl.setSocket(this);
        }
    }

    public Socket(String str, int i) throws IOException {
        this(InetAddress.getAllByName(str), i, (SocketAddress) null, true);
    }

    public Socket(InetAddress inetAddress, int i) throws IOException {
        this(nonNullAddress(inetAddress), i, (SocketAddress) null, true);
    }

    public Socket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        this(InetAddress.getAllByName(str), i, (SocketAddress) new InetSocketAddress(inetAddress, i2), true);
    }

    public Socket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        this(nonNullAddress(inetAddress), i, (SocketAddress) new InetSocketAddress(inetAddress2, i2), true);
    }

    @Deprecated
    public Socket(String str, int i, boolean z) throws IOException {
        this(InetAddress.getAllByName(str), i, (SocketAddress) null, z);
    }

    @Deprecated
    public Socket(InetAddress inetAddress, int i, boolean z) throws IOException {
        this(nonNullAddress(inetAddress), i, new InetSocketAddress(0), z);
    }

    private static InetAddress[] nonNullAddress(InetAddress inetAddress) {
        if (inetAddress == null) {
            throw new NullPointerException();
        }
        return new InetAddress[]{inetAddress};
    }

    private Socket(InetAddress[] inetAddressArr, int i, SocketAddress socketAddress, boolean z) throws IOException {
        this.created = false;
        this.bound = false;
        this.connected = false;
        this.closed = false;
        this.closeLock = new Object();
        this.shutIn = false;
        this.shutOut = false;
        this.oldImpl = false;
        if (inetAddressArr == null || inetAddressArr.length == 0) {
            throw new SocketException("Impossible: empty address list");
        }
        for (int i2 = 0; i2 < inetAddressArr.length; i2++) {
            setImpl();
            try {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddressArr[i2], i);
                createImpl(z);
                if (socketAddress != null) {
                    bind(socketAddress);
                }
                connect(inetSocketAddress);
                return;
            } catch (IOException | IllegalArgumentException | SecurityException e) {
                try {
                    this.impl.close();
                    this.closed = true;
                } catch (IOException e2) {
                    e.addSuppressed(e2);
                }
                if (i2 == inetAddressArr.length - 1) {
                    throw e;
                }
                this.impl = null;
                this.created = false;
                this.bound = false;
                this.closed = false;
            }
        }
    }

    void createImpl(boolean z) throws SocketException {
        if (this.impl == null) {
            setImpl();
        }
        try {
            this.impl.create(z);
            this.created = true;
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
    }

    private void checkOldImpl() {
        if (this.impl == null) {
            return;
        }
        this.oldImpl = ((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                Class<?> superclass = Socket.this.impl.getClass();
                do {
                    try {
                        superclass.getDeclaredMethod(SecurityConstants.SOCKET_CONNECT_ACTION, SocketAddress.class, Integer.TYPE);
                        return Boolean.FALSE;
                    } catch (NoSuchMethodException e) {
                        superclass = superclass.getSuperclass();
                    }
                } while (!superclass.equals(SocketImpl.class));
                return Boolean.TRUE;
            }
        })).booleanValue();
    }

    void setImpl() {
        if (factory != null) {
            this.impl = factory.createSocketImpl();
            checkOldImpl();
        } else {
            this.impl = new SocksSocketImpl();
        }
        if (this.impl != null) {
            this.impl.setSocket(this);
        }
    }

    SocketImpl getImpl() throws SocketException {
        if (!this.created) {
            createImpl(true);
        }
        return this.impl;
    }

    public void connect(SocketAddress socketAddress) throws IOException {
        connect(socketAddress, 0);
    }

    public void connect(SocketAddress socketAddress, int i) throws IOException {
        if (socketAddress == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }
        if (i < 0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!this.oldImpl && isConnected()) {
            throw new SocketException("already connected");
        }
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        InetAddress address = inetSocketAddress.getAddress();
        int port = inetSocketAddress.getPort();
        checkAddress(address, SecurityConstants.SOCKET_CONNECT_ACTION);
        synchronized (Socket.class) {
            if (!CtaAdapter.isSendingPermitted(port)) {
                System.out.println("Fail to send due to mom user permission");
                throw new UnknownHostException("User denied by MoM");
            }
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            if (inetSocketAddress.isUnresolved()) {
                securityManager.checkConnect(inetSocketAddress.getHostName(), port);
            } else {
                securityManager.checkConnect(address.getHostAddress(), port);
            }
        }
        if (!this.created) {
            createImpl(true);
        }
        if (!this.oldImpl) {
            this.impl.connect(inetSocketAddress, i);
        } else if (i == 0) {
            if (inetSocketAddress.isUnresolved()) {
                this.impl.connect(address.getHostName(), port);
            } else {
                this.impl.connect(address, port);
            }
        } else {
            throw new UnsupportedOperationException("SocketImpl.connect(addr, timeout)");
        }
        this.connected = true;
        if (DebugUtils.isDebugLogOn()) {
            InetSocketAddress localInetSocketAddress = IoBridge.getLocalInetSocketAddress(this.impl.fd);
            System.out.println("[socket][" + ((Object) localInetSocketAddress.getAddress()) + ":" + getLocalPort() + "] connected");
        }
        this.bound = true;
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!this.oldImpl && isBound()) {
            throw new SocketException("Already bound");
        }
        if (socketAddress != null && !(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        if (inetSocketAddress != null && inetSocketAddress.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        if (inetSocketAddress == null) {
            inetSocketAddress = new InetSocketAddress(0);
        }
        InetAddress address = inetSocketAddress.getAddress();
        int port = inetSocketAddress.getPort();
        checkAddress(address, "bind");
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkListen(port);
        }
        getImpl().bind(address, port);
        this.bound = true;
    }

    private void checkAddress(InetAddress inetAddress, String str) {
        if (inetAddress != null && !(inetAddress instanceof Inet4Address) && !(inetAddress instanceof Inet6Address)) {
            throw new IllegalArgumentException(str + ": invalid address type");
        }
    }

    final void postAccept() {
        this.connected = true;
        this.created = true;
        this.bound = true;
    }

    void setCreated() {
        this.created = true;
    }

    void setBound() {
        this.bound = true;
    }

    void setConnected() {
        this.connected = true;
    }

    public InetAddress getInetAddress() {
        if (!isConnected()) {
            return null;
        }
        try {
            return getImpl().getInetAddress();
        } catch (SocketException e) {
            return null;
        }
    }

    public InetAddress getLocalAddress() {
        if (!isBound()) {
            return InetAddress.anyLocalAddress();
        }
        try {
            InetAddress inetAddress = (InetAddress) getImpl().getOption(15);
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkConnect(inetAddress.getHostAddress(), -1);
            }
            if (inetAddress.isAnyLocalAddress()) {
                return InetAddress.anyLocalAddress();
            }
            return inetAddress;
        } catch (SecurityException e) {
            return InetAddress.getLoopbackAddress();
        } catch (Exception e2) {
            return InetAddress.anyLocalAddress();
        }
    }

    public int getPort() {
        if (!isConnected()) {
            return 0;
        }
        try {
            return getImpl().getPort();
        } catch (SocketException e) {
            return -1;
        }
    }

    public int getLocalPort() {
        if (!isBound()) {
            return -1;
        }
        try {
            return getImpl().getLocalPort();
        } catch (SocketException e) {
            return -1;
        }
    }

    public SocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }
        return new InetSocketAddress(getInetAddress(), getPort());
    }

    public SocketAddress getLocalSocketAddress() {
        if (!isBound()) {
            return null;
        }
        return new InetSocketAddress(getLocalAddress(), getLocalPort());
    }

    public SocketChannel getChannel() {
        return null;
    }

    public InputStream getInputStream() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isInputShutdown()) {
            throw new SocketException("Socket input is shutdown");
        }
        try {
            return (InputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                @Override
                public InputStream run() throws IOException {
                    return Socket.this.impl.getInputStream();
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((IOException) e.getException());
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isOutputShutdown()) {
            throw new SocketException("Socket output is shutdown");
        }
        try {
            return (OutputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                @Override
                public OutputStream run() throws IOException {
                    return Socket.this.impl.getOutputStream();
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((IOException) e.getException());
        }
    }

    public void setTcpNoDelay(boolean z) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(1, Boolean.valueOf(z));
    }

    public boolean getTcpNoDelay() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(1)).booleanValue();
    }

    public void setSoLinger(boolean z, int i) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!z) {
            getImpl().setOption(128, new Boolean(z));
        } else {
            if (i < 0) {
                throw new IllegalArgumentException("invalid value for SO_LINGER");
            }
            int i2 = 65535;
            if (i <= 65535) {
                i2 = i;
            }
            getImpl().setOption(128, new Integer(i2));
        }
    }

    public int getSoLinger() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        Object option = getImpl().getOption(128);
        if (option instanceof Integer) {
            return ((Integer) option).intValue();
        }
        return -1;
    }

    public void sendUrgentData(int i) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!getImpl().supportsUrgentData()) {
            throw new SocketException("Urgent data not supported");
        }
        getImpl().sendUrgentData(i);
    }

    public void setOOBInline(boolean z) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_OOBINLINE, Boolean.valueOf(z));
    }

    public boolean getOOBInline() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(SocketOptions.SO_OOBINLINE)).booleanValue();
    }

    public synchronized void setSoTimeout(int i) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (i < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        getImpl().setOption(SocketOptions.SO_TIMEOUT, new Integer(i));
    }

    public synchronized int getSoTimeout() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        Object option = getImpl().getOption(SocketOptions.SO_TIMEOUT);
        if (option instanceof Integer) {
            return ((Integer) option).intValue();
        }
        return 0;
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

    public void setKeepAlive(boolean z) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(8, Boolean.valueOf(z));
    }

    public boolean getKeepAlive() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(8)).booleanValue();
    }

    public void setTrafficClass(int i) throws SocketException {
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

    public int getTrafficClass() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Integer) getImpl().getOption(3)).intValue();
    }

    public void setReuseAddress(boolean z) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(4, Boolean.valueOf(z));
    }

    public boolean getReuseAddress() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(4)).booleanValue();
    }

    @Override
    public synchronized void close() throws IOException {
        synchronized (this.closeLock) {
            if (isClosed()) {
                return;
            }
            if (this.created) {
                if (DebugUtils.isDebugLogOn()) {
                    InetSocketAddress localInetSocketAddress = IoBridge.getLocalInetSocketAddress(this.impl.fd);
                    System.out.println("close [socket][" + ((Object) localInetSocketAddress.getAddress()) + ":" + getLocalPort() + "]");
                }
                this.impl.close();
            }
            this.closed = true;
        }
    }

    public void shutdownInput() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isInputShutdown()) {
            throw new SocketException("Socket input is already shutdown");
        }
        getImpl().shutdownInput();
        this.shutIn = true;
    }

    public void shutdownOutput() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isOutputShutdown()) {
            throw new SocketException("Socket output is already shutdown");
        }
        getImpl().shutdownOutput();
        this.shutOut = true;
    }

    public String toString() {
        try {
            if (isConnected()) {
                return "Socket[address=" + ((Object) getImpl().getInetAddress()) + ",port=" + getImpl().getPort() + ",localPort=" + getImpl().getLocalPort() + "]";
            }
            return "Socket[unconnected]";
        } catch (SocketException e) {
            return "Socket[unconnected]";
        }
    }

    public boolean isConnected() {
        return this.connected || this.oldImpl;
    }

    public boolean isBound() {
        return this.bound || this.oldImpl;
    }

    public boolean isClosed() {
        boolean z;
        synchronized (this.closeLock) {
            z = this.closed;
        }
        return z;
    }

    public boolean isInputShutdown() {
        return this.shutIn;
    }

    public boolean isOutputShutdown() {
        return this.shutOut;
    }

    public static synchronized void setSocketImplFactory(SocketImplFactory socketImplFactory) throws IOException {
        if (factory != null) {
            throw new SocketException("factory already defined");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSetFactory();
        }
        factory = socketImplFactory;
    }

    public void setPerformancePreferences(int i, int i2, int i3) {
    }

    public FileDescriptor getFileDescriptor$() {
        return this.impl.getFileDescriptor();
    }
}
