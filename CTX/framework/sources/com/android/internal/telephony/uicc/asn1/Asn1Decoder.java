package com.android.internal.telephony.uicc.asn1;

import com.android.internal.telephony.uicc.IccUtils;

public final class Asn1Decoder {
    private final int mEnd;
    private int mPosition;
    private final byte[] mSrc;

    public Asn1Decoder(String str) {
        this(IccUtils.hexStringToBytes(str));
    }

    public Asn1Decoder(byte[] bArr) {
        this(bArr, 0, bArr.length);
    }

    public Asn1Decoder(byte[] bArr, int i, int i2) {
        int i3;
        if (i < 0 || i2 < 0 || (i3 = i + i2) > bArr.length) {
            throw new IndexOutOfBoundsException("Out of the bounds: bytes=[" + bArr.length + "], offset=" + i + ", length=" + i2);
        }
        this.mSrc = bArr;
        this.mPosition = i;
        this.mEnd = i3;
    }

    public int getPosition() {
        return this.mPosition;
    }

    public boolean hasNextNode() {
        return this.mPosition < this.mEnd;
    }

    public Asn1Node nextNode() throws InvalidAsn1DataException {
        int i;
        if (this.mPosition >= this.mEnd) {
            throw new IllegalStateException("No bytes to parse.");
        }
        int i2 = this.mPosition;
        int i3 = i2 + 1;
        if ((this.mSrc[i2] & 31) == 31) {
            while (true) {
                if (i3 >= this.mEnd) {
                    break;
                }
                int i4 = i3 + 1;
                if ((this.mSrc[i3] & 128) == 0) {
                    i3 = i4;
                    break;
                }
                i3 = i4;
            }
        }
        if (i3 >= this.mEnd) {
            throw new InvalidAsn1DataException(0, "Invalid length at position: " + i3);
        }
        try {
            int iBytesToInt = IccUtils.bytesToInt(this.mSrc, i2, i3 - i2);
            int i5 = i3 + 1;
            int iBytesToInt2 = this.mSrc[i3];
            if ((iBytesToInt2 & 128) != 0) {
                int i6 = iBytesToInt2 & 127;
                i = i5 + i6;
                if (i > this.mEnd) {
                    throw new InvalidAsn1DataException(iBytesToInt, "Cannot parse length at position: " + i5);
                }
                try {
                    iBytesToInt2 = IccUtils.bytesToInt(this.mSrc, i5, i6);
                } catch (IllegalArgumentException e) {
                    throw new InvalidAsn1DataException(iBytesToInt, "Cannot parse length at position: " + i5, e);
                }
            } else {
                i = i5;
            }
            int i7 = i + iBytesToInt2;
            if (i7 > this.mEnd) {
                throw new InvalidAsn1DataException(iBytesToInt, "Incomplete data at position: " + i + ", expected bytes: " + iBytesToInt2 + ", actual bytes: " + (this.mEnd - i));
            }
            Asn1Node asn1Node = new Asn1Node(iBytesToInt, this.mSrc, i, iBytesToInt2);
            this.mPosition = i7;
            return asn1Node;
        } catch (IllegalArgumentException e2) {
            throw new InvalidAsn1DataException(0, "Cannot parse tag at position: " + i2, e2);
        }
    }
}
