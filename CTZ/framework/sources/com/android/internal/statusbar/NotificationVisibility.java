package com.android.internal.statusbar;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayDeque;

public class NotificationVisibility implements Parcelable {
    private static final int MAX_POOL_SIZE = 25;
    private static final String TAG = "NoViz";
    public int count;
    int id;
    public String key;
    public int rank;
    public boolean visible;
    private static ArrayDeque<NotificationVisibility> sPool = new ArrayDeque<>(25);
    private static int sNexrId = 0;
    public static final Parcelable.Creator<NotificationVisibility> CREATOR = new Parcelable.Creator<NotificationVisibility>() {
        @Override
        public NotificationVisibility createFromParcel(Parcel parcel) {
            return NotificationVisibility.obtain(parcel);
        }

        @Override
        public NotificationVisibility[] newArray(int i) {
            return new NotificationVisibility[i];
        }
    };

    private NotificationVisibility() {
        this.visible = true;
        int i = sNexrId;
        sNexrId = i + 1;
        this.id = i;
    }

    private NotificationVisibility(String str, int i, int i2, boolean z) {
        this();
        this.key = str;
        this.rank = i;
        this.count = i2;
        this.visible = z;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NotificationVisibility(id=");
        sb.append(this.id);
        sb.append(" key=");
        sb.append(this.key);
        sb.append(" rank=");
        sb.append(this.rank);
        sb.append(" count=");
        sb.append(this.count);
        sb.append(this.visible ? " visible" : "");
        sb.append(" )");
        return sb.toString();
    }

    public NotificationVisibility m46clone() {
        return obtain(this.key, this.rank, this.count, this.visible);
    }

    public int hashCode() {
        if (this.key == null) {
            return 0;
        }
        return this.key.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NotificationVisibility)) {
            return false;
        }
        NotificationVisibility notificationVisibility = (NotificationVisibility) obj;
        return (this.key == null && notificationVisibility.key == null) || this.key.equals(notificationVisibility.key);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.key);
        parcel.writeInt(this.rank);
        parcel.writeInt(this.count);
        parcel.writeInt(this.visible ? 1 : 0);
    }

    private void readFromParcel(Parcel parcel) {
        this.key = parcel.readString();
        this.rank = parcel.readInt();
        this.count = parcel.readInt();
        this.visible = parcel.readInt() != 0;
    }

    public static NotificationVisibility obtain(String str, int i, int i2, boolean z) {
        NotificationVisibility notificationVisibilityObtain = obtain();
        notificationVisibilityObtain.key = str;
        notificationVisibilityObtain.rank = i;
        notificationVisibilityObtain.count = i2;
        notificationVisibilityObtain.visible = z;
        return notificationVisibilityObtain;
    }

    private static NotificationVisibility obtain(Parcel parcel) {
        NotificationVisibility notificationVisibilityObtain = obtain();
        notificationVisibilityObtain.readFromParcel(parcel);
        return notificationVisibilityObtain;
    }

    private static NotificationVisibility obtain() {
        synchronized (sPool) {
            if (!sPool.isEmpty()) {
                return sPool.poll();
            }
            return new NotificationVisibility();
        }
    }

    public void recycle() {
        if (this.key == null) {
            return;
        }
        this.key = null;
        if (sPool.size() < 25) {
            synchronized (sPool) {
                sPool.offer(this);
            }
        }
    }
}
