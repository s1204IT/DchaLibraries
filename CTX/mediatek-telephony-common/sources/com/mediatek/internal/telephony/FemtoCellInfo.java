package com.mediatek.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class FemtoCellInfo implements Parcelable {
    public static final Parcelable.Creator<FemtoCellInfo> CREATOR = new Parcelable.Creator<FemtoCellInfo>() {
        @Override
        public FemtoCellInfo createFromParcel(Parcel parcel) {
            return new FemtoCellInfo(parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt());
        }

        @Override
        public FemtoCellInfo[] newArray(int i) {
            return new FemtoCellInfo[i];
        }
    };
    public static final int CSG_ICON_TYPE_ALLOWED = 1;
    public static final int CSG_ICON_TYPE_NOT_ALLOWED = 0;
    public static final int CSG_ICON_TYPE_OPERATOR = 2;
    public static final int CSG_ICON_TYPE_OPERATOR_UNAUTHORIZED = 3;
    private int csgIconType;
    private int csgId;
    private String homeNodeBName;
    private String operatorAlphaLong;
    private String operatorNumeric;
    private int rat;

    public int getCsgId() {
        return this.csgId;
    }

    public int getCsgIconType() {
        return this.csgIconType;
    }

    public String getHomeNodeBName() {
        return this.homeNodeBName;
    }

    public int getCsgRat() {
        return this.rat;
    }

    public String getOperatorNumeric() {
        return this.operatorNumeric;
    }

    public String getOperatorAlphaLong() {
        return this.operatorAlphaLong;
    }

    public FemtoCellInfo(int i, int i2, String str, String str2, String str3, int i3) {
        this.rat = 0;
        this.csgId = i;
        this.csgIconType = i2;
        this.homeNodeBName = str;
        this.operatorNumeric = str2;
        this.operatorAlphaLong = str3;
        this.rat = i3;
    }

    public String toString() {
        return "FemtoCellInfo " + this.csgId + "/" + this.csgIconType + "/" + this.homeNodeBName + "/" + this.operatorNumeric + "/" + this.operatorAlphaLong + "/" + this.rat;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.csgId);
        parcel.writeInt(this.csgIconType);
        parcel.writeString(this.homeNodeBName);
        parcel.writeString(this.operatorNumeric);
        parcel.writeString(this.operatorAlphaLong);
        parcel.writeInt(this.rat);
    }
}
