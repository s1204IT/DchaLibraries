package org.gsma.joyn.ft;

import android.os.Parcel;
import android.os.Parcelable;
import org.gsma.joyn.Logger;

public class FileTransferServiceConfiguration implements Parcelable {
    public static final Parcelable.Creator<FileTransferServiceConfiguration> CREATOR = new Parcelable.Creator<FileTransferServiceConfiguration>() {
        @Override
        public FileTransferServiceConfiguration createFromParcel(Parcel source) {
            return new FileTransferServiceConfiguration(source);
        }

        @Override
        public FileTransferServiceConfiguration[] newArray(int size) {
            return new FileTransferServiceConfiguration[size];
        }
    };
    public static final String TAG = "TAPI-FileTransferServiceConfiguration";
    private boolean autoAcceptMode;
    private boolean fileIcon;
    private long maxFileIconSize;
    private int maxFileTransfers;
    private long maxSize;
    private long warnSize;

    public FileTransferServiceConfiguration(long warnSize, long maxSize, boolean autoAcceptMode, boolean fileIcon, long maxFileIconSize, int maxFileTransfers) {
        Logger.i(TAG, "FileTransferServiceConfiguration entrywarnSize " + warnSize + "maxSize " + maxSize + "autoAcceptMode " + autoAcceptMode + "fileIcon" + fileIcon + "maxFileIconSize " + maxFileIconSize + "maxFileTransfers" + maxFileTransfers);
        this.warnSize = warnSize;
        this.maxSize = maxSize;
        this.autoAcceptMode = autoAcceptMode;
        this.fileIcon = fileIcon;
        this.maxFileIconSize = maxFileIconSize;
        this.maxFileTransfers = maxFileTransfers;
    }

    public FileTransferServiceConfiguration(Parcel source) {
        this.warnSize = source.readLong();
        this.maxSize = source.readLong();
        this.autoAcceptMode = source.readInt() != 0;
        this.fileIcon = source.readInt() != 0;
        this.maxFileIconSize = source.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.warnSize);
        dest.writeLong(this.maxSize);
        dest.writeInt(this.autoAcceptMode ? 1 : 0);
        dest.writeInt(this.fileIcon ? 1 : 0);
        dest.writeLong(this.maxFileIconSize);
    }

    public long getWarnSize() {
        return this.warnSize;
    }

    public long getMaxSize() {
        return this.maxSize;
    }

    public boolean getAutoAcceptMode() {
        return this.autoAcceptMode;
    }

    public boolean isFileIconSupported() {
        return this.fileIcon;
    }

    public long getMaxFileIconSize() {
        return this.maxFileIconSize;
    }

    public int getMaxFileTransfers() {
        return this.maxFileTransfers;
    }
}
