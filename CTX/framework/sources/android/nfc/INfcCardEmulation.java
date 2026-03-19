package android.nfc;

import android.content.ComponentName;
import android.nfc.cardemulation.AidGroup;
import android.nfc.cardemulation.ApduServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface INfcCardEmulation extends IInterface {
    AidGroup getAidGroupForService(int i, ComponentName componentName, String str) throws RemoteException;

    List<ApduServiceInfo> getServices(int i, String str) throws RemoteException;

    boolean isDefaultServiceForAid(int i, ComponentName componentName, String str) throws RemoteException;

    boolean isDefaultServiceForCategory(int i, ComponentName componentName, String str) throws RemoteException;

    boolean registerAidGroupForService(int i, ComponentName componentName, AidGroup aidGroup) throws RemoteException;

    boolean removeAidGroupForService(int i, ComponentName componentName, String str) throws RemoteException;

    boolean setDefaultForNextTap(int i, ComponentName componentName) throws RemoteException;

    boolean setDefaultServiceForCategory(int i, ComponentName componentName, String str) throws RemoteException;

    boolean setPreferredService(ComponentName componentName) throws RemoteException;

    boolean supportsAidPrefixRegistration() throws RemoteException;

    boolean unsetPreferredService() throws RemoteException;

    public static abstract class Stub extends Binder implements INfcCardEmulation {
        private static final String DESCRIPTOR = "android.nfc.INfcCardEmulation";
        static final int TRANSACTION_getAidGroupForService = 6;
        static final int TRANSACTION_getServices = 8;
        static final int TRANSACTION_isDefaultServiceForAid = 2;
        static final int TRANSACTION_isDefaultServiceForCategory = 1;
        static final int TRANSACTION_registerAidGroupForService = 5;
        static final int TRANSACTION_removeAidGroupForService = 7;
        static final int TRANSACTION_setDefaultForNextTap = 4;
        static final int TRANSACTION_setDefaultServiceForCategory = 3;
        static final int TRANSACTION_setPreferredService = 9;
        static final int TRANSACTION_supportsAidPrefixRegistration = 11;
        static final int TRANSACTION_unsetPreferredService = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INfcCardEmulation asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof INfcCardEmulation)) {
                return (INfcCardEmulation) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ComponentName componentNameCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsDefaultServiceForCategory = isDefaultServiceForCategory(parcel.readInt(), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsDefaultServiceForCategory ? 1 : 0);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsDefaultServiceForAid = isDefaultServiceForAid(parcel.readInt(), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsDefaultServiceForAid ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean defaultServiceForCategory = setDefaultServiceForCategory(parcel.readInt(), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultServiceForCategory ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean defaultForNextTap = setDefaultForNextTap(parcel.readInt(), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultForNextTap ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcel);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    boolean zRegisterAidGroupForService = registerAidGroupForService(i3, componentNameCreateFromParcel, parcel.readInt() != 0 ? AidGroup.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zRegisterAidGroupForService ? 1 : 0);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    AidGroup aidGroupForService = getAidGroupForService(parcel.readInt(), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    if (aidGroupForService != null) {
                        parcel2.writeInt(1);
                        aidGroupForService.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRemoveAidGroupForService = removeAidGroupForService(parcel.readInt(), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRemoveAidGroupForService ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ApduServiceInfo> services = getServices(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(services);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean preferredService = setPreferredService(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(preferredService ? 1 : 0);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUnsetPreferredService = unsetPreferredService();
                    parcel2.writeNoException();
                    parcel2.writeInt(zUnsetPreferredService ? 1 : 0);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zSupportsAidPrefixRegistration = supportsAidPrefixRegistration();
                    parcel2.writeNoException();
                    parcel2.writeInt(zSupportsAidPrefixRegistration ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INfcCardEmulation {
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
            public boolean isDefaultServiceForCategory(int i, ComponentName componentName, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isDefaultServiceForAid(int i, ComponentName componentName, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setDefaultServiceForCategory(int i, ComponentName componentName, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setDefaultForNextTap(int i, ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean registerAidGroupForService(int i, ComponentName componentName, AidGroup aidGroup) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (aidGroup != null) {
                        parcelObtain.writeInt(1);
                        aidGroup.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public AidGroup getAidGroupForService(int i, ComponentName componentName, String str) throws RemoteException {
                AidGroup aidGroupCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        aidGroupCreateFromParcel = AidGroup.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        aidGroupCreateFromParcel = null;
                    }
                    return aidGroupCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean removeAidGroupForService(int i, ComponentName componentName, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ApduServiceInfo> getServices(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ApduServiceInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setPreferredService(ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean unsetPreferredService() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean supportsAidPrefixRegistration() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
