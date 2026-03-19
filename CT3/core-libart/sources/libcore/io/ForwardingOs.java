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
    public FileDescriptor accept(FileDescriptor fd, SocketAddress peerAddress) throws SocketException, ErrnoException {
        return this.os.accept(fd, peerAddress);
    }

    @Override
    public boolean access(String path, int mode) throws ErrnoException {
        return this.os.access(path, mode);
    }

    @Override
    public InetAddress[] android_getaddrinfo(String node, StructAddrinfo hints, int netId) throws GaiException {
        return this.os.android_getaddrinfo(node, hints, netId);
    }

    @Override
    public void bind(FileDescriptor fd, InetAddress address, int port) throws SocketException, ErrnoException {
        this.os.bind(fd, address, port);
    }

    @Override
    public void bind(FileDescriptor fd, SocketAddress address) throws SocketException, ErrnoException {
        this.os.bind(fd, address);
    }

    @Override
    public void chmod(String path, int mode) throws ErrnoException {
        this.os.chmod(path, mode);
    }

    @Override
    public void chown(String path, int uid, int gid) throws ErrnoException {
        this.os.chown(path, uid, gid);
    }

    @Override
    public void close(FileDescriptor fd) throws ErrnoException {
        this.os.close(fd);
    }

    @Override
    public void connect(FileDescriptor fd, InetAddress address, int port) throws SocketException, ErrnoException {
        this.os.connect(fd, address, port);
    }

    @Override
    public void connect(FileDescriptor fd, SocketAddress address) throws SocketException, ErrnoException {
        this.os.connect(fd, address);
    }

    @Override
    public FileDescriptor dup(FileDescriptor oldFd) throws ErrnoException {
        return this.os.dup(oldFd);
    }

    @Override
    public FileDescriptor dup2(FileDescriptor oldFd, int newFd) throws ErrnoException {
        return this.os.dup2(oldFd, newFd);
    }

    @Override
    public String[] environ() {
        return this.os.environ();
    }

    @Override
    public void execv(String filename, String[] argv) throws ErrnoException {
        this.os.execv(filename, argv);
    }

    @Override
    public void execve(String filename, String[] argv, String[] envp) throws ErrnoException {
        this.os.execve(filename, argv, envp);
    }

    @Override
    public void fchmod(FileDescriptor fd, int mode) throws ErrnoException {
        this.os.fchmod(fd, mode);
    }

    @Override
    public void fchown(FileDescriptor fd, int uid, int gid) throws ErrnoException {
        this.os.fchown(fd, uid, gid);
    }

    @Override
    public int fcntlFlock(FileDescriptor fd, int cmd, StructFlock arg) throws ErrnoException, InterruptedIOException {
        return this.os.fcntlFlock(fd, cmd, arg);
    }

    @Override
    public int fcntlInt(FileDescriptor fd, int cmd, int arg) throws ErrnoException {
        return this.os.fcntlInt(fd, cmd, arg);
    }

    @Override
    public int fcntlVoid(FileDescriptor fd, int cmd) throws ErrnoException {
        return this.os.fcntlVoid(fd, cmd);
    }

    @Override
    public void fdatasync(FileDescriptor fd) throws ErrnoException {
        this.os.fdatasync(fd);
    }

    @Override
    public StructStat fstat(FileDescriptor fd) throws ErrnoException {
        return this.os.fstat(fd);
    }

    @Override
    public StructStatVfs fstatvfs(FileDescriptor fd) throws ErrnoException {
        return this.os.fstatvfs(fd);
    }

    @Override
    public void fsync(FileDescriptor fd) throws ErrnoException {
        this.os.fsync(fd);
    }

    @Override
    public void ftruncate(FileDescriptor fd, long length) throws ErrnoException {
        this.os.ftruncate(fd, length);
    }

    @Override
    public String gai_strerror(int error) {
        return this.os.gai_strerror(error);
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
    public String getenv(String name) {
        return this.os.getenv(name);
    }

    @Override
    public String getnameinfo(InetAddress address, int flags) throws GaiException {
        return this.os.getnameinfo(address, flags);
    }

    @Override
    public SocketAddress getpeername(FileDescriptor fd) throws ErrnoException {
        return this.os.getpeername(fd);
    }

    @Override
    public int getpgid(int pid) throws ErrnoException {
        return this.os.getpgid(pid);
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
    public StructPasswd getpwnam(String name) throws ErrnoException {
        return this.os.getpwnam(name);
    }

    @Override
    public StructPasswd getpwuid(int uid) throws ErrnoException {
        return this.os.getpwuid(uid);
    }

    @Override
    public SocketAddress getsockname(FileDescriptor fd) throws ErrnoException {
        return this.os.getsockname(fd);
    }

    @Override
    public int getsockoptByte(FileDescriptor fd, int level, int option) throws ErrnoException {
        return this.os.getsockoptByte(fd, level, option);
    }

    @Override
    public InetAddress getsockoptInAddr(FileDescriptor fd, int level, int option) throws ErrnoException {
        return this.os.getsockoptInAddr(fd, level, option);
    }

    @Override
    public int getsockoptInt(FileDescriptor fd, int level, int option) throws ErrnoException {
        return this.os.getsockoptInt(fd, level, option);
    }

    @Override
    public StructLinger getsockoptLinger(FileDescriptor fd, int level, int option) throws ErrnoException {
        return this.os.getsockoptLinger(fd, level, option);
    }

    @Override
    public StructTimeval getsockoptTimeval(FileDescriptor fd, int level, int option) throws ErrnoException {
        return this.os.getsockoptTimeval(fd, level, option);
    }

    @Override
    public StructUcred getsockoptUcred(FileDescriptor fd, int level, int option) throws ErrnoException {
        return this.os.getsockoptUcred(fd, level, option);
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
    public int getxattr(String path, String name, byte[] outValue) throws ErrnoException {
        return this.os.getxattr(path, name, outValue);
    }

    @Override
    public String if_indextoname(int index) {
        return this.os.if_indextoname(index);
    }

    @Override
    public InetAddress inet_pton(int family, String address) {
        return this.os.inet_pton(family, address);
    }

    @Override
    public InetAddress ioctlInetAddress(FileDescriptor fd, int cmd, String interfaceName) throws ErrnoException {
        return this.os.ioctlInetAddress(fd, cmd, interfaceName);
    }

    @Override
    public int ioctlInt(FileDescriptor fd, int cmd, MutableInt arg) throws ErrnoException {
        return this.os.ioctlInt(fd, cmd, arg);
    }

    @Override
    public boolean isatty(FileDescriptor fd) {
        return this.os.isatty(fd);
    }

    @Override
    public void kill(int pid, int signal) throws ErrnoException {
        this.os.kill(pid, signal);
    }

    @Override
    public void lchown(String path, int uid, int gid) throws ErrnoException {
        this.os.lchown(path, uid, gid);
    }

    @Override
    public void link(String oldPath, String newPath) throws ErrnoException {
        this.os.link(oldPath, newPath);
    }

    @Override
    public void listen(FileDescriptor fd, int backlog) throws ErrnoException {
        this.os.listen(fd, backlog);
    }

    @Override
    public long lseek(FileDescriptor fd, long offset, int whence) throws ErrnoException {
        return this.os.lseek(fd, offset, whence);
    }

    @Override
    public StructStat lstat(String path) throws ErrnoException {
        return this.os.lstat(path);
    }

    @Override
    public void mincore(long address, long byteCount, byte[] vector) throws ErrnoException {
        this.os.mincore(address, byteCount, vector);
    }

    @Override
    public void mkdir(String path, int mode) throws ErrnoException {
        this.os.mkdir(path, mode);
    }

    @Override
    public void mkfifo(String path, int mode) throws ErrnoException {
        this.os.mkfifo(path, mode);
    }

    @Override
    public void mlock(long address, long byteCount) throws ErrnoException {
        this.os.mlock(address, byteCount);
    }

    @Override
    public long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException {
        return this.os.mmap(address, byteCount, prot, flags, fd, offset);
    }

    @Override
    public void msync(long address, long byteCount, int flags) throws ErrnoException {
        this.os.msync(address, byteCount, flags);
    }

    @Override
    public void munlock(long address, long byteCount) throws ErrnoException {
        this.os.munlock(address, byteCount);
    }

    @Override
    public void munmap(long address, long byteCount) throws ErrnoException {
        this.os.munmap(address, byteCount);
    }

    @Override
    public FileDescriptor open(String path, int flags, int mode) throws ErrnoException {
        return this.os.open(path, flags, mode);
    }

    @Override
    public FileDescriptor[] pipe2(int flags) throws ErrnoException {
        return this.os.pipe2(flags);
    }

    @Override
    public int poll(StructPollfd[] fds, int timeoutMs) throws ErrnoException {
        return this.os.poll(fds, timeoutMs);
    }

    @Override
    public void posix_fallocate(FileDescriptor fd, long offset, long length) throws ErrnoException {
        this.os.posix_fallocate(fd, offset, length);
    }

    @Override
    public int prctl(int option, long arg2, long arg3, long arg4, long arg5) throws ErrnoException {
        return this.os.prctl(option, arg2, arg3, arg4, arg5);
    }

    @Override
    public int pread(FileDescriptor fd, ByteBuffer buffer, long offset) throws ErrnoException, InterruptedIOException {
        return this.os.pread(fd, buffer, offset);
    }

    @Override
    public int pread(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, long offset) throws ErrnoException, InterruptedIOException {
        return this.os.pread(fd, bytes, byteOffset, byteCount, offset);
    }

    @Override
    public int pwrite(FileDescriptor fd, ByteBuffer buffer, long offset) throws ErrnoException, InterruptedIOException {
        return this.os.pwrite(fd, buffer, offset);
    }

    @Override
    public int pwrite(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, long offset) throws ErrnoException, InterruptedIOException {
        return this.os.pwrite(fd, bytes, byteOffset, byteCount, offset);
    }

    @Override
    public int read(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException, InterruptedIOException {
        return this.os.read(fd, buffer);
    }

    @Override
    public int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException, InterruptedIOException {
        return this.os.read(fd, bytes, byteOffset, byteCount);
    }

    @Override
    public String readlink(String path) throws ErrnoException {
        return this.os.readlink(path);
    }

    @Override
    public String realpath(String path) throws ErrnoException {
        return this.os.realpath(path);
    }

    @Override
    public int readv(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts) throws ErrnoException, InterruptedIOException {
        return this.os.readv(fd, buffers, offsets, byteCounts);
    }

    @Override
    public int recvfrom(FileDescriptor fd, ByteBuffer buffer, int flags, InetSocketAddress srcAddress) throws SocketException, ErrnoException {
        return this.os.recvfrom(fd, buffer, flags, srcAddress);
    }

    @Override
    public int recvfrom(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, InetSocketAddress srcAddress) throws SocketException, ErrnoException {
        return this.os.recvfrom(fd, bytes, byteOffset, byteCount, flags, srcAddress);
    }

    @Override
    public void remove(String path) throws ErrnoException {
        this.os.remove(path);
    }

    @Override
    public void removexattr(String path, String name) throws ErrnoException {
        this.os.removexattr(path, name);
    }

    @Override
    public void rename(String oldPath, String newPath) throws ErrnoException {
        this.os.rename(oldPath, newPath);
    }

    @Override
    public long sendfile(FileDescriptor outFd, FileDescriptor inFd, MutableLong inOffset, long byteCount) throws ErrnoException {
        return this.os.sendfile(outFd, inFd, inOffset, byteCount);
    }

    @Override
    public int sendto(FileDescriptor fd, ByteBuffer buffer, int flags, InetAddress inetAddress, int port) throws SocketException, ErrnoException {
        return this.os.sendto(fd, buffer, flags, inetAddress, port);
    }

    @Override
    public int sendto(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, InetAddress inetAddress, int port) throws SocketException, ErrnoException {
        return this.os.sendto(fd, bytes, byteOffset, byteCount, flags, inetAddress, port);
    }

    @Override
    public int sendto(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, SocketAddress address) throws SocketException, ErrnoException {
        return this.os.sendto(fd, bytes, byteOffset, byteCount, flags, address);
    }

    @Override
    public void setegid(int egid) throws ErrnoException {
        this.os.setegid(egid);
    }

    @Override
    public void setenv(String name, String value, boolean overwrite) throws ErrnoException {
        this.os.setenv(name, value, overwrite);
    }

    @Override
    public void seteuid(int euid) throws ErrnoException {
        this.os.seteuid(euid);
    }

    @Override
    public void setgid(int gid) throws ErrnoException {
        this.os.setgid(gid);
    }

    @Override
    public void setpgid(int pid, int pgid) throws ErrnoException {
        this.os.setpgid(pid, pgid);
    }

    @Override
    public void setregid(int rgid, int egid) throws ErrnoException {
        this.os.setregid(rgid, egid);
    }

    @Override
    public void setreuid(int ruid, int euid) throws ErrnoException {
        this.os.setreuid(ruid, euid);
    }

    @Override
    public int setsid() throws ErrnoException {
        return this.os.setsid();
    }

    @Override
    public void setsockoptByte(FileDescriptor fd, int level, int option, int value) throws ErrnoException {
        this.os.setsockoptByte(fd, level, option, value);
    }

    @Override
    public void setsockoptIfreq(FileDescriptor fd, int level, int option, String value) throws ErrnoException {
        this.os.setsockoptIfreq(fd, level, option, value);
    }

    @Override
    public void setsockoptInt(FileDescriptor fd, int level, int option, int value) throws ErrnoException {
        this.os.setsockoptInt(fd, level, option, value);
    }

    @Override
    public void setsockoptIpMreqn(FileDescriptor fd, int level, int option, int value) throws ErrnoException {
        this.os.setsockoptIpMreqn(fd, level, option, value);
    }

    @Override
    public void setsockoptGroupReq(FileDescriptor fd, int level, int option, StructGroupReq value) throws ErrnoException {
        this.os.setsockoptGroupReq(fd, level, option, value);
    }

    @Override
    public void setsockoptGroupSourceReq(FileDescriptor fd, int level, int option, StructGroupSourceReq value) throws ErrnoException {
        this.os.setsockoptGroupSourceReq(fd, level, option, value);
    }

    @Override
    public void setsockoptLinger(FileDescriptor fd, int level, int option, StructLinger value) throws ErrnoException {
        this.os.setsockoptLinger(fd, level, option, value);
    }

    @Override
    public void setsockoptTimeval(FileDescriptor fd, int level, int option, StructTimeval value) throws ErrnoException {
        this.os.setsockoptTimeval(fd, level, option, value);
    }

    @Override
    public void setuid(int uid) throws ErrnoException {
        this.os.setuid(uid);
    }

    @Override
    public void setxattr(String path, String name, byte[] value, int flags) throws ErrnoException {
        this.os.setxattr(path, name, value, flags);
    }

    @Override
    public void shutdown(FileDescriptor fd, int how) throws ErrnoException {
        this.os.shutdown(fd, how);
    }

    @Override
    public FileDescriptor socket(int domain, int type, int protocol) throws ErrnoException {
        return this.os.socket(domain, type, protocol);
    }

    @Override
    public void socketpair(int domain, int type, int protocol, FileDescriptor fd1, FileDescriptor fd2) throws ErrnoException {
        this.os.socketpair(domain, type, protocol, fd1, fd2);
    }

    @Override
    public StructStat stat(String path) throws ErrnoException {
        return this.os.stat(path);
    }

    @Override
    public StructStatVfs statvfs(String path) throws ErrnoException {
        return this.os.statvfs(path);
    }

    @Override
    public String strerror(int errno) {
        return this.os.strerror(errno);
    }

    @Override
    public String strsignal(int signal) {
        return this.os.strsignal(signal);
    }

    @Override
    public void symlink(String oldPath, String newPath) throws ErrnoException {
        this.os.symlink(oldPath, newPath);
    }

    @Override
    public long sysconf(int name) {
        return this.os.sysconf(name);
    }

    @Override
    public void tcdrain(FileDescriptor fd) throws ErrnoException {
        this.os.tcdrain(fd);
    }

    @Override
    public void tcsendbreak(FileDescriptor fd, int duration) throws ErrnoException {
        this.os.tcsendbreak(fd, duration);
    }

    @Override
    public int umask(int mask) {
        return this.os.umask(mask);
    }

    @Override
    public StructUtsname uname() {
        return this.os.uname();
    }

    @Override
    public void unlink(String pathname) throws ErrnoException {
        this.os.unlink(pathname);
    }

    @Override
    public void unsetenv(String name) throws ErrnoException {
        this.os.unsetenv(name);
    }

    @Override
    public int waitpid(int pid, MutableInt status, int options) throws ErrnoException {
        return this.os.waitpid(pid, status, options);
    }

    @Override
    public int write(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException, InterruptedIOException {
        return this.os.write(fd, buffer);
    }

    @Override
    public int write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException, InterruptedIOException {
        return this.os.write(fd, bytes, byteOffset, byteCount);
    }

    @Override
    public int writev(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts) throws ErrnoException, InterruptedIOException {
        return this.os.writev(fd, buffers, offsets, byteCounts);
    }
}
