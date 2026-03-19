package com.mediatek.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class FemtoCellInfo implements Parcelable {
    public static final Parcelable.Creator<FemtoCellInfo> CREATOR = new Parcelable.Creator<FemtoCellInfo>() {
        @Override
        public FemtoCellInfo createFromParcel(Parcel in) {
            FemtoCellInfo femtoCellInfo = new FemtoCellInfo(in.readInt(), in.readInt(), in.readString(), in.readString(), in.readString(), in.readInt());
            return femtoCellInfo;
        }

        @Override
        public FemtoCellInfo[] newArray(int size) {
            return new FemtoCellInfo[size];
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

    public FemtoCellInfo(int csgId, int csgIconType, String homeNodeBName, String operatorNumeric, String operatorAlphaLong, int rat) {
        this.rat = 0;
        this.csgId = csgId;
        this.csgIconType = csgIconType;
        this.homeNodeBName = homeNodeBName;
        this.operatorNumeric = operatorNumeric;
        this.operatorAlphaLong = operatorAlphaLong;
        this.rat = rat;
    }

    public String toString() {
        return "FemtoCellInfo " + this.csgId + "/" + this.csgIconType + "/" + this.homeNodeBName + "/" + this.operatorNumeric + "/" + this.operatorAlphaLong + "/" + this.rat;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.csgId);
        dest.writeInt(this.csgIconType);
        dest.writeString(this.homeNodeBName);
        dest.writeString(this.operatorNumeric);
        dest.writeString(this.operatorAlphaLong);
        dest.writeInt(this.rat);
    }
}
