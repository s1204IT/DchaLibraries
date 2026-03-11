package com.android.launcher3.backup.nano;

import com.android.launcher3.compat.PackageInstallerCompat;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public final class BackupProtos$DeviceProfieData extends MessageNano {
    public int allappsRank;
    public float desktopCols;
    public float desktopRows;
    public float hotseatCount;

    public BackupProtos$DeviceProfieData() {
        clear();
    }

    public BackupProtos$DeviceProfieData clear() {
        this.desktopRows = 0.0f;
        this.desktopCols = 0.0f;
        this.hotseatCount = 0.0f;
        this.allappsRank = 0;
        this.cachedSize = -1;
        return this;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeFloat(1, this.desktopRows);
        output.writeFloat(2, this.desktopCols);
        output.writeFloat(3, this.hotseatCount);
        output.writeInt32(4, this.allappsRank);
        super.writeTo(output);
    }

    @Override
    protected int computeSerializedSize() {
        int size = super.computeSerializedSize();
        return size + CodedOutputByteBufferNano.computeFloatSize(1, this.desktopRows) + CodedOutputByteBufferNano.computeFloatSize(2, this.desktopCols) + CodedOutputByteBufferNano.computeFloatSize(3, this.hotseatCount) + CodedOutputByteBufferNano.computeInt32Size(4, this.allappsRank);
    }

    @Override
    public BackupProtos$DeviceProfieData mergeFrom(CodedInputByteBufferNano input) throws IOException {
        while (true) {
            int tag = input.readTag();
            switch (tag) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    return this;
                case 13:
                    this.desktopRows = input.readFloat();
                    break;
                case 21:
                    this.desktopCols = input.readFloat();
                    break;
                case 29:
                    this.hotseatCount = input.readFloat();
                    break;
                case 32:
                    this.allappsRank = input.readInt32();
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
