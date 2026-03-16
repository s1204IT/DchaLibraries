package android.view;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pools;
import java.util.ArrayList;
import java.util.List;

public class WindowInfo implements Parcelable {
    private static final int MAX_POOL_SIZE = 10;
    public final Rect boundsInScreen = new Rect();
    public List<IBinder> childTokens;
    public boolean focused;
    public int layer;
    public IBinder parentToken;
    public IBinder token;
    public int type;
    private static final Pools.SynchronizedPool<WindowInfo> sPool = new Pools.SynchronizedPool<>(10);
    public static final Parcelable.Creator<WindowInfo> CREATOR = new Parcelable.Creator<WindowInfo>() {
        @Override
        public WindowInfo createFromParcel(Parcel parcel) {
            WindowInfo window = WindowInfo.obtain();
            window.initFromParcel(parcel);
            return window;
        }

        @Override
        public WindowInfo[] newArray(int size) {
            return new WindowInfo[size];
        }
    };

    private WindowInfo() {
    }

    public static WindowInfo obtain() {
        WindowInfo window = sPool.acquire();
        if (window == null) {
            return new WindowInfo();
        }
        return window;
    }

    public static WindowInfo obtain(WindowInfo other) {
        WindowInfo window = obtain();
        window.type = other.type;
        window.layer = other.layer;
        window.token = other.token;
        window.parentToken = other.parentToken;
        window.focused = other.focused;
        window.boundsInScreen.set(other.boundsInScreen);
        if (other.childTokens != null && !other.childTokens.isEmpty()) {
            if (window.childTokens == null) {
                window.childTokens = new ArrayList(other.childTokens);
            } else {
                window.childTokens.addAll(other.childTokens);
            }
        }
        return window;
    }

    public void recycle() {
        clear();
        sPool.release(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.type);
        parcel.writeInt(this.layer);
        parcel.writeStrongBinder(this.token);
        parcel.writeStrongBinder(this.parentToken);
        parcel.writeInt(this.focused ? 1 : 0);
        this.boundsInScreen.writeToParcel(parcel, flags);
        if (this.childTokens != null && !this.childTokens.isEmpty()) {
            parcel.writeInt(1);
            parcel.writeBinderList(this.childTokens);
        } else {
            parcel.writeInt(0);
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WindowInfo[");
        builder.append("type=").append(this.type);
        builder.append(", layer=").append(this.layer);
        builder.append(", token=").append(this.token);
        builder.append(", bounds=").append(this.boundsInScreen);
        builder.append(", parent=").append(this.parentToken);
        builder.append(", focused=").append(this.focused);
        builder.append(", children=").append(this.childTokens);
        builder.append(']');
        return builder.toString();
    }

    private void initFromParcel(Parcel parcel) {
        this.type = parcel.readInt();
        this.layer = parcel.readInt();
        this.token = parcel.readStrongBinder();
        this.parentToken = parcel.readStrongBinder();
        this.focused = parcel.readInt() == 1;
        this.boundsInScreen.readFromParcel(parcel);
        boolean hasChildren = parcel.readInt() == 1;
        if (hasChildren) {
            if (this.childTokens == null) {
                this.childTokens = new ArrayList();
            }
            parcel.readBinderList(this.childTokens);
        }
    }

    private void clear() {
        this.type = 0;
        this.layer = 0;
        this.token = null;
        this.parentToken = null;
        this.focused = false;
        this.boundsInScreen.setEmpty();
        if (this.childTokens != null) {
            this.childTokens.clear();
        }
    }
}
