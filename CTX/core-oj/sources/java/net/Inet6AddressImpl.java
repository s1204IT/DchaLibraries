package java.net;

import android.system.ErrnoException;
import android.system.GaiException;
import android.system.OsConstants;
import android.system.StructAddrinfo;
import android.system.StructIcmpHdr;
import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Enumeration;
import libcore.io.IoBridge;
import libcore.io.Libcore;
import libcore.io.Os;

class Inet6AddressImpl implements InetAddressImpl {
    private static final AddressCache addressCache = new AddressCache();
    private static InetAddress anyLocalAddress;
    private static InetAddress[] loopbackAddresses;

    Inet6AddressImpl() {
    }

    @Override
    public InetAddress[] lookupAllHostAddr(String str, int i) throws UnknownHostException {
        if (str == null || str.isEmpty()) {
            return loopbackAddresses();
        }
        InetAddress numericAddressNoThrow = InetAddress.parseNumericAddressNoThrow(str);
        if (numericAddressNoThrow != null) {
            InetAddress inetAddressDisallowDeprecatedFormats = InetAddress.disallowDeprecatedFormats(str, numericAddressNoThrow);
            if (inetAddressDisallowDeprecatedFormats == null) {
                throw new UnknownHostException("Deprecated IPv4 address format: " + str);
            }
            return new InetAddress[]{inetAddressDisallowDeprecatedFormats};
        }
        return lookupHostByName(str, i);
    }

    private static InetAddress[] lookupHostByName(String str, int i) throws UnknownHostException {
        BlockGuard.getThreadPolicy().onNetwork();
        Object obj = addressCache.get(str, i);
        if (obj != null) {
            if (obj instanceof InetAddress[]) {
                return (InetAddress[]) obj;
            }
            throw new UnknownHostException((String) obj);
        }
        try {
            StructAddrinfo structAddrinfo = new StructAddrinfo();
            structAddrinfo.ai_flags = OsConstants.AI_ADDRCONFIG;
            structAddrinfo.ai_family = OsConstants.AF_UNSPEC;
            structAddrinfo.ai_socktype = OsConstants.SOCK_STREAM;
            InetAddress[] inetAddressArrAndroid_getaddrinfo = Libcore.os.android_getaddrinfo(str, structAddrinfo, i);
            for (InetAddress inetAddress : inetAddressArrAndroid_getaddrinfo) {
                inetAddress.holder().hostName = str;
                inetAddress.holder().originalHostName = str;
            }
            addressCache.put(str, i, inetAddressArrAndroid_getaddrinfo);
            return inetAddressArrAndroid_getaddrinfo;
        } catch (GaiException e) {
            if ((e.getCause() instanceof ErrnoException) && ((ErrnoException) e.getCause()).errno == OsConstants.EACCES) {
                throw new SecurityException("Permission denied (missing INTERNET permission?)", e);
            }
            String str2 = "Unable to resolve host \"" + str + "\": " + Libcore.os.gai_strerror(e.error);
            addressCache.putUnknownHost(str, i, str2);
            throw e.rethrowAsUnknownHostException(str2);
        }
    }

    @Override
    public String getHostByAddr(byte[] bArr) throws UnknownHostException {
        BlockGuard.getThreadPolicy().onNetwork();
        return getHostByAddr0(bArr);
    }

    @Override
    public void clearAddressCache() {
        addressCache.clear();
    }

    @Override
    public boolean isReachable(InetAddress inetAddress, int i, NetworkInterface networkInterface, int i2) throws IOException {
        InetAddress inetAddress2 = null;
        if (networkInterface != null) {
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (true) {
                if (!inetAddresses.hasMoreElements()) {
                    break;
                }
                InetAddress inetAddressNextElement = inetAddresses.nextElement();
                if (inetAddressNextElement.getClass().isInstance(inetAddress)) {
                    inetAddress2 = inetAddressNextElement;
                    break;
                }
            }
            if (inetAddress2 == null) {
                return false;
            }
        }
        if (icmpEcho(inetAddress, i, inetAddress2, i2)) {
            return true;
        }
        return tcpEcho(inetAddress, i, inetAddress2, i2);
    }

