package sun.nio.ch;

import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import jdk.net.NetworkPermission;
import jdk.net.SocketFlow;
import sun.net.ExtendedOptionsImpl;

public class Net {
    public static final int SHUT_RD = 0;
    public static final int SHUT_RDWR = 2;
    public static final int SHUT_WR = 1;
    private static final boolean exclusiveBind;
    private static final boolean fastLoopback;
    private static volatile boolean isIPv6Available;
    static final ProtocolFamily UNSPEC = new ProtocolFamily() {
        @Override
        public String name() {
            return "UNSPEC";
        }
    };
    private static volatile boolean checkedIPv6 = false;
    public static final short POLLIN = pollinValue();
    public static final short POLLOUT = polloutValue();
    public static final short POLLERR = pollerrValue();
    public static final short POLLHUP = pollhupValue();
    public static final short POLLNVAL = pollnvalValue();
    public static final short POLLCONN = pollconnValue();

    private static native void bind0(FileDescriptor fileDescriptor, boolean z, boolean z2, InetAddress inetAddress, int i) throws IOException;

    private static native int blockOrUnblock4(boolean z, FileDescriptor fileDescriptor, int i, int i2, int i3) throws IOException;

    static native int blockOrUnblock6(boolean z, FileDescriptor fileDescriptor, byte[] bArr, int i, byte[] bArr2) throws IOException;

    private static native boolean canIPv6SocketJoinIPv4Group0();

    private static native boolean canJoin6WithIPv4Group0();

