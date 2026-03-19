package com.android.org.conscrypt;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

public abstract class OpenSSLSocketImpl extends ConscryptSocketBase {
    @Override
    public abstract byte[] getChannelId() throws SSLException;

    @Override
    public abstract SSLSession getHandshakeSession();

    @Override
    public abstract void setChannelIdEnabled(boolean z);

    @Override
    public abstract void setChannelIdPrivateKey(PrivateKey privateKey);

    @Override
    public abstract void setUseSessionTickets(boolean z);

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        super.addHandshakeCompletedListener(handshakeCompletedListener);
    }

    @Override
    public void bind(SocketAddress socketAddress) throws IOException {
        super.bind(socketAddress);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public SocketChannel getChannel() {
        return super.getChannel();
    }

    @Override
    public InetAddress getInetAddress() {
        return super.getInetAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return super.getInputStream();
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return super.getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
        return super.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return super.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return super.getLocalSocketAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return super.getOOBInline();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return super.getOutputStream();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return super.getReceiveBufferSize();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return super.getRemoteSocketAddress();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return super.getReuseAddress();
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return super.getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return super.getSoLinger();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return super.getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return super.getTrafficClass();
    }

    @Override
    public boolean isBound() {
        return super.isBound();
    }

    @Override
    public boolean isClosed() {
        return super.isClosed();
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return super.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return super.isOutputShutdown();
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        super.removeHandshakeCompletedListener(handshakeCompletedListener);
    }

    @Override
    public void setKeepAlive(boolean z) throws SocketException {
        super.setKeepAlive(z);
    }

    @Override
    public void setPerformancePreferences(int i, int i2, int i3) {
        super.setPerformancePreferences(i, i2, i3);
    }

    @Override
    public void setReceiveBufferSize(int i) throws SocketException {
        super.setReceiveBufferSize(i);
    }

    @Override
    public void setReuseAddress(boolean z) throws SocketException {
        super.setReuseAddress(z);
    }

    @Override
    public void setSendBufferSize(int i) throws SocketException {
        super.setSendBufferSize(i);
    }

    @Override
    public void setSoLinger(boolean z, int i) throws SocketException {
        super.setSoLinger(z, i);
    }

    @Override
    public void setTcpNoDelay(boolean z) throws SocketException {
        super.setTcpNoDelay(z);
    }

    @Override
    public void setTrafficClass(int i) throws SocketException {
        super.setTrafficClass(i);
    }

    @Override
    public void shutdownInput() throws IOException {
        super.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        super.shutdownOutput();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    OpenSSLSocketImpl() throws IOException {
    }

    OpenSSLSocketImpl(String str, int i) throws IOException {
        super(str, i);
    }

    OpenSSLSocketImpl(InetAddress inetAddress, int i) throws IOException {
        super(inetAddress, i);
    }

    OpenSSLSocketImpl(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        super(str, i, inetAddress, i2);
    }

    OpenSSLSocketImpl(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        super(inetAddress, i, inetAddress2, i2);
    }

    OpenSSLSocketImpl(Socket socket, String str, int i, boolean z) throws IOException {
        super(socket, str, i, z);
    }

    @Override
    public String getHostname() {
        return super.getHostname();
    }

    @Override
    public void setHostname(String str) {
        super.setHostname(str);
    }

    @Override
    public String getHostnameOrIP() {
        return super.getHostnameOrIP();
    }

    @Override
    public FileDescriptor getFileDescriptor$() {
        return super.getFileDescriptor$();
    }

    @Override
    public void setSoWriteTimeout(int i) throws SocketException {
        super.setSoWriteTimeout(i);
    }

    @Override
    public int getSoWriteTimeout() throws SocketException {
        return super.getSoWriteTimeout();
    }

    @Override
    public void setHandshakeTimeout(int i) throws SocketException {
        super.setHandshakeTimeout(i);
    }

    @Override
    @Deprecated
    public final byte[] getNpnSelectedProtocol() {
        return super.getNpnSelectedProtocol();
    }

    @Override
    @Deprecated
    public final void setNpnProtocols(byte[] bArr) {
        super.setNpnProtocols(bArr);
    }

    @Override
    @Deprecated
    public final void setAlpnProtocols(String[] strArr) {
        if (strArr == null) {
            strArr = EmptyArray.STRING;
        }
        setApplicationProtocols(strArr);
    }

    @Override
    @Deprecated
    public final byte[] getAlpnSelectedProtocol() {
        return SSLUtils.toProtocolBytes(getApplicationProtocol());
    }

    @Override
    @Deprecated
    public final void setAlpnProtocols(byte[] bArr) {
        if (bArr == null) {
            bArr = EmptyArray.BYTE;
        }
        setApplicationProtocols(SSLUtils.decodeProtocols(bArr));
    }
}
