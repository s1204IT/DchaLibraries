package android.app;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Objects;

public class ResultInfo implements Parcelable {
    public static final Parcelable.Creator<ResultInfo> CREATOR = new Parcelable.Creator<ResultInfo>() {
        @Override
        public ResultInfo createFromParcel(Parcel parcel) {
            return new ResultInfo(parcel);
        }

        @Override
        public ResultInfo[] newArray(int i) {
            return new ResultInfo[i];
        }
    };
    public final Intent mData;
    public final int mRequestCode;
    public final int mResultCode;
    public final String mResultWho;

    public ResultInfo(String str, int i, int i2, Intent intent) {
        this.mResultWho = str;
        this.mRequestCode = i;
        this.mResultCode = i2;
        this.mData = intent;
    }

    public String toString() {
        return "ResultInfo{who=" + this.mResultWho + ", request=" + this.mRequestCode + ", result=" + this.mResultCode + ", data=" + this.mData + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mResultWho);
        parcel.writeInt(this.mRequestCode);
        parcel.writeInt(this.mResultCode);
        if (this.mData != null) {
            parcel.writeInt(1);
            this.mData.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
    }

    public ResultInfo(Parcel parcel) {
        this.mResultWho = parcel.readString();
        this.mRequestCode = parcel.readInt();
        this.mResultCode = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.mData = Intent.CREATOR.createFromParcel(parcel);
        } else {
            this.mData = null;
        }
    }

    public boolean equals(Object obj) {
        boolean zFilterEquals;
        if (obj == null || !(obj instanceof ResultInfo)) {
            return false;
        }
        ResultInfo resultInfo = (ResultInfo) obj;
        if (this.mData != null) {
            zFilterEquals = this.mData.filterEquals(resultInfo.mData);
        } else {
            zFilterEquals = resultInfo.mData == null;
        }
        return zFilterEquals && Objects.equals(this.mResultWho, resultInfo.mResultWho) && this.mResultCode == resultInfo.mResultCode && this.mRequestCode == resultInfo.mRequestCode;
    }

    public int hashCode() {
        int iHashCode = ((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.mRequestCode) * 31) + this.mResultCode) * 31) + Objects.hashCode(this.mResultWho);
        if (this.mData != null) {
            return this.mData.filterHashCode() + (31 * iHashCode);
        }
        return iHashCode;
    }
}
