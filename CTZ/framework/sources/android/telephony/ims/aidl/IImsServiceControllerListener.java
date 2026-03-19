package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.stub.ImsFeatureConfiguration;

public interface IImsServiceControllerListener extends IInterface {
    void onUpdateSupportedImsFeatures(ImsFeatureConfiguration imsFeatureConfiguration) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsServiceControllerListener {
        private static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsServiceControllerListener";
        static final int TRANSACTION_onUpdateSupportedImsFeatures = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsServiceControllerListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IImsServiceControllerListener)) {
                return (IImsServiceControllerListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ImsFeatureConfiguration imsFeatureConfigurationCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                imsFeatureConfigurationCreateFromParcel = ImsFeatureConfiguration.CREATOR.createFromParcel(parcel);
            } else {
                imsFeatureConfigurationCreateFromParcel = null;
            }
            onUpdateSupportedImsFeatures(imsFeatureConfigurationCreateFromParcel);
            return true;
        }

        private static class Proxy implements IImsServiceControllerListener {
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
            public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration imsFeatureConfiguration) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (imsFeatureConfiguration != null) {
                        parcelObtain.writeInt(1);
                        imsFeatureConfiguration.writeToParcel(parcelObtain, 0);
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
