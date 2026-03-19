package libcore.io;

import android.icu.impl.coll.CollationFastLatin;
import android.icu.lang.UProperty;
import android.icu.text.DateFormat;
import android.icu.text.PluralRules;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructGroupReq;
import android.system.StructGroupSourceReq;
import android.system.StructLinger;
import android.system.StructPollfd;
import android.system.StructTimeval;
import android.util.MutableInt;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final class IoBridge {
    public static final int JAVA_IP_MULTICAST_TTL = 17;
    public static final int JAVA_MCAST_BLOCK_SOURCE = 23;
    public static final int JAVA_MCAST_JOIN_GROUP = 19;
    public static final int JAVA_MCAST_JOIN_SOURCE_GROUP = 21;
    public static final int JAVA_MCAST_LEAVE_GROUP = 20;
    public static final int JAVA_MCAST_LEAVE_SOURCE_GROUP = 22;
    public static final int JAVA_MCAST_UNBLOCK_SOURCE = 24;

    private IoBridge() {
    }

    public static int available(FileDescriptor fd) throws IOException {
        try {
            MutableInt available = new MutableInt(0);
            Libcore.os.ioctlInt(fd, OsConstants.FIONREAD, available);
            if (available.value < 0) {
                available.value = 0;
            }
            return available.value;
        } catch (ErrnoException errnoException) {
            if (errnoException.errno == OsConstants.ENOTTY) {
                return 0;
            }
            throw errnoException.rethrowAsIOException();
        }
    }

    public static void bind(FileDescriptor fd, InetAddress address, int port) throws SocketException {
        if (address instanceof Inet6Address) {
            Inet6Address inet6Address = (Inet6Address) address;
            if (inet6Address.getScopeId() == 0 && inet6Address.isLinkLocalAddress()) {
                NetworkInterface nif = NetworkInterface.getByInetAddress(address);
                if (nif == null) {
                    throw new SocketException("Can't bind to a link-local address without a scope id: " + address);
                }
                try {
                    address = Inet6Address.getByAddress(address.getHostName(), address.getAddress(), nif.getIndex());
                } catch (UnknownHostException ex) {
                    throw new AssertionError(ex);
                }
            }
        }
        try {
            Libcore.os.bind(fd, address, port);
        } catch (ErrnoException errnoException) {
            throw new BindException(errnoException.getMessage(), errnoException);
        }
    }

    public static void connect(FileDescriptor fd, InetAddress inetAddress, int port) throws SocketException {
        try {
            connect(fd, inetAddress, port, 0);
        } catch (SocketTimeoutException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void connect(FileDescriptor fd, InetAddress inetAddress, int port, int timeoutMs) throws SocketException, SocketTimeoutException {
        try {
            connectErrno(fd, inetAddress, port, timeoutMs);
        } catch (ErrnoException errnoException) {
            throw new ConnectException(connectDetail(inetAddress, port, timeoutMs, errnoException), errnoException);
        } catch (SocketException ex) {
            throw ex;
        } catch (SocketTimeoutException ex2) {
            throw ex2;
        } catch (IOException ex3) {
            throw new SocketException(ex3);
        }
    }

    private static void connectErrno(FileDescriptor fd, InetAddress inetAddress, int port, int timeoutMs) throws IOException, ErrnoException {
        int remainingTimeoutMs;
        if (timeoutMs == 0) {
            Libcore.os.connect(fd, inetAddress, port);
            return;
        }
        IoUtils.setBlocking(fd, false);
        long finishTimeNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        try {
            Libcore.os.connect(fd, inetAddress, port);
            IoUtils.setBlocking(fd, true);
        } catch (ErrnoException errnoException) {
            if (errnoException.errno != OsConstants.EINPROGRESS) {
                throw errnoException;
            }
            do {
                remainingTimeoutMs = (int) TimeUnit.NANOSECONDS.toMillis(finishTimeNanos - System.nanoTime());
                if (remainingTimeoutMs <= 0) {
                    throw new SocketTimeoutException(connectDetail(inetAddress, port, timeoutMs, null));
                }
            } while (!isConnected(fd, inetAddress, port, timeoutMs, remainingTimeoutMs));
            IoUtils.setBlocking(fd, true);
        }
    }

    private static String connectDetail(InetAddress inetAddress, int port, int timeoutMs, ErrnoException cause) {
        String detail = "failed to connect to " + inetAddress + " (port " + port + ")";
        if (timeoutMs > 0) {
            detail = detail + " after " + timeoutMs + DateFormat.MINUTE_SECOND;
        }
        if (cause != null) {
            return detail + PluralRules.KEYWORD_RULE_SEPARATOR + cause.getMessage();
        }
        return detail;
    }

    public static void closeAndSignalBlockedThreads(FileDescriptor fd) throws IOException {
        if (fd == null || !fd.valid()) {
            return;
        }
        int intFd = fd.getInt$();
        fd.setInt$(-1);
        FileDescriptor oldFd = new FileDescriptor();
        oldFd.setInt$(intFd);
        AsynchronousCloseMonitor.signalBlockedThreads(oldFd);
        try {
            Libcore.os.close(oldFd);
        } catch (ErrnoException e) {
        }
    }

    public static boolean isConnected(FileDescriptor fd, InetAddress inetAddress, int port, int timeoutMs, int remainingTimeoutMs) throws IOException {
        try {
            StructPollfd[] pollFds = {new StructPollfd()};
            pollFds[0].fd = fd;
            pollFds[0].events = (short) OsConstants.POLLOUT;
            int rc = Libcore.os.poll(pollFds, remainingTimeoutMs);
            if (rc == 0) {
                return false;
            }
            int connectError = Libcore.os.getsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_ERROR);
            if (connectError == 0) {
                return true;
            }
            throw new ErrnoException("isConnected", connectError);
        } catch (ErrnoException errnoException) {
            if (!fd.valid()) {
                throw new SocketException("Socket closed");
            }
            String detail = connectDetail(inetAddress, port, timeoutMs, errnoException);
            if (errnoException.errno == OsConstants.ETIMEDOUT) {
                throw new SocketTimeoutException(detail, errnoException);
            }
            throw new ConnectException(detail, errnoException);
        }
    }

    public static Object getSocketOption(FileDescriptor fd, int option) throws SocketException {
        try {
            return getSocketOptionErrno(fd, option);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    private static Object getSocketOptionErrno(FileDescriptor fd, int option) throws SocketException, ErrnoException {
        switch (option) {
            case 1:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fd, OsConstants.IPPROTO_TCP, OsConstants.TCP_NODELAY)));
            case 3:
                return Integer.valueOf(Libcore.os.getsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_TCLASS));
            case 4:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR)));
            case 8:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_KEEPALIVE)));
            case 16:
                return Libcore.os.getsockoptInAddr(fd, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_IF);
            case 17:
                return Integer.valueOf(Libcore.os.getsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_HOPS));
            case 18:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_LOOP)));
            case 31:
                return Integer.valueOf(Libcore.os.getsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_IF));
            case 32:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST)));
            case 128:
                StructLinger linger = Libcore.os.getsockoptLinger(fd, OsConstants.SOL_SOCKET, OsConstants.SO_LINGER);
                if (!linger.isOn()) {
                    return false;
                }
                return Integer.valueOf(linger.l_linger);
            case 4097:
                return Integer.valueOf(Libcore.os.getsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_SNDBUF));
            case 4098:
                return Integer.valueOf(Libcore.os.getsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF));
            case 4099:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_OOBINLINE)));
            case UProperty.JOINING_GROUP:
                return Integer.valueOf((int) Libcore.os.getsockoptTimeval(fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO).toMillis());
            default:
                throw new SocketException("Unknown socket option: " + option);
        }
    }

    private static boolean booleanFromInt(int i) {
        return i != 0;
    }

    private static int booleanToInt(boolean b) {
        return b ? 1 : 0;
    }

    public static void setSocketOption(FileDescriptor fd, int option, Object value) throws SocketException {
        try {
            setSocketOptionErrno(fd, option, value);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    private static void setSocketOptionErrno(FileDescriptor fd, int option, Object value) throws SocketException, ErrnoException {
        switch (option) {
            case 1:
                Libcore.os.setsockoptInt(fd, OsConstants.IPPROTO_TCP, OsConstants.TCP_NODELAY, booleanToInt(((Boolean) value).booleanValue()));
                return;
            case 3:
                Libcore.os.setsockoptInt(fd, OsConstants.IPPROTO_IP, OsConstants.IP_TOS, ((Integer) value).intValue());
                Libcore.os.setsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_TCLASS, ((Integer) value).intValue());
                return;
            case 4:
                Libcore.os.setsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, booleanToInt(((Boolean) value).booleanValue()));
                return;
            case 8:
                Libcore.os.setsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_KEEPALIVE, booleanToInt(((Boolean) value).booleanValue()));
                return;
            case 16:
                throw new UnsupportedOperationException("Use IP_MULTICAST_IF2 on Android");
            case 17:
                Libcore.os.setsockoptByte(fd, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_TTL, ((Integer) value).intValue());
                Libcore.os.setsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_HOPS, ((Integer) value).intValue());
                return;
            case 18:
                Libcore.os.setsockoptByte(fd, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_LOOP, booleanToInt(((Boolean) value).booleanValue()));
                Libcore.os.setsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_LOOP, booleanToInt(((Boolean) value).booleanValue()));
                return;
            case 19:
            case 20:
                StructGroupReq groupReq = (StructGroupReq) value;
                int level = groupReq.gr_group instanceof Inet4Address ? OsConstants.IPPROTO_IP : OsConstants.IPPROTO_IPV6;
                int op = option == 19 ? OsConstants.MCAST_JOIN_GROUP : OsConstants.MCAST_LEAVE_GROUP;
                Libcore.os.setsockoptGroupReq(fd, level, op, groupReq);
                return;
            case 21:
            case 22:
            case 23:
            case 24:
                StructGroupSourceReq groupSourceReq = (StructGroupSourceReq) value;
                int level2 = groupSourceReq.gsr_group instanceof Inet4Address ? OsConstants.IPPROTO_IP : OsConstants.IPPROTO_IPV6;
                int op2 = getGroupSourceReqOp(option);
                Libcore.os.setsockoptGroupSourceReq(fd, level2, op2, groupSourceReq);
                return;
            case 31:
                Libcore.os.setsockoptIpMreqn(fd, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_IF, ((Integer) value).intValue());
                Libcore.os.setsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_IF, ((Integer) value).intValue());
                return;
            case 32:
                Libcore.os.setsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, booleanToInt(((Boolean) value).booleanValue()));
                return;
            case 128:
                boolean on = false;
                int seconds = 0;
                if (value instanceof Integer) {
                    on = true;
                    seconds = Math.min(((Integer) value).intValue(), 65535);
                }
                StructLinger linger = new StructLinger(booleanToInt(on), seconds);
                Libcore.os.setsockoptLinger(fd, OsConstants.SOL_SOCKET, OsConstants.SO_LINGER, linger);
                return;
            case 4097:
                Libcore.os.setsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_SNDBUF, ((Integer) value).intValue());
                return;
            case 4098:
                Libcore.os.setsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, ((Integer) value).intValue());
                return;
            case 4099:
                Libcore.os.setsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_OOBINLINE, booleanToInt(((Boolean) value).booleanValue()));
                return;
            case UProperty.JOINING_GROUP:
                int millis = ((Integer) value).intValue();
                StructTimeval tv = StructTimeval.fromMillis(millis);
                Libcore.os.setsockoptTimeval(fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, tv);
                return;
            default:
                throw new SocketException("Unknown socket option: " + option);
        }
    }

    private static int getGroupSourceReqOp(int javaValue) {
        switch (javaValue) {
            case 21:
                return OsConstants.MCAST_JOIN_SOURCE_GROUP;
            case 22:
                return OsConstants.MCAST_LEAVE_SOURCE_GROUP;
            case 23:
                return OsConstants.MCAST_BLOCK_SOURCE;
            case 24:
                return OsConstants.MCAST_UNBLOCK_SOURCE;
            default:
                throw new AssertionError("Unknown java value for setsocketopt op lookup: " + javaValue);
        }
    }

    public static FileDescriptor open(String path, int flags) throws FileNotFoundException {
        FileDescriptor fd = null;
        try {
            int mode = (OsConstants.O_ACCMODE & flags) == OsConstants.O_RDONLY ? 0 : CollationFastLatin.LATIN_LIMIT;
            fd = Libcore.os.open(path, flags, mode);
            if (OsConstants.S_ISDIR(Libcore.os.fstat(fd).st_mode)) {
                throw new ErrnoException("open", OsConstants.EISDIR);
            }
            return fd;
        } catch (ErrnoException errnoException) {
            if (fd != null) {
                try {
                    IoUtils.close(fd);
                } catch (IOException e) {
                }
            }
            FileNotFoundException ex = new FileNotFoundException(path + PluralRules.KEYWORD_RULE_SEPARATOR + errnoException.getMessage());
            ex.initCause(errnoException);
            throw ex;
        }
    }

    public static int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(bytes.length, byteOffset, byteCount);
        if (byteCount == 0) {
            return 0;
        }
        try {
            int readCount = Libcore.os.read(fd, bytes, byteOffset, byteCount);
            if (readCount == 0) {
                return -1;
            }
            return readCount;
        } catch (ErrnoException errnoException) {
            if (errnoException.errno == OsConstants.EAGAIN) {
                return 0;
            }
            throw errnoException.rethrowAsIOException();
        }
    }

    public static void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(bytes.length, byteOffset, byteCount);
        if (byteCount == 0) {
            return;
        }
        while (byteCount > 0) {
            try {
                int bytesWritten = Libcore.os.write(fd, bytes, byteOffset, byteCount);
                byteCount -= bytesWritten;
                byteOffset += bytesWritten;
            } catch (ErrnoException errnoException) {
                throw errnoException.rethrowAsIOException();
            }
        }
    }

    public static int sendto(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, InetAddress inetAddress, int port) throws IOException {
        boolean isDatagram = inetAddress != null;
        if (!isDatagram && byteCount <= 0) {
            return 0;
        }
        try {
            int result = Libcore.os.sendto(fd, bytes, byteOffset, byteCount, flags, inetAddress, port);
            return result;
        } catch (ErrnoException errnoException) {
            int result2 = maybeThrowAfterSendto(isDatagram, errnoException);
            return result2;
        }
    }

    public static int sendto(FileDescriptor fd, ByteBuffer buffer, int flags, InetAddress inetAddress, int port) throws IOException {
        boolean isDatagram = inetAddress != null;
        if (!isDatagram && buffer.remaining() == 0) {
            return 0;
        }
        try {
            int result = Libcore.os.sendto(fd, buffer, flags, inetAddress, port);
            return result;
        } catch (ErrnoException errnoException) {
            int result2 = maybeThrowAfterSendto(isDatagram, errnoException);
            return result2;
        }
    }

    private static int maybeThrowAfterSendto(boolean isDatagram, ErrnoException errnoException) throws SocketException {
        if (isDatagram) {
            if (errnoException.errno == OsConstants.ECONNRESET || errnoException.errno == OsConstants.ECONNREFUSED) {
                return 0;
            }
        } else if (errnoException.errno == OsConstants.EAGAIN) {
            return 0;
        }
        throw errnoException.rethrowAsSocketException();
    }

    public static int recvfrom(boolean isRead, FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, DatagramPacket packet, boolean isConnected) throws IOException {
        InetSocketAddress inetSocketAddress;
        if (packet == null || isConnected) {
            inetSocketAddress = null;
        } else {
            try {
                inetSocketAddress = new InetSocketAddress();
            } catch (ErrnoException errnoException) {
                int result = maybeThrowAfterRecvfrom(isRead, isConnected, errnoException);
                return result;
            }
        }
        int result2 = Libcore.os.recvfrom(fd, bytes, byteOffset, byteCount, flags, inetSocketAddress);
        return postRecvfrom(isRead, packet, isConnected, inetSocketAddress, result2);
    }

    public static int recvfrom(boolean isRead, FileDescriptor fd, ByteBuffer buffer, int flags, DatagramPacket packet, boolean isConnected) throws IOException {
        InetSocketAddress inetSocketAddress;
        if (packet == null || isConnected) {
            inetSocketAddress = null;
        } else {
            try {
                inetSocketAddress = new InetSocketAddress();
            } catch (ErrnoException errnoException) {
                int result = maybeThrowAfterRecvfrom(isRead, isConnected, errnoException);
                return result;
            }
        }
        int result2 = Libcore.os.recvfrom(fd, buffer, flags, inetSocketAddress);
        return postRecvfrom(isRead, packet, isConnected, inetSocketAddress, result2);
    }

    private static int postRecvfrom(boolean isRead, DatagramPacket packet, boolean isConnected, InetSocketAddress srcAddress, int byteCount) {
        if (isRead && byteCount == 0) {
            return -1;
        }
        if (packet != null) {
            packet.setReceivedLength(byteCount);
            if (!isConnected) {
                packet.setAddress(srcAddress.getAddress());
                packet.setPort(srcAddress.getPort());
            }
        }
        return byteCount;
    }

    private static int maybeThrowAfterRecvfrom(boolean isRead, boolean isConnected, ErrnoException errnoException) throws SocketException, SocketTimeoutException {
        if (isRead) {
            if (errnoException.errno == OsConstants.EAGAIN) {
                return 0;
            }
            throw errnoException.rethrowAsSocketException();
        }
        if (isConnected && errnoException.errno == OsConstants.ECONNREFUSED) {
            throw new PortUnreachableException("", errnoException);
        }
        if (errnoException.errno == OsConstants.EAGAIN) {
            throw new SocketTimeoutException(errnoException);
        }
        throw errnoException.rethrowAsSocketException();
    }

    public static FileDescriptor socket(boolean stream) throws SocketException {
        try {
            FileDescriptor fd = Libcore.os.socket(OsConstants.AF_INET6, stream ? OsConstants.SOCK_STREAM : OsConstants.SOCK_DGRAM, 0);
            if (!stream) {
                Libcore.os.setsockoptInt(fd, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_HOPS, 1);
            }
            return fd;
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    public static InetAddress getSocketLocalAddress(FileDescriptor fd) throws SocketException {
        try {
            SocketAddress sa = Libcore.os.getsockname(fd);
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getAddress();
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    public static int getSocketLocalPort(FileDescriptor fd) throws SocketException {
        try {
            SocketAddress sa = Libcore.os.getsockname(fd);
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getPort();
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }
}
