package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;

public interface IOnAppsChangedListener extends IInterface {
    void onPackageAdded(UserHandle userHandle, String str) throws RemoteException;

    void onPackageChanged(UserHandle userHandle, String str) throws RemoteException;

    void onPackageRemoved(UserHandle userHandle, String str) throws RemoteException;

    void onPackagesAvailable(UserHandle userHandle, String[] strArr, boolean z) throws RemoteException;

    void onPackagesUnavailable(UserHandle userHandle, String[] strArr, boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements IOnAppsChangedListener {
        private static final String DESCRIPTOR = "android.content.pm.IOnAppsChangedListener";
        static final int TRANSACTION_onPackageAdded = 2;
        static final int TRANSACTION_onPackageChanged = 3;
        static final int TRANSACTION_onPackageRemoved = 1;
        static final int TRANSACTION_onPackagesAvailable = 4;
        static final int TRANSACTION_onPackagesUnavailable = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOnAppsChangedListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOnAppsChangedListener)) {
                return (IOnAppsChangedListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            UserHandle _arg0;
            boolean _arg2;
            UserHandle _arg02;
            UserHandle _arg03;
            UserHandle _arg04;
            UserHandle _arg05;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg05 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg05 = null;
                    }
                    String _arg1 = data.readString();
                    onPackageRemoved(_arg05, _arg1);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    String _arg12 = data.readString();
                    onPackageAdded(_arg04, _arg12);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    String _arg13 = data.readString();
                    onPackageChanged(_arg03, _arg13);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    String[] _arg14 = data.createStringArray();
                    _arg2 = data.readInt() != 0;
                    onPackagesAvailable(_arg02, _arg14, _arg2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    String[] _arg15 = data.createStringArray();
                    _arg2 = data.readInt() != 0;
                    onPackagesUnavailable(_arg0, _arg15, _arg2);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IOnAppsChangedListener {
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
            public void onPackageRemoved(UserHandle user, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (user != null) {
                        _data.writeInt(1);
                        user.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPackageAdded(UserHandle user, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (user != null) {
                        _data.writeInt(1);
                        user.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPackageChanged(UserHandle user, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (user != null) {
                        _data.writeInt(1);
                        user.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPackagesAvailable(UserHandle user, String[] packageNames, boolean replacing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (user != null) {
                        _data.writeInt(1);
                        user.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(packageNames);
                    _data.writeInt(replacing ? 1 : 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPackagesUnavailable(UserHandle user, String[] packageNames, boolean replacing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (user != null) {
                        _data.writeInt(1);
                        user.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(packageNames);
                    _data.writeInt(replacing ? 1 : 0);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
