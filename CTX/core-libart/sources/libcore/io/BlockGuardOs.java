package libcore.io;

import android.system.ErrnoException;
import android.system.GaiException;
import android.system.Int64Ref;
import android.system.OsConstants;
import android.system.StructAddrinfo;
import android.system.StructLinger;
import android.system.StructPollfd;
import android.system.StructStat;
import android.system.StructStatVfs;
import dalvik.system.BlockGuard;
import dalvik.system.SocketTagger;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class BlockGuardOs extends ForwardingOs {
    public BlockGuardOs(Os os) {
        super(os);
    }

    private FileDescriptor tagSocket(FileDescriptor fileDescriptor) throws ErrnoException {
        try {
            SocketTagger.get().tag(fileDescriptor);
            return fileDescriptor;
        } catch (SocketException e) {
            throw new ErrnoException("socket", OsConstants.EINVAL, e);
        }
    }

    @Override
    public FileDescriptor accept(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        FileDescriptor fileDescriptorAccept = this.os.accept(fileDescriptor, socketAddress);
        if (isInetSocket(fileDescriptorAccept)) {
            tagSocket(fileDescriptorAccept);
        }
        return fileDescriptorAccept;
    }

    @Override
    public boolean access(String str, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.access(str, i);
    }

    @Override
    public void chmod(String str, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.chmod(str, i);
    }

    @Override
    public void chown(String str, int i, int i2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.chown(str, i, i2);
    }

    @Override
    public void close(FileDescriptor fileDescriptor) throws ErrnoException {
        try {
            if (fileDescriptor.isSocket$() && isLingerSocket(fileDescriptor)) {
                BlockGuard.getThreadPolicy().onNetwork();
            }
        } catch (ErrnoException e) {
        }
        this.os.close(fileDescriptor);
    }

    private static boolean isInetSocket(FileDescriptor fileDescriptor) throws ErrnoException {
        return isInetDomain(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_DOMAIN));
    }

    private static boolean isInetDomain(int i) {
        return i == OsConstants.AF_INET || i == OsConstants.AF_INET6;
    }

    private static boolean isLingerSocket(FileDescriptor fileDescriptor) throws ErrnoException {
        StructLinger structLinger = Libcore.os.getsockoptLinger(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_LINGER);
        return structLinger.isOn() && structLinger.l_linger > 0;
    }

    @Override
    public void connect(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException, ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        this.os.connect(fileDescriptor, inetAddress, i);
    }

    @Override
    public void connect(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        this.os.connect(fileDescriptor, socketAddress);
    }

    @Override
    public void fchmod(FileDescriptor fileDescriptor, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.fchmod(fileDescriptor, i);
    }

    @Override
    public void fchown(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.fchown(fileDescriptor, i, i2);
    }

    @Override
    public void fdatasync(FileDescriptor fileDescriptor) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.fdatasync(fileDescriptor);
    }

    @Override
    public StructStat fstat(FileDescriptor fileDescriptor) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.fstat(fileDescriptor);
    }

    @Override
    public StructStatVfs fstatvfs(FileDescriptor fileDescriptor) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.fstatvfs(fileDescriptor);
    }

    @Override
    public void fsync(FileDescriptor fileDescriptor) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.fsync(fileDescriptor);
    }

    @Override
    public void ftruncate(FileDescriptor fileDescriptor, long j) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.ftruncate(fileDescriptor, j);
    }

    @Override
    public InetAddress[] android_getaddrinfo(String str, StructAddrinfo structAddrinfo, int i) throws GaiException {
        if (!((structAddrinfo.ai_flags & OsConstants.AI_NUMERICHOST) != 0)) {
            BlockGuard.getThreadPolicy().onNetwork();
        }
        return this.os.android_getaddrinfo(str, structAddrinfo, i);
    }

    @Override
    public void lchown(String str, int i, int i2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.lchown(str, i, i2);
    }

    @Override
    public void link(String str, String str2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.link(str, str2);
    }

    @Override
    public long lseek(FileDescriptor fileDescriptor, long j, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.lseek(fileDescriptor, j, i);
    }

    @Override
    public StructStat lstat(String str) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.lstat(str);
    }

    @Override
    public void mkdir(String str, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.mkdir(str, i);
    }

    @Override
    public void mkfifo(String str, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.mkfifo(str, i);
    }

    @Override
    public FileDescriptor open(String str, int i, int i2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        if ((OsConstants.O_ACCMODE & i) != OsConstants.O_RDONLY) {
            BlockGuard.getThreadPolicy().onWriteToDisk();
        }
        return this.os.open(str, i, i2);
    }

    @Override
    public int poll(StructPollfd[] structPollfdArr, int i) throws ErrnoException {
        if (i != 0) {
            BlockGuard.getThreadPolicy().onNetwork();
        }
        return this.os.poll(structPollfdArr, i);
    }

    @Override
    public void posix_fallocate(FileDescriptor fileDescriptor, long j, long j2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.posix_fallocate(fileDescriptor, j, j2);
    }

    @Override
    public int pread(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.pread(fileDescriptor, byteBuffer, j);
    }

    @Override
    public int pread(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.pread(fileDescriptor, bArr, i, i2, j);
    }

    @Override
    public int pwrite(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return this.os.pwrite(fileDescriptor, byteBuffer, j);
    }

    @Override
    public int pwrite(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return this.os.pwrite(fileDescriptor, bArr, i, i2, j);
    }

    @Override
    public int read(FileDescriptor fileDescriptor, ByteBuffer byteBuffer) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.read(fileDescriptor, byteBuffer);
    }

    @Override
    public int read(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.read(fileDescriptor, bArr, i, i2);
    }

    @Override
    public String readlink(String str) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.readlink(str);
    }

    @Override
    public String realpath(String str) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.realpath(str);
    }

    @Override
    public int readv(FileDescriptor fileDescriptor, Object[] objArr, int[] iArr, int[] iArr2) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.readv(fileDescriptor, objArr, iArr, iArr2);
    }

    @Override
    public int recvfrom(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        return this.os.recvfrom(fileDescriptor, byteBuffer, i, inetSocketAddress);
    }

    @Override
    public int recvfrom(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        return this.os.recvfrom(fileDescriptor, bArr, i, i2, i3, inetSocketAddress);
    }

    @Override
    public void remove(String str) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.remove(str);
    }

    @Override
    public void rename(String str, String str2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.rename(str, str2);
    }

    @Override
    public long sendfile(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, Int64Ref int64Ref, long j) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return this.os.sendfile(fileDescriptor, fileDescriptor2, int64Ref, j);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, InetAddress inetAddress, int i2) throws SocketException, ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        return this.os.sendto(fileDescriptor, byteBuffer, i, inetAddress, i2);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, InetAddress inetAddress, int i4) throws SocketException, ErrnoException {
        if (inetAddress != null) {
            BlockGuard.getThreadPolicy().onNetwork();
        }
        return this.os.sendto(fileDescriptor, bArr, i, i2, i3, inetAddress, i4);
    }

    @Override
    public FileDescriptor socket(int i, int i2, int i3) throws ErrnoException {
        FileDescriptor fileDescriptorSocket = this.os.socket(i, i2, i3);
        if (isInetDomain(i)) {
            tagSocket(fileDescriptorSocket);
        }
        return fileDescriptorSocket;
    }

    @Override
    public void socketpair(int i, int i2, int i3, FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2) throws ErrnoException {
        this.os.socketpair(i, i2, i3, fileDescriptor, fileDescriptor2);
        if (isInetDomain(i)) {
            tagSocket(fileDescriptor);
            tagSocket(fileDescriptor2);
        }
    }

    @Override
    public StructStat stat(String str) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.stat(str);
    }

    @Override
    public StructStatVfs statvfs(String str) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.statvfs(str);
    }

    @Override
    public void symlink(String str, String str2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.symlink(str, str2);
    }

    @Override
    public int write(FileDescriptor fileDescriptor, ByteBuffer byteBuffer) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return this.os.write(fileDescriptor, byteBuffer);
    }

    @Override
    public int write(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return this.os.write(fileDescriptor, bArr, i, i2);
    }

    @Override
    public int writev(FileDescriptor fileDescriptor, Object[] objArr, int[] iArr, int[] iArr2) throws ErrnoException, InterruptedIOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return this.os.writev(fileDescriptor, objArr, iArr, iArr2);
    }

    @Override
    public void execv(String str, String[] strArr) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        this.os.execv(str, strArr);
    }

    @Override
    public void execve(String str, String[] strArr, String[] strArr2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        this.os.execve(str, strArr, strArr2);
    }

    @Override
    public byte[] getxattr(String str, String str2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.getxattr(str, str2);
    }

    @Override
    public void msync(long j, long j2, int i) throws ErrnoException {
        if ((OsConstants.MS_SYNC & i) != 0) {
            BlockGuard.getThreadPolicy().onWriteToDisk();
        }
        this.os.msync(j, j2, i);
    }

    @Override
    public void removexattr(String str, String str2) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.removexattr(str, str2);
    }

    @Override
    public void setxattr(String str, String str2, byte[] bArr, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.setxattr(str, str2, bArr, i);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, SocketAddress socketAddress) throws SocketException, ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        return this.os.sendto(fileDescriptor, bArr, i, i2, i3, socketAddress);
    }

    @Override
    public void unlink(String str) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        this.os.unlink(str);
    }

    @Override
    public long splice(FileDescriptor fileDescriptor, Int64Ref int64Ref, FileDescriptor fileDescriptor2, Int64Ref int64Ref2, long j, int i) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return this.os.splice(fileDescriptor, int64Ref, fileDescriptor2, int64Ref2, j, i);
    }
}
