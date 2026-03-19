package com.mediatek.advcam;

import android.hardware.camera2.CaptureRequest;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IAdvCamService extends IInterface {
    int setConfigureParam(int i, CaptureRequest captureRequest) throws RemoteException;

    public static abstract class Stub extends Binder implements IAdvCamService {
        private static final String DESCRIPTOR = "com.mediatek.advcam.IAdvCamService";
        static final int TRANSACTION_setConfigureParam = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAdvCamService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IAdvCamService)) {
                return (IAdvCamService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            CaptureRequest captureRequest;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            int i3 = parcel.readInt();
            if (parcel.readInt() != 0) {
                captureRequest = (CaptureRequest) CaptureRequest.CREATOR.createFromParcel(parcel);
            } else {
                captureRequest = null;
            }
            int configureParam = setConfigureParam(i3, captureRequest);
            parcel2.writeNoException();
            parcel2.writeInt(configureParam);
            return true;
        }

        private static class Proxy implements IAdvCamService {
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
            public int setConfigureParam(int i, CaptureRequest captureRequest) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (captureRequest != null) {
                        parcelObtain.writeInt(1);
                        captureRequest.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
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
