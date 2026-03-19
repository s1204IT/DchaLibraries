package com.mediatek.common.operamax;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.common.operamax.ILoaderStateListener;

public interface ILoaderService extends IInterface {
    void addDirectedApp(String str) throws RemoteException;

    void addDirectedHeaderField(String str, String str2) throws RemoteException;

    void addDirectedHost(String str) throws RemoteException;

    int getCompressLevel() throws RemoteException;

    String[] getDirectedAppList() throws RemoteException;

    String[] getDirectedHeaderFieldList() throws RemoteException;

    String[] getDirectedHostList() throws RemoteException;

    int getSavingState() throws RemoteException;

    int getTunnelState() throws RemoteException;

    boolean isAppDirected(String str) throws RemoteException;

    boolean isHeaderFieldDirected(String str, String str2) throws RemoteException;

    boolean isHostDirected(String str) throws RemoteException;

    void launchOperaMAX() throws RemoteException;

    void registerStateListener(ILoaderStateListener iLoaderStateListener) throws RemoteException;

    void removeAllDirectedApps() throws RemoteException;

    void removeAllDirectedHeaderFields() throws RemoteException;

    void removeAllDirectedHosts() throws RemoteException;

    void removeDirectedApp(String str) throws RemoteException;

    void removeDirectedHeaderField(String str, String str2) throws RemoteException;

    void removeDirectedHost(String str) throws RemoteException;

    void setCompressLevel(int i) throws RemoteException;

    void startSaving() throws RemoteException;

    void stopSaving() throws RemoteException;

    void unregisterStateListener(ILoaderStateListener iLoaderStateListener) throws RemoteException;

    public static abstract class Stub extends Binder implements ILoaderService {
        private static final String DESCRIPTOR = "com.mediatek.common.operamax.ILoaderService";
        static final int TRANSACTION_addDirectedApp = 8;
        static final int TRANSACTION_addDirectedHeaderField = 18;
        static final int TRANSACTION_addDirectedHost = 13;
        static final int TRANSACTION_getCompressLevel = 24;
        static final int TRANSACTION_getDirectedAppList = 12;
        static final int TRANSACTION_getDirectedHeaderFieldList = 22;
        static final int TRANSACTION_getDirectedHostList = 17;
        static final int TRANSACTION_getSavingState = 4;
        static final int TRANSACTION_getTunnelState = 3;
        static final int TRANSACTION_isAppDirected = 11;
        static final int TRANSACTION_isHeaderFieldDirected = 21;
        static final int TRANSACTION_isHostDirected = 16;
        static final int TRANSACTION_launchOperaMAX = 7;
        static final int TRANSACTION_registerStateListener = 5;
        static final int TRANSACTION_removeAllDirectedApps = 10;
        static final int TRANSACTION_removeAllDirectedHeaderFields = 20;
        static final int TRANSACTION_removeAllDirectedHosts = 15;
        static final int TRANSACTION_removeDirectedApp = 9;
        static final int TRANSACTION_removeDirectedHeaderField = 19;
        static final int TRANSACTION_removeDirectedHost = 14;
        static final int TRANSACTION_setCompressLevel = 23;
        static final int TRANSACTION_startSaving = 1;
        static final int TRANSACTION_stopSaving = 2;
        static final int TRANSACTION_unregisterStateListener = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ILoaderService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ILoaderService)) {
                return (ILoaderService) iInterfaceQueryLocalInterface;
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
                    startSaving();
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopSaving();
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int tunnelState = getTunnelState();
                    parcel2.writeNoException();
                    parcel2.writeInt(tunnelState);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int savingState = getSavingState();
                    parcel2.writeNoException();
                    parcel2.writeInt(savingState);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerStateListener(ILoaderStateListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterStateListener(ILoaderStateListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    launchOperaMAX();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    addDirectedApp(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeDirectedApp(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeAllDirectedApps();
                    parcel2.writeNoException();
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsAppDirected = isAppDirected(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsAppDirected ? 1 : 0);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] directedAppList = getDirectedAppList();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(directedAppList);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    addDirectedHost(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_removeDirectedHost:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeDirectedHost(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_removeAllDirectedHosts:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeAllDirectedHosts();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_isHostDirected:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsHostDirected = isHostDirected(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsHostDirected ? 1 : 0);
                    return true;
                case TRANSACTION_getDirectedHostList:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] directedHostList = getDirectedHostList();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(directedHostList);
                    return true;
                case TRANSACTION_addDirectedHeaderField:
                    parcel.enforceInterface(DESCRIPTOR);
                    addDirectedHeaderField(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_removeDirectedHeaderField:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeDirectedHeaderField(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_removeAllDirectedHeaderFields:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeAllDirectedHeaderFields();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_isHeaderFieldDirected:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsHeaderFieldDirected = isHeaderFieldDirected(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsHeaderFieldDirected ? 1 : 0);
                    return true;
                case TRANSACTION_getDirectedHeaderFieldList:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] directedHeaderFieldList = getDirectedHeaderFieldList();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(directedHeaderFieldList);
                    return true;
                case TRANSACTION_setCompressLevel:
                    parcel.enforceInterface(DESCRIPTOR);
                    setCompressLevel(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getCompressLevel:
                    parcel.enforceInterface(DESCRIPTOR);
                    int compressLevel = getCompressLevel();
                    parcel2.writeNoException();
                    parcel2.writeInt(compressLevel);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ILoaderService {
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
            public void startSaving() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopSaving() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getTunnelState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getSavingState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerStateListener(ILoaderStateListener iLoaderStateListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iLoaderStateListener != null ? iLoaderStateListener.asBinder() : null);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterStateListener(ILoaderStateListener iLoaderStateListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iLoaderStateListener != null ? iLoaderStateListener.asBinder() : null);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void launchOperaMAX() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addDirectedApp(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeDirectedApp(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeAllDirectedApps() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isAppDirected(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getDirectedAppList() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addDirectedHost(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeDirectedHost(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_removeDirectedHost, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeAllDirectedHosts() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_removeAllDirectedHosts, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isHostDirected(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_isHostDirected, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getDirectedHostList() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getDirectedHostList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addDirectedHeaderField(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(Stub.TRANSACTION_addDirectedHeaderField, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeDirectedHeaderField(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(Stub.TRANSACTION_removeDirectedHeaderField, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeAllDirectedHeaderFields() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_removeAllDirectedHeaderFields, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isHeaderFieldDirected(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(Stub.TRANSACTION_isHeaderFieldDirected, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getDirectedHeaderFieldList() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getDirectedHeaderFieldList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setCompressLevel(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_setCompressLevel, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getCompressLevel() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCompressLevel, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
