package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.app.ResultInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import java.util.List;
import java.util.Objects;

public class ActivityResultItem extends ClientTransactionItem {
    public static final Parcelable.Creator<ActivityResultItem> CREATOR = new Parcelable.Creator<ActivityResultItem>() {
        @Override
        public ActivityResultItem createFromParcel(Parcel parcel) {
            return new ActivityResultItem(parcel);
        }

        @Override
        public ActivityResultItem[] newArray(int i) {
            return new ActivityResultItem[i];
        }
    };
    private List<ResultInfo> mResultInfoList;

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        Trace.traceBegin(64L, "activityDeliverResult");
        clientTransactionHandler.handleSendResult(iBinder, this.mResultInfoList, "ACTIVITY_RESULT");
        Trace.traceEnd(64L);
    }

    private ActivityResultItem() {
    }

    public static ActivityResultItem obtain(List<ResultInfo> list) {
        ActivityResultItem activityResultItem = (ActivityResultItem) ObjectPool.obtain(ActivityResultItem.class);
        if (activityResultItem == null) {
            activityResultItem = new ActivityResultItem();
        }
        activityResultItem.mResultInfoList = list;
        return activityResultItem;
    }

    @Override
    public void recycle() {
        this.mResultInfoList = null;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(this.mResultInfoList, i);
    }

    private ActivityResultItem(Parcel parcel) {
        this.mResultInfoList = parcel.createTypedArrayList(ResultInfo.CREATOR);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.mResultInfoList, ((ActivityResultItem) obj).mResultInfoList);
    }

    public int hashCode() {
        return this.mResultInfoList.hashCode();
    }

    public String toString() {
        return "ActivityResultItem{resultInfoList=" + this.mResultInfoList + "}";
    }
}
