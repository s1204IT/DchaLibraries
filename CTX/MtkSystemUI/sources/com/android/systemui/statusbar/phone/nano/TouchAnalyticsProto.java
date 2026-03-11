package com.android.systemui.statusbar.phone.nano;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InternalNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface TouchAnalyticsProto {

    public static final class Session extends MessageNano {
        private int bitField0_;
        private String build_;
        private long durationMillis_;
        public PhoneEvent[] phoneEvents;
        private int result_;
        public SensorEvent[] sensorEvents;
        private long startTimestampMillis_;
        private int touchAreaHeight_;
        private int touchAreaWidth_;
        public TouchEvent[] touchEvents;
        private int type_;

        public static final class TouchEvent extends MessageNano {
            private static volatile TouchEvent[] _emptyArray;
            private int actionIndex_;
            private int action_;
            private int bitField0_;
            public Pointer[] pointers;
            public BoundingBox removedBoundingBox;
            private boolean removedRedacted_;
            private long timeOffsetNanos_;

            public static final class BoundingBox extends MessageNano {
                private int bitField0_;
                private float height_;
                private float width_;

                @Override
                public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                    if ((this.bitField0_ & 1) != 0) {
                        codedOutputByteBufferNano.writeFloat(1, this.width_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        codedOutputByteBufferNano.writeFloat(2, this.height_);
                    }
                    super.writeTo(codedOutputByteBufferNano);
                }

                @Override
                protected int computeSerializedSize() {
                    int iComputeSerializedSize = super.computeSerializedSize();
                    if ((this.bitField0_ & 1) != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeFloatSize(1, this.width_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        return iComputeSerializedSize + CodedOutputByteBufferNano.computeFloatSize(2, this.height_);
                    }
                    return iComputeSerializedSize;
                }
            }

            public static final class Pointer extends MessageNano {
                private static volatile Pointer[] _emptyArray;
                private int bitField0_;
                private int id_;
                private float pressure_;
                public BoundingBox removedBoundingBox;
                private float removedLength_;
                private float size_;
                private float x_;
                private float y_;

                public static Pointer[] emptyArray() {
                    if (_emptyArray == null) {
                        synchronized (InternalNano.LAZY_INIT_LOCK) {
                            if (_emptyArray == null) {
                                _emptyArray = new Pointer[0];
                            }
                        }
                    }
                    return _emptyArray;
                }

                public Pointer setX(float f) {
                    this.x_ = f;
                    this.bitField0_ |= 1;
                    return this;
                }

                public Pointer setY(float f) {
                    this.y_ = f;
                    this.bitField0_ |= 2;
                    return this;
                }

                public Pointer setSize(float f) {
                    this.size_ = f;
                    this.bitField0_ |= 4;
                    return this;
                }

                public Pointer setPressure(float f) {
                    this.pressure_ = f;
                    this.bitField0_ |= 8;
                    return this;
                }

                public Pointer setId(int i) {
                    this.id_ = i;
                    this.bitField0_ |= 16;
                    return this;
                }

                public Pointer() {
                    clear();
                }

                public Pointer clear() {
                    this.bitField0_ = 0;
                    this.x_ = 0.0f;
                    this.y_ = 0.0f;
                    this.size_ = 0.0f;
                    this.pressure_ = 0.0f;
                    this.id_ = 0;
                    this.removedLength_ = 0.0f;
                    this.removedBoundingBox = null;
                    this.cachedSize = -1;
                    return this;
                }

                @Override
                public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                    if ((this.bitField0_ & 1) != 0) {
                        codedOutputByteBufferNano.writeFloat(1, this.x_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        codedOutputByteBufferNano.writeFloat(2, this.y_);
                    }
                    if ((this.bitField0_ & 4) != 0) {
                        codedOutputByteBufferNano.writeFloat(3, this.size_);
                    }
                    if ((this.bitField0_ & 8) != 0) {
                        codedOutputByteBufferNano.writeFloat(4, this.pressure_);
                    }
                    if ((this.bitField0_ & 16) != 0) {
                        codedOutputByteBufferNano.writeInt32(5, this.id_);
                    }
                    if ((this.bitField0_ & 32) != 0) {
                        codedOutputByteBufferNano.writeFloat(6, this.removedLength_);
                    }
                    if (this.removedBoundingBox != null) {
                        codedOutputByteBufferNano.writeMessage(7, this.removedBoundingBox);
                    }
                    super.writeTo(codedOutputByteBufferNano);
                }

                @Override
                protected int computeSerializedSize() {
                    int iComputeSerializedSize = super.computeSerializedSize();
                    if ((this.bitField0_ & 1) != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeFloatSize(1, this.x_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeFloatSize(2, this.y_);
                    }
                    if ((this.bitField0_ & 4) != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeFloatSize(3, this.size_);
                    }
                    if ((this.bitField0_ & 8) != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeFloatSize(4, this.pressure_);
                    }
                    if ((this.bitField0_ & 16) != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.id_);
                    }
                    if ((this.bitField0_ & 32) != 0) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeFloatSize(6, this.removedLength_);
                    }
                    if (this.removedBoundingBox != null) {
                        return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(7, this.removedBoundingBox);
                    }
                    return iComputeSerializedSize;
                }
            }

            public static TouchEvent[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new TouchEvent[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public TouchEvent setTimeOffsetNanos(long j) {
                this.timeOffsetNanos_ = j;
                this.bitField0_ |= 1;
                return this;
            }

            public TouchEvent setAction(int i) {
                this.action_ = i;
                this.bitField0_ |= 2;
                return this;
            }

            public TouchEvent setActionIndex(int i) {
                this.actionIndex_ = i;
                this.bitField0_ |= 4;
                return this;
            }

            public TouchEvent() {
                clear();
            }

            public TouchEvent clear() {
                this.bitField0_ = 0;
                this.timeOffsetNanos_ = 0L;
                this.action_ = 0;
                this.actionIndex_ = 0;
                this.pointers = Pointer.emptyArray();
                this.removedRedacted_ = false;
                this.removedBoundingBox = null;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if ((this.bitField0_ & 1) != 0) {
                    codedOutputByteBufferNano.writeUInt64(1, this.timeOffsetNanos_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    codedOutputByteBufferNano.writeInt32(2, this.action_);
                }
                if ((this.bitField0_ & 4) != 0) {
                    codedOutputByteBufferNano.writeInt32(3, this.actionIndex_);
                }
                if (this.pointers != null && this.pointers.length > 0) {
                    for (int i = 0; i < this.pointers.length; i++) {
                        Pointer pointer = this.pointers[i];
                        if (pointer != null) {
                            codedOutputByteBufferNano.writeMessage(4, pointer);
                        }
                    }
                }
                if ((this.bitField0_ & 8) != 0) {
                    codedOutputByteBufferNano.writeBool(5, this.removedRedacted_);
                }
                if (this.removedBoundingBox != null) {
                    codedOutputByteBufferNano.writeMessage(6, this.removedBoundingBox);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if ((this.bitField0_ & 1) != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeUInt64Size(1, this.timeOffsetNanos_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.action_);
                }
                if ((this.bitField0_ & 4) != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.actionIndex_);
                }
                if (this.pointers != null && this.pointers.length > 0) {
                    for (int i = 0; i < this.pointers.length; i++) {
                        Pointer pointer = this.pointers[i];
                        if (pointer != null) {
                            iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(4, pointer);
                        }
                    }
                }
                if ((this.bitField0_ & 8) != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(5, this.removedRedacted_);
                }
                if (this.removedBoundingBox != null) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeMessageSize(6, this.removedBoundingBox);
                }
                return iComputeSerializedSize;
            }
        }

        public static final class SensorEvent extends MessageNano {
            private static volatile SensorEvent[] _emptyArray;
            private int bitField0_;
            private long timeOffsetNanos_;
            private long timestamp_;
            private int type_;
            public float[] values;

            public static SensorEvent[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new SensorEvent[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public SensorEvent setType(int i) {
                this.type_ = i;
                this.bitField0_ |= 1;
                return this;
            }

            public SensorEvent setTimeOffsetNanos(long j) {
                this.timeOffsetNanos_ = j;
                this.bitField0_ |= 2;
                return this;
            }

            public SensorEvent setTimestamp(long j) {
                this.timestamp_ = j;
                this.bitField0_ |= 4;
                return this;
            }

            public SensorEvent() {
                clear();
            }

            public SensorEvent clear() {
                this.bitField0_ = 0;
                this.type_ = 1;
                this.timeOffsetNanos_ = 0L;
                this.values = WireFormatNano.EMPTY_FLOAT_ARRAY;
                this.timestamp_ = 0L;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if ((this.bitField0_ & 1) != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    codedOutputByteBufferNano.writeUInt64(2, this.timeOffsetNanos_);
                }
                if (this.values != null && this.values.length > 0) {
                    for (int i = 0; i < this.values.length; i++) {
                        codedOutputByteBufferNano.writeFloat(3, this.values[i]);
                    }
                }
                if ((this.bitField0_ & 4) != 0) {
                    codedOutputByteBufferNano.writeUInt64(4, this.timestamp_);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if ((this.bitField0_ & 1) != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeUInt64Size(2, this.timeOffsetNanos_);
                }
                if (this.values != null && this.values.length > 0) {
                    iComputeSerializedSize = iComputeSerializedSize + (this.values.length * 4) + (1 * this.values.length);
                }
                if ((this.bitField0_ & 4) != 0) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeUInt64Size(4, this.timestamp_);
                }
                return iComputeSerializedSize;
            }
        }

        public static final class PhoneEvent extends MessageNano {
            private static volatile PhoneEvent[] _emptyArray;
            private int bitField0_;
            private long timeOffsetNanos_;
            private int type_;

            public static PhoneEvent[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new PhoneEvent[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public PhoneEvent setType(int i) {
                this.type_ = i;
                this.bitField0_ |= 1;
                return this;
            }

            public PhoneEvent setTimeOffsetNanos(long j) {
                this.timeOffsetNanos_ = j;
                this.bitField0_ |= 2;
                return this;
            }

            public PhoneEvent() {
                clear();
            }

            public PhoneEvent clear() {
                this.bitField0_ = 0;
                this.type_ = 0;
                this.timeOffsetNanos_ = 0L;
                this.cachedSize = -1;
                return this;
            }

            @Override
            public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
                if ((this.bitField0_ & 1) != 0) {
                    codedOutputByteBufferNano.writeInt32(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    codedOutputByteBufferNano.writeUInt64(2, this.timeOffsetNanos_);
                }
                super.writeTo(codedOutputByteBufferNano);
            }

            @Override
            protected int computeSerializedSize() {
                int iComputeSerializedSize = super.computeSerializedSize();
                if ((this.bitField0_ & 1) != 0) {
                    iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    return iComputeSerializedSize + CodedOutputByteBufferNano.computeUInt64Size(2, this.timeOffsetNanos_);
                }
                return iComputeSerializedSize;
            }
        }

        public Session setStartTimestampMillis(long j) {
            this.startTimestampMillis_ = j;
            this.bitField0_ |= 1;
            return this;
        }

        public Session setDurationMillis(long j) {
            this.durationMillis_ = j;
            this.bitField0_ |= 2;
            return this;
        }

        public Session setBuild(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.build_ = str;
            this.bitField0_ |= 4;
            return this;
        }

        public Session setResult(int i) {
            this.result_ = i;
            this.bitField0_ |= 8;
            return this;
        }

        public Session setTouchAreaWidth(int i) {
            this.touchAreaWidth_ = i;
            this.bitField0_ |= 16;
            return this;
        }

        public Session setTouchAreaHeight(int i) {
            this.touchAreaHeight_ = i;
            this.bitField0_ |= 32;
            return this;
        }

        public Session setType(int i) {
            this.type_ = i;
            this.bitField0_ |= 64;
            return this;
        }

        public Session() {
            clear();
        }

        public Session clear() {
            this.bitField0_ = 0;
            this.startTimestampMillis_ = 0L;
            this.durationMillis_ = 0L;
            this.build_ = "";
            this.result_ = 0;
            this.touchEvents = TouchEvent.emptyArray();
            this.sensorEvents = SensorEvent.emptyArray();
            this.touchAreaWidth_ = 0;
            this.touchAreaHeight_ = 0;
            this.type_ = 0;
            this.phoneEvents = PhoneEvent.emptyArray();
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                codedOutputByteBufferNano.writeUInt64(1, this.startTimestampMillis_);
            }
            if ((this.bitField0_ & 2) != 0) {
                codedOutputByteBufferNano.writeUInt64(2, this.durationMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                codedOutputByteBufferNano.writeString(3, this.build_);
            }
            if ((this.bitField0_ & 8) != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.result_);
            }
            if (this.touchEvents != null && this.touchEvents.length > 0) {
                for (int i = 0; i < this.touchEvents.length; i++) {
                    TouchEvent touchEvent = this.touchEvents[i];
                    if (touchEvent != null) {
                        codedOutputByteBufferNano.writeMessage(5, touchEvent);
                    }
                }
            }
            if (this.sensorEvents != null && this.sensorEvents.length > 0) {
                for (int i2 = 0; i2 < this.sensorEvents.length; i2++) {
                    SensorEvent sensorEvent = this.sensorEvents[i2];
                    if (sensorEvent != null) {
                        codedOutputByteBufferNano.writeMessage(6, sensorEvent);
                    }
                }
            }
            if ((this.bitField0_ & 16) != 0) {
                codedOutputByteBufferNano.writeInt32(9, this.touchAreaWidth_);
            }
            if ((this.bitField0_ & 32) != 0) {
                codedOutputByteBufferNano.writeInt32(10, this.touchAreaHeight_);
            }
            if ((this.bitField0_ & 64) != 0) {
                codedOutputByteBufferNano.writeInt32(11, this.type_);
            }
            if (this.phoneEvents != null && this.phoneEvents.length > 0) {
                for (int i3 = 0; i3 < this.phoneEvents.length; i3++) {
                    PhoneEvent phoneEvent = this.phoneEvents[i3];
                    if (phoneEvent != null) {
                        codedOutputByteBufferNano.writeMessage(12, phoneEvent);
                    }
                }
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeUInt64Size(1, this.startTimestampMillis_);
            }
            if ((this.bitField0_ & 2) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeUInt64Size(2, this.durationMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeStringSize(3, this.build_);
            }
            if ((this.bitField0_ & 8) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.result_);
            }
            if (this.touchEvents != null && this.touchEvents.length > 0) {
                int iComputeMessageSize = iComputeSerializedSize;
                for (int i = 0; i < this.touchEvents.length; i++) {
                    TouchEvent touchEvent = this.touchEvents[i];
                    if (touchEvent != null) {
                        iComputeMessageSize += CodedOutputByteBufferNano.computeMessageSize(5, touchEvent);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize;
            }
            if (this.sensorEvents != null && this.sensorEvents.length > 0) {
                int iComputeMessageSize2 = iComputeSerializedSize;
                for (int i2 = 0; i2 < this.sensorEvents.length; i2++) {
                    SensorEvent sensorEvent = this.sensorEvents[i2];
                    if (sensorEvent != null) {
                        iComputeMessageSize2 += CodedOutputByteBufferNano.computeMessageSize(6, sensorEvent);
                    }
                }
                iComputeSerializedSize = iComputeMessageSize2;
            }
            if ((this.bitField0_ & 16) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(9, this.touchAreaWidth_);
            }
            if ((this.bitField0_ & 32) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(10, this.touchAreaHeight_);
            }
            if ((this.bitField0_ & 64) != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(11, this.type_);
            }
            if (this.phoneEvents != null && this.phoneEvents.length > 0) {
                for (int i3 = 0; i3 < this.phoneEvents.length; i3++) {
                    PhoneEvent phoneEvent = this.phoneEvents[i3];
                    if (phoneEvent != null) {
                        iComputeSerializedSize += CodedOutputByteBufferNano.computeMessageSize(12, phoneEvent);
                    }
                }
            }
            return iComputeSerializedSize;
        }
    }
}
