package libcore.io;

import android.icu.impl.coll.CollationFastLatin;
import android.icu.lang.UProperty;
import android.icu.text.DateFormat;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.PluralRules;
import android.system.ErrnoException;
import android.system.Int32Ref;
import android.system.OsConstants;
import android.system.StructGroupReq;
import android.system.StructLinger;
import android.system.StructPollfd;
import android.system.StructTimeval;
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
import java.net.NoRouteToHostException;
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
    public static final int JAVA_IP_TTL = 25;
    public static final int JAVA_MCAST_JOIN_GROUP = 19;
    public static final int JAVA_MCAST_LEAVE_GROUP = 20;

    private IoBridge() {
    }

    public static int available(FileDescriptor fileDescriptor) throws IOException {
        try {
            Int32Ref int32Ref = new Int32Ref(0);
            Libcore.os.ioctlInt(fileDescriptor, OsConstants.FIONREAD, int32Ref);
            if (int32Ref.value < 0) {
                int32Ref.value = 0;
            }
            return int32Ref.value;
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.ENOTTY) {
                return 0;
            }
            throw e.rethrowAsIOException();
        }
    }

    public static void bind(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException {
        if (inetAddress instanceof Inet6Address) {
            Inet6Address inet6Address = (Inet6Address) inetAddress;
            if (inet6Address.getScopeId() == 0 && inet6Address.isLinkLocalAddress()) {
                NetworkInterface byInetAddress = NetworkInterface.getByInetAddress(inetAddress);
                if (byInetAddress == null) {
                    throw new SocketException("Can't bind to a link-local address without a scope id: " + inetAddress);
                }
                try {
                    inetAddress = Inet6Address.getByAddress(inetAddress.getHostName(), inetAddress.getAddress(), byInetAddress.getIndex());
                } catch (UnknownHostException e) {
                    throw new AssertionError(e);
                }
            }
        }
        try {
            Libcore.os.bind(fileDescriptor, inetAddress, i);
        } catch (ErrnoException e2) {
            if (e2.errno == OsConstants.EADDRINUSE || e2.errno == OsConstants.EADDRNOTAVAIL || e2.errno == OsConstants.EPERM || e2.errno == OsConstants.EACCES) {
                throw new BindException(e2.getMessage(), e2);
            }
            throw new SocketException(e2.getMessage(), e2);
        }
    }

    public static void connect(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws SocketException {
        try {
            connect(fileDescriptor, inetAddress, i, 0);
        } catch (SocketTimeoutException e) {
            throw new AssertionError(e);
        }
    }

    public static void connect(FileDescriptor fileDescriptor, InetAddress inetAddress, int i, int i2) throws SocketException, SocketTimeoutException {
        try {
            connectErrno(fileDescriptor, inetAddress, i, i2);
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.EHOSTUNREACH) {
                throw new NoRouteToHostException("Host unreachable");
            }
            if (e.errno == OsConstants.EADDRNOTAVAIL) {
                throw new NoRouteToHostException("Address not available");
            }
            throw new ConnectException(createMessageForException(fileDescriptor, inetAddress, i, i2, e), e);
        } catch (SocketException e2) {
            throw e2;
        } catch (SocketTimeoutException e3) {
            throw e3;
        } catch (IOException e4) {
            throw new SocketException(e4);
        }
    }

    private static void connectErrno(FileDescriptor fileDescriptor, InetAddress inetAddress, int i, int i2) throws IOException, ErrnoException {
        int millis;
        if (i2 <= 0) {
            Libcore.os.connect(fileDescriptor, inetAddress, i);
            return;
        }
        IoUtils.setBlocking(fileDescriptor, false);
        long jNanoTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(i2);
        try {
            Libcore.os.connect(fileDescriptor, inetAddress, i);
            IoUtils.setBlocking(fileDescriptor, true);
        } catch (ErrnoException e) {
            if (e.errno != OsConstants.EINPROGRESS) {
                throw e;
            }
            do {
                millis = (int) TimeUnit.NANOSECONDS.toMillis(jNanoTime - System.nanoTime());
                if (millis <= 0) {
                    throw new SocketTimeoutException(createMessageForException(fileDescriptor, inetAddress, i, i2, null));
                }
            } while (!isConnected(fileDescriptor, inetAddress, i, i2, millis));
            IoUtils.setBlocking(fileDescriptor, true);
        }
    }

    private static String createMessageForException(FileDescriptor fileDescriptor, InetAddress inetAddress, int i, int i2, Exception exc) {
        InetSocketAddress localInetSocketAddress;
        try {
            localInetSocketAddress = getLocalInetSocketAddress(fileDescriptor);
        } catch (SocketException e) {
            localInetSocketAddress = null;
        }
        StringBuilder sb = new StringBuilder("failed to connect");
        sb.append(" to ");
        sb.append(inetAddress);
        sb.append(" (port ");
        sb.append(i);
        sb.append(")");
        if (localInetSocketAddress != null) {
            sb.append(" from ");
            sb.append(localInetSocketAddress.getAddress());
            sb.append(" (port ");
            sb.append(localInetSocketAddress.getPort());
            sb.append(")");
        }
        if (i2 > 0) {
            sb.append(" after ");
            sb.append(i2);
            sb.append(DateFormat.MINUTE_SECOND);
        }
        if (exc != null) {
            sb.append(PluralRules.KEYWORD_RULE_SEPARATOR);
            sb.append(exc.getMessage());
        }
        return sb.toString();
    }

    public static void closeAndSignalBlockedThreads(FileDescriptor fileDescriptor) throws IOException {
        if (fileDescriptor == null || !fileDescriptor.valid()) {
            return;
        }
        int int$ = fileDescriptor.getInt$();
        fileDescriptor.setInt$(-1);
        FileDescriptor fileDescriptor2 = new FileDescriptor();
        fileDescriptor2.setInt$(int$);
        AsynchronousCloseMonitor.signalBlockedThreads(fileDescriptor2);
        try {
            Libcore.os.close(fileDescriptor2);
        } catch (ErrnoException e) {
        }
    }

    public static boolean isConnected(FileDescriptor fileDescriptor, InetAddress inetAddress, int i, int i2, int i3) throws IOException {
        try {
            StructPollfd[] structPollfdArr = {new StructPollfd()};
            structPollfdArr[0].fd = fileDescriptor;
            structPollfdArr[0].events = (short) OsConstants.POLLOUT;
            if (Libcore.os.poll(structPollfdArr, i3) == 0) {
                return false;
            }
            int i4 = Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_ERROR);
            if (i4 == 0) {
                return true;
            }
            throw new ErrnoException("isConnected", i4);
        } catch (ErrnoException e) {
            if (!fileDescriptor.valid()) {
                throw new SocketException("Socket closed");
            }
            String strCreateMessageForException = createMessageForException(fileDescriptor, inetAddress, i, i2, e);
            if (e.errno == OsConstants.ETIMEDOUT) {
                throw new SocketTimeoutException(strCreateMessageForException, e);
            }
            throw new ConnectException(strCreateMessageForException, e);
        }
    }

    public static Object getSocketOption(FileDescriptor fileDescriptor, int i) throws SocketException {
        try {
            return getSocketOptionErrno(fileDescriptor, i);
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }

    private static Object getSocketOptionErrno(FileDescriptor fileDescriptor, int i) throws SocketException, ErrnoException {
        switch (i) {
            case 1:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.IPPROTO_TCP, OsConstants.TCP_NODELAY)));
            case 3:
                return Integer.valueOf(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_TCLASS));
            case 4:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR)));
            case 8:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_KEEPALIVE)));
            case 15:
                return ((InetSocketAddress) Libcore.os.getsockname(fileDescriptor)).getAddress();
            case 16:
            case 31:
                return Integer.valueOf(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_IF));
            case 17:
                return Integer.valueOf(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_HOPS));
            case 18:
                return Boolean.valueOf(!booleanFromInt(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_LOOP)));
            case 25:
                return Integer.valueOf(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_UNICAST_HOPS));
            case 32:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST)));
            case 128:
                StructLinger structLinger = Libcore.os.getsockoptLinger(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_LINGER);
                if (!structLinger.isOn()) {
                    return false;
                }
                return Integer.valueOf(structLinger.l_linger);
            case 4097:
                return Integer.valueOf(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDBUF));
            case 4098:
                return Integer.valueOf(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF));
            case 4099:
                return Boolean.valueOf(booleanFromInt(Libcore.os.getsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_OOBINLINE)));
            case UProperty.JOINING_GROUP:
                return Integer.valueOf((int) Libcore.os.getsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO).toMillis());
            default:
                throw new SocketException("Unknown socket option: " + i);
        }
    }

    private static boolean booleanFromInt(int i) {
        return i != 0;
    }

    private static int booleanToInt(boolean z) {
        return z ? 1 : 0;
    }

    public static void setSocketOption(FileDescriptor fileDescriptor, int i, Object obj) throws SocketException {
        try {
            setSocketOptionErrno(fileDescriptor, i, obj);
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }

    private static void setSocketOptionErrno(FileDescriptor fileDescriptor, int i, Object obj) throws SocketException, ErrnoException {
        boolean z = true;
        if (i == 1) {
            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_TCP, OsConstants.TCP_NODELAY, booleanToInt(((Boolean) obj).booleanValue()));
            return;
        }
        if (i == 8) {
            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_KEEPALIVE, booleanToInt(((Boolean) obj).booleanValue()));
            return;
        }
        if (i == 25) {
            Integer num = (Integer) obj;
            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IP, OsConstants.IP_TTL, num.intValue());
            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_UNICAST_HOPS, num.intValue());
            return;
        }
        if (i == 128) {
            int iMin = 0;
            if (obj instanceof Integer) {
                iMin = Math.min(((Integer) obj).intValue(), DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH);
            } else {
                z = false;
            }
            Libcore.os.setsockoptLinger(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_LINGER, new StructLinger(booleanToInt(z), iMin));
            return;
        }
        if (i != 4102) {
            switch (i) {
                case 3:
                    Integer num2 = (Integer) obj;
                    Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IP, OsConstants.IP_TOS, num2.intValue());
                    Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_TCLASS, num2.intValue());
                    return;
                case 4:
                    Libcore.os.setsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, booleanToInt(((Boolean) obj).booleanValue()));
                    return;
                default:
                    switch (i) {
                        case 16:
                            NetworkInterface byInetAddress = NetworkInterface.getByInetAddress((InetAddress) obj);
                            if (byInetAddress == null) {
                                throw new SocketException("bad argument for IP_MULTICAST_IF : address not bound to any interface");
                            }
                            Libcore.os.setsockoptIpMreqn(fileDescriptor, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_IF, byInetAddress.getIndex());
                            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_IF, byInetAddress.getIndex());
                            return;
                        case 17:
                            Integer num3 = (Integer) obj;
                            Libcore.os.setsockoptByte(fileDescriptor, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_TTL, num3.intValue());
                            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_HOPS, num3.intValue());
                            return;
                        case 18:
                            int iBooleanToInt = booleanToInt(!((Boolean) obj).booleanValue());
                            Libcore.os.setsockoptByte(fileDescriptor, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_LOOP, iBooleanToInt);
                            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_LOOP, iBooleanToInt);
                            return;
                        case 19:
                        case 20:
                            StructGroupReq structGroupReq = (StructGroupReq) obj;
                            Libcore.os.setsockoptGroupReq(fileDescriptor, structGroupReq.gr_group instanceof Inet4Address ? OsConstants.IPPROTO_IP : OsConstants.IPPROTO_IPV6, i == 19 ? OsConstants.MCAST_JOIN_GROUP : OsConstants.MCAST_LEAVE_GROUP, structGroupReq);
                            return;
                        default:
                            switch (i) {
                                case 31:
                                    Integer num4 = (Integer) obj;
                                    Libcore.os.setsockoptIpMreqn(fileDescriptor, OsConstants.IPPROTO_IP, OsConstants.IP_MULTICAST_IF, num4.intValue());
                                    Libcore.os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_IPV6, OsConstants.IPV6_MULTICAST_IF, num4.intValue());
                                    return;
                                case 32:
                                    Libcore.os.setsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, booleanToInt(((Boolean) obj).booleanValue()));
                                    return;
                                default:
                                    switch (i) {
                                        case 4097:
                                            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDBUF, ((Integer) obj).intValue());
                                            return;
                                        case 4098:
                                            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, ((Integer) obj).intValue());
                                            return;
                                        case 4099:
                                            Libcore.os.setsockoptInt(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_OOBINLINE, booleanToInt(((Boolean) obj).booleanValue()));
                                            return;
                                        default:
                                            throw new SocketException("Unknown socket option: " + i);
                                    }
                            }
                    }
            }
        }
        Libcore.os.setsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(((Integer) obj).intValue()));
    }

    public static FileDescriptor open(String str, int i) throws FileNotFoundException {
        ErrnoException e;
        FileDescriptor fileDescriptorOpen;
        try {
            fileDescriptorOpen = Libcore.os.open(str, i, (OsConstants.O_ACCMODE & i) == OsConstants.O_RDONLY ? 0 : CollationFastLatin.LATIN_LIMIT);
        } catch (ErrnoException e2) {
            e = e2;
            fileDescriptorOpen = null;
        }
        try {
            if (OsConstants.S_ISDIR(Libcore.os.fstat(fileDescriptorOpen).st_mode)) {
                throw new ErrnoException("open", OsConstants.EISDIR);
            }
            return fileDescriptorOpen;
        } catch (ErrnoException e3) {
            e = e3;
            if (fileDescriptorOpen != null) {
                try {
                    IoUtils.close(fileDescriptorOpen);
                } catch (IOException e4) {
                }
            }
            FileNotFoundException fileNotFoundException = new FileNotFoundException(str + PluralRules.KEYWORD_RULE_SEPARATOR + e.getMessage());
            fileNotFoundException.initCause(e);
            throw fileNotFoundException;
        }
    }

    public static int read(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws IOException {
        Arrays.checkOffsetAndCount(bArr.length, i, i2);
        if (i2 == 0) {
            return 0;
        }
        try {
            int i3 = Libcore.os.read(fileDescriptor, bArr, i, i2);
            if (i3 == 0) {
                return -1;
            }
            return i3;
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.EAGAIN) {
                return 0;
            }
            throw e.rethrowAsIOException();
        }
    }

    public static void write(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2) throws IOException {
        Arrays.checkOffsetAndCount(bArr.length, i, i2);
        if (i2 == 0) {
            return;
        }
        while (i2 > 0) {
            try {
                int iWrite = Libcore.os.write(fileDescriptor, bArr, i, i2);
                i2 -= iWrite;
                i += iWrite;
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        }
    }

    public static int sendto(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, InetAddress inetAddress, int i4) throws IOException {
        boolean z = inetAddress != null;
        if (!z && i2 <= 0) {
            return 0;
        }
        try {
            return Libcore.os.sendto(fileDescriptor, bArr, i, i2, i3, inetAddress, i4);
        } catch (ErrnoException e) {
            return maybeThrowAfterSendto(z, e);
        }
    }

    public static int sendto(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, InetAddress inetAddress, int i2) throws IOException {
        boolean z = inetAddress != null;
        if (!z && byteBuffer.remaining() == 0) {
            return 0;
        }
        try {
            return Libcore.os.sendto(fileDescriptor, byteBuffer, i, inetAddress, i2);
        } catch (ErrnoException e) {
            return maybeThrowAfterSendto(z, e);
        }
    }

    private static int maybeThrowAfterSendto(boolean z, ErrnoException errnoException) throws IOException {
        if (z) {
            if (errnoException.errno == OsConstants.ECONNREFUSED) {
                throw new PortUnreachableException("ICMP Port Unreachable");
            }
        } else if (errnoException.errno == OsConstants.EAGAIN) {
            return 0;
        }
        throw errnoException.rethrowAsIOException();
    }

    public static int recvfrom(boolean z, FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3, DatagramPacket datagramPacket, boolean z2) throws IOException {
        InetSocketAddress inetSocketAddress;
        if (datagramPacket == null) {
            inetSocketAddress = null;
        } else {
            try {
                inetSocketAddress = new InetSocketAddress();
            } catch (ErrnoException e) {
                return maybeThrowAfterRecvfrom(z, z2, e);
            }
        }
        return postRecvfrom(z, datagramPacket, inetSocketAddress, Libcore.os.recvfrom(fileDescriptor, bArr, i, i2, i3, inetSocketAddress));
    }

    public static int recvfrom(boolean z, FileDescriptor fileDescriptor, ByteBuffer byteBuffer, int i, DatagramPacket datagramPacket, boolean z2) throws IOException {
        InetSocketAddress inetSocketAddress;
        if (datagramPacket == null) {
            inetSocketAddress = null;
        } else {
            try {
                inetSocketAddress = new InetSocketAddress();
            } catch (ErrnoException e) {
                return maybeThrowAfterRecvfrom(z, z2, e);
            }
        }
        return postRecvfrom(z, datagramPacket, inetSocketAddress, Libcore.os.recvfrom(fileDescriptor, byteBuffer, i, inetSocketAddress));
    }

    private static int postRecvfrom(boolean z, DatagramPacket datagramPacket, InetSocketAddress inetSocketAddress, int i) {
        if (z && i == 0) {
            return -1;
        }
        if (datagramPacket != null) {
            datagramPacket.setReceivedLength(i);
            datagramPacket.setPort(inetSocketAddress.getPort());
            if (!inetSocketAddress.getAddress().equals(datagramPacket.getAddress())) {
                datagramPacket.setAddress(inetSocketAddress.getAddress());
            }
        }
        return i;
    }

    private static int maybeThrowAfterRecvfrom(boolean z, boolean z2, ErrnoException errnoException) throws SocketException, SocketTimeoutException {
        if (z) {
            if (errnoException.errno == OsConstants.EAGAIN) {
                return 0;
            }
            throw errnoException.rethrowAsSocketException();
        }
        if (z2 && errnoException.errno == OsConstants.ECONNREFUSED) {
            throw new PortUnreachableException("ICMP Port Unreachable", errnoException);
        }
        if (errnoException.errno == OsConstants.EAGAIN) {
            throw new SocketTimeoutException(errnoException);
        }
        throw errnoException.rethrowAsSocketException();
    }

    public static FileDescriptor socket(int i, int i2, int i3) throws SocketException {
        try {
            return Libcore.os.socket(i, i2, i3);
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }

    public static void poll(FileDescriptor fileDescriptor, int i, int i2) throws SocketException, SocketTimeoutException {
        StructPollfd[] structPollfdArr = {new StructPollfd()};
        structPollfdArr[0].fd = fileDescriptor;
        structPollfdArr[0].events = (short) i;
        try {
            if (android.system.Os.poll(structPollfdArr, i2) == 0) {
                throw new SocketTimeoutException("Poll timed out");
            }
        } catch (ErrnoException e) {
            e.rethrowAsSocketException();
        }
    }

    public static InetSocketAddress getLocalInetSocketAddress(FileDescriptor fileDescriptor) throws SocketException {
        try {
            SocketAddress socketAddress = Libcore.os.getsockname(fileDescriptor);
            if (socketAddress != null && !(socketAddress instanceof InetSocketAddress)) {
                throw new SocketException("Socket assumed to be pending closure: Expected sockname to be an InetSocketAddress, got " + socketAddress.getClass());
            }
            return (InetSocketAddress) socketAddress;
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }
}