    private boolean tcpEcho(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws Throwable {
        FileDescriptor fileDescriptor = null;
        try {
            try {
                FileDescriptor fileDescriptorSocket = IoBridge.socket(OsConstants.AF_INET6, OsConstants.SOCK_STREAM, 0);
                if (i2 > 0) {
                    try {
                        IoBridge.setSocketOption(fileDescriptorSocket, 25, Integer.valueOf(i2));
                    } catch (IOException e) {
                        e = e;
                        fileDescriptor = fileDescriptorSocket;
                        Throwable cause = e.getCause();
                        boolean z = (cause instanceof ErrnoException) && ((ErrnoException) cause).errno == OsConstants.ECONNREFUSED;
                        IoBridge.closeAndSignalBlockedThreads(fileDescriptor);
                        return z;
                    } catch (Throwable th) {
                        th = th;
                        fileDescriptor = fileDescriptorSocket;
                        IoBridge.closeAndSignalBlockedThreads(fileDescriptor);
                        throw th;
                    }
                }
                if (inetAddress2 != null) {
                    IoBridge.bind(fileDescriptorSocket, inetAddress2, 0);
                }
                IoBridge.connect(fileDescriptorSocket, inetAddress, 7, i);
                IoBridge.closeAndSignalBlockedThreads(fileDescriptorSocket);
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e2) {
            e = e2;
        }
    }

    protected boolean icmpEcho(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws Throwable {
        Throwable th;
        FileDescriptor fileDescriptor;
        Os os;
        byte[] bytes;
        int port;
        byte[] bArr;
        DatagramPacket datagramPacket;
        int i3;
        int i4;
        int i5;
        int i6;
        try {
            try {
                boolean z = inetAddress instanceof Inet4Address;
                FileDescriptor fileDescriptorSocket = IoBridge.socket(z ? OsConstants.AF_INET : OsConstants.AF_INET6, OsConstants.SOCK_DGRAM, z ? OsConstants.IPPROTO_ICMP : OsConstants.IPPROTO_ICMPV6);
                if (i2 > 0) {
                    try {
                        try {
                            IoBridge.setSocketOption(fileDescriptorSocket, 25, Integer.valueOf(i2));
                        } catch (Throwable th2) {
                            th = th2;
                            fileDescriptor = fileDescriptorSocket;
                            if (fileDescriptor != null) {
                                throw th;
                            }
                            try {
                                Libcore.os.close(fileDescriptor);
                                throw th;
                            } catch (ErrnoException e) {
                                throw th;
                            }
                        }
                    } catch (IOException e2) {
                        fileDescriptor = fileDescriptorSocket;
                        if (fileDescriptor != null) {
                            os = Libcore.os;
                            os.close(fileDescriptor);
                        }
                        return false;
                    }
                }
                if (inetAddress2 != null) {
                    IoBridge.bind(fileDescriptorSocket, inetAddress2, 0);
                }
                int i7 = i;
                int i8 = 0;
                while (i7 > 0) {
                    int i9 = i7 >= 1000 ? 1000 : i7;
                    try {
                        IoBridge.setSocketOption(fileDescriptorSocket, SocketOptions.SO_TIMEOUT, Integer.valueOf(i9));
                        bytes = StructIcmpHdr.IcmpEchoHdr(z, i8).getBytes();
                        IoBridge.sendto(fileDescriptorSocket, bytes, 0, bytes.length, 0, inetAddress, 0);
                        port = IoBridge.getLocalInetSocketAddress(fileDescriptorSocket).getPort();
                        bArr = new byte[bytes.length];
                        datagramPacket = new DatagramPacket(bArr, bytes.length);
                        i3 = i9;
                        i4 = i8;
                        i5 = i7;
                        fileDescriptor = fileDescriptorSocket;
                    } catch (Throwable th3) {
                        th = th3;
                        fileDescriptor = fileDescriptorSocket;
                    }
                    try {
                        if (IoBridge.recvfrom(true, fileDescriptorSocket, bArr, 0, bArr.length, 0, datagramPacket, false) != bytes.length) {
                            i6 = i4;
                        } else {
                            byte b = z ? (byte) OsConstants.ICMP_ECHOREPLY : (byte) OsConstants.ICMP6_ECHO_REPLY;
                            if (datagramPacket.getAddress().equals(inetAddress) && bArr[0] == b && bArr[4] == ((byte) (port >> 8)) && bArr[5] == ((byte) port)) {
                                i6 = i4;
                                if (bArr[6] == ((byte) (i6 >> 8)) && bArr[7] == ((byte) i6)) {
                                    if (fileDescriptor == null) {
                                        return true;
                                    }
                                    try {
                                        Libcore.os.close(fileDescriptor);
                                        return true;
                                    } catch (ErrnoException e3) {
                                        return true;
                                    }
                                }
                            }
                        }
                        i7 = i5 - i3;
                        i8 = i6 + 1;
                        fileDescriptorSocket = fileDescriptor;
                    } catch (IOException e4) {
                        if (fileDescriptor != null) {
                        }
                        return false;
                    } catch (Throwable th4) {
                        th = th4;
                        th = th;
                        if (fileDescriptor != null) {
                        }
                    }
                }
                fileDescriptor = fileDescriptorSocket;
            } catch (ErrnoException e5) {
            }
        } catch (IOException e6) {
            fileDescriptor = null;
        } catch (Throwable th5) {
            th = th5;
            fileDescriptor = null;
        }
        if (fileDescriptor != null) {
            os = Libcore.os;
            os.close(fileDescriptor);
        }
        return false;
    }

    @Override
    public InetAddress anyLocalAddress() {
        InetAddress inetAddress;
        synchronized (Inet6AddressImpl.class) {
            if (anyLocalAddress == null) {
                Inet6Address inet6Address = new Inet6Address();
                inet6Address.holder().hostName = "::";
                anyLocalAddress = inet6Address;
            }
            inetAddress = anyLocalAddress;
        }
        return inetAddress;
    }

    @Override
    public InetAddress[] loopbackAddresses() {
        InetAddress[] inetAddressArr;
        synchronized (Inet6AddressImpl.class) {
            if (loopbackAddresses == null) {
                loopbackAddresses = new InetAddress[]{Inet6Address.LOOPBACK, Inet4Address.LOOPBACK};
            }
            inetAddressArr = loopbackAddresses;
        }
        return inetAddressArr;
    }

    private String getHostByAddr0(byte[] bArr) throws UnknownHostException {
        InetAddress byAddress = InetAddress.getByAddress(bArr);
        try {
            return Libcore.os.getnameinfo(byAddress, OsConstants.NI_NAMEREQD);
        } catch (GaiException e) {
            UnknownHostException unknownHostException = new UnknownHostException(byAddress.toString());
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }
}
