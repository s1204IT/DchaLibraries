package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import java.io.IOException;

public abstract class ASN1ApplicationSpecific extends ASN1Primitive {
    protected final boolean isConstructed;
    protected final byte[] octets;
    protected final int tag;

    ASN1ApplicationSpecific(boolean isConstructed, int tag, byte[] octets) {
        this.isConstructed = isConstructed;
        this.tag = tag;
        this.octets = octets;
    }

    public static com.android.org.bouncycastle.asn1.ASN1ApplicationSpecific getInstance(java.lang.Object r4) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.asn1.ASN1ApplicationSpecific.getInstance(java.lang.Object):com.android.org.bouncycastle.asn1.ASN1ApplicationSpecific");
    }

    protected static int getLengthOfHeader(byte[] data) {
        int length = data[1] & 255;
        if (length == 128 || length <= 127) {
            return 2;
        }
        int size = length & 127;
        if (size > 4) {
            throw new IllegalStateException("DER length more than 4 bytes: " + size);
        }
        return size + 2;
    }

    @Override
    public boolean isConstructed() {
        return this.isConstructed;
    }

    public byte[] getContents() {
        return this.octets;
    }

    public int getApplicationTag() {
        return this.tag;
    }

    public ASN1Primitive getObject() throws IOException {
        return new ASN1InputStream(getContents()).readObject();
    }

    public ASN1Primitive getObject(int derTagNo) throws IOException {
        if (derTagNo >= 31) {
            throw new IOException("unsupported tag number");
        }
        byte[] orig = getEncoded();
        byte[] tmp = replaceTagNumber(derTagNo, orig);
        if ((orig[0] & 32) != 0) {
            tmp[0] = (byte) (tmp[0] | 32);
        }
        return new ASN1InputStream(tmp).readObject();
    }

    @Override
    int encodedLength() throws IOException {
        return StreamUtil.calculateTagLength(this.tag) + StreamUtil.calculateBodyLength(this.octets.length) + this.octets.length;
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        int classBits = 64;
        if (this.isConstructed) {
            classBits = 96;
        }
        out.writeEncoded(classBits, this.tag, this.octets);
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if ((aSN1Primitive instanceof ASN1ApplicationSpecific) && this.isConstructed == aSN1Primitive.isConstructed && this.tag == aSN1Primitive.tag) {
            return Arrays.areEqual(this.octets, aSN1Primitive.octets);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((this.isConstructed ? 1 : 0) ^ this.tag) ^ Arrays.hashCode(this.octets);
    }

    private byte[] replaceTagNumber(int newTag, byte[] input) throws IOException {
        int tagNo = input[0] & 31;
        int index = 1;
        if (tagNo == 31) {
            int tagNo2 = 0;
            int b = input[1] & 255;
            if ((b & 127) != 0) {
                int index2 = 2;
                while (b >= 0 && (b & 128) != 0) {
                    tagNo2 = (tagNo2 | (b & 127)) << 7;
                    b = input[index2] & 255;
                    index2++;
                }
                index = index2;
            } else {
                throw new ASN1ParsingException("corrupted stream - invalid high tag number found");
            }
        }
        byte[] tmp = new byte[(input.length - index) + 1];
        System.arraycopy(input, index, tmp, 1, tmp.length - 1);
        tmp[0] = (byte) newTag;
        return tmp;
    }
}
