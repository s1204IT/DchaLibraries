package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class RadioAccessFamily implements Parcelable {
    public static final Parcelable.Creator<RadioAccessFamily> CREATOR = new Parcelable.Creator<RadioAccessFamily>() {
        @Override
        public RadioAccessFamily createFromParcel(Parcel in) {
            int phoneId = in.readInt();
            int radioAccessFamily = in.readInt();
            return new RadioAccessFamily(phoneId, radioAccessFamily);
        }

        @Override
        public RadioAccessFamily[] newArray(int size) {
            return new RadioAccessFamily[size];
        }
    };
    public static final int RAF_1xRTT = 64;
    public static final int RAF_EDGE = 4;
    public static final int RAF_EHRPD = 8192;
    public static final int RAF_EVDO_0 = 128;
    public static final int RAF_EVDO_A = 256;
    public static final int RAF_EVDO_B = 4096;
    public static final int RAF_GPRS = 2;
    public static final int RAF_GSM = 65536;
    public static final int RAF_HSDPA = 512;
    public static final int RAF_HSPA = 2048;
    public static final int RAF_HSPAP = 32768;
    public static final int RAF_HSUPA = 1024;
    public static final int RAF_IS95A = 16;
    public static final int RAF_IS95B = 32;
    public static final int RAF_LTE = 16384;
    public static final int RAF_TD_SCDMA = 131072;
    public static final int RAF_UMTS = 8;
    public static final int RAF_UNKNOWN = 1;
    private int mPhoneId;
    private int mRadioAccessFamily;

    public RadioAccessFamily(int phoneId, int radioAccessFamily) {
        this.mPhoneId = phoneId;
        this.mRadioAccessFamily = radioAccessFamily;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public int getRadioAccessFamily() {
        return this.mRadioAccessFamily;
    }

    public String toString() {
        String ret = "{ mPhoneId = " + this.mPhoneId + ", mRadioAccessFamily = " + this.mRadioAccessFamily + "}";
        return ret;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel outParcel, int flags) {
        outParcel.writeInt(this.mPhoneId);
        outParcel.writeInt(this.mRadioAccessFamily);
    }

    public static int getRafFromNetworkType(int type) {
        switch (type) {
            case 0:
                return 101902;
            case 1:
                return 65542;
            case 2:
                return 36360;
            case 3:
                return 101902;
            case 4:
                return 112;
            case 5:
                return 112;
            case 6:
                return 4480;
            case 7:
                return 106494;
            case 8:
                return 20976;
            case 9:
                return 118286;
            case 10:
                return 122878;
            case 11:
                return 16384;
            case 12:
                return 52744;
            default:
                return 1;
        }
    }
}
