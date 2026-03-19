package com.android.internal.telephony.uicc.asn1;

import com.android.internal.telephony.uicc.IccUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Asn1Node {
    private static final int INT_BYTES = 4;
    private final List<Asn1Node> mChildren;
    private final boolean mConstructed;
    private byte[] mDataBytes;
    private int mDataLength;
    private int mDataOffset;
    private int mEncodedLength;
    private final int mTag;
    private static final List<Asn1Node> EMPTY_NODE_LIST = Collections.emptyList();
    private static final byte[] TRUE_BYTES = {-1};
    private static final byte[] FALSE_BYTES = {0};

    public static final class Builder {
        private final List<Asn1Node> mChildren;
        private final int mTag;

        private Builder(int i) {
            if (!Asn1Node.isConstructedTag(i)) {
                throw new IllegalArgumentException("Builder should be created for a constructed tag: " + i);
            }
            this.mTag = i;
            this.mChildren = new ArrayList();
        }

        public Builder addChild(Asn1Node asn1Node) {
            this.mChildren.add(asn1Node);
            return this;
        }

        public Builder addChild(Builder builder) {
            this.mChildren.add(builder.build());
            return this;
        }

        public Builder addChildren(byte[] bArr) throws InvalidAsn1DataException {
            Asn1Decoder asn1Decoder = new Asn1Decoder(bArr, 0, bArr.length);
            while (asn1Decoder.hasNextNode()) {
                this.mChildren.add(asn1Decoder.nextNode());
            }
            return this;
        }

        public Builder addChildAsInteger(int i, int i2) {
            if (Asn1Node.isConstructedTag(i)) {
                throw new IllegalStateException("Cannot set value of a constructed tag: " + i);
            }
            byte[] bArrSignedIntToBytes = IccUtils.signedIntToBytes(i2);
            addChild(new Asn1Node(i, bArrSignedIntToBytes, 0, bArrSignedIntToBytes.length));
            return this;
        }

        public Builder addChildAsString(int i, String str) {
            if (Asn1Node.isConstructedTag(i)) {
                throw new IllegalStateException("Cannot set value of a constructed tag: " + i);
            }
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            addChild(new Asn1Node(i, bytes, 0, bytes.length));
            return this;
        }

        public Builder addChildAsBytes(int i, byte[] bArr) {
            if (Asn1Node.isConstructedTag(i)) {
                throw new IllegalStateException("Cannot set value of a constructed tag: " + i);
            }
            addChild(new Asn1Node(i, bArr, 0, bArr.length));
            return this;
        }

        public Builder addChildAsBytesFromHex(int i, String str) {
            return addChildAsBytes(i, IccUtils.hexStringToBytes(str));
        }

        public Builder addChildAsBits(int i, int i2) {
            if (Asn1Node.isConstructedTag(i)) {
                throw new IllegalStateException("Cannot set value of a constructed tag: " + i);
            }
            byte[] bArr = new byte[5];
            int iReverse = Integer.reverse(i2);
            int i3 = 0;
            for (int i4 = 1; i4 < bArr.length; i4++) {
                bArr[i4] = (byte) (iReverse >> ((4 - i4) * 8));
                if (bArr[i4] != 0) {
                    i3 = i4;
                }
            }
            int i5 = i3 + 1;
            bArr[0] = IccUtils.countTrailingZeros(bArr[i5 - 1]);
            addChild(new Asn1Node(i, bArr, 0, i5));
            return this;
        }

        public Builder addChildAsBoolean(int i, boolean z) {
            if (Asn1Node.isConstructedTag(i)) {
                throw new IllegalStateException("Cannot set value of a constructed tag: " + i);
            }
            addChild(new Asn1Node(i, z ? Asn1Node.TRUE_BYTES : Asn1Node.FALSE_BYTES, 0, 1));
            return this;
        }

        public Asn1Node build() {
            return new Asn1Node(this.mTag, this.mChildren);
        }
    }

    public static Builder newBuilder(int i) {
        return new Builder(i);
    }

    private static boolean isConstructedTag(int i) {
        return (IccUtils.unsignedIntToBytes(i)[0] & 32) != 0;
    }

    private static int calculateEncodedBytesNumForLength(int i) {
        if (i > 127) {
            return 1 + IccUtils.byteNumForUnsignedInt(i);
        }
        return 1;
    }

    Asn1Node(int i, byte[] bArr, int i2, int i3) {
        this.mTag = i;
        this.mConstructed = isConstructedTag(i);
        this.mDataBytes = bArr;
        this.mDataOffset = i2;
        this.mDataLength = i3;
        this.mChildren = this.mConstructed ? new ArrayList<>() : EMPTY_NODE_LIST;
        this.mEncodedLength = IccUtils.byteNumForUnsignedInt(this.mTag) + calculateEncodedBytesNumForLength(this.mDataLength) + this.mDataLength;
    }

    private Asn1Node(int i, List<Asn1Node> list) {
        this.mTag = i;
        this.mConstructed = true;
        this.mChildren = list;
        this.mDataLength = 0;
        int size = list.size();
        for (int i2 = 0; i2 < size; i2++) {
            this.mDataLength += list.get(i2).mEncodedLength;
        }
        this.mEncodedLength = IccUtils.byteNumForUnsignedInt(this.mTag) + calculateEncodedBytesNumForLength(this.mDataLength) + this.mDataLength;
    }

    public int getTag() {
        return this.mTag;
    }

    public boolean isConstructed() {
        return this.mConstructed;
    }

    public boolean hasChild(int i, int... iArr) throws InvalidAsn1DataException {
        try {
            getChild(i, iArr);
            return true;
        } catch (TagNotFoundException e) {
            return false;
        }
    }

    public Asn1Node getChild(int i, int... iArr) throws TagNotFoundException, InvalidAsn1DataException {
        if (!this.mConstructed) {
            throw new TagNotFoundException(i);
        }
        int i2 = i;
        int i3 = 0;
        Asn1Node asn1Node = this;
        while (asn1Node != null) {
            List<Asn1Node> children = asn1Node.getChildren();
            int size = children.size();
            int i4 = 0;
            while (true) {
                if (i4 < size) {
                    Asn1Node asn1Node2 = children.get(i4);
                    if (asn1Node2.getTag() != i2) {
                        i4++;
                    } else {
                        asn1Node = asn1Node2;
                        break;
                    }
                } else {
                    asn1Node = null;
                    break;
                }
            }
            if (i3 >= iArr.length) {
                break;
            }
            int i5 = i3 + 1;
            int i6 = iArr[i3];
            i3 = i5;
            i2 = i6;
        }
        if (asn1Node == null) {
            throw new TagNotFoundException(i2);
        }
        return asn1Node;
    }

    public List<Asn1Node> getChildren(int i) throws TagNotFoundException, InvalidAsn1DataException {
        if (!this.mConstructed) {
            return EMPTY_NODE_LIST;
        }
        List<Asn1Node> children = getChildren();
        if (children.isEmpty()) {
            return EMPTY_NODE_LIST;
        }
        ArrayList arrayList = new ArrayList();
        int size = children.size();
        for (int i2 = 0; i2 < size; i2++) {
            Asn1Node asn1Node = children.get(i2);
            if (asn1Node.getTag() == i) {
                arrayList.add(asn1Node);
            }
        }
        return arrayList.isEmpty() ? EMPTY_NODE_LIST : arrayList;
    }

    public List<Asn1Node> getChildren() throws InvalidAsn1DataException {
        if (!this.mConstructed) {
            return EMPTY_NODE_LIST;
        }
        if (this.mDataBytes != null) {
            Asn1Decoder asn1Decoder = new Asn1Decoder(this.mDataBytes, this.mDataOffset, this.mDataLength);
            while (asn1Decoder.hasNextNode()) {
                this.mChildren.add(asn1Decoder.nextNode());
            }
            this.mDataBytes = null;
            this.mDataOffset = 0;
        }
        return this.mChildren;
    }

    public boolean hasValue() {
        return (this.mConstructed || this.mDataBytes == null) ? false : true;
    }

    public int asInteger() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        }
        if (this.mDataBytes == null) {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
        try {
            return IccUtils.bytesToInt(this.mDataBytes, this.mDataOffset, this.mDataLength);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
        }
    }

    public long asRawLong() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        }
        if (this.mDataBytes == null) {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
        try {
            return IccUtils.bytesToRawLong(this.mDataBytes, this.mDataOffset, this.mDataLength);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
        }
    }

    public String asString() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        }
        if (this.mDataBytes == null) {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
        try {
            return new String(this.mDataBytes, this.mDataOffset, this.mDataLength, StandardCharsets.UTF_8);
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
        }
    }

    public byte[] asBytes() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        }
        if (this.mDataBytes == null) {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
        byte[] bArr = new byte[this.mDataLength];
        try {
            System.arraycopy(this.mDataBytes, this.mDataOffset, bArr, 0, this.mDataLength);
            return bArr;
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
        }
    }

    public int asBits() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        }
        if (this.mDataBytes == null) {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
        try {
            int iBytesToInt = IccUtils.bytesToInt(this.mDataBytes, this.mDataOffset + 1, this.mDataLength - 1);
            for (int i = this.mDataLength - 1; i < 4; i++) {
                iBytesToInt <<= 8;
            }
            return Integer.reverse(iBytesToInt);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", e);
        }
    }

    public boolean asBoolean() throws InvalidAsn1DataException {
        if (this.mConstructed) {
            throw new IllegalStateException("Cannot get value of a constructed node.");
        }
        if (this.mDataBytes == null) {
            throw new InvalidAsn1DataException(this.mTag, "Data bytes cannot be null.");
        }
        if (this.mDataLength != 1) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes as boolean: length=" + this.mDataLength);
        }
        if (this.mDataOffset < 0 || this.mDataOffset >= this.mDataBytes.length) {
            throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes.", new ArrayIndexOutOfBoundsException(this.mDataOffset));
        }
        if (this.mDataBytes[this.mDataOffset] == -1) {
            return Boolean.TRUE.booleanValue();
        }
        if (this.mDataBytes[this.mDataOffset] == 0) {
            return Boolean.FALSE.booleanValue();
        }
        throw new InvalidAsn1DataException(this.mTag, "Cannot parse data bytes as boolean: " + ((int) this.mDataBytes[this.mDataOffset]));
    }

    public int getEncodedLength() {
        return this.mEncodedLength;
    }

    public int getDataLength() {
        return this.mDataLength;
    }

    public void writeToBytes(byte[] bArr, int i) {
        if (i < 0 || this.mEncodedLength + i > bArr.length) {
            throw new IndexOutOfBoundsException("Not enough space to write. Required bytes: " + this.mEncodedLength);
        }
        write(bArr, i);
    }

    public byte[] toBytes() {
        byte[] bArr = new byte[this.mEncodedLength];
        write(bArr, 0);
        return bArr;
    }

    public String toHex() {
        return IccUtils.bytesToHexString(toBytes());
    }

    public String getHeadAsHex() {
        String strBytesToHexString = IccUtils.bytesToHexString(IccUtils.unsignedIntToBytes(this.mTag));
        if (this.mDataLength <= 127) {
            return strBytesToHexString + IccUtils.byteToHex((byte) this.mDataLength);
        }
        byte[] bArrUnsignedIntToBytes = IccUtils.unsignedIntToBytes(this.mDataLength);
        return (strBytesToHexString + IccUtils.byteToHex((byte) (bArrUnsignedIntToBytes.length | 128))) + IccUtils.bytesToHexString(bArrUnsignedIntToBytes);
    }

    private int write(byte[] bArr, int i) {
        int iWrite;
        int iUnsignedIntToBytes = i + IccUtils.unsignedIntToBytes(this.mTag, bArr, i);
        if (this.mDataLength <= 127) {
            iWrite = iUnsignedIntToBytes + 1;
            bArr[iUnsignedIntToBytes] = (byte) this.mDataLength;
        } else {
            int i2 = iUnsignedIntToBytes + 1;
            int iUnsignedIntToBytes2 = IccUtils.unsignedIntToBytes(this.mDataLength, bArr, i2);
            bArr[i2 - 1] = (byte) (iUnsignedIntToBytes2 | 128);
            iWrite = iUnsignedIntToBytes2 + i2;
        }
        if (!this.mConstructed || this.mDataBytes != null) {
            if (this.mDataBytes != null) {
                System.arraycopy(this.mDataBytes, this.mDataOffset, bArr, iWrite, this.mDataLength);
                return iWrite + this.mDataLength;
            }
            return iWrite;
        }
        int size = this.mChildren.size();
        for (int i3 = 0; i3 < size; i3++) {
            iWrite = this.mChildren.get(i3).write(bArr, iWrite);
        }
        return iWrite;
    }
}
