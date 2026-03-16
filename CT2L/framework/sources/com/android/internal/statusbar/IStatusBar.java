package com.android.internal.statusbar;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IStatusBar extends IInterface {
    void animateCollapsePanels() throws RemoteException;

    void animateExpandNotificationsPanel() throws RemoteException;

    void animateExpandSettingsPanel() throws RemoteException;

    void buzzBeepBlinked() throws RemoteException;

    void cancelPreloadRecentApps() throws RemoteException;

    void disable(int i) throws RemoteException;

    void hideRecentApps(boolean z, boolean z2) throws RemoteException;

    void notificationLightOff() throws RemoteException;

    void notificationLightPulse(int i, int i2, int i3) throws RemoteException;

    void preloadRecentApps() throws RemoteException;

    void removeIcon(int i) throws RemoteException;

    void setIcon(int i, StatusBarIcon statusBarIcon) throws RemoteException;

    void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z) throws RemoteException;

    void setSystemUiVisibility(int i, int i2) throws RemoteException;

    void setWindowState(int i, int i2) throws RemoteException;

    void showRecentApps(boolean z) throws RemoteException;

    void showScreenPinningRequest() throws RemoteException;

    void toggleRecentApps() throws RemoteException;

    void topAppWindowChanged(boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements IStatusBar {
        private static final String DESCRIPTOR = "com.android.internal.statusbar.IStatusBar";
        static final int TRANSACTION_animateCollapsePanels = 6;
        static final int TRANSACTION_animateExpandNotificationsPanel = 4;
        static final int TRANSACTION_animateExpandSettingsPanel = 5;
        static final int TRANSACTION_buzzBeepBlinked = 11;
        static final int TRANSACTION_cancelPreloadRecentApps = 18;
        static final int TRANSACTION_disable = 3;
        static final int TRANSACTION_hideRecentApps = 15;
        static final int TRANSACTION_notificationLightOff = 12;
        static final int TRANSACTION_notificationLightPulse = 13;
        static final int TRANSACTION_preloadRecentApps = 17;
        static final int TRANSACTION_removeIcon = 2;
        static final int TRANSACTION_setIcon = 1;
        static final int TRANSACTION_setImeWindowStatus = 9;
        static final int TRANSACTION_setSystemUiVisibility = 7;
        static final int TRANSACTION_setWindowState = 10;
        static final int TRANSACTION_showRecentApps = 14;
        static final int TRANSACTION_showScreenPinningRequest = 19;
        static final int TRANSACTION_toggleRecentApps = 16;
        static final int TRANSACTION_topAppWindowChanged = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatusBar asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IStatusBar)) {
                return (IStatusBar) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            StatusBarIcon _arg1;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = StatusBarIcon.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    setIcon(_arg0, _arg1);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    removeIcon(_arg02);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    disable(_arg03);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    animateExpandNotificationsPanel();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    animateExpandSettingsPanel();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    animateCollapsePanels();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    int _arg12 = data.readInt();
                    setSystemUiVisibility(_arg04, _arg12);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg05 = data.readInt() != 0;
                    topAppWindowChanged(_arg05);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg06 = data.readStrongBinder();
                    int _arg13 = data.readInt();
                    int _arg2 = data.readInt();
                    boolean _arg3 = data.readInt() != 0;
                    setImeWindowStatus(_arg06, _arg13, _arg2, _arg3);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    int _arg14 = data.readInt();
                    setWindowState(_arg07, _arg14);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    buzzBeepBlinked();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    notificationLightOff();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    int _arg15 = data.readInt();
                    int _arg22 = data.readInt();
                    notificationLightPulse(_arg08, _arg15, _arg22);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg09 = data.readInt() != 0;
                    showRecentApps(_arg09);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg010 = data.readInt() != 0;
                    boolean _arg16 = data.readInt() != 0;
                    hideRecentApps(_arg010, _arg16);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    toggleRecentApps();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    preloadRecentApps();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    cancelPreloadRecentApps();
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    showScreenPinningRequest();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IStatusBar {
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
            public void setIcon(int index, StatusBarIcon icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    if (icon != null) {
                        _data.writeInt(1);
                        icon.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void removeIcon(int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void disable(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void animateExpandNotificationsPanel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void animateExpandSettingsPanel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void animateCollapsePanels() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setSystemUiVisibility(int vis, int mask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vis);
                    _data.writeInt(mask);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void topAppWindowChanged(boolean menuVisible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(menuVisible ? 1 : 0);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(vis);
                    _data.writeInt(backDisposition);
                    _data.writeInt(showImeSwitcher ? 1 : 0);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setWindowState(int window, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(window);
                    _data.writeInt(state);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void buzzBeepBlinked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void notificationLightOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void notificationLightPulse(int argb, int millisOn, int millisOff) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(argb);
                    _data.writeInt(millisOn);
                    _data.writeInt(millisOff);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void showRecentApps(boolean triggeredFromAltTab) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(triggeredFromAltTab ? 1 : 0);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(triggeredFromAltTab ? 1 : 0);
                    _data.writeInt(triggeredFromHomeKey ? 1 : 0);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void toggleRecentApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void preloadRecentApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void cancelPreloadRecentApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void showScreenPinningRequest() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
