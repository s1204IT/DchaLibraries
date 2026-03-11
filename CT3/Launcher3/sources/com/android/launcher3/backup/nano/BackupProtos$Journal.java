package com.android.launcher3.backup.nano;

import com.android.launcher3.compat.PackageInstallerCompat;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public final class BackupProtos$Journal extends MessageNano {
    public int appVersion;
    public int backupVersion;
    public long bytes;
    public BackupProtos$Key[] key;
    public BackupProtos$DeviceProfieData profile;
    public int rows;
    public long t;

    public BackupProtos$Journal() {
        clear();
    }

    public BackupProtos$Journal clear() {
        this.appVersion = 0;
        this.t = 0L;
        this.bytes = 0L;
        this.rows = 0;
        this.key = BackupProtos$Key.emptyArray();
        this.backupVersion = 1;
        this.profile = null;
        this.cachedSize = -1;
        return this;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeInt32(1, this.appVersion);
        output.writeInt64(2, this.t);
        if (this.bytes != 0) {
            output.writeInt64(3, this.bytes);
        }
        if (this.rows != 0) {
            output.writeInt32(4, this.rows);
        }
        if (this.key != null && this.key.length > 0) {
            for (int i = 0; i < this.key.length; i++) {
                BackupProtos$Key element = this.key[i];
                if (element != null) {
                    output.writeMessage(5, element);
                }
            }
        }
        if (this.backupVersion != 1) {
            output.writeInt32(6, this.backupVersion);
        }
        if (this.profile != null) {
            output.writeMessage(7, this.profile);
        }
        super.writeTo(output);
    }

    @Override
    protected int computeSerializedSize() {
        int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt32Size(1, this.appVersion) + CodedOutputByteBufferNano.computeInt64Size(2, this.t);
        if (this.bytes != 0) {
            size += CodedOutputByteBufferNano.computeInt64Size(3, this.bytes);
        }
        if (this.rows != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(4, this.rows);
        }
        if (this.key != null && this.key.length > 0) {
            for (int i = 0; i < this.key.length; i++) {
                BackupProtos$Key element = this.key[i];
                if (element != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(5, element);
                }
            }
        }
        if (this.backupVersion != 1) {
            size += CodedOutputByteBufferNano.computeInt32Size(6, this.backupVersion);
        }
        if (this.profile != null) {
            return size + CodedOutputByteBufferNano.computeMessageSize(7, this.profile);
        }
        return size;
    }

    @Override
    public BackupProtos$Journal mergeFrom(CodedInputByteBufferNano input) throws IOException {
        while (true) {
            int tag = input.readTag();
            switch (tag) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    return this;
                case 8:
                    this.appVersion = input.readInt32();
                    break;
                case 16:
                    this.t = input.readInt64();
                    break;
                case 24:
                    this.bytes = input.readInt64();
                    break;
                case 32:
                    this.rows = input.readInt32();
                    break;
                case 42:
                    int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 42);
                    int i = this.key == null ? 0 : this.key.length;
                    BackupProtos$Key[] newArray = new BackupProtos$Key[i + arrayLength];
                    if (i != 0) {
                        System.arraycopy(this.key, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = new BackupProtos$Key();
                        input.readMessage(newArray[i]);
                        input.readTag();
                        i++;
                    }
                    newArray[i] = new BackupProtos$Key();
                    input.readMessage(newArray[i]);
                    this.key = newArray;
                    break;
                case 48:
                    this.backupVersion = input.readInt32();
                    break;
                case 58:
                    if (this.profile == null) {
                        this.profile = new BackupProtos$DeviceProfieData();
                    }
                    input.readMessage(this.profile);
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
