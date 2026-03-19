package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Objects;

public class MultiWindowModeChangeItem extends ClientTransactionItem {
    public static final Parcelable.Creator<MultiWindowModeChangeItem> CREATOR = new Parcelable.Creator<MultiWindowModeChangeItem>() {
        @Override
        public MultiWindowModeChangeItem createFromParcel(Parcel parcel) {
            return new MultiWindowModeChangeItem(parcel);
        }

        @Override
        public MultiWindowModeChangeItem[] newArray(int i) {
            return new MultiWindowModeChangeItem[i];
        }
    };
    private boolean mIsInMultiWindowMode;
    private Configuration mOverrideConfig;

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        clientTransactionHandler.handleMultiWindowModeChanged(iBinder, this.mIsInMultiWindowMode, this.mOverrideConfig);
    }

    private MultiWindowModeChangeItem() {
    }

    public static MultiWindowModeChangeItem obtain(boolean z, Configuration configuration) {
        MultiWindowModeChangeItem multiWindowModeChangeItem = (MultiWindowModeChangeItem) ObjectPool.obtain(MultiWindowModeChangeItem.class);
        if (multiWindowModeChangeItem == null) {
            multiWindowModeChangeItem = new MultiWindowModeChangeItem();
        }
        multiWindowModeChangeItem.mIsInMultiWindowMode = z;
        multiWindowModeChangeItem.mOverrideConfig = configuration;
        return multiWindowModeChangeItem;
    }

    @Override
    public void recycle() {
        this.mIsInMultiWindowMode = false;
        this.mOverrideConfig = null;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBoolean(this.mIsInMultiWindowMode);
        parcel.writeTypedObject(this.mOverrideConfig, i);
    }

    private MultiWindowModeChangeItem(Parcel parcel) {
        this.mIsInMultiWindowMode = parcel.readBoolean();
        this.mOverrideConfig = (Configuration) parcel.readTypedObject(Configuration.CREATOR);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MultiWindowModeChangeItem multiWindowModeChangeItem = (MultiWindowModeChangeItem) obj;
        if (this.mIsInMultiWindowMode == multiWindowModeChangeItem.mIsInMultiWindowMode && Objects.equals(this.mOverrideConfig, multiWindowModeChangeItem.mOverrideConfig)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + (this.mIsInMultiWindowMode ? 1 : 0))) + this.mOverrideConfig.hashCode();
    }

    public String toString() {
        return "MultiWindowModeChangeItem{isInMultiWindowMode=" + this.mIsInMultiWindowMode + ",overrideConfig=" + this.mOverrideConfig + "}";
    }
}
