package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public final class ConnectivityMetricsEvent implements Parcelable {
    public static final Parcelable.Creator<ConnectivityMetricsEvent> CREATOR = new Parcelable.Creator<ConnectivityMetricsEvent>() {
        @Override
        public ConnectivityMetricsEvent createFromParcel(Parcel source) {
            long timestamp = source.readLong();
            int componentTag = source.readInt();
            int eventTag = source.readInt();
            Parcelable data = source.readParcelable(null);
            return new ConnectivityMetricsEvent(timestamp, componentTag, eventTag, data);
        }

        @Override
        public ConnectivityMetricsEvent[] newArray(int size) {
            return new ConnectivityMetricsEvent[size];
        }
    };
    public final int componentTag;
    public final Parcelable data;
    public final int eventTag;
    public final long timestamp;

    public ConnectivityMetricsEvent(long timestamp, int componentTag, int eventTag, Parcelable data) {
        this.timestamp = timestamp;
        this.componentTag = componentTag;
        this.eventTag = eventTag;
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.timestamp);
        dest.writeInt(this.componentTag);
        dest.writeInt(this.eventTag);
        dest.writeParcelable(this.data, 0);
    }

    public String toString() {
        return String.format("ConnectivityMetricsEvent(%tT.%tL, %d, %d): %s", Long.valueOf(this.timestamp), Long.valueOf(this.timestamp), Integer.valueOf(this.componentTag), Integer.valueOf(this.eventTag), this.data);
    }

    public static final class Reference implements Parcelable {
        public static final Parcelable.Creator<Reference> CREATOR = new Parcelable.Creator<Reference>() {
            @Override
            public Reference createFromParcel(Parcel source) {
                return new Reference(source.readLong());
            }

            @Override
            public Reference[] newArray(int size) {
                return new Reference[size];
            }
        };
        private long mValue;

        public Reference(long ref) {
            this.mValue = ref;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.mValue);
        }

        public void readFromParcel(Parcel in) {
            this.mValue = in.readLong();
        }

        public long getValue() {
            return this.mValue;
        }

        public void setValue(long val) {
            this.mValue = val;
        }
    }
}
