package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRecentsNonSystemUserCallbacks extends IInterface {
    void cancelPreloadingRecents() throws RemoteException;

    void hideRecents(boolean z, boolean z2) throws RemoteException;

    void onConfigurationChanged() throws RemoteException;

    void onDraggingInRecents(float f) throws RemoteException;

    void onDraggingInRecentsEnded(float f) throws RemoteException;

    void preloadRecents() throws RemoteException;

    void showCurrentUserToast(int i, int i2) throws RemoteException;

    void showRecents(boolean z, boolean z2, boolean z3, int i) throws RemoteException;

    void splitPrimaryTask(int i, int i2, int i3, Rect rect) throws RemoteException;

    void toggleRecents(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IRecentsNonSystemUserCallbacks {
        public Stub() {
            attachInterface(this, "com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
        }

        public static IRecentsNonSystemUserCallbacks asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IRecentsNonSystemUserCallbacks)) {
                return (IRecentsNonSystemUserCallbacks) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Rect rect;
            if (i == 1598968902) {
                parcel2.writeString("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    preloadRecents();
                    return true;
                case 2:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    cancelPreloadingRecents();
                    return true;
                case 3:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    showRecents(parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt());
                    return true;
                case 4:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    hideRecents(parcel.readInt() != 0, parcel.readInt() != 0);
                    return true;
                case 5:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    toggleRecents(parcel.readInt());
                    return true;
                case 6:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    onConfigurationChanged();
                    return true;
                case 7:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    int i3 = parcel.readInt();
                    int i4 = parcel.readInt();
                    int i5 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        rect = (Rect) Rect.CREATOR.createFromParcel(parcel);
                    } else {
                        rect = null;
                    }
                    splitPrimaryTask(i3, i4, i5, rect);
                    return true;
                case 8:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    onDraggingInRecents(parcel.readFloat());
                    return true;
                case 9:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    onDraggingInRecentsEnded(parcel.readFloat());
                    return true;
                case 10:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    showCurrentUserToast(parcel.readInt(), parcel.readInt());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IRecentsNonSystemUserCallbacks {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void preloadRecents() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelPreloadingRecents() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showRecents(boolean z, boolean z2, boolean z3, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeInt(z3 ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void hideRecents(boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void toggleRecents(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onConfigurationChanged() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void splitPrimaryTask(int i, int i2, int i3, Rect rect) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onDraggingInRecents(float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onDraggingInRecentsEnded(float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showCurrentUserToast(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
