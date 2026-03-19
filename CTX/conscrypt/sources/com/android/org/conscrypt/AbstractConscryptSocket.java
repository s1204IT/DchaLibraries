package com.android.org.conscrypt;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PrivateKey;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

abstract class AbstractConscryptSocket extends SSLSocket {
    @Deprecated
    abstract byte[] getAlpnSelectedProtocol();

    @Override
    public abstract String getApplicationProtocol();

    abstract String[] getApplicationProtocols();

    abstract byte[] getChannelId() throws SSLException;

    public abstract FileDescriptor getFileDescriptor$();

    @Override
    public abstract String getHandshakeApplicationProtocol();

    @Override
    public abstract SSLSession getHandshakeSession();

    abstract String getHostname();

    abstract String getHostnameOrIP();

    abstract int getSoWriteTimeout() throws SocketException;

    abstract byte[] getTlsUnique();

    abstract PeerInfoProvider peerInfoProvider();

    @Deprecated
    abstract void setAlpnProtocols(byte[] bArr);

    @Deprecated
    abstract void setAlpnProtocols(String[] strArr);

    abstract void setApplicationProtocolSelector(ApplicationProtocolSelector applicationProtocolSelector);

    abstract void setApplicationProtocols(String[] strArr);

    abstract void setChannelIdEnabled(boolean z);

    abstract void setChannelIdPrivateKey(PrivateKey privateKey);

    abstract void setHandshakeTimeout(int i) throws SocketException;

    abstract void setHostname(String str);

    abstract void setSoWriteTimeout(int i) throws SocketException;

    abstract void setUseSessionTickets(boolean z);

    AbstractConscryptSocket() {
    }

    AbstractConscryptSocket(String str, int i) throws IOException {
        super(str, i);
    }

    AbstractConscryptSocket(InetAddress inetAddress, int i) throws IOException {
        super(inetAddress, i);
    }

    AbstractConscryptSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        super(str, i, inetAddress, i2);
    }

    AbstractConscryptSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        super(inetAddress, i, inetAddress2, i2);
    }

    @Deprecated
    byte[] getNpnSelectedProtocol() {
        return null;
    }

    @Deprecated
    void setNpnProtocols(byte[] bArr) {
    }
}
