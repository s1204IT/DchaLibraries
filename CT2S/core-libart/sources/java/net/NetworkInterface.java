package java.net;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import libcore.io.IoUtils;
import libcore.io.Libcore;

public final class NetworkInterface {
    private static final File SYS_CLASS_NET = new File("/sys/class/net");
    private final List<InetAddress> addresses;
    private final List<InterfaceAddress> interfaceAddresses;
    private final int interfaceIndex;
    private final String name;
    private final List<NetworkInterface> children = new LinkedList();
    private NetworkInterface parent = null;

    private NetworkInterface(String name, int interfaceIndex, List<InetAddress> addresses, List<InterfaceAddress> interfaceAddresses) {
        this.name = name;
        this.interfaceIndex = interfaceIndex;
        this.addresses = addresses;
        this.interfaceAddresses = interfaceAddresses;
    }

    static NetworkInterface forUnboundMulticastSocket() {
        return new NetworkInterface(null, -1, Arrays.asList(Inet6Address.ANY), Collections.emptyList());
    }

    public int getIndex() {
        return this.interfaceIndex;
    }

    public String getName() {
        return this.name;
    }

    public Enumeration<InetAddress> getInetAddresses() {
        return Collections.enumeration(this.addresses);
    }

    public String getDisplayName() {
        return this.name;
    }

    public static NetworkInterface getByName(String interfaceName) throws SocketException {
        if (interfaceName == null) {
            throw new NullPointerException("interfaceName == null");
        }
        if (isValidInterfaceName(interfaceName)) {
            return getByNameInternal(interfaceName, readIfInet6Lines());
        }
        return null;
    }

    private static NetworkInterface getByNameInternal(String interfaceName, String[] ifInet6Lines) throws SocketException {
        int interfaceIndex = readIntFile("/sys/class/net/" + interfaceName + "/ifindex");
        List<InetAddress> addresses = new ArrayList<>();
        List<InterfaceAddress> interfaceAddresses = new ArrayList<>();
        collectIpv6Addresses(interfaceName, interfaceIndex, addresses, interfaceAddresses, ifInet6Lines);
        collectIpv4Address(interfaceName, addresses, interfaceAddresses);
        return new NetworkInterface(interfaceName, interfaceIndex, addresses, interfaceAddresses);
    }

    private static String[] readIfInet6Lines() throws SocketException {
        try {
            return IoUtils.readFileAsString("/proc/net/if_inet6").split("\n");
        } catch (IOException ioe) {
            throw rethrowAsSocketException(ioe);
        }
    }

    public static void collectIpv6Addresses(String interfaceName, int interfaceIndex, List<InetAddress> addresses, List<InterfaceAddress> interfaceAddresses, String[] ifInet6Lines) throws SocketException {
        String suffix = " " + interfaceName;
        try {
            for (String line : ifInet6Lines) {
                if (line.endsWith(suffix)) {
                    byte[] addressBytes = new byte[16];
                    for (int i = 0; i < addressBytes.length; i++) {
                        addressBytes[i] = (byte) Integer.parseInt(line.substring(i * 2, (i * 2) + 2), 16);
                    }
                    int prefixLengthStart = line.indexOf(32, 33) + 1;
                    int prefixLengthEnd = line.indexOf(32, prefixLengthStart);
                    short prefixLength = Short.parseShort(line.substring(prefixLengthStart, prefixLengthEnd), 16);
                    Inet6Address inet6Address = new Inet6Address(addressBytes, null, interfaceIndex);
                    addresses.add(inet6Address);
                    interfaceAddresses.add(new InterfaceAddress(inet6Address, prefixLength));
                }
            }
        } catch (NumberFormatException ex) {
            throw rethrowAsSocketException(ex);
        }
    }

    private static void collectIpv4Address(String interfaceName, List<InetAddress> addresses, List<InterfaceAddress> interfaceAddresses) throws SocketException {
        FileDescriptor fd = null;
        try {
            try {
                fd = Libcore.os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, 0);
                InetAddress address = Libcore.os.ioctlInetAddress(fd, OsConstants.SIOCGIFADDR, interfaceName);
                InetAddress broadcast = Libcore.os.ioctlInetAddress(fd, OsConstants.SIOCGIFBRDADDR, interfaceName);
                InetAddress netmask = Libcore.os.ioctlInetAddress(fd, OsConstants.SIOCGIFNETMASK, interfaceName);
                if (broadcast.equals(Inet4Address.ANY)) {
                    broadcast = null;
                }
                addresses.add(address);
                interfaceAddresses.add(new InterfaceAddress((Inet4Address) address, (Inet4Address) broadcast, (Inet4Address) netmask));
                IoUtils.closeQuietly(fd);
            } catch (ErrnoException errnoException) {
                if (errnoException.errno != OsConstants.EADDRNOTAVAIL) {
                    throw rethrowAsSocketException(errnoException);
                }
                IoUtils.closeQuietly(fd);
            } catch (Exception ex) {
                throw rethrowAsSocketException(ex);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(fd);
            throw th;
        }
    }

