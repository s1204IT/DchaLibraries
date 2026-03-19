package com.mediatek.internal.telephony.phb;

import android.os.Parcel;
import android.os.Parcelable;

public class UsimPBMemInfo implements Parcelable {
    public static final Parcelable.Creator<UsimPBMemInfo> CREATOR = new Parcelable.Creator<UsimPBMemInfo>() {
        @Override
        public UsimPBMemInfo createFromParcel(Parcel parcel) {
            return UsimPBMemInfo.createFromParcel(parcel);
        }

        @Override
        public UsimPBMemInfo[] newArray(int i) {
            return new UsimPBMemInfo[i];
        }
    };
    public static final int INT_NOT_SET = -1;
    public static final String STRING_NOT_SET = "";
    private int mSliceIndex = -1;
    private int mAdnLength = -1;
    private int mAdnUsed = -1;
    private int mAdnTotal = -1;
    private int mAdnType = -1;
    private int mExt1Length = -1;
    private int mExt1Used = -1;
    private int mExt1Total = -1;
    private int mExt1Type = -1;
    private int mGasLength = -1;
    private int mGasUsed = -1;
    private int mGasTotal = -1;
    private int mGasType = -1;
    private int mAnrLength = -1;
    private int mAnrUsed = -1;
    private int mAnrTotal = -1;
    private int mAnrType = -1;
    private int mAasLength = -1;
    private int mAasUsed = -1;
    private int mAasTotal = -1;
    private int mAasType = -1;
    private int mSneLength = -1;
    private int mSneUsed = -1;
    private int mSneTotal = -1;
    private int mSneType = -1;
    private int mEmailLength = -1;
    private int mEmailUsed = -1;
    private int mEmailTotal = -1;
    private int mEmailType = -1;
    private int mCcpLength = -1;
    private int mCcpUsed = -1;
    private int mCcpTotal = -1;
    private int mCcpType = -1;

    public static UsimPBMemInfo createFromParcel(Parcel parcel) {
        UsimPBMemInfo usimPBMemInfo = new UsimPBMemInfo();
        usimPBMemInfo.mSliceIndex = parcel.readInt();
        usimPBMemInfo.mAdnLength = parcel.readInt();
        usimPBMemInfo.mAdnUsed = parcel.readInt();
        usimPBMemInfo.mAdnTotal = parcel.readInt();
        usimPBMemInfo.mAdnType = parcel.readInt();
        usimPBMemInfo.mExt1Length = parcel.readInt();
        usimPBMemInfo.mExt1Used = parcel.readInt();
        usimPBMemInfo.mExt1Total = parcel.readInt();
        usimPBMemInfo.mExt1Type = parcel.readInt();
        usimPBMemInfo.mGasLength = parcel.readInt();
        usimPBMemInfo.mGasUsed = parcel.readInt();
        usimPBMemInfo.mGasTotal = parcel.readInt();
        usimPBMemInfo.mGasType = parcel.readInt();
        usimPBMemInfo.mAnrLength = parcel.readInt();
        usimPBMemInfo.mAnrUsed = parcel.readInt();
        usimPBMemInfo.mAnrTotal = parcel.readInt();
        usimPBMemInfo.mAnrType = parcel.readInt();
        usimPBMemInfo.mAasLength = parcel.readInt();
        usimPBMemInfo.mAasUsed = parcel.readInt();
        usimPBMemInfo.mAasTotal = parcel.readInt();
        usimPBMemInfo.mAasType = parcel.readInt();
        usimPBMemInfo.mSneLength = parcel.readInt();
        usimPBMemInfo.mSneUsed = parcel.readInt();
        usimPBMemInfo.mSneTotal = parcel.readInt();
        usimPBMemInfo.mSneType = parcel.readInt();
        usimPBMemInfo.mEmailLength = parcel.readInt();
        usimPBMemInfo.mEmailUsed = parcel.readInt();
        usimPBMemInfo.mEmailTotal = parcel.readInt();
        usimPBMemInfo.mEmailType = parcel.readInt();
        usimPBMemInfo.mCcpLength = parcel.readInt();
        usimPBMemInfo.mCcpUsed = parcel.readInt();
        usimPBMemInfo.mCcpTotal = parcel.readInt();
        usimPBMemInfo.mCcpType = parcel.readInt();
        return usimPBMemInfo;
    }

