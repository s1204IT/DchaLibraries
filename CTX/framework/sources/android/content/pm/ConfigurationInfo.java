package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class ConfigurationInfo implements Parcelable {
    public static final Parcelable.Creator<ConfigurationInfo> CREATOR = new Parcelable.Creator<ConfigurationInfo>() {
        @Override
        public ConfigurationInfo createFromParcel(Parcel parcel) {
            return new ConfigurationInfo(parcel);
        }

        @Override
        public ConfigurationInfo[] newArray(int i) {
            return new ConfigurationInfo[i];
        }
    };
    public static final int GL_ES_VERSION_UNDEFINED = 0;
    public static final int INPUT_FEATURE_FIVE_WAY_NAV = 2;
    public static final int INPUT_FEATURE_HARD_KEYBOARD = 1;
    public int reqGlEsVersion;
    public int reqInputFeatures;
    public int reqKeyboardType;
    public int reqNavigation;
    public int reqTouchScreen;

    public ConfigurationInfo() {
        this.reqInputFeatures = 0;
    }

    public ConfigurationInfo(ConfigurationInfo configurationInfo) {
        this.reqInputFeatures = 0;
        this.reqTouchScreen = configurationInfo.reqTouchScreen;
        this.reqKeyboardType = configurationInfo.reqKeyboardType;
        this.reqNavigation = configurationInfo.reqNavigation;
        this.reqInputFeatures = configurationInfo.reqInputFeatures;
        this.reqGlEsVersion = configurationInfo.reqGlEsVersion;
    }

    public String toString() {
        return "ConfigurationInfo{" + Integer.toHexString(System.identityHashCode(this)) + " touchscreen = " + this.reqTouchScreen + " inputMethod = " + this.reqKeyboardType + " navigation = " + this.reqNavigation + " reqInputFeatures = " + this.reqInputFeatures + " reqGlEsVersion = " + this.reqGlEsVersion + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.reqTouchScreen);
        parcel.writeInt(this.reqKeyboardType);
        parcel.writeInt(this.reqNavigation);
        parcel.writeInt(this.reqInputFeatures);
        parcel.writeInt(this.reqGlEsVersion);
    }

    private ConfigurationInfo(Parcel parcel) {
        this.reqInputFeatures = 0;
        this.reqTouchScreen = parcel.readInt();
        this.reqKeyboardType = parcel.readInt();
        this.reqNavigation = parcel.readInt();
        this.reqInputFeatures = parcel.readInt();
        this.reqGlEsVersion = parcel.readInt();
    }

    public String getGlEsVersion() {
        return String.valueOf((this.reqGlEsVersion & (-65536)) >> 16) + "." + String.valueOf(this.reqGlEsVersion & 65535);
    }
}
