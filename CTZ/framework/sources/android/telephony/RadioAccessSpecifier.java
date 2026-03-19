package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class RadioAccessSpecifier implements Parcelable {
    public static final Parcelable.Creator<RadioAccessSpecifier> CREATOR = new Parcelable.Creator<RadioAccessSpecifier>() {
        @Override
        public RadioAccessSpecifier createFromParcel(Parcel parcel) {
            return new RadioAccessSpecifier(parcel);
        }

        @Override
        public RadioAccessSpecifier[] newArray(int i) {
            return new RadioAccessSpecifier[i];
        }
    };
    private int[] mBands;
    private int[] mChannels;
    private int mRadioAccessNetwork;

    public RadioAccessSpecifier(int i, int[] iArr, int[] iArr2) {
        this.mRadioAccessNetwork = i;
        if (iArr != null) {
            this.mBands = (int[]) iArr.clone();
        } else {
            this.mBands = null;
        }
        if (iArr2 != null) {
            this.mChannels = (int[]) iArr2.clone();
        } else {
            this.mChannels = null;
        }
    }

    public int getRadioAccessNetwork() {
        return this.mRadioAccessNetwork;
    }

    public int[] getBands() {
        if (this.mBands == null) {
            return null;
        }
        return (int[]) this.mBands.clone();
    }

    public int[] getChannels() {
        if (this.mChannels == null) {
            return null;
        }
        return (int[]) this.mChannels.clone();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRadioAccessNetwork);
        parcel.writeIntArray(this.mBands);
        parcel.writeIntArray(this.mChannels);
    }

    private RadioAccessSpecifier(Parcel parcel) {
        this.mRadioAccessNetwork = parcel.readInt();
        this.mBands = parcel.createIntArray();
        this.mChannels = parcel.createIntArray();
    }

    public boolean equals(Object obj) {
        try {
            RadioAccessSpecifier radioAccessSpecifier = (RadioAccessSpecifier) obj;
            return obj != null && this.mRadioAccessNetwork == radioAccessSpecifier.mRadioAccessNetwork && Arrays.equals(this.mBands, radioAccessSpecifier.mBands) && Arrays.equals(this.mChannels, radioAccessSpecifier.mChannels);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return (this.mRadioAccessNetwork * 31) + (Arrays.hashCode(this.mBands) * 37) + (Arrays.hashCode(this.mChannels) * 39);
    }
}
