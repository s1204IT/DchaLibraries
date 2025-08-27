package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IRecentsSystemUserCallbacks extends IInterface {
    void registerNonSystemUserCallbacks(IBinder iBinder, int i) throws RemoteException;

    void sendDockedFirstAnimationFrameEvent() throws RemoteException;

    void sendDockingTopTaskEvent(int i, Rect rect) throws RemoteException;

    void sendLaunchRecentsEvent() throws RemoteException;

    void sendRecentsDrawnEvent() throws RemoteException;

    void setWaitingForTransitionStartEvent(boolean z) throws RemoteException;

    void startScreenPinning(int i) throws RemoteException;

    void updateRecentsVisibility(boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements IRecentsSystemUserCallbacks {
        public Stub() {
            attachInterface(this, "com.android.systemui.recents.IRecentsSystemUserCallbacks");
        }

        public static IRecentsSystemUserCallbacks asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IRecentsSystemUserCallbacks)) {
                return (IRecentsSystemUserCallbacks) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Rect rect;
            if (i == 1598968902) {
                parcel2.writeString("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    registerNonSystemUserCallbacks(parcel.readStrongBinder(), parcel.readInt());
                    return true;
                case 2:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    updateRecentsVisibility(parcel.readInt() != 0);
                    return true;
                case 3:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    startScreenPinning(parcel.readInt());
                    return true;
                case 4:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    sendRecentsDrawnEvent();
                    return true;
                case 5:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    int i3 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        rect = (Rect) Rect.CREATOR.createFromParcel(parcel);
                    } else {
                        rect = null;
                    }
                    sendDockingTopTaskEvent(i3, rect);
                    return true;
                case 6:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    sendLaunchRecentsEvent();
                    return true;
                case 7:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    sendDockedFirstAnimationFrameEvent();
                    return true;
                case 8:
                    parcel.enforceInterface("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    setWaitingForTransitionStartEvent(parcel.readInt() != 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IRecentsSystemUserCallbacks {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void registerNonSystemUserCallbacks(IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void updateRecentsVisibility(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void startScreenPinning(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void sendRecentsDrawnEvent() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void sendDockingTopTaskEvent(int i, Rect rect) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    parcelObtain.writeInt(i);
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void sendLaunchRecentsEvent() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void sendDockedFirstAnimationFrameEvent() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
            public void setWaitingForTransitionStartEvent(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.recents.IRecentsSystemUserCallbacks");
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
