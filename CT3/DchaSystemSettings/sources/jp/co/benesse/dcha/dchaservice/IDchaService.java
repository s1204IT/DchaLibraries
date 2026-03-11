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

        public static IDchaService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("jp.co.benesse.dcha.dchaservice.IDchaService");
            if (iin != null && (iin instanceof IDchaService)) {
                return (IDchaService) iin;
            }
            return new Proxy(obj);
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

        private static class Proxy implements IDchaService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public boolean verifyUpdateImage(String updateFile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(updateFile);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean copyUpdateImage(String srcFile, String dstFile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(srcFile);
                    _data.writeString(dstFile);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void rebootPad(int rebootMode, String srcFile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeInt(rebootMode);
                    _data.writeString(srcFile);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setDefaultPreferredHomeApp(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(packageName);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearDefaultPreferredApp(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(packageName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void disableADB() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean checkPadRooted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean installApp(String path, int installFlag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(path);
                    _data.writeInt(installFlag);
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
            public boolean uninstallApp(String packageName, int uninstallFlag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(packageName);
                    _data.writeInt(uninstallFlag);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void cancelSetup() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setSetupStatus(int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeInt(status);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getSetupStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setSystemTime(String time, String timeFormat) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(time);
                    _data.writeString(timeFormat);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeTask(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(packageName);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sdUnmount() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setDefaultParam() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getForegroundPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean copyFile(String srcFilePath, String dstFilePath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(srcFilePath);
                    _data.writeString(dstFilePath);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean deleteFile(String path) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeString(path);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getUserCount() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isDeviceEncryptionEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void hideNavigationBar(boolean hide) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeInt(hide ? 1 : 0);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPermissionEnforced(boolean enforced) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("jp.co.benesse.dcha.dchaservice.IDchaService");
                    _data.writeInt(enforced ? 1 : 0);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
