package android.hardware.hdmi;

import android.os.Parcel;
import android.os.Parcelable;

public final class HdmiPortInfo implements Parcelable {
    public static final Parcelable.Creator<HdmiPortInfo> CREATOR = new Parcelable.Creator<HdmiPortInfo>() {
        @Override
        public HdmiPortInfo createFromParcel(Parcel source) {
            int id = source.readInt();
            int type = source.readInt();
            int address = source.readInt();
            boolean cec = source.readInt() == 1;
            boolean arc = source.readInt() == 1;
            boolean mhl = source.readInt() == 1;
            return new HdmiPortInfo(id, type, address, cec, mhl, arc);
        }

        @Override
        public HdmiPortInfo[] newArray(int size) {
            return new HdmiPortInfo[size];
        }
    };
    public static final int PORT_INPUT = 0;
    public static final int PORT_OUTPUT = 1;
    private final int mAddress;
    private final boolean mArcSupported;
    private final boolean mCecSupported;
    private final int mId;
    private final boolean mMhlSupported;
    private final int mType;

    public HdmiPortInfo(int id, int type, int address, boolean cec, boolean mhl, boolean arc) {
        this.mId = id;
        this.mType = type;
        this.mAddress = address;
        this.mCecSupported = cec;
        this.mArcSupported = arc;
        this.mMhlSupported = mhl;
    }

    public int getId() {
        return this.mId;
    }

    public int getType() {
        return this.mType;
    }

    public int getAddress() {
        return this.mAddress;
    }

    public boolean isCecSupported() {
        return this.mCecSupported;
    }

    public boolean isMhlSupported() {
        return this.mMhlSupported;
    }

    public boolean isArcSupported() {
        return this.mArcSupported;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mId);
        dest.writeInt(this.mType);
        dest.writeInt(this.mAddress);
        dest.writeInt(this.mCecSupported ? 1 : 0);
        dest.writeInt(this.mArcSupported ? 1 : 0);
        dest.writeInt(this.mMhlSupported ? 1 : 0);
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("port_id: ").append(this.mId).append(", ");
        s.append("address: ").append(String.format("0x%04x", Integer.valueOf(this.mAddress))).append(", ");
        s.append("cec: ").append(this.mCecSupported).append(", ");
        s.append("arc: ").append(this.mArcSupported).append(", ");
        s.append("mhl: ").append(this.mMhlSupported);
        return s.toString();
    }

    public boolean equals(Object obj) {
        return (obj instanceof HdmiPortInfo) && this.mId == obj.mId && this.mType == obj.mType && this.mAddress == obj.mAddress && this.mCecSupported == obj.mCecSupported && this.mArcSupported == obj.mArcSupported && this.mMhlSupported == obj.mMhlSupported;
    }
}
