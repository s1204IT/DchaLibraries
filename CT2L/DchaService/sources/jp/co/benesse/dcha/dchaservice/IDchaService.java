package jp.co.benesse.dcha.dchaservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IDchaService extends IInterface {
    void cancelSetup() throws RemoteException;

    boolean checkPadRooted() throws RemoteException;

    void clearDefaultPreferredApp(String str) throws RemoteException;

    boolean copyFile(String str, String str2) throws RemoteException;

    boolean copyUpdateImage(String str, String str2) throws RemoteException;

    boolean deleteFile(String str) throws RemoteException;

    void disableADB() throws RemoteException;

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

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg0 = data.readString();
                    boolean _result = verifyUpdateImage(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 2:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg02 = data.readString();
                    String _arg1 = data.readString();
                    boolean _result2 = copyUpdateImage(_arg02, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 3:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    int _arg03 = data.readInt();
                    String _arg12 = data.readString();
                    rebootPad(_arg03, _arg12);
                    return true;
                case 4:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg04 = data.readString();
                    setDefaultPreferredHomeApp(_arg04);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg05 = data.readString();
                    clearDefaultPreferredApp(_arg05);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    disableADB();
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean _result3 = checkPadRooted();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 8:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg06 = data.readString();
                    int _arg13 = data.readInt();
                    boolean _result4 = installApp(_arg06, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 9:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg07 = data.readString();
                    int _arg14 = data.readInt();
                    boolean _result5 = uninstallApp(_arg07, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result5 ? 1 : 0);
                    return true;
                case 10:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    cancelSetup();
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    int _arg08 = data.readInt();
                    setSetupStatus(_arg08);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    int _result6 = getSetupStatus();
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 13:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg09 = data.readString();
                    String _arg15 = data.readString();
                    setSystemTime(_arg09, _arg15);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg010 = data.readString();
                    removeTask(_arg010);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    sdUnmount();
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    setDefaultParam();
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _result7 = getForegroundPackageName();
                    reply.writeNoException();
                    reply.writeString(_result7);
                    return true;
                case 18:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg011 = data.readString();
                    String _arg16 = data.readString();
                    boolean _result8 = copyFile(_arg011, _arg16);
                    reply.writeNoException();
                    reply.writeInt(_result8 ? 1 : 0);
                    return true;
                case 19:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    String _arg012 = data.readString();
                    boolean _result9 = deleteFile(_arg012);
                    reply.writeNoException();
                    reply.writeInt(_result9 ? 1 : 0);
                    return true;
                case 20:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    int _result10 = getUserCount();
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    return true;
                case 21:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean _result11 = isDeviceEncryptionEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result11 ? 1 : 0);
                    return true;
                case 22:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean _arg013 = data.readInt() != 0;
                    hideNavigationBar(_arg013);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
                    boolean _arg014 = data.readInt() != 0;
                    setPermissionEnforced(_arg014);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString("jp.co.benesse.dcha.dchaservice.IDchaService");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }
}
