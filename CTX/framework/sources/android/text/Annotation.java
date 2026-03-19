package android.text;

import android.os.Parcel;

public class Annotation implements ParcelableSpan {
    private final String mKey;
    private final String mValue;

    public Annotation(String str, String str2) {
        this.mKey = str;
        this.mValue = str2;
    }

    public Annotation(Parcel parcel) {
        this.mKey = parcel.readString();
        this.mValue = parcel.readString();
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 18;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeToParcelInternal(parcel, i);
    }

    @Override
    public void writeToParcelInternal(Parcel parcel, int i) {
        parcel.writeString(this.mKey);
        parcel.writeString(this.mValue);
    }

    public String getKey() {
        return this.mKey;
    }

    public String getValue() {
        return this.mValue;
    }
}
