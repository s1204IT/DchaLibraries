package sun.security.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

public final class ObjectIdentifier implements Serializable {
    static final boolean $assertionsDisabled = false;
    private static final long serialVersionUID = 8697030238860181294L;
    private int componentLen;
    private Object components;
    private transient boolean componentsCalculated;
    private byte[] encoding;
    private volatile transient String stringForm;

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.encoding == null) {
            init((int[]) this.components, this.componentLen);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        if (!this.componentsCalculated) {
            int[] intArray = toIntArray();
            if (intArray != null) {
                this.components = intArray;
                this.componentLen = intArray.length;
            } else {
                this.components = HugeOidNotSupportedByOldJDK.theOne;
            }
            this.componentsCalculated = true;
        }
        objectOutputStream.defaultWriteObject();
    }

    static class HugeOidNotSupportedByOldJDK implements Serializable {
        private static final long serialVersionUID = 1;
        static HugeOidNotSupportedByOldJDK theOne = new HugeOidNotSupportedByOldJDK();

        HugeOidNotSupportedByOldJDK() {
        }
    }

    public ObjectIdentifier(String str) throws IOException {
        int iIndexOf;
        String strSubstring;
        int length;
        this.encoding = null;
        this.components = null;
        this.componentLen = -1;
        this.componentsCalculated = $assertionsDisabled;
        byte[] bArr = new byte[str.length()];
        int i = 0;
        int i2 = 0;
        int iIntValue = 0;
        int iPack7Oid = 0;
        do {
            try {
                iIndexOf = str.indexOf(46, i);
                if (iIndexOf == -1) {
                    strSubstring = str.substring(i);
                    length = str.length() - i;
                } else {
                    strSubstring = str.substring(i, iIndexOf);
                    length = iIndexOf - i;
                }
                if (length > 9) {
                    BigInteger bigInteger = new BigInteger(strSubstring);
                    if (i2 == 0) {
                        checkFirstComponent(bigInteger);
                        iIntValue = bigInteger.intValue();
                    } else {
                        if (i2 == 1) {
                            checkSecondComponent(iIntValue, bigInteger);
                            bigInteger = bigInteger.add(BigInteger.valueOf(40 * iIntValue));
                        } else {
                            checkOtherComponent(i2, bigInteger);
                        }
                        iPack7Oid += pack7Oid(bigInteger, bArr, iPack7Oid);
                    }
                } else {
                    int i3 = Integer.parseInt(strSubstring);
                    if (i2 == 0) {
                        checkFirstComponent(i3);
                        iIntValue = i3;
                    } else {
                        if (i2 == 1) {
                            checkSecondComponent(iIntValue, i3);
                            i3 += 40 * iIntValue;
                        } else {
                            checkOtherComponent(i2, i3);
                        }
                        iPack7Oid += pack7Oid(i3, bArr, iPack7Oid);
                    }
                }
                i = iIndexOf + 1;
                i2++;
            } catch (IOException e) {
                throw e;
            } catch (Exception e2) {
                throw new IOException("ObjectIdentifier() -- Invalid format: " + e2.toString(), e2);
            }
        } while (iIndexOf != -1);
        checkCount(i2);
        this.encoding = new byte[iPack7Oid];
        System.arraycopy(bArr, 0, this.encoding, 0, iPack7Oid);
        this.stringForm = str;
    }

    public ObjectIdentifier(int[] iArr) throws IOException {
        this.encoding = null;
        this.components = null;
        this.componentLen = -1;
        this.componentsCalculated = $assertionsDisabled;
        checkCount(iArr.length);
        checkFirstComponent(iArr[0]);
        checkSecondComponent(iArr[0], iArr[1]);
        for (int i = 2; i < iArr.length; i++) {
            checkOtherComponent(i, iArr[i]);
        }
        init(iArr, iArr.length);
    }

    public ObjectIdentifier(DerInputStream derInputStream) throws IOException {
        this.encoding = null;
        this.components = null;
        this.componentLen = -1;
        this.componentsCalculated = $assertionsDisabled;
        byte b = (byte) derInputStream.getByte();
        if (b != 6) {
            throw new IOException("ObjectIdentifier() -- data isn't an object ID (tag = " + ((int) b) + ")");
        }
        int length = derInputStream.getLength();
        if (length > derInputStream.available()) {
            throw new IOException("ObjectIdentifier() -- length exceedsdata available.  Length: " + length + ", Available: " + derInputStream.available());
        }
        this.encoding = new byte[length];
        derInputStream.getBytes(this.encoding);
        check(this.encoding);
    }

    ObjectIdentifier(DerInputBuffer derInputBuffer) throws IOException {
        this.encoding = null;
        this.components = null;
        this.componentLen = -1;
        this.componentsCalculated = $assertionsDisabled;
        DerInputStream derInputStream = new DerInputStream(derInputBuffer);
        this.encoding = new byte[derInputStream.available()];
        derInputStream.getBytes(this.encoding);
        check(this.encoding);
    }

    private void init(int[] iArr, int i) {
        int iPack7Oid;
        byte[] bArr = new byte[(i * 5) + 1];
        if (iArr[1] < Integer.MAX_VALUE - (iArr[0] * 40)) {
            iPack7Oid = pack7Oid((iArr[0] * 40) + iArr[1], bArr, 0) + 0;
        } else {
            iPack7Oid = pack7Oid(BigInteger.valueOf(iArr[1]).add(BigInteger.valueOf(iArr[0] * 40)), bArr, 0) + 0;
        }
        for (int i2 = 2; i2 < i; i2++) {
            iPack7Oid += pack7Oid(iArr[i2], bArr, iPack7Oid);
        }
        this.encoding = new byte[iPack7Oid];
        System.arraycopy(bArr, 0, this.encoding, 0, iPack7Oid);
    }

    public static ObjectIdentifier newInternal(int[] iArr) {
        try {
            return new ObjectIdentifier(iArr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.write((byte) 6, this.encoding);
    }

    @Deprecated
    public boolean equals(ObjectIdentifier objectIdentifier) {
        return equals((Object) objectIdentifier);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ObjectIdentifier)) {
            return $assertionsDisabled;
        }
        return Arrays.equals(this.encoding, ((ObjectIdentifier) obj).encoding);
    }

    public int hashCode() {
        return Arrays.hashCode(this.encoding);
    }

    public int[] toIntArray() {
        int i;
        int length = this.encoding.length;
        int[] iArrCopyOf = new int[20];
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < length; i4++) {
            if ((this.encoding[i4] & 128) == 0) {
                int i5 = (i4 - i3) + 1;
                if (i5 > 4) {
                    BigInteger bigInteger = new BigInteger(pack(this.encoding, i3, i5, 7, 8));
                    if (i3 == 0) {
                        int i6 = i2 + 1;
                        iArrCopyOf[i2] = 2;
                        BigInteger bigIntegerSubtract = bigInteger.subtract(BigInteger.valueOf(80L));
                        if (bigIntegerSubtract.compareTo(BigInteger.valueOf(2147483647L)) == 1) {
                            return null;
                        }
                        i = i6 + 1;
                        iArrCopyOf[i6] = bigIntegerSubtract.intValue();
                    } else {
                        if (bigInteger.compareTo(BigInteger.valueOf(2147483647L)) == 1) {
                            return null;
                        }
                        i = i2 + 1;
                        iArrCopyOf[i2] = bigInteger.intValue();
                    }
                } else {
                    int i7 = 0;
                    for (int i8 = i3; i8 <= i4; i8++) {
                        i7 = (i7 << 7) | (this.encoding[i8] & Byte.MAX_VALUE);
                    }
                    if (i3 == 0) {
                        if (i7 < 80) {
                            int i9 = i2 + 1;
                            iArrCopyOf[i2] = i7 / 40;
                            i = i9 + 1;
                            iArrCopyOf[i9] = i7 % 40;
                        } else {
                            int i10 = i2 + 1;
                            iArrCopyOf[i2] = 2;
                            i = i10 + 1;
                            iArrCopyOf[i10] = i7 - 80;
                        }
                    } else {
                        i = i2 + 1;
                        iArrCopyOf[i2] = i7;
                    }
                }
                i3 = i4 + 1;
                i2 = i;
            }
            if (i2 >= iArrCopyOf.length) {
                iArrCopyOf = Arrays.copyOf(iArrCopyOf, i2 + 10);
            }
        }
        return Arrays.copyOf(iArrCopyOf, i2);
    }

    public String toString() {
        String str = this.stringForm;
        if (str == null) {
            int length = this.encoding.length;
            StringBuffer stringBuffer = new StringBuffer(length * 4);
            int i = 0;
            for (int i2 = 0; i2 < length; i2++) {
                if ((this.encoding[i2] & 128) == 0) {
                    if (i != 0) {
                        stringBuffer.append('.');
                    }
                    int i3 = (i2 - i) + 1;
                    if (i3 > 4) {
                        BigInteger bigInteger = new BigInteger(pack(this.encoding, i, i3, 7, 8));
                        if (i == 0) {
                            stringBuffer.append("2.");
                            stringBuffer.append((Object) bigInteger.subtract(BigInteger.valueOf(80L)));
                        } else {
                            stringBuffer.append((Object) bigInteger);
                        }
                    } else {
                        int i4 = 0;
                        for (int i5 = i; i5 <= i2; i5++) {
                            i4 = (i4 << 7) | (this.encoding[i5] & Byte.MAX_VALUE);
                        }
                        if (i == 0) {
                            if (i4 < 80) {
                                stringBuffer.append(i4 / 40);
                                stringBuffer.append('.');
                                stringBuffer.append(i4 % 40);
                            } else {
                                stringBuffer.append("2.");
                                stringBuffer.append(i4 - 80);
                            }
                        } else {
                            stringBuffer.append(i4);
                        }
                    }
                    i = i2 + 1;
                }
            }
            String string = stringBuffer.toString();
            this.stringForm = string;
            return string;
        }
        return str;
    }

    private static byte[] pack(byte[] bArr, int i, int i2, int i3, int i4) {
        if (i3 == i4) {
            return (byte[]) bArr.clone();
        }
        int i5 = i2 * i3;
        int i6 = ((i5 + i4) - 1) / i4;
        byte[] bArr2 = new byte[i6];
        int i7 = 0;
        int i8 = (i6 * i4) - i5;
        while (i7 < i5) {
            int i9 = i3 - (i7 % i3);
            int i10 = i4 - (i8 % i4);
            int i11 = i9 > i10 ? i10 : i9;
            int i12 = i8 / i4;
            bArr2[i12] = (byte) (((((bArr[(i7 / i3) + i] + 256) >> (i9 - i11)) & ((1 << i11) - 1)) << (i10 - i11)) | bArr2[i12]);
            i7 += i11;
            i8 += i11;
        }
        return bArr2;
    }

    private static int pack7Oid(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        byte[] bArrPack = pack(bArr, i, i2, 8, 7);
        int length = bArrPack.length - 1;
        for (int length2 = bArrPack.length - 2; length2 >= 0; length2--) {
            if (bArrPack[length2] != 0) {
                length = length2;
            }
            bArrPack[length2] = (byte) (bArrPack[length2] | 128);
        }
        System.arraycopy(bArrPack, length, bArr2, i3, bArrPack.length - length);
        return bArrPack.length - length;
    }

    private static int pack8(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        byte[] bArrPack = pack(bArr, i, i2, 7, 8);
        int length = bArrPack.length - 1;
        for (int length2 = bArrPack.length - 2; length2 >= 0; length2--) {
            if (bArrPack[length2] != 0) {
                length = length2;
            }
        }
        System.arraycopy(bArrPack, length, bArr2, i3, bArrPack.length - length);
        return bArrPack.length - length;
    }

    private static int pack7Oid(int i, byte[] bArr, int i2) {
        return pack7Oid(new byte[]{(byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i}, 0, 4, bArr, i2);
    }

    private static int pack7Oid(BigInteger bigInteger, byte[] bArr, int i) {
        byte[] byteArray = bigInteger.toByteArray();
        return pack7Oid(byteArray, 0, byteArray.length, bArr, i);
    }

    private static void check(byte[] bArr) throws IOException {
        int length = bArr.length;
        if (length < 1 || (bArr[length - 1] & 128) != 0) {
            throw new IOException("ObjectIdentifier() -- Invalid DER encoding, not ended");
        }
        for (int i = 0; i < length; i++) {
            if (bArr[i] == -128 && (i == 0 || (bArr[i - 1] & 128) == 0)) {
                throw new IOException("ObjectIdentifier() -- Invalid DER encoding, useless extra octet detected");
            }
        }
    }

    private static void checkCount(int i) throws IOException {
        if (i < 2) {
            throw new IOException("ObjectIdentifier() -- Must be at least two oid components ");
        }
    }

    private static void checkFirstComponent(int i) throws IOException {
        if (i < 0 || i > 2) {
            throw new IOException("ObjectIdentifier() -- First oid component is invalid ");
        }
    }

    private static void checkFirstComponent(BigInteger bigInteger) throws IOException {
        if (bigInteger.signum() == -1 || bigInteger.compareTo(BigInteger.valueOf(2L)) == 1) {
            throw new IOException("ObjectIdentifier() -- First oid component is invalid ");
        }
    }

    private static void checkSecondComponent(int i, int i2) throws IOException {
        if (i2 < 0 || (i != 2 && i2 > 39)) {
            throw new IOException("ObjectIdentifier() -- Second oid component is invalid ");
        }
    }

    private static void checkSecondComponent(int i, BigInteger bigInteger) throws IOException {
        if (bigInteger.signum() == -1 || (i != 2 && bigInteger.compareTo(BigInteger.valueOf(39L)) == 1)) {
            throw new IOException("ObjectIdentifier() -- Second oid component is invalid ");
        }
    }

    private static void checkOtherComponent(int i, int i2) throws IOException {
        if (i2 < 0) {
            throw new IOException("ObjectIdentifier() -- oid component #" + (i + 1) + " must be non-negative ");
        }
    }

    private static void checkOtherComponent(int i, BigInteger bigInteger) throws IOException {
        if (bigInteger.signum() == -1) {
            throw new IOException("ObjectIdentifier() -- oid component #" + (i + 1) + " must be non-negative ");
        }
    }
}
