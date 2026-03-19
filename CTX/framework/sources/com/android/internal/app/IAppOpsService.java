package com.android.internal.app;

import android.app.AppOpsManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.app.IAppOpsActiveCallback;
import com.android.internal.app.IAppOpsCallback;
import java.util.List;

public interface IAppOpsService extends IInterface {
    int checkAudioOperation(int i, int i2, int i3, String str) throws RemoteException;

    int checkOperation(int i, int i2, String str) throws RemoteException;

    int checkPackage(int i, String str) throws RemoteException;

    void finishOperation(IBinder iBinder, int i, int i2, String str) throws RemoteException;

    List<AppOpsManager.PackageOps> getOpsForPackage(int i, String str, int[] iArr) throws RemoteException;

    List<AppOpsManager.PackageOps> getPackagesForOps(int[] iArr) throws RemoteException;

    IBinder getToken(IBinder iBinder) throws RemoteException;

    List<AppOpsManager.PackageOps> getUidOps(int i, int[] iArr) throws RemoteException;

    boolean isOperationActive(int i, int i2, String str) throws RemoteException;

    int noteOperation(int i, int i2, String str) throws RemoteException;

    int noteProxyOperation(int i, String str, int i2, String str2) throws RemoteException;

    int permissionToOpCode(String str) throws RemoteException;

    void removeUser(int i) throws RemoteException;

    void resetAllModes(int i, String str) throws RemoteException;

    void setAudioRestriction(int i, int i2, int i3, int i4, String[] strArr) throws RemoteException;

    void setMode(int i, int i2, String str, int i3) throws RemoteException;

    void setUidMode(int i, int i2, int i3) throws RemoteException;

    void setUserRestriction(int i, boolean z, IBinder iBinder, int i2, String[] strArr) throws RemoteException;

    void setUserRestrictions(Bundle bundle, IBinder iBinder, int i) throws RemoteException;

    int startOperation(IBinder iBinder, int i, int i2, String str, boolean z) throws RemoteException;

    void startWatchingActive(int[] iArr, IAppOpsActiveCallback iAppOpsActiveCallback) throws RemoteException;

    void startWatchingMode(int i, String str, IAppOpsCallback iAppOpsCallback) throws RemoteException;

    void startWatchingModeWithFlags(int i, String str, int i2, IAppOpsCallback iAppOpsCallback) throws RemoteException;

    void stopWatchingActive(IAppOpsActiveCallback iAppOpsActiveCallback) throws RemoteException;

