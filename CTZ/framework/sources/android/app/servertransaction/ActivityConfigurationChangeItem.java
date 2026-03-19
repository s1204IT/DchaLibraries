package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import java.util.Objects;

public class ActivityConfigurationChangeItem extends ClientTransactionItem {
    public static final Parcelable.Creator<ActivityConfigurationChangeItem> CREATOR = new Parcelable.Creator<ActivityConfigurationChangeItem>() {
        @Override
        public ActivityConfigurationChangeItem createFromParcel(Parcel parcel) {
            return new ActivityConfigurationChangeItem(parcel);
        }

        @Override
        public ActivityConfigurationChangeItem[] newArray(int i) {
            return new ActivityConfigurationChangeItem[i];
        }
    };
    private Configuration mConfiguration;

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        Trace.traceBegin(64L, "activityConfigChanged");
        clientTransactionHandler.handleActivityConfigurationChanged(iBinder, this.mConfiguration, -1);
        Trace.traceEnd(64L);
    }

    private ActivityConfigurationChangeItem() {
    }

    public static ActivityConfigurationChangeItem obtain(Configuration configuration) {
        ActivityConfigurationChangeItem activityConfigurationChangeItem = (ActivityConfigurationChangeItem) ObjectPool.obtain(ActivityConfigurationChangeItem.class);
        if (activityConfigurationChangeItem == null) {
            activityConfigurationChangeItem = new ActivityConfigurationChangeItem();
        }
        activityConfigurationChangeItem.mConfiguration = configuration;
        return activityConfigurationChangeItem;
    }

    @Override
    public void recycle() {
        this.mConfiguration = null;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedObject(this.mConfiguration, i);
    }

    private ActivityConfigurationChangeItem(Parcel parcel) {
        this.mConfiguration = (Configuration) parcel.readTypedObject(Configuration.CREATOR);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.mConfiguration, ((ActivityConfigurationChangeItem) obj).mConfiguration);
    }

    public int hashCode() {
        return this.mConfiguration.hashCode();
    }

    public String toString() {
        return "ActivityConfigurationChange{config=" + this.mConfiguration + "}";
    }
}
