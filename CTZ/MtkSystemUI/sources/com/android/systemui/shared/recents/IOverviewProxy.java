package com.android.systemui.shared.recents;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.MotionEvent;
import com.android.systemui.shared.recents.ISystemUiProxy;

public interface IOverviewProxy extends IInterface {
    void onBind(ISystemUiProxy iSystemUiProxy) throws RemoteException;

    void onMotionEvent(MotionEvent motionEvent) throws RemoteException;

    void onOverviewHidden(boolean z, boolean z2) throws RemoteException;

    void onOverviewShown(boolean z) throws RemoteException;

    void onOverviewToggle() throws RemoteException;

    void onPreMotionEvent(int i) throws RemoteException;

    void onQuickScrubEnd() throws RemoteException;

    void onQuickScrubProgress(float f) throws RemoteException;

    void onQuickScrubStart() throws RemoteException;

    void onQuickStep(MotionEvent motionEvent) throws RemoteException;

    void onTip(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IOverviewProxy {
        public static IOverviewProxy asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.android.systemui.shared.recents.IOverviewProxy");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IOverviewProxy)) {
                return (IOverviewProxy) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("com.android.systemui.shared.recents.IOverviewProxy");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onBind(ISystemUiProxy.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 2:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onPreMotionEvent(parcel.readInt());
                    return true;
                case 3:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onMotionEvent(parcel.readInt() != 0 ? (MotionEvent) MotionEvent.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 4:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onQuickScrubStart();
                    return true;
                case 5:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onQuickScrubEnd();
                    return true;
                case 6:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onQuickScrubProgress(parcel.readFloat());
                    return true;
                case 7:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onOverviewToggle();
                    return true;
                case 8:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onOverviewShown(parcel.readInt() != 0);
                    return true;
                case 9:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onOverviewHidden(parcel.readInt() != 0, parcel.readInt() != 0);
                    return true;
                case 10:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onQuickStep(parcel.readInt() != 0 ? (MotionEvent) MotionEvent.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 11:
                    parcel.enforceInterface("com.android.systemui.shared.recents.IOverviewProxy");
                    onTip(parcel.readInt(), parcel.readInt());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IOverviewProxy {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void onBind(ISystemUiProxy iSystemUiProxy) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    parcelObtain.writeStrongBinder(iSystemUiProxy != null ? iSystemUiProxy.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPreMotionEvent(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onMotionEvent(MotionEvent motionEvent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    if (motionEvent != null) {
                        parcelObtain.writeInt(1);
                        motionEvent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onQuickScrubStart() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onQuickScrubEnd() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onQuickScrubProgress(float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onOverviewToggle() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onOverviewShown(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onOverviewHidden(boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onQuickStep(MotionEvent motionEvent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    if (motionEvent != null) {
                        parcelObtain.writeInt(1);
                        motionEvent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onTip(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
