package com.mediatek.apm.test.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface ITestAmPolicyMakerService extends IInterface {
    List<String> getFrcPackageList(String str) throws RemoteException;

    List<String> getSuppressionList() throws RemoteException;

    boolean isPackageInSuppression(String str, String str2, int i) throws RemoteException;

    void startFrc(String str, int i, List<String> list) throws RemoteException;

    void startSuppression(String str, int i, int i2, String str2, List<String> list) throws RemoteException;

    void stopFrc(String str) throws RemoteException;

    void stopSuppression(String str) throws RemoteException;

    void updateFrcExtraAllowList(String str, List<String> list) throws RemoteException;

    void updateSuppressionExtraAllowList(String str, List<String> list) throws RemoteException;

    public static abstract class Stub extends Binder implements ITestAmPolicyMakerService {
        public Stub() {
            attachInterface(this, "com.mediatek.apm.test.service.ITestAmPolicyMakerService");
        }

        public static ITestAmPolicyMakerService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ITestAmPolicyMakerService)) {
                return (ITestAmPolicyMakerService) iInterfaceQueryLocalInterface;
            }
            return new a(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    startFrc(parcel.readString(), parcel.readInt(), parcel.createStringArrayList());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    stopFrc(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    List<String> frcPackageList = getFrcPackageList(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStringList(frcPackageList);
                    return true;
                case 4:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    updateFrcExtraAllowList(parcel.readString(), parcel.createStringArrayList());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    startSuppression(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.createStringArrayList());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    stopSuppression(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    updateSuppressionExtraAllowList(parcel.readString(), parcel.createStringArrayList());
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    List<String> suppressionList = getSuppressionList();
                    parcel2.writeNoException();
                    parcel2.writeStringList(suppressionList);
                    return true;
                case 9:
                    parcel.enforceInterface("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    boolean zIsPackageInSuppression = isPackageInSuppression(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPackageInSuppression ? 1 : 0);
                    return true;
                case 1598968902:
                    parcel2.writeString("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class a implements ITestAmPolicyMakerService {
            private IBinder A;

            a(IBinder iBinder) {
                this.A = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.A;
            }

            @Override
            public void startFrc(String str, int i, List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStringList(list);
                    this.A.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopFrc(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    this.A.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<String> getFrcPackageList(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    this.A.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArrayList();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateFrcExtraAllowList(String str, List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringList(list);
                    this.A.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startSuppression(String str, int i, int i2, String str2, List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeStringList(list);
                    this.A.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopSuppression(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    this.A.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateSuppressionExtraAllowList(String str, List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringList(list);
                    this.A.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<String> getSuppressionList() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    this.A.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArrayList();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPackageInSuppression(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.apm.test.service.ITestAmPolicyMakerService");
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.A.transact(9, parcelObtain, parcelObtain2, 0);
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
