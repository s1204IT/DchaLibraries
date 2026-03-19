package com.android.internal.telephony.nano;

import com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.ExtendableMessageNano;
import com.android.internal.telephony.protobuf.nano.InternalNano;
import com.android.internal.telephony.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.internal.telephony.protobuf.nano.MessageNano;
import com.android.internal.telephony.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface CarrierIdProto {

    public static final class CarrierList extends ExtendableMessageNano<CarrierList> {
        private static volatile CarrierList[] _emptyArray;
        public CarrierId[] carrierId;
        public int version;

        public static CarrierList[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CarrierList[0];
                    }
                }
            }
            return _emptyArray;
        }

        public CarrierList() {
            clear();
        }

        public CarrierList clear() {
            this.carrierId = CarrierId.emptyArray();
            this.version = 0;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.carrierId != null && this.carrierId.length > 0) {
                for (int i = 0; i < this.carrierId.length; i++) {
                    CarrierId carrierId = this.carrierId[i];
                    if (carrierId != null) {
                        codedOutputByteBufferNano.writeMessage(1, carrierId);
                    }
                }
            }
            if (this.version != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.version);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.carrierId != null && this.carrierId.length > 0) {
                for (int i = 0; i < this.carrierId.length; i++) {
                    CarrierId carrierId = this.carrierId[i];
                    if (carrierId != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(1, carrierId);
                    }
                }
            }
            if (this.version != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(2, this.version);
            }
            return iComputeSerializedSize;
        }

        @Override
        public CarrierList mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 10);
                    if (this.carrierId != null) {
                        length = this.carrierId.length;
                    } else {
                        length = 0;
                    }
                    CarrierId[] carrierIdArr = new CarrierId[repeatedFieldArrayLength + length];
                    if (length != 0) {
                        System.arraycopy(this.carrierId, 0, carrierIdArr, 0, length);
                    }
                    while (length < carrierIdArr.length - 1) {
                        carrierIdArr[length] = new CarrierId();
                        codedInputByteBufferNano.readMessage(carrierIdArr[length]);
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    carrierIdArr[length] = new CarrierId();
                    codedInputByteBufferNano.readMessage(carrierIdArr[length]);
                    this.carrierId = carrierIdArr;
                } else if (tag != 16) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.version = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static CarrierList parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (CarrierList) MessageNano.mergeFrom(new CarrierList(), bArr);
        }

        public static CarrierList parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new CarrierList().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class CarrierId extends ExtendableMessageNano<CarrierId> {
        private static volatile CarrierId[] _emptyArray;
        public int canonicalId;
        public CarrierAttribute[] carrierAttribute;
        public String carrierName;

        public static CarrierId[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CarrierId[0];
                    }
                }
            }
            return _emptyArray;
        }

        public CarrierId() {
            clear();
        }

        public CarrierId clear() {
            this.canonicalId = 0;
            this.carrierName = "";
            this.carrierAttribute = CarrierAttribute.emptyArray();
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.canonicalId != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.canonicalId);
            }
            if (!this.carrierName.equals("")) {
                codedOutputByteBufferNano.writeString(2, this.carrierName);
            }
            if (this.carrierAttribute != null && this.carrierAttribute.length > 0) {
                for (int i = 0; i < this.carrierAttribute.length; i++) {
                    CarrierAttribute carrierAttribute = this.carrierAttribute[i];
                    if (carrierAttribute != null) {
                        codedOutputByteBufferNano.writeMessage(3, carrierAttribute);
                    }
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.canonicalId != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.canonicalId);
            }
            if (!this.carrierName.equals("")) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(2, this.carrierName);
            }
            if (this.carrierAttribute != null && this.carrierAttribute.length > 0) {
                for (int i = 0; i < this.carrierAttribute.length; i++) {
                    CarrierAttribute carrierAttribute = this.carrierAttribute[i];
                    if (carrierAttribute != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(3, carrierAttribute);
                    }
                }
            }
            return iComputeSerializedSize;
        }

        @Override
        public CarrierId mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.canonicalId = codedInputByteBufferNano.readInt32();
                } else if (tag == 18) {
                    this.carrierName = codedInputByteBufferNano.readString();
                } else if (tag != 26) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 26);
                    if (this.carrierAttribute != null) {
                        length = this.carrierAttribute.length;
                    } else {
                        length = 0;
                    }
                    CarrierAttribute[] carrierAttributeArr = new CarrierAttribute[repeatedFieldArrayLength + length];
                    if (length != 0) {
                        System.arraycopy(this.carrierAttribute, 0, carrierAttributeArr, 0, length);
                    }
                    while (length < carrierAttributeArr.length - 1) {
                        carrierAttributeArr[length] = new CarrierAttribute();
                        codedInputByteBufferNano.readMessage(carrierAttributeArr[length]);
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    carrierAttributeArr[length] = new CarrierAttribute();
                    codedInputByteBufferNano.readMessage(carrierAttributeArr[length]);
                    this.carrierAttribute = carrierAttributeArr;
                }
            }
        }

        public static CarrierId parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (CarrierId) MessageNano.mergeFrom(new CarrierId(), bArr);
        }

        public static CarrierId parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new CarrierId().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class CarrierAttribute extends ExtendableMessageNano<CarrierAttribute> {
        private static volatile CarrierAttribute[] _emptyArray;
        public String[] gid1;
        public String[] gid2;
        public String[] iccidPrefix;
        public String[] imsiPrefixXpattern;
        public String[] mccmncTuple;
        public String[] plmn;
        public String[] preferredApn;
        public String[] spn;

        public static CarrierAttribute[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CarrierAttribute[0];
                    }
                }
            }
            return _emptyArray;
        }

        public CarrierAttribute() {
            clear();
        }

        public CarrierAttribute clear() {
            this.mccmncTuple = WireFormatNano.EMPTY_STRING_ARRAY;
            this.imsiPrefixXpattern = WireFormatNano.EMPTY_STRING_ARRAY;
            this.spn = WireFormatNano.EMPTY_STRING_ARRAY;
            this.plmn = WireFormatNano.EMPTY_STRING_ARRAY;
            this.gid1 = WireFormatNano.EMPTY_STRING_ARRAY;
            this.gid2 = WireFormatNano.EMPTY_STRING_ARRAY;
            this.preferredApn = WireFormatNano.EMPTY_STRING_ARRAY;
            this.iccidPrefix = WireFormatNano.EMPTY_STRING_ARRAY;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.mccmncTuple != null && this.mccmncTuple.length > 0) {
                for (int i = 0; i < this.mccmncTuple.length; i++) {
                    String str = this.mccmncTuple[i];
                    if (str != null) {
                        codedOutputByteBufferNano.writeString(1, str);
                    }
                }
            }
            if (this.imsiPrefixXpattern != null && this.imsiPrefixXpattern.length > 0) {
                for (int i2 = 0; i2 < this.imsiPrefixXpattern.length; i2++) {
                    String str2 = this.imsiPrefixXpattern[i2];
                    if (str2 != null) {
                        codedOutputByteBufferNano.writeString(2, str2);
                    }
                }
            }
            if (this.spn != null && this.spn.length > 0) {
                for (int i3 = 0; i3 < this.spn.length; i3++) {
                    String str3 = this.spn[i3];
                    if (str3 != null) {
                        codedOutputByteBufferNano.writeString(3, str3);
                    }
                }
            }
            if (this.plmn != null && this.plmn.length > 0) {
                for (int i4 = 0; i4 < this.plmn.length; i4++) {
                    String str4 = this.plmn[i4];
                    if (str4 != null) {
                        codedOutputByteBufferNano.writeString(4, str4);
                    }
                }
            }
            if (this.gid1 != null && this.gid1.length > 0) {
                for (int i5 = 0; i5 < this.gid1.length; i5++) {
                    String str5 = this.gid1[i5];
                    if (str5 != null) {
                        codedOutputByteBufferNano.writeString(5, str5);
                    }
                }
            }
            if (this.gid2 != null && this.gid2.length > 0) {
                for (int i6 = 0; i6 < this.gid2.length; i6++) {
                    String str6 = this.gid2[i6];
                    if (str6 != null) {
                        codedOutputByteBufferNano.writeString(6, str6);
                    }
                }
            }
            if (this.preferredApn != null && this.preferredApn.length > 0) {
                for (int i7 = 0; i7 < this.preferredApn.length; i7++) {
                    String str7 = this.preferredApn[i7];
                    if (str7 != null) {
                        codedOutputByteBufferNano.writeString(7, str7);
                    }
                }
            }
            if (this.iccidPrefix != null && this.iccidPrefix.length > 0) {
                for (int i8 = 0; i8 < this.iccidPrefix.length; i8++) {
                    String str8 = this.iccidPrefix[i8];
                    if (str8 != null) {
                        codedOutputByteBufferNano.writeString(8, str8);
                    }
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.mccmncTuple != null && this.mccmncTuple.length > 0) {
                int iComputeStringSizeNoTag = 0;
                int i = 0;
                for (int i2 = 0; i2 < this.mccmncTuple.length; i2++) {
                    String str = this.mccmncTuple[i2];
                    if (str != null) {
                        i++;
                        iComputeStringSizeNoTag += CodedOutputByteBufferNano.computeStringSizeNoTag(str);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag + (i * 1);
            }
            if (this.imsiPrefixXpattern != null && this.imsiPrefixXpattern.length > 0) {
                int iComputeStringSizeNoTag2 = 0;
                int i3 = 0;
                for (int i4 = 0; i4 < this.imsiPrefixXpattern.length; i4++) {
                    String str2 = this.imsiPrefixXpattern[i4];
                    if (str2 != null) {
                        i3++;
                        iComputeStringSizeNoTag2 += CodedOutputByteBufferNano.computeStringSizeNoTag(str2);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag2 + (i3 * 1);
            }
            if (this.spn != null && this.spn.length > 0) {
                int iComputeStringSizeNoTag3 = 0;
                int i5 = 0;
                for (int i6 = 0; i6 < this.spn.length; i6++) {
                    String str3 = this.spn[i6];
                    if (str3 != null) {
                        i5++;
                        iComputeStringSizeNoTag3 += CodedOutputByteBufferNano.computeStringSizeNoTag(str3);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag3 + (i5 * 1);
            }
            if (this.plmn != null && this.plmn.length > 0) {
                int iComputeStringSizeNoTag4 = 0;
                int i7 = 0;
                for (int i8 = 0; i8 < this.plmn.length; i8++) {
                    String str4 = this.plmn[i8];
                    if (str4 != null) {
                        i7++;
                        iComputeStringSizeNoTag4 += CodedOutputByteBufferNano.computeStringSizeNoTag(str4);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag4 + (i7 * 1);
            }
            if (this.gid1 != null && this.gid1.length > 0) {
                int iComputeStringSizeNoTag5 = 0;
                int i9 = 0;
                for (int i10 = 0; i10 < this.gid1.length; i10++) {
                    String str5 = this.gid1[i10];
                    if (str5 != null) {
                        i9++;
                        iComputeStringSizeNoTag5 += CodedOutputByteBufferNano.computeStringSizeNoTag(str5);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag5 + (i9 * 1);
            }
            if (this.gid2 != null && this.gid2.length > 0) {
                int iComputeStringSizeNoTag6 = 0;
                int i11 = 0;
                for (int i12 = 0; i12 < this.gid2.length; i12++) {
                    String str6 = this.gid2[i12];
                    if (str6 != null) {
                        i11++;
                        iComputeStringSizeNoTag6 += CodedOutputByteBufferNano.computeStringSizeNoTag(str6);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag6 + (i11 * 1);
            }
            if (this.preferredApn != null && this.preferredApn.length > 0) {
                int iComputeStringSizeNoTag7 = 0;
                int i13 = 0;
                for (int i14 = 0; i14 < this.preferredApn.length; i14++) {
                    String str7 = this.preferredApn[i14];
                    if (str7 != null) {
                        i13++;
                        iComputeStringSizeNoTag7 += CodedOutputByteBufferNano.computeStringSizeNoTag(str7);
                    }
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeStringSizeNoTag7 + (i13 * 1);
            }
            if (this.iccidPrefix != null && this.iccidPrefix.length > 0) {
                int iComputeStringSizeNoTag8 = 0;
                int i15 = 0;
                for (int i16 = 0; i16 < this.iccidPrefix.length; i16++) {
                    String str8 = this.iccidPrefix[i16];
                    if (str8 != null) {
                        i15++;
                        iComputeStringSizeNoTag8 += CodedOutputByteBufferNano.computeStringSizeNoTag(str8);
                    }
                }
                return iComputeSerializedSize + iComputeStringSizeNoTag8 + (1 * i15);
            }
            return iComputeSerializedSize;
        }

        @Override
        public CarrierAttribute mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            int length3;
            int length4;
            int length5;
            int length6;
            int length7;
            int length8;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 10);
                    if (this.mccmncTuple != null) {
                        length8 = this.mccmncTuple.length;
                    } else {
                        length8 = 0;
                    }
                    String[] strArr = new String[repeatedFieldArrayLength + length8];
                    if (length8 != 0) {
                        System.arraycopy(this.mccmncTuple, 0, strArr, 0, length8);
                    }
                    while (length8 < strArr.length - 1) {
                        strArr[length8] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length8++;
                    }
                    strArr[length8] = codedInputByteBufferNano.readString();
                    this.mccmncTuple = strArr;
                } else if (tag == 18) {
                    int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 18);
                    if (this.imsiPrefixXpattern != null) {
                        length7 = this.imsiPrefixXpattern.length;
                    } else {
                        length7 = 0;
                    }
                    String[] strArr2 = new String[repeatedFieldArrayLength2 + length7];
                    if (length7 != 0) {
                        System.arraycopy(this.imsiPrefixXpattern, 0, strArr2, 0, length7);
                    }
                    while (length7 < strArr2.length - 1) {
                        strArr2[length7] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length7++;
                    }
                    strArr2[length7] = codedInputByteBufferNano.readString();
                    this.imsiPrefixXpattern = strArr2;
                } else if (tag == 26) {
                    int repeatedFieldArrayLength3 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 26);
                    if (this.spn != null) {
                        length6 = this.spn.length;
                    } else {
                        length6 = 0;
                    }
                    String[] strArr3 = new String[repeatedFieldArrayLength3 + length6];
                    if (length6 != 0) {
                        System.arraycopy(this.spn, 0, strArr3, 0, length6);
                    }
                    while (length6 < strArr3.length - 1) {
                        strArr3[length6] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length6++;
                    }
                    strArr3[length6] = codedInputByteBufferNano.readString();
                    this.spn = strArr3;
                } else if (tag == 34) {
                    int repeatedFieldArrayLength4 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 34);
                    if (this.plmn != null) {
                        length5 = this.plmn.length;
                    } else {
                        length5 = 0;
                    }
                    String[] strArr4 = new String[repeatedFieldArrayLength4 + length5];
                    if (length5 != 0) {
                        System.arraycopy(this.plmn, 0, strArr4, 0, length5);
                    }
                    while (length5 < strArr4.length - 1) {
                        strArr4[length5] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length5++;
                    }
                    strArr4[length5] = codedInputByteBufferNano.readString();
                    this.plmn = strArr4;
                } else if (tag == 42) {
                    int repeatedFieldArrayLength5 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 42);
                    if (this.gid1 != null) {
                        length4 = this.gid1.length;
                    } else {
                        length4 = 0;
                    }
                    String[] strArr5 = new String[repeatedFieldArrayLength5 + length4];
                    if (length4 != 0) {
                        System.arraycopy(this.gid1, 0, strArr5, 0, length4);
                    }
                    while (length4 < strArr5.length - 1) {
                        strArr5[length4] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length4++;
                    }
                    strArr5[length4] = codedInputByteBufferNano.readString();
                    this.gid1 = strArr5;
                } else if (tag == 50) {
                    int repeatedFieldArrayLength6 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 50);
                    if (this.gid2 != null) {
                        length3 = this.gid2.length;
                    } else {
                        length3 = 0;
                    }
                    String[] strArr6 = new String[repeatedFieldArrayLength6 + length3];
                    if (length3 != 0) {
                        System.arraycopy(this.gid2, 0, strArr6, 0, length3);
                    }
                    while (length3 < strArr6.length - 1) {
                        strArr6[length3] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length3++;
                    }
                    strArr6[length3] = codedInputByteBufferNano.readString();
                    this.gid2 = strArr6;
                } else if (tag == 58) {
                    int repeatedFieldArrayLength7 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 58);
                    if (this.preferredApn != null) {
                        length2 = this.preferredApn.length;
                    } else {
                        length2 = 0;
                    }
                    String[] strArr7 = new String[repeatedFieldArrayLength7 + length2];
                    if (length2 != 0) {
                        System.arraycopy(this.preferredApn, 0, strArr7, 0, length2);
                    }
                    while (length2 < strArr7.length - 1) {
                        strArr7[length2] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length2++;
                    }
                    strArr7[length2] = codedInputByteBufferNano.readString();
                    this.preferredApn = strArr7;
                } else if (tag != 66) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    int repeatedFieldArrayLength8 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 66);
                    if (this.iccidPrefix != null) {
                        length = this.iccidPrefix.length;
                    } else {
                        length = 0;
                    }
                    String[] strArr8 = new String[repeatedFieldArrayLength8 + length];
                    if (length != 0) {
                        System.arraycopy(this.iccidPrefix, 0, strArr8, 0, length);
                    }
                    while (length < strArr8.length - 1) {
                        strArr8[length] = codedInputByteBufferNano.readString();
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    strArr8[length] = codedInputByteBufferNano.readString();
                    this.iccidPrefix = strArr8;
                }
            }
        }

        public static CarrierAttribute parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (CarrierAttribute) MessageNano.mergeFrom(new CarrierAttribute(), bArr);
        }

        public static CarrierAttribute parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new CarrierAttribute().mergeFrom(codedInputByteBufferNano);
        }
    }
}
