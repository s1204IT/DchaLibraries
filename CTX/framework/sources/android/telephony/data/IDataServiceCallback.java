package android.telephony.data;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IDataServiceCallback extends IInterface {
    void onDataCallListChanged(List<DataCallResponse> list) throws RemoteException;

    void onDeactivateDataCallComplete(int i) throws RemoteException;

    void onGetDataCallListComplete(int i, List<DataCallResponse> list) throws RemoteException;

    void onSetDataProfileComplete(int i) throws RemoteException;

    void onSetInitialAttachApnComplete(int i) throws RemoteException;

    void onSetupDataCallComplete(int i, DataCallResponse dataCallResponse) throws RemoteException;

    public static abstract class Stub extends Binder implements IDataServiceCallback {
        private static final String DESCRIPTOR = "android.telephony.data.IDataServiceCallback";
        static final int TRANSACTION_onDataCallListChanged = 6;
        static final int TRANSACTION_onDeactivateDataCallComplete = 2;
        static final int TRANSACTION_onGetDataCallListComplete = 5;
        static final int TRANSACTION_onSetDataProfileComplete = 4;
        static final int TRANSACTION_onSetInitialAttachApnComplete = 3;
        static final int TRANSACTION_onSetupDataCallComplete = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDataServiceCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IDataServiceCallback)) {
                return (IDataServiceCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            DataCallResponse dataCallResponseCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        dataCallResponseCreateFromParcel = DataCallResponse.CREATOR.createFromParcel(parcel);
                    } else {
                        dataCallResponseCreateFromParcel = null;
                    }
                    onSetupDataCallComplete(i3, dataCallResponseCreateFromParcel);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onDeactivateDataCallComplete(parcel.readInt());
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSetInitialAttachApnComplete(parcel.readInt());
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSetDataProfileComplete(parcel.readInt());
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    onGetDataCallListComplete(parcel.readInt(), parcel.createTypedArrayList(DataCallResponse.CREATOR));
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    onDataCallListChanged(parcel.createTypedArrayList(DataCallResponse.CREATOR));
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IDataServiceCallback {
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
            public void onSetupDataCallComplete(int i, DataCallResponse dataCallResponse) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (dataCallResponse != null) {
                        parcelObtain.writeInt(1);
                        dataCallResponse.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onDeactivateDataCallComplete(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSetInitialAttachApnComplete(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSetDataProfileComplete(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGetDataCallListComplete(int i, List<DataCallResponse> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeTypedList(list);
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onDataCallListChanged(List<DataCallResponse> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeTypedList(list);
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
