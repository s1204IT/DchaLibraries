package java.net;

import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import sun.net.ConnectionResetException;

class SocketOutputStream extends FileOutputStream {
    private boolean closing;
    private AbstractPlainSocketImpl impl;
    private Socket socket;
    private byte[] temp;

    private native void socketWrite0(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws IOException;

    SocketOutputStream(AbstractPlainSocketImpl abstractPlainSocketImpl) throws IOException {
        super(abstractPlainSocketImpl.getFileDescriptor());
        this.impl = null;
        this.temp = new byte[1];
        this.socket = null;
        this.closing = false;
        this.impl = abstractPlainSocketImpl;
        this.socket = abstractPlainSocketImpl.getSocket();
    }

    @Override
    public final FileChannel getChannel() {
        return null;
    }

    private void socketWrite(byte[] bArr, int i, int i2) throws IOException {
        if (i2 <= 0 || i < 0 || i2 > bArr.length - i) {
            if (i2 == 0) {
                return;
            }
            throw new ArrayIndexOutOfBoundsException("len == " + i2 + " off == " + i + " buffer length == " + bArr.length);
        }
        FileDescriptor fileDescriptorAcquireFD = this.impl.acquireFD();
        try {
            try {
                BlockGuard.getThreadPolicy().onNetwork();
                socketWrite0(fileDescriptorAcquireFD, bArr, i, i2);
            } catch (SocketException e) {
                e = e;
                if (e instanceof ConnectionResetException) {
                    this.impl.setConnectionResetPending();
                    e = new SocketException("Connection reset");
                }
                if (this.impl.isClosedOrPending()) {
                    throw new SocketException("Socket closed");
                }
                throw e;
            }
        } finally {
            this.impl.releaseFD();
        }
    }

    @Override
    public void write(int i) throws IOException {
        this.temp[0] = (byte) i;
        socketWrite(this.temp, 0, 1);
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        socketWrite(bArr, 0, bArr.length);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        socketWrite(bArr, i, i2);
    }

    @Override
    public void close() throws IOException {
        if (this.closing) {
            return;
        }
        this.closing = true;
        if (this.socket != null) {
            if (!this.socket.isClosed()) {
                this.socket.close();
            }
        } else {
            this.impl.close();
        }
        this.closing = false;
    }

    @Override
    protected void finalize() {
    }
}
