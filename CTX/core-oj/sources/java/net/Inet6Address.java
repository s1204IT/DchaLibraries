package java.net;

import android.system.OsConstants;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.Arrays;
import java.util.Enumeration;
import libcore.io.Libcore;
import sun.misc.Unsafe;
import sun.security.util.DerValue;

public final class Inet6Address extends InetAddress {
    private static final long FIELDS_OFFSET;
    static final int INADDRSZ = 16;
    private static final int INT16SZ = 2;
    private static final Unsafe UNSAFE;
    private static final long serialVersionUID = 6880410070516793377L;
    private final transient Inet6AddressHolder holder6;
    public static final InetAddress ANY = new Inet6Address("::", new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 0);
    public static final InetAddress LOOPBACK = new Inet6Address("ip6-localhost", new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}, 0);
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("ipaddress", byte[].class), new ObjectStreamField("scope_id", Integer.TYPE), new ObjectStreamField("scope_id_set", Boolean.TYPE), new ObjectStreamField("scope_ifname_set", Boolean.TYPE), new ObjectStreamField("ifname", String.class)};

    static {
        try {
            Unsafe unsafe = Unsafe.getUnsafe();
            FIELDS_OFFSET = unsafe.objectFieldOffset(Inet6Address.class.getDeclaredField("holder6"));
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private class Inet6AddressHolder {
        byte[] ipaddress;
        int scope_id;
        boolean scope_id_set;
        NetworkInterface scope_ifname;
        boolean scope_ifname_set;

        private Inet6AddressHolder() {
            this.ipaddress = new byte[16];
        }

        private Inet6AddressHolder(byte[] bArr, int i, boolean z, NetworkInterface networkInterface, boolean z2) {
            this.ipaddress = bArr;
            this.scope_id = i;
            this.scope_id_set = z;
            this.scope_ifname_set = z2;
            this.scope_ifname = networkInterface;
        }

        void setAddr(byte[] bArr) {
            if (bArr.length == 16) {
                System.arraycopy(bArr, 0, this.ipaddress, 0, 16);
            }
        }

        void init(byte[] bArr, int i) {
            setAddr(bArr);
            if (i > 0) {
                this.scope_id = i;
                this.scope_id_set = true;
            }
        }

        void init(byte[] bArr, NetworkInterface networkInterface) throws UnknownHostException {
            setAddr(bArr);
            if (networkInterface != null) {
                this.scope_id = Inet6Address.deriveNumericScope(this.ipaddress, networkInterface);
                this.scope_id_set = true;
                this.scope_ifname = networkInterface;
                this.scope_ifname_set = true;
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Inet6AddressHolder)) {
                return false;
            }
            return Arrays.equals(this.ipaddress, ((Inet6AddressHolder) obj).ipaddress);
        }

        public int hashCode() {
            int i;
            if (this.ipaddress == null) {
                return 0;
            }
            int i2 = 0;
            for (int i3 = 0; i3 < 16; i3 = i) {
                i = i3;
                int i4 = 0;
                int i5 = 0;
                while (i4 < 4 && i < 16) {
                    i5 = (i5 << 8) + this.ipaddress[i];
                    i4++;
                    i++;
                }
                i2 += i5;
            }
            return i2;
        }

        boolean isIPv4CompatibleAddress() {
            return this.ipaddress[0] == 0 && this.ipaddress[1] == 0 && this.ipaddress[2] == 0 && this.ipaddress[3] == 0 && this.ipaddress[4] == 0 && this.ipaddress[5] == 0 && this.ipaddress[6] == 0 && this.ipaddress[7] == 0 && this.ipaddress[8] == 0 && this.ipaddress[9] == 0 && this.ipaddress[10] == 0 && this.ipaddress[11] == 0;
        }

        boolean isMulticastAddress() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 255;
        }

        boolean isAnyLocalAddress() {
            byte b = 0;
            for (int i = 0; i < 16; i++) {
                b = (byte) (b | this.ipaddress[i]);
            }
            return b == 0;
        }

        boolean isLoopbackAddress() {
            byte b = 0;
            for (int i = 0; i < 15; i++) {
                b = (byte) (b | this.ipaddress[i]);
            }
            if (b != 0 || this.ipaddress[15] != 1) {
                return false;
            }
            return true;
        }

        boolean isLinkLocalAddress() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 254 && (this.ipaddress[1] & DerValue.TAG_PRIVATE) == 128;
        }

        boolean isSiteLocalAddress() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 254 && (this.ipaddress[1] & DerValue.TAG_PRIVATE) == 192;
        }

        boolean isMCGlobal() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 255 && (this.ipaddress[1] & 15) == 14;
        }

        boolean isMCNodeLocal() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 255 && (this.ipaddress[1] & 15) == 1;
        }

        boolean isMCLinkLocal() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 255 && (this.ipaddress[1] & 15) == 2;
        }

        boolean isMCSiteLocal() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 255 && (this.ipaddress[1] & 15) == 5;
        }

        boolean isMCOrgLocal() {
            return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 255 && (this.ipaddress[1] & 15) == 8;
        }
    }

    Inet6Address() {
        this.holder.init(null, OsConstants.AF_INET6);
        this.holder6 = new Inet6AddressHolder();
    }

    Inet6Address(String str, byte[] bArr, int i) {
        this.holder.init(str, OsConstants.AF_INET6);
        this.holder6 = new Inet6AddressHolder();
        this.holder6.init(bArr, i);
    }

    Inet6Address(String str, byte[] bArr) {
        this.holder6 = new Inet6AddressHolder();
        try {
            initif(str, bArr, null);
        } catch (UnknownHostException e) {
        }
    }

    Inet6Address(String str, byte[] bArr, NetworkInterface networkInterface) throws UnknownHostException {
        this.holder6 = new Inet6AddressHolder();
        initif(str, bArr, networkInterface);
    }

    Inet6Address(String str, byte[] bArr, String str2) throws UnknownHostException {
        this.holder6 = new Inet6AddressHolder();
        initstr(str, bArr, str2);
    }

    public static Inet6Address getByAddress(String str, byte[] bArr, NetworkInterface networkInterface) throws UnknownHostException {
        if (str != null && str.length() > 0 && str.charAt(0) == '[' && str.charAt(str.length() - 1) == ']') {
            str = str.substring(1, str.length() - 1);
        }
        if (bArr != null && bArr.length == 16) {
            return new Inet6Address(str, bArr, networkInterface);
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    public static Inet6Address getByAddress(String str, byte[] bArr, int i) throws UnknownHostException {
        if (str != null && str.length() > 0 && str.charAt(0) == '[' && str.charAt(str.length() - 1) == ']') {
            str = str.substring(1, str.length() - 1);
        }
        if (bArr != null && bArr.length == 16) {
            return new Inet6Address(str, bArr, i);
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    private void initstr(String str, byte[] bArr, String str2) throws UnknownHostException {
        try {
            NetworkInterface byName = NetworkInterface.getByName(str2);
            if (byName == null) {
                throw new UnknownHostException("no such interface " + str2);
            }
            initif(str, bArr, byName);
        } catch (SocketException e) {
            throw new UnknownHostException("SocketException thrown" + str2);
        }
    }

    private void initif(String str, byte[] bArr, NetworkInterface networkInterface) throws UnknownHostException {
        int i;
        this.holder6.init(bArr, networkInterface);
        if (bArr.length == 16) {
            i = OsConstants.AF_INET6;
        } else {
            i = -1;
        }
        this.holder.init(str, i);
    }

    private static boolean isDifferentLocalAddressType(byte[] bArr, byte[] bArr2) {
        if (!isLinkLocalAddress(bArr) || isLinkLocalAddress(bArr2)) {
            return !isSiteLocalAddress(bArr) || isSiteLocalAddress(bArr2);
        }
        return false;
    }

    private static int deriveNumericScope(byte[] bArr, NetworkInterface networkInterface) throws UnknownHostException {
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddressNextElement = inetAddresses.nextElement();
            if (inetAddressNextElement instanceof Inet6Address) {
                Inet6Address inet6Address = (Inet6Address) inetAddressNextElement;
                if (isDifferentLocalAddressType(bArr, inet6Address.getAddress())) {
                    return inet6Address.getScopeId();
                }
            }
        }
        throw new UnknownHostException("no scope_id found");
    }

    private int deriveNumericScope(String str) throws UnknownHostException {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterfaceNextElement = networkInterfaces.nextElement();
                if (networkInterfaceNextElement.getName().equals(str)) {
                    return deriveNumericScope(this.holder6.ipaddress, networkInterfaceNextElement);
                }
            }
            throw new UnknownHostException("No matching address found for interface : " + str);
        } catch (SocketException e) {
            throw new UnknownHostException("could not enumerate local network interfaces");
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        NetworkInterface networkInterface;
        if (getClass().getClassLoader() != Class.class.getClassLoader()) {
            throw new SecurityException("invalid address type");
        }
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        NetworkInterface networkInterface2 = null;
        byte[] bArr = (byte[]) fields.get("ipaddress", (Object) null);
        int iDeriveNumericScope = fields.get("scope_id", -1);
        boolean z = fields.get("scope_id_set", false);
        boolean z2 = fields.get("scope_ifname_set", false);
        String str = (String) fields.get("ifname", (Object) null);
        if (str == null || "".equals(str)) {
            networkInterface = networkInterface2;
        } else {
            try {
                NetworkInterface byName = NetworkInterface.getByName(str);
                if (byName == null) {
                    iDeriveNumericScope = 0;
                    z = false;
                    z2 = false;
                } else {
                    try {
                        iDeriveNumericScope = deriveNumericScope(bArr, byName);
                    } catch (SocketException e) {
                        networkInterface2 = byName;
                        z2 = true;
                    } catch (UnknownHostException e2) {
                    }
                    z2 = true;
                }
                networkInterface = byName;
            } catch (SocketException e3) {
            }
        }
        int i = iDeriveNumericScope;
        boolean z3 = z;
        boolean z4 = z2;
        byte[] bArr2 = (byte[]) bArr.clone();
        if (bArr2.length != 16) {
            throw new InvalidObjectException("invalid address length: " + bArr2.length);
        }
        if (holder().getFamily() != OsConstants.AF_INET6) {
            throw new InvalidObjectException("invalid address family type");
        }
        UNSAFE.putObject(this, FIELDS_OFFSET, new Inet6AddressHolder(bArr2, i, z3, networkInterface, z4));
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        String name = null;
        if (this.holder6.scope_ifname != null) {
            name = this.holder6.scope_ifname.getName();
            this.holder6.scope_ifname_set = true;
        }
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("ipaddress", this.holder6.ipaddress);
        putFieldPutFields.put("scope_id", this.holder6.scope_id);
        putFieldPutFields.put("scope_id_set", this.holder6.scope_id_set);
        putFieldPutFields.put("scope_ifname_set", this.holder6.scope_ifname_set);
        putFieldPutFields.put("ifname", name);
        objectOutputStream.writeFields();
    }

    @Override
    public boolean isMulticastAddress() {
        return this.holder6.isMulticastAddress();
    }

    @Override
    public boolean isAnyLocalAddress() {
        return this.holder6.isAnyLocalAddress();
    }

    @Override
    public boolean isLoopbackAddress() {
        return this.holder6.isLoopbackAddress();
    }

    @Override
    public boolean isLinkLocalAddress() {
        return this.holder6.isLinkLocalAddress();
    }

    static boolean isLinkLocalAddress(byte[] bArr) {
        return (bArr[0] & Character.DIRECTIONALITY_UNDEFINED) == 254 && (bArr[1] & DerValue.TAG_PRIVATE) == 128;
    }

    @Override
    public boolean isSiteLocalAddress() {
        return this.holder6.isSiteLocalAddress();
    }

    static boolean isSiteLocalAddress(byte[] bArr) {
        return (bArr[0] & Character.DIRECTIONALITY_UNDEFINED) == 254 && (bArr[1] & DerValue.TAG_PRIVATE) == 192;
    }

    @Override
    public boolean isMCGlobal() {
        return this.holder6.isMCGlobal();
    }

    @Override
    public boolean isMCNodeLocal() {
        return this.holder6.isMCNodeLocal();
    }

    @Override
    public boolean isMCLinkLocal() {
        return this.holder6.isMCLinkLocal();
    }

    @Override
    public boolean isMCSiteLocal() {
        return this.holder6.isMCSiteLocal();
    }

    @Override
    public boolean isMCOrgLocal() {
        return this.holder6.isMCOrgLocal();
    }

    @Override
    public byte[] getAddress() {
        return (byte[]) this.holder6.ipaddress.clone();
    }

    public int getScopeId() {
        return this.holder6.scope_id;
    }

    public NetworkInterface getScopedInterface() {
        return this.holder6.scope_ifname;
    }

    @Override
    public String getHostAddress() {
        return Libcore.os.getnameinfo(this, OsConstants.NI_NUMERICHOST);
    }

    @Override
    public int hashCode() {
        return this.holder6.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Inet6Address)) {
            return false;
        }
        return this.holder6.equals(((Inet6Address) obj).holder6);
    }

    public boolean isIPv4CompatibleAddress() {
        return this.holder6.isIPv4CompatibleAddress();
    }

    static String numericToTextFormat(byte[] bArr) {
        StringBuilder sb = new StringBuilder(39);
        for (int i = 0; i < 8; i++) {
            int i2 = i << 1;
            sb.append(Integer.toHexString(((bArr[i2] << 8) & 65280) | (bArr[i2 + 1] & Character.DIRECTIONALITY_UNDEFINED)));
            if (i < 7) {
                sb.append(":");
            }
        }
        return sb.toString();
    }
}
