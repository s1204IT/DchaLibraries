package com.mediatek.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

public class PseudoCellInfo implements Parcelable {
    public static final Parcelable.Creator<PseudoCellInfo> CREATOR = new Parcelable.Creator<PseudoCellInfo>() {
        @Override
        public PseudoCellInfo createFromParcel(Parcel parcel) {
            return new PseudoCellInfo(parcel);
        }

        @Override
        public PseudoCellInfo[] newArray(int i) {
            return new PseudoCellInfo[i];
        }
    };
    public int mApcMode;
    public boolean mApcReport;
    public int mCellCount;
    public ArrayList<CellInfo> mCellInfos;
    public int mReportInterval;

    class CellInfo {
        public int arfcn;
        public int bsic;
        public int cid;
        public int lac;
        public int plmn;
        public int type;

        CellInfo() {
        }
    }

    protected PseudoCellInfo() {
        this.mApcMode = 0;
        this.mApcReport = false;
        this.mReportInterval = 0;
        this.mCellCount = 0;
        this.mCellInfos = null;
    }

    protected PseudoCellInfo(int[] iArr) {
        this.mApcMode = iArr[0];
        this.mApcReport = iArr[1] == 1;
        this.mReportInterval = iArr[2];
        this.mCellCount = iArr[3];
        this.mCellInfos = new ArrayList<>();
        for (int i = 0; i < this.mCellCount; i++) {
            CellInfo cellInfo = new CellInfo();
            int i2 = i * 6;
            cellInfo.type = iArr[i2 + 4];
            cellInfo.plmn = iArr[i2 + 5];
            cellInfo.lac = iArr[i2 + 6];
            cellInfo.cid = iArr[i2 + 7];
            cellInfo.arfcn = iArr[i2 + 8];
            cellInfo.bsic = iArr[i2 + 9];
            this.mCellInfos.add(cellInfo);
        }
    }

    protected PseudoCellInfo(int i, boolean z, int i2, int[] iArr) {
        this.mApcMode = i;
        this.mApcReport = z;
        this.mReportInterval = i2;
        setCellInfo(iArr);
    }

    public void updateApcSetting(int i, boolean z, int i2) {
        this.mApcMode = i;
        this.mApcReport = z;
        this.mReportInterval = i2;
    }

    public void setCellInfo(int[] iArr) {
        this.mCellCount = iArr[0];
        this.mCellInfos = new ArrayList<>();
        for (int i = 0; i < this.mCellCount; i++) {
            CellInfo cellInfo = new CellInfo();
            int i2 = i * 6;
            cellInfo.type = iArr[i2 + 1];
            cellInfo.plmn = iArr[i2 + 2];
            cellInfo.lac = iArr[i2 + 3];
            cellInfo.cid = iArr[i2 + 4];
            cellInfo.arfcn = iArr[i2 + 5];
            cellInfo.bsic = iArr[i2 + 6];
            this.mCellInfos.add(cellInfo);
        }
    }

    public int getApcMode() {
        return this.mApcMode;
    }

    public boolean getReportEnable() {
        return this.mApcReport;
    }

    public int getReportInterval() {
        return this.mReportInterval;
    }

    public int getCellCount() {
        return this.mCellCount;
    }

    public int getType(int i) {
        if (i < 0 || i >= this.mCellCount || this.mCellInfos == null || this.mCellInfos.get(i) == null) {
            return 0;
        }
        return this.mCellInfos.get(i).type;
    }

    public int getPlmn(int i) {
        if (i < 0 || i >= this.mCellCount || this.mCellInfos == null || this.mCellInfos.get(i) == null) {
            return 0;
        }
        return this.mCellInfos.get(i).plmn;
    }

    public int getLac(int i) {
        if (i < 0 || i >= this.mCellCount || this.mCellInfos == null || this.mCellInfos.get(i) == null) {
            return 0;
        }
        return this.mCellInfos.get(i).lac;
    }

    public int getCid(int i) {
        if (i < 0 || i >= this.mCellCount || this.mCellInfos == null || this.mCellInfos.get(i) == null) {
            return 0;
        }
        return this.mCellInfos.get(i).cid;
    }

    public int getArfcn(int i) {
        if (i < 0 || i >= this.mCellCount || this.mCellInfos == null || this.mCellInfos.get(i) == null) {
            return 0;
        }
        return this.mCellInfos.get(i).arfcn;
    }

    public int getBsic(int i) {
        if (i < 0 || i >= this.mCellCount || this.mCellInfos == null || this.mCellInfos.get(i) == null) {
            return 0;
        }
        return this.mCellInfos.get(i).bsic;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("[");
        stringBuffer.append(this.mApcMode);
        stringBuffer.append(", ");
        stringBuffer.append(this.mApcReport);
        stringBuffer.append(", ");
        stringBuffer.append(this.mReportInterval);
        stringBuffer.append(", ");
        stringBuffer.append(this.mCellCount);
        stringBuffer.append("]");
        for (int i = 0; i < this.mCellCount && this.mCellInfos != null && this.mCellInfos.get(i) != null; i++) {
            stringBuffer.append("[");
            stringBuffer.append(this.mCellInfos.get(i).type);
            stringBuffer.append(", ");
            stringBuffer.append(this.mCellInfos.get(i).plmn);
            stringBuffer.append(", ");
            stringBuffer.append(this.mCellInfos.get(i).lac);
            stringBuffer.append(", ");
            stringBuffer.append(this.mCellInfos.get(i).cid);
            stringBuffer.append(", ");
            stringBuffer.append(this.mCellInfos.get(i).arfcn);
            stringBuffer.append(", ");
            stringBuffer.append(this.mCellInfos.get(i).bsic);
            stringBuffer.append("]");
        }
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mApcMode);
        parcel.writeInt(this.mApcReport ? 1 : 0);
        parcel.writeInt(this.mReportInterval);
        parcel.writeInt(this.mCellCount);
        for (int i2 = 0; i2 < this.mCellCount && this.mCellInfos != null && this.mCellInfos.get(i2) != null; i2++) {
            parcel.writeInt(this.mCellInfos.get(i2).type);
            parcel.writeInt(this.mCellInfos.get(i2).plmn);
            parcel.writeInt(this.mCellInfos.get(i2).lac);
            parcel.writeInt(this.mCellInfos.get(i2).cid);
            parcel.writeInt(this.mCellInfos.get(i2).arfcn);
            parcel.writeInt(this.mCellInfos.get(i2).bsic);
        }
    }

    protected PseudoCellInfo(Parcel parcel) {
        this.mApcMode = parcel.readInt();
        this.mApcReport = parcel.readInt() == 1;
        this.mReportInterval = parcel.readInt();
        this.mCellCount = parcel.readInt();
        this.mCellInfos = new ArrayList<>();
        for (int i = 0; i < this.mCellCount; i++) {
            CellInfo cellInfo = new CellInfo();
            cellInfo.type = parcel.readInt();
            cellInfo.plmn = parcel.readInt();
            cellInfo.lac = parcel.readInt();
            cellInfo.cid = parcel.readInt();
            cellInfo.arfcn = parcel.readInt();
            cellInfo.bsic = parcel.readInt();
            this.mCellInfos.add(cellInfo);
        }
    }
}
