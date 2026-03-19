package java.io;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;
import java.nio.channels.FileChannel;
import libcore.io.IoBridge;
import libcore.io.IoTracker;
import libcore.io.Libcore;
import sun.nio.ch.FileChannelImpl;

public class RandomAccessFile implements DataOutput, DataInput, Closeable {
    private static final int FLUSH_FDATASYNC = 2;
    private static final int FLUSH_FSYNC = 1;
    private static final int FLUSH_NONE = 0;
    private FileChannel channel;
    private Object closeLock;
    private volatile boolean closed;

    @ReachabilitySensitive
    private FileDescriptor fd;
    private int flushAfterWrite;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private final IoTracker ioTracker;
    private int mode;
    private final String path;
    private boolean rw;
    private final byte[] scratch;

    public RandomAccessFile(String str, String str2) throws FileNotFoundException {
        this(str != null ? new File(str) : null, str2);
    }

    public RandomAccessFile(File file, String str) throws FileNotFoundException {
        this.guard = CloseGuard.get();
        this.scratch = new byte[8];
        this.flushAfterWrite = 0;
        this.channel = null;
        this.closeLock = new Object();
        this.closed = false;
        this.ioTracker = new IoTracker();
        String path = file != null ? file.getPath() : null;
        int i = -1;
        if (str.equals("r")) {
            i = OsConstants.O_RDONLY;
        } else if (str.startsWith("rw")) {
            int i2 = OsConstants.O_RDWR | OsConstants.O_CREAT;
            this.rw = true;
            if (str.length() > 2) {
                if (str.equals("rws")) {
                    this.flushAfterWrite = 1;
                } else if (str.equals("rwd")) {
                    this.flushAfterWrite = 2;
                }
                i = i2;
            } else {
                i = i2;
            }
        }
        if (i < 0) {
            throw new IllegalArgumentException("Illegal mode \"" + str + "\" must be one of \"r\", \"rw\", \"rws\", or \"rwd\"");
        }
        if (path == null) {
            throw new NullPointerException("file == null");
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        this.path = path;
        this.mode = i;
        this.fd = IoBridge.open(path, i);
        maybeSync();
        this.guard.open("close");
    }

    private void maybeSync() {
        if (this.flushAfterWrite == 1) {
            try {
                this.fd.sync();
            } catch (IOException e) {
            }
        } else if (this.flushAfterWrite == 2) {
            try {
                Os.fdatasync(this.fd);
            } catch (ErrnoException e2) {
            }
        }
    }

    public final FileDescriptor getFD() throws IOException {
        if (this.fd != null) {
            return this.fd;
        }
        throw new IOException();
    }

    public final FileChannel getChannel() {
        FileChannel fileChannel;
        synchronized (this) {
            if (this.channel == null) {
                this.channel = FileChannelImpl.open(this.fd, this.path, true, this.rw, this);
            }
            fileChannel = this.channel;
        }
        return fileChannel;
    }

    public int read() throws IOException {
        if (read(this.scratch, 0, 1) != -1) {
            return this.scratch[0] & Character.DIRECTIONALITY_UNDEFINED;
        }
        return -1;
    }

    private int readBytes(byte[] bArr, int i, int i2) throws IOException {
        this.ioTracker.trackIo(i2, IoTracker.Mode.READ);
        return IoBridge.read(this.fd, bArr, i, i2);
    }

    public int read(byte[] bArr, int i, int i2) throws IOException {
        return readBytes(bArr, i, i2);
    }

    public int read(byte[] bArr) throws IOException {
        return readBytes(bArr, 0, bArr.length);
    }

    @Override
    public final void readFully(byte[] bArr) throws IOException {
        readFully(bArr, 0, bArr.length);
    }

    @Override
    public final void readFully(byte[] bArr, int i, int i2) throws IOException {
        int i3 = 0;
        do {
            int i4 = read(bArr, i + i3, i2 - i3);
            if (i4 < 0) {
                throw new EOFException();
            }
            i3 += i4;
        } while (i3 < i2);
    }

    @Override
    public int skipBytes(int i) throws IOException {
        if (i <= 0) {
            return 0;
        }
        long filePointer = getFilePointer();
        long length = length();
        long j = ((long) i) + filePointer;
        if (j <= length) {
            length = j;
        }
        seek(length);
        return (int) (length - filePointer);
    }

    @Override
    public void write(int i) throws IOException {
        this.scratch[0] = (byte) (i & 255);
        write(this.scratch, 0, 1);
    }

    private void writeBytes(byte[] bArr, int i, int i2) throws IOException {
        this.ioTracker.trackIo(i2, IoTracker.Mode.WRITE);
        IoBridge.write(this.fd, bArr, i, i2);
        maybeSync();
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        writeBytes(bArr, 0, bArr.length);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        writeBytes(bArr, i, i2);
    }

    public long getFilePointer() throws IOException {
        try {
            return Libcore.os.lseek(this.fd, 0L, OsConstants.SEEK_CUR);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public void seek(long j) throws IOException {
        if (j < 0) {
            throw new IOException("offset < 0: " + j);
        }
        try {
            Libcore.os.lseek(this.fd, j, OsConstants.SEEK_SET);
            this.ioTracker.reset();
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public long length() throws IOException {
        try {
            return Libcore.os.fstat(this.fd).st_size;
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public void setLength(long j) throws IOException {
        if (j < 0) {
            throw new IllegalArgumentException("newLength < 0");
        }
        try {
            Libcore.os.ftruncate(this.fd, j);
            if (getFilePointer() > j) {
                seek(j);
            }
            maybeSync();
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    @Override
    public void close() throws IOException {
        this.guard.close();
        synchronized (this.closeLock) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.channel != null && this.channel.isOpen()) {
                this.channel.close();
            }
            IoBridge.closeAndSignalBlockedThreads(this.fd);
        }
    }

    @Override
    public final boolean readBoolean() throws IOException {
        int i = read();
        if (i >= 0) {
            return i != 0;
        }
        throw new EOFException();
    }

    @Override
    public final byte readByte() throws IOException {
        int i = read();
        if (i < 0) {
            throw new EOFException();
        }
        return (byte) i;
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        int i = read();
        if (i < 0) {
            throw new EOFException();
        }
        return i;
    }

    @Override
    public final short readShort() throws IOException {
        int i = read();
        int i2 = read();
        if ((i | i2) < 0) {
            throw new EOFException();
        }
        return (short) ((i << 8) + (i2 << 0));
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        int i = read();
        int i2 = read();
        if ((i | i2) < 0) {
            throw new EOFException();
        }
        return (i << 8) + (i2 << 0);
    }

    @Override
    public final char readChar() throws IOException {
        int i = read();
        int i2 = read();
        if ((i | i2) < 0) {
            throw new EOFException();
        }
        return (char) ((i << 8) + (i2 << 0));
    }

    @Override
    public final int readInt() throws IOException {
        int i = read();
        int i2 = read();
        int i3 = read();
        int i4 = read();
        if ((i | i2 | i3 | i4) < 0) {
            throw new EOFException();
        }
        return (i << 24) + (i2 << 16) + (i3 << 8) + (i4 << 0);
    }

    @Override
    public final long readLong() throws IOException {
        return (((long) readInt()) << 32) + (((long) readInt()) & 4294967295L);
    }

    @Override
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public final String readLine() throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        boolean z = false;
        int i = -1;
        while (!z) {
            i = read();
            if (i != -1 && i != 10) {
                if (i == 13) {
                    long filePointer = getFilePointer();
                    if (read() != 10) {
                        seek(filePointer);
                    }
                } else {
                    stringBuffer.append((char) i);
                }
            }
            z = true;
        }
        if (i == -1 && stringBuffer.length() == 0) {
            return null;
        }
        return stringBuffer.toString();
    }

    @Override
    public final String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    @Override
    public final void writeBoolean(boolean z) throws IOException {
        write(z ? 1 : 0);
    }

    @Override
    public final void writeByte(int i) throws IOException {
        write(i);
    }

    @Override
    public final void writeShort(int i) throws IOException {
        write((i >>> 8) & 255);
        write((i >>> 0) & 255);
    }

    @Override
    public final void writeChar(int i) throws IOException {
        write((i >>> 8) & 255);
        write((i >>> 0) & 255);
    }

    @Override
    public final void writeInt(int i) throws IOException {
        write((i >>> 24) & 255);
        write((i >>> 16) & 255);
        write((i >>> 8) & 255);
        write((i >>> 0) & 255);
    }

    @Override
    public final void writeLong(long j) throws IOException {
        write(((int) (j >>> 56)) & 255);
        write(((int) (j >>> 48)) & 255);
        write(((int) (j >>> 40)) & 255);
        write(((int) (j >>> 32)) & 255);
        write(((int) (j >>> 24)) & 255);
        write(((int) (j >>> 16)) & 255);
        write(((int) (j >>> 8)) & 255);
        write(((int) (j >>> 0)) & 255);
    }

    @Override
    public final void writeFloat(float f) throws IOException {
        writeInt(Float.floatToIntBits(f));
    }

    @Override
    public final void writeDouble(double d) throws IOException {
        writeLong(Double.doubleToLongBits(d));
    }

    @Override
    public final void writeBytes(String str) throws IOException {
        int length = str.length();
        byte[] bArr = new byte[length];
        str.getBytes(0, length, bArr, 0);
        writeBytes(bArr, 0, length);
    }

    @Override
    public final void writeChars(String str) throws IOException {
        int length = str.length();
        int i = 2 * length;
        byte[] bArr = new byte[i];
        char[] cArr = new char[length];
        str.getChars(0, length, cArr, 0);
        int i2 = 0;
        for (int i3 = 0; i3 < length; i3++) {
            int i4 = i2 + 1;
            bArr[i2] = (byte) (cArr[i3] >>> '\b');
            i2 = i4 + 1;
            bArr[i4] = (byte) (cArr[i3] >>> 0);
        }
        writeBytes(bArr, 0, i);
    }

    @Override
    public final void writeUTF(String str) throws IOException {
        DataOutputStream.writeUTF(str, this);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }
}
