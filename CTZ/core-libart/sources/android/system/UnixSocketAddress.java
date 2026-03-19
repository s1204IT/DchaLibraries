package android.system;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class UnixSocketAddress extends SocketAddress {
    private static final int NAMED_PATH_LENGTH = OsConstants.UNIX_PATH_MAX;
    private static final byte[] UNNAMED_PATH = new byte[0];
    private byte[] sun_path;

    private UnixSocketAddress(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("sun_path must not be null");
        }
        if (bArr.length > NAMED_PATH_LENGTH) {
            throw new IllegalArgumentException("sun_path exceeds the maximum length");
        }
        if (bArr.length == 0) {
            this.sun_path = UNNAMED_PATH;
        } else {
            this.sun_path = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.sun_path, 0, bArr.length);
        }
    }

    public static UnixSocketAddress createAbstract(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] bArr = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, bArr, 1, bytes.length);
        return new UnixSocketAddress(bArr);
    }

    public static UnixSocketAddress createFileSystem(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] bArr = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, bArr, 0, bytes.length);
        return new UnixSocketAddress(bArr);
    }

    public static UnixSocketAddress createUnnamed() {
        return new UnixSocketAddress(UNNAMED_PATH);
    }

    public byte[] getSunPath() {
        if (this.sun_path.length == 0) {
            return this.sun_path;
        }
        byte[] bArr = new byte[this.sun_path.length];
        System.arraycopy(this.sun_path, 0, bArr, 0, this.sun_path.length);
        return bArr;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(this.sun_path, ((UnixSocketAddress) obj).sun_path);
    }

    public int hashCode() {
        return Arrays.hashCode(this.sun_path);
    }

    public String toString() {
        return "UnixSocketAddress[sun_path=" + Arrays.toString(this.sun_path) + ']';
    }
}
