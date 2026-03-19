package com.android.server.usb.descriptors;

import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACMixerUnit extends UsbACMixerUnit {
    private static final String TAG = "Usb10ACMixerUnit";
    private byte mChanNameID;
    private int mChannelConfig;
    private byte[] mControls;
    private byte mNameID;

    public Usb10ACMixerUnit(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public int getChannelConfig() {
        return this.mChannelConfig;
    }

    public byte getChanNameID() {
        return this.mChanNameID;
    }

    public byte[] getControls() {
        return this.mControls;
    }

    public byte getNameID() {
        return this.mNameID;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mChannelConfig = byteStream.unpackUsbShort();
        this.mChanNameID = byteStream.getByte();
        int iCalcControlArraySize = calcControlArraySize(this.mNumInputs, this.mNumOutputs);
        this.mControls = new byte[iCalcControlArraySize];
        for (int i = 0; i < iCalcControlArraySize; i++) {
            this.mControls[i] = byteStream.getByte();
        }
        this.mNameID = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.writeParagraph("Mixer Unit", false);
        reportCanvas.openList();
        reportCanvas.writeListItem("Unit ID: " + ReportCanvas.getHexString(getUnitID()));
        byte numInputs = getNumInputs();
        byte[] inputIDs = getInputIDs();
        reportCanvas.openListItem();
        reportCanvas.write("Num Inputs: " + ((int) numInputs) + " [");
        for (int i = 0; i < numInputs; i++) {
            reportCanvas.write(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ReportCanvas.getHexString(inputIDs[i]));
            if (i < numInputs - 1) {
                reportCanvas.write(" ");
            }
        }
        reportCanvas.write("]");
        reportCanvas.closeListItem();
        reportCanvas.writeListItem("Num Outputs: " + ((int) getNumOutputs()));
        reportCanvas.writeListItem("Channel Config: " + ReportCanvas.getHexString(getChannelConfig()));
        byte[] controls = getControls();
        reportCanvas.openListItem();
        reportCanvas.write("Controls: " + controls.length + " [");
        for (int i2 = 0; i2 < controls.length; i2++) {
            reportCanvas.write(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) controls[i2]));
            if (i2 < controls.length - 1) {
                reportCanvas.write(" ");
            }
        }
        reportCanvas.write("]");
        reportCanvas.closeListItem();
        reportCanvas.closeList();
    }
}
