package com.mediatek.internal.telecom;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.internal.telecom.ICallRecorderCallback;

public interface ICallRecorderService extends IInterface {
    void setCallback(ICallRecorderCallback iCallRecorderCallback) throws RemoteException;

    void startVoiceRecord() throws RemoteException;

    void stopVoiceRecord() throws RemoteException;

    public static abstract class Stub extends Binder implements ICallRecorderService {
        private static final String DESCRIPTOR = "com.mediatek.internal.telecom.ICallRecorderService";
        static final int TRANSACTION_setCallback = 3;
        static final int TRANSACTION_startVoiceRecord = 1;
        static final int TRANSACTION_stopVoiceRecord = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICallRecorderService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ICallRecorderService)) {
                return (ICallRecorderService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    startVoiceRecord();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopVoiceRecord();
                    return true;
                case TRANSACTION_setCallback:
                    parcel.enforceInterface(DESCRIPTOR);
                    setCallback(ICallRecorderCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ICallRecorderService {
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
            public void startVoiceRecord() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopVoiceRecord() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setCallback(ICallRecorderCallback iCallRecorderCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iCallRecorderCallback != null ? iCallRecorderCallback.asBinder() : null);
                    this.mRemote.transact(Stub.TRANSACTION_setCallback, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
