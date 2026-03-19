package java.net;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructGroupReq;
import java.io.IOException;
import jdk.net.ExtendedSocketOptions;
import jdk.net.SocketFlow;
import libcore.io.IoBridge;
import libcore.io.Libcore;
import libcore.util.EmptyArray;
import sun.net.ExtendedOptionsImpl;

class PlainDatagramSocketImpl extends AbstractPlainDatagramSocketImpl {
    PlainDatagramSocketImpl() {
    }

    @Override
    protected <T> void setOption(SocketOption<T> socketOption, T t) throws IOException {
        if (!socketOption.equals(ExtendedSocketOptions.SO_FLOW_SLA)) {
            super.setOption(socketOption, t);
        } else {
            if (isClosed()) {
                throw new SocketException("Socket closed");
            }
            ExtendedOptionsImpl.checkSetOptionPermission(socketOption);
            ExtendedOptionsImpl.checkValueType(t, SocketFlow.class);
            ExtendedOptionsImpl.setFlowOption(getFileDescriptor(), (SocketFlow) t);
        }
    }

    @Override
    protected <T> T getOption(SocketOption<T> socketOption) throws IOException {
        if (!socketOption.equals(ExtendedSocketOptions.SO_FLOW_SLA)) {
            return (T) super.getOption(socketOption);
        }
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        ExtendedOptionsImpl.checkGetOptionPermission(socketOption);
        ?? r2 = (T) SocketFlow.create();
        ExtendedOptionsImpl.getFlowOption(getFileDescriptor(), r2);
        return r2;
    }

    @Override
    protected void socketSetOption(int i, Object obj) throws SocketException {
        try {
            socketSetOption0(i, obj);
        } catch (SocketException e) {
            if (!this.connected) {
                throw e;
            }
        }
    }

    @Override
    protected synchronized void bind0(int i, InetAddress inetAddress) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        IoBridge.bind(this.fd, inetAddress, i);
        if (i == 0) {
            this.localPort = IoBridge.getLocalInetSocketAddress(this.fd).getPort();
        } else {
            this.localPort = i;
        }
    }

    @Override
    protected void send(DatagramPacket datagramPacket) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        if (datagramPacket.getData() == null || datagramPacket.getAddress() == null) {
            throw new NullPointerException("null buffer || null address");
        }
        IoBridge.sendto(this.fd, datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength(), 0, this.connected ? null : datagramPacket.getAddress(), this.connected ? 0 : datagramPacket.getPort());
    }

    @Override
    protected synchronized int peek(InetAddress inetAddress) throws IOException {
        DatagramPacket datagramPacket;
        datagramPacket = new DatagramPacket(EmptyArray.BYTE, 0);
        doRecv(datagramPacket, OsConstants.MSG_PEEK);
        inetAddress.holder().address = datagramPacket.getAddress().holder().address;
        return datagramPacket.getPort();
    }

    @Override
    protected synchronized int peekData(DatagramPacket datagramPacket) throws IOException {
        doRecv(datagramPacket, OsConstants.MSG_PEEK);
        return datagramPacket.getPort();
    }

    @Override
    protected synchronized void receive0(DatagramPacket datagramPacket) throws IOException {
        doRecv(datagramPacket, 0);
    }

    private void doRecv(DatagramPacket datagramPacket, int i) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        if (this.timeout != 0) {
            IoBridge.poll(this.fd, OsConstants.POLLIN | OsConstants.POLLERR, this.timeout);
        }
        IoBridge.recvfrom(false, this.fd, datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.bufLength, i, datagramPacket, this.connected);
    }

    @Override
    protected void setTimeToLive(int i) throws IOException {
        IoBridge.setSocketOption(this.fd, 17, Integer.valueOf(i));
    }

    @Override
    protected int getTimeToLive() throws IOException {
        return ((Integer) IoBridge.getSocketOption(this.fd, 17)).intValue();
    }

    @Override
    protected void setTTL(byte b) throws IOException {
        setTimeToLive(b & Character.DIRECTIONALITY_UNDEFINED);
    }

    @Override
    protected byte getTTL() throws IOException {
        return (byte) getTimeToLive();
    }

    private static StructGroupReq makeGroupReq(InetAddress inetAddress, NetworkInterface networkInterface) {
        return new StructGroupReq(networkInterface != null ? networkInterface.getIndex() : 0, inetAddress);
    }

    @Override
    protected void join(InetAddress inetAddress, NetworkInterface networkInterface) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        IoBridge.setSocketOption(this.fd, 19, makeGroupReq(inetAddress, networkInterface));
    }

    @Override
    protected void leave(InetAddress inetAddress, NetworkInterface networkInterface) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        IoBridge.setSocketOption(this.fd, 20, makeGroupReq(inetAddress, networkInterface));
    }

    @Override
    protected void datagramSocketCreate() throws SocketException {
        this.fd = IoBridge.socket(OsConstants.AF_INET6, OsConstants.SOCK_DGRAM, 0);
        IoBridge.setSocketOption(this.fd, 32, true);
        try {
            Libcore.os.setsockoptInt(this.fd, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_ALL, 0);
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }

    @Override
    protected void datagramSocketClose() {
        try {
            IoBridge.closeAndSignalBlockedThreads(this.fd);
        } catch (IOException e) {
        }
    }

    protected void socketSetOption0(int i, Object obj) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        IoBridge.setSocketOption(this.fd, i, obj);
    }

    @Override
    protected Object socketGetOption(int i) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        return IoBridge.getSocketOption(this.fd, i);
    }

    @Override
    protected void connect0(InetAddress inetAddress, int i) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket closed");
        }
        IoBridge.connect(this.fd, inetAddress, i);
    }

    @Override
    protected void disconnect0(int i) {
        if (isClosed()) {
            return;
        }
        InetAddress inetAddress = new InetAddress();
        inetAddress.holder().family = OsConstants.AF_UNSPEC;
        try {
            IoBridge.connect(this.fd, inetAddress, 0);
        } catch (SocketException e) {
        }
    }
}
