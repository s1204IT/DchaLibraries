package android.database;

import android.database.IContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

public abstract class BulkCursorNative extends Binder implements IBulkCursor {
    public BulkCursorNative() {
        attachInterface(this, IBulkCursor.descriptor);
    }

    public static IBulkCursor asInterface(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IBulkCursor iBulkCursor = (IBulkCursor) iBinder.queryLocalInterface(IBulkCursor.descriptor);
        if (iBulkCursor != null) {
            return iBulkCursor;
        }
        return new BulkCursorProxy(iBinder);
    }

    @Override
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            switch (i) {
                case 1:
                    parcel.enforceInterface(IBulkCursor.descriptor);
                    CursorWindow window = getWindow(parcel.readInt());
                    parcel2.writeNoException();
                    if (window == null) {
                        parcel2.writeInt(0);
                    } else {
                        parcel2.writeInt(1);
                        window.writeToParcel(parcel2, 1);
                    }
                    break;
                case 2:
                    parcel.enforceInterface(IBulkCursor.descriptor);
                    deactivate();
                    parcel2.writeNoException();
                    break;
                case 3:
                    parcel.enforceInterface(IBulkCursor.descriptor);
                    int iRequery = requery(IContentObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(iRequery);
                    parcel2.writeBundle(getExtras());
                    break;
                case 4:
                    parcel.enforceInterface(IBulkCursor.descriptor);
                    onMove(parcel.readInt());
                    parcel2.writeNoException();
                    break;
                case 5:
                    parcel.enforceInterface(IBulkCursor.descriptor);
                    Bundle extras = getExtras();
                    parcel2.writeNoException();
                    parcel2.writeBundle(extras);
                    break;
                case 6:
                    parcel.enforceInterface(IBulkCursor.descriptor);
                    Bundle bundleRespond = respond(parcel.readBundle());
                    parcel2.writeNoException();
                    parcel2.writeBundle(bundleRespond);
                    break;
                case 7:
                    parcel.enforceInterface(IBulkCursor.descriptor);
                    close();
                    parcel2.writeNoException();
                    break;
            }
            return true;
        } catch (Exception e) {
            DatabaseUtils.writeExceptionToParcel(parcel2, e);
            return true;
        }
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
