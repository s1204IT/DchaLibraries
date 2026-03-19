package android.service.notification;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import java.util.Objects;

public final class NotifyingApp implements Parcelable, Comparable<NotifyingApp> {
    public static final Parcelable.Creator<NotifyingApp> CREATOR = new Parcelable.Creator<NotifyingApp>() {
        @Override
        public NotifyingApp createFromParcel(Parcel parcel) {
            return new NotifyingApp(parcel);
        }

        @Override
        public NotifyingApp[] newArray(int i) {
            return new NotifyingApp[i];
        }
    };
    private long mLastNotified;
    private String mPkg;
    private int mUid;

    public NotifyingApp() {
    }

    protected NotifyingApp(Parcel parcel) {
        this.mUid = parcel.readInt();
        this.mPkg = parcel.readString();
        this.mLastNotified = parcel.readLong();
    }

    public int getUid() {
        return this.mUid;
    }

    public NotifyingApp setUid(int i) {
        this.mUid = i;
        return this;
    }

    public String getPackage() {
        return this.mPkg;
    }

    public NotifyingApp setPackage(String str) {
        this.mPkg = str;
        return this;
    }

    public long getLastNotified() {
        return this.mLastNotified;
    }

    public NotifyingApp setLastNotified(long j) {
        this.mLastNotified = j;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mUid);
        parcel.writeString(this.mPkg);
        parcel.writeLong(this.mLastNotified);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NotifyingApp notifyingApp = (NotifyingApp) obj;
        if (getUid() == notifyingApp.getUid() && getLastNotified() == notifyingApp.getLastNotified() && Objects.equals(this.mPkg, notifyingApp.mPkg)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(getUid()), this.mPkg, Long.valueOf(getLastNotified()));
    }

    @Override
    public int compareTo(NotifyingApp notifyingApp) {
        if (getLastNotified() == notifyingApp.getLastNotified()) {
            if (getUid() == notifyingApp.getUid()) {
                return getPackage().compareTo(notifyingApp.getPackage());
            }
            return Integer.compare(getUid(), notifyingApp.getUid());
        }
        return -Long.compare(getLastNotified(), notifyingApp.getLastNotified());
    }

    public String toString() {
        return "NotifyingApp{mUid=" + this.mUid + ", mPkg='" + this.mPkg + DateFormat.QUOTE + ", mLastNotified=" + this.mLastNotified + '}';
    }
}
