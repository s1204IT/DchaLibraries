package com.google.protobuf;

import java.io.IOException;
import java.util.Arrays;

public final class UnknownFieldSetLite {
    private static final UnknownFieldSetLite DEFAULT_INSTANCE = new UnknownFieldSetLite(0, new int[0], new Object[0], false);
    private static final int MIN_CAPACITY = 8;
    private int count;
    private boolean isMutable;
    private int memoizedSerializedSize;
    private Object[] objects;
    private int[] tags;

    public static UnknownFieldSetLite getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    static UnknownFieldSetLite newInstance() {
        return new UnknownFieldSetLite();
    }

    static UnknownFieldSetLite mutableCopyOf(UnknownFieldSetLite unknownFieldSetLite, UnknownFieldSetLite unknownFieldSetLite2) {
        int i = unknownFieldSetLite.count + unknownFieldSetLite2.count;
        int[] iArrCopyOf = Arrays.copyOf(unknownFieldSetLite.tags, i);
        System.arraycopy(unknownFieldSetLite2.tags, 0, iArrCopyOf, unknownFieldSetLite.count, unknownFieldSetLite2.count);
        Object[] objArrCopyOf = Arrays.copyOf(unknownFieldSetLite.objects, i);
        System.arraycopy(unknownFieldSetLite2.objects, 0, objArrCopyOf, unknownFieldSetLite.count, unknownFieldSetLite2.count);
        return new UnknownFieldSetLite(i, iArrCopyOf, objArrCopyOf, true);
    }

    private UnknownFieldSetLite() {
        this(0, new int[8], new Object[8], true);
    }

    private UnknownFieldSetLite(int i, int[] iArr, Object[] objArr, boolean z) {
        this.memoizedSerializedSize = -1;
        this.count = i;
        this.tags = iArr;
        this.objects = objArr;
        this.isMutable = z;
    }

    public void makeImmutable() {
        this.isMutable = false;
    }

    void checkMutable() {
        if (!this.isMutable) {
            throw new UnsupportedOperationException();
        }
    }

