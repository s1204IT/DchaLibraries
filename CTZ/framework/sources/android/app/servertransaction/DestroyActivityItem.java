package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import com.android.internal.logging.nano.MetricsProto;

public class DestroyActivityItem extends ActivityLifecycleItem {
    public static final Parcelable.Creator<DestroyActivityItem> CREATOR = new Parcelable.Creator<DestroyActivityItem>() {
        @Override
        public DestroyActivityItem createFromParcel(Parcel parcel) {
            return new DestroyActivityItem(parcel);
        }

        @Override
        public DestroyActivityItem[] newArray(int i) {
            return new DestroyActivityItem[i];
        }
    };
    private int mConfigChanges;
    private boolean mFinished;

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        Trace.traceBegin(64L, "activityDestroy");
        clientTransactionHandler.handleDestroyActivity(iBinder, this.mFinished, this.mConfigChanges, false, "DestroyActivityItem");
        Trace.traceEnd(64L);
    }

    @Override
    public int getTargetState() {
        return 6;
    }

    private DestroyActivityItem() {
    }

    public static DestroyActivityItem obtain(boolean z, int i) {
        DestroyActivityItem destroyActivityItem = (DestroyActivityItem) ObjectPool.obtain(DestroyActivityItem.class);
        if (destroyActivityItem == null) {
            destroyActivityItem = new DestroyActivityItem();
        }
        destroyActivityItem.mFinished = z;
        destroyActivityItem.mConfigChanges = i;
        return destroyActivityItem;
    }

    @Override
    public void recycle() {
        super.recycle();
        this.mFinished = false;
        this.mConfigChanges = 0;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBoolean(this.mFinished);
        parcel.writeInt(this.mConfigChanges);
    }

    private DestroyActivityItem(Parcel parcel) {
        this.mFinished = parcel.readBoolean();
        this.mConfigChanges = parcel.readInt();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DestroyActivityItem destroyActivityItem = (DestroyActivityItem) obj;
        if (this.mFinished == destroyActivityItem.mFinished && this.mConfigChanges == destroyActivityItem.mConfigChanges) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + (this.mFinished ? 1 : 0))) + this.mConfigChanges;
    }

    public String toString() {
        return "DestroyActivityItem{finished=" + this.mFinished + ",mConfigChanges=" + this.mConfigChanges + "}";
    }
}
