package android.hardware.hdmi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IHdmiInputChangeListener extends IInterface {
    void onChanged(HdmiDeviceInfo hdmiDeviceInfo) throws RemoteException;

    public static abstract class Stub extends Binder implements IHdmiInputChangeListener {
        private static final String DESCRIPTOR = "android.hardware.hdmi.IHdmiInputChangeListener";
        static final int TRANSACTION_onChanged = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IHdmiInputChangeListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IHdmiInputChangeListener)) {
                return (IHdmiInputChangeListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            HdmiDeviceInfo hdmiDeviceInfoCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                hdmiDeviceInfoCreateFromParcel = HdmiDeviceInfo.CREATOR.createFromParcel(parcel);
            } else {
                hdmiDeviceInfoCreateFromParcel = null;
            }
            onChanged(hdmiDeviceInfoCreateFromParcel);
            return true;
        }

        private static class Proxy implements IHdmiInputChangeListener {
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
            public void onChanged(HdmiDeviceInfo hdmiDeviceInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (hdmiDeviceInfo != null) {
                        parcelObtain.writeInt(1);
                        hdmiDeviceInfo.writeToParcel(parcelObtain, 0);
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
