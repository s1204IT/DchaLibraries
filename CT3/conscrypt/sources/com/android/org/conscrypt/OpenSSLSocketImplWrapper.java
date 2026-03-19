package com.android.org.conscrypt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class OpenSSLSocketImplWrapper extends OpenSSLSocketImpl {
    private Socket socket;

    protected OpenSSLSocketImplWrapper(Socket socket, String hostname, int port, boolean autoClose, SSLParametersImpl sslParameters) throws IOException {
        super(socket, hostname, port, autoClose, sslParameters);
        if (!socket.isConnected()) {
            throw new SocketException("Socket is not connected.");
        }
        this.socket = socket;
    }

    @Override
    public void connect(SocketAddress sockaddr, int timeout) throws IOException {
        throw new IOException("Underlying socket is already connected.");
    }

    @Override
    public void connect(SocketAddress sockaddr) throws IOException {
        throw new IOException("Underlying socket is already connected.");
    }

    @Override
    public void bind(SocketAddress sockaddr) throws IOException {
        throw new IOException("Underlying socket is already connected.");
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return this.socket.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return this.socket.getLocalSocketAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.socket.getLocalAddress();
    }

    @Override
    public InetAddress getInetAddress() {
        return this.socket.getInetAddress();
    }

    @Override
    public String toString() {
        return "SSL socket over " + this.socket.toString();
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        this.socket.setSoLinger(on, linger);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        this.socket.setTcpNoDelay(on);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        this.socket.setReuseAddress(on);
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        this.socket.setKeepAlive(on);
    }

    @Override
    public void setTrafficClass(int tos) throws SocketException {
        this.socket.setTrafficClass(tos);
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        this.socket.setSendBufferSize(size);
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        this.socket.setReceiveBufferSize(size);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return this.socket.getTcpNoDelay();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return this.socket.getReuseAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return this.socket.getOOBInline();
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return this.socket.getKeepAlive();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return this.socket.getTrafficClass();
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return this.socket.getSoTimeout();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return this.socket.getSoLinger();
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return this.socket.getSendBufferSize();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return this.socket.getReceiveBufferSize();
    }

    @Override
    public boolean isConnected() {
        return this.socket.isConnected();
    }

    @Override
    public boolean isClosed() {
        return this.socket.isClosed();
    }

    @Override
    public boolean isBound() {
        return this.socket.isBound();
    }

    @Override
    public boolean isOutputShutdown() {
        return this.socket.isOutputShutdown();
    }

    @Override
    public boolean isInputShutdown() {
        return this.socket.isInputShutdown();
    }

    @Override
    public int getPort() {
        return this.socket.getPort();
    }

    @Override
    public int getLocalPort() {
        return this.socket.getLocalPort();
    }
}
