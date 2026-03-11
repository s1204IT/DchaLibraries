package com.mediatek.keyguard.PowerOffAlarm;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;

public final class Alarm implements Parcelable {
    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel p) {
            return new Alarm(p);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(this.id);
        p.writeInt(this.enabled ? 1 : 0);
        p.writeInt(this.hour);
        p.writeInt(this.minutes);
        p.writeInt(this.daysOfWeek.getCoded());
        p.writeLong(this.time);
        p.writeInt(this.vibrate ? 1 : 0);
        p.writeString(this.label);
        p.writeParcelable(this.alert, flags);
        p.writeInt(this.silent ? 1 : 0);
    }

    public String toString() {
        return "Alarm{alert=" + this.alert + ", id=" + this.id + ", enabled=" + this.enabled + ", hour=" + this.hour + ", minutes=" + this.minutes + ", daysOfWeek=" + this.daysOfWeek + ", time=" + this.time + ", vibrate=" + this.vibrate + ", label='" + this.label + "', silent=" + this.silent + '}';
    }

    public Alarm(Parcel p) {
        this.id = p.readInt();
        this.enabled = p.readInt() == 1;
        this.hour = p.readInt();
        this.minutes = p.readInt();
        this.daysOfWeek = new DaysOfWeek(p.readInt());
        this.time = p.readLong();
        this.vibrate = p.readInt() == 1;
        this.label = p.readString();
        this.alert = (Uri) p.readParcelable(null);
        this.silent = p.readInt() == 1;
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

    public boolean equals(Object o) {
        if (!(o instanceof Alarm)) {
            return false;
        }
        Alarm other = (Alarm) o;
        return this.id == other.id;
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

        DaysOfWeek(int days) {
            this.mDays = days;
        }

        public int getCoded() {
            return this.mDays;
        }

        public String toString() {
            return "DaysOfWeek{mDays=" + this.mDays + '}';
        }
    }
}
