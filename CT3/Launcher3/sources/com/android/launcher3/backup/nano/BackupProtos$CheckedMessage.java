package com.android.launcher3.backup.nano;

import com.android.launcher3.compat.PackageInstallerCompat;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public final class BackupProtos$CheckedMessage extends MessageNano {
    public long checksum;
    public byte[] payload;

    public BackupProtos$CheckedMessage() {
        clear();
    }

    public BackupProtos$CheckedMessage clear() {
        this.payload = WireFormatNano.EMPTY_BYTES;
        this.checksum = 0L;
        this.cachedSize = -1;
        return this;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeBytes(1, this.payload);
        output.writeInt64(2, this.checksum);
        super.writeTo(output);
    }

    @Override
    protected int computeSerializedSize() {
        int size = super.computeSerializedSize();
        return size + CodedOutputByteBufferNano.computeBytesSize(1, this.payload) + CodedOutputByteBufferNano.computeInt64Size(2, this.checksum);
    }

    @Override
    public BackupProtos$CheckedMessage mergeFrom(CodedInputByteBufferNano input) throws IOException {
        while (true) {
            int tag = input.readTag();
            switch (tag) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    return this;
                case 10:
                    this.payload = input.readBytes();
                    break;
                case 16:
                    this.checksum = input.readInt64();
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
