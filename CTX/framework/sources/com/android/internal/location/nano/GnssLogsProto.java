package com.android.internal.location.nano;

import com.android.framework.protobuf.nano.CodedInputByteBufferNano;
import com.android.framework.protobuf.nano.CodedOutputByteBufferNano;
import com.android.framework.protobuf.nano.InternalNano;
import com.android.framework.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.framework.protobuf.nano.MessageNano;
import com.android.framework.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface GnssLogsProto {

    public static final class GnssLog extends MessageNano {
        private static volatile GnssLog[] _emptyArray;
        public int meanPositionAccuracyMeters;
        public int meanTimeToFirstFixSecs;
        public double meanTopFourAverageCn0DbHz;
        public int numLocationReportProcessed;
        public int numPositionAccuracyProcessed;
        public int numTimeToFirstFixProcessed;
        public int numTopFourAverageCn0Processed;
        public int percentageLocationFailure;
        public PowerMetrics powerMetrics;
        public int standardDeviationPositionAccuracyMeters;
        public int standardDeviationTimeToFirstFixSecs;
        public double standardDeviationTopFourAverageCn0DbHz;

        public static GnssLog[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new GnssLog[0];
                    }
                }
            }
            return _emptyArray;
        }

        public GnssLog() {
            clear();
        }

        public GnssLog clear() {
            this.numLocationReportProcessed = 0;
            this.percentageLocationFailure = 0;
            this.numTimeToFirstFixProcessed = 0;
            this.meanTimeToFirstFixSecs = 0;
            this.standardDeviationTimeToFirstFixSecs = 0;
            this.numPositionAccuracyProcessed = 0;
            this.meanPositionAccuracyMeters = 0;
            this.standardDeviationPositionAccuracyMeters = 0;
            this.numTopFourAverageCn0Processed = 0;
            this.meanTopFourAverageCn0DbHz = 0.0d;
            this.standardDeviationTopFourAverageCn0DbHz = 0.0d;
            this.powerMetrics = null;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.numLocationReportProcessed != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.numLocationReportProcessed);
            }
            if (this.percentageLocationFailure != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.percentageLocationFailure);
            }
            if (this.numTimeToFirstFixProcessed != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.numTimeToFirstFixProcessed);
            }
            if (this.meanTimeToFirstFixSecs != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.meanTimeToFirstFixSecs);
            }
            if (this.standardDeviationTimeToFirstFixSecs != 0) {
                codedOutputByteBufferNano.writeInt32(5, this.standardDeviationTimeToFirstFixSecs);
            }
            if (this.numPositionAccuracyProcessed != 0) {
                codedOutputByteBufferNano.writeInt32(6, this.numPositionAccuracyProcessed);
            }
            if (this.meanPositionAccuracyMeters != 0) {
                codedOutputByteBufferNano.writeInt32(7, this.meanPositionAccuracyMeters);
            }
            if (this.standardDeviationPositionAccuracyMeters != 0) {
                codedOutputByteBufferNano.writeInt32(8, this.standardDeviationPositionAccuracyMeters);
            }
            if (this.numTopFourAverageCn0Processed != 0) {
                codedOutputByteBufferNano.writeInt32(9, this.numTopFourAverageCn0Processed);
            }
            if (Double.doubleToLongBits(this.meanTopFourAverageCn0DbHz) != Double.doubleToLongBits(0.0d)) {
                codedOutputByteBufferNano.writeDouble(10, this.meanTopFourAverageCn0DbHz);
            }
            if (Double.doubleToLongBits(this.standardDeviationTopFourAverageCn0DbHz) != Double.doubleToLongBits(0.0d)) {
                codedOutputByteBufferNano.writeDouble(11, this.standardDeviationTopFourAverageCn0DbHz);
            }
            if (this.powerMetrics != null) {
                codedOutputByteBufferNano.writeMessage(12, this.powerMetrics);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.numLocationReportProcessed != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.numLocationReportProcessed);
            }
            if (this.percentageLocationFailure != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.percentageLocationFailure);
            }
            if (this.numTimeToFirstFixProcessed != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.numTimeToFirstFixProcessed);
            }
            if (this.meanTimeToFirstFixSecs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.meanTimeToFirstFixSecs);
            }
            if (this.standardDeviationTimeToFirstFixSecs != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.standardDeviationTimeToFirstFixSecs);
            }
            if (this.numPositionAccuracyProcessed != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(6, this.numPositionAccuracyProcessed);
            }
            if (this.meanPositionAccuracyMeters != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(7, this.meanPositionAccuracyMeters);
            }
            if (this.standardDeviationPositionAccuracyMeters != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(8, this.standardDeviationPositionAccuracyMeters);
            }
            if (this.numTopFourAverageCn0Processed != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(9, this.numTopFourAverageCn0Processed);
            }
            if (Double.doubleToLongBits(this.meanTopFourAverageCn0DbHz) != Double.doubleToLongBits(0.0d)) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeDoubleSize(10, this.meanTopFourAverageCn0DbHz);
            }
            if (Double.doubleToLongBits(this.standardDeviationTopFourAverageCn0DbHz) != Double.doubleToLongBits(0.0d)) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeDoubleSize(11, this.standardDeviationTopFourAverageCn0DbHz);
            }
            if (this.powerMetrics != null) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(12, this.powerMetrics);
            }
            return iComputeSerializedSize;
        }

        @Override
        public GnssLog mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.numLocationReportProcessed = codedInputByteBufferNano.readInt32();
                        break;
                    case 16:
                        this.percentageLocationFailure = codedInputByteBufferNano.readInt32();
                        break;
                    case 24:
                        this.numTimeToFirstFixProcessed = codedInputByteBufferNano.readInt32();
                        break;
                    case 32:
                        this.meanTimeToFirstFixSecs = codedInputByteBufferNano.readInt32();
                        break;
                    case 40:
                        this.standardDeviationTimeToFirstFixSecs = codedInputByteBufferNano.readInt32();
                        break;
                    case 48:
                        this.numPositionAccuracyProcessed = codedInputByteBufferNano.readInt32();
                        break;
                    case 56:
                        this.meanPositionAccuracyMeters = codedInputByteBufferNano.readInt32();
                        break;
                    case 64:
                        this.standardDeviationPositionAccuracyMeters = codedInputByteBufferNano.readInt32();
                        break;
                    case 72:
                        this.numTopFourAverageCn0Processed = codedInputByteBufferNano.readInt32();
                        break;
                    case 81:
                        this.meanTopFourAverageCn0DbHz = codedInputByteBufferNano.readDouble();
                        break;
                    case 89:
                        this.standardDeviationTopFourAverageCn0DbHz = codedInputByteBufferNano.readDouble();
                        break;
                    case 98:
                        if (this.powerMetrics == null) {
                            this.powerMetrics = new PowerMetrics();
                        }
                        codedInputByteBufferNano.readMessage(this.powerMetrics);
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

        public static GnssLog parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (GnssLog) MessageNano.mergeFrom(new GnssLog(), bArr);
        }

        public static GnssLog parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new GnssLog().mergeFrom(codedInputByteBufferNano);
        }
    }

    public static final class PowerMetrics extends MessageNano {
        private static volatile PowerMetrics[] _emptyArray;
        public double energyConsumedMah;
        public long loggingDurationMs;
        public long[] timeInSignalQualityLevelMs;

        public static PowerMetrics[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new PowerMetrics[0];
                    }
                }
            }
            return _emptyArray;
        }

        public PowerMetrics() {
            clear();
        }

        public PowerMetrics clear() {
            this.loggingDurationMs = 0L;
            this.energyConsumedMah = 0.0d;
            this.timeInSignalQualityLevelMs = WireFormatNano.EMPTY_LONG_ARRAY;
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
            if (this.timeInSignalQualityLevelMs != null && this.timeInSignalQualityLevelMs.length > 0) {
                for (int i = 0; i < this.timeInSignalQualityLevelMs.length; i++) {
                    codedOutputByteBufferNano.writeInt64(3, this.timeInSignalQualityLevelMs[i]);
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
            if (this.timeInSignalQualityLevelMs != null && this.timeInSignalQualityLevelMs.length > 0) {
                int iComputeInt64SizeNoTag = 0;
                for (int i = 0; i < this.timeInSignalQualityLevelMs.length; i++) {
                    iComputeInt64SizeNoTag += CodedOutputByteBufferNano.computeInt64SizeNoTag(this.timeInSignalQualityLevelMs[i]);
                }
                return iComputeSerializedSize + iComputeInt64SizeNoTag + (1 * this.timeInSignalQualityLevelMs.length);
            }
            return iComputeSerializedSize;
        }

        @Override
        public PowerMetrics mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            int length;
            int length2;
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.loggingDurationMs = codedInputByteBufferNano.readInt64();
                } else if (tag == 17) {
                    this.energyConsumedMah = codedInputByteBufferNano.readDouble();
                } else if (tag == 24) {
                    int repeatedFieldArrayLength = WireFormatNano.getRepeatedFieldArrayLength(codedInputByteBufferNano, 24);
                    if (this.timeInSignalQualityLevelMs != null) {
                        length2 = this.timeInSignalQualityLevelMs.length;
                    } else {
                        length2 = 0;
                    }
                    long[] jArr = new long[repeatedFieldArrayLength + length2];
                    if (length2 != 0) {
                        System.arraycopy(this.timeInSignalQualityLevelMs, 0, jArr, 0, length2);
                    }
                    while (length2 < jArr.length - 1) {
                        jArr[length2] = codedInputByteBufferNano.readInt64();
                        codedInputByteBufferNano.readTag();
                        length2++;
                    }
                    jArr[length2] = codedInputByteBufferNano.readInt64();
                    this.timeInSignalQualityLevelMs = jArr;
                } else if (tag != 26) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    int iPushLimit = codedInputByteBufferNano.pushLimit(codedInputByteBufferNano.readRawVarint32());
                    int position = codedInputByteBufferNano.getPosition();
                    int i = 0;
                    while (codedInputByteBufferNano.getBytesUntilLimit() > 0) {
                        codedInputByteBufferNano.readInt64();
                        i++;
                    }
                    codedInputByteBufferNano.rewindToPosition(position);
                    if (this.timeInSignalQualityLevelMs != null) {
                        length = this.timeInSignalQualityLevelMs.length;
                    } else {
                        length = 0;
                    }
                    long[] jArr2 = new long[i + length];
                    if (length != 0) {
                        System.arraycopy(this.timeInSignalQualityLevelMs, 0, jArr2, 0, length);
                    }
                    while (length < jArr2.length) {
                        jArr2[length] = codedInputByteBufferNano.readInt64();
                        length++;
                    }
                    this.timeInSignalQualityLevelMs = jArr2;
                    codedInputByteBufferNano.popLimit(iPushLimit);
                }
            }
        }

        public static PowerMetrics parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (PowerMetrics) MessageNano.mergeFrom(new PowerMetrics(), bArr);
        }

        public static PowerMetrics parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new PowerMetrics().mergeFrom(codedInputByteBufferNano);
        }
    }
}
