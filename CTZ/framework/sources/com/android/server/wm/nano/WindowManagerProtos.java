package com.android.server.wm.nano;

import com.android.framework.protobuf.nano.CodedInputByteBufferNano;
import com.android.framework.protobuf.nano.CodedOutputByteBufferNano;
import com.android.framework.protobuf.nano.InternalNano;
import com.android.framework.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.framework.protobuf.nano.MessageNano;
import com.android.framework.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface WindowManagerProtos {

    public static final class TaskSnapshotProto extends MessageNano {
        private static volatile TaskSnapshotProto[] _emptyArray;
        public int insetBottom;
        public int insetLeft;
        public int insetRight;
        public int insetTop;
        public boolean isRealSnapshot;
        public boolean isTranslucent;
        public int orientation;
        public int systemUiVisibility;
        public int windowingMode;

        public static TaskSnapshotProto[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TaskSnapshotProto[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TaskSnapshotProto() {
            clear();
        }

        public TaskSnapshotProto clear() {
            this.orientation = 0;
            this.insetLeft = 0;
            this.insetTop = 0;
            this.insetRight = 0;
            this.insetBottom = 0;
            this.isRealSnapshot = false;
            this.windowingMode = 0;
            this.systemUiVisibility = 0;
            this.isTranslucent = false;
            this.cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
            if (this.orientation != 0) {
                codedOutputByteBufferNano.writeInt32(1, this.orientation);
            }
            if (this.insetLeft != 0) {
                codedOutputByteBufferNano.writeInt32(2, this.insetLeft);
            }
            if (this.insetTop != 0) {
                codedOutputByteBufferNano.writeInt32(3, this.insetTop);
            }
            if (this.insetRight != 0) {
                codedOutputByteBufferNano.writeInt32(4, this.insetRight);
            }
            if (this.insetBottom != 0) {
                codedOutputByteBufferNano.writeInt32(5, this.insetBottom);
            }
            if (this.isRealSnapshot) {
                codedOutputByteBufferNano.writeBool(6, this.isRealSnapshot);
            }
            if (this.windowingMode != 0) {
                codedOutputByteBufferNano.writeInt32(7, this.windowingMode);
            }
            if (this.systemUiVisibility != 0) {
                codedOutputByteBufferNano.writeInt32(8, this.systemUiVisibility);
            }
            if (this.isTranslucent) {
                codedOutputByteBufferNano.writeBool(9, this.isTranslucent);
            }
            super.writeTo(codedOutputByteBufferNano);
        }

        @Override
        protected int computeSerializedSize() {
            int iComputeSerializedSize = super.computeSerializedSize();
            if (this.orientation != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, this.orientation);
            }
            if (this.insetLeft != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(2, this.insetLeft);
            }
            if (this.insetTop != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(3, this.insetTop);
            }
            if (this.insetRight != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(4, this.insetRight);
            }
            if (this.insetBottom != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(5, this.insetBottom);
            }
            if (this.isRealSnapshot) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(6, this.isRealSnapshot);
            }
            if (this.windowingMode != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(7, this.windowingMode);
            }
            if (this.systemUiVisibility != 0) {
                iComputeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(8, this.systemUiVisibility);
            }
            if (this.isTranslucent) {
                return iComputeSerializedSize + CodedOutputByteBufferNano.computeBoolSize(9, this.isTranslucent);
            }
            return iComputeSerializedSize;
        }

        @Override
        public TaskSnapshotProto mergeFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            while (true) {
                int tag = codedInputByteBufferNano.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 8) {
                    this.orientation = codedInputByteBufferNano.readInt32();
                } else if (tag == 16) {
                    this.insetLeft = codedInputByteBufferNano.readInt32();
                } else if (tag == 24) {
                    this.insetTop = codedInputByteBufferNano.readInt32();
                } else if (tag == 32) {
                    this.insetRight = codedInputByteBufferNano.readInt32();
                } else if (tag == 40) {
                    this.insetBottom = codedInputByteBufferNano.readInt32();
                } else if (tag == 48) {
                    this.isRealSnapshot = codedInputByteBufferNano.readBool();
                } else if (tag == 56) {
                    this.windowingMode = codedInputByteBufferNano.readInt32();
                } else if (tag == 64) {
                    this.systemUiVisibility = codedInputByteBufferNano.readInt32();
                } else if (tag != 72) {
                    if (!WireFormatNano.parseUnknownField(codedInputByteBufferNano, tag)) {
                        return this;
                    }
                } else {
                    this.isTranslucent = codedInputByteBufferNano.readBool();
                }
            }
        }

        public static TaskSnapshotProto parseFrom(byte[] bArr) throws InvalidProtocolBufferNanoException {
            return (TaskSnapshotProto) MessageNano.mergeFrom(new TaskSnapshotProto(), bArr);
        }

        public static TaskSnapshotProto parseFrom(CodedInputByteBufferNano codedInputByteBufferNano) throws IOException {
            return new TaskSnapshotProto().mergeFrom(codedInputByteBufferNano);
        }
    }
}
