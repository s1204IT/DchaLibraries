package com.android.server.connectivity.metrics.nano;

import com.android.framework.protobuf.nano.CodedInputByteBufferNano;
import com.android.framework.protobuf.nano.CodedOutputByteBufferNano;
import com.android.framework.protobuf.nano.InternalNano;
import com.android.framework.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.framework.protobuf.nano.MessageNano;
import com.android.framework.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface IpConnectivityLogClass {
    public static final int BLUETOOTH = 1;
    public static final int CELLULAR = 2;
    public static final int ETHERNET = 3;
    public static final int LOWPAN = 9;
    public static final int MULTIPLE = 6;
    public static final int NONE = 5;
    public static final int UNKNOWN = 0;
    public static final int WIFI = 4;
    public static final int WIFI_NAN = 8;
    public static final int WIFI_P2P = 7;

    public static final class NetworkId extends MessageNano {
        private static volatile NetworkId[] _emptyArray;
        public int networkId;

        public static NetworkId[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new NetworkId[0];
                    }
                }
            }
            return _emptyArray;
        }

        public NetworkId() {
            clear();
        }

        public NetworkId clear() {
            this.networkId = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.networkId != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.networkId);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.networkId != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(1, this.networkId);
            }
            return iComputeSerializedSize;
        }

        @Override
        public NetworkId mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag != 8) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.networkId = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static NetworkId parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (NetworkId) MessageNano.mergeFrom(new NetworkId(), bArr);
        }

        public static NetworkId parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new NetworkId().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class Pair extends MessageNano {
        private static volatile Pair[] _emptyArray;
        public int key;
        public int value;

        public static Pair[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Pair[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Pair() {
            clear();
        }

        public Pair clear() {
            this.key = 0;
            this.value = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.key != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.key);
            }
            if (this.value != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.value);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.key != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.key);
            }
            if (this.value != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(2, this.value);
            }
            return iComputeSerializedSize;
        }

        @Override
        public Pair mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.key = codedInputByteBufferNano.readInt32();
                } else if (tag != 16) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.value = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static Pair parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (Pair) MessageNano.mergeFrom(new Pair(), bArr);
        }

        public static Pair parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new Pair().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class DefaultNetworkEvent extends MessageNano {
        public static final int DISCONNECT = 3;
        public static final int DUAL = 3;
        public static final int INVALIDATION = 2;
        public static final int IPV4 = 1;
        public static final int IPV6 = 2;
        public static final int NONE = 0;
        public static final int OUTSCORED = 1;
        public static final int UNKNOWN = 0;
        private static volatile DefaultNetworkEvent[] _emptyArray;
        public long defaultNetworkDurationMs;
        public long finalScore;
        public long initialScore;
        public int ipSupport;
        public NetworkId networkId;
        public long noDefaultNetworkDurationMs;
        public int previousDefaultNetworkLinkLayer;
        public NetworkId previousNetworkId;
        public int previousNetworkIpSupport;
        public int[] transportTypes;
        public long validationDurationMs;

        public static DefaultNetworkEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new DefaultNetworkEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public DefaultNetworkEvent() {
            clear();
        }

        public DefaultNetworkEvent clear() {
            this.defaultNetworkDurationMs = 0L;
            this.validationDurationMs = 0L;
            this.initialScore = 0L;
            this.finalScore = 0L;
            this.ipSupport = 0;
            this.previousDefaultNetworkLinkLayer = 0;
            this.networkId = null;
            this.previousNetworkId = null;
            this.previousNetworkIpSupport = 0;
            this.transportTypes = WireFormatNano.EMPTY_INT_ARRAY;
            this.noDefaultNetworkDurationMs = 0L;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.networkId != null) {
                codedOutputByteBufferNano.writeMessage(1, this.networkId);
            }
            if (this.previousNetworkId != null) {
                codedOutputByteBufferNano.writeMessage(2, this.previousNetworkId);
            }
            if (this.previousNetworkIpSupport != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.previousNetworkIpSupport);
            }
            if (this.transportTypes != null && this.transportTypes.length > 0) {
                for (int i = 0; i < this.transportTypes.length; i++) {
                    codedOutputByteBufferNano.writeInt32(4, this.transportTypes[i]);
                }
            }
            if (this.defaultNetworkDurationMs != 0) {
                codedOutputByteBufferNano.writeInt64(5, this.defaultNetworkDurationMs);
            }
            if (this.noDefaultNetworkDurationMs != 0) {
                codedOutputByteBufferNano.writeInt64(6, this.noDefaultNetworkDurationMs);
            }
            if (this.initialScore != 0) {
                codedOutputByteBufferNano.writeInt64(7, this.initialScore);
            }
            if (this.finalScore != 0) {
                codedOutputByteBufferNano.writeInt64(8, this.finalScore);
            }
            if (this.ipSupport != 0) {
                codedOutputByteBufferNano.writeInt32(9, this.ipSupport);
            }
            if (this.previousDefaultNetworkLinkLayer != 0) {
                codedOutputByteBufferNano.writeInt32(10, this.previousDefaultNetworkLinkLayer);
            }
            if (this.validationDurationMs != 0) {
                codedOutputByteBufferNano.writeInt64(11, this.validationDurationMs);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.networkId != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(1, this.networkId);
            }
            if (this.previousNetworkId != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(2, this.previousNetworkId);
            }
            if (this.previousNetworkIpSupport != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.previousNetworkIpSupport);
            }
            if (this.transportTypes != null && this.transportTypes.length > 0) {
                int iComputeInt32SizeNoTag = 0;
                for (int i = 0; i < this.transportTypes.length; i++) {
                    iComputeInt32SizeNoTag += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.transportTypes[i]);
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeInt32SizeNoTag + (1 * this.transportTypes.length);
            }
            if (this.defaultNetworkDurationMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(5, this.defaultNetworkDurationMs);
            }
            if (this.noDefaultNetworkDurationMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(6, this.noDefaultNetworkDurationMs);
            }
            if (this.initialScore != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(7, this.initialScore);
            }
            if (this.finalScore != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(8, this.finalScore);
            }
            if (this.ipSupport != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(9, this.ipSupport);
            }
            if (this.previousDefaultNetworkLinkLayer != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(10, this.previousDefaultNetworkLinkLayer);
            }
            if (this.validationDurationMs != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(11, this.validationDurationMs);
            }
            return iComputeSerializedSize;
        }

        @Override
        public DefaultNetworkEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 10:
                        if (this.networkId == null) {
                            this.networkId = new NetworkId();
                        }
                        codedInputByteBufferNano.readMessage(this.networkId);
                        break;
                    case 18:
                        if (this.previousNetworkId == null) {
                            this.previousNetworkId = new NetworkId();
                        }
                        codedInputByteBufferNano.readMessage(this.previousNetworkId);
                        break;
                    case 24:
                        int int32 = codedInputByteBufferNano.readInt32();
                        switch (int32) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.previousNetworkIpSupport = int32;
                                break;
                        }
                        break;
                    case 32:
                        int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 32);
                        if (this.transportTypes != null) {
                            length = this.transportTypes.length;
                        } else {
                            length = 0;
                        }
                        int[] iArr = new int[repeatedFieldArrayLength + length];
                        if (length != 0) {
                            System.arraycopy(this.transportTypes, 0, iArr, 0, length);
                        }
                        while (length < iArr.length - 1) {
                            iArr[length] = codedInputByteBufferNano.readInt32();
                            codedInputByteBufferNano.readTag();
                            length++;
                        }
                        iArr[length] = codedInputByteBufferNano.readInt32();
                        this.transportTypes = iArr;
                        break;
                    case 34:
                        int iPushLimit = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                        int position = codedInputByteBufferNano.getPosition();
                        int i = 0;
                        while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                            codedInputByteBufferNano.readInt32();
                            i++;
                        }
                        codedInputByteBufferNano.rewindToPosition(position);
                        if (this.transportTypes != null) {
                            length2 = this.transportTypes.length;
                        } else {
                            length2 = 0;
                        }
                        int[] iArr2 = new int[i + length2];
                        if (length2 != 0) {
                            System.arraycopy(this.transportTypes, 0, iArr2, 0, length2);
                        }
                        while (length2 < iArr2.length) {
                            iArr2[length2] = codedInputByteBufferNano.readInt32();
                            length2++;
                        }
                        this.transportTypes = iArr2;
                        codedInputByteBufferNano.popLimit(iPushLimit);
                        break;
                    case 40:
                        this.defaultNetworkDurationMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 48:
                        this.noDefaultNetworkDurationMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 56:
                        this.initialScore = codedInputByteBufferNano.readInt64();
                        break;
                    case 64:
                        this.finalScore = codedInputByteBufferNano.readInt64();
                        break;
                    case 72:
                        int int322 = codedInputByteBufferNano.readInt32();
                        switch (int322) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.ipSupport = int322;
                                break;
                        }
                        break;
                    case 80:
                        int int323 = codedInputByteBufferNano.readInt32();
                        switch (int323) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                this.previousDefaultNetworkLinkLayer = int323;
                                break;
                        }
                        break;
                    case 88:
                        this.validationDurationMs = codedInputByteBufferNano.readInt64();
                        break;
                    default:
                        if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static DefaultNetworkEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (DefaultNetworkEvent) MessageNano.mergeFrom(new DefaultNetworkEvent(), bArr);
        }

        public static DefaultNetworkEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new DefaultNetworkEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class IpReachabilityEvent extends MessageNano {
        private static volatile IpReachabilityEvent[] _emptyArray;
        public int eventType;
        public String ifName;

        public static IpReachabilityEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new IpReachabilityEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public IpReachabilityEvent() {
            clear();
        }

        public IpReachabilityEvent clear() {
            this.ifName = "";
            this.eventType = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (!this.ifName.equals("")) {
                codedOutputByteBufferNano.writeString(1, this.ifName);
            }
            if (this.eventType != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.eventType);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (!this.ifName.equals("")) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(1, this.ifName);
            }
            if (this.eventType != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(2, this.eventType);
            }
            return iComputeSerializedSize;
        }

        @Override
        public IpReachabilityEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    this.ifName = codedInputByteBufferNano.readString();
                } else if (tag != 16) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.eventType = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static IpReachabilityEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (IpReachabilityEvent) MessageNano.mergeFrom(new IpReachabilityEvent(), bArr);
        }

        public static IpReachabilityEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new IpReachabilityEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class NetworkEvent extends MessageNano {
        private static volatile NetworkEvent[] _emptyArray;
        public int eventType;
        public int latencyMs;
        public NetworkId networkId;

        public static NetworkEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new NetworkEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public NetworkEvent() {
            clear();
        }

        public NetworkEvent clear() {
            this.networkId = null;
            this.eventType = 0;
            this.latencyMs = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.networkId != null) {
                codedOutputByteBufferNano.writeMessage(1, this.networkId);
            }
            if (this.eventType != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.eventType);
            }
            if (this.latencyMs != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.latencyMs);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.networkId != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(1, this.networkId);
            }
            if (this.eventType != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.eventType);
            }
            if (this.latencyMs != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(3, this.latencyMs);
            }
            return iComputeSerializedSize;
        }

        @Override
        public NetworkEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    if (this.networkId == null) {
                        this.networkId = new NetworkId();
                    }
                    codedInputByteBufferNano.readMessage(this.networkId);
                } else if (tag == 16) {
                    this.eventType = codedInputByteBufferNano.readInt32();
                } else if (tag != 24) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.latencyMs = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static NetworkEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (NetworkEvent) MessageNano.mergeFrom(new NetworkEvent(), bArr);
        }

        public static NetworkEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new NetworkEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ValidationProbeEvent extends MessageNano {
        private static volatile ValidationProbeEvent[] _emptyArray;
        public int latencyMs;
        public NetworkId networkId;
        public int probeResult;
        public int probeType;

        public static ValidationProbeEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ValidationProbeEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ValidationProbeEvent() {
            clear();
        }

        public ValidationProbeEvent clear() {
            this.networkId = null;
            this.latencyMs = 0;
            this.probeType = 0;
            this.probeResult = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.networkId != null) {
                codedOutputByteBufferNano.writeMessage(1, this.networkId);
            }
            if (this.latencyMs != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.latencyMs);
            }
            if (this.probeType != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.probeType);
            }
            if (this.probeResult != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.probeResult);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.networkId != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(1, this.networkId);
            }
            if (this.latencyMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.latencyMs);
            }
            if (this.probeType != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.probeType);
            }
            if (this.probeResult != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(4, this.probeResult);
            }
            return iComputeSerializedSize;
        }

        @Override
        public ValidationProbeEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    if (this.networkId == null) {
                        this.networkId = new NetworkId();
                    }
                    codedInputByteBufferNano.readMessage(this.networkId);
                } else if (tag == 16) {
                    this.latencyMs = codedInputByteBufferNano.readInt32();
                } else if (tag == 24) {
                    this.probeType = codedInputByteBufferNano.readInt32();
                } else if (tag != 32) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.probeResult = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static ValidationProbeEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ValidationProbeEvent) MessageNano.mergeFrom(new ValidationProbeEvent(), bArr);
        }

        public static ValidationProbeEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ValidationProbeEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class DNSLookupBatch extends MessageNano {
        private static volatile DNSLookupBatch[] _emptyArray;
        public int[] eventTypes;
        public long getaddrinfoErrorCount;
        public Pair[] getaddrinfoErrors;
        public long getaddrinfoQueryCount;
        public long gethostbynameErrorCount;
        public Pair[] gethostbynameErrors;
        public long gethostbynameQueryCount;
        public int[] latenciesMs;
        public NetworkId networkId;
        public int[] returnCodes;

        public static DNSLookupBatch[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new DNSLookupBatch[0];
                    }
                }
            }
            return _emptyArray;
        }

        public DNSLookupBatch() {
            clear();
        }

        public DNSLookupBatch clear() {
            this.latenciesMs = WireFormatNano.EMPTY_INT_ARRAY;
            this.getaddrinfoQueryCount = 0L;
            this.gethostbynameQueryCount = 0L;
            this.getaddrinfoErrorCount = 0L;
            this.gethostbynameErrorCount = 0L;
            this.getaddrinfoErrors = Pair.emptyArray();
            this.gethostbynameErrors = Pair.emptyArray();
            this.networkId = null;
            this.eventTypes = WireFormatNano.EMPTY_INT_ARRAY;
            this.returnCodes = WireFormatNano.EMPTY_INT_ARRAY;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.networkId != null) {
                codedOutputByteBufferNano.writeMessage(1, this.networkId);
            }
            if (this.eventTypes != null && this.eventTypes.length > 0) {
                for (int i = 0; i < this.eventTypes.length; i++) {
                    codedOutputByteBufferNano.writeInt32(2, this.eventTypes[i]);
                }
            }
            if (this.returnCodes != null && this.returnCodes.length > 0) {
                for (int i2 = 0; i2 < this.returnCodes.length; i2++) {
                    codedOutputByteBufferNano.writeInt32(3, this.returnCodes[i2]);
                }
            }
            if (this.latenciesMs != null && this.latenciesMs.length > 0) {
                for (int i3 = 0; i3 < this.latenciesMs.length; i3++) {
                    codedOutputByteBufferNano.writeInt32(4, this.latenciesMs[i3]);
                }
            }
            if (this.getaddrinfoQueryCount != 0) {
                codedOutputByteBufferNano.writeInt64(5, this.getaddrinfoQueryCount);
            }
            if (this.gethostbynameQueryCount != 0) {
                codedOutputByteBufferNano.writeInt64(6, this.gethostbynameQueryCount);
            }
            if (this.getaddrinfoErrorCount != 0) {
                codedOutputByteBufferNano.writeInt64(7, this.getaddrinfoErrorCount);
            }
            if (this.gethostbynameErrorCount != 0) {
                codedOutputByteBufferNano.writeInt64(8, this.gethostbynameErrorCount);
            }
            if (this.getaddrinfoErrors != null && this.getaddrinfoErrors.length > 0) {
                for (int i4 = 0; i4 < this.getaddrinfoErrors.length; i4++) {
                    Pair pair = this.getaddrinfoErrors[i4];
                    if (pair != null) {
                        codedOutputByteBufferNano.writeMessage(9, pair);
                    }
                }
            }
            if (this.gethostbynameErrors != null && this.gethostbynameErrors.length > 0) {
                for (int i5 = 0; i5 < this.gethostbynameErrors.length; i5++) {
                    Pair pair2 = this.gethostbynameErrors[i5];
                    if (pair2 != null) {
                        codedOutputByteBufferNano.writeMessage(10, pair2);
                    }
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.networkId != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(1, this.networkId);
            }
            if (this.eventTypes != null && this.eventTypes.length > 0) {
                int iComputeInt32SizeNoTag = 0;
                for (int i = 0; i < this.eventTypes.length; i++) {
                    iComputeInt32SizeNoTag += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.eventTypes[i]);
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeInt32SizeNoTag + (this.eventTypes.length * 1);
            }
            if (this.returnCodes != null && this.returnCodes.length > 0) {
                int iComputeInt32SizeNoTag2 = 0;
                for (int i2 = 0; i2 < this.returnCodes.length; i2++) {
                    iComputeInt32SizeNoTag2 += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.returnCodes[i2]);
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeInt32SizeNoTag2 + (this.returnCodes.length * 1);
            }
            if (this.latenciesMs != null && this.latenciesMs.length > 0) {
                int iComputeInt32SizeNoTag3 = 0;
                for (int i3 = 0; i3 < this.latenciesMs.length; i3++) {
                    iComputeInt32SizeNoTag3 += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.latenciesMs[i3]);
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeInt32SizeNoTag3 + (1 * this.latenciesMs.length);
            }
            if (this.getaddrinfoQueryCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(5, this.getaddrinfoQueryCount);
            }
            if (this.gethostbynameQueryCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(6, this.gethostbynameQueryCount);
            }
            if (this.getaddrinfoErrorCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(7, this.getaddrinfoErrorCount);
            }
            if (this.gethostbynameErrorCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(8, this.gethostbynameErrorCount);
            }
            if (this.getaddrinfoErrors != null && this.getaddrinfoErrors.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i4 = 0; i4 < this.getaddrinfoErrors.length; i4++) {
                    Pair pair = this.getaddrinfoErrors[i4];
                    if (pair != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(9, pair);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.gethostbynameErrors != null && this.gethostbynameErrors.length > 0) {
                for (int i5 = 0; i5 < this.gethostbynameErrors.length; i5++) {
                    Pair pair2 = this.gethostbynameErrors[i5];
                    if (pair2 != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(10, pair2);
                    }
                }
            }
            return iComputeSerializedSize;
        }

        @Override
        public DNSLookupBatch mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
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
                switch (tag) {
                    case 0:
                        return this;
                    case 10:
                        if (this.networkId == null) {
                            this.networkId = new NetworkId();
                        }
                        codedInputByteBufferNano.readMessage(this.networkId);
                        break;
                    case 16:
                        int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 16);
                        if (this.eventTypes != null) {
                            length = this.eventTypes.length;
                        } else {
                            length = 0;
                        }
                        int[] iArr = new int[repeatedFieldArrayLength + length];
                        if (length != 0) {
                            System.arraycopy(this.eventTypes, 0, iArr, 0, length);
                        }
                        while (length < iArr.length - 1) {
                            iArr[length] = codedInputByteBufferNano.readInt32();
                            codedInputByteBufferNano.readTag();
                            length++;
                        }
                        iArr[length] = codedInputByteBufferNano.readInt32();
                        this.eventTypes = iArr;
                        break;
                    case 18:
                        int iPushLimit = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                        int position = codedInputByteBufferNano.getPosition();
                        int i = 0;
                        while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                            codedInputByteBufferNano.readInt32();
                            i++;
                        }
                        codedInputByteBufferNano.rewindToPosition(position);
                        if (this.eventTypes != null) {
                            length2 = this.eventTypes.length;
                        } else {
                            length2 = 0;
                        }
                        int[] iArr2 = new int[i + length2];
                        if (length2 != 0) {
                            System.arraycopy(this.eventTypes, 0, iArr2, 0, length2);
                        }
                        while (length2 < iArr2.length) {
                            iArr2[length2] = codedInputByteBufferNano.readInt32();
                            length2++;
                        }
                        this.eventTypes = iArr2;
                        codedInputByteBufferNano.popLimit(iPushLimit);
                        break;
                    case 24:
                        int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 24);
                        if (this.returnCodes != null) {
                            length3 = this.returnCodes.length;
                        } else {
                            length3 = 0;
                        }
                        int[] iArr3 = new int[repeatedFieldArrayLength2 + length3];
                        if (length3 != 0) {
                            System.arraycopy(this.returnCodes, 0, iArr3, 0, length3);
                        }
                        while (length3 < iArr3.length - 1) {
                            iArr3[length3] = codedInputByteBufferNano.readInt32();
                            codedInputByteBufferNano.readTag();
                            length3++;
                        }
                        iArr3[length3] = codedInputByteBufferNano.readInt32();
                        this.returnCodes = iArr3;
                        break;
                    case 26:
                        int iPushLimit2 = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                        int position2 = codedInputByteBufferNano.getPosition();
                        int i2 = 0;
                        while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                            codedInputByteBufferNano.readInt32();
                            i2++;
                        }
                        codedInputByteBufferNano.rewindToPosition(position2);
                        if (this.returnCodes != null) {
                            length4 = this.returnCodes.length;
                        } else {
                            length4 = 0;
                        }
                        int[] iArr4 = new int[i2 + length4];
                        if (length4 != 0) {
                            System.arraycopy(this.returnCodes, 0, iArr4, 0, length4);
                        }
                        while (length4 < iArr4.length) {
                            iArr4[length4] = codedInputByteBufferNano.readInt32();
                            length4++;
                        }
                        this.returnCodes = iArr4;
                        codedInputByteBufferNano.popLimit(iPushLimit2);
                        break;
                    case 32:
                        int repeatedFieldArrayLength3 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 32);
                        if (this.latenciesMs != null) {
                            length5 = this.latenciesMs.length;
                        } else {
                            length5 = 0;
                        }
                        int[] iArr5 = new int[repeatedFieldArrayLength3 + length5];
                        if (length5 != 0) {
                            System.arraycopy(this.latenciesMs, 0, iArr5, 0, length5);
                        }
                        while (length5 < iArr5.length - 1) {
                            iArr5[length5] = codedInputByteBufferNano.readInt32();
                            codedInputByteBufferNano.readTag();
                            length5++;
                        }
                        iArr5[length5] = codedInputByteBufferNano.readInt32();
                        this.latenciesMs = iArr5;
                        break;
                    case 34:
                        int iPushLimit3 = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                        int position3 = codedInputByteBufferNano.getPosition();
                        int i3 = 0;
                        while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                            codedInputByteBufferNano.readInt32();
                            i3++;
                        }
                        codedInputByteBufferNano.rewindToPosition(position3);
                        if (this.latenciesMs != null) {
                            length6 = this.latenciesMs.length;
                        } else {
                            length6 = 0;
                        }
                        int[] iArr6 = new int[i3 + length6];
                        if (length6 != 0) {
                            System.arraycopy(this.latenciesMs, 0, iArr6, 0, length6);
                        }
                        while (length6 < iArr6.length) {
                            iArr6[length6] = codedInputByteBufferNano.readInt32();
                            length6++;
                        }
                        this.latenciesMs = iArr6;
                        codedInputByteBufferNano.popLimit(iPushLimit3);
                        break;
                    case 40:
                        this.getaddrinfoQueryCount = codedInputByteBufferNano.readInt64();
                        break;
                    case 48:
                        this.gethostbynameQueryCount = codedInputByteBufferNano.readInt64();
                        break;
                    case 56:
                        this.getaddrinfoErrorCount = codedInputByteBufferNano.readInt64();
                        break;
                    case 64:
                        this.gethostbynameErrorCount = codedInputByteBufferNano.readInt64();
                        break;
                    case 74:
                        int repeatedFieldArrayLength4 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 74);
                        if (this.getaddrinfoErrors != null) {
                            length7 = this.getaddrinfoErrors.length;
                        } else {
                            length7 = 0;
                        }
                        Pair[] pairArr = new Pair[repeatedFieldArrayLength4 + length7];
                        if (length7 != 0) {
                            System.arraycopy(this.getaddrinfoErrors, 0, pairArr, 0, length7);
                        }
                        while (length7 < pairArr.length - 1) {
                            pairArr[length7] = new Pair();
                            codedInputByteBufferNano.readMessage(pairArr[length7]);
                            codedInputByteBufferNano.readTag();
                            length7++;
                        }
                        pairArr[length7] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr[length7]);
                        this.getaddrinfoErrors = pairArr;
                        break;
                    case 82:
                        int repeatedFieldArrayLength5 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 82);
                        if (this.gethostbynameErrors != null) {
                            length8 = this.gethostbynameErrors.length;
                        } else {
                            length8 = 0;
                        }
                        Pair[] pairArr2 = new Pair[repeatedFieldArrayLength5 + length8];
                        if (length8 != 0) {
                            System.arraycopy(this.gethostbynameErrors, 0, pairArr2, 0, length8);
                        }
                        while (length8 < pairArr2.length - 1) {
                            pairArr2[length8] = new Pair();
                            codedInputByteBufferNano.readMessage(pairArr2[length8]);
                            codedInputByteBufferNano.readTag();
                            length8++;
                        }
                        pairArr2[length8] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr2[length8]);
                        this.gethostbynameErrors = pairArr2;
                        break;
                    default:
                        if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static DNSLookupBatch parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (DNSLookupBatch) MessageNano.mergeFrom(new DNSLookupBatch(), bArr);
        }

        public static DNSLookupBatch parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new DNSLookupBatch().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class DNSLatencies extends MessageNano {
        private static volatile DNSLatencies[] _emptyArray;
        public int aCount;
        public int aaaaCount;
        public int[] latenciesMs;
        public int queryCount;
        public int returnCode;
        public int type;

        public static DNSLatencies[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new DNSLatencies[0];
                    }
                }
            }
            return _emptyArray;
        }

        public DNSLatencies() {
            clear();
        }

        public DNSLatencies clear() {
            this.type = 0;
            this.returnCode = 0;
            this.queryCount = 0;
            this.aCount = 0;
            this.aaaaCount = 0;
            this.latenciesMs = WireFormatNano.EMPTY_INT_ARRAY;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.type != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.type);
            }
            if (this.returnCode != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.returnCode);
            }
            if (this.queryCount != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.queryCount);
            }
            if (this.aCount != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.aCount);
            }
            if (this.aaaaCount != 0) {
                codedOutputByteBufferNano.writeInt32(5, this.aaaaCount);
            }
            if (this.latenciesMs != null && this.latenciesMs.length > 0) {
                for (int i = 0; i < this.latenciesMs.length; i++) {
                    codedOutputByteBufferNano.writeInt32(6, this.latenciesMs[i]);
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.type != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.type);
            }
            if (this.returnCode != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.returnCode);
            }
            if (this.queryCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.queryCount);
            }
            if (this.aCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.aCount);
            }
            if (this.aaaaCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.aaaaCount);
            }
            if (this.latenciesMs != null && this.latenciesMs.length > 0) {
                int iComputeInt32SizeNoTag = 0;
                for (int i = 0; i < this.latenciesMs.length; i++) {
                    iComputeInt32SizeNoTag += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.latenciesMs[i]);
                }
                return iComputeSerializedSize + iComputeInt32SizeNoTag + (1 * this.latenciesMs.length);
            }
            return iComputeSerializedSize;
        }

        @Override
        public DNSLatencies mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.type = codedInputByteBufferNano.readInt32();
                } else if (tag == 16) {
                    this.returnCode = codedInputByteBufferNano.readInt32();
                } else if (tag == 24) {
                    this.queryCount = codedInputByteBufferNano.readInt32();
                } else if (tag == 32) {
                    this.aCount = codedInputByteBufferNano.readInt32();
                } else if (tag == 40) {
                    this.aaaaCount = codedInputByteBufferNano.readInt32();
                } else if (tag == 48) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 48);
                    if (this.latenciesMs != null) {
                        length2 = this.latenciesMs.length;
                    } else {
                        length2 = 0;
                    }
                    int[] iArr = new int[repeatedFieldArrayLength + length2];
                    if (length2 != 0) {
                        System.arraycopy(this.latenciesMs, 0, iArr, 0, length2);
                    }
                    while (length2 < iArr.length - 1) {
                        iArr[length2] = codedInputByteBufferNano.readInt32();
                        codedInputByteBufferNano.readTag();
                        length2++;
                    }
                    iArr[length2] = codedInputByteBufferNano.readInt32();
                    this.latenciesMs = iArr;
                } else if (tag != 50) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    int iPushLimit = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                    int position = codedInputByteBufferNano.getPosition();
                    int i = 0;
                    while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                        codedInputByteBufferNano.readInt32();
                        i++;
                    }
                    codedInputByteBufferNano.rewindToPosition(position);
                    if (this.latenciesMs != null) {
                        length = this.latenciesMs.length;
                    } else {
                        length = 0;
                    }
                    int[] iArr2 = new int[i + length];
                    if (length != 0) {
                        System.arraycopy(this.latenciesMs, 0, iArr2, 0, length);
                    }
                    while (length < iArr2.length) {
                        iArr2[length] = codedInputByteBufferNano.readInt32();
                        length++;
                    }
                    this.latenciesMs = iArr2;
                    codedInputByteBufferNano.popLimit(iPushLimit);
                }
            }
        }

        public static DNSLatencies parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (DNSLatencies) MessageNano.mergeFrom(new DNSLatencies(), bArr);
        }

        public static DNSLatencies parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new DNSLatencies().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ConnectStatistics extends MessageNano {
        private static volatile ConnectStatistics[] _emptyArray;
        public int connectBlockingCount;
        public int connectCount;
        public Pair[] errnosCounters;
        public int ipv6AddrCount;
        public int[] latenciesMs;
        public int[] nonBlockingLatenciesMs;

        public static ConnectStatistics[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ConnectStatistics[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ConnectStatistics() {
            clear();
        }

        public ConnectStatistics clear() {
            this.connectCount = 0;
            this.connectBlockingCount = 0;
            this.ipv6AddrCount = 0;
            this.latenciesMs = WireFormatNano.EMPTY_INT_ARRAY;
            this.nonBlockingLatenciesMs = WireFormatNano.EMPTY_INT_ARRAY;
            this.errnosCounters = Pair.emptyArray();
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.connectCount != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.connectCount);
            }
            if (this.ipv6AddrCount != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.ipv6AddrCount);
            }
            if (this.latenciesMs != null && this.latenciesMs.length > 0) {
                for (int i = 0; i < this.latenciesMs.length; i++) {
                    codedOutputByteBufferNano.writeInt32(3, this.latenciesMs[i]);
                }
            }
            if (this.errnosCounters != null && this.errnosCounters.length > 0) {
                for (int i2 = 0; i2 < this.errnosCounters.length; i2++) {
                    Pair pair = this.errnosCounters[i2];
                    if (pair != null) {
                        codedOutputByteBufferNano.writeMessage(4, pair);
                    }
                }
            }
            if (this.connectBlockingCount != 0) {
                codedOutputByteBufferNano.writeInt32(5, this.connectBlockingCount);
            }
            if (this.nonBlockingLatenciesMs != null && this.nonBlockingLatenciesMs.length > 0) {
                for (int i3 = 0; i3 < this.nonBlockingLatenciesMs.length; i3++) {
                    codedOutputByteBufferNano.writeInt32(6, this.nonBlockingLatenciesMs[i3]);
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.connectCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.connectCount);
            }
            if (this.ipv6AddrCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.ipv6AddrCount);
            }
            if (this.latenciesMs != null && this.latenciesMs.length > 0) {
                int iComputeInt32SizeNoTag = 0;
                for (int i = 0; i < this.latenciesMs.length; i++) {
                    iComputeInt32SizeNoTag += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.latenciesMs[i]);
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeInt32SizeNoTag + (this.latenciesMs.length * 1);
            }
            if (this.errnosCounters != null && this.errnosCounters.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i2 = 0; i2 < this.errnosCounters.length; i2++) {
                    Pair pair = this.errnosCounters[i2];
                    if (pair != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(4, pair);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.connectBlockingCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.connectBlockingCount);
            }
            if (this.nonBlockingLatenciesMs != null && this.nonBlockingLatenciesMs.length > 0) {
                int iComputeInt32SizeNoTag2 = 0;
                for (int i3 = 0; i3 < this.nonBlockingLatenciesMs.length; i3++) {
                    iComputeInt32SizeNoTag2 += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.nonBlockingLatenciesMs[i3]);
                }
                return iComputeSerializedSize + iComputeInt32SizeNoTag2 + (1 * this.nonBlockingLatenciesMs.length);
            }
            return iComputeSerializedSize;
        }

        @Override
        public ConnectStatistics mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            int length3;
            int length4;
            int length5;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.connectCount = codedInputByteBufferNano.readInt32();
                } else if (tag == 16) {
                    this.ipv6AddrCount = codedInputByteBufferNano.readInt32();
                } else if (tag == 24) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 24);
                    if (this.latenciesMs != null) {
                        length5 = this.latenciesMs.length;
                    } else {
                        length5 = 0;
                    }
                    int[] iArr = new int[repeatedFieldArrayLength + length5];
                    if (length5 != 0) {
                        System.arraycopy(this.latenciesMs, 0, iArr, 0, length5);
                    }
                    while (length5 < iArr.length - 1) {
                        iArr[length5] = codedInputByteBufferNano.readInt32();
                        codedInputByteBufferNano.readTag();
                        length5++;
                    }
                    iArr[length5] = codedInputByteBufferNano.readInt32();
                    this.latenciesMs = iArr;
                } else if (tag == 26) {
                    int iPushLimit = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                    int position = codedInputByteBufferNano.getPosition();
                    int i = 0;
                    while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                        codedInputByteBufferNano.readInt32();
                        i++;
                    }
                    codedInputByteBufferNano.rewindToPosition(position);
                    if (this.latenciesMs != null) {
                        length4 = this.latenciesMs.length;
                    } else {
                        length4 = 0;
                    }
                    int[] iArr2 = new int[i + length4];
                    if (length4 != 0) {
                        System.arraycopy(this.latenciesMs, 0, iArr2, 0, length4);
                    }
                    while (length4 < iArr2.length) {
                        iArr2[length4] = codedInputByteBufferNano.readInt32();
                        length4++;
                    }
                    this.latenciesMs = iArr2;
                    codedInputByteBufferNano.popLimit(iPushLimit);
                } else if (tag == 34) {
                    int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 34);
                    if (this.errnosCounters != null) {
                        length3 = this.errnosCounters.length;
                    } else {
                        length3 = 0;
                    }
                    Pair[] pairArr = new Pair[repeatedFieldArrayLength2 + length3];
                    if (length3 != 0) {
                        System.arraycopy(this.errnosCounters, 0, pairArr, 0, length3);
                    }
                    while (length3 < pairArr.length - 1) {
                        pairArr[length3] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr[length3]);
                        codedInputByteBufferNano.readTag();
                        length3++;
                    }
                    pairArr[length3] = new Pair();
                    codedInputByteBufferNano.readMessage(pairArr[length3]);
                    this.errnosCounters = pairArr;
                } else if (tag == 40) {
                    this.connectBlockingCount = codedInputByteBufferNano.readInt32();
                } else if (tag == 48) {
                    int repeatedFieldArrayLength3 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 48);
                    if (this.nonBlockingLatenciesMs != null) {
                        length2 = this.nonBlockingLatenciesMs.length;
                    } else {
                        length2 = 0;
                    }
                    int[] iArr3 = new int[repeatedFieldArrayLength3 + length2];
                    if (length2 != 0) {
                        System.arraycopy(this.nonBlockingLatenciesMs, 0, iArr3, 0, length2);
                    }
                    while (length2 < iArr3.length - 1) {
                        iArr3[length2] = codedInputByteBufferNano.readInt32();
                        codedInputByteBufferNano.readTag();
                        length2++;
                    }
                    iArr3[length2] = codedInputByteBufferNano.readInt32();
                    this.nonBlockingLatenciesMs = iArr3;
                } else if (tag != 50) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    int iPushLimit2 = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                    int position2 = codedInputByteBufferNano.getPosition();
                    int i2 = 0;
                    while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                        codedInputByteBufferNano.readInt32();
                        i2++;
                    }
                    codedInputByteBufferNano.rewindToPosition(position2);
                    if (this.nonBlockingLatenciesMs != null) {
                        length = this.nonBlockingLatenciesMs.length;
                    } else {
                        length = 0;
                    }
                    int[] iArr4 = new int[i2 + length];
                    if (length != 0) {
                        System.arraycopy(this.nonBlockingLatenciesMs, 0, iArr4, 0, length);
                    }
                    while (length < iArr4.length) {
                        iArr4[length] = codedInputByteBufferNano.readInt32();
                        length++;
                    }
                    this.nonBlockingLatenciesMs = iArr4;
                    codedInputByteBufferNano.popLimit(iPushLimit2);
                }
            }
        }

        public static ConnectStatistics parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ConnectStatistics) MessageNano.mergeFrom(new ConnectStatistics(), bArr);
        }

        public static ConnectStatistics parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ConnectStatistics().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class DHCPEvent extends MessageNano {
        public static final int ERROR_CODE_FIELD_NUMBER = 3;
        public static final int STATE_TRANSITION_FIELD_NUMBER = 2;
        private static volatile DHCPEvent[] _emptyArray;
        public int durationMs;
        public String ifName;
        private int valueCase_ = 0;
        private Object value_;

        public int getValueCase() {
            return this.valueCase_;
        }

        public DHCPEvent clearValue() {
            this.valueCase_ = 0;
            this.value_ = null;
            return this;
        }

        public static DHCPEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new DHCPEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public boolean hasStateTransition() {
            return this.valueCase_ == 2;
        }

        public String getStateTransition() {
            if (this.valueCase_ == 2) {
                return (String) this.value_;
            }
            return "";
        }

        public DHCPEvent setStateTransition(String str) {
            this.valueCase_ = 2;
            this.value_ = str;
            return this;
        }

        public boolean hasErrorCode() {
            return this.valueCase_ == 3;
        }

        public int getErrorCode() {
            if (this.valueCase_ == 3) {
                return ((Integer) this.value_).intValue();
            }
            return 0;
        }

        public DHCPEvent setErrorCode(int i) {
            this.valueCase_ = 3;
            this.value_ = Integer.valueOf(i);
            return this;
        }

        public DHCPEvent() {
            clear();
        }

        public DHCPEvent clear() {
            this.ifName = "";
            this.durationMs = 0;
            clearValue();
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (!this.ifName.equals("")) {
                codedOutputByteBufferNano.writeString(1, this.ifName);
            }
            if (this.valueCase_ == 2) {
                codedOutputByteBufferNano.writeString(2, (String) this.value_);
            }
            if (this.valueCase_ == 3) {
                codedOutputByteBufferNano.writeInt32(3, ((Integer) this.value_).intValue());
            }
            if (this.durationMs != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.durationMs);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (!this.ifName.equals("")) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(1, this.ifName);
            }
            if (this.valueCase_ == 2) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(2, (String) this.value_);
            }
            if (this.valueCase_ == 3) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, ((Integer) this.value_).intValue());
            }
            if (this.durationMs != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(4, this.durationMs);
            }
            return iComputeSerializedSize;
        }

        @Override
        public DHCPEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    this.ifName = codedInputByteBufferNano.readString();
                } else if (tag == 18) {
                    this.value_ = codedInputByteBufferNano.readString();
                    this.valueCase_ = 2;
                } else if (tag == 24) {
                    this.value_ = Integer.valueOf(codedInputByteBufferNano.readInt32());
                    this.valueCase_ = 3;
                } else if (tag != 32) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.durationMs = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static DHCPEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (DHCPEvent) MessageNano.mergeFrom(new DHCPEvent(), bArr);
        }

        public static DHCPEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new DHCPEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ApfProgramEvent extends MessageNano {
        private static volatile ApfProgramEvent[] _emptyArray;
        public int currentRas;
        public boolean dropMulticast;
        public long effectiveLifetime;
        public int filteredRas;
        public boolean hasIpv4Addr;
        public long lifetime;
        public int programLength;

        public static ApfProgramEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ApfProgramEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ApfProgramEvent() {
            clear();
        }

        public ApfProgramEvent clear() {
            this.lifetime = 0L;
            this.effectiveLifetime = 0L;
            this.filteredRas = 0;
            this.currentRas = 0;
            this.programLength = 0;
            this.dropMulticast = false;
            this.hasIpv4Addr = false;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.lifetime != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.lifetime);
            }
            if (this.filteredRas != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.filteredRas);
            }
            if (this.currentRas != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.currentRas);
            }
            if (this.programLength != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.programLength);
            }
            if (this.dropMulticast) {
                codedOutputByteBufferNano.writeBool(5, this.dropMulticast);
            }
            if (this.hasIpv4Addr) {
                codedOutputByteBufferNano.writeBool(6, this.hasIpv4Addr);
            }
            if (this.effectiveLifetime != 0) {
                codedOutputByteBufferNano.writeInt64(7, this.effectiveLifetime);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.lifetime != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.lifetime);
            }
            if (this.filteredRas != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.filteredRas);
            }
            if (this.currentRas != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.currentRas);
            }
            if (this.programLength != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.programLength);
            }
            if (this.dropMulticast) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(5, this.dropMulticast);
            }
            if (this.hasIpv4Addr) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(6, this.hasIpv4Addr);
            }
            if (this.effectiveLifetime != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(7, this.effectiveLifetime);
            }
            return iComputeSerializedSize;
        }

        @Override
        public ApfProgramEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.lifetime = codedInputByteBufferNano.readInt64();
                } else if (tag == 16) {
                    this.filteredRas = codedInputByteBufferNano.readInt32();
                } else if (tag == 24) {
                    this.currentRas = codedInputByteBufferNano.readInt32();
                } else if (tag == 32) {
                    this.programLength = codedInputByteBufferNano.readInt32();
                } else if (tag == 40) {
                    this.dropMulticast = codedInputByteBufferNano.readBool();
                } else if (tag == 48) {
                    this.hasIpv4Addr = codedInputByteBufferNano.readBool();
                } else if (tag != 56) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.effectiveLifetime = codedInputByteBufferNano.readInt64();
                }
            }
        }

        public static ApfProgramEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ApfProgramEvent) MessageNano.mergeFrom(new ApfProgramEvent(), bArr);
        }

        public static ApfProgramEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ApfProgramEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ApfStatistics extends MessageNano {
        private static volatile ApfStatistics[] _emptyArray;
        public int droppedRas;
        public long durationMs;
        public Pair[] hardwareCounters;
        public int matchingRas;
        public int maxProgramSize;
        public int parseErrors;
        public int programUpdates;
        public int programUpdatesAll;
        public int programUpdatesAllowingMulticast;
        public int receivedRas;
        public int totalPacketDropped;
        public int totalPacketProcessed;
        public int zeroLifetimeRas;

        public static ApfStatistics[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ApfStatistics[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ApfStatistics() {
            clear();
        }

        public ApfStatistics clear() {
            this.durationMs = 0L;
            this.receivedRas = 0;
            this.matchingRas = 0;
            this.droppedRas = 0;
            this.zeroLifetimeRas = 0;
            this.parseErrors = 0;
            this.programUpdates = 0;
            this.maxProgramSize = 0;
            this.programUpdatesAll = 0;
            this.programUpdatesAllowingMulticast = 0;
            this.totalPacketProcessed = 0;
            this.totalPacketDropped = 0;
            this.hardwareCounters = Pair.emptyArray();
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.durationMs != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.durationMs);
            }
            if (this.receivedRas != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.receivedRas);
            }
            if (this.matchingRas != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.matchingRas);
            }
            if (this.droppedRas != 0) {
                codedOutputByteBufferNano.writeInt32(5, this.droppedRas);
            }
            if (this.zeroLifetimeRas != 0) {
                codedOutputByteBufferNano.writeInt32(6, this.zeroLifetimeRas);
            }
            if (this.parseErrors != 0) {
                codedOutputByteBufferNano.writeInt32(7, this.parseErrors);
            }
            if (this.programUpdates != 0) {
                codedOutputByteBufferNano.writeInt32(8, this.programUpdates);
            }
            if (this.maxProgramSize != 0) {
                codedOutputByteBufferNano.writeInt32(9, this.maxProgramSize);
            }
            if (this.programUpdatesAll != 0) {
                codedOutputByteBufferNano.writeInt32(10, this.programUpdatesAll);
            }
            if (this.programUpdatesAllowingMulticast != 0) {
                codedOutputByteBufferNano.writeInt32(11, this.programUpdatesAllowingMulticast);
            }
            if (this.totalPacketProcessed != 0) {
                codedOutputByteBufferNano.writeInt32(12, this.totalPacketProcessed);
            }
            if (this.totalPacketDropped != 0) {
                codedOutputByteBufferNano.writeInt32(13, this.totalPacketDropped);
            }
            if (this.hardwareCounters != null && this.hardwareCounters.length > 0) {
                for (int i = 0; i < this.hardwareCounters.length; i++) {
                    Pair pair = this.hardwareCounters[i];
                    if (pair != null) {
                        codedOutputByteBufferNano.writeMessage(14, pair);
                    }
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.durationMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.durationMs);
            }
            if (this.receivedRas != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.receivedRas);
            }
            if (this.matchingRas != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.matchingRas);
            }
            if (this.droppedRas != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.droppedRas);
            }
            if (this.zeroLifetimeRas != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(6, this.zeroLifetimeRas);
            }
            if (this.parseErrors != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(7, this.parseErrors);
            }
            if (this.programUpdates != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(8, this.programUpdates);
            }
            if (this.maxProgramSize != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(9, this.maxProgramSize);
            }
            if (this.programUpdatesAll != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(10, this.programUpdatesAll);
            }
            if (this.programUpdatesAllowingMulticast != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(11, this.programUpdatesAllowingMulticast);
            }
            if (this.totalPacketProcessed != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(12, this.totalPacketProcessed);
            }
            if (this.totalPacketDropped != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(13, this.totalPacketDropped);
            }
            if (this.hardwareCounters != null && this.hardwareCounters.length > 0) {
                for (int i = 0; i < this.hardwareCounters.length; i++) {
                    Pair pair = this.hardwareCounters[i];
                    if (pair != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(14, pair);
                    }
                }
            }
            return iComputeSerializedSize;
        }

        @Override
        public ApfStatistics mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.durationMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 16:
                        this.receivedRas = codedInputByteBufferNano.readInt32();
                        break;
                    case 24:
                        this.matchingRas = codedInputByteBufferNano.readInt32();
                        break;
                    case 40:
                        this.droppedRas = codedInputByteBufferNano.readInt32();
                        break;
                    case 48:
                        this.zeroLifetimeRas = codedInputByteBufferNano.readInt32();
                        break;
                    case 56:
                        this.parseErrors = codedInputByteBufferNano.readInt32();
                        break;
                    case 64:
                        this.programUpdates = codedInputByteBufferNano.readInt32();
                        break;
                    case 72:
                        this.maxProgramSize = codedInputByteBufferNano.readInt32();
                        break;
                    case 80:
                        this.programUpdatesAll = codedInputByteBufferNano.readInt32();
                        break;
                    case 88:
                        this.programUpdatesAllowingMulticast = codedInputByteBufferNano.readInt32();
                        break;
                    case 96:
                        this.totalPacketProcessed = codedInputByteBufferNano.readInt32();
                        break;
                    case 104:
                        this.totalPacketDropped = codedInputByteBufferNano.readInt32();
                        break;
                    case 114:
                        int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 114);
                        if (this.hardwareCounters != null) {
                            length = this.hardwareCounters.length;
                        } else {
                            length = 0;
                        }
                        Pair[] pairArr = new Pair[repeatedFieldArrayLength + length];
                        if (length != 0) {
                            System.arraycopy(this.hardwareCounters, 0, pairArr, 0, length);
                        }
                        while (length < pairArr.length - 1) {
                            pairArr[length] = new Pair();
                            codedInputByteBufferNano.readMessage(pairArr[length]);
                            codedInputByteBufferNano.readTag();
                            length++;
                        }
                        pairArr[length] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr[length]);
                        this.hardwareCounters = pairArr;
                        break;
                    default:
                        if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static ApfStatistics parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ApfStatistics) MessageNano.mergeFrom(new ApfStatistics(), bArr);
        }

        public static ApfStatistics parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ApfStatistics().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class RaEvent extends MessageNano {
        private static volatile RaEvent[] _emptyArray;
        public long dnsslLifetime;
        public long prefixPreferredLifetime;
        public long prefixValidLifetime;
        public long rdnssLifetime;
        public long routeInfoLifetime;
        public long routerLifetime;

        public static RaEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new RaEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public RaEvent() {
            clear();
        }

        public RaEvent clear() {
            this.routerLifetime = 0L;
            this.prefixValidLifetime = 0L;
            this.prefixPreferredLifetime = 0L;
            this.routeInfoLifetime = 0L;
            this.rdnssLifetime = 0L;
            this.dnsslLifetime = 0L;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.routerLifetime != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.routerLifetime);
            }
            if (this.prefixValidLifetime != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.prefixValidLifetime);
            }
            if (this.prefixPreferredLifetime != 0) {
                codedOutputByteBufferNano.writeInt64(3, this.prefixPreferredLifetime);
            }
            if (this.routeInfoLifetime != 0) {
                codedOutputByteBufferNano.writeInt64(4, this.routeInfoLifetime);
            }
            if (this.rdnssLifetime != 0) {
                codedOutputByteBufferNano.writeInt64(5, this.rdnssLifetime);
            }
            if (this.dnsslLifetime != 0) {
                codedOutputByteBufferNano.writeInt64(6, this.dnsslLifetime);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.routerLifetime != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.routerLifetime);
            }
            if (this.prefixValidLifetime != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(2, this.prefixValidLifetime);
            }
            if (this.prefixPreferredLifetime != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(3, this.prefixPreferredLifetime);
            }
            if (this.routeInfoLifetime != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(4, this.routeInfoLifetime);
            }
            if (this.rdnssLifetime != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(5, this.rdnssLifetime);
            }
            if (this.dnsslLifetime != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(6, this.dnsslLifetime);
            }
            return iComputeSerializedSize;
        }

        @Override
        public RaEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.routerLifetime = codedInputByteBufferNano.readInt64();
                } else if (tag == 16) {
                    this.prefixValidLifetime = codedInputByteBufferNano.readInt64();
                } else if (tag == 24) {
                    this.prefixPreferredLifetime = codedInputByteBufferNano.readInt64();
                } else if (tag == 32) {
                    this.routeInfoLifetime = codedInputByteBufferNano.readInt64();
                } else if (tag == 40) {
                    this.rdnssLifetime = codedInputByteBufferNano.readInt64();
                } else if (tag != 48) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.dnsslLifetime = codedInputByteBufferNano.readInt64();
                }
            }
        }

        public static RaEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (RaEvent) MessageNano.mergeFrom(new RaEvent(), bArr);
        }

        public static RaEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new RaEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class IpProvisioningEvent extends MessageNano {
        private static volatile IpProvisioningEvent[] _emptyArray;
        public int eventType;
        public String ifName;
        public int latencyMs;

        public static IpProvisioningEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new IpProvisioningEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public IpProvisioningEvent() {
            clear();
        }

        public IpProvisioningEvent clear() {
            this.ifName = "";
            this.eventType = 0;
            this.latencyMs = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (!this.ifName.equals("")) {
                codedOutputByteBufferNano.writeString(1, this.ifName);
            }
            if (this.eventType != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.eventType);
            }
            if (this.latencyMs != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.latencyMs);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (!this.ifName.equals("")) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(1, this.ifName);
            }
            if (this.eventType != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.eventType);
            }
            if (this.latencyMs != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(3, this.latencyMs);
            }
            return iComputeSerializedSize;
        }

        @Override
        public IpProvisioningEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    this.ifName = codedInputByteBufferNano.readString();
                } else if (tag == 16) {
                    this.eventType = codedInputByteBufferNano.readInt32();
                } else if (tag != 24) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.latencyMs = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static IpProvisioningEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (IpProvisioningEvent) MessageNano.mergeFrom(new IpProvisioningEvent(), bArr);
        }

        public static IpProvisioningEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new IpProvisioningEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class NetworkStats extends MessageNano {
        private static volatile NetworkStats[] _emptyArray;
        public long durationMs;
        public boolean everValidated;
        public int ipSupport;
        public int noConnectivityReports;
        public boolean portalFound;
        public int validationAttempts;
        public Pair[] validationEvents;
        public Pair[] validationStates;

        public static NetworkStats[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new NetworkStats[0];
                    }
                }
            }
            return _emptyArray;
        }

        public NetworkStats() {
            clear();
        }

        public NetworkStats clear() {
            this.durationMs = 0L;
            this.ipSupport = 0;
            this.everValidated = false;
            this.portalFound = false;
            this.noConnectivityReports = 0;
            this.validationAttempts = 0;
            this.validationEvents = Pair.emptyArray();
            this.validationStates = Pair.emptyArray();
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.durationMs != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.durationMs);
            }
            if (this.ipSupport != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.ipSupport);
            }
            if (this.everValidated) {
                codedOutputByteBufferNano.writeBool(3, this.everValidated);
            }
            if (this.portalFound) {
                codedOutputByteBufferNano.writeBool(4, this.portalFound);
            }
            if (this.noConnectivityReports != 0) {
                codedOutputByteBufferNano.writeInt32(5, this.noConnectivityReports);
            }
            if (this.validationAttempts != 0) {
                codedOutputByteBufferNano.writeInt32(6, this.validationAttempts);
            }
            if (this.validationEvents != null && this.validationEvents.length > 0) {
                for (int i = 0; i < this.validationEvents.length; i++) {
                    Pair pair = this.validationEvents[i];
                    if (pair != null) {
                        codedOutputByteBufferNano.writeMessage(7, pair);
                    }
                }
            }
            if (this.validationStates != null && this.validationStates.length > 0) {
                for (int i2 = 0; i2 < this.validationStates.length; i2++) {
                    Pair pair2 = this.validationStates[i2];
                    if (pair2 != null) {
                        codedOutputByteBufferNano.writeMessage(8, pair2);
                    }
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.durationMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.durationMs);
            }
            if (this.ipSupport != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.ipSupport);
            }
            if (this.everValidated) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(3, this.everValidated);
            }
            if (this.portalFound) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(4, this.portalFound);
            }
            if (this.noConnectivityReports != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.noConnectivityReports);
            }
            if (this.validationAttempts != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(6, this.validationAttempts);
            }
            if (this.validationEvents != null && this.validationEvents.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i = 0; i < this.validationEvents.length; i++) {
                    Pair pair = this.validationEvents[i];
                    if (pair != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(7, pair);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.validationStates != null && this.validationStates.length > 0) {
                for (int i2 = 0; i2 < this.validationStates.length; i2++) {
                    Pair pair2 = this.validationStates[i2];
                    if (pair2 != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(8, pair2);
                    }
                }
            }
            return iComputeSerializedSize;
        }

        @Override
        public NetworkStats mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.durationMs = codedInputByteBufferNano.readInt64();
                } else if (tag == 16) {
                    int int32 = codedInputByteBufferNano.readInt32();
                    switch (int32) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            this.ipSupport = int32;
                            break;
                    }
                } else if (tag == 24) {
                    this.everValidated = codedInputByteBufferNano.readBool();
                } else if (tag == 32) {
                    this.portalFound = codedInputByteBufferNano.readBool();
                } else if (tag == 40) {
                    this.noConnectivityReports = codedInputByteBufferNano.readInt32();
                } else if (tag == 48) {
                    this.validationAttempts = codedInputByteBufferNano.readInt32();
                } else if (tag == 58) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 58);
                    if (this.validationEvents != null) {
                        length2 = this.validationEvents.length;
                    } else {
                        length2 = 0;
                    }
                    Pair[] pairArr = new Pair[repeatedFieldArrayLength + length2];
                    if (length2 != 0) {
                        System.arraycopy(this.validationEvents, 0, pairArr, 0, length2);
                    }
                    while (length2 < pairArr.length - 1) {
                        pairArr[length2] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr[length2]);
                        codedInputByteBufferNano.readTag();
                        length2++;
                    }
                    pairArr[length2] = new Pair();
                    codedInputByteBufferNano.readMessage(pairArr[length2]);
                    this.validationEvents = pairArr;
                } else if (tag != 66) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 66);
                    if (this.validationStates != null) {
                        length = this.validationStates.length;
                    } else {
                        length = 0;
                    }
                    Pair[] pairArr2 = new Pair[repeatedFieldArrayLength2 + length];
                    if (length != 0) {
                        System.arraycopy(this.validationStates, 0, pairArr2, 0, length);
                    }
                    while (length < pairArr2.length - 1) {
                        pairArr2[length] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr2[length]);
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    pairArr2[length] = new Pair();
                    codedInputByteBufferNano.readMessage(pairArr2[length]);
                    this.validationStates = pairArr2;
                }
            }
        }

        public static NetworkStats parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (NetworkStats) MessageNano.mergeFrom(new NetworkStats(), bArr);
        }

        public static NetworkStats parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new NetworkStats().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class WakeupStats extends MessageNano {
        private static volatile WakeupStats[] _emptyArray;
        public long applicationWakeups;
        public long durationSec;
        public Pair[] ethertypeCounts;
        public Pair[] ipNextHeaderCounts;
        public long l2BroadcastCount;
        public long l2MulticastCount;
        public long l2UnicastCount;
        public long noUidWakeups;
        public long nonApplicationWakeups;
        public long rootWakeups;
        public long systemWakeups;
        public long totalWakeups;

        public static WakeupStats[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new WakeupStats[0];
                    }
                }
            }
            return _emptyArray;
        }

        public WakeupStats() {
            clear();
        }

        public WakeupStats clear() {
            this.durationSec = 0L;
            this.totalWakeups = 0L;
            this.rootWakeups = 0L;
            this.systemWakeups = 0L;
            this.applicationWakeups = 0L;
            this.nonApplicationWakeups = 0L;
            this.noUidWakeups = 0L;
            this.ethertypeCounts = Pair.emptyArray();
            this.ipNextHeaderCounts = Pair.emptyArray();
            this.l2UnicastCount = 0L;
            this.l2MulticastCount = 0L;
            this.l2BroadcastCount = 0L;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.durationSec != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.durationSec);
            }
            if (this.totalWakeups != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.totalWakeups);
            }
            if (this.rootWakeups != 0) {
                codedOutputByteBufferNano.writeInt64(3, this.rootWakeups);
            }
            if (this.systemWakeups != 0) {
                codedOutputByteBufferNano.writeInt64(4, this.systemWakeups);
            }
            if (this.applicationWakeups != 0) {
                codedOutputByteBufferNano.writeInt64(5, this.applicationWakeups);
            }
            if (this.nonApplicationWakeups != 0) {
                codedOutputByteBufferNano.writeInt64(6, this.nonApplicationWakeups);
            }
            if (this.noUidWakeups != 0) {
                codedOutputByteBufferNano.writeInt64(7, this.noUidWakeups);
            }
            if (this.ethertypeCounts != null && this.ethertypeCounts.length > 0) {
                for (int i = 0; i < this.ethertypeCounts.length; i++) {
                    Pair pair = this.ethertypeCounts[i];
                    if (pair != null) {
                        codedOutputByteBufferNano.writeMessage(8, pair);
                    }
                }
            }
            if (this.ipNextHeaderCounts != null && this.ipNextHeaderCounts.length > 0) {
                for (int i2 = 0; i2 < this.ipNextHeaderCounts.length; i2++) {
                    Pair pair2 = this.ipNextHeaderCounts[i2];
                    if (pair2 != null) {
                        codedOutputByteBufferNano.writeMessage(9, pair2);
                    }
                }
            }
            if (this.l2UnicastCount != 0) {
                codedOutputByteBufferNano.writeInt64(10, this.l2UnicastCount);
            }
            if (this.l2MulticastCount != 0) {
                codedOutputByteBufferNano.writeInt64(11, this.l2MulticastCount);
            }
            if (this.l2BroadcastCount != 0) {
                codedOutputByteBufferNano.writeInt64(12, this.l2BroadcastCount);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.durationSec != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.durationSec);
            }
            if (this.totalWakeups != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(2, this.totalWakeups);
            }
            if (this.rootWakeups != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(3, this.rootWakeups);
            }
            if (this.systemWakeups != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(4, this.systemWakeups);
            }
            if (this.applicationWakeups != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(5, this.applicationWakeups);
            }
            if (this.nonApplicationWakeups != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(6, this.nonApplicationWakeups);
            }
            if (this.noUidWakeups != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(7, this.noUidWakeups);
            }
            if (this.ethertypeCounts != null && this.ethertypeCounts.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i = 0; i < this.ethertypeCounts.length; i++) {
                    Pair pair = this.ethertypeCounts[i];
                    if (pair != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(8, pair);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.ipNextHeaderCounts != null && this.ipNextHeaderCounts.length > 0) {
                for (int i2 = 0; i2 < this.ipNextHeaderCounts.length; i2++) {
                    Pair pair2 = this.ipNextHeaderCounts[i2];
                    if (pair2 != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(9, pair2);
                    }
                }
            }
            if (this.l2UnicastCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(10, this.l2UnicastCount);
            }
            if (this.l2MulticastCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(11, this.l2MulticastCount);
            }
            if (this.l2BroadcastCount != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(12, this.l2BroadcastCount);
            }
            return iComputeSerializedSize;
        }

        @Override
        public WakeupStats mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.durationSec = codedInputByteBufferNano.readInt64();
                        break;
                    case 16:
                        this.totalWakeups = codedInputByteBufferNano.readInt64();
                        break;
                    case 24:
                        this.rootWakeups = codedInputByteBufferNano.readInt64();
                        break;
                    case 32:
                        this.systemWakeups = codedInputByteBufferNano.readInt64();
                        break;
                    case 40:
                        this.applicationWakeups = codedInputByteBufferNano.readInt64();
                        break;
                    case 48:
                        this.nonApplicationWakeups = codedInputByteBufferNano.readInt64();
                        break;
                    case 56:
                        this.noUidWakeups = codedInputByteBufferNano.readInt64();
                        break;
                    case 66:
                        int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 66);
                        if (this.ethertypeCounts != null) {
                            length = this.ethertypeCounts.length;
                        } else {
                            length = 0;
                        }
                        Pair[] pairArr = new Pair[repeatedFieldArrayLength + length];
                        if (length != 0) {
                            System.arraycopy(this.ethertypeCounts, 0, pairArr, 0, length);
                        }
                        while (length < pairArr.length - 1) {
                            pairArr[length] = new Pair();
                            codedInputByteBufferNano.readMessage(pairArr[length]);
                            codedInputByteBufferNano.readTag();
                            length++;
                        }
                        pairArr[length] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr[length]);
                        this.ethertypeCounts = pairArr;
                        break;
                    case 74:
                        int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 74);
                        if (this.ipNextHeaderCounts != null) {
                            length2 = this.ipNextHeaderCounts.length;
                        } else {
                            length2 = 0;
                        }
                        Pair[] pairArr2 = new Pair[repeatedFieldArrayLength2 + length2];
                        if (length2 != 0) {
                            System.arraycopy(this.ipNextHeaderCounts, 0, pairArr2, 0, length2);
                        }
                        while (length2 < pairArr2.length - 1) {
                            pairArr2[length2] = new Pair();
                            codedInputByteBufferNano.readMessage(pairArr2[length2]);
                            codedInputByteBufferNano.readTag();
                            length2++;
                        }
                        pairArr2[length2] = new Pair();
                        codedInputByteBufferNano.readMessage(pairArr2[length2]);
                        this.ipNextHeaderCounts = pairArr2;
                        break;
                    case 80:
                        this.l2UnicastCount = codedInputByteBufferNano.readInt64();
                        break;
                    case 88:
                        this.l2MulticastCount = codedInputByteBufferNano.readInt64();
                        break;
                    case 96:
                        this.l2BroadcastCount = codedInputByteBufferNano.readInt64();
                        break;
                    default:
                        if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static WakeupStats parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (WakeupStats) MessageNano.mergeFrom(new WakeupStats(), bArr);
        }

        public static WakeupStats parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new WakeupStats().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class IpConnectivityEvent extends MessageNano {
        public static final int APF_PROGRAM_EVENT_FIELD_NUMBER = 9;
        public static final int APF_STATISTICS_FIELD_NUMBER = 10;
        public static final int CONNECT_STATISTICS_FIELD_NUMBER = 14;
        public static final int DEFAULT_NETWORK_EVENT_FIELD_NUMBER = 2;
        public static final int DHCP_EVENT_FIELD_NUMBER = 6;
        public static final int DNS_LATENCIES_FIELD_NUMBER = 13;
        public static final int DNS_LOOKUP_BATCH_FIELD_NUMBER = 5;
        public static final int IP_PROVISIONING_EVENT_FIELD_NUMBER = 7;
        public static final int IP_REACHABILITY_EVENT_FIELD_NUMBER = 3;
        public static final int NETWORK_EVENT_FIELD_NUMBER = 4;
        public static final int NETWORK_STATS_FIELD_NUMBER = 19;
        public static final int RA_EVENT_FIELD_NUMBER = 11;
        public static final int VALIDATION_PROBE_EVENT_FIELD_NUMBER = 8;
        public static final int WAKEUP_STATS_FIELD_NUMBER = 20;
        private static volatile IpConnectivityEvent[] _emptyArray;
        private int eventCase_ = 0;
        private Object event_;
        public String ifName;
        public int linkLayer;
        public int networkId;
        public long timeMs;
        public long transports;

        public int getEventCase() {
            return this.eventCase_;
        }

        public IpConnectivityEvent clearEvent() {
            this.eventCase_ = 0;
            this.event_ = null;
            return this;
        }

        public static IpConnectivityEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new IpConnectivityEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public boolean hasDefaultNetworkEvent() {
            return this.eventCase_ == 2;
        }

        public DefaultNetworkEvent getDefaultNetworkEvent() {
            if (this.eventCase_ == 2) {
                return (DefaultNetworkEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setDefaultNetworkEvent(DefaultNetworkEvent defaultNetworkEvent) {
            if (defaultNetworkEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 2;
            this.event_ = defaultNetworkEvent;
            return this;
        }

        public boolean hasIpReachabilityEvent() {
            return this.eventCase_ == 3;
        }

        public IpReachabilityEvent getIpReachabilityEvent() {
            if (this.eventCase_ == 3) {
                return (IpReachabilityEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setIpReachabilityEvent(IpReachabilityEvent ipReachabilityEvent) {
            if (ipReachabilityEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 3;
            this.event_ = ipReachabilityEvent;
            return this;
        }

        public boolean hasNetworkEvent() {
            return this.eventCase_ == 4;
        }

        public NetworkEvent getNetworkEvent() {
            if (this.eventCase_ == 4) {
                return (NetworkEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setNetworkEvent(NetworkEvent networkEvent) {
            if (networkEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 4;
            this.event_ = networkEvent;
            return this;
        }

        public boolean hasDnsLookupBatch() {
            return this.eventCase_ == 5;
        }

        public DNSLookupBatch getDnsLookupBatch() {
            if (this.eventCase_ == 5) {
                return (DNSLookupBatch) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setDnsLookupBatch(DNSLookupBatch dNSLookupBatch) {
            if (dNSLookupBatch == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 5;
            this.event_ = dNSLookupBatch;
            return this;
        }

        public boolean hasDnsLatencies() {
            return this.eventCase_ == 13;
        }

        public DNSLatencies getDnsLatencies() {
            if (this.eventCase_ == 13) {
                return (DNSLatencies) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setDnsLatencies(DNSLatencies dNSLatencies) {
            if (dNSLatencies == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 13;
            this.event_ = dNSLatencies;
            return this;
        }

        public boolean hasConnectStatistics() {
            return this.eventCase_ == 14;
        }

        public ConnectStatistics getConnectStatistics() {
            if (this.eventCase_ == 14) {
                return (ConnectStatistics) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setConnectStatistics(ConnectStatistics connectStatistics) {
            if (connectStatistics == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 14;
            this.event_ = connectStatistics;
            return this;
        }

        public boolean hasDhcpEvent() {
            return this.eventCase_ == 6;
        }

        public DHCPEvent getDhcpEvent() {
            if (this.eventCase_ == 6) {
                return (DHCPEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setDhcpEvent(DHCPEvent dHCPEvent) {
            if (dHCPEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 6;
            this.event_ = dHCPEvent;
            return this;
        }

        public boolean hasIpProvisioningEvent() {
            return this.eventCase_ == 7;
        }

        public IpProvisioningEvent getIpProvisioningEvent() {
            if (this.eventCase_ == 7) {
                return (IpProvisioningEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setIpProvisioningEvent(IpProvisioningEvent ipProvisioningEvent) {
            if (ipProvisioningEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 7;
            this.event_ = ipProvisioningEvent;
            return this;
        }

        public boolean hasValidationProbeEvent() {
            return this.eventCase_ == 8;
        }

        public ValidationProbeEvent getValidationProbeEvent() {
            if (this.eventCase_ == 8) {
                return (ValidationProbeEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setValidationProbeEvent(ValidationProbeEvent validationProbeEvent) {
            if (validationProbeEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 8;
            this.event_ = validationProbeEvent;
            return this;
        }

        public boolean hasApfProgramEvent() {
            return this.eventCase_ == 9;
        }

        public ApfProgramEvent getApfProgramEvent() {
            if (this.eventCase_ == 9) {
                return (ApfProgramEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setApfProgramEvent(ApfProgramEvent apfProgramEvent) {
            if (apfProgramEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 9;
            this.event_ = apfProgramEvent;
            return this;
        }

        public boolean hasApfStatistics() {
            return this.eventCase_ == 10;
        }

        public ApfStatistics getApfStatistics() {
            if (this.eventCase_ == 10) {
                return (ApfStatistics) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setApfStatistics(ApfStatistics apfStatistics) {
            if (apfStatistics == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 10;
            this.event_ = apfStatistics;
            return this;
        }

        public boolean hasRaEvent() {
            return this.eventCase_ == 11;
        }

        public RaEvent getRaEvent() {
            if (this.eventCase_ == 11) {
                return (RaEvent) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setRaEvent(RaEvent raEvent) {
            if (raEvent == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 11;
            this.event_ = raEvent;
            return this;
        }

        public boolean hasNetworkStats() {
            return this.eventCase_ == 19;
        }

        public NetworkStats getNetworkStats() {
            if (this.eventCase_ == 19) {
                return (NetworkStats) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setNetworkStats(NetworkStats networkStats) {
            if (networkStats == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 19;
            this.event_ = networkStats;
            return this;
        }

        public boolean hasWakeupStats() {
            return this.eventCase_ == 20;
        }

        public WakeupStats getWakeupStats() {
            if (this.eventCase_ == 20) {
                return (WakeupStats) this.event_;
            }
            return null;
        }

        public IpConnectivityEvent setWakeupStats(WakeupStats wakeupStats) {
            if (wakeupStats == null) {
                throw new NullPointerException();
            }
            this.eventCase_ = 20;
            this.event_ = wakeupStats;
            return this;
        }

        public IpConnectivityEvent() {
            clear();
        }

        public IpConnectivityEvent clear() {
            this.timeMs = 0L;
            this.linkLayer = 0;
            this.networkId = 0;
            this.ifName = "";
            this.transports = 0L;
            clearEvent();
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.timeMs != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.timeMs);
            }
            if (this.eventCase_ == 2) {
                codedOutputByteBufferNano.writeMessage(2, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 3) {
                codedOutputByteBufferNano.writeMessage(3, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 4) {
                codedOutputByteBufferNano.writeMessage(4, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 5) {
                codedOutputByteBufferNano.writeMessage(5, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 6) {
                codedOutputByteBufferNano.writeMessage(6, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 7) {
                codedOutputByteBufferNano.writeMessage(7, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 8) {
                codedOutputByteBufferNano.writeMessage(8, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 9) {
                codedOutputByteBufferNano.writeMessage(9, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 10) {
                codedOutputByteBufferNano.writeMessage(10, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 11) {
                codedOutputByteBufferNano.writeMessage(11, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 13) {
                codedOutputByteBufferNano.writeMessage(13, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 14) {
                codedOutputByteBufferNano.writeMessage(14, (MessageNano) this.event_);
            }
            if (this.linkLayer != 0) {
                codedOutputByteBufferNano.writeInt32(15, this.linkLayer);
            }
            if (this.networkId != 0) {
                codedOutputByteBufferNano.writeInt32(16, this.networkId);
            }
            if (!this.ifName.equals("")) {
                codedOutputByteBufferNano.writeString(17, this.ifName);
            }
            if (this.transports != 0) {
                codedOutputByteBufferNano.writeInt64(18, this.transports);
            }
            if (this.eventCase_ == 19) {
                codedOutputByteBufferNano.writeMessage(19, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 20) {
                codedOutputByteBufferNano.writeMessage(20, (MessageNano) this.event_);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.timeMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.timeMs);
            }
            if (this.eventCase_ == 2) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(2, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 3) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(3, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 4) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(4, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 5) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(5, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 6) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(6, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 7) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(7, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 8) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(8, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 9) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(9, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 10) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(10, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 11) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(11, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 13) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(13, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 14) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(14, (MessageNano) this.event_);
            }
            if (this.linkLayer != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(15, this.linkLayer);
            }
            if (this.networkId != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(16, this.networkId);
            }
            if (!this.ifName.equals("")) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(17, this.ifName);
            }
            if (this.transports != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(18, this.transports);
            }
            if (this.eventCase_ == 19) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(19, (MessageNano) this.event_);
            }
            if (this.eventCase_ == 20) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(20, (MessageNano) this.event_);
            }
            return iComputeSerializedSize;
        }

        @Override
        public IpConnectivityEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.timeMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 18:
                        if (this.eventCase_ != 2) {
                            this.event_ = new DefaultNetworkEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 2;
                        break;
                    case 26:
                        if (this.eventCase_ != 3) {
                            this.event_ = new IpReachabilityEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 3;
                        break;
                    case 34:
                        if (this.eventCase_ != 4) {
                            this.event_ = new NetworkEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 4;
                        break;
                    case 42:
                        if (this.eventCase_ != 5) {
                            this.event_ = new DNSLookupBatch();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 5;
                        break;
                    case 50:
                        if (this.eventCase_ != 6) {
                            this.event_ = new DHCPEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 6;
                        break;
                    case 58:
                        if (this.eventCase_ != 7) {
                            this.event_ = new IpProvisioningEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 7;
                        break;
                    case 66:
                        if (this.eventCase_ != 8) {
                            this.event_ = new ValidationProbeEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 8;
                        break;
                    case 74:
                        if (this.eventCase_ != 9) {
                            this.event_ = new ApfProgramEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 9;
                        break;
                    case 82:
                        if (this.eventCase_ != 10) {
                            this.event_ = new ApfStatistics();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 10;
                        break;
                    case 90:
                        if (this.eventCase_ != 11) {
                            this.event_ = new RaEvent();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 11;
                        break;
                    case 106:
                        if (this.eventCase_ != 13) {
                            this.event_ = new DNSLatencies();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 13;
                        break;
                    case 114:
                        if (this.eventCase_ != 14) {
                            this.event_ = new ConnectStatistics();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 14;
                        break;
                    case 120:
                        int int32 = codedInputByteBufferNano.readInt32();
                        switch (int32) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                                this.linkLayer = int32;
                                break;
                        }
                        break;
                    case 128:
                        this.networkId = codedInputByteBufferNano.readInt32();
                        break;
                    case 138:
                        this.ifName = codedInputByteBufferNano.readString();
                        break;
                    case 144:
                        this.transports = codedInputByteBufferNano.readInt64();
                        break;
                    case 154:
                        if (this.eventCase_ != 19) {
                            this.event_ = new NetworkStats();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 19;
                        break;
                    case 162:
                        if (this.eventCase_ != 20) {
                            this.event_ = new WakeupStats();
                        }
                        codedInputByteBufferNano.readMessage((MessageNano) this.event_);
                        this.eventCase_ = 20;
                        break;
                    default:
                        if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static IpConnectivityEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (IpConnectivityEvent) MessageNano.mergeFrom(new IpConnectivityEvent(), bArr);
        }

        public static IpConnectivityEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new IpConnectivityEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class IpConnectivityLog extends MessageNano {
        private static volatile IpConnectivityLog[] _emptyArray;
        public int droppedEvents;
        public IpConnectivityEvent[] events;
        public int version;

        public static IpConnectivityLog[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new IpConnectivityLog[0];
                    }
                }
            }
            return _emptyArray;
        }

        public IpConnectivityLog() {
            clear();
        }

        public IpConnectivityLog clear() {
            this.events = IpConnectivityEvent.emptyArray();
            this.droppedEvents = 0;
            this.version = 0;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.events != null && this.events.length > 0) {
                for (int i = 0; i < this.events.length; i++) {
                    IpConnectivityEvent ipConnectivityEvent = this.events[i];
                    if (ipConnectivityEvent != null) {
                        codedOutputByteBufferNano.writeMessage(1, ipConnectivityEvent);
                    }
                }
            }
            if (this.droppedEvents != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.droppedEvents);
            }
            if (this.version != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.version);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.events != null && this.events.length > 0) {
                for (int i = 0; i < this.events.length; i++) {
                    IpConnectivityEvent ipConnectivityEvent = this.events[i];
                    if (ipConnectivityEvent != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(1, ipConnectivityEvent);
                    }
                }
            }
            if (this.droppedEvents != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.droppedEvents);
            }
            if (this.version != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(3, this.version);
            }
            return iComputeSerializedSize;
        }

        @Override
        public IpConnectivityLog mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 10);
                    if (this.events != null) {
                        length = this.events.length;
                    } else {
                        length = 0;
                    }
                    IpConnectivityEvent[] ipConnectivityEventArr = new IpConnectivityEvent[repeatedFieldArrayLength + length];
                    if (length != 0) {
                        System.arraycopy(this.events, 0, ipConnectivityEventArr, 0, length);
                    }
                    while (length < ipConnectivityEventArr.length - 1) {
                        ipConnectivityEventArr[length] = new IpConnectivityEvent();
                        codedInputByteBufferNano.readMessage(ipConnectivityEventArr[length]);
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    ipConnectivityEventArr[length] = new IpConnectivityEvent();
                    codedInputByteBufferNano.readMessage(ipConnectivityEventArr[length]);
                    this.events = ipConnectivityEventArr;
                } else if (tag == 16) {
                    this.droppedEvents = codedInputByteBufferNano.readInt32();
                } else if (tag != 24) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.version = codedInputByteBufferNano.readInt32();
                }
            }
        }

        public static IpConnectivityLog parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (IpConnectivityLog) MessageNano.mergeFrom(new IpConnectivityLog(), bArr);
        }

        public static IpConnectivityLog parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new IpConnectivityLog().mergeFrom(codedInputByteBufferNano);
        }
    }
}
