package android.view;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pools;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

public class WindowInfo implements Parcelable {
    private static final int MAX_POOL_SIZE = 10;
    public IBinder activityToken;
    public List<IBinder> childTokens;
    public boolean focused;
    public boolean inPictureInPicture;
    public int layer;
    public IBinder parentToken;
    public CharSequence title;
    public IBinder token;
    public int type;
    private static final Pools.SynchronizedPool<WindowInfo> sPool = new Pools.SynchronizedPool<>(10);
    public static final Parcelable.Creator<WindowInfo> CREATOR = new Parcelable.Creator<WindowInfo>() {
        @Override
        public WindowInfo createFromParcel(Parcel parcel) {
            WindowInfo windowInfoObtain = WindowInfo.obtain();
            windowInfoObtain.initFromParcel(parcel);
            return windowInfoObtain;
        }

        @Override
        public WindowInfo[] newArray(int i) {
            return new WindowInfo[i];
        }
    };
    public final Rect boundsInScreen = new Rect();
    public long accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;

    private WindowInfo() {
    }

    public static WindowInfo obtain() {
        WindowInfo windowInfoAcquire = sPool.acquire();
        if (windowInfoAcquire == null) {
            return new WindowInfo();
        }
        return windowInfoAcquire;
    }

    public static WindowInfo obtain(WindowInfo windowInfo) {
        WindowInfo windowInfoObtain = obtain();
        windowInfoObtain.type = windowInfo.type;
        windowInfoObtain.layer = windowInfo.layer;
        windowInfoObtain.token = windowInfo.token;
        windowInfoObtain.parentToken = windowInfo.parentToken;
        windowInfoObtain.activityToken = windowInfo.activityToken;
        windowInfoObtain.focused = windowInfo.focused;
        windowInfoObtain.boundsInScreen.set(windowInfo.boundsInScreen);
        windowInfoObtain.title = windowInfo.title;
        windowInfoObtain.accessibilityIdOfAnchor = windowInfo.accessibilityIdOfAnchor;
        windowInfoObtain.inPictureInPicture = windowInfo.inPictureInPicture;
        if (windowInfo.childTokens != null && !windowInfo.childTokens.isEmpty()) {
            if (windowInfoObtain.childTokens == null) {
                windowInfoObtain.childTokens = new ArrayList(windowInfo.childTokens);
            } else {
                windowInfoObtain.childTokens.addAll(windowInfo.childTokens);
            }
        }
        return windowInfoObtain;
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.type);
        parcel.writeInt(this.layer);
        parcel.writeStrongBinder(this.token);
        parcel.writeStrongBinder(this.parentToken);
        parcel.writeStrongBinder(this.activityToken);
        parcel.writeInt(this.focused ? 1 : 0);
        this.boundsInScreen.writeToParcel(parcel, i);
        parcel.writeCharSequence(this.title);
        parcel.writeLong(this.accessibilityIdOfAnchor);
        parcel.writeInt(this.inPictureInPicture ? 1 : 0);
        if (this.childTokens != null && !this.childTokens.isEmpty()) {
            parcel.writeInt(1);
            parcel.writeBinderList(this.childTokens);
        } else {
            parcel.writeInt(0);
        }
    }

    public String toString() {
        return "WindowInfo[title=" + this.title + ", type=" + this.type + ", layer=" + this.layer + ", token=" + this.token + ", bounds=" + this.boundsInScreen + ", parent=" + this.parentToken + ", focused=" + this.focused + ", children=" + this.childTokens + ", accessibility anchor=" + this.accessibilityIdOfAnchor + ']';
    }

    private void initFromParcel(Parcel parcel) {
        this.type = parcel.readInt();
        this.layer = parcel.readInt();
        this.token = parcel.readStrongBinder();
        this.parentToken = parcel.readStrongBinder();
        this.activityToken = parcel.readStrongBinder();
        this.focused = parcel.readInt() == 1;
        this.boundsInScreen.readFromParcel(parcel);
        this.title = parcel.readCharSequence();
        this.accessibilityIdOfAnchor = parcel.readLong();
        this.inPictureInPicture = parcel.readInt() == 1;
        if (parcel.readInt() == 1) {
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
        this.activityToken = null;
        this.focused = false;
        this.boundsInScreen.setEmpty();
        if (this.childTokens != null) {
            this.childTokens.clear();
        }
        this.inPictureInPicture = false;
    }
}