    public void writeToParcel(Parcel parcel) {
        parcel.writeInt(this.mSliceIndex);
        parcel.writeInt(this.mAdnLength);
        parcel.writeInt(this.mAdnUsed);
        parcel.writeInt(this.mAdnTotal);
        parcel.writeInt(this.mAdnType);
        parcel.writeInt(this.mExt1Length);
        parcel.writeInt(this.mExt1Used);
        parcel.writeInt(this.mExt1Total);
        parcel.writeInt(this.mExt1Type);
        parcel.writeInt(this.mGasLength);
        parcel.writeInt(this.mGasUsed);
        parcel.writeInt(this.mGasTotal);
        parcel.writeInt(this.mGasType);
        parcel.writeInt(this.mAnrLength);
        parcel.writeInt(this.mAnrUsed);
        parcel.writeInt(this.mAnrTotal);
        parcel.writeInt(this.mAnrType);
        parcel.writeInt(this.mAasLength);
        parcel.writeInt(this.mAasUsed);
        parcel.writeInt(this.mAasTotal);
        parcel.writeInt(this.mAasType);
        parcel.writeInt(this.mSneLength);
        parcel.writeInt(this.mSneUsed);
        parcel.writeInt(this.mSneTotal);
        parcel.writeInt(this.mSneType);
        parcel.writeInt(this.mEmailLength);
        parcel.writeInt(this.mEmailUsed);
        parcel.writeInt(this.mEmailTotal);
        parcel.writeInt(this.mEmailType);
        parcel.writeInt(this.mCcpLength);
        parcel.writeInt(this.mCcpUsed);
        parcel.writeInt(this.mCcpTotal);
        parcel.writeInt(this.mCcpType);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeToParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return super.toString() + " mSliceIndex: " + this.mSliceIndex + " mAdnLength: " + this.mAdnLength + " mAdnUsed: " + Integer.toString(this.mAdnUsed) + " mAdnTotal:" + Integer.toString(this.mAdnTotal) + " mAdnType:" + Integer.toString(this.mAdnType) + " mExt1Length:" + Integer.toString(this.mExt1Length) + " mExt1Used:" + Integer.toString(this.mExt1Used) + " mExt1Total" + Integer.toString(this.mExt1Total) + " mExt1Type" + Integer.toString(this.mExt1Type) + " mGasLength" + Integer.toString(this.mGasLength) + " mGasUsed" + Integer.toString(this.mGasUsed) + " mGasTotal: " + Integer.toString(this.mGasTotal) + " mGasType: " + Integer.toString(this.mGasType) + " mAnrLength: " + Integer.toString(this.mAnrLength) + " mAnrUsed: " + Integer.toString(this.mAnrUsed) + " mAnrTotal: " + Integer.toString(this.mAnrTotal) + " mAnrType: " + Integer.toString(this.mAnrType) + " mEmailLength: " + Integer.toString(this.mEmailLength) + " mEmailUsed: " + Integer.toString(this.mEmailUsed) + " mEmailTotal: " + Integer.toString(this.mEmailTotal) + " mEmailType: " + Integer.toString(this.mEmailType);
    }

    public int getSliceIndex() {
        return this.mSliceIndex;
    }

    public int getAdnLength() {
        return this.mAdnLength;
    }

    public int getAdnUsed() {
        return this.mAdnUsed;
    }

    public int getAdnTotal() {
        return this.mAdnTotal;
    }

    public int getAdnType() {
        return this.mAdnType;
    }

    public int getAdnFree() {
        return this.mAdnTotal - this.mAdnUsed;
    }

    public int getExt1Length() {
        return this.mExt1Length;
    }

    public int getExt1Used() {
        return this.mExt1Used;
    }

    public int getExt1Total() {
        return this.mExt1Total;
    }

    public int getExt1Type() {
        return this.mExt1Type;
    }

    public int getExt1Free() {
        return this.mExt1Total - this.mExt1Used;
    }

    public int getGasLength() {
        return this.mGasLength;
    }

    public int getGasUsed() {
        return this.mGasUsed;
    }

    public int getGasTotal() {
        return this.mGasTotal;
    }

    public int getGasType() {
        return this.mGasType;
    }

    public int getAnrLength() {
        return this.mAnrLength;
    }

    public int getAnrUsed() {
        return this.mAnrUsed;
    }

