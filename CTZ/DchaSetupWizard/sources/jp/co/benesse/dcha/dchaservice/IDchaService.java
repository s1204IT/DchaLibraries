package jp.co.benesse.dcha.dchaservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IDchaService extends IInterface {

    public static class Default implements IDchaService {
        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void cancelSetup() throws RemoteException {
        }

        @Override
        public boolean checkPadRooted() throws RemoteException {
            return false;
        }

        @Override
        public void clearDefaultPreferredApp(String str) throws RemoteException {
        }

        @Override
        public boolean copyFile(String str, String str2) throws RemoteException {
            return false;
        }

        @Override
        public boolean copyUpdateImage(String str, String str2) throws RemoteException {
            return false;
        }

        @Override
        public boolean deleteFile(String str) throws RemoteException {
            return false;
        }

        @Override
        public void disableADB() throws RemoteException {
        }

        @Override
        public String getCanonicalExternalPath(String str) throws RemoteException {
            return null;
        }

        @Override
        public String getForegroundPackageName() throws RemoteException {
            return null;
        }

        @Override
        public int getSetupStatus() throws RemoteException {
            return 0;
        }

        @Override
        public int getUserCount() throws RemoteException {
            return 0;
        }

        @Override
        public void hideNavigationBar(boolean z) throws RemoteException {
        }

        @Override
        public boolean installApp(String str, int i) throws RemoteException {
            return false;
        }

        @Override
        public boolean isDeviceEncryptionEnabled() throws RemoteException {
            return false;
        }

        @Override
        public void rebootPad(int i, String str) throws RemoteException {
        }

        @Override
        public void removeTask(String str) throws RemoteException {
        }

        @Override
        public void sdUnmount() throws RemoteException {
        }

        @Override
        public void setDefaultParam() throws RemoteException {
        }

        @Override
        public void setDefaultPreferredHomeApp(String str) throws RemoteException {
        }

        @Override
        public void setPermissionEnforced(boolean z) throws RemoteException {
        }

        @Override
        public void setSetupStatus(int i) throws RemoteException {
        }

        @Override
        public void setSystemTime(String str, String str2) throws RemoteException {
        }

        @Override
        public boolean uninstallApp(String str, int i) throws RemoteException {
            return false;
        }

        @Override
        public boolean verifyUpdateImage(String str) throws RemoteException {
            return false;
        }
    }

    void cancelSetup() throws RemoteException;

    boolean checkPadRooted() throws RemoteException;

    void clearDefaultPreferredApp(String str) throws RemoteException;

    boolean copyFile(String str, String str2) throws RemoteException;

    boolean copyUpdateImage(String str, String str2) throws RemoteException;

    boolean deleteFile(String str) throws RemoteException;

    void disableADB() throws RemoteException;

    String getCanonicalExternalPath(String str) throws RemoteException;

    String getForegroundPackageName() throws RemoteException;

    int getSetupStatus() throws RemoteException;

    int getUserCount() throws RemoteException;

    void hideNavigationBar(boolean z) throws RemoteException;

    boolean installApp(String str, int i) throws RemoteException;

    boolean isDeviceEncryptionEnabled() throws RemoteException;

    void rebootPad(int i, String str) throws RemoteException;

    void removeTask(String str) throws RemoteException;

    void sdUnmount() throws RemoteException;

    void setDefaultParam() throws RemoteException;

    void setDefaultPreferredHomeApp(String str) throws RemoteException;

    void setPermissionEnforced(boolean z) throws RemoteException;

    void setSetupStatus(int i) throws RemoteException;

    void setSystemTime(String str, String str2) throws RemoteException;

    boolean uninstallApp(String str, int i) throws RemoteException;

    boolean verifyUpdateImage(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IDchaService {
        private static final String DESCRIPTOR = "jp.co.benesse.dcha.dchaservice.IDchaService";
        static final int TRANSACTION_cancelSetup = 10;
        static final int TRANSACTION_checkPadRooted = 7;
        static final int TRANSACTION_clearDefaultPreferredApp = 5;
        static final int TRANSACTION_copyFile = 18;
        static final int TRANSACTION_copyUpdateImage = 2;
        static final int TRANSACTION_deleteFile = 19;
        static final int TRANSACTION_disableADB = 6;
        static final int TRANSACTION_getCanonicalExternalPath = 24;
        static final int TRANSACTION_getForegroundPackageName = 17;
        static final int TRANSACTION_getSetupStatus = 12;
        static final int TRANSACTION_getUserCount = 20;
        static final int TRANSACTION_hideNavigationBar = 22;
        static final int TRANSACTION_installApp = 8;
        static final int TRANSACTION_isDeviceEncryptionEnabled = 21;
        static final int TRANSACTION_rebootPad = 3;
        static final int TRANSACTION_removeTask = 14;
        static final int TRANSACTION_sdUnmount = 15;
        static final int TRANSACTION_setDefaultParam = 16;
        static final int TRANSACTION_setDefaultPreferredHomeApp = 4;
        static final int TRANSACTION_setPermissionEnforced = 23;
        static final int TRANSACTION_setSetupStatus = 11;
        static final int TRANSACTION_setSystemTime = 13;
        static final int TRANSACTION_uninstallApp = 9;
        static final int TRANSACTION_verifyUpdateImage = 1;

        @Override
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDchaService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IDchaService)) {
                return (IDchaService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
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
                    boolean zVerifyUpdateImage = verifyUpdateImage(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zVerifyUpdateImage ? 1 : 0);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCopyUpdateImage = copyUpdateImage(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCopyUpdateImage ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    rebootPad(parcel.readInt(), parcel.readString());
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultPreferredHomeApp(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_clearDefaultPreferredApp:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearDefaultPreferredApp(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_disableADB:
                    parcel.enforceInterface(DESCRIPTOR);
                    disableADB();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCheckPadRooted = checkPadRooted();
                    parcel2.writeNoException();
                    parcel2.writeInt(zCheckPadRooted ? 1 : 0);
                    return true;
                case TRANSACTION_installApp:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zInstallApp = installApp(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zInstallApp ? 1 : 0);
                    return true;
                case TRANSACTION_uninstallApp:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUninstallApp = uninstallApp(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zUninstallApp ? 1 : 0);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelSetup();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_setSetupStatus:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSetupStatus(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getSetupStatus:
                    parcel.enforceInterface(DESCRIPTOR);
                    int setupStatus = getSetupStatus();
                    parcel2.writeNoException();
                    parcel2.writeInt(setupStatus);
                    return true;
                case TRANSACTION_setSystemTime:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSystemTime(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_removeTask:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeTask(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_sdUnmount:
                    parcel.enforceInterface(DESCRIPTOR);
                    sdUnmount();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_setDefaultParam:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultParam();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getForegroundPackageName:
                    parcel.enforceInterface(DESCRIPTOR);
                    String foregroundPackageName = getForegroundPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(foregroundPackageName);
                    return true;
                case TRANSACTION_copyFile:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCopyFile = copyFile(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCopyFile ? 1 : 0);
                    return true;
                case TRANSACTION_deleteFile:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zDeleteFile = deleteFile(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeleteFile ? 1 : 0);
                    return true;
                case TRANSACTION_getUserCount:
                    parcel.enforceInterface(DESCRIPTOR);
                    int userCount = getUserCount();
                    parcel2.writeNoException();
                    parcel2.writeInt(userCount);
                    return true;
                case TRANSACTION_isDeviceEncryptionEnabled:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsDeviceEncryptionEnabled = isDeviceEncryptionEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsDeviceEncryptionEnabled ? 1 : 0);
                    return true;
                case TRANSACTION_hideNavigationBar:
                    parcel.enforceInterface(DESCRIPTOR);
                    hideNavigationBar(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_setPermissionEnforced:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPermissionEnforced(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getCanonicalExternalPath:
                    parcel.enforceInterface(DESCRIPTOR);
                    String canonicalExternalPath = getCanonicalExternalPath(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(canonicalExternalPath);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IDchaService {
            public static IDchaService sDefaultImpl;
            private IBinder mRemote;

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public boolean verifyUpdateImage(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(1, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().verifyUpdateImage(str);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean copyUpdateImage(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (!this.mRemote.transact(2, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().copyUpdateImage(str, str2);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void rebootPad(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    if (this.mRemote.transact(3, parcelObtain, null, 1) || Stub.getDefaultImpl() == null) {
                        return;
                    }
                    Stub.getDefaultImpl().rebootPad(i, str);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultPreferredHomeApp(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(4, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setDefaultPreferredHomeApp(str);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearDefaultPreferredApp(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(Stub.TRANSACTION_clearDefaultPreferredApp, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().clearDefaultPreferredApp(str);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disableADB() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(Stub.TRANSACTION_disableADB, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().disableADB();
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean checkPadRooted() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(7, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().checkPadRooted();
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean installApp(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (!this.mRemote.transact(Stub.TRANSACTION_installApp, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().installApp(str, i);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean uninstallApp(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (!this.mRemote.transact(Stub.TRANSACTION_uninstallApp, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().uninstallApp(str, i);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelSetup() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(10, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().cancelSetup();
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSetupStatus(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (!this.mRemote.transact(Stub.TRANSACTION_setSetupStatus, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setSetupStatus(i);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getSetupStatus() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(Stub.TRANSACTION_getSetupStatus, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getSetupStatus();
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSystemTime(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (!this.mRemote.transact(Stub.TRANSACTION_setSystemTime, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setSystemTime(str, str2);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeTask(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(Stub.TRANSACTION_removeTask, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().removeTask(str);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sdUnmount() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(Stub.TRANSACTION_sdUnmount, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().sdUnmount();
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultParam() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(Stub.TRANSACTION_setDefaultParam, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setDefaultParam();
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getForegroundPackageName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(Stub.TRANSACTION_getForegroundPackageName, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getForegroundPackageName();
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean copyFile(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (!this.mRemote.transact(Stub.TRANSACTION_copyFile, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().copyFile(str, str2);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean deleteFile(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(Stub.TRANSACTION_deleteFile, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().deleteFile(str);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getUserCount() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(Stub.TRANSACTION_getUserCount, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getUserCount();
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isDeviceEncryptionEnabled() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(Stub.TRANSACTION_isDeviceEncryptionEnabled, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isDeviceEncryptionEnabled();
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void hideNavigationBar(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (!this.mRemote.transact(Stub.TRANSACTION_hideNavigationBar, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().hideNavigationBar(z);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPermissionEnforced(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (!this.mRemote.transact(Stub.TRANSACTION_setPermissionEnforced, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setPermissionEnforced(z);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCanonicalExternalPath(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(Stub.TRANSACTION_getCanonicalExternalPath, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getCanonicalExternalPath(str);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IDchaService iDchaService) {
            if (Proxy.sDefaultImpl != null || iDchaService == null) {
                return false;
            }
            Proxy.sDefaultImpl = iDchaService;
            return true;
        }

        public static IDchaService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