    private static native int connect0(boolean z, FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws IOException;

    private static native int getIntOption0(FileDescriptor fileDescriptor, boolean z, int i, int i2) throws IOException;

    static native int getInterface4(FileDescriptor fileDescriptor) throws IOException;

    static native int getInterface6(FileDescriptor fileDescriptor) throws IOException;

    private static native int isExclusiveBindAvailable();

    private static native boolean isIPv6Available0();

    private static native int joinOrDrop4(boolean z, FileDescriptor fileDescriptor, int i, int i2, int i3) throws IOException;

    private static native int joinOrDrop6(boolean z, FileDescriptor fileDescriptor, byte[] bArr, int i, byte[] bArr2) throws IOException;

    static native void listen(FileDescriptor fileDescriptor, int i) throws IOException;

    private static native InetAddress localInetAddress(FileDescriptor fileDescriptor) throws IOException;

    private static native int localPort(FileDescriptor fileDescriptor) throws IOException;

    static native int poll(FileDescriptor fileDescriptor, int i, long j) throws IOException;

    static native short pollconnValue();

    static native short pollerrValue();

    static native short pollhupValue();

    static native short pollinValue();

    static native short pollnvalValue();

    static native short polloutValue();

    private static native InetAddress remoteInetAddress(FileDescriptor fileDescriptor) throws IOException;

    private static native int remotePort(FileDescriptor fileDescriptor) throws IOException;

    private static native void setIntOption0(FileDescriptor fileDescriptor, boolean z, int i, int i2, int i3, boolean z2) throws IOException;

    static native void setInterface4(FileDescriptor fileDescriptor, int i) throws IOException;

    static native void setInterface6(FileDescriptor fileDescriptor, int i) throws IOException;

    static native void shutdown(FileDescriptor fileDescriptor, int i) throws IOException;

    private static native int socket0(boolean z, boolean z2, boolean z3, boolean z4);

    private Net() {
    }

    static {
        int iIsExclusiveBindAvailable = isExclusiveBindAvailable();
        if (iIsExclusiveBindAvailable >= 0) {
            String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty("sun.net.useExclusiveBind");
                }
            });
            if (str != null) {
                exclusiveBind = str.length() != 0 ? Boolean.parseBoolean(str) : true;
            } else if (iIsExclusiveBindAvailable == 1) {
                exclusiveBind = true;
            } else {
                exclusiveBind = false;
            }
        } else {
            exclusiveBind = false;
        }
        fastLoopback = isFastTcpLoopbackRequested();
    }

    static boolean isIPv6Available() {
        if (!checkedIPv6) {
            isIPv6Available = isIPv6Available0();
            checkedIPv6 = true;
        }
        return isIPv6Available;
    }

    static boolean useExclusiveBind() {
        return exclusiveBind;
    }

    static boolean canIPv6SocketJoinIPv4Group() {
        return canIPv6SocketJoinIPv4Group0();
    }

    static boolean canJoin6WithIPv4Group() {
        return canJoin6WithIPv4Group0();
    }

    public static InetSocketAddress checkAddress(SocketAddress socketAddress) {
        if (socketAddress == null) {
            throw new IllegalArgumentException("sa == null");
        }
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        if (inetSocketAddress.isUnresolved()) {
            throw new UnresolvedAddressException();
        }
        InetAddress address = inetSocketAddress.getAddress();
        if (!(address instanceof Inet4Address) && !(address instanceof Inet6Address)) {
            throw new IllegalArgumentException("Invalid address type");
        }
        return inetSocketAddress;
    }

    static InetSocketAddress asInetSocketAddress(SocketAddress socketAddress) {
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        return (InetSocketAddress) socketAddress;
    }

    static void translateToSocketException(Exception exc) throws SocketException {
        Exception socketException;
        if (exc instanceof SocketException) {
            throw ((SocketException) exc);
        }
        if (exc instanceof ClosedChannelException) {
            socketException = new SocketException("Socket is closed");
        } else if (exc instanceof NotYetConnectedException) {
            socketException = new SocketException("Socket is not connected");
        } else if (exc instanceof AlreadyBoundException) {
            socketException = new SocketException("Already bound");
        } else if (exc instanceof NotYetBoundException) {
            socketException = new SocketException("Socket is not bound yet");
        } else if (exc instanceof UnsupportedAddressTypeException) {
            socketException = new SocketException("Unsupported address type");
        } else if (exc instanceof UnresolvedAddressException) {
            socketException = new SocketException("Unresolved address");
        } else if (exc instanceof AlreadyConnectedException) {
            socketException = new SocketException("Already connected");
        } else {
            socketException = exc;
        }
        if (socketException != exc) {
            socketException.initCause(exc);
        }
        if (socketException instanceof SocketException) {
            throw ((SocketException) socketException);
        }
        if (socketException instanceof RuntimeException) {
            throw ((RuntimeException) socketException);
        }
        throw new Error("Untranslated exception", socketException);
    }

    static void translateException(Exception exc, boolean z) throws IOException {
        if (exc instanceof IOException) {
            throw ((IOException) exc);
        }
        if (z && (exc instanceof UnresolvedAddressException)) {
            throw new UnknownHostException();
        }
        translateToSocketException(exc);
    }

    static void translateException(Exception exc) throws IOException {
        translateException(exc, false);
    }

    static InetSocketAddress getRevealedLocalAddress(InetSocketAddress inetSocketAddress) {
        SecurityManager securityManager = System.getSecurityManager();
        if (inetSocketAddress == null || securityManager == null) {
            return inetSocketAddress;
        }
        try {
            securityManager.checkConnect(inetSocketAddress.getAddress().getHostAddress(), -1);
            return inetSocketAddress;
        } catch (SecurityException e) {
            return getLoopbackAddress(inetSocketAddress.getPort());
        }
    }

    static String getRevealedLocalAddressAsString(InetSocketAddress inetSocketAddress) {
        return System.getSecurityManager() == null ? inetSocketAddress.toString() : getLoopbackAddress(inetSocketAddress.getPort()).toString();
    }

    private static InetSocketAddress getLoopbackAddress(int i) {
        return new InetSocketAddress(InetAddress.getLoopbackAddress(), i);
    }

    static Inet4Address anyInet4Address(final NetworkInterface networkInterface) {
        return (Inet4Address) AccessController.doPrivileged(new PrivilegedAction<Inet4Address>() {
            @Override
            public Inet4Address run() {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddressNextElement = inetAddresses.nextElement();
                    if (inetAddressNextElement instanceof Inet4Address) {
                        return (Inet4Address) inetAddressNextElement;
                    }
                }
                return null;
            }
        });
    }

    static int inet4AsInt(InetAddress inetAddress) {
        if (inetAddress instanceof Inet4Address) {
            byte[] address = inetAddress.getAddress();
            return ((address[0] << 24) & (-16777216)) | (address[3] & Character.DIRECTIONALITY_UNDEFINED) | ((address[2] << 8) & 65280) | ((address[1] << 16) & 16711680);
        }
        throw new AssertionError((Object) "Should not reach here");
    }

    static InetAddress inet4FromInt(int i) {
        try {
            return InetAddress.getByAddress(new byte[]{(byte) ((i >>> 24) & 255), (byte) ((i >>> 16) & 255), (byte) ((i >>> 8) & 255), (byte) (i & 255)});
        } catch (UnknownHostException e) {
            throw new AssertionError((Object) "Should not reach here");
        }
    }

    static byte[] inet6AsByteArray(InetAddress inetAddress) {
        if (inetAddress instanceof Inet6Address) {
            return inetAddress.getAddress();
        }
        if (inetAddress instanceof Inet4Address) {
            byte[] address = inetAddress.getAddress();
            byte[] bArr = new byte[16];
            bArr[10] = -1;
            bArr[11] = -1;
            bArr[12] = address[0];
            bArr[13] = address[1];
            bArr[14] = address[2];
            bArr[15] = address[3];
            return bArr;
        }
        throw new AssertionError((Object) "Should not reach here");
    }

    static void setSocketOption(FileDescriptor fileDescriptor, ProtocolFamily protocolFamily, SocketOption<?> socketOption, Object obj) throws IOException {
        ?? BooleanValue;
        int iIntValue;
        int iIntValue2;
        if (obj == null) {
            throw new IllegalArgumentException("Invalid option value");
        }
        Class<?> clsType = socketOption.type();
        if (clsType == SocketFlow.class) {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(new NetworkPermission("setOption.SO_FLOW_SLA"));
            }
            ExtendedOptionsImpl.setFlowOption(fileDescriptor, (SocketFlow) obj);
            return;
        }
        if (clsType != Integer.class && clsType != Boolean.class) {
            throw new AssertionError((Object) "Should not reach here");
        }
        if ((socketOption == StandardSocketOptions.SO_RCVBUF || socketOption == StandardSocketOptions.SO_SNDBUF) && ((Integer) obj).intValue() < 0) {
            throw new IllegalArgumentException("Invalid send/receive buffer size");
        }
        if (socketOption == StandardSocketOptions.SO_LINGER) {
            int iIntValue3 = ((Integer) obj).intValue();
            if (iIntValue3 < 0) {
                obj = -1;
            }
            if (iIntValue3 > 65535) {
                obj = 65535;
            }
        }
        if (socketOption == StandardSocketOptions.IP_TOS && ((iIntValue2 = ((Integer) obj).intValue()) < 0 || iIntValue2 > 255)) {
            throw new IllegalArgumentException("Invalid IP_TOS value");
        }
        if (socketOption == StandardSocketOptions.IP_MULTICAST_TTL && ((iIntValue = ((Integer) obj).intValue()) < 0 || iIntValue > 255)) {
            throw new IllegalArgumentException("Invalid TTL/hop value");
        }
        OptionKey optionKeyFindOption = SocketOptionRegistry.findOption(socketOption, protocolFamily);
        if (optionKeyFindOption == null) {
            throw new AssertionError((Object) "Option not found");
        }
        if (clsType == Integer.class) {
            BooleanValue = ((Integer) obj).intValue();
        } else {
            BooleanValue = ((Boolean) obj).booleanValue();
        }
        setIntOption0(fileDescriptor, protocolFamily == UNSPEC, optionKeyFindOption.level(), optionKeyFindOption.name(), BooleanValue == true ? 1 : 0, protocolFamily == StandardProtocolFamily.INET6);
    }

    static Object getSocketOption(FileDescriptor fileDescriptor, ProtocolFamily protocolFamily, SocketOption<?> socketOption) throws IOException {
        Class<?> clsType = socketOption.type();
        if (clsType == SocketFlow.class) {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(new NetworkPermission("getOption.SO_FLOW_SLA"));
            }
            SocketFlow socketFlowCreate = SocketFlow.create();
            ExtendedOptionsImpl.getFlowOption(fileDescriptor, socketFlowCreate);
            return socketFlowCreate;
        }
        if (clsType != Integer.class && clsType != Boolean.class) {
            throw new AssertionError((Object) "Should not reach here");
        }
        OptionKey optionKeyFindOption = SocketOptionRegistry.findOption(socketOption, protocolFamily);
        if (optionKeyFindOption == null) {
            throw new AssertionError((Object) "Option not found");
        }
        int intOption0 = getIntOption0(fileDescriptor, protocolFamily == UNSPEC, optionKeyFindOption.level(), optionKeyFindOption.name());
        if (clsType == Integer.class) {
            return Integer.valueOf(intOption0);
        }
        return intOption0 == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    public static boolean isFastTcpLoopbackRequested() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("jdk.net.useFastTcpLoopback");
            }
        });
        if ("".equals(str)) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    static FileDescriptor socket(boolean z) throws IOException {
        return socket(UNSPEC, z);
    }

    static FileDescriptor socket(ProtocolFamily protocolFamily, boolean z) throws IOException {
        return IOUtil.newFD(socket0(isIPv6Available() && protocolFamily != StandardProtocolFamily.INET, z, false, fastLoopback));
    }

    static FileDescriptor serverSocket(boolean z) {
        return IOUtil.newFD(socket0(isIPv6Available(), z, true, fastLoopback));
    }

    public static void bind(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws IOException {
        bind(UNSPEC, fileDescriptor, inetAddress, i);
    }

    static void bind(ProtocolFamily protocolFamily, FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws IOException {
        bind0(fileDescriptor, isIPv6Available() && protocolFamily != StandardProtocolFamily.INET, exclusiveBind, inetAddress, i);
    }

    static int connect(FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws IOException {
        return connect(UNSPEC, fileDescriptor, inetAddress, i);
    }

    static int connect(ProtocolFamily protocolFamily, FileDescriptor fileDescriptor, InetAddress inetAddress, int i) throws IOException {
        BlockGuard.getThreadPolicy().onNetwork();
        return connect0(isIPv6Available() && protocolFamily != StandardProtocolFamily.INET, fileDescriptor, inetAddress, i);
    }

    public static InetSocketAddress localAddress(FileDescriptor fileDescriptor) throws IOException {
        return new InetSocketAddress(localInetAddress(fileDescriptor), localPort(fileDescriptor));
    }

    static InetSocketAddress remoteAddress(FileDescriptor fileDescriptor) throws IOException {
        return new InetSocketAddress(remoteInetAddress(fileDescriptor), remotePort(fileDescriptor));
    }

    static int join4(FileDescriptor fileDescriptor, int i, int i2, int i3) throws IOException {
        return joinOrDrop4(true, fileDescriptor, i, i2, i3);
    }

    static void drop4(FileDescriptor fileDescriptor, int i, int i2, int i3) throws IOException {
        joinOrDrop4(false, fileDescriptor, i, i2, i3);
    }

    static int block4(FileDescriptor fileDescriptor, int i, int i2, int i3) throws IOException {
        return blockOrUnblock4(true, fileDescriptor, i, i2, i3);
    }

    static void unblock4(FileDescriptor fileDescriptor, int i, int i2, int i3) throws IOException {
        blockOrUnblock4(false, fileDescriptor, i, i2, i3);
    }

    static int join6(FileDescriptor fileDescriptor, byte[] bArr, int i, byte[] bArr2) throws IOException {
        return joinOrDrop6(true, fileDescriptor, bArr, i, bArr2);
    }

    static void drop6(FileDescriptor fileDescriptor, byte[] bArr, int i, byte[] bArr2) throws IOException {
        joinOrDrop6(false, fileDescriptor, bArr, i, bArr2);
    }

    static int block6(FileDescriptor fileDescriptor, byte[] bArr, int i, byte[] bArr2) throws IOException {
        return blockOrUnblock6(true, fileDescriptor, bArr, i, bArr2);
    }

    static void unblock6(FileDescriptor fileDescriptor, byte[] bArr, int i, byte[] bArr2) throws IOException {
        blockOrUnblock6(false, fileDescriptor, bArr, i, bArr2);
    }
}
