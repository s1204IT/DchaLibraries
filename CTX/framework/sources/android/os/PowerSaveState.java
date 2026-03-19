package android.os;

import android.os.Parcelable;

public class PowerSaveState implements Parcelable {
    public static final Parcelable.Creator<PowerSaveState> CREATOR = new Parcelable.Creator<PowerSaveState>() {
        @Override
        public PowerSaveState createFromParcel(Parcel parcel) {
            return new PowerSaveState(parcel);
        }

        @Override
        public PowerSaveState[] newArray(int i) {
            return new PowerSaveState[i];
        }
    };
    public final boolean batterySaverEnabled;
    public final float brightnessFactor;
    public final boolean globalBatterySaverEnabled;
    public final int gpsMode;

    public PowerSaveState(Builder builder) {
        this.batterySaverEnabled = builder.mBatterySaverEnabled;
        this.gpsMode = builder.mGpsMode;
        this.brightnessFactor = builder.mBrightnessFactor;
        this.globalBatterySaverEnabled = builder.mGlobalBatterySaverEnabled;
    }

    public PowerSaveState(Parcel parcel) {
        this.batterySaverEnabled = parcel.readByte() != 0;
        this.globalBatterySaverEnabled = parcel.readByte() != 0;
        this.gpsMode = parcel.readInt();
        this.brightnessFactor = parcel.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte(this.batterySaverEnabled ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.globalBatterySaverEnabled ? (byte) 1 : (byte) 0);
        parcel.writeInt(this.gpsMode);
        parcel.writeFloat(this.brightnessFactor);
    }

    public static final class Builder {
        private boolean mBatterySaverEnabled = false;
        private boolean mGlobalBatterySaverEnabled = false;
        private int mGpsMode = 0;
        private float mBrightnessFactor = 0.5f;

        public Builder setBatterySaverEnabled(boolean z) {
            this.mBatterySaverEnabled = z;
            return this;
        }

        public Builder setGlobalBatterySaverEnabled(boolean z) {
            this.mGlobalBatterySaverEnabled = z;
            return this;
        }

        public Builder setGpsMode(int i) {
            this.mGpsMode = i;
            return this;
        }

        public Builder setBrightnessFactor(float f) {
            this.mBrightnessFactor = f;
            return this;
        }

        public PowerSaveState build() {
            return new PowerSaveState(this);
        }
    }
}
