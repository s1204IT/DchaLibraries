package org.apache.http.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import org.apache.http.HttpInetConnection;
import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

@Deprecated
public class SocketHttpClientConnection extends AbstractHttpClientConnection implements HttpInetConnection {
    private volatile boolean open;
    private Socket socket = null;

    protected void assertNotOpen() {
        if (this.open) {
            throw new IllegalStateException("Connection is already open");
        }
    }

    @Override
    protected void assertOpen() {
        if (!this.open) {
            throw new IllegalStateException("Connection is not open");
        }
    }

    protected SessionInputBuffer createSessionInputBuffer(Socket socket, int i, HttpParams httpParams) throws IOException {
        return new SocketInputBuffer(socket, i, httpParams);
    }

    protected SessionOutputBuffer createSessionOutputBuffer(Socket socket, int i, HttpParams httpParams) throws IOException {
        return new SocketOutputBuffer(socket, i, httpParams);
    }

    protected void bind(Socket socket, HttpParams httpParams) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("Socket may not be null");
        }
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.socket = socket;
        int socketBufferSize = HttpConnectionParams.getSocketBufferSize(httpParams);
        init(createSessionInputBuffer(socket, socketBufferSize, httpParams), createSessionOutputBuffer(socket, socketBufferSize, httpParams), httpParams);
        this.open = true;
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    protected Socket getSocket() {
        return this.socket;
    }

    @Override
    public InetAddress getLocalAddress() {
        if (this.socket != null) {
            return this.socket.getLocalAddress();
        }
        return null;
    }

    @Override
    public int getLocalPort() {
        if (this.socket != null) {
            return this.socket.getLocalPort();
        }
        return -1;
    }

    @Override
    public InetAddress getRemoteAddress() {
        if (this.socket != null) {
            return this.socket.getInetAddress();
        }
        return null;
    }

    @Override
    public int getRemotePort() {
        if (this.socket != null) {
            return this.socket.getPort();
        }
        return -1;
    }

    @Override
    public void setSocketTimeout(int i) {
        assertOpen();
        if (this.socket != null) {
            try {
                this.socket.setSoTimeout(i);
            } catch (SocketException e) {
            }
        }
    }

    @Override
    public int getSocketTimeout() {
        if (this.socket == null) {
            return -1;
        }
        try {
            return this.socket.getSoTimeout();
        } catch (SocketException e) {
            return -1;
        }
    }

    @Override
    public void shutdown() throws IOException {
        this.open = false;
        Socket socket = this.socket;
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void close() throws IOException {
        if (!this.open) {
            return;
        }
        this.open = false;
        doFlush();
        try {
            try {
                this.socket.shutdownOutput();
            } catch (IOException e) {
            }
            try {
                this.socket.shutdownInput();
            } catch (IOException e2) {
            }
        } catch (UnsupportedOperationException e3) {
        }
        this.socket.close();
    }
}