    public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
        for (int i = 0; i < this.count; i++) {
            int i2 = this.tags[i];
            int tagFieldNumber = WireFormat.getTagFieldNumber(i2);
            int tagWireType = WireFormat.getTagWireType(i2);
            if (tagWireType != 5) {
                switch (tagWireType) {
                    case 0:
                        codedOutputStream.writeUInt64(tagFieldNumber, ((Long) this.objects[i]).longValue());
                        break;
                    case 1:
                        codedOutputStream.writeFixed64(tagFieldNumber, ((Long) this.objects[i]).longValue());
                        break;
                    case 2:
                        codedOutputStream.writeBytes(tagFieldNumber, (ByteString) this.objects[i]);
                        break;
                    case 3:
                        codedOutputStream.writeTag(tagFieldNumber, 3);
                        ((UnknownFieldSetLite) this.objects[i]).writeTo(codedOutputStream);
                        codedOutputStream.writeTag(tagFieldNumber, 4);
                        break;
                    default:
                        throw InvalidProtocolBufferException.invalidWireType();
                }
            } else {
                codedOutputStream.writeFixed32(tagFieldNumber, ((Integer) this.objects[i]).intValue());
            }
        }
    }

    public int getSerializedSize() {
        int iComputeFixed32Size;
        int i = this.memoizedSerializedSize;
        if (i != -1) {
            return i;
        }
        int i2 = 0;
        for (int i3 = 0; i3 < this.count; i3++) {
            int i4 = this.tags[i3];
            int tagFieldNumber = WireFormat.getTagFieldNumber(i4);
            int tagWireType = WireFormat.getTagWireType(i4);
            if (tagWireType != 5) {
                switch (tagWireType) {
                    case 0:
                        iComputeFixed32Size = CodedOutputStream.computeUInt64Size(tagFieldNumber, ((Long) this.objects[i3]).longValue());
                        break;
                    case 1:
                        iComputeFixed32Size = CodedOutputStream.computeFixed64Size(tagFieldNumber, ((Long) this.objects[i3]).longValue());
                        break;
                    case 2:
                        iComputeFixed32Size = CodedOutputStream.computeBytesSize(tagFieldNumber, (ByteString) this.objects[i3]);
                        break;
                    case 3:
                        iComputeFixed32Size = (CodedOutputStream.computeTagSize(tagFieldNumber) * 2) + ((UnknownFieldSetLite) this.objects[i3]).getSerializedSize();
                        break;
                    default:
                        throw new IllegalStateException(InvalidProtocolBufferException.invalidWireType());
                }
            } else {
                iComputeFixed32Size = CodedOutputStream.computeFixed32Size(tagFieldNumber, ((Integer) this.objects[i3]).intValue());
            }
            i2 += iComputeFixed32Size;
        }
        this.memoizedSerializedSize = i2;
        return i2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != 0 && (obj instanceof UnknownFieldSetLite) && this.count == obj.count && Arrays.equals(this.tags, obj.tags) && Arrays.deepEquals(this.objects, obj.objects)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((527 + this.count) * 31) + Arrays.hashCode(this.tags))) + Arrays.deepHashCode(this.objects);
    }

    final void printWithIndent(StringBuilder sb, int i) {
        for (int i2 = 0; i2 < this.count; i2++) {
            MessageLiteToString.printField(sb, i, String.valueOf(WireFormat.getTagFieldNumber(this.tags[i2])), this.objects[i2]);
        }
    }

    private void storeField(int i, Object obj) {
        ensureCapacity();
        this.tags[this.count] = i;
        this.objects[this.count] = obj;
        this.count++;
    }

    private void ensureCapacity() {
        if (this.count == this.tags.length) {
            int i = this.count + (this.count < 4 ? 8 : this.count >> 1);
            this.tags = Arrays.copyOf(this.tags, i);
            this.objects = Arrays.copyOf(this.objects, i);
        }
    }

    boolean mergeFieldFrom(int i, CodedInputStream codedInputStream) throws IOException {
        checkMutable();
        int tagFieldNumber = WireFormat.getTagFieldNumber(i);
        switch (WireFormat.getTagWireType(i)) {
            case 0:
                storeField(i, Long.valueOf(codedInputStream.readInt64()));
                return true;
            case 1:
                storeField(i, Long.valueOf(codedInputStream.readFixed64()));
                return true;
            case 2:
                storeField(i, codedInputStream.readBytes());
                return true;
            case 3:
                UnknownFieldSetLite unknownFieldSetLite = new UnknownFieldSetLite();
                unknownFieldSetLite.mergeFrom(codedInputStream);
                codedInputStream.checkLastTagWas(WireFormat.makeTag(tagFieldNumber, 4));
                storeField(i, unknownFieldSetLite);
                return true;
            case 4:
                return false;
            case 5:
                storeField(i, Integer.valueOf(codedInputStream.readFixed32()));
                return true;
            default:
                throw InvalidProtocolBufferException.invalidWireType();
        }
    }

    UnknownFieldSetLite mergeVarintField(int i, int i2) {
        checkMutable();
        if (i == 0) {
            throw new IllegalArgumentException("Zero is not a valid field number.");
        }
        storeField(WireFormat.makeTag(i, 0), Long.valueOf(i2));
        return this;
    }

    UnknownFieldSetLite mergeLengthDelimitedField(int i, ByteString byteString) {
        checkMutable();
        if (i == 0) {
            throw new IllegalArgumentException("Zero is not a valid field number.");
        }
        storeField(WireFormat.makeTag(i, 2), byteString);
        return this;
    }

    private UnknownFieldSetLite mergeFrom(CodedInputStream codedInputStream) throws IOException {
        int tag;
        do {
            tag = codedInputStream.readTag();
            if (tag == 0) {
                break;
            }
        } while (mergeFieldFrom(tag, codedInputStream));
        return this;
    }
}
