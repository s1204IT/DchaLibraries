package java.net;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructIfaddrs;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import libcore.io.IoUtils;
import libcore.io.Libcore;

public final class NetworkInterface {
    private static final int defaultIndex;
    private static final NetworkInterface defaultInterface = DefaultInterface.getDefault();
    private InetAddress[] addrs;
    private InterfaceAddress[] bindings;
    private List<NetworkInterface> childs;
    private String displayName;
    private byte[] hardwareAddr;
    private int index;
    private String name;
    private NetworkInterface parent = null;
    private boolean virtual = false;

    static {
        if (defaultInterface != null) {
            defaultIndex = defaultInterface.getIndex();
        } else {
            defaultIndex = 0;
        }
    }

    NetworkInterface() {
    }

    NetworkInterface(String str, int i, InetAddress[] inetAddressArr) {
        this.name = str;
        this.index = i;
        this.addrs = inetAddressArr;
    }

    public String getName() {
        return this.name;
    }

    public Enumeration<InetAddress> getInetAddresses() {
        return new Enumeration<InetAddress>() {
            private int count;
            private int i = 0;
            private InetAddress[] local_addrs;

            {
                boolean z;
                this.count = 0;
                this.local_addrs = new InetAddress[NetworkInterface.this.addrs.length];
                SecurityManager securityManager = System.getSecurityManager();
                if (securityManager != null) {
                    try {
                        securityManager.checkPermission(new NetPermission("getNetworkInformation"));
                        z = true;
                    } catch (SecurityException e) {
                        z = false;
                    }
                } else {
                    z = true;
                }
                for (int i = 0; i < NetworkInterface.this.addrs.length; i++) {
                    if (securityManager != null && !z) {
                        try {
                            securityManager.checkConnect(NetworkInterface.this.addrs[i].getHostAddress(), -1);
                        } catch (SecurityException e2) {
                        }
                    }
                    InetAddress[] inetAddressArr = this.local_addrs;
                    int i2 = this.count;
                    this.count = i2 + 1;
                    inetAddressArr[i2] = NetworkInterface.this.addrs[i];
                }
            }

            @Override
            public InetAddress nextElement() {
                if (this.i < this.count) {
                    InetAddress[] inetAddressArr = this.local_addrs;
                    int i = this.i;
                    this.i = i + 1;
                    return inetAddressArr[i];
                }
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasMoreElements() {
                return this.i < this.count;
            }
        };
    }

    public List<InterfaceAddress> getInterfaceAddresses() {
        ArrayList arrayList = new ArrayList(1);
        if (this.bindings != null) {
            SecurityManager securityManager = System.getSecurityManager();
            for (int i = 0; i < this.bindings.length; i++) {
                if (securityManager != null) {
                    try {
                        securityManager.checkConnect(this.bindings[i].getAddress().getHostAddress(), -1);
                    } catch (SecurityException e) {
                    }
                }
                arrayList.add(this.bindings[i]);
            }
        }
        return arrayList;
    }

    public Enumeration<NetworkInterface> getSubInterfaces() {
        return Collections.enumeration(this.childs);
    }

    public NetworkInterface getParent() {
        return this.parent;
    }

    public int getIndex() {
        return this.index;
    }

    public String getDisplayName() {
        if ("".equals(this.displayName)) {
            return null;
        }
        return this.displayName;
    }

    public static NetworkInterface getByName(String str) throws SocketException {
        if (str == null) {
            throw new NullPointerException();
        }
        for (NetworkInterface networkInterface : getAll()) {
            if (networkInterface.getName().equals(str)) {
                return networkInterface;
            }
        }
        return null;
    }

    public static NetworkInterface getByIndex(int i) throws SocketException {
        if (i < 0) {
            throw new IllegalArgumentException("Interface index can't be negative");
        }
        for (NetworkInterface networkInterface : getAll()) {
            if (networkInterface.getIndex() == i) {
                return networkInterface;
            }
        }
        return null;
    }

    public static NetworkInterface getByInetAddress(InetAddress inetAddress) throws SocketException {
        if (inetAddress == null) {
            throw new NullPointerException();
        }
        if (!(inetAddress instanceof Inet4Address) && !(inetAddress instanceof Inet6Address)) {
            throw new IllegalArgumentException("invalid address type");
        }
        for (NetworkInterface networkInterface : getAll()) {
            Iterator it = Collections.list(networkInterface.getInetAddresses()).iterator();
            while (it.hasNext()) {
                if (((InetAddress) it.next()).equals(inetAddress)) {
                    return networkInterface;
                }
            }
        }
        return null;
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        NetworkInterface[] all = getAll();
        if (all.length == 0) {
            return null;
        }
        return Collections.enumeration(Arrays.asList(all));
    }

    private static NetworkInterface[] getAll() throws SocketException {
        HashMap map = new HashMap();
        try {
            for (StructIfaddrs structIfaddrs : Libcore.os.getifaddrs()) {
                String str = structIfaddrs.ifa_name;
                Collection arrayList = (List) map.get(str);
                if (arrayList == null) {
                    arrayList = new ArrayList();
                    map.put(str, arrayList);
                }
                arrayList.add(structIfaddrs);
            }
            HashMap map2 = new HashMap(map.size());
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String str2 = (String) entry.getKey();
                int iIf_nametoindex = Libcore.os.if_nametoindex((String) entry.getKey());
                if (iIf_nametoindex != 0) {
                    NetworkInterface networkInterface = new NetworkInterface(str2, iIf_nametoindex, null);
                    networkInterface.displayName = str2;
                    ArrayList arrayList2 = new ArrayList();
                    ArrayList arrayList3 = new ArrayList();
                    for (StructIfaddrs structIfaddrs2 : (List) entry.getValue()) {
                        if (structIfaddrs2.ifa_addr != null) {
                            arrayList2.add(structIfaddrs2.ifa_addr);
                            arrayList3.add(new InterfaceAddress(structIfaddrs2.ifa_addr, (Inet4Address) structIfaddrs2.ifa_broadaddr, structIfaddrs2.ifa_netmask));
                        }
                        if (structIfaddrs2.hwaddr != null) {
                            networkInterface.hardwareAddr = structIfaddrs2.hwaddr;
                        }
                    }
                    networkInterface.addrs = (InetAddress[]) arrayList2.toArray(new InetAddress[arrayList2.size()]);
                    networkInterface.bindings = (InterfaceAddress[]) arrayList3.toArray(new InterfaceAddress[arrayList3.size()]);
                    networkInterface.childs = new ArrayList(0);
                    map2.put(str2, networkInterface);
                }
            }
            Iterator it2 = map2.entrySet().iterator();
            while (it2.hasNext()) {
                NetworkInterface networkInterface2 = (NetworkInterface) ((Map.Entry) it2.next()).getValue();
                String name = networkInterface2.getName();
                int iIndexOf = name.indexOf(58);
                if (iIndexOf != -1) {
                    NetworkInterface networkInterface3 = (NetworkInterface) map2.get(name.substring(0, iIndexOf));
                    networkInterface2.virtual = true;
                    networkInterface2.parent = networkInterface3;
                    networkInterface3.childs.add(networkInterface2);
                }
            }
            return (NetworkInterface[]) map2.values().toArray(new NetworkInterface[map2.size()]);
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }

    public boolean isUp() throws SocketException {
        int i = OsConstants.IFF_UP | OsConstants.IFF_RUNNING;
        return (getFlags() & i) == i;
    }

    public boolean isLoopback() throws SocketException {
        return (getFlags() & OsConstants.IFF_LOOPBACK) != 0;
    }

    public boolean isPointToPoint() throws SocketException {
        return (getFlags() & OsConstants.IFF_POINTOPOINT) != 0;
    }

    public boolean supportsMulticast() throws SocketException {
        return (getFlags() & OsConstants.IFF_MULTICAST) != 0;
    }

    public byte[] getHardwareAddress() throws SocketException {
        NetworkInterface byName = getByName(this.name);
        if (byName == null) {
            throw new SocketException("NetworkInterface doesn't exist anymore");
        }
        return byName.hardwareAddr;
    }

    public int getMTU() throws Throwable {
        FileDescriptor fileDescriptorSocket;
        FileDescriptor fileDescriptor = null;
        try {
            try {
                fileDescriptorSocket = Libcore.rawOs.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, 0);
            } catch (Throwable th) {
                th = th;
            }
        } catch (ErrnoException e) {
            e = e;
        } catch (Exception e2) {
            e = e2;
        }
        try {
            int iIoctlMTU = Libcore.rawOs.ioctlMTU(fileDescriptorSocket, this.name);
            IoUtils.closeQuietly(fileDescriptorSocket);
            return iIoctlMTU;
        } catch (ErrnoException e3) {
            e = e3;
            throw e.rethrowAsSocketException();
        } catch (Exception e4) {
            e = e4;
            throw new SocketException(e);
        } catch (Throwable th2) {
            th = th2;
            fileDescriptor = fileDescriptorSocket;
            IoUtils.closeQuietly(fileDescriptor);
            throw th;
        }
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    private int getFlags() throws Throwable {
        FileDescriptor fileDescriptorSocket;
        FileDescriptor fileDescriptor = null;
        try {
            try {
                fileDescriptorSocket = Libcore.rawOs.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, 0);
            } catch (Throwable th) {
                th = th;
            }
        } catch (ErrnoException e) {
            e = e;
        } catch (Exception e2) {
            e = e2;
        }
        try {
            int iIoctlFlags = Libcore.rawOs.ioctlFlags(fileDescriptorSocket, this.name);
            IoUtils.closeQuietly(fileDescriptorSocket);
            return iIoctlFlags;
        } catch (ErrnoException e3) {
            e = e3;
            throw e.rethrowAsSocketException();
        } catch (Exception e4) {
            e = e4;
            throw new SocketException(e);
        } catch (Throwable th2) {
            th = th2;
            fileDescriptor = fileDescriptorSocket;
            IoUtils.closeQuietly(fileDescriptor);
            throw th;
        }
    }

    public boolean equals(Object obj) {
        boolean z;
        if (!(obj instanceof NetworkInterface)) {
            return false;
        }
        NetworkInterface networkInterface = (NetworkInterface) obj;
        if (this.name != null) {
            if (!this.name.equals(networkInterface.name)) {
                return false;
            }
        } else if (networkInterface.name != null) {
            return false;
        }
        if (this.addrs == null) {
            return networkInterface.addrs == null;
        }
        if (networkInterface.addrs == null || this.addrs.length != networkInterface.addrs.length) {
            return false;
        }
        InetAddress[] inetAddressArr = networkInterface.addrs;
        int length = inetAddressArr.length;
        for (int i = 0; i < length; i++) {
            int i2 = 0;
            while (true) {
                if (i2 < length) {
                    if (!this.addrs[i].equals(inetAddressArr[i2])) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        if (this.name == null) {
            return 0;
        }
        return this.name.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("name:");
        sb.append(this.name == null ? "null" : this.name);
        String string = sb.toString();
        if (this.displayName != null) {
            return string + " (" + this.displayName + ")";
        }
        return string;
    }

    static NetworkInterface getDefault() {
        return defaultInterface;
    }
}
