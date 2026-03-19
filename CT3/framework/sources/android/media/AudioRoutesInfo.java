package android.media;

import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class AudioRoutesInfo implements Parcelable {
    public static final Parcelable.Creator<AudioRoutesInfo> CREATOR = new Parcelable.Creator<AudioRoutesInfo>() {
        @Override
        public AudioRoutesInfo createFromParcel(Parcel in) {
            return new AudioRoutesInfo(in);
        }

        @Override
        public AudioRoutesInfo[] newArray(int size) {
            return new AudioRoutesInfo[size];
        }
    };
    public static final int MAIN_DOCK_SPEAKERS = 4;
    public static final int MAIN_HDMI = 8;
    public static final int MAIN_HEADPHONES = 2;
    public static final int MAIN_HEADSET = 1;
    public static final int MAIN_SPEAKER = 0;
    public static final int MAIN_USB = 16;
    public CharSequence bluetoothName;
    public int mainType;

    public AudioRoutesInfo() {
        this.mainType = 0;
    }

    public AudioRoutesInfo(AudioRoutesInfo o) {
        this.mainType = 0;
        this.bluetoothName = o.bluetoothName;
        this.mainType = o.mainType;
    }

    AudioRoutesInfo(Parcel src) {
        this.mainType = 0;
        this.bluetoothName = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(src);
        this.mainType = src.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return getClass().getSimpleName() + "{ type=" + typeToString(this.mainType) + (TextUtils.isEmpty(this.bluetoothName) ? ProxyInfo.LOCAL_EXCL_LIST : ", bluetoothName=" + this.bluetoothName) + " }";
    }

    private static String typeToString(int type) {
        return type == 0 ? "SPEAKER" : (type & 1) != 0 ? "HEADSET" : (type & 2) != 0 ? "HEADPHONES" : (type & 4) != 0 ? "DOCK_SPEAKERS" : (type & 8) != 0 ? "HDMI" : (type & 16) != 0 ? "USB" : Integer.toHexString(type);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(this.bluetoothName, dest, flags);
        dest.writeInt(this.mainType);
    }
}
