package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.content.NativeLibraryHelper;

public class NeighboringCellInfo implements Parcelable {
    public static final Parcelable.Creator<NeighboringCellInfo> CREATOR = new Parcelable.Creator<NeighboringCellInfo>() {
        @Override
        public NeighboringCellInfo createFromParcel(Parcel parcel) {
            return new NeighboringCellInfo(parcel);
        }

        @Override
        public NeighboringCellInfo[] newArray(int i) {
            return new NeighboringCellInfo[i];
        }
    };
    public static final int UNKNOWN_CID = -1;
    public static final int UNKNOWN_RSSI = 99;
    private int mCid;
    private int mLac;
    private int mNetworkType;
    private int mPsc;
    private int mRssi;

    @Deprecated
    public NeighboringCellInfo() {
        this.mRssi = 99;
        this.mLac = -1;
        this.mCid = -1;
        this.mPsc = -1;
        this.mNetworkType = 0;
    }

    @Deprecated
    public NeighboringCellInfo(int i, int i2) {
        this.mRssi = i;
        this.mCid = i2;
    }

    public NeighboringCellInfo(int r7, java.lang.String r8, int r9) {
        r6.mRssi = r7;
        r6.mNetworkType = 0;
        r6.mPsc = -1;
        r6.mLac = -1;
        r6.mCid = -1;
        r1 = r8.length();
        if (r1 > 8) {
            return;
        } else {
            if (r1 < 8) {
                r3 = r8;
                r8 = 0;
                while (r8 < 8 - r1) {
                    r4 = new java.lang.StringBuilder();
                    r4.append(android.net.wifi.WifiEnterpriseConfig.ENGINE_DISABLE);
                    r4.append(r3);
                    r3 = r4.toString();
                    r8 = r8 + 1;
                }
                r8 = r3;
            }
            try {
            } catch (java.lang.NumberFormatException e) {
                r6.mPsc = -1;
                r6.mLac = -1;
                r6.mCid = -1;
                r6.mNetworkType = 0;
            }
            switch (r9) {
                case 1:
                case 2:
                    r6.mNetworkType = r9;
                    if (!r8.equalsIgnoreCase("FFFFFFFF")) {
                        r6.mCid = java.lang.Integer.parseInt(r8.substring(4), 16);
                        r6.mLac = java.lang.Integer.parseInt(r8.substring(0, 4), 16);
                    }
                    break;
                default:
                    switch (r9) {
                    }
                case 3:
                    r6.mNetworkType = r9;
                    r6.mPsc = java.lang.Integer.parseInt(r8, 16);
            }
            return;
        }
    }

    public NeighboringCellInfo(Parcel parcel) {
        this.mRssi = parcel.readInt();
        this.mLac = parcel.readInt();
        this.mCid = parcel.readInt();
        this.mPsc = parcel.readInt();
        this.mNetworkType = parcel.readInt();
    }

    public int getRssi() {
        return this.mRssi;
    }

    public int getLac() {
        return this.mLac;
    }

    public int getCid() {
        return this.mCid;
    }

    public int getPsc() {
        return this.mPsc;
    }

    public int getNetworkType() {
        return this.mNetworkType;
    }

    @Deprecated
    public void setCid(int i) {
        this.mCid = i;
    }

    @Deprecated
    public void setRssi(int i) {
        this.mRssi = i;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (this.mPsc != -1) {
            sb.append(Integer.toHexString(this.mPsc));
            sb.append("@");
            sb.append(this.mRssi == 99 ? NativeLibraryHelper.CLEAR_ABI_OVERRIDE : Integer.valueOf(this.mRssi));
        } else if (this.mLac != -1 && this.mCid != -1) {
            sb.append(Integer.toHexString(this.mLac));
            sb.append(Integer.toHexString(this.mCid));
            sb.append("@");
            sb.append(this.mRssi == 99 ? NativeLibraryHelper.CLEAR_ABI_OVERRIDE : Integer.valueOf(this.mRssi));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRssi);
        parcel.writeInt(this.mLac);
        parcel.writeInt(this.mCid);
        parcel.writeInt(this.mPsc);
        parcel.writeInt(this.mNetworkType);
    }
}
