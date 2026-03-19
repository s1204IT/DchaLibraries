package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class PcoData implements Parcelable {
    public static final Parcelable.Creator<PcoData> CREATOR = new Parcelable.Creator() {
        @Override
        public PcoData createFromParcel(Parcel parcel) {
            return new PcoData(parcel);
        }

        @Override
        public PcoData[] newArray(int i) {
            return new PcoData[i];
        }
    };
    public final String bearerProto;
    public final int cid;
    public final byte[] contents;
    public final int pcoId;

    public PcoData(int i, String str, int i2, byte[] bArr) {
        this.cid = i;
        this.bearerProto = str;
        this.pcoId = i2;
        this.contents = bArr;
    }

    public PcoData(Parcel parcel) {
        this.cid = parcel.readInt();
        this.bearerProto = parcel.readString();
        this.pcoId = parcel.readInt();
        this.contents = parcel.createByteArray();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.cid);
        parcel.writeString(this.bearerProto);
        parcel.writeInt(this.pcoId);
        parcel.writeByteArray(this.contents);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "PcoData(" + this.cid + ", " + this.bearerProto + ", " + this.pcoId + ", contents[" + this.contents.length + "])";
    }
}
