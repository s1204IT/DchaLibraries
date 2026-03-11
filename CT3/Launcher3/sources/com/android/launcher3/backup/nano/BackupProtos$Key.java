package com.android.launcher3.backup.nano;

import com.android.launcher3.compat.PackageInstallerCompat;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InternalNano;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public final class BackupProtos$Key extends MessageNano {
    private static volatile BackupProtos$Key[] _emptyArray;
    public long checksum;
    public long id;
    public String name;
    public int type;

    public static BackupProtos$Key[] emptyArray() {
        if (_emptyArray == null) {
            synchronized (InternalNano.LAZY_INIT_LOCK) {
                if (_emptyArray == null) {
                    _emptyArray = new BackupProtos$Key[0];
                }
            }
        }
        return _emptyArray;
    }

    public BackupProtos$Key() {
        clear();
    }

    public BackupProtos$Key clear() {
        this.type = 1;
        this.name = "";
        this.id = 0L;
        this.checksum = 0L;
        this.cachedSize = -1;
        return this;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeInt32(1, this.type);
        if (!this.name.equals("")) {
            output.writeString(2, this.name);
        }
        if (this.id != 0) {
            output.writeInt64(3, this.id);
        }
        if (this.checksum != 0) {
            output.writeInt64(4, this.checksum);
        }
        super.writeTo(output);
    }

    @Override
    protected int computeSerializedSize() {
        int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt32Size(1, this.type);
        if (!this.name.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(2, this.name);
        }
        if (this.id != 0) {
            size += CodedOutputByteBufferNano.computeInt64Size(3, this.id);
        }
        if (this.checksum != 0) {
            return size + CodedOutputByteBufferNano.computeInt64Size(4, this.checksum);
        }
        return size;
    }

    @Override
    public BackupProtos$Key mergeFrom(CodedInputByteBufferNano input) throws IOException {
        while (true) {
            int tag = input.readTag();
            switch (tag) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    return this;
                case 8:
                    int value = input.readInt32();
                    switch (value) {
                        case PackageInstallerCompat.STATUS_INSTALLING:
                        case PackageInstallerCompat.STATUS_FAILED:
                        case 3:
                        case 4:
                            this.type = value;
                            break;
                    }
                    break;
                case 18:
                    this.name = input.readString();
                    break;
                case 24:
                    this.id = input.readInt64();
                    break;
                case 32:
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

    public static BackupProtos$Key parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
        return (BackupProtos$Key) MessageNano.mergeFrom(new BackupProtos$Key(), data);
    }
}
