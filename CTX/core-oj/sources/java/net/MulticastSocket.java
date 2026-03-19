package java.net;

import java.io.IOException;
import java.util.Enumeration;

public class MulticastSocket extends DatagramSocket {
    private InetAddress infAddress;
    private Object infLock;
    private boolean interfaceSet;
    private Object ttlLock;

    public MulticastSocket() throws IOException {
        this(new InetSocketAddress(0));
    }

    public MulticastSocket(int i) throws IOException {
        this(new InetSocketAddress(i));
    }

    public MulticastSocket(SocketAddress socketAddress) throws IOException {
        super((SocketAddress) null);
        this.ttlLock = new Object();
        this.infLock = new Object();
        this.infAddress = null;
        setReuseAddress(true);
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

    @Deprecated
    public void setTTL(byte b) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setTTL(b);
    }

    public void setTimeToLive(int i) throws IOException {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("ttl out of range");
        }
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setTimeToLive(i);
    }

    @Deprecated
    public byte getTTL() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return getImpl().getTTL();
    }

    public int getTimeToLive() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return getImpl().getTimeToLive();
    }

    public void joinGroup(InetAddress inetAddress) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        checkAddress(inetAddress, "joinGroup");
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkMulticast(inetAddress);
        }
        if (!inetAddress.isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        NetworkInterface networkInterface = NetworkInterface.getDefault();
        if (!this.interfaceSet && networkInterface != null) {
            setNetworkInterface(networkInterface);
        }
        getImpl().join(inetAddress);
    }

    public void leaveGroup(InetAddress inetAddress) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        checkAddress(inetAddress, "leaveGroup");
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkMulticast(inetAddress);
        }
        if (!inetAddress.isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        getImpl().leave(inetAddress);
    }

    public void joinGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (socketAddress == null || !(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        if (this.oldImpl) {
            throw new UnsupportedOperationException();
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        checkAddress(inetSocketAddress.getAddress(), "joinGroup");
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkMulticast(inetSocketAddress.getAddress());
        }
        if (!inetSocketAddress.getAddress().isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        getImpl().joinGroup(socketAddress, networkInterface);
    }

    public void leaveGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (socketAddress == null || !(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        if (this.oldImpl) {
            throw new UnsupportedOperationException();
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        checkAddress(inetSocketAddress.getAddress(), "leaveGroup");
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkMulticast(inetSocketAddress.getAddress());
        }
        if (!inetSocketAddress.getAddress().isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        getImpl().leaveGroup(socketAddress, networkInterface);
    }

    public void setInterface(InetAddress inetAddress) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        checkAddress(inetAddress, "setInterface");
        synchronized (this.infLock) {
            getImpl().setOption(16, inetAddress);
            this.infAddress = inetAddress;
            this.interfaceSet = true;
        }
    }

    public InetAddress getInterface() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        synchronized (this.infLock) {
            InetAddress inetAddress = (InetAddress) getImpl().getOption(16);
            if (this.infAddress == null) {
                return inetAddress;
            }
            if (inetAddress.equals(this.infAddress)) {
                return inetAddress;
            }
            try {
                Enumeration<InetAddress> inetAddresses = NetworkInterface.getByInetAddress(inetAddress).getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    if (inetAddresses.nextElement().equals(this.infAddress)) {
                        return this.infAddress;
                    }
                }
                this.infAddress = null;
                return inetAddress;
            } catch (Exception e) {
                return inetAddress;
            }
        }
    }

    public void setNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        synchronized (this.infLock) {
            getImpl().setOption(31, networkInterface);
            this.infAddress = null;
            this.interfaceSet = true;
        }
    }

    public NetworkInterface getNetworkInterface() throws SocketException {
        Integer num = (Integer) getImpl().getOption(31);
        if (num.intValue() == 0) {
            InetAddress[] inetAddressArr = {InetAddress.anyLocalAddress()};
            return new NetworkInterface(inetAddressArr[0].getHostName(), 0, inetAddressArr);
        }
        return NetworkInterface.getByIndex(num.intValue());
    }

    public void setLoopbackMode(boolean z) throws SocketException {
        getImpl().setOption(18, Boolean.valueOf(z));
    }

    public boolean getLoopbackMode() throws SocketException {
        return ((Boolean) getImpl().getOption(18)).booleanValue();
    }

    @Deprecated
    public void send(DatagramPacket datagramPacket, byte b) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        checkAddress(datagramPacket.getAddress(), "send");
        synchronized (this.ttlLock) {
            synchronized (datagramPacket) {
                if (this.connectState == 0) {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        if (datagramPacket.getAddress().isMulticastAddress()) {
                            securityManager.checkMulticast(datagramPacket.getAddress(), b);
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
                        throw new SecurityException("connected address and packet address differ");
                    }
                }
                byte ttl = getTTL();
                if (b != ttl) {
                    try {
                        getImpl().setTTL(b);
                    } catch (Throwable th) {
                        if (b != ttl) {
                            getImpl().setTTL(ttl);
                        }
                        throw th;
                    }
                }
                getImpl().send(datagramPacket);
                if (b != ttl) {
                    getImpl().setTTL(ttl);
                }
            }
        }
    }
}
