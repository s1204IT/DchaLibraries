package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.logging.nano.MetricsProto;
import java.util.List;
import java.util.Objects;

public class NewIntentItem extends ClientTransactionItem {
    public static final Parcelable.Creator<NewIntentItem> CREATOR = new Parcelable.Creator<NewIntentItem>() {
        @Override
        public NewIntentItem createFromParcel(Parcel parcel) {
            return new NewIntentItem(parcel);
        }

        @Override
        public NewIntentItem[] newArray(int i) {
            return new NewIntentItem[i];
        }
    };
    private List<ReferrerIntent> mIntents;
    private boolean mPause;

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        Trace.traceBegin(64L, "activityNewIntent");
        clientTransactionHandler.handleNewIntent(iBinder, this.mIntents, this.mPause);
        Trace.traceEnd(64L);
    }

    private NewIntentItem() {
    }

    public static NewIntentItem obtain(List<ReferrerIntent> list, boolean z) {
        NewIntentItem newIntentItem = (NewIntentItem) ObjectPool.obtain(NewIntentItem.class);
        if (newIntentItem == null) {
            newIntentItem = new NewIntentItem();
        }
        newIntentItem.mIntents = list;
        newIntentItem.mPause = z;
        return newIntentItem;
    }

    @Override
    public void recycle() {
        this.mIntents = null;
        this.mPause = false;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBoolean(this.mPause);
        parcel.writeTypedList(this.mIntents, i);
    }

    private NewIntentItem(Parcel parcel) {
        this.mPause = parcel.readBoolean();
        this.mIntents = parcel.createTypedArrayList(ReferrerIntent.CREATOR);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NewIntentItem newIntentItem = (NewIntentItem) obj;
        if (this.mPause == newIntentItem.mPause && Objects.equals(this.mIntents, newIntentItem.mIntents)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + (this.mPause ? 1 : 0))) + this.mIntents.hashCode();
    }

    public String toString() {
        return "NewIntentItem{pause=" + this.mPause + ",intents=" + this.mIntents + "}";
    }
}
