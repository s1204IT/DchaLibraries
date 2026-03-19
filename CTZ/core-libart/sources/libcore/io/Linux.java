package libcore.io;

import android.system.ErrnoException;
import android.system.GaiException;
import android.system.Int32Ref;
import android.system.Int64Ref;
import android.system.StructAddrinfo;
import android.system.StructCapUserData;
import android.system.StructCapUserHeader;
import android.system.StructFlock;
import android.system.StructGroupReq;
import android.system.StructIfaddrs;
import android.system.StructLinger;
import android.system.StructPasswd;
import android.system.StructPollfd;
import android.system.StructRlimit;
import android.system.StructStat;
import android.system.StructStatVfs;
import android.system.StructTimeval;
import android.system.StructUcred;
import android.system.StructUtsname;
import dalvik.bytecode.Opcodes;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.NioUtils;

public final class Linux implements Os {
    private native int preadBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, long j) throws ErrnoException, InterruptedIOException;

    private native int pwriteBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, long j) throws ErrnoException, InterruptedIOException;

    private native int readBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2) throws ErrnoException, InterruptedIOException;

    private native int recvfromBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, int i3, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException;

    private native int sendtoBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, int i3, InetAddress inetAddress, int i4) throws SocketException, ErrnoException;

    private native int sendtoBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2, int i3, SocketAddress socketAddress) throws SocketException, ErrnoException;

    private native int umaskImpl(int i);

    private native int writeBytes(FileDescriptor fileDescriptor, Object obj, int i, int i2) throws ErrnoException, InterruptedIOException;

    @Override
    public native FileDescriptor accept(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException;

    @Override
    public native boolean access(String str, int i) throws ErrnoException;

    @Override
    public native InetAddress[] android_getaddrinfo(String str, StructAddrinfo structAddrinfo, int i) throws GaiException;

    @Override
    public native void bind(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException, ErrnoException;

    @Override
    public native void bind(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException;

    @Override
    public native StructCapUserData[] capget(StructCapUserHeader structCapUserHeader) throws ErrnoException;

    @Override
    public native void capset(StructCapUserHeader structCapUserHeader, StructCapUserData[] structCapUserDataArr) throws ErrnoException;

    @Override
    public native void chmod(String str, int i) throws ErrnoException;

    @Override
    public native void chown(String str, int i, int i2) throws ErrnoException;

    @Override
    public native void close(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native void connect(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException, ErrnoException;

    @Override
    public native void connect(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException;

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
    public native int fcntlInt(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException;

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
    public native StructIfaddrs[] getifaddrs() throws ErrnoException;

    @Override
    public native String getnameinfo(InetAddress inetAddress, int i) throws GaiException;

    @Override
    public native SocketAddress getpeername(FileDescriptor fileDescriptor) throws ErrnoException;

    @Override
    public native int getpgid(int i);

    @Override
    public native int getpid();

    @Override
    public native int getppid();

    @Override
    public native StructPasswd getpwnam(String str) throws ErrnoException;

    @Override
    public native StructPasswd getpwuid(int i) throws ErrnoException;

    @Override
    public native StructRlimit getrlimit(int i) throws ErrnoException;

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
    public native byte[] getxattr(String str, String str2) throws ErrnoException;

    @Override
    public native String if_indextoname(int i);

    @Override
    public native int if_nametoindex(String str);

    @Override
    public native InetAddress inet_pton(int i, String str);

    @Override
    public native int ioctlFlags(FileDescriptor fileDescriptor, String str) throws ErrnoException;

    @Override
    public native InetAddress ioctlInetAddress(FileDescriptor fileDescriptor, int i, String str) throws ErrnoException;

    @Override
    public native int ioctlInt(FileDescriptor fileDescriptor, int i, Int32Ref int32Ref) throws ErrnoException;

    @Override
    public native int ioctlMTU(FileDescriptor fileDescriptor, String str) throws ErrnoException;

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
    public native String[] listxattr(String str) throws ErrnoException;

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
    public native FileDescriptor[] pipe2(int i) throws ErrnoException;

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
    public native String realpath(String str) throws ErrnoException;

    @Override
    public native void remove(String str) throws ErrnoException;

    @Override
    public native void removexattr(String str, String str2) throws ErrnoException;

    @Override
    public native void rename(String str, String str2) throws ErrnoException;

    @Override
    public native long sendfile(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, Int64Ref int64Ref, long j) throws ErrnoException;

    @Override
    public native void setegid(int i) throws ErrnoException;

    @Override
    public native void setenv(String str, String str2, boolean z) throws ErrnoException;

    @Override
    public native void seteuid(int i) throws ErrnoException;

    @Override
    public native void setgid(int i) throws ErrnoException;

    @Override
    public native void setpgid(int i, int i2) throws ErrnoException;

    @Override
    public native void setregid(int i, int i2) throws ErrnoException;

    @Override
    public native void setreuid(int i, int i2) throws ErrnoException;

    @Override
    public native int setsid() throws ErrnoException;

    @Override
    public native void setsockoptByte(FileDescriptor fileDescriptor, int i, int i2, int i3) throws ErrnoException;

    @Override
    public native void setsockoptGroupReq(FileDescriptor fileDescriptor, int i, int i2, StructGroupReq structGroupReq) throws ErrnoException;

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
    public native void setxattr(String str, String str2, byte[] bArr, int i) throws ErrnoException;

    @Override
    public native void shutdown(FileDescriptor fileDescriptor, int i) throws ErrnoException;

    @Override
    public native FileDescriptor socket(int i, int i2, int i3) throws ErrnoException;

    @Override
    public native void socketpair(int i, int i2, int i3, FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2) throws ErrnoException;

    @Override
    public native long splice(FileDescriptor fileDescriptor, Int64Ref int64Ref, FileDescriptor fileDescriptor2, Int64Ref int64Ref2, long j, int i) throws ErrnoException;

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
    public native void unlink(String str) throws ErrnoException;

    @Override
    public native void unsetenv(String str) throws ErrnoException;

    @Override
    public native int waitpid(int i, Int32Ref int32Ref, int i2) throws ErrnoException;

    @Override
    public native int writev(FileDescriptor fileDescriptor, Object[] objArr, int[] iArr, int[] iArr2) throws ErrnoException, InterruptedIOException;

    Linux() {
    }

    @Override
    public int pread(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j) throws ErrnoException, InterruptedIOException {
        int iPreadBytes;
        int iPosition = byteBuffer.position();
        if (byteBuffer.isDirect()) {
            iPreadBytes = preadBytes(fileDescriptor, byteBuffer, iPosition, byteBuffer.remaining(), j);
        } else {
            iPreadBytes = preadBytes(fileDescriptor, NioUtils.unsafeArray(byteBuffer), NioUtils.unsafeArrayOffset(byteBuffer) + iPosition, byteBuffer.remaining(), j);
        }
        maybeUpdateBufferPosition(byteBuffer, iPosition, iPreadBytes);
        return iPreadBytes;
    }

    @Override
    public int pread(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, InterruptedIOException {
        return preadBytes(fileDescriptor, bArr, i, i2, j);
    }

    @Override
    public int pwrite(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j) throws ErrnoException, InterruptedIOException {
        int iPwriteBytes;
        int iPosition = byteBuffer.position();
        if (byteBuffer.isDirect()) {
            iPwriteBytes = pwriteBytes(fileDescriptor, byteBuffer, iPosition, byteBuffer.remaining(), j);
        } else {
            iPwriteBytes = pwriteBytes(fileDescriptor, NioUtils.unsafeArray(byteBuffer), NioUtils.unsafeArrayOffset(byteBuffer) + iPosition, byteBuffer.remaining(), j);
        }
        maybeUpdateBufferPosition(byteBuffer, iPosition, iPwriteBytes);
        return iPwriteBytes;
    }

    @Override
    public int pwrite(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, InterruptedIOException {
        return pwriteBytes(fileDescriptor, bArr, i, i2, j);
    }

    @Override
    public int read(FileDescriptor fileDescriptor, ByteBuffer byteBuffer) throws ErrnoException, InterruptedIOException {
        int bytes;
        int iPosition = byteBuffer.position();
        if (byteBuffer.isDirect()) {
            bytes = readBytes(fileDescriptor, byteBuffer, iPosition, byteBuffer.remaining());
        } else {
            bytes = readBytes(fileDescriptor, NioUtils.unsafeArray(byteBuffer), NioUtils.unsafeArrayOffset(byteBuffer) + iPosition, byteBuffer.remaining());
        }
        maybeUpdateBufferPosition(byteBuffer, iPosition, bytes);
        return bytes;
    }

    @Override
    public int read(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws ErrnoException, InterruptedIOException {
        return readBytes(fileDescriptor, bArr, i, i2);
    }

    @Override
    public int recvfrom(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException {
        int iRecvfromBytes;
        int iPosition = byteBuffer.position();
        if (byteBuffer.isDirect()) {
            iRecvfromBytes = recvfromBytes(fileDescriptor, byteBuffer, iPosition, byteBuffer.remaining(), i, inetSocketAddress);
        } else {
            iRecvfromBytes = recvfromBytes(fileDescriptor, NioUtils.unsafeArray(byteBuffer), NioUtils.unsafeArrayOffset(byteBuffer) + iPosition, byteBuffer.remaining(), i, inetSocketAddress);
        }
        maybeUpdateBufferPosition(byteBuffer, iPosition, iRecvfromBytes);
        return iRecvfromBytes;
    }

    @Override
    public int recvfrom(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException {
        return recvfromBytes(fileDescriptor, bArr, i, i2, i3, inetSocketAddress);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, InetAddress inetAddress, int i2) throws SocketException, ErrnoException {
        int iSendtoBytes;
        int iPosition = byteBuffer.position();
        if (byteBuffer.isDirect()) {
            iSendtoBytes = sendtoBytes(fileDescriptor, byteBuffer, iPosition, byteBuffer.remaining(), i, inetAddress, i2);
        } else {
            iSendtoBytes = sendtoBytes(fileDescriptor, NioUtils.unsafeArray(byteBuffer), NioUtils.unsafeArrayOffset(byteBuffer) + iPosition, byteBuffer.remaining(), i, inetAddress, i2);
        }
        maybeUpdateBufferPosition(byteBuffer, iPosition, iSendtoBytes);
        return iSendtoBytes;
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, InetAddress inetAddress, int i4) throws SocketException, ErrnoException {
        return sendtoBytes(fileDescriptor, bArr, i, i2, i3, inetAddress, i4);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, SocketAddress socketAddress) throws SocketException, ErrnoException {
        return sendtoBytes(fileDescriptor, bArr, i, i2, i3, socketAddress);
    }

    @Override
    public int umask(int i) {
        if ((i & Opcodes.OP_CHECK_CAST_JUMBO) != i) {
            throw new IllegalArgumentException("Invalid umask: " + i);
        }
        return umaskImpl(i);
    }

    @Override
    public int write(FileDescriptor fileDescriptor, ByteBuffer byteBuffer) throws ErrnoException, InterruptedIOException {
        int iWriteBytes;
        int iPosition = byteBuffer.position();
        if (byteBuffer.isDirect()) {
            iWriteBytes = writeBytes(fileDescriptor, byteBuffer, iPosition, byteBuffer.remaining());
        } else {
            iWriteBytes = writeBytes(fileDescriptor, NioUtils.unsafeArray(byteBuffer), NioUtils.unsafeArrayOffset(byteBuffer) + iPosition, byteBuffer.remaining());
        }
        maybeUpdateBufferPosition(byteBuffer, iPosition, iWriteBytes);
        return iWriteBytes;
    }

    @Override
    public int write(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws ErrnoException, InterruptedIOException {
        return writeBytes(fileDescriptor, bArr, i, i2);
    }

    private static void maybeUpdateBufferPosition(ByteBuffer byteBuffer, int i, int i2) {
        if (i2 > 0) {
            byteBuffer.position(i2 + i);
        }
    }
}
