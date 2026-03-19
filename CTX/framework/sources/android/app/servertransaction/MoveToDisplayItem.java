package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Objects;

public class MoveToDisplayItem extends ClientTransactionItem {
    public static final Parcelable.Creator<MoveToDisplayItem> CREATOR = new Parcelable.Creator<MoveToDisplayItem>() {
        @Override
        public MoveToDisplayItem createFromParcel(Parcel parcel) {
            return new MoveToDisplayItem(parcel);
        }

        @Override
        public MoveToDisplayItem[] newArray(int i) {
            return new MoveToDisplayItem[i];
        }
    };
    private Configuration mConfiguration;
    private int mTargetDisplayId;

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        Trace.traceBegin(64L, "activityMovedToDisplay");
        clientTransactionHandler.handleActivityConfigurationChanged(iBinder, this.mConfiguration, this.mTargetDisplayId);
        Trace.traceEnd(64L);
    }

    private MoveToDisplayItem() {
    }

    public static MoveToDisplayItem obtain(int i, Configuration configuration) {
        MoveToDisplayItem moveToDisplayItem = (MoveToDisplayItem) ObjectPool.obtain(MoveToDisplayItem.class);
        if (moveToDisplayItem == null) {
            moveToDisplayItem = new MoveToDisplayItem();
        }
        moveToDisplayItem.mTargetDisplayId = i;
        moveToDisplayItem.mConfiguration = configuration;
        return moveToDisplayItem;
    }

    @Override
    public void recycle() {
        this.mTargetDisplayId = 0;
        this.mConfiguration = null;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mTargetDisplayId);
        parcel.writeTypedObject(this.mConfiguration, i);
    }

    private MoveToDisplayItem(Parcel parcel) {
        this.mTargetDisplayId = parcel.readInt();
        this.mConfiguration = (Configuration) parcel.readTypedObject(Configuration.CREATOR);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MoveToDisplayItem moveToDisplayItem = (MoveToDisplayItem) obj;
        if (this.mTargetDisplayId == moveToDisplayItem.mTargetDisplayId && Objects.equals(this.mConfiguration, moveToDisplayItem.mConfiguration)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.mTargetDisplayId)) + this.mConfiguration.hashCode();
    }

    public String toString() {
        return "MoveToDisplayItem{targetDisplayId=" + this.mTargetDisplayId + ",configuration=" + this.mConfiguration + "}";
    }
}
