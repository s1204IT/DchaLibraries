package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

@Deprecated
public class WpsInfo implements Parcelable {

    @Deprecated
    public static final Parcelable.Creator<WpsInfo> CREATOR = new Parcelable.Creator<WpsInfo>() {
        @Override
        @Deprecated
        public WpsInfo createFromParcel(Parcel parcel) {
            WpsInfo wpsInfo = new WpsInfo();
            wpsInfo.setup = parcel.readInt();
            wpsInfo.BSSID = parcel.readString();
            wpsInfo.pin = parcel.readString();
            return wpsInfo;
        }

        @Override
        @Deprecated
        public WpsInfo[] newArray(int i) {
            return new WpsInfo[i];
        }
    };

    @Deprecated
    public static final int DISPLAY = 1;

    @Deprecated
    public static final int INVALID = 4;

    @Deprecated
    public static final int KEYPAD = 2;

    @Deprecated
    public static final int LABEL = 3;

    @Deprecated
    public static final int PBC = 0;

    @Deprecated
    public String BSSID;

    @Deprecated
    public String pin;

    @Deprecated
    public int setup;

    @Deprecated
    public WpsInfo() {
        this.setup = 4;
        this.BSSID = null;
        this.pin = null;
    }

    @Deprecated
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(" setup: ");
        stringBuffer.append(this.setup);
        stringBuffer.append('\n');
        stringBuffer.append(" BSSID: ");
        stringBuffer.append(this.BSSID);
        stringBuffer.append('\n');
        stringBuffer.append(" pin: ");
        stringBuffer.append(this.pin);
        stringBuffer.append('\n');
        return stringBuffer.toString();
    }

    @Override
    @Deprecated
    public int describeContents() {
        return 0;
    }

    @Deprecated
    public WpsInfo(WpsInfo wpsInfo) {
        if (wpsInfo != null) {
            this.setup = wpsInfo.setup;
            this.BSSID = wpsInfo.BSSID;
            this.pin = wpsInfo.pin;
        }
    }

    @Override
    @Deprecated
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.setup);
        parcel.writeString(this.BSSID);
        parcel.writeString(this.pin);
    }
}
