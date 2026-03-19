package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.Reporting;
import com.android.server.usb.descriptors.report.UsbStrings;

public abstract class UsbDescriptor implements Reporting {
    public static final int AUDIO_AUDIOCONTROL = 1;
    public static final int AUDIO_AUDIOSTREAMING = 2;
    public static final int AUDIO_MIDISTREAMING = 3;
    public static final int AUDIO_SUBCLASS_UNDEFINED = 0;
    public static final int CLASSID_APPSPECIFIC = 254;
    public static final int CLASSID_AUDIO = 1;
    public static final int CLASSID_AUDIOVIDEO = 16;
    public static final int CLASSID_BILLBOARD = 17;
    public static final int CLASSID_CDC_CONTROL = 10;
    public static final int CLASSID_COM = 2;
    public static final int CLASSID_DEVICE = 0;
    public static final int CLASSID_DIAGNOSTIC = 220;
    public static final int CLASSID_HEALTHCARE = 15;
    public static final int CLASSID_HID = 3;
    public static final int CLASSID_HUB = 9;
    public static final int CLASSID_IMAGE = 6;
    public static final int CLASSID_MISC = 239;
    public static final int CLASSID_PHYSICAL = 5;
    public static final int CLASSID_PRINTER = 7;
    public static final int CLASSID_SECURITY = 13;
    public static final int CLASSID_SMART_CARD = 11;
    public static final int CLASSID_STORAGE = 8;
    public static final int CLASSID_TYPECBRIDGE = 18;
    public static final int CLASSID_VENDSPECIFIC = 255;
    public static final int CLASSID_VIDEO = 14;
    public static final int CLASSID_WIRELESS = 224;
    public static final byte DESCRIPTORTYPE_AUDIO_ENDPOINT = 37;
    public static final byte DESCRIPTORTYPE_AUDIO_INTERFACE = 36;
    public static final byte DESCRIPTORTYPE_BOS = 15;
    public static final byte DESCRIPTORTYPE_CAPABILITY = 16;
    public static final byte DESCRIPTORTYPE_CONFIG = 2;
    public static final byte DESCRIPTORTYPE_DEVICE = 1;
    public static final byte DESCRIPTORTYPE_ENDPOINT = 5;
    public static final byte DESCRIPTORTYPE_ENDPOINT_COMPANION = 48;
    public static final byte DESCRIPTORTYPE_HID = 33;
    public static final byte DESCRIPTORTYPE_HUB = 41;
    public static final byte DESCRIPTORTYPE_INTERFACE = 4;
    public static final byte DESCRIPTORTYPE_INTERFACEASSOC = 11;
    public static final byte DESCRIPTORTYPE_PHYSICAL = 35;
    public static final byte DESCRIPTORTYPE_REPORT = 34;
    public static final byte DESCRIPTORTYPE_STRING = 3;
    public static final byte DESCRIPTORTYPE_SUPERSPEED_HUB = 42;
    public static final int REQUEST_CLEAR_FEATURE = 1;
    public static final int REQUEST_GET_ADDRESS = 5;
    public static final int REQUEST_GET_CONFIGURATION = 8;
    public static final int REQUEST_GET_DESCRIPTOR = 6;
    public static final int REQUEST_GET_STATUS = 0;
    public static final int REQUEST_SET_CONFIGURATION = 9;
    public static final int REQUEST_SET_DESCRIPTOR = 7;
    public static final int REQUEST_SET_FEATURE = 3;
    private static final int SIZE_STRINGBUFFER = 256;
    public static final int STATUS_PARSED_OK = 1;
    public static final int STATUS_PARSED_OVERRUN = 3;
    public static final int STATUS_PARSED_UNDERRUN = 2;
    public static final int STATUS_PARSE_EXCEPTION = 4;
    public static final int STATUS_UNPARSED = 0;
    private static final String TAG = "UsbDescriptor";
    protected int mHierarchyLevel;
    protected final int mLength;
    private int mOverUnderRunCount;
    private byte[] mRawData;
    private int mStatus = 0;
    protected final byte mType;
    private static byte[] sStringBuffer = new byte[256];
    private static String[] sStatusStrings = {"UNPARSED", "PARSED - OK", "PARSED - UNDERRUN", "PARSED - OVERRUN"};

    UsbDescriptor(int i, byte b) {
        if (i < 2) {
            throw new IllegalArgumentException();
        }
        this.mLength = i;
        this.mType = b;
    }

    public int getLength() {
        return this.mLength;
    }

    public byte getType() {
        return this.mType;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public void setStatus(int i) {
        this.mStatus = i;
    }

    public int getOverUnderRunCount() {
        return this.mOverUnderRunCount;
    }

    public String getStatusString() {
        return sStatusStrings[this.mStatus];
    }

    public byte[] getRawData() {
        return this.mRawData;
    }

    public void postParse(ByteStream byteStream) {
        int readCount = byteStream.getReadCount();
        if (readCount < this.mLength) {
            byteStream.advance(this.mLength - readCount);
            this.mStatus = 2;
            this.mOverUnderRunCount = this.mLength - readCount;
            Log.w(TAG, "UNDERRUN t:0x" + Integer.toHexString(this.mType) + " r: " + readCount + " < l: " + this.mLength);
            return;
        }
        if (readCount > this.mLength) {
            byteStream.reverse(readCount - this.mLength);
            this.mStatus = 3;
            this.mOverUnderRunCount = readCount - this.mLength;
            Log.w(TAG, "OVERRRUN t:0x" + Integer.toHexString(this.mType) + " r: " + readCount + " > l: " + this.mLength);
            return;
        }
        this.mStatus = 1;
    }

    public int parseRawDescriptors(ByteStream byteStream) {
        int readCount = this.mLength - byteStream.getReadCount();
        if (readCount > 0) {
            this.mRawData = new byte[readCount];
            for (int i = 0; i < readCount; i++) {
                this.mRawData[i] = byteStream.getByte();
            }
        }
        return this.mLength;
    }

    public static String getUsbDescriptorString(UsbDeviceConnection usbDeviceConnection, byte b) {
        String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (b == 0) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        try {
            int iControlTransfer = usbDeviceConnection.controlTransfer(128, 6, 768 | b, 0, sStringBuffer, 255, 0);
            if (iControlTransfer >= 0) {
                str = new String(sStringBuffer, 2, iControlTransfer - 2, "UTF-16LE");
                return str;
            }
            return "?";
        } catch (Exception e) {
            Log.e(TAG, "Can not communicate with USB device", e);
            return str;
        }
    }

    private void reportParseStatus(ReportCanvas reportCanvas) {
        switch (getStatus()) {
            case 0:
            case 2:
            case 3:
                reportCanvas.writeParagraph("status: " + getStatusString() + " [" + getOverUnderRunCount() + "]", true);
                break;
        }
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        String str = UsbStrings.getDescriptorName(getType()) + ": " + ReportCanvas.getHexString(getType()) + " Len: " + getLength();
        if (this.mHierarchyLevel != 0) {
            reportCanvas.writeHeader(this.mHierarchyLevel, str);
        } else {
            reportCanvas.writeParagraph(str, false);
        }
        if (getStatus() != 1) {
            reportParseStatus(reportCanvas);
        }
    }

    @Override
    public void shortReport(ReportCanvas reportCanvas) {
        reportCanvas.writeParagraph(UsbStrings.getDescriptorName(getType()) + ": " + ReportCanvas.getHexString(getType()) + " Len: " + getLength(), false);
    }
}
