package android.os;

import android.os.IPermissionController;

public abstract class ServiceManagerNative extends Binder implements IServiceManager {
    public static IServiceManager asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IServiceManager in = (IServiceManager) obj.queryLocalInterface(IServiceManager.descriptor);
        if (in != null) {
            return in;
        }
        return new ServiceManagerProxy(obj);
    }

    public ServiceManagerNative() {
        attachInterface(this, IServiceManager.descriptor);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        switch (code) {
            case 1:
                data.enforceInterface(IServiceManager.descriptor);
                String name = data.readString();
                IBinder service = getService(name);
                reply.writeStrongBinder(service);
                return true;
            case 2:
                data.enforceInterface(IServiceManager.descriptor);
                String name2 = data.readString();
                IBinder service2 = checkService(name2);
                reply.writeStrongBinder(service2);
                return true;
            case 3:
                data.enforceInterface(IServiceManager.descriptor);
                String name3 = data.readString();
                IBinder service3 = data.readStrongBinder();
                boolean allowIsolated = data.readInt() != 0;
                addService(name3, service3, allowIsolated);
                return true;
            case 4:
                data.enforceInterface(IServiceManager.descriptor);
                String[] list = listServices();
                reply.writeStringArray(list);
                return true;
            case 5:
            default:
                return false;
            case 6:
                data.enforceInterface(IServiceManager.descriptor);
                IPermissionController controller = IPermissionController.Stub.asInterface(data.readStrongBinder());
                setPermissionController(controller);
                return true;
        }
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
