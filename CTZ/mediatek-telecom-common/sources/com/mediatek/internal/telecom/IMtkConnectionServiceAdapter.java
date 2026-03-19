package com.mediatek.internal.telecom;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.ParcelableConference;

public interface IMtkConnectionServiceAdapter extends IInterface {
    void handleCreateConferenceComplete(String str, ConnectionRequest connectionRequest, ParcelableConference parcelableConference, DisconnectCause disconnectCause) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkConnectionServiceAdapter {
        private static final String DESCRIPTOR = "com.mediatek.internal.telecom.IMtkConnectionServiceAdapter";
        static final int TRANSACTION_handleCreateConferenceComplete = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkConnectionServiceAdapter asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkConnectionServiceAdapter)) {
                return (IMtkConnectionServiceAdapter) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ConnectionRequest connectionRequest;
            ParcelableConference parcelableConference;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                connectionRequest = (ConnectionRequest) ConnectionRequest.CREATOR.createFromParcel(parcel);
            } else {
                connectionRequest = null;
            }
            if (parcel.readInt() != 0) {
                parcelableConference = (ParcelableConference) ParcelableConference.CREATOR.createFromParcel(parcel);
            } else {
                parcelableConference = null;
            }
            handleCreateConferenceComplete(string, connectionRequest, parcelableConference, parcel.readInt() != 0 ? (DisconnectCause) DisconnectCause.CREATOR.createFromParcel(parcel) : null);
            return true;
        }

        private static class Proxy implements IMtkConnectionServiceAdapter {
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
            public void handleCreateConferenceComplete(String str, ConnectionRequest connectionRequest, ParcelableConference parcelableConference, DisconnectCause disconnectCause) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (connectionRequest != null) {
                        parcelObtain.writeInt(1);
                        connectionRequest.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (parcelableConference != null) {
                        parcelObtain.writeInt(1);
                        parcelableConference.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (disconnectCause != null) {
                        parcelObtain.writeInt(1);
                        disconnectCause.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
