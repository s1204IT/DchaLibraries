package java.net;

import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.security.AccessController;
import java.util.Enumeration;
import libcore.io.IoBridge;
import sun.net.ResourceManager;
import sun.security.action.GetPropertyAction;

abstract class AbstractPlainDatagramSocketImpl extends DatagramSocketImpl {
    private static final String os = (String) AccessController.doPrivileged(new GetPropertyAction("os.name"));
    private static final boolean connectDisabled = os.contains("OS X");
    int timeout = 0;
    boolean connected = false;
    private int trafficClass = 0;
    protected InetAddress connectedAddress = null;
    private int connectedPort = -1;
    private final CloseGuard guard = CloseGuard.get();

    protected abstract void bind0(int i, InetAddress inetAddress) throws SocketException;

    protected abstract void connect0(InetAddress inetAddress, int i) throws SocketException;

    protected abstract void datagramSocketClose();

    protected abstract void datagramSocketCreate() throws SocketException;

    protected abstract void disconnect0(int i);

    @Override
    @Deprecated
    protected abstract byte getTTL() throws IOException;

    @Override
    protected abstract int getTimeToLive() throws IOException;

    protected abstract void join(InetAddress inetAddress, NetworkInterface networkInterface) throws IOException;

    protected abstract void leave(InetAddress inetAddress, NetworkInterface networkInterface) throws IOException;

    @Override
    protected abstract int peek(InetAddress inetAddress) throws IOException;

    @Override
    protected abstract int peekData(DatagramPacket datagramPacket) throws IOException;

    protected abstract void receive0(DatagramPacket datagramPacket) throws IOException;

    @Override
    protected abstract void send(DatagramPacket datagramPacket) throws IOException;

    @Override
    @Deprecated
    protected abstract void setTTL(byte b) throws IOException;

    @Override
    protected abstract void setTimeToLive(int i) throws IOException;

    protected abstract Object socketGetOption(int i) throws SocketException;

    protected abstract void socketSetOption(int i, Object obj) throws SocketException;

    AbstractPlainDatagramSocketImpl() {
    }

    @Override
    protected synchronized void create() throws SocketException {
        ResourceManager.beforeUdpCreate();
        this.fd = new FileDescriptor();
        try {
            datagramSocketCreate();
            if (this.fd != null && this.fd.valid()) {
                this.guard.open("close");
            }
        } catch (SocketException e) {
            ResourceManager.afterUdpClose();
            this.fd = null;
            throw e;
        }
    }

    @Override
    protected synchronized void bind(int i, InetAddress inetAddress) throws SocketException {
        bind0(i, inetAddress);
    }

    @Override
    protected void connect(InetAddress inetAddress, int i) throws SocketException {
        BlockGuard.getThreadPolicy().onNetwork();
        connect0(inetAddress, i);
        this.connectedAddress = inetAddress;
        this.connectedPort = i;
        this.connected = true;
    }

    @Override
    protected void disconnect() {
        disconnect0(this.connectedAddress.holder().getFamily());
        this.connected = false;
        this.connectedAddress = null;
        this.connectedPort = -1;
    }

    @Override
    protected synchronized void receive(DatagramPacket datagramPacket) throws IOException {
        receive0(datagramPacket);
    }

    @Override
    protected void join(InetAddress inetAddress) throws IOException {
        join(inetAddress, null);
    }

    @Override
    protected void leave(InetAddress inetAddress) throws IOException {
        leave(inetAddress, null);
    }

