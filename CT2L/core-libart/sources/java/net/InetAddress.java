package java.net;

import android.system.ErrnoException;
import android.system.GaiException;
import android.system.OsConstants;
import android.system.StructAddrinfo;
import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.io.IoBridge;
import libcore.io.Libcore;
import libcore.io.Memory;

public class InetAddress implements Serializable {
    private static final int NETID_UNSET = 0;
    private static final long serialVersionUID = 3286316764910316507L;
    private int family;
    String hostName;
    byte[] ipaddress;
    private static final AddressCache addressCache = new AddressCache();
    public static final InetAddress UNSPECIFIED = new InetAddress(OsConstants.AF_UNSPEC, null, null);
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("address", Integer.TYPE), new ObjectStreamField("family", Integer.TYPE), new ObjectStreamField("hostName", (Class<?>) String.class)};

    InetAddress(int family, byte[] ipaddress, String hostName) {
        this.family = family;
        this.ipaddress = ipaddress;
        this.hostName = hostName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof InetAddress) {
            return Arrays.equals(this.ipaddress, ((InetAddress) obj).ipaddress);
        }
        return false;
    }

    public byte[] getAddress() {
        return (byte[]) this.ipaddress.clone();
    }

    private static InetAddress[] bytesToInetAddresses(byte[][] rawAddresses, String hostName) throws UnknownHostException {
        InetAddress[] returnedAddresses = new InetAddress[rawAddresses.length];
        for (int i = 0; i < rawAddresses.length; i++) {
            returnedAddresses[i] = makeInetAddress(rawAddresses[i], hostName);
        }
        return returnedAddresses;
    }

    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        return (InetAddress[]) getAllByNameImpl(host, 0).clone();
    }

    public static InetAddress[] getAllByNameOnNet(String host, int netId) throws UnknownHostException {
        return (InetAddress[]) getAllByNameImpl(host, netId).clone();
    }

    private static InetAddress[] getAllByNameImpl(String host, int netId) throws UnknownHostException {
        if (host == null || host.isEmpty()) {
            return loopbackAddresses();
        }
        InetAddress result = parseNumericAddressNoThrow(host);
        if (result != null) {
            InetAddress result2 = disallowDeprecatedFormats(host, result);
            if (result2 == null) {
                throw new UnknownHostException("Deprecated IPv4 address format: " + host);
            }
            return new InetAddress[]{result2};
        }
        return (InetAddress[]) lookupHostByName(host, netId).clone();
    }

    private static InetAddress makeInetAddress(byte[] bytes, String hostName) throws UnknownHostException {
        if (bytes.length == 4) {
            return new Inet4Address(bytes, hostName);
        }
        if (bytes.length == 16) {
            return new Inet6Address(bytes, hostName, 0);
        }
        throw badAddressLength(bytes);
    }

    private static InetAddress disallowDeprecatedFormats(String address, InetAddress inetAddress) {
        return ((inetAddress instanceof Inet4Address) && address.indexOf(58) == -1) ? Libcore.os.inet_pton(OsConstants.AF_INET, address) : inetAddress;
    }

    private static InetAddress parseNumericAddressNoThrow(String address) {
        if (address.startsWith("[") && address.endsWith("]") && address.indexOf(58) != -1) {
            address = address.substring(1, address.length() - 1);
        }
        StructAddrinfo hints = new StructAddrinfo();
        hints.ai_flags = OsConstants.AI_NUMERICHOST;
        InetAddress[] addresses = null;
        try {
            addresses = Libcore.os.android_getaddrinfo(address, hints, 0);
        } catch (GaiException e) {
        }
        if (addresses != null) {
            return addresses[0];
        }
        return null;
    }

    public static InetAddress getByName(String host) throws UnknownHostException {
        return getAllByNameImpl(host, 0)[0];
    }

    public static InetAddress getByNameOnNet(String host, int netId) throws UnknownHostException {
        return getAllByNameImpl(host, netId)[0];
    }

    public String getHostAddress() {
        return Libcore.os.getnameinfo(this, OsConstants.NI_NUMERICHOST);
    }

    public String getHostName() {
        if (this.hostName == null) {
            try {
                this.hostName = getHostByAddrImpl(this).hostName;
            } catch (UnknownHostException e) {
                this.hostName = getHostAddress();
            }
        }
        return this.hostName;
    }

    public String getCanonicalHostName() {
        try {
            return getHostByAddrImpl(this).hostName;
        } catch (UnknownHostException e) {
            return getHostAddress();
        }
    }

    public static InetAddress getLocalHost() throws UnknownHostException {
        String host = Libcore.os.uname().nodename;
        return lookupHostByName(host, 0)[0];
    }

    public int hashCode() {
        return Arrays.hashCode(this.ipaddress);
    }

    private static InetAddress[] lookupHostByName(String host, int netId) throws UnknownHostException {
        BlockGuard.getThreadPolicy().onNetwork();
        Object cachedResult = addressCache.get(host, netId);
        if (cachedResult != null) {
            if (cachedResult instanceof InetAddress[]) {
                return (InetAddress[]) cachedResult;
            }
            throw new UnknownHostException((String) cachedResult);
        }
        try {
            StructAddrinfo hints = new StructAddrinfo();
            hints.ai_flags = OsConstants.AI_ADDRCONFIG;
            hints.ai_family = OsConstants.AF_UNSPEC;
            hints.ai_socktype = OsConstants.SOCK_STREAM;
            InetAddress[] addresses = Libcore.os.android_getaddrinfo(host, hints, netId);
            for (InetAddress address : addresses) {
                address.hostName = host;
            }
            addressCache.put(host, netId, addresses);
            return addresses;
        } catch (GaiException gaiException) {
            if ((gaiException.getCause() instanceof ErrnoException) && ((ErrnoException) gaiException.getCause()).errno == OsConstants.EACCES) {
                throw new SecurityException("Permission denied (missing INTERNET permission?)", gaiException);
            }
            String detailMessage = "Unable to resolve host \"" + host + "\": " + Libcore.os.gai_strerror(gaiException.error);
            addressCache.putUnknownHost(host, netId, detailMessage);
            throw gaiException.rethrowAsUnknownHostException(detailMessage);
        }
    }

    public static void clearDnsCache() {
        addressCache.clear();
    }

    private static InetAddress getHostByAddrImpl(InetAddress address) throws UnknownHostException {
        BlockGuard.getThreadPolicy().onNetwork();
        try {
            String hostname = Libcore.os.getnameinfo(address, OsConstants.NI_NAMEREQD);
            return makeInetAddress((byte[]) address.ipaddress.clone(), hostname);
        } catch (GaiException gaiException) {
            throw gaiException.rethrowAsUnknownHostException();
        }
    }

    public String toString() {
        return (this.hostName == null ? "" : this.hostName) + "/" + getHostAddress();
    }

    public static boolean isNumeric(String address) {
        InetAddress inetAddress = parseNumericAddressNoThrow(address);
        return (inetAddress == null || disallowDeprecatedFormats(address, inetAddress) == null) ? false : true;
    }

    public static InetAddress parseNumericAddress(String numericAddress) {
        if (numericAddress == null || numericAddress.isEmpty()) {
            return Inet6Address.LOOPBACK;
        }
        InetAddress result = disallowDeprecatedFormats(numericAddress, parseNumericAddressNoThrow(numericAddress));
        if (result == null) {
            throw new IllegalArgumentException("Not a numeric address: " + numericAddress);
        }
        return result;
    }

    private static InetAddress[] loopbackAddresses() {
        return new InetAddress[]{Inet6Address.LOOPBACK, Inet4Address.LOOPBACK};
    }

    public static InetAddress getLoopbackAddress() {
        return Inet6Address.LOOPBACK;
    }

    public boolean isAnyLocalAddress() {
        return false;
    }

    public boolean isLinkLocalAddress() {
        return false;
    }

    public boolean isLoopbackAddress() {
        return false;
    }

    public boolean isMCGlobal() {
        return false;
    }

    public boolean isMCLinkLocal() {
        return false;
    }

    public boolean isMCNodeLocal() {
        return false;
    }

    public boolean isMCOrgLocal() {
        return false;
    }

    public boolean isMCSiteLocal() {
        return false;
    }

    public boolean isMulticastAddress() {
        return false;
    }

    public boolean isSiteLocalAddress() {
        return false;
    }

    public boolean isReachable(int timeout) throws IOException {
        return isReachable((NetworkInterface) null, 0, timeout);
    }

    public boolean isReachable(NetworkInterface networkInterface, int ttl, final int timeout) throws IOException {
        if (ttl < 0 || timeout < 0) {
            throw new IllegalArgumentException("ttl < 0 || timeout < 0");
        }
        if (networkInterface == null) {
            return isReachable(this, (InetAddress) null, timeout);
        }
        List<InetAddress> sourceAddresses = Collections.list(networkInterface.getInetAddresses());
        if (sourceAddresses.isEmpty()) {
            return false;
        }
        final CountDownLatch latch = new CountDownLatch(sourceAddresses.size());
        final AtomicBoolean isReachable = new AtomicBoolean(false);
        for (final InetAddress sourceAddress : sourceAddresses) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (InetAddress.this.isReachable(this, sourceAddress, timeout)) {
                            isReachable.set(true);
                            while (latch.getCount() > 0) {
                                latch.countDown();
                            }
                        }
                    } catch (IOException e) {
                    }
                    latch.countDown();
                }
            }.start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return isReachable.get();
    }

    private boolean isReachable(InetAddress destination, InetAddress source, int timeout) throws IOException {
        FileDescriptor fd = IoBridge.socket(true);
        boolean reached = false;
        if (source != null) {
            try {
                IoBridge.bind(fd, source, 0);
            } catch (IOException e) {
                if (e.getCause() instanceof ErrnoException) {
                    reached = ((ErrnoException) e.getCause()).errno == OsConstants.ECONNREFUSED;
                }
            }
        }
        IoBridge.connect(fd, destination, 7, timeout);
        reached = true;
        IoBridge.closeAndSignalBlockedThreads(fd);
        return reached;
    }

    public static InetAddress getByAddress(byte[] ipAddress) throws UnknownHostException {
        return getByAddress(null, ipAddress, 0);
    }

    public static InetAddress getByAddress(String hostName, byte[] ipAddress) throws UnknownHostException {
        return getByAddress(hostName, ipAddress, 0);
    }

    private static InetAddress getByAddress(String hostName, byte[] ipAddress, int scopeId) throws UnknownHostException {
        if (ipAddress == null) {
            throw new UnknownHostException("ipAddress == null");
        }
        if (ipAddress.length == 4) {
            return new Inet4Address((byte[]) ipAddress.clone(), hostName);
        }
        if (ipAddress.length == 16) {
            if (isIPv4MappedAddress(ipAddress)) {
                return new Inet4Address(ipv4MappedToIPv4(ipAddress), hostName);
            }
            return new Inet6Address((byte[]) ipAddress.clone(), hostName, scopeId);
        }
        throw badAddressLength(ipAddress);
    }

    private static UnknownHostException badAddressLength(byte[] bytes) throws UnknownHostException {
        throw new UnknownHostException("Address is neither 4 or 16 bytes: " + Arrays.toString(bytes));
    }

    private static boolean isIPv4MappedAddress(byte[] ipAddress) {
        if (ipAddress == null || ipAddress.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (ipAddress[i] != 0) {
                return false;
            }
        }
        return ipAddress[10] == -1 && ipAddress[11] == -1;
    }

    private static byte[] ipv4MappedToIPv4(byte[] mappedAddress) {
        byte[] ipv4Address = new byte[4];
        for (int i = 0; i < 4; i++) {
            ipv4Address[i] = mappedAddress[i + 12];
        }
        return ipv4Address;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        if (this.ipaddress == null) {
            fields.put("address", 0);
        } else {
            fields.put("address", Memory.peekInt(this.ipaddress, 0, ByteOrder.BIG_ENDIAN));
        }
        fields.put("family", this.family);
        fields.put("hostName", this.hostName);
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        int addr = fields.get("address", 0);
        this.ipaddress = new byte[4];
        Memory.pokeInt(this.ipaddress, 0, addr, ByteOrder.BIG_ENDIAN);
        this.hostName = (String) fields.get("hostName", (Object) null);
        this.family = fields.get("family", 2);
    }

    private Object readResolve() throws ObjectStreamException {
        return new Inet4Address(this.ipaddress, this.hostName);
    }
}
