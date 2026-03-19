package com.android.internal.telephony.nano;

import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.ExtendableMessageNano;
import com.android.internal.telephony.protobuf.nano.InternalNano;
import com.android.internal.telephony.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.internal.telephony.protobuf.nano.MessageNano;
import com.android.internal.telephony.protobuf.nano.WireFormatNano;
import com.google.android.mms.pdu.PduHeaders;
import java.io.IOException;

public interface TelephonyProto {

    public interface PdpType {
        public static final int PDP_TYPE_IP = 1;
        public static final int PDP_TYPE_IPV4V6 = 3;
        public static final int PDP_TYPE_IPV6 = 2;
        public static final int PDP_TYPE_PPP = 4;
        public static final int PDP_UNKNOWN = 0;
    }

    public interface RadioAccessTechnology {
        public static final int RAT_1XRTT = 6;
        public static final int RAT_EDGE = 2;
        public static final int RAT_EHRPD = 13;
        public static final int RAT_EVDO_0 = 7;
        public static final int RAT_EVDO_A = 8;
        public static final int RAT_EVDO_B = 12;
        public static final int RAT_GPRS = 1;
        public static final int RAT_GSM = 16;
        public static final int RAT_HSDPA = 9;
        public static final int RAT_HSPA = 11;
        public static final int RAT_HSPAP = 15;
        public static final int RAT_HSUPA = 10;
        public static final int RAT_IS95A = 4;
        public static final int RAT_IS95B = 5;
        public static final int RAT_IWLAN = 18;
        public static final int RAT_LTE = 14;
        public static final int RAT_LTE_CA = 19;
        public static final int RAT_TD_SCDMA = 17;
        public static final int RAT_UMTS = 3;
        public static final int RAT_UNKNOWN = 0;
        public static final int UNKNOWN = -1;
    }

    public interface RilErrno {
        public static final int RIL_E_ABORTED = 66;
        public static final int RIL_E_CANCELLED = 8;
        public static final int RIL_E_DEVICE_IN_USE = 65;
        public static final int RIL_E_DIAL_MODIFIED_TO_DIAL = 21;
        public static final int RIL_E_DIAL_MODIFIED_TO_SS = 20;
        public static final int RIL_E_DIAL_MODIFIED_TO_USSD = 19;
        public static final int RIL_E_EMPTY_RECORD = 56;
        public static final int RIL_E_ENCODING_ERR = 58;
        public static final int RIL_E_FDN_CHECK_FAILURE = 15;
        public static final int RIL_E_GENERIC_FAILURE = 3;
        public static final int RIL_E_ILLEGAL_SIM_OR_ME = 16;
        public static final int RIL_E_INTERNAL_ERR = 39;
        public static final int RIL_E_INVALID_ARGUMENTS = 45;
        public static final int RIL_E_INVALID_CALL_ID = 48;
        public static final int RIL_E_INVALID_MODEM_STATE = 47;
        public static final int RIL_E_INVALID_RESPONSE = 67;
        public static final int RIL_E_INVALID_SIM_STATE = 46;
        public static final int RIL_E_INVALID_SMSC_ADDRESS = 59;
        public static final int RIL_E_INVALID_SMS_FORMAT = 57;
        public static final int RIL_E_INVALID_STATE = 42;
        public static final int RIL_E_LCE_NOT_SUPPORTED = 36;
        public static final int RIL_E_LCE_NOT_SUPPORTED_NEW = 37;
        public static final int RIL_E_MISSING_RESOURCE = 17;
        public static final int RIL_E_MODEM_ERR = 41;
        public static final int RIL_E_MODE_NOT_SUPPORTED = 14;
        public static final int RIL_E_NETWORK_ERR = 50;
        public static final int RIL_E_NETWORK_NOT_READY = 61;
        public static final int RIL_E_NETWORK_REJECT = 54;
        public static final int RIL_E_NOT_PROVISIONED = 62;
        public static final int RIL_E_NO_MEMORY = 38;
        public static final int RIL_E_NO_NETWORK_FOUND = 64;
        public static final int RIL_E_NO_RESOURCES = 43;
        public static final int RIL_E_NO_SMS_TO_ACK = 49;
        public static final int RIL_E_NO_SUBSCRIPTION = 63;
        public static final int RIL_E_NO_SUCH_ELEMENT = 18;
        public static final int RIL_E_NO_SUCH_ENTRY = 60;
        public static final int RIL_E_OPERATION_NOT_ALLOWED = 55;
        public static final int RIL_E_OP_NOT_ALLOWED_BEFORE_REG_TO_NW = 10;
        public static final int RIL_E_OP_NOT_ALLOWED_DURING_VOICE_CALL = 9;
        public static final int RIL_E_PASSWORD_INCORRECT = 4;
        public static final int RIL_E_RADIO_NOT_AVAILABLE = 2;
        public static final int RIL_E_REQUEST_NOT_SUPPORTED = 7;
        public static final int RIL_E_REQUEST_RATE_LIMITED = 51;
        public static final int RIL_E_SIM_ABSENT = 12;
        public static final int RIL_E_SIM_BUSY = 52;
        public static final int RIL_E_SIM_ERR = 44;
        public static final int RIL_E_SIM_FULL = 53;
        public static final int RIL_E_SIM_PIN2 = 5;
        public static final int RIL_E_SIM_PUK2 = 6;
        public static final int RIL_E_SMS_SEND_FAIL_RETRY = 11;
        public static final int RIL_E_SS_MODIFIED_TO_DIAL = 25;
        public static final int RIL_E_SS_MODIFIED_TO_SS = 28;
        public static final int RIL_E_SS_MODIFIED_TO_USSD = 26;
        public static final int RIL_E_SUBSCRIPTION_NOT_AVAILABLE = 13;
        public static final int RIL_E_SUBSCRIPTION_NOT_SUPPORTED = 27;
        public static final int RIL_E_SUCCESS = 1;
        public static final int RIL_E_SYSTEM_ERR = 40;
        public static final int RIL_E_UNKNOWN = 0;
        public static final int RIL_E_USSD_MODIFIED_TO_DIAL = 22;
        public static final int RIL_E_USSD_MODIFIED_TO_SS = 23;
        public static final int RIL_E_USSD_MODIFIED_TO_USSD = 24;
    }

    public interface TimeInterval {
        public static final int TI_100_MILLIS = 4;
        public static final int TI_10_MILLIS = 1;
        public static final int TI_10_MINUTES = 14;
        public static final int TI_10_SEC = 10;
        public static final int TI_1_HOUR = 16;
        public static final int TI_1_MINUTE = 12;
        public static final int TI_1_SEC = 7;
        public static final int TI_200_MILLIS = 5;
        public static final int TI_20_MILLIS = 2;
        public static final int TI_2_HOURS = 17;
        public static final int TI_2_SEC = 8;
        public static final int TI_30_MINUTES = 15;
        public static final int TI_30_SEC = 11;
        public static final int TI_3_MINUTES = 13;
        public static final int TI_4_HOURS = 18;
        public static final int TI_500_MILLIS = 6;
        public static final int TI_50_MILLIS = 3;
        public static final int TI_5_SEC = 9;
        public static final int TI_MANY_HOURS = 19;
        public static final int TI_UNKNOWN = 0;
    }

    public static final class TelephonyLog extends ExtendableMessageNano<TelephonyLog> {
        private static volatile TelephonyLog[] _emptyArray;
        public TelephonyCallSession[] callSessions;
        public Time endTime;
        public TelephonyEvent[] events;
        public boolean eventsDropped;
        public TelephonyHistogram[] histograms;
        public ModemPowerStats modemPowerStats;
        public SmsSession[] smsSessions;
        public Time startTime;

