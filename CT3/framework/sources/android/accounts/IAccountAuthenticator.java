package android.accounts;

import android.accounts.IAccountAuthenticatorResponse;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IAccountAuthenticator extends IInterface {
    void addAccount(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str, String str2, String[] strArr, Bundle bundle) throws RemoteException;

    void addAccountFromCredentials(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, Bundle bundle) throws RemoteException;

    void confirmCredentials(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, Bundle bundle) throws RemoteException;

    void editProperties(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str) throws RemoteException;

    void finishSession(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str, Bundle bundle) throws RemoteException;

    void getAccountCredentialsForCloning(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account) throws RemoteException;

    void getAccountRemovalAllowed(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account) throws RemoteException;

    void getAuthToken(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str, Bundle bundle) throws RemoteException;

    void getAuthTokenLabel(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str) throws RemoteException;

    void hasFeatures(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String[] strArr) throws RemoteException;

    void isCredentialsUpdateSuggested(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str) throws RemoteException;

    void startAddAccountSession(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str, String str2, String[] strArr, Bundle bundle) throws RemoteException;

    void startUpdateCredentialsSession(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str, Bundle bundle) throws RemoteException;

    void updateCredentials(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str, Bundle bundle) throws RemoteException;

    public static abstract class Stub extends Binder implements IAccountAuthenticator {
        private static final String DESCRIPTOR = "android.accounts.IAccountAuthenticator";
        static final int TRANSACTION_addAccount = 1;
        static final int TRANSACTION_addAccountFromCredentials = 10;
        static final int TRANSACTION_confirmCredentials = 2;
        static final int TRANSACTION_editProperties = 6;
        static final int TRANSACTION_finishSession = 13;
        static final int TRANSACTION_getAccountCredentialsForCloning = 9;
        static final int TRANSACTION_getAccountRemovalAllowed = 8;
        static final int TRANSACTION_getAuthToken = 3;
        static final int TRANSACTION_getAuthTokenLabel = 4;
        static final int TRANSACTION_hasFeatures = 7;
        static final int TRANSACTION_isCredentialsUpdateSuggested = 14;
        static final int TRANSACTION_startAddAccountSession = 11;
        static final int TRANSACTION_startUpdateCredentialsSession = 12;
        static final int TRANSACTION_updateCredentials = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAccountAuthenticator asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAccountAuthenticator)) {
                return (IAccountAuthenticator) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Account accountCreateFromParcel;
            Bundle bundleCreateFromParcel;
            Account accountCreateFromParcel2;
            Bundle bundleCreateFromParcel2;
            Bundle bundleCreateFromParcel3;
            Account accountCreateFromParcel3;
            Bundle bundleCreateFromParcel4;
            Account accountCreateFromParcel4;
            Account accountCreateFromParcel5;
            Account accountCreateFromParcel6;
            Account accountCreateFromParcel7;
            Bundle bundleCreateFromParcel5;
            Account accountCreateFromParcel8;
            Bundle bundleCreateFromParcel6;
            Account accountCreateFromParcel9;
            Bundle bundleCreateFromParcel7;
            Bundle bundleCreateFromParcel8;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg0 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    String _arg1 = data.readString();
                    String _arg2 = data.readString();
                    String[] _arg3 = data.createStringArray();
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel8 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel8 = null;
                    }
                    addAccount(_arg0, _arg1, _arg2, _arg3, bundleCreateFromParcel8);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg02 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel9 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel9 = null;
                    }
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel7 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel7 = null;
                    }
                    confirmCredentials(_arg02, accountCreateFromParcel9, bundleCreateFromParcel7);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg03 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel8 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel8 = null;
                    }
                    String _arg22 = data.readString();
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel6 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel6 = null;
                    }
                    getAuthToken(_arg03, accountCreateFromParcel8, _arg22, bundleCreateFromParcel6);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg04 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    String _arg12 = data.readString();
                    getAuthTokenLabel(_arg04, _arg12);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg05 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel7 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel7 = null;
                    }
                    String _arg23 = data.readString();
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel5 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel5 = null;
                    }
                    updateCredentials(_arg05, accountCreateFromParcel7, _arg23, bundleCreateFromParcel5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg06 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    String _arg13 = data.readString();
                    editProperties(_arg06, _arg13);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg07 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel6 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel6 = null;
                    }
                    String[] _arg24 = data.createStringArray();
                    hasFeatures(_arg07, accountCreateFromParcel6, _arg24);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg08 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel5 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel5 = null;
                    }
                    getAccountRemovalAllowed(_arg08, accountCreateFromParcel5);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg09 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel4 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel4 = null;
                    }
                    getAccountCredentialsForCloning(_arg09, accountCreateFromParcel4);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg010 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel3 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel3 = null;
                    }
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel4 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel4 = null;
                    }
                    addAccountFromCredentials(_arg010, accountCreateFromParcel3, bundleCreateFromParcel4);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg011 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    String _arg14 = data.readString();
                    String _arg25 = data.readString();
                    String[] _arg32 = data.createStringArray();
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel3 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel3 = null;
                    }
                    startAddAccountSession(_arg011, _arg14, _arg25, _arg32, bundleCreateFromParcel3);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg012 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel2 = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel2 = null;
                    }
                    String _arg26 = data.readString();
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel2 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel2 = null;
                    }
                    startUpdateCredentialsSession(_arg012, accountCreateFromParcel2, _arg26, bundleCreateFromParcel2);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg013 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    String _arg15 = data.readString();
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    finishSession(_arg013, _arg15, bundleCreateFromParcel);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    IAccountAuthenticatorResponse _arg014 = IAccountAuthenticatorResponse.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        accountCreateFromParcel = Account.CREATOR.createFromParcel(data);
                    } else {
                        accountCreateFromParcel = null;
                    }
                    String _arg27 = data.readString();
                    isCredentialsUpdateSuggested(_arg014, accountCreateFromParcel, _arg27);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IAccountAuthenticator {
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
            public void addAccount(IAccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    _data.writeString(accountType);
                    _data.writeString(authTokenType);
                    _data.writeStringArray(requiredFeatures);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void confirmCredentials(IAccountAuthenticatorResponse response, Account account, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getAuthToken(IAccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(authTokenType);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getAuthTokenLabel(IAccountAuthenticatorResponse response, String authTokenType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    _data.writeString(authTokenType);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void updateCredentials(IAccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(authTokenType);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void editProperties(IAccountAuthenticatorResponse response, String accountType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    _data.writeString(accountType);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void hasFeatures(IAccountAuthenticatorResponse response, Account account, String[] features) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(features);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getAccountRemovalAllowed(IAccountAuthenticatorResponse response, Account account) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getAccountCredentialsForCloning(IAccountAuthenticatorResponse response, Account account) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void addAccountFromCredentials(IAccountAuthenticatorResponse response, Account account, Bundle accountCredentials) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (accountCredentials != null) {
                        _data.writeInt(1);
                        accountCredentials.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void startAddAccountSession(IAccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    _data.writeString(accountType);
                    _data.writeString(authTokenType);
                    _data.writeStringArray(requiredFeatures);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void startUpdateCredentialsSession(IAccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(authTokenType);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void finishSession(IAccountAuthenticatorResponse response, String accountType, Bundle sessionBundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    _data.writeString(accountType);
                    if (sessionBundle != null) {
                        _data.writeInt(1);
                        sessionBundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void isCredentialsUpdateSuggested(IAccountAuthenticatorResponse response, Account account, String statusToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(response != null ? response.asBinder() : null);
                    if (account != null) {
                        _data.writeInt(1);
                        account.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(statusToken);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
