package java.net;

import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import sun.net.ConnectionResetException;

class SocketInputStream extends FileInputStream {
    private boolean closing;
    private boolean eof;
    private AbstractPlainSocketImpl impl;
    private Socket socket;
    private byte[] temp;

    private native int socketRead0(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3) throws IOException;

    SocketInputStream(AbstractPlainSocketImpl abstractPlainSocketImpl) throws IOException {
        super(abstractPlainSocketImpl.getFileDescriptor());
        this.impl = null;
        this.socket = null;
        this.closing = false;
        this.impl = abstractPlainSocketImpl;
        this.socket = abstractPlainSocketImpl.getSocket();
    }

    @Override
    public final FileChannel getChannel() {
        return null;
    }

    private int socketRead(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3) throws IOException {
        return socketRead0(fileDescriptor, bArr, i, i2, i3);
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        return read(bArr, i, i2, this.impl.getTimeout());
    }

    int read(byte[] bArr, int i, int i2, int i3) throws IOException {
        int iSocketRead;
        if (this.eof) {
            return -1;
        }
        if (this.impl.isConnectionReset()) {
            throw new SocketException("Connection reset");
        }
        boolean z = false;
        if (i2 <= 0 || i < 0 || i2 > bArr.length - i) {
            if (i2 == 0) {
                return 0;
            }
            throw new ArrayIndexOutOfBoundsException("length == " + i2 + " off == " + i + " buffer length == " + bArr.length);
        }
        FileDescriptor fileDescriptorAcquireFD = this.impl.acquireFD();
        try {
            BlockGuard.getThreadPolicy().onNetwork();
            iSocketRead = socketRead(fileDescriptorAcquireFD, bArr, i, i2, i3);
        } catch (ConnectionResetException e) {
            this.impl.releaseFD();
            z = true;
        } catch (Throwable th) {
            this.impl.releaseFD();
            throw th;
        }
        if (iSocketRead > 0) {
            this.impl.releaseFD();
            return iSocketRead;
        }
        this.impl.releaseFD();
        if (z) {
            this.impl.setConnectionResetPending();
            this.impl.acquireFD();
            try {
                int iSocketRead2 = socketRead(fileDescriptorAcquireFD, bArr, i, i2, i3);
                if (iSocketRead2 > 0) {
                    this.impl.releaseFD();
                    return iSocketRead2;
                }
            } catch (ConnectionResetException e2) {
            } catch (Throwable th2) {
                this.impl.releaseFD();
                throw th2;
            }
            this.impl.releaseFD();
        }
        if (this.impl.isClosedOrPending()) {
            throw new SocketException("Socket closed");
        }
        if (this.impl.isConnectionResetPending()) {
            this.impl.setConnectionReset();
        }
        if (this.impl.isConnectionReset()) {
            throw new SocketException("Connection reset");
        }
        this.eof = true;
        return -1;
    }

    @Override
    public int read() throws IOException {
        if (this.eof) {
            return -1;
        }
        this.temp = new byte[1];
        if (read(this.temp, 0, 1) <= 0) {
            return -1;
        }
        return this.temp[0] & Character.DIRECTIONALITY_UNDEFINED;
    }

    @Override
    public long skip(long j) throws IOException {
        int i;
        if (j <= 0) {
            return 0L;
        }
        int iMin = (int) Math.min(1024L, j);
        byte[] bArr = new byte[iMin];
        long j2 = j;
        while (j2 > 0 && (i = read(bArr, 0, (int) Math.min(iMin, j2))) >= 0) {
            j2 -= (long) i;
        }
        return j - j2;
    }

    @Override
    public int available() throws IOException {
        if (this.eof) {
            return 0;
        }
        return this.impl.available();
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

    void setEOF(boolean z) {
        this.eof = z;
    }

    @Override
    protected void finalize() {
    }
}
