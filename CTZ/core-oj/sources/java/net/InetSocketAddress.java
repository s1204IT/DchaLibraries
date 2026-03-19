package java.net;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import sun.misc.Unsafe;

public class InetSocketAddress extends SocketAddress {
    private static final long FIELDS_OFFSET;
    private static final Unsafe UNSAFE;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("hostname", String.class), new ObjectStreamField("addr", InetAddress.class), new ObjectStreamField("port", Integer.TYPE)};
    private static final long serialVersionUID = 5076001401234631237L;
    private final transient InetSocketAddressHolder holder;

    private static class InetSocketAddressHolder {
        private InetAddress addr;
        private String hostname;
        private int port;

        private InetSocketAddressHolder(String str, InetAddress inetAddress, int i) {
            this.hostname = str;
            this.addr = inetAddress;
            this.port = i;
        }

        private int getPort() {
            return this.port;
        }

        private InetAddress getAddress() {
            return this.addr;
        }

        private String getHostName() {
            if (this.hostname != null) {
                return this.hostname;
            }
            if (this.addr != null) {
                return this.addr.getHostName();
            }
            return null;
        }

        private String getHostString() {
            if (this.hostname != null) {
                return this.hostname;
            }
            if (this.addr != null) {
                if (this.addr.holder().getHostName() != null) {
                    return this.addr.holder().getHostName();
                }
                return this.addr.getHostAddress();
            }
            return null;
        }

        private boolean isUnresolved() {
            return this.addr == null;
        }

        public String toString() {
            if (isUnresolved()) {
                return this.hostname + ":" + this.port;
            }
            return this.addr.toString() + ":" + this.port;
        }

        public final boolean equals(Object obj) {
            boolean zEquals;
            if (obj == null || !(obj instanceof InetSocketAddressHolder)) {
                return false;
            }
            InetSocketAddressHolder inetSocketAddressHolder = (InetSocketAddressHolder) obj;
            if (this.addr != null) {
                zEquals = this.addr.equals(inetSocketAddressHolder.addr);
            } else {
                zEquals = this.hostname == null ? inetSocketAddressHolder.addr == null && inetSocketAddressHolder.hostname == null : inetSocketAddressHolder.addr == null && this.hostname.equalsIgnoreCase(inetSocketAddressHolder.hostname);
            }
            return zEquals && this.port == inetSocketAddressHolder.port;
        }

        public final int hashCode() {
            if (this.addr != null) {
                return this.addr.hashCode() + this.port;
            }
            if (this.hostname != null) {
                return this.hostname.toLowerCase().hashCode() + this.port;
            }
            return this.port;
        }
    }

    private static int checkPort(int i) {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("port out of range:" + i);
        }
        return i;
    }

    private static String checkHost(String str) {
        if (str == null) {
            throw new IllegalArgumentException("hostname can't be null");
        }
        return str;
    }

    public InetSocketAddress() {
        this.holder = new InetSocketAddressHolder(null, 0 == true ? 1 : 0, 0);
    }

    public InetSocketAddress(int i) {
        this((InetAddress) null, i);
    }

    public InetSocketAddress(InetAddress inetAddress, int i) {
        this.holder = new InetSocketAddressHolder(null, inetAddress == null ? Inet6Address.ANY : inetAddress, checkPort(i));
    }

    public InetSocketAddress(String str, int i) {
        InetAddress byName;
        checkHost(str);
        try {
            byName = InetAddress.getByName(str);
            str = null;
        } catch (UnknownHostException e) {
            byName = null;
        }
        this.holder = new InetSocketAddressHolder(str, byName, checkPort(i));
    }

    private InetSocketAddress(int i, String str) {
        this.holder = new InetSocketAddressHolder(str, null, i);
    }

    public static InetSocketAddress createUnresolved(String str, int i) {
        return new InetSocketAddress(checkPort(i), checkHost(str));
    }

    static {
        try {
            Unsafe unsafe = Unsafe.getUnsafe();
            FIELDS_OFFSET = unsafe.objectFieldOffset(InetSocketAddress.class.getDeclaredField("holder"));
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("hostname", this.holder.hostname);
        putFieldPutFields.put("addr", this.holder.addr);
        putFieldPutFields.put("port", this.holder.port);
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        String str = (String) fields.get("hostname", (Object) null);
        InetAddress inetAddress = (InetAddress) fields.get("addr", (Object) null);
        int i = fields.get("port", -1);
        checkPort(i);
        if (str == null && inetAddress == null) {
            throw new InvalidObjectException("hostname and addr can't both be null");
        }
        UNSAFE.putObject(this, FIELDS_OFFSET, new InetSocketAddressHolder(str, inetAddress, i));
    }

    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Stream data required");
    }

    public final int getPort() {
        return this.holder.getPort();
    }

    public final InetAddress getAddress() {
        return this.holder.getAddress();
    }

    public final String getHostName() {
        return this.holder.getHostName();
    }

    public final String getHostString() {
        return this.holder.getHostString();
    }

    public final boolean isUnresolved() {
        return this.holder.isUnresolved();
    }

    public String toString() {
        return this.holder.toString();
    }

    public final boolean equals(Object obj) {
        if (obj == null || !(obj instanceof InetSocketAddress)) {
            return false;
        }
        return this.holder.equals(((InetSocketAddress) obj).holder);
    }

    public final int hashCode() {
        return this.holder.hashCode();
    }
}
