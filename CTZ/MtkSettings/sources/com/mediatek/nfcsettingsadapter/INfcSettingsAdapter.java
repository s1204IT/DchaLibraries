package com.mediatek.nfcsettingsadapter;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface INfcSettingsAdapter extends IInterface {
    void commitServiceEntryList(List<ServiceEntry> list) throws RemoteException;

    int getModeFlag(int i) throws RemoteException;

    List<ServiceEntry> getServiceEntryList(int i) throws RemoteException;

    boolean isRoutingTableOverflow() throws RemoteException;

    boolean isShowOverflowMenu() throws RemoteException;

    void setModeFlag(int i, int i2) throws RemoteException;

    boolean testServiceEntryList(List<ServiceEntry> list) throws RemoteException;

    public static abstract class Stub extends Binder implements INfcSettingsAdapter {
        public static INfcSettingsAdapter asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof INfcSettingsAdapter)) {
                return (INfcSettingsAdapter) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    int modeFlag = getModeFlag(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(modeFlag);
                    return true;
                case 2:
                    parcel.enforceInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    setModeFlag(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    boolean zIsRoutingTableOverflow = isRoutingTableOverflow();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsRoutingTableOverflow ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    boolean zIsShowOverflowMenu = isShowOverflowMenu();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsShowOverflowMenu ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    List<ServiceEntry> serviceEntryList = getServiceEntryList(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(serviceEntryList);
                    return true;
                case 6:
                    parcel.enforceInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    boolean zTestServiceEntryList = testServiceEntryList(parcel.createTypedArrayList(ServiceEntry.CREATOR));
                    parcel2.writeNoException();
                    parcel2.writeInt(zTestServiceEntryList ? 1 : 0);
                    return true;
                case 7:
                    parcel.enforceInterface("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    commitServiceEntryList(parcel.createTypedArrayList(ServiceEntry.CREATOR));
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INfcSettingsAdapter {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public int getModeFlag(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
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
            public void setModeFlag(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isRoutingTableOverflow() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isShowOverflowMenu() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ServiceEntry> getServiceEntryList(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ServiceEntry.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean testServiceEntryList(List<ServiceEntry> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    parcelObtain.writeTypedList(list);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void commitServiceEntryList(List<ServiceEntry> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.nfcsettingsadapter.INfcSettingsAdapter");
                    parcelObtain.writeTypedList(list);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