    void stopWatchingMode(IAppOpsCallback iAppOpsCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IAppOpsService {
        private static final String DESCRIPTOR = "com.android.internal.app.IAppOpsService";
        static final int TRANSACTION_checkAudioOperation = 17;
        static final int TRANSACTION_checkOperation = 1;
        static final int TRANSACTION_checkPackage = 10;
        static final int TRANSACTION_finishOperation = 4;
        static final int TRANSACTION_getOpsForPackage = 12;
        static final int TRANSACTION_getPackagesForOps = 11;
        static final int TRANSACTION_getToken = 7;
        static final int TRANSACTION_getUidOps = 13;
        static final int TRANSACTION_isOperationActive = 24;
        static final int TRANSACTION_noteOperation = 2;
        static final int TRANSACTION_noteProxyOperation = 9;
        static final int TRANSACTION_permissionToOpCode = 8;
        static final int TRANSACTION_removeUser = 21;
        static final int TRANSACTION_resetAllModes = 16;
        static final int TRANSACTION_setAudioRestriction = 18;
        static final int TRANSACTION_setMode = 15;
        static final int TRANSACTION_setUidMode = 14;
        static final int TRANSACTION_setUserRestriction = 20;
        static final int TRANSACTION_setUserRestrictions = 19;
        static final int TRANSACTION_startOperation = 3;
        static final int TRANSACTION_startWatchingActive = 22;
        static final int TRANSACTION_startWatchingMode = 5;
        static final int TRANSACTION_startWatchingModeWithFlags = 25;
        static final int TRANSACTION_stopWatchingActive = 23;
        static final int TRANSACTION_stopWatchingMode = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAppOpsService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IAppOpsService)) {
                return (IAppOpsService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckOperation = checkOperation(parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckOperation);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iNoteOperation = noteOperation(parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iNoteOperation);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStartOperation = startOperation(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(iStartOperation);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    finishOperation(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    startWatchingMode(parcel.readInt(), parcel.readString(), IAppOpsCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopWatchingMode(IAppOpsCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder token = getToken(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(token);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iPermissionToOpCode = permissionToOpCode(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iPermissionToOpCode);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iNoteProxyOperation = noteProxyOperation(parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iNoteProxyOperation);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckPackage = checkPackage(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckPackage);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<AppOpsManager.PackageOps> packagesForOps = getPackagesForOps(parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(packagesForOps);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<AppOpsManager.PackageOps> opsForPackage = getOpsForPackage(parcel.readInt(), parcel.readString(), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(opsForPackage);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<AppOpsManager.PackageOps> uidOps = getUidOps(parcel.readInt(), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(uidOps);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    setUidMode(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    setMode(parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    resetAllModes(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckAudioOperation = checkAudioOperation(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckAudioOperation);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAudioRestriction(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createStringArray());
                    parcel2.writeNoException();
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    setUserRestrictions(bundleCreateFromParcel, parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    setUserRestriction(parcel.readInt(), parcel.readInt() != 0, parcel.readStrongBinder(), parcel.readInt(), parcel.createStringArray());
                    parcel2.writeNoException();
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeUser(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    startWatchingActive(parcel.createIntArray(), IAppOpsActiveCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopWatchingActive(IAppOpsActiveCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsOperationActive = isOperationActive(parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsOperationActive ? 1 : 0);
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    startWatchingModeWithFlags(parcel.readInt(), parcel.readString(), parcel.readInt(), IAppOpsCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IAppOpsService {
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
            public int checkOperation(int i, int i2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int noteOperation(int i, int i2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startOperation(IBinder iBinder, int i, int i2, String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void finishOperation(IBinder iBinder, int i, int i2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startWatchingMode(int i, String str, IAppOpsCallback iAppOpsCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iAppOpsCallback != null ? iAppOpsCallback.asBinder() : null);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopWatchingMode(IAppOpsCallback iAppOpsCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iAppOpsCallback != null ? iAppOpsCallback.asBinder() : null);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IBinder getToken(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int permissionToOpCode(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int noteProxyOperation(int i, String str, int i2, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkPackage(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<AppOpsManager.PackageOps> getPackagesForOps(int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(AppOpsManager.PackageOps.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<AppOpsManager.PackageOps> getOpsForPackage(int i, String str, int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(AppOpsManager.PackageOps.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<AppOpsManager.PackageOps> getUidOps(int i, int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(AppOpsManager.PackageOps.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setUidMode(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setMode(int i, int i2, String str, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resetAllModes(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkAudioOperation(int i, int i2, int i3, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAudioRestriction(int i, int i2, int i3, int i4, String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setUserRestrictions(Bundle bundle, IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setUserRestriction(int i, boolean z, IBinder iBinder, int i2, String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeUser(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startWatchingActive(int[] iArr, IAppOpsActiveCallback iAppOpsActiveCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeStrongBinder(iAppOpsActiveCallback != null ? iAppOpsActiveCallback.asBinder() : null);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopWatchingActive(IAppOpsActiveCallback iAppOpsActiveCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iAppOpsActiveCallback != null ? iAppOpsActiveCallback.asBinder() : null);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isOperationActive(int i, int i2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startWatchingModeWithFlags(int i, String str, int i2, IAppOpsCallback iAppOpsCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iAppOpsCallback != null ? iAppOpsCallback.asBinder() : null);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
