package com.android.systemui.shared.recents;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.systemui.shared.system.GraphicBufferCompat;

/* loaded from: classes.dex */
public interface ISystemUiProxy extends IInterface {
    Rect getNonMinimizedSplitScreenSecondaryBounds() throws RemoteException;

    void onOverviewShown(boolean z) throws RemoteException;

    void onSplitScreenInvoked() throws RemoteException;

    GraphicBufferCompat screenshot(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) throws RemoteException;

    void setBackButtonAlpha(float f, boolean z) throws RemoteException;

    void setInteractionState(int i) throws RemoteException;

    void startScreenPinning(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements ISystemUiProxy {
        public Stub() {
            attachInterface(this, "com.android.systemui.shared.recents.ISystemUiProxy");
        }

        public static ISystemUiProxy asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.android.systemui.shared.recents.ISystemUiProxy");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ISystemUiProxy)) {
                return (ISystemUiProxy) iInterfaceQueryLocalInterface;
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
                parcel2.writeString("com.android.systemui.shared.recents.ISystemUiProxy");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.android.systemui.shared.recents.ISystemUiProxy");
                    if (parcel.readInt() != 0) {
                        rect = (Rect) Rect.CREATOR.createFromParcel(parcel);
                    } else {
                        rect = null;
                    }
                    GraphicBufferCompat graphicBufferCompatScreenshot = screenshot(rect, parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    if (graphicBufferCompatScreenshot != null) {
                        parcel2.writeInt(1);
                        graphicBufferCompatScreenshot.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface("com.android.systemui.shared.recents.ISystemUiProxy");
                    startScreenPinning(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                default:
                    switch (i) {
                        case 5:
                            parcel.enforceInterface("com.android.systemui.shared.recents.ISystemUiProxy");
                            setInteractionState(parcel.readInt());
                            parcel2.writeNoException();
                            return true;
                        case 6:
                            parcel.enforceInterface("com.android.systemui.shared.recents.ISystemUiProxy");
                            onSplitScreenInvoked();
                            parcel2.writeNoException();
                            return true;
                        case 7:
                            parcel.enforceInterface("com.android.systemui.shared.recents.ISystemUiProxy");
                            onOverviewShown(parcel.readInt() != 0);
                            parcel2.writeNoException();
                            return true;
                        case 8:
                            parcel.enforceInterface("com.android.systemui.shared.recents.ISystemUiProxy");
                            Rect nonMinimizedSplitScreenSecondaryBounds = getNonMinimizedSplitScreenSecondaryBounds();
                            parcel2.writeNoException();
                            if (nonMinimizedSplitScreenSecondaryBounds != null) {
                                parcel2.writeInt(1);
                                nonMinimizedSplitScreenSecondaryBounds.writeToParcel(parcel2, 1);
                            } else {
                                parcel2.writeInt(0);
                            }
                            return true;
                        case 9:
                            parcel.enforceInterface("com.android.systemui.shared.recents.ISystemUiProxy");
                            setBackButtonAlpha(parcel.readFloat(), parcel.readInt() != 0);
                            parcel2.writeNoException();
                            return true;
                        default:
                            return super.onTransact(i, parcel, parcel2, i2);
                    }
            }
        }

        private static class Proxy implements ISystemUiProxy {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.android.systemui.shared.recents.ISystemUiProxy
            public GraphicBufferCompat screenshot(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) throws RemoteException {
                GraphicBufferCompat graphicBufferCompatCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i5);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        graphicBufferCompatCreateFromParcel = GraphicBufferCompat.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        graphicBufferCompatCreateFromParcel = null;
                    }
                    return graphicBufferCompatCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.shared.recents.ISystemUiProxy
            public void startScreenPinning(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.shared.recents.ISystemUiProxy
            public void setInteractionState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.shared.recents.ISystemUiProxy
            public void onSplitScreenInvoked() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.shared.recents.ISystemUiProxy
            public void onOverviewShown(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.shared.recents.ISystemUiProxy
            public Rect getNonMinimizedSplitScreenSecondaryBounds() throws RemoteException {
                Rect rect;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        rect = (Rect) Rect.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        rect = null;
                    }
                    return rect;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.android.systemui.shared.recents.ISystemUiProxy
            public void setBackButtonAlpha(float f, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    parcelObtain.writeFloat(f);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
