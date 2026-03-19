package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class WpsResult implements Parcelable {
    public static final Parcelable.Creator<WpsResult> CREATOR = new Parcelable.Creator<WpsResult>() {
        @Override
        public WpsResult createFromParcel(Parcel in) {
            WpsResult result = new WpsResult();
            result.status = Status.valueOf(in.readString());
            result.pin = in.readString();
            return result;
        }

        @Override
        public WpsResult[] newArray(int size) {
            return new WpsResult[size];
        }
    };
    public String pin;
    public Status status;

    public enum Status {
        SUCCESS,
        FAILURE,
        IN_PROGRESS;

        public static Status[] valuesCustom() {
            return values();
        }
    }

    public WpsResult() {
        this.status = Status.FAILURE;
        this.pin = null;
    }

    public WpsResult(Status s) {
        this.status = s;
        this.pin = null;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(" status: ").append(this.status.toString());
        sbuf.append('\n');
        sbuf.append(" pin: ").append(this.pin);
        sbuf.append("\n");
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WpsResult(WpsResult source) {
        if (source == null) {
            return;
        }
        this.status = source.status;
        this.pin = source.pin;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.status.name());
        dest.writeString(this.pin);
    }
}
