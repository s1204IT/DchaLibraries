package java.net;

public final class DatagramPacket {
    InetAddress address;
    byte[] buf;
    int bufLength;
    int length;
    int offset;
    int port;

    public DatagramPacket(byte[] bArr, int i, int i2) {
        setData(bArr, i, i2);
        this.address = null;
        this.port = -1;
    }

    public DatagramPacket(byte[] bArr, int i) {
        this(bArr, 0, i);
    }

    public DatagramPacket(byte[] bArr, int i, int i2, InetAddress inetAddress, int i3) {
        setData(bArr, i, i2);
        setAddress(inetAddress);
        setPort(i3);
    }

    public DatagramPacket(byte[] bArr, int i, int i2, SocketAddress socketAddress) {
        setData(bArr, i, i2);
        setSocketAddress(socketAddress);
    }

    public DatagramPacket(byte[] bArr, int i, InetAddress inetAddress, int i2) {
        this(bArr, 0, i, inetAddress, i2);
    }

    public DatagramPacket(byte[] bArr, int i, SocketAddress socketAddress) {
        this(bArr, 0, i, socketAddress);
    }

    public synchronized InetAddress getAddress() {
        return this.address;
    }

    public synchronized int getPort() {
        return this.port;
    }

    public synchronized byte[] getData() {
        return this.buf;
    }

    public synchronized int getOffset() {
        return this.offset;
    }

    public synchronized int getLength() {
        return this.length;
    }

    public synchronized void setData(byte[] bArr, int i, int i2) {
        int i3;
        if (i2 >= 0 && i >= 0 && (i3 = i2 + i) >= 0) {
            if (i3 <= bArr.length) {
                this.buf = bArr;
                this.length = i2;
                this.bufLength = i2;
                this.offset = i;
            }
        }
        throw new IllegalArgumentException("illegal length or offset");
    }

    public synchronized void setAddress(InetAddress inetAddress) {
        this.address = inetAddress;
    }

    public void setReceivedLength(int i) {
        this.length = i;
    }

    public synchronized void setPort(int i) {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("Port out of range:" + i);
        }
        this.port = i;
    }

    public synchronized void setSocketAddress(SocketAddress socketAddress) {
        if (socketAddress != null) {
            try {
                if (socketAddress instanceof InetSocketAddress) {
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                    if (inetSocketAddress.isUnresolved()) {
                        throw new IllegalArgumentException("unresolved address");
                    }
                    setAddress(inetSocketAddress.getAddress());
                    setPort(inetSocketAddress.getPort());
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        throw new IllegalArgumentException("unsupported address type");
    }

    public synchronized SocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddress(), getPort());
    }

    public synchronized void setData(byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("null packet buffer");
        }
        this.buf = bArr;
        this.offset = 0;
        this.length = bArr.length;
        this.bufLength = bArr.length;
    }

    public synchronized void setLength(int i) {
        if (this.offset + i > this.buf.length || i < 0 || this.offset + i < 0) {
            throw new IllegalArgumentException("illegal length");
        }
        this.length = i;
        this.bufLength = this.length;
    }
}
