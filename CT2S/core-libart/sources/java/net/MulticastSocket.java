package java.net;

import java.io.IOException;
import java.util.Enumeration;

public class MulticastSocket extends DatagramSocket {
    private InetAddress setAddress;

    public MulticastSocket() throws IOException {
        setReuseAddress(true);
    }

    public MulticastSocket(int port) throws IOException {
        super(port);
        setReuseAddress(true);
    }

    public MulticastSocket(SocketAddress localAddress) throws IOException {
        super(localAddress);
        setReuseAddress(true);
    }

    public InetAddress getInterface() throws SocketException {
        NetworkInterface theInterface;
        Enumeration<InetAddress> addresses;
        checkOpen();
        if (this.setAddress != null) {
            return this.setAddress;
        }
        InetAddress ipvXaddress = (InetAddress) this.impl.getOption(16);
        if (ipvXaddress.isAnyLocalAddress() && (theInterface = getNetworkInterface()) != null && (addresses = theInterface.getInetAddresses()) != null) {
            while (addresses.hasMoreElements()) {
                InetAddress nextAddress = addresses.nextElement();
                if (nextAddress instanceof Inet6Address) {
                    return nextAddress;
                }
            }
        }
        return ipvXaddress;
    }

    public NetworkInterface getNetworkInterface() throws SocketException {
        checkOpen();
        int index = ((Integer) this.impl.getOption(31)).intValue();
        return index != 0 ? NetworkInterface.getByIndex(index) : NetworkInterface.forUnboundMulticastSocket();
    }

    public int getTimeToLive() throws IOException {
        checkOpen();
        return this.impl.getTimeToLive();
    }

    @Deprecated
    public byte getTTL() throws IOException {
        checkOpen();
        return this.impl.getTTL();
    }

    public void joinGroup(InetAddress groupAddr) throws IOException {
        checkJoinOrLeave(groupAddr);
        this.impl.join(groupAddr);
    }

    public void joinGroup(SocketAddress groupAddress, NetworkInterface netInterface) throws IOException {
        checkJoinOrLeave(groupAddress, netInterface);
        this.impl.joinGroup(groupAddress, netInterface);
    }

    public void leaveGroup(InetAddress groupAddr) throws IOException {
        checkJoinOrLeave(groupAddr);
        this.impl.leave(groupAddr);
    }

    public void leaveGroup(SocketAddress groupAddress, NetworkInterface netInterface) throws IOException {
        checkJoinOrLeave(groupAddress, netInterface);
        this.impl.leaveGroup(groupAddress, netInterface);
    }

    private void checkJoinOrLeave(SocketAddress groupAddress, NetworkInterface netInterface) throws IOException {
        checkOpen();
        if (groupAddress == null) {
            throw new IllegalArgumentException("groupAddress == null");
        }
        if (netInterface != null && !netInterface.getInetAddresses().hasMoreElements()) {
            throw new SocketException("No address associated with interface: " + netInterface);
        }
        if (!(groupAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Group address not an InetSocketAddress: " + groupAddress.getClass());
        }
        InetAddress groupAddr = ((InetSocketAddress) groupAddress).getAddress();
        if (groupAddr == null) {
            throw new SocketException("Group address has no address: " + groupAddress);
        }
        if (!groupAddr.isMulticastAddress()) {
            throw new IOException("Not a multicast group: " + groupAddr);
        }
    }

    private void checkJoinOrLeave(InetAddress groupAddr) throws IOException {
        checkOpen();
        if (groupAddr == null) {
            throw new IllegalArgumentException("groupAddress == null");
        }
        if (!groupAddr.isMulticastAddress()) {
            throw new IOException("Not a multicast group: " + groupAddr);
        }
    }

    @Deprecated
    public void send(DatagramPacket packet, byte ttl) throws IOException {
        checkOpen();
        InetAddress packAddr = packet.getAddress();
        int currTTL = getTimeToLive();
        if (packAddr.isMulticastAddress() && ((byte) currTTL) != ttl) {
            try {
                setTimeToLive(ttl & Character.DIRECTIONALITY_UNDEFINED);
                this.impl.send(packet);
                return;
            } finally {
                setTimeToLive(currTTL);
            }
        }
        this.impl.send(packet);
    }

    public void setInterface(InetAddress address) throws SocketException {
        checkOpen();
        if (address == null) {
            throw new NullPointerException("address == null");
        }
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
        if (networkInterface == null) {
            throw new SocketException("Address not associated with an interface: " + address);
        }
        this.impl.setOption(31, Integer.valueOf(networkInterface.getIndex()));
        this.setAddress = address;
    }

    @Override
    public void setNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        checkOpen();
        if (networkInterface == null) {
            throw new SocketException("networkInterface == null");
        }
        this.impl.setOption(31, Integer.valueOf(networkInterface.getIndex()));
        this.setAddress = null;
    }

    public void setTimeToLive(int ttl) throws IOException {
        checkOpen();
        if (ttl < 0 || ttl > 255) {
            throw new IllegalArgumentException("TimeToLive out of bounds: " + ttl);
        }
        this.impl.setTimeToLive(ttl);
    }

    @Deprecated
    public void setTTL(byte ttl) throws IOException {
        checkOpen();
        this.impl.setTTL(ttl);
    }

    @Override
    synchronized void createSocket(int aPort, InetAddress addr) throws SocketException {
        this.impl = factory != null ? factory.createDatagramSocketImpl() : new PlainDatagramSocketImpl();
        this.impl.create();
        try {
            this.impl.setOption(4, Boolean.TRUE);
            this.impl.bind(aPort, addr);
            this.isBound = true;
        } catch (SocketException e) {
            close();
            throw e;
        }
    }

    public boolean getLoopbackMode() throws SocketException {
        checkOpen();
        return !((Boolean) this.impl.getOption(18)).booleanValue();
    }

    public void setLoopbackMode(boolean disable) throws SocketException {
        checkOpen();
        this.impl.setOption(18, Boolean.valueOf(!disable));
    }
}
