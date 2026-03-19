package com.mediatek.common.multiwindow;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMWAmsCallback extends IInterface {
    String findProcessNameByToken(IBinder iBinder) throws RemoteException;

    int findStackIdByTask(int i) throws RemoteException;

    int findStackIdByToken(IBinder iBinder) throws RemoteException;

    boolean moveActivityTaskToFront(IBinder iBinder) throws RemoteException;

    void restoreStack(IBinder iBinder, boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements IMWAmsCallback {
        private static final String DESCRIPTOR = "com.mediatek.common.multiwindow.IMWAmsCallback";
        static final int TRANSACTION_findProcessNameByToken = 2;
        static final int TRANSACTION_findStackIdByTask = 5;
        static final int TRANSACTION_findStackIdByToken = 4;
        static final int TRANSACTION_moveActivityTaskToFront = 3;
        static final int TRANSACTION_restoreStack = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMWAmsCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMWAmsCallback)) {
                return (IMWAmsCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            boolean z;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder = parcel.readStrongBinder();
                    if (parcel.readInt() == 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    restoreStack(strongBinder, z);
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String strFindProcessNameByToken = findProcessNameByToken(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeString(strFindProcessNameByToken);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMoveActivityTaskToFront = moveActivityTaskToFront(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zMoveActivityTaskToFront ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iFindStackIdByToken = findStackIdByToken(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(iFindStackIdByToken);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iFindStackIdByTask = findStackIdByTask(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iFindStackIdByTask);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMWAmsCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void restoreStack(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String findProcessNameByToken(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean moveActivityTaskToFront(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int findStackIdByToken(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int findStackIdByTask(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