    public int getAnrTotal() {
        return this.mAnrTotal;
    }

    public int getAnrType() {
        return this.mAnrType;
    }

    public int getAnrFree() {
        return this.mAnrTotal - this.mAnrUsed;
    }

    public int getAasLength() {
        return this.mAasLength;
    }

    public int getAasUsed() {
        return this.mAasUsed;
    }

    public int getAasTotal() {
        return this.mAasTotal;
    }

    public int getAasType() {
        return this.mAasType;
    }

    public int getSneLength() {
        return this.mSneLength;
    }

    public int getSneUsed() {
        return this.mSneUsed;
    }

    public int getSneTotal() {
        return this.mSneTotal;
    }

    public int getSneType() {
        return this.mSneType;
    }

    public int getEmailLength() {
        return this.mEmailLength;
    }

    public int getEmailUsed() {
        return this.mEmailUsed;
    }

    public int getEmailTotal() {
        return this.mEmailTotal;
    }

    public int getEmailType() {
        return this.mEmailType;
    }

    public int getEmailFree() {
        return this.mEmailTotal - this.mEmailUsed;
    }

    public int getCcpLength() {
        return this.mCcpLength;
    }

    public int getCcpUsed() {
        return this.mCcpUsed;
    }

    public int getCcpTotal() {
        return this.mCcpTotal;
    }

    public int getCcpType() {
        return this.mCcpType;
    }

    public int getCcpFree() {
        return this.mCcpTotal - this.mCcpUsed;
    }

    public int getGasFree() {
        return this.mGasTotal - this.mGasUsed;
    }

    public int getAasFree() {
        return this.mAasTotal - this.mAasUsed;
    }

    public int getSneFree() {
        return this.mSneTotal - this.mSneUsed;
    }

    public void setSliceIndex(int i) {
        this.mSliceIndex = i;
    }

    public void setAdnLength(int i) {
        this.mAdnLength = i;
    }

    public void setAdnUsed(int i) {
        this.mAdnUsed = i;
    }

    public void setAdnTotal(int i) {
        this.mAdnTotal = i;
    }

    public void setAdnType(int i) {
        this.mAdnType = i;
    }

    public void setExt1Length(int i) {
        this.mExt1Length = i;
    }

    public void setExt1Used(int i) {
        this.mExt1Used = i;
    }

    public void setExt1Total(int i) {
        this.mExt1Total = i;
    }

    public void setExt1Type(int i) {
        this.mExt1Type = i;
    }

    public void setGasLength(int i) {
        this.mGasLength = i;
    }

    public void setGasUsed(int i) {
        this.mGasUsed = i;
    }

    public void setGasTotal(int i) {
        this.mGasTotal = i;
    }

    public void setGasType(int i) {
        this.mGasType = i;
    }

    public void setAnrLength(int i) {
        this.mAnrLength = i;
    }

    public void setAnrUsed(int i) {
        this.mAnrUsed = i;
    }

    public void setAnrTotal(int i) {
        this.mAnrTotal = i;
    }

    public void setAnrType(int i) {
        this.mAnrType = i;
    }

    public void setAasLength(int i) {
        this.mAasLength = i;
    }

    public void setAasUsed(int i) {
        this.mAasUsed = i;
    }

    public void setAasTotal(int i) {
        this.mAasTotal = i;
    }

    public void setAasType(int i) {
        this.mAasType = i;
    }

    public void setSneLength(int i) {
        this.mSneLength = i;
    }

    public void setSneUsed(int i) {
        this.mSneUsed = i;
    }

    public void setSneTotal(int i) {
        this.mSneTotal = i;
    }

    public void setSneType(int i) {
        this.mSneType = i;
    }

    public void setEmailLength(int i) {
        this.mEmailLength = i;
    }

    public void setEmailUsed(int i) {
        this.mEmailUsed = i;
    }

    public void setEmailTotal(int i) {
        this.mEmailTotal = i;
    }

    public void setEmailType(int i) {
        this.mEmailType = i;
    }

    public void setCcpLength(int i) {
        this.mCcpLength = i;
    }

    public void setCcpUsed(int i) {
        this.mCcpUsed = i;
    }

    public void setCcpTotal(int i) {
        this.mCcpTotal = i;
    }

    public void setCcpType(int i) {
        this.mCcpType = i;
    }
}
