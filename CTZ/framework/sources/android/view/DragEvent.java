package android.view;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.view.IDragAndDropPermissions;

public class DragEvent implements Parcelable {
    public static final int ACTION_DRAG_ENDED = 4;
    public static final int ACTION_DRAG_ENTERED = 5;
    public static final int ACTION_DRAG_EXITED = 6;
    public static final int ACTION_DRAG_LOCATION = 2;
    public static final int ACTION_DRAG_STARTED = 1;
    public static final int ACTION_DROP = 3;
    private static final int MAX_RECYCLED = 10;
    private static final boolean TRACK_RECYCLED_LOCATION = false;
    int mAction;
    ClipData mClipData;
    ClipDescription mClipDescription;
    IDragAndDropPermissions mDragAndDropPermissions;
    boolean mDragResult;
    boolean mEventHandlerWasCalled;
    Object mLocalState;
    private DragEvent mNext;
    private boolean mRecycled;
    private RuntimeException mRecycledLocation;
    float mX;
    float mY;
    private static final Object gRecyclerLock = new Object();
    private static int gRecyclerUsed = 0;
    private static DragEvent gRecyclerTop = null;
    public static final Parcelable.Creator<DragEvent> CREATOR = new Parcelable.Creator<DragEvent>() {
        @Override
        public DragEvent createFromParcel(Parcel parcel) {
            DragEvent dragEventObtain = DragEvent.obtain();
            dragEventObtain.mAction = parcel.readInt();
            dragEventObtain.mX = parcel.readFloat();
            dragEventObtain.mY = parcel.readFloat();
            dragEventObtain.mDragResult = parcel.readInt() != 0;
            if (parcel.readInt() != 0) {
                dragEventObtain.mClipData = ClipData.CREATOR.createFromParcel(parcel);
            }
            if (parcel.readInt() != 0) {
                dragEventObtain.mClipDescription = ClipDescription.CREATOR.createFromParcel(parcel);
            }
            if (parcel.readInt() != 0) {
                dragEventObtain.mDragAndDropPermissions = IDragAndDropPermissions.Stub.asInterface(parcel.readStrongBinder());
            }
            return dragEventObtain;
        }

        @Override
        public DragEvent[] newArray(int i) {
            return new DragEvent[i];
        }
    };

    private DragEvent() {
    }

    private void init(int i, float f, float f2, ClipDescription clipDescription, ClipData clipData, IDragAndDropPermissions iDragAndDropPermissions, Object obj, boolean z) {
        this.mAction = i;
        this.mX = f;
        this.mY = f2;
        this.mClipDescription = clipDescription;
        this.mClipData = clipData;
        this.mDragAndDropPermissions = iDragAndDropPermissions;
        this.mLocalState = obj;
        this.mDragResult = z;
    }

    static DragEvent obtain() {
        return obtain(0, 0.0f, 0.0f, null, null, null, null, false);
    }

    public static DragEvent obtain(int i, float f, float f2, Object obj, ClipDescription clipDescription, ClipData clipData, IDragAndDropPermissions iDragAndDropPermissions, boolean z) {
        synchronized (gRecyclerLock) {
            if (gRecyclerTop == null) {
                DragEvent dragEvent = new DragEvent();
                dragEvent.init(i, f, f2, clipDescription, clipData, iDragAndDropPermissions, obj, z);
                return dragEvent;
            }
            DragEvent dragEvent2 = gRecyclerTop;
            gRecyclerTop = dragEvent2.mNext;
            gRecyclerUsed--;
            dragEvent2.mRecycledLocation = null;
            dragEvent2.mRecycled = false;
            dragEvent2.mNext = null;
            dragEvent2.init(i, f, f2, clipDescription, clipData, iDragAndDropPermissions, obj, z);
            return dragEvent2;
        }
    }

    public static DragEvent obtain(DragEvent dragEvent) {
        return obtain(dragEvent.mAction, dragEvent.mX, dragEvent.mY, dragEvent.mLocalState, dragEvent.mClipDescription, dragEvent.mClipData, dragEvent.mDragAndDropPermissions, dragEvent.mDragResult);
    }

    public int getAction() {
        return this.mAction;
    }

    public float getX() {
        return this.mX;
    }

    public float getY() {
        return this.mY;
    }

    public ClipData getClipData() {
        return this.mClipData;
    }

    public ClipDescription getClipDescription() {
        return this.mClipDescription;
    }

    public IDragAndDropPermissions getDragAndDropPermissions() {
        return this.mDragAndDropPermissions;
    }

    public Object getLocalState() {
        return this.mLocalState;
    }

    public boolean getResult() {
        return this.mDragResult;
    }

    public final void recycle() {
        if (this.mRecycled) {
            throw new RuntimeException(toString() + " recycled twice!");
        }
        this.mRecycled = true;
        this.mClipData = null;
        this.mClipDescription = null;
        this.mLocalState = null;
        this.mEventHandlerWasCalled = false;
        synchronized (gRecyclerLock) {
            if (gRecyclerUsed < 10) {
                gRecyclerUsed++;
                this.mNext = gRecyclerTop;
                gRecyclerTop = this;
            }
        }
    }

    public String toString() {
        return "DragEvent{" + Integer.toHexString(System.identityHashCode(this)) + " action=" + this.mAction + " @ (" + this.mX + ", " + this.mY + ") desc=" + this.mClipDescription + " data=" + this.mClipData + " local=" + this.mLocalState + " result=" + this.mDragResult + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAction);
        parcel.writeFloat(this.mX);
        parcel.writeFloat(this.mY);
        parcel.writeInt(this.mDragResult ? 1 : 0);
        if (this.mClipData == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(1);
            this.mClipData.writeToParcel(parcel, i);
        }
        if (this.mClipDescription == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(1);
            this.mClipDescription.writeToParcel(parcel, i);
        }
        if (this.mDragAndDropPermissions == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(1);
            parcel.writeStrongBinder(this.mDragAndDropPermissions.asBinder());
        }
    }
}
