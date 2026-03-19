package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.ContactsContract;

public final class UidRange implements Parcelable {
    public static final Parcelable.Creator<UidRange> CREATOR = new Parcelable.Creator<UidRange>() {
        @Override
        public UidRange createFromParcel(Parcel in) {
            int start = in.readInt();
            int stop = in.readInt();
            return new UidRange(start, stop);
        }

        @Override
        public UidRange[] newArray(int size) {
            return new UidRange[size];
        }
    };
    public final int start;
    public final int stop;

    public UidRange(int startUid, int stopUid) {
        if (startUid < 0) {
            throw new IllegalArgumentException("Invalid start UID.");
        }
        if (stopUid < 0) {
            throw new IllegalArgumentException("Invalid stop UID.");
        }
        if (startUid > stopUid) {
            throw new IllegalArgumentException("Invalid UID range.");
        }
        this.start = startUid;
        this.stop = stopUid;
    }

    public static UidRange createForUser(int userId) {
        return new UidRange(userId * UserHandle.PER_USER_RANGE, ((userId + 1) * UserHandle.PER_USER_RANGE) - 1);
    }

    public int getStartUser() {
        return this.start / UserHandle.PER_USER_RANGE;
    }

    public boolean contains(int uid) {
        return this.start <= uid && uid <= this.stop;
    }

    public boolean containsRange(UidRange other) {
        return this.start <= other.start && other.stop <= this.stop;
    }

    public int hashCode() {
        int result = this.start + 527;
        return (result * 31) + this.stop;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UidRange)) {
            return false;
        }
        UidRange other = (UidRange) o;
        return this.start == other.start && this.stop == other.stop;
    }

    public String toString() {
        return this.start + ContactsContract.Aas.ENCODE_SYMBOL + this.stop;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.start);
        dest.writeInt(this.stop);
    }
}
