package android.text;

import android.os.Parcel;

public class Annotation implements ParcelableSpan {
    private final String mKey;
    private final String mValue;

    public Annotation(String key, String value) {
        this.mKey = key;
        this.mValue = value;
    }

    public Annotation(Parcel src) {
        this.mKey = src.readString();
        this.mValue = src.readString();
    }

    @Override
    public int getSpanTypeId() {
        return 18;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mKey);
        dest.writeString(this.mValue);
    }

    public String getKey() {
        return this.mKey;
    }

    public String getValue() {
        return this.mValue;
    }
}
