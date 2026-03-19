package android.content;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class UndoOperation<DATA> implements Parcelable {
    UndoOwner mOwner;

    public abstract void commit();

    public abstract void redo();

    public abstract void undo();

    public UndoOperation(UndoOwner undoOwner) {
        this.mOwner = undoOwner;
    }

    protected UndoOperation(Parcel parcel, ClassLoader classLoader) {
    }

    public UndoOwner getOwner() {
        return this.mOwner;
    }

    public DATA getOwnerData() {
        return (DATA) this.mOwner.getData();
    }

    public boolean matchOwner(UndoOwner undoOwner) {
        return undoOwner == getOwner();
    }

    public boolean hasData() {
        return true;
    }

    public boolean allowMerge() {
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
