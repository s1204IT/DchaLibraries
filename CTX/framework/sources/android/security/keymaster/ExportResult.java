package android.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;

public class ExportResult implements Parcelable {
    public static final Parcelable.Creator<ExportResult> CREATOR = new Parcelable.Creator<ExportResult>() {
        @Override
        public ExportResult createFromParcel(Parcel parcel) {
            return new ExportResult(parcel);
        }

        @Override
        public ExportResult[] newArray(int i) {
            return new ExportResult[i];
        }
    };
    public final byte[] exportData;
    public final int resultCode;

    protected ExportResult(Parcel parcel) {
        this.resultCode = parcel.readInt();
        this.exportData = parcel.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.resultCode);
        parcel.writeByteArray(this.exportData);
    }
}
