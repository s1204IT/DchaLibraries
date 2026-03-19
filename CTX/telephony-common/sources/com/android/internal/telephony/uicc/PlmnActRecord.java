package com.android.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.Rlog;
import java.util.Arrays;

public class PlmnActRecord implements Parcelable {
    public static final int ACCESS_TECH_CDMA2000_1XRTT = 16;
    public static final int ACCESS_TECH_CDMA2000_HRPD = 32;
    public static final int ACCESS_TECH_EUTRAN = 16384;
    public static final int ACCESS_TECH_GSM = 128;
    public static final int ACCESS_TECH_GSM_COMPACT = 64;
    public static final int ACCESS_TECH_RESERVED = 16143;
    public static final int ACCESS_TECH_UTRAN = 32768;
    public static final Parcelable.Creator<PlmnActRecord> CREATOR = new Parcelable.Creator<PlmnActRecord>() {
        @Override
        public PlmnActRecord createFromParcel(Parcel parcel) {
            return new PlmnActRecord(parcel.readString(), parcel.readInt());
        }

        @Override
        public PlmnActRecord[] newArray(int i) {
            return new PlmnActRecord[i];
        }
    };
    public static final int ENCODED_LENGTH = 5;
    private static final String LOG_TAG = "PlmnActRecord";
    private static final boolean VDBG = false;
    public final int accessTechs;
    public final String plmn;

    public PlmnActRecord(byte[] bArr, int i) {
        this.plmn = IccUtils.bcdPlmnToString(bArr, i);
        this.accessTechs = Byte.toUnsignedInt(bArr[i + 4]) | (Byte.toUnsignedInt(bArr[i + 3]) << 8);
    }

    private PlmnActRecord(String str, int i) {
        this.plmn = str;
        this.accessTechs = i;
    }

    private String accessTechString() {
        if (this.accessTechs == 0) {
            return "NONE";
        }
        StringBuilder sb = new StringBuilder();
        if ((this.accessTechs & 32768) != 0) {
            sb.append("UTRAN|");
        }
        if ((this.accessTechs & 16384) != 0) {
            sb.append("EUTRAN|");
        }
        if ((this.accessTechs & 128) != 0) {
            sb.append("GSM|");
        }
        if ((this.accessTechs & 64) != 0) {
            sb.append("GSM_COMPACT|");
        }
        if ((this.accessTechs & 32) != 0) {
            sb.append("CDMA2000_HRPD|");
        }
        if ((this.accessTechs & 16) != 0) {
            sb.append("CDMA2000_1XRTT|");
        }
        if ((this.accessTechs & ACCESS_TECH_RESERVED) != 0) {
            sb.append(String.format("UNKNOWN:%x|", Integer.valueOf(this.accessTechs & ACCESS_TECH_RESERVED)));
        }
        return sb.substring(0, sb.length() - 1);
    }

    public String toString() {
        return String.format("{PLMN=%s,AccessTechs=%s}", this.plmn, accessTechString());
    }

    public static PlmnActRecord[] getRecords(byte[] bArr) {
        if (bArr == null || bArr.length == 0 || bArr.length % 5 != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Malformed PlmnActRecord, bytes: ");
            sb.append(bArr != null ? Arrays.toString(bArr) : null);
            Rlog.e(LOG_TAG, sb.toString());
            return null;
        }
        int length = bArr.length / 5;
        PlmnActRecord[] plmnActRecordArr = new PlmnActRecord[length];
        for (int i = 0; i < length; i++) {
            plmnActRecordArr[i] = new PlmnActRecord(bArr, i * 5);
        }
        return plmnActRecordArr;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.plmn);
        parcel.writeInt(this.accessTechs);
    }
}