    @Override
    protected void joinGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException {
        if (socketAddress == null || !(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        join(((InetSocketAddress) socketAddress).getAddress(), networkInterface);
    }

    @Override
    protected void leaveGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException {
        if (socketAddress == null || !(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        leave(((InetSocketAddress) socketAddress).getAddress(), networkInterface);
    }

    @Override
    protected void close() {
        this.guard.close();
        if (this.fd != null) {
            datagramSocketClose();
            ResourceManager.afterUdpClose();
            this.fd = null;
        }
    }

    protected boolean isClosed() {
        return this.fd == null;
    }

    protected void finalize() {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        close();
    }

    @Override
    public void setOption(int i, Object obj) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket Closed");
        }
        switch (i) {
            case 3:
                if (obj == null || !(obj instanceof Integer)) {
                    throw new SocketException("bad argument for IP_TOS");
                }
                this.trafficClass = ((Integer) obj).intValue();
                break;
                break;
            case 4:
                if (obj == null || !(obj instanceof Boolean)) {
                    throw new SocketException("bad argument for SO_REUSEADDR");
                }
                break;
            case 15:
                throw new SocketException("Cannot re-bind Socket");
            case 16:
                if (obj == null || !(obj instanceof InetAddress)) {
                    throw new SocketException("bad argument for IP_MULTICAST_IF");
                }
                break;
            case 18:
                if (obj == null || !(obj instanceof Boolean)) {
                    throw new SocketException("bad argument for IP_MULTICAST_LOOP");
                }
                break;
            case SocketOptions.IP_MULTICAST_IF2:
                if (obj == null || (!(obj instanceof Integer) && !(obj instanceof NetworkInterface))) {
                    throw new SocketException("bad argument for IP_MULTICAST_IF2");
                }
                if (obj instanceof NetworkInterface) {
                    obj = new Integer(((NetworkInterface) obj).getIndex());
                }
                break;
                break;
            case 32:
                if (obj == null || !(obj instanceof Boolean)) {
                    throw new SocketException("bad argument for SO_BROADCAST");
                }
                break;
            case SocketOptions.SO_SNDBUF:
            case SocketOptions.SO_RCVBUF:
                if (obj == null || !(obj instanceof Integer) || ((Integer) obj).intValue() < 0) {
                    throw new SocketException("bad argument for SO_SNDBUF or SO_RCVBUF");
                }
                break;
            case SocketOptions.SO_TIMEOUT:
                if (obj == null || !(obj instanceof Integer)) {
                    throw new SocketException("bad argument for SO_TIMEOUT");
                }
                int iIntValue = ((Integer) obj).intValue();
                if (iIntValue < 0) {
                    throw new IllegalArgumentException("timeout < 0");
                }
                this.timeout = iIntValue;
                return;
            default:
                throw new SocketException("invalid option: " + i);
        }
        socketSetOption(i, obj);
    }

    @Override
    public Object getOption(int i) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket Closed");
        }
        switch (i) {
            case 3:
                Object objSocketGetOption = socketGetOption(i);
                if (((Integer) objSocketGetOption).intValue() == -1) {
                    return new Integer(this.trafficClass);
                }
                return objSocketGetOption;
            case 4:
            case 15:
            case 16:
            case 18:
            case SocketOptions.IP_MULTICAST_IF2:
            case 32:
            case SocketOptions.SO_SNDBUF:
            case SocketOptions.SO_RCVBUF:
                Object objSocketGetOption2 = socketGetOption(i);
                if (i == 16) {
                    return getNIFirstAddress(((Integer) objSocketGetOption2).intValue());
                }
                return objSocketGetOption2;
            case SocketOptions.SO_TIMEOUT:
                return new Integer(this.timeout);
            default:
                throw new SocketException("invalid option: " + i);
        }
    }

    static InetAddress getNIFirstAddress(int i) throws SocketException {
        if (i > 0) {
            Enumeration<InetAddress> inetAddresses = NetworkInterface.getByIndex(i).getInetAddresses();
            if (inetAddresses.hasMoreElements()) {
                return inetAddresses.nextElement();
            }
        }
        return InetAddress.anyLocalAddress();
    }

    protected boolean nativeConnectDisabled() {
        return connectDisabled;
    }

    @Override
    int dataAvailable() {
        try {
            return IoBridge.available(this.fd);
        } catch (IOException e) {
            return -1;
        }
    }
}
