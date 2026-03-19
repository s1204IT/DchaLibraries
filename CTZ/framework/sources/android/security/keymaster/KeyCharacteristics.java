package android.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KeyCharacteristics implements Parcelable {
    public static final Parcelable.Creator<KeyCharacteristics> CREATOR = new Parcelable.Creator<KeyCharacteristics>() {
        @Override
        public KeyCharacteristics createFromParcel(Parcel parcel) {
            return new KeyCharacteristics(parcel);
        }

        @Override
        public KeyCharacteristics[] newArray(int i) {
            return new KeyCharacteristics[i];
        }
    };
    public KeymasterArguments hwEnforced;
    public KeymasterArguments swEnforced;

    public KeyCharacteristics() {
    }

    protected KeyCharacteristics(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.swEnforced.writeToParcel(parcel, i);
        this.hwEnforced.writeToParcel(parcel, i);
    }

    public void readFromParcel(Parcel parcel) {
        this.swEnforced = KeymasterArguments.CREATOR.createFromParcel(parcel);
        this.hwEnforced = KeymasterArguments.CREATOR.createFromParcel(parcel);
    }

    public Integer getEnum(int i) {
        if (this.hwEnforced.containsTag(i)) {
            return Integer.valueOf(this.hwEnforced.getEnum(i, -1));
        }
        if (this.swEnforced.containsTag(i)) {
            return Integer.valueOf(this.swEnforced.getEnum(i, -1));
        }
        return null;
    }

    public List<Integer> getEnums(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.hwEnforced.getEnums(i));
        arrayList.addAll(this.swEnforced.getEnums(i));
        return arrayList;
    }

    public long getUnsignedInt(int i, long j) {
        if (this.hwEnforced.containsTag(i)) {
            return this.hwEnforced.getUnsignedInt(i, j);
        }
        return this.swEnforced.getUnsignedInt(i, j);
    }

    public List<BigInteger> getUnsignedLongs(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.hwEnforced.getUnsignedLongs(i));
        arrayList.addAll(this.swEnforced.getUnsignedLongs(i));
        return arrayList;
    }

    public Date getDate(int i) {
        Date date = this.swEnforced.getDate(i, null);
        if (date != null) {
            return date;
        }
        return this.hwEnforced.getDate(i, null);
    }

    public boolean getBoolean(int i) {
        if (this.hwEnforced.containsTag(i)) {
            return this.hwEnforced.getBoolean(i);
        }
        return this.swEnforced.getBoolean(i);
    }
}
