package android.os;

import java.util.ArrayList;

class ServiceManagerProxy implements IServiceManager {
    private IBinder mRemote;

    public ServiceManagerProxy(IBinder iBinder) {
        this.mRemote = iBinder;
    }

    @Override
    public IBinder asBinder() {
        return this.mRemote;
    }

    @Override
    public IBinder getService(String str) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        parcelObtain.writeInterfaceToken(IServiceManager.descriptor);
        parcelObtain.writeString(str);
        this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
        IBinder strongBinder = parcelObtain2.readStrongBinder();
        parcelObtain2.recycle();
        parcelObtain.recycle();
        return strongBinder;
    }

    @Override
    public IBinder checkService(String str) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        parcelObtain.writeInterfaceToken(IServiceManager.descriptor);
        parcelObtain.writeString(str);
        this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
        IBinder strongBinder = parcelObtain2.readStrongBinder();
        parcelObtain2.recycle();
        parcelObtain.recycle();
        return strongBinder;
    }

    @Override
    public void addService(String str, IBinder iBinder, boolean z, int i) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        parcelObtain.writeInterfaceToken(IServiceManager.descriptor);
        parcelObtain.writeString(str);
        parcelObtain.writeStrongBinder(iBinder);
        parcelObtain.writeInt(z ? 1 : 0);
        parcelObtain.writeInt(i);
        this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
        parcelObtain2.recycle();
        parcelObtain.recycle();
    }

    @Override
    public String[] listServices(int i) throws RemoteException {
        ArrayList arrayList = new ArrayList();
        int i2 = 0;
        while (true) {
            Parcel parcelObtain = Parcel.obtain();
            Parcel parcelObtain2 = Parcel.obtain();
            parcelObtain.writeInterfaceToken(IServiceManager.descriptor);
            parcelObtain.writeInt(i2);
            parcelObtain.writeInt(i);
            i2++;
            try {
                if (!this.mRemote.transact(4, parcelObtain, parcelObtain2, 0)) {
                    break;
                }
                arrayList.add(parcelObtain2.readString());
                parcelObtain2.recycle();
                parcelObtain.recycle();
            } catch (RuntimeException e) {
            }
        }
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        return strArr;
    }

    @Override
    public void setPermissionController(IPermissionController iPermissionController) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        parcelObtain.writeInterfaceToken(IServiceManager.descriptor);
        parcelObtain.writeStrongBinder(iPermissionController.asBinder());
        this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
        parcelObtain2.recycle();
        parcelObtain.recycle();
    }
}
