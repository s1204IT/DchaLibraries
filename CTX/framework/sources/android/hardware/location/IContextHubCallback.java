package android.hardware.location;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IContextHubCallback extends IInterface {
    void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage) throws RemoteException;

    public static abstract class Stub extends Binder implements IContextHubCallback {
        private static final String DESCRIPTOR = "android.hardware.location.IContextHubCallback";
        static final int TRANSACTION_onMessageReceipt = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IContextHubCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IContextHubCallback)) {
                return (IContextHubCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ContextHubMessage contextHubMessageCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            int i3 = parcel.readInt();
            int i4 = parcel.readInt();
            if (parcel.readInt() != 0) {
                contextHubMessageCreateFromParcel = ContextHubMessage.CREATOR.createFromParcel(parcel);
            } else {
                contextHubMessageCreateFromParcel = null;
            }
            onMessageReceipt(i3, i4, contextHubMessageCreateFromParcel);
            return true;
        }

        private static class Proxy implements IContextHubCallback {
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
            public void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (contextHubMessage != null) {
                        parcelObtain.writeInt(1);
                        contextHubMessage.writeToParcel(parcelObtain, 0);
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
