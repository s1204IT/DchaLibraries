package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;

public final class ApfStats implements Parcelable {
    public static final Parcelable.Creator<ApfStats> CREATOR = new Parcelable.Creator<ApfStats>() {
        @Override
        public ApfStats createFromParcel(Parcel parcel) {
            return new ApfStats(parcel);
        }

        @Override
        public ApfStats[] newArray(int i) {
            return new ApfStats[i];
        }
    };
    public int droppedRas;
    public long durationMs;
    public int matchingRas;
    public int maxProgramSize;
    public int parseErrors;
    public int programUpdates;
    public int programUpdatesAll;
    public int programUpdatesAllowingMulticast;
    public int receivedRas;
    public int zeroLifetimeRas;

    public ApfStats() {
    }

    private ApfStats(Parcel parcel) {
        this.durationMs = parcel.readLong();
        this.receivedRas = parcel.readInt();
        this.matchingRas = parcel.readInt();
        this.droppedRas = parcel.readInt();
        this.zeroLifetimeRas = parcel.readInt();
        this.parseErrors = parcel.readInt();
        this.programUpdates = parcel.readInt();
        this.programUpdatesAll = parcel.readInt();
        this.programUpdatesAllowingMulticast = parcel.readInt();
        this.maxProgramSize = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.durationMs);
        parcel.writeInt(this.receivedRas);
        parcel.writeInt(this.matchingRas);
        parcel.writeInt(this.droppedRas);
        parcel.writeInt(this.zeroLifetimeRas);
        parcel.writeInt(this.parseErrors);
        parcel.writeInt(this.programUpdates);
        parcel.writeInt(this.programUpdatesAll);
        parcel.writeInt(this.programUpdatesAllowingMulticast);
        parcel.writeInt(this.maxProgramSize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "ApfStats(" + String.format("%dms ", Long.valueOf(this.durationMs)) + String.format("%dB RA: {", Integer.valueOf(this.maxProgramSize)) + String.format("%d received, ", Integer.valueOf(this.receivedRas)) + String.format("%d matching, ", Integer.valueOf(this.matchingRas)) + String.format("%d dropped, ", Integer.valueOf(this.droppedRas)) + String.format("%d zero lifetime, ", Integer.valueOf(this.zeroLifetimeRas)) + String.format("%d parse errors}, ", Integer.valueOf(this.parseErrors)) + String.format("updates: {all: %d, RAs: %d, allow multicast: %d})", Integer.valueOf(this.programUpdatesAll), Integer.valueOf(this.programUpdates), Integer.valueOf(this.programUpdatesAllowingMulticast));
    }
}
