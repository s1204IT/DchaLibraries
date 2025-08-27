package com.google.protobuf.nano;

import com.google.protobuf.nano.ExtendableMessageNano;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class Extension<M extends ExtendableMessageNano<M>, T> {
    public static final int TYPE_BOOL = 8;
    public static final int TYPE_BYTES = 12;
    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_ENUM = 14;
    public static final int TYPE_FIXED32 = 7;
    public static final int TYPE_FIXED64 = 6;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_GROUP = 10;
    public static final int TYPE_INT32 = 5;
    public static final int TYPE_INT64 = 3;
    public static final int TYPE_MESSAGE = 11;
    public static final int TYPE_SFIXED32 = 15;
    public static final int TYPE_SFIXED64 = 16;
    public static final int TYPE_SINT32 = 17;
    public static final int TYPE_SINT64 = 18;
    public static final int TYPE_STRING = 9;
    public static final int TYPE_UINT32 = 13;
    public static final int TYPE_UINT64 = 4;
    protected final Class<T> clazz;
    protected final boolean repeated;
    public final int tag;
    protected final int type;

    /* synthetic */ Extension(int i, Class cls, int i2, boolean z, AnonymousClass1 anonymousClass1) {
        this(i, cls, i2, z);
    }

    @Deprecated
    public static <M extends ExtendableMessageNano<M>, T extends MessageNano> Extension<M, T> createMessageTyped(int i, Class<T> cls, int i2) {
        return new Extension<>(i, cls, i2, false);
    }

    public static <M extends ExtendableMessageNano<M>, T extends MessageNano> Extension<M, T> createMessageTyped(int i, Class<T> cls, long j) {
        return new Extension<>(i, cls, (int) j, false);
    }

    public static <M extends ExtendableMessageNano<M>, T extends MessageNano> Extension<M, T[]> createRepeatedMessageTyped(int i, Class<T[]> cls, long j) {
        return new Extension<>(i, cls, (int) j, true);
    }

    public static <M extends ExtendableMessageNano<M>, T> Extension<M, T> createPrimitiveTyped(int i, Class<T> cls, long j) {
        return new PrimitiveExtension(i, cls, (int) j, false, 0, 0);
    }

    public static <M extends ExtendableMessageNano<M>, T> Extension<M, T> createRepeatedPrimitiveTyped(int i, Class<T> cls, long j, long j2, long j3) {
        return new PrimitiveExtension(i, cls, (int) j, true, (int) j2, (int) j3);
    }

    private Extension(int i, Class<T> cls, int i2, boolean z) {
        this.type = i;
        this.clazz = cls;
        this.tag = i2;
        this.repeated = z;
    }

    final T getValueFrom(List<UnknownFieldData> list) {
        if (list == null) {
            return null;
        }
        return this.repeated ? getRepeatedValueFrom(list) : getSingularValueFrom(list);
    }

    private T getRepeatedValueFrom(List<UnknownFieldData> list) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            UnknownFieldData unknownFieldData = list.get(i);
            if (unknownFieldData.bytes.length != 0) {
                readDataInto(unknownFieldData, arrayList);
            }
        }
        int size = arrayList.size();
        if (size == 0) {
            return null;
        }
        T tCast = this.clazz.cast(Array.newInstance(this.clazz.getComponentType(), size));
        for (int i2 = 0; i2 < size; i2++) {
            Array.set(tCast, i2, arrayList.get(i2));
        }
        return tCast;
    }

    private T getSingularValueFrom(List<UnknownFieldData> list) {
        if (list.isEmpty()) {
            return null;
        }
        return this.clazz.cast(readData(CodedInputByteBufferNano.newInstance(list.get(list.size() - 1).bytes)));
    }

    protected Object readData(CodedInputByteBufferNano codedInputByteBufferNano) {
        Class componentType = this.repeated ? this.clazz.getComponentType() : this.clazz;
        try {
            switch (this.type) {
                case 10:
                    MessageNano messageNano = (MessageNano) componentType.newInstance();
                    codedInputByteBufferNano.readGroup(messageNano, WireFormatNano.getTagFieldNumber(this.tag));
                    return messageNano;
                case 11:
                    MessageNano messageNano2 = (MessageNano) componentType.newInstance();
                    codedInputByteBufferNano.readMessage(messageNano2);
                    return messageNano2;
                default:
                    throw new IllegalArgumentException("Unknown type " + this.type);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading extension field", e);
        } catch (IllegalAccessException e2) {
            throw new IllegalArgumentException("Error creating instance of class " + componentType, e2);
        } catch (InstantiationException e3) {
            throw new IllegalArgumentException("Error creating instance of class " + componentType, e3);
        }
    }

    protected void readDataInto(UnknownFieldData unknownFieldData, List<Object> list) {
        list.add(readData(CodedInputByteBufferNano.newInstance(unknownFieldData.bytes)));
    }

    void writeTo(Object obj, CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (this.repeated) {
            writeRepeatedData(obj, codedOutputByteBufferNano);
        } else {
            writeSingularData(obj, codedOutputByteBufferNano);
        }
    }

    protected void writeSingularData(Object obj, CodedOutputByteBufferNano codedOutputByteBufferNano) {
        try {
            codedOutputByteBufferNano.writeRawVarint32(this.tag);
            switch (this.type) {
                case 10:
                    int tagFieldNumber = WireFormatNano.getTagFieldNumber(this.tag);
                    codedOutputByteBufferNano.writeGroupNoTag((MessageNano) obj);
                    codedOutputByteBufferNano.writeTag(tagFieldNumber, 4);
                    return;
                case 11:
                    codedOutputByteBufferNano.writeMessageNoTag((MessageNano) obj);
                    return;
                default:
                    throw new IllegalArgumentException("Unknown type " + this.type);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void writeRepeatedData(Object obj, CodedOutputByteBufferNano codedOutputByteBufferNano) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        int length = Array.getLength(obj);
        for (int i = 0; i < length; i++) {
            Object obj2 = Array.get(obj, i);
            if (obj2 != null) {
                writeSingularData(obj2, codedOutputByteBufferNano);
            }
        }
    }

    int computeSerializedSize(Object obj) {
        if (this.repeated) {
            return computeRepeatedSerializedSize(obj);
        }
        return computeSingularSerializedSize(obj);
    }

    protected int computeRepeatedSerializedSize(Object obj) {
        int length = Array.getLength(obj);
        int iComputeSingularSerializedSize = 0;
        for (int i = 0; i < length; i++) {
            if (Array.get(obj, i) != null) {
                iComputeSingularSerializedSize += computeSingularSerializedSize(Array.get(obj, i));
            }
        }
        return iComputeSingularSerializedSize;
    }

    protected int computeSingularSerializedSize(Object obj) {
        int tagFieldNumber = WireFormatNano.getTagFieldNumber(this.tag);
        switch (this.type) {
            case 10:
                return CodedOutputByteBufferNano.computeGroupSize(tagFieldNumber, (MessageNano) obj);
            case 11:
                return CodedOutputByteBufferNano.computeMessageSize(tagFieldNumber, (MessageNano) obj);
            default:
                throw new IllegalArgumentException("Unknown type " + this.type);
        }
    }

    private static class PrimitiveExtension<M extends ExtendableMessageNano<M>, T> extends Extension<M, T> {
        private final int nonPackedTag;
        private final int packedTag;

        public PrimitiveExtension(int i, Class<T> cls, int i2, boolean z, int i3, int i4) {
            super(i, cls, i2, z);
            this.nonPackedTag = i3;
            this.packedTag = i4;
        }

        @Override // com.google.protobuf.nano.Extension
        protected Object readData(CodedInputByteBufferNano codedInputByteBufferNano) {
            try {
                return codedInputByteBufferNano.readPrimitiveField(this.type);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error reading extension field", e);
            }
        }

        @Override // com.google.protobuf.nano.Extension
        protected void readDataInto(UnknownFieldData unknownFieldData, List<Object> list) {
            if (unknownFieldData.tag == this.nonPackedTag) {
                list.add(readData(CodedInputByteBufferNano.newInstance(unknownFieldData.bytes)));
                return;
            }
            CodedInputByteBufferNano codedInputByteBufferNanoNewInstance = CodedInputByteBufferNano.newInstance(unknownFieldData.bytes);
            try {
                codedInputByteBufferNanoNewInstance.pushLimit(codedInputByteBufferNanoNewInstance.readRawVarint32());
                while (!codedInputByteBufferNanoNewInstance.isAtEnd()) {
                    list.add(readData(codedInputByteBufferNanoNewInstance));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Error reading extension field", e);
            }
        }

        @Override // com.google.protobuf.nano.Extension
        protected final void writeSingularData(Object obj, CodedOutputByteBufferNano codedOutputByteBufferNano) {
            try {
                codedOutputByteBufferNano.writeRawVarint32(this.tag);
                switch (this.type) {
                    case 1:
                        codedOutputByteBufferNano.writeDoubleNoTag(((Double) obj).doubleValue());
                        return;
                    case 2:
                        codedOutputByteBufferNano.writeFloatNoTag(((Float) obj).floatValue());
                        return;
                    case 3:
                        codedOutputByteBufferNano.writeInt64NoTag(((Long) obj).longValue());
                        return;
                    case 4:
                        codedOutputByteBufferNano.writeUInt64NoTag(((Long) obj).longValue());
                        return;
                    case 5:
                        codedOutputByteBufferNano.writeInt32NoTag(((Integer) obj).intValue());
                        return;
                    case 6:
                        codedOutputByteBufferNano.writeFixed64NoTag(((Long) obj).longValue());
                        return;
                    case 7:
                        codedOutputByteBufferNano.writeFixed32NoTag(((Integer) obj).intValue());
                        return;
                    case 8:
                        codedOutputByteBufferNano.writeBoolNoTag(((Boolean) obj).booleanValue());
                        return;
                    case 9:
                        codedOutputByteBufferNano.writeStringNoTag((String) obj);
                        return;
                    case 10:
                    case 11:
                    default:
                        throw new IllegalArgumentException("Unknown type " + this.type);
                    case 12:
                        codedOutputByteBufferNano.writeBytesNoTag((byte[]) obj);
                        return;
                    case 13:
                        codedOutputByteBufferNano.writeUInt32NoTag(((Integer) obj).intValue());
                        return;
                    case 14:
                        codedOutputByteBufferNano.writeEnumNoTag(((Integer) obj).intValue());
                        return;
                    case 15:
                        codedOutputByteBufferNano.writeSFixed32NoTag(((Integer) obj).intValue());
                        return;
                    case 16:
                        codedOutputByteBufferNano.writeSFixed64NoTag(((Long) obj).longValue());
                        return;
                    case 17:
                        codedOutputByteBufferNano.writeSInt32NoTag(((Integer) obj).intValue());
                        return;
                    case 18:
                        codedOutputByteBufferNano.writeSInt64NoTag(((Long) obj).longValue());
                        return;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.google.protobuf.nano.Extension
        protected void writeRepeatedData(Object obj, CodedOutputByteBufferNano codedOutputByteBufferNano) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
            if (this.tag == this.nonPackedTag) {
                super.writeRepeatedData(obj, codedOutputByteBufferNano);
                return;
            }
            if (this.tag == this.packedTag) {
                int length = Array.getLength(obj);
                int iComputePackedDataSize = computePackedDataSize(obj);
                try {
                    codedOutputByteBufferNano.writeRawVarint32(this.tag);
                    codedOutputByteBufferNano.writeRawVarint32(iComputePackedDataSize);
                    int i = this.type;
                    int i2 = 0;
                    switch (i) {
                        case 1:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeDoubleNoTag(Array.getDouble(obj, i2));
                                i2++;
                            }
                            return;
                        case 2:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeFloatNoTag(Array.getFloat(obj, i2));
                                i2++;
                            }
                            return;
                        case 3:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeInt64NoTag(Array.getLong(obj, i2));
                                i2++;
                            }
                            return;
                        case 4:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeUInt64NoTag(Array.getLong(obj, i2));
                                i2++;
                            }
                            return;
                        case 5:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeInt32NoTag(Array.getInt(obj, i2));
                                i2++;
                            }
                            return;
                        case 6:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeFixed64NoTag(Array.getLong(obj, i2));
                                i2++;
                            }
                            return;
                        case 7:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeFixed32NoTag(Array.getInt(obj, i2));
                                i2++;
                            }
                            return;
                        case 8:
                            while (i2 < length) {
                                codedOutputByteBufferNano.writeBoolNoTag(Array.getBoolean(obj, i2));
                                i2++;
                            }
                            return;
                        default:
                            switch (i) {
                                case 13:
                                    while (i2 < length) {
                                        codedOutputByteBufferNano.writeUInt32NoTag(Array.getInt(obj, i2));
                                        i2++;
                                    }
                                    return;
                                case 14:
                                    while (i2 < length) {
                                        codedOutputByteBufferNano.writeEnumNoTag(Array.getInt(obj, i2));
                                        i2++;
                                    }
                                    return;
                                case 15:
                                    while (i2 < length) {
                                        codedOutputByteBufferNano.writeSFixed32NoTag(Array.getInt(obj, i2));
                                        i2++;
                                    }
                                    return;
                                case 16:
                                    while (i2 < length) {
                                        codedOutputByteBufferNano.writeSFixed64NoTag(Array.getLong(obj, i2));
                                        i2++;
                                    }
                                    return;
                                case 17:
                                    while (i2 < length) {
                                        codedOutputByteBufferNano.writeSInt32NoTag(Array.getInt(obj, i2));
                                        i2++;
                                    }
                                    return;
                                case 18:
                                    while (i2 < length) {
                                        codedOutputByteBufferNano.writeSInt64NoTag(Array.getLong(obj, i2));
                                        i2++;
                                    }
                                    return;
                                default:
                                    throw new IllegalArgumentException("Unpackable type " + this.type);
                            }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            throw new IllegalArgumentException("Unexpected repeated extension tag " + this.tag + ", unequal to both non-packed variant " + this.nonPackedTag + " and packed variant " + this.packedTag);
        }

        /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
            jadx.core.utils.exceptions.JadxRuntimeException: Failed to find switch 'out' block (already processed)
            	at jadx.core.dex.visitors.regions.maker.SwitchRegionMaker.calcSwitchOut(SwitchRegionMaker.java:200)
            	at jadx.core.dex.visitors.regions.maker.SwitchRegionMaker.process(SwitchRegionMaker.java:61)
            	at jadx.core.dex.visitors.regions.maker.RegionMaker.traverse(RegionMaker.java:112)
            	at jadx.core.dex.visitors.regions.maker.RegionMaker.makeRegion(RegionMaker.java:66)
            	at jadx.core.dex.visitors.regions.maker.SwitchRegionMaker.processFallThroughCases(SwitchRegionMaker.java:105)
            	at jadx.core.dex.visitors.regions.maker.SwitchRegionMaker.process(SwitchRegionMaker.java:64)
            	at jadx.core.dex.visitors.regions.maker.RegionMaker.traverse(RegionMaker.java:112)
            	at jadx.core.dex.visitors.regions.maker.RegionMaker.makeRegion(RegionMaker.java:66)
            	at jadx.core.dex.visitors.regions.maker.RegionMaker.makeMthRegion(RegionMaker.java:48)
            	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:25)
            */
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private int computePackedDataSize(java.lang.Object r6) {
            /*
                r5 = this;
                int r0 = java.lang.reflect.Array.getLength(r6)
                int r1 = r5.type
                r2 = 0
                switch(r1) {
                    case 1: goto La5;
                    case 2: goto La2;
                    case 3: goto L91;
                    case 4: goto L80;
                    case 5: goto L6f;
                    case 6: goto La5;
                    case 7: goto La2;
                    case 8: goto L6d;
                    default: goto Lb;
                }
            Lb:
                switch(r1) {
                    case 13: goto L5c;
                    case 14: goto L4b;
                    case 15: goto La2;
                    case 16: goto La5;
                    case 17: goto L39;
                    case 18: goto L27;
                    default: goto Le;
                }
            Le:
                java.lang.IllegalArgumentException r6 = new java.lang.IllegalArgumentException
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "Unexpected non-packable type "
                r0.append(r1)
                int r1 = r5.type
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                r6.<init>(r0)
                throw r6
            L27:
                r1 = r2
            L28:
                if (r2 >= r0) goto L37
            L2b:
                long r3 = java.lang.reflect.Array.getLong(r6, r2)
                int r3 = com.google.protobuf.nano.CodedOutputByteBufferNano.computeSInt64SizeNoTag(r3)
                int r1 = r1 + r3
                int r2 = r2 + 1
                goto L28
            L37:
                goto La9
            L39:
                r1 = r2
            L3a:
                if (r2 >= r0) goto L49
            L3d:
                int r3 = java.lang.reflect.Array.getInt(r6, r2)
                int r3 = com.google.protobuf.nano.CodedOutputByteBufferNano.computeSInt32SizeNoTag(r3)
                int r1 = r1 + r3
                int r2 = r2 + 1
                goto L3a
            L49:
                goto La9
            L4b:
                r1 = r2
            L4c:
                if (r2 >= r0) goto L5b
            L4f:
                int r3 = java.lang.reflect.Array.getInt(r6, r2)
                int r3 = com.google.protobuf.nano.CodedOutputByteBufferNano.computeEnumSizeNoTag(r3)
                int r1 = r1 + r3
                int r2 = r2 + 1
                goto L4c
            L5b:
                goto La9
            L5c:
                r1 = r2
            L5d:
                if (r2 >= r0) goto L6c
            L60:
                int r3 = java.lang.reflect.Array.getInt(r6, r2)
                int r3 = com.google.protobuf.nano.CodedOutputByteBufferNano.computeUInt32SizeNoTag(r3)
                int r1 = r1 + r3
                int r2 = r2 + 1
                goto L5d
            L6c:
                goto La9
            L6d:
                goto La8
            L6f:
                r1 = r2
            L70:
                if (r2 >= r0) goto L7f
            L73:
                int r3 = java.lang.reflect.Array.getInt(r6, r2)
                int r3 = com.google.protobuf.nano.CodedOutputByteBufferNano.computeInt32SizeNoTag(r3)
                int r1 = r1 + r3
                int r2 = r2 + 1
                goto L70
            L7f:
                goto La9
            L80:
                r1 = r2
            L81:
                if (r2 >= r0) goto L90
            L84:
                long r3 = java.lang.reflect.Array.getLong(r6, r2)
                int r3 = com.google.protobuf.nano.CodedOutputByteBufferNano.computeUInt64SizeNoTag(r3)
                int r1 = r1 + r3
                int r2 = r2 + 1
                goto L81
            L90:
                goto La9
            L91:
                r1 = r2
            L92:
                if (r2 >= r0) goto La1
            L95:
                long r3 = java.lang.reflect.Array.getLong(r6, r2)
                int r3 = com.google.protobuf.nano.CodedOutputByteBufferNano.computeInt64SizeNoTag(r3)
                int r1 = r1 + r3
                int r2 = r2 + 1
                goto L92
            La1:
                goto La9
            La2:
                int r0 = r0 * 4
                goto La8
            La5:
                int r0 = r0 * 8
            La8:
                r1 = r0
            La9:
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.protobuf.nano.Extension.PrimitiveExtension.computePackedDataSize(java.lang.Object):int");
        }

        @Override // com.google.protobuf.nano.Extension
        protected int computeRepeatedSerializedSize(Object obj) {
            if (this.tag == this.nonPackedTag) {
                return super.computeRepeatedSerializedSize(obj);
            }
            if (this.tag == this.packedTag) {
                int iComputePackedDataSize = computePackedDataSize(obj);
                return iComputePackedDataSize + CodedOutputByteBufferNano.computeRawVarint32Size(iComputePackedDataSize) + CodedOutputByteBufferNano.computeRawVarint32Size(this.tag);
            }
            throw new IllegalArgumentException("Unexpected repeated extension tag " + this.tag + ", unequal to both non-packed variant " + this.nonPackedTag + " and packed variant " + this.packedTag);
        }

        @Override // com.google.protobuf.nano.Extension
        protected final int computeSingularSerializedSize(Object obj) {
            int tagFieldNumber = WireFormatNano.getTagFieldNumber(this.tag);
            switch (this.type) {
                case 1:
                    return CodedOutputByteBufferNano.computeDoubleSize(tagFieldNumber, ((Double) obj).doubleValue());
                case 2:
                    return CodedOutputByteBufferNano.computeFloatSize(tagFieldNumber, ((Float) obj).floatValue());
                case 3:
                    return CodedOutputByteBufferNano.computeInt64Size(tagFieldNumber, ((Long) obj).longValue());
                case 4:
                    return CodedOutputByteBufferNano.computeUInt64Size(tagFieldNumber, ((Long) obj).longValue());
                case 5:
                    return CodedOutputByteBufferNano.computeInt32Size(tagFieldNumber, ((Integer) obj).intValue());
                case 6:
                    return CodedOutputByteBufferNano.computeFixed64Size(tagFieldNumber, ((Long) obj).longValue());
                case 7:
                    return CodedOutputByteBufferNano.computeFixed32Size(tagFieldNumber, ((Integer) obj).intValue());
                case 8:
                    return CodedOutputByteBufferNano.computeBoolSize(tagFieldNumber, ((Boolean) obj).booleanValue());
                case 9:
                    return CodedOutputByteBufferNano.computeStringSize(tagFieldNumber, (String) obj);
                case 10:
                case 11:
                default:
                    throw new IllegalArgumentException("Unknown type " + this.type);
                case 12:
                    return CodedOutputByteBufferNano.computeBytesSize(tagFieldNumber, (byte[]) obj);
                case 13:
                    return CodedOutputByteBufferNano.computeUInt32Size(tagFieldNumber, ((Integer) obj).intValue());
                case 14:
                    return CodedOutputByteBufferNano.computeEnumSize(tagFieldNumber, ((Integer) obj).intValue());
                case 15:
                    return CodedOutputByteBufferNano.computeSFixed32Size(tagFieldNumber, ((Integer) obj).intValue());
                case 16:
                    return CodedOutputByteBufferNano.computeSFixed64Size(tagFieldNumber, ((Long) obj).longValue());
                case 17:
                    return CodedOutputByteBufferNano.computeSInt32Size(tagFieldNumber, ((Integer) obj).intValue());
                case 18:
                    return CodedOutputByteBufferNano.computeSInt64Size(tagFieldNumber, ((Long) obj).longValue());
            }
        }
    }
}
