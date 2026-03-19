package org.apache.http.impl.io;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.apache.http.params.HttpParams;

@Deprecated
public class SocketInputBuffer extends AbstractSessionInputBuffer {
    private final Socket socket;

    public SocketInputBuffer(Socket socket, int i, HttpParams httpParams) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("Socket may not be null");
        }
        this.socket = socket;
        init(socket.getInputStream(), 8192, httpParams);
    }

    @Override
    public boolean isDataAvailable(int i) throws IOException {
        boolean zHasBufferedData = hasBufferedData();
        if (!zHasBufferedData) {
            int soTimeout = this.socket.getSoTimeout();
            try {
                this.socket.setSoTimeout(i);
                fillBuffer();
                return hasBufferedData();
            } catch (InterruptedIOException e) {
                if (e instanceof SocketTimeoutException) {
                    return zHasBufferedData;
                }
                throw e;
            } finally {
                this.socket.setSoTimeout(soTimeout);
            }
        }
        return zHasBufferedData;
    }

    public boolean isStale() throws IOException {
        if (hasBufferedData()) {
            return false;
        }
        int soTimeout = this.socket.getSoTimeout();
        try {
            this.socket.setSoTimeout(1);
            return fillBuffer() == -1;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e2) {
            return true;
        } finally {
            this.socket.setSoTimeout(soTimeout);
        }
    }
}
