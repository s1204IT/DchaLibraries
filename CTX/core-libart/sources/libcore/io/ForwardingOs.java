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
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class ForwardingOs implements Os {
    protected final Os os;

    public ForwardingOs(Os os) {
        this.os = os;
    }

    @Override
    public FileDescriptor accept(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException {
        return this.os.accept(fileDescriptor, socketAddress);
    }

    @Override
    public boolean access(String str, int i) throws ErrnoException {
        return this.os.access(str, i);
    }

    @Override
    public InetAddress[] android_getaddrinfo(String str, StructAddrinfo structAddrinfo, int i) throws GaiException {
        return this.os.android_getaddrinfo(str, structAddrinfo, i);
    }

    @Override
    public void bind(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException, ErrnoException {
        this.os.bind(fileDescriptor, inetAddress, i);
    }

    @Override
    public void bind(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException {
        this.os.bind(fileDescriptor, socketAddress);
    }

    @Override
    public StructCapUserData[] capget(StructCapUserHeader structCapUserHeader) throws ErrnoException {
        return this.os.capget(structCapUserHeader);
    }

    @Override
    public void capset(StructCapUserHeader structCapUserHeader, StructCapUserData[] structCapUserDataArr) throws ErrnoException {
        this.os.capset(structCapUserHeader, structCapUserDataArr);
    }

    @Override
    public void chmod(String str, int i) throws ErrnoException {
        this.os.chmod(str, i);
    }

    @Override
    public void chown(String str, int i, int i2) throws ErrnoException {
        this.os.chown(str, i, i2);
    }

    @Override
    public void close(FileDescriptor fileDescriptor) throws ErrnoException {
        this.os.close(fileDescriptor);
    }

    @Override
    public void connect(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException, ErrnoException {
        this.os.connect(fileDescriptor, inetAddress, i);
    }

    @Override
    public void connect(FileDescriptor fileDescriptor, SocketAddress socketAddress) throws SocketException, ErrnoException {
        this.os.connect(fileDescriptor, socketAddress);
    }

    @Override
    public FileDescriptor dup(FileDescriptor fileDescriptor) throws ErrnoException {
        return this.os.dup(fileDescriptor);
    }

    @Override
    public FileDescriptor dup2(FileDescriptor fileDescriptor, int i) throws ErrnoException {
        return this.os.dup2(fileDescriptor, i);
    }

    @Override
    public String[] environ() {
        return this.os.environ();
    }

    @Override
    public void execv(String str, String[] strArr) throws ErrnoException {
        this.os.execv(str, strArr);
    }

    @Override
    public void execve(String str, String[] strArr, String[] strArr2) throws ErrnoException {
        this.os.execve(str, strArr, strArr2);
    }

    @Override
    public void fchmod(FileDescriptor fileDescriptor, int i) throws ErrnoException {
        this.os.fchmod(fileDescriptor, i);
    }

    @Override
    public void fchown(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        this.os.fchown(fileDescriptor, i, i2);
    }

    @Override
    public int fcntlFlock(FileDescriptor fileDescriptor, int i, StructFlock structFlock) throws ErrnoException, InterruptedIOException {
        return this.os.fcntlFlock(fileDescriptor, i, structFlock);
    }

    @Override
    public int fcntlInt(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        return this.os.fcntlInt(fileDescriptor, i, i2);
    }

    @Override
    public int fcntlVoid(FileDescriptor fileDescriptor, int i) throws ErrnoException {
        return this.os.fcntlVoid(fileDescriptor, i);
    }

    @Override
    public void fdatasync(FileDescriptor fileDescriptor) throws ErrnoException {
        this.os.fdatasync(fileDescriptor);
    }

    @Override
    public StructStat fstat(FileDescriptor fileDescriptor) throws ErrnoException {
        return this.os.fstat(fileDescriptor);
    }

    @Override
    public StructStatVfs fstatvfs(FileDescriptor fileDescriptor) throws ErrnoException {
        return this.os.fstatvfs(fileDescriptor);
    }

    @Override
    public void fsync(FileDescriptor fileDescriptor) throws ErrnoException {
        this.os.fsync(fileDescriptor);
    }

    @Override
    public void ftruncate(FileDescriptor fileDescriptor, long j) throws ErrnoException {
        this.os.ftruncate(fileDescriptor, j);
    }

    @Override
    public String gai_strerror(int i) {
        return this.os.gai_strerror(i);
    }

    @Override
    public int getegid() {
        return this.os.getegid();
    }

    @Override
    public int geteuid() {
        return this.os.geteuid();
    }

    @Override
    public int getgid() {
        return this.os.getgid();
    }

    @Override
    public String getenv(String str) {
        return this.os.getenv(str);
    }

    @Override
    public String getnameinfo(InetAddress inetAddress, int i) throws GaiException {
        return this.os.getnameinfo(inetAddress, i);
    }

    @Override
    public SocketAddress getpeername(FileDescriptor fileDescriptor) throws ErrnoException {
        return this.os.getpeername(fileDescriptor);
    }

    @Override
    public int getpgid(int i) throws ErrnoException {
        return this.os.getpgid(i);
    }

    @Override
    public int getpid() {
        return this.os.getpid();
    }

    @Override
    public int getppid() {
        return this.os.getppid();
    }

    @Override
    public StructPasswd getpwnam(String str) throws ErrnoException {
        return this.os.getpwnam(str);
    }

    @Override
    public StructPasswd getpwuid(int i) throws ErrnoException {
        return this.os.getpwuid(i);
    }

    @Override
    public StructRlimit getrlimit(int i) throws ErrnoException {
        return this.os.getrlimit(i);
    }

    @Override
    public SocketAddress getsockname(FileDescriptor fileDescriptor) throws ErrnoException {
        return this.os.getsockname(fileDescriptor);
    }

    @Override
    public int getsockoptByte(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        return this.os.getsockoptByte(fileDescriptor, i, i2);
    }

    @Override
    public InetAddress getsockoptInAddr(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        return this.os.getsockoptInAddr(fileDescriptor, i, i2);
    }

    @Override
    public int getsockoptInt(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        return this.os.getsockoptInt(fileDescriptor, i, i2);
    }

    @Override
    public StructLinger getsockoptLinger(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        return this.os.getsockoptLinger(fileDescriptor, i, i2);
    }

    @Override
    public StructTimeval getsockoptTimeval(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        return this.os.getsockoptTimeval(fileDescriptor, i, i2);
    }

    @Override
    public StructUcred getsockoptUcred(FileDescriptor fileDescriptor, int i, int i2) throws ErrnoException {
        return this.os.getsockoptUcred(fileDescriptor, i, i2);
    }

    @Override
    public int gettid() {
        return this.os.gettid();
    }

    @Override
    public int getuid() {
        return this.os.getuid();
    }

    @Override
    public byte[] getxattr(String str, String str2) throws ErrnoException {
        return this.os.getxattr(str, str2);
    }

    @Override
    public StructIfaddrs[] getifaddrs() throws ErrnoException {
        return this.os.getifaddrs();
    }

    @Override
    public String if_indextoname(int i) {
        return this.os.if_indextoname(i);
    }

    @Override
    public int if_nametoindex(String str) {
        return this.os.if_nametoindex(str);
    }

    @Override
    public InetAddress inet_pton(int i, String str) {
        return this.os.inet_pton(i, str);
    }

    @Override
    public int ioctlFlags(FileDescriptor fileDescriptor, String str) throws ErrnoException {
        return this.os.ioctlFlags(fileDescriptor, str);
    }

    @Override
    public InetAddress ioctlInetAddress(FileDescriptor fileDescriptor, int i, String str) throws ErrnoException {
        return this.os.ioctlInetAddress(fileDescriptor, i, str);
    }

    @Override
    public int ioctlInt(FileDescriptor fileDescriptor, int i, Int32Ref int32Ref) throws ErrnoException {
        return this.os.ioctlInt(fileDescriptor, i, int32Ref);
    }

    @Override
    public int ioctlMTU(FileDescriptor fileDescriptor, String str) throws ErrnoException {
        return this.os.ioctlMTU(fileDescriptor, str);
    }

    @Override
    public boolean isatty(FileDescriptor fileDescriptor) {
        return this.os.isatty(fileDescriptor);
    }

    @Override
    public void kill(int i, int i2) throws ErrnoException {
        this.os.kill(i, i2);
    }

    @Override
    public void lchown(String str, int i, int i2) throws ErrnoException {
        this.os.lchown(str, i, i2);
    }

    @Override
    public void link(String str, String str2) throws ErrnoException {
        this.os.link(str, str2);
    }

    @Override
    public void listen(FileDescriptor fileDescriptor, int i) throws ErrnoException {
        this.os.listen(fileDescriptor, i);
    }

    @Override
    public String[] listxattr(String str) throws ErrnoException {
        return this.os.listxattr(str);
    }

    @Override
    public long lseek(FileDescriptor fileDescriptor, long j, int i) throws ErrnoException {
        return this.os.lseek(fileDescriptor, j, i);
    }

    @Override
    public StructStat lstat(String str) throws ErrnoException {
        return this.os.lstat(str);
    }

    @Override
    public void mincore(long j, long j2, byte[] bArr) throws ErrnoException {
        this.os.mincore(j, j2, bArr);
    }

    @Override
    public void mkdir(String str, int i) throws ErrnoException {
        this.os.mkdir(str, i);
    }

    @Override
    public void mkfifo(String str, int i) throws ErrnoException {
        this.os.mkfifo(str, i);
    }

    @Override
    public void mlock(long j, long j2) throws ErrnoException {
        this.os.mlock(j, j2);
    }

    @Override
    public long mmap(long j, long j2, int i, int i2, FileDescriptor fileDescriptor, long j3) throws ErrnoException {
        return this.os.mmap(j, j2, i, i2, fileDescriptor, j3);
    }

    @Override
    public void msync(long j, long j2, int i) throws ErrnoException {
        this.os.msync(j, j2, i);
    }

    @Override
    public void munlock(long j, long j2) throws ErrnoException {
        this.os.munlock(j, j2);
    }

    @Override
    public void munmap(long j, long j2) throws ErrnoException {
        this.os.munmap(j, j2);
    }

    @Override
    public FileDescriptor open(String str, int i, int i2) throws ErrnoException {
        return this.os.open(str, i, i2);
    }

    @Override
    public FileDescriptor[] pipe2(int i) throws ErrnoException {
        return this.os.pipe2(i);
    }

    @Override
    public int poll(StructPollfd[] structPollfdArr, int i) throws ErrnoException {
        return this.os.poll(structPollfdArr, i);
    }

    @Override
    public void posix_fallocate(FileDescriptor fileDescriptor, long j, long j2) throws ErrnoException {
        this.os.posix_fallocate(fileDescriptor, j, j2);
    }

    @Override
    public int prctl(int i, long j, long j2, long j3, long j4) throws ErrnoException {
        return this.os.prctl(i, j, j2, j3, j4);
    }

    @Override
    public int pread(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j) throws ErrnoException, InterruptedIOException {
        return this.os.pread(fileDescriptor, byteBuffer, j);
    }

    @Override
    public int pread(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, InterruptedIOException {
        return this.os.pread(fileDescriptor, bArr, i, i2, j);
    }

    @Override
    public int pwrite(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j) throws ErrnoException, InterruptedIOException {
        return this.os.pwrite(fileDescriptor, byteBuffer, j);
    }

    @Override
    public int pwrite(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, InterruptedIOException {
        return this.os.pwrite(fileDescriptor, bArr, i, i2, j);
    }

    @Override
    public int read(FileDescriptor fileDescriptor, ByteBuffer byteBuffer) throws ErrnoException, InterruptedIOException {
        return this.os.read(fileDescriptor, byteBuffer);
    }

    @Override
    public int read(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws ErrnoException, InterruptedIOException {
        return this.os.read(fileDescriptor, bArr, i, i2);
    }

    @Override
    public String readlink(String str) throws ErrnoException {
        return this.os.readlink(str);
    }

    @Override
    public String realpath(String str) throws ErrnoException {
        return this.os.realpath(str);
    }

    @Override
    public int readv(FileDescriptor fileDescriptor, Object[] objArr, int[] iArr, int[] iArr2) throws ErrnoException, InterruptedIOException {
        return this.os.readv(fileDescriptor, objArr, iArr, iArr2);
    }

    @Override
    public int recvfrom(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException {
        return this.os.recvfrom(fileDescriptor, byteBuffer, i, inetSocketAddress);
    }

    @Override
    public int recvfrom(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, InetSocketAddress inetSocketAddress) throws SocketException, ErrnoException {
        return this.os.recvfrom(fileDescriptor, bArr, i, i2, i3, inetSocketAddress);
    }

    @Override
    public void remove(String str) throws ErrnoException {
        this.os.remove(str);
    }

    @Override
    public void removexattr(String str, String str2) throws ErrnoException {
        this.os.removexattr(str, str2);
    }

    @Override
    public void rename(String str, String str2) throws ErrnoException {
        this.os.rename(str, str2);
    }

    @Override
    public long sendfile(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, Int64Ref int64Ref, long j) throws ErrnoException {
        return this.os.sendfile(fileDescriptor, fileDescriptor2, int64Ref, j);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, InetAddress inetAddress, int i2) throws SocketException, ErrnoException {
        return this.os.sendto(fileDescriptor, byteBuffer, i, inetAddress, i2);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, InetAddress inetAddress, int i4) throws SocketException, ErrnoException {
        return this.os.sendto(fileDescriptor, bArr, i, i2, i3, inetAddress, i4);
    }

    @Override
    public int sendto(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, SocketAddress socketAddress) throws SocketException, ErrnoException {
        return this.os.sendto(fileDescriptor, bArr, i, i2, i3, socketAddress);
    }

    @Override
    public void setegid(int i) throws ErrnoException {
        this.os.setegid(i);
    }

    @Override
    public void setenv(String str, String str2, boolean z) throws ErrnoException {
        this.os.setenv(str, str2, z);
    }

    @Override
    public void seteuid(int i) throws ErrnoException {
        this.os.seteuid(i);
    }

    @Override
    public void setgid(int i) throws ErrnoException {
        this.os.setgid(i);
    }

    @Override
    public void setpgid(int i, int i2) throws ErrnoException {
        this.os.setpgid(i, i2);
    }

    @Override
    public void setregid(int i, int i2) throws ErrnoException {
        this.os.setregid(i, i2);
    }

    @Override
    public void setreuid(int i, int i2) throws ErrnoException {
        this.os.setreuid(i, i2);
    }

    @Override
    public int setsid() throws ErrnoException {
        return this.os.setsid();
    }

    @Override
    public void setsockoptByte(FileDescriptor fileDescriptor, int i, int i2, int i3) throws ErrnoException {
        this.os.setsockoptByte(fileDescriptor, i, i2, i3);
    }

    @Override
    public void setsockoptIfreq(FileDescriptor fileDescriptor, int i, int i2, String str) throws ErrnoException {
        this.os.setsockoptIfreq(fileDescriptor, i, i2, str);
    }

    @Override
    public void setsockoptInt(FileDescriptor fileDescriptor, int i, int i2, int i3) throws ErrnoException {
        this.os.setsockoptInt(fileDescriptor, i, i2, i3);
    }

    @Override
    public void setsockoptIpMreqn(FileDescriptor fileDescriptor, int i, int i2, int i3) throws ErrnoException {
        this.os.setsockoptIpMreqn(fileDescriptor, i, i2, i3);
    }

    @Override
    public void setsockoptGroupReq(FileDescriptor fileDescriptor, int i, int i2, StructGroupReq structGroupReq) throws ErrnoException {
        this.os.setsockoptGroupReq(fileDescriptor, i, i2, structGroupReq);
    }

    @Override
    public void setsockoptLinger(FileDescriptor fileDescriptor, int i, int i2, StructLinger structLinger) throws ErrnoException {
        this.os.setsockoptLinger(fileDescriptor, i, i2, structLinger);
    }

    @Override
    public void setsockoptTimeval(FileDescriptor fileDescriptor, int i, int i2, StructTimeval structTimeval) throws ErrnoException {
        this.os.setsockoptTimeval(fileDescriptor, i, i2, structTimeval);
    }

    @Override
    public void setuid(int i) throws ErrnoException {
        this.os.setuid(i);
    }

    @Override
    public void setxattr(String str, String str2, byte[] bArr, int i) throws ErrnoException {
        this.os.setxattr(str, str2, bArr, i);
    }

    @Override
    public void shutdown(FileDescriptor fileDescriptor, int i) throws ErrnoException {
        this.os.shutdown(fileDescriptor, i);
    }

    @Override
    public FileDescriptor socket(int i, int i2, int i3) throws ErrnoException {
        return this.os.socket(i, i2, i3);
    }

    @Override
    public void socketpair(int i, int i2, int i3, FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2) throws ErrnoException {
        this.os.socketpair(i, i2, i3, fileDescriptor, fileDescriptor2);
    }

    @Override
    public long splice(FileDescriptor fileDescriptor, Int64Ref int64Ref, FileDescriptor fileDescriptor2, Int64Ref int64Ref2, long j, int i) throws ErrnoException {
        return this.os.splice(fileDescriptor, int64Ref, fileDescriptor2, int64Ref2, j, i);
    }

    @Override
    public StructStat stat(String str) throws ErrnoException {
        return this.os.stat(str);
    }

    @Override
    public StructStatVfs statvfs(String str) throws ErrnoException {
        return this.os.statvfs(str);
    }

    @Override
    public String strerror(int i) {
        return this.os.strerror(i);
    }

    @Override
    public String strsignal(int i) {
        return this.os.strsignal(i);
    }

    @Override
    public void symlink(String str, String str2) throws ErrnoException {
        this.os.symlink(str, str2);
    }

    @Override
    public long sysconf(int i) {
        return this.os.sysconf(i);
    }

    @Override
    public void tcdrain(FileDescriptor fileDescriptor) throws ErrnoException {
        this.os.tcdrain(fileDescriptor);
    }

    @Override
    public void tcsendbreak(FileDescriptor fileDescriptor, int i) throws ErrnoException {
        this.os.tcsendbreak(fileDescriptor, i);
    }

    @Override
    public int umask(int i) {
        return this.os.umask(i);
    }

    @Override
    public StructUtsname uname() {
        return this.os.uname();
    }

    @Override
    public void unlink(String str) throws ErrnoException {
        this.os.unlink(str);
    }

    @Override
    public void unsetenv(String str) throws ErrnoException {
        this.os.unsetenv(str);
    }

    @Override
    public int waitpid(int i, Int32Ref int32Ref, int i2) throws ErrnoException {
        return this.os.waitpid(i, int32Ref, i2);
    }

    @Override
    public int write(FileDescriptor fileDescriptor, ByteBuffer byteBuffer) throws ErrnoException, InterruptedIOException {
        return this.os.write(fileDescriptor, byteBuffer);
    }

    @Override
    public int write(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws ErrnoException, InterruptedIOException {
        return this.os.write(fileDescriptor, bArr, i, i2);
    }

    @Override
    public int writev(FileDescriptor fileDescriptor, Object[] objArr, int[] iArr, int[] iArr2) throws ErrnoException, InterruptedIOException {
        return this.os.writev(fileDescriptor, objArr, iArr, iArr2);
    }
}