    @FindBugsSuppressWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
    private static boolean isValidInterfaceName(String interfaceName) {
        String[] interfaceList = SYS_CLASS_NET.list();
        if (interfaceList == null) {
            return false;
        }
        for (String validName : interfaceList) {
            if (interfaceName.equals(validName)) {
                return true;
            }
        }
        return false;
    }

    private static int readIntFile(String path) throws SocketException {
        try {
            String s = IoUtils.readFileAsString(path).trim();
            return s.startsWith("0x") ? Integer.parseInt(s.substring(2), 16) : Integer.parseInt(s);
        } catch (Exception ex) {
            throw rethrowAsSocketException(ex);
        }
    }

    private static SocketException rethrowAsSocketException(Exception ex) throws SocketException {
        SocketException result = new SocketException();
        result.initCause(ex);
        throw result;
    }

    public static NetworkInterface getByInetAddress(InetAddress address) throws SocketException {
        if (address == null) {
            throw new NullPointerException("address == null");
        }
        for (NetworkInterface networkInterface : getNetworkInterfacesList()) {
            if (networkInterface.addresses.contains(address)) {
                return networkInterface;
            }
        }
        return null;
    }

    public static NetworkInterface getByIndex(int index) throws SocketException {
        String name = Libcore.os.if_indextoname(index);
        if (name == null) {
            return null;
        }
        return getByName(name);
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        return Collections.enumeration(getNetworkInterfacesList());
    }

    @FindBugsSuppressWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
    private static List<NetworkInterface> getNetworkInterfacesList() throws SocketException {
        String[] interfaceNames = SYS_CLASS_NET.list();
        NetworkInterface[] interfaces = new NetworkInterface[interfaceNames.length];
        boolean[] done = new boolean[interfaces.length];
        String[] ifInet6Lines = readIfInet6Lines();
        for (int i = 0; i < interfaceNames.length; i++) {
            interfaces[i] = getByNameInternal(interfaceNames[i], ifInet6Lines);
            if (interfaces[i] == null) {
                done[i] = true;
            }
        }
        List<NetworkInterface> result = new ArrayList<>();
        for (int counter = 0; counter < interfaces.length; counter++) {
            if (!done[counter]) {
                for (int counter2 = counter; counter2 < interfaces.length; counter2++) {
                    if (!done[counter2] && interfaces[counter2].name.startsWith(interfaces[counter].name + ":")) {
                        interfaces[counter].children.add(interfaces[counter2]);
                        interfaces[counter2].parent = interfaces[counter];
                        interfaces[counter].addresses.addAll(interfaces[counter2].addresses);
                        done[counter2] = true;
                    }
                }
                result.add(interfaces[counter]);
                done[counter] = true;
            }
        }
        return result;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NetworkInterface)) {
            return false;
        }
        NetworkInterface rhs = (NetworkInterface) obj;
        return this.interfaceIndex == rhs.interfaceIndex && this.name.equals(rhs.name) && this.addresses.equals(rhs.addresses);
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(25);
        sb.append("[");
        sb.append(this.name);
        sb.append("][");
        sb.append(this.interfaceIndex);
        sb.append("]");
        for (InetAddress address : this.addresses) {
            sb.append("[");
            sb.append(address.toString());
            sb.append("]");
        }
        return sb.toString();
    }

    public List<InterfaceAddress> getInterfaceAddresses() {
        return Collections.unmodifiableList(this.interfaceAddresses);
    }

    public Enumeration<NetworkInterface> getSubInterfaces() {
        return Collections.enumeration(this.children);
    }

    public NetworkInterface getParent() {
        return this.parent;
    }

    public boolean isUp() throws SocketException {
        return hasFlag(OsConstants.IFF_UP);
    }

    public boolean isLoopback() throws SocketException {
        return hasFlag(OsConstants.IFF_LOOPBACK);
    }

    public boolean isPointToPoint() throws SocketException {
        return hasFlag(OsConstants.IFF_POINTOPOINT);
    }

    public boolean supportsMulticast() throws SocketException {
        return hasFlag(OsConstants.IFF_MULTICAST);
    }

    private boolean hasFlag(int mask) throws SocketException {
        int flags = readIntFile("/sys/class/net/" + this.name + "/flags");
        return (flags & mask) != 0;
    }

    public byte[] getHardwareAddress() throws SocketException {
        try {
            String s = IoUtils.readFileAsString("/sys/class/net/" + this.name + "/address");
            byte[] result = new byte[s.length() / 3];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) Integer.parseInt(s.substring(i * 3, (i * 3) + 2), 16);
            }
            for (byte b : result) {
                if (b != 0) {
                    return result;
                }
            }
            return null;
        } catch (Exception ex) {
            throw rethrowAsSocketException(ex);
        }
    }

    public int getMTU() throws SocketException {
        return readIntFile("/sys/class/net/" + this.name + "/mtu");
    }

    public boolean isVirtual() {
        return this.parent != null;
    }
}
