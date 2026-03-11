package com.android.launcher3.backup.nano;

import com.android.launcher3.compat.PackageInstallerCompat;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;
import java.util.Arrays;

public final class BackupProtos$Favorite extends MessageNano {
    public int appWidgetId;
    public String appWidgetProvider;
    public int cellX;
    public int cellY;
    public int container;
    public int displayMode;
    public byte[] icon;
    public String iconPackage;
    public String iconResource;
    public int iconType;
    public long id;
    public String intent;
    public int itemType;
    public int rank;
    public int screen;
    public int spanX;
    public int spanY;
    public int targetType;
    public String title;
    public String uri;

    public BackupProtos$Favorite() {
        clear();
    }

    public BackupProtos$Favorite clear() {
        this.id = 0L;
        this.itemType = 0;
        this.title = "";
        this.container = 0;
        this.screen = 0;
        this.cellX = 0;
        this.cellY = 0;
        this.spanX = 0;
        this.spanY = 0;
        this.displayMode = 0;
        this.appWidgetId = 0;
        this.appWidgetProvider = "";
        this.intent = "";
        this.uri = "";
        this.iconType = 0;
        this.iconPackage = "";
        this.iconResource = "";
        this.icon = WireFormatNano.EMPTY_BYTES;
        this.targetType = 0;
        this.rank = 0;
        this.cachedSize = -1;
        return this;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeInt64(1, this.id);
        output.writeInt32(2, this.itemType);
        if (!this.title.equals("")) {
            output.writeString(3, this.title);
        }
        if (this.container != 0) {
            output.writeInt32(4, this.container);
        }
        if (this.screen != 0) {
            output.writeInt32(5, this.screen);
        }
        if (this.cellX != 0) {
            output.writeInt32(6, this.cellX);
        }
        if (this.cellY != 0) {
            output.writeInt32(7, this.cellY);
        }
        if (this.spanX != 0) {
            output.writeInt32(8, this.spanX);
        }
        if (this.spanY != 0) {
            output.writeInt32(9, this.spanY);
        }
        if (this.displayMode != 0) {
            output.writeInt32(10, this.displayMode);
        }
        if (this.appWidgetId != 0) {
            output.writeInt32(11, this.appWidgetId);
        }
        if (!this.appWidgetProvider.equals("")) {
            output.writeString(12, this.appWidgetProvider);
        }
        if (!this.intent.equals("")) {
            output.writeString(13, this.intent);
        }
        if (!this.uri.equals("")) {
            output.writeString(14, this.uri);
        }
        if (this.iconType != 0) {
            output.writeInt32(15, this.iconType);
        }
        if (!this.iconPackage.equals("")) {
            output.writeString(16, this.iconPackage);
        }
        if (!this.iconResource.equals("")) {
            output.writeString(17, this.iconResource);
        }
        if (!Arrays.equals(this.icon, WireFormatNano.EMPTY_BYTES)) {
            output.writeBytes(18, this.icon);
        }
        if (this.targetType != 0) {
            output.writeInt32(19, this.targetType);
        }
        if (this.rank != 0) {
            output.writeInt32(20, this.rank);
        }
        super.writeTo(output);
    }

    @Override
    protected int computeSerializedSize() {
        int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt64Size(1, this.id) + CodedOutputByteBufferNano.computeInt32Size(2, this.itemType);
        if (!this.title.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(3, this.title);
        }
        if (this.container != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(4, this.container);
        }
        if (this.screen != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(5, this.screen);
        }
        if (this.cellX != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(6, this.cellX);
        }
        if (this.cellY != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(7, this.cellY);
        }
        if (this.spanX != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(8, this.spanX);
        }
        if (this.spanY != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(9, this.spanY);
        }
        if (this.displayMode != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(10, this.displayMode);
        }
        if (this.appWidgetId != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(11, this.appWidgetId);
        }
        if (!this.appWidgetProvider.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(12, this.appWidgetProvider);
        }
        if (!this.intent.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(13, this.intent);
        }
        if (!this.uri.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(14, this.uri);
        }
        if (this.iconType != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(15, this.iconType);
        }
        if (!this.iconPackage.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(16, this.iconPackage);
        }
        if (!this.iconResource.equals("")) {
            size += CodedOutputByteBufferNano.computeStringSize(17, this.iconResource);
        }
        if (!Arrays.equals(this.icon, WireFormatNano.EMPTY_BYTES)) {
            size += CodedOutputByteBufferNano.computeBytesSize(18, this.icon);
        }
        if (this.targetType != 0) {
            size += CodedOutputByteBufferNano.computeInt32Size(19, this.targetType);
        }
        if (this.rank != 0) {
            return size + CodedOutputByteBufferNano.computeInt32Size(20, this.rank);
        }
        return size;
    }

    @Override
    public BackupProtos$Favorite mergeFrom(CodedInputByteBufferNano input) throws IOException {
        while (true) {
            int tag = input.readTag();
            switch (tag) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    return this;
                case 8:
                    this.id = input.readInt64();
                    break;
                case 16:
                    this.itemType = input.readInt32();
                    break;
                case 26:
                    this.title = input.readString();
                    break;
                case 32:
                    this.container = input.readInt32();
                    break;
                case 40:
                    this.screen = input.readInt32();
                    break;
                case 48:
                    this.cellX = input.readInt32();
                    break;
                case 56:
                    this.cellY = input.readInt32();
                    break;
                case 64:
                    this.spanX = input.readInt32();
                    break;
                case 72:
                    this.spanY = input.readInt32();
                    break;
                case 80:
                    this.displayMode = input.readInt32();
                    break;
                case 88:
                    this.appWidgetId = input.readInt32();
                    break;
                case 98:
                    this.appWidgetProvider = input.readString();
                    break;
                case 106:
                    this.intent = input.readString();
                    break;
                case 114:
                    this.uri = input.readString();
                    break;
                case 120:
                    this.iconType = input.readInt32();
                    break;
                case 130:
                    this.iconPackage = input.readString();
                    break;
                case 138:
                    this.iconResource = input.readString();
                    break;
                case 146:
                    this.icon = input.readBytes();
                    break;
                case 152:
                    int value = input.readInt32();
                    switch (value) {
                        case PackageInstallerCompat.STATUS_INSTALLED:
                        case PackageInstallerCompat.STATUS_INSTALLING:
                        case PackageInstallerCompat.STATUS_FAILED:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                            this.targetType = value;
                            break;
                    }
                    break;
                case 160:
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
