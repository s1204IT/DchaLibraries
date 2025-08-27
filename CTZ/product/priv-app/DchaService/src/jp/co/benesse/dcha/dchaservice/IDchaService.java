package jp.co.benesse.dcha.dchaservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IDchaService extends IInterface {
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
        public Stub() {
            attachInterface(this, "jp.co.benesse.dcha.dchaservice.IDchaService");
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("jp.co.benesse.dcha.dchaservice.IDchaService");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zVerifyUpdateImage = verifyUpdateImage(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zVerifyUpdateImage ? 1 : 0);
                    return true;
                case 2:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zCopyUpdateImage = copyUpdateImage(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCopyUpdateImage ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    rebootPad(parcel.readInt(), parcel.readString());
                    return true;
                case 4:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    setDefaultPreferredHomeApp(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    clearDefaultPreferredApp(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    disableADB();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zCheckPadRooted = checkPadRooted();
                    parcel2.writeNoException();
                    parcel2.writeInt(zCheckPadRooted ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zInstallApp = installApp(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zInstallApp ? 1 : 0);
                    return true;
                case 9:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zUninstallApp = uninstallApp(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zUninstallApp ? 1 : 0);
                    return true;
                case 10:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    cancelSetup();
                    parcel2.writeNoException();
                    return true;
                case 11:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    setSetupStatus(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    int setupStatus = getSetupStatus();
                    parcel2.writeNoException();
                    parcel2.writeInt(setupStatus);
                    return true;
                case 13:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    setSystemTime(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    removeTask(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    sdUnmount();
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    setDefaultParam();
                    parcel2.writeNoException();
                    return true;
                case 17:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String foregroundPackageName = getForegroundPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(foregroundPackageName);
                    return true;
                case 18:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zCopyFile = copyFile(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCopyFile ? 1 : 0);
                    return true;
                case 19:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zDeleteFile = deleteFile(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeleteFile ? 1 : 0);
                    return true;
                case 20:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    int userCount = getUserCount();
                    parcel2.writeNoException();
                    parcel2.writeInt(userCount);
                    return true;
                case 21:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean zIsDeviceEncryptionEnabled = isDeviceEncryptionEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsDeviceEncryptionEnabled ? 1 : 0);
                    return true;
                case 22:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    hideNavigationBar(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    setPermissionEnforced(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String canonicalExternalPath = getCanonicalExternalPath(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(canonicalExternalPath);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }
}
