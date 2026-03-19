package android.content.pm;

import android.content.IntentSender;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInstaller;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IPackageInstaller extends IInterface {
    void abandonSession(int i) throws RemoteException;

    int createSession(PackageInstaller.SessionParams sessionParams, String str, int i) throws RemoteException;

    ParceledListSlice getAllSessions(int i) throws RemoteException;

    ParceledListSlice getMySessions(String str, int i) throws RemoteException;

    PackageInstaller.SessionInfo getSessionInfo(int i) throws RemoteException;

    IPackageInstallerSession openSession(int i) throws RemoteException;

    void registerCallback(IPackageInstallerCallback iPackageInstallerCallback, int i) throws RemoteException;

    void setPermissionsResult(int i, boolean z) throws RemoteException;

    void uninstall(VersionedPackage versionedPackage, String str, int i, IntentSender intentSender, int i2) throws RemoteException;

    void unregisterCallback(IPackageInstallerCallback iPackageInstallerCallback) throws RemoteException;

    void updateSessionAppIcon(int i, Bitmap bitmap) throws RemoteException;

    void updateSessionAppLabel(int i, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IPackageInstaller {
        private static final String DESCRIPTOR = "android.content.pm.IPackageInstaller";
        static final int TRANSACTION_abandonSession = 4;
        static final int TRANSACTION_createSession = 1;
        static final int TRANSACTION_getAllSessions = 7;
        static final int TRANSACTION_getMySessions = 8;
        static final int TRANSACTION_getSessionInfo = 6;
        static final int TRANSACTION_openSession = 5;
        static final int TRANSACTION_registerCallback = 9;
        static final int TRANSACTION_setPermissionsResult = 12;
        static final int TRANSACTION_uninstall = 11;
        static final int TRANSACTION_unregisterCallback = 10;
        static final int TRANSACTION_updateSessionAppIcon = 2;
        static final int TRANSACTION_updateSessionAppLabel = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPackageInstaller asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IPackageInstaller)) {
                return (IPackageInstaller) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            VersionedPackage versionedPackageCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCreateSession = createSession(parcel.readInt() != 0 ? PackageInstaller.SessionParams.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCreateSession);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateSessionAppIcon(parcel.readInt(), parcel.readInt() != 0 ? Bitmap.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateSessionAppLabel(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    abandonSession(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    IPackageInstallerSession iPackageInstallerSessionOpenSession = openSession(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iPackageInstallerSessionOpenSession != null ? iPackageInstallerSessionOpenSession.asBinder() : null);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    PackageInstaller.SessionInfo sessionInfo = getSessionInfo(parcel.readInt());
                    parcel2.writeNoException();
                    if (sessionInfo != null) {
                        parcel2.writeInt(1);
                        sessionInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice allSessions = getAllSessions(parcel.readInt());
                    parcel2.writeNoException();
                    if (allSessions != null) {
                        parcel2.writeInt(1);
                        allSessions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice mySessions = getMySessions(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (mySessions != null) {
                        parcel2.writeInt(1);
                        mySessions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerCallback(IPackageInstallerCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterCallback(IPackageInstallerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        versionedPackageCreateFromParcel = VersionedPackage.CREATOR.createFromParcel(parcel);
                    } else {
                        versionedPackageCreateFromParcel = null;
                    }
                    uninstall(versionedPackageCreateFromParcel, parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? IntentSender.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPermissionsResult(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IPackageInstaller {
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
            public int createSession(PackageInstaller.SessionParams sessionParams, String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionParams != null) {
                        parcelObtain.writeInt(1);
                        sessionParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateSessionAppIcon(int i, Bitmap bitmap) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (bitmap != null) {
                        parcelObtain.writeInt(1);
                        bitmap.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateSessionAppLabel(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void abandonSession(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IPackageInstallerSession openSession(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IPackageInstallerSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PackageInstaller.SessionInfo getSessionInfo(int i) throws RemoteException {
                PackageInstaller.SessionInfo sessionInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        sessionInfoCreateFromParcel = PackageInstaller.SessionInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        sessionInfoCreateFromParcel = null;
                    }
                    return sessionInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getAllSessions(int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getMySessions(String str, int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerCallback(IPackageInstallerCallback iPackageInstallerCallback, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iPackageInstallerCallback != null ? iPackageInstallerCallback.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterCallback(IPackageInstallerCallback iPackageInstallerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iPackageInstallerCallback != null ? iPackageInstallerCallback.asBinder() : null);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void uninstall(VersionedPackage versionedPackage, String str, int i, IntentSender intentSender, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (versionedPackage != null) {
                        parcelObtain.writeInt(1);
                        versionedPackage.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (intentSender != null) {
                        parcelObtain.writeInt(1);
                        intentSender.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPermissionsResult(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
