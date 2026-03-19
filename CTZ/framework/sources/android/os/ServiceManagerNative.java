package android.os;

import android.os.IPermissionController;

public abstract class ServiceManagerNative extends Binder implements IServiceManager {
    public static IServiceManager asInterface(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IServiceManager iServiceManager = (IServiceManager) iBinder.queryLocalInterface(IServiceManager.descriptor);
        if (iServiceManager != null) {
            return iServiceManager;
        }
        return new ServiceManagerProxy(iBinder);
    }

    public ServiceManagerNative() {
        attachInterface(this, IServiceManager.descriptor);
    }

    @Override
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) {
        try {
            if (i != 6) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface(IServiceManager.descriptor);
                        parcel2.writeStrongBinder(getService(parcel.readString()));
                        break;
                    case 2:
                        parcel.enforceInterface(IServiceManager.descriptor);
                        parcel2.writeStrongBinder(checkService(parcel.readString()));
                        break;
                    case 3:
                        parcel.enforceInterface(IServiceManager.descriptor);
                        addService(parcel.readString(), parcel.readStrongBinder(), parcel.readInt() != 0, parcel.readInt());
                        break;
                    case 4:
                        parcel.enforceInterface(IServiceManager.descriptor);
                        parcel2.writeStringArray(listServices(parcel.readInt()));
                        break;
                }
                return true;
            }
            parcel.enforceInterface(IServiceManager.descriptor);
            setPermissionController(IPermissionController.Stub.asInterface(parcel.readStrongBinder()));
            return true;
        } catch (RemoteException e) {
        }
        return false;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
