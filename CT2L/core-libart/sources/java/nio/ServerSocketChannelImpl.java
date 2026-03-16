package java.nio;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import libcore.io.IoUtils;

final class ServerSocketChannelImpl extends ServerSocketChannel implements FileDescriptorChannel {
    private final Object acceptLock;
    private final ServerSocketAdapter socket;

    public ServerSocketChannelImpl(SelectorProvider sp) throws IOException {
        super(sp);
        this.acceptLock = new Object();
        this.socket = new ServerSocketAdapter(this);
    }

    @Override
    public ServerSocket socket() {
        return this.socket;
    }

    @Override
    public SocketChannel accept() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (!this.socket.isBound()) {
            throw new NotYetBoundException();
        }
        SocketChannelImpl result = new SocketChannelImpl(provider(), false);
        try {
            begin();
            synchronized (this.acceptLock) {
                try {
                    this.socket.implAccept(result);
                } catch (SocketTimeoutException e) {
                    if (shouldThrowSocketTimeoutExceptionFromAccept(e)) {
                        throw e;
                    }
                }
            }
            end(result.isConnected());
            if (result.isConnected()) {
                return result;
            }
            return null;
        } catch (Throwable th) {
            end(result.isConnected());
            throw th;
        }
    }

    private boolean shouldThrowSocketTimeoutExceptionFromAccept(SocketTimeoutException e) {
        if (isBlocking()) {
            return true;
        }
        Throwable cause = e.getCause();
        return ((cause instanceof ErrnoException) && ((ErrnoException) cause).errno == OsConstants.EAGAIN) ? false : true;
    }

    @Override
    protected void implConfigureBlocking(boolean blocking) throws IOException {
        IoUtils.setBlocking(this.socket.getFD$(), blocking);
    }

    @Override
    protected synchronized void implCloseSelectableChannel() throws IOException {
        if (!this.socket.isClosed()) {
            this.socket.close();
        }
    }

    @Override
    public FileDescriptor getFD() {
        return this.socket.getFD$();
    }

    private static class ServerSocketAdapter extends ServerSocket {
        private final ServerSocketChannelImpl channelImpl;

        ServerSocketAdapter(ServerSocketChannelImpl aChannelImpl) throws IOException {
            this.channelImpl = aChannelImpl;
        }

        @Override
        public Socket accept() throws IOException {
            if (!isBound()) {
                throw new IllegalBlockingModeException();
            }
            SocketChannel sc = this.channelImpl.accept();
            if (sc == null) {
                throw new IllegalBlockingModeException();
            }
            return sc.socket();
        }

        public Socket implAccept(SocketChannelImpl clientSocketChannel) throws IOException {
            Socket clientSocket = clientSocketChannel.socket();
            boolean connectOK = false;
            try {
                synchronized (this) {
                    super.implAccept(clientSocket);
                    InetSocketAddress remoteAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());
                    clientSocketChannel.onAccept(remoteAddress, false);
                }
                connectOK = true;
                return clientSocket;
            } finally {
                if (!connectOK) {
                    clientSocket.close();
                }
            }
        }

        @Override
        public ServerSocketChannel getChannel() {
            return this.channelImpl;
        }

        @Override
        public void close() throws IOException {
            synchronized (this.channelImpl) {
                super.close();
                if (this.channelImpl.isOpen()) {
                    this.channelImpl.close();
                }
            }
        }

        private FileDescriptor getFD$() {
            return super.getImpl$().getFD$();
        }
    }
}
