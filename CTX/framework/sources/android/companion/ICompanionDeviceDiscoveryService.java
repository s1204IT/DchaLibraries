package android.companion;

import android.companion.ICompanionDeviceDiscoveryServiceCallback;
import android.companion.IFindDeviceCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ICompanionDeviceDiscoveryService extends IInterface {
    void startDiscovery(AssociationRequest associationRequest, String str, IFindDeviceCallback iFindDeviceCallback, ICompanionDeviceDiscoveryServiceCallback iCompanionDeviceDiscoveryServiceCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements ICompanionDeviceDiscoveryService {
        private static final String DESCRIPTOR = "android.companion.ICompanionDeviceDiscoveryService";
        static final int TRANSACTION_startDiscovery = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICompanionDeviceDiscoveryService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ICompanionDeviceDiscoveryService)) {
                return (ICompanionDeviceDiscoveryService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            AssociationRequest associationRequestCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                associationRequestCreateFromParcel = AssociationRequest.CREATOR.createFromParcel(parcel);
            } else {
                associationRequestCreateFromParcel = null;
            }
            startDiscovery(associationRequestCreateFromParcel, parcel.readString(), IFindDeviceCallback.Stub.asInterface(parcel.readStrongBinder()), ICompanionDeviceDiscoveryServiceCallback.Stub.asInterface(parcel.readStrongBinder()));
            parcel2.writeNoException();
            return true;
        }

        private static class Proxy implements ICompanionDeviceDiscoveryService {
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
            public void startDiscovery(AssociationRequest associationRequest, String str, IFindDeviceCallback iFindDeviceCallback, ICompanionDeviceDiscoveryServiceCallback iCompanionDeviceDiscoveryServiceCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (associationRequest != null) {
                        parcelObtain.writeInt(1);
                        associationRequest.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iFindDeviceCallback != null ? iFindDeviceCallback.asBinder() : null);
                    parcelObtain.writeStrongBinder(iCompanionDeviceDiscoveryServiceCallback != null ? iCompanionDeviceDiscoveryServiceCallback.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
