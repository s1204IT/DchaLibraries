package com.mediatek.common.multiwindow;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.common.multiwindow.IMWAmsCallback;
import com.mediatek.common.multiwindow.IMWSystemUiCallback;
import com.mediatek.common.multiwindow.IMWWmsCallback;
import java.util.List;

public interface IMultiWindowManager extends IInterface {
    void activityCreated(IBinder iBinder) throws RemoteException;

    void addConfigNotChangePkg(String str) throws RemoteException;

    void addDisableFloatPkg(String str) throws RemoteException;

    void addMiniMaxRestartPkg(String str) throws RemoteException;

    int appErrorHandling(String str, boolean z, boolean z2) throws RemoteException;

    void closeWindow(IBinder iBinder) throws RemoteException;

    void enableFocusedFrame(boolean z) throws RemoteException;

    List<String> getDisableFloatComponentList() throws RemoteException;

    List<String> getDisableFloatPkgList() throws RemoteException;

    boolean isFloatingStack(int i) throws RemoteException;

    boolean isInMiniMax(int i) throws RemoteException;

    boolean isStickStack(int i) throws RemoteException;

    boolean isSticky(IBinder iBinder) throws RemoteException;

    boolean matchConfigChangeList(String str) throws RemoteException;

    boolean matchConfigNotChangeList(String str) throws RemoteException;

    boolean matchDisableFloatActivityList(String str) throws RemoteException;

    boolean matchDisableFloatPkgList(String str) throws RemoteException;

    boolean matchDisableFloatWinList(String str) throws RemoteException;

    boolean matchMinimaxRestartList(String str) throws RemoteException;

    void miniMaxTask(int i) throws RemoteException;

    void moveActivityTaskToFront(IBinder iBinder) throws RemoteException;

    void moveFloatingWindow(int i, int i2) throws RemoteException;

    void resizeFloatingWindow(int i, int i2, int i3) throws RemoteException;

    void restoreWindow(IBinder iBinder, boolean z) throws RemoteException;

    void setAMSCallback(IMWAmsCallback iMWAmsCallback) throws RemoteException;

    void setFloatingStack(int i) throws RemoteException;

    void setSystemUiCallback(IMWSystemUiCallback iMWSystemUiCallback) throws RemoteException;

    void setWMSCallback(IMWWmsCallback iMWWmsCallback) throws RemoteException;

    void showRestoreButton(boolean z) throws RemoteException;

    void stickWindow(IBinder iBinder, boolean z) throws RemoteException;

    void taskAdded(int i) throws RemoteException;

