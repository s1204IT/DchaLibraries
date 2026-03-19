package java.net;

import java.io.FileDescriptor;
import java.io.IOException;

public abstract class DatagramSocketImpl implements SocketOptions {
    protected FileDescriptor fd;
    protected int localPort;
    DatagramSocket socket;

    protected abstract void bind(int i, InetAddress inetAddress) throws SocketException;

    protected abstract void close();

    protected abstract void create() throws SocketException;

    @Deprecated
    protected abstract byte getTTL() throws IOException;

    protected abstract int getTimeToLive() throws IOException;

    protected abstract void join(InetAddress inetAddress) throws IOException;

    protected abstract void joinGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException;

    protected abstract void leave(InetAddress inetAddress) throws IOException;

    protected abstract void leaveGroup(SocketAddress socketAddress, NetworkInterface networkInterface) throws IOException;

    protected abstract int peek(InetAddress inetAddress) throws IOException;

    protected abstract int peekData(DatagramPacket datagramPacket) throws IOException;

    protected abstract void receive(DatagramPacket datagramPacket) throws IOException;

    protected abstract void send(DatagramPacket datagramPacket) throws IOException;

    @Deprecated
    protected abstract void setTTL(byte b) throws IOException;

    protected abstract void setTimeToLive(int i) throws IOException;

    int dataAvailable() {
        return 0;
    }

    void setDatagramSocket(DatagramSocket datagramSocket) {
        this.socket = datagramSocket;
    }

    DatagramSocket getDatagramSocket() {
        return this.socket;
    }

    protected void connect(InetAddress inetAddress, int i) throws SocketException {
    }

    protected void disconnect() {
    }

    protected int getLocalPort() {
        return this.localPort;
    }

    <T> void setOption(SocketOption<T> socketOption, T t) throws IOException {
        if (socketOption == StandardSocketOptions.SO_SNDBUF) {
            setOption(SocketOptions.SO_SNDBUF, t);
            return;
        }
        if (socketOption == StandardSocketOptions.SO_RCVBUF) {
            setOption(SocketOptions.SO_RCVBUF, t);
            return;
        }
        if (socketOption == StandardSocketOptions.SO_REUSEADDR) {
            setOption(4, t);
            return;
        }
        if (socketOption == StandardSocketOptions.IP_TOS) {
            setOption(3, t);
            return;
        }
        if (socketOption == StandardSocketOptions.IP_MULTICAST_IF && (getDatagramSocket() instanceof MulticastSocket)) {
            setOption(31, t);
            return;
        }
        if (socketOption == StandardSocketOptions.IP_MULTICAST_TTL && (getDatagramSocket() instanceof MulticastSocket)) {
            if (!(t instanceof Integer)) {
                throw new IllegalArgumentException("not an integer");
            }
            setTimeToLive(((Integer) t).intValue());
        } else {
            if (socketOption == StandardSocketOptions.IP_MULTICAST_LOOP && (getDatagramSocket() instanceof MulticastSocket)) {
                setOption(18, t);
                return;
            }
            throw new UnsupportedOperationException("unsupported option");
        }
    }

    <T> T getOption(SocketOption<T> socketOption) throws IOException {
        if (socketOption == StandardSocketOptions.SO_SNDBUF) {
            return (T) getOption(SocketOptions.SO_SNDBUF);
        }
        if (socketOption == StandardSocketOptions.SO_RCVBUF) {
            return (T) getOption(SocketOptions.SO_RCVBUF);
        }
        if (socketOption == StandardSocketOptions.SO_REUSEADDR) {
            return (T) getOption(4);
        }
        if (socketOption == StandardSocketOptions.IP_TOS) {
            return (T) getOption(3);
        }
        if (socketOption == StandardSocketOptions.IP_MULTICAST_IF && (getDatagramSocket() instanceof MulticastSocket)) {
            return (T) getOption(31);
        }
        if (socketOption == StandardSocketOptions.IP_MULTICAST_TTL && (getDatagramSocket() instanceof MulticastSocket)) {
            return (T) Integer.valueOf(getTimeToLive());
        }
        if (socketOption == StandardSocketOptions.IP_MULTICAST_LOOP && (getDatagramSocket() instanceof MulticastSocket)) {
            return (T) getOption(18);
        }
        throw new UnsupportedOperationException("unsupported option");
    }

    protected FileDescriptor getFileDescriptor() {
        return this.fd;
    }
}
