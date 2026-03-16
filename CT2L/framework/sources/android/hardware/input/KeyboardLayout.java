package android.hardware.input;

import android.os.Parcel;
import android.os.Parcelable;

public final class KeyboardLayout implements Parcelable, Comparable<KeyboardLayout> {
    public static final Parcelable.Creator<KeyboardLayout> CREATOR = new Parcelable.Creator<KeyboardLayout>() {
        @Override
        public KeyboardLayout createFromParcel(Parcel source) {
            return new KeyboardLayout(source);
        }

        @Override
        public KeyboardLayout[] newArray(int size) {
            return new KeyboardLayout[size];
        }
    };
    private final String mCollection;
    private final String mDescriptor;
    private final String mLabel;
    private final int mPriority;

    public KeyboardLayout(String descriptor, String label, String collection, int priority) {
        this.mDescriptor = descriptor;
        this.mLabel = label;
        this.mCollection = collection;
        this.mPriority = priority;
    }

    private KeyboardLayout(Parcel source) {
        this.mDescriptor = source.readString();
        this.mLabel = source.readString();
        this.mCollection = source.readString();
        this.mPriority = source.readInt();
    }

    public String getDescriptor() {
        return this.mDescriptor;
    }

    public String getLabel() {
        return this.mLabel;
    }

    public String getCollection() {
        return this.mCollection;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDescriptor);
        dest.writeString(this.mLabel);
        dest.writeString(this.mCollection);
        dest.writeInt(this.mPriority);
    }

    @Override
    public int compareTo(KeyboardLayout another) {
        int result = Integer.compare(another.mPriority, this.mPriority);
        if (result == 0) {
            result = this.mLabel.compareToIgnoreCase(another.mLabel);
        }
        if (result == 0) {
            return this.mCollection.compareToIgnoreCase(another.mCollection);
        }
        return result;
    }

    public String toString() {
        return this.mCollection.isEmpty() ? this.mLabel : this.mLabel + " - " + this.mCollection;
    }
}
