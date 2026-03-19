package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.io.Streams;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public abstract class ASN1BitString extends ASN1Primitive implements ASN1String {
    private static final char[] table = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    protected final byte[] data;
    protected final int padBits;

    @Override
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    protected static int getPadBits(int i) {
        int i2;
        int i3 = 3;
        while (true) {
            if (i3 >= 0) {
                if (i3 != 0) {
                    int i4 = i >> (i3 * 8);
                    if (i4 == 0) {
                        i3--;
                    } else {
                        i2 = i4 & 255;
                        break;
                    }
                } else if (i == 0) {
                    i3--;
                } else {
                    i2 = i & 255;
                    break;
                }
            } else {
                i2 = 0;
                break;
            }
        }
        if (i2 == 0) {
            return 0;
        }
        int i5 = 1;
        while (true) {
            i2 <<= 1;
            if ((i2 & 255) != 0) {
                i5++;
            } else {
                return 8 - i5;
            }
        }
    }

    protected static byte[] getBytes(int i) {
        if (i == 0) {
            return new byte[0];
        }
        int i2 = 4;
        for (int i3 = 3; i3 >= 1 && ((255 << (i3 * 8)) & i) == 0; i3--) {
            i2--;
        }
        byte[] bArr = new byte[i2];
        for (int i4 = 0; i4 < i2; i4++) {
            bArr[i4] = (byte) ((i >> (i4 * 8)) & 255);
        }
        return bArr;
    }

    public ASN1BitString(byte[] bArr, int i) {
        if (bArr == null) {
            throw new NullPointerException("data cannot be null");
        }
        if (bArr.length == 0 && i != 0) {
            throw new IllegalArgumentException("zero length data with non-zero pad bits");
        }
        if (i > 7 || i < 0) {
            throw new IllegalArgumentException("pad bits cannot be greater than 7 or less than 0");
        }
        this.data = Arrays.clone(bArr);
        this.padBits = i;
    }

    @Override
    public String getString() {
        StringBuffer stringBuffer = new StringBuffer("#");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            new ASN1OutputStream(byteArrayOutputStream).writeObject(this);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            for (int i = 0; i != byteArray.length; i++) {
                stringBuffer.append(table[(byteArray[i] >>> 4) & 15]);
                stringBuffer.append(table[byteArray[i] & 15]);
            }
            return stringBuffer.toString();
        } catch (IOException e) {
            throw new ASN1ParsingException("Internal error encoding BitString: " + e.getMessage(), e);
        }
    }

    public int intValue() {
        byte[] bArrDerForm = this.data;
        if (this.padBits > 0 && this.data.length <= 4) {
            bArrDerForm = derForm(this.data, this.padBits);
        }
        int i = 0;
        for (int i2 = 0; i2 != bArrDerForm.length && i2 != 4; i2++) {
            i |= (bArrDerForm[i2] & 255) << (8 * i2);
        }
        return i;
    }

    public byte[] getOctets() {
        if (this.padBits != 0) {
            throw new IllegalStateException("attempt to get non-octet aligned data from BIT STRING");
        }
        return Arrays.clone(this.data);
    }

    public byte[] getBytes() {
        return derForm(this.data, this.padBits);
    }

    public int getPadBits() {
        return this.padBits;
    }

    public String toString() {
        return getString();
    }

    @Override
    public int hashCode() {
        return this.padBits ^ Arrays.hashCode(getBytes());
    }

    @Override
    protected boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1BitString)) {
            return false;
        }
        ASN1BitString aSN1BitString = (ASN1BitString) aSN1Primitive;
        return this.padBits == aSN1BitString.padBits && Arrays.areEqual(getBytes(), aSN1BitString.getBytes());
    }

    protected static byte[] derForm(byte[] bArr, int i) {
        byte[] bArrClone = Arrays.clone(bArr);
        if (i > 0) {
            int length = bArr.length - 1;
            bArrClone[length] = (byte) ((255 << i) & bArrClone[length]);
        }
        return bArrClone;
    }

    static ASN1BitString fromInputStream(int i, InputStream inputStream) throws IOException {
        if (i < 1) {
            throw new IllegalArgumentException("truncated BIT STRING detected");
        }
        int i2 = inputStream.read();
        byte[] bArr = new byte[i - 1];
        if (bArr.length != 0) {
            if (Streams.readFully(inputStream, bArr) != bArr.length) {
                throw new EOFException("EOF encountered in middle of BIT STRING");
            }
            if (i2 > 0 && i2 < 8 && bArr[bArr.length - 1] != ((byte) (bArr[bArr.length - 1] & (255 << i2)))) {
                return new DLBitString(bArr, i2);
            }
        }
        return new DERBitString(bArr, i2);
    }

    public ASN1Primitive getLoadedObject() {
        return toASN1Primitive();
    }

    @Override
    ASN1Primitive toDERObject() {
        return new DERBitString(this.data, this.padBits);
    }

    @Override
    ASN1Primitive toDLObject() {
        return new DLBitString(this.data, this.padBits);
    }
}