    void taskRemoved(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IMultiWindowManager {
        private static final String DESCRIPTOR = "com.mediatek.common.multiwindow.IMultiWindowManager";
        static final int TRANSACTION_activityCreated = 30;
        static final int TRANSACTION_addConfigNotChangePkg = 26;
        static final int TRANSACTION_addDisableFloatPkg = 25;
        static final int TRANSACTION_addMiniMaxRestartPkg = 27;
        static final int TRANSACTION_appErrorHandling = 28;
        static final int TRANSACTION_closeWindow = 2;
        static final int TRANSACTION_enableFocusedFrame = 14;
        static final int TRANSACTION_getDisableFloatComponentList = 22;
        static final int TRANSACTION_getDisableFloatPkgList = 21;
        static final int TRANSACTION_isFloatingStack = 7;
        static final int TRANSACTION_isInMiniMax = 11;
        static final int TRANSACTION_isStickStack = 10;
        static final int TRANSACTION_isSticky = 29;
        static final int TRANSACTION_matchConfigChangeList = 24;
        static final int TRANSACTION_matchConfigNotChangeList = 17;
        static final int TRANSACTION_matchDisableFloatActivityList = 19;
        static final int TRANSACTION_matchDisableFloatPkgList = 18;
        static final int TRANSACTION_matchDisableFloatWinList = 20;
        static final int TRANSACTION_matchMinimaxRestartList = 23;
        static final int TRANSACTION_miniMaxTask = 15;
        static final int TRANSACTION_moveActivityTaskToFront = 1;
        static final int TRANSACTION_moveFloatingWindow = 12;
        static final int TRANSACTION_resizeFloatingWindow = 13;
        static final int TRANSACTION_restoreWindow = 3;
        static final int TRANSACTION_setAMSCallback = 4;
        static final int TRANSACTION_setFloatingStack = 8;
        static final int TRANSACTION_setSystemUiCallback = 5;
        static final int TRANSACTION_setWMSCallback = 9;
        static final int TRANSACTION_showRestoreButton = 16;
        static final int TRANSACTION_stickWindow = 6;
        static final int TRANSACTION_taskAdded = 31;
        static final int TRANSACTION_taskRemoved = 32;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMultiWindowManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMultiWindowManager)) {
                return (IMultiWindowManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
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
                    moveActivityTaskToFront(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    closeWindow(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    restoreWindow(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAMSCallback(IMWAmsCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSystemUiCallback(IMWSystemUiCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    stickWindow(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsFloatingStack = isFloatingStack(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsFloatingStack ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    setFloatingStack(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    setWMSCallback(IMWWmsCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsStickStack = isStickStack(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsStickStack ? 1 : 0);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInMiniMax = isInMiniMax(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInMiniMax ? 1 : 0);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    moveFloatingWindow(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    resizeFloatingWindow(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_enableFocusedFrame:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableFocusedFrame(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_miniMaxTask:
                    parcel.enforceInterface(DESCRIPTOR);
                    miniMaxTask(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_showRestoreButton:
                    parcel.enforceInterface(DESCRIPTOR);
                    showRestoreButton(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_matchConfigNotChangeList:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMatchConfigNotChangeList = matchConfigNotChangeList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zMatchConfigNotChangeList ? 1 : 0);
                    return true;
                case TRANSACTION_matchDisableFloatPkgList:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMatchDisableFloatPkgList = matchDisableFloatPkgList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zMatchDisableFloatPkgList ? 1 : 0);
                    return true;
                case TRANSACTION_matchDisableFloatActivityList:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMatchDisableFloatActivityList = matchDisableFloatActivityList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zMatchDisableFloatActivityList ? 1 : 0);
                    return true;
                case TRANSACTION_matchDisableFloatWinList:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMatchDisableFloatWinList = matchDisableFloatWinList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zMatchDisableFloatWinList ? 1 : 0);
                    return true;
                case TRANSACTION_getDisableFloatPkgList:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<String> disableFloatPkgList = getDisableFloatPkgList();
                    parcel2.writeNoException();
                    parcel2.writeStringList(disableFloatPkgList);
                    return true;
                case TRANSACTION_getDisableFloatComponentList:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<String> disableFloatComponentList = getDisableFloatComponentList();
                    parcel2.writeNoException();
                    parcel2.writeStringList(disableFloatComponentList);
                    return true;
                case TRANSACTION_matchMinimaxRestartList:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMatchMinimaxRestartList = matchMinimaxRestartList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zMatchMinimaxRestartList ? 1 : 0);
                    return true;
                case TRANSACTION_matchConfigChangeList:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMatchConfigChangeList = matchConfigChangeList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zMatchConfigChangeList ? 1 : 0);
                    return true;
                case TRANSACTION_addDisableFloatPkg:
                    parcel.enforceInterface(DESCRIPTOR);
                    addDisableFloatPkg(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_addConfigNotChangePkg:
                    parcel.enforceInterface(DESCRIPTOR);
                    addConfigNotChangePkg(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_addMiniMaxRestartPkg:
                    parcel.enforceInterface(DESCRIPTOR);
                    addMiniMaxRestartPkg(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_appErrorHandling:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAppErrorHandling = appErrorHandling(parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAppErrorHandling);
                    return true;
                case TRANSACTION_isSticky:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsSticky = isSticky(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSticky ? 1 : 0);
                    return true;
                case TRANSACTION_activityCreated:
                    parcel.enforceInterface(DESCRIPTOR);
                    activityCreated(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_taskAdded:
                    parcel.enforceInterface(DESCRIPTOR);
                    taskAdded(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_taskRemoved:
                    parcel.enforceInterface(DESCRIPTOR);
                    taskRemoved(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMultiWindowManager {
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
            public void moveActivityTaskToFront(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void closeWindow(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restoreWindow(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAMSCallback(IMWAmsCallback iMWAmsCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMWAmsCallback != null ? iMWAmsCallback.asBinder() : null);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSystemUiCallback(IMWSystemUiCallback iMWSystemUiCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMWSystemUiCallback != null ? iMWSystemUiCallback.asBinder() : null);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stickWindow(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isFloatingStack(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setFloatingStack(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setWMSCallback(IMWWmsCallback iMWWmsCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMWWmsCallback != null ? iMWWmsCallback.asBinder() : null);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isStickStack(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInMiniMax(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveFloatingWindow(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resizeFloatingWindow(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableFocusedFrame(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_enableFocusedFrame, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void miniMaxTask(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_miniMaxTask, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showRestoreButton(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_showRestoreButton, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean matchConfigNotChangeList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_matchConfigNotChangeList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean matchDisableFloatPkgList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_matchDisableFloatPkgList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean matchDisableFloatActivityList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_matchDisableFloatActivityList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean matchDisableFloatWinList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_matchDisableFloatWinList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<String> getDisableFloatPkgList() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getDisableFloatPkgList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArrayList();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<String> getDisableFloatComponentList() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getDisableFloatComponentList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArrayList();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean matchMinimaxRestartList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_matchMinimaxRestartList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean matchConfigChangeList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_matchConfigChangeList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addDisableFloatPkg(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_addDisableFloatPkg, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addConfigNotChangePkg(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_addConfigNotChangePkg, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addMiniMaxRestartPkg(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_addMiniMaxRestartPkg, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int appErrorHandling(String str, boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_appErrorHandling, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isSticky(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(Stub.TRANSACTION_isSticky, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activityCreated(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(Stub.TRANSACTION_activityCreated, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void taskAdded(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_taskAdded, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void taskRemoved(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_taskRemoved, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
