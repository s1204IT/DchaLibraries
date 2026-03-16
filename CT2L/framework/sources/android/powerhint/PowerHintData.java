package android.powerhint;

import android.os.Parcel;
import android.os.Parcelable;

public final class PowerHintData implements Parcelable {
    protected int mDuration;
    protected String mHint;
    protected Parcel mHintStateData;
    protected double mTimerId;
    static PowerHintData EMPTY_POWER_HINT_DATA = new PowerHintData();
    public static final Parcelable.Creator<PowerHintData> CREATOR = new Parcelable.Creator<PowerHintData>() {
        @Override
        public PowerHintData createFromParcel(Parcel in) {
            return PowerHintData.EMPTY_POWER_HINT_DATA;
        }

        @Override
        public PowerHintData[] newArray(int size) {
            return new PowerHintData[size];
        }
    };

    private PowerHintData() {
    }

    public PowerHintData(String hint) {
        this.mHint = hint;
        this.mDuration = -1;
        this.mTimerId = 0.0d;
        this.mHintStateData = null;
    }

    public PowerHintData(String hint, double timerId, int duration) {
        this.mHint = hint;
        this.mDuration = duration;
        this.mTimerId = timerId;
        this.mHintStateData = null;
    }

    public PowerHintData(String hint, double timerId, int duration, Parcelable data) {
        this.mHint = hint;
        this.mDuration = duration;
        this.mTimerId = timerId;
        this.mHintStateData = Parcel.obtain();
        int pos = this.mHintStateData.dataPosition();
        data.writeToParcel(this.mHintStateData, 1);
        this.mHintStateData.setDataPosition(pos);
    }

    public PowerHintData(String hint, double timerId, int duration, Parcel data) {
        this.mHint = hint;
        this.mDuration = duration;
        this.mTimerId = timerId;
        this.mHintStateData = Parcel.obtain();
        int offset = data.dataPosition();
        int length = data.dataSize();
        int pos = this.mHintStateData.dataPosition();
        this.mHintStateData.appendFrom(data, offset, length);
        this.mHintStateData.setDataPosition(pos);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mHint);
        dest.writeDouble(this.mTimerId);
        dest.writeInt(this.mDuration);
        if (this.mHintStateData != null) {
            int offset = this.mHintStateData.dataPosition();
            int length = this.mHintStateData.dataSize();
            dest.appendFrom(this.mHintStateData, offset, length);
        }
    }
}
