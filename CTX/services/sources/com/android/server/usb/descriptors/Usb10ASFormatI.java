package com.android.server.usb.descriptors;

import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ASFormatI extends UsbASFormat {
    private static final String TAG = "Usb10ASFormatI";
    private byte mBitResolution;
    private byte mNumChannels;
    private byte mSampleFreqType;
    private int[] mSampleRates;
    private byte mSubframeSize;

    public Usb10ASFormatI(int i, byte b, byte b2, byte b3, int i2) {
        super(i, b, b2, b3, i2);
    }

    public byte getNumChannels() {
        return this.mNumChannels;
    }

    public byte getSubframeSize() {
        return this.mSubframeSize;
    }

    public byte getBitResolution() {
        return this.mBitResolution;
    }

    public byte getSampleFreqType() {
        return this.mSampleFreqType;
    }

    @Override
    public int[] getSampleRates() {
        return this.mSampleRates;
    }

    @Override
    public int[] getBitDepths() {
        return new int[]{this.mBitResolution};
    }

    @Override
    public int[] getChannelCounts() {
        return new int[]{this.mNumChannels};
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mNumChannels = byteStream.getByte();
        this.mSubframeSize = byteStream.getByte();
        this.mBitResolution = byteStream.getByte();
        this.mSampleFreqType = byteStream.getByte();
        if (this.mSampleFreqType == 0) {
            this.mSampleRates = new int[2];
            this.mSampleRates[0] = byteStream.unpackUsbTriple();
            this.mSampleRates[1] = byteStream.unpackUsbTriple();
        } else {
            this.mSampleRates = new int[this.mSampleFreqType];
            for (int i = 0; i < this.mSampleFreqType; i++) {
                this.mSampleRates[i] = byteStream.unpackUsbTriple();
            }
        }
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) getNumChannels()) + " Channels.");
        StringBuilder sb = new StringBuilder();
        sb.append("Subframe Size: ");
        sb.append((int) getSubframeSize());
        reportCanvas.writeListItem(sb.toString());
        reportCanvas.writeListItem("Bit Resolution: " + ((int) getBitResolution()));
        byte sampleFreqType = getSampleFreqType();
        int[] sampleRates = getSampleRates();
        reportCanvas.writeListItem("Sample Freq Type: " + ((int) sampleFreqType));
        reportCanvas.openList();
        if (sampleFreqType == 0) {
            reportCanvas.writeListItem("min: " + sampleRates[0]);
            reportCanvas.writeListItem("max: " + sampleRates[1]);
        } else {
            for (int i = 0; i < sampleFreqType; i++) {
                reportCanvas.writeListItem(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + sampleRates[i]);
            }
        }
        reportCanvas.closeList();
        reportCanvas.closeList();
    }
}
