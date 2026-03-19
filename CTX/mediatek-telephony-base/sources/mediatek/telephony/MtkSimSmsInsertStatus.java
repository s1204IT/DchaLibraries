package mediatek.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.Rlog;

public class MtkSimSmsInsertStatus implements Parcelable {
    public static final Parcelable.Creator<MtkSimSmsInsertStatus> CREATOR = new Parcelable.Creator<MtkSimSmsInsertStatus>() {
        @Override
        public MtkSimSmsInsertStatus createFromParcel(Parcel parcel) {
            return new MtkSimSmsInsertStatus(parcel.readInt(), parcel.readString());
        }

        @Override
        public MtkSimSmsInsertStatus[] newArray(int i) {
            return new MtkSimSmsInsertStatus[i];
        }
    };
    private static final String TAG = "MtkSimSmsInsertStatus";
    public String indexInIcc;
    public int insertStatus;

    public MtkSimSmsInsertStatus(int i, String str) {
        this.insertStatus = 0;
        this.indexInIcc = null;
        this.insertStatus = i;
        this.indexInIcc = str;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.insertStatus);
        parcel.writeString(this.indexInIcc);
    }

    public int[] getIndex() {
        if (this.indexInIcc == null) {
            return null;
        }
        String[] strArrSplit = this.indexInIcc.split(",");
        if (strArrSplit != null && strArrSplit.length > 0) {
            int[] iArr = new int[strArrSplit.length];
            for (int i = 0; i < iArr.length; i++) {
                try {
                    iArr[i] = Integer.parseInt(strArrSplit[i]);
                    Rlog.d(TAG, "index is " + iArr[i]);
                } catch (NumberFormatException e) {
                    Rlog.d("TAG", "fail to parse index");
                    iArr[i] = -1;
                }
            }
            return iArr;
        }
        Rlog.d(TAG, "should not arrive here");
        return null;
    }
}
