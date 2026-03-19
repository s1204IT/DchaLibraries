package com.mediatek.gba;

import android.os.Parcel;
import android.os.Parcelable;

public class NafSessionKey implements Parcelable {
    public static final Parcelable.Creator<NafSessionKey> CREATOR = new Parcelable.Creator<NafSessionKey>() {
        @Override
        public NafSessionKey createFromParcel(Parcel parcel) {
            NafSessionKey nafSessionKey = new NafSessionKey();
            String string = parcel.readString();
            if (string != null) {
                nafSessionKey.setBtid(string);
            }
            byte[] bArrCreateByteArray = parcel.createByteArray();
            if (bArrCreateByteArray != null) {
                nafSessionKey.setKey(bArrCreateByteArray);
            }
            String string2 = parcel.readString();
            if (string2 != null) {
                nafSessionKey.setKeylifetime(string2);
            }
            String string3 = parcel.readString();
            if (string3 != null) {
                nafSessionKey.setNafKeyName(string3);
            }
            String string4 = parcel.readString();
            if (string4 != null) {
                nafSessionKey.setAuthHeader(string4);
            }
            parcel.readInt();
            String string5 = parcel.readString();
            if (string5 != null) {
                nafSessionKey.setException(new IllegalStateException(string5));
            }
            return nafSessionKey;
        }

        @Override
        public NafSessionKey[] newArray(int i) {
            return new NafSessionKey[i];
        }
    };
    private String mAuthHeader;
    private String mBtid;
    private Exception mException;
    private byte[] mKey;
    private String mKeylifetime;
    private byte[] mNafId;
    private String mNafKeyName;

    public NafSessionKey() {
    }

    public NafSessionKey(String str, byte[] bArr, String str2) {
        this.mBtid = str;
        this.mKey = bArr;
        this.mKeylifetime = str2;
    }

    public String getBtid() {
        return this.mBtid;
    }

    public void setBtid(String str) {
        this.mBtid = str;
    }

    public byte[] getKey() {
        return this.mKey;
    }

    public void setKey(byte[] bArr) {
        this.mKey = bArr;
    }

    public String getKeylifetime() {
        return this.mKeylifetime;
    }

    public void setKeylifetime(String str) {
        this.mKeylifetime = str;
    }

    public String getNafKeyName() {
        return this.mNafKeyName;
    }

    public void setNafKeyName(String str) {
        this.mNafKeyName = str;
    }

    public void setNafId(byte[] bArr) {
        this.mNafId = bArr;
    }

    public byte[] getNafId() {
        return this.mNafId;
    }

    public String getAuthHeader() {
        return this.mAuthHeader;
    }

    public void setAuthHeader(String str) {
        this.mAuthHeader = str;
    }

    public void setException(Exception exc) {
        this.mException = exc;
    }

    public Exception getException() {
        return this.mException;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mBtid);
        parcel.writeByteArray(this.mKey);
        parcel.writeString(this.mKeylifetime);
        parcel.writeString(this.mNafKeyName);
        parcel.writeString(this.mAuthHeader);
        if (this.mException != null) {
            parcel.writeException(this.mException);
        }
    }

    public String toString() {
        String string;
        synchronized (this) {
            StringBuilder sb = new StringBuilder("NafSessionKey -");
            sb.append(" btid: " + this.mBtid);
            sb.append(" keylifetime: " + this.mKeylifetime);
            sb.append(" nafkeyname: " + this.mNafKeyName);
            sb.append(" authheader: " + this.mAuthHeader);
            string = sb.toString();
        }
        return string;
    }
}
