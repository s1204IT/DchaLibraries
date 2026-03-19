package java.net;

import android.system.GaiException;
import android.system.OsConstants;
import android.system.StructAddrinfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import libcore.io.Libcore;
import sun.net.spi.nameservice.NameService;
import sun.net.util.IPAddressUtil;

public class InetAddress implements Serializable {
    static final int NETID_UNSET = 0;
    private static final long serialVersionUID = 3286316764910316507L;
    private transient String canonicalHostName = null;
    transient InetAddressHolder holder = new InetAddressHolder();
    static final InetAddressImpl impl = new Inet6AddressImpl();
    private static final NameService nameService = new NameService() {
        @Override
        public InetAddress[] lookupAllHostAddr(String str, int i) throws UnknownHostException {
            return InetAddress.impl.lookupAllHostAddr(str, i);
        }

        @Override
        public String getHostByAddr(byte[] bArr) throws UnknownHostException {
            return InetAddress.impl.getHostByAddr(bArr);
        }
    };
    private static final ClassLoader BOOT_CLASSLOADER = Object.class.getClassLoader();
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("hostName", String.class), new ObjectStreamField("address", Integer.TYPE), new ObjectStreamField("family", Integer.TYPE)};

    static class InetAddressHolder {
        int address;
        int family;
        String hostName;
        String originalHostName;

        InetAddressHolder() {
        }

        InetAddressHolder(String str, int i, int i2) {
            this.originalHostName = str;
            this.hostName = str;
            this.address = i;
            this.family = i2;
        }

        void init(String str, int i) {
            this.originalHostName = str;
            this.hostName = str;
            if (i != -1) {
                this.family = i;
            }
        }

        String getHostName() {
            return this.hostName;
        }

        String getOriginalHostName() {
            return this.originalHostName;
        }

        int getAddress() {
            return this.address;
        }

        int getFamily() {
            return this.family;
        }
    }

    InetAddressHolder holder() {
        return this.holder;
    }

    InetAddress() {
    }

    private Object readResolve() throws ObjectStreamException {
        return new Inet4Address(holder().getHostName(), holder().getAddress());
    }

    public boolean isMulticastAddress() {
        return false;
    }

    public boolean isAnyLocalAddress() {
        return false;
    }

    public boolean isLoopbackAddress() {
        return false;
    }

    public boolean isLinkLocalAddress() {
        return false;
    }

    public boolean isSiteLocalAddress() {
        return false;
    }

    public boolean isMCGlobal() {
        return false;
    }

    public boolean isMCNodeLocal() {
        return false;
    }

    public boolean isMCLinkLocal() {
        return false;
    }

    public boolean isMCSiteLocal() {
        return false;
    }

    public boolean isMCOrgLocal() {
        return false;
    }

    public boolean isReachable(int i) throws IOException {
        return isReachable(null, 0, i);
    }

    public boolean isReachable(NetworkInterface networkInterface, int i, int i2) throws IOException {
        if (i < 0) {
            throw new IllegalArgumentException("ttl can't be negative");
        }
        if (i2 < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        return impl.isReachable(this, i2, networkInterface, i);
    }

    public boolean isReachableByICMP(int i) throws IOException {
        return ((Inet6AddressImpl) impl).icmpEcho(this, i, null, 0);
    }

    public String getHostName() {
        if (holder().getHostName() == null) {
            holder().hostName = getHostFromNameService(this);
        }
        return holder().getHostName();
    }

    public String getCanonicalHostName() {
        if (this.canonicalHostName == null) {
            this.canonicalHostName = getHostFromNameService(this);
        }
        return this.canonicalHostName;
    }

    private static String getHostFromNameService(InetAddress inetAddress) {
        try {
            String hostByAddr = nameService.getHostByAddr(inetAddress.getAddress());
            boolean zEquals = false;
            InetAddress[] inetAddressArrLookupAllHostAddr = nameService.lookupAllHostAddr(hostByAddr, 0);
            if (inetAddressArrLookupAllHostAddr != null) {
                for (int i = 0; !zEquals && i < inetAddressArrLookupAllHostAddr.length; i++) {
                    zEquals = inetAddress.equals(inetAddressArrLookupAllHostAddr[i]);
                }
            }
            if (!zEquals) {
                return inetAddress.getHostAddress();
            }
            return hostByAddr;
        } catch (UnknownHostException e) {
            return inetAddress.getHostAddress();
        }
    }

    public byte[] getAddress() {
        return null;
    }

    public String getHostAddress() {
        return null;
    }

    public int hashCode() {
        return -1;
    }

    public boolean equals(Object obj) {
        return false;
    }

    public String toString() {
        String hostName = holder().getHostName();
        StringBuilder sb = new StringBuilder();
        if (hostName == null) {
            hostName = "";
        }
        sb.append(hostName);
        sb.append("/");
        sb.append(getHostAddress());
        return sb.toString();
    }

    public static InetAddress getByAddress(String str, byte[] bArr) throws UnknownHostException {
        return getByAddress(str, bArr, -1);
    }

    private static InetAddress getByAddress(String str, byte[] bArr, int i) throws UnknownHostException {
        if (str != null && str.length() > 0 && str.charAt(0) == '[' && str.charAt(str.length() - 1) == ']') {
            str = str.substring(1, str.length() - 1);
        }
        if (bArr != null) {
            if (bArr.length == 4) {
                return new Inet4Address(str, bArr);
            }
            if (bArr.length == 16) {
                byte[] bArrConvertFromIPv4MappedAddress = IPAddressUtil.convertFromIPv4MappedAddress(bArr);
                if (bArrConvertFromIPv4MappedAddress != null) {
                    return new Inet4Address(str, bArrConvertFromIPv4MappedAddress);
                }
                return new Inet6Address(str, bArr, i);
            }
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    public static InetAddress getByName(String str) throws UnknownHostException {
        return impl.lookupAllHostAddr(str, 0)[0];
    }

    public static InetAddress[] getAllByName(String str) throws UnknownHostException {
        return (InetAddress[]) impl.lookupAllHostAddr(str, 0).clone();
    }

    public static InetAddress getLoopbackAddress() {
        return impl.loopbackAddresses()[0];
    }

    public static InetAddress getByAddress(byte[] bArr) throws UnknownHostException {
        return getByAddress(null, bArr);
    }

    public static InetAddress getLocalHost() throws UnknownHostException {
        return impl.lookupAllHostAddr(Libcore.os.uname().nodename, 0)[0];
    }

    static InetAddress anyLocalAddress() {
        return impl.anyLocalAddress();
    }

    private void readObjectNoData(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        if (getClass().getClassLoader() != BOOT_CLASSLOADER) {
            throw new SecurityException("invalid address type");
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        if (getClass().getClassLoader() != BOOT_CLASSLOADER) {
            throw new SecurityException("invalid address type");
        }
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        this.holder = new InetAddressHolder((String) fields.get("hostName", (Object) null), fields.get("address", 0), fields.get("family", 0));
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        if (getClass().getClassLoader() != BOOT_CLASSLOADER) {
            throw new SecurityException("invalid address type");
        }
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("hostName", holder().hostName);
        putFieldPutFields.put("address", holder().address);
        putFieldPutFields.put("family", holder().family);
        objectOutputStream.writeFields();
        objectOutputStream.flush();
    }

    public static boolean isNumeric(String str) {
        InetAddress numericAddressNoThrow = parseNumericAddressNoThrow(str);
        return (numericAddressNoThrow == null || disallowDeprecatedFormats(str, numericAddressNoThrow) == null) ? false : true;
    }

    static InetAddress parseNumericAddressNoThrow(String str) {
        InetAddress[] inetAddressArrAndroid_getaddrinfo;
        if (str.startsWith("[") && str.endsWith("]") && str.indexOf(58) != -1) {
            str = str.substring(1, str.length() - 1);
        }
        StructAddrinfo structAddrinfo = new StructAddrinfo();
        structAddrinfo.ai_flags = OsConstants.AI_NUMERICHOST;
        try {
            inetAddressArrAndroid_getaddrinfo = Libcore.os.android_getaddrinfo(str, structAddrinfo, 0);
        } catch (GaiException e) {
            inetAddressArrAndroid_getaddrinfo = null;
        }
        if (inetAddressArrAndroid_getaddrinfo != null) {
            return inetAddressArrAndroid_getaddrinfo[0];
        }
        return null;
    }

    static InetAddress disallowDeprecatedFormats(String str, InetAddress inetAddress) {
        if (!(inetAddress instanceof Inet4Address) || str.indexOf(58) != -1) {
            return inetAddress;
        }
        return Libcore.os.inet_pton(OsConstants.AF_INET, str);
    }

    public static InetAddress parseNumericAddress(String str) {
        if (str == null || str.isEmpty()) {
            return Inet6Address.LOOPBACK;
        }
        InetAddress inetAddressDisallowDeprecatedFormats = disallowDeprecatedFormats(str, parseNumericAddressNoThrow(str));
        if (inetAddressDisallowDeprecatedFormats == null) {
            throw new IllegalArgumentException("Not a numeric address: " + str);
        }
        return inetAddressDisallowDeprecatedFormats;
    }

    public static void clearDnsCache() {
        impl.clearAddressCache();
    }

    public static InetAddress getByNameOnNet(String str, int i) throws UnknownHostException {
        return impl.lookupAllHostAddr(str, i)[0];
    }

    public static InetAddress[] getAllByNameOnNet(String str, int i) throws UnknownHostException {
        return (InetAddress[]) impl.lookupAllHostAddr(str, i).clone();
    }

    static InetAddress[] getAllByName0(String str, boolean z) throws UnknownHostException {
        throw new UnsupportedOperationException();
    }

    String getHostName(boolean z) {
        throw new UnsupportedOperationException();
    }
}
