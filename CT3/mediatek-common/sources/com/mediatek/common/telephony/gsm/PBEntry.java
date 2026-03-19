package com.mediatek.common.telephony.gsm;

import android.os.Parcel;
import android.os.Parcelable;

public class PBEntry implements Parcelable {
    public static final Parcelable.Creator<PBEntry> CREATOR = new Parcelable.Creator<PBEntry>() {
        @Override
        public PBEntry createFromParcel(Parcel source) {
            return PBEntry.reateFromParcel(source);
        }

        @Override
        public PBEntry[] newArray(int size) {
            return new PBEntry[size];
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

    public static PBEntry reateFromParcel(Parcel source) {
        PBEntry p = new PBEntry();
        p.mIndex1 = source.readInt();
        p.mNumber = source.readString();
        p.mType = source.readInt();
        p.mText = source.readString();
        p.mHidden = source.readInt();
        p.mGroup = source.readString();
        p.mAdnumber = source.readString();
        p.mAdtype = source.readInt();
        p.mSecondtext = source.readString();
        p.mEmail = source.readString();
        return p;
    }

    public void writeToParcel(Parcel dest) {
        dest.writeInt(this.mIndex1);
        dest.writeString(this.mNumber);
        dest.writeInt(this.mType);
        dest.writeString(this.mText);
        dest.writeInt(this.mHidden);
        dest.writeString(this.mGroup);
        dest.writeString(this.mAdnumber);
        dest.writeInt(this.mAdtype);
        dest.writeString(this.mSecondtext);
        dest.writeString(this.mEmail);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        writeToParcel(dest);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return super.toString() + ", index1: " + this.mIndex1 + ", number: " + this.mNumber + ", type:" + this.mType + ", text:" + this.mText + ", hidden:" + this.mHidden + ", group:" + this.mGroup + ", adnumber:" + this.mAdnumber + ", adtype:" + this.mAdtype + ", secondtext:" + this.mSecondtext + ", email:" + this.mEmail;
    }

    public void setIndex1(int iIndex1) {
        this.mIndex1 = iIndex1;
    }

    public void setNumber(String sNumber) {
        this.mNumber = sNumber;
    }

    public void setType(int iType) {
        this.mType = iType;
    }

    public void setText(String sText) {
        if (sText == null) {
            return;
        }
        this.mText = sText;
    }

    public void setHidden(int iHidden) {
        this.mHidden = iHidden;
    }

    public void setGroup(String sGroup) {
        this.mGroup = sGroup;
    }

    public void setAdnumber(String sAdnumber) {
        if (sAdnumber == null) {
            return;
        }
        this.mAdnumber = sAdnumber;
    }

    public void setAdtype(int iAdtype) {
        this.mAdtype = iAdtype;
    }

    public void setSecondtext(String sSecondtext) {
        this.mSecondtext = sSecondtext;
    }

    public void setEmail(String sEmail) {
        if (sEmail == null) {
            return;
        }
        this.mEmail = sEmail;
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
