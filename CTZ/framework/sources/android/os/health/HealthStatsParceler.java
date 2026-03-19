package android.os.health;

import android.os.Parcel;
import android.os.Parcelable;

public class HealthStatsParceler implements Parcelable {
    public static final Parcelable.Creator<HealthStatsParceler> CREATOR = new Parcelable.Creator<HealthStatsParceler>() {
        @Override
        public HealthStatsParceler createFromParcel(Parcel parcel) {
            return new HealthStatsParceler(parcel);
        }

        @Override
        public HealthStatsParceler[] newArray(int i) {
            return new HealthStatsParceler[i];
        }
    };
    private HealthStats mHealthStats;
    private HealthStatsWriter mWriter;

    public HealthStatsParceler(HealthStatsWriter healthStatsWriter) {
        this.mWriter = healthStatsWriter;
    }

    public HealthStatsParceler(Parcel parcel) {
        this.mHealthStats = new HealthStats(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mWriter != null) {
            this.mWriter.flattenToParcel(parcel);
            return;
        }
        throw new RuntimeException("Can not re-parcel HealthStatsParceler that was constructed from a Parcel");
    }

    public HealthStats getHealthStats() {
        if (this.mWriter != null) {
            Parcel parcelObtain = Parcel.obtain();
            this.mWriter.flattenToParcel(parcelObtain);
            parcelObtain.setDataPosition(0);
            this.mHealthStats = new HealthStats(parcelObtain);
            parcelObtain.recycle();
        }
        return this.mHealthStats;
    }
}
