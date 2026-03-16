package com.android.ims.internal;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.ims.ImsCallForwardInfo;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsSsInfo;
import com.android.ims.internal.IImsUt;

public interface IImsUtListener extends IInterface {
    void utConfigurationCallBarringQueried(IImsUt iImsUt, int i, ImsSsInfo[] imsSsInfoArr) throws RemoteException;

    void utConfigurationCallForwardQueried(IImsUt iImsUt, int i, ImsCallForwardInfo[] imsCallForwardInfoArr) throws RemoteException;

    void utConfigurationCallWaitingQueried(IImsUt iImsUt, int i, ImsSsInfo[] imsSsInfoArr) throws RemoteException;

    void utConfigurationQueried(IImsUt iImsUt, int i, Bundle bundle) throws RemoteException;

    void utConfigurationQueryFailed(IImsUt iImsUt, int i, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void utConfigurationUpdateFailed(IImsUt iImsUt, int i, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void utConfigurationUpdated(IImsUt iImsUt, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsUtListener {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsUtListener";
        static final int TRANSACTION_utConfigurationCallBarringQueried = 5;
        static final int TRANSACTION_utConfigurationCallForwardQueried = 6;
        static final int TRANSACTION_utConfigurationCallWaitingQueried = 7;
        static final int TRANSACTION_utConfigurationQueried = 3;
        static final int TRANSACTION_utConfigurationQueryFailed = 4;
        static final int TRANSACTION_utConfigurationUpdateFailed = 2;
        static final int TRANSACTION_utConfigurationUpdated = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsUtListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IImsUtListener)) {
                return (IImsUtListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ImsReasonInfo _arg2;
            Bundle _arg22;
            ImsReasonInfo _arg23;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IImsUt _arg0 = IImsUt.Stub.asInterface(data.readStrongBinder());
                    int _arg1 = data.readInt();
                    utConfigurationUpdated(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IImsUt _arg02 = IImsUt.Stub.asInterface(data.readStrongBinder());
                    int _arg12 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg23 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    utConfigurationUpdateFailed(_arg02, _arg12, _arg23);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IImsUt _arg03 = IImsUt.Stub.asInterface(data.readStrongBinder());
                    int _arg13 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg22 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    utConfigurationQueried(_arg03, _arg13, _arg22);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IImsUt _arg04 = IImsUt.Stub.asInterface(data.readStrongBinder());
                    int _arg14 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg2 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    utConfigurationQueryFailed(_arg04, _arg14, _arg2);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IImsUt _arg05 = IImsUt.Stub.asInterface(data.readStrongBinder());
                    int _arg15 = data.readInt();
                    ImsSsInfo[] _arg24 = (ImsSsInfo[]) data.createTypedArray(ImsSsInfo.CREATOR);
                    utConfigurationCallBarringQueried(_arg05, _arg15, _arg24);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IImsUt _arg06 = IImsUt.Stub.asInterface(data.readStrongBinder());
                    int _arg16 = data.readInt();
                    ImsCallForwardInfo[] _arg25 = (ImsCallForwardInfo[]) data.createTypedArray(ImsCallForwardInfo.CREATOR);
                    utConfigurationCallForwardQueried(_arg06, _arg16, _arg25);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IImsUt _arg07 = IImsUt.Stub.asInterface(data.readStrongBinder());
                    int _arg17 = data.readInt();
                    ImsSsInfo[] _arg26 = (ImsSsInfo[]) data.createTypedArray(ImsSsInfo.CREATOR);
                    utConfigurationCallWaitingQueried(_arg07, _arg17, _arg26);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IImsUtListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void utConfigurationUpdated(IImsUt ut, int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(ut != null ? ut.asBinder() : null);
                    _data.writeInt(id);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void utConfigurationUpdateFailed(IImsUt ut, int id, ImsReasonInfo error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(ut != null ? ut.asBinder() : null);
                    _data.writeInt(id);
                    if (error != null) {
                        _data.writeInt(1);
                        error.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void utConfigurationQueried(IImsUt ut, int id, Bundle ssInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(ut != null ? ut.asBinder() : null);
                    _data.writeInt(id);
                    if (ssInfo != null) {
                        _data.writeInt(1);
                        ssInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void utConfigurationQueryFailed(IImsUt ut, int id, ImsReasonInfo error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(ut != null ? ut.asBinder() : null);
                    _data.writeInt(id);
                    if (error != null) {
                        _data.writeInt(1);
                        error.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void utConfigurationCallBarringQueried(IImsUt ut, int id, ImsSsInfo[] cbInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(ut != null ? ut.asBinder() : null);
                    _data.writeInt(id);
                    _data.writeTypedArray(cbInfo, 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void utConfigurationCallForwardQueried(IImsUt ut, int id, ImsCallForwardInfo[] cfInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(ut != null ? ut.asBinder() : null);
                    _data.writeInt(id);
                    _data.writeTypedArray(cfInfo, 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void utConfigurationCallWaitingQueried(IImsUt ut, int id, ImsSsInfo[] cwInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(ut != null ? ut.asBinder() : null);
                    _data.writeInt(id);
                    _data.writeTypedArray(cwInfo, 0);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
