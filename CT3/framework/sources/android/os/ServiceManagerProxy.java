package android.os;

import java.util.ArrayList;

class ServiceManagerProxy implements IServiceManager {
    private IBinder mRemote;

    public ServiceManagerProxy(IBinder remote) {
        this.mRemote = remote;
    }

    @Override
    public IBinder asBinder() {
        return this.mRemote;
    }

    @Override
    public IBinder getService(String name) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        this.mRemote.transact(1, data, reply, 0);
        IBinder binder = reply.readStrongBinder();
        reply.recycle();
        data.recycle();
        return binder;
    }

    @Override
    public IBinder checkService(String name) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        this.mRemote.transact(2, data, reply, 0);
        IBinder binder = reply.readStrongBinder();
        reply.recycle();
        data.recycle();
        return binder;
    }

    @Override
    public void addService(String name, IBinder service, boolean allowIsolated) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        data.writeStrongBinder(service);
        data.writeInt(allowIsolated ? 1 : 0);
        this.mRemote.transact(3, data, reply, 0);
        reply.recycle();
        data.recycle();
    }

    @Override
    public String[] listServices() throws RemoteException {
        ArrayList<String> services = new ArrayList<>();
        int n = 0;
        while (true) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(IServiceManager.descriptor);
            data.writeInt(n);
            n++;
            try {
                boolean res = this.mRemote.transact(4, data, reply, 0);
                if (!res) {
                    break;
                }
                services.add(reply.readString());
                reply.recycle();
                data.recycle();
            } catch (RuntimeException e) {
            }
        }
        String[] array = new String[services.size()];
        services.toArray(array);
        return array;
    }

    @Override
    public void setPermissionController(IPermissionController controller) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeStrongBinder(controller.asBinder());
        this.mRemote.transact(6, data, reply, 0);
        reply.recycle();
        data.recycle();
    }
}
