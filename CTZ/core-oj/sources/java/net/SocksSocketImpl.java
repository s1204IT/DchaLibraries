package java.net;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Types;
import sun.security.action.GetPropertyAction;

class SocksSocketImpl extends PlainSocketImpl implements SocksConsts {
    static final boolean $assertionsDisabled = false;
    private boolean applicationSetProxy;
    private InputStream cmdIn;
    private OutputStream cmdOut;
    private Socket cmdsock;
    private InetSocketAddress external_address;
    private String server;
    private int serverPort;
    private boolean useV4;

    SocksSocketImpl() {
        this.server = null;
        this.serverPort = SocksConsts.DEFAULT_PORT;
        this.useV4 = false;
        this.cmdsock = null;
        this.cmdIn = null;
        this.cmdOut = null;
    }

    SocksSocketImpl(String str, int i) {
        this.server = null;
        this.serverPort = SocksConsts.DEFAULT_PORT;
        this.useV4 = false;
        this.cmdsock = null;
        this.cmdIn = null;
        this.cmdOut = null;
        this.server = str;
        this.serverPort = i == -1 ? 1080 : i;
    }

    SocksSocketImpl(Proxy proxy) {
        this.server = null;
        this.serverPort = SocksConsts.DEFAULT_PORT;
        this.useV4 = false;
        this.cmdsock = null;
        this.cmdIn = null;
        this.cmdOut = null;
        SocketAddress socketAddressAddress = proxy.address();
        if (socketAddressAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddressAddress;
            this.server = inetSocketAddress.getHostString();
            this.serverPort = inetSocketAddress.getPort();
        }
    }

    void setV4() {
        this.useV4 = true;
    }

