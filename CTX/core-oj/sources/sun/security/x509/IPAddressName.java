package sun.security.x509;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import sun.misc.HexDumpEncoder;
import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class IPAddressName implements GeneralNameInterface {
    private static final int MASKSIZE = 16;
    private byte[] address;
    private boolean isIPv4;
    private String name;

    public IPAddressName(DerValue derValue) throws IOException {
        this(derValue.getOctetString());
    }

    public IPAddressName(byte[] bArr) throws IOException {
        if (bArr.length == 4 || bArr.length == 8) {
            this.isIPv4 = true;
        } else if (bArr.length == 16 || bArr.length == 32) {
            this.isIPv4 = false;
        } else {
            throw new IOException("Invalid IPAddressName");
        }
        this.address = bArr;
    }

    public IPAddressName(String str) throws IOException {
        if (str == null || str.length() == 0) {
            throw new IOException("IPAddress cannot be null or empty");
        }
        if (str.charAt(str.length() - 1) == '/') {
            throw new IOException("Invalid IPAddress: " + str);
        }
        if (str.indexOf(58) >= 0) {
            parseIPv6(str);
            this.isIPv4 = false;
        } else if (str.indexOf(46) >= 0) {
            parseIPv4(str);
            this.isIPv4 = true;
        } else {
            throw new IOException("Invalid IPAddress: " + str);
        }
    }

    private void parseIPv4(String str) throws IOException {
        int iIndexOf = str.indexOf(47);
        if (iIndexOf == -1) {
            this.address = InetAddress.getByName(str).getAddress();
            return;
        }
        this.address = new byte[8];
        byte[] address = InetAddress.getByName(str.substring(iIndexOf + 1)).getAddress();
        System.arraycopy(InetAddress.getByName(str.substring(0, iIndexOf)).getAddress(), 0, this.address, 0, 4);
        System.arraycopy(address, 0, this.address, 4, 4);
    }

    private void parseIPv6(String str) throws IOException {
        int iIndexOf = str.indexOf(47);
        if (iIndexOf == -1) {
            this.address = InetAddress.getByName(str).getAddress();
            return;
        }
        this.address = new byte[32];
        System.arraycopy(InetAddress.getByName(str.substring(0, iIndexOf)).getAddress(), 0, this.address, 0, 16);
        int i = Integer.parseInt(str.substring(iIndexOf + 1));
        if (i < 0 || i > 128) {
            throw new IOException("IPv6Address prefix length (" + i + ") in out of valid range [0,128]");
        }
        BitArray bitArray = new BitArray(128);
        for (int i2 = 0; i2 < i; i2++) {
            bitArray.set(i2, true);
        }
        byte[] byteArray = bitArray.toByteArray();
        for (int i3 = 0; i3 < 16; i3++) {
            this.address[16 + i3] = byteArray[i3];
        }
    }

    @Override
    public int getType() {
        return 7;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putOctetString(this.address);
    }

    public String toString() {
        try {
            return "IPAddress: " + getName();
        } catch (IOException e) {
            return "IPAddress: " + new HexDumpEncoder().encodeBuffer(this.address);
        }
    }

    public String getName() throws IOException {
        if (this.name != null) {
            return this.name;
        }
        int i = 0;
        if (this.isIPv4) {
            byte[] bArr = new byte[4];
            System.arraycopy(this.address, 0, bArr, 0, 4);
            this.name = InetAddress.getByAddress(bArr).getHostAddress();
            if (this.address.length == 8) {
                byte[] bArr2 = new byte[4];
                System.arraycopy(this.address, 4, bArr2, 0, 4);
                this.name += "/" + InetAddress.getByAddress(bArr2).getHostAddress();
            }
        } else {
            byte[] bArr3 = new byte[16];
            System.arraycopy(this.address, 0, bArr3, 0, 16);
            this.name = InetAddress.getByAddress(bArr3).getHostAddress();
            if (this.address.length == 32) {
                byte[] bArr4 = new byte[16];
                for (int i2 = 16; i2 < 32; i2++) {
                    bArr4[i2 - 16] = this.address[i2];
                }
                BitArray bitArray = new BitArray(128, bArr4);
                while (i < 128 && bitArray.get(i)) {
                    i++;
                }
                this.name += "/" + i;
                while (i < 128) {
                    if (!bitArray.get(i)) {
                        i++;
                    } else {
                        throw new IOException("Invalid IPv6 subdomain - set bit " + i + " not contiguous");
                    }
                }
            }
        }
        return this.name;
    }

    public byte[] getBytes() {
        return (byte[]) this.address.clone();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IPAddressName)) {
            return false;
        }
        byte[] bArr = ((IPAddressName) obj).address;
        if (bArr.length != this.address.length) {
            return false;
        }
        if (this.address.length == 8 || this.address.length == 32) {
            int length = this.address.length / 2;
            for (int i = 0; i < length; i++) {
                int i2 = i + length;
                if (((byte) (this.address[i] & this.address[i2])) != ((byte) (bArr[i] & bArr[i2]))) {
                    return false;
                }
            }
            while (length < this.address.length) {
                if (this.address[length] != bArr[length]) {
                    return false;
                }
                length++;
            }
            return true;
        }
        return Arrays.equals(bArr, this.address);
    }

    public int hashCode() {
        int i = 0;
        for (int i2 = 0; i2 < this.address.length; i2++) {
            i += this.address[i2] * i2;
        }
        return i;
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        int i = 0;
        if (generalNameInterface == null || generalNameInterface.getType() != 7) {
            return -1;
        }
        IPAddressName iPAddressName = (IPAddressName) generalNameInterface;
        if (iPAddressName.equals(this)) {
            return 0;
        }
        byte[] bArr = iPAddressName.address;
        if (bArr.length != 4 || this.address.length != 4) {
            if ((bArr.length == 8 && this.address.length == 8) || (bArr.length == 32 && this.address.length == 32)) {
                int length = this.address.length / 2;
                boolean z = false;
                boolean z2 = false;
                boolean z3 = true;
                boolean z4 = true;
                for (int i2 = 0; i2 < length; i2++) {
                    int i3 = i2 + length;
                    if (((byte) (this.address[i2] & this.address[i3])) != this.address[i2]) {
                        z = true;
                    }
                    if (((byte) (bArr[i2] & bArr[i3])) != bArr[i2]) {
                        z2 = true;
                    }
                    if (((byte) (this.address[i3] & bArr[i3])) != this.address[i3] || ((byte) (this.address[i2] & this.address[i3])) != ((byte) (bArr[i2] & this.address[i3]))) {
                        z3 = false;
                    }
                    if (((byte) (bArr[i3] & this.address[i3])) != bArr[i3] || ((byte) (bArr[i2] & bArr[i3])) != ((byte) (this.address[i2] & bArr[i3]))) {
                        z4 = false;
                    }
                }
                if (!z && !z2) {
                    if (!z3) {
                        if (!z4) {
                            return 3;
                        }
                        return 2;
                    }
                    return 1;
                }
                if (z && z2) {
                    return 0;
                }
            } else {
                if (bArr.length == 8 || bArr.length == 32) {
                    int length2 = bArr.length / 2;
                    while (i < length2 && (this.address[i] & bArr[i + length2]) == bArr[i]) {
                        i++;
                    }
                    return i == length2 ? 2 : 3;
                }
                if (this.address.length == 8 || this.address.length == 32) {
                    int length3 = this.address.length / 2;
                    while (i < length3 && (bArr[i] & this.address[i + length3]) == this.address[i]) {
                        i++;
                    }
                    return i == length3 ? 1 : 3;
                }
            }
        }
        return 3;
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("subtreeDepth() not defined for IPAddressName");
    }
}
