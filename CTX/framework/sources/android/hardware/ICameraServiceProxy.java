package android.hardware;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ICameraServiceProxy extends IInterface {
    public static final int CAMERA_API_LEVEL_1 = 1;
    public static final int CAMERA_API_LEVEL_2 = 2;
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_EXTERNAL = 2;
    public static final int CAMERA_FACING_FRONT = 1;
    public static final int CAMERA_STATE_ACTIVE = 1;
    public static final int CAMERA_STATE_CLOSED = 3;
    public static final int CAMERA_STATE_IDLE = 2;
    public static final int CAMERA_STATE_OPEN = 0;

    void notifyCameraState(String str, int i, int i2, String str2, int i3) throws RemoteException;

    void pingForUserUpdate() throws RemoteException;

    public static abstract class Stub extends Binder implements ICameraServiceProxy {
        private static final String DESCRIPTOR = "android.hardware.ICameraServiceProxy";
        static final int TRANSACTION_notifyCameraState = 2;
        static final int TRANSACTION_pingForUserUpdate = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICameraServiceProxy asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ICameraServiceProxy)) {
                return (ICameraServiceProxy) iInterfaceQueryLocalInterface;
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
                    pingForUserUpdate();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyCameraState(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ICameraServiceProxy {
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
            public void pingForUserUpdate() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyCameraState(String str, int i, int i2, String str2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
