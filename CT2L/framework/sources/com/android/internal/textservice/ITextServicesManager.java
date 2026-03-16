package com.android.internal.textservice;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesSessionListener;

public interface ITextServicesManager extends IInterface {
    void finishSpellCheckerService(ISpellCheckerSessionListener iSpellCheckerSessionListener) throws RemoteException;

    SpellCheckerInfo getCurrentSpellChecker(String str) throws RemoteException;

    SpellCheckerSubtype getCurrentSpellCheckerSubtype(String str, boolean z) throws RemoteException;

    SpellCheckerInfo[] getEnabledSpellCheckers() throws RemoteException;

    void getSpellCheckerService(String str, String str2, ITextServicesSessionListener iTextServicesSessionListener, ISpellCheckerSessionListener iSpellCheckerSessionListener, Bundle bundle) throws RemoteException;

    boolean isSpellCheckerEnabled() throws RemoteException;

    void setCurrentSpellChecker(String str, String str2) throws RemoteException;

    void setCurrentSpellCheckerSubtype(String str, int i) throws RemoteException;

    void setSpellCheckerEnabled(boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements ITextServicesManager {
        private static final String DESCRIPTOR = "com.android.internal.textservice.ITextServicesManager";
        static final int TRANSACTION_finishSpellCheckerService = 4;
        static final int TRANSACTION_getCurrentSpellChecker = 1;
        static final int TRANSACTION_getCurrentSpellCheckerSubtype = 2;
        static final int TRANSACTION_getEnabledSpellCheckers = 9;
        static final int TRANSACTION_getSpellCheckerService = 3;
        static final int TRANSACTION_isSpellCheckerEnabled = 8;
        static final int TRANSACTION_setCurrentSpellChecker = 5;
        static final int TRANSACTION_setCurrentSpellCheckerSubtype = 6;
        static final int TRANSACTION_setSpellCheckerEnabled = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITextServicesManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITextServicesManager)) {
                return (ITextServicesManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle _arg4;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    SpellCheckerInfo _result = getCurrentSpellChecker(_arg0);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    boolean _arg1 = data.readInt() != 0;
                    SpellCheckerSubtype _result2 = getCurrentSpellCheckerSubtype(_arg02, _arg1);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    String _arg12 = data.readString();
                    ITextServicesSessionListener _arg2 = ITextServicesSessionListener.Stub.asInterface(data.readStrongBinder());
                    ISpellCheckerSessionListener _arg3 = ISpellCheckerSessionListener.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg4 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    getSpellCheckerService(_arg03, _arg12, _arg2, _arg3, _arg4);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    ISpellCheckerSessionListener _arg04 = ISpellCheckerSessionListener.Stub.asInterface(data.readStrongBinder());
                    finishSpellCheckerService(_arg04);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    String _arg13 = data.readString();
                    setCurrentSpellChecker(_arg05, _arg13);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    int _arg14 = data.readInt();
                    setCurrentSpellCheckerSubtype(_arg06, _arg14);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg07 = data.readInt() != 0;
                    setSpellCheckerEnabled(_arg07);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result3 = isSpellCheckerEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    SpellCheckerInfo[] _result4 = getEnabledSpellCheckers();
                    reply.writeNoException();
                    reply.writeTypedArray(_result4, 1);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ITextServicesManager {
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
            public SpellCheckerInfo getCurrentSpellChecker(String locale) throws RemoteException {
                SpellCheckerInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(locale);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = SpellCheckerInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public SpellCheckerSubtype getCurrentSpellCheckerSubtype(String locale, boolean allowImplicitlySelectedSubtype) throws RemoteException {
                SpellCheckerSubtype _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(locale);
                    _data.writeInt(allowImplicitlySelectedSubtype ? 1 : 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = SpellCheckerSubtype.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void getSpellCheckerService(String sciId, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, Bundle bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(sciId);
                    _data.writeString(locale);
                    _data.writeStrongBinder(tsListener != null ? tsListener.asBinder() : null);
                    _data.writeStrongBinder(scListener != null ? scListener.asBinder() : null);
                    if (bundle != null) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void finishSpellCheckerService(ISpellCheckerSessionListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setCurrentSpellChecker(String locale, String sciId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(locale);
                    _data.writeString(sciId);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setCurrentSpellCheckerSubtype(String locale, int hashCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(locale);
                    _data.writeInt(hashCode);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setSpellCheckerEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public boolean isSpellCheckerEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public SpellCheckerInfo[] getEnabledSpellCheckers() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    SpellCheckerInfo[] _result = (SpellCheckerInfo[]) _reply.createTypedArray(SpellCheckerInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
