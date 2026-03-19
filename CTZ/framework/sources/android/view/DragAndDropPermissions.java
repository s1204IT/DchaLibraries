package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import com.android.internal.view.IDragAndDropPermissions;

public final class DragAndDropPermissions implements Parcelable {
    public static final Parcelable.Creator<DragAndDropPermissions> CREATOR = new Parcelable.Creator<DragAndDropPermissions>() {
        @Override
        public DragAndDropPermissions createFromParcel(Parcel parcel) {
            return new DragAndDropPermissions(parcel);
        }

        @Override
        public DragAndDropPermissions[] newArray(int i) {
            return new DragAndDropPermissions[i];
        }
    };
    private final IDragAndDropPermissions mDragAndDropPermissions;
    private IBinder mTransientToken;

    public static DragAndDropPermissions obtain(DragEvent dragEvent) {
        if (dragEvent.getDragAndDropPermissions() == null) {
            return null;
        }
        return new DragAndDropPermissions(dragEvent.getDragAndDropPermissions());
    }

    private DragAndDropPermissions(IDragAndDropPermissions iDragAndDropPermissions) {
        this.mDragAndDropPermissions = iDragAndDropPermissions;
    }

    public boolean take(IBinder iBinder) {
        try {
            this.mDragAndDropPermissions.take(iBinder);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean takeTransient() {
        try {
            this.mTransientToken = new Binder();
            this.mDragAndDropPermissions.takeTransient(this.mTransientToken);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void release() {
        try {
            this.mDragAndDropPermissions.release();
            this.mTransientToken = null;
        } catch (RemoteException e) {
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongInterface(this.mDragAndDropPermissions);
        parcel.writeStrongBinder(this.mTransientToken);
    }

    private DragAndDropPermissions(Parcel parcel) {
        this.mDragAndDropPermissions = IDragAndDropPermissions.Stub.asInterface(parcel.readStrongBinder());
        this.mTransientToken = parcel.readStrongBinder();
    }
}
