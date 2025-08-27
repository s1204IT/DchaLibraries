package com.mediatek.keyguard.PowerOffAlarm;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;

/* loaded from: classes.dex */
public final class Alarm implements Parcelable {
    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() { // from class: com.mediatek.keyguard.PowerOffAlarm.Alarm.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Alarm createFromParcel(Parcel parcel) {
            return new Alarm(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Alarm[] newArray(int i) {
            return new Alarm[i];
        }
    };
    Uri alert;
    DaysOfWeek daysOfWeek;
    boolean enabled;
    int hour;
    int id;
    String label;
    int minutes;
    boolean silent;
    long time;
    boolean vibrate;

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.id);
        parcel.writeInt(this.enabled ? 1 : 0);
        parcel.writeInt(this.hour);
        parcel.writeInt(this.minutes);
        parcel.writeInt(this.daysOfWeek.getCoded());
        parcel.writeLong(this.time);
        parcel.writeInt(this.vibrate ? 1 : 0);
        parcel.writeString(this.label);
        parcel.writeParcelable(this.alert, i);
        parcel.writeInt(this.silent ? 1 : 0);
    }

    public String toString() {
        return "Alarm{alert=" + this.alert + ", id=" + this.id + ", enabled=" + this.enabled + ", hour=" + this.hour + ", minutes=" + this.minutes + ", daysOfWeek=" + this.daysOfWeek + ", time=" + this.time + ", vibrate=" + this.vibrate + ", label='" + this.label + "', silent=" + this.silent + '}';
    }

    public Alarm(Parcel parcel) {
        this.id = parcel.readInt();
        this.enabled = parcel.readInt() == 1;
        this.hour = parcel.readInt();
        this.minutes = parcel.readInt();
        this.daysOfWeek = new DaysOfWeek(parcel.readInt());
        this.time = parcel.readLong();
        this.vibrate = parcel.readInt() == 1;
        this.label = parcel.readString();
        this.alert = (Uri) parcel.readParcelable(null);
        this.silent = parcel.readInt() == 1;
    }

    public Alarm() {
        this.id = -1;
        this.hour = 0;
        this.minutes = 0;
        this.vibrate = true;
        this.daysOfWeek = new DaysOfWeek(0);
        this.label = "";
        this.alert = RingtoneManager.getDefaultUri(4);
    }

    public int hashCode() {
        return this.id;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Alarm) && this.id == ((Alarm) obj).id;
    }

    static final class DaysOfWeek {
        private static int[] DAY_MAP = {2, 3, 4, 5, 6, 7, 1};
        private static HashMap<Integer, Integer> DAY_TO_BIT_MASK = new HashMap<>();
        private int mDays;

        static {
            for (int i = 0; i < DAY_MAP.length; i++) {
                DAY_TO_BIT_MASK.put(Integer.valueOf(DAY_MAP[i]), Integer.valueOf(i));
            }
        }

        DaysOfWeek(int i) {
            this.mDays = i;
        }

        public int getCoded() {
            return this.mDays;
        }

        public String toString() {
            return "DaysOfWeek{mDays=" + this.mDays + '}';
        }
    }
}
