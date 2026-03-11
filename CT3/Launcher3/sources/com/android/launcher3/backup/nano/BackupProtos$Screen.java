package com.android.launcher3.backup.nano;

import com.android.launcher3.compat.PackageInstallerCompat;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public final class BackupProtos$Screen extends MessageNano {
    public long id;
    public int rank;

    public BackupProtos$Screen() {
        clear();
    }

    public BackupProtos$Screen clear() {
        this.id = 0L;
        this.rank = 0;
        this.cachedSize = -1;
        return this;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeInt64(1, this.id);
        if (this.rank != 0) {
            output.writeInt32(2, this.rank);
        }
        super.writeTo(output);
    }

    @Override
    protected int computeSerializedSize() {
        int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt64Size(1, this.id);
        if (this.rank != 0) {
            return size + CodedOutputByteBufferNano.computeInt32Size(2, this.rank);
        }
        return size;
    }

    @Override
    public BackupProtos$Screen mergeFrom(CodedInputByteBufferNano input) throws IOException {
        while (true) {
            int tag = input.readTag();
            switch (tag) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    return this;
                case 8:
                    this.id = input.readInt64();
                    break;
                case 16:
                    this.rank = input.readInt32();
                    break;
                default:
                    if (!WireFormatNano.parseUnknownField(input, tag)) {
                        return this;
                    }
                    break;
                    break;
            }
        }
    }
}