    private synchronized void privilegedConnect(final String str, final int i, final int i2) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    SocksSocketImpl.this.superConnectServer(str, i, i2);
                    SocksSocketImpl.this.cmdIn = SocksSocketImpl.this.getInputStream();
                    SocksSocketImpl.this.cmdOut = SocksSocketImpl.this.getOutputStream();
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((IOException) e.getException());
        }
    }

    private void superConnectServer(String str, int i, int i2) throws IOException {
        super.connect(new InetSocketAddress(str, i), i2);
    }

    private static int remainingMillis(long j) throws IOException {
        if (j == 0) {
            return 0;
        }
        long jCurrentTimeMillis = j - System.currentTimeMillis();
        if (jCurrentTimeMillis > 0) {
            return (int) jCurrentTimeMillis;
        }
        throw new SocketTimeoutException();
    }

    private int readSocksReply(InputStream inputStream, byte[] bArr) throws IOException {
        return readSocksReply(inputStream, bArr, 0L);
    }

    private int readSocksReply(InputStream inputStream, byte[] bArr, long j) throws IOException {
        int length = bArr.length;
        int i = 0;
        for (int i2 = 0; i < length && i2 < 3; i2++) {
            try {
                int i3 = ((SocketInputStream) inputStream).read(bArr, i, length - i, remainingMillis(j));
                if (i3 < 0) {
                    throw new SocketException("Malformed reply from SOCKS server");
                }
                i += i3;
            } catch (SocketTimeoutException e) {
                throw new SocketTimeoutException("Connect timed out");
            }
        }
        return i;
    }

    private boolean authenticate(byte b, InputStream inputStream, BufferedOutputStream bufferedOutputStream) throws IOException {
        return authenticate(b, inputStream, bufferedOutputStream, 0L);
    }

    private boolean authenticate(byte b, InputStream inputStream, BufferedOutputStream bufferedOutputStream, long j) throws IOException {
        String str;
        String userName;
        if (b == 0) {
            return true;
        }
        if (b != 2) {
            return false;
        }
        final InetAddress byName = InetAddress.getByName(this.server);
        PasswordAuthentication passwordAuthentication = (PasswordAuthentication) AccessController.doPrivileged(new PrivilegedAction<PasswordAuthentication>() {
            @Override
            public PasswordAuthentication run() {
                return Authenticator.requestPasswordAuthentication(SocksSocketImpl.this.server, byName, SocksSocketImpl.this.serverPort, "SOCKS5", "SOCKS authentication", null);
            }
        });
        if (passwordAuthentication != null) {
            userName = passwordAuthentication.getUserName();
            str = new String(passwordAuthentication.getPassword());
        } else {
            str = null;
            userName = (String) AccessController.doPrivileged(new GetPropertyAction("user.name"));
        }
        if (userName == null) {
            return false;
        }
        bufferedOutputStream.write(1);
        bufferedOutputStream.write(userName.length());
        try {
            bufferedOutputStream.write(userName.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
        }
        if (str != null) {
            bufferedOutputStream.write(str.length());
            try {
                bufferedOutputStream.write(str.getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException e2) {
            }
        } else {
            bufferedOutputStream.write(0);
        }
        bufferedOutputStream.flush();
        byte[] bArr = new byte[2];
        if (readSocksReply(inputStream, bArr, j) == 2 && bArr[1] == 0) {
            return true;
        }
        bufferedOutputStream.close();
        inputStream.close();
        return false;
    }

    private void connectV4(InputStream inputStream, OutputStream outputStream, InetSocketAddress inetSocketAddress, long j) throws IOException {
        if (!(inetSocketAddress.getAddress() instanceof Inet4Address)) {
            throw new SocketException("SOCKS V4 requires IPv4 only addresses");
        }
        outputStream.write(4);
        outputStream.write(1);
        outputStream.write((inetSocketAddress.getPort() >> 8) & 255);
        outputStream.write((inetSocketAddress.getPort() >> 0) & 255);
        outputStream.write(inetSocketAddress.getAddress().getAddress());
        try {
            outputStream.write(getUserName().getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
        }
        outputStream.write(0);
        outputStream.flush();
        byte[] bArr = new byte[8];
        int socksReply = readSocksReply(inputStream, bArr, j);
        if (socksReply != 8) {
            throw new SocketException("Reply from SOCKS server has bad length: " + socksReply);
        }
        if (bArr[0] != 0 && bArr[0] != 4) {
            throw new SocketException("Reply from SOCKS server has bad version");
        }
        SocketException socketException = null;
        switch (bArr[1]) {
            case 90:
                this.external_address = inetSocketAddress;
                break;
            case Types.DATE:
                socketException = new SocketException("SOCKS request rejected");
                break;
            case Types.TIME:
                socketException = new SocketException("SOCKS server couldn't reach destination");
                break;
            case Types.TIMESTAMP:
                socketException = new SocketException("SOCKS authentication failed");
                break;
            default:
                socketException = new SocketException("Reply from SOCKS server contains bad status");
                break;
        }
        if (socketException != null) {
            inputStream.close();
            outputStream.close();
            throw socketException;
        }
    }

    @Override
    protected void connect(SocketAddress socketAddress, int i) throws IOException {
        long j = 0;
        if (i != 0) {
            long jCurrentTimeMillis = System.currentTimeMillis() + ((long) i);
            j = jCurrentTimeMillis < 0 ? Long.MAX_VALUE : jCurrentTimeMillis;
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (socketAddress == null || !(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        if (securityManager != null) {
            if (inetSocketAddress.isUnresolved()) {
                securityManager.checkConnect(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
            } else {
                securityManager.checkConnect(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
            }
        }
        if (this.server == null) {
            super.connect(inetSocketAddress, remainingMillis(j));
            return;
        }
        try {
            privilegedConnect(this.server, this.serverPort, remainingMillis(j));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(this.cmdOut, 512);
            InputStream inputStream = this.cmdIn;
            if (this.useV4) {
                if (inetSocketAddress.isUnresolved()) {
                    throw new UnknownHostException(inetSocketAddress.toString());
                }
                connectV4(inputStream, bufferedOutputStream, inetSocketAddress, j);
                return;
            }
            bufferedOutputStream.write(5);
            bufferedOutputStream.write(2);
            bufferedOutputStream.write(0);
            bufferedOutputStream.write(2);
            bufferedOutputStream.flush();
            byte[] bArr = new byte[2];
            if (readSocksReply(inputStream, bArr, j) != 2 || bArr[0] != 5) {
                if (inetSocketAddress.isUnresolved()) {
                    throw new UnknownHostException(inetSocketAddress.toString());
                }
                connectV4(inputStream, bufferedOutputStream, inetSocketAddress, j);
                return;
            }
            if (bArr[1] == -1) {
                throw new SocketException("SOCKS : No acceptable methods");
            }
            if (!authenticate(bArr[1], inputStream, bufferedOutputStream, j)) {
                throw new SocketException("SOCKS : authentication failed");
            }
            bufferedOutputStream.write(5);
            bufferedOutputStream.write(1);
            bufferedOutputStream.write(0);
            if (inetSocketAddress.isUnresolved()) {
                bufferedOutputStream.write(3);
                bufferedOutputStream.write(inetSocketAddress.getHostName().length());
                try {
                    bufferedOutputStream.write(inetSocketAddress.getHostName().getBytes("ISO-8859-1"));
                } catch (UnsupportedEncodingException e) {
                }
                bufferedOutputStream.write((inetSocketAddress.getPort() >> 8) & 255);
                bufferedOutputStream.write((inetSocketAddress.getPort() >> 0) & 255);
            } else if (inetSocketAddress.getAddress() instanceof Inet6Address) {
                bufferedOutputStream.write(4);
                bufferedOutputStream.write(inetSocketAddress.getAddress().getAddress());
                bufferedOutputStream.write((inetSocketAddress.getPort() >> 8) & 255);
                bufferedOutputStream.write((inetSocketAddress.getPort() >> 0) & 255);
            } else {
                bufferedOutputStream.write(1);
                bufferedOutputStream.write(inetSocketAddress.getAddress().getAddress());
                bufferedOutputStream.write((inetSocketAddress.getPort() >> 8) & 255);
                bufferedOutputStream.write((inetSocketAddress.getPort() >> 0) & 255);
            }
            bufferedOutputStream.flush();
            byte[] bArr2 = new byte[4];
            if (readSocksReply(inputStream, bArr2, j) != 4) {
                throw new SocketException("Reply from SOCKS server has bad length");
            }
            SocketException socketException = null;
            switch (bArr2[1]) {
                case 0:
                    byte b = bArr2[3];
                    if (b == 1) {
                        if (readSocksReply(inputStream, new byte[4], j) != 4) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        if (readSocksReply(inputStream, new byte[2], j) != 2) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                    } else {
                        switch (b) {
                            case 3:
                                int i2 = bArr2[1];
                                if (readSocksReply(inputStream, new byte[i2], j) != i2) {
                                    throw new SocketException("Reply from SOCKS server badly formatted");
                                }
                                if (readSocksReply(inputStream, new byte[2], j) != 2) {
                                    throw new SocketException("Reply from SOCKS server badly formatted");
                                }
                                break;
                            case 4:
                                int i3 = bArr2[1];
                                if (readSocksReply(inputStream, new byte[i3], j) != i3) {
                                    throw new SocketException("Reply from SOCKS server badly formatted");
                                }
                                if (readSocksReply(inputStream, new byte[2], j) != 2) {
                                    throw new SocketException("Reply from SOCKS server badly formatted");
                                }
                                break;
                            default:
                                socketException = new SocketException("Reply from SOCKS server contains wrong code");
                                break;
                        }
                    }
                    break;
                case 1:
                    socketException = new SocketException("SOCKS server general failure");
                    break;
                case 2:
                    socketException = new SocketException("SOCKS: Connection not allowed by ruleset");
                    break;
                case 3:
                    socketException = new SocketException("SOCKS: Network unreachable");
                    break;
                case 4:
                    socketException = new SocketException("SOCKS: Host unreachable");
                    break;
                case 5:
                    socketException = new SocketException("SOCKS: Connection refused");
                    break;
                case 6:
                    socketException = new SocketException("SOCKS: TTL expired");
                    break;
                case 7:
                    socketException = new SocketException("SOCKS: Command not supported");
                    break;
                case 8:
                    socketException = new SocketException("SOCKS: address type not supported");
                    break;
            }
            if (socketException != null) {
                inputStream.close();
                bufferedOutputStream.close();
                throw socketException;
            }
            this.external_address = inetSocketAddress;
        } catch (IOException e2) {
            throw new SocketException(e2.getMessage());
        }
    }

    @Override
    protected InetAddress getInetAddress() {
        if (this.external_address != null) {
            return this.external_address.getAddress();
        }
        return super.getInetAddress();
    }

    @Override
    protected int getPort() {
        if (this.external_address != null) {
            return this.external_address.getPort();
        }
        return super.getPort();
    }

    @Override
    protected int getLocalPort() {
        if (this.socket != null) {
            return super.getLocalPort();
        }
        if (this.external_address != null) {
            return this.external_address.getPort();
        }
        return super.getLocalPort();
    }

    @Override
    protected void close() throws IOException {
        if (this.cmdsock != null) {
            this.cmdsock.close();
        }
        this.cmdsock = null;
        super.close();
    }

    private String getUserName() {
        if (this.applicationSetProxy) {
            try {
                return System.getProperty("user.name");
            } catch (SecurityException e) {
                return "";
            }
        }
        return (String) AccessController.doPrivileged(new GetPropertyAction("user.name"));
    }
}
