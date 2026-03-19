package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class SmsMemoryStatus implements Parcelable {
    public static final Parcelable.Creator<SmsMemoryStatus> CREATOR = new Parcelable.Creator<SmsMemoryStatus>() {
        @Override
        public SmsMemoryStatus createFromParcel(Parcel source) {
            int used = source.readInt();
            int total = source.readInt();
            return new SmsMemoryStatus(used, total);
        }

        @Override
        public SmsMemoryStatus[] newArray(int size) {
            return new SmsMemoryStatus[size];
        }
    };
    public int mTotal;
    public int mUsed;

    public SmsMemoryStatus() {
        this.mUsed = 0;
        this.mTotal = 0;
    }

    public SmsMemoryStatus(int used, int total) {
        this.mUsed = used;
        this.mTotal = total;
    }

    public int getUsed() {
        return this.mUsed;
    }

    public int getTotal() {
        return this.mTotal;
    }

    public int getUnused() {
        return this.mTotal - this.mUsed;
    }

    public void reset() {
        this.mUsed = 0;
        this.mTotal = 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mUsed);
        dest.writeInt(this.mTotal);
    }
}
