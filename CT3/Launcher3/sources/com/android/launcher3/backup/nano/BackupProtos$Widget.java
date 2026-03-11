package com.android.launcher3.backup.nano;

import com.android.launcher3.compat.PackageInstallerCompat;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public final class BackupProtos$Widget extends MessageNano {
    public boolean configure;
    public BackupProtos$Resource icon;
    public String label;
    public int minSpanX;
    public int minSpanY;
    public BackupProtos$Resource preview;
    public String provider;

    public BackupProtos$Widget() {
        clear();
    }

    public BackupProtos$Widget clear() {
        this.provider = "";
        this.label = "";
        this.configure = false;
        this.icon = null;
        this.preview = null;
        this.minSpanX = 2;
        this.minSpanY = 2;
        this.cachedSize = -1;
        return this;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeString(1, this.provider);
        if (!this.label.equals("")) {
            output.writeString(2, this.label);
        }
        if (this.configure) {
            output.writeBool(3, this.configure);
        }
        if (this.icon != null) {
            output.writeMessage(4, this.icon);
        }
        if (this.preview != null) {
            output.writeMessage(5, this.preview);
        }
        if (this.minSpanX != 2) {
            output.writeInt32(6, this.minSpanX);
        }
        if (this.minSpanY != 2) {
            output.writeInt32(7, this.minSpanY);
        }
        super.writeTo(output);
    }

    @Override
    protected int computeSerializedSize() {
        int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeStringSize(1, this.provider);
        if (!this.label.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(2, this.label);
        }
        if (this.configure) {
            size += CodedOutputByteBufferNano.computeBoolSize(3, this.configure);
        }
        if (this.icon != null) {
            size += CodedOutputByteBufferNano.computeMessageSize(4, this.icon);
        }
        if (this.preview != null) {
            size += CodedOutputByteBufferNano.computeMessageSize(5, this.preview);
        }
        if (this.minSpanX != 2) {
            size += CodedOutputByteBufferNano.computeInt32Size(6, this.minSpanX);
        }
        if (this.minSpanY != 2) {
            return size + CodedOutputByteBufferNano.computeInt32Size(7, this.minSpanY);
        }
        return size;
    }

    @Override
    public BackupProtos$Widget mergeFrom(CodedInputByteBufferNano input) throws IOException {
        while (true) {
            int tag = input.readTag();
            switch (tag) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    return this;
                case 10:
                    this.provider = input.readString();
                    break;
                case 18:
                    this.label = input.readString();
                    break;
                case 24:
                    this.configure = input.readBool();
                    break;
                case 34:
                    if (this.icon == null) {
                        this.icon = new BackupProtos$Resource();
                    }
                    input.readMessage(this.icon);
                    break;
                case 42:
                    if (this.preview == null) {
                        this.preview = new BackupProtos$Resource();
                    }
                    input.readMessage(this.preview);
                    break;
                case 48:
                    this.minSpanX = input.readInt32();
                    break;
                case 56:
                    this.minSpanY = input.readInt32();
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
