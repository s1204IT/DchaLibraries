package jp.co.omronsoft.android.decoemojimanager.interfacedata;

import android.os.Parcel;
import android.os.Parcelable;

public class DecoEmojiAttrInfo implements Parcelable {
    public static final Parcelable.Creator<DecoEmojiAttrInfo> CREATOR = new Parcelable.Creator<DecoEmojiAttrInfo>() {
        @Override
        public DecoEmojiAttrInfo createFromParcel(Parcel in) {
            return new DecoEmojiAttrInfo(in);
        }

        @Override
        public DecoEmojiAttrInfo[] newArray(int size) {
            return new DecoEmojiAttrInfo[size];
        }
    };
    private int mId;
    private String[] mName;
    private String[] mNote;
    private byte[] mPart;

    public DecoEmojiAttrInfo() {
        this.mName = new String[10];
        this.mPart = new byte[10];
        this.mNote = new String[10];
    }

    public DecoEmojiAttrInfo(Parcel in) {
        this.mName = new String[10];
        this.mPart = new byte[10];
        this.mNote = new String[10];
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mId);
        dest.writeStringArray(this.mName);
        dest.writeByteArray(this.mPart);
        dest.writeStringArray(this.mNote);
    }

    public void readFromParcel(Parcel in) {
        this.mId = in.readInt();
        in.readStringArray(this.mName);
        in.readByteArray(this.mPart);
        in.readStringArray(this.mNote);
    }

    public int getId() {
        return this.mId;
    }

    public void setID(int id) {
        this.mId = id;
    }

    public String getName(int idx) {
        return this.mName[idx];
    }

    public byte getPart(int idx) {
        return this.mPart[idx];
    }

    public void setName(int idx, String name) {
        this.mName[idx] = name;
    }

    public void setPart(int idx, byte part) {
        this.mPart[idx] = part;
    }

    public String getNote(int idx) {
        return this.mNote[idx];
    }

    public void setNote(int idx, String note) {
        this.mNote[idx] = note;
    }
}
