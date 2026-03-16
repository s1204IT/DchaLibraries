package libcore.io;

import android.system.ErrnoException;
import android.system.GaiException;
import android.system.StructAddrinfo;
import android.system.StructFlock;
import android.system.StructGroupReq;
import android.system.StructGroupSourceReq;
import android.system.StructLinger;
import android.system.StructPasswd;
import android.system.StructPollfd;
import android.system.StructStat;
import android.system.StructStatVfs;
import android.system.StructTimeval;
import android.system.StructUcred;
import android.system.StructUtsname;
import android.util.MutableInt;
import android.util.MutableLong;
import dalvik.bytecode.Opcodes;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.NioUtils;

public final class Posix implements Os {
    private native int preadBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, long j) throws ErrnoException, InterruptedIOException;

    private native int pwriteBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, long j) throws ErrnoException, InterruptedIOException;

    private native int readBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2) throws ErrnoException, InterruptedIOException;

    private native int recvfromBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, int i3, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException;

    private native int sendtoBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, int i3, InetAddress inetAddress, int i4) throws SocketException, ErrnoException;

    private native int umaskImpl(int i);

    private native int writeBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2) throws ErrnoException, InterruptedIOException;

    @Override
    public native FileDescriptor accept(FileDescriptor fileDescriptor, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException;

    @Override
    public native boolean access(String str, int i) throws ErrnoException;

    @Override
    public native InetAddress[] android_getaddrinfo(String str, StructAddrinfo structAddrinfo, int i) throws GaiException;

    @Override
    public native void bind(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException, ErrnoException;

    @Override
    public native void chmod(String str, int i) throws ErrnoException;

    @Override
    public native void chown(String str, int i, int i2) throws ErrnoException;

    @Override
    public native void close(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native void connect(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException, ErrnoException;

    @Override
    public native FileDescriptor dup(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native FileDescriptor dup2(FileDescriptor fileDescriptor, int i) throws ErrnoException;

    @Override
    public native String[] environ();

    @Override
    public native void execv(String str, String[] strArr) throws ErrnoException;

    @Override
    public native void execve(String str, String[] strArr, String[] strArr2) throws ErrnoException;

    @Override
    public native void fchmod(FileDescriptor fileDescriptor, int i) throws ErrnoException;

    @Override
    public native void fchown(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

    @Override
    public native int fcntlFlock(FileDescriptor fileDescriptor, int i, StructFlock structFlock) throws ErrnoException, InterruptedIOException;

    @Override
    public native int fcntlLong(FileDescriptor fileDescriptor, int i, long j) throws ErrnoException;

    @Override
    public native int fcntlVoid(FileDescriptor fileDescriptor, int i) throws ErrnoException;

    @Override
    public native void fdatasync(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native StructStat fstat(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native StructStatVfs fstatvfs(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native void fsync(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native void ftruncate(FileDescriptor fileDescriptor, long j) throws ErrnoException;

    @Override
    public native String gai_strerror(int i);

    @Override
    public native int getegid();

    @Override
    public native String getenv(String str);

    @Override
    public native int geteuid();

    @Override
    public native int getgid();

    @Override
    public native String getnameinfo(InetAddress inetAddress, int i) throws GaiException;

    @Override
    public native SocketAddress getpeername(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native int getpid();

    @Override
    public native int getppid();

    @Override
    public native StructPasswd getpwnam(String str) throws ErrnoException;

    @Override
    public native StructPasswd getpwuid(int i) throws ErrnoException;

    @Override
    public native SocketAddress getsockname(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native int getsockoptByte(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

    @Override
    public native InetAddress getsockoptInAddr(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

    @Override
    public native int getsockoptInt(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

    @Override
    public native StructLinger getsockoptLinger(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

    @Override
    public native StructTimeval getsockoptTimeval(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

    @Override
    public native StructUcred getsockoptUcred(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

    @Override
    public native int gettid();

    @Override
    public native int getuid();

    @Override
    public native String if_indextoname(int i);

    @Override
    public native InetAddress inet_pton(int i, String str);

    @Override
    public native InetAddress ioctlInetAddress(FileDescriptor fileDescriptor, int i, String str) throws ErrnoException;

    @Override
    public native int ioctlInt(FileDescriptor fileDescriptor, int i, MutableInt mutableInt) throws ErrnoException;

    @Override
    public native boolean isatty(FileDescriptor fileDescriptor);

    @Override
    public native void kill(int i, int i2) throws ErrnoException;

    @Override
    public native void lchown(String str, int i, int i2) throws ErrnoException;

    @Override
    public native void link(String str, String str2) throws ErrnoException;

    @Override
    public native void listen(FileDescriptor fileDescriptor, int i) throws ErrnoException;

    @Override
    public native long lseek(FileDescriptor fileDescriptor, long j, int i) throws ErrnoException;

    @Override
    public native StructStat lstat(String str) throws ErrnoException;

    @Override
    public native void mincore(long j, long j2, byte[] bArr) throws ErrnoException;

    @Override
    public native void mkdir(String str, int i) throws ErrnoException;

    @Override
    public native void mkfifo(String str, int i) throws ErrnoException;

    @Override
    public native void mlock(long j, long j2) throws ErrnoException;

    @Override
    public native long mmap(long j, long j2, int i, int i2, FileDescriptor fileDescriptor, long j3) throws ErrnoException;

    @Override
    public native void msync(long j, long j2, int i) throws ErrnoException;

    @Override
    public native void munlock(long j, long j2) throws ErrnoException;

    @Override
    public native void munmap(long j, long j2) throws ErrnoException;

    @Override
    public native FileDescriptor open(String str, int i, int i2) throws ErrnoException;

    @Override
    public native FileDescriptor[] pipe() throws ErrnoException;

    @Override
    public native int poll(StructPollfd[] structPollfdArr, int i) throws ErrnoException;

    @Override
    public native void posix_fallocate(FileDescriptor fileDescriptor, long j, long j2) throws ErrnoException;

    @Override
    public native int prctl(int i, long j, long j2, long j3, long j4) throws ErrnoException;

    @Override
    public native String readlink(String str) throws ErrnoException;

    @Override
    public native int readv(FileDescriptor fileDescriptor, Object[] objArr, int[] iArr, int[] iArr2) throws ErrnoException, InterruptedIOException;

    @Override
    public native void remove(String str) throws ErrnoException;

    @Override
    public native void rename(String str, String str2) throws ErrnoException;

    @Override
    public native long sendfile(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, MutableLong mutableLong, long j) throws ErrnoException;

    @Override
    public native void setegid(int i) throws ErrnoException;

    @Override
    public native void setenv(String str, String str2, boolean z) throws ErrnoException;

    @Override
    public native void seteuid(int i) throws ErrnoException;

    @Override
    public native void setgid(int i) throws ErrnoException;

    @Override
    public native int setsid() throws ErrnoException;

    @Override
    public native void setsockoptByte(FileDescriptor fileDescriptor, int i, int i2, int i3) throws ErrnoException;

    @Override
    public native void setsockoptGroupReq(FileDescriptor fileDescriptor, int i, int i2, StructGroupReq structGroupReq) throws ErrnoException;

    @Override
    public native void setsockoptGroupSourceReq(FileDescriptor fileDescriptor, int i, int i2, StructGroupSourceReq structGroupSourceReq) throws ErrnoException;

    @Override
    public native void setsockoptIfreq(FileDescriptor fileDescriptor, int i, int i2, String str) throws ErrnoException;

    @Override
    public native void setsockoptInt(FileDescriptor fileDescriptor, int i, int i2, int i3) throws ErrnoException;

    @Override
    public native void setsockoptIpMreqn(FileDescriptor fileDescriptor, int i, int i2, int i3) throws ErrnoException;

    @Override
    public native void setsockoptLinger(FileDescriptor fileDescriptor, int i, int i2, StructLinger structLinger) throws ErrnoException;

    @Override
    public native void setsockoptTimeval(FileDescriptor fileDescriptor, int i, int i2, StructTimeval structTimeval) throws ErrnoException;

    @Override
    public native void setuid(int i) throws ErrnoException;

    @Override
    public native void shutdown(FileDescriptor fileDescriptor, int i) throws ErrnoException;

    @Override
    public native FileDescriptor socket(int i, int i2, int i3) throws ErrnoException;

    @Override
    public native void socketpair(int i, int i2, int i3, FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2) throws ErrnoException;

    @Override
    public native StructStat stat(String str) throws ErrnoException;

    @Override
    public native StructStatVfs statvfs(String str) throws ErrnoException;

    @Override
    public native String strerror(int i);

    @Override
    public native String strsignal(int i);

    @Override
    public native void symlink(String str, String str2) throws ErrnoException;

    @Override
    public native long sysconf(int i);

    @Override
    public native void tcdrain(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native void tcsendbreak(FileDescriptor fileDescriptor, int i) throws ErrnoException;

    @Override
    public native StructUtsname uname();

    @Override
    public native void unsetenv(String str) throws ErrnoException;

    @Override
    public native int waitpid(int i, MutableInt mutableInt, int i2) throws ErrnoException;

    @Override
    public native int writev(FileDescriptor fileDescriptor, Object[] objArr, int[] iArr, int[] iArr2) throws ErrnoException, InterruptedIOException;

    Posix() {
    }

    @Override
    public int pread(FileDescriptor fd, ByteBuffer buffer, long offset) throws ErrnoException, InterruptedIOException {
        int bytesRead;
        int position = buffer.position();
        if (buffer.isDirect()) {
            bytesRead = preadBytes(fd, buffer, position, buffer.remaining(), offset);
        } else {
            bytesRead = preadBytes(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + position, buffer.remaining(), offset);
        }
        maybeUpdateBufferPosition(buffer, position, bytesRead);
        return bytesRead;
    }

    @Override
    public int pread(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, long offset) throws ErrnoException, InterruptedIOException {
        return preadBytes(fd, bytes, byteOffset, byteCount, offset);
    }

    @Override
    public int pwrite(FileDescriptor fd, ByteBuffer buffer, long offset) throws ErrnoException, InterruptedIOException {
        int bytesWritten;
        int position = buffer.position();
        if (buffer.isDirect()) {
            bytesWritten = pwriteBytes(fd, buffer, position, buffer.remaining(), offset);
        } else {
            bytesWritten = pwriteBytes(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + position, buffer.remaining(), offset);
        }
        maybeUpdateBufferPosition(buffer, position, bytesWritten);
        return bytesWritten;
    }

    @Override
    public int pwrite(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, long offset) throws ErrnoException, InterruptedIOException {
        return pwriteBytes(fd, bytes, byteOffset, byteCount, offset);
    }

    @Override
    public int read(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException, InterruptedIOException {
        int bytesRead;
        int position = buffer.position();
        if (buffer.isDirect()) {
            bytesRead = readBytes(fd, buffer, position, buffer.remaining());
        } else {
            bytesRead = readBytes(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + position, buffer.remaining());
        }
        maybeUpdateBufferPosition(buffer, position, bytesRead);
        return bytesRead;
    }

    @Override
    public int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException, InterruptedIOException {
        return readBytes(fd, bytes, byteOffset, byteCount);
    }

    @Override
    public int recvfrom(FileDescriptor fd, ByteBuffer buffer, int flags, InetSocketAddress srcAddress) throws SocketException, ErrnoException {
        int bytesReceived;
        int position = buffer.position();
        if (buffer.isDirect()) {
            bytesReceived = recvfromBytes(fd, buffer, position, buffer.remaining(), flags, srcAddress);
        } else {
            bytesReceived = recvfromBytes(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + position, buffer.remaining(), flags, srcAddress);
        }
        maybeUpdateBufferPosition(buffer, position, bytesReceived);
        return bytesReceived;
    }

    @Override
    public int recvfrom(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, InetSocketAddress srcAddress) throws SocketException, ErrnoException {
        return recvfromBytes(fd, bytes, byteOffset, byteCount, flags, srcAddress);
    }

    @Override
    public int sendto(FileDescriptor fd, ByteBuffer buffer, int flags, InetAddress inetAddress, int port) throws SocketException, ErrnoException {
        int bytesSent;
        int position = buffer.position();
        if (buffer.isDirect()) {
            bytesSent = sendtoBytes(fd, buffer, position, buffer.remaining(), flags, inetAddress, port);
        } else {
            bytesSent = sendtoBytes(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + position, buffer.remaining(), flags, inetAddress, port);
        }
        maybeUpdateBufferPosition(buffer, position, bytesSent);
        return bytesSent;
    }

    @Override
    public int sendto(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, InetAddress inetAddress, int port) throws SocketException, ErrnoException {
        return sendtoBytes(fd, bytes, byteOffset, byteCount, flags, inetAddress, port);
    }

    @Override
    public int umask(int mask) {
        if ((mask & Opcodes.OP_CHECK_CAST_JUMBO) != mask) {
            throw new IllegalArgumentException("Invalid umask: " + mask);
        }
        return umaskImpl(mask);
    }

    @Override
    public int write(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException, InterruptedIOException {
        int bytesWritten;
        int position = buffer.position();
        if (buffer.isDirect()) {
            bytesWritten = writeBytes(fd, buffer, position, buffer.remaining());
        } else {
            bytesWritten = writeBytes(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + position, buffer.remaining());
        }
        maybeUpdateBufferPosition(buffer, position, bytesWritten);
        return bytesWritten;
    }

    @Override
    public int write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException, InterruptedIOException {
        return writeBytes(fd, bytes, byteOffset, byteCount);
    }

    private static void maybeUpdateBufferPosition(ByteBuffer buffer, int originalPosition, int bytesReadOrWritten) {
        if (bytesReadOrWritten > 0) {
            buffer.position(bytesReadOrWritten + originalPosition);
        }
    }
}
