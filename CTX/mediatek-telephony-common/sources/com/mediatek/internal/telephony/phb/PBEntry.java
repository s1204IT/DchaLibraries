package com.mediatek.internal.telephony.phb;

import android.os.Parcel;
import android.os.Parcelable;

public class PBEntry implements Parcelable {
    public static final Parcelable.Creator<PBEntry> CREATOR = new Parcelable.Creator<PBEntry>() {
        @Override
        public PBEntry createFromParcel(Parcel parcel) {
            return PBEntry.reateFromParcel(parcel);
        }

        @Override
        public PBEntry[] newArray(int i) {
            return new PBEntry[i];
        }
    };
    public static final int INT_NOT_SET = -1;
    public static final String STRING_NOT_SET = "";
    private int mIndex1 = -1;
    private String mNumber = "";
    private int mType = -1;
    private String mText = "";
    private int mHidden = 0;
    private String mGroup = "";
    private String mAdnumber = "";
    private int mAdtype = -1;
    private String mSecondtext = "";
    private String mEmail = "";

    public static PBEntry reateFromParcel(Parcel parcel) {
        PBEntry pBEntry = new PBEntry();
        pBEntry.mIndex1 = parcel.readInt();
        pBEntry.mNumber = parcel.readString();
        pBEntry.mType = parcel.readInt();
        pBEntry.mText = parcel.readString();
        pBEntry.mHidden = parcel.readInt();
        pBEntry.mGroup = parcel.readString();
        pBEntry.mAdnumber = parcel.readString();
        pBEntry.mAdtype = parcel.readInt();
        pBEntry.mSecondtext = parcel.readString();
        pBEntry.mEmail = parcel.readString();
        return pBEntry;
    }

    public void writeToParcel(Parcel parcel) {
        parcel.writeInt(this.mIndex1);
        parcel.writeString(this.mNumber);
        parcel.writeInt(this.mType);
        parcel.writeString(this.mText);
        parcel.writeInt(this.mHidden);
        parcel.writeString(this.mGroup);
        parcel.writeString(this.mAdnumber);
        parcel.writeInt(this.mAdtype);
        parcel.writeString(this.mSecondtext);
        parcel.writeString(this.mEmail);
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
        return super.toString() + ", index1: " + this.mIndex1 + ", number: " + this.mNumber + ", type:" + this.mType + ", text:" + this.mText + ", hidden:" + this.mHidden + ", group:" + this.mGroup + ", adnumber:" + this.mAdnumber + ", adtype:" + this.mAdtype + ", secondtext:" + this.mSecondtext + ", email:" + this.mEmail;
    }

    public void setIndex1(int i) {
        this.mIndex1 = i;
    }

    public void setNumber(String str) {
        this.mNumber = str;
    }

    public void setType(int i) {
        this.mType = i;
    }

    public void setText(String str) {
        if (str != null) {
            this.mText = str;
        }
    }

    public void setHidden(int i) {
        this.mHidden = i;
    }

    public void setGroup(String str) {
        this.mGroup = str;
    }

    public void setAdnumber(String str) {
        if (str != null) {
            this.mAdnumber = str;
        }
    }

    public void setAdtype(int i) {
        this.mAdtype = i;
    }

    public void setSecondtext(String str) {
        this.mSecondtext = str;
    }

    public void setEmail(String str) {
        if (str != null) {
            this.mEmail = str;
        }
    }

    public int getIndex1() {
        return this.mIndex1;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public int getType() {
        return this.mType;
    }

    public String getText() {
        return this.mText;
    }

    public int getHidden() {
        return this.mHidden;
    }

    public String getGroup() {
        return this.mGroup;
    }

    public String getAdnumber() {
        return this.mAdnumber;
    }

    public int getAdtype() {
        return this.mAdtype;
    }

    public String getSecondtext() {
        return this.mSecondtext;
    }

    public String getEmail() {
        return this.mEmail;
    }
}
