package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class AudioRoutesInfo implements Parcelable {
    public static final Parcelable.Creator<AudioRoutesInfo> CREATOR = new Parcelable.Creator<AudioRoutesInfo>() {
        @Override
        public AudioRoutesInfo createFromParcel(Parcel parcel) {
            return new AudioRoutesInfo(parcel);
        }

        @Override
        public AudioRoutesInfo[] newArray(int i) {
            return new AudioRoutesInfo[i];
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

    public AudioRoutesInfo(AudioRoutesInfo audioRoutesInfo) {
        this.mainType = 0;
        this.bluetoothName = audioRoutesInfo.bluetoothName;
        this.mainType = audioRoutesInfo.mainType;
    }

    AudioRoutesInfo(Parcel parcel) {
        this.mainType = 0;
        this.bluetoothName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.mainType = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{ type=");
        sb.append(typeToString(this.mainType));
        if (TextUtils.isEmpty(this.bluetoothName)) {
            str = "";
        } else {
            str = ", bluetoothName=" + ((Object) this.bluetoothName);
        }
        sb.append(str);
        sb.append(" }");
        return sb.toString();
    }

    private static String typeToString(int i) {
        return i == 0 ? "SPEAKER" : (i & 1) != 0 ? "HEADSET" : (i & 2) != 0 ? "HEADPHONES" : (i & 4) != 0 ? "DOCK_SPEAKERS" : (i & 8) != 0 ? "HDMI" : (i & 16) != 0 ? "USB" : Integer.toHexString(i);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        TextUtils.writeToParcel(this.bluetoothName, parcel, i);
        parcel.writeInt(this.mainType);
    }
}
