package android.app.servertransaction;

import android.app.ActivityThread;
import android.app.ClientTransactionHandler;
import android.app.ResultInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import android.util.MergedConfiguration;
import android.util.Slog;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.logging.nano.MetricsProto;
import java.util.List;
import java.util.Objects;

public class ActivityRelaunchItem extends ClientTransactionItem {
    public static final Parcelable.Creator<ActivityRelaunchItem> CREATOR = new Parcelable.Creator<ActivityRelaunchItem>() {
        @Override
        public ActivityRelaunchItem createFromParcel(Parcel parcel) {
            return new ActivityRelaunchItem(parcel);
        }

        @Override
        public ActivityRelaunchItem[] newArray(int i) {
            return new ActivityRelaunchItem[i];
        }
    };
    private static final String TAG = "ActivityRelaunchItem";
    private ActivityThread.ActivityClientRecord mActivityClientRecord;
    private MergedConfiguration mConfig;
    private int mConfigChanges;
    private List<ReferrerIntent> mPendingNewIntents;
    private List<ResultInfo> mPendingResults;
    private boolean mPreserveWindow;

    @Override
    public void preExecute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder) {
        this.mActivityClientRecord = clientTransactionHandler.prepareRelaunchActivity(iBinder, this.mPendingResults, this.mPendingNewIntents, this.mConfigChanges, this.mConfig, this.mPreserveWindow);
    }

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        if (this.mActivityClientRecord == null) {
            if (ActivityThread.DEBUG_ORDER) {
                Slog.d(TAG, "Activity relaunch cancelled");
            }
        } else {
            Trace.traceBegin(64L, "activityRestart");
            clientTransactionHandler.handleRelaunchActivity(this.mActivityClientRecord, pendingTransactionActions);
            Trace.traceEnd(64L);
        }
    }

    @Override
    public void postExecute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        clientTransactionHandler.reportRelaunch(iBinder, pendingTransactionActions);
    }

    private ActivityRelaunchItem() {
    }

    public static ActivityRelaunchItem obtain(List<ResultInfo> list, List<ReferrerIntent> list2, int i, MergedConfiguration mergedConfiguration, boolean z) {
        ActivityRelaunchItem activityRelaunchItem = (ActivityRelaunchItem) ObjectPool.obtain(ActivityRelaunchItem.class);
        if (activityRelaunchItem == null) {
            activityRelaunchItem = new ActivityRelaunchItem();
        }
        activityRelaunchItem.mPendingResults = list;
        activityRelaunchItem.mPendingNewIntents = list2;
        activityRelaunchItem.mConfigChanges = i;
        activityRelaunchItem.mConfig = mergedConfiguration;
        activityRelaunchItem.mPreserveWindow = z;
        return activityRelaunchItem;
    }

    @Override
    public void recycle() {
        this.mPendingResults = null;
        this.mPendingNewIntents = null;
        this.mConfigChanges = 0;
        this.mConfig = null;
        this.mPreserveWindow = false;
        this.mActivityClientRecord = null;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(this.mPendingResults, i);
        parcel.writeTypedList(this.mPendingNewIntents, i);
        parcel.writeInt(this.mConfigChanges);
        parcel.writeTypedObject(this.mConfig, i);
        parcel.writeBoolean(this.mPreserveWindow);
    }

    private ActivityRelaunchItem(Parcel parcel) {
        this.mPendingResults = parcel.createTypedArrayList(ResultInfo.CREATOR);
        this.mPendingNewIntents = parcel.createTypedArrayList(ReferrerIntent.CREATOR);
        this.mConfigChanges = parcel.readInt();
        this.mConfig = (MergedConfiguration) parcel.readTypedObject(MergedConfiguration.CREATOR);
        this.mPreserveWindow = parcel.readBoolean();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ActivityRelaunchItem activityRelaunchItem = (ActivityRelaunchItem) obj;
        if (Objects.equals(this.mPendingResults, activityRelaunchItem.mPendingResults) && Objects.equals(this.mPendingNewIntents, activityRelaunchItem.mPendingNewIntents) && this.mConfigChanges == activityRelaunchItem.mConfigChanges && Objects.equals(this.mConfig, activityRelaunchItem.mConfig) && this.mPreserveWindow == activityRelaunchItem.mPreserveWindow) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + Objects.hashCode(this.mPendingResults)) * 31) + Objects.hashCode(this.mPendingNewIntents)) * 31) + this.mConfigChanges) * 31) + Objects.hashCode(this.mConfig))) + (this.mPreserveWindow ? 1 : 0);
    }

    public String toString() {
        return "ActivityRelaunchItem{pendingResults=" + this.mPendingResults + ",pendingNewIntents=" + this.mPendingNewIntents + ",configChanges=" + this.mConfigChanges + ",config=" + this.mConfig + ",preserveWindow" + this.mPreserveWindow + "}";
    }
}
