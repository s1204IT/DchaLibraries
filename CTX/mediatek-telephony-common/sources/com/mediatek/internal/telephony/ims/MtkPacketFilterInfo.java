package com.mediatek.internal.telephony.ims;

import android.os.Parcel;
import android.os.Parcelable;

public class MtkPacketFilterInfo implements Parcelable {
    public static final Parcelable.Creator<MtkPacketFilterInfo> CREATOR = new Parcelable.Creator<MtkPacketFilterInfo>() {
        @Override
        public MtkPacketFilterInfo createFromParcel(Parcel parcel) {
            return MtkPacketFilterInfo.readFrom(parcel);
        }

        @Override
        public MtkPacketFilterInfo[] newArray(int i) {
            return new MtkPacketFilterInfo[i];
        }
    };
    public static final int IMC_BMP_FLOW_LABEL = 512;
    public static final int IMC_BMP_LOCAL_PORT_RANGE = 16;
    public static final int IMC_BMP_LOCAL_PORT_SINGLE = 8;
    public static final int IMC_BMP_NONE = 0;
    public static final int IMC_BMP_PROTOCOL = 4;
    public static final int IMC_BMP_REMOTE_PORT_RANGE = 64;
    public static final int IMC_BMP_REMOTE_PORT_SINGLE = 32;
    public static final int IMC_BMP_SPI = 128;
    public static final int IMC_BMP_TOS = 256;
    public static final int IMC_BMP_V4_ADDR = 1;
    public static final int IMC_BMP_V6_ADDR = 2;
    public String mAddress;
    public int mBitmap;
    public int mDirection;
    public int mFlowLabel;
    public int mId;
    public int mLocalPortHigh;
    public int mLocalPortLow;
    public String mMask;
    public int mNetworkPfIdentifier;
    public int mPrecedence;
    public int mProtocolNextHeader;
    public int mRemotePortHigh;
    public int mRemotePortLow;
    public int mSpi;
    public int mTos;
    public int mTosMask;

    public MtkPacketFilterInfo(int i, int i2, int i3, int i4, int i5, String str, String str2, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14) {
        this.mId = i;
        this.mPrecedence = i2;
        this.mDirection = i3;
        this.mNetworkPfIdentifier = i4;
        this.mBitmap = i5;
        this.mAddress = str;
        this.mMask = str2;
        this.mProtocolNextHeader = i6;
        this.mLocalPortLow = i7;
        this.mLocalPortHigh = i8;
        this.mRemotePortLow = i9;
        this.mRemotePortHigh = i10;
        this.mSpi = i11;
        this.mTos = i12;
        this.mTosMask = i13;
        this.mFlowLabel = i14;
    }

    public static MtkPacketFilterInfo readFrom(Parcel parcel) {
        return new MtkPacketFilterInfo(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
    }

    public void writeTo(Parcel parcel) {
        parcel.writeInt(this.mId);
        parcel.writeInt(this.mPrecedence);
        parcel.writeInt(this.mDirection);
        parcel.writeInt(this.mNetworkPfIdentifier);
        parcel.writeInt(this.mBitmap);
        parcel.writeString(this.mAddress == null ? "" : this.mAddress);
        parcel.writeString(this.mMask == null ? "" : this.mMask);
        parcel.writeInt(this.mProtocolNextHeader);
        parcel.writeInt(this.mLocalPortLow);
        parcel.writeInt(this.mLocalPortHigh);
        parcel.writeInt(this.mRemotePortLow);
        parcel.writeInt(this.mRemotePortHigh);
        parcel.writeInt(this.mSpi);
        parcel.writeInt(this.mTos);
        parcel.writeInt(this.mTosMask);
        parcel.writeInt(this.mFlowLabel);
    }

    public String toString() {
        return "[id=" + this.mId + ", precedence=" + this.mPrecedence + ", direction=" + this.mDirection + ", networkPfIdentifier=" + this.mNetworkPfIdentifier + ", bitmap=" + Integer.toHexString(this.mBitmap) + ", address=" + this.mAddress + ", mask=" + this.mMask + ", protocolNextHeader=" + this.mProtocolNextHeader + ", localPortLow=" + this.mLocalPortLow + ", localPortHigh=" + this.mLocalPortHigh + ", remotePortLow=" + this.mRemotePortLow + ", remotePortHigh=" + this.mRemotePortHigh + ", spi=" + Integer.toHexString(this.mSpi) + ", tos=" + this.mTos + ", tosMask=" + this.mTosMask + ", flowLabel=" + Integer.toHexString(this.mFlowLabel) + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeTo(parcel);
    }
}
