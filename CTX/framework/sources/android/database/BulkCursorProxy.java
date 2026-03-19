package android.database;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

final class BulkCursorProxy implements IBulkCursor {
    private Bundle mExtras = null;
    private IBinder mRemote;

    public BulkCursorProxy(IBinder iBinder) {
        this.mRemote = iBinder;
    }

    @Override
    public IBinder asBinder() {
        return this.mRemote;
    }

    @Override
    public CursorWindow getWindow(int i) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IBulkCursor.descriptor);
            parcelObtain.writeInt(i);
            this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            CursorWindow cursorWindowNewFromParcel = null;
            if (parcelObtain2.readInt() == 1) {
                cursorWindowNewFromParcel = CursorWindow.newFromParcel(parcelObtain2);
            }
            return cursorWindowNewFromParcel;
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public void onMove(int i) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IBulkCursor.descriptor);
            parcelObtain.writeInt(i);
            this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public void deactivate() throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IBulkCursor.descriptor);
            this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public void close() throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IBulkCursor.descriptor);
            this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public int requery(IContentObserver iContentObserver) throws RemoteException {
        int i;
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IBulkCursor.descriptor);
            parcelObtain.writeStrongInterface(iContentObserver);
            boolean zTransact = this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            if (!zTransact) {
                i = -1;
            } else {
                i = parcelObtain2.readInt();
                this.mExtras = parcelObtain2.readBundle();
            }
            return i;
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public Bundle getExtras() throws RemoteException {
        if (this.mExtras == null) {
            Parcel parcelObtain = Parcel.obtain();
            Parcel parcelObtain2 = Parcel.obtain();
            try {
                parcelObtain.writeInterfaceToken(IBulkCursor.descriptor);
                this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                DatabaseUtils.readExceptionFromParcel(parcelObtain2);
                this.mExtras = parcelObtain2.readBundle();
            } finally {
                parcelObtain.recycle();
                parcelObtain2.recycle();
            }
        }
        return this.mExtras;
    }

    @Override
    public Bundle respond(Bundle bundle) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IBulkCursor.descriptor);
            parcelObtain.writeBundle(bundle);
            this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.readBundle();
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }
}