        public static TelephonyLog[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyLog[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyLog() {
            clear();
        }

        public TelephonyLog clear() {
            this.events = TelephonyEvent.emptyArray();
            this.callSessions = TelephonyCallSession.emptyArray();
            this.smsSessions = SmsSession.emptyArray();
            this.histograms = TelephonyHistogram.emptyArray();
            this.eventsDropped = false;
            this.startTime = null;
            this.endTime = null;
            this.modemPowerStats = null;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.events != null && this.events.length > 0) {
                for (int i = 0; i < this.events.length; i++) {
                    TelephonyEvent telephonyEvent = this.events[i];
                    if (telephonyEvent != null) {
                        codedOutputByteBufferNano.writeMessage(1, telephonyEvent);
                    }
                }
            }
            if (this.callSessions != null && this.callSessions.length > 0) {
                for (int i2 = 0; i2 < this.callSessions.length; i2++) {
                    TelephonyCallSession telephonyCallSession = this.callSessions[i2];
                    if (telephonyCallSession != null) {
                        codedOutputByteBufferNano.writeMessage(2, telephonyCallSession);
                    }
                }
            }
            if (this.smsSessions != null && this.smsSessions.length > 0) {
                for (int i3 = 0; i3 < this.smsSessions.length; i3++) {
                    SmsSession smsSession = this.smsSessions[i3];
                    if (smsSession != null) {
                        codedOutputByteBufferNano.writeMessage(3, smsSession);
                    }
                }
            }
            if (this.histograms != null && this.histograms.length > 0) {
                for (int i4 = 0; i4 < this.histograms.length; i4++) {
                    TelephonyHistogram telephonyHistogram = this.histograms[i4];
                    if (telephonyHistogram != null) {
                        codedOutputByteBufferNano.writeMessage(4, telephonyHistogram);
                    }
                }
            }
            if (this.eventsDropped) {
                codedOutputByteBufferNano.writeBool(5, this.eventsDropped);
            }
            if (this.startTime != null) {
                codedOutputByteBufferNano.writeMessage(6, this.startTime);
            }
            if (this.endTime != null) {
                codedOutputByteBufferNano.writeMessage(7, this.endTime);
            }
            if (this.modemPowerStats != null) {
                codedOutputByteBufferNano.writeMessage(8, this.modemPowerStats);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.events != null && this.events.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i = 0; i < this.events.length; i++) {
                    TelephonyEvent telephonyEvent = this.events[i];
                    if (telephonyEvent != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(1, telephonyEvent);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.callSessions != null && this.callSessions.length > 0) {
                int iComputeMessageSize2 = iComputeSerializedSize;
                for (int i2 = 0; i2 < this.callSessions.length; i2++) {
                    TelephonyCallSession telephonyCallSession = this.callSessions[i2];
                    if (telephonyCallSession != null) {
                        iComputeMessageSize2 += CodedOutputByteBufferNano.computeMessageSize(2, telephonyCallSession);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize2;
            }
            if (this.smsSessions != null && this.smsSessions.length > 0) {
                int iComputeMessageSize3 = iComputeSerializedSize;
                for (int i3 = 0; i3 < this.smsSessions.length; i3++) {
                    SmsSession smsSession = this.smsSessions[i3];
                    if (smsSession != null) {
                        iComputeMessageSize3 += CodedOutputByteBufferNano.computeMessageSize(3, smsSession);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize3;
            }
            if (this.histograms != null && this.histograms.length > 0) {
                for (int i4 = 0; i4 < this.histograms.length; i4++) {
                    TelephonyHistogram telephonyHistogram = this.histograms[i4];
                    if (telephonyHistogram != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(4, telephonyHistogram);
                    }
                }
            }
            if (this.eventsDropped) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(5, this.eventsDropped);
            }
            if (this.startTime != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(6, this.startTime);
            }
            if (this.endTime != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(7, this.endTime);
            }
            if (this.modemPowerStats != null) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(8, this.modemPowerStats);
            }
            return iComputeSerializedSize;
        }

        @Override
        public TelephonyLog mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            int length3;
            int length4;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 10);
                    if (this.events != null) {
                        length4 = this.events.length;
                    } else {
                        length4 = 0;
                    }
                    TelephonyEvent[] telephonyEventArr = new TelephonyEvent[repeatedFieldArrayLength + length4];
                    if (length4 != 0) {
                        System.arraycopy(this.events, 0, telephonyEventArr, 0, length4);
                    }
                    while (length4 < telephonyEventArr.length - 1) {
                        telephonyEventArr[length4] = new TelephonyEvent();
                        codedInputByteBufferNano.readMessage(telephonyEventArr[length4]);
                        codedInputByteBufferNano.readTag();
                        length4++;
                    }
                    telephonyEventArr[length4] = new TelephonyEvent();
                    codedInputByteBufferNano.readMessage(telephonyEventArr[length4]);
                    this.events = telephonyEventArr;
                } else if (tag == 18) {
                    int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 18);
                    if (this.callSessions != null) {
                        length3 = this.callSessions.length;
                    } else {
                        length3 = 0;
                    }
                    TelephonyCallSession[] telephonyCallSessionArr = new TelephonyCallSession[repeatedFieldArrayLength2 + length3];
                    if (length3 != 0) {
                        System.arraycopy(this.callSessions, 0, telephonyCallSessionArr, 0, length3);
                    }
                    while (length3 < telephonyCallSessionArr.length - 1) {
                        telephonyCallSessionArr[length3] = new TelephonyCallSession();
                        codedInputByteBufferNano.readMessage(telephonyCallSessionArr[length3]);
                        codedInputByteBufferNano.readTag();
                        length3++;
                    }
                    telephonyCallSessionArr[length3] = new TelephonyCallSession();
                    codedInputByteBufferNano.readMessage(telephonyCallSessionArr[length3]);
                    this.callSessions = telephonyCallSessionArr;
                } else if (tag == 26) {
                    int repeatedFieldArrayLength3 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 26);
                    if (this.smsSessions != null) {
                        length2 = this.smsSessions.length;
                    } else {
                        length2 = 0;
                    }
                    SmsSession[] smsSessionArr = new SmsSession[repeatedFieldArrayLength3 + length2];
                    if (length2 != 0) {
                        System.arraycopy(this.smsSessions, 0, smsSessionArr, 0, length2);
                    }
                    while (length2 < smsSessionArr.length - 1) {
                        smsSessionArr[length2] = new SmsSession();
                        codedInputByteBufferNano.readMessage(smsSessionArr[length2]);
                        codedInputByteBufferNano.readTag();
                        length2++;
                    }
                    smsSessionArr[length2] = new SmsSession();
                    codedInputByteBufferNano.readMessage(smsSessionArr[length2]);
                    this.smsSessions = smsSessionArr;
                } else if (tag == 34) {
                    int repeatedFieldArrayLength4 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 34);
                    if (this.histograms != null) {
                        length = this.histograms.length;
                    } else {
                        length = 0;
                    }
                    TelephonyHistogram[] telephonyHistogramArr = new TelephonyHistogram[repeatedFieldArrayLength4 + length];
                    if (length != 0) {
                        System.arraycopy(this.histograms, 0, telephonyHistogramArr, 0, length);
                    }
                    while (length < telephonyHistogramArr.length - 1) {
                        telephonyHistogramArr[length] = new TelephonyHistogram();
                        codedInputByteBufferNano.readMessage(telephonyHistogramArr[length]);
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    telephonyHistogramArr[length] = new TelephonyHistogram();
                    codedInputByteBufferNano.readMessage(telephonyHistogramArr[length]);
                    this.histograms = telephonyHistogramArr;
                } else if (tag == 40) {
                    this.eventsDropped = codedInputByteBufferNano.readBool();
                } else if (tag == 50) {
                    if (this.startTime == null) {
                        this.startTime = new Time();
                    }
                    codedInputByteBufferNano.readMessage(this.startTime);
                } else if (tag == 58) {
                    if (this.endTime == null) {
                        this.endTime = new Time();
                    }
                    codedInputByteBufferNano.readMessage(this.endTime);
                } else if (tag != 66) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    if (this.modemPowerStats == null) {
                        this.modemPowerStats = new ModemPowerStats();
                    }
                    codedInputByteBufferNano.readMessage(this.modemPowerStats);
                }
            }
        }

        public static TelephonyLog parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (TelephonyLog) MessageNano.mergeFrom(new TelephonyLog(), bArr);
        }

        public static TelephonyLog parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new TelephonyLog().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class Time extends ExtendableMessageNano<Time> {
        private static volatile Time[] _emptyArray;
        public long elapsedTimestampMillis;
        public long systemTimestampMillis;

        public static Time[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Time[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Time() {
            clear();
        }

        public Time clear() {
            this.systemTimestampMillis = 0L;
            this.elapsedTimestampMillis = 0L;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.systemTimestampMillis != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.systemTimestampMillis);
            }
            if (this.elapsedTimestampMillis != 0) {
                codedOutputByteBufferNano.writeInt64(2, this.elapsedTimestampMillis);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.systemTimestampMillis != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.systemTimestampMillis);
            }
            if (this.elapsedTimestampMillis != 0) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(2, this.elapsedTimestampMillis);
            }
            return iComputeSerializedSize;
        }

        @Override
        public Time mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.systemTimestampMillis = codedInputByteBufferNano.readInt64();
                } else if (tag != 16) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.elapsedTimestampMillis = codedInputByteBufferNano.readInt64();
                }
            }
        }

        public static Time parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (Time) MessageNano.mergeFrom(new Time(), bArr);
        }

        public static Time parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new Time().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class TelephonyHistogram extends ExtendableMessageNano<TelephonyHistogram> {
        private static volatile TelephonyHistogram[] _emptyArray;
        public int avgTimeMillis;
        public int bucketCount;
        public int[] bucketCounters;
        public int[] bucketEndPoints;
        public int category;
        public int count;
        public int id;
        public int maxTimeMillis;
        public int minTimeMillis;

        public static TelephonyHistogram[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyHistogram[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyHistogram() {
            clear();
        }

        public TelephonyHistogram clear() {
            this.category = 0;
            this.id = 0;
            this.minTimeMillis = 0;
            this.maxTimeMillis = 0;
            this.avgTimeMillis = 0;
            this.count = 0;
            this.bucketCount = 0;
            this.bucketEndPoints = WireFormatNano.EMPTY_INT_ARRAY;
            this.bucketCounters = WireFormatNano.EMPTY_INT_ARRAY;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.category != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.category);
            }
            if (this.id != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.id);
            }
            if (this.minTimeMillis != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.minTimeMillis);
            }
            if (this.maxTimeMillis != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.maxTimeMillis);
            }
            if (this.avgTimeMillis != 0) {
                codedOutputByteBufferNano.writeInt32(5, this.avgTimeMillis);
            }
            if (this.count != 0) {
                codedOutputByteBufferNano.writeInt32(6, this.count);
            }
            if (this.bucketCount != 0) {
                codedOutputByteBufferNano.writeInt32(7, this.bucketCount);
            }
            if (this.bucketEndPoints != null && this.bucketEndPoints.length > 0) {
                for (int i = 0; i < this.bucketEndPoints.length; i++) {
                    codedOutputByteBufferNano.writeInt32(8, this.bucketEndPoints[i]);
                }
            }
            if (this.bucketCounters != null && this.bucketCounters.length > 0) {
                for (int i2 = 0; i2 < this.bucketCounters.length; i2++) {
                    codedOutputByteBufferNano.writeInt32(9, this.bucketCounters[i2]);
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.category != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.category);
            }
            if (this.id != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.id);
            }
            if (this.minTimeMillis != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.minTimeMillis);
            }
            if (this.maxTimeMillis != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.maxTimeMillis);
            }
            if (this.avgTimeMillis != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.avgTimeMillis);
            }
            if (this.count != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(6, this.count);
            }
            if (this.bucketCount != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(7, this.bucketCount);
            }
            if (this.bucketEndPoints != null && this.bucketEndPoints.length > 0) {
                int iComputeInt32SizeNoTag = 0;
                for (int i = 0; i < this.bucketEndPoints.length; i++) {
                    iComputeInt32SizeNoTag += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.bucketEndPoints[i]);
                }
                iComputeSerializedSize = iComputeSerializedSize + iComputeInt32SizeNoTag + (this.bucketEndPoints.length * 1);
            }
            if (this.bucketCounters != null && this.bucketCounters.length > 0) {
                int iComputeInt32SizeNoTag2 = 0;
                for (int i2 = 0; i2 < this.bucketCounters.length; i2++) {
                    iComputeInt32SizeNoTag2 += CodedOutputByteBufferNano.computeInt32SizeNoTag(this.bucketCounters[i2]);
                }
                return iComputeSerializedSize + iComputeInt32SizeNoTag2 + (1 * this.bucketCounters.length);
            }
            return iComputeSerializedSize;
        }

        @Override
        public TelephonyHistogram mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            int length3;
            int length4;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.category = codedInputByteBufferNano.readInt32();
                        break;
                    case 16:
                        this.id = codedInputByteBufferNano.readInt32();
                        break;
                    case 24:
                        this.minTimeMillis = codedInputByteBufferNano.readInt32();
                        break;
                    case 32:
                        this.maxTimeMillis = codedInputByteBufferNano.readInt32();
                        break;
                    case 40:
                        this.avgTimeMillis = codedInputByteBufferNano.readInt32();
                        break;
                    case 48:
                        this.count = codedInputByteBufferNano.readInt32();
                        break;
                    case 56:
                        this.bucketCount = codedInputByteBufferNano.readInt32();
                        break;
                    case 64:
                        int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 64);
                        if (this.bucketEndPoints != null) {
                            length = this.bucketEndPoints.length;
                        } else {
                            length = 0;
                        }
                        int[] iArr = new int[repeatedFieldArrayLength + length];
                        if (length != 0) {
                            System.arraycopy(this.bucketEndPoints, 0, iArr, 0, length);
                        }
                        while (length < iArr.length - 1) {
                            iArr[length] = codedInputByteBufferNano.readInt32();
                            codedInputByteBufferNano.readTag();
                            length++;
                        }
                        iArr[length] = codedInputByteBufferNano.readInt32();
                        this.bucketEndPoints = iArr;
                        break;
                    case 66:
                        int iPushLimit = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                        int position = codedInputByteBufferNano.getPosition();
                        int i = 0;
                        while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                            codedInputByteBufferNano.readInt32();
                            i++;
                        }
                        codedInputByteBufferNano.rewindToPosition(position);
                        if (this.bucketEndPoints != null) {
                            length2 = this.bucketEndPoints.length;
                        } else {
                            length2 = 0;
                        }
                        int[] iArr2 = new int[i + length2];
                        if (length2 != 0) {
                            System.arraycopy(this.bucketEndPoints, 0, iArr2, 0, length2);
                        }
                        while (length2 < iArr2.length) {
                            iArr2[length2] = codedInputByteBufferNano.readInt32();
                            length2++;
                        }
                        this.bucketEndPoints = iArr2;
                        codedInputByteBufferNano.popLimit(iPushLimit);
                        break;
                    case 72:
                        int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 72);
                        if (this.bucketCounters != null) {
                            length3 = this.bucketCounters.length;
                        } else {
                            length3 = 0;
                        }
                        int[] iArr3 = new int[repeatedFieldArrayLength2 + length3];
                        if (length3 != 0) {
                            System.arraycopy(this.bucketCounters, 0, iArr3, 0, length3);
                        }
                        while (length3 < iArr3.length - 1) {
                            iArr3[length3] = codedInputByteBufferNano.readInt32();
                            codedInputByteBufferNano.readTag();
                            length3++;
                        }
                        iArr3[length3] = codedInputByteBufferNano.readInt32();
                        this.bucketCounters = iArr3;
                        break;
                    case 74:
                        int iPushLimit2 = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                        int position2 = codedInputByteBufferNano.getPosition();
                        int i2 = 0;
                        while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                            codedInputByteBufferNano.readInt32();
                            i2++;
                        }
                        codedInputByteBufferNano.rewindToPosition(position2);
                        if (this.bucketCounters != null) {
                            length4 = this.bucketCounters.length;
                        } else {
                            length4 = 0;
                        }
                        int[] iArr4 = new int[i2 + length4];
                        if (length4 != 0) {
                            System.arraycopy(this.bucketCounters, 0, iArr4, 0, length4);
                        }
                        while (length4 < iArr4.length) {
                            iArr4[length4] = codedInputByteBufferNano.readInt32();
                            length4++;
                        }
                        this.bucketCounters = iArr4;
                        codedInputByteBufferNano.popLimit(iPushLimit2);
                        break;
                    default:
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static TelephonyHistogram parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (TelephonyHistogram) MessageNano.mergeFrom(new TelephonyHistogram(), bArr);
        }

        public static TelephonyHistogram parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new TelephonyHistogram().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class TelephonySettings extends ExtendableMessageNano<TelephonySettings> {
        private static volatile TelephonySettings[] _emptyArray;
        public boolean isAirplaneMode;
        public boolean isCellularDataEnabled;
        public boolean isDataRoamingEnabled;
        public boolean isEnhanced4GLteModeEnabled;
        public boolean isVtOverLteEnabled;
        public boolean isVtOverWifiEnabled;
        public boolean isWifiCallingEnabled;
        public boolean isWifiEnabled;
        public int preferredNetworkMode;
        public int wifiCallingMode;

        public interface RilNetworkMode {
            public static final int NETWORK_MODE_CDMA = 5;
            public static final int NETWORK_MODE_CDMA_NO_EVDO = 6;
            public static final int NETWORK_MODE_EVDO_NO_CDMA = 7;
            public static final int NETWORK_MODE_GLOBAL = 8;
            public static final int NETWORK_MODE_GSM_ONLY = 2;
            public static final int NETWORK_MODE_GSM_UMTS = 4;
            public static final int NETWORK_MODE_LTE_CDMA_EVDO = 9;
            public static final int NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA = 11;
            public static final int NETWORK_MODE_LTE_GSM_WCDMA = 10;
            public static final int NETWORK_MODE_LTE_ONLY = 12;
            public static final int NETWORK_MODE_LTE_TDSCDMA = 16;
            public static final int NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 23;
            public static final int NETWORK_MODE_LTE_TDSCDMA_GSM = 18;
            public static final int NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA = 21;
            public static final int NETWORK_MODE_LTE_TDSCDMA_WCDMA = 20;
            public static final int NETWORK_MODE_LTE_WCDMA = 13;
            public static final int NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 22;
            public static final int NETWORK_MODE_TDSCDMA_GSM = 17;
            public static final int NETWORK_MODE_TDSCDMA_GSM_WCDMA = 19;
            public static final int NETWORK_MODE_TDSCDMA_ONLY = 14;
            public static final int NETWORK_MODE_TDSCDMA_WCDMA = 15;
            public static final int NETWORK_MODE_UNKNOWN = 0;
            public static final int NETWORK_MODE_WCDMA_ONLY = 3;
            public static final int NETWORK_MODE_WCDMA_PREF = 1;
        }

        public interface WiFiCallingMode {
            public static final int WFC_MODE_CELLULAR_PREFERRED = 2;
            public static final int WFC_MODE_UNKNOWN = 0;
            public static final int WFC_MODE_WIFI_ONLY = 1;
            public static final int WFC_MODE_WIFI_PREFERRED = 3;
        }

        public static TelephonySettings[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonySettings[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonySettings() {
            clear();
        }

        public TelephonySettings clear() {
            this.isAirplaneMode = false;
            this.isCellularDataEnabled = false;
            this.isDataRoamingEnabled = false;
            this.preferredNetworkMode = 0;
            this.isEnhanced4GLteModeEnabled = false;
            this.isWifiEnabled = false;
            this.isWifiCallingEnabled = false;
            this.wifiCallingMode = 0;
            this.isVtOverLteEnabled = false;
            this.isVtOverWifiEnabled = false;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.isAirplaneMode) {
                codedOutputByteBufferNano.writeBool(1, this.isAirplaneMode);
            }
            if (this.isCellularDataEnabled) {
                codedOutputByteBufferNano.writeBool(2, this.isCellularDataEnabled);
            }
            if (this.isDataRoamingEnabled) {
                codedOutputByteBufferNano.writeBool(3, this.isDataRoamingEnabled);
            }
            if (this.preferredNetworkMode != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.preferredNetworkMode);
            }
            if (this.isEnhanced4GLteModeEnabled) {
                codedOutputByteBufferNano.writeBool(5, this.isEnhanced4GLteModeEnabled);
            }
            if (this.isWifiEnabled) {
                codedOutputByteBufferNano.writeBool(6, this.isWifiEnabled);
            }
            if (this.isWifiCallingEnabled) {
                codedOutputByteBufferNano.writeBool(7, this.isWifiCallingEnabled);
            }
            if (this.wifiCallingMode != 0) {
                codedOutputByteBufferNano.writeInt32(8, this.wifiCallingMode);
            }
            if (this.isVtOverLteEnabled) {
                codedOutputByteBufferNano.writeBool(9, this.isVtOverLteEnabled);
            }
            if (this.isVtOverWifiEnabled) {
                codedOutputByteBufferNano.writeBool(10, this.isVtOverWifiEnabled);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.isAirplaneMode) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(1, this.isAirplaneMode);
            }
            if (this.isCellularDataEnabled) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(2, this.isCellularDataEnabled);
            }
            if (this.isDataRoamingEnabled) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(3, this.isDataRoamingEnabled);
            }
            if (this.preferredNetworkMode != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.preferredNetworkMode);
            }
            if (this.isEnhanced4GLteModeEnabled) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(5, this.isEnhanced4GLteModeEnabled);
            }
            if (this.isWifiEnabled) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(6, this.isWifiEnabled);
            }
            if (this.isWifiCallingEnabled) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(7, this.isWifiCallingEnabled);
            }
            if (this.wifiCallingMode != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(8, this.wifiCallingMode);
            }
            if (this.isVtOverLteEnabled) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(9, this.isVtOverLteEnabled);
            }
            if (this.isVtOverWifiEnabled) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeBoolSize(10, this.isVtOverWifiEnabled);
            }
            return iComputeSerializedSize;
        }

        @Override
        public TelephonySettings mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.isAirplaneMode = codedInputByteBufferNano.readBool();
                        break;
                    case 16:
                        this.isCellularDataEnabled = codedInputByteBufferNano.readBool();
                        break;
                    case 24:
                        this.isDataRoamingEnabled = codedInputByteBufferNano.readBool();
                        break;
                    case 32:
                        int position = codedInputByteBufferNano.getPosition();
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
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                            case 21:
                            case 22:
                            case 23:
                                this.preferredNetworkMode = int32;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                        break;
                    case 40:
                        this.isEnhanced4GLteModeEnabled = codedInputByteBufferNano.readBool();
                        break;
                    case 48:
                        this.isWifiEnabled = codedInputByteBufferNano.readBool();
                        break;
                    case 56:
                        this.isWifiCallingEnabled = codedInputByteBufferNano.readBool();
                        break;
                    case 64:
                        int position2 = codedInputByteBufferNano.getPosition();
                        int int322 = codedInputByteBufferNano.readInt32();
                        switch (int322) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.wifiCallingMode = int322;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position2);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                        break;
                    case 72:
                        this.isVtOverLteEnabled = codedInputByteBufferNano.readBool();
                        break;
                    case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                        this.isVtOverWifiEnabled = codedInputByteBufferNano.readBool();
                        break;
                    default:
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static TelephonySettings parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (TelephonySettings) MessageNano.mergeFrom(new TelephonySettings(), bArr);
        }

        public static TelephonySettings parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new TelephonySettings().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class TelephonyServiceState extends ExtendableMessageNano<TelephonyServiceState> {
        private static volatile TelephonyServiceState[] _emptyArray;
        public TelephonyOperator dataOperator;
        public int dataRat;
        public int dataRoamingType;
        public TelephonyOperator voiceOperator;
        public int voiceRat;
        public int voiceRoamingType;

        public interface RoamingType {
            public static final int ROAMING_TYPE_DOMESTIC = 2;
            public static final int ROAMING_TYPE_INTERNATIONAL = 3;
            public static final int ROAMING_TYPE_NOT_ROAMING = 0;
            public static final int ROAMING_TYPE_UNKNOWN = 1;
            public static final int UNKNOWN = -1;
        }

        public static final class TelephonyOperator extends ExtendableMessageNano<TelephonyOperator> {
            private static volatile TelephonyOperator[] _emptyArray;
            public String alphaLong;
            public String alphaShort;
            public String numeric;

            public static TelephonyOperator[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new TelephonyOperator[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public TelephonyOperator() {
                clear();
            }

            public TelephonyOperator clear() {
                this.alphaLong = "";
                this.alphaShort = "";
                this.numeric = "";
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (!this.alphaLong.equals("")) {
                    codedOutputByteBufferNano.writeString(1, this.alphaLong);
                }
                if (!this.alphaShort.equals("")) {
                    codedOutputByteBufferNano.writeString(2, this.alphaShort);
                }
                if (!this.numeric.equals("")) {
                    codedOutputByteBufferNano.writeString(3, this.numeric);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (!this.alphaLong.equals("")) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(1, this.alphaLong);
                }
                if (!this.alphaShort.equals("")) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(2, this.alphaShort);
                }
                if (!this.numeric.equals("")) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeStringSize(3, this.numeric);
                }
                return iComputeSerializedSize;
            }

            @Override
            public TelephonyOperator mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 10) {
                        this.alphaLong = codedInputByteBufferNano.readString();
                    } else if (tag == 18) {
                        this.alphaShort = codedInputByteBufferNano.readString();
                    } else if (tag != 26) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        this.numeric = codedInputByteBufferNano.readString();
                    }
                }
            }

            public static TelephonyOperator parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (TelephonyOperator) MessageNano.mergeFrom(new TelephonyOperator(), bArr);
            }

            public static TelephonyOperator parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new TelephonyOperator().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static TelephonyServiceState[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyServiceState[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyServiceState() {
            clear();
        }

        public TelephonyServiceState clear() {
            this.voiceOperator = null;
            this.dataOperator = null;
            this.voiceRoamingType = -1;
            this.dataRoamingType = -1;
            this.voiceRat = -1;
            this.dataRat = -1;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.voiceOperator != null) {
                codedOutputByteBufferNano.writeMessage(1, this.voiceOperator);
            }
            if (this.dataOperator != null) {
                codedOutputByteBufferNano.writeMessage(2, this.dataOperator);
            }
            if (this.voiceRoamingType != -1) {
                codedOutputByteBufferNano.writeInt32(3, this.voiceRoamingType);
            }
            if (this.dataRoamingType != -1) {
                codedOutputByteBufferNano.writeInt32(4, this.dataRoamingType);
            }
            if (this.voiceRat != -1) {
                codedOutputByteBufferNano.writeInt32(5, this.voiceRat);
            }
            if (this.dataRat != -1) {
                codedOutputByteBufferNano.writeInt32(6, this.dataRat);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.voiceOperator != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(1, this.voiceOperator);
            }
            if (this.dataOperator != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(2, this.dataOperator);
            }
            if (this.voiceRoamingType != -1) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.voiceRoamingType);
            }
            if (this.dataRoamingType != -1) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.dataRoamingType);
            }
            if (this.voiceRat != -1) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.voiceRat);
            }
            if (this.dataRat != -1) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(6, this.dataRat);
            }
            return iComputeSerializedSize;
        }

        @Override
        public TelephonyServiceState mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    if (this.voiceOperator == null) {
                        this.voiceOperator = new TelephonyOperator();
                    }
                    codedInputByteBufferNano.readMessage(this.voiceOperator);
                } else if (tag == 18) {
                    if (this.dataOperator == null) {
                        this.dataOperator = new TelephonyOperator();
                    }
                    codedInputByteBufferNano.readMessage(this.dataOperator);
                } else if (tag == 24) {
                    int position = codedInputByteBufferNano.getPosition();
                    int int32 = codedInputByteBufferNano.readInt32();
                    switch (int32) {
                        case -1:
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            this.voiceRoamingType = int32;
                            break;
                        default:
                            codedInputByteBufferNano.rewindToPosition(position);
                            storeUnknownField(codedInputByteBufferNano, tag);
                            break;
                    }
                } else if (tag == 32) {
                    int position2 = codedInputByteBufferNano.getPosition();
                    int int322 = codedInputByteBufferNano.readInt32();
                    switch (int322) {
                        case -1:
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            this.dataRoamingType = int322;
                            break;
                        default:
                            codedInputByteBufferNano.rewindToPosition(position2);
                            storeUnknownField(codedInputByteBufferNano, tag);
                            break;
                    }
                } else if (tag == 40) {
                    int position3 = codedInputByteBufferNano.getPosition();
                    int int323 = codedInputByteBufferNano.readInt32();
                    switch (int323) {
                        case -1:
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
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                        case 18:
                        case 19:
                            this.voiceRat = int323;
                            break;
                        default:
                            codedInputByteBufferNano.rewindToPosition(position3);
                            storeUnknownField(codedInputByteBufferNano, tag);
                            break;
                    }
                } else if (tag != 48) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    int position4 = codedInputByteBufferNano.getPosition();
                    int int324 = codedInputByteBufferNano.readInt32();
                    switch (int324) {
                        case -1:
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
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                        case 18:
                        case 19:
                            this.dataRat = int324;
                            break;
                        default:
                            codedInputByteBufferNano.rewindToPosition(position4);
                            storeUnknownField(codedInputByteBufferNano, tag);
                            break;
                    }
                }
            }
        }

        public static TelephonyServiceState parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (TelephonyServiceState) MessageNano.mergeFrom(new TelephonyServiceState(), bArr);
        }

        public static TelephonyServiceState parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new TelephonyServiceState().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ImsReasonInfo extends ExtendableMessageNano<ImsReasonInfo> {
        private static volatile ImsReasonInfo[] _emptyArray;
        public int extraCode;
        public String extraMessage;
        public int reasonCode;

        public static ImsReasonInfo[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ImsReasonInfo[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ImsReasonInfo() {
            clear();
        }

        public ImsReasonInfo clear() {
            this.reasonCode = 0;
            this.extraCode = 0;
            this.extraMessage = "";
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.reasonCode != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.reasonCode);
            }
            if (this.extraCode != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.extraCode);
            }
            if (!this.extraMessage.equals("")) {
                codedOutputByteBufferNano.writeString(3, this.extraMessage);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.reasonCode != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.reasonCode);
            }
            if (this.extraCode != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.extraCode);
            }
            if (!this.extraMessage.equals("")) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeStringSize(3, this.extraMessage);
            }
            return iComputeSerializedSize;
        }

        @Override
        public ImsReasonInfo mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.reasonCode = codedInputByteBufferNano.readInt32();
                } else if (tag == 16) {
                    this.extraCode = codedInputByteBufferNano.readInt32();
                } else if (tag != 26) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.extraMessage = codedInputByteBufferNano.readString();
                }
            }
        }

        public static ImsReasonInfo parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ImsReasonInfo) MessageNano.mergeFrom(new ImsReasonInfo(), bArr);
        }

        public static ImsReasonInfo parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ImsReasonInfo().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ImsConnectionState extends ExtendableMessageNano<ImsConnectionState> {
        private static volatile ImsConnectionState[] _emptyArray;
        public ImsReasonInfo reasonInfo;
        public int state;

        public interface State {
            public static final int CONNECTED = 1;
            public static final int DISCONNECTED = 3;
            public static final int PROGRESSING = 2;
            public static final int RESUMED = 4;
            public static final int STATE_UNKNOWN = 0;
            public static final int SUSPENDED = 5;
        }

        public static ImsConnectionState[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ImsConnectionState[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ImsConnectionState() {
            clear();
        }

        public ImsConnectionState clear() {
            this.state = 0;
            this.reasonInfo = null;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.state != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.state);
            }
            if (this.reasonInfo != null) {
                codedOutputByteBufferNano.writeMessage(2, this.reasonInfo);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.state != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.state);
            }
            if (this.reasonInfo != null) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(2, this.reasonInfo);
            }
            return iComputeSerializedSize;
        }

        @Override
        public ImsConnectionState mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    int position = codedInputByteBufferNano.getPosition();
                    int int32 = codedInputByteBufferNano.readInt32();
                    switch (int32) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            this.state = int32;
                            break;
                        default:
                            codedInputByteBufferNano.rewindToPosition(position);
                            storeUnknownField(codedInputByteBufferNano, tag);
                            break;
                    }
                } else if (tag != 18) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    if (this.reasonInfo == null) {
                        this.reasonInfo = new ImsReasonInfo();
                    }
                    codedInputByteBufferNano.readMessage(this.reasonInfo);
                }
            }
        }

        public static ImsConnectionState parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ImsConnectionState) MessageNano.mergeFrom(new ImsConnectionState(), bArr);
        }

        public static ImsConnectionState parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ImsConnectionState().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ImsCapabilities extends ExtendableMessageNano<ImsCapabilities> {
        private static volatile ImsCapabilities[] _emptyArray;
        public boolean utOverLte;
        public boolean utOverWifi;
        public boolean videoOverLte;
        public boolean videoOverWifi;
        public boolean voiceOverLte;
        public boolean voiceOverWifi;

        public static ImsCapabilities[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ImsCapabilities[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ImsCapabilities() {
            clear();
        }

        public ImsCapabilities clear() {
            this.voiceOverLte = false;
            this.voiceOverWifi = false;
            this.videoOverLte = false;
            this.videoOverWifi = false;
            this.utOverLte = false;
            this.utOverWifi = false;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.voiceOverLte) {
                codedOutputByteBufferNano.writeBool(1, this.voiceOverLte);
            }
            if (this.voiceOverWifi) {
                codedOutputByteBufferNano.writeBool(2, this.voiceOverWifi);
            }
            if (this.videoOverLte) {
                codedOutputByteBufferNano.writeBool(3, this.videoOverLte);
            }
            if (this.videoOverWifi) {
                codedOutputByteBufferNano.writeBool(4, this.videoOverWifi);
            }
            if (this.utOverLte) {
                codedOutputByteBufferNano.writeBool(5, this.utOverLte);
            }
            if (this.utOverWifi) {
                codedOutputByteBufferNano.writeBool(6, this.utOverWifi);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.voiceOverLte) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(1, this.voiceOverLte);
            }
            if (this.voiceOverWifi) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(2, this.voiceOverWifi);
            }
            if (this.videoOverLte) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(3, this.videoOverLte);
            }
            if (this.videoOverWifi) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(4, this.videoOverWifi);
            }
            if (this.utOverLte) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(5, this.utOverLte);
            }
            if (this.utOverWifi) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeBoolSize(6, this.utOverWifi);
            }
            return iComputeSerializedSize;
        }

        @Override
        public ImsCapabilities mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.voiceOverLte = codedInputByteBufferNano.readBool();
                } else if (tag == 16) {
                    this.voiceOverWifi = codedInputByteBufferNano.readBool();
                } else if (tag == 24) {
                    this.videoOverLte = codedInputByteBufferNano.readBool();
                } else if (tag == 32) {
                    this.videoOverWifi = codedInputByteBufferNano.readBool();
                } else if (tag == 40) {
                    this.utOverLte = codedInputByteBufferNano.readBool();
                } else if (tag != 48) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.utOverWifi = codedInputByteBufferNano.readBool();
                }
            }
        }

        public static ImsCapabilities parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ImsCapabilities) MessageNano.mergeFrom(new ImsCapabilities(), bArr);
        }

        public static ImsCapabilities parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ImsCapabilities().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class RilDataCall extends ExtendableMessageNano<RilDataCall> {
        private static volatile RilDataCall[] _emptyArray;
        public int cid;
        public String iframe;
        public int type;

        public static RilDataCall[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new RilDataCall[0];
                    }
                }
            }
            return _emptyArray;
        }

        public RilDataCall() {
            clear();
        }

        public RilDataCall clear() {
            this.cid = 0;
            this.type = 0;
            this.iframe = "";
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.cid != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.cid);
            }
            if (this.type != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.type);
            }
            if (!this.iframe.equals("")) {
                codedOutputByteBufferNano.writeString(3, this.iframe);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.cid != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.cid);
            }
            if (this.type != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.type);
            }
            if (!this.iframe.equals("")) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeStringSize(3, this.iframe);
            }
            return iComputeSerializedSize;
        }

        @Override
        public RilDataCall mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.cid = codedInputByteBufferNano.readInt32();
                } else if (tag == 16) {
                    int position = codedInputByteBufferNano.getPosition();
                    int int32 = codedInputByteBufferNano.readInt32();
                    switch (int32) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            this.type = int32;
                            break;
                        default:
                            codedInputByteBufferNano.rewindToPosition(position);
                            storeUnknownField(codedInputByteBufferNano, tag);
                            break;
                    }
                } else if (tag != 26) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.iframe = codedInputByteBufferNano.readString();
                }
            }
        }

        public static RilDataCall parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (RilDataCall) MessageNano.mergeFrom(new RilDataCall(), bArr);
        }

        public static RilDataCall parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new RilDataCall().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class TelephonyEvent extends ExtendableMessageNano<TelephonyEvent> {
        private static volatile TelephonyEvent[] _emptyArray;
        public CarrierIdMatching carrierIdMatching;
        public CarrierKeyChange carrierKeyChange;
        public RilDataCall[] dataCalls;
        public int dataStallAction;
        public RilDeactivateDataCall deactivateDataCall;
        public int error;
        public ImsCapabilities imsCapabilities;
        public ImsConnectionState imsConnectionState;
        public ModemRestart modemRestart;
        public long nitzTimestampMillis;
        public int phoneId;
        public TelephonyServiceState serviceState;
        public TelephonySettings settings;
        public RilSetupDataCall setupDataCall;
        public RilSetupDataCallResponse setupDataCallResponse;
        public long timestampMillis;
        public int type;

        public interface Type {
            public static final int CARRIER_ID_MATCHING = 13;
            public static final int CARRIER_KEY_CHANGED = 14;
            public static final int DATA_CALL_DEACTIVATE = 8;
            public static final int DATA_CALL_DEACTIVATE_RESPONSE = 9;
            public static final int DATA_CALL_LIST_CHANGED = 7;
            public static final int DATA_CALL_SETUP = 5;
            public static final int DATA_CALL_SETUP_RESPONSE = 6;
            public static final int DATA_STALL_ACTION = 10;
            public static final int IMS_CAPABILITIES_CHANGED = 4;
            public static final int IMS_CONNECTION_STATE_CHANGED = 3;
            public static final int MODEM_RESTART = 11;
            public static final int NITZ_TIME = 12;
            public static final int RIL_SERVICE_STATE_CHANGED = 2;
            public static final int SETTINGS_CHANGED = 1;
            public static final int UNKNOWN = 0;
        }

        public static final class RilSetupDataCall extends ExtendableMessageNano<RilSetupDataCall> {
            private static volatile RilSetupDataCall[] _emptyArray;
            public String apn;
            public int dataProfile;
            public int rat;
            public int type;

            public interface RilDataProfile {
                public static final int RIL_DATA_PROFILE_CBS = 5;
                public static final int RIL_DATA_PROFILE_DEFAULT = 1;
                public static final int RIL_DATA_PROFILE_FOTA = 4;
                public static final int RIL_DATA_PROFILE_IMS = 3;
                public static final int RIL_DATA_PROFILE_INVALID = 7;
                public static final int RIL_DATA_PROFILE_OEM_BASE = 6;
                public static final int RIL_DATA_PROFILE_TETHERED = 2;
                public static final int RIL_DATA_UNKNOWN = 0;
            }

            public static RilSetupDataCall[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new RilSetupDataCall[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public RilSetupDataCall() {
                clear();
            }

            public RilSetupDataCall clear() {
                this.rat = -1;
                this.dataProfile = 0;
                this.apn = "";
                this.type = 0;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.rat != -1) {
                    codedOutputByteBufferNano.writeInt32(1, this.rat);
                }
                if (this.dataProfile != 0) {
                    codedOutputByteBufferNano.writeInt32(2, this.dataProfile);
                }
                if (!this.apn.equals("")) {
                    codedOutputByteBufferNano.writeString(3, this.apn);
                }
                if (this.type != 0) {
                    codedOutputByteBufferNano.writeInt32(4, this.type);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.rat != -1) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.rat);
                }
                if (this.dataProfile != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.dataProfile);
                }
                if (!this.apn.equals("")) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(3, this.apn);
                }
                if (this.type != 0) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(4, this.type);
                }
                return iComputeSerializedSize;
            }

            @Override
            public RilSetupDataCall mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 8) {
                        int position = codedInputByteBufferNano.getPosition();
                        int int32 = codedInputByteBufferNano.readInt32();
                        switch (int32) {
                            case -1:
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
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                                this.rat = int32;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                    } else if (tag == 16) {
                        int position2 = codedInputByteBufferNano.getPosition();
                        int int322 = codedInputByteBufferNano.readInt32();
                        switch (int322) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                                this.dataProfile = int322;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position2);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                    } else if (tag == 26) {
                        this.apn = codedInputByteBufferNano.readString();
                    } else if (tag != 32) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        int position3 = codedInputByteBufferNano.getPosition();
                        int int323 = codedInputByteBufferNano.readInt32();
                        switch (int323) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                this.type = int323;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position3);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                    }
                }
            }

            public static RilSetupDataCall parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (RilSetupDataCall) MessageNano.mergeFrom(new RilSetupDataCall(), bArr);
            }

            public static RilSetupDataCall parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new RilSetupDataCall().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static final class RilSetupDataCallResponse extends ExtendableMessageNano<RilSetupDataCallResponse> {
            private static volatile RilSetupDataCallResponse[] _emptyArray;
            public RilDataCall call;
            public int status;
            public int suggestedRetryTimeMillis;

            public interface RilDataCallFailCause {
                public static final int PDP_FAIL_ACTIVATION_REJECT_GGSN = 30;
                public static final int PDP_FAIL_ACTIVATION_REJECT_UNSPECIFIED = 31;
                public static final int PDP_FAIL_APN_TYPE_CONFLICT = 112;
                public static final int PDP_FAIL_AUTH_FAILURE_ON_EMERGENCY_CALL = 122;
                public static final int PDP_FAIL_COMPANION_IFACE_IN_USE = 118;
                public static final int PDP_FAIL_CONDITIONAL_IE_ERROR = 100;
                public static final int PDP_FAIL_DATA_REGISTRATION_FAIL = -2;
                public static final int PDP_FAIL_EMERGENCY_IFACE_ONLY = 116;
                public static final int PDP_FAIL_EMM_ACCESS_BARRED = 115;
                public static final int PDP_FAIL_EMM_ACCESS_BARRED_INFINITE_RETRY = 121;
                public static final int PDP_FAIL_ERROR_UNSPECIFIED = 65535;
                public static final int PDP_FAIL_ESM_INFO_NOT_RECEIVED = 53;
                public static final int PDP_FAIL_FEATURE_NOT_SUPP = 40;
                public static final int PDP_FAIL_FILTER_SEMANTIC_ERROR = 44;
                public static final int PDP_FAIL_FILTER_SYTAX_ERROR = 45;
                public static final int PDP_FAIL_IFACE_AND_POL_FAMILY_MISMATCH = 120;
                public static final int PDP_FAIL_IFACE_MISMATCH = 117;
                public static final int PDP_FAIL_INSUFFICIENT_RESOURCES = 26;
                public static final int PDP_FAIL_INTERNAL_CALL_PREEMPT_BY_HIGH_PRIO_APN = 114;
                public static final int PDP_FAIL_INVALID_MANDATORY_INFO = 96;
                public static final int PDP_FAIL_INVALID_PCSCF_ADDR = 113;
                public static final int PDP_FAIL_INVALID_TRANSACTION_ID = 81;
                public static final int PDP_FAIL_IP_ADDRESS_MISMATCH = 119;
                public static final int PDP_FAIL_LLC_SNDCP = 25;
                public static final int PDP_FAIL_MAX_ACTIVE_PDP_CONTEXT_REACHED = 65;
                public static final int PDP_FAIL_MESSAGE_INCORRECT_SEMANTIC = 95;
                public static final int PDP_FAIL_MESSAGE_TYPE_UNSUPPORTED = 97;
                public static final int PDP_FAIL_MISSING_UKNOWN_APN = 27;
                public static final int PDP_FAIL_MSG_AND_PROTOCOL_STATE_UNCOMPATIBLE = 101;
                public static final int PDP_FAIL_MSG_TYPE_NONCOMPATIBLE_STATE = 98;
                public static final int PDP_FAIL_MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED = 55;
                public static final int PDP_FAIL_NAS_SIGNALLING = 14;
                public static final int PDP_FAIL_NETWORK_FAILURE = 38;
                public static final int PDP_FAIL_NONE = 1;
                public static final int PDP_FAIL_NSAPI_IN_USE = 35;
                public static final int PDP_FAIL_ONLY_IPV4_ALLOWED = 50;
                public static final int PDP_FAIL_ONLY_IPV6_ALLOWED = 51;
                public static final int PDP_FAIL_ONLY_SINGLE_BEARER_ALLOWED = 52;
                public static final int PDP_FAIL_OPERATOR_BARRED = 8;
                public static final int PDP_FAIL_PDN_CONN_DOES_NOT_EXIST = 54;
                public static final int PDP_FAIL_PDP_WITHOUT_ACTIVE_TFT = 46;
                public static final int PDP_FAIL_PREF_RADIO_TECH_CHANGED = -4;
                public static final int PDP_FAIL_PROTOCOL_ERRORS = 111;
                public static final int PDP_FAIL_QOS_NOT_ACCEPTED = 37;
                public static final int PDP_FAIL_RADIO_POWER_OFF = -5;
                public static final int PDP_FAIL_REGULAR_DEACTIVATION = 36;
                public static final int PDP_FAIL_SERVICE_OPTION_NOT_SUBSCRIBED = 33;
                public static final int PDP_FAIL_SERVICE_OPTION_NOT_SUPPORTED = 32;
                public static final int PDP_FAIL_SERVICE_OPTION_OUT_OF_ORDER = 34;
                public static final int PDP_FAIL_SIGNAL_LOST = -3;
                public static final int PDP_FAIL_TETHERED_CALL_ACTIVE = -6;
                public static final int PDP_FAIL_TFT_SEMANTIC_ERROR = 41;
                public static final int PDP_FAIL_TFT_SYTAX_ERROR = 42;
                public static final int PDP_FAIL_UMTS_REACTIVATION_REQ = 39;
                public static final int PDP_FAIL_UNKNOWN = 0;
                public static final int PDP_FAIL_UNKNOWN_INFO_ELEMENT = 99;
                public static final int PDP_FAIL_UNKNOWN_PDP_ADDRESS_TYPE = 28;
                public static final int PDP_FAIL_UNKNOWN_PDP_CONTEXT = 43;
                public static final int PDP_FAIL_UNSUPPORTED_APN_IN_CURRENT_PLMN = 66;
                public static final int PDP_FAIL_USER_AUTHENTICATION = 29;
                public static final int PDP_FAIL_VOICE_REGISTRATION_FAIL = -1;
            }

            public static RilSetupDataCallResponse[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new RilSetupDataCallResponse[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public RilSetupDataCallResponse() {
                clear();
            }

            public RilSetupDataCallResponse clear() {
                this.status = 0;
                this.suggestedRetryTimeMillis = 0;
                this.call = null;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.status != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.status);
                }
                if (this.suggestedRetryTimeMillis != 0) {
                    codedOutputByteBufferNano.writeInt32(2, this.suggestedRetryTimeMillis);
                }
                if (this.call != null) {
                    codedOutputByteBufferNano.writeMessage(3, this.call);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.status != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.status);
                }
                if (this.suggestedRetryTimeMillis != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.suggestedRetryTimeMillis);
                }
                if (this.call != null) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(3, this.call);
                }
                return iComputeSerializedSize;
            }

            @Override
            public RilSetupDataCallResponse mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 8) {
                        int position = codedInputByteBufferNano.getPosition();
                        int int32 = codedInputByteBufferNano.readInt32();
                        if (int32 != 8 && int32 != 14 && int32 != 81 && int32 != 65535) {
                            switch (int32) {
                                case -6:
                                case -5:
                                case -4:
                                case -3:
                                case -2:
                                case -1:
                                case 0:
                                case 1:
                                    break;
                                default:
                                    switch (int32) {
                                        case 25:
                                        case 26:
                                        case 27:
                                        case 28:
                                        case 29:
                                        case 30:
                                        case 31:
                                        case 32:
                                        case 33:
                                        case 34:
                                        case 35:
                                        case 36:
                                        case 37:
                                        case 38:
                                        case 39:
                                        case 40:
                                        case 41:
                                        case 42:
                                        case 43:
                                        case 44:
                                        case 45:
                                        case 46:
                                            break;
                                        default:
                                            switch (int32) {
                                                case 50:
                                                case 51:
                                                case 52:
                                                case 53:
                                                case 54:
                                                case 55:
                                                    break;
                                                default:
                                                    switch (int32) {
                                                        case 65:
                                                        case 66:
                                                            break;
                                                        default:
                                                            switch (int32) {
                                                                case 95:
                                                                case 96:
                                                                case 97:
                                                                case 98:
                                                                case 99:
                                                                case 100:
                                                                case 101:
                                                                    break;
                                                                default:
                                                                    switch (int32) {
                                                                        case 111:
                                                                        case 112:
                                                                        case 113:
                                                                        case 114:
                                                                        case 115:
                                                                        case 116:
                                                                        case 117:
                                                                        case 118:
                                                                        case 119:
                                                                        case 120:
                                                                        case 121:
                                                                        case 122:
                                                                            break;
                                                                        default:
                                                                            codedInputByteBufferNano.rewindToPosition(position);
                                                                            storeUnknownField(codedInputByteBufferNano, tag);
                                                                            continue;
                                                                    }
                                                                    break;
                                                            }
                                                            break;
                                                    }
                                                    break;
                                            }
                                            break;
                                    }
                                    break;
                            }
                        }
                        this.status = int32;
                    } else if (tag == 16) {
                        this.suggestedRetryTimeMillis = codedInputByteBufferNano.readInt32();
                    } else if (tag != 26) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        if (this.call == null) {
                            this.call = new RilDataCall();
                        }
                        codedInputByteBufferNano.readMessage(this.call);
                    }
                }
            }

            public static RilSetupDataCallResponse parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (RilSetupDataCallResponse) MessageNano.mergeFrom(new RilSetupDataCallResponse(), bArr);
            }

            public static RilSetupDataCallResponse parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new RilSetupDataCallResponse().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static final class CarrierKeyChange extends ExtendableMessageNano<CarrierKeyChange> {
            private static volatile CarrierKeyChange[] _emptyArray;
            public boolean isDownloadSuccessful;
            public int keyType;

            public interface KeyType {
                public static final int EPDG = 2;
                public static final int UNKNOWN = 0;
                public static final int WLAN = 1;
            }

            public static CarrierKeyChange[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new CarrierKeyChange[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public CarrierKeyChange() {
                clear();
            }

            public CarrierKeyChange clear() {
                this.keyType = 0;
                this.isDownloadSuccessful = false;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.keyType != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.keyType);
                }
                if (this.isDownloadSuccessful) {
                    codedOutputByteBufferNano.writeBool(2, this.isDownloadSuccessful);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.keyType != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.keyType);
                }
                if (this.isDownloadSuccessful) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeBoolSize(2, this.isDownloadSuccessful);
                }
                return iComputeSerializedSize;
            }

            @Override
            public CarrierKeyChange mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 8) {
                        int position = codedInputByteBufferNano.getPosition();
                        int int32 = codedInputByteBufferNano.readInt32();
                        switch (int32) {
                            case 0:
                            case 1:
                            case 2:
                                this.keyType = int32;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                    } else if (tag != 16) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        this.isDownloadSuccessful = codedInputByteBufferNano.readBool();
                    }
                }
            }

            public static CarrierKeyChange parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (CarrierKeyChange) MessageNano.mergeFrom(new CarrierKeyChange(), bArr);
            }

            public static CarrierKeyChange parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new CarrierKeyChange().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static final class RilDeactivateDataCall extends ExtendableMessageNano<RilDeactivateDataCall> {
            private static volatile RilDeactivateDataCall[] _emptyArray;
            public int cid;
            public int reason;

            public interface DeactivateReason {
                public static final int DEACTIVATE_REASON_HANDOVER = 4;
                public static final int DEACTIVATE_REASON_NONE = 1;
                public static final int DEACTIVATE_REASON_PDP_RESET = 3;
                public static final int DEACTIVATE_REASON_RADIO_OFF = 2;
                public static final int DEACTIVATE_REASON_UNKNOWN = 0;
            }

            public static RilDeactivateDataCall[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new RilDeactivateDataCall[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public RilDeactivateDataCall() {
                clear();
            }

            public RilDeactivateDataCall clear() {
                this.cid = 0;
                this.reason = 0;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.cid != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.cid);
                }
                if (this.reason != 0) {
                    codedOutputByteBufferNano.writeInt32(2, this.reason);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.cid != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.cid);
                }
                if (this.reason != 0) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(2, this.reason);
                }
                return iComputeSerializedSize;
            }

            @Override
            public RilDeactivateDataCall mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 8) {
                        this.cid = codedInputByteBufferNano.readInt32();
                    } else if (tag != 16) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        int position = codedInputByteBufferNano.getPosition();
                        int int32 = codedInputByteBufferNano.readInt32();
                        switch (int32) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                this.reason = int32;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                    }
                }
            }

            public static RilDeactivateDataCall parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (RilDeactivateDataCall) MessageNano.mergeFrom(new RilDeactivateDataCall(), bArr);
            }

            public static RilDeactivateDataCall parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new RilDeactivateDataCall().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static final class ModemRestart extends ExtendableMessageNano<ModemRestart> {
            private static volatile ModemRestart[] _emptyArray;
            public String basebandVersion;
            public String reason;

            public static ModemRestart[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new ModemRestart[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public ModemRestart() {
                clear();
            }

            public ModemRestart clear() {
                this.basebandVersion = "";
                this.reason = "";
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (!this.basebandVersion.equals("")) {
                    codedOutputByteBufferNano.writeString(1, this.basebandVersion);
                }
                if (!this.reason.equals("")) {
                    codedOutputByteBufferNano.writeString(2, this.reason);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (!this.basebandVersion.equals("")) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(1, this.basebandVersion);
                }
                if (!this.reason.equals("")) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeStringSize(2, this.reason);
                }
                return iComputeSerializedSize;
            }

            @Override
            public ModemRestart mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 10) {
                        this.basebandVersion = codedInputByteBufferNano.readString();
                    } else if (tag != 18) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        this.reason = codedInputByteBufferNano.readString();
                    }
                }
            }

            public static ModemRestart parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (ModemRestart) MessageNano.mergeFrom(new ModemRestart(), bArr);
            }

            public static ModemRestart parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new ModemRestart().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static final class CarrierIdMatching extends ExtendableMessageNano<CarrierIdMatching> {
            private static volatile CarrierIdMatching[] _emptyArray;
            public int cidTableVersion;
            public CarrierIdMatchingResult result;

            public static CarrierIdMatching[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new CarrierIdMatching[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public CarrierIdMatching() {
                clear();
            }

            public CarrierIdMatching clear() {
                this.cidTableVersion = 0;
                this.result = null;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.cidTableVersion != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.cidTableVersion);
                }
                if (this.result != null) {
                    codedOutputByteBufferNano.writeMessage(2, this.result);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.cidTableVersion != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.cidTableVersion);
                }
                if (this.result != null) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(2, this.result);
                }
                return iComputeSerializedSize;
            }

            @Override
            public CarrierIdMatching mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 8) {
                        this.cidTableVersion = codedInputByteBufferNano.readInt32();
                    } else if (tag != 18) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        if (this.result == null) {
                            this.result = new CarrierIdMatchingResult();
                        }
                        codedInputByteBufferNano.readMessage(this.result);
                    }
                }
            }

            public static CarrierIdMatching parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (CarrierIdMatching) MessageNano.mergeFrom(new CarrierIdMatching(), bArr);
            }

            public static CarrierIdMatching parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new CarrierIdMatching().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static final class CarrierIdMatchingResult extends ExtendableMessageNano<CarrierIdMatchingResult> {
            private static volatile CarrierIdMatchingResult[] _emptyArray;
            public int carrierId;
            public String gid1;
            public String mccmnc;

            public static CarrierIdMatchingResult[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new CarrierIdMatchingResult[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public CarrierIdMatchingResult() {
                clear();
            }

            public CarrierIdMatchingResult clear() {
                this.carrierId = 0;
                this.gid1 = "";
                this.mccmnc = "";
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.carrierId != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.carrierId);
                }
                if (!this.gid1.equals("")) {
                    codedOutputByteBufferNano.writeString(2, this.gid1);
                }
                if (!this.mccmnc.equals("")) {
                    codedOutputByteBufferNano.writeString(3, this.mccmnc);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.carrierId != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.carrierId);
                }
                if (!this.gid1.equals("")) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(2, this.gid1);
                }
                if (!this.mccmnc.equals("")) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeStringSize(3, this.mccmnc);
                }
                return iComputeSerializedSize;
            }

            @Override
            public CarrierIdMatchingResult mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    if (tag == 0) {
                        return this;
                    }
                    if (tag == 8) {
                        this.carrierId = codedInputByteBufferNano.readInt32();
                    } else if (tag == 18) {
                        this.gid1 = codedInputByteBufferNano.readString();
                    } else if (tag != 26) {
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                    } else {
                        this.mccmnc = codedInputByteBufferNano.readString();
                    }
                }
            }

            public static CarrierIdMatchingResult parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (CarrierIdMatchingResult) MessageNano.mergeFrom(new CarrierIdMatchingResult(), bArr);
            }

            public static CarrierIdMatchingResult parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new CarrierIdMatchingResult().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static TelephonyEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyEvent() {
            clear();
        }

        public TelephonyEvent clear() {
            this.timestampMillis = 0L;
            this.phoneId = 0;
            this.type = 0;
            this.settings = null;
            this.serviceState = null;
            this.imsConnectionState = null;
            this.imsCapabilities = null;
            this.dataCalls = RilDataCall.emptyArray();
            this.error = 0;
            this.setupDataCall = null;
            this.setupDataCallResponse = null;
            this.deactivateDataCall = null;
            this.dataStallAction = 0;
            this.modemRestart = null;
            this.nitzTimestampMillis = 0L;
            this.carrierIdMatching = null;
            this.carrierKeyChange = null;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.timestampMillis != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.timestampMillis);
            }
            if (this.phoneId != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.phoneId);
            }
            if (this.type != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.type);
            }
            if (this.settings != null) {
                codedOutputByteBufferNano.writeMessage(4, this.settings);
            }
            if (this.serviceState != null) {
                codedOutputByteBufferNano.writeMessage(5, this.serviceState);
            }
            if (this.imsConnectionState != null) {
                codedOutputByteBufferNano.writeMessage(6, this.imsConnectionState);
            }
            if (this.imsCapabilities != null) {
                codedOutputByteBufferNano.writeMessage(7, this.imsCapabilities);
            }
            if (this.dataCalls != null && this.dataCalls.length > 0) {
                for (int i = 0; i < this.dataCalls.length; i++) {
                    RilDataCall rilDataCall = this.dataCalls[i];
                    if (rilDataCall != null) {
                        codedOutputByteBufferNano.writeMessage(8, rilDataCall);
                    }
                }
            }
            if (this.error != 0) {
                codedOutputByteBufferNano.writeInt32(9, this.error);
            }
            if (this.setupDataCall != null) {
                codedOutputByteBufferNano.writeMessage(10, this.setupDataCall);
            }
            if (this.setupDataCallResponse != null) {
                codedOutputByteBufferNano.writeMessage(11, this.setupDataCallResponse);
            }
            if (this.deactivateDataCall != null) {
                codedOutputByteBufferNano.writeMessage(12, this.deactivateDataCall);
            }
            if (this.dataStallAction != 0) {
                codedOutputByteBufferNano.writeInt32(13, this.dataStallAction);
            }
            if (this.modemRestart != null) {
                codedOutputByteBufferNano.writeMessage(14, this.modemRestart);
            }
            if (this.nitzTimestampMillis != 0) {
                codedOutputByteBufferNano.writeInt64(15, this.nitzTimestampMillis);
            }
            if (this.carrierIdMatching != null) {
                codedOutputByteBufferNano.writeMessage(16, this.carrierIdMatching);
            }
            if (this.carrierKeyChange != null) {
                codedOutputByteBufferNano.writeMessage(17, this.carrierKeyChange);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.timestampMillis != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.timestampMillis);
            }
            if (this.phoneId != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.phoneId);
            }
            if (this.type != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.type);
            }
            if (this.settings != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(4, this.settings);
            }
            if (this.serviceState != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(5, this.serviceState);
            }
            if (this.imsConnectionState != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(6, this.imsConnectionState);
            }
            if (this.imsCapabilities != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(7, this.imsCapabilities);
            }
            if (this.dataCalls != null && this.dataCalls.length > 0) {
                for (int i = 0; i < this.dataCalls.length; i++) {
                    RilDataCall rilDataCall = this.dataCalls[i];
                    if (rilDataCall != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(8, rilDataCall);
                    }
                }
            }
            if (this.error != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(9, this.error);
            }
            if (this.setupDataCall != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(10, this.setupDataCall);
            }
            if (this.setupDataCallResponse != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(11, this.setupDataCallResponse);
            }
            if (this.deactivateDataCall != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(12, this.deactivateDataCall);
            }
            if (this.dataStallAction != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(13, this.dataStallAction);
            }
            if (this.modemRestart != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(14, this.modemRestart);
            }
            if (this.nitzTimestampMillis != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(15, this.nitzTimestampMillis);
            }
            if (this.carrierIdMatching != null) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(16, this.carrierIdMatching);
            }
            if (this.carrierKeyChange != null) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(17, this.carrierKeyChange);
            }
            return iComputeSerializedSize;
        }

        @Override
        public TelephonyEvent mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.timestampMillis = codedInputByteBufferNano.readInt64();
                        break;
                    case 16:
                        this.phoneId = codedInputByteBufferNano.readInt32();
                        break;
                    case 24:
                        int position = codedInputByteBufferNano.getPosition();
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
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                                this.type = int32;
                                break;
                            default:
                                codedInputByteBufferNano.rewindToPosition(position);
                                storeUnknownField(codedInputByteBufferNano, tag);
                                break;
                        }
                        break;
                    case 34:
                        if (this.settings == null) {
                            this.settings = new TelephonySettings();
                        }
                        codedInputByteBufferNano.readMessage(this.settings);
                        break;
                    case 42:
                        if (this.serviceState == null) {
                            this.serviceState = new TelephonyServiceState();
                        }
                        codedInputByteBufferNano.readMessage(this.serviceState);
                        break;
                    case 50:
                        if (this.imsConnectionState == null) {
                            this.imsConnectionState = new ImsConnectionState();
                        }
                        codedInputByteBufferNano.readMessage(this.imsConnectionState);
                        break;
                    case 58:
                        if (this.imsCapabilities == null) {
                            this.imsCapabilities = new ImsCapabilities();
                        }
                        codedInputByteBufferNano.readMessage(this.imsCapabilities);
                        break;
                    case 66:
                        int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 66);
                        if (this.dataCalls != null) {
                            length = this.dataCalls.length;
                        } else {
                            length = 0;
                        }
                        RilDataCall[] rilDataCallArr = new RilDataCall[repeatedFieldArrayLength + length];
                        if (length != 0) {
                            System.arraycopy(this.dataCalls, 0, rilDataCallArr, 0, length);
                        }
                        while (length < rilDataCallArr.length - 1) {
                            rilDataCallArr[length] = new RilDataCall();
                            codedInputByteBufferNano.readMessage(rilDataCallArr[length]);
                            codedInputByteBufferNano.readTag();
                            length++;
                        }
                        rilDataCallArr[length] = new RilDataCall();
                        codedInputByteBufferNano.readMessage(rilDataCallArr[length]);
                        this.dataCalls = rilDataCallArr;
                        break;
                    case 72:
                        int position2 = codedInputByteBufferNano.getPosition();
                        int int322 = codedInputByteBufferNano.readInt32();
                        switch (int322) {
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
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                            case 21:
                            case 22:
                            case 23:
                            case 24:
                            case 25:
                            case 26:
                            case 27:
                            case 28:
                                this.error = int322;
                                break;
                            default:
                                switch (int322) {
                                    case 36:
                                    case 37:
                                    case 38:
                                    case 39:
                                    case 40:
                                    case 41:
                                    case 42:
                                    case 43:
                                    case 44:
                                    case 45:
                                    case 46:
                                    case 47:
                                    case 48:
                                    case 49:
                                    case 50:
                                    case 51:
                                    case 52:
                                    case 53:
                                    case 54:
                                    case 55:
                                    case 56:
                                    case 57:
                                    case 58:
                                    case 59:
                                    case 60:
                                    case 61:
                                    case 62:
                                    case 63:
                                    case 64:
                                    case 65:
                                    case 66:
                                    case RilErrno.RIL_E_INVALID_RESPONSE:
                                        break;
                                    default:
                                        codedInputByteBufferNano.rewindToPosition(position2);
                                        storeUnknownField(codedInputByteBufferNano, tag);
                                        continue;
                                }
                                this.error = int322;
                                break;
                        }
                        break;
                    case RadioNVItems.RIL_NV_LTE_BSR_MAX_TIME:
                        if (this.setupDataCall == null) {
                            this.setupDataCall = new RilSetupDataCall();
                        }
                        codedInputByteBufferNano.readMessage(this.setupDataCall);
                        break;
                    case 90:
                        if (this.setupDataCallResponse == null) {
                            this.setupDataCallResponse = new RilSetupDataCallResponse();
                        }
                        codedInputByteBufferNano.readMessage(this.setupDataCallResponse);
                        break;
                    case 98:
                        if (this.deactivateDataCall == null) {
                            this.deactivateDataCall = new RilDeactivateDataCall();
                        }
                        codedInputByteBufferNano.readMessage(this.deactivateDataCall);
                        break;
                    case 104:
                        this.dataStallAction = codedInputByteBufferNano.readInt32();
                        break;
                    case 114:
                        if (this.modemRestart == null) {
                            this.modemRestart = new ModemRestart();
                        }
                        codedInputByteBufferNano.readMessage(this.modemRestart);
                        break;
                    case 120:
                        this.nitzTimestampMillis = codedInputByteBufferNano.readInt64();
                        break;
                    case 130:
                        if (this.carrierIdMatching == null) {
                            this.carrierIdMatching = new CarrierIdMatching();
                        }
                        codedInputByteBufferNano.readMessage(this.carrierIdMatching);
                        break;
                    case 138:
                        if (this.carrierKeyChange == null) {
                            this.carrierKeyChange = new CarrierKeyChange();
                        }
                        codedInputByteBufferNano.readMessage(this.carrierKeyChange);
                        break;
                    default:
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static TelephonyEvent parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (TelephonyEvent) MessageNano.mergeFrom(new TelephonyEvent(), bArr);
        }

        public static TelephonyEvent parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new TelephonyEvent().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class TelephonyCallSession extends ExtendableMessageNano<TelephonyCallSession> {
        private static volatile TelephonyCallSession[] _emptyArray;
        public Event[] events;
        public boolean eventsDropped;
        public int phoneId;
        public int startTimeMinutes;

        public static final class Event extends ExtendableMessageNano<Event> {
            private static volatile Event[] _emptyArray;
            public int callIndex;
            public int callState;
            public RilCall[] calls;
            public RilDataCall[] dataCalls;
            public int delay;
            public int error;
            public ImsCapabilities imsCapabilities;
            public int imsCommand;
            public ImsConnectionState imsConnectionState;
            public int mergedCallIndex;
            public long nitzTimestampMillis;
            public int phoneState;
            public ImsReasonInfo reasonInfo;
            public int rilRequest;
            public int rilRequestId;
            public TelephonyServiceState serviceState;
            public TelephonySettings settings;
            public int srcAccessTech;
            public int srvccState;
            public int targetAccessTech;
            public int type;

            public interface CallState {
                public static final int CALL_ACTIVE = 2;
                public static final int CALL_ALERTING = 5;
                public static final int CALL_DIALING = 4;
                public static final int CALL_DISCONNECTED = 8;
                public static final int CALL_DISCONNECTING = 9;
                public static final int CALL_HOLDING = 3;
                public static final int CALL_IDLE = 1;
                public static final int CALL_INCOMING = 6;
                public static final int CALL_UNKNOWN = 0;
                public static final int CALL_WAITING = 7;
            }

            public interface ImsCommand {
                public static final int IMS_CMD_ACCEPT = 2;
                public static final int IMS_CMD_CONFERENCE_EXTEND = 9;
                public static final int IMS_CMD_HOLD = 5;
                public static final int IMS_CMD_INVITE_PARTICIPANT = 10;
                public static final int IMS_CMD_MERGE = 7;
                public static final int IMS_CMD_REJECT = 3;
                public static final int IMS_CMD_REMOVE_PARTICIPANT = 11;
                public static final int IMS_CMD_RESUME = 6;
                public static final int IMS_CMD_START = 1;
                public static final int IMS_CMD_TERMINATE = 4;
                public static final int IMS_CMD_UNKNOWN = 0;
                public static final int IMS_CMD_UPDATE = 8;
            }

            public interface PhoneState {
                public static final int STATE_IDLE = 1;
                public static final int STATE_OFFHOOK = 3;
                public static final int STATE_RINGING = 2;
                public static final int STATE_UNKNOWN = 0;
            }

            public interface RilRequest {
                public static final int RIL_REQUEST_ANSWER = 2;
                public static final int RIL_REQUEST_CDMA_FLASH = 6;
                public static final int RIL_REQUEST_CONFERENCE = 7;
                public static final int RIL_REQUEST_DIAL = 1;
                public static final int RIL_REQUEST_HANGUP = 3;
                public static final int RIL_REQUEST_SET_CALL_WAITING = 4;
                public static final int RIL_REQUEST_SWITCH_HOLDING_AND_ACTIVE = 5;
                public static final int RIL_REQUEST_UNKNOWN = 0;
            }

            public interface RilSrvccState {
                public static final int HANDOVER_CANCELED = 4;
                public static final int HANDOVER_COMPLETED = 2;
                public static final int HANDOVER_FAILED = 3;
                public static final int HANDOVER_STARTED = 1;
                public static final int HANDOVER_UNKNOWN = 0;
            }

            public interface Type {
                public static final int DATA_CALL_LIST_CHANGED = 5;
                public static final int EVENT_UNKNOWN = 0;
                public static final int IMS_CALL_HANDOVER = 18;
                public static final int IMS_CALL_HANDOVER_FAILED = 19;
                public static final int IMS_CALL_RECEIVE = 15;
                public static final int IMS_CALL_STATE_CHANGED = 16;
                public static final int IMS_CALL_TERMINATED = 17;
                public static final int IMS_CAPABILITIES_CHANGED = 4;
                public static final int IMS_COMMAND = 11;
                public static final int IMS_COMMAND_COMPLETE = 14;
                public static final int IMS_COMMAND_FAILED = 13;
                public static final int IMS_COMMAND_RECEIVED = 12;
                public static final int IMS_CONNECTION_STATE_CHANGED = 3;
                public static final int NITZ_TIME = 21;
                public static final int PHONE_STATE_CHANGED = 20;
                public static final int RIL_CALL_LIST_CHANGED = 10;
                public static final int RIL_CALL_RING = 8;
                public static final int RIL_CALL_SRVCC = 9;
                public static final int RIL_REQUEST = 6;
                public static final int RIL_RESPONSE = 7;
                public static final int RIL_SERVICE_STATE_CHANGED = 2;
                public static final int SETTINGS_CHANGED = 1;
            }

            public static final class RilCall extends ExtendableMessageNano<RilCall> {
                private static volatile RilCall[] _emptyArray;
                public int callEndReason;
                public int index;
                public boolean isMultiparty;
                public int state;
                public int type;

                public interface Type {
                    public static final int MO = 1;
                    public static final int MT = 2;
                    public static final int UNKNOWN = 0;
                }

                public static RilCall[] emptyArray() {
                    if (_emptyArray == null) {
                        synchronized (InternalNano.LAZY_INIT_LOCK) {
                            if (_emptyArray == null) {
                                _emptyArray = new RilCall[0];
                            }
                        }
                    }
                    return _emptyArray;
                }

                public RilCall() {
                    clear();
                }

                public RilCall clear() {
                    this.index = 0;
                    this.state = 0;
                    this.type = 0;
                    this.callEndReason = 0;
                    this.isMultiparty = false;
                    this.unknownFieldData = null;
                    this.cachedSize = -1;
                    return this;
                }

                @Override
                public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                    if (this.index != 0) {
                        codedOutputByteBufferNano.writeInt32(1, this.index);
                    }
                    if (this.state != 0) {
                        codedOutputByteBufferNano.writeInt32(2, this.state);
                    }
                    if (this.type != 0) {
                        codedOutputByteBufferNano.writeInt32(3, this.type);
                    }
                    if (this.callEndReason != 0) {
                        codedOutputByteBufferNano.writeInt32(4, this.callEndReason);
                    }
                    if (this.isMultiparty) {
                        codedOutputByteBufferNano.writeBool(5, this.isMultiparty);
                    }
                    super.writeTo(codedOutputByteBufferNano);
                }

                @Override
                protected int computeSerializedSize() {
                    int iComputeSerializedSize = super.computeSerializedSize();
                    if (this.index != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.index);
                    }
                    if (this.state != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.state);
                    }
                    if (this.type != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.type);
                    }
                    if (this.callEndReason != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.callEndReason);
                    }
                    if (this.isMultiparty) {
                        return iComputeSerializedSize + CodedOutputByteBufferNano.computeBoolSize(5, this.isMultiparty);
                    }
                    return iComputeSerializedSize;
                }

                @Override
                public RilCall mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                    while (true) {
                        int tag = codedInputByteBufferNano.readTag();
                        if (tag == 0) {
                            return this;
                        }
                        if (tag == 8) {
                            this.index = codedInputByteBufferNano.readInt32();
                        } else if (tag == 16) {
                            int position = codedInputByteBufferNano.getPosition();
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
                                    this.state = int32;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                        } else if (tag == 24) {
                            int position2 = codedInputByteBufferNano.getPosition();
                            int int322 = codedInputByteBufferNano.readInt32();
                            switch (int322) {
                                case 0:
                                case 1:
                                case 2:
                                    this.type = int322;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position2);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                        } else if (tag == 32) {
                            this.callEndReason = codedInputByteBufferNano.readInt32();
                        } else if (tag != 40) {
                            if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                                return this;
                            }
                        } else {
                            this.isMultiparty = codedInputByteBufferNano.readBool();
                        }
                    }
                }

                public static RilCall parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                    return (RilCall) MessageNano.mergeFrom(new RilCall(), bArr);
                }

                public static RilCall parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                    return new RilCall().mergeFrom(codedInputByteBufferNano);
                }
            }

            public static Event[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new Event[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public Event() {
                clear();
            }

            public Event clear() {
                this.type = 0;
                this.delay = 0;
                this.settings = null;
                this.serviceState = null;
                this.imsConnectionState = null;
                this.imsCapabilities = null;
                this.dataCalls = RilDataCall.emptyArray();
                this.phoneState = 0;
                this.callState = 0;
                this.callIndex = 0;
                this.mergedCallIndex = 0;
                this.calls = RilCall.emptyArray();
                this.error = 0;
                this.rilRequest = 0;
                this.rilRequestId = 0;
                this.srvccState = 0;
                this.imsCommand = 0;
                this.reasonInfo = null;
                this.srcAccessTech = -1;
                this.targetAccessTech = -1;
                this.nitzTimestampMillis = 0L;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.type != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.type);
                }
                if (this.delay != 0) {
                    codedOutputByteBufferNano.writeInt32(2, this.delay);
                }
                if (this.settings != null) {
                    codedOutputByteBufferNano.writeMessage(3, this.settings);
                }
                if (this.serviceState != null) {
                    codedOutputByteBufferNano.writeMessage(4, this.serviceState);
                }
                if (this.imsConnectionState != null) {
                    codedOutputByteBufferNano.writeMessage(5, this.imsConnectionState);
                }
                if (this.imsCapabilities != null) {
                    codedOutputByteBufferNano.writeMessage(6, this.imsCapabilities);
                }
                if (this.dataCalls != null && this.dataCalls.length > 0) {
                    for (int i = 0; i < this.dataCalls.length; i++) {
                        RilDataCall rilDataCall = this.dataCalls[i];
                        if (rilDataCall != null) {
                            codedOutputByteBufferNano.writeMessage(7, rilDataCall);
                        }
                    }
                }
                if (this.phoneState != 0) {
                    codedOutputByteBufferNano.writeInt32(8, this.phoneState);
                }
                if (this.callState != 0) {
                    codedOutputByteBufferNano.writeInt32(9, this.callState);
                }
                if (this.callIndex != 0) {
                    codedOutputByteBufferNano.writeInt32(10, this.callIndex);
                }
                if (this.mergedCallIndex != 0) {
                    codedOutputByteBufferNano.writeInt32(11, this.mergedCallIndex);
                }
                if (this.calls != null && this.calls.length > 0) {
                    for (int i2 = 0; i2 < this.calls.length; i2++) {
                        RilCall rilCall = this.calls[i2];
                        if (rilCall != null) {
                            codedOutputByteBufferNano.writeMessage(12, rilCall);
                        }
                    }
                }
                if (this.error != 0) {
                    codedOutputByteBufferNano.writeInt32(13, this.error);
                }
                if (this.rilRequest != 0) {
                    codedOutputByteBufferNano.writeInt32(14, this.rilRequest);
                }
                if (this.rilRequestId != 0) {
                    codedOutputByteBufferNano.writeInt32(15, this.rilRequestId);
                }
                if (this.srvccState != 0) {
                    codedOutputByteBufferNano.writeInt32(16, this.srvccState);
                }
                if (this.imsCommand != 0) {
                    codedOutputByteBufferNano.writeInt32(17, this.imsCommand);
                }
                if (this.reasonInfo != null) {
                    codedOutputByteBufferNano.writeMessage(18, this.reasonInfo);
                }
                if (this.srcAccessTech != -1) {
                    codedOutputByteBufferNano.writeInt32(19, this.srcAccessTech);
                }
                if (this.targetAccessTech != -1) {
                    codedOutputByteBufferNano.writeInt32(20, this.targetAccessTech);
                }
                if (this.nitzTimestampMillis != 0) {
                    codedOutputByteBufferNano.writeInt64(21, this.nitzTimestampMillis);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.type != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.type);
                }
                if (this.delay != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.delay);
                }
                if (this.settings != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(3, this.settings);
                }
                if (this.serviceState != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(4, this.serviceState);
                }
                if (this.imsConnectionState != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(5, this.imsConnectionState);
                }
                if (this.imsCapabilities != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(6, this.imsCapabilities);
                }
                if (this.dataCalls != null && this.dataCalls.length > 0) {
                    int iComputeMessageSize = iComputeSerializedSize;
                    for (int i = 0; i < this.dataCalls.length; i++) {
                        RilDataCall rilDataCall = this.dataCalls[i];
                        if (rilDataCall != null) {
                            iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(7, rilDataCall);
                        }
                    }
                    iComputeSerializedSize = iComputeMessageSize;
                }
                if (this.phoneState != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(8, this.phoneState);
                }
                if (this.callState != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(9, this.callState);
                }
                if (this.callIndex != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(10, this.callIndex);
                }
                if (this.mergedCallIndex != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(11, this.mergedCallIndex);
                }
                if (this.calls != null && this.calls.length > 0) {
                    for (int i2 = 0; i2 < this.calls.length; i2++) {
                        RilCall rilCall = this.calls[i2];
                        if (rilCall != null) {
                            iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(12, rilCall);
                        }
                    }
                }
                if (this.error != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(13, this.error);
                }
                if (this.rilRequest != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(14, this.rilRequest);
                }
                if (this.rilRequestId != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(15, this.rilRequestId);
                }
                if (this.srvccState != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(16, this.srvccState);
                }
                if (this.imsCommand != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(17, this.imsCommand);
                }
                if (this.reasonInfo != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(18, this.reasonInfo);
                }
                if (this.srcAccessTech != -1) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(19, this.srcAccessTech);
                }
                if (this.targetAccessTech != -1) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(20, this.targetAccessTech);
                }
                if (this.nitzTimestampMillis != 0) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt64Size(21, this.nitzTimestampMillis);
                }
                return iComputeSerializedSize;
            }

            @Override
            public Event mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                int length;
                int length2;
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            int position = codedInputByteBufferNano.getPosition();
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
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                case 20:
                                case 21:
                                    this.type = int32;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 16:
                            int position2 = codedInputByteBufferNano.getPosition();
                            int int322 = codedInputByteBufferNano.readInt32();
                            switch (int322) {
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
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                    this.delay = int322;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position2);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 26:
                            if (this.settings == null) {
                                this.settings = new TelephonySettings();
                            }
                            codedInputByteBufferNano.readMessage(this.settings);
                            break;
                        case 34:
                            if (this.serviceState == null) {
                                this.serviceState = new TelephonyServiceState();
                            }
                            codedInputByteBufferNano.readMessage(this.serviceState);
                            break;
                        case 42:
                            if (this.imsConnectionState == null) {
                                this.imsConnectionState = new ImsConnectionState();
                            }
                            codedInputByteBufferNano.readMessage(this.imsConnectionState);
                            break;
                        case 50:
                            if (this.imsCapabilities == null) {
                                this.imsCapabilities = new ImsCapabilities();
                            }
                            codedInputByteBufferNano.readMessage(this.imsCapabilities);
                            break;
                        case 58:
                            int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 58);
                            if (this.dataCalls != null) {
                                length = this.dataCalls.length;
                            } else {
                                length = 0;
                            }
                            RilDataCall[] rilDataCallArr = new RilDataCall[repeatedFieldArrayLength + length];
                            if (length != 0) {
                                System.arraycopy(this.dataCalls, 0, rilDataCallArr, 0, length);
                            }
                            while (length < rilDataCallArr.length - 1) {
                                rilDataCallArr[length] = new RilDataCall();
                                codedInputByteBufferNano.readMessage(rilDataCallArr[length]);
                                codedInputByteBufferNano.readTag();
                                length++;
                            }
                            rilDataCallArr[length] = new RilDataCall();
                            codedInputByteBufferNano.readMessage(rilDataCallArr[length]);
                            this.dataCalls = rilDataCallArr;
                            break;
                        case 64:
                            int position3 = codedInputByteBufferNano.getPosition();
                            int int323 = codedInputByteBufferNano.readInt32();
                            switch (int323) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                    this.phoneState = int323;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position3);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 72:
                            int position4 = codedInputByteBufferNano.getPosition();
                            int int324 = codedInputByteBufferNano.readInt32();
                            switch (int324) {
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
                                    this.callState = int324;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position4);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                            this.callIndex = codedInputByteBufferNano.readInt32();
                            break;
                        case 88:
                            this.mergedCallIndex = codedInputByteBufferNano.readInt32();
                            break;
                        case 98:
                            int repeatedFieldArrayLength2 = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 98);
                            if (this.calls != null) {
                                length2 = this.calls.length;
                            } else {
                                length2 = 0;
                            }
                            RilCall[] rilCallArr = new RilCall[repeatedFieldArrayLength2 + length2];
                            if (length2 != 0) {
                                System.arraycopy(this.calls, 0, rilCallArr, 0, length2);
                            }
                            while (length2 < rilCallArr.length - 1) {
                                rilCallArr[length2] = new RilCall();
                                codedInputByteBufferNano.readMessage(rilCallArr[length2]);
                                codedInputByteBufferNano.readTag();
                                length2++;
                            }
                            rilCallArr[length2] = new RilCall();
                            codedInputByteBufferNano.readMessage(rilCallArr[length2]);
                            this.calls = rilCallArr;
                            break;
                        case 104:
                            int position5 = codedInputByteBufferNano.getPosition();
                            int int325 = codedInputByteBufferNano.readInt32();
                            switch (int325) {
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
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                case 20:
                                case 21:
                                case 22:
                                case 23:
                                case 24:
                                case 25:
                                case 26:
                                case 27:
                                case 28:
                                    this.error = int325;
                                    break;
                                default:
                                    switch (int325) {
                                        case 36:
                                        case 37:
                                        case 38:
                                        case 39:
                                        case 40:
                                        case 41:
                                        case 42:
                                        case 43:
                                        case 44:
                                        case 45:
                                        case 46:
                                        case 47:
                                        case 48:
                                        case 49:
                                        case 50:
                                        case 51:
                                        case 52:
                                        case 53:
                                        case 54:
                                        case 55:
                                        case 56:
                                        case 57:
                                        case 58:
                                        case 59:
                                        case 60:
                                        case 61:
                                        case 62:
                                        case 63:
                                        case 64:
                                        case 65:
                                        case 66:
                                        case RilErrno.RIL_E_INVALID_RESPONSE:
                                            break;
                                        default:
                                            codedInputByteBufferNano.rewindToPosition(position5);
                                            storeUnknownField(codedInputByteBufferNano, tag);
                                            continue;
                                    }
                                    this.error = int325;
                                    break;
                            }
                            break;
                        case 112:
                            int position6 = codedInputByteBufferNano.getPosition();
                            int int326 = codedInputByteBufferNano.readInt32();
                            switch (int326) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                    this.rilRequest = int326;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position6);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 120:
                            this.rilRequestId = codedInputByteBufferNano.readInt32();
                            break;
                        case 128:
                            int position7 = codedInputByteBufferNano.getPosition();
                            int int327 = codedInputByteBufferNano.readInt32();
                            switch (int327) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                    this.srvccState = int327;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position7);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 136:
                            int position8 = codedInputByteBufferNano.getPosition();
                            int int328 = codedInputByteBufferNano.readInt32();
                            switch (int328) {
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
                                case 10:
                                case 11:
                                    this.imsCommand = int328;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position8);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 146:
                            if (this.reasonInfo == null) {
                                this.reasonInfo = new ImsReasonInfo();
                            }
                            codedInputByteBufferNano.readMessage(this.reasonInfo);
                            break;
                        case 152:
                            int position9 = codedInputByteBufferNano.getPosition();
                            int int329 = codedInputByteBufferNano.readInt32();
                            switch (int329) {
                                case -1:
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
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                    this.srcAccessTech = int329;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position9);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 160:
                            int position10 = codedInputByteBufferNano.getPosition();
                            int int3210 = codedInputByteBufferNano.readInt32();
                            switch (int3210) {
                                case -1:
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
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                    this.targetAccessTech = int3210;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position10);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case PduHeaders.ATTRIBUTES:
                            this.nitzTimestampMillis = codedInputByteBufferNano.readInt64();
                            break;
                        default:
                            if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                                return this;
                            }
                            break;
                            break;
                    }
                }
            }

            public static Event parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (Event) MessageNano.mergeFrom(new Event(), bArr);
            }

            public static Event parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new Event().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static TelephonyCallSession[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyCallSession[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyCallSession() {
            clear();
        }

        public TelephonyCallSession clear() {
            this.startTimeMinutes = 0;
            this.phoneId = 0;
            this.events = Event.emptyArray();
            this.eventsDropped = false;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.startTimeMinutes != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.startTimeMinutes);
            }
            if (this.phoneId != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.phoneId);
            }
            if (this.events != null && this.events.length > 0) {
                for (int i = 0; i < this.events.length; i++) {
                    Event event = this.events[i];
                    if (event != null) {
                        codedOutputByteBufferNano.writeMessage(3, event);
                    }
                }
            }
            if (this.eventsDropped) {
                codedOutputByteBufferNano.writeBool(4, this.eventsDropped);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.startTimeMinutes != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.startTimeMinutes);
            }
            if (this.phoneId != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.phoneId);
            }
            if (this.events != null && this.events.length > 0) {
                for (int i = 0; i < this.events.length; i++) {
                    Event event = this.events[i];
                    if (event != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(3, event);
                    }
                }
            }
            if (this.eventsDropped) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeBoolSize(4, this.eventsDropped);
            }
            return iComputeSerializedSize;
        }

        @Override
        public TelephonyCallSession mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.startTimeMinutes = codedInputByteBufferNano.readInt32();
                } else if (tag == 16) {
                    this.phoneId = codedInputByteBufferNano.readInt32();
                } else if (tag == 26) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 26);
                    if (this.events != null) {
                        length = this.events.length;
                    } else {
                        length = 0;
                    }
                    Event[] eventArr = new Event[repeatedFieldArrayLength + length];
                    if (length != 0) {
                        System.arraycopy(this.events, 0, eventArr, 0, length);
                    }
                    while (length < eventArr.length - 1) {
                        eventArr[length] = new Event();
                        codedInputByteBufferNano.readMessage(eventArr[length]);
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    eventArr[length] = new Event();
                    codedInputByteBufferNano.readMessage(eventArr[length]);
                    this.events = eventArr;
                } else if (tag != 32) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.eventsDropped = codedInputByteBufferNano.readBool();
                }
            }
        }

        public static TelephonyCallSession parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (TelephonyCallSession) MessageNano.mergeFrom(new TelephonyCallSession(), bArr);
        }

        public static TelephonyCallSession parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new TelephonyCallSession().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class SmsSession extends ExtendableMessageNano<SmsSession> {
        private static volatile SmsSession[] _emptyArray;
        public Event[] events;
        public boolean eventsDropped;
        public int phoneId;
        public int startTimeMinutes;

        public static final class Event extends ExtendableMessageNano<Event> {
            private static volatile Event[] _emptyArray;
            public CBMessage cellBroadcastMessage;
            public RilDataCall[] dataCalls;
            public int delay;
            public int error;
            public int errorCode;
            public int format;
            public ImsCapabilities imsCapabilities;
            public ImsConnectionState imsConnectionState;
            public int rilRequestId;
            public TelephonyServiceState serviceState;
            public TelephonySettings settings;
            public int tech;
            public int type;

            public interface CBMessageType {
                public static final int CMAS = 2;
                public static final int ETWS = 1;
                public static final int OTHER = 3;
                public static final int TYPE_UNKNOWN = 0;
            }

            public interface CBPriority {
                public static final int EMERGENCY = 4;
                public static final int INTERACTIVE = 2;
                public static final int NORMAL = 1;
                public static final int PRIORITY_UNKNOWN = 0;
                public static final int URGENT = 3;
            }

            public interface Format {
                public static final int SMS_FORMAT_3GPP = 1;
                public static final int SMS_FORMAT_3GPP2 = 2;
                public static final int SMS_FORMAT_UNKNOWN = 0;
            }

            public interface Tech {
                public static final int SMS_CDMA = 2;
                public static final int SMS_GSM = 1;
                public static final int SMS_IMS = 3;
                public static final int SMS_UNKNOWN = 0;
            }

            public interface Type {
                public static final int CB_SMS_RECEIVED = 9;
                public static final int DATA_CALL_LIST_CHANGED = 5;
                public static final int EVENT_UNKNOWN = 0;
                public static final int IMS_CAPABILITIES_CHANGED = 4;
                public static final int IMS_CONNECTION_STATE_CHANGED = 3;
                public static final int RIL_SERVICE_STATE_CHANGED = 2;
                public static final int SETTINGS_CHANGED = 1;
                public static final int SMS_RECEIVED = 8;
                public static final int SMS_SEND = 6;
                public static final int SMS_SEND_RESULT = 7;
            }

            public static final class CBMessage extends ExtendableMessageNano<CBMessage> {
                private static volatile CBMessage[] _emptyArray;
                public int msgFormat;
                public int msgPriority;
                public int msgType;
                public int serviceCategory;

                public static CBMessage[] emptyArray() {
                    if (_emptyArray == null) {
                        synchronized (InternalNano.LAZY_INIT_LOCK) {
                            if (_emptyArray == null) {
                                _emptyArray = new CBMessage[0];
                            }
                        }
                    }
                    return _emptyArray;
                }

                public CBMessage() {
                    clear();
                }

                public CBMessage clear() {
                    this.msgFormat = 0;
                    this.msgPriority = 0;
                    this.msgType = 0;
                    this.serviceCategory = 0;
                    this.unknownFieldData = null;
                    this.cachedSize = -1;
                    return this;
                }

                @Override
                public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                    if (this.msgFormat != 0) {
                        codedOutputByteBufferNano.writeInt32(1, this.msgFormat);
                    }
                    if (this.msgPriority != 0) {
                        codedOutputByteBufferNano.writeInt32(2, this.msgPriority);
                    }
                    if (this.msgType != 0) {
                        codedOutputByteBufferNano.writeInt32(3, this.msgType);
                    }
                    if (this.serviceCategory != 0) {
                        codedOutputByteBufferNano.writeInt32(4, this.serviceCategory);
                    }
                    super.writeTo(codedOutputByteBufferNano);
                }

                @Override
                protected int computeSerializedSize() {
                    int iComputeSerializedSize = super.computeSerializedSize();
                    if (this.msgFormat != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.msgFormat);
                    }
                    if (this.msgPriority != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.msgPriority);
                    }
                    if (this.msgType != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.msgType);
                    }
                    if (this.serviceCategory != 0) {
                        return iComputeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(4, this.serviceCategory);
                    }
                    return iComputeSerializedSize;
                }

                @Override
                public CBMessage mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                    while (true) {
                        int tag = codedInputByteBufferNano.readTag();
                        if (tag == 0) {
                            return this;
                        }
                        if (tag == 8) {
                            int position = codedInputByteBufferNano.getPosition();
                            int int32 = codedInputByteBufferNano.readInt32();
                            switch (int32) {
                                case 0:
                                case 1:
                                case 2:
                                    this.msgFormat = int32;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                        } else if (tag == 16) {
                            int position2 = codedInputByteBufferNano.getPosition();
                            int int322 = codedInputByteBufferNano.readInt32();
                            switch (int322) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                    this.msgPriority = int322;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position2);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                        } else if (tag == 24) {
                            int position3 = codedInputByteBufferNano.getPosition();
                            int int323 = codedInputByteBufferNano.readInt32();
                            switch (int323) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                    this.msgType = int323;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position3);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                        } else if (tag != 32) {
                            if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                                return this;
                            }
                        } else {
                            this.serviceCategory = codedInputByteBufferNano.readInt32();
                        }
                    }
                }

                public static CBMessage parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                    return (CBMessage) MessageNano.mergeFrom(new CBMessage(), bArr);
                }

                public static CBMessage parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                    return new CBMessage().mergeFrom(codedInputByteBufferNano);
                }
            }

            public static Event[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new Event[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public Event() {
                clear();
            }

            public Event clear() {
                this.type = 0;
                this.delay = 0;
                this.settings = null;
                this.serviceState = null;
                this.imsConnectionState = null;
                this.imsCapabilities = null;
                this.dataCalls = RilDataCall.emptyArray();
                this.format = 0;
                this.tech = 0;
                this.errorCode = 0;
                this.error = 0;
                this.rilRequestId = 0;
                this.cellBroadcastMessage = null;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if (this.type != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.type);
                }
                if (this.delay != 0) {
                    codedOutputByteBufferNano.writeInt32(2, this.delay);
                }
                if (this.settings != null) {
                    codedOutputByteBufferNano.writeMessage(3, this.settings);
                }
                if (this.serviceState != null) {
                    codedOutputByteBufferNano.writeMessage(4, this.serviceState);
                }
                if (this.imsConnectionState != null) {
                    codedOutputByteBufferNano.writeMessage(5, this.imsConnectionState);
                }
                if (this.imsCapabilities != null) {
                    codedOutputByteBufferNano.writeMessage(6, this.imsCapabilities);
                }
                if (this.dataCalls != null && this.dataCalls.length > 0) {
                    for (int i = 0; i < this.dataCalls.length; i++) {
                        RilDataCall rilDataCall = this.dataCalls[i];
                        if (rilDataCall != null) {
                            codedOutputByteBufferNano.writeMessage(7, rilDataCall);
                        }
                    }
                }
                if (this.format != 0) {
                    codedOutputByteBufferNano.writeInt32(8, this.format);
                }
                if (this.tech != 0) {
                    codedOutputByteBufferNano.writeInt32(9, this.tech);
                }
                if (this.errorCode != 0) {
                    codedOutputByteBufferNano.writeInt32(10, this.errorCode);
                }
                if (this.error != 0) {
                    codedOutputByteBufferNano.writeInt32(11, this.error);
                }
                if (this.rilRequestId != 0) {
                    codedOutputByteBufferNano.writeInt32(12, this.rilRequestId);
                }
                if (this.cellBroadcastMessage != null) {
                    codedOutputByteBufferNano.writeMessage(13, this.cellBroadcastMessage);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if (this.type != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.type);
                }
                if (this.delay != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.delay);
                }
                if (this.settings != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(3, this.settings);
                }
                if (this.serviceState != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(4, this.serviceState);
                }
                if (this.imsConnectionState != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(5, this.imsConnectionState);
                }
                if (this.imsCapabilities != null) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(6, this.imsCapabilities);
                }
                if (this.dataCalls != null && this.dataCalls.length > 0) {
                    for (int i = 0; i < this.dataCalls.length; i++) {
                        RilDataCall rilDataCall = this.dataCalls[i];
                        if (rilDataCall != null) {
                            iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(7, rilDataCall);
                        }
                    }
                }
                if (this.format != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(8, this.format);
                }
                if (this.tech != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(9, this.tech);
                }
                if (this.errorCode != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(10, this.errorCode);
                }
                if (this.error != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(11, this.error);
                }
                if (this.rilRequestId != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(12, this.rilRequestId);
                }
                if (this.cellBroadcastMessage != null) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(13, this.cellBroadcastMessage);
                }
                return iComputeSerializedSize;
            }

            @Override
            public Event mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                int length;
                while (true) {
                    int tag = codedInputByteBufferNano.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            int position = codedInputByteBufferNano.getPosition();
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
                                    this.type = int32;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 16:
                            int position2 = codedInputByteBufferNano.getPosition();
                            int int322 = codedInputByteBufferNano.readInt32();
                            switch (int322) {
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
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                    this.delay = int322;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position2);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 26:
                            if (this.settings == null) {
                                this.settings = new TelephonySettings();
                            }
                            codedInputByteBufferNano.readMessage(this.settings);
                            break;
                        case 34:
                            if (this.serviceState == null) {
                                this.serviceState = new TelephonyServiceState();
                            }
                            codedInputByteBufferNano.readMessage(this.serviceState);
                            break;
                        case 42:
                            if (this.imsConnectionState == null) {
                                this.imsConnectionState = new ImsConnectionState();
                            }
                            codedInputByteBufferNano.readMessage(this.imsConnectionState);
                            break;
                        case 50:
                            if (this.imsCapabilities == null) {
                                this.imsCapabilities = new ImsCapabilities();
                            }
                            codedInputByteBufferNano.readMessage(this.imsCapabilities);
                            break;
                        case 58:
                            int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 58);
                            if (this.dataCalls != null) {
                                length = this.dataCalls.length;
                            } else {
                                length = 0;
                            }
                            RilDataCall[] rilDataCallArr = new RilDataCall[repeatedFieldArrayLength + length];
                            if (length != 0) {
                                System.arraycopy(this.dataCalls, 0, rilDataCallArr, 0, length);
                            }
                            while (length < rilDataCallArr.length - 1) {
                                rilDataCallArr[length] = new RilDataCall();
                                codedInputByteBufferNano.readMessage(rilDataCallArr[length]);
                                codedInputByteBufferNano.readTag();
                                length++;
                            }
                            rilDataCallArr[length] = new RilDataCall();
                            codedInputByteBufferNano.readMessage(rilDataCallArr[length]);
                            this.dataCalls = rilDataCallArr;
                            break;
                        case 64:
                            int position3 = codedInputByteBufferNano.getPosition();
                            int int323 = codedInputByteBufferNano.readInt32();
                            switch (int323) {
                                case 0:
                                case 1:
                                case 2:
                                    this.format = int323;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position3);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case 72:
                            int position4 = codedInputByteBufferNano.getPosition();
                            int int324 = codedInputByteBufferNano.readInt32();
                            switch (int324) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                    this.tech = int324;
                                    break;
                                default:
                                    codedInputByteBufferNano.rewindToPosition(position4);
                                    storeUnknownField(codedInputByteBufferNano, tag);
                                    break;
                            }
                            break;
                        case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                            this.errorCode = codedInputByteBufferNano.readInt32();
                            break;
                        case 88:
                            int position5 = codedInputByteBufferNano.getPosition();
                            int int325 = codedInputByteBufferNano.readInt32();
                            switch (int325) {
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
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                case 20:
                                case 21:
                                case 22:
                                case 23:
                                case 24:
                                case 25:
                                case 26:
                                case 27:
                                case 28:
                                    this.error = int325;
                                    break;
                                default:
                                    switch (int325) {
                                        case 36:
                                        case 37:
                                        case 38:
                                        case 39:
                                        case 40:
                                        case 41:
                                        case 42:
                                        case 43:
                                        case 44:
                                        case 45:
                                        case 46:
                                        case 47:
                                        case 48:
                                        case 49:
                                        case 50:
                                        case 51:
                                        case 52:
                                        case 53:
                                        case 54:
                                        case 55:
                                        case 56:
                                        case 57:
                                        case 58:
                                        case 59:
                                        case 60:
                                        case 61:
                                        case 62:
                                        case 63:
                                        case 64:
                                        case 65:
                                        case 66:
                                        case RilErrno.RIL_E_INVALID_RESPONSE:
                                            break;
                                        default:
                                            codedInputByteBufferNano.rewindToPosition(position5);
                                            storeUnknownField(codedInputByteBufferNano, tag);
                                            continue;
                                    }
                                    this.error = int325;
                                    break;
                            }
                            break;
                        case 96:
                            this.rilRequestId = codedInputByteBufferNano.readInt32();
                            break;
                        case 106:
                            if (this.cellBroadcastMessage == null) {
                                this.cellBroadcastMessage = new CBMessage();
                            }
                            codedInputByteBufferNano.readMessage(this.cellBroadcastMessage);
                            break;
                        default:
                            if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                                return this;
                            }
                            break;
                            break;
                    }
                }
            }

            public static Event parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
                return (Event) MessageNano.mergeFrom(new Event(), bArr);
            }

            public static Event parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
                return new Event().mergeFrom(codedInputByteBufferNano);
            }
        }

        public static SmsSession[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new SmsSession[0];
                    }
                }
            }
            return _emptyArray;
        }

        public SmsSession() {
            clear();
        }

        public SmsSession clear() {
            this.startTimeMinutes = 0;
            this.phoneId = 0;
            this.events = Event.emptyArray();
            this.eventsDropped = false;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.startTimeMinutes != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.startTimeMinutes);
            }
            if (this.phoneId != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.phoneId);
            }
            if (this.events != null && this.events.length > 0) {
                for (int i = 0; i < this.events.length; i++) {
                    Event event = this.events[i];
                    if (event != null) {
                        codedOutputByteBufferNano.writeMessage(3, event);
                    }
                }
            }
            if (this.eventsDropped) {
                codedOutputByteBufferNano.writeBool(4, this.eventsDropped);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.startTimeMinutes != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.startTimeMinutes);
            }
            if (this.phoneId != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.phoneId);
            }
            if (this.events != null && this.events.length > 0) {
                for (int i = 0; i < this.events.length; i++) {
                    Event event = this.events[i];
                    if (event != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(3, event);
                    }
                }
            }
            if (this.eventsDropped) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeBoolSize(4, this.eventsDropped);
            }
            return iComputeSerializedSize;
        }

        @Override
        public SmsSession mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.startTimeMinutes = codedInputByteBufferNano.readInt32();
                } else if (tag == 16) {
                    this.phoneId = codedInputByteBufferNano.readInt32();
                } else if (tag == 26) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 26);
                    if (this.events != null) {
                        length = this.events.length;
                    } else {
                        length = 0;
                    }
                    Event[] eventArr = new Event[repeatedFieldArrayLength + length];
                    if (length != 0) {
                        System.arraycopy(this.events, 0, eventArr, 0, length);
                    }
                    while (length < eventArr.length - 1) {
                        eventArr[length] = new Event();
                        codedInputByteBufferNano.readMessage(eventArr[length]);
                        codedInputByteBufferNano.readTag();
                        length++;
                    }
                    eventArr[length] = new Event();
                    codedInputByteBufferNano.readMessage(eventArr[length]);
                    this.events = eventArr;
                } else if (tag != 32) {
                    if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.eventsDropped = codedInputByteBufferNano.readBool();
                }
            }
        }

        public static SmsSession parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (SmsSession) MessageNano.mergeFrom(new SmsSession(), bArr);
        }

        public static SmsSession parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new SmsSession().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class ModemPowerStats extends ExtendableMessageNano<ModemPowerStats> {
        private static volatile ModemPowerStats[] _emptyArray;
        public long cellularKernelActiveTimeMs;
        public double energyConsumedMah;
        public long idleTimeMs;
        public long loggingDurationMs;
        public long numPacketsTx;
        public long rxTimeMs;
        public long sleepTimeMs;
        public long timeInVeryPoorRxSignalLevelMs;
        public long[] txTimeMs;

        public static ModemPowerStats[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ModemPowerStats[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ModemPowerStats() {
            clear();
        }

        public ModemPowerStats clear() {
            this.loggingDurationMs = 0L;
            this.energyConsumedMah = 0.0d;
            this.numPacketsTx = 0L;
            this.cellularKernelActiveTimeMs = 0L;
            this.timeInVeryPoorRxSignalLevelMs = 0L;
            this.sleepTimeMs = 0L;
            this.idleTimeMs = 0L;
            this.rxTimeMs = 0L;
            this.txTimeMs = WireFormatNano.EMPTY_LONG_ARRAY;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.loggingDurationMs != 0) {
                codedOutputByteBufferNano.writeInt64(1, this.loggingDurationMs);
            }
            if (Double.doubleToLongBits(this.energyConsumedMah) != Double.doubleToLongBits(0.0d)) {
                codedOutputByteBufferNano.writeDouble(2, this.energyConsumedMah);
            }
            if (this.numPacketsTx != 0) {
                codedOutputByteBufferNano.writeInt64(3, this.numPacketsTx);
            }
            if (this.cellularKernelActiveTimeMs != 0) {
                codedOutputByteBufferNano.writeInt64(4, this.cellularKernelActiveTimeMs);
            }
            if (this.timeInVeryPoorRxSignalLevelMs != 0) {
                codedOutputByteBufferNano.writeInt64(5, this.timeInVeryPoorRxSignalLevelMs);
            }
            if (this.sleepTimeMs != 0) {
                codedOutputByteBufferNano.writeInt64(6, this.sleepTimeMs);
            }
            if (this.idleTimeMs != 0) {
                codedOutputByteBufferNano.writeInt64(7, this.idleTimeMs);
            }
            if (this.rxTimeMs != 0) {
                codedOutputByteBufferNano.writeInt64(8, this.rxTimeMs);
            }
            if (this.txTimeMs != null && this.txTimeMs.length > 0) {
                for (int i = 0; i < this.txTimeMs.length; i++) {
                    codedOutputByteBufferNano.writeInt64(9, this.txTimeMs[i]);
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.loggingDurationMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(1, this.loggingDurationMs);
            }
            if (Double.doubleToLongBits(this.energyConsumedMah) != Double.doubleToLongBits(0.0d)) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeDoubleSize(2, this.energyConsumedMah);
            }
            if (this.numPacketsTx != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(3, this.numPacketsTx);
            }
            if (this.cellularKernelActiveTimeMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(4, this.cellularKernelActiveTimeMs);
            }
            if (this.timeInVeryPoorRxSignalLevelMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(5, this.timeInVeryPoorRxSignalLevelMs);
            }
            if (this.sleepTimeMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(6, this.sleepTimeMs);
            }
            if (this.idleTimeMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(7, this.idleTimeMs);
            }
            if (this.rxTimeMs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt64Size(8, this.rxTimeMs);
            }
            if (this.txTimeMs != null && this.txTimeMs.length > 0) {
                int iComputeInt64SizeNoTag = 0;
                for (int i = 0; i < this.txTimeMs.length; i++) {
                    iComputeInt64SizeNoTag += CodedOutputByteBufferNano.computeInt64SizeNoTag(this.txTimeMs[i]);
                }
                return iComputeSerializedSize + iComputeInt64SizeNoTag + (1 * this.txTimeMs.length);
            }
            return iComputeSerializedSize;
        }

        @Override
        public ModemPowerStats mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.loggingDurationMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 17:
                        this.energyConsumedMah = codedInputByteBufferNano.readDouble();
                        break;
                    case 24:
                        this.numPacketsTx = codedInputByteBufferNano.readInt64();
                        break;
                    case 32:
                        this.cellularKernelActiveTimeMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 40:
                        this.timeInVeryPoorRxSignalLevelMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 48:
                        this.sleepTimeMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 56:
                        this.idleTimeMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 64:
                        this.rxTimeMs = codedInputByteBufferNano.readInt64();
                        break;
                    case 72:
                        int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 72);
                        if (this.txTimeMs != null) {
                            length = this.txTimeMs.length;
                        } else {
                            length = 0;
                        }
                        long[] jArr = new long[repeatedFieldArrayLength + length];
                        if (length != 0) {
                            System.arraycopy(this.txTimeMs, 0, jArr, 0, length);
                        }
                        while (length < jArr.length - 1) {
                            jArr[length] = codedInputByteBufferNano.readInt64();
                            codedInputByteBufferNano.readTag();
                            length++;
                        }
                        jArr[length] = codedInputByteBufferNano.readInt64();
                        this.txTimeMs = jArr;
                        break;
                    case 74:
                        int iPushLimit = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                        int position = codedInputByteBufferNano.getPosition();
                        int i = 0;
                        while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                            codedInputByteBufferNano.readInt64();
                            i++;
                        }
                        codedInputByteBufferNano.rewindToPosition(position);
                        if (this.txTimeMs != null) {
                            length2 = this.txTimeMs.length;
                        } else {
                            length2 = 0;
                        }
                        long[] jArr2 = new long[i + length2];
                        if (length2 != 0) {
                            System.arraycopy(this.txTimeMs, 0, jArr2, 0, length2);
                        }
                        while (length2 < jArr2.length) {
                            jArr2[length2] = codedInputByteBufferNano.readInt64();
                            length2++;
                        }
                        this.txTimeMs = jArr2;
                        codedInputByteBufferNano.popLimit(iPushLimit);
                        break;
                    default:
                        if (!storeUnknownField(codedInputByteBufferNano, tag)) {
                            return this;
                        }
                        break;
                        break;
                }
            }
        }

        public static ModemPowerStats parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (ModemPowerStats) MessageNano.mergeFrom(new ModemPowerStats(), bArr);
        }

        public static ModemPowerStats parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new ModemPowerStats().mergeFrom(codedInputByteBufferNano);
        }
    }
}
