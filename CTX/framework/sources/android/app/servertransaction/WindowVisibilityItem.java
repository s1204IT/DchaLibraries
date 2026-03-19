package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;

public class WindowVisibilityItem extends ClientTransactionItem {
    public static final Parcelable.Creator<WindowVisibilityItem> CREATOR = new Parcelable.Creator<WindowVisibilityItem>() {
        @Override
        public WindowVisibilityItem createFromParcel(Parcel parcel) {
            return new WindowVisibilityItem(parcel);
        }

        @Override
        public WindowVisibilityItem[] newArray(int i) {
            return new WindowVisibilityItem[i];
        }
    };
    private boolean mShowWindow;

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        Trace.traceBegin(64L, "activityShowWindow");
        clientTransactionHandler.handleWindowVisibility(iBinder, this.mShowWindow);
        Trace.traceEnd(64L);
    }

    private WindowVisibilityItem() {
    }

    public static WindowVisibilityItem obtain(boolean z) {
        WindowVisibilityItem windowVisibilityItem = (WindowVisibilityItem) ObjectPool.obtain(WindowVisibilityItem.class);
        if (windowVisibilityItem == null) {
            windowVisibilityItem = new WindowVisibilityItem();
        }
        windowVisibilityItem.mShowWindow = z;
        return windowVisibilityItem;
    }

    @Override
    public void recycle() {
        this.mShowWindow = false;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBoolean(this.mShowWindow);
    }

    private WindowVisibilityItem(Parcel parcel) {
        this.mShowWindow = parcel.readBoolean();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mShowWindow == ((WindowVisibilityItem) obj).mShowWindow) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return 17 + (31 * (this.mShowWindow ? 1 : 0));
    }

    public String toString() {
        return "WindowVisibilityItem{showWindow=" + this.mShowWindow + "}";
    }
}
